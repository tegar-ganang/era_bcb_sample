package com.simontuffs.eclipse.jarplug.views;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.core.JarPackageFragmentRoot;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.ui.filters.NamePatternFilter;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.core.FileAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import com.simontuffs.eclipse.jarplug.JarPlugPlugin;
import com.simontuffs.eclipse.jarplug.popup.actions.UpdateArchive;

/**
 * JarPlug/E: Java Archive Plugin for Eclipse.
 */
public class JarPlug extends ViewPart implements ISelectionListener, IResourceChangeListener {

    protected TreeViewer viewer;

    protected DrillDownAdapter drillDownAdapter;

    protected Action updateAction, showHideDatesAction, expandAllAction, collapseAllAction;

    protected Action extractAction, closeAction;

    protected Action deleteAction, undeleteAction;

    protected IProject project;

    protected IPath currentJar;

    protected IProject currentProject;

    protected Map viewCache = new HashMap(), deletedMap = new HashMap();

    protected PackageExplorerPart explorer;

    public static class ViewCache {

        public ViewCache(TreeParent $node, Object[] $elements) {
            node = $node;
            elements = $elements;
        }

        public TreeParent node;

        public Object elements[];
    }

    ;

    protected boolean hideDate = true, expanded = false;

    public static final QualifiedName JARPLUG_JAR_PATH = new QualifiedName("jarplug", "archive.path");

    public static final String JARPLUG = "$jarplug.work";

    public static final String WORK = "$work";

    protected DateFormat df = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");

    protected static boolean debug = false;

    protected boolean extracting = false, filterRemoved = false;

    protected static JarPlug instance;

    public static JarPlug getInstance() {
        return instance;
    }

    protected class TreeObject implements IAdaptable {

        protected String name;

        protected Date date;

        protected long size;

        protected IPath zipPath;

        protected String entry;

        protected IProject project;

        protected TreeParent parent;

        protected boolean top, deleted;

        public TreeObject(String name) {
            this.name = name;
        }

        public TreeObject(String name, boolean $top) {
            this(name);
            top = $top;
        }

        public void setZipEntry(String $entry) {
            entry = $entry;
        }

        public String getZipEntryName() {
            return entry;
        }

        public void setProject(IProject $project) {
            project = $project;
        }

        public void setDeleted(boolean $deleted) {
            deleted = $deleted;
        }

        public boolean isDeleted() {
            return deleted || (parent != null && parent.isDeleted());
        }

        public boolean isConflicted() {
            if (!isDeleted() || zipPath == null) return false;
            int remove = zipPath.segment(1).equals(JARPLUG) ? 2 : 1;
            IPath path = zipPath.uptoSegment(1).append(JARPLUG).append(zipPath.removeFirstSegments(remove)).addFileExtension(WORK).append(entry);
            Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
            IResource file = workspace.newResource(path, IResource.FILE);
            IResource folder = workspace.newResource(path, IResource.FOLDER);
            return file.exists() || folder.exists();
        }

        public IProject getProject() {
            return project;
        }

        public String getName() {
            return name;
        }

        public void setParent(TreeParent parent) {
            this.parent = parent;
        }

        public TreeParent getParent() {
            return parent;
        }

        public String toString() {
            String kb = date == null || hideDate ? "" : " (" + size + " bytes)";
            return name + kb + (date == null || hideDate ? "" : "   " + df.format(date));
        }

        public Object getAdapter(Class key) {
            return null;
        }

        /**
         * @return Returns the zipPath.
         */
        public IPath getZipPath() {
            return zipPath;
        }

        /**
         * @param zipPath
         *            The zipPath to set.
         */
        public void setZipPath(IPath zipPath) {
            this.zipPath = zipPath;
            if (top) {
                ZipFile zip = null;
                try {
                    Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
                    IFile arch = (IFile) workspace.newResource(zipPath, IResource.FILE);
                    IPath location = arch.getLocation();
                    if (location == null) location = arch.getFullPath();
                    zip = new ZipFile(location.toFile());
                    name += " (" + zip.size() + " entries)";
                } catch (IOException iox) {
                } finally {
                    try {
                        if (zip != null) zip.close();
                    } catch (IOException iox) {
                    }
                }
            }
        }

