import iwork.util.*;

public class ReaderWriterMutexTest {

    ReaderWriterMutex testMutex = new ReaderWriterMutex(new TestRW());

    class TestRW {

        int value;

        public int readVal() {
            return value;
        }

        public int writeVal(int newValue) {
            int oldVal = value;
            value = newValue;
            return oldVal;
        }
    }

    class Reader extends FixedIteration {

        int threadNum;

        long totalBlockTime = 0;

        public boolean init() {
            return true;
        }

        public void finish() {
            System.out.println("R" + threadNum + ": Average block time " + (double) totalBlockTime / (double) getTotalIterations() + " ms. Exiting...");
            System.out.flush();
        }

        public void executeTask() {
            long startTime = System.currentTimeMillis();
            TestRW testObj = (TestRW) testMutex.getReadLock();
            totalBlockTime += System.currentTimeMillis() - startTime;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            int readVal = testObj.readVal();
            testMutex.releaseReadLock();
        }

        Reader(int threadNum, int iterations) {
            super(iterations);
            this.threadNum = threadNum;
        }
    }

    class Writer extends FixedIteration {

        int threadNum;

        long totalBlockTime = 0;

        public boolean init() {
            return true;
        }

        public void finish() {
            System.out.println("W" + threadNum + ": Average block time " + (double) totalBlockTime / (double) getTotalIterations() + " ms. Exiting...");
            System.out.flush();
        }

        public void executeTask() {
            long startTime = System.currentTimeMillis();
            TestRW testObj = (TestRW) testMutex.getWriteLock();
            totalBlockTime += System.currentTimeMillis() - startTime;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            int writeVal = testObj.readVal() + 1;
            testObj.writeVal(writeVal);
            testMutex.releaseWriteLock();
        }

        Writer(int threadNum, int iterations) {
            super(iterations);
            this.threadNum = threadNum;
        }
    }

    public ReaderWriterMutexTest(int numReaders, int numWriters, int numTests, double maxSleep) {
        Thread threadArray[] = new Thread[numReaders + numWriters];
        int j = 0;
        for (int i = 0; i < numReaders; i++) {
            Thread newThread = new PeriodicExecute(new RandomSleeper(new Reader(i, numTests), 1, maxSleep));
            newThread.start();
            threadArray[j++] = newThread;
        }
        for (int i = 0; i < numWriters; i++) {
            Thread newThread = new PeriodicExecute(new RandomSleeper(new Writer(i, numTests), 1, maxSleep));
            newThread.start();
            threadArray[j++] = newThread;
        }
        for (int i = 0; i < numReaders + numWriters; i++) {
            try {
                threadArray[i].join();
            } catch (InterruptedException e) {
                System.out.println("Interrupted...");
                System.out.flush();
            }
        }
        System.out.println("All done...");
    }

    public static void main(String args[]) {
        if (args.length < 4) {
            System.out.println("java ReaderWriterMutexTest <numReaders> " + " <numWriters> <numTests> <maxSleep>");
            return;
        }
        int numReaders = Integer.parseInt(args[0]);
        int numWriters = Integer.parseInt(args[1]);
        int numTests = Integer.parseInt(args[2]);
        double maxSleep = (double) (Integer.parseInt(args[3]));
        System.out.println("Testing with:" + "\n\tReaders:\t" + numReaders + "\n\tWriters:\t" + numWriters + "\n\tTests:\t" + numTests + "\n\tMax Sleep:\t" + maxSleep);
        new ReaderWriterMutexTest(numReaders, numWriters, numTests, maxSleep);
    }
}
