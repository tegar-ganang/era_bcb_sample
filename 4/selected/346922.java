package org.vegbank.nvcrs.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import javax.servlet.http.HttpServletResponse;

public class DownloadFile {

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

    /**
     * Send the given file as an InputStream to the servlet response. If attachment 
     * is set to true, then show a "Save as" dialogue, else show the file inline
     * in the browser or let the operating system open it in the right application.
     * @param response The HttpServletResponse to be used.
     * @param input The file contents in an InputStream.
     * @param fileName The file name.
     * @param attachment Download as attachment? 
     */
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
