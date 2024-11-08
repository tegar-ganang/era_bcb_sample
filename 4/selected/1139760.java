package net.sf.refactorit.commonIDE;

import net.sf.refactorit.Version;
import net.sf.refactorit.classmodel.Project;
import net.sf.refactorit.common.util.AppRegistry;
import net.sf.refactorit.common.util.Assert;
import net.sf.refactorit.commonIDE.options.Path;
import net.sf.refactorit.commonIDE.options.PathItem;
import net.sf.refactorit.loader.ASTTreeCache;
import net.sf.refactorit.refactorings.undo.MilestoneManager;
import net.sf.refactorit.refactorings.undo.RitUndoManager;
import net.sf.refactorit.reports.Statistics;
import net.sf.refactorit.source.SourceParsingException;
import net.sf.refactorit.ui.DialogManager;
import net.sf.refactorit.ui.JErrorDialog;
import net.sf.refactorit.ui.JProgressDialog;
import net.sf.refactorit.ui.ParsingMessageDialog;
import net.sf.refactorit.ui.RuntimePlatform;
import net.sf.refactorit.ui.SearchingInterruptedException;
import net.sf.refactorit.ui.dialog.RitDialog;
import net.sf.refactorit.ui.errors.ErrorsTab;
import net.sf.refactorit.ui.module.RefactorItContext;
import net.sf.refactorit.ui.projectoptions.ProjectOptions;
import net.sf.refactorit.utils.ParsingInterruptedException;
import net.sf.refactorit.utils.RefactorItConstants;
import net.sf.refactorit.utils.XMLSerializer;
import net.sf.refactorit.vfs.ClassPath;
import net.sf.refactorit.vfs.Source;
import net.sf.refactorit.vfs.SourcePath;
import org.apache.log4j.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.util.HashMap;
import java.util.Random;

/**
 * Abstract base class for all IDE versions
 * use IDEController.getInstance() to get version specific controller
 *
 * @author Tonis
 * @author Anton Safonov
 * @author Oleg Golovachov
 */
public abstract class IDEController {

    public static final int UNKNOWN_PLATFROM = -1;

    public static final int JBUILDER = 1;

    public static final int NETBEANS = 2;

    public static final int JDEV = 3;

    public static final int STANDALONE = 4;

    public static final int TEST = 5;

    public static final int ECLIPSE = 6;

    public static boolean browserUnderNB = false;

    static final Logger log = Logger.getLogger(IDEController.class);

    static IDEController instance;

    private LoadingProperties properties = new LoadingProperties();

    /**
   * IDE project related to active project
   */
    private Object activeIdeProject;

    protected Project activeProject;

    private boolean activeProjectCacheLoaded;

    HashMap projectsCache = new HashMap(1, 1f);

    private String ideName = "";

    private String ideVersion = "";

    private String ideBuild = "";

    /**
   * Set true after setInstance first call
   */
    private static boolean initialized;

    public IDEController() {
        getIdeInfo();
        Statistics.updateStats();
        AppRegistry.getLogger(IDEController.class).info(getInfoString());
    }

    public String getInfoString() {
        return "RIT: " + Version.getVersion() + " (" + Version.getBuildId() + "), IDE: " + getIdeName() + " " + getIdeVersion() + " " + getIdeBuild() + ", JAVA: " + System.getProperty("java.vm.name", "unknown") + " " + System.getProperty("java.vm.version", "") + ", OS: " + System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", "") + " " + System.getProperty("sun.os.patch.level", "") + " " + System.getProperty("os.arch", "");
    }

    public static IDEController getInstance() {
        if (instance == null) {
            initializeInstance();
        }
        return instance;
    }

    public int getPlatform() {
        return UNKNOWN_PLATFROM;
    }

    /** @return true then controllers active project isn't active in IDE anymore */
    public boolean isProjectChangedInIDE() {
        return getActiveProjectFromIDE() != getIDEProject();
    }

    public abstract RefactorItContext createProjectContext();

    /**
   *
   * @return active ide project, can be null if can't determine one or if it isn't java project
   */
    public abstract Object getActiveProjectFromIDE();

    public static boolean runningJDev() {
        return checkPlatform(JDEV);
    }

    public abstract ActionRepository getActionRepository();

    protected abstract void getIdeInfo();

    public static boolean runningJBuilder() {
        return checkPlatform(JBUILDER);
    }

    private static boolean checkPlatform(final int code) {
        if (instance == null) {
            return false;
        }
        return instance.getPlatform() == code;
    }

    public static boolean runningNetBeans() {
        return checkPlatform(NETBEANS);
    }

