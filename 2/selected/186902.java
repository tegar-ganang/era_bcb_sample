package org.dozer.loader.xml;

import org.dozer.config.BeanContainer;
import org.dozer.util.DozerClassLoader;
import org.dozer.util.DozerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import java.io.InputStream;
import java.net.URL;

/**
 * Internal EntityResolver implementation to load Xml Schema from the dozer classpath resp. JAR file.
 * 
 * <p>
 * Fetches "beanmapping.xsd" from the classpath resource "/beanmapping.xsd", no matter if specified as some
 * local URL or as "http://dozer.sourceforge.net/schema/beanmapping.xsd".
 * 
 * @author garsombke.franz
 */
public class DozerResolver implements EntityResolver {

    private static final Logger log = LoggerFactory.getLogger(DozerResolver.class);

    public InputSource resolveEntity(String publicId, String systemId) {
        log.debug("Trying to resolve XML entity with public ID [{}] and system ID [{}]", publicId, systemId);
        if (systemId != null && systemId.indexOf(DozerConstants.XSD_NAME) > systemId.lastIndexOf("/")) {
            String fileName = systemId.substring(systemId.indexOf(DozerConstants.XSD_NAME));
            log.debug("Trying to locate [{}] in classpath", fileName);
            try {
                DozerClassLoader classLoader = BeanContainer.getInstance().getClassLoader();
                URL url = classLoader.loadResource(fileName);
                InputStream stream = url.openStream();
                InputSource source = new InputSource(stream);
                source.setPublicId(publicId);
                source.setSystemId(systemId);
                log.debug("Found beanmapping XML Schema [{}] in classpath", systemId);
                return source;
            } catch (Exception ex) {
                log.error("Could not resolve beanmapping XML Schema [" + systemId + "]: not found in classpath", ex);
            }
        }
        return null;
    }
}
