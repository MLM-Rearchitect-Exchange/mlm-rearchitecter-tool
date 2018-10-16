package recommender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.eclipse.emf.common.util.URI;

import common.Constants;
import common.Utils;

public class Main {

	public static void main(String[] args) {
		// Command-line options
		OptionParser parser = new OptionParser();
		parser.acceptsAll(Constants.OPTION_LIST_PATH).withRequiredArg();
		parser.accepts(Constants.OPTION_DEBUG);
		parser.acceptsAll(Constants.OPTION_LIST_BULK).withRequiredArg();

		OptionSet options = parser.parse(args);
		common.Constants.DEBUGGING = options.has(Constants.OPTION_DEBUG);
		if (!Recommender.init()) {
			System.err.println("Error initialising Recommender");
			return;
		}
		
		URI absoluteMMURI = null;
		if (options.has(Constants.OPTION_PATH)) {
			String relativeMMURIString = options.valueOf(Constants.OPTION_PATH).toString();
			absoluteMMURI = Utils.getAbsoluteURI(relativeMMURIString, Main.class);
			if(null != absoluteMMURI) {
				if (!Recommender.openCSV())
					return;
				List<Score> scores = Recommender.run(absoluteMMURI);
				Recommender.writeCSVLine(absoluteMMURI.lastSegment(), scores);
				if (!Recommender.closeCSV())
					return;
			}
		} else if (options.has(Constants.OPTION_BULK)) {
			String bulkFilePath = options.valueOf(Constants.OPTION_BULK).toString();
			try (BufferedReader br = new BufferedReader(new FileReader(bulkFilePath))) {
				URI bulkFileURI = Utils.getAbsoluteURI(bulkFilePath, Main.class);
			    String line;
				if (!Recommender.openCSV())
					return;
			    while ((line = br.readLine()) != null) {
					absoluteMMURI = URI.createFileURI(line).resolve(bulkFileURI);
					if(null != absoluteMMURI) {
						List<Score> scores = Recommender.run(absoluteMMURI);
						Recommender.writeCSVLine(absoluteMMURI.lastSegment(), scores);
					}
			    }
				if (!Recommender.closeCSV())
					return;
			} catch (IOException e) {
				System.err.println("Error parsing bulk file \"" + bulkFilePath + "\"");
				e.printStackTrace();
				return;
			}
		} else {
			System.out.println("No path to XMI file provided");
			return;
		}
	}

}
