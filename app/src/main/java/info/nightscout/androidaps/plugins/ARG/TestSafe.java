package info.nightscout.androidaps.plugins.ARG;

import java.util.ArrayList;

public class TestSafe {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		double kDia = 16.3e-3;
		double ts = .1;
		double weight = 80.0;
		double uBasal = 1.0;
		double CR = 15.0;
		double tau = 0.0;
		double wFilterEdgeFreq = 0.5;
		
		Matrix Ak     = new Matrix(13,13);
		Matrix Bk     = new Matrix(13,2);
		Matrix CkCons = new Matrix(1,13);
		Matrix CkAgg  = new Matrix(1,13);
    			
		SLQGController slqgController   = buildSlqgController(Ak, Bk, CkCons, CkAgg);
		
		Safe safe = new Safe(kDia, ts, weight, uBasal, CR, tau, wFilterEdgeFreq,slqgController);
		
		//safe.getIob().getX().show();
		
		double u = 500;
		int controllerTs = 5;
		int mealClass = 1;
		int mCount = 0;
		double trend = 0;
		double gEst = 130;
		double setPoint = 120;
		String slqgState = "Conservative";
		int extAgg = 0;
		double cgm = 120.0;
		
		for(int ii = 0; ii<20; ++ii){
			//safe.run(u, controllerTs, weight, mealClass, mCount, trend, gEst, setPoint, slqgState, extAgg, CR, uBasal,cgm);
			/*safe.getIob().getX().show();
			System.out.println(safe.getW());
			safe.getwFilter().getX().show();*/
			System.out.println(safe.getGamma());
			System.out.println(safe.getIobEst(weight));
			System.out.println(safe.getIobMaxBasal());
			System.out.println(safe.getIobMaxSmall());
			System.out.println(safe.getIobMaxMedium());
			System.out.println(safe.getIobMaxBig());
			System.out.println(safe.getIobMax());
			System.out.println("");
			/*System.out.println(safe.getSafeEndFlag());
			System.out.println(safe.getGamma());*/
		}
		
		/*
		u = 0.0;
		
		for(int ii = 0; ii<100; ++ii){
			safe.run(u, controllerTs, weight);
		
			System.out.println(safe.getSafeEndFlag());
			System.out.println(safe.getGamma());
		}
		*/
	}
	
	public static SLQGController buildSlqgController(Matrix Ak, Matrix Bk, Matrix CkCons, Matrix CkAgg){
		
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

}
