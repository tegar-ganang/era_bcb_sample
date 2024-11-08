package gov.sns.apps.mpsinputtest;

import java.util.Map;
import java.util.List;
import javax.swing.Timer;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import gov.sns.ca.*;

public class TestAllBLMsPanel extends JDialog implements CAValueListener {

    public static final int WAIT_TICS = 10;

    private static JPanel basePanel, chanStatPanel, blmPane;

    private static JPanel inputStatPanel, blmStatPanel, blmPane2, msgPane;

    public static List _channelWrappers, MPSblmWrap = new ArrayList(300), MPSpllWrap;

    private static int InSt1 = -1, InSt2 = -1, InSt3 = -1;

    private JProgressBar resetPB, startPB, stopPB, progressbar;

    private static LongTask task;

    private static Timer timer;

    private static ChannelWrapper wrapper, hvrbWrapper, inStWrapper, hvWrapper, chStWrapper;

    private static ChannelWrapper pllWrapper, cspWrapper, tdWrapper;

    private JLabel PFstartLabel = new JLabel();

    public static Map MPSblmMap = new LinkedHashMap();

    public static Map MPSpllMap;

    private static int IsStarted = 0, initInSt = -1, oldInSt = -1, initChSt = -1;

    private static JLabel PFstopLabel = new JLabel();

    private static String FailedStatus = "<html><font align=center color=red>FAILED!</font></html>";

    private static JLabel TitleLabel = new JLabel();

    private static JLabel label = new JLabel();

    private static JLabel tstrslt = new JLabel();

    private static Container contentPane;

    private static JPanel listPane;

    private JButton closeButton;

    public static JLabel[][] BLMLabels = { { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() }, { new JLabel(), new JLabel() } };

    private static float initPLL;

    private static String TestResult = "";

    private static String chainSel = "";

    private static TestAllBLMsPanel newPanel;

    private static String result = "";

    public TestAllBLMsPanel() {
        JPanel readyPane = new JPanel();
    }

    public TestAllBLMsPanel(String initPv) {
        createChannelWrappers(initPv);
    }

    public static TestAllBLMsPanel showPanel(Frame frame, Component locationComp, Object[][] data, String title, String lText, String initValue, final String longValue, int numSel) {
        newPanel = new TestAllBLMsPanel(frame, locationComp, data, title, lText, initValue, longValue, numSel);
        newPanel.setVisible(true);
        return newPanel;
    }

