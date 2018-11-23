package info.nightscout.androidaps.plugins.ARG;

public class Off implements NSFState {
	
	NSF nsf;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public Off(NSF newNSF){
		
		nsf = newNSF;
		
	}
	
	/**************************************************************************************************************/
	
	public double run(double cgm, int controllerTs) {
		
		// Guardo la primera muestra y paso a estado On para la siguiente iteraci´n
		
		nsf.setgPrev(cgm);
		nsf.setNSFState(nsf.getOn());
		return cgm;
				
	}
	
	/**************************************************************************************************************/
	
	public void show(){
		
		System.out.println("Off");
		
	}

}