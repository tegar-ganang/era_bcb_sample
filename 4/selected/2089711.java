package com.ericsson.xsmp.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.codehaus.xfire.transport.http.XFireServletController;
import org.spark.util.ClassUtil;
import org.spark.util.HttpClientInfo;
import org.spark.util.HttpUtil;
import org.spark.util.StringUtil;
import org.spark.util.activation.StreamDataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import com.ericsson.xsmp.aaa.APIAuthority;
import com.ericsson.xsmp.aaa.APIMethod;
import com.ericsson.xsmp.aaa.Application;
import com.ericsson.xsmp.admin.Management;
import com.ericsson.xsmp.admin.ManagementUtil;
import com.ericsson.xsmp.core.Dispatcher;
import com.ericsson.xsmp.core.XSMPLoginReqDocument;
import com.ericsson.xsmp.core.XSMPLoginRespDocument;
import com.ericsson.xsmp.core.XSMPLogoutReqDocument;
import com.ericsson.xsmp.core.XSMPMsgDocument;
import com.ericsson.xsmp.core.XSMPFault;
import com.ericsson.xsmp.core.XSMPMsgDocument.XSMPMsg.Header;
import com.ericsson.xsmp.service.event.EventDocument;
import com.ericsson.xsmp.service.event.EventMgmt;
import com.ericsson.xsmp.service.event.EventUtil;
import com.ericsson.xsmp.service.event.Level;

public class DispatcherImpl implements Dispatcher, ApplicationContextAware, Controller, Management {

    ApplicationContext context;

    Map sessions = new ConcurrentHashMap();

    XSMPConfiguration configuration;

    EventMgmt eventService;

    public Map fetchStatus() {
        Map status = new HashMap();
        status.put("sessionCount", "" + sessions.size());
        status.put("appIds", StringUtil.merge(sessions.keySet()));
        return status;
    }

    public void start() {
    }

    public void shutdown() {
    }

    public void setApplicationContext(ApplicationContext _context) throws BeansException {
        this.context = _context;
    }

    public EventMgmt getEventService() {
        return eventService;
    }

    public void setEventService(EventMgmt eventService) {
        this.eventService = eventService;
    }

    public DispatcherImpl() {
    }

