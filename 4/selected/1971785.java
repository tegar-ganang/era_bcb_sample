package view.component;

import javax.swing.*;
import javax.swing.filechooser.*;
import view.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * This class contains the toolbar and all of the toolbar buttons.
 */
public class ToolBar extends JToolBar {

    private JButton newShowButton;

    private JButton newCueButton;

    private JButton newCueSetButton;

    private JButton saveShowButton;

    private JButton loadShowButton;

    private JButton nextCueButton;

    private JButton prevCueButton;

    private JButton connectButton;

    private JButton stopButton;

    private JButton inputButton;

    private JComboBox deviceSelection;

    private JFrame parent;

    private IChannelValueGetter channelValues;

    private ICueAdder cueAdder;

    private ICueInformationGetter infoGetter;

    private IShowLoaderSaver showLoaderSaver;

    private IDeviceConnector deviceConnector;

    private ICueTransitioner cueTransitioner;

    private ICueSetAdder cueSetAdder;

    private IInputDeviceConnector inputDeviceConnector;

    public ToolBar(JFrame _parent, IChannelValueGetter _channelValues, ICueAdder _cueAdder, ICueInformationGetter _infoGetter, IShowLoaderSaver _showLoaderSaver, IDeviceConnector _deviceConnector, ICueTransitioner _cueTransitioner, ICueSetAdder _cueSetAdder, IInputDeviceConnector _inputDeviceConnector) {
        super("The Tool Bar", JToolBar.HORIZONTAL);
        channelValues = _channelValues;
        parent = _parent;
        cueAdder = _cueAdder;
        infoGetter = _infoGetter;
        showLoaderSaver = _showLoaderSaver;
        deviceConnector = _deviceConnector;
        cueTransitioner = _cueTransitioner;
        cueSetAdder = _cueSetAdder;
        inputDeviceConnector = _inputDeviceConnector;
        createGUI();
    }

    private void createGUI() {
        newCueButton = new JButton("New Cue");
        newCueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                CueCreationDialog cueDialog = new CueCreationDialog(parent, cueAdder, infoGetter);
            }
        });
        add(newCueButton);
        newCueSetButton = new JButton("New Cueset");
        newCueSetButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                CueSetCreationDialog cueSetDialog = new CueSetCreationDialog(parent, cueSetAdder);
            }
        });
        add(newCueSetButton);
        add(new JToolBar.Separator());
        nextCueButton = new JButton("Next Cue");
        nextCueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String[] cueNames = infoGetter.getCueNames();
                int index = infoGetter.getCurrentCueIndex();
                if (index < cueNames.length - 1) cueTransitioner.setCurrentCueIndex(index + 1);
            }
        });
        add(nextCueButton);
        prevCueButton = new JButton("Prev Cue");
        prevCueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String[] cueNames = infoGetter.getCueNames();
                int index = infoGetter.getCurrentCueIndex();
                if (index > 0) cueTransitioner.setCurrentCueIndex(index - 1);
            }
        });
        add(prevCueButton);
        stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cueTransitioner.stopTransition();
            }
        });
        add(stopButton);
        stopButton.setEnabled(false);
        add(new JToolBar.Separator());
        newShowButton = new JButton("New Show");
        newShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (showLoaderSaver.getFileName().equals("") == false) {
                    int result = JOptionPane.showConfirmDialog(parent, "Do you want to save " + showLoaderSaver.getFileName() + "?", "Save Existing Show?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.YES_OPTION) {
                        showLoaderSaver.saveShow();
                        showLoaderSaver.newShow();
                    } else if (result == JOptionPane.NO_OPTION) showLoaderSaver.newShow();
                } else showLoaderSaver.newShow();
            }
        });
        add(newShowButton);
        saveShowButton = new JButton("Save Show");
        saveShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveShow();
            }
        });
        add(saveShowButton);
        loadShowButton = new JButton("Load Show");
        loadShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new ShowFileFilter());
                chooser.setMultiSelectionEnabled(false);
                int returnVal = chooser.showOpenDialog(parent);
                if (returnVal == JFileChooser.APPROVE_OPTION) showLoaderSaver.loadShow(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        add(loadShowButton);
        add(new JToolBar.Separator());
        connectButton = new JButton("Connect to Device");
        connectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (connectButton.getText().equals("Connect to Device")) {
                    if (deviceConnector.connect((String) deviceSelection.getSelectedItem())) {
                        connectButton.setText("Disconnect from Device");
                        deviceSelection.setEnabled(false);
                    }
                } else {
                    deviceConnector.disconnect();
                    connectButton.setText("Connect to Device");
                    deviceSelection.setEnabled(true);
                }
            }
        });
        add(connectButton);
        deviceSelection = new JComboBox(deviceConnector.getDevices());
        add(deviceSelection);
        inputButton = new JButton("Enable Network Input");
        inputButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (inputButton.getText().equals("Enable Network Input")) {
                    inputButton.setText("Disable Network Input");
                    inputDeviceConnector.enableNetworkInput();
                } else {
                    inputDeviceConnector.disableNetworkInput();
                    inputButton.setText("Enable Network Input");
                }
            }
        });
        add(inputButton);
    }

    /**
   * This method will notify the view if the device has disconnected without the view specifically
   * telling it to disconnect.
   */
    public void deviceDisconnected() {
        connectButton.setText("Connect to Device");
    }

    public void toggleDataEntry(boolean enabled) {
        newCueButton.setEnabled(enabled);
        newCueSetButton.setEnabled(enabled);
        saveShowButton.setEnabled(enabled);
        loadShowButton.setEnabled(enabled);
        nextCueButton.setEnabled(enabled);
        prevCueButton.setEnabled(enabled);
        connectButton.setEnabled(enabled);
        newShowButton.setEnabled(enabled);
        stopButton.setEnabled(!enabled);
    }

    private class ShowFileFilter extends javax.swing.filechooser.FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            if (f.getName().lastIndexOf('.') != -1) {
                String s = f.getName().substring(f.getName().lastIndexOf('.'));
                if (s.equalsIgnoreCase(".shw")) return true;
            }
            return false;
        }

        public String getDescription() {
            return "Show Files (.shw)";
        }
    }

    private String addFileExtension(String filename) {
        if (filename.lastIndexOf('.') != -1) {
            if (filename.substring(filename.lastIndexOf('.')).equalsIgnoreCase(".shw") == false) return filename + ".shw"; else return filename;
        } else return filename + ".shw";
    }

    public void saveShow() {
        if (showLoaderSaver.getFileName().equals("")) {
            JFileChooser chooser = new JFileChooser();
            String filename = "";
            chooser.setFileFilter(new ShowFileFilter());
            chooser.setMultiSelectionEnabled(false);
            do {
                if (filename.equals("") == false) chooser.setSelectedFile(new File(filename));
                int returnVal = chooser.showSaveDialog(parent);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    filename = addFileExtension(chooser.getSelectedFile().getAbsolutePath());
                    if (chooser.getSelectedFile().exists()) {
                        int result = JOptionPane.showConfirmDialog(parent, filename + " already exists.  Do you want to replace this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (result == JOptionPane.YES_OPTION) {
                            showLoaderSaver.saveShow(filename);
                            break;
                        }
                    } else {
                        showLoaderSaver.saveShow(filename);
                        break;
                    }
                } else {
                    break;
                }
            } while (true);
        } else {
            showLoaderSaver.saveShow();
        }
    }
}
