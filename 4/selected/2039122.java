package onepoint.resource;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import onepoint.log.XLog;
import onepoint.log.XLogFactory;

public class XResourceBroker implements XResourceBrokerIfc {

    private static final XLog logger = XLogFactory.getLogger(XResourceBroker.class);

    private static final int FILE_BUFFER_SIZE = 8192;

    private static String resourcePath = "";

    private Map onLoadFilters = new HashMap();

    private Map onGetFilters = new HashMap();

    public static void setResourcePath(String path) {
        resourcePath = path;
    }

    public static String getResourcePath() {
        return resourcePath;
    }

    public void registerResourceLoader(String extension, XResourceFilter filter) {
        onLoadFilters.put(extension, filter);
    }

    public void registerResourceGetter(String extension, XResourceFilter filter) {
        onGetFilters.put(extension, filter);
    }

    private byte[] applyFilter(String file_name, byte[] byte_buffer, Map filters) {
        logger.debug("XResourceManager._applyLoader() : filename = " + file_name);
        if (byte_buffer != null) {
            logger.debug("   resource-byte-size " + byte_buffer.length);
        }
        int first_dot = file_name.indexOf('.');
        if ((first_dot != -1) && (first_dot < file_name.length() - 1)) {
            String extension = file_name.substring(first_dot + 1, file_name.length());
            XResourceFilter filter = (XResourceFilter) (filters.get(extension));
            if (filter != null) {
                try {
                    ByteArrayInputStream byte_input = new ByteArrayInputStream(byte_buffer);
                    filter.setInputStream(byte_input);
                    ByteArrayOutputStream filtered_output = new ByteArrayOutputStream();
                    byte[] file_buffer = new byte[FILE_BUFFER_SIZE];
                    int bytes_read = filter.read(file_buffer);
                    while (bytes_read != -1) {
                        filtered_output.write(file_buffer, 0, bytes_read);
                        bytes_read = filter.read(file_buffer);
                    }
                    byte_buffer = filtered_output.toByteArray();
                } catch (java.io.IOException e) {
                    logger.error("Error on filtering resource " + e + ", within file " + file_name);
                }
            }
        }
        return byte_buffer;
    }

    private byte[] _loadResource(String path) {
        logger.debug("XResourceManager._loadFileResource() : path = " + path);
        byte[] byte_buffer = null;
        try {
            BufferedInputStream input = new BufferedInputStream(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
            ByteArrayOutputStream byte_output = new ByteArrayOutputStream();
            byte[] file_buffer = new byte[FILE_BUFFER_SIZE];
            int bytes_read = input.read(file_buffer);
            while (bytes_read != -1) {
                byte_output.write(file_buffer, 0, bytes_read);
                bytes_read = input.read(file_buffer);
            }
            byte_buffer = byte_output.toByteArray();
        } catch (Exception e) {
            logger.error("Error on loading form resource: " + path, e);
        }
        byte_buffer = applyFilter(path, byte_buffer, onLoadFilters);
        logger.debug("/XResourceManager._loadFileResource()");
        return byte_buffer;
    }

    public byte[] applyFilters(String path, byte[] buffer) {
        return applyFilter(path, buffer, onLoadFilters);
    }

    public byte[] getResource(String path) {
        logger.debug("XResourceManager.getResource() : file_name = " + path);
        byte[] byte_buffer = XResourceCache.getResource(path);
        if (byte_buffer == null) {
            String complete_path = resourcePath + path;
            byte_buffer = _loadResource(complete_path);
            if (byte_buffer == null) {
                logger.warn("Error loading file resource " + path);
            } else {
                XResourceCache.putResource(path, byte_buffer);
            }
        }
        logger.debug("/XResourceManager.getResource()");
        return byte_buffer;
    }
}
