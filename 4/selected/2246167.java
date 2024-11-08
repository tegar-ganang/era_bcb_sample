package net.sf.buildbox.worker.impl;

import net.sf.buildbox.util.BbxSystemUtils;
import net.sf.buildbox.worker.api.BuildRepository;
import net.sf.buildbox.reactor.model.JobDependency;
import net.sf.buildbox.util.PushingDirectoryTraversal;
import net.sf.buildbox.worker.api.Transfer;
import net.sf.buildbox.worker.api.TransferProgress;
import org.codehaus.plexus.util.FileUtils;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class LocalJobTransfer implements Transfer {

    private static final Logger LOGGER = Logger.getLogger(LocalJobTransfer.class.getName());

    private File remoteCatalogRootDir;

    public void setRemoteCatalogRootDir(File remoteCatalogRootDir) {
        this.remoteCatalogRootDir = remoteCatalogRootDir;
    }

    public Collection<File> retrieve(JobDependency jobDependency, final File localAreaDir, final TransferProgress progressObject) throws IOException {
        final List<File> retrievedFiles = new ArrayList<File>();
        final String area = jobDependency.parseArea().toString();
        final File remoteJobDir = new File(remoteCatalogRootDir, BuildRepository.executionBase(jobDependency));
        final File remoteAreaBase = new File(remoteJobDir, area);
        LOGGER.fine(String.format("LocalJobTransfer.retrieve %s", localAreaDir));
        final int remotePrefixLength = remoteAreaBase.getAbsolutePath().length() + 1;
        final PushingDirectoryTraversal t = new PushingDirectoryTraversal(new FileFilter() {

            int fileCount = 0;

            int dirCount = 0;

            int byteCount = 0;

            public boolean accept(File src) {
                try {
                    final String uri = src.getAbsolutePath().substring(remotePrefixLength);
                    final File dest = new File(localAreaDir, uri);
                    retrievedFiles.add(dest);
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
        t.traverse(remoteAreaBase);
        return retrievedFiles;
    }

    public void store(File localAreaDir, final JobDependency jobDependency, final TransferProgress progressObject) throws IOException {
        final String area = jobDependency.parseArea().toString();
        final String path = jobDependency.getPath();
        final String subpath = path.substring(area.length() + 1);
        if (subpath.length() != 0) {
            throw new UnsupportedOperationException("partial upload is not supported yet: " + jobDependency);
        }
        final File remoteJobDir = new File(remoteCatalogRootDir, BuildRepository.executionBase(jobDependency));
        final File remoteAreaBase = new File(remoteJobDir, area);
        remoteAreaBase.mkdirs();
        if (!remoteAreaBase.isDirectory()) {
            throw new IOException("Failed to create directory: " + remoteAreaBase);
        }
        LOGGER.fine(String.format("LocalJobTransfer.store %s", localAreaDir));
        final File incompleteUploadIndicator = new File(remoteAreaBase + ".part");
        FileUtils.fileWrite(incompleteUploadIndicator, "# presence of this file means that the upload did not finish yet");
        final File listOfFilesFile = new File(remoteAreaBase + BuildRepository.EXTENSION_FILELIST);
        final PrintWriter listOfFiles = new PrintWriter(listOfFilesFile);
        listOfFiles.println("# " + listOfFilesFile);
        final File listOfDirsFile = new File(remoteAreaBase + BuildRepository.EXTENSION_DIRLIST);
        final PrintWriter listOfDirs = new PrintWriter(listOfDirsFile);
        listOfDirs.println("# " + listOfDirsFile);
        final String srcPrefix = localAreaDir.getAbsolutePath() + '/';
        final File remoteContentProperties = new File(remoteAreaBase + BuildRepository.EXTENSION_PROPERTIES);
        final String comment = jobDependency.toString();
        final PushingDirectoryTraversal t = new PushingDirectoryTraversal(new FileFilter() {

            int fileCount = progressObject.getTransferredFiles();

            int dirCount = progressObject.getTransferredDirs();

            long byteCount = progressObject.getTransferredBytes();

            static final long PROPSAVE_BYTE_RATE = 100000;

            long nextpropsave = PROPSAVE_BYTE_RATE;

            public boolean accept(File src) {
                try {
                    final String absPath = src.getAbsolutePath().replace('\\', '/');
                    final String uri = absPath.substring(srcPrefix.length());
                    final File dest = new File(remoteAreaBase, uri);
                    if (src.isDirectory()) {
                        dest.mkdirs();
                        final AtomicInteger fileCnt = new AtomicInteger(0);
                        final AtomicInteger dirCnt = new AtomicInteger(0);
                        final AtomicInteger otherCnt = new AtomicInteger(0);
                        src.listFiles(new FileFilter() {

                            public boolean accept(File pathname) {
                                if (pathname.isDirectory()) {
                                    dirCnt.incrementAndGet();
                                } else if (pathname.isFile()) {
                                    fileCnt.incrementAndGet();
                                } else {
                                    otherCnt.incrementAndGet();
                                }
                                return false;
                            }
                        });
                        if (dirCnt.intValue() == 0) {
                            listOfDirs.println(uri + "/ " + dirCnt + " " + fileCnt + " " + otherCnt);
                            listOfDirs.flush();
                        }
                        dirCount++;
                        progressObject.setTransferredDirs(dirCount);
                    } else {
                        dest.getParentFile().mkdirs();
                        FileUtils.copyFile(src, dest);
                        listOfFiles.println(uri + " " + src.length());
                        listOfFiles.flush();
                        fileCount++;
                        progressObject.setTransferredFiles(fileCount);
                        byteCount += src.length();
                        progressObject.setTransferredBytes(byteCount);
                        if (byteCount > nextpropsave) {
                            saveProgress(remoteContentProperties, progressObject, "--- INCOMPLETE --- " + comment);
                            nextpropsave = byteCount + PROPSAVE_BYTE_RATE;
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException("error while storing " + src, e);
                }
                return false;
            }
        });
        t.setWantDirs(true);
        t.setWantFiles(true);
        t.setSorted(true);
        try {
            listOfFiles.println("# traversing " + localAreaDir);
            listOfFiles.flush();
            t.traverse(localAreaDir);
            listOfFiles.println("# done ");
            listOfFiles.flush();
            listOfDirs.println("# done ");
            listOfDirs.flush();
        } finally {
            listOfFiles.close();
            listOfDirs.close();
        }
        saveProgress(remoteContentProperties, progressObject, comment);
        if (listOfFiles.checkError()) {
            throw new IOException("failure in updating " + listOfFilesFile);
        }
        if (listOfDirs.checkError()) {
            throw new IOException("failure in updating " + listOfDirsFile);
        }
        incompleteUploadIndicator.delete();
    }

    private void loadProgress(File contentProperties, TransferProgress progressObject) throws IOException {
        if (contentProperties.isFile()) {
            final InputStream is = new FileInputStream(contentProperties);
            try {
                final Properties properties = new Properties();
                properties.loadFromXML(is);
                progressObject.setTransferredDirs(Integer.parseInt("0" + properties.get("dirs.totalcount")));
                progressObject.setTransferredFiles(Integer.parseInt("0" + properties.get("files.totalcount")));
                progressObject.setTransferredBytes(Integer.parseInt("0" + properties.get("files.totalsize")));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } finally {
                is.close();
            }
        }
    }

    private static void saveProgress(File propertyFileName, TransferProgress progressObject, String comment) throws IOException {
        final Properties properties = new Properties();
        properties.put("dirs.totalcount", progressObject.getTransferredDirs() + "");
        properties.put("files.totalcount", progressObject.getTransferredFiles() + "");
        properties.put("files.totalsize", progressObject.getTransferredBytes() + "");
        BbxSystemUtils.storeProperties(properties, propertyFileName, comment);
    }
}
