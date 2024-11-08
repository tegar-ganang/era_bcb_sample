package org.sharp.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.sharp.intf.Logger;

public class IOUtils {

    private static Logger logger;

    public static void setLogger(Logger log) {
        logger = log;
    }

    public static Logger consoleLogger() {
        return new Logger() {

            @Override
            public void log(String msg) {
                System.out.println(msg);
            }

            @Override
            public void log(String msg, Throwable e) {
                e.printStackTrace();
            }
        };
    }

    public static String relativePath(String fullPath, String prefix) {
        if (fullPath != null && !fullPath.startsWith(prefix)) return fullPath;
        String after = StringUtils.substringAfter(fullPath, prefix);
        String ret = StringUtils.removeStart(after, "/");
        return ret;
    }

    public static String fullPath(String prefix, String relativePath) {
        String pre = StringUtils.removeEnd(prefix, "/");
        String rela = StringUtils.removeStart(relativePath, "/");
        return pre + "/" + rela;
    }

    public static File[] findMp3Files(String dir) {
        final List<File> fl = new ArrayList<File>();
        HashMap<String, Integer> hm = new HashMap<String, Integer>();
        BaseFileHandler fh = null;
        new IOUtils().travDir(dir, fh = new BaseFileHandler(fl) {

            public void handle(File file) {
                fc++;
                if (FilenameUtils.isExtension(file.getName(), new String[] { "mp3", "MP3", "mP3", "Mp3" })) fl.add(file);
            }
        });
        fh.summary(hm);
        Collections.sort(fl, new Comparator<File>() {

            @Override
            public int compare(File f0, File f1) {
                return f0.compareTo(f1);
            }
        });
        return fl.toArray(new File[0]);
    }

    private void travDir(String s, FileHandler fh) {
        File f = new File(s);
        if (!f.exists()) {
            return;
        }
        if (f.isFile()) fh.handle(f); else if (f.isDirectory()) {
            fh.handleDir(f);
            String objects[] = f.list();
            if (objects == null) return;
            for (int i = 0; i < objects.length; i++) travDir(s + File.separator + objects[i], fh);
        }
    }

    public static void downFile(String urL, String path) {
        URL url;
        try {
            logger.log("downloading file from:" + urL + " save to:" + path);
            url = new URL(urL);
            FileUtils.copyURLToFile(url, new File(path));
        } catch (Exception e) {
            logger.log("downFile exception:", e);
        }
    }

    public static class DownloadInfo {

        public int fileSize;

        public int downloadedBytes;

        public String errorMsg;

        public String filePath;

        public Exception exception;

        public static final int UNKNOWN_CONTENT_LENGTH = -1;
    }

    public static interface DownloadInfoReceiver {

        void startReceive(DownloadInfo di);

        void newBytesSaved(DownloadInfo di);

        void complete(DownloadInfo di);

        void errorOccur(DownloadInfo di);

        void canceled(DownloadInfo downInfo);
    }

    public static void downFile(String urL, String path, DownloadInfoReceiver dlr, AtomicBoolean cancelFlag) {
        DownloadInfo downInfo = new DownloadInfo();
        try {
            logger.log("downloading file from:" + urL + " save to:" + path);
            URL uRL = new URL(urL);
            URLConnection conn = uRL.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            downInfo.fileSize = conn.getContentLength();
            if (downInfo.fileSize <= 0) {
                logger.log("unknown content length");
                downInfo.fileSize = DownloadInfo.UNKNOWN_CONTENT_LENGTH;
            }
            dlr.startReceive(downInfo);
            if (is == null) throw new RuntimeException("stream is null");
            File file = new File(path);
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            byte buf[] = new byte[1024];
            downInfo.downloadedBytes = 0;
            do {
                int numread = is.read(buf);
                if (numread == -1) {
                    break;
                }
                fos.write(buf, 0, numread);
                downInfo.downloadedBytes += numread;
                dlr.newBytesSaved(downInfo);
            } while (!cancelFlag.get());
            fos.close();
            if (!cancelFlag.get()) {
                downInfo.filePath = path;
                dlr.complete(downInfo);
                logger.log("finish downloading file from:" + urL + " save to:" + path);
            } else {
                boolean deleted = file.delete();
                dlr.canceled(downInfo);
                logger.log("download canceld by user,file " + path + (deleted ? " deleted" : " not deleted"));
            }
            is.close();
        } catch (Exception e) {
            logger.log("error: " + e.getMessage(), e);
            downInfo.errorMsg = e.getMessage();
            dlr.errorOccur(downInfo);
        }
    }

    public static interface UnzipEventReceiver {

        void startUnzip();

        void completeUnzip();

        void errorUnzip(Exception e);

        void canceled();
    }

    public static void unzip(File zipFile, File destination, UnzipEventReceiver uer, AtomicBoolean cancelFlag) {
        if (!destination.exists()) destination.mkdirs();
        if (!destination.isDirectory()) throw new IllegalArgumentException();
        ZipInputStream in = null;
        OutputStream out = null;
        try {
            try {
                in = new ZipInputStream(new FileInputStream(zipFile));
                uer.startUnzip();
                logger.log("start to unzip file " + zipFile.getPath() + " to " + destination.getPath());
                ZipEntry entry = null;
                while ((entry = in.getNextEntry()) != null && !cancelFlag.get()) {
                    String outFilename = entry.getName();
                    outFilename = FilenameUtils.normalize(outFilename);
                    if (entry.isDirectory()) {
                        new File(destination, outFilename).mkdirs();
                    } else {
                        File file = new File(destination, outFilename);
                        file.getParentFile().mkdirs();
                        out = new FileOutputStream(file);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                        out.close();
                    }
                }
                if (!cancelFlag.get()) {
                    logger.log("finish unzipping file " + zipFile.getPath());
                    uer.completeUnzip();
                } else {
                    uer.canceled();
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            logger.log("error unzipping", e);
            uer.errorUnzip(e);
        }
    }

    public static void zip(File file, File zip) throws IOException {
        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(zip));
            new IOUtils().travDir(file.getPath(), new FileHandler() {

                @Override
                public void handleDir(File dir) {
                }

                @Override
                public void handle(File file) {
                }
            });
        } finally {
        }
    }

    public static Object fromString(String str, Class clazz) {
        Object o = null;
        if (Double.class.equals(clazz) || Double.TYPE.equals(clazz)) {
            o = Double.parseDouble(str);
        } else if (Float.class.equals(clazz) || Float.TYPE.equals(clazz)) {
            o = Float.parseFloat(str);
        } else if (Integer.class.equals(clazz) || Integer.TYPE.equals(clazz)) {
            o = Integer.parseInt(str);
        } else if (Long.class.equals(clazz) || Long.TYPE.equals(clazz)) {
            o = Long.parseLong(str);
        } else if (Short.class.equals(clazz) || Short.TYPE.equals(clazz)) {
            o = Short.parseShort(str);
        } else if (Byte.class.equals(clazz) || Byte.TYPE.equals(clazz)) {
            o = Byte.parseByte(str);
        } else if (Boolean.class.equals(clazz) || Boolean.TYPE.equals(clazz)) {
            o = Boolean.parseBoolean(str);
        } else if (String.class.equals(clazz)) {
            o = str;
        }
        return o;
    }
}
