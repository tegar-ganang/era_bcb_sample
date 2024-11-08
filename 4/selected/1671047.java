package net.sf.asyncobjects.io;

import net.sf.asyncobjects.ACloseable;
import net.sf.asyncobjects.AsyncUnicastServer;
import net.sf.asyncobjects.Promise;
import net.sf.asyncobjects.When;
import net.sf.asyncobjects.util.Condition;
import net.sf.asyncobjects.util.RequestQueue;
import net.sf.asyncobjects.util.Serialized;
import net.sf.asyncobjects.util.Wait;

/**
 * A pipe is pair of output stream and input stream, what is written to output
 * stream, could be later read from input stream. The object supports
 * {@link AChannel} interface. Note that input supports
 * {@link AInput#pushback(BatchedData)} operation.
 * 
 * @param <D>
 *            a batched data type
 * @param <I>
 *            an input type
 * @param <O>
 *            an output type
 * @param <C>
 *            a channel type
 * @see AChannel
 */
public abstract class Pipe<D extends BatchedData<D>, I extends AInput<D>, O extends AOutput<D>, C extends AChannel<D, I, O>> extends AsyncUnicastServer<C> implements AChannel<D, I, O> {

    /** a limit for buffered data */
    private final int bufferedDataLimit;

    /** buffered data for pipe */
    private D bufferedData;

    /** is input closed */
    private boolean inClosed = false;

    /** is input closed */
    private boolean outClosed = false;

    /** read request queue */
    private final RequestQueue reads = new RequestQueue();

    /** read request queue */
    private final RequestQueue writes = new RequestQueue();

    /** input stream */
    private final I in;

    /** output stream */
    private final O out;

    /**
	 * make a pipe
	 * 
	 * @param size
	 *            buffer size for pipe
	 */
    public Pipe(int size) {
        bufferedDataLimit = size;
        in = createInput();
        out = createOutput();
    }

    /**
	 * @return an created output
	 */
    protected abstract O createOutput();

    /**
	 * @return an created input
	 */
    protected abstract I createInput();

    /**
	 * @return input stream
	 * @see AChannel#getInput()
	 */
    public Promise<I> getInput() {
        return Promise.with(in);
    }

    /**
	 * @return output stream
	 * @see AChannel#getOutput()
	 */
    public Promise<O> getOutput() {
        return Promise.with(out);
    }

    /**
	 * Send data to pipe
	 * 
	 * @param data
	 *            a data to be piped out
	 * @return a promise that resolves when pipe is ready for next write
	 *         operation
	 */
    private Promise<Void> send(final D data) {
        return new Serialized<Void>(writes) {

            @Override
            protected Promise<Void> run() throws Throwable {
                if (outClosed) {
                    throw new IllegalStateException("output stream is already closed");
                }
                if (data.length() == 0) {
                    return null;
                }
                if (inClosed) {
                    throw new IllegalStateException("input stream is already closed, so data will " + "be never read");
                }
                if (bufferedData == null) {
                    bufferedData = data;
                } else {
                    bufferedData = bufferedData.concat(data);
                }
                reads.awake();
                return readyForWrite(bufferedDataLimit);
            }
        }.promise();
    }

    /**
	 * @param limit
	 *            a limit to check
	 * @return null or promise that resolves to null when length of buffered
	 *         data less than limit.
	 */
    private Promise<Void> readyForWrite(final int limit) {
        if (bufferedData == null || bufferedData.length() <= limit) {
            return null;
        } else {
            if (inClosed) {
                throw new IllegalStateException("input is alread closed write cannot complete");
            }
            return new When<Void, Void>(writes.awaken()) {

                @Override
                public Promise<Void> resolved(Void o) {
                    return readyForWrite(limit);
                }
            }.promise();
        }
    }

    /**
	 * @return a promise that resolves when output is flushed
	 */
    private Promise<Void> flushOutput() {
        return new Serialized<Void>(writes) {

            @Override
            protected Promise<Void> run() throws Throwable {
                if (outClosed) {
                    throw new IllegalStateException("output stream is already closed");
                }
                if (inClosed) {
                    throw new IllegalStateException("input is alread closed so data could not be flushed");
                }
                return readyForWrite(0);
            }
        }.promise();
    }

    /**
	 * Receive data from pipe
	 * 
	 * @param limit
	 *            a limit to receive
	 * @return a received data or promise for it
	 */
    private Promise<D> receive(final int limit) {
        return new Serialized<D>(reads) {

            @Override
            protected Promise<D> run() throws Throwable {
                return performReceive(limit);
            }
        }.promise();
    }

