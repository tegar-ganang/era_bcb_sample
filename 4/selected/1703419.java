package com.gopawpaw.android.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * �ļ�����������
 * @version 2010-12-17
 * @author Jason
 */
public class GppFileUtils {

    /**
	 * ��ȡ�ļ���չ��
	 * @param
	 * @return String
	 */
    public static String getFileExtension(String file) {
        if (file == null) {
            return null;
        }
        int beginIndex = file.lastIndexOf(File.separator);
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
	 * ��ȡ�ļ���,������չ��
	 * @param file
	 * @return
	 */
    public static String getFileName(String file) {
        if (file == null) {
            return null;
        }
        int beginIndex = file.lastIndexOf(File.separator);
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

    /**
	 * ��ȡ�ļ�����Ŀ¼���·��
	 * @param file
	 * @return
	 */
    public static String getFileDirectory(String file) {
        if (file == null) {
            return null;
        }
        int endIndex = file.lastIndexOf(File.separator);
        if (endIndex < 0) {
            return null;
        } else {
            return file.substring(0, endIndex);
        }
    }

    /**
	 * ��ȡ�ļ���С
	 * @param file
	 * @return
	 */
    public static long getFileSize(File file) {
        long size = 0;
        if (file == null || !file.exists()) {
            return size;
        }
        if (file.isFile()) {
            size = file.length();
        } else if (file.isDirectory()) {
            File flist[] = file.listFiles();
            for (int i = 0; i < flist.length; i++) {
                if (flist[i].isDirectory()) {
                    size = size + getFileSize(flist[i]);
                } else {
                    size = size + flist[i].length();
                }
            }
        }
        return size;
    }

    /**
	 * ɾ���ļ�,�����ļ����ļ���
	 * @param file
	 * @return
	 */
    public static boolean deleteFiles(File file) {
        boolean flag = false;
        if (file == null || !file.exists()) {
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
	 * @param file
	 * @return
	 */
    private static boolean deleteFile(File file) {
        boolean flag = false;
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
	 * ɾ��Ŀ¼���ļ��У��Լ�Ŀ¼�µ��ļ�
	 * @param file
	 * @return
	 */
    private static boolean deleteDirectory(File file) {
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
     * @param srcFile Դ�ļ����ļ���
     * @param tagFile Ŀ���ļ����ļ���
     * @return
     * @throws IOException
     */
    public static boolean copyFiles(File srcFile, File tagFile) throws IOException {
        if (srcFile == null || tagFile == null) {
            return false;
        }
        if (srcFile.isDirectory()) {
            return copyFolder(srcFile, tagFile);
        } else if (srcFile.isFile()) {
            return copyFile(srcFile, tagFile);
        } else {
            return false;
        }
    }

    /**
     * �����ļ�
     * @param srcFile Դ�ļ�
     * @param tagFile Ŀ���ļ�
     * @return
     * @throws IOException 
     */
    private static boolean copyFile(File srcFile, File tagFile) throws IOException {
        if (srcFile == null || tagFile == null) {
            return false;
        }
        int length = 2097152;
        FileInputStream in = new FileInputStream(srcFile);
        FileOutputStream out = new FileOutputStream(tagFile);
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

    /**
     * �����ļ���
     * @param srcFolder Դ�ļ���
     * @param tagFolder Ŀ���ļ���
     * @return
     * @throws IOException 
     */
    private static boolean copyFolder(File srcFolder, File tagFolder) throws IOException {
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
                copyFile(f, new File(tagFolder.getAbsolutePath() + File.separator + f.getName()));
            } else {
                copyFolder(f, new File(tagFolder.getAbsolutePath() + File.separator + f.getName()));
            }
        }
        return true;
    }

    /**
     * �ƶ��ļ�
     * @param
     * @return boolean
     * @throws IOException 
     */
    public static boolean moveFiles(File srcFile, File tagFile) throws IOException {
        if (copyFiles(srcFile, tagFile)) {
            return deleteFiles(srcFile);
        } else {
            return false;
        }
    }
}
