package info.nightscout.androidaps.plugins.ARG;

public interface PumpBuilder{
	
	public void buildPumpMinBasal();
	
	public void buildPumpMaxBasal();
	
	public void buildPumpIncBasal();
	
	public void buildPumpMinBolus();
	
	public void buildPumpMaxBolus();
	
	public void buildPumpIncBolus();
	
	public Pump getPump();
	
}
