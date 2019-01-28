package info.nightscout.androidaps.plugins.ARG;

import android.content.Context;

import java.util.ArrayList;

public class GController {
	
	private double setpoint;                   // Setpoint deseado
	private Pump pump; 						   // Bomba de insulina
	private Patient patient; 				   // Paciente
	private NSF nsf; 						   // Noise-spike Filter
	private SLQGController slqgController; 	   // SLQG
	private Safe safe; 						   // SAFE
	private GControllerState open; 			   // Estado open-loop (NO SE UTILIZA)
	private GControllerState closed; 		   // Estado closed-loop
	private GControllerState gControllerState; // Estado actual 
	private int itNumber; 					   // N´mero de iteraciones actuales
	private int olTime; 					   // N´mero inicial de iteraciones en open-loop (NO SE UTILIZA)
	private Estimator estimator; 			   // Estimador
	private double pCBolus; 				   // Bolo generado por la cuantizaci´n de la bomba
	private int rCFBolus; 					   // Contador asociado a los BACs
	private int tEndAgg; 					   // Contador asociado a los BACs
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public GController(double parameterSetpoint, double parameterTDI, double parameterCR, double parameterCF, double parameterWeight, double parameterIBasal, Context contexto){
		
		/**************************************************************************************************************/
		
		// Setpoint
		
		setpoint = parameterSetpoint;
		
		/**************************************************************************************************************/
		
		// Pump
		
		// Genero una bomba con las caracter´sticas de la bomba Roche que usaremos en las pruebas
		
		RochePumpBuilder rochePumpBuilder = new RochePumpBuilder();
		PumpEngineer pumpEngineer         = new PumpEngineer(rochePumpBuilder);
		pumpEngineer.makePump();
		Pump rochePump   = pumpEngineer.getPump();
		pump             = rochePump;
		
		/**************************************************************************************************************/
		
		// Patient
		
		patient = new Patient(parameterTDI, parameterCR, parameterCF, parameterWeight, parameterSetpoint, parameterIBasal);
		
		/**************************************************************************************************************/
		
		// NSF Filter
		
		// NO SE UTILIZA, pero se mantiene su definici´n. Se setea con 3 mg/dl/min (la m´xima tasa de cambio fisiol´gica de glucosa).
		
		nsf = new NSF(3.0);
		
		/**************************************************************************************************************/
		
		// Switched LQG
		
		// Para testear con Matlab se definen las matrices iguales a 0. En Matlab luego se setean iguales a las definidas
		// en ctrlsetup.m mediante la funci´n createGController.m
		
		// En la versi´n DiAs esto es diferente, ya que las matrices se cargan del archivo cmatrices.txt
		AndroidFileRead metodo = new AndroidFileRead(13);

		double[][] data = metodo.getData(contexto);

		double[][] Am = metodo.getAMatrix(data);
		Matrix Ak = new Matrix(Am);

		double[][] Bm = metodo.getBMatrix(data);
		Matrix Bk = new Matrix(Bm);

		double[][] CmCons = metodo.getCConsMatrix(data);
		Matrix CkCons = new Matrix(CmCons);

		double[][] CmAgg = metodo.getCAggMatrix(data);
		Matrix CkAgg = new Matrix(CmAgg);

		slqgController = buildSlqgController(Ak, Bk, CkCons, CkAgg);
		
		/**************************************************************************************************************/
		
		// SAFE
		
		double kDia = 16.3e-03;      // Se define un DIA por default de 5 hs
		double ts   = 0.1;           // Tiempo de muestreo [min]
		double tau  = 0.0;           // En la funci´n de sliding no se toma en cuenta la derivada del IOB
		double filterEdgefreq = 0.5; // El filtro de primer orden no se considera m´s, pero se mantiene su definici´n
		
		safe = new Safe(kDia,ts,parameterWeight,parameterIBasal,parameterCR,tau,filterEdgefreq,slqgController);
		
		/**************************************************************************************************************/
				
		open             = new Open(this);
		closed           = new Closed(this);
		gControllerState = closed; // Como el estado open-loop no se utiliza se inicia en closed-loop
		itNumber         = 1;
		olTime           = 0; // Como el estado open-loop no se utiliza se define en 0
		
		/**************************************************************************************************************/
		
		// Estimator
		
		int nSamples = 6; // N´mero de muestras del estimador
		estimator    = new Estimator(nSamples,slqgController.getTs());
		
		/**************************************************************************************************************/
		
		pCBolus  = 0.0;
		rCFBolus = 0;
		tEndAgg  = 0;
		
	}
	
	/**************************************************************************************************************/
	
	public double run(boolean mealFlag, int mealClass, double cgm){
		
		return gControllerState.run(mealFlag, mealClass, cgm);
		
	}
	
	/**************************************************************************************************************/
	
