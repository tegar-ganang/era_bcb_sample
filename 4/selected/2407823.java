package net.sf.cotta.ftp;

import net.sf.cotta.FileSystem;
import net.sf.cotta.TIoException;
import net.sf.cotta.TPath;
import net.sf.cotta.ftp.client.commonsNet.CommonsNetFtpClient;
import net.sf.cotta.io.OutputMode;
import org.apache.commons.io.IOUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FtpFileSystemBehaviour extends UsingTestFtpServer {

    private FileSystem fileSystem;

    public void setUp() throws Exception {
        super.setUp();
        fileSystem = new FtpFileSystem(new CommonsNetFtpClient(ftpClient));
    }

    public void shouldTellFileExistsOrNot() throws TIoException {
        boolean exists = fileSystem.fileExists(_("/testFile"));
        assertThat().bool(exists).isFalse();
        rootDir.file("testFile").save("");
        exists = fileSystem.fileExists(_("/testFile"));
        assertThat().bool(exists).isTrue();
    }

    public void shouldTellDirExistsOrNot() throws IOException {
        assertThat().bool(fileSystem.dirExists(_("/"))).isTrue();
        assertThat().bool(fileSystem.dirExists(_("/abc"))).isFalse();
        ftpClient.makeDirectory("abc");
        assertThat().bool(fileSystem.dirExists(_("/abc"))).isTrue();
        ftpClient.changeWorkingDirectory("abc");
        ftpClient.makeDirectory("def");
        assertThat().bool(fileSystem.dirExists(_("/abc/def"))).isTrue();
    }

    public void shouldLeaveZeroByteFileAfterFileCreated() throws IOException {
        fileSystem.createFile(_("hello"));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ftpClient.retrieveFile("/hello", outputStream);
        assertThat().integer(outputStream.toByteArray().length).isEqualTo(0);
    }

    public void shouldBeAbleToDeleteFile() throws IOException {
        fileSystem.createFile(_("hello"));
        fileSystem.deleteFile(_("/hello"));
        assertThat().integer(ftpClient.listNames().length).isEqualTo(0);
    }

    public void shouldBeAbleToCreateDirectory() throws IOException {
        fileSystem.createDir(_("hello/world"));
        assertThat().string(ftpClient.listNames()[0]).isEqualTo("hello/");
        assertThat().string(ftpClient.listNames("hello")[0]).isEqualTo("world/");
    }

    public void shoudlNotCountFileWhenListingDir() throws IOException {
        fileSystem.createFile(_("hello"));
        assertThat().integer(fileSystem.listDirs(_("/")).length).isEqualTo(0);
        fileSystem.createDir(_("abc"));
        assertThat().array(fileSystem.listDirs(_("."))).isEqualTo(new TPath[] { _("abc") });
    }

    public void shouldNotCountDirWhenListingFile() throws IOException {
        fileSystem.createDir(_("abc"));
        assertThat().integer(fileSystem.listFiles(_("/")).length).isEqualTo(0);
        fileSystem.createFile(_("hello"));
        assertThat().array(fileSystem.listFiles(_("."))).isEqualTo(new TPath[] { _("hello") });
    }

    public void shouldBeAbleToDeleteDirectory() throws IOException {
        fileSystem.createDir(_("abc"));
        fileSystem.deleteDirectory(_("abc"));
        assertThat().integer(ftpClient.listNames().length).isEqualTo(0);
    }

    public void shouldBeAbleToMoveFile() throws IOException {
        fileSystem.createFile(_("abc"));
        fileSystem.createDir(_("hello"));
        fileSystem.moveFile(_("abc"), _("hello/abc"));
        assertThat().string(ftpClient.listNames()[0]).isEqualTo("hello/");
        assertThat().string(ftpClient.listNames("hello")[0]).isEqualTo("abc");
    }

    public void shouldBeAbleToMoveDirectory() throws IOException {
        fileSystem.createDir(_("abc"));
        fileSystem.createDir(_("hello"));
        fileSystem.moveDirectory(_("abc"), _("hello/abc"));
        assertThat().string(ftpClient.listNames()[0]).isEqualTo("hello/");
        assertThat().string(ftpClient.listNames("hello")[0]).isEqualTo("abc/");
    }

    public void shouldBeAbleToTellFileLength() throws IOException {
        fileSystem.createFile(_("hello"));
        assertThat().longValue(fileSystem.fileLength(_("hello"))).isEqualTo(0);
    }

    public void shouldBeAbleToDownloadAndUpload() throws IOException {
        OutputStream outputStream = fileSystem.createOutputStream(_("hello"), OutputMode.OVERWRITE);
        outputStream.write(new byte[] { 1, 2, 3 });
        outputStream.close();
        InputStream inputStream = fileSystem.createInputStream(_("hello"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, buffer);
        inputStream.close();
        assertThat().array(buffer.toByteArray()).isEqualTo(new byte[] { 1, 2, 3 });
    }

    public void shouldAllowClosingOutputStreamTwice() throws IOException {
        OutputStream outputStream = fileSystem.createOutputStream(_("hello"), OutputMode.OVERWRITE);
        outputStream.write(new byte[] { 1, 2, 3 });
        outputStream.close();
        outputStream.close();
    }

    public void shouldAllowClosingInputStreamTwice() throws IOException {
        OutputStream outputStream = fileSystem.createOutputStream(_("hello"), OutputMode.OVERWRITE);
        outputStream.write(new byte[] { 1, 2, 3 });
        outputStream.close();
        InputStream inputStream = fileSystem.createInputStream(_("hello"));
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, buffer);
        inputStream.close();
        inputStream.close();
    }

    private TPath _(String pathString) {
        return TPath.parse(pathString);
    }
}
