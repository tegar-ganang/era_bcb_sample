package org.nees.tivo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.nees.rbnb.ArchiveUtility;
import org.nees.rbnb.ChannelUtility;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Sink;

public class SegmentPushArgsDialog extends JDialog implements KeyEventDispatcher, ActionListener {

    ArchiveInterface archive;

    static ViewerUtilities v = new ViewerUtilities();

    private static final String NOT_OK = v.htmlRed("Set This");

    private static final String OK = "OK";

    private static final String WINDOW_TITLE = "Parameter Values for Capture";

    private static final String DEFAULT_HOST = "neestpm.sdsc.edu";

    private static final String DEFAULT_PORT = "3333";

    private static final String DEFAULT_SINK_NAME = "_CaptureSink";

    private Frame root;

    private static SimpleDateFormat TIMEFORMAT = ArchiveUtility.getCommandFormat();

    private JTextField host = new JTextField(15);

    private JTextField port = new JTextField(4);

    private JTextField sourceName = new JTextField(10);

    private JTextField channelName = new JTextField(8);

    private double startTime = 0.0;

    private double endTime = 0.0;

    private ArchiveSegmentInterface selectedSeg = null;

    private String previousServer = null;

    private String previousChannel = null;

    private boolean serverOK = false;

    private static final String DEFAULT_SOURCE_NAME = "FromArchive";

    private static final String DEFAULT_CHANNEL_NAME = "video.jpg";

    private static final String SEGMENT_ACTION = "Segment Name";

    private static final String SERVER_ACTION = "Verify Server";

    private static final String OK_ACTION = "Send to RBNB";

    private static final String CANCEL_ACTION = "Cancel";

    private JButton segmentButton;

    private JButton serverButton;

    private JButton okButton;

    private JButton cancelButton;

    private JLabel segmentName = new JLabel();

    private JLabel generalStatus = new JLabel();

    private String errorMessage = null;

    private boolean cancled = false;

    private SegmentPushArgsDialog() {
    }

