package info.nightscout.androidaps.plugins.ARG;

public interface SLQGState {

	void show();
	void updateLQG(SLQGController slqgController);
	String getStateString();
	void updateExtAgg(SLQGController slqgController);
	double standardCB(boolean hypoFlag, GController gController);
	boolean hypoProtect(int mCount, double iobEst, double trend, double gEst, double setPoint, SLQGController slqg, Safe safe);
	void setIobLimit(int mClass, Safe safe);
	
}