    public static boolean runningTest() {
        return checkPlatform(TEST);
    }

    public static boolean runningEclipse() {
        return checkPlatform(ECLIPSE);
    }

    public static class ParsingResult {

        Exception exception;

        int code;

        public static final int OK = 0;

        public static final int CANCELED = 1;

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public int getCode() {
            return this.code;
        }

        void setCode(int code) {
            this.code = code;
        }
    }

    protected final ParsingResult parseProject(Project project) {
        ParsingResult result = new ParsingResult();
        result.setCode(ParsingResult.OK);
        Exception e = null;
        try {
            if (properties.showDialogsIfNeeded) {
                ParsingMessageDialog dlg = new ParsingMessageDialog(createProjectContext());
                dlg.setDialogTask(new ParsingMessageDialog.RebuildProjectTask(project, properties.clean, properties.forceFullBuild));
                try {
                    dlg.show(true);
                } catch (ParsingInterruptedException ex2) {
                    result.setCode(ParsingResult.CANCELED);
                    return result;
                }
            } else {
                project.getProjectLoader().build(null, false);
            }
        } catch (SourceParsingException spe) {
            e = spe;
        } catch (Exception e1) {
            e = e1;
        }
        result.setException(e);
        if (project.getProjectLoader().isParsingCanceledLastTime()) {
            result.setCode(ParsingResult.CANCELED);
        }
        return result;
    }

    /**
   * Ensure project with default loading properties
   */
    public final boolean ensureProject() {
        return ensureProject(new LoadingProperties());
    }

    /**
   * ensures the project with default loading properties and no parsing
   * overhead
   */
    public final boolean ensureProjectWithoutParsing() {
        return ensureProjectWithoutParsing(new LoadingProperties());
    }

    /**
   * Ensure project
   *  @return true if project parsing was finished succesfully and now errors was found,
   *           otherwise false. It returns false also when can't determine active IDE project
   *  NB! this method must call {@link #ensureProjectWithoutParsing(LoadingProperties) } first!
   */
    public final synchronized boolean ensureProject(LoadingProperties loadingProperties) {
        if (!ensureProjectWithoutParsing(loadingProperties)) {
            return false;
        }
        Project project = getActiveProject();
        project.getPaths().getClassPath().release();
        if (!checkParsingPreconditions(project)) {
            return false;
        }
        if (!activeProjectCacheLoaded) {
            activeProjectCacheLoaded = true;
            deserializeCache(true);
        }
        return buildProject(project);
    }

    private boolean buildProject(Project project) {
        try {
            ParsingResult parsingResult = parseProject(project);
            boolean result = processParsingResult(project, parsingResult);
            if (parsingResult.getCode() == ParsingResult.CANCELED) {
                return false;
            }
            return result;
        } finally {
            releaseResources(project);
        }
    }

    /**
   * Creates new project from ideProject
   *
   * @param ideProject object returned by {@link #getActiveProjectFromIDE() }, NB! it can be null
   *
   * @return project or null if can't create RIT project ( for example when it isn't java project)
   */
    protected abstract Project createNewProjectFromIdeProject(Object ideProject);

    public final Project createNewProject(Object ideProject) {
        return createNewProjectFromIdeProject(ideProject);
    }

    /**
   *
   * @return true if resolving active project was correct false otherwise
   */
    private boolean ensureProjectWithoutParsing(LoadingProperties loadingProperties) {
        properties = loadingProperties;
        beforeEnsureProject();
        boolean wasSaved = saveAllFiles();
        if (wasSaved == false && properties.showDialogsIfNeeded) {
            String message = "Save all files command was unsuccessful.\r\n" + "Save them manually.";
            RitDialog.showMessageDialog(createProjectContext(), message);
            new Exception("save").printStackTrace();
            RitDialog.showMessageDialog(createProjectContext(), message);
        }
        Project resolvedProject = getActiveProject();
        if (resolvedProject == null) {
            log.debug("ensureProject called when project==null");
            return false;
        }
        return true;
    }

    /**
   * Resolves active project
   * If project was changed in ide creates new, otherwise just returns active project
   *
   * Postcond: result != null => getActiveProject() == result
   *
   * @return resolved project or null if can't create or resolve
   */
    public Project getActiveProject() {
        return getWorkspace().getActiveProject();
    }

    protected void releaseResources(Project project) {
        project.getPaths().getClassPath().release();
    }

