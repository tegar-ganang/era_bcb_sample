package uk.co.pointofcare.echobase.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

/**
 * @author RCHALLEN
 *
 */
public class FileIO {

    static Logger log = Logger.getLogger(FileIO.class);

    public static String NEWLINE = System.getProperty("line.separator");

    public static String readToString(File file) throws java.io.IOException {
        byte[] buffer = new byte[(int) file.length()];
        FileInputStream f = new FileInputStream(file);
        f.read(buffer);
        f.close();
        return new String(buffer);
    }

    public static String readToString(URL url) throws java.io.IOException {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(url.toString());
        client.executeMethod(method);
        return method.getResponseBodyAsString();
    }

    public static void writeStringToFile(String s, File f) {
        PrintWriter pw;
        try {
            pw = FileIO.createPrintFileWriter(f);
        } catch (IOException e) {
            log.error("Could not write to file: " + f.toString());
            throw new RuntimeException(e);
        }
        pw.append(s);
        pw.close();
    }

    public static void writeStream(File f, InputStream is) throws IOException {
        PrintStream pw = new PrintStream(f);
        is.reset();
        int b;
        b = is.read();
        while (b > -1) {
            pw.write(b);
            b = is.read();
        }
        pw.close();
        is.close();
    }

    @Deprecated
    public static BufferedReader createBufferedFileReader(String file_name) throws FileNotFoundException {
        return FileIO.createBufferedFileReader(new File(file_name));
    }

    public static BufferedReader createBufferedFileReader(File file) throws FileNotFoundException {
        BufferedReader attempt = new BufferedReader(new FileReader(file));
        log.info("file opened for reading: " + file);
        return attempt;
    }

    public static BufferedReader createBufferedUtf8FileReader(String file_name) throws FileNotFoundException, UnsupportedEncodingException {
        BufferedReader attempt = new BufferedReader(new InputStreamReader(new FileInputStream(file_name), "UTF-8"));
        log.info("UTF8 file opened for reading: " + file_name);
        return attempt;
    }

    @Deprecated
    public static PrintWriter createPrintFileWriter(String file_name) throws IOException {
        return FileIO.createPrintFileWriter(new File(file_name));
    }

    public static PrintWriter createPrintFileWriter(File file) throws IOException {
        PrintWriter attempt = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        log.debug("file opened for writing: " + file);
        return attempt;
    }

    public static PrintWriter createPrintUtf8FileWriter(String filename) throws IOException {
        return createPrintUtf8FileWriter(new File(filename));
    }

    public static PrintWriter createPrintUtf8FileWriter(File file) throws IOException {
        PrintWriter attempt = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8")));
        log.debug("file opened for UTF8 writing: " + file);
        return attempt;
    }

    public static PrintWriter createPrintUtf16FileWriter(File file) throws IOException {
        PrintWriter attempt = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-16")));
        log.debug("file opened for UTF16 writing: " + file);
        return attempt;
    }

    @Deprecated
    public static PrintWriter xstreamFriendlyWriter(File file) throws IOException {
        PrintWriter pw = createPrintUtf8FileWriter(file);
        pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        return pw;
    }

    public static void recursiveDelete(File f) {
        try {
            for (File g : f.listFiles()) {
                if (g.isDirectory()) recursiveDelete(g);
                g.delete();
            }
            f.delete();
        } catch (NullPointerException e) {
            f.delete();
        }
    }

    public static File tempDirectory() throws IOException {
        File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!(temp.delete()) || !(temp.mkdir())) {
            log.error("Could not create temp directory: " + temp.getAbsolutePath());
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        log.debug("Created directory: " + temp.getAbsolutePath());
        return temp;
    }

    public static ArrayList<File> recursiveLs(File f) {
        return recursiveLs(f, new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return true;
            }
        });
    }

    public static ArrayList<File> recursiveLs(File f, final String extension) {
        return recursiveLs(f, new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(extension)) return true;
                return false;
            }
        });
    }

    public static ArrayList<File> recursiveLs(File f, FilenameFilter filter) {
        ArrayList<File> out = new ArrayList<File>();
        if (f.exists()) {
            if (f.isDirectory()) {
                for (File g : f.listFiles(filter)) {
                    out.add(g);
                }
                for (File g : f.listFiles()) {
                    if (g.isDirectory()) out.addAll(recursiveLs(g, filter));
                }
            } else {
                if (filter.accept(f.getParentFile(), f.getName())) out.add(f);
            }
        }
        return out;
    }

    /**
	 * @param source a File object to copy (cannot be a directory).
	 * @param target a target File object to copy to (will overwrite if it exists, must be an specific filename, not a directory, it will create parent directories if required)
	 * @return true on success, false otherwise
	 */
    public static boolean filecopy(final File source, final File target) {
        boolean out = false;
        if (source.isDirectory() || !source.exists() || target.isDirectory() || source.equals(target)) return false;
        try {
            target.getParentFile().mkdirs();
            target.createNewFile();
            FileChannel sourceChannel = new FileInputStream(source).getChannel();
            try {
                FileChannel targetChannel = new FileOutputStream(target).getChannel();
                try {
                    targetChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
                    out = true;
                } finally {
                    targetChannel.close();
                }
            } finally {
                sourceChannel.close();
            }
        } catch (IOException e) {
            out = false;
        }
        return out;
    }

    /** A convenience method to iterate over the lines in a text file 
	 * @param f a File object
	 * @return an iterable string of all the lines in the file.
	 * @throws FileNotFoundException
	 */
    public static Iterable<String> lineReader(File f) throws FileNotFoundException {
        final BufferedReader reader = FileIO.createBufferedFileReader(f);
        return new Iterable<String>() {

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    String line;

                    boolean cached = false;

                    boolean isCacheFull() {
                        if (line != null) return true; else return false;
                    }

                    @Override
                    public boolean hasNext() {
                        if (cached) return isCacheFull();
                        try {
                            line = reader.readLine();
                            cached = true;
                            return isCacheFull();
                        } catch (IOException e) {
                            return false;
                        }
                    }

                    @Override
                    public String next() {
                        if (cached && isCacheFull()) {
                            return line;
                        } else {
                            if (hasNext()) {
                                return next();
                            } else {
                                throw new NoSuchElementException();
                            }
                        }
                    }

                    @Override
                    public void remove() {
                        line = null;
                        cached = false;
                    }
                };
            }
        };
    }

    /**
	 * @param singleton
	 */
    public static void serialise(Serializable obj, File f) {
        try {
            FileOutputStream fos = new FileOutputStream(f);
            ObjectOutputStream out = new ObjectOutputStream(fos);
            out.writeObject(obj);
            out.close();
            log.debug("Object Persisted to: " + f.getCanonicalPath());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * @param class1
	 * @param out
	 * @return
	 * @throws ClassNotFoundException 
	 */
    public static <T extends Serializable> T deserialise(Class<T> class1, File out) throws ClassNotFoundException {
        try {
            FileInputStream fis = new FileInputStream(out);
            ObjectInputStream in = new ObjectInputStream(fis);
            Object output = in.readObject();
            in.close();
            return class1.cast(output);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
