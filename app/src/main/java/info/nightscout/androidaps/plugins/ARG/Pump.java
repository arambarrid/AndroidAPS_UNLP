package info.nightscout.androidaps.plugins.ARG;

public class Pump implements PumpPlan{

	private double pumpMinBasal;
	private double pumpMaxBasal;
	private double pumpIncBasal;
	
	private double pumpMinBolus;
	private double pumpMaxBolus;
	private double pumpIncBolus;
	
    /**************************************************************************************************************/
    
    public double saturateBolus(double u){
		
		double output = 0.0;
		
		if(u>pumpMaxBolus){
			output = pumpMaxBolus;
		}else if(u<pumpMinBolus){
			output = pumpMinBolus;
		}else{
			output = u;
		}
		
		return output;
		
    }
	
	/**************************************************************************************************************/
	
	public double quantizeBolus(double u){
		
		return pumpIncBolus*Math.floor(u/(pumpIncBolus)); // Se aplica un flooring
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS Y SETTERS

	public double getPumpMinBasal() {
		
		return pumpMinBasal;
		
	}

	public void setPumpMinBasal(double pumpMinBasal) {
		
		this.pumpMinBasal = pumpMinBasal;
		
	}

	public double getPumpMaxBasal() {
		
		return pumpMaxBasal;
		
	}

	public void setPumpMaxBasal(double pumpMaxBasal) {
		
		this.pumpMaxBasal = pumpMaxBasal;
		
	}

	public double getPumpIncBasal() {
		
		return pumpIncBasal;
		
	}

	public void setPumpIncBasal(double pumpIncBasal) {
		
		this.pumpIncBasal = pumpIncBasal;
		
	}

	public double getPumpMinBolus() {
		
		return pumpMinBolus;
		
	}

	public void setPumpMinBolus(double pumpMinBolus) {
		
		this.pumpMinBolus = pumpMinBolus;
		
	}

	public double getPumpMaxBolus() {
		
		return pumpMaxBolus;
		
	}

	public void setPumpMaxBolus(double pumpMaxBolus) {
		
		this.pumpMaxBolus = pumpMaxBolus;
		
	}

	public double getPumpIncBolus() {
		
		return pumpIncBolus;
		
	}

	public void setPumpIncBolus(double pumpIncBolus) {
		
		this.pumpIncBolus = pumpIncBolus;
		
	}
	
}