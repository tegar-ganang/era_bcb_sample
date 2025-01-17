package org.gudy.azureus2.ui.webplugin;

import java.io.*;
import java.util.*;
import java.net.*;
import org.gudy.azureus2.core3.util.*;
import org.gudy.azureus2.plugins.*;
import org.gudy.azureus2.plugins.logging.*;
import org.gudy.azureus2.plugins.ipfilter.*;
import org.gudy.azureus2.plugins.tracker.*;
import org.gudy.azureus2.plugins.tracker.web.*;
import org.gudy.azureus2.plugins.ui.*;
import org.gudy.azureus2.plugins.ui.config.*;
import org.gudy.azureus2.plugins.ui.model.*;
import com.aelitis.azureus.core.pairing.PairedService;
import com.aelitis.azureus.core.pairing.PairingConnectionData;
import com.aelitis.azureus.core.pairing.PairingManager;
import com.aelitis.azureus.core.pairing.PairingManagerFactory;
import com.aelitis.azureus.core.pairing.PairingManagerListener;
import com.aelitis.azureus.core.pairing.PairingTest;
import com.aelitis.azureus.core.pairing.PairingTestListener;
import com.aelitis.azureus.plugins.upnp.UPnPMapping;
import com.aelitis.azureus.plugins.upnp.UPnPPlugin;

public class WebPlugin implements Plugin, TrackerWebPageGenerator {

    public static final String PR_ENABLE = "Enable";

    public static final String PR_DISABLABLE = "Disablable";

    public static final String PR_PORT = "Port";

    public static final String PR_BIND_IP = "Bind IP";

    public static final String PR_ROOT_RESOURCE = "Root Resource";

    public static final String PR_ROOT_DIR = "Root Dir";

    public static final String PR_ACCESS = "Access";

    public static final String PR_LOG = "DefaultLoggerChannel";

    public static final String PR_CONFIG_MODEL_PARAMS = "DefaultConfigModelParams";

    public static final String PR_CONFIG_MODEL = "DefaultConfigModel";

    public static final String PR_VIEW_MODEL = "DefaultViewModel";

    public static final String PR_HIDE_RESOURCE_CONFIG = "DefaultHideResourceConfig";

    public static final String PR_ENABLE_KEEP_ALIVE = "DefaultEnableKeepAlive";

    public static final String PR_PAIRING_SID = "PairingSID";

    public static final String PROPERTIES_MIGRATED = "Properties Migrated";

    public static final String CONFIG_MIGRATED = "Config Migrated";

    public static final String PAIRING_MIGRATED = "Pairing Migrated";

    public static final String PAIRING_SESSION_KEY = "Pairing Session Key";

    public static final String CONFIG_PASSWORD_ENABLE = "Password Enable";

    public static final boolean CONFIG_PASSWORD_ENABLE_DEFAULT = false;

    public static final String CONFIG_PAIRING_ENABLE = "Pairing Enable";

    public static final boolean CONFIG_PAIRING_ENABLE_DEFAULT = true;

    public static final String CONFIG_PORT_OVERRIDE = "Port Override";

    public static final String CONFIG_PAIRING_AUTO_AUTH = "Pairing Auto Auth";

    public static final boolean CONFIG_PAIRING_AUTO_AUTH_DEFAULT = true;

    public static final String CONFIG_ENABLE = PR_ENABLE;

    public boolean CONFIG_ENABLE_DEFAULT = true;

    public static final String CONFIG_USER = "User";

    public static final String CONFIG_USER_DEFAULT = "";

    public static final String CONFIG_PASSWORD = "Password";

    public static final byte[] CONFIG_PASSWORD_DEFAULT = {};

    public static final String CONFIG_PORT = PR_PORT;

    public int CONFIG_PORT_DEFAULT = 8089;

    public static final String CONFIG_BIND_IP = PR_BIND_IP;

    public String CONFIG_BIND_IP_DEFAULT = "";

    public static final String CONFIG_PROTOCOL = "Protocol";

    public static final String CONFIG_PROTOCOL_DEFAULT = "HTTP";

    public static final String CONFIG_UPNP_ENABLE = "UPnP Enable";

    public static final boolean CONFIG_UPNP_ENABLE_DEFAULT = true;

    public static final String CONFIG_HOME_PAGE = "Home Page";

    public static final String CONFIG_HOME_PAGE_DEFAULT = "index.html";

    public static final String CONFIG_ROOT_DIR = PR_ROOT_DIR;

    public String CONFIG_ROOT_DIR_DEFAULT = "";

    public static final String CONFIG_ROOT_RESOURCE = PR_ROOT_RESOURCE;

    public String CONFIG_ROOT_RESOURCE_DEFAULT = "";

    public static final String CONFIG_MODE = "Mode";

    public static final String CONFIG_MODE_FULL = "full";

    public static final String CONFIG_MODE_DEFAULT = CONFIG_MODE_FULL;

    public static final String CONFIG_ACCESS = PR_ACCESS;

    public String CONFIG_ACCESS_DEFAULT = "all";

    protected static final String NL = "\r\n";

    protected static final String[] welcome_pages = { "index.html", "index.htm", "index.php", "index.tmpl" };

    protected static File[] welcome_files;

    protected PluginInterface plugin_interface;

    private LoggerChannel log;

    private PluginConfig plugin_config;

    private BasicPluginViewModel view_model;

    private BasicPluginConfigModel config_model;

    private StringParameter param_home;

    private StringParameter param_rootdir;

    private StringParameter param_rootres;

    private IntParameter param_port;

    private StringListParameter param_protocol;

    private StringParameter param_bind;

    private StringParameter param_access;

    private BooleanParameter p_upnp_enable;

    private BooleanParameter pw_enable;

    private StringParameter p_user_name;

    private PasswordParameter p_password;

    private BooleanParameter param_auto_auth;

    private IntParameter param_port_or;

    private boolean setting_auto_auth;

