package de.iritgo.aktera.struts;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.avalon.framework.context.ContextException;
import org.apache.commons.beanutils.BasicDynaBean;
import org.apache.commons.beanutils.BasicDynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.MessageResources;
import de.iritgo.aktera.authentication.DefaultUserEnvironment;
import de.iritgo.aktera.authentication.UserEnvironment;
import de.iritgo.aktera.authorization.AuthorizationException;
import de.iritgo.aktera.authorization.AuthorizationManager;
import de.iritgo.aktera.clients.ClientException;
import de.iritgo.aktera.clients.ResponseElementDynaBean;
import de.iritgo.aktera.comm.BinaryWrapper;
import de.iritgo.aktera.core.container.KeelContainer;
import de.iritgo.aktera.model.Command;
import de.iritgo.aktera.model.Input;
import de.iritgo.aktera.model.Output;
import de.iritgo.aktera.model.ResponseElement;
import de.iritgo.aktera.servers.ComparableContext;
import de.iritgo.aktera.servers.KeelAbstractServer;
import de.iritgo.aktera.ui.UIController;
import de.iritgo.aktera.ui.UIControllerException;
import de.iritgo.aktera.ui.UIRequest;
import de.iritgo.aktera.ui.UIResponse;
import de.iritgo.aktera.util.i18n.Message;
import de.iritgo.simplelife.string.StringTools;

public class BeanAction extends Action {

    /** File writing buffer size in bytes */
    private static final int BUFFER_SIZE = 1024 * 1024;

    protected static final String COMMAND_PARAM = "COMMAND_";

    protected static final int COMMAND_PARAM_LEN = COMMAND_PARAM.length();

    protected static final String PARAMETER_PARAM = "PARAMS_";

    protected static final String MODEL_PARAM = "model";

    protected static final String MODEL_PARAMS_PARAM = "MODEL_PARAMS_PARAM";

    protected static final String KEEL_MODEL_PARAM = "orig-model";

    protected static final String BEAN_PARAM = "bean";

    /** Logger */
    private static Log log = LogFactory.getFactory().getInstance("de.iritgo.aktera.struts.BeanAction");

