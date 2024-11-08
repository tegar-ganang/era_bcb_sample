package org.softnetwork.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author $Author: smanciot $
 *
 * @version $Revision: 84 $
 */
public class IOTools implements FileExtensions {

    private static IOTools _instance;

    /**
	 * Constructor for IOTools.
	 */
    private IOTools() {
        super();
    }

    /**
	 * Method getInstance.
	 * @return IOTools
	 */
    public static IOTools getInstance() {
        if (_instance == null) {
            _instance = new IOTools();
        }
        return _instance;
    }

    /**
	 * Method isDirectory.
	 * @param path
	 * @return boolean
	 */
    public boolean isDirectory(String path) {
        return new DirectoryFilter().accept(new File(path));
    }

    /**
	 * Method isLibrary.
	 * @param path
	 * @return boolean
	 */
    public boolean isLibrary(String path) {
        return new LibraryFilter().accept(new File(path));
    }

    /**
	 * Method isImage
	 * @param path
	 * @return boolean
	 */
    public boolean isImage(String path) {
        return new ImageFilter().accept(new File(path));
    }

    /**
	 * Method listFiles.
	 * @param root
	 * @param recursive
	 * @param filter
	 * @return File[]
	 */
    public File[] listFiles(File root, boolean recursive, java.io.FileFilter filter) {
        return getFiles(root, new ArrayList(), filter, recursive);
    }

    /**
	 * Method listFilesByExtensions.
	 * @param root
	 * @param recursive
	 * @param exts
	 * @return File[]
	 */
    public File[] listFilesByExtensions(File root, boolean recursive, String[] exts) {
        String str = "";
        for (int i = 0; i < exts.length; i++) {
            str += exts[i] + ((i < exts.length - 1) ? "," : "");
        }
        return getFiles(root, new ArrayList(), new FileExtensionFilter(str), recursive);
    }

    /**
	 * Method listDirectories.
	 * @param root
	 * @param recursive
	 * @return File[]
	 */
    public File[] listDirectories(File root, boolean recursive) {
        return listFiles(root, recursive, new DirectoryFilter());
    }

    /**
	 * Method listFiles.
	 * @param root
	 * @param recursive
	 * @return File[]
	 */
    public File[] listAllFiles(File root, boolean recursive) {
        return listFiles(root, recursive, new AllFilesFilter());
    }

    /**
	 * Method listImages.
	 * @param root
	 * @param recursive
	 * @return File[]
	 */
    public File[] listImages(File root, boolean recursive) {
        return listFiles(root, recursive, new ImageFilter());
    }

    /**
	 * Method listLibraryFiles.
	 * @param root
	 * @param recursive
	 * @return File[]
	 */
    public File[] listLibraries(File root, boolean recursive) {
        return listFiles(root, recursive, new LibraryFilter());
    }

    /**
	 * Method getPath.
	 * @param dir
	 * @param path
	 * @return String
	 */
    public String getPath(String dir, String path) {
        path = path.replace(fs_linux, fs);
        path = path.replace(fs_windows, fs);
        File f;
        if (dir == null) {
            f = new File(path);
            if (f.exists()) return path;
            throw new IllegalArgumentException();
        }
        f = new File(dir);
        if (!f.isDirectory()) f = f.getParentFile();
        if (f.exists()) {
            while (path.startsWith("..")) {
                path = path.substring(2);
                dir = f.getParent();
            }
        }
        path = dir + path;
        f = new File(path);
        if (f.exists()) {
            if (f.isDirectory() && !path.endsWith(FILE_SEPARATOR)) path += FILE_SEPARATOR;
            return path;
        }
        return null;
    }

