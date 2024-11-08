package marubinotto.piggydb.server;

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SplashScreen;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

public class PiggydbServer {

    private static Log logger = LogFactory.getLog(PiggydbServer.class);

    static final String SERVER_NAME = "Piggydb";

    static final String URL_PREFIX = "http://localhost";

    static ResourceBundle messages;

    static PiggydbSplashScreen splashScreen = PiggydbSplashScreen.NULL;

    static Settings settings;

    static Server server;

    static TrayIcon trayIcon;

    public static void main(String[] args) throws Throwable {
        try {
            launch();
        } catch (Throwable e) {
            JOptionPane.showMessageDialog(null, e.toString(), SERVER_NAME + " Error", JOptionPane.ERROR_MESSAGE);
            logger.error(e.toString(), e);
            throw e;
        }
    }

    static void launch() throws Exception {
        messages = ResourceBundle.getBundle("messages");
        initSplashScreen();
        splashScreen.message("Loading " + Settings.FILE_NAME + " ...");
        loadSettings();
        splashScreen.message("Initializing the server ...");
        initServer();
        splashScreen.message("Starting the server ...");
        server.start();
        setupTrayIcon();
        if (settings.isLaunchBrowserWhenStartup()) {
            splashScreen.message("Opening the home page ...");
            launchBrowser();
        }
        splashScreen.close();
        splashScreen = null;
        server.join();
    }

    static void initSplashScreen() throws Exception {
        String version = null;
        InputStream input = PiggydbServer.class.getResourceAsStream("VERSION");
        try {
            version = IOUtils.toString(input);
        } finally {
            input.close();
        }
        SplashScreen s = SplashScreen.getSplashScreen();
        if (s != null) splashScreen = new PiggydbSplashScreen(s, version);
    }

    static void loadSettings() throws Exception {
        File baseDir = new File(System.getProperty("user.dir"));
        logger.info("baseDir: " + baseDir.getAbsolutePath());
        settings = new Settings(baseDir);
    }

    static final String WEBAPP_DIR = "webapp";

    static final String APP_SETTINGS_FILE_NAME = "application.properties";

    static final String APP_SETTINGS_FILE_PATH = WEBAPP_DIR + "/WEB-INF/config/" + APP_SETTINGS_FILE_NAME;

    static void initServer() throws IOException {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(settings.getPort());
        server.addConnector(connector);
        File resourceBase = settings.getResourcePath(WEBAPP_DIR);
        File descriptor = settings.getResourcePath(WEBAPP_DIR + "/WEB-INF/web.xml");
        if (!resourceBase.isDirectory()) throw new FileNotFoundException(resourceBase.getAbsolutePath());
        if (!descriptor.isFile()) throw new FileNotFoundException(descriptor.getAbsolutePath());
        File appSettingsSrc = settings.getResourcePath(APP_SETTINGS_FILE_NAME);
        if (appSettingsSrc.isFile()) {
            File appSettingsDest = settings.getResourcePath(APP_SETTINGS_FILE_PATH);
            FileUtils.copyFile(appSettingsSrc, appSettingsDest);
        }
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setResourceBase(resourceBase.getAbsolutePath());
        webapp.setDescriptor(descriptor.getAbsolutePath());
        webapp.setParentLoaderPriority(true);
        server.setHandler(webapp);
    }

    static void setupTrayIcon() throws IOException, AWTException {
        trayIcon = new TrayIcon(ImageIO.read(PiggydbServer.class.getResourceAsStream("tray-icon.png")), SERVER_NAME);
        PopupMenu menu = new PopupMenu();
        menu.add(createMenuItem(messages.getString("home"), new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                try {
                    launchBrowser();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }));
        menu.add(createMenuItem(messages.getString("info"), new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                trayIcon.displayMessage(SERVER_NAME, "Piggydb server is running on port " + settings.getPort(), TrayIcon.MessageType.INFO);
            }
        }));
        menu.add(createMenuItem(messages.getString("shutdown"), new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                try {
                    server.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                System.exit(0);
            }
        }));
        trayIcon.setPopupMenu(menu);
        SystemTray.getSystemTray().add(trayIcon);
    }

    static MenuItem createMenuItem(String label, ActionListener listener) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(listener);
        return item;
    }

    static void launchBrowser() throws IOException, URISyntaxException {
        String url = URL_PREFIX;
        if (settings.getPort() != 80) url = url + ":" + settings.getPort();
        Desktop.getDesktop().browse(new URI(url));
    }
}
