package edu.java.homework.hw03.ipj06;

import java.util.Scanner;

/**
 * Realization of several operations with matrix
 * 
 * @author Tsvetan Vasilev
 * 
 */
public class Exer07 {

    public static void main(String[] args) {
        double[][] c = { { 1, 2, 3, 4, 5 }, { 6, 7, 8, 9, 10 }, { 11, 12, 13, 14, 15 }, { 16, 17, 18, 19, 20 }, { 21, 22, 23, 24, 25 } };
        Scanner input = new Scanner(System.in);
        System.out.println("Please, enter the dimention of the matrix: ");
        int length = input.nextInt();
        double[][] matrix = new double[length][length];
        inputMatrix(matrix);
        double[] series = new double[length * length];
        inputSeries(matrix, series);
        printSeries(series);
        sortSeries(series);
        printSeries(series);
        double[][] revisedMatrix = new double[length][length];
        inputMatrixFromSeries(revisedMatrix, series);
        printMatrix(revisedMatrix);
        input.close();
    }

    public static void inputMatrixFromSeries(double[][] matrix, double[] series) {
        if (matrix == null) {
            throw new IllegalArgumentException("Incorrect input, the matrix is null-pointed");
        }
        if (series == null) {
            throw new IllegalArgumentException("The series is null-pointed");
        }
        int length = matrix.length;
        if (length < 2) {
            throw new IllegalArgumentException("The dimention of the matrix is incorrect. It's smaller than 2");
        }
        int counter = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix.length; j++) {
                matrix[i][j] = series[counter];
                counter++;
            }
        }
    }

    /**
     * Sort in ascending order series of real numbers
     * 
     * @param series
     *            - array from real numbers
     * @throws IllegalArgumentException
     *             - the array is null-pointed
     */
    public static void sortSeries(double[] series) {
        if (series == null) {
            throw new IllegalArgumentException("Incorrect series. It's null-pointed");
        }
        int k = 0;
        int right = series.length - 1;
        while (right > 0) {
            k = 0;
            for (int i = 0; i <= right - 1; i++) {
                if (series[i] > series[i + 1]) {
                    k = i;
                    double tmp = series[i];
                    series[i] = series[i + 1];
                    series[i + 1] = tmp;
                }
            }
            right = k;
        }
    }

    /**
     * Input the matrix's elements in series in order of the matrix's columns
     * 
     * @param matrix
     *            - matrix of real numbers
     * @param series
     *            - the series where the elements of the matrix are inputed
     * @throws IllegalArgumentException
     *             - the matrix or series are null pointed or the matrix has
     *             incorrect dimention
     */
    public static void inputSeries(double[][] matrix, double[] series) {
        if (matrix == null) {
            throw new IllegalArgumentException("Incorrect input, the matrix is null-pointed");
        }
        if (series == null) {
            throw new IllegalArgumentException("The series is null-pointed");
        }
        int length = matrix.length;
        if (length < 2) {
            throw new IllegalArgumentException("The dimention of the matrix is incorrect. It's smaller than 2");
        }
        int counter = 0;
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                series[counter] = matrix[j][i];
                counter++;
            }
        }
    }

    public static void printSeries(double[] series) {
        if (series == null) {
            throw new IllegalArgumentException("Incorrect input, the series is null-pointed");
        }
        System.out.println("\nSeries:");
        for (int i = 0; i < series.length; i++) {
            System.out.print(series[i] + "  ");
        }
    }

    /**
     * Input the elements of matrix in order of rows
     * 
     * @param matrix
     *            - matrix of real numbers
     * @throws IllegalArgumentException
     *             - the matrix is null pointed or has incorrect dimention
     */
    public static void inputMatrix(double[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("Incorrect input, the matrix is null-pointed");
        }
        int length = matrix.length;
        if (length < 2) {
            throw new IllegalArgumentException("The dimention of the matrix is incorrect. It's smaller than 2");
        }
        Scanner input = new Scanner(System.in);
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                System.out.print("a[" + i + "][" + j + "]= ");
                matrix[i][j] = input.nextDouble();
            }
        }
        input.close();
    }

    /**
     * Print the elements of matrix in order of rows
     * 
     * @param matrix
     *            - matrix of real numbers
     * @throws IllegalArgumentException
     *             - the matrix is null pointed or has incorrect dimention
     */
    public static void printMatrix(double[][] matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("Incorrect input, the matrix is null-pointed");
        }
        int length = matrix.length;
        if (length < 2) {
            throw new IllegalArgumentException("The dimention of the matrix is incorrect. It's smaller than 2");
        }
        System.out.println("\nMatrix:");
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < length; j++) {
                System.out.print(matrix[i][j] + "\t");
            }
            System.out.println();
        }
    }
}
