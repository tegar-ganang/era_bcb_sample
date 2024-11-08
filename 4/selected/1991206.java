package org.rdv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rdv.DataViewer;
import org.rdv.rbnb.Channel;
import org.rdv.rbnb.EventMarker;
import org.rdv.rbnb.ProgressListener;
import org.rdv.rbnb.RBNBController;
import org.rdv.rbnb.RBNBExport;
import org.rdv.util.ReadableStringComparator;

public class ExportVideoDialog extends JDialog implements ProgressListener {

    /** serialization version identifier */
    private static final long serialVersionUID = -5129293655952117325L;

    static Log log = org.rdv.LogFactory.getLog(ExportVideoDialog.class.getName());

    ExportVideoDialog dialog;

    RBNBController rbnb;

    RBNBExport export;

    boolean exporting;

    JButton startTimeButton;

    JLabel durationLabel;

    JButton endTimeButton;

    TimeSlider timeSlider;

    JList videoChannelList;

    DefaultListModel videoChannelModel;

    JTextField directoryTextField;

    JFileChooser directoryChooser;

    JButton directoryButton;

    JProgressBar exportProgressBar;

    JButton exportButton;

    JButton cancelButton;

    List<String> channels;

    public ExportVideoDialog(JFrame owner, RBNBController rbnb, List<String> channels) {
        super(owner);
        this.dialog = this;
        this.rbnb = rbnb;
        this.channels = channels;
        Collections.sort(channels, new ReadableStringComparator());
        export = new RBNBExport(rbnb.getRBNBHostName(), rbnb.getRBNBPortNumber());
        exporting = false;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle("Export video to disk");
        setChannelModel();
        initComponents();
    }

    private void setChannelModel() {
        videoChannelModel = new DefaultListModel();
        for (int i = 0; i < channels.size(); i++) {
            String channelName = (String) channels.get(i);
            Channel channel = rbnb.getChannel(channelName);
            String mime = channel.getMetadata("mime");
            if (mime.equals("image/jpeg") || mime.equals("video/jpeg")) {
                videoChannelModel.addElement(new ExportChannel(channelName));
            }
        }
    }

    private void initComponents() {
        JPanel container = new JPanel();
        setContentPane(container);
        InputMap inputMap = container.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = container.getActionMap();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.ipadx = 0;
        c.ipady = 0;
        JLabel headerLabel = new JLabel("Select the time range and video channels to export.");
        headerLabel.setBackground(Color.white);
        headerLabel.setOpaque(true);
        headerLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new java.awt.Insets(0, 0, 0, 0);
        container.add(headerLabel, c);
        JPanel timeButtonPanel = new JPanel();
        timeButtonPanel.setLayout(new BorderLayout());
        MouseListener hoverMouseListener = new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                e.getComponent().setForeground(Color.red);
            }

