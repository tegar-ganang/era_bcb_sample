package org.apache.velocity.runtime.resource.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Hashtable;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.Log;
import org.apache.velocity.exception.ResourceNotFoundException;

/**
 * A small wrapper around a Jar
 *
 * @author <a href="mailto:daveb@miceda-data.com">Dave Bryson</a>
 * @version $Id: JarHolder.java 471259 2006-11-04 20:26:57Z henning $
 */
public class JarHolder {

    private String urlpath = null;

    private JarFile theJar = null;

    private JarURLConnection conn = null;

    private Log log = null;

    /**
     * @param rs
     * @param urlpath
     */
    public JarHolder(RuntimeServices rs, String urlpath) {
        this.log = rs.getLog();
        this.urlpath = urlpath;
        init();
        if (log.isDebugEnabled()) {
            log.debug("JarHolder: initialized JAR: " + urlpath);
        }
    }

    /**
     *
     */
    public void init() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("JarHolder: attempting to connect to " + urlpath);
            }
            URL url = new URL(urlpath);
            conn = (JarURLConnection) url.openConnection();
            conn.setAllowUserInteraction(false);
            conn.setDoInput(true);
            conn.setDoOutput(false);
            conn.connect();
            theJar = conn.getJarFile();
        } catch (IOException ioe) {
            log.error("JarHolder: error establishing connection to JAR at \"" + urlpath + "\"", ioe);
        }
    }

    /**
     *
     */
    public void close() {
        try {
            theJar.close();
        } catch (Exception e) {
            log.error("JarHolder: error closing the JAR file", e);
        }
        theJar = null;
        conn = null;
        log.trace("JarHolder: JAR file closed");
    }

    /**
     * @param theentry
     * @return The requested resource.
     * @throws ResourceNotFoundException
     */
    public InputStream getResource(String theentry) throws ResourceNotFoundException {
        InputStream data = null;
        try {
            JarEntry entry = theJar.getJarEntry(theentry);
            if (entry != null) {
                data = theJar.getInputStream(entry);
            }
        } catch (Exception fnfe) {
            log.error("JarHolder: getResource() error", fnfe);
            throw new ResourceNotFoundException(fnfe);
        }
        return data;
    }

    /**
     * @return The entries of the jar as a hashtable.
     */
    public Hashtable getEntries() {
        Hashtable allEntries = new Hashtable(559);
        Enumeration all = theJar.entries();
        while (all.hasMoreElements()) {
            JarEntry je = (JarEntry) all.nextElement();
            if (!je.isDirectory()) {
                allEntries.put(je.getName(), this.urlpath);
            }
        }
        return allEntries;
    }

    /**
     * @return The URL path of this jar holder.
     */
    public String getUrlPath() {
        return urlpath;
    }
}
