package net.sf.fileexchange.util.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BoundaryInputStream extends InputStream {

    private final InputStream inputStream;

    private final int[] boundary;

    /**
	 * 
	 * @param inputStream
	 *            must support mark.
	 */
    public BoundaryInputStream(InputStream inputStream, byte[] boundary) throws IOException {
        if (!inputStream.markSupported()) throw new IllegalArgumentException("InputStream must support mark");
        this.inputStream = inputStream;
        this.boundary = new int[boundary.length];
        for (int i = 0; i < boundary.length; i++) {
            if (boundary[i] >= 0) this.boundary[i] = boundary[i]; else this.boundary[i] = 256 + boundary[i];
            assert this.boundary[i] >= 0;
            assert this.boundary[i] <= 255;
        }
    }

    /**
	 * Checks
	 * 
	 * @return true, if the next bytes equal the specified boundary and false
	 *         otherwise.
	 * @throws IllegalStateException
	 *             if an read call has failed with an Exception before.
	 */
    public boolean isAtBoundary() throws IOException, IllegalStateException {
        inputStream.mark(boundary.length);
        boolean result = true;
        for (int index = 0; index < boundary.length; index++) {
            final int readed = inputStream.read();
            if (boundary[index] != readed) {
                result = false;
                break;
            }
        }
        inputStream.reset();
        return result;
    }

    @Override
    public int read() throws IOException {
        if (isAtBoundary()) return -1;
        return inputStream.read();
    }

    /**
	 * Skips bytes until after the next boundary. Note that it is still possible
	 * to use the {@link #reset} method to get before the boundary again, if
	 * {@link #mark(int)} got called before this method.
	 * 
	 * @return the number of bytes skipped. This amount includes the number of
	 *         bytes before and in the boundary.
	 * @throws NoBoundaryException
	 *             if all bytes have been skipped, but there was no boundary.
	 * 
	 */
    public int skipUntilAfterNextBoundary() throws IOException, NoBoundaryException {
        int counter = 0;
        while (read() != -1) {
            counter++;
        }
        if (!isAtBoundary()) throw new NoBoundaryException();
        long boundaryBytesToSkip = boundary.length;
        do {
            long boundaryBytesSkipped = inputStream.skip(boundaryBytesToSkip);
            if (boundaryBytesSkipped < 1) {
                throw new IOException("Unable to skip over boundary");
            }
            boundaryBytesToSkip -= boundaryBytesSkipped;
        } while (boundaryBytesToSkip > 0);
        counter += boundary.length;
        return counter;
    }

    private ByteArrayOutputStream outputToBoundary() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int readedByte = read();
        while (readedByte != -1) {
            output.write(readedByte);
            readedByte = read();
        }
        return output;
    }

    public byte[] bytesToEndOrBoundary() throws IOException {
        return outputToBoundary().toByteArray();
    }

    public String stringToEndOrBoundary(String charsetName) throws IOException {
        return outputToBoundary().toString(charsetName);
    }

    public static class NoBoundaryException extends IOException {

        private static final long serialVersionUID = 2802005478227894619L;
    }

    /**
	 * The {@link InputStream#mark(int)} and {@link InputStream#reset()} methods
	 * are not supported by this inputStream. This method will always return
	 * false.
	 * 
	 */
    @Override
    public boolean markSupported() {
        return false;
    }
}
