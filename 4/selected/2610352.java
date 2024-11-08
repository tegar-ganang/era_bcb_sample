package net.sf.gsearch.internal.core;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 * @author allon moritz
 */
public class Utilities {

    /**
	 * Closes the given input stream.
	 * 
	 * @param is
	 */
    public static void close(InputStream is) {
        try {
            if (is != null) is.close();
        } catch (IOException e) {
        }
    }

    /**
	 * Closes the given output stream.
	 * 
	 * @param os
	 */
    public static void close(OutputStream os) {
        try {
            if (os != null) os.close();
        } catch (IOException e) {
        }
    }

    /**
	 * Closes the given reader.
	 * 
	 * @param reader
	 */
    public static void close(Reader reader) {
        try {
            if (reader != null) reader.close();
        } catch (IOException e) {
        }
    }

    /**
	 * Closes the given writer.
	 * 
	 * @param writer
	 */
    public static void close(BufferedWriter writer) {
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
        }
    }

    /**
	 * Returns the content of the given file as a byte array.
	 * 
	 * @param file
	 * @return the content
	 * @throws CoreException
	 */
    public static byte[] getBinaryContent(IFile file) throws CoreException {
        InputStreamReader in = null;
        BufferedInputStream bis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            file.refreshLocal(IResource.DEPTH_ZERO, null);
            bis = new BufferedInputStream(file.getContents());
            byte[] readBuffer = new byte[2048];
            int n = bis.read(readBuffer);
            while (n > 0) {
                baos.write(readBuffer, 0, n);
                n = bis.read(readBuffer);
            }
        } catch (Exception e) {
            throw GSearchCorePlugin.createException(e);
        } finally {
            close(in);
            close(bis);
        }
        return baos.toByteArray();
    }

    /**
	 * Returns a String from an InputStream is aware of its encoding. If the
	 * encoding is <code>null</code> it sets the platforms default encoding (
	 * <code>ResourcesPlugin.getEncoding()</code>).
	 * 
	 * @param stream
	 * @return the content
	 */
    public static String getStringFromInputStream(InputStream stream, String encoding) throws CoreException {
        StringBuffer sb = new StringBuffer(2048);
        InputStreamReader in = null;
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(stream);
            in = new InputStreamReader(bis, encoding == null ? ResourcesPlugin.getEncoding() : encoding);
            char[] readBuffer = new char[2048];
            int n = in.read(readBuffer);
            while (n > 0) {
                sb.append(readBuffer, 0, n);
                n = in.read(readBuffer);
            }
        } catch (Exception e) {
            throw GSearchCorePlugin.createException(e);
        } finally {
            close(in);
            close(bis);
        }
        return sb.toString();
    }
}