    private String pairing_access_code;

    private String pairing_session_code;

    private boolean plugin_enabled;

    private String home_page;

    private String file_root;

    private String resource_root;

    private String root_dir;

    private boolean ip_range_all = false;

    private List<IPRange> ip_ranges;

    private TrackerWebContext tracker_context;

    private UPnPMapping upnp_mapping;

    private PairingManagerListener pairing_listener;

    private Properties properties;

    private static ThreadLocal<String> tls = new ThreadLocal<String>() {

        public String initialValue() {
            return (null);
        }
    };

    private static final int LOGOUT_GRACE_MILLIS = 5 * 1000;

    private static final String GRACE_PERIOD_MARKER = "<grace_period>";

    private Map<String, Long> logout_timer = new HashMap<String, Long>();

    public WebPlugin() {
        properties = new Properties();
    }

    public WebPlugin(Properties defaults) {
        properties = defaults;
    }

    public void initialize(PluginInterface _plugin_interface) throws PluginException {
        plugin_interface = _plugin_interface;
        plugin_config = plugin_interface.getPluginconfig();
        Properties plugin_properties = plugin_interface.getPluginProperties();
        if (plugin_properties != null) {
            Object o = plugin_properties.get("plugin." + PR_ROOT_DIR.replaceAll(" ", "_"));
            if (o instanceof String) {
                properties.put(PR_ROOT_DIR, o);
            }
        }
        Boolean pr_enable = (Boolean) properties.get(PR_ENABLE);
        if (pr_enable != null) {
            CONFIG_ENABLE_DEFAULT = pr_enable.booleanValue();
        }
        Integer pr_port = (Integer) properties.get(PR_PORT);
        if (pr_port != null) {
            CONFIG_PORT_DEFAULT = pr_port.intValue();
        }
        String pr_bind_ip = (String) properties.get(PR_BIND_IP);
        if (pr_bind_ip != null) {
            CONFIG_BIND_IP_DEFAULT = pr_bind_ip.trim();
        }
        String pr_root_resource = (String) properties.get(PR_ROOT_RESOURCE);
        if (pr_root_resource != null) {
            CONFIG_ROOT_RESOURCE_DEFAULT = pr_root_resource;
        }
        String pr_root_dir = (String) properties.get(PR_ROOT_DIR);
        if (pr_root_dir != null) {
            CONFIG_ROOT_DIR_DEFAULT = pr_root_dir;
        }
        String pr_access = (String) properties.get(PR_ACCESS);
        if (pr_access != null) {
            CONFIG_ACCESS_DEFAULT = pr_access;
        }
        Boolean pr_hide_resource_config = (Boolean) properties.get(PR_HIDE_RESOURCE_CONFIG);
        log = (LoggerChannel) properties.get(PR_LOG);
        if (log == null) {
            log = plugin_interface.getLogger().getChannel("WebPlugin");
        }
        UIManager ui_manager = plugin_interface.getUIManager();
        view_model = (BasicPluginViewModel) properties.get(PR_VIEW_MODEL);
        if (view_model == null) {
            view_model = ui_manager.createBasicPluginViewModel(plugin_interface.getPluginName());
        }
        String sConfigSectionID = "plugins." + plugin_interface.getPluginID();
        view_model.setConfigSectionID(sConfigSectionID);
        view_model.getStatus().setText("Running");
        view_model.getActivity().setVisible(false);
        view_model.getProgress().setVisible(false);
        log.addListener(new LoggerChannelListener() {

            public void messageLogged(int type, String message) {
                view_model.getLogArea().appendText(message + "\n");
            }

            public void messageLogged(String str, Throwable error) {
                view_model.getLogArea().appendText(str + "\n");
                view_model.getLogArea().appendText(error.toString() + "\n");
            }
        });
        config_model = (BasicPluginConfigModel) properties.get(PR_CONFIG_MODEL);
        if (config_model == null) {
            String[] cm_params = (String[]) properties.get(PR_CONFIG_MODEL_PARAMS);
            if (cm_params == null || cm_params.length == 0) {
                config_model = ui_manager.createBasicPluginConfigModel(ConfigSection.SECTION_PLUGINS, sConfigSectionID);
            } else if (cm_params.length == 1) {
                config_model = ui_manager.createBasicPluginConfigModel(cm_params[0]);
            } else {
                config_model = ui_manager.createBasicPluginConfigModel(cm_params[0], cm_params[1]);
            }
        }
        boolean save_needed = false;
        if (!plugin_config.getPluginBooleanParameter(CONFIG_MIGRATED, false)) {
            plugin_config.setPluginParameter(CONFIG_MIGRATED, true);
            save_needed = true;
            plugin_config.setPluginParameter(CONFIG_PASSWORD_ENABLE, plugin_config.getBooleanParameter("Tracker Password Enable Web", CONFIG_PASSWORD_ENABLE_DEFAULT));
            plugin_config.setPluginParameter(CONFIG_USER, plugin_config.getStringParameter("Tracker Username", CONFIG_USER_DEFAULT));
            plugin_config.setPluginParameter(CONFIG_PASSWORD, plugin_config.getByteParameter("Tracker Password", CONFIG_PASSWORD_DEFAULT));
        }
        if (!plugin_config.getPluginBooleanParameter(PROPERTIES_MIGRATED, false)) {
            plugin_config.setPluginParameter(PROPERTIES_MIGRATED, true);
            Properties props = plugin_interface.getPluginProperties();
            if (props.getProperty("port", "").length() > 0) {
                save_needed = true;
                String prop_port = props.getProperty("port", "" + CONFIG_PORT_DEFAULT);
                String prop_protocol = props.getProperty("protocol", CONFIG_PROTOCOL_DEFAULT);
                String prop_home = props.getProperty("homepage", CONFIG_HOME_PAGE_DEFAULT);
                String prop_rootdir = props.getProperty("rootdir", CONFIG_ROOT_DIR_DEFAULT);
                String prop_rootres = props.getProperty("rootresource", CONFIG_ROOT_RESOURCE_DEFAULT);
                String prop_mode = props.getProperty("mode", CONFIG_MODE_DEFAULT);
                String prop_access = props.getProperty("access", CONFIG_ACCESS_DEFAULT);
                int prop_port_int = CONFIG_PORT_DEFAULT;
                try {
                    prop_port_int = Integer.parseInt(prop_port);
                } catch (Throwable e) {
                }
                plugin_config.setPluginParameter(CONFIG_PORT, prop_port_int);
                plugin_config.setPluginParameter(CONFIG_PROTOCOL, prop_protocol);
                plugin_config.setPluginParameter(CONFIG_HOME_PAGE, prop_home);
                plugin_config.setPluginParameter(CONFIG_ROOT_DIR, prop_rootdir);
                plugin_config.setPluginParameter(CONFIG_ROOT_RESOURCE, prop_rootres);
                plugin_config.setPluginParameter(CONFIG_MODE, prop_mode);
                plugin_config.setPluginParameter(CONFIG_ACCESS, prop_access);
                File props_file = new File(plugin_interface.getPluginDirectoryName(), "plugin.properties");
                PrintWriter pw = null;
                try {
                    File backup = new File(plugin_interface.getPluginDirectoryName(), "plugin.properties.bak");
                    props_file.renameTo(backup);
                    pw = new PrintWriter(new FileWriter(props_file));
                    pw.println("plugin.class=" + props.getProperty("plugin.class"));
                    pw.println("plugin.name=" + props.getProperty("plugin.name"));
                    pw.println("plugin.version=" + props.getProperty("plugin.version"));
                    pw.println("plugin.id=" + props.getProperty("plugin.id"));
                    pw.println("");
                    pw.println("# configuration has been migrated to plugin config - see view->config->plugins");
                    pw.println("# in the SWT user interface");
                    log.logAlert(LoggerChannel.LT_INFORMATION, plugin_interface.getPluginName() + " - plugin.properties settings migrated to plugin configuration.");
                } catch (Throwable e) {
                    Debug.printStackTrace(e);
                    log.logAlert(LoggerChannel.LT_ERROR, plugin_interface.getPluginName() + " - plugin.properties settings migration failed.");
                } finally {
                    if (pw != null) {
                        pw.close();
                    }
                }
            }
        }
        if (save_needed) {
            plugin_config.save();
        }
        Boolean disablable = (Boolean) properties.get(PR_DISABLABLE);
        final BooleanParameter param_enable;
        if (disablable != null && disablable) {
            param_enable = config_model.addBooleanParameter2(CONFIG_ENABLE, "webui.enable", CONFIG_ENABLE_DEFAULT);
            plugin_enabled = param_enable.getValue();
        } else {
            param_enable = null;
            plugin_enabled = true;
        }
        param_port = config_model.addIntParameter2(CONFIG_PORT, "webui.port", CONFIG_PORT_DEFAULT);
        param_bind = config_model.addStringParameter2(CONFIG_BIND_IP, "webui.bindip", CONFIG_BIND_IP_DEFAULT);
        param_protocol = config_model.addStringListParameter2(CONFIG_PROTOCOL, "webui.protocol", new String[] { "http", "https" }, CONFIG_PROTOCOL_DEFAULT);
        ParameterListener update_server_listener = new ParameterListener() {

            public void parameterChanged(Parameter param) {
                setupServer();
            }
        };
        param_port.addListener(update_server_listener);
        param_bind.addListener(update_server_listener);
        param_protocol.addListener(update_server_listener);
        p_upnp_enable = config_model.addBooleanParameter2(CONFIG_UPNP_ENABLE, "webui.upnpenable", CONFIG_UPNP_ENABLE_DEFAULT);
        p_upnp_enable.addListener(new ParameterListener() {

            public void parameterChanged(Parameter param) {
                setupUPnP();
            }
        });
        plugin_interface.addListener(new PluginListener() {

            public void initializationComplete() {
                setupUPnP();
            }

            public void closedownInitiated() {
            }

            public void closedownComplete() {
            }
        });
        final String p_sid = (String) properties.get(PR_PAIRING_SID);
        final LabelParameter pairing_info;
        final BooleanParameter pairing_enable;
        final HyperlinkParameter pairing_test;
        final HyperlinkParameter connection_test;
        if (p_sid != null) {
            final PairingManager pm = PairingManagerFactory.getSingleton();
            pairing_info = config_model.addLabelParameter2("webui.pairing.info." + (pm.isEnabled() ? "y" : "n"));
            pairing_enable = config_model.addBooleanParameter2(CONFIG_PAIRING_ENABLE, "webui.pairingenable", CONFIG_PAIRING_ENABLE_DEFAULT);
            if (!plugin_config.getPluginBooleanParameter(PAIRING_MIGRATED, false)) {
                boolean has_pw_enabled = plugin_config.getPluginBooleanParameter(CONFIG_PASSWORD_ENABLE, CONFIG_PASSWORD_ENABLE_DEFAULT);
                if (has_pw_enabled) {
                    plugin_config.setPluginParameter(CONFIG_PAIRING_AUTO_AUTH, false);
                }
                plugin_config.setPluginParameter(PAIRING_MIGRATED, true);
            }
            param_port_or = config_model.addIntParameter2(CONFIG_PORT_OVERRIDE, "webui.port.override", 0);
            param_auto_auth = config_model.addBooleanParameter2(CONFIG_PAIRING_AUTO_AUTH, "webui.pairing.autoauth", CONFIG_PAIRING_AUTO_AUTH_DEFAULT);
            param_auto_auth.addListener(new ParameterListener() {

                public void parameterChanged(Parameter param) {
                    if (pairing_enable.getValue() && pm.isEnabled()) {
                        setupAutoAuth();
                    }
                }
            });
            connection_test = config_model.addHyperlinkParameter2("webui.connectiontest", getConnectionTestURL(p_sid));
            pairing_test = config_model.addHyperlinkParameter2("webui.pairingtest", "http://remote.vuze.com/?sid=" + p_sid);
        } else {
            pairing_info = null;
            pairing_enable = null;
            param_auto_auth = null;
            param_port_or = null;
            pairing_test = null;
            connection_test = null;
        }
        config_model.createGroup("ConfigView.section.Pairing", new Parameter[] { pairing_info, pairing_enable, param_port_or, param_auto_auth, connection_test, pairing_test });
        config_model.createGroup("ConfigView.section.server", new Parameter[] { param_port, param_bind, param_protocol, p_upnp_enable });
        param_home = config_model.addStringParameter2(CONFIG_HOME_PAGE, "webui.homepage", CONFIG_HOME_PAGE_DEFAULT);
        param_rootdir = config_model.addStringParameter2(CONFIG_ROOT_DIR, "webui.rootdir", CONFIG_ROOT_DIR_DEFAULT);
        param_rootres = config_model.addStringParameter2(CONFIG_ROOT_RESOURCE, "webui.rootres", CONFIG_ROOT_RESOURCE_DEFAULT);
        if (pr_hide_resource_config != null && pr_hide_resource_config.booleanValue()) {
            param_home.setVisible(false);
            param_rootdir.setVisible(false);
            param_rootres.setVisible(false);
        } else {
            ParameterListener update_resources_listener = new ParameterListener() {

                public void parameterChanged(Parameter param) {
                    setupResources();
                }
            };
            param_home.addListener(update_resources_listener);
            param_rootdir.addListener(update_resources_listener);
            param_rootres.addListener(update_resources_listener);
        }
        LabelParameter a_label1 = config_model.addLabelParameter2("webui.mode.info");
        StringListParameter param_mode = config_model.addStringListParameter2(CONFIG_MODE, "webui.mode", new String[] { "full", "view" }, CONFIG_MODE_DEFAULT);
        LabelParameter a_label2 = config_model.addLabelParameter2("webui.access.info");
        param_access = config_model.addStringParameter2(CONFIG_ACCESS, "webui.access", CONFIG_ACCESS_DEFAULT);
        param_access.addListener(new ParameterListener() {

            public void parameterChanged(Parameter param) {
                setupAccess();
            }
        });
        pw_enable = config_model.addBooleanParameter2(CONFIG_PASSWORD_ENABLE, "webui.passwordenable", CONFIG_PASSWORD_ENABLE_DEFAULT);
        p_user_name = config_model.addStringParameter2(CONFIG_USER, "webui.user", CONFIG_USER_DEFAULT);
        p_password = config_model.addPasswordParameter2(CONFIG_PASSWORD, "webui.password", PasswordParameter.ET_SHA1, CONFIG_PASSWORD_DEFAULT);
        pw_enable.addEnabledOnSelection(p_user_name);
        pw_enable.addEnabledOnSelection(p_password);
        ParameterListener auth_change_listener = new ParameterListener() {

            public void parameterChanged(Parameter param) {
                if (param_auto_auth != null) {
                    if (!setting_auto_auth) {
                        log("Disabling pairing auto-authentication as overridden by user");
                        param_auto_auth.setValue(false);
                    }
                }
            }
        };
        p_user_name.addListener(auth_change_listener);
        p_password.addListener(auth_change_listener);
        pw_enable.addListener(auth_change_listener);
        config_model.createGroup("webui.group.access", new Parameter[] { a_label1, param_mode, a_label2, param_access, pw_enable, p_user_name, p_password });
        if (p_sid != null) {
            final PairingManager pm = PairingManagerFactory.getSingleton();
            pairing_enable.addListener(new ParameterListener() {

                public void parameterChanged(Parameter param) {
                    boolean enabled = pairing_enable.getValue();
                    param_auto_auth.setEnabled(pm.isEnabled() && enabled);
                    param_port_or.setEnabled(pm.isEnabled() && enabled);
                    boolean test_ok = pm.isEnabled() && pairing_enable.getValue() && pm.peekAccessCode() != null && !pm.hasActionOutstanding();
                    pairing_test.setEnabled(test_ok);
                    connection_test.setEnabled(test_ok);
                    setupPairing(p_sid, enabled);
                }
            });
            pairing_listener = new PairingManagerListener() {

                public void somethingChanged(PairingManager pm) {
                    pairing_info.setLabelKey("webui.pairing.info." + (pm.isEnabled() ? "y" : "n"));
                    if (plugin_enabled) {
                        pairing_enable.setEnabled(pm.isEnabled());
                        param_auto_auth.setEnabled(pm.isEnabled() && pairing_enable.getValue());
                        param_port_or.setEnabled(pm.isEnabled() && pairing_enable.getValue());
                        boolean test_ok = pm.isEnabled() && pairing_enable.getValue() && pm.peekAccessCode() != null && !pm.hasActionOutstanding();
                        pairing_test.setEnabled(test_ok);
                        connection_test.setEnabled(test_ok);
                    }
                    connection_test.setHyperlink(getConnectionTestURL(p_sid));
                    setupPairing(p_sid, pairing_enable.getValue());
                }
            };
            pairing_listener.somethingChanged(pm);
            pm.addListener(pairing_listener);
            setupPairing(p_sid, pairing_enable.getValue());
            ParameterListener update_pairing_listener = new ParameterListener() {

                public void parameterChanged(Parameter param) {
                    updatePairing(p_sid);
                    setupUPnP();
                }
            };
            param_port.addListener(update_pairing_listener);
            param_port_or.addListener(update_pairing_listener);
            param_protocol.addListener(update_pairing_listener);
        }
        if (param_enable != null) {
            final List<Parameter> changed_params = new ArrayList<Parameter>();
            if (!plugin_enabled) {
                Parameter[] params = config_model.getParameters();
                for (Parameter param : params) {
                    if (param == param_enable) {
                        continue;
                    }
                    if (param.isEnabled()) {
                        changed_params.add(param);
                        param.setEnabled(false);
                    }
                }
            }
            param_enable.addListener(new ParameterListener() {

                public void parameterChanged(Parameter e_p) {
                    plugin_enabled = ((BooleanParameter) e_p).getValue();
                    if (plugin_enabled) {
                        for (Parameter p : changed_params) {
                            p.setEnabled(true);
                        }
                    } else {
                        changed_params.clear();
                        Parameter[] params = config_model.getParameters();
                        for (Parameter param : params) {
                            if (param == e_p) {
                                continue;
                            }
                            if (param.isEnabled()) {
                                changed_params.add(param);
                                param.setEnabled(false);
                            }
                        }
                    }
                    setupServer();
                    setupUPnP();
                    if (p_sid != null) {
                        setupPairing(p_sid, pairing_enable.getValue());
                    }
                }
            });
        }
        setupResources();
        setupAccess();
        setupServer();
    }

