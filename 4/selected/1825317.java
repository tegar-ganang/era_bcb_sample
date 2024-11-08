package net.admin4j.ui.servlets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.admin4j.deps.commons.io.IOUtils;
import net.admin4j.util.Admin4jRuntimeException;
import net.admin4j.util.FreemarkerUtils;
import freemarker.template.Template;

/**
 * Base class for Administrative display servlets.
 * @author D. Ashmore
 * @since 1.0
 */
public abstract class AdminDisplayServlet extends Admin4JServlet {

    public static final String ADMIN4J_SESSION_VARIABLE_PREFIX = "Admin4j";

    private static final long serialVersionUID = -7324380011384838424L;

    /**
     * Will generate Freemarker template results and write it to the output stream.
     * @param response
     * @param templateName
     * @param variableMap
     * @throws IOException
     */
    protected void displayFreeMarkerPage(HttpServletRequest request, HttpServletResponse response, String templateName, Map<String, Object> variableMap) throws IOException {
        response.setContentType("text/html");
        displayFreeMarkerResponse(request, response, templateName, variableMap);
    }

    @SuppressWarnings("unchecked")
    protected void displayFreeMarkerResponse(HttpServletRequest request, HttpServletResponse response, String templateName, Map<String, Object> variableMap) throws IOException {
        Enumeration<String> attrNameEnum = request.getSession().getAttributeNames();
        String attrName;
        while (attrNameEnum.hasMoreElements()) {
            attrName = attrNameEnum.nextElement();
            if (attrName != null && attrName.startsWith(ADMIN4J_SESSION_VARIABLE_PREFIX)) {
                variableMap.put("Session" + attrName, request.getSession().getAttribute(attrName));
            }
        }
        variableMap.put("RequestAdmin4jCurrentUri", request.getRequestURI());
        Template temp = FreemarkerUtils.createConfiguredTemplate(this.getClass(), templateName);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try {
            temp.process(variableMap, new OutputStreamWriter(outStream));
            response.setContentLength(outStream.size());
            IOUtils.copy(new ByteArrayInputStream(outStream.toByteArray()), response.getOutputStream());
            response.getOutputStream().flush();
            response.getOutputStream().close();
        } catch (Exception e) {
            throw new Admin4jRuntimeException(e);
        }
    }

    @Override
    public boolean hasDisplay() {
        return true;
    }

    public abstract String getServletLabel();
}
