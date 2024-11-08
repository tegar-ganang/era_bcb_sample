package net.googlecode.vasetskiy;

import java.util.Random;
import java.math.BigInteger;
import static java.lang.Math.sqrt;

/**
 * Class, that realize task2 requirements.
 * Depending on the command line arguments, execute:
  *1) If the argument is 'prime' to find the maximum prime number not exceeding N.
  *2) If the argument 'fibonacci' to find the one hundredth member of the sequence.
  *3) If the argument is 'sort' program creates and populates an array of length N
 * random numbers from 1 to 10 and sorted of bubble sort.
 * @author Vasetskiy Vladislav
 */
public class Task2 {

    /**
        * Constant, which use functions of the class.
        */
    public static final int N = 100;

    /**
        * 1) procedure of counting the maximum prime number < N
        *
        * @return the prime number
        */
    public int prime() {
        for (int i = N + (N % 2) - 1; i > 1; i -= 2) {
            for (int j = 3; j < sqrt(i); j += 2) {
                if (i % j == 0) break;
                if (j >= sqrt(i) - 2) {
                    return i;
                }
            }
        }
        return N;
    }

    /**
        * 2) procedure of counting the n-th term of the Fibonacci.
        *
        * @return n-th term of the Fibonacci
        */
    public BigInteger fibonacci() {
        BigInteger An = BigInteger.ZERO;
        BigInteger An_1 = BigInteger.ONE;
        BigInteger An_2 = BigInteger.ZERO;
        if (N == 1) return BigInteger.ONE; else {
            for (int i = 2; i <= N; i++) {
                An = An_1.add(An_2);
                An_2 = An_1;
                An_1 = An;
            }
            return An;
        }
    }

    /**
        * 3) Sort array with bubbleSort method.
        */
    private void bubbleSort(int[] mas) {
        boolean t = true;
        int temp = 0;
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
        {
        }
        if (args[0].equals("prime")) {
            System.out.println(task.prime());
        } else if (args[0].equals("fibonacci")) {
            System.out.println(task.fibonacci());
        } else if (args[0].equals("sort")) {
            int[] mas = task.sort();
            for (int i = 0; i < mas.length; i++) {
                System.out.print(mas[i] + " ");
            }
        }
    }
}
