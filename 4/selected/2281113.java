package com.warserver;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;
import com.warserver.Jvm;

/**
 * This class contains static methods I hijacked from the JBoss project. <br>
 * These methods make it easier to download and unzip files.
 *
 * @author  Kurt Olsen
 * @version 1.0 
 */
public class JBoss {

    /** The following methods were hijacked from JBoss, with slight modifications */
    public static final int PACK_ALL_EXCEPT_JAVA_AND_JAR = 0;

    public static final int PACK_CLASSES_ONLY = 1;

    public static final int PACK_ALL = 2;

    public static URL jbdownload(URL _src, URL _dest) throws IOException {
        if (!_dest.getProtocol().equals("file")) throw new IOException("only file: protocol is allowed as destination!");
        InputStream in;
        OutputStream out;
        String s = _dest.getFile();
        File dir = new File(s.substring(0, s.lastIndexOf("/")));
        if (!dir.exists()) dir.mkdirs();
        in = _src.openStream();
        out = new FileOutputStream(s);
        jbwrite(in, out);
        out.close();
        in.close();
        return _dest;
    }

    /** copies the source to the destination. The destination is composed from the
   _destDirectory, _prefix and _suffix  */
    public static URL jbdownloadTemporary(URL _src, URL _destDirectory, String _prefix, String _suffix) throws IOException {
        return jbdownload(_src, jbcreateTempFile(_destDirectory, _prefix, _suffix));
    }

    /** 
    * packs the source directory the _src url points to to a jar archiv at the _dest position
    *
    * @param _src the directory containing the files to be jar'd, as a 'file:' protocal url.
    * @param _dest the name of the jar file to create.
    * @param _prefix a directory name to add in front a filename going into the jar.
    * @param packMode use one of PACK_ALL, PACK_CLASSES_ONLY PACK_ALL_EXCEPT_JAVA_AND_JAR 
    */
    public static void jbdownloadAndPack(URL _src, URL _dest, String _prefix, int packMode) throws IOException {
        if (!_dest.getProtocol().equals("file")) throw new IOException("only file: protocol is allowed as destination!");
        if (!_src.getProtocol().equals("file")) throw new IOException("only file: protocol is allowed as source!");
        InputStream in;
        OutputStream out;
        String s = _dest.getFile();
        File dir = new File(s.substring(0, s.lastIndexOf("/")));
        if (!dir.exists()) dir.mkdirs();
        JarOutputStream jout = new JarOutputStream(new FileOutputStream(_dest.getFile()));
        jbadd(jout, new File(_src.getFile()), _prefix, packMode);
        jout.close();
    }

    /** 
    * Adds the files to a JarOutputStream.
    *
    *
    * @param _jout JarOutputStream to pack the file(s) into.
    * @param _dir all files under this directory are jar'd. 
    * @param _prefix a directory name to add in front a filename going into the jar.
    * @param packMode use one of PACK_ALL, PACK_CLASSES_ONLY PACK_ALL_EXCEPT_JAVA_AND_JAR 
    
    */
    private static void jbadd(JarOutputStream _jout, File _dir, String _prefix, int packMode) throws IOException {
        File[] content = _dir.listFiles();
        for (int i = 0, l = content.length; i < l; ++i) {
            if (content[i].isDirectory()) {
                jbadd(_jout, content[i], _prefix + (_prefix.equals("") ? "" : "/") + content[i].getName(), packMode);
            } else {
                boolean canPack = false;
                switch(packMode) {
                    case PACK_ALL_EXCEPT_JAVA_AND_JAR:
                        if (!(content[i].getName().endsWith(".java") || content[i].getName().endsWith(".jar"))) canPack = true;
                        break;
                    case PACK_CLASSES_ONLY:
                        if (content[i].getName().endsWith(".class")) canPack = true;
                        break;
                    case PACK_ALL:
                        canPack = true;
                        break;
                    default:
                        canPack = true;
                }
                if (canPack == true) {
                    _jout.putNextEntry(new ZipEntry(_prefix + "/" + content[i].getName()));
                    FileInputStream in = new FileInputStream(content[i]);
                    jbwrite(in, _jout);
                    in.close();
                }
            }
        }
    }