    private String getConnectionTestURL(String sid) {
        String res = "http://pair.vuze.com/pairing/web/test?sid=" + sid;
        PairingManager pm = PairingManagerFactory.getSingleton();
        if (pm.isEnabled()) {
            String ac = pm.peekAccessCode();
            if (ac != null) {
                res += "&ac=" + ac;
            }
        }
        return (res);
    }

    protected void unloadPlugin() {
        if (view_model != null) {
            view_model.destroy();
            view_model = null;
        }
        if (config_model != null) {
            config_model.destroy();
            config_model = null;
        }
        if (tracker_context != null) {
            tracker_context.destroy();
            tracker_context = null;
        }
        if (upnp_mapping != null) {
            upnp_mapping.destroy();
            upnp_mapping = null;
        }
        if (pairing_listener != null) {
            PairingManager pm = PairingManagerFactory.getSingleton();
            pm.removeListener(pairing_listener);
            pairing_listener = null;
        }
    }

    private void setupResources() {
        home_page = param_home.getValue().trim();
        if (home_page.length() == 0) {
            home_page = null;
        } else if (!home_page.startsWith("/")) {
            home_page = "/" + home_page;
        }
        resource_root = param_rootres.getValue().trim();
        if (resource_root.length() == 0) {
            resource_root = null;
        } else if (resource_root.startsWith("/")) {
            resource_root = resource_root.substring(1);
        }
        root_dir = param_rootdir.getValue().trim();
        if (root_dir.length() == 0) {
            file_root = plugin_interface.getPluginDirectoryName();
            if (file_root == null) {
                file_root = SystemProperties.getUserPath() + "web";
            }
        } else {
            if (root_dir.startsWith(File.separator) || root_dir.indexOf(":") != -1) {
                file_root = root_dir;
            } else {
                if (File.separatorChar != '/' && root_dir.contains("/")) {
                    root_dir = root_dir.replace('/', File.separatorChar);
                }
                file_root = plugin_interface.getPluginDirectoryName();
                if (file_root != null) {
                    file_root = file_root + File.separator + root_dir;
                    if (!new File(file_root).exists()) {
                        file_root = null;
                    }
                }
                if (file_root == null) {
                    file_root = SystemProperties.getUserPath() + "web" + File.separator + root_dir;
                }
            }
        }
        File f_root = new File(file_root);
        if (!f_root.exists()) {
            String error = "WebPlugin: root dir '" + file_root + "' doesn't exist";
            log.log(LoggerChannel.LT_ERROR, error);
        } else if (!f_root.isDirectory()) {
            String error = "WebPlugin: root dir '" + file_root + "' isn't a directory";
            log.log(LoggerChannel.LT_ERROR, error);
        }
        welcome_files = new File[welcome_pages.length];
        for (int i = 0; i < welcome_pages.length; i++) {
            welcome_files[i] = new File(file_root + File.separator + welcome_pages[i]);
        }
    }

