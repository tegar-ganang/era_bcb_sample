package org.susan.java.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ReadWriterWithBuffer {

    public static void main(String args[]) {
        ReadWriterWithBuffer rw = new ReadWriterWithBuffer();
        try {
            long startTime = System.currentTimeMillis();
            rw.readWrite("D:/setup.exe", "D:/myeclipse1.exe");
            long endTime = System.currentTimeMillis();
            System.out.println("Direct read and write time: " + (endTime - startTime) + "ms");
            startTime = System.currentTimeMillis();
            rw.readWriterBuffer("D:/setup.exe", "D:/myeclipse2.exe");
            endTime = System.currentTimeMillis();
            System.out.println("Buffer read and write time: " + (endTime - startTime) + "ms");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * 直接通过文件输入输出流读写文件
	 * @param fileFrom 源文件
	 * @param fileTo 目标文件
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
	 * 通过系统缓冲区类读取和写入文件的方法
	 * @param fileFrom 源文件
	 * @param fileTo 目标文件
	 * @throws IOException IO异常
	 */
    public void readWriterBuffer(String fileFrom, String fileTo) throws IOException {
        InputStream inBuffer = null;
        OutputStream outBuffer = null;
        try {
            InputStream in = new FileInputStream(fileFrom);
            inBuffer = new BufferedInputStream(in);
            OutputStream out = new FileOutputStream(fileTo);
            outBuffer = new BufferedOutputStream(out);
            while (true) {
                int bytedata = inBuffer.read();
                if (bytedata == -1) break;
                outBuffer.write(bytedata);
            }
        } finally {
            if (inBuffer != null) inBuffer.close();
            if (outBuffer != null) outBuffer.close();
        }
    }
}
