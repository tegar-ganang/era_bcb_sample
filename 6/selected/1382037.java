package ces.research.oa.document.mail.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletContext;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import ces.research.oa.document.mail.upload.UploadConfig;

public class MFile {

    private File file = null;

    private String fileName = null;

    private String fileType = null;

    private String serverPath = null;

    private String savePath = null;

    private UploadConfig config = null;

    private InputStream input;

    public MFile() {
    }

    public MFile(File fil, String name, String type, InputStream in) {
        this.setFile(fil);
        this.setFileName(name);
        this.setFileType(type);
        this.setInput(in);
        config = new UploadConfig();
        savePath = config.getUploadPath();
        serverPath = config.getServerPath();
    }

    public MFile(InputStream in, String name, String type) {
        this.setFileName(name);
        this.setFileType(type);
        this.setInput(in);
        config = new UploadConfig();
        savePath = config.getUploadPath();
        serverPath = config.getServerPath();
    }

    public MFile(File fil, String name, String type, String path) {
        this.setFile(fil);
        this.setFileName(name);
        this.setFileType(type);
        this.setSavePath(path);
    }

    public String save(boolean big) throws IOException {
        return save(getSavePath(), big);
    }

    public String save(String path, boolean big) throws IOException {
        if (big) {
            return FtpUpload();
        }
        return HttpUpload(path);
    }

    public String HttpUpload(String path) throws IOException {
        File file = new File(path + "\\" + DateUtil.getSysmonth());
        if (!file.exists()) {
            file.mkdir();
        }
        OutputStream fos = new FileOutputStream(file.getAbsolutePath() + "\\" + getFileName());
        InputStream fis = null;
        if (null != getFile()) {
            fis = new FileInputStream(getFile());
        } else {
            fis = getInput();
        }
        byte[] buf = new byte[1024];
        int len = 0;
        while ((len = fis.read(buf)) > 0) {
            fos.write(buf, 0, len);
        }
        return DateUtil.getSysmonth();
    }

    public String FtpUpload() {
        return uploadFile(config.getHostUrl(), config.getFtpPort(), config.getUname(), config.getUpass(), getInput());
    }

    public String uploadFile(String url, int port, String uname, String upass, InputStream input) {
        String serverPath = config.getServerPath() + DateUtil.getSysmonth();
        FTPClient ftp = new FTPClient();
        try {
            int replyCode;
            ftp.connect(url, port);
            ftp.login(uname, upass);
            replyCode = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftp.disconnect();
                return config.getServerPath();
            }
            if (!ftp.changeWorkingDirectory(serverPath)) {
                ftp.makeDirectory(DateUtil.getSysmonth());
                ftp.changeWorkingDirectory(serverPath);
            }
            ftp.storeFile(getFileName(), input);
            input.close();
            ftp.logout();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serverPath;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    public InputStream getInput() {
        return input;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }
}
