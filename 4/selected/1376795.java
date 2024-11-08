package com.webstersmalley.picweb.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Date;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Utility class for the basic file/IO related functions (eg write out an
 * {@link org.w3c.dom.Document} as an XML file)
 * 
 * @author Matthew Smalley
 */
public class IOUtils {

    /** Logger for the class. */
    private static Logger log = Logger.getLogger(IOUtils.class);

    /**
     * Private constructor - should only used via static utilities methods
     */
    private IOUtils() {
    }

    /**
     * Writes out a string as a file. Will create parent directories in order to
     * succeed.
     * 
     * @param filename
     *            the name of the new file
     * @param contents
     *            the contents of the new file
     * @throws FileNotFoundException
     *             File wasn't found
     */
    public static void writeFile(String filename, String contents) throws FileNotFoundException {
        File output = new File(filename);
        File parent = output.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        PrintWriter out = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(output))));
        out.println(contents);
        out.flush();
        out.close();
    }

    /**
     * Writes out a {@link org.w3c.dom.Document} as an XML file.
     * 
     * @param filename
     *            the filename of the new XML file
     * @param doc
     *            the document to write
     * @throws Exception
     *             wraps up exceptions from the IO and the transformation
     */
    public static void writeXmlFile(String filename, Document doc) throws Exception {
        try {
            Source source = new DOMSource(doc);
            File file = new File(filename);
            FileOutputStream fos = new FileOutputStream(file);
            StreamResult result = new StreamResult(fos);
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            throw new Exception("Error - unable to write XML File: " + e.toString(), e);
        }
    }

    /**
     * Copies the contents of one folder into another. e.g. consider the
     * following tree: <code>
     * src\file1
     * src\dir1\file2
     * src\dir1\file3
     * </code> If
     * we called copyFolderContents("src", "dest") we'd end up with: <code> 
     * dest\file1
     * dest\dir1\file2
     * dest\dir2\file3
     * </code>
     * 
     * @param src
     *            Source folder
     * @param dest
     *            Destination folder
     * @throws IOException
     */
    public static void copyFolderContents(String src, String dest) throws IOException {
        log.debug("Copying contents of: '" + src + "' to '" + dest + "'");
        File srcDir = new File(src);
        File destDir = new File(dest);
        destDir.mkdirs();
        File[] children = srcDir.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            String destinationChild = destDir.getAbsolutePath() + File.separator + child.getName();
            if (child.isDirectory()) {
                copyFolderContents(child.getAbsolutePath(), destinationChild);
            } else {
                copyFile(child, new File(destinationChild));
            }
        }
    }

    /**
     * Copies the contents of a file to a new one, creating subdirectories as
     * necessary.
     * 
     * @param src
     *            Source file
     * @param dest
     *            Destination file
     * @throws IOException
     */
    public static void copyFile(File src, File dest) throws IOException {
        log.debug("Copying file: '" + src + "' to '" + dest + "'");
        FileChannel srcChannel = new FileInputStream(src).getChannel();
        FileChannel dstChannel = new FileOutputStream(dest).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    /**
     * Deletes the contents of a folder, leaving that folder intact.
     * 
     * @param folder
     *            the folder to purge
     * @throws IOException
     */
    public static void purgeFolder(File folder) throws IOException {
        File[] children = folder.listFiles();
        for (int i = 0; i < children.length; i++) {
            File child = children[i];
            if (child.isDirectory()) {
                purgeFolder(child);
                log.debug("Deleting: " + child.getAbsolutePath());
                if (!child.delete()) {
                    throw new IOException("Error - couldn't delete: " + child.getAbsolutePath());
                }
            } else {
                log.debug("Deleting: " + child.getAbsolutePath());
                if (!child.delete()) {
                    throw new IOException("Error - couldn't delete: " + child.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Creates a blank (0-byte) file, creating folders as necessary. If the file
     * already exists, updates the lastModified time. If the file exists but is
     * not a regular file, throws an IOException.
     * 
     * @param file
     *            the file to create
     * @throws IOException
     */
    public static void touch(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("Error touching file " + file.getAbsolutePath() + ". Already exists as a directory.");
            } else {
                file.setLastModified(new Date().getTime());
            }
        } else {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
    }

    /**
     * Copies a resource from the classpath to a specified filename.
     * 
     * @param resourcePath
     *            source classpath of the resource.
     * @param output
     *            output filename
     * @throws IOException
     *             if there's a problem
     */
    public static void copyFromClassPath(final String resourcePath, final String output) throws IOException {
        log.debug("right...");
        log.debug(resourcePath);
        log.debug(IOUtils.class);
        log.debug(IOUtils.class.getResource(resourcePath));
        InputStream is = IOUtils.class.getResource(resourcePath).openStream();
        File outputFile = new File(output);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        OutputStream os = new BufferedOutputStream(new FileOutputStream(output));
        int c;
        while ((c = is.read()) != -1) {
            os.write(c);
        }
        is.close();
        os.close();
    }
}
