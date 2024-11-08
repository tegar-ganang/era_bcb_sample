package net.sourceforge.processdash.tool.export.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import net.sourceforge.processdash.Settings;
import net.sourceforge.processdash.tool.bridge.client.ResourceBridgeClient;
import net.sourceforge.processdash.tool.bridge.client.TeamServerSelector;
import net.sourceforge.processdash.util.FileUtils;
import net.sourceforge.processdash.util.RobustFileOutputStream;
import net.sourceforge.processdash.util.StringUtils;
import net.sourceforge.processdash.util.TempFileFactory;
import net.sourceforge.processdash.util.lock.LockFailureException;

public class ExportFileStream {

    private String lastUrl;

    private File exportFile;

    private File directFile;

    private Object target;

    private URL serverUrl;

    private File tempOutFile;

    private AbortableOutputStream outStream;

    private static final Logger logger = Logger.getLogger(ExportFileStream.class.getName());

    public ExportFileStream(String lastUrl, File exportFile) {
        this.lastUrl = lastUrl;
        this.exportFile = exportFile;
        this.target = exportFile;
    }

    public Object getTarget() {
        return target;
    }

    public void abort() {
        AbortableOutputStream os = outStream;
        if (os != null) {
            try {
                os.abort();
            } catch (Exception e) {
            }
        }
        if (tempOutFile != null) tempOutFile.delete();
    }

    public OutputStream getOutputStream() throws IOException {
        this.target = validateTarget();
        tempOutFile = TempFileFactory.get().createTempFile("pdash-export-", ".tmp");
        tempOutFile.deleteOnExit();
        outStream = new AbortableOutputStream(tempOutFile);
        return outStream;
    }

    private Object validateTarget() throws IOException {
        boolean exportViaTeamServer = Settings.getBool("teamServer.useForDataExport", true);
        File exportDirectory = exportFile.getParentFile();
        if (exportDirectory != null) directFile = exportFile;
        if (exportViaTeamServer) {
            serverUrl = TeamServerSelector.resolveServerURL(lastUrl, MIN_SERVER_VERSION);
            if (serverUrl != null) return serverUrl;
        }
        if (exportDirectory == null) {
            target = lastUrl;
            throw new IOException("Cannot contact server '" + lastUrl + "'");
        }
        if (exportViaTeamServer) {
            serverUrl = TeamServerSelector.getServerURL(exportDirectory, MIN_SERVER_VERSION);
            if (serverUrl != null) return serverUrl;
        }
        if (!exportDirectory.isDirectory() || (directFile.exists() && !directFile.canWrite())) throw new FileNotFoundException(directFile.getPath());
        return directFile;
    }

    public void finish() throws IOException {
        try {
            long checksum = outStream.getChecksum().getValue();
            if (tryCopyToServer(checksum) == false) copyToDestFile(checksum);
        } finally {
            outStream = null;
            if (tempOutFile != null) tempOutFile.delete();
        }
    }

    private boolean tryCopyToServer(long checksum) throws IOException {
        if (serverUrl == null) return false;
        try {
            copyToServer(checksum);
            return true;
        } catch (Exception e) {
            if (directFile == null) throw new IOException("Could not contact server " + serverUrl);
            String exceptionType = e.getClass().getName();
            if (e.getMessage() != null) exceptionType += " (" + e.getMessage() + ")";
            logger.warning(exceptionType + " while exporting file to '" + serverUrl + "' - trying direct file route");
            return false;
        }
    }

    private void copyToServer(long checksum) throws IOException, LockFailureException {
        FileInputStream in = new FileInputStream(tempOutFile);
        String name = exportFile.getName();
        Long serverSum = ResourceBridgeClient.uploadSingleFile(serverUrl, name, in);
        if (serverSum == null || serverSum != checksum) throw new IOException("checksum mismatch after uploading file");
        target = serverUrl;
    }

    private void copyToDestFile(long checksum) throws IOException {
        RobustFileOutputStream out = new RobustFileOutputStream(directFile, false);
        FileUtils.copyFile(tempOutFile, out);
        long copySum = out.getChecksum();
        if (copySum == checksum) {
            out.close();
            target = directFile;
        } else {
            out.abort();
            throw new IOException("Error writing to " + directFile + " - checksums do not match");
        }
    }

    @Override
    public String toString() {
        return target.toString();
    }

    private static class AbortableOutputStream extends CheckedOutputStream {

        private volatile boolean aborted;

        public AbortableOutputStream(File dest) throws IOException {
            this(new BufferedOutputStream(new FileOutputStream(dest)));
        }

        public AbortableOutputStream(OutputStream out) {
            super(out, new Adler32());
            this.aborted = false;
        }

        public void abort() {
            this.aborted = true;
            try {
                close();
            } catch (Exception e) {
            }
        }

        private void checkStatus() throws IOException {
            if (aborted) throw new IOException("Output aborted");
        }

        public void write(int b) throws IOException {
            checkStatus();
            super.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            checkStatus();
            super.write(b, off, len);
        }
    }

    private static final String MIN_SERVER_VERSION = "1.2";

    /**
     * Return a path which uniquely describes the destination of a file
     * which might be exported by this class.
     */
    public static String getExportTargetPath(File file, String url) {
        File dir = file.getParentFile();
        if (dir != null || !StringUtils.hasValue(url)) return file.getPath().replace('\\', '/');
        String result = url;
        if (!result.endsWith("/")) result = result + "/";
        result = result + file.getName();
        return result;
    }

    public interface ExportTargetDeletionFilter {

        public boolean shouldDelete(URL exportTarget);
    }

    /**
     * Attempt to delete a file that was exported by this class in the past.
     * 
     * @param targetPath
     *            a string that describes a past export target; this should be a
     *            value previously returned by
     *            {@link #getExportTargetPath(File, String)}.
     * @param deletionFilter
     *            a filter than can determine whether files should be deleted.
     * @return true if the path was recognized and successfully deleted, or if
     *            the deletion filter said it didn't need to be deleted.
     */
    public static boolean deleteExportTarget(String targetPath, ExportTargetDeletionFilter deletionFilter) {
        if (!StringUtils.hasValue(targetPath)) return true;
        if (TeamServerSelector.isUrlFormat(targetPath)) return deleteUrlExportTarget(targetPath, deletionFilter); else return deleteFilesystemExportTarget(targetPath, deletionFilter);
    }

    private static boolean deleteUrlExportTarget(String url, ExportTargetDeletionFilter deletionFilter) {
        int slashPos = url.lastIndexOf('/');
        if (slashPos == -1) return false;
        try {
            URL fullURL = new URL(url);
            if (deletionFilter != null && deletionFilter.shouldDelete(fullURL) == false) return true;
        } catch (Exception e) {
        }
        try {
            URL baseURL = new URL(url.substring(0, slashPos));
            String filename = url.substring(slashPos + 1);
            return ResourceBridgeClient.deleteSingleFile(baseURL, filename);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean deleteFilesystemExportTarget(String path, ExportTargetDeletionFilter deletionFilter) {
        path = path.replace('/', File.separatorChar);
        File file = new File(path);
        if (file.exists()) {
            try {
                URL fileURL = file.toURI().toURL();
                if (deletionFilter != null && deletionFilter.shouldDelete(fileURL) == false) return true;
            } catch (Exception e) {
            }
            return file.delete();
        }
        return file.getParentFile() != null && file.getParentFile().exists();
    }
}
