package com.iver.cit.gvsig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.KeyException;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.iver.andami.Launcher;
import com.iver.andami.PluginServices;
import com.iver.andami.plugins.Extension;
import com.iver.cit.gvsig.fmap.layers.FLayerFileVectorial;
import com.iver.cit.gvsig.fmap.layers.FLayerGenericVectorial;
import com.iver.cit.gvsig.fmap.layers.LayerFactory;
import com.iver.utiles.extensionPoints.ExtensionPoint;
import com.iver.utiles.extensionPoints.ExtensionPoints;
import com.iver.utiles.extensionPoints.ExtensionPointsSingleton;

public class IntializeApplicationExtension extends Extension {

    private ExtensionPoints extensionPoints = ExtensionPointsSingleton.getInstance();

    private static Logger logger = Logger.getLogger("gvSIG");

    public void initialize() {
        addToLogInfo();
        this.extensionPoints.add("Layers", FLayerFileVectorial.class.getName(), FLayerFileVectorial.class);
        this.extensionPoints.add("Layers", FLayerGenericVectorial.class.getName(), FLayerGenericVectorial.class);
        try {
            ((ExtensionPoint) this.extensionPoints.get("Layers")).addAlias(FLayerFileVectorial.class.getName(), "FileVectorial");
            ((ExtensionPoint) this.extensionPoints.get("Layers")).addAlias(FLayerGenericVectorial.class.getName(), "GenericVectorial");
        } catch (KeyException e) {
            e.printStackTrace();
        }
        registerIcons();
    }

    private void registerIcons() {
        PluginServices.getIconTheme().registerDefault("view-add-event-layer", this.getClass().getClassLoader().getResource("images/addeventtheme.png"));
        PluginServices.getIconTheme().registerDefault("gvsig-logo-icon", this.getClass().getClassLoader().getResource("images/icon_gvsig.png"));
        PluginServices.getIconTheme().registerDefault("mapa-icono", this.getClass().getClassLoader().getResource("images/mapas.png"));
        PluginServices.getIconTheme().registerDefault("layout-insert-view", this.getClass().getClassLoader().getResource("images/MapaVista.png"));
        PluginServices.getIconTheme().registerDefault("vista-icono", this.getClass().getClassLoader().getResource("images/Vista.png"));
        PluginServices.getIconTheme().registerDefault("hand-icono", this.getClass().getClassLoader().getResource("images/Hand.png"));
        PluginServices.getIconTheme().registerDefault("add-layer-icono", this.getClass().getClassLoader().getResource("images/add-layer.png"));
        PluginServices.getIconTheme().registerDefault("delete-icono", this.getClass().getClassLoader().getResource("images/delete.png"));
        PluginServices.getIconTheme().registerDefault("arrow-up-icono", this.getClass().getClassLoader().getResource("images/up-arrow.png"));
        PluginServices.getIconTheme().registerDefault("arrow-down-icono", this.getClass().getClassLoader().getResource("images/down-arrow.png"));
    }

    public void execute(String actionCommand) {
    }

    public boolean isEnabled() {
        return false;
    }

    public boolean isVisible() {
        return false;
    }

    private void addToLogInfo() {
        String info[] = this.getStringInfo().split("\n");
        for (int i = 0; i < info.length; i++) {
            logger.info(info[i]);
        }
    }

    public String getStringInfo() {
        StringWriter writer = new StringWriter();
        String andamiPath;
        String extensionsPath;
        String jaiVersion;
        Properties props = System.getProperties();
        try {
            try {
                andamiPath = (new File(Launcher.class.getResource(".").getFile() + File.separator + ".." + File.separator + ".." + File.separator + "..")).getCanonicalPath();
            } catch (IOException e) {
                andamiPath = (new File(Launcher.class.getResource(".").getFile() + File.separator + ".." + File.separator + ".." + File.separator + "..")).getAbsolutePath();
            }
        } catch (Exception e1) {
            andamiPath = (String) props.get("user.dir");
        }
        try {
            try {
                extensionsPath = (new File(Launcher.getAndamiConfig().getPluginsDirectory())).getCanonicalPath();
            } catch (IOException e) {
                extensionsPath = (new File(Launcher.getAndamiConfig().getPluginsDirectory())).getAbsolutePath();
            }
        } catch (Exception e1) {
            extensionsPath = "???";
        }
        writer.write("gvSIG version: " + Version.longFormat() + "\n");
        writer.write("    gvSIG app exec path: " + andamiPath + "\n");
        writer.write("    gvSIG user app home: " + Launcher.getAppHomeDir() + "\n");
        writer.write("    gvSIG extension path: " + extensionsPath + "\n");
        writer.write("    gvSIG locale language: " + Launcher.getAndamiConfig().getLocaleLanguage() + "\n");
        String osName = props.getProperty("os.name");
        writer.write("OS name: " + osName + "\n");
        writer.write("    arch:" + props.get("os.arch") + "\n");
        writer.write("    version:" + props.get("os.version") + "\n");
        if (osName.startsWith("Linux")) {
            try {
                String[] command = { "lsb_release", "-a" };
                Process p = Runtime.getRuntime().exec(command);
                InputStream is = p.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) writer.write("    " + line + "\n");
            } catch (Exception ex) {
            }
        }
        writer.write("JAVA vendor: " + props.get("java.vendor") + "\n");
        writer.write("    version:" + props.get("java.version") + "\n");
        writer.write("    home: " + props.get("java.home") + "\n");
        return writer.toString();
    }

    public void terminate() {
        super.terminate();
        try {
            LayerFactory.getDataSourceFactory().finalizeThis();
        } catch (Exception e) {
        }
    }
}
