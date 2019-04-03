package info.nightscout.androidaps.plugins.ARG;

public class Open implements GControllerState {
	
	private GController gController;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public Open(GController newGController){
		
		gController = newGController;
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public double run(boolean mealFlag, int mealClass, double cgm, double iobFactor) {
		
		/**************************************************************************************************************/
		
		// Cuando se cumple el tiempo preestablecido se conmuta a lazo-cerrado
		
		if(gController.getItNumber()>=gController.getOlTime()/gController.getSlqgController().getTs() && gController.getItNumber()>=gController.getEstimator().getCgmVector().getM()){
			gController.setgControllerState(gController.getClosed());
		}
		
		gController.increaseItNumber();
		
		/**************************************************************************************************************/
		
		// C´lculo de la infusi´n basal
		
		double cgmF = gController.getNsf().run(cgm, gController.getSlqgController().getTs());
		gController.getEstimator().insert(cgmF);
		
		double pcb = gController.getpCBolus(); // Bolo anterior por cuantización no infundido
		
		double basalBolus = gController.getPump().quantizeBolus(gController.getPatient().getBasalU()/12.0+pcb); // Bolo asociado a la infusión basal cuantizado
		
		gController.setpCBolus(gController.getPatient().getBasalU()/12.0+pcb-basalBolus); // Calculo el bolo que por cuantización no inyecté y lo guardo para la próxima iteración
		
		return basalBolus;		
		
	}

	/**************************************************************************************************************/
	
	public double preMealCB(boolean mealFlag) {
		return 0.0;
	}
	
	/**************************************************************************************************************/
	
	public double standardCB(boolean hypoFlag) {
		return 0.0;
	}
	
	/**************************************************************************************************************/
	
	public void updateBolusVar(){
		
	}
	
	/**************************************************************************************************************/
		
	@Override
	public void show() {
		
		System.out.println("Open-Loop");
		
	}
	
	/**************************************************************************************************************/


	public double getBACs(){
		return 0.0;
	}
}
