package cloudspace.controlpanel.filemanager;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.MimetypesFileTypeMap;
import javax.swing.event.ListDataEvent;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.zkoss.util.media.Media;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.SuspendNotAllowedException;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Button;
import org.zkoss.zul.Fileupload;
import org.zkoss.zul.Image;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;
import org.zkoss.zul.Window;
import org.zkoss.zul.impl.LabelElement;
import cloudspace.config.CloudSpaceConfiguration;
import cloudspace.config.ServletLocationFinder;
import cloudspace.security.CloudController;
import cloudspace.ui.Dialogs;
import cloudspace.ui.InlineEditListener;
import cloudspace.ui.InlineEditor;
import cloudspace.ui.impl.DatePopup;
import cloudspace.ui.zhtml.Applet;
import cloudspace.util.ClassFinder;
import cloudspace.util.ClassFinder.DirectoryResources;
import cloudspace.util.ZipUtil;
import cloudspace.vm.filesystem.CSPath;
import cloudspace.vm.filesystem.CSPathOutputStream;
import cloudspace.vm.filesystem.CSPathReader;
import cloudspace.vm.filesystem.CSPathWriter;
import cloudspace.vm.filesystem.ProjectSpec;

/**
 * A controller class that manages the file manager interface.
 * 
 * @author Mike Woods, Tony Allevato
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class FileManagerController extends GenericComposer {

    private static final long serialVersionUID = 1L;

    private Window fileManagerWindow;

    private Button newFileToolButton;

    private Button newFolderToolButton;

    private Button newProjectToolButton;

    private Button uploadButton;

    private Button deleteToolButton;

    private Button submitButton;

    private Button compileButton;

    private Menuitem newFileButton;

    private Menuitem newFolderButton;

    private Menuitem newProjectButton;

    private Menuitem renameButton;

    private Menuitem deleteButton;

    private Menuitem editButton;

    private Textbox currentDirBox;

    private Tree fileTree;

    private Listbox fileList;

    private FileManagerModel model;

    private CSPathDirectoryTreeModel treeModel;

    private InlineEditableListModelList fileListModel;

    private static final String HIDDEN_FILE_STYLE = "opacity: 0.5";

    private static final java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("hh:mm:ssaa MM/dd/yyyy");

    private static Logger log = Logger.getLogger(FileManagerController.class);

    private Applet editorApplet;

    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        fileManagerWindow = (Window) comp;
        model = new FileManagerModel();
        if (model.getCurrentDirectory() == null) {
            return;
        }
        String editorPath = Executions.getCurrent().getDesktop().getWebApp().getRealPath("/controlpanel/editor/EditorApplet.class");
        File editorFile = new File(editorPath);
        if (editorFile.exists()) {
            editorApplet = new Applet();
            editorApplet.setCodebase("editor");
            editorApplet.setCode("EditorApplet.class");
            fileManagerWindow.appendChild(editorApplet);
        }
        editButton = (Menuitem) fileManagerWindow.getFellow("editButton");
        newFileButton = (Menuitem) fileManagerWindow.getFellow("newFileButton");
        newFileToolButton = (Button) fileManagerWindow.getFellow("newFileToolButton");
        newFolderButton = (Menuitem) fileManagerWindow.getFellow("newFolderButton");
        newFolderToolButton = (Button) fileManagerWindow.getFellow("newFolderToolButton");
        newProjectButton = (Menuitem) fileManagerWindow.getFellow("newProjectButton");
        newProjectToolButton = (Button) fileManagerWindow.getFellow("newProjectToolButton");
        uploadButton = (Button) fileManagerWindow.getFellow("uploadButton");
        renameButton = (Menuitem) fileManagerWindow.getFellow("renameButton");
        deleteButton = (Menuitem) fileManagerWindow.getFellow("deleteButton");
        deleteToolButton = (Button) fileManagerWindow.getFellow("deleteToolButton");
        submitButton = (Button) fileManagerWindow.getFellow("submitButton");
        compileButton = (Button) fileManagerWindow.getFellow("compileButton");
        currentDirBox = (Textbox) fileManagerWindow.getFellow("curDirectoryDisplay");
        updateCurrentDirectoryField();
        redrawTree(fileManagerWindow);
        fileList = (Listbox) fileManagerWindow.getFellow("fileList");
        List<CSPath> allFiles = model.getCurrentDirectory().getChildren();
        if (allFiles == null) {
            allFiles = new ArrayList<CSPath>();
        }
        fileListModel = new InlineEditableListModelList(allFiles);
        fileList.setModel(fileListModel);
        Menupopup popup = (Menupopup) fileManagerWindow.getFellow("contextPopup");
        fileList.setContext(popup);
        fileList.setItemRenderer(new CSPathListitemRenderer());
        fileListModel.sort(new DirectoriesFirstCSPathComparator(), true);
        updateActionStates();
    }

    private void addFolderToTree(CSPath path) {
        Treeitem parentTreeitem = getTreeItem(model.getCurrentDirectory());
        addFolderToTree(parentTreeitem, path);
    }

    private void addFolderToTree(Treeitem destItem, CSPath targetFile) {
        treeModel.addFolderToTree(destItem, targetFile, false);
    }

    private Set<CSPath> getSelectedPaths() {
        Set<CSPath> files = new HashSet<CSPath>();
        Treeitem selectedTreeItem = fileTree.getSelectedItem();
        if (selectedTreeItem != null) {
            files.add((CSPath) selectedTreeItem.getValue());
        } else {
            for (Object item_ : fileList.getSelectedItems()) {
                Listitem item = (Listitem) item_;
                files.add((CSPath) item.getValue());
            }
        }
        return files;
    }

    private Treeitem getTreeItem(Object path) {
        int[] treePath = treeModel.getPath(model, path);
        if (treePath.length == 0) {
            treePath = new int[] { 0 };
        }
        return fileTree.renderItemByPath(treePath);
    }

    public void onNewFileClicked(Event event) {
        fileListModel.startEditing(new InlineEditListener() {

            @Override
            public void accept(String newName) {
                if (newName != null && newName.length() > 0) {
                    CSPath potentialFile = model.getPathForFile(newName);
                    if (!potentialFile.exists()) {
                        try {
                            model.createFile(newName);
                            fileListModel.endEditing();
                            updateFileGrid();
                        } catch (Exception e) {
                            displayError("The file " + newName + " could not be created for the following" + " reason: " + e.getMessage());
                        }
                    } else {
                        displayError("A file or folder with the name " + newName + "already exists");
                    }
                }
            }

            @Override
            public void decline() {
                fileListModel.endEditing();
            }
        });
    }

    public void onNewFolderClicked(Event event) {
        fileListModel.startEditing(new InlineEditListener() {

            @Override
            public void accept(String newName) {
                if (newName != null && newName.length() > 0) {
                    CSPath potentialFolder = model.getPathForFile(newName);
                    if (!potentialFolder.exists()) {
                        try {
                            potentialFolder.mkdir();
                            fileListModel.endEditing();
                            updateFileGrid();
                            addFolderToTree(potentialFolder);
                        } catch (Exception e) {
                            displayError("The folder " + newName + " could not be created for the following" + " reason: " + e.getMessage());
                        }
                    } else {
                        displayError("A file or folder with the name " + newName + "already exists");
                    }
                }
            }

            @Override
            public void decline() {
                fileListModel.endEditing();
            }
        });
    }

    public void onNewProjectClicked(Event event) {
        fileListModel.startEditing(new InlineEditListener() {

            @Override
            public void accept(String newName) {
                if (newName != null && newName.length() > 0) {
                    CSPath potentialFolder = model.getPathForFile(newName);
                    if (!potentialFolder.exists()) {
                        try {
                            potentialFolder.mkdir();
                            model.makeProject(potentialFolder);
                            fileListModel.endEditing();
                            updateFileGrid();
                            addFolderToTree(potentialFolder);
                        } catch (Exception e) {
                            displayError("The project folder " + newName + " could not be created for the following" + " reason: " + e.getMessage());
                        }
                    } else {
                        displayError("A file or folder with the name " + newName + "already exists");
                    }
                }
            }

            @Override
            public void decline() {
                fileListModel.endEditing();
            }
        });
    }

    public void onDeleteClicked(Event event) {
        int result = Messagebox.NO;
        try {
            result = Messagebox.show("Are you sure you want to delete the selected files? " + "This operation cannot be undone.", "Confirm Delete", Messagebox.YES | Messagebox.NO, Messagebox.QUESTION, Messagebox.NO);
        } catch (InterruptedException e) {
            log.error("An exception occurred when displaying the " + "confirmation dialog", e);
        }
        if (result == Messagebox.YES) {
            Set<CSPath> selectedPaths = getSelectedPaths();
            List<CSPath> paths = this.model.getRoots();
            for (CSPath path : selectedPaths) {
                boolean isDir = path.isDirectory();
                if (paths.contains(path)) {
                    displayError("You cannot delete a Root Directory!");
                    break;
                }
                boolean success = path.delete();
                if (!success) {
                    displayError("The file could not be deleted");
                    break;
                }
                if (isDir) {
                    treeModel.deleteFolderFromTree(getTreeItem(model.getCurrentDirectory()), path);
                }
            }
            updateFileGrid();
        }
    }

    public void onRenameClicked(Event event) {
        Set<CSPath> selectedPaths = getSelectedPaths();
        if (selectedPaths.isEmpty()) {
            return;
        }
        final CSPath selectedFile = selectedPaths.iterator().next();
        InlineEditListener listener = new InlineEditListener() {

            @Override
            public void accept(String newName) {
                CSPath potentialFile = new CSPath(selectedFile.getParent(), newName);
                if (!potentialFile.exists()) {
                    boolean success = selectedFile.moveTo(potentialFile);
                    if (success) {
                        renameFileInTree(selectedFile, potentialFile);
                        if (fileTree.getSelectedItem() != null) {
                            model.setCurrentDirectory(potentialFile);
                        }
                        updateFileGrid();
                    }
                } else if (!selectedFile.equals(potentialFile)) {
                    displayError("A file with the name " + newName + " already exists");
                }
            }
        };
        LabelElement editingElement = null;
        if (fileList.getSelectedItem() != null) {
            Listitem selectedItem = fileList.getSelectedItem();
            editingElement = (Listcell) selectedItem.getChildren().get(1);
        } else if (fileTree.getSelectedItem() != null) {
            Treeitem selectedItem = fileTree.getSelectedItem();
            editingElement = (Treecell) selectedItem.getTreerow().getChildren().get(0);
        }
        if (editingElement != null) {
            InlineEditor.attachToElement(editingElement, listener);
        }
    }

    public void onUploadClicked(Event event) {
        Media[] medias = null;
        try {
            medias = Fileupload.get("Select one or more files to upload to " + "the current directory.", "Upload Files", 5);
        } catch (Exception e) {
            log.error("An exception occurred when displaying the file " + "upload dialog", e);
        }
        if (medias == null) {
            return;
        }
        for (Media media : medias) {
            String name = media.getName();
            CSPath potentialFile = model.getPathForFile(name);
            if (media.isBinary()) {
                CSPathOutputStream writer = null;
                try {
                    potentialFile.createNewFile();
                    if (potentialFile.exists()) {
                        writer = new CSPathOutputStream(potentialFile);
                        IOUtils.copy(media.getStreamData(), writer);
                    }
                } catch (IOException e) {
                    displayError("An error occurred when uploading the file " + name + ": " + e.getMessage());
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                        }
                    }
                }
            } else {
                CSPathWriter writer = null;
                try {
                    potentialFile.createNewFile();
                    if (potentialFile.exists()) {
                        writer = new CSPathWriter(potentialFile);
                        IOUtils.write(media.getStringData(), writer);
                    }
                } catch (IOException e) {
                    displayError("An error occurred when uploading the file " + name + ": " + e.getMessage());
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            model.fileCleanup(potentialFile);
            updateFileGrid();
        }
    }

    public void onSubmitClicked(Event event) {
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("selectedPaths", getSelectedPaths());
        Window dialog = (Window) Executions.createComponents("/partials/controlpanel/submitFilesDialog.zul", null, args);
        try {
            dialog.doModal();
        } catch (Exception e) {
            log.error("An exception occurred when displaying the modal " + "upload users dialog", e);
            dialog.detach();
        }
        try {
            if ((Boolean) dialog.getAttribute("ok")) {
                model.snapshotCurrentDirectory();
                this.updateFileGrid();
                this.redrawTree();
            }
        } catch (IOException e) {
            displayError("Could not Snapshot submission");
        }
    }

    public void onContextOpen(Event event) {
        if (fileList.getSelectedCount() == 1) {
            CSPath path = (CSPath) fileList.getSelectedItem().getValue();
            editButton.setVisible(path.isFile());
        } else {
            editButton.setVisible(false);
        }
    }

    public void onDateRangeClicked(Event event) {
        DatePopup dialog = (DatePopup) Executions.createComponents("/partials/dialogs/datePopup.zul", null, null);
        try {
            dialog.doModal();
        } catch (SuspendNotAllowedException e) {
            log.error(e);
        } catch (InterruptedException e) {
            log.error(e);
        }
        this.updateFileGrid();
    }

    public void onCompileClicked(Event event) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        File currentDirectory = model.getCurrentDirectory().getPhysicalFile();
        DirectoryResources finder = ClassFinder.buildSource(currentDirectory);
        List<File> sourceFiles = finder.getSources();
        List<File> libraryFiles = finder.getLibraries();
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);
        File lib = new File(Executions.getCurrent().getDesktop().getWebApp().getRealPath("/WEB-INF/lib"));
        for (File classPFile : lib.listFiles()) libraryFiles.add(classPFile);
        try {
            fileManager.setLocation(StandardLocation.CLASS_PATH, libraryFiles);
            CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits);
            task.call();
        } catch (IOException e) {
            log.error("Could not set Class Path.", e);
        }
        this.updateFileGrid();
        this.fileManagerWindow.getPage().getFellow("compTab").setVisible(true);
        Window compileHelper = (Window) Executions.getCurrent().getDesktop().getAttribute("CompileHelper");
        Executions.getCurrent().getDesktop().setAttribute("compiledFiles", sourceFiles);
        Events.sendEvent("onUpdate", compileHelper, diagnostics);
    }

    public void onRedirect(Event e) {
        String location = this.model.getCurrentDirectory().getPath();
        String zone = this.model.getCurrentDirectory().getZone();
        Executions.getCurrent().sendRedirect("/" + zone + location);
    }

    public void onFolderTreeSelect(Event event) {
        Treeitem treeItem = fileTree.getSelectedItem();
        CSPath dir = (CSPath) treeItem.getValue();
        changeCurrentDirectory(dir);
        updateFileGrid();
        updateActionStates();
        updateCloudLink();
    }

    public void onFileListSelect(Event event) {
        fileTree.clearSelection();
        updateActionStates();
        updateCloudLink();
    }

    public void onPromoteClicked(Event event) throws InterruptedException {
        try {
            A selectedLink = (A) this.fileManagerWindow.getFellow("selectedLink");
            InetAddress addr = InetAddress.getLocalHost();
            String hostname = addr.getHostName();
            String selectedURI = selectedLink.getHref();
            String webappsPath = "";
            try {
                String servletPath = ServletLocationFinder.getServletResourcePath("/");
                if (!servletPath.contains("ROOT")) {
                    String[] splitVals = servletPath.split("/webapps/");
                    webappsPath = splitVals[splitVals.length - 1];
                    webappsPath = webappsPath.substring(0, webappsPath.length() - 1);
                    webappsPath = "/" + webappsPath;
                }
            } catch (Exception e) {
            }
            String programURI = "https://" + hostname + webappsPath + selectedURI;
            String tooltip = "";
            try {
                tooltip = extractTooltip(selectedURI);
            } catch (Exception e) {
            }
            Map<String, String> args = new HashMap<String, String>();
            args.put("programURI", programURI);
            args.put("tooltip", tooltip);
            Window dialog = (Window) Executions.createComponents("/partials/controlpanel/promotePageDialog.zul", null, args);
            try {
                dialog.doModal();
            } catch (Exception e) {
                log.error("An exception occurred when displaying the modal " + "promote page dialog", e);
                dialog.detach();
            }
        } catch (Exception e) {
            log.error("An error occured while promoting the demo page\n", e);
            displayError("Error: " + e.getMessage());
        }
    }

    /**
     * Extracts the required tooltip from the page referred to by selectedURI.
     * It does not send a HTTP request but uses the local file system on the server.
     * It checks for HTML <title> tag first, if not found, then it check for
     * title="" which may have been specified in zul file or other UI elements. 
     * @param selectedURI
     * @return
     * @throws IOException 
     */
    private String extractTooltip(String selectedURI) throws IOException {
        String tooltip = "";
        if (!selectedURI.contains(".zhtml") && !selectedURI.contains(".zul") && !selectedURI.contains(".html")) {
            selectedURI = selectedURI.concat("/index.zhtml");
        }
        File programFile = CloudSpaceConfiguration.getInstance().getFileInStorage(selectedURI);
        if (programFile != null && !programFile.exists()) {
            programFile = new File(ServletLocationFinder.getServletResourcePath("/") + selectedURI);
        }
        if (programFile.exists()) {
            FileInputStream fis = null;
            DataInputStream in = null;
            BufferedReader br = null;
            try {
                fis = new FileInputStream(programFile);
                in = new DataInputStream(fis);
                br = new BufferedReader(new InputStreamReader(in));
                int numRead = 0;
                char[] buf = new char[1024];
                while ((numRead = br.read(buf)) != -1) {
                    String readData = String.valueOf(buf, 0, numRead);
                    int startingPoint = readData.indexOf("<title>");
                    int endingPoint = readData.indexOf("</title>");
                    if (startingPoint != -1 && endingPoint != -1) {
                        tooltip = readData.substring((startingPoint + 7), endingPoint);
                        tooltip = tooltip.trim();
                        break;
                    }
                    startingPoint = readData.indexOf("title=\"");
                    if (startingPoint != -1) {
                        StringBuilder tooltipStr = new StringBuilder();
                        int i = startingPoint + 7;
                        char curChar;
                        while ((curChar = readData.charAt(i)) != '"') {
                            tooltipStr.append(curChar);
                            i += 1;
                        }
                        tooltip = tooltipStr.toString().trim();
                        break;
                    }
                    buf = new char[1024];
                }
            } catch (Exception e) {
                log.error("Not able to extract tooltip for link: " + selectedURI, e);
            } finally {
                if (in != null) {
                    in.close();
                }
                if (fis != null) {
                    fis.close();
                }
                if (br != null) {
                    br.close();
                }
            }
        }
        return tooltip;
    }

    public void updateCloudLink() {
        Set<CSPath> selectedPaths = getSelectedPaths();
        CSPath path = selectedPaths.iterator().next();
        A selectedLink = (A) this.fileManagerWindow.getFellow("selectedLink");
        if (path.getZone().equals("work")) {
            String prefix = ServletLocationFinder.getServletResourcePath("/");
            String location = CloudSpaceConfiguration.getInstance().getWorkLocation().getPath();
            location = location.substring(prefix.length());
            selectedLink.setHref("/" + location + path.getPath());
            selectedLink.setLabel("/" + location + path.getPath());
        } else {
            selectedLink.setHref("/" + path.getZone() + path.getPath());
            selectedLink.setLabel("/" + path.getZone() + path.getPath());
        }
        selectedLink.setTooltiptext("Open " + path.getName() + " in Browser");
    }

    private void updateActionStates() {
        Set<CSPath> selectedPaths = getSelectedPaths();
        boolean canWrite = CloudController.checkWriteNoThrow(model.getCurrentDirectory());
        newFileButton.setDisabled(!canWrite);
        newFileToolButton.setDisabled(!canWrite);
        newFolderButton.setDisabled(!canWrite);
        newFolderToolButton.setDisabled(!canWrite);
        newProjectButton.setDisabled(!canWrite);
        newProjectToolButton.setDisabled(!canWrite);
        uploadButton.setDisabled(!canWrite);
        compileButton.setDisabled(!canWrite);
        renameButton.setDisabled(!canWrite || selectedPaths.size() != 1);
        deleteButton.setDisabled(!canWrite || selectedPaths.size() == 0);
        deleteToolButton.setDisabled(!canWrite || selectedPaths.size() == 0);
        submitButton.setDisabled(selectedPaths.size() == 0);
    }

    public void onEditClicked(Event event) {
        CSPath path = (CSPath) fileList.getSelectedItem().getValue();
        openEditor(path);
    }

    public void onDownloadClicked(Event event) {
        CSPath path = (CSPath) fileList.getSelectedItem().getValue();
        if (path.isFile()) {
            promptForDownload(path.getPhysicalFile(), path.getName());
        } else {
            try {
                promptForDownload(ZipUtil.zipFile(path.getPhysicalFile()), path.getName() + ".zip");
            } catch (IOException e) {
                log.error("Could not zip file " + path.getName(), e);
            }
        }
    }

    private void promptForDownload(File targetFile, String promptName) {
        String contentType = new MimetypesFileTypeMap().getContentType(targetFile);
        try {
            Filedownload.save(new FileInputStream(targetFile), contentType, promptName);
        } catch (FileNotFoundException e) {
            log.error("Could not find the file to serve to client", e);
        }
    }

    public void openEditor(CSPath targetFile) {
        if (!FileTypes.isFileEditable(targetFile)) {
            promptForDownload(targetFile.getPhysicalFile(), targetFile.getName());
            return;
        }
        if (editorApplet != null && this.editorApplet.isInstalled()) {
            String toExecute = "getApplet().displayFile(\"" + targetFile.getZone() + targetFile.getPath() + "\");";
            Clients.evalJavaScript(toExecute);
            return;
        }
        String content = null;
        CSPathReader reader = null;
        try {
            reader = new CSPathReader(targetFile);
            content = IOUtils.toString(reader);
        } catch (IOException e) {
            displayError("There was an error reading the contents of " + targetFile.getName() + ": " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
        String newContent = Dialogs.textEditor("Edit " + targetFile.getName(), content, targetFile);
        if (newContent != null) {
            CSPathWriter writer = null;
            try {
                writer = new CSPathWriter(targetFile);
                IOUtils.write(newContent, writer);
            } catch (IOException e) {
                displayError("There was an error writing the contents of " + targetFile.getName() + ": " + e.getMessage());
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private void displayError(String errorMessage) {
        try {
            Messagebox.show(errorMessage, "Error", Messagebox.OK, Messagebox.ERROR);
        } catch (InterruptedException e) {
            log.error("An error occurred when displaying the error " + "dialog", e);
        }
    }

    public void removeFileFromTree(Treeitem parentItem, CSPath targetFile) {
        treeModel.deleteFolderFromTree(parentItem, targetFile);
    }

    public void renameFileInTree(CSPath oldFile, CSPath newFile) {
        treeModel.renameFolderInTree(this.getTreeItem(model), oldFile, newFile);
    }

    public void setCurrentDirectory(CSPath targetFile) {
        changeCurrentDirectory(targetFile);
        updateFileGrid();
        updateFileTree();
    }

    private void redrawTree(Window window) {
        fileTree = (Tree) window.getFellow("fileTree");
        Menupopup popup = (Menupopup) window.getFellow("contextPopup");
        DragDropCSPathTreeitemRenderer fileTreeitemRenderer = new DragDropCSPathTreeitemRenderer();
        fileTree.setTreeitemRenderer(fileTreeitemRenderer);
        treeModel = new CSPathDirectoryTreeModel(model);
        fileTree.setModel(treeModel);
        fileTree.renderItemByPath(new int[] { 0 });
        fileTree.setSelectedItem(this.getTreeItem(model.getCurrentDirectory()));
    }

    private void changeCurrentDirectory(CSPath dir) {
        model.setCurrentDirectory(dir);
        updateCurrentDirectoryField();
    }

    private void updateCurrentDirectoryField() {
        CSPath currentDir = model.getCurrentDirectory();
        currentDirBox.setValue(currentDir.getQualifiedPath());
    }

    public void updateFileGrid() {
        fileListModel.clear();
        List<CSPath> paths = model.getCurrentDirectory().getChildren();
        if (paths != null) {
            for (CSPath path : paths) {
                fileListModel.add(path);
            }
        }
        fileListModel.sort(new DirectoriesFirstCSPathComparator(), true);
        if (model.getCurrentDirectory().isProectDirectory()) submitButton.setVisible(true); else submitButton.setVisible(false);
    }

    public void updateFileTree() {
        CSPath dirFile = model.getCurrentDirectory();
        Treeitem newCurDirTreeItem = getTreeItem(dirFile);
        fileTree.setSelectedItem(newCurDirTreeItem);
    }

    public void moveTreeitem(Treeitem item, Treeitem destItem) {
        treeModel.moveTreeitem(item, destItem);
    }

    public void redrawTree() {
        redrawTree(fileManagerWindow);
    }

    private class CSPathListitemRenderer implements ListitemRenderer {

        private static final String LATE_STYLE_CLASS = "late";

        public void render(final Listitem listItem, Object data) {
            if (data instanceof InlineEditListener) {
                renderInlineEditItem(listItem, (InlineEditListener) data);
            } else {
                renderModelItem(listItem, (CSPath) data);
            }
        }

        private void renderInlineEditItem(final Listitem listItem, final InlineEditListener listener) {
            Listcell emptyCell = new Listcell();
            emptyCell.setParent(listItem);
            Listcell nameCell = new Listcell();
            nameCell.setParent(listItem);
            Listcell typeCell = new Listcell();
            typeCell.setParent(listItem);
            Listcell sizeCell = new Listcell();
            sizeCell.setParent(listItem);
            Listcell timeCell = new Listcell();
            timeCell.setParent(listItem);
            InlineEditor.attachToElement(nameCell, new InlineEditListener() {

                @Override
                public void accept(String text) {
                    listener.accept(text);
                }

                @Override
                public void decline() {
                    listener.decline();
                }

                @Override
                public void finish() {
                    listener.finish();
                }
            });
        }

        private void renderModelItem(Listitem listItem, CSPath file) {
            Long timestamp = (Long) listItem.getPage().getAttribute("filemanager-timestamp");
            listItem.setValue(file);
            Listcell imageCell = new Listcell();
            imageCell.setParent(listItem);
            Listcell nameCell = new Listcell();
            nameCell.setParent(listItem);
            Listcell typeCell = new Listcell();
            typeCell.setParent(listItem);
            Listcell sizeCell = new Listcell();
            sizeCell.setParent(listItem);
            Listcell timeCell = new Listcell();
            timeCell.setParent(listItem);
            nameCell.setLabel(file.getName());
            if (file.getPhysicalFile().length() > 1000) {
                sizeCell.setLabel("" + file.getPhysicalFile().length() / 1000 + " KB");
            } else {
                sizeCell.setLabel("" + file.getPhysicalFile().length() + " bytes");
            }
            long lastMod = file.getPhysicalFile().lastModified();
            timeCell.setLabel(formatter.format(new Date(lastMod)));
            if (timestamp != null && lastMod > timestamp) {
                timeCell.setClass(LATE_STYLE_CLASS);
                imageCell.setClass(LATE_STYLE_CLASS);
                nameCell.setClass(LATE_STYLE_CLASS);
                typeCell.setClass(LATE_STYLE_CLASS);
                sizeCell.setClass(LATE_STYLE_CLASS);
            }
            if (file.isDirectory()) {
                if (ProjectSpec.isProjectDirectory(file)) {
                    nameCell.setStyle("color: blue;");
                }
                listItem.setDraggable("Directory");
                listItem.setDroppable("Directory, File");
            } else {
                listItem.setDraggable("File");
            }
            String fileType = FileTypes.getFileType(file);
            String fileIcon = FileTypes.getFileIcon(file);
            typeCell.setLabel(fileType);
            Image image = new Image(fileIcon);
            image.setParent(imageCell);
            if (file.isHidden()) {
                image.setStyle(HIDDEN_FILE_STYLE);
                nameCell.setStyle(HIDDEN_FILE_STYLE);
                typeCell.setStyle(HIDDEN_FILE_STYLE);
                timeCell.setStyle(HIDDEN_FILE_STYLE);
                imageCell.setStyle(HIDDEN_FILE_STYLE);
                sizeCell.setStyle(HIDDEN_FILE_STYLE);
            }
            listItem.addEventListener("onDrop", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    CSPath destFile = (CSPath) ((Listitem) event.getTarget()).getValue();
                    log.trace("In File Grid onDrop");
                    DropEvent dropEvent = (DropEvent) event;
                    Listitem listItem = (Listitem) dropEvent.getDragged();
                    if (!listItem.isSelected()) {
                        listItem.setSelected(true);
                    }
                    Set<Listitem> selectedItems = (Set<Listitem>) listItem.getListbox().getSelectedItems();
                    for (Listitem li : selectedItems) {
                        CSPath targetFile = (CSPath) li.getValue();
                        log.trace("Moving " + targetFile.getName());
                        boolean success = targetFile.moveTo(destFile);
                        if (success) {
                            if (targetFile.isDirectory()) {
                                Treeitem item = getTreeItem(targetFile);
                                Treeitem destItem = getTreeItem(destFile);
                                moveTreeitem(item, destItem);
                            }
                        } else {
                            displayError("Cannot move the file to that directory.  A File with that name already exists there or you do not have the privleges to move that file.");
                        }
                    }
                    updateFileGrid();
                    redrawTree();
                }
            });
            listItem.addEventListener("onClick", new EventListener() {

                CSPath lastClicked = null;

                public void onEvent(Event event) throws Exception {
                    Listitem clickedItem = (Listitem) event.getTarget();
                    CSPath targetFile = (CSPath) clickedItem.getValue();
                    if (lastClicked != null && lastClicked.equals(targetFile)) {
                        if (targetFile.isDirectory()) {
                            setCurrentDirectory(targetFile);
                        } else if (targetFile.isFile()) {
                            openEditor(targetFile);
                        }
                        lastClicked = null;
                    } else {
                        lastClicked = targetFile;
                    }
                }
            });
        }
    }

    /**
     * An extension of ListModelList that supports a "dummy" item at the end of
     * the list that can be used to add new items with inline editing support.
     * This is somewhat of a hack -- this code doesn't really belong in the
     * model (and some support is required in the renderer above as well) but
     * it's a quick fix that doesn't involve messing around with other listbox
     * internals.
     */
    private class InlineEditableListModelList extends ListModelList {

        public InlineEditableListModelList(List<?> list) {
            super(list);
        }

        public void startEditing(InlineEditListener listener) {
            if (!isEditing) {
                isEditing = true;
                editListener = listener;
                fireEvent(ListDataEvent.INTERVAL_ADDED, getInnerList().size(), getInnerList().size());
            }
        }

        public void endEditing() {
            if (isEditing) {
                isEditing = false;
                editListener = null;
                fireEvent(ListDataEvent.INTERVAL_REMOVED, getInnerList().size(), getInnerList().size());
            }
        }

        public int getSize() {
            return getInnerList().size() + (isEditing ? 1 : 0);
        }

        public Object getElementAt(int index) {
            if (index < getInnerList().size()) {
                return getInnerList().get(index);
            } else {
                return editListener;
            }
        }

        private boolean isEditing;

        private InlineEditListener editListener;

        private static final long serialVersionUID = -2657814078877741658L;
    }

    private class DragDropCSPathTreeitemRenderer extends CSPathTreeitemRenderer {

        public void render(Treeitem item, Object data) {
            super.render(item, data);
            Treerow tr = item.getTreerow();
            tr.setDroppable("Directory, File");
            tr.addEventListener("onDrop", new EventListener() {

                public void onEvent(Event event) throws Exception {
                    CSPath destFile = (CSPath) ((Treeitem) ((Treerow) event.getTarget()).getParent()).getValue();
                    DropEvent dropEvent = (DropEvent) event;
                    Listitem listItem = (Listitem) dropEvent.getDragged();
                    if (!listItem.isSelected()) {
                        listItem.setSelected(true);
                    }
                    Set<Listitem> selectedItems = (Set<Listitem>) listItem.getListbox().getSelectedItems();
                    for (Listitem li : selectedItems) {
                        CSPath targetFile = (CSPath) li.getValue();
                        log.trace("Moving " + targetFile.getName());
                        boolean success = targetFile.moveTo(destFile);
                        if (!success) {
                            displayError("Cannot move the file to that directory.  A File with that name already exists there or you do not have the privleges to do so.");
                        }
                    }
                    redrawTree();
                    updateFileGrid();
                }
            });
        }
    }
}
