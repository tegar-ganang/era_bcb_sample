package com.bitgate.util.services.engine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import org.w3c.dom.Node;
import com.bitgate.server.Server;
import com.bitgate.util.base64.Base64;
import com.bitgate.util.db.DBPool;
import com.bitgate.util.db.Pool;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.nntp.NNTPPool;
import com.bitgate.util.nntp.NNTPResult;
import com.bitgate.util.services.ClientContext;
import com.bitgate.util.services.ReservedVars;
import com.bitgate.util.services.SocketChannelWrapper;
import com.bitgate.util.services.VendContext;
import com.bitgate.util.services.WorkerContext;

/**
 * This is the class that performs the hard work of the document rendering.  It is the engine class that handles the processing
 * of elements, and storage of variable information for each element.  It also stores state information about a connected
 * client, and database information for the currently rendered document.
 * <p/>
 * Variables in use in the rendered objects are used in a <code>VariableContainer</code> object, which contains all of the
 * valid variables, and types of data they contain.  This code simply references a currently active object of that type.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/services/engine/RenderEngine.java#121 $
 */
public class RenderEngine {

    /** This is the data contained inside the current instance. */
    public StringBuffer data;

    /** List of query containers loaded for this document rendering instance. */
    public HashMap containerSets;

    /** List of Node Sets loaded for this document rendering instance. */
    public HashMap nodeSets;

    /** List of this session's currently assigned NNTP Pools. */
    public HashMap nntpPools;

    /** List of the available stacks for rendering instance. */
    public HashMap stackSets;

    /** List of the available LinkedLists for rendering instance. */
    public HashMap listSets;

    /** This is a list of soap output variables. */
    public ArrayList soapVariables;

    /** Flag indicating whether or not this rendered document requires authentication. */
    public boolean requiresAuthentication;

    /** Flag indicating if the rendered document has been authenticated. */
    public boolean authenticated;

    /** Flag indicating a break state during the render. */
    public boolean breakState;

    /** Message associated with a break state that occurred. */
    public String breakMessage;

    /** Flag indicating an exception state during the render. */
    public boolean exceptionState;

    /** Flag indicating whether or not rendering is bypassed. */
    public boolean bypassRendering;

    /** Exception message that happened. */
    public String exceptionMessage;

    /** Flag indicating if this document was activated by SOAP Action. */
    public boolean soapAction;

    /** Flag indicating a loop stop state during the render. */
    public boolean stopState;

    public static final boolean RENDER_BUFFERED = true;

    public static final boolean RENDER_UNBUFFERED = false;

    private HashMap cookies;

    private WorkerContext ww;

    private String currentProcedureLevel;

    private String currentClassName;

    private boolean buffered;

    private boolean soapBypassAction;

    private boolean echo;

    private boolean isCached;

    private boolean isConnected;

    private boolean isConnectedBypass;

    private HashMap activePools, activeLocalPools;

    private ArrayList serverPools;

    private ByteArrayOutputStream baos;

    private PrintStream ps;

    private VariableContainer vContainer;

    private DocumentEngine documentEngine;

    private RenderContext rendContext;

    private SocketChannelWrapper sChannel;

    /**
     * Instantiates a new Rendering Engine system, storing the current Services Worker instance.
     *
     * @param ww The currently active Services Worker instance.
     */
    public RenderEngine(WorkerContext ww) {
        data = new StringBuffer();
        nodeSets = new HashMap();
        containerSets = new HashMap();
        cookies = new HashMap();
        nntpPools = new HashMap();
        activePools = new HashMap();
        activeLocalPools = new HashMap();
        stackSets = new HashMap();
        listSets = new HashMap();
        serverPools = new ArrayList();
        soapVariables = new ArrayList();
        vContainer = new VariableContainer(ww);
        requiresAuthentication = false;
        authenticated = false;
        breakState = false;
        isConnected = true;
        isConnectedBypass = true;
        breakMessage = null;
        exceptionState = false;
        stopState = false;
        bypassRendering = false;
        soapAction = false;
        soapBypassAction = false;
        isCached = false;
        exceptionMessage = null;
        currentProcedureLevel = null;
        currentClassName = null;
        sChannel = null;
        rendContext = new RenderContext();
        this.baos = new ByteArrayOutputStream();
        this.ps = new PrintStream(this.baos);
        buffered = RENDER_BUFFERED;
        echo = true;
        this.ww = ww;
        rendContext.setContentType("text/html");
    }

