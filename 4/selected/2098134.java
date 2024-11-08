package com.hitao.codegen.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * File Utilities class<br>
 * Utility methods to perform various file operations. At this point all public
 * methods are static and final.<br>
 *
 * @author zhangjun.ht
 * @created 2010-11-10
 * @version $Id: FileUtils.java 28 2011-02-24 09:41:20Z guest $
 */
public final class FileUtils {

    static final Logger logger_ = Logger.getLogger(FileUtils.class);

    /**
	 * Buffer size for Buffered objects to use during file ops. This number was
	 * determined to work well for files. I can only venture a guess as to why
	 * it worked well, most likely due to page or block sizes, whatever. YMMV.
	 * DMP
	 */
    public static final int INT_BUFFER_SIZE = 264440;

    private FileUtils() {
    }

    /**
	 * Returns the file extension associated with the specified file. The
	 * separator character will not be included.
	 *
	 * @param f
	 *            File the file whose extension is to be returned
	 * @return String the file extension associated with <code>f</code>
	 */
    public static String getExtension(File f) {
        return getExtension(f, false);
    }

    /**
	 * Returns the file extension associated with the specified file.
	 *
	 * @param f
	 *            File the file whose extension is to be returned
	 * @param includeSeparator
	 *            boolean <code>true</code> if the returned extension should
	 *            include the separator character; <code>false</code> if this
	 *            character should be omitted
	 * @return String the file extension associated with <code>f</code>
	 */
    public static String getExtension(File f, boolean includeSeparator) {
        return getExtension(f.getName(), includeSeparator);
    }

    /**
	 * Returns the file extension associated with the specified file name. The
	 * separator character will not be included.
	 *
	 * @param filename
	 *            String the file name whose extension is to be returned
	 * @return String the file extension associated with <code>f</code>
	 */
    public static String getExtension(String filename) {
        return getExtension(filename, false);
    }

    /**
	 * Returns the file extension associated with the specified file name.
	 *
	 * @param filename
	 *            String the file name whose extension is to be returned
	 * @param includeSeparator
	 *            boolean <code>true</code> if the returned extension should
	 *            include the separator character; <code>false</code> if this
	 *            character should be omitted
	 * @return String the file extension associated with <code>filename</code>
	 */
    public static String getExtension(String filename, boolean includeSeparator) {
        String ext = "";
        filename = ("./".equals(StringUtils.left(filename, 2))) ? filename.substring(2) : filename;
        int i = filename.lastIndexOf('.');
        if ((i > -1) && (i < (filename.length() - 1))) {
            int firstIdx = (includeSeparator) ? i : i + 1;
            ext = filename.substring(firstIdx).toLowerCase();
        }
        return ext;
    }

    /**
	 * Returns the name of the specified file sans any file extension suffix it
	 * includes.
	 *
	 * @param f
	 *            File the file from which the root name is to be extracted
	 * @return String the root file name of <code>f</code>
	 */
    public static String getRootName(File f) {
        return getRootName(f.getName());
    }

    /**
	 * Returns the specified file name sans any file extension suffix it
	 * includes.
	 *
	 * @param filename
	 *            String the file name from which the root name is to be
	 *            extracted
	 * @return String the root file name of <code>filename</code>
	 */
    public static String getRootName(String filename) {
        String rootName = filename;
        filename = (".".equals(StringUtils.left(filename, 1))) ? filename.substring(1) : filename;
        int i = filename.lastIndexOf('.');
        if ((i >= 0) && (i < filename.length() - 1)) {
            rootName = filename.substring(0, i);
        }
        return rootName;
    }

