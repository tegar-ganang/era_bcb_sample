package org.contineo.web.document;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.contineo.core.security.ExtMenu;
import org.contineo.core.security.Menu;
import org.contineo.core.security.MenuManager;
import org.contineo.core.security.UserDoc;
import org.contineo.core.security.dao.MenuDAO;
import org.contineo.core.security.dao.UserDocDAO;
import org.contineo.core.transfer.ZipImport;
import org.contineo.util.Context;
import org.contineo.util.config.SettingsConfig;
import org.contineo.web.SessionManagement;
import org.contineo.web.i18n.Messages;
import org.contineo.web.navigation.PageContentBean;
import org.contineo.web.upload.InputFileBean;
import com.icesoft.faces.async.render.RenderManager;
import com.icesoft.faces.async.render.Renderable;
import com.icesoft.faces.component.ext.RowSelectorEvent;
import com.icesoft.faces.webapp.xmlhttp.PersistentFacesState;
import com.icesoft.faces.webapp.xmlhttp.RenderingException;

/**
 * <p>
 * The <code>DocumentsRecordsManager</code> class is responsible for
 * constructing the list of <code>DocumentRecord</code> beans which will be
 * bound to a ice:dataTable JSF component. <p/>
 * <p>
 * Large data sets could be handle by adding a ice:dataPaginator. Alternatively
 * the dataTable could also be hidden and the dataTable could be added to
 * scrollable ice:panelGroup.
 * </p>
 * 
 * @author Marco Meschieri
 * @version $Id: DocumentsRecordsManager.java,v 1.1 2007/06/29 06:28:29 marco
 *          Exp $
 * @since 3.0
 */
public class DocumentsRecordsManager implements Renderable {

    protected static Log log = LogFactory.getLog(DocumentsRecordsManager.class);

    public static final String GROUP_INDENT_STYLE_CLASS = "groupRowIndentStyle";

    public static final String GROUP_ROW_STYLE_CLASS = "groupRowStyle";

    public static final String CHILD_INDENT_STYLE_CLASS = "childRowIndentStyle";

    public static final String CHILD_ROW_STYLE_CLASS = "childRowStyle";

    public static final String CONTRACT_IMAGE = "contract.png";

    public static final String EXPAND_IMAGE = "expand.png";

    private ArrayList<DocumentRecord> documents;

    private boolean multipleSelection = true;

    private int selectedDirectory;

    private int sourceDirectory;

    private PersistentFacesState state;

    private RenderManager renderManager;

    private Set<DocumentRecord> selection = new HashSet<DocumentRecord>();

    private Set<DocumentRecord> clipboard = new HashSet<DocumentRecord>();

    private Comparator drsc = new DocumentRecordSelectedComparator();

    public DocumentsRecordsManager() {
        selectDirectory(Menu.MENUID_DOCUMENTS);
        selection.clear();
        clipboard.clear();
        state = PersistentFacesState.getInstance();
    }

    /**
	 * Changes the currently selected directory and updates the documents list.
	 * 
	 * @param directoryId
	 */
    public void selectDirectory(int directoryId) {
        selectedDirectory = directoryId;
        selection.clear();
        if (documents != null) {
            documents.clear();
        } else {
            documents = new ArrayList<DocumentRecord>(10);
        }
        String username = SessionManagement.getUsername();
        MenuDAO menuDao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
        Collection<ExtMenu> menus = menuDao.getContainedMenus(directoryId, username);
        for (ExtMenu menu : menus) {
            if (menu.getMenuType() == Menu.MENUTYPE_FILE) {
                DocumentRecord record;
                Collection<ExtMenu> children = menuDao.getContainedMenus(menu.getMenuId(), username);
                if (children.size() > 0) {
                    record = new DocumentRecord(menu, GROUP_INDENT_STYLE_CLASS, GROUP_ROW_STYLE_CLASS, EXPAND_IMAGE, CONTRACT_IMAGE, documents, false);
                } else {
                    record = new DocumentRecord(menu, CHILD_INDENT_STYLE_CLASS, CHILD_ROW_STYLE_CLASS);
                }
                if (!documents.contains(record)) {
                    documents.add(record);
                }
                for (ExtMenu childMenu : children) {
                    DocumentRecord child = new DocumentRecord(childMenu, CHILD_INDENT_STYLE_CLASS, CHILD_ROW_STYLE_CLASS);
                    record.addChildRecord(child);
                }
            }
        }
    }

