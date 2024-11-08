package org.apache.maven.plugin;

import com.werken.forehead.Forehead;
import com.werken.forehead.ForeheadClassLoader;
import com.werken.werkz.NoSuchGoalException;
import com.werken.werkz.Session;
import com.werken.werkz.WerkzProject;
import com.werken.werkz.jelly.JellySession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.expression.Expression;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.AbstractMavenComponent;
import org.apache.maven.AntProjectBuilder;
import org.apache.maven.MavenConstants;
import org.apache.maven.MavenException;
import org.apache.maven.MavenSession;
import org.apache.maven.MavenUtils;
import org.apache.maven.UnknownGoalException;
import org.apache.maven.jelly.JellyUtils;
import org.apache.maven.jelly.MavenJellyContext;
import org.apache.maven.project.Dependency;
import org.apache.maven.project.Project;
import org.apache.maven.repository.Artifact;
import org.apache.maven.util.Expand;

/*********************************************************************
 * Plugin manager for MavenSession.
 * <p>
 * <p/>The <code>PluginManager</code> deals with all aspects of a
 * plugins lifecycle.
 * </p>
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl </a>
 * @author <a href="mailto:bob@eng.werken.com">bob mcwhirter </a>
 * @author <a href="mailto:brett@apache.org">Brett Porter </a>
 * @version $Id: PluginManager.java,v 1.70.4.45 2004/05/11 09:48:16
 *          brett Exp $
 */
public class PluginManager extends AbstractMavenComponent {

    /** * Logger */
    private static final Log log = LogFactory.getLog(PluginManager.class);

    /*****************************************************************
     * The variable key that holds an implementation of the
     * <code>com.werken.werkz.Session</code> in the parent scope
     * context.
     */
    public static final String GLOBAL_SESSION_KEY = "maven.session.global";

    public static final String PLUGIN_MANAGER = "maven.plugin.manager";

    public static final String PLUGIN_HOUSING = "maven.plugin.script.housing";

    public static final String GOAL_MAPPER = "maven.plugin.mapper";

    /** * */
    public static final String BASE_CONTEXT = "maven.goalAttainmentContext";

    /** * The directory where plugin jars reside under Maven's home. */
    private File pluginsDir;

    /** * The directory where the plugin jars are unpacked to. */
    private File unpackedPluginsDir;

    /** * This contains a map of plugins, keyed by id. */
    private final Map pluginHousings = new HashMap();

    /** * This contains a map of plugins, keyed by artifact id. */
    private final Map artifactIdToHousingMap = new HashMap();

    /** * Maven session reference. */
    private MavenSession mavenSession;

    /** * Plugin cache manager. */
    private final PluginCacheManager cacheManager = new PluginCacheManager();

    /** * Goal to Plugins mapper. */
    private GoalToJellyScriptHousingMapper mapper = new GoalToJellyScriptHousingMapper();

    /** * Current plugins mapper (transient - includes maven.xml, etc). * */
    private GoalToJellyScriptHousingMapper transientMapper = mapper;

    /** * Plugins to be popped afterwards. */
    private Set delayedPops = new HashSet();

    /*****************************************************************
     * Default constructor.
     * @param session The MavenSession this plugin manager will use
     *        until Maven shuts down.
     */
    public PluginManager(MavenSession session) {
        mavenSession = session;
    }

    /*****************************************************************
     * Get the list of plugin files.
     */
    private Map getPluginFiles(File directory, boolean acceptDirectories) throws MavenException {
        File[] files = directory.listFiles();
        if (files == null) {
            return Collections.EMPTY_MAP;
        }
        Map pluginFiles = new HashMap();
        for (int i = 0; i < files.length; i++) {
            String plugin = files[i].getName();
            if (files[i].isDirectory() && acceptDirectories) {
                pluginFiles.put(plugin, files[i]);
            } else {
                int index = plugin.indexOf(".jar");
                if (index >= 0) {
                    String name = plugin.substring(0, index);
                    pluginFiles.put(name, files[i]);
                }
            }
        }
        return pluginFiles;
    }

