package maze.commons.shared.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import maze.commons.shared.base.BasicBase;

/**
 * @author Normunds Mazurs (MAZE)
 * 
 */
public class ReadWriteLockBase extends BasicBase {

    protected ReadWriteLock createReadWriteLock() {
        return new ReentrantReadWriteLock(true);
    }

    protected final ReadWriteLock readWriteLock;

    protected final Lock readLock;

    protected final Lock writeLock;

    protected ReadWriteLockBase() {
        readWriteLock = createReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
    }
}
