package jexcel;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class Registry {

    private static final String REGQUERY_UTIL = "reg query ";

    private static final String REGSTR_TOKEN = "REG_SZ";

    private static final String EXCEL_PATH_CMD = REGQUERY_UTIL + "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Office\\10.0\\Excel\\InstallRoot";

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println(getExcelInstallRoot());
    }

    public static String getExcelInstallRoot() {
        try {
            Process process = Runtime.getRuntime().exec(EXCEL_PATH_CMD);
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
