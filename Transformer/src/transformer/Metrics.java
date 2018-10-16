package transformer;


// Simple POJO for metric handling
public class Metrics {

	private int twoLevelClasses;
	private int twoLevelReferences;
	private int twoLevelAttributes;
	private int multilevelClasses;
	private int multilevelReferences;
	private int multilevelAttributes;
	
	
	public int getTwoLevelClasses () {
		return twoLevelClasses;
	}
	
	
	public void setTwoLevelClasses (int twoLevelClasses) {
		this.twoLevelClasses = twoLevelClasses;
	}
	
	
	public int getTwoLevelReferences () {
		return twoLevelReferences;
	}
	
	
	public void setTwoLevelReferences (int twoLevelReferences) {
		this.twoLevelReferences = twoLevelReferences;
	}
	
	
	public int getTwoLevelAttributes () {
		return twoLevelAttributes;
	}
	
	
	public void setTwoLevelAttributes (int twoLevelAttributes) {
		this.twoLevelAttributes = twoLevelAttributes;
	}
	
	
	public int getMultilevelClasses () {
		return multilevelClasses;
	}
	
	
	public void setMultilevelClasses (int multilevelClasses) {
		this.multilevelClasses = multilevelClasses;
	}
	
	
	public int getMultilevelReferences () {
		return multilevelReferences;
	}
	
	
	public void setMultilevelReferences (int multilevelReferences) {
		this.multilevelReferences = multilevelReferences;
	}
	
	
	public int getMultilevelAttributes () {
		return multilevelAttributes;
	}
	
	
	public void setMultilevelAttributes (int multilevelAttributes) {
		this.multilevelAttributes = multilevelAttributes;
	}
	
	
	public int getTwoLevelTotal () {
		return twoLevelClasses + twoLevelReferences + twoLevelAttributes;
	}
	
	
	public int getMultilevelTotal () {
		return multilevelClasses + multilevelReferences + multilevelAttributes;
	}
	
	
	public int getClassReduction () {
		return getReduction(twoLevelClasses, multilevelClasses);
	}
	
	
	public int getReferenceReduction () {
		return getReduction(twoLevelReferences, multilevelReferences);
	}
	
	
	public int getAttributeReduction () {
		return getReduction(twoLevelAttributes, multilevelAttributes);
	}
	
	
	public int getTotalReduction () {
		return getReduction(getTwoLevelTotal(), getMultilevelTotal());
	}
	
	
	private int getReduction (int twoLevelValue, int multilevelValue) {
		if (0==twoLevelValue)
			return 0;
		
		return 100 - (100*multilevelValue)/twoLevelValue;
	}
	
}
