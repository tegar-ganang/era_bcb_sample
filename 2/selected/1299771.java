package net.lukemurphey.nsia.web;

import javax.servlet.http.*;
import javax.servlet.*;
import net.lukemurphey.nsia.Application;
import net.lukemurphey.nsia.GenericUtils;
import net.lukemurphey.nsia.MimeType;
import net.lukemurphey.nsia.eventlog.EventLogMessage;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.*;
import java.net.JarURLConnection;

public class MediaServlet extends HttpServlet {

    private boolean cachingEnabled = true;

    private static String JAR_FILENAME = null;

    private static final long serialVersionUID = -8746902066794712658L;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(request, response, true);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doRequest(request, response, false);
    }

    protected void doRequest(HttpServletRequest request, HttpServletResponse response, boolean inGet) throws ServletException, IOException {
        response.setHeader("Server", WebConsoleServlet.SERVER_STRING);
        try {
            String requestedFilename = request.getRequestURI().substring(1);
            URL url = new URL(getJarFileName() + "/");
            JarURLConnection jarConnection = (JarURLConnection) (url.openConnection());
            JarFile jarFile = jarConnection.getJarFile();
            String negotiatedFilename = null;
            ZipEntry zipEntry = null;
            zipEntry = negotiateImageFile(jarFile, requestedFilename, isIE6OrEarlier(request.getHeader("User-Agent")));
            if (zipEntry == null) {
                zipEntry = jarFile.getEntry(requestedFilename);
            } else {
                negotiatedFilename = zipEntry.getName();
            }
            if (zipEntry == null || zipEntry.isDirectory()) {
                handleFileNotFound(inGet, request, response);
                return;
            }
            int fileSize = (int) zipEntry.getSize();
            response.setContentLength(fileSize);
            if (negotiatedFilename != null) {
                response.setContentType(getContentType(negotiatedFilename));
            } else {
                response.setContentType(getContentType(request.getRequestURI()));
            }
            InputStream in = jarFile.getInputStream(zipEntry);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            byte[] file = new byte[fileSize];
            int bytesRead = bufferedInputStream.read(file);
            bufferedInputStream.close();
            if (bytesRead == fileSize && cachingEnabled) {
                SimpleDateFormat formatter = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
                java.util.Date today = new java.util.Date();
                String date = formatter.format(GenericUtils.addOrSubstractDaysFromDate(today, 365));
                response.setHeader("Expires", date);
            }
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(file);
        } catch (FileNotFoundException e) {
            handleFileNotFound(inGet, request, response);
        } catch (IOException e) {
        } catch (Throwable t) {
            Application.getApplication().logExceptionEvent(EventLogMessage.EventType.WEB_ERROR, t);
        }
    }

    private void handleFileNotFound(boolean inGet, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getOutputStream().print("File not found");
        } catch (IOException e) {
        } catch (Throwable t) {
            Application.getApplication().logExceptionEvent(EventLogMessage.EventType.WEB_ERROR, t);
        }
    }

    /**
	 * Negotiate the file associated with the given name.
	 * @param jarFile
	 * @param name
	 * @param tryToAvoidPNGs
	 * @return
	 */
    private JarEntry negotiateImageFile(JarFile jarFile, String name, boolean tryToAvoidPNGs) {
        JarEntry entry = null;
        if (tryToAvoidPNGs) {
            entry = getJarEntry(name + ".jpg", jarFile);
            if (entry != null) {
                return entry;
            }
            entry = getJarEntry(name + ".gif", jarFile);
            if (entry != null) {
                return entry;
            }
            entry = getJarEntry(name + ".png", jarFile);
            if (entry != null) {
                return entry;
            }
        } else {
            entry = getJarEntry(name + ".png", jarFile);
            if (entry != null) {
                return entry;
            }
            entry = getJarEntry(name + ".jpg", jarFile);
            if (entry != null) {
                return entry;
            }
            entry = getJarEntry(name + ".gif", jarFile);
            if (entry != null) {
                return entry;
            }
        }
        return entry;
    }

    /**
	 * Extract the entry from the Jar that corresponds to the name given.
	 * @param name
	 * @param jarFile
	 * @return
	 */
    private JarEntry getJarEntry(String name, JarFile jarFile) {
        for (Enumeration<JarEntry> entry = jarFile.entries(); entry.hasMoreElements(); ) {
            JarEntry jarEntry = entry.nextElement();
            if (jarEntry.getName().equals(name)) {
                return jarEntry;
            }
        }
        return null;
    }

    /**
	 * Get the content-type associated with the file name.
	 * @param filename
	 * @return
	 */
    private String getContentType(String filename) {
        String mimeType = MimeType.getMimeTypeFromName(filename);
        if (mimeType != null) {
            return mimeType;
        }
        if (filename.endsWith("png")) {
            return "image/png";
        } else if (filename.endsWith("css")) {
            return "text/css";
        } else if (filename.endsWith("gif")) {
            return "image/gif";
        } else if (filename.endsWith("jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith("js")) {
            return "text/javascript";
        } else if (filename.endsWith("html")) {
            return "text/html";
        } else if (filename.endsWith("htm")) {
            return "text/html";
        } else {
            return "text/plain";
        }
    }

    /**
	 * Returns true if the user-agent string indicates that the browser is IE 6 or earlier. This is noteworthy because IE 6 and earlier had dreadful support for PNGs.
	 * @param userAgent
	 * @return
	 */
    public static boolean isIE6OrEarlier(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        Pattern pattern = Pattern.compile("MSIE [0-6]");
        Matcher matcher = pattern.matcher(userAgent);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Get the jar filename of the web console web-archive.
	 * @return
	 */
    private String getJarFileName() {
        if (JAR_FILENAME == null) {
            String jarFile = "jar:file:../lib/webConsole.war!";
            JAR_FILENAME = jarFile;
            return JAR_FILENAME;
        } else return JAR_FILENAME;
    }
}
