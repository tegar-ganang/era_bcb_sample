package dms.core.document.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.struts2.interceptor.validation.SkipValidation;
import com.opensymphony.xwork2.ActionContext;
import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;
import dms.core.actions.Action;
import dms.core.actions.ActionBase;
import dms.core.document.docversion.logic.DocVersion;
import dms.core.document.docversion.logic.DocVersionModel;
import dms.core.document.docversion.logic.DocVersionService;
import dms.core.document.folder.logic.Folder;
import dms.core.document.folder.logic.FolderModel;
import dms.core.document.folder.logic.FolderService;
import dms.core.document.logic.Document;
import dms.core.document.logic.DocumentModel;
import dms.core.document.logic.DocumentService;
import dms.core.document.resourceauthority.logic.ResourceAuthority;
import dms.core.document.resourceauthority.logic.ResourceAuthorityModel;
import dms.core.document.resourceauthority.logic.ResourceAuthorityService;
import dms.core.user.group.logic.Group;
import dms.core.user.group.logic.GroupModel;
import dms.core.user.group.logic.GroupService;
import dms.core.user.logic.User;
import dms.core.user.logic.UserModel;
import dms.core.user.logic.UserService;
import dms.core.user.role.logic.Role;
import dms.core.user.role.logic.RoleModel;
import dms.core.user.role.logic.RoleService;
import dms.portal.preference.logic.Preference;
import dms.portal.preference.logic.PreferenceService;
import dms.search.logic.SearchService;
import dms.util.Constants;

public class CommonAction extends ActionBase implements Action {

    private static final String TYPE_GROUP = "GROUP";

    private DocumentService documentService;

    private DocVersionService docVersionService;

    private FolderService folderService;

    private UserService userService;

    private GroupService groupService;

    private RoleService roleService;

    private PreferenceService prefService;

    private ResourceAuthorityService resourceAuthorityService;

    private SearchService searchService;

    private static Logger log = Logger.getLogger(CommonAction.class);

    private Document document;

    private Long docid;

    private Long fid;

    private Long wid;

    private List<Long> docids;

    private User crtBy;

    private User updBy;

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private String checkInMinor;

    private String checkInKeepOut;

    private String choice;

    private String name;

    private String type;

    private String action;

    private List<User> users;

    private List<Group> groups;

    private List<Role> roles;

    private List<Long> userids;

    private List<Long> groupids;

    private List<Long> roleids;

    private ResourceAuthority resourceAuthority = new ResourceAuthorityModel();

    private Long deleteid;

    private List<DocVersion> versions;

    private List<ResourceAuthority> authorities;

    private List<ResourceAuthority> userAuthorities;

    private Preference preference;

    private String operation;

    public CommonAction() {
        document = new DocumentModel();
        crtBy = new UserModel();
        updBy = new UserModel();
        versions = new ArrayList<DocVersion>();
        fid = 0L;
        wid = 0L;
        operation = "";
    }

    public String execute() {
        return SUCCESS;
    }

    public String showProperties() throws Exception {
        if (getDocid() != null) {
            setDocument((Document) documentService.find(getDocid()));
            if (getDocument() != null) {
                if (getDocument().getCrtby() != null) {
                    crtBy = (User) userService.find(getDocument().getCrtby());
                }
                if (getDocument().getUpdby() != null) {
                    updBy = (User) userService.find(getDocument().getUpdby());
                }
                if (getDocument().getFolder() != null) {
                    setFid(document.getFolder().getId());
                }
            }
            authorities();
            if (!StringUtils.isEmpty(StringUtils.trimToEmpty(getName()))) {
                if (TYPE_GROUP.equals(getType())) {
                    setGroups(groupService.findByName(getName()));
                } else {
                    setUsers(userService.findByName(getName()));
                }
            } else {
                setUsers(new ArrayList<User>());
                setGroups(new ArrayList<Group>());
            }
            setRoles(roleService.findAll());
        }
        return SUCCESS;
    }

