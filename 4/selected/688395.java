package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Misc {

    public static final String NEWLINE = System.getProperty("line.separator");

    public static final String FOLDERSEPARATOR = System.getProperty("file.separator");

    public static final Level LOGLEVEL = Options.LogLevel;

    static RSA authKey = null;

    public static int msgID = 0;

    public static Logger log = Logger.getLogger("TCPFile", null);

    public static void main(String[] args) {
        init();
    }

    static {
        log = initLogger(log);
    }

    private Misc() {
    }

    public static void init() {
        if (!new File("RSA.key").exists()) {
            authKey = new RSA(3000);
            saveObject("RSA.key", authKey);
        } else authKey = (RSA) Misc.loadObject("RSA.key");
        String home = System.getProperty("user.home");
        File f = new File(home, "tcpfileserver.xml");
        if (!f.exists()) {
            String message = "Did not find config file at " + f.getAbsolutePath() + NEWLINE + "Did not start.";
            log.severe(message);
            echo(message);
            System.exit(40);
        }
        XML.parseDocument(f.getAbsolutePath());
    }

    public static boolean checkName(String name) {
        return name != null && name.length() < 50 && name.length() == 0 || !name.matches("[A-Za-z0-9_]+");
    }

    public static void exit() {
    }

    /**
	 * Overload for Integers using the method echo(Object)
	 * @param bla integer
	 */
    public static void echo(int bla) {
        echo(bla + "");
    }

    /**
	 * Standard output method for Console, which outputs to both the GUI and System.out
	 * @param object The Object which should be given out
	 */
    public static void echo(Object object) {
        String timestamp = getTime();
        String out;
        if (object != null) out = (timestamp + ": " + object.toString()); else out = (timestamp + ": NULL");
        out = out.substring(11, out.length());
        if (out.endsWith(Misc.NEWLINE)) out = out.substring(0, out.length() - 1);
        System.out.println(out);
    }

    public static String getTime() {
        java.util.Date today = new java.util.Date();
        String timestamp = "" + new java.sql.Timestamp(today.getTime());
        while (timestamp.length() < 23) timestamp += "0";
        return timestamp;
    }

    public static Timestamp getTime(int minutes) {
        Date today = new Date();
        Timestamp bla = new Timestamp(today.getTime() + minutes * 60 * 1000);
        return bla;
    }

    /**
	 * Returns the name of the thread. Is used in the logger
	 * @param ThreadID
	 * @return
	 */
    public static String getNameOfThread(int ThreadID) {
        Thread[] bla = new Thread[100];
        Thread.enumerate(bla);
        for (Thread t : bla) if (t != null) {
            int blu = (int) t.getId();
            if (blu == ThreadID) return t.getName();
        }
        return "";
    }

    /**
	 * Standard error handler
	 * @param e The Exception
	 * @param severeness How bad the error is. 10 = ignore, 1 = very bad
	 * Just as a standard for errors
	 */
    public static void error(Exception e, int severeness) {
        if (severeness < 10) e.printStackTrace();
    }

    public static void LoadSettings(String classname) {
        Hashtable settings = (Hashtable) loadObject("options.bin");
        Reflection.LoadSettingsFromHashTable(settings);
    }

    public static void SaveSettings(String classname) {
        Hashtable settings = Reflection.saveAllFields(classname);
        saveObject("options.bin", settings);
    }

    /**
	 * Takes a byte array and zips it using GZIPOutputStream
	 * @param input
	 * @return
	 */
    public static byte[] zip(byte[] input) {
        Misc.log.finest("Zipping input with length " + Misc.prettyPrintSize(input.length));
        try {
            ByteArrayOutputStream bla = new ByteArrayOutputStream();
            GZIPOutputStream gzos = new GZIPOutputStream(bla);
            gzos.write(input);
            gzos.finish();
            input = new byte[5];
            byte[] output = bla.toByteArray();
            bla.close();
            gzos.close();
            Misc.log.finest("Done zipping output length: " + Misc.prettyPrintSize(output.length));
            return output;
        } catch (Exception e) {
            Misc.log.log(Level.WARNING, "", e);
            Misc.log.log(Level.WARNING, "", e);
        }
        return null;
    }

    /**
	 * Unzips a given byte array.
	 * Needs about 4 times less memory than unzip 2 and is about as much faster...
	 * dont know why
	 * @param input A GZIPped byte[]
	 * @return unzipped byte[]
	 */
    public static byte[] unzip(byte[] input) {
        Misc.log.finest("Unzipping input with length " + Misc.prettyPrintSize(input.length));
        try {
            ByteArrayInputStream bla = new ByteArrayInputStream(input);
            GZIPInputStream gzis = new GZIPInputStream(bla);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (gzis.available() > 0) out.write(gzis.read());
            out.flush();
            byte[] output = out.toByteArray();
            gzis.close();
            bla.close();
            out.close();
            Misc.log.finest("Unzipping output length: " + Misc.prettyPrintSize(output.length));
            return copyfromto(output, 0, output.length - 1);
        } catch (Exception e) {
            Misc.log.log(Level.WARNING, "", e);
        }
        return null;
    }

    /**
	 * Copies the given part of a byte[] into a new array
	 * @param input The array which is copied from
	 * @param start
	 * @param end
	 * @return 
	 */
    public static byte[] copyfromto(byte[] input, long start, long end) {
        ByteArrayInputStream in = new ByteArrayInputStream(input);
        if (end > input.length) end = input.length;
        byte[] output = new byte[(int) (end - start)];
        in.skip(start);
        in.read(output, 0, (int) (end - start));
        return output;
    }

    /**
	 * Overload for below Method
	 * @param size in Bytes
	 * @return 2 Digits after Comma
	 */
    public static String prettyPrintSize(long size) {
        return prettyPrintSize((float) size, 0, 2);
    }

    /**
	 * Makes a size more readable (storage in byte). Only a value decrease conversion possible!
	 * @param size the size you want to convert
	 * @param currentUnit the current unit (0=Byte, 1=KB, 2=MB, 3=GB, 4=TB)
	 * @param commaDigits the number of digits after the comma (rounded)
	 * @return a string containig size max 1024 with 1 digit after the comma and the according extension
	 */
    public static String prettyPrintSize(float size, int currentUnit, int commaDigits) {
        String[] ext = new String[] { "B", "KB", "MB", "GB", "TB" };
        int extCount = 0;
        while (size > 1024) {
            size /= 1024f;
            extCount++;
        }
        if (extCount + currentUnit > ext.length || extCount + currentUnit < 0) return size + "";
        float commaPower = (float) java.lang.Math.pow(10, commaDigits);
        return (Math.round((size) * commaPower) / commaPower) + " " + ext[extCount + currentUnit];
    }

    public static void saveObject(String filename, Object output_veld) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            GZIPOutputStream gzos = new GZIPOutputStream(fos);
            ObjectOutputStream out = new ObjectOutputStream(gzos);
            out.writeObject(output_veld);
            out.flush();
            out.close();
        } catch (IOException e) {
            System.out.println(e + output_veld.toString());
        }
    }

    public static Object loadObject(String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            GZIPInputStream gzis = new GZIPInputStream(fis);
            ObjectInputStream in = new ObjectInputStream(gzis);
            Object gelezen_veld = in.readObject();
            in.close();
            return gelezen_veld;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public static void loopthroughdirectory(String folder, String clas, String method) {
        File toplevel = new File(folder);
        if (toplevel.isDirectory()) {
            String[] children = toplevel.list();
            for (int i = 0; i < children.length - 1; i++) {
                if (children[i] != ".." && children[i] != ".") {
                    File current = new File(folder + FOLDERSEPARATOR + children[i]);
                    if (current.isDirectory()) loopthroughdirectory(current.getAbsolutePath(), clas, method);
                    Reflection.selectFunctionWithParameters(clas, method, new Object[] { current });
                }
            }
        }
    }

    /**
	 * Takes a serializable Object and returns it as a byte array
	 * @param obj
	 * @return
	 */
    public static byte[] toByteArray(Object obj) {
        try {
            ByteArrayOutputStream bla = new ByteArrayOutputStream();
            ObjectOutputStream blu = new ObjectOutputStream(bla);
            blu.writeObject(obj);
            return bla.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Downloads the Page at the specified URL and returns it in one string with Newlines
	 * @param pagehtml
	 * @return
	 */
    public static String downloadpage(String pagehtml) {
        boolean successful = false;
        String type = "";
        while (!successful) {
            try {
                String thisLine;
                URL u = new URL(pagehtml);
                BufferedReader d = new BufferedReader(new InputStreamReader(u.openStream()));
                while ((thisLine = d.readLine()) != null) {
                    type += thisLine + Misc.NEWLINE;
                }
                successful = true;
            } catch (MalformedURLException ex) {
                System.err.println("not a URL I understand:" + pagehtml);
                successful = true;
            } catch (IOException e) {
                if ((e) != null) {
                    if (e.toString().contains("UnknownHostException")) successful = true;
                } else System.err.println(e + " " + pagehtml);
            }
        }
        return type;
    }

    public static String readFile(String filename) {
        String full = "";
        try {
            String inputLine;
            BufferedReader is = new BufferedReader(new FileReader(filename));
            while ((inputLine = is.readLine()) != null) {
                full = inputLine + Misc.NEWLINE;
            }
            is.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
        return full;
    }

    /**
	 * Simple text file writer. Creates a file in the program folder and writes a string into it.
	 * @param fileName String with the file name
	 * @param fileContent String with the file content
	 * @return true for a full success and false if something bad happend (Misc.echo is used for the error message)
	 */
    public static boolean writeFile(String fileName, String fileContent) {
        File newNameFile = new File(fileName);
        try {
            newNameFile.createNewFile();
        } catch (IOException e) {
            Misc.log.warning("Couldn't create " + fileName);
            return false;
        }
        if (!newNameFile.canWrite()) {
            Misc.log.warning("Can not write to " + fileName);
            return false;
        }
        Writer output = null;
        try {
            output = new BufferedWriter(new FileWriter(newNameFile));
            output.write(fileContent);
        } catch (IOException e) {
            Misc.log.warning("Writing to " + fileName + " failed");
            return false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Misc.log.warning("Closing BufferedWriter output failed ( " + fileName + ")");
                    return false;
                }
            }
        }
        return true;
    }

    public static int generateInt(int start, int end) {
        SecureRandom random = new SecureRandom();
        int rand = random.nextInt();
        rand = random.nextInt(java.lang.Math.abs((end - start)));
        if (end >= start) rand = (rand) + start; else rand = rand + end;
        return rand;
    }

    public static String generatestring(int length) {
        length = 3 * length;
        SecureRandom random = new SecureRandom();
        String out = "";
        while (out.length() < length) {
            String adding = random.nextInt(255) + "";
            while (adding.length() < 3) adding = "0" + adding;
            out += adding;
        }
        return out;
    }

    /**
	 * Does return a byte[] from the given Hex String
	 * Has been tested and should work for most cases (unlike its predecessor)
	 * @param hex
	 * @return
	 */
    public static byte[] getBytesfromHexString(String hex) {
        hex = hex.replace(":", "").toLowerCase();
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    public static String dumpBytes2(byte[] bs) {
        return dumpBytes(bs).replace(":", "").toLowerCase();
    }

    /**
	 * Returns a byte array in Hex Format
	 * @param bs
	 * @return
	 */
    public static String dumpBytes(byte[] bs) {
        if (bs == null) return "";
        StringBuffer ret = new StringBuffer(bs.length);
        for (int i = 0; i < bs.length; i++) {
            String hex = Integer.toHexString(0x0100 + (bs[i] & 0x00FF)).substring(1);
            ret.append((hex.length() < 2 ? "0" : "") + hex);
            ret.append(":");
        }
        String ret2 = ret.toString().substring(0, ret.length() - 1).toUpperCase();
        return ret2;
    }

    /**
	 * Takes a serialized object in form of a byte array and returns the object
	 * @param input the serialized object
	 * @return Object! Needs casting. Returns null in case of an error
	 */
    public static Object fromByteArrayToObject(byte[] input) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(input);
            ObjectInputStream in = new ObjectInputStream(bis);
            Object gelezen_veld = in.readObject();
            in.close();
            return gelezen_veld;
        } catch (Exception e) {
            Misc.log.warning(e + "");
            Misc.log.log(Level.WARNING, "", e);
        }
        return null;
    }

    /**
	 * Initializes a logger. 
	 * Takes out the unwanted standard root logger
	 * @param log
	 * @return
	 */
    public static Logger initLogger(Logger log) {
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0) rootLogger.removeHandler(handlers[0]);
        log.setFilter(new LogHandler());
        if (Options.LogToConsole) log.addHandler(new LogHandler());
        log.setLevel(LOGLEVEL);
        FileHandler fh = null;
        try {
            if (!new File("log").exists()) new File("log").mkdir();
            fh = new FileHandler("log" + Misc.FOLDERSEPARATOR + "log.txt", 5 * 1024 * 1024, 4, false);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fh.setFormatter(new StivoFormatter());
        if (Options.LogToFile) log.addHandler(fh);
        return log;
    }
}

