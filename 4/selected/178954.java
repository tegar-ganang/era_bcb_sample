package net.sf.magicmap.client.plugin;

import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.AbstractAction;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import net.sf.magicmap.artifact.Artifact;
import net.sf.magicmap.artifact.ArtifactTools;
import net.sf.magicmap.artifact.IArtifact;
import net.sf.magicmap.artifact.IConfigureable;
import net.sf.magicmap.artifact.Version;
import net.sf.magicmap.client.controller.IController;
import net.sf.magicmap.client.gui.MainFrame;
import net.sf.magicmap.client.gui.MainGUI;
import net.sf.magicmap.client.gui.forms.UserInterface;
import net.sf.magicmap.client.gui.utils.GUIUtils;
import net.sf.magicmap.client.gui.utils.table.FilteredTableModel;
import net.sf.magicmap.client.gui.utils.table.NotFilter;
import net.sf.magicmap.client.gui.views.MapView;
import net.sf.magicmap.client.plugin.action.ShowPluginsAction;
import net.sf.magicmap.client.plugin.forms.PluginsForm;
import net.sf.magicmap.client.plugin.ui.PluginDescriptorModel;
import net.sf.magicmap.client.utils.AncestorAdaptor;
import net.sf.magicmap.client.utils.Settings;
import org.apache.log4j.Logger;

/**
 * Manages all Plugin in the system.
 * The Pluginmanager has the following Attributes.
 * <dl>
 * <dt>AVAILABLE_PLUGINS</dt><dd>A List of all available plugins</dd>
 * <dt>INSTALLED_PLUGINS</dt><dd>A List of all installed plugins</dd>
 * <dt>PLUGIN_DETAILS</dt><dd>The currently selected plugins details (if any)</dd> 
 * </dl>
 */
public class PluginManager implements IConfigureable, UserInterface, Iterable<net.sf.magicmap.client.plugin.IPluginDescriptor>, ItemSelectable {

    private FilteredTableModel installedModel;

    private DefaultTableModel tableModel;

    private FilteredTableModel availableModel;

    public enum Attributes {

        AvailablePlugins("AVAILABLE_PLUGINS"), InstalledPlugins("INSTALLED_PLUGINS"), PluginDetails("PLUGIN_DETAILS"), PluginRepository("PLUGIN_REPOSITORY"), PluginActions("PLUGIN_ACTIONS");

        private final String name;

