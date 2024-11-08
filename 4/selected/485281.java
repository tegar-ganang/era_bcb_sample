package com.volantis.mcs.policies.impl.io;

import com.volantis.synergetics.io.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import junit.framework.Assert;

/**
 * @todo rename to TestResourceLoader to avoid confusion with runtime 
 * ResourceLoader interface
 */
public class ResourceLoader {

    private Class aClass;

    public ResourceLoader(Class aClass) {
        this.aClass = aClass;
    }

    public String getResourceAsString(String name) throws IOException {
        String content = null;
        InputStream stream = aClass.getResourceAsStream(name);
        if (stream != null) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            IOUtils.copyAndClose(stream, buffer);
            content = buffer.toString();
        } else {
            Assert.fail("Resource not available: " + name);
        }
        return content;
    }
}