    /**
	 * Cleans up the resources used by this class. This method could be called
	 * when a session destroyed event is called.
	 */
    public void dispose() {
        documents.clear();
    }

    /**
	 * Gets the list of DocumentRecord which will be used by the ice:dataTable
	 * component.
	 * 
	 * @return array list of parent DocumentRecord
	 */
    public ArrayList getDocuments() {
        return documents;
    }

    public boolean isMultipleSelection() {
        return multipleSelection;
    }

    public void setMultipleSelection(boolean multiple) {
        this.multipleSelection = multiple;
    }

    public void selectRow(RowSelectorEvent e) {
        DocumentRecord record = documents.get(e.getRow());
        if (e.isSelected() || !selection.contains(record)) {
            selection.add(record);
        } else if (!e.isSelected() || selection.contains(record)) {
            selection.remove(record);
        }
    }

    public void refresh() {
        selectDirectory(selectedDirectory);
    }

    public int getClipboardSize() {
        return clipboard.size();
    }

    public int getCount() {
        if (documents == null) {
            return 0;
        } else {
            return documents.size();
        }
    }

    /**
	 * Deletes all selected documents
	 */
    public String deleteSelected() {
        if (SessionManagement.isValid()) {
            if (!selection.isEmpty()) {
                MenuManager manager = (MenuManager) Context.getInstance().getBean(MenuManager.class);
                String username = SessionManagement.getUsername();
                for (DocumentRecord record : selection) {
                    try {
                        manager.deleteMenu(record.getMenu(), username);
                        Messages.addLocalizedInfo("msg.action.deleteitem");
                    } catch (AccessControlException e) {
                        Messages.addLocalizedError("document.write.nopermission");
                    } catch (Exception e) {
                        Messages.addLocalizedInfo("errors.action.deleteitem");
                    }
                }
                refresh();
            } else {
                Messages.addLocalizedWarn("noselection");
            }
            return null;
        } else {
            return "login";
        }
    }

    /**
	 * Trims all selected documents
	 */
    public String trimSelected() {
        if (SessionManagement.isValid()) {
            if (!selection.isEmpty()) {
                sourceDirectory = selectedDirectory;
                clipboard.clear();
                for (DocumentRecord record : selection) {
                    clipboard.add(record);
                }
                refresh();
            } else {
                Messages.addLocalizedWarn("noselection");
            }
            return null;
        } else {
            return "login";
        }
    }

