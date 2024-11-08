package org.jdna.bmt.web.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdna.bmt.web.client.ui.prefs.Channel;
import org.jdna.bmt.web.client.ui.prefs.ChannelNumberComparator;
import org.jdna.bmt.web.client.ui.prefs.ConfigError;
import org.jdna.bmt.web.client.ui.prefs.Log4jPrefs;
import org.jdna.bmt.web.client.ui.prefs.PluginDetail;
import org.jdna.bmt.web.client.ui.prefs.PluginQuery;
import org.jdna.bmt.web.client.ui.prefs.PrefItem;
import org.jdna.bmt.web.client.ui.prefs.PreferencesService;
import org.jdna.bmt.web.client.ui.prefs.RegexValidation;
import org.jdna.bmt.web.client.ui.prefs.VideoSource;
import org.jdna.bmt.web.client.ui.prefs.VideoSource.SourceType;
import org.jdna.bmt.web.client.ui.util.ServiceReply;
import sage.SageTVEventListener;
import sagex.api.ChannelAPI;
import sagex.api.Configuration;
import sagex.api.Global;
import sagex.api.PluginAPI;
import sagex.phoenix.ConfigurationErrorEventBus;
import sagex.phoenix.ConfigurationErrorEventBus.ConfigurationErrorItem;
import sagex.phoenix.Phoenix;
import sagex.phoenix.configuration.Field;
import sagex.phoenix.configuration.Group;
import sagex.phoenix.configuration.IConfigurationElement;
import sagex.phoenix.configuration.NewSearchGroup;
import sagex.phoenix.menu.Menu;
import sagex.phoenix.menu.XmlMenuSerializer;
import sagex.phoenix.plugin.PhoenixPlugin;
import sagex.phoenix.util.PropertiesUtils;
import sagex.phoenix.util.SortedProperties;
import sagex.util.Log4jConfigurator;
import sagex.util.Log4jConfigurator.LogStruct;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class PreferencesServiceImpl extends RemoteServiceServlet implements PreferencesService {

    private static final Logger log = Logger.getLogger(PreferencesServiceImpl.class);

    private Map<String, Group> groups = null;

    public PreferencesServiceImpl() {
        groups = new HashMap<String, Group>();
        ServicesInit.init();
    }

    public PrefItem[] getPreferences(PrefItem parent) {
        try {
            Group group = null;
            if (parent == null) {
                group = phoenix.config.GetConfigurationRoot();
            } else {
                group = groups.get(parent.getKey());
            }
            if (group == null) {
                log.warn("No Configuration For: " + parent.getKey());
                return null;
            }
            return createChildenArray(group);
        } catch (Throwable t) {
            log.error("getPreferences failed!", t);
            return null;
        }
    }

    public PrefItem searchPreferences(String search) {
        log.debug("*** Search: " + search);
        PrefItem pi = new PrefItem();
        pi.setLabel("Search: " + search);
        pi.setChildren(createChildenArray(phoenix.config.AddConfigurationSearch(search)));
        return pi;
    }

    private PrefItem[] createChildenArray(Group group) {
        List<PrefItem> items = new LinkedList<PrefItem>();
        for (IConfigurationElement g : group.getChildren()) {
            if (!g.isVisible()) continue;
            if (NewSearchGroup.NEW_SEARCH_GROUP_ID.equals(g.getId())) continue;
            PrefItem pi = new PrefItem();
            pi.setHints(g.getHints());
            pi.setLabel(g.getLabel());
            pi.setDescription(g.getDescription());
            pi.setKey(g.getId());
            log.debug("Item: " + g.getId());
            if (g.getElementType() == Group.GROUP || g.getElementType() == Group.APPLICATION) {
                log.debug("Group: " + true);
                pi.setGroup(true);
                pi.setKey(String.valueOf(g.hashCode()));
                groups.put(pi.getKey(), (Group) g);
            } else {
                Object o = phoenix.config.GetProperty(g.getId());
                if (o != null) {
                    pi.setValue(String.valueOf(o));
                    pi.setResetValue(pi.getValue());
                }
                o = phoenix.config.GetConfigurationDefaultValue(g);
                if (o != null) {
                    pi.setDefaultValue(String.valueOf(o));
                }
                pi.setType(phoenix.config.GetConfigurationFieldType(g));
                pi.setListSeparator(((Field) g).getListSeparator());
            }
            items.add(pi);
        }
        return items.toArray(new PrefItem[items.size()]);
    }

    public static void main(String args[]) {
        BasicConfigurator.configure();
        PreferencesServiceImpl impl = new PreferencesServiceImpl();
        for (PrefItem pi : impl.getPreferences(null)) {
            log.debug("Label: " + pi.getLabel());
        }
    }

    public boolean savePreferences(PrefItem[] preferences) {
        for (PrefItem pi : preferences) {
            log.info(String.format("Saving Preference (%s): %s=%s\n", pi.getLabel(), pi.getKey(), pi.getValue()));
            phoenix.config.SetProperty(pi.getKey(), pi.getValue());
        }
        return true;
    }

    private Properties getLogProperties() {
        Properties props = new SortedProperties();
        File f = new File("log4j.properties");
        if (f.exists()) {
            try {
                PropertiesUtils.load(props, f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return props;
    }

    private void saveProperties(Properties props) throws IOException {
        File f = new File("log4j.properties");
        PropertiesUtils.store(props, f, "log4j configuration created using BMT Web UI");
        PropertyConfigurator.configure(props);
        log.info("logging is reconfigured.");
    }

    public Log4jPrefs getLog4jPreferences() {
        Log4jPrefs prefs = new Log4jPrefs();
        Properties props = getLogProperties();
        String level = props.getProperty("log4j.rootCategory", "error, log");
        String parts[] = level.split(",");
        prefs.setLevel(parts[0]);
        prefs.setFile(props.getProperty("log4j.appender.log.File", "sage-plugins-log4j.log"));
        prefs.setPattern(props.getProperty("log4j.appender.log.layout.ConversionPattern", "%5r %-5p [%t] %c - %m%n"));
        prefs.setBmtLevel(props.getProperty("log4j.logger.bmt", "info"));
        prefs.setPhoenixLevel(props.getProperty("log4j.logger.phoenix", "info"));
        return prefs;
    }

    public String saveLog4jPreferences(Log4jPrefs prefs) {
        log.info("Saving Preferences");
        Properties props = getLogProperties();
        String defaultLog = "log";
        props.setProperty("log4j.rootCategory", String.format("%s, %s", prefs.getLevel().get(), defaultLog));
        props.setProperty("log4j.appender.log.File", prefs.getFile().get());
        props.setProperty("log4j.appender.log.layout.ConversionPattern", prefs.getPattern().get());
        props.setProperty("log4j.appender.log", "org.apache.log4j.DailyRollingFileAppender");
        props.setProperty("log4j.appender.log.DatePattern", "'.'yyyy-MM-dd");
        props.setProperty("log4j.appender.log.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.logger.org.jdna", prefs.getBmtLevel().get());
        props.setProperty("log4j.logger.bmt", prefs.getBmtLevel().get());
        props.setProperty("log4j.logger.phoenix", prefs.getPhoenixLevel().get());
        props.setProperty("log4j.logger.sagex.phoenix", prefs.getPhoenixLevel().get());
        try {
            saveProperties(props);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "ok";
    }

    public List<VideoSource> getVideoSources() {
        List<VideoSource> list = new ArrayList<VideoSource>();
        File files[] = Configuration.GetVideoLibraryImportPaths();
        if (files != null) {
            for (File f : files) {
                list.add(new VideoSource(f.getAbsolutePath(), SourceType.VIDEO));
            }
        }
        return list;
    }

    public List<VideoSource> saveVideoSources(List<VideoSource> sources) {
        for (VideoSource vs : sources) {
            if (vs.isDeleted()) {
                Configuration.RemoveVideoLibraryImportPath(new File(vs.getPath()));
            } else if (vs.isNew()) {
                Configuration.AddVideoLibraryImportPath(vs.getPath());
            }
        }
        return getVideoSources();
    }

    public List<PrefItem> getSageProperties() {
        List<PrefItem> items = new ArrayList<PrefItem>();
        Properties props = new Properties();
        try {
            PropertiesUtils.load(props, new File("Sage.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Sage.properties");
        }
        for (Object key : props.keySet()) {
            PrefItem i = new PrefItem();
            i.setKey(String.valueOf(key));
            i.setLabel(String.valueOf(key));
            i.setValue(String.valueOf(props.getProperty(String.valueOf(key))));
            i.setType("string");
            items.add(i);
        }
        return items;
    }

    public String getSagePropertiesAsString() {
        try {
            return IOUtils.toString(new FileReader(new File("Sage.properties")));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Sage.properties");
        }
    }

    public ArrayList<String> getSagePropertiesAsList() {
        ArrayList<String> items = new ArrayList<String>();
        Properties props = new Properties();
        try {
            PropertiesUtils.load(props, new File("Sage.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Sage.properties");
        }
        for (Map.Entry<Object, Object> es : props.entrySet()) {
            items.add(es.getKey() + "=" + es.getValue());
        }
        Collections.sort(items);
        return items;
    }

    public RegexValidation validateRegex(RegexValidation val) {
        try {
            val.setResults(null);
            Pattern p = Pattern.compile(val.getRegex());
            LineIterator li = IOUtils.lineIterator(IOUtils.toInputStream(val.getSampleData(), "UTF-8"), "UTF-8");
            while (li.hasNext()) {
                String s = li.nextLine();
                Matcher m = p.matcher(s);
                if (m.find()) {
                    if (val.getResults() == null) {
                        val.setResults(s);
                    } else {
                        val.setResults(val.getResults() + "\n" + s);
                    }
                }
            }
            if (StringUtils.isEmpty(val.getResults())) {
                val.setResults("Regex is valid, but it did not match anything.");
            }
        } catch (Exception e) {
            val.setResults(ExceptionUtils.getStackTrace(e));
            val.setValid(false);
        }
        return val;
    }

    public String[] getLog4jLoggers() {
        Log4jConfigurator.LogStruct[] logs = Log4jConfigurator.getConfiguredLogs();
        if (logs == null) {
            return null;
        }
        String log[] = new String[logs.length];
        for (int i = 0; i < logs.length; i++) {
            log[i] = logs[i].id;
        }
        return log;
    }

    private LogStruct getLog(String log) {
        Log4jConfigurator.LogStruct[] logs = Log4jConfigurator.getConfiguredLogs();
        if (logs == null) {
            return null;
        }
        for (int i = 0; i < logs.length; i++) {
            if (log.equals(logs[i].id)) return logs[i];
        }
        return null;
    }

    public ArrayList<PrefItem> getLog4jProperties(String logId) {
        ArrayList<PrefItem> items = new ArrayList<PrefItem>();
        LogStruct log = getLog(logId);
        for (Map.Entry<Object, Object> me : log.properties.entrySet()) {
            PrefItem pi = new PrefItem();
            pi.setKey(String.valueOf(me.getKey()));
            pi.setValue(String.valueOf(me.getValue()));
            items.add(pi);
        }
        Collections.sort(items, new Comparator<PrefItem>() {

            public int compare(PrefItem p1, PrefItem p2) {
                return p1.getKey().compareTo(p2.getKey());
            }
        });
        return items;
    }

    public void saveLog4jProperties(String logId, ArrayList<PrefItem> items) {
        Properties props = new Properties();
        for (PrefItem p : items) {
            props.setProperty(p.getKey(), p.getValue());
        }
        Log4jConfigurator.reconfigure(logId, props);
    }

    @Override
    public void refreshConfiguration(String id) {
        if (id == null) throw new RuntimeException("Invalid Refresh ID");
        log.info("Refreshing Configuration: " + id);
        if (REFRESH_VFS.equals(id)) {
            phoenix.umb.ReloadViews();
        } else if (REFRESH_MENUS.equals(id)) {
            phoenix.menu.ReloadMenus();
        } else if (REFRESH_MEDIA_TITLES.equals(id)) {
            phoenix.umb.ReloadMediaTitles();
        } else if (REFRESH_IMAGE_CACHE.equals(id)) {
            phoenix.fanart.ClearMemoryCaches();
        } else {
            throw new RuntimeException("Invalid Configuration to Refresh: " + id);
        }
    }

    @Override
    public ArrayList<Channel> getChannels() {
        ArrayList<Channel> channels = new ArrayList<Channel>();
        Object allChan[] = ChannelAPI.GetAllChannels();
        for (Object ch : allChan) {
            Channel c = new Channel();
            c.setNumber(ChannelAPI.GetChannelNumber(ch));
            if (StringUtils.isEmpty(c.getNumber())) continue;
            c.setDescription(ChannelAPI.GetChannelDescription(ch));
            c.setName(ChannelAPI.GetChannelName(ch));
            c.setNetwork(ChannelAPI.GetChannelNetwork(ch));
            c.enabled().set(ChannelAPI.IsChannelViewable(ch));
            c.setStationId(ChannelAPI.GetStationID(ch));
            channels.add(c);
        }
        Collections.sort(channels, new ChannelNumberComparator());
        return channels;
    }

    @Override
    public ArrayList<Channel> saveChannels(ArrayList<Channel> channels) {
        ArrayList<Channel> updated = new ArrayList<Channel>();
        String lineups[] = Global.GetAllLineups();
        if (lineups == null || lineups.length == 0) {
            return updated;
        }
        for (Channel c : channels) {
            Object chan = ChannelAPI.GetChannelForStationID(c.getStationId());
            if (chan != null) {
                if (ChannelAPI.IsChannelViewable(chan) != c.enabled().get()) {
                    for (String l : lineups) {
                        log.info("Channel was modified: " + c.getNumber() + "; Visible: " + c.enabled().get());
                        ChannelAPI.SetChannelViewabilityForChannelOnLineup(chan, l, c.enabled().get());
                        updated.add(c);
                    }
                }
            }
        }
        return updated;
    }

    @Override
    public void refreshCustomMetadataFields() {
        PhoenixPlugin.updateCustomMetadataFields();
    }

    public ArrayList<PluginDetail> getPlugins(PluginQuery query) {
        Object plugins[] = null;
        ArrayList<PluginDetail> list = new ArrayList<PluginDetail>();
        if (PluginQuery.SOURCE_INSTALLED.equals(query.Source)) {
            plugins = PluginAPI.GetInstalledPlugins();
        } else if (PluginQuery.SOURCE_CLIENT_INSTALLED.equals(query.Source)) {
            plugins = PluginAPI.GetInstalledClientPlugins();
        } else {
            plugins = PluginAPI.GetAllAvailablePlugins();
        }
        if (PluginQuery.QUERY_AUTHOR.equals(query.QueryType)) {
            for (Object p : plugins) {
                String auth = PluginAPI.GetPluginAuthor(p);
                if (query.Query.equalsIgnoreCase(auth)) {
                    list.add(createPlugin(query, p));
                } else if (auth != null && auth.contains(query.Query)) {
                    list.add(createPlugin(query, p));
                }
            }
        } else if (PluginQuery.QUERY_SEARCH_ALL.equals(query.QueryType)) {
            String qstr = query.Query;
            String qtype = null;
            if (qstr == null) return null;
            String parts[] = qstr.split("\\s*:\\s*");
            if (parts.length > 1) {
                qstr = parts[1];
                qtype = parts[0];
            }
            Pattern pat = Pattern.compile(Matcher.quoteReplacement(qstr), Pattern.CASE_INSENSITIVE);
            for (Object p : plugins) {
                if ("depends".equals(qtype)) {
                    String deps[] = PluginAPI.GetPluginDependencies(p);
                    if (deps != null) {
                        for (String s : deps) {
                            Matcher m1 = pat.matcher(s);
                            if (m1.find()) {
                                list.add(createPlugin(query, p));
                                break;
                            }
                        }
                    }
                } else {
                    Matcher m1 = pat.matcher(PluginAPI.GetPluginName(p));
                    if (m1.find()) {
                        list.add(createPlugin(query, p));
                        continue;
                    }
                    m1 = pat.matcher(PluginAPI.GetPluginDescription(p));
                    if (m1.find()) {
                        list.add(createPlugin(query, p));
                        continue;
                    }
                    m1 = pat.matcher(PluginAPI.GetPluginIdentifier(p));
                    if (m1.find()) {
                        list.add(createPlugin(query, p));
                        continue;
                    }
                }
            }
        } else {
            for (Object p : plugins) {
                if (query.Type != null) {
                    if (query.Type.equalsIgnoreCase(PluginAPI.GetPluginType(p))) {
                        list.add(createPlugin(query, p));
                    }
                } else {
                    list.add(createPlugin(query, p));
                }
            }
        }
        Collections.sort(list, new Comparator<PluginDetail>() {

            @Override
            public int compare(PluginDetail o1, PluginDetail o2) {
                if (o1.getName() == null) return -1;
                if (o2.getName() == null) return 1;
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return list;
    }

    private PluginDetail createPlugin(PluginQuery query, Object p) {
        PluginDetail det = new PluginDetail();
        det.setName(PluginAPI.GetPluginName(p));
        det.setDescription(PluginAPI.GetPluginDescription(p));
        det.setVersion(PluginAPI.GetPluginVersion(p));
        det.setId(PluginAPI.GetPluginIdentifier(p));
        det.setAuthor(PluginAPI.GetPluginAuthor(p));
        det.setCreatedDate(PluginAPI.GetPluginCreationDate(p));
        det.setDemoVideos(PluginAPI.GetPluginDemoVideos(p));
        det.setPluginDependencies(PluginAPI.GetPluginDependencies(p));
        det.setInstalledDate(PluginAPI.GetPluginInstallDate(p));
        det.setLastModified(PluginAPI.GetPluginModificationDate(p));
        det.setReleaseNotes(PluginAPI.GetPluginReleaseNotes(p));
        det.setScreenShots(PluginAPI.GetPluginScreenshots(p));
        det.setPluginWebsites(PluginAPI.GetPluginWebsites(p));
        return det;
    }

    @Override
    public PluginDetail getPluginDetails(String id) {
        if (id == null) return null;
        Object[] plugins = PluginAPI.GetAllAvailablePlugins();
        if (plugins != null) {
            for (Object p : plugins) {
                if (id.equals(PluginAPI.GetPluginIdentifier(p))) {
                    return createPlugin(null, p);
                }
            }
        }
        return null;
    }

    @Override
    public ArrayList<String> getMenus() {
        ArrayList<String> l = new ArrayList<String>();
        for (Menu m : Phoenix.getInstance().getMenuManager().getMenus()) {
            l.add(m.getId());
        }
        return l;
    }

    @Override
    public String loadMenu(String id) {
        Menu m = Phoenix.getInstance().getMenuManager().getMenu(id);
        XmlMenuSerializer ser = new XmlMenuSerializer();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ser.serialize(m, os);
        } catch (IOException e) {
            log.warn("Failed to load menu: " + m, e);
            throw new RuntimeException("Unable to load menu");
        }
        return os.toString();
    }

    @Override
    public String saveMenu(String menu) throws Exception {
        return menu;
    }

    @Override
    public synchronized ServiceReply<ArrayList<ConfigError>> refreshConfigurations() {
        final ArrayList<ConfigError> msgs = new ArrayList<ConfigError>();
        final SageTVEventListener l = new SageTVEventListener() {

            @Override
            public void sageEvent(String name, Map args) {
                ConfigurationErrorEventBus.ConfigurationErrorItem ci = (ConfigurationErrorItem) args.get(ConfigurationErrorEventBus.ERROR_KEY);
                if (ci.exception != null) {
                    ConfigError ce = new ConfigError();
                    ce.file = ci.name;
                    ce.message = ci.exception.getMessage();
                    ce.datetime = ci.datetime;
                    ce.line = ci.exception.getLineNumber();
                    ce.column = ci.exception.getColumnNumber();
                    msgs.add(ce);
                } else {
                    ConfigError ce = new ConfigError();
                    ce.file = ci.name;
                    ce.datetime = ci.datetime;
                    msgs.add(ce);
                }
            }
        };
        ConfigurationErrorEventBus.getBus().addListener(ConfigurationErrorEventBus.EVENT_NEW_ERROR, l);
        try {
            phoenix.umb.ReloadViews();
            phoenix.menu.ReloadMenus();
            phoenix.umb.ReloadMediaTitles();
        } finally {
            ConfigurationErrorEventBus.getBus().removeListener(ConfigurationErrorEventBus.EVENT_NEW_ERROR, l);
        }
        return new ServiceReply<ArrayList<ConfigError>>(msgs);
    }
}
