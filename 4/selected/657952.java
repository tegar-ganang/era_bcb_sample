package com.turnpage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;
import android.util.Log;

public class FileUtils {

    public static final String EXTERNAL_STORAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath();

    public static final String BOOK_STORAGE_DIRECTORY = EXTERNAL_STORAGE_DIRECTORY + File.separator + "epub_book_root";

    public static final String EBK2_BACKUP_DIRECTORY = BOOK_STORAGE_DIRECTORY + File.separator + BOOK_STORAGE_DIRECTORY + "ebk2_root";

    public static final String NETWORK_IMAGE_CACHE_DIRECTORY = BOOK_STORAGE_DIRECTORY + File.separator + "network" + File.separator + "image";

    public static final String SHARE_STORAGE_DIRECTORY = BOOK_STORAGE_DIRECTORY + File.separator + "share_apk";

    public static final String NETWORK_DOWNLOAD_DIRECTORY = BOOK_STORAGE_DIRECTORY + File.separator + "network" + File.separator + "download";

    public static final String LOCAL_IMAGE_DIRECTORY = BOOK_STORAGE_DIRECTORY + File.separator + "local" + File.separator + "image";

    /**
	 * ���̿ռ䲻��
	 */
    public static final int NO_MORE_SPACE = 12;

    /**
	 * ��ȡ�ļ���չ�� Since:2010-11-29
	 * 
	 * @param file
	 * @return
	 */
    public static String getFileExtension(String file) {
        int beginIndex = file.lastIndexOf("/");
        int endIndex = file.lastIndexOf(".");
        if (beginIndex >= 0) {
            if (beginIndex > endIndex) {
                return null;
            } else {
                return file.substring(endIndex + 1);
            }
        } else {
            if (endIndex < 0) {
                return null;
            } else {
                return file.substring(endIndex + 1);
            }
        }
    }

    /**
	 * ��ȡ�ļ��� ��������չ��
	 * 
	 * @param file
	 * @return
	 */
    public static String getFileName(String file) {
        int beginIndex = file.lastIndexOf("/");
        int endIndex = file.lastIndexOf(".");
        if (beginIndex >= 0) {
            if (beginIndex > endIndex) {
                return file.substring(beginIndex + 1).trim();
            } else {
                return file.substring(beginIndex + 1, endIndex).trim();
            }
        } else {
            if (endIndex < 0) {
                return file.trim();
            } else {
                return file.substring(0, endIndex).trim();
            }
        }
    }

    /**
	 * ��ȡ�ļ�ȫ�����չ��
	 * 
	 * @Since:2010-12-18
	 * @param file
	 * @return
	 */
    public static String getFileFullName(String file) {
        int beginIndex = file.lastIndexOf("/");
        if (beginIndex >= 0) {
            return file.substring(beginIndex + 1).trim();
        } else {
            return null;
        }
    }

    public static void printlnBitmap(Bitmap bm, String name) {
        FileOutputStream fos = null;
        try {
            File file = new File("/sdcard/iNetBooks/");
            if (!file.exists()) {
                file.mkdir();
            }
            file = new File("/sdcard/iNetBooks/cache_cover_" + name + ".jpg");
            if (file.exists()) {
                return;
            } else {
                file.createNewFile();
                fos = new FileOutputStream(file);
                bm.compress(CompressFormat.JPEG, 75, fos);
                fos.flush();
                fos.close();
            }
        } catch (Exception e) {
            Log.e("MyLog", e.toString());
        }
    }

    /**
	 * ��ȡ�ļ�����Ŀ¼ Since:2010-11-29
	 * 
	 * @param file
	 * @return
	 */
    public static String getFileDir(String file) {
        int i = file.lastIndexOf(File.separator);
        if (i == -1) {
            return "";
        } else {
            return file.substring(0, i);
        }
    }

    public static String getFileDirectory(String filePath) {
        return getFileDirectory(new File(filePath));
    }

