package gov.sns.apps.mpsinputtest;

import java.util.Map;
import java.util.List;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import gov.sns.ca.*;

public class TestMagPSPanel extends JDialog implements CAValueListener {

    public static final int WAIT_TICS = 10;

    private JPanel basePanel, magnetPane3, MsgPanel;

    private JPanel chanStatPanel, inputStatPanel, magStatPanel;

    public static List _channelWrappers, MPSmagWrap;

    public static int newCWvalue;

    private JProgressBar resetPB, startPB, stopPB, progressbar;

    private LongTask task;

    private Timer timer;

    private static ChannelWrapper wrapper, ccmdWrapper, chStWrapper;

    private JLabel PFresetLabel = new JLabel();

    private JLabel PFstartLabel = new JLabel();

    public static Map MPSmagMap = new LinkedHashMap();

    private JLabel PFstopLabel = new JLabel();

    private JLabel MagStatLabel;

    private static String FailedStatus = "<html><font align=center color=red>******************** FAILED!</font></html>";

    private JButton startButton, stopButton, resetButton, logButton, closeButton;

    public static JLabel[][] MEBTmagLabels = { { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() } };

    public TestMagPSPanel() {
        JPanel readyPane = new JPanel();
    }

    public static TestMagPSPanel showPanel(Frame frame, Component locationComp, Object[][] data, String title, String longValue, String labelText, String selTwo) {
        TestMagPSPanel newPanel = new TestMagPSPanel(frame, locationComp, title, data, longValue, labelText, selTwo);
        newPanel.setVisible(true);
        return newPanel;
    }

