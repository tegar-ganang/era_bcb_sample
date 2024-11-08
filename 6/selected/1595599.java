package net.sf.cotta.ftp;

import net.sf.cotta.TDirectory;
import net.sf.cotta.TFileFactory;
import net.sf.cotta.UsingTfsConstratins;
import net.sf.cotta.memory.InMemoryFileSystem;
import org.apache.commons.net.ftp.FTPClient;
import java.io.IOException;

public class UsingTestFtpServer extends UsingTfsConstratins {

    protected FTPClient ftpClient;

    private static boolean serverStarted = false;

    private static TestFtpServer ftpServer;

    protected TDirectory rootDir;

    public void setUp() throws Exception {
        super.setUp();
        ensureServerStarted();
        cleanServerFileSystem();
        establish();
    }

    private void ensureServerStarted() throws InterruptedException {
        if (!serverStarted) {
            ftpServer = new TestFtpServer();
            ftpServer.start();
            Thread.sleep(1000);
            serverStarted = true;
        }
    }

    private void cleanServerFileSystem() {
        TFileFactory fileFactory = new TFileFactory(new InMemoryFileSystem());
        ftpServer.cleanFileSystem(fileFactory);
        rootDir = fileFactory.dir("/");
    }

    private void establish() throws IOException {
        ftpClient = new FTPClient();
        ftpClient.connect("localhost", 8021);
        ftpClient.login("anonymous", "test@test.com");
    }
}
