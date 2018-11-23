package info.nightscout.androidaps.plugins.ARG;

public class TestMatrix {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		double kDia = 16.3e-3;
		
		/*
		double[][] a = {{-kDia, 0.0},{kDia, -kDia}};
		Matrix A = new Matrix(a);
		
		double[][] b = {{1.0},{0.0}};
		Matrix B = new Matrix(b);
		
		double[][] c = {{1,1}};
		Matrix C = new Matrix(c);
		*/
		
		double ts = 0.1;
		
		double[][] a = {{Math.exp(-ts*kDia), 0.0},{ts*kDia*Math.exp(-ts*kDia), Math.exp(-ts*kDia)}};
		double[][] b = {{-(Math.exp(-ts*kDia)-1)/kDia},{-(Math.exp(-ts*kDia)+ts*kDia*Math.exp(-ts*kDia)-1)/kDia}};
		double[][] c = {{1.0, 1.0}};
		double[][] d = {{0}};
		
		Matrix A = new Matrix(a);
		Matrix B = new Matrix(b);
		Matrix C = new Matrix(c);
		Matrix D = new Matrix(d);
		
		double[][] u = {{100.0/70.0}};
		
		Matrix uEq = new Matrix(u);
				
		Matrix xEq = Matrix.identity(A.getM()).minus(A).solve(B);
		
		xEq.show();
		
		xEq.times(uEq).show();
		
		uEq.show();		
		
		double[][] temp = xEq.getData();
		
		System.out.print(temp[0][0]);
		
	}

}
