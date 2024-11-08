package com.volantis.osgi.j2ee.bridge.http.service;

import com.volantis.synergetics.io.IOUtils;
import org.osgi.framework.Bundle;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**
 * A resource registration.
 */
public class ResourceRegistration extends Registration {

    /**
     * The name of the root directory where the resources can be found.
     */
    private final String name;

    /**
     * Initialise.
     *
     * @param bundle         The bundle.
     * @param servletContext The servlet context.
     * @param alias          The alias.
     * @param name           The name of the root directory where the resources
     *                       can be found.
     */
    public ResourceRegistration(Bundle bundle, InternalServletContext servletContext, String alias, String name) {
        super(bundle, servletContext, alias);
        if (name.equals("/")) {
            name = "";
        }
        this.name = name;
        initialised = true;
    }

    protected boolean doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getPathInfo();
        if (!path.startsWith(alias)) {
            throw new ServletException("Path '" + path + "' does not start with registered alias '" + alias + "'");
        }
        String internal;
        if (alias.equals("/")) {
            internal = name + path;
        } else {
            internal = name + path.substring(alias.length(), path.length());
        }
        URL resource = httpContext.getResource(internal);
        if (resource == null) {
            return false;
        }
        String mimeType = servletContext.getMimeType(internal);
        if (mimeType != null) {
            response.setContentType(mimeType);
        }
        InputStream is = resource.openStream();
        OutputStream os = response.getOutputStream();
        IOUtils.copyAndClose(is, os);
        return true;
    }

    protected void doShutDown() {
    }
}
