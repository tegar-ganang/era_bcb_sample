package hm.core.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

public class FileHelper {

    public static void copyFile(String target, String source) {
        try {
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dstChannel = new FileOutputStream(target).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            ExceptionHelper.showErrorDialog(e);
        }
    }

    public static void copyFile(String target, InputStream source) {
        try {
            FileOutputStream dstChannel = new FileOutputStream(target);
            byte[] buffer = new byte[1];
            while (source.read(buffer) != -1) {
                dstChannel.write(buffer);
            }
            dstChannel.close();
        } catch (Exception e) {
            ExceptionHelper.showErrorDialog(e);
        }
    }

    public static void copyFile(File dst, File src) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public static String getOnlyFileName(String file) {
        return file.substring(file.lastIndexOf(File.separatorChar));
    }

    public static String getFilenameWithoutSuffix(String file) {
        return file.substring(0, file.lastIndexOf('.'));
    }

    public static String getFileAsString(String url) throws Exception {
        StringBuffer buff = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(url));
        String temp;
        while ((temp = reader.readLine()) != null) {
            buff.append(temp);
            buff.append("\r\n");
        }
        reader.close();
        return buff.toString();
    }

    public static String getFileAsString(InputStream stream) throws IOException {
        StringBuffer buff = new StringBuffer();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String temp;
        while ((temp = reader.readLine()) != null) {
            buff.append(temp);
            buff.append("\r\n");
        }
        reader.close();
        return buff.toString();
    }

    public static String convertToAscii(String string) throws Exception {
        String result = new String(string);
        for (int i = 0; i < result.length(); i++) {
            char next = result.charAt(i);
            if ((next < 65) || (next > 122)) {
                result = result.replace(next, 'f');
            }
        }
        return result;
    }
}
