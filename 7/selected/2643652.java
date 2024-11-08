package net.googlecode.exigenlab.task2;

import java.util.Arrays;
import java.util.Random;
import java.math.BigInteger;

/**
 * Class that realizes task 2. Use the command line arguments :
 * <ul>
 * <li>If argument is "prime" - counts the max prime number that less than N;</li>
 * <li>If argument is "fibonacci" - printing N-th member of Fibonacci row;</li>
 * <li>If argument is "sort" - create a random filled array(from 1 to 10) of
 * length N, and sorting it with Bubble-sort algorithm.</li>
 * </ul>
 * 
 * @author Poddybniak Petr
 */
public class MainClass {

    /**
	 * <code>N</code> constant specified in the task;
	 */
    public static final int N = 100;

    /**
	 * Counts the max prime number that less than n, using the sieve of
	 * Eratosthenes algorithm and printing it.
	 * 
	 * @param n
	 *            - the limit of search for max simple number.
	 */
    public void getMaxPrime(int n) {
        int primeMax = 2;
        if (n <= 2) {
            System.out.println("First prime number is " + primeMax);
        } else {
            boolean[] primes = new boolean[n + 1];
            Arrays.fill(primes, 2, n + 1, true);
            for (int i = 2; i * i <= n; i++) {
                if (primes[i]) {
                    for (int k = i * i; k <= n; k += i) {
                        primes[k] = false;
                    }
                }
            }
            for (int i = n; i >= 0; i--) {
                if (primes[i] == true) {
                    primeMax = i;
                    System.out.println("Max prime number (<=" + n + ") is " + primeMax);
                    break;
                }
            }
        }
    }

    /**
	 * Counting n-th member of Fibonacci row. <code>BigInteger</code> class is
	 * used because the result exceeds the limits of int.
	 * 
	 * @return res - the n-th member of Fibonacci row.
	 */
    public BigInteger fibonacci(int n) {
        BigInteger res = BigInteger.ONE;
        BigInteger prev = BigInteger.ZERO;
        BigInteger next = BigInteger.ONE;
        if (n <= 0) {
            return prev;
        } else if (n == 1) {
            return res;
        } else {
            for (int i = 2; i <= n; i++) {
                res = prev.add(next);
                prev = next;
                next = res;
            }
            return res;
        }
    }

    /**
	 * Create a random filled array(from 1 to 10) of length <code>n</code>, and
	 * sorting it with Bubble-sort algorithm.
	 * 
	 * @param n
	 *            - length of array.
	 * @return mas - output random filled and sorted array.
	 */
    public int[] getRandMas(int n) {
        boolean t = true;
        int interim = 0;
        int[] mas = new int[n];
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            mas[i] = rand.nextInt(10) + 1;
        }
        while (t) {
            t = false;
            for (int i = 0; i < mas.length - 1; i++) {
                if (mas[i] > mas[i + 1]) {
                    interim = mas[i];
                    mas[i] = mas[i + 1];
                    mas[i + 1] = interim;
                    t = true;
                }
            }
        }
        return mas;
    }

    /**
	 * The main class that calls methods depending on the command line args.
	 * 
	 * @param args
	 *            command line arguments
	 */
    public static void main(String[] args) {
        MainClass task = new MainClass();
        if (args.length > 0) {
            if (args[0].equals("prime")) {
                task.getMaxPrime(N);
            } else if (args[0].equals("fibonacci")) {
                System.out.println(N + "-s member of fibonacci row is " + task.fibonacci(N));
            } else if (args[0].equals("sort")) {
                System.out.println("Random sorted array:");
                int[] mas = new int[N];
                mas = task.getRandMas(N);
                for (int k : mas) System.out.print(k + " ");
            } else {
                System.out.println("Incorrect parameters!");
            }
        } else System.out.println("Enter parameters!");
    }
}
