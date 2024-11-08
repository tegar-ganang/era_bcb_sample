package de.genodeftest.k8055_old.gui.analogIn;

import java.awt.AWTEvent;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.commons.collections.primitives.ArrayIntList;
import org.apache.commons.collections.primitives.ArrayShortList;
import de.genodeftest.k8055_old.IOChannels;
import de.genodeftest.k8055_old.IOEvent;
import de.genodeftest.k8055_old.IOListener;
import de.genodeftest.k8055_old.K8055Channel;
import de.genodeftest.k8055_old.IOEvent.AnalogAllEvent;
import de.genodeftest.k8055_old.gui.CompleteView;
import de.genodeftest.k8055_old.gui.JK8055GUI;
import de.genodeftest.swing.JCopyableLabel;
import de.genodeftest.swing.jKnob.JKnob;

/**
 * @author Chris
 */
public class CompleteView_A_In extends CompleteView {

    private static final long serialVersionUID = -6300518306185658840L;

    private static final int initialArraySize = 1000000;

    ArrayShortList c1_data, c2_data;

    ArrayIntList time_data;

    Color c1_color = Color.BLUE;

    Color c2_color = Color.RED;

    boolean c1_enabled = true;

    boolean c2_enabled = false;

    int c1_VShift, c2_VShift;

    private boolean isPaused = true;

    private boolean isPerspectiveEnabled = false;

    private long time_start;

    private long lastRepaint = 0L;

    /**
	 * number of milliseconds from 0 on, where the actual view is shifted
	 */
    int start = 0;

    /**
	 * zoom : width of one displayed millisecond in pixels<br>
	 * zoom := t/px ; t: time in ms; px: number of pixels;
	 */
    double zoom = 3.4698;

    private Graph2 graph2;

    private JScrollBar jScrollBar_oszi = new JScrollBar(Adjustable.HORIZONTAL);

    private final JScrollPane jScrollPane_Table;

    private JTable werteTabelle;

    private final JButton jButton_pause_unpause, jButton_start_reset;

    private JKnob jKnob_vShift1 = new JKnob(-255, 255, 0, 2), jKnob_vShift2 = new JKnob(-255, 255, 0, 2);

    final JTextArea currentMousePosition;

    final JCheckBox jCheckBox_EnableTable;

    private final OsziTableModel osziTableModel;

    private final IOListener ioListener;

    private int oldMaximumStartValue = 0;