    public static String getFileDirectory(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                return file.getAbsolutePath();
            } else if (file.isFile()) {
                return file.getParent();
            }
        }
        return null;
    }

    /**
	 * //ȡ���ļ���С
	 * 
	 * @author �����
	 * @Since:2010-12-2
	 * @param file
	 * @return
	 * @throws Exception
	 */
    public static long getFileSizes(File file) throws Exception {
        long s = 0;
        if (file.exists()) {
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            s = fis.available();
        } else {
            file.createNewFile();
            System.out.println("�ļ�������");
        }
        return s;
    }

    /**
	 * ȡ���ļ��д�С
	 * 
	 * @author �����
	 * @Since:2010-12-2
	 * @param file
	 * @return
	 * @throws Exception
	 */
    public static long getFileSize(File file) {
        long size = 0;
        File flist[] = file.listFiles();
        for (int i = 0; i < flist.length; i++) {
            if (flist[i].isDirectory()) {
                size = size + getFileSize(flist[i]);
            } else {
                size = size + flist[i].length();
            }
        }
        return size;
    }

    /**
	 * ɾ���ļ���
	 * 
	 * @author �����
	 * @Since:2010-12-3
	 * @param file
	 * @return
	 */
    public static boolean deleteFolder(File file) {
        boolean flag = false;
        if (!file.exists()) {
            return flag;
        } else {
            if (file.isFile()) {
                flag = deleteFile(file);
            } else {
                flag = deleteDirectory(file);
            }
        }
        return flag;
    }

    /**
	 * ɾ����ļ�
	 * 
	 * @author �����
	 * @Since:2010-12-3
	 * @param file
	 * @return
	 */
    public static boolean deleteFile(File file) {
        boolean flag = false;
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
	 * ɾ��Ŀ¼���ļ��У��Լ�Ŀ¼�µ��ļ�
	 * 
	 * @author �����
	 * @Since:2010-12-3
	 * @param file
	 * @return
	 */
    public static boolean deleteDirectory(File file) {
        boolean flag = false;
        if (!file.exists() || !file.isDirectory()) {
            return false;
        }
        flag = true;
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = deleteFile(files[i]);
                if (!flag) break;
            } else {
                flag = deleteDirectory(files[i]);
                if (!flag) break;
            }
        }
        if (!flag) return false;
        if (file.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * �����ļ����ļ���
	 * 
	 * @author �����
	 * @Since:2010-12-15
	 * @param srcFile
	 *            Դ�ļ����ļ���
	 * @param tagFile
	 *            Ŀ���ļ����ļ���
	 * @return
	 * @throws IOException
	 */
    public static boolean CopyFiles(File srcFile, File tagFile) throws IOException, Exception {
        if (srcFile == null || tagFile == null) {
            return false;
        }
        if (srcFile.isDirectory()) {
            if (SDCardUtils.getFreeSize() <= getFileSize(srcFile)) {
            }
            return CopyFolder(srcFile, tagFile);
        } else if (srcFile.isFile()) {
            if (SDCardUtils.getFreeSize() <= getFileSizes(srcFile)) {
            }
            return CopyFile(srcFile, tagFile);
        } else {
            return false;
        }
    }

    /**
	 * �����ļ�
	 * 
	 * @author �����
	 * @Since:2010-12-15
	 * @param srcFile
	 *            Դ�ļ�
	 * @param tagFile
	 *            Ŀ���ļ�
	 * @return
	 * @throws IOException
	 */
    private static boolean CopyFile(File srcFile, File tagFile) throws IOException {
        if (srcFile == null || tagFile == null) {
            return false;
        }
        int length = 2097152;
        File dirFile = new File(getFileFolder(tagFile.getAbsolutePath()));
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        FileInputStream in = new FileInputStream(srcFile);
        FileOutputStream out = new FileOutputStream(tagFile.getAbsolutePath());
        FileChannel inC = in.getChannel();
        FileChannel outC = out.getChannel();
        int i = 0;
        while (true) {
            if (inC.position() == inC.size()) {
                inC.close();
                outC.close();
                break;
            }
            if ((inC.size() - inC.position()) < 20971520) length = (int) (inC.size() - inC.position()); else length = 20971520;
            inC.transferTo(inC.position(), length, outC);
            inC.position(inC.position() + length);
            i++;
        }
        return true;
    }

    private static String getFileFolder(String filePath) {
        return filePath.substring(0, filePath.lastIndexOf("/"));
    }

    /**
	 * �����ļ���
	 * 
	 * @author �����
	 * @Since:2010-12-15
	 * @param srcFolder
	 *            Դ�ļ���
	 * @param tagFolder
	 *            Ŀ���ļ���
	 * @return
	 * @throws IOException
	 */
    private static boolean CopyFolder(File srcFolder, File tagFolder) throws IOException {
        if (srcFolder == null && tagFolder == null) {
            return false;
        }
        if (!tagFolder.exists()) {
            tagFolder.mkdirs();
        }
        File[] files = srcFolder.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isFile()) {
                CopyFile(f, new File(tagFolder.getAbsolutePath() + File.separator + f.getName()));
            } else {
                CopyFolder(f, new File(tagFolder.getAbsolutePath() + File.separator + f.getName()));
            }
        }
        return true;
    }

    public static boolean isFileExist(String filePath) {
        return new File(filePath).exists();
    }
}
