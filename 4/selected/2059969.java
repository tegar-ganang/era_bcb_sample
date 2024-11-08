package org.epo.gui.generic.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.epo.gui.generic.description.GenericContextKeys;
import org.epo.gui.generic.description.GenericDescKeys;
import org.epo.gui.generic.description.GenericParamKeys;
import org.epo.gui.generic.error.GenericGuiException;
import org.epo.gui.jphoenix.bean.JPhoenixHost;
import org.epo.gui.jphoenix.description.JPhoenixContextKeys;
import org.epo.gui.jphoenix.description.JPhoenixDescKeys;
import org.epo.gui.jphoenix.description.JPhoenixParamKeys;
import org.epo.gui.jphoenix.error.ErrorBean;
import org.epo.gui.jphoenix.error.InvalidSessionException;
import org.epo.gui.jphoenix.generic.ActionDesc;
import org.epo.gui.jphoenix.generic.ContextType;
import org.epo.gui.jphoenix.generic.GuiTrace;
import org.epo.gui.jphoenix.generic.UserContext;
import org.epo.gui.jphoenix.service.ProcessManager;

/**
 * General servlet enabling to send call RMI services from the jPhoenix servers
 * Creation date: 04/2001
 * @author: INFOTEL
 */
public class GenericGuiServlet extends HttpServlet {

    private static final String PROP_FILENAME = "gui.generic.config";

    public static final String JSP_HOST_CHOICE = "/generic_host_choice.jsp";

    public static final String JSP_HOST_CONFIG = "/generic_host_configuration.jsp";

    public static final String JSP_INFO_LIST = "/generic_info_list.jsp";

    public static final String JSP_TAB_INFO_LIST = "/generic_tab_info_list.jsp";

    public static final String JSP_EXPLORER = "/generic_explorer.jsp";

    public static final String JSP_TREE = "/generic_tree.jsp";

    private String GenericJspPath;

    private GuiTrace trace;

    private LinkedList actionList;

    /**
	 * GuiServlet constructor comment.
	 */
    public GenericGuiServlet() {
        super();
    }

