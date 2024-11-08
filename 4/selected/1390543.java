package com.aol.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class FilePackager {

    private long modTime = 0;

    private final byte[] METADATA_MAGIC = new byte[] { 0x50, 0x61, 0x63, 0x6B, 0x61, 0x67, 0x65, 0x72 };

    private final int METADATA_MAGIC_LEN = METADATA_MAGIC.length;

    private Logger logger = Logger.getLogger(FilePackager.class);

    public FilePackager() {
    }

    public FilePackager(long modTime) {
        this.modTime = modTime;
    }

    public static void main(String[] args) throws IOException {
        FilePackager packager = new FilePackager();
        String[] filteredFiles = packager.findFiles(".", args);
        System.err.println("Packaging");
        for (int i = 0; i < filteredFiles.length; i++) System.err.println("\t" + filteredFiles[i]);
        long start = System.currentTimeMillis();
        byte[] data = packager.build(filteredFiles);
        long elapsed = System.currentTimeMillis() - start;
        System.err.println("Elapsed time " + (elapsed / 1000.0) + " sec");
        System.out.write(data);
    }

    public byte[] build(String[] filenames) throws IOException, FileNotFoundException {
        return build(null, filenames);
    }

    public byte[] build(String root, String[] filenames) throws IOException, FileNotFoundException {
        StringBuffer header = new StringBuffer();
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        int num_files = build(root, filenames, header, bytesOut);
        return pkg(num_files, header, bytesOut.toByteArray());
    }

    public int build(String root, String[] filenames, StringBuffer header, ByteArrayOutputStream bytesOut) throws IOException, FileNotFoundException {
        int num_files = 0;
        if (filenames != null) num_files = filenames.length;
        long total_size = 0L;
        for (int i = 0; i < num_files; i++) {
            File f;
            if (root != null) f = new File(root, filenames[i]); else f = new File(filenames[i]);
            if (!f.exists()) throw new FileNotFoundException(filenames[i] + " cannot be found");
            long size = f.length();
            if (size == 0L) System.err.println("File " + filenames[i] + " is empty!");
            FileChannel chan = new RandomAccessFile(f, "r").getChannel();
            size = chan.size();
            ByteBuffer buf = ByteBuffer.allocate((int) size);
            while (buf.hasRemaining()) {
                int read = chan.read(buf);
                if (read == 0) throw new IOException("Read failure: " + f.getPath());
            }
            buf.flip();
            byte data[] = new byte[buf.capacity()];
            buf.get(data, 0, data.length);
            bytesOut.write(data);
            chan.close();
            total_size += size;
            header.append(",").append(filenames[i]).append(",").append(size);
        }
        return num_files;
    }

    public int build(String root, String[] filenames, StringBuffer header, HashMap metadata, ByteArrayOutputStream bytesOut) throws IOException, FileNotFoundException {
        int num_files = 0;
        if (filenames != null) num_files = filenames.length;
        long total_size = 0L;
        for (int i = 0; i < num_files; i++) {
            File f;
            if (root != null) f = new File(root, filenames[i]); else f = new File(filenames[i]);
            if (!f.exists()) throw new FileNotFoundException(filenames[i] + " cannot be found");
            long size = f.length();
            if (size == 0L) System.err.println("File " + filenames[i] + " is empty!");
            FileChannel chan = new RandomAccessFile(f, "r").getChannel();
            size = chan.size();
            ByteBuffer buf = ByteBuffer.allocate((int) size);
            while (buf.hasRemaining()) {
                int read = chan.read(buf);
                if (read == 0) throw new IOException("Read failure: " + f.getPath());
            }
            buf.flip();
            byte data[] = new byte[buf.capacity()];
            buf.get(data, 0, data.length);
            bytesOut.write(data);
            chan.close();
            if (metadata.containsKey(filenames[i])) {
                String md = (String) metadata.get(filenames[i]);
                int md_len = md.length();
                bytesOut.write(md.getBytes("UTF-8"));
                bytesOut.write((byte) (md_len & 0xFF));
                bytesOut.write((byte) ((md_len >> 8) & 0xFF));
                bytesOut.write((byte) ((md_len >> 16) & 0xFF));
                bytesOut.write((byte) ((md_len >> 24) & 0xFF));
                bytesOut.write(METADATA_MAGIC);
                size += md_len + 4 + METADATA_MAGIC_LEN;
            }
            total_size += size;
            header.append(",").append(filenames[i]).append(",").append(size);
        }
        return num_files;
    }

    public byte[] pkg(int num_files, StringBuffer header, byte[] body) {
        String fcount = Integer.toString(num_files);
        header.append("\n");
        byte[] hdr = null;
        try {
            hdr = (fcount + header.toString()).getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("FilePackager.pkg: Error getting UTF-8 bytes for header!: " + e.getMessage());
            return null;
        }
        byte[] buffer = new byte[2 + hdr.length + body.length];
        short hdrLen = (short) hdr.length;
        buffer[0] = (byte) (hdrLen & 0xFF);
        buffer[1] = (byte) ((hdrLen >> 8) & 0xFF);
        System.arraycopy(hdr, 0, buffer, 2, hdrLen);
        System.arraycopy(body, 0, buffer, hdrLen + 2, body.length);
        return buffer;
    }

    public String[] expand(byte[] pkg, String rootDir) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(pkg);
        int header_len = 0;
        while (in.read() != '\n') header_len++;
        in.reset();
        byte[] headbuf = new byte[header_len];
        in.read(headbuf, 0, header_len);
        in.skip(1);
        String head = new String(headbuf);
        int index = head.indexOf(",");
        int num_files = Integer.parseInt(head.substring(0, index));
        String[] outfiles = new String[num_files];
        String[] header = head.substring(index + 1).split(",");
        for (int i = 0; i < num_files; i++) {
            outfiles[i] = rootDir + "/" + header[i * 2];
            int slash = outfiles[i].lastIndexOf("/");
            File path = new File(outfiles[i].substring(0, slash));
            path.mkdirs();
            long size = Long.parseLong(header[i * 2 + 1]);
            byte[] buf = new byte[(int) size];
            int read = in.read(buf, 0, buf.length);
            if (read != buf.length) System.err.println("!!! only read " + read + "/" + buf.length);
            FileOutputStream fos = new FileOutputStream(outfiles[i]);
            fos.write(buf);
            fos.close();
        }
        return outfiles;
    }

    /**
     * (A convenience function) Returns an array of files matching file
     * patterns in 'files' in the directory 'root'.
     */
    public String[] findFiles(final String root, String[] files) {
        return findFiles(root, null, files);
    }

    /**
     * (A convenience function) Returns an array of files matching file
     * patterns in 'files' in the directory 'root/subdir'.
     */
    public String[] findFiles(final String root, String subdir, String[] files) {
        if (files == null) return null;
        ArrayList subdirs = null;
        if (subdir != null) {
            subdirs = new ArrayList();
            subdirs.add(subdir);
        }
        LinkedHashSet filteredFiles = new LinkedHashSet();
        for (int i = 0; i < files.length; i++) {
            String[] filtfiles = findFiles(root, subdirs, files[i]);
            if (filtfiles != null) filteredFiles.addAll(Arrays.asList(filtfiles));
        }
        return (String[]) filteredFiles.toArray(new String[0]);
    }

    /**
     * Returns an array of files matching file pattern 'path' in
     * the subdirectories 'subdirs'.
     * List of subdirectories is assumed not to have any wildcard expressions.
     *
     * @return An array of strings naming the files and directories in the
     * given subdirectories matching the file pattern in path. The array will
     * be empty if the subdirectories are empty or if no files match.
     */
    private String[] findFiles(final String root, final List subdirs, String path) {
        String filePattern = path;
        int slash = path.indexOf("/", 1);
        if (slash != -1) filePattern = path.substring(0, slash);
        FilenameFilter filter;
        if (modTime > 0) filter = new ModSinceRegexFileFilter(filePattern, modTime); else filter = new RegexFileFilter(filePattern);
        ArrayList new_subdirs = new ArrayList();
        if (subdirs == null) {
            String[] matches = new File(root).list(filter);
            if (matches != null) new_subdirs.addAll(Arrays.asList(matches)); else logger.error(root + " is not a directory or an I/O error has occurred");
        } else {
            int num_subdirs = subdirs.size();
            for (int i = 0; i < num_subdirs; i++) {
                String dir = (String) subdirs.get(i);
                String[] matches = new File(root, dir).list(filter);
                if (matches == null) System.err.println(dir + " is not a directory or an I/O error has occurred");
                for (int m = 0; m < matches.length; m++) new_subdirs.add(dir + "/" + matches[m]);
            }
        }
        if (slash == -1) return (String[]) new_subdirs.toArray(new String[0]); else return findFiles(root, new_subdirs, path.substring(slash + 1));
    }

    class RegexFileFilter implements FilenameFilter {

        protected Pattern regex = null;

        protected Pattern wild_star = Pattern.compile("\\*");

        protected Pattern wild_qm = Pattern.compile("\\?");

        protected Pattern wild_dot = Pattern.compile("\\.");

        public RegexFileFilter(final String wildexp) {
            String exp = wildexp;
            exp = wild_dot.matcher(exp).replaceAll("\\\\.");
            exp = wild_star.matcher(exp).replaceAll(".*");
            exp = wild_qm.matcher(exp).replaceAll(".");
            regex = Pattern.compile(exp);
        }

        public boolean accept(final File dir, final String name) {
            if (regex != null) return regex.matcher(name).matches(); else return true;
        }
    }

    class ModSinceRegexFileFilter extends RegexFileFilter {

        protected long modTime = 0;

        public ModSinceRegexFileFilter(final String wildexp, final long modTime) {
            super(wildexp);
            this.modTime = modTime;
        }

        public boolean accept(final File dir, final String name) {
            boolean match = super.accept(dir, name);
            File f = new File(dir, name);
            if (f.isDirectory()) return match;
            if (match) return (f.lastModified() > modTime); else return false;
        }
    }
}