    /*****************************************************************
     * Load plugins.
     * @throws MavenException when the plugin jars can't be expanded
     */
    private void loadUncachedPlugins(Map pluginFiles) throws IOException, MavenException {
        log.debug("Now loading uncached plugins");
        for (Iterator i = pluginFiles.keySet().iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            File pluginDir = (File) pluginFiles.get(name);
            if (!isLoaded(name)) {
                JellyScriptHousing housing = createPluginHousing(pluginDir);
                if (housing != null) {
                    cacheManager.registerPlugin(name, housing);
                    housing.parse(cacheManager);
                    housing.parse(mapper);
                }
            }
        }
    }

    /*****************************************************************
     * Initialize all plugins.
     * @throws IOException If an error occurs while initializing any
     *         plugin.
     */
    public void initialize() throws IOException, MavenException {
        if (log.isDebugEnabled()) {
            log.debug("Initializing Plugins!");
        }
        setPluginsDir(new File(mavenSession.getRootContext().getPluginsDir()));
        setUnpackedPluginsDir(new File(mavenSession.getRootContext().getUnpackedPluginsDir()));
        if (log.isDebugEnabled()) {
            log.debug("Set plugin source directory to " + getPluginsDir().getAbsolutePath());
            log.debug("Set unpacked plugin directory to " + getUnpackedPluginsDir().getAbsolutePath());
        }
        Map pluginFiles = getPluginFiles(pluginsDir, true);
        pluginFiles.putAll(getPluginFiles(unpackedPluginsDir, false));
        Map pluginDirs = expandPluginFiles(pluginFiles);
        cacheManager.loadCache(unpackedPluginsDir);
        log.debug("Now mapping cached plugins");
        if (!cacheManager.mapPlugins(mapper, this, pluginDirs)) {
            log.info("Cache invalidated due to out of date plugins");
            for (Iterator i = pluginHousings.values().iterator(); i.hasNext(); ) {
                JellyScriptHousing housing = (JellyScriptHousing) i.next();
                cacheManager.registerPlugin(housing.getName(), housing);
                housing.parse(cacheManager);
                housing.parse(mapper);
            }
        }
        loadUncachedPlugins(pluginDirs);
        cacheManager.saveCache(unpackedPluginsDir);
        log.debug("Finished initializing Plugins!");
    }

    private Map expandPluginFiles(Map pluginFiles) throws MavenException {
        Map pluginDirs = new HashMap();
        for (Iterator i = pluginFiles.keySet().iterator(); i.hasNext(); ) {
            String name = (String) i.next();
            File jarFile = (File) pluginFiles.get(name);
            File dir = jarFile.isDirectory() ? jarFile : unpackPlugin(name, jarFile);
            pluginDirs.put(name, dir);
        }
        return pluginDirs;
    }

    JellyScriptHousing loadPluginHousing(String name, File pluginDir) throws IOException {
        JellyScriptHousing housing = (JellyScriptHousing) pluginHousings.get(name);
        return (housing == null ? createLazyPluginHousing(pluginDir) : housing);
    }

    private JellyScriptHousing createPluginHousing(File pluginDir) throws MavenException, IOException {
        JellyScriptHousing housing = createLazyPluginHousing(pluginDir);
        if (housing != null) {
            String artifactId = housing.getProject().getArtifactId();
            mapArtifactIdToPluginHousing(artifactId, housing);
        }
        return housing;
    }

    void mapArtifactIdToPluginHousing(String artifactId, JellyScriptHousing housing) {
        artifactIdToHousingMap.put(artifactId, housing);
    }

    private JellyScriptHousing createLazyPluginHousing(File pluginDir) throws IOException {
        if (!pluginDir.isDirectory() || !new File(pluginDir, "project.xml").exists()) {
            log.debug("Not a plugin directory: " + pluginDir);
            return null;
        }
        String pluginName = pluginDir.getName();
        log.debug("Loading plugin '" + pluginName + "'");
        JellyScriptHousing jellyScriptHousing = new JellyScriptHousing(pluginDir, mavenSession.getRootContext());
        pluginHousings.put(pluginName, jellyScriptHousing);
        return jellyScriptHousing;
    }

    private boolean isLoaded(String name) {
        return pluginHousings.containsKey(name);
    }

