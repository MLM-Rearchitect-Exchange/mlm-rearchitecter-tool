package rearchitecter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.eclipse.emf.common.util.URI;

import recommender.Recommender;
import recommender.Score;
import transformer.Metrics;
import transformer.Transformer;
import annotator.Annotator;

import common.Constants;
import common.Debugger;
import common.Utils;

import exporter.Exporter;

public class Main {
	
	private static enum ExportMode {NOEXPORT, LIST, ALL, BEST};
	private static ExportMode export = ExportMode.NOEXPORT;
	private static List<String> toolNames;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// Command-line options
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Constants.OPTION_LIST_PATH).withRequiredArg();
		parser.accepts(Constants.OPTION_DEBUG);
		parser.acceptsAll(Constants.OPTION_LIST_BULK).withRequiredArg();
		parser.acceptsAll(Constants.OPTION_LIST_EXPORT).withRequiredArg();
		parser.acceptsAll(Constants.OPTION_LIST_EXPORT_ALL);
		parser.acceptsAll(Constants.OPTION_LIST_EXPORT_BEST);

		OptionSet options = parser.parse(args);
		common.Constants.DEBUGGING = options.has(Constants.OPTION_DEBUG);
		
		if (options.has(Constants.OPTION_EXPORT_ALL)) {
			Debugger.log("Resulting models will be exported to all registered tool formats");
			export = ExportMode.ALL;
		} else if (options.has(Constants.OPTION_EXPORT_BEST)) {
			Debugger.log("Resulting models will be exported to best scoring tool format");
			export = ExportMode.BEST;
		} else if (options.has(Constants.OPTION_EXPORT)) {
			toolNames = (List<String>) options.valuesOf(Constants.OPTION_EXPORT);
			Debugger.log("Resulting models will be exported to tools:");
			for (String toolName : toolNames) {
				Debugger.log(toolName);
			}
			export = ExportMode.LIST;
		}
		
		if (!Annotator.init()) {
			System.err.println("Error initialising Annotator");
			return;
		}
		if (!Transformer.init()) {
			System.err.println("Error initialising Transformer");
			return;
		}
		if (!Recommender.init()) {
			System.err.println("Error initialising Recommender");
			return;
		}
		if ((export!=ExportMode.NOEXPORT) && (!Exporter.init())) {
			System.err.println("Error initialising Exporter");
			return;
		}
		
		
		URI absoluteMMURI = null;
		if (options.has(Constants.OPTION_PATH)) {
			String relativeMMURIString = options.valueOf(Constants.OPTION_PATH).toString();
			absoluteMMURI = Utils.getAbsoluteURI(relativeMMURIString, Main.class);
			if(null != absoluteMMURI) {
				if (!Transformer.openCSV())
					return;
				if (!Recommender.openCSV())
					return;
				runProcess(absoluteMMURI);
				if (!Transformer.closeCSV())
					return;
				if (!Recommender.closeCSV())
					return;
			}
		} else if (options.has(Constants.OPTION_BULK)) {
			String bulkFilePath = options.valueOf(Constants.OPTION_BULK).toString();
			try (BufferedReader br = new BufferedReader(new FileReader(bulkFilePath))) {
				URI bulkFileURI = Utils.getAbsoluteURI(bulkFilePath, Main.class);
			    String line;
				if (!Transformer.openCSV())
					return;
				if (!Recommender.openCSV())
					return;
			    while ((line = br.readLine()) != null) {
					absoluteMMURI = URI.createFileURI(line).resolve(bulkFileURI);
					if(null != absoluteMMURI) {
						runProcess(absoluteMMURI);
					}
			    }
				if (!Transformer.closeCSV())
					return;
				if (!Recommender.closeCSV())
					return;
			} catch (IOException e) {
				System.err.println("Error parsing bulk file \"" + bulkFilePath + "\"");
				e.printStackTrace();
				return;
			}
		} else {
			System.err.println("No path to Ecore file provided");
			return;
		}
		
	}

	private static void runProcess (URI absoluteMMURI) {
		// Annotator
		Annotator a = new Annotator();
		a.annotate(absoluteMMURI);
		
		// Transformer
		absoluteMMURI = absoluteMMURI.trimSegments(1).appendSegment("generated").appendSegment(absoluteMMURI.lastSegment());
		Metrics m = Transformer.run(absoluteMMURI);
		Transformer.writeCSVLine(absoluteMMURI.lastSegment(), m);
		
		// Recommender
		absoluteMMURI = absoluteMMURI.trimFileExtension().appendFileExtension("xmi");
		List<Score> scores = Recommender.run(absoluteMMURI);
		Recommender.writeCSVLine(absoluteMMURI.lastSegment(), scores);
		
		// Exporter
		switch (export) {
			case ALL:
				Exporter.run(absoluteMMURI, null);
			break;
			case BEST:
				Exporter.run(absoluteMMURI, Arrays.asList(scores.get(0).getTool().getClass().getSimpleName()));
			break;
			case LIST:
				Exporter.run(absoluteMMURI, toolNames);
			break;
			case NOEXPORT:
			default:
			return;
		}
	}
	
}
