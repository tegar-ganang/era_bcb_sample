package org.xsocket.connection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.Assert;
import org.junit.Test;
import org.xsocket.DataConverter;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.IServer;

/**
*
* @author grro@xlightweb.org
*/
public final class DataHandlerExampleTest {

    @Test
    public void echoHandlerTest() throws Exception {
        IServer server = new Server(new LineEchoHandler());
        server.start();
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        for (int i = 0; i < 10000; i++) {
            String request = "request#" + i;
            con.write(request + "\r\n");
            String response = con.readStringByDelimiter("\r\n");
            Assert.assertEquals(request, response);
        }
        con.close();
        server.close();
    }

    @Test
    public void fileStreamingHandlerTest() throws Exception {
        IServer server = new Server(new FileStreamingHandler());
        server.start();
        File file = QAUtil.createTestfile_400k();
        IBlockingConnection con = new BlockingConnection("localhost", server.getLocalPort());
        con.setAutoflush(false);
        for (int i = 0; i < 5; i++) {
            FileInputStream fis = new FileInputStream(file);
            FileChannel fc = fis.getChannel();
            int length = (int) fc.size();
            con.write(length);
            con.transferFrom(fc);
            con.flush();
            fc.close();
            fis.close();
            String fname = con.readStringByDelimiter("\r\n");
            QAUtil.isEquals(file, new File(fname));
            new File(fname).delete();
        }
        file.delete();
        con.close();
        server.close();
    }

    private static final class LineEchoHandler implements IConnectHandler, IDataHandler {

        private static final Logger LOG = Logger.getLogger(EchoHandler.class.getName());

        public boolean onConnect(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.setAutoflush(false);
            return true;
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            ByteBuffer[] data = connection.readByteBufferByDelimiter("\r\n");
            if (LOG.isLoggable(Level.FINE)) {
                ByteBuffer[] dataCopy = new ByteBuffer[data.length];
                for (int i = 0; i < data.length; i++) {
                    dataCopy[i] = data[i].duplicate();
                }
                LOG.fine(DataConverter.toString(dataCopy));
            }
            connection.write(data);
            connection.write("\r\n");
            connection.flush();
            return true;
        }
    }

    private static final class FileStreamingHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            int length = connection.readInt();
            File file = QAUtil.createTempfile();
            connection.setHandler(new FileStreamer(this, length, file.getAbsolutePath()));
            return true;
        }
    }

    private static final class FileStreamer implements IDataHandler {

        private IDataHandler orgDataHandler;

        private int remaining;

        private String filename;

        private RandomAccessFile raf;

        private FileChannel fc;

        public FileStreamer(IDataHandler orgDataHandler, int length, String filename) throws IOException {
            this.orgDataHandler = orgDataHandler;
            this.remaining = length;
            this.filename = filename;
            raf = new RandomAccessFile(filename, "rw");
            fc = raf.getChannel();
        }

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, ClosedChannelException, MaxReadSizeExceededException {
            int available = connection.available();
            if (available <= 0) {
                return true;
            }
            int lengthToRead = remaining;
            if (available < remaining) {
                lengthToRead = available;
            }
            connection.transferTo(fc, lengthToRead);
            remaining -= lengthToRead;
            if (remaining == 0) {
                fc.close();
                raf.close();
                connection.write(filename + "\r\n");
                connection.setHandler(orgDataHandler);
            }
            return true;
        }
    }
}
