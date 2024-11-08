package com.swsoft.trial.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;

public class ResourceUtil {

    /**
	 * Prints resource with given name to <code>OutputStream</code>
	 * @param os <code>OutputStream</code> to print resource in
	 * @param resourceName Resource name
	 * @throws IOException If I/O error occured
	 */
    public static void printResource(OutputStream os, String resourceName) throws IOException {
        InputStream is = null;
        try {
            is = ResourceLoader.loadResource(resourceName);
            if (is == null) {
                throw new IOException("Given resource not found!");
            }
            IOUtils.copy(is, os);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private ResourceUtil() {
    }
}
