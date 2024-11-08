package it.ilz.hostingjava.loader;

import java.io.File;
import java.io.FilePermission;
import java.security.*;
import org.apache.catalina.LifecycleException;

/**
 *
 * @author Luigi
 */
public class WebappClassLoader extends org.apache.catalina.loader.WebappClassLoader {

    SecurityManager securityManager = null;

    /** Creates a new instance of WebappClassLoader */
    public WebappClassLoader() {
        super();
        securityManager = System.getSecurityManager();
    }

    public WebappClassLoader(ClassLoader classloader) {
        super(classloader);
    }

    public void addPermission(String path) {
        if (true) {
            super.addPermission(path);
            return;
        }
        if (path == null) return;
        if (securityManager != null) {
            Permission permission = null;
            if (path.startsWith("jndi:") || path.startsWith("jar:jndi:")) {
                super.addPermission(path);
            } else {
                if (!path.endsWith(File.separator)) {
                    permission = new FilePermission(path, "read,write");
                    addPermission(permission);
                    path = path + File.separator;
                }
                permission = new FilePermission(path + "-", "read,write");
                addPermission(permission);
            }
        }
    }
}