    /**
	 * Oscilloscope -> SkalaY1, Graph + JScrollbar, SkalaY2<br>
	 * -------<br>
	 * Control -><br>
	 * -------<br>
	 * Table
	 */
    public CompleteView_A_In() {
        super();
        this.setLayout(new BorderLayout());
        {
            Box box_oszilloscope = new Box(SwingConstants.VERTICAL);
            graph2 = new Graph2(this);
            box_oszilloscope.add(graph2);
            {
                jScrollBar_oszi.addAdjustmentListener(new java.awt.event.AdjustmentListener() {

                    @Override
                    public void adjustmentValueChanged(java.awt.event.AdjustmentEvent e) {
                        start = new Integer(jScrollBar_oszi.getValue());
                        fireDataChanged();
                    }
                });
                box_oszilloscope.add(jScrollBar_oszi);
            }
            this.add(box_oszilloscope, BorderLayout.CENTER);
            box_oszilloscope.setAlignmentX(0.5f);
        }
        {
            JPanel panel_control = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.ipady = 5;
            gbc.insets = new Insets(1, 20, 1, 20);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.NONE;
            panel_control.add(new JCopyableLabel("Einstellungen"), gbc);
            {
                jButton_start_reset = new JButton("Start");
                jButton_start_reset.addActionListener(new java.awt.event.ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (time_data == null) {
                            command_start();
                            jCheckBox_EnableTable.setSelected(false);
                            jCheckBox_EnableTable.setEnabled(false);
                        } else {
                            command_reset();
                            jCheckBox_EnableTable.setEnabled(true);
                        }
                    }
                });
                gbc.gridy = 1;
                gbc.insets = new Insets(1, 8, 1, 8);
                gbc.fill = GridBagConstraints.HORIZONTAL;
                panel_control.add(jButton_start_reset, gbc);
            }
            {
                jButton_pause_unpause = new JButton("Pause");
                jButton_pause_unpause.addActionListener(new java.awt.event.ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (isPaused) {
                            jButton_pause_unpause.setText("Pause");
                            isPaused = false;
                            jCheckBox_EnableTable.setSelected(false);
                        } else {
                            jButton_pause_unpause.setText("Fortsetzen");
                            isPaused = true;
                        }
                        jCheckBox_EnableTable.setEnabled(isPaused);
                    }
                });
                gbc.gridy = 2;
                jButton_pause_unpause.setEnabled(false);
                isPaused = true;
                panel_control.add(jButton_pause_unpause, gbc);
            }
            {
                JButton jButton_save = new JButton("Speichern");
                jButton_save.addActionListener(new java.awt.event.ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        command_save();
                    }
                });
                gbc.gridy = 3;
                panel_control.add(jButton_save, gbc);
            }
            {
                jCheckBox_EnableTable = new JCheckBox("Tabelle anzeigen", false);
                jCheckBox_EnableTable.addItemListener(new java.awt.event.ItemListener() {

                    public void itemStateChanged(java.awt.event.ItemEvent e) {
                        if (jCheckBox_EnableTable.isSelected()) {
                            CompleteView_A_In.this.add(jScrollPane_Table, BorderLayout.SOUTH);
                            osziTableModel.fireColumnInserted();
                        } else {
                            CompleteView_A_In.this.remove(jScrollPane_Table);
                        }
                        CompleteView_A_In.this.repaint();
                        SwingUtilities.updateComponentTreeUI(CompleteView_A_In.this);
                    }
                });
                gbc.insets = new Insets(0, 8, 0, 8);
                gbc.gridy = 4;
                panel_control.add(jCheckBox_EnableTable, gbc);
            }
            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.NONE;
            panel_control.add(new JCopyableLabel("Kanal 1 : V shift"));
            {
                jKnob_vShift1.setEnabled(c1_enabled);
                jKnob_vShift1.setText(jKnob_vShift1.getValue() + "/255");
                jKnob_vShift1.addChangeListener(new javax.swing.event.ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        c1_VShift = -jKnob_vShift1.getValue();
                        graph2.repaint();
                        jKnob_vShift1.setText(jKnob_vShift1.getValue() + "/255");
                    }
                });
                gbc.gridy = 1;
                gbc.gridheight = 4;
                panel_control.add(jKnob_vShift1, gbc);
            }
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.gridheight = 1;
            panel_control.add(new JCopyableLabel("Kanal 2 : V shift"), gbc);
            {
                jKnob_vShift2 = new JKnob(-255, 255, 0, 2);
                jKnob_vShift2.setEnabled(c2_enabled);
                jKnob_vShift2.setText(jKnob_vShift2.getValue() + "/255");
                jKnob_vShift2.addChangeListener(new javax.swing.event.ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        c2_VShift = -jKnob_vShift2.getValue();
                        graph2.repaint();
                        jKnob_vShift2.setText(jKnob_vShift2.getValue() + "/255");
                    }
                });
                gbc.gridheight = 4;
                gbc.gridy = 1;
                panel_control.add(jKnob_vShift2, gbc);
            }
            gbc.gridx = 3;
            gbc.gridy = 0;
            gbc.gridheight = 1;
            panel_control.add(new JCopyableLabel("Time DIV : zoom"), gbc);
            {
                final JKnob jKnob_Time_DIV = new JKnob(1, 2000, (int) ((Math.log(zoom) / Math.log(2) + 15) * 100), 10);
                jKnob_Time_DIV.setStartingAngle(0);
                jKnob_Time_DIV.addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        zoom = new Double(Math.pow(2, jKnob_Time_DIV.getValue() / 100d - 15));
                        fireDataChanged();
                    }
                });
                gbc.gridheight = 4;
                gbc.gridy = 1;
                panel_control.add(jKnob_Time_DIV, gbc);
            }
            {
                final JCheckBox jCheckBox_enableChannel1 = new JCheckBox("Kanal 1 aktiveren", c1_enabled);
                jCheckBox_enableChannel1.addChangeListener(new javax.swing.event.ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        boolean isSelected = jCheckBox_enableChannel1.isSelected();
                        if (c1_enabled != isSelected) {
                            c1_enabled = isSelected;
                            jKnob_vShift1.setEnabled(isSelected);
                            command_reset();
                            if (!c1_enabled & !c2_enabled) return;
                            command_start();
                        }
                    }
                });
                gbc.gridx = 4;
                gbc.gridheight = 1;
                gbc.gridy = 0;
                panel_control.add(jCheckBox_enableChannel1, gbc);
            }
            {
                final JButton jButton_colorChannel1 = new JButton("Farbe Kanal1");
                jButton_colorChannel1.setBackground(c1_color);
                jButton_colorChannel1.setForeground(inverseColor(c1_color));
                jButton_colorChannel1.addActionListener(new java.awt.event.ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        c1_color = chooseColor(1, c1_color);
                        jButton_colorChannel1.setBackground(c1_color);
                        jButton_colorChannel1.setForeground(inverseColor(c1_color));
                        graph2.repaint();
                    }
                });
                gbc.gridy = 1;
                panel_control.add(jButton_colorChannel1, gbc);
            }
            {
                final JCheckBox jCheckBox_enableChannel2 = new JCheckBox("Kanal 2 aktivieren", c2_enabled);
                jCheckBox_enableChannel2.addChangeListener(new javax.swing.event.ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        boolean isSelected = jCheckBox_enableChannel2.isSelected();
                        if (c2_enabled != isSelected) {
                            c2_enabled = isSelected;
                            jKnob_vShift2.setEnabled(isSelected);
                            command_reset();
                            command_start();
                        }
                    }
                });
                gbc.gridy = 3;
                panel_control.add(jCheckBox_enableChannel2, gbc);
            }
            {
                final JButton jButton_colorChannel2 = new JButton("Farbe Kanal2");
                jButton_colorChannel2.setBackground(c2_color);
                jButton_colorChannel2.setForeground(inverseColor(c2_color));
                jButton_colorChannel2.addActionListener(new java.awt.event.ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        c2_color = chooseColor(2, c2_color);
                        jButton_colorChannel2.setBackground(c2_color);
                        jButton_colorChannel2.setForeground(inverseColor(c2_color));
                        CompleteView_A_In.this.repaint();
                    }
                });
                gbc.gridy = 4;
                panel_control.add(jButton_colorChannel2, gbc);
            }
            {
                currentMousePosition = new JTextArea("In den Graphen\nrechtsklicken,\num die Werte\nanzuzeigen");
                currentMousePosition.setEditable(false);
                currentMousePosition.setBackground(Color.WHITE);
                currentMousePosition.setBorder(BorderFactory.createTitledBorder("aktueller Wert"));
                currentMousePosition.setMinimumSize(new Dimension(120, 146));
                currentMousePosition.setPreferredSize(new Dimension(120, 146));
                gbc.gridheight = 5;
                gbc.gridx = 5;
                gbc.gridy = 0;
                panel_control.add(currentMousePosition, gbc);
            }
            panel_control.setPreferredSize(new Dimension(panel_control.getMinimumSize().width, 180));
            panel_control.setMaximumSize(panel_control.getPreferredSize());
            panel_control.setAlignmentX(0.5f);
            panel_control.setPreferredSize(panel_control.getMinimumSize());
            this.add(panel_control, BorderLayout.NORTH);
        }
        {
            werteTabelle = new JTable(2, 0);
            jScrollPane_Table = new JScrollPane(werteTabelle);
            jScrollPane_Table.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            jScrollPane_Table.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            werteTabelle.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            werteTabelle.setCellSelectionEnabled(true);
            osziTableModel = new OsziTableModel(this);
            werteTabelle.setModel(osziTableModel);
            werteTabelle.getColumnModel().setColumnSelectionAllowed(false);
            werteTabelle.getTableHeader().setReorderingAllowed(false);
            jScrollPane_Table.setMinimumSize(new Dimension(0, (int) (JK8055GUI.FONTMETRICS.getHeight() * 4.5)));
            jScrollPane_Table.setMaximumSize(new Dimension(JK8055GUI.WINDOW_SIZE.width, (int) (JK8055GUI.FONTMETRICS.getHeight() * 4.5)));
            jScrollPane_Table.setPreferredSize(new Dimension(Integer.MAX_VALUE, (int) (JK8055GUI.FONTMETRICS.getHeight() * 4.5)));
        }
        ioListener = new IOListener() {

            @Override
            public K8055Channel getChannel() {
                return null;
            }

            @Override
            public IOChannels getDataType() {
                return IOChannels.ANALOG;
            }

            @Override
            public Component getTargetComponent() {
                return CompleteView_A_In.this;
            }

            @Override
            public boolean listenToAllChannels() {
                return true;
            }
        };
        fireDataChanged();
    }

    @Override
    protected void processEvent(AWTEvent evt) {
        if (evt.getID() == IOEvent.ID_ANALOG_ALL) {
            if (!isPaused & isPerspectiveEnabled) {
                AnalogAllEvent e = (AnalogAllEvent) evt;
                time_data.add((int) (e.eventMoment - time_start));
                if (c1_enabled) c1_data.add(e.values[0]);
                if (c2_enabled) c2_data.add(e.values[1]);
                if (jCheckBox_EnableTable.isSelected()) {
                    osziTableModel.fireColumnInserted();
                }
                if (System.currentTimeMillis() - lastRepaint > 100) {
                    fireDataChanged();
                    lastRepaint = System.currentTimeMillis();
                }
            }
        }
        super.processEvent(evt);
    }

    int getMaximumGraphValue() {
        if (time_data == null) return 0;
        try {
            return (int) (time_data.get(time_data.size() - 1) + 10 / zoom);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
	 * @return the difference between last and first visible point (in milliseconds)
	 */
    private int getVisibleIntervalSize() {
        return (int) (graph2.getPaintedWidth() / zoom);
    }

    /**
	 * returns the {@link Color} with the highest contrast to the argument Color
	 * 
	 * @param a Color
	 * @return the inverse Color to the argument
	 */
    private static Color inverseColor(Color color) {
        int R, G, B;
        if (color.getRed() < 126) R = 255; else R = 0;
        if (color.getGreen() < 126) G = 255; else G = 0;
        if (color.getBlue() < 126) B = 255; else B = 0;
        return new Color(R, G, B);
    }

    /**
	 * Shows a JColorChooser with the initialColor selected
	 * 
	 * @param channelNo
	 * @param initialColor
	 * @return
	 */
    private static Color chooseColor(int channelNo, Color initialColor) {
        Color color = JColorChooser.showDialog(null, "Farbe f�r Kanal " + channelNo + " ausw�hlen", initialColor);
        if (color == null) return initialColor;
        return color;
    }

    @Override
    protected Dimension getMinimumPanelSize() {
        return new Dimension(400, 500);
    }

    @Override
    public void setPerspectiveEnabled(boolean flag) {
        isPerspectiveEnabled = flag;
        setComponentsEnabled(this, flag);
    }

    private void setComponentsEnabled(JComponent c, boolean flag) {
        if (c.getComponents() == null) return;
        for (Component c2 : c.getComponents()) {
            if (!(c2 instanceof JComponent)) {
                c2.setEnabled(false);
            } else {
                JComponent c1 = (JComponent) c2;
                setComponentsEnabled(c1, flag);
                if (c1 == jButton_pause_unpause) {
                    if (flag) jButton_pause_unpause.setEnabled(jButton_start_reset.getText() == "Reset"); else jButton_pause_unpause.setEnabled(false);
                } else if (c1 instanceof JKnob) {
                    if (c1 == jKnob_vShift1) {
                        if (flag) jKnob_vShift1.setEnabled(c1_enabled); else c1.setEnabled(false);
                    } else if (c1 == jKnob_vShift2) {
                        if (flag) jKnob_vShift2.setEnabled(c2_enabled); else c1.setEnabled(false);
                    } else {
                        c1.setEnabled(flag);
                    }
                } else if (c1 == jCheckBox_EnableTable) {
                    if (flag) {
                        jCheckBox_EnableTable.setEnabled(isPaused);
                    } else jCheckBox_EnableTable.setEnabled(false);
                } else c1.setEnabled(flag);
            }
        }
    }

    private void command_start() {
        jButton_start_reset.setText("Reset");
        jButton_pause_unpause.setEnabled(true);
        jButton_pause_unpause.setText("Pause");
        isPaused = false;
        time_start = System.currentTimeMillis();
        c1_data = new ArrayShortList(initialArraySize);
        c2_data = new ArrayShortList(initialArraySize);
        time_data = new ArrayIntList(initialArraySize);
        JK8055GUI.getDataAdapter().addDataListener(ioListener);
        fireDataChanged();
    }

    private void command_reset() {
        jButton_start_reset.setText("Start");
        jButton_pause_unpause.setEnabled(false);
        isPaused = true;
        time_start = 0;
        c1_data = null;
        c2_data = null;
        time_data = null;
        start = 0;
        osziTableModel.fireDataDeleted();
        JK8055GUI.getDataAdapter().removeDataListener(ioListener);
        fireDataChanged();
    }

    private void command_save() {
        if (!c1_enabled & !c2_enabled) return;
        isPaused = true;
        int i = 1;
        if (c1_enabled) i++;
        if (c2_enabled) i++;
        String[] a = new String[i];
        a[0] = "Zeit t in ms";
        if (c1_enabled) {
            a[1] = "Kanal 1: Wert von 0 bis 255, der eine Spannung von 0V bis 5V repräsentiert";
        }
        if (c2_enabled) {
            a[i - 1] = "Kanal 2: Wert von 0 bis 255, der eine Spannung von 0V bis 5V repräsentiert";
        }
        SaveData saveData;
        if (c1_enabled) {
            if (c2_enabled) saveData = new SaveData(time_data, c1_data, c2_data, a); else saveData = new SaveData(time_data, c1_data, a);
        } else {
            saveData = new SaveData(time_data, c2_data, a);
        }
        saveData.showSaveDialog();
        System.gc();
    }

    /**
	 * re-render fields like size of jScrollBar_oszi and start value of the graph
	 */
    void fireDataChanged() {
        int visibleIntervalSize = getVisibleIntervalSize();
        int maximumGraphValue = getMaximumGraphValue();
        if (time_data == null) {
            jScrollBar_oszi.setMinimum(0);
            jScrollBar_oszi.setMaximum(visibleIntervalSize);
            if (jScrollBar_oszi.isEnabled()) jScrollBar_oszi.setEnabled(false);
        } else {
            try {
                jScrollBar_oszi.setValues(start, visibleIntervalSize / (maximumGraphValue - visibleIntervalSize), 0, maximumGraphValue - visibleIntervalSize);
            } catch (ArithmeticException e) {
            }
            if (!jScrollBar_oszi.isEnabled()) jScrollBar_oszi.setEnabled(true);
        }
        if (start > oldMaximumStartValue - 100d / zoom) start = oldMaximumStartValue = maximumGraphValue - visibleIntervalSize;
        if (start < 0) start = 0;
        graph2.repaint();
    }

    @Override
    public String getTitle() {
        return "Analog Input with Oscilloscope";
    }
}
