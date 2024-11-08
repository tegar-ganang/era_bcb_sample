package blue.mixer;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import blue.Arrangement;
import blue.ArrangementEvent;
import blue.ArrangementListener;
import blue.BlueSystem;
import blue.WindowSettingManager;
import blue.WindowSettingsSavable;
import blue.gui.DialogUtil;
import blue.utility.GUI;
import electric.xml.Element;

/**
 * 
 * @author Steven Yi
 */
public class MixerDialog extends JDialog implements ArrangementListener, WindowSettingsSavable {

    JCheckBox enabled;

    private ChannelListPanel channelsPanel = new ChannelListPanel();

    private SubChannelListPanel subChannelsPanel = new SubChannelListPanel();

    private ChannelPanel masterPanel;

    private JTextField extraRenderText = new JTextField();

    private JSplitPane splitPane;

    private Mixer mixer;

    private Arrangement arrangement;

    /** Creates new form MixerDialog */
    public MixerDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        GUI.centerOnScreen(this);
        WindowSettingManager.getInstance().registerWindow("MixerDialog", this);
        DialogUtil.registerJDialog(this);
    }

    private void initComponents() {
        this.setTitle(BlueSystem.getString("menu.window.mixer.text"));
        JPanel topPanel = new JPanel();
        BoxLayout box = new BoxLayout(topPanel, BoxLayout.X_AXIS);
        topPanel.setLayout(box);
        enabled = new JCheckBox("Enabled");
        enabled.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (mixer != null) {
                    mixer.setEnabled(enabled.isSelected());
                    extraRenderText.setEnabled(enabled.isSelected());
                }
            }
        });
        extraRenderText.setPreferredSize(new Dimension(80, 20));
        extraRenderText.setMaximumSize(new Dimension(80, 20));
        extraRenderText.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateExtraRenderValue();
            }
        });
        extraRenderText.addFocusListener(new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                updateExtraRenderValue();
            }
        });
        topPanel.add(enabled);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(new JLabel("Extra Render Time:"));
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(extraRenderText);
        splitPane = new JSplitPane();
        subChannelsPanel.addPropertyChangeListener(channelsPanel);
        splitPane.add(new JScrollPane(channelsPanel), JSplitPane.LEFT);
        splitPane.add(new JScrollPane(subChannelsPanel), JSplitPane.RIGHT);
        splitPane.setDividerLocation(400);
        ((JScrollPane) splitPane.getLeftComponent()).setBorder(null);
        ((JScrollPane) splitPane.getRightComponent()).setBorder(null);
        masterPanel = new ChannelPanel();
        masterPanel.setMaster(true);
        masterPanel.setBorder(null);
        Container c = this.getContentPane();
        c.setLayout(new BorderLayout());
        ((JComponent) c).setBorder(new EmptyBorder(10, 10, 10, 10));
        c.add(topPanel, BorderLayout.NORTH);
        c.add(masterPanel, BorderLayout.EAST);
        c.add(splitPane, BorderLayout.CENTER);
        setSize(700, 500);
    }

    protected void updateExtraRenderValue() {
        String val = extraRenderText.getText();
        try {
            float value = Float.parseFloat(val);
            if (value < 0.0f) {
                value = 0.0f;
            }
            mixer.setExtraRenderTime(value);
        } catch (NumberFormatException nfe) {
            extraRenderText.setText(Float.toString(mixer.getExtraRenderTime()));
        }
    }

    public void setMixer(Mixer mixer) {
        this.mixer = null;
        enabled.setSelected(mixer.isEnabled());
        extraRenderText.setEnabled(mixer.isEnabled());
        extraRenderText.setText(Float.toString(mixer.getExtraRenderTime()));
        channelsPanel.setChannelList(mixer.getChannels(), mixer.getSubChannels());
        subChannelsPanel.setChannelList(mixer.getSubChannels());
        masterPanel.clear();
        masterPanel.setChannel(mixer.getMaster());
        this.mixer = mixer;
        EffectEditorManager.getInstance().clear();
        SendEditorManager.getInstance().clear();
    }

    public void setArrangement(Arrangement arrangement) {
        if (this.arrangement != null) {
            arrangement.removeArrangementListener(this);
            this.arrangement = null;
        }
        this.arrangement = arrangement;
        reconcileWithArrangement();
        arrangement.addArrangementListener(this);
    }

    public void arrangementChanged(ArrangementEvent arrEvt) {
        switch(arrEvt.getType()) {
            case ArrangementEvent.UPDATE:
                reconcileWithArrangement();
                break;
            case ArrangementEvent.INSTRUMENT_ID_CHANGED:
                switchMixerId(arrEvt.getOldId(), arrEvt.getNewId());
                break;
        }
    }

    /**
     * Because blue allows multiple instruments to have the same arrangmentId,
     * must handle cases of if channels exist for oldId and newId, as well as
     * creating or destroying channels
     */
    private void switchMixerId(String oldId, String newId) {
        ChannelList channels = mixer.getChannels();
        int oldIdCount = 0;
        int newIdCount = 0;
        for (int i = 0; i < arrangement.size(); i++) {
            String instrId = arrangement.getInstrumentAssignment(i).arrangementId;
            if (instrId.equals(oldId)) {
                oldIdCount++;
            } else if (instrId.equals(newId)) {
                newIdCount++;
            }
        }
        if (oldIdCount == 0 && newIdCount == 1) {
            for (int i = 0; i < channels.size(); i++) {
                Channel channel = channels.getChannel(i);
                if (channel.getName().equals(oldId)) {
                    channel.setName(newId);
                    break;
                }
            }
        } else if (oldIdCount == 0 && newIdCount > 1) {
            for (int i = 0; i < channels.size(); i++) {
                Channel channel = channels.getChannel(i);
                if (channel.getName().equals(oldId)) {
                    channels.removeChannel(channel);
                    break;
                }
            }
        } else if (oldIdCount > 0 && newIdCount == 1) {
            Channel channel = new Channel();
            channel.setName(newId);
            channels.addChannel(channel);
        }
    }

    private void reconcileWithArrangement() {
        ChannelList channels = mixer.getChannels();
        ArrayList idList = new ArrayList();
        for (int i = 0; i < arrangement.size(); i++) {
            String instrId = arrangement.getInstrumentAssignment(i).arrangementId;
            if (!idList.contains(instrId)) {
                idList.add(instrId);
            }
        }
        for (int i = channels.size() - 1; i >= 0; i--) {
            Channel channel = channels.getChannel(i);
            if (!idList.contains(channel.getName())) {
                channels.removeChannel(channel);
            }
        }
        for (int i = 0; i < idList.size(); i++) {
            channels.checkOrCreate((String) idList.get(i));
        }
        channels.sort();
        channelsPanel.sort();
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(String args[]) {
        GUI.setBlueLookAndFeel();
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                MixerDialog m = new MixerDialog(null, true);
                Mixer mixer = new Mixer();
                for (int i = 0; i < 3; i++) {
                    mixer.getChannels().addChannel(new Channel());
                }
                m.setMixer(mixer);
                m.setVisible(true);
                m.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            }
        });
    }

    public void loadWindowSettings(Element settings) {
        WindowSettingManager.setBasicSettings(settings, this);
        Element elem = settings.getElement("splitVal");
        if (elem != null) {
            splitPane.setDividerLocation(Integer.parseInt(elem.getTextString()));
        }
    }

    public Element saveWindowSettings() {
        Element retVal = WindowSettingManager.getBasicSettings(this);
        retVal.addElement("splitVal").setText(Integer.toString(splitPane.getDividerLocation()));
        return retVal;
    }
}
