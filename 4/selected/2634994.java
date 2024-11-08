package org.qtitools.mathqurate.utilities;

import java.io.*;
import java.util.UUID;

/**
 * This was originally a set of utilities that read values from the Windows
 * registry, but has since evolved further to include a function to read
 * proxy config details on the Mac. Essentially this class is used to determine
 * proxy configuration on various platforms
 * @author Paul Neve
 *
 */
public class RegQuery {

    private static final String REGQUERY_UTIL = "reg query ";

    private static final String REGSTR_TOKEN = "REG_SZ";

    private static final String REGDWORD_TOKEN = "REG_DWORD";

    private static final String MAC_SCUTIL_UTIL = "scutil --proxy";

    /**
   * Get an SZ value from Windows registry. Was originally derived from
   * @param param1 Registry location (eg. HKLM\Software\blah\blah)
   * @param param2 Key to extract.
   * @return value of the registry key, or null if not found
   */
    public static String getRegSz(String param1, String param2) {
        try {
            Process process = Runtime.getRuntime().exec(REGQUERY_UTIL + "\"" + param1 + "\" /v " + param2);
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            int p = result.indexOf(REGSTR_TOKEN);
            if (p == -1) return null;
            return result.substring(p + REGSTR_TOKEN.length()).trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
   * Get an DWORD value from Windows registry.
   * @param param1 Registry location (eg. HKLM\Software\blah\blah)
   * @param param2 Key to extract.
   * @return value of the registry key, or 0 if not found
   */
    public static int getRegDWord(String param1, String param2) {
        try {
            Process process = Runtime.getRuntime().exec(REGQUERY_UTIL + "\"" + param1 + "\" /v " + param2);
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            int p = result.indexOf(REGDWORD_TOKEN);
            if (p == -1) return 0;
            String temp = result.substring(p + REGDWORD_TOKEN.length()).trim();
            return Integer.parseInt(temp.substring("0x".length()), 16) + 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
   * Get the first chunk of a random UUID
   * Used to generate unique IDs
   * @return an abbreviated UUID
   */
    public static String miniUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString().split("-")[2];
    }

    /**
   * Get proxy-related setting on the Mac
   * @param setting The setting to get, eg. HTTPProxy
   * @return the value of the setting, or null if not found
   */
    public static String getMacProxySetting(String setting) {
        setting += " : ";
        try {
            Process process = Runtime.getRuntime().exec(MAC_SCUTIL_UTIL);
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
            String result = reader.getResult();
            int p = result.indexOf(setting);
            if (p == -1) return null;
            result = result.substring(p + setting.length()).trim();
            p = result.indexOf(System.getProperty("line.separator"));
            result = result.substring(0, p);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static class StreamReader extends Thread {

        private InputStream is;

        private StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) sw.write(c);
            } catch (IOException e) {
                ;
            }
        }

        String getResult() {
            return sw.toString();
        }
    }
}
