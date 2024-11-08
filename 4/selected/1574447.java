package action;

import java.io.File;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.interceptor.ServletRequestAware;
import pojo.Store;
import pojo.User;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;
import dao.StoreDao;

public class AddStoreAction extends ActionSupport implements ServletRequestAware {

    private File userLogo;

    private String userLogoContentType;

    public File getUserLogo() {
        return userLogo;
    }

    public void setUserLogo(File userLogo) {
        this.userLogo = userLogo;
    }

    public String getUserLogoContentType() {
        return userLogoContentType;
    }

    public void setUserLogoContentType(String userLogoContentType) {
        this.userLogoContentType = userLogoContentType;
    }

    public String getUserLogoFileName() {
        return userLogoFileName;
    }

    public void setUserLogoFileName(String userLogoFileName) {
        this.userLogoFileName = userLogoFileName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMainpage() {
        return mainpage;
    }

    public void setMainpage(String mainpage) {
        this.mainpage = mainpage;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    private String userLogoFileName;

    private HttpServletRequest servletRequest;

    private String name;

    private String description;

    private String mainpage;

    private User user;

    public User getUser() {
        Map session = ActionContext.getContext().getSession();
        user = (User) session.get("user");
        return user;
    }

    @Override
    public String execute() {
        Map session = ActionContext.getContext().getSession();
        User user = (User) session.get("user");
        if (user == null) {
            return LOGIN;
        }
        if (name != null) {
            Store store = new Store(user, name);
            if (UploadLogo()) {
                SetDataToInsert(store);
                StoreDao.save(store);
                return SUCCESS;
            }
            return ERROR;
        }
        return ERROR;
    }

    public void SetDataToInsert(Store store) {
        store.setDescription(description);
        store.setMainpage(mainpage);
        store.setLogo(userLogoFileName);
    }

    public boolean UploadLogo() {
        try {
            String filePath = servletRequest.getRealPath("/") + "upload\\";
            File fileToCreate = new File(filePath, this.userLogoFileName);
            FileUtils.copyFile(this.userLogo, fileToCreate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }
}
