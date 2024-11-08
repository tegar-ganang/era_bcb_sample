package com.aimluck.eip.services.storage.impl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.util.Calendar;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.services.storage.ALStorageHandler;

/**
 *
 */
public class ALDefaultStorageHanlder extends ALStorageHandler {

    private static final JetspeedLogger logger = JetspeedLogFactoryService.getLogger(ALDefaultStorageHanlder.class.getName());

    private static final String EXT_FILENAME = ".txt";

    private static ALStorageHandler instance;

    public static ALStorageHandler getInstance() {
        if (instance == null) {
            instance = new ALDefaultStorageHanlder();
        }
        return instance;
    }

    @Override
    public void saveFile(InputStream is, String folderPath, String filename) {
        File path = new File(folderPath);
        if (!path.exists()) {
            try {
                path.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + path);
            }
        }
        String filepath = path + separator() + filename;
        File file = new File(filepath);
        FileOutputStream os = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            os = new FileOutputStream(filepath);
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } catch (IOException e) {
            logger.error(e, e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Throwable e) {
                }
            }
        }
    }

    /**
   * @param inputStream
   * @param filepath
   */
    @Override
    public void createNewFile(InputStream is, String filepath) {
        File file = new File(filepath);
        if (!file.exists()) {
            try {
                String parent = file.getParent();
                if (parent != null) {
                    File dir = new File(parent);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                }
                file.createNewFile();
            } catch (Exception e) {
                logger.error("Can't create file...:" + file);
            }
        }
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filepath);
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } catch (IOException e) {
            logger.error(e, e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Throwable e) {
                }
            }
        }
    }

    /**
   * @param is
   * @param folderPath
   * @param filename
   */
    @Override
    public void createNewFile(InputStream is, String folderPath, String filename) {
        File path = new File(folderPath);
        if (!path.exists()) {
            try {
                path.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + path);
            }
        }
        String filepath = path + separator() + filename;
        File file = new File(filepath);
        FileOutputStream os = null;
        try {
            file.createNewFile();
            os = new FileOutputStream(filepath);
            int c;
            while ((c = is.read()) != -1) {
                os.write(c);
            }
        } catch (IOException e) {
            logger.error(e, e);
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (Throwable e) {
                }
            }
        }
    }

    /**
   * @param is
   * @param rootPath
   * @param fileName
   */
    @Override
    public void createNewTmpFile(InputStream is, int uid, String dir, String fileName, String realFileName) {
        File path = new File(FOLDER_TMP_FOR_ATTACHMENT_FILES + separator() + Database.getDomainName() + separator() + uid + separator() + dir);
        if (!path.exists()) {
            try {
                path.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + path);
            }
        }
        try {
            String filepath = path + separator() + fileName;
            File file = new File(filepath);
            file.createNewFile();
            FileOutputStream os = new FileOutputStream(filepath);
            int c;
            try {
                while ((c = is.read()) != -1) {
                    os.write(c);
                }
            } catch (IOException e) {
                logger.error(e, e);
            } finally {
                if (os != null) {
                    try {
                        os.flush();
                        os.close();
                    } catch (Throwable e) {
                    }
                }
            }
            PrintWriter w = null;
            try {
                w = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filepath + EXT_FILENAME), "UTF-8"));
                w.println(realFileName);
            } catch (IOException e) {
                logger.error(e, e);
            } finally {
                if (w != null) {
                    try {
                        w.flush();
                        w.close();
                    } catch (Throwable e) {
                    }
                }
            }
        } catch (FileNotFoundException e) {
            logger.error(e, e);
        } catch (IOException e) {
            logger.error(e, e);
        }
    }

    @Override
    public boolean copyFile(String srcRootPath, String srcDir, String srcFileName, String destRootPath, String destDir, String destFileName) {
        File srcPath = new File(srcRootPath + separator() + Database.getDomainName() + separator() + srcDir);
        if (!srcPath.exists()) {
            try {
                srcPath.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + srcPath);
                return false;
            }
        }
        File destPath = new File(destRootPath + separator() + Database.getDomainName() + separator() + destDir);
        if (!destPath.exists()) {
            try {
                destPath.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + destPath);
                return false;
            }
        }
        File from = new File(srcPath + separator() + srcFileName);
        File to = new File(destPath + separator() + destFileName);
        boolean res = true;
        FileChannel srcChannel = null;
        FileChannel destChannel = null;
        try {
            srcChannel = new FileInputStream(from).getChannel();
            destChannel = new FileOutputStream(to).getChannel();
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        } catch (Exception ex) {
            logger.error("Exception", ex);
            res = false;
        } finally {
            if (destChannel != null) {
                try {
                    destChannel.close();
                } catch (IOException ex) {
                    logger.error("Exception", ex);
                    res = false;
                }
            }
            if (srcChannel != null) {
                try {
                    srcChannel.close();
                } catch (IOException ex) {
                    logger.error("Exception", ex);
                    res = false;
                }
            }
        }
        return res;
    }

    /**
   * @param rootPath
   * @param dir
   */
    @Override
    public long getFolderSize(String rootPath, String dir) {
        return getFolderSize(rootPath + separator() + Database.getDomainName() + separator() + dir);
    }

    protected long getFolderSize(String folderPath) {
        if (folderPath == null || folderPath.equals("")) {
            return 0;
        }
        File folder = new File(folderPath);
        if (!folder.exists()) {
            return 0;
        }
        if (folder.isFile()) {
            return getFileSize(folder);
        }
        int fileSizeSum = 0;
        File file = null;
        String[] files = folder.list();
        int length = files.length;
        for (int i = 0; i < length; i++) {
            file = new File(folderPath + separator() + files[i]);
            if (file.isFile()) {
                fileSizeSum += getFileSize(file);
            } else if (file.isDirectory()) {
                fileSizeSum += getFolderSize(file.getAbsolutePath());
            }
        }
        return fileSizeSum;
    }

    @Override
    public long getFileSize(String rootPath, String dir, String filename) {
        return getFileSize(new File(rootPath + separator() + Database.getDomainName() + separator() + dir + separator() + filename));
    }

    protected int getFileSize(File file) {
        if (file == null) {
            return -1;
        }
        FileInputStream fileInputStream = null;
        int size = -1;
        try {
            fileInputStream = new FileInputStream(file);
            BufferedInputStream input = new BufferedInputStream(fileInputStream);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] b = new byte[512];
            int len = -1;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
                output.flush();
            }
            input.close();
            fileInputStream.close();
            byte[] fileArray = output.toByteArray();
            if (fileArray != null) {
                size = fileArray.length;
            } else {
                size = -1;
            }
            output.close();
        } catch (FileNotFoundException e) {
            return -1;
        } catch (IOException ioe) {
            return -1;
        }
        return size;
    }

    @Override
    public boolean deleteFolder(String rootPath, String dir) {
        File file = new File(rootPath + separator() + Database.getDomainName() + separator() + dir);
        if (!file.exists()) {
            return true;
        }
        return deleteFolder(file);
    }

    protected boolean deleteFolder(File folder) {
        if (folder == null) {
            return true;
        }
        String[] files = folder.list();
        if (files == null) {
            folder.delete();
            return true;
        }
        int length = files.length;
        if (length <= 0) {
            folder.delete();
            return true;
        }
        String folderPath = folder.getAbsolutePath() + separator();
        File tmpfile = null;
        for (int i = 0; i < length; i++) {
            tmpfile = new File(folderPath + files[i]);
            if (tmpfile.exists()) {
                if (tmpfile.isFile()) {
                    tmpfile.delete();
                } else if (tmpfile.isDirectory()) {
                    deleteFolder(tmpfile);
                }
            }
        }
        folder.delete();
        return true;
    }

    @Override
    public InputStream getFile(String rootPath, String dir, String fileName) throws FileNotFoundException {
        return getFile(rootPath + separator() + Database.getDomainName() + separator() + dir + separator() + fileName);
    }

    @Override
    public InputStream getFile(String filePath) throws FileNotFoundException {
        return new FileInputStream(filePath);
    }

    @Override
    public String getDocumentPath(String rootPath, String categoryKey) {
        File rootDir = new File(rootPath);
        String org_name = Database.getDomainName();
        if (!rootDir.exists()) {
            try {
                rootDir.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + rootPath);
                return rootDir.getAbsolutePath();
            }
        }
        if (org_name == null) {
            return rootDir.getAbsolutePath();
        }
        File base = null;
        base = new File(rootDir.getAbsolutePath() + separator() + org_name + separator() + categoryKey);
        if (!base.exists()) {
            try {
                base.mkdirs();
            } catch (Exception e) {
                logger.error("Can't create directory...:" + base);
                return base.getAbsolutePath();
            }
        }
        return base.getAbsolutePath();
    }

    /**
   * @return
   */
    @Override
    public String separator() {
        return File.separator;
    }

    /**
   * @param rootPath
   * @param dir
   * @param filename
   * @return
   */
    @Override
    public boolean deleteFile(String rootPath, String dir, String filename) {
        File file = new File(getDocumentPath(rootPath, dir) + separator() + filename);
        if (file != null && file.exists()) {
            file.delete();
        }
        return true;
    }

    @Override
    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file != null && file.exists()) {
            file.delete();
        }
        return true;
    }

    @Override
    public boolean deleteOldFolder(String folderPath, Calendar cal) {
        Calendar mod = Calendar.getInstance();
        boolean flag = true;
        File parent_folder = new File(folderPath);
        try {
            if (!parent_folder.exists()) {
                return false;
            }
            if (parent_folder.isFile()) {
                return false;
            }
            String folders_path[] = parent_folder.list();
            if (folders_path.length == 0) {
                return true;
            }
            int length = folders_path.length;
            for (int i = 0; i < length; i++) {
                File folder = new File(parent_folder.getAbsolutePath() + File.separator + folders_path[i]);
                mod.setTimeInMillis(folder.lastModified());
                if (folder.isDirectory()) {
                    if (!deleteOldFolder(folder.getAbsolutePath(), cal)) {
                        flag = false;
                    } else if (mod.before(cal)) {
                        if (!folder.delete()) {
                            flag = false;
                        }
                    }
                } else {
                    if (mod.before(cal)) {
                        if (!folder.delete()) {
                            flag = false;
                        }
                    } else {
                        flag = false;
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
        return flag;
    }
}