    /**
     * If you want to change the render output mode, use this function, and combine the usage of <code>RENDER_BUFFERED</code>
     * and <code>RENDER_UNBUFFERED</code> as required here.
     *
     * @param mode The buffering mode to use.
     */
    public void setBufferMode(boolean mode) {
        buffered = mode;
    }

    /**
     * This assigns the current <code>DocumentEngine</code> object.
     *
     * @param docEngine The <code>DocumentEngine</code> object.
     */
    public void setDocumentEngine(DocumentEngine docEngine) {
        this.documentEngine = docEngine;
    }

    /**
     * Returns the currently assigned <code>DocumentEngine</code> object.
     *
     * @return <code>DocumentEngine</code> object.
     */
    public DocumentEngine getDocumentEngine() {
        return this.documentEngine;
    }

    /**
     * If you wish to retrieve the current <code>ByteArrayOutputStream</code>, use this.
     *
     * @return <code>ByteArrayOutputStream</code> currently assigned.
     */
    public ByteArrayOutputStream getByteOutputStream() {
        return this.baos;
    }

    /**
     * If you wish to retrieve the current <code>PrintStream</code>, use this.
     *
     * @return <code>PrintStream</code> currently assigned.
     */
    public PrintStream getPrintStream() {
        return this.ps;
    }

    /**
     * Returns the current RenderContext object.
     *
     * @return <code>RenderContext</code> object.
     */
    public RenderContext getRenderContext() {
        return rendContext;
    }

    /**
     * Sets the socket channel object.
     *
     * @param sChannel The socket channel to set.
     */
    public void setSocketChannel(SocketChannelWrapper sChannel) {
        this.sChannel = sChannel;
    }

    /**
     * This function allows you to pre-load variables into the Rendering Engine before the rendering takes place.
     *
     * @param vars A HashMap containing the variables to pre-load.
     */
    public void preloadVariables(HashMap vars) {
        Iterator itVars = vars.keySet().iterator();
        while (itVars.hasNext()) {
            String key = (String) itVars.next();
            if (vars.get(key) instanceof Vector) {
                vContainer.setVector(key, (Vector) vars.get(key));
            } else {
                try {
                    String value = (String) vars.get(key);
                } catch (ClassCastException e) {
                }
            }
        }
        itVars = null;
    }

    /**
     * Preloads an entire VariableContainer object.
     *
     * @param container The variable container object.
     */
    public void preloadVariables(VariableContainer container) {
        this.vContainer = container;
    }

    /**
     * This function retrieves the current buffer mode.
     *
     * @return <code>RENDER_BUFFERED</code> if the output is buffered, <code>RENDER_UNBUFFERED</code> otherwise.
     */
    public boolean getBufferMode() {
        return buffered;
    }

    /**
     * This function enables or disables direct rendering of TEXT/CDATA elements to the rendering system (if unbuffered
     * text is selected).
     *
     * @param echo <code>true</code> to echo to the rendering system, <code>false</code> otherwise.
     */
    public void setEchoMode(boolean mode) {
        echo = mode;
    }

    /**
     * This returns the current echo mode.
     *
     * @return <code>true</code> if echo is turned on, <code>false</code> otherwise.
     */
    public boolean getEchoMode() {
        return echo;
    }

    /**
     * This function loads in post variables from a previous instance, and tacks on the word "POST_" to the beginning of
     * each posted variable.  This is used to strictly indicate which variables were loaded in with a "POST" request, and
     * which ones were auto-loaded by the rendering system (or underlying rendering engine.)
     *
     * @param varlist The HashMap containing the posted variables.
     */
    public void loadPostVariables(HashMap varlist) {
        if (varlist == null) {
            return;
        }
        Iterator elements = varlist.keySet().iterator();
        while (elements.hasNext()) {
            String key = (String) elements.next();
            if (varlist.get(key) instanceof Vector) {
                vContainer.setVector(key, (Vector) varlist.get(key));
                Debug.debug("Setting variable '" + key + "' with " + ((Vector) varlist.get(key)).size() + " vector entry(s).");
            } else {
                String val = (String) varlist.get(key);
                String newKey = key;
                if (val != null && val.length() > 40) {
                    Debug.debug("Setting variable '" + newKey + "' to val '(suppressed)'");
                } else {
                    Debug.debug("Setting variable '" + newKey + "' to val '" + val + "'");
                }
                vContainer.setVariable(newKey, val);
            }
        }
        elements = null;
    }

