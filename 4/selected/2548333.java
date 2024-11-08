package jaxlib.io.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;
import jaxlib.thread.AsyncTask;
import jaxlib.util.CheckArg;

/**
 * A task reading bytes from one channel and writing them to another.
 * <p>
 * One thread generates data and sends it e.g. to the {@link java.nio.channels.Pipe#sink() sink channel} of a 
 * pipe. Another thread runs a {@code PipeTask}. Latter reads e.g. from a pipe's 
 * {@link java.nio.channels.Pipe#source() source channel} and writes to another channel.
 * </p><p>
 * The task returns normally when the source channel's {@link ReadableByteChannel#read(ByteBuffer) read}
 * method returned a value less than or equal to zero and all bytes have been written to the destination 
 * channel.
 * </p><p>
 * Source and destination channels which are instance of {@link SelectableChannel} are automatically
 * {@link SelectableChannel#configureBlocking(boolean) configured for blocking}. 
 * The {@link SelectableChannel#blockingLock() blocking lock} is held as long as the task runs. When the task
 * exits it reconfigures the channel to its initial blocking mode.
 * </p><p>
 * <b>{@code PipeTask} is not suitable to be used with non-blocking channels.</b>
 * </p><p>
 * If a source channel is instance of {@link FileChannel} then the channels 
 * {@link FileChannel#transferTo(long,long,WritableByteChannel) transferTo} method is used instead of an
 * intermediate buffer. The {@link FileChannel#position() position} of the channel is moved forward after
 * each successful read operation.
 * </p><p>
 * If the source channel is not instance of {@code FileChannel} but the destination channel is, then the
 * {@link FileChannel#transferFrom(ReadableByteChannel,long,long) transferFrom} method of the destination 
 * channel is used instead of an intermediate buffer. The {@link FileChannel#position() position} of the
 * destination channel is moved forward after each successful write operation.
 * </p><p>
 * If the task is configured to close one or both channels, then it guarantees to call the 
 * {@link java.nio.Channel#close() close} method of the channel(s) even if an exception occured or if the
 * task has been cancelled.
 * </p>
 * 
 * @author  <a href="mailto:joerg.wassmer@web.de">Joerg Wassmer</a>
 * @since   JaXLib 1.0
 * @version $Id: PipeTask.java 2773 2010-01-24 02:08:47Z joerg_wassmer $
 */
public class PipeTask extends AsyncTask<Object> {

    private static boolean preferDirectBuffer(final Channel channel) {
        return (channel instanceof SelectableChannel) || (channel instanceof FileChannel);
    }

    private static boolean preferInputByteChannelTransfer(final ReadableByteChannel channel) {
        if (!(channel instanceof InputByteChannel)) return false;
        final String s = channel.getClass().getName();
        return (s == "jaxlib.io.channel.FilePipe$SourceChannel") || (s == "jaxlib.io.channel.FilePipe$SourceStream");
    }

    private static boolean preferOutputByteChannelTransfer(final WritableByteChannel channel) {
        if (!(channel instanceof OutputByteChannel)) return false;
        final String s = channel.getClass().getName();
        return (s == "jaxlib.io.channel.FilePipe$SinkChannel") || (s == "jaxlib.io.channel.FilePipe$SinkStream");
    }

    private final boolean autoCloseSource;

    private final boolean autoCloseDestination;

    private final int bufferSize;

    private final boolean directBuffer;

    private ReadableByteChannel source;

    private WritableByteChannel destination;

    /**
   * Creates a task which transfers bytes from the specified source to the specified destination channel,
   * using a buffer of size {@code 8192}.
   * <p>
   * The task will close both channels when the transfer has been done or when an exception occurs.
   * It is unspecified whether the task will use a direct or a heap buffer.
   * </p>
   *
   * @param source
   *  the channel to read from.
   * @param destination
   *  the channel to read to.
   *
   * @throws NullPointerException
   *  if {@code (source == null) || (destination == null)}.
   *
   * @since JaXLib 1.0
   */
    public PipeTask(final ReadableByteChannel source, final WritableByteChannel destination) {
        this(source, destination, 8192);
    }