    /***
     * @param project
     * @param unpackedPluginDirectory
     * @param jelly
     * @return
     * @throws Exception
     * @todo [1.0] refactor into housing
     * @deprecated get rid of this - it duplicates functionality in the housing
     * @todo don't throw Exception
     */
    private JellyScriptHousing createJellyScriptHousing(Project project, InputStream jelly) throws Exception {
        JellyScriptHousing jellyScriptHousing = new JellyScriptHousing();
        Script script = JellyUtils.compileScript(jelly, project.getContext());
        jellyScriptHousing.setProject(project);
        jellyScriptHousing.setScript(script);
        return jellyScriptHousing;
    }

    /***
     * @param project
     * @param classesDirectory
     * @param jelly
     * @return
     * @todo [1.0] into the housing?
     */
    private JellyScriptHousing createJellyScriptHousing(Project project, File jelly) {
        JellyScriptHousing jellyScriptHousing = new JellyScriptHousing();
        jellyScriptHousing.setProject(project);
        jellyScriptHousing.setSource(jelly);
        return jellyScriptHousing;
    }

    /*****************************************************************
     * Process the dependencies of the project, adding dependencies to
     * the appropriate classloader etc
     * @throws MalformedURLException if a file can't be converted to a
     *         URL.
     * @throws Exception for any other issue. FIXME
     */
    public void processDependencies(Project project) throws MalformedURLException, Exception {
        if (project.getArtifacts() == null) {
            log.debug("No dependencies to process for project " + project.getName());
            return;
        }
        ForeheadClassLoader projectClassLoader = (ForeheadClassLoader) project.getContext().getClassLoader();
        log.debug("Processing dependencies for project " + project.getName() + "; classloader " + projectClassLoader);
        for (Iterator i = project.getArtifacts().iterator(); i.hasNext(); ) {
            Artifact artifact = (Artifact) i.next();
            Dependency dependency = artifact.getDependency();
            if (dependency.isPlugin()) {
                installPlugin(artifact.getFile(), project);
            }
            String dependencyClassLoader = dependency.getProperty("classloader");
            if (artifact.exists()) {
                if (dependency.isAddedToClasspath()) {
                    if (dependencyClassLoader != null) {
                        log.debug("DEPRECATION: " + dependency.getId() + " in project " + project.getId() + " forces the classloader '" + dependencyClassLoader + "'");
                        log.debug("             This behaviour is deprecated. Please refer to the FAQ");
                        ForeheadClassLoader loader = Forehead.getInstance().getClassLoader(dependencyClassLoader);
                        if (loader == null) {
                            log.warn("classloader '" + dependencyClassLoader + "' not found. Adding dependencies to the project classloader instead");
                            loader = projectClassLoader;
                        } else {
                            log.debug("poking dependency " + artifact.getFile() + " into classloader " + dependencyClassLoader);
                        }
                        loader.addURL(artifact.getFile().toURL());
                    } else {
                        log.debug("adding dependency " + artifact.getFile() + " into project classloader");
                        projectClassLoader.addURL(artifact.getFile().toURL());
                    }
                } else {
                    log.debug("Non classpath dependency: '" + artifact.getFile() + "' not added to classpath");
                }
            } else {
                log.info("Artifact '" + artifact.getFile() + "' not found to add to classpath");
            }
        }
        project.getContext().setClassLoader(projectClassLoader);
    }

    List readMavenXml(Project project, GoalToJellyScriptHousingMapper mapper) throws MavenException {
        Project p = project;
        List projectHousings = new ArrayList();
        while (p != null) {
            if (p.hasMavenXml()) {
                File mavenXml = p.getMavenXml();
                JellyScriptHousing jellyScriptHousing = createJellyScriptHousing(project, mavenXml);
                jellyScriptHousing.parse(mapper);
                projectHousings.add(jellyScriptHousing);
            }
            p = p.getParent();
        }
        return projectHousings;
    }

