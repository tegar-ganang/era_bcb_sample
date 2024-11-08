package com.fray.evo.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingWorker;

/**
 * Automatically downloads the newest version of the application and runs it.
 */
public class EcAutoUpdate extends SwingWorker<Void, Void> {

    private static final Logger logger = Logger.getLogger(EcAutoUpdate.class.getName());

    /**
	 * The format of the URL that points to the JAR file.
	 */
    private static final String downloadUrlFormat = "http://evolutionchamber.googlecode.com/files/evolutionchamber-version-%s.jar";

    /**
	 * The format of the filename that is given to the downloaded JAR file.
	 */
    private static final String downloadedFileNameFormat = "evolutionchamber-version-%s.jar";

    /**
	 * The format of the URL that contains the JAR file's checksum.
	 */
    private static final String checksumUrlFormat = "http://code.google.com/p/evolutionchamber/downloads/detail?name=evolutionchamber-version-%s.jar";

    /**
	 * The regular expression used to scrape the checksum from the webpage of the JAR file.
	 */
    private static final Pattern checksumPattern = Pattern.compile("SHA1 Checksum: </th><td style=\"white-space:nowrap\"> ([0-9a-f]{40})", Pattern.CASE_INSENSITIVE);

    /**
	 * The URL of the project's download page.
	 */
    private static final String downloadsPageUrl = "http://code.google.com/p/evolutionchamber/downloads/list";

    /**
	 * A regular expression that matches against URLs to the various versions of
	 * the application.
	 */
    private static final Pattern jarUrlRegex = Pattern.compile("evolutionchamber\\.googlecode\\.com/files/evolutionchamber-version-(\\d+)\\.jar");

    /**
	 * Whether or not there is a newer version available.
	 */
    private boolean updateAvailable = false;

    /**
	 * Whether or not it is performing an update.
	 */
    private boolean updating = false;

    /**
	 * The version number of the latest version. For example, "0017".
	 */
    private String latestVersion = "";

    /**
	 * The name of the downloaded JAR file.
	 */
    private String jarFile = "";

    /**
	 * Whether or not the checksum of the downloaded file matches the expected checksum.
	 */
    private boolean checksumMatches = true;

    /**
     * Allows the code calling this class to handle certain auto-updater events.
     */
    protected final Callback callback;

    /**
	 * Constructor.
	 * @param ecVersion the version of the currently running application. For example, "0017".
	 */
    public EcAutoUpdate(String ecVersion, Callback callback) {
        configureProxy();
        this.callback = callback;
        this.latestVersion = findLatestVersion(ecVersion);
        if (!this.latestVersion.equals(ecVersion)) this.updateAvailable = true;
    }

    /**
	 * Allows the auto-update process to work if the user is behind a proxy.
	 * @see http://stackoverflow.com/questions/376101/setting-jvm-jre-to-use-windows-proxy-automatically
	 */
    private void configureProxy() {
        System.setProperty("java.net.useSystemProxies", "true");
        try {
            List<Proxy> proxies = ProxySelector.getDefault().select(new URI(downloadsPageUrl));
            for (Proxy proxy : proxies) {
                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                if (addr == null) {
                } else {
                    System.setProperty("http.proxyHost", addr.getHostName());
                    System.setProperty("http.proxyPort", Integer.toString(addr.getPort()));
                }
            }
        } catch (URISyntaxException e) {
            logger.log(Level.SEVERE, "Download list URL is invalid.", e);
        }
    }

    /**
	 * Whether or not there is a newer version available.
	 * @return true if a newer version is available, false if not
	 */
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    /**
	 * Whether or not it is performing an update.
	 * @return true if an update is being performed, false if not.
	 */
    public boolean isUpdating() {
        return updating;
    }

    /**
	 * Sets whether or not it is performing an update.
	 * @param updating true if an update is being performed, false if not.
	 */
    public void setUpdating(boolean updating) {
        this.updating = updating;
    }

    /**
	 * Gets the version number of the latest version. For example, "0017".
	 * @return the latest version number
	 */
    public String getLatestVersion() {
        return latestVersion;
    }

    /**
	 * Whether or not the checksum of the downloaded file matches the expected checksum.
	 * @return true if the file checksum matches the expected checksum
	 */
    public boolean isChecksumMatches() {
        return checksumMatches;
    }

