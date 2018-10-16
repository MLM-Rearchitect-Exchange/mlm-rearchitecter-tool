package transformer;

import java.util.List;

import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.resource.Resource;

public interface Transformation {
	
	public List<String> getSupportedAnnotations();
	
	public boolean apply(EModelElement em, Resource multilevelResource);
	
}
