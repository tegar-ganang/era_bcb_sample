package net.googlecode.sokolovskiy;

import java.math.BigInteger;
import java.util.Random;

/**
 * Class, that realize task2 requirements.
 * Depends on the command line argument it :
 * 1) Counts the max prime number that less than N, if argument is "prime"
 * 2) Counts the N-th member of Fibonacci sequence, if argument is "fibonacci"
 * 3) Create an array of length N and fill them with numbers from 1 to 10,
 * then sort the array with Bubble sort method, if argument is "sort"
 *
 * @author Sokolovskiy Mike
 * @version 1.0
 */
public class Task2 {

    /**
     * Constant, which is used by functions of class.
     */
    public static final int N = 100;

    /**
     * Checks whether a number is prime.
     *
     * @return boolean
     */
    private boolean isPrime() {
        if (N <= 1) return false;
        for (int j = 2; j * j <= N; j++) if (N % j == 0) return false;
        return true;
    }

    /**
     * Counts the max prime number that less than <code>N</code>.
     *
     * @return the prime number
     */
    public int prime() {
        for (int i = N; i > 0; i--) {
            if (isPrime()) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Counts the N-th member of Fibonacci sequence.
     *
     * @return the N-th member of Fibonacci sequence
     */
    public BigInteger fibonacci() {
        BigInteger result = BigInteger.ZERO;
        if (N == 1) return BigInteger.ONE;
        BigInteger prprev = BigInteger.ZERO;
        BigInteger prev = BigInteger.ONE;
        for (int i = 2; i <= N; i++) {
            result = prprev.add(prev);
            prprev = prev;
            prev = result;
        }
        return result;
    }

    /**
     * Sorts the array with Bubble sort method.
     *
     * @param mas array
     */
    private void bubbleSort(int[] mas) {
        boolean t = true;
        while (t) {
            t = false;
            for (int i = 0; i < mas.length - 1; i++) {
                if (mas[i] > mas[i + 1]) {
                    int temp = mas[i];
                    mas[i] = mas[i + 1];
                    mas[i + 1] = temp;
                    t = true;
                }
            }
        }
    }

    /**
     * Create an array of length <code>N</code> and fill them with numbers from 1 to 10,
     * then sort the array with Bubble sort method.
     *
     * @return sorted array
     */
    public int[] sort() {
        int[] mas = new int[N];
        Random rand = new Random();
        for (int i = 0; i < N; i++) {
            mas[i] = rand.nextInt(10) + 1;
        }
        bubbleSort(mas);
        return mas;
    }

    /**
     * The main function.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Task2 task = new Task2();
        if (args.length == 0) return;
        if (args[0].equals("prime")) System.out.println(task.prime()); else if (args[0].equals("fibonacci")) System.out.println(task.fibonacci()); else if (args[0].equals("sort")) {
            int[] mas = task.sort();
            for (int i = 0; i < mas.length; i++) {
                System.out.print(mas[i] + " ");
            }
        }
    }
}
