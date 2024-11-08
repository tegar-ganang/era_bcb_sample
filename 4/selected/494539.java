package de.iritgo.aktera.struts;

import de.iritgo.aktera.clients.ClientException;
import de.iritgo.aktera.clients.ResponseElementDynaBean;
import de.iritgo.aktera.clients.webapp.AbstractWebappClientConnector;
import de.iritgo.aktera.clients.webapp.DefaultWebappRequest;
import de.iritgo.aktera.clients.webapp.DefaultWebappResponse;
import de.iritgo.aktera.clients.webapp.WebappRequest;
import de.iritgo.aktera.clients.webapp.WebappResponse;
import de.iritgo.aktera.comm.BinaryWrapper;
import de.iritgo.aktera.comm.ModelRequestMessage;
import de.iritgo.aktera.model.Command;
import de.iritgo.aktera.model.Input;
import de.iritgo.aktera.model.KeelRequest;
import de.iritgo.aktera.model.KeelResponse;
import de.iritgo.aktera.model.ModelException;
import de.iritgo.aktera.model.Output;
import de.iritgo.aktera.model.ResponseElement;
import de.iritgo.aktera.util.i18n.Message;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;
import org.apache.struts.util.MessageResources;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * The glue to connect Struts to Keel
 *
 * @version $Revision: 1.18 $ $Date: 2006/09/20 15:57:14 $
 * @author Michael Nash
 * @author Schatterjee Created on May 24, 2003
 */
public class StrutsClientConnector extends AbstractWebappClientConnector {

    /**
	 * Write Buffer for writing files.
	 */
    private static final int BUFFER_SIZE = 1024 * 1024;

    private HttpServletRequest hreq = null;

    private HttpServletResponse hres = null;

    private ActionServlet servlet = null;

    private ActionForm form = null;

    public StrutsClientConnector() {
    }

    public StrutsClientConnector(HttpServletRequest hreq, HttpServletResponse hres, ActionForm form, ActionServlet servlet) {
        this.hreq = hreq;
        this.hres = hres;
        this.form = form;
        this.servlet = servlet;
    }

    /**
	 * @see de.iritgo.aktera.clients.webapp.WebappClientConnector#execute(de.iritgo.aktera.clients.webapp.WebappRequest,
	 *      de.iritgo.aktera.clients.webapp.WebappResponse, java.lang.String)
	 */
    public KeelResponse execute() throws ClientException, ModelException {
        WebappRequest wreq = new DefaultWebappRequest(hreq);
        WebappResponse wres = new DefaultWebappResponse(hres);
        String model = hreq.getParameter("model");
        KeelResponse kres = super.execute(wreq, wres, model);
        model = (String) kres.getAttribute("model");
        createDynaBean(kres, wreq, wres, model);
        handleErrors(kres, wreq, wres, model);
        return kres;
    }

    /**
	 * @see de.iritgo.aktera.clients.webapp.WebappClientConnector#getForward()
	 */
    public String getForward(KeelResponse kres) {
        String fwd = hreq.getParameter("forward");
        if (fwd == null) {
            fwd = (String) kres.getAttribute("forward");
        }
        if (fwd == null) {
            if (log.isWarnEnabled()) {
                log.warn("Request '" + hreq.getRequestURL() + "?" + hreq.getQueryString() + "' had no 'forward' attribute - returning default forward");
            }
            fwd = "default";
        }
        return fwd;
    }

