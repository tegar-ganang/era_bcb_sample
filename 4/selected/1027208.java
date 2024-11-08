package net.sourceforge.javautil.common.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import net.sourceforge.javautil.common.ThreadUtil;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;

/**
 * A registry that will always be reading from an input source (using a thread)
 * and provide access to the input during a dynamic period of time for different
 * targets inputs.<br/><br/>
 * 
 * This will also act as an input stream and can be used as a wrapper as well.
 * 
 * @author elponderador
 * @author $Author$
 * @version $Id$
 */
public class InputStreamRegistry extends InputStream implements IInputStreamStatus {

    protected final String name;

    protected final InputStream input;

    protected ProcessInput processor;

    protected boolean running = false;

    protected int bufferSize;

    protected ChainedBuffer buffer;

    protected Marker marker;

    public InputStreamRegistry(String name, InputStream input) {
        this(name, input, 1024);
    }

    public InputStreamRegistry(String name, InputStream input, int bufferSize) {
        this.input = input;
        this.name = name;
        this.bufferSize = bufferSize;
    }

    public boolean isOpen() {
        return this.running;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        this.ensureOpen();
        if (buffer == null) {
            return -1;
        }
        MarkerOperation op = marker.read(b, 0, len);
        if (op == null) {
            return -1;
        }
        this.marker = op.marker;
        return op.read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    @Override
    public int read() throws IOException {
        this.ensureOpen();
        byte[] single = new byte[1];
        if (buffer == null) {
            return -1;
        }
        MarkerOperation op = marker.read(single, 0, 1);
        if (op == null) {
            return -1;
        }
        this.marker = op.marker;
        return single[0];
    }

    @Override
    public int available() throws IOException {
        return marker.getAvailable();
    }

    @Override
    public void close() throws IOException {
    }

    private void ensureOpen() throws IOException {
        if (!this.isOpen()) throw new IOException("Input stream has already been closed");
    }

    /**
	 * @return The current buffer size for the chained buffer
	 */
    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
	 * @return A new input stream for reading from this registry
	 */
    public InputStream getNewInputStream() {
        if (!this.running) throw new IllegalStateException("This registry has not yet been started");
        return new RegistryListener();
    }

    /**
	 * Start this registry
	 */
    public void start() {
        if (this.running) throw new IllegalStateException("This registry has already been started");
        this.running = true;
        this.processor = new ProcessInput(input);
        this.buffer = new ChainedBuffer();
        this.marker = this.buffer.getCurrentMarker(this);
        Thread thread = new Thread(processor, "Input Processor [" + name + "]");
        thread.setDaemon(true);
        thread.start();
    }

    /**
	 * Stop this registry and cause call generated input streams to close.
	 */
    public void stop() {
        if (!this.running) throw new IllegalStateException("This registry has not been started");
        this.running = false;
        this.buffer = null;
    }

    /**
	 * This is an input stream
	 * 
	 * @author elponderador
	 * @author $Author$
	 * @version $Id$
	 */
    private class RegistryListener extends InputStream implements IInputStreamStatus {

        protected byte[] single = new byte[1];

        protected boolean open = true;

        protected Marker marker = buffer.getCurrentMarker(this);

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            this.ensureOpen();
            if (buffer == null) {
                open = false;
                return -1;
            }
            MarkerOperation op = marker.read(b, 0, len);
            if (op == null) {
                open = false;
                return -1;
            }
            this.marker = op.marker;
            return op.read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            return this.read(b, 0, b.length);
        }

        @Override
        public int read() throws IOException {
            this.ensureOpen();
            if (buffer == null) {
                open = false;
                return -1;
            }
            MarkerOperation op = marker.read(single, 0, 1);
            if (op == null) {
                open = false;
                return -1;
            }
            this.marker = op.marker;
            return single[0];
        }

        public boolean isOpen() {
            return this.open;
        }

        @Override
        public int available() throws IOException {
            return marker.getAvailable();
        }

        @Override
        public void close() throws IOException {
            this.ensureOpen();
            this.open = false;
        }

        private void ensureOpen() throws IOException {
            if (!this.open) throw new IOException("Input stream has already been closed");
        }
    }

    /**
	 * This will allow listening input streams to hold on to input they have
	 * not read yet while others may advance.
	 * 
	 * @author elponderador
	 * @author $Author$
	 * @version $Id$
	 */
    private class ChainedBuffer {

        protected int pointer = 0;

        protected byte[] data = new byte[bufferSize];

        protected ChainedBuffer next;

        public Marker getCurrentMarker(IInputStreamStatus listener) {
            return new Marker(listener, this, pointer);
        }

        /**
		 * @param pointer The pointer from which to calculate what is available
		 * @return The amount of bytes available in the buffer, or 0 if none
		 */
        public int getAvailable(int pointer) {
            if (pointer < this.pointer || next != null) {
                return this.pointer - pointer + (next == null ? 0 : next.getAvailable(0));
            }
            return 0;
        }

