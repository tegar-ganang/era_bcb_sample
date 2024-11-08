package com.j2biz.blogunity.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.j2biz.blogunity.BlogunityManager;
import com.j2biz.blogunity.exception.BlogunityException;
import com.j2biz.blogunity.i18n.I18N;
import com.j2biz.blogunity.i18n.I18NStatusFactory;

/**
 * @author michelson
 * @version $$
 * @since 0.1
 * 
 * 
 */
public class ResourceUtils {

    /**
     * Logger for this class
     */
    private static final Log log = LogFactory.getLog(ResourceUtils.class);

    private static final long KB = 1024;

    private static final long MB = 1024 * 1024;

    private static final long GB = 1024 * 1024 * 1024;

    private static DecimalFormat FILESIZE_FORMATER = new DecimalFormat("####.##");

    public static InputStream getResourceAsStream(String url) throws BlogunityException {
        if (log.isDebugEnabled()) {
            log.debug("Getting InputStream for requested resource url=" + url);
        }
        if (url.charAt(0) != '/') url = "/" + url;
        String mainDataDirectory = BlogunityManager.getSystemConfiguration().getDataDir();
        File f = new File(mainDataDirectory, url);
        if (f.exists() && f.isFile() && f.canRead()) {
            FileInputStream in;
            try {
                in = new FileInputStream(f);
                return in;
            } catch (FileNotFoundException e) {
                log.error("getThemeResourceAsStream(String)", e);
                throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.RESOURCE_NOT_FOUND, url));
            }
        }
        throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.RESOURCE_NOT_FOUND, url));
    }

    public static synchronized void copyDirectory(File source, File destination) throws BlogunityException {
        if (source.isDirectory()) {
            if (!destination.exists()) {
                destination.mkdir();
            }
            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyDirectory(new File(source, children[i]), new File(destination, children[i]));
            }
        } else {
            copyFile(source, destination);
        }
    }

    public static synchronized void copyFile(File source, File destination) throws BlogunityException {
        try {
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Throwable t) {
            throw new BlogunityException(I18NStatusFactory.createUnknown(t));
        }
    }

    public static synchronized void copyFile(InputStream in, File destination) throws BlogunityException {
        try {
            OutputStream out = new FileOutputStream(destination);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Throwable t) {
            throw new BlogunityException(I18NStatusFactory.createUnknown(t));
        }
    }

    public static synchronized File zipDirectory(File sourceDirectory) throws BlogunityException {
        return zipDirectory(sourceDirectory, sourceDirectory.getName());
    }

    public static synchronized File zipDirectory(File sourceDirectory, String rootDirName) throws BlogunityException {
        try {
            File zippedFile = new File(BlogunityManager.getSystemConfiguration().getTempDir(), sourceDirectory.getName() + System.currentTimeMillis() + ".zip");
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zippedFile));
            ZipEntry anEntry = new ZipEntry(rootDirName);
            zipDir(sourceDirectory, zos, sourceDirectory.getAbsolutePath(), rootDirName);
            zos.close();
            return zippedFile;
        } catch (Throwable t) {
            throw new BlogunityException(I18NStatusFactory.createUnknown(t));
        }
    }

    private static synchronized void zipDir(File zipDir, ZipOutputStream zos, String absolutePathToThemeDir, String rootDirName) throws IOException {
        String[] dirList = zipDir.list();
        byte[] readBuffer = new byte[2156];
        int bytesIn = 0;
        for (int i = 0; i < dirList.length; i++) {
            File f = new File(zipDir, dirList[i]);
            if (f.isDirectory()) {
                zipDir(f, zos, absolutePathToThemeDir, rootDirName);
                continue;
            }
            FileInputStream fis = new FileInputStream(f);
            String path = (StringUtils.isNotEmpty(rootDirName) ? rootDirName : "") + "/" + f.getAbsolutePath().substring(absolutePathToThemeDir.length() + 1, f.getAbsolutePath().length());
            ZipEntry anEntry = new ZipEntry(path);
            zos.putNextEntry(anEntry);
            while ((bytesIn = fis.read(readBuffer)) != -1) {
                zos.write(readBuffer, 0, bytesIn);
            }
            fis.close();
        }
    }

    public static synchronized void removeDirectory(File dir) throws BlogunityException {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                removeDirectory(new File(dir, children[i]));
            }
        }
        removeFile(dir);
    }

    public static synchronized void removeFile(File file) throws BlogunityException {
        boolean result = file.delete();
        if (!result) {
            throw new BlogunityException(I18NStatusFactory.create(I18N.ERRORS.DELETE_FILE, file.getAbsolutePath()));
        }
    }

    public static synchronized void unzipFile(File zipFile, File destinationDir) throws BlogunityException {
        try {
            ZipInputStream in = new ZipInputStream(new FileInputStream(zipFile));
            unzipFile(in, destinationDir);
            in.close();
        } catch (Throwable t) {
            throw new BlogunityException(I18NStatusFactory.createUnknown(t));
        }
    }

    public static synchronized void unzipFile(ZipInputStream zin, File destinationDir) throws BlogunityException {
        try {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) {
                    new File(destinationDir, e.getName()).mkdir();
                } else {
                    String tempName = e.getName();
                    int indx = tempName.lastIndexOf("/");
                    if (indx > 0) {
                        String dirs = tempName.substring(0, indx);
                        new File(destinationDir, dirs).mkdirs();
                    } else indx = 0;
                    File f = new File(destinationDir, e.getName());
                    f.createNewFile();
                    unzipEntry(zin, f);
                }
            }
            zin.close();
        } catch (Throwable t) {
            throw new BlogunityException(I18NStatusFactory.createUnknown(t));
        }
    }

    private static synchronized void unzipEntry(ZipInputStream zin, File outFile) throws IOException {
        FileOutputStream out = new FileOutputStream(outFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = zin.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
    }

    public static String getPreformattedFilesize(long size) {
        if (size < KB) {
            return size + " Bytes";
        }
        if (size < MB) {
            double kbytes = (double) size / KB;
            return FILESIZE_FORMATER.format(kbytes) + " KB";
        }
        if (size < GB) {
            double gbytes = (double) size / MB;
            return FILESIZE_FORMATER.format(gbytes) + " MB";
        }
        double gbytes = (double) size / GB;
        return FILESIZE_FORMATER.format(gbytes) + " GB";
    }

    /**
     * Calculates size of the given directory/file.
     * 
     * @param resource
     * @return
     */
    public static long calculateDirectorySize(File resource) {
        if (resource.isFile()) return resource.length(); else if (resource.isDirectory()) {
            File[] files = resource.listFiles();
            long size = 0;
            for (int i = 0; i < files.length; i++) {
                size += calculateDirectorySize(files[i]);
            }
            return size;
        } else return 0;
    }
}
