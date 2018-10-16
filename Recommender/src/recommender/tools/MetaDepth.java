package recommender.tools;

import recommender.Constants.Support;
import recommender.Tool;

public class MetaDepth implements Tool {

	@Override
	public Support getStandardPotencySupport() {
		return Support.SUPPORTED;
	}

	@Override
	public Support getLeapPotencySupport() {
		return Support.SUPPORTED;
	}

	@Override
	public Support getReplicabilitySupport() {
		return Support.UNSUPPORTED;
	}

	@Override
	public Support getDeepLeapPotencySupport() {
		return Support.UNSUPPORTED;
	}

	@Override
	public Support getDeepReplicabilitySupport() {
		return Support.UNSUPPORTED;
	}

	@Override
	public Support getAttributeDurabilitySupport() {
		return Support.SUPPORTED;
	}

	@Override
	public Support getAttributeMutabilitySupport() {
		return Support.EMULATED;
	}

	@Override
	public Support getShallowCardinalitySupport() {
		return Support.SUPPORTED;
	}

	@Override
	public Support getDeepCardinalitySupport() {
		return Support.EMULATED;
	}

	@Override
	public Support getMultipleTypingSupport() {
		return Support.SUPPORTED;
	}

	@Override
	public Support getAbstractTypesSupport() {
		return Support.SUPPORTED;
	}

}
