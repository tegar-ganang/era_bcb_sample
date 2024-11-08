package de.cinek.rssview;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import de.cinek.rssview.event.ChannelEvent;
import de.cinek.rssview.event.ChannelListener;
import de.cinek.rssview.ui.JOptionsTitle;

/**
 * @author saintedlama
 * @version $Id: ChannelContentPanel.java,v 1.14 2004/06/10 22:21:48 ms140569
 *          Exp $
 */
public class ChannelContentPanel extends JPanel {

    private JTextArea channelDescriptionField;

    private JButton queryButton;

    private JLabel channelNameLabel;

    private JTextField searchTextField;

    private JLabel articleCount;

    private JLabel newArticleCount;

    private JLabel articleCountLabel;

    private JLabel newArticleCountLabel;

    private Channel channel;

    private ChannelListener listenerHelper;

    private ResourceBundle rb;

    /**
	 * Creates a new instance of ChannelContentPanel
	 */
    public ChannelContentPanel() {
        super(new GridBagLayout());
        initComponents();
    }

    protected void initComponents() {
        this.listenerHelper = new ListenerHelper();
        super.setAlignmentX(0.0f);
        rb = ResourceBundle.getBundle("rssview");
        setOpaque(true);
        setBackground(Color.WHITE);
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(2, 10, 2, 2);
        gbcLeft.anchor = GridBagConstraints.EAST;
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(2, 2, 2, 2);
        gbcRight.anchor = GridBagConstraints.NORTHWEST;
        gbcRight.gridwidth = GridBagConstraints.REMAINDER;
        GridBagConstraints gbcHeaderLabel = new GridBagConstraints();
        gbcHeaderLabel.insets = new Insets(2, 2, 2, 2);
        gbcHeaderLabel.weightx = 1.0;
        gbcHeaderLabel.anchor = GridBagConstraints.EAST;
        gbcHeaderLabel.fill = GridBagConstraints.HORIZONTAL;
        gbcHeaderLabel.gridwidth = GridBagConstraints.REMAINDER;
        channelNameLabel = new JOptionsTitle(rb.getString("No_Channel_selected"));
        channelNameLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        channelNameLabel.setIconTextGap(100);
        Dimension dim = channelNameLabel.getPreferredSize();
        dim.height = 50;
        channelNameLabel.setPreferredSize(dim);
        add(channelNameLabel, gbcHeaderLabel);
        articleCountLabel = new JLabel(rb.getString("article_number"));
        add(articleCountLabel, gbcLeft);
        articleCount = new JLabel();
        add(articleCount, gbcRight);
        newArticleCountLabel = new JLabel(rb.getString("Unread"));
        add(newArticleCountLabel, gbcLeft);
        newArticleCount = new JLabel();
        add(newArticleCount, gbcRight);
        searchTextField = new JTextField(30);
        searchTextField.setMinimumSize(new Dimension(100, 25));
        gbcLeft.gridwidth = 2;
        add(searchTextField, gbcLeft);
        queryButton = new JButton(new QueryAction());
        add(queryButton, gbcRight);
        channelDescriptionField = new JTextArea(5, 60);
        channelDescriptionField.setMinimumSize(new Dimension(200, 100));
        channelDescriptionField.setLineWrap(true);
        channelDescriptionField.setWrapStyleWord(true);
        channelDescriptionField.setEditable(false);
        channelDescriptionField.setForeground(Color.GRAY);
        gbcRight.insets = new Insets(20, 10, 2, 2);
        add(channelDescriptionField, gbcRight);
        JLabel spacerLabel = new JLabel();
        gbcRight.weighty = 1.0;
        gbcRight.insets = new Insets(0, 0, 0, 0);
        add(spacerLabel, gbcRight);
    }

    /**
	 * Getter for property channelContent.
	 * @return Value of property channelContent.
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * Setter for property RssSubscription.
	 * @param channel New value of property channel.
	 */
    public void setChannel(Channel channel) {
        if (this.channel != null) {
            this.channel.removeChannelListener(listenerHelper);
        }
        if (channel != null) {
            updateDynamicFields(channel);
            this.channel = channel;
            this.channel.addChannelListener(listenerHelper);
        }
    }

    public void updateDynamicFields(Channel updatedChannel) {
        ChannelHeader header = updatedChannel.getHeader();
        if (header != null) {
            channelNameLabel.setText(header.getTitle());
            channelDescriptionField.setText(header.getDescription());
        }
        Query query = updatedChannel.getQuery();
        if (query != null) {
            this.queryButton.setEnabled(true);
            this.searchTextField.setEnabled(true);
        } else {
            this.queryButton.setEnabled(false);
            this.searchTextField.setEnabled(false);
        }
        articleCount.setText("" + updatedChannel.getArticleCount());
        newArticleCount.setText("" + updatedChannel.getUnread());
    }

    public void startSearch() {
        if (channel != null && channel.getQuery() != null) {
            Query query = channel.getQuery();
            try {
                String searchquery = query.getLink().trim() + "?" + query.getName().trim() + "=";
                new RssBrowserStart(searchquery + searchTextField.getText());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, rb.getString("error_resolving_url"), rb.getString("Query_error"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private class ListenerHelper implements ChannelListener {

        public void articleRemoved(ChannelEvent event) {
            updateDynamicFields((Channel) event.getSource());
        }

        public void articleStateChanged(ChannelEvent event) {
            updateDynamicFields((Channel) event.getSource());
        }

        public void articlesAdded(ChannelEvent event) {
            updateDynamicFields((Channel) event.getSource());
        }
    }

    private class QueryAction extends AbstractAction {

        public QueryAction() {
            super(rb.getString("Search"));
        }

        public void actionPerformed(ActionEvent e) {
            startSearch();
        }
    }
}
