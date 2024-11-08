package com.luxoft.fitpro.plugin.properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

public class DefaultFolderPropertiesTest extends TestCase {

    private IProject project = null;

    protected void setUp() throws Exception {
        project = createJavaProject();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetDefaultOutputLocation() {
        Assert.assertEquals("bin", DefaultFolderProperties.getDefaultOutputLocation(project));
    }

    public void testGetDefaultBuildLocation() {
        Assert.assertEquals("bin", DefaultFolderProperties.getDefaultBuildLocation(project));
    }

    public void testGetDefaultSourceLocation() {
        Assert.assertEquals("src", DefaultFolderProperties.getDefaultSourceLocation(project));
    }

    public void testGetDefaultBuildSourceLocation() {
        Assert.assertEquals("src", DefaultFolderProperties.getDefaultBuildSourceLocation(project));
    }

    public void testDefinedOutputLocation() {
        Assert.assertEquals(null, DefaultFolderProperties.getDefinedOutputLocation(project));
        DefaultFolderProperties.setDefinedOutputLocation(project, "test_output");
        Assert.assertEquals("test_output", DefaultFolderProperties.getDefinedOutputLocation(project));
        DefaultFolderProperties.removeDefinedOutputLocation(project);
        Assert.assertEquals(null, DefaultFolderProperties.getDefinedOutputLocation(project));
    }

    public void testDefinedSourceLocation() {
        Assert.assertEquals(null, DefaultFolderProperties.getDefinedSourceLocation(project));
        DefaultFolderProperties.setDefinedSourceLocation(project, "test_source");
        Assert.assertEquals("test_source", DefaultFolderProperties.getDefinedSourceLocation(project));
        DefaultFolderProperties.removeDefinedSourceLocation(project);
        Assert.assertEquals(null, DefaultFolderProperties.getDefinedSourceLocation(project));
    }

    private IProject createJavaProject() {
        IProject proj = ResourcesPlugin.getWorkspace().getRoot().getProject("DefaultFolderPropertiesTest");
        if (!proj.exists()) {
            try {
                proj.create(null);
                proj.open(null);
                IProjectDescription desc = proj.getDescription();
                desc.setNatureIds(new String[] { JavaCore.NATURE_ID });
                proj.setDescription(desc, null);
                IJavaProject javaProject = JavaCore.create(proj);
                javaProject.open(null);
                IFolder srcFolder1 = proj.getFolder(new Path("src"));
                srcFolder1.create(true, true, null);
                IFolder srcFolder2 = proj.getFolder(new Path("custom_src"));
                srcFolder2.create(true, true, null);
                IClasspathEntry[] classpathEntries = new IClasspathEntry[] { JavaCore.newSourceEntry(srcFolder1.getFullPath()), JavaCore.newSourceEntry(srcFolder2.getFullPath()), JavaRuntime.getDefaultJREContainerEntry() };
                javaProject.setRawClasspath(classpathEntries, null);
                IFolder binFolder = proj.getFolder(new Path("bin"));
                if (!binFolder.exists()) {
                    binFolder.create(true, true, null);
                }
                javaProject.setOutputLocation(binFolder.getFullPath(), null);
                IFolder testFolder = proj.getFolder(new Path("test"));
                testFolder.create(true, true, null);
                IFolder resultFolder = proj.getFolder(new Path("result"));
                resultFolder.create(true, true, null);
            } catch (CoreException e) {
                fail(e.getMessage());
            }
        }
        return proj;
    }
}
