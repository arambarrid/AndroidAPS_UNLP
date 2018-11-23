package info.nightscout.androidaps.plugins.ARG;

import java.util.ArrayList;
import java.util.List;

public class TestSlqgController {

	public static void main(String[] args) {
				
		SLQGState conservative = Conservative.getInstance();
		SLQGState aggressive = Aggressive.getInstance();
		List<SLQGTransition> transitions = new ArrayList<SLQGTransition>();
		
		boolean[] condition1M = new boolean[] {true,false,false};
		boolean[] condition2M = new boolean[] {true,false,true};
		boolean[] condition3M = new boolean[] {true,true,false};
		boolean[] condition4M = new boolean[] {true,true,true};
		
		boolean[] condition5M = new boolean[] {false,false,true};
		boolean[] condition6M = new boolean[] {false,true,false};
		boolean[] condition7M = new boolean[] {false,true,true};
		
		Condition condition1 = new Condition(condition1M);
		Condition condition2 = new Condition(condition2M);
		Condition condition3 = new Condition(condition3M);
		Condition condition4 = new Condition(condition4M);
		Condition condition5 = new Condition(condition5M);
		Condition condition6 = new Condition(condition6M);
		Condition condition7 = new Condition(condition7M);
		
		SLQGTransition transition1 = new SLQGTransition(conservative,condition1,aggressive);
		SLQGTransition transition2 = new SLQGTransition(conservative,condition2,aggressive);
		SLQGTransition transition3 = new SLQGTransition(conservative,condition3,aggressive);
		SLQGTransition transition4 = new SLQGTransition(conservative,condition4,aggressive);

		SLQGTransition transition5 = new SLQGTransition(aggressive,condition5,conservative);
		SLQGTransition transition6 = new SLQGTransition(aggressive,condition6,conservative);
		SLQGTransition transition7 = new SLQGTransition(aggressive,condition7,conservative);

		transitions.add(transition1);
		transitions.add(transition2);
		transitions.add(transition3);
		transitions.add(transition4);
		transitions.add(transition5);
		transitions.add(transition6);
		transitions.add(transition7);
		
		/*
		SLQGController slqgController = new SLQGController(transitions,5);
		
		slqgController.getSLQGState().show();
		slqgController.apply(condition5);
		slqgController.getSLQGState().show();
		*/
		
	}

}
