package info.nightscout.androidaps.plugins.ARG;

public class TestPumpBuilder {

	public static void main(String[] args) {

		PumpBuilder defaultPump = new RochePumpBuilder();
		
		PumpEngineer pumpEngineer = new PumpEngineer(defaultPump);
		
		pumpEngineer.makePump();
		
		Pump dPump = pumpEngineer.getPump();
		
		System.out.println("Pump Built");
		
		System.out.println("Pump Min: " + dPump.getPumpMinBasal());
		
		System.out.println("Pump Max: " + dPump.getPumpMaxBasal());
		
		System.out.println("Pump Inc: " + dPump.getPumpIncBasal());
		
		System.out.println(dPump.quantizeBolus(0.12));
		
	}

}
