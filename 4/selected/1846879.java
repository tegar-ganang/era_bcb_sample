package net.sourceforge.thinfeeder;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;
import net.sourceforge.thinfeeder.command.action.ApplyLanguageAction;
import net.sourceforge.thinfeeder.command.action.ApplySkinAction;
import net.sourceforge.thinfeeder.command.action.CheckNewVersionAction;
import net.sourceforge.thinfeeder.command.action.ClearHistoryAction;
import net.sourceforge.thinfeeder.command.action.DisableIconsAction;
import net.sourceforge.thinfeeder.command.action.EnableIconsAction;
import net.sourceforge.thinfeeder.command.action.InitDynamicInterfaceAction;
import net.sourceforge.thinfeeder.command.action.InitI18NAction;
import net.sourceforge.thinfeeder.command.action.InitInterfaceAction;
import net.sourceforge.thinfeeder.command.action.InitJVMParametersAction;
import net.sourceforge.thinfeeder.command.action.InitTrayAction;
import net.sourceforge.thinfeeder.command.action.LaunchInBrowserAction;
import net.sourceforge.thinfeeder.command.action.MarkAllAsReadAction;
import net.sourceforge.thinfeeder.command.action.MarkAllAsUnreadAction;
import net.sourceforge.thinfeeder.command.action.ShowChannelAction;
import net.sourceforge.thinfeeder.command.action.ShowItemAction;
import net.sourceforge.thinfeeder.command.action.ToggleReadUnreadItemAction;
import net.sourceforge.thinfeeder.command.action.UnsubscribeAction;
import net.sourceforge.thinfeeder.command.thread.AddFeedsThread;
import net.sourceforge.thinfeeder.command.thread.ExportOPMLThread;
import net.sourceforge.thinfeeder.command.thread.ImportOPMLThread;
import net.sourceforge.thinfeeder.command.thread.RefreshAllFeedsThread;
import net.sourceforge.thinfeeder.command.thread.RefreshCurrentFeedThread;
import net.sourceforge.thinfeeder.command.thread.ShutdownThread;
import net.sourceforge.thinfeeder.model.dao.DAOChannel;
import net.sourceforge.thinfeeder.model.dao.DAOI18N;
import net.sourceforge.thinfeeder.model.dao.DAOSkin;
import net.sourceforge.thinfeeder.model.dao.DAOSystem;
import net.sourceforge.thinfeeder.util.Utils;
import net.sourceforge.thinfeeder.vo.I18NIF;
import net.sourceforge.thinfeeder.vo.SkinIF;
import net.sourceforge.thinfeeder.vo.SystemIF;
import net.sourceforge.thinfeeder.widget.About;
import net.sourceforge.thinfeeder.widget.ChannelProperties;
import net.sourceforge.thinfeeder.widget.FindMoreSites;
import net.sourceforge.thinfeeder.widget.Help;
import net.sourceforge.thinfeeder.widget.NewFeed;
import net.sourceforge.thinfeeder.widget.Options;
import net.sourceforge.thinfeeder.widget.tray.TrayManager;
import thinlet.Thinlet;
import de.nava.informa.core.ChannelIF;

