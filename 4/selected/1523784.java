package honeycrm.server.test.small.dyn.hotreload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;

public abstract class ClassLoaderDelegate {

    protected byte[] findClass(String binaryName) {
        InputStream stream = findResourceAsStream(InterceptClassLoader.toClassFilePath(binaryName));
        if (stream == null) {
            return null;
        }
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1000];
            while (true) {
                int read = stream.read(buffer);
                if (read < 0) {
                    break;
                }
                result.write(buffer, 0, read);
            }
            return result.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    protected URL findResource(String path) {
        Iterator<URL> found = findAllResources(path).iterator();
        if (found.hasNext()) {
            return found.next();
        }
        return null;
    }

    protected Iterable<URL> findAllResources(String path) {
        return Collections.emptyList();
    }

    protected InputStream findResourceAsStream(String path) {
        URL location = findResource(path);
        if (location == null) {
            return null;
        }
        try {
            return location.openStream();
        } catch (IOException e) {
            return null;
        }
    }
}
