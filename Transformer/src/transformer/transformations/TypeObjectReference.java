package transformer.transformations;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;

import common.Annotations;
import transformer.ElementRegistry;

public class TypeObjectReference extends CopySimple {
	
	public TypeObjectReference () {
		super();
	}
	
	
	@Override
	public List<String> getSupportedAnnotations() {
		return Arrays.asList(Annotations.TYPE_OBJECT_EXPLICIT_INSTANCE);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean apply(EModelElement em, Resource multilevelResource) {
		super.apply(em, multilevelResource);

		if (!(em instanceof EReferenceImpl))
			return false;

		EReference referenceInstanceER = (EReference) em;
		EReference referenceTypeER = (EReference) referenceInstanceER.getEAnnotation("instance").getReferences().get(0);
		
		if (null==referenceTypeER)
			return false;
		
		EClass sourceInstanceEC = referenceInstanceER.getEContainingClass();
		EClass targetInstanceEC = referenceInstanceER.getEReferenceType();
		EObject sourceInstanceEO = ElementRegistry.getInstance().getMultilevelClass(sourceInstanceEC);
		EObject targetInstanceEO = ElementRegistry.getInstance().getMultilevelClass(targetInstanceEC);
		
		if (null == sourceInstanceEO || null == targetInstanceEO) {
			return false;
		}

		if ((null==sourceInstanceEC.getEAnnotation("instance")) && (null==targetInstanceEC.getEAnnotation("instance")))
			return false;
		
		EClass sourceTypeEC = referenceTypeER.getEContainingClass();
		EClass targetTypeEC = referenceTypeER.getEReferenceType();
		EObject sourceTypeEO = ElementRegistry.getInstance().getMultilevelClass(sourceTypeEC);
		EObject targetTypeEO = ElementRegistry.getInstance().getMultilevelClass(targetTypeEC);

		if (null == sourceTypeEO || null == targetTypeEO) {
			return false;
		}

		if ((null==sourceTypeEC.getEAnnotation("type")) && (null==targetTypeEC.getEAnnotation("type")))
			return false;
		
		// Create type reference
		EObject referenceTypeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.referenceEC);
		addPotency(referenceTypeEO, 1, 1, 2);
		addCardinality(referenceTypeEO, referenceTypeER.getLowerBound(), referenceTypeER.getUpperBound(), 1);
		referenceTypeEO.eSet(common.Constants.referenceEC.getEStructuralFeature("name"), referenceTypeER.getName());
		referenceTypeEO.eSet(common.Constants.referenceEC.getEStructuralFeature("target"), targetTypeEO);

		// Add to the source (container) clabject and the registry
		EList<EObject> features = (EList<EObject>) sourceTypeEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
		features.add(referenceTypeEO);
		ElementRegistry.getInstance().registerMultilevelReference(referenceTypeER, referenceTypeEO);
		
		// If the source/target and its type have not "collapsed" into one node, we create both references
		if ((EcoreUtil.equals(sourceTypeEO, sourceInstanceEO)) && (EcoreUtil.equals(targetTypeEO, targetInstanceEO))) {
			ElementRegistry.getInstance().registerMultilevelReference(referenceInstanceER, referenceTypeEO);
			addCardinality(referenceTypeEO, referenceInstanceER.getLowerBound(), referenceInstanceER.getUpperBound(), 2);
		} else {
			// Create instance reference
			EObject referenceInstanceEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.referenceEC);
			addPotency(referenceInstanceEO, 1, 1, 1);
			addCardinality(referenceInstanceEO, referenceInstanceER.getLowerBound(), referenceInstanceER.getUpperBound(), 1);
			referenceInstanceEO.eSet(common.Constants.referenceEC.getEStructuralFeature("name"), referenceInstanceER.getName());
			referenceInstanceEO.eSet(common.Constants.referenceEC.getEStructuralFeature("target"), targetInstanceEO);
			referenceInstanceEO.eSet(common.Constants.referenceEC.getEStructuralFeature("metaType"), referenceTypeEO);

			// Add to the source (container) clabject and the registry
			features = (EList<EObject>) sourceInstanceEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
			features.add(referenceInstanceEO);
			ElementRegistry.getInstance().registerMultilevelReference(referenceInstanceER, referenceInstanceEO);
		}
		
		return true;

	}
}
