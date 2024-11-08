package com.kni.etl.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.util.Sort;

/**
 * The Class ExternalSort.
 */
public final class ExternalSort implements Set {

    /**
	 * The Class BufferedObjectStore.
	 */
    public final class BufferedObjectStore {

        /** The store cnt. */
        int storeCnt = 0;

        /** The stores. */
        ArrayList stores = new ArrayList();

        /**
		 * Close.
		 * 
		 * @throws Exception
		 *             the exception
		 */
        public final void close() throws Exception {
            for (int i = 0; i < this.stores.size(); i++) {
                ((Store) this.stores.get(i)).close();
            }
            this.stores.clear();
        }

        /**
		 * Creates the set.
		 * 
		 * @param bufferSize
		 *            the buffer size
		 * 
		 * @return the store
		 * 
		 * @throws IOException
		 *             Signals that an I/O exception has occurred.
		 */
        public final Store createSet(int bufferSize) throws IOException {
            Store store = new Store(bufferSize);
            this.stores.add(store);
            return store;
        }

        @Override
        protected void finalize() throws Throwable {
            if ((this.stores != null) & (this.stores.size() > 0)) {
                this.close();
            }
            super.finalize();
        }
    }

    /** The commit pending. */
    boolean commitPending = true;

    /** The current pos. */
    int currentPos;

    /** The data. */
    public Object[] data;

    /** The db. */
    public BufferedObjectStore db;

    /** The ma stores. */
    Store[] maStores;

    /** The max individual read buffer size. */
    int maxIndividualReadBufferSize;

    /** The max sort size. */
    int maxSortSize = 1000000;

    /** The comparator. */
    Comparator mComparator = null;

    /** The distinct. */
    boolean mDistinct = false;

    /** The merged spools. */
    ArrayList mergedSpools = new ArrayList();

    /** The last exception. */
    Exception mLastException;

    /** The low memory threashold. */
    long mLowMemoryThreashold = -1;

    /** The merge size. */
    int mMergeSize = 128;

    /** The previous. */
    Object mPrevious = null;

    /** The pos. */
    int pos;

    private final int prevSortSpeed[][] = new int[5][2];

    private int sortSize = 5000;

    /** The read buffer size. */
    int readBufferSize;

    /** The records. */
    int records = 0;

    /** The root. */
    SortedList root;

    /** The spool lookup. */
    private final HashMap spoolLookup = new HashMap();

    /** The spools. */
    ArrayList spools = new ArrayList();

    private final long startTime;

    /** The total records. */
    int totalRecords = 0;

    private final int tuneCnt = 0, tuneInterval = 2;

    private final boolean tuning = false;

    private final int tuningRun = 0;

    /** The write buffer size. */
    int writeBufferSize;

    /**
	 * Instantiates a new external sort.
	 * 
	 * @param cmp
	 *            the cmp
	 * @param maxSortSize
	 *            the max sort size
	 * @param mergeSize
	 *            the merge size
	 * @param readBufferSize
	 *            the read buffer size
	 * @param maxIndividualReadBufferSize
	 *            the max individual read buffer size
	 * @param writeBufferSize
	 *            the write buffer size
	 */
    public ExternalSort(Comparator cmp, int maxSortSize, int mergeSize, int readBufferSize, int maxIndividualReadBufferSize, int writeBufferSize) {
        super();
        this.db = new BufferedObjectStore();
        this.maxSortSize = maxSortSize;
        this.mMergeSize = mergeSize;
        this.writeBufferSize = writeBufferSize;
        this.readBufferSize = readBufferSize;
        this.maxIndividualReadBufferSize = maxIndividualReadBufferSize;
        this.mComparator = cmp;
        this.data = new Object[this.maxSortSize];
        this.currentPos = 0;
        this.startTime = System.currentTimeMillis();
    }

