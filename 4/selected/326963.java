package org.tcpfile.fileio;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tcpfile.gui.GUI;
import org.tcpfile.main.Misc;

/**
 * Handles all the IO from/to the HardDisk.
 * @author Stivo
 *
 */
public class FileHandling {

    private static Logger log = LoggerFactory.getLogger(FileHandling.class);

    private static Map<String, RandomAccessFile> readfilesCache = new WeakHashMap<String, RandomAccessFile>(10);

    private static Map<String, FileChannel> readfileChannelCache = new WeakHashMap<String, FileChannel>(10);

    private static Map<String, RandomAccessFile> writefilesCache = new WeakHashMap<String, RandomAccessFile>(10);

    private static Map<String, FileChannel> writefileChannelCache = new WeakHashMap<String, FileChannel>(10);

    private static HashSet<String> writePathes = new HashSet<String>();

    private static HashSet<String> readPathes = new HashSet<String>();

    private static int readHits = 0;

    private static int writeHits = 0;

    private static int readMisses = 0;

    private static int writeMisses = 0;

    public static byte[] readFileToByteArray(final String path) {
        return readPartOfFile(path, 0, Long.MAX_VALUE);
    }

    public static byte[] readPartOfFile(final String path, final long pos, long end) {
        try {
            RandomAccessFile raf;
            raf = readfilesCache.get(path);
            if (readPathes.contains(path)) {
                if (raf == null) readMisses++; else readHits++;
            }
            if (raf == null) {
                raf = new RandomAccessFile(path, "r");
                readfilesCache.put(path, raf);
                readPathes.add(path);
            }
            if (raf.length() < end) end = raf.length();
            if (raf.length() < pos) throw new NullPointerException("Start Position is not in File!");
            FileChannel fc;
            if ((fc = readfileChannelCache.get(path)) == null) {
                fc = raf.getChannel();
                readfileChannelCache.put(path, fc);
            }
            fc.position(pos);
            ByteBuffer bb = ByteBuffer.allocate((int) ((end - pos)));
            fc.read(bb);
            return bb.array();
        } catch (FileNotFoundException e) {
            log.warn("", e);
        } catch (IOException e) {
            log.warn("", e);
        }
        return null;
    }

    public static boolean writeFile(final String path, byte[] input) throws IOException {
        return writePartOfFile(path, 0, input);
    }

    public static boolean writePartOfFile(final String path, final long pos, byte[] input) throws IOException {
        try {
            RandomAccessFile raf;
            raf = writefilesCache.get(path);
            if (writePathes.contains(path)) {
                if (raf == null) writeMisses++; else writeHits++;
            }
            if (raf == null) {
                raf = new RandomAccessFile(path, "rw");
                writefilesCache.put(path, raf);
                writePathes.add(path);
            }
            FileChannel fc;
            if ((fc = writefileChannelCache.get(path)) == null) {
                fc = raf.getChannel();
                writefileChannelCache.put(path, fc);
            }
            long end = pos + input.length;
            fc.position(pos);
            FileLock fl = fc.tryLock(pos, end, false);
            if (!fl.isValid()) log.info("Lock invalid on " + path + ". Writing anyway");
            ByteBuffer bb = ByteBuffer.wrap(input);
            fc.write(bb);
            fl.release();
            return true;
        } catch (FileNotFoundException e) {
            log.warn("", e);
        }
        return false;
    }

    public static void printHitsMisses() {
        log.info("Read: {} Hits vs {} Misses", readHits, readMisses);
        log.info("Write: {} Hits vs {} Misses", writeHits, writeMisses);
    }

    public static synchronized void deleteFromCache(String path) {
        try {
            writefileChannelCache.remove(path).close();
        } catch (Exception e1) {
            log.debug("", e1);
        }
        try {
            writefilesCache.remove(path).close();
        } catch (Exception e1) {
            log.debug("", e1);
        }
        try {
            readfileChannelCache.remove(path).close();
        } catch (Exception e1) {
            log.debug("", e1);
        }
        try {
            readfilesCache.remove(path).close();
        } catch (Exception e1) {
            log.debug("", e1);
        }
    }

    /**
	 * Convenience Method which reads a text file into a String 
	 * @param filename
	 * @return
	 */
    public static String readFile(File f) {
        try {
            String out = FileUtils.readFileToString(f);
            return out;
        } catch (IOException e) {
            log.warn("", e);
        }
        return "";
    }

    /**
	 * Convenience Method which reads a text file into a String 
	 * @param filename
	 * @return
	 */
    public static String readFile(String filename) {
        return readFile(new CFile(filename));
    }

    /**
	 * Simple text file writer. Creates a file in the program folder and writes a string into it.
	 * @param fileName String with the file name
	 * @param fileContent String with the file content
	 * @return true for a full success and false if something bad happend (Misc.echo is used for the error message)
	 */
    public static boolean writeFile(String fileName, String fileContent) {
        try {
            FileUtils.writeStringToFile(new CFile(fileName), fileContent);
            return true;
        } catch (IOException e1) {
            log.warn("", e1);
        }
        return false;
    }

