package netx.jnlp;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import java.awt.AWTPermission;

/**
 * The security element.
 *
 * @author <a href="mailto:jmaxwell@users.sourceforge.net">Jon A. Maxwell (JAM)</a> - initial author
 * @version $Revision: 1.7 $
 */
public class SecurityDesc {

    /** All permissions. */
    public static final Object ALL_PERMISSIONS = "All";

    /** Applet permissions. */
    public static final Object SANDBOX_PERMISSIONS = "Sandbox";

    /** J2EE permissions. */
    public static final Object J2EE_PERMISSIONS = "J2SE";

    /** permissions type */
    private Object type;

    /** the download host */
    private String downloadHost;

    /** the JNLP file */
    private JNLPFile file;

    /** basic permissions for restricted mode */
    private static Permission j2eePermissions[] = { new AWTPermission("accessClipboard"), new AWTPermission("showWindowWithoutWarningBanner"), new RuntimePermission("exitVM"), new RuntimePermission("loadLibrary"), new RuntimePermission("queuePrintJob"), new SocketPermission("*", "connect"), new SocketPermission("localhost:1024-", "accept, listen"), new FilePermission("*", "read, write"), new PropertyPermission("*", "read") };

    /** basic permissions for restricted mode */
    private static Permission sandboxPermissions[] = { new SocketPermission("localhost:1024-", "listen"), new PropertyPermission("java.version", "read"), new PropertyPermission("java.vendor", "read"), new PropertyPermission("java.vendor.url", "read"), new PropertyPermission("java.class.version", "read"), new PropertyPermission("os.name", "read"), new PropertyPermission("os.version", "read"), new PropertyPermission("os.arch", "read"), new PropertyPermission("file.separator", "read"), new PropertyPermission("path.separator", "read"), new PropertyPermission("line.separator", "read"), new PropertyPermission("java.specification.version", "read"), new PropertyPermission("java.specification.vendor", "read"), new PropertyPermission("java.specification.name", "read"), new PropertyPermission("java.vm.specification.vendor", "read"), new PropertyPermission("java.vm.specification.name", "read"), new PropertyPermission("java.vm.version", "read"), new PropertyPermission("java.vm.vendor", "read"), new PropertyPermission("java.vm.name", "read"), new RuntimePermission("exitVM"), new RuntimePermission("stopThread"), new AWTPermission("showWindowWithoutWarningBanner") };

    /**
     * Create a security descriptor.
     *
     * @param file the JNLP file
     * @param type the type of security
     * @param downloadHost the download host (can always connect to)
     */
    public SecurityDesc(JNLPFile file, Object type, String downloadHost) {
        this.file = file;
        this.type = type;
        this.downloadHost = downloadHost;
    }

    /**
     * Returns the permissions type, one of: ALL_PERMISSIONS,
     * SANDBOX_PERMISSIONS, J2EE_PERMISSIONS.
     */
    public Object getSecurityType() {
        return type;
    }

    /**
     * Returns a PermissionCollection containing the basic
     * permissions granted depending on the security type.
     */
    public PermissionCollection getPermissions() {
        Permissions permissions = new Permissions();
        if (type == ALL_PERMISSIONS) {
            permissions.add(new AllPermission());
            return permissions;
        }
        if (type == SANDBOX_PERMISSIONS) {
            for (int i = 0; i < sandboxPermissions.length; i++) permissions.add(sandboxPermissions[i]);
            if (downloadHost != null) permissions.add(new SocketPermission(downloadHost, "connect, accept"));
        }
        if (type == J2EE_PERMISSIONS) for (int i = 0; i < j2eePermissions.length; i++) permissions.add(j2eePermissions[i]);
        PropertyDesc props[] = file.getResources().getProperties();
        for (int i = 0; i < props.length; i++) {
            permissions.add(new PropertyPermission(props[i].getKey(), "read,write"));
        }
        return permissions;
    }
}