    /**
	 * Gets a (buffered) reader for the named resource file. The file is looked
	 * up through the Java resource loading mechanism.
	 *
	 * @param argResourceName
	 *            the file to open
	 * @return a reader to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    public static Reader getResourceReader(String argResourceName) throws FileNotFoundException {
        return getFileReader(argResourceName, true);
    }

    /**
	 * Gets a (buffered) input stream for the named resource file. The file is
	 * looked up through the Java resource loading mechanism.
	 *
	 * @param argResourceName
	 *            the file to open
	 * @return an input stream to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    public static InputStream getResourceStream(String argResourceName) throws FileNotFoundException {
        return getInputStream(argResourceName, true);
    }

    /**
	 * Gets a (buffered) reader for the named file.
	 *
	 * @param argFileName
	 *            the name of the file to open
	 * @return a reader to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    public static Reader getFileReader(String argFileName) throws FileNotFoundException {
        return getFileReader(argFileName, false);
    }

    /**
	 * Gets a (buffered) writer for the file.
	 *
	 * @param argFile
	 *            the file to open
	 * @return a writer to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static Writer getFileWriter(File argFile) throws IOException {
        return getFileWriter(argFile, false);
    }

    /**
	 * Gets a (buffered) writer for the named file.
	 *
	 * @param argFileName
	 *            the name of the file to open
	 * @return a writer to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static Writer getFileWriter(String argFileName) throws IOException {
        return getFileWriter(argFileName, false);
    }

    /**
	 * Gets a (buffered) writer for the named file.
	 *
	 * @param argFileName
	 *            the name of the file to open
	 * @param argAppend
	 *            should the contents of the file be replaced
	 * @return a writer to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static Writer getFileWriter(String argFileName, boolean argAppend) throws IOException {
        return getFileWriter(new File(argFileName), argAppend);
    }

    /**
	 * Gets a (buffered) writer for the file.
	 *
	 * @param argFile
	 *            the file to open
	 * @param argAppend
	 *            should the contents of the file be replaced
	 * @return a writer to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static Writer getFileWriter(File argFile, boolean argAppend) throws IOException {
        return new BufferedWriter(new FileWriter(argFile, argAppend), INT_BUFFER_SIZE);
    }

    /**
	 * Gets a (non buffered) writer for the file.
	 *
	 * @param argFile
	 *            the file to open
	 * @return a writer to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static Writer getNonBufferedFileWriter(File argFile) throws IOException {
        return new FileWriter(argFile, false);
    }

    /**
	 * Gets a (non buffered) writer for the file.
	 *
	 * @param argFile
	 *            the file to open
	 * @param argAppend
	 *            should the contents of the file be replaced
	 * @return a writer to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static Writer getNonBufferedFileWriter(File argFile, boolean argAppend) throws IOException {
        return new FileWriter(argFile, argAppend);
    }

    /**
	 * Gets a (buffered) output stream for the named file.
	 *
	 * @param argFileName
	 *            the name of the file to open
	 * @return an output stream to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static OutputStream getOutputStream(String argFileName) throws IOException {
        return getOutputStream(new File(argFileName));
    }

    /**
	 * Gets a (buffered) output stream for the file.
	 *
	 * @param argFile
	 *            the name of the file to open
	 * @return an output stream to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static OutputStream getOutputStream(File argFile) throws IOException {
        return getOutputStream(argFile, false);
    }

    /**
	 * Gets a (buffered) output stream for the named file.
	 *
	 * @param argFileName
	 *            the name of the file to open
	 * @param argAppend
	 *            should the contents of the file be replaced
	 * @return an output stream to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static OutputStream getOutputStream(String argFileName, boolean argAppend) throws IOException {
        return getOutputStream(new File(argFileName), argAppend);
    }

    /**
	 * Gets a (buffered) output stream for the file.
	 *
	 * @param argFile
	 *            the file to open
	 * @param argAppend
	 *            should the contents of the file be replaced
	 * @return an output stream to the opened file
	 * @throws IOException
	 *             if the file cannot be written to
	 */
    public static OutputStream getOutputStream(File argFile, boolean argAppend) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(argFile, argAppend));
    }

    /**
	 * Determines if the named resource exists in the class path or can be
	 * located via any other installed class loader.
	 *
	 * @param argResourceName
	 *            the name of the resource to check
	 * @return <code>true</code> if the resource can be located, otherwise
	 *         <code>false</code>
	 */
    public static boolean isValidResource(String argResourceName) {
        try {
            InputStream stream = getResourceStream(argResourceName);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ex) {
                    logger_.debug("CAUGHT EXCEPTION", ex);
                }
                return true;
            }
            return false;
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
	 * Determines if the named file exists.
	 *
	 * @param argFileName
	 *            the name of the file to locate
	 * @return <code>true</code> if the file can be located, otherwise
	 *         <code>false</code>
	 */
    public static boolean isValidFile(String argFileName) {
        try {
            Reader reader = getFileReader(argFileName);
            return (reader != null);
        } catch (FileNotFoundException e) {
            return false;
        }
    }

    /**
	 * Determines if the file is not null and exists.
	 *
	 * @param argFile
	 *            the file to check
	 * @return <code>true</code> if <code>argFile</code> is non-
	 *         <code>null</code> and exists, otherwise <code>false</code>
	 */
    public static boolean isValidFile(File argFile) {
        return (argFile != null) ? argFile.exists() : false;
    }

    /**
	 * Enable returning of a (buffered) reader for the named file. The file is
	 * either looked up through the Java resource loading mechanism or through
	 * the file loading one, based on the boolean passed to this method.
	 *
	 * @param argFileName
	 *            the file to open
	 * @param asResource
	 *            whether or not the file is a system resource loaded via the
	 *            class path
	 * @return a reader to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    private static Reader getFileReader(String argFileName, boolean asResource) throws FileNotFoundException {
        return new InputStreamReader(getInputStream(argFileName, asResource));
    }

    /**
	 * Enable returning of a (buffered) input stream for the named file. The
	 * file is either looked up through the Java resource loading mechanism or
	 * through the file loading one, based on the boolean passed to this method.
	 *
	 * @param argFileName
	 *            the file to open
	 * @param argIsResource
	 *            whether or not the file is a system resource loaded via the
	 *            class path
	 * @return an input stream to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    public static InputStream getInputStream(String argFileName, boolean argIsResource) throws FileNotFoundException {
        InputStream stream = null;
        if (argIsResource) {
            stream = FileUtils.class.getClassLoader().getResourceAsStream(argFileName);
        } else {
            File file = new File(argFileName);
            if (!file.exists()) {
                throw new FileNotFoundException("Unable to find: " + argFileName);
            }
            stream = new FileInputStream(file);
        }
        if (stream == null) {
            throw new FileNotFoundException("Unable to find: " + argFileName);
        }
        return new BufferedInputStream(stream, INT_BUFFER_SIZE);
    }

    /**
	 * Enable returning of a (buffered) input stream for the named file.
	 *
	 * @param argFile
	 *            the file to open
	 * @return an input stream to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    public static InputStream getInputStream(File argFile) throws FileNotFoundException {
        if (!argFile.exists()) {
            throw new FileNotFoundException("Unable to find: " + argFile.getAbsolutePath());
        }
        InputStream in = new FileInputStream(argFile);
        in = new BufferedInputStream(in, INT_BUFFER_SIZE);
        return in;
    }

    /**
	 * Enable returning of a (buffered) reader for the file.
	 *
	 * @param argFile
	 *            the file to open
	 * @return a reader to the opened file
	 * @throws FileNotFoundException
	 *             if the file is not found
	 */
    public static BufferedReader getFileReader(File argFile) throws FileNotFoundException {
        if (!argFile.exists()) {
            throw new FileNotFoundException("Unable to find: " + argFile.getAbsolutePath());
        }
        Reader reader = new FileReader(argFile);
        return new BufferedReader(reader, INT_BUFFER_SIZE);
    }

    /**
	 * Loads a file and returns a string. Buffered for performance.
	 *
	 * @param argFileName
	 *            Name of file to Load.
	 * @return String contents of file.
	 * @throws FileNotFoundException
	 *             if the file was not found.
	 * @throws IOException
	 *             if there was a problem closing the file.
	 * @author DMP
	 * @created 12/15/2002 8:11:36 PM
	 */
    public static final String loadFile(String argFileName) throws FileNotFoundException, IOException {
        Reader reader = getFileReader(argFileName);
        return loadFile(reader);
    }

    /**
	 * Returns a string consisting of every line in the specified file,
	 * separated by platform- appropriate newline characters.
	 *
	 * @param argFileName
	 *            the name of the file whose contents to return
	 * @return the contents of <code>codeargFileName</code>, separated by
	 *         newline characters
	 * @throws IOException
	 *             if any exceptions occur in parsing the contents of
	 *             <code>argFileName</code>
	 */
    public static final String loadFileLines(String argFileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(argFileName));
        try {
            String fileLine = reader.readLine();
            while (fileLine != null) {
                sb.append(fileLine);
                sb.append(StringUtils.NEW_LINE);
                fileLine = reader.readLine();
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    /**
	 * Gets the contents of the named resource. The resource is loaded using the
	 * class path or other installed class loader.
	 *
	 * @param argFileName
	 *            the name of the resource
	 * @return the contents of the resource
	 * @throws FileNotFoundException
	 *             if the resource cannot be located
	 * @throws IOException
	 *             if there are problems reading the contents of the resource
	 */
    public static final String loadResource(String argFileName) throws FileNotFoundException, IOException {
        Reader reader = getResourceReader(argFileName);
        return loadFile(reader);
    }

    /**
	 * Gets the contents of the file.
	 *
	 * @param argFile
	 *            the file to load
	 * @return the contents of the file
	 * @throws FileNotFoundException
	 *             if the file cannot be located
	 * @throws IOException
	 *             if there are problems reading the contents of the file
	 */
    public static final String loadFile(File argFile) throws FileNotFoundException, IOException {
        Reader reader = getFileReader(argFile);
        return loadFile(reader);
    }

    private static final String loadFile(Reader argReader) throws IOException {
        StringWriter sw = new StringWriter();
        int c = -1;
        try {
            while ((c = argReader.read()) != -1) {
                sw.write(c);
            }
        } catch (IOException ex) {
            logger_.info("CAUGHT EXCEPTION", ex);
        }
        argReader.close();
        return sw.getBuffer().toString();
    }

    /**
	 * Writes <code>argContents</code> as the contents of
	 * <code>argFilename</code>, replacing any previous contents, or creating
	 * the file if necessary.
	 *
	 * @param argFilename
	 *            the name of the file to write
	 * @param argContents
	 *            the contents to write
	 * @throws IOException
	 *             if there are problems writing the file
	 */
    public static final void writeFile(String argFilename, String argContents) throws IOException {
        Writer w = getFileWriter(argFilename);
        writeFile(w, argContents);
    }

    /**
	 * Writes <code>argContents</code> as the contents of <code>argFile</code>,
	 * replacing any previous contents, or creating the file if necessary.
	 *
	 * @param argFile
	 *            the file to write
	 * @param argContents
	 *            the contents to write
	 * @throws IOException
	 *             if there are problems writing the file
	 */
    public static final void writeFile(File argFile, String argContents) throws IOException {
        Writer w = getFileWriter(argFile);
        writeFile(w, argContents);
    }

    private static final void writeFile(Writer argWriter, String argContents) throws IOException {
        argWriter.write(argContents);
        argWriter.close();
    }

    /**
	 * Ensures that a file and it's parent directory hierarchy exists.
	 *
	 * @param argFile
	 *            File the file to check for existance and create if necessary
	 * @throws IOException
	 *             if unable to create any level of the path
	 */
    public static final void ensureFileExists(File argFile) throws IOException {
        if (!argFile.exists()) {
            argFile.getParentFile().mkdirs();
            argFile.createNewFile();
        }
    }

    /**
	 * Deletes a directory and all files underneath it.
	 *
	 * @param argDir
	 *            The directory to remove.
	 */
    public static void deleteTree(File argDir) {
        File[] list = argDir.listFiles();
        for (File element : list) {
            if (element.isDirectory()) {
                deleteTree(element);
            } else {
                element.delete();
            }
        }
        argDir.delete();
    }

    /**
	 * Schedules <code>argFile</code> to be deleted asynchronously after
	 * <code>argDelayMillis</code> milliseconds. If the file cannot be deleted
	 * after <code>argDelayMillis</code> milliseconds, the file is scheduled to
	 * be deleted when the JVM exits using {@link java.io.File#deleteOnExit()}.
	 *
	 * @param argFile
	 *            the file to delete
	 * @param argDelayMillis
	 *            the number of milliseconds to wait before deleting
	 */
    public static final void deleteFileWithDelay(final File argFile, final int argDelayMillis) {
        if (argFile == null) {
            throw new NullPointerException();
        }
        Thread deleteWorker = new Thread() {

            @Override
            public void run() {
                try {
                    sleep(argDelayMillis);
                } catch (InterruptedException ex) {
                    logger_.debug("CAUGHT EXCEPTION", ex);
                }
                boolean success = false;
                try {
                    success = argFile.delete();
                } catch (Exception ex) {
                    logger_.error("CAUGHT EXCEPTION with '" + argFile.getAbsolutePath() + "'", ex);
                }
                if (!success) {
                    argFile.deleteOnExit();
                }
            }
        };
        deleteWorker.setDaemon(true);
        deleteWorker.start();
    }

    /**
	 * Find all files beneath the provided file (directory) that utilize the
	 * specified extension.
	 *
	 * @param argFile
	 *            Directory under which to search.
	 * @param argExtension
	 *            Extension that the returned list of files shall use.
	 * @return List of files conforming to the specifications.
	 */
    public static final List<File> getFiles(File argFile, String argExtension) {
        return getFiles(argFile, argExtension, null);
    }

    /**
	 * Find all files beneath the provided file (directory) that utilize the
	 * specified extension.
	 *
	 * @param argFile
	 *            Directory under which to search.
	 * @param argExtension
	 *            Extension that the returned list of files shall use.
	 * @param 
	 * 			  argNameFilter the file name filter
	 * @return List of files conforming to the specifications.
	 */
    public static final List<File> getFiles(File argFile, String argExtension, FilenameFilter argNameFilter) {
        List<File> result = new ArrayList<File>();
        if (argFile.isDirectory()) {
            for (File file : argFile.listFiles(argNameFilter)) {
                if (file.isDirectory()) {
                    List<File> subFiles = getFiles(file, argExtension, argNameFilter);
                    if (subFiles != null) {
                        result.addAll(subFiles);
                    }
                } else if (file.getName().endsWith(argExtension)) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    public static final URL[] getUrls(File argFile, String argExtension) {
        return getUrls(argFile, argExtension, null);
    }

    public static final URL[] getUrls(File argFile, String argExtension, FilenameFilter argFilenameFilter) {
        List<File> files = getFiles(argFile, argExtension, argFilenameFilter);
        URL[] urls = new URL[files.size()];
        try {
            for (int i = 0; i < files.size(); i++) {
                urls[i] = files.get(i).toURI().toURL();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return urls;
    }

    /**
	 * Copies <tt>src</tt> as <tt>dst</tt>.
	 *
	 * @param argSource
	 *            the source file
	 * @param argDestination
	 *            the destination file
	 * @throws IOException
	 *             if unable to complete the file copy
	 */
    public static final void copyFile(File argSource, File argDestination) throws IOException {
        FileChannel srcChannel = new FileInputStream(argSource).getChannel();
        FileChannel dstChannel = new FileOutputStream(argDestination).getChannel();
        try {
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } finally {
            srcChannel.close();
            dstChannel.close();
        }
    }

    /**
	 * Creates a temporary directory. This is similar to
	 * {@link File#createTempFile(String, String)} except that it creates a
	 * directory instead of a file.
	 *
	 * @return a new temporary directory
	 * @throws IOException
	 *             if unable to create the directory
	 */
    public static final File createTempDir() throws IOException {
        File f = File.createTempFile(".dir", "tmp");
        f.delete();
        f.mkdir();
        return f;
    }

    /**
	 * Attempts to convert the given URL to a valid File reference.
	 *
	 * @param argUrl
	 * @return File
	 */
    public static final File getFileForURL(URL argUrl) {
        try {
            return new File(java.net.URLDecoder.decode(argUrl.getFile(), "UTF-8"));
        } catch (Exception ee) {
            logger_.error("An exception occurred while decoding URL: " + argUrl, ee);
            return null;
        }
    }
}
