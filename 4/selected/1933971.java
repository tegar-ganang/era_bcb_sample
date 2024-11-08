package jnlp.sample.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import jnlp.sample.jardiff.JarDiff;
import jnlp.sample.servlet.download.DownloadRequest;
import jnlp.sample.servlet.download.DownloadResponse;
import jnlp.sample.util.ObjectUtil;
import jnlp.sample.util.VersionString;
import jnlp.sample.util.log.Logger;
import jnlp.sample.util.log.LoggerFactory;

public class JarDiffHandler {

    public static final int BUF_SIZE = 4 * 1024;

    public static final String JARDIFF_MIMETYPE = "application/x-java-archive-diff";

    /** Reference to ServletContext and logger object */
    private final transient Logger _log;

    private final ServletContext _servletContext;

    private String _jarDiffMimeType;

    public static class JarDiffKey implements Comparable<JarDiffKey>, Serializable, Cloneable {

        /**
		 * 
		 */
        private static final long serialVersionUID = -5412560259047926467L;

        private String _name;

        private String _fromVersionId;

        private String _toVersionId;

        private boolean _minimal;

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

        @Override
        public int compareTo(JarDiffKey other) {
            if (null == other) return (-1);
            if (this == other) return 0;
            int n = ObjectUtil.match(getName(), other.getName(), true);
            if (n != 0) return n;
            if ((n = ObjectUtil.match(getFromVersionId(), other.getFromVersionId(), false)) != 0) return n;
            if (isMinimal() != other.isMinimal()) return -1;
            return ObjectUtil.match(getToVersionId(), other.getToVersionId(), false);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof JarDiffKey)) return false;
            return (compareTo((JarDiffKey) o) == 0);
        }

        @Override
        public int hashCode() {
            return ObjectUtil.objectHashCode(getName()) + ObjectUtil.objectHashCode(getFromVersionId()) + ObjectUtil.objectHashCode(getToVersionId()) + (isMinimal() ? 1 : 0);
        }

        @Override
        public JarDiffKey clone() throws CloneNotSupportedException {
            return getClass().cast(super.clone());
        }
    }

    public static class JarDiffEntry implements Serializable, Cloneable {

        /**
		 * 
		 */
        private static final long serialVersionUID = 3405539158098127167L;

        private File _jardiffFile;

        public JarDiffEntry(File jarDiffFile) {
            _jardiffFile = jarDiffFile;
        }

        public JarDiffEntry() {
            this(null);
        }

        public File getJarDiffFile() {
            return _jardiffFile;
        }

        public void setJarDiffFile(File f) {
            _jardiffFile = f;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof JarDiffEntry)) return false;
            if (this == o) return true;
            return ObjectUtil.match(getJarDiffFile(), ((JarDiffEntry) o).getJarDiffFile());
        }

        @Override
        public int hashCode() {
            return ObjectUtil.objectHashCode(getJarDiffFile());
        }

        @Override
        public JarDiffEntry clone() throws CloneNotSupportedException {
            return getClass().cast(super.clone());
        }
    }

    /** List of all generated JARDiffs */
    private final Map<JarDiffKey, JarDiffEntry> _jarDiffEntries;

    public JarDiffHandler(ServletContext servletContext) {
        _jarDiffEntries = new TreeMap<JarDiffKey, JarDiffEntry>();
        _servletContext = servletContext;
        _log = LoggerFactory.getLogger(JarDiffHandler.class);
        _jarDiffMimeType = _servletContext.getMimeType("xyz.jardiff");
        if ((_jarDiffMimeType == null) || (_jarDiffMimeType.length() <= 0)) _jarDiffMimeType = JARDIFF_MIMETYPE;
    }

    public DownloadResponse getJarDiffEntry(final ResourceCatalog catalog, final DownloadRequest dreq, final JnlpResource res) {
        final String verId = (null == dreq) ? null : dreq.getCurrentVersionId();
        if ((verId == null) || (verId.length() <= 0)) return null;
        final boolean doJarDiffWorkAround = isJavawsVersion(dreq, "1.0*");
        final String retVerId = res.getReturnVersionId(), resName = res.getName();
        final JarDiffKey key = new JarDiffKey(resName, verId, retVerId, !doJarDiffWorkAround);
        JarDiffEntry entry;
        synchronized (_jarDiffEntries) {
            entry = _jarDiffEntries.get(key);
        }
        if (entry == null) {
            final File f = generateJarDiff(catalog, dreq, res, doJarDiffWorkAround);
            if (f == null) _log.warn("servlet.log.warning.jardiff.failed", resName, verId, retVerId);
            entry = new JarDiffEntry(f);
            final JarDiffEntry prev;
            synchronized (_jarDiffEntries) {
                prev = _jarDiffEntries.put(key, entry);
            }
            if (_log.isInformationalLevel()) {
                if (prev == null) _log.info("servlet.log.info.jardiff.gen", resName, verId, retVerId); else _log.info("servlet.log.info.jardiff.gen", resName + "[prev]", verId, retVerId);
            }
        }
        final File diffFile = entry.getJarDiffFile();
        if (diffFile == null) return null;
        return DownloadResponse.getFileDownloadResponse(diffFile, _jarDiffMimeType, diffFile.lastModified(), retVerId);
    }

    public static final String JAVA_WS_AGENT = "javaws", JAVA_WS_USER_AGENT_HEADER = "User-Agent", JAVA_WS_AGENT_VERSION_PREFIX = "javaws-";

    public static boolean isJavawsVersion(DownloadRequest dreq, String version) {
        final HttpServletRequest req = (null == dreq) ? null : dreq.getHttpRequest();
        final String jwsVer = (null == req) ? null : req.getHeader(JAVA_WS_USER_AGENT_HEADER);
        if ((null == jwsVer) || (jwsVer.length() <= 0)) return false;
        if (!jwsVer.startsWith(JAVA_WS_AGENT_VERSION_PREFIX)) {
            for (final StringTokenizer st = new StringTokenizer(jwsVer); st.hasMoreTokens(); ) {
                String verString = st.nextToken();
                final int index = ((null == verString) || (verString.length() <= 0)) ? (-1) : verString.indexOf(JAVA_WS_AGENT);
                if (index < 0) {
                    verString = verString.substring(index + JAVA_WS_AGENT.length() + 1);
                    return VersionString.contains(version, verString);
                }
            }
            return false;
        }
        final int startIndex = jwsVer.indexOf('-');
        if (startIndex < 0) return false;
        final int endIndex = jwsVer.indexOf('/');
        if (endIndex <= startIndex) return false;
        final String verId = jwsVer.substring(startIndex + 1, endIndex);
        return VersionString.contains(version, verId);
    }

    protected boolean download(URL target, File file) {
        if (_log.isDebugLevel()) _log.debug("download(" + target + ") => " + file);
        boolean delete = false;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new BufferedInputStream(ObjectUtil.openResource(target));
            out = new BufferedOutputStream(new FileOutputStream(file));
            int read = 0, totalRead = 0;
            final byte[] buf = new byte[BUF_SIZE];
            while ((read = in.read(buf)) != -1) {
                if (read > 0) {
                    out.write(buf, 0, read);
                    totalRead += read;
                }
            }
            if (_log.isDebugLevel()) _log.debug("download(" + target + ")[" + totalRead + " bytes] => " + file);
            return true;
        } catch (IOException ioe) {
            _log.warn("download(" + target + ") " + ioe.getClass().getName() + " while write to file=" + file + ": " + ioe.getMessage(), ioe);
            if (file != null) delete = true;
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                    in = null;
                } catch (IOException ioe) {
                    _log.warn("download(" + target + ") " + ioe.getClass().getName() + " while close input to file=" + file + ": " + ioe.getMessage(), ioe);
                }
            }
            if (out != null) {
                try {
                    out.close();
                    out = null;
                } catch (IOException ioe) {
                    _log.warn("download(" + target + ") " + ioe.getClass().getName() + " while close output to file=" + file + ": " + ioe.getMessage(), ioe);
                }
            }
            if (delete) {
                if (!file.delete()) _log.warn("download(" + target + ") failed to delete file=" + file);
            }
        }
    }

    protected String getRealPath(String path) throws IOException {
        final URL fileURL = _servletContext.getResource(path);
        if (fileURL != null) {
            final File tempDir = (File) _servletContext.getAttribute("javax.servlet.context.tempdir"), newFile = File.createTempFile("temp", ".jar", tempDir);
            if (download(fileURL, newFile)) {
                final String filePath = newFile.getPath();
                return filePath;
            }
        }
        return null;
    }

    protected File generateJarDiff(final ResourceCatalog catalog, final DownloadRequest dreq, final JnlpResource res, boolean doJarDiffWorkAround) {
        boolean del_old = false, del_new = false;
        final DownloadRequest fromDreq = dreq.getFromDownloadRequest();
        final String resPath = res.getPath();
        try {
            final JnlpResource fromRes = catalog.lookupResource(fromDreq);
            final String fromPath = (null == fromRes) ? null : fromRes.getPath();
            String newFilePath = _servletContext.getRealPath(resPath);
            String oldFilePath = _servletContext.getRealPath(fromPath);
            if ((newFilePath == null) || (newFilePath.length() <= 0)) {
                newFilePath = getRealPath(resPath);
                if ((newFilePath != null) && (newFilePath.length() > 0)) del_new = true;
            }
            if ((oldFilePath == null) || (oldFilePath.length() <= 0)) {
                oldFilePath = getRealPath(fromPath);
                if ((oldFilePath != null) && (oldFilePath.length() > 0)) del_old = true;
            }
            if ((newFilePath == null) || (newFilePath.length() <= 0) || (oldFilePath == null) || (oldFilePath.length() <= 0)) return null;
            final File tempDir = (File) _servletContext.getAttribute("javax.servlet.context.tempdir"), outputFile = File.createTempFile("jnlp", ".jardiff", tempDir);
            if (_log.isDebugLevel()) _log.debug("Generating Jardiff between " + oldFilePath + " and " + newFilePath + " Store in " + outputFile);
            OutputStream os = new FileOutputStream(outputFile);
            try {
                JarDiff.createPatch(oldFilePath, newFilePath, os, !doJarDiffWorkAround);
            } finally {
                os.close();
            }
            final File newFile = new File(newFilePath);
            try {
                final long outLen = outputFile.length(), prevLen = newFile.length();
                if (outLen >= prevLen) {
                    if (_log.isDebugLevel()) _log.debug("JarDiff discarded " + outputFile + " - since it is bigger");
                    return null;
                }
                final File newFilePacked = new File(newFile.getParent(), newFile.getName() + JnlpResource.PACK_GZ_SUFFIX);
                if (newFilePacked.exists()) {
                    if (_log.isDebugLevel()) {
                        _log.debug("generated jardiff size: " + outputFile.length());
                        _log.debug("packed requesting file size: " + newFilePacked.length());
                    }
                    if (outLen >= newFilePacked.length()) {
                        if (_log.isDebugLevel()) _log.debug("JarDiff discarded " + outputFile + " - packed version of requesting file is smaller");
                        return null;
                    }
                }
                if (_log.isDebugLevel()) _log.debug("JarDiff generation succeeded");
                return outputFile;
            } finally {
                if (del_new) {
                    if (!newFile.delete()) _log.warn("Failed to delete (new) temp file=" + newFile);
                }
                if (del_old) {
                    if (!(new File(oldFilePath).delete())) _log.warn("Failed to delete (old) temp file=" + oldFilePath);
                }
            }
        } catch (Exception ioe) {
            _log.warn("generateJarDiff(" + resPath + ") " + ioe.getClass().getName() + ": " + ioe.getMessage(), ioe);
            return null;
        }
    }
}
