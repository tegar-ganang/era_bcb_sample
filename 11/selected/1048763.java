package org.parallelj.codegen.tests;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.parallelj.codegen.Activator;
import org.parallelj.codegen.constants.Constants;

/**
 * Abstract Test Case for ParallelJ generated classes. Process is centralized
 * into abstract class.
 * 
 * @author Atos Worldline
 *  
 */
public class ForkJavaProject implements IForkProject {

    private IProject prj;

    private IJavaProject javaProject;

    private Class<?> activatorClass;

    public IProject getPrj() {
        return prj;
    }

    public ForkJavaProject(String projectName, Class<?> activatorClass) {
        this.activatorClass = activatorClass;
        try {
            IWorkspaceRoot rootWorkspace = ResourcesPlugin.getWorkspace().getRoot();
            this.prj = rootWorkspace.getProject(projectName);
            if (this.prj.exists()) {
                this.prj.delete(true, true, new NullProgressMonitor());
            }
            this.prj.create(new NullProgressMonitor());
            this.prj.open(new NullProgressMonitor());
            IProjectDescription description = this.prj.getDescription();
            description.setNatureIds(new String[] { "org.eclipse.jdt.core.javanature" });
            this.prj.setDescription(description, new NullProgressMonitor());
            createProjectDir(Constants.Dirs.DIR_MAIN_JAVA);
            createProjectDir(Constants.Dirs.DIR_CONFIG);
            createProjectDir(Constants.Dirs.DIR_MAIN_RESOURCES);
            createProjectDir(Constants.Dirs.DIR_MODELS);
            createProjectDir(Constants.Dirs.DIR_TESTS_JAVA);
            createProjectDir(Constants.Dirs.DIR_TESTS_RESOURCES);
            createProjectDir(Constants.Dirs.DIR_CLASSES);
            createProjectDir(Constants.Dirs.DIR_LIB);
            this.prj.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
            this.javaProject = JavaCore.create(this.prj);
            if (this.javaProject.exists() && !this.javaProject.isOpen()) {
                this.javaProject.open(new NullProgressMonitor());
            }
            File javaHome = new File(System.getProperty("java.home"));
            IPath jreLibPath = new Path(javaHome.getPath()).append("lib").append("rt.jar");
            this.javaProject.setOutputLocation(prj.getFolder(Constants.Dirs.DIR_CLASSES).getFullPath(), new NullProgressMonitor());
            JavaCore.setClasspathVariable("JRE_LIB", jreLibPath, new NullProgressMonitor());
            this.javaProject.setRawClasspath(getProjectClassPath(), new NullProgressMonitor());
        } catch (CoreException e) {
            Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, "An exception has been thrown while creating Project", e));
        }
    }

    private IClasspathEntry[] getProjectClassPath() {
        List<Path> pathEntry = new ArrayList<Path>();
        List<IClasspathEntry> classPathEntry = new ArrayList<IClasspathEntry>();
        classPathEntry.add(JavaCore.newSourceEntry(this.prj.getFolder(Constants.Dirs.DIR_MAIN_JAVA).getFullPath()));
        classPathEntry.add(JavaCore.newSourceEntry(this.prj.getFolder(Constants.Dirs.DIR_MAIN_RESOURCES).getFullPath()));
        classPathEntry.add(JavaCore.newSourceEntry(this.prj.getFolder(Constants.Dirs.DIR_TESTS_JAVA).getFullPath()));
        classPathEntry.add(JavaCore.newSourceEntry(this.prj.getFolder(Constants.Dirs.DIR_TESTS_RESOURCES).getFullPath()));
        classPathEntry.add(JavaCore.newSourceEntry(this.prj.getFolder(Constants.Dirs.DIR_MODELS).getFullPath()));
        classPathEntry.add(JavaCore.newVariableEntry(new Path("JRE_LIB"), new Path("JRE_SRC"), new Path("JRE_SRCROOT")));
        IClasspathEntry[] returnClassPathEntry = new IClasspathEntry[classPathEntry.size()];
        int i = 0;
        for (IClasspathEntry entry : classPathEntry) {
            returnClassPathEntry[i] = entry;
            i++;
        }
        for (Path path : pathEntry) {
            classPathEntry.add(JavaCore.newLibraryEntry(path, null, null));
        }
        return returnClassPathEntry;
    }

    public void addJarToProjectPath(String jarFileName) {
        String basedir = System.getProperty("basedir");
        addJarToProjectLib(basedir, jarFileName);
    }

    public void addM2JarToProjectPath(String jarFileName) {
        String basedir = System.getProperty("basedirm2");
        addJarToProjectLib(basedir, jarFileName);
    }

    private void addJarToProjectLib(String pBaseDir, String jarFileName) {
        StringBuffer basedir = new StringBuffer(pBaseDir);
        StringTokenizer strk = new StringTokenizer(jarFileName, "/");
        while (strk.hasMoreTokens()) {
            basedir.append("/").append(strk.nextToken("/"));
        }
        Path path = new Path(basedir.toString());
        IClasspathEntry[] classPathEntries;
        try {
            classPathEntries = javaProject.getRawClasspath();
            IClasspathEntry[] newClassPathEntries = new IClasspathEntry[classPathEntries.length + 1];
            System.arraycopy(classPathEntries, 0, newClassPathEntries, 0, classPathEntries.length);
            newClassPathEntries[classPathEntries.length] = JavaCore.newLibraryEntry(path, null, null);
            javaProject.setRawClasspath(newClassPathEntries, new NullProgressMonitor());
        } catch (JavaModelException e) {
            Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, "An exception has been thrown while setting Project's classpath", e));
        }
    }

    public void createProjectDir(String dir) {
        IFile destFile = this.prj.getFile(dir + Path.SEPARATOR + "aFile");
        String destFileName = destFile.getLocation().toString();
        File dstFile = new File(destFileName);
        if (!dstFile.getParentFile().exists()) {
            dstFile.getParentFile().mkdirs();
        }
    }

    public void createSrcPackage(String pck) {
        String dir = pck = Constants.Dirs.DIR_MAIN_JAVA + "/" + pck.replace('.', '/');
        this.createProjectDir(dir);
    }

    /**
	 * Tested class generated folder
	 */
    public IFile addTextFileToForkProject(String origine, String destination) {
        IFile destFile = null;
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = activatorClass.getResourceAsStream("/" + origine.replace('\\', '/'));
        } catch (Exception e) {
            return null;
        }
        if (resourceAsStream != null) {
            destFile = this.prj.getFile(destination.replace('\\', '/'));
            String destFileName = destFile.getLocation().toString();
            File dstFile = new File(destFileName);
            if (!dstFile.getParentFile().exists()) {
                dstFile.getParentFile().mkdirs();
            }
            try {
                InputStreamReader reader = new InputStreamReader(resourceAsStream);
                BufferedWriter writer = new BufferedWriter(new FileWriter(dstFile));
                char[] buffer;
                buffer = new char[resourceAsStream.available()];
                while (reader.ready()) {
                    reader.read(buffer);
                    writer.write(buffer);
                }
                writer.close();
                reader.close();
                this.prj.refreshLocal(2, null);
            } catch (Exception e) {
                Activator.getDefault().getLog().log(new Status(Status.ERROR, Activator.PLUGIN_ID, "An exception has been thrown while refreshing Project", e));
            }
        }
        return destFile;
    }

    public IJavaProject getJavaPrj() {
        return javaProject;
    }
}
