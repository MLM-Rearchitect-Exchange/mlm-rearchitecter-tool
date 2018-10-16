package importer;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

import common.Debugger;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

public class Importer {
		
	
	public static boolean init() {
		// Load all tool formats in the "formats" subpackage
		List<Class<? extends Format>> formatClasses = new ArrayList<>();
		new FastClasspathScanner(Format.class.getPackage().getName() + ".formats").matchClassesImplementing(Format.class, formatClasses::add).scan();
		for (Class<? extends Format> f : formatClasses) {
			try {
				Constants.FORMAT_REGISTRY.add(f.newInstance());
				Debugger.log("Registering tool format " + f.getSimpleName());
			} catch (Exception e) {
				System.err.println("Error during registration of tool formats");
				e.printStackTrace();
				return false;
			}
		}
		
		return true;
	}
	
	
	public void importHierarchy(Format format, URI hierarchyURI) {
		URI destinationURI = hierarchyURI.trimFileExtension().appendFileExtension("xmi");

		System.out.println("------ IMPORTER ------");
		System.out.println("FILE: " + destinationURI.toFileString());
		System.out.println("----------------------");
		
		Resource multilevelResource = common.Constants.resourceSet.createResource(destinationURI);
		String name = format.getClass().getSimpleName();
		
		Debugger.log("Importing from tool " + name);
		try {
			format.openImportFile(hierarchyURI);
		} catch (Exception e1) {
			System.err.println("I/O error when creating file with " + name + "'s format");
			e1.printStackTrace();
			return;
		}
		try {
			format.importHierarchy(multilevelResource);
		} catch (Exception e2) {
			System.err.println("I/O error when importing from " + name + "'s format");
			e2.printStackTrace();
			return;
		}
		try {
			format.closeImportFile();
		} catch (Exception e3) {
			System.err.println("I/O error when closing imported file");
			e3.printStackTrace();
			return;
		}
		Debugger.log("Finished importing from tool " + name);
	}
	
	
	public static void run(URI hierarchyModelURI, String toolName) {
		Importer i = new Importer();

		for (Format f : Constants.FORMAT_REGISTRY) {
			if (toolName.equalsIgnoreCase(f.getClass().getSimpleName())) {
				i.importHierarchy(f, hierarchyModelURI);
				return;
			}
		}
	}
	
}
