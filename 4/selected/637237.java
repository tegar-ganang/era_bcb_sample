package sce.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class WindowsRegistry {

    private static final String FSLOC = "HKLM\\Software\\Microsoft\\Microsoft Games\\Flight Simulator\\";

    private static final String FS64LOC = "HKLM\\Software\\WOW6432Node\\Microsoft\\Microsoft Games\\Flight Simulator\\";

    private static final String FS9_KEY = "EXE Path";

    private static final String FSX_KEY = "SetupPath";

    private static final String REG_STRING = "REG_SZ";

    public static String readFS9Directory() {
        String value = readRegistry(FSLOC + "9.0", FS9_KEY);
        if ("".equals(value)) {
            value = readRegistry(FS64LOC + "9.0", FS9_KEY);
        }
        return value;
    }

    public static String readFSXDirectory() {
        String value = readRegistry(FSLOC + "10.0", FSX_KEY);
        if ("".equals(value)) {
            value = readRegistry(FS64LOC + "10.0", FSX_KEY);
        }
        return value;
    }

    public static String readRegistry(String location, String key) {
        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            try {
                Process p = Runtime.getRuntime().exec("reg query \"" + location + "\" /v \"" + key + "\"");
                final InputStream is = p.getInputStream();
                final StringWriter sw = new StringWriter();
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            int read;
                            while ((read = is.read()) != -1) {
                                sw.write(read);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                t.start();
                p.waitFor();
                t.join();
                String output = sw.toString();
                if (output.indexOf(REG_STRING) < 0) {
                    return "";
                }
                output = output.substring(output.indexOf(REG_STRING) + REG_STRING.length()).trim();
                return output.replaceAll("\\\\\\\\", "\\\\");
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return "";
    }
}