    @SkipValidation
    public String saveAuthority() throws Exception {
        List<Preference> preferences = prefService.findAll();
        if (preferences != null && !preferences.isEmpty()) {
            preference = preferences.get(0);
        }
        log.info("saveAuthority document entry. docid:" + getDocid());
        Role role = new RoleModel();
        Group group = new GroupModel();
        User user = new UserModel();
        if (getText("generic.search").equals(action)) {
            return "search";
        } else if (getRoleids() != null && (getGroupids() != null || getUserids() != null)) {
            if (getDocid() != null) {
                document = documentService.find(getDocid());
            }
            if (!getRoleids().isEmpty()) {
                role = roleService.find(getRoleids().get(0));
                resourceAuthority.setRole(role);
            }
            if (getGroupids() != null && !getGroupids().isEmpty()) {
                group = groupService.find(getGroupids().get(0));
                resourceAuthority.setGroup(group);
            }
            if (getUserids() != null && !getUserids().isEmpty()) {
                user = userService.find(getUserids().get(0));
                resourceAuthority.setUser(user);
            }
            Map session = ActionContext.getContext().getSession();
            resourceAuthority.setAuthType(ResourceAuthority.AUTHTYPE_GRANT);
            resourceAuthority.setInherit(ResourceAuthority.INHERIT_N);
            if (resourceAuthority.getId() == null) {
                resourceAuthority = (ResourceAuthority) preAdd(resourceAuthority, session);
            } else {
                resourceAuthority = (ResourceAuthority) preUpdate(resourceAuthority, session);
            }
            resourceAuthorityService.applyAuthority(document, resourceAuthority);
            searchService.indexDocument(preference, document.getId());
        }
        return SUCCESS;
    }

    public String removeAuthority() throws Exception {
        if (deleteid != null) {
            resourceAuthority = resourceAuthorityService.find(deleteid);
            if (resourceAuthority != null && resourceAuthority.getId() != null) {
                resourceAuthorityService.removeAll(resourceAuthority);
            }
        }
        if (docid != null) {
            List<Preference> preferences = prefService.findAll();
            if (preferences != null && !preferences.isEmpty()) {
                preference = preferences.get(0);
            }
            searchService.removeDocument(docid);
            searchService.indexDocument(preference, docid);
            log.info("removeAuthority docid:" + docid);
        }
        return SUCCESS;
    }

    public String checkOut() throws Exception {
        document = (Document) documentService.find(getDocid());
        if (document != null && document.getId() != null) {
            Map session = ActionContext.getContext().getSession();
            document.setCheckOutStatus(Document.STATUS_CHECKOUT);
            document = (Document) preUpdate(document, session);
            setFid(document.getFolder().getId());
            setWid(document.getWorkspace().getId());
            log.info("checkOut set FID " + getFid());
            log.info("checkOut set WID " + getWid());
            documentService.save(document);
            addActionMessage(getText("document.checkout.success", new String[] { document.getName() }));
        } else {
            addActionError(getText("document.checkout.failed"));
        }
        return SUCCESS;
    }

    public String checkIn() {
        return SUCCESS;
    }

    public String uploadCheckIn() throws Exception {
        List<Preference> preferences = prefService.findAll();
        if (preferences != null && !preferences.isEmpty()) {
            preference = preferences.get(0);
        }
        uploadFile(getUpload());
        if (document != null && document.getFolder() != null && document.getWorkspace() != null) {
            setFid(document.getFolder().getId());
            setWid(document.getWorkspace().getId());
        }
        addActionMessage(getText("document.checkin.success", new String[] { document.getName() }));
        return SUCCESS;
    }

    private void uploadFile(File file) throws Exception {
        Map session = ActionContext.getContext().getSession();
        document = (Document) documentService.find(getDocid());
        document = extractMeta(file, document, session);
        log.info("Check in after extract meta " + document.getId());
        document = saveDocument(document);
        log.info("Check in after save doc " + document.getId());
        document = saveFile(document, file);
        log.info("Check in after save file" + document.getId());
    }

    private Document extractMeta(File file, Document document, Map session) throws Exception {
        log.info("extractMeta : " + getUploadFileName());
        document.setName(getUploadFileName());
        document.setContentType(getUploadContentType());
        document.setExt(getExtension(getUploadFileName()));
        document.setSize(getUpload().length());
        document = saveVersion(document, session);
        if (checkInKeepOut == null) {
            document.setCheckOutStatus(null);
        }
        document = (Document) preUpdate(document, session);
        return document;
    }

    private Document saveDocument(Document document) throws Exception {
        documentService.save(document);
        return document;
    }

    private Document saveVersion(Document document, Map session) throws Exception {
        DocVersion version = new DocVersionModel();
        version.setValues(document.getValues());
        version.setId(null);
        version.setDocument(document);
        DocVersion latestVersion = docVersionService.findLatestByDoc(document);
        if (latestVersion != null && latestVersion.getId() != null) {
            Double dVersion = latestVersion.getVersion();
            if (getCheckInMinor() != null) {
                dVersion += Constants.VERSION_MINOR;
            } else {
                dVersion += Constants.VERSION_1;
            }
            version.setVersion(dVersion);
        } else {
            version.setVersion(Constants.VERSION_1);
        }
        version = (DocVersion) preAdd(version, session);
        log.info("Check in version " + version.getId());
        docVersionService.save(version);
        document.setVersion(version.getVersion());
        return document;
    }

