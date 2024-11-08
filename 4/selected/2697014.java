package net.sf.ruleminer.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Copies data from input stream to output stream.
 * 
 * @author Michal Burda
 */
public class Copier {

    /**
	 * The size of the buffer used for copying.
	 */
    private int bufferSize;

    /**
	 * @param bufferSize The size of the buffer used for copying
	 */
    public Copier(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
	 * @param input
	 * @param output
	 * @throws IOException
	 */
    public void copy(InputStream input, OutputStream output) throws IOException {
        ContractChecker.mustNotBeNull(input, "input");
        ContractChecker.mustNotBeNull(output, "output");
        byte[] buffer = new byte[this.bufferSize];
        int read = -1;
        try {
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        } finally {
            try {
                input.close();
            } finally {
                output.close();
            }
        }
    }

    /**
	 * @param input
	 * @param output
	 * @throws IOException
	 */
    public void copy(Reader input, Writer output) throws IOException {
        ContractChecker.mustNotBeNull(input, "input");
        ContractChecker.mustNotBeNull(output, "output");
        char[] buffer = new char[this.bufferSize];
        int read = -1;
        try {
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
        } finally {
            try {
                input.close();
            } finally {
                output.close();
            }
        }
    }
}
