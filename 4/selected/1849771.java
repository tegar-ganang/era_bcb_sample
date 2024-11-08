package com.calipso.reportgenerator.reportcalculator.matrix;

import com.calipso.reportgenerator.common.LanguageTraslator;
import com.calipso.reportgenerator.reportcalculator.Matrix;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.ResultSet;

/**
 *
 * User: jbassino
 * Date: 16-sep-2005
 * Time: 14:07:46
 */
public class BlockSerializerThread extends Thread {

    private String blockFileName;

    private boolean serialized;

    private Matrix matrix;

    private Exception savedException;

    public BlockSerializerThread(String blockFileName) {
        super();
        this.blockFileName = blockFileName;
    }

    public BlockSerializerThread(String fileName, Matrix usedPart) {
        super();
        this.blockFileName = fileName;
        this.matrix = usedPart;
    }

    public void run() {
        try {
            if (matrix == null) {
                loadBlock();
            } else {
                serializeBlock();
            }
        } catch (Exception e) {
            e.printStackTrace();
            savedException = e;
        } catch (Throwable t) {
            savedException = new Exception(t);
        }
        System.out.println(LanguageTraslator.traslate("487"));
        System.out.println(LanguageTraslator.traslate("488") + blockFileName);
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.gc();
        System.out.println("5 gc()" + blockFileName);
    }

    private void serializeBlock() throws Exception, Throwable {
        System.out.println(LanguageTraslator.traslate("489") + blockFileName);
        File file = new File(blockFileName);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(blockFileName);
        FileChannel channel = fileOutputStream.getChannel();
        FileLock lock = channel.tryLock();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
        objectOutputStream.writeObject(matrix);
        matrix = null;
        lock.release();
        objectOutputStream.flush();
        fileOutputStream.flush();
        objectOutputStream.close();
        fileOutputStream.close();
    }

    private synchronized void loadBlock() throws Exception {
        System.out.println(LanguageTraslator.traslate("490") + blockFileName);
        FileInputStream fileInputStream = new FileInputStream(blockFileName);
        while (matrix == null) {
            try {
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                matrix = (Matrix) objectInputStream.readObject();
                objectInputStream.close();
            } catch (IOException e) {
                sleep(10000);
            }
        }
        fileInputStream.close();
        notify();
    }

    public synchronized Matrix getPreparedPart() throws Exception {
        while (matrix == null && savedException == null) {
            wait();
        }
        if (savedException != null) {
            System.out.println(LanguageTraslator.traslate("491"));
            savedException.printStackTrace();
            throw savedException;
        }
        return matrix;
    }
}
