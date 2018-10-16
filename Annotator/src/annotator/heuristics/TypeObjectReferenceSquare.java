package annotator.heuristics;

import java.util.Arrays;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import annotator.Heuristic;

public class TypeObjectReferenceSquare extends TypeObjectReference {

	@Override
	public boolean apply(EModelElement em) {
		if (!(em instanceof EReference))
			return false;
		
		EReference referenceInstanceER = (EReference) em;
		
		EAnnotation sourceInstanceEAnn = referenceInstanceER.getEContainingClass().getEAnnotation("instance");
		if (null == sourceInstanceEAnn)
			return false;

		if (!(referenceInstanceER.getEType() instanceof EClass))
			return false;
		
		EAnnotation targetInstanceEAnn = ((EClass) referenceInstanceER.getEType()).getEAnnotation("instance");
		if (null == targetInstanceEAnn)
			return false;

		EClass sourceTypeEC = (EClass) sourceInstanceEAnn.getReferences().get(0);
		if (null == sourceTypeEC)
			return false;

		EClass targetTypeEC = (EClass) targetInstanceEAnn.getReferences().get(0);
		if (null == targetTypeEC)
			return false;
		
		EReference referenceTypeER = null;
		int maxConfidence = 0;
		for (EReference er : sourceTypeEC.getEReferences()) {
			if (!EcoreUtil.equals(er.getEType(), targetTypeEC))
				continue;
				//TODO Structure fits; check names (tricky because of plurals)
			
			int confidence = satisfiesPattern(referenceInstanceER, er);
			if(confidence > maxConfidence) {
				referenceTypeER = er;
				maxConfidence = confidence;					
			}
		}

		int sourceInstanceEAnnConfidence = Integer.parseInt(sourceInstanceEAnn.getDetails().get("confidence"));
		int targetInstanceEAnnConfidence = Integer.parseInt(targetInstanceEAnn.getDetails().get("confidence"));
		maxConfidence = Math.min(maxConfidence, Math.min(sourceInstanceEAnnConfidence, targetInstanceEAnnConfidence));
		if ((maxConfidence==0) || (null==referenceTypeER))
			return false;
		
		String confidenceString = Integer.toString(maxConfidence);
		EAnnotation ea = Heuristic.addEAnnotation(referenceInstanceER, "instance", Arrays.asList(referenceTypeER));
		ea.getDetails().put("confidence", confidenceString);
		ea = Heuristic.addEAnnotation(referenceTypeER, "type", Arrays.asList(referenceInstanceER));
		ea.getDetails().put("confidence", confidenceString);
		return true;
	}

}
