package info.nightscout.androidaps.plugins.ARG;

import java.util.Objects;

public class Safe {
	
	private double gamma;        // Ganancia modulaci´n acci´n de control
	private double kDia;         // Constante asociada al DIA
	private double ts;           // Tiempo de muestreo
	private Filter iob;          // Filtro para estimaci´n del IOB y su derivada
	private double iobMax;       // L´mite de IOB
	private double iobMaxSmall;  // T´rmino del l´mite de IOB para comidas chicas
	private double iobMaxMedium; // T´rmino del l´mite de IOB para comidas medianas
	private double iobMaxBig;    // T´rmino del l´mite de IOB para comidas grandes
	private double iobMaxBasal;  // T´rmino del l´mite de IOB por infusi´n basal
	private double tau;          // Peso sobre la derivada del IOB en la funci´n de sliding (SE SETEA EN 0)
	private Filter wFilter;      // Filtro para suavizar la se´al w (NO SE UTILIZA)
	private double w;            // Se´al l´gica
	private double IOBMaxCF;     // Corrimiento del IOB m´ximo por el BAC pre-comida
	
	/****************************************************************************************************************/
	
	// Constructor
	
	public Safe(double parameterKDia, double parameterTs, double weight, double uBasal, double CR, double parameterTau, double wFilterEdgeFreq, SLQGController slqg){
		
		/****************************************************************************************************************/
		
		gamma    = 1.0;
		kDia     = parameterKDia; 
		ts       = parameterTs;
		tau      = parameterTau;
		w        = 1.0;
		IOBMaxCF = 0.0;
		
		/****************************************************************************************************************/
		
		// El modelo tiene 2 salidas: el IOB estimado y su derivada. Sin embargo, el IOB estimado est´ en [pmol/kg], mientras
		// que la derivada en [U/min]
		
		double[][] aTemp = {{Math.exp(-ts*kDia), 0.0, 0.0},{ts*kDia*Math.exp(-ts*kDia), Math.exp(-ts*kDia), 0.0},{weight/(ts*6000.0), weight/(ts*6000.0), 0.0}};
		double[][] bTemp = {{-(Math.exp(-ts*kDia)-1)/kDia},{-(Math.exp(-ts*kDia)+ts*kDia*Math.exp(-ts*kDia)-1)/kDia},{0}};
		double[][] cTemp = {{1.0, 1.0, 0.0},{weight/(ts*6000.0), weight/(ts*6000.0), -1}};
		double[][] dTemp = {{0},{0}};
		
		Matrix aIob = new Matrix(aTemp);
		Matrix bIob = new Matrix(bTemp);
		Matrix cIob = new Matrix(cTemp);
		Matrix dIob = new Matrix(dTemp);
		
		/****************************************************************************************************************/
		
		// En esta versi´n JAVA se considera que se inicia el modelo en el valor basal. En la versi´n DiAs la inicializaci´n
		// depende del valor informado por el paciente
		
		double[][] uTemp = {{uBasal*100.0/weight}};
		Matrix u         = new Matrix(uTemp); // [pmol/kg/min]
		Matrix xIobIni   = Matrix.identity(aIob.getM()).minus(aIob).solve(bIob).times(u);
		
		iob = new Filter(aIob,bIob,cIob,dIob,xIobIni);
		
		/****************************************************************************************************************/
		
		// Se setea el l´mite de IOB inicial, por eso se toma en cuenta una comida chica (1 primer argumento)
		// y sin BAC (´ltimo argumento 0.0)
		
		this.setIobLimit(1, CR, weight, uBasal,slqg); 
		
		this.setIobMax(iobMaxSmall); // Se define el IOB m´ximo inicial igual al asociado a una comida chica
		
		/****************************************************************************************************************/
		
		// Filtro de primer orden para filtrar la se´al de conmutaci´n w. NO SE UTILIZA, pero se mantiene su definici´n
		
		double[][] aTemp1 = {{Math.exp(-ts*wFilterEdgeFreq)}};
		double[][] bTemp1 = {{1-Math.exp(-ts*wFilterEdgeFreq)}};
		double[][] cTemp1 = {{1}};
		double[][] dTemp1 = {{0}};
		
		Matrix aWF = new Matrix(aTemp1);
		Matrix bWF = new Matrix(bTemp1);
		Matrix cWF = new Matrix(cTemp1);
		Matrix dWF = new Matrix(dTemp1);
		
		double[][] xTemp = {{gamma}};
		Matrix xWFIni    = new Matrix(xTemp);
		wFilter          = new Filter(aWF,bWF,cWF,dWF,xWFIni);
		
		/****************************************************************************************************************/
					
	}
	
