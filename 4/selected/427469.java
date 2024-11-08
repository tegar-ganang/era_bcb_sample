package org.jage.util;

/**
 * <tt>ReaderWriterLock</tt> solves the classic problem of readers and
 * writers. <br>
 * It prevents starvation of readers and writers.
 * <p>
 * Assumptions: <br>
 * <li>If there are any waiting writers new readers have to wait (at least)
 * until the first of them finishes writing
 * <li>If there are any waiting readers they will be resumed before the next
 * writing
 * 
 * @author KrzS
 * @author Pawel Kedzior (preventing deadlocks)  
 */
public class ReaderWriterLock {

    /**
	 * Number of readers which have set the lock.
	 */
    private int _readerCount = 0;

    /**
	 * Indicates that writer lock have been set.
	 */
    private boolean _writerLock = false;

    /**
	 * Number of readers waiting.
	 */
    private int _waitingReaders = 0;

    /**
	 * Number of writers waiting.
	 */
    private int _waitingWriters = 0;

    /**
	 * Number of readers to be woken.
	 */
    private int _wakeUpReaders = 0;

    /**
	 * Number of writers to be woken.
	 * Actually only zero and 1 is allowed.
	 */
    private int _wakeUpWriters = 0;

    /**
	 * Common monitor for all kinds of events.
	 * _monitor.notifyAll() must be called whenever an event
	 * occurs.
	 */
    private final Object _monitor = new Object();

    /**
	 * Constructor.
	 */
    public ReaderWriterLock() {
    }

    /**
	 * Waits for waking up all readers or writers, which should
	 * be woken. In order to call this, _monitor must be
	 * synchronized.
	 * 
	 * @throws InterruptedException
	 */
    private void waitForWakingUp() throws InterruptedException {
        while (_wakeUpWriters != 0 || _wakeUpReaders != 0) {
            _monitor.wait();
        }
    }

    /**
	 * Acquires reader's lock.
	 * 
	 * @throws InterruptedException
	 */
    public void acquireReaderLock() throws InterruptedException {
        synchronized (_monitor) {
            waitForWakingUp();
            if (_writerLock || _waitingWriters > 0) {
                _waitingReaders++;
                try {
                    do {
                        _monitor.wait();
                        if (_wakeUpReaders > 0) {
                            _wakeUpReaders--;
                            _monitor.notifyAll();
                            break;
                        }
                    } while (true);
                } finally {
                    _waitingReaders--;
                }
            }
            _readerCount++;
        }
    }

    /**
	 * Releases reader's lock.
	 * @throws InterruptedException 
	 */
    public void releaseReaderLock() throws InterruptedException {
        synchronized (_monitor) {
            waitForWakingUp();
            _readerCount--;
            if (_readerCount == 0) {
                if (_waitingWriters >= 1) {
                    _wakeUpWriters = 1;
                    _monitor.notifyAll();
                }
            }
        }
    }

    /**
	 * Acquires writer's lock.
	 * 
	 * @throws InterruptedException
	 */
    public void acquireWriterLock() throws InterruptedException {
        synchronized (_monitor) {
            waitForWakingUp();
            if (_readerCount > 0 || _writerLock) {
                _waitingWriters++;
                try {
                    do {
                        _monitor.wait();
                        if (_wakeUpWriters == 1) {
                            _wakeUpWriters = 0;
                            _monitor.notifyAll();
                            break;
                        }
                    } while (true);
                } finally {
                    _waitingWriters--;
                }
            }
            _writerLock = true;
        }
    }

    /**
	 * Releases the writer's lock.
	 * @throws InterruptedException 
	 */
    public void releaseWriterLock() throws InterruptedException {
        synchronized (_monitor) {
            waitForWakingUp();
            _writerLock = false;
            if (_waitingReaders > 0) {
                _wakeUpReaders = _waitingReaders;
                _monitor.notifyAll();
            } else {
                if (_waitingWriters >= 1) {
                    _wakeUpWriters = 1;
                    _monitor.notifyAll();
                }
            }
        }
    }
}
