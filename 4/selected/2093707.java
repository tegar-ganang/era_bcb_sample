package jpfm;

import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;

/**
 * Would be used to allow mounting initiated using pismo quick mount feature.
 * But it would not be a good idea to have a javaformatter. That can make
 * quickmount of other archives slow as well. That is why this is not supported.
 * @author Shashank Tulsyan
 */
public final class JPfmFSFinder {

    /**
     * In an instance originating from native side only one formatter exists
     */
    private static Class<? extends JPfmBasicFileSystem> formatterClass = null;

    public static boolean canManage(String name, java.nio.ByteBuffer volumeRawData, java.nio.channels.FileChannel volumeFileChannel) {
        return true;
    }

    public static boolean serve(String name, String mountLocation, java.io.FileDescriptor fileDescriptor) {
        if (formatterClass == null) {
            return false;
        }
        Method createMethod = null;
        try {
            createMethod = formatterClass.getDeclaredMethod("create", String.class, FileChannel.class, String.class);
        } catch (Exception any) {
            any.printStackTrace();
            return false;
        }
        try {
            createMethod.invoke(null, name, new FileInputStream(fileDescriptor).getChannel(), mountLocation);
        } catch (Exception any) {
            any.printStackTrace();
        }
        return true;
    }
}
