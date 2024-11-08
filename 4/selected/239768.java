package net.sourceforge.thinfeeder.widget;

import net.sourceforge.thinfeeder.ThinFeeder;
import net.sourceforge.thinfeeder.model.dao.DAOChannel;
import thinlet.Thinlet;
import de.nava.informa.core.ChannelIF;

/**
 * @author fabianofranz@users.sourceforge.net
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ChannelProperties extends Widget {

    public ChannelProperties(Thinlet thinlet) {
        this.thinlet = thinlet;
    }

    private Object dialog = null;

    public void show() throws Exception {
        Object channels = thinlet.find("channels");
        Object selectedChannel = thinlet.getSelectedItem(channels);
        if (selectedChannel == null) {
            ((ThinFeeder) thinlet).status(((ThinFeeder) thinlet).getI18N("i18n.error_15"), true);
            return;
        }
        dialog = thinlet.parse("/net/sourceforge/thinfeeder/widget/channelproperties.xml", this);
        thinlet.add(dialog);
        long id = ((Long) thinlet.getProperty(selectedChannel, "id")).longValue();
        ChannelIF channel = DAOChannel.getChannel(id);
        thinlet.setString(thinlet.find(dialog, "channel_title"), "text", channel.getTitle());
        thinlet.setString(thinlet.find(dialog, "channel_rss"), "text", channel.getLocation() == null ? "" : channel.getLocation().toExternalForm());
    }

    public void close() {
        closeDialog(dialog);
    }

    public void ok() throws Exception {
        Object channels = thinlet.find("channels");
        Object selectedChannel = thinlet.getSelectedItem(channels);
        Object channelTitle = thinlet.find("channel_title");
        String channelTitleStr = thinlet.getString(channelTitle, "text");
        long id = ((Long) thinlet.getProperty(selectedChannel, "id")).longValue();
        ChannelIF channel = DAOChannel.getChannel(id);
        channel.setTitle(channelTitleStr);
        DAOChannel.updateChannel(channel);
        thinlet.setString(selectedChannel, "text", channelTitleStr);
        closeDialog(dialog);
    }

    public void cancel() {
        closeDialog(dialog);
    }
}
