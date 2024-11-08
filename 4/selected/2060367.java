package net.cytopia.tofu.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 *
 * @author kev
 */
public class FileUtil {

    public static String getRelativePath(File file) {
        String currentDirectory = System.getProperty("user.dir");
        String filePath = null;
        try {
            filePath = file.getCanonicalPath();
        } catch (IOException e) {
        }
        if (filePath.startsWith(currentDirectory)) {
            filePath = filePath.replace(currentDirectory, "");
            filePath = "." + filePath;
            return filePath;
        }
        return null;
    }

    public static String getFilePathDelimeter() {
        if (isWin32()) {
            return WIN32_DELIMETER;
        } else {
            return DEFAULT_DELIMETER;
        }
    }

    public static boolean isWin32() {
        System.out.println("[FileUtilities] checking system: " + System.getProperty("os.name"));
        if (System.getProperty("os.name").indexOf("Windows") > -1) {
            System.out.println("[FileUtilities] system is windows...");
            return true;
        } else {
            return false;
        }
    }

    public static String getWebAppFilePath(String webAppContext) {
        String delimeter = getFilePathDelimeter();
        String basePath = System.getProperty("user.dir") + delimeter + ".." + delimeter + "webapps" + webAppContext;
        return basePath;
    }

    public static String[] getFileLines(File file) {
        Vector<String> flines = new Vector<String>();
        try {
            FileInputStream fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                flines.add(strLine);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (String[]) flines.toArray(new String[flines.size()]);
    }

    public static synchronized byte[] getBytesFromStream(InputStream in) {
        byte[] bytes = null;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
            in.close();
            out.close();
            bytes = out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static boolean copyFileToDir(String toPath, String toFileName, File file) {
        boolean result = true;
        HttpUtils.log(FileUtil.class, "Copy file " + file.getName() + " to path: " + toPath + toFileName);
        try {
            byte[] bytes = getBytesFromFile(file);
            if (bytes != null) {
                String fullPath = toPath + toFileName;
                writeBytesToFile(bytes, fullPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        int fileLength = (int) file.length();
        byte[] bytes = new byte[fileLength];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }

    public static synchronized boolean writeBytesToFile(byte[] bytes, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(filePath);
            out.write(bytes);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (SecurityException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static final String DEFAULT_DELIMETER = "/";

    public static final String WIN32_DELIMETER = "\\";
}
