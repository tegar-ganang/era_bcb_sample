package uk.ac.ebi.mydas.controller;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;
import org.apache.log4j.Logger;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;
import uk.ac.ebi.mydas.datasource.AnnotationDataSource;
import uk.ac.ebi.mydas.datasource.RangeHandlingAnnotationDataSource;
import uk.ac.ebi.mydas.datasource.RangeHandlingReferenceDataSource;
import uk.ac.ebi.mydas.datasource.ReferenceDataSource;
import uk.ac.ebi.mydas.datasource.WritebackDataSource;
import uk.ac.ebi.mydas.exceptions.*;
import uk.ac.ebi.mydas.model.*;
import za.ac.uct.mydas.writeback.HTTPMethod;
import za.ac.uct.mydas.writeback.MyDasWriteback;
import za.ac.uct.mydas.writeback.WritebackDocument;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * Created Using IntelliJ IDEA.
 * User: phil
 * Date: 04-May-2007
 * Time: 12:10:01
 * A DAS server allowing the easy creation of plugins to different data
 * sources that does not tie in the plugin developer to any particular API
 * (apart from the very simple interfaces defined by this API.)
 *
 * This DAS server provides a complete implementation of
 * <a href="http://biodas.org/documents/spec.html">
 *     Distributed Sequence Annotation Systems (DAS) Version 1.53
 * </a>
 *
 * @author Phil Jones, EMBL EBI, pjones@ebi.ac.uk
 */
@SuppressWarnings("serial")
public class MydasServlet extends HttpServlet {

    /**
     * Define a static logger variable so that it references the
     * Logger instance named "XMLUnmarshaller".
     */
    private static final Logger logger = Logger.getLogger(MydasServlet.class);

    /**
     * This pattern is used to parse the URI part of the request.
     * Returns two groups:
     *
     * <b>dsn command</b>
     * Group 1: "dsn"
     * Group 2: ""
     *
     * <b>All other commands</b>
     * Group 1: "DSN_NAME"
     * Group 2: "command"
     *
     * The URI part of the request as returned by <code>request.getRequestURI();</code>
     * should look like one of the following examples:
     *
     [PREFIX]/das/dsn

     [PREFIX]/das/dsnname/entry_points
     [PREFIX]/das/dsnname/dna
     [PREFIX]/das/dsnname/sequenceString
     [PREFIX]/das/DSNNAME/types
     [PREFIX]/das/dsnname/features
     [PREFIX]/das/dsnname/link
     [PREFIX]/das/dsnname/stylesheet
     */
    private static final Pattern REQUEST_URI_PATTERN = Pattern.compile("/das/([^\\s/?]+)/?([^\\s/?]*)$");

    private static final Pattern DAS_ONLY_URI_PATTERN = Pattern.compile("/das[/]?$");

    /**
     * Pattern used to parse a segment range, as used for the dna and sequenceString commands.
     * This can be used based on the assumption that the segments have already been split
     * into indidual Strings (i.e. by splitting on the ; character).
     * Three groups are returned from a match as follows:
     * Group 1: segment name
     * Group 3: start coordinate
     * Group 4: stop coordinate
     */
    private static final Pattern SEGMENT_RANGE_PATTERN = Pattern.compile("^segment=([^:\\s]*)(:(\\d+),(\\d+))?$");

    private static DataSourceManager DATA_SOURCE_MANAGER = null;

    private static final String RESOURCE_FOLDER = "/";

    private static final String CONFIGURATION_FILE_NAME = RESOURCE_FOLDER + "MydasServerConfig.xml";

    private static final String COMMAND_DSN = "dsn";

    private static final String COMMAND_DNA = "dna";

    private static final String COMMAND_TYPES = "types";

    private static final String COMMAND_LINK = "link";

    private static final String COMMAND_STYLESHEET = "stylesheet";

    private static final String COMMAND_FEATURES = "features";

    private static final String COMMAND_ENTRY_POINTS = "entry_points";

    private static final String COMMAND_SEQUENCE = "sequence";

    private static final String COMMAND_HISTORICAL = "historical";

    /**
     * List<String> of valid 'field' parameters for the link command.
     */
    public static final List<String> VALID_LINK_COMMAND_FIELDS = new ArrayList<String>(5);

    static {
        VALID_LINK_COMMAND_FIELDS.add(AnnotationDataSource.LINK_FIELD_CATEGORY);
        VALID_LINK_COMMAND_FIELDS.add(AnnotationDataSource.LINK_FIELD_FEATURE);
        VALID_LINK_COMMAND_FIELDS.add(AnnotationDataSource.LINK_FIELD_METHOD);
        VALID_LINK_COMMAND_FIELDS.add(AnnotationDataSource.LINK_FIELD_TARGET);
        VALID_LINK_COMMAND_FIELDS.add(AnnotationDataSource.LINK_FIELD_TYPE);
    }

    private static final String HEADER_KEY_X_DAS_VERSION = "X-DAS-Version";

    private static final String HEADER_KEY_X_DAS_STATUS = "X-DAS-Status";

    private static final String HEADER_KEY_X_DAS_CAPABILITIES = "X-DAS-Capabilities";

    private static final String HEADER_VALUE_CAPABILITIES = "dsn/1.0; dna/1.0; types/1.0; stylesheet/1.0; features/1.0; entry_points/1.0; error-segment/1.0; unknown-segment/1.0; feature-by-id/1.0; group-by-id/1.0; component/1.0; supercomponent/1.0; sequenceString/1.0";

    private static final String HEADER_VALUE_DAS_VERSION = "DAS/1.5";

    private static final String ENCODING_REQUEST_HEADER_KEY = "Accept-Encoding";

    private static final String ENCODING_RESPONSE_HEADER_KEY = "Content-Encoding";

    private static final String ENCODING_GZIPPED = "gzip";

    private static final String DAS_XML_NAMESPACE = null;

    private static XmlPullParserFactory PULL_PARSER_FACTORY = null;

    private static final String INDENTATION_PROPERTY = "http://xmlpull.org/v1/doc/properties.html#serializer-indentation";

    private static final String INDENTATION_PROPERTY_VALUE = "  ";

    static GeneralCacheAdministrator CACHE_MANAGER = null;

    private static MyDasWriteback writeback;

    /**
     * This method will ensure that all the plugins are registered and call
     * the corresonding init() method on all of the plugins.
     *
     * Also initialises the XMLPullParser factory.
     * @throws ServletException
     */
    public void init() throws ServletException {
        super.init();
        if (CACHE_MANAGER == null) {
            CACHE_MANAGER = new GeneralCacheAdministrator();
        }
        if (DATA_SOURCE_MANAGER == null) {
            DATA_SOURCE_MANAGER = new DataSourceManager(this.getServletContext());
            try {
                DATA_SOURCE_MANAGER.init(CACHE_MANAGER, CONFIGURATION_FILE_NAME);
            } catch (Exception e) {
                logger.error("Fatal Exception thrown at initialisation.  None of the datasources will be usable.", e);
                throw new IllegalStateException("Fatal Exception thrown at initialisation.  None of the datasources will be usable.", e);
            }
        }
        if (PULL_PARSER_FACTORY == null) {
            try {
                PULL_PARSER_FACTORY = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
                PULL_PARSER_FACTORY.setNamespaceAware(true);
                writeback = new MyDasWriteback(PULL_PARSER_FACTORY, this);
            } catch (XmlPullParserException xppe) {
                logger.error("Fatal Exception thrown at initialisation.  Cannot initialise the PullParserFactory required to allow generation of the DAS XML.", xppe);
                throw new IllegalStateException("Fatal Exception thrown at initialisation.  Cannot initialise the PullParserFactory required to allow generation of the DAS XML.", xppe);
            }
        }
    }

    /**
     * This method will ensure that call the corresponding destroy() method on
     * all of the registered plugins to allow them to clean up resources.
     */
    public void destroy() {
        super.destroy();
        if (DATA_SOURCE_MANAGER != null) {
            DATA_SOURCE_MANAGER.destroy();
        }
    }

