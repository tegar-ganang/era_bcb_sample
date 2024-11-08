package cz.tmapy.tools.wmstester;

import cz.tmapy.tools.wmstester.config.Config;
import cz.tmapy.tools.wmstester.tests.AbstractWmsTester;
import cz.tmapy.tools.wmstester.tests.RandomExtentRead;
import cz.tmapy.tools.wmstester.tests.ZoomExtentRead;
import cz.tmapy.tools.wmstester.tests.RandomSequenceRead;
import cz.tmapy.tools.wmstester.tests.RandomSizeRead;

/**
 * Hello world!
 *
 */
public class Main {

    /** Layer name is #PCDATA type => Every element of text is parsed. Parsing sofar only means
     * that &, < and > are replaced by their XML entities (&amp;, &lt; and &gt;). =>
     * Sequense '&&' can not be in any part of layer name.
     */
    private Thread writerThread;

    private SingleThreadTester stt;

    private MultiThreadTester<SingleThreadTester> mtt;

    private Config conf;

    private int debug = 0;

    public static void main(String[] args) {
        new Main(args);
    }

    public Main(String[] args) {
        try {
            conf = new Config(args);
            debug = conf.getDebug();
            Runtime.getRuntime().addShutdownHook(new ShutdownInterceptor());
            System.out.print("Start log writer thread... ");
            StatsWriter sw = SingletonFactory.getInstance(StatsWriter.class);
            writerThread = new Thread(sw);
            writerThread.setPriority(Thread.MIN_PRIORITY);
            writerThread.start();
            System.out.println("\t OK");
            while (conf.nextTest()) {
                System.out.println("Test: " + conf.getName());
                System.out.println("---------------------------------------");
                try {
                    WmsClient wmsClient = new WmsClient(conf.getUrl(), conf.getLogin(), conf.getPasswd(), conf.getVendor());
                    wmsClient.printCapabilities();
                    if (!wmsClient.checkLayers(conf.getLayers())) {
                        System.err.println("ERR: Selected Layer isn't available:");
                        continue;
                    }
                    if (!wmsClient.checkLayers(conf.getLayers(), conf.getSrs())) {
                        System.err.println("ERR: Selected Srs isn't available:");
                        continue;
                    }
                } catch (Exception e) {
                    System.out.println("WARN: GetCapabilities for test " + conf.getName() + " failed with: " + e.getMessage());
                    System.out.println("WARN: with URL: " + conf.getUrl());
                    continue;
                }
                try {
                    System.out.println("Start Multithread tester:");
                    AbstractWmsTester test;
                    String name = conf.getMode();
                    if (conf.getMode().equalsIgnoreCase("rer")) {
                        test = new RandomExtentRead();
                    } else if (conf.getMode().equalsIgnoreCase("zer")) {
                        test = new ZoomExtentRead();
                    } else if (conf.getMode().equalsIgnoreCase("rsr")) {
                        test = new RandomSizeRead();
                    } else {
                        test = new RandomSequenceRead();
                    }
                    test.setTestConf(conf);
                    mtt = createMultiThreadTest(test, conf.getThreads(), conf.getIncrease(), conf.getTotalTime());
                    mtt.start();
                    mtt.join();
                } catch (Exception e) {
                    System.out.println("WARN: test :" + conf.getName() + " Connection problem ?? : " + e.getMessage());
                }
            }
            shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private <T extends AbstractWmsTester> SingleThreadTester createSingleThreadTest(T clazz) {
        return new SingleThreadTester<T>(clazz);
    }

    private <T extends AbstractWmsTester> MultiThreadTester<SingleThreadTester> createMultiThreadTest(T clazz, int threads, long delay, long totalTime) throws CloneNotSupportedException {
        System.out.print("Pre-create " + String.valueOf(threads) + " client:");
        MultiThreadTester<SingleThreadTester> _mtt = new MultiThreadTester<SingleThreadTester>(delay, totalTime);
        for (int i = 0; i < threads; i++) {
            T runner = (T) clazz.clone();
            _mtt.addThread(createSingleThreadTest(runner));
            System.out.print("*");
        }
        System.out.println("\t OK");
        return _mtt;
    }

    private void shutdown() throws InterruptedException {
        if (stt != null) {
            stt.kill();
            stt.join();
        }
        if (mtt != null) {
            mtt.killAll();
            mtt.join();
        }
        if (writerThread != null) {
            System.out.println("Stop log writer thread... ");
            writerThread.interrupt();
            writerThread.join();
        }
        String[] bay = new String[11];
        bay[0] = "Stoped.";
        bay[1] = "See you soon";
        bay[2] = "Thank you for running and Goodbye...";
        bay[3] = "Bay...";
        bay[4] = "Terminated sir!";
        bay[5] = "Thanks for the use, Take care...";
        bay[6] = "GoodBay...";
        bay[7] = "Bay...";
        bay[8] = "Bay...";
        bay[9] = "Bay...";
        bay[10] = "Thank you for running, I'll miss you...";
        System.out.println(bay[new java.util.Random().nextInt(((Thread.currentThread().getName().equals("main") == true) ? 10 : 1))]);
    }

    class ShutdownInterceptor extends Thread {

        @Override
        public void run() {
            System.out.println("\nTerminating... ");
            try {
                try {
                    shutdown();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } finally {
            }
        }
    }
}