    private String getExtension(String fName) {
        return fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
    }

    private Document saveFile(Document document, File file) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATEFORMAT_YYYYMMDD);
        List<Preference> preferences = prefService.findAll();
        if (preferences != null && !preferences.isEmpty()) {
            Preference preference = preferences.get(0);
            String repo = preference.getRepository();
            StringBuffer sbRepo = new StringBuffer(repo);
            sbRepo.append(File.separator);
            StringBuffer sbFolder = new StringBuffer(document.getLocation());
            File folder = new File(sbRepo.append(sbFolder).toString());
            log.info("Check in file ID [" + document.getId() + "] to " + folder.getAbsolutePath());
            if (!folder.exists()) {
                folder.mkdirs();
            }
            FileChannel fcSource = null, fcDest = null, fcVersionDest = null;
            try {
                StringBuffer sbFile = new StringBuffer(folder.getAbsolutePath()).append(File.separator).append(document.getId()).append(".").append(document.getExt());
                StringBuffer sbVersionFile = new StringBuffer(folder.getAbsolutePath()).append(File.separator).append(document.getId()).append("_").append(document.getVersion().toString()).append(".").append(document.getExt());
                fcSource = new FileInputStream(file).getChannel();
                fcDest = new FileOutputStream(sbFile.toString()).getChannel();
                fcVersionDest = new FileOutputStream(sbVersionFile.toString()).getChannel();
                fcDest.transferFrom(fcSource, 0, fcSource.size());
                fcSource = new FileInputStream(file).getChannel();
                fcVersionDest.transferFrom(fcSource, 0, fcSource.size());
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
                    if (fcVersionDest != null) {
                        fcVersionDest.close();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return document;
    }

    public String listVersion() throws Exception {
        if (getDocid() != null) {
            Document doc = new DocumentModel();
            doc = (Document) documentService.find(getDocid());
            if (doc != null) {
                versions = docVersionService.findByDoc(doc);
            }
        } else {
            addActionError(getText("document.listversion.failed"));
        }
        return SUCCESS;
    }

    @SkipValidation
    public String remove() throws Exception {
        List<Long> removeIds = new ArrayList<Long>();
        if (getDocid() != null) {
            log.info("document remove docid:" + getDocid());
            removeIds.add(getDocid());
        } else if (getDocids() != null && !getDocids().isEmpty()) {
            removeIds.addAll(getDocids());
        }
        for (Long docid : removeIds) {
            document = (Document) documentService.find(docid);
            if (document != null && document.getId() != null) {
                log.info("document inactive:" + docid);
                documentService.inactive(document);
                if (document.getFolder() != null) {
                    setFid(document.getFolder().getId());
                }
            }
        }
        return SUCCESS;
    }

    @SkipValidation
    public String operate() {
        log.info("operate entry.");
        log.info("operation:" + getOperation());
        log.info("fid:" + getFid());
        String aMessage = "";
        try {
            List<Long> operateIds = new ArrayList<Long>();
            if (getDocid() != null) {
                operateIds.add(getDocid());
            } else if (getDocids() != null) {
                operateIds.addAll(getDocids());
            }
            Map session = ActionContext.getContext().getSession();
            if ("cut".equals(getOperation()) || "copy".equals(getOperation())) {
                session.put(Constants.CLIPBOARD_DOCIDS, operateIds);
                session.put(Constants.CLIPBOARD_OPERATION, operation);
                aMessage = getText("document." + getOperation() + ".success", new String[] { Integer.toString(operateIds.size()) });
                log.info(aMessage);
                addActionMessage(aMessage);
            } else if ("paste".equals(getOperation())) {
                String operation = (String) session.get(Constants.CLIPBOARD_OPERATION);
                operateIds = (List<Long>) session.get(Constants.CLIPBOARD_DOCIDS);
                boolean status = true;
                if (getFid() != null) {
                    Folder folder = (Folder) folderService.find(getFid());
                    for (Long operateId : operateIds) {
                        document = (Document) documentService.find(operateId);
                        if ("cut".equals(operation)) {
                            status &= documentService.cut(document, folder);
                        } else if ("copy".equals(operation)) {
                            status &= documentService.copy(document, folder);
                        }
                        log.info(getOperation() + " docid:" + operateId + " name:" + document.getName() + " to folder:" + folder.getName());
                    }
                    session.put(Constants.CLIPBOARD_OPERATION, "");
                    session.put(Constants.CLIPBOARD_DOCIDS, new ArrayList<Long>());
                } else {
                    addActionError(getText("document.paste.missing.fid"));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            addActionError(getText("generic.exception"));
            return INPUT;
        }
        return SUCCESS;
    }

    public String restore() throws Exception {
        log.info("restore docid:" + getDocid());
        document = documentService.find(getDocid());
        if (document != null) {
            if (document.getWorkspace() != null) {
                setWid(document.getWorkspace().getId());
            }
            if (document.getId() != null) {
                documentService.restore(document);
            }
        }
        return SUCCESS;
    }

    public String removeAll() throws Exception {
        log.info("removeAll docid:" + getDocid());
        document = (Document) documentService.find(getDocid());
        if (document != null) {
            if (document.getWorkspace() != null) {
                setWid(document.getWorkspace().getId());
            }
            if (document.getId() != null) {
                documentService.removeAll(document);
            }
        }
        return SUCCESS;
    }

    public String authorities() {
        if (getDocid() != null) {
            try {
                User user = null;
                Map session = ActionContext.getContext().getSession();
                if (session.get(Constants.SESSION_USER) != null) {
                    user = (User) session.get(Constants.SESSION_USER);
                }
                document = documentService.find(getDocid());
                setAuthorities(resourceAuthorityService.findByResource(document));
                setUserAuthorities(resourceAuthorityService.findByResourceUser(document, user));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return SUCCESS;
    }

    public String editProperties() {
        return SUCCESS;
    }

    public String saveProperties() {
        return SUCCESS;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Long getDocid() {
        return docid;
    }

    public void setDocid(Long docid) {
        this.docid = docid;
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

    public String getCheckInMinor() {
        return checkInMinor;
    }

    public void setCheckInMinor(String checkInMinor) {
        this.checkInMinor = checkInMinor;
    }

    public String getCheckInKeepOut() {
        return checkInKeepOut;
    }

    public void setCheckInKeepOut(String checkInKeepOut) {
        this.checkInKeepOut = checkInKeepOut;
    }

    public User getCrtBy() {
        return crtBy;
    }

    public void setCrtBy(User crtBy) {
        this.crtBy = crtBy;
    }

    public User getUpdBy() {
        return updBy;
    }

    public void setUpdBy(User updBy) {
        this.updBy = updBy;
    }

    public List<DocVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<DocVersion> versions) {
        this.versions = versions;
    }

    public Long getFid() {
        return fid;
    }

    public void setFid(Long fid) {
        this.fid = fid;
    }

    public Long getWid() {
        return wid;
    }

    public void setWid(Long wid) {
        this.wid = wid;
    }

    public String getChoice() {
        return choice;
    }

    public void setChoice(String choice) {
        this.choice = choice;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    public void setDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public DocVersionService getDocVersionService() {
        return docVersionService;
    }

    public void setDocVersionService(DocVersionService docVersionService) {
        this.docVersionService = docVersionService;
    }

    public FolderService getFolderService() {
        return folderService;
    }

    public void setFolderService(FolderService folderService) {
        this.folderService = folderService;
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

    public ResourceAuthorityService getResourceAuthorityService() {
        return resourceAuthorityService;
    }

    public void setResourceAuthorityService(ResourceAuthorityService resourceAuthorityService) {
        this.resourceAuthorityService = resourceAuthorityService;
    }

    public SearchService getSearchService() {
        return searchService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public List<ResourceAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(List<ResourceAuthority> authorities) {
        this.authorities = authorities;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    public RoleService getRoleService() {
        return roleService;
    }

    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<Long> getUserids() {
        return userids;
    }

    public void setUserids(List<Long> userids) {
        this.userids = userids;
    }

    public List<Long> getGroupids() {
        return groupids;
    }

    public void setGroupids(List<Long> groupids) {
        this.groupids = groupids;
    }

    public List<Long> getRoleids() {
        return roleids;
    }

    public void setRoleids(List<Long> roleids) {
        this.roleids = roleids;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Long getDeleteid() {
        return deleteid;
    }

    public void setDeleteid(Long deleteid) {
        this.deleteid = deleteid;
    }

    public List<ResourceAuthority> getUserAuthorities() {
        return userAuthorities;
    }

    public void setUserAuthorities(List<ResourceAuthority> userAuthorities) {
        this.userAuthorities = userAuthorities;
    }

    public List<Long> getDocids() {
        return docids;
    }

    public void setDocids(List<Long> docids) {
        this.docids = docids;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
