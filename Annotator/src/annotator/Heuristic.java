package annotator;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcoreFactory;

public interface Heuristic {

	public boolean apply (EModelElement em);
	
	public static EAnnotation addEAnnotation (EModelElement em, String text, Collection<EObject> references) {
		EAnnotation ea = EcoreFactory.eINSTANCE.createEAnnotation();
		ea.setSource(text);
		if (null != references) {
			ea.getReferences().addAll(references);
		}
		em.getEAnnotations().add(ea);
		
		return ea;
	}

	
	// Attempt to match name with all possible combinations of prefix and suffix
	// Returns the matcher if success, and null otherwise
	public static Matcher checkName (String name, String pattern, List<String> pre, List<String> post) {
		boolean match = false;
		
		// The last attempt in the loop below is to try matching without prefix and/or suffix (hence, empty ones)
		pre.add("");
		post.add("");
		
		// Attempt to match with both prefix and suffix
		Iterator<String> preIt = pre.iterator();
		while (preIt.hasNext() && !match) {
			Iterator<String> postIt = post.iterator();
			String preItNext = preIt.next();
			while (postIt.hasNext() && !match) {
				Matcher matcher = Pattern.compile("(" + preItNext + ")(" + pattern + ")(" + postIt.next() + ")", Pattern.CASE_INSENSITIVE).matcher(name);
				if (matcher.matches()) {
					return matcher;
				}
			}
		}
		return null;
	}
	
}