    /**
	 * Paste previously trimmed documents into the current directory
	 */
    public String paste() {
        if (SessionManagement.isValid()) {
            if (!clipboard.isEmpty()) {
                String username = SessionManagement.getUsername();
                MenuDAO menuDao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
                if (menuDao.isWriteEnable(selectedDirectory, username)) {
                    try {
                        DocumentNavigation navigation = ((DocumentNavigation) FacesContext.getCurrentInstance().getApplication().createValueBinding("#{documentNavigation}").getValue(FacesContext.getCurrentInstance()));
                        for (DocumentRecord record : clipboard) {
                            Menu menu = menuDao.findByPrimaryKey(record.getMenuId());
                            if (!menuDao.isWriteEnable(menu.getMenuId(), username)) {
                                throw new AccessControlException("");
                            }
                            Menu dir = menuDao.findByPrimaryKey(menu.getMenuParent());
                            SettingsConfig settings = (SettingsConfig) Context.getInstance().getBean(SettingsConfig.class);
                            String path = settings.getValue("docdir") + "/" + menu.getMenuPath() + "/" + menu.getMenuId();
                            File originalDocDir = new File(path);
                            menu.setMenuParent(selectedDirectory);
                            dir = menuDao.findByPrimaryKey(selectedDirectory);
                            menu.setMenuPath(dir.getMenuPath() + "/" + dir.getMenuId());
                            menuDao.store(menu);
                            path = settings.getValue("docdir") + "/" + menu.getMenuPath() + "/" + menu.getMenuId();
                            File newDocDir = new File(path);
                            FileUtils.copyDirectory(originalDocDir, newDocDir);
                            FileUtils.forceDelete(originalDocDir);
                            Directory destination = navigation.getDirectory(selectedDirectory);
                            destination.setCount(destination.getCount() + 1);
                            Directory source = navigation.getDirectory(sourceDirectory);
                            source.setCount(source.getCount() - 1);
                        }
                    } catch (AccessControlException e) {
                        Messages.addLocalizedWarn("document.write.nopermission");
                    } catch (Exception e) {
                        Messages.addLocalizedInfo("errors.action.movedocument");
                    }
                    clipboard.clear();
                } else {
                    Messages.addLocalizedWarn("document.write.nopermission");
                }
                refresh();
            }
            return null;
        } else {
            return "login";
        }
    }

    /**
	 * Shows the zip upload form
	 */
    public String startZipUpload() {
        log.debug("startZipUpload");
        Application application = FacesContext.getCurrentInstance().getApplication();
        ValueBinding vb = application.createValueBinding("#{documentNavigation}");
        DocumentNavigation documentNavigation = (DocumentNavigation) vb.getValue(FacesContext.getCurrentInstance());
        if (SessionManagement.isValid()) {
            log.debug("session is valid");
            try {
                try {
                    ValueBinding vb2 = application.createValueBinding("#{inputFile}");
                    InputFileBean inputFile = (InputFileBean) vb2.getValue(FacesContext.getCurrentInstance());
                    inputFile.reset();
                } catch (RuntimeException e) {
                    log.info("catched Exception deleting old upload file: " + e.getMessage(), e);
                }
                MenuDAO mdao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
                String username = SessionManagement.getUsername();
                Directory dir = documentNavigation.getSelectedDir();
                int parentId = dir.getMenuId();
                if (mdao.isWriteEnable(parentId, username)) {
                    log.debug("mdao.isWriteEnabled");
                    documentNavigation.setSelectedPanel(new PageContentBean("zipImport"));
                } else {
                    log.debug("no permission to upload");
                    Messages.addLocalizedError("document.write.nopermission");
                }
                log.debug("show the upload zip panel");
                documentNavigation.setSelectedPanel(new PageContentBean("zipUpload"));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Messages.addError(e.getMessage());
            }
        } else {
            return "login";
        }
        return null;
    }

