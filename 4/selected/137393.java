package jcfs.core;

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
public class BasicTest extends StorageDirBased {

    @Test
    public void testStartStop() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        try {
            server.start();
        } finally {
            server.stop();
        }
    }

    @Test
    public void testPutUnBuffered() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        JCFS.configureDiscovery(defaultUdpAddress, defaultUdpPort);
        try {
            server.start();
            RFile file = new RFile("test.txt");
            RFileOutputStream out = new RFileOutputStream(file);
            byte[] raw = "test".getBytes();
            for (int i = 0; i < raw.length; i++) {
                out.write(raw[i]);
            }
            out.close();
            File expected = new File(dir, "test.txt");
            assertTrue(expected.isFile());
            assertEquals(4, expected.length());
        } finally {
            server.stop();
        }
    }

    @Test
    public void testPutBuffered() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        JCFS.configureDiscovery(defaultUdpAddress, defaultUdpPort);
        try {
            server.start();
            RFile file = new RFile("test.txt");
            RFileOutputStream out = new RFileOutputStream(file);
            out.write("test".getBytes());
            out.close();
            File expected = new File(dir, "test.txt");
            assertTrue(expected.isFile());
            assertEquals(4, expected.length());
        } finally {
            server.stop();
        }
    }

    @Test
    public void testWriteAndRead() throws Exception {
        JCFSFileServer server = new JCFSFileServer(defaultTcpPort, defaultTcpAddress, defaultUdpPort, defaultUdpAddress, dir, 0, 0);
        JCFS.configureDiscovery(defaultUdpAddress, defaultUdpPort);
        try {
            server.start();
            RFile file = new RFile("testreadwrite.txt");
            RFileOutputStream out = new RFileOutputStream(file);
            out.write("test".getBytes("utf-8"));
            out.close();
            RFileInputStream in = new RFileInputStream(file);
            byte[] buffer = new byte[4];
            int readCount = in.read(buffer);
            in.close();
            assertEquals(4, readCount);
            String resultRead = new String(buffer, "utf-8");
            assertEquals("test", resultRead);
        } finally {
            server.stop();
        }
    }
}