class StivoFormatter extends java.util.logging.Formatter {

    @Override
    public String format(LogRecord arg0) {
        String timestamp = Misc.getTime();
        String out = timestamp + ": " + Misc.getNameOfThread((int) Thread.currentThread().getId());
        out += " => ";
        out += arg0.getSourceClassName().replace("main.", "");
        out += " => ";
        out += arg0.getSourceMethodName();
        out += " => " + Misc.NEWLINE;
        out += arg0.getLevel() + ": ";
        out += arg0.getMessage();
        if (arg0.getParameters() != null && arg0.getParameters().length > 0) out += Misc.NEWLINE + Arrays.deepToString(arg0.getParameters());
        if (arg0.getParameters() != null && arg0.getParameters()[0].equals("Memory")) {
        }
        return out + Misc.NEWLINE;
    }
}

/**
 * @author stivo
 * Funnily enough, this class can be used as Filter and as a Handler :)
 */
class LogHandler extends Handler implements Filter {

    StivoFormatter formatter = new StivoFormatter();

    HashMap<String, Long> lastmsg = new HashMap<String, Long>();

    @Override
    public boolean isLoggable(LogRecord arg0) {
        if (arg0.getParameters() != null) if (arg0.getParameters()[0].equals("Memory")) return false;
        return true;
    }

    @Override
    public void close() throws SecurityException {
    }

    @Override
    public void flush() {
    }

    @Override
    public void publish(LogRecord arg0) {
        String identifier = arg0.getSourceClassName() + arg0.getSourceMethodName();
        long time = new java.util.Date().getTime();
        boolean show = true;
        if (lastmsg.containsKey(identifier)) {
            if (time - lastmsg.get(identifier) < 100) {
                show = false;
            } else lastmsg.put(identifier, time);
        } else lastmsg.put(identifier, time);
        String out = formatter.format(arg0);
        out = out.substring(25, out.length());
        Misc.echo(out);
    }
}
