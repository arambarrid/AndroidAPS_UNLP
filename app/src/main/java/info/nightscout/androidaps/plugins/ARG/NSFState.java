package info.nightscout.androidaps.plugins.ARG;

public interface NSFState {
	
	void show();
	double run(double cgm, int controllerTs);

}
