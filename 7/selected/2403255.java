package net.googlecode.exigenlab.pryadko;

import java.util.Random;

public class HomeWork {

    protected void prime(int a) {
        Random generator = new Random();
        int[] masiv = new int[a];
        for (int i = 0; i <= a; i++) {
            masiv[i] = generator.nextInt(100);
        }
        int max = 0;
        for (int i = 0; i <= a; i++) {
            if (masiv[i] > max) {
                max = masiv[i];
            }
        }
        System.out.println("prime: " + max);
    }

    protected void fibonacci(int a) {
        long[] masiv = new long[a + 2];
        masiv[1] = 1;
        masiv[2] = 1;
        for (int i = 3; i <= a; i++) {
            masiv[i] = masiv[i - 1] + masiv[i - 2];
        }
        System.out.println("fibonacci: " + masiv[a]);
    }

    protected void sort(int a) {
        int[] masiv = new int[a];
        Random rand = new Random();
        for (int i = 0; i <= a; i++) {
            masiv[i] = rand.nextInt(200);
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

    static int N = 100;

    public static void main(String[] args) {
        HomeWork names = new HomeWork();
        if (args.length > 0) {
            if (args[0].equals("prime")) {
                names.prime(N);
            } else if (args[0].equals("fibonacci")) {
                names.fibonacci(N);
            } else if (args[0].equals("sort")) {
                names.sort(N);
            } else {
                System.out.println("error option");
            }
        } else {
            System.out.println("enter option");
        }
    }
}