    private void setupAccess() {
        String access_str = param_access.getValue().trim();
        String ip_ranges_str = "";
        ip_ranges = null;
        ip_range_all = false;
        if (access_str.length() > 7 && Character.isDigit(access_str.charAt(0))) {
            String[] ranges = access_str.replace(';', ',').split(",");
            ip_ranges = new ArrayList<IPRange>();
            for (String range : ranges) {
                range = range.trim();
                if (range.length() > 7) {
                    IPRange ip_range = plugin_interface.getIPFilter().createRange(true);
                    int sep = range.indexOf("-");
                    if (sep == -1) {
                        ip_range.setStartIP(range);
                        ip_range.setEndIP(range);
                    } else {
                        ip_range.setStartIP(range.substring(0, sep).trim());
                        ip_range.setEndIP(range.substring(sep + 1).trim());
                    }
                    ip_range.checkValid();
                    if (!ip_range.isValid()) {
                        log.log(LoggerChannel.LT_ERROR, "Access parameter '" + range + "' is invalid");
                    } else {
                        ip_ranges.add(ip_range);
                        ip_ranges_str += (ip_ranges_str.length() == 0 ? "" : ", ") + ip_range.getStartIP() + " - " + ip_range.getEndIP();
                    }
                }
            }
            if (ip_ranges.size() == 0) {
                ip_ranges = null;
            }
        } else {
            if (access_str.equalsIgnoreCase("all") || access_str.length() == 0) {
                ip_range_all = true;
            }
        }
        log.log(LoggerChannel.LT_INFORMATION, "Acceptable IP range = " + (ip_ranges == null ? (ip_range_all ? "all" : "local") : (ip_ranges_str)));
    }