    /**
     * This function loads in file variables from a previous instance.
     *
     * @param varlist The HashMap containing the file list variables.
     */
    public void loadFileVariables(HashMap varlist) {
        if (varlist == null) {
            return;
        }
        Iterator elements = varlist.keySet().iterator();
        while (elements.hasNext()) {
            String key = (String) elements.next();
            String val = (String) varlist.get(key);
            if (val != null && val.length() > 80) {
                Debug.debug("Setting file variable '" + key + "' to val '(suppressed)'");
            } else {
                Debug.debug("Setting file variable '" + key + "' to val '" + val + "'");
            }
            vContainer.setFileVariable(key, val);
        }
        elements = null;
    }

    /**
     * This allows the programmer to set the page to require authentication (or a tag, for that matter.)
     *
     * @param sw Flag indicating whether or not this page is to require authentication.
     */
    public void setAuthenticatedPage(boolean sw) {
        Debug.inform("Set authentication to " + sw);
        requiresAuthentication = sw;
    }

    /**
     * This function allows a programmer to set a break state.
     *
     * @param es Flag indicating the break state (true or false)
     * @param str The break string.
     */
    public void setBreakState(boolean es, String str) {
        breakState = es;
        breakMessage = str;
    }

    /**
     * This function allows a programmer to tell the rendering system to bypass all further rendering.
     *
     * @param es Flag indicating the bypass state.
     */
    public void setBypassRendering(boolean es) {
        bypassRendering = es;
    }

    /**
     * This function allows a programmer to set an exception state.
     *
     * @param es Flag indicating the exception state (true or false)
     * @param str The exception string.
     */
    public void setExceptionState(boolean es, String str) {
        exceptionState = es;
        if (exceptionMessage == null) {
            exceptionMessage = str;
        } else {
            exceptionMessage += "; nested exception: " + str;
        }
    }

    /**
     * This function clears an exception state.
     */
    public void clearExceptionState() {
        exceptionState = false;
        exceptionMessage = null;
    }

    /**
     * Sets the stop state.
     *
     * @param stop <code>true</code> to stop the loop, <code>false</code> otherwise.
     */
    public void setStopState(boolean stop) {
        stopState = stop;
    }

    /**
     * Returns the stop state.
     *
     * @return <code>true</code> if in a stop state, <code>false</code> otherwise.
     */
    public boolean isStopState() {
        return stopState;
    }

    /**
     * This function adds a variable name to the list of output variables.
     *
     * @param var A variable name to add.
     */
    public void addSoapOutputVariable(String var) {
        soapVariables.add(var);
    }

    /**
     * Clears the stop state.
     */
    public void clearStopState() {
        setStopState(false);
    }

    /**
     * This function checks authentication against the supplied user and pass.  If the supplied user/pass matches
     * the "Authorization" field in the headers specified by the currently active Worker, authentication is granted.
     *
     * @param user Username to authenticate.
     * @param pass Password to authenticate.
     */
    public void checkAuthentication(String user, String pass) {
        if (authenticated == false) {
            if (ww.getClientContext().getRequestHeader("authorization") != null) {
                String authLine = ww.getClientContext().getRequestHeader("authorization");
                String authCompareLine = Base64.encode(user + ":" + pass);
                int authLineSpace = authLine.lastIndexOf(" ");
                if (authLineSpace != -1) {
                    authLine = authLine.substring(authLineSpace + 1);
                }
                if (authCompareLine.equalsIgnoreCase(authLine)) {
                    authenticated = true;
                    vContainer.setVariable("AUTHENTICATED_USER", user);
                    vContainer.setVariable("AUTHENTICATED_PASS", pass);
                }
            }
        }
        if (authenticated) {
            Debug.debug("Authentication for user '" + user + "' successful.");
        } else {
            Debug.debug("Authentication for user '" + user + "' failed.");
        }
    }

