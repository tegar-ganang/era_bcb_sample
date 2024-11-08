package whiteboard;

import java.io.File;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.util.ServletContextAware;
import whiteboard.course.Course;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

/**
 * Action used to upload a file for a document.
 * 
 * Src: http://java.dzone.com/articles/struts2-tutorial-part-67 Only works for
 * one folder directory, hope to expand to create dir for each course
 */
public class FileUploadAction extends ActionSupport implements ServletRequestAware, ServletContextAware {

    private File userImage;

    private String userImageContentType;

    private String userImageFileName;

    private String title;

    private String text;

    private HttpServletRequest servletRequest;

    private ServletContext servletContext;

    public String execute() {
        try {
            String filePath = servletRequest.getRealPath("/whiteboard/upload");
            System.out.println("Server Path :" + filePath);
            System.out.println("FILE NAME: " + userImageFileName);
            File fileToCreate = new File(filePath, this.userImageFileName);
            this.userImageFileName = "upload/" + this.userImageFileName;
            FileUtils.copyFile(this.userImage, fileToCreate);
            HomePage homepage = getHomePage();
            Course selectedCourse = homepage.getSelectedCourse();
            String action = selectedCourse.getAction();
            String typeOfDoc = DocumentAction.findTypeOfDoc(action);
            String typeOfAction = DocumentAction.findTypeOfAction(action);
            String whatDocument = selectedCourse.getWhatDocument();
            if (typeOfAction.compareTo("edit") == 0) {
                Document selectedDoc = null;
                if (typeOfDoc.compareTo("notes") == 0) {
                    selectedDoc = selectedCourse.extractNote(whatDocument);
                } else if (typeOfDoc.compareTo("announcements") == 0) {
                    selectedDoc = selectedCourse.extractAnnouncement(whatDocument);
                } else if (typeOfDoc.compareTo("homework") == 0) {
                    selectedDoc = selectedCourse.extractHomework(whatDocument);
                } else {
                    System.out.println("ERRor in upload");
                }
                selectedDoc.setLink(userImageFileName);
            } else {
                selectedCourse.setTempLink(userImageFileName);
                if (getText() == null) {
                    selectedCourse.setTempText("");
                } else {
                    selectedCourse.setTempText(getText());
                }
                if (getTitle() == null) {
                    selectedCourse.setTempTitle("");
                } else {
                    selectedCourse.setTempTitle(getTitle());
                }
                System.out.println("Set temp text and title to:---->" + getText() + "   " + getTitle());
                System.out.println("Set temp link to " + userImageFileName);
                String text2 = (String) servletContext.getAttribute("text");
                System.out.println("from  context :" + text2);
            }
        } catch (Exception e) {
            return INPUT;
        }
        return SUCCESS;
    }

    private HomePage getHomePage() {
        Map<String, Object> attibutes = ActionContext.getContext().getSession();
        return ((HomePage) attibutes.get("homePage"));
    }

    public File getUserImage() {
        return userImage;
    }

    public void setUserImage(File userImage) {
        this.userImage = userImage;
    }

    public String getUserImageContentType() {
        return userImageContentType;
    }

    public void setUserImageContentType(String userImageContentType) {
        this.userImageContentType = userImageContentType;
    }

    public String getUserImageFileName() {
        return userImageFileName;
    }

    public void setUserImageFileName(String userImageFileName) {
        this.userImageFileName = userImageFileName;
    }

    @Override
    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    @Override
    public void setServletContext(ServletContext arg0) {
        this.servletContext = arg0;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