    public XSMPConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(XSMPConfiguration configuration) {
        this.configuration = configuration;
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpClientInfo clientInfo = HttpUtil.parseClientInfo((HttpServletRequest) request);
        if (request.getParameter("_debug_") != null) {
            StringBuffer buffer = new StringBuffer();
            Enumeration iter = request.getHeaderNames();
            while (iter.hasMoreElements()) {
                String name = (String) iter.nextElement();
                buffer.append(name + "=" + request.getHeader(name)).append("\n");
            }
            buffer.append("\n");
            iter = request.getParameterNames();
            while (iter.hasMoreElements()) {
                String name = (String) iter.nextElement();
                String value = request.getParameter(name);
                if (!"ISO-8859-1".equalsIgnoreCase(clientInfo.getPreferCharset())) value = new String(value.getBytes("ISO-8859-1"), clientInfo.getPreferCharset());
                buffer.append(name).append("=").append(value).append("\n");
            }
            response.setContentType("text/plain; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(buffer.toString());
            return null;
        }
        Object resultObj = handleRequest(request);
        if (resultObj == null) {
            String requestException = (String) request.getAttribute("XSMP.handleRequest.Exception");
            if (requestException != null) response.sendError(500, requestException); else response.setContentLength(0);
        } else {
            if (resultObj instanceof DataHandler) {
                response.setContentType(((DataHandler) resultObj).getContentType());
                response.setContentLength(((DataHandler) resultObj).getInputStream().available());
                IOUtils.copy(((DataHandler) resultObj).getInputStream(), response.getOutputStream());
            } else {
                String temp = resultObj.toString();
                if (temp.startsWith("<") && temp.endsWith(">")) response.setContentType("text/html; charset=" + clientInfo.getPreferCharset()); else response.setContentType("text/plain; charset=" + clientInfo.getPreferCharset());
                byte[] buffer = temp.getBytes(clientInfo.getPreferCharset());
                response.setContentLength(buffer.length);
                response.getOutputStream().write(buffer);
            }
        }
        return null;
    }

    public Object handleRequest(HttpServletRequest request) throws Exception {
        HttpClientInfo clientInfo = HttpUtil.parseClientInfo((HttpServletRequest) request);
        String[] attachIds = null;
        DataHandler[] attachments = null;
        if (request instanceof MultipartHttpServletRequest) {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
            if (multipartRequest.getFileNames() != null) {
                List tempList = new ArrayList();
                Iterator iter = multipartRequest.getFileNames();
                while (iter.hasNext()) tempList.add(iter.next());
                if (tempList.size() > 0) {
                    attachIds = new String[tempList.size()];
                    tempList.toArray(attachIds);
                    attachments = new DataHandler[attachIds.length];
                    for (int i = 0; i < attachIds.length; i++) {
                        MultipartFile multipartFile = multipartRequest.getFile(attachIds[i]);
                        attachments[i] = new DataHandler(new StreamDataSource(multipartFile.getInputStream(), multipartFile.getContentType()));
                    }
                }
            }
        }
        String dpn = request.getParameter("dpn");
        if (dpn == null) dpn = "Data";
        String data = request.getParameter(dpn);
        if (data == null) {
            data = (String) request.getAttribute(dpn);
            if (data == null) data = IOUtils.toString(request.getInputStream(), clientInfo.getPreferCharset());
        } else {
            if (!"ISO-8859-1".equalsIgnoreCase(clientInfo.getPreferCharset())) data = new String(data.getBytes("ISO-8859-1"), clientInfo.getPreferCharset());
        }
        if (data != null && data.length() > 0 && data.charAt(0) == 0xFEFF) data = data.substring(1);
        String accessMode = request.getParameter("AccessMode");
        if (accessMode == null) accessMode = (String) request.getAttribute("AccessMode");
        XSMPMsgDocument messageDoc = null;
        String bizCode = null;
        String activityCode = null;
        if ("R".equalsIgnoreCase(accessMode)) {
            messageDoc = XSMPMsgDocument.Factory.newInstance();
            messageDoc.addNewXSMPMsg();
            bizCode = request.getParameter("BizCode");
            if (bizCode == null) bizCode = (String) request.getAttribute("BizCode");
            activityCode = request.getParameter("ActivityCode");
            if (activityCode == null) activityCode = (String) request.getAttribute("ActivityCode");
            Header header = messageDoc.getXSMPMsg().addNewHeader();
            header.setBizCode(bizCode);
            header.setActivityCode(activityCode);
            if (request.getParameter("Sender") != null) header.setSender(request.getParameter("Sender")); else header.setSender((String) request.getAttribute("Sender"));
            if (request.getParameter("Receiver") != null) header.setReceiver(request.getParameter("Receiver")); else header.setReceiver((String) request.getAttribute("Receiver"));
            if (request.getParameter("Version") != null) header.setVersion(request.getParameter("Version")); else header.setVersion((String) request.getAttribute("Version"));
            messageDoc.getXSMPMsg().setBody(data);
        } else {
            try {
                messageDoc = XSMPMsgDocument.Factory.parse(data);
                if (messageDoc != null && messageDoc.getXSMPMsg() != null && messageDoc.getXSMPMsg().getHeader() != null) {
                    bizCode = messageDoc.getXSMPMsg().getHeader().getBizCode();
                    activityCode = messageDoc.getXSMPMsg().getHeader().getActivityCode();
                }
            } catch (XmlException err) {
                throw new RuntimeException(err);
            }
        }
        boolean trusting = false;
        if (request.getUserPrincipal() != null) trusting = true; else {
            if (request.getSession(false) != null && request.getSession().getAttribute(Principal.class.getName()) != null) trusting = true;
        }
        Object resultObj = internalService(messageDoc, attachIds, attachments, request.getRemoteAddr(), trusting);
        if (resultObj == null) return null; else if (resultObj instanceof XSMPMsgDocument && "R".equalsIgnoreCase(accessMode)) {
            XSMPMsgDocument resultDoc = (XSMPMsgDocument) resultObj;
            if (resultDoc.getXSMPMsg().getFault() != null) {
                request.setAttribute("XSMP.handleRequest.Exception", resultDoc.getXSMPMsg().getFault().getCode() + ":" + resultDoc.getXSMPMsg().getFault().getDescription());
                return null;
            } else return resultDoc.getXSMPMsg().getBody();
        } else return resultObj;
    }

    public Object service(String request) {
        return service2(request, null, null);
    }

    public Object service2(String request, String[] attachIds, DataHandler[] attachments) {
        if (request != null && request.length() > 0 && request.charAt(0) == 0xFEFF) request = request.substring(1);
        String remoteAddress = "";
        HttpServletRequest httpRequest = XFireServletController.getRequest();
        if (httpRequest != null && httpRequest.getRemoteAddr() != null) remoteAddress = httpRequest.getRemoteAddr();
        try {
            XSMPMsgDocument messageDoc = XSMPMsgDocument.Factory.parse(request);
            Object resultObj = internalService(messageDoc, attachIds, attachments, remoteAddress, false);
            if (resultObj == null || resultObj instanceof DataHandler) return resultObj; else return resultObj.toString();
        } catch (XmlException err) {
            throw new RuntimeException(err);
        }
    }

    private Object internalService(XSMPMsgDocument messageDoc, String[] attachIds, DataHandler[] attachments, String remoteAddress, boolean trusting) {
        XSMPMsgDocument.XSMPMsg message = null;
        XSMPMsgDocument.XSMPMsg.Header header = null;
        String sender = "";
        String receiver = "";
        String bizCode;
        String activityCode;
        String sessionId;
        String result = "";
        String version;
        message = messageDoc.getXSMPMsg();
        if (message == null || message.getHeader() == null) throw new XSMPException("00000005", "Invalid message format.");
        long startTicket = System.currentTimeMillis();
        header = message.getHeader();
        sender = header.getSender();
        receiver = header.getReceiver();
        bizCode = header.getBizCode();
        activityCode = header.getActivityCode();
        sessionId = header.getSessionId();
        version = header.getVersion();
        result = "";
        try {
            Application application = configuration.getApplication(sender);
            if (application == null) throw new XSMPException("00000004", "Invalid app id <" + sender + ">");
            if (application.getIPAddressArray() != null && application.getIPAddressArray().length > 0) {
                boolean allow = false;
                for (int i = 0; i < application.getIPAddressArray().length; i++) {
                    if (remoteAddress.equals(application.getIPAddressArray(i))) {
                        allow = true;
                        break;
                    }
                }
                if (!allow) throw new XSMPException("00000006", "Application <" + sender + "> access from a unauthorized address<" + remoteAddress + ">.");
            }
            if ("XSMP".equals(bizCode)) {
                if ("login".equals(activityCode)) result = login(message.getBody()); else if ("logout".equals(activityCode)) result = logout(message.getBody());
            } else if (!application.getTrusting() && !trusting && !authenticate(sender, sessionId)) {
                throw new XSMPException("00000003", "Invalid session id.");
            } else if (!configuration.authorize(sender, bizCode, activityCode)) {
                EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Authorize.Failed", "No right for this business function.", "Dispatch", "Dispatcher", Level.INFO, sender + "/" + bizCode + "/" + activityCode, null);
                eventService.postEvent(eventDoc, null);
                throw new XSMPException("00000006", "No right for this business function.");
            } else {
                APIAuthority apiAuthority = configuration.getAPIAuthority(sender, bizCode, activityCode);
                if (apiAuthority.getVersion() != null && apiAuthority.getVersion().length() > 0) version = apiAuthority.getVersion();
                APIMethod method = configuration.getAPIMethod(bizCode, activityCode, version);
                if (method == null) throw new XSMPException("00000007", "Invalid BizCode or ActivityCode or Version");
                SessionContext context = new SessionContext();
                context.setAppId(sender);
                context.setSessionId(sessionId);
                context.setBizCode(bizCode);
                context.setActivityCode(activityCode);
                context.setReceiver(receiver);
                context.setVersion(version);
                context.setRemoteAddress(remoteAddress);
                SessionContextUtil.pushContext(context);
                Map attachMap = null;
                if (attachIds != null && attachIds.length > 0) {
                    attachMap = new HashMap();
                    for (int i = 0; i < attachIds.length; i++) attachMap.put(attachIds[i], attachments[i]);
                }
                String msgBody = null;
                Object temp = null;
                if (method.getProxy()) {
                    msgBody = messageDoc.xmlText();
                    temp = dispatch(method.getBeanName(), method.getMethod(), msgBody, attachMap);
                    if (temp != null && temp instanceof String) {
                        String tempStr = (String) temp;
                        try {
                            XSMPMsgDocument proxyRespDoc = XSMPMsgDocument.Factory.parse(tempStr);
                            if (proxyRespDoc.getXSMPMsg() == null) temp = (String) null; else temp = proxyRespDoc.getXSMPMsg().getBody();
                        } catch (XmlException err) {
                        }
                    }
                } else {
                    msgBody = message.getBody();
                    if (msgBody != null && msgBody.length() > 0 && msgBody.charAt(0) == 0xFEFF) msgBody = msgBody.substring(1);
                    temp = dispatch(method.getBeanName(), method.getMethod(), msgBody, attachMap);
                }
                EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Dispatch.Success", "Service Success.", "Dispatch", "Dispatcher", Level.INFO, sender + "/" + bizCode + "/" + activityCode, null);
                eventService.postEvent(eventDoc, null);
                if (temp != null) {
                    if (temp instanceof DataHandler) return temp; else result = temp.toString();
                }
            }
            message.setBody(result);
        } catch (NoSuchBeanDefinitionException err) {
            EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Dispatch.Failed", "Invalid BizCode or ActivityCode", "Dispatch", "Dispatcher", Level.INFO, sender + "/" + bizCode + "/" + activityCode, null);
            eventService.postEvent(eventDoc, null);
            XSMPFault fault = message.addNewFault();
            fault.setCode("00000007");
            fault.setDescription("Invalid BizCode or ActivityCode");
        } catch (XSMPException err) {
            EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Dispatch.Failed", "Service Failed!", "Dispatch", "Dispatcher", Level.INFO, sender + "/" + bizCode + "/" + activityCode + "-->" + err.getMessage() + "-->" + message.getBody(), null);
            eventService.postEvent(eventDoc, null);
            XSMPFault fault = message.addNewFault();
            fault.setCode(err.getErrorCode());
            fault.setDescription(err.getMessage());
        } catch (InvocationTargetException err) {
            EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Dispatch.Failed", "Service Failed!", "Dispatch", "Dispatcher", Level.INFO, sender + "/" + bizCode + "/" + activityCode + "-->" + err.getCause() == null ? err.getMessage() : err.getCause().getMessage() + "-->" + message.getBody(), null);
            eventService.postEvent(eventDoc, null);
            XSMPFault fault = message.addNewFault();
            if (err.getCause() != null && err.getCause() instanceof XSMPException) {
                XSMPException err2 = (XSMPException) err.getCause();
                fault.setCode(err2.getErrorCode());
                fault.setDescription(err2.getMessage());
            } else {
                fault.setCode("00000008");
                fault.setDescription("X-Platform System Error");
                err.printStackTrace();
            }
        } catch (Throwable err) {
            EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Dispatch.Failed", "Service Failed!", "Dispatch", "Dispatcher", Level.INFO, sender + "/" + bizCode + "/" + activityCode + "-->" + err.getMessage() + "-->" + message.getBody(), null);
            eventService.postEvent(eventDoc, null);
            XSMPFault fault = message.addNewFault();
            fault.setCode("00000001");
            fault.setDescription("Unknown Error");
            err.printStackTrace();
        } finally {
            SessionContextUtil.popupContext();
        }
        long elasped = System.currentTimeMillis() - startTicket;
        if (messageDoc.getXSMPMsg().getFault() == null) {
            ManagementUtil.postTicket("Dispatcher.Success", bizCode + "." + activityCode, 1);
            ManagementUtil.postTicket("Dispatcher.Success.Elapsed", bizCode + "." + activityCode, elasped);
            ManagementUtil.postTicket("Dispatcher.Success." + sender, bizCode + "." + activityCode, 1);
            ManagementUtil.postTicket("Dispatcher.Success.Elapsed." + sender, bizCode + "." + activityCode, elasped);
        } else {
            if (!"00000003".equals(messageDoc.getXSMPMsg().getFault().getCode())) {
                ManagementUtil.postTicket("Dispatcher.Failed", bizCode + "." + activityCode, 1);
                ManagementUtil.postTicket("Dispatcher.Failed.Elapsed", bizCode + "." + activityCode, elasped);
                ManagementUtil.postTicket("Dispatcher.Failed." + sender, bizCode + "." + activityCode, 1);
                ManagementUtil.postTicket("Dispatcher.Failed.Elapsed." + sender, bizCode + "." + activityCode, elasped);
            }
        }
        return messageDoc;
    }

    boolean authenticate(String _appId, String sessionId) {
        String appId = _appId.toUpperCase();
        if (!sessions.containsKey(appId)) return false;
        if (!sessions.get(appId).equals(sessionId)) return false; else return true;
    }

    String login(String request) throws XSMPException {
        XSMPLoginReqDocument reqDoc = null;
        XSMPLoginReqDocument.XSMPLoginReq req = null;
        try {
            reqDoc = XSMPLoginReqDocument.Factory.parse(request);
            req = reqDoc.getXSMPLoginReq();
        } catch (XmlException err) {
            throw new XSMPException("00000005", "Invalid message format.", err);
        }
        String appId = req.getAppId();
        String password = req.getPassword();
        if (password == null) password = "";
        Application application = configuration.getApplication(appId);
        if (application == null || (!application.getTrusting() && !password.equals(application.getPassword()))) {
            throw new XSMPException("00000004", "Invalid app id or app password.");
        }
        String sessionId = configuration.generateSID(appId, password);
        sessions.put(appId.toUpperCase(), sessionId);
        XSMPLoginRespDocument respDoc = XSMPLoginRespDocument.Factory.newInstance();
        XSMPLoginRespDocument.XSMPLoginResp resp = respDoc.addNewXSMPLoginResp();
        resp.setSessionId(sessionId);
        EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Login.Success", "Login success.", "Login", "Dispatcher", Level.INFO, appId + "/" + sessionId, null);
        eventService.postEvent(eventDoc, null);
        return respDoc.xmlText();
    }

    String logout(String request) throws XSMPException {
        XSMPLogoutReqDocument reqDoc = null;
        XSMPLogoutReqDocument.XSMPLogoutReq req = null;
        try {
            reqDoc = XSMPLogoutReqDocument.Factory.parse(request);
            req = reqDoc.getXSMPLogoutReq();
        } catch (XmlException err) {
            throw new XSMPException("00000005", "Invalid message format.", err);
        }
        String appId = req.getAppId();
        sessions.remove(appId.toUpperCase());
        EventDocument eventDoc = EventUtil.buildEventDocument("Dispatcher.Logout.Success", "Logout success.", "Logout", "Dispatcher", Level.INFO, appId, null);
        eventService.postEvent(eventDoc, null);
        return "";
    }

    public Object dispatch(String beanName, String methodName, Object message, Map attachments) throws InvocationTargetException {
        Object serviceBean = context.getBean(beanName);
        if (serviceBean == null) return null;
        Method method = ClassUtil.getSingletonMethod(serviceBean.getClass(), methodName);
        if (method == null) throw new XSMPException("00000007", "Invalid BizCode or ActivityCode");
        Object messageObj = message;
        if (message != null && (message instanceof String) && method.getParameterTypes().length > 0) {
            Class clzType = method.getParameterTypes()[0];
            if (XmlObject.class.isAssignableFrom(clzType)) {
                try {
                    messageObj = XmlObject.Factory.parse((String) message);
                    XmlObject xbeanObj = (XmlObject) messageObj;
                    if (!clzType.isAssignableFrom(xbeanObj.schemaType().getJavaClass())) {
                        if (!xbeanObj.schemaType().isDocumentType()) throw new XSMPException("00000005", "Invalid message format."); else {
                            XmlObject[] children = xbeanObj.selectChildren(xbeanObj.schemaType().getDocumentElementName());
                            if (children == null || children.length != 1) throw new XSMPException("00000005", "Invalid message format."); else messageObj = children[0];
                        }
                    }
                } catch (XmlException err) {
                    throw new XSMPException("00000005", "Invalid message format.");
                }
            }
        }
        Object obj = null;
        try {
            if (method.getParameterTypes().length > 1) {
                if (method.getParameterTypes().length == 2 && Map.class.isAssignableFrom(method.getParameterTypes()[1])) obj = method.invoke(serviceBean, new Object[] { messageObj, attachments }); else {
                    String temp = messageObj == null ? "" : messageObj.toString();
                    String[] temps = temp.split("@@", method.getParameterTypes().length);
                    Object[] params = new Object[method.getParameterTypes().length];
                    for (int i = 0; i < temps.length; i++) {
                        Class clz = method.getParameterTypes()[i];
                        if (clz.equals(String.class)) params[i] = temps[i]; else if (clz.equals(Integer.class)) params[i] = Integer.valueOf(temps[i]); else if (clz.equals(Float.class)) params[i] = Float.valueOf(temps[i]); else if (clz.equals(Double.class)) params[i] = Double.valueOf(temps[i]); else if (clz.equals(Short.class)) params[i] = Short.valueOf(temps[i]); else if (clz.equals(Boolean.class)) params[i] = Boolean.valueOf(temps[i]); else if (XmlObject.class.isAssignableFrom(clz)) {
                            try {
                                params[i] = XmlObject.Factory.parse((String) temps[i]);
                                XmlObject xbeanObj = (XmlObject) params[i];
                                if (!clz.isAssignableFrom(xbeanObj.schemaType().getJavaClass())) {
                                    if (!xbeanObj.schemaType().isDocumentType()) throw new XSMPException("00000005", "Invalid message format."); else {
                                        XmlObject[] children = xbeanObj.selectChildren(xbeanObj.schemaType().getDocumentElementName());
                                        if (children == null || children.length != 1) throw new XSMPException("00000005", "Invalid message format."); else params[i] = children[0];
                                    }
                                }
                            } catch (XmlException err) {
                                throw new XSMPException("00000005", "Invalid message format.");
                            }
                        }
                    }
                    obj = method.invoke(serviceBean, params);
                }
            } else if (method.getParameterTypes().length == 1) obj = method.invoke(serviceBean, new Object[] { messageObj }); else obj = method.invoke(serviceBean, new Object[] {});
        } catch (IllegalAccessException err) {
            throw new XSMPException("00000006", "No right for this business function.");
        }
        return obj;
    }
}
