package fr.umlv.jee.hibou.web.project;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts2.ServletActionContext;
import com.opensymphony.xwork2.ActionSupport;
import fr.umlv.jee.hibou.web.session.UserSession;
import fr.umlv.jee.hibou.wsclient.DocumentBean;
import fr.umlv.jee.hibou.wsclient.DocumentListBean;
import fr.umlv.jee.hibou.wsclient.HibouWS;
import fr.umlv.jee.hibou.wsclient.HibouWSService;

/**
 * This class manages the download of project documents
 * @author nak, alex, micka, matt
 *
 */
public class DownloadFilesProject extends ActionSupport {

    private static final long serialVersionUID = 1457916763594396982L;

    private static final String TECHNICAL_ERROR = "technical_error";

    private final HibouWS hibouWSPort = new HibouWSService().getHibouWSPort();

    /**
	 * Init the page
	 */
    public String execute() {
        try {
            System.out.println("download execute() : projectName --> " + projectName);
            DocumentListBean documentListBean = hibouWSPort.getDownloadableDocuments(projectName);
            UserSession usr = (UserSession) ServletActionContext.getRequest().getSession().getAttribute("navigationContext");
            username = usr.getUser().getEmail();
            for (DocumentBean doc : documentListBean.getDocumentationsList()) {
                listDocumentation.add(doc.getFilename());
            }
            for (DocumentBean doc : documentListBean.getMavenList()) {
                listMaven.add(doc.getFilename());
            }
            for (DocumentBean doc : documentListBean.getArchivesList()) {
                listArchives.add(doc.getFilename());
            }
            return SUCCESS;
        } catch (Exception e) {
            return TECHNICAL_ERROR;
        }
    }

    /**
	 * this method download one document choose by a user
	 * @return the file download
	 */
    public String downloadProjectFile() {
        try {
            System.out.println("download() : projectName --> " + projectName);
            System.out.println("nom du fichier : " + fileName);
            System.out.println("type : " + type);
            String hibouHome = System.getenv().get("HIBOU_HOME");
            String separator = System.getProperty("file.separator");
            String path = hibouHome + separator + projectName + separator + type + separator + fileName;
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
            if (type.equals("Archives")) {
                hibouWSPort.incrementDownloadStatistics();
            }
            return null;
        } catch (Exception e) {
            return TECHNICAL_ERROR;
        }
    }

    private String fileName;

    private List<String> listDocumentation = new ArrayList<String>();

    private List<String> listMaven = new ArrayList<String>();

    private List<String> listArchives = new ArrayList<String>();

    private String projectName;

    private String username;

    private String type;

    /**
	 * @return the fileName
	 */
    public String getFileName() {
        return fileName;
    }

    /**
	 * @param fileName the fileName to set
	 */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
	 * @return the listArchives
	 */
    public List<String> getListArchives() {
        return listArchives;
    }

    /**
	 * @param listArchives the listArchives to set
	 */
    public void setListArchives(List<String> listArchives) {
        this.listArchives = listArchives;
    }

    /**
	 * @return the listDocumentation
	 */
    public List<String> getListDocumentation() {
        return listDocumentation;
    }

    /**
	 * @param listDocumentation the listDocumentation to set
	 */
    public void setListDocumentation(List<String> listDocumentation) {
        this.listDocumentation = listDocumentation;
    }

    /**
	 * @return the listMaven
	 */
    public List<String> getListMaven() {
        return listMaven;
    }

    /**
	 * @param listMaven the listMaven to set
	 */
    public void setListMaven(List<String> listMaven) {
        this.listMaven = listMaven;
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
	 * @return the type
	 */
    public String getType() {
        return type;
    }

    /**
	 * @param type the type to set
	 */
    public void setType(String type) {
        this.type = type;
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