    /***
     * Attain the goals.
     *
     * @throws Exception
     *                   If one of the specified
     *                   goals refers to an non-existent goal.
     * @throws Exception If an exception occurs while running a goal.
     * @todo stop throwing Exception
     */
    public void attainGoals(Project project, List goals) throws Exception {
        MavenJellyContext baseContext = new MavenJellyContext(mavenSession.getRootContext());
        baseContext.setInherit(true);
        JellyUtils.populateVariables(baseContext, project.getContext());
        project.pushContext(baseContext);
        baseContext.setProject(project);
        project.verifyDependencies();
        AntProjectBuilder.build(project, baseContext);
        transientMapper = new GoalToJellyScriptHousingMapper();
        Session session = new JellySession(baseContext.getXMLOutput());
        session.setAttribute(BASE_CONTEXT, baseContext);
        session.setAttribute(PLUGIN_MANAGER, this);
        session.setAttribute(GOAL_MAPPER, transientMapper);
        baseContext.setVariable(GLOBAL_SESSION_KEY, session);
        InputStream driver = getClass().getResourceAsStream("/driver.jelly");
        JellyScriptHousing driverHousing = createJellyScriptHousing(project, driver);
        driver.close();
        driver = getClass().getResourceAsStream("/driver.jelly");
        driverHousing.parse(transientMapper, null, driver);
        driver.close();
        List projectHousings = readMavenXml(project, transientMapper);
        if (goals != null) {
            for (Iterator i = goals.iterator(); i.hasNext(); ) {
                String goal = (String) i.next();
                if (goal.trim().length() == 0) {
                    i.remove();
                }
            }
        }
        String defaultGoalName = transientMapper.getDefaultGoalName();
        if (defaultGoalName != null) {
            Expression e = JellyUtils.decomposeExpression(defaultGoalName, baseContext);
            defaultGoalName = e.evaluateAsString(baseContext);
            baseContext.setVariable(MavenConstants.DEFAULT_GOAL, defaultGoalName);
            if (goals != null && goals.size() == 0) {
                log.debug("Using default goal: " + defaultGoalName);
                goals.add(defaultGoalName);
            }
        }
        if (goals == null) {
            goals = Collections.EMPTY_LIST;
        } else {
            goals.add(0, "build:start");
            goals.add("build:end");
        }
        transientMapper.merge(mapper);
        WerkzProject werkzProject = new WerkzProject();
        baseContext.setWerkzProject(werkzProject);
        Set pluginSet = new HashSet();
        Set oldDelayedPops = new HashSet(delayedPops);
        delayedPops.clear();
        Thread.currentThread().setContextClassLoader(null);
        try {
            runScript(driverHousing, baseContext);
            transientMapper.addResolvedPlugins(Collections.singletonList(driverHousing));
            for (Iterator j = projectHousings.iterator(); j.hasNext(); ) {
                JellyScriptHousing housing = (JellyScriptHousing) j.next();
                runScript(housing, baseContext);
            }
            transientMapper.addResolvedPlugins(projectHousings);
            for (Iterator i = goals.iterator(); i.hasNext(); ) {
                String goalName = (String) i.next();
                pluginSet.addAll(prepAttainGoal(goalName, baseContext, transientMapper));
            }
            for (Iterator i = goals.iterator(); i.hasNext(); ) {
                String goalName = (String) i.next();
                log.debug("attaining goal " + goalName);
                try {
                    werkzProject.attainGoal(goalName, session);
                } catch (NoSuchGoalException e) {
                    throw new UnknownGoalException(goalName);
                }
            }
        } finally {
            cleanupAttainGoal(pluginSet);
            delayedPops = oldDelayedPops;
            project.popContext();
        }
    }

    /***
     * @todo don't throw Exception
     */
    public void cleanupAttainGoal(Set pluginSet) throws Exception {
        delayedPops.addAll(pluginSet);
        for (Iterator j = delayedPops.iterator(); j.hasNext(); ) {
            JellyScriptHousing housing = (JellyScriptHousing) j.next();
            housing.getProject().popContext();
        }
        delayedPops.clear();
    }

