package common;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;

import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;
import net.ricecode.similarity.StringSimilarityService;
import net.ricecode.similarity.StringSimilarityServiceImpl;

public abstract class Utils {
	
	public static URI getAbsoluteURI(String relativeMMURIString, Class<?> callingClass) {
		String activeFolderPath = "";
		try {
			activeFolderPath = callingClass.getProtectionDomain().getCodeSource().getLocation().toURI().normalize().getRawPath(); // Get absolute URI to running folder
		} catch (URISyntaxException e) {
			System.err.println("The current path \"" + activeFolderPath + "\" is malformed");
			e.printStackTrace();
			return null;
		}
		URI absoluteMMURI = URI.createFileURI(activeFolderPath);
		if (absoluteMMURI.segment(absoluteMMURI.segmentCount()-1).equals("")) {
			absoluteMMURI = URI.createURI(relativeMMURIString).resolve(absoluteMMURI.trimSegments(1)); // Delete trailing separator and go up one folder
		} else {
			absoluteMMURI = URI.createURI(relativeMMURIString).resolve(absoluteMMURI); // Go up one folder 
		}
		
		String filePath = absoluteMMURI.toFileString(); // Get path as absolute URI
		if (!(new File(filePath)).exists()) {
			System.out.println("File \"" + filePath + "\" does not exist");
			return null;
		}
		return absoluteMMURI;
	}
	
	
	// Get the most suitable EPackage to be rearchitected, based on size
	public static EPackage getRelevantEPackage(Resource resource) {
		int maxSize = 0;
		EPackage relevantEPackage = null;
		for (EObject eo : resource.getContents()) {
			if (!(eo instanceof EPackage))
				continue;

			EPackage ep = (EPackage) eo;
			// If there are several (sub)EPackages, select the biggest one
			ep = biggestSubEPackage(ep);
			int size = ep.getEClassifiers().size(); 
			if (size > maxSize) {
				maxSize = size;
				relevantEPackage = ep;
			}
		}
		return relevantEPackage;
	}
	
	
	// Returns the biggest subEPackage, or currentEPackage if it is bigger than all of them
	private static EPackage biggestSubEPackage(EPackage currentEPackage) {
		EPackage biggestEPackage = currentEPackage;
		int maxSize = currentEPackage.getEClassifiers().size();
		for (EPackage ep : currentEPackage.getESubpackages()) {
			int size = ep.getEClassifiers().size(); 
			if (size > maxSize) {
				maxSize = size;
				biggestEPackage = ep;
			}
		}
		return biggestEPackage;
	}
	
	
	public static Set<EClass> getSubEClasses(EClass parentEClass, boolean transitive) {
		Set<EClass> subEClasses = new HashSet<EClass>();
		TreeIterator<EObject> foo = parentEClass.getEPackage().eAllContents();
		while (foo.hasNext()) {
			EObject eo = foo.next();
			if (!(eo instanceof EClass))
				continue;
			EClass ec = (EClass) eo;
			if (transitive) {
				if (ec.getEAllSuperTypes().contains(parentEClass))
					subEClasses.add(ec);
			} else {
				if (ec.getESuperTypes().contains(parentEClass))
					subEClasses.add(ec);				
			}
		}
		return subEClasses;
	}
	
	
	public static double stringSimilarity(String string1, String string2) {
		SimilarityStrategy strategy = new JaroWinklerStrategy();
		StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
		return service.score(string1, string2);
	}
	
	
	public static Object getValueOfFeature(EObject eObject, String structuralFeatureName) {
		return eObject.eGet(eObject.eClass().getEStructuralFeature(structuralFeatureName));
	}
	
	
	@SuppressWarnings("unchecked")
	public static void setValueOfFeature(EObject eObject, String structuralFeatureName, Object value) {
		EStructuralFeature eStructuralFeature = eObject.eClass().getEStructuralFeature(structuralFeatureName);
		if (eStructuralFeature.isMany()) {
			EList<Object> valuesList = (EList<Object>) eObject.eGet(eStructuralFeature);
			valuesList.add(value);
		} else {
			eObject.eSet(eStructuralFeature, value);
		}
	}
	
}
