package fi.hip.gb.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globus.myproxy.MyProxyException;
import org.ietf.jgss.GSSException;
import fi.hip.gb.core.Config;
import fi.hip.gb.core.JobAttachment;
import fi.hip.gb.core.JobExecutable;
import fi.hip.gb.core.JobInfo;
import fi.hip.gb.core.SessionHandler;
import fi.hip.gb.core.WorkDescription;
import fi.hip.gb.core.WorkResult;
import fi.hip.gb.core.WorkStatus;
import fi.hip.gb.net.ComputingInterface;
import fi.hip.gb.net.discovery.DiscoveryPacket;
import fi.hip.gb.net.discovery.DiscoveryService;
import fi.hip.gb.serializer.BinaryMessage;
import fi.hip.gb.utils.FileUtils;
import fi.hip.gb.utils.GSIUtils;
import fi.hip.gb.utils.MyProxyInit;
import fi.hip.gb.utils.TextUtils;

/**
 * Service for J2ME clients. It is basicly only a servlet which just forwards
 * all client's requets into the real server ({@link DefaultService }.
 * <code>J2meService</code> returns only the most relevant parts to the j2me
 * client using binary coded Strings instead of XML. This reduses the used
 * network bandwidth between j2me client and server.
 * <p>
 * Even the communication is simplified the server is still full functional. The
 * requests are forwarded to the {@link DefaultService }similar to using
 * heavier SOAP client. GBAgent server doesn't see any difference on that.
 * 
 * @author Juho Karppinen
 */