    /**
   * Creates a task which transfers bytes from the specified source to the specified destination channel,
   * using a buffer of the specified size.
   * <p>
   * The task will close both channels when the transfer has been done or when an exception occurs.
   * It is unspecified whether the task will use a direct or a heap buffer.
   * </p>
   *
   * @param source
   *  the channel to read from.
   * @param destination
   *  the channel to read to.
   * @param bufferSize
   *  the size of the intermediate buffer.
   *
   * @throws IllegalArgumentException
   *  if {@code bufferSize <= 0}.
   * @throws NullPointerException
   *  if {@code (source == null) || (destination == null)}.
   *
   * @since JaXLib 1.0
   */
    public PipeTask(final ReadableByteChannel source, final WritableByteChannel destination, final int bufferSize) {
        this(source, true, destination, true, bufferSize, preferDirectBuffer(source) || preferDirectBuffer(destination));
    }

    /**
   * Creates a task which transfers bytes from the specified source to the specified destination channel,
   * using a buffer of the specified size.
   *
   * @param source
   *  the channel to read from.
   * @param autoCloseSource
   *  whether to close the source stream when the task terminates.
   * @param destination
   *  the channel to read to.
   * @param autoCloseDestination
   *  whether to close the destination stream when the task terminates.
   * @param bufferSize
   *  the size of the intermediate buffer.
   * @param directBuffer
   *  whether to use a direct or a heap buffer.
   *
   * @throws IllegalArgumentException
   *  if {@code bufferSize <= 0}.
   * @throws NullPointerException
   *  if {@code (source == null) || (destination == null)}.
   *
   * @since JaXLib 1.0
   */
    public PipeTask(final ReadableByteChannel source, final boolean autoCloseSource, final WritableByteChannel destination, final boolean autoCloseDestination, final int bufferSize, final boolean directBuffer) {
        super();
        CheckArg.notNull(source, "source");
        CheckArg.notNull(destination, "destination");
        CheckArg.positive(bufferSize, "bufferSize");
        this.autoCloseSource = autoCloseSource;
        this.autoCloseDestination = autoCloseDestination;
        this.directBuffer = directBuffer;
        this.source = source;
        this.destination = destination;
        this.bufferSize = bufferSize;
    }

    private Throwable closeIfRequired(ReadableByteChannel source, WritableByteChannel destination) {
        Throwable ex = null;
        if ((source != null) && this.autoCloseSource) {
            try {
                source.close();
            } catch (final Throwable t) {
                ex = t;
            }
        }
        source = null;
        this.source = null;
        if ((destination != null) && this.autoCloseDestination) {
            try {
                destination.close();
            } catch (final Throwable t) {
                if (ex == null) ex = t;
            }
        }
        destination = null;
        this.destination = null;
        return ex;
    }

    public final int getBufferSize() {
        return this.bufferSize;
    }

    public final boolean isAutoClosingDestination() {
        return this.autoCloseDestination;
    }

    public final boolean isAutoClosingSource() {
        return this.autoCloseSource;
    }

    public final boolean isDirectBuffer() {
        return this.directBuffer;
    }

    protected void runPipe(final ReadableByteChannel source, final WritableByteChannel destination) throws Throwable {
        if (source instanceof FileChannel) transmitFromFileChannel((FileChannel) source, destination); else if (destination instanceof FileChannel) transmitToFileChannel(source, (FileChannel) destination); else if (preferInputByteChannelTransfer(source)) transmitFromInputByteChannel((InputByteChannel) source, destination); else if (preferOutputByteChannelTransfer(destination)) transmitToOutputByteChannel(source, (OutputByteChannel) destination); else transmit(source, destination);
    }

