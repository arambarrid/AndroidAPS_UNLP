package info.nightscout.androidaps.plugins.ARG;

public class SLQGTransition {

	private SLQGState from;
	private Condition condition;
	private SLQGState to;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public SLQGTransition(SLQGState fromState,Condition cond,SLQGState toState){
		
		from = fromState; // Estado actual
		condition = cond; // Condici´n
		to = toState;     // Estado siguiente
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS y SETTERS

	public SLQGState getFrom() {
		
		return from;
		
	}

	public void setFrom(SLQGState from) {
		
		this.from = from;
		
	}

	public Condition getCondition() {
		
		return condition;
		
	}

	public void setCondition(Condition condition) {
		
		this.condition = condition;
		
	}

	public SLQGState getTo() {
		
		return to;
		
	}

	public void setTo(SLQGState to) {
		
		this.to = to;
		
	}
	
}
