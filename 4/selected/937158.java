package testdb;

import java.util.*;

/**
 *
 * @author francescoburato
 */
public class ThreadTest {

    public static void main(String args[]) throws InterruptedException {
        Thread t = new Thread(new SimpleThread1()), t1 = new Thread(new SimpleThread2("Mess 2"));
        t.start();
        t1.start();
    }
}

class SimpleThread1 implements Runnable {

    private Scanner msg;

    public SimpleThread1() {
        this.msg = new Scanner(System.in);
    }

    public void run() {
        while (true) {
            System.out.println(this.msg.nextLine());
        }
    }
}

class SimpleThread2 implements Runnable {

    private String msg;

    public SimpleThread2(String s) {
        this.msg = s;
    }

    public void run() {
        while (true) {
            System.out.println(this.msg);
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
            }
        }
    }
}
