package com.hangar.fileupdate.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**  
 * <pre>  
 * �����������ļ�����������  
 *           ʵ���ļ��Ĵ�����ɾ���ơ�ѹ������ѹ�Լ�Ŀ¼�Ĵ�����ɾ���ơ�ѹ����ѹ�ȹ���  
 * </pre>  
 * @author ����        <p>  
 *         Blog:  http://myclover.javaeye.com <p>  
 *             ��   �ڣ�  2010-07-26 <p>  
 * @version 0.1   <p>  
 * {@code com.myclover.utils.file.FileOperateUtils.java}  
 *   
 */
public class FileOperateUtils {

    /**  
     *   
     * �������������Ƶ����ļ������Ŀ���ļ����ڣ��򲻸���  
     * @param srcFileName   ���Ƶ��ļ���  
     * @param descFileName  Ŀ���ļ���  
     * @return              ���أ�  
     *                          ����Ƴɹ����򷵻�true�����򷵻�false  
     */
    public static boolean copyFile(String srcFileName, String descFileName) {
        return FileOperateUtils.copyFileCover(srcFileName, descFileName, false);
    }

    /**  
     *   
     * �������������Ƶ����ļ�  
     * @param srcFileName    ���Ƶ��ļ���  
     * @param descFileName   Ŀ���ļ���  
     * @param coverlay        ���Ŀ���ļ��Ѵ��ڣ��Ƿ񸲸�  
     * @return               ���أ�  
     *                           ����Ƴɹ����򷵻�true�����򷵻�false  
     */
    public static boolean copyFileCover(String srcFileName, String descFileName, boolean coverlay) {
        File srcFile = new File(srcFileName);
        if (!srcFile.exists()) {
            System.out.println("�����ļ�ʧ�ܣ�Դ�ļ�" + srcFileName + "������!");
            return false;
        } else if (!srcFile.isFile()) {
            System.out.println("�����ļ�ʧ�ܣ�" + srcFileName + "����һ���ļ�!");
            return false;
        }
        File descFile = new File(descFileName);
        if (descFile.exists()) {
            if (coverlay) {
                System.out.println("Ŀ���ļ��Ѵ��ڣ�׼��ɾ��!");
                if (!FileOperateUtils.delFile(descFileName)) {
                    System.out.println("ɾ��Ŀ���ļ�" + descFileName + "ʧ��!");
                    return false;
                }
            } else {
                System.out.println("�����ļ�ʧ�ܣ�Ŀ���ļ�" + descFileName + "�Ѵ���!");
                return false;
            }
        } else {
            if (!descFile.getParentFile().exists()) {
                System.out.println("Ŀ���ļ����ڵ�Ŀ¼�����ڣ�����Ŀ¼!");
                if (!descFile.getParentFile().mkdirs()) {
                    System.out.println("����Ŀ���ļ����ڵ�Ŀ¼ʧ��!");
                    return false;
                }
            }
        }
        int readByte = 0;
        InputStream ins = null;
        OutputStream outs = null;
        try {
            ins = new FileInputStream(srcFile);
            outs = new FileOutputStream(descFile);
            byte[] buf = new byte[1024];
            while ((readByte = ins.read(buf)) != -1) {
                outs.write(buf, 0, readByte);
            }
            System.out.println("���Ƶ����ļ�" + srcFileName + "��" + descFileName + "�ɹ�!");
            return true;
        } catch (Exception e) {
            System.out.println("�����ļ�ʧ�ܣ�" + e.getMessage());
            return false;
        } finally {
            if (outs != null) {
                try {
                    outs.close();
                } catch (IOException oute) {
                    oute.printStackTrace();
                }
            }
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ine) {
                    ine.printStackTrace();
                }
            }
        }
    }

    /**  
     *   
     * �����������������Ŀ¼�����ݣ����Ŀ��Ŀ¼���ڣ��򲻸���  
     * @param srcDirName   ԴĿ¼��  
     * @param descDirName  Ŀ��Ŀ¼��  
     * @return             ���أ�  
     *                          ����Ƴɹ�����true�����򷵻�false  
     */
    public static boolean copyDirectory(String srcDirName, String descDirName) {
        return FileOperateUtils.copyDirectoryCover(srcDirName, descDirName, false);
    }

    /**  
     *   
     * �����������������Ŀ¼������  
     * @param srcDirName   ԴĿ¼��  
     * @param descDirName  Ŀ��Ŀ¼��  
     * @param coverlay      ���Ŀ��Ŀ¼���ڣ��Ƿ񸲸�     
     * @return             ���أ�  
     *                          ����Ƴɹ�����true�����򷵻�false  
     */
    public static boolean copyDirectoryCover(String srcDirName, String descDirName, boolean coverlay) {
        File srcDir = new File(srcDirName);
        if (!srcDir.exists()) {
            System.out.println("����Ŀ¼ʧ�ܣ�ԴĿ¼" + srcDirName + "������!");
            return false;
        } else if (!srcDir.isDirectory()) {
            System.out.println("����Ŀ¼ʧ�ܣ�" + srcDirName + "����һ��Ŀ¼!");
            return false;
        }
        if (!descDirName.endsWith(File.separator)) {
            descDirName = descDirName + File.separator;
        }
        File descDir = new File(descDirName);
        if (descDir.exists()) {
            if (coverlay) {
                System.out.println("Ŀ��Ŀ¼�Ѵ��ڣ�׼��ɾ��!");
                if (!FileOperateUtils.delFile(descDirName)) {
                    System.out.println("ɾ��Ŀ¼" + descDirName + "ʧ��!");
                    return false;
                }
            } else {
                System.out.println("Ŀ��Ŀ¼����ʧ�ܣ�Ŀ��Ŀ¼" + descDirName + "�Ѵ���!");
                return false;
            }
        } else {
            System.out.println("Ŀ��Ŀ¼�����ڣ�׼������!");
            if (!descDir.mkdirs()) {
                System.out.println("����Ŀ��Ŀ¼ʧ��!");
                return false;
            }
        }
        boolean flag = true;
        File[] files = srcDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = FileOperateUtils.copyFile(files[i].getAbsolutePath(), descDirName + files[i].getName());
                if (!flag) {
                    break;
                }
            }
            if (files[i].isDirectory()) {
                flag = FileOperateUtils.copyDirectory(files[i].getAbsolutePath(), descDirName + files[i].getName());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            System.out.println("����Ŀ¼" + srcDirName + "��" + descDirName + "ʧ��!");
            return false;
        }
        System.out.println("����Ŀ¼" + srcDirName + "��" + descDirName + "�ɹ�!");
        return true;
    }

    /**  
     *   
     * ����������ɾ���ļ�������ɾ����ļ����ļ���  
     * @param fileName   ��ɾ����ļ���  
     * @return             ���أ�  
     *                         ���ɾ��ɹ����򷵻�true�����Ƿ���false  
     */
    public static boolean delFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("ɾ���ļ�ʧ�ܣ�" + fileName + "�ļ�������!");
            return false;
        } else {
            if (file.isFile()) {
                return FileOperateUtils.deleteFile(fileName);
            } else {
                return FileOperateUtils.deleteDirectory(fileName);
            }
        }
    }

    /**  
     *   
     * ����������ɾ����ļ�  
     * @param fileName  ��ɾ����ļ���  
     * @return          ���أ�  
     *                      ���ɾ��ɹ����򷵻�true�����򷵻�false  
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("ɾ����ļ�" + fileName + "�ɹ�!");
                return true;
            } else {
                System.out.println("ɾ����ļ�" + fileName + "ʧ��!");
                return false;
            }
        } else {
            System.out.println("ɾ����ļ�ʧ�ܣ�" + fileName + "�ļ�������!");
            return false;
        }
    }

    /**  
     *   
     * ����������ɾ��Ŀ¼��Ŀ¼�µ��ļ�  
     * @param dirName  ��ɾ���Ŀ¼���ڵ��ļ�·��  
     * @return         ���أ�  
     *                      ���Ŀ¼ɾ��ɹ����򷵻�true�����򷵻�false  
     */
    public static boolean deleteDirectory(String dirName) {
        if (!dirName.endsWith(File.separator)) {
            dirName = dirName + File.separator;
        }
        File dirFile = new File(dirName);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            System.out.println("ɾ��Ŀ¼ʧ��" + dirName + "Ŀ¼������!");
            return false;
        }
        boolean flag = true;
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = FileOperateUtils.deleteFile(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            } else if (files[i].isDirectory()) {
                flag = FileOperateUtils.deleteDirectory(files[i].getAbsolutePath());
                if (!flag) {
                    break;
                }
            }
        }
        if (!flag) {
            System.out.println("ɾ��Ŀ¼ʧ��!");
            return false;
        }
        if (dirFile.delete()) {
            System.out.println("ɾ��Ŀ¼" + dirName + "�ɹ�!");
            return true;
        } else {
            System.out.println("ɾ��Ŀ¼" + dirName + "ʧ��!");
            return false;
        }
    }

    /**  
     *   
     * �������������������ļ�  
     * @param descFileName  �ļ����·��  
     * @return              ���أ�  
     *                          ����ɹ����򷵻�true�����򷵻�false  
     */
    public static boolean createFile(String descFileName) {
        File file = new File(descFileName);
        if (file.exists()) {
            System.out.println("�ļ�" + descFileName + "�Ѵ���!");
            return false;
        }
        if (descFileName.endsWith(File.separator)) {
            System.out.println(descFileName + "ΪĿ¼�����ܴ���Ŀ¼!");
            return false;
        }
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                System.out.println("�����ļ����ڵ�Ŀ¼ʧ��!");
                return false;
            }
        }
        try {
            if (file.createNewFile()) {
                System.out.println(descFileName + "�ļ������ɹ�!");
                return true;
            } else {
                System.out.println(descFileName + "�ļ�����ʧ��!");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(descFileName + "�ļ�����ʧ��!");
            return false;
        }
    }

    /**  
     *   
     * ��������������Ŀ¼  
     * @param descDirName  Ŀ¼��,��·��  
     * @return             ���أ�  
     *                          ����ɹ����򷵻�true�����򷵻�false  
     */
    public static boolean createDirectory(String descDirName) {
        if (!descDirName.endsWith(File.separator)) {
            descDirName = descDirName + File.separator;
        }
        File descDir = new File(descDirName);
        if (descDir.exists()) {
            System.out.println("Ŀ¼" + descDirName + "�Ѵ���!");
            return false;
        }
        if (descDir.mkdirs()) {
            System.out.println("Ŀ¼" + descDirName + "�����ɹ�!");
            return true;
        } else {
            System.out.println("Ŀ¼" + descDirName + "����ʧ��!");
            return false;
        }
    }

    /**  
     *   
     * ����������ѹ���ļ���Ŀ¼  
     * @param srcDirName     ѹ���ĸ�Ŀ¼  
     * @param fileName       ��Ŀ¼�µĴ�ѹ�����ļ�����ļ���������*��""��ʾ��Ŀ¼�µ�ȫ���ļ�  
     * @param descFileName   Ŀ��zip�ļ�  
     */
    public static void zipFiles(String srcDirName, String fileName, String descFileName) {
        if (srcDirName == null) {
            System.out.println("�ļ�ѹ��ʧ�ܣ�Ŀ¼" + srcDirName + "������!");
            return;
        }
        File fileDir = new File(srcDirName);
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            System.out.println("�ļ�ѹ��ʧ�ܣ�Ŀ¼" + srcDirName + "������!");
            return;
        }
        String dirPath = fileDir.getAbsolutePath();
        File descFile = new File(descFileName);
        try {
            ZipOutputStream zouts = new ZipOutputStream(new FileOutputStream(descFile));
            if ("*".equals(fileName) || "".equals(fileName)) {
                FileOperateUtils.zipDirectoryToZipFile(dirPath, fileDir, zouts);
            } else {
                File file = new File(fileDir, fileName);
                if (file.isFile()) {
                    FileOperateUtils.zipFilesToZipFile(dirPath, file, zouts);
                } else {
                    FileOperateUtils.zipDirectoryToZipFile(dirPath, file, zouts);
                }
            }
            zouts.close();
            System.out.println(descFileName + "�ļ�ѹ���ɹ�!");
        } catch (Exception e) {
            System.out.println("�ļ�ѹ��ʧ�ܣ�" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**  
     * ������������ѹ��ZIP�ļ�����ZIP�ļ�������ݽ�ѹ��descFileNameĿ¼��  
     * @param zipFileName   ��Ҫ��ѹ��ZIP�ļ�  
     * @param descFileName  Ŀ���ļ�  
     */
    public static void unZipFiles(String zipFileName, String descFileName) {
        if (!descFileName.endsWith(File.separator)) {
            descFileName = descFileName + File.separator;
        }
        try {
            ZipFile zipFile = new ZipFile(zipFileName);
            ZipEntry entry = null;
            String entryName = null;
            String descFileDir = null;
            byte[] buf = new byte[4096];
            int readByte = 0;
            Enumeration enums = zipFile.entries();
            while (enums.hasMoreElements()) {
                entry = (ZipEntry) enums.nextElement();
                entryName = entry.getName();
                descFileDir = descFileName + entryName;
                if (entry.isDirectory()) {
                    new File(descFileDir).mkdirs();
                    continue;
                } else {
                    new File(descFileDir).getParentFile().mkdirs();
                }
                File file = new File(descFileDir);
                FileOutputStream fouts = new FileOutputStream(file);
                InputStream ins = zipFile.getInputStream(entry);
                while ((readByte = ins.read(buf)) != -1) {
                    fouts.write(buf, 0, readByte);
                }
                fouts.close();
                ins.close();
            }
            System.out.println("�ļ���ѹ�ɹ�!");
        } catch (Exception e) {
            System.out.println("�ļ���ѹʧ�ܣ�" + e.getMessage());
        }
    }

    /**  
     *   
     * ������������Ŀ¼ѹ����ZIP�����  
     * @param dirPath  Ŀ¼·��  
     * @param fileDir  �ļ���Ϣ  
     * @param zouts    �����  
     */
    public static void zipDirectoryToZipFile(String dirPath, File fileDir, ZipOutputStream zouts) {
        if (fileDir.isDirectory()) {
            File[] files = fileDir.listFiles();
            if (files.length == 0) {
                ZipEntry entry = new ZipEntry(getEntryName(dirPath, fileDir));
                try {
                    zouts.putNextEntry(entry);
                    zouts.closeEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile()) {
                    FileOperateUtils.zipFilesToZipFile(dirPath, files[i], zouts);
                } else {
                    FileOperateUtils.zipDirectoryToZipFile(dirPath, files[i], zouts);
                }
            }
        }
    }

    /**  
     *   
     * �������������ļ�ѹ����ZIP�����  
     * @param dirPath  Ŀ¼·��  
     * @param file     �ļ�  
     * @param zouts    �����  
     */
    public static void zipFilesToZipFile(String dirPath, File file, ZipOutputStream zouts) {
        FileInputStream fin = null;
        ZipEntry entry = null;
        byte[] buf = new byte[4096];
        int readByte = 0;
        if (file.isFile()) {
            try {
                fin = new FileInputStream(file);
                entry = new ZipEntry(getEntryName(dirPath, file));
                zouts.putNextEntry(entry);
                while ((readByte = fin.read(buf)) != -1) {
                    zouts.write(buf, 0, readByte);
                }
                zouts.closeEntry();
                fin.close();
                System.out.println("����ļ�" + file.getAbsolutePath() + "��zip�ļ���!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**  
     *   
     * ������������ȡ��ѹ���ļ���ZIP�ļ���entry�����֣�������ڸ�Ŀ¼�����·����  
     * @param dirPath  Ŀ¼��  
     * @param file     entry�ļ���  
     * @return  
     */
    private static String getEntryName(String dirPath, File file) {
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath + File.separator;
        }
        String filePath = file.getAbsolutePath();
        if (file.isDirectory()) {
            filePath += "/";
        }
        int index = filePath.indexOf(dirPath);
        return filePath.substring(index + dirPath.length());
    }
}
