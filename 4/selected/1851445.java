package com.mephi.util;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipUtils;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.log4j.Logger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author OpenGL
 */
public class CompressionUtil implements Compression {

    public enum CompressionAlgoritm {

        ZIP, GZIP
    }

    public static CompressionUtil newInstance(CompressionAlgoritm compressionAlgoritm) {
        return new CompressionUtil(compressionAlgoritm);
    }

    public List<File> decompressFile(File directory, File compressedFile) throws IOException {
        switch(compressionAlgoritm) {
            case ZIP:
                return unzipFile(directory, compressedFile);
            case GZIP:
                return ungzipFile(directory, compressedFile);
            default:
                return null;
        }
    }

    public void compressFile(File file, File compressedFile) throws IOException {
        switch(compressionAlgoritm) {
            case ZIP:
                zipFile(file, compressedFile);
                break;
            case GZIP:
                gzipFile(file, compressedFile);
                break;
            default:
                break;
        }
    }

    public void compressFiles(Set<File> files, File compressedFile) throws IOException {
        switch(compressionAlgoritm) {
            case ZIP:
                zipFiles(files, compressedFile);
                break;
            case GZIP:
                gzipFiles(files, compressedFile);
                break;
            default:
                break;
        }
    }

    public void compressStream(InputStream inStream, String streamName, File compressedFile) throws IOException {
        switch(compressionAlgoritm) {
            case ZIP:
                zipStream(inStream, streamName, compressedFile);
                break;
            case GZIP:
                gzipStream(inStream, streamName, compressedFile);
                break;
            default:
                break;
        }
    }

    public CompressionAlgoritm getCompressionAlgoritm() {
        return compressionAlgoritm;
    }

    public void setCompressionAlgoritm(CompressionAlgoritm compressionAlgoritm) {
        this.compressionAlgoritm = compressionAlgoritm;
    }

    private void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
    }

    private List<File> unzipFile(File directory, File compressedFile) throws IOException {
        ZipFile zf = new ZipFile(compressedFile);
        Enumeration entries = zf.entries();
        List<File> files = new ArrayList<File>();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
                log.warn("ZIP archive contains directories which are being ignored");
                continue;
            }
            String fn = new File(entry.getName()).getName();
            if (fn.startsWith(".")) {
                log.warn("ZIP archive contains a hidden file which is being ignored");
                continue;
            }
            File targetFile = new File(directory, fn);
            files.add(targetFile);
            log.debug("Extracting file: " + entry.getName() + " to: " + targetFile.getAbsolutePath());
            copyInputStream(zf.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(targetFile)));
        }
        zf.close();
        return files;
    }

    private List<File> ungzipFile(File directory, File compressedFile) throws IOException {
        List<File> files = new ArrayList<File>();
        TarArchiveInputStream in = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(compressedFile)));
        try {
            TarArchiveEntry entry = in.getNextTarEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    log.warn("TAR archive contains directories which are being ignored");
                    entry = in.getNextTarEntry();
                    continue;
                }
                String fn = new File(entry.getName()).getName();
                if (fn.startsWith(".")) {
                    log.warn("TAR archive contains a hidden file which is being ignored");
                    entry = in.getNextTarEntry();
                    continue;
                }
                File targetFile = new File(directory, fn);
                if (targetFile.exists()) {
                    log.warn("TAR archive contains duplicate filenames, only the first is being extracted");
                    entry = in.getNextTarEntry();
                    continue;
                }
                files.add(targetFile);
                log.debug("Extracting file: " + entry.getName() + " to: " + targetFile.getAbsolutePath());
                OutputStream fout = new BufferedOutputStream(new FileOutputStream(targetFile));
                InputStream entryIn = new FileInputStream(entry.getFile());
                IOUtils.copy(entryIn, fout);
                fout.close();
                entryIn.close();
            }
        } finally {
            in.close();
        }
        return files;
    }

    private void zipFile(File file, File zipFile) throws IOException {
        Set<File> files = new HashSet<File>();
        files.add(file);
        zipFiles(files, zipFile);
    }

    private void zipFiles(Set<File> files, File zipFile) throws IOException {
        if (files.isEmpty()) {
            log.warn("No files to zip.");
        } else {
            try {
                BufferedInputStream origin = null;
                FileOutputStream dest = new FileOutputStream(zipFile);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                byte data[] = new byte[BUFFER];
                for (File f : files) {
                    log.debug("Adding file " + f + " to archive");
                    FileInputStream fi = new FileInputStream(f);
                    origin = new BufferedInputStream(fi, BUFFER);
                    ZipEntry entry = new ZipEntry(f.getName());
                    out.putNextEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    origin.close();
                }
                out.finish();
                out.close();
            } catch (IOException e) {
                log.error("IOException while zipping files: " + files);
                throw e;
            }
        }
    }

    private void gzipFile(File file, File zipFile) throws IOException {
        Set<File> files = new HashSet<File>();
        files.add(file);
        zipFiles(files, zipFile);
    }

    private void gzipFiles(Set<File> files, File gzipFile) throws IOException {
        if (files.isEmpty()) {
            log.warn("No files to gzip.");
        } else {
            try {
                BufferedInputStream origin = null;
                FileOutputStream dest = new FileOutputStream(gzipFile);
                TarArchiveOutputStream out = new TarArchiveOutputStream(new GzipCompressorOutputStream(dest));
                byte data[] = new byte[BUFFER];
                for (File f : files) {
                    log.debug("Adding file " + f + " to archive");
                    FileInputStream fi = new FileInputStream(f);
                    origin = new BufferedInputStream(fi, BUFFER);
                    TarArchiveEntry entry = new TarArchiveEntry(f.getName());
                    out.putArchiveEntry(entry);
                    int count;
                    while ((count = origin.read(data, 0, BUFFER)) != -1) {
                        out.write(data, 0, count);
                    }
                    origin.close();
                }
                out.finish();
                out.close();
            } catch (IOException e) {
                log.error("IOException while gzipping files: " + files);
                throw e;
            }
        }
    }

    private void zipStream(InputStream inStream, String streamName, File zipFile) throws IOException {
        if (inStream == null) {
            log.warn("No stream to zip.");
        } else {
            try {
                FileOutputStream dest = new FileOutputStream(zipFile);
                ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
                ZipEntry entry = new ZipEntry(streamName);
                out.putNextEntry(entry);
                copyInputStream(inStream, out);
                out.close();
                dest.close();
                inStream.close();
            } catch (IOException e) {
                log.error("IOException while zipping stream");
                throw e;
            }
        }
    }

    private void gzipStream(InputStream inStream, String streamName, File gzipFile) throws IOException {
        if (inStream == null) {
            log.warn("No stream to gzip.");
        } else {
            try {
                FileOutputStream dest = new FileOutputStream(gzipFile);
                TarArchiveOutputStream out = new TarArchiveOutputStream(new GzipCompressorOutputStream(dest));
                TarArchiveEntry entry = new TarArchiveEntry(streamName);
                entry.setSize(2048);
                out.putArchiveEntry(entry);
                copyInputStream(inStream, out);
                out.close();
                dest.close();
                inStream.close();
            } catch (IOException e) {
                log.error("IOException while gzipping stream");
                throw e;
            }
        }
    }

    private CompressionUtil(CompressionAlgoritm compressionAlgoritm) {
        setCompressionAlgoritm(compressionAlgoritm);
    }

    private CompressionAlgoritm compressionAlgoritm;

    private static Logger log = Logger.getLogger("LOG");

    private static final int BUFFER = 2048;
}
