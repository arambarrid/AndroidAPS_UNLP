package info.nightscout.androidaps.plugins.ARG;

public class NSF {
	
	private double gROCLimit; // M´xima variaci´n de glucosa permitida
	private double gPrev;     // Glucosa previa

	private NSFState on;
	private NSFState off;
	private NSFState nsfState;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public NSF(double parameterGROCLimit){
	
		gROCLimit = parameterGROCLimit; // M´xima velocidad de cambio de glucosa fisiol´gica [mg/dl/min]
		gPrev     = 0.0;
		on        = new On(this);
		off       = new Off(this);
		
		// Se define como estado inicial Off para que el proceso de limitaci´n no comience en la primera muestra donde
		// no tiene sentido, sino en la segunda
		
		nsfState  = off;
		
	}
	
	/**************************************************************************************************************/
	
	public double run(double cgm, int controllerTs){
		
		return nsfState.run(cgm, controllerTs);
		
	}

	/**************************************************************************************************************/
	
	// GETTERS y SETTERS
	
	public void setNSFState(NSFState newNSFState){
		
		nsfState = newNSFState;
		
	}
	
	public void setgROCLimit(double gROCLimit) {
		
		this.gROCLimit = gROCLimit;
		
	}

	public void setgPrev(double gPrev) {
		
		this.gPrev = gPrev;
		
	}

	public void setNsfState(NSFState nsfState) {
		
		this.nsfState = nsfState;
		
	}

	public double getgROCLimit() {
		
		return gROCLimit;
		
	}

	public double getgPrev() {
		
		return gPrev;
		
	}

	public NSFState getOn() {
		
		return on;
		
	}

	public NSFState getOff() {
		
		return off;
		
	}

	public NSFState getNsfState() {
		
		return nsfState;
		
	}

}
