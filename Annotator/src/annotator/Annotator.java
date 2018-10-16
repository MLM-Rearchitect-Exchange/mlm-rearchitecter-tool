package annotator;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;

import common.Debugger;

public class Annotator {
	
	public Annotator() {
		// Initialise
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
	}
	
	
	public void annotate (URI originalMMURI) {
		System.out.println("------ ANNOTATOR ------");
		System.out.println("FILE: " + originalMMURI.toFileString());
		System.out.println("-----------------------");
		
		// Load original Ecore metamodel from file
		Resource originalMMResource = common.Constants.resourceSet.getResource(originalMMURI, true);
		
		// Create output Ecore, which will have the annotations, by cloning the input one
		URI annotatedMMURI = originalMMURI.trimSegments(1).appendSegment("generated").appendSegment(originalMMURI.lastSegment().toString());
		Resource annotatedMMResource = common.Constants.resourceSet.createResource(annotatedMMURI);
		
		EPackage originalEPackage = common.Utils.getRelevantEPackage(originalMMResource);
		if (null == originalEPackage) {
			System.err.println("Could not find a valid EPackage in " + originalMMURI.toFileString());
			return;
		}
		
		EPackage copyEPackage = EcoreUtil.copy(originalEPackage);
		EList<EClassifier> elements = copyEPackage.getEClassifiers();
		
		// For each EClass, apply all possible heuristics
		for (EClassifier e : elements) {
			if (!(e instanceof EClass))
				break;
			
			EClass ec = (EClass) e;
			for (Heuristic h : Constants.HEURISTIC_REGISTRY) {
				if (h.apply(ec)) {
					Debugger.log("Applied heuristic " + h.getClass().getSimpleName() + " to element " + ec.getName());
				}
			}
		}
		
		// For each EReference of each EClass, apply all possible heuristics
		for (EClassifier e : elements) {
			if (!(e instanceof EClass))
				break;
			
			EClass ec = (EClass) e;
			for (EReference er : ec.getEReferences()) {
				for (Heuristic h : Constants.HEURISTIC_REGISTRY) {
					if (h.apply(er)) {
						Debugger.log("Applied heuristic " + h.getClass().getSimpleName() + " to element " + er.getName());
					}
				}
			}
		}
		
		// Save generated Ecore metamodel with the annotated elements
		annotatedMMResource.getContents().add(copyEPackage);
		Map<String, Boolean> options = new HashMap<String, Boolean>();
		options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
		try {
			annotatedMMResource.save(options);
		} catch (IOException e1) {
			System.err.println("Error saving model");
			e1.printStackTrace();
		}
	}
	
	
	public static boolean init () {
		// Load all heuristics in the "heuristics" subpackage
		List<Class<? extends Heuristic>> heuristicClasses = new ArrayList<>();
		new FastClasspathScanner(Heuristic.class.getPackage().getName() + ".heuristics").matchClassesImplementing(Heuristic.class, heuristicClasses::add).scan();
		for (Class<? extends Heuristic> c : heuristicClasses) {
			try {
				if (!Modifier.isAbstract(c.getModifiers())) {
					Constants.HEURISTIC_REGISTRY.add(c.newInstance());
					Debugger.log("Registering heuristic " + c.getSimpleName());
				}
			} catch (Exception e) {
				System.err.println("Error during registration of heuristics");
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
}