    /** packs the source directory the _src url points to to a jar archiv at
   the position composed from _destDir, _prefix and _suffix */
    public static URL jbdownloadAndPackTemporary(URL _src, URL _destDir, String _prefix, String _suffix, int packMode) throws IOException {
        if (!_destDir.getProtocol().equals("file")) throw new IOException("only file: protocol is allowed as destination!");
        if (!_src.getProtocol().equals("file")) throw new IOException("only file: protocol is allowed as source!");
        InputStream in;
        OutputStream out;
        File dest = new File(jbcreateTempFile(_destDir, _prefix, _suffix).getFile());
        JarOutputStream jout = new JarOutputStream(new FileOutputStream(dest));
        jbadd(jout, new File(_src.getFile()), "", packMode);
        jout.close();
        return dest.toURL();
    }

    /** 
    * Extracts a jar file into the _dest directory. If _dest exists it is deleted first.
    *
    * @param _src the jar file to extract
    * @param _dest the directory to extract into, if it exists it is deleted first!
    *
    */
    public static URL jbdownloadAndInflate(URL _src, URL _dest) throws IOException {
        InputStream in;
        OutputStream out;
        in = _src.openStream();
        boolean jar = false;
        String jarPath = "";
        String filePath;
        String fileName;
        String s = _dest.toString();
        if (!_dest.getProtocol().equals("file")) throw new IOException("only file: protocol is allowed as destination!");
        File base = new File(_dest.getFile());
        if (base.exists()) jbdeleteTree(_dest);
        base.mkdirs();
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            String name = entry.getName();
            if (!entry.isDirectory()) {
                int x = name.lastIndexOf("/");
                if (x != -1) {
                    File dir = new File(base.getCanonicalPath() + File.separator + name.substring(0, x));
                    if (!dir.exists()) dir.mkdirs();
                }
                out = new FileOutputStream(base.getCanonicalPath() + File.separator + name);
                jbwrite(zin, out);
                out.close();
            }
        }
        zin.close();
        return _dest;
    }

    /** inflates the given zip file into a directory created in the dest directory with the 
   given prefix */
    public static URL jbdownloadAndInflateTemporary(URL _src, URL _destDir, String _prefix) throws IOException {
        return jbdownloadAndInflate(_src, jbcreateTempDir(_destDir, _prefix));
    }

    /** creates a directory like the File.createTempFile method */
    public static URL jbcreateTempDir(URL _baseDir, String _prefix) throws IOException {
        do {
            File f = new File(_baseDir.getFile(), _prefix + jbgetId());
            if (!f.exists()) {
                f.mkdirs();
                return f.toURL();
            }
        } while (true);
    }

    private static int id = 1000;

    /** used by createTempDir */
    private static String jbgetId() {
        return String.valueOf(++id);
    }

    /** creates a temporary file like File.createTempFile() */
    public static URL jbcreateTempFile(URL _baseDir, String _prefix, String _suffix) throws IOException {
        File f = new File(_baseDir.getFile());
        if (!f.exists()) f.mkdirs();
        File file;
        do {
            file = new File(f, _prefix + jbgetId() + _suffix);
        } while (!file.createNewFile());
        return file.toURL();
    }

    /** deletes the given file:/... url recursively */
    public static void jbdeleteTree(URL _dir) throws IOException {
        if (!_dir.getProtocol().equals("file")) throw new IOException("Protocol not supported");
        File f = new File(_dir.getFile());
        if (!jbdelete(f)) throw new IOException("deleting " + _dir.toString() + "recursively failed!");
    }

    /** deletes a file recursively */
    private static boolean jbdelete(File _f) throws IOException {
        if (_f.exists()) {
            if (_f.isDirectory()) {
                File[] files = _f.listFiles();
                for (int i = 0, l = files.length; i < l; ++i) if (!jbdelete(files[i])) return false;
            }
            return _f.delete();
        }
        return true;
    }

    /** writes the content of the InputStream into the OutputStream */
    private static void jbwrite(InputStream _in, OutputStream _out) throws IOException {
        int b;
        while ((b = _in.read()) != -1) _out.write((byte) b);
        _out.flush();
    }
}
