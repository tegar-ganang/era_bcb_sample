package org.benetech.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.benetech.event.EventListener;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;
import org.xml.sax.ErrorHandler;

/**
 * File utilities.
 *
 * @author Reuben Firmin
 */
public final class FileUtils {

    public static final char SLASH = File.separatorChar;

    private static File TMP_DIR;

    /** Non constructor. */
    private FileUtils() {
    }

    /**
	 * Get a section of a file.
	 *
	 * @param beginLine The line number to begin at, 0 based
	 * @param endLine The line number to end at, 0 based
	 * @param file The file
	 * @return Never null
	 * @throws IOException If file access fails
	 */
    public static String[] getLinesFromFile(final int beginLine, final int endLine, final File file) throws IOException {
        if (file != null && file.exists()) {
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            try {
                int lineNum = 0;
                String line;
                final List<String> lines = new ArrayList<String>();
                while ((line = reader.readLine()) != null) {
                    if (lineNum >= beginLine && lineNum <= endLine) {
                        lines.add(line);
                    }
                    lineNum++;
                }
                return lines.toArray(new String[lines.size()]);
            } finally {
                reader.close();
            }
        } else {
            return new String[0];
        }
    }

    /**
	 * Get the contents of the file as an array of strings, one line per array element.
	 *
	 * @param file The file to retrieve
	 * @return Never null
	 * @throws IOException If file access fails.
	 */
    public static String[] getFileAsStrings(final File file) throws IOException {
        final String delim = "\n\n\n";
        final String fileContents = getFileAsString(file, delim);
        return fileContents.split(delim);
    }

