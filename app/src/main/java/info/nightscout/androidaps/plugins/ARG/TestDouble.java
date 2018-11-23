package info.nightscout.androidaps.plugins.ARG;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TestDouble {

	public static void main(String[] args) {

		int controllerTs = 5;
		double ts = 0.1;
		
		double gamma = controllerTs/ts;
		
		System.out.println(controllerTs/ts);
		
		System.out.println(-4.4409e-15>=0);
		
		double nPcb = new BigDecimal(Double.toString(-4.4409e-15)).setScale(10, RoundingMode.HALF_DOWN).doubleValue();
		
		System.out.println(nPcb);
		
		double basal = 1.223793990830904;
		
		double basal2 = new BigDecimal(Double.toString(basal)).setScale(16, RoundingMode.HALF_DOWN).doubleValue();
		
		System.out.println(basal/12 + "basal2: "+basal2/12);
	}

}
