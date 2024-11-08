package org.maven.ide.eclipse.ext.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.ext.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.nexopenframework.ide.eclipse.commons.io.FileUtils;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.launch.TimeoutLaunchConfiguration;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.VersionCoordinator;
import org.nexopenframework.ide.eclipse.velocity.VelocityEngineHolder;
import org.nexopenframework.ide.eclipse.velocity.VelocityEngineUtils;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Creates a new module to the related project</p>
 * 
 * @see org.maven.ide.eclipse.launch.Maven2LaunchConstants
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public final class ModuleExtensionBuilder implements Maven2LaunchConstants {

    /**
	 * <p>Avoid create a new instance</p>
	 *
	 */
    private ModuleExtensionBuilder() {
        super();
    }

    /**
	 * <p>Creates a new module from an existent one</p>
	 * 
	 * @param project
	 * @param moduleExtension
	 * @throws CoreException
	 * @throws InterruptedException 
	 */
    public static void buildModule(final IProject project, final IModuleExtension moduleExtension) throws CoreException, IOException, InterruptedException {
        final boolean webtype = moduleExtension.getModuleType() == IModuleType.WEB_TYPE;
        if (webtype && isWebProject(project)) {
            throw new IllegalStateException("Web projects could not add web modules. Use an EAR project instead");
        }
        final IWorkspace ws = ResourcesPlugin.getWorkspace();
        IFolder folder = project.getFolder(moduleExtension.getModuleName());
        if (folder.exists()) {
            throw new IllegalStateException("folder already exists");
        }
        folder.create(false, true, null);
        IJavaProject jproject = null;
        if (project.exists()) {
            jproject = JavaCore.create(project);
        }
        final List<IClasspathEntry> collEntries = new ArrayList<IClasspathEntry>();
        final IClasspathEntry entries[] = jproject.getRawClasspath();
        if (webtype) {
            addClasspathEntries(collEntries, entries);
        }
        final IPath folderPath = folder.getFullPath();
        final IPath srcdir = folderPath.append("src/main/java");
        {
            ws.getRoot().getFolder(srcdir).getLocation().toFile().mkdirs();
            collEntries.add(JavaCore.newSourceEntry(srcdir));
        }
        final IPath srcResources = folderPath.append("src/main/resources");
        {
            ws.getRoot().getFolder(srcResources).getLocation().toFile().mkdirs();
            collEntries.add(JavaCore.newSourceEntry(srcResources));
        }
        if (moduleExtension.getModuleType() == IModuleType.WEB_TYPE) {
            final IPath srcWeb = folderPath.append("src/main/webapp");
            {
                ws.getRoot().getFolder(srcWeb).getLocation().toFile().mkdirs();
            }
        }
        final IPath srcTest = folderPath.append("src/test/java");
        {
            ws.getRoot().getFolder(srcTest).getLocation().toFile().mkdirs();
            collEntries.add(JavaCore.newSourceEntry(srcTest));
        }
        final IPath srcTestResources = folderPath.append("src/test/resources");
        {
            ws.getRoot().getFolder(srcTestResources).getLocation().toFile().mkdirs();
            collEntries.add(JavaCore.newSourceEntry(srcTestResources));
        }
        if (!webtype) {
            addClasspathEntries(collEntries, entries);
        }
        try {
            final IClasspathEntry[] cp = (IClasspathEntry[]) collEntries.toArray(new IClasspathEntry[collEntries.size()]);
            jproject.setRawClasspath(cp, null);
        } catch (JavaModelException e) {
            Logger.log(Logger.INFO, "Java Model Exception. In the setRawClasspath method :: " + e);
            throw new RuntimeException(e);
        }
        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
        if (moduleExtension.getModuleType() == IModuleType.BUSINESS_TYPE) {
            final StringBuffer nexopen = new StringBuffer(moduleExtension.getModuleName()).append("/src/main/resources/");
            nexopen.append("nexopen.properties");
            final IFile file = project.getFile(nexopen.toString());
            final StringBuffer sb = new StringBuffer("# \n# DO NOT EDIT THIS FILE\n#");
            sb.append("\n#File for auto-detection of ServiceComponent classes");
            sb.append(" (Business Services or Services)").append("\n");
            sb.append("#and Entity classes which holds a JSR-220 annotation.");
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(sb.toString().getBytes());
                file.create(is, false, null);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        final VelocityEngine ve = VelocityEngineHolder.getEngine();
        final Map<String, String> model = new HashMap<String, String>();
        {
            model.put("artifactId", moduleExtension.getArtifact());
            model.put("groupId", moduleExtension.getGroup());
            model.put("version", moduleExtension.getVersion());
            model.put("moduleName", moduleExtension.getModuleName());
            model.put("description", moduleExtension.getDescription());
        }
        if (moduleExtension.getModuleType() == IModuleType.WEB_TYPE) {
            final StringBuffer nexopen = new StringBuffer(moduleExtension.getModuleName());
            nexopen.append("/").append("MANIFEST.MF");
            IFile file = project.getFile(nexopen.toString());
            String manifest;
            try {
                {
                    final QualifiedName qn = new QualifiedName("org.nexopenframework.ide.eclipse.ui", "nexopen.version");
                    final String nexopenVersion = project.getPersistentProperty(qn);
                    model.put("nexopen.version", nexopenVersion);
                    final String springVersion = VersionCoordinator.getSpringFrameworkVersion(nexopenVersion);
                    model.put("spring.version", springVersion);
                    final String hbVersion = VersionCoordinator.getHibernateVersion(nexopenVersion);
                    model.put("hibernate.version", hbVersion);
                    final String hbAnnotVersion = VersionCoordinator.getHibernateAnnotationsVersion(nexopenVersion);
                    model.put("hibernate.annot.version", hbAnnotVersion);
                }
                manifest = VelocityEngineUtils.mergeTemplateIntoString(ve, "Manifest.vm", model);
            } catch (final VelocityException e) {
                throw new RuntimeException(e);
            }
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(manifest.getBytes());
                file.create(is, false, null);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (final IOException e) {
                    }
                }
            }
        }
        try {
            final String vmPomFile = (webtype) ? "pom-web.vm" : "pom-business.vm";
            final String pom = VelocityEngineUtils.mergeTemplateIntoString(ve, vmPomFile, model);
            final StringBuffer sb = new StringBuffer(moduleExtension.getModuleName());
            sb.append("/").append("pom.xml");
            final IFile pomFile = project.getFile(sb.toString());
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(pom.getBytes());
                pomFile.create(is, false, null);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                        is = null;
                    } catch (IOException e) {
                    }
                }
            }
        } catch (final VelocityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
	 * <p></p>
	 * 
	 * @param jproject
	 * @param collEntries
	 * @param entries
	 */
    private static void addClasspathEntries(List<IClasspathEntry> collEntries, IClasspathEntry[] entries) {
        for (final IClasspathEntry entry : entries) {
            collEntries.add(entry);
        }
    }

    /**
	 * @param prj
	 * @return
	 */
    protected static boolean isWebProject(final IProject prj) {
        IFacetedProject fp;
        try {
            fp = ProjectFacetsManager.create(prj);
        } catch (final CoreException e) {
            throw new RuntimeException(e);
        }
        if (fp == null) {
            return false;
        }
        Set facets = fp.getProjectFacets();
        boolean nexopenWeb = false;
        Iterator it_facets = facets.iterator();
        while (it_facets.hasNext()) {
            IProjectFacetVersion version = (IProjectFacetVersion) it_facets.next();
            IProjectFacet pf = version.getProjectFacet();
            nexopenWeb = pf.getId().equals("jst.nexopen.web");
            if (nexopenWeb) {
                break;
            }
        }
        return nexopenWeb;
    }

    /**
	 * @param project
	 * @param moduleExtension
	 * @param location
	 * @throws CoreException
	 * @throws InterruptedException
	 * @throws IOException
	 */
    public static void invokeMvnArtifact(final IProject project, final IModuleExtension moduleExtension, final String location) throws CoreException, InterruptedException, IOException {
        final Properties properties = new Properties();
        properties.put("archetypeGroupId", "org.nexopenframework.plugins");
        properties.put("archetypeArtifactId", "openfrwk-archetype-webmodule");
        final String version = org.maven.ide.eclipse.ext.Maven2Plugin.getArchetypeVersion();
        properties.put("archetypeVersion", version);
        properties.put("artifactId", moduleExtension.getArtifact());
        properties.put("groupId", moduleExtension.getGroup());
        properties.put("version", moduleExtension.getVersion());
        final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        final ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(LAUNCH_CONFIGURATION_TYPE_ID);
        final ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, "Creating WEB module using Apache Maven archetype");
        File archetypePomDirectory = getDefaultArchetypePomDirectory();
        try {
            final String dfPom = getPomFile(moduleExtension.getGroup(), moduleExtension.getArtifact());
            final ByteArrayInputStream bais = new ByteArrayInputStream(dfPom.getBytes());
            final File f = new File(archetypePomDirectory, "pom.xml");
            OutputStream fous = null;
            try {
                fous = new FileOutputStream(f);
                IOUtils.copy(bais, fous);
            } finally {
                try {
                    if (fous != null) {
                        fous.close();
                    }
                    if (bais != null) {
                        bais.close();
                    }
                } catch (final IOException e) {
                }
            }
            String goalName = "archetype:create";
            boolean offline = false;
            try {
                final Class clazz = Thread.currentThread().getContextClassLoader().loadClass("org.maven.ide.eclipse.Maven2Plugin");
                final Maven2Plugin plugin = (Maven2Plugin) clazz.getMethod("getDefault", new Class[0]).invoke(null, new Object[0]);
                offline = plugin.getPreferenceStore().getBoolean("eclipse.m2.offline");
            } catch (final ClassNotFoundException e) {
                Logger.logException("No class [org.maven.ide.eclipse.ext.Maven2Plugin] in classpath", e);
            } catch (final NoSuchMethodException e) {
                Logger.logException("No method getDefault", e);
            } catch (final Throwable e) {
                Logger.logException(e);
            }
            if (offline) {
                goalName = new StringBuffer(goalName).append(" -o").toString();
            }
            if (!offline) {
                final IPreferenceStore ps = Maven2Plugin.getDefault().getPreferenceStore();
                final String repositories = ps.getString(Maven2PreferenceConstants.P_M2_REPOSITORIES);
                final String[] repos = repositories.split(org.maven.ide.eclipse.ext.Maven2Plugin.REPO_SEPARATOR);
                final StringBuffer sbRepos = new StringBuffer();
                for (int k = 0; k < repos.length; k++) {
                    sbRepos.append(repos[k]);
                    if (k != repos.length - 1) {
                        sbRepos.append(",");
                    }
                }
                properties.put("remoteRepositories", sbRepos.toString());
            }
            workingCopy.setAttribute(ATTR_GOALS, goalName);
            workingCopy.setAttribute(ATTR_POM_DIR, archetypePomDirectory.getAbsolutePath());
            workingCopy.setAttribute(ATTR_PROPERTIES, convertPropertiesToList(properties));
            final long timeout = org.maven.ide.eclipse.ext.Maven2Plugin.getTimeout();
            TimeoutLaunchConfiguration.launchWithTimeout(new NullProgressMonitor(), workingCopy, project, timeout);
            FileUtils.copyDirectoryStructure(new File(archetypePomDirectory, project.getName()), new File(location));
            FileUtils.deleteDirectory(new File(location + "/src"));
            FileUtils.forceDelete(new File(location, "pom.xml"));
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
        } finally {
            FileUtils.deleteDirectory(archetypePomDirectory);
            Logger.log(Logger.INFO, "Invoked removing of archetype POM directory");
        }
    }

    /**
	 * @param properties
	 * @return
	 */
    private static List<String> convertPropertiesToList(final Properties properties) {
        final List<String> propertiesList = new ArrayList<String>();
        Iterator iter = properties.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            String value = properties.getProperty(key);
            StringBuffer sb = new StringBuffer(key).append("=");
            sb.append(value);
            propertiesList.add(sb.toString());
        }
        return propertiesList;
    }

    /**
	 * @return
	 * @throws IOException
	 */
    private static File getDefaultArchetypePomDirectory() throws IOException {
        File pomFileDirectory = File.createTempFile("archetypePom", "xml");
        pomFileDirectory.delete();
        pomFileDirectory.mkdirs();
        return pomFileDirectory;
    }

    /**
	 * <p>In order to execute maven2, we have to provide this dummy
	 * pom file with nonsense information for allowing to MavenEmbedder the execution
	 * of our maven2 archetype. Probably this functionality changes in future
	 * releases of the maven2 eclipse plugin [Currently we are working with
	 * the 0.0.9 version and we hope to change to 0.0.11 in the last weeks].</p>
	 * 
	 * @param groupId
	 * @param artifactId
	 * @return
	 */
    private static String getPomFile(final String groupId, final String artifactId) {
        StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<project>");
        sb.append("<modelVersion>4.0.0</modelVersion>");
        sb.append("<groupId>").append(groupId).append("</groupId>");
        sb.append("<artifactId>").append(artifactId).append("</artifactId>");
        sb.append("<version>0.0.1</version>");
        sb.append("<packaging>pom</packaging>");
        sb.append("</project>");
        return sb.toString();
    }
}