    /***
     * Use the name of a goal to lookup all the plugins (that are stored in the plugin housings) that need to be
     * executed in order to satisfy all the required preconditions for successful goal attainment.
     *
     * @param goalName    the goal
     * @param baseContext the base context to attain in
     * @return a set of plugins required to attain the goal
     * @throws Exception 
     * @todo don't throw Exception
     */
    public Set prepAttainGoal(String goalName, MavenJellyContext baseContext, GoalToJellyScriptHousingMapper goalMapper) throws Exception {
        Set pluginSet = goalMapper.resolveJellyScriptHousings(goalName);
        for (Iterator j = pluginSet.iterator(); j.hasNext(); ) {
            JellyScriptHousing housing = (JellyScriptHousing) j.next();
            Project project = housing.getProject();
            MavenUtils.integrateMapInContext(housing.getPluginProperties(), baseContext);
            MavenJellyContext pluginContext = new MavenJellyContext(baseContext);
            project.pushContext(pluginContext);
            pluginContext.setInherit(true);
            pluginContext.setVariable("context", pluginContext);
            pluginContext.setVariable("plugin", project);
            pluginContext.setVariable("plugin.dir", housing.getPluginDirectory());
            pluginContext.setVariable("plugin.resources", new File(housing.getPluginDirectory(), "plugin-resources"));
            log.debug("initialising plugin housing: " + project);
            runScript(housing, pluginContext);
        }
        return pluginSet;
    }

    /*****************************************************************
     * Sets the pluginsDir attribute of the PluginManager object
     * @param dir The maven plugin directory.
     */
    private void setPluginsDir(File dir) {
        pluginsDir = dir;
    }

    /*****************************************************************
     * Retrieve the directory containing all plugins.
     * @return The directory containing all plugins.
     */
    private File getPluginsDir() {
        return pluginsDir;
    }

    /*****************************************************************
     * Sets the directory where the unpacked plugins are located.
     * @param dir The directory where the unpacked plugins are
     *        located.
     */
    private void setUnpackedPluginsDir(File dir) {
        unpackedPluginsDir = dir;
    }

    /*****************************************************************
     * Sets the directory where the unpacked plugins are located.
     * @return the directory where the unpacked plugins are located.
     */
    private File getUnpackedPluginsDir() {
        return unpackedPluginsDir;
    }

    /*****************************************************************
     * @return
     */
    public Set getGoalNames() {
        return mapper.getGoalNames();
    }

    /***
     * Warning - this completely scrogs the default mapper. Only use this before System.exit!
     * (currently used by maven -u).
     * @todo refactor to return mapper instead and use that, or perhaps instantiate a new plugin manager
     * @return
     */
    public Set getGoalNames(Project project) throws MavenException {
        mapper = new GoalToJellyScriptHousingMapper();
        readMavenXml(project, mapper);
        return mapper.getGoalNames();
    }

    /***
     */
    public void installPlugin(File file, Project parentProject) throws MavenException {
        installPlugin(file, parentProject, false);
    }

    /***
     * Load and install a plugin.
     *
     * @param file          the file to install. Must be a plugin jar
     * @param parentProject the project to load the installed plugin into
     * @todo remove any old one
     */
    public void installPlugin(File file, Project parentProject, boolean installToUnpackedPluginDirectory) throws MavenException {
        log.debug("Using plugin dependency: " + file);
        try {
            if (installToUnpackedPluginDirectory) {
                FileUtils.copyFileToDirectory(file, unpackedPluginsDir);
            }
            String pluginName = file.getCanonicalFile().getName();
            pluginName = pluginName.substring(0, pluginName.indexOf(".jar"));
            if (!isLoaded(pluginName)) {
                File unpackedPluginDir = unpackPlugin(pluginName, file);
                if (unpackedPluginDir != null) {
                    JellyScriptHousing housing = createPluginHousing(unpackedPluginDir);
                    if (housing == null) {
                        throw new MavenException("Not a valid plugin file: " + file);
                    }
                    housing.parse(transientMapper);
                    housing.parse(mapper);
                    if (installToUnpackedPluginDirectory) {
                        cacheManager.registerPlugin(pluginName, housing);
                        housing.parse(cacheManager);
                        cacheManager.saveCache(unpackedPluginsDir);
                    }
                } else {
                    throw new MavenException("Not a valid JAR file: " + file);
                }
            }
        } catch (IOException e) {
            throw new MavenException("Error installing plugin", e);
        }
    }

