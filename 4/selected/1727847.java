package org.extwind.osgi.console.dmserver.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import org.extwind.osgi.console.dmserver.support.DmContext;
import org.extwind.osgi.console.dmserver.support.DmServer;
import org.extwind.osgi.console.service.FrameworkDescription;
import org.extwind.osgi.console.service.LaunchService;
import org.extwind.osgi.console.service.Repository;
import org.extwind.osgi.console.service.RepositoryFactory;
import org.extwind.osgi.launch.remote.BundleDefinition;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Donf Yang
 * 
 */
public class DmServerLaunchService implements LaunchService {

    protected DmServer server;

    protected DmContext module;

    protected MBeanServerConnection serverConnection;

    protected ObjectName oname;

    protected FrameworkDescription frameworkDescription;

    protected RepositoryFactory repositoryFactory;

    private final Logger logger = LoggerFactory.getLogger(DmServerLaunchService.class);

    public DmServerLaunchService(DmServer server, DmContext module) throws Exception {
        this.server = server;
        this.serverConnection = server.getConnection();
        this.oname = new ObjectName(module.getDomain() + ":type=OSGiConsole,host=" + module.getHostName() + ",Context=" + module.getContextName());
        this.module = module;
        this.frameworkDescription = new DmServerFrameworkDescription(serverConnection, oname);
        this.repositoryFactory = new DmServerRepositoryFactory(serverConnection, oname);
    }

    public DmServer getDmServer() {
        return server;
    }

    public DmContext getDmContext() {
        return module;
    }

    public void createContext(String name) throws Exception {
        Object[] params = { name };
        String[] signature = { String.class.getName() };
        serverConnection.invoke(oname, "createContext", params, signature);
    }

    public Bundle getBundle(long id) throws Exception {
        Long[] params = { id };
        String[] signature = { long.class.getName() };
        Object obj = serverConnection.invoke(oname, "getBundle", params, signature);
        if (obj != null && obj instanceof BundleDefinition) {
            return new RemoteBundle(serverConnection, oname, (BundleDefinition) obj);
        }
        return null;
    }

    @Override
    public Bundle[] getBundles() throws Exception {
        BundleDefinition[] defs = getRemoteBundleDefinitions();
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (BundleDefinition def : defs) {
            Bundle bundle = new RemoteBundle(serverConnection, oname, def);
            bundles.add(bundle);
        }
        return bundles.toArray(new Bundle[0]);
    }

    @Override
    public int getBundleStartLevel(Bundle bundle) throws Exception {
        return 0;
    }

    @Override
    public FrameworkDescription getFrameworkDescription() throws Exception {
        return frameworkDescription;
    }

    @Override
    public String getFrameworkLocation() throws Exception {
        Object obj = serverConnection.getAttribute(oname, "frameworkLocation");
        return obj == null ? null : obj.toString();
    }

    @Override
    public RepositoryFactory getRepositoryFactory() throws Exception {
        return repositoryFactory;
    }

    @Override
    public String getStateName(long id) throws Exception {
        Long[] params = { id };
        String[] signature = { long.class.getName() };
        Object obj = serverConnection.invoke(oname, "getStateName", params, signature);
        if (obj != null) {
            return obj.toString();
        }
        return "Unknown";
    }

    @Override
    public Bundle installBundle(String location) throws Exception {
        String[] params = { location };
        String[] signature = { String.class.getName() };
        Object obj = serverConnection.invoke(oname, "installBundle", params, signature);
        if (obj != null && obj instanceof BundleDefinition) {
            return new RemoteBundle(serverConnection, oname, (BundleDefinition) obj);
        }
        return null;
    }

    @Override
    public Bundle installBundle(String location, InputStream input) throws Exception {
        File bundleFile = generateTempBundleFile(input);
        Object[] params = { location, bundleFile };
        String[] signature = { String.class.getName(), File.class.getName() };
        Object obj = serverConnection.invoke(oname, "installBundle", params, signature);
        if (obj != null && obj instanceof BundleDefinition) {
            return new RemoteBundle(serverConnection, oname, (BundleDefinition) obj);
        }
        return null;
    }

    @Override
    public Bundle installBundle(Repository repository, String location) throws Exception {
        String[] params = { repository.getLocation(), location };
        String[] signature = { String.class.getName(), String.class.getName() };
        Object obj = serverConnection.invoke(oname, "installBundle", params, signature);
        if (obj != null && obj instanceof BundleDefinition) {
            return new RemoteBundle(serverConnection, oname, (BundleDefinition) obj);
        }
        return null;
    }

    @Override
    public boolean isReadOnly() throws Exception {
        return false;
    }

    @Override
    public void refreshPackages(Bundle[] bundles) throws Exception {
        if (bundles == null || bundles.length == 0) {
            return;
        }
        List<String> ids = new ArrayList<String>(bundles.length);
        for (Bundle bundle : bundles) {
            ids.add(Long.toString(bundle.getBundleId()));
        }
        Object[] params = { ids.toArray(new String[0]) };
        String[] signature = { String[].class.getName() };
        serverConnection.invoke(oname, "refreshPackages", params, signature);
    }

    protected BundleDefinition[] getRemoteBundleDefinitions() throws Exception {
        Boolean[] params = { false };
        String[] signature = { boolean.class.getName() };
        Object object = serverConnection.invoke(oname, "getBundles", params, signature);
        if (object instanceof BundleDefinition[]) {
            return (BundleDefinition[]) object;
        }
        return new BundleDefinition[0];
    }

    protected File generateTempBundleFile(InputStream stream) throws Exception {
        final String tmpDir = System.getProperty("java.io.tmpdir");
        File extwindFolder = new File(tmpDir, "/extwind/tempbundlefiles");
        if (!extwindFolder.exists()) {
            extwindFolder.mkdirs();
        }
        File tmpBundleFile = new File(extwindFolder, Long.toString(System.currentTimeMillis()));
        logger.debug("Create temp bundle file - " + tmpBundleFile.getAbsolutePath());
        tmpBundleFile.createNewFile();
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(stream);
            bos = new BufferedOutputStream(new FileOutputStream(tmpBundleFile));
            byte[] buffer = new byte[1024];
            int read = -1;
            while ((read = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            bos.flush();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
        return tmpBundleFile;
    }

    @Override
    public String getDescription() {
        return server.getServiceUrl() + " -> " + module.toURI();
    }
}