	/****************************************************************************************************************/
	
	public void run(double u, int controllerTs, double weight){
			
		double wSum   = 0.0;
		double iobEst = 0.0;
		
		for(int ii = 0; ii < controllerTs/ts; ++ii){
			
			/****************************************************************************************************************/
			
			// Estimo el IOB (como no se va utilizar su derivada para la l´gica de sliding no la obtengo, sin embargo est´
			// disponible a la salida de este modelo)

			iobEst = this.getIobEst(weight);
			
			/****************************************************************************************************************/
			
			// Funci´n de sliding
			
			double sigma = this.iobMax-iobEst;
			
			/****************************************************************************************************************/
			
			// L´gica de switching
			
			w = 0.0;
			
			if(sigma>0.0){
				
				w = 1.0;
			}
			
			/****************************************************************************************************************/
			
			wSum +=w; // Acumulo la suma para luego calcular el w promedio
			
			/****************************************************************************************************************/
			
			// Actualizo el IOB
			
			double[][] uTemp = {{w*u/weight}};
			Matrix iobInput  = new Matrix(uTemp);
			this.iob.stateUpdate(iobInput);
			
			/****************************************************************************************************************/
			
			// Actualizaci´n del filtro de primer orden (NO SE UTILIZA)
			
			// Salida
			
			double[][] wTemp      = {{0.0}};
			Matrix wInput         = new Matrix(wTemp);
			Matrix gammaTemp      = this.wFilter.outputUpdate(wInput);
			double[][] gammaTemp1 = gammaTemp.getData();
			//gammaSum += gammaTemp1[0][0];
			
			// Estado
			
			double[][] wTemp1 = {{w}};
			wInput            = new Matrix(wTemp1);
			this.wFilter.stateUpdate(wInput);
			
		}
		
		//gamma = gammaSum/(controllerTs/ts); // Defino el gamma como la salida del filtro de primer orden (NO SE UTILIZA)
		
		gamma = wSum/(controllerTs/ts); // Calculo el gamma como el promedio de w en los pr´ximos 5 min
		
	}
	
	/****************************************************************************************************************/
	
	public double getIobEst(double weight){
		
		Matrix iobInput     = new Matrix(1,1); // Tomo entrada 0, ya que la matriz D es nula
		Matrix iobTemp      = this.iob.outputUpdate(iobInput);
		double[][] iobTemp1 = iobTemp.getData();
		double iobEst       = iobTemp1[0][0]*weight/6000.0;
		return iobEst;
		
	}
	
	/****************************************************************************************************************/
	
	public boolean hypoProtect(int mCount, double iobEst, double trend, double gEst, double setPoint, SLQGController slqg, double cgm){
		
		boolean hypoFlag = false;
		
		if(cgm<60){
			this.iobMax = 0*iobMaxBasal;
			hypoFlag    = true;
		}
		else if(cgm<70){
			this.iobMax = 0.5*iobMaxBasal;
			hypoFlag    = true;
		}
		else{
			hypoFlag = slqg.hypoProtect(mCount, iobEst, trend, gEst, setPoint, slqg, this);
		}
		return hypoFlag;
		
	}
	
	/****************************************************************************************************************/
	
