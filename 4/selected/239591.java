package org.designerator.media.actions;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.designerator.common.eclipse.WorkspaceUtil;
import org.designerator.common.wizards.CustomNewFolderResourceWizard;
import org.designerator.media.image.util.IO;
import org.designerator.media.importWizards.MediaImportWizard;
import org.designerator.media.thumbs.ThumbFileUtils;
import org.designerator.media.util.ImageHelper;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

public class ImportFolderAction extends Action implements IWorkbenchWindowActionDelegate {

    private IStructuredSelection selection;

    ISelectionProvider provider;

    private Shell shell;

    private IWorkbenchWindow window;

    public ImportFolderAction() {
        super();
    }

    public ImportFolderAction(ISelectionProvider provider, Shell shell) {
        super();
        System.out.println("ImportFolderAction.ImportFolderAction():" + shell);
        this.provider = provider;
        this.shell = shell;
        init();
    }

    public void init() {
        setText(Messages.ImportFolderAction_title);
        setToolTipText("Import");
        setImageDescriptor(ImageHelper.createImageDescriptor(null, "/icons/media-optical.png"));
    }

    public void run() {
        updateSelection();
        IStructuredSelection selection2 = null;
        if (provider == null) {
            selection2 = (IStructuredSelection) window.getSelectionService().getSelection();
            if (selection2 == null) {
                selection2 = new StructuredSelection(new Object());
            }
        } else {
            selection2 = (IStructuredSelection) provider.getSelection();
        }
        MediaImportWizard wizard = new MediaImportWizard(selection2);
        wizard.init(PlatformUI.getWorkbench(), selection2);
        wizard.setNeedsProgressMonitor(true);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.create();
        dialog.getShell().setText(IDEWorkbenchMessages.CreateFolderAction_title);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IIDEHelpContextIds.NEW_FOLDER_WIZARD);
        System.out.println("ImportFolderAction.run()2:");
        dialog.open();
    }

    public void buildThumbs(CustomNewFolderResourceWizard wizard) {
        IFolder folder = wizard.getFolder();
        if (folder != null && folder.exists()) {
            IPath cache = ThumbFileUtils.getExplorerThumbContainer(Path.fromPortableString(folder.getName()));
            IPath tcache = ThumbFileUtils.getDefaultThumbContainer(folder.getFullPath());
            if (cache != null && tcache != null) {
                final File dir = cache.toFile();
                final File dir2 = tcache.toFile();
                final File[] thumbs = dir.listFiles();
                if (dir != null && dir.exists() && dir2 != null && dir2.exists() && thumbs != null && thumbs.length > 0) {
                    Job j = new Job("Copy Cache") {

                        @Override
                        protected IStatus run(IProgressMonitor monitor) {
                            for (int i = 0; i < thumbs.length; i++) {
                                if (IO.isValidImageFile(thumbs[i].getName())) {
                                    try {
                                        FileUtils.copyFileToDirectory(thumbs[i], dir2);
                                        thumbs[i].delete();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            return Status.OK_STATUS;
                        }
                    };
                    j.setSystem(true);
                    j.schedule(500);
                }
            }
        }
    }

    private void updateSelection() {
        if (selection == null) {
            if (provider != null) {
                selection = (IStructuredSelection) provider.getSelection();
                if (selection != null) {
                    return;
                }
            }
            IWorkbenchPage activePage;
            try {
                activePage = WorkspaceUtil.getActivePage();
                IViewPart view = activePage.findView("org.eclipse.ui.navigator.ProjectExplorer");
                if (view != null) {
                    ISelection selection2 = ((ProjectExplorer) view).getCommonViewer().getSelection();
                    this.selection = (StructuredSelection) selection2;
                } else {
                    this.selection = new StructuredSelection(new Object());
                }
            } catch (WorkbenchException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    public void run(IAction action) {
        run();
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void init(IWorkbenchWindow window) {
        this.window = window;
        shell = window.getShell();
    }

    public void init(ISelectionProvider provider, Shell shell2) {
    }
}