	public SLQGController buildSlqgController(Matrix Ak, Matrix Bk, Matrix CkCons, Matrix CkAgg){
		
		/**************************************************************************************************************/
		
		// Cada estado (conservador y agresivo) tiene asociado una csigma que es el elemento que se conmuta al cambiar
		// de modo
		
		Conservative conservative = Conservative.getInstance();
		conservative.setcCon(CkCons);
		
		Aggressive aggressive = Aggressive.getInstance();
		aggressive.setcAgg(CkAgg);
		
		double[][] aux = {{0.0}};
		Matrix Dk      = new Matrix(aux);
		Filter lqg     = new Filter(Ak,Bk,CkCons,Dk);
		
		/**************************************************************************************************************/
		
		// Se definen las posibles transiciones
		// Notar que la condici´n {false, false} no se considera ya que no implicar´a una conmutaci´n
		
		boolean[] condBoolean1 = {true,false};
		Condition condition1   = new Condition(condBoolean1);
		boolean[] condBoolean2 = {true,true};
		Condition condition2   = new Condition(condBoolean2);
		boolean[] condBoolean3 = {false,true};
		Condition condition3   = new Condition(condBoolean3);

		SLQGTransition transition1 = new SLQGTransition(conservative,condition1,aggressive);
		SLQGTransition transition2 = new SLQGTransition(conservative,condition2,aggressive);
		SLQGTransition transition3 = new SLQGTransition(aggressive,condition3,conservative);

		ArrayList<SLQGTransition> transitions = new ArrayList<SLQGTransition>();
		transitions.add(transition1);
		transitions.add(transition2);
		transitions.add(transition3);
		
		/**************************************************************************************************************/
		
		// Tiempo de muestreo del controlador [min]
		
		int controllerTs = 5;
		
		/**************************************************************************************************************/
		
		// Construcci´n del SLQG
		
		SLQGController slqgController = new SLQGController(aggressive, conservative, lqg, transitions, controllerTs); 
		
		return slqgController;
				
	}

	/**************************************************************************************************************/
	
	public double preMealCB(boolean mealFlag){
		
		return gControllerState.preMealCB(mealFlag);
		
	}
	
	/**************************************************************************************************************/
	
	public double standardCB(boolean hypoFlag){
		
		return gControllerState.standardCB(hypoFlag);
		
	}
	
	/**************************************************************************************************************/
	
	public void increaseItNumber(){
		
		++this.itNumber;
		
	}

	/**************************************************************************************************************/
	
	// Getters y setters
	
	public double getSetpoint() {
		
		return setpoint;
		
	}
	
	public void setSetpoint(double setpoint) {
		
		this.setpoint = setpoint;
		
	}
	
	public Pump getPump() {
		
		return pump;
		
	}
	
	public void setPump(Pump pump) {
		
		this.pump = pump;
		
	}
	
	public Patient getPatient() {
		
		return patient;
		
	}
	
	public void setPatient(Patient patient) {
		
		this.patient = patient;
		
	}
	
	public NSF getNsf() {
		
		return nsf;
		
	}
	
	public void setNsf(NSF nsf) {
		
		this.nsf = nsf;
		
	}
	
	public SLQGController getSlqgController() {
		
		return slqgController;
		
	}
	
	public void setSlqgController(SLQGController slqgController) {
		
		this.slqgController = slqgController;
		
	}
	
	public Safe getSafe() {
		
		return safe;
		
	}
	
	public void setSafe(Safe safe) {
		
		this.safe = safe;
		
	}
	
	public GControllerState getOpen() {
		
		return open;
		
	}
	
	public GControllerState getClosed() {
		
		return closed;
		
	}
	
	public GControllerState getgControllerState() {
		
		return gControllerState;
		
	}
	
	public void setgControllerState(GControllerState gControllerState) {
		
		this.gControllerState = gControllerState;
		
	}
	
	public int getItNumber() {
		
		return itNumber;
		
	}

	public int getOlTime() {
		
		return olTime;
		
	}

	public void setOlTime(int olTime) {
		
		this.olTime = olTime;
		
	}

	public Estimator getEstimator() {
		
		return estimator;
		
	}

	public void setEstimator(Estimator estimator) {
		
		this.estimator = estimator;
		
	}
	
	public double getpCBolus() {
		
		return pCBolus;
		
	}

	public void setpCBolus(double pCBolus) {
		
		this.pCBolus = pCBolus;
		
	}

	public int getrCFBolus() {
		
		return rCFBolus;
		
	}

	public void setrCFBolus(int rCFBolus) {
		
		this.rCFBolus = rCFBolus;
		
	}

	public int gettEndAgg() {
		
		return tEndAgg;
		
	}

	public void settEndAgg(int tEndAgg) {
		
		this.tEndAgg = tEndAgg;
		
	}

}