import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.*;
import org.eclipse.swt.widgets.*;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * File Viewer example
 */
public class FileViewer {

    private static ResourceBundle resourceBundle = ResourceBundle.getBundle("examples_fileviewer");

    private static final String DRIVE_A = "a:" + File.separator;

    private static final String DRIVE_B = "b:" + File.separator;

    private Display display;

    private Shell shell;

    private ToolBar toolBar;

    private Label numObjectsLabel;

    private Label diskSpaceLabel;

    private File currentDirectory = null;

    private boolean initial = true;

    private boolean isDragging = false;

    private boolean isDropping = false;

    private File[] processedDropFiles = null;

    private File[] deferredRefreshFiles = null;

    private boolean deferredRefreshRequested = false;

    private ProgressDialog progressDialog = null;

    private static final String COMBODATA_ROOTS = "Combo.roots";

    private static final String COMBODATA_LASTTEXT = "Combo.lastText";

    private Combo combo;

    private IconCache iconCache = new IconCache();

    private static final String TREEITEMDATA_FILE = "TreeItem.file";

    private static final String TREEITEMDATA_IMAGEEXPANDED = "TreeItem.imageExpanded";

    private static final String TREEITEMDATA_IMAGECOLLAPSED = "TreeItem.imageCollapsed";

    private static final String TREEITEMDATA_STUB = "TreeItem.stub";

    private Tree tree;

    private Label treeScopeLabel;

    private static final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

    private static final String TABLEITEMDATA_FILE = "TableItem.file";

    private static final String TABLEDATA_DIR = "Table.dir";

    private static final int[] tableWidths = new int[] { 150, 60, 75, 150 };

    private final String[] tableTitles = new String[] { FileViewer.getResourceString("table.Name.title"), FileViewer.getResourceString("table.Size.title"), FileViewer.getResourceString("table.Type.title"), FileViewer.getResourceString("table.Modified.title") };

    private Table table;

    private Label tableContentsOfLabel;

    private final Object workerLock = new Object();

    private volatile Thread workerThread = null;

    private volatile boolean workerStopped = false;

    private volatile boolean workerCancelled = false;

    private volatile File workerStateDir = null;

    private volatile File workerNextDir = null;

    private boolean simulateOnly = true;

    /**
	 * Runs main program.
	 */
    public static void main(String[] args) {
        Display display = new Display();
        FileViewer application = new FileViewer();
        Shell shell = application.open(display);
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        application.close();
        display.dispose();
    }

    /**
	 * Opens the main program.
	 */
    public Shell open(Display display) {
        this.display = display;
        iconCache.initResources(display);
        shell = new Shell();
        createShellContents();
        notifyRefreshFiles(null);
        shell.open();
        return shell;
    }

    /**
	 * Closes the main program.
	 */
    void close() {
        workerStop();
        iconCache.freeResources();
    }

