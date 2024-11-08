package symbiosis;

import symbiosis.message.ChannelManager;
import symbiosis.security.IdentityManager;
import symbiosis.module.ModuleManager;
import symbiosis.service.ServiceManager;
import symbiosis.client.ClientManager;
import symbiosis.ui.UIManager;
import symbiosis.util.Util;

/**
 * @author Borne J. Goodman-Mace
 */
public class Symbiosis {

    public static final String SYMBIOSIS_NAME = "Symbiosis";

    public static final String SYMBIOSIS_VERSION = "0.1.0";

    private static Symbiosis symbiosis = null;

    private static ChannelManager channelManager = null;

    private static IdentityManager identityManager = null;

    private static ModuleManager moduleManager = null;

    private static ServiceManager serviceManager = null;

    private static ClientManager clientManager = null;

    private static UIManager uiManager = null;

    public Symbiosis() {
        if (symbiosis != null) return;
        symbiosis = this;
        Util.info(SYMBIOSIS_NAME + " version " + SYMBIOSIS_VERSION);
    }

    private void init() {
        initChannelManager();
        initIdentityManager();
        initModuleManager();
        initServiceManager();
        initClientManager();
        initUIManager();
    }

    private void start() {
        Util.info(SYMBIOSIS_NAME + " Starting");
        startChannelManager();
        startIdentityManager();
        startModuleManager();
        startServiceManager();
        startClientManager();
        startUIManager();
        Util.info(SYMBIOSIS_NAME + " Started");
    }

    public void shutdown(int exitCode) {
        uiManager.shutdown();
        clientManager.shutdown();
        serviceManager.shutdown();
        moduleManager.shutdown();
        identityManager.shutdown();
        channelManager.shutdown();
        System.exit(exitCode);
    }

    private void initChannelManager() {
        channelManager = new ChannelManager();
        channelManager.init();
    }

    private void startChannelManager() {
        channelManager.start();
    }

    private void initIdentityManager() {
        identityManager = new IdentityManager();
        identityManager.init();
    }

    private void startIdentityManager() {
        identityManager.start();
    }

    private void initModuleManager() {
        moduleManager = new ModuleManager();
        moduleManager.init();
    }

    private void startModuleManager() {
        moduleManager.start();
    }

    private void initServiceManager() {
        serviceManager = new ServiceManager();
        serviceManager.init();
    }

    private void startServiceManager() {
        serviceManager.start();
    }

    private void initClientManager() {
        clientManager = new ClientManager();
        clientManager.init();
    }

    private void startClientManager() {
        clientManager.start();
    }

    private void initUIManager() {
        uiManager = new UIManager();
        uiManager.init();
    }

    private void startUIManager() {
        uiManager.start();
    }

    public static Symbiosis getInstance() {
        return symbiosis;
    }

    public static ChannelManager getChannelManager() {
        return channelManager;
    }

    public static IdentityManager getIdentityManager() {
        return identityManager;
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static ServiceManager getServiceManager() {
        return serviceManager;
    }

    public static ClientManager getClientManager() {
        return clientManager;
    }

    public static UIManager getUIManager() {
        return uiManager;
    }

    public static void main(String args[]) {
        Symbiosis symbiosis = new Symbiosis();
        symbiosis.init();
        symbiosis.start();
    }
}
