package mh.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**  
 * <pre>  
 * 功能描述：文件操作工具类  
 *           实现文件的创建、删除、复制、压缩、解压以及目录的创建、删除、复制、压缩解压等功能  
 * </pre>  
 * @author 方方        <p>  
 *         Blog:  http://myclover.javaeye.com <p>  
 *             日   期：  2010-07-26 <p>  
 * @version 0.1   <p>  
 * {@code com.myclover.utils.file.FileOperateUtils.java}  
 *   
 */
public class FileOperateUtils {

    /**  
     *   
     * 功能描述：复制单个文件，如果目标文件存在，则不覆盖  
     * @param srcFileName   待复制的文件名  
     * @param descFileName  目标文件名  
     * @return              返回：  
     *                          如果复制成功，则返回true，否则返回false  
     */
    public static boolean copyFile(String srcFileName, String descFileName) {
        return FileOperateUtils.copyFileCover(srcFileName, descFileName, false);
    }

    /**  
     *   
     * 功能描述：复制单个文件  
     * @param srcFileName    待复制的文件名  
     * @param descFileName   目标文件名  
     * @param coverlay        如果目标文件已存在，是否覆盖  
     * @return               返回：  
     *                           如果复制成功，则返回true，否则返回false  
     */
    public static boolean copyFileCover(String srcFileName, String descFileName, boolean coverlay) {
        File srcFile = new File(srcFileName);
        if (!srcFile.exists()) {
            System.out.println("复制文件失败，源文件" + srcFileName + "不存在!");
            return false;
        } else if (!srcFile.isFile()) {
            System.out.println("复制文件失败，" + srcFileName + "不是一个文件!");
            return false;
        }
        File descFile = new File(descFileName);
        if (descFile.exists()) {
            if (coverlay) {
                System.out.println("目标文件已存在，准备删除!");
                if (!FileOperateUtils.delFile(descFileName)) {
                    System.out.println("删除目标文件" + descFileName + "失败!");
                    return false;
                }
            } else {
                System.out.println("复制文件失败，目标文件" + descFileName + "已存在!");
                return false;
            }
        } else {
            if (!descFile.getParentFile().exists()) {
                System.out.println("目标文件所在的目录不存在，创建目录!");
                if (!descFile.getParentFile().mkdirs()) {
                    System.out.println("创建目标文件所在的目录失败!");
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
            System.out.println("复制单个文件" + srcFileName + "到" + descFileName + "成功!");
            return true;
        } catch (Exception e) {
            System.out.println("复制文件失败：" + e.getMessage());
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
     * 功能描述：复制整个目录的内容，如果目标目录存在，则不覆盖  
     * @param srcDirName   源目录名  
     * @param descDirName  目标目录名  
     * @return             返回：  
     *                          如果复制成功返回true，否则返回false  
     */
    public static boolean copyDirectory(String srcDirName, String descDirName) {
        return FileOperateUtils.copyDirectoryCover(srcDirName, descDirName, false);
    }

    /**  
     *   
     * 功能描述：复制整个目录的内容  
     * @param srcDirName   源目录名  
     * @param descDirName  目标目录名  
     * @param coverlay      如果目标目录存在，是否覆盖     
     * @return             返回：  
     *                          如果复制成功返回true，否则返回false  
     */
    public static boolean copyDirectoryCover(String srcDirName, String descDirName, boolean coverlay) {
        File srcDir = new File(srcDirName);
        if (!srcDir.exists()) {
            System.out.println("复制目录失败，源目录" + srcDirName + "不存在!");
            return false;
        } else if (!srcDir.isDirectory()) {
            System.out.println("复制目录失败，" + srcDirName + "不是一个目录!");
            return false;
        }
        if (!descDirName.endsWith(File.separator)) {
            descDirName = descDirName + File.separator;
        }
        File descDir = new File(descDirName);
        if (descDir.exists()) {
            if (coverlay) {
                System.out.println("目标目录已存在，准备删除!");
                if (!FileOperateUtils.delFile(descDirName)) {
                    System.out.println("删除目录" + descDirName + "失败!");
                    return false;
                }
            } else {
                System.out.println("目标目录复制失败，目标目录" + descDirName + "已存在!");
                return false;
            }
        } else {
            System.out.println("目标目录不存在，准备创建!");
            if (!descDir.mkdirs()) {
                System.out.println("创建目标目录失败!");
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
            System.out.println("复制目录" + srcDirName + "到" + descDirName + "失败!");
            return false;
        }
        System.out.println("复制目录" + srcDirName + "到" + descDirName + "成功!");
        return true;
    }

    /**  
     *   
     * 功能描述：删除文件，可以删除单个文件或文件夹  
     * @param fileName   被删除的文件名  
     * @return             返回：  
     *                         如果删除成功，则返回true，否是返回false  
     */
    public static boolean delFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            System.out.println("删除文件失败，" + fileName + "文件不存在!");
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
     * 功能描述：删除单个文件  
     * @param fileName  被删除的文件名  
     * @return          返回：  
     *                      如果删除成功，则返回true，否则返回false  
     */
    public static boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                System.out.println("删除单个文件" + fileName + "成功!");
                return true;
            } else {
                System.out.println("删除单个文件" + fileName + "失败!");
                return false;
            }
        } else {
            System.out.println("删除单个文件失败，" + fileName + "文件不存在!");
            return false;
        }
    }

    /**  
     *   
     * 功能描述：删除目录及目录下的文件  
     * @param dirName  被删除的目录所在的文件路径  
     * @return         返回：  
     *                      如果目录删除成功，则返回true，否则返回false  
     */
    public static boolean deleteDirectory(String dirName) {
        if (!dirName.endsWith(File.separator)) {
            dirName = dirName + File.separator;
        }
        File dirFile = new File(dirName);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            System.out.println("删除目录失败" + dirName + "目录不存在!");
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
            System.out.println("删除目录失败!");
            return false;
        }
        if (dirFile.delete()) {
            System.out.println("删除目录" + dirName + "成功!");
            return true;
        } else {
            System.out.println("删除目录" + dirName + "失败!");
            return false;
        }
    }

    /**  
     *   
     * 功能描述：创建单个文件  
     * @param descFileName  文件名，包含路径  
     * @return              返回：  
     *                          如果创建成功，则返回true，否则返回false  
     */
    public static boolean createFile(String descFileName) {
        File file = new File(descFileName);
        if (file.exists()) {
            System.out.println("文件" + descFileName + "已存在!");
            return false;
        }
        if (descFileName.endsWith(File.separator)) {
            System.out.println(descFileName + "为目录，不能创建目录!");
            return false;
        }
        if (!file.getParentFile().exists()) {
            if (!file.getParentFile().mkdirs()) {
                System.out.println("创建文件所在的目录失败!");
                return false;
            }
        }
        try {
            if (file.createNewFile()) {
                System.out.println(descFileName + "文件创建成功!");
                return true;
            } else {
                System.out.println(descFileName + "文件创建失败!");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(descFileName + "文件创建失败!");
            return false;
        }
    }

    /**  
     *   
     * 功能描述：创建目录  
     * @param descDirName  目录名,包含路径  
     * @return             返回：  
     *                          如果创建成功，则返回true，否则返回false  
     */
    public static boolean createDirectory(String descDirName) {
        if (!descDirName.endsWith(File.separator)) {
            descDirName = descDirName + File.separator;
        }
        File descDir = new File(descDirName);
        if (descDir.exists()) {
            System.out.println("目录" + descDirName + "已存在!");
            return false;
        }
        if (descDir.mkdirs()) {
            System.out.println("目录" + descDirName + "创建成功!");
            return true;
        } else {
            System.out.println("目录" + descDirName + "创建失败!");
            return false;
        }
    }

    /**  
     *   
     * 功能描述：压缩文件或目录  
     * @param srcDirName     压缩的根目录  
     * @param fileName       根目录下的待压缩的文件名或文件夹名，其中*或""表示跟目录下的全部文件  
     * @param descFileName   目标zip文件  
     */
    public static void zipFiles(String srcDirName, String fileName, String descFileName) {
        if (srcDirName == null) {
            System.out.println("文件压缩失败，目录" + srcDirName + "不存在!");
            return;
        }
        File fileDir = new File(srcDirName);
        if (!fileDir.exists() || !fileDir.isDirectory()) {
            System.out.println("文件压缩失败，目录" + srcDirName + "不存在!");
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
            System.out.println(descFileName + "文件压缩成功!");
        } catch (Exception e) {
            System.out.println("文件压缩失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**  
     * 功能描述：解压缩ZIP文件，将ZIP文件里的内容解压到descFileName目录下  
     * @param zipFileName   需要解压的ZIP文件  
     * @param descFileName  目标文件  
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
            System.out.println("文件解压成功!");
        } catch (Exception e) {
            System.out.println("文件解压失败：" + e.getMessage());
        }
    }

    /**  
     *   
     * 功能描述：将目录压缩到ZIP输出流  
     * @param dirPath  目录路径  
     * @param fileDir  文件信息  
     * @param zouts    输出流  
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
     * 功能描述：将文件压缩到ZIP输出流  
     * @param dirPath  目录路径  
     * @param file     文件  
     * @param zouts    输出流  
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
                System.out.println("添加文件" + file.getAbsolutePath() + "到zip文件中!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**  
     *   
     * 功能描述：获取待压缩文件在ZIP文件中entry的名字，即相对于跟目录的相对路径名  
     * @param dirPath  目录名  
     * @param file     entry文件名  
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

    /**
     * 精品课程解压zip文件专用函数
     * 将上传后的第一个文件夹修改成指定文件夹名
     */
    public static boolean reJpkcFolderName(String filePath, String newFileName) {
        File descDir = new File(filePath);
        File rFolder = descDir.listFiles()[0];
        rFolder.renameTo(new File(filePath + newFileName));
        return true;
    }

    /**
     * 获得指定文件夹下的文件名数组
     * @param filePath
     * @return
     */
    public static String getChildFileNameAndReName(String filePath, String rtnFile) {
        StringBuffer rtn = new StringBuffer();
        File descDir = new File(filePath);
        File childFolderList[] = descDir.listFiles();
        File childFolder = null;
        String fileName = "";
        String newName = "";
        for (int i = 0; i < childFolderList.length; i++) {
            childFolder = childFolderList[i];
            fileName = childFolder.getName();
            rtn.append(",").append(rtnFile).append(fileName);
            newName = filePath + "\\" + i + "." + CommonUtil.getFileNameSuffix(fileName);
            childFolder.renameTo(new File(newName));
        }
        return rtn.toString().substring(1);
    }

    public static void main(String args[]) {
        String n = FileOperateUtils.getChildFileNameAndReName("g:\\a", "");
        System.out.println(n);
    }
}
