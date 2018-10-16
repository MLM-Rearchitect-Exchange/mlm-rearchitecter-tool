package exporter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.util.URI;

import common.Constants;
import common.Debugger;
import common.Utils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Main {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		// Command-line options
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Constants.OPTION_LIST_PATH).withRequiredArg();
		parser.accepts(Constants.OPTION_DEBUG);
		parser.acceptsAll(Constants.OPTION_LIST_BULK).withRequiredArg();
		parser.accepts(exporter.Constants.OPTION_ALL);
		parser.accepts(exporter.Constants.OPTION_TOOLS).withRequiredArg();

		OptionSet options = parser.parse(args);
		common.Constants.DEBUGGING = options.has(Constants.OPTION_DEBUG);
		if (!Exporter.init()) {
			System.err.println("Error initialising Exporter");
			return;
		}

		List<String> tools = null;
		if (options.has(exporter.Constants.OPTION_ALL)) {
			Debugger.log("Exporting to all registered tools");
		} else if (options.has(exporter.Constants.OPTION_TOOLS)) {
			tools = (List<String>) options.valuesOf(exporter.Constants.OPTION_TOOLS);
			Debugger.log("Attempting to export to tools:");
			for (String toolName : tools) {
				Debugger.log(toolName);
			}
		} else {
			Debugger.log("No tool specified. Attempting to export to all");
		}
		
		URI absoluteMMURI = null;
		if (options.has(Constants.OPTION_PATH)) {
			String relativeMMURIString = options.valueOf(Constants.OPTION_PATH).toString();
			absoluteMMURI = Utils.getAbsoluteURI(relativeMMURIString, Main.class);
			if(null != absoluteMMURI) {
				Exporter.run(absoluteMMURI, tools);
			}
		} else if (options.has(Constants.OPTION_BULK)) {
			String bulkFilePath = options.valueOf(Constants.OPTION_BULK).toString();
			try (BufferedReader br = new BufferedReader(new FileReader(bulkFilePath))) {
				URI bulkFileURI = Utils.getAbsoluteURI(bulkFilePath, Main.class);
			    String line;
			    while ((line = br.readLine()) != null) {
					absoluteMMURI = URI.createFileURI(line).resolve(bulkFileURI);
					if(null != absoluteMMURI) {
						Exporter.run(absoluteMMURI, tools);
					}
			    }
			} catch (IOException e) {
				System.err.println("Error parsing bulk file \"" + bulkFilePath + "\"");
				e.printStackTrace();
				return;
			}
		} else {
			System.err.println("No path to XMI file provided");
			return;
		}
		
	}

}
