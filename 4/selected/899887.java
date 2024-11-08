package org.ezaero.sandbox.conflation;

import static java.lang.String.format;
import gnu.trove.TIntArrayList;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.ezaero.sandbox.conflation.store.PlainMapPriceStore;
import org.ezaero.sandbox.conflation.store.PriceStore;
import org.ezaero.sandbox.conflation.store.RWLockLongMapAltPriceStore;
import org.ezaero.sandbox.conflation.store.RWLockLongMapPriceStore;
import org.ezaero.sandbox.conflation.store.RWLockMapAltPriceStore;
import org.ezaero.sandbox.conflation.store.RWLockMapPriceStore;
import org.ezaero.sandbox.conflation.store.SyncLongMapAltPriceStore;
import org.ezaero.sandbox.conflation.store.SyncLongMapPriceStore;
import org.ezaero.sandbox.conflation.store.SyncMapAltPriceStore;
import org.ezaero.sandbox.conflation.store.SyncMapPriceStore;

public class ConflationPerfTool {

    private static final void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    private static final Random RAND = new Random(0xDEADBEEFCAFEBABEL);

    private static final int COUNT = 1000000;

    private static final int BURST_COUNT = 4;

    private static final int BURST_SIZE = (COUNT / BURST_COUNT);

    private static final int[] ordered = new int[COUNT];

    private static final int[] shuffled;

    private static final int[] random;

    static {
        System.out.println("Preparing test data...");
        final TIntArrayList s = new TIntArrayList(COUNT);
        final TIntArrayList r = new TIntArrayList(COUNT);
        for (int i = 0; i < COUNT; i++) {
            s.add(i);
            r.add(Math.abs(RAND.nextInt(COUNT)));
            ordered[i] = i;
        }
        s.shuffle(RAND);
        shuffled = s.toNativeArray();
        random = r.toNativeArray();
        memCheckpoint("Setup");
        System.out.println("Done");
    }

    private static enum AccessMode {

        ORDERED(ordered), SHUFFLED(shuffled), RANDOM(random);

        public final int[] ids;

        private AccessMode(int[] ids) {
            this.ids = ids;
        }
    }

    ;

    private static enum StoreMode {

        PLAIN(PlainMapPriceStore.class), SYNC_MAP(SyncMapPriceStore.class), RWLOCK_MAP(RWLockMapPriceStore.class), SYNC_MAP_ALT(SyncMapAltPriceStore.class), RWLOCK_MAP_ALT(RWLockMapAltPriceStore.class), SYNC_LONGMAP(SyncLongMapPriceStore.class), RWLOCK_LONGMAP(RWLockLongMapPriceStore.class), SYNC_LONGMAP_ALT(SyncLongMapAltPriceStore.class), RWLOCK_LONGMAP_ALT(RWLockLongMapAltPriceStore.class);

        private final Class<?> storeClass;

        private StoreMode(Class<?> storeClass) {
            this.storeClass = storeClass;
        }

        public PriceStore create(int size) {
            try {
                final Constructor<?> ctor = storeClass.getConstructor(int.class);
                return (PriceStore) ctor.newInstance(size);
            } catch (Throwable e) {
                throw new IllegalStateException();
            }
        }
    }

    private static void memCheckpoint(String msg) {
        System.gc();
        System.gc();
        System.gc();
        final long free = Runtime.getRuntime().freeMemory() / 1024;
        final long total = Runtime.getRuntime().totalMemory() / 1024;
        final long used = (total - free);
        System.out.println(format("Heap: total=%dK, free=%dK, used=%dK - %s", total, free, used, msg));
    }

