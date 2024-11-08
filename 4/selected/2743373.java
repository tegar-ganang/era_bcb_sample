package com.townsfolkdesigns.lucene.jedit.manager;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IndexManager {

    private static final IndexManager instance = new IndexManager();

    private ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private IndexManager() {
    }

    public static IndexManager getInstance() {
        return instance;
    }

    public void aquireReadLock() {
        readWriteLock.readLock().lock();
    }

    public void aquireWriteLock() {
        readWriteLock.writeLock().lock();
    }

    public void releaseReadLock() {
        readWriteLock.readLock().unlock();
    }

    public void releaseWriteLock() {
        readWriteLock.writeLock().unlock();
    }
}
