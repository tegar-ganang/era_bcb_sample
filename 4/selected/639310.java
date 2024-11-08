package com.simpledata.win32;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Tools to access Windows registry<BR>
 * Execute a command on windows shell (reg query) to gather information 
 * from the system<BR>
 * <BR>
 * This code is 90% cut / paste from: <BR>
 * http://forum.java.sun.com/thread.jsp?thread=519212&forum=54&message=2479370
 */
public class Win32EnvironementVariable {

    private static final String PERSONAL_FOLDER = "\"HKCU\\Software\\Microsoft\\Windows\\CurrentVersion" + "\\Explorer\\Shell Folders\"";

    private static final String REGSTR_TOKEN = "REG_SZ";

    /** the command to execute **/
    private static final String CMD = "reg query";

    /** the code to indicates we want a User value **/
    private static final String CMD_END = "/v Personal";

    private static String currentUserPersonalFolderPath;

    public static String getCurrentUserPersonalFolderPath() {
        return getPersonalKey(PERSONAL_FOLDER);
    }

    public static String getPersonalKey(String key) {
        if (currentUserPersonalFolderPath != null) return currentUserPersonalFolderPath;
        try {
            Process process = Runtime.getRuntime().exec(CMD + " " + key + " " + CMD_END);
            StreamPumper pumper = new StreamPumper(process.getInputStream());
            pumper.start();
            process.waitFor();
            pumper.join();
            String result = pumper.getResult();
            int p = result.indexOf(REGSTR_TOKEN);
            if (p == -1) return null;
            currentUserPersonalFolderPath = result.substring(p + REGSTR_TOKEN.length()).trim();
            return currentUserPersonalFolderPath;
        } catch (Exception e) {
            return null;
        }
    }

    static class StreamPumper extends Thread {

        private InputStream is;

        private StringWriter sw;

        StreamPumper(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) sw.write(c);
            } catch (IOException e) {
            }
        }

        String getResult() {
            return sw.toString();
        }
    }
}
