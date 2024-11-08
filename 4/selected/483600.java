package ui;

import javax.swing.JTextArea;
import de.nava.informa.core.ChannelSubscriptionIF;

/**
 * @author jp
 *
 * Display summary of the subscription
 */
public class SubscriptionSummaryViewer implements SubscriptionSelectionListenerIF {

    private JTextArea jtextArea;

    public SubscriptionSummaryViewer(JTextArea jtextArea) {
        this.jtextArea = jtextArea;
    }

    public void subscriptionSelected(ChannelSubscriptionIF channelSubscription) {
        String summary = "";
        summary += "Title: " + channelSubscription.toString() + '\n' + "Active: " + (channelSubscription.isActive() ? "yes" : "no") + '\n' + "Refresh Time: " + String.valueOf(channelSubscription.getUpdateInterval()) + " seconds" + '\n' + "Location: " + channelSubscription.getChannel().getLocation();
        jtextArea.setText(summary);
    }
}
