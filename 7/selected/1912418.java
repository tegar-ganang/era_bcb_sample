package net.googlecode.exigenlab;

import java.util.Random;

/**
 * This class has three methods: prime, fibonacci and sort.
 * Each method has own algorithm for calculating the number N, which equals 100
 */
public class HomeWork2 {

    /**
     * n - const
     */
    static final int n = 100;

    /**
     * looking for MAX prime to N
     *
     * @param a is N
     */
    protected void prime(int a) {
        int[] masiv = new int[a + 1];
        for (int i = 2; i <= a; i++) {
            masiv[i] = i;
        }
        int k;
        for (int i = 2; i <= a; i++) {
            for (k = i * i; k <= a; k = k + i) {
                masiv[k] = 0;
            }
        }
        int d = a;
        while (masiv[d] == 0) {
            d--;
        }
        System.out.println("prime: " + masiv[d]);
    }

    /**
     * locking for number of the Fibbonacci numbers
     *
     * @param a is number of the Fibbonacci numbers
     */
    protected void fibonacci(int a) {
        long[] masiv = new long[a + 2];
        masiv[1] = 1;
        masiv[2] = 1;
        for (int i = 3; i <= a; i++) {
            masiv[i] = masiv[i - 1] + masiv[i - 2];
        }
        System.out.println("fibonacci: " + masiv[a]);
    }

    /**
     * Sorting array
     *
     * @param a is value of the array
     */
    protected void sort(int a) {
        int[] masiv = new int[a + 1];
        Random fff = new Random();
        for (int i = 0; i <= a; i++) {
            masiv[i] = fff.nextInt(9);
        }
        int d;
        for (int j = 0; j < a; j++) {
            for (int i = 0; i < a; i++) {
                if (masiv[i] < masiv[i + 1]) {
                } else {
                    d = masiv[i];
                    masiv[i] = masiv[i + 1];
                    masiv[i + 1] = d;
                }
            }
        }
        while (a != 0) {
            System.out.println("sort: " + masiv[a]);
            a--;
        }
    }

    /**
     * Method "Main" calls option
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        HomeWork2 aaa = new HomeWork2();
        if (args.length > 0) {
            if (args[0].equals("prime")) {
                aaa.prime(n);
            } else if (args[0].equals("fibonacci")) {
                aaa.fibonacci(n);
            } else if (args[0].equals("sort")) {
                aaa.sort(n);
            } else {
                System.out.println("error option");
            }
        } else {
            System.out.println("enter option");
        }
    }
}
