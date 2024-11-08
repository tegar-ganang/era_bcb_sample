package net.sf.asyncobjects.io;

import java.io.EOFException;
import net.sf.asyncobjects.Promise;
import net.sf.asyncobjects.When;
import net.sf.asyncobjects.util.AsyncProcess;
import net.sf.asyncobjects.util.Wait;

/**
 * IO Utilties
 * 
 * @author const
 * 
 */
public class IOUtils {

    /** an unlmited value */
    public static final int UNLIMITED = -1;

    /**
	 * Discard specified amount of elements from input. Discard process also
	 * finishes when EOF is encountered.
	 * 
	 * @param <D>
	 *            a batched data type
	 * @param in
	 *            an input
	 * @param length
	 *            a length to discard, {@link #UNLIMITED} means discard until
	 *            EOF
	 * @param readDataSize
	 *            a portion that is used to discard
	 * @return a promise that resolves to amount of discarded elments (as
	 *         {@link Long}). Note that amount is counted using long's +
	 *         operation. So for very large datasets amount will be invalid.
	 */
    public static <D extends BatchedData<D>> Promise<Long> discard(final AInput<D> in, final long length, final int readDataSize) {
        if (in == null) {
            throw new NullPointerException("Input must not be null.");
        }
        if (readDataSize < 1) {
            throw new IllegalArgumentException("readDataSize must be positive: " + readDataSize);
        }
        if (length < -1) {
            throw new IllegalArgumentException("Length must be UNLIMITED or non-negative: " + length);
        }
        return new AsyncProcess<Long>() {

            long remainingLength = length;

            long discardedLength = 0;

            @Override
            public void run() {
                if (remainingLength == 0) {
                    success(discardedLength);
                }
                final int toRead;
                if (remainingLength != UNLIMITED && readDataSize > remainingLength) {
                    toRead = (int) remainingLength;
                } else {
                    toRead = readDataSize;
                }
                new ProcessWhen<D>(in.read(toRead)) {

                    @Override
                    protected Promise<Void> resolved(D value) throws Throwable {
                        if (value == null) {
                            success(discardedLength);
                        } else {
                            discardedLength += value.length();
                            if (remainingLength != UNLIMITED) {
                                remainingLength -= value.length();
                            }
                            run();
                        }
                        return null;
                    }
                };
            }
        }.promise();
    }

    /**
	 * Write specified binary data repeating it until length bytes are written.
	 * The data is truncated if lengh is not mulitpliy of data.length().
	 * 
	 * @param <D>
	 *            a batched data type
	 * @param out
	 *            an output stream
	 * @param data
	 *            data
	 * @param length
	 *            length to be written. if {@link #UNLIMITED}, data is being
	 *            written infinitely until exception is encountered.
	 * @return a promise that resolves to amount of written bytes (as
	 *         {@link Long}). Note that amount is counted using long's +
	 *         operation. So for very large datasets amount will be invalid.
	 */
    public static <D extends BatchedData<D>> Promise<Long> repeat(final AOutput<D> out, final D data, final long length) {
        if (out == null) {
            throw new NullPointerException("Output must not be null.");
        }
        if (data == null) {
            throw new NullPointerException("Data must not be null.");
        }
        if (data.length() == 0) {
            throw new IllegalArgumentException("Empty data is invalid.");
        }
        if (length < -1) {
            throw new IllegalArgumentException("Length must be UNLIMITED or non-negative: " + length);
        }
        return new AsyncProcess<Long>() {

            long remainingLength = length;

            @Override
            public void run() {
                if (remainingLength == 0) {
                    success(length);
                } else {
                    D toWrite;
                    if (0 < remainingLength && remainingLength < data.length()) {
                        toWrite = data.head((int) remainingLength);
                    } else {
                        toWrite = data;
                    }
                    if (remainingLength != UNLIMITED) {
                        remainingLength -= toWrite.length();
                    }
                    new ProcessWhen<Void>(out.write(toWrite)) {

                        @Override
                        protected Promise<Void> resolved(Void value) throws Throwable {
                            run();
                            return null;
                        }
                    };
                }
            }
        }.promise();
    }

    /**
	 * Read exactly specified amount data from byte input
	 * 
	 * @param in
	 *            a byte input to read from
	 * @param amount
	 *            an amount to read
	 * @return promise for read data or exception
	 */
    public static Promise<BinaryData> readFully(final AByteInput in, final int amount) {
        return readFully(BinaryData.EMPTY, in, amount);
    }

    /**
	 * Read exactly specified amount data from text input
	 * 
	 * @param in
	 *            a byte input to read from
	 * @param amount
	 *            an amount to read
	 * @return promise for read data or exception
	 */
    public static Promise<TextData> readFully(final ATextInput in, final int amount) {
        return readFully(TextData.EMPTY, in, amount);
    }

