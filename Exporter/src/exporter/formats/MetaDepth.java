package exporter.formats;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import common.Constants;
import exporter.Format;

public class MetaDepth implements Format {

	BufferedWriter bw = null;
	FileWriter fw = null;
	
	
	@Override
	public void openExportFile (URI destinationURI) throws IOException {
		fw = new FileWriter(destinationURI.appendFileExtension("mdepth").toFileString());
		bw = new BufferedWriter(fw);
	}
	

	@SuppressWarnings("unchecked")
	@Override
	public boolean export (Resource multilevelResource) throws IOException {		
		int modelPotencyDepth = 0;
		
		TreeIterator<EObject> allContents = multilevelResource.getAllContents();
		while (allContents.hasNext()) {
			EObject modelEO = allContents.next();
			if (!EcoreUtil.equals(modelEO.eClass(), Constants.modelEC))
				continue;
			
			// Models
			modelEO.eGet(modelEO.eClass().getEStructuralFeature("name"));
			EObject parentModelEO = (EObject) modelEO.eGet(modelEO.eClass().getEStructuralFeature("parent"));
			if (null == parentModelEO) {
				EObject potencyEO = (EObject) modelEO.eGet(modelEO.eClass().getEStructuralFeature("potency"));
				modelPotencyDepth = (null == potencyEO)? 1 : Integer.parseInt(potencyEO.eGet(potencyEO.eClass().getEStructuralFeature("depth")).toString());
				bw.write("Model" + " " + modelEO.eGet(modelEO.eClass().getEStructuralFeature("name")) + "@" + modelPotencyDepth + "{\n");
			} else {
				bw.write(parentModelEO.eGet(modelEO.eClass().getEStructuralFeature("name")) + " " + modelEO.eGet(modelEO.eClass().getEStructuralFeature("name")) + "{\n");
			}
			
			// Classes
			EList<EObject> nodes = (EList<EObject>) modelEO.eGet(modelEO.eClass().getEStructuralFeature("elements"));		
			for (EObject clabjectEO : nodes) {
				if (!EcoreUtil.equals(clabjectEO.eClass(), Constants.clabjectEC))
					continue;

				String parentNodeName = "Node";
				EList<EObject> parentNodeList = (EList<EObject>) clabjectEO.eGet(clabjectEO.eClass().getEStructuralFeature("types"));
				EObject parentNodeEO = null;
				if ((null != parentNodeList) && (!parentNodeList.isEmpty()) && (null != (parentNodeEO = parentNodeList.get(0)))) {
					parentNodeName = (String) parentNodeEO.eGet(parentNodeEO.eClass().getEStructuralFeature("name"));
				}
				String isAbstract = "";
				if ((boolean) clabjectEO.eGet(clabjectEO.eClass().getEStructuralFeature("isAbstract"))) {
					isAbstract = "abstract ";
				}
				String superTypesString = "";
				EList<EObject> superTypes = (EList<EObject>) clabjectEO.eGet(clabjectEO.eClass().getEStructuralFeature("superTypes"));
				if (!superTypes.isEmpty()) {
					superTypesString = " : ";
					for (EObject seo : superTypes) {
						superTypesString += seo.eGet(seo.eClass().getEStructuralFeature("name")) + ", ";
					}
					superTypesString = superTypesString.substring(0, superTypesString.length() - 2);
				}
				
				bw.write(isAbstract + parentNodeName + " " + clabjectEO.eGet(clabjectEO.eClass().getEStructuralFeature("name")) + superTypesString + " {\n");
				
				EList<EObject> features = (EList<EObject>) clabjectEO.eGet(clabjectEO.eClass().getEStructuralFeature("features"));
				
				// Common for features (attributes or references)
				for (EObject featureEO : features) {
					EObject potencyEO = (EObject) featureEO.eGet(featureEO.eClass().getEStructuralFeature("potency"));
					int start = Integer.parseInt(potencyEO.eGet(potencyEO.eClass().getEStructuralFeature("start")).toString());
					int end = Integer.parseInt(potencyEO.eGet(potencyEO.eClass().getEStructuralFeature("end")).toString());
					int depth = Integer.parseInt(potencyEO.eGet(potencyEO.eClass().getEStructuralFeature("depth")).toString());
					String potency = (depth!=modelPotencyDepth)? "@" + depth : "";
					String name = (String) featureEO.eGet(featureEO.eClass().getEStructuralFeature("name"));
					EList<EObject> cardinalities = (EList<EObject>) featureEO.eGet(featureEO.eClass().getEStructuralFeature("cardinality"));
					EObject cardinalityEO = null;
					for (EObject eo : cardinalities) {
						EObject cardinalityPotencyEO = (EObject) eo.eGet(eo.eClass().getEStructuralFeature("potency"));
						if (cardinalityPotencyEO.eGet(potencyEO.eClass().getEStructuralFeature("start")).equals(1)) {
							cardinalityEO = eo;
						}
					}
					if (null == cardinalityEO) cardinalityEO = cardinalities.get(0);
					int min = (int) cardinalityEO.eGet(cardinalityEO.eClass().getEStructuralFeature("min"));
					int max = (int) cardinalityEO.eGet(cardinalityEO.eClass().getEStructuralFeature("max"));
					String minString = min==-1? "*" : String.valueOf(min);
					String maxString = max==-1? "*" : String.valueOf(max);
					String cardinality = "[" + minString + ".." + maxString + "]";
					String type = "";
					String metaTypeName = "";
					
					// References
					if (EcoreUtil.equals(featureEO.eClass(), Constants.referenceEC)) {
						EObject targetEO = (EObject) featureEO.eGet(featureEO.eClass().getEStructuralFeature("target"));
						type = targetEO.eGet(targetEO.eClass().getEStructuralFeature("name")).toString();
						EList<EObject> typesList = (EList<EObject>) featureEO.eGet(featureEO.eClass().getEStructuralFeature("types"));
						EObject metaType = ((null != typesList) && (!typesList.isEmpty()))? typesList.get(0) : null;
						if (null != metaType) {
							metaTypeName = metaType.eGet(metaType.eClass().getEStructuralFeature("name")).toString();
						}
						
						// Special syntax if it is the bottommost level
						if (depth==0) {							
							bw.write(metaTypeName + " = " + type + ";\n");
							continue;
						}
						// If it is not the bottommost level, the metaType syntax is as in the rest of the cases
						if (null != metaType) metaTypeName = " {" + metaTypeName + "}";
					
						if ((depth==1) && (start>1) && (start==end)) {
							potency = "(@" + start + ")";
						}
						
					// Attributes
					} else if (EcoreUtil.equals(featureEO.eClass(), Constants.attributeEC)) {
						type = featureEO.eGet(featureEO.eClass().getEStructuralFeature("type")).toString();
						if (type.equals("string")) type = "String";
						if (type.equals("float")) type = "double";
						if (min==1 && max==1) {
							cardinality = "";
						}
						
						// It cannot be that depth!=1 if (start and) end > 1, so depth is ignored (assumed 1) in the latter case
						if (end>1) {
							for (int i=start+1;i<=end;i++) {
								potency = "@" + i;
								bw.write(name + (i-start+1) + potency + " : " + type + cardinality + "\n");
							}
							potency = (start!=modelPotencyDepth)? "@" + start : "";
						}
					}
					bw.write(name + potency + " : " + type + cardinality + metaTypeName + ";\n");
				}
				
				bw.write("}\n");
			}
			
			bw.write("}\n");

		}
		return true;
	}

	
	@Override
	public void closeExportFile () throws IOException {
		if (bw != null)
			bw.close();
			
		if (fw != null)
			fw.close();
	}

}
