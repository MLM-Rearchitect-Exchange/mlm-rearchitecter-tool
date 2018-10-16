package importer.formats;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import common.Constants;
import common.Debugger;
import common.Utils;
import importer.Format;

public class MultEcore implements Format {

	private List<File> mefFiles;
	private Resource multilevelResource;
	private String hierarchyName;
	
	
	@Override
	public void openImportFile(URI hierarchyURI) throws IOException {
		File file = new File(hierarchyURI.toFileString());
		if (file.isDirectory()) {
			mefFiles = Arrays.asList(file.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					String[] fragments = name.split("\\.");
					if ((fragments.length == 2) && (fragments[1].equalsIgnoreCase("mef")))
						return true;
					Debugger.log("Ignoring file " + name + " for importing");
					return false;
				}
			}));
		} else {
			mefFiles = new ArrayList<File>();
			mefFiles.add(file);
		}
		hierarchyName = hierarchyURI.trimFileExtension().lastSegment();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public boolean importHierarchy(Resource multilevelResource) throws IOException {
		// Create hierarchy
		this.multilevelResource = multilevelResource;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			System.err.println("Could not import documents for " + multilevelResource.getURI().lastSegment());
			return false;
		}
		EObject hierarchyEO = EcoreUtil.create(Constants.hierarchyEC);
		Utils.setValueOfFeature(hierarchyEO, "name", hierarchyName);
		multilevelResource.getContents().add(hierarchyEO);
		
		Map<String, EObject> metamodels = new HashMap<String, EObject>();
		Map<EObject, Map<String, EObject>> allClabjects = new HashMap<EObject, Map<String,EObject>>();
		Map<EObject, Map<String, Set<EObject>>> allReferences = new HashMap<EObject, Map<String,Set<EObject>>>();
		// To ensure that they are added in order from top to bottom
		while (metamodels.size() < mefFiles.size()) {
			// Create models
			for (File mefFile : mefFiles) {
				int maxDepth = 0;
				Document document = null;
				try {
					document = documentBuilder.parse(mefFile);
				} catch (SAXException e) {
					System.err.println("Could not import " + mefFile.getName());
					continue;
				}
				Node rootNode = getChildNode(document, "Model");
				String modelName = mefFile.getName().split("\\.")[0];
				String metamodelName = rootNode.getAttributes().getNamedItem("om").getNodeValue();
				String[] metamodelNameFragments = metamodelName.split("/");
				if (metamodelNameFragments.length > 1) metamodelName = metamodelNameFragments[metamodelNameFragments.length - 1];
				if ((metamodels.containsKey(modelName)) ||
						(!(metamodelName.equals("Ecore")) && (!metamodels.containsKey(metamodelName))))
					continue;
				
				List<Node> nodeList = getChildrenElementNodes(getChildNode(rootNode, "Elements")); // The nodes
				List<Node> referenceList = getChildrenElementNodes(getChildNode(rootNode, "Relations")); // The relations
				EObject modelEO = EcoreUtil.create(Constants.modelEC);
				Utils.setValueOfFeature(modelEO, "name", modelName);
				Utils.setValueOfFeature(hierarchyEO, "models", modelEO);
				
				// To set metamodels correctly
				metamodels.put(modelName, modelEO);
				EObject metamodelEO = metamodels.get(metamodelName);
				int level;
				if (null != metamodelEO) {
					Utils.setValueOfFeature(modelEO, "parent", metamodelEO);
					Utils.setValueOfFeature(metamodelEO, "children", modelEO);
					level = ((int) Utils.getValueOfFeature((EObject) Utils.getValueOfFeature(metamodelEO, "level"), "level")) + 1;
				} else {
					level = 1;
				}
				EObject levelEO = EcoreUtil.create(Constants.levelEC);
				Utils.setValueOfFeature(levelEO, "level", level);
				Utils.setValueOfFeature(modelEO, "level", levelEO);

				// Create clabjects
				Map<String, EObject> clabjects = new HashMap<String, EObject>();
				for (Node nodeNode : nodeList) {
					EObject clabjectEO = EcoreUtil.create(Constants.clabjectEC);
					String nodeName = nodeNode.getNodeName();
					clabjects.put(nodeName, clabjectEO);
					Utils.setValueOfFeature(clabjectEO, "name", nodeName);
					NamedNodeMap nodeAttributes = nodeNode.getAttributes();
					
					// Set abstract
					boolean isAbstract = Boolean.valueOf(nodeAttributes.getNamedItem("isAbstract").getNodeValue());
					Utils.setValueOfFeature(clabjectEO, "isAbstract", isAbstract);
					
					// Set potency
					int potencyDepth = Integer.parseInt(nodeAttributes.getNamedItem("potencyDepth").getNodeValue());
					maxDepth = (maxDepth < potencyDepth)? potencyDepth : maxDepth;
					addPotency(clabjectEO,
							Integer.parseInt(nodeAttributes.getNamedItem("potencyStart").getNodeValue()),
							Integer.parseInt(nodeAttributes.getNamedItem("potencyEnd").getNodeValue()),
							potencyDepth);
					Utils.setValueOfFeature(modelEO, "elements", clabjectEO);
					
					// Set type
					String clabjectTypeName = nodeAttributes.getNamedItem("type").getNodeValue();
					EObject potencyMetamodelEO = metamodelEO;
					if (clabjectTypeName.contains("@")) {
						String[] fragments = clabjectTypeName.split("@");
						int typeReversePotency = Integer.valueOf(fragments[1]);
						clabjectTypeName = fragments[0];
						for (int i=1; i<typeReversePotency; i++) {
							potencyMetamodelEO = (EObject) Utils.getValueOfFeature(potencyMetamodelEO, "parent");
						}
					}
					Map<String, EObject> metamodelClabjects = allClabjects.get(potencyMetamodelEO);
					if (null != metamodelClabjects)
						Utils.setValueOfFeature(clabjectEO, "types", metamodelClabjects.get(clabjectTypeName));
					
					// Create attributes for this clabject
					for (Node attributeNode : getChildrenElementNodes(nodeNode)) {
						NamedNodeMap attributeAttributes = attributeNode.getAttributes();
						Node attributeTypeNode = attributeAttributes.getNamedItem("type");
						if (null == attributeTypeNode)
							continue;
						EObject attributeEO = EcoreUtil.create(Constants.attributeEC);
						Utils.setValueOfFeature(attributeEO, "name", attributeNode.getNodeName());
						int potencyStart = Integer.parseInt(attributeAttributes.getNamedItem("potencyStart").getNodeValue());
						int potencyEnd = Integer.parseInt(attributeAttributes.getNamedItem("potencyEnd").getNodeValue());
						int attributePotencyDepth = Integer.parseInt(attributeAttributes.getNamedItem("potencyDepth").getNodeValue());
						maxDepth = (maxDepth < attributePotencyDepth)? attributePotencyDepth : maxDepth;
						addPotency(attributeEO, potencyStart, potencyEnd, attributePotencyDepth);
						addCardinality(attributeEO,
								Integer.parseInt(attributeAttributes.getNamedItem("lowerBound").getNodeValue()),
								Integer.parseInt(attributeAttributes.getNamedItem("upperBound").getNodeValue()),
								potencyStart, potencyEnd, attributePotencyDepth);
						String attributeTypeName = null;
						switch (attributeTypeNode.getNodeValue()) {
						case "Boolean":
							attributeTypeName = "bool";
							break;
						case "Integer":
							attributeTypeName = "int";
							break;
						case "Real":
							attributeTypeName = "float";
							break;
						case "String":
						default:
							attributeTypeName = "string";
						}
						Utils.setValueOfFeature(attributeEO, "type", Constants.dataTypeEE.getEEnumLiteral(attributeTypeName));
						Utils.setValueOfFeature(clabjectEO, "features", attributeEO);
					}
				}
				// To keep track and be able to set types
				allClabjects.put(modelEO, clabjects);
				
				// Add superclasses
				for (Node nodeNode : nodeList) {
					String parentNodesString = nodeNode.getAttributes().getNamedItem("parentNodes").getNodeValue();
					if (parentNodesString.isEmpty())
						continue;
					EObject clabjectEO = clabjects.get(nodeNode.getNodeName());
					String[] parentNodeNames = parentNodesString.split(";");
					for (int i=0; i<parentNodeNames.length; i++) {
						EObject parentClabjectEO = clabjects.get(parentNodeNames[i]);
						Utils.setValueOfFeature(clabjectEO, "superTypes", parentClabjectEO);
						Utils.setValueOfFeature(parentClabjectEO, "subTypes", clabjectEO);
					}
				}
				
				// Create references
				Map<String, Set<EObject>> references = new HashMap<String, Set<EObject>>();
				for (Node referenceNode : referenceList) {
					NamedNodeMap referenceAttributes = referenceNode.getAttributes();
					String sourceNodeName = referenceAttributes.getNamedItem("source").getNodeValue();
					String targetNodeName = referenceAttributes.getNamedItem("target").getNodeValue();
					EObject referenceEO = EcoreUtil.create(Constants.referenceEC);
					String referenceName = referenceNode.getNodeName();
					references.computeIfAbsent(referenceName, k -> new HashSet<EObject>()).add(referenceEO);
					Utils.setValueOfFeature(referenceEO, "name", referenceName);
					Utils.setValueOfFeature(referenceEO, "target", clabjects.get(targetNodeName));
					int potencyStart = Integer.parseInt(referenceAttributes.getNamedItem("potencyStart").getNodeValue());
					int potencyEnd = Integer.parseInt(referenceAttributes.getNamedItem("potencyEnd").getNodeValue());
					int potencyDepth = Integer.parseInt(referenceAttributes.getNamedItem("potencyDepth").getNodeValue());
					maxDepth = (maxDepth < potencyDepth)? potencyDepth : maxDepth;
					addPotency(referenceEO, potencyStart, potencyEnd, potencyDepth);
					addCardinality(referenceEO,
							Integer.parseInt(referenceAttributes.getNamedItem("lowerBound").getNodeValue()),
							Integer.parseInt(referenceAttributes.getNamedItem("upperBound").getNodeValue()),
							potencyStart, potencyEnd, potencyDepth);
					
					// Set type
					String referenceTypeName = referenceAttributes.getNamedItem("type").getNodeValue();
					EObject potencyMetamodelEO = metamodelEO;
					if (referenceTypeName.contains("@")) {
						String[] fragments = referenceTypeName.split("@");
						int typeReversePotency = Integer.valueOf(fragments[1]);
						referenceTypeName = fragments[0];
						for (int i=1; i<typeReversePotency; i++) {
							potencyMetamodelEO = (EObject) Utils.getValueOfFeature(potencyMetamodelEO, "parent");
						}
					}
					EObject sourceEO = clabjects.get(sourceNodeName);
					Utils.setValueOfFeature(sourceEO, "features", referenceEO);
					Map<String, Set<EObject>> metamodelReferences = allReferences.get(potencyMetamodelEO);
					if (null == metamodelReferences)
						continue;
					Set<EObject> referenceTypeCandidates = metamodelReferences.get(referenceTypeName);
					if (null == referenceTypeCandidates)
						continue;
					EObject referenceTypeEO = null;
					for (EObject eo : referenceTypeCandidates) {
						if (Utils.getValueOfFeature(eo, "name").equals(referenceTypeName)) {
							EObject sourceTypeEO = sourceEO;
							boolean found = false;
							do {
								EList<EObject> sourceTypes = (EList<EObject>) Utils.getValueOfFeature(sourceTypeEO, "types");
								sourceTypeEO = (sourceTypes.isEmpty())? null : sourceTypes.get(0);
								if ((null != sourceTypeEO) && (sourceTypeEO.equals(eo.eContainer()))) found = true;
							} while ((null != sourceTypeEO) && (!found));
							if (found) {
								referenceTypeEO = eo;
								break;
							}
						}
					}
					if (null != referenceTypeEO)
						Utils.setValueOfFeature(referenceEO, "types", referenceTypeEO);
				}
				allReferences.put(modelEO, references);
				
				addPotency(modelEO, 1, 1, maxDepth);
			}
			
		}
		
		return true;
	}
	
	
	private Node getChildNode(Node parentNode, String nodeName) {
		NodeList childNodes = parentNode.getChildNodes();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if ((childNode.getNodeType() == Node.ELEMENT_NODE) && (childNode.getNodeName().equals(nodeName)))
				return childNode;
		}
		return null;
	}

	
	private List<Node> getChildrenElementNodes(Node parentNode) {
		List<Node> childrenElementNodes = new ArrayList<Node>();
		NodeList childNodes = parentNode.getChildNodes();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE)
				childrenElementNodes.add(childNode);
		}
		return childrenElementNodes;
	}
	
	
	private void addPotency(EObject eObject, int start, int end, int depth) {
		EObject potencyEO = EcoreUtil.create(Constants.potencyEC);
		Utils.setValueOfFeature(potencyEO, "start", start);
		Utils.setValueOfFeature(potencyEO, "end", end);
		Utils.setValueOfFeature(potencyEO, "depth", depth);
		Utils.setValueOfFeature(eObject, "potency", potencyEO);
	}
	
	
	private void addCardinality(EObject eObject, int min, int max, int potencyStart, int potencyEnd, int potencyDepth) {
		if (potencyDepth > 1) {
			for (int i=1; i<=potencyDepth; i++) {
				EObject cardinalityEO = EcoreUtil.create(Constants.cardinalityEC);
				Utils.setValueOfFeature(cardinalityEO, "min", min);
				Utils.setValueOfFeature(cardinalityEO, "max", max);
				addPotency(cardinalityEO, i, i, 1);
				Utils.setValueOfFeature(eObject, "cardinality", cardinalityEO);
			}
		} else {
			for (int i=potencyStart; i<=potencyEnd; i++) {
				EObject cardinalityEO = EcoreUtil.create(Constants.cardinalityEC);
				Utils.setValueOfFeature(cardinalityEO, "min", min);
				Utils.setValueOfFeature(cardinalityEO, "max", max);
				addPotency(cardinalityEO, i, i, 1);
				Utils.setValueOfFeature(eObject, "cardinality", cardinalityEO);
			}
		}
	}

	
	@Override
	public void closeImportFile() throws IOException {
		// Save generated model with the multilevel hierarchy
		Map<String, Boolean> options = new HashMap<String, Boolean>();
		options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
		try {
			multilevelResource.save(options);
		} catch (IOException e1) {
			System.err.println("Error saving model");
			e1.printStackTrace();
		}
	}

}
