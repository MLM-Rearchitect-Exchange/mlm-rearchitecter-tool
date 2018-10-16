package transformer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import common.Debugger;

// Implements the singleton pattern
public class ElementRegistry {

	private Map<EClass, EObject> classes;
	private Map<EReference, EObject> references;
	
	private static ElementRegistry instance = null;
	
	
	protected ElementRegistry () {
		classes = new HashMap<EClass, EObject>();	
		references = new HashMap<EReference, EObject>();	
	}

	
	public static ElementRegistry getInstance () {
		if(null == instance) {
			instance = new ElementRegistry();
		}
		return instance;
	}
	
	
	public void registerMultilevelClass (EClass ec, EObject mmClass) {
		classes.put(ec, mmClass);
		Debugger.log("Transformed of EClass " + ec.getName() + " is class " + ElementRegistry.getInstance().getMultilevelClass(ec).eGet(mmClass.eClass().getEStructuralFeature("name")));
	}
	
	
	public EObject getMultilevelClass (EClass ec) {
		return classes.get(ec);
	}
	
	
	public void registerMultilevelReference (EReference er, EObject reference) {
		references.put(er, reference);
		Debugger.log("Transformed of EReference " + er.getName() + " is reference " + ElementRegistry.getInstance().getMultilevelReference(er).eGet(reference.eClass().getEStructuralFeature("name")));
	}
	
	
	public EObject getMultilevelReference (EReference er) {
		return references.get(er);
	}
	
	
	public int getClassAmount () {
		Set<EObject> nodeSet = new HashSet<EObject>(classes.values());
		return nodeSet.size();
	}
	
	
	public int getReferenceAmount () {
		Set<EObject> referenceSet = new HashSet<EObject>(references.values());
		return referenceSet.size();
	}
	
	
	@SuppressWarnings("unchecked")
	public int getAttributeAmount () {
		int amount = 0;
		Set<EObject> nodeSet = new HashSet<EObject>(classes.values());
		for (EObject eo : nodeSet) {
			EList<EObject> features = (EList<EObject>) eo.eGet(classes.values().iterator().next().eClass().getEStructuralFeature("features"));
			for (EObject eoa : features) {
				if (eoa.eClass().getName().equals("Attribute")) {
					amount++;
				}
			}
		}
		return amount;
	}
	
	
	public void clean () {
		instance = null;
	}
	
}