/**
 * @author fabianofranz@users.sourceforge.net
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class ThinFeeder extends Thinlet {

    private static final long serialVersionUID = 1L;

    private ResourceBundle bundle;

    private ThreadGroup threads = new ThreadGroup("user_threads");

    private boolean disposed = false;

    public ThinFeeder() throws Exception {
        new InitI18NAction(this).doAction();
        new InitJVMParametersAction(this).doAction();
        new InitInterfaceAction(this).doAction();
        new InitDynamicInterfaceAction(this).doAction();
        new InitTrayAction(this).doAction();
    }

    /**
	 * Anti-aliasing
	 */
    public void paint(Graphics g) {
        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        super.paint(g);
    }

    public void exit() {
        status(getI18N("i18n.closing_thinfeeder"));
        TrayManager.getInstance().hide();
        destroy();
    }

    public void closeDialog(Object dialog) {
        remove(dialog);
    }

    public void handleException(Throwable t) {
        super.handleException(t);
    }

    public void showChannel(Object thinList, Object thinItems) throws Exception {
        Object thinSelectedChannel = getSelectedItem(thinList);
        new ShowChannelAction(this, thinSelectedChannel, thinList, thinItems).doAction();
    }

    public void showItem(Object items) throws Exception {
        new ShowItemAction(this, items).doAction();
    }

    public void newFeed() throws Exception {
        status(getI18N("i18n.input_feed_url"));
        String feedURL = new NewFeed(this, getI18N("i18n.feed_url")).show();
        addFeed(feedURL);
    }

    public void addFeed(String url) {
        if (url != null) new AddFeedsThread(this, new String[] { url }).start();
    }

    public void importOPML() {
        new ImportOPMLThread(this).start();
    }

    public void exportOPML() {
        new ExportOPMLThread(this).start();
    }

    public void findMoreSites() throws Exception {
        new FindMoreSites(this).show();
    }

    public void help() throws Exception {
        new Help(this).show();
    }

    public void configure() throws Exception {
        new Options(this).show();
    }

    public void about() throws Exception {
        new About(this).show();
    }

    public void status(String text) {
        status(text, false);
    }

    public void status(String text, boolean error) {
        if (text == null) text = " ";
        Object status = find("status");
        if (error) setColor(status, "foreground", Color.RED); else setColor(status, "foreground", Color.BLACK);
        setString(status, "text", " " + text);
    }

    public void refreshAllFeeds() {
        new RefreshAllFeedsThread(this).start();
    }

    public void refreshCurrentFeed(Object thinList, Object thinItems) {
        new RefreshCurrentFeedThread(this, thinList, thinItems).start();
    }

    public void markAllAsRead() {
        try {
            new MarkAllAsReadAction(this).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void markAllAsUnread() {
        try {
            new MarkAllAsUnreadAction(this).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void clearHistory() {
        try {
            new ClearHistoryAction(this).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void unsubscribe() {
        try {
            new UnsubscribeAction(this).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void enableIcons() {
        try {
            new EnableIconsAction(this).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void disableIcons() {
        try {
            new DisableIconsAction(this).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void applySkin(Object skinsMenuNode) {
        long id = ((Long) getProperty(skinsMenuNode, "skin_id")).longValue();
        try {
            SkinIF skin = DAOSkin.getSkin(id);
            new ApplySkinAction(this, skin).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void applyLanguage(Object languagesMenuNode) {
        long id = ((Long) getProperty(languagesMenuNode, "language_id")).longValue();
        try {
            I18NIF i18n = (I18NIF) DAOI18N.getI18N(id);
            new ApplyLanguageAction(this, i18n).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public String getI18N(String key) {
        return getI18N(key, null);
    }

    public String getI18N(String key, String[] replace) {
        if (key == null) return null; else if (!key.startsWith("i18n.")) return key; else if (replace == null) return bundle.getString(key.substring(5)); else {
            String message = bundle.getString(key.substring(5));
            if (message != null) message = MessageFormat.format(message, (Object[]) replace);
            return message;
        }
    }

    public void launchInBrowser() {
        showLink(null);
    }

    public void showLink(String url) {
        try {
            new LaunchInBrowserAction(this, url).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void showChannelProperties() {
        try {
            new ChannelProperties(this).show();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void showLink(Object channelsList) {
        try {
            Object node = getSelectedItem(channelsList);
            if (node != null) {
                long id = ((Long) getProperty(node, "id")).longValue();
                ChannelIF channel = DAOChannel.getChannel(id);
                showLink(channel.getSite() == null ? "" : channel.getSite().toExternalForm());
            }
        } catch (Exception e) {
            handleException(e);
        }
    }

    public void toggleReadUnreadItem(Object itemList) {
        try {
            Object itemNode = getSelectedItem(itemList);
            if (itemNode != null) new ToggleReadUnreadItemAction(this, itemNode).doAction();
        } catch (Exception e) {
            handleException(e);
        }
    }

    public String getDateTime(Date date) {
        return Utils.dateToString(date, getI18N("i18n.date_time_format"));
    }

    public int hasNewVersion() {
        try {
            CheckNewVersionAction action = new CheckNewVersionAction(this);
            action.doAction();
            return action.hasNewVersion() ? 1 : 0;
        } catch (Throwable t) {
            return -1;
        }
    }

    public ThreadGroup getUsersThreadsGroup() {
        return threads;
    }

    public void stopAllUserThreads() {
        threads.interrupt();
    }

    public ResourceBundle getResourceBundle() {
        return bundle;
    }

    public void setResourceBundle(ResourceBundle bundle) {
        this.bundle = bundle;
        super.setResourceBundle(bundle);
    }

    public void channelsFocusGained() {
        Object refreshButton = find("refresh_button");
        Object refreshMenuItem = find("refresh_menuitem");
        Object launchInBrowserMenuItem = find("launch_in_browser_menuitem");
        Object clearHistoryMenuItem = find("clear_history_menuitem");
        Object unsubscribeMenuItem = find("unsubscribe_menuitem");
        Object channelPropertiesMenuItem = find("channel_properties_menuitem");
        Object markReadMenuItem = find("mark_read_menuitem");
        Object markUnreadMenuItem = find("mark_unread_menuitem");
        setBoolean(refreshButton, "enabled", true);
        setBoolean(refreshMenuItem, "enabled", true);
        setBoolean(launchInBrowserMenuItem, "enabled", true);
        setBoolean(clearHistoryMenuItem, "enabled", true);
        setBoolean(unsubscribeMenuItem, "enabled", true);
        setBoolean(channelPropertiesMenuItem, "enabled", true);
        setBoolean(markReadMenuItem, "enabled", true);
        setBoolean(markUnreadMenuItem, "enabled", true);
    }

    public boolean destroy() {
        try {
            SystemIF system = DAOSystem.getSystem();
            int dividerX = getInteger(find("divider_x"), "divider");
            int dividerY = getInteger(find("divider_y"), "divider");
            system.setBounds(getParent().getBounds());
            system.setDividerXPosition(dividerX);
            system.setDividerYPosition(dividerY);
            DAOSystem.updateSystem(system);
            ((Frame) getParent()).dispose();
            new ShutdownThread(this).start();
        } catch (Exception e) {
            handleException(e);
        }
        return true;
    }

    public void restore() {
        getParent().setVisible(true);
        requestFocus();
        disposed = false;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void setDisposed(boolean disposed) {
        this.disposed = disposed;
    }
}
