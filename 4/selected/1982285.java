package org.malu.framework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.apache.commons.io.FileUtils;

/**
 * 文件工具类
 *
 */
public class FileUtil {

    private FileUtil() {
    }

    /**
	 * 是否是允许上传文件
	 *
	 * @param logoFileName
	 * @return
	 */
    public static boolean isAllowUp(String logoFileName) {
        String allowTYpe = "gif,jpg,bmp,swf";
        if (!logoFileName.trim().equals("") && logoFileName.length() > 0) {
            String ex = logoFileName.substring(logoFileName.lastIndexOf(".") + 1, logoFileName.length());
            return allowTYpe.toString().indexOf(ex) >= 0;
        } else {
            return false;
        }
    }

    /**
	 * 把内容写入文件
	 *
	 * @param filePath
	 * @param fileContent
	 */
    public static void write(String filePath, String fileContent) {
        try {
            FileOutputStream fo = new FileOutputStream(filePath);
            OutputStreamWriter out = new OutputStreamWriter(fo, "UTF-8");
            out.write(fileContent);
            out.close();
        } catch (IOException ex) {
            System.err.println("Create File Error!");
            ex.printStackTrace();
        }
    }

    /**
	 * 读取文件内容 默认是UTF-8编码
	 *
	 * @param filePath
	 * @return
	 */
    public static String read(String filePath, String code) {
        if (code == null || code.equals("")) {
            code = "UTF-8";
        }
        String fileContent = "";
        File file = new File(filePath);
        try {
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), code);
            BufferedReader reader = new BufferedReader(read);
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent = fileContent + line + "\n";
            }
            read.close();
            read = null;
            reader.close();
            read = null;
        } catch (Exception ex) {
            fileContent = "";
        }
        return fileContent;
    }

    /**
	 * 删除文件
	 *
	 * @param filePath
	 */
    public static void delete(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) file.delete();
        } catch (Exception ex) {
        }
    }

    public static boolean exist(String filepath) {
        File file = new File(filepath);
        return file.exists();
    }

    /**
	 * 创建文件夹
	 *
	 * @param filePath
	 */
    public static void createFolder(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception ex) {
            System.err.println("Make Folder Error:" + ex.getMessage());
        }
    }

    /**
	 * 得到文件的扩展名
	 *
	 * @param fileName
	 * @return
	 */
    public static String getFileExt(String fileName) {
        int potPos = fileName.lastIndexOf('.') + 1;
        String type = fileName.substring(potPos, fileName.length());
        return type;
    }

    /**
	 * 通过File对象创建文件
	 *
	 * @param file
	 * @param filePath
	 */
    public static void createFile(File file, String filePath) {
        int potPos = filePath.lastIndexOf('/') + 1;
        String folderPath = filePath.substring(0, potPos);
        createFolder(folderPath);
        FileOutputStream outputStream = null;
        FileInputStream fileInputStream = null;
        try {
            outputStream = new FileOutputStream(filePath);
            fileInputStream = new FileInputStream(file);
            byte[] by = new byte[1024];
            int c;
            while ((c = fileInputStream.read(by)) != -1) {
                outputStream.write(by, 0, c);
            }
        } catch (IOException e) {
            e.getStackTrace().toString();
        }
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(String resource) {
        InputStream stream = getResourceAsStream(resource);
        String content = readStreamToString(stream);
        return content;
    }

    public static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;
        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        return stream;
    }

    public static String readStreamToString(InputStream stream) {
        String fileContent = "";
        try {
            InputStreamReader read = new InputStreamReader(stream, "utf-8");
            BufferedReader reader = new BufferedReader(read);
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent = fileContent + line + "\n";
            }
            read.close();
            read = null;
            reader.close();
            read = null;
        } catch (Exception ex) {
            fileContent = "";
        }
        return fileContent;
    }

    /**
	 * delete file folder
	 *
	 * @param path
	 */
    public static void removeFile(File path) {
        System.out.println("removing file " + path.getPath());
        if (path.isDirectory()) {
            File[] child = path.listFiles();
            if (child != null && child.length != 0) {
                for (int i = 0; i < child.length; i++) {
                    removeFile(child[i]);
                    child[i].delete();
                }
            }
        }
        path.delete();
    }

    public static void copyFile(String srcFile, String destFile) {
        try {
            FileUtils.copyFile(new File(srcFile), new File(destFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFolder(String sourceFolder, String destinationFolder) throws Exception {
        File sourceF = new File(sourceFolder);
        if (sourceF.exists()) FileUtils.copyDirectory(new File(sourceFolder), new File(destinationFolder));
    }

    public static void main(String arg[]) {
        String filePath = "d:/aaa/bb/cc.jpg";
        int potPos = filePath.lastIndexOf('/') + 1;
        String folderPath = filePath.substring(0, potPos);
        System.out.println(folderPath);
    }
}
