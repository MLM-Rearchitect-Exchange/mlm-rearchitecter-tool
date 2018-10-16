package transformer.transformations;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import common.Annotations;
import transformer.ElementRegistry;

public class InheritanceSimple extends CopySimple {
	
	public InheritanceSimple () {
		super();
	}
	
	
	@Override
	public List<String> getSupportedAnnotations() {
		return Arrays.asList(Annotations.TYPE_OBJECT_STATIC_INSTANCE);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean apply(EModelElement em, Resource multilevelResource) {
		super.apply(em, multilevelResource);
		
		if (!(em instanceof EClass))
			return false;
		
		EClass eci = (EClass) em;
		
		// Get superclass, labelled "type"
		for (EClass ect : eci.getEAllSuperTypes()) {
			for (EAnnotation ean : ect.getEAnnotations()) {
				if (ean.getSource().equals(Annotations.TYPE_OBJECT_STATIC_TYPE)) {
					// If the pattern is found, create instance node
					EObject instEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.clabjectEC);
					instEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("name"), eci.getName());
					if (eci.isAbstract()) {
						instEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("isAbstract"), true);
					}
		        	addPotency(instEO, 1, 1, 1);

		        	// Create type node if not previously created
		        	EObject typeEO = ElementRegistry.getInstance().getMultilevelClass(ect);
		        	if (null == typeEO) {
		        		typeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.clabjectEC);
		        		typeEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("name"), ect.getName());
						if (ect.isAbstract()) {
							typeEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("isAbstract"), true);
						}
		        		addPotency(typeEO, 1, 1, 2);
		        	}
		        	
		        	// Link instance to its type
		        	instEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("metaType"), typeEO);
		        	
					// Copy type attributes
			        for (EAttribute ea : ect.getEAttributes()) {
			        	EObject attributeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.attributeEC);
			        	attributeEO.eSet(common.Constants.attributeEC.getEStructuralFeature("name"), ea.getName());
			        	addPotency(attributeEO, 1, 1, 1);
			        	addCardinality(attributeEO, ea.getLowerBound(), ea.getUpperBound(), 1);
						addDataType(attributeEO, ea.getEAttributeType());
			        	EList<EObject> features = (EList<EObject>) typeEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
			        	features.add(attributeEO);
			        }

					// Copy instance attributes
			        for (EAttribute ea : eci.getEAttributes()) {
			        	EObject attributeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.attributeEC);
			        	attributeEO.eSet(common.Constants.attributeEC.getEStructuralFeature("name"), eci.getName().toLowerCase().charAt(0) + ea.getName());
			        	addCardinality(attributeEO, ea.getLowerBound(), ea.getUpperBound(), 1);
			        	addPotency(attributeEO, 1, 1, 1);
						addDataType(attributeEO, ea.getEAttributeType());
			        	EList<EObject> features = (EList<EObject>) typeEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
			        	features.add(attributeEO);
			        }
			        
					// Add to model
			        EList<EObject> models = (EList<EObject>) multilevelResource.getContents().get(0).eGet(common.Constants.hierarchyEC.getEStructuralFeature("models"));
			        for (EObject eom : models) {
			        	String name = (String) eom.eGet(common.Constants.clabjectEC.getEStructuralFeature("name"));
		        		EList<EObject> elements = (EList<EObject>) eom.eGet(common.Constants.modelEC.getEStructuralFeature("elements"));
			        	if (name.equals("Top")) {
			        		elements.add(typeEO);
			        		ElementRegistry.getInstance().registerMultilevelClass(ect, typeEO);
			        	} else if (name.equals("Bottom")) {
			        		elements.add(instEO);
			        		ElementRegistry.getInstance().registerMultilevelClass(eci, instEO);
			        	}
			        }
			        
			        return true;
				}
			}
		}
		
		return false;
	}
	
}
