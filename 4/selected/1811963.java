package uk.ac.reload.straker.ui.viewers;

import java.io.File;
import java.io.IOException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.DrillDownAdapter;
import uk.ac.reload.diva.util.FileUtils;
import uk.ac.reload.straker.IIcons;
import uk.ac.reload.straker.StrakerPlugin;
import uk.ac.reload.straker.actions.StrakerActionFactory;
import uk.ac.reload.straker.ui.dialogs.NewFileDialog;
import uk.ac.reload.straker.ui.dialogs.NewFolderDialog;
import uk.ac.reload.straker.ui.dialogs.RenameFileDialog;
import uk.ac.reload.straker.util.StrakerFileUtils;

/**
 * File Tree Viewer
 * 
 * @author Phillip Beauvoir
 * @version $Id: FileTreeViewer.java,v 1.11 2006/07/10 11:50:55 phillipus Exp $
 */
public class FileTreeViewer extends TreeViewer implements IIcons {

    /**
	 * The Root Folder
	 */
    private File _rootFolder;

    /**
	 * Drill-down into tree
	 */
    private DrillDownAdapter _drillDownAdapter;

    /**
	 * Common Actions
	 */
    protected Action _actionRefresh, _actionDelete, _actionRename, _actionNewFile, _actionNewFolder;

    protected Action _actionCut, _actionCopy, _actionPaste, _actionImport;

    protected Action _actionEdit, _actionView;

    /**
	 * Refresh timer
	 */
    private Runnable _timer;

    /**
	 * Refresh timer interval of 5 seconds
	 */
    static int TIMERDELAY = 5000;

    /**
	 * Flag to denote whether the drag source is this tree
	 */
    private boolean ISDRAGSOURCE = false;

    /**
	 * Constructor
	 */
    public FileTreeViewer() {
        super(null, 1);
    }

