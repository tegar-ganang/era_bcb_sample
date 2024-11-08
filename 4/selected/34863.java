package uk.ac.ncl.neresc.dynasoar.hostProvider.SQLServer2005StoredProcInstaller;

import uk.ac.ncl.neresc.dynasoar.Interfaces.ServiceProvider.ServiceInstaller;
import uk.ac.ncl.neresc.dynasoar.client.codestore.codestore.CodeStoreServiceLocator;
import uk.ac.ncl.neresc.dynasoar.client.codestore.messages.ServiceCodeType;
import uk.ac.ncl.neresc.dynasoar.dataObjects.ServiceObject;
import uk.ac.ncl.neresc.dynasoar.exceptions.ConfigurationException;
import uk.ac.ncl.neresc.dynasoar.exceptions.EvilDynamicStoredProcedureException;
import uk.ac.ncl.neresc.dynasoar.exceptions.UnableToDeployException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.*;
import java.util.Properties;
import java.util.Vector;

/**
 * Code Monkey: Einar
 * <p/>
 * <p/>
 * All bugs, comments and complaints to einar@vollset.org
 */
public class SQLServer2005SPInstaller implements ServiceInstaller {

    enum StoredProcedureType {

        TSQL, DOTNET
    }

    class ERROR_CODES {

        public static final int ALREADY_THERE = 2714;

        public static final int SQL_BROKEN = 102;

        public static final int NO_PERMISSION_TO_EXPOSE = 6004;

        public static final int NAME_ALREADY_EXISTS = 6613;

        public static final int ASSEMBLY_EXISTS_ALREADY = 6246;

        public static final int URL_ALREADY_EXISTS = 7806;
    }

    enum EXISTING_ENDPOINT_STRATEGY {

        REMOVE_EXISTING_ENDPOINT, ADD_METHODS_TO_EXISTING_ENDPOINT, FAIL
    }

    enum EXISTING_STORED_PROC_STRATEGY {

        OVERWRITE_EXISTING_PROCEDURE, FAIL
    }

    enum EXISTING_ASSEMBLY_STRATEGY {

        DELETE_EXISTING_ASSEMBLY, USE_EXISTING_ASSEMBLY, FAIL
    }

    public SQLServer2005SPInstaller() throws ConfigurationException {
    }

