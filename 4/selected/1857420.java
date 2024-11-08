package org.mobicents.media.server.component;

import org.mobicents.media.server.spi.memory.Frame;

/**
 * Elastic buffer is asynchronous FIFO buffer wich automatically compensates
 * for drift between receiver clock and transmitter clock signals.
 *
 * @author kulikov
 */
public class ElasticBuffer {

    /** the initial delay of signal measured in number of frames */
    private int delay;

    /** The limit of the buffer in frames */
    private int limit;

    /** backing array */
    private Frame[] buffer;

    /** reader indexes */
    private int rs, ro;

    /** writer indexes */
    private int ws, wo = -1;

    /** frame for reading */
    private Frame frame;

    /** buffer state */
    private boolean isReady;

    /** buffer state monitor */
    private BufferListener listener;

    /**
     * Creates new elastic buffer.
     *
     * @param delay the initial delay of signal measured in frames
     * @param limit the physical limit of the buffer measured in frames.
     */
    public ElasticBuffer(int delay, int limit) {
        this.delay = delay;
        this.limit = limit;
        this.buffer = new Frame[limit];
        this.isReady = false;
    }

    /**
     * Sets buffer state monitor.
     * 
     * @param listener the monitor implementation.
     */
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }

    /**
     * Writes given frame to the tail of the buffer.
     *
     * @param frame the frame to write
     */
    public synchronized void write(Frame frame) {
        this.updateWrite();
        if (this.wo == this.ro && this.isReady) {
            this.updateRead();
        }
        buffer[wo] = frame;
        if (!this.isReady && (this.writeIndex() - this.readIndex()) >= delay) {
            this.isReady = true;
            if (listener != null) listener.onReady();
        }
    }

    /**
     * Retreives and removes frame from the head of the buffer.
     *
     * @return the frame or null if buffer is empty.
     */
    public synchronized Frame read() {
        if (!this.isReady) {
            return null;
        }
        frame = buffer[ro];
        buffer[ro] = null;
        this.updateRead();
        if (this.readIndex() - 1 == this.writeIndex()) {
            this.isReady = false;
        }
        return frame;
    }

    /**
     * Gets the absolute index of reader position buffer.
     *
     * @return the index value
     */
    private long readIndex() {
        return rs * limit + ro;
    }

    /**
     * Gets the absolute index of writer position in buffer.
     *
     * @return the index value
     */
    private long writeIndex() {
        return ws * limit + wo;
    }

    /**
     * Modify write position on one step
     */
    private void updateWrite() {
        wo++;
        if (wo == limit) {
            wo = 0;
            ws++;
        }
    }

    /**
     * Modify reader position on one step.
     */
    private void updateRead() {
        ro++;
        if (ro == limit) {
            ro = 0;
            rs++;
        }
    }

    public void clear() {
        ro = 0;
        rs = 0;
        wo = 0;
        ws = 0;
        this.isReady = false;
    }

    public boolean isEmpty() {
        return !this.isReady;
    }
}
