package gov.sns.apps.mpsinputtest;

import java.util.Map;
import java.util.List;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import gov.sns.ca.*;

public class TestBLMPanel extends JDialog implements CAValueListener {

    public static final int WAIT_TICS = 10;

    private JPanel basePanel, chanStatPanel = new JPanel(), blmPane3, MsgPanel, blmPane = new JPanel();

    private JPanel inputStatPanel = new JPanel(), blmStatPanel = new JPanel(), blmPane2 = new JPanel();

    private JPanel msgPane = new JPanel();

    public static List _channelWrappers, MPSblmWrap;

    private JProgressBar resetPB, startPB, stopPB, progressbar;

    private LongTask task;

    private Timer timer;

    private static ChannelWrapper wrapper, hvrbWrapper, inStWrapper, hvWrapper;

    private static ChannelWrapper pllWrapper, cspWrapper, tdWrapper, chStWrapper;

    private JLabel PFstartLabel = new JLabel();

    public static Map MPSblmMap = new LinkedHashMap();

    private static int IsStarted = 0, initInSt = 0, oldInSt = -1;

    private static int InSt1 = -1, InSt2 = -1, InSt3 = -1, initChSt = 1;

    private static JLabel PFstopLabel = new JLabel();

    private static String FailedStatus = "<html><font align=center color=red>FAILED!</font></html>";

    private JButton BLMtestButton, logButton, closeButton, resetButton;

    public static JLabel[][] BLMLabels = { { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() } };

    private static float initPLL;

    private static String TestResult = "";

    private static String chainSel = "";

    public TestBLMPanel() {
        JPanel readyPane = new JPanel();
    }

    public static TestBLMPanel showPanel(Frame frame, Object[][] data, Component locationComp, String title, String lText, String initValue, final String longValue) {
        TestBLMPanel newPanel = new TestBLMPanel(frame, locationComp, data, title, lText, initValue, longValue);
        newPanel.setVisible(true);
        return newPanel;
    }

