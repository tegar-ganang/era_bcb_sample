package com.conicsoft.bdkJ.core;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class FilePrivate {

    public static boolean Copy(String __from, String __to) {
        try {
            int bytesum = 0;
            int byteread = -1;
            java.io.File oldfile = new java.io.File(__from);
            if (oldfile.exists()) {
                InputStream inStream = new FileInputStream(__from);
                FileOutputStream fs = new FileOutputStream(__to);
                byte[] buffer = new byte[1024];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("processFile.copyFile()���Ƶ����ļ��������� " + e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean CreateFile(String __dir, String __file) {
        if (__dir.equals("")) {
            System.out.print("�����ļ����ִ���!");
            return false;
        }
        java.io.File f = new java.io.File(__dir, __file);
        if (f.exists()) {
            System.out.print(f.getAbsolutePath());
            System.out.print(f.getName());
            System.out.print(f.length());
        } else {
            if (!f.isDirectory()) {
                new java.io.File(__dir).mkdir();
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    System.out.print("�����ļ����ִ���!");
                    e.printStackTrace();
                }
            }
        }
        return true;
    }
}
