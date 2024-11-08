package com.starlight;

import com.starlight.io.CloseBlockingInputStream;
import com.starlight.thread.ThreadKit;
import java.io.*;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.Selector;
import java.nio.channels.WritableByteChannel;
import java.text.MessageFormat;

/**
 * Useful static functions for dealing with I/O.
 *
 * @author reden
 */
public class IOKit {

    /**
	 * This can be used as a hint to {@link #isBeingDeserialized()} that the current
	 * thread is performing deserialization. Setting this to true will indicate that
	 * deserialization is being performed and will allow the method to avoid creating
	 * expensive stack traces. Care should be taken to ensure this is never set
	 * incorrectly or the results of {@link #isBeingDeserialized()} will be incorrect.
	 * If this is not used, the results will still be correct (so don't use it unless
	 * you're certain of what you're doing), but the performance will be worse.
	 */
    public static final ThreadLocal<Boolean> DESERIALIZATION_HINT = new ThreadLocal<Boolean>();

    private static final boolean XSTREAM_SUPPORTED;

    static {
        boolean supported = false;
        try {
            Class.forName("com.thoughtworks.xstream.XStream");
            supported = true;
        } catch (Throwable t) {
        }
        XSTREAM_SUPPORTED = supported;
    }

    /**
	 * Construct an IOException with a cause.
	 */
    public static IOException createIOExceptionWithCause(Throwable cause) {
        return createIOExceptionWithCause(null, cause);
    }

    /**
	 * Construct an IOException with a cause.
	 */
    public static IOException createIOExceptionWithCause(String message, Throwable cause) {
        IOException ioe = new IOException(message);
        ioe.initCause(cause);
        return ioe;
    }

    /**
	 * Close a channel, dealing with nulls or exceptions.
	 */
    public static void close(Selector selector) {
        if (selector == null) return;
        try {
            selector.close();
        } catch (IOException ex) {
        }
    }

    /**
	 * Close a socket, dealing with nulls or exceptions.
	 */
    public static void close(Socket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    /**
	 * Close a server socket, dealing with nulls or exceptions.
	 */
    public static void close(ServerSocket socket) {
        if (socket == null) return;
        try {
            socket.close();
        } catch (IOException e) {
        }
    }

    /**
	 * Close a socket, dealing with nulls or exceptions.
	 */
    public static void close(MulticastSocket socket) {
        if (socket == null) return;
        socket.close();
    }

    /**
	 * Close a socket, dealing with nulls or exceptions.
	 */
    public static void close(DatagramSocket socket) {
        if (socket == null) return;
        socket.close();
    }

    /**
	 * Close a Closable, dealing with nulls.
	 */
    public static void close(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ex) {
        }
    }

    /**
	 * Close an ObjectInput, dealing with nulls.
	 */
    public static void close(ObjectInput input) {
        if (input == null) return;
        try {
            input.close();
        } catch (IOException ex) {
        }
    }

    /**
	 * Close an ObjectOutput, dealing with nulls.
	 */
    public static void close(ObjectOutput output) {
        if (output == null) return;
        try {
            output.close();
        } catch (IOException ex) {
        }
    }

    /**
	 * Convenience method to write a String to an DataOutput. This will write a boolean
	 * saying whether or not the string is null. If it is not, it will write the string
	 * using writeUTF().
	 */
    public static void writeString(DataOutput out, String string) throws IOException {
        if (string == null) out.writeBoolean(false); else {
            out.writeBoolean(true);
            out.writeUTF(string);
        }
    }

    /**
	 * Convenience method to read a string externalized with teh format used by
	 * {@link #writeString}.
	 */
    public static String readString(DataInput in) throws IOException {
        if (in.readBoolean()) {
            return in.readUTF();
        } else return null;
    }

