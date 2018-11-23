package info.nightscout.androidaps.plugins.ARG;

public class Conservative implements SLQGState {
	
	private static Conservative firstInstance = null;
	private Matrix cCon;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	private Conservative(){
		
		cCon = new Matrix(1,13);
		
	}
	
	/**************************************************************************************************************/
	
	public static Conservative getInstance(){
		
		if(firstInstance == null){
			
			firstInstance = new Conservative();
		}
		
		return firstInstance;
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void updateLQG(SLQGController slqgController) {

		slqgController.getLqg().setCMatrix(cCon); // Seteo la csigma del modo conservador
		slqgController.settMeal(0); // El contador asociado al tiempo del modo agresivo activo se reinicia
		
	}
	
	/**************************************************************************************************************/
	
	public void updateExtAgg(SLQGController slqgController){
		
		if(slqgController.getExtAgg()>0){
			slqgController.setExtAgg(slqgController.getExtAgg()-slqgController.getTs());
		}
		
	}
	
	/**************************************************************************************************************/
	
	public double standardCB(boolean hypoFlag, GController gController){
		
		double cfBolus = 0.0;
		
		if(gController.getEstimator().getMCount()==0){ // Verifico que no est´ en modo listening
			
			// Verifico que no se haya activado el flag de la capa de hipo y que los timers del BAC anterior est´n en 0
			
			if(!hypoFlag && gController.getrCFBolus()==0 && gController.gettEndAgg() == 0){
				
				// Generaci´n de condiciones para aplicaci´n del BAC
				
				/**************************************************************************************************************/
				
				// Datos del vector de 30 min
				
				double[][] cVector1 = gController.getEstimator().getCgmVector().getData();
				double trend1 = gController.getEstimator().getTrend();
				double pred1  = gController.getEstimator().getPred();
				
				/**************************************************************************************************************/
				
				// Armo un estimador con las ´ltimas 3 muestras
				
				Estimator estimator1 = new Estimator(3,gController.getSlqgController().getTs());
				
				for(int ii = 0; ii < 3; ++ii){
					estimator1.insert(cVector1[cVector1.length-1-2+ii][0]);
				}
							
				double trend2 = estimator1.getTrend();
				
				/**************************************************************************************************************/
				
				// Verifico si todas las muestras del vector de 30 min est´n por encima de 160 mg/dl
				
				int nSamples = 0;
				for(int ii = 0; ii < gController.getEstimator().getCgmVector().getM(); ++ii){
					if(cVector1[ii][0]>160.0){ 
						nSamples += 1;
					}
				}
				
				/**************************************************************************************************************/
				
				double gMean = gController.getEstimator().getMean();
				
				if((nSamples==gController.getEstimator().getCgmVector().getM() && trend1>=-0.1 && trend2>=-0.5) || gMean>200.0){
					cfBolus = (Math.min(gMean, pred1)-gController.getSetpoint())/gController.getPatient().getCf();
					if(cfBolus<=0){
						cfBolus = 0.0;
					}
					else{
						cfBolus = 0.8*cfBolus;
						gController.setrCFBolus(24); // Seteo la prohibici´n de generaci´n de otros BACs por 2 hs
					}
				}
			}
		}
		
		return cfBolus;
		
	}

	/**************************************************************************************************************/
	
	public boolean hypoProtect(int mCount, double iobEst, double trend, double gEst, double setPoint, SLQGController slqg, Safe safe){
		
		boolean hypoFlag = false;
		
		if(mCount==0){
			
			if(trend<-0.5 || (iobEst>=safe.getIobMaxBasal() && trend<0.5)){
				
				if(gEst<70){
					safe.setIobMax(0.5*safe.getIobMaxBasal());
					hypoFlag = true;
				}
				else if(gEst<100){
					safe.setIobMax(0.75*safe.getIobMaxBasal());
					hypoFlag = true;
				}
				else if(gEst<setPoint){	
					safe.setIobMax(safe.getIobMaxBasal());
					hypoFlag = true;
				}
				else {
					if(slqg.getExtAgg()==0){
						safe.setIobMax(safe.getIobMaxSmall());
					}
				}
			} 
			else {
				if(slqg.getExtAgg()==0){
					safe.setIobMax(safe.getIobMaxSmall());
				}
			}
		} 
		else{
			safe.setIobMax(safe.getIobMaxSmall());
		}			
		
		return hypoFlag;
		
	}
	
	/**************************************************************************************************************/
	
	public void setIobLimit(int mClass, Safe safe){
		
	}
	
	/**************************************************************************************************************/
	
	public void show(){
		
		System.out.println("Conservative");
		
	}
	
	/**************************************************************************************************************/
	
	public String getStateString(){
		
		return(new String("Conservative"));
		
	}

	/**************************************************************************************************************/

	// GETTERS Y SETTERS
	
	public Matrix getcCon() {
		return cCon;
	}

	public void setcCon(Matrix cCon) {
		this.cCon = cCon;
	}
	
}