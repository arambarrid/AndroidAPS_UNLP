package info.nightscout.androidaps.plugins.ARG;

public class On implements NSFState {
	
	NSF nsf;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public On(NSF newNSF){
		
		nsf = newNSF;
		
	}
	
	/**************************************************************************************************************/
	
	public double run(double cgm, int controllerTs) {
		
		double gF = 0.0;
		
		if(Math.abs(cgm-nsf.getgPrev())<=nsf.getgROCLimit()*controllerTs){
			gF = cgm;
		}
		else if(nsf.getgPrev()-cgm>nsf.getgROCLimit()*controllerTs){
			gF = nsf.getgPrev()-nsf.getgROCLimit()*controllerTs;
		}
		else{
			gF = nsf.getgPrev()+nsf.getgROCLimit()*controllerTs;
		}
		
		nsf.setgPrev(gF);
		return gF;
				
	}
	
	/**************************************************************************************************************/
	
	public void show(){
		
		System.out.println("On");
		
	}

}