    private TestAllBLMsPanel(Frame frame, Component locationComp, Object[][] data, String title, final String lText, String initValue, final String longValue, int numSel) {
        for (int i = 0; i < 5; i++) {
            BLMLabels[i][0] = new JLabel("");
            BLMLabels[i][1] = new JLabel("");
        }
        chainSel = initValue;
        listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        label = new JLabel(lText);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.setPreferredSize(new Dimension(300, 250));
        listPane.add(label);
        listPane.add(Box.createRigidArea(new Dimension(0, 30)));
        chanStatPanel = new JPanel();
        chanStatPanel.setLayout(new BoxLayout(chanStatPanel, BoxLayout.LINE_AXIS));
        chanStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        chanStatPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        BLMLabels[0][0] = new JLabel("FPAR_" + chainSel + "_input_status ");
        chanStatPanel.add(BLMLabels[0][0]);
        BLMLabels[0][1] = new JLabel(String.valueOf(getInputStatValue(lText)));
        chanStatPanel.add(BLMLabels[0][1]);
        inputStatPanel = new JPanel();
        inputStatPanel.setLayout(new BoxLayout(inputStatPanel, BoxLayout.LINE_AXIS));
        inputStatPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        BLMLabels[1][0] = new JLabel("DbgHVBias ");
        inputStatPanel.add(BLMLabels[1][0]);
        BLMLabels[1][1] = new JLabel(getHVBiasLabel());
        inputStatPanel.add(BLMLabels[1][1]);
        inputStatPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        listPane.add(chanStatPanel);
        listPane.add(inputStatPanel);
        listPane.validate();
        blmStatPanel = new JPanel();
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
        blmPane2 = new JPanel();
        blmPane2.setLayout(new BoxLayout(blmPane2, BoxLayout.LINE_AXIS));
        blmPane2.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        BLMLabels[3][0] = new JLabel("DbgMPSPulseLossLimit ");
        blmPane2.add(BLMLabels[3][0]);
        BLMLabels[3][1] = new JLabel(getPllLabel());
        blmPane2.add(BLMLabels[3][1]);
        blmPane2.setAlignmentX(Component.CENTER_ALIGNMENT);
        blmPane = new JPanel();
        blmPane.setLayout(new BoxLayout(blmPane, BoxLayout.LINE_AXIS));
        blmPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        BLMLabels[4][0] = new JLabel("DbgCmdSetParameters ");
        blmPane.add(BLMLabels[4][0]);
        BLMLabels[4][1] = new JLabel(getCspLabel());
        blmPane.add(BLMLabels[4][1]);
        blmPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton = new JButton("Close");
        closeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        closeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setVisible(false);
            }
        });
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout());
        buttonPane.add(closeButton);
        buttonPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        PFstopLabel.setText("");
        PFstopLabel.validate();
        tstrslt.setText("");
        tstrslt.validate();
        PFstopLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        msgPane = new JPanel();
        msgPane.setLayout(new BoxLayout(msgPane, BoxLayout.LINE_AXIS));
        msgPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        msgPane.add(PFstopLabel);
        msgPane.add(tstrslt);
        msgPane.add(buttonPane);
        listPane.add(blmPane2);
        listPane.add(Box.createRigidArea(new Dimension(0, 5)));
        listPane.add(blmPane);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        listPane.setVisible(true);
        contentPane = getContentPane();
        contentPane.add(readyPane, BorderLayout.PAGE_START);
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(msgPane, BorderLayout.PAGE_END);
        pack();
        setLocationRelativeTo(locationComp);
    }

    private void putBLMLabels(JLabel[][] PSLabels) {
        getMainWindow().putBLMLabels(PSLabels);
    }

    public static MPSWindow getMainWindow() {
        MPSDocument document = new MPSDocument();
        return document.getWindow();
    }

    public void createPLLwrappers(String initPv) {
        Channel ch;
        String chName = initPv + ":DbgMPSPulseLossLimit";
        pllWrapper = new ChannelWrapper(chName);
        MPSpllWrap.add(pllWrapper);
        pllWrapper.addCAValueListener(this);
        String str;
        str = "" + pllWrapper.getValue();
        MPSpllMap.put(chName, str);
        try {
            ch = pllWrapper.getChannel();
            ch.putVal(1);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        chName = initPv + ":DbgCmdSetParameters";
        cspWrapper = new ChannelWrapper(chName);
        MPSblmWrap.add(cspWrapper);
        cspWrapper.addCAValueListener(this);
        try {
            ch = cspWrapper.getChannel();
            ch.putVal(1);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
    }

    public void createChannelWrappers(String initPv) {
        String[] chNames = { "", "", "", "", "", "" };
        if (MPSblmWrap != null) {
            Iterator iter = MPSblmWrap.iterator();
            while (iter.hasNext()) {
                wrapper = (ChannelWrapper) iter.next();
                wrapper.removeCAValueListener(this);
            }
        }
        String chName = initPv + ":DbgMPSPulseLossLimit";
        chNames[0] = chName;
        String str;
        str = "";
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
        if (chainSel.length() == 0) chainSel = getMainWindow().getSelChain();
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
        chNames[3] = chName;
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
        final String InStName = inStWrapper.getName();
        final String ch2Name = label.getText();
        int index = chName.lastIndexOf("FPAR_");
        if (chName.equals(InStName) && index > 0) {
            BLMLabels[0][1].setText(String.valueOf(value));
            if (chanStatPanel != null) chanStatPanel.validate();
            if (listPane != null) listPane.validate();
        }
        if (index > 0 && chName.equals(InStName) && IsStarted == 1 && getMainWindow().getSwitchMachMd().equals(chainSel)) {
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
        } else if (index > 0 && chName.equals(InStName) && IsStarted == 1 && !getMainWindow().getSwitchMachMd().equals(chainSel)) {
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
            index = chName.lastIndexOf("FPAR_");
            if (index > -1) {
                BLMLabels[0][1].setText(String.valueOf(value));
                if (chanStatPanel != null) chanStatPanel.validate();
                if (listPane != null) listPane.validate();
            } else {
                index = chName.lastIndexOf("DbgHVBias");
                if (index > -1) {
                    BLMLabels[1][1].setText(String.valueOf(value));
                    if (inputStatPanel != null) inputStatPanel.validate();
                    if (listPane != null) listPane.validate();
                } else {
                    index = chName.lastIndexOf("DbgHVBiasRb");
                    if (index > -1) {
                        BLMLabels[2][1].setText(String.valueOf(value));
                        if (blmStatPanel != null) blmStatPanel.validate();
                        if (listPane != null) listPane.validate();
                    } else {
                        index = chName.lastIndexOf("DbgMPSPulseLossLimit");
                        if (index > -1) {
                            BLMLabels[3][1].setText(String.valueOf(fvalue));
                            if (blmPane2 != null) blmPane2.validate();
                            if (listPane != null) listPane.validate();
                        } else {
                            index = chName.lastIndexOf("DbgCmdSetParameters");
                            if (index > -1) {
                                BLMLabels[4][1].setText(String.valueOf(value));
                                if (blmPane != null) blmPane.validate();
                                if (listPane != null) listPane.validate();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void updatePanel(Frame frame, Component locationComp, Object[][] data, String title, final String lText, String initValue, final String longValue) {
        label.setText(lText);
        listPane.validate();
        chainSel = initValue;
        initInSt = -1;
        result = "";
        int recheck = -1;
        if (initInSt == -1) {
            recheck = getInputStatValue(lText);
            if (recheck == 0) {
                task.MPSwait();
                recheck = getInputStatValue(lText);
            }
            initInSt = recheck;
        }
        BLMLabels[0][1].setText(String.valueOf(initInSt));
        chanStatPanel.validate();
        listPane.validate();
        newPanel = new TestAllBLMsPanel(lText);
        TitleLabel.setText(lText);
        listPane.validate();
        BLMLabels[0][1] = new JLabel(String.valueOf(getInputStatValue(lText)));
        chanStatPanel.validate();
        listPane.validate();
        BLMLabels[1][1].setText(getHVBiasLabel());
        inputStatPanel.validate();
        listPane.validate();
        BLMLabels[2][1].setText(getHVRbLabel());
        blmStatPanel.validate();
        listPane.validate();
        task = new LongTask(inStWrapper);
        InSt1 = -1;
        InSt2 = -1;
        InSt3 = -1;
        testBLM(lText);
        int ChSt = getChanStatValue(lText);
        if (ChSt == 0) {
            TestResult = "Passed";
        } else TestResult = "Failed";
        task.sendResult(TestResult);
        tstrslt.setText(TestResult);
        tstrslt.validate();
        msgPane.validate();
        getMainWindow().publishMPSinputTestResultsToLogbook(lText, TestResult, longValue, result);
        msgPane.validate();
        BLMLabels[3][1].setText(getPllLabel());
        blmPane2.validate();
        BLMLabels[4][1].setText(getCspLabel());
        blmPane.validate();
        listPane.validate();
        contentPane.validate();
        TestResult = "";
    }

    public static JLabel getTitleLbl() {
        return TitleLabel;
    }

    public static void stopTimer() {
        timer.stop();
    }

    private static String getPllLabel() {
        float val = getPllValue();
        String stat = "" + val;
        return stat;
    }

    public void initPLLwrappers() {
        MPSpllWrap = new ArrayList();
        MPSpllMap = new LinkedHashMap();
    }

    private static float getPllValue() {
        if (pllWrapper == null) return -1;
        return pllWrapper.getFloatValue();
    }

    private static String getCspLabel() {
        int val = getCspValue();
        String stat = "" + val;
        return stat;
    }

    private static int getCspValue() {
        return cspWrapper.getValue();
    }

    private static String getHVRbLabel() {
        int val = getHVRbValue();
        String stat = "" + val;
        return stat;
    }

    private static int getHVRbValue() {
        return hvrbWrapper.getValue();
    }

    private static String getHVBiasLabel() {
        int val = getHVBiasValue();
        String stat = "" + val;
        return stat;
    }

    private static int getHVBiasValue() {
        return hvWrapper.getValue();
    }

    private static void setTdValue(int val) {
        Channel td;
        try {
            td = tdWrapper.getChannel();
            td.putVal(val);
            td.flushIO();
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

    private static void testBLM(String lText) {
        Channel ch;
        PFstopLabel.setText("");
        PFstopLabel.setVisible(true);
        PFstopLabel.validate();
        tstrslt.setText("");
        tstrslt.validate();
        msgPane.validate();
        initInSt = getInputStatValue(lText);
        initChSt = getChanStatValue(lText);
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
            return;
        }
        IsStarted = 1;
        try {
            ch = cspWrapper.getChannel();
            ch.putVal(1);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        int val = getHVRbValue();
        if (val > -900) {
            try {
                ch = hvWrapper.getChannel();
                ch.putVal(-1000);
                ch.flushIO();
            } catch (ConnectionException e) {
                System.err.println("Unable to connect to channel access.");
            } catch (PutException e) {
                System.err.println("Unable to set process variables.");
            }
        }
        Iterator iter = MPSpllWrap.iterator();
        while (iter.hasNext()) {
            pllWrapper = (ChannelWrapper) iter.next();
            if (pllWrapper.getName().indexOf(lText) != -1) break;
        }
        try {
            ch = pllWrapper.getChannel();
            ch.putVal(.01);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        setTdValue(1);
        try {
            ch = cspWrapper.getChannel();
            ch.putVal(1);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        task.MPSwait();
        setTdValue(0);
        try {
            ch = cspWrapper.getChannel();
            ch.putVal(1);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        task.MPSwait();
        iter = MPSpllWrap.iterator();
        while (iter.hasNext()) {
            pllWrapper = (ChannelWrapper) iter.next();
            if (pllWrapper.getName().indexOf(lText) != -1) break;
        }
        String value = "" + pllWrapper.getFloatValue();
        initPLL = pllWrapper.getFloatValue();
        try {
            ch = pllWrapper.getChannel();
            ch.putVal(initPLL);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
        try {
            ch = cspWrapper.getChannel();
            ch.putVal(1);
            ch.flushIO();
        } catch (ConnectionException e) {
            System.err.println("Unable to connect to channel access.");
        } catch (PutException e) {
            System.err.println("Unable to set process variables.");
        }
    }

    public static int getInputStatValue(String lText) {
        Iterator iter = MPSblmWrap.iterator();
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            if (wrapper.getName().indexOf(lText) != -1 && wrapper.getName().indexOf(":FPAR_") != -1 && wrapper.getName().indexOf("input_status") != -1) {
                inStWrapper = wrapper;
                return inStWrapper.getValue();
            }
        }
        return -1;
    }

    public static int getChanStatValue(String lText) {
        Iterator iter = MPSblmWrap.iterator();
        while (iter.hasNext()) {
            wrapper = (ChannelWrapper) iter.next();
            if (wrapper.getName().indexOf(lText) != -1 && wrapper.getName().indexOf(":FPAR_") != -1 && wrapper.getName().indexOf("chan_status") != -1) {
                chStWrapper = wrapper;
                return chStWrapper.getValue();
            }
        }
        return -1;
    }

    public void TestStart() {
        IsStarted = 1;
    }

    public void TestDone() {
        IsStarted = 0;
        PFstopLabel.setText("Done");
        PFstopLabel.setVisible(true);
        PFstopLabel.validate();
        tstrslt.setText("");
        tstrslt.validate();
        msgPane.validate();
    }

    public CAValueListener getListen() {
        return this;
    }
}
