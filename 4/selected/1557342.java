package dms.core.document.actions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import dms.core.actions.Action;
import dms.core.actions.ActionBase;
import dms.core.document.folder.logic.Folder;
import dms.core.document.folder.logic.FolderModel;
import dms.core.document.folder.logic.FolderService;
import dms.core.document.logic.Document;
import dms.core.document.logic.DocumentModel;
import dms.core.document.logic.DocumentService;
import dms.core.document.workspace.logic.Workspace;
import dms.core.document.workspace.logic.WorkspaceModel;
import dms.core.document.workspace.logic.WorkspaceService;
import dms.core.user.logic.User;
import dms.core.user.logic.UserModel;
import dms.core.user.logic.UserService;
import dms.portal.preference.logic.Preference;
import dms.portal.preference.logic.PreferenceModel;
import dms.portal.preference.logic.PreferenceService;
import dms.util.AppConstants;
import dms.util.SpringUtil;

public class BatchUploadAction extends ActionBase implements Action {

    private ApplicationContext sctx;

    private PreferenceService prefService;

    private UserService userService;

    private DocumentService docService;

    private FolderService folderService;

    private WorkspaceService workspaceService;

    private File upload;

    private String uploadContentType;

    private String uploadFileName;

    private String fileCaption;

    private Preference preference;

    private SimpleDateFormat sdf;

    private static Logger log = Logger.getLogger(BatchUploadAction.class);

    public static final Long DEFAULT_FOLDER = 1L;

    public static final Long DEFAULT_WORKSPACE = 1L;

    public static final Long DEFAULT_USER = 1L;

    private Folder folder;

    private Workspace workspace;

    private User user;

    public BatchUploadAction() {
        sctx = SpringUtil.getInstance().getContext();
        prefService = (PreferenceService) sctx.getBean(PreferenceService.class.getSimpleName());
        userService = (UserService) sctx.getBean(UserService.class.getSimpleName());
        docService = (DocumentService) sctx.getBean(DocumentService.class.getSimpleName());
        folderService = (FolderService) sctx.getBean(FolderService.class.getSimpleName());
        workspaceService = (WorkspaceService) sctx.getBean(WorkspaceService.class.getSimpleName());
        List<Preference> preferences = prefService.findAll();
        if (preferences != null && !preferences.isEmpty()) {
            preference = preferences.get(0);
        }
        sdf = new SimpleDateFormat(AppConstants.DATEFORMAT_YYYYMMDD);
        folder = new FolderModel();
        workspace = new WorkspaceModel();
        user = new UserModel();
    }

    public String execute() throws Exception {
        String szRepo = "", szTempRepo = "";
        if (preference.getTemprepository() != null && preference.getTemprepository().length() > 0) {
            szTempRepo = preference.getTemprepository() + File.separator + new Long(System.currentTimeMillis()).toString();
            szRepo = preference.getRepository() + File.separator;
        } else {
            szTempRepo = AppConstants.TMP_REPOSITORY + File.separator + new Long(System.currentTimeMillis()).toString();
            szRepo = preference.getRepository() + File.separator;
        }
        List<String> fNames = new ArrayList<String>();
        File fTempFolder = new File(szTempRepo);
        fTempFolder.mkdirs();
        File fRepoFolder = new File(szRepo);
        fRepoFolder.mkdirs();
        fNames = unzipUploadFile(getUpload(), szTempRepo);
        if (!fNames.isEmpty()) {
            if (fNames.indexOf(AppConstants.DMS_XML) != -1) {
                saveDocumentXml(szRepo, szTempRepo);
            }
        } else {
            System.out.println("No file found in zip file");
        }
        return SUCCESS;
    }