    protected boolean processParsingResult(Project project, ParsingResult result) {
        if ((project.getProjectLoader().getErrorCollector()).hasUserFriendlyErrors()) {
            RefactorItContext context = createProjectContext();
            ErrorsTab.addNew(context);
        }
        Exception e = result.getException();
        boolean ignoreException = (e instanceof SourceParsingException) && ((SourceParsingException) e).justInformsThatUserFriendlyErrorsExist();
        if (e != null && !ignoreException) {
            JErrorDialog err = new JErrorDialog(createProjectContext(), "Error");
            err.setException(e);
            AppRegistry.getExceptionLogger().error(e, "");
            err.show();
            return false;
        }
        if ((project.getProjectLoader().getErrorCollector()).hasCriticalUserErrors()) {
            DialogManager.getInstance().showCriticalError(createProjectContext(), project);
            if ((project.getProjectLoader().getErrorCollector()).hasErrorsCausedByWrongJavaVersion()) {
                DialogManager.getInstance().showJavaVersionWarning(createProjectContext());
            }
            return false;
        }
        if ((project.getProjectLoader().getErrorCollector()).hasErrorsCausedByWrongJavaVersion()) {
            DialogManager.getInstance().showJavaVersionWarning(createProjectContext());
        }
        return !project.getProjectLoader().isParsingCanceledLastTime();
    }

    /**
   * @return properties
   */
    protected LoadingProperties getLoadingProperties() {
        return properties;
    }

    /**
   * @deprecated shall be used via Workspace.closeProject()
   */
    public void onIdeExit() {
        try {
            serializeProjectCache(activeProject, false);
            MilestoneManager.clear();
        } catch (Error error) {
            AppRegistry.getExceptionLogger().error(error, this);
        }
    }

    /**
   * Serializes project, returned by getProject(), cache.
   * @param prj project to serialize
   * @param showDialogs showDialogs
   * @return true if successful
   */
    public boolean serializeProjectCache(final Project prj, boolean showDialogs) {
        if (prj != null && prj.getCachePath() != null) {
            final String cachePath = (String) prj.getCachePath();
            Assert.must(cachePath != null);
            if (RefactorItConstants.debugInfo) {
                log.debug("serializing project to " + cachePath);
            }
            Runnable writerTask = new Runnable() {

                public void run() {
                    ASTTreeCache.writeCache(prj.getProjectLoader().getAstTreeCache(), cachePath);
                }
            };
            if (showDialogs) {
                try {
                    JProgressDialog.run(createProjectContext(), writerTask, "Serializing cache", false);
                } catch (SearchingInterruptedException ex) {
                    if (Assert.enabled) {
                        Assert.must(false, "SearchingInterruptedException caught");
                    }
                    return false;
                }
            } else {
                Thread writingThread = new Thread(writerTask);
                writingThread.start();
                try {
                    writingThread.join();
                } catch (InterruptedException ex1) {
                    AppRegistry.getExceptionLogger().error(ex1, this);
                }
            }
        }
        this.projectsCache.clear();
        return true;
    }

    /**
   * Deserializes cache.
   * @param showInDialog showInDialog
   */
    public void deserializeCache(boolean showInDialog) {
        final Object cachePath = activeProject.getCachePath();
        if (cachePath != null) {
            ASTTreeCache cache = deserializeCacheImpl(showInDialog, cachePath);
            if (cache != null) {
                activeProject.getProjectLoader().setAstTreeCache(cache);
                activeProject.getProjectLoader().validateAstTreeCache();
            } else {
                log.debug("cache was null: " + cachePath);
            }
        } else {
            log.warn("IDEController: cachepath==null");
        }
        return;
    }

    ASTTreeCache deserializeCacheImpl(final boolean showInDialog, final Object cachePath) {
        synchronized (this.projectsCache) {
            final ASTTreeCache[] cio = new ASTTreeCache[1];
            log.debug("IDEController: deserializing project from " + cachePath);
            Object activeIDEProject = getActiveProjectFromIDE();
            cio[0] = (ASTTreeCache) this.projectsCache.get(activeIDEProject);
            if (cio[0] != null) {
                this.projectsCache.remove(activeIDEProject);
                return cio[0];
            }
            if (this.projectsCache.containsKey(Object.class)) {
                return null;
            }
            this.projectsCache.put(Object.class, Object.class);
            RuntimePlatform.console.print("RefactorIT: Deserializing project cache ... ");
            Runnable runnable = new Runnable() {

                public void run() {
                    cio[0] = readCache(cachePath);
                }
            };
            if (showInDialog) {
                try {
                    JProgressDialog.run(createProjectContext(), runnable, "Deserializing cache", false);
                } catch (SearchingInterruptedException ex) {
                    if (Assert.enabled) {
                        Assert.must(false, "SearchingInterruptedException caught");
                    }
                    return null;
                }
            } else {
                Thread deserializingThread = new Thread(runnable);
                deserializingThread.start();
                try {
                    deserializingThread.join();
                } catch (InterruptedException ex1) {
                    AppRegistry.getExceptionLogger().error(ex1, this);
                    return null;
                }
            }
            if (cio[0] != null) {
                RuntimePlatform.console.println("DONE");
            } else {
                RuntimePlatform.console.println("CACHE IS EMPTY");
            }
            return cio[0];
        }
    }

