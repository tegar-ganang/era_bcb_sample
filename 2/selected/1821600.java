package com.zagile.confluence.plugins.semforms.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.zagile.confluence.plugins.semforms.settings.ZSemanticPluginSettings;

/**
 * Bridges between ZSLayer and Confluence.
 * 
 * @author jneyra, jhuertas
 *
 */
@SuppressWarnings("serial")
public class ZSearchEngineBridgeServlet extends javax.servlet.http.HttpServlet {

    SettingsManager settingsManager;

    private String zRpcServBaseUrl;

    protected static Logger logger = Logger.getLogger(ZSearchEngineBridgeServlet.class);

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        doGet(req, resp);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String queryString = req.getQueryString();
        if (queryString == null) queryString = "";
        Enumeration paramNames = req.getParameterNames();
        Object paramName;
        while (paramNames.hasMoreElements()) {
            paramName = paramNames.nextElement();
            if (!queryString.isEmpty()) queryString += "&";
            queryString += paramName + "=" + URLEncoder.encode(req.getParameter(paramName.toString()));
        }
        Serializable ps = settingsManager.getPluginSettings("com.zagile.confluence.plugins.zsemantic_plugin");
        ZSemanticPluginSettings settings = (ZSemanticPluginSettings) ps;
        zRpcServBaseUrl = settings.getZOntoApiBaseUrl();
        logger.info("REQUESTING: " + zRpcServBaseUrl + "Search?" + queryString);
        URLConnection urlConnection;
        try {
            urlConnection = new URL(zRpcServBaseUrl + "Search?" + queryString).openConnection();
            urlConnection.setRequestProperty("Content-Type", "text/html;charset=UTF-8");
            urlConnection.connect();
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String strLine;
            int b = br.read();
            while (b != -1) {
                resp.getOutputStream().write(b);
                b = br.read();
            }
            is.close();
            resp.getOutputStream().close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }

    public void setSettingsManager(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public SettingsManager getSettingsManager() {
        return this.settingsManager;
    }
}
