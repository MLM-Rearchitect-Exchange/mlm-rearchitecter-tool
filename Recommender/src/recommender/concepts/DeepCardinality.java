package recommender.concepts;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import recommender.Concept;

public class DeepCardinality implements Concept {

	@Override
	public int appearances(Resource multilevelResource) {
		int appearances = 0;
		
		TreeIterator<EObject> eObjectsIt = multilevelResource.getAllContents();
		while (eObjectsIt.hasNext()) {
			EObject eo = eObjectsIt.next();
			if (!eo.eClass().equals(common.Constants.referenceEC))
				continue;
			
			EObject potencyEO = (EObject) eo.eGet(common.Constants.referenceEC.getEStructuralFeature("potency"));
			if (null==potencyEO)
				continue;
			
			int start = (int) potencyEO.eGet(common.Constants.potencyEC.getEStructuralFeature("start"));
			int end = (int) potencyEO.eGet(common.Constants.potencyEC.getEStructuralFeature("end"));
			int depth = (int) potencyEO.eGet(common.Constants.potencyEC.getEStructuralFeature("depth"));
			if (start > 1 && end > start && (depth > 1 || depth == -1))
				appearances++;
		}
		
		return appearances;
	}

}
