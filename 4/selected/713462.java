package org.jomper.pluto;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import org.jomper.pluto.model.ConnectionDigest;
import org.jomper.pluto.model.ThreadDigest;
import org.jomper.pluto.util.JomperXMLOperator;

public class SingleFileThreadWriter implements Runnable {

    private ConnectionDigest connectionDigest;

    private RandomAccessFile raf;

    private int threadIndex;

    private JomperXMLOperator jxo;

    public void run() {
        ThreadDigest threadDigest = connectionDigest.getThreadsDigest().get(threadIndex - 1);
        long actualLength = threadDigest.getActualLength();
        long startPos = threadDigest.getStartPos() + actualLength;
        long endPos = threadDigest.getEndPos();
        long segmentLength = threadDigest.getSegmentLength();
        long threadId = threadDigest.getThreadId();
        int bufferBytes = (int) (segmentLength - actualLength);
        System.out.println("start thread:" + threadId + " startPos:" + startPos + " actualLength:" + actualLength);
        if (startPos <= endPos) {
            URLConnection uc = null;
            try {
                uc = connectionDigest.getURL().openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            uc.setRequestProperty("User-Agent", "Jomper");
            uc.setRequestProperty("RANGE", "bytes=" + startPos);
            InputStream raw = null;
            try {
                raw = uc.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            DataInputStream dis = new DataInputStream(new BufferedInputStream(raw));
            byte[] b = new byte[bufferBytes];
            int readInt;
            long offset = actualLength;
            try {
                if (startPos != 0) raf.seek(startPos);
                while (offset < segmentLength && (readInt = dis.read(b, 0, bufferBytes)) > 0) {
                    if (offset + readInt > segmentLength) {
                        int oInt = readInt;
                        int spilth = oInt - (int) (segmentLength - offset);
                        readInt = readInt - spilth;
                        System.out.println("thread " + threadId + " total: " + (offset + oInt) + " - split: " + spilth + " =" + (offset + oInt - spilth) + " byte");
                    }
                    raf.write(b, 0, readInt);
                    offset += readInt;
                    jxo.update("//ConnectionDigest/ThreadDigest[@threadId='" + threadId + "']/@actualLength", Long.toString(offset));
                    jxo.save();
                    System.out.println("thread " + threadId + " actual:" + (offset) + " byte[]:" + readInt);
                }
                raw.close();
                dis.close();
                raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("finish thread:" + threadId + " writed " + offset + " byte");
        } else {
            System.out.println("finish thread:" + threadId + " writed 0 byte");
        }
    }

    public SingleFileThreadWriter(ConnectionDigest connectionDigest, int threadIndex, JomperXMLOperator jxo) {
        try {
            this.raf = new RandomAccessFile(connectionDigest.getFileName(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.connectionDigest = connectionDigest;
        this.threadIndex = threadIndex;
        this.jxo = jxo;
    }
}
