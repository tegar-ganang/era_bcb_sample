package com.safi.asterisk.handler.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import com.safi.asterisk.handler.classloader.exception.JclException;

/**
 * Class that builds a virtual classpath by loading resources from different files/paths 
 * 
 * @author Kamran Zafar
 * 
 */
public class ClasspathResources extends JarResources {

    /**
     * Reads the resource content
     * 
     * @param resource
     * @throws IOException
     * @throws JclException
     */
    private void loadResourceContent(String resource) throws IOException, JclException {
        File resourceFile = new File(resource);
        FileInputStream fis = new FileInputStream(resourceFile);
        byte[] content = new byte[(int) resourceFile.length()];
        fis.read(content);
        if (jarEntryContents.containsKey(resourceFile.getName())) {
            if (!Configuration.supressCollisionException()) throw new JclException("Resource " + resourceFile.getName() + " already loaded"); else {
                logger.debug("Resource " + resourceFile.getName() + " already loaded; ignoring entry...");
                return;
            }
        }
        fis.close();
        logger.debug("Loading resource: " + resourceFile.getName());
        jarEntryContents.put(resourceFile.getName(), content);
    }

    /**
     * Attempts to load a remote resource (jars, properties files, etc)
     * 
     * @param url
     * @throws IOException
     * @throws JclException
     */
    private void loadRemoteResource(URL url) throws IOException, JclException {
        logger.debug("Attempting to load a remote resource.");
        if (url.toString().toLowerCase().endsWith(".jar")) {
            loadJar(url);
            return;
        }
        InputStream stream = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int byt;
        while (((byt = stream.read()) != -1)) {
            out.write(byt);
        }
        byte[] content = out.toByteArray();
        if (jarEntryContents.containsKey(url.toString())) {
            if (!Configuration.supressCollisionException()) throw new JclException("Resource " + url.toString() + " already loaded"); else {
                logger.debug("Resource " + url.toString() + " already loaded; ignoring entry...");
                return;
            }
        }
        logger.debug("Loading remote resource.");
        jarEntryContents.put(url.toString(), content);
        out.close();
        stream.close();
    }

    /**
     * Reads the class content
     * 
     * @param clazz
     * @param pack
     * @throws JclException
     * @throws IOException
     */
    private void loadClassContent(String clazz, String pack) throws JclException, IOException {
        File cf = new File(clazz);
        FileInputStream fis = new FileInputStream(cf);
        byte[] content = new byte[(int) cf.length()];
        fis.read(content);
        String entryName = pack + "/" + cf.getName();
        if (jarEntryContents.containsKey(entryName)) {
            if (!Configuration.supressCollisionException()) throw new JclException("Class " + entryName + " already loaded"); else {
                logger.debug("Class " + entryName + " already loaded; ignoring entry...");
                return;
            }
        }
        fis.close();
        logger.debug("Loading class: " + entryName);
        jarEntryContents.put(entryName, content);
    }

    /**
     * Reads local and remote resources
     * 
     * @param url
     * @throws IOException
     * @throws JclException
     * @throws URISyntaxException
     */
    public void loadResource(URL url) throws IOException, JclException, URISyntaxException {
        try {
            loadResource(new File(url.toURI()), "");
        } catch (IllegalArgumentException iae) {
            loadRemoteResource(url);
        }
    }

    /**
     * Reads local resources from
     * - Jar files
     * - Class folders
     * - Jar Library folders
     * 
     * @param path
     * @throws IOException
     * @throws JclException
     */
    public void loadResource(String path) throws IOException, JclException {
        logger.debug("Resource: " + path);
        loadResource(new File(path), "");
    }

    /**
     * Reads local resources from
     * - Jar files
     * - Class folders
     * - Jar Library folders
     * 
     * @param fol
     * @param packName
     * @throws IOException
     * @throws JclException
     */
    private void loadResource(File fol, String packName) throws IOException, JclException {
        if (fol.isFile()) {
            if (fol.getName().toLowerCase().endsWith(".class")) {
                loadClassContent(fol.getAbsolutePath(), packName);
            } else {
                if (fol.getName().toLowerCase().endsWith(".jar")) {
                    logger.debug("Loading jar: " + fol.getName());
                    loadJar(fol.getAbsolutePath());
                } else {
                    loadResourceContent(fol.getAbsolutePath());
                }
            }
            return;
        }
        if (fol.list() != null) {
            for (String f : fol.list()) {
                File fl = new File(fol.getAbsolutePath() + "/" + f);
                String pn = packName;
                if (fl.isDirectory()) {
                    if (!pn.equals("")) pn = pn + "/";
                    pn = pn + fl.getName();
                }
                loadResource(fl, pn);
            }
        }
    }
}