    /**
	 * Read an array of longs that was written with {@link #writeLongArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static long[] readLongArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            long[] result = new long[n];
            for (int i = 0; i < n; ++i) result[i] = in.readLong();
            return result;
        }
    }

    /**
	 * Write an array of longs to be read with {@link #readLongArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeLongArray(DataOutput out, long[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) out.writeLong(array[i]);
        }
    }

    /**
	 * Read an array of ints that was written with {@link #writeIntArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static int[] readIntArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            int[] result = new int[n];
            for (int i = 0; i < n; ++i) result[i] = in.readInt();
            return result;
        }
    }

    /**
	 * Write an array of ints to be read with {@link #readIntArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeIntArray(DataOutput out, int[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) out.writeInt(array[i]);
        }
    }

    /**
	 * Read an array of chars that was written with {@link #writeCharArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static char[] readCharArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            char[] result = new char[n];
            for (int i = 0; i < n; ++i) result[i] = in.readChar();
            return result;
        }
    }

    /**
	 * Write an array of chars to be read with {@link #readCharArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeCharArray(DataOutput out, char[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) out.writeChar(array[i]);
        }
    }

    /**
	 * Read an array of doubles that was written with {@link #writeDoubleArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static double[] readDoubleArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            double[] result = new double[n];
            for (int i = 0; i < n; ++i) result[i] = in.readDouble();
            return result;
        }
    }

    /**
	 * Write an array of doubles to be read with {@link #readDoubleArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeDoubleArray(DataOutput out, double[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) out.writeDouble(array[i]);
        }
    }

    /**
	 * Read an array of shorts that was written with {@link #writeShortArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static short[] readShortArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            short[] result = new short[n];
            for (int i = 0; i < n; ++i) result[i] = in.readShort();
            return result;
        }
    }

    /**
	 * Write an array of shorts to be read with {@link #readShortArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeShortArray(DataOutput out, short[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) out.writeShort(array[i]);
        }
    }

    /**
	 * Read an array of Objects that was written with {@link #writeObjectArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static Object[] readObjectArray(ObjectInput in) throws IOException, ClassNotFoundException {
        return readObjectArray(in, Object.class);
    }

    /**
	 * Read an array of Objects that was written with {@link #writeObjectArray}.
	 * Preserves null & zero-length arrays.
	 *
	 * @param array_component_type The class of the array components that you would
	 *                             like returned. For example, if the array should
	 *                             be a <tt>String[]</tt>, this would be String.class.
	 *                             If null is given, the array will just be an Object
	 *                             array.
	 */
    public static <T> T[] readObjectArray(ObjectInput in, Class<T> array_component_type) throws IOException, ClassNotFoundException {
        int size = in.readInt();
        if (size == -1) return null; else {
            Object[] result = new Object[size];
            for (int i = 0; i < size; ++i) result[i] = in.readObject();
            if (array_component_type == null || array_component_type == Object.class) {
                return (T[]) result;
            }
            @SuppressWarnings({ "unchecked" }) T[] final_array = (T[]) java.lang.reflect.Array.newInstance(array_component_type, size);
            System.arraycopy(result, 0, final_array, 0, size);
            return final_array;
        }
    }

    /**
	 * Write an array of Objects to be read with {@link #readObjectArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeObjectArray(ObjectOutput out, Object[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) out.writeObject(array[i]);
        }
    }

    /**
	 * Read an array of Strings that was written with {@link #writeStringArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static String[] readStringArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            String[] result = new String[n];
            for (int i = 0; i < n; ++i) {
                result[i] = readString(in);
            }
            return result;
        }
    }

    /**
	 * Write an array of Strings to be read with {@link #readStringArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeStringArray(DataOutput out, String[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (int i = 0; i < array.length; i++) {
                writeString(out, array[i]);
            }
        }
    }

    /**
	 * Read an array of bytes that was written with {@link #writeByteArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static byte[] readByteArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            byte[] result = new byte[n];
            if (n != 0) in.readFully(result);
            return result;
        }
    }

    /**
	 * Write an array of bytes to be read with {@link #readByteArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeByteArray(DataOutput out, byte[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            out.write(array);
        }
    }

    /**
	 * Read an array of booleans that was written with {@link #writeByteArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static boolean[] readBooleanArray(DataInput in) throws IOException {
        int n = in.readInt();
        if (n == -1) return null; else {
            boolean[] result = new boolean[n];
            for (int i = 0; i < n; i++) {
                result[i] = in.readBoolean();
            }
            return result;
        }
    }

    /**
	 * Write an array of booleans to be read with {@link #readBooleanArray}.
	 * Preserves null & zero-length arrays.
	 */
    public static void writeBooleanArray(DataOutput out, boolean[] array) throws IOException {
        if (array == null) out.writeInt(-1); else {
            out.writeInt(array.length);
            for (boolean b : array) {
                out.writeBoolean(b);
            }
        }
    }

    /**
	 * Write an NIO ByteBuffer to an OutputStream.
	 */
    public static void write(ByteBuffer buffy, OutputStream out) throws IOException {
        if (!buffy.hasRemaining()) return;
        byte[] buffer = new byte[128];
        while (buffy.hasRemaining()) {
            int remaining = buffy.remaining();
            buffy.get(buffer, 0, remaining);
            if (remaining > buffer.length) out.write(buffer); else out.write(buffer, 0, remaining);
        }
    }

