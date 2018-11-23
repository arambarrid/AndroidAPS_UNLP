package info.nightscout.androidaps.plugins.ARG;

public class TestNSF {

	public static void main(String[] args) {

		double gROCLimit = 3.0;
		NSF nsf = new NSF(gROCLimit);
		nsf.getNsfState().show();
		
		double cgm = 110.0;
		int controllerTs = 5;
		
		double gF = 0.0;
		
		gF = nsf.run(cgm, controllerTs);
		
		System.out.println("Filtered glucose: " + gF);
		
		nsf.getNsfState().show();
		
		cgm = 130.0;
		
		gF = nsf.run(cgm, controllerTs);
		
		System.out.println("Filtered glucose: " + gF);
		
		nsf.getNsfState().show();
		
		cgm = 140.0;
		
		gF = nsf.run(cgm, controllerTs);
		
		System.out.println("Filtered glucose: " + gF);
		
		nsf.getNsfState().show();
	}

}