        /**
         * @return Returns the topness.
         */
        public boolean isTop() {
            return top;
        }

        /**
         * @return Returns the date.
         */
        public Date getDate() {
            return date;
        }

        /**
         * @param date
         *            The date to set.
         */
        public void setDate(Date date) {
            this.date = date;
        }

        /**
         * @return Returns the size.
         */
        public long getSize() {
            return size;
        }

        /**
         * @param size The size to set.
         */
        public void setSize(long size) {
            this.size = size;
        }
    }

    class TreeParent extends TreeObject {

        protected ArrayList children;

        public TreeParent(String name) {
            super(name);
            children = new ArrayList();
        }

        public void addChild(TreeObject child) {
            children.add(child);
            child.setParent(this);
        }

        public void removeChild(TreeObject child) {
            children.remove(child);
            child.setParent(null);
        }

        public TreeObject[] getChildren() {
            return (TreeObject[]) children.toArray(new TreeObject[children.size()]);
        }

        public boolean hasChildren() {
            return children.size() > 0;
        }
    }

    protected TreeParent invisibleRoot;

    protected Map entryMap = new HashMap();

    class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }

        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                if (invisibleRoot == null) initialize();
                return getChildren(invisibleRoot);
            }
            return getChildren(parent);
        }

        public Object getParent(Object child) {
            if (child instanceof TreeObject) {
                return ((TreeObject) child).getParent();
            }
            return null;
        }

        public Object[] getChildren(Object parent) {
            if (parent instanceof TreeParent) {
                return ((TreeParent) parent).getChildren();
            }
            return new Object[0];
        }

        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeParent) return ((TreeParent) parent).hasChildren();
            return false;
        }

        protected void initialize() {
            invisibleRoot = new TreeParent("");
            invisibleRoot.addChild(new TreeParent("Please select a JAR file..."));
        }
    }

    class ViewLabelProvider extends LabelProvider {

        public String getText(Object obj) {
            return obj.toString();
        }

        public Image getImage(Object obj) {
            TreeObject node = (TreeObject) obj;
            if (node.isTop()) return null;
            String imageKey = ISharedImages.IMG_OBJ_FILE;
            if (node.isConflicted()) {
                imageKey = ISharedImages.IMG_TOOL_DELETE_DISABLED;
            } else if (node.isDeleted()) {
                imageKey = ISharedImages.IMG_TOOL_DELETE;
            } else {
                if (obj instanceof TreeParent) imageKey = ISharedImages.IMG_OBJ_FOLDER;
            }
            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
    }

    class NameSorter extends ViewerSorter {

        public int category(Object element) {
            if (element instanceof TreeParent) {
                if (!Character.isLetter(element.toString().charAt(0))) return 4;
                if (element.toString().startsWith("META")) return 2;
                return 3;
            } else {
                TreeObject node = (TreeObject) element;
                if (node.isTop()) return 0;
                return 1;
            }
        }
    }

    /**
     * The constructor.
     */
    public JarPlug() {
        if (instance == null) instance = this;
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        contributeToActionBars();
        hookResourcesChanged();
    }

    protected void hookResourcesChanged() {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        getViewSite().getPage().addSelectionListener(this);
        workspace.addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    protected void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {

            public void menuAboutToShow(IMenuManager manager) {
                JarPlug.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    protected void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    protected void fillLocalPullDown(IMenuManager manager) {
        manager.add(updateAction);
    }

    protected void fillContextMenu(IMenuManager manager) {
        manager.add(extractAction);
        manager.add(updateAction);
        manager.add(closeAction);
        manager.add(new Separator());
        manager.add(deleteAction);
        manager.add(undeleteAction);
        manager.add(new Separator());
        manager.add(expandAllAction);
        manager.add(collapseAllAction);
        manager.add(showHideDatesAction);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    protected void fillLocalToolBar(IToolBarManager manager) {
        manager.add(updateAction);
        manager.add(showHideDatesAction);
        manager.add(deleteAction);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    public IProject getCurrentProject() {
        return currentProject;
    }

    protected List copyFiles(Object nodes[], boolean copy) {
        List existingFiles = new ArrayList();
        try {
            IFolder folder = null;
            IFile lastFile = null;
            for (int n = 0; n < nodes.length; n++) {
                TreeObject node = (TreeObject) nodes[n];
                try {
                    IWorkbench workbench = PlatformUI.getWorkbench();
                    IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
                    Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
                    IWorkspaceRoot root = workspace.getRoot();
                    project = node.getProject();
                    if (project == null) return null;
                    IPath location = project.getLocation();
                    IEditorDescriptor editor = workbench.getEditorRegistry().getDefaultEditor(node.getName());
                    if (editor == null) editor = workbench.getEditorRegistry().getDefaultEditor("foo.txt");
                    IPath proj = root.getFullPath().append(project.getName()).append(JARPLUG);
                    IPath jarPath = node.getZipPath();
                    IPath relative = jarPath.removeFirstSegments(proj.segmentCount() - 1);
                    if (relative.segment(0).equals(JARPLUG)) relative = relative.removeFirstSegments(1);
                    IPath path = proj.append(relative);
                    if (!node.isTop() && !(node instanceof TreeParent)) path = path.append(node.getZipEntryName());
                    String segments[] = path.segments();
                    IPath oldDir = null;
                    IPath dir = proj;
                    folder = null;
                    mkdirs(proj.uptoSegment(1), proj);
                    for (int i = 2; i < segments.length; i++) {
                        if (!node.isTop() && !(node instanceof TreeParent) && i == segments.length - 1) break;
                        dir = dir.append(segments[i]);
                        IResource existing = workspace.newResource(dir, IResource.FILE);
                        if (node.getZipPath() != null && segments[i].equals(node.getZipPath().lastSegment())) {
                            dir = dir.addFileExtension(WORK);
                            folder = (IFolder) workspace.newResource(dir, IResource.FOLDER);
                            if (!folder.exists()) folder.create(true, true, null);
                            folder.setPersistentProperty(JARPLUG_JAR_PATH, node.getZipPath().toString());
                        } else {
                            folder = (IFolder) workspace.newResource(dir, IResource.FOLDER);
                            if (!folder.exists()) folder.create(true, true, null);
                        }
                    }
                    path = dir.append(path.lastSegment());
                    IFile archive = (IFile) workspace.newResource(jarPath, IResource.FILE);
                    if (debug) System.out.println("new ZipFile(" + archive.getLocation().lastSegment() + ")");
                    location = archive.getLocation();
                    if (location == null) location = archive.getFullPath();
                    ZipFile jar = new ZipFile(location.toFile());
                    try {
                        if (node instanceof TreeParent || node.isTop()) {
                            folder = (IFolder) workspace.newResource(dir, IResource.FOLDER);
                            if (!folder.exists()) folder.create(true, true, null);
                            ZipEntry entry = node.isTop() ? new ZipEntry("") : jar.getEntry(node.getZipEntryName());
                            if (entry == null || (!node.isTop() && !entry.isDirectory())) {
                                System.err.println("Error: " + node.getZipEntryName() + " should be a directory in " + jar.getName());
                            } else {
                                ProgressMonitorDialog progress = new ProgressMonitorDialog(getSite().getShell());
                                progress.setCancelable(true);
                                progress.open();
                                IProgressMonitor monitor = progress.getProgressMonitor();
                                int count = jar.size();
                                if (debug) System.out.println("Extracting " + count + " items");
                                monitor.beginTask("Extracting " + node.getZipPath(), count);
                                try {
                                    Enumeration entries = jar.entries();
                                    while (entries.hasMoreElements() && !monitor.isCanceled()) {
                                        ZipEntry source = (ZipEntry) entries.nextElement();
                                        if (!source.isDirectory() && source.getName().startsWith(entry.getName())) {
                                            if (!node.isTop()) {
                                                String residual = source.getName().substring(entry.getName().length());
                                                if (residual.indexOf('/') >= 0) continue;
                                            }
                                            IPath name = dir.append(source.getName());
                                            IFile resource = project.getFile(name.removeFirstSegments(1));
                                            if (debug) System.out.println(name.segments()[name.segmentCount() - 1]);
                                            java.io.File file = resource.getLocation().toFile();
                                            if (copy) {
                                                java.io.File parent = file.getParentFile();
                                                if (!parent.exists()) parent.mkdirs();
                                                copy(jar.getInputStream(source), file);
                                            } else if (file.exists()) {
                                                if (oldDir == null || !dir.equals(oldDir)) {
                                                    oldDir = dir;
                                                    existingFiles.add(dir);
                                                }
                                                existingFiles.add("    /" + source.getName());
                                            }
                                        }
                                        monitor.worked(1);
                                    }
                                } finally {
                                    monitor.done();
                                    progress.close();
                                }
                            }
                        } else {
                            IPath name = path;
                            boolean isArchive = false;
                            IFile file = (IFile) workspace.newResource(name, IResource.FILE);
                            if (copy) {
                                if (!file.exists()) {
                                    file.create(jar.getInputStream(new ZipEntry(node.getZipEntryName())), true, null);
                                } else {
                                    file.setContents(jar.getInputStream(new ZipEntry(node.getZipEntryName())), true, true, null);
                                }
                            } else if (file.exists()) {
                                if (oldDir == null || !dir.equals(oldDir)) {
                                    oldDir = dir;
                                    existingFiles.add(dir.removeLastSegments(1));
                                }
                                existingFiles.add("    /" + node.getZipEntryName());
                            }
                            lastFile = file;
                            try {
                                if (debug) System.out.println("new ZipFile(" + file.getLocation().lastSegment() + ")");
                                ZipFile tmp = new ZipFile(file.getLocation().toFile());
                                if (debug) System.out.println("close " + file.getLocation().lastSegment());
                                tmp.close();
                                isArchive = true;
                            } catch (ZipException zx) {
                            }
                            if (isArchive) {
                                buildView(file);
                            } else {
                                if (nodes.length == 1) IDE.openEditor(page, new FileEditorInput(file), editor.getId());
                            }
                        }
                    } finally {
                        try {
                            if (debug) System.out.println("close " + new java.io.File(jar.getName()).getName());
                            jar.close();
                        } catch (IOException iox) {
                        }
                    }
                } catch (Exception x) {
                    if (debug) System.out.println("Error: " + x);
                    x.printStackTrace();
                } finally {
                }
            }
            if (project != null) try {
                project.refreshLocal(IProject.DEPTH_INFINITE, null);
                if (explorer != null) {
                    if (lastFile != null) {
                        explorer.selectAndReveal(lastFile);
                    } else {
                        explorer.selectAndReveal(folder);
                    }
                }
            } catch (CoreException cx) {
            }
            viewer.refresh();
        } finally {
            extracting = false;
        }
        return existingFiles;
    }

    protected void makeActions() {
        ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
        updateAction = new Action() {

            public void runWithEvent(Event e) {
                if (currentJar != null) new UpdateArchive().update(currentJar.toString());
            }
        };
        updateAction.setText("Update Archive");
        updateAction.setToolTipText("Update Archive");
        updateAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_OBJS_TASK_TSK));
        showHideDatesAction = new Action() {

            public void run() {
                hideDate = !hideDate;
                viewer.refresh();
            }
        };
        showHideDatesAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_DEF_VIEW));
        showHideDatesAction.setText("Show/Hide Dates/Sizes");
        showHideDatesAction.setToolTipText("Show/Hide Dates/Sizes");
        expandAllAction = new Action() {

            public void run() {
                viewer.expandAll();
            }
        };
        expandAllAction.setText("Expand All");
        ImageDescriptor sync = ImageDescriptor.createFromURL(JarPlugPlugin.getPlugin().getBundle().getEntry("icons/e_synch_nav.gif"));
        expandAllAction.setImageDescriptor(sync);
        collapseAllAction = new Action() {

            public void run() {
                viewer.collapseAll();
            }
        };
        collapseAllAction.setText("Collapse All");
        collapseAllAction.setImageDescriptor(sync);
        deleteAction = new Action() {

            public void run() {
                StructuredSelection selection = (StructuredSelection) viewer.getSelection();
                Object selections[] = selection.toArray();
                for (int i = 0; i < selections.length; i++) {
                    TreeObject node = (TreeObject) selections[i];
                    node.setDeleted(true);
                    Map nodes = (Map) deletedMap.get(currentJar);
                    nodes.put(node.getZipEntryName(), node);
                    if (debug) System.out.println("Marked " + node.getZipEntryName() + " for deletion");
                }
                viewer.refresh();
            }
        };
        deleteAction.setText("Delete");
        deleteAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
        undeleteAction = new Action() {

            public void run() {
                StructuredSelection selection = (StructuredSelection) viewer.getSelection();
                Object selections[] = selection.toArray();
                for (int i = 0; i < selections.length; i++) {
                    TreeObject node = (TreeObject) selections[i];
                    node.setDeleted(false);
                    Map nodes = (Map) deletedMap.get(currentJar);
                    nodes.remove(node.getZipEntryName());
                }
                viewer.refresh();
            }
        };
        undeleteAction.setText("Undelete");
        undeleteAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
        extractAction = new Action() {

            public void run() {
                ISelection selection = viewer.getSelection();
                Object nodes[] = ((IStructuredSelection) selection).toArray();
                extracting = true;
                IProject project = null;
                List existingFiles = copyFiles(nodes, false);
                boolean overwrite = true;
                if (existingFiles != null && existingFiles.size() > 0) {
                    overwrite = queryOverwrite(existingFiles);
                }
                copyFiles(nodes, overwrite);
            }
        };
        extractAction.setText("Extract to " + JARPLUG);
        URL url = JarPlugPlugin.getPlugin().getBundle().getEntry("icons/openFolder.gif");
        extractAction.setImageDescriptor(ImageDescriptor.createFromURL(url));
        closeAction = new Action() {

            public void run() {
                System.out.println("close " + currentJar);
                viewCache.remove(currentJar);
                currentJar = null;
                currentProject = null;
                refresh(null);
            }
        };
        closeAction.setText("Close Jar File");
    }

    protected void copy(InputStream input, java.io.File file) throws IOException {
        FileOutputStream output = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        int len = 0;
        while ((len = input.read(buf)) > 0) {
            output.write(buf, 0, len);
        }
        input.close();
        output.close();
    }

    protected boolean queryOverwrite(final List files) {
        MessageDialog dialog = new MessageDialog(new Shell(), "Overwrite existing file in $jarplug tree?", null, "The following files are already present in the workspace under the " + JARPLUG + " directory. " + "Do you wish to overwrite them?", MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0) {

            protected Control createCustomArea(Composite parent) {
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.widthHint = 400;
                gd.heightHint = 300;
                Text text = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
                text.setEditable(false);
                text.setLayoutData(gd);
                for (int i = 0; i < files.size(); i++) {
                    text.append(files.get(i) + "\n");
                }
                return text;
            }
        };
        int result = dialog.open();
        return result == MessageDialog.OK;
    }

    protected void mkdirs(IPath folder, IPath subfolder) throws CoreException {
        Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
        subfolder = subfolder.removeFirstSegments(folder.segmentCount());
        IFolder f = null;
        for (int i = 0; i < subfolder.segmentCount(); i++) {
            f = ((IFolder) workspace.newResource(folder.append(subfolder.uptoSegment(i + 1)), IResource.FOLDER));
            if (!f.exists()) f.create(true, true, null);
        }
    }

    protected boolean isExpanded;

    protected void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection sel = (IStructuredSelection) event.getSelection();
                    TreeObject node = (TreeObject) sel.getFirstElement();
                    if (node instanceof TreeParent) {
                        TreeViewer viewer = (TreeViewer) event.getSource();
                        if (viewer.getExpandedState(node)) {
                            viewer.collapseToLevel(node, TreeViewer.ALL_LEVELS);
                        } else {
                            viewer.expandToLevel(node, TreeViewer.ALL_LEVELS);
                        }
                    } else if (!node.isTop()) {
                        extractAction.run();
                        viewer.refresh(node);
                    } else {
                        if (isExpanded) {
                            viewer.collapseAll();
                            isExpanded = false;
                        } else {
                            viewer.expandAll();
                            isExpanded = true;
                        }
                    }
                }
                if (debug) System.out.println("doubleClick: " + event);
            }
        });
    }

    protected void showMessage(String message) {
        MessageDialog.openInformation(viewer.getControl().getShell(), "Jar View", message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public void selectionChanged(IWorkbenchPart part, ISelection $selection) {
        if (debug) System.out.println("Selection Changed: " + part + " " + $selection);
        if (part instanceof PackageExplorerPart && !filterRemoved) {
            filterRemoved = true;
            explorer = (PackageExplorerPart) part;
            IViewSite view = explorer.getViewSite();
            if (debug) System.out.println("view=" + view);
            StructuredViewer viewer = explorer.getTreeViewer();
            ViewerFilter filters[] = viewer.getFilters();
            if (debug) System.out.println(Arrays.asList(filters));
            if (filters[0] instanceof NamePatternFilter) {
                NamePatternFilter filter = (NamePatternFilter) filters[0];
                String patterns[] = filter.getPatterns();
                ArrayList newpatterns = new ArrayList();
                String inner = "*$*.class";
                for (int i = 0; i < patterns.length; i++) {
                    if (!patterns[i].equals(inner)) newpatterns.add(patterns[i]);
                }
                filter.setPatterns((String[]) newpatterns.toArray(new String[] {}));
            }
        }
        if (!($selection instanceof IStructuredSelection)) {
            return;
        }
        try {
            IStructuredSelection selection = (IStructuredSelection) $selection;
            Object sel = selection.getFirstElement();
            buildView(sel);
        } catch (Exception e) {
            if (debug) System.out.println("Error: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Is the entry, or its parent, for the current jar file marked for deletion?
     * @param entry
     * @return
     */
    public boolean isDeleted(String entry) {
        Map nodes = (Map) deletedMap.get(currentJar);
        if (nodes == null) return false;
        TreeObject node = (TreeObject) nodes.get(entry);
        if (node == null) {
            int last = entry.lastIndexOf('/');
            if (last < 0) return false;
            String prefix = entry.substring(0, last + 1);
            node = (TreeObject) nodes.get(prefix);
        }
        return node != null && node.isDeleted();
    }

    public void refresh(IResource target) {
        try {
            viewCache.remove(currentJar);
            currentJar = null;
            currentProject = null;
            buildView(target);
            if (explorer != null && !extracting) {
                explorer.selectAndReveal(target);
            }
        } catch (Exception x) {
        }
        viewer.getTree().getDisplay().asyncExec(new Runnable() {

            public void run() {
                viewer.refresh();
            }
        });
    }

    protected void buildView(Object sel) throws CoreException, IOException {
        Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IProject project = null;
        if (sel == null && currentJar != null) {
            sel = workspace.newResource(currentJar, IResource.FILE);
        }
        if (sel instanceof JavaElement) {
            project = ((IJavaProject) ((JavaElement) sel).getJavaProject()).getProject();
        } else if (sel instanceof IFile) {
            project = ((IFile) sel).getProject();
        }
        if (sel == null) {
            invisibleRoot = null;
            if (viewer != null) viewer.refresh();
            return;
        } else if (sel instanceof IFolder) {
            IFolder folder = (IFolder) sel;
            if (!folder.isLocal(IFolder.DEPTH_ZERO)) return;
            String jarPath = folder.getPersistentProperty(JARPLUG_JAR_PATH);
            if (jarPath != null) {
                IPath path = folder.getProject().getWorkspace().getRoot().getFullPath().append(jarPath);
                sel = workspace.getRoot().getFile(path);
                project = folder.getProject();
            } else {
                if (debug) System.out.println("Unable to locate persisted information " + JARPLUG_JAR_PATH + " for JAR workspace " + folder);
            }
        }
        IPath jarPath = null;
        if (sel instanceof IFile) {
            IFile file = (IFile) sel;
            jarPath = file.getFullPath();
        } else if (sel instanceof JarPackageFragmentRoot) {
            JarPackageFragmentRoot frag = (JarPackageFragmentRoot) sel;
            jarPath = frag.getPath();
        } else if (sel instanceof FileAdapter) {
            java.io.File file = ((FileAdapter) sel).getFile();
            jarPath = root.getLocation().uptoSegment(0).append(file.getPath());
            System.out.println("PDE: jarPath=" + jarPath);
        }
        if (debug) {
            System.out.println();
            System.out.println("buildView currentJar=" + currentJar + " jarPath=" + jarPath);
        }
        ViewCache cache = (ViewCache) viewCache.get(currentJar);
        if (cache != null) {
            cache.elements = viewer.getExpandedElements();
        }
        ZipFile jar = null;
        if (jarPath != null) {
            IFile file = (IFile) workspace.newResource(jarPath, IResource.FILE);
            try {
                if (debug && file.getLocation() != null) System.out.println("new ZipFile(" + file.getLocation().lastSegment() + ")");
                IPath location = file.getLocation();
                if (location == null) location = file.getFullPath();
                jar = new ZipFile(location.toFile());
            } catch (ZipException zx) {
                if (debug) System.err.println(zx + " " + file.getLocation());
                invisibleRoot = null;
                viewer.refresh();
                return;
            }
        }
        if (jar != null) {
            if (debug) System.out.println("buildView(" + sel + ")");
            cache = (ViewCache) viewCache.get(jarPath);
            if (cache != null && new java.io.File(jar.getName()).lastModified() > cache.node.date.getTime()) {
                viewCache.remove(jarPath);
                cache = null;
            }
            if (cache != null) {
                invisibleRoot = cache.node;
                viewer.refresh();
                viewer.setExpandedElements(cache.elements);
                currentJar = jarPath;
                currentProject = project;
                return;
            }
        }
        if (jar != null) {
            Enumeration<? extends ZipEntry> _enum = jar.entries();
            TreeParent parent = new TreeParent(jar.getName());
            parent.setDate(new Date(new java.io.File(jar.getName()).lastModified()));
            invisibleRoot = parent;
            deletedMap.put(jarPath, new HashMap());
            currentJar = jarPath;
            currentProject = project;
            TreeObject top = new TreeObject(jarPath.toString(), true);
            top.setProject(project);
            top.setZipPath(jarPath);
            invisibleRoot.addChild(top);
            entryMap.clear();
            while (_enum.hasMoreElements()) {
                ZipEntry entry = _enum.nextElement();
                String name = entry.getName();
                int last = name.lastIndexOf("/");
                String prefix = "", suffix = name;
                if (last >= 0) {
                    prefix = name.substring(0, last);
                    suffix = name.substring(last + 1);
                }
                TreeParent dir = (TreeParent) entryMap.get(prefix);
                if (dir == null) {
                    if (prefix.length() > 0) {
                        dir = new TreeParent(prefix);
                        dir.setProject(project);
                        dir.setZipEntry(prefix + "/");
                        dir.setZipPath(jarPath);
                        entryMap.put(prefix, dir);
                        invisibleRoot.addChild(dir);
                    } else {
                        dir = invisibleRoot;
                    }
                }
                if (suffix.length() > 0) {
                    TreeObject node = new TreeObject(suffix);
                    node.setZipEntry(entry.getName());
                    node.setZipPath(jarPath);
                    node.setProject(project);
                    node.setDate(new Date(entry.getTime()));
                    node.setSize(entry.getSize());
                    dir.addChild(node);
                }
            }
            Iterator iter = entryMap.values().iterator();
            while (iter.hasNext()) {
                TreeParent dir = (TreeParent) iter.next();
                if (!dir.hasChildren()) invisibleRoot.removeChild(dir);
                entryMap.remove(dir);
            }
            viewCache.put(jarPath, new ViewCache(invisibleRoot, new Object[] {}));
            viewer.refresh();
            if (debug) System.out.println("close " + new java.io.File(jar.getName()).getName());
            jar.close();
        }
    }

    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta children[] = event.getDelta().getAffectedChildren();
        if (children.length == 0) return;
        IResourceDelta child = children[0];
        while (child.getAffectedChildren().length > 0) {
            child = child.getAffectedChildren()[0];
        }
        IResource resource = child.getResource();
        if (debug) System.out.println("resourceChanged(" + resource + ")");
        if (resource.getFullPath().equals(currentJar)) refresh(resource);
    }

    public void dispose() {
        super.dispose();
        viewer = null;
    }
}
