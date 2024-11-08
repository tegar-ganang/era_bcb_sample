package jcfs.core;

import org.junit.Ignore;
import java.io.ByteArrayOutputStream;
import java.io.File;
import jcfs.core.client.JCFS;
import jcfs.core.fs.RFile;
import jcfs.core.fs.RFileInputStream;
import jcfs.core.fs.RFileOutputStream;
import jcfs.core.serverside.JCFSFileServer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for simple App.
 */
@Ignore
public class LargeStreamsTest extends StorageDirBased {

    @Test
    public void testPutBufferedBigger() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        JCFS.configureDiscovery(defaultUdpAddress, defaultUdpPort);
        try {
            server.start();
            RFile file = new RFile("test.txt");
            RFileOutputStream out = new RFileOutputStream(file);
            String body = "";
            for (int i = 0; i < 50 * 1024; i++) {
                body = body + "a";
            }
            out.write(body.getBytes());
            out.close();
            File expected = new File(dir, "test.txt");
            assertTrue(expected.isFile());
            assertEquals(body.length(), expected.length());
        } finally {
            server.stop();
        }
    }

    @Test
    public void testWriteAndReadBigger() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        JCFS.configureDiscovery(defaultUdpAddress, defaultUdpPort);
        try {
            server.start();
            RFile file = new RFile("testreadwrite.txt");
            RFileOutputStream out = new RFileOutputStream(file);
            String body = "";
            int size = 50 * 1024;
            for (int i = 0; i < size; i++) {
                body = body + "a";
            }
            out.write(body.getBytes("utf-8"));
            out.close();
            File expected = new File(dir, "testreadwrite.txt");
            assertTrue(expected.isFile());
            assertEquals(body.length(), expected.length());
            RFileInputStream in = new RFileInputStream(file);
            byte[] buffer = new byte[body.length()];
            int readCount = in.read(buffer);
            in.close();
            assertEquals(body.length(), readCount);
            String resultRead = new String(buffer, "utf-8");
            assertEquals(body, resultRead);
        } finally {
            server.stop();
        }
    }

    @Test
    public void testWriteAndReadBiggerUnbuffered() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        JCFS.configureDiscovery(defaultUdpAddress, defaultUdpPort);
        try {
            server.start();
            RFile file = new RFile("testreadwriteb.txt");
            RFileOutputStream out = new RFileOutputStream(file);
            String body = "";
            int size = 50 * 1024;
            for (int i = 0; i < size; i++) {
                body = body + "a";
            }
            out.write(body.getBytes("utf-8"));
            out.close();
            File expected = new File(dir, "testreadwriteb.txt");
            assertTrue(expected.isFile());
            assertEquals(body.length(), expected.length());
            RFileInputStream in = new RFileInputStream(file);
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            int b = in.read();
            while (b != -1) {
                tmp.write(b);
                b = in.read();
            }
            byte[] buffer = tmp.toByteArray();
            in.close();
            assertEquals(body.length(), buffer.length);
            String resultRead = new String(buffer, "utf-8");
            assertEquals(body, resultRead);
        } finally {
            server.stop();
        }
    }
}
