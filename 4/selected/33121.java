package com.volantis.devrep.device.impl.integration;

import com.volantis.synergetics.io.IOUtils;
import org.jibx.runtime.JiBXException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceJiBXTester extends JiBXTester {

    private Class resourceClass;

    public ResourceJiBXTester(Class resourceClass) {
        this.resourceClass = resourceClass;
    }

    public Object unmarshall(Class aClass, String resourceName) throws JiBXException {
        InputStream resourceStream = resourceClass.getResourceAsStream(resourceName);
        InputStreamReader reader = new InputStreamReader(resourceStream);
        return unmarshall(aClass, reader);
    }

    public String getResource(String resourceName) throws IOException {
        InputStream resourceStream = resourceClass.getResourceAsStream(resourceName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        IOUtils.copyAndClose(resourceStream, baos);
        String expected = new String(baos.toByteArray());
        return expected;
    }
}
