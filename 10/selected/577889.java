package de.sonivis.tool.core;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mysql.jdbc.Driver;
import de.sonivis.tool.core.datamodel.Actor;
import de.sonivis.tool.core.datamodel.ActorContentElementRelation;
import de.sonivis.tool.core.datamodel.ContentElement;
import de.sonivis.tool.core.datamodel.ContextRelation;
import de.sonivis.tool.core.datamodel.Graph;
import de.sonivis.tool.core.datamodel.InfoSpace;
import de.sonivis.tool.core.datamodel.InfoSpaceItem;
import de.sonivis.tool.core.datamodel.InteractionRelation;
import de.sonivis.tool.core.datamodel.exceptions.CannotConnectToDatabaseException;
import de.sonivis.tool.core.datamodel.exceptions.DataModelAnnotationException;
import de.sonivis.tool.core.datamodel.exceptions.DataModelMappingException;
import de.sonivis.tool.core.datamodel.extension.ActorAggregation;
import de.sonivis.tool.core.datamodel.extension.Administrator;
import de.sonivis.tool.core.datamodel.extension.AggregatedBy;
import de.sonivis.tool.core.datamodel.extension.Artificial;
import de.sonivis.tool.core.datamodel.extension.Author;
import de.sonivis.tool.core.datamodel.extension.Collaboration;
import de.sonivis.tool.core.datamodel.extension.Created;
import de.sonivis.tool.core.datamodel.extension.Deleted;
import de.sonivis.tool.core.datamodel.extension.GroupedBy;
import de.sonivis.tool.core.datamodel.extension.GroupingElement;
import de.sonivis.tool.core.datamodel.extension.Human;
import de.sonivis.tool.core.datamodel.extension.LinksTo;
import de.sonivis.tool.core.datamodel.extension.MetaData;
import de.sonivis.tool.core.datamodel.extension.PartOf;
import de.sonivis.tool.core.datamodel.extension.Read;
import de.sonivis.tool.core.datamodel.extension.RevisionElement;
import de.sonivis.tool.core.datamodel.extension.Updated;
import de.sonivis.tool.core.datamodel.networkloader.GenericNetworkLoader;
import de.sonivis.tool.core.eventhandling.EEventType;
import de.sonivis.tool.core.eventhandling.EventManager;
import de.sonivis.tool.core.eventhandling.IListener;
import de.sonivis.tool.core.eventhandling.INetworkFilter;
import de.sonivis.tool.core.exception.InfoSpaceSelectionException;
import de.sonivis.tool.core.exception.NetworkLoaderException;
import de.sonivis.tool.core.gnu.r.RManager;

/**
 * Manager component for SONIVIS:Tool data models.
 * 
 * @author Benedikt
 * @author Andreas Erber
 * @version $Revision: 1625 $, $Date: 2010-04-07 15:22:53 -0400 (Wed, 07 Apr 2010) $
 */
public final class ModelManager implements IListener {

    /**
	 * Singleton instance of the {@link ModelManager}.
	 */
    private static final ModelManager INSTANCE = new ModelManager();

