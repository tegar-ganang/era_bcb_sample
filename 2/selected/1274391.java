package org.apache.myfaces.config.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.logging.Logger;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.shared_impl.util.ClassUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * DOCUMENT ME!
 * @author Manfred Geiler (latest modification by $Author: lu4242 $)
 * @author Thomas Spiegl
 * @version $Revision: 824859 $ $Date: 2009-10-13 12:42:36 -0500 (Tue, 13 Oct 2009) $
 */
public class FacesConfigEntityResolver implements EntityResolver {

    private static final Logger log = Logger.getLogger(FacesConfigEntityResolver.class.getName());

    private static final String FACES_CONFIG_1_0_DTD_SYSTEM_ID = "http://java.sun.com/dtd/web-facesconfig_1_0.dtd";

    private static final String FACES_CONFIG_1_0_DTD_RESOURCE = "org.apache.myfaces.resource".replace('.', '/') + "/web-facesconfig_1_0.dtd";

    private static final String FACES_CONFIG_1_1_DTD_SYSTEM_ID = "http://java.sun.com/dtd/web-facesconfig_1_1.dtd";

    private static final String FACES_CONFIG_1_1_DTD_RESOURCE = "org.apache.myfaces.resource".replace('.', '/') + "/web-facesconfig_1_1.dtd";

    private ExternalContext _externalContext = null;

    public FacesConfigEntityResolver(ExternalContext context) {
        _externalContext = context;
    }

    public FacesConfigEntityResolver() {
    }

    public InputSource resolveEntity(String publicId, String systemId) throws IOException {
        InputStream stream;
        if (systemId.equals(FACES_CONFIG_1_0_DTD_SYSTEM_ID)) {
            stream = ClassUtils.getResourceAsStream(FACES_CONFIG_1_0_DTD_RESOURCE);
        } else if (systemId.equals(FACES_CONFIG_1_1_DTD_SYSTEM_ID)) {
            stream = ClassUtils.getResourceAsStream(FACES_CONFIG_1_1_DTD_RESOURCE);
        } else if (systemId.startsWith("jar:")) {
            URL url = new URL(systemId);
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            conn.setUseCaches(false);
            JarEntry jarEntry = conn.getJarEntry();
            if (jarEntry == null) {
                log.severe("JAR entry '" + systemId + "' not found.");
            }
            stream = conn.getJarFile().getInputStream(jarEntry);
        } else {
            if (_externalContext == null) {
                stream = ClassUtils.getResourceAsStream(systemId);
            } else {
                if (systemId.startsWith("file:")) {
                    systemId = systemId.substring(7);
                }
                stream = _externalContext.getResourceAsStream(systemId);
            }
        }
        if (stream == null) {
            return null;
        }
        InputSource is = new InputSource(stream);
        is.setPublicId(publicId);
        is.setSystemId(systemId);
        is.setEncoding("ISO-8859-1");
        return is;
    }
}
