package javalightserver.io;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class DataBank {

    private static GregorianCalendar gc = new GregorianCalendar();

    private static int day = gc.get(Calendar.DAY_OF_MONTH);

    private static int month = gc.get(Calendar.MONTH);

    private static int year = gc.get(Calendar.YEAR);

    private static int hour = gc.get(Calendar.HOUR_OF_DAY);

    private static int minute = gc.get(Calendar.MINUTE);

    private static int second = gc.get(Calendar.SECOND);

    private static String TimeResult = day + "/" + month + "/" + year + " " + hour + ":" + minute + ":" + second;

    public DataBank() {
    }

    public static synchronized boolean WriteLog(String line) {
        try {
            File f = new File("jls.log");
            FileOutputStream fos = new FileOutputStream(f, true);
            PrintStream ps = new PrintStream(fos);
            ps.println(TimeResult + " " + line);
        } catch (IOException err) {
            System.out.println("Error in Input/Output: " + err);
        }
        return true;
    }

    public static synchronized boolean SimpleWriteLog(String line) {
        try {
            File f = new File("jls.log");
            FileOutputStream fos = new FileOutputStream(f, true);
            PrintStream ps = new PrintStream(fos);
            ps.print(" " + line);
        } catch (IOException err) {
            System.out.println("Error in Input/Output: " + err);
        }
        return true;
    }

    public static String[] readFile(String file, int[] righe) {
        String ret[] = new String[righe.length];
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            int j = 0;
            for (int i = 1; i <= righe[righe.length - 1]; i++) {
                if (i == righe[j]) ret[j++] = in.readLine(); else in.readLine();
            }
            in.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        return ret;
    }

    public static String inputStr() {
        try {
            BufferedReader flussoInput = new BufferedReader(new InputStreamReader(System.in));
            String stringa = flussoInput.readLine();
            return (stringa);
        } catch (Exception e) {
            System.out.println("Errore: " + e + " in input");
            System.exit(0);
            return ("");
        }
    }

    public static int inputInt() {
        try {
            BufferedReader flussoInput = new BufferedReader(new InputStreamReader(System.in));
            String stringa = flussoInput.readLine();
            return (Integer.valueOf(stringa).intValue());
        } catch (IOException e) {
            System.out.println("Error in I/O: " + e);
            System.exit(0);
            return -1;
        }
    }

    public static long copy(InputStream in, OutputStream out) throws IOException {
        long bytesCopied = 0;
        byte[] buffer = new byte[4096];
        int bytes;
        try {
            while ((bytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytes);
                bytesCopied += bytes;
            }
        } finally {
            in.close();
            out.close();
        }
        return bytesCopied;
    }

    public static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static boolean unZip(ZipFile zipFile) {
        Enumeration entries = zipFile.entries();
        try {
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                System.err.println("Extracting file: " + entry.getName());
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.out.println("Error in the I/O: " + ioe);
            return false;
        }
        return true;
    }

    public static final byte[] intToByteArray(int value) {
        return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
    }

    public static final int byteArrayToInt(byte[] b) {
        return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
    }

    public static void WriteCmdEnabled(int port, String hostname, String wlkMsg, String tmpCat, String tmpCwd, String tmpDf, String tmpExit, String tmpGet, String tmpHelo, String tmpHelp, String tmpHttp, String tmpLs, String tmpMkFile, String tmpMkDir, String tmpMsg, String tmpPasswd, String tmpPwd, String tmpRm, String tmpRmDir, String tmpShell, String enableSSL) {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream("settings.conf"));
            ps.println(port);
            ps.println(hostname);
            ps.println(wlkMsg);
            ps.println(tmpCat);
            ps.println(tmpCwd);
            ps.println(tmpDf);
            ps.println(tmpExit);
            ps.println(tmpGet);
            ps.println(tmpHelo);
            ps.println(tmpHelp);
            ps.println(tmpHttp);
            ps.println(tmpLs);
            ps.println(tmpMkFile);
            ps.println(tmpMkDir);
            ps.println(tmpMsg);
            ps.println(tmpPasswd);
            ps.println(tmpPwd);
            ps.println(tmpRm);
            ps.println(tmpRmDir);
            ps.println(tmpShell);
            ps.println(enableSSL);
            ps.println("### CREATED WITH JAVALIGHTSERVER CONFIGURATION MANAGER 0.4 ###");
        } catch (IOException e) {
            System.out.println("Error in the I/O: " + e);
        }
    }
}
