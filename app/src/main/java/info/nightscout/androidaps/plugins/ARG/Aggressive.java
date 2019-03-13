package info.nightscout.androidaps.plugins.ARG;

public class Aggressive implements SLQGState {
	
	private static Aggressive firstInstance = null;
	private Matrix cAgg;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	private Aggressive(){
		
		cAgg = new Matrix(1,13);

	}
	
	/**************************************************************************************************************/
	
	public static Aggressive getInstance(){
		
		if(firstInstance == null){
			
			firstInstance = new Aggressive();
		}
		
		return firstInstance;
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void updateLQG(SLQGController slqgController) {
		
		slqgController.getLqg().setCMatrix(cAgg); // Seteo la csigma del modo agresivo
		slqgController.settMeal(slqgController.gettMeal()+slqgController.getTs()); // El contador asociado al tiempo del modo agresivo activo se actualiza
		
	}
	
	/**************************************************************************************************************/
	
	public double standardCB(boolean hypoFlag, GController gController){
		
		return 0.0;
		
	}

	/**************************************************************************************************************/

	public void updateExtAgg(SLQGController slqgController){
		
	}
	
	/**************************************************************************************************************/
	
	public boolean hypoProtect(int mCount, double iobEst, double trend, double gEst, double setPoint, SLQGController slqg, Safe safe){
		
		return false;
		
	}
	
	/**************************************************************************************************************/
	
	public void setIobLimit(int mClass, Safe safe, double iobFactor){
		
		double iobMaxAux = 0.0;
		if(mClass == 1){
			iobMaxAux = safe.getIobMaxSmall()+safe.getIOBMaxCF();
		} else if(mClass == 2){
			iobMaxAux = safe.getIobMaxMedium()+safe.getIOBMaxCF();
		} else if(mClass == 3){
			iobMaxAux = safe.getIobMaxBig()+safe.getIOBMaxCF();
		}
		
		safe.setIobMax(iobFactor*iobMaxAux);

	}
	
	/**************************************************************************************************************/
	
	public void show(){
		
		System.out.println("Aggressive");
		
	}
	
	/**************************************************************************************************************/
	
	public String getStateString(){
		
		return(new String("Aggressive"));
		
	}

	/**************************************************************************************************************/
	
	// GETTERS Y SETTERS
	
	public Matrix getcAgg() {
		return cAgg;
	}

	public void setcAgg(Matrix cAgg) {
		this.cAgg = cAgg;
	}

}
