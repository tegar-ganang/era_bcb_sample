package com.tcs.hrr.action;

import java.io.File;
import org.apache.commons.io.FileUtils;
import com.opensymphony.xwork2.ActionSupport;
import com.tcs.hrr.util.Constant;
import com.tcs.hrr.util.FileAccessUtil;

public class MutilFileAction extends ActionSupport {

    private java.util.List<File> uploads;

    private java.util.List<String> fileNames;

    private java.util.List<String> uploadContentTypes;

    public java.util.List<String> getUploadFileName() {
        return fileNames;
    }

    public void setUploadFileName(java.util.List<String> fileNames) {
        this.fileNames = fileNames;
    }

    public java.util.List<File> getUpload() {
        return uploads;
    }

    public void setUpload(java.util.List<File> uploads) {
        this.uploads = uploads;
    }

    public void setUploadContentType(java.util.List<String> contentTypes) {
        this.uploadContentTypes = contentTypes;
    }

    public java.util.List<String> getUploadContentType() {
        return this.uploadContentTypes;
    }

    /**
	 * Upload the mutil-CV files
	 */
    public String execute() throws Exception {
        String path = FileAccessUtil.readPropertiesFile("CV_FILE_PATH");
        String uploadType = Constant.FILE_TYPE_CV;
        StringBuffer[] cvs = new StringBuffer[2];
        if (uploads.get(0) != null) {
            cvs[0] = new StringBuffer("");
            cvs[0].append("cn_" + FileAccessUtil.generateUploadFileName(fileNames.get(0), uploadType));
        }
        if (uploads.get(1) != null) {
            cvs[1] = new StringBuffer("");
            cvs[1].append("en_" + FileAccessUtil.generateUploadFileName(fileNames.get(1), uploadType));
        }
        System.out.println(cvs[0].toString());
        System.out.println(cvs[1].toString());
        File dir = new File(path);
        if (!dir.isDirectory()) dir.mkdirs();
        File target_cn = new File(path, cvs[0].toString());
        File target_en = new File(path, cvs[1].toString());
        FileUtils.copyFile(uploads.get(0), target_cn);
        FileUtils.copyFile(uploads.get(1), target_en);
        return SUCCESS;
    }
}
