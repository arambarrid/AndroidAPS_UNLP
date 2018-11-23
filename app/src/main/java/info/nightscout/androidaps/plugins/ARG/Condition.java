package info.nightscout.androidaps.plugins.ARG;

public class Condition {

	private boolean[] condition;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public Condition(boolean[] parameterCondition){
		
		condition = parameterCondition;
		
	}
	
	/**************************************************************************************************************/
	
	public boolean equals(Condition condition1){
		
		/**************************************************************************************************************/
		
		// Si los vectores de condiciones no coinciden en tama´o se devuelve false
		
		if(condition.length!=condition1.getCondition().length){
			return false;
		}
		
		/**************************************************************************************************************/
		
		// Se verifica si cada elemento del vector de condici´n coincide con la condici´n pasada
		
		boolean result = true;
		
		int i = 0;
		
		while(result == true && i <= condition.length-1){
			
			result = condition[i] == condition1.getCondition()[i];
			++i;
			
		}
		
		return result;
		
	}

	/**************************************************************************************************************/
	
	// GETTERS Y SETTERS
	
	public boolean[] getCondition() {
		
		return condition;
		
	}
	
	public void setCondition(boolean[] condition) {
		
		this.condition = condition;
		
	}
	
}
