package recommender;

import recommender.Constants.Support;

// Must be kept in sync with the implementations of the Concept interface contained in the "concepts" subpackage
public interface Tool {

	public Support getStandardPotencySupport();
	public Support getLeapPotencySupport();
	public Support getReplicabilitySupport();
	public Support getDeepLeapPotencySupport();
	public Support getDeepReplicabilitySupport();

	public Support getAttributeDurabilitySupport();
	public Support getAttributeMutabilitySupport();

	public Support getShallowCardinalitySupport();
	public Support getDeepCardinalitySupport();
	public Support getMultipleTypingSupport();
	public Support getAbstractTypesSupport();
	
}