    /**
	 * Method copyDirectory.
	 * @param inputDir
	 * @param outputDir
	 * @param recursive
	 * @throws IOException
	 */
    public void copyDirectory(File inputDir, String outputDir, boolean recursive) throws IOException {
        if (inputDir.exists()) {
            if (inputDir.isDirectory()) {
                File[] files = listAllFiles(inputDir, false);
                File dir = new File(outputDir);
                if (!dir.exists()) dir.mkdir();
                outputDir += System.getProperty("file.separator") + inputDir.getName();
                dir = new File(outputDir);
                if (!dir.exists()) dir.mkdir();
                for (int i = 0; i < files.length; i++) copyFile(files[i], new File(dir, files[i].getName()));
                if (recursive) {
                    File[] dirs = listDirectories(inputDir, false);
                    for (int i = 0; i < dirs.length; i++) {
                        copyDirectory(dirs[i], outputDir, recursive);
                    }
                }
            } else throw new IOException(inputDir + " is not a directory.");
        } else throw new IOException(inputDir + " does not exist.");
    }

    /**
	 * Method copyFiles.
	 * @param inputDir
	 * @param outputDir
	 * @param filter
	 * @param recursive
	 * @throws IOException
	 */
    public void copyFiles(File inputDir, String outputDir, java.io.FileFilter filter, boolean recursive) throws IOException {
        if (inputDir.exists()) {
            if (inputDir.isDirectory()) {
                File[] files = getFiles(inputDir, new ArrayList(), filter, false);
                File dir = new File(outputDir);
                if (!dir.exists()) dir.mkdir();
                outputDir += System.getProperty("file.separator") + inputDir.getName();
                dir = new File(outputDir);
                if (!dir.exists()) dir.mkdir();
                for (int i = 0; i < files.length; i++) copyFile(files[i], new File(dir, files[i].getName()));
                if (recursive) {
                    File[] dirs = listDirectories(inputDir, false);
                    for (int i = 0; i < dirs.length; i++) {
                        copyFiles(dirs[i], outputDir, filter, recursive);
                    }
                }
            } else throw new IOException(inputDir + " is not a directory.");
        } else throw new IOException(inputDir + " does not exist.");
    }

    /**
	 * Method copyFile.
	 * @param inputFile
	 * @param outputFile
	 * @return FileOutputStream
	 * @throws IOException
	 */
    public FileOutputStream copyFile(File inputFile, File outputFile) throws IOException {
        if (!outputFile.exists()) outputFile.createNewFile();
        return (FileOutputStream) copyInputStream(new FileInputStream(inputFile), new FileOutputStream(outputFile));
    }

    /**
	 * Method getFileContents.
	 * @param in
	 * @return byte[]
	 * @throws IOException
	 */
    public byte[] getFileContents(File in) throws IOException {
        AsyncReadInputStream reader = new AsyncReadInputStream(new FileInputStream(in));
        StringBuffer buffer = new StringBuffer();
        int i;
        while ((i = reader.read()) != -1) {
            buffer.append((char) i);
        }
        reader.close();
        return buffer.toString().getBytes();
    }