    /**
	 * {@link PropertyChangeSupport} for the model.
	 */
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    /**
	 * Logger for the {@link ModelManager}.
	 */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelManager.class);

    /**
	 * Set of annotated classes extending the data model.
	 */
    private final Set<Class<? extends InfoSpaceItem>> annotatedEntityClasses = new HashSet<Class<? extends InfoSpaceItem>>();

    /**
	 * Hibernate session factory.
	 */
    private SessionFactory factory;

    /**
	 * Name of the host of the used database.
	 */
    private String sonivisDbHostname;

    /**
	 * Name of the port of the used database.
	 */
    private Integer sonivisDbPort;

    /**
	 * Name of the used database schema.
	 */
    private String sonivisDbSchema;

    /**
	 * Name of the database user.
	 */
    private String sonivisDbUser;

    /**
	 * Password of the database user.
	 */
    private String sonivisDbPw;

    /**
	 * Date for which the analyses should begin.
	 */
    private Date startDate;

    /**
	 * Date for which the analyses should end.
	 */
    private Date endDate;

    /**
	 * NetworkFilter for which the analyses should take place.
	 */
    private INetworkFilter filter;

    /**
	 * Current {@link Graph} to work with.
	 */
    private Graph currentGraph;

    /**
	 * Current {@link InfoSpace} to operate in.
	 */
    private InfoSpace currentInfoSpace;

    /**
	 * Settings.
	 */
    private static PreferenceDialog prefDialog;

    /**
	 * Serialization IDs to R IDs mapping.
	 */
    private Map<Long, Long> serialIdToRId = new Hashtable<Long, Long>();

    /**
	 * Flag whether the user started the tool without database connection details.
	 */
    private boolean startupWithoutDb;

    /**
	 * Singleton constructor.
	 */
    private ModelManager() {
        EventManager.getInstance().addListener(this, EEventType.NETWORK_LOAD, EEventType.WORKBENCH_STARTED, EEventType.EXTRACT_FINISHED, EEventType.HIBERNATE_CONFIG_CHANGE);
    }

    /**
	 * Get the singleton instance of the {@link ModelManager}.
	 * 
	 * @return Singleton instance of the {@link ModelManager}.
	 */
    public static synchronized ModelManager getInstance() {
        return INSTANCE;
    }

    /**
	 * 
	 * @param pcl
	 */
    public void addPropertyChangeListener(final PropertyChangeListener pcl) {
        pcs.addPropertyChangeListener(pcl);
    }

    /**
	 * 
	 * @param pcl
	 */
    public void removePropertyChangeListener(final PropertyChangeListener pcl) {
        pcs.removePropertyChangeListener(pcl);
    }

    /**
	 * Sets a new {@link Graph} to become the current one.
	 * 
	 * @param newGraph
	 *            The new working {@link Graph}.
	 */
    public void setCurrentGraph(final Graph newGraph) {
        synchronized (this) {
            this.currentGraph = newGraph;
            pcs.firePropertyChange("currentGraph", null, newGraph);
            if (newGraph != null) {
                try {
                    ModelManager.getInstance().synchModelToR(newGraph);
                } catch (final FileNotFoundException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("saveModelToGraphML failed: File not found.", e);
                    }
                } catch (final IOException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("saveModelToGraphML failed: File not found.", e);
                    }
                }
            }
        }
        EventManager.getInstance().fireEvent(EEventType.GRAPH_CHANGE, newGraph);
    }

    /**
	 * Retrieve the currently set {@link InfoSpace} to operate in.
	 * 
	 * @return The currently set {@link InfoSpace}.
	 */
    public InfoSpace getCurrentInfoSpace() throws InfoSpaceSelectionException {
        if (currentInfoSpace == null) {
            throw new InfoSpaceSelectionException();
        }
        return currentInfoSpace;
    }

    /**
	 * Reset the current {@link InfoSpace}.
	 * 
	 * @param infoSpace
	 *            The new {@link InfoSpace} to be used.
	 */
    public void setCurrentInfoSpace(final InfoSpace infoSpace) {
        synchronized (this) {
            currentInfoSpace = infoSpace;
            pcs.firePropertyChange("currentInfoSpace", null, infoSpace);
        }
    }

    /**
	 * Retrieve the currently set {@link Graph}.
	 * 
	 * @return The currently set {@link Graph} to work with.
	 */
    public Graph getCurrentGraph() {
        synchronized (this) {
            return currentGraph;
        }
    }

    /**
	 * Reset the current network filter.
	 * 
	 * @param filter
	 *            The new {@link INetworkFilter} to be used.
	 */
    public void setCurrentFilter(final INetworkFilter filter) {
        this.filter = filter;
        pcs.firePropertyChange("currentFilter", null, filter);
    }

    /**
	 * Retrieve the current {@link INetworkFilter network filter}.
	 * 
	 * @return The currently set {@link INetworkFilter} to be applied.
	 */
    public INetworkFilter getCurrentFilter() {
        return filter;
    }

    /**
	 * Reset the {@link Date} when analyses should begin.
	 * 
	 * @param startDate
	 *            The starting {@link Date} for analyses.
	 */
    @Deprecated
    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
        pcs.firePropertyChange("startDate", null, startDate);
    }

    /**
	 * Retrieve the {@link Date} when analyses are to begin.
	 * 
	 * @return The current starting {@link Date} for analyses.
	 */
    @Deprecated
    public Date getStartDate() {
        return startDate;
    }

    /**
	 * Reset the {@link Date} when analyses should end.
	 * 
	 * @param endDate
	 *            The {@link Date} when to end analyses.
	 */
    @Deprecated
    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
        pcs.firePropertyChange("endDate", null, endDate);
    }

    /**
	 * Retrieve the {@link Date} when analyses are to end.
	 * 
	 * @return The currently set end {@link Date} for analyses.
	 */
    @Deprecated
    public Date getEndDate() {
        return endDate;
    }

    /**
	 * Load a network according to the given {@link INetworkFilter filter}.
	 * 
	 * @param filter
	 *            Settings for network loading.
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #getCurrentSession()} does
	 * @throws NetworkLoaderException
	 */
    public void loadNetwork(final INetworkFilter filter) throws CannotConnectToDatabaseException, NetworkLoaderException, InfoSpaceSelectionException {
        if (currentGraph != null) {
            currentGraph.dispose();
            this.setCurrentGraph(null);
        }
        if (!filter.getNetworkType().equals(null)) {
            this.setCurrentGraph(new GenericNetworkLoader().loadGraph(filter.getNetworkType(), getCurrentInfoSpace()));
        }
    }

    /**
	 * Writes the actual {@link Graph} as GraphML to file toFile. This method also arranges the
	 * synchronization of the graph with R.
	 * <p>
	 * The upload of the graph to R maps also the ids used in R with the ids from the graph.
	 * 
	 * @param toFile
	 *            {@link File} to write to. If is <code>null</code> or does not exists a temporary
	 *            file will be created.
	 * @param graph
	 *            The {@link Graph} to write. If is <code>null</code> the actual model will be
	 *            written.
	 * @return Returns the {@link File} where {@link Graph} was written to.
	 * @throws FileNotFoundException
	 *             if the <code>toFile</code> cannot be found
	 */
    public File saveModelToGraphML(File toFile, Graph graph) throws FileNotFoundException {
        if (graph == null) {
            synchronized (this) {
                graph = this.currentGraph;
            }
        }
        if ((toFile == null) || !toFile.isFile()) {
            String path = SONIVISCore.getRuntimeFolder().concat(File.separator).concat("temp");
            final File outDir = new File(path);
            if (!outDir.isDirectory()) {
                outDir.mkdir();
            }
            path = path.concat(File.separator).concat("network" + graph.getName() + new Date().getTime() + ".xml");
            toFile = new File(path);
        }
        final GraphMLWriter writer = GraphMLWriter.getInstance();
        try {
            writer.writeToFile(graph, toFile);
        } catch (final IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("IOException in GraphMLWriter", e);
            }
        } catch (final CannotConnectToDatabaseException cctde) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Persistence store is not available.", cctde);
            }
        }
        return toFile;
    }

    /**
	 * Saves given graph as GraphML to filesystem and loads the file into running R instance.
	 * 
	 * @param graph
	 * @throws IOException
	 */
    public void synchModelToR(final Graph graph) throws IOException {
        final File graphml = saveModelToGraphML(null, graph);
        RManager.getInstance().loadNetworkFromGraphML(graphml);
        serialIdToRId = RManager.getInstance().getAllRNodeIDs(graph);
    }

    /**
	 * Retrieve the serialization ID to R ID mapping.
	 * 
	 * @return A mapping of serialization IDs to R IDs.
	 */
    public Map<Long, Long> getIdToRIdMap() {
        return serialIdToRId;
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @see IListener#update(EEventType, java.lang.Object[])
	 */
    @Override
    public void update(final EEventType eventType, final Object... arguments) {
        switch(eventType) {
            case NETWORK_LOAD:
                this.currentGraph = (Graph) arguments[0];
                break;
            case WORKBENCH_STARTED:
                workbenchStarted();
                break;
            case HIBERNATE_CONFIG_CHANGE:
                this.factory.close();
                this.factory = null;
            default:
                break;
        }
    }

    /**
	 * Helper method to initialize the Hibernate {@link SessionFactory}.
	 * <p>
	 * Method checks for the current state of the {@link SessionFactory}. If it was already setup
	 * and not changed since the last call it will be returned directly. Otherwise the configuration
	 * input from the GUI is read and used to setup the connection to the target persistence unit.
	 * Currently this persistence unit is hardwired to be a MySQL 5 Database which might be subject
	 * to future change.
	 * </p>
	 * <p>
	 * The next step includes the mapping files provided by the application in the configuration
	 * before the {@link SessionFactory} is newly created from it. Note, that for the inclusion of
	 * the mapping files temporarily a different class loader is used.
	 * </p>
	 * 
	 * @return A {@link SessionFactory} to handle the Hibernate access to the persistence layer.
	 * @throws CannotConnectToDatabaseException
	 *             if no DB connection can be established with the current settings.
	 * @TODO (by Andreas, not urgent) currently MySQL DB is the hardwired persistence unit - this
	 *       should generally be configurable from the GUI or somewhere else
	 */
    private SessionFactory initializeSessionFactory() throws CannotConnectToDatabaseException {
        if (!this.testCurrentDbConnection()) {
            throw new CannotConnectToDatabaseException("Unable to establish a DB connection with the current settings.");
        }
        if (factory != null && !connectionDetailsChanged() && !factory.isClosed()) {
            return factory;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Setting up SessionFactory.");
        }
        final Properties hibernateProps = this.initHibernateProperties();
        final ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        final Set<File> mappingFiles = this.initMappingFiles();
        final Configuration hibernateConfig = this.initHibernateConfig(hibernateProps, mappingFiles);
        try {
            factory = hibernateConfig.buildSessionFactory();
        } catch (final HibernateException he) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to build Session Factory.");
                he.printStackTrace();
            }
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
        Session sess = null;
        try {
            sess = factory.openSession();
            if (sess.isConnected()) {
                setStartupWithoutDb(true);
            }
        } catch (final HibernateException he) {
            if (LOGGER.isErrorEnabled()) {
                factory.close();
                if (he.getMessage().contains("Cannot open connection")) {
                    setStartupWithoutDb(false);
                    openPreferences(he.getCause().getMessage());
                }
            }
        } finally {
            sess.close();
        }
        this.createDbIndices();
        return factory;
    }

    /**
	 * Set up the Hibernate {@link Configuration}.
	 * <p>
	 * Puts together the Hibernate configuration. It relies on the static {@link #MAPPING_FILES}
	 * collection. In case that {@link Collection} is <code>null</code> or empty an exception will
	 * be thrown.
	 * </p>
	 * 
	 * @throws DataModelMappingException
	 *             in case {@link #MAPPING_FILES} is <code>null</code> or empty in the moment this
	 *             method is executed.
	 */
    private Configuration initHibernateConfig(final Properties hibernateProps, final Set<File> mappingFiles) {
        Configuration hibernateConfig;
        if (annotatedEntityClasses != null && !annotatedEntityClasses.isEmpty()) {
            hibernateConfig = new AnnotationConfiguration().addProperties(hibernateProps);
            for (final Class<?> clazz : annotatedEntityClasses) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding annotated class \"" + clazz.getCanonicalName() + "\" to Hibernate config.");
                }
                hibernateConfig = ((AnnotationConfiguration) hibernateConfig).addAnnotatedClass(clazz);
            }
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Hibernate Config: No annotated classes to be added.");
            }
            hibernateConfig = new Configuration().addProperties(hibernateProps);
        }
        if (mappingFiles == null || mappingFiles.isEmpty()) {
            throw new DataModelMappingException("Could not find any mapping files!");
        } else {
            for (final File mappingFile : mappingFiles) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Adding mapping file \"" + mappingFile.getPath() + "\" to Hibernate config.");
                }
                hibernateConfig = hibernateConfig.addFile(mappingFile);
            }
        }
        return hibernateConfig;
    }

    /**
	 * Collect all the mapping files currently hard wired to the system.
	 * <p>
	 * This method is simply for clearness according to the mapping files. So, the files are nicely
	 * grouped here.
	 * </p>
	 * <p>
	 * This is currently a work around status since some mapping files of plug-ins are contained
	 * here, too. This will be changed soon.
	 * </p>
	 */
    private Set<File> initMappingFiles() {
        String resourcePath = SONIVISCore.getPluginBundlePath(CorePlugin.PLUGIN_ID) + "java/src/de/sonivis/tool/core/datamodel/".replace("/", File.separator);
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new DataModelMappingException("Path to data model mapping files is not available.");
        }
        final Set<File> mappingFiles = new HashSet<File>();
        mappingFiles.add(new File(resourcePath + "Property.hbm.xml"));
        mappingFiles.add(new File(resourcePath + "GraphItem.hbm.xml"));
        mappingFiles.add(new File(resourcePath + "InfoSpace.hbm.xml"));
        this.annotatedEntityClasses.add(InfoSpaceItem.class);
        this.annotatedEntityClasses.add(Actor.class);
        this.annotatedEntityClasses.add(ContentElement.class);
        this.annotatedEntityClasses.add(ContextRelation.class);
        this.annotatedEntityClasses.add(InteractionRelation.class);
        this.annotatedEntityClasses.add(ActorContentElementRelation.class);
        resourcePath = SONIVISCore.getPluginBundlePath(CorePlugin.PLUGIN_ID) + "java/src/de/sonivis/tool/core/datamodel/extension/".replace("/", File.separator);
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new DataModelMappingException("Path to data model extension mapping files is not available.");
        }
        this.annotatedEntityClasses.add(ActorAggregation.class);
        this.annotatedEntityClasses.add(Administrator.class);
        this.annotatedEntityClasses.add(Artificial.class);
        this.annotatedEntityClasses.add(Author.class);
        this.annotatedEntityClasses.add(Human.class);
        this.annotatedEntityClasses.add(MetaData.class);
        this.annotatedEntityClasses.add(RevisionElement.class);
        this.annotatedEntityClasses.add(GroupingElement.class);
        this.annotatedEntityClasses.add(LinksTo.class);
        this.annotatedEntityClasses.add(PartOf.class);
        this.annotatedEntityClasses.add(GroupedBy.class);
        this.annotatedEntityClasses.add(AggregatedBy.class);
        this.annotatedEntityClasses.add(Collaboration.class);
        this.annotatedEntityClasses.add(Created.class);
        this.annotatedEntityClasses.add(Deleted.class);
        this.annotatedEntityClasses.add(Read.class);
        this.annotatedEntityClasses.add(Updated.class);
        return mappingFiles;
    }

    /**
	 * Extend the {@link Collection} of mapping files to use by the specified
	 * <em>additionalMappingFiles</em>.
	 * <p>
	 * Adding new mapping files change the Hibernate configuration. This is then supposed to cause
	 * the current SessionFactory to be destroyed immediately and re-setup with the new
	 * configuration. This functionality is not yet provided so this method does currently not what
	 * it is supposed to.
	 * </p>
	 * 
	 * TODO implement full functionality
	 * 
	 * @param additionalMappingFiles
	 *            A {@link Collection} of additional mapping files from plug-ins.
	 */
    public void addMappingFiles(final Collection<File> additionalMappingFiles, final Set<File> mappingFiles) {
        if (additionalMappingFiles == null || additionalMappingFiles.isEmpty()) {
            return;
        }
        final int fileCount = additionalMappingFiles.size();
        for (final File file : additionalMappingFiles) {
            if (file.getName().endsWith(".hbm.xml")) {
                mappingFiles.add(file);
            }
        }
        if (fileCount < additionalMappingFiles.size() && this.factory != null) {
            EventManager.getInstance().fireEvent(EEventType.HIBERNATE_CONFIG_CHANGE);
        }
    }

    /**
	 * Initialize Hibernate {@link Properties}.
	 * <p>
	 * The {@link Properties} are completely set up anew. Any settings done before are wiped out.
	 * The method uses the settings from {@link DataModelPreferences}.
	 * </p>
	 */
    private Properties initHibernateProperties() {
        sonivisDbHostname = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_HOSTNAME);
        sonivisDbPort = CorePlugin.getDefault().getPreferenceStore().getInt(DataModelPreferencesControl.INTERNAL_DB_PORT);
        final StringBuilder url = new StringBuilder(sonivisDbHostname);
        if (sonivisDbPort != null && sonivisDbPort != 0) {
            url.append(":" + sonivisDbPort);
        }
        sonivisDbSchema = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DATABASE);
        sonivisDbUser = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_USERNAME);
        sonivisDbPw = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_PASSWORD);
        final Properties hibernateProps = new Properties();
        hibernateProps.setProperty("hibernate.connection.url", "jdbc:mysql://" + url + "/" + sonivisDbSchema);
        hibernateProps.setProperty("hibernate.connection.characterEncoding", "UTF-8");
        hibernateProps.setProperty("hibernate.connection.useUnicode", "true");
        hibernateProps.setProperty(Environment.USER, sonivisDbUser);
        hibernateProps.setProperty(Environment.PASS, sonivisDbPw);
        hibernateProps.setProperty(Environment.DIALECT, "org.hibernate.dialect.MySQL5InnoDBDialect");
        hibernateProps.setProperty(Environment.DRIVER, "com.mysql.jdbc.Driver");
        hibernateProps.setProperty(Environment.HBM2DDL_AUTO, "update");
        hibernateProps.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");
        hibernateProps.setProperty(Environment.RELEASE_CONNECTIONS, "auto");
        hibernateProps.setProperty(Environment.CONNECTION_PROVIDER, "org.hibernate.connection.C3P0ConnectionProvider");
        hibernateProps.setProperty(Environment.C3P0_MIN_SIZE, "5");
        hibernateProps.setProperty(Environment.C3P0_MAX_SIZE, "20");
        hibernateProps.setProperty(Environment.C3P0_TIMEOUT, "3600");
        hibernateProps.setProperty(Environment.C3P0_MAX_STATEMENTS, "120");
        hibernateProps.setProperty("hibernate.c3p0.idle_test_period", "3600");
        hibernateProps.setProperty("hibernate.c3p0.acquire_increment", "5");
        hibernateProps.setProperty("hibernate.c3p0.maxIdleTimeExcessConnections", "300");
        hibernateProps.setProperty("hibernate.c3p0.numHelperThreads", "10");
        hibernateProps.setProperty("hibernate.c3p0.maxAdministrativeTaskTime", "1800");
        hibernateProps.setProperty("hibernate.c3p0.acquireRetryAttempts", "30");
        return hibernateProps;
    }

    /**
	 * This method opens a dialog in which the user can decide to start with a database connection
	 * or without.
	 * 
	 * @param message
	 */
    private void openPreferences(final String message) {
        Display display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        if (display != null) {
            final Shell shell = display.getActiveShell();
            final String[] buttons = new String[] { "Set internal Database", "Start without internal Database" };
            final MessageDialog dialog = new MessageDialog(shell, "DB Connection Error", Display.getCurrent().getSystemImage(SWT.ICON_WARNING), "There was an error connecting to database. Please check connection details.\n\n" + (message.length() < 300 ? message : message.substring(0, 300)) + "...", MessageDialog.WARNING, buttons, 0) {

                @Override
                protected void buttonPressed(final int buttonId) {
                    if (buttonId == 0) {
                        if (prefDialog != null) {
                            prefDialog.updateButtons();
                        }
                        close();
                        setStartupWithoutDb(false);
                        prefDialog = SONIVISCore.getInstance().getPreferenceDialog(null, "de.sonivis.tool.core.datamodel.DataModelPreferences", true);
                        prefDialog.open();
                    } else if (buttonId == 1) {
                        close();
                        setStartupWithoutDb(true);
                    }
                }
            };
            dialog.open();
        }
    }

    /**
	 * Check if connection details have changed in GUI (preference page "connectors")
	 * 
	 * @return true if connection details have changed, false if connection details have not changed
	 */
    private boolean connectionDetailsChanged() {
        if (sonivisDbHostname != CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_HOSTNAME) || sonivisDbSchema != CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DATABASE) || sonivisDbUser != CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_USERNAME) || sonivisDbPw != CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_PASSWORD)) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Returns a {@link Session} to access the persistence store.
	 * <p>
	 * Note, this does not return {@link SessionFactory#getCurrentSession()} rather wraps
	 * {@link SessionFactory#openSession()}.
	 * </p>
	 * 
	 * @return A {@link Session} for acting upon the persistence layer.
	 * @throws CannotConnectToDatabaseException
	 *             if {@link #initializeSessionFactory()} does.
	 */
    public synchronized Session getCurrentSession() throws CannotConnectToDatabaseException {
        return initializeSessionFactory().openSession();
    }

    private void workbenchStarted() {
        if (isStartupWithoutDb() & testCurrentDbConnection()) {
            EventManager.getInstance().fireEvent(EEventType.CONNECTION_CHANGE);
        }
    }

    public void setStartupWithoutDb(final boolean status) {
        startupWithoutDb = status;
    }

    public boolean isStartupWithoutDb() {
        return startupWithoutDb;
    }

    /**
	 * Test if current configured database can be connected.
	 * 
	 * @return <code>true</code> if connection can be established.
	 */
    public boolean testCurrentDbConnection() {
        final String hostname = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_HOSTNAME);
        final Integer port = CorePlugin.getDefault().getPreferenceStore().getInt(DataModelPreferencesControl.INTERNAL_DB_PORT);
        final String schema = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DATABASE);
        final String user = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_USERNAME);
        final String password = CorePlugin.getDefault().getPreferenceStore().getString(DataModelPreferencesControl.INTERNAL_DB_PASSWORD);
        try {
            return testDbConnection(hostname, schema, user, password, port, false);
        } catch (final CannotConnectToDatabaseException e) {
            return false;
        }
    }

    /**
	 * Test a connection to a database with the given parameters.
	 * 
	 * @param host
	 *            Host URL where the database server resides.
	 * @param schema
	 *            Schema for the database-connection.
	 * @param userName
	 *            Name of the user to log in with.
	 * @param password
	 *            Password of the user to log in with.
	 * @param port
	 *            Optional port number.
	 * @param setAsCurrentDb
	 *            Flag if given connection details should be set as current DB settings.
	 * @return true if a connection could be established
	 * @throws CannotConnectToDatabaseException
	 * @throws IllegalArgumentException
	 *             if either <em>host</em>, <em>userName</em>, or <em>password</em> are
	 *             <code>null</code> or empty.
	 * @throws CannotConnectToDatabaseException
	 *             if attempt to connect according to the given arguments fails.
	 */
    public boolean testDbConnection(final String host, final String schema, final String userName, final String password, final Integer port, final boolean setAsCurrentDb) throws CannotConnectToDatabaseException {
        if (host == null || host.isEmpty()) {
            throw new CannotConnectToDatabaseException("Argument host cannot be null or empty.");
        }
        if (userName == null || userName.isEmpty()) {
            throw new CannotConnectToDatabaseException("Argument userName cannot be null or empty.");
        }
        if (password == null) {
            throw new CannotConnectToDatabaseException("Argument password cannot be null or empty.");
        }
        final StringBuilder url = new StringBuilder("jdbc:mysql://" + host);
        if (port != null && port != 0) {
            url.append(":" + port);
        }
        url.append("/" + schema);
        Connection conn;
        try {
            DriverManager.registerDriver(new Driver());
            conn = DriverManager.getConnection(url.toString(), userName, password);
        } catch (final SQLException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Attempt to connect to database failed.", e);
            }
            throw new CannotConnectToDatabaseException(e.getMessage());
        }
        try {
            conn.close();
        } catch (final SQLException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Closing the connection just established failed.", e);
            }
            if (setAsCurrentDb) {
                saveNewDbConnection(host, schema, userName, password, port);
            }
            return true;
        }
        if (setAsCurrentDb) {
            saveNewDbConnection(host, schema, userName, password, port);
        }
        return true;
    }

    /**
	 * Saves given connection details to the tool's {@link IPreferenceStore}.
	 * 
	 * @param host
	 * @param schema
	 * @param userName
	 * @param password
	 * @param port
	 */
    private void saveNewDbConnection(final String host, final String schema, final String userName, final String password, final Integer port) {
        final IPreferenceStore prefStore = CorePlugin.getDefault().getPreferenceStore();
        prefStore.setValue(DataModelPreferencesControl.INTERNAL_DATABASE, schema);
        prefStore.setValue(DataModelPreferencesControl.INTERNAL_DB_USERNAME, userName);
        prefStore.setValue(DataModelPreferencesControl.INTERNAL_DB_PASSWORD, password);
        prefStore.setValue(DataModelPreferencesControl.INTERNAL_DB_HOSTNAME, host);
        prefStore.setValue(DataModelPreferencesControl.INTERNAL_DB_PORT, Integer.valueOf(port));
        EventManager.getInstance().fireEvent(EEventType.CONNECTION_CHANGE);
    }

    /**
	 * Make the specified {@link Collection} of annotated class with super type
	 * {@link InfoSpaceItem} known to the ModelManager.
	 * <p>
	 * Each class will be checked for conformity. The annotations {@link Entity}, {@link Table}, and
	 * {@link DiscriminatorValue} have to be present at class level. The element &quot;name&quot; of
	 * the {@link Table} annotation must take one of the values in <em>actor</em>,
	 * <em>contentelement</em>, <em>context</em>, <em>interaction</em>, or <em>knowledge</em>. The
	 * value of the {@link DiscriminatorValue} annotation must equal the fully qualified name of the
	 * class.
	 * </p>
	 * <p>
	 * All fields of the class - if any - must be marked &quot;transient&quot; either by modifier or
	 * by the {@link Transient} annotation. Same goes for the declared methods of the class except
	 * they are marked to {@link Override} a super class method.
	 * </p>
	 * <p>
	 * All occasions not matching the conformity will issue an {@link DataModelAnnotationException}.
	 * </p>
	 * 
	 * @param annotatedEntityClasses
	 *            A {@link Collection} of annotated entity sub-classes of {@link InfoSpaceItem}.
	 * @throws DataModelAnnotationException
	 *             if the class is not annotated with {@link Entity}, {@link Table}, and
	 *             {@link DiscriminatorValue}, all set up with appropriate element values, one or
	 *             more of its declared fields or methods is not marked transient or a
	 *             {@link SecurityException} occurs.
	 */
    public void registerAnnotatedEntityClasses(final Collection<Class<? extends InfoSpaceItem>> annotatedEntityClasses) {
        if (annotatedEntityClasses == null || annotatedEntityClasses.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No annotated class to be added.");
            }
            return;
        }
        for (final Class<? extends InfoSpaceItem> clazz : annotatedEntityClasses) {
            final Table tableAnnotation = clazz.getAnnotation(Table.class);
            final DiscriminatorValue discrValAnnotation = clazz.getAnnotation(DiscriminatorValue.class);
            if (clazz.getAnnotation(Entity.class) == null) {
                throw new DataModelAnnotationException("Class " + clazz.getCanonicalName() + "does not have @Entity annotation.");
            }
        }
        this.annotatedEntityClasses.addAll(annotatedEntityClasses);
        if (this.factory != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Model Manager's SessionFactory is already set up, destroying current instance.");
            }
            this.factory = null;
        }
    }

    /**
	 * To be called during the initialization process of the local {@link SessionFactory}.
	 * <p>
	 * Due to a bug in Hibernate it is impossible to create a database index on a non-unique column
	 * through the Hibernate mapping set up. The application though requires two indices on the name
	 * field of the {@link Actor} entity and the title field of the {@link ContentElement} entity to
	 * speed up queries. These are set up here.
	 * </p>
	 * <p>
	 * The persistence store is queried to check the existence of the index on the actor table. If
	 * it is not yet present it will be created through a second query. Next, the same procedure is
	 * executed with the contentelement table.
	 * </p>
	 * <p>
	 * Note, this action will do no harm if the bug gets in Hibernate and the annotations of the
	 * above mentioned entities do the job. However, this is MySQL specific code such that its
	 * execution on a different DB is unpredictable.
	 * </p>
	 * TODO: MySQL specific code! Do not use with other DBs
	 * 
	 * @throws CannotConnectToDatabaseException
	 *             in case the persistence store is not available.
	 */
    private void createDbIndices() throws CannotConnectToDatabaseException {
        final String showQueryAc = "SHOW INDEX FROM actor WHERE Key_name LIKE 'IDX_Actor_Name';";
        final String createQueryAc = "CREATE INDEX IDX_Actor_Name ON actor(name);";
        Session s = ModelManager.getInstance().getCurrentSession();
        Transaction tx = null;
        int result;
        try {
            tx = s.beginTransaction();
            result = s.createSQLQuery(showQueryAc).executeUpdate();
            if (result == 0) {
                s.createSQLQuery(createQueryAc).executeUpdate();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Successfully created index on name field of actor table.");
                }
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Index on name field of actor table already exists. No need to create.");
                }
            }
            s.clear();
            tx.commit();
        } catch (final HibernateException he) {
            tx.rollback();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to create index on actor table - transaction was rolled back.", he);
            }
            throw he;
        } finally {
            s.close();
        }
        final String showQueryCE = "SHOW KEYS FROM contentelement WHERE Key_name LIKE 'IDX_CE_Title';";
        final String createQueryCE = "CREATE INDEX IDX_CE_Title ON contentelement(name);";
        s = ModelManager.getInstance().getCurrentSession();
        try {
            tx = s.beginTransaction();
            result = s.createSQLQuery(showQueryCE).executeUpdate();
            if (result == 0) {
                s.createSQLQuery(createQueryCE).executeUpdate();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Successfully created index on name field of contentelement table.");
                }
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Index on name field of contentelement table already exists. No need to create.");
                }
            }
            s.clear();
            tx.commit();
        } catch (final HibernateException he) {
            tx.rollback();
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Failed to create index on contentelement table - transaction was rolled back.", he);
            }
            throw he;
        } finally {
            s.close();
        }
    }
}
