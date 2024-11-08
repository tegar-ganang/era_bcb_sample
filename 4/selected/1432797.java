package blue.mixer;

import java.util.Iterator;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * 
 * @author steven
 */
public class ChannelOutComboBoxModel implements ComboBoxModel, ListDataListener {

    ChannelList channels = null;

    String selectedItem = null;

    Vector listeners = null;

    private Vector copies = null;

    /** Creates a new instance of ChannelOutComboBox */
    public ChannelOutComboBoxModel() {
    }

    public void setChannels(ChannelList channels) {
        if (this.channels != null) {
            this.channels.removeListDataListener(this);
        }
        this.channels = channels;
        this.channels.addListDataListener(this);
    }

    public void clearListeners() {
        this.channels.removeListDataListener(this);
        this.channels = null;
    }

    public void setSelectedItem(Object anItem) {
        if (Channel.MASTER.equals(anItem)) {
            selectedItem = Channel.MASTER;
        } else {
            int index = channels.indexByName(anItem);
            if (index < 0) {
                selectedItem = Channel.MASTER;
            } else {
                selectedItem = (String) anItem;
            }
        }
        ListDataEvent lde = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        fireListEvent(lde);
    }

    public Object getSelectedItem() {
        return selectedItem;
    }

    public int getSize() {
        if (channels == null) {
            return 1;
        }
        return channels.size() + 1;
    }

    public Object getElementAt(int index) {
        if (index == 0) {
            return Channel.MASTER;
        }
        if (channels == null) {
            return null;
        }
        return channels.getChannel(index - 1).getName();
    }

    public void addListDataListener(ListDataListener l) {
        if (listeners == null) {
            listeners = new Vector();
        }
        listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l) {
        if (listeners == null) {
            return;
        }
        listeners.remove(l);
    }

    private void fireListEvent(ListDataEvent lde) {
        if (listeners == null) {
            return;
        }
        for (Iterator it = listeners.iterator(); it.hasNext(); ) {
            ListDataListener listener = (ListDataListener) it.next();
            switch(lde.getType()) {
                case ListDataEvent.INTERVAL_ADDED:
                    listener.intervalAdded(lde);
                    break;
                case ListDataEvent.INTERVAL_REMOVED:
                    listener.intervalRemoved(lde);
                    break;
                case ListDataEvent.CONTENTS_CHANGED:
                    listener.contentsChanged(lde);
                    break;
            }
        }
    }

    public void intervalAdded(ListDataEvent e) {
        ListDataEvent lde = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        fireListEvent(lde);
    }

    public void intervalRemoved(ListDataEvent e) {
        if (channels.indexByName(selectedItem) < 0) {
            setSelectedItem(Channel.MASTER);
        }
    }

    public void contentsChanged(ListDataEvent e) {
        System.out.println("channelOutComboBoxModel::contentsChanged()");
    }

    public void reconcile(String oldName, String newName) {
        if (this.getSelectedItem().toString().equals(oldName)) {
            setSelectedItem(newName);
        }
        if (copies != null) {
            for (int i = 0; i < copies.size(); i++) {
                ((ChannelOutComboBoxModel) copies.get(i)).reconcile(oldName, newName);
            }
        }
    }

    public ChannelOutComboBoxModel getCopy() {
        ChannelOutComboBoxModel copy = new ChannelOutComboBoxModel();
        copy.setChannels(this.channels);
        if (copies == null) {
            copies = new Vector();
        }
        copies.add(copy);
        return copy;
    }
}
