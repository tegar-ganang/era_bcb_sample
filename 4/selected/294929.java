package org.ajaxaio.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author qiangli
 * 
 */
public class FileWalk {

    public interface Visitor {

        public void visitFile(java.io.File file);

        public void visitDirectory(java.io.File file);
    }

    private long numberOfFiles = 0;

    private long originalSize = 0;

    private long compressedSize = 0;

    private double compressionRatio = 0.0;

    public static void main(String[] args) {
        File dir = new File("C:/Apache/jakarta-tomcat-5.0.28/temp/aio9630.js");
        FileWalk fileWalk = new FileWalk();
        fileWalk.walk(dir);
    }

    public void walk(File dir) {
        String[] suffixes = new String[] { ".js" };
        FileFilter filter = new MyFileFilter(suffixes);
        Visitor visitor = new MyVisitor();
        walk(dir, filter, visitor);
        compressionRatio = ((double) compressedSize / (double) originalSize);
        System.out.println("max: " + Integer.MAX_VALUE);
        System.out.println("no: " + numberOfFiles);
        System.out.println("orig size: " + originalSize);
        System.out.println("comp size: " + compressedSize);
        System.out.println("ratio: " + compressionRatio);
    }

    public void walk(File dir, FileFilter filter, Visitor visitor) {
        if (dir.isDirectory()) {
            visitor.visitDirectory(dir);
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                walk(files[i], filter, visitor);
            }
        } else if (dir.isFile()) {
            if (filter.accept(dir)) {
                visitor.visitFile(dir);
            }
        }
    }

    private long gzip(File file) throws Exception {
        InputStream is = null;
        OutputStream os = null;
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            is = new BufferedInputStream(new FileInputStream(file));
            os = new GZIPOutputStream(bos);
            byte[] buf = new byte[4096];
            int nread = -1;
            while ((nread = is.read(buf)) != -1) {
                System.out.println(nread);
                os.write(buf, 0, nread);
            }
            os.flush();
        } finally {
            os.close();
            is.close();
        }
        return bos.size();
    }

    class MyVisitor implements Visitor {

        public void visitDirectory(File file) {
        }

        public void visitFile(File file) {
            try {
                compressedSize += gzip(file);
                numberOfFiles++;
                originalSize += file.length();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class MyFileFilter implements FileFilter {

        String[] suffixes = null;

        MyFileFilter(String[] suffixes) {
            this.suffixes = suffixes;
        }

        public boolean accept(File pathname) {
            String name = pathname.getName().toLowerCase();
            for (int i = 0; i < suffixes.length; i++) {
                if (name.endsWith(suffixes[i])) {
                    return true;
                }
            }
            return false;
        }
    }
}
