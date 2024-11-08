import org.opentrust.jsynch.*;

/**
 *	A class to test various LockableQueue scenarios.
 */
public class Test {

    public static void simple() {
        LockableQueue lq = new LockableQueue(10);
        for (int i = 0; i < 10; i++) {
            lq.insertItem(new Object());
        }
        try {
            lq.insertItem(new Object());
            System.out.println("Expected FullException!");
        } catch (LockableQueue.FullException fe) {
            System.out.println("Expected: " + fe);
        }
        System.out.println(lq.removeItem());
        System.out.println("Inserting last item");
        lq.insertItem(new Object(), true);
        try {
            lq.insertItem(new Object());
            System.out.println("Expected ClosedException!");
        } catch (LockableQueue.ClosedException ce) {
            System.out.println("Expected: " + ce);
        }
        try {
            while (lq.tryToRemoveItem() != null) {
                System.out.println(lq.removeItem());
            }
        } catch (LockableQueue.ClosedException expected) {
            System.out.println("Expected: " + expected);
        }
    }

    public static void startReader(final int num, final LockableQueue lq) {
        new Thread() {

            int itemsRead = 0;

            public void run() {
                while (true) {
                    try {
                        if (lq.waitForLockable()) {
                            Object item;
                            if ((item = lq.removeItem()) != null) {
                                itemsRead++;
                                System.out.println("Reader (" + num + "): " + item);
                            }
                            sleep(100);
                        } else {
                            System.out.println("Reader (" + num + "): Unable to acquire LockableQueue. Quitting...");
                            break;
                        }
                    } catch (LockableQueue.ClosedException ce) {
                        System.out.println("Reader (" + num + "): (expected) " + ce);
                        break;
                    } catch (Exception e) {
                        System.out.println("Reader (" + num + "): (unexpected)" + e);
                        break;
                    }
                }
                System.out.println("Reader (" + num + "): Items Read = " + itemsRead);
            }
        }.start();
    }

    public static void startReader2(final int num, final LockableQueue lq) {
        new Thread() {

            int itemsRead = 0;

            public void run() {
                while (true) {
                    try {
                        Object item;
                        if ((item = lq.removeItem()) != null) {
                            itemsRead++;
                            System.out.println("Reader (" + num + "): " + item);
                        }
                        sleep(100);
                    } catch (LockableQueue.ClosedException ce) {
                        System.out.println("Reader (" + num + "): (expected) " + ce);
                        break;
                    } catch (Exception e) {
                        System.out.println("Reader (" + num + "): (unexpected)" + e);
                        break;
                    }
                }
                System.out.println("Reader (" + num + "): Items Read = " + itemsRead);
            }
        }.start();
    }

    public static Thread startWriter(final int num, final LockableQueue lq) {
        Thread t = new Thread() {

            int itemsWritten = 0;

            public void run() {
                while (true) {
                    try {
                        System.out.println("Writer (" + num + "): Writing block of 5...");
                        for (int i = 0; i < 5; itemsWritten++, i++) {
                            lq.insertItem(new Object());
                        }
                        System.out.println("Writer (" + num + "): OK");
                        if (itemsWritten >= 50) {
                            break;
                        }
                        sleep(50);
                    } catch (Exception e) {
                        System.out.println("Writer (" + num + "): (unexpected)" + e);
                    }
                }
                System.out.println("Writer (" + num + "): Items Written = " + itemsWritten);
            }
        };
        t.start();
        return t;
    }

    public static void readerWriter() {
        LockableQueue lq = new LockableQueue();
        startReader(1, lq);
        int i = 0;
        for (; i < 9; i++) {
            lq.insertItem(new Object());
        }
        lq.insertItem(new Object(), true);
        System.out.println("Writer: wrote " + i + " items.");
    }

    public static void multiReaderWriter() {
        LockableQueue lq = new LockableQueue();
        startReader(1, lq);
        startReader2(2, lq);
        int i = 0;
        for (; i < 19; i++) {
            lq.insertItem(new Object());
        }
        lq.insertItem(new Object(), true);
        System.out.println("Writer: wrote " + i + " items. Closing queue");
    }

    public static void dozenReaderWriter() {
        LockableQueue lq = new LockableQueue();
        for (int i = 0; i < 12; i++) {
            if ((i & 1) != 0) {
                startReader(i, lq);
            } else {
                startReader(i, lq);
            }
        }
        int i = 0;
        for (; i < 399; i++) {
            lq.insertItem(new Object());
        }
        lq.insertItem(new Object(), true);
        System.out.println("Writer: wrote " + i + " items.");
    }

    public static void starvingDozenReaderWriter() {
        LockableQueue lq = new LockableQueue();
        for (int i = 0; i < 12; i++) {
            if ((i & 1) != 0) {
                startReader(i, lq);
            } else {
                startReader2(i, lq);
            }
        }
        int i = 0;
        int j = 0;
        for (; j < 20; j++) {
            for (i = 0; i < 10; i++) {
                lq.insertItem(new Object());
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
            }
        }
        lq.insertItem(new Object(), true);
        System.out.println("Writer: wrote " + ((j * i) + 1) + " items.");
    }

    public static void starvingDozenReaderMultiWriter(boolean fAbandonQueue) {
        LockableQueue lq = new LockableQueue();
        for (int i = 0; i < 12; i++) {
            if ((i & 1) != 0) {
                startReader(i, lq);
            } else {
                startReader2(i, lq);
            }
        }
        Thread w1 = startWriter(1, lq);
        Thread w2 = startWriter(2, lq);
        try {
            w1.join();
            w2.join();
        } catch (InterruptedException ie) {
        }
        System.out.println("Abandoning the Queue");
        lq.close(fAbandonQueue);
    }

    public static void main(String args[]) {
        System.out.println("Starting simple test...");
        simple();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {
        }
        System.out.println("\nStarting reader/writer test...");
        readerWriter();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        System.out.println("\nStarting multi-reader/writer test...");
        multiReaderWriter();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        System.out.println("\nStarting dozen-reader/writer test...");
        dozenReaderWriter();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        System.out.println("\nStarting starving-dozen-reader/writer test...");
        starvingDozenReaderWriter();
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        System.out.println("\nStarting starving-dozen-reader/Multi-writer test (normal close)");
        starvingDozenReaderMultiWriter(false);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }
        System.out.println("\nStarting starving-dozen-reader/Multi-writer test (abandon)");
        starvingDozenReaderMultiWriter(true);
        System.out.println("Exiting the test app");
    }
}
