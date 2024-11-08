package net.sourceforge.thinfeeder.mobile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import net.sourceforge.thinfeeder.mobile.rss.RSSListener;
import net.sourceforge.thinfeeder.mobile.rss.RSSParser;
import net.sourceforge.thinfeeder.mobile.rss.dao.ChannelDAO;
import net.sourceforge.thinfeeder.mobile.rss.dao.ItemDAO;
import net.sourceforge.thinfeeder.mobile.rss.vo.ChannelVO;
import net.sourceforge.thinfeeder.mobile.rss.vo.ItemVO;
import thinlet.Thinlet;

/**
 *
 * @author  ffr
 * @version
 */
public class ThinFeederMobile extends MIDlet implements CommandListener, RSSListener {

    public ThinFeederMobile() {
        initialize();
    }

    private Thinlet main;

    private Thinlet channel;

    private Thinlet item;

    private Thinlet about;

    private Thinlet add;

    private org.netbeans.microedition.lcdui.SplashScreen splash;

    private Image splashImage;

    private Image aboutImage;

    private Command channelCommand;

    private Command itemCommand;

    private Command refreshCommand;

    private Command aboutCommand;

    private Command quitCommand;

    private Command backCommand;

    private Command addCommand;

    private ChannelDAO channelDAO;

    private ItemDAO itemDAO;

    private RSSParser rssParser;

    private int parsingChannel;

    /** 
     * Called by the system to indicate that a command has been invoked on a particular displayable.                      
     * @param command the Command that ws invoked
     * @param displayable the Displayable on which the command was invoked
     */
    public void commandAction(Command command, Displayable displayable) {
        if (displayable == main) {
            if (command == quitCommand) {
                exitMIDlet();
            } else if (command == channelCommand) {
                getDisplay().setCurrent(getChannelScreen());
            } else if (command == aboutCommand) {
                getDisplay().setCurrent(getAboutScreen());
            } else if (command == addCommand) {
                getDisplay().setCurrent(getAddScreen());
            }
        } else if (displayable == item) {
            if (command == backCommand) {
                getDisplay().setCurrent(getChannelScreen());
            }
        } else if (displayable == channel) {
            if (command == refreshCommand) {
                ChannelVO channel = getChannelDAO().load(getSelectedChannel());
                getRSSParser().parse(channel.getUrl());
            } else if (command == itemCommand) {
                getDisplay().setCurrent(getItemScreen());
            } else if (command == backCommand) {
                getDisplay().setCurrent(getMainScreen());
            }
        } else if (displayable == about) {
            if (command == backCommand) {
                getDisplay().setCurrent(getMainScreen());
            }
        } else if (displayable == add) {
            if (command == backCommand) {
                getDisplay().setCurrent(getMainScreen());
            } else if (command == addCommand) {
                String url = add.getString(add.find("url"), "text", "").toLowerCase();
                ChannelVO channel = insertChannel(url);
                updateMainScreen();
                parseChannel(channel);
                getDisplay().setCurrent(getMainScreen());
            }
        }
    }

    private void initialize() {
        getDisplay().setCurrent(getSplash());
    }

    /**
     * This method should return an instance of the display.
     */
    public Display getDisplay() {
        return Display.getDisplay(this);
    }

    /**
     * This method should exit the midlet.
     */
    public void exitMIDlet() {
        getDisplay().setCurrent(null);
        destroyApp(true);
        notifyDestroyed();
    }

    /** This method returns instance for splash component and should be called instead of accessing splash field directly.                        
     * @return Instance for splash component
     */
    public org.netbeans.microedition.lcdui.SplashScreen getSplash() {
        if (splash == null) {
            splash = new org.netbeans.microedition.lcdui.SplashScreen(getDisplay());
            splash.setTitle("Starting ThinFeeder Mobile...");
            splash.setText("");
            splash.setImage(getSplashImage());
            splash.setNextDisplayable(getMainScreen());
        }
        return splash;
    }