    protected void setupServer() {
        try {
            if (!plugin_enabled) {
                if (tracker_context != null) {
                    tracker_context.destroy();
                    tracker_context = null;
                }
                return;
            }
            final int port = param_port.getValue();
            String protocol_str = param_protocol.getValue().trim();
            String bind_ip_str = param_bind.getValue().trim();
            InetAddress bind_ip = null;
            if (bind_ip_str.length() > 0) {
                try {
                    bind_ip = InetAddress.getByName(bind_ip_str);
                } catch (Throwable e) {
                    log.log(LoggerChannel.LT_ERROR, "Bind IP parameter '" + bind_ip_str + "' is invalid");
                }
            }
            if (tracker_context != null) {
                URL url = tracker_context.getURLs()[0];
                String existing_protocol = url.getProtocol();
                int existing_port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
                InetAddress existing_bind_ip = tracker_context.getBindIP();
                if (existing_port == port && existing_protocol.equalsIgnoreCase(protocol_str) && sameAddress(bind_ip, existing_bind_ip)) {
                    return;
                }
                tracker_context.destroy();
                tracker_context = null;
            }
            int protocol = protocol_str.equalsIgnoreCase("HTTP") ? Tracker.PR_HTTP : Tracker.PR_HTTPS;
            log.log(LoggerChannel.LT_INFORMATION, "Server initialisation: port = " + port + (bind_ip == null ? "" : (", bind = " + bind_ip_str + ")")) + ", protocol = " + protocol_str + (root_dir.length() == 0 ? "" : (", root = " + root_dir)));
            tracker_context = plugin_interface.getTracker().createWebContext(Constants.APP_NAME + " - " + plugin_interface.getPluginName(), port, protocol, bind_ip);
            Boolean pr_enable_keep_alive = (Boolean) properties.get(PR_ENABLE_KEEP_ALIVE);
            if (pr_enable_keep_alive != null && pr_enable_keep_alive) {
                tracker_context.setEnableKeepAlive(true);
            }
            tracker_context.addPageGenerator(this);
            tracker_context.addAuthenticationListener(new TrackerAuthenticationAdapter() {

                private String last_pw = "";

                private byte[] last_hash = {};

                private final int DELAY = 10 * 1000;

                private Map<String, Object[]> fail_map = new HashMap<String, Object[]>();

                public boolean authenticate(String headers, URL resource, String user, String pw) {
                    long now = SystemTime.getMonotonousTime();
                    String client_address = getHeaderField(headers, "X-Real-IP");
                    if (client_address == null) {
                        client_address = "<unknown>";
                    }
                    synchronized (logout_timer) {
                        Long logout_time = logout_timer.get(client_address);
                        if (logout_time != null && now - logout_time <= LOGOUT_GRACE_MILLIS) {
                            tls.set(GRACE_PERIOD_MARKER);
                            return (true);
                        }
                    }
                    boolean result = authenticateSupport(headers, resource, user, pw);
                    if (!result) {
                        AESemaphore waiter = null;
                        synchronized (fail_map) {
                            Object[] x = fail_map.get(client_address);
                            if (x == null) {
                                x = new Object[] { new AESemaphore("af:waiter"), new Long(-1), new Long(-1), now };
                                fail_map.put(client_address, x);
                            } else {
                                x[1] = x[2];
                                x[2] = x[3];
                                x[3] = now;
                                long t = (Long) x[1];
                                if (now - t < 10 * 1000) {
                                    log("Too many recent authentication failures from '" + client_address + "' - rate limiting");
                                    x[2] = now + DELAY;
                                    waiter = (AESemaphore) x[0];
                                }
                            }
                        }
                        if (waiter != null) {
                            waiter.reserve(DELAY);
                        }
                    }
                    return (result);
                }

                private boolean authenticateSupport(String headers, URL resource, String user, String pw) {
                    boolean result;
                    boolean auto_auth = param_auto_auth != null && param_auto_auth.getValue();
                    if (!pw_enable.getValue()) {
                        result = true;
                    } else {
                        if (auto_auth) {
                            user = user.trim().toLowerCase();
                        }
                        if (!user.equals(p_user_name.getValue())) {
                            result = false;
                        } else {
                            byte[] hash = last_hash;
                            if (!last_pw.equals(pw)) {
                                hash = plugin_interface.getUtilities().getSecurityManager().calculateSHA1(auto_auth ? pw.toUpperCase().getBytes() : pw.getBytes());
                                last_pw = pw;
                                last_hash = hash;
                            }
                            result = Arrays.equals(hash, p_password.getValue());
                        }
                    }
                    if (result) {
                        checkCookieSet(headers, resource);
                    } else if (auto_auth) {
                        int x = checkCookieSet(headers, resource);
                        if (x == 1) {
                            result = true;
                        } else if (x == 0) {
                            result = hasOurCookie(getHeaderField(headers, "Cookie"));
                        }
                    }
                    return (result);
                }

                /**
						 * 
						 * @param headers
						 * @param resource
						 * @return 0 = unknown, 1 = ok, 2 = bad
						 */
                private int checkCookieSet(String headers, URL resource) {
                    if (pairing_access_code == null) {
                        return (2);
                    }
                    String[] locations = { resource.getQuery(), getHeaderField(headers, "Referer") };
                    for (String location : locations) {
                        if (location != null) {
                            int p1 = location.indexOf("vuze_pairing_ac=");
                            if (p1 != -1) {
                                int p2 = location.indexOf('&', p1);
                                String ac = location.substring(p1 + 16, p2 == -1 ? location.length() : p2).trim();
                                p2 = ac.indexOf('#');
                                if (p2 != -1) {
                                    ac = ac.substring(0, p2);
                                }
                                if (ac.equalsIgnoreCase(pairing_access_code)) {
                                    tls.set(pairing_session_code);
                                    return (1);
                                } else {
                                    return (2);
                                }
                            }
                        }
                    }
                    return (0);
                }

                private String getHeaderField(String headers, String field) {
                    String lc_headers = headers.toLowerCase();
                    int p1 = lc_headers.indexOf(field.toLowerCase() + ":");
                    if (p1 != -1) {
                        int p2 = lc_headers.indexOf('\n', p1);
                        if (p2 != -1) {
                            return (headers.substring(p1 + field.length() + 1, p2).trim());
                        }
                    }
                    return (null);
                }
            });
        } catch (TrackerException e) {
            log.log("Server initialisation failed", e);
        }
    }

