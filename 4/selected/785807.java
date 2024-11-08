package za.co.data.plugins;

import za.co.data.framework.modler.ModlerUI;
import za.co.data.framework.plugin.PluginManager;
import za.co.data.util.IOUtils;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.net.URISyntaxException;

/**
 * Created by Darryl Culverwell
 * On Jul 17, 2008 6:27:38 PM
 */
public abstract class SiteLayoutPlugin {

    protected static File WEB_ROOT = new File(ModlerUI.properties.getProperty("silo.absoloute.path") + "/web/");

    protected static File LAYOUT_OUTPUT_FILE = new File(ModlerUI.properties.getProperty("silo.absoloute.path") + "/web/main.html");

    protected static File INDEX_OUTPUT_FILE = new File(ModlerUI.properties.getProperty("silo.absoloute.path") + "/web/index.html");

    protected static File TITLE_OUTPUT_FILE = new File(ModlerUI.properties.getProperty("silo.absoloute.path") + "/web/title.html");

    static {
        if (!WEB_ROOT.exists()) WEB_ROOT.mkdirs();
    }

    public abstract String getLayoutSource();

    public abstract String getTitleSource();

    public abstract String getIndexSource();

    /**
     * Returns the source and target for files to copy.
     * The name of the resource in the first locateion, relative output name in position 2
     * ie {"/plugins/web/images/test1.png", "/web/images/test1.png"}
     *
     * @return
     */
    public abstract String[][] getDependancyFiles();

    public void createLayout() throws IOException {
        createPage(TITLE_OUTPUT_FILE, getTitleSource());
        createPage(INDEX_OUTPUT_FILE, getIndexSource());
        createPage(LAYOUT_OUTPUT_FILE, getLayoutSource());
    }

    /**
     * Writes the contents of getLayoutSource to the LAYOUT_OUTPUT_FILE
     *
     * @throws IOException
     */
    public void createPage(File outputFile, String layoutSource) throws IOException {
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        FileWriter fw = new FileWriter(outputFile, false);
        fw.write(layoutSource);
        fw.close();
    }

    /**
     * 
     */
    public void copyDependancyFiles() {
        for (String[] depStrings : getDependancyFiles()) {
            String source = depStrings[0];
            String target = depStrings[1];
            try {
                File sourceFile = PluginManager.getFile(source);
                IOUtils.copyEverything(sourceFile, new File(WEB_ROOT + target));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
