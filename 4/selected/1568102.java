package com.comarch.depth.helper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import com.comarch.depth.share.helper.DocumentHelper;

public class OutputHelper {

    public static final String XHTML_DTD_PUBLIC_TRANS = "-//W3C//DTD XHTML 1.0 Transitional//EN";

    public static final String XHTML_DTD_SYSTEM_TRANS = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";

    public static final String XHTML_DTD_PUBLIC_FRAME = "-//W3C//DTD XHTML 1.0 Frameset//EN";

    public static final String XHTML_DTD_SYSTEM_FRAME = "http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd";

    public static final String DEPTH_DTD = "DTD/Depth Doc 0.1";

    public static void saveFile(InputStream from, IPath to) throws IOException {
        File toFile = to.toFile();
        if (toFile.isDirectory()) throw new IOException("File Save: destination is not a valid file name.");
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("File Save: " + "destination file is unwriteable: " + to.lastSegment());
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("File Save: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("File Save: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("File Save: " + "destination directory is unwriteable: " + parent);
        }
        OutputStream output = null;
        try {
            output = new BufferedOutputStream(new FileOutputStream(toFile));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) output.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
            }
            if (output != null) try {
                output.close();
            } catch (IOException e) {
            }
        }
    }

    public static void copyFile(IPath fromFileName, IPath toFileName) throws IOException {
        File fromFile = fromFileName.toFile();
        File toFile = toFileName.toFile();
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFileName);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFileName);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFileName);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        InputStream from = null;
        OutputStream to = null;
        try {
            from = new BufferedInputStream(new FileInputStream(fromFile));
            to = new BufferedOutputStream(new FileOutputStream(toFile));
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Save the xml document to the given path using the given DOCTYPE infos.
	 * @param doc
	 * @param path
	 * @param doctypePublic
	 * @param docTypeSystem
	 */
    public static void saveXMLDocument(Document doc, IPath path, String doctypePublic, String doctypeSystem) {
        try {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(path.toOSString()));
            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(out);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer serializer = tf.newTransformer();
            serializer.setOutputProperty(OutputKeys.METHOD, "xml");
            serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctypeSystem);
            serializer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctypePublic);
            serializer.transform(domSource, streamResult);
            out.flush();
            out.close();
        } catch (Exception e) {
            Logger.log(Status.ERROR, "Could not save xml document", e);
        }
    }

    /**
	 * Save the xml document to the given path. The default DOCTYPE is transitional xhtml.
	 * 
	 * @param doc
	 * @param path
	 */
    public static void saveXMLDocument(Document doc, IPath path) {
        saveXMLDocument(doc, path, XHTML_DTD_PUBLIC_TRANS, XHTML_DTD_SYSTEM_TRANS);
    }

    /**
	 * Returns the full path of the given File located in com.comarch.depth.files.
	 * @param entryPoint
	 * @return
	 */
    public static IPath getAbsolutePath(String entryPoint) {
        return getAbsolutePath("com.comarch.depth.files", entryPoint);
    }

    /**
	 * Returns the full path of the given file in the context of the given plugin.
	 * @param plugin
	 * @param entryPoint
	 * @return
	 */
    public static IPath getAbsolutePath(String plugin, String entryPoint) {
        Bundle bundle = Platform.getBundle(plugin);
        try {
            URL url = FileLocator.resolve(bundle.getEntry(entryPoint));
            return new Path(url.getFile());
        } catch (Exception e) {
            Logger.log(Status.ERROR, "Could not resolve bundle.", e);
        }
        return null;
    }

    /**
	 * Removes a given directory and all containing files and directories
	 * 
	 * @param directory
	 *            The directory to delete
	 */
    public static void deleteDirecotryRecursive(File directory) {
        if (directory.isDirectory()) {
            String[] filenames = directory.list();
            for (String filename : filenames) {
                File file = new File(directory + "/" + filename);
                if (file.isDirectory()) {
                    deleteDirecotryRecursive(file);
                    file.delete();
                    continue;
                }
                if (file.isFile()) {
                    file.delete();
                    continue;
                }
            }
            directory.delete();
        }
    }

    /**
	 * This callback should stay here because so it can still be used by the export plugins without having to have an
	 * explicit dependency to the share plugin
	 * @return
	 * @throws ParserConfigurationException
	 */
    public static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
        return DocumentHelper.getDocumentBuilder();
    }
}