    /** This method returns instance for splashImage component and should be called instead of accessing splashImage field directly.                        
     * @return Instance for splashImage component
     */
    public Image getSplashImage() {
        if (splashImage == null) {
            try {
                splashImage = Image.createImage("/splash.png");
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        return splashImage;
    }

    /** This method returns instance for quit component and should be called instead of accessing quit field directly.                         
     * @return Instance for quit component
     */
    public Command getQuitCommand() {
        if (quitCommand == null) {
            quitCommand = new Command("Exit", Command.EXIT, 1);
        }
        return quitCommand;
    }

    /** This method returns instance for news component and should be called instead of accessing news field directly.                         
     * @return Instance for news component
     */
    public Command getNewsCommand() {
        if (channelCommand == null) {
            channelCommand = new Command("Notícias", Command.ITEM, 1);
        }
        return channelCommand;
    }

    /** This method returns instance for itemCommand2 component and should be called instead of accessing itemCommand2 field directly.                         
     * @return Instance for itemCommand2 component
     */
    public Command getItemCommand() {
        if (itemCommand == null) {
            itemCommand = new Command("Detalhes", Command.ITEM, 1);
        }
        return itemCommand;
    }

    /** This method returns instance for backCommand1 component and should be called instead of accessing backCommand1 field directly.                         
     * @return Instance for backCommand1 component
     */
    public Command getBackCommand() {
        if (backCommand == null) {
            backCommand = new Command("Back", Command.BACK, 1);
        }
        return backCommand;
    }

    /** This method returns instance for about1 component and should be called instead of accessing about1 field directly.                         
     * @return Instance for about1 component
     */
    public Command getAboutCommand() {
        if (aboutCommand == null) {
            aboutCommand = new Command("Sobre", Command.ITEM, 1);
        }
        return aboutCommand;
    }

    public Command getRefreshCommand() {
        if (refreshCommand == null) {
            refreshCommand = new Command("Refresh", Command.ITEM, 1);
        }
        return refreshCommand;
    }

    public Command getAddCommand() {
        if (addCommand == null) {
            addCommand = new Command("Add Channel", Command.ITEM, 1);
        }
        return addCommand;
    }

    public Displayable getMainScreen() {
        if (main == null) {
            main = new Thinlet(this);
            try {
                main.add(main.parse(Constants.XML_MAIN));
            } catch (IOException e) {
                e.printStackTrace();
            }
            main.addCommand(getQuitCommand());
            main.addCommand(getNewsCommand());
            main.addCommand(getAddCommand());
            main.addCommand(getAboutCommand());
            main.setCommandListener(this);
        }
        updateMainScreen();
        return main;
    }

    public Displayable getChannelScreen() {
        if (channel == null) {
            channel = new Thinlet(this);
            try {
                channel.add(main.parse(Constants.XML_CHANNEL));
            } catch (IOException e) {
                e.printStackTrace();
            }
            channel.addCommand(getRefreshCommand());
            channel.addCommand(getItemCommand());
            channel.addCommand(getBackCommand());
            channel.setCommandListener(this);
        }
        updateChannelScreen();
        return channel;
    }

    public Displayable getItemScreen() {
        if (item == null) {
            item = new Thinlet(this);
            try {
                item.add(item.parse(Constants.XML_ITEM));
            } catch (IOException e) {
                e.printStackTrace();
            }
            item.addCommand(getBackCommand());
            item.setCommandListener(this);
        }
        updateItemScreen();
        return item;
    }

    public Displayable getAddScreen() {
        if (add == null) {
            add = new Thinlet(this);
            try {
                add.add(add.parse(Constants.XML_ADD));
            } catch (IOException e) {
                e.printStackTrace();
            }
            add.addCommand(getBackCommand());
            add.addCommand(getAddCommand());
            add.setCommandListener(this);
        }
        return add;
    }

    /** 
     * This method returns instance for about component and should be called instead of accessing about field directly.                         
     * @return Instance for about component
     */
    public Displayable getAboutScreen() {
        if (about == null) {
            about = new Thinlet(this);
            try {
                about.add(about.parse(Constants.XML_ABOUT));
            } catch (IOException e) {
                e.printStackTrace();
            }
            about.addCommand(getBackCommand());
            about.setCommandListener(this);
        }
        return about;
    }

    private void updateMainScreen() {
        Vector all = getChannelDAO().findAll();
        Object channels = main.find("channels");
        main.removeAll(channels);
        boolean select = true;
        for (int i = 0; i < all.size(); i++) {
            ChannelVO channel = (ChannelVO) all.elementAt(i);
            long id = channel.getId();
            Object channelsNode = main.create("item");
            main.setString(channelsNode, "text", channel.getName());
            if (select) {
                main.setBoolean(channelsNode, "selected", true);
                select = false;
            }
            main.putProperty(channelsNode, "id", new Integer(channel.getId()));
            main.add(channels, channelsNode);
        }
    }

    private void updateChannelScreen() {
        int channelId = getSelectedChannel();
        Vector all = getItemDAO().findByChannel(channelId);
        channel.removeAll(channel.find("items"));
        boolean select = true;
        for (int i = 0; i < all.size(); i++) {
            ItemVO item = (ItemVO) all.elementAt(i);
            addItemToChannelScreen(item, select);
            if (select) select = false;
        }
    }

    private void updateItemScreen() {
        int itemId = getSelectedItem();
        ItemVO i = getItemDAO().load(itemId);
        Object itemTitle = item.find("itemTitle");
        Object itemDescription = item.find("itemDescription");
        item.setString(itemTitle, "text", i.getTitle());
        item.setString(itemDescription, "text", i.getDescription());
    }

    private void addItemToChannelScreen(ItemVO item, boolean select) {
        if (channel != null) {
            long id = item.getId();
            Object itemsNode = channel.create("item");
            channel.setString(itemsNode, "text", item.getTitle() == null ? "" : item.getTitle());
            channel.setBoolean(itemsNode, "selected", select);
            channel.putProperty(itemsNode, "id", new Integer(item.getId()));
            channel.add(channel.find("items"), itemsNode);
        }
    }

    public void startApp() {
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public void itemParsed(String title, String link, String description) {
        ItemVO item = new ItemVO(parsingChannel, title, link, description);
        getItemDAO().insert(item);
    }

    public void exception(IOException e) {
        e.printStackTrace();
    }

    public ChannelDAO getChannelDAO() {
        if (channelDAO == null) channelDAO = new ChannelDAO();
        return channelDAO;
    }

    public ItemDAO getItemDAO() {
        if (itemDAO == null) itemDAO = new ItemDAO();
        return itemDAO;
    }

    public RSSParser getRSSParser() {
        if (rssParser == null) {
            rssParser = new RSSParser();
            rssParser.setRSSListener(this);
        }
        return rssParser;
    }

    private int getSelectedChannel() {
        Object selectedNode = main.getSelectedItem(main.find("channels"));
        int id = ((Integer) main.getProperty(selectedNode, "id")).intValue();
        return id;
    }

    public ChannelVO insertChannel(String url) {
        return getChannelDAO().insert(new ChannelVO(url, url));
    }

    public void parseChannel(ChannelVO channel) {
        parsingChannel = channel.getId();
        getRSSParser().parse(channel.getUrl());
    }

    private int getSelectedItem() {
        Object selectedItem = channel.getSelectedItem(channel.find("items"));
        int id = ((Integer) channel.getProperty(selectedItem, "id")).intValue();
        return id;
    }
}
