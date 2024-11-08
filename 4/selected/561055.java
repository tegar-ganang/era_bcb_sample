package blue.ui.core.mixer;

import blue.mixer.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * @author steven
 */
public class ChannelListPanel extends JComponent implements ListDataListener, PropertyChangeListener {

    private ChannelList channels = null;

    private ChannelList subChannels = null;

    /** Creates a new instance of ChanelListPanel */
    public ChannelListPanel() {
        this.setLayout(new ChannelListLayout());
        this.addContainerListener(new ContainerListener() {

            public void componentAdded(ContainerEvent e) {
                Dimension preferredLayoutSize = getLayout().preferredLayoutSize(ChannelListPanel.this);
                setPreferredSize(preferredLayoutSize);
                setSize(preferredLayoutSize);
            }

            public void componentRemoved(ContainerEvent e) {
                Dimension preferredLayoutSize = getLayout().preferredLayoutSize(ChannelListPanel.this);
                setPreferredSize(preferredLayoutSize);
                setSize(preferredLayoutSize);
            }
        });
        this.setMinimumSize(new Dimension(0, 0));
    }

    public void setChannelList(ChannelList channels, ChannelList subChannels) {
        if (this.channels != null) {
            this.channels.removeListDataListener(this);
        }
        this.channels = channels;
        this.subChannels = subChannels;
        rebuildChannelsUI(channels);
        this.channels.addListDataListener(this);
    }

    private void clearChannels() {
        for (int i = 0; i < getComponentCount(); i++) {
            ChannelPanel cPanel = (ChannelPanel) getComponent(i);
            cPanel.clear();
        }
        this.removeAll();
    }

    private void rebuildChannelsUI(final ChannelList channels) {
        clearChannels();
        for (int i = 0; i < channels.size(); i++) {
            Channel channel = channels.getChannel(i);
            ChannelPanel cPanel = createChannelPanel(channel);
            this.add(cPanel);
        }
    }

    /**
     * @param channel
     * @return
     */
    private ChannelPanel createChannelPanel(Channel channel) {
        ChannelPanel cPanel = new ChannelPanel();
        ChannelOutComboBoxModel model = new ChannelOutComboBoxModel();
        model.setChannels(subChannels);
        model.setSelectedItem(channel.getOutChannel());
        cPanel.setChannelOutModel(model);
        cPanel.setChannel(channel);
        return cPanel;
    }

    public void intervalAdded(ListDataEvent e) {
        int index0 = e.getIndex0();
        int index1 = e.getIndex1();
        for (int i = index0; i <= index1; i++) {
            Channel channel = channels.getChannel(i);
            ChannelPanel cPanel = createChannelPanel(channel);
            this.add(cPanel, i);
        }
    }

    public void intervalRemoved(ListDataEvent e) {
        int index0 = e.getIndex0();
        int index1 = e.getIndex1();
        for (int i = index1; i >= index0; i--) {
            ChannelPanel cPanel = (ChannelPanel) getComponent(i);
            cPanel.clear();
            this.remove(index0);
        }
    }

    public void contentsChanged(ListDataEvent e) {
        rebuildChannelsUI(channels);
    }

    void sort() {
        ArrayList list = new ArrayList();
        for (int i = 0; i < this.getComponentCount(); i++) {
            list.add(getComponent(i));
        }
        this.removeAll();
        Collections.sort(list);
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            this.add((Component) it.next());
        }
    }

    public void propertyChange(PropertyChangeEvent pce) {
        if (pce.getPropertyName().equals(Channel.NAME)) {
            String oldName = (String) pce.getOldValue();
            String newName = (String) pce.getNewValue();
            for (int i = 0; i < getComponentCount(); i++) {
                ChannelPanel chanPanel = (ChannelPanel) getComponent(i);
                ChannelOutComboBoxModel model = (ChannelOutComboBoxModel) chanPanel.getChannelOutModel();
                model.reconcile(oldName, newName);
            }
        }
    }
}
