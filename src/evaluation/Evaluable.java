package evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * An Evaluable object is one which can be evaluated via 
 * Evaluation Trackers.
 *
 */
public abstract class Evaluable {
	List<EvalTracker> trackers = null;
	
	public Evaluable () {
		trackers = new ArrayList<EvalTracker>();
	}
	
	/**
	 * Evaluates the System via the Trackers
	 */
	public void evaluate () {
		for (EvalTracker tracker : trackers)
			tracker.doEval();
	}
	
	/**
	 * Adds a new tracker to the list of evaluation trackers
	 */
	public void addTracker (EvalTracker tracker) {
		trackers.add(tracker);
	}
	
	/**
	 * Returns the evaluation results from our trackers
	 */
	public String getEvaluationResults () {
		String out = "";
		for (EvalTracker tracker : trackers)
			out += tracker.getScoreBattery() + "\n";
		return out;
	}
	
	/**
	 * Provides data to our Evaluation Agents when they 
	 * need to preform their eval.
	 */
	@SuppressWarnings("unchecked")
	public abstract void provideData (EvaluationAgent a);
}
