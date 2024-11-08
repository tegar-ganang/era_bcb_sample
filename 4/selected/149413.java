package com.monad.homerun.pkg.jnlp.servlet;

import java.io.*;
import java.util.*;
import com.monad.homerun.pkg.jnlp.jardiff.*;
import javax.servlet.*;
import com.monad.homerun.pkg.jnlp.util.VersionString;
import java.net.URL;

public class JarDiffHandler {

    private static final int BUF_SIZE = 32 * 1024;

    private static final String JARDIFF_MIMETYPE = "application/x-java-archive-diff";

    /** List of all generated JARDiffs */
    private HashMap<JarDiffKey, JarDiffEntry> _jarDiffEntries = null;

    /** Reference to ServletContext and logger object */
    private static Logger _log = null;

    private ServletContext _servletContext = null;

    private String _jarDiffMimeType = null;

    private static class JarDiffKey implements Comparable {

        private String _name;

        private String _fromVersionId;

        private String _toVersionId;

        private boolean _minimal;

        /** Constructor used to generate a query object */
        public JarDiffKey(String name, String fromVersionId, String toVersionId, boolean minimal) {
            _name = name;
            _fromVersionId = fromVersionId;
            _toVersionId = toVersionId;
            _minimal = minimal;
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

        public int compareTo(Object o) {
            if (!(o instanceof JarDiffKey)) return -1;
            JarDiffKey other = (JarDiffKey) o;
            int n = _name.compareTo(other.getName());
            if (n != 0) return n;
            n = _fromVersionId.compareTo(other.getFromVersionId());
            if (n != 0) return n;
            if (_minimal != other.isMinimal()) return -1;
            return _toVersionId.compareTo(other.getToVersionId());
        }

        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }

        public int hashCode() {
            return _name.hashCode() + _fromVersionId.hashCode() + _toVersionId.hashCode();
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
        _jarDiffEntries = new HashMap<JarDiffKey, JarDiffEntry>();
        _servletContext = servletContext;
        _log = log;
        _jarDiffMimeType = _servletContext.getMimeType("xyz.jardiff");
        if (_jarDiffMimeType == null) _jarDiffMimeType = JARDIFF_MIMETYPE;
    }

    /** Returns a JarDiff for the given request */
    public synchronized DownloadResponse getJarDiffEntry(ResourceCatalog catalog, DownloadRequest dreq, JnlpResource res) {
        if (dreq.getCurrentVersionId() == null) return null;
        boolean doJarDiffWorkAround = isJavawsVersion(dreq, "1.0 1.0.1");
        JarDiffKey key = new JarDiffKey(res.getName(), dreq.getCurrentVersionId(), res.getReturnVersionId(), !doJarDiffWorkAround);
        JarDiffEntry entry = _jarDiffEntries.get(key);
        if (entry == null) {
            if (_log.isInformationalLevel()) {
                _log.addInformational("servlet.log.info.jardiff.gen", res.getName(), dreq.getCurrentVersionId(), res.getReturnVersionId());
            }
            File f = generateJarDiff(catalog, dreq, res, doJarDiffWorkAround);
            if (f == null) {
                _log.addWarning("servlet.log.warning.jardiff.failed", res.getName(), dreq.getCurrentVersionId(), res.getReturnVersionId());
            }
            entry = new JarDiffEntry(f);
            _jarDiffEntries.put(key, entry);
        }
        if (entry.getJarDiffFile() == null) {
            return null;
        } else {
            return DownloadResponse.getFileDownloadResponse(entry.getJarDiffFile(), _jarDiffMimeType, entry.getJarDiffFile().lastModified(), res.getReturnVersionId());
        }
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
        _log.addDebug("JarDiffHandler:  Doing download");
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
            _log.addDebug("total read: " + totalRead);
            _log.addDebug("Wrote URL " + target.toString() + " to file " + file);
        } catch (IOException ioe) {
            _log.addDebug("Got exception while downloading resource: " + ioe);
            ret = false;
            if (file != null) delete = true;
        } finally {
            try {
                in.close();
                in = null;
            } catch (IOException ioe) {
                _log.addDebug("Got exception while downloading resource: " + ioe);
            }
            try {
                out.close();
                out = null;
            } catch (IOException ioe) {
                _log.addDebug("Got exception while downloading resource: " + ioe);
            }
            if (delete) {
                file.delete();
            }
        }
        return ret;
    }

    private String getRealPath(String path) throws IOException {
        URL fileURL = _servletContext.getResource(path);
        File tempDir = (File) _servletContext.getAttribute("javax.servlet.context.tempdir");
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
        boolean del_old = false;
        boolean del_new = false;
        DownloadRequest fromDreq = dreq.getFromDownloadRequest();
        try {
            JnlpResource fromRes = catalog.lookupResource(fromDreq);
            String newFilePath = _servletContext.getRealPath(res.getPath());
            String oldFilePath = _servletContext.getRealPath(fromRes.getPath());
            if (newFilePath == null) {
                newFilePath = getRealPath(res.getPath());
                if (newFilePath != null) del_new = true;
            }
            if (oldFilePath == null) {
                oldFilePath = getRealPath(fromRes.getPath());
                if (oldFilePath != null) del_old = true;
            }
            if (newFilePath == null || oldFilePath == null) {
                return null;
            }
            File tempDir = (File) _servletContext.getAttribute("javax.servlet.context.tempdir");
            File outputFile = File.createTempFile("jnlp", ".jardiff", tempDir);
            _log.addDebug("Generating Jardiff between " + oldFilePath + " and " + newFilePath + " Store in " + outputFile);
            OutputStream os = new FileOutputStream(outputFile);
            JarDiff.createPatch(oldFilePath, newFilePath, os, !doJarDiffWorkAround);
            os.close();
            try {
                if (outputFile.length() >= (new File(newFilePath).length())) {
                    _log.addDebug("JarDiff discarded - since it is bigger");
                    return null;
                }
                File newFilePacked = new File(newFilePath + ".pack.gz");
                if (newFilePacked.exists()) {
                    _log.addDebug("generated jardiff size: " + outputFile.length());
                    _log.addDebug("packed requesting file size: " + newFilePacked.length());
                    if (outputFile.length() >= newFilePacked.length()) {
                        _log.addDebug("JarDiff discarded - packed version of requesting file is smaller");
                        return null;
                    }
                }
                _log.addDebug("JarDiff generation succeeded");
                return outputFile;
            } finally {
                if (del_new) {
                    new File(newFilePath).delete();
                }
                if (del_old) {
                    new File(oldFilePath).delete();
                }
            }
        } catch (IOException ioe) {
            _log.addDebug("Failed to genereate jardiff", ioe);
            return null;
        } catch (ErrorResponseException ere) {
            _log.addDebug("Failed to genereate jardiff", ere);
            return null;
        }
    }
}
