package exporter.formats;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import common.Constants;
import common.Debugger;
import common.Utils;
import exporter.Format;

public class MultEcore implements Format {
	
	private URI destinationURI;
	private DocumentBuilder documentBuilder;
	private Map<String,Document> documents;
	

	@Override
	public void openExportFile(URI destinationURI) throws IOException {
		this.destinationURI = destinationURI;
		documents = new HashMap<String,Document>();
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			Debugger.log("Could not export " + destinationURI + "to MultEcore"); //TODO
			return;
		}
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public boolean export(Resource multilevelResource) throws IOException {
		EObject hierarchyEO = multilevelResource.getContents().get(0);
		if (!EcoreUtil.equals(hierarchyEO.eClass(), Constants.hierarchyEC)) {
			Debugger.log("Malformed multilevel hierarchy");
			return false;
		}
		EStructuralFeature modelsESF = hierarchyEO.eClass().getEStructuralFeature("models");
		EList<EObject> modelList = (EList<EObject>) hierarchyEO.eGet(modelsESF);
		for (EObject modelEO : modelList) {
			EStructuralFeature nameESF = modelEO.eClass().getEStructuralFeature("name");
			String modelName = (String) modelEO.eGet(nameESF);
			EStructuralFeature parentESF = modelEO.eClass().getEStructuralFeature("parent");
			EObject metamodelEO = (EObject) modelEO.eGet(parentESF);
			String metamodelName = "Ecore";
			if (null != metamodelEO) {
				metamodelName = (String) metamodelEO.eGet(nameESF);
			}
			Document document = documentBuilder.newDocument();
			documents.put(modelName, document);
			
			// Create model container
			Element model = document.createElement("Model");
			model.setAttribute("om", metamodelName);
			document.appendChild(model);

			// Create nodes container
			Element nodes = document.createElement("Elements");
			model.appendChild(nodes);

			// Create relations container
			Element relations = document.createElement("Relations");
			model.appendChild(relations);
			
			// Create nodes and relations
			for (EObject clabjectEO : (EList<EObject>) Utils.getValueOfFeature(modelEO, "elements")) {
				Element nodeElement = createNode(document, clabjectEO);
				nodes.appendChild(nodeElement);
				for (EObject featureEO : (EList<EObject>) Utils.getValueOfFeature(clabjectEO, "features")) {
					if (EcoreUtil.equals(featureEO.eClass(), Constants.referenceEC)) {
						relations.appendChild(createRelation(document, featureEO));
					} else {
						nodeElement.appendChild(createAttribute(document, featureEO));
					}
				}
			}
		}
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	private Element createNode(Document document, EObject clabjectEO) {
		if (Boolean.parseBoolean(Utils.getValueOfFeature(clabjectEO, "isMultiType").toString()))
			Debugger.log("Ignoring multiple types of clabject: " + clabjectEO.toString());
		EList<EObject> types = ((EList<EObject>) Utils.getValueOfFeature(clabjectEO, "types"));
		EObject typeEO = (null == types || types.isEmpty())? null : types.get(0);
		String typeName = (null == typeEO)? "EClass" : (String) Utils.getValueOfFeature(typeEO, "name");
        Element element = document.createElement((String) Utils.getValueOfFeature(clabjectEO, "name"));
        element.setAttribute("isAbstract", String.valueOf(Utils.getValueOfFeature(clabjectEO, "isAbstract")));
        element.setAttribute("type", typeName);
        EObject potencyEO = (EObject) Utils.getValueOfFeature(clabjectEO, "potency");
        element.setAttribute("potencyStart", String.valueOf(Utils.getValueOfFeature(potencyEO, "start")));
        element.setAttribute("potencyEnd", String.valueOf(Utils.getValueOfFeature(potencyEO, "end")));
        element.setAttribute("potencyDepth", String.valueOf(Utils.getValueOfFeature(potencyEO, "depth")));
        String parentNodesAttribute = "";
        for (EObject parentNode : ((EList<EObject>) Utils.getValueOfFeature(clabjectEO, "superTypes"))) {
        	parentNodesAttribute += Utils.getValueOfFeature(parentNode, "name") + ";";
        }
		element.setAttribute("parentNodes", parentNodesAttribute);
        return element;
	}
	
	
	@SuppressWarnings("unchecked")
	private Element createRelation(Document document, EObject referenceEO) {
        Element element = createFeature(document, referenceEO);
		EList<EObject> types = ((EList<EObject>) Utils.getValueOfFeature(referenceEO, "types"));
		EObject typeEO = (null == types || types.isEmpty())? null : types.get(0);
		if (null != types && types.size() > 1)
			Debugger.log("Ignoring multiple types of reference: " + referenceEO.toString());
		String typeName = (null == typeEO)? "EReference" : (String) Utils.getValueOfFeature(typeEO, "name");
		element.setAttribute("type", typeName);
        EObject potencyEO = (EObject) Utils.getValueOfFeature(referenceEO, "potency");
        element.setAttribute("potencyStart", String.valueOf(Utils.getValueOfFeature(potencyEO, "start")));
        element.setAttribute("potencyEnd", String.valueOf(Utils.getValueOfFeature(potencyEO, "end")));
        element.setAttribute("potencyDepth", String.valueOf(Utils.getValueOfFeature(potencyEO, "depth")));
        element.setAttribute("source", (String) Utils.getValueOfFeature(referenceEO.eContainer(), "name"));
        EObject targetEO = (EObject) Utils.getValueOfFeature(referenceEO, "target");
        element.setAttribute("target", (String) Utils.getValueOfFeature(targetEO, "name"));
		element.setAttribute("containment", "false");
        return element;
	}
	

	private Element createAttribute(Document document, EObject attributeEO) {
        Element element = createFeature(document, attributeEO);
        EObject typeEO = (EObject) Utils.getValueOfFeature(attributeEO, "type");
        String typeName = (null == typeEO)? "String" : (String) Utils.getValueOfFeature(typeEO, "name");
        switch (typeName) {
        case "bool":
        	typeName = "Boolean";
        	break;
        case "int":
        	typeName = "Integer";
        	break;
        case "float":
        	typeName = "Real";
        	break;
        case "string":
        	typeName = "String";
        default:
        }
		element.setAttribute("type", typeName);
        element.setAttribute("isId", "false");
        EObject potencyEO = (EObject) Utils.getValueOfFeature(attributeEO, "potency");
        element.setAttribute("potencyStart", String.valueOf(Utils.getValueOfFeature(potencyEO, "start")));
        element.setAttribute("potencyEnd", String.valueOf(Utils.getValueOfFeature(potencyEO, "end")));
        element.setAttribute("potencyDepth", String.valueOf(Utils.getValueOfFeature(potencyEO, "depth")));
        return element;
	}
	

	@SuppressWarnings("unchecked")
	private Element createFeature(Document document, EObject featureEO) {
		Element element = document.createElement((String) Utils.getValueOfFeature(featureEO, "name"));
		EList<EObject> cardinalities = (EList<EObject>) Utils.getValueOfFeature(featureEO, "cardinality");
		EObject cardinalityEO = (null == cardinalities || cardinalities.isEmpty())? null : cardinalities.get(0);
		if (null != cardinalities && cardinalities.size() > 1)
			Debugger.log("Ignoring multiple cardinalities of feature: " + featureEO.toString());
		element.setAttribute("lowerBound", String.valueOf(Utils.getValueOfFeature(cardinalityEO, "min")));
		element.setAttribute("upperBound", String.valueOf(Utils.getValueOfFeature(cardinalityEO, "max")));
        EObject potencyEO = (EObject) Utils.getValueOfFeature(featureEO, "potency");
        element.setAttribute("potencyStart", String.valueOf(Utils.getValueOfFeature(potencyEO, "start")));
        element.setAttribute("potencyEnd", String.valueOf(Utils.getValueOfFeature(potencyEO, "end")));
		return element;
	}
	
	
	@Override
	public void closeExportFile() throws IOException {
		// Create required folders
		new File(destinationURI.toFileString()).mkdirs();

		// Create one MEF file per model
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
        for (String modelName : documents.keySet()) {
        	Document document = documents.get(modelName);
        	DOMSource source = new DOMSource(document);
        	String destinationURIString = destinationURI.toFileString() + File.separator + modelName + ".mef";
			File newMefFile = new File(destinationURIString);
        	StreamResult result = new StreamResult(newMefFile);
        	javax.xml.transform.Transformer transformer = null;
			try {
				transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes"); 
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
				transformer.transform(source, result);
			} catch (TransformerException e) {
				Debugger.log("Could not save file " + destinationURIString);
				continue;
			}
        }
	}

}
