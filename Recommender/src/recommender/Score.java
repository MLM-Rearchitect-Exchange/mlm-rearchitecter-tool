package recommender;

public class Score implements Comparable<Score> {
	
	private Tool tool;
	private int score = 0;
	private boolean unsupportedFeatures = false;
	
	public Score (Tool t) {
		tool = t;
	}

	public Tool getTool () {
		return tool;
	}

	public int getScore () {
		return score;
	}

	public void setScore (int score) {
		this.score = score;
	}

	public boolean getUnsupported () {
		return unsupportedFeatures;
	}

	public void setUnsupported () {
		this.unsupportedFeatures = true;
	}

	@Override
	public int compareTo (Score other) {
		int comparison = Integer.compare(this.score, other.score);

		// Not being able to support some feature plays heavily against the tool
		if (this.unsupportedFeatures) {
			if (other.unsupportedFeatures) {
				return -comparison;
			} else {
				return 1;
			}
		} else {
			if (other.unsupportedFeatures) {
				return -1;
			} else {
				return -comparison;
			}
		}
	}

}