    private TestMagPSPanel(Frame frame, Component locationComp, String title, Object[][] data, final String longValue, final String labelText, String selTwo) {
        super(frame, title, true);
        if (_channelWrappers == null) _channelWrappers = new ArrayList();
        if (MPSmagWrap == null) createChannelWrappers(labelText);
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(labelText);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.setPreferredSize(new Dimension(300, 250));
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 30)));
        chanStatPanel = new JPanel();
        chanStatPanel.setLayout(new BoxLayout(chanStatPanel, BoxLayout.LINE_AXIS));
        chanStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        chanStatPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        MEBTmagLabels[0][0] = new JLabel("FPL_LDmp_chan_status ");
        chanStatPanel.add(MEBTmagLabels[0][0]);
        MEBTmagLabels[0][1] = new JLabel(String.valueOf(getChanStatValue()));
        chanStatPanel.add(MEBTmagLabels[0][1]);
        inputStatPanel = new JPanel();
        inputStatPanel.setLayout(new BoxLayout(inputStatPanel, BoxLayout.LINE_AXIS));
        inputStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        MEBTmagLabels[1][0] = new JLabel("FPL_LDmp_input_status ");
        inputStatPanel.add(MEBTmagLabels[1][0]);
        MEBTmagLabels[1][1] = new JLabel(getInputStatLabel());
        inputStatPanel.add(MEBTmagLabels[1][1]);
        inputStatPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.add(chanStatPanel);
        listPane.add(inputStatPanel);
        putMagLabels(MEBTmagLabels);
        String stat = getCCmdLabel();
        MagStatLabel = new JLabel("Magnet Status is " + stat);
        magStatPanel = new JPanel();
        magStatPanel.setLayout(new BoxLayout(magStatPanel, BoxLayout.LINE_AXIS));
        magStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        magStatPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        magStatPanel.add(MagStatLabel);
        listPane.add(magStatPanel);
        task = new LongTask(wrapper);
        JPanel readyPane = new JPanel();
        readyPane.setLayout(new FlowLayout());
        String srcChain = longValue + " " + selTwo;
        JLabel testLabel = new JLabel(srcChain);
        readyPane.add(testLabel);
        if (longValue.equals("MEBT_BS")) startButton = new JButton("Start"); else if (longValue.equals("CCL_BS") || longValue.equals("LDmp")) startButton = new JButton("On"); else startButton = new JButton("");
        if (getCCmdValue() == 0 && getChanStatValue() == 1) startButton.setVisible(false); else startButton.setVisible(true);
        startButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (getCCmdValue() == 0 && getChanStatValue() == 1) {
                    stopButton.setEnabled(false);
                    ;
                    return;
                }
                PFstopLabel.setText("");
                PFstopLabel.setVisible(false);
                PFstopLabel.validate();
                MsgPanel.validate();
                try {
                    ccmdWrapper.getChannel().putVal(2);
                } catch (ConnectionException e) {
                    System.err.println("Unable to connect to channel access.");
                } catch (PutException e) {
                    System.err.println("Unable to set process variables.");
                }
                try {
                    ccmdWrapper.getChannel().putVal(0);
                    if ((getCCmdLabel().equals("ON") && getChanStatValue() != 1) || getCCmdLabel().equals("?")) {
                        PFstopLabel.setHorizontalTextPosition(JLabel.CENTER);
                        PFstopLabel.setText(FailedStatus);
                    } else PFstopLabel.setText("Passed");
                    PFstopLabel.validate();
                    MsgPanel.validate();
                } catch (ConnectionException e) {
                    System.err.println("Unable to connect to channel access.");
                } catch (PutException e) {
                    System.err.println("Unable to set process variables.");
                }
                task.go(1);
                timer.start();
            }
        });
        if (longValue.equals("MEBT_BS")) stopButton = new JButton("Stop"); else if (longValue.equals("CCL_BS") || longValue.equals("LDmp")) stopButton = new JButton("Off"); else stopButton = new JButton("");
        stopPB = new JProgressBar(0, task.getLengthOfTask());
        stopPB.setValue(0);
        stopPB.setStringPainted(true);
        stopPB.setVisible(false);
        MsgPanel = new JPanel();
        MsgPanel.setLayout(new BoxLayout(MsgPanel, BoxLayout.LINE_AXIS));
        MsgPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        MsgPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        MsgPanel.add(PFstopLabel);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(MsgPanel);
        stopButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (getCCmdValue() == 1 && getChanStatValue() == 0) return;
                PFstopLabel.setText("");
                PFstopLabel.setVisible(true);
                PFstopLabel.validate();
                MsgPanel.validate();
                try {
                    ccmdWrapper.getChannel().putVal(1);
                } catch (ConnectionException e) {
                    System.err.println("Unable to connect to channel access.");
                } catch (PutException e) {
                    System.err.println("Unable to set process variables.");
                }
                task.go(0);
                timer.start();
            }
        });
        if (longValue.equals("LDmp")) resetButton = new JButton("Reset"); else if (longValue.equals("CCL_BS")) resetButton = new JButton("Standby"); else resetButton = new JButton("");
        resetPB = new JProgressBar(0, task.getLengthOfTask());
        resetPB.setValue(0);
        resetPB.setStringPainted(true);
        resetPB.setVisible(false);
        resetButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PFresetLabel.setVisible(false);
                resetPB.setVisible(true);
                task.go(1);
                timer.start();
            }
        });
        logButton = new JButton("Log Report");
        logButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
                String TestResult = PFstopLabel.getText();
                if (TestResult.length() > 0 && !TestResult.equals("Passed")) TestResult = "Failed";
                getMainWindow().publishMPSinputTestResultsToLogbook(labelText, TestResult, longValue);
            }
        });
        closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        buttonPane.add(logButton);
        buttonPane.add(Box.createRigidArea(new Dimension(0, 30)));
        buttonPane.add(closeButton);
        buttonPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        timer = new Timer(WAIT_TICS, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                String Mstat = getCCmdLabel();
                progressbar = stopPB;
                if (Mstat.equals("ON") && getChanStatValue() == 1) {
                    progressbar = stopPB;
                    progressbar.setValue(task.getCurrent());
                } else if (Mstat.equals("OFF") && getChanStatValue() == 1) stopButton.setEnabled(false); else if (Mstat.equals("OFF") && getChanStatValue() == 0) stopButton.setEnabled(true);
                checkTask();
            }
        });
        JPanel magnetPane = new JPanel();
        magnetPane.setLayout(new BoxLayout(magnetPane, BoxLayout.LINE_AXIS));
        magnetPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        magnetPane.add(Box.createRigidArea(new Dimension(10, 0)));
        JPanel magnetPane2 = new JPanel();
        magnetPane2.setLayout(new BoxLayout(magnetPane2, BoxLayout.LINE_AXIS));
        magnetPane2.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        magnetPane2.add(Box.createRigidArea(new Dimension(10, 0)));
        label = new JLabel("Turn Magnet On");
        magnetPane2.add(label);
        magnetPane2.add(Box.createRigidArea(new Dimension(10, 0)));
        magnetPane2.add(startButton);
        magnetPane2.add(Box.createRigidArea(new Dimension(10, 0)));
        magnetPane3 = new JPanel();
        magnetPane3.setLayout(new BoxLayout(magnetPane3, BoxLayout.LINE_AXIS));
        magnetPane3.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        magnetPane3.add(Box.createRigidArea(new Dimension(10, 0)));
        label = new JLabel("Turn Magnet Off");
        magnetPane3.add(label);
        magnetPane3.add(Box.createRigidArea(new Dimension(10, 0)));
        magnetPane3.add(stopButton);
        magnetPane3.add(Box.createRigidArea(new Dimension(10, 0)));
        magnetPane3.add(stopPB);
        magnetPane3.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.add(magnetPane);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(magnetPane2);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(magnetPane3);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Container contentPane = getContentPane();
        contentPane.add(readyPane, BorderLayout.PAGE_START);
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(locationComp);
    }

    private void putMagLabels(JLabel[][] PSLabels) {
        getMainWindow().putMagLabels(PSLabels);
    }

    public MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }

    public void createChannelWrappers(String initPv) {
        String[] chNames = { "", "", "", "", "" };
        MPSmagWrap = new ArrayList();
        String chName = initPv + ":FltS";
        wrapper = new ChannelWrapper(chName);
        MPSmagWrap.add(wrapper);
        wrapper.addCAValueListener(this);
        chNames[0] = chName;
        String str;
        str = "" + wrapper.getValue();
        MPSmagMap.put(chNames[0], str);
        chName = initPv + ":FPL_LDmp_cable_status";
        wrapper = new ChannelWrapper(chName);
        MPSmagWrap.add(wrapper);
        wrapper.addCAValueListener(this);
        chNames[1] = chName;
        str = "" + wrapper.getValue();
        MPSmagMap.put(chNames[1], str);
        chName = initPv + ":FPL_LDmp_chan_status";
        wrapper = new ChannelWrapper(chName);
        MPSmagWrap.add(wrapper);
        chStWrapper = wrapper;
        wrapper.addCAValueListener(this);
        chNames[2] = chName;
        str = "" + wrapper.getValue();
        MPSmagMap.put(chNames[2], str);
        chName = initPv + ":FPL_LDmp_input_status";
        wrapper = new ChannelWrapper(chName);
        MPSmagWrap.add(wrapper);
        wrapper.addCAValueListener(this);
        chNames[3] = chName;
        str = "" + wrapper.getValue();
        MPSmagMap.put(chNames[3], str);
        chName = initPv + ":CCmd";
        wrapper = new ChannelWrapper(chName);
        ccmdWrapper = wrapper;
        MPSmagWrap.add(wrapper);
        wrapper.addCAValueListener(this);
        chNames[4] = chName;
        str = "" + wrapper.getValue();
        MPSmagMap.put(chNames[4], str);
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(final ChannelWrapper wrapper, int value) {
        value = wrapper.getValue();
        newCWvalue = value;
        final String chName = wrapper.getName();
        final Runnable updateLabels = new Runnable() {

            public void run() {
                int index = chName.lastIndexOf("FPL_LDmp_chan_status");
                if (index > -1) {
                    MEBTmagLabels[0][1].setText(String.valueOf(getChanStatValue()));
                }
                index = chName.lastIndexOf("FPL_LDmp_input_status");
                if (index > -1) {
                    MEBTmagLabels[1][1].setText(getInputStatLabel());
                }
                index = chName.lastIndexOf(":CCmd");
                if (index > -1) {
                    if (MagStatLabel == null) MagStatLabel = new JLabel();
                    MagStatLabel.setText("Magnet Status is " + getCCmdLabel());
                    MagStatLabel.validate();
                }
            }
        };
        Thread appThread = new Thread() {

            @Override
            public void run() {
                try {
                    SwingUtilities.invokeAndWait(updateLabels);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        appThread.start();
    }

    private String getCCmdLabel() {
        int val = getCCmdValue();
        int chstat = getChanStatValue();
        String stat = "";
        if (val == 0 && chstat == 1) stat = "ON"; else if (val == 1 && chstat == 0) stat = "OFF"; else if (val == 2) stat = "STANDBY"; else stat = "?";
        return stat;
    }

    private int getCCmdValue() {
        return ccmdWrapper.getValue();
    }

    private int getChanStatValue() {
        return chStWrapper.getValue();
    }

    private String getInputStatLabel() {
        int val = getInputStatValue();
        String stat = "";
        if (val == 0) stat = "FAULT"; else if (val == 1) stat = "OK";
        return stat;
    }

    private int getInputStatValue() {
        return wrapper.getValue();
    }

    private void checkTask() {
        if (task.isDone()) {
            String Mstat = getCCmdLabel();
            Toolkit.getDefaultToolkit().beep();
            timer.stop();
            if ((Mstat.equals("ON") && getChanStatValue() != 1) || (Mstat.equals("OFF") && getChanStatValue() != 0) || Mstat.equals("?")) {
                PFstopLabel.setHorizontalTextPosition(JLabel.CENTER);
                PFstopLabel.setText(FailedStatus);
            } else PFstopLabel.setText("Passed");
            PFstopLabel.setVisible(true);
            PFstopLabel.validate();
            MsgPanel.validate();
            progressbar.setVisible(false);
            setCursor(null);
            progressbar.setValue(progressbar.getMinimum());
            MagStatLabel.setText("Magnet Status is " + Mstat);
            MagStatLabel.validate();
            MEBTmagLabels[0][1].setText(String.valueOf(getChanStatValue()));
            MEBTmagLabels[0][1].validate();
            MEBTmagLabels[1][1].setText(getInputStatLabel());
            MEBTmagLabels[1][1].validate();
            if (!Mstat.equals("ON") && getChanStatValue() == 0) startButton.setEnabled(true); else if (Mstat.equals("ON") && getChanStatValue() == 1) startButton.setEnabled(false); else if (Mstat.equals("OFF") && getChanStatValue() == 1) stopButton.setEnabled(false); else if (Mstat.equals("OFF") && getChanStatValue() == 0) stopButton.setEnabled(true);
        }
    }

    public Map getRdyToTestMPSmag(String initPv) {
        List _channelWrappers = new ArrayList(300);
        if (MPSmagWrap == null) createChannelWrappers(initPv);
        return MPSmagMap;
    }

    public List getMPSmagWrap(String pv) {
        return MPSmagWrap;
    }
}
