package info.nightscout.androidaps.plugins.ARG;

public class TestGController {

	public static void main(String[] args) {

		double setPoint = 120.0;
		double TDI      = 25.0;
		double CR       = 15.0;
		double CF       = 20.0;
		double weight   = 80.0;
		double uBasal   = 1.0;
		
		GController gController = new GController(setPoint, TDI, CR, CF, weight, uBasal);
		
		System.out.println("setPoint: " + gController.getSetpoint());
		
	}
}
