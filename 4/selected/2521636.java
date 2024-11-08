package org.granite.webcompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class WebCompilerUtils {

    private static final String WINDOWS_OS = "windows";

    private static final String MAC_OS = "mac os x";

    private static final String MAC_OS_DARWIN = "darwin";

    private static final String LINUX_OS = "linux";

    public static void createFileFromStream(File f, String name) throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        FileOutputStream fos = new FileOutputStream(f);
        byte buffer[] = new byte[4096];
        int read = 0;
        do {
            read = is.read(buffer);
            if (read != -1) {
                bout.write(buffer, 0, read);
            }
        } while (read != -1);
        fos.write(bout.toByteArray());
        fos.flush();
        fos.close();
    }

    public static void createFileFromStream(String name, String baseDir, String finalName) throws IOException {
        File f = new File(baseDir + File.separator + finalName);
        if (!f.exists()) {
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            createFileFromStream(f, name);
        }
    }

    public static void createFileFromStream(String name, String baseDir) throws IOException {
        createFileFromStream(name, baseDir, name);
    }

    public static String osString() {
        return System.getProperty("os.name").toLowerCase();
    }

    /**
	  * Return a boolean to show if we are running on Windows.
	  * 
	  * @return true if we are running on Windows.
	  */
    public static boolean isWindows() {
        return osString().startsWith(WINDOWS_OS);
    }

    /**
	  * Return a boolean to show if we are running on Linux.
	  * 
	  * @return true if we are running on Linux.
	  */
    public static boolean isLinux() {
        return osString().startsWith(LINUX_OS);
    }

    /**
	  * Return a boolean to show if we are running on Mac OS X.
	  * 
	  * @return true if we are running on Mac OS X.
	  */
    public static boolean isMac() {
        return osString().startsWith(MAC_OS) || osString().startsWith(MAC_OS_DARWIN);
    }
}
