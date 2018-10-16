package annotator.heuristics;

//import java.util.function.Predicate;

import java.util.Arrays;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import annotator.Heuristic;

public class TypeObjectClassByReference extends TypeObjectClass {
		
	@Override
	public boolean apply (EModelElement em) {
		if (!(em instanceof EClass))
			return false;

		EClass ecs = (EClass) em;
		EClass instanceEC = null;
		EClass typeEC = null;
		EReference typingER = null;
		int maxConfidence = 0;
		for (EReference er : ecs.getEReferences()) {
			EClassifier eType = er.getEType();
			if (!(eType instanceof EClass) || EcoreUtil.equals(eType, ecs))
				continue;

			EClass ect = (EClass) eType;
			int confidence = satisfiesPattern(ecs, ect, er);

			if(confidence > maxConfidence) {
				maxConfidence = confidence;
				instanceEC = ecs;
				typeEC = ect;
				typingER = er;
			}
		}

		if ((maxConfidence==0) || (null==instanceEC) || (null==typeEC) || (null==typingER))
			return false;
		
		String confidenceString = Integer.toString(maxConfidence);
		EAnnotation ea = Heuristic.addEAnnotation(instanceEC, "instance", Arrays.asList(typeEC));
		ea.getDetails().put("confidence", confidenceString);
		ea = Heuristic.addEAnnotation(typeEC, "type", Arrays.asList(instanceEC));
		ea.getDetails().put("confidence", confidenceString);
		ea = Heuristic.addEAnnotation(typingER, "typing", null);
		ea.getDetails().put("confidence", confidenceString);
		return true;
	}
	
	
	@Override
	protected int satisfiesPattern (EClass source, EClass target, EReference ref) {
		if (!(ref.eContainer().equals(source) && ref.getEType().equals(target)))
			return 0;
		
		return super.satisfiesPattern(source, target, ref);
	}	

}