    public void uninstallPlugin(String artifactId) throws IOException {
        log.debug("Uninstalling plugin: " + artifactId);
        JellyScriptHousing housing = (JellyScriptHousing) artifactIdToHousingMap.get(artifactId);
        if (housing == null) {
            log.warn("Plugin not found when attempting to uninstall '" + artifactId + "'");
            return;
        }
        String name = housing.getName();
        pluginHousings.remove(name);
        cacheManager.invalidateCache(name);
        mapper.invalidatePlugin(housing);
        transientMapper.invalidatePlugin(housing);
        artifactIdToHousingMap.remove(artifactId);
        cacheManager.saveCache(unpackedPluginsDir);
    }

    /***
     * @todo [1.0] refactor out, or make more appropriate structure
     * @param id
     * @return
     * @throws UnknownPluginException
     * @todo remove throws Exception
     */
    public MavenJellyContext getPluginContext(String id) throws MavenException, UnknownPluginException {
        JellyScriptHousing housing = (JellyScriptHousing) artifactIdToHousingMap.get(id);
        if (housing != null) {
            Project project = housing.getProject();
            return project.getContext();
        }
        throw new UnknownPluginException(id);
    }

    public String getGoalDescription(String goalName) {
        return mapper.getGoalDescription(goalName);
    }

    public void addDelayedPops(Set set) {
        delayedPops.addAll(set);
    }

    /*****************************************************************
     * Unpack the plugin.
     * @throws MavenException if there was a problem unpacking
     */
    File unpackPlugin(String pluginName, File jarFile) throws MavenException {
        File unzipDir = new File(unpackedPluginsDir, pluginName);
        if (!unzipDir.exists() || (jarFile.lastModified() > unzipDir.lastModified())) {
            if (log.isDebugEnabled()) {
                log.debug("Unpacking " + jarFile.getName() + " to directory --> " + unzipDir.getAbsolutePath());
            }
            try {
                Expand unzipper = new Expand();
                unzipper.setSrc(jarFile);
                unzipper.setDest(unzipDir);
                unzipper.execute();
            } catch (IOException e) {
                throw new MavenException("Unable to extract plugin: " + jarFile, e);
            }
        }
        return unzipDir;
    }

    /***
     * @todo get rid of throws Exception
     * @return
     */
    private Script loadScript(JellyScriptHousing jellyScriptHousing) throws Exception {
        if (jellyScriptHousing.getPluginDirectory() != null) {
            jellyScriptHousing.getProject().verifyDependencies();
        }
        MavenJellyContext context = jellyScriptHousing.getProject().getContext();
        URL oldRoot = context.getRootURL();
        URL oldCurrent = context.getCurrentURL();
        context.setRootURL(jellyScriptHousing.getSource().toURL());
        context.setCurrentURL(jellyScriptHousing.getSource().toURL());
        Script script = JellyUtils.compileScript(jellyScriptHousing.getSource(), context);
        context.setRootURL(oldRoot);
        context.setCurrentURL(oldCurrent);
        return script;
    }

    /***
     * @param context            
     * @throws Exception 
     * @todo get rid of throws Exception
     */
    void runScript(JellyScriptHousing jellyScriptHousing, MavenJellyContext context) throws Exception {
        log.debug("running script " + jellyScriptHousing.getSource());
        Script s = jellyScriptHousing.getScript();
        if (s == null) {
            s = loadScript(jellyScriptHousing);
            jellyScriptHousing.setScript(s);
        }
        if (context.getVariable(PLUGIN_HOUSING) != null) {
            throw new IllegalStateException("nested plugin housings");
        }
        context.setVariable(PLUGIN_HOUSING, jellyScriptHousing);
        s.run(context, context.getXMLOutput());
        context.removeVariable(PLUGIN_HOUSING);
    }

    public Project getPluginProjectFromGoal(String goal) throws MavenException {
        JellyScriptHousing housing = mapper.getPluginHousing(goal);
        return housing != null ? housing.getProject() : null;
    }

    public Collection getPluginList() {
        Collection list = new ArrayList();
        for (Iterator i = pluginHousings.values().iterator(); i.hasNext(); ) {
            JellyScriptHousing housing = (JellyScriptHousing) i.next();
            list.add(housing.getName());
        }
        return list;
    }
}