    public static void main(String[] args) {
        System.out.println("Waiting 20s to start...");
        sleep(20 * 1000);
        final ExecutorService executor = Executors.newCachedThreadPool();
        final List<Setup> setups = new ArrayList<Setup>();
        for (AccessMode accessMode : AccessMode.values()) {
            for (StoreMode storeMode : StoreMode.values()) {
                setups.add(new Setup(64, accessMode, storeMode, 10, 3, 10, 100, 5, 5));
            }
        }
        for (Setup setup : setups) {
            final PriceStore store = setup.storeMode.create(setup.initSize);
            final CountDownLatch latch = new CountDownLatch(setup.readerCount + setup.writerCount);
            new Writer(store, 0, ordered, null, 1).run();
            memCheckpoint("Pre test");
            System.out.println("\nStarted Test:");
            System.out.println("    count=" + COUNT);
            System.out.println("    burstCount=" + BURST_COUNT);
            System.out.println("    burstSize=" + BURST_SIZE);
            System.out.println("    initSize=" + setup.initSize);
            System.out.println("    storeMode=" + setup.storeMode);
            System.out.println("    accessMode=" + setup.accessMode);
            System.out.println("    writerCount=" + setup.writerCount);
            System.out.println("    writerLoop=" + setup.writerLoop);
            System.out.println("    writerBurstLagMillis=" + setup.writerBurstLag);
            System.out.println("    readerCount=" + setup.readerCount);
            System.out.println("    readerLoop=" + setup.readerLoop);
            System.out.println("    readerBurstLagMillis=" + setup.readerBurstLag);
            final long timestamp = System.currentTimeMillis();
            for (int i = 0; i < setup.writerCount; i++) {
                executor.execute(new Writer(store, setup.writerBurstLag, setup.accessMode.ids, latch, setup.writerLoop));
            }
            for (int i = 0; i < setup.readerCount; i++) {
                executor.execute(new Reader(store, setup.readerBurstLag, setup.accessMode.ids, latch, setup.readerLoop));
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
            }
            final long duration = (System.currentTimeMillis() - timestamp);
            System.out.println(String.format("Test Complete. Duration=%.2fs\n", (duration / 1000.0)));
            memCheckpoint("Post test");
            System.out.println("Waiting 10s...");
            sleep(10 * 1000);
        }
        System.exit(0);
    }

    private static class Setup {

        final int writerCount;

        final int writerLoop;

        final int writerBurstLag;

        final int readerCount;

        final int readerLoop;

        final int readerBurstLag;

        final AccessMode accessMode;

        final StoreMode storeMode;

        final int initSize;

        private Setup(int initSize, AccessMode mode, StoreMode store, int writerCount, int writerLoop, int writerBurstLag, int readerCount, int readerLoop, int readerBurstLag) {
            this.initSize = initSize;
            this.accessMode = mode;
            this.storeMode = store;
            this.writerCount = writerCount;
            this.writerLoop = writerLoop;
            this.writerBurstLag = writerBurstLag;
            this.readerCount = readerCount;
            this.readerLoop = readerLoop;
            this.readerBurstLag = readerBurstLag;
        }
    }

    private static class Writer implements Runnable {

        private final PriceStore store;

        private final long lag;

        private final int[] ids;

        private final CountDownLatch latch;

        private final int loop;

        public Writer(PriceStore store, long lag, int[] ids, CountDownLatch latch, int loop) {
            this.store = store;
            this.lag = lag;
            this.ids = ids;
            this.latch = latch;
            this.loop = loop;
        }

        public void run() {
            for (int l = 0; l < loop; l++) {
                for (int i = 0; i < BURST_COUNT; i++) {
                    for (int j = 0; j < BURST_SIZE; j++) {
                        final int idx = (i * BURST_SIZE) + j;
                        store.update(ids[idx], idx, idx, idx, idx, idx, idx);
                    }
                    if (lag != 0) sleep(lag);
                }
            }
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    private static class Reader implements Runnable {

        private final PriceStore store;

        private final long lag;

        private final int[] ids;

        private final CountDownLatch latch;

        private final int loop;

        public Reader(PriceStore store, long lag, int[] ids, CountDownLatch latch, int loop) {
            this.store = store;
            this.lag = lag;
            this.ids = ids;
            this.latch = latch;
            this.loop = loop;
        }

        public void run() {
            for (int l = 0; l < loop; l++) {
                for (int i = 0; i < BURST_COUNT; i++) {
                    for (int j = 0; j < BURST_SIZE; j++) {
                        final int idx = (i * BURST_SIZE) + j;
                        final Price price = store.get(ids[idx]);
                        if (price == null) {
                            System.err.println("Failed to get price for" + ids[i]);
                            continue;
                        }
                        final long id = price.getId();
                        final double ask = price.getAsk();
                        final double bid = price.getBid();
                        final double last = price.getLast();
                        final double mid = (ask + bid) / 2.0;
                    }
                    if (lag != 0) sleep(lag);
                }
            }
            latch.countDown();
        }
    }
}
