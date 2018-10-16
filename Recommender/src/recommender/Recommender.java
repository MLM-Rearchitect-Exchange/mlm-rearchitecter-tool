package recommender;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import recommender.Constants.Support;

import com.opencsv.CSVWriter;
import common.Debugger;

public class Recommender {
	
	static CSVWriter writer;
	static String[] csvInitialHeaders;
	
	
	public Recommender () {
		// Initialise
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
	}

	
	// Returns the best scoring tool
	public List<Score> computeScores (URI multilevelURI) {
		System.out.println("----- RECOMMENDER -----");
		System.out.println("FILE: " + multilevelURI.toFileString());
		System.out.println("-----------------------");
		Resource multilevelResource = common.Constants.resourceSet.getResource(multilevelURI, true);
        
		List<Score> toolScores = new ArrayList<Score>();
		for (Tool t : Constants.TOOL_REGISTRY) {
			toolScores.add(new Score(t));
		}
		
		for (Concept c : Constants.CONCEPT_REGISTRY) {
			
			int appearances = c.appearances(multilevelResource);
			if (appearances > 0) {
				for (Score s : toolScores) {
					Tool t = s.getTool();
					Support support = Support.UNKNOWN;
					try {
						support = (Support) t.getClass().getDeclaredMethod ("get" + c.getClass().getSimpleName() + "Support").invoke(t);
					} catch (Exception e) {
						System.err.println("Unspecified support of " + t.getClass().getSimpleName() + " for " + c.getClass().getSimpleName() + ". Defaulting to  \"UNKNOWN\"");
					}
					switch (support) {
						case SUPPORTED:
							s.setScore(s.getScore() + appearances * 2);
						break;
						case EMULATED:
							s.setScore(s.getScore() + appearances);
						break;
						case UNSUPPORTED:
							s.setScore(s.getScore() - appearances);
							s.setUnsupported();
						break;
						case UNKNOWN:
						default:
						break;
					}
				}
			}
		}
		
		Collections.sort(toolScores);

		int i = 1;
		for (Score s : toolScores) {
			String unsupported = "";
			if (s.getUnsupported())
				unsupported = " - Unsupported features";
			Debugger.log(i + ". " + s.getTool().getClass().getSimpleName() + " (" + s.getScore() + " points)" + unsupported);
			i++;
		}
		
		return toolScores;
	}
	
	
	public static boolean init() {
		// Load all tools in the "tools" subpackage
		List<Class<? extends Tool>> toolClasses = new ArrayList<>();
		new FastClasspathScanner(Tool.class.getPackage().getName() + ".tools").matchClassesImplementing(Tool.class, toolClasses::add).scan();
		for (Class<? extends Tool> t : toolClasses) {
			try {
				Constants.TOOL_REGISTRY.add(t.newInstance());
				Debugger.log("Registering tool " + t.getSimpleName());
			} catch (Exception e) {
				System.err.println("Error during registration of tools");
				e.printStackTrace();
				return false;
			}
		}
		
		// Load all MLM concepts in the "concepts" subpackage
		List<Class<? extends Concept>> conceptClasses = new ArrayList<>();
		new FastClasspathScanner(Concept.class.getPackage().getName() + ".concepts").matchClassesImplementing(Concept.class, conceptClasses::add).scan();
		for (Class<? extends Concept> c : conceptClasses) {
			try {
				Constants.CONCEPT_REGISTRY.add(c.newInstance());
				Debugger.log("Registering MLM concept " + c.getSimpleName());
			} catch (Exception e) {
				System.err.println("Error during registration of MLM concepts");
				e.printStackTrace();
				return false;
			}
		}
		
		csvInitialHeaders = new String[Constants.TOOL_REGISTRY.size()*3 + 2];
		csvInitialHeaders[0] = "Model";
		csvInitialHeaders[1] = "Best scoring tool";
		int i = 2;
		for (Tool t : Constants.TOOL_REGISTRY) {
			String toolName = t.getClass().getSimpleName();
			csvInitialHeaders[i++] = toolName + " points";
			csvInitialHeaders[i++] = toolName + " ranking";
			csvInitialHeaders[i++] = toolName + " unsupported features";
		}
		
		return true;
	}
	

	public static List<Score> run (URI absoluteMMURI) {
		Recommender r = new Recommender();
		return r.computeScores(absoluteMMURI);
	}
	
	
	public static boolean openCSV () {
		try {
			writer = new CSVWriter(new FileWriter("ranking.csv"), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER);
		} catch (IOException e) {
			System.err.println("Error generating the CSV ranking");
			e.printStackTrace();
			return false;
		}
		
		writer.writeNext(csvInitialHeaders);
		return true;
	}
	

	public static void writeCSVLine (String modelName, List<Score> scores) {
		String[] nextLine = new String[csvInitialHeaders.length];
		nextLine[0] = modelName;
		nextLine[1] = scores.get(0).getTool().getClass().getSimpleName();
		
		int i = 2;
		for (Tool t : Constants.TOOL_REGISTRY) {
			for (Score s : scores) {
				if (!s.getTool().equals(t))
					continue;

				nextLine[i++] = String.valueOf(s.getScore());
				nextLine[i++] = String.valueOf(scores.indexOf(s) + 1);
				if (s.getUnsupported()) {
					nextLine[i++] = "Yes";
				} else {
					nextLine[i++] = "No";
				}
			}
		}
		writer.writeNext(nextLine);
	}
	

	public static boolean closeCSV () {
		try {
			writer.close();
		} catch (IOException e) {
			System.err.println("Error generating the CSV ranking");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
