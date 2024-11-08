package com.ununbium.Util;

import java.io.*;
import java.util.Hashtable;
import java.util.zip.*;
import java.util.jar.*;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

/**
* Utility File Classes to overcome bugs in windows, yet functions in other operating systems just fine
*/
public class FileUtil {

    private static final Logger logger = Logger.getLogger(FileUtil.class);

    private FileUtil() {
    }

    /**
	* Utility method to rename a file
	*/
    public static void renameTo(File src, File dst) {
        for (int i = 0; i < 20; i++) {
            if (src.renameTo(dst)) return;
            System.gc();
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            }
        }
    }

    /**
	* Utility method to delete a file
	*/
    public static void deleteFile(File src) {
        for (int i = 0; i < 20; i++) {
            if (src.delete()) {
                return;
            }
            System.gc();
            try {
                Thread.sleep(50);
            } catch (Exception e) {
            }
        }
    }

    /**
	* Utility method to delete an entire directory structure
	*/
    public static void removeDir(File dir) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                FileUtil.removeDir(files[i]);
            } else {
                deleteFile(files[i]);
            }
        }
        deleteFile(dir);
    }

    /**
	* Utility method to read an entire file and return it as a string
	*/
    public static String readFileToString(File in) throws IOException {
        char[] buf = new char[1024];
        int count = 0;
        StringBuffer sb = new StringBuffer();
        InputStreamReader isr = new InputStreamReader(new FileInputStream(in));
        while ((count = isr.read(buf, 0, 1024)) != -1) {
            sb.append(buf, 0, count);
        }
        isr.close();
        return sb.toString();
    }

    /**
	* Utility method to write a string to a file, overwriting the file contents with the string
	*/
    public static void writeStringToFile(String st, File in) throws IOException {
        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(in)));
        pw.print(st);
        pw.close();
    }

    /**
	* Utility method to convert an unformatted string to XML
	*/
    public static String encodeXML(String s) {
        String t = s.replaceAll("&", "&amp;");
        s = t.replaceAll("<", "&lt;");
        t = s.replaceAll(">", "&gt;");
        s = t.replaceAll("\"", "&quot;");
        return s.replaceAll("\'", "&apos;");
    }

    /**
	* Utility method to write the beginning of an xml file
	*/
    public static void writeStart(PrintWriter pw) {
        writeBlock(pw, "<?xml version=\"1.0\" encoding=\"utf-8\"?>", 0);
    }

    /**
	* Utility method to write a property to an XML configuration File
	*/
    public static void writeProp(PrintWriter pw, String prop, Object value, int depth) {
        writeProp(pw, prop, value, depth, false);
    }

    public static void writeProp(PrintWriter pw, String prop, Object value, int depth, boolean cdata) {
        for (int i = 0; i < depth; i++) pw.print("\t");
        if (value != null && !value.equals("")) {
            pw.print("<" + prop + ">");
            if (cdata) {
                pw.print("<![CDATA[" + value.toString() + "]]>");
            } else {
                pw.print(encodeXML(value.toString()));
            }
            pw.println("</" + prop + ">");
        } else {
            pw.println("<" + prop + " />");
        }
    }

    /**
	* Utility method to write a String block to an XML configuration File
	*/
    public static void writeBlock(PrintWriter pw, String block, int depth) {
        for (int i = 0; i < depth; i++) pw.print("\t");
        pw.println(block);
    }

    /**
	* Utility method to copy a file from one location to another
	*/
    public static void copyFile(File src, File dst) throws IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(src));
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(dst));
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = is.read(buf, 0, 1024)) != -1) os.write(buf, 0, count);
        is.close();
        os.close();
    }

    /**
	* Utility method to jar a directory up
	*/
    public static void jarDirectory(File dir, JarOutputStream jos, String dirToRemove) throws IOException {
        File[] entries = dir.listFiles();
        byte[] buf = new byte[1024];
        int count;
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].isDirectory()) {
                jarDirectory(entries[i], jos, dirToRemove);
            } else {
                InputStream is = new BufferedInputStream(new FileInputStream(entries[i].getPath()));
                String destFile = entries[i].getPath().replaceAll(dirToRemove, "");
                destFile = destFile.replaceAll("\\\\", "/");
                JarEntry entry = new JarEntry(destFile);
                jos.putNextEntry(entry);
                while ((count = is.read(buf)) != -1) {
                    jos.write(buf, 0, count);
                }
            }
        }
    }

    /** 
	* Utility function to extract a jar file from a URL (another jar) for dynamic class loading
	*/
    public static File extractToTempFile(InputStream input) throws IOException {
        File tempFile = File.createTempFile("driver", ".jar");
        tempFile.deleteOnExit();
        BufferedInputStream is = new BufferedInputStream(input);
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(tempFile));
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = is.read(buf, 0, 1024)) != -1) os.write(buf, 0, count);
        is.close();
        os.close();
        return tempFile;
    }

    /**
	* Utility method to open a file and read it into a byte[] 
	*/
    public static byte[] readFileToByteArray(File src) throws IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(src));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = is.read(buf, 0, 1024)) != -1) os.write(buf, 0, count);
        is.close();
        os.close();
        return os.toByteArray();
    }

    private static class ScriptParser {

        private Hashtable<String, String> _scriptHash = null;

        public ScriptParser() {
            _scriptHash = new Hashtable<String, String>();
        }

        public Hashtable<String, String> getHash() {
            return _scriptHash;
        }

        public void addScript(String name, String project, String description, String definitions, String scriptBodyText, String recordedIndex, String type) {
            _scriptHash.put("Name", name);
            _scriptHash.put("Project", project);
            _scriptHash.put("Description", description);
            _scriptHash.put("Definitions", definitions);
            _scriptHash.put("ScriptBodyText", scriptBodyText);
            _scriptHash.put("RecordedIndex", recordedIndex);
            _scriptHash.put("Type", type);
        }
    }

    public static synchronized Hashtable<String, String> parseScript(File jarFile) {
        InputStream is = null;
        try {
            JarFile jar = new JarFile(jarFile);
            is = jar.getInputStream(jar.getEntry("script.xml"));
        } catch (IOException e) {
            logger.warn("Error reading script file: " + jarFile, e);
            return null;
        }
        if (is == null) {
            logger.warn("This is not a script file: " + jarFile);
            return null;
        }
        Digester dig = new Digester();
        ScriptParser sp = new ScriptParser();
        dig.push(sp);
        dig.addCallMethod("Script-File", "addScript", 7);
        dig.addCallParam("Script-File/Name", 0);
        dig.addCallParam("Script-File/Project", 1);
        dig.addCallParam("Script-File/Description", 2);
        dig.addCallParam("Script-File/Definitions", 3);
        dig.addCallParam("Script-File/ScriptBodyText", 4);
        dig.addCallParam("Script-File/RecordedIndex", 5);
        dig.addCallParam("Script-File/Type", 6);
        try {
            dig.parse(is);
        } catch (Exception e) {
            logger.warn("Error parsing script file: " + jarFile + " Exception: " + e);
        }
        return sp.getHash();
    }

    public static boolean jarContains(File jarFile, String fileName) {
        try {
            JarFile jar = new JarFile(jarFile);
            JarEntry entry = jar.getJarEntry(fileName);
            if (entry == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }
}
