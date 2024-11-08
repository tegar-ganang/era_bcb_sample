package se.sics.trust.azureus.plugins.trust;

import java.io.InputStream;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.plugins.config.ConfigParameterListener;
import org.gudy.azureus2.plugins.download.Download;
import org.gudy.azureus2.plugins.logging.LoggerChannel;
import org.gudy.azureus2.plugins.ui.UIManager;
import org.gudy.azureus2.plugins.ui.SWT.SWTManager;
import org.gudy.azureus2.plugins.ui.config.Parameter;
import org.gudy.azureus2.plugins.ui.config.PluginConfigUIFactory;
import org.gudy.azureus2.plugins.ui.menus.MenuItem;
import org.gudy.azureus2.plugins.ui.menus.MenuItemListener;
import org.gudy.azureus2.plugins.ui.tables.TableColumn;
import org.gudy.azureus2.plugins.ui.tables.TableContextMenuItem;
import org.gudy.azureus2.plugins.ui.tables.TableManager;
import org.gudy.azureus2.plugins.ui.tables.TableRow;
import se.sics.trust.azureus.plugins.trust.ui.TrustValueColumn;
import se.sics.trust.azureus.plugins.trust.ui.TrustValueWindow;
import se.sics.trust.azureus.plugins.trust.updater.TrustValueUpdater;

/**
 * Trust-eze project trust value plugin for Azureus. 
 * Based on Azureus rating plugin.
 * Created by Wang Yudan and modified by Fredrik Espinoza
 */
public class TrustValuePlugin implements Plugin, ConfigParameterListener, PluginListener, UnloadablePlugin {

    private static final String COLUMN_ID_RATING = "TrustValueColumn";

    private PluginInterface pluginInterface;

    LoggerChannel log;

    private String nick;

    private TrustValueUpdater updater;

    private static final String resPath = "se/sics/trust/azureus/plugins/trust/ui/icons/";

    public Image imgNoTrustValue;

    public Image[] imgTrustValue;

    public void initialize(PluginInterface pluginInterface) {
        this.pluginInterface = pluginInterface;
        log = pluginInterface.getLogger().getChannel("TrustValue Plugin");
        System.out.println("***************** starting trust trust plugin **********************");
        imgNoTrustValue = loadImage(resPath + "unrated.png");
        imgTrustValue = new Image[5];
        for (int i = 0; i < 5; i++) {
            imgTrustValue[i] = loadImage(resPath + "rated" + (i + 1) + ".png");
        }
        nick = pluginInterface.getPluginconfig().getPluginStringParameter("nick", "Anonymous");
        addPluginConfig();
        updater = new TrustValueUpdater(this);
        pluginInterface.addListener(this);
        addMyTorrentsColumn();
        addMyTorrentsMenu();
    }

    public void closedownComplete() {
    }

    public void closedownInitiated() {
    }

    public void initializationComplete() {
        updater.initialize();
    }

    private void addPluginConfig() {
        PluginConfigUIFactory factory = pluginInterface.getPluginConfigUIFactory();
        Parameter parameters[] = new Parameter[1];
        parameters[0] = factory.createStringParameter("nick", "trust.config.nick", "");
        parameters[0].addConfigParameterListener(this);
        pluginInterface.addConfigUIParameters(parameters, "trust.config.title");
    }

    private Image loadImage(String res) {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(res);
        if (is != null) {
            ImageData imageData = new ImageData(is);
            return new Image(pluginInterface.getUIManager().getSWTManager().getDisplay(), imageData);
        }
        return null;
    }

    private void addMyTorrentsColumn() {
        TrustValueColumn trustValueColumn = new TrustValueColumn(this);
        addTrustValueColumnToTable(TableManager.TABLE_MYTORRENTS_INCOMPLETE, trustValueColumn);
        addTrustValueColumnToTable(TableManager.TABLE_MYTORRENTS_COMPLETE, trustValueColumn);
    }

    private void addTrustValueColumnToTable(String tableID, TrustValueColumn trustValueColumn) {
        UIManager uiManager = pluginInterface.getUIManager();
        SWTManager swtManager = uiManager.getSWTManager();
        TableManager tableManager = uiManager.getTableManager();
        TableColumn activityColumn = tableManager.createColumn(tableID, COLUMN_ID_RATING);
        activityColumn.setAlignment(TableColumn.ALIGN_LEAD);
        activityColumn.setPosition(5);
        activityColumn.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
        activityColumn.setType(TableColumn.TYPE_GRAPHIC);
        activityColumn.addCellRefreshListener(trustValueColumn);
        tableManager.addColumn(activityColumn);
    }

    private void addMyTorrentsMenu() {
        MenuItemListener listener = new MenuItemListener() {

            public void selected(MenuItem _menu, Object _target) {
                Download download = (Download) ((TableRow) _target).getDataSource();
                if (download == null || download.getTorrent() == null) {
                    return;
                }
                new TrustValueWindow(TrustValuePlugin.this, download);
            }
        };
        TableContextMenuItem menu1 = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_INCOMPLETE, "TrustValuePlugin.contextmenu.manageTrustValue");
        TableContextMenuItem menu2 = pluginInterface.getUIManager().getTableManager().addContextMenuItem(TableManager.TABLE_MYTORRENTS_COMPLETE, "TrustValuePlugin.contextmenu.manageTrustValue");
        menu1.addListener(listener);
        menu2.addListener(listener);
    }

    public void configParameterChanged(ConfigParameter param) {
        nick = pluginInterface.getPluginconfig().getPluginStringParameter("nick", "Anonymous");
    }

    public PluginInterface getPluginInterface() {
        return this.pluginInterface;
    }

    public String getNick() {
        return nick;
    }

    public void logInfo(String text) {
        log.log(LoggerChannel.LT_INFORMATION, text);
    }

    public void logError(String text) {
        log.log(LoggerChannel.LT_ERROR, text);
    }

    public TrustValueUpdater getUpdater() {
        return updater;
    }

    public void unload() throws PluginException {
    }
}