    /**
	 * Set things up. Implementors can over-ride
	 */
    protected void setup() {
        _drillDownAdapter = new DrillDownAdapter(this);
        makeActions();
        hookContextMenu();
        registerGlobalActions();
        registerDragDrop();
        setupRefreshTimer();
        setSorter(new ViewerSorter() {

            public int category(Object element) {
                if (element instanceof File) {
                    File f = (File) element;
                    return f.isDirectory() ? 0 : 1;
                }
                return 0;
            }
        });
        addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                handleSelectionChanged(event.getSelection());
            }
        });
        addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                handleDoubleClick();
            }
        });
    }

    /**
	 * Set up the Refresh timer
	 */
    protected void setupRefreshTimer() {
        _timer = new Runnable() {

            public void run() {
                if (!getTree().isDisposed()) {
                    refresh();
                    Display.getDefault().timerExec(TIMERDELAY, this);
                }
            }
        };
        Display.getDefault().timerExec(TIMERDELAY, _timer);
    }

    /**
	 * Register Global Actions on focus events
	 */
    protected void registerGlobalActions() {
    }

    /**
	 * Set up drag and drop support
	 */
    protected void registerDragDrop() {
        int operations = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT;
        Transfer[] types = new Transfer[] { FileTransfer.getInstance() };
        addDropSupport(operations, types, new DropTargetListener() {

            public void dragEnter(DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    event.detail = DND.DROP_COPY;
                }
            }

            public void dragLeave(DropTargetEvent event) {
            }

            public void dragOperationChanged(DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    event.detail = DND.DROP_COPY;
                }
            }

            public void dragOver(DropTargetEvent event) {
                event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL | DND.FEEDBACK_EXPAND;
            }

            public void drop(DropTargetEvent event) {
                if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
                    handleFilesDropped(event);
                }
            }

            public void dropAccept(DropTargetEvent event) {
            }
        });
        addDragSupport(operations, types, new DragSourceListener() {

            /**
			 * Internal Drag Data
			 */
            Object dragData;

            public void dragStart(DragSourceEvent event) {
                StructuredSelection selection = (StructuredSelection) getSelection();
                Object[] objects = selection.toArray();
                if (objects.length == 0) {
                    event.doit = false;
                    dragData = null;
                    ISDRAGSOURCE = false;
                } else {
                    String[] files = new String[objects.length];
                    for (int i = 0; i < objects.length; i++) {
                        files[i] = ((File) objects[i]).getAbsolutePath();
                    }
                    event.doit = true;
                    dragData = files;
                    ISDRAGSOURCE = true;
                }
            }

            public void dragSetData(DragSourceEvent event) {
                event.data = dragData;
            }

            public void dragFinished(DragSourceEvent event) {
                ISDRAGSOURCE = false;
            }
        });
    }

    /**
	 * Make the Common Actions Implementors can over-ride to add their own
	 */
    protected void makeActions() {
        _actionNewFile = new StrakerActionFactory.NewFileAction() {

            public void run() {
                handleNewFileAction();
            }
        };
        _actionNewFolder = new StrakerActionFactory.NewFolderAction() {

            public void run() {
                handleNewFolderAction();
            }
        };
        _actionImport = new StrakerActionFactory.ImportAction() {

            public void run() {
                handleImportAction();
            }
        };
        _actionRefresh = new StrakerActionFactory.RefreshAction() {

            public void run() {
                handleRefreshAction();
            }
        };
        _actionDelete = new StrakerActionFactory.DeleteAction() {

            public void run() {
                handleDeleteAction();
            }
        };
        _actionDelete.setEnabled(false);
        _actionRename = new StrakerActionFactory.RenameAction() {

            public void run() {
                handleRenameAction();
            }
        };
        _actionRename.setEnabled(false);
        _actionCut = new StrakerActionFactory.CutAction() {

            public void run() {
                handleCutAction();
            }
        };
        _actionCut.setEnabled(false);
        _actionCopy = new StrakerActionFactory.CopyAction() {

            public void run() {
                handleCopyAction();
            }
        };
        _actionCopy.setEnabled(false);
        _actionPaste = new StrakerActionFactory.PasteAction() {

            public void run() {
                handlePasteAction();
            }
        };
        _actionPaste.setEnabled(false);
        _actionEdit = new StrakerActionFactory.OpenInEditorAction() {

            public void run() {
                handleEditAction();
            }
        };
        _actionEdit.setEnabled(false);
        _actionView = new StrakerActionFactory.ViewInBrowserAction() {

            public void run() {
                handleViewAction();
            }
        };
        _actionView.setEnabled(false);
    }

    /**
	 * Register Global Action Handlers. This needs to be called by the
	 * superclass.
	 * 
	 * @param bars
	 */
    protected void registerGlobalActions(IActionBars bars) {
        bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), _actionDelete);
        bars.setGlobalActionHandler(ActionFactory.RENAME.getId(), _actionRename);
        bars.updateActionBars();
    }

    /**
	 * Unregister Global Action Handlers. This needs to be called by the
	 * superclass.
	 * 
	 * @param bars
	 */
    protected void unregisterGlobalActions(IActionBars bars) {
        bars.setGlobalActionHandler(ActionFactory.DELETE.getId(), null);
        bars.setGlobalActionHandler(ActionFactory.RENAME.getId(), null);
        bars.updateActionBars();
    }

    /**
	 * @return The DrillDownAdapter
	 */
    public DrillDownAdapter getDrillDownAdapter() {
        return _drillDownAdapter;
    }

    /**
	 * Hook into a right-click menu
	 */
    protected void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(getControl());
        getControl().setMenu(menu);
    }

    /**
	 * Fill the right-click menu
	 * 
	 * @param manager
	 */
    protected void fillContextMenu(IMenuManager manager) {
        boolean isEmpty = getSelection().isEmpty();
        IMenuManager newMenu = new MenuManager("New", "new");
        manager.add(newMenu);
        newMenu.add(_actionNewFile);
        newMenu.add(_actionNewFolder);
        manager.add(_actionImport);
        if (!isEmpty) {
            manager.add(new Separator());
            manager.add(_actionEdit);
            manager.add(_actionView);
            manager.add(new Separator());
            manager.add(_actionDelete);
            manager.add(_actionRename);
        }
        manager.add(_actionRefresh);
        DrillDownAdapter driller = getDrillDownAdapter();
        if (driller.canGoInto() || driller.canGoBack() || driller.canGoHome()) {
            manager.add(new Separator());
            driller.addNavigationActions(manager);
        }
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    public void refresh() {
        _rootFolder.mkdirs();
        super.refresh();
    }

    public void refresh(final Object element) {
        _rootFolder.mkdirs();
        super.refresh(element);
    }

    /**
	 * Some files were dropped on the tree
	 * 
	 * @param event
	 *           The DropTargetEvent
	 */
    protected void handleFilesDropped(DropTargetEvent event) {
        String[] files = (String[]) event.data;
        if (files != null) {
            File targetParentFolder = null;
            if (event.item == null) {
                targetParentFolder = _rootFolder;
            } else if (event.item instanceof TreeItem) {
                TreeItem treeItem = (TreeItem) event.item;
                targetParentFolder = (File) treeItem.getData();
                if (!targetParentFolder.isDirectory()) {
                    targetParentFolder = targetParentFolder.getParentFile();
                }
            }
            if (targetParentFolder != null && targetParentFolder.exists() && targetParentFolder.isDirectory()) {
                try {
                    StrakerFileUtils.copyFilesWithProgressMonitor(targetParentFolder, files, getControl().getShell());
                } catch (IOException ex) {
                    MessageDialog.openError(getControl().getShell(), "Copy Files", ex.getMessage());
                }
                refresh(targetParentFolder);
                expandToLevel(targetParentFolder, 1);
                setSelection(new StructuredSelection(targetParentFolder));
            }
        }
    }

    /**
	 * Tree selection happened
	 * 
	 * @param selection
	 */
    protected void handleSelectionChanged(ISelection selection) {
        File file = (File) ((IStructuredSelection) selection).getFirstElement();
        boolean isEmpty = selection.isEmpty();
        _actionDelete.setEnabled(!isEmpty);
        _actionRename.setEnabled(!isEmpty);
        _actionEdit.setEnabled(!isEmpty && !file.isDirectory() && file.exists());
        _actionView.setEnabled(!isEmpty && !file.isDirectory() && file.exists());
    }

    /**
	 * Double-click happened - Open in Editor
	 * 
	 * @param selection
	 */
    protected void handleDoubleClick() {
        handleEditAction();
    }

    /**
	 * Launch file/folder natively
	 * 
	 * @param selection
	 */
    protected void handleViewExternal(ISelection selection) {
        final File file = (File) ((IStructuredSelection) selection).getFirstElement();
        if (file != null && file.exists()) {
            BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {

                public void run() {
                    boolean result = Program.launch(file.getAbsolutePath());
                }
            });
        }
    }

    /**
	 * New File event happened
	 */
    protected void handleNewFileAction() {
        File parent = (File) ((IStructuredSelection) getSelection()).getFirstElement();
        if (parent == null) {
            parent = _rootFolder;
        } else if (!parent.isDirectory()) {
            parent = parent.getParentFile();
        }
        if (parent.exists()) {
            NewFileDialog dialog = new NewFileDialog(getControl().getShell(), parent);
            if (dialog.open()) {
                File newFile = dialog.getFile();
                if (newFile != null) {
                    try {
                        newFile.createNewFile();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    expandToLevel(parent, 1);
                    refresh();
                }
            }
        }
    }

    /**
	 * New Folder event happened
	 */
    protected void handleNewFolderAction() {
        File parent = (File) ((IStructuredSelection) getSelection()).getFirstElement();
        if (parent == null) {
            parent = _rootFolder;
        } else if (!parent.isDirectory()) {
            parent = parent.getParentFile();
        }
        if (parent.exists()) {
            NewFolderDialog dialog = new NewFolderDialog(getControl().getShell(), parent);
            if (dialog.open()) {
                File newFolder = dialog.getFolder();
                if (newFolder != null) {
                    newFolder.mkdirs();
                    expandToLevel(parent, 1);
                    refresh();
                }
            }
        }
    }

    /**
	 * Import event happened
	 */
    protected void handleImportAction() {
        File parent = (File) ((IStructuredSelection) getSelection()).getFirstElement();
        if (parent == null) {
            parent = _rootFolder;
        } else if (!parent.isDirectory()) {
            parent = parent.getParentFile();
        }
        if (parent.exists() && parent.isDirectory()) {
            FileDialog dialog = new FileDialog(getControl().getShell(), SWT.MULTI | SWT.OPEN);
            dialog.setText("Choose files to import");
            String str = dialog.open();
            if (str != null) {
                String folder = dialog.getFilterPath();
                String[] filenames = dialog.getFileNames();
                String[] files = new String[filenames.length];
                for (int i = 0; i < filenames.length; i++) {
                    File tmp = new File(folder, filenames[i]);
                    files[i] = tmp.getAbsolutePath();
                }
                try {
                    StrakerFileUtils.copyFilesWithProgressMonitor(parent, files, getControl().getShell());
                } catch (IOException ex) {
                    MessageDialog.openError(getControl().getShell(), "Import", ex.getMessage());
                }
                expandToLevel(parent, 1);
                refresh();
            }
        }
    }

    /**
	 * Refresh event happened
	 */
    protected void handleRefreshAction() {
        refresh();
    }

    /**
	 * Paste event happened
	 */
    protected void handlePasteAction() {
    }

    /**
	 * Copy event happened
	 */
    protected void handleCopyAction() {
    }

    /**
	 * Cut event happened
	 */
    protected void handleCutAction() {
    }

    /**
	 * Rename event happened
	 */
    protected void handleRenameAction() {
        File file = (File) ((IStructuredSelection) getSelection()).getFirstElement();
        if (file != null && file.exists()) {
            RenameFileDialog dialog = new RenameFileDialog(getControl().getShell(), file);
            if (dialog.open()) {
                File newFile = dialog.getRenamedFile();
                if (newFile != null) {
                    boolean ok = file.renameTo(newFile);
                    if (ok) {
                        refresh();
                        setSelection(new StructuredSelection(newFile));
                    }
                }
            }
        }
    }

    /**
	 * Delete event happened
	 */
    protected void handleDeleteAction() {
        StructuredSelection selection = (StructuredSelection) getSelection();
        Object[] objects = selection.toArray();
        if (objects.length == 0) {
            return;
        }
        boolean ok = MessageDialog.openQuestion(getControl().getShell(), "Delete", objects.length == 1 ? "Are you sure you want to delete this file/folder?" : "Are you sure you want to delete these files/folders?");
        if (!ok) {
            return;
        }
        File sel = ((File) objects[0]).getParentFile();
        for (int i = 0; i < objects.length; i++) {
            File file = (File) objects[i];
            try {
                if (file.isDirectory()) {
                    FileUtils.deleteFolder(file);
                } else if (file.exists()) {
                    file.delete();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        refresh();
        if (sel != null) {
            setSelection(new StructuredSelection(sel));
        }
    }

    /**
	 * Edit event happened
	 */
    protected void handleEditAction() {
        File file = (File) ((IStructuredSelection) getSelection()).getFirstElement();
        if (file != null && !file.isDirectory() && file.exists()) {
            StrakerPlugin.getDefault().editInTextEditor(file);
        }
    }

    /**
	 * View event happened
	 */
    protected void handleViewAction() {
        File file = (File) ((IStructuredSelection) getSelection()).getFirstElement();
        if (file != null && !file.isDirectory() && file.exists()) {
            StrakerPlugin.getDefault().showInDefaultBrowser("file:///" + file.getPath());
        }
    }

    /**
	 * Dispose of stuff
	 */
    public void dispose() {
        if (_timer != null) {
            Display.getDefault().timerExec(-1, _timer);
            _timer = null;
        }
        _drillDownAdapter = null;
        _actionRefresh = null;
        _actionDelete = null;
        _actionRename = null;
        _actionCut = null;
        _actionCopy = null;
        _actionPaste = null;
        _actionNewFile = null;
        _actionNewFolder = null;
        _actionEdit = null;
        _actionView = null;
        _actionImport = null;
    }

    /**
	 * The Tree Model for the Tree.
	 */
    class FileTreeContentProvider implements ITreeContentProvider {

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }

        public Object getParent(Object child) {
            if (child instanceof File) {
                return ((File) child).getParentFile();
            }
            return null;
        }

        public Object[] getChildren(Object parent) {
            if (parent instanceof File) {
                return ((File) parent).listFiles();
            }
            return new Object[0];
        }

        public boolean hasChildren(Object parent) {
            if (parent instanceof File) {
                File f = (File) parent;
                return f.isDirectory() && f.listFiles().length > 0;
            }
            return false;
        }
    }

    class FileTreeLabelProvider extends LabelProvider {

        public String getText(Object obj) {
            if (obj instanceof File) {
                File f = (File) obj;
                return f.getName();
            } else {
                return "";
            }
        }

        public Image getImage(Object obj) {
            Image image = null;
            if (obj instanceof File) {
                File f = (File) obj;
                if (f.isDirectory()) {
                    image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                } else {
                    image = StrakerPlugin.getDefault().getFileExtensionImage(f);
                }
            }
            if (image == null) {
                image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
            }
            return image;
        }
    }
}
