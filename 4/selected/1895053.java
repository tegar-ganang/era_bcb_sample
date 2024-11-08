package net.guruj;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.shell.*;
import org.mortbay.servlet.*;

public class AskariServlet extends HttpServlet {

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void destroy() {
        super.destroy();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        Context cx = Context.enter();
        try {
            AskariShell shell = new AskariShell(request, response);
            Scriptable scope = cx.initStandardObjects(shell);
            initCustomObjects(shell);
            shell.setDefaultPage(this.getServletConfig().getInitParameter("default.page"));
            PrintWriter out = response.getWriter();
            Enumeration e = request.getParameterNames();
            cx.evaluateString(scope, "var parameters = {};", "Askari", 1, null);
            Scriptable paramsObj = (Scriptable) scope.get("parameters", scope);
            while (e.hasMoreElements()) {
                String paramName = (String) e.nextElement();
                String[] paramValues = request.getParameterValues(paramName);
                try {
                    if (paramValues.length == 1) {
                        paramsObj.put(paramName, paramsObj, paramValues[0]);
                    } else {
                        if (paramValues.length > 1) {
                            paramsObj.put(paramName, paramsObj, Context.toObject(paramValues, scope));
                        }
                    }
                } catch (Exception ex) {
                }
            }
            try {
                shell.processFile(cx, request.getPathTranslated());
            } catch (RhinoException ee) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println(formatError(ee));
            }
        } finally {
            Context.exit();
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse res) throws IOException, ServletException {
        if (!request.getContentType().equals("multipart/form-data")) {
            doGet(request, res);
        } else {
            org.mortbay.servlet.MultiPartRequest multi = new MultiPartRequest(request);
            String[] partNames = multi.getPartNames();
        }
    }

    public void initCustomObjects(AskariShell shell) {
        String[] names = { "defineClass", "loadClass", "serialize", "deserialize", "use", "include", "load", "print", "readFile", "readUrl", "writeFile", "writeUrl", "runCommand", "seal", "spawn", "sync", "persist", "remember", "forget" };
        shell.defineFunctionProperties(names, AskariShell.class, ScriptableObject.DONTENUM);
    }

    private String formatError(RhinoException ee) {
        String heading = new String();
        String detail = new String();
        heading = "At line " + ee.lineNumber() + ", column " + ee.columnNumber() + ":";
        detail = ee.getMessage();
        return ServletError.formatError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, heading, detail);
    }
}
