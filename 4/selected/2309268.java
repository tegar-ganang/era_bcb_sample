package si.cit.eprojekti.edocs.util.link;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.io.*;
import com.jcorporate.expresso.core.db.DBException;
import com.jcorporate.expresso.core.misc.DateTime;
import com.jcorporate.expresso.services.dbobj.Setup;
import si.cit.eprojekti.edocs.DocsSchema;
import si.cit.eprojekti.edocs.dbobj.AllowedUser;
import si.cit.eprojekti.edocs.dbobj.Category;
import si.cit.eprojekti.edocs.dbobj.Document;
import si.cit.eprojekti.edocs.dbobj.DocumentFile;
import si.cit.eprojekti.edocs.dbobj.ProjectData;
import si.cit.eprojekti.projectvianet.util.link.ICategory;
import si.cit.eprojekti.projectvianet.util.link.IPVNFacade;

/**
 * @author Toma� Taks
 *
 * Created 2004.10.06 12:10:23
 * 
 * DocsFacade description:
 * Programer's Facade for eDocs component
 */
public class DocsFacade implements IPVNFacade {

    /**
	 * Constructor
	 */
    public DocsFacade() {
    }

    private void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close();
        out.close();
    }

    public int createDocument(int UID, int categoryId, String documentTitle) throws DBException {
        Document newDoc = new Document();
        newDoc.setField("CategoryId", categoryId);
        newDoc.setField("DocumentTitle", documentTitle);
        newDoc.setField("DocumentVisible", "V");
        newDoc.setField("DocumentLanguage", "ENG");
        newDoc.setField("AuthorId", "0");
        newDoc.add();
        newDoc.setRequestingUid(UID);
        newDoc.setField("DocumentApproved", UID);
        newDoc.setField("UpdatedUid", UID);
        newDoc.setField("CreatedUid", UID);
        newDoc.update(true);
        return newDoc.getFieldInt("DocumentId");
    }

    public void duplicateFile(int fileId, int documentId) throws DBException {
        DocumentFile file = new DocumentFile();
        file.setField("FileId", fileId);
        file.retrieve();
        DocumentFile f = new DocumentFile();
        f.setField("DocumentId", documentId);
        f.setField("ProjectId", file.getField("ProjectId"));
        f.setField("FileTitle", file.getField("FileTitle"));
        f.setField("FileDesc", file.getField("FileDesc"));
        f.setField("FileName", file.getField("FileName"));
        f.setField("TypeId", file.getField("TypeId"));
        f.setField("FileVisible", file.getField("FileVisible"));
        f.setField("FileURL", file.getField("FileURL"));
        f.setField("CreatedUid", file.getField("CreatedUid"));
        f.setField("UpdatedUid", file.getField("UpdatedUid"));
        f.add();
    }

    public int duplicateDocument(int documentId, int newCategoryId, boolean copyAttachFiles) throws Exception {
        Document doc = new Document();
        doc.setField("DocumentId", documentId);
        doc.retrieve();
        Document newDoc = new Document();
        newDoc.setField("CategoryId", newCategoryId);
        newDoc.setField("DocumentTitle", doc.getField("DocumentTitle"));
        newDoc.setField("DocumentBody", doc.getField("DocumentBody"));
        newDoc.setField("DocumentSource", doc.getField("DocumentSource"));
        newDoc.setField("DocumentLanguage", doc.getField("DocumentLanguage"));
        newDoc.setField("AuthorId", doc.getField("AuthorId"));
        newDoc.setField("DocumentVisible", doc.getField("DocumentVisible"));
        newDoc.setField("Created", DateTime.getDateTimeForDB());
        newDoc.setField("CreatedUid", doc.getField("CreatedUid"));
        newDoc.setField("Updated", DateTime.getDateTimeForDB());
        newDoc.setField("UpdatedUid", doc.getField("UpdatedUid"));
        newDoc.add();
        newDoc.setRequestingUid(doc.getFieldInt("UpdatedUid"));
        newDoc.setField("DocumentApproved", doc.getField("DocumentApproved"));
        newDoc.setField("CheckedOutBy", "0");
        newDoc.setField("DocumentVersion", doc.getField("DocumentVersion"));
        newDoc.setField("TempVersionDoc", "0");
        newDoc.setField("UpdatedUid", doc.getFieldInt("UpdatedUid"));
        newDoc.update(true);
        if (copyAttachFiles) {
            int projectId = doc.getCategoryProjectIdFromDocument(doc.getFieldInt("CategoryId"));
            String fileDirectory = Setup.getValue("", "BaseDir") + Setup.getValue("", "si.cit.eprojekti.edocs.DocsSchema", "docsFileDir");
            fileDirectory += projectId + "/";
            File isDir = new File(fileDirectory + newDoc.getField("DocumentId") + "/" + doc.getField("DocumentVersion"));
            if (!isDir.isDirectory()) isDir.mkdirs();
            DocumentFile file = new DocumentFile();
            Vector fileV = new Vector();
            fileV = file.getFilesWithNoHistory(documentId);
            for (int i = 0; i < fileV.size(); i++) {
                file = new DocumentFile();
                file.setField("FileId", String.valueOf(fileV.elementAt(i)));
                file.retrieve();
                String fn = fileDirectory + documentId + "/" + doc.getField("DocumentVersion") + "/" + file.getField("FileName");
                String nfn = fileDirectory + newDoc.getField("DocumentId") + "/" + doc.getField("DocumentVersion") + "/" + file.getField("FileName");
                File ff1 = new File(fn);
                File ff2 = new File(nfn);
                copyFile(ff1, ff2);
                duplicateFile(file.getFieldInt("FileId"), newDoc.getFieldInt("DocumentId"));
            }
        }
        return newDoc.getFieldInt("DocumentId");
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getComponentElementsName()
	 */
    public String getComponentElementsName() {
        return "Documents";
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getComponentName()
	 */
    public String getComponentName() {
        return "eDocs";
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getComponentKeyName()
	 */
    public String getComponentKeyName() {
        return "eDocs";
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getComponentTinyIconFullPath()
	 */
    public String getComponentTinyIconFullPath() {
        return "icon_edocs_10x10.gif";
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#isCategoryComponent()
	 */
    public boolean isCategoryComponent() {
        return true;
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#enableAdvancedSecurity()
	 */
    public void enableAdvancedSecurity() throws Exception {
        Setup set = new Setup();
        set.setField(Setup.SCHEMA_CLASS, DocsSchema.class.getName());
        set.setField(Setup.SETUP_CODE, "UseAdvancedSecurity");
        set = (Setup) set.searchAndRetrieveList().get(0);
        set.setField(Setup.SETUP_VALUE, "Y");
        set.update();
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#disableAdvancedSecurity()
	 */
    public void disableAdvancedSecurity() throws Exception {
        Setup set = new Setup();
        set.setField(Setup.SCHEMA_CLASS, DocsSchema.class.getName());
        set.setField(Setup.SETUP_CODE, "UseAdvancedSecurity");
        set = (Setup) set.searchAndRetrieveList().get(0);
        set.setField(Setup.SETUP_VALUE, "N");
        set.update();
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#grantUserOnProject(int, int, String)
	 */
    public void grantUserOnProject(int UID, int PID, String groupCode) throws Exception {
        AllowedUser au = new AllowedUser();
        au.setField(AllowedUser.FLD_PID, PID);
        au.setField(AllowedUser.FLD_UID, UID);
        if (au.find()) {
            au.delete();
            au = new AllowedUser();
            au.setField(AllowedUser.FLD_PID, PID);
            au.setField(AllowedUser.FLD_UID, UID);
        }
        au.setField(AllowedUser.FLD_GROUP, groupCode);
        au.add();
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#revokeUserOnProject(int, int)
	 */
    public void revokeUserOnProject(int UID, int PID) throws Exception {
        AllowedUser au = new AllowedUser();
        au.setField("ProjectID", PID);
        au.setField("UID", UID);
        ArrayList al = au.searchAndRetrieveList();
        for (int i = 0; i < al.size(); i++) ((AllowedUser) al.get(i)).delete();
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getProjectCategories(int)
	 */
    public ICategory[] getProjectCategories(int PID) throws Exception {
        Category cat = new Category();
        cat.setField("ProjectId", PID);
        ICategory[] ret = new ICategory[cat.count()];
        int ii = 0;
        ArrayList allRecords = cat.searchAndRetrieveList();
        Iterator i = allRecords.iterator();
        while (i.hasNext()) {
            cat = (Category) i.next();
            ret[ii] = cat;
            ii++;
        }
        return ret;
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getTopLevelProjectCategories(int)
	 */
    public ICategory[] getTopLevelProjectCategories(int PID) throws Exception {
        ICategory[] temp = getProjectCategories(PID);
        int count = 0;
        for (int i = 0; i < temp.length; i++) {
            Category curr = (Category) temp[i];
            if (curr.getFieldInt("SuperCategory") == 0) count++;
        }
        ICategory[] ret = new ICategory[count];
        count = 0;
        for (int i = 0; i < temp.length; i++) {
            Category curr = (Category) temp[i];
            if (curr.getFieldInt("SuperCategory") == 0) {
                ret[count] = curr;
                count++;
            }
        }
        return ret;
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#createProjectEnvironment(int, java.lang.String, java.lang.String, boolean, java.lang.String)
	 */
    public void createProjectEnvironment(int PID, String name, String description, boolean enabled, String type) throws Exception {
        ProjectData pd = new ProjectData();
        pd.setField(ProjectData.FLD_PID, PID);
        pd.setField(ProjectData.FLD_ENABLED, enabled);
        pd.setField(ProjectData.FLD_TYPE, type);
        pd.add();
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#disableProject(int)
	 */
    public void disableProject(int PID) throws Exception {
        ProjectData pd = new ProjectData();
        pd.setField("ProjectID", PID);
        if (pd.find()) {
            pd.setField("Enabled", false);
            pd.update();
        }
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#enableProject(int)
	 */
    public void enableProject(int PID) throws Exception {
        ProjectData pd = new ProjectData();
        pd.setField("ProjectID", PID);
        if (pd.find()) {
            pd.setField("Enabled", true);
            pd.update();
        }
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#setProjectLeader(int,int)
	 */
    public void setProjectLeader(int PID, int UID) throws Exception {
        grantUserOnProject(UID, PID, "PVN_LEADER");
        setCategoryAprover(PID, UID);
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getURLToProject(int)
	 */
    public String getURLToProject(int PID) {
        return "edocs/Document.do?projectId=" + PID;
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getURLToCategory(int)
	 */
    public String getURLToCategory(int category) {
        return "edocs/Document.do?state=listDocumentCategoryState&category=" + category;
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#createCategoryInProject(int,si.cit.eprojekti.projectvianet.util.link.ICategory)
	 */
    public int createCategoryInProject(int PID, ICategory cat) throws Exception {
        Category nc = new Category();
        nc.setField("ProjectId", PID);
        nc.setField("CategoryName", cat.getCategoryName());
        nc.setField("CategoryDesc", cat.getCategoryDescription());
        nc.setField("CategoryVisible", cat.isCategoryVisible() ? "V" : "H");
        nc.add();
        return nc.getFieldInt("CategoryId");
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#createCategoryInProject(int, int, si.cit.eprojekti.projectvianet.util.link.ICategory)
	 */
    public int createCategoryInProject(int PID, int superCat, ICategory cat) throws Exception {
        Category nc = new Category();
        nc.setField("ProjectId", PID);
        if (superCat != -1) nc.setField("SuperCategory", superCat);
        nc.setField("CategoryName", cat.getCategoryName());
        nc.setField("CategoryDesc", cat.getCategoryDescription());
        nc.setField("CategoryVisible", cat.isCategoryVisible() ? "V" : "H");
        nc.add();
        return nc.getFieldInt("CategoryId");
    }

    /**
	 * @see si.cit.eprojekti.projectvianet.util.link.IPVNFacade#getCategoryByID(int)
	 */
    public ICategory getCategoryByID(int catId) throws Exception {
        Category c = new Category();
        c.setField("CategoryId", catId);
        if (c.find()) return c;
        return null;
    }

    public void setCategoryAprover(int PID, int UID) throws Exception {
        Category uc = new Category();
        uc.setField("CategoryId", PID);
        uc.setField("CategoryAprover", UID);
        uc.update(true);
    }
}
