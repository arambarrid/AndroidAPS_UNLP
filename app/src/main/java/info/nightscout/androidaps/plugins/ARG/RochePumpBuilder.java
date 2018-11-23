package info.nightscout.androidaps.plugins.ARG;

public class RochePumpBuilder implements PumpBuilder{
	
	private Pump pump;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public RochePumpBuilder(){
		
		pump = new Pump();
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void buildPumpMinBasal() {

		pump.setPumpMinBasal(0.0);
		
	}

	/**************************************************************************************************************/
	
	@Override
	public void buildPumpMaxBasal() {
		
		pump.setPumpMaxBasal(25.0);
		
	}

	/**************************************************************************************************************/
	
	@Override
	public void buildPumpIncBasal() {

		pump.setPumpIncBasal(0.05);
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void buildPumpMinBolus() {

		pump.setPumpMinBolus(0.0);
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void buildPumpMaxBolus() {

		pump.setPumpMaxBolus(25.0);
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void buildPumpIncBolus() {

		pump.setPumpIncBolus(0.1);
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS  y SETTERS
	
	@Override
	public Pump getPump() {

		return pump;
		
	}

}
