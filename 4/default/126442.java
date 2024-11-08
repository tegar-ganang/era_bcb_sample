import rsswaba.rss.*;
import rsswaba.persistence.RssChannelHomePersistence;
import superwaba.ext.xplat.html.HtmlContainer;
import superwaba.ext.xplat.html.Style;
import waba.fx.Color;
import waba.fx.Rect;
import waba.ui.*;

public class RssWaba extends MainWindow {

    RssChannelHomePersistence rssChannelHome;

    RssChannel rssChannel;

    RssItem rssItem;

    ComboBox rssCb;

    RssItemsHtmlContainer headLinesLb;

    HtmlContainer descriptionMe;

    Button updateButton;

    MenuBar mbar;

    public RssWaba() {
        super("RssWaba", MainWindow.TAB_ONLY_BORDER);
        this.setDoubleBuffer(true);
        String[][] menus = { { "File", "Rss Channels", "Proxy", "Exit" } };
        setMenuBar(mbar = new MenuBar(menus));
        setBackColor(new Color(220, 200, 160));
    }

    public void onStart() {
        rssChannelHome = new RssChannelHomePersistence();
        String cbItems[] = { "                                           " };
        this.add(new Label("Rss's"), SAME, AFTER);
        rssCb = new ComboBox(cbItems);
        this.add(rssCb, SAME, AFTER);
        updateButton = new Button("Refresh");
        this.add(updateButton, AFTER + 1, SAME);
        this.add(new Label("News"), LEFT, AFTER + 2);
        descriptionMe = new HtmlContainer();
        headLinesLb = new RssItemsHtmlContainer(descriptionMe, rssChannelHome);
        this.add(headLinesLb, SAME, AFTER);
        Rect base = headLinesLb.getRect();
        base.width = 240;
        base.height = rssCb.getRect().height * 3;
        headLinesLb.setRect(base);
        Style.defaultFontFace = "Tahoma";
        Style.defaultFontSize = 8;
        this.add(new Label("Description"), SAME, AFTER + 2);
        this.add(descriptionMe, SAME, AFTER);
        base = descriptionMe.getRect();
        base.width = headLinesLb.getRect().width;
        base.height = headLinesLb.getRect().height * 2;
        descriptionMe.setRect(base);
        this.refreshChannelHome();
    }

    public void refreshChannelHome() {
        rssCb.removeAll();
        String[] names = rssChannelHome.getChannelsNames();
        if (names.length > 0) rssCb.add(rssChannelHome.getChannelsNames());
    }

    public void refreshChannel() {
        String name = (String) rssCb.getSelectedItem();
        if (name == null) {
            if (rssChannel == null) return;
            headLinesLb.setRssChannel(null);
        } else {
            if ((rssChannel == null) || (rssChannel.getName() != name)) {
                rssChannel = rssChannelHome.getRssChannelNamed(name);
                headLinesLb.setRssChannel(rssChannel);
            }
        }
    }

    public void updateChannel() {
        if (rssChannel == null) return;
        try {
            rssChannelHome.updateChannel(rssChannel);
        } catch (Exception e) {
            return;
        }
        rssChannel = null;
        this.refreshChannel();
    }

    public void onMenuEvent(Event event) {
        if (event.type == ControlEvent.WINDOW_CLOSED) {
            int sel = mbar.getSelectedMenuItem();
            switch(sel) {
                case 1:
                    {
                        RssChannelsWindow rss = new RssChannelsWindow(rssChannelHome);
                        popupBlockingModal(rss);
                        refreshChannelHome();
                        break;
                    }
                case 2:
                    {
                        RssProxyWindow proxy = new RssProxyWindow(rssChannelHome);
                        popupBlockingModal(proxy);
                        break;
                    }
                case 3:
                    {
                        exit(0);
                        break;
                    }
            }
        }
    }

    public void onEvent(Event event) {
        if ((event.target) == rssCb) this.onRssCbEvent(event);
        if ((event.target) == updateButton) this.onUpdateButtonEvent(event);
        if ((event.target) == mbar) this.onMenuEvent(event);
    }

    private void onRssCbEvent(Event event) {
        if (event.type == ControlEvent.PRESSED) {
            refreshChannel();
        }
    }

    private void onUpdateButtonEvent(Event event) {
        if (event.type == ControlEvent.PRESSED) {
            updateChannel();
        }
    }
}
