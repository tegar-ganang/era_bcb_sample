package oragen.util;

import java.io.*;

/**
 *
 * @author arunvenkat
 */
public class RegQuery {

    private static final String REGQUERY_UTIL = "reg query ";

    private static final String REGSTR_TOKEN = "REG_SZ";

    public static String getValue(String path, String key) {
        final String REG_PATH = REGQUERY_UTIL + "\"" + path + "\" /v " + key;
        System.out.println(REG_PATH);
        try {
            Process process = Runtime.getRuntime().exec(REG_PATH);
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

    static class StreamReader extends Thread {

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
