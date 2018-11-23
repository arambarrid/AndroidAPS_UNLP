package info.nightscout.androidaps.plugins.ARG;

public interface GControllerState {
	
	double run(boolean mealFlag, int mealClass, double cgm);
	double preMealCB(boolean mealFlag);
	double standardCB(boolean hypoFlag);
	void updateBolusVar();
	void show(); 
	
}
