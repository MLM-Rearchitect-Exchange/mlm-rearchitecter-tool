package transformer.transformations;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import common.Annotations;
import transformer.ElementRegistry;

public class CopyReferenceSimple extends CopySimple {
	
	public CopyReferenceSimple () {
		super();
	}
	
	
	@Override
	public List<String> getSupportedAnnotations() {
		return Arrays.asList(Annotations.DEFAULT_REFERENCE);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean apply(EModelElement em, Resource multilevelResource) {
		super.apply(em, multilevelResource);
		
		if (!(em instanceof EReferenceImpl))
			return false;
		
		EReferenceImpl er = (EReferenceImpl) em;
		
		// Source and target clabjects should already exist. If not, abort
		EObject sourceEO = ElementRegistry.getInstance().getMultilevelClass(er.getEContainingClass());
		EObject targetEO = ElementRegistry.getInstance().getMultilevelClass(er.getEReferenceType());
		if (null == sourceEO || null == targetEO) {
			return false;
		}
		
		// Create reference
		EObject referenceEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.referenceEC);
		if (null == er.getEContainingClass().getEAnnotation("type")) {
			addPotency(referenceEO, 2, 2, 1);
			addCardinality(referenceEO, er.getLowerBound(), er.getUpperBound(), 2);
		} else {
			EObject potencyEO = EcoreUtil.copy((EObject) sourceEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("potency")));
			referenceEO.eSet(common.Constants.referenceEC.getEStructuralFeature("potency"), potencyEO);
			addCardinality(referenceEO, er.getLowerBound(), er.getUpperBound(), (int) potencyEO.eGet(common.Constants.potencyEC.getEStructuralFeature("start")));
		}
		referenceEO.eSet(common.Constants.referenceEC.getEStructuralFeature("name"), er.getName());
		referenceEO.eSet(common.Constants.referenceEC.getEStructuralFeature("target"), targetEO);
		
		// Add to the source (container) clabject and the registry
		EList<EObject> features = (EList<EObject>) sourceEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
		features.add(referenceEO);
		ElementRegistry.getInstance().registerMultilevelReference(er, referenceEO);
		
        return true;
	}
	
}