    /**
	 * Read exactly specified amount data from byte input
	 * 
	 * @param <D>
	 *            a data type
	 * @param empty
	 *            an empty data
	 * @param in
	 *            a byte input to read from
	 * @param amount
	 *            an amount to read
	 * @return promise for read data or exception
	 */
    public static <D extends BatchedData<D>> Promise<D> readFully(final D empty, final AInput<D> in, final int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative: " + amount);
        }
        if (amount == 0) {
            return Promise.with(empty);
        }
        return new AsyncProcess<D>() {

            private D rc = empty;

            @Override
            public void run() {
                new ProcessWhen<D>(in.read(amount - rc.length())) {

                    @Override
                    protected Promise<Void> resolved(D value) throws Throwable {
                        if (value == null) {
                            throw new EOFException("Stream terminated before reading required" + " amount of bytes");
                        }
                        rc = rc.concat(value);
                        if (rc.length() == amount) {
                            success(rc);
                            return null;
                        }
                        if (rc.length() > amount) {
                            throw new IllegalStateException("Underlying stream returned more data " + "than have been asked for.");
                        }
                        run();
                        return null;
                    }
                };
            }
        }.promise();
    }

    /**
	 * Forward data from input to output
	 * 
	 * @param <D>
	 *            a batched data type
	 * @param in
	 *            a data source
	 * @param out
	 *            a data destination
	 * @param length
	 *            a length to copy ({@link #UNLIMITED} means until eof)
	 * @param readDataSize
	 *            a portion to use during copying process
	 * @return a promise that resolves to amount of discarded bytes (as
	 *         {@link Long}). Note that amount is counted using long's +
	 *         operation. So for very large datasets amount will be invalid.
	 */
    public static final <D extends BatchedData<D>> Promise<Long> forward(final AInput<D> in, final AOutput<D> out, final long length, final int readDataSize) {
        return forward(in, out, length, readDataSize, false);
    }

    /**
	 * Forward data from input to output
	 * 
	 * @param <D>
	 *            a batched data type
	 * @param in
	 *            a data source
	 * @param out
	 *            a data destination
	 * @param length
	 *            a length to copy ({@link #UNLIMITED} means until eof)
	 * @param readDataSize
	 *            a portion to use during copying process
	 * @param autoflush
	 *            if true, autoflush after each write
	 * @return a promise that resolves to amount of discarded bytes (as
	 *         {@link Long}). Note that amount is counted using long's +
	 *         operation. So for very large datasets amount will be invalid.
	 */
    public static final <D extends BatchedData<D>> Promise<Long> forward(final AInput<D> in, final AOutput<D> out, final long length, final int readDataSize, final boolean autoflush) {
        if (in == null) {
            throw new NullPointerException("Input must not be null.");
        }
        if (out == null) {
            throw new NullPointerException("Output must not be null.");
        }
        if (readDataSize < 1) {
            throw new IllegalArgumentException("readDataSize must be positive: " + readDataSize);
        }
        if (length < -1) {
            throw new IllegalArgumentException("Length must be UNLIMITED or non-negative: " + length);
        }
        return new AsyncProcess<Long>() {

            long remainingLength = length;

            long copiedLength = 0;

            @Override
            public void run() {
                if (remainingLength == 0) {
                    success(copiedLength);
                    return;
                }
                run(null);
            }

            /**
			 * The read-write cycle. Note that cycle does read and write
			 * operation with data from previous cycle operations at the same
			 * time.
			 * 
			 * @param data
			 *            null or data to write
			 */
            void run(D data) {
                final int toRead;
                if (remainingLength != UNLIMITED && readDataSize > remainingLength) {
                    toRead = (int) remainingLength;
                } else {
                    toRead = readDataSize;
                }
                final Promise<Object[]> readAndWrite = Wait.all(readData(in, toRead), writeData(out, data));
                new ProcessWhen<Object[]>(readAndWrite) {

                    @Override
                    protected Promise<Void> resolved(Object[] value) throws Throwable {
                        D data = IOUtils.<D>blindCast(value[0]);
                        if (data == null) {
                            success(copiedLength);
                        } else {
                            copiedLength += data.length();
                            if (remainingLength != UNLIMITED) {
                                remainingLength -= data.length();
                            }
                            run(data);
                        }
                        return null;
                    }
                };
            }

            /**
			 * Read data if there is still something to read
			 * 
			 * @param in
			 *            an input stream
			 * @param toRead
			 *            amount to read
			 * @return promise for input stream or promise for null if limit is
			 *         reached.
			 */
            private Promise<D> readData(final AInput<D> in, final int toRead) {
                if (toRead == 0) {
                    return Promise.nullPromise();
                } else {
                    return in.read(toRead);
                }
            }

            /**
			 * Write data and flush it if autoflush is true
			 * 
			 * @param out
			 *            output stream
			 * @param data
			 *            a data to write
			 * @return a promise that resolves when data is written
			 */
            private Promise<Void> writeData(final AOutput<D> out, D data) {
                if (data == null) {
                    return null;
                }
                if (autoflush) {
                    return new When<Void, Void>(out.write(data)) {

                        protected Promise<Void> resolved(Void value) throws Throwable {
                            return out.flush();
                        }
                    }.promise();
                } else {
                    return out.write(data);
                }
            }
        }.promise();
    }

    /**
	 * Compare inputs. Two stream are being read and compared. If the
	 * 
	 * @param <D>
	 *            a batched data type
	 * @param in1
	 *            first input
	 * @param in2
	 *            second input
	 * @param readSize
	 *            max amount to read during single iteration of comparison
	 * @return a comparison result (see {@link BinaryData} for comparison
	 *         algorithm)
	 */
    public static <D extends BatchedData<D>> Promise<Integer> compare(final AInput<D> in1, final AInput<D> in2, final int readSize) {
        if (in1 == null) {
            throw new NullPointerException("First input must not be null.");
        }
        if (in2 == null) {
            throw new NullPointerException("Second input must not be null.");
        }
        if (readSize < 1) {
            throw new NullPointerException("Read size should be at least one: " + readSize);
        }
        return new AsyncProcess<Integer>() {

            /** data of the first stream */
            D data1;

            /** data of the second stream */
            D data2;

            /**
			 * @see net.sf.asyncobjects.util.AsyncProcess#run()
			 */
            @Override
            protected void run() throws Throwable {
                prepareData();
            }

            /**
			 * Prepare data phase of the loop
			 */
            private void prepareData() {
                if (data1 == null & data2 == null) {
                    new ProcessWhen<Object[]>(Wait.all(in1.read(readSize), in2.read(readSize))) {

                        @Override
                        protected Promise<Void> resolved(Object[] value) throws Throwable {
                            data1 = IOUtils.<D>blindCast(value[0]);
                            data2 = IOUtils.<D>blindCast(value[1]);
                            compare();
                            return null;
                        }
                    };
                } else if (data1 == null) {
                    new ProcessWhen<D>(in1.read(readSize)) {

                        @Override
                        protected Promise<Void> resolved(D value) throws Throwable {
                            data1 = value;
                            compare();
                            return null;
                        }
                    };
                } else {
                    new ProcessWhen<D>(in2.read(readSize)) {

                        @Override
                        protected Promise<Void> resolved(D value) throws Throwable {
                            data2 = value;
                            compare();
                            return null;
                        }
                    };
                }
            }

            /**
			 * Compare phase of the loop
			 */
            private void compare() {
                if (data1 == null && data2 == null) {
                    success(0);
                    return;
                } else if (data1 == null) {
                    success(data2.head(0).compareTo(data2));
                    return;
                } else if (data2 == null) {
                    success(data1.compareTo(data1.head(0)));
                    return;
                }
                int toCompare = Math.min(data1.length(), data2.length());
                D d1;
                if (toCompare == data1.length()) {
                    d1 = data1;
                    data1 = null;
                } else {
                    d1 = data1.head(toCompare);
                    data1 = data1.drop(toCompare);
                }
                D d2;
                if (toCompare == data2.length()) {
                    d2 = data2;
                    data2 = null;
                } else {
                    d2 = data2.head(toCompare);
                    data2 = data2.drop(toCompare);
                }
                int result = d1.compareTo(d2);
                if (result != 0) {
                    success(result);
                    return;
                }
                prepareData();
            }
        }.promise();
    }

    /**
	 * Read all data from the stream. This utility should be used with a great
	 * care since it has an obvious resource managment issues.
	 * 
	 * @param <D>
	 *            a data
	 * @param initial
	 *            intial data, the next read data is appended to it
	 * @param in
	 *            input stream
	 * @param limit
	 *            a limit for individual read operations
	 * @return all data from the stream
	 */
    public static <D extends BatchedData<D>> Promise<D> readAll(final D initial, final AInput<D> in, final int limit) {
        if (in == null) {
            throw new NullPointerException("input must not be null");
        }
        if (initial == null) {
            throw new NullPointerException("initial must not be null");
        }
        return new AsyncProcess<D>() {

            D current = initial;

            @Override
            protected void run() throws Throwable {
                new ProcessWhen<D>(in.read(limit)) {

                    @Override
                    protected Promise<Void> resolved(D value) throws Throwable {
                        if (value == null) {
                            success(current);
                        } else {
                            current = current.concat(value);
                            run();
                        }
                        return null;
                    }
                };
            }
        }.promise();
    }

    /**
	 * Read all data from the stream. This utility should be used with a great
	 * care since it has an obvious resource managment issues.
	 * 
	 * @param in
	 *            input stream
	 * @param limit
	 *            a limit for individual read operations
	 * @return all data from the stream
	 */
    public static Promise<BinaryData> readAll(final AByteInput in, final int limit) {
        return readAll(BinaryData.empty(), in, limit);
    }

    /**
	 * Read all data from the stream. This utility should be used with a great
	 * care since it has an obvious resource managment issues.
	 * 
	 * @param in
	 *            input stream
	 * @param limit
	 *            a limit for individual read operations
	 * @return all data from the stream
	 */
    public static Promise<TextData> readAll(final ATextInput in, final int limit) {
        return readAll(TextData.empty(), in, limit);
    }

    /**
	 * Blindly cast a value. This is done to remove warning.
	 * 
	 * @param
	 * <Q> a type to cast to
	 * @param value
	 *            a value to cast
	 * @return a cased value
	 */
    @SuppressWarnings({ "unchecked" })
    private static <Q> Q blindCast(Object value) {
        return (Q) value;
    }
}