    /**
	 * Answer to a GET HTTP request
	 * Creation date: 01/2001
	 * @param req javax.servlet.http.HttpServletRequest
	 * @param res javax.servlet.http.HttpServletResponse
	 * @exception javax.servlet.ServletException The exception description.
	 * @exception java.io.IOException The exception description.
	 */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        performTask(req, res);
    }

    /**
	 * Answer to a POST HTTP request
	 * Creation date: 01/2001
	 * @param req javax.servlet.http.HttpServletRequest
	 * @param res javax.servlet.http.HttpServletResponse
	 * @exception javax.servlet.ServletException The exception description.
	 * @exception java.io.IOException The exception description.
	 */
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        performTask(req, res);
    }

    /**
	 * Retrieve the parameters sent by the client
	 * Creation date: 01/2001
	 * @return java.util.Hashtable
	 * @param req javax.servlet.http.HttpServletRequest
	 */
    private Hashtable getParams(HttpServletRequest req) throws GenericGuiException, InvalidSessionException {
        trace(6, "--------------------- Begin getParams --------------------");
        String myParamName, myParamValue;
        Hashtable myHashtableParam = new Hashtable();
        for (Enumeration e = req.getParameterNames(); e.hasMoreElements(); ) {
            myParamName = (String) e.nextElement();
            myParamValue = req.getParameter(myParamName);
            trace(5, "Retrieved parameter and its value: " + myParamName + " " + myParamValue);
            myHashtableParam.put(myParamName, myParamValue);
        }
        trace(6, "---------------------- End getParams ---------------------");
        return myHashtableParam;
    }

    /**
	 * Initialization of the servlet
	 * Creation date: 01/2001
	 */
    public void init() throws ServletException {
        try {
            super.init();
            loadStoredParameters();
        } catch (GenericGuiException gge) {
            System.out.println("An exception has occured during the Generic GUI servlet initialization: " + gge.getMessage());
            throw new ServletException(gge.getMessage());
        }
    }

    /**
	 * Process of the HTTP requests
	 * Creation date: 04/2001
	 * @param theRequest  javax.servlet.http.HttpServletRequest
	 * @param theResponse javax.servlet.http.HttpServletResponse
	 */
    private void performTask(HttpServletRequest theRequest, HttpServletResponse theResponse) {
        trace(7, "[performTask] ##################### Generic GUI Servlet Begin ####################");
        UserContext myUserContext = null;
        ActionDesc myActionDesc = null;
        String myJSPName = null;
        boolean isRequestRedirected = false;
        try {
            getServletContext().setAttribute(JPhoenixContextKeys.GENERIC_SERVLET_URI, theRequest.getRequestURI());
            myUserContext = new UserContext(getServletContext(), theRequest, getTrace());
            myActionDesc = getActionDesc(myUserContext);
            if (myActionDesc != null) {
                String myActionCode = (String) myUserContext.get(JPhoenixParamKeys.ACTION);
                if (myActionCode != null) {
                    trace(7, "[performTask] Action Code: " + myActionCode, myUserContext);
                    ProcessManager myProcessManager = new ProcessManager(myUserContext);
                    if (myActionCode.equals(GenericContextKeys.BROWSE)) {
                        Object myReturnObject = myProcessManager.executeAction();
                        JPhoenixHost myHost = (JPhoenixHost) myUserContext.getAttribute(JPhoenixContextKeys.HOST_BEAN, ContextType.REQUEST);
                        int[] mySocketParam = (int[]) myReturnObject;
                        trace(7, "[performTask] InputStream retrieved", myUserContext);
                        String myParameter = (String) myUserContext.get(GenericParamKeys.TOPIC_LOCATION);
                        trace(7, "[performTask] Server file location: " + myParameter, myUserContext);
                        isRequestRedirected = true;
                        browseFile(myHost.getIPAddress(), myParameter, mySocketParam, theResponse, myUserContext);
                        trace(7, "[performTask] Retrieve the path (about:blank) by not forward...", myUserContext);
                    }
                    myJSPName = myActionDesc.getJspUrl();
                    trace(7, "[performTask] Retrieve the JSP path: " + myJSPName, myUserContext);
                }
            } else {
                myJSPName = (String) myUserContext.getAttribute(GenericDescKeys.GENERIC_DEFAULT_JSP, ContextType.APPLICATION);
            }
        } catch (GenericGuiException gge) {
            trace(2, "[performTask] GenericGuiException: (" + gge.getReturnCode() + ") " + gge.getMessage());
            theRequest.setAttribute(JPhoenixContextKeys.ERROR_BEAN, new ErrorBean(gge.getMessage()));
            if (myActionDesc != null) myJSPName = myActionDesc.getErrorJspUrl(); else myJSPName = (String) getServletContext().getAttribute(GenericDescKeys.GENERIC_ERROR_JSP);
        } catch (InvalidSessionException ise) {
            trace(2, "[performTask] InvalidSessionException: " + ise.getMessage());
            theRequest.setAttribute(JPhoenixContextKeys.ERROR_BEAN, new ErrorBean(ise.getMessage()));
            myJSPName = (String) getServletContext().getAttribute(JPhoenixDescKeys.DEFAULT_JSP);
        } catch (Exception e) {
            String myMsg = "[performTask] Warning!!! Uncaught exception: " + e.getMessage();
            trace(1, myMsg);
            theRequest.setAttribute(JPhoenixContextKeys.ERROR_BEAN, new ErrorBean(e.getMessage()));
            if (myActionDesc != null) {
                myJSPName = myActionDesc.getErrorJspUrl();
            } else {
                myJSPName = (String) getServletContext().getAttribute(JPhoenixDescKeys.DEFAULT_ERROR_JSP);
            }
        } finally {
            if (!isRequestRedirected) {
                try {
                    trace(7, "[performTask] JSP name before forwarding: " + myJSPName);
                    if (myJSPName == null) {
                        String myErrorMsg = "Functional error: the JSP name is not filled in!";
                        trace(2, "[performTask] " + myErrorMsg);
                        theRequest.setAttribute(JPhoenixContextKeys.ERROR_BEAN, new ErrorBean(myErrorMsg));
                        myJSPName = (String) getServletContext().getAttribute(GenericDescKeys.GENERIC_ERROR_JSP);
                    }
                    trace(7, "[performTask] Forward to  " + myJSPName);
                    getServletContext().getRequestDispatcher(myJSPName).forward(theRequest, theResponse);
                } catch (ServletException e) {
                    trace(2, "[performTask] ServletException while dispatching toward the " + myJSPName + " JSP: " + e.getMessage());
                } catch (IOException e) {
                    trace(2, "[performTask] IOException while dispatching toward the JSP: " + myJSPName + " JSP: " + e.getMessage());
                }
            }
        }
        trace(7, "[performTask] ###################### Generic GUI Servlet End #####################");
    }

    /**
	 * Get back the value of a given property in the properties file.
	 * Creation date: 03/2001
	 * @return java.lang.String
	 * @param thePropFile java.util.Properties
	 * @param theProperty java.lang.String
	 * @exception JBpsGuiException The exception description.
	 */
    private String checkGetProperty(java.util.Properties thePropFile, String theProperty) throws GenericGuiException {
        String myValue = thePropFile.getProperty(theProperty);
        if ((myValue == null) || (myValue.trim().equals(""))) {
            String myErrorMsg = "Missing mandatory field in the properties file: " + theProperty;
            trace(2, myErrorMsg);
            throw new GenericGuiException(GenericGuiException.ERR_PROPERTY, myErrorMsg);
        }
        return myValue;
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (04/05/01 10:28:24)
	 * @return java.util.LinkedList
	 */
    private java.util.LinkedList getActionList() {
        return actionList;
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (03/04/01 11:21:18)
	 * @return org.epo.gui.jphoenix.generic.GuiTrace
	 */
    private org.epo.gui.jphoenix.generic.GuiTrace getTrace() {
        return trace;
    }

    /**
	 * Load Stored Parameters of the servlet from the config file
	 * Creation date: 01/2001
	 */
    private void loadStoredParameters() throws GenericGuiException {
        String myRelativeConfigFile;
        InputStream myPropFileStream;
        Properties myProperties = new Properties();
        String myMsg;
        setTrace((GuiTrace) getServletContext().getAttribute(JPhoenixDescKeys.TRACE));
        if (getTrace() != null) trace(4, "--------------------- Generic GUI Begin loadStoredParameters ---------------------"); else {
            myMsg = "Missing trace";
            System.out.println(myMsg);
            throw new GenericGuiException(GenericGuiException.ERR_PROPFILE, myMsg);
        }
        try {
            myRelativeConfigFile = getServletConfig().getInitParameter(PROP_FILENAME);
        } catch (Exception e) {
            myMsg = "Missing parameter " + PROP_FILENAME;
            trace(2, myMsg);
            throw new GenericGuiException(GenericGuiException.ERR_PROPFILE, myMsg);
        }
        try {
            myPropFileStream = getServletContext().getResourceAsStream(myRelativeConfigFile);
        } catch (Exception e) {
            myMsg = "Invalid properties file " + PROP_FILENAME + ": " + e.getMessage();
            trace(2, myMsg);
            throw new GenericGuiException(GenericGuiException.ERR_PROPFILE, myMsg);
        }
        try {
            myProperties.load(myPropFileStream);
        } catch (IOException e) {
            myMsg = "Exception while opening the properties file " + PROP_FILENAME + ": " + e.getMessage();
            trace(2, myMsg);
            throw new GenericGuiException(GenericGuiException.ERR_OPEN_FILE, myMsg);
        }
        String myInternalJspPath = (String) getServletContext().getAttribute(JPhoenixContextKeys.JPHOENIX_JSP_PATH);
        String myExternalJspPath = (String) getServletContext().getAttribute(JPhoenixContextKeys.JSP_PATH);
        String myScriptPath = (String) getServletContext().getAttribute(JPhoenixContextKeys.SCRIPT_PATH);
        String myCssPath = (String) getServletContext().getAttribute(JPhoenixContextKeys.CSS_PATH);
        myInternalJspPath += checkGetProperty(myProperties, "generic.jsp.path");
        myExternalJspPath += checkGetProperty(myProperties, "generic.jsp.path");
        String myGenericCss = myCssPath + checkGetProperty(myProperties, "generic.css");
        String myGenericScript = myScriptPath + checkGetProperty(myProperties, "generic.script");
        String myTree_NSScript = myScriptPath + checkGetProperty(myProperties, "tree_ns.script");
        getServletContext().setAttribute(GenericContextKeys.GENERIC_JSP_PATH, myExternalJspPath);
        getServletContext().setAttribute(GenericContextKeys.GENERIC_SCRIPT, myGenericScript);
        getServletContext().setAttribute(GenericContextKeys.TREE_NS_SCRIPT, myTree_NSScript);
        getServletContext().setAttribute(GenericContextKeys.GENERIC_CSS, myGenericCss);
        String myDefaultJsp = checkGetProperty(myProperties, "generic.default");
        String myErrorDefaultJsp = checkGetProperty(myProperties, "generic.error.default");
        getServletContext().setAttribute(GenericDescKeys.GENERIC_DEFAULT_JSP, myInternalJspPath + myDefaultJsp);
        getServletContext().setAttribute(GenericDescKeys.GENERIC_ERROR_JSP, myInternalJspPath + myErrorDefaultJsp);
        trace(6, "*** JPhoenix Path - External call ***");
        trace(6, "JSP_PATH:    " + getServletContext().getAttribute(JPhoenixContextKeys.JSP_PATH));
        trace(6, "CSS_PATH:    " + getServletContext().getAttribute(JPhoenixContextKeys.CSS_PATH));
        trace(6, "SCRIPT_PATH: " + getServletContext().getAttribute(JPhoenixContextKeys.SCRIPT_PATH));
        trace(6, "*** Generic Path - External call ***");
        trace(6, "GENERIC_JSP_PATH:    " + getServletContext().getAttribute(GenericContextKeys.GENERIC_JSP_PATH));
        trace(6, "GENERIC_SCRIPT:      " + getServletContext().getAttribute(GenericContextKeys.GENERIC_SCRIPT));
        trace(6, "TREE_NS_SCRIPT:      " + getServletContext().getAttribute(GenericContextKeys.TREE_NS_SCRIPT));
        trace(6, "GENERIC_CSS:         " + getServletContext().getAttribute(GenericContextKeys.GENERIC_CSS));
        trace(6, "*** Generic Path - Internal call ***");
        trace(6, "GENERIC_DEFAULT_JSP: " + getServletContext().getAttribute(GenericDescKeys.GENERIC_DEFAULT_JSP));
        trace(6, "GENERIC_ERROR_JSP:   " + getServletContext().getAttribute(GenericDescKeys.GENERIC_ERROR_JSP));
        trace(6, "Store all possible action defined by NAME, JSP_CALLER, JSP_CALLED");
        storeActionDesc(myInternalJspPath);
        trace(4, "--------------------- Generic GUI End loadStoredParameters ---------------------");
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (04/05/01 10:28:24)
	 * @param newActionList java.util.LinkedList
	 */
    private void setActionList(java.util.LinkedList newActionList) {
        actionList = newActionList;
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (03/04/01 11:21:18)
	 * @param newTrace org.epo.gui.jphoenix.generic.GuiTrace
	 */
    private void setTrace(org.epo.gui.jphoenix.generic.GuiTrace newTrace) {
        trace = newTrace;
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (07/06/01 10:40:20)
	 * @param theLevel int
	 * @param theMessage java.lang.String
	 */
    private void trace(int theLevel, String theMessage) {
        getTrace().sendTrace(theLevel, "[G] " + theMessage);
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (07/06/01 10:40:20)
	 * @param theLevel int
	 * @param theMessage java.lang.String
	 */
    private void trace(int theLevel, String theMessage, UserContext theUserContext) throws InvalidSessionException {
        getTrace().userTrace(theLevel, "[G] " + theMessage, theUserContext);
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (17/04/01 15:16:55)
	 */
    private void browseFile(String theIPAdress, String theFileName, int[] theSocketParam, HttpServletResponse theResponse, UserContext theUserContext) throws GenericGuiException, InvalidSessionException {
        trace(9, "[browseFile] Begin", theUserContext);
        if (theSocketParam == null || theSocketParam[0] == 0) {
            trace(5, "[browseFile] SocketParam null or empty.", theUserContext);
            browseFileError(theFileName, theResponse, theUserContext);
            return;
        }
        int myPortSocket = theSocketParam[1];
        java.net.Socket mySocket = null;
        java.io.InputStream myInput = null;
        ServletOutputStream mySOS = null;
        try {
            int myTotalFileByte = theSocketParam[0];
            trace(7, "[browseFile] Open client socket", theUserContext);
            mySocket = new java.net.Socket(theIPAdress, myPortSocket);
            myInput = mySocket.getInputStream();
            trace(7, "[browseFile] InputStream retrieved", theUserContext);
            int myBufferSize = 4096;
            byte[] myRequestBody = new byte[myBufferSize];
            byte[] myEndBody = null;
            int myNbByteRead = 0, myTotalByteRead = 0, myTotalByteWritten = 0;
            String myContentType = "text/plain";
            theResponse.setContentType(myContentType);
            trace(7, "[browseFile] Response Content-Type set (" + myContentType + ").", theUserContext);
            theResponse.setContentLength(myTotalFileByte);
            trace(7, "[browseFile] Response Content-Length set (" + myTotalFileByte + ").", theUserContext);
            mySOS = theResponse.getOutputStream();
            while ((myNbByteRead = myInput.read(myRequestBody)) != -1) {
                myTotalByteRead += myNbByteRead;
                if (myTotalByteRead > myTotalFileByte) {
                    trace(7, "[browseFile] Try to write more bytes than content-length: " + myTotalByteRead + " bytes read for " + myTotalFileByte + " bytes declared (current buffer: " + myNbByteRead + " bytes)", theUserContext);
                    myNbByteRead = myTotalFileByte - (myTotalByteRead - myNbByteRead);
                    trace(7, "[browseFile] -> Cut the extra part and write only " + myNbByteRead + " bytes", theUserContext);
                }
                if (myNbByteRead != myBufferSize) {
                    trace(7, "[browseFile] Try to write less than buffer size (" + myBufferSize + "): " + myNbByteRead + " bytes", theUserContext);
                    myEndBody = new byte[myNbByteRead];
                    System.arraycopy(myRequestBody, 0, myEndBody, 0, myNbByteRead);
                    mySOS.write(myEndBody);
                } else {
                    mySOS.write(myRequestBody);
                }
                myTotalByteWritten += myNbByteRead;
                trace(9, "[browseFile] Write " + myNbByteRead + " bytes of Request Body in ServletOutputStream (" + myTotalByteWritten + ")", theUserContext);
            }
            trace(7, "[browseFile] " + myTotalByteWritten + " bytes written in ServletOutputStream.", theUserContext);
        } catch (IOException ioe) {
            trace(2, "[browseFile] IOException: " + ioe.getMessage(), theUserContext);
            ioe.printStackTrace();
            browseFileError(theFileName, theResponse, theUserContext);
            throw new GenericGuiException(GenericGuiException.ERR_BROWSE, ioe.getMessage());
        } finally {
            try {
                if (myInput != null) {
                    myInput.close();
                }
                if (mySocket != null) {
                    mySocket.close();
                }
                if (mySOS != null) {
                    mySOS.flush();
                    mySOS.close();
                }
            } catch (Exception e) {
                mySocket = null;
                trace(2, "[browseFile] Exception during socket & stream closure: " + e.getMessage(), theUserContext);
            }
        }
        trace(9, "[browseFile] End", theUserContext);
    }

    /**
	 * Insert the method's description here.
	 * Creation date: (17/04/01 15:16:55)
	 */
    private void browseFileError(String theFileName, HttpServletResponse theResponse, UserContext theUserContext) throws GenericGuiException, InvalidSessionException {
        trace(9, "[browseFileError] Begin", theUserContext);
        String myHTMLString = "<HTML><BODY>";
        myHTMLString += "<H1>File not found or empty: " + theFileName + "</H1>";
        myHTMLString += "</BODY></HTML>";
        byte[] myRequestBody = myHTMLString.getBytes();
        int myNbByte = myRequestBody.length;
        String myContentType = "text/html";
        theResponse.setContentType(myContentType);
        trace(7, "[browseFileError] Response Content-Type set (" + myContentType + ").", theUserContext);
        theResponse.setContentLength(myNbByte);
        trace(7, "[browseFileError] Response Content-Length set (" + myNbByte + ").", theUserContext);
        ServletOutputStream mySOS = null;
        try {
            mySOS = theResponse.getOutputStream();
            mySOS.write(myRequestBody);
            trace(9, "[browseFileError] Write " + myNbByte + " byte(s) of Request Body in SOS", theUserContext);
        } catch (IOException ioe) {
            trace(2, "[browseFileError] IOException: " + ioe.getMessage(), theUserContext);
            throw new GenericGuiException(GenericGuiException.ERR_BROWSE, ioe.getMessage());
        } catch (Exception e) {
            trace(2, "[browseFileError] Exception: " + e.getMessage(), theUserContext);
            throw new GenericGuiException(GenericGuiException.ERR_BROWSE, e.getMessage());
        } finally {
            try {
                if (mySOS != null) {
                    mySOS.flush();
                    mySOS.close();
                }
            } catch (Exception e) {
                trace(2, "[browseFileError] Exception during ServletOutputStream closure: " + e.getMessage(), theUserContext);
            }
        }
        trace(9, "[browseFileError] End", theUserContext);
    }

    /**
	 * Retrieve in parameter hashtable what action is required
	 * Creation date: 01/2001
	 * @return java.lang.String
	 * @param theHashParams java.util.Hashtable
	 */
    private ActionDesc getActionDesc(UserContext theUserContext) throws InvalidSessionException {
        trace(9, "--------------------- Begin getActionDesc --------------------", theUserContext);
        String myActionCode = null;
        ActionDesc myActionDesc = null, returnActionDesc = null;
        if (theUserContext.containsKey(JPhoenixParamKeys.ACTION)) {
            myActionCode = (String) theUserContext.get(JPhoenixParamKeys.ACTION);
            trace(7, "myActionCode: " + myActionCode, theUserContext);
            if (getActionList() != null && myActionCode != null) {
                for (int i = 0; i < getActionList().size(); i++) {
                    myActionDesc = (ActionDesc) getActionList().get(i);
                    if (myActionDesc.getActionCode().equals(myActionCode)) {
                        returnActionDesc = myActionDesc;
                        break;
                    }
                }
            }
        }
        if (returnActionDesc == null) trace(5, "ActionDesc null -> default jsp is called", theUserContext);
        trace(9, "---------------------- End getActionDesc ---------------------", theUserContext);
        return returnActionDesc;
    }

    /**
	 * Store in the context the available actions and their related properties in an hashtable.
	 * Creation date: 03/2001
	 */
    private void storeActionDesc(String theJspPath) {
        setActionList(new LinkedList());
        getActionList().add(new ActionDesc(JPhoenixContextKeys.HOST_CHOICE, theJspPath + JSP_HOST_CHOICE, theJspPath + JSP_HOST_CHOICE));
        getActionList().add(new ActionDesc(GenericContextKeys.CONFIG, theJspPath + JSP_HOST_CONFIG, theJspPath + JSP_HOST_CONFIG));
        getActionList().add(new ActionDesc(GenericContextKeys.EXPLORER, theJspPath + JSP_EXPLORER, theJspPath + JSP_EXPLORER));
        getActionList().add(new ActionDesc(GenericContextKeys.TREE, theJspPath + JSP_TREE, theJspPath + JSP_TREE));
        getActionList().add(new ActionDesc(GenericContextKeys.INFO, theJspPath + JSP_INFO_LIST, theJspPath + JSP_INFO_LIST));
        getActionList().add(new ActionDesc(GenericContextKeys.TABINFO, theJspPath + JSP_TAB_INFO_LIST, theJspPath + JSP_TAB_INFO_LIST));
        getActionList().add(new ActionDesc(GenericContextKeys.TABFILTER, theJspPath + JSP_TAB_INFO_LIST, theJspPath + JSP_TAB_INFO_LIST));
        getActionList().add(new ActionDesc(GenericContextKeys.UPDATE, theJspPath + JSP_INFO_LIST, theJspPath + JSP_INFO_LIST));
        getActionList().add(new ActionDesc(GenericContextKeys.UPDATETAB, theJspPath + JSP_TAB_INFO_LIST, theJspPath + JSP_TAB_INFO_LIST));
        getActionList().add(new ActionDesc(GenericContextKeys.COMMAND, theJspPath + JSP_INFO_LIST, theJspPath + JSP_INFO_LIST));
        getActionList().add(new ActionDesc(GenericContextKeys.BROWSE, "about:blank", theJspPath + JSP_INFO_LIST));
    }
}
