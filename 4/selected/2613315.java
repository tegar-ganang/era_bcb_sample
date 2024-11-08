package org.susan.java.io;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReadWriteWithArray {

    public static void main(String args[]) {
        ReadWriteWithArray rw = new ReadWriteWithArray();
        try {
            long startTime = System.currentTimeMillis();
            rw.readWrite("D:/setup.exe", "D:/myeclipse1.exe");
            long endTime = System.currentTimeMillis();
            System.out.println("Direct read and write time: " + (endTime - startTime) + "ms");
            startTime = System.currentTimeMillis();
            rw.readWriteArray("D:/setup.exe", "D:/myeclipse2.exe");
            endTime = System.currentTimeMillis();
            System.out.println("Buffer read and write time: " + (endTime - startTime) + "ms");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * 通过文件输入输出流读写文件
	 * @param fileFrom 源文件
	 * @param fileTo 目的文件
	 * @throws IOException IO异常
	 */
    public void readWrite(String fileFrom, String fileTo) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(fileFrom);
            out = new FileOutputStream(fileTo);
            while (true) {
                int bytedata = in.read();
                if (bytedata == -1) break;
                out.write(bytedata);
            }
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
	 * 通过自定义缓冲区读写文件方法
	 * @param fileFrom 源文件
	 * @param fileTo 目标文件
	 * @throws IOException IO异常
	 */
    public void readWriteArray(String fileFrom, String fileTo) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(fileFrom);
            out = new FileOutputStream(fileTo);
            int availableLength = in.available();
            byte[] totalBytes = new byte[availableLength];
            @SuppressWarnings("unused") int bytedata = in.read(totalBytes);
            out.write(totalBytes);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