    /**
   * Returns the code from the codestore.
   *
   * @param codeId            The unique id used to identify the stored procedure in the
   *                          codestore
   * @param codeStoreLocation The URL of the codestore
   * @return A byte[] containing the fetched code.
   *
   * @throws EvilDynamicStoredProcedureException
   *          if something goes wrong.
   */
    public byte[] getCode(String codeId, String codeStoreLocation) throws EvilDynamicStoredProcedureException {
        ServiceCodeType codestore = null;
        try {
            codestore = ((new CodeStoreServiceLocator()).getCodeStoreService(new URL(codeStoreLocation))).getServiceCode(codeId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new EvilDynamicStoredProcedureException("Failed obtaining anything sensible from the CodeStore: " + codeStoreLocation + ", giving up. Tried getting " + codeId, e);
        }
        if (codestore == null) {
            throw new EvilDynamicStoredProcedureException("ServiceCode is NULL, though no exception thrown when attempting to get it", new NullPointerException());
        }
        String storedProcedureURLString = codestore.getCodeStoreEndpoint().toString();
        if (storedProcedureURLString.equals("ERROR")) {
            throw new EvilDynamicStoredProcedureException("Tried to search for a non-existant file :" + storedProcedureURLString, new FileNotFoundException(storedProcedureURLString));
        }
        ReadableByteChannel channelIn = null;
        ByteBuffer buf = null;
        int codeLength = -1;
        try {
            URL storedProcedureURL = new URL(storedProcedureURLString);
            URLConnection con = storedProcedureURL.openConnection();
            channelIn = Channels.newChannel(storedProcedureURL.openStream());
            codeLength = con.getContentLength();
            buf = ByteBuffer.allocate(codeLength);
            channelIn.read(buf);
            if (codeLength <= 0) throw new IOException("Zero length stored procedure found..");
            channelIn.close();
        } catch (IOException e) {
            throw new EvilDynamicStoredProcedureException("Failed obtaining code from codestore URL: " + storedProcedureURLString, e);
        }
        return buf.array();
    }

    public void installTSQLStoredProcedure(String storedProcedureName, String storedProcSQL, EXISTING_STORED_PROC_STRATEGY existingEndpointStrategy, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        boolean done = false;
        while (!done) {
            try {
                Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                executeSQL(storedProcSQL, conn);
                closeDatabaseConnection(conn);
                done = true;
            } catch (SQLException e) {
                switch(e.getErrorCode()) {
                    case ERROR_CODES.ALREADY_THERE:
                        {
                            System.err.println("Stored procedure'" + storedProcedureName + "' already installed in database '" + databaseName + "'");
                            switch(existingEndpointStrategy) {
                                case OVERWRITE_EXISTING_PROCEDURE:
                                    {
                                        System.err.println("Attempting overwrite..");
                                        existingEndpointStrategy = EXISTING_STORED_PROC_STRATEGY.FAIL;
                                        deleteStoredProcedure(storedProcedureName, jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                                        break;
                                    }
                                default:
                                    {
                                        throw new EvilDynamicStoredProcedureException("Not allowed to overwrite existing stored procedure, or tried DROP'ing once already", e);
                                    }
                            }
                            break;
                        }
                    default:
                        {
                            throw new EvilDynamicStoredProcedureException("Unknown SQL error code when attempting install stored procedure: " + e.getErrorCode(), e);
                        }
                }
            }
        }
    }

    public void deleteStoredProcedure(String storedProcedureName, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        try {
            Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
            executeSQL("DROP PROCEDURE " + storedProcedureName, conn);
            closeDatabaseConnection(conn);
        } catch (SQLException e2) {
            throw new EvilDynamicStoredProcedureException("Can't seem to DROP already installed procedure, bailing out", e2);
        }
    }

    public void installDotNetAssembly(String assemblyName, byte[] assembly, EXISTING_ASSEMBLY_STRATEGY assemblyStrategy, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        String targetFileName = System.getProperty("java.io.tmpdir") + assemblyName + ".dll";
        File targetFile = new File(targetFileName);
        try {
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(targetFile, false);
            out.write(assembly);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new EvilDynamicStoredProcedureException("Failed in storing .dll on local filesystem. Bailing out", e);
        }
        String installAssemblySQL = "CREATE ASSEMBLY " + assemblyName + " FROM '" + targetFileName + "' \n";
        boolean done = false;
        while (!done) {
            try {
                Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                executeSQL(installAssemblySQL, conn);
                closeDatabaseConnection(conn);
                done = true;
            } catch (SQLException e) {
                switch(e.getErrorCode()) {
                    case ERROR_CODES.SQL_BROKEN:
                        {
                            throw new EvilDynamicStoredProcedureException("Broken SQL when attempting to install assembly: \n " + installAssemblySQL + "\n", e);
                        }
                    case ERROR_CODES.ASSEMBLY_EXISTS_ALREADY:
                        {
                            System.err.println("Attempting to install duplicate .NET assembly");
                            switch(assemblyStrategy) {
                                case DELETE_EXISTING_ASSEMBLY:
                                    {
                                        System.err.println("Attempting to delete assembly.");
                                        assemblyStrategy = EXISTING_ASSEMBLY_STRATEGY.FAIL;
                                        deleteDotNetAssembly(assemblyName, jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                                        break;
                                    }
                                case USE_EXISTING_ASSEMBLY:
                                    {
                                        System.err.println("Using existing assembly");
                                        done = true;
                                        break;
                                    }
                                case FAIL:
                                    {
                                        throw new EvilDynamicStoredProcedureException("Duplicate .NET assembly, I may have tried and failed to remove it; remove.", e);
                                    }
                            }
                            break;
                        }
                    default:
                        {
                            throw new EvilDynamicStoredProcedureException("Unknown SQL error code when attempting install .Net assembly: " + e.getErrorCode(), e);
                        }
                }
            }
        }
    }

    public void deleteDotNetAssembly(String assemblyName, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        try {
            Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
            executeSQL("DROP ASSEMBLY " + assemblyName, conn);
            closeDatabaseConnection(conn);
        } catch (SQLException e2) {
            throw new EvilDynamicStoredProcedureException("Can't seem to DELETE already installed assembly, bailing out", e2);
        }
    }

    public void exposeStoredProcedureFromDotNetAssembly(Config.StoredProc spConf, String assemblyName, EXISTING_STORED_PROC_STRATEGY storedProcStrategy, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        String createProcedureSQL = "CREATE PROCEDURE " + spConf.toString() + " AS EXTERNAL NAME " + assemblyName + ".StoredProcedures." + spConf.name;
        boolean done = false;
        while (!done) {
            try {
                Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                executeSQL(createProcedureSQL, conn);
                closeDatabaseConnection(conn);
                done = true;
            } catch (SQLException e) {
                switch(e.getErrorCode()) {
                    case ERROR_CODES.ALREADY_THERE:
                        {
                            System.err.println("Stored procedure'" + spConf.name + "' already installed in database '" + databaseName + "'");
                            switch(storedProcStrategy) {
                                case OVERWRITE_EXISTING_PROCEDURE:
                                    {
                                        System.err.println("Attempting overwrite..");
                                        try {
                                            Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                                            executeSQL("DROP PROCEDURE " + spConf.name, conn);
                                            closeDatabaseConnection(conn);
                                            storedProcStrategy = EXISTING_STORED_PROC_STRATEGY.FAIL;
                                        } catch (SQLException e2) {
                                            throw new EvilDynamicStoredProcedureException("Can't seem to DROP already installed procedure, bailing out", e2);
                                        }
                                        break;
                                    }
                                default:
                                    {
                                        throw new EvilDynamicStoredProcedureException("Not allowed to overwrite existing stored procedure, or tried DROP'ing once already", e);
                                    }
                            }
                            break;
                        }
                    default:
                        {
                            throw new EvilDynamicStoredProcedureException("Unknown SQL error code when attempting install stored procedure: " + createProcedureSQL + e.getErrorCode(), e);
                        }
                }
            }
        }
    }

    public String createSoapEndpointForStoredProcedure(String storedProcedure, String endpointName, String site, EXISTING_ENDPOINT_STRATEGY endpointStrategy, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        CreateEndpoint createEndpoint = new CreateEndpoint(endpointName);
        createEndpoint.addDefaultAS_HTTPclause(site);
        createEndpoint.addDefaultFOR_SOAPclause(storedProcedure, databaseName);
        String exposeStoredProcSQL = createEndpoint.toSQL();
        boolean done = false;
        while (!done) {
            try {
                Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                executeSQL(exposeStoredProcSQL, conn);
                closeDatabaseConnection(conn);
                done = true;
            } catch (SQLException e) {
                switch(e.getErrorCode()) {
                    case ERROR_CODES.NAME_ALREADY_EXISTS:
                        {
                            if (e.getLocalizedMessage().contains(createEndpoint.endPointName)) {
                                System.err.println("Duplicate endpoint name: " + createEndpoint.endPointName);
                                switch(endpointStrategy) {
                                    case REMOVE_EXISTING_ENDPOINT:
                                        {
                                            System.err.println("Attempting to DROP existing endpoint.");
                                            endpointStrategy = EXISTING_ENDPOINT_STRATEGY.FAIL;
                                            deleteSoapEndpoint(createEndpoint.endPointName, jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
                                            break;
                                        }
                                    case ADD_METHODS_TO_EXISTING_ENDPOINT:
                                        {
                                            System.err.println("Altering existing endpoint to include webmethod(s):");
                                            for (CreateEndpoint.FOR_SOAP.EXPOSED_METHOD method : createEndpoint.for_soap_clause.methods) System.err.println("   " + method.NAME);
                                            exposeStoredProcSQL = "ALTER ENDPOINT " + createEndpoint.endPointName + " \n" + createEndpoint.for_soap_clause.toSQLAdd();
                                            endpointStrategy = EXISTING_ENDPOINT_STRATEGY.FAIL;
                                            break;
                                        }
                                    case FAIL:
                                        {
                                            throw new EvilDynamicStoredProcedureException("Duplicate endpoint: " + createEndpoint.endPointName + ". May have failed to add more methods to endpoint if asked. ", e);
                                        }
                                }
                            } else {
                                Vector<CreateEndpoint.FOR_SOAP.EXPOSED_METHOD> methods = createEndpoint.for_soap_clause.methods;
                                for (CreateEndpoint.FOR_SOAP.EXPOSED_METHOD method : methods) {
                                    if (e.getLocalizedMessage().contains(method.NAME)) throw new EvilDynamicStoredProcedureException("Trying to expose the SAME stored procedure " + method.NAME + " twice in the same ENDPOINT. Not good, either use the existing webmethod: " + method.NAME + ", or remove it and try again.", e);
                                }
                                throw new EvilDynamicStoredProcedureException("There's some duplicate name somewhere, but I can't figure out which. See chained SQL exception and call me in the morning", e);
                            }
                            break;
                        }
                    case ERROR_CODES.URL_ALREADY_EXISTS:
                        {
                            System.err.println("URL already exists, changing it slightly");
                            createEndpoint.as_http_clause.PATH = "/WebMethods" + (int) (Math.random() * 3778);
                            exposeStoredProcSQL = createEndpoint.toSQL();
                            break;
                        }
                    case ERROR_CODES.NO_PERMISSION_TO_EXPOSE:
                        {
                            throw new EvilDynamicStoredProcedureException("This installer does not have the right permissions to expose a stored procedure as configured.", e);
                        }
                    case ERROR_CODES.SQL_BROKEN:
                        {
                            throw new EvilDynamicStoredProcedureException("Broken SQL when attempting to install endpoint: " + exposeStoredProcSQL, e);
                        }
                    default:
                        {
                            throw new EvilDynamicStoredProcedureException("Unknown SQL error code when attempting install endpoint. " + e.getErrorCode() + " " + exposeStoredProcSQL, e);
                        }
                }
            }
        }
        return "http://" + createEndpoint.as_http_clause.SITE + createEndpoint.as_http_clause.PATH + "?wsdl";
    }

    public void deleteSoapEndpoint(String endpointName, String jdbcDriverName, String jdbcUrl, String databaseName, String hpUserName, String hpPassword) throws EvilDynamicStoredProcedureException {
        try {
            Connection conn = establishDatabaseConnection(jdbcDriverName, jdbcUrl, databaseName, hpUserName, hpPassword);
            executeSQL("DROP ENDPOINT " + "DynaSoarGeneratedEndpoint_" + endpointName, conn);
            closeDatabaseConnection(conn);
        } catch (SQLException e1) {
            throw new EvilDynamicStoredProcedureException("Failed attempting to DROP ENDPOINT " + endpointName, e1);
        }
    }

    public String installCode(String codeStoreLocation, Config config) throws EvilDynamicStoredProcedureException {
        if (config.procs.length > 1 && config.strategies.endpointStrategy != EXISTING_ENDPOINT_STRATEGY.ADD_METHODS_TO_EXISTING_ENDPOINT) throw new EvilDynamicStoredProcedureException("SANITY CHECK: It's no good attempting to add more than one method if you don't allow me to add to an existing endpoint. Solution: Set STRATEGY to ADD_METHODS_TO_EXISTING_ENDPOINT", new Exception());
        if (config.type == StoredProcedureType.DOTNET && config.assemblyName == null) throw new EvilDynamicStoredProcedureException("SANITY CHECK: If you want me to install a stored procedure from a CLR assembly, you need to give me the name of the assembly..", new NullPointerException());
        if (config.type == StoredProcedureType.TSQL && config.procs.length != 1) throw new EvilDynamicStoredProcedureException("SANITY CHECK: You can only add one T-SQL Stored procedure at a time", new Exception());
        if (config.procs.length < 1) throw new EvilDynamicStoredProcedureException("SANITY CHECK: You must specify at least one stored procedure.. Holy moly..", new Exception());
        String wsdl = null;
        switch(config.type) {
            case TSQL:
                {
                    String sp = new String(getCode(config.procs[0].name, codeStoreLocation));
                    installTSQLStoredProcedure(config.procs[0].name, sp, config.strategies.storedProcStrategy, config.db.jdbcDriverName, config.db.jdbcUrl, config.db.databaseName, config.db.hpUserName, config.db.hpPassword);
                    wsdl = createSoapEndpointForStoredProcedure(config.procs[0].name, config.procs[0].name, config.db.site, config.strategies.endpointStrategy, config.db.jdbcDriverName, config.db.jdbcUrl, config.db.databaseName, config.db.hpUserName, config.db.hpPassword);
                    break;
                }
            case DOTNET:
                {
                    byte[] assembly = getCode(config.assemblyName, codeStoreLocation);
                    installDotNetAssembly(config.assemblyName, assembly, config.strategies.assemblyStrategy, config.db.jdbcDriverName, config.db.jdbcUrl, config.db.databaseName, config.db.hpUserName, config.db.hpPassword);
                    for (Config.StoredProc sp : config.procs) exposeStoredProcedureFromDotNetAssembly(sp, config.assemblyName, config.strategies.storedProcStrategy, config.db.jdbcDriverName, config.db.jdbcUrl, config.db.databaseName, config.db.hpUserName, config.db.hpPassword);
                    for (Config.StoredProc sp : config.procs) wsdl = createSoapEndpointForStoredProcedure(sp.name, config.assemblyName, config.db.site, config.strategies.endpointStrategy, config.db.jdbcDriverName, config.db.jdbcUrl, config.db.databaseName, config.db.hpUserName, config.db.hpPassword);
                    break;
                }
            default:
                {
                    throw new RuntimeException("Can't figure out which type of stored procedure this is.");
                }
        }
        return wsdl;
    }

    /**
   * Creates the connection to the database.
   *
   * @param jdbcDriverClassName The class name string of the JDBC driver to use.
   * @param databaseURL         The URL to the database (starting with jdbc: )
   * @param databaseName        The name of the database to create the stored procedure in.
   * @param userName            The username to use to login to the database - MUST have the
   *                            correct credentials (be able to create stored procedures)
   * @param password            For userName..
   */
    public Connection establishDatabaseConnection(String jdbcDriverClassName, String databaseURL, String databaseName, String userName, String password) {
        Properties props = new Properties();
        props.setProperty("user", userName);
        props.setProperty("password", password);
        props.setProperty("databaseName", databaseName);
        return establishDatabaseConnection(jdbcDriverClassName, databaseURL, props);
    }

    /**
   * Alternate way to connect to the database if you need to set non-standard options (pass them
   * as a Properties)
   *
   * @param jdbcDriverClassName
   * @param databaseURL
   * @param connectionProperties
   */
    private Connection establishDatabaseConnection(String jdbcDriverClassName, String databaseURL, Properties connectionProperties) {
        Connection conn = null;
        try {
            Class.forName(jdbcDriverClassName);
            conn = DriverManager.getConnection(databaseURL, connectionProperties);
        } catch (ClassNotFoundException e) {
            System.err.println("Genesis creation failed - have you got the right JDBC jar in classpath?");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Problem connecting to database - has HP user/password been added and correct credentials given?");
            System.err.println("(also.. in SQLServer, MIXED AUTHENTICATION must be enabled)");
            e.printStackTrace();
        }
        try {
            assert conn != null;
            DatabaseMetaData meta = conn.getMetaData();
        } catch (SQLException e) {
            System.err.println("Screwed up getting meta data - most peculiar..");
            e.printStackTrace();
        }
        return conn;
    }

    /**
   * Closes the connection to the database.
   */
    private void closeDatabaseConnection(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
            conn = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
   * Executes the provided CreateEndpoint.SQLServer2005.CreateEndpoint on the given connection.
   * <p/>
   * Not invoked directly
   *
   * @param sqlStatement The CreateEndpoint.SQLServer2005.CreateEndpoint to execute
   * @param conn         The JDBC connection to the DB to execute on.
   */
    private void executeSQL(String sqlStatement, Connection conn) throws SQLException {
        if (conn == null) throw new SQLException("Not connected to any database.. JDBC = null");
        Statement st = conn.createStatement();
        st.execute(sqlStatement);
    }

    public String installCode(ServiceObject service) throws UnableToDeployException {
        try {
            byte[] configBytes = getCode(service.getId() + "_conf", service.getCodeStoreLocation());
            Config conf = null;
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(configBytes));
                conf = (Config) ois.readObject();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return installCode(service.getCodeStoreLocation(), conf);
        } catch (EvilDynamicStoredProcedureException dynoExceptionType) {
            dynoExceptionType.printStackTrace();
            throw new UnableToDeployException(dynoExceptionType.getLocalizedMessage());
        }
    }

    /**
   * This class represents the state which a call to create an HTTP endpoint for SOAP on
   * SQLServer 2005 can contain. It tries to have sensible defaults where possible
   */
    class CreateEndpoint {

        String endPointName = "DynaSoarGeneratedEndpoint_";

        String STATE = "STARTED";

        AS_HTTP as_http_clause = null;

        FOR_SOAP for_soap_clause = null;

        /**
     * Creates a CREATE ENDPOINT class. Must add a FOR SOAP and an AS HTTP clause after this.
     */
        public CreateEndpoint(String endpointName) {
            this.endPointName += endpointName;
        }

        /**
     * Attempst to create a sensible default for the AS HTTP clause Returns a reference to the
     * generated clause to hack around with.
     *
     * @return A reference to the generated AS HTTP class for hacking.
     */
        public CreateEndpoint.AS_HTTP addDefaultAS_HTTPclause(String site) {
            this.as_http_clause = new AS_HTTP();
            this.as_http_clause.SITE = site;
            return this.as_http_clause;
        }

        /**
     * A class representing the AS HTTP clause inside the CREATE ENDPOINT sql.
     */
        class AS_HTTP {

            String PATH = "/WebMethods";

            String AUTHENTICATION = "INTEGRATED";

            String PORTS = "CLEAR";

            String SITE = null;

            String CLEAR_PORT = null;

            String SSL_PORT = null;

            String AUTH_REALM = null;

            String DEFAULT_LOGON_DOMAIN = null;

            String COMPRESSION = null;

            /**
       * Generates the CreateEndpoint.SQLServer2005.CreateEndpoint for the AS HTTP clause
       * inside the CREATE ENDPOINT.
       *
       * @return
       */
            public String toSQL() {
                if (PATH == null) throw new RuntimeException("You have to specify a PATH for the endpoint..");
                String sql = "AS HTTP \n(\n" + "    PATH = '" + PATH + "',\n" + "    AUTHENTICATION = (" + AUTHENTICATION + "),\n" + "    PORTS = (" + PORTS + ") \n";
                if (SITE != null) sql += "    ,SITE = '" + SITE + "'\n";
                if (CLEAR_PORT != null) sql += "    ,CLEAR_PORT = " + CLEAR_PORT + "\n";
                if (SSL_PORT != null) sql += "    ,SSL_PORT = " + SSL_PORT + "\n";
                if (AUTH_REALM != null) sql += "    ,AUTH_REALM = " + AUTH_REALM + "\n";
                if (DEFAULT_LOGON_DOMAIN != null) sql += "    ,DEFAULT_LOGON_DOMAIN = " + DEFAULT_LOGON_DOMAIN + "\n";
                if (COMPRESSION != null) sql += "    ,COMPRESSION = " + COMPRESSION + "\n";
                sql += ")\n";
                return sql;
            }
        }

        /**
     * Attempts to create a sensible default FOR SOAP clause for use inside the CREATE ENDPOINT
     * CreateEndpoint.SQLServer2005.CreateEndpoint. Only asks for the bare minimum, but returns
     * a reference to the created class to play around with (e.g. exposing more than one stored
     * procedure to be exposed)
     *
     * @param storedProcedureName
     * @param database            Not _strictly_ required, but would be dumb without. You can
     *                            reset to DEFAULT by hand if you want.
     * @return a reference to the FOR SOAP clause to hack around with.
     */
        public CreateEndpoint.FOR_SOAP addDefaultFOR_SOAPclause(String storedProcedureName, String database) {
            this.for_soap_clause = new FOR_SOAP();
            this.for_soap_clause.DATABASE = database;
            this.for_soap_clause.addWebMethod(storedProcedureName + ".ws", storedProcedureName);
            return this.for_soap_clause;
        }

        /**
     * A class representing the FOR SOAP clause of the CREATE ENDPOINT.
     */
        class FOR_SOAP {

            Vector<EXPOSED_METHOD> methods = null;

            String BATCHES = "DISABLED";

            String WSDL = "DEFAULT";

            String SESSIONS = null;

            String LOGIN_TYPE = null;

            String SESSION_TIMEOUT = null;

            String DATABASE = "DEFAULT";

            String NAMESPACE = "DEFAULT";

            String SCHEMA = null;

            String CHARACTER_SET = null;

            String HEADER_LIMIT = null;

            /**
       * This adds the required CreateEndpoint.SQLServer2005.CreateEndpoint to expose given
       * stored procedure as a webservice. At least one of these must be in each FOR SOAP
       * clause. You get returned a reference to the method, and can play around with the
       * parameters if you so please.
       *
       * @param webMethodName
       * @param storedProcedureName
       * @return A reference to the generated EXPOSED_METHOD for setting non-standard
       *         options.
       */
            public CreateEndpoint.FOR_SOAP.EXPOSED_METHOD addWebMethod(String webMethodName, String storedProcedureName) {
                CreateEndpoint.FOR_SOAP.EXPOSED_METHOD m = new CreateEndpoint.FOR_SOAP.EXPOSED_METHOD();
                m.WEBMETHOD = webMethodName;
                m.NAME = storedProcedureName;
                if (methods == null) methods = new Vector<EXPOSED_METHOD>();
                methods.add(m);
                return m;
            }

            /**
       * Each FOR_SOAP can have a number of WEBMETHODs, represented as EXPOSED_METHOD
       * classes
       * <p/>
       * This is such a clause.
       */
            class EXPOSED_METHOD {

                String WEBMETHOD = null;

                String NAME = null;

                String SCHEMA = null;

                String FORMAT = null;

                /**
         * Generates the CreateEndpoint.SQLServer2005.CreateEndpoint required for each
         * stored procedure to be exposed as a webmethod.
         *
         * @return
         */
                public String toSQL() {
                    if (WEBMETHOD == null) throw new RuntimeException("You must give the webservice to expose a name.");
                    if (NAME == null) throw new RuntimeException("You must specify the name of the stored procedure to expose.");
                    String sql = "    WEBMETHOD '" + WEBMETHOD + "' ( NAME = '" + DATABASE + ".dbo." + NAME + "' ";
                    if (SCHEMA != null) sql += "    ,SCHEMA = " + SCHEMA + " ";
                    if (FORMAT != null) sql += "    ,FORMAT = " + FORMAT + " ";
                    sql += "), \n";
                    return sql;
                }
            }

            public String toSQL() {
                if (methods == null || methods.size() == 0) throw new RuntimeException("You have to define at least one WebMethod to expose in FOR SOAP");
                String sql = "FOR SOAP\n";
                sql += "( \n";
                for (EXPOSED_METHOD method : methods) sql += method.toSQL();
                sql += "    BATCHES = " + BATCHES + ",\n";
                sql += "    WSDL = " + WSDL + ",\n";
                if (SESSIONS != null) sql += "    SESSIONS = " + SESSIONS + ",\n";
                if (LOGIN_TYPE != null) sql += "    LOGIN_TYPE = " + LOGIN_TYPE + ",\n";
                if (SESSION_TIMEOUT != null) sql += "    SESSION_TIMEOUT = " + SESSION_TIMEOUT + ",\n";
                if (SCHEMA != null) sql += "    SCHEMA = " + SCHEMA + ",\n";
                if (NAMESPACE != null) sql += "    NAMESPACE = " + NAMESPACE + ",\n";
                if (CHARACTER_SET != null) sql += "    CHARACTER_SET = " + CHARACTER_SET + ",\n";
                if (HEADER_LIMIT != null) sql += "    HEADER_LIMIT = " + HEADER_LIMIT + ",\n";
                sql += "    DATABASE = '" + DATABASE + "' \n)\n";
                return sql;
            }

            public String toSQLAdd() {
                if (methods == null || methods.size() == 0) throw new RuntimeException("You have to define at least one WebMethod to expose in FOR SOAP");
                String sql = "FOR SOAP\n";
                sql += "( \n";
                for (EXPOSED_METHOD method : methods) sql += "ADD " + method.toSQL();
                sql += "    DATABASE = '" + DATABASE + "' \n);\n";
                return sql;
            }
        }

        /**
     * Returns the CreateEndpoint.SQLServer2005.CreateEndpoint for the CREATE ENDPOINT required
     * to expose an ALREADY INSTALLED(!) stored procedure in SQLServer 2005. It will throw
     * various runtime exceptions if you've not done the required thing.
     *
     * @return
     */
        public String toSQL() {
            if (endPointName == null) throw new RuntimeException("You must specify a name for the endpoint.");
            if (as_http_clause == null) throw new RuntimeException("You must add an AS HTTP clause - use addDefaultAS_HTTPclause()");
            if (for_soap_clause == null) throw new RuntimeException("You must add a FOR SOAP clause - use addDefaultFOR_SOAPclause()");
            String sql = "CREATE ENDPOINT " + endPointName + " \n";
            sql += "    STATE = " + STATE + " \n";
            sql += as_http_clause.toSQL();
            sql += for_soap_clause.toSQL();
            return sql;
        }
    }
}