    private boolean hasOurCookie(String cookies) {
        if (cookies == null) {
            return (false);
        }
        String[] cookie_list = cookies.split(";");
        for (String cookie : cookie_list) {
            String[] bits = cookie.split("=");
            if (bits.length == 2) {
                if (bits[0].trim().equals("vuze_pairing_sc")) {
                    if (bits[1].trim().equals(pairing_session_code)) {
                        return (true);
                    }
                }
            }
        }
        return (false);
    }

    private boolean sameAddress(InetAddress a1, InetAddress a2) {
        if (a1 == null && a2 == null) {
            return (true);
        } else if (a1 == null || a2 == null) {
            return (false);
        } else {
            return (a1.equals(a2));
        }
    }

    protected void setupUPnP() {
        if (!plugin_enabled || !p_upnp_enable.getValue()) {
            if (upnp_mapping != null) {
                log("Removing UPnP mapping");
                upnp_mapping.destroy();
                upnp_mapping = null;
            }
            return;
        }
        PluginInterface pi_upnp = plugin_interface.getPluginManager().getPluginInterfaceByClass(UPnPPlugin.class);
        if (pi_upnp == null) {
            log.log("No UPnP plugin available, not attempting port mapping");
        } else {
            int port = param_port.getValue();
            if (upnp_mapping != null) {
                if (upnp_mapping.getPort() == port) {
                    return;
                }
                log("Updating UPnP mapping");
                upnp_mapping.destroy();
            } else {
                log("Creating UPnP mapping");
            }
            upnp_mapping = ((UPnPPlugin) pi_upnp.getPlugin()).addMapping(plugin_interface.getPluginName(), true, port, true);
        }
    }