    /**
	 * Push data back to the pipe
	 * 
	 * @param pushbackData
	 *            a data to push backk
	 * @return a promise that resolves when operation finishes
	 */
    public Promise<Void> pushback(final D pushbackData) {
        return new Serialized<Void>(reads) {

            @Override
            protected Promise<Void> run() throws Throwable {
                if (inClosed) {
                    throw new IllegalStateException("stream is already closed");
                }
                if (pushbackData == null) {
                    throw new NullPointerException("pushbackData cannot be null");
                }
                if (bufferedData == null) {
                    bufferedData = pushbackData;
                } else {
                    bufferedData = pushbackData.concat(bufferedData);
                }
                return null;
            }
        }.promise();
    }

    /**
	 * Receive no more than limit of the data
	 * 
	 * @param limit
	 *            a receive limit
	 * @return a received datat or a promise for received data
	 */
    private Promise<D> performReceive(final int limit) {
        if (limit <= 1) {
            throw new IllegalStateException("invalid limit value");
        }
        if (inClosed) {
            throw new IllegalStateException("stream is already closed");
        }
        if (outClosed && bufferedData == null) {
            return null;
        }
        if (bufferedData != null) {
            if (bufferedData.length() < limit) {
                D rc = bufferedData;
                bufferedData = null;
                writes.awake();
                return Promise.with(rc);
            } else {
                D rc = bufferedData.head(limit);
                bufferedData = bufferedData.drop(limit);
                writes.awake();
                return Promise.with(rc);
            }
        } else {
            return new When<Void, D>(reads.awaken()) {

                @Override
                public Promise<D> resolved(Void o) {
                    return performReceive(limit);
                }
            }.promise();
        }
    }

    /**
	 * Close input
	 * 
	 * @return a promise or null
	 */
    private Promise<Void> closeInput() {
        return new Serialized<Void>(reads) {

            @Override
            protected Promise<Void> run() throws Throwable {
                inClosed = true;
                writes.awake();
                return null;
            }
        }.promise();
    }

    /**
	 * Close ouput
	 * 
	 * @return a promise or null
	 */
    private Promise<Void> closeOutput() {
        return new Serialized<Void>(writes) {

            @Override
            protected Promise<Void> run() throws Throwable {
                outClosed = true;
                reads.awake();
                return null;
            }
        }.promise();
    }

    /**
	 * Channel close
	 * 
	 * @return proimse that resolves when channle is closed.
	 */
    public Promise<Void> close() {
        return Wait.all(closeInput(), closeOutput()).toVoid();
    }

    /**
	 * This is an internal class. Input of the pipe. This class supports
	 * {@link AByteInput} interface.
	 */
    public class InternalPipeInput extends AsyncUnicastServer<I> implements AInput<D> {

        /** a private constructor */
        protected InternalPipeInput() {
        }

        /**
		 * read some bytes
		 * 
		 * @param limit
		 *            a maximum amount of data expected by client.
		 * @return Promise for read data or null (if eof)
		 * @see AByteInput#read(int)
		 */
        public Promise<D> read(int limit) {
            return receive(limit);
        }

        /**
		 * Push data back to the stream
		 * 
		 * @param pushbackData
		 *            a data that is being pushed back
		 * 
		 * @return a promise that resolves to null or breaks with exception.
		 * @see AInput#pushback(BatchedData)
		 */
        public Promise<Void> pushback(final D pushbackData) {
            return Pipe.this.pushback(pushbackData);
        }

        /**
		 * close stream
		 * 
		 * @return a promise that resolves to null or breaks with exception.
		 * @see ACloseable#close()
		 */
        public Promise<Void> close() {
            return closeInput();
        }

        /**
		 * @see net.sf.asyncobjects.io.AInput#isPushbackSupported()
		 */
        public Promise<Boolean> isPushbackSupported() {
            return Condition.truePromise();
        }
    }

    /**
	 * This is an internal class. Output of the pipe. This class supports
	 * {@link AByteOutput} interface.
	 */
    public class InternalPipeOutput extends AsyncUnicastServer<O> implements AOutput<D> {

        /**
		 * A constructor
		 */
        protected InternalPipeOutput() {
        }

        /**
		 * Write data to stream
		 * 
		 * @param buffer
		 *            a data to write
		 * @return a promise that resolves when pipe is ready for further
		 *         writing
		 * @see AOutput#write(BatchedData)
		 */
        public Promise<Void> write(D buffer) {
            return send(buffer);
        }

        /**
		 * Flush output
		 * 
		 * @return when flush finishes
		 * @see AByteOutput#flush()
		 */
        public Promise<Void> flush() {
            return flushOutput();
        }

        /**
		 * close stream
		 * 
		 * @return a promise that resolves to null or breaks with exception.
		 * @see ACloseable#close()
		 */
        public Promise<Void> close() {
            return closeOutput();
        }
    }
}
