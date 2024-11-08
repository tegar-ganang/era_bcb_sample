package net.sf.chellow.monad;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import net.sf.chellow.monad.types.EmailAddress;
import net.sf.chellow.monad.types.GeoPoint;
import net.sf.chellow.monad.types.MonadDate;
import net.sf.chellow.monad.types.MonadDouble;
import net.sf.chellow.monad.types.MonadInteger;
import net.sf.chellow.monad.types.MonadLong;
import net.sf.chellow.monad.types.MonadString;
import net.sf.chellow.monad.types.MonadUri;
import net.sf.chellow.monad.types.MonadValidatable;
import net.sf.chellow.monad.types.UriPathElement;
import net.sf.chellow.physical.User;
import net.sf.chellow.ui.Chellow;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import com.Ostermiller.util.Base64;

public class Invocation {

    private HttpServletRequest req;

    private HttpServletResponse res;

    private String action = null;

    private List<String> params = new ArrayList<String>();

    private List<FileItem> multipartItems;

    private Monad monad;

    private Map<String, Object> responseHeaders = new HashMap<String, Object>();

    private int responseStatusCode;

    public enum HttpMethod {

        OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE
    }

    ;

    @SuppressWarnings("unchecked")
    public Invocation(HttpServletRequest req, HttpServletResponse res, Monad monad) throws ProgrammerException, UserException {
        this.monad = monad;
        String pathInfo;
        int start = 1;
        int finish;
        if ((this.req = req) == null) {
            throw new IllegalArgumentException("The 'req' argument must " + "not be null.");
        }
        if ((this.res = res) == null) {
            throw new IllegalArgumentException("The 'res' argument must " + "not be null.");
        }
        pathInfo = monad.getTemplateDirName().length() == 0 ? req.getServletPath() : req.getPathInfo();
        if (pathInfo != null) {
            while (start < pathInfo.length()) {
                finish = pathInfo.indexOf("/", start);
                if (finish == -1) {
                    finish = pathInfo.length();
                }
                params.add(pathInfo.substring(start, finish));
                start = finish + 1;
            }
        }
        if (params.size() > 0) {
            action = (String) params.get(0);
        }
        if (ServletFileUpload.isMultipartContent(new ServletRequestContext(req))) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                multipartItems = upload.parseRequest(req);
            } catch (FileUploadException e) {
                throw new ProgrammerException(e);
            }
        }
    }

    public String getAction() {
        return action;
    }

    public HttpServletRequest getRequest() {
        return req;
    }

    public HttpServletResponse getResponse() {
        return res;
    }

    public Monad getMonad() {
        return monad;
    }

    public String[] getParameterValues(ParameterName paramName) {
        String[] values = null;
        if (ServletFileUpload.isMultipartContent(new ServletRequestContext(req))) {
            List<String> valueList = new ArrayList<String>();
            for (FileItem item : multipartItems) {
                if (item.isFormField() && item.getFieldName().equals(paramName.toString())) {
                    valueList.add(item.getString());
                }
            }
            if (valueList.size() > 0) {
                values = new String[valueList.size()];
                for (int i = 0; i < valueList.size(); i++) {
                    values[i] = (String) valueList.get(i);
                }
            }
        } else {
            values = req.getParameterValues(paramName.toString());
        }
        return values;
    }

    protected List<MonadInstantiationException> instantiationExceptions = new ArrayList<MonadInstantiationException>();

    public <T extends MonadValidatable> T getValidatable(Class<T> clazz, String parameterName) throws ProgrammerException {
        return getValidatable(clazz, new String[] { parameterName }, parameterName);
    }

    public <T extends MonadValidatable> T getValidatable(Class<T> clazz, String[] parameterNamesString, String label) throws ProgrammerException {
        ParameterName[] parameterNames = null;
        if (parameterNamesString != null) {
            parameterNames = new ParameterName[parameterNamesString.length];
            for (int i = 0; i < parameterNamesString.length; i++) {
                parameterNames[i] = new ParameterName(parameterNamesString[i]);
            }
        }
        return getValidatable(clazz, parameterNames, null, label);
    }

    public MonadLong getMonadLongNull(String parameterName) throws ProgrammerException {
        MonadLong monadLong = null;
        HttpParameter parameter = null;
        try {
            parameter = (HttpParameter) getParameters(new ParameterName[] { new ParameterName(parameterName) }).get(0);
            String parameterValue = parameter.getFirstValue();
            if (!parameterValue.equals("null")) {
                monadLong = new MonadLong(parameterValue);
            }
        } catch (UserException e) {
            instantiationExceptions.add(new MonadInstantiationException(Long.class.getName(), parameterName, e));
        }
        return monadLong;
    }

    public boolean getBoolean(String parameterName) throws ProgrammerException {
        if (hasParameter(parameterName) && getString(parameterName).equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    public Date getDate(String baseName) throws ProgrammerException {
        MonadDate date = getMonadDate(baseName);
        if (date == null) {
            return null;
        } else {
            return date.getDate();
        }
    }

    public MonadDate getMonadDate(String baseName) throws ProgrammerException {
        return getValidatable(MonadDate.class, new String[] { baseName + "-year", baseName + "-month", baseName + "-day" }, baseName);
    }

    public GeoPoint getGeoPoint(String baseName) throws ProgrammerException {
        return getValidatable(GeoPoint.class, new String[] { baseName + "-latitude", baseName + "-longitude" }, baseName);
    }

    public Long getLong(String parameterNameString) throws ProgrammerException {
        MonadLong monadLong = getValidatable(MonadLong.class, parameterNameString);
        return monadLong == null ? null : monadLong.getLong();
    }

    public Integer getInteger(String parameterNameString) throws ProgrammerException {
        MonadInteger monadInteger = getValidatable(MonadInteger.class, parameterNameString);
        return monadInteger == null ? null : monadInteger.getInteger();
    }

    public Double getDouble(String parameterNameString) throws ProgrammerException {
        MonadDouble monadDouble = getValidatable(MonadDouble.class, parameterNameString);
        return monadDouble == null ? null : monadDouble.getDouble();
    }

    public MonadLong getMonadLong(String parameterNameString) throws ProgrammerException {
        return getValidatable(MonadLong.class, parameterNameString);
    }

    public MonadUri getMonadUri(String parameterNameString) throws ProgrammerException {
        return getValidatable(MonadUri.class, parameterNameString);
    }

    public UriPathElement getUriPathElement(String parameterNameString) throws ProgrammerException {
        return getValidatable(UriPathElement.class, parameterNameString);
    }

    public EmailAddress getEmailAddress(String parameterNameString) throws ProgrammerException {
        return getValidatable(EmailAddress.class, parameterNameString);
    }

    public MonadInteger getMonadInteger(String parameterNameString) throws ProgrammerException {
        return getValidatable(MonadInteger.class, parameterNameString);
    }

    public MonadDouble getMonadDouble(String parameterNameString) throws ProgrammerException {
        return getValidatable(MonadDouble.class, parameterNameString);
    }

    public String getString(String parameterName) throws ProgrammerException {
        MonadString monadString = getMonadString(parameterName);
        if (monadString != null) {
            return monadString.getString();
        } else {
            return null;
        }
    }

    public MonadString getMonadString(String parameterNameString) throws ProgrammerException {
        return getValidatable(MonadString.class, parameterNameString);
    }

    public <T extends MonadValidatable> T getValidatable(Class<T> clazz, ParameterName[] parameterNames, List<? extends Object> list, String label) throws ProgrammerException {
        T obj = null;
        List<HttpParameter> parameters = null;
        try {
            parameters = getParameters(parameterNames);
            Object[] parameterValues = new Object[parameters.size() + ((list == null) ? 1 : 2)];
            Class<?>[] constructorClasses = new Class[parameters.size() + ((list == null) ? 1 : 2)];
            parameterValues[0] = label;
            constructorClasses[0] = String.class;
            for (int i = 0; i < parameters.size(); i++) {
                parameterValues[i + 1] = ((HttpParameter) parameters.get(i)).getFirstValue();
                constructorClasses[i + 1] = String.class;
            }
            if (list != null) {
                constructorClasses[constructorClasses.length - 1] = List.class;
                parameterValues[parameterValues.length - 1] = list;
            }
            obj = clazz.getConstructor(constructorClasses).newInstance(parameterValues);
        } catch (IllegalArgumentException e) {
            throw new ProgrammerException(e);
        } catch (InstantiationException e) {
            throw new ProgrammerException(e);
        } catch (IllegalAccessException e) {
            throw new ProgrammerException(e);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof UserException) {
                instantiationExceptions.add(new MonadInstantiationException(clazz.getName(), label, (UserException) e.getCause()));
            } else {
                throw new ProgrammerException(e);
            }
        } catch (SecurityException e) {
            throw new ProgrammerException(e);
        } catch (NoSuchMethodException e) {
            throw new ProgrammerException(e);
        } catch (UserException e) {
            instantiationExceptions.add(new MonadInstantiationException(clazz.getName(), label, e));
        } catch (ProgrammerException e) {
            instantiationExceptions.add(new MonadInstantiationException(clazz.getName(), label, UserException.newInvalidParameter(e.getMessage())));
        }
        return obj;
    }

    public final boolean isValid() {
        return instantiationExceptions.isEmpty();
    }

    public void setResponseStatusCode(int statusCode) {
        responseStatusCode = statusCode;
        res.setStatus(statusCode);
    }

    public Node responseXml(Document doc) throws ProgrammerException, UserException {
        Element responseElement = doc.createElement("response");
        for (Map.Entry<String, Object> entry : responseHeaders.entrySet()) {
            Element headerElement = doc.createElement("header");
            headerElement.setAttribute("name", entry.getKey());
            headerElement.setAttribute("value", entry.getValue().toString());
            responseElement.appendChild(headerElement);
        }
        responseElement.setAttribute("status-code", Integer.toString(responseStatusCode));
        return responseElement;
    }

    public void setResponseHeader(String name, String value) {
        res.setHeader(name, value);
        responseHeaders.put(name, value);
    }

    @SuppressWarnings("unchecked")
    public Node requestXml(Document doc) throws ProgrammerException, UserException, DesignerException {
        Element requestElement = doc.createElement("request");
        Map<String, String[]> parameterMap = getRequest().getParameterMap();
        requestElement.setAttribute("context-path", getRequest().getContextPath());
        requestElement.setAttribute("path-info", getRequest().getPathInfo());
        requestElement.setAttribute("method", getRequest().getMethod().toLowerCase());
        requestElement.setAttribute("server-name", getRequest().getServerName());
        for (Entry<String, String[]> entry : parameterMap.entrySet()) {
            requestElement.appendChild(new HttpParameter(new ParameterName(entry.getKey()), entry.getValue()).toXML(doc));
        }
        if (ServletFileUpload.isMultipartContent(new ServletRequestContext(req))) {
            for (Iterator it = multipartItems.iterator(); it.hasNext(); ) {
                FileItem item = (FileItem) it.next();
                requestElement.appendChild(new HttpParameter(new ParameterName(item.getFieldName()), item.isFormField() ? item.getString() : item.getName()).toXML(doc));
            }
        }
        for (MonadInstantiationException e : instantiationExceptions) {
            requestElement.appendChild(e.toXML(doc));
        }
        return requestElement;
    }

    public FileItem getFileItem(String name) throws ProgrammerException {
        FileItem fileItem = null;
        for (FileItem item : multipartItems) {
            if (!item.isFormField() && item.getFieldName().equals(name)) {
                fileItem = item;
            }
        }
        if (fileItem == null) {
            instantiationExceptions.add(new MonadInstantiationException(FileItem.class.getName(), name, UserException.newInvalidParameter("File parameter '" + name + "' is required.")));
        }
        return fileItem;
    }

    public List<HttpParameter> getParameters(ParameterName[] parameterNames) throws UserException, ProgrammerException {
        List<HttpParameter> parameters = new ArrayList<HttpParameter>();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                String[] parameterValues = getParameterValues(parameterNames[i]);
                if (parameterValues == null || parameterValues.length < 1) {
                    throw UserException.newInvalidParameter(new VFMessage("The parameter '" + parameterNames[i] + "' is required.", new VFParameter[] { new VFParameter("code", VFMessage.PARAMETER_REQUIRED), new VFParameter("name", parameterNames[i].toString()) }));
                } else if (parameterValues.length > 1) {
                    throw UserException.newInvalidParameter(new VFMessage("Too many parameter values.", VFMessage.TOO_MANY_PARAMETER_VALUES, "name", parameterNames[i].toString()));
                }
                parameters.add(new HttpParameter(parameterNames[i], parameterValues[0]));
            }
        }
        return parameters;
    }

    public void sendCreated(MonadUri uri) throws ProgrammerException, DesignerException, DeployerException, UserException {
        sendCreated(null, uri);
    }

    public void sendBadRequest(String message) throws ProgrammerException, DesignerException, DeployerException, UserException {
        try {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
    }

    public void sendFound(MonadString uri) throws ProgrammerException, DesignerException, DeployerException, UserException {
        sendFound(uri.toString());
    }

    public void sendFound(String uri) throws ProgrammerException, DesignerException, DeployerException, UserException {
        URI locationUri;
        try {
            locationUri = new URI(req.getScheme(), null, req.getServerName(), req.getServerPort(), req.getContextPath() + uri, null, null);
            setResponseHeader("Location", locationUri.toString());
            res.sendError(HttpServletResponse.SC_FOUND);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        } catch (URISyntaxException e) {
            throw new ProgrammerException(e);
        }
    }

    public void sendCreated(Document doc, MonadUri uri) throws ProgrammerException, DesignerException, DeployerException, UserException {
        URI locationUri;
        try {
            locationUri = new URI(req.getScheme(), null, req.getServerName(), req.getServerPort(), req.getContextPath() + uri.toString(), null, null);
        } catch (URISyntaxException e) {
            throw new ProgrammerException(e);
        }
        setResponseHeader("Location", locationUri.toString());
        setResponseStatusCode(HttpServletResponse.SC_CREATED);
        returnPage(doc, req.getPathInfo(), "template.xsl");
    }

    public void sendMovedPermanently(URI location) throws ProgrammerException {
        res.setHeader("Location", location.toString());
        try {
            res.sendError(HttpServletResponse.SC_MOVED_PERMANENTLY);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
    }

    public HttpMethod getMethod() throws UserException, ProgrammerException {
        String method = req.getMethod();
        if (method.equals("GET")) {
            return HttpMethod.GET;
        } else if (method.equals("POST")) {
            return HttpMethod.POST;
        } else if (method.equals("DELETE")) {
            return HttpMethod.DELETE;
        } else {
            throw UserException.newNotImplemented();
        }
    }

    public void sendInvalidParameter(Document doc) throws UserException, ProgrammerException, DesignerException, DeployerException {
        res.setStatus(418);
        returnPage(doc, req.getPathInfo(), "template.xsl");
    }

    public void sendOk() throws UserException, ProgrammerException, DesignerException, DeployerException {
        sendOk(null);
    }

    public void sendOk(Document doc, String templatePath, String templateName) throws DesignerException, ProgrammerException, DeployerException, UserException {
        res.setStatus(HttpServletResponse.SC_OK);
        returnPage(doc, templatePath, templateName);
    }

    public void sendOk(Document doc) throws DesignerException, ProgrammerException, DeployerException, UserException {
        String templatePath = req.getPathInfo();
        if (templatePath == null) {
            templatePath = "/";
        }
        sendOk(doc, templatePath, "template.xsl");
    }

    public void sendForbidden() throws ProgrammerException {
        try {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
    }

    public void sendNotFound() throws ProgrammerException {
        try {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
    }

    public void sendUnauthorized() throws ProgrammerException {
        res.setHeader("WWW-Authenticate", "Basic realm=\"" + monad.getRealmName() + "\"");
        try {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
    }

    public void sendMethodNotAllowed(HttpMethod[] methods) throws ProgrammerException {
        try {
            StringBuilder methodsString = new StringBuilder();
            for (int i = 0; i < methodsString.length(); i++) {
                if (i != 0) {
                    methodsString.append(", ");
                }
                methodsString.append(methods[i]);
            }
            res.setHeader("Allow", methodsString.toString());
            res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
    }

    public void sendSeeOther(MonadUri uri) throws ProgrammerException {
        try {
            URI locationUri = new URI(req.getScheme(), null, req.getServerName(), req.getServerPort(), req.getContextPath() + uri.toString(), null, null);
            res.setHeader("Location", locationUri.toString());
            res.sendError(HttpServletResponse.SC_SEE_OTHER);
        } catch (IOException e) {
            throw new ProgrammerException(e);
        } catch (URISyntaxException e) {
            throw new ProgrammerException(e);
        }
    }

    public User getUser() throws ProgrammerException, UserException {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic")) {
            return null;
        }
        String[] usernameAndPassword = Base64.decode(authHeader.substring(6)).split(":");
        if (usernameAndPassword.length != 2) {
            return null;
        }
        User user = Chellow.USERS_INSTANCE.findUser(new EmailAddress(usernameAndPassword[0]));
        if (user == null) {
            return null;
        } else if (!user.getPassword().getString().equals(usernameAndPassword[1])) {
            return null;
        }
        return user;
    }

    private void returnPage(Document doc, String templatePath, String templateName) throws DesignerException, ProgrammerException, DeployerException, UserException {
        if (doc == null) {
            doc = MonadUtils.newSourceDocument();
        }
        Result result;
        Element source = doc.getDocumentElement();
        if (source == null) {
            throw new ProgrammerException("There is no child element for " + " a document requiring the template 'template'. Request URL: " + getRequest().getRequestURL().toString() + "?" + getRequest().getQueryString());
        }
        source.appendChild(requestXml(doc));
        source.appendChild(responseXml(doc));
        getResponse().setContentType("text/html;charset=us-ascii");
        res.setDateHeader("Date", System.currentTimeMillis());
        res.setHeader("Cache-Control", "no-cache");
        try {
            result = new StreamResult(getResponse().getWriter());
        } catch (IOException e) {
            throw new ProgrammerException(e);
        }
        Monad.returnStream(doc, templatePath, templateName, result);
    }

    public boolean hasParameter(String parameterName) {
        boolean found = req.getParameter(parameterName) != null;
        if (!found && multipartItems != null) {
            for (FileItem item : multipartItems) {
                if (!item.isFormField() && item.getFieldName().equals(parameterName)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    public void returnStatic(ServletContext servletContext, String uri) throws UserException, ProgrammerException {
        InputStream is;
        try {
            res.setDateHeader("Expires", System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            OutputStream os = res.getOutputStream();
            if (uri == null) {
                throw UserException.newNotFound();
            }
            if (uri.endsWith("/")) {
                uri = uri.substring(0, uri.length() - 1);
            }
            int lastSlash = uri.lastIndexOf("/");
            String resourcePath = uri.substring(0, lastSlash + 1);
            URL url;
            Set<String> paths = servletContext.getResourcePaths(resourcePath);
            if (paths == null) {
                throw UserException.newNotFound();
            }
            String potentialPath = null;
            for (String candidatePath : paths) {
                if (!candidatePath.endsWith("/") && candidatePath.startsWith(uri + ".")) {
                    potentialPath = candidatePath;
                    break;
                }
            }
            if (potentialPath == null) {
                throw UserException.newNotFound();
            }
            url = servletContext.getResource(potentialPath);
            if (url == null) {
                throw UserException.newNotFound();
            }
            URLConnection con = url.openConnection();
            String contentType = servletContext.getMimeType(url.toString());
            int c;
            con.connect();
            is = con.getInputStream();
            if (contentType != null) {
                res.setContentType(contentType);
            }
            while ((c = is.read()) != -1) {
                os.write(c);
            }
            os.close();
            is.close();
        } catch (MalformedURLException e) {
            throw UserException.newBadRequest();
        } catch (FileNotFoundException e) {
            throw UserException.newNotFound();
        } catch (IOException e) {
        }
    }

    public Urlable dereferenceUrl() throws UserException, ProgrammerException {
        try {
            String pathInfo = req.getPathInfo();
            return Monad.dereferenceUri(new URI(pathInfo == null ? "/" : pathInfo));
        } catch (URISyntaxException e1) {
            throw UserException.newBadRequest();
        }
    }

    private class MonadInstantiationException extends Exception implements XmlDescriber {

        private static final long serialVersionUID = 1L;

        private VFMessage message = null;

        private String typeName;

        private String label;

        public MonadInstantiationException(String typeName, String label, UserException e) {
            this(typeName, label, e.getVFMessage());
        }

        public MonadInstantiationException(String typeName, String label, VFMessage message) {
            this(typeName, label);
            this.message = message;
        }

        public MonadInstantiationException(String typeName, String label, String messageCode, String parameterName, String parameterValue) {
            this(typeName, label, new VFMessage(messageCode, new VFParameter(parameterName, parameterValue)));
        }

        public MonadInstantiationException(String typeName, String label) {
            this.typeName = typeName;
            this.label = label;
        }

        public MonadInstantiationException(String typeName, String label, String messageCode) {
            this(typeName, label, new VFMessage(messageCode));
        }

        public VFMessage getVFMessage() {
            return message;
        }

        public String getMessage() {
            return message.getDescription();
        }

        public Node toXML(Document doc) throws ProgrammerException, UserException, DesignerException {
            Element element = doc.createElement(typeName);
            if (label != null) {
                element.setAttribute("label", label);
            }
            if (message != null) {
                element.appendChild(message.toXML(doc));
            }
            return element;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Node getXML(XmlTree tree, Document doc) throws ProgrammerException, UserException, DesignerException {
            return toXML(doc);
        }
    }
}