    protected void setupPairing(String sid, boolean pairing_enabled) {
        PairingManager pm = PairingManagerFactory.getSingleton();
        PairedService service = pm.getService(sid);
        if (plugin_enabled && pairing_enabled && pm.isEnabled()) {
            setupAutoAuth();
            if (service == null) {
                log("Adding pairing service");
                service = pm.addService(sid);
                PairingConnectionData cd = service.getConnectionData();
                try {
                    updatePairing(cd);
                } finally {
                    cd.sync();
                }
            }
        } else {
            pairing_access_code = null;
            pairing_session_code = null;
            if (service != null) {
                log("Removing pairing service");
                service.remove();
            }
        }
    }

    protected void setupAutoAuth() {
        PairingManager pm = PairingManagerFactory.getSingleton();
        String ac = pm.peekAccessCode();
        if (ac != null && (pairing_access_code == null || !ac.equals(pairing_access_code))) {
            synchronized (this) {
                String existing_key = plugin_config.getPluginStringParameter(PAIRING_SESSION_KEY, "");
                String[] bits = existing_key.split("=");
                if (bits.length == 2 && bits[0].equals(ac)) {
                    pairing_session_code = bits[1];
                } else {
                    pairing_session_code = Base32.encode(RandomUtils.nextSecureHash());
                    plugin_config.setPluginParameter(PAIRING_SESSION_KEY, ac + "=" + pairing_session_code);
                }
            }
        }
        pairing_access_code = ac;
        if (pairing_access_code != null) {
            if (param_auto_auth.getValue()) {
                try {
                    setting_auto_auth = true;
                    if (!p_user_name.getValue().equals("vuze")) {
                        p_user_name.setValue("vuze");
                    }
                    SHA1Hasher hasher = new SHA1Hasher();
                    byte[] encoded = hasher.calculateHash(pairing_access_code.getBytes());
                    if (!Arrays.equals(p_password.getValue(), encoded)) {
                        p_password.setValue(pairing_access_code);
                    }
                    if (!pw_enable.getValue()) {
                        pw_enable.setValue(true);
                    }
                } finally {
                    setting_auto_auth = false;
                }
            }
        }
    }

    protected void updatePairing(String sid) {
        PairingManager pm = PairingManagerFactory.getSingleton();
        PairedService service = pm.getService(sid);
        if (service != null) {
            PairingConnectionData cd = service.getConnectionData();
            log("Updating pairing information");
            try {
                updatePairing(cd);
            } finally {
                cd.sync();
            }
        }
    }

    protected void updatePairing(PairingConnectionData cd) {
        cd.setAttribute(PairingConnectionData.ATTR_PORT, String.valueOf(param_port.getValue()));
        int override = param_port_or == null ? 0 : param_port_or.getValue();
        if (override > 0) {
            cd.setAttribute(PairingConnectionData.ATTR_PORT_OVERRIDE, String.valueOf(override));
        } else {
            cd.setAttribute(PairingConnectionData.ATTR_PORT_OVERRIDE, null);
        }
        cd.setAttribute(PairingConnectionData.ATTR_PROTOCOL, param_protocol.getValue());
    }

    protected int getPort() {
        return (param_port.getValue());
    }

    protected String getProtocol() {
        return (param_protocol.getValue());
    }

    public void setUserAndPassword(String user, String password) {
        p_user_name.setValue(user);
        p_password.setValue(password);
        pw_enable.setValue(true);
    }

    public void unsetUserAndPassword() {
        pw_enable.setValue(false);
    }

    public boolean generateSupport(TrackerWebPageRequest request, TrackerWebPageResponse response) throws IOException {
        return (false);
    }

