import rsswaba.rss.RssChannel;
import rsswaba.persistence.RssChannelHomePersistence;
import superwaba.ext.xplat.ui.MultiEdit;
import waba.sys.Settings;
import waba.ui.*;

/**
 * @author xp
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RssChannelsWindow extends Window {

    Button addB;

    Button removeB;

    Button okB;

    MultiEdit channelLink;

    ListBox channelsLb;

    RssChannelHomePersistence rssChannelHome;

    public RssChannelsWindow(RssChannelHomePersistence rsh) {
        super("Rss Channels", ROUND_BORDER);
        rssChannelHome = rsh;
        setRect(CENTER, CENTER, Settings.screenWidth * 150 / 160, Settings.screenHeight * 100 / 130);
        String lbItems[] = { "                                           ", "                                           ", "                                           " };
        this.add(new Label("Link"), SAME, AFTER);
        addB = new Button(" + ");
        this.add(addB, AFTER + 3, SAME);
        removeB = new Button(" - ");
        this.add(removeB, AFTER + 1, SAME);
        channelLink = new MultiEdit("                                                                     ", 4, 1);
        this.add(channelLink, LEFT, AFTER);
        this.add(new Label("Channels"), LEFT, AFTER);
        channelsLb = new ListBox(lbItems);
        this.add(channelsLb, SAME, AFTER);
        okB = new Button("   OK   ");
        this.add(okB, SAME, AFTER + 3);
        this.refreshProperties();
    }

    /**
	 * 
	 */
    private void refreshProperties() {
        channelsLb.removeAll();
        if (rssChannelHome.getChannelsNames().length > 0) channelsLb.add(rssChannelHome.getChannelsNames());
    }

    public void onEvent(Event event) {
        if ((event.target) == channelsLb) this.onChannelsLbEvent(event);
        if ((event.target) == addB) this.onAddBEvent(event);
        if ((event.target) == removeB) this.onRemoveBEvent(event);
        if ((event.target) == okB) this.onOkBEvent(event);
    }

    /**
	 * @param event
	 */
    private void onOkBEvent(Event event) {
        rssChannelHome.saveConfig();
        unpop();
    }

    /**
	 * @param event
	 */
    private void onRemoveBEvent(Event event) {
        if (event.type != ControlEvent.PRESSED) return;
        RssChannel rssChannel = getSelectedChannel();
        if (rssChannel != null) {
            rssChannelHome.removeChannel(rssChannel);
        }
        this.refreshProperties();
    }

    /**
	 * @param event
	 */
    private void onAddBEvent(Event event) {
        if (event.type != ControlEvent.PRESSED) return;
        RssChannel rss = rssChannelHome.addChannelFromLink(channelLink.getText());
        if (rss == null) return;
        this.refreshProperties();
    }

    /**
	 * @param event
	 */
    private void onChannelsLbEvent(Event event) {
        if (event.type != ControlEvent.PRESSED) return;
        RssChannel rssChannel = getSelectedChannel();
        if (rssChannel != null) {
            channelLink.setText(rssChannel.getRssLink());
            channelLink.repaint();
            return;
        }
        channelLink.setText("");
        channelLink.repaint();
    }

    /**
	 * @return
	 */
    private RssChannel getSelectedChannel() {
        String name = (String) channelsLb.getSelectedItem();
        RssChannel rssChannel = rssChannelHome.getRssChannelNamed(name);
        return rssChannel;
    }
}