    public SegmentPushArgsDialog(Frame root, ArchiveInterface archive, ArchiveSegmentInterface previousSeg) {
        super(root, true);
        this.root = root;
        this.archive = archive;
        this.selectedSeg = previousSeg;
        setTitle(WINDOW_TITLE);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowActivated(WindowEvent e) {
                bindKeys();
            }

            public void windowDeactivated(WindowEvent e) {
                unbindKeys();
            }
        });
        segmentButton = setupButton(SEGMENT_ACTION, this);
        serverButton = setupButton(SERVER_ACTION, this);
        okButton = setupButton(OK_ACTION, this);
        cancelButton = setupButton(CANCEL_ACTION, this);
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JPanel row;
        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.add(new JLabel("Name of Segment to send:"));
        row.add(segmentName);
        row.add(segmentButton);
        top.add(row);
        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        row.add(new JLabel("RBNB Host"));
        row.add(host);
        row.add(new JLabel("Port"));
        row.add(port);
        row.add(serverButton);
        top.add(row);
        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        row.add(new JLabel("RBNB Source name: "));
        row.add(sourceName);
        row.add(new JLabel("Channel name: "));
        row.add(channelName);
        top.add(row);
        JPanel buttonPanel = new JPanel();
        JButton button;
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        top.add(buttonPanel);
        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
        top.add(makeBox(row, generalStatus));
        host.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent ignore) {
            }

            public void focusLost(FocusEvent arg0) {
                System.out.println("Focus off host");
                validateHostAndPort();
                updateGraphics();
            }
        });
        port.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent ignore) {
            }

            public void focusLost(FocusEvent arg0) {
                System.out.println("Focus off port");
                validateHostAndPort();
                updateGraphics();
            }
        });
        updateGraphics();
        getContentPane().add(top, BorderLayout.CENTER);
    }

    public void start() {
        setDefaults();
        validateHostAndPort();
        updateGraphics();
        setVisible(true);
    }

    public void restart(String host, String port, ArchiveSegmentInterface seg) {
        this.selectedSeg = seg;
        setDefaults();
        setHost(host);
        setPort(port);
        previousServer = null;
        previousChannel = null;
        validateHostAndPort();
        updateGraphics();
        setVisible(true);
    }

    private void setDefaults() {
        setSegmentName();
        setHost(DEFAULT_HOST);
        setPort(DEFAULT_PORT);
        setSourceName(DEFAULT_SOURCE_NAME);
        setChannelName(DEFAULT_CHANNEL_NAME);
    }

    private void updateGraphics() {
        updateButtons();
        String s = "";
        if (errorMessage != null) s += v.red(errorMessage);
        generalStatus.setText(v.htmlSmall(s));
        errorMessage = null;
        pack();
    }

    private JPanel makeBox(JPanel row, JLabel statusLabel) {
        JPanel left = new JPanel();
        left.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(statusLabel);
        JPanel box = new JPanel();
        box.setLayout(new BorderLayout());
        box.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        box.add(row, BorderLayout.CENTER);
        box.add(left, BorderLayout.SOUTH);
        return box;
    }

    private JButton setupButton(String action, ActionListener l) {
        JButton button = new JButton(action);
        button.addActionListener(l);
        button.setActionCommand(action);
        return button;
    }

    private void updateButtons() {
        if (selectedSeg == null) segmentButton.setText(v.htmlRed("Set " + SEGMENT_ACTION)); else segmentButton.setText("Reset " + SEGMENT_ACTION);
        if (serverOK) {
            serverButton.setText("Server OK");
            serverButton.setEnabled(false);
        } else {
            serverButton.setText(SERVER_ACTION);
            serverButton.setEnabled(true);
        }
        segmentButton.setEnabled(true);
        cancelButton.setEnabled(true);
        okButton.setEnabled(ready());
    }

    private void bindKeys() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addKeyEventDispatcher(this);
    }

    private void unbindKeys() {
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.removeKeyEventDispatcher(this);
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (keyCode == KeyEvent.VK_ESCAPE) {
            dispose();
            return true;
        } else {
            return false;
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (!(ev.getSource() instanceof JButton)) return;
        String arg = ev.getActionCommand();
        if (arg.equals(OK_ACTION)) {
            if (!ready()) {
                updateButtons();
                return;
            }
            cancled = false;
            dispose();
        } else if (arg.equals(CANCEL_ACTION)) {
            cancled = true;
            dispose();
        } else if (arg.equals(SEGMENT_ACTION)) {
            GetSegmentDialog sel = new GetSegmentDialog("Select Segment to Send to RBNB", (ArchiveInterface) archive, (JFrame) root, selectedSeg);
            selectedSeg = sel.getSelectedSegment();
            setSegmentName();
        } else System.out.println("Missed an action = " + arg);
        updateGraphics();
    }

    private boolean ready() {
        return (selectedSeg != null) && serverOK;
    }

    public boolean cancled() {
        return cancled;
    }

    private void exit() {
        setVisible(false);
    }

    private void validateHostAndPort() {
        setHost(getHost());
        setPort(getPort());
        if (getHost().length() == 0) {
            serverOK = false;
            return;
        }
        if (getPort().length() == 0) {
            serverOK = false;
            return;
        }
        try {
            Sink sink = new Sink();
            sink.OpenRBNBConnection(getServer(), "__test");
            sink.CloseRBNBConnection();
        } catch (SAPIException e) {
            serverOK = false;
            errorMessage = "Failed to connect to server" + getServer();
            return;
        }
        serverOK = true;
    }

    /** @return */
    public String getHost() {
        return host.getText().trim();
    }

    /** @return */
    public String getPort() {
        return port.getText().trim();
    }

    /** @return */
    public String getServer() {
        return getHost() + ":" + getPort();
    }

    /** @return */
    public String getSegmentName() {
        if (selectedSeg != null) return selectedSeg.getName();
        return null;
    }

    /** @return */
    public ArchiveSegmentInterface getSegment() {
        return selectedSeg;
    }

    /** @return */
    public String getSource() {
        if (!serverOK) return null;
        return sourceName.getText();
    }

    /** @return */
    public String getChannel() {
        if (!serverOK) return null;
        return channelName.getText();
    }

    private void setHost(String field) {
        host.setText(field);
    }

    private void setPort(String field) {
        port.setText(field);
    }

    private void setSegmentName() {
        String name = v.htmlRed("-- none --");
        if (selectedSeg != null) name = selectedSeg.getName();
        segmentName.setText(name);
    }

    private void setChannelName(String field) {
        channelName.setText(field);
    }

    private void setSourceName(String field) {
        sourceName.setText(field);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                SegmentPushArgsDialog wrap = new SegmentPushArgsDialog();
                TestFrame t = wrap.new TestFrame();
                t.go();
            }
        });
    }

    private class TestFrame extends JFrame {

        public TestFrame() {
            super();
        }

        public void go() {
            JFrame.setDefaultLookAndFeelDecorated(true);
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    dispose();
                }
            });
            doTest();
        }

        private void doTest() {
            Archive a = null;
            try {
                a = new Archive();
            } catch (ArchiveException e) {
                e.printStackTrace();
                System.out.println("Could not get an archive");
            }
            if (a == null) return;
            ArchiveSegmentInterface seg = null;
            if (a.getSegmentsArray().length < 0) seg = a.getSegmentsArray()[0];
            SegmentPushArgsDialog test = new SegmentPushArgsDialog(this, a, seg);
            test.start();
            printTest(test);
            while (!test.cancled()) {
                test.restart(test.getHost(), test.getPort(), test.getSegment());
                printTest(test);
            }
            test.dispose();
            System.exit(0);
        }

        /**
         * @param test
         */
        private void printTest(SegmentPushArgsDialog test) {
            if (test.cancled()) {
                System.out.println("");
                System.out.println("Test cancled.");
            }
            System.out.println("");
            System.out.print("Segment Name: " + test.getSegmentName());
            System.out.print(";  Server: " + test.getHost() + ":" + test.getPort());
            System.out.print(";  Source/Channel: " + test.getSource() + "/" + test.getChannel());
            System.out.println("");
        }
    }

    private void traceTime(double d) {
        traceTime("", d);
    }

    private void traceTime(String prefix, double d) {
        long unixTime = (long) (d * 1000.0);
        traceTime(prefix, unixTime);
    }

    private void traceTime(long t) {
        traceTime("", t);
    }

    private void traceTime(String prefix, long t) {
        System.out.println(prefix + ": " + TIMEFORMAT.format(new Date(t)));
    }
}
