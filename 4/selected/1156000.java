package net.sf.ajaxplus.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import javax.servlet.http.HttpServletResponse;

public class FileUtil {

    private FileUtil() {
    }

    /**
     * Method createInputStream attempts to create an InputStream object using 
     * the passed file name. It first attempts to get the classloader from the 
     * current thread which is the most reliable, if that fails it attempts 
     * creation using File.class.getResourceAsStream(..).
     * 
     * @param fileName
     * @return InputStream
     */
    public static InputStream createInputStream(String fileName) {
        InputStream inputStream = null;
        try {
            inputStream = createInputStream(Thread.currentThread().getContextClassLoader(), fileName);
        } catch (Exception e) {
        }
        if (inputStream != null) return inputStream;
        try {
            inputStream = createInputStream(FileUtil.class.getClassLoader(), fileName);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to locate the file: " + ex.getMessage());
        }
        if (inputStream == null) throw new IllegalArgumentException("Can't locate the file: " + fileName);
        return inputStream;
    }

    public static InputStream createInputStream(ClassLoader classLoader, String fileName) throws Exception {
        return classLoader.getResourceAsStream(SystemUtil.getFileSeparator() + fileName);
    }

    /**
	 * Send the given file as a byte array to the servlet response. If attachment 
	 * is set to true, then show a "Save as" dialogue, else show the file inline
	 * in the browser or let the operating system open it in the right application.
	 * @param response The HttpServletResponse to be used.
	 * @param bytes The file contents in a byte array.
	 * @param fileName The file name.
	 * @param attachment Download as attachment? 
	 */
    public static void downloadFile(HttpServletResponse response, byte[] bytes, String fileName, boolean attachment) throws IOException {
        downloadFile(response, new ByteArrayInputStream(bytes), fileName, attachment);
    }

    /**
	 * Send the given file as a File object to the servlet response. If attachment
	 * is set to true, then show a "Save as" dialogue, else show the file inline
	 * in the browser or let the operating system open it in the right application.
	 * @param response The HttpServletResponse to be used.
	 * @param file The file as a File object.
	 * @param attachment Download as attachment? 
	 */
    public static void downloadFile(HttpServletResponse response, File file, boolean attachment) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            downloadFile(response, input, file.getName(), attachment);
        } catch (IOException e) {
            throw e;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void downloadFile(HttpServletResponse response, InputStream input, String fileName, boolean attachment) throws IOException {
        BufferedOutputStream output = null;
        try {
            int contentLength = input.available();
            String contentType = URLConnection.guessContentTypeFromName(fileName);
            String disposition = attachment ? "attachment" : "inline";
            response.setContentLength(contentLength);
            response.setContentType(contentType);
            response.setHeader("Content-disposition", disposition + "; filename=\"" + fileName + "\"");
            output = new BufferedOutputStream(response.getOutputStream());
            while (contentLength-- > 0) {
                output.write(input.read());
            }
            output.flush();
        } catch (IOException e) {
            throw e;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
