package com.zhiyun.estore.common.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.FileSet;

/**
 * 对文件和文件夹的处理<br>
 * 管理整个系统中的文件，上传下载，文件路径的确定<br>
 * 文件读，写<br>
 * 文件流拷贝<br>
 * 文件浏览<br>
 * 文件域管理等功能实现<br>
 * 
 * @author yinshuwei
 */
public class ExFileUtils {

    /**
	 * 将输入流的内容拷贝到输出流<br>
	 * 如来前台获得了一个流(输入流)：<br>
	 * ServletInputStream inputStream = request.getInputStream();<br>
	 * 有必要放入输出流，作为响应返回前台：<br>
	 * OutputStream outputStream = response.getOutputStream();<br>
	 * FileTools.copyStream(outputStream, inputStream);<br>
	 * 或,保存到文件：<br>
	 * OutputStream outputStream = new FileOutputStream(path);<br>
	 * FileTools.copyStream(outputStream, inputStream);<br>
	 * 
	 * @param outputStream 输出流
	 * @param inputStream 输入流
	 */
    public static void copyStream(OutputStream outputStream, InputStream inputStream) {
        int c;
        try {
            while ((c = inputStream.read()) != -1) outputStream.write(c);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 在rootPath下创建一个名为folderName的文件夹,已存在就不创建，rootPath不存在会先创建rootPath路径
	 * 
	 * @param rootPath
	 *            文件夹的路径
	 * @param folderName
	 *            文件夹的名字
	 * @return 新创建文件夹的目录
	 */
    public static String createFolder(String rootPath, String folderName) {
        try {
            folderName = folderName.replace("/", "");
            rootPath = getRealFolderPath(rootPath);
            File file = new File(rootPath);
            if (!file.exists()) {
                String newRootPath = rootPath.substring(0, rootPath.length() - 1);
                createFolder(rootPath.substring(0, newRootPath.lastIndexOf("/")), rootPath.substring(newRootPath.lastIndexOf("/")));
            }
            file = new File(rootPath + folderName);
            if (!file.exists()) file.mkdir();
            return rootPath + folderName + (folderName.equals("") ? "" : "/");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * 创建一个名为folderName的文件夹,已存在就不创建，rootPath不存在会先创建rootPath路径
	 * 
	 * @param folderPath
	 *            要创建的文件夹的路径
	 * @return 新创建文件夹的目录
	 */
    public static String createFolder(String folderPath) {
        return createFolder(folderPath, "");
    }

    /**
	 * 给出上级目录的路径
	 * 
	 * @param folderPath
	 * @return 上级目录的路径
	 */
    public static String getParentDirectory(String folderPath) {
        File file = new File(getRealFolderPath(folderPath));
        file = file.getParentFile() == null ? file : file.getParentFile();
        return getRealFolderPath(file.getPath());
    }

    /**
	 * 给出向上n级目录的路径
	 * 
	 * @param folderPath
	 * @return 向上n级目录的路径
	 */
    public static String getParentDirectory(String folderPath, int n) {
        File file = new File(getRealFolderPath(folderPath));
        while (n-- > 0) {
            file = file.getParentFile() == null ? file : file.getParentFile();
        }
        return getRealFolderPath(file.getPath());
    }

    /**
	 * 删除指定文件或文件夹
	 * 
	 * @param filePath
	 *            文件或文件夹路径
	 */
    public static void deleteFile(String filePath) {
        try {
            File f = new File(filePath);
            if (f.exists() && f.isDirectory()) {
                if (f.listFiles().length == 0) {
                    f.delete();
                } else {
                    File delFile[] = f.listFiles();
                    int i = f.listFiles().length;
                    for (int j = 0; j < i; j++) {
                        if (delFile[j].isDirectory()) {
                            deleteFile(delFile[j].getAbsolutePath());
                        }
                        delFile[j].delete();
                    }
                }
                deleteFile(filePath);
            } else if (f.exists()) {
                f.delete();
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 浏览目录（文件夹）
	 * 
	 * @param folderPath
	 *            目录的路径
	 * @param fileFilter 文件过滤器
	 * @return 该目录下所有文件列表
	 */
    public static File[] browseFolder(String folderPath, FileFilter fileFilter) {
        try {
            File f = new File(folderPath);
            if (f.exists() && f.isDirectory()) {
                if (fileFilter != null) return f.listFiles(fileFilter);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return new File[] {};
    }

    /**
	 * 浏览目录（文件夹）
	 * 
	 * @param f 目录的路径
	 * @param fileFilter 文件过滤器
	 * 
	 * @return 该目录下所有文件列表
	 */
    public static File[] browseFolder(File f, FileFilter fileFilter) {
        try {
            if (f.exists() && f.isDirectory()) {
                if (fileFilter != null) return f.listFiles(fileFilter);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return new File[] {};
    }

    /**
	 * 将目录路径转换为Linux与Windows都识别的格式 即转换成比较正规的路径字符串
	 * 
	 * @param path
	 *            原始路径
	 * @return 转换后的路径
	 */
    private static String getRealFolderPath(String path) {
        path = path.replace("\\", "/");
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        if (!path.endsWith("/")) path += "/";
        return path;
    }

    /**
	 * 将文件路径转换为Linux与Windows都识别的格式 即转换成比较正规的路径字符串
	 * 
	 * @param path
	 *            原始路径
	 * @return 转换后的路径
	 */
    public static String getRealFileName(String path) {
        path = path.replace("\\", "/");
        while (path.contains("//")) {
            path = path.replace("//", "/");
        }
        return path;
    }

    /**
	 * 由文件名获得扩展名
	 * @param fileName 文件名
	 * @return 扩展名
	 */
    public static String getExtendName(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
    }

    /**
	 * 由文件获得扩展名
	 * @param file 文件
	 * @return 扩展名
	 */
    public static String getExtendName(File file) {
        if (file.isDirectory()) return "folder";
        return getExtendName(file.getName());
    }

    /**
	 * 追加文本到文本类型文件尾部（UTF-8编码）<br>
	 * 与createFile不一样，createFile是重新生成一个文件覆盖以前的文件
	 * 
	 * @param content
	 *            追加的文本内容
	 * @param rootPath
	 *            父目录
	 * @param fileName
	 *            文件名
	 */
    public static void appendContentToFile(String content, String rootPath, String fileName) {
        String str = content;
        try {
            appendContentToFile(str.getBytes("UTF-8"), rootPath, fileName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 创建文本类型文件（UTF-8编码）<br>
	 * 如果已存在，就覆盖以前的内容
	 * 
	 * @param content
	 *            文本内容
	 * @param rootPath
	 *            目录
	 * @param fileName
	 *            文件名
	 */
    public static void createFile(String content, String rootPath, String fileName) {
        try {
            createFile(content.getBytes("UTF-8"), rootPath, fileName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 创建文本类型文件（UTF-8编码）
	 * 
	 * @param content
	 *            文本内容
	 * @param rootPath
	 *            目录
	 * @param fileFullName
	 *            文件名
	 */
    public static void createFile(String content, String fileFullName) {
        try {
            createFile(content.getBytes("UTF-8"), fileFullName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
	 * 读取文本类型文件（UTF-8编码）
	 * 
	 * @param rootPath
	 *            目录
	 * @param fileName
	 *            文件名
	 * @return 文件内容
	 */
    public static String readCharsFile(String rootPath, String fileName) {
        try {
            return new String(readBytesFile(rootPath, fileName), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * 读取文本类型文件（UTF-8编码）
	 * 
	 * @param fileFullName
	 *            文件目录
	 * @return 文件内容
	 */
    public static String readCharsFile(String fileFullName) {
        try {
            return new String(readBytesFile(fileFullName), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
	 * 追加二进制到二进制类型文件尾部<br>
	 * 与createFile不一样，createFile是重新生成一个文件覆盖以前的文件
	 * 
	 * @param content
	 *            追加的二进制内容
	 * @param rootPath
	 *            父目录
	 * @param fileName
	 *            文件名
	 */
    public static void appendContentToFile(byte[] content, String rootPath, String fileName) {
        File file = null;
        RandomAccessFile randomAccessFile = null;
        try {
            fileName = fileName.replace("/", "");
            rootPath = getRealFolderPath(rootPath);
            createFolder(rootPath);
            String filePath = rootPath + fileName;
            file = new File(filePath);
            randomAccessFile = new RandomAccessFile(filePath, "rw");
            if (content != null) {
                if (file.exists()) {
                    randomAccessFile.seek(randomAccessFile.length());
                } else {
                    randomAccessFile.setLength(0);
                }
                randomAccessFile.write(content);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
	 * 创建二进制类型文件<br>
	 * 如果已存在，就覆盖以前的内容
	 * 
	 * @param content
	 *            二进制内容
	 * @param rootPath
	 *            目录
	 * @param fileName
	 *            文件名
	 */
    public static void createFile(byte[] content, String rootPath, String fileName) {
        RandomAccessFile randomAccessFile = null;
        try {
            fileName = fileName.replace("/", "");
            rootPath = getRealFolderPath(rootPath);
            createFolder(rootPath);
            String filePath = rootPath + fileName;
            deleteFile(filePath);
            randomAccessFile = new RandomAccessFile(filePath, "rw");
            randomAccessFile.setLength(0);
            if (content != null) {
                randomAccessFile.write(content);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
	 * 创建二进制类型文件
	 * 
	 * @param content
	 *            二进制内容
	 * @param rootPath
	 *            目录
	 * @param fileFullName
	 *            文件名
	 */
    public static void createFile(byte[] content, String fileFullName) {
        fileFullName = getRealFileName(fileFullName);
        String rootPath = fileFullName.substring(0, fileFullName.lastIndexOf('/'));
        fileFullName = fileFullName.substring(fileFullName.lastIndexOf('/'));
        createFile(content, rootPath, fileFullName);
    }

    /**
	 * 读取二进制类型文件
	 * 
	 * @param rootPath
	 *            目录
	 * @param fileName
	 *            文件名
	 * @return 文件内容
	 */
    public static byte[] readBytesFile(String rootPath, String fileName) {
        File file = null;
        FileInputStream fis = null;
        try {
            fileName = fileName.replace("/", "");
            rootPath = getRealFolderPath(rootPath);
            String filePath = rootPath + fileName;
            file = new File(filePath);
            if (file.exists()) {
                fis = new FileInputStream(file);
                byte[] bytes = readInputStream(fis);
                return bytes;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new byte[0];
    }

    /**
	 * 读取输入流
	 * 
	 * @param is 输入流
	 * @return 二进制数据
	 */
    public static byte[] readInputStream(InputStream inputStream) {
        List<Byte> bytes = new ArrayList<Byte>();
        try {
            int c;
            while ((c = inputStream.read()) != -1) {
                bytes.add((byte) c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] bytesResult = new byte[bytes.size()];
        for (int i = 0; i < bytesResult.length; i++) {
            bytesResult[i] = bytes.get(i);
        }
        return bytesResult;
    }

    /**
	 * 读取二进制类型文件<br>
	 * 返回结果是一个byte[]类型，可以转换为String
	 * 
	 * @param fileFullName
	 *            文件目录
	 * @return 文件内容
	 */
    public static byte[] readBytesFile(String fileFullName) {
        File file = null;
        FileInputStream fis = null;
        try {
            fileFullName = getRealFileName(fileFullName);
            file = new File(fileFullName);
            if (file.exists()) {
                fis = new FileInputStream(file);
                byte[] bytes = readInputStream(fis);
                return bytes;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new byte[0];
    }

    public static void createZipAnt(String dirPath, String outPath) {
        File infile = new File(dirPath);
        File outFile = new File(outPath);
        Project prj = new Project();
        Zip zip = new Zip();
        zip.setProject(prj);
        zip.setDestFile(outFile);
        FileSet fileSet = new FileSet();
        fileSet.setProject(prj);
        fileSet.setDir(infile);
        zip.addFileset(fileSet);
        zip.execute();
        deleteFile(dirPath);
    }

    public static void main(String[] args) {
        System.out.println(readCharsFile("E:\\xvxv\\Function\\dataaccess\\dataaccess\\src\\main\\java\\cn\\rtdata\\dataaccess\\util\\file\\FileTools.java"));
    }
}
