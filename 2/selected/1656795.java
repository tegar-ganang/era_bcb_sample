package ca.sqlpower.wabit.enterprise.client;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ca.sqlpower.dao.MessageSender;
import ca.sqlpower.dao.SPPersistenceException;
import ca.sqlpower.dao.json.JSONHttpMessageSender;
import ca.sqlpower.dao.json.SPJSONMessageDecoder;
import ca.sqlpower.enterprise.client.SPServerInfo;
import ca.sqlpower.http.HttpResponseHandler;
import ca.sqlpower.sql.DataSourceCollection;
import ca.sqlpower.sql.DatabaseListChangeEvent;
import ca.sqlpower.sql.DatabaseListChangeListener;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sql.Olap4jDataSource;
import ca.sqlpower.sql.PlDotIni;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.util.UserPrompter.UserPromptOptions;
import ca.sqlpower.util.UserPrompter.UserPromptResponse;
import ca.sqlpower.util.UserPrompterFactory.UserPromptType;
import ca.sqlpower.wabit.WabitSession;
import ca.sqlpower.wabit.WabitSessionContext;
import ca.sqlpower.wabit.WabitSessionImpl;
import ca.sqlpower.wabit.WabitWorkspace;
import ca.sqlpower.wabit.dao.WabitSessionPersister;
import ca.sqlpower.wabit.dao.json.WabitJSONPersister;
import ca.sqlpower.wabit.dao.session.WorkspacePersisterListener;
import ca.sqlpower.wabit.swingui.WabitSwingSessionContext;

/**
 * A special kind of session that binds itself to a remote Wabit Enterprise
 * Server. Provides database connection information and file storage capability
 * based on the remote server.
 */
public class WabitClientSession extends WabitSessionImpl {

    private static final Logger logger = Logger.getLogger(WabitClientSession.class);

    /**
     * The relative path to the Mondrian schemas from the server's base URI.
     */
    private static final String MONDRIAN_SCHEMA_REL_PATH = "mondrian-schema/";

    private final Updater updater;

    /**
     * This workspace's location information.
     */
    private final WorkspaceLocation workspaceLocation;

    private final HttpClient outboundHttpClient;

    /**
	 * Handles output Wabit persistence calls for this WabitServerSession
	 */
    private final WabitJSONPersister jsonPersister;

    /**
	 * Applies Wabit persistence calls coming from a Wabit server to this WabitServerSession
	 */
    private final WabitSessionPersister sessionPersister;

    private static CookieStore cookieStore = new BasicCookieStore();

    /**
     * The data source collection retrieved from the server. This field is
     * lazy-loaded; it should always be accessed by calling
     * {@link #getDataSources()}. This data source is monitored for changes, and
     * those changes are posted back to the server.
     */
    private DataSourceCollection<SPDataSource> dataSourceCollection;

    /**
     * Sends changes to the local copy of the data source collection back to the
     * server. Gets detached and shut down when this session is closed.
     */
    private final DataSourceCollectionUpdater dataSourceCollectionUpdater = new DataSourceCollectionUpdater();

    public WabitClientSession(@Nonnull WorkspaceLocation workspaceLocation, @Nonnull WabitSessionContext context) {
        super(context);
        this.workspaceLocation = workspaceLocation;
        if (workspaceLocation == null) {
            throw new NullPointerException("workspaceLocation must not be null");
        }
        super.fontLoader = new RemoteFontLoader(workspaceLocation.getServiceInfo());
        outboundHttpClient = createHttpClient(workspaceLocation.getServiceInfo());
        getWorkspace().setUUID(workspaceLocation.getUuid());
        getWorkspace().setName("Loading Workspace...");
        getWorkspace().setSession(this);
        sessionPersister = new WabitSessionPersister("inbound-" + workspaceLocation.getUuid(), WabitClientSession.this, true);
        sessionPersister.setGodMode(true);
        updater = new Updater(workspaceLocation.getUuid(), new SPJSONMessageDecoder(sessionPersister));
        MessageSender<JSONObject> httpSender = new JSONHttpMessageSender(outboundHttpClient, workspaceLocation.getServiceInfo(), workspaceLocation.getUuid());
        jsonPersister = new WabitJSONPersister(httpSender);
        try {
            ServerInfoProvider.getServerVersion(this.workspaceLocation.getServiceInfo().getServerAddress(), String.valueOf(this.workspaceLocation.getServiceInfo().getPort()), this.workspaceLocation.getServiceInfo().getPath(), this.workspaceLocation.getServiceInfo().getUsername(), this.workspaceLocation.getServiceInfo().getPassword());
        } catch (Exception e) {
            throw new AssertionError("Exception encountered while verifying the server license:" + e.getMessage());
        }
    }

