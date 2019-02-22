package info.nightscout.androidaps.plugins.ARG;
import java.util.Comparator;

public class PairComparator implements Comparator<Pair> {
	
	public int compare(Pair a, Pair b) {
		if (a.time() < b.time()) {
			return -1;
		}
		else if (a.time() > b.time()) {
			return 1;
		}
		else if (a.endTime() < b.endTime()){
			return -1;
		}
		else if (a.endTime() > b.endTime()){
			return 1;
		}
		else {
			return 0;
		}
	}


}
