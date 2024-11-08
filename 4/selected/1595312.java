package de.reichhold.jrehearsal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * Table to display the name of the sound.
 */
class PositionsTable extends JPanel {

    private static final long serialVersionUID = 1L;

    private static class PositionData implements Serializable {

        private static final long serialVersionUID = 1L;

        private String name;

        private int speed;

        private long tickPosition;

        private List<MySequencer.Data> channelData;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSpeed() {
            return speed;
        }

        public void setSpeed(int speed) {
            this.speed = speed;
        }

        public long getTickPosition() {
            return tickPosition;
        }

        public void setTickPosition(long tickPosition) {
            this.tickPosition = tickPosition;
        }

        public List<MySequencer.Data> getChannelData() {
            return channelData;
        }

        public void setChannelData(List<MySequencer.Data> channelData) {
            this.channelData = channelData;
        }

        public String toString() {
            return getName();
        }
    }

    ;

    private TableModel dataModel;

    private JTable table;

    private List<PositionData> positionsList = new ArrayList<PositionData>();

    private MySequencer sequencer;

    private String parentFile;

    private JButton saveButton;

    private JButton removeButton;

    private JButton addButton;

    private MidiPlayer player;

    private JButton modifyButton;

    public PositionsTable(MidiPlayer argPlayer) {
        player = argPlayer;
        sequencer = argPlayer.getSequencer();
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(260, 300));
        final String[] names = { "#", Messages.getString("PositionsTable.Name") };
        dataModel = new AbstractTableModel() {

            private static final long serialVersionUID = 1L;

            public int getColumnCount() {
                return names.length;
            }

            public int getRowCount() {
                return positionsList.size();
            }

            public Object getValueAt(int row, int col) {
                if (col == 0) {
                    return new Integer(row + 1);
                } else {
                    return positionsList.get(row);
                }
            }

            public String getColumnName(int col) {
                return names[col];
            }

            public Class<? extends Object> getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

            public boolean isCellEditable(int row, int col) {
                return col == 1;
            }

            public void setValueAt(Object aValue, int row, int col) {
                ((PositionData) getValueAt(row, col)).setName(aValue.toString());
            }
        };
        table = new JTable(dataModel);
        table.getColumn(names[1]).setCellEditor(new DefaultCellEditor(new JTextField()));
        TableColumn col = table.getColumn("#");
        col.setMaxWidth(20);
        table.sizeColumnsToFit(0);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    PositionData posData = positionsList.get(table.getSelectedRow());
                    sequencer.setTickPosition(posData.getTickPosition());
                    sequencer.setTempoInBPM(posData.getSpeed());
                    try {
                        sequencer.setTrackDescriptions(posData.getChannelData());
                    } catch (MidiParserException e1) {
                        e1.printStackTrace();
                    } catch (InvalidMidiDataException e1) {
                        e1.printStackTrace();
                    }
                    player.setSeekSlider(posData.getTickPosition());
                    player.channelTableChanged();
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        EmptyBorder eb = new EmptyBorder(5, 5, 2, 5);
        scrollPane.setBorder(new CompoundBorder(eb, new EtchedBorder()));
        add(scrollPane, BorderLayout.CENTER);
        JPanel p1 = new JPanel(new GridBagLayout());
        addButton = new JButton(Messages.getString("AddButton"));
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PositionData newPositionData = new PositionData();
                newPositionData.setTickPosition(sequencer.getTickPosition());
                newPositionData.setSpeed(sequencer.getTempoInBPM());
                newPositionData.setName(Messages.getString("PositionsTable.NewPosition"));
                newPositionData.setChannelData(sequencer.getClonedTrackDescriptions());
                positionsList.add(newPositionData);
                tableChanged();
            }
        });
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        p1.add(addButton, gridBagConstraints);
        modifyButton = new JButton(Messages.getString("PositionsTable.ModifyButton"));
        modifyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (table.getSelectedRow() >= 0) {
                    PositionData positionData = positionsList.get(table.getSelectedRow());
                    positionData.setTickPosition(sequencer.getTickPosition());
                    positionData.setSpeed(sequencer.getTempoInBPM());
                    positionData.setChannelData(sequencer.getClonedTrackDescriptions());
                    tableChanged();
                }
            }
        });
        gridBagConstraints.gridy = 1;
        p1.add(modifyButton, gridBagConstraints);
        removeButton = new JButton(Messages.getString("RemoveButton"));
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = table.getSelectedRows();
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    positionsList.remove(selectedRows[i]);
                }
                tableChanged();
            }
        });
        gridBagConstraints.gridy = 2;
        p1.add(removeButton, gridBagConstraints);
        saveButton = new JButton(Messages.getString("PositionsTable.SaveButton"));
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    FileOutputStream fos = new FileOutputStream(parentFile + ".sav");
                    ObjectOutputStream out = new ObjectOutputStream(fos);
                    out.writeObject(positionsList);
                    out.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        gridBagConstraints.gridy = 3;
        p1.add(saveButton, gridBagConstraints);
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        p1.add(new JPanel(), gridBagConstraints);
        add(p1, BorderLayout.EAST);
        add(new JLabel(Messages.getString("PositionsTable.Positions")), BorderLayout.NORTH);
        setFileName(null);
    }

    public void addSelectionListener(ListSelectionListener argListener) {
        table.getSelectionModel().addListSelectionListener(argListener);
    }

    @SuppressWarnings("unchecked")
    public void setFileName(String argFileName) {
        parentFile = argFileName;
        positionsList = new ArrayList<PositionData>();
        if (parentFile == null) {
            addButton.setEnabled(false);
            modifyButton.setEnabled(false);
            removeButton.setEnabled(false);
            saveButton.setEnabled(false);
        } else {
            try {
                FileInputStream fis = new FileInputStream(parentFile + ".sav");
                ObjectInputStream in = new ObjectInputStream(fis);
                positionsList = (List<PositionData>) in.readObject();
                in.close();
            } catch (FileNotFoundException ex) {
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            addButton.setEnabled(true);
            modifyButton.setEnabled(true);
            removeButton.setEnabled(true);
            saveButton.setEnabled(true);
        }
        tableChanged();
    }

    public Dimension getMinimumSize() {
        Dimension result = super.getMinimumSize();
        result.width = 250;
        return result;
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void tableChanged() {
        table.tableChanged(new TableModelEvent(dataModel));
    }
}