    @Override
    protected Object runTask() throws Throwable {
        final ReadableByteChannel source = this.source;
        final WritableByteChannel destination = this.destination;
        if ((source == null) || (destination == null)) throw new IllegalStateException("already called");
        this.source = null;
        this.destination = null;
        Throwable ex = null;
        if (source instanceof SelectableChannel) {
            SelectableChannel selectableSource = (SelectableChannel) source;
            synchronized (selectableSource.blockingLock()) {
                boolean sourceWasBlocking = selectableSource.isBlocking();
                if (!sourceWasBlocking) selectableSource.configureBlocking(true);
                try {
                    runTask0(source, destination);
                } catch (final Throwable t) {
                    ex = t;
                } finally {
                    if (!sourceWasBlocking) {
                        try {
                            selectableSource.configureBlocking(false);
                        } catch (final Throwable t) {
                            if (ex == null) ex = t;
                        }
                    }
                }
            }
        } else {
            try {
                runTask0(source, destination);
            } catch (final Throwable t) {
                ex = t;
            }
        }
        Throwable t = closeIfRequired(source, destination);
        if (ex != null) ex = t;
        t = null;
        if (ex != null) throw ex; else return null;
    }

    private void runTask0(final ReadableByteChannel source, final WritableByteChannel destination) throws Throwable {
        Throwable ex = null;
        if (destination instanceof SelectableChannel) {
            SelectableChannel selectableDestination = (SelectableChannel) destination;
            synchronized (selectableDestination.blockingLock()) {
                boolean destinationWasBlocking = selectableDestination.isBlocking();
                if (!destinationWasBlocking) selectableDestination.configureBlocking(true);
                try {
                    runPipe(source, destination);
                } catch (final Throwable t) {
                    ex = t;
                } finally {
                    if (!destinationWasBlocking) {
                        try {
                            selectableDestination.configureBlocking(false);
                        } catch (final Throwable t) {
                            if (ex == null) ex = t;
                        }
                    }
                }
            }
        } else {
            runPipe(source, destination);
        }
    }

    private void transmit(ReadableByteChannel source, final WritableByteChannel destination) throws IOException {
        ByteBuffer buffer = null;
        if (this.directBuffer) {
            try {
                buffer = ByteBuffer.allocateDirect(this.bufferSize);
            } catch (final RuntimeException ex) {
            }
        }
        if (buffer == null) buffer = ByteBuffer.allocate(this.bufferSize);
        READ: while (source != null) {
            if (source.read(buffer) <= 0) {
                if ((source != destination) && this.autoCloseSource) source.close();
                source = null;
            }
            buffer.flip();
            if (!buffer.hasRemaining()) return;
            WRITE: while (true) {
                int step = destination.write(buffer);
                if (!buffer.hasRemaining()) {
                    buffer.clear();
                    continue READ;
                } else if ((source == null) || (buffer.position() <= this.bufferSize >> 1)) {
                    if (step <= 0) Thread.yield();
                    continue WRITE;
                } else {
                    buffer.compact();
                    continue READ;
                }
            }
        }
    }

    private static void transmitFromFileChannel(final FileChannel source, final WritableByteChannel destination) throws IOException {
        while (true) {
            final long position = source.position();
            final long step = source.transferTo(position, Long.MAX_VALUE, destination);
            if (step <= 0) break;
            source.position(position + step);
        }
    }

    private static void transmitFromInputByteChannel(final InputByteChannel source, final WritableByteChannel destination) throws IOException {
        while (source.transferToByteChannel(destination, Long.MAX_VALUE) > 0) continue;
    }

    private static void transmitToFileChannel(final ReadableByteChannel source, final FileChannel destination) throws IOException {
        while (true) {
            final long position = destination.position();
            final long step = destination.transferFrom(source, position, Long.MAX_VALUE);
            if (step <= 0) break;
            destination.position(position + step);
        }
    }

    private static void transmitToOutputByteChannel(final ReadableByteChannel source, final OutputByteChannel destination) throws IOException {
        while (destination.transferFromByteChannel(source, Long.MAX_VALUE) > 0) continue;
    }

    /**
   * Overriden to close the channels if configured to do.
   */
    @Override
    protected void taskCancelled() {
        super.taskCancelled();
        closeIfRequired(this.source, this.destination);
    }
}
