package transformer.transformations;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import common.Annotations;
import transformer.ElementRegistry;

public class TypeObjectClass extends CopySimple {
	
	public TypeObjectClass () {
		super();
	}
	
	
	@Override
	public List<String> getSupportedAnnotations() {
		return Arrays.asList(Annotations.TYPE_OBJECT_EXPLICIT_INSTANCE);
	}
	
	
	@Override
	public boolean apply(EModelElement em, Resource multilevelResource) {
		boolean result = super.apply(em, multilevelResource);
		
		if (!(em instanceof EClass) || !result)
			return false;
		
		EClass eci = (EClass) em;
		EList<EAnnotation> instanceEAnnotations = new BasicEList<>();
		for (EAnnotation ea : eci.getEAnnotations()) {
			if (ea.getSource().equals("instance") && (Integer.parseInt(ea.getDetails().get("confidence"))>=common.Constants.CONFIDENCE_THRESHOLD)) {
				instanceEAnnotations.add(ea);
			}
		}
		boolean multiType = instanceEAnnotations.size() > 1;
		for (EAnnotation ea : instanceEAnnotations) {
			EList<EObject> references = ea.getReferences();
			if (references.size() == 1) {
				result &= createElement(multilevelResource, eci, (EClass) references.get(0), multiType);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private boolean createElement(Resource multilevelResource, EClass eci, EClass ect, boolean multiType) {
		EObject typeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.clabjectEC);
		typeEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("name"), ect.getName());
		if (eci.isAbstract() || ect.isAbstract()) {
			typeEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("isAbstract"), true);
		}
		typeEO.eSet(common.Constants.clabjectEC.getEStructuralFeature("isMultiType"), multiType);
		addPotency(typeEO, 1, 1, 2);
		EList<EObject> features = (EList<EObject>) typeEO.eGet(common.Constants.clabjectEC.getEStructuralFeature("features"));
		
		// Copy type attributes
		for (EAttribute ea : ect.getEAttributes()) {
			EObject attributeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.attributeEC);
			attributeEO.eSet(common.Constants.attributeEC.getEStructuralFeature("name"), ea.getName());
			addCardinality(attributeEO, ea.getLowerBound(), ea.getUpperBound(), 1);
			addPotency(attributeEO, 1, 1, 1);
			addDataType(attributeEO, ea.getEAttributeType());
			features.add(attributeEO);
		}

		// Copy instance attributes (with increased potency)
		for (EAttribute ea : eci.getEAttributes()) {
			boolean duplicated = false;
			for (EObject f : features) {
				if (f.eClass().equals(common.Constants.attributeEC)) {
					if (f.eGet(common.Constants.attributeEC.getEStructuralFeature("name")).equals(ea.getName())) {
						EObject potency = (EObject) f.eGet(common.Constants.attributeEC.getEStructuralFeature("potency"));
						potency.eSet(common.Constants.potencyEC.getEStructuralFeature("end"), 2);
						addCardinality(f, ea.getLowerBound(), ea.getUpperBound(), 2);
						duplicated = true;
						break;
					}
				}
			}
			if (!duplicated) {
				EObject attributeEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.attributeEC);
				attributeEO.eSet(common.Constants.attributeEC.getEStructuralFeature("name"), ea.getName());
				addCardinality(attributeEO, ea.getLowerBound(), ea.getUpperBound(), 2);
				addPotency(attributeEO, 2, 2, 1);
				addDataType(attributeEO, ea.getEAttributeType());
				features.add(attributeEO);
			}
		}
		
		// Add to model
		EList<EObject> models = (EList<EObject>) multilevelResource.getContents().get(0).eGet(common.Constants.hierarchyEC.getEStructuralFeature("models"));
		for (EObject eom : models) {
			if (eom.eGet(common.Constants.modelEC.getEStructuralFeature("name")).equals("Top")) {
				EList<EObject> elements = (EList<EObject>) eom.eGet(common.Constants.modelEC.getEStructuralFeature("elements"));
				elements.add(typeEO);
				ElementRegistry.getInstance().registerMultilevelClass(eci, typeEO);
				ElementRegistry.getInstance().registerMultilevelClass(ect, typeEO);
			}
		}
		
		return true;
	}

}
