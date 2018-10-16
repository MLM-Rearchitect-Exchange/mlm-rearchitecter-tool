package recommender.concepts;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import recommender.Concept;

public class AbstractTypes implements Concept {

	@Override
	public int appearances(Resource multilevelResource) {
		int appearances = 0;
		
		TreeIterator<EObject> eObjectsIt = multilevelResource.getAllContents();
		while (eObjectsIt.hasNext()) {
			EObject eo = eObjectsIt.next();
			if (!eo.eClass().equals(common.Constants.clabjectEC))
				continue;
			
			if ((boolean) eo.eGet(common.Constants.clabjectEC.getEStructuralFeature("isAbstract")))
				appearances++;
		}
		
		return appearances;
	}

}
