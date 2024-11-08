package net.sf.orcc.oj;

/**
 * A FIFO of integers.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class IntFifo {

    private int[] contents;

    private int read;

    private int size;

    private int write;

    public IntFifo(int size) {
        this.size = size;
        contents = new int[size];
    }

    public void get(boolean[] target) {
        peek(target);
        read += target.length;
    }

    public void get(int[] target) {
        peek(target);
        read += target.length;
    }

    public boolean hasRoom(int n) {
        if ((size - write) >= n) {
            return true;
        } else {
            FifoManager.getInstance().addFullFifo(this);
            return false;
        }
    }

    public boolean hasTokens(int n) {
        if ((write - read) >= n) {
            return true;
        } else {
            FifoManager.getInstance().addEmptyFifo(this);
            return false;
        }
    }

    void moveTokens() {
        int n = write - read;
        if (read > n) {
            if (n > 0) {
                System.arraycopy(contents, read, contents, 0, n);
            }
            read = 0;
            write = n;
        }
    }

    public void peek(boolean[] target) {
        int n = target.length;
        for (int i = 0; i < n; i++) {
            target[i] = (contents[read + i] != 0);
        }
    }

    public void peek(int[] target) {
        int n = target.length;
        System.arraycopy(contents, read, target, 0, n);
    }

    public void put(boolean[] source) {
        int n = source.length;
        for (int i = 0; i < n; i++) {
            contents[write + i] = source[i] ? 1 : 0;
        }
        write += n;
    }

    public void put(int[] source) {
        int n = source.length;
        System.arraycopy(source, 0, contents, write, n);
        write += n;
    }

    public String toString() {
        return write + "/" + read;
    }
}
