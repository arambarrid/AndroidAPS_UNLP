package info.nightscout.androidaps.plugins.ARG;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class TestARG {

    public static void main(String[] args) throws IOException, InterruptedException {

        double setPoint = 120.0;
        double TDI      = 25.0;
        double CR       = 15.0;
        double CF       = 20.0;
        double weight   = 80.0;
        double uBasal   = 1.0;
        double resultado;
        double[][] inputD = {{1},{1}};
        Matrix input = new Matrix(inputD);
        double[][] uD = {{0}};
        Matrix u = new Matrix(uD);
        GController gController = new GController(setPoint, TDI, CR, CF, weight, uBasal, null);

        System.out.println("setPoint: " + gController.getSetpoint());
        while(true) {
            resultado = gController.run(false, 1, 200);
            System.out.println("Resultado: " + resultado);
            double[][] xstates = gController.getSlqgController().getLqg().getX().getData();
            System.out.println("Matriz de estados: ");
            for (int x=0; x < xstates.length; x++) {
                System.out.print("|");
                for (int y=0; y < xstates[x].length; y++) {
                    System.out.print (xstates[x][y]);
                    if (y!=xstates[x].length-1) System.out.print("\t");
                }
                System.out.println("|");
            }
            double[][] iobStates = gController.getSafe().getIob().getX().getData();
            System.out.println("Matriz IOB: ");
            for (int x=0; x < iobStates.length; x++) {
                System.out.print("|");
                for (int y=0; y < iobStates[x].length; y++) {
                    System.out.print (iobStates[x][y]);
                    if (y!=iobStates[x].length-1) System.out.print("\t");
                }
                System.out.println("|");
            }
            TimeUnit.SECONDS.sleep(5);


        }
    }
}

