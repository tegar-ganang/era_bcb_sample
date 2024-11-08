package au.com.forward.futureTalker;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;

/**
 * A collection of utilites for coding tasks so that they can be interrupted.
 *
 *
 * @author Matthew Ford
 */
public class TaskUtilities {

    /**
   * Checks if the interrupt flag is set for the thread running this task
   * and throws an {@link java.lang.InterruptedException} if it is.
   * <p>In order to stop the task, this {@link java.lang.InterruptedException}
   * must propergate out of the {@link java.util.concurrent.Callable#call} method.
   * <p>If this check is done in an internal method which is not defined to
   * throw exceptions you will have to catch the InterruptedException and wrap
   * it in a RuntimeException and re-throw it.
   * @throws java.lang.InterruptedException Thrown if the interrupt flag is set for the thread running this method.
   */
    public static void ifInterruptedStop() throws InterruptedException {
        Thread.yield();
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Stopped by ifInterruptedStop()");
        }
    }

    /**
   * Close this Closable, if it is not null, ignoring any errors.
   * <br>This should only be called in a finally clause after normal
   * close methods have been called.
   * This method is designed only to be called in the cleanup
   * after some error has already occured.
   * <p>
   * Typical usage is
   * <PRE>
   * FileInputStream in_1 = null;
   * FileInputStream in_2 = null;
   * try {
   *   ....
   *   in_1.close(); // throws exception if error.
   *   in_1 = null;  // flag as closed
   *   in_2.close(); // throws exception if error.
   *   in_2 = null;  // flag as closed
   * } finally {
   *   // clean up any un-released resources.
   *   // if streams are local to this method no need to assign them to null here
   *   // but assign to null if global to indicate they have been closed.
   *   // in_1 = (FileInputStream)TaskUtilities.closeIgnoringErrors(in_1); // if global
   *   TaskUtilities.closeIgnoringErrors(in_1); // if local
   *   // in_2 = (FileInputStream)TaskUtilities.closeIgnoringErrors(in_2); // if global
   *   TaskUtilities.closeIgnoringErrors(in_2); // if local
   *   // etc...
   * }</pre>
   * @param c The Closable that needs closing
   * @return null to be assigned to the object being closed.
   */
    public static Closeable closeIgnoringErrors(Closeable c) {
        if (c == null) {
            return null;
        }
        try {
            c.close();
        } catch (Throwable t) {
        }
        return null;
    }

    /**
   * Returns an interruptible InputStream connected to System.in
   *
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   *<p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   *<p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   *
   * @return interruptible stream connected to System.in
   */
    public static InputStream interruptibleSystemIn() {
        return Channels.newInputStream((new FileInputStream(FileDescriptor.in)).getChannel());
    }

    /**
   * Returns an interruptible InputStream connected to a RandomAccessFile.
   * <br>If the RandomAccessFile is not opened for reading, attempting to
   * read from this stream will throw an exception
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible stream connected to the RandomAccessFile
   * @param f_in the RandomAccessFile to connect to.
   */
    public static InputStream interruptibleInputStream(RandomAccessFile f_in) {
        return Channels.newInputStream(f_in.getChannel());
    }

    /**
   * Returns an interruptible OutputStream connected to a RandomAccessFile.
   * <br>flush() does not work due to problem in Sun's library so the 
   * latest output will be lost on closing/interruption.
   * <br>Also you cannot interrupt a write operation. You can only
   * interrupt when the write is blocked.
   * <br>If the RandomAccessFile is not opened for writing, attempting to
   * write to this stream will throw an exception
   * 
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible stream connected to the RandomAccessFile
   * @param f_out the RandomAccessFile to connect to.
   */
    public static OutputStream interruptibleOutputStream(RandomAccessFile f_out) {
        return Channels.newOutputStream(f_out.getChannel());
    }

    /**
   * Returns an interruptible InputStream connected to a FileInputStream.
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible stream connected to the FileInputStream
   * @param f_in the FileInputStream to connect to
   */
    public static InputStream interruptibleInputStream(FileInputStream f_in) {
        return Channels.newInputStream(f_in.getChannel());
    }

    /**
   * Returns an interruptible OutputStream connected to a 
   * FileOutputStream.
   * <br>flush() does not work due to problem in Sun's library so the 
   * latest output will be lost on closing/interruption.
   * <br>Also you cannot interrupt a write operation. You can only
   * interrupt when the write is blocked.
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible stream connected to the FileOutputStream
   * @param f_out the FileOutputStream to connect to.
   */
    public static OutputStream interruptibleOutputStream(FileOutputStream f_out) {
        return Channels.newOutputStream(f_out.getChannel());
    }

    /**
   * Returns an interruptible Reader connected to a RandomAccessFile using the default character set.
   *<br>If the RandomAccessFile is not opened for reading, attempting to
   *read from this stream will throw an exception
   *
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   *<p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   *<p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   *
   * @return interruptible stream connected to the RandomAccessFile
   * @param f_in the RandomAccessFile to connect to.
   */
    public static Reader interruptibleReader(RandomAccessFile f_in) {
        return Channels.newReader(f_in.getChannel(), Charset.defaultCharset().name());
    }

    /**
   * Returns an interruptible Reader connected to a RandomAccessFile using the given character set.
   *<br>If the RandomAccessFile is not opened for reading, attempting to
   *read from this stream will throw an exception
   *
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   *<p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   *<p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   *
   * @return interruptible Reader connected to the RandomAccessFile
   * @param f_in the RandomAccessFile to connect to.
   * @param csName the Charset name.
   *
   * @throws IllegalCharsetNameException - If the given charset name is illegal 
   * @throws UnsupportedCharsetException - If no support for the named charset is
   *  available in this instance of the Java virtual machine
   */
    public static Reader interruptibleReader(RandomAccessFile f_in, String csName) {
        return Channels.newReader(f_in.getChannel(), csName);
    }

    /**
   * Returns an interruptible Writer connected to a 
   * RandomAccessFile using the default character set.
   * <br>flush() does not work due to problem in Sun's library so the 
   * latest output will be lost on closing/interruption.
   * <br>Also you cannot interrupt a write operation. You can only
   * interrupt when the write is blocked.
   * <br>If the RandomAccessFile is not opened for writing, attempting to
   * write to this reader will throw an exception
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible Writer connected to the RandomAccessFile
   * @param f_out the RandomAccessFile to connect to.
   */
    public static Writer interruptibleWriter(RandomAccessFile f_out) {
        return Channels.newWriter(f_out.getChannel(), Charset.defaultCharset().name());
    }

    /**
   * Returns an interruptible Writer connected to a 
   * RandomAccessFile using the given character set.
   * <br>flush() does not work due to problem in Sun's library so the 
   * latest output will be lost on closing/interruption.
   * <br>Also you cannot interrupt a write operation. You can only
   * interrupt when the write is blocked.
   * RandomAccessFile using the given character set.
   * <br>If the RandomAccessFile is not opened for writing, attempting to
   * write to this reader will throw an exception
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible Writer connected to the RandomAccessFile
   * @param f_out the RandomAccessFile to connect to.
   * @param csName the Charset name.
   * @throws IllegalCharsetNameException - If the given charset name is illegal
   * @throws UnsupportedCharsetException - If no support for the named charset is
   *  available in this instance of the Java virtual machine
   */
    public static Writer interruptibleWriter(RandomAccessFile f_out, String csName) {
        return Channels.newWriter(f_out.getChannel(), csName);
    }

    /**
   * Returns an interruptible Reader connected to a FileInputStream using the default character set.
   *
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   *<p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   *<p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   *
   * @return interruptible Reader connected to the FileInputStream
   * @param f_in the FileInputStream to connect to.
   */
    public static Reader interruptibleReader(FileInputStream f_in) {
        return Channels.newReader(f_in.getChannel(), Charset.defaultCharset().name());
    }

    /**
   * Returns an interruptible Reader connected to a FileInputStream using the given character set.
   *
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   *<p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   *<p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   *
   * @return interruptible Reader connected to the FileInputStream
   * @param f_in the FileInputStream to connect to.
   * @param csName the Charset name.
   *
   * @throws IllegalCharsetNameException - If the given charset name is illegal 
   * @throws UnsupportedCharsetException - If no support for the named charset is
   *  available in this instance of the Java virtual machine
   */
    public static Reader interruptibleReader(FileInputStream f_in, String csName) {
        return Channels.newReader(f_in.getChannel(), csName);
    }

    /**
   * Returns an interruptible Writer connected to a 
   * FileOutputStream using the default character set.
   * <br>flush() does not work due to problem in Sun's library so the 
   * latest output will be lost on closing/interruption.
   * <br>Also you cannot interrupt a write operation. You can only
   * interrupt when the write is blocked.
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible Writer connected to the FileOutputStream
   * @param f_out the FileInputStream to connect to.
   */
    public static Writer interruptibleWriter(FileOutputStream f_out) {
        return Channels.newWriter(f_out.getChannel(), Charset.defaultCharset().name());
    }

    /**
   * Returns an interruptible Writer connected to a 
   * FileOutputStream using the given character set.
   * <br>flush() does not work due to problem in Sun's library so the 
   * latest output will be lost on closing/interruption.
   * <br>Also you cannot interrupt a write operation. You can only
   * interrupt when the write is blocked.
   * 
   * <p>If a thread is blocked in an I/O operation on the returned stream 
   * then another thread may invoke the stream's close method. This will cause 
   * the blocked thread to receive an AsynchronousCloseException.
   * <p>
   * If a thread is blocked in an I/O operation on the returned stream,
   * then another thread may invoke the blocked thread's interrupt method.
   * This will cause the stream to be closed, the blocked thread to receive 
   * a ClosedByInterruptException, and the blocked thread's interrupt status to be set.
   * <p>
   * If a thread's interrupt status is already set and it invokes a blocking I/O
   * operation upon a the returned stream, then the stream will be closed and
   * the thread will immediately receive a ClosedByInterruptException.
   * The thread's interrupt status will remain set.
   * @return interruptible Writer connected to the FileOutputStream
   * @param f_out the FileInputStream to connect to.
   * @param csName the Charset name.
   * @throws IllegalCharsetNameException - If the given charset name is illegal
   * @throws UnsupportedCharsetException - If no support for the named charset is
   *  available in this instance of the Java virtual machine
   */
    public static Writer interruptibleWriter(FileOutputStream f_out, String csName) {
        return Channels.newWriter(f_out.getChannel(), csName);
    }
}
