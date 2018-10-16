package exporter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import common.Debugger;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

public class Exporter {
	
	private ResourceSet resourceSet;
	private EPackage multilevelEPackage;
	
	
	public Exporter() {
		// Initialise internal EMF ResourceSet
		resourceSet = new ResourceSetImpl();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

		// Load MultilevelHierarchy.ecore from file
		multilevelEPackage = common.Constants.multilevelEPackage;
		EPackage.Registry.INSTANCE.put(multilevelEPackage.getNsURI(), multilevelEPackage);
	}

	
	public void export(Format format, URI hierarchyModelURI) {
		URI destinationURI = hierarchyModelURI.trimFileExtension();
		Resource multilevelResource = resourceSet.getResource(hierarchyModelURI, true);
		String name = format.getClass().getSimpleName();
		
		Debugger.log("Exporting to tool " + name);
		try {
			format.openExportFile(destinationURI);
		} catch (Exception e1) {
			System.err.println("I/O error when creating file with " + name + "'s format");
			e1.printStackTrace();
			return;
		}
		try {
			format.export(multilevelResource);
		} catch (Exception e2) {
			System.err.println("I/O error when exporting to " + name + "'s format");
			e2.printStackTrace();
			return;
		}
		try {
			format.closeExportFile();
		} catch (Exception e3) {
			System.err.println("I/O error when closing file with " + name + "'s format");
			e3.printStackTrace();
			return;
		}
		Debugger.log("Finished exporting to tool " + name);
	}
	
	
	public void exportMultiple(URI hierarchyModelURI, List<String> toolNames) {
		System.out.println("------ EXPORTER -------");
		System.out.println("FILE: " + hierarchyModelURI.toFileString());
		System.out.println("-----------------------");
		
		for (Format f : Constants.FORMAT_REGISTRY) {
			for (String name : toolNames) {
				if (name.equalsIgnoreCase(f.getClass().getSimpleName())) {
					export(f, hierarchyModelURI);
					break;
				}
			}
		}
	}
	
	
	public void exportAll(URI hierarchyModelURI) {
		System.out.println("------ EXPORTER -------");
		System.out.println("FILE: " + hierarchyModelURI.toFileString());
		System.out.println("-----------------------");
		for (Format f : Constants.FORMAT_REGISTRY) {
			export(f, hierarchyModelURI);
		}
	}
	
	
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
	
	
	public static void run(URI hierarchyModelURI, List<String> toolNames) {
		Exporter e = new Exporter();
		
		if ((null == toolNames) || (toolNames.size()==0)) {
			e.exportAll(hierarchyModelURI);
			return;
		}
		
		e.exportMultiple(hierarchyModelURI, toolNames);
	}
	
}
