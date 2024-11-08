package blue.ui.core.mixer;

import blue.Arrangement;
import blue.ArrangementEvent;
import blue.ArrangementListener;
import blue.BlueData;
import blue.mixer.Channel;
import blue.mixer.ChannelList;
import blue.mixer.Mixer;
import blue.projects.BlueProject;
import blue.projects.BlueProjectManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
final class MixerTopComponent extends TopComponent implements ArrangementListener {

    private static MixerTopComponent instance;

    private static final String PREFERRED_ID = "MixerTopComponent";

    private Mixer mixer;

    private Arrangement arrangement;

    private MixerTopComponent() {
        initComponents();
        ((JScrollPane) jSplitPane1.getLeftComponent()).setBorder(null);
        ((JScrollPane) jSplitPane1.getRightComponent()).setBorder(null);
        setName(NbBundle.getMessage(MixerTopComponent.class, "CTL_MixerTopComponent"));
        setToolTipText(NbBundle.getMessage(MixerTopComponent.class, "HINT_MixerTopComponent"));
        BlueProjectManager.getInstance().addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (BlueProjectManager.CURRENT_PROJECT.equals(evt.getPropertyName())) {
                    reinitialize();
                }
            }
        });
        reinitialize();
    }

    protected void reinitialize() {
        BlueProject project = BlueProjectManager.getInstance().getCurrentProject();
        BlueData data = null;
        if (project != null) {
            data = project.getData();
            setMixer(data.getMixer());
            setArrangement(data.getArrangement());
        }
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
        channelsPanel.sort();
    }

    private void initComponents() {
        jScrollPane3 = new javax.swing.JScrollPane();
        jPanel1 = new javax.swing.JPanel();
        enabled = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        extraRenderText = new javax.swing.JTextField();
        masterPanel = new blue.ui.core.mixer.ChannelPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        channelsPanel = new blue.ui.core.mixer.ChannelListPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        subChannelsPanel = new blue.ui.core.mixer.SubChannelListPanel();
        jScrollPane3.setBorder(null);
        org.openide.awt.Mnemonics.setLocalizedText(enabled, org.openide.util.NbBundle.getMessage(MixerTopComponent.class, "MixerTopComponent.enabled.text"));
        enabled.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enabledActionPerformed(evt);
            }
        });
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MixerTopComponent.class, "MixerTopComponent.jLabel1.text"));
        extraRenderText.setText(org.openide.util.NbBundle.getMessage(MixerTopComponent.class, "MixerTopComponent.extraRenderText.text"));
        extraRenderText.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extraRenderTextActionPerformed(evt);
            }
        });
        extraRenderText.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                extraRenderTextFocusLost(evt);
            }
        });
        masterPanel.setBorder(null);
        masterPanel.setMaster(true);
        jSplitPane1.setDividerLocation(400);
        jScrollPane1.setViewportView(channelsPanel);
        jSplitPane1.setLeftComponent(jScrollPane1);
        jScrollPane2.setViewportView(subChannelsPanel);
        jSplitPane1.setRightComponent(jScrollPane2);
        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(enabled).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 539, Short.MAX_VALUE).add(jLabel1)).add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 701, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(extraRenderText).add(masterPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(extraRenderText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jLabel1).add(enabled)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jPanel1Layout.createSequentialGroup().add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE).add(0, 0, 0)).add(jPanel1Layout.createSequentialGroup().add(masterPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 392, Short.MAX_VALUE).addContainerGap()))));
        jScrollPane3.setViewportView(jPanel1);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 838, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE));
    }

    private void enabledActionPerformed(java.awt.event.ActionEvent evt) {
        if (mixer != null) {
            mixer.setEnabled(enabled.isSelected());
            extraRenderText.setEnabled(enabled.isSelected());
        }
    }

    private void extraRenderTextActionPerformed(java.awt.event.ActionEvent evt) {
        updateExtraRenderValue();
    }

    private void extraRenderTextFocusLost(java.awt.event.FocusEvent evt) {
        updateExtraRenderValue();
    }

    private blue.ui.core.mixer.ChannelListPanel channelsPanel;

    private javax.swing.JCheckBox enabled;

    private javax.swing.JTextField extraRenderText;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    private javax.swing.JSplitPane jSplitPane1;

    private blue.ui.core.mixer.ChannelPanel masterPanel;

    private blue.ui.core.mixer.SubChannelListPanel subChannelsPanel;

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link #findInstance}.
     */
    public static synchronized MixerTopComponent getDefault() {
        if (instance == null) {
            instance = new MixerTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the MixerTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized MixerTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Logger.getLogger(MixerTopComponent.class.getName()).warning("Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof MixerTopComponent) {
            return (MixerTopComponent) win;
        }
        Logger.getLogger(MixerTopComponent.class.getName()).warning("There seem to be multiple components with the '" + PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior.");
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper(jSplitPane1.getDividerLocation());
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    static final class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int dividerLocation;

        private ResolvableHelper(int dividerLocation) {
            this.dividerLocation = dividerLocation;
        }

        public Object readResolve() {
            MixerTopComponent mtc = MixerTopComponent.getDefault();
            mtc.jSplitPane1.setDividerLocation(dividerLocation);
            return mtc;
        }
    }
}
