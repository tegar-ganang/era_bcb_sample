package com.thesett.junit.extensions.example;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.thesett.common.throttle.SleepThrottle;
import com.thesett.common.throttle.Throttle;
import com.thesett.junit.extensions.TestThreadAware;
import com.thesett.junit.extensions.TimingController;
import com.thesett.junit.extensions.TimingControllerAware;
import junit.framework.TestCase;

/**
 * ContinuousTestPerf is an example of a self-timed test case that runs continuously, until it is told to stop. This is
 * often usefull when writing asynchronous test-cases, where the number of iterations to complete is not to be set in
 * advance, that is, typically when a duration test will be used. The desired effect is to run some test processes
 * continously, logging many timings, until the duration expires, or the test framework is shut-down by pressing CTRL-C.
 *
 * <p/>One alternative solution, is to use an asymptotic test case, and set a size parameter of a few
 * tens/hundres/thousands of iterations (a large batch), and run the test processes against that many iterations of a
 * test case, then exit the test method, to run another batch only if there is time left to do so. This is a fairly ugly
 * solution, because it turns what should be a set of continously running processes into an approximation of that by
 * running them in large batches.
 *
 * <p/>To give a concrete example. Suppose a test is to consist of a database process, and one writer and one reader
 * thread. The writer thread continously writes new records into the database at a controlled rate. The reader thread
 * queries against the database, to see how quickly it can consume the results of a test query against it as the
 * database grows, it also cleans up the database as it goes, removing old records based on some criteria. The idea
 * behind this example, is that it is a fairly complex producer/consumer test that may behave unpredictably enough that
 * it requires analysis by simulation. The reader thread runs continously and asynchronously, logging results back to
 * the test framework through the {@link TimingController} interface. The aim is to plot a graph of the query duration
 * as the time goes by, in order to understand how the system behaves and to resolve any issues with its performance.
 * One way to simulate the continuous behaviour of this set of processes would be to run batches of 1000 reads or
 * writes, logging out 1000 timing results, before terminating the test method. This would mean that the write thread
 * would need to coordinate with the read thread, waiting on the read thread when it reaches 1000 writes, and having the
 * read thread unblock it when it consumes 1000 reads, whereupon that cycle of the test completes as a batch. The
 * batchiness is undesirable, for example; it may artificially prevent the depth of the producer/consumer event stream
 * from growing beyond 1000, which may be smaller than a real buffer limit that the code under test might be
 * implementing; it may negate the effect of a read or write bias which is affecting the code under test; it may provide
 * an opportunity for the garbage collector to run; and so on.
 *
 * <p/>ContinuousTestPerf uses the {@link TimingController#completeTest} call-back, to explicitly log timings with the
 * test framework. This call-back documents that it throws an InterruptedException, if the test is to stop immediately.
 * The framework will do this, if it is shutting down (because CTRL-C was invoked), or because the test is running for a
 * fixed duration and that duration has expired. The example uses a bounded producer/consumer queue under a particular
 * event arrival rate, and processing time, and provides statistics on the throughput and latency of events passing
 * through this queueing system.
 *
 * <p/>One thing to note, is that the {@link TimingController} for the test thread, is set in the per-thread test
 * fixture, during the {@link #threadSetUp()} method. This is because the timing call-backs are made by the reader
 * thread, which is a different thread to the one which the test framework calls the test method from. So long as the
 * correct timing controller is used, the framework will be able to identify which test thread the timings are for.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Time the latency of an event over a bounded producer/consumer queue under varying arrival/processing rates.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class ContinuousTestPerf extends TestCase implements TimingControllerAware, TestThreadAware {

    /** Defines the event arrival rate. */
    private static final float ARRIVAL_RATE = 10f;

    /** Defines the event processing rate, which is 1/(processing time). */
    private static final float PROCESSING_RATE = 11f;

    /** Defines the maximum size of the event buffer. */
    private static final int BUFFER_SIZE = 10;

    /** The timing controller. */
    private TimingController tc;

    /** Thread local to hold the per-thread test fixtures. */
    final ThreadLocal<PerThreadFixture> threadFixtures = new ThreadLocal<PerThreadFixture>();

    /**
     * Constructs a test case with the given name.
     *
     * @param name The name of the test case (matching a method name) to run.
     */
    public ContinuousTestPerf(String name) {
        super(name);
    }

    /** Time the latency of an event over a bounded producer/consumer queue under varying arrival/processing rates. */
    public void testContinuously() {
        PerThreadFixture threadFixture = threadFixtures.get();
        threadFixture.timingController = getTimingController().getControllerForCurrentThread();
        Thread readerThread = new Thread(new Reader(Thread.currentThread(), threadFixture));
        readerThread.start();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                threadFixture.writerThrottle.throttle();
                threadFixture.buffer.put(System.nanoTime());
            } catch (InterruptedException e) {
                e = null;
                if (threadFixture.shouldStop) {
                    break;
                } else {
                    Thread.currentThread().interrupt();
                    fail("Writer thread was unexpectedly interrtuped.");
                }
            }
        }
    }

    /**
     * Used by test runners that can supply a {@link TimingController} to set the controller on an aware test.
     *
     * @param controller The timing controller.
     */
    public void setTimingController(TimingController controller) {
        tc = controller;
    }

    /**
     * Gets the timing controller passed into the {@link #setTimingController(TimingController)} method.
     *
     * @return The timing controller, or null if none has been set.
     */
    public TimingController getTimingController() {
        return tc;
    }

    /** Called when a test thread is created. */
    public void threadSetUp() {
        PerThreadFixture threadFixture = new PerThreadFixture();
        threadFixture.buffer = new LinkedBlockingQueue<Long>(BUFFER_SIZE);
        threadFixture.writerThrottle = new SleepThrottle();
        threadFixture.writerThrottle.setRate(ARRIVAL_RATE);
        threadFixture.readerThrottle = new SleepThrottle();
        threadFixture.readerThrottle.setRate(PROCESSING_RATE);
        threadFixture.shouldStop = false;
        threadFixtures.set(threadFixture);
    }

    /** Called when a test thread is destroyed. */
    public void threadTearDown() {
        threadFixtures.remove();
    }

    /**
     * PerThreadFixture contains a test fixture for each test thread, this consists of a bounded producer/consumer
     * buffer, throttles for the reader and writer threads, and a flag to indicate that the test should stop.
     */
    private static class PerThreadFixture {

        /** Holds the producer/consumer buffer. */
        BlockingQueue<Long> buffer;

        /** Holds the throttle for the producer to control the arrival rate. */
        Throttle writerThrottle;

        /** Holds the throttle for the consumer to simulate a processing time. */
        Throttle readerThrottle;

        /** Holds a flag to indicate to the producer that it should stop the test. */
        volatile boolean shouldStop;

        /** Holds the timing controller for the test thread. */
        TimingController timingController;
    }

    /**
     * Reader implements a continous read cycle, that consumes events from the test buffer, and logs them as latency
     * timings.
     */
    private static class Reader implements Runnable {

        /** Holds a reference to the writer thread, so that it may be interrupted. */
        private final Thread writerThread;

        /** Holds a reference to the per-thread fixture for the main test thread. */
        private final PerThreadFixture threadFixture;

        /**
         * Creates a reader with a reference to the writer thread, so that the writer may be interrupted and asked to
         * stop, when the test is to complete.
         *
         * @param writerThread  The writer thread.
         * @param threadFixture The test fixture for the main test thread.
         */
        public Reader(Thread writerThread, PerThreadFixture threadFixture) {
            this.writerThread = writerThread;
            this.threadFixture = threadFixture;
        }

        /**
         * Consumes messages from the event buffer, and logs timings for the event latencies, until the test framework
         * interrupts this task, wherupon, it in turn interrupts the writer thread with the 'stop' flag set, to indicate
         * that the entire test procedure should complete.
         */
        public void run() {
            boolean interrupted = false;
            while (!interrupted) {
                try {
                    long start = threadFixture.buffer.take();
                    threadFixture.readerThrottle.throttle();
                    long now = System.nanoTime();
                    threadFixture.timingController.completeTest(true, 1, now - start);
                } catch (InterruptedException e) {
                    e = null;
                    interrupted = true;
                }
            }
            threadFixture.shouldStop = true;
            writerThread.interrupt();
        }
    }
}