    /**
     * Returns a flag indicating whether or not the page was successfully authenticated.
     *
     * @return boolean Flag indicating whether or not authentication was successful.
     */
    public boolean isAuthenticated() {
        if (requiresAuthentication == false) {
            return true;
        } else if (requiresAuthentication == true) {
            if (authenticated == true) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether or not a render is completely cancelled.
     *
     * @return <code>true</code> if the render is cancelled, <code>false</code> otherwise.
     */
    public boolean isCancelled() {
        if (!Server.getCancellable()) {
            return false;
        }
        if (isConnectedBypass) {
            return false;
        }
        if (!isConnected) {
            Debug.debug("Socket disconnected; aborting render.");
            return true;
        }
        if (ww != null && ww.getClientContext() != null) {
            if (ww.getClientContext().getSocketChannel() != null) {
                if (!ww.getClientContext().getSocketChannel().isConnected()) {
                    isConnected = false;
                    Debug.debug("Socket disconnected; aborting render.");
                    return true;
                }
            } else {
                isConnectedBypass = true;
            }
        } else {
            isConnectedBypass = true;
        }
        return false;
    }

    /**
     * Returns a flag indicating whether or not break or exception state is set.  A break state can be indicated using
     * a true break state, an exception, a break of a loop, or a bypassing of rendering.  Any of these states will
     * make the break state <code>true</code>.
     *
     * @return boolean Flag indicating whether or not a break or exception state was set.
     */
    public boolean isBreakState() {
        return (breakState || exceptionState || stopState || bypassRendering);
    }

    /**
     * Returns a flag indicating whether or not rendering is bypassed.
     *
     * @return boolean Flag indicating whether or not a rendering bypass state was set.
     */
    public boolean isBypassRendering() {
        return bypassRendering;
    }

    /**
     * Returns the break message if a break occurred.
     *
     * @return String containing the break message.
     */
    public String getBreakMessage() {
        return breakMessage;
    }

    /**
     * Sets whether or not a document's rendered output is to be cached.
     *
     * @param cache Boolean value indicating caching state.
     */
    public void setCached(boolean cached) {
        isCached = cached;
    }

    /**
     * Returns whether or not a document's rendered output is to be cached.
     *
     * @return <code>true</code> if the document is cached, <code>false</code> otherwise.
     */
    public boolean isCached() {
        return isCached;
    }

    /**
     * Returns the current exception state.
     *
     * @return <code>true</code> if an exception occurred, <code>false</code> otherwise.
     */
    public boolean isExceptionState() {
        return exceptionState;
    }

    /**
     * Returns the currently active pool for the name specified.  If no pool was available at the time, it will create one,
     * and cache it.
     *
     * @param name The pool name to look up.
     * @return <code>Connection</code> object for the database connection, or <code>null</code> if unavailable.
     */
    public Connection getPool(String name) {
        if (activePools.get(name) != null) {
            Connection conn = (Connection) activePools.get(name);
            boolean connected = false;
            try {
                connected = !conn.isClosed();
            } catch (Exception e) {
                Debug.debug("Could not determine if connection for pool '" + name + "' is open: " + e);
            }
            if (connected) {
                Debug.debug("Retrieval of pool '" + name + "' successful.");
                return (Connection) activePools.get(name);
            } else {
                Debug.debug("Retrieval of pool '" + name + "' is closed.  Falling through to grab another.");
            }
        }
        Pool dbPool = DBPool.getDefault().getPool(name);
        if (dbPool != null) {
            Connection conn = null;
            for (int retries = 0; retries < 10; retries++) {
                conn = DBPool.getDefault().getPool(name).getNextConnection();
                boolean connected = false;
                try {
                    connected = !conn.isClosed();
                } catch (Exception e) {
                    Debug.debug("Could not determine if connection for pool '" + name + "' is open: " + e);
                }
                if (connected) {
                    try {
                        conn.setAutoCommit(false);
                    } catch (SQLException e) {
                        return null;
                    }
                    activePools.put(name, conn);
                } else {
                    Debug.debug("Connection closed for pool '" + name + "' after " + retries + " try(s).");
                    conn = null;
                }
            }
            if (conn != null) {
                Debug.debug("Retrieved pool '" + name + "'");
            } else {
                Debug.debug("Unable to retrieve pool after 10 tries for pool '" + name + "'");
            }
            return conn;
        }
        return null;
    }

    /**
     * Entry function to add a local pool that has been newly created.
     *
     * @param name The name of the pool.
     * @param conn The <code>Connection</code> object assigned to the pool.
     */
    public void addLocalPool(String name, Connection conn) {
        activeLocalPools.put(name, conn);
        Debug.debug("Local pool added: Name='" + name + "'");
    }

    /**
     * Returns the currently active local pool for the name specified.  If no pool was available at the time, it will create one,
     * and cache it.
     *
     * @param name The pool name to look up.
     * @return <code>Connection</code> object for the database connection, or <code>null</code> if unavailable.
     */
    public Connection getLocalPool(String name) {
        if (activeLocalPools.get(name) != null) {
            Connection conn = (Connection) activeLocalPools.get(name);
            boolean connected = false;
            try {
                connected = !conn.isClosed();
            } catch (Exception e) {
                Debug.debug("Could not determine if connection for pool '" + name + "' is open: " + e);
            }
            if (connected) {
                Debug.debug("Retrieved local pool '" + name + "'");
                return conn;
            } else {
                Debug.debug("Retrieval of local pool '" + name + "' is closed.  Falling through to grab another.");
            }
        }
        Pool dbPool = DBPool.getDefault().getPool(name);
        if (dbPool != null) {
            Connection conn = null;
            for (int retries = 0; retries < 10; retries++) {
                conn = DBPool.getDefault().getPool(name).getNextConnection();
                boolean connected = false;
                try {
                    connected = !conn.isClosed();
                } catch (Exception e) {
                    Debug.debug("Could not determine if connection for local pool '" + name + "' is open: " + e);
                }
                if (connected) {
                    activeLocalPools.put(name, conn);
                    break;
                } else {
                    Debug.debug("Connection closed for local pool '" + name + "' after " + retries + " try(s).");
                    conn = null;
                }
            }
            if (conn != null) {
                Debug.debug("Retrieved local pool '" + name + "'");
            } else {
                Debug.debug("Unable to retrieve local pool after 10 tries for pool '" + name + "'");
            }
            return conn;
        }
        return null;
    }

    /**
     * Deletes the local pool and closes all active connections to said pool.
     *
     * @param name The pool name to look up.
     */
    public void closeLocalPool(String name) {
        if (activeLocalPools.get(name) != null) {
            Connection conn = (Connection) activeLocalPools.get(name);
            boolean connected = false;
            try {
                connected = !conn.isClosed();
            } catch (Exception e) {
                Debug.debug("Could not determine if connection for pool '" + name + "' is open: " + e);
            }
            if (connected) {
                Debug.debug("Retrieved local pool '" + name + "'");
            }
        }
        DBPool.getDefault().closePool(name);
    }

    /**
     * Stops the local pool and closes all active connections to said pool.
     *
     * @param name The pool name to look up.
     */
    public void stopLocalPool(String name) {
        if (activeLocalPools.get(name) != null) {
            Connection conn = (Connection) activeLocalPools.get(name);
            boolean connected = false;
            try {
                connected = !conn.isClosed();
            } catch (Exception e) {
                Debug.debug("Could not determine if connection for pool '" + name + "' is open: " + e);
            }
            if (connected) {
                Debug.debug("Retrieved local pool '" + name + "'");
            }
        }
        DBPool.getDefault().closePool(name);
        DBPool.getDefault().stopPool(name);
    }

    /**
     * Returns the current exception message if an exception occurred.
     *
     * @return String containing the exception message.
     */
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    /**
     * This sets the indicator telling the rendering system that the document just rendered contained a SOAPAction client header.
     *
     * @param soap boolean containing whether or not the rendered document contained SOAPAction.
     */
    public void setSoapAction(boolean soap) {
        soapAction = soap;
    }

    /**
     * This returns whether or not the soap action was seen.
     *
     * @return <code>true</code> if a soap action was set, <code>false</code> otherwise.
     */
    public boolean getSoapAction() {
        return soapAction;
    }

    /**
     * This function is used for the &lt;SET&gt; element, which tells it that a soap action is present, but not
     * the standard SOAP request.
     *
     * @param soap boolean containing whether or not the rendered document contained a POST_SoapAction.
     */
    public void setSoapBypass(boolean soap) {
        soapBypassAction = soap;
    }

    /**
     * This returns whether or not the soap bypassing action was seen.
     *
     * @return <code>true</code> if a soap action was set, <code>false</code> otherwise.
     */
    public boolean getSoapBypass() {
        return soapBypassAction;
    }

    /**
     * Returns whether or not an element in the namespace specified can be rendered.
     *
     * @param namespace The namespace to check.
     * @return <code>true</code> if the namespace can be rendered, <code>false</code> otherwise.
     */
    public boolean canRender(String namespace) {
        if (ww == null || ww.getVendContext() == null || ww.getVendContext().getVend() == null) {
            return true;
        }
        return ww.canRender(namespace);
    }

    /**
     * Sets the current procedure scope for u:procedure.
     *
     * @param scope The scope to set.
     */
    public void setScope(String scope) {
        currentProcedureLevel = scope;
    }

    /**
     * Gets the current procedure scope for u:procedure.
     *
     * @return <code>String</code> containing the current procedure level, <code>null</code> if none was set.
     */
    public String getScope() {
        return currentProcedureLevel;
    }

    /**
     * This function allows a programmer to add text to the internal data of this tag.
     * 
     * @param text The text to add.
     */
    public void write(String text) {
        if (text == null) {
            return;
        }
        data.append(text);
    }

    /**
     * This function allows a programmer to add text to the internal data of this tag.
     * 
     * @param text The text to add as a StringBuffer.
     */
    public void write(StringBuffer text) {
        if (text == null) {
            return;
        }
        data.append(text);
    }

    /**
     * Retrieves a variable from the current container.
     *
     * @param var Variable name.
     * @return <code>String</code> containing the variable data.
     */
    public String getVariable(String var) {
        Object obj = vContainer.getVariable(var);
        if (obj == null) {
            return "";
        }
        return (String) obj;
    }

    /**
     * This function allows you to retrieve a variable passed from the client at the time of a request.
     *
     * @param key The header variable name to grab.
     * @return String containing the value of that key.
     */
    public String getClientHeader(String var) {
        if (ww != null) {
            String val = null;
            if (ww.getClientContext().getRequestHeader(var) != null) {
                val = ww.getClientContext().getRequestHeader(var);
            } else {
                val = new String();
            }
            return val;
        } else {
            return new String();
        }
    }

    /**
     * This function allows a programmer or tag to add an XML node set by variable.
     *
     * @param varname The variable name to assign the data to.
     * @param node The Node object to add.
     */
    public void addNodeSet(String varname, Node node) {
        nodeSets.put(varname.toLowerCase(), node);
    }

    /**
     * This function returns the current Node set based on the variable specified.
     *
     * @param varname The variable name to look up.
     * @return Node containing the XML data.
     */
    public Node getNodeSet(String varname) {
        if (varname == null || varname.equals("")) {
            return null;
        }
        if (varname.equalsIgnoreCase("engine_config")) {
            return Server.getConfig();
        }
        if (nodeSets.get(varname.toLowerCase()) != null) {
            return (Node) nodeSets.get(varname.toLowerCase());
        }
        return null;
    }

    /**
     * This function adds an available pool to the list.
     *
     * @param poolname A poolname to add.
     */
    public void setAvailablePool(String poolname) {
        serverPools.add(poolname);
    }

    /**
     * This function checks to see if an available pool is part of the list.
     *
     * @param poolname The poolname to check for.
     * @return <code>true</code> if the pool specified can be accessed, <code>false</code> otherwise.
     */
    public boolean getAvailablePool(String poolname) {
        if (serverPools == null) {
            return false;
        }
        int serverPoolsSize = serverPools.size();
        for (int i = 0; i < serverPoolsSize; i++) {
            if (((String) serverPools.get(i)).equals(poolname)) {
                return true;
            }
            if (((String) serverPools.get(i)).equals("*")) {
                return true;
            }
        }
        return false;
    }

    /**
     * This function returns the currently active Services Worker instance.
     *
     * @return Worker object.
     */
    public WorkerContext getWorkerContext() {
        return ww;
    }

    /**
     * Retrieves the current Client Context.
     *
     * @return <code>ClientContext</code> object.
     */
    public ClientContext getClientContext() {
        if (ww != null) {
            return ww.getClientContext();
        }
        SocketChannelWrapper wrapperObject = new SocketChannelWrapper();
        wrapperObject.setStandardSocket(new Socket().getChannel());
        return new ClientContext(wrapperObject);
    }

    /**
     * Retrieves the current Vend Context.
     *
     * @return <code>VendContext</code> object.
     */
    public VendContext getVendContext() {
        if (ww != null) {
            return ww.getVendContext();
        }
        Debug.debug("No worker context available, so no vend context is available.");
        return null;
    }

    /**
     * This function sets a cookie, with the given key and value.
     *
     * @param key The cookie name.
     * @param value The value of the cookie.
     */
    public void addCookie(String key, String value) {
        if (ww != null) {
            ww.getClientContext().setCookie(key, value);
        } else {
            cookies.put(key, Encoder.URLEncode(value));
        }
    }

    /**
     * This function retrieves a cookie.
     *
     * @param key The cookie name to look up.
     * @return String containing the value of that cookie.
     */
    public String getCookie(String key) {
        if (ww != null) {
            Debug.debug("Cookie value of key '" + key + "' value '" + ww.getClientContext().getCookie(key) + "'");
            return ww.getClientContext().getCookie(key);
        } else {
            Debug.debug("Cookie value of key '" + key + "' value '" + (String) cookies.get(key) + "'");
            return Encoder.URLDecode((String) cookies.get(key));
        }
    }

    /**
     * This function returns an NNTP Pool array entry.
     *
     * @param key The pool name to look up.
     * @return NNTPResult object if successful, or <code>null</code> otherwise.
     */
    public NNTPResult getNNTPPool(String name) {
        if (nntpPools.get(name) != null) {
            return (NNTPResult) nntpPools.get(name);
        }
        NNTPResult obj = NNTPPool.getDefault().getPool(name);
        if (obj == null) {
            return null;
        }
        nntpPools.put(name, obj);
        return obj;
    }

    /**
     * This function clears out a cookie.  It deletes cookies if they are locally stored, and if there is an active
     * Worker object, it deletes the cookie that way.
     *
     * @param key The cookie name to delete.
     */
    public void deleteCookie(String key) {
        if (ww != null) {
            ww.getClientContext().setDeletedCookie(key);
        } else {
            cookies.remove(key);
        }
    }

    /**
     * Determines if a variable name specified is protected.
     *
     * @param varname The variable name to check for protection.
     * @return <code>true</code> if the variable is protected, <code>false</code> otherwise.
     */
    public boolean isProtectedVariable(String varname) {
        return ReservedVars.getDefault().getVariable(varname);
    }

    /**
     * Determines if a property name specified is protected.
     *
     * @param propname The property name to check for protection.
     * @return <code>true</code> if the variable is protected, <code>false</code> otherwise.
     */
    public boolean isProtectedProperty(String propname) {
        return ReservedVars.getDefault().getProperty(propname);
    }

    /**
     * Retrieves the currently active VariableContainer object.
     *
     * @return <code>VariableContainer</code> object.
     */
    public VariableContainer getVariableContainer() {
        return vContainer;
    }

    /**
     * Sets the name of the current class name.
     *
     * @param name The classname.
     */
    public void setClassName(String name) {
        this.currentClassName = name;
    }

    /**
     * Returns the currently assigned class name.
     *
     * @return <code>String</code> containing the currently assigned class name.
     */
    public String getClassName() {
        return currentClassName;
    }

    /**
     * This function cleans out all used variables, just in case they are stored in memory and not garbage collected.
     */
    public void clean() {
        vContainer.clean();
        data = null;
        nodeSets.clear();
        cookies.clear();
        nodeSets = null;
        cookies = null;
    }
}
