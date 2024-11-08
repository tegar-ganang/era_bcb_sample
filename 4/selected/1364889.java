package netxrv.jnlp.servlet;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import netxrv.jnlp.jardescriptor.NormalizedJarDescriptor;
import netxrv.jnlp.jardiff.*;
import netxrv.jnlp.util.VersionString;
import java.net.URL;

public class JarDiffHandler {

    private static final int BUF_SIZE = 32 * 1024;

    private static final String JARDIFF_MIMETYPE = "application/x-java-archive-diff";

    /** Reference to logger object */
    private final java.util.logging.Logger _log;

    /** List of all generated JARDiffs */
    private HashMap _jarDiffEntries = null;

    /** Reference to ServletContext */
    private ServletContext _servletContext = null;

    private String _jarDiffMimeType = null;

    private String _jarDiffStrategy = "recursive";

    private String _tarDiffStrategy = "on";

    private static class JarDiffKey implements Comparable {

        private String _name;

        private String _fromVersionId;

        private String _toVersionId;

        private boolean _minimal;

        private boolean _recursive;

        private boolean _usesTarDiffs;

        /** Constructor used to generate a query object */
        public JarDiffKey(String name, DownloadRequest dreq, String toVersionId, boolean minimal) {
            _name = name;
            _fromVersionId = dreq.getCurrentVersionId();
            _toVersionId = toVersionId;
            _minimal = minimal;
            _recursive = dreq.isRecursiveJarDiff();
            _usesTarDiffs = dreq.usesTarDiffs();
        }

        public String getName() {
            return _name;
        }

        public String getFromVersionId() {
            return _fromVersionId;
        }

        public String getToVersionId() {
            return _toVersionId;
        }

        public boolean isMinimal() {
            return _minimal;
        }

        public boolean isRecursive() {
            return _recursive;
        }

        public boolean usesTarDiffs() {
            return _usesTarDiffs;
        }

        public int compareTo(Object o) {
            if (!(o instanceof JarDiffKey)) return -1;
            JarDiffKey other = (JarDiffKey) o;
            int n = _name.compareTo(other.getName());
            if (n != 0) return n;
            n = _fromVersionId.compareTo(other.getFromVersionId());
            if (n != 0) return n;
            if (_minimal != other.isMinimal()) return -1;
            if (_recursive != other.isRecursive()) return -1;
            if (_usesTarDiffs != other.usesTarDiffs()) return -1;
            return _toVersionId.compareTo(other.getToVersionId());
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }

        public int hashCode() {
            return _name.hashCode() + _fromVersionId.hashCode() + _toVersionId.hashCode();
        }

        public String toString() {
            return "_name" + _name + ", _fromVersionId:" + _fromVersionId + ", _toVersionId:" + _toVersionId;
        }
    }

    private static class JarDiffEntry {

        private File _jardiffFile;

        public JarDiffEntry(File jarDiffFile) {
            _jardiffFile = jarDiffFile;
        }

        public File getJarDiffFile() {
            return _jardiffFile;
        }
    }

    /** Initialize JarDiff handler */
    public JarDiffHandler(ServletContext servletContext, Logger log) {
        _jarDiffEntries = new HashMap();
        _servletContext = servletContext;
        _jarDiffMimeType = _servletContext.getMimeType("xyz.jardiff");
        if (_jarDiffMimeType == null) _jarDiffMimeType = JARDIFF_MIMETYPE;
        _log = log;
        getTempDir().delete();
    }

    /** Returns a JarDiff for the given request */
    public synchronized DownloadResponse getJarDiffEntry(ResourceCatalog catalog, DownloadRequest dreq, JnlpResource res) {
        if (dreq.getCurrentVersionId() == null) return null;
        if (dreq.getCurrentVersionId().equals(res.getReturnVersionId())) {
            _log.info("current and returned version are the same: " + dreq.getCurrentVersionId());
            return null;
        }
        boolean doJarDiffWorkAround = isJavawsVersion(dreq, "1.0 1.0.1");
        JarDiffKey key = new JarDiffKey(res.getName(), dreq, res.getReturnVersionId(), !doJarDiffWorkAround);
        JarDiffEntry entry = (JarDiffEntry) _jarDiffEntries.get(key);
        if (entry == null) {
            if (LoggerUtil.isInformationalLevel(_log)) {
                LoggerUtil.addInformational(_log, "servlet.log.info.jardiff.gen", res.getName(), dreq.getCurrentVersionId(), res.getReturnVersionId());
            }
            File f = generateJarDiff(catalog, dreq, res, doJarDiffWorkAround);
            if (f == null) {
                LoggerUtil.addWarning(_log, "servlet.log.warning.jardiff.failed", res.getName(), dreq.getCurrentVersionId(), res.getReturnVersionId());
            }
            entry = new JarDiffEntry(f);
            _log.info("Adding jar diff for " + key);
            _jarDiffEntries.put(key, entry);
        }
        if (entry.getJarDiffFile() == null) {
            return null;
        } else {
            return DownloadResponse.getFileDownloadResponse(entry.getJarDiffFile(), _jarDiffMimeType, entry.getJarDiffFile().lastModified(), res.getReturnVersionId());
        }
    }

