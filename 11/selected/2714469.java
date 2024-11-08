package fr.macymed.eclipse.plugin;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import fr.macymed.eclipse.plugin.classpath.ModuloClasspathContainer;
import fr.macymed.modulo.framework.module.VersionNumber;
import fr.macymed.modulo.platform.archive.ModuleDefinition;

public class ModuloProjectCreationWizard extends NewElementWizard implements IExecutableExtension {

    private ModuloProjectCreationWizardPage fModuloPage;

    private WizardNewProjectCreationPage fMainPage;

    private IConfigurationElement fConfigElement;

    protected IProject newProject;

    /**
     * <p>
     * Creates a new ModuloProjectCreationWizard.
     * </p>
     */
    public ModuloProjectCreationWizard() {
        super();
        ImageDescriptor banner = this.getBannerImg();
        if (banner != null) setDefaultPageImageDescriptor(banner);
        setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
        setWindowTitle(Messages.getString("ModuloProjectCreationWizard.Title"));
    }

    /**
     * @see org.eclipse.jface.wizard.IWizard#canFinish()
     * @return
     */
    public boolean canFinish() {
        return true;
    }

    private ImageDescriptor getBannerImg() {
        try {
            URL prefix = new URL(ModuloLauncherPlugin.getDefault().getDescriptor().getInstallURL(), "icons/");
            return ImageDescriptor.createFromURL(new URL(prefix, "modulo_wiz.gif"));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages() {
        super.addPages();
        fMainPage = new WizardNewProjectCreationPage("Page 1");
        fMainPage.setTitle(Messages.getString("ModuloProjectCreationWizard.Page1Title"));
        fMainPage.setDescription(Messages.getString("ModuloProjectCreationWizard.Page1Description"));
        addPage(fMainPage);
        fModuloPage = new ModuloProjectCreationWizardPage("NewModuloProjectPage");
        addPage(fModuloPage);
    }

    /**
     * @see org.eclipse.jface.wizard.IWizard#performFinish()
     * @return
     */
    public boolean performFinish() {
        IRunnableWithProgress projectCreationOperation = new WorkspaceModifyDelegatingOperation(getProjectCreationRunnable());
        try {
            getContainer().run(false, true, projectCreationOperation);
        } catch (Exception e) {
            ModuloLauncherPlugin.log(e);
            return false;
        }
        BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
        selectAndReveal(newProject);
        return true;
    }

    protected IRunnableWithProgress getProjectCreationRunnable() {
        return new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                int remainingWorkUnits = 10;
                monitor.beginTask("New Modulo Project Creation", remainingWorkUnits);
                IWorkspace ws = ResourcesPlugin.getWorkspace();
                newProject = fMainPage.getProjectHandle();
                IProjectDescription description = ws.newProjectDescription(newProject.getName());
                String[] natures = { JavaCore.NATURE_ID, ModuloLauncherPlugin.NATURE_ID };
                description.setNatureIds(natures);
                ICommand command = description.newCommand();
                command.setBuilderName(JavaCore.BUILDER_ID);
                ICommand[] commands = { command };
                description.setBuildSpec(commands);
                IJavaProject jproject = JavaCore.create(newProject);
                ModuloProject modProj = new ModuloProject();
                modProj.setJavaProject(jproject);
                try {
                    newProject.create(description, new SubProgressMonitor(monitor, 1));
                    newProject.open(new SubProgressMonitor(monitor, 1));
                    IFolder srcFolder = newProject.getFolder("src");
                    IFolder javaFolder = srcFolder.getFolder("java");
                    IFolder buildFolder = newProject.getFolder("build");
                    IFolder classesFolder = buildFolder.getFolder("classes");
                    modProj.createFolder(srcFolder);
                    modProj.createFolder(javaFolder);
                    modProj.createFolder(buildFolder);
                    modProj.createFolder(classesFolder);
                    IPath buildPath = newProject.getFolder("build/classes").getFullPath();
                    jproject.setOutputLocation(buildPath, new SubProgressMonitor(monitor, 1));
                    IClasspathEntry[] entries = new IClasspathEntry[] { JavaCore.newSourceEntry(newProject.getFolder("src/java").getFullPath()), JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER)), JavaCore.newContainerEntry(new Path(ModuloClasspathContainer.CONTAINER_ID)) };
                    jproject.setRawClasspath(entries, new SubProgressMonitor(monitor, 1));
                    ModuleDefinition definition = new ModuleDefinition();
                    definition.setId(fModuloPage.getPackageName());
                    definition.setVersion(new VersionNumber(1, 0, 0));
                    definition.setMetaName(fModuloPage.getModuleName());
                    definition.setMetaDescription("The " + fModuloPage.getModuleName() + " Module.");
                    definition.setModuleClassName(fModuloPage.getPackageName() + "." + fModuloPage.getModuleClassName());
                    if (fModuloPage.isConfigSelectioned()) definition.setConfigurationClassName(fModuloPage.getPackageName() + "." + fModuloPage.getConfigClassName());
                    if (fModuloPage.isStatSelectioned()) definition.setStatisticsClassName(fModuloPage.getPackageName() + "." + fModuloPage.getStatClassName());
                    modProj.setDefinition(definition);
                    modProj.createPackage();
                    modProj.createModuleXML();
                    modProj.createMainClass();
                    if (fModuloPage.isConfigSelectioned()) modProj.createConfigClass();
                    if (fModuloPage.isStatSelectioned()) modProj.createStatClass();
                    modProj.createModuleProperties();
                    modProj.createMessagesProperties();
                    IFolder binFolder = newProject.getFolder("bin");
                    binFolder.delete(true, new SubProgressMonitor(monitor, 1));
                } catch (CoreException e) {
                    e.printStackTrace();
                } finally {
                    monitor.done();
                }
            }
        };
    }

    /**
     * <p>
     * Stores the configuration element for the wizard. The config element will be used in <code>performFinish</code> to set the result perspective.
     * </p>
     * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
     * @param cfig
     * @param propertyName
     * @param data
     */
    public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
        fConfigElement = cfig;
    }

    /**
     * @see org.eclipse.jface.wizard.IWizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
     * @param page
     * @return
     */
    public IWizardPage getNextPage(IWizardPage page) {
        if (page instanceof WizardNewProjectCreationPage) {
            if (!fModuloPage.wasDisplayedOnce()) {
                String moduleName = fMainPage.getProjectName();
                if (moduleName.indexOf('.') != -1) moduleName = moduleName.substring(moduleName.lastIndexOf('.') + 1);
                fModuloPage.setModuleName(moduleName);
            }
        }
        return super.getNextPage(page);
    }

    /**
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
     * @param monitor
     * @throws InterruptedException
     * @throws CoreException
     */
    protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
    }

    /**
     * Getters for subclasses
     */
    protected IConfigurationElement getFConfigElement() {
        return fConfigElement;
    }

    protected WizardNewProjectCreationPage getFMainPage() {
        return fMainPage;
    }

    protected ModuloProjectCreationWizardPage getFModuloPage() {
        return fModuloPage;
    }

    /**
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
     * @return
     */
    public IJavaElement getCreatedElement() {
        return null;
    }
}
