package org.jsynthlib.core;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

/**
 * This class provides a preferences panel for synthesiser configuration.
 *
 * @author ???
 * @author Hiroo Hayashi
 */
public class SynthConfigPanel extends ConfigPanel {

    private static final long serialVersionUID = 1L;

    private TableModel tableModel;

    /** Multiple MIDI Interface CheckBox */
    private JCheckBox cbxMMI;

    private boolean multiMIDI;

    private JTable table;

    private MidiScan midiScan;

    private JPopupMenu popup;

    private static final int SYNTH_NAME = 0;

    private static final int DEVICE = 1;

    private static final int MIDI_IN = 2;

    private static final int MIDI_OUT = 3;

    private static final int MIDI_CHANNEL = 4;

    private static final int MIDI_DEVICE_ID = 5;

    public SynthConfigPanel(final PrefsDialog parent) {
        super(parent, "Synth Driver");
        setLayout(new BorderLayout());
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        tableModel = new TableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setPreferredScrollableViewportSize(new Dimension(750, 150));
        TableColumn column;
        column = table.getColumnModel().getColumn(SYNTH_NAME);
        column.setPreferredWidth(75);
        column = table.getColumnModel().getColumn(DEVICE);
        column.setPreferredWidth(250);
        column = table.getColumnModel().getColumn(MIDI_IN);
        column.setPreferredWidth(200);
        DefaultComboBoxModel<String> inputNamesModel = new DefaultComboBoxModel<String>();
        inputNamesModel.addRows(MidiUtil.getInputDeviceNames());
        column.setCellEditor(new DefaultCellEditor(new JComboBox(inputNamesModel)));
        column = table.getColumnModel().getColumn(MIDI_OUT);
        column.setPreferredWidth(200);
        DefaultComboBoxModel<String> outputNamesModel = new DefaultComboBoxModel<String>();
        outputNamesModel.addRows(MidiUtil.getOutputDeviceNames());
        column.setCellEditor(new DefaultCellEditor(new JComboBox(outputNamesModel)));
        column = table.getColumnModel().getColumn(MIDI_CHANNEL);
        column.setPreferredWidth(90);
        JScrollPane scrollpane = new JScrollPane(table);
        p.add(scrollpane, c);
        cbxMMI = new JCheckBox("Use Multiple MIDI Interface");
        cbxMMI.setToolTipText("Allows users to select different MIDI port for each synth.");
        cbxMMI.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                multiMIDI = cbxMMI.isSelected();
                setModified(true);
            }
        });
        ++c.gridy;
        p.add(cbxMMI, c);
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton add = new JButton("Add Device...");
        add.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addDevice();
            }
        });
        buttonPanel.add(add);
        JButton scan = new JButton("Auto-Scan...");
        scan.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                scanMidi();
            }
        });
        buttonPanel.add(scan);
        ++c.gridy;
        p.add(buttonPanel, c);
        add(p, BorderLayout.CENTER);
        popup = new JPopupMenu();
        JMenuItem mi;
        mi = new JMenuItem("Delete");
        mi.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeDevice();
            }
        });
        popup.add(mi);
        mi = new JMenuItem("Property...");
        mi.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showDeviceProperty();
            }
        });
        popup.add(mi);
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void removeDevice() {
        if ((table.getSelectedRow() == -1) || (table.getSelectedRow() == 0)) {
            return;
        }
        if (JOptionPane.showConfirmDialog(null, "Are you sure?", "Remove Device?", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }
        appConfig.removeDevice(table.getSelectedRow());
        Utility.revalidateLibraries();
        tableModel.fireTableDataChanged();
        table.repaint();
    }

    private void showDeviceProperty() {
        if ((table.getSelectedRow() != -1)) {
            Device device = appConfig.getDevice(table.getSelectedRow());
            DeviceDetailsDialog ddd = new DeviceDetailsDialog(Utility.getFrame(this), device);
            ddd.setVisible(true);
        }
    }

    private void addDevice() {
        DeviceAddDialog dad = new DeviceAddDialog(null);
        dad.setVisible(true);
        Utility.revalidateLibraries();
        tableModel.fireTableDataChanged();
    }

    private void scanMidi() {
        if (JOptionPane.showConfirmDialog(null, "Scanning the System for supported Synthesizers may take\n" + "a few minutes if you have many MIDI ports. During the scan\n" + "it is normal for the system to be unresponsive.\n" + "Do you wish to scan?", "Scan for Synthesizers", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
            return;
        }
        if (midiScan != null) {
            midiScan.close();
        }
        ProgressMonitor pm = new ProgressMonitor(null, "Scanning for SupportedSynthesizers", "Initializing Midi Devices", 0, 100);
        midiScan = new MidiScan(tableModel, pm, null);
        midiScan.start();
        Utility.revalidateLibraries();
        tableModel.fireTableDataChanged();
    }

    @Override
    public void init() {
        multiMIDI = appConfig.getMultiMIDI();
        cbxMMI.setSelected(multiMIDI);
        cbxMMI.setEnabled(appConfig.getMidiEnable());
        tableModel.fireTableDataChanged();
    }

    @Override
    public void commit() {
        appConfig.setMultiMIDI(multiMIDI);
        tableModel.fireTableDataChanged();
        if (!multiMIDI) {
            try {
                int out = appConfig.getInitPortOut();
                int in = appConfig.getInitPortIn();
                for (Device device : appConfig.getDevices()) {
                    device.setPort(out);
                    device.setInPort(in);
                }
            } catch (MidiUnavailableException exception) {
                ErrorDialog.showDialog(parent, "Error", "Failed to commit", exception);
            }
        }
        setModified(false);
    }

    private class TableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        private final String[] columnNames = { "Synth ID", "Device", "MIDI In Port", "MIDI Out Port", "Channel #", "Device ID" };

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public Class<?> getColumnClass(final int col) {
            return getValueAt(0, col).getClass();
        }

        @Override
        public String getColumnName(final int col) {
            return columnNames[col];
        }

        @Override
        public int getRowCount() {
            return appConfig.getDeviceCount();
        }

        @Override
        public boolean isCellEditable(final int row, final int col) {
            return (col == SYNTH_NAME || (col == MIDI_IN && multiMIDI) || (col == MIDI_OUT && multiMIDI) || col == MIDI_CHANNEL || col == MIDI_DEVICE_ID);
        }

        @Override
        public Object getValueAt(int row, int col) {
            Device dev = appConfig.getDevice(row);
            switch(col) {
                case SYNTH_NAME:
                    return dev.getSynthName();
                case DEVICE:
                    return dev.getManufacturerName() + " " + dev.getModelName();
                case MIDI_IN:
                    if (MidiUtil.isInputAvailable()) {
                        try {
                            int port = multiMIDI ? dev.getInPort() : appConfig.getInitPortIn();
                            return MidiUtil.getInputDeviceName(port);
                        } catch (Exception ex) {
                            return "not available";
                        }
                    } else {
                        return "not available";
                    }
                case MIDI_OUT:
                    if (MidiUtil.isOutputAvailable()) {
                        try {
                            int port = multiMIDI ? dev.getPort() : appConfig.getInitPortOut();
                            return MidiUtil.getOutputDeviceName(port);
                        } catch (Exception ex) {
                            return "not available";
                        }
                    } else {
                        return "not available";
                    }
                case MIDI_CHANNEL:
                    return Integer.valueOf(dev.getChannel());
                case MIDI_DEVICE_ID:
                    return Integer.valueOf(dev.getDeviceID());
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public void setValueAt(final Object value, final int row, final int col) {
            Device dev = appConfig.getDevice(row);
            switch(col) {
                case SYNTH_NAME:
                    dev.setSynthName(value.toString());
                    break;
                case DEVICE:
                    return;
                case MIDI_IN:
                    try {
                        dev.setInPort(MidiUtil.getInputDevicePort(value.toString()));
                    } catch (MidiUnavailableException exception) {
                    }
                    break;
                case MIDI_OUT:
                    dev.setPort(MidiUtil.getOutputDevicePort(value.toString()));
                    break;
                case MIDI_CHANNEL:
                    dev.setChannel(((Integer) value).intValue());
                    break;
                case MIDI_DEVICE_ID:
                    dev.setDeviceID(((Integer) value).intValue());
                    break;
                default:
                    throw new IndexOutOfBoundsException();
            }
            fireTableCellUpdated(row, col);
        }
    }
}
