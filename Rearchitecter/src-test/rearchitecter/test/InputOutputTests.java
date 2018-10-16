package rearchitecter.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.diff.DefaultDiffEngine;
import org.eclipse.emf.compare.diff.DiffBuilder;
import org.eclipse.emf.compare.diff.FeatureFilter;
import org.eclipse.emf.compare.diff.IDiffEngine;
import org.eclipse.emf.compare.diff.IDiffProcessor;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import transformer.Transformer;
import annotator.Annotator;

import common.Utils;

public class InputOutputTests {

	private static IDiffEngine diffEngine;

	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// Redefine EMFCompare comparison to disregard order changes
		IDiffProcessor diffProcessor = new DiffBuilder();
		diffEngine = new DefaultDiffEngine(diffProcessor) {
			@Override
			protected FeatureFilter createFeatureFilter() {
				return new FeatureFilter() {
					@Override
					public boolean checkForOrderingChanges(EStructuralFeature feature) {
						return false;
					}
				};
			}
		};
		
		// Register XMI extension
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
	}

	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	
	@Before
	public void setUp() throws Exception {
	}

	
	@After
	public void tearDown() throws Exception {
	}

	
	@Test
	public void testSecurityPolicies() {
		String metamodelName = "SecurityPolicy";
		runAnnotatorAndTransformer(metamodelName);
		List<Diff> differences = compare(metamodelName);
		assertEquals(differences.size(), 0);
	}
	
	
	protected void runAnnotatorAndTransformer(String metamodelName) {
		String ecoreMMPath = "test-resources/" + metamodelName + ".ecore";
		
		URI currentMMURI = Utils.getAbsoluteURI(ecoreMMPath, InputOutputTests.class);
		Annotator.init();
		Annotator a = new Annotator();
		a.annotate(currentMMURI);
		currentMMURI = currentMMURI.trimSegments(1).appendSegment("generated").appendSegment(currentMMURI.lastSegment());
		Transformer.init();
		Transformer t = new Transformer();
		t.transform(currentMMURI);
	}
	
	
	protected List<Diff> compare (String metamodelName) {
		ResourceSet resourceSet1 = new ResourceSetImpl();
		ResourceSet resourceSet2 = new ResourceSetImpl();
		
		resourceSet1.getResource(URI.createFileURI("test-resources/correct/" + metamodelName + ".xmi"), true);
		resourceSet2.getResource(URI.createFileURI("test-resources/generated/" + metamodelName + ".xmi"), true);
		
		IComparisonScope scope = new DefaultComparisonScope(resourceSet1, resourceSet2, null);
		Comparison comparison = EMFCompare.builder().setDiffEngine(diffEngine).build().compare(scope);

		return comparison.getDifferences();
	}

}
