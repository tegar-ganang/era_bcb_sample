package net.sf.immc.util.file;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件相关操作工具类
 * 
 * @author <b>oxidy</b>, Copyright &#169; 2007-2010
 * @version 0.1,2009/12/28
 */
public class FileUtils {

    private static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * 获取扩展名
     * 
     * @param fileName
     *            文件全名
     * @return 文件扩展名
     */
    public static String getFileExt(String fileName) {
        if (fileName != null && fileName.trim().length() > 0) {
            int i = fileName.lastIndexOf('.');
            if (i > 0 && i < fileName.length() - 1) {
                return fileName.substring(i + 1).toLowerCase();
            }
        }
        return "";
    }

    /**
     * 获取包含扩展名的文件名称。<Br/>
     * 实际上就是路径中的最后一个路径分隔符后的部分。
     * 
     * @param filepath
     *            包含文件完整路径的文件名
     * @return 包含扩展名的文件名称
     * @author zhangyj
     * @version v0.1,2010/01/06
     */
    public static String getFileNameAndExt(String filepath) {
        int point = getPathLastIndex(filepath);
        int length = filepath.length();
        if (point == -1) {
            return filepath;
        } else if (point == length - 1) {
            int secondPoint = getPathLastIndex(filepath, point - 1);
            if (secondPoint == -1) {
                if (length == 1) {
                    return filepath;
                } else {
                    return filepath.substring(0, point);
                }
            } else {
                return filepath.substring(secondPoint + 1, point);
            }
        } else {
            return filepath.substring(point + 1);
        }
    }

    /**
     * 获取不包含扩展名的文件名称
     * 
     * @param filename
     *            文件名
     * @return 不包含扩展名的文件名称
     * @author zhangyj
     * @version v0.1,2010/01/06
     */
    public static String getFileNameNoExt(String filename) {
        int index = filename.lastIndexOf(".");
        if (index != -1) {
            return filename.substring(0, index);
        } else {
            return filename;
        }
    }

    /**
     * 根据文件路径获取不包含扩展名的文件名称
     * 
     * @param filepath
     *            文件路径
     * @return 不包含扩展名的文件名称
     * @author zhangyj
     * @version v0.1,2010/01/06
     */
    public static String getFileNameWithoutExt(String filepath) {
        return getFileNameNoExt(getFileNameAndExt(filepath));
    }

