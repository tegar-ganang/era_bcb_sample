package fr.umlv.jee.hibou.web.visitor;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.util.ListEntry;
import com.opensymphony.xwork2.ActionSupport;
import fr.umlv.jee.hibou.web.session.UserSession;
import fr.umlv.jee.hibou.wsclient.DescriptionBean;
import fr.umlv.jee.hibou.wsclient.HibouWS;
import fr.umlv.jee.hibou.wsclient.HibouWSService;
import fr.umlv.jee.hibou.wsclient.ProjectBean;
import fr.umlv.jee.hibou.wsclient.ProjectListBean;
import fr.umlv.jee.hibou.wsclient.UserBean;
import fr.umlv.jee.hibou.wsclient.UserListBean;

/**
 * This class allows the users to reach the description of a project, the FAQ of a project 
 * and with the remote loading of the file of a project  
 * @author alex, matt, micka, nak
 *
 */
public class FreeProjectAccess extends ActionSupport {

    private static final long serialVersionUID = -8839512354471432217L;

    private static final String TECHNICAL_ERROR = "technical_error";

    private static final String FREE_PROJECT_VIEW = "free_project_view";

    private final HibouWS hibouWSPort = new HibouWSService().getHibouWSPort();

    /**
	 * Init the page
	 */
    public String execute() throws Exception {
        System.out.println("init download project");
        UserSession usr = (UserSession) ServletActionContext.getRequest().getSession().getAttribute("navigationContext");
        if (usr == null) {
            System.out.println("not connect");
            connect = false;
        } else {
            System.out.println("is connect");
            connect = true;
            username = usr.getUser().getEmail();
        }
        display = false;
        categories.add(new ListEntry("", "", false));
        categories.add(new ListEntry("desktop", getText("desktop"), false));
        categories.add(new ListEntry("database", getText("database"), false));
        categories.add(new ListEntry("entreprise", getText("entreprise"), false));
        categories.add(new ListEntry("financial", getText("financial"), false));
        categories.add(new ListEntry("game", getText("game"), false));
        categories.add(new ListEntry("multimedia", getText("multimedia"), false));
        categories.add(new ListEntry("networking", getText("networking"), false));
        categories.add(new ListEntry("security", getText("security"), false));
        return SUCCESS;
    }

    /**
	 * return a list of project by category
	 */
    public String ajax() {
        try {
            System.out.println("category : " + category);
            if ("".equals(category)) {
                display = false;
            } else {
                ProjectListBean projectListBean = hibouWSPort.getProjectsByCategory(category);
                if (projectListBean == null) {
                    return TECHNICAL_ERROR;
                }
                for (ProjectBean projectBean : projectListBean.getList()) {
                    projects.add(projectBean.getProjectName());
                }
                display = true;
            }
            return "ajax";
        } catch (Exception e) {
            return TECHNICAL_ERROR;
        }
    }

    /**
	 * Init the project view page
	 */
    public String initFreeProjectView() {
        try {
            UserSession usr = (UserSession) ServletActionContext.getRequest().getSession().getAttribute("navigationContext");
            if (usr == null) {
                System.out.println("not connect");
                connect = false;
            } else {
                System.out.println("is connect");
                connect = true;
                username = usr.getUser().getEmail();
            }
            System.out.println("initFreeProjectView ---> nom du projet : " + projectName);
            ProjectBean project = hibouWSPort.getProject(projectName);
            creationDate = project.getCreationDate();
            DescriptionBean descriptionBean = project.getDescription();
            language = descriptionBean.getLanguage();
            category = descriptionBean.getCategory();
            description = descriptionBean.getDescriptionText();
            archive = hibouWSPort.hasArchive(projectName);
            UserListBean userListBean = hibouWSPort.getProjectLeaders(projectName);
            StringBuilder stringBuilder = new StringBuilder();
            for (UserBean userBean : userListBean.getList()) {
                stringBuilder.append(userBean.getEmail());
                stringBuilder.append(", ");
            }
            stringBuilder.replace(stringBuilder.length() - 2, stringBuilder.length(), "");
            projectLeader = stringBuilder.toString();
            return FREE_PROJECT_VIEW;
        } catch (Exception e) {
            return TECHNICAL_ERROR;
        }
    }

