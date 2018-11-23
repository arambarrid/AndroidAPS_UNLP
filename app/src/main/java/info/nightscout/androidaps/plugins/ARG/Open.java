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
	public double run(boolean mealFlag, int mealClass, double cgm) {
		
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
		
		double pcb = gController.getpCBolus(); // Bolo anterior por cuantizaci´n no infundido
		
		double basalBolus = gController.getPump().quantizeBolus(gController.getPatient().getBasalU()/12.0+pcb); // Bolo asociado a la infusi´n basal cuantizado
		
		gController.setpCBolus(gController.getPatient().getBasalU()/12.0+pcb-basalBolus); // Calculo el bolo que por cuantizaci´n no inyect´ y lo guardo para la pr´xima iteraci´n
		
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

}
