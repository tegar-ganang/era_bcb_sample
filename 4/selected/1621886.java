package uk.jumblebee.actions.jumble;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.apache.struts2.ServletActionContext;
import org.apache.commons.io.FileUtils;
import uk.jumblebee.actions.BaseAction;

public class JumbleAction extends BaseAction {

    private static final long serialVersionUID = -8733732111606843632L;

    private File Filedata;

    private String FiledataContentType;

    private String FiledataFileName;

    public File getFiledata() {
        return Filedata;
    }

    public void setFiledata(File filedata) {
        Filedata = filedata;
    }

    public String getFiledataContentType() {
        return FiledataContentType;
    }

    public void setFiledataContentType(String filedataContentType) {
        FiledataContentType = filedataContentType;
    }

    public String getFiledataFileName() {
        return FiledataFileName;
    }

    public void setFiledataFileName(String filedataFileName) {
        FiledataFileName = filedataFileName;
    }

    public String upload() {
        ServletContext context = ServletActionContext.getServletContext();
        String baseDir = context.getRealPath("/");
        String destFile = "/tech/java/code/ethantest/jumblebee/WebContent/uploads/buzz/" + this.FiledataFileName;
        String destFile2 = baseDir + "/uploads/buzz/" + this.FiledataFileName;
        try {
            FileUtils.copyFile(this.Filedata, new File(destFile));
            FileUtils.copyFile(this.Filedata, new File(destFile2));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(this.FiledataFileName);
        return "upload";
    }
}