    /**
	 * Acceptable values:
	 * none, single, recursive
	 * default is recursive
	 * @param strategy
	 * @return
	 */
    public void setJarDiffStrategy(String strategy) {
        _jarDiffStrategy = strategy;
    }

    public void setTarDiffStrategy(String strategy) {
        _tarDiffStrategy = strategy;
    }

    public static boolean isJavawsVersion(DownloadRequest dreq, String version) {
        String javawsAgent = "javaws";
        String jwsVer = dreq.getHttpRequest().getHeader("User-Agent");
        if (!jwsVer.startsWith("javaws-")) {
            StringTokenizer st = new StringTokenizer(jwsVer);
            while (st.hasMoreTokens()) {
                String verString = st.nextToken();
                int index = verString.indexOf(javawsAgent);
                if (index != -1) {
                    verString = verString.substring(index + javawsAgent.length() + 1);
                    return VersionString.contains(version, verString);
                }
            }
            return false;
        }
        int startIndex = jwsVer.indexOf("-");
        if (startIndex == -1) {
            return false;
        }
        int endIndex = jwsVer.indexOf("/");
        if (endIndex == -1 || endIndex < startIndex) {
            return false;
        }
        String verId = jwsVer.substring(startIndex + 1, endIndex);
        return VersionString.contains(version, verId);
    }

