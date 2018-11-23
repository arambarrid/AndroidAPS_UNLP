package info.nightscout.androidaps.plugins.ARG;

public class TestEstimator {

	public static void main(String[] args) {
		
		Estimator estimator = new Estimator(6,5);
		//estimator.getCgmVector().show();
		
		/*estimator.insert(124.3);
		estimator.insert(120);
		estimator.insert(115);
		estimator.insert(110);
		estimator.insert(100);
		estimator.insert(85);
		estimator.getCgmVector().show();
		
		estimator.updatePred();
		
		double pred = estimator.getPred();
		double trend = estimator.getTrend();
		
		System.out.println("Prediction: " + pred);
		System.out.println("Trend" + trend);*/
		/*System.out.println(estimator.getMCount());
		boolean mealFlag = true;
		estimator.activateMCount(mealFlag);
		System.out.println(estimator.getMCount());
		estimator.updateMCount();
		System.out.println(estimator.getMCount());*/
		
		int pepe = 1;
		System.out.println(pepe!=0);
		if (pepe!=0){
			pepe = ++pepe;
		}
		System.out.println(pepe);
	}

}
