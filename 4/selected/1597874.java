package coopnetclient.frames.renderers;

import coopnetclient.Globals;
import coopnetclient.frames.models.VoiceChatChannelListModel;
import coopnetclient.frames.models.VoiceChatChannelListModel.Channel;
import coopnetclient.utils.Settings;
import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JList;

/**
 * Renders the elements in the contact list
 */
public class VoiceChatRenderer extends DefaultListCellRenderer {

    public static ImageIcon emptyIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(Globals.getResourceAsString("data/icons/quicktab/voicechat/empty.png")).getScaledInstance(20, 20, Image.SCALE_SMOOTH));

    public static ImageIcon talkingIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(Globals.getResourceAsString("data/icons/quicktab/voicechat/talking.png")).getScaledInstance(20, 20, Image.SCALE_SMOOTH));

    public static ImageIcon mutedIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(Globals.getResourceAsString("data/icons/quicktab/voicechat/muted.png")).getScaledInstance(20, 20, Image.SCALE_SMOOTH));

    private VoiceChatChannelListModel model;

    public VoiceChatRenderer(VoiceChatChannelListModel model) {
        setOpaque(true);
        this.model = model;
        putClientProperty("html.disable", Boolean.TRUE);
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected && !(value.toString().equals(Globals.getThisPlayer_loginName())), cellHasFocus);
        setFont(new Font(Settings.getNameStyle(), Font.PLAIN, 14));
        setToolTipText("<html><xmp>" + value.toString());
        setText(value.toString());
        if (Settings.getColorizeBody()) {
            setForeground(Settings.getForegroundColor());
            if (isSelected && !(value.toString().equals(Globals.getThisPlayer_loginName()))) {
                setBackground(Settings.getSelectionColor());
            } else {
                setBackground(Settings.getBackgroundColor());
            }
        }
        setHorizontalAlignment(LEFT);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        Channel c = model.getChannel(value.toString());
        if (c != null) {
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            setFont(new Font(Settings.getNameStyle(), Font.BOLD, 14));
        } else {
            if (model.isMuted(value.toString())) {
                setIcon(mutedIcon);
            } else {
                if (model.isTalking(value.toString())) {
                    setIcon(talkingIcon);
                } else {
                    setIcon(emptyIcon);
                }
            }
        }
        return this;
    }
}
