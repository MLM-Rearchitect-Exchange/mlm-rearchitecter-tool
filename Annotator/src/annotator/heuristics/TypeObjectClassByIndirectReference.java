package annotator.heuristics;

import java.util.Arrays;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

import annotator.Heuristic;

public class TypeObjectClassByIndirectReference extends TypeObjectClass {
		
	@Override
	public boolean apply (EModelElement em) {
		if (!(em instanceof EClass))
			return false;

		EClass ecs = (EClass) em;
		EClass instanceEC = null;
		EClass typeEC = null;
		EReference typingER = null;
		EPackage ep = ecs.getEPackage();
		int maxConfidence = 0;

		EList<EClass> superTypeList = ecs.getEAllSuperTypes();

		for (EClass stec : superTypeList) {
			for (EReference er : stec.getEReferences()) {
				if (!(er.getEType() instanceof EClass))
					continue;
				
				if (superTypeList.contains(er.getEType()))
					continue;

				EClass ect = (EClass) er.getEType();
				for (EClassifier e : ep.getEClassifiers()) {
					if (!(e instanceof EClass) || EcoreUtil.equals(e, ecs))
						continue;

					EClass ecti = (EClass) e;
					if (!ect.isSuperTypeOf(ecti) || EcoreUtil.equals(ect, ecti))
						continue;

					int confidence = satisfiesPattern(ecs, ecti, er);
					if(confidence > maxConfidence) {
						maxConfidence = confidence;
						instanceEC = ecs;
						typeEC = ecti;
						typingER = er;
					}
				}
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

}