        /**
		 * Public access to starting a read operation.
		 * 
		 * @param marker The marker from which to start reading from
		 * 
		 * @see #read(byte[], int, MarkerOperation)
		 */
        public MarkerOperation read(byte[] buffer, int pos, int len, Marker marker) {
            return this.read(buffer, pos, len, new MarkerOperation(marker));
        }

        /**
 		 * @param buffer The buffer to read to
		 * @param pos The position to read to
		 * @param op The operation to record reading to
		 * @return The operation, or null if operation canceled or registry stopped
		 */
        private MarkerOperation read(byte[] buffer, int pos, int len, MarkerOperation op) {
            int read = 0;
            if (pointer == op.marker.pointer) {
                while (pointer == op.marker.pointer && running && op.marker.listener.isOpen()) {
                    ThreadUtil.sleep(100);
                }
            }
            if (op.marker.pointer <= data.length) {
                if (len > pointer - op.marker.pointer) {
                    System.arraycopy(data, op.marker.pointer, buffer, pos, pointer - op.marker.pointer);
                    op.update(read = pointer - op.marker.pointer);
                    op.marker.update(pointer - op.marker.pointer);
                    if (op.marker.pointer <= data.length) return op;
                } else {
                    System.arraycopy(data, op.marker.pointer, buffer, pos, len);
                    op.marker.update(len);
                    return op.update(len);
                }
            }
            while (read < len && next == null && running && op.marker.listener.isOpen()) {
                ThreadUtil.sleep(100);
            }
            if (read < len && next == null || !op.marker.listener.isOpen()) return null;
            return next == null || read == len ? op : next.read(buffer, pos + read, len - op.read, op.update(new Marker(op.marker.listener, next, 0)));
        }

        /**
		 * @param data The data to write
		 * @param pos The position to write from
		 * @param len The amount of data to write
		 * @return This buffer or a new one if this one is full
		 */
        public ChainedBuffer write(byte[] data, int pos, int len) {
            if (this.pointer + len >= this.data.length) {
                System.arraycopy(data, pos, this.data, pointer, this.data.length - pointer);
                this.next = new ChainedBuffer();
                return this.next.write(data, pos + (this.data.length - pointer), len - pos);
            } else {
                System.arraycopy(data, pos, this.data, pointer, len);
                this.pointer += len;
            }
            return this;
        }
    }

    /**
	 * A marker recording the current position in a {@link ChainedBuffer}.
	 * 
	 * @author elponderador
	 * @author $Author$
	 * @version $Id$
	 */
    private class Marker {

        protected IInputStreamStatus listener;

        protected final ChainedBuffer buffer;

        protected int pointer;

        public Marker(IInputStreamStatus listener, ChainedBuffer buffer, int pointer) {
            this.buffer = buffer;
            this.pointer = pointer;
            this.listener = listener;
        }

        /**
		 * @return The amount of bytes available from this marker on
		 */
        public int getAvailable() {
            return buffer.getAvailable(pointer);
        }

        /**
		 * @param amount The amount to increment the pointer by
		 * @return This for chaining
		 */
        public Marker update(int inc) {
            this.pointer += inc;
            return this;
        }

        /**
		 * @param buffer The buffer to read to
		 * @param pos The position to start reading to
		 * @return This marker or a new marker for a subsequent chained buffer, or null if no more data is available
		 */
        public MarkerOperation read(byte[] buffer, int pos, int len) {
            return this.buffer.read(buffer, pos, len, this);
        }
    }

    /**
	 * This will record a single read operation for counting read bytes.
	 * 
	 * @author elponderador
	 * @author $Author$
	 * @version $Id$
	 */
    private class MarkerOperation {

        protected int read = 0;

        protected Marker marker;

        protected boolean active = true;

        public MarkerOperation(Marker marker) {
            this.marker = marker;
        }

        /**
		 * @param marker The new marker for continuous reading
		 * @return This for chaining
		 */
        public MarkerOperation update(Marker marker) {
            this.marker = marker;
            return this;
        }

        /**
		 * @param inc The amount to increment read counter
		 * @return This for chaining
		 */
        public MarkerOperation update(int read) {
            this.read += read;
            return this;
        }
    }

    /**
	 * This will allow a single thread to monitor an input stream and
	 * write to a buffer.
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id$
	 */
    private class ProcessInput implements Runnable {

        protected InputStream input;

        private ProcessInput(InputStream input) {
            this.input = input;
        }

        public void run() {
            try {
                byte[] data = new byte[9046];
                int read = -1;
                while (running) {
                    if (input.available() > 0) {
                        read = input.read(data);
                        if (read == -1) break;
                        if (buffer != null) {
                            buffer = buffer.write(data, 0, read);
                        } else break;
                    } else Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                return;
            } catch (IOException e) {
                throw ThrowableManagerRegistry.caught(e);
            } finally {
                this.input = null;
            }
        }
    }
}
