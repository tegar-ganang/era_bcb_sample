package blue.ui.core.mixer;

import blue.mixer.*;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.Locale;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import blue.BlueSystem;
import java.awt.Frame;
import javax.swing.JFrame;
import org.openide.windows.WindowManager;

/**
 * 
 * @author Steven Yi
 * @author Michael Bechard
 */
public class ChannelPanel extends javax.swing.JPanel implements PropertyChangeListener, Comparable {

    boolean subChannel = false;

    boolean updating = false;

    /** Creates new form ChannelPanel */
    public ChannelPanel() {
        initComponents();
        levelLabel.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2) {
                    switchLevelValueView(true);
                    levelValueField.requestFocus();
                }
            }
        });
        levelValueField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                setLevelValueFromField();
                switchLevelValueView(false);
            }
        });
        levelValueField.addFocusListener(new FocusAdapter() {

            public void focusLost(FocusEvent e) {
                switchLevelValueView(false);
            }
        });
        Dimension miniScrollDim = new Dimension(9, 55);
        preScroll.getVerticalScrollBar().setPreferredSize(miniScrollDim);
        postScroll.getVerticalScrollBar().setPreferredSize(miniScrollDim);
        miniScrollDim = new Dimension(1, 9);
        preScroll.getHorizontalScrollBar().setPreferredSize(miniScrollDim);
        postScroll.getHorizontalScrollBar().setPreferredSize(miniScrollDim);
        levelSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (!updating) {
                    updateLevelValue();
                }
            }
        });
        outputList.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (channel != null) {
                    channel.setOutChannel((String) outputList.getSelectedItem());
                }
            }
        });
        preList.setCellRenderer(new EnabledListCellRenderer());
        postList.setCellRenderer(new EnabledListCellRenderer());
    }

    private void setLevelValueFromField() {
        try {
            float val = Float.parseFloat(levelValueField.getText());
            val = Math.max(val, -96.0f);
            val = Math.min(val, 12.0f);
            channel.setLevel(val);
        } catch (NumberFormatException ex) {
        }
    }

    private void switchLevelValueView(boolean toTextField) {
        String compName;
        if (toTextField) {
            compName = "levelField";
            MessageFormat fmt = new MessageFormat("{0,number,##.####}", Locale.ENGLISH);
            levelValueField.setText(fmt.format(new Object[] { new Float(channel.getLevel()) }));
        } else {
            compName = "levelLabel";
            levelLabel.setText(channel.getLevel() + " dB");
        }
        CardLayout cardLayout = (CardLayout) levelValuePanel.getLayout();
        cardLayout.show(levelValuePanel, compName);
    }

    public Channel getChannel() {
        return this.channel;
    }

    public synchronized void setChannel(Channel channel) {
        if (this.channel != null) {
            this.channel.removePropertyChangeListener(this);
        }
        this.channel = null;
        preList.setModel(channel.getPreEffects());
        postList.setModel(channel.getPostEffects());
        channelNameLabel.setText(channel.getName());
        outputList.setSelectedItem(channel.getOutChannel());
        int levelVal = getSliderValFromChannel(channel);
        levelSlider.setValue(levelVal);
        levelLabel.setText(channel.getLevel() + " dB");
        this.channel = channel;
        this.channel.addPropertyChangeListener(this);
    }

    private int getSliderValFromChannel(Channel channel) {
        int levelVal = 0;
        if (channel != null) {
            if (channel.getLevel() > 0) {
                levelVal = (int) (channel.getLevel() * 20);
            } else {
                levelVal = (int) (channel.getLevel() * 10);
            }
        }
        return levelVal;
    }

    public void setSubChannel(boolean val) {
        subChannel = val;
    }

    public void setChannelOutModel(ComboBoxModel model) {
        this.outputList.setModel(model);
    }

    public ComboBoxModel getChannelOutModel() {
        return this.outputList.getModel();
    }

    public synchronized void clear() {
        if (this.channel != null) {
            this.channel.removePropertyChangeListener(this);
        }
        DefaultListModel fakeModel = new DefaultListModel();
        fakeModel.addElement("clear");
        preList.setModel(fakeModel);
        postList.setModel(fakeModel);
        ComboBoxModel model = this.outputList.getModel();
        if (model instanceof ChannelOutComboBoxModel) {
            ((ChannelOutComboBoxModel) model).clearListeners();
        }
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
        outputLabel.setVisible(!master);
        outputList.setVisible(!master);
    }

    private void updateLevelValue() {
        if (this.channel == null) {
            return;
        }
        float db = levelSlider.getValue();
        db = db > 0 ? db / 20 : db / 10;
        channel.setLevel(db);
        levelLabel.setText(db + " dB");
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        levelSlider = new javax.swing.JSlider();
        channelNameLabel = new javax.swing.JLabel();
        preLabel = new javax.swing.JLabel();
        postLabel = new javax.swing.JLabel();
        outputLabel = new javax.swing.JLabel();
        outputList = new javax.swing.JComboBox();
        postScroll = new javax.swing.JScrollPane();
        postList = new javax.swing.JList();
        preScroll = new javax.swing.JScrollPane();
        preList = new javax.swing.JList();
        levelValuePanel = new javax.swing.JPanel();
        levelLabel = new javax.swing.JLabel();
        levelValueField = new javax.swing.JTextField();
        setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                formMousePressed(evt);
            }
        });
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Level");
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                jLabel1MousePressed(evt);
            }
        });
        levelSlider.setMaximum(240);
        levelSlider.setMinimum(-960);
        levelSlider.setOrientation(javax.swing.JSlider.VERTICAL);
        levelSlider.setValue(0);
        channelNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        channelNameLabel.setText("Channel Name");
        channelNameLabel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        channelNameLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                channelNameLabelMouseClicked(evt);
            }
        });
        preLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        preLabel.setText("Pre");
        preLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                preLabelMousePressed(evt);
            }
        });
        postLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        postLabel.setText("Post");
        postLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                postLabelMousePressed(evt);
            }
        });
        outputLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        outputLabel.setText("Output");
        outputLabel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent evt) {
                outputLabelMousePressed(evt);
            }
        });
        outputList.setFont(new java.awt.Font("Dialog", 0, 10));
        outputList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        outputList.setFocusable(false);
        postList.setFont(new java.awt.Font("Dialog", 0, 10));
        postList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        postList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        postList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                postListMouseClicked(evt);
            }
        });
        postScroll.setViewportView(postList);
        preList.setFont(new java.awt.Font("Dialog", 0, 10));
        preList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        preList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        preList.addFocusListener(new java.awt.event.FocusAdapter() {

            public void focusLost(java.awt.event.FocusEvent evt) {
                preListFocusLost(evt);
            }
        });
        preList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                preListMouseClicked(evt);
            }
        });
        preScroll.setViewportView(preList);
        levelValuePanel.setMaximumSize(new java.awt.Dimension(24, 20));
        levelValuePanel.setMinimumSize(new java.awt.Dimension(24, 20));
        levelValuePanel.setPreferredSize(new java.awt.Dimension(24, 20));
        levelValuePanel.setLayout(new java.awt.CardLayout());
        levelLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        levelLabel.setText("0db");
        levelLabel.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        levelLabel.setName("levelLabel");
        levelValuePanel.add(levelLabel, "levelLabel");
        levelValueField.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.green));
        levelValueField.setMargin(new java.awt.Insets(0, 2, 0, 2));
        levelValueField.setName("levelField");
        levelValuePanel.add(levelValueField, "levelField");
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, levelSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, preLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, postLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, outputLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, postScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, outputList, 0, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, jLabel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, preScroll, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, channelNameLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE).add(levelValuePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup().addContainerGap().add(channelNameLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(preLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(preScroll, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(levelSlider, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 161, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(levelValuePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 16, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(postLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(postScroll, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(outputLabel).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(outputList, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addContainerGap()));
    }

    private void postLabelMousePressed(java.awt.event.MouseEvent evt) {
        postLabel.requestFocus();
    }

    private void jLabel1MousePressed(java.awt.event.MouseEvent evt) {
        jLabel1.requestFocus();
    }

    private void preLabelMousePressed(java.awt.event.MouseEvent evt) {
        preLabel.requestFocus();
    }

    private void outputLabelMousePressed(java.awt.event.MouseEvent evt) {
        outputLabel.requestFocus();
    }

    private void formMousePressed(java.awt.event.MouseEvent evt) {
        requestFocus();
    }

    private void preListFocusLost(java.awt.event.FocusEvent evt) {
        preList.setSelectedIndex(-1);
    }

    private void postListMouseClicked(java.awt.event.MouseEvent evt) {
        if (SwingUtilities.isRightMouseButton(evt)) {
            EffectsPopup popup = EffectsPopup.getInstance();
            popup.setEffectsChain(this.channel.getPostEffects(), postList.getSelectedIndex());
            popup.setComboBoxModel(outputList.getModel());
            popup.setMaster(isMaster());
            popup.show(postList, evt.getX(), evt.getY());
        } else if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
            if (postList.getSelectedValue() == null) {
                return;
            }
            Object obj = postList.getSelectedValue();
            Frame root = WindowManager.getDefault().getMainWindow();
            if (obj instanceof Effect) {
                Effect effect = (Effect) obj;
                EffectEditorManager.getInstance().openEffectEditor(root, effect);
            } else if (obj instanceof Send) {
                Send send = (Send) obj;
                ComboBoxModel model = outputList.getModel();
                ComboBoxModel temp = null;
                if (model instanceof ChannelOutComboBoxModel) {
                    temp = ((ChannelOutComboBoxModel) model).getCopy();
                } else if (model instanceof SubChannelOutComboBoxModel) {
                    temp = ((SubChannelOutComboBoxModel) model).getCopy();
                }
                SendEditorManager.getInstance().openSendEditor(root, send, temp);
            }
        }
    }

    private void preListMouseClicked(java.awt.event.MouseEvent evt) {
        if (SwingUtilities.isRightMouseButton(evt)) {
            EffectsPopup popup = EffectsPopup.getInstance();
            popup.setEffectsChain(this.channel.getPreEffects(), preList.getSelectedIndex());
            popup.setComboBoxModel(outputList.getModel());
            popup.setMaster(isMaster());
            popup.show(preList, evt.getX(), evt.getY());
        } else if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
            if (preList.getSelectedValue() == null) {
                return;
            }
            Object obj = preList.getSelectedValue();
            Frame root = WindowManager.getDefault().getMainWindow();
            if (obj instanceof Effect) {
                Effect effect = (Effect) obj;
                EffectEditorManager.getInstance().openEffectEditor(root, effect);
            } else if (obj instanceof Send) {
                Send send = (Send) obj;
                ComboBoxModel model = outputList.getModel();
                ComboBoxModel temp = null;
                if (model instanceof ChannelOutComboBoxModel) {
                    temp = ((ChannelOutComboBoxModel) model).getCopy();
                } else if (model instanceof SubChannelOutComboBoxModel) {
                    temp = ((SubChannelOutComboBoxModel) model).getCopy();
                }
                SendEditorManager.getInstance().openSendEditor(root, send, temp);
            } else {
                System.err.println("ERR: " + obj);
            }
        }
    }

    private void channelNameLabelMouseClicked(java.awt.event.MouseEvent evt) {
        if (subChannel && evt.getClickCount() == 2) {
            editChannelName();
        }
    }

    /**
     * 
     */
    private void editChannelName() {
        boolean finished = false;
        String originalName = channel.getName();
        SubChannelOutComboBoxModel model = (SubChannelOutComboBoxModel) getChannelOutModel();
        ChannelList subChannels = model.getChannels();
        while (!finished) {
            String retVal = JOptionPane.showInputDialog(this, "Please Enter SubChannel Name", originalName);
            if (retVal != null && retVal.trim().length() > 0 && !retVal.equals(originalName)) {
                retVal = retVal.trim();
                if (!isValidChannelName(retVal)) {
                    JOptionPane.showMessageDialog(this, "Error: Channel names may only contain letters, " + "numbers, or underscores", BlueSystem.getString("common.error"), JOptionPane.ERROR_MESSAGE);
                    finished = false;
                } else if (retVal.equals(Channel.MASTER) || subChannels.isChannelNameInUse(retVal)) {
                    JOptionPane.showMessageDialog(this, "Error: Channel Name already in use", BlueSystem.getString("common.error"), JOptionPane.ERROR_MESSAGE);
                    finished = false;
                } else {
                    channel.setName(retVal);
                    finished = true;
                }
            } else {
                finished = true;
            }
        }
    }

    private boolean isValidChannelName(String retVal) {
        for (int i = 0; i < retVal.length(); i++) {
            char c = retVal.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() != this.channel) {
            return;
        }
        String prop = evt.getPropertyName();
        if (prop.equals(Channel.NAME)) {
            channelNameLabel.setText(channel.getName());
        } else if (prop.equals(Channel.LEVEL)) {
            updating = true;
            int levelVal = 0;
            if (channel.getLevel() > 0) {
                levelVal = (int) (channel.getLevel() * 20);
            } else {
                levelVal = (int) (channel.getLevel() * 10);
            }
            levelSlider.setValue(levelVal);
            levelLabel.setText(channel.getLevel() + " dB");
            updating = false;
        }
    }

    public int compareTo(Object o) {
        ChannelPanel chanB = (ChannelPanel) o;
        try {
            int a = Integer.parseInt(this.channel.getName());
            int b = Integer.parseInt(chanB.getChannel().getName());
            return a - b;
        } catch (NumberFormatException nfe) {
            return (this.channel.getName()).compareToIgnoreCase(chanB.getChannel().getName());
        }
    }

    private javax.swing.JLabel channelNameLabel;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel levelLabel;

    private javax.swing.JSlider levelSlider;

    private javax.swing.JTextField levelValueField;

    private javax.swing.JPanel levelValuePanel;

    private javax.swing.JLabel outputLabel;

    private javax.swing.JComboBox outputList;

    private javax.swing.JLabel postLabel;

    private javax.swing.JList postList;

    private javax.swing.JScrollPane postScroll;

    private javax.swing.JLabel preLabel;

    private javax.swing.JList preList;

    private javax.swing.JScrollPane preScroll;

    private Channel channel;

    private boolean master;

    private static class EnabledListCellRenderer extends DefaultListCellRenderer {

        private final Font ENABLED_FONT = new java.awt.Font("Dialog", 0, 10);

        private final Font DISABLED_FONT = new java.awt.Font("Dialog", Font.ITALIC, 10);

        private static final Color ENABLED_COLOR = Color.WHITE;

        private static final Color DISABLED_COLOR = Color.GRAY;

        public EnabledListCellRenderer() {
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (list.getModel() != null && index >= 0) {
                if (list.getModel() instanceof EffectsChain) {
                    EffectsChain chain = (EffectsChain) list.getModel();
                    Object obj = chain.getElementAt(index);
                    if (obj instanceof Effect) {
                        Effect effect = (Effect) obj;
                        if (effect.isEnabled()) {
                            c.setForeground(ENABLED_COLOR);
                        } else {
                            c.setForeground(DISABLED_COLOR);
                        }
                    } else {
                        Send send = (Send) obj;
                        if (send.isEnabled()) {
                            c.setForeground(ENABLED_COLOR);
                        } else {
                            c.setForeground(DISABLED_COLOR);
                        }
                    }
                }
            }
            return c;
        }
    }
}