    /**
	 * Download one archive by one user
	 * @return the file
	 */
    public String downloadArchive() {
        try {
            System.out.println("download() : projectName --> " + projectName);
            String fileName = projectName + ".zip";
            String hibouHome = System.getenv().get("HIBOU_HOME");
            String separator = System.getProperty("file.separator");
            String path = hibouHome + separator + projectName + separator + "Archives" + separator + fileName;
            File file = new File(path);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            HttpServletResponse response = ServletActionContext.getResponse();
            String mimetype = ServletActionContext.getServletContext().getMimeType(fileName);
            System.out.println("mimetype --> " + mimetype);
            response.setContentType(mimetype);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
            ServletOutputStream os = response.getOutputStream();
            BufferedOutputStream bos = new BufferedOutputStream(os);
            long l = file.length();
            for (long i = 0; i < l; i++) {
                bos.write(bis.read());
            }
            bos.flush();
            os.close();
            bis.close();
            hibouWSPort.incrementDownloadStatistics();
            return null;
        } catch (Exception e) {
            return TECHNICAL_ERROR;
        }
    }

    private List<ListEntry> categories = new ArrayList<ListEntry>();

    private String category;

    private boolean display;

    private List<String> projects = new ArrayList<String>();

    private String projectName;

    private String creationDate;

    private String language;

    private String description;

    private String username;

    private String projectLeader;

    private boolean connect;

    private boolean archive;

    /**
	 * @return the archive
	 */
    public boolean isArchive() {
        return archive;
    }

    /**
	 * @param archive the archive to set
	 */
    public void setArchive(boolean archive) {
        this.archive = archive;
    }

    /**
	 * @return the categories
	 */
    public List<ListEntry> getCategories() {
        return categories;
    }

    /**
	 * @param categories the categories to set
	 */
    public void setCategories(List<ListEntry> categories) {
        this.categories = categories;
    }

    /**
	 * @return the category
	 */
    public String getCategory() {
        return category;
    }

    /**
	 * @param category the category to set
	 */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
	 * @return the connect
	 */
    public boolean isConnect() {
        return connect;
    }

    /**
	 * @param connect the connect to set
	 */
    public void setConnect(boolean connect) {
        this.connect = connect;
    }

    /**
	 * @return the creationDate
	 */
    public String getCreationDate() {
        return creationDate;
    }

    /**
	 * @param creationDate the creationDate to set
	 */
    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    /**
	 * @return the description
	 */
    public String getDescription() {
        return description;
    }

    /**
	 * @param description the description to set
	 */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
	 * @return the display
	 */
    public boolean isDisplay() {
        return display;
    }

    /**
	 * @param display the display to set
	 */
    public void setDisplay(boolean display) {
        this.display = display;
    }

    /**
	 * @return the language
	 */
    public String getLanguage() {
        return language;
    }

    /**
	 * @param language the language to set
	 */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
	 * @return the projectLeader
	 */
    public String getProjectLeader() {
        return projectLeader;
    }

    /**
	 * @param projectLeader the projectLeader to set
	 */
    public void setProjectLeader(String projectLeader) {
        this.projectLeader = projectLeader;
    }

    /**
	 * @return the projectName
	 */
    public String getProjectName() {
        return projectName;
    }

    /**
	 * @param projectName the projectName to set
	 */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
	 * @return the projects
	 */
    public List<String> getProjects() {
        return projects;
    }

    /**
	 * @param projects the projects to set
	 */
    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    /**
	 * @return the username
	 */
    public String getUsername() {
        return username;
    }

    /**
	 * @param username the username to set
	 */
    public void setUsername(String username) {
        this.username = username;
    }
}
