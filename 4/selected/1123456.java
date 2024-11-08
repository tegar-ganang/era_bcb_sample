package org.larz.dom3.dm.ui.editor;

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.ui.URIEditorInput;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.xtext.ui.editor.XtextEditor;

public class DmXtextEditor extends XtextEditor {

    protected static final String AUTOLINK_PROJECT_NAME = "_LinkedModFiles_";

    protected static final String ENCODING_UTF8 = "utf-8";

    private String path;

    public DmXtextEditor() {
        super();
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof XtextEditor) == false) return false;
        if (getEditorInput() == null || ((XtextEditor) obj).getEditorInput() == null) return false;
        java.net.URI u1 = ((IURIEditorInput) getEditorInput()).getURI();
        java.net.URI u2 = ((IURIEditorInput) ((XtextEditor) obj).getEditorInput()).getURI();
        if (u1 == null || u2 == null) return false;
        return u1.toString().equals(u2.toString());
    }

    /**
	 * Here it comes the part that handles external files
	 * (from outside the workspace)
	 *
	 */
    private static void createLink(IProject project, IFile linkFile, java.net.URI uri) throws CoreException {
        IPath path = linkFile.getFullPath();
        IPath folders = path.removeLastSegments(1).removeFirstSegments(1);
        IPath checkPath = Path.ROOT;
        int segmentCount = folders.segmentCount();
        for (int i = 0; i < segmentCount; i++) {
            checkPath = checkPath.addTrailingSeparator().append(folders.segment(i));
            IFolder folder = project.getFolder(checkPath);
            if (!folder.exists()) folder.create(true, true, null);
        }
        linkFile.createLink(uri, IResource.ALLOW_MISSING_LOCAL, null);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        MenuManager manager = new MenuManager();
        manager.setRemoveAllWhenShown(true);
        manager.addMenuListener(new IMenuListener() {

            @Override
            public void menuAboutToShow(IMenuManager manager) {
                manager.removeAll();
                manager.add(new Separator(ITextEditorActionConstants.GROUP_UNDO));
                manager.add(new GroupMarker(ITextEditorActionConstants.GROUP_SAVE));
                manager.add(new Separator(ITextEditorActionConstants.GROUP_COPY));
                manager.add(new Separator(ITextEditorActionConstants.GROUP_SETTINGS));
                if (isEditable()) {
                    addAction(manager, ITextEditorActionConstants.GROUP_UNDO, ITextEditorActionConstants.UNDO);
                    addAction(manager, ITextEditorActionConstants.GROUP_UNDO, ITextEditorActionConstants.REVERT_TO_SAVED);
                    addAction(manager, ITextEditorActionConstants.GROUP_SAVE, ITextEditorActionConstants.SAVE);
                    addAction(manager, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.CUT);
                    addAction(manager, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
                    addAction(manager, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.PASTE);
                } else {
                    addAction(manager, ITextEditorActionConstants.GROUP_COPY, ITextEditorActionConstants.COPY);
                }
                IAction preferencesAction = getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES);
                manager.appendToGroup(ITextEditorActionConstants.GROUP_SETTINGS, preferencesAction);
            }
        });
        StyledText styledText = getSourceViewer().getTextWidget();
        styledText.setMenu(manager.createContextMenu(styledText));
    }

    private IFile getWorkspaceFile(IFileStore fileStore) {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IFile[] files = workspaceRoot.findFilesForLocationURI(fileStore.toURI());
        if (files != null && files.length == 1) return files[0];
        return null;
    }

    /**
	 * Translates an incoming IEditorInput being an FilestoreEditorInput, or IURIEditorInput
	 * that is not also a IFileEditorInput.
	 * FilestoreEditorInput is used when opening external files in an IDE environment.
	 * The result is that the regular XtextEditor gets an IEFSEditorInput which is also an
	 * IStorageEditorInput.
	 */
    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        if (input instanceof IURIEditorInput && !(input instanceof IFileEditorInput)) {
            java.net.URI uri = ((IURIEditorInput) input).getURI();
            path = uri.getPath();
            String name = ((IURIEditorInput) input).getName();
            if (uri.getScheme().equals("file")) {
                IFile linkedFile = obtainLink(uri);
                IFileEditorInput linkedInput = new LinkedFileEditorInput(linkedFile);
                super.init(site, linkedInput);
            } else {
                URIEditorInput uriInput = new URIEditorInput(URI.createURI(uri.toString()), name);
                super.init(site, uriInput);
            }
            return;
        } else if (input instanceof IFileEditorInput) {
            path = ((IFileEditorInput) input).getFile().getLocation().toString();
        }
        super.init(site, input);
    }

    public String getPath() {
        return path;
    }

    /**
	 * Throws WrappedException - the URI must refer to an existing file!
	 *
	 * @param uri
	 * @return
	 */
    public static IFile obtainLink(java.net.URI uri) {
        try {
            IWorkspace ws = ResourcesPlugin.getWorkspace();
            IProject project = ws.getRoot().getProject(AUTOLINK_PROJECT_NAME);
            boolean newProject = false;
            if (!project.exists()) {
                project.create(null);
                newProject = true;
            }
            if (!project.isOpen()) {
                project.open(null);
            }
            if (newProject) project.setDefaultCharset(ENCODING_UTF8, new NullProgressMonitor());
            IFile linkFile = project.getFile(uri.getPath());
            if (linkFile.exists()) linkFile.refreshLocal(1, null); else {
                createLink(project, linkFile, uri);
            }
            return linkFile;
        } catch (CoreException e) {
            throw new WrappedException(e);
        }
    }

    @Override
    protected void performSaveAs(IProgressMonitor progressMonitor) {
        Shell shell = getSite().getShell();
        final IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput && ((IFileEditorInput) input).getFile().isLinked() && ((IFileEditorInput) input).getFile().getProject().getName().equals(AUTOLINK_PROJECT_NAME)) {
            final IEditorInput newInput;
            IDocumentProvider provider = getDocumentProvider();
            FileDialog dialog = new FileDialog(shell, SWT.SAVE);
            IPath oldPath = URIUtil.toPath(((IURIEditorInput) input).getURI());
            if (oldPath != null) {
                dialog.setFileName(oldPath.lastSegment());
                dialog.setFilterPath(oldPath.toOSString());
            }
            dialog.setFilterExtensions(new String[] { "*.dm", "*.*" });
            String path = dialog.open();
            if (path == null) {
                if (progressMonitor != null) progressMonitor.setCanceled(true);
                return;
            }
            final File localFile = new File(path);
            if (localFile.exists()) {
                MessageDialog overwriteDialog = new MessageDialog(shell, "File Exists", null, path + " already exists. Do you wish to overwrite? ", MessageDialog.WARNING, new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 1);
                if (overwriteDialog.open() != Window.OK) {
                    if (progressMonitor != null) {
                        progressMonitor.setCanceled(true);
                        return;
                    }
                }
            }
            IFileStore fileStore;
            try {
                fileStore = EFS.getStore(localFile.toURI());
            } catch (CoreException ex) {
                MessageDialog.openError(shell, "Error", "Couldn't write file. " + ex.getMessage());
                return;
            }
            IFile file = getWorkspaceFile(fileStore);
            if (file != null) newInput = new FileEditorInput(file); else {
                IURIEditorInput uriInput = new FileStoreEditorInput(fileStore);
                java.net.URI uri = uriInput.getURI();
                IFile linkedFile = obtainLink(uri);
                newInput = new FileEditorInput(linkedFile);
            }
            if (provider == null) {
                return;
            }
            boolean success = false;
            try {
                provider.aboutToChange(newInput);
                provider.saveDocument(progressMonitor, newInput, provider.getDocument(input), true);
                success = true;
            } catch (CoreException x) {
                final IStatus status = x.getStatus();
                if (status == null || status.getSeverity() != IStatus.CANCEL) {
                    MessageDialog.openError(shell, "Error", "Couldn't write file. " + x.getMessage());
                }
            } finally {
                provider.changed(newInput);
                if (success) setInput(newInput);
            }
            if (progressMonitor != null) progressMonitor.setCanceled(!success);
            return;
        }
        super.performSaveAs(progressMonitor);
    }
}
