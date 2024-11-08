package com.mlib.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import com.mlib.io.BufferedFileReader;

/**
 * 文件操作工具类
 * 
 * @author zitee@163.com
 * @创建时间 2009-09-17 13:37:04
 * @version 1.0
 */
public class FileUtil {

    /**
	 * 文件复制-根据给定的文件路径对文件进行复制 -复制过程中会对已有文件进行覆盖操作
	 * 
	 * @param sourceFilePath
	 * @param targetFilePath
	 * @throws IOException
	 */
    public static void copyFile(String sourceFilePath, String targetFilePath) throws IOException {
        File sourceFile = new File(sourceFilePath);
        File targetFile = new File(targetFilePath);
        copyFile(sourceFile, targetFile);
    }

    /**
	 * 文件复制-根据给定的文件对象对文件进行复制 -复制过程中会对已有文件进行覆盖操作
	 * 
	 * @param sourceFile
	 * @param targetFile
	 * @throws IOException
	 */
    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        FileInputStream iStream = new FileInputStream(sourceFile);
        FileOutputStream oStream = new FileOutputStream(targetFile);
        FileChannel inChannel = iStream.getChannel();
        FileChannel outChannel = oStream.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        while (true) {
            buffer.clear();
            int readCount = inChannel.read(buffer);
            if (readCount == -1) {
                break;
            }
            buffer.flip();
            outChannel.write(buffer);
        }
        iStream.close();
        oStream.close();
    }

    /**
	 * 文件复制-根据给定的文件对象对文件进行复制 -复制过程中会对已有文件进行覆盖操作
	 * 
	 * @param sourceStream
	 * @param targetFile
	 * @throws IOException
	 */
    public static void copyFile(InputStream sourceStream, File targetFile) throws IOException {
        OutputStream oStream = new FileOutputStream(targetFile);
        byte[] buffer = new byte[1024];
        int legnth = -1;
        while ((legnth = sourceStream.read(buffer)) > 0) {
            oStream.write(buffer, 0, legnth);
        }
        sourceStream.close();
        oStream.close();
    }

    public static String readAllAsString(File file, String... charset) throws IOException {
        BufferedFileReader reader;
        if (charset.length > 0) {
            reader = new BufferedFileReader(file, charset[0]);
        } else {
            reader = new BufferedFileReader(file);
        }
        String buffer = null;
        StringBuilder sb = new StringBuilder();
        while ((buffer = reader.readLine()) != null) {
            sb.append(buffer + "\n");
        }
        return sb.toString();
    }
}
