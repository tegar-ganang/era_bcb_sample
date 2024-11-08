package org.cilogon.portal.servlets;

import edu.uiuc.ncsa.security.core.Initializable;
import edu.uiuc.ncsa.security.core.exceptions.GeneralException;
import edu.uiuc.ncsa.security.oauth_1_0a.OAuthTokenFactory;
import edu.uiuc.ncsa.security.rdf.MyThingSession;
import edu.uiuc.ncsa.security.rdf.UriRefFactory;
import edu.uiuc.ncsa.security.rdf.storage.*;
import edu.uiuc.ncsa.security.servlet.JSPUtil;
import org.cilogon.portal.config.cli.PortalConfigurationDepot;
import org.cilogon.portal.config.models.PortalParametersModel;
import org.cilogon.portal.config.rdf.PortalRoot;
import org.cilogon.portal.config.rdf.PortalVocabulary;
import org.cilogon.portal.storage.PortalStoreFactory;
import org.cilogon.storage.FileStoreModel;
import org.cilogon.util.CILogonUriRefFactory;
import org.cilogon.util.exceptions.CILogonException;
import org.tupeloproject.kernel.OperatorException;
import org.tupeloproject.rdf.Resource;
import org.tupeloproject.rdf.terms.Rdfs;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

/**
 * Servlet that handles editing or creating a portal configuration file. This is pretty 'basic but should cover most
 * situations.
 * <p>Created by Jeff Gaynor<br>
 * on Jun 11, 2011 at  3:27:35 PM
 */
public class SetupServlet extends PortalAbstractServlet {

    static class ConfigRecord {

        String fileName;

        PortalConfigurationDepot pcd;

        String id;
    }

    public static final String CONFIG_STATUS_KEY = "configStatusKey";

    public static final String CONFIG_IDENTIFIER_KEY = "configIdentifierKey";

    public static final String CONFIG_STATUS_BASIC = "basic";

    public static final String CONFIG_STATUS_SETUP_FILE_STORE = "setupFileStore";

    public static final String CONFIG_STATUS_SETUP_POSTGRES_STORE = "setupPostgresStore";

    public static final String CONFIG_STATUS_STORE_ADMIN = "storeAdmin";

    public static final String CONFIG_STATUS_LOAD_FILE = "loadFile";

    /**
     * For forms, this tells the jsp page how to get back to this servlet
     */
    public static final String FORM_ACTION = "formAction";

    protected static final int __NO_OP = 0;

    protected static final int __BASIC = 1;

    protected static final int __LOAD_CONFIG = 2;

    protected static final int __SETUP_FILE_STORE = 3;

    protected static final int __SETUP_POSTGRES_STORE = 4;

    protected static final int __SETUP_STORE_ADMIN = 5;

    protected static final int __DONE = -100;

    public static final String CONFIG_FILE_PATH = "filePath";

    public static HashMap<String, ConfigRecord> getConfigs() {
        if (configs == null) {
            configs = new HashMap<String, ConfigRecord>();
        }
        return configs;
    }

    static HashMap<String, ConfigRecord> configs;

    protected PortalRoot getConfig(HttpServletRequest request) {
        String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
        if (isEmpty(id)) return null;
        if (!getConfigs().containsKey(id)) {
            throw new IllegalStateException("Error: no configuration associated with id=" + id);
        }
        return getConfigs().get(id).pcd.getPortalRoot();
    }

    protected String getFormAction(HttpServletRequest request) {
        return request.getContextPath() + "/setup";
    }

    @Override
    protected void doIt(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        try {
            System.out.println("\n--------------------");
            Enumeration ee = request.getParameterNames();
            while (ee.hasMoreElements()) {
                String xxxx = ee.nextElement().toString();
                System.out.println("key=" + xxxx + ", value=" + request.getParameter(xxxx));
            }
            String toDo = request.getParameter(CONFIG_STATUS_KEY);
            System.out.println("TO DO = " + toDo);
            switch(lookupAction(request)) {
                case __NO_OP:
                    getConfigFile(request, response);
                    return;
                case __LOAD_CONFIG:
                    getOrCreateConfiguration(request, response);
                    return;
                case __BASIC:
                    doneBasic(request, response);
                    break;
                case __DONE:
                    String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
                    if (!getConfigs().containsKey(id)) {
                        throw new GeneralException("Error: no configuration for internal id " + id);
                    } else {
                        saveConfig(id);
                    }
                    fwd(request, response, "/setup-done.jsp");
                    return;
                case __SETUP_FILE_STORE:
                    setupFileStore(request, response);
                    System.out.println("OK");
                    return;
                case __SETUP_POSTGRES_STORE:
                    setupPostgresStore(request, response);
                    return;
                case __SETUP_STORE_ADMIN:
                    doAdmin(request, response);
                    return;
                default:
                    throw new CILogonException("Error: unknown form action. This implies an internal problem");
            }
        } catch (Throwable t) {
            handleException(t, request, response);
        }
    }

