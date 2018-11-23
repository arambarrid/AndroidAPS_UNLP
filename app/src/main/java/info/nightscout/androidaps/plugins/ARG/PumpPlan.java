package info.nightscout.androidaps.plugins.ARG;

public interface PumpPlan{
	
	public void setPumpMinBasal(double min);
	
	public void setPumpMaxBasal(double max);

	public void setPumpIncBasal(double inc);
	
	public void setPumpMinBolus(double min);
	
	public void setPumpMaxBolus(double max);

	public void setPumpIncBolus(double inc);
	
}
