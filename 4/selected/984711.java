package rsswaba.persistence;

import rsswaba.rss.RssChannel;
import rsswaba.rss.RssItem;
import rsswaba.broker.RssReader;
import rsswaba.broker.RssSAXParser;
import superwaba.ext.xplat.io.http.HttpStream;
import superwaba.ext.xplat.io.http.URI;
import superwaba.ext.xplat.util.props.Properties;
import superwaba.ext.xplat.xml.SyntaxException;
import superwaba.ext.xplat.xml.XmlReadableSocket;
import superwaba.ext.xplat.xml.XmlReader;
import waba.io.File;
import waba.sys.Convert;
import waba.sys.Vm;

/**
 * @author xp
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RssChannelHomePersistence {

    public static String dir = "/ChannelHome/";

    private int defaultBauds = -1;

    private Properties configProperties;

    private Properties channelProperties;

    private int nextChannelId = 0;

    public RssChannelHomePersistence() {
        readProxy();
    }

    public static void main(String args[]) throws Exception {
    }

    public File ensureHomeDirectory() {
        return ensureDirectory(dir);
    }

    public File ensureSubDirectory(String sub) {
        this.ensureHomeDirectory();
        return ensureDirectory(dir + sub);
    }

    private File ensureDirectory(String directory) {
        File f = new File(directory);
        if (!f.exists()) {
            Vm.debug("Create dir" + directory);
            f.createDir();
        }
        Vm.debug("Error: " + directory + " " + f.lastError + " " + f.isDir());
        return f;
    }

    public String dir() {
        return dir;
    }

    public String fileNameDescriptionFor(RssChannel rssChannel, RssItem item) {
        return dir() + this.descriptionDir(rssChannel) + "/D" + item.getId() + ".txt";
    }

    public String descriptionDir(RssChannel rssChannel) {
        return "CD" + rssChannel.getId();
    }

    public void readProxy() {
        if (proxyUse()) {
            HttpStream.proxyAddress = proxyAddress();
            HttpStream.proxyPort = proxyPort();
            ;
        } else {
            HttpStream.proxyAddress = null;
            HttpStream.proxyPort = -1;
        }
        this.defaultBauds = defaultBauds();
    }

    public void setProxyAddress(String address) {
        configProperties().put("ProxyAddress", new Properties.Str(address));
    }

    public void setProxyPort(int port) {
        configProperties().put("ProxyPort", new Properties.Int(port));
    }

    public void setProxyUse(boolean use) {
        configProperties().put("ProxyUse", new Properties.Boolean(use));
    }

    public void setDefaultBauds(int defaultBauds) {
        configProperties().put("DefaultBauds", new Properties.Int(defaultBauds));
    }

    public String proxyAddress() {
        if (configProperties().get("ProxyAddress") != null) {
            return ((Properties.Str) configProperties().get("ProxyAddress")).value;
        } else {
            return null;
        }
    }

    public int proxyPort() {
        if (configProperties().get("ProxyPort") != null) {
            return ((Properties.Int) configProperties().get("ProxyPort")).value;
        } else {
            return -1;
        }
    }

    public int defaultBauds() {
        if (configProperties().get("DefaultBauds") != null) {
            return ((Properties.Int) configProperties().get("DefaultBauds")).value;
        } else {
            return -1;
        }
    }

    public boolean proxyUse() {
        return (configProperties().get("ProxyUse") != null) && (((Properties.Boolean) (configProperties().get("ProxyUse"))).value);
    }

    public void updateChannel(RssChannel channel) throws Exception {
        RssReader reader = new RssChannelUpdateRssReader(this, channel);
        this.readChannelFromLink(channel.getRssLink(), reader);
    }

    public RssChannel readChannelFromLink(String link, RssReader reader) throws Exception {
        XmlReader xrd = new XmlReader();
        RssSAXParser rssParser = new RssSAXParser(reader);
        xrd.setContentHandler(rssParser);
        XmlReadableSocket xrs;
        if (defaultBauds < 0) xrs = new XmlReadableSocket(new URI(link)); else xrs = new XmlReadableSocket(new URI(link), defaultBauds);
        xrs.readXml(xrd);
        RssChannel rssChannel = rssParser.getRssChannel();
        reader.endParse(rssChannel);
        return rssChannel;
    }

    public Properties configProperties() {
        if (configProperties == null) {
            configProperties = new Properties();
            PropertiesUtil.readProperties(configProperties, "Config.properties");
        }
        return configProperties;
    }

    public Properties channelProperties() {
        if (channelProperties == null) {
            channelProperties = new Properties();
            PropertiesUtil.readProperties(channelProperties, "Channel.properties");
            Object[] names = channelProperties.getKeys().toObjectArray();
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    String name = (String) names[i];
                    int id = Convert.toInt(((Properties.Str) channelProperties.get(name)).value);
                    if (nextChannelId <= id) nextChannelId = id + 1;
                }
            }
        }
        return channelProperties;
    }

    /**
	 * @param rssChannelHome
	 * @param string
	 */
    public RssChannel addChannelFromLink(String link) {
        RssChannel rssChannel;
        try {
            rssChannel = this.readChannelFromLink(link, new RssChannelAddFromLinkRssReader(this, link));
        } catch (Exception e) {
            return null;
        }
        return rssChannel;
    }

    /**
	 * @param rssChannel
	 * @param properties
	 */
    public void removeChannel(RssChannel rssChannel) {
        File directory = ensureHomeDirectory();
        File file = new File(dir + rssChannel.getFileName(), File.DONT_OPEN);
        if (file.exists()) file.delete();
        File descriptionDirectory = new File(dir + descriptionDir(rssChannel), File.DONT_OPEN);
        if (descriptionDirectory.exists()) {
            String[] files = descriptionDirectory.listFiles();
            for (int i = 0; i < files.length; i++) {
                File descFile = new File(dir + descriptionDir(rssChannel) + "/" + files[i], File.DONT_OPEN);
                if (descFile.exists()) descFile.delete();
                Vm.debug(files[i]);
            }
            descriptionDirectory.delete();
        }
        channelProperties().remove(rssChannel.getName());
        saveChannels();
    }

    /**
	 * @param channel
	 */
    public void addChannel(RssChannel channel) {
        channelProperties().put(channel.getName(), new Properties.Str(channel.getId()));
        saveChannels();
        nextChannelId++;
    }

    public int nextId() {
        channelProperties();
        return nextChannelId;
    }

    private void saveChannels() {
        PropertiesUtil.saveProperties(channelProperties(), "Channel.properties");
    }

    /**
	 * @param properties
	 */
    public void saveConfig() {
        PropertiesUtil.saveProperties(configProperties(), "Config.properties");
    }

    public String[] getChannelsNames() {
        String names[] = (String[]) channelProperties().getKeys().toObjectArray();
        if (names == null) return new String[0];
        return names;
    }

    public RssChannel getRssChannelNamed(String name) {
        Properties.Str id = (Properties.Str) channelProperties().get(name);
        if (id == null) return null;
        File file = new File(dir + '/' + "Channel" + id.value + ".xml", File.READ_ONLY);
        XmlReader xrd = new XmlReader();
        RssReader r = new RssReaderDefault();
        RssSAXParser rssParser = new RssSAXParser(r);
        xrd.setContentHandler(rssParser);
        try {
            xrd.parse(file);
        } catch (SyntaxException e) {
        }
        file.close();
        RssChannel rc = rssParser.getRssChannel();
        r.endParse(rc);
        if (rc != null) rc.setId(Convert.toInt(id.value));
        return rssParser.getRssChannel();
    }

    public String getDescriptionFor(RssChannel rssChannel, RssItem item) {
        String dfName = fileNameDescriptionFor(rssChannel, item);
        File file = new File(dfName, File.READ_ONLY);
        if (!file.exists()) return "";
        byte[] buf = new byte[file.getSize()];
        file.readBytes(buf, 0, file.getSize());
        file.close();
        return new String(buf);
    }
}