    public ASTTreeCache readCache(Object cachePath) {
        if (cachePath instanceof String) {
            return ASTTreeCache.readCache((String) cachePath);
        }
        return null;
    }

    /**
   *
   * @param ideProject TODO
   * @return cache path for project returned by {@link #getActiveProjectFromIDE()}
   */
    protected abstract Object getCachePathForActiveProject(Object ideProject);

    private static void initializeInstance() {
        log.debug("instance not initialized -- should not happened");
    }

    /**
   * NB!!! for testing only!!!!
   * @param controllerInstance controllerInstance
   */
    public static void setInstance(IDEController controllerInstance) {
        boolean needToSearchForCache = true;
        if (instance != null && controllerInstance.getPlatform() != IDEController.TEST) {
            if (RefactorItConstants.debugInfo) {
                log.debug("IDEController setInstance called second time!!!");
            }
            needToSearchForCache = instance.getIDEProject() != controllerInstance.getIDEProject();
        }
        instance = controllerInstance;
        if (!initialized) {
            initialized = true;
        }
        if (controllerInstance.getPlatform() != IDEController.TEST) {
            log.debug("IDEController.instance set to " + controllerInstance.getClass().getName());
        }
        if (needToSearchForCache && controllerInstance.getPlatform() != IDEController.TEST) {
            searchForCache();
        }
    }

