package net.sf.orcc.runtime;

import java.io.IOException;

/**
 * This class defines a FIFO of integers.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class Fifo_int extends Fifo {

    /**
	 * The contents of the FIFO.
	 */
    private int[] contents;

    /**
	 * Creates a new FIFO with the given size.
	 * 
	 * @param size
	 *            the size of this FIFO
	 */
    public Fifo_int(int size) {
        super(size);
        contents = new int[size];
    }

    /**
	 * Creates a new FIFO with the given size and a file for tracing exchanged
	 * data.
	 * 
	 * @param size
	 *            the size of the FIFO
	 * @param folderName
	 *            output traces folder
	 * @param fifoName
	 *            name of the fifo (and the trace file)
	 */
    public Fifo_int(int size, String folderName, String fifoName, boolean enableTraces) {
        super(size, folderName, fifoName, enableTraces);
        contents = new int[size];
    }

    /**
	 * Returns the array where <code>numTokens</code> can be read.
	 * 
	 * @param numTokens
	 *            a number of tokens to read
	 * @return the array where <code>numTokens</code> can be read
	 */
    public final int[] getReadArray(int numTokens) {
        if (read + numTokens <= size) {
            return contents;
        } else {
            int[] buffer = new int[numTokens];
            int numEnd = size - read;
            int numBeginning = numTokens - numEnd;
            if (numEnd != 0) {
                System.arraycopy(contents, read, buffer, 0, numEnd);
            }
            if (numBeginning != 0) {
                System.arraycopy(contents, 0, buffer, numEnd, numBeginning);
            }
            return buffer;
        }
    }

    /**
	 * Returns the array where <code>numTokens</code> can be written.
	 * 
	 * @param numTokens
	 *            a number of tokens to write
	 * @return the array where <code>numTokens</code> can be written
	 */
    public final int[] getWriteArray(int numTokens) {
        if (write + numTokens <= size) {
            return contents;
        } else {
            return new int[numTokens];
        }
    }

    public String toString() {
        return write + "/" + read;
    }

    /**
	 * Signals that writing is finished.
	 * 
	 * @param numTokens
	 *            the number of tokens that were written
	 */
    public final void writeEnd(int numTokens, int[] buffer) {
        fillCount += numTokens;
        if (write + numTokens <= size) {
            write += numTokens;
        } else {
            int numEnd = size - write;
            int numBeginning = numTokens - numEnd;
            if (numEnd != 0) {
                System.arraycopy(buffer, 0, contents, write, numEnd);
            }
            if (numBeginning != 0) {
                System.arraycopy(buffer, numEnd, contents, 0, numBeginning);
            }
            write = numBeginning;
        }
        if (out != null) {
            try {
                for (int i = 0; i < buffer.length; i++) {
                    out.write(buffer[i] + "\n");
                }
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
