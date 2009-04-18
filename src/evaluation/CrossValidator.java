package evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Divides data into train and test sets.
 * Evaluates data.
 */
public class CrossValidator<T> {
	int numValidations;
	CrossEvaluable<T> caller;
	List<T> data;
	
	/**
	 * To Cross Validate, we need to know the following:
	 * @param _numValidations- How many times to split/evaluate the data
	 * @param _data- The actual data to split/evaluate
	 * @param _caller- The object to evaluate the split data
	 */
	public CrossValidator (int _numValidations, List<T> _data, CrossEvaluable<T> _caller) {
		numValidations = _numValidations;
		data = _data;
		caller = _caller;
		//shuffle();
	}
	
	/**
	 * Divides our data and gives back to the caller the
	 * train and test data to allow the caller to train.
	 */
	public void crossValidate () {
		double index = 0.0;
		double increment = data.size() / (double) numValidations;
		for (int i = 0; i < numValidations; i++) {
			int start = (int) Math.round(index);
			int end = (int) Math.round(index + increment);
			/*List<T> test = data.subList(start, end);
			List<T> train = data.subList(0, start);
			train.addAll(data.subList(end, data.size()));*/
			ArrayList<T> test = new ArrayList<T>();
			ArrayList<T> train = new ArrayList<T>();
			for (int j = 0; j < data.size(); j++) {
				if (j >= start && j < end)
					test.add(data.get(j));
				else
					train.add(data.get(j));
			}
			caller.handleData(train, test); // Let the caller chew on this test train data a bit
			caller.evaluate(); // Run his evaluation to see how he did
			index += increment;
		}
	}
	
	/**
	 * Shuffles our data
	 */
	public void shuffle () {
		Collections.shuffle(data);
	}
}
