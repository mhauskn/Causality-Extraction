package evaluation;

import java.util.ArrayList;
import java.util.List;

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
	
	
	public String getEvaluationResults () {
		String out = "";
		for (EvalTracker tracker : trackers)
			out += tracker.getScoreBattery() + "\n";
		return out;
	}
}
