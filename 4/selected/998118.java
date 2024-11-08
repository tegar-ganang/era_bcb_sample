package net.sf.webwarp.util.web;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Utility class providing methods for file downloads (inline & attachment) in browsers.
 * 
 * @author sro
 */
public final class HttpServletFileDownload {

    private static final Logger log = Logger.getLogger(HttpServletFileDownload.class);

    /**
     * Singleton constructor.
     */
    private HttpServletFileDownload() {
    }

    /**
     * Send the given file as a File object to the servlet response. If attachment is set to true, then show a "Save as" dialogue, else show the file inline in
     * the browser or let the operating system open it in the right application. When no content type is provided, a contentType will be guessed from the file
     * name.
     * 
     * @param response
     *            The HttpServletResponse to be used.
     * @param file
     *            The file as a File object.
     * @param contentType -
     *            content type to be put in response header
     * @param attachment
     *            Download as attachment?
     */
    public static void downloadFile(final HttpServletResponse response, final File file, final String contentType, final boolean attachment) throws IOException {
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            HttpServletFileDownload.downloadFile(response, input, file.getName(), contentType, attachment);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (final IOException e) {
                    final String message = "Closing file " + file.getPath() + " failed. " + e;
                    log.error(message);
                }
            }
        }
    }

    /**
     * Send the given file as an InputStream to the servlet response. If attachment is set to true, then show a "Save as" dialogue, else show the file inline in
     * the browser or let the operating system open it in the right application. When no content type is provided, a contentType will be guessed from the file
     * name.
     * 
     * @param response
     *            The HttpServletResponse to be used.
     * @param input
     *            The file contents in an InputStream.
     * @param fileName
     *            The file name.
     * @param contentType -
     *            content type to be put in response header
     * @param attachment
     *            Download as attachment?
     */
    public static void downloadFile(final HttpServletResponse response, final InputStream input, final String fileName, String contentType, final boolean attachment) throws IOException {
        BufferedOutputStream output = null;
        try {
            int contentLength = input.available();
            final String disposition = attachment ? "attachment" : "inline";
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(fileName);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }
            response.reset();
            response.setContentLength(contentLength);
            response.setContentType(contentType);
            response.setHeader("Content-disposition", disposition + "; filename=\"" + fileName + "\"");
            output = new BufferedOutputStream(response.getOutputStream());
            while (contentLength-- > 0) {
                output.write(input.read());
            }
            output.flush();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (final IOException e) {
                    final String message = "Closing HttpServletResponse#getOutputStream() failed. " + e;
                    log.error(message);
                }
            }
        }
    }

    public static void downloadFile(final HttpServletResponse response, final byte[] bytes, final String fileName, String contentType, final boolean attachment) throws IOException {
        try {
            final int contentLength = bytes.length;
            final String disposition = attachment ? "attachment" : "inline";
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(fileName);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }
            response.reset();
            response.setContentLength(contentLength);
            response.setContentType(contentType);
            response.setHeader("Content-disposition", disposition + "; filename=\"" + fileName + "\"");
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } finally {
            try {
                response.getOutputStream().close();
            } catch (final IOException e) {
                final String message = "Closing HttpServletResponse#getOutputStream() failed. " + e;
                log.error(message);
            }
        }
    }

    public static void downloadFile(final HttpServletResponse response, final String content, String encoding, final String fileName, String contentType, final boolean attachment) throws IOException {
        if (content == null) {
            return;
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        try {
            final int contentLength = content.length();
            final String disposition = attachment ? "attachment" : "inline";
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(fileName);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            }
            response.reset();
            response.setContentLength(contentLength);
            response.setContentType(contentType);
            response.setCharacterEncoding(encoding);
            response.setHeader("Content-disposition", disposition + "; filename=\"" + fileName + "\"");
            response.getWriter().write(content);
            response.getWriter().flush();
        } finally {
            try {
                response.getOutputStream().close();
            } catch (final IOException e) {
                final String message = "Closing HttpServletResponse#getOutputStream() failed. " + e;
                log.error(message);
            }
        }
    }
}
