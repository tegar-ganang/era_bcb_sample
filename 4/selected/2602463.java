package org.yccheok.jstock.engine;

import java.util.concurrent.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author yccheok
 */
public class StockHistoryMonitor extends Subject<StockHistoryMonitor, StockHistoryMonitor.StockHistoryRunnable> {

    /** Creates a new instance of StockHistoryMonitor */
    public StockHistoryMonitor(int nThreads) {
        this(nThreads, 10);
    }

    public StockHistoryMonitor(int nThreads, int databaseSize) {
        pool = Executors.newFixedThreadPool(nThreads);
        readWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
        readerLock = readWriteLock.readLock();
        writerLock = readWriteLock.writeLock();
        this.DATABASE_SIZE = databaseSize;
        this.stockHistorySerializer = null;
    }

    public boolean setStockServerFactories(java.util.List<StockServerFactory> factories) {
        this.factories.clear();
        return this.factories.addAll(factories);
    }

    public boolean addStockCode(final Code code) {
        writerLock.lock();
        if (stockCodes.contains(code)) {
            writerLock.unlock();
            return false;
        }
        boolean status = stockCodes.add(code);
        pool.execute(new StockHistoryRunnable(code));
        writerLock.unlock();
        return status;
    }

    /**
     * @return the duration
     */
    public Duration getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(Duration duration) {
        if (duration == null) {
            throw new IllegalArgumentException("duration cannot be null");
        }
        this.duration = duration;
    }

    public class StockHistoryRunnable implements Runnable {

        public StockHistoryRunnable(Code code) {
            this.code = code;
            this.historyServer = null;
        }

        @Override
        public void run() {
            final Thread currentThread = Thread.currentThread();
            for (StockServerFactory factory : factories) {
                StockHistoryServer history = factory.getStockHistoryServer(this.code, duration);
                if (history != null) {
                    readerLock.lock();
                    if (stockCodes.contains(code)) {
                        this.historyServer = history;
                        boolean shouldUseSerializer = false;
                        if (histories.size() < StockHistoryMonitor.this.DATABASE_SIZE) {
                            synchronized (histories) {
                                if (histories.size() < StockHistoryMonitor.this.DATABASE_SIZE) {
                                    histories.put(code, history);
                                } else {
                                    shouldUseSerializer = true;
                                }
                            }
                        } else {
                            shouldUseSerializer = true;
                        }
                        if (shouldUseSerializer) {
                            if (StockHistoryMonitor.this.stockHistorySerializer != null) {
                                StockHistoryMonitor.this.stockHistorySerializer.save(history);
                            } else {
                                log.error("Fail to perform serialization on stock history due to uninitialized serialization component.");
                            }
                        }
                    }
                    readerLock.unlock();
                    break;
                }
                if (currentThread.isInterrupted()) break;
            }
            if (historyServer == null) {
                writerLock.lock();
                stockCodes.remove(code);
                writerLock.unlock();
            }
            StockHistoryMonitor.this.notify(StockHistoryMonitor.this, this);
        }

        @Override
        public int hashCode() {
            return code.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof StockHistoryRunnable)) {
                return false;
            }
            StockHistoryRunnable stockHistoryRunnable = (StockHistoryRunnable) o;
            return this.code.equals(stockHistoryRunnable.code);
        }

        @Override
        public String toString() {
            return StockHistoryRunnable.class.getName() + "[code=" + code + "]";
        }

        public Code getCode() {
            return code;
        }

        public StockHistoryServer getStockHistoryServer() {
            return historyServer;
        }

        private final Code code;

        private StockHistoryServer historyServer;
    }

    public void clearStockCodes() {
        writerLock.lock();
        final ThreadPoolExecutor threadPoolExecutor = ((ThreadPoolExecutor) pool);
        final int nThreads = threadPoolExecutor.getMaximumPoolSize();
        stockCodes.clear();
        histories.clear();
        threadPoolExecutor.shutdownNow();
        pool = Executors.newFixedThreadPool(nThreads);
        writerLock.unlock();
    }

    public boolean removeStockCode(Code code) {
        writerLock.lock();
        boolean status = stockCodes.remove(code);
        histories.remove(code);
        ((ThreadPoolExecutor) pool).remove(new StockHistoryRunnable(code));
        writerLock.unlock();
        return status;
    }

    public StockHistoryServer getStockHistoryServer(Code code) {
        readerLock.lock();
        if (histories.containsKey(code)) {
            final StockHistoryServer stockHistoryServer = histories.get(code);
            readerLock.unlock();
            return stockHistoryServer;
        } else {
            if (StockHistoryMonitor.this.stockHistorySerializer != null) {
                StockHistoryServer stockHistoryServer = StockHistoryMonitor.this.stockHistorySerializer.load(code);
                if (stockHistoryServer != null && (this.DATABASE_SIZE > histories.size())) {
                    synchronized (histories) {
                        if (stockHistoryServer != null && (this.DATABASE_SIZE > histories.size())) {
                            histories.put(code, stockHistoryServer);
                        }
                    }
                }
                readerLock.unlock();
                return stockHistoryServer;
            } else {
                log.error("Fail to retrieve stock history due to uninitialized serialization component.");
            }
        }
        readerLock.unlock();
        return null;
    }

    public void setStockHistorySerializer(StockHistorySerializer stockHistorySerializer) {
        this.stockHistorySerializer = stockHistorySerializer;
    }

    public void stop() {
        writerLock.lock();
        final ThreadPoolExecutor threadPoolExecutor = ((ThreadPoolExecutor) pool);
        final int nThreads = threadPoolExecutor.getMaximumPoolSize();
        threadPoolExecutor.shutdown();
        threadPoolExecutor.purge();
        pool = Executors.newFixedThreadPool(nThreads);
        writerLock.unlock();
        try {
            threadPoolExecutor.awaitTermination(100, TimeUnit.DAYS);
        } catch (InterruptedException exp) {
            log.error("", exp);
        }
    }

    private final java.util.List<StockServerFactory> factories = new java.util.concurrent.CopyOnWriteArrayList<StockServerFactory>();

    private final java.util.List<Code> stockCodes = new java.util.ArrayList<Code>();

    private final java.util.Map<Code, StockHistoryServer> histories = new java.util.HashMap<Code, StockHistoryServer>();

    private final java.util.concurrent.locks.ReadWriteLock readWriteLock;

    private final java.util.concurrent.locks.Lock readerLock;

    private final java.util.concurrent.locks.Lock writerLock;

    private Executor pool;

    private final int DATABASE_SIZE;

    private StockHistorySerializer stockHistorySerializer;

    private volatile Duration duration = Duration.getTodayDurationByYears(10);

    private static final Log log = LogFactory.getLog(StockHistoryMonitor.class);
}