    public boolean generate(TrackerWebPageRequest request, TrackerWebPageResponse response) throws IOException {
        String client = request.getClientAddress();
        if (!ip_range_all) {
            try {
                boolean valid_ip = true;
                InetAddress ia = InetAddress.getByName(client);
                if (ip_ranges == null) {
                    if (!ia.isLoopbackAddress()) {
                        log.log(LoggerChannel.LT_ERROR, "Client '" + client + "' is not local, rejecting");
                        valid_ip = false;
                    }
                } else {
                    boolean ok = false;
                    for (IPRange range : ip_ranges) {
                        if (range.isInRange(ia.getHostAddress())) {
                            ok = true;
                        }
                    }
                    if (!ok) {
                        log.log(LoggerChannel.LT_ERROR, "Client '" + client + "' (" + ia.getHostAddress() + ") is not in range, rejecting");
                        valid_ip = false;
                    }
                }
                if (!valid_ip) {
                    response.setReplyStatus(403);
                    return (returnTextPlain(response, "Cannot access resource from this IP address."));
                }
            } catch (Throwable e) {
                Debug.printStackTrace(e);
                return (false);
            }
        }
        String url = request.getURL();
        if (url.toString().endsWith(".class")) {
            System.out.println("WebPlugin::generate:" + url);
        }
        String cookie_to_set = tls.get();
        if (cookie_to_set == GRACE_PERIOD_MARKER) {
            return (returnTextPlain(response, "Logout in progress, please try again later."));
        }
        if (cookie_to_set != null) {
            response.setHeader("Set-Cookie", "vuze_pairing_sc=" + cookie_to_set + "; HttpOnly");
            tls.set(null);
        }
        URL full_url = request.getAbsoluteURL();
        String full_url_path = full_url.getPath();
        if (full_url_path.equals("/isPairedServiceAvailable")) {
            String redirect = getArgumentFromURL(full_url, "redirect_to");
            if (redirect != null) {
                try {
                    URL target = new URL(redirect);
                    String host = target.getHost();
                    if (!Constants.isAzureusDomain(host)) {
                        if (!InetAddress.getByName(host).isLoopbackAddress()) {
                            log("Invalid redirect host: " + host);
                            redirect = null;
                        }
                    }
                } catch (Throwable e) {
                    Debug.out(e);
                    redirect = null;
                }
            }
            if (redirect != null) {
                response.setReplyStatus(302);
                response.setHeader("Location", redirect);
                return (true);
            }
            String callback = getArgumentFromURL(full_url, "jsoncallback");
            if (callback != null) {
                return (returnTextPlain(response, callback + "( {'pairedserviceavailable':true} )"));
            }
        } else if (full_url_path.equals("/isServicePaired")) {
            boolean paired = cookie_to_set != null || hasOurCookie((String) request.getHeaders().get("cookie"));
            return (returnJSON(response, "{ 'servicepaired': " + (paired ? "true" : "false") + " }"));
        } else if (full_url_path.equals("/pairedServiceLogout")) {
            synchronized (logout_timer) {
                logout_timer.put(client, SystemTime.getMonotonousTime());
            }
            response.setHeader("Set-Cookie", "vuze_pairing_sc=<deleted>, expires=" + TimeFormatter.getCookieDate(0));
            String redirect = getArgumentFromURL(full_url, "redirect_to");
            if (redirect != null) {
                try {
                    URL target = new URL(redirect);
                    String host = target.getHost();
                    if (!Constants.isAzureusDomain(host)) {
                        if (!InetAddress.getByName(host).isLoopbackAddress()) {
                            log("Invalid redirect host: " + host);
                            redirect = null;
                        }
                    }
                } catch (Throwable e) {
                    Debug.out(e);
                    redirect = null;
                }
            }
            if (redirect == null) {
                return (returnTextPlain(response, ""));
            } else {
                response.setReplyStatus(302);
                response.setHeader("Location", redirect);
                return (true);
            }
        }
        if (generateSupport(request, response)) {
            return (true);
        }
        if (url.equals("/") || url.startsWith("/?")) {
            url = "/";
            if (home_page != null) {
                url = home_page;
            } else {
                for (int i = 0; i < welcome_files.length; i++) {
                    if (welcome_files[i].exists()) {
                        url = "/" + welcome_pages[i];
                        break;
                    }
                }
            }
        }
        if (response.useFile(file_root, url)) {
            return (true);
        }
        String resource_name = url;
        if (resource_name.startsWith("/")) {
            resource_name = resource_name.substring(1);
        }
        int pos = resource_name.lastIndexOf(".");
        if (pos != -1) {
            String type = resource_name.substring(pos + 1);
            ClassLoader cl = plugin_interface.getPluginClassLoader();
            InputStream is = cl.getResourceAsStream(resource_name);
            if (is == null) {
                if (resource_root != null) {
                    resource_name = resource_root + "/" + resource_name;
                    is = cl.getResourceAsStream(resource_name);
                }
            }
            if (is != null) {
                try {
                    response.useStream(type, is);
                } finally {
                    is.close();
                }
                return (true);
            }
        }
        return (false);
    }

    private String getArgumentFromURL(URL url, String argument) {
        String query = url.getQuery();
        if (query != null) {
            String[] args = query.split("&");
            for (String arg : args) {
                String[] x = arg.split("=");
                if (x.length == 2) {
                    if (x[0].equals(argument)) {
                        return (UrlUtils.decode(x[1]));
                    }
                }
            }
        }
        return (null);
    }

    private boolean returnTextPlain(TrackerWebPageResponse response, String str) {
        return (returnStuff(response, "text/plain", str));
    }

    private boolean returnJSON(TrackerWebPageResponse response, String str) {
        return (returnStuff(response, "text/plain", str));
    }

    private boolean returnStuff(TrackerWebPageResponse response, String content_type, String str) {
        response.setContentType(content_type);
        PrintWriter pw = new PrintWriter(response.getOutputStream());
        pw.println(str);
        pw.flush();
        pw.close();
        return (true);
    }

    protected BasicPluginConfigModel getConfigModel() {
        return (config_model);
    }

    protected BasicPluginViewModel getViewModel() {
        return this.view_model;
    }

    protected void log(String str) {
        log.log(str);
    }

    protected void log(String str, Throwable e) {
        log.log(str, e);
    }
}
