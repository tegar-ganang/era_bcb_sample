package org.qtitools.playr;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 * Provides control over the jetty core and
 * deployment of our services
 *
 */
public class PlayrServerController {

    Server server;

    File baseDirectory;

    File settingsDirectory;

    File webappsDirectory;

    int PORT;

    public PlayrServerController() {
        this(38080);
    }

    public PlayrServerController(int port) {
        setPort(port);
    }

    public void setPort(int port) {
        this.PORT = port;
    }

    public boolean start() {
        try {
            InitServer(PORT);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void stop() {
        try {
            server.stop();
            server.destroy();
            server = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void InitServer(int port) throws Exception {
        baseDirectory = createTempDir("playr", null);
        unpackSettings();
        changeSettingsPort(port);
        changeSettingsPaths();
        webappsDirectory = new File(baseDirectory.toString() + File.separator + "webapps");
        webappsDirectory.mkdirs();
        copyWebapps();
        server = new Server();
        Connector connector = new SelectChannelConnector();
        connector.setPort(port);
        connector.setHost("127.0.0.1");
        server.addConnector(connector);
        System.setProperty("R2Q2_HOME", settingsDirectory.toString() + File.separator);
        WebAppContext wac = new WebAppContext();
        fixContext(wac);
        wac.setContextPath("/r2q2");
        wac.setWar(new File(webappsDirectory.toString() + File.separator + "rq2q.war").toString());
        server.addHandler(wac);
        ContextHandler ch = new ContextHandler();
        ch.setContextPath("/content");
        ch.setResourceBase(settingsDirectory.toString() + File.separator);
        ResourceHandler rh = new ResourceHandler();
        ch.addHandler(rh);
        server.addHandler(ch);
        System.setProperty("asdel.properties.root", settingsDirectory.toString() + File.separator);
        System.out.println(System.getProperty("asdel.properties.root"));
        wac = new WebAppContext();
        fixContext(wac);
        wac.setContextPath("/");
        wac.setWar(new File(webappsDirectory.toString() + File.separator + "playr.war").toString());
        wac.addEventListener(new AppInit());
        server.addHandler(wac);
        server.setStopAtShutdown(true);
    }

    private void fixContext(WebAppContext wac) {
        try {
            Class.forName("javax.activation.DataSource", false, null);
            List<String> classes = new ArrayList<String>();
            classes.addAll(Arrays.asList(wac.getSystemClasses()));
            classes.add("javax.activation.");
            wac.setSystemClasses(classes.toArray(new String[classes.size()]));
        } catch (ClassNotFoundException e) {
        }
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) delete(files[i]);
        }
        file.delete();
    }

    @Override
    protected void finalize() throws Throwable {
    }

    public void resume() throws Exception {
        if (server.isStopped()) server.start();
    }

    public void pause() throws Exception {
        if (server.isRunning()) server.stop();
    }

    private void changeSettingsPort(int port) throws Exception {
        propsReplace(new File(settingsDirectory.toString() + File.separator + "r2q2.properties"), "8080", Integer.toString(port));
        propsReplace(new File(settingsDirectory.toString() + File.separator + "TestControllerEngine.properties"), "8080", Integer.toString(port));
    }

    private void changeSettingsPaths() throws Exception {
        propsReplace(new File(settingsDirectory.toString() + File.separator + "r2q2.properties"), "/path_to_settings_dir", settingsDirectory.toString());
    }

    private void propsReplace(File propsFile, String search, String replace) throws Exception {
        FileInputStream fis = new FileInputStream(propsFile);
        Properties p = new Properties();
        p.load(fis);
        fis.close();
        for (Object key : p.keySet()) {
            String s = p.getProperty((String) key);
            s = s.replace(search, replace);
            p.setProperty((String) key, s);
        }
        FileOutputStream fos = new FileOutputStream(propsFile);
        p.store(fos, "");
        fos.flush();
        fos.close();
    }

    private void copyWebapps() throws Exception {
        writeStream(getClass().getResourceAsStream("/r2q2.war"), new File(webappsDirectory.toString() + File.separator + "rq2q.war"));
        writeStream(getClass().getResourceAsStream("/playr.war"), new File(webappsDirectory.toString() + File.separator + "playr.war"));
    }

    private void writeStream(InputStream in, File out) throws Exception {
        FileOutputStream fos = new FileOutputStream(out);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            fos.write(buf, 0, len);
        }
        in.close();
        fos.close();
    }

    private void unpackSettings() throws Exception {
        ZipInputStream zipInputStream = new ZipInputStream(getClass().getResourceAsStream("/settings.zip"));
        int BUFFER_SIZE = 4096;
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            File destFile = new File(baseDirectory, zipEntry.getName());
            destFile.getParentFile().mkdirs();
            if (!zipEntry.isDirectory()) {
                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(destFile), BUFFER_SIZE);
                byte[] buffer = new byte[BUFFER_SIZE];
                int length;
                while ((length = zipInputStream.read(buffer, 0, BUFFER_SIZE)) != -1) output.write(buffer, 0, length);
                output.close();
                zipInputStream.closeEntry();
            }
        }
        zipInputStream.close();
        settingsDirectory = new File(baseDirectory.toString() + File.separator + "settings");
    }

    private File createTempDir(String prefix, File dir) throws IOException {
        File tempFile = File.createTempFile(prefix, "", dir);
        if (!tempFile.delete()) throw new IOException();
        if (!tempFile.mkdir()) throw new IOException();
        return tempFile;
    }

    private class AppInit implements ServletContextListener {

        public void contextDestroyed(ServletContextEvent arg0) {
            delete(baseDirectory);
        }

        public void contextInitialized(ServletContextEvent arg0) {
            BareBonesBrowserLaunch.openURL("http://127.0.0.1:" + PORT + "/");
        }
    }
}
