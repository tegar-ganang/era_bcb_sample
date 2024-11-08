package com.jaeksoft.searchlib.crawler.file.process.fileInstances;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.crawler.file.database.FilePathItem;

public class FtpsFileInstance extends FtpFileInstance {

    public FtpsFileInstance() {
        super();
    }

    public FtpsFileInstance(FilePathItem filePathItem, FtpFileInstance parent, FTPFile ftpFile) throws URISyntaxException, SearchLibException {
        super(filePathItem, parent, ftpFile);
    }

    @Override
    protected FtpFileInstance newInstance(FilePathItem filePathItem, FtpFileInstance parent, FTPFile ftpFile) throws URISyntaxException, SearchLibException {
        return new FtpsFileInstance(filePathItem, parent, ftpFile);
    }

    @Override
    public URI init() throws URISyntaxException {
        return new URI("ftps", filePathItem.getHost(), getPath(), null);
    }

    @Override
    protected FTPClient ftpConnect() throws SocketException, IOException, NoSuchAlgorithmException {
        FilePathItem fpi = getFilePathItem();
        FTPClient f = new FTPSClient();
        f.connect(fpi.getHost());
        f.login(fpi.getUsername(), fpi.getPassword());
        return f;
    }
}