    /**
     * 得到文件的前缀名.
     * 
     * @date 2005-10-18
     * @param fileName
     *            需要处理的文件的名字.
     * @return the prefix portion of the file's name.
     */
    public static String getPrefix(String fileName) {
        if (fileName != null) {
            fileName = fileName.replace('\\', '/');
            if (fileName.lastIndexOf("/") > 0) {
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length());
            }
            int i = fileName.lastIndexOf('.');
            if (i > 0 && i < fileName.length() - 1) {
                return fileName.substring(0, i);
            }
        }
        return "";
    }

    /**
     * 文件重命名
     * 
     * @param path
     *            文件目录
     * @param oldname
     *            原来的文件名
     * @param newname
     *            新文件名
     */
    public static boolean renameFile(String path, String oldname, String newname) {
        if (!oldname.equals(newname)) {
            File oldfile = new File(path + "/" + oldname);
            File newfile = new File(path + "/" + newname);
            if (newfile.exists()) {
                System.out.println(newname + "已经存在！");
                return false;
            } else {
                oldfile.renameTo(newfile);
                return true;
            }
        }
        return false;
    }

    /**
     * 将文件从源位置srFilePath拷贝到目标位置dtFilePath
     * 
     * @param srFilePath
     *            源位置
     * @param dtFilePath
     *            目标位置
     * @author zhangyj
     * @version v0.1,2010/01/05
     */
    public static void copyFile(String srFilePath, String dtFilePath) {
        try {
            File srFile = new File(srFilePath);
            File dtFile = new File(dtFilePath);
            InputStream in = new FileInputStream(srFile);
            OutputStream out = new FileOutputStream(dtFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除单个文件
     * 
     * @param sPath
     *            被删除文件的文件名
     * @return 单个文件删除成功返回true,否则返回false
     * @author zhangyj
     * @version v0.1 2009/12/21 新增
     */
    public static boolean deleteFile(String sPath) {
        boolean flag = false;
        java.io.File file = new File(sPath);
        if (file.isFile() && file.exists()) {
            file.delete();
            flag = true;
        }
        return flag;
    }

    /**
     * 删除目录（文件夹）以及目录下的文件
     * 
     * @param sPath
     *            被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String sPath) {
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }
        File dirFile = new File(sPath);
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }
        boolean flag = true;
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag) break;
            } else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag) break;
            }
        }
        if (!flag) return false;
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据文件夹路径创建文件夹
     * 
     * @param folderPath
     *            传入的文件夹路径
     * @return 成功创建返回true;其他情况返回false;
     * @author zhangyj
     * @version v0.1,2010/01/05,新增<br/>
     *          v0.2,2010/01/15,修正创建多层目录问题
     */
    public static boolean createFolder(String folderPath) {
        boolean flg = false;
        boolean createflg = false;
        folderPath = folderPath.toString();
        folderPath = folderPath.replaceAll("/", "\\\\");
        if (folderPath.trim().length() <= 0) {
            flg = false;
        } else {
            File directory_long = new File(folderPath);
            if (!directory_long.exists()) {
                StringTokenizer st = new StringTokenizer(folderPath, "\\");
                String path1 = st.nextToken() + "\\";
                String path2 = path1;
                while (st.hasMoreTokens()) {
                    path1 = st.nextToken() + "\\";
                    path2 += path1;
                    File directory = new File(path2);
                    if (!directory.exists()) {
                        createflg = directory.mkdir();
                    }
                    if (createflg) {
                        flg = true;
                    }
                }
            } else {
                flg = true;
            }
        }
        return flg;
    }

    /**
     * 根据路径删除指定的目录或文件，无论存在与否
     * 
     * @param sPath
     *            要删除的目录或文件
     * @return 删除成功返回 true，否则返回 false。
     */
    public static boolean DeleteFolder(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        if (!file.exists()) {
            return flag;
        } else {
            if (file.isFile()) {
                return deleteFile(sPath);
            } else {
                return deleteDirectory(sPath);
            }
        }
    }

    /**
     * 判断文件夹是否存在
     * 
     * @param:String folderPath 文件夹名称;
     */
    public static boolean isFolderExists(String folderPath) {
        boolean isexist = false;
        if (folderPath == null || folderPath.trim().length() <= 0) {
        } else {
            folderPath = folderPath.toString();
            java.io.File myFilePath = new java.io.File(folderPath);
            if (myFilePath.exists()) {
                isexist = true;
            }
        }
        return isexist;
    }

    /**
     * 判断文件是否存在
     * 
     */
    public static boolean isFileExists() {
        return false;
    }

    /**
     * 根据文件地址以文件流的形式读取文件，并将文件流转换为byte[]输出。
     * 
     * @param sPath
     *            文件的绝对路径
     * @return byte[]
     * @author zhangyj
     * @version v0.1 2009/12/21 新增
     */
    public static byte[] readInputStream(String sPath) {
        try {
            int readSize;
            File file = new File(sPath);
            if (file.isFile() && file.exists()) {
                InputStream in = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while ((readSize = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, readSize);
                }
                in.close();
                return out.toByteArray();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 得到路径分隔符在文件路径中最后出现的位置。<BR/>
     * 对于DOS或者UNIX风格的分隔符都可以。
     * 
     * @param fileName
     *            文件路径
     * @return 路径分隔符在路径中最后出现的位置，没有出现时返回-1。
     * @since 0.5
     */
    private static int getPathLastIndex(String fileName) {
        int point = fileName.lastIndexOf('/');
        if (point == -1) {
            point = fileName.lastIndexOf('\\');
        }
        return point;
    }

    /**
     * 得到路径分隔符在文件路径中指定位置前最后出现的位置。<BR/>
     * 对于DOS或者UNIX风格的分隔符都可以。
     * 
     * @param filePath
     *            文件路径
     * @param fromIndex
     *            开始查找的位置
     * @return 路径分隔符在路径中指定位置前最后出现的位置，没有出现时返回-1。
     * @author zhangyj
     * @version v0.1,2010/01/06
     */
    private static int getPathLastIndex(String filePath, int fromIndex) {
        int point = filePath.lastIndexOf('/', fromIndex);
        if (point == -1) {
            point = filePath.lastIndexOf('\\', fromIndex);
        }
        return point;
    }
}
