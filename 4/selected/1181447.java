package net.sf.wubiq.clients;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.UnknownServiceException;
import java.util.HashMap;
import java.util.Map;
import javax.print.DocFlavor;
import javax.print.PrintService;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.PrintJobAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import net.sf.wubiq.common.CommandKeys;
import net.sf.wubiq.common.ParameterKeys;
import net.sf.wubiq.utils.ClientLabels;
import net.sf.wubiq.utils.ClientPrintDirectUtils;
import net.sf.wubiq.utils.ClientProperties;
import net.sf.wubiq.utils.Is;
import net.sf.wubiq.utils.Labels;
import net.sf.wubiq.utils.PrintServiceUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Keeps gathering printer information and reports it to the server.
 * This is the main element of the client.
 * If the connection to the client fails, it keeps trying until the program is closed.
 * As soon as connection is established identifies itself, sends the probable computer name
 * and, if defined, a unique id (UUID). If no unique id is defined, the uuid is automatically
 * generated.
 * @author Federico Alcantara
 *
 */
public class LocalPrintManager implements Runnable {

    private static final Log LOG = LogFactory.getLog(LocalPrintManager.class);

    private String host;

    private String port;

    private String applicationName;

    private String servletName;

    private String uuid;

    private boolean killManager;

    private boolean refreshServices;

    private boolean debugMode;

    private long checkPendingJobInterval = 10000;

    private long printingJobInterval = 3000;

    private int connectionErrorRetries = -1;

    private int connectionErrorCount = 0;

    private boolean cancelManager = false;

    private Map<String, PrintService> printServicesName;

    private long lastServerTimestamp = -1;

    public LocalPrintManager() {
    }

    public LocalPrintManager(String host) {
        this.host = host;
    }

    public LocalPrintManager(String host, String port) {
        this(host);
        this.port = port;
    }

    public LocalPrintManager(String host, String port, String applicationName) {
        this(host, port);
        this.applicationName = applicationName;
    }

