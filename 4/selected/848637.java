package org.netbeans.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.netbeans.server.uihandler.api.bugs.BugReporterException;
import org.netbeans.modules.exceptions.utils.PersistenceUtils;

/**
 *
 * @author Jan Horvath
 */
public class Utils {

    private static final Pattern pattern = Pattern.compile("http://([a-zA-Z1-9\\.\\:]+)/");

    public static final String EXCEPTION_DETAIL = "http://statistics.netbeans.org/exceptions/detail.do?id=";

    public Utils() {
    }

    public static String getContextURL(HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String host = request.getHeader("x-forwarded-host");
        String contextURL = url.substring(0, url.lastIndexOf("/"));
        Matcher m = pattern.matcher(url);
        if (m.find() && host != null) {
            contextURL = contextURL.replace(m.group(1), host);
        }
        return contextURL;
    }

    public static Map<String, Object> getParamsFromRequest(HttpServletRequest request) {
        String[] components = request.getParameterValues("component");
        String[] subcomponents = request.getParameterValues("subcomponent");
        String[] os = request.getParameterValues("os");
        String[] vm = request.getParameterValues("vm");
        String[] build = request.getParameterValues("build");
        String username = request.getParameter("username");
        String topduplicates = request.getParameter("topduplicates");
        String reset = request.getParameter("reset");
        Map<String, Object> params = new HashMap<String, Object>();
        boolean newParams = false;
        if (components != null) {
            params.put("component", Arrays.asList(components));
            newParams = true;
        }
        if (subcomponents != null) {
            params.put("subcomponent", Arrays.asList(subcomponents));
            newParams = true;
        }
        if (topduplicates != null) {
            params.put("topduplicates", topduplicates);
            newParams = true;
        }
        if (os != null) {
            params.put("operatingsystem", Arrays.asList(os));
            newParams = true;
        }
        if (build != null) {
            params.put("build", Arrays.asList(build));
            newParams = true;
        }
        if (vm != null) {
            params.put("vm", Arrays.asList(vm));
            newParams = true;
        }
        if ((username != null) && (username.length() > 0)) {
            params.put("username", username);
            newParams = true;
        }
        if (newParams || (reset != null)) {
            request.getSession().setAttribute("params", params);
        } else {
            Object o = request.getSession().getAttribute("params");
            if (o != null) {
                params = (Map) o;
            }
        }
        return params;
    }

    /**
     * This is simple framework for running DB queries from web with EntityManager
     * EntityManager is sure to be closed after this run
     * 
     * @param persistable {@link Persistable Persistable} instance to process
     * @throws Persistance exception if some exception is thrown during processing
     */
    public static void processPersistable(Persistable persistable) {
        EntityManager em = PersistenceUtils.getInstance().createEntityManager();
        EntityTransaction transaction = em.getTransaction();
        if (persistable.needsTransaction()) {
            transaction.begin();
        }
        try {
            Persistable.TransactionResult result = persistable.runQuery(em);
            if (persistable.needsTransaction()) {
                if (result.equals(Persistable.TransactionResult.COMMIT)) {
                    transaction.commit();
                } else {
                    transaction.rollback();
                }
            }
        } catch (BugReporterException exc) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exc;
        } catch (Throwable thrown) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new PersistenceException("Persistable processing failed", thrown);
        } finally {
            em.close();
        }
    }

    private static int MAX_ID_LENGTH = 7;

    public static String getFixedLengthString(int id) {
        String dStr = Integer.toString(id);
        for (int i = dStr.length(); i < MAX_ID_LENGTH; i++) {
            dStr = "0" + dStr;
        }
        return dStr;
    }

    public static Integer getInteger(String s) {
        Integer result = null;
        try {
            result = new Integer(s);
        } catch (NumberFormatException e) {
        }
        return result;
    }

    public static void uploadFile(HttpServletResponse response, File fileName, boolean gunzip) throws IOException {
        if (!gunzip) {
            response.setContentLength(new Long(fileName.length()).intValue());
        }
        OutputStream out = null;
        InputStream is = null;
        byte[] buffer = new byte[512];
        try {
            out = response.getOutputStream();
            is = new FileInputStream(fileName);
            if (gunzip) {
                is = new GZIPInputStream(is);
            }
            int read = is.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = is.read(buffer);
            }
        } finally {
            out.close();
            if (is != null) {
                is.close();
            }
        }
    }
}
