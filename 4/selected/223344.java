package org.cubictest.ui.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.io.FileUtils;
import org.cubictest.CubicTestPlugin;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.views.navigator.ResourceNavigator;

public class NewCubicTestProjectWizard extends Wizard implements INewWizard {

    private WizardNewProjectCreationPage namePage;

    private NewProjectSummaryPage summaryPage;

    private IWorkbench workbench;

    public NewCubicTestProjectWizard() {
        super();
        setNeedsProgressMonitor(true);
    }

    public void addPages() {
        super.addPages();
        namePage = new WizardNewProjectCreationPage("newCubicTestProjectNamepage");
        namePage.setTitle("New CubicTest project");
        namePage.setDescription("Choose name of project");
        summaryPage = new NewProjectSummaryPage();
        summaryPage.setTitle("New CubicTest project");
        summaryPage.setDescription("Ready to create project");
        addPage(namePage);
        addPage(summaryPage);
    }

    public boolean performFinish() {
        try {
            getContainer().run(false, false, new WorkspaceModifyOperation(null) {

                protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
                    createProject(monitor, summaryPage.getCreateTestOnFinish());
                }
            });
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void createProject(IProgressMonitor monitor, boolean launchNewTestWizard) {
        try {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = root.getProject(namePage.getProjectName());
            IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
            if (!Platform.getLocation().equals(namePage.getLocationPath())) description.setLocation(namePage.getLocationPath());
            description.setNatureIds(new String[] { JavaCore.NATURE_ID });
            ICommand buildCommand = description.newCommand();
            buildCommand.setBuilderName(JavaCore.BUILDER_ID);
            description.setBuildSpec(new ICommand[] { buildCommand });
            project.create(description, monitor);
            project.open(monitor);
            IJavaProject javaProject = JavaCore.create(project);
            IFolder testFolder = project.getFolder("tests");
            testFolder.create(false, true, monitor);
            IFolder srcFolder = project.getFolder("src");
            srcFolder.create(false, true, monitor);
            IFolder binFolder = project.getFolder("bin");
            binFolder.create(false, true, monitor);
            IFolder libFolder = project.getFolder("lib");
            libFolder.create(false, true, monitor);
            try {
                FileUtils.copyFile(new Path(Platform.asLocalURL(CubicTestPlugin.getDefault().find(new Path("lib/CubicTestElementAPI.jar"))).getPath()).toFile(), libFolder.getFile("CubicTestElementAPI.jar").getLocation().toFile());
                FileUtils.copyFile(new Path(Platform.asLocalURL(CubicTestPlugin.getDefault().find(new Path("lib/CubicUnit.jar"))).getPath()).toFile(), libFolder.getFile("CubicUnit.jar").getLocation().toFile());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            javaProject.setOutputLocation(binFolder.getFullPath(), monitor);
            IClasspathEntry[] classpath;
            classpath = new IClasspathEntry[] { JavaCore.newSourceEntry(srcFolder.getFullPath()), JavaCore.newContainerEntry(new Path("org.eclipse.jdt.launching.JRE_CONTAINER")), JavaCore.newLibraryEntry(libFolder.getFile("CubicTestElementAPI.jar").getFullPath(), null, null), JavaCore.newLibraryEntry(libFolder.getFile("CubicUnit.jar").getFullPath(), null, null) };
            javaProject.setRawClasspath(classpath, binFolder.getFullPath(), monitor);
            ResourceNavigator navigator = null;
            IViewPart viewPart = workbench.getActiveWorkbenchWindow().getActivePage().getViewReferences()[0].getView(false);
            if (viewPart instanceof ResourceNavigator) {
                navigator = (ResourceNavigator) viewPart;
            }
            if (launchNewTestWizard) {
                launchNewTestWizard(testFolder);
                if (navigator != null && testFolder.members().length > 0) {
                    navigator.selectReveal(new StructuredSelection(testFolder.members()[0]));
                }
            }
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        this.workbench = workbench;
    }

    public void launchNewTestWizard(IFolder testFolder) {
        NewTestWizard wiz = new NewTestWizard();
        wiz.init(workbench, new StructuredSelection(testFolder));
        WizardDialog dialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wiz);
        dialog.open();
    }

    public boolean canFinish() {
        if (getContainer().getCurrentPage() instanceof NewProjectSummaryPage) {
            return true;
        }
        return false;
    }
}