    public final boolean add(Object o) {
        this.data[this.currentPos++] = o;
        this.records++;
        if (this.records == this.sortSize) {
            if (this.sortSize == this.maxSortSize || this.isMemoryLow()) {
                this.totalRecords = this.totalRecords + this.records;
                this.records = 0;
                try {
                    this.spool();
                } catch (Exception e) {
                    throw new SortException(e);
                }
            } else {
                this.sortSize = (int) (this.sortSize * 1.5);
                if (this.sortSize > this.maxSortSize) this.sortSize = this.maxSortSize;
            }
        }
        return true;
    }

    public boolean addAll(Collection c) {
        throw new RuntimeException();
    }

    /**
	 * Adds the to sorted list.
	 * 
	 * @param arg0
	 *            the arg0
	 * @param arg1
	 *            the arg1
	 */
    private void addToSortedList(Object arg0, Object arg1) {
        this.spoolLookup.put(arg0, arg1);
        this.root.add(arg0);
    }

    public void clear() {
        throw new RuntimeException();
    }

    /**
	 * Close.
	 * 
	 * @throws Exception
	 *             the exception
	 */
    public final void close() throws Exception {
        this.db.close();
    }

    /**
	 * Commit.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
    public final void commit() throws IOException, ClassNotFoundException {
        if (this.records > 0) {
            this.totalRecords = this.totalRecords + this.records;
            this.records = 0;
        }
        if ((this.spools.size() > 0) || (this.mergedSpools.size() > 0)) {
            if (this.currentPos > 0) {
                this.spool();
            }
            if ((this.spools.size() > this.mMergeSize) || (this.mergedSpools.size() > 0)) {
                boolean merging = true;
                while (merging) {
                    this.merge();
                    if (this.spools.size() == 0) {
                        this.spools = this.mergedSpools;
                        if (this.spools.size() < this.mMergeSize) {
                            merging = false;
                        }
                    }
                }
            }
            this.data = null;
            this.prepSpoolsForMergeSortedList();
            this.spools = null;
        } else {
            this.currentPos = this.totalRecords;
            Sort.quickSort2(this.data, this.mComparator, 0, this.currentPos - 1);
            if (this.mDistinct) {
                this.dedup();
            }
        }
        this.commitPending = false;
    }

    public boolean contains(Object o) {
        return false;
    }

    public boolean containsAll(Collection c) {
        throw new RuntimeException();
    }

    /**
	 * Dedup.
	 */
    final void dedup() {
        Object current = this.data[0];
        ArrayList ar = new ArrayList();
        for (int i = 1; i < this.currentPos; i++) {
            if (this.mComparator.compare(current, this.data[i]) != 0) {
                ar.add(current);
                current = this.data[i];
            }
        }
        ar.add(current);
        if (ar.size() < this.currentPos) {
            ar.toArray(this.data);
            this.currentPos = ar.size();
        }
    }

    /**
	 * Gets the last exception.
	 * 
	 * @return the last exception
	 */
    public final Exception getLastException() {
        return this.mLastException;
    }

