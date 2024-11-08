package net.googlecode.harchenko;

import java.math.BigInteger;
import java.util.Random;
import static java.lang.Math.sqrt;

/**
 * Class, that realize task2 requirements.
 * Depends on the command line argument it :
 * 1) Counts the max prime number that less than N, if argument is "prime"
 * 2) Counts the N-th member of Fibonacci sequence, if argument is "fibonacci"
 * 3) Create an array of length N and fill them with numbers from 1 to 10,
 * then sort the array with Bubble-sort algorithm, if argument is "sort"
 *
 * @author Kharchenko Yaroslav
 */
public class Task2 {

    /**
     * Constant, which use functions of the class.
     */
    private static final int N = 100;

    /**
     * Counts the max prime number that less than <code>N</code>.
     *
     * @return the prime number
     */
    public int prime() {
        for (int i = N + (N % 2) - 1; i > 1; i -= 2) {
            for (int j = 3; j < sqrt(i); j += 2) {
                if (i % j == 0) {
                    break;
                }
                if (j >= sqrt(i) - 2) {
                    return i;
                }
            }
        }
        return N;
    }

    /**
     * Counts the N-th member of Fibonacci sequence.
     *
     * @return the N-th member of Fibonacci sequence
     */
    public BigInteger fibonacci() {
        BigInteger res = BigInteger.ZERO;
        if (N == 1) {
            return BigInteger.ONE;
        }
        BigInteger first = BigInteger.ZERO;
        BigInteger second = BigInteger.ONE;
        for (int i = 2; i <= N; i++) {
            res = first.add(second);
            first = second;
            second = res;
        }
        return res;
    }

    /**
     * Create an array of length <code>N</code> and fill them with numbers from 1 to 10,
     * then sort the array with Bubble-sort algorithm.
     *
     * @return Sorted array
     */
    public int[] sort() {
        boolean t = true;
        int temp = 0;
        int[] mas = new int[N];
        Random rand = new Random();
        for (int i = 0; i < N; i++) {
            mas[i] = rand.nextInt(10) + 1;
        }
        while (t) {
            t = false;
            for (int i = 0; i < mas.length - 1; i++) {
                if (mas[i] > mas[i + 1]) {
                    temp = mas[i];
                    mas[i] = mas[i + 1];
                    mas[i + 1] = temp;
                    t = true;
                }
            }
        }
        return mas;
    }

    /**
     * The main function.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        Task2 tsk = new Task2();
        if (args.length == 0) {
            return;
        }
        if (args[0].equals("prime")) {
            System.out.println(tsk.prime());
        } else if (args[0].equals("fibonacci")) {
            System.out.println(tsk.fibonacci());
        } else if (args[0].equals("sort")) {
            int[] mas = tsk.sort();
            for (int i = 0; i < mas.length; i++) {
                System.out.print(mas[i] + " ");
            }
        }
    }
}
