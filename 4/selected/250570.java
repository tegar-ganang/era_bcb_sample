package it.glz.hostingjava;

import java.io.File;
import java.io.FilePermission;
import java.security.*;

/**
 *
 * @author Luigi
 */
public class WebappLoader extends org.apache.catalina.loader.WebappClassLoader {

    SecurityManager securityManager = null;

    /** Creates a new instance of WebappClassLoader */
    public WebappLoader() {
        super();
        securityManager = System.getSecurityManager();
    }

    public void addPermission(String path) {
        System.out.println("Permission " + path);
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
