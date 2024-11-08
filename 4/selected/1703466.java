package net.sf.katta.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;

public class FileUtil {

    private static final Logger LOG = Logger.getLogger(FileUtil.class);

    private static final int BUFFER = 4096;

    public static final FilenameFilter VISIBLE_FILES_FILTER = new FilenameFilter() {

        public boolean accept(final File dir, final String name) {
            return !name.startsWith(".");
        }
    };

    public static void deleteFolder(File folder) {
        try {
            org.apache.hadoop.fs.FileUtil.fullyDelete(folder);
        } catch (IOException e) {
            throw new RuntimeException("could not delete folder '" + folder + "'");
        }
    }

    /**
   * Simply unzips the content from the source zip to the target folder. The
   * first level folder of the zip content is removed.
   */
    public static void unzip(final File sourceZip, final File targetFolder) {
        try {
            targetFolder.mkdirs();
            BufferedOutputStream dest = null;
            final FileInputStream fis = new FileInputStream(sourceZip);
            final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                LOG.debug("Extracting:   " + entry + " from '" + sourceZip.getAbsolutePath() + "'");
                final String entryPath = entry.getName();
                final int indexOf = entryPath.indexOf("/");
                final String cleanUpPath = entryPath.substring(indexOf + 1, entryPath.length());
                final File targetFile = new File(targetFolder, cleanUpPath);
                if (entry.isDirectory()) {
                    targetFile.mkdirs();
                } else {
                    int count;
                    final byte data[] = new byte[BUFFER];
                    final FileOutputStream fos = new FileOutputStream(targetFile);
                    dest = new BufferedOutputStream(fos, BUFFER);
                    while ((count = zis.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.flush();
                    dest.close();
                }
            }
            zis.close();
        } catch (final Exception e) {
            throw new RuntimeException("unable to expand upgrade files", e);
        }
    }

    public static void zip(final File inputFolder, final File outputFile) throws IOException {
        final FileOutputStream fileWriter = new FileOutputStream(outputFile);
        final ZipOutputStream zip = new ZipOutputStream(fileWriter);
        addFolderToZip("", inputFolder, zip);
        zip.flush();
        zip.close();
    }

    private static void addFolderToZip(final String path, final File folder, final ZipOutputStream zip) throws IOException {
        final String zipEnry = path + (path.equals("") ? "" : File.separator) + folder.getName();
        final File[] listFiles = folder.listFiles();
        for (final File file : listFiles) {
            if (file.isDirectory()) {
                addFolderToZip(zipEnry, file, zip);
            } else {
                addFileToZip(zipEnry, file, zip);
            }
        }
    }

    private static void addFileToZip(final String path, final File file, final ZipOutputStream zip) throws IOException {
        final byte[] buffer = new byte[1024];
        int read = -1;
        final FileInputStream in = new FileInputStream(file);
        try {
            final String zipEntry = path + File.separator + file.getName();
            LOG.debug("add zip entry: " + zipEntry);
            zip.putNextEntry(new ZipEntry(zipEntry));
            while ((read = in.read(buffer)) > -1) {
                zip.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    public static void unzipInDfs(FileSystem fileSystem, final Path source, final Path target) {
        try {
            FSDataInputStream dfsInputStream = fileSystem.open(source);
            fileSystem.mkdirs(target);
            final ZipInputStream zipInputStream = new ZipInputStream(dfsInputStream);
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                final String entryPath = entry.getName();
                final int indexOf = entryPath.indexOf("/");
                final String cleanUpPath = entryPath.substring(indexOf + 1, entryPath.length());
                Path path = target;
                if (!cleanUpPath.equals("")) {
                    path = new Path(target, cleanUpPath);
                }
                LOG.info("Extracting: " + entry + " to " + path);
                if (entry.isDirectory()) {
                    fileSystem.mkdirs(path);
                } else {
                    int count;
                    final byte data[] = new byte[4096];
                    FSDataOutputStream fsDataOutputStream = fileSystem.create(path);
                    while ((count = zipInputStream.read(data, 0, 4096)) != -1) {
                        fsDataOutputStream.write(data, 0, count);
                    }
                    fsDataOutputStream.flush();
                    fsDataOutputStream.close();
                }
            }
            zipInputStream.close();
        } catch (final Exception e) {
            LOG.error("can not open zip file", e);
            throw new RuntimeException("unable to expand upgrade files", e);
        }
    }
}