    public static HttpClient createHttpClient(SPServerInfo serviceInfo) {
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, 2000);
        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        httpClient.setCookieStore(cookieStore);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(serviceInfo.getServerAddress(), AuthScope.ANY_PORT), new UsernamePasswordCredentials(serviceInfo.getUsername(), serviceInfo.getPassword()));
        return httpClient;
    }

    @Override
    public boolean close() {
        logger.debug("Closing Client Session");
        try {
            HttpUriRequest request = new HttpDelete(getServerURI(workspaceLocation.getServiceInfo(), "session/" + getWorkspace().getUUID()));
            outboundHttpClient.execute(request, new BasicResponseHandler());
        } catch (Exception e) {
            try {
                logger.error(e);
                getContext().createUserPrompter("Cannot access the server to close the server session", UserPromptType.MESSAGE, UserPromptOptions.OK, UserPromptResponse.OK, UserPromptResponse.OK, "OK");
            } catch (Throwable t) {
            }
        }
        outboundHttpClient.getConnectionManager().shutdown();
        updater.interrupt();
        if (dataSourceCollection != null) {
            dataSourceCollectionUpdater.detach(dataSourceCollection);
        }
        return super.close();
    }

    /**
     * Returns the location this workspace was loaded from.
     */
    public WorkspaceLocation getWorkspaceLocation() {
        return workspaceLocation;
    }

    /**
     * Sends local data source collection changes to the server. In order for
     * this to work, the data source collection must be attached. To prevent
     * memory leaks, the collection updater must be detached from all data
     * source collections it was monitoring when the Wabit session is closed.
     */
    private class DataSourceCollectionUpdater implements DatabaseListChangeListener, PropertyChangeListener {

        /**
    	 * If true this updater is currently posting properties to the server. If
    	 * properties are being posted to the server and an event comes in because
    	 * of a change during posting the updater should not try to repost the message
    	 * it is currently trying to post.
    	 */
        private boolean postingProperties = false;

        public void attach(DataSourceCollection<SPDataSource> dsCollection) {
            dsCollection.addDatabaseListChangeListener(this);
            for (SPDataSource ds : dsCollection.getConnections()) {
                ds.addPropertyChangeListener(this);
            }
        }

        public void detach(DataSourceCollection<SPDataSource> dsCollection) {
            dsCollection.removeDatabaseListChangeListener(this);
            for (SPDataSource ds : dsCollection.getConnections()) {
                ds.removePropertyChangeListener(this);
            }
        }

        /**
         * Handles the addition of a new database entry, relaying its current
         * state to the server. Also begins listening to the new data source as
         * would have happened if the new data source existed before
         * {@link #attach(DataSourceCollection)} was invoked.
         */
        public void databaseAdded(DatabaseListChangeEvent e) {
            SPDataSource newDS = e.getDataSource();
            newDS.addPropertyChangeListener(this);
            List<NameValuePair> properties = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> ent : newDS.getPropertiesMap().entrySet()) {
                properties.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
            }
            postPropertiesToServer(newDS, properties);
        }

        /**
         * Handles changes to individual data sources by relaying their new
         * state to the server.
         * <p>
         * <b>Implementation note:</b> Presently, all properties for the data
         * source are sent back to the server every time one of them changes.
         * This is not the desired behaviour, but without rethinking the
         * SPDataSource event system, there is little else we can do: the
         * property change events tell us JavaBeans property names, but in order
         * to send incremental updates, we's need to know the pl.ini property
         * key names.
         * 
         * @param evt
         *            The event describing the change. Its source must be the
         *            data source object which was modified.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            SPDataSource ds = (SPDataSource) evt.getSource();
            ds.addPropertyChangeListener(this);
            List<NameValuePair> properties = new ArrayList<NameValuePair>();
            for (Map.Entry<String, String> ent : ds.getPropertiesMap().entrySet()) {
                properties.add(new BasicNameValuePair(ent.getKey(), ent.getValue()));
            }
            postPropertiesToServer(ds, properties);
        }

        /**
         * Modifies the properties of the given data source on the server. If
         * the given data source does not exist on the server, it will be
         * created with all of the given properties.
         * 
         * @param ds
         *            The data source to update on the server.
         * @param properties
         *            The properties to update. No properties will be removed
         *            from the server, and only the given properties will be
         *            updated or created.
         */
        private void postPropertiesToServer(SPDataSource ds, List<NameValuePair> properties) {
            if (postingProperties) return;
            HttpClient httpClient = createHttpClient(workspaceLocation.getServiceInfo());
            try {
                final ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {

                    public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                        if (response.getStatusLine().getStatusCode() != 200) {
                            throw new ClientProtocolException("Failed to create/update data source on server. Reason:\n" + EntityUtils.toString(response.getEntity()));
                        } else {
                            return null;
                        }
                    }
                };
                if (ds instanceof Olap4jDataSource && ((Olap4jDataSource) ds).getMondrianSchema() != null && ((Olap4jDataSource) ds).getMondrianSchema().getScheme().equals("file")) {
                    Olap4jDataSource olapDS = ((Olap4jDataSource) ds);
                    File schemaFile = new File(olapDS.getMondrianSchema());
                    if (!schemaFile.exists()) logger.error("Schema file " + schemaFile.getAbsolutePath() + " does not exist for data source " + ds.getName());
                    HttpPost request = new HttpPost(getServerURI(workspaceLocation.getServiceInfo(), MONDRIAN_SCHEMA_REL_PATH + schemaFile.getName()));
                    request.setEntity(new FileEntity(schemaFile, "text/xml"));
                    httpClient.execute(request, responseHandler);
                    for (int i = properties.size() - 1; i >= 0; i--) {
                        NameValuePair pair = properties.get(i);
                        if (pair.getName().equals(Olap4jDataSource.MONDRIAN_SCHEMA)) {
                            properties.add(new BasicNameValuePair(Olap4jDataSource.MONDRIAN_SCHEMA, SPDataSource.SERVER + schemaFile.getName()));
                            properties.remove(pair);
                            break;
                        }
                    }
                    try {
                        postingProperties = true;
                        olapDS.setMondrianSchema(new URI(SPDataSource.SERVER + schemaFile.getName()));
                    } finally {
                        postingProperties = false;
                    }
                }
                HttpPost request = new HttpPost(dataSourceURI(ds));
                request.setEntity(new UrlEncodedFormEntity(properties));
                httpClient.execute(request, responseHandler);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        /**
         * Handles deleting of a database entry by requesting that the server
         * deletes it. Also unlistens to the data source to prevent memory
         * leaks.
         */
        public void databaseRemoved(DatabaseListChangeEvent e) {
            HttpClient httpClient = createHttpClient(workspaceLocation.getServiceInfo());
            try {
                SPDataSource removedDS = e.getDataSource();
                HttpDelete request = new HttpDelete(dataSourceURI(removedDS));
                final ResponseHandler<Void> responseHandler = new ResponseHandler<Void>() {

                    public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                        if (response.getStatusLine().getStatusCode() != 200) {
                            throw new ClientProtocolException("Failed to delete data source on server. Reason:\n" + EntityUtils.toString(response.getEntity()));
                        } else {
                            return null;
                        }
                    }
                };
                httpClient.execute(request, responseHandler);
                if (removedDS instanceof Olap4jDataSource && ((Olap4jDataSource) removedDS).getMondrianSchema() != null) {
                    URI serverURI = ((Olap4jDataSource) removedDS).getMondrianSchema();
                    logger.debug("Server URI for deletion is " + serverURI);
                    HttpDelete schemaRequest = new HttpDelete(serverURI);
                    httpClient.execute(schemaRequest, responseHandler);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }

        /**
         * Returns the URI that references the given data source on the server.
         * 
         * @param ds
         *            The data source whose server URI to return.
         * @return An absolute URI for the given data source on this session's
         *         Wabit server.
         */
        private URI dataSourceURI(SPDataSource ds) throws URISyntaxException {
            String type;
            if (ds instanceof JDBCDataSource) {
                type = "jdbc";
            } else if (ds instanceof Olap4jDataSource) {
                type = "olap4j";
            } else {
                throw new UnsupportedOperationException("Data source type " + ds.getClass() + " is not known");
            }
            return getServerURI(workspaceLocation.getServiceInfo(), "data-sources/" + type + "/" + ds.getName());
        }
    }

    /**
     * Returns the server's data source list, retrieving it from the server if
     * that has not already been done during this session. Changes made to this
     * data source collection will be sent back to the server, but the changes
     * will not be applied on the server side unless the user has the
     * appropriate permissions.
     * <p>
     * Future plans: In the future, the server will probably be a proxy for all
     * database operations, and we won't actually send the connection
     * information to the client. This has the advantage that it can work over
     * an HTTP firewall or proxy, where the present method would fail.
     */
    @Override
    public DataSourceCollection<SPDataSource> getDataSources() {
        if (dataSourceCollection != null) return dataSourceCollection;
        ResponseHandler<DataSourceCollection<SPDataSource>> plIniHandler = new ResponseHandler<DataSourceCollection<SPDataSource>>() {

            public DataSourceCollection<SPDataSource> handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new IOException("Server error while reading data sources: " + response.getStatusLine());
                }
                PlDotIni plIni;
                try {
                    plIni = new PlDotIni(getServerURI(workspaceLocation.getServiceInfo(), "jdbc/"), getServerURI(workspaceLocation.getServiceInfo(), MONDRIAN_SCHEMA_REL_PATH));
                    plIni.read(response.getEntity().getContent());
                    logger.debug("Data source collection has URI " + plIni.getServerBaseURI());
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                return plIni;
            }
        };
        try {
            dataSourceCollection = executeServerRequest(outboundHttpClient, workspaceLocation.getServiceInfo(), "data-sources/", plIniHandler);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        dataSourceCollectionUpdater.attach(dataSourceCollection);
        return dataSourceCollection;
    }

    /**
     * List all the workspaces on this context's server.
     * 
     * @param serviceInfo
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws JSONException 
     */
    public static List<WorkspaceLocation> getWorkspaceNames(SPServerInfo serviceInfo) throws IOException, URISyntaxException, JSONException {
        HttpClient httpClient = createHttpClient(serviceInfo);
        try {
            HttpUriRequest request = new HttpGet(getServerURI(serviceInfo, "workspaces"));
            String responseBody = httpClient.execute(request, new BasicResponseHandler());
            JSONArray response;
            List<WorkspaceLocation> workspaces = new ArrayList<WorkspaceLocation>();
            response = new JSONArray(responseBody);
            logger.debug("Workspace list:\n" + responseBody);
            for (int i = 0; i < response.length(); i++) {
                JSONObject workspace = (JSONObject) response.get(i);
                workspaces.add(new WorkspaceLocation(workspace.getString("name"), workspace.getString("UUID"), serviceInfo));
            }
            return workspaces;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
	 * Sends an HTTP request to a Wabit Enterprise Server to create a new remote
	 * Wabit Workspace on that server.
	 * 
	 * @param serviceInfo
	 *            A {@link SPServerInfo} containing the connection
	 *            information for that server
	 * @return The {@link WorkspaceLocation} of the newly created remote
	 *         WabitWorkspace
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws JSONException
	 */
    public static WorkspaceLocation createNewServerSession(SPServerInfo serviceInfo) throws URISyntaxException, ClientProtocolException, IOException, JSONException {
        HttpClient httpClient = createHttpClient(serviceInfo);
        try {
            HttpUriRequest request = new HttpPost(getServerURI(serviceInfo, "workspaces"));
            String responseBody = httpClient.execute(request, new BasicResponseHandler());
            JSONObject response = new JSONObject(responseBody);
            logger.debug("New Workspace:" + responseBody);
            return new WorkspaceLocation(response.getString("name"), response.getString("UUID"), serviceInfo);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public void deleteServerWorkspace() throws URISyntaxException, ClientProtocolException, IOException {
        SPServerInfo serviceInfo = workspaceLocation.getServiceInfo();
        HttpClient httpClient = createHttpClient(serviceInfo);
        try {
            HttpUriRequest request = new HttpDelete(getServerURI(serviceInfo, "workspaces/" + getWorkspace().getUUID()));
            httpClient.execute(request, new HttpResponseHandler());
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    /**
	 * Finds and opens a specific Wabit Workspace from the given
	 * {@link WorkspaceLocation}. The new session will keep itself up-to-date by
	 * polling the server for new state. Likewise, local changes to the session will be pushed its own
	 * updates back to the server.
	 * 
	 * @param context
	 *            The context to register the new remote WabitSession with
	 * @param workspaceLoc
	 *            A {@link WorkspaceLocation} detailing the location of the
	 *            remote workspace to be opened
	 * @return A remote WabitSession based on the given workspace
	 */
    public static WabitClientSession openServerSession(WabitSessionContext context, WorkspaceLocation workspaceLoc) {
        final WabitClientSession session = new WabitClientSession(workspaceLoc, context);
        context.registerChildSession(session);
        session.startUpdaterThread();
        return session;
    }

    /**
	 * Finds and opens all visible Wabit workspaces on the given Wabit Enterprise Server.
	 * Calling this method essentially constitutes "logging in" to the given server.
	 * 
	 * @param context the context to add the newly-retrieved sessions to
	 * @param serverInfo The server to contact.
	 * @return the list of sessions that were opened.
	 * @throws JSONException 
	 * @throws URISyntaxException 
	 * @throws IOException 
	 */
    public static List<WabitClientSession> openServerSessions(WabitSessionContext context, SPServerInfo serverInfo) throws IOException, URISyntaxException, JSONException {
        List<WabitClientSession> openedSessions = new ArrayList<WabitClientSession>();
        for (WorkspaceLocation workspaceLoc : WabitClientSession.getWorkspaceNames(serverInfo)) {
            openedSessions.add(openServerSession(context, workspaceLoc));
        }
        return openedSessions;
    }

    private static <T> T executeServerRequest(HttpClient httpClient, SPServerInfo serviceInfo, String contextRelativePath, ResponseHandler<T> responseHandler) throws IOException, URISyntaxException {
        HttpUriRequest request = new HttpGet(getServerURI(serviceInfo, contextRelativePath));
        return httpClient.execute(request, responseHandler);
    }

    private static URI getServerURI(SPServerInfo serviceInfo, String contextRelativePath) throws URISyntaxException {
        logger.debug("Getting server URI for: " + serviceInfo);
        String contextPath = serviceInfo.getPath();
        URI serverURI = new URI("http", null, serviceInfo.getServerAddress(), serviceInfo.getPort(), contextPath + contextRelativePath, null, null);
        logger.debug("Created URI " + serverURI);
        return serverURI;
    }

    public void startUpdaterThread() {
        updater.start();
        WorkspacePersisterListener.attachListener(this, jsonPersister, sessionPersister, true);
    }

    public void persistWorkspaceToServer() throws SPPersistenceException {
        WorkspacePersisterListener tempListener = new WorkspacePersisterListener(this, jsonPersister, true);
        tempListener.persistObject(this.getWorkspace());
    }

    /**
	 * Polls this session's server for updates until interrupted. There should
	 * be exactly one instance of this class per WabitServerSession.
	 */
    private class Updater extends Thread {

        /**
		 * How long we will pause after an update error before attempting to
		 * contact the server again.
		 */
        private long retryDelay = 1000;

        private final SPJSONMessageDecoder jsonDecoder;

        /**
		 * Used by the Updater to handle inbound HTTP updates
		 */
        private final HttpClient inboundHttpClient;

        private volatile boolean cancelled;

        /**
		 * Creates, but does not start, the updater thread.
		 * 
		 * @param workspaceUUID
		 *            the ID of the workspace this updater is responsible for. This is
		 *            used in creating the thread's name.
		 */
        Updater(String workspaceUUID, SPJSONMessageDecoder jsonDecoder) {
            super("updater-" + workspaceUUID);
            this.jsonDecoder = jsonDecoder;
            inboundHttpClient = createHttpClient(workspaceLocation.getServiceInfo());
        }

        public void interrupt() {
            logger.debug("Updater Thread interrupt sent");
            super.interrupt();
            cancelled = true;
        }

        @Override
        public void run() {
            logger.info("Updater thread starting");
            final String contextRelativePath = "workspaces/" + getWorkspace().getUUID();
            try {
                while (!this.isInterrupted() && !cancelled) {
                    try {
                        final String jsonArray = executeServerRequest(inboundHttpClient, workspaceLocation.getServiceInfo(), contextRelativePath, new BasicResponseHandler());
                        runInForeground(new Runnable() {

                            public void run() {
                                try {
                                    jsonDecoder.decode(jsonArray);
                                } catch (SPPersistenceException e) {
                                    logger.error("Update from server failed!", e);
                                    createUserPrompter("Wabit failed to apply an update that was just received from the Enterprise Server.\n" + "The error was:" + "\n" + e.getMessage(), UserPromptType.MESSAGE, UserPromptOptions.OK, UserPromptResponse.OK, UserPromptResponse.OK, "OK");
                                }
                            }
                        });
                    } catch (Exception ex) {
                        logger.error("Failed to contact server. Will retry in " + retryDelay + " ms.", ex);
                        Thread.sleep(retryDelay);
                    }
                }
            } catch (InterruptedException ex) {
                logger.info("Updater thread exiting normally due to interruption.");
            }
            inboundHttpClient.getConnectionManager().shutdown();
        }
    }

    /**
	 * Fetches the system workspace from the same server as this session.
	 * Returns null if the user doesn't have access to a given workspace.
	 */
    public WabitWorkspace getSystemWorkspace() {
        for (WabitSession session : this.getContext().getSessions()) {
            if (session.getWorkspace().getUUID().equals("system")) {
                return session.getWorkspace();
            }
        }
        return null;
    }

    @Override
    public void runInForeground(Runnable runner) {
        if (getContext() instanceof WabitSwingSessionContext) {
            SwingUtilities.invokeLater(runner);
        } else {
            super.runInForeground(runner);
        }
    }

    @Override
    public boolean isEnterpriseServerSession() {
        return true;
    }

    /**
	 * Exposes the shared cookie store so we don't spawn useless sessions
	 * through the client.
	 */
    public static CookieStore getCookieStore() {
        return cookieStore;
    }
}
