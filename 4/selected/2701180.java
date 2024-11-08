package com.kni.etl.ketl.smp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.kni.etl.util.SortedList;

/**
 * The Class ManagedBlockingQueueImpl.
 */
final class ManagedBlockingQueueImpl extends ManagedBlockingQueue {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The reading threads. */
    private int writingThreads = 0, readingThreads = 0;

    /** The name. */
    private String name;

    /**
	 * The Constructor.
	 * 
	 * @param pCapacity
	 *            the capacity
	 */
    public ManagedBlockingQueueImpl(int pCapacity) {
        super(pCapacity);
    }

    @Override
    public void setName(String arg0) {
        this.name = arg0;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name == null ? "NA" : this.name + ",Size: " + this.size() + ", Readers: " + this.readers + ", Writers: " + this.writers;
    }

    private final List<ETLWorker> readers = new ArrayList();

    private final List<ETLWorker> writers = new ArrayList();

    @Override
    public synchronized void registerReader(ETLWorker worker) {
        this.readingThreads++;
        this.readers.add(worker);
    }

    @Override
    public synchronized void registerWriter(ETLWorker worker) {
        this.writingThreads++;
        this.writers.add(worker);
    }

    /** The buffered sort. */
    private List<Object[]> bufferedSort;

    /**
	 * Sets the sort comparator.
	 * 
	 * @param arg0
	 *            the new sort comparator
	 */
    public void setSortComparator(Comparator arg0) {
        this.bufferedSort = new SortedList<Object[]>(arg0);
    }

    @Override
    public void put(Object pO) throws InterruptedException {
        if (pO == com.kni.etl.ketl.smp.ETLWorker.ENDOBJ) {
            synchronized (this) {
                this.writingThreads--;
                if (this.writingThreads == 0) {
                    if (this.bufferedSort != null) {
                        Object[][] batch;
                        ((SortedList) this.bufferedSort).releaseAll();
                        while ((batch = this.bufferedSort.toArray(new Object[((SortedList) this.bufferedSort).fetchSize()][])) != null) super.put(batch);
                    }
                    for (int i = 0; i < this.readingThreads; i++) {
                        super.put(pO);
                    }
                }
            }
            return;
        }
        if (this.bufferedSort != null) {
            Object[][] batch = (Object[][]) pO;
            int size = batch.length;
            for (int i = 0; i < size; i++) this.bufferedSort.add(batch[i]);
            batch = this.bufferedSort.toArray(batch);
            if (batch == null) return;
        }
        super.put(pO);
    }
}
