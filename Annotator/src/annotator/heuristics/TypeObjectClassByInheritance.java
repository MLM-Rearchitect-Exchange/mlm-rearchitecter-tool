package annotator.heuristics;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;

import annotator.Heuristic;
import common.Annotations;
import common.Utils;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.POS;

public class TypeObjectClassByInheritance implements Heuristic {

	private WuPalmer wuPalmer;
	private IDictionary dict;
	
	public TypeObjectClassByInheritance() {
		ILexicalDatabase db = new NictWordNet();
		WS4JConfiguration.getInstance().setMFS(true);
		wuPalmer = new WuPalmer(db);
		initializeDictionary();
	}
		
	@Override
	public boolean apply (EModelElement em) {
		if (!(em instanceof EClass))
			return false;
		EClass ecs = (EClass) em;
		EClass parentEC = null;
		double maxConfidence = 0;
		for (EClass pec : ecs.getESuperTypes()) {
			EAnnotation existingEAnnotation = pec.getEAnnotation(Annotations.TYPE_OBJECT_STATIC_TYPE);
			if (null != existingEAnnotation) {
				existingEAnnotation.getReferences().add(ecs);
				String confidenceString = existingEAnnotation.getDetails().get(Annotations.CONFIDENCE);
				EAnnotation ea = Heuristic.addEAnnotation(ecs, Annotations.TYPE_OBJECT_STATIC_INSTANCE, Arrays.asList(pec));
				ea.getDetails().put(Annotations.CONFIDENCE, confidenceString);
				return false;
			}
			if (0 == satisfiesPattern(ecs, pec))
				continue;
			String parentName = pec.getName();
			IIndexWord parentIndexWord = dict.getIndexWord(parentName, POS.NOUN);
			double confidence = 0;
			Set<EClass> siblings = Utils.getSubEClasses(pec, false);
			for (EClass siblingEC : siblings) {
				String siblingName = siblingEC.getName();
				IIndexWord siblingIndexWord = dict.getIndexWord(siblingName, POS.NOUN);
				if ((null != siblingIndexWord) && (null != parentIndexWord)) {
					confidence += wuPalmer.calcRelatednessOfWords(siblingName, parentName);
				} else {
					confidence += Utils.stringSimilarity(siblingName, parentName);
				}
			}
			confidence = confidence / siblings.size();
			if (confidence > maxConfidence) {
				maxConfidence = confidence;
				parentEC = pec;
			}
		}
		if (null == parentEC)
			return false;
		
		String confidenceString = Long.toString(Math.round(maxConfidence * 10));
		EAnnotation ea = Heuristic.addEAnnotation(ecs, Annotations.TYPE_OBJECT_STATIC_INSTANCE, Arrays.asList(parentEC));
		ea.getDetails().put(Annotations.CONFIDENCE, confidenceString);
		ea = Heuristic.addEAnnotation(parentEC, Annotations.TYPE_OBJECT_STATIC_TYPE, Arrays.asList(ecs));
		ea.getDetails().put(Annotations.CONFIDENCE, confidenceString);
		return true;
	}
	
	
	protected int satisfiesPattern (EClass child, EClass parent) {
		if (parent.isAbstract())
			return 0;
		for (EClass ec:  Utils.getSubEClasses(parent, false)) {
			if (ec.isAbstract()) {
				return 0;
			}
		}
		return 1;
	}
	
	
	private void initializeDictionary() {
		// URL to the Wordnet dictionary directory
		String path = "C:/Users/fernando/Documents/wordnet/dict";
		URL url = null;
		try {
			url = new URL("file", null, path);
		} catch (MalformedURLException e) {
			System.err.println("Broken dictionary URI");
		}

		// Construct the dictionary object and open it
		dict = new Dictionary(url);
		try {
			dict.open();
		} catch (IOException e) {
			System.err.println("Could not open dictionary");
		}
	}

}
