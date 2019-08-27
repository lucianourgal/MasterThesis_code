/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package auxs;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

/**
 *
 * @author lucia
 */
public class matrixUtilities {

    public static double MACHEPS = 2E-16;

    /**
     * Updates MACHEPS for the executing machine.
     */
    public static void updateMacheps() {
        MACHEPS = 1;
        do {
            MACHEPS /= 2;
        } while (1 + MACHEPS / 2 != 1);
    }

    public static double[][] transposeMatrixD(double[][] m) {
        double[][] temp = new double[m[0].length][m.length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                temp[j][i] = m[i][j];
            }
        }
        return temp;
    }

    public static int[][] transposeMatrixI(int[][] m) {
        int[][] temp = new int[m[0].length][m.length];
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[0].length; j++) {
                temp[j][i] = m[i][j];
            }
        }
        return temp;
    }

    
    public double [][] inverteMatriz(double [][] mat){
    
    Matrix m = new Matrix(mat);
    return pinv(m).getArrayCopy();
        
    }   
    
    public double[][] mult(double[][] m1, double[][] m2){
    return multiplyByMatrix(m1,m2);
    }
    
    private double[][] multiplyByMatrix(double[][] m1, double[][] m2) {
        int m1ColLength = m1[0].length; // m1 columns length
        int m2RowLength = m2.length;    // m2 rows length
        if(m1ColLength != m2RowLength) return null; // matrix multiplication is not possible
        int mRRowLength = m1.length;    // m result rows length
        int mRColLength = m2[0].length; // m result columns length
        double[][] mResult = new double[mRRowLength][mRColLength];
        for(int i = 0; i < mRRowLength; i++) {         // rows from m1
            for(int j = 0; j < mRColLength; j++) {     // columns from m2
                for(int k = 0; k < m1ColLength; k++) { // columns from m1
                    mResult[i][j] += m1[i][k] * m2[k][j];
                }
            }
        }
        return mResult;
    }
    
    
    
    private Matrix pinv(Matrix x) {
        int rows = x.getRowDimension();
        int cols = x.getColumnDimension();
        if (rows < cols) {
            Matrix result = pinv(x.transpose());
            if (result != null) {
                result = result.transpose();
            }
            return result;
        }
        SingularValueDecomposition svdX = new SingularValueDecomposition(x);
        if (svdX.rank() < 1) {
            return null;
        }
        double[] singularValues = svdX.getSingularValues();
        double tol = Math.max(rows, cols) * singularValues[0] * MACHEPS;
        double[] singularValueReciprocals = new double[singularValues.length];
        for (int i = 0; i < singularValues.length; i++) {
            if (Math.abs(singularValues[i]) >= tol) {
                singularValueReciprocals[i] = 1.0 / singularValues[i];
            }
        }
        double[][] u = svdX.getU().getArray();
        double[][] v = svdX.getV().getArray();
        int min = Math.min(cols, u[0].length);
        double[][] inverse = new double[cols][rows];
        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < u.length; j++) {
                for (int k = 0; k < min; k++) {
                    inverse[i][j] += v[i][k] * singularValueReciprocals[k] * u[j][k];
                }
            }
        }
        return new Matrix(inverse);
    }

}
