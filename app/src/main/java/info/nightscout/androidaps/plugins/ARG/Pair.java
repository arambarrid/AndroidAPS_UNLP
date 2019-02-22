package info.nightscout.androidaps.plugins.ARG;


import java.util.Comparator;
import java.lang.Long;
import java.lang.Double;

public class Pair extends Object  {
	private long t;
	private long t2;
	private double v;
	
	
	public long time() {
		return t;
	}
	
	public long endTime() {
		return t2;
	}
	
	public double value() {
		return v;
	}
	
	public void put(long time, double value) {
		t = time;
		t2 = 0;
		v = value;
	}
	
	public void put(long startTime, long endTime){
		t = startTime;
		t2 = endTime;
		v = 0;
	}
}
