package transformer.transformations;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import transformer.Transformation;

public abstract class CopySimple implements Transformation {
	
	@Override
	public boolean apply(EModelElement em, Resource multilevelResource) {		
		return true;
	}
	

	protected void addPotency (EObject eo, int start, int end, int depth) {
		EObject potencyEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.potencyEC);
		potencyEO.eSet(common.Constants.potencyEC.getEStructuralFeature("start"), start);
		potencyEO.eSet(common.Constants.potencyEC.getEStructuralFeature("end"), end);
		potencyEO.eSet(common.Constants.potencyEC.getEStructuralFeature("depth"), depth);
		
		eo.eSet(eo.eClass().getEStructuralFeature("potency"), potencyEO);
	}

	
	// Potency is single-valued since always depth == 1 and start == end
	@SuppressWarnings("unchecked")
	protected void addCardinality (EObject eo, int min, int max, int potency) {
		EObject cardinalityEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.cardinalityEC);
		cardinalityEO.eSet(common.Constants.cardinalityEC.getEStructuralFeature("min"), min);
		cardinalityEO.eSet(common.Constants.cardinalityEC.getEStructuralFeature("max"), max);
		addPotency(cardinalityEO, potency, potency, 1);
		
		EList<EObject> cardinalities = (EList<EObject>) eo.eGet(eo.eClass().getEStructuralFeature("cardinality"));
		cardinalities.add(cardinalityEO);
	}
	
	
	protected void addDataType (EObject attributeEO, EDataType originalDataType) {
		EAttribute dataTypeAttribute = (EAttribute) common.Constants.attributeEC.getEStructuralFeature("type");
		EEnum dataTypeEEnum = (EEnum) dataTypeAttribute.getEType();
		String dataTypeString = "";
		String dataTypeName = originalDataType.getName();
		
		// In case of custom data types in a different ePackage, try to
		// transform it by name to one of the native, supported EMF data types
		if (null==dataTypeName) {
			String[] fragments = originalDataType.toString().split("[\\/#]");
			if (0==fragments.length)
				dataTypeName = "EString";
			else
				dataTypeName = fragments[fragments.length - 1].toLowerCase();
			
			if (dataTypeName.contains("string"))
				dataTypeName = "EString";
			if (dataTypeName.contains("float") || dataTypeName.contains("double") || dataTypeName.contains("long"))
				dataTypeName = "EFloat";
			if (dataTypeName.contains("bool"))
				dataTypeName = "EBoolean";
			if (dataTypeName.contains("int"))
				dataTypeName = "EInt";
		}
		
		switch (dataTypeName) {
		case "EBoolean":
			dataTypeString = "bool";
			break;
		case "EInt":
			dataTypeString = "int";
			break;
		case "EFloat":
		case "EDouble":
		case "ELong":
			dataTypeString = "float";
			break;
		case "EString":
		default:
			dataTypeString = "string";
		}
			
		attributeEO.eSet(dataTypeAttribute, dataTypeEEnum.getEEnumLiteral(dataTypeString));
	}
	
}
