package ui;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ChannelSubscriptionIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.exporters.RSS_1_0_Exporter;
import de.nava.informa.impl.basic.Channel;
import java.awt.Rectangle;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import javax.swing.JTextArea;

/**
 * @author jp 
 * 
 * Displays content of a channel in a subscription
 */
public class SubscriptionContentViewer implements SubscriptionSelectionListenerIF, de.nava.informa.core.ChannelObserverIF {

    ChannelIF channelIF;

    private JTextArea jtextArea;

    /**
     * Constructor for SubscriptionContentViewer.
     */
    public SubscriptionContentViewer(JTextArea jtextArea) {
        this.jtextArea = jtextArea;
    }

    public void subscriptionSelected(ChannelSubscriptionIF channelSubscription) {
        if (channelSubscription == null) {
            this.jtextArea.setText("");
            return;
        }
        printChannel(channelSubscription.getChannel());
        if (channelIF instanceof Channel) {
            Channel c = (Channel) channelIF;
            c.removeObserver(this);
            c.addObserver(this);
        }
    }

    protected void printChannel(ChannelIF newChannelIF) {
        this.channelIF = newChannelIF;
        try {
            String channelString = "";
            Iterator i = newChannelIF.getItems().iterator();
            channelString += ("[ " + newChannelIF.getTitle() + " ]" + '\n');
            channelString += '\n';
            while (i.hasNext()) {
                ItemIF item = (ItemIF) i.next();
                channelString += printItem(item);
                channelString += '\n';
            }
            jtextArea.setText(channelString);
        } catch (Exception e) {
            jtextArea.setText("Error displaying channel: " + e.getMessage());
        }
    }

    protected String printItem(ItemIF item) {
        String strItem = "";
        strItem += (item.getTitle().toUpperCase() + '\n');
        strItem += printField("Date", item.getDate());
        strItem += printField("By", item.getCreator());
        strItem += printField("Subject", item.getSubject());
        strItem += printField("Link", item.getLink().toExternalForm());
        strItem += printField("Description", item.getDescription());
        return strItem;
    }

    protected String printField(String title, Date date) {
        if (date == null) {
            return "";
        }
        return title + ": " + DateFormat.getDateInstance().format(date) + '\n';
    }

    protected String printField(String title, String field) {
        if (field == null) {
            return "";
        }
        return title + ": " + field + '\n';
    }

    public void itemAdded(ItemIF newItem) {
        this.jtextArea.append(printItem(newItem));
    }

    public void channelRetrieved(ChannelIF channel) {
    }
}
