package org.gudy.azureus2.pluginsimpl.local.utils.xml.rss;

import java.net.URL;
import java.util.Properties;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.AESemaphore;
import org.gudy.azureus2.core3.util.AEThread;
import org.gudy.azureus2.plugins.Plugin;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.PluginManager;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSChannel;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSItem;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */
public class Test implements Plugin {

    private static AESemaphore init_sem = new AESemaphore("RSSTester");

    private static AEMonitor class_mon = new AEMonitor("RSSTester");

    private static Test singleton;

    public static Test getSingleton() {
        try {
            class_mon.enter();
            if (singleton == null) {
                new AEThread("plugin initialiser") {

                    public void runSupport() {
                        PluginManager.registerPlugin(Test.class);
                        Properties props = new Properties();
                        props.put(PluginManager.PR_MULTI_INSTANCE, "true");
                        PluginManager.startAzureus(PluginManager.UI_SWT, props);
                    }
                }.start();
                init_sem.reserve();
            }
            return (singleton);
        } finally {
            class_mon.exit();
        }
    }

    protected PluginInterface plugin_interface;

    public void initialize(PluginInterface _pi) {
        plugin_interface = _pi;
        singleton = this;
        init_sem.release();
        try {
            RSSFeed feed = plugin_interface.getUtilities().getRSSFeed(new URL("http://aelitis.com:7979/rss_feed.xml"));
            RSSChannel[] channels = feed.getChannels();
            for (int i = 0; i < channels.length; i++) {
                RSSChannel channel = channels[i];
                System.out.println("chan: title = " + channel.getTitle() + ", desc = " + channel.getDescription() + ", link = " + channel.getLink() + ", pub = " + channel.getPublicationDate());
                RSSItem[] items = channel.getItems();
                for (int j = 0; j < items.length; j++) {
                    RSSItem item = items[j];
                    System.out.println("    item:" + item.getTitle() + ", desc = " + item.getDescription() + ", link = " + item.getLink());
                    SimpleXMLParserDocumentNode node = item.getNode();
                    System.out.println("        [hash] " + node.getChild("torrent_sha1").getValue());
                    System.out.println("        [size] " + node.getChild("torrent_size").getValue());
                    System.out.println("        [seed] " + node.getChild("torrent_seeders").getValue());
                    System.out.println("        [leec] " + node.getChild("torrent_leechers").getValue());
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        getSingleton();
    }
}
