package annotator.heuristics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EReference;

import annotator.Heuristic;

public abstract class TypeObjectReference implements Heuristic {

	@Override
	public abstract boolean apply(EModelElement em);

	
	protected int satisfiesPattern (EReference referenceInstanceER, EReference referenceTypeER) {
		String iName = referenceInstanceER.getName();
		String tName = referenceTypeER.getName();
		
		return checkNames(iName,
				tName,
				new ArrayList<String>(Arrays.asList("")),
				new ArrayList<String>(Arrays.asList("s", "es", "ies")),
				new ArrayList<String>(Arrays.asList("")),
				new ArrayList<String>(Arrays.asList("\\wtype", "\\wtypes")));		
	}
	

	// Check if names are consistent with pattern
	protected int checkNames (String iName, String tName, List<String> iPre, List<String> iPost, List<String> tPre, List<String> tPost) {
		int confidence = 0;
		
		Matcher sMatcher = Heuristic.checkName(iName, "\\w*", iPre, iPost);
		if ((null != sMatcher) && sMatcher.matches())
			confidence += 5;
		
		Matcher tMatcher = Heuristic.checkName(tName, sMatcher.group(2), tPre, tPost);
		if ((null != tMatcher) && tMatcher.matches())
			confidence += 5;
		
		return confidence;
	}	

}
