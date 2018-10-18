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
		parser.accepts(Constants.OPTION_HELP, Constants.OPTION_HELP_DESCRIPTION).forHelp();
		parser.acceptsAll(Constants.OPTION_LIST_PATH, Constants.OPTION_PATH_DESCRIPTION).withRequiredArg();
		parser.accepts(Constants.OPTION_DEBUG, Constants.OPTION_DEBUG_DESCRIPTION);
		parser.acceptsAll(Constants.OPTION_LIST_BULK, Constants.OPTION_BULK_DESCRIPTION).withRequiredArg();
		parser.acceptsAll(Constants.OPTION_LIST_EXPORT_ALL, Constants.OPTION_EXPORT_ALL_DESCRIPTION);
		parser.accepts(Constants.OPTION_EXPORT, Constants.OPTION_EXPORT_DESCRIPTION).withRequiredArg();

		OptionSet options = parser.parse(args);
		if (options.has(Constants.OPTION_HELP)) {
			try {
				parser.printHelpOn(System.out);
			} catch (IOException e) {
				System.err.println("There was an error when trying to display the help");
			} 
			return;
		}
		
		common.Constants.DEBUGGING = options.has(Constants.OPTION_DEBUG);
		if (!Exporter.init()) {
			System.err.println("Error initialising Exporter");
			return;
		}

		List<String> tools = null;
		if (options.has(Constants.OPTION_EXPORT_ALL)) {
			Debugger.log("Exporting to all registered tools");
		} else if (options.has(Constants.OPTION_EXPORT)) {
			tools = (List<String>) options.valuesOf(Constants.OPTION_EXPORT);
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