    @Override
    public void run() {
        if (killManager) {
            killManager();
            System.out.println(ClientLabels.get("client.closing_local_manager"));
        } else {
            if (!isActive()) {
                bringAlive();
                refreshServices = true;
                while (!isKilled()) {
                    try {
                        if (needsRefresh()) {
                            registerPrintServices();
                            refreshServices = false;
                            connectionErrorCount = 0;
                        }
                        String[] pendingJobs = getPendingJobs();
                        for (String pendingJob : pendingJobs) {
                            processPendingJob(pendingJob);
                            Thread.sleep(getPrintingJobInterval());
                        }
                        Thread.sleep(getCheckPendingJobInterval());
                    } catch (ConnectException e) {
                        if (getConnectionErrorRetries() >= 0) {
                            connectionErrorCount++;
                            if (connectionErrorCount > getConnectionErrorRetries()) {
                                break;
                            }
                        }
                        LOG.debug(e.getMessage());
                        refreshServices = true;
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e1) {
                            LOG.debug(e.getMessage());
                        }
                    } catch (InterruptedException e) {
                        LOG.error(e.getMessage(), e);
                        break;
                    }
                }
                LOG.info(ClientLabels.get("client.closing_local_manager"));
                killManager();
            } else {
                LOG.info(ClientLabels.get("client.another_process_is_running"));
            }
        }
    }

    /**
	 * Kills the manager.
	 */
    protected void killManager() {
        doLog("Kill Manager");
        try {
            askServer(CommandKeys.KILL_MANAGER);
        } catch (ConnectException e) {
            doLog(e.getMessage());
        }
    }

    /**
	 * Allow manager to re-register.
	 */
    protected void bringAlive() {
        doLog("Bring alive");
        try {
            askServer(CommandKeys.BRING_ALIVE);
        } catch (ConnectException e) {
            doLog(e.getMessage());
        }
    }

    /**
	 * Process a single pending job.
	 * @param jobId Id of the job to be printed.
	 */
    protected void processPendingJob(String jobId) throws ConnectException {
        String parameter = printJobPollString(jobId);
        doLog("Process Pending Job:" + jobId);
        InputStream printData = null;
        try {
            String printServiceName = askServer(CommandKeys.READ_PRINT_SERVICE_NAME, parameter);
            doLog("Job(" + jobId + ") printServiceName:" + printServiceName);
            String printRequestAttributesData = askServer(CommandKeys.READ_PRINT_REQUEST_ATTRIBUTES, parameter);
            doLog("Job(" + jobId + ") printRequestAttributes:" + printRequestAttributesData);
            String printJobAttributesData = askServer(CommandKeys.READ_PRINT_JOB_ATTRIBUTES, parameter);
            doLog("Job(" + jobId + ") printJobAttributes:" + printJobAttributesData);
            String docAttributesData = askServer(CommandKeys.READ_DOC_ATTRIBUTES, parameter);
            doLog("Job(" + jobId + ") docAttributes:" + docAttributesData);
            String docFlavorData = askServer(CommandKeys.READ_DOC_FLAVOR, parameter);
            doLog("Job(" + jobId + ") docFlavor:" + docFlavorData);
            printData = (InputStream) pollServer(CommandKeys.READ_PRINT_JOB, parameter);
            PrintService printService = getPrintServicesName().get(printServiceName);
            PrintRequestAttributeSet printRequestAttributeSet = PrintServiceUtils.convertToPrintRequestAttributeSet(printRequestAttributesData);
            PrintJobAttributeSet printJobAttributeSet = (PrintJobAttributeSet) PrintServiceUtils.convertToPrintJobAttributeSet(printJobAttributesData);
            DocAttributeSet docAttributeSet = PrintServiceUtils.convertToDocAttributeSet(docAttributesData);
            DocFlavor docFlavor = PrintServiceUtils.deSerializeDocFlavor(docFlavorData);
            ClientPrintDirectUtils.print(jobId, printService, printRequestAttributeSet, printJobAttributeSet, docAttributeSet, docFlavor, printData);
            doLog("Job(" + jobId + ") printed.");
        } catch (ConnectException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            try {
                if (printData != null) {
                    printData.close();
                }
            } catch (IOException e) {
                doLog(e.getMessage());
            }
            try {
                askServer(CommandKeys.CLOSE_PRINT_JOB, parameter);
                doLog("Job(" + jobId + ") closing print job.");
            } catch (Exception e) {
                doLog(e.getMessage());
            }
        }
    }

    /**
	 * Creates the print job poll string.
	 * @param jobId Id of the print job to pull from the server.
	 * @return String (url) for polling the server.
	 */
    private String printJobPollString(String jobId) {
        StringBuffer parameter = new StringBuffer(ParameterKeys.PRINT_JOB_ID).append(ParameterKeys.PARAMETER_SEPARATOR).append(jobId);
        return parameter.toString();
    }

    /**
	 * Ask to the server for a list of all pending jobs for this client instance.
	 * @return A list of pending jobs, or a zero element String array. Never null.
	 */
    protected String[] getPendingJobs() throws ConnectException {
        doLog("Get Pending Jobs");
        String[] returnValue = new String[] {};
        String pendingJobResponse = askServer(CommandKeys.PENDING_JOBS);
        if (!Is.emptyString(pendingJobResponse) && pendingJobResponse.startsWith(ParameterKeys.PENDING_JOB_SIGNATURE)) {
            returnValue = pendingJobResponse.substring(ParameterKeys.PENDING_JOB_SIGNATURE.length()).split(ParameterKeys.CATEGORIES_SEPARATOR);
        }
        doLog("pending jobs:" + pendingJobResponse);
        return returnValue;
    }

    /**
	 * Registers the probable computer name, and initializes remote print services for this client instance.
	 */
    protected void registerComputerName() throws ConnectException {
        doLog("Register Computer Name");
        StringBuffer computerName = new StringBuffer(ParameterKeys.COMPUTER_NAME).append(ParameterKeys.PARAMETER_SEPARATOR);
        try {
            computerName.append(InetAddress.getLocalHost().getHostName());
            doLog("Register computer name:" + computerName);
        } catch (UnknownHostException e) {
            LOG.error(e.getMessage(), e);
        }
        askServer(CommandKeys.REGISTER_COMPUTER_NAME, computerName.toString());
    }

    /**
	 * Registers all valid local print services to the remote server.
	 */
    protected void registerPrintServices() throws ConnectException {
        boolean reload = false;
        Map<String, PrintService> newPrintServices = new HashMap<String, PrintService>();
        for (PrintService printService : PrintServiceUtils.getPrintServices()) {
            String printServiceName = PrintServiceUtils.cleanPrintServiceName(printService);
            newPrintServices.put(printServiceName, printService);
            if (!getPrintServicesName().containsKey(printServiceName)) {
                reload = true;
            }
        }
        if (newPrintServices.size() != getPrintServicesName().size()) {
            reload = true;
        }
        long serverTimestamp = -2l;
        try {
            serverTimestamp = Long.parseLong(askServer(CommandKeys.SERVER_TIMESTAMP));
        } catch (NumberFormatException e) {
            serverTimestamp = -2l;
        }
        if (serverTimestamp != lastServerTimestamp) {
            reload = true;
        }
        if (!reload) {
            try {
                askServer(CommandKeys.IS_ACTIVE);
            } catch (ConnectException e) {
                getPrintServicesName().clear();
                reload = true;
                return;
            }
        }
        if (reload) {
            registerComputerName();
            doLog("Register Print Services");
            getPrintServicesName().clear();
            for (PrintService printService : PrintServiceUtils.getPrintServices()) {
                String printServiceName = PrintServiceUtils.serializeServiceName(printService, debugMode);
                getPrintServicesName().put(PrintServiceUtils.cleanPrintServiceName(printService), printService);
                doLog("Print service:" + printServiceName);
                StringBuffer printServiceRegister = new StringBuffer(printServiceName);
                StringBuffer categories = new StringBuffer(PrintServiceUtils.serializeServiceCategories(printService, debugMode));
                categories.insert(0, ParameterKeys.PARAMETER_SEPARATOR).insert(0, ParameterKeys.PRINT_SERVICE_CATEGORIES);
                askServer(CommandKeys.REGISTER_PRINT_SERVICE, printServiceRegister.toString(), categories.toString(), PrintServiceUtils.serializeDocumentFlavors(printService, debugMode));
            }
            lastServerTimestamp = serverTimestamp;
        }
    }

    /**
	 * Asks the server if this instance of the client should be closed. This will allow for remote cancellation of client.
	 * (Not yet implemented).
	 * @return
	 */
    protected boolean isKilled() {
        boolean returnValue = isCancelManager();
        if (!returnValue) {
            try {
                if (askServer(CommandKeys.IS_KILLED).equals("1")) {
                    returnValue = true;
                }
            } catch (ConnectException e) {
                doLog(e.getMessage());
            }
        }
        doLog("Is Killed?" + returnValue);
        return returnValue;
    }

    /**
	 * Asks the server if this instance of the client should be closed. This will allow for remote cancellation of client.
	 * (Not yet implemented).
	 * @return
	 */
    protected boolean isActive() {
        boolean returnValue = false;
        try {
            if (askServer(CommandKeys.IS_ACTIVE).equals("1")) {
                returnValue = true;
            }
        } catch (ConnectException e) {
            doLog(e.getMessage());
        }
        doLog("Is Active?" + returnValue);
        return returnValue;
    }

    /**
	 * Asks the server if this instance of the client needs to be refreshed with the list of print services.
	 * @return
	 */
    protected boolean needsRefresh() {
        boolean returnValue = true;
        try {
            if (askServer(CommandKeys.IS_REFRESHED).equals("1") && !refreshServices) {
                returnValue = false;
            }
        } catch (ConnectException e) {
            doLog(e.getMessage());
        }
        doLog("Needs Refresh?" + returnValue);
        return returnValue;
    }

    /**
	 * Send command to server and returns its response as a string.
	 * @param command Command to send to the server.
	 * @param parameters List of parameters to be sent.
	 * @return Response from server.
	 * @throws ConnectException If it can't connect to the server.
	 */
    protected String askServer(String command, String... parameters) throws ConnectException {
        return (String) pollServer(command, parameters);
    }

    /**
	 * Send command to server and returns it response as an object.
	 * @param command Command to send to the server.
	 * @param parameters List of parameters to be sent.
	 * @return Response from server.
	 * @throws ConnectException If it can't connect to the server.
	 */
    protected Object pollServer(String command, String... parameters) throws ConnectException {
        Object returnValue = "";
        String url = null;
        URL webUrl = null;
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {
            url = getEncodedUrl(command, parameters);
            doLog("URL:" + url);
            webUrl = new URL(url);
            connection = (HttpURLConnection) webUrl.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
            connection.setRequestMethod("POST");
            Object content = connection.getContent();
            if (connection.getContentType() != null) {
                if (connection.getContentType().equals("application/pdf")) {
                    returnValue = (InputStream) content;
                } else if (connection.getContentType().equals("text/html")) {
                    InputStream inputStream = (InputStream) content;
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    int readByte;
                    while ((readByte = inputStream.read()) > -1) {
                        output.write(readByte);
                    }
                    returnValue = new String(output.toByteArray());
                    output.close();
                    inputStream.close();
                }
            }
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage() + "->" + url);
        } catch (UnknownServiceException e) {
            doLog(e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage() + " " + url);
            throw new ConnectException(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
        return returnValue;
    }

    /**
	 * Properly forms the url and encode the parameters so that servers can receive them correctly.
	 * @param command Command to be encoded as part of the url.
	 * @param parameters Arrays of parameters in the form parameterName=parameterValue that will be appended to the url.
	 * @return Url string with parameterValues encoded.
	 */
    protected String getEncodedUrl(String command, String... parameters) {
        StringBuffer url = new StringBuffer(hostServletUrl()).append('?').append(ParameterKeys.UUID).append(ParameterKeys.PARAMETER_SEPARATOR).append(getUuid());
        if (!Is.emptyString(command)) {
            url.append('&').append(ParameterKeys.COMMAND).append(ParameterKeys.PARAMETER_SEPARATOR).append(command);
        }
        for (String parameter : parameters) {
            String parameterString = parameter;
            if (parameter.contains("=")) {
                String parameterName = parameter.substring(0, parameter.indexOf("="));
                String parameterValue = parameter.substring(parameter.indexOf("=") + 1);
                try {
                    parameterValue = URLEncoder.encode(parameterValue, "UTF-8");
                    parameterString = parameterName + "=" + parameterValue;
                } catch (UnsupportedEncodingException e) {
                    LOG.error(e.getMessage());
                }
            }
            url.append('&').append(parameterString);
        }
        String returnValue = url.toString();
        if (command.equals(CommandKeys.PRINT_TEST_PAGE) || command.equals(CommandKeys.SHOW_PRINT_SERVICES)) {
            returnValue = returnValue.replace("wubiq.do", "wubiq-print-test.do");
        }
        return returnValue;
    }

    /**
	 * Properly concatenates host, port, applicationName and servlet name.
	 * @return Concatenated strings.
	 */
    protected String hostServletUrl() {
        StringBuffer buffer = new StringBuffer(getHost());
        if (!Is.emptyString(getPort())) {
            buffer.append(':').append(getPort());
        }
        if (!Is.emptyString(getApplicationName())) {
            appendWebChar(buffer, '/').append(getApplicationName());
        }
        if (!Is.emptyString(getServletName())) {
            appendWebChar(buffer, '/').append(getServletName());
        }
        return buffer.toString();
    }

    /**
	 * @param host the host to set
	 */
    protected void setHost(String host) {
        this.host = host;
    }

    /**
	 * @return the host
	 */
    public String getHost() {
        return host;
    }

    /**
	 * @param port the port to set
	 */
    protected void setPort(String port) {
        this.port = port;
    }

    /**
	 * @return the port
	 */
    public String getPort() {
        return port.trim();
    }

    /**
	 * @param applicationName the applicationName to set
	 */
    protected void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
	 * @return the applicationName
	 */
    public String getApplicationName() {
        return applicationName.trim();
    }

    /**
	 * @param servletName the servletName to set
	 */
    protected void setServletName(String servletName) {
        this.servletName = servletName;
    }

    /**
	 * @return the servletName
	 */
    public String getServletName() {
        return servletName.trim();
    }

    /**
	 * @param uuid the uuid to set
	 */
    protected void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
	 * @return the uuid
	 */
    public String getUuid() {
        return uuid.trim();
    }

    /**
	 * @param debugMode the debugMode to set
	 */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
	 * @return the debugMode
	 */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
	 * @return the checkPendingJobInterval
	 */
    public long getCheckPendingJobInterval() {
        return checkPendingJobInterval;
    }

    /**
	 * @param checkPendingJobInterval the checkPendingJobInterval to set
	 */
    public void setCheckPendingJobInterval(long checkPendingJobInterval) {
        this.checkPendingJobInterval = checkPendingJobInterval;
    }

    /**
	 * @return the printingJobInterval
	 */
    public long getPrintingJobInterval() {
        return printingJobInterval;
    }

    /**
	 * @param printingJobInterval the printingJobInterval to set
	 */
    public void setPrintingJobInterval(long printingJobInterval) {
        this.printingJobInterval = printingJobInterval;
    }

    /**
	 * @return the connectionErrorRetries
	 */
    public int getConnectionErrorRetries() {
        return connectionErrorRetries;
    }

    /**
	 * @param connectionErrorRetries the connectionErrorRetries to set
	 */
    public void setConnectionErrorRetries(int connectionErrorRetries) {
        this.connectionErrorRetries = connectionErrorRetries;
    }

    /**
	 * @return the cancelManager
	 */
    public boolean isCancelManager() {
        return cancelManager;
    }

    /**
	 * @param cancelManager the cancelManager to set
	 */
    public void setCancelManager(boolean cancelManager) {
        this.cancelManager = cancelManager;
    }

    /**
	 * Appends a character to the buffer only if the buffer doesn't end with that character.
	 * @param buffer Buffer to be appended.
	 * @param webChar Character to be appended.
	 * @return The StringBuffer ending with the char.
	 */
    private StringBuffer appendWebChar(StringBuffer buffer, char webChar) {
        if (buffer.length() == 0) {
            buffer.append(webChar);
        } else if (buffer.charAt(buffer.length() - 1) != webChar) {
            buffer.append(webChar);
        }
        return buffer;
    }

    protected void doLog(Object message) {
        if (isDebugMode()) {
            LOG.info(message);
        } else {
            LOG.debug(message);
        }
    }

    protected static void initializeDefault(LocalPrintManager manager) {
        manager.setHost(ClientProperties.getHost());
        manager.setPort(ClientProperties.getPort());
        manager.setApplicationName(ClientProperties.getApplicationName());
        manager.setServletName(ClientProperties.getServletName());
        manager.setUuid(ClientProperties.getUuid());
    }

    private Map<String, PrintService> getPrintServicesName() {
        if (printServicesName == null) {
            printServicesName = new HashMap<String, PrintService>();
        }
        return printServicesName;
    }

    /**
	 * Parses the command line and starts an instance of a client.
	 * @param args Command line arguments.
	 * @throws Exception
	 */
    public static void main(String[] args) throws Exception {
        LocalPrintManager manager = new LocalPrintManager();
        System.out.println(ClientLabels.get("client.version", Labels.VERSION));
        System.out.println("http://sourceforge.net/projects/wubiq\n");
        Options options = new Options();
        options.addOption("?", "help", false, ClientLabels.get("client.command_line_help"));
        options.addOption("k", "kill", false, ClientLabels.get("client.command_line_kill"));
        options.addOption("h", "host", true, ClientLabels.get("client.command_line_host"));
        options.addOption("p", "port", true, ClientLabels.get("client.command_line_port"));
        options.addOption("a", "app", true, ClientLabels.get("client.command_line_app"));
        options.addOption("s", "servlet", true, ClientLabels.get("client.command_line_servlet"));
        options.addOption("u", "uuid", true, ClientLabels.get("client.command_line_uuid"));
        options.addOption("v", "verbose", false, ClientLabels.get("client.command_line_verbose"));
        initializeDefault(manager);
        CommandLineParser parser = new GnuParser();
        try {
            CommandLine line = parser.parse(options, args);
            if (line.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("java -jar wubiq-client.jar", options, true);
            } else {
                if (line.hasOption("kill")) {
                    manager.killManager = true;
                }
                if (line.hasOption("host")) {
                    manager.setHost(line.getOptionValue("host"));
                }
                if (line.hasOption("port")) {
                    manager.setPort(line.getOptionValue("port"));
                }
                if (line.hasOption("app")) {
                    manager.setApplicationName(line.getOptionValue("app"));
                }
                if (line.hasOption("servlet")) {
                    manager.setServletName(line.getOptionValue("servlet"));
                }
                if (line.hasOption("uuid")) {
                    manager.setUuid(line.getOptionValue("uuid"));
                }
                if (line.hasOption("verbose")) {
                    manager.setDebugMode(true);
                }
                Thread r = new Thread(manager);
                r.start();
            }
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar wubiq-client.jar", options, true);
        }
    }
}
