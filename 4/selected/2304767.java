package struts2.sample10;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.StrutsStatics;
import org.apache.struts2.config.Result;
import org.apache.struts2.config.Results;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

/**
 * ファイルアップロードのサンプル。
 */
@SuppressWarnings("serial")
@Results({ @Result(name = "input", value = "upload_input.jsp"), @Result("upload_result.jsp") })
public class FileUploadSampleAction extends ActionSupport {

    /** アップロードファイル */
    private File upload;

    /** ファイルのContent-Type（プロパティ名＋「ContentType」） */
    private String uploadContentType;

    /** ファイル名（プロパティ名＋「FileName」） */
    private String uploadFileName;

    /** ファイルサイズ */
    private String fileSize;

    /**
	 * 初期表示メソッド
	 */
    public String input() {
        return "input";
    }

    /**
	 * アクションを実行するメソッド。
	 */
    public String execute() throws IOException {
        ActionContext ac = ActionContext.getContext();
        ServletContext sc = (ServletContext) ac.get(StrutsStatics.SERVLET_CONTEXT);
        File uploadDir = new File(sc.getRealPath("/WEB-INF/upload"));
        if (uploadDir.exists() == false) {
            uploadDir.mkdirs();
        }
        FileUtils.copyFile(upload, new File(uploadDir, uploadFileName));
        fileSize = FileUtils.byteCountToDisplaySize(upload.length());
        return "success";
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        this.upload = upload;
    }

    public String getUploadContentType() {
        return uploadContentType;
    }

    public void setUploadContentType(String uploadContentType) {
        this.uploadContentType = uploadContentType;
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        this.uploadFileName = uploadFileName;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }
}
