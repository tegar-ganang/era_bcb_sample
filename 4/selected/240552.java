package org.appspy.server.dao.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Olivier HEDIN / olivier@appspy.org
 */
public class XmlClasspathResourceLoader implements XmlResourceLoader {

    public byte[] loadResource(String name) throws IOException {
        ClassPathResource cpr = new ClassPathResource(name);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(cpr.getInputStream(), baos);
        return baos.toByteArray();
    }
}
