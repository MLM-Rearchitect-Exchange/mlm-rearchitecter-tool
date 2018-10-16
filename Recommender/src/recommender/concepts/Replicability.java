package recommender.concepts;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import recommender.Concept;

public class Replicability implements Concept {

	@Override
	public int appearances(Resource multilevelResource) {
		int appearances = 0;
		
		TreeIterator<EObject> eObjectsIt = multilevelResource.getAllContents();
		while (eObjectsIt.hasNext()) {
			EObject eo = eObjectsIt.next();
			EObject ec = eo.eContainer();
			if (eo.eClass().equals(common.Constants.potencyEC) && (ec.eClass().equals(common.Constants.clabjectEC) || ec.eClass().equals(common.Constants.referenceEC))) {
				int start = (int) eo.eGet(common.Constants.potencyEC.getEStructuralFeature("start"));
				int end = (int) eo.eGet(common.Constants.potencyEC.getEStructuralFeature("end"));
				int depth = (int) eo.eGet(common.Constants.potencyEC.getEStructuralFeature("depth"));
				if (start >= 1 && end > start && depth == 1) {
					appearances++;
				}
			}
		}
		return appearances;
	}

}