    /**
	 * Determines what the latest version of the application is.
	 * @param ecVersion the version of the currently running application. For example, "0017".
	 * @return the latest version number
	 */
    private String findLatestVersion(String ecVersion) {
        String latestVersion = ecVersion;
        try {
            String html = null;
            {
                URL url = new URL(downloadsPageUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int read;
                while ((read = in.read()) != -1) {
                    out.write(read);
                }
                in.close();
                html = new String(out.toByteArray());
            }
            int latest = Integer.parseInt(ecVersion);
            Matcher m = jarUrlRegex.matcher(html);
            while (m.find()) {
                int cur = Integer.parseInt(m.group(1));
                if (cur > latest) {
                    latest = cur;
                    latestVersion = m.group(1);
                }
            }
        } catch (MalformedURLException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(sw.toString());
        } catch (UnknownHostException e) {
            callback.updateCheckFailed();
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(sw.toString());
        }
        return latestVersion;
    }

    /**
	 * Gets an inputstream to the file and the length of the file.
	 * @param version the version to retrieve
	 * @return an inputstream to the file and the length of the file
	 * @throws IOException
	 */
    protected FileInfo getFileInputStreamAndLength(String version) throws IOException {
        URL downloadUrl = new URL(String.format(downloadUrlFormat, version));
        URLConnection uc = downloadUrl.openConnection();
        String contentType = uc.getContentType();
        int contentLength = uc.getContentLength();
        if (contentType.startsWith("text/") || contentLength == -1) {
            throw new IOException("This is not a binary file.");
        }
        return new FileInfo(uc.getInputStream(), contentLength);
    }

    @Override
    public Void doInBackground() {
        int progress = 0;
        setProgress(0);
        this.updating = true;
        try {
            String expectedChecksum = getExpectedChecksum(latestVersion);
            boolean download = true;
            File file = new File(String.format(downloadedFileNameFormat, this.latestVersion));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            if (file.exists()) {
                FileInputStream in = new FileInputStream(file);
                byte[] buf = new byte[1024];
                int read;
                while ((read = in.read(buf)) != -1) {
                    md.update(buf, 0, read);
                }
                in.close();
                String fileChecksum = convertToHex(md.digest());
                if (expectedChecksum != null && expectedChecksum.equalsIgnoreCase(fileChecksum)) {
                    download = false;
                    checksumMatches = true;
                }
            }
            if (download) {
                FileInfo info = getFileInputStreamAndLength(latestVersion);
                String fileChecksum;
                {
                    FileOutputStream out = new FileOutputStream(file);
                    InputStream raw = info.inputStream;
                    InputStream in = new BufferedInputStream(raw);
                    byte[] buf = new byte[1024];
                    int bytesRead = 0;
                    int offset = 0;
                    while ((bytesRead = in.read(buf)) != -1) {
                        md.update(buf, 0, bytesRead);
                        out.write(buf, 0, bytesRead);
                        offset += bytesRead;
                        progress = (int) ((offset / (float) info.length) * 100);
                        setProgress(progress);
                    }
                    fileChecksum = convertToHex(md.digest());
                    in.close();
                    out.close();
                    if (offset != info.length) {
                        throw new IOException("Only read " + offset + " bytes; Expected " + info.length + " bytes");
                    }
                }
                checksumMatches = expectedChecksum == null || expectedChecksum.equalsIgnoreCase(fileChecksum);
            }
            this.jarFile = file.getName();
        } catch (MalformedURLException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(sw.toString());
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(sw.toString());
        } catch (NoSuchAlgorithmException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe(sw.toString());
        }
        return null;
    }

    /**
     * Scrapes the checksum for the JAR file of the given version from the website.
     * @param version the version of the application to get the checksum for
     * @return the checksum or null if it couldn't find the checksum
     * @throws IOException if there's a problem loading the page
     */
    public static String getExpectedChecksum(String version) throws IOException {
        String pageChecksum = null;
        URL checksumUrl = new URL(String.format(checksumUrlFormat, version));
        HttpURLConnection conn = (HttpURLConnection) checksumUrl.openConnection();
        BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read;
        while ((read = in.read()) != -1) {
            out.write(read);
        }
        String html = new String(out.toByteArray());
        in.close();
        out.close();
        Matcher m = checksumPattern.matcher(html);
        if (m.find()) {
            pageChecksum = m.group(1);
        }
        return pageChecksum;
    }

    @Override
    public void done() {
        setProgress(100);
        if (checksumMatches) {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String restartCmd[] = new String[] { javaBin, "-jar", this.jarFile };
            try {
                Runtime.getRuntime().exec(restartCmd);
            } catch (IOException e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logger.severe(sw.toString());
            }
        } else {
            callback.checksumFailed();
        }
        System.exit(0);
    }

    /**
     * Converts an array of bytes to a hex string.
     * @see http://www.anyexample.com/programming/java/java_simple_class_to_compute_sha_1_hash.xml
     * 
     * @param data the data to convert
     * @return the hex string
     */
    private static String convertToHex(byte[] data) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    /**
     * Allows the code calling this class to handle certain auto-updater events.
     * @author mike.angstadt
     *
     */
    public interface Callback {

        /**
    	 * Called when the checksum of the downloaded JAR file did not match its expected checksum.
    	 */
        void checksumFailed();

        /**
    	 * Called if there is a problem determining what the latest version is.
    	 */
        void updateCheckFailed();
    }

    protected static class FileInfo {

        public InputStream inputStream;

        public long length;

        public FileInfo(InputStream in, long length) {
            this.inputStream = in;
            this.length = length;
        }
    }
}
