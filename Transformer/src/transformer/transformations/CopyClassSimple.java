package transformer.transformations;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import common.Annotations;
import transformer.ElementRegistry;

public class CopyClassSimple extends CopySimple {

	public CopyClassSimple () {
		super();
	}
	
	
	@Override
	public List<String> getSupportedAnnotations () {
		return Arrays.asList(Annotations.DEFAULT_CLASS);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean apply(EModelElement em, Resource multilevelResource) {
		super.apply(em, multilevelResource);
		
		EClass ec = (EClass) em;
		
		// Translate into clabject
		EObject clabjectEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.clabjectEC);
		clabjectEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("name"), ec.getName());
		if (ec.isAbstract()) {
			clabjectEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("isAbstract"), true);
		}
		
		
		// Copy attributes
        for (EAttribute ea : ec.getEAttributes()) {
        	EObject attributeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.attributeEC);
        	attributeEO.eSet(common.Constants.attributeEC.getEStructuralFeature("name"), ea.getName());
        	addCardinality(attributeEO, ea.getLowerBound(), ea.getUpperBound(), 1);
        	addPotency(attributeEO, 1, 1, 1);
			addDataType(attributeEO, ea.getEAttributeType());
        	EList<EObject> features = (EList<EObject>) clabjectEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
        	features.add(attributeEO);
        }

        for (EReference er : ec.getEReferences()) {
        	EObject relatedEO = ElementRegistry.getInstance().getMultilevelClass((EClass) er.getEType());
        	// If a related element has already been transformed, copy its potency and add to same level
        	if (null != relatedEO) {
        		EObject potencyEO = EcoreUtil.copy((EObject) relatedEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("potency")));
        		clabjectEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("potency"), potencyEO);
        		EList<EObject> elements = (EList<EObject>) relatedEO.eContainer().eGet(common.Constants.modelEC.getEStructuralFeature("elements"));
        		elements.add(clabjectEO);
        		ElementRegistry.getInstance().registerMultilevelClass(ec, clabjectEO);
        		
        		return true;
        	}
        }

		// Add to model, to default level (top) with default potency
		addPotency(clabjectEO, 1, 1, 2);
        EList<EObject> models = (EList<EObject>) multilevelResource.getContents().get(0).eGet(common.Constants.hierarchyEC.getEStructuralFeature("models"));
        for (EObject eom : models) {
        	if (eom.eGet(common.Constants.modelEC.getEStructuralFeature("name")).equals("Top")) {
        		EList<EObject> elements = (EList<EObject>) eom.eGet(common.Constants.modelEC.getEStructuralFeature("elements"));
        		elements.add(clabjectEO);
        		ElementRegistry.getInstance().registerMultilevelClass(ec, clabjectEO);
        	}
        }
        
        return true;
	}

}
