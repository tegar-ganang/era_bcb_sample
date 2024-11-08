package informaclient;

import javax.swing.JLabel;
import javax.swing.JPanel;
import de.nava.informa.core.ChannelIF;

/**
 * <p>Title: InformaClient</p>
 * <p>Description: RSS Client based on Informa library</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author Pito Salas
 * @version 1.0
 */
public class ChannelStamp extends JPanel {

    JLabel channelTitleLabel;

    private javax.swing.JTextArea channelDescriptionBox;

    private javax.swing.JLabel channelPublisherLabel;

    private javax.swing.JLabel channelFormatLabel;

    private javax.swing.JLabel channelLastUpdatedLabel;

    private javax.swing.JLabel channelItemCountLabel;

    public ChannelStamp() {
    }

    public void displayChannel(ChannelIF theChan) {
        channelTitleLabel.setText(theChan.getTitle());
        channelDescriptionBox.setText(theChan.getDescription());
        channelPublisherLabel.setText(theChan.getPublisher());
        channelFormatLabel.setText(theChan.getFormat().toString());
        channelLastUpdatedLabel.setText(theChan.getLastUpdated().toString());
        channelItemCountLabel.setText(new Integer(theChan.getItems().size()).toString());
    }

    public JLabel getChannelTitleLabel() {
        return channelTitleLabel;
    }

    public void setChannelTitleLabel(JLabel channelTitleLabel) {
        this.channelTitleLabel = channelTitleLabel;
    }

    public void setChannelDescriptionBox(javax.swing.JTextArea channelDescriptionLabel) {
        this.channelDescriptionBox = channelDescriptionLabel;
    }

    public javax.swing.JTextArea getChannelDescriptionBox() {
        return channelDescriptionBox;
    }

    public void setChannelPublisherLabel(javax.swing.JLabel channelPublisherLabel) {
        this.channelPublisherLabel = channelPublisherLabel;
    }

    public javax.swing.JLabel getChannelPublisherLabel() {
        return channelPublisherLabel;
    }

    public void setChannelFormatLabel(javax.swing.JLabel channelFormatLabel) {
        this.channelFormatLabel = channelFormatLabel;
    }

    public javax.swing.JLabel getChannelFormatLabel() {
        return channelFormatLabel;
    }

    public void setChannelLastUpdatedLabel(javax.swing.JLabel channelLastUpdatedLabel) {
        this.channelLastUpdatedLabel = channelLastUpdatedLabel;
    }

    public javax.swing.JLabel getChannelLastUpdatedLabel() {
        return channelLastUpdatedLabel;
    }

    public void setChannelItemCountLabel(javax.swing.JLabel channelItemCountLabel) {
        this.channelItemCountLabel = channelItemCountLabel;
    }

    public javax.swing.JLabel getChannelItemCountLabel() {
        return channelItemCountLabel;
    }
}