    /**
	 * creates a directory
	 * @param destination
	 * @return true on success and if it already exists and false otherwise
	 */
    public static boolean createDir(String destination) {
        File f = new CFile(destination);
        if (f.exists()) {
            return true;
        }
        try {
            f.mkdir();
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    /**
	 * copys a file
	 * @param from relative or absolut name of the source file
	 * @param to relative or absolut name of the target file
	 * @param overWrite whether an existing target file should be overwritten
	 * @param suppressWarnings display no warnings
	 * @return true on success, false otherwise
	 */
    public static boolean fileCopy(String from, String to, boolean overWrite, boolean suppressWarnings) {
        File source = new File(from);
        if (!source.exists()) {
            if (!suppressWarnings) {
                log.warn("Couldn't find file: " + from);
            }
            return false;
        }
        File target = new File(to);
        if (target.exists() && !overWrite) {
            if (!suppressWarnings) {
                log.warn("Target already exists: " + from);
            }
            return false;
        }
        try {
            copyFile(source, target);
        } catch (Exception e) {
            if (!suppressWarnings) {
                log.warn("Failed to copy file from: " + from + " to: " + to);
            }
            return false;
        }
        return true;
    }

    /**
	 * the effective file copy method
	 * @param in file to copy
	 * @param out target file
	 * @throws Exception
	 */
    public static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    /**
	 * Reads a file within the jar
	 * @param source relative or absolut name of the source file
	 * @return true on success, false otherwise
	 */
    public static String readFileFromJar(String source) {
        try {
            InputStream fis = GUI.class.getResourceAsStream(source);
            if (fis == null) {
                log.info("Could not open inputstream to {}" + source);
                return "";
            }
            int length = fis.available();
            byte[] bytes = new byte[length];
            fis.read(bytes);
            return new String(bytes);
        } catch (IOException e) {
            log.warn("", e);
        }
        return "";
    }

    public static String readStringFromJar(String s) {
        InputStream is = null;
        BufferedReader br = null;
        String line = "";
        String out = "";
        try {
            is = Misc.class.getResourceAsStream(s);
            br = new BufferedReader(new InputStreamReader(is));
            while (null != (line = br.readLine())) {
                out += line + Misc.NEWLINE;
            }
        } catch (Exception e) {
            log.warn("", e);
        } finally {
            try {
                if (br != null) br.close();
                if (is != null) is.close();
            } catch (IOException e) {
                log.warn("", e);
            }
        }
        return out;
    }

    public static List<String> readTextFromJar(String s) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        ArrayList<String> list = new ArrayList<String>();
        try {
            is = Misc.class.getResourceAsStream(s);
            br = new BufferedReader(new InputStreamReader(is));
            while (null != (line = br.readLine())) {
                list.add(line);
            }
        } catch (Exception e) {
            log.warn("", e);
        } finally {
            try {
                if (br != null) br.close();
                if (is != null) is.close();
            } catch (IOException e) {
                log.warn("", e);
            }
        }
        return list;
    }

    /**
	 * copys a file from the programs jar to the users system
	 * @param source relative or absolut name of the source file
	 * @param target relative or absolut name of the target file
	 * @param overWrite wheter an existing target file should be overwritten
	 * @param suppressWarnings display no warnings
	 * @return true on success, false otherwise
	 */
    public static boolean copyFromJarToClient(String source, String target, boolean overWrite, boolean suppressWarnings) {
        java.net.URL fileURL = GUI.class.getResource(source);
        if (fileURL == null) {
            if (!suppressWarnings) {
                log.warn("Couldn't find file: " + source);
            }
            return false;
        }
        File targetfile = new CFile(target);
        if (targetfile.exists() && !overWrite) {
            if (!suppressWarnings) {
                log.info("Target already exists: " + target);
            }
            return false;
        }
        try {
            InputStream fis = GUI.class.getResourceAsStream(source);
            FileOutputStream fos = new FileOutputStream(targetfile);
            byte[] buf = new byte[1024];
            int i = 0;
            while ((i = fis.read(buf)) != -1) {
                fos.write(buf, 0, i);
            }
            fis.close();
            fos.close();
        } catch (FileNotFoundException e) {
            if (!suppressWarnings) {
                log.warn("File not found: " + target);
            }
            return false;
        } catch (IOException e) {
            if (!suppressWarnings) {
                log.warn("Failed to copy: " + target);
            }
            return false;
        }
        return true;
    }

    public static String takeOutPathFromFile(File top, File cur) {
        String out = "";
        while (!top.equals(cur) && cur != null) {
            out = cur.getName() + "/" + out;
            cur = cur.getParentFile();
        }
        return "/" + out;
    }
}
