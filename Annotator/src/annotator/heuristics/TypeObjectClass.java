package annotator.heuristics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;

import annotator.Heuristic;

public abstract class TypeObjectClass implements Heuristic {

	@Override
	public abstract boolean apply(EModelElement em);
	

	// Extract names and returns the confidence after invoking the checker method
	protected int satisfiesPattern (EClass source, EClass target, EReference ref) {
		String sName = source.getName();
		String tName = target.getName();
		String rName = ref.getName();
		
		return checkNames(
				sName,
				tName,
				rName,
				new ArrayList<String>(Arrays.asList("")),
				new ArrayList<String>(Arrays.asList("instance", "inst", "object")),
				new ArrayList<String>(Arrays.asList("meta")),
				new ArrayList<String>(Arrays.asList("type", "class")),
				"(typ(e|ing))|(inst(ance)?)|(speciali(z|s)ation)",
				new ArrayList<String>(Arrays.asList("meta", "is", "isA", "isThe")),
				new ArrayList<String>(Arrays.asList("of")));
	}
	
	
	// Check if names are consistent with pattern
	protected int checkNames (String sName, String tName, String rName,
			List<String> sPre, List<String> sPost,
			List<String> tPre, List<String> tPost,
			String rPatt, List<String> rPre, List<String> rPost) {
		int confidence = 0;
		
		Matcher sMatcher = Heuristic.checkName(sName, "\\w*", sPre, sPost);
		if (null != sMatcher) {
			if (sMatcher.matches()) {
				confidence += 3;
			}
		} else {
			return 0;
		}
		
		String instanceCoreName = "";
		Matcher tMatcher = null;
		for (int i=sMatcher.group(2).length();i>0;i--) {
			tMatcher = Heuristic.checkName(tName, sMatcher.group(2).substring(0, i), tPre, tPost);
			if ((null != tMatcher) && tMatcher.matches()) {
				confidence += Math.round(3 * i / sMatcher.group(2).length());
				instanceCoreName = "|(" + tMatcher.group(2) + ")";
				break;
			}
		}
		
		Matcher rMatcher = Heuristic.checkName(rName, rPatt + instanceCoreName, rPre, rPost);
		if ((null != rMatcher) && rMatcher.matches())
			confidence += 4;
		
		return confidence;
	}
	
}
