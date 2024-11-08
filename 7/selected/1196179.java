package net.googlecode.myshenko;

import java.math.BigInteger;
import java.util.Random;

/**
 * Class solves tasks depending on argument:
 * <ul>
 * <li>argument equals "prime": find max prime les than N.</li>
 * <li>argument equals "fibonacci": find N-th element of Fibonacci sequence.</li>
 * <li>argument equals "sort": generate and sort array of random numbers from 1 to 10,
 * array capacity N.</li>
 * </ul>
 *
 * @author Vitalii Myshenko
 * @since 21.03.11
 */
public class Task2 {

    /**
     * <code>N</code> constant specified in the task
     */
    private static final int N = 100;

    /**
     * Finds max prime number les than n using sieve of Eratosthenes method
     *
     * @param n the limit of search
     * @return value of the max prime number
     */
    private int prime(int n) {
        int[] numberMas = new int[n];
        for (int i = 0; i < n; i++) {
            numberMas[i] = i;
        }
        numberMas[1] = 0;
        for (int i = 2; i < n; i++) {
            if (numberMas[i] != 0) {
                for (int j = 2 * numberMas[i]; j < n; j += numberMas[i]) {
                    numberMas[j] = 0;
                }
            }
        }
        int cntr = 99;
        while (numberMas[cntr] == 0) {
            cntr--;
        }
        return numberMas[cntr];
    }

    /**
     * Finds n-th element of Fibonacci sequence.
     * <p><code>BigInteger</code> class is used because result exceeds the limit of int.</p>
     *
     * @param n the number of searched element
     * @return value of n-th element of Fibonacci sequence
     */
    private BigInteger fibonacci(int n) {
        BigInteger frst = BigInteger.ZERO;
        BigInteger scnd = BigInteger.ONE;
        BigInteger res = frst;
        if (n > 0) {
            if (n == 0) {
                res = BigInteger.ZERO;
            } else if (n == 1) {
                res = BigInteger.ONE;
            } else {
                for (int i = 2; i < n; i++) {
                    res = frst.add(scnd);
                    frst = scnd;
                    scnd = res;
                }
            }
        }
        return res;
    }

    /**
     * Fills array with random numbers (value = 1..10, length n) and sorts them with Bubble-sort algorythm
     *
     * @param n array length
     * @return sorted array
     */
    private int[] sort(int n) {
        int[] mas = new int[n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            mas[i] = rand.nextInt(10) + 1;
        }
        boolean t = true;
        int tmp = 0;
        while (t) {
            t = false;
            for (int i = 0; i < mas.length - 1; i++) {
                if (mas[i] > mas[i + 1]) {
                    tmp = mas[i];
                    mas[i] = mas[i + 1];
                    mas[i + 1] = tmp;
                    t = true;
                }
            }
        }
        return mas;
    }

    /**
     * Prints result of calculations to console
     *
     * @param args input arguments
     */
    public static void main(String[] args) {
        Task2 task = new Task2();
        if (args[0].equals("prime")) {
            System.out.println("Max prime = " + task.prime(N));
        } else if (args[0].equals("fibonacci")) {
            System.out.println(N + " number of fibonacci numbers is " + task.fibonacci(N));
        } else if (args[0].equals("sort")) {
            int[] mas = task.sort(N);
            for (int i = 0; i < N; i++) {
                System.out.println(mas[i]);
            }
        } else {
            System.out.println("Wrong parameter");
            return;
        }
    }
}