    private boolean saveDocumentXml(String repository, String tempRepo) {
        boolean result = true;
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            String expression = "documents/document";
            InputSource insource = new InputSource(new FileInputStream(tempRepo + File.separator + AppConstants.DMS_XML));
            NodeList nodeList = (NodeList) xpath.evaluate(expression, insource, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                System.out.println(node.getNodeName());
                DocumentModel document = new DocumentModel();
                NodeList childs = node.getChildNodes();
                for (int j = 0; j < childs.getLength(); j++) {
                    Node child = childs.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        if (child.getNodeName() != null && child.getFirstChild() != null && child.getFirstChild().getNodeValue() != null) {
                            System.out.println(child.getNodeName() + "::" + child.getFirstChild().getNodeValue());
                        }
                        if (Document.FLD_ID.equals(child.getNodeName())) {
                            if (child.getFirstChild() != null) {
                                String szId = child.getFirstChild().getNodeValue();
                                if (szId != null && szId.length() > 0) {
                                    try {
                                        document.setId(new Long(szId));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else if (document.FLD_NAME.equals(child.getNodeName())) {
                            document.setName(child.getFirstChild().getNodeValue());
                            document.setTitle(document.getName());
                            document.setDescr(document.getName());
                            document.setExt(getExtension(document.getName()));
                        } else if (document.FLD_LOCATION.equals(child.getNodeName())) {
                            document.setLocation(child.getFirstChild().getNodeValue());
                        } else if (document.FLD_OWNER.equals(child.getNodeName())) {
                            Long id = new Long(child.getFirstChild().getNodeValue());
                            User user = new UserModel();
                            user.setId(id);
                            user = (User) userService.find(user);
                            if (user != null && user.getId() != null) {
                                document.setOwner(user);
                            }
                        }
                    }
                }
                boolean isSave = docService.save(document);
                if (isSave) {
                    String repo = preference.getRepository();
                    Calendar calendar = Calendar.getInstance();
                    StringBuffer sbRepo = new StringBuffer(repo);
                    sbRepo.append(File.separator);
                    StringBuffer sbFolder = new StringBuffer(sdf.format(calendar.getTime()));
                    sbFolder.append(File.separator).append(calendar.get(Calendar.HOUR_OF_DAY));
                    File fileFolder = new File(sbRepo.append(sbFolder).toString());
                    if (!fileFolder.exists()) {
                        fileFolder.mkdirs();
                    }
                    FileChannel fcSource = null, fcDest = null;
                    try {
                        StringBuffer sbFile = new StringBuffer(fileFolder.getAbsolutePath());
                        StringBuffer fname = new StringBuffer(document.getId().toString());
                        fname.append(".").append(document.getExt());
                        sbFile.append(File.separator).append(fname);
                        fcSource = new FileInputStream(tempRepo + File.separator + document.getName()).getChannel();
                        fcDest = new FileOutputStream(sbFile.toString()).getChannel();
                        fcDest.transferFrom(fcSource, 0, fcSource.size());
                        document.setLocation(sbFolder.toString());
                        document.setSize(fcSource.size());
                        log.info("Batch upload file " + document.getName() + " into [" + document.getLocation() + "] as " + document.getName() + "." + document.getExt());
                        folder.setId(DEFAULT_FOLDER);
                        folder = (Folder) folderService.find(folder);
                        if (folder != null && folder.getId() != null) {
                            document.setFolder(folder);
                        }
                        workspace.setId(DEFAULT_WORKSPACE);
                        workspace = (Workspace) workspaceService.find(workspace);
                        if (workspace != null && workspace.getId() != null) {
                            document.setWorkspace(workspace);
                        }
                        user.setId(DEFAULT_USER);
                        user = (User) userService.find(user);
                        if (user != null && user.getId() != null) {
                            document.setCrtby(user.getId());
                        }
                        document.setCrtdate(new Date());
                        document = (DocumentModel) docService.resetDuplicateDocName(document);
                        docService.save(document);
                        DocumentIndexer.indexDocument(preference, document);
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
                }
            }
        } catch (Exception e) {
            result = false;
            e.printStackTrace();
        }
        return result;
    }

    private String getExtension(String fName) {
        return fName.substring(fName.indexOf(".") + 1, fName.length());
    }

    private List<String> unzipUploadFile(File file, String folder) {
        List<String> fNames = new ArrayList<String>();
        try {
            ZipInputStream zins = new ZipInputStream(new FileInputStream(getUpload()));
            FileOutputStream outs = null;
            ZipEntry entry = null;
            while ((entry = zins.getNextEntry()) != null) {
                File outf = new File(folder + File.separator + entry.getName());
                outs = new FileOutputStream(outf);
                fNames.add(entry.getName());
                System.out.println(entry.getName());
                byte[] buf = new byte[1024];
                int len;
                while ((len = zins.read(buf)) > 0) {
                    outs.write(buf, 0, len);
                }
            }
            if (outs != null) {
                outs.close();
            }
            if (zins != null) {
                zins.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fNames;
    }

    public File getUpload() {
        return upload;
    }

    public void setUpload(File upload) {
        if (upload != null) {
            this.upload = upload;
        }
    }

    public String getUploadContentType() {
        return uploadContentType;
    }

    public void setUploadContentType(String uploadContentType) {
        if (uploadContentType != null) {
            this.uploadContentType = uploadContentType;
        }
    }

    public String getUploadFileName() {
        return uploadFileName;
    }

    public void setUploadFileName(String uploadFileName) {
        if (uploadFileName != null) {
            this.uploadFileName = uploadFileName;
        }
    }

    public String getFileCaption() {
        return fileCaption;
    }

    public void setFileCaption(String fileCaption) {
        if (fileCaption != null) {
            this.fileCaption = fileCaption;
        }
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }
}
