package annotator.heuristics;

import java.util.Arrays;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import annotator.Heuristic;

public class TypeObjectReferenceSameSource extends TypeObjectReference {
	
	@Override
	public boolean apply(EModelElement em) {
		if (!(em instanceof EReference))
			return false;
		
		EReference referenceInstanceER = (EReference) em;
		
		EClass sourceInstanceEC = referenceInstanceER.getEContainingClass();
		if (null == sourceInstanceEC)
			return false;
		
		if (!(referenceInstanceER.getEType() instanceof EClass))
			return false;
		
		EClass targetInstanceEC = ((EClass) referenceInstanceER.getEType());
		if ((null == targetInstanceEC) || (targetInstanceEC.equals(sourceInstanceEC)))
			return false;
		
		EAnnotation targetInstanceEAnn = targetInstanceEC.getEAnnotation("instance");
		if (null == targetInstanceEAnn)
			return false;

		EClass targetTypeEC = (EClass) targetInstanceEAnn.getReferences().get(0);
		if ((null == targetTypeEC) || (targetTypeEC.equals(sourceInstanceEC)) || (null == targetTypeEC.getEAnnotation("type")))
			return false;

		EReference referenceTypeER = null;
		int maxConfidence = 0;
		for (EReference er : sourceInstanceEC.getEReferences()) {
			if (!EcoreUtil.equals(er.getEType(), targetTypeEC))
				continue;
			
			int confidence = satisfiesPattern(referenceInstanceER, er);
			if(confidence > maxConfidence) {
				referenceTypeER = er;
				maxConfidence = confidence;					
			}
		}

		maxConfidence = Math.min(maxConfidence, Integer.parseInt(targetInstanceEAnn.getDetails().get("confidence")));
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
