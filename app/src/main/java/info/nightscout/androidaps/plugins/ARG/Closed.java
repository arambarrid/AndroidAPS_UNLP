package info.nightscout.androidaps.plugins.ARG;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import info.nightscout.androidaps.logging.L;

public class Closed implements GControllerState {

    private static Logger log = LoggerFactory.getLogger(L.DATABASE);

	private GController gController; // Switched LQG + SAFE

	private double sumaDeBACs = 0.0;
	
	/**************************************************************************************************************/
	
	// Constructor
	
	public Closed(GController newGController){
		
		gController = newGController;
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public double run(boolean mealFlag, int mealClass, double cgm, double iobFactor) {
		
		// mealFlag es el flag de anuncio de comida
		// mealClass es la categor´a de la comida (small, medium, large)
		// cgm es la muestra actual de CGM
		
		/**************************************************************************************************************/
		
		double cgmF = cgm; // Capturo la nueva muestra de CGM
		 
		/**************************************************************************************************************/
		
		// Actualizaci´n predicci´n

		gController.getEstimator().updatePred(15); // Predigo la concentración de glucosa a 15 min 
		
		/**************************************************************************************************************/
		
		// Si llega el flag de anuncio de comida se activa el mCount
		
		gController.getEstimator().activateMCount(mealFlag); 
		
		/**************************************************************************************************************/
		
		// C´lculo del BAC pre-comida
		
		double cfMBolus = gController.preMealCB(mealFlag);
		
		if (mealFlag)
		{
			gController.getSafe().setIOBMaxCF(0.8*cfMBolus);
		}
		
		/**************************************************************************************************************/
		
		// Verifico si la se´al CGM est´ apta (trend necesario) para conmutar al agresivo
		
		boolean mealFFlag = gController.getEstimator().aggressiveSwitch(gController.getSlqgController());
		
		/**************************************************************************************************************/
		
		gController.getEstimator().updateMCount();
		
		// Tiene sentido cuando se define un determinado tiempo hasta pasar al modo listening. Aqu´ no tiene efecto
		// pero se mantiene para ofrecer m´s grados de libertad
		
		/**************************************************************************************************************/
		
		// Se verifica si el controlador ya cumpli´ los 60 min en modo agresivo
		
		boolean aggFlag = gController.getSlqgController().aggEndCheck();
		
		/**************************************************************************************************************/
		
		// Conmutaci´n del agresivo al conservador
		
		if(aggFlag){
			
			gController.getSlqgController().setExtAgg(30); // El límite de IOB para la comida se extiende por 30 min
			gController.settEndAgg(36); // No se permiten BACs durante el postprandial
			
		}
		
		/**************************************************************************************************************/
		
		// Manejo de la transici´n
		
		boolean[] conditionM = {mealFFlag, aggFlag};
		
		Condition condition = new Condition(conditionM);
		
		gController.getSlqgController().apply(condition); // Aplico la condición
		gController.getSlqgController().updateLQG(); // Actualizo la matriz C del SLQG
		
		/**************************************************************************************************************/
		
		// Seteo l´mites IOB
		
		gController.getSafe().setIobLimit(mealClass, gController.getPatient().getCr(), gController.getPatient().getWeight(), gController.getPatient().getBasalU(), gController.getSlqgController(), iobFactor);
		
		/**************************************************************************************************************/
		
		// Capa de hipoglucemia
		
		double iobEst    = gController.getSafe().getIobEst(gController.getPatient().getWeight());
		int mCount       = gController.getEstimator().getMCount();
		double gEst      = gController.getEstimator().getPred(); // Glucosa futura estimada
		double trend     = gController.getEstimator().getTrend(); // Trend estimado
		
		boolean hypoFlag = gController.getSafe().hypoProtect(mCount, iobEst, trend, gEst, gController.getSetpoint(), gController.getSlqgController(), cgm);
		
		/**************************************************************************************************************/
		
		// C´lculo del BAC est´ndar
		
		double cfSBolus = gController.standardCB(hypoFlag);

		/**************************************************************************************************************/
		
		// Unifico los 2 tipos de BACs
		
		double cfBolusAux = cfMBolus+cfSBolus;
		
		double cfBolus = new BigDecimal(Double.toString(cfBolusAux)).setScale(3, RoundingMode.HALF_DOWN).doubleValue();
		
		cfBolus = Math.round(cfBolus*100.0)/100.0;

		sumaDeBACs = cfBolus;
		log.debug("[ARGPLUGIN:CONTROLADOR] run() cfBolus:" + cfBolus +
		 " cfBolusAux:" + cfBolusAux + " cfMBolus:" + cfMBolus + " cfSBolus:" + cfSBolus);
		
		/**************************************************************************************************************/
		
		// Se´al de error del lazo cerrado
		
		double errorSignal = gController.getSetpoint()-cgmF;
		
		/**************************************************************************************************************/
		
		// Actualizaci´n de la salida del SLQG
		
		double[][] aux      = {{errorSignal}};
		Matrix errorSignalM = new Matrix(aux);
		Matrix uCM          = gController.getSlqgController().getLqg().outputUpdate(errorSignalM);
		double uC           = uCM.getData()[0][0]; // en [pmol/min]
		
		/**************************************************************************************************************/
		
		// Convierto la velocidad de infusi´n de pmol/min a U/5 min
		// Se suma el bolo asociado a la insulina basal
		// Se suma el bolo de la iteraci´n anterior no inyectado por la cuantizaci´n de la bomba
		// Se suma el BAC (si no se gener´ es 0)
		
		double uSlqgBolusAux    = uC/100.0/12.0+gController.getPatient().getBasalU()/12.0+gController.getpCBolus()+cfBolus;
		
		double uSlqgBolus = new BigDecimal(Double.toString(uSlqgBolusAux)).setScale(16, RoundingMode.HALF_DOWN).doubleValue();
		
		/**************************************************************************************************************/
		
		double uSlqgSatBolus = gController.getPump().saturateBolus(uSlqgBolus); // Saturo el bolo propuesto
		
		/**************************************************************************************************************/
		
		// Aplicaci´n del SAFE
		
		double uSlqgSat = 12.0*100.0*gController.getPump().quantizeBolus(uSlqgSatBolus); // Convierto el bolo a basal para correr el SAFE
		
		gController.getSafe().run(uSlqgSat, gController.getSlqgController().getTs(), gController.getPatient().getWeight()); // Corro el SAFE
		
		double uF = gController.getPump().quantizeBolus(gController.getSafe().getGamma()*uSlqgSatBolus); // Bolo final propuesto por el SLQG+SAFE 
		
		/**************************************************************************************************************/
		
		// Carry-over scheme
		
		// Calculo el bolo que por cuantizaci´n no inyect´ y lo guardo para la pr´xima iteraci´n
		
		double nPcb = new BigDecimal(Double.toString(gController.getSafe().getGamma()*uSlqgSatBolus-uF)).setScale(16, RoundingMode.HALF_DOWN).doubleValue(); 
		
		gController.setpCBolus(nPcb);
		
		/**************************************************************************************************************/
		
		// Actualizo los estados del SLQG
		
		// Calculo la acci´n de control (basal rate) para actualizar el controlador, restando el bolo basal y el posible BAC
		
		double auxUp = new BigDecimal(Double.toString(uSlqgSatBolus-cfBolus-gController.getPatient().getBasalU()/12.0)).setScale(10, RoundingMode.HALF_DOWN).doubleValue();
		
		double uFC = 100.0*12.0*gController.getPump().quantizeBolus(gController.getSafe().getGamma()*(auxUp));
		
		double[][] inputLqgD = {{uFC},{errorSignal}};
		Matrix inputLqgM     = new Matrix(inputLqgD); 
		gController.getSlqgController().getLqg().stateUpdate(inputLqgM);
		
		/**************************************************************************************************************/
		
		// Actualizo el contador de extensi´n del l´mite de IOB
		
		gController.getSlqgController().getSLQGState().updateExtAgg(gController.getSlqgController());
		
		/**************************************************************************************************************/
		
		// Actualizo los timers asociados a los BACs
		
		gController.getgControllerState().updateBolusVar();
		
		/**************************************************************************************************************/
		
		return 100.0*12.0*uF;
		
	}
	
	/**************************************************************************************************************/
	
	public double preMealCB(boolean mealFlag){
		
		// mealFlag indica si se anunci´ o no una comida
		
		double cfBolus = 0.0;

		if(mealFlag){
			
			// Generaci´n de condiciones para aplicaci´n del BAC pre-comida
			
			/**************************************************************************************************************/
			
			// Datos del vector de 30 min
			
			double[][] cVector1 = gController.getEstimator().getCgmVector().getData();
			double trend1       = gController.getEstimator().getTrend();
			double pred1        = gController.getEstimator().getPred();
			
			/**************************************************************************************************************/
			
			// Armo un estimador con las ´ltimas 3 muestras
			
			Estimator estimator1 = new Estimator(3,gController.getSlqgController().getTs());
			
			for(int ii = 0; ii < 3; ++ii){
				estimator1.insert(cVector1[cVector1.length-1-2+ii][0]);
			}
						
			double trend2 = estimator1.getTrend();
			
			/**************************************************************************************************************/
			
			// Verifico si todas las muestras del vector de 30 min est´n por encima de 150 mg/dl
			
			int nSamples = 0;
			
			for(int ii = 0; ii < gController.getEstimator().getCgmVector().getM(); ++ii){
				if(cVector1[ii][0]>150.0){ 
					nSamples += 1;
				}
			}
			
			/**************************************************************************************************************/
			
			if(nSamples==gController.getEstimator().getCgmVector().getM() && trend1>=-0.1 && trend2>=-0.5 && gController.getrCFBolus()<=6){
				cfBolus = (Math.min(gController.getEstimator().getMean(), pred1)-gController.getSetpoint())/gController.getPatient().getCf();
				if(cfBolus<=0){
					cfBolus = 0.0;
				}
				else{
					int rCFBolus = gController.getrCFBolus();
					gController.setrCFBolus(rCFBolus+18); // Extiendo la prohibición de generación de otros BACs por 1 hora y media
				}
			}
		}
		
		return cfBolus;
	}
	
	/**************************************************************************************************************/
	
	public double standardCB(boolean hypoFlag){
		
		double cfBolus = gController.getSlqgController().standardCB(hypoFlag, gController);
		
		return cfBolus;
	}
	
	/**************************************************************************************************************/
	
	public void updateBolusVar(){
		
		if(gController.getrCFBolus()>0){
			gController.setrCFBolus(gController.getrCFBolus()-1);
		}
		
		if(gController.gettEndAgg()>0){
			gController.settEndAgg(gController.gettEndAgg()-1);
		}
		
	}
	
	/**************************************************************************************************************/
	
	@Override
	public void show() {

		System.out.println("Closed-Loop");
		
	}

	// Funciones extras para AndroidAPS

	public double getBACs(){
		return sumaDeBACs;
	}

}
