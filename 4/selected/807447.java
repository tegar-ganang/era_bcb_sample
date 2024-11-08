package org.designerator.media.actions;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.designerator.common.eclipse.WorkspaceUtil;
import org.designerator.common.wizards.CustomNewFolderResourceWizard;
import org.designerator.image.algo.util.ImageUtils;
import org.designerator.media.MediaPlugin;
import org.designerator.media.image.util.IO;
import org.designerator.media.util.ImageHelper;
import org.designerator.media.importWizards.MediaImportWizard;
import org.designerator.media.thumbs.ThumbFileUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class LinkFolderAction extends Action implements IWorkbenchWindowActionDelegate, IObjectActionDelegate {

    public static final String CD = "isCD";

    public static final String ID = "org.designerator.media.folder";

    private IStructuredSelection selection;

    ISelectionProvider provider;

    private Shell shell;

    public LinkFolderAction() {
        super();
        setText(Messages.LinkFolderAction_title);
        setToolTipText(Messages.LinkFolderAction_titletip);
        setImageDescriptor(ImageHelper.createImageDescriptor(null, "/icons/importdir_wiz.gif"));
    }

    public LinkFolderAction(ISelectionProvider provider, Shell shell) {
        super();
        setText(Messages.LinkFolderAction_title);
        setToolTipText(Messages.LinkFolderAction_titletip);
        setImageDescriptor(ImageHelper.createImageDescriptor(null, "/icons/importdir_wiz.gif"));
        init(provider, shell);
    }

    public static void copyThumbs(IFolder folder) {
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

    @Override
    public void dispose() {
    }

    public void init(ISelectionProvider provider, Shell shell) {
        this.provider = provider;
        this.shell = shell;
    }

    @Override
    public void init(IWorkbenchWindow window) {
        shell = window.getShell();
        init(null, shell);
    }

    public void run() {
        updateSelection();
        CustomNewFolderResourceWizard wizard = new CustomNewFolderResourceWizard(provider, Messages.LinkFolderAction_addfolder, Messages.LinkFolderAction_addfoldertip);
        wizard.setContainerPath(MediaPlugin.DEFAULT_IMAGE_PROJECT_ID);
        wizard.init(PlatformUI.getWorkbench(), selection);
        wizard.setNeedsProgressMonitor(true);
        WizardDialog dialog = new WizardDialog(shell, wizard);
        dialog.create();
        dialog.getShell().setText(Messages.LinkFolderAction_shelltext);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IIDEHelpContextIds.NEW_FOLDER_WIZARD);
        int open = dialog.open();
    }

    public void setSelection(IStructuredSelection selection) {
        this.selection = selection;
    }

    @Override
    public void run(IAction action) {
        run();
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            this.selection = (IStructuredSelection) selection;
        }
    }

    private void updateSelection() {
        if (provider != null) {
            selection = (IStructuredSelection) provider.getSelection();
            if (selection != null) {
                return;
            }
        }
        if (selection == null) {
            IWorkbenchPage activePage;
            try {
                activePage = WorkspaceUtil.getActivePage();
                IViewPart view = activePage.findView("org.eclipse.ui.navigator.ProjectExplorer");
                if (view != null) {
                    ProjectExplorer view2 = (ProjectExplorer) view;
                    ISelection selection2 = view2.getCommonViewer().getSelection();
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
    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }
}
