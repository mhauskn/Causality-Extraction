package evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the evaluation of a given object 
 */
public class EvalTracker {
	String trackerName;
	int retrieved_relevant;
	int relevant;
	int retrieved;
	List<EvaluationAgent<?>> agents;
	
	public EvalTracker (EvaluationAgent<?> agent, String _trackerName) {
		trackerName = _trackerName;
		retrieved_relevant = 0;
		relevant = 0;
		retrieved = 0;
		agents = new ArrayList<EvaluationAgent<?>>();
		addAgent(agent);
	}
	
	/**
	 * Adds Another Agent to our list of evaluation agents
	 */
	public void addAgent (EvaluationAgent<?> agent) {
		agent.setTracker(this);
		agents.add(agent);
	}
	
	/**
	 * Prompts Every Evaluation Agent to preform its evaluation.
	 */
	public void doEval () {
		for (EvaluationAgent<?> agent : agents)
			agent.doEvaluation();
	}
	
	public void addCorrect () {
		retrieved_relevant++;
	}
	
	public void addRelevant () {
		relevant++;
	}
	
	public void addRetrieved () {
		retrieved++;
	}
	
	/**
	 * Precision is the measure of how accurate the retrieved information is
	 */
	public double getPrecision () {
		return retrieved_relevant / (double) retrieved;
	}
	
	/**
	 * Recall is the measure of how comprehensive the retrieved information is
	 */
	public double getRecall () {
		return retrieved_relevant / (double) relevant;
	}
	
	public double getFMeasure () {
		 return (2 * getPrecision() * getRecall()) / (getPrecision() + getRecall());
	}
	
	public String getScoreBattery () {
		String out = trackerName + ":\n";
		out += "\tPrecision: " + getPrecision() + " Recall: " + getRecall() + " F-Measure: " + 
			getFMeasure();
		return out;
	}
}
