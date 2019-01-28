package info.nightscout.androidaps.plugins.ARG;

/**************************************************************************************************************/

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import info.nightscout.androidaps.R;

/**************************************************************************************************************/

public class AndroidFileRead {
	
	private int nSsOrder;
	double[][] 	ssArray;

	/**************************************************************************************************************/
	
	// Constructor
	
	public AndroidFileRead(int n){
		this.nSsOrder = n+2; // B-Matrix has 2 columns
		this.ssArray  = new double[n+2][n+2];
	}
	
	/**************************************************************************************************************/
	
	public double[][] getData(Context contexto) {
		
		int Rowc = 0;
		
		BufferedReader reader = null;
		InputStream inputStream = contexto.getResources().openRawResource(R.raw.cmatrices);
		if (inputStream != null) {
			reader = new BufferedReader(new InputStreamReader(inputStream));


			try {
				String line;
				String[] values;

				while ((line = reader.readLine()) != null){
					values = line.split(",");
					for(int ii = 0; ii < values.length ; ii++){
						ssArray[Rowc][ii] = Double.parseDouble(values[ii]);
					}					
					Rowc++;
				}	
		

				
			} catch (IOException ie) {
			}
			// 	Debug.i("fetchDataFile", FUNC_TAG, "Load complete...");
			} else {
				//	You would need to indicate to controller that it cannot run
				}
			return ssArray;
			}
	
	/**************************************************************************************************************/
	
	// getA_matrix Method
	
	public double[][] getAMatrix(double[][] abcd){
		int 	   n = this.nSsOrder;
		double[][] Am = new double[n-2][n-2];

		for (int ii = 0;ii < Am.length; ii++){
			for (int jj = 0 ; jj < Am.length ; jj++){
				Am[ii][jj]  = abcd[ii][jj];
			}
		}
		return Am;
	} 		
	
	/**************************************************************************************************************/
	
	// getB_matrix Method	
	
	public double[][] getBMatrix(double[][] abcd){
		int 	   n  = this.nSsOrder;
		double[][] Bm = new double[n-2][2];
		
		for (int ii = 0;ii < n-2; ii++){
			for (int jj = 0;jj < 2; jj++){
				Bm[ii][jj]  = abcd[ii][n-2+jj];
			}
		}

		return Bm;
	} 
	
	/**************************************************************************************************************/
	
	// getCCons_matrix Method	
	
	public double[][] getCConsMatrix(double[][] abcd){
		int 	   n  = this.nSsOrder;
		double[][] Cm = new double[1][n-2];
		
		for (int ii = 0;ii < n-2; ii++){
			Cm[0][ii]  = abcd[n-2][ii];
		}

		return Cm;
	} 
	
	/**************************************************************************************************************/
	
	// getCAgg_matrix Method	
	
	public double[][] getCAggMatrix(double[][] abcd){
		int 	   n  = this.nSsOrder;
		double[][] Cm = new double[1][n-2];
			
		for (int ii = 0;ii < n-2; ii++){
			Cm[0][ii]  = abcd[n-1][ii];
		}

		return Cm;
	} 

}