            public void mouseExited(MouseEvent e) {
                e.getComponent().setForeground(Color.blue);
            }
        };
        startTimeButton = new JButton();
        startTimeButton.setBorder(null);
        startTimeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startTimeButton.setForeground(Color.blue);
        startTimeButton.addMouseListener(hoverMouseListener);
        startTimeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                double startTime = DateTimeDialog.showDialog(ExportVideoDialog.this, timeSlider.getStart(), timeSlider.getMinimum(), timeSlider.getEnd());
                if (startTime >= 0) {
                    timeSlider.setStart(startTime);
                }
            }
        });
        timeButtonPanel.add(startTimeButton, BorderLayout.WEST);
        durationLabel = new JLabel();
        durationLabel.setHorizontalAlignment(JLabel.CENTER);
        timeButtonPanel.add(durationLabel, BorderLayout.CENTER);
        endTimeButton = new JButton();
        endTimeButton.setBorder(null);
        endTimeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        endTimeButton.setForeground(Color.blue);
        endTimeButton.addMouseListener(hoverMouseListener);
        endTimeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                double endTime = DateTimeDialog.showDialog(ExportVideoDialog.this, timeSlider.getEnd(), timeSlider.getStart(), timeSlider.getMaximum());
                if (endTime >= 0) {
                    timeSlider.setEnd(endTime);
                }
            }
        });
        timeButtonPanel.add(endTimeButton, BorderLayout.EAST);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new java.awt.Insets(10, 10, 10, 10);
        container.add(timeButtonPanel, c);
        timeSlider = new TimeSlider();
        timeSlider.setValueChangeable(false);
        timeSlider.setValueVisible(false);
        timeSlider.addTimeAdjustmentListener(new TimeAdjustmentListener() {

            public void timeChanged(TimeEvent event) {
            }

            public void rangeChanged(TimeEvent event) {
                updateTimeRangeLabel();
            }

            public void boundsChanged(TimeEvent event) {
            }
        });
        updateTimeRangeLabel();
        updateTimeBounds();
        List<EventMarker> markers = rbnb.getMarkerManager().getMarkers();
        for (EventMarker marker : markers) {
            timeSlider.addMarker(marker);
        }
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new java.awt.Insets(0, 10, 10, 10);
        container.add(timeSlider, c);
        JLabel numericHeaderLabel = new JLabel("Video Channels:");
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new java.awt.Insets(0, 10, 10, 10);
        container.add(numericHeaderLabel, c);
        videoChannelList = new JList(videoChannelModel);
        videoChannelList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        videoChannelList.setCellRenderer(new CheckListRenderer());
        videoChannelList.setVisibleRowCount(10);
        videoChannelList.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                int index = videoChannelList.locationToIndex(e.getPoint());
                ExportChannel item = (ExportChannel) videoChannelList.getModel().getElementAt(index);
                item.setSelected(!item.isSelected());
                Rectangle rect = videoChannelList.getCellBounds(index, index);
                videoChannelList.repaint(rect);
                updateTimeBounds();
            }
        });
        JScrollPane scrollPane = new JScrollPane(videoChannelList);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.insets = new java.awt.Insets(0, 10, 10, 10);
        container.add(scrollPane, c);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new java.awt.Insets(0, 10, 10, 5);
        container.add(new JLabel("Choose Directory: "), c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        directoryTextField = new JTextField(20);
        c.insets = new java.awt.Insets(0, 0, 10, 5);
        container.add(directoryTextField, c);
        directoryChooser = new JFileChooser();
        directoryChooser.setDialogTitle("Select export directory");
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryTextField.setText(directoryChooser.getCurrentDirectory().getAbsolutePath());
        directoryButton = new JButton("Browse");
        directoryButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                int status = directoryChooser.showOpenDialog(null);
                if (status == JFileChooser.APPROVE_OPTION) {
                    directoryTextField.setText(directoryChooser.getSelectedFile().getAbsolutePath());
                }
            }
        });
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.gridx = 2;
        c.gridy = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new java.awt.Insets(0, 0, 10, 10);
        container.add(directoryButton, c);
        exportProgressBar = new JProgressBar(0, 100000);
        exportProgressBar.setStringPainted(true);
        exportProgressBar.setValue(0);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new java.awt.Insets(0, 10, 10, 10);
        container.add(exportProgressBar, c);
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        Action exportAction = new AbstractAction() {

            private static final long serialVersionUID = 1547500154252213911L;

            public void actionPerformed(ActionEvent e) {
                exportVideo();
            }
        };
        exportAction.putValue(Action.NAME, "Export");
        inputMap.put(KeyStroke.getKeyStroke("ENTER"), "export");
        actionMap.put("export", exportAction);
        exportButton = new JButton(exportAction);
        panel.add(exportButton);
        Action cancelAction = new AbstractAction() {

            private static final long serialVersionUID = -7440298547807878651L;

            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        };
        cancelAction.putValue(Action.NAME, "Cancel");
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        actionMap.put("cancel", cancelAction);
        cancelButton = new JButton(cancelAction);
        panel.add(cancelButton);
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 7;
        c.gridwidth = GridBagConstraints.REMAINDER;
        ;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new java.awt.Insets(0, 0, 10, 5);
        container.add(panel, c);
        pack();
        if (getWidth() < 600) {
            setSize(600, getHeight());
        }
        directoryTextField.requestFocusInWindow();
        setLocationByPlatform(true);
        setVisible(true);
    }

    private void disableUI() {
        startTimeButton.setEnabled(false);
        endTimeButton.setEnabled(false);
        videoChannelList.setEnabled(false);
        directoryTextField.setEnabled(false);
        directoryButton.setEnabled(false);
        exportButton.setEnabled(false);
    }

    private void enableUI() {
        startTimeButton.setEnabled(true);
        endTimeButton.setEnabled(true);
        videoChannelList.setEnabled(true);
        directoryTextField.setEnabled(true);
        directoryButton.setEnabled(true);
        exportButton.setEnabled(true);
    }

    private void updateTimeBounds() {
        List<String> selectedChannels = getSelectedChannels();
        if (selectedChannels.size() == 0) {
            return;
        }
        double minimum = Double.MAX_VALUE;
        double maximum = 0;
        for (String channelName : selectedChannels) {
            Channel channel = rbnb.getChannel(channelName);
            if (channel == null) {
                continue;
            }
            double channelStart = Double.parseDouble(channel.getMetadata("start"));
            double channelDuration = Double.parseDouble(channel.getMetadata("duration"));
            double channelEnd = channelStart + channelDuration;
            if (channelStart < minimum) {
                minimum = channelStart;
            }
            if (channelEnd > maximum) {
                maximum = channelEnd;
            }
        }
        timeSlider.setValues(minimum, maximum);
    }

    private void updateTimeRangeLabel() {
        double start = timeSlider.getStart();
        double end = timeSlider.getEnd();
        double duration = end - start;
        startTimeButton.setText(DataViewer.formatDate(start));
        durationLabel.setText(DataViewer.formatSeconds(duration));
        endTimeButton.setText(DataViewer.formatDate(end));
    }

    private List<String> getSelectedChannels() {
        List<String> selectedChannels = new ArrayList<String>();
        for (int i = 0; i < videoChannelModel.size(); i++) {
            ExportChannel channel = (ExportChannel) videoChannelModel.get(i);
            if (channel.isSelected()) {
                selectedChannels.add(channel.toString());
            }
        }
        return selectedChannels;
    }

    private boolean createDirectory(File directory) {
        File file = new File(directory.getPath());
        return file.mkdirs();
    }

    private boolean checkDirectory(File directory) {
        if (directory.exists()) {
            if (directory.isDirectory()) {
                int overwriteReturn = JOptionPane.showConfirmDialog(null, directory.getName() + " already exists.\nResume writing in the same folder?", "Write into existing directory?", JOptionPane.YES_NO_OPTION);
                if (overwriteReturn == JOptionPane.NO_OPTION) {
                    return false;
                }
            } else {
                JOptionPane.showMessageDialog(null, "Could not create directory " + directory.getPath() + "\nA file with that name already exists!", "Create Directory error", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } else {
            int createDir = JOptionPane.showConfirmDialog(null, directory.getPath() + " does NOT exist. Directory will be created", "Create new directory?", JOptionPane.YES_NO_OPTION);
            if (createDir == JOptionPane.YES_OPTION) {
                if (!createDirectory(directory)) {
                    JOptionPane.showMessageDialog(null, "Error: Could not create directory " + directory.getPath(), "Create Directory error", JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    private void exportVideo() {
        List<String> selectedChannels = getSelectedChannels();
        if (selectedChannels.size() == 0) {
            JOptionPane.showMessageDialog(this, "No video channels selected!", "Export Video channels", JOptionPane.ERROR_MESSAGE);
            return;
        }
        File directory = new File(directoryTextField.getText());
        if (!checkDirectory(directory)) {
            return;
        }
        disableUI();
        double start = timeSlider.getStart();
        double end = timeSlider.getEnd();
        if (start == end) {
            JOptionPane.showMessageDialog(this, "The start and end export time must not be the same.", "Export Video Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        exporting = true;
        export.startExport(null, null, selectedChannels, directory, start, end, this);
    }

    private void cancel() {
        if (exporting) {
            export.cancelExport();
        } else {
            dispose();
        }
    }

    public void postProgress(double progress) {
        exportProgressBar.setValue((int) (progress * 100000));
    }

    public void postCompletion() {
        exporting = false;
        dispose();
        JOptionPane.showMessageDialog(this, "Export complete.", "Export complete", JOptionPane.INFORMATION_MESSAGE);
    }

    public void postError(String errorMessage) {
        exportProgressBar.setValue(0);
        exporting = false;
        enableUI();
        JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    class ExportChannel {

        String channelName;

        boolean selected;

        public ExportChannel(String channelName) {
            this.channelName = channelName;
            selected = true;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String toString() {
            return channelName;
        }
    }

    class CheckListRenderer extends JCheckBox implements ListCellRenderer {

        /** serialization version identifier */
        private static final long serialVersionUID = 215713751613298905L;

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            setEnabled(list.isEnabled());
            setSelected(((ExportChannel) value).isSelected());
            if (index % 2 == 0) {
                setBackground(UIManager.getColor("List.textBackground"));
            } else {
                setBackground(new Color(230, 230, 230));
            }
            setForeground(UIManager.getColor("List.textForeground"));
            setFont(list.getFont());
            setText(value.toString());
            return this;
        }
    }
}
