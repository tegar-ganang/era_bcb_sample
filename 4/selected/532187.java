package eu.davidgamez.mas.agents.midifragmentsequencer.gui;

import java.util.Vector;
import java.util.Iterator;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.sound.midi.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import eu.davidgamez.mas.exception.MASXmlException;
import eu.davidgamez.mas.gui.AgentPropertiesPanel;

public class MidiFragmentSequencer extends AgentPropertiesPanel implements ActionListener, DragGestureListener, DropTargetListener, DragSourceListener, ListSelectionListener {

    private JButton loadMidiFiles = new JButton("Load Midi Files");

    private JList midiFragmentList = new JList();

    private Vector midiFragmentNames = new Vector();

    private Vector midiFragments;

    private boolean isDragging = false;

    private int selectedIndex = -1;

    private String selectedFragmentDescription;

    private MidiEvent[] selectedMidiFragment;

    private JFileChooser midiFileChooser;

    private Track[] trackArray;

    public MidiFragmentSequencer() {
        super("MIDIFragmentSequencer");
        setUpPanel();
    }

    private void setUpPanel() {
        BorderLayout borderLayout = new BorderLayout();
        borderLayout.setVgap(10);
        borderLayout.setHgap(10);
        this.setLayout(borderLayout);
        loadMidiFiles.addActionListener(this);
        Box loadFilesBox = Box.createHorizontalBox();
        loadFilesBox.add(loadMidiFiles);
        loadFilesBox.add(Box.createHorizontalGlue());
        this.add(loadFilesBox, BorderLayout.NORTH);
        midiFragmentList.setSelectionBackground(Color.yellow);
        midiFragmentList.setListData(midiFragmentNames);
        midiFragmentList.addListSelectionListener(this);
        DragSource dragSource = DragSource.getDefaultDragSource();
        dragSource.createDefaultDragGestureRecognizer(midiFragmentList, DnDConstants.ACTION_COPY_OR_MOVE, this);
        DropTarget dropTarget = new DropTarget(midiFragmentList, DnDConstants.ACTION_COPY_OR_MOVE, this);
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(midiFragmentList);
        this.add(scrollPane, BorderLayout.CENTER);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == loadMidiFiles) {
            loadMidiFragments();
        }
    }

    public void loadPanelState(String agentPropertiesString) throws Exception {
    }

    public void loadAgentProperties() {
    }

    public boolean okButtonPressed() {
        return true;
    }

    public boolean applyButtonPressed() {
        return true;
    }

    public boolean cancelButtonPressed() {
        return true;
    }

    public void dragGestureRecognized(DragGestureEvent e) {
        selectedFragmentDescription = (String) midiFragmentList.getSelectedValue();
        selectedIndex = midiFragmentList.getSelectedIndex();
        selectedMidiFragment = (MidiEvent[]) midiFragments.get(selectedIndex);
        isDragging = true;
        midiFragmentList.setSelectionForeground(Color.lightGray);
        midiFragmentList.setSelectionBackground(Color.white);
        e.startDrag(DragSource.DefaultCopyDrop, new StringSelection(selectedFragmentDescription), this);
    }

    public void dragDropEnd(DragSourceDropEvent e) {
    }

    public void dragEnter(DragSourceDragEvent e) {
    }

    public void dragExit(DragSourceEvent e) {
    }

    public void dragOver(DragSourceDragEvent e) {
    }

    public void dropActionChanged(DragSourceDragEvent e) {
    }

    public void drop(DropTargetDropEvent e) {
        isDragging = false;
        midiFragmentList.setSelectionForeground(Color.black);
        midiFragmentList.setSelectionBackground(Color.lightGray);
        Point dropPoint = e.getLocation();
        int dropIndex = midiFragmentList.locationToIndex(dropPoint);
        Rectangle dropCell = midiFragmentList.getCellBounds(dropIndex, dropIndex);
        midiFragmentNames.remove(selectedIndex);
        midiFragments.remove(selectedIndex);
        if (dropPoint.y - dropCell.y < dropCell.height / 2) {
            midiFragmentNames.add(dropIndex, selectedFragmentDescription);
            midiFragments.add(dropIndex, selectedMidiFragment);
            System.out.println("Dropping " + selectedFragmentDescription + " at position " + (dropIndex));
        } else {
            if (dropIndex == midiFragmentNames.size() + 1) {
                midiFragmentNames.add(selectedFragmentDescription);
                midiFragments.add(selectedMidiFragment);
                System.out.println("Dropping " + selectedFragmentDescription + " at position " + (midiFragmentNames.size() - 1));
            } else {
                midiFragmentNames.add(dropIndex, selectedFragmentDescription);
                midiFragments.add(dropIndex, selectedMidiFragment);
                System.out.println("Dropping " + selectedFragmentDescription + " at position " + dropIndex);
            }
        }
        midiFragmentList.clearSelection();
        e.dropComplete(true);
    }

    public void dragEnter(DropTargetDragEvent e) {
    }

    public void dragExit(DropTargetEvent e) {
    }

    public void dragOver(DropTargetDragEvent e) {
        int index = midiFragmentList.locationToIndex(e.getLocation());
        if (index % 2 == 0) {
            midiFragmentList.setSelectedIndex(index);
        }
    }

    public void dropActionChanged(DropTargetDragEvent e) {
    }

    public void valueChanged(ListSelectionEvent e) {
        if (isDragging) {
            midiFragmentList.setSelectedIndex(selectedIndex);
        }
    }

    public void loadMidiFragments() {
        midiFileChooser.rescanCurrentDirectory();
        if (midiFileChooser.showOpenDialog(this) != JFileChooser.CANCEL_OPTION) {
            File file = midiFileChooser.getSelectedFile();
            if (file != null) {
                String s = file.getName();
                if (s.endsWith(".mid")) {
                    try {
                        Sequence sequence = MidiSystem.getSequence(file);
                        if (sequence == null) System.out.println("NULL SEQUENCE"); else {
                            trackArray = sequence.getTracks();
                            midiFragments.clear();
                            midiFragmentNames.clear();
                            for (int i = 0; i < trackArray.length; i++) {
                                Track tempTrack = trackArray[i];
                                if (containsShortMessages(tempTrack)) {
                                    MidiEvent[] tempMidiEvents = new MidiEvent[tempTrack.size()];
                                    for (int j = 0; j < tempTrack.size(); j++) {
                                        tempMidiEvents[j] = tempTrack.get(j);
                                    }
                                    midiFragments.add(tempMidiEvents);
                                    midiFragmentNames.add(new String("[" + i + "] " + file.getName()));
                                }
                            }
                        }
                        changeMidiFormat();
                        midiFragmentList.setListData(midiFragmentNames);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void changeMidiFormat() {
        Iterator iterator = midiFragments.iterator();
        while (iterator.hasNext()) {
            MidiEvent[] tempMidiEvents = (MidiEvent[]) iterator.next();
            for (int i = 0; i < tempMidiEvents.length; i++) {
                if (tempMidiEvents[i].getMessage() instanceof ShortMessage) {
                    ShortMessage message = (ShortMessage) tempMidiEvents[i].getMessage();
                    if (message.getCommand() == ShortMessage.NOTE_ON && message.getData2() == 0) {
                        try {
                            message.setMessage(ShortMessage.NOTE_OFF, message.getChannel(), message.getData1(), 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private boolean containsShortMessages(Track track) {
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMessage() instanceof ShortMessage) return true;
        }
        return false;
    }

    public void printMidiEvents(MidiEvent[] midiEventArray) {
        System.out.println("========================================MIDI EVENTS=======================================");
        System.out.println("Midi event array length = " + midiEventArray.length);
        for (int j = 0; j < midiEventArray.length; j++) {
            if (midiEventArray[j].getMessage() instanceof ShortMessage) {
                System.out.print("Tick: " + midiEventArray[j].getTick());
                ShortMessage sm = (ShortMessage) midiEventArray[j].getMessage();
                switch(sm.getCommand()) {
                    case (ShortMessage.NOTE_ON):
                        System.out.print("; Note On");
                        break;
                    case (ShortMessage.NOTE_OFF):
                        System.out.print("; Note Off");
                        break;
                    default:
                        System.out.print("; Unrecognised");
                }
                System.out.println("; Channel: " + sm.getChannel() + "; Note: " + sm.getData1() + "; Velocity: " + sm.getData2());
            }
        }
        System.out.println();
    }

    /** Returns an XML string with the parameters of the panel */
    public String getXML(String indent) {
        String panelStr = indent + "<agent_panel>";
        panelStr += super.getXML(indent + "\t");
        panelStr += indent + "</agent_panel>";
        return panelStr;
    }

    @Override
    public void loadFromXML(String arg0) throws MASXmlException {
    }
}
