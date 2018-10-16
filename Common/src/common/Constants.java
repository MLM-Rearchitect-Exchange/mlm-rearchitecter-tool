package common;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

public abstract class Constants {

	public static boolean DEBUGGING = false;
	
	public static int CONFIDENCE_THRESHOLD = 5;

	public static final String OPTION_DEBUG = "debug";
	
	public static final String OPTION_PATH = "path";
	private static final String OPTION_PATH_ALT_2 = "uri";
	public static final List<String> OPTION_LIST_PATH = (List<String>) Arrays.asList(OPTION_PATH, OPTION_PATH_ALT_2);
	
	public static final String OPTION_BULK = "bulk";
	private static final String OPTION_BULK_ALT_2 = "batch";
	public static final List<String> OPTION_LIST_BULK = (List<String>) Arrays.asList(OPTION_BULK, OPTION_BULK_ALT_2);

	public static final String OPTION_EXPORT = "export";
	private static final String OPTION_EXPORT_ALT_2 = "tool";
	public static final List<String> OPTION_LIST_EXPORT = (List<String>) Arrays.asList(OPTION_EXPORT, OPTION_EXPORT_ALT_2);
	public static final String OPTION_EXPORT_ALL = "exportAll";
	private static final String OPTION_EXPORT_ALL_ALT_2 = "all";
	public static final List<String> OPTION_LIST_EXPORT_ALL = (List<String>) Arrays.asList(OPTION_EXPORT_ALL, OPTION_EXPORT_ALL_ALT_2);
	public static final String OPTION_EXPORT_BEST = "exportBest";
	private static final String OPTION_EXPORT_BEST_ALT_2 = "exportWinner";
	private static final String OPTION_EXPORT_BEST_ALT_3 = "best";
	private static final String OPTION_EXPORT_BEST_ALT_4 = "winner";
	public static final List<String> OPTION_LIST_EXPORT_BEST = (List<String>) Arrays.asList(OPTION_EXPORT_BEST, OPTION_EXPORT_BEST_ALT_2, OPTION_EXPORT_BEST_ALT_3, OPTION_EXPORT_BEST_ALT_4);
	//TODO Add EAnnotations text
	

	static {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
	}
	
	public static final ResourceSet resourceSet = new ResourceSetImpl();
	public static final Resource multielvelHierarchyMM = resourceSet.getResource(Utils.getAbsoluteURI("MultilevelHierarchy.ecore", Constants.class), true);
	public static final EPackage multilevelEPackage = (EPackage) multielvelHierarchyMM.getContents().get(0);
	
	static {
		EPackage.Registry.INSTANCE.put(multilevelEPackage.getNsURI(), multilevelEPackage);
	}
	
	public static final EClass hierarchyEC = (EClass) multilevelEPackage.getEClassifier("Hierarchy");
	public static final EClass namedEC = (EClass) multilevelEPackage.getEClassifier("Named");
	public static final EClass modelEC = (EClass) multilevelEPackage.getEClassifier("Model");
	public static final EClass clabjectEC = (EClass) multilevelEPackage.getEClassifier("Clabject");
	public static final EClass featureEC = (EClass) multilevelEPackage.getEClassifier("Feature");
	public static final EClass referenceEC = (EClass) multilevelEPackage.getEClassifier("Reference");
	public static final EClass attributeEC = (EClass) multilevelEPackage.getEClassifier("Attribute");
	public static final EClass potencyEC = (EClass) multilevelEPackage.getEClassifier("Potency");
	public static final EClass cardinalityEC = (EClass) multilevelEPackage.getEClassifier("Cardinality");
	public static final EClass levelEC = (EClass) multilevelEPackage.getEClassifier("Level");
	public static final EEnum dataTypeEE = (EEnum) multilevelEPackage.getEClassifier("DataType");
	
	
}
