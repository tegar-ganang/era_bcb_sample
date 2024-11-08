package org.creativor.rayson.transport.common;

/**
 * The Class PacketCounter.
 */
public class PacketCounter {

    private volatile long readCount = 0;

    private volatile long writeCount = 0;

    /**
	 * Instantiates a new packet counter.
	 */
    public PacketCounter() {
    }

    /**
	 * Read count.
	 * 
	 * @return the long
	 */
    public long readCount() {
        return readCount;
    }

    /**
	 * Read one.
	 */
    public synchronized void readOne() {
        readCount++;
    }

    /**
	 * Pending write packet count.
	 */
    public synchronized long writePendingCount() {
        return writeCount - readCount;
    }

    /**
	 * Pending read packet count.
	 */
    public synchronized long readPendingCount() {
        return readCount - writeCount;
    }

    /**
	 * To string.
	 * 
	 * @return the string
	 */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{");
        sb.append("read: ");
        sb.append(readCount);
        sb.append(", write: ");
        sb.append(writeCount);
        sb.append("}");
        return sb.toString();
    }

    /**
	 * Write count.
	 * 
	 * @return the long
	 */
    public long writeCount() {
        return writeCount;
    }

    /**
	 * Write one.
	 */
    public synchronized void writeOne() {
        writeCount++;
    }
}