    /**
	 * <Replace with description for createDynaBean>
	 *
	 * @param kres
	 * @param wres
	 */
    protected void createDynaBean(KeelResponse kres, WebappRequest wreq, WebappResponse wres, String modelName) throws ClientException {
        ArrayList inputs = new ArrayList();
        ArrayList outputs = new ArrayList();
        ArrayList commands = new ArrayList();
        Iterator allElements = kres.getAll();
        if (!allElements.hasNext()) {
            log.error("No elements in response from server");
        }
        MessageResources messages = getMessageResources(kres, wreq, wres, modelName);
        ResponseElement re = null;
        for (Iterator i = allElements; i.hasNext(); ) {
            re = (ResponseElement) i.next();
            internationalize(re, messages);
            ResponseElementDynaBean reAsBean = new ResponseElementDynaBean(re);
            wreq.setAttribute(re.getName(), reAsBean);
            if (re instanceof Input) {
                inputs.add(reAsBean);
            } else if (re instanceof Output) {
                final String outputType = (String) re.getAttribute("type");
                if ((outputType != null) && outputType.equals("binary")) {
                    log.debug("File Data is available");
                    final BinaryWrapper data = (BinaryWrapper) ((Output) re).getContent();
                    final long dataSize = data.getSize();
                    if ((dataSize > 0) && (dataSize < Integer.MAX_VALUE)) {
                        hres.setContentLength((int) data.getSize());
                    }
                    hres.setContentType(data.getContentType());
                    hres.setHeader("Content-Disposition", (String) re.getAttribute("Content-Disposition"));
                    BufferedOutputStream buffOut = null;
                    try {
                        log.info("Writing data with no compression");
                        OutputStream out = hres.getOutputStream();
                        buffOut = new BufferedOutputStream(out, BUFFER_SIZE);
                        data.writeTo(buffOut);
                        log.trace("Wrote Buffer.");
                    } catch (IOException e) {
                        e.printStackTrace();
                        log.error("Exception during file read/write:", e);
                        throw new ClientException("Exception during file read/write", e);
                    } finally {
                        try {
                            data.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        try {
                            buffOut.flush();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                } else {
                    outputs.add(reAsBean);
                }
            } else if (re instanceof Command) {
                commands.add(reAsBean);
            }
        }
        wreq.setAttribute("inputs", inputs);
        wreq.setAttribute("outputs", outputs);
        wreq.setAttribute("commands", commands);
        int inputCount = 0;
        DynaProperty[] dps = new DynaProperty[inputs.size()];
        ResponseElementDynaBean oneInput = null;
        for (Iterator ii = inputs.iterator(); ii.hasNext(); ) {
            oneInput = (ResponseElementDynaBean) ii.next();
            Object defValue = oneInput.get("defaultValue");
            DynaProperty dp = null;
            if (defValue != null) {
                dp = new DynaProperty((String) oneInput.get("name"), oneInput.get("defaultValue").getClass());
            } else {
                try {
                    dp = new DynaProperty((String) oneInput.get("name"), Class.forName("java.lang.String"));
                } catch (ClassNotFoundException e) {
                    throw new ClientException("Cannot create String dynaproperty", e);
                }
            }
            dps[inputCount++] = dp;
        }
        BasicDynaClass bd;
        try {
            bd = new BasicDynaClass(modelName, Class.forName("org.apache.commons.beanutils.BasicDynaBean"), dps);
            BasicDynaBean newForm = (BasicDynaBean) bd.newInstance();
            for (Iterator i2 = inputs.iterator(); i2.hasNext(); ) {
                oneInput = (ResponseElementDynaBean) i2.next();
                newForm.set((String) oneInput.get("name"), oneInput.get("defaultValue"));
            }
            wreq.setAttribute("default", newForm);
        } catch (ClassNotFoundException e) {
            throw new ClientException(e);
        } catch (IllegalAccessException e) {
            throw new ClientException(e);
        } catch (InstantiationException e) {
            throw new ClientException(e);
        }
    }

    /**
	 * @see de.iritgo.aktera.clients.webapp.AbstractWebappClientConnector#preProcessParamName(java.lang.String)
	 */
    protected String preProcessParamName(String oneParamName) {
        if ((oneParamName.endsWith(".x")) || (oneParamName.endsWith(".y"))) {
            oneParamName = oneParamName.substring(0, oneParamName.length() - 2);
        }
        return oneParamName;
    }

    /**
	 * @see de.iritgo.aktera.clients.webapp.AbstractWebappClientConnector#setKeelRequestParameters(javax.servlet.http.HttpServletRequest,
	 *      de.iritgo.aktera.model.KeelRequest)
	 */
    protected void setRequestParameters(WebappRequest wreq, KeelRequest kreq) throws ClientException {
        super.setRequestParameters(wreq, kreq);
        setRequestSource(wreq, kreq);
        setRequestHeaders(wreq, kreq);
        setRequestLocale(wreq, kreq);
        kreq.setScheme(wreq.getScheme());
        kreq.setServerName(wreq.getServerName());
        kreq.setServerPort(wreq.getServerPort());
        kreq.setContextPath(wreq.getContextPath());
        kreq.setRequestUrl(wreq.getRequestURL());
        kreq.setQueryString(wreq.getQueryString());
        if (form != null) {
            if (form instanceof DynaActionForm) {
                final boolean isMultipart = ServletFileUpload.isMultipartContent(hreq);
                if (isMultipart) {
                    log.debug("MultipartContent Form...");
                    log.debug("Starting request parse...");
                    DynaActionForm df = (DynaActionForm) form;
                    DynaClass dc = df.getDynaClass();
                    if (dc == null) {
                        throw new ClientException("Null dynaclass from the DynaActionForm - can't read properties");
                    }
                    DynaProperty[] props = dc.getDynaProperties();
                    DynaProperty oneProp = null;
                    if (log.isDebugEnabled()) {
                        for (final Enumeration enumeration = this.hreq.getParameterNames(); enumeration.hasMoreElements(); ) {
                            final String name = (String) enumeration.nextElement();
                            try {
                                for (int idx = 0; ; idx++) {
                                    final Object value = df.get(name, idx);
                                    log.debug("Array Access Parameter/Value/Index: " + name + '/' + value + '/' + idx);
                                }
                            } catch (Exception e) {
                                log.debug("Exception: " + e);
                                log.debug("No more values for: " + name);
                            }
                        }
                        for (final Enumeration enumeration = hreq.getParameterNames(); enumeration.hasMoreElements(); ) {
                            final String name = (String) enumeration.nextElement();
                            final String[] values = hreq.getParameterValues(name);
                            log.debug("Servlet Parameter name: " + name);
                            log.debug("Number of values: " + values.length);
                            for (int idx = 0; idx < values.length; idx++) {
                                log.debug("Idx/Value: " + idx + '/' + values[idx]);
                            }
                        }
                        log.debug("# of properties: " + props.length);
                    }
                    for (int i = 0; i < props.length; i++) {
                        oneProp = props[i];
                        String oneName = oneProp.getName();
                        final Object value = df.get(oneName);
                        log.debug("Getting parameter/value/type:" + oneName + '/' + value + '/' + (value == null ? "null" : value.getClass().getName()));
                        if (df.get(oneName) != null && df.get(oneName) instanceof FormFile) {
                            log.debug("Formfile");
                            FormFile fileInfo = (FormFile) df.get(oneName);
                            if (fileInfo != null && fileInfo.getFileSize() > 0) {
                                BufferedInputStream inStream = null;
                                try {
                                    inStream = new BufferedInputStream(fileInfo.getInputStream(), BUFFER_SIZE);
                                } catch (IOException e) {
                                    throw new ClientException(e.getMessage(), e);
                                }
                                final BinaryWrapper fileWrapper = new BinaryWrapper(null, hreq.getContentType(), fileInfo.getFileName(), BUFFER_SIZE, null);
                                try {
                                    final long written = fileWrapper.writeFrom(inStream);
                                    if (this.log.isDebugEnabled()) {
                                        log.debug("Read/Wrote " + written + "bytes.");
                                    }
                                } catch (IOException e) {
                                    throw new ClientException(e.getMessage(), e);
                                } finally {
                                    if (inStream != null) {
                                        try {
                                            inStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    try {
                                        fileWrapper.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (log.isDebugEnabled()) {
                                    log.debug("Setting FormFile parameter/value:" + oneName + '/' + fileWrapper.getName());
                                }
                                kreq.setParameter(oneName, fileWrapper);
                            }
                        } else if (df.get(oneName) != null && df.get(oneName) instanceof java.io.Serializable) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting FormField parameter/value:" + oneName + '/' + df.get(oneName));
                            }
                            final String[] values = hreq.getParameterValues(oneName);
                            if (values.length < 1) {
                                log.debug("No values, so setting value=null");
                                kreq.setParameter(oneName, null);
                            } else if (values.length == 1) {
                                log.debug("One value, saving as string");
                                kreq.setParameter(oneName, values[1]);
                            } else {
                                log.debug("Many values, saving as array");
                                kreq.setParameter(oneName, values);
                            }
                        }
                        log.debug("Name/Value written to request: " + oneName);
                    }
                } else {
                    log.debug("Standard Dyna Form...");
                    DynaActionForm df = (DynaActionForm) form;
                    DynaClass dc = df.getDynaClass();
                    if (dc == null) {
                        throw new ClientException("Null dynaclass from the DynaActionForm - can't read properties");
                    }
                    DynaProperty[] props = dc.getDynaProperties();
                    DynaProperty oneProp = null;
                    for (int i = 0; i < props.length; i++) {
                        oneProp = props[i];
                        String oneName = oneProp.getName();
                        final Object value = df.get(oneName);
                        if (log.isDebugEnabled()) {
                            log.debug("Getting parameter/value/type:" + oneName + '/' + value + '/' + (value == null ? "null" : value.getClass().getName()));
                        }
                        if (df.get(oneName) != null && df.get(oneName) instanceof java.io.Serializable) {
                            if (log.isDebugEnabled()) {
                                log.debug("Setting parameter/value:" + oneName + '/' + df.get(oneName));
                            }
                            kreq.setParameter(oneName, df.get(oneName));
                        }
                    }
                }
            } else {
                log.debug("Standard Form...");
                PropertyDescriptor[] pd = PropertyUtils.getPropertyDescriptors(form);
                PropertyDescriptor oneDescriptor = null;
                String onePropertyName = null;
                for (int i = 0; i < pd.length; i++) {
                    oneDescriptor = pd[i];
                    onePropertyName = oneDescriptor.getName();
                    try {
                        kreq.setParameter(onePropertyName, PropertyUtils.getProperty(form, onePropertyName));
                    } catch (IllegalAccessException e) {
                        throw new ClientException(e);
                    } catch (InvocationTargetException e) {
                        throw new ClientException(e);
                    } catch (NoSuchMethodException e) {
                        throw new ClientException(e);
                    }
                }
            }
        }
    }

    protected void handleErrors(KeelResponse kres, WebappRequest wreq, WebappResponse wres, String modelName) {
        MessageResources messages = getMessageResources(kres, wreq, wres, modelName);
        Map errors = kres.getErrors();
        StringBuffer fatalMessage = new StringBuffer();
        if (errors.size() > 0) {
            String oneKey = null;
            String oneErrorMessage = null;
            String translatedMessage = null;
            ActionErrors ae = new ActionErrors();
            for (Iterator ei = errors.keySet().iterator(); ei.hasNext(); ) {
                oneKey = ei.next().toString();
                Object o = errors.get(oneKey);
                if (o != null) {
                    oneErrorMessage = o.toString();
                } else {
                    oneErrorMessage = "No error message provided";
                }
                if (oneErrorMessage.startsWith("\n")) {
                    oneErrorMessage = oneErrorMessage.substring(1);
                }
                translatedMessage = translateString(oneErrorMessage, messages);
                fatalMessage.append(translatedMessage + "\n");
                String t = kres.getStackTrace(oneKey);
                if (t != null) {
                    log.debug("Exception for error '" + oneKey + "' (" + translatedMessage + ")\n" + t);
                    fatalMessage.append(t + "\n");
                }
                String errorType = kres.getErrorType(oneKey);
                if (errorType != null) {
                    if (errorType.equals("java.lang.SecurityException")) {
                        throw new SecurityException(translatedMessage);
                    } else if (errorType.equals("de.iritgo.aktera.permissions.PermissionException")) {
                        throw new PermissionException(translatedMessage);
                    }
                }
                if (oneErrorMessage.startsWith("$") && (!oneErrorMessage.startsWith("$$"))) {
                    oneErrorMessage = oneErrorMessage.substring(1);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("WARNING: Non-internationalized message '" + oneErrorMessage + "' added to ActionErrors. <html:errors/> may not work correctly");
                    }
                }
                ActionMessage oneError = null;
                int pipePosition = oneErrorMessage.indexOf("|");
                if (pipePosition >= 0) {
                    oneError = new ActionMessage(oneErrorMessage.substring(0, pipePosition), makeArgArray(oneErrorMessage));
                } else {
                    oneError = new ActionMessage(oneErrorMessage);
                }
                ae.add(oneKey, oneError);
            }
            wreq.setAttribute(Globals.ERROR_KEY, ae);
        }
    }

    /**
	 * @param oneErrorMessage
	 * @return
	 */
    private Object[] makeArgArray(String msg) {
        ArrayList args = new ArrayList(3);
        int pipeStart = msg.indexOf("|");
        int pipeEnd = -1;
        while (pipeStart >= 0) {
            String oneArg = null;
            pipeEnd = msg.indexOf("|", pipeStart + 1);
            if (pipeEnd >= 0) {
                oneArg = msg.substring(pipeStart + 1, pipeEnd);
            } else {
                oneArg = msg.substring(pipeStart + 1);
            }
            args.add(oneArg);
            pipeStart = pipeEnd;
        }
        return args.toArray();
    }

    private ActionServlet getServlet() {
        return servlet;
    }

    protected MessageResources getMessageResources(KeelResponse kres, WebappRequest wreq, @SuppressWarnings("unused") WebappResponse wres, String modelName) {
        MessageResources appMessages = (MessageResources) wreq.getAttribute(Globals.MESSAGES_KEY);
        if (appMessages.getReturnNull()) {
            appMessages.setReturnNull(false);
        }
        String modelBundle = (String) kres.getAttribute("bundle");
        if (modelBundle == null) {
            if (modelName.indexOf(".") > 0) {
                String appName = modelName.substring(0, modelName.indexOf("."));
                MessageResources newMessages = (MessageResources) getServlet().getServletContext().getAttribute(appName);
                if (newMessages != null) {
                    appMessages = newMessages;
                    if (log.isDebugEnabled()) {
                        log.debug("Application-specific message bundle for model '" + modelName + "' found under key '" + appName + "'");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No application-specific message bundle for model '" + modelName + "' found under key '" + appName + "'");
                    }
                }
            }
        } else {
            MessageResources newMessages = (MessageResources) getServlet().getServletContext().getAttribute(modelBundle);
            if (newMessages != null) {
                appMessages = newMessages;
                log.warn("Model specified message bundle '" + modelBundle + ", but no bundle was found under that key");
            }
        }
        appMessages.setReturnNull(false);
        return appMessages;
    }

    protected String translateString(String orig, MessageResources messages) {
        if (orig == null) {
            return null;
        }
        messages.setReturnNull(true);
        if (orig.startsWith("$$")) {
            return orig.substring(1);
        }
        if (orig.startsWith("$")) {
            if (orig.indexOf("|") > 0) {
                String argString = orig.substring(orig.indexOf("|") + 1);
                Object[] args = tokenize(argString, messages);
                String key = orig.substring(1, orig.indexOf("|"));
                String xlatedMsg = messages.getMessage(key, args);
                if (xlatedMsg == null) {
                    return orig;
                }
                return xlatedMsg;
            }
            String xlated = messages.getMessage(orig.substring(1));
            if (xlated == null) {
                return orig;
            }
            return xlated;
        }
        return orig;
    }

    /**
	 * Utility method to translate a message. The result is stored back in the
	 * Message object itself. The Message may or may not specify a message
	 * bundle key to be used for the translation. If it does not, we use the
	 * application-specific bundle we would normally use for a string instead.
	 *
	 * @returns The same Message object, but with the translated string set as
	 *          it's result string. Calling toString on this message would now
	 *          return the translated result.
	 */
    private Message translateMessage(Message message, MessageResources messages) {
        MessageResources useMessages = messages;
        if (message.getBundle() != null) {
            useMessages = (MessageResources) getServlet().getServletContext().getAttribute(message.getBundle());
            if (useMessages == null) {
                useMessages = messages;
                log.warn("Message '" + message.getKey() + "' specified message bundle '" + message.getBundle() + ", but no bundle was found under that key");
            }
        }
        Object[] values = message.getValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof String) {
                String oneString = (String) values[i];
                values[i] = translateString(oneString, useMessages);
            }
        }
        message.setValues(values);
        message.setResultString(useMessages.getMessage(message.getKey(), message.getValues()));
        return message;
    }

    protected void internationalize(ResponseElement re, MessageResources messages) {
        ResponseElement oneNested = null;
        for (Iterator i = re.getAll().iterator(); i.hasNext(); ) {
            oneNested = (ResponseElement) i.next();
            internationalize(oneNested, messages);
        }
        String oneAttribKey = null;
        Object oneAttrib = null;
        Map attribs = re.getAttributes();
        for (Iterator ia = attribs.keySet().iterator(); ia.hasNext(); ) {
            oneAttribKey = (String) ia.next();
            oneAttrib = attribs.get(oneAttribKey);
            if (oneAttrib instanceof String) {
                String s = (String) oneAttrib;
                re.setAttribute(oneAttribKey, translateString(s, messages));
            } else if (oneAttrib instanceof Message) {
                Message m = (Message) oneAttrib;
                re.setAttribute(oneAttribKey, translateMessage(m, messages));
            } else if (oneAttrib instanceof ResponseElement) {
                internationalize((ResponseElement) oneAttrib, messages);
            }
        }
        if (re instanceof Input) {
            Input i = (Input) re;
            i.setLabel(translateString(i.getLabel(), messages));
            Map validValues = i.getValidValues();
            if (validValues != null) {
                TreeMap newMap = new TreeMap();
                String oneKey = null;
                Object oneValue = null;
                for (Iterator iv = validValues.keySet().iterator(); iv.hasNext(); ) {
                    oneKey = iv.next().toString();
                    oneValue = validValues.get(oneKey);
                    if (oneValue instanceof String) {
                        newMap.put(oneKey, translateString(oneValue.toString(), messages));
                    } else {
                        newMap.put(oneKey, oneValue);
                    }
                }
                i.setValidValues(newMap);
            }
        } else if (re instanceof Output) {
            Output o = (Output) re;
            Object c = o.getContent();
            if (c instanceof String) {
                o.setContent(translateString((String) c, messages));
            }
            if (c instanceof Message) {
                o.setContent(translateMessage((Message) c, messages));
            }
        } else if (re instanceof Command) {
            Command c = (Command) re;
            c.setLabel(translateString(c.getLabel(), messages));
        }
    }

    protected Object[] tokenize(String orig, MessageResources messages) {
        ArrayList params = new ArrayList();
        StringTokenizer stk = new StringTokenizer(orig, "|");
        while (stk.hasMoreTokens()) {
            params.add(stk.nextToken());
        }
        Object[] args = new Object[params.size()];
        Iterator ai = params.iterator();
        Object oneParam = null;
        for (int i = 0; i < params.size(); i++) {
            oneParam = ai.next();
            if ((oneParam != null) && (oneParam instanceof String)) {
                args[i] = translateString(oneParam.toString(), messages);
            } else {
                args[i] = ai.next();
            }
        }
        return args;
    }

    public boolean allowed(String resource, String operation) {
        KeelRequest keelRequest = new ModelRequestMessage();
        keelRequest.setModel("security.authorization");
        keelRequest.setAttribute("sessionid", hreq.getSession().getId());
        keelRequest.setParameter("component", resource);
        if (operation != null) {
            keelRequest.setParameter("operation", operation);
        }
        try {
            KeelResponse kres = execute();
            return ((Boolean) kres.getAttribute("allowed")).booleanValue();
        } catch (ModelException e) {
            log.error("Unable to check authorization", e);
        } catch (ClientException e) {
            log.error("Unable to check authorization", e);
        }
        throw new RuntimeException("Unable to execute AuthorizationModel");
    }

    /**
	 */
    public KeelResponse execute(WebappRequest wreq, WebappResponse wres, String model) throws ClientException, ModelException {
        KeelResponse kres = super.execute(wreq, wres, model);
        return kres;
    }

    /***
	 */
    protected void setRequestHeaders(WebappRequest wreq, KeelRequest kreq) {
        String oneHeaderName = null;
        if (wreq.getHeaderNames() != null) {
            for (Enumeration e = wreq.getHeaderNames(); e.hasMoreElements(); ) {
                oneHeaderName = (String) e.nextElement();
                kreq.setHeader(oneHeaderName, wreq.getHeader(oneHeaderName));
            }
        }
    }

    /***
	 */
    protected void setRequestSource(WebappRequest wreq, KeelRequest kreq) {
        kreq.setSource(wreq.getSource());
    }

    /***
	 */
    protected void setRequestLocale(WebappRequest wreq, KeelRequest kreq) {
        kreq.setLocale(wreq.getLocale());
    }

    public void startClient() throws ModelException, ClientException, Exception {
        getClient().start();
    }

    public void stopClient() throws ModelException, ClientException, Exception {
        getClient().stop();
    }
}
