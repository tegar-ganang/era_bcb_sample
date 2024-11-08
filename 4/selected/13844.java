package net.sf.buildbox.worker.cache;

import java.io.*;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Properties;
import net.sf.buildbox.scheduler.model.ResourceId;
import net.sf.buildbox.worker.impl.WorkerHelper;
import net.sf.buildbox.worker.api.TransferProgress;
import net.sf.buildbox.worker.api.Transfer;
import net.sf.buildbox.util.PushingDirectoryTraversal;
import org.codehaus.plexus.util.FileUtils;

public class LocalJobTransfer implements Transfer {

    private File directory;

    public File getDirectory() {
        return directory;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public File resourceCacheBase(ResourceId resourceId) {
        final StringBuilder sb = resourceCacheUri(resourceId);
        return new File(directory, sb.toString());
    }

    public static StringBuilder resourceCacheUri(ResourceId resourceId) {
        final StringBuilder sb = new StringBuilder(WorkerHelper.toFilename(resourceId.getProjectId()));
        sb.append(File.separatorChar);
        sb.append(WorkerHelper.toFilename(resourceId.getIncarnation()));
        char sep = '/';
        for (String coord : resourceId.getCoords()) {
            sb.append(sep);
            sep = '-';
            sb.append(WorkerHelper.toFilename(coord));
        }
        sb.append(File.separatorChar);
        sb.append(resourceId.getType());
        return sb;
    }

    public Properties getInfo(ResourceId resourceId) {
        throw new UnsupportedOperationException("getInfo");
    }

    public void retrieve(ResourceId resourceId, final File destRootDir, final TransferProgress progressObject) throws IOException {
        final File cacheBase = resourceCacheBase(resourceId);
        final File srcRootDir = new File(cacheBase, "content");
        final int srcPrefixLength = srcRootDir.getAbsolutePath().length() + 1;
        final PushingDirectoryTraversal t = new PushingDirectoryTraversal(new FileFilter() {

            int fileCount = 0;

            int dirCount = 0;

            int byteCount = 0;

            public boolean accept(File src) {
                try {
                    final String uri = src.getAbsolutePath().substring(srcPrefixLength);
                    final File dest = new File(destRootDir, uri);
                    progressObject.addFile(dest);
                    if (src.isDirectory()) {
                        dest.mkdirs();
                        dirCount++;
                        progressObject.setTransferredDirs(dirCount);
                    } else {
                        dest.getParentFile().mkdirs();
                        FileUtils.copyFile(src, dest);
                        fileCount++;
                        progressObject.setTransferredFiles(fileCount);
                        byteCount += src.length();
                        progressObject.setTransferredBytes(byteCount);
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("error while retrieving " + src, e);
                }
                return false;
            }
        });
        t.setWantDirs(true);
        t.setWantFiles(true);
        t.setSorted(false);
        t.traverse(srcRootDir);
    }

    public void store(File srcRootDir, final ResourceId resourceId, boolean storeDirEntries, final TransferProgress progressObject) throws IOException {
        final File cacheBase = resourceCacheBase(resourceId);
        final File cacheTmpBase = new File(cacheBase.getAbsolutePath() + ".tmp");
        final File destTmpRootDir = new File(cacheTmpBase, "content");
        destTmpRootDir.mkdirs();
        if (!destTmpRootDir.isDirectory()) {
            throw new IOException("Failed to create directory: " + destTmpRootDir);
        }
        final File destTmpListFile = new File(cacheTmpBase, "content.lst");
        final PrintWriter pw = new PrintWriter(destTmpListFile);
        final String srcPrefix = srcRootDir.getAbsolutePath() + '/';
        final PushingDirectoryTraversal t = new PushingDirectoryTraversal(new FileFilter() {

            int fileCount = 0;

            int dirCount = 0;

            int byteCount = 0;

            public boolean accept(File src) {
                try {
                    final String absPath = src.getAbsolutePath().replace('\\', '/');
                    final String uri = absPath.substring(srcPrefix.length());
                    final File dest = new File(destTmpRootDir, uri);
                    progressObject.addFile(src);
                    if (src.isDirectory()) {
                        dest.mkdirs();
                        pw.println(uri + "/");
                        dirCount++;
                        progressObject.setTransferredDirs(dirCount);
                    } else {
                        dest.getParentFile().mkdirs();
                        FileUtils.rename(src, dest);
                        pw.println(uri);
                        fileCount++;
                        progressObject.setTransferredFiles(fileCount);
                        byteCount += src.length();
                        progressObject.setTransferredBytes(byteCount);
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("error while storing " + src, e);
                }
                return false;
            }
        });
        t.setWantDirs(storeDirEntries);
        t.setWantFiles(true);
        t.setSorted(false);
        t.traverse(srcRootDir);
        pw.close();
        if (pw.checkError()) {
            throw new IOException("failure in updating " + destTmpListFile);
        }
        cacheTmpBase.renameTo(cacheBase);
        if (!cacheBase.isDirectory()) {
            throw new IOException(cacheTmpBase + " - failed renaming to " + cacheBase);
        }
    }

    @Deprecated
    public Set<String> dataList(ResourceId resourceId) throws IOException {
        final File cacheBase = resourceCacheBase(resourceId);
        final File listFile = new File(cacheBase, "content.lst");
        final BufferedReader r = new BufferedReader(new FileReader(listFile));
        try {
            final Set<String> result = new LinkedHashSet<String>();
            String line = r.readLine();
            while (line != null) {
                result.add(line);
                line = r.readLine();
            }
            return result;
        } finally {
            r.close();
        }
    }
}
