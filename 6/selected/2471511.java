package net.sf.cotta.ftp;

import net.sf.cotta.TDirectory;
import net.sf.cotta.TFileFactory;
import net.sf.cotta.memory.InMemoryFileSystem;
import net.sf.cotta.test.TestBase;
import net.sf.cotta.test.TestFixture;
import org.apache.commons.net.ftp.FTPClient;
import java.io.IOException;

public class FtpFixture implements TestFixture {

    private TestFtpServer ftpServer;

    public void setUp() {
        ftpServer = new TestFtpServer();
        ftpServer.start();
    }

    public void tearDown() {
        ftpServer.stop();
    }

    public void beforeMethod(TestBase testBase) throws IOException {
        TFileFactory fileFactory = new TFileFactory(new InMemoryFileSystem());
        ftpServer.cleanFileSystem(fileFactory);
        TDirectory rootDir = fileFactory.dir("/");
        testBase.inject(rootDir);
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect("localhost", 8021);
        ftpClient.login("anonymous", "test@test.com");
        testBase.inject(ftpClient);
    }

    public void afterMethod(TestBase testBase) {
    }
}
