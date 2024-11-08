package net.googlecode.demenkov;

import java.util.Random;
import java.math.BigInteger;

/**
 * Class thar realise Task number 2: depends upon the argument it counts a simple number <=n(n=100),
 * number n in fibonacci numbers list,
 * or array of numbers(1-10) length=n, sorted by BubbleSort.
 *
 * @author Yura Demenkov
 */
public class Task2 {

    /**
     * N - constant from task,=100.
     */
    private static final int N = 100;

    /**
     * Counts max simple number not greater then m.
     *
     * @param m limit to look for max simple number below.
     * @return max simple number.
     */
    public int prime(int m) {
        if (m > 1) {
            int maxSimple = 1;
            for (int i = 1; i <= m; i++) {
                boolean simple = true;
                for (int j = 2; j < i; j++) {
                    if ((i % j) == 0) {
                        simple = false;
                        break;
                    }
                }
                if (simple) {
                    maxSimple = i;
                }
            }
            return maxSimple;
        } else {
            return 0;
        }
    }

    /**
     * Counts number m of fibonacci numbers list.
     * Using BigInteger class because 100 number in Fibonacci list greater then long's limit.
     *
     * @param m number of fibonacci numbers list to count.
     * @return numeric value of number m in fibonacci numbers list.
     */
    public BigInteger fibonacci(int m) {
        if (m > 0) {
            BigInteger temp;
            BigInteger[] fib = new BigInteger[3];
            fib[0] = BigInteger.valueOf(1);
            fib[1] = BigInteger.valueOf(1);
            if (m > 2) {
                for (int i = 3; i <= m; i++) {
                    fib[2] = BigInteger.valueOf(0);
                    temp = BigInteger.valueOf(0);
                    fib[2] = fib[0].add(fib[1]);
                    temp = temp.add(fib[1]);
                    fib[1] = fib[1].add(fib[0]);
                    fib[0] = BigInteger.valueOf(0);
                    fib[0] = fib[0].add(temp);
                }
            } else {
                fib[2] = BigInteger.valueOf(1);
            }
            return fib[2];
        } else {
            return BigInteger.valueOf(0);
        }
    }

    /**
     * Randomize input array of integers.
     *
     * @param mas array of integers.
     */
    public void randomizeArray(int mas[]) {
        Random rand = new Random();
        for (int i = 0; i < mas.length; i++) {
            mas[i] = rand.nextInt(10) + 1;
        }
    }

    /**
     * Sorts input array of integers.
     *
     * @param mas input array of integers.
     */
    public void sort(int[] mas) {
        int temp;
        boolean t = true;
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
    }

    /**
     * Main class. Depends upon argument, it count:
     * arg=prime - max Simple number
     * arg=fibonacci - value of n number in fibonacci numbers list
     * arg=sort - creates array of random numbers,sorted by BubbleSort.
     *
     * @param args - input arguments.
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            Task2 task = new Task2();
            String param = args[0];
            if (param.equals("prime")) {
                if (task.prime(N) != 0) {
                    System.out.println("Max Simple number not greater then " + N + " is " + task.prime(N));
                } else {
                    System.out.println("Wrong input number");
                }
            } else if (param.equals("fibonacci")) {
                if (task.fibonacci(N) != BigInteger.valueOf(0)) {
                    System.out.println(N + " number of fibonacci numbers is " + task.fibonacci(N));
                } else {
                    System.out.println("Wrong input number");
                }
            } else {
                if (param.equals("sort")) {
                    int[] mas = new int[N];
                    task.randomizeArray(mas);
                    task.sort(mas);
                    for (int x : mas) {
                        System.out.print(x + " ");
                    }
                } else {
                    System.out.println("Unknown parametr");
                }
            }
        } else System.out.println("Enter parametr please");
    }
}
