package com.ecyrd.jspwiki.modules;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import org.jdom.Element;
import com.ecyrd.jspwiki.FileUtil;

/**
 *  A WikiModule describes whatever JSPWiki plugin there is: it can be a plugin,
 *  an editor, a filter, etc.
 *
 *  @since 2.4
 */
public class WikiModuleInfo implements Comparable<WikiModuleInfo> {

    protected String m_name;

    protected String m_scriptLocation;

    protected String m_scriptText;

    protected String m_stylesheetLocation;

    protected String m_stylesheetText;

    protected String m_author;

    protected URL m_resource;

    protected String m_minVersion;

    protected String m_maxVersion;

    protected String m_adminBeanClass;

    /**
     *  Create a new info container.
     *  
     *  @param name The name of the module.
     */
    public WikiModuleInfo(String name) {
        m_name = name;
    }

    /**
     *  The WikiModuleInfo is equal to another WikiModuleInfo, if the name is equal.  All
     *  objects are unique across JSPWiki.
     *  
     *  @param obj {@inheritDoc}
     *  @return {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WikiModuleInfo) {
            return ((WikiModuleInfo) obj).m_name.equals(m_name);
        }
        return false;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_name.hashCode();
    }

    /**
     *  Initializes the ModuleInfo from some standard XML elements
     *  which are under the given element.
     *  
     *  @param el The element to parse.
     */
    protected void initializeFromXML(Element el) {
        m_scriptLocation = el.getChildText("script");
        m_stylesheetLocation = el.getChildText("stylesheet");
        m_author = el.getChildText("author");
        m_minVersion = el.getChildText("minVersion");
        m_maxVersion = el.getChildText("maxVersion");
        m_adminBeanClass = el.getChildText("adminBean");
    }

    /**
     *  Returns the AdminBean class which is supposed to manage this module.
     *  
     *  @return A class name.
     */
    public String getAdminBeanClass() {
        return m_adminBeanClass;
    }

    /**
     *  Returns the common name for this particular module.  Note that
     *  this is not the class name, nor is it an alias.  For different modules
     *  the name may have different meanings.
     *  <p>
     *  Every module defines a name, so this method should never return null.
     *  
     *  @return A module name.
     */
    public String getName() {
        return m_name;
    }

    /**
     *  Returns the style sheet location for this module.
     *  
     *  @return The path to the location.
     */
    public String getStylesheetLocation() {
        return m_stylesheetLocation;
    }

    /**
     *  Return the location of the script for this module.
     *  
     *  @return The path to the location.
     */
    public String getScriptLocation() {
        return m_scriptLocation;
    }

    /**
     *  Returns the name of the author of this plugin (if defined).
     * @return Author name, or null.
     */
    public String getAuthor() {
        return m_author;
    }

    /**
     *  Returns the minimum version of JSPWiki that this module supports.
     *  
     *  @return The minimum version.
     */
    public String getMinVersion() {
        return m_minVersion;
    }

    /**
     *  Returns the maximum version of JSPWiki that this module supports.
     *  
     *  @return The maximum version.
     */
    public String getMaxVersion() {
        return m_maxVersion;
    }

    /**
     *  Attempts to locate a resource from a JAR file and returns it as a string.
     *  
     *  @param resourceLocation an URI of the resource
     *  @return The content of the file
     *  
     *  @throws IOException if the JAR file or the resource cannot be read
     */
    protected String getTextResource(String resourceLocation) throws IOException {
        if (m_resource == null) {
            return "";
        }
        String spec = m_resource.toString();
        int length = ModuleManager.PLUGIN_RESOURCE_LOCATION.length();
        spec = spec.substring(0, spec.length() - length) + resourceLocation;
        URL url = new URL(spec);
        BufferedInputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
        FileUtil.copyContents(in, out);
        in.close();
        String text = out.toString();
        out.close();
        return text;
    }

    /**
     *  {@inheritDoc}
     */
    public int compareTo(WikiModuleInfo arg0) {
        return m_name.compareTo(arg0.getName());
    }
}
