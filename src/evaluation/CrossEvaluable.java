package evaluation;

import java.util.List;

public abstract class CrossEvaluable<T> extends Evaluable {
	
	/**
	 * Gets the Train/Test set from the Cross Validated Data
	 */
	public abstract void handleData (List<T> trainData, List<T> testData);
}