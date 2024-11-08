package org.xsocket.connection;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.ReadableByteChannel;
import org.junit.Test;
import org.xsocket.MaxReadSizeExceededException;
import org.xsocket.QAUtil;
import org.xsocket.connection.INonBlockingConnection;

/**
*
* @author grro@xsocket.org
*/
public class FlowControlTest {

    @Test
    public void testSimple() throws Exception {
    }

    private static final class ServerHandler implements IDataHandler {

        public boolean onData(INonBlockingConnection connection) throws IOException, BufferUnderflowException, MaxReadSizeExceededException {
            connection.readStringByDelimiter("\r\n");
            File file = QAUtil.createTestfile_400k();
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            ReadableByteChannel in = raf.getChannel();
            connection.transferFrom(in);
            in.close();
            raf.close();
            file.delete();
            return true;
        }
    }
}
