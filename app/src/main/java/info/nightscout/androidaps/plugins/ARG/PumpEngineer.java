package info.nightscout.androidaps.plugins.ARG;

public class PumpEngineer{

	private PumpBuilder pumpBuilder;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public PumpEngineer(PumpBuilder pumpBuilder){
		
		this.pumpBuilder = pumpBuilder;
		
	}
	
	/**************************************************************************************************************/
	
	public Pump getPump(){
		
		return this.pumpBuilder.getPump();
		
	}
	
	/**************************************************************************************************************/
	
	public void makePump(){
		
		// Seteo las caracter´sticas de la bomba
		
		this.pumpBuilder.buildPumpMinBasal();
		this.pumpBuilder.buildPumpMaxBasal();
		this.pumpBuilder.buildPumpIncBasal();
		this.pumpBuilder.buildPumpMinBolus();
		this.pumpBuilder.buildPumpMaxBolus();
		this.pumpBuilder.buildPumpIncBolus();
		
	}
	
}
