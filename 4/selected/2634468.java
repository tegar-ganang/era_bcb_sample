package com.google.gdt.eclipse.designer.wizards.model.project;

import com.google.gdt.eclipse.designer.common.Constants;
import com.google.gdt.eclipse.designer.model.web.WebUtils;
import com.google.gdt.eclipse.designer.preferences.MainPreferencePage;
import com.google.gdt.eclipse.designer.util.Utils;
import com.google.gdt.eclipse.designer.wizards.Activator;
import org.eclipse.wb.internal.core.DesignerPlugin;
import org.eclipse.wb.internal.core.utils.jdt.core.ProjectUtils;
import org.eclipse.wb.internal.core.utils.reflect.ReflectionUtils;
import org.eclipse.wb.internal.core.wizards.DesignerJavaProjectWizard;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author scheglov_ke
 * @coverage gwt.wizard.ui
 */
public class ProjectWizard extends DesignerJavaProjectWizard {

    private CreateModuleWizardPage m_createModulePage;

    public ProjectWizard() {
        super();
        setDefaultPageImageDescriptor(Activator.getImageDescriptor("wizards/project/banner.gif"));
        setWindowTitle("New GWT Java Project");
    }

    @Override
    public void addPages() {
        if (!MainPreferencePage.validateLocation() && !Utils.hasGPE()) {
            addPage(new ConfigureWizardPage());
        }
        super.addPages();
        {
            List<WizardPage> pages = getPagesList();
            {
                m_createModulePage = new CreateModuleWizardPage();
                int index = getWizardNewProjectIndex(pages);
                pages.add(index + 1, m_createModulePage);
                m_createModulePage.setWizard(this);
            }
        }
    }

    /**
   * @return the internal {@link List} of {@link WizardPage}'s from super-wizard.
   */
    @SuppressWarnings("unchecked")
    private List<WizardPage> getPagesList() {
        try {
            Field pagesField = Wizard.class.getDeclaredField("pages");
            pagesField.setAccessible(true);
            return (List<WizardPage>) pagesField.get(this);
        } catch (Throwable e) {
            throw ReflectionUtils.propagate(e);
        }
    }

    /**
   * @return the index of first standard Java project wizard page.
   */
    private static int getWizardNewProjectIndex(List<WizardPage> pages) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i) instanceof WizardNewProjectCreationPage) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean performFinish() {
        boolean goodFinish = super.performFinish();
        if (goodFinish) {
            try {
                IJavaProject javaProject = getCreatedElement();
                configureProjectAsGWTProject(javaProject);
                m_createModulePage.createModule(javaProject);
            } catch (Throwable e) {
                DesignerPlugin.log(e);
                return false;
            }
        }
        return goodFinish;
    }

    /**
   * Configures given {@link IJavaProject} as GWT project - adds GWT library and nature.
   */
    public static final void configureProjectAsGWTProject(IJavaProject javaProject) throws Exception {
        IProject project = javaProject.getProject();
        if (!ProjectUtils.hasType(javaProject, "com.google.gwt.core.client.GWT")) {
            IClasspathEntry entry;
            if (Utils.hasGPE()) {
                entry = JavaCore.newContainerEntry(new Path("com.google.gwt.eclipse.core.GWT_CONTAINER"));
            } else {
                entry = JavaCore.newVariableEntry(new Path("GWT_HOME/gwt-user.jar"), null, null);
            }
            ProjectUtils.addClasspathEntry(javaProject, entry);
        }
        {
            ProjectUtils.addNature(project, Constants.NATURE_ID);
            if (Utils.hasGPE()) {
                ProjectUtils.addNature(project, "com.google.gwt.eclipse.core.gwtNature");
            }
        }
        {
            String webFolderName = WebUtils.getWebFolderName(project);
            ensureCreateFolder(project, webFolderName);
            ensureCreateFolder(project, webFolderName + "/WEB-INF");
            IFolder classesFolder = ensureCreateFolder(project, webFolderName + "/WEB-INF/classes");
            IFolder libFolder = ensureCreateFolder(project, webFolderName + "/WEB-INF/lib");
            javaProject.setOutputLocation(classesFolder.getFullPath(), null);
            if (!libFolder.getFile("gwt-servlet.jar").exists()) {
                String servletJarLocation = Utils.getGWTLocation(project) + "/gwt-servlet.jar";
                File srcFile = new File(servletJarLocation);
                File destFile = new File(libFolder.getLocation().toFile(), "gwt-servlet.jar");
                FileUtils.copyFile(srcFile, destFile);
                libFolder.refreshLocal(IResource.DEPTH_INFINITE, null);
            }
        }
    }

    private static IFolder ensureCreateFolder(IProject project, String name) throws CoreException {
        IFolder folder = project.getFolder(new Path(name));
        if (!folder.exists()) {
            folder.create(true, true, null);
        }
        return folder;
    }
}