    private void doAdmin(HttpServletRequest request, HttpServletResponse response) throws OperatorException, IOException, ServletException {
        String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
        info("Doing store admin for " + id);
        PortalRoot portalRoot = getPortalRoot(id);
        PortalStoreFactory portalStoreFactory = new PortalStoreFactory();
        portalStoreFactory.setConfiguration(portalRoot);
        Initializable ppa = portalStoreFactory.getAdminClient();
        String pgAdminAction = request.getParameter(ConfigConstants.ADMIN_ACTION);
        if (pgAdminAction == null || pgAdminAction.length() == 0) {
        } else {
            if (pgAdminAction.equals(ConfigConstants.ADMIN_CREATE)) {
                ppa.createNew();
            }
            if (pgAdminAction.equals(ConfigConstants.ADMIN_DESTROY)) {
                ppa.destroy();
            }
            if (pgAdminAction.equals(ConfigConstants.ADMIN_INITIALIZE)) {
                ppa.destroy();
                ppa.init();
            }
        }
        fwd(request, response, "/setup-done.jsp");
    }

    protected void startStoreAdminSetup(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute(ConfigConstants.ADMIN_ACTION, ConfigConstants.ADMIN_ACTION);
        request.setAttribute(ConfigConstants.ADMIN_CREATE, ConfigConstants.ADMIN_CREATE);
        request.setAttribute(ConfigConstants.ADMIN_DESTROY, ConfigConstants.ADMIN_DESTROY);
        request.setAttribute(ConfigConstants.ADMIN_DO_NOTHING, ConfigConstants.ADMIN_DO_NOTHING);
        addState(request, getParameter(request, CONFIG_IDENTIFIER_KEY), CONFIG_STATUS_STORE_ADMIN);
        fwd(request, response, "/setup-admin.jsp");
    }

