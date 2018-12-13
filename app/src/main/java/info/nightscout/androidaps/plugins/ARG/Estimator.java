package info.nightscout.androidaps.plugins.ARG;

public class Estimator {

	private Matrix cgmVector; // Vector de muestras de CGM
	private Matrix mLs;       // Matriz utilizada para hacer la interpolaci´n lineal
	private int step;		  // Tiempo entre muestra y muestra [min]
	private double trend;     // Pendiente
	private int mCount;       // Flag inicial anuncio de comida (es int porque se puede regular el tiempo para pasar a listening)
	private int listening;    // Contador asociado al modo listening
	private double pred;      // Predicci´n de glucosa
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public Estimator(int parameterNSamples, int controllerTs){
		
		/**************************************************************************************************************/
		
		// El vector inicial se setea en 0
		
		cgmVector = new Matrix(parameterNSamples,1);
		
		/**************************************************************************************************************/
		
		// Construcci´n matriz para estimaci´n
		
		Matrix V  = new Matrix(parameterNSamples,2);
		
		for(int ii = 0; ii < parameterNSamples; ++ii){
			V.getData()[ii][0] = ii*controllerTs;
		} 
		
		for(int ii = 0; ii < parameterNSamples; ++ii){
			V.getData()[ii][1] = 1;
		}
		
		Matrix auxMatrix = V.transpose().times(V);
		double detDouble = 1/(auxMatrix.getData()[0][0]*auxMatrix.getData()[1][1]-auxMatrix.getData()[0][1]*auxMatrix.getData()[1][0]);
		double[][] auxDouble = {{auxMatrix.getData()[1][1],-auxMatrix.getData()[0][1]},{-auxMatrix.getData()[1][0],auxMatrix.getData()[0][0]}};
		Matrix auxMatrix2 = new Matrix(auxDouble);
		Matrix invMatrix = auxMatrix2.mFactor(detDouble);
		mLs = invMatrix.times(V.transpose());
		
		/**************************************************************************************************************/
		
		step      = controllerTs;
		trend     = 0;
		mCount    = 0;
		listening = 0;
		pred      = 0;
		
	}
	
	/**************************************************************************************************************/
	
	public void insert(double cgmValue){
		
		// La nueva muestra se inserta como ´ltima elemento del vector y la primera se descarta
		
		Matrix auxVector = new Matrix(cgmVector.getM(),1);
		
		for(int ii = 1; ii < cgmVector.getM(); ++ii){
			auxVector.getData()[ii-1][0] = cgmVector.getData()[ii][0];
		}
		
		auxVector.getData()[cgmVector.getM()-1][0] = cgmValue;
		cgmVector = auxVector;
		
		}
	
	/**************************************************************************************************************/
	
	public void updatePred(int hor){
		
		// hor refiere a horizonte no hor. Las unidades de hor son [min]
		
		Matrix li = mLs.times(cgmVector); // El primer elemento de li es la pendiente, el segundo el t´rmino constante
		pred      = li.getData()[0][0]*(step*(cgmVector.getM()-1)+hor)+li.getData()[1][0];
		trend     = li.getData()[0][0];
		
	}
	
	/**************************************************************************************************************/
	
	public boolean aggressiveSwitch(SLQGController slqgController){

		int mSwitch       = 0;
		boolean mealFFlag = false;
		
		// La condici´n mCount>=1 genera que ni bien se anuncia la comida se verifique el trend para la posible conmutaci´n
		
		if(mCount>=1){
			
			// Si hubo un trend superior a 2 mg/dl/5 min durante los ´ltimos 20 min, entonces se conmuta al agresivo
			
			for(int ii = 0; ii < 3; ++ii){
				if((cgmVector.getData()[cgmVector.getM()-1-ii][0]-cgmVector.getData()[cgmVector.getM()-1-ii-1][0])>2){
					mSwitch= mSwitch+1;
				}
			}
			
			if(mSwitch==3){
				mealFFlag = true; // La activaci´n de este flag indica la conmutaci´n al agresivo
			}
			
			// Si se conmuta al agresivo o se estuvo en listening por 90 min o m´s, entonces se reinician las variables asociadas
			// para salir del modo listening
			
			if(mealFFlag || listening>=90){
				mCount    = 0;
				listening = 0;
				if(mealFFlag){
					slqgController.settMeal(0); // El contador se pone en 0 para que el agresivo se mantenga efectivamente por 60 min
				}
			}
			else{
				listening += step;
			}
				
		}
		
		return mealFFlag;
		
	}
	
	/**************************************************************************************************************/
	
	public void activateMCount(boolean mealFlag){
		
		if(mealFlag==true){
			mCount    = 1;
			listening = 0; // listening se pone en 0 para reiniciar el estado de escucha si se anuncia una nueva comida
		}
		
	}
	
	/**************************************************************************************************************/
	
	public void updateMCount(){
		
		if(mCount!=0){
			mCount = mCount+1;
		}
		
	}
	
	/**************************************************************************************************************/
	
	public double getMean(){
		
		double cMean = 0.0;
		double aSum = 0.0;
		double[][] cVector = cgmVector.getData();
		
		for(int ii = 0; ii < cgmVector.getM(); ++ii){
			aSum += cVector[ii][0];
		}
		
		cMean = Math.round(100.0*aSum/cgmVector.getM())/100.0;
		
		return cMean;
	}
	
	/**************************************************************************************************************/
	
	public double getTrend(){
		
		Matrix li = mLs.times(cgmVector);
		trend = li.getData()[0][0];
		return Math.round(100.0*trend)/100.0;
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS Y SETTERS
	
	public Matrix getCgmVector(){
		
		return cgmVector;
		
	}

	public double getPred() {
		
		return Math.round(100.0*pred)/100.0;
		
	}
	
	public int getMCount() {
		
		return mCount;
		
	}
	
	public void setMCount(int mCount) {
		
		this.mCount = mCount;
		
	}

	public int getListening() {
		
		return listening;
		
	}

	public void setListening(int listening) {
		
		this.listening = listening;
		
	}

}