    /**
	 * Imports the zip content in the current directory
	 */
    public String uploadZip() {
        if (SessionManagement.isValid()) {
            try {
                final String username = SessionManagement.getUsername();
                final Application application = FacesContext.getCurrentInstance().getApplication();
                final ValueBinding vb = application.createValueBinding("#{inputFile}");
                final InputFileBean inputFile = (InputFileBean) vb.getValue(FacesContext.getCurrentInstance());
                File file = inputFile.getFile();
                log.debug("file = " + file);
                final String zipLanguage = inputFile.getLanguage();
                MenuDAO menuDao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
                final Menu parent = menuDao.findByPrimaryKey(selectedDirectory);
                log.debug("parent.getMenuId() = " + parent.getMenuId());
                log.debug("parent.getMenuPath() = " + parent.getMenuPath());
                SettingsConfig conf = (SettingsConfig) Context.getInstance().getBean(SettingsConfig.class);
                String path = conf.getValue("userdir");
                if (!path.endsWith(File.pathSeparator)) {
                    path += File.pathSeparator;
                }
                path += username + File.pathSeparator + File.separator;
                log.debug("path = " + path);
                FileUtils.forceMkdir(new File(path));
                final File destFile = new File(path, file.getName());
                log.debug("destFile = " + destFile);
                FileUtils.copyFile(file, destFile);
                inputFile.deleteUploadDir();
                Thread zipImporter = new Thread(new Runnable() {

                    public void run() {
                        ZipImport importer = new ZipImport();
                        importer.setExtractKeywords(inputFile.isExtractKeywords());
                        log.debug("importing: = " + destFile.getPath());
                        importer.process(destFile.getPath(), zipLanguage, parent, username);
                        try {
                            FileUtils.forceDelete(destFile);
                        } catch (IOException e) {
                            log.error("Unable to delete " + destFile, e);
                        }
                    }
                });
                zipImporter.start();
                Messages.addLocalizedInfo("msg.action.importfolder");
                ValueBinding vb2 = application.createValueBinding("#{documentNavigation}");
                DocumentNavigation documentNavigation = (DocumentNavigation) vb2.getValue(FacesContext.getCurrentInstance());
                documentNavigation.refresh();
                documentNavigation.setSelectedPanel(new PageContentBean("documents"));
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                Messages.addError(e.getMessage());
            }
        } else {
            return "login";
        }
        return null;
    }

    /**
	 * Retrieves the list of last accessed documents from the database
	 */
    public List<DocumentRecord> getLastDocs() {
        List<DocumentRecord> lastdocs = new ArrayList<DocumentRecord>();
        if (SessionManagement.isValid()) {
            try {
                String username = SessionManagement.getUsername();
                UserDocDAO uddao = (UserDocDAO) Context.getInstance().getBean(UserDocDAO.class);
                Collection userdocs = uddao.findByUserName(username);
                Iterator iter = userdocs.iterator();
                MenuDAO mdao = (MenuDAO) Context.getInstance().getBean(MenuDAO.class);
                while (iter.hasNext()) {
                    UserDoc userdoc = (UserDoc) iter.next();
                    Menu m = mdao.findByPrimaryKey(userdoc.getMenuId());
                    ExtMenu menu = new ExtMenu(m);
                    lastdocs.add(new DocumentRecord(menu, GROUP_INDENT_STYLE_CLASS, GROUP_ROW_STYLE_CLASS));
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return lastdocs;
    }

    public PersistentFacesState getState() {
        return state;
    }

    public void renderingException(RenderingException renderingException) {
        renderingException.printStackTrace();
    }

    public RenderManager getRenderManager() {
        return renderManager;
    }

    public void setRenderManager(RenderManager renderManager) {
        this.renderManager = renderManager;
    }

    protected void selectHighlightedDocument(int menuId) {
        DocumentRecord myFatherDocument = null;
        for (DocumentRecord document : documents) {
            if (document.menu.getMenuId() == menuId) {
                document.selected = true;
                selection.add(document);
                break;
            }
            for (DocumentRecord childDocument : document.getChildRecords()) {
                if (childDocument.menu.getMenuId() == menuId) {
                    myFatherDocument = document;
                    document.selected = true;
                    childDocument.selected = true;
                    selection.add(childDocument);
                    break;
                }
            }
        }
        sortDocumentsBySelected();
        if (myFatherDocument != null) {
            myFatherDocument.setSelected(false);
            myFatherDocument.setExpanded(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void sortDocumentsBySelected() {
        Collections.sort(documents, drsc);
        Collections.reverse(documents);
    }

    class DocumentRecordSelectedComparator implements Comparator {

        public int compare(Object arg0, Object arg1) {
            DocumentRecord dr0 = (DocumentRecord) arg0;
            DocumentRecord dr1 = (DocumentRecord) arg1;
            return new Boolean(dr0.isSelected()).compareTo(new Boolean(dr1.isSelected()));
        }
    }

    ;
}