    /**
	 * Return a file as a string, with an optional delimiter between lines.
	 *
	 * @param file The file to read
	 * @param delim The delimiter to use, can be null for none
	 * @return The file as string, null if file not found
	 * @throws IOException If file access fails
	 */
    public static String getFileAsString(final File file, final String delim) throws IOException {
        if (file != null && file.exists()) {
            final BufferedReader reader = new BufferedReader(new FileReader(file));
            final StringBuffer out = new StringBuffer();
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line);
                    if (delim != null) {
                        out.append(delim);
                    }
                }
            } finally {
                reader.close();
            }
            return out.toString();
        } else {
            return null;
        }
    }

    /**
	 * Copy a file from a zipentry to a location under a given tmp directory. Full path information will be retained.
	 *
	 * @param zip The zip file to read from
	 * @param zipEntry The zip entry to read
	 * @param tmpPath The temporary directory, already existing
	 * @return Never null
	 * @throws IOException If file access fails
	 */
    public static File copyFileFromZip(final ZipFile zip, final ZipEntry zipEntry, final File tmpPath) throws IOException {
        OutputStream os = null;
        InputStream is = null;
        try {
            int folderSep = zipEntry.getName().lastIndexOf("/");
            if (folderSep < 0) {
                folderSep = zipEntry.getName().lastIndexOf("\\");
            }
            if (folderSep > 0) {
                final File entryPath = new File(tmpPath.getAbsolutePath() + File.separatorChar + zipEntry.getName().substring(0, folderSep) + File.separatorChar);
                entryPath.mkdirs();
            }
            final File out = new File(tmpPath.getAbsolutePath() + File.separatorChar + zipEntry.getName());
            if (!out.isDirectory()) {
                os = new FileOutputStream(out);
                final byte[] entryBytes = new byte[(int) zipEntry.getSize()];
                is = zip.getInputStream(zipEntry);
                int readSize = 0;
                while ((readSize = is.read(entryBytes)) > 0) {
                    os.write(entryBytes, 0, readSize);
                }
                os.flush();
            }
            return out;
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    /**
	 * Change the encoding of the given file. Results will be unpredictable if the file is binary.
	 *
	 * @param file The file to change the encoding of
	 * @param charSet The character set to use instead
	 * @throws IOException If file access fails XXX needs work
	 */
    public static void changeEncoding(final File file, final Charset charSet) throws IOException {
        final String fileRep = getFileAsString(file, null);
        final ByteBuffer buffer = charSet.newEncoder().encode(CharBuffer.wrap(fileRep));
        writeToFile(file, buffer.array());
    }

    /**
	 * Erases the existing contents of the file, and writes the given bytes to it.
	 *
	 * @param file The file to write to; does not have to exist
	 * @param contents The contents to write to it
	 * @throws IOException If file access fails
	 */
    public static void writeToFile(final File file, final byte[]... contents) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        final FileOutputStream fos = new FileOutputStream(file);
        for (byte[] contentChunk : contents) {
            fos.write(contentChunk);
        }
        fos.flush();
        fos.close();
    }

    /**
	 * Write bytes to a file, inserting newline every linelength bytes. Not efficient.
	 *
	 * @param file File to write to; does not have to exist
	 * @param lineLength The line length
	 * @param contents The contents to write to the file
	 * @throws IOException if file access fails
	 */
    public static void writeToFile(final File file, final int lineLength, final byte[]... contents) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        final FileOutputStream fos = new FileOutputStream(file);
        for (byte[] contentChunk : contents) {
            for (int i = 0; i < contentChunk.length; i++) {
                fos.write(contentChunk[i]);
                if (i % lineLength == 0) {
                    fos.write('\n');
                }
            }
        }
        fos.flush();
        fos.close();
    }

    /**
	 * Appends to the end of a file.
	 *
	 * @param file The file to write to; does not have to exist
	 * @param contents The contents to write to it
	 * @throws IOException If file access fails
	 */
    public static void appendToFile(final File file, final byte[]... contents) throws IOException {
        final FileOutputStream fos = new FileOutputStream(file, true);
        for (byte[] contentChunk : contents) {
            fos.write(contentChunk);
        }
        fos.flush();
        fos.close();
    }

    /**
	 * Erases the existing contents of the file, and writes the given bytes to it. Useful only for non-binary files.
	 *
	 * @param file The file to write to; does not have to exist
	 * @param contents The contents to write to it
	 * @param charSet The charset to write
	 * @throws IOException If file access fails
	 */
    public static void writeToFile(final File file, final byte[] contents, final Charset charSet) throws IOException {
        if (file.exists()) {
            file.delete();
        }
        final OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(file), charSet);
        final ByteBuffer byteBuffer = ByteBuffer.wrap(contents);
        for (int i = 0; i < byteBuffer.limit(); i++) {
            osw.write(byteBuffer.get(i));
        }
        osw.flush();
        osw.close();
    }

    /**
	 * Copy a given file to another file.
	 *
	 * @param fromFile The file to copy
	 * @param toFile The file to create
	 * @throws IOException If file access fails
	 */
    public static void copyFile(final File fromFile, final File toFile) throws IOException {
        org.apache.commons.io.FileUtils.copyFile(fromFile, toFile);
    }

    /**
	 * Copy a file to a directory location.
	 *
	 * @param fromFile The file to copy
	 * @param toDir The directory to copy to
	 * @throws IOException If file access fails
	 * @return The new file
	 */
    public static File copyFileToDirectory(final File fromFile, final File toDir) throws IOException {
        org.apache.commons.io.FileUtils.copyFileToDirectory(fromFile, toDir);
        return new File(toDir.getAbsolutePath() + File.separator + fromFile.getName());
    }

    /**
	 * Copy a file to a location under a directory, using the file's relative position under the specified folder to
	 * generate the folder structure in the new directory.
	 *
	 * @param fromFile The file to copy
	 * @param toDir The directory to copy to
	 * @param relativeDir An ancestor of the file being copied, the relative path of which is used to construct the new
	 *            folder structure
	 * @throws IOException If file access fails
	 */
    public static void copyFileToDirectory(final File fromFile, final File relativeDir, final File toDir) throws IOException {
        final String relativePath = getRelativePath(relativeDir, fromFile);
        if (relativePath == null || relativePath.length() == 0) {
            copyFileToDirectory(fromFile, toDir);
        } else {
            final File outFile = new File(toDir.getAbsolutePath() + File.separator + relativePath + File.separator + fromFile.getName());
            copyFile(fromFile, outFile);
        }
    }

    /**
	 * Copy resources from a named directory somewhere on the classpath.
	 *
	 * @param classpathDir The directory on the classpath, e.g. "images"
	 * @param outputDir The directory to copy the contents of the directory to, must already exist.
	 * @throws IOException If file access fails.
	 */
    public static void copyResourcesFromClasspath(final String classpathDir, final File outputDir) throws IOException {
        final PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver();
        final File theDirectory = pmrpr.getResource("classpath:" + classpathDir).getFile();
        final File[] contents = theDirectory.listFiles();
        for (int i = 0; i < contents.length; i++) {
            FileUtils.copyFileToDirectory(contents[i], outputDir);
        }
    }

    /**
	 * Remove spaces and dots from the file name.
	 *
	 * @param fileName never null
	 * @return never null
	 */
    public static String sanitizeFileName(final String fileName) {
        return fileName.replace('.', '_').replace(' ', '_');
    }

    /**
	 * Get the relative path of a file somewhere underneath a directory (i.e. further down in the tree) relative to that
	 * directory.
	 *
	 * @param directory The directory that's an ancestor of the file
	 * @param fileSomewhereUnderDirectory The file
	 * @return null if the directory is not an ancestor
	 */
    public static String getRelativePath(final File directory, final File fileSomewhereUnderDirectory) {
        if (!directory.isDirectory() || !directory.exists() || !fileSomewhereUnderDirectory.exists()) {
            return null;
        }
        final String dirPath = directory.getAbsolutePath();
        final String filePath = fileSomewhereUnderDirectory.getAbsolutePath();
        if (!filePath.startsWith(dirPath)) {
            return null;
        }
        final String difference = filePath.substring(dirPath.length());
        final int fileNameIndex = difference.lastIndexOf(File.separator);
        if (fileNameIndex > 0) {
            return difference.substring(0, fileNameIndex);
        }
        return "";
    }

    /**
	 * Find and replace a given string in a given file.
	 *
	 * @param file The file to look through.
	 * @param find The string to find; regex.
	 * @param replace The string to replace.
	 * @throws IOException if file access fails
	 */
    public static void findAndReplace(final File file, final String find, final String replace) throws IOException {
        final String string = getFileAsString(file, null);
        writeToFile(file, string.replaceAll(find, replace).getBytes());
    }

    /**
	 * Renames the file.
	 *
	 * @param file the file to be renamed
	 * @param rename the name to chagne the file to
	 * @return new File
	 * @throws IOException If file can't be oppened
	 */
    public static File renameFile(final File file, final String rename) throws IOException {
        final String oldName = file.getName();
        if (oldName.equals(rename)) {
            return file;
        }
        final String parent = file.getParent();
        final File renamedFile = new File(parent + File.separator + rename);
        FileUtils.copyFile(file, renamedFile);
        file.delete();
        return renamedFile;
    }

    /**
	 * Recursively delete a directory.
	 *
	 * @param directory Directory to delete
	 * @throws IOException in case deletion is unsuccessful.
	 */
    public static void deleteDirectory(final File directory) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(directory);
    }

    /**
	 * Get the system tmp dir.
	 *
	 * @return The tmp dir
	 * @throws IOException If tmp dir couldn't be determined
	 */
    public static File getTmpDir() throws IOException {
        if (TMP_DIR == null) {
            final File tmpFile = File.createTempFile("foo", "bah");
            TMP_DIR = tmpFile.getParentFile();
        }
        return TMP_DIR;
    }

    /**
	 * Finds files within a given directory (and optionally its subdirectories) which match an array of extensions.
	 *
	 * @param directory the directory to search in
	 * @param extensions an array of extensions, ex. {"java","xml"}. If this parameter is <code>null</code>, all
	 *            files are returned.
	 * @param recursive if true all subdirectories are searched as well
	 * @return List of java.io.File with the matching files
	 */
    public static List<File> listFiles(final File directory, final String[] extensions, final boolean recursive) {
        final Collection fileCollection = org.apache.commons.io.FileUtils.listFiles(directory, extensions, recursive);
        final List<File> files = new ArrayList<File>(fileCollection.size());
        for (final Object o : fileCollection) {
            files.add((File) o);
        }
        return files;
    }

    /**
	 * Validate an XML file against its DTD, inserting any violations into the error handler, and return the parsed
	 * document.
	 *
	 * @param file The file to parse and validate
	 * @param errorHandler The error handler to add violations to; null to turn off validation
	 * @param listener Event listener to provide feedback to the user
	 * @return never null
	 * @throws JDOMException if an error occurs during parsing
	 * @throws IOException if IO to the file breaks
	 */
    public static Document fileToDocument(final File file, final ErrorHandler errorHandler, final EventListener listener) throws JDOMException, IOException {
        final SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setValidation(errorHandler != null);
        saxBuilder.setEntityResolver(new SafetyNetResolver(listener));
        if (errorHandler instanceof ViolationCollatingErrorHandler) {
            ((ViolationCollatingErrorHandler) errorHandler).setErrorContext(file);
        }
        if (errorHandler != null) {
            saxBuilder.setErrorHandler(errorHandler);
        }
        listener.message("Validating: " + file.getName() + " against DTD (may need to download DTD from remote server)");
        return saxBuilder.build(file);
    }

    /**
	 * Save a given document to a given file (which will be wiped).
	 *
	 * @param document The document to save
	 * @param file The file to save to
	 * @param compactPrint boolean to set format: raw if false, compact if true
	 * @throws IOException if file access fails
	 */
    public static void saveDocumentToFile(final Document document, final File file, final boolean compactPrint) throws IOException {
        final Format format = compactPrint ? Format.getCompactFormat() : Format.getRawFormat();
        final XMLOutputter xmlOutputter = new XMLOutputter(format);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        final FileOutputStream fos = new FileOutputStream(file);
        final CharsetEncoder enc = Charset.forName("UTF8").newEncoder();
        final OutputStreamWriter outStream = new OutputStreamWriter(fos, enc);
        try {
            xmlOutputter.output(document, outStream);
            outStream.flush();
        } finally {
            outStream.close();
        }
    }

    /**
	 * Validate XML files against their DTDs, inserting any violations into the errorHandler, and return parsed list of
	 * documents, mapped against the file name.
	 *
	 * @param xmlFiles The xml files to parse and validate
	 * @param errorHandler The error handler to add violations to; can be null if validation is not required
	 * @param listener Event listener to provide feedback to the user
	 * @throws JDOMException if an error occurs during parsing
	 * @throws IOException if IO to the file breaks
	 * @return Never null.
	 */
    public static Map<File, Document> parseAndValidate(final List<File> xmlFiles, final ErrorHandler errorHandler, final EventListener listener) throws JDOMException, IOException {
        final Map<File, Document> xmlDocs = new HashMap<File, Document>();
        final SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setValidation(errorHandler != null);
        saxBuilder.setEntityResolver(new SafetyNetResolver(listener));
        if (errorHandler != null) {
            saxBuilder.setErrorHandler(errorHandler);
        }
        for (File xmlFile : xmlFiles) {
            if (errorHandler != null && errorHandler instanceof ViolationCollatingErrorHandler) {
                ((ViolationCollatingErrorHandler) errorHandler).setErrorContext(xmlFile);
            }
            listener.message("Validating: " + xmlFile.getName() + " against DTD (may need to download DTD " + "from remote server)");
            xmlDocs.put(xmlFile, saxBuilder.build(xmlFile));
        }
        return xmlDocs;
    }

    /**
	 * If needed, creates necessary set of nested directories.
	 *
	 * @param dir Proposed to directory.
	 * @return Directory after created.
	 */
    public static File mkdir(final File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        Assert.isTrue(dir.exists());
        return dir;
    }

    /**
	 * @param basePath Null not allowed.
	 * @param pathFragments One or more path fragments.
	 * @return What is generated by calls to {@link FilenameUtils#concat(String, String)}.
	 */
    public static String concat(final String basePath, final String... pathFragments) {
        Assert.hasText(basePath);
        if (ArrayUtils.isEmpty(pathFragments)) {
            return basePath;
        }
        String concat = basePath;
        for (final String fragment : pathFragments) {
            Assert.hasText(fragment);
            concat = FilenameUtils.concat(concat, fragment);
        }
        return concat;
    }

    /**
	 * Makes the directory indicated by {@link #concat(String, String[])}.
	 *
	 * @param basePath Null not allowed.
	 * @param pathFragments Zero or more.
	 * @return Made directory that is guaranteed to exist.
	 */
    public static File mkdir(final String basePath, final String... pathFragments) {
        return mkdir(concat(basePath, pathFragments));
    }

    /**
	 * @see #mkdir(String, String[])
	 */
    public static File mkdir(final File basePath, final String... pathFragments) {
        Assert.isTrue(basePath != null && basePath.exists() && basePath.isDirectory());
        return mkdir(concat(basePath.getAbsolutePath(), pathFragments));
    }
}