    /**
	 * Returns a string from the resource bundle.
	 * We don't want to crash because of a missing String.
	 * Returns the key if not found.
	 */
    static String getResourceString(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        } catch (NullPointerException e) {
            return "!" + key + "!";
        }
    }

    /**
	 * Returns a string from the resource bundle and binds it
	 * with the given arguments. If the key is not found,
	 * return the key.
	 */
    static String getResourceString(String key, Object[] args) {
        try {
            return MessageFormat.format(getResourceString(key), args);
        } catch (MissingResourceException e) {
            return key;
        } catch (NullPointerException e) {
            return "!" + key + "!";
        }
    }

    /**
	 * Construct the UI
	 * 
	 * @param container the ShellContainer managing the Shell we are rendering inside
	 */
    private void createShellContents() {
        shell.setText(getResourceString("Title", new Object[] { "" }));
        shell.setImage(iconCache.stockImages[iconCache.shellIcon]);
        Menu bar = new Menu(shell, SWT.BAR);
        shell.setMenuBar(bar);
        createFileMenu(bar);
        createHelpMenu(bar);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        gridLayout.marginHeight = gridLayout.marginWidth = 0;
        shell.setLayout(gridLayout);
        GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gridData.widthHint = 185;
        createComboView(shell, gridData);
        gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        gridData.horizontalSpan = 2;
        createToolBar(shell, gridData);
        SashForm sashForm = new SashForm(shell, SWT.NONE);
        sashForm.setOrientation(SWT.HORIZONTAL);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
        gridData.horizontalSpan = 3;
        sashForm.setLayoutData(gridData);
        createTreeView(sashForm);
        createTableView(sashForm);
        sashForm.setWeights(new int[] { 2, 5 });
        numObjectsLabel = new Label(shell, SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
        gridData.widthHint = 185;
        numObjectsLabel.setLayoutData(gridData);
        diskSpaceLabel = new Label(shell, SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
        gridData.horizontalSpan = 2;
        diskSpaceLabel.setLayoutData(gridData);
    }

    /**
	 * Creates the File Menu.
	 * 
	 * @param parent the parent menu
	 */
    private void createFileMenu(Menu parent) {
        Menu menu = new Menu(parent);
        MenuItem header = new MenuItem(parent, SWT.CASCADE);
        header.setText(getResourceString("menu.File.text"));
        header.setMenu(menu);
        final MenuItem simulateItem = new MenuItem(menu, SWT.CHECK);
        simulateItem.setText(getResourceString("menu.File.SimulateOnly.text"));
        simulateItem.setSelection(simulateOnly);
        simulateItem.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                simulateOnly = simulateItem.getSelection();
            }
        });
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(getResourceString("menu.File.Close.text"));
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                shell.close();
            }
        });
    }

    /**
	 * Creates the Help Menu.
	 * 
	 * @param parent the parent menu
	 */
    private void createHelpMenu(Menu parent) {
        Menu menu = new Menu(parent);
        MenuItem header = new MenuItem(parent, SWT.CASCADE);
        header.setText(getResourceString("menu.Help.text"));
        header.setMenu(menu);
        MenuItem item = new MenuItem(menu, SWT.PUSH);
        item.setText(getResourceString("menu.Help.About.text"));
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                box.setText(getResourceString("dialog.About.title"));
                box.setMessage(getResourceString("dialog.About.description", new Object[] { System.getProperty("os.name") }));
                box.open();
            }
        });
    }

    /**
	 * Creates the toolbar
	 * 
	 * @param shell the shell on which to attach the toolbar
	 * @param layoutData the layout data
	 */
    private void createToolBar(final Shell shell, Object layoutData) {
        toolBar = new ToolBar(shell, SWT.NONE);
        toolBar.setLayoutData(layoutData);
        ToolItem item = new ToolItem(toolBar, SWT.SEPARATOR);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdParent]);
        item.setToolTipText(getResourceString("tool.Parent.tiptext"));
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                doParent();
            }
        });
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdRefresh]);
        item.setToolTipText(getResourceString("tool.Refresh.tiptext"));
        item.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                doRefresh();
            }
        });
        SelectionAdapter unimplementedListener = new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                MessageBox box = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                box.setText(getResourceString("dialog.NotImplemented.title"));
                box.setMessage(getResourceString("dialog.ActionNotImplemented.description"));
                box.open();
            }
        };
        item = new ToolItem(toolBar, SWT.SEPARATOR);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdCut]);
        item.setToolTipText(getResourceString("tool.Cut.tiptext"));
        item.addSelectionListener(unimplementedListener);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdCopy]);
        item.setToolTipText(getResourceString("tool.Copy.tiptext"));
        item.addSelectionListener(unimplementedListener);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdPaste]);
        item.setToolTipText(getResourceString("tool.Paste.tiptext"));
        item.addSelectionListener(unimplementedListener);
        item = new ToolItem(toolBar, SWT.SEPARATOR);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdDelete]);
        item.setToolTipText(getResourceString("tool.Delete.tiptext"));
        item.addSelectionListener(unimplementedListener);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdRename]);
        item.setToolTipText(getResourceString("tool.Rename.tiptext"));
        item.addSelectionListener(unimplementedListener);
        item = new ToolItem(toolBar, SWT.SEPARATOR);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdSearch]);
        item.setToolTipText(getResourceString("tool.Search.tiptext"));
        item.addSelectionListener(unimplementedListener);
        item = new ToolItem(toolBar, SWT.PUSH);
        item.setImage(iconCache.stockImages[iconCache.cmdPrint]);
        item.setToolTipText(getResourceString("tool.Print.tiptext"));
        item.addSelectionListener(unimplementedListener);
    }

    /**
	 * Creates the combo box view.
	 * 
	 * @param parent the parent control
	 */
    private void createComboView(Composite parent, Object layoutData) {
        combo = new Combo(parent, SWT.NONE);
        combo.setLayoutData(layoutData);
        combo.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                final File[] roots = (File[]) combo.getData(COMBODATA_ROOTS);
                if (roots == null) return;
                int selection = combo.getSelectionIndex();
                if (selection >= 0 && selection < roots.length) {
                    notifySelectedDirectory(roots[selection]);
                }
            }

            public void widgetDefaultSelected(SelectionEvent e) {
                final String lastText = (String) combo.getData(COMBODATA_LASTTEXT);
                String text = combo.getText();
                if (text == null) return;
                if (lastText != null && lastText.equals(text)) return;
                combo.setData(COMBODATA_LASTTEXT, text);
                notifySelectedDirectory(new File(text));
            }
        });
    }

    /**
	 * Creates the file tree view.
	 * 
	 * @param parent the parent control
	 */
    private void createTreeView(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = gridLayout.marginWidth = 2;
        gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);
        treeScopeLabel = new Label(composite, SWT.BORDER);
        treeScopeLabel.setText(FileViewer.getResourceString("details.AllFolders.text"));
        treeScopeLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
        tree = new Tree(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
        tree.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
        tree.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent event) {
                final TreeItem[] selection = tree.getSelection();
                if (selection != null && selection.length != 0) {
                    TreeItem item = selection[0];
                    File file = (File) item.getData(TREEITEMDATA_FILE);
                    notifySelectedDirectory(file);
                }
            }

            public void widgetDefaultSelected(SelectionEvent event) {
                final TreeItem[] selection = tree.getSelection();
                if (selection != null && selection.length != 0) {
                    TreeItem item = selection[0];
                    item.setExpanded(true);
                    treeExpandItem(item);
                }
            }
        });
        tree.addTreeListener(new TreeAdapter() {

            public void treeExpanded(TreeEvent event) {
                final TreeItem item = (TreeItem) event.item;
                final Image image = (Image) item.getData(TREEITEMDATA_IMAGEEXPANDED);
                if (image != null) item.setImage(image);
                treeExpandItem(item);
            }

            public void treeCollapsed(TreeEvent event) {
                final TreeItem item = (TreeItem) event.item;
                final Image image = (Image) item.getData(TREEITEMDATA_IMAGECOLLAPSED);
                if (image != null) item.setImage(image);
            }
        });
        createTreeDragSource(tree);
        createTreeDropTarget(tree);
    }

    /**
	 * Creates the Drag & Drop DragSource for items being dragged from the tree.
	 * 
	 * @return the DragSource for the tree
	 */
    private DragSource createTreeDragSource(final Tree tree) {
        DragSource dragSource = new DragSource(tree, DND.DROP_MOVE | DND.DROP_COPY);
        dragSource.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dragSource.addDragListener(new DragSourceListener() {

            TreeItem[] dndSelection = null;

            String[] sourceNames = null;

            public void dragStart(DragSourceEvent event) {
                dndSelection = tree.getSelection();
                sourceNames = null;
                event.doit = dndSelection.length > 0;
                isDragging = true;
                processedDropFiles = null;
            }

            public void dragFinished(DragSourceEvent event) {
                dragSourceHandleDragFinished(event, sourceNames);
                dndSelection = null;
                sourceNames = null;
                isDragging = false;
                processedDropFiles = null;
                handleDeferredRefresh();
            }

            public void dragSetData(DragSourceEvent event) {
                if (dndSelection == null || dndSelection.length == 0) return;
                if (!FileTransfer.getInstance().isSupportedType(event.dataType)) return;
                sourceNames = new String[dndSelection.length];
                for (int i = 0; i < dndSelection.length; i++) {
                    File file = (File) dndSelection[i].getData(TREEITEMDATA_FILE);
                    sourceNames[i] = file.getAbsolutePath();
                }
                event.data = sourceNames;
            }
        });
        return dragSource;
    }

    /**
	 * Creates the Drag & Drop DropTarget for items being dropped onto the tree.
	 * 
	 * @return the DropTarget for the tree
	 */
    private DropTarget createTreeDropTarget(final Tree tree) {
        DropTarget dropTarget = new DropTarget(tree, DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dropTarget.addDropListener(new DropTargetAdapter() {

            public void dragEnter(DropTargetEvent event) {
                isDropping = true;
            }

            public void dragLeave(DropTargetEvent event) {
                isDropping = false;
                handleDeferredRefresh();
            }

            public void dragOver(DropTargetEvent event) {
                dropTargetValidate(event, getTargetFile(event));
                event.feedback |= DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
            }

            public void drop(DropTargetEvent event) {
                File targetFile = getTargetFile(event);
                if (dropTargetValidate(event, targetFile)) dropTargetHandleDrop(event, targetFile);
            }

            private File getTargetFile(DropTargetEvent event) {
                TreeItem item = tree.getItem(tree.toControl(new Point(event.x, event.y)));
                File targetFile = null;
                if (item != null) {
                    targetFile = (File) item.getData(TREEITEMDATA_FILE);
                }
                return targetFile;
            }
        });
        return dropTarget;
    }

    /**
	 * Handles expand events on a tree item.
	 * 
	 * @param item the TreeItem to fill in
	 */
    private void treeExpandItem(TreeItem item) {
        shell.setCursor(iconCache.stockCursors[iconCache.cursorWait]);
        final Object stub = item.getData(TREEITEMDATA_STUB);
        if (stub == null) treeRefreshItem(item, true);
        shell.setCursor(iconCache.stockCursors[iconCache.cursorDefault]);
    }

    /**
	 * Traverse the entire tree and update only what has changed.
	 * 
	 * @param roots the root directory listing
	 */
    private void treeRefresh(File[] masterFiles) {
        TreeItem[] items = tree.getItems();
        int masterIndex = 0;
        int itemIndex = 0;
        for (int i = 0; i < items.length; ++i) {
            final TreeItem item = items[i];
            final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
            if ((itemFile == null) || (masterIndex == masterFiles.length)) {
                item.dispose();
                continue;
            }
            final File masterFile = masterFiles[masterIndex];
            int compare = compareFiles(masterFile, itemFile);
            if (compare == 0) {
                treeRefreshItem(item, false);
                ++itemIndex;
                ++masterIndex;
            } else if (compare < 0) {
                TreeItem newItem = new TreeItem(tree, SWT.NONE, itemIndex);
                treeInitVolume(newItem, masterFile);
                new TreeItem(newItem, SWT.NONE);
                ++itemIndex;
                ++masterIndex;
                --i;
            } else {
                item.dispose();
            }
        }
        for (; masterIndex < masterFiles.length; ++masterIndex) {
            final File masterFile = masterFiles[masterIndex];
            TreeItem newItem = new TreeItem(tree, SWT.NONE);
            treeInitVolume(newItem, masterFile);
            new TreeItem(newItem, SWT.NONE);
        }
    }

    /**
	 * Traverse an item in the tree and update only what has changed.
	 * 
	 * @param dirItem the tree item of the directory
	 * @param forcePopulate true iff we should populate non-expanded items as well
	 */
    private void treeRefreshItem(TreeItem dirItem, boolean forcePopulate) {
        final File dir = (File) dirItem.getData(TREEITEMDATA_FILE);
        if (!forcePopulate && !dirItem.getExpanded()) {
            if (dirItem.getData(TREEITEMDATA_STUB) != null) {
                treeItemRemoveAll(dirItem);
                new TreeItem(dirItem, SWT.NONE);
                dirItem.setData(TREEITEMDATA_STUB, null);
            }
            return;
        }
        dirItem.setData(TREEITEMDATA_STUB, this);
        File[] subFiles = (dir != null) ? FileViewer.getDirectoryList(dir) : null;
        if (subFiles == null || subFiles.length == 0) {
            treeItemRemoveAll(dirItem);
            dirItem.setExpanded(false);
            return;
        }
        TreeItem[] items = dirItem.getItems();
        final File[] masterFiles = subFiles;
        int masterIndex = 0;
        int itemIndex = 0;
        File masterFile = null;
        for (int i = 0; i < items.length; ++i) {
            while ((masterFile == null) && (masterIndex < masterFiles.length)) {
                masterFile = masterFiles[masterIndex++];
                if (!masterFile.isDirectory()) masterFile = null;
            }
            final TreeItem item = items[i];
            final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
            if ((itemFile == null) || (masterFile == null)) {
                item.dispose();
                continue;
            }
            int compare = compareFiles(masterFile, itemFile);
            if (compare == 0) {
                treeRefreshItem(item, false);
                masterFile = null;
                ++itemIndex;
            } else if (compare < 0) {
                TreeItem newItem = new TreeItem(dirItem, SWT.NONE, itemIndex);
                treeInitFolder(newItem, masterFile);
                new TreeItem(newItem, SWT.NONE);
                masterFile = null;
                ++itemIndex;
                --i;
            } else {
                item.dispose();
            }
        }
        while ((masterFile != null) || (masterIndex < masterFiles.length)) {
            if (masterFile != null) {
                TreeItem newItem = new TreeItem(dirItem, SWT.NONE);
                treeInitFolder(newItem, masterFile);
                new TreeItem(newItem, SWT.NONE);
                if (masterIndex == masterFiles.length) break;
            }
            masterFile = masterFiles[masterIndex++];
            if (!masterFile.isDirectory()) masterFile = null;
        }
    }

    /**
	 * Foreign method: removes all children of a TreeItem.
	 * @param treeItem the TreeItem
	 */
    private static void treeItemRemoveAll(TreeItem treeItem) {
        final TreeItem[] children = treeItem.getItems();
        for (int i = 0; i < children.length; ++i) {
            children[i].dispose();
        }
    }

    /**
	 * Initializes a folder item.
	 * 
	 * @param item the TreeItem to initialize
	 * @param folder the File associated with this TreeItem
	 */
    private void treeInitFolder(TreeItem item, File folder) {
        item.setText(folder.getName());
        item.setImage(iconCache.stockImages[iconCache.iconClosedFolder]);
        item.setData(TREEITEMDATA_FILE, folder);
        item.setData(TREEITEMDATA_IMAGEEXPANDED, iconCache.stockImages[iconCache.iconOpenFolder]);
        item.setData(TREEITEMDATA_IMAGECOLLAPSED, iconCache.stockImages[iconCache.iconClosedFolder]);
    }

    /**
	 * Initializes a volume item.
	 * 
	 * @param item the TreeItem to initialize
	 * @param volume the File associated with this TreeItem
	 */
    private void treeInitVolume(TreeItem item, File volume) {
        item.setText(volume.getPath());
        item.setImage(iconCache.stockImages[iconCache.iconClosedDrive]);
        item.setData(TREEITEMDATA_FILE, volume);
        item.setData(TREEITEMDATA_IMAGEEXPANDED, iconCache.stockImages[iconCache.iconOpenDrive]);
        item.setData(TREEITEMDATA_IMAGECOLLAPSED, iconCache.stockImages[iconCache.iconClosedDrive]);
    }

    /**
	 * Creates the file details table.
	 * 
	 * @param parent the parent control
	 */
    private void createTableView(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = gridLayout.marginWidth = 2;
        gridLayout.horizontalSpacing = gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);
        tableContentsOfLabel = new Label(composite, SWT.BORDER);
        tableContentsOfLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
        table = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
        table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
        for (int i = 0; i < tableTitles.length; ++i) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(tableTitles[i]);
            column.setWidth(tableWidths[i]);
        }
        table.setHeaderVisible(true);
        table.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent event) {
                notifySelectedFiles(getSelectedFiles());
            }

            public void widgetDefaultSelected(SelectionEvent event) {
                doDefaultFileAction(getSelectedFiles());
            }

            private File[] getSelectedFiles() {
                final TableItem[] items = table.getSelection();
                final File[] files = new File[items.length];
                for (int i = 0; i < items.length; ++i) {
                    files[i] = (File) items[i].getData(TABLEITEMDATA_FILE);
                }
                return files;
            }
        });
        createTableDragSource(table);
        createTableDropTarget(table);
    }

    /**
	 * Creates the Drag & Drop DragSource for items being dragged from the table.
	 * 
	 * @return the DragSource for the table
	 */
    private DragSource createTableDragSource(final Table table) {
        DragSource dragSource = new DragSource(table, DND.DROP_MOVE | DND.DROP_COPY);
        dragSource.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dragSource.addDragListener(new DragSourceListener() {

            TableItem[] dndSelection = null;

            String[] sourceNames = null;

            public void dragStart(DragSourceEvent event) {
                dndSelection = table.getSelection();
                sourceNames = null;
                event.doit = dndSelection.length > 0;
                isDragging = true;
            }

            public void dragFinished(DragSourceEvent event) {
                dragSourceHandleDragFinished(event, sourceNames);
                dndSelection = null;
                sourceNames = null;
                isDragging = false;
                handleDeferredRefresh();
            }

            public void dragSetData(DragSourceEvent event) {
                if (dndSelection == null || dndSelection.length == 0) return;
                if (!FileTransfer.getInstance().isSupportedType(event.dataType)) return;
                sourceNames = new String[dndSelection.length];
                for (int i = 0; i < dndSelection.length; i++) {
                    File file = (File) dndSelection[i].getData(TABLEITEMDATA_FILE);
                    sourceNames[i] = file.getAbsolutePath();
                }
                event.data = sourceNames;
            }
        });
        return dragSource;
    }

    /**
	 * Creates the Drag & Drop DropTarget for items being dropped onto the table.
	 * 
	 * @return the DropTarget for the table
	 */
    private DropTarget createTableDropTarget(final Table table) {
        DropTarget dropTarget = new DropTarget(table, DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        dropTarget.addDropListener(new DropTargetAdapter() {

            public void dragEnter(DropTargetEvent event) {
                isDropping = true;
            }

            public void dragLeave(DropTargetEvent event) {
                isDropping = false;
                handleDeferredRefresh();
            }

            public void dragOver(DropTargetEvent event) {
                dropTargetValidate(event, getTargetFile(event));
                event.feedback |= DND.FEEDBACK_EXPAND | DND.FEEDBACK_SCROLL;
            }

            public void drop(DropTargetEvent event) {
                File targetFile = getTargetFile(event);
                if (dropTargetValidate(event, targetFile)) dropTargetHandleDrop(event, targetFile);
            }

            private File getTargetFile(DropTargetEvent event) {
                TableItem item = table.getItem(table.toControl(new Point(event.x, event.y)));
                File targetFile = null;
                if (item == null) {
                    if (event.detail == DND.DROP_COPY) {
                        targetFile = (File) table.getData(TABLEDATA_DIR);
                    }
                } else {
                    targetFile = (File) item.getData(TABLEITEMDATA_FILE);
                }
                return targetFile;
            }
        });
        return dropTarget;
    }

    /**
	 * Notifies the application components that a new current directory has been selected
	 * 
	 * @param dir the directory that was selected, null is ignored
	 */
    void notifySelectedDirectory(File dir) {
        if (dir == null) return;
        if (currentDirectory != null && dir.equals(currentDirectory)) return;
        currentDirectory = dir;
        notifySelectedFiles(null);
        shell.setText(getResourceString("Title", new Object[] { currentDirectory.getPath() }));
        workerUpdate(dir, false);
        final File[] comboRoots = (File[]) combo.getData(COMBODATA_ROOTS);
        int comboEntry = -1;
        if (comboRoots != null) {
            for (int i = 0; i < comboRoots.length; ++i) {
                if (dir.equals(comboRoots[i])) {
                    comboEntry = i;
                    break;
                }
            }
        }
        if (comboEntry == -1) combo.setText(dir.getPath()); else combo.select(comboEntry);
        Vector path = new Vector();
        while (dir != null) {
            path.add(dir);
            dir = dir.getParentFile();
        }
        TreeItem[] items = tree.getItems();
        TreeItem lastItem = null;
        for (int i = path.size() - 1; i >= 0; --i) {
            final File pathElement = (File) path.elementAt(i);
            TreeItem item = null;
            for (int k = 0; k < items.length; ++k) {
                item = items[k];
                if (item.isDisposed()) continue;
                final File itemFile = (File) item.getData(TREEITEMDATA_FILE);
                if (itemFile != null && itemFile.equals(pathElement)) break;
            }
            if (item == null) break;
            lastItem = item;
            if (i != 0 && !item.getExpanded()) {
                treeExpandItem(item);
                item.setExpanded(true);
            }
            items = item.getItems();
        }
        tree.setSelection((lastItem != null) ? new TreeItem[] { lastItem } : new TreeItem[0]);
    }

    /**
	 * Notifies the application components that files have been selected
	 * 
	 * @param files the files that were selected, null or empty array indicates no active selection
	 */
    void notifySelectedFiles(File[] files) {
        if ((files != null) && (files.length != 0)) {
            numObjectsLabel.setText(getResourceString("details.NumberOfSelectedFiles.text", new Object[] { new Integer(files.length) }));
            long fileSize = 0L;
            for (int i = 0; i < files.length; ++i) {
                fileSize += files[i].length();
            }
            diskSpaceLabel.setText(getResourceString("details.FileSize.text", new Object[] { new Long(fileSize) }));
        } else {
            diskSpaceLabel.setText("");
            if (currentDirectory != null) {
                int numObjects = getDirectoryList(currentDirectory).length;
                numObjectsLabel.setText(getResourceString("details.DirNumberOfObjects.text", new Object[] { new Integer(numObjects) }));
            } else {
                numObjectsLabel.setText("");
            }
        }
    }

    /**
	 * Notifies the application components that files must be refreshed
	 * 
	 * @param files the files that need refreshing, empty array is a no-op, null refreshes all
	 */
    void notifyRefreshFiles(File[] files) {
        if (files != null && files.length == 0) return;
        if ((deferredRefreshRequested) && (deferredRefreshFiles != null) && (files != null)) {
            File[] newRequest = new File[deferredRefreshFiles.length + files.length];
            System.arraycopy(deferredRefreshFiles, 0, newRequest, 0, deferredRefreshFiles.length);
            System.arraycopy(files, 0, newRequest, deferredRefreshFiles.length, files.length);
            deferredRefreshFiles = newRequest;
        } else {
            deferredRefreshFiles = files;
            deferredRefreshRequested = true;
        }
        handleDeferredRefresh();
    }

    /**
	 * Handles deferred Refresh notifications (due to Drag & Drop)
	 */
    void handleDeferredRefresh() {
        if (isDragging || isDropping || !deferredRefreshRequested) return;
        if (progressDialog != null) {
            progressDialog.close();
            progressDialog = null;
        }
        deferredRefreshRequested = false;
        File[] files = deferredRefreshFiles;
        deferredRefreshFiles = null;
        shell.setCursor(iconCache.stockCursors[iconCache.cursorWait]);
        boolean refreshTable = false;
        if (files != null) {
            for (int i = 0; i < files.length; ++i) {
                final File file = files[i];
                if (file.equals(currentDirectory)) {
                    refreshTable = true;
                    break;
                }
                File parentFile = file.getParentFile();
                if ((parentFile != null) && (parentFile.equals(currentDirectory))) {
                    refreshTable = true;
                    break;
                }
            }
        } else refreshTable = true;
        if (refreshTable) workerUpdate(currentDirectory, true);
        final File[] roots = getRoots();
        if (files == null) {
            boolean refreshCombo = false;
            final File[] comboRoots = (File[]) combo.getData(COMBODATA_ROOTS);
            if ((comboRoots != null) && (comboRoots.length == roots.length)) {
                for (int i = 0; i < roots.length; ++i) {
                    if (!roots[i].equals(comboRoots[i])) {
                        refreshCombo = true;
                        break;
                    }
                }
            } else refreshCombo = true;
            if (refreshCombo) {
                combo.removeAll();
                combo.setData(COMBODATA_ROOTS, roots);
                for (int i = 0; i < roots.length; ++i) {
                    final File file = roots[i];
                    combo.add(file.getPath());
                }
            }
        }
        treeRefresh(roots);
        final File dir = currentDirectory;
        currentDirectory = null;
        notifySelectedDirectory(dir);
        shell.setCursor(iconCache.stockCursors[iconCache.cursorDefault]);
    }

    /**
	 * Performs the default action on a set of files.
	 * 
	 * @param files the array of files to process
	 */
    void doDefaultFileAction(File[] files) {
        if (files.length == 0) return;
        final File file = files[0];
        if (file.isDirectory()) {
            notifySelectedDirectory(file);
        } else {
            final String fileName = file.getAbsolutePath();
            if (!Program.launch(fileName)) {
                MessageBox dialog = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
                dialog.setMessage(getResourceString("error.FailedLaunch.message", new Object[] { fileName }));
                dialog.setText(shell.getText());
                dialog.open();
            }
        }
    }

    /**
	 * Navigates to the parent directory
	 */
    void doParent() {
        if (currentDirectory == null) return;
        File parentDirectory = currentDirectory.getParentFile();
        notifySelectedDirectory(parentDirectory);
    }

    /**
	 * Performs a refresh
	 */
    void doRefresh() {
        notifyRefreshFiles(null);
    }

    /**
	 * Validates a drop target as a candidate for a drop operation.
	 * <p>
	 * Used in dragOver() and dropAccept().<br>
	 * Note event.detail is set to DND.DROP_NONE by this method if the target is not valid.
	 * </p>
	 * @param event the DropTargetEvent to validate
	 * @param targetFile the File representing the drop target location
	 *        under inspection, or null if none
	 */
    private boolean dropTargetValidate(DropTargetEvent event, File targetFile) {
        if (targetFile != null && targetFile.isDirectory()) {
            if (event.detail != DND.DROP_COPY && event.detail != DND.DROP_MOVE) {
                event.detail = DND.DROP_MOVE;
            }
        } else {
            event.detail = DND.DROP_NONE;
        }
        return event.detail != DND.DROP_NONE;
    }

    /**
	 * Handles a drop on a dropTarget.
	 * <p>
	 * Used in drop().<br>
	 * Note event.detail is modified by this method.
	 * </p>
	 * @param event the DropTargetEvent passed as parameter to the drop() method
	 * @param targetFile the File representing the drop target location
	 *        under inspection, or null if none
	 */
    private void dropTargetHandleDrop(DropTargetEvent event, File targetFile) {
        if (!dropTargetValidate(event, targetFile)) return;
        final String[] sourceNames = (String[]) event.data;
        if (sourceNames == null) event.detail = DND.DROP_NONE;
        if (event.detail == DND.DROP_NONE) return;
        progressDialog = new ProgressDialog(shell, (event.detail == DND.DROP_MOVE) ? ProgressDialog.MOVE : ProgressDialog.COPY);
        progressDialog.setTotalWorkUnits(sourceNames.length);
        progressDialog.open();
        Vector processedFiles = new Vector();
        for (int i = 0; (i < sourceNames.length) && (!progressDialog.isCancelled()); i++) {
            final File source = new File(sourceNames[i]);
            final File dest = new File(targetFile, source.getName());
            if (source.equals(dest)) continue;
            progressDialog.setDetailFile(source, ProgressDialog.COPY);
            while (!progressDialog.isCancelled()) {
                if (copyFileStructure(source, dest)) {
                    processedFiles.add(source);
                    break;
                } else if (!progressDialog.isCancelled()) {
                    if (event.detail == DND.DROP_MOVE && (!isDragging)) {
                        MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.RETRY | SWT.CANCEL);
                        box.setText(getResourceString("dialog.FailedCopy.title"));
                        box.setMessage(getResourceString("dialog.FailedCopy.description", new Object[] { source, dest }));
                        int button = box.open();
                        if (button == SWT.CANCEL) {
                            i = sourceNames.length;
                            event.detail = DND.DROP_NONE;
                            break;
                        }
                    } else {
                        MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.ABORT | SWT.RETRY | SWT.IGNORE);
                        box.setText(getResourceString("dialog.FailedCopy.title"));
                        box.setMessage(getResourceString("dialog.FailedCopy.description", new Object[] { source, dest }));
                        int button = box.open();
                        if (button == SWT.ABORT) i = sourceNames.length;
                        if (button != SWT.RETRY) break;
                    }
                }
                progressDialog.addProgress(1);
            }
        }
        if (isDragging) {
            processedDropFiles = ((File[]) processedFiles.toArray(new File[processedFiles.size()]));
        } else {
            progressDialog.close();
            progressDialog = null;
        }
        notifyRefreshFiles(new File[] { targetFile });
    }

    /**
	 * Handles the completion of a drag on a dragSource.
	 * <p>
	 * Used in dragFinished().<br>
	 * </p>
	 * @param event the DragSourceEvent passed as parameter to the dragFinished() method
	 * @param sourceNames the names of the files that were dragged (event.data is invalid)
	 */
    private void dragSourceHandleDragFinished(DragSourceEvent event, String[] sourceNames) {
        if (sourceNames == null) return;
        if (event.detail != DND.DROP_MOVE) return;
        final File[] sourceFiles;
        if (processedDropFiles != null) {
            sourceFiles = processedDropFiles;
        } else {
            sourceFiles = new File[sourceNames.length];
            for (int i = 0; i < sourceNames.length; ++i) sourceFiles[i] = new File(sourceNames[i]);
        }
        if (progressDialog == null) progressDialog = new ProgressDialog(shell, ProgressDialog.MOVE);
        progressDialog.setTotalWorkUnits(sourceFiles.length);
        progressDialog.setProgress(0);
        progressDialog.open();
        for (int i = 0; (i < sourceFiles.length) && (!progressDialog.isCancelled()); i++) {
            final File source = sourceFiles[i];
            progressDialog.setDetailFile(source, ProgressDialog.DELETE);
            while (!progressDialog.isCancelled()) {
                if (deleteFileStructure(source)) {
                    break;
                } else if (!progressDialog.isCancelled()) {
                    MessageBox box = new MessageBox(shell, SWT.ICON_ERROR | SWT.ABORT | SWT.RETRY | SWT.IGNORE);
                    box.setText(getResourceString("dialog.FailedDelete.title"));
                    box.setMessage(getResourceString("dialog.FailedDelete.description", new Object[] { source }));
                    int button = box.open();
                    if (button == SWT.ABORT) i = sourceNames.length;
                    if (button == SWT.RETRY) break;
                }
            }
            progressDialog.addProgress(1);
        }
        notifyRefreshFiles(sourceFiles);
        progressDialog.close();
        progressDialog = null;
    }

    /**
	 * Gets filesystem root entries
	 * 
	 * @return an array of Files corresponding to the root directories on the platform,
	 *         may be empty but not null
	 */
    File[] getRoots() {
        if (System.getProperty("os.name").indexOf("Windows") != -1) {
            Vector list = new Vector();
            list.add(new File(DRIVE_A));
            list.add(new File(DRIVE_B));
            for (char i = 'c'; i <= 'z'; ++i) {
                File drive = new File(i + ":" + File.separator);
                if (drive.isDirectory() && drive.exists()) {
                    list.add(drive);
                    if (initial && i == 'c') {
                        currentDirectory = drive;
                        initial = false;
                    }
                }
            }
            File[] roots = (File[]) list.toArray(new File[list.size()]);
            sortFiles(roots);
            return roots;
        }
        File root = new File(File.separator);
        if (initial) {
            currentDirectory = root;
            initial = false;
        }
        return new File[] { root };
    }

    /**
	 * Gets a directory listing
	 * 
	 * @param file the directory to be listed
	 * @return an array of files this directory contains, may be empty but not null
	 */
    static File[] getDirectoryList(File file) {
        File[] list = file.listFiles();
        if (list == null) return new File[0];
        sortFiles(list);
        return list;
    }

    /**
	 * Copies a file or entire directory structure.
	 * 
	 * @param oldFile the location of the old file or directory
	 * @param newFile the location of the new file or directory
	 * @return true iff the operation succeeds without errors
	 */
    boolean copyFileStructure(File oldFile, File newFile) {
        if (oldFile == null || newFile == null) return false;
        File searchFile = newFile;
        do {
            if (oldFile.equals(searchFile)) return false;
            searchFile = searchFile.getParentFile();
        } while (searchFile != null);
        if (oldFile.isDirectory()) {
            if (progressDialog != null) {
                progressDialog.setDetailFile(oldFile, ProgressDialog.COPY);
            }
            if (simulateOnly) {
            } else {
                if (!newFile.mkdirs()) return false;
            }
            File[] subFiles = oldFile.listFiles();
            if (subFiles != null) {
                if (progressDialog != null) {
                    progressDialog.addWorkUnits(subFiles.length);
                }
                for (int i = 0; i < subFiles.length; i++) {
                    File oldSubFile = subFiles[i];
                    File newSubFile = new File(newFile, oldSubFile.getName());
                    if (!copyFileStructure(oldSubFile, newSubFile)) return false;
                    if (progressDialog != null) {
                        progressDialog.addProgress(1);
                        if (progressDialog.isCancelled()) return false;
                    }
                }
            }
        } else {
            if (simulateOnly) {
            } else {
                FileReader in = null;
                FileWriter out = null;
                try {
                    in = new FileReader(oldFile);
                    out = new FileWriter(newFile);
                    int count;
                    while ((count = in.read()) != -1) out.write(count);
                } catch (FileNotFoundException e) {
                    return false;
                } catch (IOException e) {
                    return false;
                } finally {
                    try {
                        if (in != null) in.close();
                        if (out != null) out.close();
                    } catch (IOException e) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
	 * Deletes a file or entire directory structure.
	 * 
	 * @param oldFile the location of the old file or directory
	 * @return true iff the operation succeeds without errors
	 */
    boolean deleteFileStructure(File oldFile) {
        if (oldFile == null) return false;
        if (oldFile.isDirectory()) {
            if (progressDialog != null) {
                progressDialog.setDetailFile(oldFile, ProgressDialog.DELETE);
            }
            File[] subFiles = oldFile.listFiles();
            if (subFiles != null) {
                if (progressDialog != null) {
                    progressDialog.addWorkUnits(subFiles.length);
                }
                for (int i = 0; i < subFiles.length; i++) {
                    File oldSubFile = subFiles[i];
                    if (!deleteFileStructure(oldSubFile)) return false;
                    if (progressDialog != null) {
                        progressDialog.addProgress(1);
                        if (progressDialog.isCancelled()) return false;
                    }
                }
            }
        }
        if (simulateOnly) {
            return true;
        }
        return oldFile.delete();
    }

    /**
	 * Sorts files lexicographically by name.
	 * 
	 * @param files the array of Files to be sorted
	 */
    static void sortFiles(File[] files) {
        sortBlock(files, 0, files.length - 1, new File[files.length]);
    }

    private static void sortBlock(File[] files, int start, int end, File[] mergeTemp) {
        final int length = end - start + 1;
        if (length < 8) {
            for (int i = end; i > start; --i) {
                for (int j = end; j > start; --j) {
                    if (compareFiles(files[j - 1], files[j]) > 0) {
                        final File temp = files[j];
                        files[j] = files[j - 1];
                        files[j - 1] = temp;
                    }
                }
            }
            return;
        }
        final int mid = (start + end) / 2;
        sortBlock(files, start, mid, mergeTemp);
        sortBlock(files, mid + 1, end, mergeTemp);
        int x = start;
        int y = mid + 1;
        for (int i = 0; i < length; ++i) {
            if ((x > mid) || ((y <= end) && compareFiles(files[x], files[y]) > 0)) {
                mergeTemp[i] = files[y++];
            } else {
                mergeTemp[i] = files[x++];
            }
        }
        for (int i = 0; i < length; ++i) files[i + start] = mergeTemp[i];
    }

    private static int compareFiles(File a, File b) {
        int compare = a.getName().compareToIgnoreCase(b.getName());
        if (compare == 0) compare = a.getName().compareTo(b.getName());
        return compare;
    }

    /**
	 * Stops the worker and waits for it to terminate.
	 */
    void workerStop() {
        if (workerThread == null) return;
        synchronized (workerLock) {
            workerCancelled = true;
            workerStopped = true;
            workerLock.notifyAll();
        }
        while (workerThread != null) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }

    /**
	 * Notifies the worker that it should update itself with new data.
	 * Cancels any previous operation and begins a new one.
	 * 
	 * @param dir the new base directory for the table, null is ignored
	 * @param force if true causes a refresh even if the data is the same
	 */
    void workerUpdate(File dir, boolean force) {
        if (dir == null) return;
        if ((!force) && (workerNextDir != null) && (workerNextDir.equals(dir))) return;
        synchronized (workerLock) {
            workerNextDir = dir;
            workerStopped = false;
            workerCancelled = true;
            workerLock.notifyAll();
        }
        if (workerThread == null) {
            workerThread = new Thread(workerRunnable);
            workerThread.start();
        }
    }

    /**
	 * Manages the worker's thread
	 */
    private final Runnable workerRunnable = new Runnable() {

        public void run() {
            while (!workerStopped) {
                synchronized (workerLock) {
                    workerCancelled = false;
                    workerStateDir = workerNextDir;
                }
                workerExecute();
                synchronized (workerLock) {
                    try {
                        if ((!workerCancelled) && (workerStateDir == workerNextDir)) workerLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            workerThread = null;
            display.wake();
        }
    };

    /**
	 * Updates the table's contents
	 */
    private void workerExecute() {
        File[] dirList;
        display.syncExec(new Runnable() {

            public void run() {
                tableContentsOfLabel.setText(FileViewer.getResourceString("details.ContentsOf.text", new Object[] { workerStateDir.getPath() }));
                table.removeAll();
                table.setData(TABLEDATA_DIR, workerStateDir);
            }
        });
        dirList = getDirectoryList(workerStateDir);
        for (int i = 0; (!workerCancelled) && (i < dirList.length); i++) {
            workerAddFileDetails(dirList[i]);
        }
    }

    /**
	 * Adds a file's detail information to the directory list
	 */
    private void workerAddFileDetails(final File file) {
        final String nameString = file.getName();
        final String dateString = dateFormat.format(new Date(file.lastModified()));
        final String sizeString;
        final String typeString;
        final Image iconImage;
        if (file.isDirectory()) {
            typeString = getResourceString("filetype.Folder");
            sizeString = "";
            iconImage = iconCache.stockImages[iconCache.iconClosedFolder];
        } else {
            sizeString = getResourceString("filesize.KB", new Object[] { new Long((file.length() + 512) / 1024) });
            int dot = nameString.lastIndexOf('.');
            if (dot != -1) {
                String extension = nameString.substring(dot);
                Program program = Program.findProgram(extension);
                if (program != null) {
                    typeString = program.getName();
                    iconImage = iconCache.getIconFromProgram(program);
                } else {
                    typeString = getResourceString("filetype.Unknown", new Object[] { extension.toUpperCase() });
                    iconImage = iconCache.stockImages[iconCache.iconFile];
                }
            } else {
                typeString = getResourceString("filetype.None");
                iconImage = iconCache.stockImages[iconCache.iconFile];
            }
        }
        final String[] strings = new String[] { nameString, sizeString, typeString, dateString };
        display.syncExec(new Runnable() {

            public void run() {
                if (shell.isDisposed()) return;
                TableItem tableItem = new TableItem(table, 0);
                tableItem.setText(strings);
                tableItem.setImage(iconImage);
                tableItem.setData(TABLEITEMDATA_FILE, file);
            }
        });
    }

    /**
	 * Instances of this class manage a progress dialog for file operations.
	 */
    class ProgressDialog {

        public static final int COPY = 0;

        public static final int DELETE = 1;

        public static final int MOVE = 2;

        Shell shell;

        Label messageLabel, detailLabel;

        ProgressBar progressBar;

        Button cancelButton;

        boolean isCancelled = false;

        final String operationKeyName[] = { "Copy", "Delete", "Move" };

        /**
		 * Creates a progress dialog but does not open it immediately.
		 * 
		 * @param parent the parent Shell
		 * @param style one of COPY, MOVE
		 */
        public ProgressDialog(Shell parent, int style) {
            shell = new Shell(parent, SWT.BORDER | SWT.TITLE | SWT.APPLICATION_MODAL);
            GridLayout gridLayout = new GridLayout();
            shell.setLayout(gridLayout);
            shell.setText(getResourceString("progressDialog." + operationKeyName[style] + ".title"));
            shell.addShellListener(new ShellAdapter() {

                public void shellClosed(ShellEvent e) {
                    isCancelled = true;
                }
            });
            messageLabel = new Label(shell, SWT.HORIZONTAL);
            messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
            messageLabel.setText(getResourceString("progressDialog." + operationKeyName[style] + ".description"));
            progressBar = new ProgressBar(shell, SWT.HORIZONTAL | SWT.WRAP);
            progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
            progressBar.setMinimum(0);
            progressBar.setMaximum(0);
            detailLabel = new Label(shell, SWT.HORIZONTAL);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
            gridData.widthHint = 400;
            detailLabel.setLayoutData(gridData);
            cancelButton = new Button(shell, SWT.PUSH);
            cancelButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.VERTICAL_ALIGN_FILL));
            cancelButton.setText(getResourceString("progressDialog.cancelButton.text"));
            cancelButton.addSelectionListener(new SelectionAdapter() {

                public void widgetSelected(SelectionEvent e) {
                    isCancelled = true;
                    cancelButton.setEnabled(false);
                }
            });
        }

        /**
		 * Sets the detail text to show the filename along with a string
		 * representing the operation being performed on that file.
		 * 
		 * @param file the file to be detailed
		 * @param operation one of COPY, DELETE
		 */
        public void setDetailFile(File file, int operation) {
            detailLabel.setText(getResourceString("progressDialog." + operationKeyName[operation] + ".operation", new Object[] { file }));
        }

        /**
		 * Returns true if the Cancel button was been clicked.
		 * 
		 * @return true if the Cancel button was clicked.
		 */
        public boolean isCancelled() {
            return isCancelled;
        }

        /**
		 * Sets the total number of work units to be performed.
		 * 
		 * @param work the total number of work units
		 */
        public void setTotalWorkUnits(int work) {
            progressBar.setMaximum(work);
        }

        /**
		 * Adds to the total number of work units to be performed.
		 * 
		 * @param work the number of work units to add
		 */
        public void addWorkUnits(int work) {
            setTotalWorkUnits(progressBar.getMaximum() + work);
        }

        /**
		 * Sets the progress of completion of the total work units.
		 * 
		 * @param work the total number of work units completed
		 */
        public void setProgress(int work) {
            progressBar.setSelection(work);
            while (display.readAndDispatch()) {
            }
        }

        /**
		 * Adds to the progress of completion of the total work units.
		 * 
		 * @param work the number of work units completed to add
		 */
        public void addProgress(int work) {
            setProgress(progressBar.getSelection() + work);
        }

        /**
		 * Opens the dialog.
		 */
        public void open() {
            shell.pack();
            final Shell parentShell = (Shell) shell.getParent();
            Rectangle rect = parentShell.getBounds();
            Rectangle bounds = shell.getBounds();
            bounds.x = rect.x + (rect.width - bounds.width) / 2;
            bounds.y = rect.y + (rect.height - bounds.height) / 2;
            shell.setBounds(bounds);
            shell.open();
        }

        /**
		 * Closes the dialog and disposes its resources.
		 */
        public void close() {
            shell.close();
            shell.dispose();
            shell = null;
            messageLabel = null;
            detailLabel = null;
            progressBar = null;
            cancelButton = null;
        }
    }
}
