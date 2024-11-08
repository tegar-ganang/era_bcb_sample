package com.tcs.hrr.action;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.util.ServletContextAware;
import com.opensymphony.xwork2.ActionSupport;
import com.tcs.hrr.util.Constant;
import com.tcs.hrr.util.FileAccessUtil;

public class FileAction extends ActionSupport {

    private static final int BUFFER_SIZE = 16 * 1024;

    private String title;

    private File upload;

    private String uploadFileName;

    private String uploadContentType;

    private String uploadType;

    public String getUploadType() {
        return uploadType;
    }

    public void setUploadType(String uploadType) {
        this.uploadType = uploadType;
    }

    /**
	 * Upload single requirement excel from ultimatix download
	 */
    @Override
    public String execute() throws Exception {
        String uploadType = Constant.FILE_TYPE_REQUIREMENT;
        String path = FileAccessUtil.readPropertiesFile("REQUIREMENT_FILE_PATH");
        File dir = new File(path.toString());
        if (!dir.isDirectory()) dir.mkdirs();
        String targetFileName = FileAccessUtil.generateUploadFileName(uploadFileName, uploadType);
        File target = new File(path.toString(), targetFileName);
        FileUtils.copyFile(upload, target);
        return SUCCESS;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public String getUploadContentType() {
        return uploadContentType;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public static int getBufferSize() {
        return BUFFER_SIZE;
    }
}
