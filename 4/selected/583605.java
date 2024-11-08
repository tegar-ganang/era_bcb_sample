package webirc.client.gui.contactpanel;

import webirc.client.Channel;
import webirc.client.GUIController;
import webirc.client.MainSystem;
import webirc.client.gui.StatusIcon;
import webirc.client.gui.menu.ContextMenu;
import webirc.client.gui.menu.MenuController;
import webirc.client.gui.menu.MenuNotFoundException;
import webirc.client.gui.messagepanel.MessagePanel;

/**
 * @author Ayzen
 * @version 1.0 18.08.2006 0:29:24
 */
public class ChannelLine extends AbstractLine {

    private Channel channel;

    public ChannelLine(Channel channel) {
        super();
        setChannel(channel);
        icon.setType(StatusIcon.TYPE_CHANNEL);
    }

    public void lineClicked() {
        MessagePanel messagePanel = GUIController.getInstance().getMessagePanel();
        if (!messagePanel.isTabExist(channel)) messagePanel.addTab(channel);
        messagePanel.selectTab(channel);
    }

    public void optionsClicked() {
        try {
            int x = optionsBtn.getAbsoluteLeft();
            int y = optionsBtn.getAbsoluteTop();
            MenuController.getInstance().setParameter("channel", channel);
            MenuController.getInstance().getContextMenu("channel").show(x, y);
        } catch (MenuNotFoundException e) {
            MainSystem.showError(e.getMessage());
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
        content.setText(channel.getName());
    }
}
