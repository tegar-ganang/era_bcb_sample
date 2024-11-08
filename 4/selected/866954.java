package blue.orchestra.editor.blueSynthBuilder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import blue.mixer.Channel;
import blue.mixer.ChannelListListener;
import blue.mixer.Mixer;
import blue.orchestra.blueSynthBuilder.BSBSubChannelDropdown;
import blue.projects.BlueProjectManager;

/**
 * @author Steven Yi
 */
public class BSBSubChannelDropdownView extends BSBObjectView implements PropertyChangeListener {

    BSBSubChannelDropdown dropdown = null;

    SubChannelComboBoxModel model;

    JComboBox comboBox;

    ActionListener updateIndexListener;

    boolean updating = false;

    public BSBSubChannelDropdownView(BSBSubChannelDropdown dropdown2) {
        this.dropdown = dropdown2;
        this.setBSBObject(dropdown2);
        this.setLayout(null);
        model = new SubChannelComboBoxModel();
        comboBox = new JComboBox(model);
        comboBox.setSelectedItem(dropdown.getChannelOutput());
        this.add(comboBox);
        comboBox.setSize(comboBox.getPreferredSize());
        this.setSize(comboBox.getPreferredSize());
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (!updating) {
                    dropdown.setChannelOutput((String) comboBox.getSelectedItem());
                }
            }
        });
        revalidate();
        dropdown.addPropertyChangeListener(this);
    }

    public void cleanup() {
        model.clearListeners();
        dropdown.removePropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == this.dropdown) {
            if (evt.getPropertyName().equals("channelOutput")) {
                if (evt.getOldValue().equals(comboBox.getSelectedItem())) {
                    updating = true;
                    comboBox.setSelectedItem(evt.getNewValue());
                    updating = false;
                }
            }
        }
    }
}

class SubChannelComboBoxModel implements ComboBoxModel, ChannelListListener {

    Vector listeners = new Vector();

    Mixer mixer;

    Object selected = null;

    public SubChannelComboBoxModel() {
        this.mixer = BlueProjectManager.getInstance().getCurrentBlueData().getMixer();
    }

    public void clearListeners() {
    }

    public Object getSelectedItem() {
        return selected;
    }

    public void setSelectedItem(Object anItem) {
        this.selected = anItem;
        ListDataEvent lde = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, -1, -1);
        fireListDataEvent(lde);
    }

    public int getSize() {
        return mixer.getSubChannels().size() + 1;
    }

    public Object getElementAt(int index) {
        if (index == 0) {
            return Channel.MASTER;
        }
        return mixer.getSubChannel(index - 1).getName();
    }

    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    public void channelAdded(Channel channel) {
    }

    public void channelRemoved(Channel channel) {
        if (!Channel.MASTER.equals(selected)) {
            if (mixer.getSubChannels().indexOfChannel((String) selected) < 0) {
                setSelectedItem(Channel.MASTER);
            }
        }
    }

    private void fireListDataEvent(ListDataEvent lde) {
        for (Iterator iter = new Vector(listeners).iterator(); iter.hasNext(); ) {
            ListDataListener ldl = (ListDataListener) iter.next();
            ldl.contentsChanged(lde);
        }
    }
}
