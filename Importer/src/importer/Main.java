package importer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.emf.common.util.URI;

import common.Constants;
import common.Debugger;
import common.Utils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class Main {

	public static void main(String[] args) {
		// Command-line options
		OptionParser parser = new OptionParser();
		parser.accepts(Constants.OPTION_HELP, Constants.OPTION_HELP_DESCRIPTION).forHelp();
		parser.acceptsAll(Constants.OPTION_LIST_PATH, Constants.OPTION_PATH_DESCRIPTION).withRequiredArg();
		parser.accepts(Constants.OPTION_DEBUG, Constants.OPTION_DEBUG_DESCRIPTION);
		parser.acceptsAll(Constants.OPTION_LIST_BULK, Constants.OPTION_BULK_DESCRIPTION).withRequiredArg();
		parser.accepts(importer.Constants.OPTION_TOOL, importer.Constants.OPTION_TOOL_DESCRIPTION).withRequiredArg();

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
		if (!Importer.init()) {
			System.err.println("Error initialising Exporter");
			return;
		}

		if (!options.has(importer.Constants.OPTION_TOOL)) {
			System.err.println("The tool needs to be specified");
			return;
		}
		
		String toolName = options.valuesOf(importer.Constants.OPTION_TOOL).get(0).toString();
		Debugger.log("Attempting to import from tool: " + toolName);
		
		URI absoluteMMURI = null;
		if (options.has(Constants.OPTION_PATH)) {
			String relativeMMURIString = options.valueOf(Constants.OPTION_PATH).toString();
			absoluteMMURI = Utils.getAbsoluteURI(relativeMMURIString, Main.class);
			if(null != absoluteMMURI) {
				Importer.run(absoluteMMURI, toolName);
			}
		} else if (options.has(Constants.OPTION_BULK)) {
			String bulkFilePath = options.valueOf(Constants.OPTION_BULK).toString();
			try (BufferedReader br = new BufferedReader(new FileReader(bulkFilePath))) {
				URI bulkFileURI = Utils.getAbsoluteURI(bulkFilePath, Main.class);
			    String line;
			    while ((line = br.readLine()) != null) {
					absoluteMMURI = URI.createFileURI(line).resolve(bulkFileURI);
					if(null != absoluteMMURI) {
						Importer.run(absoluteMMURI, toolName);
					}
			    }
			} catch (IOException e) {
				System.err.println("Error parsing bulk file \"" + bulkFilePath + "\"");
				e.printStackTrace();
				return;
			}
		} else {
			System.err.println("No path to folder or file provided");
			return;
		}
		
	}

}
