package transformer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import com.opencsv.CSVWriter;

import common.Debugger;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

public class Transformer {

	private URI multilevelURI;
	private EPackage twoLevelEPackage;
	
	private Map<Transformation, Integer> transformationOccurrences;

	static CSVWriter writer;
	static final String[] csvHeaders = {
		"Model",
		"Two-level classes",
		"Two-level references",
		"Two-level attributes",
		"Two-level total",
		"Mutilevel classes",
		"Mutilevel references",
		"Mutilevel attributes",
		"Mutilevel total",
		"Reduction for classes (%)",
		"Reduction for references (%)",
		"Reduction for attributes (%)",
		"Reduction for total (%)"
	};
	
	
	public Transformer() {    	
		// Initialise
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		
		// Clean element registry and initialise transformation occurrences registry
		ElementRegistry.getInstance().clean();
		transformationOccurrences = new HashMap<Transformation, Integer>();
	}

	
	@SuppressWarnings("unchecked")
	public Map<Transformation, Integer> transform(URI twoLevelMMURI) {
		System.out.println("----- TRANSFORMER -----");
		System.out.println("FILE: " + twoLevelMMURI.toFileString());
		System.out.println("-----------------------");
		
		// Read elements from input two-level model
		Resource twoLevelResource = common.Constants.resourceSet.getResource(twoLevelMMURI, true);
		twoLevelEPackage = (EPackage) twoLevelResource.getContents().get(0);
		EList<EClassifier> elements = twoLevelEPackage.getEClassifiers();

		multilevelURI = twoLevelMMURI.trimFileExtension().appendFileExtension("xmi");
		Resource multilevelResource = common.Constants.resourceSet.createResource(multilevelURI);

		// Create the hierarchy and two levels inside
		EObject hierarchyEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.hierarchyEC);
		hierarchyEO.eSet(common.Constants.hierarchyEC.getEStructuralFeature("name"), twoLevelMMURI.trimFileExtension().lastSegment());
		EObject modelEOT = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.modelEC); // Top model
		modelEOT.eSet(common.Constants.modelEC.getEStructuralFeature("name"), "Top");
		EObject potencyEO = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.potencyEC);
		potencyEO.eSet(common.Constants.potencyEC.getEStructuralFeature("depth"), 2);
		modelEOT.eSet(common.Constants.modelEC.getEStructuralFeature("potency"), potencyEO);
		EObject modelEOB = common.Constants.multilevelEPackage.getEFactoryInstance().create(common.Constants.modelEC); // Bottom model
		modelEOB.eSet(common.Constants.modelEC.getEStructuralFeature("name"), "Bottom");
		modelEOB.eSet(common.Constants.modelEC.getEStructuralFeature("parent"), modelEOT);
		EList<EObject> models = (EList<EObject>) hierarchyEO.eGet(common.Constants.hierarchyEC.getEStructuralFeature("models"));
		models.add(modelEOT);
		models.add(modelEOB);
		multilevelResource.getContents().add(hierarchyEO);

		// Find EAnnotations and apply transformation for each EClass
		for (EClassifier e : elements) {
			if (!(e instanceof EClass))
				continue;
			
			EClass ec = (EClass) e;
			
			// Filter EAnnotations that do not reach the threshold
			EList<EAnnotation> eAnnotations = new BasicEList<EAnnotation>(ec.getEAnnotations());
			for (EAnnotation ea : eAnnotations) {
				if(Integer.parseInt(ea.getDetails().get("confidence"))<common.Constants.CONFIDENCE_THRESHOLD) {
					ec.getEAnnotations().remove(ea);
				}
			}
			
			if(ec.getEAnnotations().size() == 0) { // Default transformation if no EAnnotations
				for (Transformation t : Constants.TRANSFORMATION_REGISTRY.get("classDefault")) {
					if(t.apply(ec, multilevelResource)) {
						Debugger.log("Applied default transformation to element " + ec.getName());
						registerOccurence(t);
					}
				}
			} else {
				for (EAnnotation ea : ec.getEAnnotations()) {
					Collection<Transformation> tranformations = Constants.TRANSFORMATION_REGISTRY.get(ea.getSource());
					if ((tranformations != null) && (Integer.parseInt(ea.getDetails().get("confidence"))>=common.Constants.CONFIDENCE_THRESHOLD)) {
						for (Transformation t : tranformations) {
							if(t.apply(ec, multilevelResource)) {
								Debugger.log("Applied transformation " + t.getClass().getSimpleName() + " to element " + ec.getName());
								registerOccurence(t);
							}
						}
					}
				}
			}
		}
		
		// Find EAnnotations and apply transformation for each EReference
		for (EClassifier e : elements) {
			if (!(e instanceof EClass))
				continue;

			EClass ec = (EClass) e;
			for (EReference er : ec.getEReferences()) {
				
				// Filter EAnnotations that do not reach the threshold
				EList<EAnnotation> eAnnotations = new BasicEList<EAnnotation>(er.getEAnnotations());
				for (EAnnotation ea : eAnnotations) {
					if(Integer.parseInt(ea.getDetails().get("confidence"))<common.Constants.CONFIDENCE_THRESHOLD) {
						er.getEAnnotations().remove(ea);
					}
				}
				
				if(er.getEAnnotations().size()==0) {  // Default transformation if no EAnnotations
					for (Transformation t : Constants.TRANSFORMATION_REGISTRY.get("referenceDefault")) {
						if(t.apply(er, multilevelResource)) {
							Debugger.log("Applied default transformation to reference " + er.getName());
							registerOccurence(t);
						}
					}
				} else {
					for (EAnnotation ea : er.getEAnnotations()) {
						Collection<Transformation> transformations = Constants.TRANSFORMATION_REGISTRY.get(ea.getSource());
						if ((transformations != null)) {
							for (Transformation t : transformations) {
								if(t.apply(er, multilevelResource)) {
									Debugger.log("Applied transformation " + t.getClass().getSimpleName() + " to element " + er.getName());
									registerOccurence(t);
								}
							}
						}
					}
				}
			}

		}
		
		
		// Set inheritance relations between classes on the generated elements
		for (EClassifier e : elements) {
			if (!(e instanceof EClass))
				continue;
			
			EClass ec = (EClass) e;
			EObject eo = ElementRegistry.getInstance().getMultilevelClass(ec);
			EList<EObject> superTypes = (EList<EObject>) eo.eGet(common.Constants.clabjectEC.getEStructuralFeature("superTypes"));
			for (EClass sec : ec.getESuperTypes()) {
				EObject seo = ElementRegistry.getInstance().getMultilevelClass(sec);
				superTypes.add(seo);
			}
			
		}
			
		// Save generated model with the multilevel hierarchy
		Map<String, Boolean> options = new HashMap<String, Boolean>();
		options.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
		try {
			multilevelResource.save(options);
		} catch (IOException e1) {
			System.err.println("Error saving model");
			e1.printStackTrace();
		}
		
		return transformationOccurrences;
	}
	

	private void registerOccurence(Transformation h) {
		Integer occurences = transformationOccurrences.get(h);
		if (null == occurences) {
			transformationOccurrences.put(h, new Integer(1));
		} else {
			transformationOccurrences.put(h, new Integer(occurences.intValue() + 1));
		}
	}
	
	
	public Metrics getMetrics() {
		if (!(new File(multilevelURI.toFileString())).exists())
			return null;
		
		EList<EClassifier> twoLevelElements = twoLevelEPackage.getEClassifiers();

		int twoLevelClasses = 0;
		int twoLevelReferences = 0;
		int twoLevelAttributes = 0;
		
		for (EClassifier e : twoLevelElements) {
			if (e instanceof EClass) {
				EClass ec = (EClass) e;
				twoLevelClasses++;
				twoLevelReferences += ec.getEReferences().size();
				twoLevelAttributes += ec.getEAttributes().size();
			}
		}
		
		int multilevelClasses = ElementRegistry.getInstance().getClassAmount();
		int multilevelReferences = ElementRegistry.getInstance().getReferenceAmount();
		int multilevelAttributes = ElementRegistry.getInstance().getAttributeAmount();
		
		Metrics metrics = new Metrics();
		metrics.setTwoLevelClasses(twoLevelClasses);
		metrics.setTwoLevelAttributes(twoLevelAttributes);
		metrics.setTwoLevelReferences(twoLevelReferences);
		metrics.setMultilevelClasses(multilevelClasses);
		metrics.setMultilevelReferences(multilevelReferences);
		metrics.setMultilevelAttributes(multilevelAttributes);
		
		return metrics;
		
	}


	public static boolean init() {
		// Load all transformations in the "transformations" subpackage to registry
		List<Class<? extends Transformation>> transformationClasses = new ArrayList<>();
		new FastClasspathScanner(Transformation.class.getPackage().getName() + ".transformations").matchClassesImplementing(Transformation.class, transformationClasses::add).scan();
		for (Class<? extends Transformation> c : transformationClasses) {
			try {
				if (Modifier.isAbstract(c.getModifiers()))
					continue;
					
				Transformation transformation = (Transformation) c.getConstructor().newInstance();
				for (String annotation : transformation.getSupportedAnnotations()) {
					Constants.TRANSFORMATION_REGISTRY.computeIfAbsent(annotation, k -> new ArrayList<Transformation>()).add(transformation);
					Debugger.log("Registering transformation " + c.getSimpleName() + " for annotation " + annotation);
				}
			} catch (Exception e) {
				System.err.println("Error during registration of heuristics");
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}


	public static Metrics run(URI absoluteMMURI) {
		Transformer t;
		t = new Transformer();
		Map<Transformation, Integer> transformationOccurrences = t.transform(absoluteMMURI);
		if (common.Constants.DEBUGGING) {
			Debugger.log("\nTransformation occurrences:");
			for (Transformation h : transformationOccurrences.keySet()) {
				System.out.println(h.getClass().getSimpleName() + ": " + transformationOccurrences.get(h));
			}
			
		}
		Metrics m = t.getMetrics();
		return m;
	}
	
	
	public static boolean openCSV() {
		try {
			writer = new CSVWriter(new FileWriter("reduction.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		} catch (IOException e) {
			System.err.println("Error generating the CSV report");
			e.printStackTrace();
			return false;
		}
		writer.writeNext(csvHeaders);
		return true;
	}
	

	public static void writeCSVLine(String modelName, Metrics m) {
		String [] nextLine = {
				modelName,
				String.valueOf(m.getTwoLevelClasses()),
				String.valueOf(m.getTwoLevelReferences()),
				String.valueOf(m.getTwoLevelAttributes()),
				String.valueOf(m.getTwoLevelTotal()),
				String.valueOf(m.getMultilevelClasses()),
				String.valueOf(m.getMultilevelReferences()),
				String.valueOf(m.getMultilevelAttributes()),
				String.valueOf(m.getMultilevelTotal()),
				String.valueOf(m.getClassReduction()),
				String.valueOf(m.getReferenceReduction()),
				String.valueOf(m.getAttributeReduction()),
				String.valueOf(m.getTotalReduction())};
		writer.writeNext(nextLine);
	}
	

	public static boolean closeCSV() {
		try {
			writer.close();
		} catch (IOException e) {
			System.err.println("Error generating the CSV report");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
