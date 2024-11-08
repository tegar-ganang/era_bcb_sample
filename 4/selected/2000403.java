package com.golden.gamedev.engine.network.tcp;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * @author Kevin
 */
public class PipedInputStream extends InputStream {

    private int[] data;

    private int write;

    private int read;

    private int available;

    public PipedInputStream(PipedOutputStream pout, int size) throws IOException {
        this.data = new int[size];
        pout.connect(this);
    }

    public synchronized int available() throws IOException {
        return this.available;
    }

    public void close() throws IOException {
    }

    public synchronized int read() throws IOException {
        if (this.available <= 0) {
            return -1;
        }
        if (this.read == this.write) {
            return -1;
        }
        this.available--;
        int value = this.data[this.read];
        this.read++;
        if (this.read >= this.data.length) {
            this.read = 0;
        }
        return value;
    }

    public synchronized int read(byte[] ret, int off, int len) throws IOException {
        if (this.available <= 0) {
            return -1;
        }
        int count = 0;
        for (int i = off; i < off + len; i++) {
            int b = this.read();
            if (b == -1) {
                break;
            }
            count++;
            ret[i] = (byte) b;
        }
        return count;
    }

    protected synchronized void receive(int b) throws IOException {
        if (b < 0) {
            b = 256 + b;
        }
        this.data[this.write] = b;
        this.write++;
        if (this.write >= this.data.length) {
            this.write = 0;
        }
        if (this.write == this.read) {
            throw new IOException("Buffer overflow in NewPipedInputStream");
        }
        this.available++;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }
}
