package dms.core.document.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import com.opensymphony.xwork2.ActionContext;
import dms.core.actions.Action;
import dms.core.actions.ActionBase;
import dms.core.document.folder.logic.Folder;
import dms.core.document.folder.logic.FolderModel;
import dms.core.document.folder.logic.FolderService;
import dms.core.document.logic.Document;
import dms.core.document.logic.DocumentModel;
import dms.core.document.logic.DocumentService;
import dms.core.document.resourceauthority.logic.ResourceAuthorityService;
import dms.core.user.logic.User;
import dms.core.user.logic.UserService;
import dms.portal.preference.logic.Preference;
import dms.portal.preference.logic.PreferenceService;
import dms.search.logic.SearchService;
import dms.util.Constants;

public class UploadAction extends ActionBase implements Action {

    private PreferenceService prefService;

    private UserService userService;

    private DocumentService documentService;

    private FolderService folderService;

    private SearchService searchService;

    private ResourceAuthorityService resourceAuthorityService;

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private Long wid;

    private Long fid;

    private String action;

    private String docid;

    private Document document = new DocumentModel();

    private Preference preference;

    private static final String FORWARD_UPLOAD = "upload";

    private static final String FORWARD_META = "meta";

    private static final String FORWARD_CUSTOM_META = "custom-meta";

    private static Logger log = Logger.getLogger(UploadAction.class);

    public String execute() throws Exception {
        String forward = FORWARD_UPLOAD;
        if (getUpload() != null && getAction() != null) {
            if (FORWARD_UPLOAD.equals(getAction())) {
                uploadFile(getUpload());
                forward = FORWARD_META;
            }
        } else if (getAction() != null) {
            if (FORWARD_META.equals(getAction())) {
                Document oriDocument = (Document) documentService.find(document.getId());
                oriDocument.setName(document.getName());
                oriDocument.setDescr(document.getDescr());
                oriDocument.setTitle(document.getTitle());
                log.info("update meta fid:" + fid);
                log.info("update meta after set values " + document.getName().toString());
                log.info("update meta for " + document.getId());
                updateMeta(oriDocument);
                try {
                    indexDocument(oriDocument);
                } catch (Exception e) {
                    addActionError(getText("document.upload.index.failed"));
                    e.printStackTrace();
                }
                forward = SUCCESS;
            } else if (FORWARD_CUSTOM_META.equals(getAction())) {
            }
        }
        return forward;
    }

    private void uploadFile(File file) throws Exception {
        Document document = extractMeta(file);
        log.info("extracted meta: " + document.getName());
        document = saveDocument(document);
        log.info("saved document: " + document.getId());
        document = saveFile(document, file);
        log.info("saved file: " + document.getId());
        resourceAuthorityService.applyAuthority(document);
        log.info("applied authority:" + document.getId());
        setDocid(document.getId().toString());
        setDocument(document);
    }

    private Document extractMeta(File file) throws Exception {
        Map session = ActionContext.getContext().getSession();
        Document document = new DocumentModel();
        document.setName(getUploadFileName());
        document.setContentType(getUploadContentType());
        document.setExt(getExtension(getUploadFileName()));
        document.setSize(file.length());
        Folder folder = new FolderModel();
        folder = (Folder) folderService.find(getFid());
        log.info("extractMeta folder id:" + folder.getId());
        log.info("extractMeta workspace id:" + folder.getWorkspace().getId());
        if (folder != null && folder.getWorkspace() != null) {
            document.setFolder(folder);
            document.setWorkspace(folder.getWorkspace());
        } else {
            log.info("Upload action unable to find folder [" + getFid() + "]");
        }
        if (session.get(Constants.SESSION_USER) != null) {
            document.setOwner((User) session.get(Constants.SESSION_USER));
        }
        document.setVersion(Constants.VERSION_1);
        document = (Document) preAdd(document, session);
        return document;
    }

    private Document saveDocument(Document document) throws Exception {
        documentService.save(document);
        return document;
    }

    private Document saveFile(Document document, File file) throws Exception {
        List<Preference> preferences = prefService.findAll();
        if (preferences != null && !preferences.isEmpty()) {
            preference = preferences.get(0);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATEFORMAT_YYYYMMDD);
        String repo = preference.getRepository();
        Calendar calendar = Calendar.getInstance();
        StringBuffer sbRepo = new StringBuffer(repo);
        sbRepo.append(File.separator);
        StringBuffer sbFolder = new StringBuffer(sdf.format(calendar.getTime()));
        sbFolder.append(File.separator).append(calendar.get(Calendar.HOUR_OF_DAY));
        File folder = new File(sbRepo.append(sbFolder).toString());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        FileChannel fcSource = null, fcDest = null;
        try {
            StringBuffer sbFile = new StringBuffer(folder.getAbsolutePath());
            StringBuffer fname = new StringBuffer(document.getId().toString());
            fname.append(".").append(document.getExt());
            sbFile.append(File.separator).append(fname);
            fcSource = new FileInputStream(file).getChannel();
            fcDest = new FileOutputStream(sbFile.toString()).getChannel();
            fcDest.transferFrom(fcSource, 0, fcSource.size());
            document.setLocation(sbFolder.toString());
            documentService.save(document);
        } catch (FileNotFoundException notFoundEx) {
            log.error("saveFile file not found: " + document.getName(), notFoundEx);
        } catch (IOException ioEx) {
            log.error("saveFile IOException: " + document.getName(), ioEx);
        } finally {
            try {
                if (fcSource != null) {
                    fcSource.close();
                }
                if (fcDest != null) {
                    fcDest.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return document;
    }

    private String getExtension(String fName) {
        return fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
    }

    private void updateMeta(Document document) throws Exception {
        if (document != null && document.getId() != null) {
            documentService.save(document);
        }
        if (document.getId() != null) {
            document = (Document) documentService.find(document.getId());
            setFid(document.getFolder().getId());
            log.info("updateMeta: " + getFid());
        }
    }

    private void indexDocument(Document document) throws Exception {
        if (document != null && document.getId() != null) {
            List<Preference> preferences = prefService.findAll();
            if (preferences != null && !preferences.isEmpty()) {
                preference = preferences.get(0);
            }
            searchService.indexDocument(preference, document.getId());
        }
    }

    public UserService getUserService() {
        return userService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public PreferenceService getPrefService() {
        return prefService;
    }

    public void setPrefService(PreferenceService prefService) {
        this.prefService = prefService;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public FolderService getFolderService() {
        return folderService;
    }

    public void setFolderService(FolderService folderService) {
        this.folderService = folderService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
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

    public Long getWid() {
        return wid;
    }

    public void setWid(Long wid) {
        this.wid = wid;
    }

    public Long getFid() {
        return fid;
    }

    public void setFid(Long fid) {
        this.fid = fid;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDocid() {
        return docid;
    }

    public void setDocid(String docid) {
        this.docid = docid;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public ResourceAuthorityService getResourceAuthorityService() {
        return resourceAuthorityService;
    }

    public void setResourceAuthorityService(ResourceAuthorityService resourceAuthorityService) {
        this.resourceAuthorityService = resourceAuthorityService;
    }
}
