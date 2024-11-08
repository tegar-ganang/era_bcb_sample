package org.xsocket.connection;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import org.junit.Assert;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.BlockingConnection;
import org.xsocket.connection.IBlockingConnection;
import org.xsocket.connection.INonBlockingConnection;
import org.xsocket.connection.Server;

/**
*
* @author grro@xsocket.org
*/
public final class FileSendTest {

    @Test
    public void testSmallFile() throws Exception {
        ServerHandler hdl = new ServerHandler();
        Server server = new Server(hdl);
        server.start();
        File file = QAUtil.createTestfile_4k();
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.write(file.getAbsolutePath() + "\r\n");
        int length = connection.readInt();
        File tempFile = QAUtil.createTempfile();
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel fc = raf.getChannel();
        connection.transferTo(fc, length);
        fc.close();
        raf.close();
        Assert.assertTrue(QAUtil.isEquals(tempFile, file));
        file.delete();
        tempFile.delete();
        connection.close();
        server.close();
    }

    @Test
    public void testLargeFile() throws Exception {
        ServerHandler hdl = new ServerHandler();
        Server server = new Server(hdl);
        server.start();
        File file = QAUtil.createTestfile_800k();
        IBlockingConnection connection = new BlockingConnection("localhost", server.getLocalPort());
        connection.write(file.getAbsolutePath() + "\r\n");
        int length = connection.readInt();
        File tempFile = QAUtil.createTempfile();
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        FileChannel fc = raf.getChannel();
        connection.transferTo(fc, length);
        fc.close();
        raf.close();
        if (!QAUtil.isEquals(tempFile, file)) {
            System.out.println("files are not equals");
            Assert.fail("files are not equals");
        }
        file.delete();
        tempFile.delete();
        connection.close();
        server.close();
    }

    private static final class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            String filename = connection.readStringByDelimiter("\r\n");
            if (!new File(filename).exists()) {
                System.out.println("file " + filename + " not exists");
            }
            RandomAccessFile raf = new RandomAccessFile(filename, "r");
            FileChannel fc = raf.getChannel();
            connection.write((int) raf.length());
            connection.transferFrom(fc);
            fc.close();
            raf.close();
            System.out.println(raf.length() + " bytes written");
            return true;
        }
    }
}
