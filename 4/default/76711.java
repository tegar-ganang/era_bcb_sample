import org.opentrust.jsynch.ReadWriteLock;

public class Test {

    protected static ReadWriteLock testLock = new ReadWriteLock();

    public static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
        }
    }

    public static void readerThread() {
        new Thread() {

            public void run() {
                testLock.waitReadLock();
                try {
                    System.err.println("I have the read lock!");
                    delay(2000);
                } finally {
                    testLock.releaseReadLock();
                }
            }
        }.start();
    }

    public static void writerThread() {
        new Thread() {

            public void run() {
                testLock.waitWriteLock();
                try {
                    delay(1000);
                    System.err.println("I have the write lock!");
                    delay(5000);
                } finally {
                    testLock.releaseWriteLock();
                }
            }
        }.start();
    }

    public static void writerThread2() {
        new Thread() {

            public void run() {
                testLock.waitWriteLock(2000);
                try {
                    delay(1000);
                    System.err.println("I have the write lock 2!");
                    delay(5000);
                } finally {
                    testLock.releaseWriteLock();
                }
            }
        }.start();
    }

    protected static void basicTest() {
        readerThread();
        writerThread();
        writerThread();
        readerThread();
    }

    protected static void timeoutTest() {
        readerThread();
        writerThread();
        writerThread2();
        readerThread();
    }

    public static void main(String args[]) {
        basicTest();
        delay(20000);
        timeoutTest();
    }
}
