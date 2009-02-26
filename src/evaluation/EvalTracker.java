package evaluation;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the evaluation of a given object 
 */
@SuppressWarnings("unchecked")
public class EvalTracker {
	String trackerName;
	int retrieved_relevant;
	int relevant;
	int retrieved;
	int total_visited;
	
	List<EvaluationAgent> agents;
	Evaluable dataSource;
	
	public EvalTracker (EvaluationAgent agent, String _trackerName, Evaluable _dataSource) {
		trackerName = _trackerName;
		retrieved_relevant = 0;
		relevant = 0;
		retrieved = 0;
		total_visited = 0;
		agents = new ArrayList<EvaluationAgent>();
		addAgent(agent);
		dataSource = _dataSource;
	}
	
	/**
	 * Adds Another Agent to our list of evaluation agents
	 */
	public void addAgent (EvaluationAgent agent) {
		agent.setTracker(this);
		agents.add(agent);
	}
	
	/**
	 * Prompts Every Evaluation Agent to preform its evaluation.
	 */
	public void doEval () {
		for (EvaluationAgent agent : agents)
			agent.doEvaluation();
	}
	
	public void addVisited () {
		total_visited++;
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
		out += "\tRelevant: " + relevant + " Retrieved: " + retrieved + " Rel&Ret: " + retrieved_relevant + " Total_Seen: " + total_visited + "\n";
		out += "\t%Rel: " + (relevant/(double)total_visited) + " %Ret: " + (retrieved/(double)total_visited) + "\n";
		out += "\tPrecision: " + getPrecision() + " Recall: " + getRecall() + " F-Measure: " + 
			getFMeasure() + "\n";
		return out;
	}
}
