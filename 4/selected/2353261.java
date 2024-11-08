package archives.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.StringTokenizer;
import com.mongodb.gridfs.GridFSDBFile;

public class FileUtils {

    /**
	 * 
	 * @param source
	 */
    public static void deleteFile(String source) {
        new File(source).delete();
    }

    /**
	 * 
	 * @param file
	 */
    public static void deleteFile(File file) {
        file.delete();
    }

    /**
	 * 
	 * @param srcFile
	 * @param targetPath
	 */
    public static void moveFile(File srcFile, String targetPath) {
        srcFile.renameTo(new File(targetPath));
        System.out.println("=> File [ " + srcFile.getName() + " ] moved to [ " + targetPath + " ] successfully ...");
    }

    /**
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
    public static void copyFile(String source, String destination) throws IOException {
        File srcDir = new File(source);
        File[] files = srcDir.listFiles();
        FileChannel in = null;
        FileChannel out = null;
        for (File file : files) {
            try {
                in = new FileInputStream(file).getChannel();
                File outFile = new File(destination, file.getName());
                out = new FileOutputStream(outFile).getChannel();
                in.transferTo(0, in.size(), out);
            } finally {
                if (in != null) in.close();
                if (out != null) out.close();
            }
        }
    }

    /**
	 * 将数据库文件写到磁盘指定文件路径
	 * 
	 * @param dbFile
	 * @param filename
	 * @return
	 */
    public static boolean writeTo(GridFSDBFile dbFile, String filename) {
        File tempFile = new File(filename);
        String prefix = FileUtils.trimExtension(filename);
        String suffix = FileUtils.getExtension(filename);
        try {
            if (tempFile.exists()) {
                String newName = prefix + " [ " + Calendar.getInstance().getTimeInMillis() + " ]." + suffix;
                dbFile.writeTo(newName);
                System.out.println("=> File [ " + filename + " ] already exists , [ " + newName + " ] renamed file writed successfully ...");
                return true;
            }
            dbFile.writeTo(filename);
            System.out.println("=> File [ " + filename + " ] writed successfully ...");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * 
	 * @param dbFile
	 * @param diskFile
	 * @return
	 */
    public boolean writeTo(GridFSDBFile dbFile, File diskFile) {
        try {
            dbFile.writeTo(diskFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * 将数据库文件写到磁盘指定文件路径
	 * 
	 * @param dbFile
	 * @param out
	 * @return
	 */
    public static boolean writeTo(GridFSDBFile dbFile, OutputStream out) {
        try {
            dbFile.writeTo(out);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * 获得目录全名
	 * 
	 * @return String
	 */
    public static String getAllPath(String filePath) {
        if (!filePath.endsWith("/")) {
            filePath += "/";
        }
        return filePath;
    }

    /**
	 * 创建目录
	 * 
	 * @param fileDir
	 * @param context
	 */
    public static void makeDir(String context, String fileDir) {
        StringTokenizer stringTokenizer = new StringTokenizer(fileDir, "/");
        String strTemp = "";
        while (stringTokenizer.hasMoreTokens()) {
            String str = stringTokenizer.nextToken();
            if ("".equals(strTemp)) {
                strTemp = str;
            } else {
                strTemp = strTemp + "/" + str;
            }
            File dir = new File(context + strTemp);
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
        }
    }

    /**
	 * 获取文件扩展名
	 * 
	 * @param f
	 * @return
	 */
    public static String getExtension(File f) {
        return (f != null) ? getExtension(f.getName()) : "";
    }

    public static String getExtension(String filename) {
        return getExtension(filename, "");
    }

    public static String getExtension(String filename, String defExt) {
        if ((filename != null) && (filename.length() > 0)) {
            int i = filename.lastIndexOf('.');
            if ((i > -1) && (i < (filename.length() - 1))) {
                return filename.substring(i + 1);
            }
        }
        return defExt;
    }

    /**
	 * 去掉文件扩展名
	 * 
	 * @param filename
	 * @return
	 */
    public static String trimExtension(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int i = filename.lastIndexOf('.');
            if ((i > -1) && (i < (filename.length()))) {
                return filename.substring(0, i);
            }
        }
        return filename;
    }

    /**
	 * 
	 * @param file
	 * @param list
	 * @throws Exception
	 */
    public static void acceptFiles(File file, List<File> list) throws Exception {
        if (file.isDirectory()) {
            String[] files = file.list();
            Arrays.sort(files);
            for (int i = 0; i < files.length; i++) {
                acceptFiles(new File(file, files[i]), list);
            }
        } else if (file.getPath().endsWith(".doc") || file.getPath().endsWith(".docx")) {
            list.add(file);
            System.out.println("Hit target > " + file.getParent() + "\\" + file.getName());
        }
    }
}
