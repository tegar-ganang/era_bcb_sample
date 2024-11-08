package net.sf.ovanttasks.ovnative.win32;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class OV4NLibLoader {

    static boolean loaded = false;

    static String libName;

    public static String getLibName(boolean appendVersion) {
        if (libName == null) {
            Properties props = new Properties();
            try {
                props.load(OV4NLibLoader.class.getResourceAsStream("ov-native.properties"));
                libName = "ov-native-win32";
                if (appendVersion) {
                    libName += "-" + props.getProperty("net.sf.ovanttasks.ovnative.libversion");
                }
            } catch (IOException e) {
                throw new Win32Exception("Unable to get libname fom properties", e);
            }
        }
        return libName;
    }

    static synchronized void loadLibWithoutVersion() {
        if (loaded) {
            return;
        }
        libName = null;
        System.loadLibrary(getLibName(false));
        loaded = true;
    }

    public static synchronized void loadLib() {
        if (loaded) {
            return;
        }
        libName = null;
        try {
            System.loadLibrary(getLibName(true));
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            File file = new File(System.getProperty("user.home") + File.separator + ".ov4n" + File.separator + getLibName(true) + ".dll");
            if (!file.exists()) {
                try {
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdir();
                    }
                    System.out.println("Install native library " + file.getAbsolutePath());
                    InputStream in = OV4NLibLoader.class.getResourceAsStream("/" + getLibName(true) + ".dll");
                    if (in == null) {
                        throw new Win32Exception("Cant find dll in resource");
                    }
                    OutputStream out = new FileOutputStream(file);
                    byte buffer[] = new byte[8192];
                    int read;
                    int written = 0;
                    while ((read = in.read(buffer)) > -1) {
                        written += read;
                        out.write(buffer, 0, read);
                    }
                    out.close();
                    in.close();
                    if (written == 0) {
                        file.delete();
                        loaded = true;
                    } else {
                        loaded = true;
                    }
                } catch (Exception ex) {
                    file.delete();
                    ex.printStackTrace();
                    throw new Win32Exception("Unable to copy " + file.getName() + " to user.home" + File.separator + ".ov4n.", ex);
                }
            }
            System.load(file.getAbsolutePath());
        }
    }
}