    private static void searchForCache() {
        new Thread(new Runnable() {

            public void run() {
                long start = System.currentTimeMillis();
                Object project = null;
                Object cachePath = null;
                while (true) {
                    if (project == null) {
                        try {
                            project = instance.getActiveProjectFromIDE();
                        } catch (Exception e) {
                        }
                    }
                    if (cachePath == null) {
                        try {
                            if (project != null) {
                                cachePath = instance.getCachePathForActiveProject(project);
                            }
                        } catch (Exception e) {
                        }
                    }
                    if (project != null && cachePath != null) {
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        AppRegistry.getExceptionLogger().error(e, this);
                    }
                    if (System.currentTimeMillis() - start > 60000) {
                        break;
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    AppRegistry.getExceptionLogger().error(e, this);
                }
                if (project != null && cachePath != null) {
                    log.debug("Finally project: " + project + " after " + (System.currentTimeMillis() - start) + " ms");
                    synchronized (instance.projectsCache) {
                        if (instance.projectsCache.size() == 0) {
                            ASTTreeCache treeCache = instance.deserializeCacheImpl(false, cachePath);
                            if (treeCache != null) {
                                instance.projectsCache.put(project, treeCache);
                                log.debug("Deserialized cache: " + project + " after " + (System.currentTimeMillis() - start) + " ms");
                            }
                        }
                    }
                } else {
                    log.debug("Deserializing failed: " + project + ", cachePath: " + cachePath);
                }
            }
        }).start();
    }

    /**
   * @param cacheDir
   * @return full path to the file
   */
    public static String generateNewCacheFileName(String cacheDir) {
        String cache = "cache_" + Math.abs((new Random()).nextInt());
        String projectHome = cacheDir;
        projectHome += File.separator;
        projectHome += "Cache";
        projectHome += File.separator;
        projectHome += cache;
        return projectHome;
    }

    /**
   * precond: project != null
   * @param newProject
   */
    public void setActiveProject(Project newProject) {
        if (getPlatform() == TEST) {
            this.activeProject = newProject;
        } else {
            Project oldProject = this.activeProject;
            this.activeProject = newProject;
            if (newProject != oldProject) {
                onProjectChanged(oldProject);
                if (oldProject != null) {
                    if (!serializeProjectCache(oldProject, false)) {
                        log.debug("error: project serializing failed!!!");
                    }
                    oldProject.cleanClassmodel();
                }
                newProject.getProjectLoader().markProjectForRebuild();
                this.activeIdeProject = getActiveProjectFromIDE();
                activeProjectCacheLoaded = false;
            } else {
                log.warn("setActiveProject called with newProject same as old!!");
                return;
            }
        }
    }

    protected void beforeEnsureProject() {
    }

    /**
   * Called when activeProject was changed, note that
   * getActiveProject will already return new project
   *
   * @param oldProject
   */
    protected void onProjectChanged(Project oldProject) {
        RitUndoManager.clear();
        MilestoneManager.clear();
    }

    /**
   * Returns (current, cached) IDE project corresponding to getActiveProject().
   * NB! To get project directly from IDE use {@link #getActiveProjectFromIDE}
   */
    public final Object getIDEProject() {
        return this.activeIdeProject;
    }

    public boolean checkClassPathSanity(ClassPath classpath, boolean showDialogIfNeeded) {
        if (!classpath.contains("java/lang/Object.class")) {
            if (showDialogIfNeeded) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        RitDialog.showMessageDialog(createProjectContext(), "RefactorIT: Please fix your classpath (under" + " \"RefactorIT Project Options\"), it does not" + " contain java.lang.Object", "Classpath error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            return false;
        }
        return true;
    }

    /**
   * Probably should make static and move to util class
   *
   * @param srcPath
   * @param showDialogIfNeeded
   */
    public boolean checkSourcePathSanity(SourcePath srcPath, boolean showDialogIfNeeded) {
        if (srcPath.getRootSources().length == 0) {
            if (showDialogIfNeeded) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        RitDialog.showMessageDialog(createProjectContext(), "RefactorIT: Please fix your sourcepath (under" + " \"RefactorIT Project Options\"), it is currently empty!", "Sourcepath error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
            return false;
        }
        return true;
    }

    public abstract MenuBuilder createMenuBuilder(String name, char mnemonic, String icon, boolean submenu);

    public static boolean runningStandalone() {
        return checkPlatform(STANDALONE);
    }

    /**
   * Saves all modified editors
   *
   * @return true if succeeded
   */
    public abstract boolean saveAllFiles();

    /**
   * @return Returns the ideBuild.
   */
    public String getIdeBuild() {
        return ideBuild;
    }

    /**
   * @param ideBuild The ideBuild to set.
   */
    public void setIdeBuild(String ideBuild) {
        this.ideBuild = ideBuild;
    }

    /**
   * @return Returns the ideName.
   */
    public String getIdeName() {
        return ideName;
    }

    /**
   * @param ideName The ideName to set.
   */
    public void setIdeName(String ideName) {
        this.ideName = ideName;
    }

    /**
   * @return Returns the ideVersion.
   */
    public String getIdeVersion() {
        return ideVersion;
    }

    /**
   * @param ideVersion The ideVersion to set.
   */
    public void setIdeVersion(String ideVersion) {
        this.ideVersion = ideVersion;
    }

    public XMLSerializer getXMLSerializer() {
        return XMLSerializer.getDefaultSerializer();
    }

    /**
   *
   * @return <code>true</code> if we are able to cause the running IDE to exit
   */
    public boolean isExitIdePossible() {
        return false;
    }

    /**
   * Causes IDE to exit.
   *
   * @throws UnsupportedOperationException if we cannot exit IDE
   */
    public void exitIde() {
        throw new UnsupportedOperationException("Cannot exit IDE");
    }

    public void addIgnoredSources(Project pr, Source[] sourcePaths) {
        ProjectOptions projectOptions = pr.getOptions();
        Path ignoredSourcePath = projectOptions.getIgnoredSourcePath();
        for (int i = 0; i < sourcePaths.length; i++) {
            ignoredSourcePath.addItem(new PathItem(sourcePaths[i]));
        }
        pr.fireProjectSettingsChangedEvent();
    }

    /**
   * Runs atomic sources modification operation in IDE. Implementations
   * should batch all code modifications to target sources, supressing
   * sending modification events if possible. All source modifications
   * should be executed done using this method.
   *
   * @param op
   *
   * TODO: make abstract and override this for all IDE-s
   */
    public void run(final SourcesModificationOperation op) {
        op.run();
    }

    public WorkspaceManager getWorkspaceManager() {
        return DefaultWorkspaceManager.getInstance();
    }

    public final Workspace getWorkspace() {
        return getWorkspaceManager().getWorkspace();
    }

    protected boolean checkParsingPreconditions(Project pr) {
        return true;
    }

    /**
   * @return IDE specific warning about low memory and
   * on how to increase it
   */
    public String getLowMemoryWarning(int recommendedInMBs) {
        return "Use the -Xmx" + recommendedInMBs + "M JVM option to allow the IDE access the recommended amount of memory.";
    }

    /**
   * @deprecated is used to make some tests still work
   */
    public Project getCachedActiveProject() {
        return this.activeProject;
    }

    public void showAndLogInternalError(Throwable t) {
        throw new RuntimeException(t);
    }
}
