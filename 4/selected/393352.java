package org.xsocket.web.http;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import org.junit.Test;
import org.xsocket.stream.Server;
import org.xsocket.stream.StreamUtils;
import org.xsocket.web.http.HttpProtocolHandler;
import org.xsocket.web.http.IHeader;
import org.xsocket.web.http.IHttpConnection;
import org.xsocket.web.http.IMessageHandler;
import org.xsocket.web.http.IResponseHeader;
import org.xsocket.web.http.IWriteableChannel;

/**
*
* @author grro@xsocket.org
*/
public final class HttpConnectionServerTest {

    @Test
    public void testServer() throws Exception {
        QAUtil.setLogLevel(Level.FINE);
        Server server = new Server(8080, new HttpProtocolHandler(new Handler()));
        StreamUtils.start(server);
        QAUtil.sleep(10000000);
        server.close();
    }

    private static final class Handler implements IMessageHandler {

        @Override
        public void onMessageHeader(IHttpConnection httpConnection) throws BufferUnderflowException, IOException {
            IHeader header = httpConnection.receiveMessageHeader();
            System.out.println(header);
            IResponseHeader responseHeader = httpConnection.createResponseHeader(200);
            responseHeader.addHeader("Content-Encoding", "text/plain");
            byte[] data = (System.currentTimeMillis() + "  Hello how are you?").getBytes();
            IWriteableChannel bodyHandle = httpConnection.sendChunkedMessageHeader(responseHeader);
            bodyHandle.transferFrom(getFileChannel(data));
            bodyHandle.close();
        }

        private FileChannel getFileChannel(byte[] data) throws IOException {
            File file = File.createTempFile("temp", "temp");
            file.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            FileChannel fc = new RandomAccessFile(file, "r").getChannel();
            return fc;
        }
    }
}