    private void setupPostgresStore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
        info("Doing postgres store setup for id " + id);
        PortalRoot portalRoot = getPortalRoot(id);
        PostgresStoreModel psm = new PostgresStoreModel(portalRoot.getStore());
        DatabaseModel dbm = psm.getFirstDatabase();
        dbm.setName(getParameter(request, SQLVocabulary.SQL_DATABASE_NAME));
        dbm.setSchema(getParameter(request, SQLVocabulary.SQL_DATABASE_SCHEMA));
        dbm.getTransactionTable().setPrefix(getParameter(request, SQLVocabulary.SQL_TABLE_PREFIX));
        dbm.getTransactionTable().setName(getParameter(request, SQLVocabulary.SQL_TABLE_NAME));
        ConnectionParametersModel cpm = psm.getConnectionParametersModel();
        AdminConnectionParametersModel apm = psm.getAdminConnectionParametersModel();
        cpm.setUserName(getParameter(request, SQLVocabulary.CONNECTION_USERNAME));
        cpm.setPassword(getParameter(request, SQLVocabulary.CONNECTION_PASSWORD));
        try {
            cpm.setPort(Integer.parseInt(getParameter(request, SQLVocabulary.CONNECTION_PORT)));
        } catch (Throwable t) {
        }
        cpm.setHost(getParameter(request, SQLVocabulary.CONNECTION_HOST));
        cpm.setDriver(getParameter(request, SQLVocabulary.CONNECTION_DRIVER));
        apm.setAdminUserName(getParameter(request, SQLVocabulary.CONNECTION_ADMIN_USERNAME));
        apm.setUserName(getParameter(request, SQLVocabulary.CONNECTION_USERNAME));
        apm.setAdminPassword(getParameter(request, SQLVocabulary.CONNECTION_ADMIN_PASSWORD));
        try {
            apm.setAdminPort(Integer.parseInt(getParameter(request, SQLVocabulary.CONNECTION_ADMIN_PORT)));
        } catch (Throwable t) {
        }
        apm.setAdminHost(getParameter(request, SQLVocabulary.CONNECTION_ADMIN_HOST));
        apm.setDriver(getParameter(request, SQLVocabulary.CONNECTION_ADMIN_DRIVER));
        saveConfig(id);
        startStoreAdminSetup(request, response);
    }

    protected void setupFileStore(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
        info("Doing file store setup for id " + id);
        PortalRoot portalRoot = getPortalRoot(id);
        FileStoreModel fsm = new FileStoreModel(portalRoot.getStore());
        fsm.setDataPath(getParameter(request, SQLVocabulary.FILE_DATA_PATH));
        fsm.setIndexPath(getParameter(request, SQLVocabulary.FILE_INDEX_PATH));
        saveConfig(id);
        startStoreAdminSetup(request, response);
    }

    protected PortalRoot getPortalRoot(String id) {
        if (getConfigs().containsKey(id)) {
            return getConfigs().get(id).pcd.getPortalRoot();
        }
        throw new IllegalStateException("Error: no configuration for internal id \"" + id + "\"");
    }

    /**
     * At this point, the basic information has been filled in. We read it and put it where it belongs, then
     * forward this to the next point in the chain.
     *
     * @param request
     * @param response
     */
    protected void doneBasic(HttpServletRequest request, HttpServletResponse response) throws Exception, IOException, ServletException {
        String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
        info("Doing basic for id " + id);
        PortalRoot portalRoot = getPortalRoot(id);
        PortalParametersModel ppm = portalRoot.getPortalParametersModel();
        ppm.setCallback(getParameter(request, PortalVocabulary.PORTAL_CALLBACK_URI));
        ppm.setSuccess(getParameter(request, PortalVocabulary.PORTAL_SUCCESS_URI));
        ppm.setFailure(getParameter(request, PortalVocabulary.PORTAL_FAILURE_URI));
        ppm.setName(getParameter(request, PortalVocabulary.PORTAL_NAME));
        ppm.setSkin(getParameter(request, PortalVocabulary.PORTAL_SKIN));
        StoreModel storeModel = null;
        if (portalRoot.hasA(SQLVocabulary.HAS_STORE)) {
            storeModel = portalRoot.getStore();
        }
        if (request.getParameter(ConfigConstants.STORE_TYPE).equals(SQLVocabulary.MEMORY_STORE_TYPE.toString())) {
            MemoryStoreModel msm = null;
            boolean isNew = isNewStore(storeModel, SQLVocabulary.MEMORY_STORE_TYPE);
            if (isNew) {
                msm = new MemoryStoreModel(portalRoot.getMyThingSession(), UriRefFactory.uriRef());
                portalRoot.setStore(msm);
            }
            saveConfig(id);
            fwd(request, response, "setup-done.jsp");
            return;
        }
        if (request.getParameter(ConfigConstants.STORE_TYPE).equals(SQLVocabulary.FILE_STORE_TYPE.toString())) {
            FileStoreModel fsm = null;
            boolean isNew = isNewStore(storeModel, SQLVocabulary.FILE_STORE_TYPE);
            if (isNew) {
                fsm = new FileStoreModel(portalRoot.getMyThingSession(), UriRefFactory.uriRef());
                portalRoot.setStore(fsm);
            } else {
                fsm = new FileStoreModel(storeModel);
            }
            setAtt(request, "path", fsm.getDataPath(), SQLVocabulary.FILE_DATA_PATH);
            setAtt(request, "index", fsm.getIndexPath(), SQLVocabulary.FILE_INDEX_PATH);
            saveConfig(id);
            addState(request, id, CONFIG_STATUS_SETUP_FILE_STORE);
            fwd(request, response, "setup-file.jsp");
            return;
        }
        if (request.getParameter(ConfigConstants.STORE_TYPE).equals(SQLVocabulary.POSTGRES_STORE_TYPE.toString())) {
            info("Setting up postgres store for id =" + id);
            boolean isNew = isNewStore(storeModel, SQLVocabulary.POSTGRES_STORE_TYPE);
            PostgresStoreModel psm = null;
            AdminConnectionParametersModel acm;
            ConnectionParametersModel cpm;
            DatabaseModel dbm;
            TransactionTableModel ttm;
            MyThingSession ts = portalRoot.getMyThingSession();
            if (isNew) {
                psm = new PostgresStoreModel(ts, UriRefFactory.uriRef());
                acm = new AdminConnectionParametersModel(ts, UriRefFactory.uriRef());
                cpm = new ConnectionParametersModel(ts, UriRefFactory.uriRef());
                dbm = new DatabaseModel(ts, UriRefFactory.uriRef());
                ttm = new TransactionTableModel(ts, UriRefFactory.uriRef());
                dbm.setTransactionTable(ttm);
                psm.setAdminConnectionParameters(acm);
                psm.setConnectionParameters(cpm);
                psm.addDatabase(dbm);
                ttm.setPrefix("portal");
                ttm.setName("transactions");
                portalRoot.setStore(psm);
            } else {
                psm = new PostgresStoreModel(storeModel);
                acm = psm.getAdminConnectionParametersModel();
                cpm = psm.getConnectionParametersModel();
                dbm = psm.getFirstDatabase();
                ttm = dbm.getTransactionTable();
            }
            saveConfig(id);
            setAtt(request, "dbName", dbm.getName(), SQLVocabulary.SQL_DATABASE_NAME);
            setAtt(request, "dbSchema", dbm.getSchema(), SQLVocabulary.SQL_DATABASE_SCHEMA);
            setAtt(request, "tName", ttm.getName(), SQLVocabulary.SQL_TABLE_NAME);
            setAtt(request, "tPrefix", ttm.getPrefix(), SQLVocabulary.SQL_TABLE_PREFIX);
            setAtt(request, "cUsername", cpm.getUserName(), SQLVocabulary.CONNECTION_USERNAME);
            setAtt(request, "cPassword", cpm.getPassword(), SQLVocabulary.CONNECTION_PASSWORD);
            setAtt(request, "cHost", cpm.getHost(), SQLVocabulary.CONNECTION_HOST);
            int port = cpm.getPort();
            if (port == -1) {
                setAtt(request, "cPort", null, SQLVocabulary.CONNECTION_PORT);
            } else {
                setAtt(request, "cPort", Integer.toString(port), SQLVocabulary.CONNECTION_PORT);
            }
            setAtt(request, "cDriver", cpm.getDriver(), SQLVocabulary.CONNECTION_DRIVER);
            setAtt(request, "aUsername", acm.getAdminUserName(), SQLVocabulary.CONNECTION_ADMIN_USERNAME);
            setAtt(request, "aPassword", acm.getAdminPassword(), SQLVocabulary.CONNECTION_ADMIN_PASSWORD);
            setAtt(request, "aHost", acm.getAdminHost(), SQLVocabulary.CONNECTION_ADMIN_HOST);
            port = acm.getAdminPort();
            if (port == -1) {
                setAtt(request, "aPort", null, SQLVocabulary.CONNECTION_ADMIN_PORT);
            } else {
                setAtt(request, "aPort", Integer.toString(port), SQLVocabulary.CONNECTION_ADMIN_PORT);
            }
            setAtt(request, "aDriver", acm.getDriver(), SQLVocabulary.CONNECTION_ADMIN_DRIVER);
            addState(request, id, CONFIG_STATUS_SETUP_POSTGRES_STORE);
            fwd(request, response, "setup-postgres.jsp");
        }
    }

    /**
     * Checks if the store is of the given type and decided if this servlet should create a new one.
     *
     * @param storeModel
     * @param rdfType
     * @return
     * @throws OperatorException
     */
    boolean isNewStore(StoreModel storeModel, Resource rdfType) throws OperatorException {
        if (storeModel == null) {
            return true;
        }
        if (storeModel.isA(rdfType)) {
            return false;
        }
        return true;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        OAuthTokenFactory.getTokenFactory();
        CILogonUriRefFactory.getFactory();
    }

    private void getOrCreateConfiguration(HttpServletRequest request, HttpServletResponse response) throws Exception, OperatorException, ServletException {
        System.out.println("Starting load file");
        String filePath = getParameter(request, CONFIG_FILE_PATH);
        String id = getParameter(request, CONFIG_IDENTIFIER_KEY);
        System.out.println("file path = " + filePath + ", id=" + id);
        if (isEmpty(id)) {
            throw new IllegalStateException("Error: no internal identifier found. Aborting");
        }
        File file = new File(filePath);
        PortalConfigurationDepot pcd = null;
        if (file.exists()) {
            pcd = new PortalConfigurationDepot(filePath);
        } else {
            if (!file.getParentFile().canWrite() || !file.getParentFile().canWrite()) {
                throw new IllegalStateException("Error: The file you specified  does not have a readable or writeable directory \"" + file.getParentFile() + "\"");
            }
            pcd = new PortalConfigurationDepot();
        }
        String rootName = getParameter(request, Rdfs.LABEL);
        PortalRoot portalRoot = null;
        if (isEmpty(rootName)) {
            try {
                portalRoot = (PortalRoot) pcd.findRoot();
                if (portalRoot == null) {
                    pcd.createRoot("default configuration");
                }
            } catch (IllegalStateException x) {
                throw x;
            }
        } else {
            List<PortalRoot> roots = pcd.listPortalRoots();
            if (roots.isEmpty()) {
                portalRoot = (PortalRoot) pcd.createRoot(rootName);
                pcd.setRoot(portalRoot);
            } else {
                for (PortalRoot root : roots) {
                    if (root.getLabel().equals(rootName)) {
                        portalRoot = root;
                        pcd.setRoot(root);
                        break;
                    }
                }
            }
        }
        ConfigRecord configRecord = new ConfigRecord();
        configRecord.fileName = file.getCanonicalPath();
        configRecord.id = id;
        configRecord.pcd = pcd;
        getConfigs().put(id, configRecord);
        PortalParametersModel ppm = null;
        if (!portalRoot.hasA(PortalVocabulary.HAS_PORTAL_PARAMETERS)) {
            ppm = new PortalParametersModel(portalRoot.getMyThingSession(), UriRefFactory.uriRef());
            portalRoot.setPortalParametersModel(ppm);
        } else {
            ppm = portalRoot.getPortalParametersModel();
        }
        setAtt(request, "callback", portalRoot.getPortalParametersModel().getCallback(), PortalVocabulary.PORTAL_CALLBACK_URI);
        setAtt(request, "success", portalRoot.getPortalParametersModel().getSuccess(), PortalVocabulary.PORTAL_SUCCESS_URI);
        setAtt(request, "failure", portalRoot.getPortalParametersModel().getFailure(), PortalVocabulary.PORTAL_FAILURE_URI);
        setAtt(request, "portalName", portalRoot.getPortalParametersModel().getName(), PortalVocabulary.PORTAL_NAME);
        setAtt(request, "portalSkin", portalRoot.getPortalParametersModel().getSkin(), PortalVocabulary.PORTAL_SKIN);
        request.setAttribute("storeType", ConfigConstants.STORE_TYPE);
        if (portalRoot.hasA(SQLVocabulary.HAS_STORE)) {
            StoreModel storeModel = portalRoot.getStore();
            if (storeModel.isA(SQLVocabulary.MEMORY_STORE_TYPE)) {
                request.setAttribute("memoryStoreChecked", "checked");
            } else if (storeModel.isA(SQLVocabulary.FILE_STORE_TYPE)) {
                request.setAttribute("fileStoreChecked", "checked");
            } else if (storeModel.isA(SQLVocabulary.POSTGRES_STORE_TYPE)) {
                request.setAttribute("postgresStoreChecked", "checked");
            }
        }
        request.setAttribute("memoryStore", SQLVocabulary.MEMORY_STORE_TYPE);
        request.setAttribute("fileStore", SQLVocabulary.FILE_STORE_TYPE);
        request.setAttribute("postgresStore", SQLVocabulary.POSTGRES_STORE_TYPE);
        addState(request, id, CONFIG_STATUS_BASIC);
        saveConfig(id);
        fwd(request, response, "setup-basic.jsp");
    }

    protected static SecureRandom getRandom() {
        if (random == null) {
            random = new SecureRandom();
        }
        return random;
    }

    static SecureRandom random;

    protected String newID() {
        return "id-" + Math.abs(getRandom().nextLong());
    }

    /**
     * Sets a pair of attributes of the form <BR><BR>
     * keyName -> predicate <BR>
     * keyName+"Value" -> value<BR>
     * This checks that value is not null, then invokes toString on it (so you can pass URLs and URIs as needed
     * without a lot of messy checking).
     * In the form, the field name will be pred and the default value will be set to value.
     * You can then recover the value the usr types in by issuing getParameter(req, pred)
     * <p/>
     * This lets you set field names with "disposable" identifiers which map into the
     * vocabulary. This means you do not need to have big long lists of constants or
     * really much else in the way of machinery. Just make sure the JSP page follows the
     * convention of referring to the field name as "keyName" and the default value
     * as "keyNameValue".
     *
     * @param req
     * @param keyName
     * @param value
     * @param pred
     */
    protected void setAtt(HttpServletRequest req, String keyName, Object value, Resource pred) {
        req.setAttribute(keyName, pred);
        if (value != null) {
            req.setAttribute(keyName + "Value", value.toString());
        }
    }

    private void getConfigFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setAttribute("filePath", "filePath");
        File f = new File(ConfigConstants.CONFIG_FILE_PATH + ConfigConstants.CONFIG_FILE_NAME);
        if (f.exists()) {
            request.setAttribute("defaultFile", f.getCanonicalPath());
        } else {
            if (getServletContext().getInitParameter(PortalConfigurationDepot.CONFIG_FILE_PROPERTY) != null) {
                f = new File(getServletContext().getInitParameter(PortalConfigurationDepot.CONFIG_FILE_PROPERTY));
                request.setAttribute("defaultFile", f.getCanonicalPath());
            } else {
                request.setAttribute("defaultFile", "cfg.rdf");
            }
        }
        setAtt(request, "configName", "", Rdfs.LABEL);
        addState(request, newID(), CONFIG_STATUS_LOAD_FILE);
        fwd(request, response, "/setup-init.jsp");
    }

    /**
     * Every form has the same basic state. (This just makes sure it all gets written right.
     *
     * @param request
     * @param id
     * @param nextState
     */
    private void addState(HttpServletRequest request, String id, String nextState) {
        request.setAttribute(CONFIG_STATUS_KEY, CONFIG_STATUS_KEY);
        request.setAttribute(CONFIG_IDENTIFIER_KEY, CONFIG_IDENTIFIER_KEY);
        request.setAttribute("configIdentifier", id);
        request.setAttribute("configStatus", nextState);
        request.setAttribute(FORM_ACTION, getFormAction(request));
    }

    protected boolean isEmpty(String x) {
        return JSPUtil.isEmpty(x);
    }

    /**
     * Convenience since we use it so darned much
     *
     * @param request
     * @param key
     * @return
     */
    protected String getParameter(HttpServletRequest request, Object key) {
        return JSPUtil.getParameter(request, key);
    }

    protected int lookupAction(HttpServletRequest request) {
        String toDo = getParameter(request, CONFIG_STATUS_KEY);
        if (isEmpty(toDo)) return __NO_OP;
        if (toDo.equals(CONFIG_STATUS_BASIC)) return __BASIC;
        if (toDo.equals(CONFIG_STATUS_LOAD_FILE)) return __LOAD_CONFIG;
        if (toDo.equals(CONFIG_STATUS_SETUP_FILE_STORE)) return __SETUP_FILE_STORE;
        if (toDo.equals(CONFIG_STATUS_SETUP_POSTGRES_STORE)) return __SETUP_POSTGRES_STORE;
        if (toDo.equals(CONFIG_STATUS_STORE_ADMIN)) return __SETUP_STORE_ADMIN;
        return __NO_OP;
    }

    protected void fwd(HttpServletRequest req, HttpServletResponse res, String resource) throws IOException, ServletException {
        JSPUtil.fwd(req, res, resource);
    }

    public static PortalRoot getConfig(ServletContext context) throws IOException, OperatorException {
        File f = new File(context.getRealPath(CONFIG_FILE_PATH + ConfigConstants.CONFIG_FILE_NAME));
        PortalConfigurationDepot portalConfigurationDepot = null;
        if (f.exists()) {
            portalConfigurationDepot = new PortalConfigurationDepot(f);
        } else {
            portalConfigurationDepot = new PortalConfigurationDepot();
        }
        if (portalConfigurationDepot.findRoot() == null) {
            portalConfigurationDepot.createRoot(UriRefFactory.uriRef());
        }
        return (PortalRoot) portalConfigurationDepot.getCurrentConfiguration();
    }

    public void saveConfig(String id) throws Exception {
        ConfigRecord configRecord = getConfigs().get(id);
        if (configRecord.pcd != null) {
            configRecord.pcd.save();
            configRecord.pcd.serialize(configRecord.fileName);
            return;
        }
        throw new IllegalStateException("Error: no configuration associated with id \"" + id + "\"");
    }

    protected void handleException(Throwable t, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        warn("Error: There was an error. Here is the stack trace.");
        t.printStackTrace();
        JSPUtil.handleException(t, request, response, "/setup-error.jsp");
    }
}
