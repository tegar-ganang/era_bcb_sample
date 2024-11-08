package com.yuchengtech.fileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileIo {

    protected static boolean copyFile(File src, File dest) {
        try {
            if (!dest.exists()) {
                dest.createNewFile();
            }
            FileInputStream fis = new FileInputStream(src);
            FileOutputStream fos = new FileOutputStream(dest);
            byte[] temp = new byte[1024 * 8];
            int readSize = 0;
            do {
                readSize = fis.read(temp);
                fos.write(temp, 0, readSize);
            } while (readSize == temp.length);
            temp = null;
            fis.close();
            fos.flush();
            fos.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static String readFileAsString(File file) {
        return readFileAsStringBuilder(file, null).toString();
    }

    public static String readFileAsString(File file, String charSet) {
        return readFileAsStringBuilder(file, charSet).toString();
    }

    public static StringBuilder readFileAsStringBuilder(File file) {
        return readFileAsStringBuilder(file, null);
    }

    public static StringBuilder readFileAsStringBuilder(File file, String charSet) {
        StringBuilder builder = new StringBuilder();
        if (file.length() == 0) return builder;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            InputStreamReader reader = null;
            if (charSet == null) {
                reader = new InputStreamReader(inputStream);
            } else reader = new InputStreamReader(inputStream, charSet);
            char[] chars = new char[1024 * 4];
            int length = 0;
            do {
                length = reader.read(chars);
                builder.append(new String(chars, 0, length));
            } while (length == chars.length);
            reader.close();
            return builder;
        } catch (FileNotFoundException e) {
            System.out.println("文件不存在：" + file.getAbsolutePath() + "\n");
            return builder;
        } catch (IOException e) {
            System.out.println(e.getMessage() + "\n");
            return builder;
        }
    }

    public static boolean writeToFile(File file, String content, String charset) {
        FileOutputStream outputStream = null;
        OutputStreamWriter writer = null;
        try {
            outputStream = new FileOutputStream(file, false);
            if (charset == null) writer = new OutputStreamWriter(outputStream); else writer = new OutputStreamWriter(outputStream, charset);
            writer.write(content);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (writer != null) try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
            }
        }
        return true;
    }
}