        private Attributes(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final Logger log = Logger.getLogger(PluginManager.class);

    private final Map<net.sf.magicmap.client.plugin.IPluginDescriptor, IPlugin> plugins = new TreeMap<net.sf.magicmap.client.plugin.IPluginDescriptor, IPlugin>();

    /**
     * Used to  find and setup the plugins
     */
    private final Settings settings;

    /**
     *
     */
    private final IController controller;

    /**
     * Loads all installed plugins
     */
    private net.sf.magicmap.client.plugin.PluginLoader pluginLoader;

    /**
     *  contains a list of plugins the user can install.
     */
    private PluginRepository repository;

    private PluginDescriptorModel pluginDetailModel;

    private final DefaultListSelectionModel installedPluginSelection = new DefaultListSelectionModel();

    private final DefaultListSelectionModel availablePluginSelection = new DefaultListSelectionModel();

    private final TableSelectListener tableSelectionListener = new TableSelectListener();

    private final EventListenerList listenerList = new EventListenerList();

    /**
     *
     * @param settings the clients settings.
     * @param controller the controller.
     */
    public PluginManager(Settings settings, IController controller) throws MalformedURLException {
        this.settings = settings;
        this.controller = controller;
        this.pluginLoader = null;
        ((MapView) MainGUI.getInstance().getJComponent("mapView")).addAncestorListener(new AncestorAdaptor() {

            @Override
            public void ancestorAdded(AncestorEvent event) {
                final MainFrame mainFrame = MainGUI.getInstance().getMainFrame();
                final JMenuBar bar = mainFrame.getJMenuBar();
                final JMenu jMenu = bar.getMenu(2);
                jMenu.addSeparator();
                jMenu.add(new ShowPluginsAction(mainFrame, PluginManager.this));
                jMenu.addSeparator();
                for (IPlugin plugin : plugins.values()) {
                    if (plugin instanceof UserInterface) {
                        final UserInterface ui = (UserInterface) plugin;
                        jMenu.add(new AbstractAction(plugin.getPluginInfos().getName()) {

                            private static final long serialVersionUID = 1L;

                            public void actionPerformed(ActionEvent e) {
                                JDialog d = new JDialog(mainFrame);
                                d.setContentPane(ui.visualProxy("", null));
                                d.pack();
                                d.setVisible(true);
                            }
                        });
                    }
                }
            }
        });
        repository = new PluginRepository(new URL("http://phl.informatik.hu-berlin.de/magicmap-plugins/"));
        tableModel = new DefaultTableModel() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tableModel.addColumn(GUIUtils.i18n("plugins.name"));
        tableModel.addColumn(GUIUtils.i18n("plugins.version"));
        tableModel.addColumn("--");
        installedModel = new FilteredTableModel(tableModel);
        FilteredTableModel.Filter installedFilter = new FilteredTableModel.Filter() {

            public boolean evaluate(Object[] o) {
                return isInstalled((IArtifact) o[0]);
            }

            public int[] getColumns() {
                return new int[] { 2 };
            }
        };
        installedModel.addFilter(installedFilter);
        availableModel = new FilteredTableModel(tableModel);
        availableModel.addFilter(new NotFilter(installedFilter));
        setup(settings);
        pluginDetailModel = new net.sf.magicmap.client.plugin.ui.PluginDescriptorModel();
        addItemListener(pluginDetailModel);
        installedPluginSelection.addListSelectionListener(tableSelectionListener);
        availablePluginSelection.addListSelectionListener(tableSelectionListener);
    }

    /**
     * Adds the plugin. If the plugin exist an IllegalArgumentException will
     * be thrown.
     *
     * @param plugin
     * @throws IllegalArgumentException
     */
    private void addPlugin(IPlugin plugin) throws IllegalArgumentException {
        plugin.setController(this.controller);
        plugin.setup(this.settings);
        plugins.put(plugin.getPluginInfos(), plugin);
        tableModel.addRow(new Object[] { plugin.getPluginInfos().getName(), plugin.getVersion(), plugin.getPluginInfos() });
    }

    /**
     *
     */
    private void loadPlugins() {
        this.pluginLoader.loadPlugins();
        for (IPlugin iPlugin : this.pluginLoader) addPlugin(iPlugin);
    }

    /**
     *
     * @throws IOException
     */
    public void loadRepository() throws IOException {
        Collection<net.sf.magicmap.client.plugin.IPluginDescriptor> collection = repository.loadIndex();
        for (net.sf.magicmap.client.plugin.IPluginDescriptor descriptor : collection) {
            if (plugins.containsKey(descriptor)) continue;
            plugins.put(descriptor, AbstractPlugin.EMPTY_PLUGIN);
            tableModel.addRow(new Object[] { descriptor.getName(), descriptor.getVersion(), descriptor });
        }
    }

    /**
     *
     * @param groupId
     * @param pluginId
     * @param version
     * @return
     */
    public net.sf.magicmap.client.plugin.IPlugin getPlugin(String groupId, String pluginId, String version) {
        Artifact a = new Artifact(groupId, pluginId, version);
        if (plugins.containsKey(a)) return plugins.get(a);
        return null;
    }

    public Collection<Version> getVersions(String groupId, String artifactId) {
        return Collections.EMPTY_LIST;
    }

    /**
     *
     * @param settings
     */
    public void setup(Settings settings) {
        String[] pluginPaths = settings.getPluginPaths();
        ArrayList<URL> urlList = new ArrayList<URL>();
        for (String url : pluginPaths) try {
            urlList.add(new URL(url));
        } catch (MalformedURLException e) {
            PluginManager.log.info(e);
        }
        this.pluginLoader = new net.sf.magicmap.client.plugin.PluginLoader(urlList.toArray(new URL[0]));
        loadPlugins();
    }

    /**
     *
     * @param attributeName the name of the attribute to visualize.
     * @param parent a Scrollpane for example.
     * @return
     * @throws IllegalArgumentException
     */
    public JComponent visualProxy(String attributeName, JComponent parent) throws IllegalArgumentException {
        JComponent proxy = null;
        if (Attributes.AvailablePlugins.getName().equals(attributeName)) {
            JTable table = new JTable(availableModel);
            table.setDefaultRenderer(net.sf.magicmap.client.plugin.IPluginDescriptor.class, new net.sf.magicmap.client.plugin.ui.PluginTableRenderer(this));
            table.setSelectionModel(availablePluginSelection);
            proxy = new JScrollPane(table);
        } else if (Attributes.InstalledPlugins.getName().equals(attributeName)) {
            JTable table = new JTable(installedModel);
            table.setSelectionModel(installedPluginSelection);
            proxy = new JScrollPane(table);
        } else if (Attributes.PluginDetails.getName().equals(attributeName)) {
            proxy = new JLabel("Later");
        } else if ("".equals(attributeName)) {
            net.sf.magicmap.client.plugin.forms.PluginsForm form = new PluginsForm(this);
            proxy = form.attatch(this);
        } else if (Attributes.PluginRepository.getName().equals(attributeName)) {
            JTextField tf = new JTextField("http://phl.informatik.hu-berlin.de/magicmap-plugins/");
            tf.setEditable(false);
            proxy = tf;
        } else {
            proxy = pluginDetailModel.visualProxy(attributeName, parent);
        }
        if (parent != null) {
            parent.add(proxy);
        }
        return proxy;
    }

    public String[] getAttributeNames() {
        return new String[] { Attributes.InstalledPlugins.getName(), Attributes.AvailablePlugins.getName(), Attributes.PluginDetails.getName() };
    }

    /**
     * Called from the controller to notifi all installed plugins that
     * the cleint is now connected to a server.
     */
    public void connect() {
        for (IPlugin iPlugin : plugins.values()) iPlugin.connect();
    }

    /**
     * Disposes all Plugins.
     */
    public void dispose() {
        for (IPlugin iPlugin : plugins.values()) iPlugin.disposePlugin();
    }

    /**
     * Called by the controller if a new Map was loaded.
     */
    public void loadMap() {
        for (IPlugin iPlugin : plugins.values()) iPlugin.loadMap();
    }

    /**
     * Iinstalls a plugin.
     * @param pluginToInstall the plugin to install.
     */
    public void installPlugin(net.sf.magicmap.client.plugin.IPluginDescriptor pluginToInstall) throws IOException {
        URL pluginUrl = repository.getPluginUrl(pluginToInstall);
        try {
            ArtifactTools tool = new ArtifactTools();
            File installDir = new File(pluginLoader.getURLs()[0].toURI());
            File jarFile = new File(installDir + "/" + tool.getFileName(pluginToInstall));
            if (jarFile.exists()) {
                uninstallPlugin(pluginToInstall);
                jarFile = new File(installDir + "/" + tool.getFileName(pluginToInstall));
            }
            if (jarFile.createNewFile()) {
                FileOutputStream oStr = new FileOutputStream(jarFile);
                InputStream iStr = pluginUrl.openStream();
                copy(iStr, oStr);
            } else {
                throw new IOException("Can not create new file");
            }
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     *
     * @param iStr
     * @param oStr
     * @throws IOException
     */
    private void copy(InputStream iStr, OutputStream oStr) throws IOException {
        try {
            while (iStr.available() > 0) {
                oStr.write(iStr.read());
            }
        } finally {
            if (oStr != null) {
                oStr.flush();
                oStr.close();
            }
            if (iStr != null) {
                iStr.close();
            }
        }
    }

    /**
     * uninstalls a plugin.
     *
     * @param pluginToRemove
     */
    public void uninstallPlugin(net.sf.magicmap.client.plugin.IPluginDescriptor pluginToRemove) throws IOException {
        ArtifactTools tool = new ArtifactTools();
        File installDir = null;
        try {
            installDir = new File(pluginLoader.getURLs()[0].toURI());
            File jarFile = new File(installDir + "/" + tool.getFileName(pluginToRemove));
            boolean b = jarFile.delete();
            if (!b) throw new IOException("Can not delete File: " + jarFile.getAbsoluteFile().toString());
        } catch (URISyntaxException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     *
     * @param artiafact the atrifact to check.
     * @return true if a plugin with the given artifactId is installed.
     */
    public boolean isInstalled(IArtifact artiafact) {
        return plugins.containsKey(artiafact) && plugins.get(artiafact) != net.sf.magicmap.client.plugin.AbstractPlugin.EMPTY_PLUGIN;
    }

    /**
     *
     * @return
     */
    public Iterator<net.sf.magicmap.client.plugin.IPluginDescriptor> iterator() {
        return plugins.keySet().iterator();
    }

    public boolean isUpdateAvailable(IArtifact artifact) {
        return false;
    }

    /**
     *
     * @return
     */
    public int getNumberOfPlugins() {
        return plugins.size();
    }

    public Object[] getSelectedObjects() {
        return tableSelectionListener.selectedPluginDescriptor == null ? new Object[0] : new Object[] { tableSelectionListener.selectedPluginDescriptor };
    }

    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    /**
     * Combines the two views of the plugins.
     */
    private class TableSelectListener implements ListSelectionListener {

        private net.sf.magicmap.client.plugin.IPluginDescriptor selectedPluginDescriptor = null;

        public void valueChanged(ListSelectionEvent e) {
            if (e.getValueIsAdjusting()) return;
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (lsm.isSelectionEmpty()) {
                System.out.println("No rows are selected.");
            } else {
                int selectedRow = lsm.getMinSelectionIndex();
                TableModel model = getTableModel(lsm);
                selectedPluginDescriptor = ((net.sf.magicmap.client.plugin.IPluginDescriptor) model.getValueAt(selectedRow, 2));
            }
            ItemListener[] itemListeners = listenerList.getListeners(ItemListener.class);
            ItemEvent event = null;
            for (ItemListener itl : itemListeners) {
                if (event == null) {
                    event = new ItemEvent(PluginManager.this, selectedPluginDescriptor == null ? 0 : selectedPluginDescriptor.hashCode(), selectedPluginDescriptor, ItemEvent.SELECTED);
                }
                itl.itemStateChanged(event);
            }
        }

        private TableModel getTableModel(ListSelectionModel lsm) {
            if (lsm.equals(installedPluginSelection)) {
                availablePluginSelection.clearSelection();
                return installedModel;
            } else if (lsm.equals(availablePluginSelection)) {
                installedPluginSelection.clearSelection();
                return availableModel;
            }
            return null;
        }
    }
}
