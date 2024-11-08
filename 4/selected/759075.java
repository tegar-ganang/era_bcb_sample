package org.tuotoo.shared;

import java.io.IOException;

final class IOQueue {

    private byte[] buff;

    private int readPos;

    private int writePos;

    private boolean bWriteClosed;

    private boolean bReadClosed;

    private static final int BUFF_SIZE = 10000;

    private boolean bFull;

    public IOQueue() {
        buff = new byte[BUFF_SIZE];
        readPos = 0;
        writePos = 0;
        bWriteClosed = false;
        bReadClosed = false;
        bFull = false;
    }

    public synchronized void write(byte[] in, int pos, int len) throws IOException {
        int toCopy;
        while (len > 0) {
            if (bReadClosed || bWriteClosed) {
                throw new IOException("IOQueue closed");
            }
            if (bFull) {
                notify();
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new IOException("IOQueue write interrupted");
                }
                continue;
            }
            if (readPos <= writePos) {
                toCopy = BUFF_SIZE - writePos;
            } else {
                toCopy = readPos - writePos;
            }
            if (toCopy > len) {
                toCopy = len;
            }
            System.arraycopy(in, pos, buff, writePos, toCopy);
            pos += toCopy;
            writePos += toCopy;
            len -= toCopy;
            if (writePos >= BUFF_SIZE) {
                writePos = 0;
            }
            if (readPos == writePos) {
                bFull = true;
            }
        }
        notify();
    }

    public synchronized int read() throws IOException {
        while (true) {
            if (bReadClosed) {
                throw new IOException("IOQueue closed");
            }
            if (readPos == writePos && !bFull) {
                if (bWriteClosed) {
                    return -1;
                } else {
                    notify();
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new IOException("IOQueue read() interrupted");
                    }
                    continue;
                }
            }
            int i = buff[readPos++] & 0xFF;
            if (readPos >= BUFF_SIZE) {
                readPos = 0;
            }
            if (bFull) {
                bFull = false;
                notify();
            }
            return i;
        }
    }

    public synchronized int read(byte[] in, int pos, int len) throws IOException {
        if (len <= 0) {
            return 0;
        }
        while (true) {
            if (bReadClosed) {
                throw new IOException("IOQueue closed");
            }
            if (readPos == writePos && !bFull) {
                if (bWriteClosed) {
                    return -1;
                } else {
                    notify();
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new IOException("IOQueue read() interrupted");
                    }
                    continue;
                }
            }
            int toCopy;
            if (writePos <= readPos) {
                toCopy = BUFF_SIZE - readPos;
            } else {
                toCopy = writePos - readPos;
            }
            if (toCopy > len) {
                toCopy = len;
            }
            System.arraycopy(buff, readPos, in, pos, toCopy);
            readPos += toCopy;
            if (readPos >= BUFF_SIZE) {
                readPos = 0;
            }
            if (bFull) {
                bFull = false;
                notify();
            }
            return toCopy;
        }
    }

    public synchronized int available() {
        if (bFull) {
            return BUFF_SIZE;
        }
        if (readPos == writePos && !bFull) {
            return 0;
        }
        if (writePos <= readPos) {
            return BUFF_SIZE - readPos;
        } else {
            return writePos - readPos;
        }
    }

    public synchronized void closeWrite() {
        bWriteClosed = true;
        notify();
    }

    public synchronized void closeRead() {
        bReadClosed = true;
        notify();
    }

    public synchronized void finalize() throws Throwable {
        bReadClosed = true;
        bWriteClosed = true;
        notify();
        buff = null;
        super.finalize();
    }
}
