package info.nightscout.androidaps.plugins.ARG;

import java.util.List;

public class SLQGController {
	
	private SLQGState aggressive;             // Estado agresivo
	private SLQGState conservative;           // Estado conservador
	private SLQGState slqgState;              // Estado actual
	private List<SLQGTransition> transitions; // Lista de posibles transiciones
	private Filter lqg;                       // Filtro SLQG
	private int tMeal;                        // Contador asociado al tiempo en que el modo agresivo est´ activo
	private int ts;                           // Tiempo de muestreo
	private int extAgg; 					  // Contador extensi´n l´mite de IOB post conmutaci´n al conservador

	/**************************************************************************************************************/
	
	// Constructor
	
	public SLQGController(SLQGState agg, SLQGState cons, Filter selectedLqg, List<SLQGTransition> selectedTrans, int selectedTs){
		
		aggressive   = agg;
		conservative = cons;
		slqgState    = conservative; // El estado inicial se define como conservador
		transitions  = selectedTrans;
		lqg          = selectedLqg;
		tMeal        = 0;
		ts           = selectedTs;
		extAgg       = 0;
		
	}
	
	/**************************************************************************************************************/
	
	public void updateLQG(){
		
		slqgState.updateLQG(this);
		
	}
	
	/**************************************************************************************************************/
	
	public void apply(Condition conditions){
		
		slqgState = getNextState(conditions);
		
	}
	
	/**************************************************************************************************************/
	
	public SLQGState getNextState(Condition newCondition){
		
		SLQGState currentState = slqgState;
		
		for(SLQGTransition transition : transitions){
			
			boolean currentStateMatches = transition.getFrom().equals(slqgState);
			boolean conditionsMatch     = transition.getCondition().equals(newCondition);
			
			if(currentStateMatches && conditionsMatch){
				return transition.getTo();
			}
		}
		
		return currentState;
		
	}
	
	/**************************************************************************************************************/
	
	public boolean aggEndCheck(){
		
		boolean flag = false;
		
		if(tMeal<60){
			flag = false;
		}else{
			flag = true;
		}
		
		return flag;
		
	}
	
	/**************************************************************************************************************/
	
	// A diferencia del preMealCB que se define en el estado del GController, que por default es closed-loop, aqu´ el
	// standardCB se define en SLQGController, dado que depende del estado del SLQG. Esto es b´sicamente, porque el
	// BAC est´ndar no puede generarse si el controlador est´ en agresivo
	
	public double standardCB(boolean hypoFlag, GController gController){
		
		return slqgState.standardCB(hypoFlag, gController);
		
	}
	
	/**************************************************************************************************************/
	
	public boolean hypoProtect(int mCount, double iobEst, double trend, double gEst, double setPoint, SLQGController slqg, Safe safe){
		
		return slqgState.hypoProtect(mCount, iobEst, trend, gEst, setPoint, slqg, safe);
		
	}
	
	/**************************************************************************************************************/
	
	public void setIobLimit(int mClass, Safe safe, double iobFactor){
		
		slqgState.setIobLimit(mClass, safe, iobFactor);
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS Y SETTERS
	
	public void setSLQGState(SLQGState newSLQGState){
		
		slqgState = newSLQGState;
		
	}
	
	public SLQGState getSLQGState(){
		
		return this.slqgState;
		
	}
		
	public SLQGState getAggressiveState() { return aggressive;}
	
	public SLQGState getConservativeState() { return conservative;}

	public List<SLQGTransition> getTransitions() {
		
		return transitions;
		
	}

	public void setTransitions(List<SLQGTransition> transitions) {
		
		this.transitions = transitions;
		
	}

	public Filter getLqg() {
		
		return lqg;
		
	}

	public void setLqg(Filter lqg) {
		
		this.lqg = lqg;
		
	}

	public int gettMeal() {
		
		return tMeal;
		
	}

	public void settMeal(int tMeal) {
		
		this.tMeal = tMeal;
		
	}

	public int getTs() {
		
		return ts;
		
	}

	public int getExtAgg() {
		
		return extAgg;
		
	}

	public void setExtAgg(int extAgg) {
		
		this.extAgg = extAgg;
		
	}

}