    private TestBLMPanel(Frame frame, Component locationComp, Object[][] data, String title, final String lText, String initValue, final String longValue) {
        super(frame, title, true);
        PFstopLabel.setText("");
        PFstopLabel.validate();
        chainSel = initValue;
        if (_channelWrappers == null) _channelWrappers = new ArrayList();
        createChannelWrappers(lText);
        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        JLabel label = new JLabel(lText);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.setPreferredSize(new Dimension(300, 250));
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 30)));
        chanStatPanel.setLayout(new BoxLayout(chanStatPanel, BoxLayout.LINE_AXIS));
        chanStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        chanStatPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        BLMLabels[0][0] = new JLabel("FPAR_" + chainSel + "_input_status ");
        chanStatPanel.add(BLMLabels[0][0]);
        BLMLabels[0][1] = new JLabel(String.valueOf(getInputStatValue()));
        chanStatPanel.add(BLMLabels[0][1]);
        inputStatPanel.setLayout(new BoxLayout(inputStatPanel, BoxLayout.LINE_AXIS));
        inputStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        BLMLabels[1][0] = new JLabel("DbgHVBias ");
        inputStatPanel.add(BLMLabels[1][0]);
        BLMLabels[1][1] = new JLabel(getHVBiasLabel());
        inputStatPanel.add(BLMLabels[1][1]);
        inputStatPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.add(chanStatPanel);
        listPane.add(inputStatPanel);
        putBLMLabels(BLMLabels);
        blmStatPanel.setLayout(new BoxLayout(blmStatPanel, BoxLayout.LINE_AXIS));
        blmStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        blmStatPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        BLMLabels[2][0] = new JLabel("DbgHVBiasRb ");
        blmStatPanel.add(BLMLabels[2][0]);
        BLMLabels[2][1] = new JLabel(getHVRbLabel());
        blmStatPanel.add(BLMLabels[2][1]);
        blmStatPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.add(blmStatPanel);
        task = new LongTask(inStWrapper);
        JPanel readyPane = new JPanel();
        readyPane.setLayout(new FlowLayout());
        String srcChain = initValue + " " + longValue;
        JLabel testLabel = new JLabel(srcChain);
        readyPane.add(testLabel);
        BLMtestButton = new JButton("Start Test");
        BLMtestButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                final Runnable updateLabels = new Runnable() {

                    public void run() {
                        testBLM(lText);
                    }
                };
                Thread appThread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            SwingUtilities.invokeLater(updateLabels);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                appThread.start();
            }
        });
        resetButton = new JButton("Reset");
        resetButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        resetButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                IsStarted = 0;
                InSt1 = -1;
                InSt2 = -1;
                InSt3 = -1;
                PFstopLabel.setText("");
                PFstopLabel.validate();
                msgPane.validate();
            }
        });
        logButton = new JButton("Log Report");
        logButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
                if (TestResult.length() > 0 && !TestResult.equals("Passed")) TestResult = "Failed";
                getMainWindow().publishMPSinputTestResultsToLogbook(lText, TestResult, longValue);
            }
        });
        closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
                timer.stop();
            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        buttonPane.add(logButton);
        buttonPane.add(Box.createRigidArea(new Dimension(0, 30)));
        buttonPane.add(resetButton);
        buttonPane.add(closeButton);
        buttonPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgPane.setLayout(new BoxLayout(msgPane, BoxLayout.Y_AXIS));
        msgPane.add(PFstopLabel);
        msgPane.add(buttonPane);
        timer = new Timer(WAIT_TICS, new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                timer.setRepeats(false);
                checkTask();
            }
        });
        blmPane2.setLayout(new BoxLayout(blmPane2, BoxLayout.LINE_AXIS));
        blmPane2.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        BLMLabels[3][0] = new JLabel("DbgMPSPulseLossLimit ");
        blmPane2.add(BLMLabels[3][0]);
        BLMLabels[3][1] = new JLabel(getPllLabel());
        blmPane2.add(BLMLabels[3][1]);
        blmPane2.setAlignmentX(Component.CENTER_ALIGNMENT);
        blmPane3 = new JPanel();
        blmPane3.setLayout(new BoxLayout(blmPane3, BoxLayout.LINE_AXIS));
        blmPane3.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        blmPane3.add(Box.createRigidArea(new Dimension(10, 0)));
        blmPane3.add(BLMtestButton);
        blmPane3.setAlignmentX(Component.CENTER_ALIGNMENT);
        blmPane.setLayout(new BoxLayout(blmPane, BoxLayout.LINE_AXIS));
        blmPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        BLMLabels[4][0] = new JLabel("DbgCmdSetParameters ");
        blmPane.add(BLMLabels[4][0]);
        BLMLabels[4][1] = new JLabel(getCspLabel());
        blmPane.add(BLMLabels[4][1]);
        blmPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.add(blmPane2);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(blmPane);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(blmPane3);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        Container contentPane = getContentPane();
        contentPane.add(readyPane, BorderLayout.PAGE_START);
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(msgPane, BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(locationComp);
    }

    private void putBLMLabels(JLabel[][] PSLabels) {
        getMainWindow().putBLMLabels(PSLabels);
    }

    public MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }

    public void createChannelWrappers(String initPv) {
        String[] chNames = { "", "", "", "", "", "" };
        MPSblmWrap = new ArrayList(300);
        String chName = initPv + ":DbgMPSPulseLossLimit";
        wrapper = new ChannelWrapper(chName);
        pllWrapper = wrapper;
        MPSblmWrap.add(pllWrapper);
        pllWrapper.addCAValueListener(this);
        chNames[0] = chName;
        String str;
        str = "" + wrapper.getValue();
        MPSblmMap.put(chNames[0], str);
        chName = initPv + ":DbgHVBias";
        wrapper = new ChannelWrapper(chName);
        hvWrapper = wrapper;
        MPSblmWrap.add(hvWrapper);
        hvWrapper.addCAValueListener(this);
        chNames[1] = chName;
        str = "" + wrapper.getValue();
        MPSblmMap.put(chNames[1], str);
        chName = initPv + ":DbgCmdSetParameters";
        wrapper = new ChannelWrapper(chName);
        cspWrapper = wrapper;
        MPSblmWrap.add(cspWrapper);
        cspWrapper.addCAValueListener(this);
        chNames[2] = chName;
        str = "" + wrapper.getValue();
        MPSblmMap.put(chNames[2], str);
        chName = initPv + ":FPAR_" + chainSel + "_input_status";
        wrapper = new ChannelWrapper(chName);
        inStWrapper = wrapper;
        MPSblmWrap.add(inStWrapper);
        inStWrapper.addCAValueListener(this);
        chNames[3] = chName;
        str = "" + wrapper.getValue();
        MPSblmMap.put(chNames[3], str);
        chName = initPv + ":FPAR_" + chainSel + "_chan_status";
        chStWrapper = new ChannelWrapper(chName);
        MPSblmWrap.add(chStWrapper);
        chStWrapper.addCAValueListener(this);
        str = "" + chStWrapper.getValue();
        MPSblmMap.put(chName, str);
        chName = initPv + ":DbgHVBiasRb";
        wrapper = new ChannelWrapper(chName);
        hvrbWrapper = wrapper;
        MPSblmWrap.add(hvrbWrapper);
        hvrbWrapper.addCAValueListener(this);
        chNames[4] = chName;
        str = "" + wrapper.getValue();
        MPSblmMap.put(chNames[4], str);
        chName = initPv + ":DbgDetectorTest";
        wrapper = new ChannelWrapper(chName);
        tdWrapper = wrapper;
        MPSblmWrap.add(tdWrapper);
        tdWrapper.addCAValueListener(this);
        chNames[5] = chName;
        str = "" + wrapper.getValue();
        MPSblmMap.put(chNames[5], str);
    }

    /** Indicates a new channel access value has been found for this wrapped channel. */
    public void newValue(final ChannelWrapper wrapper, int value) {
        float fvalue;
        value = wrapper.getValue();
        fvalue = wrapper.getFloatValue();
        final String chName = wrapper.getName();
        if (inStWrapper == null) return;
        final String ch2Name = inStWrapper.getName();
        int index = ch2Name.lastIndexOf("FPAR_" + chainSel + "_input_status");
        if (chName.equals(ch2Name) && index > 0) {
            BLMLabels[0][1].setText(String.valueOf(value));
            chanStatPanel.validate();
        }
        if (index > 0 && chName.equals(ch2Name) && IsStarted == 1 && getMainWindow().getSwitchMachMd().equals(chainSel)) {
            if (InSt1 == -1) InSt1 = initInSt;
            if (InSt2 == -1) InSt2 = value; else if (InSt3 == -1) InSt3 = value;
            if (InSt1 == 1 && InSt2 == 0) {
                TestResult = "Passed";
                PFstopLabel.setText("Passed");
                PFstopLabel.validate();
                msgPane.validate();
                oldInSt = -1;
                IsStarted = 0;
                InSt1 = -1;
                InSt2 = -1;
                InSt3 = -1;
                return;
            } else if (IsStarted == 1) {
                oldInSt = value;
                if (InSt1 == -1) InSt1 = value; else if (InSt2 == -1) InSt2 = value; else if (InSt3 == -1) InSt3 = value;
            }
        } else if (index > 0 && chName.equals(ch2Name) && IsStarted == 1 && !getMainWindow().getSwitchMachMd().equals(chainSel)) {
            if (InSt1 == -1) InSt1 = initInSt;
            if (InSt2 == -1) InSt2 = value; else if (InSt3 == -1) InSt3 = value;
            if (InSt1 == 1 && InSt2 == 0 && InSt3 == 1) {
                TestResult = "Passed";
                PFstopLabel.setText("Passed");
                PFstopLabel.validate();
                msgPane.validate();
                oldInSt = -1;
                IsStarted = 0;
                InSt1 = -1;
                InSt2 = -1;
                InSt3 = -1;
                return;
            } else if (IsStarted == 1) {
                oldInSt = value;
                if (InSt1 == -1) InSt1 = value; else if (InSt2 == -1) InSt2 = value; else if (InSt3 == -1) InSt3 = value;
            }
        } else {
            index = chName.lastIndexOf("DbgHVBias");
            if (index > -1) {
                BLMLabels[1][1].setText(String.valueOf(value));
                inputStatPanel.validate();
            } else {
                index = chName.lastIndexOf("DbgHVBiasRb");
                if (index > -1) {
                    BLMLabels[2][1].setText(String.valueOf(value));
                    blmStatPanel.validate();
                } else {
                    index = chName.lastIndexOf("DbgMPSPulseLossLimit");
                    if (index > -1) {
                        BLMLabels[3][1].setText(String.valueOf(fvalue));
                        blmPane2.validate();
                    } else {
                        index = chName.lastIndexOf("DbgCmdSetParameters");
                        if (index > -1) {
                            BLMLabels[4][1].setText(String.valueOf(value));
                            blmPane.validate();
                        }
                    }
                }
            }
        }
    }

    private String getPllLabel() {
        float val = getPllValue();
        String stat = "" + val;
        return stat;
    }

    private float getPllValue() {
        return pllWrapper.getFloatValue();
    }

    private String getCspLabel() {
        int val = getCspValue();
        String stat = "" + val;
        return stat;
    }

    private int getCspValue() {
        return cspWrapper.getValue();
    }

    private String getHVRbLabel() {
        int val = getHVRbValue();
        String stat = "" + val;
        return stat;
    }

    private int getHVRbValue() {
        return hvrbWrapper.getValue();
    }

    private int getInputStatValue() {
        if (inStWrapper == null) System.out.println("inStWrapper is NULL");
        return inStWrapper.getValue();
    }

    public static int getChanStatValue() {
        if (chStWrapper == null) System.out.println("chStWrapper is NULL");
        return chStWrapper.getValue();
    }

    private String getHVBiasLabel() {
        int val = getHVBiasValue();
        String stat = "" + val;
        return stat;
    }

    private int getHVBiasValue() {
        return hvWrapper.getValue();
    }

    private void setTdValue(int val) {
        try {
            tdWrapper.getChannel().putVal(val);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
    }

    private void checkTask() {
        if (task.isDone()) {
            Toolkit.getDefaultToolkit().beep();
            timer.stop();
            setCursor(null);
            progressbar.setValue(progressbar.getMinimum());
        }
    }

    public Map getRdyToTestMPSblm(String initPv) {
        List _channelWrappers = new ArrayList(300);
        createChannelWrappers(initPv);
        return MPSblmMap;
    }

    public List getMPSblmWrap(String pv) {
        return MPSblmWrap;
    }

    private void testBLM(String lText) {
        initInSt = getInputStatValue();
        initChSt = getChanStatValue();
        if (initInSt == 0 || initChSt == 0) {
            TestResult = "Failed";
            PFstopLabel.setText(FailedStatus);
            PFstopLabel.validate();
            msgPane.validate();
            oldInSt = -1;
            IsStarted = 0;
            InSt1 = -1;
            InSt2 = -1;
            InSt3 = -1;
            String newMsg = FailedStatus + "\nInitially equals 0.";
            PFstopLabel.setText(TestResult);
            PFstopLabel.validate();
            msgPane.validate();
            return;
        }
        IsStarted = 1;
        PFstopLabel.setText("");
        PFstopLabel.setVisible(true);
        PFstopLabel.validate();
        msgPane.validate();
        initPLL = getPllValue();
        try {
            pllWrapper.getChannel().putVal(1);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        try {
            cspWrapper.getChannel().putVal(1);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        int val = getHVRbValue();
        if (val > -900) {
            try {
                hvWrapper.getChannel().putVal(-1000);
            } catch (ConnectionException e) {
                System.err.println("Unable to connect to channel access.");
            } catch (PutException e) {
                System.err.println("Unable to set process variables.");
            }
        }
        try {
            pllWrapper.getChannel().putVal(.01);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        setTdValue(1);
        try {
            cspWrapper.getChannel().putVal(1);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        task.MPSwait();
        setTdValue(0);
        try {
            cspWrapper.getChannel().putVal(1);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        try {
            pllWrapper.getChannel().putVal(initPLL);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        try {
            cspWrapper.getChannel().putVal(1);
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        int ChSt = getChanStatValue();
        if (ChSt == 0) TestResult = "Passed"; else TestResult = "Failed";
        PFstopLabel.setText(TestResult);
        PFstopLabel.validate();
        msgPane.validate();
    }

    public CAValueListener getListen() {
        return this;
    }
}