	public void setIobLimit(int mealClass, double CR, double weight, double uBasal,SLQGController slqg){
		
		/****************************************************************************************************************/
		
		// C´lculo del IOB asociado a la infusi´n basal de insulina
		
		double[][] uTemp = {{uBasal*100.0/weight}};
		Matrix u         = new Matrix(uTemp); // [pmol/kg/min]
		Matrix xIobIni   = Matrix.identity(iob.getAMatrix().getM()).minus(iob.getAMatrix()).solve(iob.getBMatrix()).times(u);
	
		Matrix iobTemp = this.iob.getCMatrix().times(xIobIni);
		double[][] iobTemp1 = iobTemp.getData();
		iobMaxBasal = iobTemp1[0][0]*weight/6000.0;
		
		/****************************************************************************************************************/
		
		// Definici´n de l´mites
		
		double gCHOs      = 40;
		double As         = gCHOs/CR;
		this.iobMaxSmall  = As+this.iobMaxBasal;
		
		double gCHOm      = 55;
		double Am         = gCHOm/CR;
		this.iobMaxMedium = Am+this.iobMaxBasal;
		
		double gCHOb      = 70;
		double Ab         = gCHOb/CR;
		this.iobMaxBig    = Ab+this.iobMaxBasal;
		
		/****************************************************************************************************************/
		
		// Seteo del l´mite y correcci´n del l´mite en caso de un BAC de comida
				
		slqg.setIobLimit(mealClass,this);
		
	}
	

	/****************************************************************************************************************/
	// TODO_APS: esto no esta en JavaVersion, pero si en DiASVersion
	
	public double getIobBasal(double uBasal, double weight){
		
		double[][] uTemp = {{uBasal*100.0/weight}};
		Matrix u         = new Matrix(uTemp); // [pmol/kg/min]
		Matrix xIobIni   = Matrix.identity(iob.getAMatrix().getM()).minus(iob.getAMatrix()).solve(iob.getBMatrix()).times(u);
	
		Matrix iobTemp      = this.iob.getCMatrix().times(xIobIni);
		double[][] iobTemp1 = iobTemp.getData();
		double iobBasal     = iobTemp1[0][0]*weight/6000.0;
		
		return iobBasal;
		
	}

	/****************************************************************************************************************/
	
	// GETTERS Y SETTERS

	public double getGamma(){
		
		return gamma;
		
	}

	public double getkDia() {
		
		return kDia;
		
	}

	public double getTs() {
		
		return ts;
		
	}

	public Filter getIob() {
		
		return iob;
		
	}

	public double getIobMaxBasal() {
		
		return iobMaxBasal;
		
	}
	
	public double getIobMax() {
		
		return iobMax;
		
	}
	
	public double getIobMaxSmall() {
		
		return iobMaxSmall;
		
	}
	
	public double getIobMaxMedium() {
		
		return iobMaxMedium;
		
	}
	
	public double getIobMaxBig() {
		
		return iobMaxBig;
		
	}


	public double getTau() {
		
		return tau;
		
	}


	public Filter getwFilter() {
		
		return wFilter;
		
	}


	public double getW() {
		
		return w;
		
	}

	public void setGamma(double gamma) {
		
		this.gamma = gamma;
		
	}

	public void setkDia(double kDia) {
		
		this.kDia = kDia;
		
	}

	public void setTs(double ts) {
		
		this.ts = ts;
		
	}

	public void setIob(Filter iob) {
		
		this.iob = iob;
		
	}

	public void setIobMax(double iobMax) {
		
		this.iobMax = iobMax;
		
	}

	public void setIobMaxSmall(double iobMaxSmall) {
		
		this.iobMaxSmall = iobMaxSmall;
		
	}

	public void setIobMaxMedium(double iobMaxMedium) {
		
		this.iobMaxMedium = iobMaxMedium;
		
	}

	public void setIobMaxBig(double iobMaxBig) {
		
		this.iobMaxBig = iobMaxBig;
		
	}

	public void setIobMaxBasal(double iobMaxBasal) {
		
		this.iobMaxBasal = iobMaxBasal;
		
	}

	public void setTau(double tau) {
		
		this.tau = tau;
		
	}

	public void setwFilter(Filter wFilter) {
		
		this.wFilter = wFilter;
		
	}

	public void setW(double w) {
		
		this.w = w;
		
	}

	public double getIOBMaxCF() {
		
		return IOBMaxCF;
		
	}

	public void setIOBMaxCF(double iOBMaxCF) {
		
		IOBMaxCF = iOBMaxCF;
		
	}

}