    /**
	 * Method copyInputStream.
	 * @param in
	 * @param out
	 * @return OutputStream
	 * @throws IOException
	 */
    public OutputStream copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
        return out;
    }

    /**
	 * Method copyInputStream.
	 * @param in
	 * @return ByteArrayOutputStream
	 * @throws IOException
	 */
    public ByteArrayOutputStream copyInputStream(InputStream in) throws IOException {
        return (ByteArrayOutputStream) copyInputStream(in, new ByteArrayOutputStream());
    }

    /**
	 * Method jar.
	 * @param z
	 * @param root
	 */
    public void jar(File z, File root) {
        jar(z, root, new AllFilesFilter());
    }

    /**
	 * Method jar.
	 * @param z
	 * @param root
	 * @param exts
	 */
    public void jar(File z, File root, String[] exts) {
        String str = "";
        for (int i = 0; i < exts.length; i++) {
            str += exts[i] + ((i < exts.length - 1) ? "," : "");
        }
        jar(z, root, new FileExtensionFilter(str));
    }

    /**
	 * Method jar.
	 * @param z
	 * @param root
	 * @param filter
	 */
    public void jar(File z, File root, FileFilter filter) {
        if (root.exists()) {
            try {
                if (!z.exists()) z.createNewFile();
                if (root.isDirectory()) {
                    JarOutputStream jar = new JarOutputStream(new FileOutputStream(z));
                    File[] jarFiles = listFiles(root, false, filter);
                    for (int i = 0; i < jarFiles.length; i++) {
                        addJarEntry(jar, jarFiles[i], filter, null);
                    }
                    File[] dirs = listDirectories(root, false);
                    for (int i = 0; i < dirs.length; i++) {
                        addJarEntry(jar, dirs[i], filter, null);
                    }
                    jar.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    /**
	 * Method addJarFile.
	 * @param jar
	 * @param jarName
	 * @param jarFile
	 * @throws IOException
	 */
    public void addJarEntry(JarOutputStream jar, String jarName, File jarFile) throws IOException {
        JarEntry entry = new JarEntry(jarName);
        jar.putNextEntry(entry);
        if (!jarFile.isDirectory()) jar.write(getFileContents(jarFile));
        jar.closeEntry();
    }

    /**
	 * Method addJarEntry.
	 * @param jar
	 * @param jarName
	 * @param _bytes
	 * @throws IOException
	 */
    public void addJarEntry(JarOutputStream jar, String jarName, byte[] _bytes) throws IOException {
        JarEntry entry = new JarEntry(jarName);
        jar.putNextEntry(entry);
        if (_bytes != null) jar.write(_bytes);
        jar.closeEntry();
    }

    /**
	 * Method unjar.
	 * @param jar
	 * @param dir
	 */
    public void unjar(JarFile jar, File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        }
        try {
            JarEntry entry;
            Enumeration entries = jar.entries();
            while (entries.hasMoreElements()) {
                entry = (JarEntry) entries.nextElement();
                String name = dir.getPath() + FILE_SEPARATOR + entry.getName();
                if (entry.isDirectory()) {
                    File f = new File(name);
                    if (!f.exists()) f.mkdir();
                } else {
                    copyInputStream(jar.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(name)));
                }
            }
            jar.close();
        } catch (FileNotFoundException ex) {
            org.softnetwork.log.Log4jConnector.getConsole().error(ex.getMessage(), ex);
        } catch (IOException ex) {
            org.softnetwork.log.Log4jConnector.getConsole().error(ex.getMessage(), ex);
        }
    }

    /**
	 * Method zip.
	 * @param z
	 * @param dir
	 */
    public void zip(File z, File root) {
        zip(z, root, new AllFilesFilter());
    }

    /**
	 * Method zip.
	 * @param z
	 * @param root
	 * @param exts
	 */
    public void zip(File z, File root, String[] exts) {
        String str = "";
        for (int i = 0; i < exts.length; i++) {
            str += exts[i] + ((i < exts.length - 1) ? "," : "");
        }
        zip(z, root, new FileExtensionFilter(str));
    }

    /**
	 * Method zip.
	 * @param z
	 * @param root
	 * @param filter
	 */
    public void zip(File z, File root, FileFilter filter) {
        if (root.exists()) {
            try {
                if (!z.exists()) z.createNewFile();
                ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(z));
                addZipEntry(zip, root, filter, null);
                zip.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
	 * Method addZipFile.
	 * @param zip
	 * @param zipName
	 * @param zipFile
	 * @throws IOException
	 */
    public void addZipEntry(ZipOutputStream zip, String zipName, File zipFile) throws IOException {
        ZipEntry entry = new ZipEntry(zipName);
        zip.putNextEntry(entry);
        if (!zipFile.isDirectory()) zip.write(getFileContents(zipFile));
        zip.closeEntry();
    }

    /**
	 * Method addZipEntry.
	 * @param zip
	 * @param zipName
	 * @param _bytes
	 * @throws IOException
	 */
    public void addZipEntry(ZipOutputStream zip, String zipName, byte[] _bytes) throws IOException {
        ZipEntry entry = new ZipEntry(zipName);
        zip.putNextEntry(entry);
        if (_bytes != null) zip.write(_bytes);
        zip.closeEntry();
    }

    /**
	 * Method unzip.
	 * @param zip
	 * @param dir
	 */
    public void unzip(ZipFile zip, File dir) {
        if (!dir.exists()) {
            dir.mkdir();
        }
        try {
            ZipEntry entry;
            Enumeration entries = zip.entries();
            while (entries.hasMoreElements()) {
                entry = (ZipEntry) entries.nextElement();
                String name = dir.getPath() + FILE_SEPARATOR + entry.getName();
                if (entry.isDirectory()) {
                    File f = new File(name);
                    if (!f.exists()) f.mkdir();
                } else {
                    copyInputStream(zip.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(name)));
                }
            }
            zip.close();
        } catch (FileNotFoundException ex) {
            org.softnetwork.log.Log4jConnector.getConsole().error(ex.getMessage(), ex);
        } catch (IOException ex) {
            org.softnetwork.log.Log4jConnector.getConsole().error(ex.getMessage(), ex);
        }
    }

    public static void main(String[] args) throws Exception {
        IOTools _tools = IOTools.getInstance();
        File root = new File("D:\\TMP\\test\\");
        File z = new File("D:\\TMP\\test\\test.jar");
        JarFile _jar = new JarFile("D:\\PROJECTS\\framework\\lib\\framework.jar");
        _tools.unjar(_jar, root);
        _tools.jar(z, root, new String[] { ".class" });
        _tools.unjar(new JarFile(z), root);
    }

    class CopyInputStream implements Runnable {

        InputStream in;

        OutputStream out;

        Thread t;

        /**
		 * Constructor CopyInputStream.
		 * @param in
		 * @param out
		 */
        CopyInputStream(InputStream in, OutputStream out) {
            this.in = in;
            this.out = out;
            init();
        }

        synchronized void init() {
            t = new Thread(this);
            t.start();
        }

        /**
		 * @see java.lang.Runnable#run()
		 */
        public void run() {
            try {
                org.softnetwork.log.Log4jConnector.getConsole().debug("running");
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
                in.close();
                out.close();
            } catch (IOException io) {
            }
        }
    }

    /**
	 * Method addZipEntry.
	 * @param zip
	 * @param zipFile
	 * @param filter
	 * @param dir
	 * @throws IOException
	 */
    void addZipEntry(ZipOutputStream zip, File zipFile, java.io.FileFilter filter, String dir) throws IOException {
        String name = "";
        if (dir != null) name += dir;
        name += zipFile.getName();
        if (zipFile.isDirectory()) {
            name += "/";
        }
        addZipEntry(zip, name, zipFile);
        if (zipFile.isDirectory()) {
            File[] zipFiles = listFiles(zipFile, false, filter);
            for (int i = 0; i < zipFiles.length; i++) {
                addZipEntry(zip, zipFiles[i], filter, name);
            }
            File[] dirs = listDirectories(zipFile, false);
            for (int i = 0; i < dirs.length; i++) {
                addZipEntry(zip, dirs[i], filter, name);
            }
        }
    }

    /**
	 * Method addJarEntry.
	 * @param jar
	 * @param jarFile
	 * @param filter
	 * @param dir
	 * @throws IOException
	 */
    void addJarEntry(JarOutputStream jar, File jarFile, java.io.FileFilter filter, String dir) throws IOException {
        String name = "";
        if (dir != null) name += dir;
        name += jarFile.getName();
        if (jarFile.isDirectory()) {
            name += "/";
        }
        addJarEntry(jar, name, jarFile);
        if (jarFile.isDirectory()) {
            File[] jarFiles = listFiles(jarFile, false, filter);
            for (int i = 0; i < jarFiles.length; i++) {
                addJarEntry(jar, jarFiles[i], filter, name);
            }
            File[] dirs = listDirectories(jarFile, false);
            for (int i = 0; i < dirs.length; i++) {
                addJarEntry(jar, dirs[i], filter, name);
            }
        }
    }

    /**
	 * Method getFiles.
	 * @param dir
	 * @param filter
	 * @param recursive
	 * @return File[]
	 */
    File[] getFiles(File root, List files, java.io.FileFilter filter, boolean recursive) {
        if (root.isDirectory()) {
            files.addAll(Arrays.asList(root.listFiles(filter)));
            if (recursive) {
                File[] dirs = listDirectories(root, false);
                for (int i = 0; i < dirs.length; i++) {
                    getFiles(dirs[i], files, filter, recursive);
                }
            }
        }
        return (File[]) files.toArray(new File[0]);
    }
}
