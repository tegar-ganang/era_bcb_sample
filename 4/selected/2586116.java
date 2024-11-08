package uk.ac.dl.dp.web.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.*;

/**
 *
 * @author gjd37
 */
public class Util {

    private static Logger log = Logger.getLogger(Util.class);

    /** Creates a new instance of Util */
    public Util() {
    }

    public static void copyFile(File source, File dest) throws Exception {
        log.warn("File names are " + source.toString() + "   and " + dest.toString());
        if (!dest.getParentFile().exists()) dest.getParentFile().mkdir();
        FileChannel sourceChannel = new FileInputStream(source).getChannel();
        FileChannel destinationChannel = new FileOutputStream(dest).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    public static String createDate() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int month = Calendar.getInstance().get(Calendar.MONTH);
        int year = Calendar.getInstance().get(Calendar.YEAR);
        return day + "-" + month + "-" + year;
    }

    public static synchronized String createWebStartFile(HttpServletRequest req, String SRBFilename, String workingDir, String contextPath) throws UnknownHostException {
        ServletRequest sreq = (ServletRequest) req;
        File file = new File(workingDir + File.separator + "web-start" + File.separator + "webstart_template.jnlp");
        File webStartDir = new File(workingDir + File.separator + "web-start" + File.separator + createDate());
        if (!webStartDir.exists()) webStartDir.mkdir();
        String filename = "" + Math.random() + ".jnlp";
        File webStartFile = new File(webStartDir, filename);
        BufferedReader in = null;
        BufferedWriter out = null;
        InetAddress host = InetAddress.getLocalHost();
        String hostURL = sreq.getScheme() + "://" + host.getHostAddress() + ":" + sreq.getServerPort() + contextPath + "/web-start/" + createDate();
        try {
            in = new BufferedReader(new FileReader(file));
            out = new BufferedWriter(new FileWriter(webStartFile));
            String str;
            while ((str = in.readLine()) != null) {
                if (str.trim().startsWith("codebase")) {
                    out.write("codebase=\"" + hostURL + "\"\n");
                } else if (str.trim().startsWith("href")) {
                    out.write("href=\"" + filename + "\">\n");
                } else if (str.trim().startsWith("<application-desc")) {
                    out.write(str);
                    out.write("\n<argument>" + hostURL + "/" + SRBFilename + "</argument>\n");
                    out.write("</application-desc>\n");
                    out.write("</jnlp>");
                    break;
                } else out.write(str + "\n");
            }
        } catch (IOException e) {
            System.out.println(e);
        } finally {
            try {
                in.close();
                out.close();
                File dir = new File(workingDir + File.separator + "web-start");
                File[] webStartDatefiles = dir.listFiles();
                for (File Sfile : webStartDatefiles) {
                    log.trace("File: " + Sfile.getAbsolutePath());
                    if (Sfile.isDirectory()) {
                        if (!Sfile.getName().equals(createDate())) {
                            log.trace("Deleting");
                            deleteDirectory(Sfile);
                        } else log.trace("Not deleting");
                    }
                }
            } catch (IOException ex) {
                log.warn("Error closing buffers", ex);
            }
        }
        return filename;
    }

    /**
     * Recursively deletes a directory, dangerous!
     * @param directory the directory to delete
     */
    public static boolean deleteDirectory(File directory) {
        return deleteDirectory(directory, directory.getPath().length());
    }

    public static boolean deleteDirectory(File directory, int pathSize) {
        if (directory != null && directory.isDirectory()) {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.getPath().length() > pathSize) {
                    if (file.isDirectory()) {
                        boolean success = deleteDirectory(file, file.getPath().length());
                        if (!success) {
                            return false;
                        }
                    } else {
                        boolean success = file.delete();
                        log.trace("Trying to delete: " + file.getName() + "?: " + success);
                        if (!success) {
                            return false;
                        }
                    }
                } else log.warn("Stopped deleting " + directory.getAbsolutePath() + ", detected a link to smaller path size: " + file.getPath());
            }
            return directory.delete();
        }
        return directory.delete();
    }

    public static String getSRBHost(String[] urls) throws MalformedURLException {
        String host1 = urls[0].toLowerCase().replace("srb://", "http://");
        return new URL(host1).getHost();
    }

    public static int getSRBPort(String[] urls) {
        try {
            String host1 = urls[0].toLowerCase().replace("srb://", "http://");
            int port = new URL(host1).getPort();
            if (port == -1) {
                log.info("Port set to nothing in " + urls[0] + " setting to default: " + DownloadConstants.DEFAULT_SRB_PORT);
                return DownloadConstants.DEFAULT_SRB_PORT;
            } else return port;
        } catch (MalformedURLException ex) {
            log.warn("Unable to get port, trying default as: " + DownloadConstants.DEFAULT_SRB_PORT);
            return DownloadConstants.DEFAULT_SRB_PORT;
        }
    }

    public static String escapeInvalidStrings(String invalid) {
        if (invalid == null) return null;
        invalid = invalid.replaceAll(">", "&#62;");
        invalid = invalid.replaceAll("<", "&#60");
        return invalid.replaceAll("~", "approx.");
    }
}
