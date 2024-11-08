package org.makagiga.commons;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
 * An application locking facility.
 *
 * EXAMPLE:
 * @code
 * if (AppLock.isLocked()) {
 *   System.out.println("Only one instance is allowed.");
 *   System.exit(0);
 * }
 * System.out.println("Loading...");
 * @endcode
 *
 * @see isLocked
 */
public final class AppLock {

    private static FileLock lock;

    private static String file;

    /**
	 * Returns the lock file path or @c null if the @ref isLocked method was not called.
	 */
    public static String getFile() {
        return file;
    }

    /**
	 * Returns @c true if application is locked.
	 * @throws IllegalStateException If @ref org.makagiga.commons.AppInfo.internalName property is not set
	 */
    public static boolean isLocked() {
        AppInfo.checkState();
        file = FS.makeConfigPath(AppInfo.internalName + ".lock");
        try {
            lock = new FileOutputStream(file).getChannel().tryLock();
        } catch (FileNotFoundException exception) {
            return false;
        } catch (IOException exception) {
            return false;
        }
        return (lock == null);
    }

    @Uninstantiable
    private AppLock() {
    }
}
