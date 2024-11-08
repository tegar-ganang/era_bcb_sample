package de.reichhold.jrehearsal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URL;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * A JukeBox for sampled and midi sound files. Features duration progress, seek
 * slider, pan and volume controls.
 * 
 * @version
 * @(#)Juke.java 1.19 00/01/31
 * @author Brian Lichtenwalter
 */
public class MidiPlayer extends JPanel {

    public static final int GAIN_RANGE = 10;

    private static final long serialVersionUID = -1116882158348539434L;

    private static final String BASE_TITLE = Messages.getString("MidiPlayer.Title");

    final int bufSize = 16384;

    private PlaybackMonitor playbackMonitor = new PlaybackMonitor(this);

    private MySequencer sequencer;

    private SequenceHandler.BarTypeData barData;

    private String currentName;

    private JButton startB;

    private JButton pauseB;

    private JCheckBox countInB;

    private JTable channelTable;

    private JSlider gainSlider;

    private JSlider speedSlider;

    private JSlider seekSlider;

    private JukeTable jukeTable;

    private PositionsTable positionTable;

    private ButtonGroup selectableUnit;

    private JukeControls controls;

    private ChangeListener speedChangeListener = new ChangeListener() {

        public void stateChanged(ChangeEvent e) {
            setSpeed();
        }
    };

    ListSelectionListener soundSelectionListener = new ListSelectionListener() {

        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                final URL selectedSound = jukeTable.getSelectedUrl();
                startB.setEnabled(selectedSound != null);
                if (selectedSound != null) {
                    new Thread(new Runnable() {

                        public void run() {
                            loadSound(selectedSound);
                        }
                    }).start();
                }
            }
        }
    };

    public MidiPlayer(String dirName) {
        sequencer = new MySequencer();
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, jukeTable = new JukeTable(), positionTable = new PositionsTable(this));
        splitPane1.setContinuousLayout(true);
        splitPane1.setDividerLocation(200);
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane1, controls = new JukeControls());
        splitPane2.setContinuousLayout(true);
        add(splitPane2);
        if (dirName != null) {
            jukeTable.loadJuke(dirName);
        }
        jukeTable.addSelectionListener(soundSelectionListener);
        jukeTable.addErrorListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                playbackMonitor.setErrorText((String) e.getSource());
                playbackMonitor.repaint();
            }
        });
        sequencer.addObserver(new Observer() {

            public void update(Observable o, Object arg) {
                if (arg instanceof Float) {
                    float bpm = (Float) arg;
                    speedSlider.removeChangeListener(speedChangeListener);
                    speedSlider.setValue((int) bpm);
                    speedSlider.addChangeListener(speedChangeListener);
                } else if (arg.equals(MySequencer.TRACK_HAS_ENDED)) {
                    stop();
                }
            }
        });
    }

    public synchronized void loadSound(URL midiFile) {
        stop();
        playbackMonitor.startLoading();
        currentName = midiFile.getFile();
        currentName = currentName.substring(currentName.lastIndexOf('/') + 1);
        ((JFrame) this.getParent().getParent().getParent().getParent()).setTitle(BASE_TITLE + " - " + currentName);
        try {
            sequencer.loadFile(midiFile);
            barData = sequencer.getBarData();
            positionTable.setFileName(midiFile.getFile());
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
        playbackMonitor.stopLoading();
        setSeekSliderRange();
        seekSlider.setEnabled(true);
        gainSlider.setEnabled(true);
        speedSlider.setEnabled(true);
        speedSlider.setValue(sequencer.getInitialTempoInBPM());
        for (Enumeration<AbstractButton> i = selectableUnit.getElements(); i.hasMoreElements(); ) {
            i.nextElement().setEnabled(true);
        }
        channelTableChanged();
        startB.setEnabled(true);
        playbackMonitor.repaint();
        return;
    }

    public void channelTableChanged() {
        channelTable.tableChanged(new TableModelEvent(channelTable.getModel()));
    }

    private void setSeekSliderRange() {
        long currentPosition = sequencer.getTickPosition();
        if (getUnit().equals("Bars")) {
            seekSlider.setMinimum(1);
            seekSlider.setMaximum(barData.getBarNumber(sequencer.getTickLength()).bar);
        } else {
            seekSlider.setMinimum(0);
            seekSlider.setMaximum((int) (sequencer.getMicrosecondLength() / 1000));
        }
        sequencer.setTickPosition(currentPosition);
        setSeekSlider(currentPosition);
        setSpeed();
    }

    public void playSound() throws InvalidMidiDataException, MidiParserException {
        playbackMonitor.start();
        sequencer.startAt(getSliderTickPositionInSequence(), countInB.isSelected());
    }

    public MySequencer getSequencer() {
        return sequencer;
    }

    public SequenceHandler.BarTypeData.PositionInBar getPositionInSequenceAsBars() {
        return sequencer.getPositionInSequenceAsBars();
    }

    public void setGain() throws MidiParserException, InvalidMidiDataException {
        sequencer.setGain(gainSlider.getValue());
    }

    public void setSeekSlider(long argTickPosition) {
        if (getUnit().equals("Bars")) {
            seekSlider.setValue(barData.getBarNumber(argTickPosition).bar);
        } else {
            seekSlider.setValue((int) (argTickPosition / 1000));
        }
    }

    public void setSpeed() {
        int value = speedSlider.getValue();
        if (sequencer != null) {
            sequencer.setTempoInBPM((float) (value));
        }
    }

    private long getSliderTickPositionInSequence() {
        int value = seekSlider.getValue();
        long result = 0;
        if (selectableUnit.getSelection().getActionCommand().equals("Seconds")) {
            sequencer.setSecondPosition(value);
            result = sequencer.getTickPosition();
        } else if (selectableUnit.getSelection().getActionCommand().equals("Bars")) {
            result = barData.convertBarPositionToTicks(value);
        } else {
            sequencer.setSecondPosition(value);
            result = sequencer.getTickPosition();
        }
        return result;
    }

    private void stop() {
        playbackMonitor.stop();
        startB.setText(Messages.getString("MidiPlayer.StartButton"));
        pauseB.setText(Messages.getString("MidiPlayer.PauseButton"));
        controls.setComponentsEnabled(false);
        sequencer.stop();
    }

    private static class SliderEditor extends AbstractCellEditor implements TableCellEditor {

        private static final long serialVersionUID = 1L;

        private JSlider control;

        {
            control = new JSlider(0, GAIN_RANGE);
            control.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    if (!control.getModel().getValueIsAdjusting()) {
                        stopCellEditing();
                    }
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            control.setValue((Integer) value);
            return control;
        }

        public Object getCellEditorValue() {
            return control.getValue();
        }
    }

    private static class SliderRenderer implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private JSlider control = new JSlider(0, GAIN_RANGE);

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            control.setValue((Integer) value);
            return control;
        }
    }

    /**
   * GUI controls for start, stop, previous, next, pan and gain.
   */
    @SuppressWarnings("serial")
    class JukeControls extends JPanel {

        public JukeControls() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            JPanel p1 = new JPanel();
            p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
            p1.setBorder(new EmptyBorder(10, 0, 5, 0));
            JPanel p2 = new JPanel();
            startB = new JButton(Messages.getString("MidiPlayer.StartButton"));
            startB.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        if (startB.getText().equals(Messages.getString("MidiPlayer.StartButton"))) {
                            startB.setText(Messages.getString("MidiPlayer.StopButton"));
                            playSound();
                            setComponentsEnabled(true);
                        } else {
                            stop();
                        }
                    } catch (InvalidMidiDataException e1) {
                        e1.printStackTrace();
                    } catch (MidiParserException e2) {
                        e2.printStackTrace();
                    }
                }
            });
            startB.setEnabled(false);
            p2.add(startB);
            pauseB = new JButton(Messages.getString("MidiPlayer.PauseButton"));
            pauseB.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        if (pauseB.getText().equals(Messages.getString("MidiPlayer.PauseButton"))) {
                            sequencer.stop();
                            playbackMonitor.stop();
                            pauseB.setText(Messages.getString("MidiPlayer.ResumeButton"));
                        } else {
                            sequencer.startAt(sequencer.getTickPosition(), false);
                            speedSlider.setValue(sequencer.getTempoInBPM());
                            playbackMonitor.start();
                            pauseB.setText(Messages.getString("MidiPlayer.PauseButton"));
                        }
                    } catch (InvalidMidiDataException e1) {
                        e1.printStackTrace();
                    } catch (MidiParserException e2) {
                        e2.printStackTrace();
                    }
                }
            });
            pauseB.setEnabled(false);
            p2.add(pauseB);
            p1.add(p2);
            JPanel p3 = new JPanel();
            p1.add(p3);
            add(p1);
            JPanel p4 = new JPanel(new BorderLayout());
            JPanel p41 = new JPanel(new BorderLayout());
            EmptyBorder eb = new EmptyBorder(5, 5, 5, 5);
            BevelBorder bb = new BevelBorder(BevelBorder.LOWERED);
            p41.setBorder(new CompoundBorder(eb, bb));
            p41.add(playbackMonitor);
            seekSlider = new JSlider(JSlider.HORIZONTAL);
            seekSlider.setEnabled(false);
            seekSlider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    sequencer.setTickPosition(getSliderTickPositionInSequence());
                    playbackMonitor.repaint();
                }
            });
            p41.add("South", seekSlider);
            p4.add("Center", p41);
            JPanel units = new JPanel();
            units.setLayout(new BoxLayout(units, BoxLayout.Y_AXIS));
            selectableUnit = new ButtonGroup();
            JRadioButton radioButton = new JRadioButton(Messages.getString("MidiPlayer.SecondsRel"));
            radioButton.getModel().setActionCommand("Seconds");
            radioButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setSeekSliderRange();
                    countInB.setEnabled(false);
                }
            });
            radioButton.setEnabled(false);
            units.add(radioButton);
            selectableUnit.add(radioButton);
            radioButton = new JRadioButton(Messages.getString("MidiPlayer.Seconds"));
            radioButton.getModel().setActionCommand("real Seconds");
            radioButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setSeekSliderRange();
                    countInB.setEnabled(false);
                }
            });
            radioButton.setEnabled(false);
            units.add(radioButton);
            selectableUnit.add(radioButton);
            radioButton = new JRadioButton(Messages.getString("MidiPlayer.Bars"));
            radioButton.getModel().setActionCommand("Bars");
            radioButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    setSeekSliderRange();
                    countInB.setEnabled(true);
                }
            });
            units.add(radioButton);
            radioButton.setEnabled(false);
            selectableUnit.add(radioButton);
            selectableUnit.setSelected(radioButton.getModel(), true);
            countInB = new JCheckBox(Messages.getString("MidiPlayer.Count"));
            countInB.setEnabled(true);
            units.add(countInB);
            p4.add("East", units);
            add(p4);
            JPanel p5 = new JPanel();
            p5.setLayout(new GridBagLayout());
            p5.setBorder(new EmptyBorder(5, 5, 10, 5));
            gainSlider = new JSlider(0, GAIN_RANGE, GAIN_RANGE);
            gainSlider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    int value = gainSlider.getValue();
                    TitledBorder border2 = (TitledBorder) gainSlider.getBorder();
                    String s = (border2).getTitle();
                    s = s.substring(0, s.indexOf('=') + 1) + String.valueOf(value);
                    try {
                        setGain();
                    } catch (MidiParserException e1) {
                        e1.printStackTrace();
                    } catch (InvalidMidiDataException e1) {
                        e1.printStackTrace();
                    }
                    border2.setTitle(s);
                    gainSlider.repaint();
                }
            });
            TitledBorder tb = new TitledBorder(new EtchedBorder());
            tb.setTitle(Messages.getString("MidiPlayer.Gain") + " = " + GAIN_RANGE);
            gainSlider.setBorder(tb);
            p5.add(gainSlider, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
            speedSlider = new JSlider(10, 200, 80);
            tb = new TitledBorder(new EtchedBorder());
            tb.setTitle(Messages.getString("MidiPlayer.Speed") + " = 80");
            speedSlider.setBorder(tb);
            speedSlider.addChangeListener(speedChangeListener);
            speedSlider.addChangeListener(new ChangeListener() {

                public void stateChanged(ChangeEvent e) {
                    int value = speedSlider.getValue();
                    TitledBorder border2 = (TitledBorder) speedSlider.getBorder();
                    String s = (border2).getTitle();
                    s = s.substring(0, s.indexOf('=') + 1) + value;
                    border2.setTitle(s);
                    speedSlider.repaint();
                }
            });
            p5.add(speedSlider, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
            add(p5);
            channelTable = getChannelTable();
            add(new JScrollPane(channelTable));
        }

        private JTable getChannelTable() {
            final String[] names = { Messages.getString("MidiPlayer.Instrument"), Messages.getString("MidiPlayer.Active"), Messages.getString("MidiPlayer.Gain") };
            TableModel dataModel = new AbstractTableModel() {

                public int getColumnCount() {
                    return names.length;
                }

                public int getRowCount() {
                    return sequencer.getTrackDescriptions().size();
                }

                public Object getValueAt(int row, int col) {
                    Object result = null;
                    switch(col) {
                        case 0:
                            result = sequencer.getTrackDescriptions().get(row).name;
                            break;
                        case 1:
                            result = sequencer.getTrackDescriptions().get(row).active ? Boolean.TRUE : Boolean.FALSE;
                            break;
                        case 2:
                            result = sequencer.getTrackDescriptions().get(row).gain;
                            break;
                    }
                    return result;
                }

                public String getColumnName(int col) {
                    return names[col];
                }

                public Class<? extends Object> getColumnClass(int c) {
                    return getValueAt(0, c).getClass();
                }

                public boolean isCellEditable(int row, int col) {
                    return col == 0 ? false : true;
                }

                public void setValueAt(Object aValue, int row, int col) {
                    if (col == 1) {
                        sequencer.getTrackDescriptions().get(row).active = (Boolean) aValue;
                        try {
                            sequencer.notifySettingsChange();
                        } catch (MidiParserException e) {
                            e.printStackTrace();
                        } catch (InvalidMidiDataException e) {
                            e.printStackTrace();
                        }
                    } else if (col == 2) {
                        sequencer.getTrackDescriptions().get(row).gain = (Integer) aValue;
                        try {
                            sequencer.notifySettingsChange();
                        } catch (MidiParserException e) {
                            e.printStackTrace();
                        } catch (InvalidMidiDataException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            JTable result = new JTable(dataModel);
            result.getColumn(names[0]).setMinWidth(120);
            result.getColumn(names[2]).setCellEditor(new SliderEditor());
            result.getColumn(names[2]).setCellRenderer(new SliderRenderer());
            return result;
        }

        public void setComponentsEnabled(boolean state) {
            pauseB.setEnabled(state);
        }
    }

    public static void main(String args[]) throws LineUnavailableException {
        String media = ".";
        final MidiPlayer player = new MidiPlayer(args.length == 0 ? media : args[0]);
        JFrame f = new JFrame(BASE_TITLE);
        f.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            public void windowIconified(WindowEvent e) {
            }
        });
        f.getContentPane().add("Center", player);
        f.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = 850;
        int h = 540;
        f.setLocation(screenSize.width / 2 - w / 2, screenSize.height / 2 - h / 2);
        f.setSize(w, h);
        f.setVisible(true);
        if (args.length > 0) {
            File file = new File(args[0]);
            if (file != null && !file.isDirectory()) {
                System.out.println("usage: java Juke audioDirectory");
            }
        }
        f.validate();
    }

    public String getCurrentName() {
        return currentName;
    }

    public String getUnit() {
        return selectableUnit.getSelection().getActionCommand();
    }
}