    /**
	 * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping,
	 *      org.apache.struts.action.ActionForm,
	 *      javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String controllerId = "?";
        try {
            BeanRequest uiRequest = new BeanRequest();
            uiRequest.setLocale(request.getLocale());
            setRequestParameters(uiRequest, request);
            setRequestBean(uiRequest, request);
            controllerId = uiRequest.getBean();
            if (controllerId != null && controllerId.startsWith("_MODEL_.")) {
                StringBuffer redirectLocation = new StringBuffer();
                redirectLocation.append(request.getRequestURL().toString().replace("bean.do", "model.do"));
                redirectLocation.append("?model=" + controllerId.replace("_MODEL_.", ""));
                for (Map.Entry<String, Object> parameter : uiRequest.getParameters().entrySet()) {
                    if ("bean".equals(parameter.getKey())) {
                        continue;
                    }
                    redirectLocation.append("&");
                    redirectLocation.append(parameter.getKey());
                    redirectLocation.append("=");
                    redirectLocation.append(parameter.getValue());
                }
                response.sendRedirect(redirectLocation.toString());
                return null;
            }
            UserEnvironment userEnvironment = getUserEnvironment(request);
            uiRequest.setUserEnvironment(userEnvironment);
            UIController controller = (UIController) KeelContainer.defaultContainer().getSpringBean(controllerId);
            AuthorizationManager authorizationManager = (AuthorizationManager) KeelContainer.defaultContainer().getSpringBean(AuthorizationManager.ID);
            if (!authorizationManager.allowed(controller, controllerId, userEnvironment)) {
                throw new SecurityException("Controller '" + controllerId + "' not authorized");
            }
            BeanResponse uiResponse = new BeanResponse();
            controller.execute(uiRequest, uiResponse);
            createDynaBean(controller, uiResponse, request, response, controllerId);
            handleErrors(controller, uiResponse, request, response, controllerId);
            String forward = uiResponse.getForward();
            if (forward == null) {
                forward = controller.getForward();
                if (forward == null) {
                    forward = "default";
                }
            }
            return mapping.findForward(forward);
        } catch (ClassCastException x) {
            String msg = "Requested controller '" + controllerId + "' doesn't implement interface UIConroller: " + x.toString();
            log.error(msg);
            throw new ServletException(msg);
        }
    }

    public static void execute(UIRequest request, UIResponse response) throws AuthorizationException, UIControllerException {
        UIController controller = (UIController) KeelContainer.defaultContainer().getSpringBean(request.getBean());
        AuthorizationManager authorizationManager = (AuthorizationManager) KeelContainer.defaultContainer().getSpringBean(AuthorizationManager.ID);
        if (!authorizationManager.allowed(controller, request.getBean(), request.getUserEnvironment())) {
            throw new SecurityException("Controller '" + request.getBean() + "' not authorized");
        }
        controller.execute(request, response);
        if (response.getForward() == null) {
            response.setForward(controller.getForward());
            if (response.getForward() == null) {
                response.setForward("default");
            }
        }
    }

    private UserEnvironment getUserEnvironment(HttpServletRequest request) throws ContextException {
        String sessionId = request.getSession(true).getId();
        ComparableContext context = KeelAbstractServer.getContexts().get(sessionId);
        UserEnvironment userEnvironment = null;
        if (context != null) {
            try {
                userEnvironment = (UserEnvironment) context.get(UserEnvironment.CONTEXT_KEY);
            } catch (ContextException ignored) {
            }
        }
        if (userEnvironment == null) {
            userEnvironment = new DefaultUserEnvironment();
        }
        return userEnvironment;
    }

    /**
	 * Describe method createDynaBean() here.
	 *
	 * @param controller
	 * @param uiResponse
	 * @param request
	 * @param response
	 * @param beanName
	 * @throws ClientException
	 */
    private void createDynaBean(UIController controller, BeanResponse uiResponse, HttpServletRequest request, HttpServletResponse response, String beanName) throws ClientException {
        ArrayList inputs = new ArrayList();
        ArrayList outputs = new ArrayList();
        ArrayList commands = new ArrayList();
        Iterator<ResponseElement> allElements = uiResponse.getAll();
        if (!allElements.hasNext()) {
            log.error("No elements in response from server");
        }
        MessageResources messages = getMessageResources(controller, request, beanName);
        ResponseElement re = null;
        for (Iterator i = allElements; i.hasNext(); ) {
            re = (ResponseElement) i.next();
            internationalize(re, messages);
            ResponseElementDynaBean reAsBean = new ResponseElementDynaBean(re);
            request.setAttribute(re.getName(), reAsBean);
            if (re instanceof Input) {
                inputs.add(reAsBean);
            } else if (re instanceof Output) {
                final String outputType = (String) re.getAttribute("type");
                if ((outputType != null) && outputType.equals("binary")) {
                    log.debug("File Data is available");
                    final BinaryWrapper data = (BinaryWrapper) ((Output) re).getContent();
                    final long dataSize = data.getSize();
                    if ((dataSize > 0) && (dataSize < Integer.MAX_VALUE)) {
                        response.setContentLength((int) data.getSize());
                    }
                    response.setContentType(data.getContentType());
                    response.setHeader("Content-Disposition", (String) re.getAttribute("Content-Disposition"));
                    BufferedOutputStream buffOut = null;
                    try {
                        log.info("Writing data with no compression");
                        OutputStream out = response.getOutputStream();
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
        request.setAttribute("inputs", inputs);
        request.setAttribute("outputs", outputs);
        request.setAttribute("commands", commands);
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
            bd = new BasicDynaClass(beanName, Class.forName("org.apache.commons.beanutils.BasicDynaBean"), dps);
            BasicDynaBean newForm = (BasicDynaBean) bd.newInstance();
            for (Iterator i2 = inputs.iterator(); i2.hasNext(); ) {
                oneInput = (ResponseElementDynaBean) i2.next();
                newForm.set((String) oneInput.get("name"), oneInput.get("defaultValue"));
            }
            request.setAttribute("default", newForm);
        } catch (ClassNotFoundException e) {
            throw new ClientException(e);
        } catch (IllegalAccessException e) {
            throw new ClientException(e);
        } catch (InstantiationException e) {
            throw new ClientException(e);
        }
    }

    /**
	 * Describe method getMessageResources() here.
	 *
	 * @param controller
	 * @param request
	 * @param beanName
	 * @return
	 */
    private MessageResources getMessageResources(UIController controller, HttpServletRequest request, String beanName) {
        MessageResources appMessages = (MessageResources) request.getAttribute(Globals.MESSAGES_KEY);
        if (appMessages.getReturnNull()) {
            appMessages.setReturnNull(false);
        }
        String bundle = (String) controller.getBundle();
        if (bundle == null) {
            if (beanName.indexOf(".") > 0) {
                String appName = beanName.substring(0, beanName.indexOf("."));
                MessageResources newMessages = (MessageResources) getServlet().getServletContext().getAttribute(appName);
                if (newMessages != null) {
                    appMessages = newMessages;
                    if (log.isDebugEnabled()) {
                        log.debug("Application-specific message bundle for controller '" + beanName + "' found under key '" + appName + "'");
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("No application-specific message bundle for controller '" + beanName + "' found under key '" + appName + "'");
                    }
                }
            }
        } else {
            MessageResources newMessages = (MessageResources) getServlet().getServletContext().getAttribute(bundle);
            if (newMessages != null) {
                appMessages = newMessages;
                log.warn("Controller specified message bundle '" + bundle + ", but no bundle was found under that key");
            }
        }
        appMessages.setReturnNull(false);
        return appMessages;
    }

    /**
	 * Describe method internationalize() here.
	 *
	 * @param re
	 * @param messages
	 */
    private void internationalize(ResponseElement re, MessageResources messages) {
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

    /**
	 * Describe method translateString() here.
	 *
	 * @param orig
	 * @param messages
	 * @return
	 */
    private String translateString(String orig, MessageResources messages) {
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
	 * Describe method tokenize() here.
	 *
	 * @param orig
	 * @param messages
	 * @return
	 */
    private Object[] tokenize(String orig, MessageResources messages) {
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

    /**
	 * Describe method translateMessage() here.
	 *
	 * @param message
	 * @param messages
	 * @return
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

    private void setRequestParameters(UIRequest uiRequest, HttpServletRequest request) throws ClientException {
        String oneParamName = null;
        for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
            oneParamName = (String) e.nextElement();
            if (!oneParamName.equals("")) {
                if (oneParamName.startsWith(COMMAND_PARAM)) {
                    processCommandParam(uiRequest, request, oneParamName.substring(COMMAND_PARAM_LEN));
                } else if (oneParamName.equals(MODEL_PARAMS_PARAM)) {
                    processCommandParam(uiRequest, request, request.getParameter(MODEL_PARAMS_PARAM));
                }
                if ((!oneParamName.startsWith(COMMAND_PARAM)) && (!oneParamName.startsWith(PARAMETER_PARAM)) && (!oneParamName.equals(MODEL_PARAMS_PARAM))) {
                    processNonCommandParam(uiRequest, request, oneParamName);
                }
            }
        }
    }

    private void processNonCommandParam(UIRequest uiRequest, HttpServletRequest request, String oneParamName) {
        final String[] values = request.getParameterValues(oneParamName);
        if (values.length <= 1) {
            uiRequest.setParameter(oneParamName, request.getParameter(oneParamName));
        } else {
            uiRequest.setParameter(oneParamName, values);
        }
        if (log.isDebugEnabled()) {
            log.debug("Regular form parameter '" + oneParamName + "', '" + request.getParameter(oneParamName) + "'");
        }
    }

    private void processCommandParam(UIRequest uiRequest, HttpServletRequest request, String commandName) throws ClientException {
        if (commandName == null || "".equals(commandName)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Command:'" + commandName + "'");
        }
        String associatedParams = request.getParameter(PARAMETER_PARAM + commandName);
        if (associatedParams == null) {
            throw new ClientException("Command '" + commandName + "' was found, but no parameters under name '" + PARAMETER_PARAM + commandName + "' was found");
        }
        StringTokenizer stk1 = new StringTokenizer(associatedParams, "&");
        String onePair = null;
        String oneCommandParamName = null;
        String oneCommandParamValue = null;
        while (stk1.hasMoreTokens()) {
            onePair = stk1.nextToken();
            StringTokenizer stk2 = new StringTokenizer(onePair, "=");
            oneCommandParamName = stk2.nextToken();
            if (!stk2.hasMoreTokens()) {
                oneCommandParamValue = "";
            } else {
                oneCommandParamValue = stk2.nextToken();
            }
            if (oneCommandParamName.equals(MODEL_PARAM)) {
                if (log.isDebugEnabled()) {
                    log.debug("Model from command:'" + oneCommandParamValue + "'");
                }
                uiRequest.setParameter(KEEL_MODEL_PARAM, oneCommandParamValue);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Parameter '" + oneCommandParamName + "', '" + oneCommandParamValue + "'");
                }
                uiRequest.setParameter(oneCommandParamName, oneCommandParamValue);
            }
        }
    }

    private void setRequestBean(BeanRequest uiRequest, HttpServletRequest request) throws ClientException {
        String beanName = (String) request.getParameter(BEAN_PARAM);
        if (StringTools.isTrimEmpty(beanName)) {
            beanName = (String) uiRequest.getParameter(BEAN_PARAM);
            if (StringTools.isTrimEmpty(beanName)) {
                throw new ClientException("No bean specified in request: '" + request.getRequestURL() + "?" + request.getQueryString() + "'");
            }
        }
        uiRequest.setBean(beanName);
    }

    protected void handleErrors(UIController controller, BeanResponse uiResponse, HttpServletRequest request, HttpServletResponse response, String beanName) {
        MessageResources messages = getMessageResources(controller, request, beanName);
        Map errors = uiResponse.getErrors();
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
                String t = uiResponse.getStackTrace(oneKey);
                if (t != null) {
                    log.debug("Exception for error '" + oneKey + "' (" + translatedMessage + ")\n" + t);
                    fatalMessage.append(t + "\n");
                }
                String errorType = uiResponse.getErrorType(oneKey);
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
            request.setAttribute(Globals.ERROR_KEY, ae);
        }
    }

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
}
