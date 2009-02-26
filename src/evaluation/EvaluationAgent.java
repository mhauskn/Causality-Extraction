package evaluation;

/**
 * The EvaluationAgent is the Right-hand-man of the EvalTracker.
 * It is responsible for giving updates to the eval tracker.
 * It gets data of type T and must be shown how to determine if the 
 * data is relevant and is retrieved.
 * @author epn
 *
 */
public abstract class EvaluationAgent <baseUnit,dataSource> {
	EvalTracker tracker = null;
	dataSource data = null;
	
	/**
	 * Sets the tracker this agent reports to
	 * @param _tracker
	 */
	public void setTracker (EvalTracker _tracker) {
		tracker = _tracker;
	}
	
	/**
	 * Evaluates the performance of the system
	 * 
	 * 1. Get data from our data source
	 * 2. Evaluate our data
	 */
	void doEvaluation () {
		tracker.dataSource.provideData(this);
		processData(data);
		while (!exhausted()) {
			baseUnit unit = getNextUnit();
			boolean relevant = isRelevant(unit);
			boolean retrieved = isRetrieved(unit);
			
			tracker.addVisited();
			if (relevant)
				tracker.addRelevant();
			if (retrieved)
				tracker.addRetrieved();
			if (relevant && retrieved)
				tracker.addCorrect();
		}
	}
		
	/**
	 * Determines if the baseUnit unit has is deemed relevant
	 */
	public abstract boolean isRelevant (baseUnit unit);
	
	/**
	 * Determines if the baseUnit unit has been retrieved by the system
	 */
	public abstract boolean isRetrieved (baseUnit unit);
	
	/**
	 * Determines if we have exhausted our data source and should stop
	 */
	public abstract boolean exhausted ();
	
	/**
	 * Returns the next base unit in our data
	 */
	public abstract baseUnit getNextUnit ();
	
	/**
	 * Sets up our data source for the acquisition of new data
	 */
	public void addData (dataSource d) {
		data = d;
	}
	
	/**
	 * Processes the data provided by our data source.
	 */
	public abstract void processData (dataSource data);
}
