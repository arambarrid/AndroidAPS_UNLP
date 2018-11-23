package info.nightscout.androidaps.plugins.ARG;

public class Filter {
	
	private Matrix aMatrix;
	private Matrix bMatrix;
	private Matrix cMatrix;
	private Matrix dMatrix;
	private Matrix x;
	
	/**************************************************************************************************************/
	
	// Constructores
	
	public Filter(Matrix a, Matrix b, Matrix c, Matrix d){
		
		aMatrix = a;
		bMatrix = b;
		cMatrix = c;
		dMatrix = d;
		
		// Los estados iniciales se setean en 0
		
		setX(new Matrix(aMatrix.getM(),1));
		
	}
		
	public Filter(Matrix a, Matrix b, Matrix c, Matrix d, Matrix x){
		
		aMatrix = a;
		bMatrix = b;
		cMatrix = c;
		dMatrix = d;
		
		// Los estados iniciales se setean de acuerdo al argumento x pasado
		
		setX(x);
		
	}
	
	/**************************************************************************************************************/
	
	public void stateUpdate(Matrix input){
		
		Matrix xK = x;
		x = this.aMatrix.times(xK).plus(this.bMatrix.times(input));
		
	}

	/**************************************************************************************************************/
	
	public Matrix outputUpdate(Matrix input){
		
		Matrix y = this.cMatrix.times(this.x).plus(this.dMatrix.times(input));
		return y;
		
	}
	
	/**************************************************************************************************************/
	
	// GETTERS Y SETTERS
	
	public Matrix getAMatrix() {
		
		return aMatrix;
		
	}
	
	public void setAMatrix(Matrix aMatrix) {
		
		this.aMatrix = aMatrix;
		
	}
	
	public Matrix getBMatrix() {
		
		return bMatrix;
		
	}
	
	public void setBMatrix(Matrix bMatrix) {
		
		this.bMatrix = bMatrix;
		
	}
	
	public Matrix getCMatrix() {
		
		return cMatrix;
		
	}
	
	public void setCMatrix(Matrix cMatrix) {
		
		this.cMatrix = cMatrix;
		
	}
	
	public Matrix getDMatrix() {
		
		return dMatrix;
		
	}
	
	public void setDMatrix(Matrix dMatrix) {
		
		this.dMatrix = dMatrix;
		
	}
	
	public Matrix getX() {
		
		return x;
		
	}
	
	public void setX(Matrix x) {
		
		this.x = x;
		
	}	
	
}
