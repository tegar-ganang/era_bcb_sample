package com.vangent.hieos.DocViewer.server.framework;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import javax.servlet.ServletContext;
import org.apache.commons.io.IOUtils;
import com.vangent.hieos.xutil.exception.XConfigException;
import com.vangent.hieos.xutil.xconfig.XConfig;
import com.vangent.hieos.xutil.xconfig.XConfigActor;
import com.vangent.hieos.xutil.xconfig.XConfigObject;
import org.apache.commons.lang.StringUtils;

/**
 * 
 * @author Bernie Thuman
 * 
 */
public class ServletUtilMixin {

    static final String XCONFIG_FILE = "/resources/xconfig.xml";

    private ServletContext servletContext;

    public void init(ServletContext servletContext) {
        this.servletContext = servletContext;
        String xConfigRealPath = servletContext.getRealPath(ServletUtilMixin.XCONFIG_FILE);
        System.out.println("Real Path: " + xConfigRealPath);
        XConfig.setConfigLocation(xConfigRealPath);
    }

    /**
	 * 
	 * @return
	 */
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    /**
	 * 
	 * @param servletContext
	 * @param key
	 * @return
	 */
    public String getProperty(String key) {
        String value = null;
        XConfigObject configObject = this.getConfig();
        if (configObject != null) {
            value = configObject.getProperty(key);
        }
        return value;
    }

    public String getProperty(String key, String defaultString) {
        String value = null;
        XConfigObject configObject = this.getConfig();
        if (configObject != null) {
            value = configObject.getProperty(key);
            if (StringUtils.isBlank(value)) {
                value = defaultString;
            }
        }
        return value;
    }

    /**
	 * 
	 * @param templateFilename
	 * @return
	 */
    public String getTemplateString(String templateFilename) {
        InputStream is = servletContext.getResourceAsStream("/resources/" + templateFilename);
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(is, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return writer.toString();
    }

    /**
	 * 
	 * @param name
	 * @param type
	 * @return
	 */
    public XConfigActor getActorConfig(String name, String type) {
        XConfigActor actorConfig = null;
        XConfigObject configObject = this.getConfig();
        actorConfig = (XConfigActor) configObject.getXConfigObjectWithName(name, type);
        return actorConfig;
    }

    /**
	 * 
	 * @return
	 */
    public XConfigObject getConfig() {
        XConfigObject config = null;
        try {
            XConfig xconf = XConfig.getInstance();
            config = xconf.getXConfigObjectByName("DocViewerProperties", "DocViewerPropertiesType");
        } catch (XConfigException e) {
            e.printStackTrace();
        }
        return config;
    }
}