public class J2meService extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Log log = LogFactory.getLog(J2meService.class);

    /**
     * Initializes the servlet
     * 
     * @param config
     *            configurations
     * @throws ServletException
     *             if error occurred
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Handler, reads users requests and returns the results
     * 
     * @param request
     *            request object
     * @param response
     *            our response object
     * @throws ServletException
     *             if error occurred
     * @throws IOException
     *             if error occurred
     */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletInputStream in = request.getInputStream();
        BinaryMessage query = null;
        BinaryMessage output = null;
        try {
            query = new BinaryMessage(in);
            String method = query.getType();
            HttpSession session = request.getSession();
            if (session.isNew()) log.info("New session created: " + session.getId()); else log.info("Used existing session: " + session.getId());
            if (method.equals("login")) {
                output = login(query, request);
            } else {
                GSIUtils proxyStatus = null;
                String proxyContent = (String) session.getAttribute("User-Information");
                if (proxyContent != null) {
                    if (proxyContent.equals("none")) {
                        proxyStatus = new GSIUtils();
                    } else {
                        proxyStatus = new GSIUtils(proxyContent.getBytes());
                        if (!proxyStatus.getProxyStatus()) throw new RemoteException(proxyStatus.getProxyStatusMessage());
                    }
                } else {
                    throw new RemoteException("No session found");
                }
                ComputingInterface server = new DefaultService(proxyStatus.getUserDN());
                if (method.equals("getJoblist")) {
                    output = getJoblist(query, server);
                } else if (method.equals("getDescription")) {
                    output = getDescription(query, server);
                } else if (method.equals("getStatus")) {
                    output = getStatus(query, server);
                } else if (method.equals("getResult")) {
                    output = getResult(query, server);
                } else if (method.equals("dispatch")) {
                    output = dispatch(query, server, proxyStatus);
                } else if (method.equals("abortJob")) {
                    output = abort(query, server);
                } else if (method.equals("listServices")) {
                    output = listServices();
                } else {
                    throw new RemoteException("no method specified");
                }
            }
        } catch (RemoteException re) {
            output = new BinaryMessage(1);
            output.addString("REMOTEEXCEPTION" + re.getMessage());
            log.error(re.getMessage(), re);
        } finally {
            if (query != null) query.close();
        }
        byte[] bytes = output.toByteArray();
        response.setContentType("application/octet-stream");
        response.setContentLength(bytes.length);
        response.setStatus(HttpServletResponse.SC_OK);
        OutputStream out = response.getOutputStream();
        out.write(bytes);
        out.close();
    }

    /**
     * Gets the login status for user the user
     * 
     * @param query
     *            contains following elements
     *            <ol>
     *            <li>username</li>
     *            <li>password, if null retrieve the existing session</li>
     *            </ol>
     * @param request
     *            reference to http request
     * @return expiration time of proxy
     * @throws RemoteException
     *             if user is not logged in
     */
    private BinaryMessage login(BinaryMessage query, HttpServletRequest request) throws RemoteException {
        String username = query.readString();
        String password = query.hasNext() ? query.readString() : null;
        String hostname = query.hasNext() ? query.readString() : null;
        if (hostname == null || hostname.length() == 0) hostname = Config.getInstance().getMyProxyServer();
        HttpSession session = request.getSession();
        String proxyContent = (String) session.getAttribute("User-Information");
        if (password == null && proxyContent == null) {
            throw new RemoteException("User not logged in");
        }
        if (password != null) {
            log.info("User " + username + " logging in...");
            if (username.equals("local")) {
                proxyContent = new GSIUtils(Config.getInstance().getProxyFile()).getProxyContent();
                if (proxyContent == null) {
                    throw new RemoteException("Local proxy not found from " + Config.getInstance().getProxyFile());
                }
            } else if (username.equals("none")) {
                log.warn("Skipping security");
            } else {
                int port = Config.getInstance().getMyProxyPort();
                int index = hostname.indexOf(':');
                if (index != -1) {
                    port = Integer.parseInt(hostname.substring(index + 1));
                    hostname = hostname.substring(0, index);
                }
                MyProxyInit myproxyInit = new MyProxyInit(username, password, hostname, port, null, Config.getInstance().getMyProxyLocalLifetime() * 60, Config.getInstance().getMyProxyRemoteLifetime() * 60);
                log.info("Getting Myproxy for user " + username);
                try {
                    proxyContent = new String(myproxyInit.doGet(true));
                } catch (MyProxyException mpe) {
                    log.error(mpe.getMessage(), mpe);
                    throw new RemoteException("Cannot receive proxy for " + username + "@" + hostname + "\n" + mpe.getMessage());
                } catch (GSSException gse) {
                    log.error(gse.getMessage(), gse);
                    throw new RemoteException("Cannot receive proxy for " + username + "@" + hostname + "\n" + gse.getMessage());
                }
                log.info("Myproxy received for user " + username);
            }
        }
        BinaryMessage result = new BinaryMessage(1);
        if (username.equals("none")) {
            session.setAttribute("User-Information", "none");
            result.addString("forever");
        } else {
            GSIUtils gsi = new GSIUtils(proxyContent.getBytes());
            log.debug("Myproxy status for user is " + username + " " + gsi.getProxyStatus());
            if (gsi.getProxyStatus() == false) {
                throw new RemoteException(gsi.getProxyStatusMessage());
            }
            session.setAttribute("User-Information", gsi.getProxyContent());
            session.setMaxInactiveInterval((int) (gsi.getExpirationTime() / 1000l));
            result.addString(Long.toString(gsi.getExpirationTime()));
        }
        return result;
    }

    /**
     * Dispatches new job
     * 
     * @param query
     *            contains information needed to create real
     *            {@link WorkDescription}
     *            <ol>
     *            <li>jobID1/jobID2</li>
     *            <li>name of the job</li>
     *            <li>URL for the service</li>
     *            <li>URL for the jar file, using jar-scheme</li>
     *            <li>classname</li>
     *            <li>method name</li>
     *            <li>number of subdescriptions
     *            <li>number of parameters
     *            <li>number of flag/value pairs
     *            <li>subdescription methodnames</li>
     *            <li>all parameters
     *            <li>flag key and values
     *            <li>the rest of items are attachment file name and data -pairs</li>
     *            </ol>
     * @param proxy
     *            current proxy file
     * @return empty message
     * @throws RemoteException
     *             if could not dispatch the job
     */
    private BinaryMessage dispatch(BinaryMessage query, ComputingInterface server, GSIUtils proxy) throws RemoteException {
        log.debug("Dispathing new job");
        WorkDescription wds = new WorkDescription();
        wds.getInfo().setOwner(proxy.getUserDN());
        wds.getInfo().setPrivacy(JobInfo.OPEN_FOR_ALL);
        String[] ids = query.readString().split("/");
        Long[] jobID = new Long[ids.length];
        for (int i = 0; i < ids.length; i++) jobID[i] = new Long(ids[i]);
        wds.setJobID(jobID);
        wds.getInfo().setJobName(query.readString());
        String serviceURL = query.readString();
        if (serviceURL.length() > 0) wds.setServiceURL(serviceURL); else wds.setServiceURL(DiscoveryService.getInstance().getLocalService());
        FileUtils.createDir(Config.getWorkingDir(jobID[0]));
        try {
            String jarFile = query.readString();
            if (jarFile.length() > 0) {
                URL jarURL = new URL(jarFile);
                URL targetURL = FileUtils.copyFile(jarURL, Config.getWorkingDir(jobID[0]));
                wds.attachFile(new URL("jar:" + targetURL.toString() + "!/"));
            }
        } catch (IOException mue) {
            throw new RemoteException("Cannot load the agent JAR file: " + mue.getMessage());
        }
        wds.getExecutable().setClassName(query.readString());
        wds.getExecutable().setMethodName(query.readString());
        int subCount = Integer.parseInt(query.readString());
        int parameterCount = Integer.parseInt(query.readString());
        int flagPairCount = Integer.parseInt(query.readString());
        if (subCount > 0) {
            for (int i = 0; i < subCount; i++) {
                WorkDescription subWds = new WorkDescription();
                String methodName = query.readString();
                if (methodName.length() == 0) methodName = null;
                subWds.getExecutable().setMethodName(methodName);
                wds.addChildren(subWds);
            }
        }
        String[] jobParameters = new String[parameterCount];
        for (int i = 0; i < parameterCount; i++) {
            jobParameters[i] = query.readString();
        }
        wds.getExecutable().putParameters(jobParameters);
        Properties jobFlags = new Properties();
        for (int i = 0; i < flagPairCount; i++) {
            String key = query.readString();
            String value = query.readString();
            jobFlags.setProperty(key, value);
        }
        wds.flags(jobFlags);
        while (query.hasNext()) {
            String name = Config.getWorkingDir(jobID[0]) + "/" + query.readString();
            byte[] data = query.readBinary();
            FileUtils.writeFile(name, data);
            try {
                wds.attachFile(new URL("file:" + name));
            } catch (IOException ioe) {
                throw new RemoteException(ioe.getMessage());
            }
        }
        log.debug("WDS " + wds.toString());
        server.dispatch(wds);
        log.debug("job dispatched " + wds.getJobID().toString());
        return new BinaryMessage(0);
    }

    /**
     * @param query
     *            contains only jobID of the job
     * @param service
     *            server implementation
     * @return description with following elements of {@link WorkDescription}
     *            <ol>
     *            <li>jobID</li>
     *            <li>name of the job</li>
     *            <li>URL of the service
     *            <li>URL for the agent jar</li>
     *            <li>class name of the agent</li>
     *            <li>method name</li>
     *            <li>number of subelements</li>
     *            <li>number of parameters
     *            <li>number of flag/value pairs
     *            <li>subelement method name</li>
     *            <li>all parameters
     *            <li>flag key and values
     *            </ol>
     * @throws RemoteException
     *             if cannot receive the status information
     * @see fi.hip.gb.net.ComputingInterface#getDescription(java.lang.Long)
     */
    private BinaryMessage getDescription(BinaryMessage query, ComputingInterface server) throws RemoteException {
        Long jobID = Long.valueOf(query.readString());
        WorkDescription wds = server.getDescription(jobID);
        JobExecutable exec = wds.getExecutable();
        BinaryMessage results = new BinaryMessage(9 + wds.children().size() + exec.parameters().length + wds.flags().size() * 2);
        results.addString(wds.currentID().toString());
        results.addString(wds.getInfo().getJobName());
        results.addString(wds.getServiceURL());
        results.addString(wds.jarFiles()[0].toString());
        results.addString(exec.getClassName());
        results.addString(exec.getMethodName());
        results.addString(Integer.toString(wds.children().size()));
        results.addString(Integer.toString(exec.parameters().length));
        results.addString(Integer.toString(wds.flags().size()));
        for (Iterator<Long[]> i = wds.children().keySet().iterator(); i.hasNext(); ) {
            Long[] subID = i.next();
            log.debug("adding sub " + Arrays.toString(subID));
            results.addString(wds.children().get(subID).getExecutable().getMethodName());
        }
        for (Object param : exec.parameters()) {
            log.debug("adding param " + param);
            results.addString(param.toString());
        }
        for (Object flag : wds.flags().keySet()) {
            log.debug("adding flag " + flag + "=" + wds.flags().getProperty((String) flag));
            results.addString((String) flag);
            results.addString(wds.flags().getProperty((String) flag, ""));
        }
        return results;
    }

    /**
     * Gets status of the job
     * 
     * @param query
     *            contains only jobID of the job
     * @param proxy
     *            current proxy file
     * @return status with following elements
     *         <ol>
     *         <li>state - from WorkStatus.*_STATE</li>
     *         <li>serviceURL</li>
     *         <li>start time</li>
     *         <li>end time</li>
     *         <li>current execution value</li>
     *         <li>maximum value of execution</li>
     *         <li>current transfer status</li>
     *         <li>maximum data to be transfered</li>
     *         <li>error string</li>
     *         </ol>
     * @throws RemoteException
     *             if cannot receive the status information
     */
    private BinaryMessage getStatus(BinaryMessage query, ComputingInterface server) throws RemoteException {
        Long jobID = Long.valueOf(query.readString());
        WorkStatus status = server.getStatus(jobID);
        BinaryMessage result = new BinaryMessage(9);
        result.addString(Integer.toString(status.getState()));
        result.addString(status.getServiceURL());
        result.addString((status.getStartTime() != null) ? TextUtils.getDateFormat(status.getStartTime()) : "");
        result.addString((status.getEndTime() != null) ? TextUtils.getDateFormat(status.getEndTime()) : "");
        result.addString(Integer.toString(status.getExecModel()[WorkStatus.VALUE]));
        result.addString(Integer.toString(status.getExecModel()[WorkStatus.MAXIMUM]));
        result.addString(Integer.toString(status.getTransferModel()[WorkStatus.VALUE]));
        result.addString(Integer.toString(status.getTransferModel()[WorkStatus.MAXIMUM]));
        result.addString(status.getError() != null ? status.getError() : "");
        return result;
    }

    /**
     * Gets results for wanted result names
     * 
     * @param query
     *            contains job ID, boolean saying if result payloads should be downloaded 
     * 				and rest fields are result names to be retrieved
     * @param proxy
     *            current proxy file
     * @return wanted results. There are always following items on one result:
     * <ul>
     * <li>filename
     * <li>classname of the result object
     * <li>size of the result file, not the bytes transfered
     * <li>actual data payload
     * </ul>
     * These sets are multiplied as many times as there are results.
     * @throws RemoteException
     *             if failed to receive results
     */
    private BinaryMessage getResult(BinaryMessage query, ComputingInterface server) throws RemoteException {
        Long jobID = Long.valueOf(query.readString());
        Boolean includePayload = Boolean.valueOf(query.readString());
        String[] names = new String[query.remainingItems()];
        for (int i = 0; i < names.length; i++) {
            names[i] = query.readString();
        }
        WorkResult wr = server.getResult(jobID, names, true);
        int counter = 0;
        for (Iterator<JobAttachment> er = wr.results(); er.hasNext(); ) {
            er.next();
            counter += 4;
        }
        BinaryMessage results = new BinaryMessage(counter);
        for (Iterator<JobAttachment> er = wr.results(); er.hasNext(); ) {
            JobAttachment res = er.next();
            byte[] bytes = new byte[0];
            if (res.getFile() != null && (includePayload || res.fileName().endsWith(".txt"))) {
                bytes = res.readBytes();
            }
            results.addString(res.fileName());
            results.addString(res.getType() != null ? res.getType() : "");
            results.addString(Long.toString(res.getSize()));
            results.addBinary(bytes);
        }
        return results;
    }

    /**
     * Gets list of jobs
     * 
     * @param query
     *            containing following items
     *            <ol>
     *            <li>publics, list all jobs, not only private jobs</li>
     * 			  <li>agentClassFilter, filter classnames of the agentr</li>
     *            </ul>
     * @param proxy
     *            current proxy file
     * @return item for every session with syntax of jobID#jobName
     * @throws RemoteException
     *             if failed to get job list
     */
    private BinaryMessage getJoblist(BinaryMessage query, ComputingInterface server) throws RemoteException {
        Boolean publics = Boolean.valueOf(query.readString());
        String classFilter = query.readString();
        Long[] sessions = server.getJoblist(publics, classFilter);
        BinaryMessage results = new BinaryMessage(sessions.length);
        for (int i = 0; i < sessions.length; i++) {
            WorkDescription wds = SessionHandler.getInstance().getSession(sessions[i]).getDescription();
            results.addString(sessions[i].toString() + "#" + wds.getInfo().getJobName());
        }
        return results;
    }

    /**
     * Gets the list of available servers
     * 
     * @return an array of service URLs
     * @throws RemoteException
     *             if failed to get job list
     */
    private BinaryMessage listServices() throws RemoteException {
        DiscoveryPacket[] services = new DiscoveryService().listServices();
        BinaryMessage results = new BinaryMessage(services.length);
        for (int i = 0; i < services.length; i++) {
            results.addString(services[i].getServiceURL());
        }
        return results;
    }

    /**
     * Aborts/removes the job(s)
     * 
     * @param query
     *            containing following items
     *            <ol>
     *            <li>jobID for the job, -1 if all jobs should be removed</li>
     *            <li>remove, should job be just aborted or removed too</li>
     *            </ul>
     * @param proxy
     *            current proxy file
     * @return empty message
     * @throws RemoteException
     *             if something went wrong
     */
    private BinaryMessage abort(BinaryMessage query, ComputingInterface server) throws RemoteException {
        Long jobID = Long.valueOf(query.readString());
        Integer operation = Boolean.valueOf(query.readString()).booleanValue() ? ComputingInterface.ABORT_AND_REMOVE : ComputingInterface.ABORT_JOB;
        server.abort(jobID, operation);
        BinaryMessage res = new BinaryMessage(0);
        return res;
    }
}