    /**
	 * Serialize an object and return the byte array (suitable for storage).
	 */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream b_out = null;
        ObjectOutputStream o_out = null;
        try {
            b_out = new ByteArrayOutputStream();
            o_out = new ObjectOutputStream(b_out);
            o_out.writeObject(obj);
            return b_out.toByteArray();
        } finally {
            close((Closeable) o_out);
            close(b_out);
        }
    }

    /**
	 * Serialize an object and return the byte array (suitable for storage).
	 */
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        Boolean previous_value = IOKit.DESERIALIZATION_HINT.get();
        IOKit.DESERIALIZATION_HINT.set(Boolean.TRUE);
        try {
            ByteArrayInputStream b_in = null;
            ObjectInputStream o_in = null;
            try {
                b_in = new ByteArrayInputStream(data);
                o_in = new ObjectInputStream(b_in);
                return o_in.readObject();
            } finally {
                close((Closeable) o_in);
                close(b_in);
            }
        } finally {
            if (previous_value == null) IOKit.DESERIALIZATION_HINT.remove(); else IOKit.DESERIALIZATION_HINT.set(previous_value);
        }
    }

    /**
	 * Can be called to check to see if the method was called from within a serialization
	 * call. Example:
	 * <pre>
	 * public class Foo implements Externalizable {
	 *     // FOR SERIALIZATION ONLY!!!
	 *     public Foo() {
	 *         assert IOKit.isBeingDeserialized();
	 *     }
	 * }
	 * </pre>
	 */
    public static boolean isBeingDeserialized() {
        Boolean hint = DESERIALIZATION_HINT.get();
        if (hint != null) return hint.booleanValue();
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement element = stack[i];
            String method_name = element.getMethodName();
            String class_name = element.getClassName();
            if (class_name.equals("java.io.ObjectInputStream")) return true;
            if (XSTREAM_SUPPORTED) {
                if (class_name.equals("com.thoughtworks.xstream.XStream") && method_name.equals("fromXML")) return true;
            }
            if (i == 2 && method_name.equals("readExternal")) return true;
        }
        return false;
    }

    /**
	 * Return the extension on a file (not included the dot). Null will be returned if
	 * there is no file extension.
	 */
    public static String getFileExtension(File file) {
        ValidationKit.checkNonnull(file, "File");
        String name = file.getName();
        String[] toks = name.split("\\.");
        if (toks.length == 0) return null;
        return toks[toks.length - 1];
    }

    /**
	 * Copy the contents of one stream to another. Note this will NOT close the streams.
	 */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) {
                ThreadKit.sleep(50);
                continue;
            }
            output.write(buffer, 0, read);
        }
    }

    /**
	 * Copy the contents of one stream to another. Note this will NOT close the streams.
	 */
    public static void copy(Reader input, Writer output) throws IOException {
        char[] buffer = new char[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) {
                ThreadKit.sleep(50);
                continue;
            }
            output.write(buffer, 0, read);
        }
    }

    /**
	 * Copy from one (blocking) channel to another. This method will technically work
	 * for non-blocking channels, but would be <strong>VERY</strong> inefficient and
	 * so is strongly discouraged.
	 * 
	 * @param input     The input channel, from which data will be read.
	 * @param output    The output channel, to which data will be written.
	 * @param buffer    The buffer to use for the copy, if provided. This may be null, in
	 *                  which case a temporary (64k) one will be created.
	 */
    public static void copy(ReadableByteChannel input, WritableByteChannel output, ByteBuffer buffer) throws IOException {
        if (buffer == null) buffer = ByteBuffer.allocate(1 << 16); else buffer.clear();
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (read != 0) {
                System.out.println("read: " + read);
                buffer.flip();
                while (buffer.hasRemaining()) {
                    output.write(buffer);
                }
            }
            buffer.clear();
        }
    }

    /**
	 * Read a password from the system Console if available or System.in if it isn't.
	 *
	 * @param prompt        The String using MessageFormat syntax with the prompt that
	 *                      should be displayed. This is useful because a warning will
	 *                      be displayed if the password will be echoed to the terminal
	 *                      (if a Console was not available). The following is an example:
	 *                      "Enter password{0}: ". In this case, the following would be
	 *                      displayed if the password would be echoed:
	 *                      "Enter password (warning: password will be visible): ". If
	 *                      null is specified, no prompt will be displayed.
	 */
    public static char[] readPasswordFromConsole(String prompt) throws IOException {
        Console console = System.console();
        if (console != null) {
            if (prompt != null) {
                System.out.print(MessageFormat.format(prompt, ""));
            }
            return console.readPassword();
        }
        if (prompt != null) {
            System.out.print(MessageFormat.format(prompt, " (warning: password will be visible)"));
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(new CloseBlockingInputStream(System.in)));
        try {
            String line = in.readLine();
            if (line == null) return null; else return line.toCharArray();
        } catch (Exception ex) {
            return null;
        } finally {
            IOKit.close(in);
        }
    }

    /**
	 * Read a line of text from the system Console if available or System.in if it isn't.
	 *
	 * @param prompt        The String using MessageFormat syntax with the prompt that
	 *                      should be displayed. If null is specified, no prompt will be
	 *                      displayed.
	 */
    public static String readFromConsole(String prompt) throws IOException {
        if (prompt != null) System.out.print(prompt);
        Console console = System.console();
        if (console != null) {
            return console.readLine();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(new CloseBlockingInputStream(System.in)));
        try {
            String line = in.readLine();
            if (line == null) return null; else return line;
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        } finally {
            IOKit.close(in);
        }
    }

    /**
	 * Deletes a file or directory, recursively deleting all files in the directory
	 * first (if applicable). Use with caution (this is "rm -rf")!
	 */
    public static void deleteRecursive(File file) throws IOException {
        if (!file.exists()) return;
        if (!file.isDirectory()) {
            if (!file.delete()) throw new IOException("Unable to delete " + file);
            return;
        }
        File[] files = file.listFiles();
        for (File child : files) {
            deleteRecursive(child);
        }
        if (!file.delete()) throw new IOException("Unable to delete " + file);
    }
}