    /**
	 * Gets the next.
	 * 
	 * @return the next
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
    public final Object getNext() throws IOException, ClassNotFoundException {
        if (this.commitPending) {
            this.commit();
        }
        if (this.maStores == null) {
            if (this.pos == this.currentPos) {
                return null;
            }
            return this.data[this.pos++];
        }
        if (this.mDistinct == false) {
            return this.getNextFromSpoolsSortedList();
        }
        Object current = this.getNextFromSpoolsSortedList();
        if ((this.mPrevious == null) || (current == null)) {
            this.mPrevious = current;
            return current;
        }
        while (true) {
            if (this.mComparator.compare(current, this.mPrevious) == 0) {
                current = this.getNextFromSpoolsSortedList();
                if (current == null) return null;
            } else {
                this.mPrevious = current;
                return current;
            }
        }
    }

    /**
	 * Gets the next from spools sorted list.
	 * 
	 * @return the next from spools sorted list
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
    final Object getNextFromSpoolsSortedList() throws IOException, ClassNotFoundException {
        Object first = this.root.removeFirst();
        if (first == null) {
            return null;
        }
        Integer pos = (Integer) this.spoolLookup.remove(first);
        Store spool = this.maStores[pos];
        if (spool.hasNext()) {
            Object o = spool.next();
            this.addToSortedList(o, pos);
        } else {
            spool.close();
        }
        return first;
    }

    public final boolean isEmpty() {
        return (this.records == 0) ? true : false;
    }

    protected final boolean isMemoryLow() {
        Runtime r = Runtime.getRuntime();
        long free = (r.maxMemory() - (r.totalMemory() - r.freeMemory()));
        if (free < (1048576 * 4)) return true;
        return false;
    }

    public Iterator iterator() {
        throw new RuntimeException();
    }

    /**
	 * Merge.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
    final void merge() throws IOException, ClassNotFoundException {
        if (this.spools.size() == 1) {
            this.mergedSpools.add(this.spools.get(0));
            this.spools.remove(0);
            return;
        }
        this.prepSpoolsForMergeSortedList();
        Store store = this.db.createSet(this.writeBufferSize);
        Object o = null;
        while (!((o = this.getNextFromSpoolsSortedList()) == null)) {
            store.add(o);
        }
        store.commit();
        this.mergedSpools.add(store);
        this.currentPos = 0;
    }

    /**
	 * Prep spools for merge sorted list.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
    private final void prepSpoolsForMergeSortedList() throws IOException, ClassNotFoundException {
        int nSpools = (this.mMergeSize < this.spools.size()) ? this.mMergeSize : this.spools.size();
        int iReadBufferSize = this.readBufferSize / nSpools;
        int objectBufferSize = this.maxSortSize / nSpools;
        if (iReadBufferSize > this.maxIndividualReadBufferSize) {
            iReadBufferSize = this.maxIndividualReadBufferSize;
        }
        this.maStores = new Store[nSpools];
        for (int i = 0; i < nSpools; i++) {
            this.maStores[i] = (Store) this.spools.get(i);
            this.maStores[i].start(iReadBufferSize, objectBufferSize);
        }
        this.root = null;
        for (int i = 0; i < nSpools; i++) {
            this.spools.remove(this.maStores[i]);
            if (this.maStores[i].hasNext()) {
                if (this.root == null) {
                    this.root = new SortedList(this.mComparator);
                }
                Object o = this.maStores[i].next();
                this.addToSortedList(o, i);
            }
        }
    }

    public boolean remove(Object o) {
        throw new RuntimeException();
    }

    public boolean removeAll(Collection c) {
        throw new RuntimeException();
    }

    public boolean retainAll(Collection c) {
        throw new RuntimeException();
    }

    public final int size() {
        return this.records;
    }

    /**
	 * Spool.
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws ClassNotFoundException
	 *             the class not found exception
	 */
    final void spool() throws IOException, ClassNotFoundException {
        Sort.quickSort2(this.data, this.mComparator, 0, this.currentPos - 1);
        if (this.mDistinct) {
            this.dedup();
        }
        Store store = this.db.createSet(this.writeBufferSize);
        store.add(this.data, this.currentPos);
        store.commit();
        this.spools.add(store);
        if (this.spools.size() >= this.mMergeSize) {
            while (this.spools.size() > 0) {
                ResourcePool.LogMessage(this, ResourcePool.INFO_MESSAGE, "Merging");
                this.merge();
            }
        }
        this.currentPos = 0;
    }

    public Object[] toArray() {
        throw new RuntimeException();
    }

    public Object[] toArray(Object[] a) {
        throw new RuntimeException();
    }

    public long sortRate() {
        return (this.totalRecords / (System.currentTimeMillis() - this.startTime)) * 1000;
    }
}
