package com.jaeksoft.searchlib.crawler.file.process.fileInstances;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import com.jaeksoft.searchlib.Logging;
import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.crawler.file.database.FilePathItem;
import com.jaeksoft.searchlib.crawler.file.database.FileTypeEnum;
import com.jaeksoft.searchlib.crawler.file.process.FileInstanceAbstract;
import com.jaeksoft.searchlib.util.LinkUtils;

public class FtpFileInstance extends FileInstanceAbstract {

    private FTPFile ftpFile;

    public FtpFileInstance() {
        ftpFile = null;
    }

    protected FtpFileInstance(FilePathItem filePathItem, FtpFileInstance parent, FTPFile ftpFile) throws URISyntaxException, SearchLibException {
        init(filePathItem, parent, LinkUtils.concatPath(parent.getPath(), ftpFile.getName()));
        this.ftpFile = ftpFile;
    }

    @Override
    public URI init() throws URISyntaxException {
        return new URI("ftp", filePathItem.getHost(), getPath(), null);
    }

    @Override
    public FileTypeEnum getFileType() {
        if (ftpFile == null) return FileTypeEnum.directory;
        switch(ftpFile.getType()) {
            case FTPFile.DIRECTORY_TYPE:
                return FileTypeEnum.directory;
            case FTPFile.FILE_TYPE:
                return FileTypeEnum.file;
        }
        return null;
    }

    protected FTPFile getFTPFile() {
        return ftpFile;
    }

    protected FTPClient ftpConnect() throws SocketException, IOException, NoSuchAlgorithmException {
        FilePathItem fpi = getFilePathItem();
        FTPClient f = new FTPClient();
        f.connect(fpi.getHost());
        f.login(fpi.getUsername(), fpi.getPassword());
        return f;
    }

    private void ftpQuietDisconnect(FTPClient f) {
        if (f == null) return;
        try {
            f.disconnect();
        } catch (IOException e) {
            Logging.warn(e);
        }
    }

    protected FtpFileInstance newInstance(FilePathItem filePathItem, FtpFileInstance parent, FTPFile ftpFile) throws URISyntaxException, SearchLibException {
        return new FtpFileInstance(filePathItem, parent, ftpFile);
    }

    private FileInstanceAbstract[] buildFileInstanceArray(FTPFile[] files) throws URISyntaxException, SearchLibException {
        if (files == null) return null;
        FileInstanceAbstract[] fileInstances = new FileInstanceAbstract[files.length];
        int i = 0;
        for (FTPFile file : files) fileInstances[i++] = newInstance(filePathItem, this, file);
        return fileInstances;
    }

    private class FileOnlyDirectoryFilter implements FTPFileFilter {

        @Override
        public boolean accept(FTPFile ff) {
            return ff.getType() == FTPFile.FILE_TYPE;
        }
    }

    @Override
    public FileInstanceAbstract[] listFilesOnly() throws SearchLibException {
        FTPClient f = null;
        try {
            f = ftpConnect();
            FTPFile[] files = f.listFiles(getPath(), new FileOnlyDirectoryFilter());
            return buildFileInstanceArray(files);
        } catch (SocketException e) {
            throw new SearchLibException(e);
        } catch (IOException e) {
            throw new SearchLibException(e);
        } catch (URISyntaxException e) {
            throw new SearchLibException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SearchLibException(e);
        } finally {
            ftpQuietDisconnect(f);
        }
    }

    @Override
    public FileInstanceAbstract[] listFilesAndDirectories() throws SearchLibException {
        FTPClient f = null;
        try {
            f = ftpConnect();
            FTPFile[] files = f.listFiles(getPath());
            return buildFileInstanceArray(files);
        } catch (SocketException e) {
            throw new SearchLibException(e);
        } catch (IOException e) {
            throw new SearchLibException(e);
        } catch (URISyntaxException e) {
            throw new SearchLibException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SearchLibException(e);
        } finally {
            ftpQuietDisconnect(f);
        }
    }

    @Override
    public Long getLastModified() {
        if (ftpFile == null) return null;
        return ftpFile.getTimestamp().getTimeInMillis();
    }

    @Override
    public Long getFileSize() {
        if (ftpFile == null) return null;
        return ftpFile.getSize();
    }

    @Override
    public String getFileName() throws SearchLibException {
        if (ftpFile == null) return null;
        return ftpFile.getName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        FTPClient f = null;
        try {
            f = ftpConnect();
            return f.retrieveFileStream(getPath());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        } finally {
            ftpQuietDisconnect(f);
        }
    }
}
