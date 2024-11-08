package org.apache.harmony.nio;

import java.io.FileDescriptor;
import java.nio.Buffer;
import java.nio.channels.Channel;
import org.apache.harmony.luni.platform.FileDescriptorHandler;
import org.apache.harmony.nio.internal.DirectBuffer;
import org.apache.harmony.nio.internal.FileChannelImpl;

public class AddressUtil {

    /**
     * Gets the start address of a direct buffer.
     * <p>
     * This method corresponds to the JNI function:
     * 
     * <pre>
     *    void* GetDirectBufferAddress(JNIEnv* env, jobject buf);
     * </pre>
     * 
     * @param buf
     *            the direct buffer whose address shall be returned must not be
     *            <code>null</code>.
     * @return the address of the buffer given, or zero if the buffer is not a
     *         direct Buffer.
     */
    public static long getDirectBufferAddress(Buffer buf) {
        if (!(buf instanceof DirectBuffer)) {
            return 0;
        }
        return ((DirectBuffer) buf).getEffectiveAddress().toLong();
    }

    /**
     * Gets the address of native resource held by the given channel, if it has
     * any.
     * 
     * For network related channel, including {@link SocketChannel},
     * {@link ServerSocketChannel} and {@link DatagramChannel}, this method
     * returns the Socket handle (long) in Linux, and returns a SOCKET
     * (UINT_PTR) in windows.
     * 
     * For {@link FileChannel}, this method returns the native file descriptor.
     * 
     * For other channels, this method return 0, which means unsupported
     * operation.
     * 
     * @param channel
     *            the given channel which may holds a native resource address
     * @return the address of native resource held by the given channel, if any,
     *         otherwise return 0
     */
    public static long getChannelAddress(Channel channel) {
        if (channel instanceof FileDescriptorHandler) {
            return getFDAddress(((FileDescriptorHandler) channel).getFD());
        } else if (channel instanceof FileChannelImpl) {
            return ((FileChannelImpl) channel).getHandle();
        }
        return 0;
    }

    private static native long getFDAddress(FileDescriptor fd);
}
