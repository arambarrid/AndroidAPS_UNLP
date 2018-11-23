package info.nightscout.androidaps.plugins.ARG;

public class Patient {
	
	private double tdi;
	private double cr;
	private double cf;
	private double weight;
	private double fastingG; // Setpoint definido
	private double basalU; // [U/h]

	/**************************************************************************************************************/
	
	// Constructor
	
	public Patient(double parameterTdi, double parameterCr, double parameterCf, double parameterWeight, double parameterFastingG, double parameterBasalU){
		
		tdi      = parameterTdi;
		cr       = parameterCr;
		cf       = parameterCf;
		weight   = parameterWeight;
		fastingG = parameterFastingG;
		basalU   = parameterBasalU;
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS y SETTERS
	
	public double getTdi() {
		
		return tdi;
		
	}

	public void setTdi(double tdi) {
		
		this.tdi = tdi;
		
	}

	public double getCr() {
		
		return cr;
		
	}

	public void setCr(double cr) {
		
		this.cr = cr;
		
	}

	public double getCf() {
		
		return cf;
		
	}

	public void setCf(double cf) {
		
		this.cf = cf;
		
	}

	public double getWeight() {
		
		return weight;
		
	}

	public void setWeight(double weight) {
		
		this.weight = weight;
		
	}

	public double getFastingG() {
		
		return fastingG;
		
	}

	public void setFastingG(double fastingG) {
		
		this.fastingG = fastingG;
		
	}

	public double getBasalU() {
		
		return basalU;
		
	}

	public void setBasalU(double basalU) {
		
		this.basalU = basalU;
		
	}
	
}