    /**
     * Delegates to the parseAndHandleRequest method
     * @param request containing details of the request, including the command and command arguments.
     * @param response to which the HTTP header / XML response will be written
     * @throws ServletException as defined in the HTTPServlet interface.
     * @throws IOException as defined in the HTTPServlet interface.
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        parseAndHandleRequest(request, response);
    }

    /**
     * @param request containing details of the request, including the command and command arguments.
     * @param response to which the HTTP header / XML response will be written
     * @throws ServletException as defined in the HTTPServlet interface.
     * @throws IOException as defined in the HTTPServlet interface.
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            writeback.parseAndHandleWritebackRequest(request, response, HTTPMethod.HTTP_POST, getWritebackDataSource(request, response), dataSourceConfigWB);
        } catch (DataSourceException e) {
            logger.error("DataSourceException thrown by a data source.", e);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "The data source has thrown a 'DataSourceException' indicating a software error has occurred: " + e.getMessage(), request, response);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            writeback.parseAndHandleWritebackRequest(request, response, HTTPMethod.HTTP_DELETE, getWritebackDataSource(request, response), dataSourceConfigWB);
        } catch (DataSourceException e) {
            logger.error("BadCommandException thrown", e);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "Authentication error. Openid couldn't be validated.", request, response);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            writeback.parseAndHandleWritebackRequest(request, response, HTTPMethod.HTTP_PUT, getWritebackDataSource(request, response), dataSourceConfigWB);
        } catch (DataSourceException e) {
            logger.error("BadCommandException thrown", e);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "Authentication error. Openid couldn't be validated.", request, response);
        }
    }

    private DataSourceConfiguration dataSourceConfigWB;

    private WritebackDataSource getWritebackDataSource(HttpServletRequest request, HttpServletResponse response) throws IOException {
        dataSourceConfigWB = null;
        String uri = request.getRequestURI();
        Matcher match = REQUEST_URI_PATTERN.matcher(uri);
        try {
            if (DATA_SOURCE_MANAGER == null || DATA_SOURCE_MANAGER.getServerConfiguration() == null || DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration() == null || DATA_SOURCE_MANAGER.getServerConfiguration().getDataSourceConfigMap() == null) {
                throw new ConfigurationException("The datasources were not initialized successfully.");
            }
            if (match.find()) {
                if (!COMMAND_DSN.equals(match.group(1))) {
                    String dsnName = match.group(1);
                    dataSourceConfigWB = DATA_SOURCE_MANAGER.getServerConfiguration().getDataSourceConfigMap().get(dsnName);
                    if (dataSourceConfigWB != null) {
                        if (dataSourceConfigWB.isOK()) {
                            return ((WritebackDataSource) (dataSourceConfigWB.getDataSource()));
                        }
                    }
                }
            } else throw new BadCommandException("The command is not recognised.");
        } catch (ConfigurationException ce) {
            logger.error("ConfigurationException thrown: This mydas installation was not correctly initialised.", ce);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "This installation of MyDas is not correctly configured.", request, response);
        } catch (DataSourceException dse) {
            logger.error("DataSourceException thrown by a data source.", dse);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "The data source has thrown a 'DataSourceException' indicating a software error has occurred: " + dse.getMessage(), request, response);
        } catch (BadCommandException bce) {
            logger.error("BadCommandException thrown", bce);
            writeHeader(request, response, XDasStatus.STATUS_400_BAD_COMMAND, false);
            reportError(XDasStatus.STATUS_400_BAD_COMMAND, "Bad Command - Command not recognised as a valid DAS command.", request, response);
        }
        return null;
    }

    /**
     * Handles requests encoded as GET or POST.
     * First of all splits up the request and then delegates to an appropriate method
     * to respond to this request.  Only basic checking of the request is done here - checking of command
     * arguments is the responsibility of the handling method.
     *
     * This method also handles all exceptions that can be reported as defined DAS errors and returns the
     * appropriate X-DAS-STATUS HTTP header in the event of a problem.
     * @param request The http request object.
     * @param response The response - normally an XML file in HTTP/1.0 protocol.
     * @throws ServletException in the event of an internal error
     * @throws IOException in the event of a low level I/O error.
     */
    private void parseAndHandleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String queryString = request.getQueryString();
        if (logger.isDebugEnabled()) {
            logger.debug("RequestURI: '" + request.getRequestURI() + "'");
            logger.debug("Query String: '" + queryString + "'");
        }
        Matcher match = REQUEST_URI_PATTERN.matcher(request.getRequestURI());
        try {
            if (DATA_SOURCE_MANAGER == null || DATA_SOURCE_MANAGER.getServerConfiguration() == null || DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration() == null || DATA_SOURCE_MANAGER.getServerConfiguration().getDataSourceConfigMap() == null) {
                throw new ConfigurationException("The datasources were not initialized successfully.");
            }
            if (match.find()) {
                if (COMMAND_DSN.equals(match.group(1))) {
                    if (match.group(2) == null || match.group(2).length() == 0) {
                        dsnCommand(request, response, queryString);
                    } else {
                        throw new BadCommandException("A bad dsn command has been sent to the server, including unrecognised additional query parameters.");
                    }
                } else {
                    String dsnName = match.group(1);
                    String command = match.group(2);
                    if (logger.isDebugEnabled()) {
                        logger.debug("dsnName: '" + dsnName + "'");
                        logger.debug("command: '" + command + "'");
                    }
                    DataSourceConfiguration dataSourceConfig = DATA_SOURCE_MANAGER.getServerConfiguration().getDataSourceConfigMap().get(dsnName);
                    if (dataSourceConfig != null) {
                        if (dataSourceConfig.isOK()) {
                            if (COMMAND_DNA.equals(command)) {
                                dnaCommand(request, response, dataSourceConfig, queryString);
                            } else if (COMMAND_TYPES.equals(command)) {
                                typesCommand(request, response, dataSourceConfig, queryString);
                            } else if (COMMAND_STYLESHEET.equals(command)) {
                                stylesheetCommand(request, response, dataSourceConfig, queryString);
                            } else if (COMMAND_FEATURES.equals(command)) {
                                featuresCommand(request, response, dataSourceConfig, queryString);
                            } else if (COMMAND_ENTRY_POINTS.equals(command)) {
                                entryPointsCommand(request, response, dataSourceConfig, queryString);
                            } else if (COMMAND_SEQUENCE.equals(command)) {
                                sequenceCommand(request, response, dataSourceConfig, queryString);
                            } else if (COMMAND_LINK.equals(command)) {
                                linkCommand(response, dataSourceConfig, queryString);
                            } else if (COMMAND_HISTORICAL.equals(command)) {
                                writeback.historicalCommand(request, response, getWritebackDataSource(request, response), dataSourceConfigWB, queryString);
                            } else {
                                throw new BadCommandException("The command is not recognised.");
                            }
                        } else {
                            throw new BadDataSourceException("The datasource was not correctly initialised.");
                        }
                    } else {
                        throw new BadDataSourceException("The requested datasource does not exist.");
                    }
                }
            } else if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().isSlashDasPointsToDsn() && DAS_ONLY_URI_PATTERN.matcher(request.getRequestURI()).find()) {
                dsnCommand(request, response, queryString);
            } else {
                throw new BadCommandException("The command is not recognised.");
            }
        } catch (BadCommandException bce) {
            logger.error("BadCommandException thrown", bce);
            writeHeader(request, response, XDasStatus.STATUS_400_BAD_COMMAND, false);
            reportError(XDasStatus.STATUS_400_BAD_COMMAND, "Bad Command - Command not recognised as a valid DAS command.", request, response);
        } catch (BadDataSourceException bdse) {
            logger.error("BadDataSourceException thrown", bdse);
            writeHeader(request, response, XDasStatus.STATUS_401_BAD_DATA_SOURCE, false);
            reportError(XDasStatus.STATUS_401_BAD_DATA_SOURCE, "Bad Data Source", request, response);
        } catch (BadCommandArgumentsException bcae) {
            logger.error("BadCommandArgumentsException thrown", bcae);
            writeHeader(request, response, XDasStatus.STATUS_402_BAD_COMMAND_ARGUMENTS, false);
            reportError(XDasStatus.STATUS_402_BAD_COMMAND_ARGUMENTS, "Bad Command Arguments - Command not recognised as a valid DAS command.", request, response);
        } catch (BadReferenceObjectException broe) {
            logger.error("BadReferenceObjectException thrown", broe);
            writeHeader(request, response, XDasStatus.STATUS_403_BAD_REFERENCE_OBJECT, false);
            reportError(XDasStatus.STATUS_403_BAD_REFERENCE_OBJECT, "Unrecognised reference object: the requested segment is not available from this server.", request, response);
        } catch (BadStylesheetException bse) {
            logger.error("BadStylesheetException thrown:", bse);
            writeHeader(request, response, XDasStatus.STATUS_404_BAD_STYLESHEET, false);
            reportError(XDasStatus.STATUS_404_BAD_STYLESHEET, "Bad Stylesheet.", request, response);
        } catch (CoordinateErrorException cee) {
            logger.error("CoordinateErrorException thrown", cee);
            writeHeader(request, response, XDasStatus.STATUS_405_COORDINATE_ERROR, false);
            reportError(XDasStatus.STATUS_405_COORDINATE_ERROR, "Coordinate error - the requested coordinates are outside the scope of the requested segment.", request, response);
        } catch (XmlPullParserException xppe) {
            logger.error("XmlPullParserException thrown when attempting to ouput XML.", xppe);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "An error has occurred when attempting to output the DAS XML.", request, response);
        } catch (DataSourceException dse) {
            logger.error("DataSourceException thrown by a data source.", dse);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "The data source has thrown a 'DataSourceException' indicating a software error has occurred: " + dse.getMessage(), request, response);
        } catch (ConfigurationException ce) {
            logger.error("ConfigurationException thrown: This mydas installation was not correctly initialised.", ce);
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "This installation of MyDas is not correctly configured.", request, response);
        } catch (UnimplementedFeatureException efe) {
            logger.error("UnimplementedFeatureException thrown", efe);
            writeHeader(request, response, XDasStatus.STATUS_501_UNIMPLEMENTED_FEATURE, false);
            reportError(XDasStatus.STATUS_501_UNIMPLEMENTED_FEATURE, "Unimplemented feature: this DAS server cannot serve the request you have made.", request, response);
        }
    }

    private void doOpenid(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Object obj = request.getSession().getAttribute("writebackDoc");
        HTTPMethod m = null;
        if (obj == null) {
            obj = request.getSession().getAttribute("user_id");
            m = HTTPMethod.HTTP_DELETE;
            doDelete(request, response);
            return;
        }
        if (obj == null) {
            logger.error("OPENID request with no writeback document in session");
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            reportError(XDasStatus.STATUS_500_SERVER_ERROR, "OPENID request with no writeback document in session.", request, response);
        } else {
            m = ((WritebackDocument) obj).getMethod();
            if (m == HTTPMethod.HTTP_POST) doPost(request, response); else if (m == HTTPMethod.HTTP_PUT) doPut(request, response);
        }
    }

    private void reportError(XDasStatus dasStatus, String errorMessage, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Writer out = response.getWriter();
        out.write("<html><head><title>DAS Error</title></head><body><h2>MyDas Error Message</h2><h4>Request: <code>");
        out.write(request.getRequestURI());
        if (request.getQueryString() != null) {
            out.write('?');
            out.write(request.getQueryString());
        }
        out.write("</code></h4>");
        if (dasStatus != null) {
            out.write("<h4>");
            out.write(HEADER_KEY_X_DAS_STATUS);
            out.write(": ");
            out.write(dasStatus.toString());
            out.write("</h4>");
        }
        out.write("<h4>Error: <span style='color:red'>");
        out.write(errorMessage);
        out.write("</span></h4></body></html>");
        out.flush();
        out.close();
    }

    /**
     * Implements the dsn command.  Only reports dsns that have initialised successfully.
     * @param request to allow writing of the HTTP header
     * @param response to which the HTTP header and DASDSN XML are written
     * @param queryString to check no spurious arguments have been passed to the command
     * @throws XmlPullParserException in the event of an error being thrown when writing out the XML
     * @throws IOException in the event of an error being thrown when writing out the XML
     */
    private void dsnCommand(HttpServletRequest request, HttpServletResponse response, String queryString) throws XmlPullParserException, IOException {
        if (DATA_SOURCE_MANAGER.getServerConfiguration() == null) {
            writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
            logger.error("A request has been made to the das server, however initialisation failed - possibly the mydasserverconfig.xml file was not found.");
            return;
        }
        if (queryString == null || queryString.length() == 0) {
            List<String> dsns = DATA_SOURCE_MANAGER.getServerConfiguration().getDsnNames();
            if (dsns == null || dsns.size() == 0) {
                writeHeader(request, response, XDasStatus.STATUS_500_SERVER_ERROR, false);
                logger.error("The dsn command has been called, but no dsns have been initialised successfully.");
            } else {
                writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
                XmlSerializer serializer;
                serializer = PULL_PARSER_FACTORY.newSerializer();
                BufferedWriter out = null;
                try {
                    out = getResponseWriter(request, response);
                    serializer.setOutput(out);
                    serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
                    serializer.startDocument(null, false);
                    serializer.text("\n");
                    if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDsnXSLT() != null) {
                        serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDsnXSLT());
                        serializer.text("\n");
                    }
                    serializer.docdecl(" DASDSN SYSTEM \"http://www.biodas.org/dtd/dasdsn.dtd\"");
                    serializer.text("\n");
                    serializer.startTag(DAS_XML_NAMESPACE, "DASDSN");
                    for (String dsn : dsns) {
                        DataSourceConfiguration dsnConfig = DATA_SOURCE_MANAGER.getServerConfiguration().getDataSourceConfig(dsn);
                        serializer.startTag(DAS_XML_NAMESPACE, "DSN");
                        serializer.startTag(DAS_XML_NAMESPACE, "SOURCE");
                        serializer.attribute(DAS_XML_NAMESPACE, "id", dsnConfig.getId());
                        if (dsnConfig.getVersion() != null && dsnConfig.getVersion().length() > 0) {
                            serializer.attribute(DAS_XML_NAMESPACE, "version", dsnConfig.getVersion());
                        }
                        if (dsnConfig.getName() != null && dsnConfig.getName().length() > 0) {
                            serializer.text(dsnConfig.getName());
                        } else {
                            serializer.text(dsnConfig.getId());
                        }
                        serializer.endTag(DAS_XML_NAMESPACE, "SOURCE");
                        serializer.startTag(DAS_XML_NAMESPACE, "MAPMASTER");
                        serializer.text(dsnConfig.getMapmaster());
                        serializer.endTag(DAS_XML_NAMESPACE, "MAPMASTER");
                        if (dsnConfig.getDescription() != null && dsnConfig.getDescription().length() > 0) {
                            serializer.startTag(DAS_XML_NAMESPACE, "DESCRIPTION");
                            serializer.text(dsnConfig.getDescription());
                            serializer.endTag(DAS_XML_NAMESPACE, "DESCRIPTION");
                        }
                        serializer.endTag(DAS_XML_NAMESPACE, "DSN");
                    }
                    serializer.endTag(DAS_XML_NAMESPACE, "DASDSN");
                    serializer.flush();
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            }
        } else {
            writeHeader(request, response, XDasStatus.STATUS_402_BAD_COMMAND_ARGUMENTS, true);
        }
    }

    private void dnaCommand(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, String queryString) throws XmlPullParserException, IOException, DataSourceException, UnimplementedFeatureException, BadReferenceObjectException, BadCommandArgumentsException, CoordinateErrorException {
        if (dsnConfig.isDnaCommandEnabled()) {
            if (dsnConfig.getDataSource() instanceof ReferenceDataSource) {
                Collection<SequenceReporter> sequences = getSequences(dsnConfig, queryString);
                writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
                XmlSerializer serializer;
                serializer = PULL_PARSER_FACTORY.newSerializer();
                BufferedWriter out = null;
                try {
                    out = getResponseWriter(request, response);
                    serializer.setOutput(out);
                    serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
                    serializer.startDocument(null, false);
                    serializer.text("\n");
                    if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDnaXSLT() != null) {
                        serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDnaXSLT());
                        serializer.text("\n");
                    }
                    serializer.docdecl(" DASDNA SYSTEM \"http://www.biodas.org/dtd/dasdna.dtd\"");
                    serializer.text("\n");
                    serializer.startTag(DAS_XML_NAMESPACE, "DASDNA");
                    for (SequenceReporter sequenceReporter : sequences) {
                        serializer.startTag(DAS_XML_NAMESPACE, "SEQUENCE");
                        serializer.attribute(DAS_XML_NAMESPACE, "id", sequenceReporter.getSegmentName());
                        serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(sequenceReporter.getStart()));
                        serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(sequenceReporter.getStop()));
                        serializer.attribute(DAS_XML_NAMESPACE, "version", sequenceReporter.getSequenceVersion());
                        serializer.startTag(DAS_XML_NAMESPACE, "DNA");
                        serializer.attribute(DAS_XML_NAMESPACE, "length", Integer.toString(sequenceReporter.getSequenceString().length()));
                        serializer.text(sequenceReporter.getSequenceString());
                        serializer.endTag(DAS_XML_NAMESPACE, "DNA");
                        serializer.endTag(DAS_XML_NAMESPACE, "SEQUENCE");
                    }
                    serializer.endTag(DAS_XML_NAMESPACE, "DASDNA");
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            } else {
                throw new UnimplementedFeatureException("The dna command has been called on an annotation server.");
            }
        } else {
            throw new UnimplementedFeatureException("The dna command has been disabled for this data source.");
        }
    }

    private void typesCommand(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, String queryString) throws BadCommandArgumentsException, BadReferenceObjectException, DataSourceException, CoordinateErrorException, IOException, XmlPullParserException {
        List<SegmentQuery> requestedSegments = new ArrayList<SegmentQuery>();
        List<String> typeFilter = new ArrayList<String>();
        if (queryString != null && queryString.length() > 0) {
            String[] queryParts = queryString.split(";");
            for (String queryPart : queryParts) {
                boolean queryPartParsable = false;
                Matcher segmentRangeMatcher = SEGMENT_RANGE_PATTERN.matcher(queryPart);
                if (segmentRangeMatcher.find()) {
                    requestedSegments.add(new SegmentQuery(segmentRangeMatcher));
                    queryPartParsable = true;
                } else {
                    String[] queryPartKeysValues = queryPart.split("=");
                    if (queryPartKeysValues.length != 2) {
                        throw new BadCommandArgumentsException("Bad command arguments to the features command: " + queryString);
                    }
                    String key = queryPartKeysValues[0];
                    String value = queryPartKeysValues[1];
                    if ("type".equals(key)) {
                        typeFilter.add(value);
                        queryPartParsable = true;
                    }
                }
                if (!queryPartParsable) {
                    throw new BadCommandArgumentsException("Bad command arguments to the features command: " + queryString);
                }
            }
        }
        if (requestedSegments.size() == 0) {
            typesCommandAllTypes(request, response, dsnConfig, typeFilter);
        } else {
            typesCommandSpecificSegments(request, response, dsnConfig, requestedSegments, typeFilter);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<DasType> getAllTypes(DataSourceConfiguration dsnConfig) throws DataSourceException {
        Collection<DasType> allTypes;
        String cacheKey = dsnConfig.getId() + "_ALL_TYPES";
        try {
            allTypes = (Collection<DasType>) CACHE_MANAGER.getFromCache(cacheKey);
            if (logger.isDebugEnabled()) {
                logger.debug("ALL TYPES RETRIEVED FROM CACHE.");
            }
        } catch (NeedsRefreshException nre) {
            try {
                allTypes = dsnConfig.getDataSource().getTypes();
                CACHE_MANAGER.putInCache(cacheKey, allTypes, dsnConfig.getCacheGroup());
                if (logger.isDebugEnabled()) {
                    logger.debug("ALL TYPES RETRIEVED FROM DSN (Not in Cache).");
                }
            } catch (DataSourceException dse) {
                CACHE_MANAGER.cancelUpdate(cacheKey);
                throw dse;
            }
        }
        return (allTypes == null) ? Collections.EMPTY_LIST : allTypes;
    }

    private void typesCommandAllTypes(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, List<String> typeFilter) throws DataSourceException, XmlPullParserException, IOException {
        Map<DasType, Integer> allTypesReport;
        Collection<DasType> allTypes = getAllTypes(dsnConfig);
        allTypesReport = new HashMap<DasType, Integer>(allTypes.size());
        for (DasType type : allTypes) {
            if (type != null) {
                if (typeFilter.size() == 0 || typeFilter.contains(type.getId())) {
                    Integer typeCount;
                    StringBuffer keyBuf = new StringBuffer(dsnConfig.getId());
                    keyBuf.append("_TYPECOUNT_ID_").append(type.getId()).append("_CAT_").append((type.getCategory() == null) ? "null" : type.getCategory()).append("_METHOD_").append((type.getMethod() == null) ? "null" : type.getMethod());
                    String cacheKey = keyBuf.toString();
                    try {
                        typeCount = (Integer) CACHE_MANAGER.getFromCache(cacheKey);
                    } catch (NeedsRefreshException nre) {
                        try {
                            typeCount = dsnConfig.getDataSource().getTotalCountForType(type);
                            CACHE_MANAGER.putInCache(cacheKey, typeCount, dsnConfig.getCacheGroup());
                        } catch (DataSourceException dse) {
                            CACHE_MANAGER.cancelUpdate(cacheKey);
                            throw dse;
                        }
                    }
                    allTypesReport.put(type, typeCount);
                }
            }
        }
        writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
        XmlSerializer serializer;
        serializer = PULL_PARSER_FACTORY.newSerializer();
        BufferedWriter out = null;
        try {
            out = getResponseWriter(request, response);
            serializer.setOutput(out);
            serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
            serializer.startDocument(null, false);
            serializer.text("\n");
            if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getTypesXSLT() != null) {
                serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getTypesXSLT());
                serializer.text("\n");
            }
            serializer.docdecl(" DASTYPES SYSTEM \"http://www.biodas.org/dtd/dastypes.dtd\"");
            serializer.text("\n");
            serializer.startTag(DAS_XML_NAMESPACE, "DASTYPES");
            serializer.startTag(DAS_XML_NAMESPACE, "GFF");
            serializer.attribute(DAS_XML_NAMESPACE, "version", "1.0");
            serializer.attribute(DAS_XML_NAMESPACE, "href", this.buildRequestHref(request));
            serializer.startTag(DAS_XML_NAMESPACE, "SEGMENT");
            serializer.attribute(DAS_XML_NAMESPACE, "version", dsnConfig.getVersion());
            serializer.attribute(DAS_XML_NAMESPACE, "label", "Complete datasource summary");
            for (DasType type : allTypesReport.keySet()) {
                serializer.startTag(DAS_XML_NAMESPACE, "TYPE");
                serializer.attribute(DAS_XML_NAMESPACE, "id", type.getId());
                if (type.getMethod() != null && type.getMethod().length() > 0) {
                    serializer.attribute(DAS_XML_NAMESPACE, "method", type.getMethod());
                }
                if (type.getCategory() != null && type.getCategory().length() > 0) {
                    serializer.attribute(DAS_XML_NAMESPACE, "category", type.getCategory());
                }
                if (allTypesReport.get(type) != null) {
                    serializer.text(Integer.toString(allTypesReport.get(type)));
                }
                serializer.endTag(DAS_XML_NAMESPACE, "TYPE");
            }
            serializer.endTag(DAS_XML_NAMESPACE, "SEGMENT");
            serializer.endTag(DAS_XML_NAMESPACE, "GFF");
            serializer.endTag(DAS_XML_NAMESPACE, "DASTYPES");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void typesCommandSpecificSegments(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, List<SegmentQuery> requestedSegments, List<String> typeFilter) throws DataSourceException, BadReferenceObjectException, XmlPullParserException, IOException, CoordinateErrorException {
        Map<FoundFeaturesReporter, Map<DasType, Integer>> typesReport = new HashMap<FoundFeaturesReporter, Map<DasType, Integer>>(requestedSegments.size());
        Collection<SegmentReporter> segmentReporters = this.getFeatureCollection(dsnConfig, requestedSegments, false);
        for (SegmentReporter uncastReporter : segmentReporters) {
            if (uncastReporter instanceof FoundFeaturesReporter) {
                FoundFeaturesReporter segmentReporter = (FoundFeaturesReporter) uncastReporter;
                Map<DasType, Integer> segmentTypes = new HashMap<DasType, Integer>();
                typesReport.put(segmentReporter, segmentTypes);
                if (dsnConfig.isIncludeTypesWithZeroCount()) {
                    Collection<DasType> allTypes = getAllTypes(dsnConfig);
                    for (DasType type : allTypes) {
                        if (type != null && (typeFilter.size() == 0 || typeFilter.contains(type.getId()))) segmentTypes.put(type, 0);
                    }
                }
                for (DasFeature feature : segmentReporter.getFeatures(dsnConfig.isFeaturesStrictlyEnclosed())) {
                    if (typeFilter.size() == 0 || typeFilter.contains(feature.getTypeId())) {
                        DasType featureType = new DasType(feature.getTypeId(), feature.getTypeCategory(), feature.getMethodId());
                        if (segmentTypes.keySet().contains(featureType)) {
                            segmentTypes.put(featureType, segmentTypes.get(featureType) + 1);
                        } else {
                            segmentTypes.put(featureType, 1);
                        }
                    }
                }
            }
        }
        writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
        XmlSerializer serializer;
        serializer = PULL_PARSER_FACTORY.newSerializer();
        BufferedWriter out = null;
        try {
            out = getResponseWriter(request, response);
            serializer.setOutput(out);
            serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
            serializer.startDocument(null, false);
            serializer.text("\n");
            if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getTypesXSLT() != null) {
                serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getTypesXSLT());
                serializer.text("\n");
            }
            serializer.docdecl(" DASTYPES SYSTEM \"http://www.biodas.org/dtd/dastypes.dtd\"");
            serializer.text("\n");
            serializer.startTag(DAS_XML_NAMESPACE, "DASTYPES");
            serializer.startTag(DAS_XML_NAMESPACE, "GFF");
            serializer.attribute(DAS_XML_NAMESPACE, "version", "1.0");
            serializer.attribute(DAS_XML_NAMESPACE, "href", this.buildRequestHref(request));
            for (FoundFeaturesReporter featureReporter : typesReport.keySet()) {
                serializer.startTag(DAS_XML_NAMESPACE, "SEGMENT");
                serializer.attribute(DAS_XML_NAMESPACE, "id", featureReporter.getSegmentId());
                serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(featureReporter.getStart()));
                serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(featureReporter.getStop()));
                if (featureReporter.getType() != null && featureReporter.getType().length() > 0) {
                    serializer.attribute(DAS_XML_NAMESPACE, "type", featureReporter.getType());
                }
                serializer.attribute(DAS_XML_NAMESPACE, "version", featureReporter.getVersion());
                if (featureReporter.getSegmentLabel() != null && featureReporter.getSegmentLabel().length() > 0) {
                    serializer.attribute(DAS_XML_NAMESPACE, "label", featureReporter.getSegmentLabel());
                }
                Map<DasType, Integer> typeMap = typesReport.get(featureReporter);
                for (DasType type : typeMap.keySet()) {
                    Integer count = typeMap.get(type);
                    serializer.startTag(DAS_XML_NAMESPACE, "TYPE");
                    serializer.attribute(DAS_XML_NAMESPACE, "id", type.getId());
                    if (type.getMethod() != null && type.getMethod().length() > 0) {
                        serializer.attribute(DAS_XML_NAMESPACE, "method", type.getMethod());
                    }
                    if (type.getCategory() != null && type.getCategory().length() > 0) {
                        serializer.attribute(DAS_XML_NAMESPACE, "category", type.getCategory());
                    }
                    if (count != null) {
                        serializer.text(Integer.toString(count));
                    }
                    serializer.endTag(DAS_XML_NAMESPACE, "TYPE");
                }
                serializer.endTag(DAS_XML_NAMESPACE, "SEGMENT");
            }
            serializer.endTag(DAS_XML_NAMESPACE, "GFF");
            serializer.endTag(DAS_XML_NAMESPACE, "DASTYPES");
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void stylesheetCommand(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, String queryString) throws BadCommandArgumentsException, IOException, BadStylesheetException {
        if (queryString != null && queryString.trim().length() > 0) {
            throw new BadCommandArgumentsException("Arguments have been passed to the stylesheet command, which does not expect any.");
        }
        String stylesheetFileName;
        if (dsnConfig.getStyleSheet() != null && dsnConfig.getStyleSheet().trim().length() > 0) {
            stylesheetFileName = dsnConfig.getStyleSheet().trim();
        } else if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDefaultStyleSheet() != null && DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDefaultStyleSheet().trim().length() > 0) {
            stylesheetFileName = DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getDefaultStyleSheet().trim();
        } else {
            throw new BadStylesheetException("This data source has not defined a stylesheet.");
        }
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getServletContext().getResourceAsStream(RESOURCE_FOLDER + stylesheetFileName)));
            if (reader.ready()) {
                writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
                writer = getResponseWriter(request, response);
                while (reader.ready()) {
                    writer.write(reader.readLine());
                }
            } else {
                throw new BadStylesheetException("A problem has occurred reading in the stylesheet from the open stream");
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Implements the link command.  This is done using a simple mechanism - the request is parsed and checked for
     * correctness, then the 'field' and 'id' are passed to the DSN that should return a well formed URL.  This method
     * then redirects the browser to the URL specified.  This mechanism gets around any problems with odd MIME types
     * in the results page.
     * @param response which is redirected to the URL specified (unless there is a problem, in which case the
     * appropriate X-DAS-STATUS will be sent instead)
     * @param dataSourceConfig holding configuration of the dsn and the data source object itself.
     * @param queryString from which the 'field' and 'id' parameters are parsed.
     * @throws IOException during handling of the response
     * @throws BadCommandArgumentsException if the arguments to the link command are not as specified in the
     * DAS 1.53 specification
     * @throws DataSourceException to handle problems from the DSN.
     * @throws UnimplementedFeatureException if the DSN reports that it does not implement this command.
     */
    private void linkCommand(HttpServletResponse response, DataSourceConfiguration dataSourceConfig, String queryString) throws IOException, BadCommandArgumentsException, DataSourceException, UnimplementedFeatureException {
        if (queryString == null || queryString.length() == 0) {
            throw new BadCommandArgumentsException("The link command has been called with no arguments.");
        }
        String[] queryParts = queryString.split(";");
        if (queryParts.length != 2) {
            throw new BadCommandArgumentsException("The wrong number of arguments have been passed to the link command.");
        }
        String field = null;
        String id = null;
        for (String keyValuePair : queryParts) {
            String[] queryPartKeysValues = keyValuePair.split("=");
            if (queryPartKeysValues.length != 2) {
                throw new BadCommandArgumentsException("keys and values cannot be extracted from the arguments to the link command");
            }
            if ("field".equals(queryPartKeysValues[0])) {
                field = queryPartKeysValues[1];
            } else if ("id".equals(queryPartKeysValues[0])) {
                id = queryPartKeysValues[1];
            } else {
                throw new BadCommandArgumentsException("unknown key to one of the command arguments to the link command");
            }
        }
        if (field == null || !VALID_LINK_COMMAND_FIELDS.contains(field) || id == null) {
            throw new BadCommandArgumentsException("The link command must be passed a valid field and id argument.");
        }
        URL url;
        StringBuffer cacheKeyBuffer = new StringBuffer(dataSourceConfig.getId());
        cacheKeyBuffer.append("_LINK_").append(field).append('_').append(id);
        String cacheKey = cacheKeyBuffer.toString();
        try {
            url = (URL) CACHE_MANAGER.getFromCache(cacheKey);
            if (logger.isDebugEnabled()) {
                logger.debug("LINK RETRIEVED FROM CACHE: " + url.toString());
            }
        } catch (NeedsRefreshException e) {
            try {
                url = dataSourceConfig.getDataSource().getLinkURL(field, id);
                CACHE_MANAGER.putInCache(cacheKey, url, dataSourceConfig.getCacheGroup());
            } catch (UnimplementedFeatureException ufe) {
                CACHE_MANAGER.cancelUpdate(cacheKey);
                throw ufe;
            } catch (DataSourceException dse) {
                CACHE_MANAGER.cancelUpdate(cacheKey);
                throw dse;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("LINK RETRIEVED FROM DSN (NOT CACHED): " + url.toString());
            }
        }
        response.sendRedirect(response.encodeRedirectURL(url.toString()));
    }

    /**
     * This method handles the complete features command, including all variants as specified in DAS 1.53.
     *
     * @param request to allow the writing of the http header
     * @param response to which the http header and the XML are written.
     * @param dsnConfig holding configuration of the dsn and the data source object itself.
     * @param queryString from which the requested segments and other allowed parameters are parsed.
     * @throws XmlPullParserException in the event of a problem with writing out the DASFEATURE XML file.
     * @throws IOException during writing of the XML
     * @throws DataSourceException to capture any error returned from the data source.
     * @throws BadCommandArgumentsException if the arguments to the feature command are not as specified in the
     * DAS 1.53 specification
     * @throws UnimplementedFeatureException if the dsn reports that it cannot handle an aspect of the feature
     * command (although all dsns are required to implement at least the basic feature command).
     * @throws BadReferenceObjectException will not be thrown, but a helper method used by this method
     * can throw this exception under some circumstances (but not when called by the featureCommand method!)
     * @throws CoordinateErrorException will not be thrown, but a helper method used by this method
     * can throw this exception under some circumstances (but not when called by the featureCommand method!)
     */
    @SuppressWarnings("unchecked")
    private void featuresCommand(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, String queryString) throws XmlPullParserException, IOException, DataSourceException, BadCommandArgumentsException, UnimplementedFeatureException, BadReferenceObjectException, CoordinateErrorException {
        if (queryString == null || queryString.length() == 0) {
            throw new BadCommandArgumentsException("Expecting at least one reference in the query string, but found nothing.");
        }
        List<SegmentQuery> requestedSegments = new ArrayList<SegmentQuery>();
        String[] queryParts = queryString.split(";");
        DasFeatureRequestFilter filter = new DasFeatureRequestFilter();
        boolean categorize = true;
        for (String queryPart : queryParts) {
            boolean queryPartParsable = false;
            Matcher segmentRangeMatcher = SEGMENT_RANGE_PATTERN.matcher(queryPart);
            if (segmentRangeMatcher.find()) {
                requestedSegments.add(new SegmentQuery(segmentRangeMatcher));
                queryPartParsable = true;
            } else {
                String[] queryPartKeysValues = queryPart.split("=");
                if (queryPartKeysValues.length != 2) {
                    throw new BadCommandArgumentsException("Bad command arguments to the features command: " + queryString);
                }
                String key = queryPartKeysValues[0];
                String value = queryPartKeysValues[1];
                if ("type".equals(key)) {
                    filter.addTypeId(value);
                    queryPartParsable = true;
                } else if ("category".equals(key)) {
                    filter.addCategoryId(value);
                    queryPartParsable = true;
                } else if ("categorize".equals(key)) {
                    if ("no".equals(value)) {
                        categorize = false;
                    }
                    queryPartParsable = true;
                } else if ("feature_id".equals(key)) {
                    filter.addFeatureId(value);
                    queryPartParsable = true;
                } else if ("group_id".equals(key)) {
                    filter.addGroupId(value);
                    queryPartParsable = true;
                }
            }
            if (!queryPartParsable) {
                throw new BadCommandArgumentsException("Bad command arguments to the features command: " + queryString);
            }
        }
        Collection<SegmentReporter> segmentReporterCollections;
        if (requestedSegments.size() > 0) {
            segmentReporterCollections = getFeatureCollection(dsnConfig, requestedSegments, true);
        } else {
            if (filter.containsFeatureIds() || filter.containsGroupIds()) {
                Collection<DasAnnotatedSegment> annotatedSegments = dsnConfig.getDataSource().getFeatures(filter.getFeatureIds(), filter.getGroupIds());
                if (annotatedSegments != null) {
                    segmentReporterCollections = new ArrayList<SegmentReporter>(annotatedSegments.size());
                    for (DasAnnotatedSegment segment : annotatedSegments) {
                        segmentReporterCollections.add(new FoundFeaturesReporter(segment));
                    }
                } else {
                    segmentReporterCollections = Collections.EMPTY_LIST;
                }
            } else {
                throw new BadCommandArgumentsException("Bad command arguments to the features command: " + queryString);
            }
        }
        writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
        XmlSerializer serializer;
        serializer = PULL_PARSER_FACTORY.newSerializer();
        BufferedWriter out = null;
        try {
            boolean referenceSource = dsnConfig.getDataSource() instanceof ReferenceDataSource;
            out = getResponseWriter(request, response);
            serializer.setOutput(out);
            serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
            serializer.startDocument(null, false);
            serializer.text("\n");
            if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getFeaturesXSLT() != null) {
                serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getFeaturesXSLT());
                serializer.text("\n");
            }
            serializer.docdecl(" DASGFF SYSTEM \"http://www.biodas.org/dtd/dasgff.dtd\"");
            serializer.text("\n");
            serializer.startTag(DAS_XML_NAMESPACE, "DASGFF");
            serializer.startTag(DAS_XML_NAMESPACE, "GFF");
            serializer.attribute(DAS_XML_NAMESPACE, "version", "1.0");
            serializer.attribute(DAS_XML_NAMESPACE, "href", buildRequestHref(request));
            for (SegmentReporter segmentReporter : segmentReporterCollections) {
                if (segmentReporter instanceof UnknownSegmentReporter) {
                    serializer.startTag(DAS_XML_NAMESPACE, (referenceSource) ? "ERRORSEGMENT" : "UNKNOWNSEGMENT");
                    serializer.attribute(DAS_XML_NAMESPACE, "id", segmentReporter.getSegmentId());
                    if (segmentReporter.getStart() != null) {
                        serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(segmentReporter.getStart()));
                    }
                    if (segmentReporter.getStop() != null) {
                        serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(segmentReporter.getStop()));
                    }
                    serializer.endTag(DAS_XML_NAMESPACE, (referenceSource) ? "ERRORSEGMENT" : "UNKNOWNSEGMENT");
                } else {
                    FoundFeaturesReporter foundFeaturesReporter = (FoundFeaturesReporter) segmentReporter;
                    serializer.startTag(DAS_XML_NAMESPACE, "SEGMENT");
                    serializer.attribute(DAS_XML_NAMESPACE, "id", foundFeaturesReporter.getSegmentId());
                    serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(foundFeaturesReporter.getStart()));
                    serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(foundFeaturesReporter.getStop()));
                    if (foundFeaturesReporter.getType() != null && foundFeaturesReporter.getType().length() > 0) {
                        serializer.attribute(DAS_XML_NAMESPACE, "type", foundFeaturesReporter.getType());
                    }
                    serializer.attribute(DAS_XML_NAMESPACE, "version", foundFeaturesReporter.getVersion());
                    if (foundFeaturesReporter.getSegmentLabel() != null && foundFeaturesReporter.getSegmentLabel().length() > 0) {
                        serializer.attribute(DAS_XML_NAMESPACE, "label", foundFeaturesReporter.getSegmentLabel());
                    }
                    for (DasFeature feature : foundFeaturesReporter.getFeatures(dsnConfig.isFeaturesStrictlyEnclosed())) {
                        if (filter.featurePasses(feature)) {
                            serializer.startTag(DAS_XML_NAMESPACE, "FEATURE");
                            serializer.attribute(DAS_XML_NAMESPACE, "id", feature.getFeatureId());
                            if (feature.getFeatureLabel() != null && feature.getFeatureLabel().length() > 0) {
                                serializer.attribute(DAS_XML_NAMESPACE, "label", feature.getFeatureLabel());
                            } else if (dsnConfig.isUseFeatureIdForFeatureLabel()) {
                                serializer.attribute(DAS_XML_NAMESPACE, "label", feature.getFeatureId());
                            }
                            serializer.startTag(DAS_XML_NAMESPACE, "TYPE");
                            serializer.attribute(DAS_XML_NAMESPACE, "id", feature.getTypeId());
                            if (feature instanceof DasComponentFeature) {
                                DasComponentFeature refFeature = (DasComponentFeature) feature;
                                serializer.attribute(DAS_XML_NAMESPACE, "reference", "yes");
                                serializer.attribute(DAS_XML_NAMESPACE, "superparts", (refFeature.hasSuperParts()) ? "yes" : "no");
                                serializer.attribute(DAS_XML_NAMESPACE, "subparts", (refFeature.hasSubParts()) ? "yes" : "no");
                            }
                            if (categorize) {
                                if (feature.getTypeCategory() != null && feature.getTypeCategory().length() > 0) {
                                    serializer.attribute(DAS_XML_NAMESPACE, "category", feature.getTypeCategory());
                                } else {
                                    serializer.attribute(DAS_XML_NAMESPACE, "category", feature.getTypeId());
                                }
                            }
                            if (feature.getTypeLabel() != null && feature.getTypeLabel().length() > 0) {
                                serializer.text(feature.getTypeLabel());
                            }
                            serializer.endTag(DAS_XML_NAMESPACE, "TYPE");
                            serializer.startTag(DAS_XML_NAMESPACE, "METHOD");
                            if (feature.getMethodId() != null && feature.getMethodId().length() > 0) {
                                serializer.attribute(DAS_XML_NAMESPACE, "id", feature.getMethodId());
                            }
                            if (feature.getMethodLabel() != null && feature.getMethodLabel().length() > 0) {
                                serializer.text(feature.getMethodLabel());
                            }
                            serializer.endTag(DAS_XML_NAMESPACE, "METHOD");
                            serializer.startTag(DAS_XML_NAMESPACE, "START");
                            serializer.text(Integer.toString(feature.getStartCoordinate()));
                            serializer.endTag(DAS_XML_NAMESPACE, "START");
                            serializer.startTag(DAS_XML_NAMESPACE, "END");
                            serializer.text(Integer.toString(feature.getStopCoordinate()));
                            serializer.endTag(DAS_XML_NAMESPACE, "END");
                            serializer.startTag(DAS_XML_NAMESPACE, "SCORE");
                            serializer.text((feature.getScore() == null) ? "-" : Double.toString(feature.getScore()));
                            serializer.endTag(DAS_XML_NAMESPACE, "SCORE");
                            serializer.startTag(DAS_XML_NAMESPACE, "ORIENTATION");
                            serializer.text(feature.getOrientation());
                            serializer.endTag(DAS_XML_NAMESPACE, "ORIENTATION");
                            serializer.startTag(DAS_XML_NAMESPACE, "PHASE");
                            serializer.text(feature.getPhase());
                            serializer.endTag(DAS_XML_NAMESPACE, "PHASE");
                            serializeFeatureNoteElements(feature.getNotes(), serializer);
                            serializeFeatureLinkElements(feature.getLinks(), serializer);
                            serializeFeatureTargetElements(feature.getTargets(), serializer);
                            if (feature.getGroups() != null) {
                                for (DasGroup group : feature.getGroups()) {
                                    serializer.startTag(DAS_XML_NAMESPACE, "GROUP");
                                    serializer.attribute(DAS_XML_NAMESPACE, "id", group.getGroupId());
                                    if (group.getGroupLabel() != null && group.getGroupLabel().length() > 0) {
                                        serializer.attribute(DAS_XML_NAMESPACE, "label", group.getGroupLabel());
                                    }
                                    if (group.getGroupType() != null && group.getGroupType().length() > 0) {
                                        serializer.attribute(DAS_XML_NAMESPACE, "type", group.getGroupType());
                                    }
                                    serializeFeatureNoteElements(group.getNotes(), serializer);
                                    serializeFeatureLinkElements(group.getLinks(), serializer);
                                    serializeFeatureTargetElements(group.getTargets(), serializer);
                                    serializer.endTag(DAS_XML_NAMESPACE, "GROUP");
                                }
                            }
                            serializer.endTag(DAS_XML_NAMESPACE, "FEATURE");
                        }
                    }
                    serializer.endTag(DAS_XML_NAMESPACE, "SEGMENT");
                }
            }
            serializer.endTag(DAS_XML_NAMESPACE, "GFF");
            serializer.endTag(DAS_XML_NAMESPACE, "DASGFF");
            serializer.flush();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Helper method - serializes out the NOTE element which is used in two places in the DASFEATURE XML file.
     * (Hence factored out).
     * @param notes being a Collection of Strings, each of which is a note to be serialized.
     * @param serializer to write out the XML
     * @throws IOException during writing of the XML.
     */
    void serializeFeatureNoteElements(Collection<String> notes, XmlSerializer serializer) throws IOException {
        if (notes != null) {
            for (String note : notes) {
                serializer.startTag(DAS_XML_NAMESPACE, "NOTE");
                serializer.text(note);
                serializer.endTag(DAS_XML_NAMESPACE, "NOTE");
            }
        }
    }

    /**
     * Helper method - serializes out the LINK element which is used in two places in the DASFEATURE XML file.
     * (Hence factored out).
     * @param links being a Map of URL to String, with the String being an optional human-readable form of the URL.
     * @param serializer to write out the XML
     * @throws IOException during writing of the XML.
     */
    void serializeFeatureLinkElements(Map<URL, String> links, XmlSerializer serializer) throws IOException {
        if (links != null) {
            for (URL url : links.keySet()) {
                if (url != null) {
                    serializer.startTag(DAS_XML_NAMESPACE, "LINK");
                    serializer.attribute(DAS_XML_NAMESPACE, "href", url.toString());
                    String linkText = links.get(url);
                    if (linkText != null && linkText.length() > 0) {
                        serializer.text(linkText);
                    }
                    serializer.endTag(DAS_XML_NAMESPACE, "LINK");
                }
            }
        }
    }

    /**
     * Helper method - serializes out the TARGET element which is used in two places in the DASFEATURE XML file.
     * (Hence factored out).
     * @param targets being a Collection of DasTarget objects, encapsulating the details of the targets.
     * @param serializer to write out the XML
     * @throws IOException during writing of the XML.
     */
    void serializeFeatureTargetElements(Collection<DasTarget> targets, XmlSerializer serializer) throws IOException {
        if (targets != null) {
            for (DasTarget target : targets) {
                serializer.startTag(DAS_XML_NAMESPACE, "TARGET");
                serializer.attribute(DAS_XML_NAMESPACE, "id", target.getTargetId());
                serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(target.getStartCoordinate()));
                serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(target.getStopCoordinate()));
                if (target.getTargetName() != null && target.getTargetName().length() > 0) {
                    serializer.text(target.getTargetName());
                }
                serializer.endTag(DAS_XML_NAMESPACE, "TARGET");
            }
        }
    }

    /**
     * Implements the entry_points command.
     * @param request to allow the writing of the http header
     * @param response to which the http header and the XML are written.
     * @param dsnConfig holding configuration of the dsn and the data source object itself.
     * @param queryString to be checked for bad arguments (there should be no arguments to this command)
     * @throws XmlPullParserException in the event of a problem with writing out the DASENTRYPOINT XML file.
     * @throws IOException during writing of the XML
     * @throws DataSourceException to capture any error returned from the data source.
     * @throws UnimplementedFeatureException if the dsn reports that it cannot return entry_points.
     * @throws BadCommandArgumentsException in the event that spurious arguments have been passed in the queryString.
     */
    private void entryPointsCommand(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, String queryString) throws XmlPullParserException, IOException, DataSourceException, UnimplementedFeatureException, BadCommandArgumentsException {
        if (queryString != null && queryString.trim().length() > 0) {
            throw new BadCommandArgumentsException("Unexpected arguments have been passed to the entry_points command.");
        }
        if (dsnConfig.getDataSource() instanceof ReferenceDataSource) {
            ReferenceDataSource refDsn = (ReferenceDataSource) dsnConfig.getDataSource();
            Collection<DasEntryPoint> entryPoints = refDsn.getEntryPoints();
            if (refDsn.getEntryPointVersion() == null) {
                throw new DataSourceException("The dsn " + dsnConfig.getId() + "is returning null for the entry point version, which is invalid.");
            }
            writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
            XmlSerializer serializer;
            serializer = PULL_PARSER_FACTORY.newSerializer();
            BufferedWriter out = null;
            try {
                out = getResponseWriter(request, response);
                serializer.setOutput(out);
                serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
                serializer.startDocument(null, false);
                serializer.text("\n");
                if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getEntryPointsXSLT() != null) {
                    serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getEntryPointsXSLT());
                    serializer.text("\n");
                }
                serializer.docdecl(" DASEP SYSTEM \"http://www.biodas.org/dtd/dasep.dtd\"");
                serializer.text("\n");
                serializer.startTag(DAS_XML_NAMESPACE, "DASEP");
                serializer.startTag(DAS_XML_NAMESPACE, "ENTRY_POINTS");
                serializer.attribute(DAS_XML_NAMESPACE, "href", buildRequestHref(request));
                serializer.attribute(DAS_XML_NAMESPACE, "version", refDsn.getEntryPointVersion());
                for (DasEntryPoint entryPoint : entryPoints) {
                    if (entryPoint != null) {
                        serializer.startTag(DAS_XML_NAMESPACE, "SEGMENT");
                        serializer.attribute(DAS_XML_NAMESPACE, "id", entryPoint.getSegmentId());
                        serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(entryPoint.getStartCoordinate()));
                        serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(entryPoint.getStopCoordinate()));
                        if (entryPoint.getType() != null && entryPoint.getType().length() > 0) {
                            serializer.attribute(DAS_XML_NAMESPACE, "type", entryPoint.getType());
                        }
                        serializer.attribute(DAS_XML_NAMESPACE, "orientation", entryPoint.getOrientation().toString());
                        if (entryPoint.hasSubparts()) {
                            serializer.attribute(DAS_XML_NAMESPACE, "subparts", "yes");
                        }
                        if (entryPoint.getDescription() != null && entryPoint.getDescription().length() > 0) {
                            serializer.text(entryPoint.getDescription());
                        }
                        serializer.endTag(DAS_XML_NAMESPACE, "SEGMENT");
                    }
                }
                serializer.endTag(DAS_XML_NAMESPACE, "ENTRY_POINTS");
                serializer.endTag(DAS_XML_NAMESPACE, "DASEP");
                serializer.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } else {
            throw new UnimplementedFeatureException("An attempt to request entry_point information from an annotation server has been detected.");
        }
    }

    /**
     * Implements the sequence command.  Delegates to the getSequences method to return the requested sequences.
     * @param request to allow the writing of the http header
     * @param response to which the http header and the XML are written.
     * @param dsnConfig holding configuration of the dsn and the data source object itself.
     * @param queryString from which the requested segments are parsed.
     * @throws XmlPullParserException in the event of a problem with writing out the DASSEQUENCE XML file.
     * @throws IOException during writing of the XML
     * @throws DataSourceException to capture any error returned from the data source.
     * @throws UnimplementedFeatureException if the dsn reports that it cannot return sequence.
     * @throws BadReferenceObjectException in the event that the segment id is not known to the dsn
     * @throws BadCommandArgumentsException if the arguments to the sequence command are not as specified in the
     * DAS 1.53 specification
     * @throws CoordinateErrorException if the requested coordinates are outside those of the segment id requested.
     */
    private void sequenceCommand(HttpServletRequest request, HttpServletResponse response, DataSourceConfiguration dsnConfig, String queryString) throws XmlPullParserException, IOException, DataSourceException, UnimplementedFeatureException, BadReferenceObjectException, BadCommandArgumentsException, CoordinateErrorException {
        if (dsnConfig.getDataSource() instanceof ReferenceDataSource) {
            Collection<SequenceReporter> sequences = getSequences(dsnConfig, queryString);
            writeHeader(request, response, XDasStatus.STATUS_200_OK, true);
            XmlSerializer serializer;
            serializer = PULL_PARSER_FACTORY.newSerializer();
            BufferedWriter out = null;
            try {
                out = getResponseWriter(request, response);
                serializer.setOutput(out);
                serializer.setProperty(INDENTATION_PROPERTY, INDENTATION_PROPERTY_VALUE);
                serializer.startDocument(null, false);
                serializer.text("\n");
                if (DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getSequenceXSLT() != null) {
                    serializer.processingInstruction(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getSequenceXSLT());
                    serializer.text("\n");
                }
                serializer.docdecl(" DASSEQUENCE SYSTEM \"http://www.biodas.org/dtd/dassequence.dtd\"");
                serializer.text("\n");
                serializer.startTag(DAS_XML_NAMESPACE, "DASSEQUENCE");
                for (SequenceReporter sequenceReporter : sequences) {
                    serializer.startTag(DAS_XML_NAMESPACE, "SEQUENCE");
                    serializer.attribute(DAS_XML_NAMESPACE, "id", sequenceReporter.getSegmentName());
                    serializer.attribute(DAS_XML_NAMESPACE, "start", Integer.toString(sequenceReporter.getStart()));
                    serializer.attribute(DAS_XML_NAMESPACE, "stop", Integer.toString(sequenceReporter.getStop()));
                    serializer.attribute(DAS_XML_NAMESPACE, "moltype", sequenceReporter.getSequenceMoleculeType());
                    serializer.attribute(DAS_XML_NAMESPACE, "version", sequenceReporter.getSequenceVersion());
                    serializer.text(sequenceReporter.getSequenceString());
                    serializer.endTag(DAS_XML_NAMESPACE, "SEQUENCE");
                }
                serializer.endTag(DAS_XML_NAMESPACE, "DASSEQUENCE");
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } else {
            throw new UnimplementedFeatureException("An attempt to request sequence information from an anntation server has been detected.");
        }
    }

    /**
     * Helper method used by both the featuresCommand and typesCommand to return a Collection of SegmentReporter objects.
     *
     * The SegmentReporter interface is implemented to allow both correctly returned segments and missing segments
     * to be returned.
     * @param dsnConfig holding configuration of the dsn and the data source object itself.
     * @param requestedSegments being a List of SegmentQuery objects, which encapsulate the segment request (including
     * the segment id and optional start / stop coordinates)
     * @return a Collection of FeatureReporter objects that wrap the DasFeature objects returned from the data source
     * @throws DataSourceException to capture any error returned from the data source that cannot be handled in a more
     * elegant manner.
     * @param unknownSegmentsHandled to indicate if the calling method is able to report missing segments (i.e.
     * the feature command can return errorsegment / unknownsegment).
     * @throws uk.ac.ebi.mydas.exceptions.BadReferenceObjectException thrown if unknownSegmentsHandled is false and
     * the segment id is not known to the DSN.
     * @throws uk.ac.ebi.mydas.exceptions.CoordinateErrorException thrown if unknownSegmentsHandled is false and
     * the segment coordinates are out of scope for the provided segment id.
     */
    private Collection<SegmentReporter> getFeatureCollection(DataSourceConfiguration dsnConfig, List<SegmentQuery> requestedSegments, boolean unknownSegmentsHandled) throws DataSourceException, BadReferenceObjectException, CoordinateErrorException {
        List<SegmentReporter> segmentReporterLists = new ArrayList<SegmentReporter>(requestedSegments.size());
        AnnotationDataSource dataSource = dsnConfig.getDataSource();
        for (SegmentQuery segmentQuery : requestedSegments) {
            try {
                DasAnnotatedSegment annotatedSegment;
                StringBuffer cacheKeyBuffer = new StringBuffer(dsnConfig.getId());
                cacheKeyBuffer.append("_FEATURES_");
                if (dataSource instanceof RangeHandlingAnnotationDataSource || dataSource instanceof RangeHandlingReferenceDataSource) {
                    cacheKeyBuffer.append(segmentQuery.toString());
                } else {
                    cacheKeyBuffer.append(segmentQuery.getSegmentId());
                }
                String cacheKey = cacheKeyBuffer.toString();
                try {
                    annotatedSegment = (DasAnnotatedSegment) CACHE_MANAGER.getFromCache(cacheKey);
                    if (logger.isDebugEnabled()) {
                        logger.debug("FEATURES RETRIEVED FROM CACHE: " + annotatedSegment.getSegmentId());
                    }
                    if (annotatedSegment == null) {
                        throw new BadReferenceObjectException(segmentQuery.getSegmentId(), "Obtained an annotatedSegment from the cache for this segment.  It was null, so assume this is a bad segment id.");
                    }
                } catch (NeedsRefreshException nre) {
                    try {
                        if (segmentQuery.getStartCoordinate() == null) {
                            annotatedSegment = dataSource.getFeatures(segmentQuery.getSegmentId());
                        } else {
                            if (dataSource instanceof RangeHandlingAnnotationDataSource) {
                                annotatedSegment = ((RangeHandlingAnnotationDataSource) dataSource).getFeatures(segmentQuery.getSegmentId(), segmentQuery.getStartCoordinate(), segmentQuery.getStopCoordinate());
                            } else if (dataSource instanceof RangeHandlingReferenceDataSource) {
                                annotatedSegment = ((RangeHandlingReferenceDataSource) dataSource).getFeatures(segmentQuery.getSegmentId(), segmentQuery.getStartCoordinate(), segmentQuery.getStopCoordinate());
                            } else {
                                annotatedSegment = dataSource.getFeatures(segmentQuery.getSegmentId());
                            }
                        }
                        if (logger.isDebugEnabled()) {
                            logger.debug("FEATURES NOT IN CACHE: " + annotatedSegment.getSegmentId());
                        }
                        CACHE_MANAGER.putInCache(cacheKey, annotatedSegment, dsnConfig.getCacheGroup());
                    } catch (BadReferenceObjectException broe) {
                        CACHE_MANAGER.cancelUpdate(cacheKey);
                        throw broe;
                    } catch (CoordinateErrorException cee) {
                        CACHE_MANAGER.cancelUpdate(cacheKey);
                        throw cee;
                    }
                }
                segmentReporterLists.add(new FoundFeaturesReporter(annotatedSegment, segmentQuery));
            } catch (BadReferenceObjectException broe) {
                if (unknownSegmentsHandled) {
                    segmentReporterLists.add(new UnknownSegmentReporter(segmentQuery));
                } else {
                    throw broe;
                }
            } catch (CoordinateErrorException cee) {
                if (unknownSegmentsHandled) {
                    segmentReporterLists.add(new UnknownSegmentReporter(segmentQuery));
                } else {
                    throw cee;
                }
            }
        }
        return segmentReporterLists;
    }

    /**
     * Helper method used by both the dnaCommand and the sequenceCommand
     * @param dsnConfig holding configuration of the dsn and the data source object itself.
     * @param queryString to be parsed, which includes details of the requested segments
     * @return a Collection of SequenceReporter objects.  The SequenceReporter wraps the DasSequence object
     * to provide additional functionality that is hidden (for simplicity) from the dsn developer.
     * @throws BadReferenceObjectException if the segment id is not available from the data source
     * @throws CoordinateErrorException if the requested coordinates fall outside those of the requested segment id
     * @throws DataSourceException to capture any error returned from the data source.
     * @throws BadCommandArgumentsException if the arguments to the command are not recognised.
     */
    private Collection<SequenceReporter> getSequences(DataSourceConfiguration dsnConfig, String queryString) throws DataSourceException, BadCommandArgumentsException, BadReferenceObjectException, CoordinateErrorException {
        ReferenceDataSource refDsn = (ReferenceDataSource) dsnConfig.getDataSource();
        if (refDsn == null) {
            throw new DataSourceException("An attempt has been made to retrieve a sequenceString from datasource " + dsnConfig.getId() + " however the DataSource object is null.");
        }
        Collection<SequenceReporter> sequenceCollection = new ArrayList<SequenceReporter>();
        if (queryString == null || queryString.length() == 0) {
            throw new BadCommandArgumentsException("Expecting at least one reference in the query string, but found nothing.");
        }
        String[] referenceStrings = queryString.split(";");
        for (String referenceString : referenceStrings) {
            Matcher referenceStringMatcher = SEGMENT_RANGE_PATTERN.matcher(referenceString);
            if (referenceStringMatcher.find()) {
                SegmentQuery segmentQuery = new SegmentQuery(referenceStringMatcher);
                DasSequence sequence;
                StringBuffer cacheKeyBuffer = new StringBuffer(dsnConfig.getId());
                cacheKeyBuffer.append("_SEQUENCE_");
                if (refDsn instanceof RangeHandlingReferenceDataSource) {
                    cacheKeyBuffer.append(segmentQuery.toString());
                } else {
                    cacheKeyBuffer.append(segmentQuery.getSegmentId());
                }
                String cacheKey = cacheKeyBuffer.toString();
                try {
                    sequence = (DasSequence) CACHE_MANAGER.getFromCache(cacheKey);
                    if (logger.isDebugEnabled()) {
                        logger.debug("SEQUENCE RETRIEVED FROM CACHE: " + sequence.getSegmentId());
                    }
                } catch (NeedsRefreshException nre) {
                    try {
                        if (segmentQuery.getStartCoordinate() != null) {
                            if (refDsn instanceof RangeHandlingReferenceDataSource) {
                                sequence = ((RangeHandlingReferenceDataSource) refDsn).getSequence(segmentQuery.getSegmentId(), segmentQuery.getStartCoordinate(), segmentQuery.getStopCoordinate());
                                CACHE_MANAGER.putInCache(cacheKey, sequence, dsnConfig.getCacheGroup());
                            } else {
                                sequence = refDsn.getSequence(segmentQuery.getSegmentId());
                                CACHE_MANAGER.putInCache(cacheKey, sequence, dsnConfig.getCacheGroup());
                            }
                        } else {
                            sequence = refDsn.getSequence(segmentQuery.getSegmentId());
                            CACHE_MANAGER.putInCache(cacheKey, sequence, dsnConfig.getCacheGroup());
                        }
                    } catch (BadReferenceObjectException broe) {
                        CACHE_MANAGER.cancelUpdate(cacheKey);
                        throw broe;
                    } catch (DataSourceException dse) {
                        CACHE_MANAGER.cancelUpdate(cacheKey);
                        throw dse;
                    } catch (CoordinateErrorException cee) {
                        CACHE_MANAGER.cancelUpdate(cacheKey);
                        throw cee;
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Sequence retrieved from DSN (not cached): " + sequence.getSegmentId());
                    }
                }
                if (sequence == null) throw new BadReferenceObjectException(segmentQuery.getSegmentId(), "Segment cannot be found.");
                sequenceCollection.add(new SequenceReporter(sequence, segmentQuery));
            } else {
                throw new BadCommandArgumentsException("The query string format is not recognized.");
            }
        }
        if (sequenceCollection.size() == 0) {
            throw new BadCommandArgumentsException("The query string format is not recognized.");
        }
        return sequenceCollection;
    }

    /**
     * Writes the response header with the additional DAS Http headers.
     * @param response to which to write the headers.
     * @param status being the status to write.
     * @param request required to determine if the client will accept a compressed response
     * @param compressionAllowed to indicate if the specific response should be gzipped. (e.g. an error message with
     * no content should not set the compressed header.)
     */
    private void writeHeader(HttpServletRequest request, HttpServletResponse response, XDasStatus status, boolean compressionAllowed) {
        response.setHeader(HEADER_KEY_X_DAS_VERSION, HEADER_VALUE_DAS_VERSION);
        response.setHeader(HEADER_KEY_X_DAS_CAPABILITIES, HEADER_VALUE_CAPABILITIES);
        response.setHeader(HEADER_KEY_X_DAS_STATUS, status.toString());
        if (compressionAllowed && compressResponse(request)) {
            response.setHeader(ENCODING_RESPONSE_HEADER_KEY, ENCODING_GZIPPED);
        }
    }

    /**
     * Returns a PrintWriter for the response. First checks if the output should / can be
     * gzipped. If so, wraps the OutputStream in a GZIPOutputStream and then returns
     * a PrintWriter to this.
     * @param request the HttpServletRequest, needed to check the capabilities of the
     * client.
     * @param response from which the OutputStream is obtained
     * @return a PrintWriter that will either produce plain or gzipped output.
     * @throws IOException due to a problem with initiating the output stream or writer.
     */
    public BufferedWriter getResponseWriter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (compressResponse(request)) {
            GZIPOutputStream zipStream = new GZIPOutputStream(response.getOutputStream());
            return new BufferedWriter(new PrintWriter(zipStream));
        } else {
            return new BufferedWriter(response.getWriter());
        }
    }

    /**
     * Checks in the configuration to see if the output should be gzipped and also
     * checks if the client can accept gzipped output.
     * @param request being the HttpServletRequest, to allow a check of the client capabilities to be checked.
     * @return a boolean indicating if the response should be compressed.
     */
    private boolean compressResponse(HttpServletRequest request) {
        String clientEncodingAbility = request.getHeader(ENCODING_REQUEST_HEADER_KEY);
        return DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().isGzipped() && clientEncodingAbility != null && clientEncodingAbility.contains(ENCODING_GZIPPED);
    }

    /**
     * Helper method that re-constructs the URL that was used to query the service.
     * @param request to retrieve elements of the URL
     * @return the URL that was used to query the service.
     */
    public String buildRequestHref(HttpServletRequest request) {
        StringBuffer requestURL = new StringBuffer(DATA_SOURCE_MANAGER.getServerConfiguration().getGlobalConfiguration().getBaseURL());
        String requestURI = request.getRequestURI();
        requestURL.append(requestURI.substring(5 + requestURI.indexOf("/das/")));
        String queryString = request.getQueryString();
        if (queryString != null && queryString.length() > 0) {
            requestURL.append('?').append(queryString);
        }
        return requestURL.toString();
    }

    GeneralCacheAdministrator getCacheManager() {
        return CACHE_MANAGER;
    }

    public DataSourceManager getDataSourceManager() {
        return DATA_SOURCE_MANAGER;
    }
}