    /** Download resource to the given file */
    private boolean download(URL target, File file) {
        LoggerUtil.addFine(_log, "JarDiffHandler:  Doing download");
        boolean ret = true;
        boolean delete = false;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = new BufferedInputStream(target.openStream());
            out = new BufferedOutputStream(new FileOutputStream(file));
            int read = 0;
            int totalRead = 0;
            byte[] buf = new byte[BUF_SIZE];
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                totalRead += read;
            }
            LoggerUtil.addFine(_log, "total read: " + totalRead);
            LoggerUtil.addFine(_log, "Wrote URL " + target.toString() + " to file " + file);
        } catch (IOException ioe) {
            LoggerUtil.addFine(_log, "Got exception while downloading resource: " + ioe);
            ret = false;
            if (file != null) delete = true;
        } finally {
            try {
                in.close();
                in = null;
            } catch (IOException ioe) {
                LoggerUtil.addFine(_log, "Got exception while downloading resource: " + ioe);
            }
            try {
                out.close();
                out = null;
            } catch (IOException ioe) {
                LoggerUtil.addFine(_log, "Got exception while downloading resource: " + ioe);
            }
            if (delete) {
                file.delete();
            }
        }
        return ret;
    }

    private String getRealPath(String path) throws IOException {
        URL fileURL = new URL(JNLPSessionUtil.getResourcePath(_servletContext, path.trim()));
        File tempDir = getTempDir();
        if (fileURL != null) {
            File newFile = File.createTempFile("temp", ".jar", tempDir);
            if (download(fileURL, newFile)) {
                String filePath = newFile.getPath();
                return filePath;
            }
        }
        return null;
    }

    private File generateJarDiff(ResourceCatalog catalog, DownloadRequest dreq, JnlpResource res, boolean doJarDiffWorkAround) {
        if (_jarDiffStrategy == null) {
            LoggerUtil.addFine(_log, "JarDiff discarded - since no strategy specified");
            return null;
        }
        if ("none".equals(_jarDiffStrategy.toLowerCase())) {
            LoggerUtil.addFine(_log, "JarDiff discarded - since strategy = none");
            return null;
        }
        boolean del_old = false;
        boolean del_new = false;
        DownloadRequest fromDreq = dreq.getFromDownloadRequest();
        try {
            JnlpResource fromRes = catalog.lookupResource(fromDreq);
            String newFilePath = null;
            if (res.getPath() != null && res.getPath().startsWith("file://")) {
                newFilePath = res.getPath().substring(7);
            } else {
                newFilePath = JNLPSessionUtil.getResourcePath(_servletContext, res.getPath().trim());
            }
            String oldFilePath = null;
            String fromPath = fromRes.getPath();
            if (fromPath != null && fromPath.startsWith("file://")) {
                LoggerUtil.addFine(_log, "Getting Real Path by truncating: " + fromPath);
                oldFilePath = fromPath.substring(7);
            } else {
                LoggerUtil.addFine(_log, "Getting Real Path from Servlet Context for: " + fromPath);
                oldFilePath = JNLPSessionUtil.getResourcePath(_servletContext, fromPath.trim());
            }
            if (newFilePath == null) {
                newFilePath = getRealPath(res.getPath());
                if (newFilePath != null) del_new = true;
            }
            if (oldFilePath == null) {
                LoggerUtil.addFine(_log, "Getting Real Path for: " + fromPath);
                oldFilePath = getRealPath(fromPath);
                if (oldFilePath != null) del_old = true;
            }
            if (newFilePath == null || oldFilePath == null) {
                return null;
            }
            if (dreq.getJarDescriptorDigest() != null) {
                LoggerUtil.addFine(_log, "Creating Normalized Jar Descriptor for: " + oldFilePath);
                String trueDigest = NormalizedJarDescriptor.createNormalizedJarDescriptorDigest(oldFilePath);
                if (!dreq.getJarDescriptorDigest().equals(trueDigest)) {
                    return null;
                }
            }
            File tempDir = getTempDir();
            File outputFile = File.createTempFile("jnlp", ".jardiff", tempDir);
            LoggerUtil.addFine(_log, "Generating Jardiff between " + oldFilePath + " and " + newFilePath + " Store in " + outputFile);
            OutputStream os = new FileOutputStream(outputFile);
            boolean recursiveJarDiff = dreq.isRecursiveJarDiff();
            if (recursiveJarDiff && !"recursive".equals(_jarDiffStrategy.toLowerCase())) {
                recursiveJarDiff = false;
                LoggerUtil.addFine(_log, "Recursive JarDiff ignored - since strategy not set to recursive");
            }
            LoggerUtil.addFine(_log, "Creating patch oldFilePath is " + oldFilePath);
            LoggerUtil.addFine(_log, "Creating patch newFilePath is " + newFilePath);
            LoggerUtil.addFine(_log, "Creating patch doJarDiffWorkAround is " + doJarDiffWorkAround);
            LoggerUtil.addFine(_log, "Creating patch recursiveJarDiff is " + recursiveJarDiff);
            boolean doTarDiffs = dreq.usesTarDiffs();
            if (doTarDiffs && !"on".equals(_tarDiffStrategy.toLowerCase())) {
                doTarDiffs = false;
                LoggerUtil.addFine(_log, "TarDiff ignored - since strategy not set to on");
            }
            JarDiff.createPatch(oldFilePath, newFilePath, os, !doJarDiffWorkAround, recursiveJarDiff, doTarDiffs);
            os.close();
            try {
                if (outputFile.length() >= (new File(newFilePath).length())) {
                    LoggerUtil.addFine(_log, "JarDiff discarded - since it is bigger");
                    return null;
                }
                File newFilePacked = new File(newFilePath + ".pack.gz");
                if (newFilePacked.exists()) {
                    LoggerUtil.addFine(_log, "generated jardiff size: " + outputFile.length());
                    LoggerUtil.addFine(_log, "packed requesting file size: " + newFilePacked.length());
                    if (outputFile.length() >= newFilePacked.length()) {
                        LoggerUtil.addFine(_log, "JarDiff discarded - packed version of requesting file is smaller");
                        return null;
                    }
                }
                LoggerUtil.addFine(_log, "JarDiff generation succeeded");
                return outputFile;
            } finally {
                if (del_new) {
                    new File(newFilePath).delete();
                }
                if (del_old) {
                    new File(oldFilePath).delete();
                }
            }
        } catch (Exception e) {
            _log.log(Level.SEVERE, "Failed to generate jardiff", e);
            return null;
        }
    }

    private File getTempDir() {
        File tempDirParent = (File) _servletContext.getAttribute("javax.servlet.context.tempdir");
        File tempDir = new File(tempDirParent, "netxrv");
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        return tempDir;
    }
}
