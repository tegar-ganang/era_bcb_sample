package org.maven.ide.eclipse.ext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.PatternSyntaxException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.FormColors;
import org.maven.ide.eclipse.MavenEmbedderCallback;
import org.maven.ide.eclipse.ext.embedder.MavenEmbeddedRuntime;
import org.maven.ide.eclipse.ext.embedder.MavenRuntimeManager;
import org.maven.ide.eclipse.ext.preferences.Maven2PreferenceConstants;
import org.maven.ide.eclipse.index.Indexer;
import org.maven.ide.eclipse.launch.console.Maven2Console;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.Assert;
import org.nexopenframework.ide.eclipse.ui.NexOpenUIActivator;
import org.nexopenframework.ide.eclipse.ui.util.SettingsUtils;
import org.osgi.framework.BundleContext;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Extension of the Maven2Plugin for dealing with custom problems and enhaced features of the version <code>0.0.9</code>.
 * One of the main extended features are </p>
 * 
 * <ul>
 *   <li>Download of artifact jar sources</li>
 *   <li>Inspection of J2SE or JRE (if JRE indicates change for working properl with Maven2)</li>
 *   <li>Create a <code>settings.xml</code> if it does not exist</li>
 * </ul>
 * 
 * @see org.maven.ide.eclipse.ext.embedder.MavenRuntimeManager
 * @see org.maven.ide.eclipse.Maven2Plugin
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class Maven2Plugin extends org.maven.ide.eclipse.Maven2Plugin {

    /**key fo searching in the persistent property related to project*/
    public static final QualifiedName WEB_MODULES_EXT = new QualifiedName(Maven2Plugin.PLUGIN_ID, "web.modules.extensions");

    /**key for search in persistence property*/
    public static final QualifiedName BUSINESS_MODULES_EXT = new QualifiedName(Maven2Plugin.PLUGIN_ID, "business.modules.extensions");

    /**Maven2 classifier for sources of related artifacts*/
    public static final String CLASSIFIER_SOURCES = "sources";

    /**Separator of the maven2 repositories into the preference store*/
    public static final String REPO_SEPARATOR = "#";

    /**implementation of the {@link IResourceChangeListener} for dealing with team events*/
    private IResourceChangeListener team_resourceChangeListener;

    /***/
    private static FormColors formColors;

    /**Manager of maven2 executors (embedded or external engines)*/
    private MavenRuntimeManager runtimeManager;

    /**
	 * <p>Start extension of Maven2 plugin</p>
	 * 
	 * @see org.maven.ide.eclipse.Maven2Plugin#start(org.osgi.framework.BundleContext)
	 */
    public void start(final BundleContext context) throws Exception {
        final StringBuffer sb = new StringBuffer("");
        sb.append(System.getProperty("user.home")).append(File.separator);
        sb.append(".m2").append(File.separator).append("repository");
        final String localRepository = sb.toString();
        {
            final File repository = new File(localRepository);
            Logger.log(Logger.INFO, "Maven2 local repository :: " + localRepository);
            if (!repository.exists()) {
                Logger.log(Logger.INFO, "Local repository does not exist. Create a new one at location :: " + localRepository);
                try {
                    final boolean created = repository.mkdirs();
                    if (!created) {
                        Logger.log(Logger.WARNING, "Local repository not created");
                    }
                } catch (final SecurityException e) {
                    Logger.logException("Not enough privilegies for creation of the Maven2 local repository", e);
                }
            }
        }
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            super.start(context);
            final IPreferenceStore prefStore = getPreferenceStore();
            if (!prefStore.contains(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR)) {
                Logger.getLog().info("Set Local repository directory property [" + Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR + "] with value :: " + localRepository);
                prefStore.setValue(Maven2PreferenceConstants.P_LOCAL_REPOSITORY_DIR, localRepository);
            }
            this.runtimeManager = new MavenRuntimeManager(getPreferenceStore());
            this.runtimeManager.setEmbeddedRuntime(new MavenEmbeddedRuntime(getBundle()));
            final NexOpenIndexerJob job = new NexOpenIndexerJob("nexopen-indexer");
            job.schedule();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
        createSettingsIfNecessary();
        team_resourceChangeListener = new Maven2TeamResourceChangeListener();
        ResourcesPlugin.getWorkspace().addResourceChangeListener(team_resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
        checkJdk();
    }

    /**
	 * This method is called when the plug-in is stopped
	 */
    public void stop(final BundleContext context) throws Exception {
        super.stop(context);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(team_resourceChangeListener);
    }

    public static MavenRuntimeManager getMavenRuntimeManager() {
        return ((Maven2Plugin) getDefault()).runtimeManager;
    }

    /**
	 * <p></p>
	 * 
	 * @return
	 */
    public static String getArchetypeVersion() {
        return getDefault().getPreferenceStore().getString(Maven2PreferenceConstants.P_M2_ARCHETYPE_VERSION);
    }

    /**
	 * <p>Returns timeout of Maven2 executions in order to avoid high delays 
	 *    if some internet problems appers.</p>
	 * 
	 * @see org.nexopenframework.ide.eclipse.ui.NexOpenUIActivator#getTimeout()
	 */
    public static long getTimeout() {
        return NexOpenUIActivator.getTimeout();
    }

    /**
	 * @param project
	 * @param path
	 * @throws CoreException
	 */
    public static void downloadSources(final IProject project, final IPath path) {
        final StringBuffer sb = new StringBuffer("");
        sb.append(System.getProperty("user.home")).append(File.separator);
        sb.append(".m2").append(File.separator).append("repository");
        final String localRepository = sb.toString();
        String os_path = path.toOSString();
        try {
            os_path = os_path.replace(File.separator, "/");
            final String[] str_artifacts = os_path.substring(localRepository.length() + 1).split("/");
            final int num = str_artifacts.length;
            final boolean duplicates = !(new HashSet<String>(Arrays.asList(str_artifacts)).size() == num);
            final String str_artifactId = str_artifacts[num - 1].substring(0, str_artifacts[num - 1].lastIndexOf("-"));
            final String str_version = str_artifacts[num - 2];
            final StringBuffer str_groupId = new StringBuffer();
            for (int k = 0; k < num; k++) {
                String str = str_artifacts[k];
                if (k > 0 && str.indexOf(str_artifactId) > -1) {
                    if (duplicates && !str.equals(str_artifacts[k - 1])) {
                        str_groupId.append(".").append(str);
                    }
                    break;
                }
                if (k == 0) {
                    str_groupId.append(str);
                } else {
                    str_groupId.append(".").append(str);
                }
            }
            new DownloadSourcesJob("nexopen-downloadSources", str_groupId.toString(), str_artifactId, str_version, project, path).schedule();
        } catch (final PatternSyntaxException e) {
            Logger.logException(e);
        }
    }

    /**
	 * <p></p>
	 * 
	 * @see org.maven.ide.eclipse.Maven2Plugin#executeInEmbedder(org.maven.ide.eclipse.MavenEmbedderCallback, org.eclipse.core.runtime.IProgressMonitor)
	 */
    public Object executeInEmbedder(final MavenEmbedderCallback template, final IProgressMonitor monitor) {
        Object value = null;
        final Maven2Console console = this.getConsole();
        try {
            final MavenEmbedder embedder = getMavenEmbedderFromSuperclass();
            final boolean offline = getPreferenceStore().getBoolean(Maven2PreferenceConstants.P_OFFLINE);
            if (template instanceof ReadProjectTask && !offline) {
                Logger.getLog().debug("ReadProjectTask template in line. It could appears performance problems. Turn off line");
            }
            embedder.setOffline(offline);
            final String name = template.getClass().getSimpleName();
            final EmbedderJob job = new EmbedderJob(name, template, embedder);
            job.schedule();
            job.join();
            final IStatus status = job.getResult();
            if (status == null) {
                console.logError("Job " + name + " terminated; " + job);
            } else {
                if (status.isOK()) {
                    value = job.getCallbackResult();
                } else {
                    console.logError("Job " + name + " failed; " + status.getException().toString());
                    value = new CoreException(status);
                }
            }
        } catch (final SecurityException e) {
            console.logMessage("Security Exception trying to add to getMavenEmbedder method");
        } catch (final NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
            Throwable root = e.getTargetException();
            if (root == null) {
                root = e;
            }
            console.logMessage("Exception execution of MavenEmbedderCallback run method :: " + root);
            throw new RuntimeException(root);
        } catch (final Throwable e) {
            console.logMessage("Exception in executeInEmbedder method :: " + e);
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        if (value instanceof CoreException) {
            throw new RuntimeException((CoreException) value);
        } else {
            Logger.getLog().debug("returning value");
            return value;
        }
    }

    /**
	 * @param display
	 * @return
	 */
    public static FormColors getFormColors(final Display display) {
        if (formColors == null) {
            formColors = new FormColors(display);
            formColors.markShared();
        }
        return formColors;
    }

    protected static MavenEmbedder getMavenEmbedderFromSuperclass() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final Method getMavenEmbedder = getDefault().getClass().getSuperclass().getDeclaredMethod("getMavenEmbedder", new Class[0]);
        getMavenEmbedder.setAccessible(true);
        final MavenEmbedder embedder = (MavenEmbedder) getMavenEmbedder.invoke(getDefault(), new Object[0]);
        return embedder;
    }

    /**
	 * <p>Creates if necessary the <code>settings.xml</code> file under <code>[USER_HOME]/.m2</code></p>
	 * 
	 * @throws IOException
	 */
    protected void createSettingsIfNecessary() throws IOException {
        OutputStream out = null;
        try {
            final File fSettings = SettingsUtils.getSettingsFile();
            if (!fSettings.exists()) {
                fSettings.createNewFile();
                final Path src = new Path("mvn/settings.xml");
                final InputStream in = FileLocator.openStream(getBundle(), src, false);
                out = new FileOutputStream(SettingsUtils.getSettings(), true);
                IOUtils.copy(in, out);
            } else {
                Logger.getLog().info("File settings.xml already exists at " + fSettings);
            }
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    private void checkJdk() {
        final String osName = System.getProperty("os.name", "");
        if (osName.toLowerCase().indexOf("mac os") == -1) {
            final String javaHome = System.getProperty("java.home");
            final File toolsJar = new File(javaHome, "../lib/tools.jar");
            if (!toolsJar.exists()) {
                getConsole().logError("Eclipse is running in a JRE, but a JDK is required\n" + "  Some Maven plugins may not work when importing projects or updating source folders.");
                if (!getPreferenceStore().getBoolean(Maven2PreferenceConstants.P_DISABLE_JDK_WARNING)) {
                    showJdkWarning();
                }
            }
        }
    }

    private void showJdkWarning() {
        PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {

            public void run() {
                Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
                MessageDialogWithToggle dialog = new MessageDialogWithToggle(shell, "NexOpen-Maven2 Integration for Eclipse JDK Warning", null, "The Maven2 Integration requires that Eclipse be running in a JDK, " + "because a number of Maven core plugins are using jars from the JDK.\n\n" + "Please make sure the -vm option in <a>eclipse.ini</a> " + "is pointing to a JDK and verify that <a>Installed JREs</a> " + "are also using JDK installs.", MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL }, 0, "Do not warn again", false) {

                    protected Control createMessageArea(Composite composite) {
                        Image image = getImage();
                        if (image != null) {
                            imageLabel = new Label(composite, SWT.NULL);
                            image.setBackground(imageLabel.getBackground());
                            imageLabel.setImage(image);
                            GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.BEGINNING).applyTo(imageLabel);
                        }
                        Link link = new Link(composite, getMessageLabelStyle());
                        link.setText(message);
                        link.addSelectionListener(new SelectionAdapter() {

                            public void widgetSelected(SelectionEvent e) {
                                if ("eclipse.ini".equals(e.text)) {
                                    try {
                                        IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser();
                                        browser.openURL(new URL("http://help.eclipse.org/help33/index.jsp?topic=/org.eclipse.platform.doc.user/tasks/running_eclipse.htm"));
                                    } catch (final MalformedURLException ex) {
                                        Logger.getLog().error("Malformed URL", ex);
                                    } catch (final PartInitException ex) {
                                        Logger.getLog().error(ex);
                                    }
                                } else {
                                    PreferencesUtil.createPreferenceDialogOn(getShell(), "org.eclipse.jdt.debug.ui.preferences.VMPreferencePage", null, null).open();
                                }
                            }
                        });
                        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).grab(true, false).hint(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT).applyTo(link);
                        return composite;
                    }
                };
                dialog.setPrefStore(getPreferenceStore());
                dialog.setPrefKey(Maven2PreferenceConstants.P_DISABLE_JDK_WARNING);
                dialog.open();
                getPreferenceStore().setValue(Maven2PreferenceConstants.P_DISABLE_JDK_WARNING, dialog.getToggleState());
            }
        });
    }

    protected class NexOpenIndexerJob extends Job {

        protected NexOpenIndexerJob(final String name) {
            super(name);
        }

        protected IStatus run(final IProgressMonitor monitor) {
            try {
                final File file = new File(getIndexDir(), "nexopen");
                if (!file.exists()) {
                    file.mkdirs();
                }
                final Indexer indexer = new Indexer();
                final String localRepository = getPreferenceStore().getString("eclipse.m2.localRepositoryDirectory");
                indexer.reindex(file.getAbsolutePath(), localRepository, "nexopen", monitor);
                return Status.OK_STATUS;
            } catch (final Throwable t) {
                Logger.getLog().error("Unexpected Exception :: " + t.getMessage(), t);
                return new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, t.getMessage(), t);
            }
        }
    }

    /**
	 * <p>Inner class for execution of the {@link MavenEmbedderCallback} in
	 * a job</p>
	 *
	 */
    protected static class EmbedderJob extends Job {

        private final MavenEmbedderCallback template;

        private final MavenEmbedder embedder;

        /**the callback result*/
        private Object callbackResult;

        protected EmbedderJob(final String name, final MavenEmbedderCallback template, final MavenEmbedder embedder) {
            super(name);
            this.template = template;
            this.embedder = embedder;
        }

        protected IStatus run(final IProgressMonitor monitor) {
            try {
                callbackResult = this.template.run(this.embedder, monitor);
                return Status.OK_STATUS;
            } catch (Throwable t) {
                Logger.getLog().warn("Unexpected Exception in EmbedderJob", t);
                final String message = (t.getMessage() != null) ? t.getMessage() : "unexpected Exception arised in " + this.getClass().getName();
                return new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, message, t);
            }
        }

        public Object getCallbackResult() {
            return this.callbackResult;
        }
    }

    protected static class DownloadSourcesJob extends Job {

        final String groupId;

        final String artifactId;

        final String version;

        final IProject project;

        final IPath path;

        public DownloadSourcesJob(final String name, final String groupId, final String artifactId, final String version, IProject project, IPath path) {
            super(name);
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.version = version;
            this.project = project;
            this.path = path;
        }

        protected IStatus run(final IProgressMonitor monitor) {
            MavenEmbedder embedder = null;
            try {
                monitor.beginTask("download sources for " + groupId + ":" + artifactId + ":" + version, 20);
                embedder = getMavenEmbedderFromSuperclass();
                final Artifact artifact = embedder.createArtifactWithClassifier(groupId, artifactId, version, "jar", CLASSIFIER_SOURCES);
                Assert.notNull(artifact);
                final IPreferenceStore prefStore = getDefault().getPreferenceStore();
                final String repos_str = prefStore.getString(Maven2PreferenceConstants.P_M2_REPOSITORIES);
                final List repo_list = Arrays.asList(repos_str.split(REPO_SEPARATOR));
                final List<ArtifactRepository> remote_repos = new ArrayList<ArtifactRepository>(repo_list.size());
                final Iterator it_repos = repo_list.iterator();
                int k = 0;
                while (it_repos.hasNext()) {
                    final ArtifactRepository ar = new DefaultArtifactRepository("remote.repository." + k++, (String) it_repos.next(), new DefaultRepositoryLayout());
                    remote_repos.add(ar);
                }
                embedder.resolve(artifact, remote_repos, embedder.getLocalRepository());
                updateClasspathEntry(monitor, artifact);
                project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
                monitor.done();
                return Status.OK_STATUS;
            } catch (final Throwable t) {
                Logger.getLog().error("Unexpected Exception :: " + t.getMessage(), t);
                return new Status(IStatus.ERROR, Maven2Plugin.PLUGIN_ID, IStatus.ERROR, t.getMessage(), t);
            } finally {
                if (embedder != null) {
                    try {
                        embedder.stop();
                    } catch (final MavenEmbedderException e) {
                        Logger.getLog().info("Unexpected exception MavenEmbedderException", e);
                    }
                }
            }
        }

        /**
		 * @param monitor
		 * @param artifact
		 * @throws JavaModelException
		 */
        private void updateClasspathEntry(final IProgressMonitor monitor, final Artifact artifact) throws JavaModelException {
            final IClasspathEntry entry = JavaCore.newLibraryEntry(path, new Path(artifact.getFile().getAbsolutePath()), null);
            final IJavaProject javaProject = JavaCore.create(project);
            final IClasspathContainer container = JavaCore.getClasspathContainer(new Path(Maven2Plugin.CONTAINER_ID), javaProject);
            final List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>(Arrays.asList(container.getClasspathEntries()));
            final Iterator it_entries = entries.iterator();
            int position = 0;
            while (it_entries.hasNext()) {
                final IClasspathEntry ce = (IClasspathEntry) it_entries.next();
                if (ce.getPath().equals(entry.getPath())) {
                    position = entries.indexOf(ce);
                    break;
                }
            }
            entries.set(position, entry);
            final IClasspathEntry[] classpath = (IClasspathEntry[]) entries.toArray(new IClasspathEntry[entries.size()]);
            final IClasspathContainer n_container = new MavenClasspathContainer(path, classpath);
            JavaCore.setClasspathContainer(container.getPath(), new IJavaProject[] { javaProject }, new IClasspathContainer[] { n_container }, monitor);
        }
    }

    private static class MavenClasspathContainer implements IClasspathContainer {

        private final IClasspathEntry[] entries;

        private final IPath path;

        public MavenClasspathContainer() {
            this.path = new Path(Maven2Plugin.CONTAINER_ID);
            this.entries = new IClasspathEntry[0];
        }

        public MavenClasspathContainer(final IPath path, final IClasspathEntry[] entries) {
            this.path = path;
            this.entries = entries;
        }

        public MavenClasspathContainer(final IPath path, final Set<IClasspathEntry> entrySet) {
            this(path, entrySet.toArray(new IClasspathEntry[entrySet.size()]));
        }

        public synchronized IClasspathEntry[] getClasspathEntries() {
            return entries;
        }

        public String getDescription() {
            return "Maven Dependencies";
        }

        public int getKind() {
            return IClasspathContainer.K_APPLICATION;
        }

        public IPath getPath() {
            return path;
        }
    }
}
