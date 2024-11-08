package net.sf.bimbo;

import static net.sf.bimbo.impl.HtmlUtil.escape;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sf.bimbo.impl.AccountPage;
import net.sf.bimbo.impl.BimboPrincipal;
import net.sf.bimbo.impl.BooleanRenderer;
import net.sf.bimbo.impl.DateRenderer;
import net.sf.bimbo.impl.DoubleRenderer;
import net.sf.bimbo.impl.FloatRenderer;
import net.sf.bimbo.impl.HtmlElement;
import net.sf.bimbo.impl.IntegerRenderer;
import net.sf.bimbo.impl.LoginPage;
import net.sf.bimbo.impl.StringRenderer;
import net.sf.bimbo.impl.StyleAdviceManager;
import net.sf.bimbo.spi.ConversionException;
import net.sf.bimbo.spi.Renderer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Bimbo web application runtime servlet. This servlet interprets all Bimbo
 * annotations of your web application.
 * 
 * <p>
 * The required init parameter <tt>WelcomePage</tt> indicates the welcome page
 * full class name. All other web application configuration is driven by
 * annotations.
 * </p>
 * 
 * @author fcorneli
 * 
 */
public class BimboServlet extends HttpServlet implements BimboContext {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(BimboServlet.class);

    private Class<?> welcomePageClass;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String welcomePageInitParameter = config.getInitParameter("WelcomePage");
        if (null != welcomePageInitParameter) {
            String welcomePageClassName = welcomePageInitParameter.trim();
            this.welcomePageClass = loadClass(welcomePageClassName);
            StyleAdviceManager.setWelcomePageClass(this.welcomePageClass);
        }
    }

    public static Class<?> loadClass(String className) throws ServletException {
        Thread currentThread = Thread.currentThread();
        ClassLoader classLoader = currentThread.getContextClassLoader();
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ServletException("invalid page class: " + className);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.debug("doGet");
        PrintWriter writer = response.getWriter();
        if (null == this.welcomePageClass) {
            showErrorPage(writer);
        } else {
            Object pageObject = null;
            try {
                pageObject = createPage(this.welcomePageClass, request, response);
            } catch (Exception e) {
                LOG.debug("create page error: " + e.getMessage(), e);
                writer.println("Could not instantiate page class: " + this.welcomePageClass.getName());
            }
            outputPage(this.welcomePageClass, pageObject, request, response);
        }
    }

    private void outputPage(Class<?> pageClass, Object page, HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        HttpSession session = request.getSession();
        outputPage(pageClass, page, writer, session);
    }

    private void showErrorPage(PrintWriter writer) {
        writer.println("<html>");
        {
            writer.println("<body>");
            {
                writer.println("<p>Not configured</p>");
            }
            writer.println("</body>");
        }
        writer.println("</html>");
    }

    private void outputPage(Class<?> pageClass, Object page, PrintWriter writer, HttpSession session) {
        outputPage(pageClass, page, session, null, new HashMap<String, String>(), writer);
    }

    private String webappTitle = null;

    private void outputPage(Class<?> pageClass, Object page, HttpSession session, String message, Map<String, String> constraintViolations, PrintWriter writer) {
        loadWebAppTitle(pageClass);
        StyleAdviceManager styleAdviceManager = new StyleAdviceManager(pageClass);
        writer.println("<html>");
        writeHead(writer);
        writeBody(pageClass, page, session, message, constraintViolations, writer, styleAdviceManager);
        writer.println("</html>");
    }

    private void writeBody(Class<?> pageClass, Object page, HttpSession session, String message, Map<String, String> constraintViolations, PrintWriter writer, StyleAdviceManager styleAdviceManager) {
        writer.println("<body>");
        {
            outputJavascript(pageClass, writer);
            outputWebAppTitle(writer, styleAdviceManager);
            outputLoginBox(session, writer);
            outputPageTitle(pageClass, writer, styleAdviceManager);
            outputGlobalActionForm(pageClass, writer);
            outputActionForm(pageClass, page, message, constraintViolations, writer);
        }
        writer.println("</body>");
    }

    private void outputActionForm(Class<?> pageClass, Object page, String message, Map<String, String> constraintViolations, PrintWriter writer) {
        writer.println("<form name=\"ActionForm\" action=\"" + pageClass.getSimpleName() + ".bimbo\" method=\"POST\">");
        {
            new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", "PageClass").addAttribute("value", pageClass.getName()).write(writer);
            new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", "ActionName").write(writer);
            writePageContent(pageClass, page, constraintViolations, message, writer);
        }
        writer.println("</form>");
    }

    private void outputGlobalActionForm(Class<?> pageClass, PrintWriter writer) {
        writer.println("<form name=\"GlobalActionForm\" action=\"" + pageClass.getSimpleName() + ".bimbo\" method=\"POST\">");
        new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", "GlobalActionName").write(writer);
        writer.println("</form>");
    }

    private void loadWebAppTitle(Class<?> pageClass) {
        Package pagePackage = pageClass.getPackage();
        if (null == pagePackage) {
            return;
        }
        Title packageTitle = pagePackage.getAnnotation(Title.class);
        if (null == packageTitle) {
            return;
        }
        this.webappTitle = packageTitle.value();
    }

    private void outputPageTitle(Class<?> pageClass, PrintWriter writer, StyleAdviceManager styleAdviceManager) {
        String pageTitle = getTitle(pageClass);
        HtmlElement h2Element = new HtmlElement("h2").setBody(pageTitle);
        h2Element.addAttribute("style", styleAdviceManager.getPageTitleStyle());
        h2Element.write(writer);
    }

    private void outputLoginBox(HttpSession session, PrintWriter writer) {
        String username = (String) session.getAttribute(USERNAME_SESSION_ATTRIBUTE);
        String identityContent;
        if (null == username) {
            identityContent = "Welcome, Guest";
        } else {
            identityContent = "Welcome, " + "<a href=\"javascript:doGlobalAction('account');\">" + username + "</a>&nbsp;" + "<a href=\"javascript:doGlobalAction('logout');\">Logout</a>";
        }
        identityContent += "&nbsp;<a href=\"javascript:doGlobalAction('home');\">Home</a>";
        new HtmlElement("div").addAttribute("style", "position: absolute; right: 0%; text-align: right; background-color: #e0e0e0; border-style: solid; border-width: 1px; border-color: black; padding-left: 5px; padding-right: 5px;").setEscapedBody(identityContent).write(writer);
    }

    private void outputWebAppTitle(PrintWriter writer, StyleAdviceManager styleAdviceManager) {
        if (null == this.webappTitle) {
            return;
        }
        String style = styleAdviceManager.getApplicationTitleStyle();
        new HtmlElement("h1").setBody(this.webappTitle).addAttribute("style", style).write(writer);
    }

    private void writeHead(PrintWriter writer) {
        writer.println("<head>");
        {
            if (null != this.webappTitle) {
                new HtmlElement("title").setBody(webappTitle).write(writer);
            }
            new HtmlElement("meta").addAttribute("name", "Identifier").addAttribute("content", UUID.randomUUID().toString()).write(writer);
        }
        writer.println("</head>");
    }

    private void outputJavascript(Class<?> pageClass, PrintWriter writer) {
        writer.println("<script type=\"text/javascript\">");
        {
            writer.println("function doAction(name) {");
            writer.println("\tdocument.ActionForm.ActionName.value = name;");
            writer.println("\tdocument.ActionForm.submit();");
            writer.println("}");
            writer.println();
            writer.println("function doConfirmAction(name, confirmation) {");
            writer.println("\tvar answer = confirm(confirmation);");
            writer.println("\tif (answer) {");
            writer.println("\t\tdocument.ActionForm.ActionName.value = name;");
            writer.println("\t\tdocument.ActionForm.submit();");
            writer.println("\t}");
            writer.println("}");
            writer.println();
            writer.println("function doGlobalAction(name) {");
            writer.println("\tdocument.GlobalActionForm.GlobalActionName.value = name;");
            writer.println("\tdocument.GlobalActionForm.submit();");
            writer.println("}");
            writer.println();
            writer.println("var req = false;");
            writer.println("function doAjaxAction(name) {");
            {
                writer.println("\tif (window.XMLHttpRequest) {");
                {
                    writer.println("\t\ttry {req = new XMLHttpRequest();} catch(e) {}");
                    writer.println("\t} else {");
                    writer.println("\t\ttry {");
                    writer.println("\t\t\treq = new ActiveXObject('Msxml2.XMLHTTP');");
                    writer.println("\t\t} catch(e) {");
                    writer.println("\t\t\ttry {req = new ActiveXObject('Microsoft.XMLHTTP');} catch(e) {}");
                    writer.println("\t\t}");
                }
                writer.println("\t}");
            }
            writer.println("\tif (req) {");
            writer.println("\t\tdocument.getElementById('bimbo.ajax.status').innerHTML = 'Processing...';");
            writer.println("\t\tvar params='AjaxActionName=' + escape(name);");
            writer.println("\t\treq.onreadystatechange = processAjaxResponse;");
            writer.println("\t\treq.open('POST', 'ajax.bimbo', true);");
            writer.println("\t\treq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');");
            writer.println("\t\treq.setRequestHeader('Content-Length', params.length);");
            writer.println("\t\treq.setRequestHeader('Connection', 'close');");
            writer.println("\t\treq.send(params);");
            writer.println("\t}");
            writer.println("}");
            writer.println();
            writer.println("function processAjaxResponse() {");
            writer.println("\tif (req.readyState == 4) {");
            writer.println("\t\tif (req.status == 200) {");
            writer.println("\t\t\tdocument.getElementById('bimbo.ajax.status').innerHTML = '';");
            writer.println("\t\t}");
            writer.println("\t}");
            writer.println("}");
        }
        writer.println("</script>");
    }

    private interface GlobalAction {

        void doAction(BimboServlet bimboServlet, HttpServletRequest request, HttpServletResponse response) throws Exception;
    }

    private static final Map<String, GlobalAction> globalActions = new HashMap<String, GlobalAction>();

    static {
        globalActions.put("account", new ShowAccount());
        globalActions.put("logout", new Logout());
        globalActions.put("home", new ShowHome());
    }

    private static class ShowAccount implements GlobalAction {

        public void doAction(BimboServlet bimboServlet, HttpServletRequest request, HttpServletResponse response) throws Exception {
            AccountPage accountPage = bimboServlet.createPage(AccountPage.class, request, response);
            PrintWriter writer = response.getWriter();
            HttpSession session = request.getSession();
            bimboServlet.outputPage(AccountPage.class, accountPage, writer, session);
        }
    }

    private static class Logout implements GlobalAction {

        public void doAction(BimboServlet bimboServlet, HttpServletRequest request, HttpServletResponse response) throws Exception {
            HttpSession session = request.getSession();
            session.removeAttribute(USERNAME_SESSION_ATTRIBUTE);
            Object welcomePage = bimboServlet.createPage(bimboServlet.welcomePageClass, request, response);
            PrintWriter writer = response.getWriter();
            bimboServlet.outputPage(bimboServlet.welcomePageClass, welcomePage, writer, session);
        }
    }

    private static class ShowHome implements GlobalAction {

        public void doAction(BimboServlet bimboServlet, HttpServletRequest request, HttpServletResponse response) throws Exception {
            HttpSession session = request.getSession();
            Object welcomePage = bimboServlet.createPage(bimboServlet.welcomePageClass, request, response);
            PrintWriter writer = response.getWriter();
            bimboServlet.outputPage(bimboServlet.welcomePageClass, welcomePage, writer, session);
        }
    }

    private void handleGlobalAction(String globalActionName, PrintWriter writer, HttpServletRequest request, HttpServletResponse response) {
        LOG.debug("global action: " + globalActionName);
        GlobalAction globalAction = globalActions.get(globalActionName);
        if (null == globalAction) {
            writer.println("unsupported global action: " + globalActionName);
            return;
        }
        try {
            globalAction.doAction(this, request, response);
        } catch (Exception e) {
            writer.println("error executing global action: " + globalActionName);
            return;
        }
    }

    private void handleAjaxAction(String ajaxActionName, PrintWriter writer, HttpServletRequest request, HttpServletResponse response) {
        LOG.debug("ajax action: " + ajaxActionName);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LOG.debug("doPost");
        PrintWriter writer = response.getWriter();
        String globalActionName = request.getParameter("GlobalActionName");
        if (null != globalActionName) {
            handleGlobalAction(globalActionName, writer, request, response);
            return;
        }
        String ajaxActionName = request.getParameter("AjaxActionName");
        if (null != ajaxActionName) {
            handleAjaxAction(ajaxActionName, writer, request, response);
            return;
        }
        String pageClassName = request.getParameter("PageClass");
        LOG.debug("page class: " + pageClassName);
        if (null == pageClassName) {
            writer.println("need a PageClass parameter");
            return;
        }
        Class<?> pageClass = loadClass(pageClassName);
        String actionName = request.getParameter("ActionName");
        LOG.debug("action name: " + actionName);
        if (null == actionName) {
            writer.println("need an ActionName parameter");
            return;
        }
        Method actionMethod;
        if (-1 != actionName.indexOf("(")) {
            LOG.debug("table action requested");
            String tableFieldName = actionName.substring(actionName.indexOf("(") + 1, actionName.indexOf("."));
            LOG.debug("table field name: " + tableFieldName);
            Field tableField;
            try {
                tableField = pageClass.getDeclaredField(tableFieldName);
            } catch (Exception e) {
                writer.println("field not found: " + tableFieldName);
                return;
            }
            if (false == List.class.equals(tableField.getType())) {
                writer.println("field is not a list: " + tableFieldName);
                return;
            }
            Integer tableActionIdx = Integer.parseInt(actionName.substring(actionName.indexOf(".") + 1, actionName.indexOf(")")));
            LOG.debug("table action idx: " + tableActionIdx);
            String tableEntryType = request.getParameter(tableFieldName + "." + tableActionIdx);
            LOG.debug("table entry type: " + tableEntryType);
            Class<?> tableEntryClass = loadClass(tableEntryType);
            String actionMethodName = actionName.substring(0, actionName.indexOf("("));
            try {
                actionMethod = pageClass.getDeclaredMethod(actionMethodName, new Class[] { tableEntryClass });
            } catch (Exception e) {
                LOG.debug("no action method found for name: " + actionMethodName);
                writer.println("no action method found for name: " + actionMethodName);
                return;
            }
        } else {
            try {
                actionMethod = pageClass.getDeclaredMethod(actionName, new Class[] {});
            } catch (Exception e) {
                writer.println("no action method found for name: " + actionName);
                return;
            }
        }
        Action actionAnnotation = actionMethod.getAnnotation(Action.class);
        if (null == actionAnnotation) {
            writer.println("action method not annotated with @Action: " + actionName);
            return;
        }
        HttpSession session = request.getSession();
        Object page;
        try {
            page = createPage(pageClass, request, response);
        } catch (Exception e) {
            writer.println("could not init page: " + pageClassName + ". Missing default constructor?");
            return;
        }
        Map<String, String> conversionErrors;
        try {
            conversionErrors = restorePageFromRequest(request, pageClass, page);
        } catch (Exception e) {
            LOG.debug("error on restore: " + e.getMessage(), e);
            throw new ServletException("error on restore: " + e.getMessage(), e);
        }
        if (false == conversionErrors.isEmpty()) {
            LOG.debug("conversion errors: " + conversionErrors);
            outputPage(pageClass, page, session, null, conversionErrors, writer);
            return;
        }
        if (false == actionAnnotation.skipConstraints()) {
            Map<String, String> constraintViolationFieldNames = checkConstraints(pageClass, page);
            if (false == constraintViolationFieldNames.isEmpty()) {
                outputPage(pageClass, page, session, null, constraintViolationFieldNames, writer);
                return;
            }
        }
        Authenticated authenticatedAnnotation = actionMethod.getAnnotation(Authenticated.class);
        if (null != authenticatedAnnotation) {
            LOG.debug("authentication required");
            String username = (String) session.getAttribute(USERNAME_SESSION_ATTRIBUTE);
            if (null == username) {
                LOG.debug("login required");
                LoginPage loginPage;
                try {
                    loginPage = createPage(LoginPage.class, request, response);
                } catch (Exception e) {
                    writer.println("could not init login page");
                    return;
                }
                LoginPage.saveActionPage(page, actionName, session);
                outputPage(LoginPage.class, loginPage, writer, session);
                return;
            }
        }
        Object actionParam = null;
        if (-1 != actionName.indexOf("(")) {
            LOG.debug("table action requested");
            String tableFieldName = actionName.substring(actionName.indexOf("(") + 1, actionName.indexOf("."));
            LOG.debug("table field name: " + tableFieldName);
            Field tableField;
            try {
                tableField = pageClass.getDeclaredField(tableFieldName);
            } catch (Exception e) {
                writer.println("field not found: " + tableFieldName);
                return;
            }
            if (false == List.class.equals(tableField.getType())) {
                writer.println("field is not a list: " + tableFieldName);
                return;
            }
            Integer tableActionIdx = Integer.parseInt(actionName.substring(actionName.indexOf(".") + 1, actionName.indexOf(")")));
            tableField.setAccessible(true);
            List<?> table;
            try {
                table = (List<?>) tableField.get(page);
            } catch (Exception e) {
                writer.println("error reading list: " + tableFieldName);
                return;
            }
            actionParam = table.get(tableActionIdx);
        }
        performAction(page, actionName, actionParam, session, writer, request, response);
    }

    public void performAction(Object page, String actionName, Object actionParam, HttpSession session, PrintWriter writer, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Class<?> pageClass = page.getClass();
        Method actionMethod;
        if (null != actionParam) {
            Class<?> actionParamType = actionParam.getClass();
            try {
                actionMethod = pageClass.getDeclaredMethod(actionName.substring(0, actionName.indexOf("(")), new Class[] { actionParamType });
            } catch (Exception e) {
                LOG.debug("no action method found for name: " + actionName + " and param type " + actionParamType.getName());
                writer.println("no action method found for name: " + actionName + " and param type " + actionParamType.getName());
                return;
            }
        } else {
            try {
                actionMethod = pageClass.getDeclaredMethod(actionName, new Class[] {});
            } catch (Exception e) {
                writer.println("no action method found for name: " + actionName);
                return;
            }
        }
        Action actionAnnotation = actionMethod.getAnnotation(Action.class);
        if (null == actionAnnotation) {
            writer.println("action method not annotated with @Action: " + actionName);
            return;
        }
        Object resultPage;
        try {
            if (null == actionParam) {
                resultPage = actionMethod.invoke(page, new Object[] {});
            } else {
                resultPage = actionMethod.invoke(page, new Object[] { actionParam });
            }
        } catch (InvocationTargetException e) {
            String actionLabel;
            if ("".equals(actionAnnotation.value())) {
                actionLabel = actionMethod.getName();
            } else {
                actionLabel = actionAnnotation.value();
            }
            Throwable targetException = e.getTargetException();
            Map<String, String> constraintMessages = blameInputFields(page, targetException);
            LOG.debug("# blamed fields: " + constraintMessages.size());
            String message;
            if (constraintMessages.isEmpty()) {
                message = actionLabel + " Error: " + targetException.getMessage();
                LOG.debug("error message: " + message);
            } else {
                LOG.debug("contraint messages: " + constraintMessages);
                message = null;
            }
            outputPage(pageClass, page, session, message, constraintMessages, writer);
            return;
        } catch (Exception e) {
            LOG.debug("error invoking action: " + e.getMessage());
            LOG.debug("exception type: " + e.getClass().getName());
            writer.println("error invoking action: " + actionName);
            return;
        } finally {
            saveSessionAttribute(pageClass, page, session);
        }
        if (null == resultPage) {
            return;
        }
        Class<?> resultPageClass = resultPage.getClass();
        try {
            injectResources(resultPageClass, request, response, resultPage);
            injectSessionAttributes(resultPageClass, request, resultPage);
            executePostConstruct(resultPageClass, resultPage);
        } catch (Exception e) {
            LOG.debug("error: " + e.getMessage());
            LOG.debug("exception type: " + e.getClass().getName());
            writer.println(e.getMessage());
            return;
        }
        LOG.debug("result page class: " + resultPageClass.getSimpleName());
        outputPage(resultPageClass, resultPage, writer, session);
    }

    private Map<String, String> blameInputFields(Object page, Throwable exception) {
        Map<String, String> constraintMessages = new HashMap<String, String>();
        Class<?> pageClass = page.getClass();
        Field[] fields = pageClass.getDeclaredFields();
        LOG.debug("exception type: " + exception.getClass().getName());
        for (Field field : fields) {
            Input inputAnnotation = field.getAnnotation(Input.class);
            if (null == inputAnnotation) {
                continue;
            }
            BlameMe blameMeAnnotation = field.getAnnotation(BlameMe.class);
            if (null == blameMeAnnotation) {
                continue;
            }
            LOG.debug("blame input field found: " + field.getName());
            for (Class<? extends Exception> exceptionClass : blameMeAnnotation.value()) {
                if (exceptionClass.equals(exception.getClass())) {
                    String fieldName = field.getName();
                    LOG.debug(fieldName + ": blame me for exception: " + exceptionClass.getName());
                    constraintMessages.put(fieldName, exception.getMessage());
                }
            }
        }
        return constraintMessages;
    }

    private void saveSessionAttribute(Class<?> pageClass, Object page, HttpSession session) {
        Field[] fields = pageClass.getDeclaredFields();
        for (Field field : fields) {
            SessionAttribute sessionAttributeAnnotation = field.getAnnotation(SessionAttribute.class);
            if (null == sessionAttributeAnnotation) {
                continue;
            }
            String attributeName = sessionAttributeAnnotation.value();
            if ("".equals(attributeName)) {
                attributeName = field.getName();
            }
            field.setAccessible(true);
            Object attributeValue = null;
            try {
                attributeValue = field.get(page);
            } catch (Exception e) {
                LOG.debug("error reading session attribute field: " + field.getName());
            }
            LOG.debug("saving session attribute:" + attributeName);
            session.setAttribute(attributeName, attributeValue);
        }
    }

    public static final String USERNAME_SESSION_ATTRIBUTE = BimboServlet.class.getName() + ".USERNAME";

    public <T> T createPage(Class<T> pageClass, HttpServletRequest request, HttpServletResponse response) throws InstantiationException, IllegalAccessException, IllegalArgumentException, NamingException, ServletException {
        LOG.debug("create page: " + pageClass.getSimpleName());
        T page = pageClass.newInstance();
        injectResources(pageClass, request, response, page);
        injectSessionAttributes(pageClass, request, page);
        executePostConstruct(pageClass, page);
        return page;
    }

    private void injectSessionAttributes(Class<?> pageClass, HttpServletRequest request, Object page) throws IllegalArgumentException, IllegalAccessException {
        Field[] fields = pageClass.getDeclaredFields();
        HttpSession session = null;
        for (Field field : fields) {
            SessionAttribute sessionAttributeAnnotation = field.getAnnotation(SessionAttribute.class);
            if (null == sessionAttributeAnnotation) {
                continue;
            }
            String attributeName = sessionAttributeAnnotation.value();
            if ("".equals(attributeName)) {
                attributeName = field.getName();
            }
            if (null == session) {
                session = request.getSession();
            }
            Object attributeValue = session.getAttribute(attributeName);
            field.setAccessible(true);
            LOG.debug("restore session attribute: " + attributeName);
            field.set(page, attributeValue);
        }
    }

    private void executePostConstruct(Class<?> pageClass, Object page) throws ServletException {
        Method[] methods = pageClass.getDeclaredMethods();
        for (Method method : methods) {
            PostConstruct postConstructAnnotation = method.getAnnotation(PostConstruct.class);
            if (null == postConstructAnnotation) {
                continue;
            }
            try {
                LOG.debug("invoking @PostConstruct: " + method.getName());
                method.setAccessible(true);
                method.invoke(page, new Object[] {});
            } catch (Exception e) {
                LOG.debug("error invoking postcontruct method: " + e.getMessage(), e);
                throw new ServletException("Could not execute PostConstruct method: " + method.getName());
            }
        }
    }

    public static interface ResourceProvider<T> {

        T getResource(HttpServletRequest request, HttpServletResponse response, BimboContext context);
    }

    public static class HttpSessionResourceProvider implements ResourceProvider<HttpSession> {

        @Override
        public HttpSession getResource(HttpServletRequest request, HttpServletResponse response, BimboContext context) {
            HttpSession session = request.getSession();
            return session;
        }
    }

    public static class HttpServletRequestResourceProvider implements ResourceProvider<HttpServletRequest> {

        @Override
        public HttpServletRequest getResource(HttpServletRequest request, HttpServletResponse response, BimboContext context) {
            return request;
        }
    }

    public static class HttpServletResponseResourceProvider implements ResourceProvider<HttpServletResponse> {

        @Override
        public HttpServletResponse getResource(HttpServletRequest request, HttpServletResponse response, BimboContext context) {
            return response;
        }
    }

    public static class PrincipalResourceProvider implements ResourceProvider<Principal> {

        @Override
        public Principal getResource(HttpServletRequest request, HttpServletResponse response, BimboContext context) {
            HttpSession session = request.getSession();
            String username = (String) session.getAttribute(USERNAME_SESSION_ATTRIBUTE);
            if (null == username) {
                username = "anonymous";
            }
            BimboPrincipal principal = new BimboPrincipal(username);
            return principal;
        }
    }

    public static class BimboContextResourceProvider implements ResourceProvider<BimboContext> {

        @Override
        public BimboContext getResource(HttpServletRequest request, HttpServletResponse response, BimboContext context) {
            return context;
        }
    }

    private static final Map<Class<?>, ResourceProvider<?>> typedResourceProvider = new HashMap<Class<?>, ResourceProvider<?>>();

    static {
        typedResourceProvider.put(HttpSession.class, new HttpSessionResourceProvider());
        typedResourceProvider.put(HttpServletRequest.class, new HttpServletRequestResourceProvider());
        typedResourceProvider.put(HttpServletResponse.class, new HttpServletResponseResourceProvider());
        typedResourceProvider.put(Principal.class, new PrincipalResourceProvider());
        typedResourceProvider.put(BimboContext.class, new BimboContextResourceProvider());
    }

    private void injectResources(Class<?> pageClass, HttpServletRequest request, HttpServletResponse response, Object page) throws NamingException, IllegalArgumentException, IllegalAccessException, ServletException {
        Field[] fields = pageClass.getDeclaredFields();
        InitialContext initialContext = null;
        for (Field field : fields) {
            Resource resourceAnnotation = field.getAnnotation(Resource.class);
            if (null == resourceAnnotation) {
                continue;
            }
            String resourceName = resourceAnnotation.name();
            if (false == "".equals(resourceName)) {
                if (null == initialContext) {
                    initialContext = new InitialContext();
                }
                Object resource;
                try {
                    resource = initialContext.lookup(resourceName);
                } catch (NoInitialContextException e) {
                    LOG.debug("no initial context: " + e.getMessage());
                    throw new ServletException("no initial context");
                }
                if (null == resource) {
                    throw new ServletException("resource " + resourceName + " is null");
                }
                LOG.debug("injecting resource " + resourceName + " into field " + field.getName());
                field.setAccessible(true);
                field.set(page, resource);
                continue;
            }
            ResourceProvider<?> resourceProvider = typedResourceProvider.get(field.getType());
            if (null == resourceProvider) {
                LOG.debug("no resource provider found for type: " + field.getType().getName());
                throw new ServletException("no resource provider found for type: " + field.getType().getName());
            }
            Object resource = resourceProvider.getResource(request, response, this);
            field.setAccessible(true);
            field.set(page, resource);
        }
    }

    /**
	 * @param pageClass
	 * @param page
	 * @return map with constraint violation error messages per field.
	 * @throws ServletException
	 */
    private Map<String, String> checkConstraints(Class<?> pageClass, Object page) throws ServletException {
        Map<String, String> constraintViolations = new HashMap<String, String>();
        Field[] fields = pageClass.getDeclaredFields();
        LOG.debug("check constraints: " + pageClass.getSimpleName());
        for (Field field : fields) {
            Constraint constraintAnnotation = field.getAnnotation(Constraint.class);
            if (null == constraintAnnotation) {
                continue;
            }
            if (constraintAnnotation.required()) {
                LOG.debug("constraint check for field: " + field.getName());
                field.setAccessible(true);
                Object value;
                try {
                    value = field.get(page);
                } catch (Exception e) {
                    throw new ServletException("could not read field: " + field.getName() + " of type " + field.getType().getName());
                }
                if (String.class.equals(field.getType())) {
                    String strValue = (String) value;
                    if ("".equals(strValue.trim())) {
                        LOG.debug("field " + field.getName() + " is required");
                        constraintViolations.put(field.getName(), "Value required.");
                    }
                } else if (field.getType().isPrimitive()) {
                } else {
                    throw new ServletException("constraint violation: field type not supported: " + field.getType().getName());
                }
            }
        }
        return constraintViolations;
    }

    /**
	 * @param request
	 * @param pageClass
	 * @param page
	 * @return map with conversion error messages per field
	 * @throws ServletException
	 */
    private Map<String, String> restorePageFromRequest(HttpServletRequest request, Class<?> pageClass, Object page) throws ServletException {
        LOG.debug("restore page: " + pageClass.getSimpleName());
        Map<String, String> conversionErrors = new HashMap<String, String>();
        Field[] fields = pageClass.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.FINAL == (field.getModifiers() & Modifier.FINAL)) {
                continue;
            }
            Input inputAnnotation = field.getAnnotation(Input.class);
            Output outputAnnotation = field.getAnnotation(Output.class);
            if (null == inputAnnotation && null == outputAnnotation) {
                continue;
            }
            String fieldName = field.getName();
            LOG.debug("restore field: " + fieldName);
            Render renderAnnotation = field.getAnnotation(Render.class);
            if (null != renderAnnotation) {
                Class<? extends Renderer<?>> rendererClass = renderAnnotation.value();
                Renderer<?> renderer = null;
                try {
                    renderer = rendererClass.newInstance();
                } catch (Exception e) {
                    throw new ServletException("could not init renderer class: " + rendererClass.getName());
                }
                if (null != renderer) {
                    Object value;
                    try {
                        value = renderer.restore(fieldName, request);
                    } catch (ConversionException e) {
                        conversionErrors.put(fieldName, e.getMessage());
                        continue;
                    }
                    field.setAccessible(true);
                    try {
                        field.set(page, value);
                    } catch (Exception e) {
                        throw new ServletException("could not set field: " + fieldName, e);
                    }
                }
                continue;
            }
            if (List.class.equals(field.getType())) {
                restoreTableField(request, field, page);
                continue;
            }
            String fieldPageValue = request.getParameter(fieldName);
            Object fieldValue;
            Class<? extends Renderer<?>> rendererClass = typeRenderers.get(field.getType());
            if (null != rendererClass) {
                Renderer<?> renderer;
                try {
                    renderer = rendererClass.newInstance();
                } catch (Exception e) {
                    throw new ServletException("could not init renderer class: " + rendererClass.getName());
                }
                try {
                    fieldValue = renderer.restore(fieldName, request);
                } catch (ConversionException e) {
                    conversionErrors.put(fieldName, e.getMessage());
                    continue;
                }
            } else if (field.getType().isEnum()) {
                Object[] enumConstants = field.getType().getEnumConstants();
                fieldValue = null;
                for (Object enumConstant : enumConstants) {
                    Enum<?> enumClass = (Enum<?>) enumConstant;
                    if (fieldPageValue.equals(enumClass.name())) {
                        fieldValue = enumConstant;
                        break;
                    }
                }
                if (null == fieldValue) {
                    throw new ServletException("invalid enum value: " + fieldPageValue);
                }
            } else if (hasOutputFields(field.getType())) {
                fieldValue = restoreFromRecord(request, field);
            } else {
                LOG.debug("restore page: field type not supported: " + field.getType());
                throw new ServletException("restore page: field type not supported: " + field.getType());
            }
            field.setAccessible(true);
            try {
                field.set(page, fieldValue);
            } catch (Exception e) {
                LOG.debug("restore: could not set field: " + fieldName);
                throw new ServletException("restore: could not set field: " + fieldName);
            }
        }
        LOG.debug("page restored");
        return conversionErrors;
    }

    private void restoreTableField(HttpServletRequest request, Field field, Object page) throws ServletException {
        LOG.debug("table detected");
        String fieldName = field.getName();
        String tableType = request.getParameter(fieldName + ".type");
        List<Object> list = new LinkedList<Object>();
        if (null != tableType) {
            Class<?> tableEntryClass = loadClass(tableType);
            if (String.class.equals(tableEntryClass)) {
                int idx = 0;
                String value;
                while (null != (value = request.getParameter(fieldName + "." + idx))) {
                    list.add(value);
                    idx++;
                }
            } else {
                int idx = 0;
                while (null != (request.getParameter(fieldName + "." + idx))) {
                    Object tableEntry;
                    try {
                        tableEntry = tableEntryClass.newInstance();
                    } catch (Exception e) {
                        LOG.debug("could not init table entry: " + tableEntryClass.getName());
                        throw new ServletException("could not init table entry: " + tableEntryClass.getName());
                    }
                    Field[] tableEntryFields = tableEntryClass.getDeclaredFields();
                    for (Field tableEntryField : tableEntryFields) {
                        if (null != tableEntryField.getAnnotation(Output.class)) {
                            String pageValue = request.getParameter(fieldName + "." + idx + "." + tableEntryField.getName());
                            Object value;
                            if (Integer.TYPE.equals(tableEntryField.getType())) {
                                value = Integer.parseInt(pageValue);
                            } else if (Float.TYPE.equals(tableEntryField.getType())) {
                                value = Float.parseFloat(pageValue);
                            } else if (Double.TYPE.equals(tableEntryField.getType())) {
                                value = Double.parseDouble(pageValue);
                            } else {
                                value = pageValue;
                            }
                            tableEntryField.setAccessible(true);
                            try {
                                tableEntryField.set(tableEntry, value);
                            } catch (Exception e) {
                                LOG.debug("table entry field: could not set field: " + tableEntryField.getName());
                                throw new ServletException("table entry field: could not set field: " + tableEntryField.getName());
                            }
                        }
                    }
                    list.add(tableEntry);
                    idx++;
                }
            }
        }
        field.setAccessible(true);
        try {
            LOG.debug("restore table field: " + fieldName);
            field.set(page, list);
        } catch (Exception e) {
            LOG.debug("restore table field: could not set field: " + fieldName);
            throw new ServletException("restore table field: could not set field: " + fieldName);
        }
    }

    private Object restoreFromRecord(HttpServletRequest request, Field field) throws ServletException {
        Object fieldValue;
        try {
            fieldValue = field.getType().newInstance();
        } catch (Exception e) {
            throw new ServletException("cannot init type: " + field.getType().getName());
        }
        Field[] recordFields = field.getType().getDeclaredFields();
        for (Field recordField : recordFields) {
            Output recordOutputAnnotation = recordField.getAnnotation(Output.class);
            if (null == recordOutputAnnotation) {
                continue;
            }
            LOG.debug("restore " + field.getName() + "." + recordField.getName());
            Class<? extends Renderer<?>> rendererClass = typeRenderers.get(recordField.getType());
            if (null == rendererClass) {
                throw new ServletException("no renderer class for type: " + recordField.getType());
            }
            Renderer<?> renderer;
            try {
                renderer = rendererClass.newInstance();
            } catch (Exception e) {
                throw new ServletException("renderer init error: " + e.getMessage(), e);
            }
            Object recordValue;
            try {
                recordValue = renderer.restore(field.getName() + "." + recordField.getName(), request);
            } catch (ConversionException e) {
                LOG.debug("error restoring from record: " + e.getMessage(), e);
                throw new ServletException("error restoring from record: " + field.getName(), e);
            }
            recordField.setAccessible(true);
            try {
                recordField.set(fieldValue, recordValue);
            } catch (Exception e) {
                throw new ServletException("error on restore record field: " + recordField.getName());
            }
        }
        return fieldValue;
    }

    private void writePageContent(Class<?> pageClass, Object pageObject, Map<String, String> constraintViolations, String errorMessage, PrintWriter writer) {
        LOG.debug("write page content for page: " + pageClass.getSimpleName());
        outputFields(pageClass, pageObject, constraintViolations, writer);
        outputMessages(writer, errorMessage);
        outputActions(pageClass, writer);
    }

    private void outputFields(Class<?> pageClass, Object pageObject, Map<String, String> constraintViolations, PrintWriter writer) {
        writer.println("<table>");
        Field[] fields = pageClass.getDeclaredFields();
        for (Field field : fields) {
            Output outputAnnotation = field.getAnnotation(Output.class);
            if (null != outputAnnotation) {
                writeOutputField(field, outputAnnotation, pageClass, pageObject, writer);
            }
            Input inputAnnotation = field.getAnnotation(Input.class);
            if (null != inputAnnotation) {
                String fieldName = field.getName();
                String constraintViolation;
                if (constraintViolations.containsKey(fieldName)) {
                    constraintViolation = constraintViolations.get(fieldName);
                    if (null == constraintViolation) {
                        constraintViolation = "null";
                    }
                } else {
                    constraintViolation = null;
                }
                writeInputField(field, inputAnnotation, pageObject, constraintViolation, writer);
            }
        }
        writer.println("</table>");
    }

    private void outputActions(Class<?> pageClass, PrintWriter writer) {
        writer.println("<div>");
        Method[] methods = pageClass.getDeclaredMethods();
        for (Method method : methods) {
            Action actionAnnotation = method.getAnnotation(Action.class);
            if (null == actionAnnotation) {
                continue;
            }
            if (0 != method.getParameterTypes().length) {
                continue;
            }
            String methodName = method.getName();
            String actionName = actionAnnotation.value();
            if ("".equals(actionName)) {
                actionName = methodName;
            }
            HtmlElement inputElement = new HtmlElement("input").addAttribute("type", "button").addAttribute("name", methodName).addAttribute("value", actionName);
            String confirmationMessage = actionAnnotation.confirmation();
            Class<?> returnType = method.getReturnType();
            if (Void.TYPE.equals(returnType)) {
                LOG.debug("ajax method");
                inputElement.addAttribute("onclick", "doAjaxAction('" + methodName + "');");
            } else {
                if ("".equals(confirmationMessage)) {
                    inputElement.addAttribute("onclick", "doAction('" + methodName + "');");
                } else {
                    inputElement.addAttribute("onclick", "doConfirmAction('" + methodName + "', '" + confirmationMessage + "');");
                }
            }
            inputElement.write(writer);
        }
        writer.println("</div>");
    }

    private void outputMessages(PrintWriter writer, String message) {
        writer.println("<div id=\"bimbo.ajax.status\"></div>");
        if (null == message) {
            return;
        }
        writer.println("<p>");
        {
            new HtmlElement("div").addAttribute("style", "color: red; background-color: #ffe0e0; display: inline; border-style: solid; border-width: 1px; border-color: red; padding-left: 5px; padding-right: 5px;").setBody(message).write(writer);
        }
        writer.println("</p>");
    }

    private void writeOutputField(Field field, Output outputAnnotation, Class<?> pageClass, Object pageObject, PrintWriter writer) {
        writer.println("<tr>");
        {
            String outputLabel = outputAnnotation.value();
            if (false == "".equals(outputLabel)) {
                new HtmlElement("th").addAttribute("align", "left").addAttribute("style", "background-color: #e0e0e0").setBody(outputLabel + ":").write(writer);
                writer.println("<td>");
            } else {
                writer.println("<td colspan=\"2\">");
            }
            String fieldName = field.getName();
            try {
                field.setAccessible(true);
                Object outputValue = field.get(pageObject);
                Render renderAnnotation = field.getAnnotation(Render.class);
                if (null != renderAnnotation) {
                    Class<? extends Renderer<?>> rendererClass = renderAnnotation.value();
                    Renderer renderer = rendererClass.newInstance();
                    renderer.renderOutput(fieldName, outputValue, writer);
                } else if (outputAnnotation.verbatim()) {
                    new HtmlElement("pre").setBody(outputValue.toString()).write(writer);
                    new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", fieldName).addAttribute("value", outputValue.toString()).write(writer);
                } else if (List.class.equals(field.getType())) {
                    List<?> outputList = (List<?>) outputValue;
                    outputTable(fieldName, outputList, pageClass, writer);
                } else {
                    Class<?> outputClass;
                    if (null != outputValue) {
                        outputClass = outputValue.getClass();
                    } else {
                        outputClass = field.getType();
                    }
                    if (hasOutputFields(outputClass)) {
                        writeOutputRecordField(field, outputValue, outputClass, writer);
                    } else {
                        if (null == outputValue) {
                            outputValue = "";
                        }
                        writer.println(escape(outputValue));
                        new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", fieldName).addAttribute("value", outputValue.toString()).write(writer);
                    }
                }
            } catch (Exception e) {
                LOG.debug("cannot read field " + fieldName + ": " + e.getMessage());
                writer.println("cannot read field: " + fieldName);
            }
            writer.println("</td>");
        }
        writer.println("</tr>");
    }

    private void writeOutputRecordField(Field field, Object outputValue, Class<?> outputClass, PrintWriter writer) throws IllegalAccessException, InstantiationException {
        writer.println("<table>");
        Field[] recordFields = outputClass.getDeclaredFields();
        for (Field recordField : recordFields) {
            Output recordOutputAnnotation = recordField.getAnnotation(Output.class);
            if (null == recordOutputAnnotation) {
                continue;
            }
            writer.println("<tr>");
            {
                String recordLabel = recordOutputAnnotation.value();
                if ("".equals(recordLabel)) {
                    recordLabel = recordField.getName();
                }
                new HtmlElement("th").addAttribute("align", "left").addAttribute("style", "background-color: #e0e0e0;").setBody(recordLabel + ":").write(writer);
                recordField.setAccessible(true);
                Object value = recordField.get(outputValue);
                Class<? extends Renderer<?>> rendererClass = typeRenderers.get(recordField.getType());
                Renderer renderer = rendererClass.newInstance();
                writer.println("<td>");
                renderer.renderOutput(field.getName() + "." + recordField.getName(), value, writer);
                writer.println("</td>");
            }
            writer.println("</tr>");
        }
        writer.println("</table>");
    }

    private static final Map<Class<?>, Class<? extends Renderer<?>>> typeRenderers = new HashMap<Class<?>, Class<? extends Renderer<?>>>();

    static {
        typeRenderers.put(Date.class, DateRenderer.class);
        typeRenderers.put(Boolean.TYPE, BooleanRenderer.class);
        typeRenderers.put(String.class, StringRenderer.class);
        typeRenderers.put(Double.TYPE, DoubleRenderer.class);
        typeRenderers.put(Integer.TYPE, IntegerRenderer.class);
        typeRenderers.put(Float.TYPE, FloatRenderer.class);
    }

    private void writeConstraintViolation(String constraintViolation, String fieldName, PrintWriter writer) {
        if (null == constraintViolation) {
            return;
        }
        new HtmlElement("div").addAttribute("id", "bimbo.error." + fieldName).addAttribute("style", "color: red; background-color: #ffe0e0; display: inline; border-style: solid; border-width: 1px; border-color: red; padding-left: 5px; padding-right: 5px;").setBody(constraintViolation).write(writer);
    }

    private void writeInputField(Field field, Input inputAnnotation, Object pageObject, String constraintViolation, PrintWriter writer) {
        String fieldName = field.getName();
        writer.println("<tr>");
        {
            writer.println("<th style=\"background-color: #e0e0e0;\" align=\"left\">");
            {
                String inputLabel = inputAnnotation.value();
                if ("".equals(inputLabel)) {
                    inputLabel = fieldName;
                }
                writer.println(escape(inputLabel) + ":");
                Constraint constraintAnnotation = field.getAnnotation(Constraint.class);
                if (null != constraintAnnotation) {
                    if (constraintAnnotation.required()) {
                        writer.println("*");
                    }
                }
            }
            writer.println("</th>");
            writer.println("<td>");
            writeInputFieldValue(field, inputAnnotation, pageObject, fieldName, writer);
            writeConstraintViolation(constraintViolation, fieldName, writer);
            writer.println("</td>");
        }
        writer.println("</tr>");
    }

    private void writeInputFieldValue(Field field, Input inputAnnotation, Object pageObject, String fieldName, PrintWriter writer) {
        Object initialValue;
        field.setAccessible(true);
        try {
            initialValue = field.get(pageObject);
        } catch (Exception e) {
            LOG.debug("error reading field: " + field.getName());
            return;
        }
        Render renderAnnotation = field.getAnnotation(Render.class);
        if (null != renderAnnotation) {
            Class<? extends Renderer<?>> rendererClass = renderAnnotation.value();
            Renderer renderer;
            try {
                renderer = rendererClass.newInstance();
            } catch (Exception e) {
                writer.println("could not init renderer class: " + rendererClass.getName());
                return;
            }
            renderer.renderInput(fieldName, initialValue, inputAnnotation, writer);
            return;
        }
        if (typeRenderers.containsKey(field.getType())) {
            Class<? extends Renderer<?>> rendererClass = typeRenderers.get(field.getType());
            Renderer renderer;
            try {
                renderer = rendererClass.newInstance();
            } catch (Exception e) {
                writer.println("could not init render class: " + rendererClass.getName());
                return;
            }
            renderer.renderInput(fieldName, initialValue, inputAnnotation, writer);
            return;
        }
        if (field.getType().isEnum()) {
            writer.println("<select name=\"" + fieldName + "\">");
            Object[] enumConstants = field.getType().getEnumConstants();
            Enum<?> initialEnum = (Enum<?>) initialValue;
            for (Object enumConstant : enumConstants) {
                Enum<?> enumClass = (Enum<?>) enumConstant;
                HtmlElement optionElement = new HtmlElement("option").addAttribute("value", enumClass.name()).setBody(enumConstant.toString());
                if (enumClass == initialEnum) {
                    optionElement.addAttribute("selected", "true");
                }
                optionElement.write(writer);
            }
            writer.println("</select>");
            return;
        }
        if (hasOutputFields(field.getType())) {
            try {
                writeInputRecordField(field, initialValue, writer);
            } catch (Exception e) {
                LOG.debug("error writing input record field: " + field.getName(), e);
                writer.println("error writing input record: " + field.getName());
            }
            return;
        }
        writer.println("Could not render input field: " + fieldName);
    }

    private void writeInputRecordField(Field field, Object outputValue, PrintWriter writer) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
        writer.println("<table>");
        Class<?> outputClass = field.getType();
        Field[] recordFields = outputClass.getDeclaredFields();
        for (Field recordField : recordFields) {
            Output recordOutputAnnotation = recordField.getAnnotation(Output.class);
            if (null == recordOutputAnnotation) {
                continue;
            }
            writer.println("<tr>");
            {
                String recordLabel = recordOutputAnnotation.value();
                if ("".equals(recordLabel)) {
                    recordLabel = recordField.getName();
                }
                new HtmlElement("th").addAttribute("align", "left").addAttribute("style", "background-color: #e0e0e0;").setBody(recordLabel + ":").write(writer);
                recordField.setAccessible(true);
                Object value;
                if (null != outputValue) {
                    value = recordField.get(outputValue);
                } else {
                    value = null;
                }
                Class<? extends Renderer<?>> rendererClass = typeRenderers.get(recordField.getType());
                Renderer renderer = rendererClass.newInstance();
                writer.println("<td>");
                renderer.renderInput(field.getName() + "." + recordField.getName(), value, null, writer);
                writer.println("</td>");
            }
            writer.println("</tr>");
        }
        writer.println("</table>");
    }

    private boolean hasOutputFields(Class<?> outputClass) {
        Field[] fields = outputClass.getDeclaredFields();
        for (Field field : fields) {
            if (null != field.getAnnotation(Output.class)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFields(Class<?> type) {
        if (String.class.equals(type)) {
            return false;
        }
        Field[] fields = type.getDeclaredFields();
        return fields.length != 0;
    }

    private void outputTable(String tableName, List<?> list, Class<?> pageClass, PrintWriter writer) {
        if (null == list) {
            return;
        }
        if (list.isEmpty()) {
            return;
        }
        writer.println("<table style=\"border: 1 px;\">");
        Class<?> listEntryClass = list.get(0).getClass();
        writer.println("<input type=\"hidden\" name=\"" + tableName + ".type\" value=\"" + listEntryClass.getName() + "\"/>");
        if (hasFields(listEntryClass)) {
            writer.println("<tr>");
            {
                Field[] fields = listEntryClass.getDeclaredFields();
                for (Field field : fields) {
                    Output outputAnnotation = field.getAnnotation(Output.class);
                    String label;
                    if (null != outputAnnotation) {
                        label = outputAnnotation.value();
                    } else {
                        label = field.getName();
                    }
                    new HtmlElement("th").addAttribute("style", "background-color: #a0a0a0;").setBody(label).write(writer);
                }
            }
            writer.println("</tr>");
        }
        int size = list.size();
        for (int idx = 0; idx < size; idx++) {
            Object item = list.get(idx);
            writer.println("<tr>");
            {
                {
                    if (hasFields(listEntryClass)) {
                        writer.println("<input type=\"hidden\" name=\"" + tableName + "." + idx + "\" value=\"" + listEntryClass.getName() + "\" />");
                        Field[] fields = listEntryClass.getDeclaredFields();
                        for (Field field : fields) {
                            Object fieldValue;
                            try {
                                field.setAccessible(true);
                                fieldValue = field.get(item);
                            } catch (Exception e) {
                                writer.println("Could not read field: " + field.getName());
                                continue;
                            }
                            HtmlElement tdElement = new HtmlElement("td").setBody(fieldValue.toString());
                            if (0 != idx % 2) {
                                tdElement.addAttribute("style", "background-color: #e0e0e0;");
                            }
                            tdElement.write(writer);
                            new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", tableName + "." + idx + "." + field.getName()).addAttribute("value", fieldValue.toString()).write(writer);
                        }
                    } else {
                        new HtmlElement("td").setBody(item.toString()).write(writer);
                        new HtmlElement("input").addAttribute("type", "hidden").addAttribute("name", tableName + "." + idx).addAttribute("value", item.toString()).write(writer);
                    }
                }
            }
            Method[] methods = pageClass.getDeclaredMethods();
            for (Method method : methods) {
                Action actionAnnotation = method.getAnnotation(Action.class);
                if (null == actionAnnotation) {
                    continue;
                }
                if (1 != method.getParameterTypes().length) {
                    continue;
                }
                Class<?> methodParamType = method.getParameterTypes()[0];
                if (false == methodParamType.equals(listEntryClass)) {
                    continue;
                }
                String actionName = actionAnnotation.value();
                if ("".equals(actionName)) {
                    actionName = method.getName();
                }
                writer.println("<td>");
                {
                    new HtmlElement("input").addAttribute("type", "button").addAttribute("value", actionName).addAttribute("name", tableName + "." + method.getName() + "." + idx).addAttribute("onclick", "doAction('" + method.getName() + "(" + tableName + "." + idx + ")" + "')").write(writer);
                }
                writer.println("</td>");
            }
            writer.println("</tr>");
        }
        writer.println("</table>");
    }

    private String getTitle(Class<?> pageClass) {
        Title titleAnnotation = pageClass.getAnnotation(Title.class);
        if (null == titleAnnotation) {
            return pageClass.getSimpleName();
        }
        return titleAnnotation.value();
    }
}
