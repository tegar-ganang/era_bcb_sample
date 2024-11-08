package org.maven.ide.eclipse.wst.datamodel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jst.common.project.facet.WtpUtils;
import org.eclipse.jst.j2ee.project.facet.J2EEFacetInstallDelegate;
import org.eclipse.update.core.SiteManager;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.datamodel.FacetDataModelProvider;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;
import org.eclipse.wst.common.frameworks.datamodel.IDataModelOperation;
import org.eclipse.wst.common.project.facet.core.IDelegate;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.maven.ide.eclipse.Maven2Plugin;
import org.maven.ide.eclipse.container.Maven2ClasspathContainer;
import org.maven.ide.eclipse.container.Maven2ClasspathContainerInitializer;
import org.maven.ide.eclipse.ext.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.launch.Maven2LaunchConstants;
import org.maven.ide.eclipse.wst.util.ArchetypePOMHelper;
import org.nexopenframework.ide.eclipse.commons.io.FileUtils;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.launch.TimeoutLaunchConfiguration;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.PersistentContext;

/**
 * <p>NexOpen Framework/p>
 * 
 * <p>A {@link IDelegate} for dealing with Maven2 features. It generates the Maven2 structure</p>
 * 
 * @see org.eclipse.wst.common.project.facet.core.IDelegate;
 * @see org.eclipse.jst.j2ee.project.facet.J2EEFacetInstallDelegate
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class MavenFacetInstallDelegate extends J2EEFacetInstallDelegate implements IDelegate, Maven2LaunchConstants {

    /***/
    public static final String NEW_NEXOPEN_PROJECT = "new.project";

    /**
	 * <p></p>
	 * 
	 * @see org.eclipse.wst.common.project.facet.core.IDelegate#execute(org.eclipse.core.resources.IProject,
	 *      org.eclipse.wst.common.project.facet.core.IProjectFacetVersion,
	 *      java.lang.Object, org.eclipse.core.runtime.IProgressMonitor)
	 */
    public void execute(final IProject project, final IProjectFacetVersion fv, final Object config, final IProgressMonitor monitor) throws CoreException {
        final IDataModel model = (IDataModel) config;
        try {
            PersistentContext.setAttribute(NEW_NEXOPEN_PROJECT, Boolean.TRUE);
            if (monitor != null) {
                monitor.beginTask("", 7);
                monitor.setTaskName("Creating archetype - ");
            }
            boolean offline = false;
            try {
                final Class clazz = Thread.currentThread().getContextClassLoader().loadClass("org.maven.ide.eclipse.Maven2Plugin");
                final Maven2Plugin plugin = (Maven2Plugin) clazz.getMethod("getDefault", new Class[0]).invoke(null, new Object[0]);
                offline = plugin.getPreferenceStore().getBoolean("eclipse.m2.offline");
            } catch (ClassNotFoundException e) {
                Logger.logException("No class [org.maven.ide.eclipse.ext.Maven2Plugin] in classpath", e);
            } catch (NoSuchMethodException e) {
                Logger.logException("No method getDefault", e);
            } catch (Throwable e) {
                Logger.logException(e);
            }
            generateArchetype(project, model, monitor, offline);
            if (monitor != null) {
                monitor.setTaskName("Creating component core infrastructure");
            }
            ComponentCore.createComponent(project);
            if (monitor != null) {
                monitor.worked(1);
                monitor.setTaskName("Adding Eclipse Web Tools Platform natures");
            }
            WtpUtils.addNatures(project);
            addNature(project);
            if (monitor != null) {
                monitor.worked(1);
                monitor.setTaskName("Adding Apache Maven natures");
            }
            enableMavenNature(project, fv);
            if (monitor != null) {
                monitor.worked(1);
            }
            final IDataModelOperation operation = ((IDataModelOperation) model.getProperty(FacetDataModelProvider.NOTIFICATION_OPERATION));
            operation.execute(monitor, null);
        } catch (IOException e) {
            IStatus status = new Status(4, "org.maven.ide.eclipse.wtp", 0, "Unable to install Apache Maven facet [I/O problem]", e);
            Logger.log(Logger.ERROR, "Unable to install Apache Maven facet [I/O problem]", e);
            throw new CoreException(status);
        } catch (CoreException e) {
            final IStatus status = new Status(4, "org.maven.ide.eclipse.wtp", 0, "Unable to install Apache Maven facet [Core problem]", e);
            Logger.log(Logger.ERROR, "Unable to install Apache Maven facet [Core problem]", e);
            throw new CoreException(status);
        } catch (Throwable e) {
            final IStatus status = new Status(4, "org.maven.ide.eclipse.wtp", 0, "Unexpected exception installing Apache Maven facet", e);
            Logger.log(Logger.ERROR, "Unexpected exception installing Apache Maven facet", e);
            throw new CoreException(status);
        } finally {
            try {
                FileUtils.deleteDirectory(new File(project.getName()));
            } catch (IOException e) {
                Logger.log(Logger.INFO, "IOException deleting project file " + e);
            }
            if (monitor != null) {
                monitor.done();
            }
        }
    }

    /**
	 * <p></p>
	 * 
	 * @param project
	 * @throws CoreException
	 */
    void addNature(final IProject project) throws CoreException {
        final IProjectDescription desc = project.getDescription();
        final String[] current = desc.getNatureIds();
        final String[] replacement = new String[current.length + 1];
        System.arraycopy(current, 0, replacement, 0, current.length);
        replacement[current.length] = "org.eclipse.wst.common.project.facet.core.nature";
        desc.setNatureIds(replacement);
        project.setDescription(desc, null);
    }

    /**
	 * <p></p>
	 * 
	 * @param project
	 * @param model
	 * @param monitor
	 * @param offline 
	 * @throws CoreException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws Exception
	 */
    private void generateArchetype(final IProject project, final IDataModel model, final IProgressMonitor monitor, final boolean offline) throws CoreException, InterruptedException, IOException {
        if (getArchetypeArtifactId(model) != null) {
            final Properties properties = new Properties();
            properties.put("archetypeArtifactId", getArchetypeArtifactId(model));
            properties.put("archetypeGroupId", getArchetypeGroupId(model));
            properties.put("archetypeVersion", getArchetypeVersion(model));
            String artifact = (String) model.getProperty(IMavenFacetInstallDataModelProperties.PROJECT_ARTIFACT_ID);
            if (artifact == null || artifact.trim().length() == 0) {
                artifact = project.getName();
            }
            properties.put("artifactId", artifact);
            String group = (String) model.getProperty(IMavenFacetInstallDataModelProperties.PROJECT_GROUP_ID);
            if (group == null || group.trim().length() == 0) {
                group = project.getName();
            }
            properties.put("groupId", group);
            properties.put("version", model.getProperty(IMavenFacetInstallDataModelProperties.PROJECT_VERSION));
            final StringBuffer sb = new StringBuffer(System.getProperty("user.home")).append(File.separator);
            sb.append(".m2").append(File.separator).append("repository");
            final String local = sb.toString();
            Logger.getLog().debug("Local Maven2 repository :: " + local);
            properties.put("localRepository", local);
            if (!offline) {
                final String sbRepos = getRepositories();
                properties.put("remoteRepositories", sbRepos);
            }
            final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
            final ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(LAUNCH_CONFIGURATION_TYPE_ID);
            final ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, "Creating project using Apache Maven archetype");
            File archetypePomDirectory = getDefaultArchetypePomDirectory();
            try {
                String dfPom = getPomFile(group, artifact);
                ByteArrayInputStream bais = new ByteArrayInputStream(dfPom.getBytes());
                File f = new File(archetypePomDirectory, "pom.xml");
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
                    } catch (IOException e) {
                    }
                }
                if (SiteManager.isHttpProxyEnable()) {
                    addProxySettings(properties);
                }
                workingCopy.setAttribute(ATTR_POM_DIR, archetypePomDirectory.getAbsolutePath());
                workingCopy.setAttribute(ATTR_PROPERTIES, convertPropertiesToList(properties));
                String goalName = "archetype:create";
                if (offline) {
                    goalName = new StringBuffer(goalName).append(" -o").toString();
                }
                goalName = updateGoal(goalName);
                workingCopy.setAttribute(ATTR_GOALS, goalName);
                final long timeout = org.maven.ide.eclipse.ext.Maven2Plugin.getTimeout();
                TimeoutLaunchConfiguration.launchWithTimeout(monitor, workingCopy, project, timeout);
                monitor.setTaskName("Moving to workspace");
                FileUtils.copyDirectoryStructure(new File(archetypePomDirectory, project.getName()), ArchetypePOMHelper.getProjectDirectory(project));
                monitor.worked(1);
                performMavenInstall(monitor, project, offline);
                project.refreshLocal(2, monitor);
            } catch (final IOException ioe) {
                Logger.log(Logger.ERROR, "I/O exception. One probably solution is absence " + "of mvn2 archetypes or not the correct version, " + "in your local repository. Please, check existence " + "of this archetype.");
                Logger.getLog().error("I/O Exception arised creating mvn2 archetype", ioe);
                throw ioe;
            } finally {
                FileUtils.deleteDirectory(archetypePomDirectory);
                Logger.log(Logger.INFO, "Invoked removing of archetype POM directory");
            }
        }
        monitor.worked(1);
    }

    /**
	 * <p>A comma separated list of remote repositories</p>
	 * 
	 * @return
	 */
    private String getRepositories() {
        final IPreferenceStore ps = Maven2Plugin.getDefault().getPreferenceStore();
        final String repositories = ps.getString(Maven2PreferenceConstants.P_M2_REPOSITORIES);
        final String[] repos = repositories.split(org.maven.ide.eclipse.ext.Maven2Plugin.REPO_SEPARATOR);
        final StringBuilder sbRepos = new StringBuilder();
        for (int k = 0; k < repos.length; k++) {
            sbRepos.append(repos[k]);
            if (k != repos.length - 1) {
                sbRepos.append(",");
            }
        }
        return sbRepos.toString();
    }

    /**
	 * @return
	 * @throws IOException
	 */
    private File getDefaultArchetypePomDirectory() throws IOException {
        File pomFileDirectory = File.createTempFile("archetypePom", "xml");
        pomFileDirectory.delete();
        pomFileDirectory.mkdirs();
        return pomFileDirectory;
    }

    private void addProxySettings(final Properties props) {
        props.put("proxySet", "true");
        props.put("proxyHost", SiteManager.getHttpProxyServer());
        props.put("proxyPort", SiteManager.getHttpProxyPort());
    }

    /**
	 * <p></p>
	 * 
	 * @param monitor
	 * @param project
	 * @param offline 
	 * @throws CoreException 
	 * @throws InterruptedException 
	 * @throws Exception
	 */
    private void performMavenInstall(final IProgressMonitor monitor, final IProject project, final boolean offline) throws CoreException, InterruptedException {
        monitor.setTaskName("Installing project [" + project.getName() + "] for the first time");
        final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        final ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(LAUNCH_CONFIGURATION_TYPE_ID);
        final ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, "Installing project for the first time");
        workingCopy.setAttribute(ATTR_POM_DIR, project.getLocation().toOSString());
        String goalName = "install";
        if (offline) {
            goalName = new StringBuilder(goalName).append(" -o").toString();
        }
        goalName = updateGoal(goalName);
        workingCopy.setAttribute(ATTR_GOALS, goalName);
        final List properties = new ArrayList(2);
        {
            properties.add("maven.test.skip=true");
        }
        if (!offline) {
            properties.add("remoteRepositories=" + getRepositories());
        }
        if (SiteManager.isHttpProxyEnable()) {
            final Properties props = new Properties();
            try {
                addProxySettings(props);
                properties.addAll(convertPropertiesToList(props));
            } finally {
                props.clear();
            }
        }
        workingCopy.setAttribute(ATTR_PROPERTIES, properties);
        final long timeout = org.maven.ide.eclipse.ext.Maven2Plugin.getTimeout();
        TimeoutLaunchConfiguration.launchWithTimeout(monitor, workingCopy, project, timeout);
    }

    /**
	 * @param goalName
	 * @return
	 */
    private String updateGoal(final String _goalName) {
        String goalName = _goalName;
        final Maven2Plugin plugin = Maven2Plugin.getDefault();
        final boolean npu = plugin.getPreferenceStore().getBoolean("eclipse.m2.suppress_upToDate");
        if (npu) {
            goalName = new StringBuffer(goalName).append(" -npu").toString();
        }
        final boolean debugOutput = plugin.getPreferenceStore().getBoolean("eclipse.m2.debugOutput");
        if (debugOutput) {
            goalName = new StringBuffer(goalName).append(" -X").toString();
        }
        boolean updateSnapshots = plugin.getPreferenceStore().getBoolean("eclipse.m2.updateSnapshots");
        if (updateSnapshots) {
            goalName = new StringBuffer(goalName).append(" -U").toString();
        }
        final boolean produceErrors = plugin.getPreferenceStore().getBoolean("eclipse.m2.produce_errors");
        if (produceErrors) {
            goalName = new StringBuffer(goalName).append(" -e").toString();
        }
        return goalName;
    }

    /**
	 * 
	 * @param project
	 * @param fv 
	 * @throws Exception
	 */
    private void enableMavenNature(final IProject project, final IProjectFacetVersion fv) throws Exception {
        IJavaProject javaProject = JavaCore.create(project);
        if (javaProject != null) {
            IClasspathContainer maven2ClasspathContainer = Maven2ClasspathContainerInitializer.getMaven2ClasspathContainer(javaProject);
            IClasspathEntry containerEntries[] = maven2ClasspathContainer.getClasspathEntries();
            HashSet containerEntrySet = new HashSet();
            for (int i = 0; i < containerEntries.length; i++) {
                containerEntrySet.add(containerEntries[i].getPath().toString());
            }
            IClasspathEntry entries[] = javaProject.getRawClasspath();
            if (entries.length == 0) {
                Logger.log(Logger.ERROR, "No entries found in the classpath in maven2Facet");
                throw new IllegalStateException();
            }
            final ArrayList newEntries = new ArrayList();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry entry = entries[i];
                if (!Maven2ClasspathContainer.isMaven2ClasspathContainer(entry.getPath()) && !containerEntrySet.contains(entry.getPath().toString())) {
                    newEntries.add(entry);
                }
            }
            newEntries.add(JavaCore.newContainerEntry(new Path(Maven2Plugin.CONTAINER_ID)));
            final IClasspathEntry[] cpEntries = (IClasspathEntry[]) newEntries.toArray(new IClasspathEntry[newEntries.size()]);
            javaProject.setRawClasspath(cpEntries, null);
        }
    }

    /**
	 * @param properties
	 * @return
	 */
    private List convertPropertiesToList(final Properties properties) {
        final List propertiesList = new ArrayList();
        final Iterator iter = properties.keySet().iterator();
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
	 * @param model
	 * @return
	 */
    private Object getArchetypeVersion(IDataModel model) {
        return model.getProperty(IMavenFacetInstallDataModelProperties.ARCHETYPE_VERSION);
    }

    private Object getArchetypeGroupId(IDataModel model) {
        return model.getProperty(IMavenFacetInstallDataModelProperties.ARCHETYPE_GROUP_ID);
    }

    private Object getArchetypeArtifactId(IDataModel model) {
        return model.getProperty(IMavenFacetInstallDataModelProperties.ARCHETYPE_ARTIFACT_ID);
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
    private String getPomFile(String groupId, String artifactId) {
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
