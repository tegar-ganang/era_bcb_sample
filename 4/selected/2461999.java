package org.nees.buffalo.rbnb.dataviewer;

import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Iterator;
import java.util.Vector;
import java.util.Enumeration;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JDialog;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.dnd.DropTarget;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nees.calculate.expression.Expression;
import com.rbnb.sapi.ChannelMap;
import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.Source;

/**
 * @author Terry E. Weymouth
 */
public class ExpressionDataPanel extends AbstractDataPanel implements ActionListener, ListSelectionListener {

    static Log log = LogFactory.getLog(ExpressionDataPanel.class.getName());

    private double front = -1.0;

    private DefaultListModel channelList = new DefaultListModel();

    Expression parseTree = null;

    boolean running;

    String outSourceName;

    String outChannelName;

    Source source;

    /**
	 * @param dataPanelContainer
	 * @param player
	 */
    public ExpressionDataPanel(DataPanelContainer dataPanelContainer, Player player) {
        super(dataPanelContainer, player);
        setDataComponent(initPanel());
        setControlBar(true);
        setDropTarget(true);
    }

    private JPanel inside;

    private JTextArea statusMessagePanel;

    private JTextArea expressionPanel;

    private JTextField sourceNameField;

    private JTextField channelNameField;

    private JScrollPane expressionScrollPane;

    private JList channelView;

    private static final String CHECK = "Check Expression";

    private static final String RESET = "Reset";

    private static final String DETAILS = "Details";

    JButton checkButton, resetButton, detailsButton;

    String detailString = null;

    JComponent initPanel() {
        JButton button;
        JPanel buttonHolder = new JPanel();
        button = new JButton(CHECK);
        button.addActionListener(this);
        buttonHolder.add(button);
        checkButton = button;
        button = new JButton(DETAILS);
        button.addActionListener(this);
        buttonHolder.add(button);
        detailsButton = button;
        button = new JButton(RESET);
        button.addActionListener(this);
        buttonHolder.add(button);
        resetButton = button;
        adjustButtons(true, false, false);
        JLabel label;
        label = new JLabel("Enter output channel source name and channal name: ");
        JPanel channelNameArea = new JPanel();
        JLabel sourceNameLabel = new JLabel("SourceName:");
        JLabel channelNameLabel = new JLabel("ChannelName:");
        sourceNameField = new JTextField(8);
        channelNameField = new JTextField(8);
        sourceNameField.setText("Compute");
        channelNameField.setText("out");
        channelNameArea.add(sourceNameLabel);
        channelNameArea.add(sourceNameField);
        channelNameArea.add(channelNameLabel);
        channelNameArea.add(channelNameField);
        JPanel channelNameHolder = new JPanel();
        channelNameHolder.setLayout(new BorderLayout());
        channelNameHolder.add(label, BorderLayout.NORTH);
        channelNameHolder.add(channelNameArea, BorderLayout.CENTER);
        label = new JLabel("Enter Expression:");
        expressionPanel = new JTextArea();
        expressionPanel.setEditable(true);
        expressionPanel.setText("(c0 + c1)/2.0;");
        expressionScrollPane = new JScrollPane(expressionPanel);
        JPanel expressionHolder = new JPanel();
        expressionHolder.setLayout(new BorderLayout());
        expressionHolder.add(label, BorderLayout.NORTH);
        expressionHolder.add(expressionScrollPane, BorderLayout.CENTER);
        label = new JLabel("Drag and Drop Channels on background (select to delete):");
        channelView = new JList(channelList);
        channelView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        channelView.addListSelectionListener(this);
        channelView.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(channelView);
        JPanel channelHolder = new JPanel();
        channelHolder.setLayout(new BorderLayout());
        channelHolder.add(label, BorderLayout.NORTH);
        channelHolder.add(listScrollPane, BorderLayout.CENTER);
        label = new JLabel("Status:");
        statusMessagePanel = new JTextArea(4, 40);
        statusMessagePanel.setEditable(true);
        statusMessagePanel.setText("Status message.");
        statusMessagePanel.setEditable(false);
        JScrollPane messageScrollPane = new JScrollPane(statusMessagePanel);
        JPanel messsageHolder = new JPanel();
        messsageHolder.setLayout(new BorderLayout());
        messsageHolder.add(label, BorderLayout.NORTH);
        messsageHolder.add(messageScrollPane, BorderLayout.CENTER);
        JPanel panel = new JPanel();
        BoxLayout b = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(b);
        panel.add(buttonHolder);
        panel.add(channelNameHolder);
        panel.add(expressionHolder);
        panel.add(channelHolder);
        panel.add(messsageHolder);
        return panel;
    }

    private void adjustButtons(boolean check, boolean reset, boolean details) {
        checkButton.setEnabled(check);
        resetButton.setEnabled(reset);
        detailsButton.setEnabled(details);
    }

    public void dragOver(DropTargetDragEvent e) {
        super.dragOver(e);
        disableExpressionPanel();
    }

    public void drop(DropTargetDropEvent e) {
        super.drop(e);
        enableExpressionPanel();
    }

    private void disableExpressionPanel() {
        expressionPanel.setEditable(false);
    }

    private void enableExpressionPanel() {
        expressionPanel.setEditable(true);
    }

    private void setMessage(String text) {
        statusMessagePanel.setText(text + "\n");
    }

    private void addMessage(String text) {
        statusMessagePanel.append(text + "\n");
    }

    private void setDetailFromException(Throwable e) {
        CharArrayWriter buf = new CharArrayWriter();
        PrintWriter out = new PrintWriter(buf);
        e.printStackTrace(out);
        detailString = buf.toString();
    }

    public String[] getSupportedMimeTypes() {
        return new String[] { "application/octet-stream" };
    }

    public boolean supportsMultipleChannels() {
        return true;
    }

    public boolean addChannel(Channel channel) {
        String channelName = channel.getName();
        String unit = channel.getUnit();
        if (!super.addChannel(channel)) {
            return false;
        }
        int n = channelList.size();
        channelList.addElement(new ChannelCover(channel));
        channelView.ensureIndexIsVisible(n);
        if (channelsOk()) ready(); else notReady();
        return true;
    }

    private void removeSelectedChannel(ChannelCover ch) {
        channelList.removeElement(ch);
        removeChannel(ch.getName());
        if (channelsOk()) ready(); else notReady();
    }

    public void postTime(double time) {
        if (!running) return;
        super.postTime(time);
        if (channelMap == null) {
            return;
        }
        Iterator i = channels.iterator();
        while (i.hasNext()) {
            String channelName = (String) i.next();
            int channelIndex = channelMap.GetIndex(channelName);
            if (channelIndex != -1) {
                Enumeration e = channelList.elements();
                ChannelCover cc = null;
                while (e.hasMoreElements() && (cc == null)) {
                    ChannelCover test = (ChannelCover) e.nextElement();
                    if (test.getName().equals(channelName)) cc = test;
                }
                if (cc == null) {
                    log.error("Unexpected failure to find channel cover for " + channelName);
                    return;
                }
                double[] times = channelMap.GetTimes(channelIndex);
                double[] data = channelMap.GetDataAsFloat64(channelIndex);
                cc.addData(times, data);
                process();
            }
        }
    }

    private void process() {
        if (channelList == null) {
            log.error("Unexpected empty channel list");
            return;
        }
        ChannelCover[] cc = new ChannelCover[channelList.size()];
        for (int i = 0; i < cc.length; i++) {
            cc[i] = (ChannelCover) channelList.getElementAt(i);
        }
        double[] expressionData = new double[cc.length];
        double results = 0.0;
        boolean ok = true;
        while (ok) {
            for (int i = 0; i < cc.length; i++) ok = ok && cc[i].advance(front);
            if (!ok) break;
            for (int i = 0; i < cc.length; i++) {
                expressionData[i] = cc[i].getData();
                cc[i].increment();
                if (cc[i].getTime() > front) front = cc[i].getTime();
            }
            parseTree.setValues(expressionData);
            try {
                results = 0.0;
                results = parseTree.eval();
            } catch (Throwable t) {
                setMessage("Error in evaulation of expression");
                setDetailFromException(t);
                notReady();
                return;
            }
            postOutData(front, results);
        }
    }

    private boolean allHaveData() {
        Enumeration e = channelList.elements();
        while (e.hasMoreElements()) {
            ChannelCover test = (ChannelCover) e.nextElement();
            if (!test.hasData()) return false;
        }
        return true;
    }

    static final int OUT_SIZE = 10;

    int outIndex = 0;

    double[] outTime = new double[OUT_SIZE];

    double[] outData = new double[OUT_SIZE];

    private void postOutData(double time, double data) {
        outTime[outIndex] = time;
        outData[outIndex] = data;
        outIndex++;
        if (outIndex >= outTime.length) {
            outIndex = 0;
            if (connected()) {
                try {
                    double duration = outTime[outTime.length - 1] - outTime[0];
                    ChannelMap outMap = new ChannelMap();
                    int index = outMap.Add(outChannelName);
                    outMap.PutMime(index, "application/octet-stream");
                    outMap.PutTime(outTime[0], duration);
                    outMap.PutTimes(outTime);
                    outMap.PutDataAsFloat64(index, outData);
                    source.Flush(outMap);
                } catch (SAPIException e) {
                    setDetailFromException(e);
                    setMessage("Failed to send results. Exception = " + e.getMessage() + ". See Details.");
                    notReady();
                }
            } else {
                log.error("Unexpected disconnected Source.");
                notReady();
            }
        }
    }

    public void postData(ChannelMap channelMap) {
        super.postData(channelMap);
    }

    public void postState(int s1, int s2) {
        super.postState(s1, s2);
    }

    void clearData() {
    }

    public String toString() {
        return "Expression Data Panel";
    }

    public void actionPerformed(ActionEvent ev) {
        if (!(ev.getSource() instanceof JButton)) return;
        String arg = ev.getActionCommand();
        log.info("Called actionPerformed " + arg);
        if (arg.equals(CHECK)) {
            if (parseExpression() && channelsOk()) ready(); else notReady();
        } else if (arg.equals(RESET)) {
            notReady();
        } else if (arg.equals(DETAILS)) {
            new DetailDialog(detailString);
            detailString = null;
            notReady();
        }
    }

    private void notReady() {
        boolean details = (detailString != null);
        adjustButtons(true, false, details);
        parseTree = null;
        disconnect();
        running = false;
    }

    private void ready() {
        if (!setup()) {
            notReady();
            return;
        }
        adjustButtons(false, true, false);
        running = true;
        setMessage("Expression is ready to evaluate; " + "start data to see output on " + outSourceName + "/" + outChannelName + ".");
        connect();
    }

    private boolean setup() {
        if ((parseTree == null) && (!parseExpression())) return false;
        if (!channelsOk()) return false;
        outSourceName = sourceNameField.getText().trim();
        outChannelName = channelNameField.getText().trim();
        return true;
    }

    private boolean parseExpression() {
        String exStr = expressionPanel.getText().trim();
        try {
            parseTree = new Expression(new java.io.BufferedReader(new java.io.StringReader(exStr)));
            parseTree.setup();
        } catch (Throwable e) {
            parseTree = null;
            setMessage("The parse of expresison failed (" + exStr + ") -- click on 'Details' for details.\n");
            setDetailFromException(e);
            return false;
        }
        log.info("parse expresion ok");
        return true;
    }

    private boolean channelsOk() {
        log.info("Check channels...");
        if (parseTree == null) {
            setMessage("Expression not set up in parse tree. Use '" + CHECK + "'.");
            return false;
        }
        try {
            boolean[] expressionChannels = parseTree.getChannelList();
            if (expressionChannels.length > channelList.size()) {
                if (channelList.size() == 0) setMessage("Channels not set. Drag and Drop channels to channel list."); else setMessage("Missing some channel or channels needed in the expression");
                return false;
            }
        } catch (Throwable e) {
            setDetailFromException(e);
            setMessage("The parsed expression tree failed to return clannel list.\n" + "Click on 'Details' for details.\n");
            return false;
        }
        return true;
    }

    public void valueChanged(ListSelectionEvent ev) {
        if (ev.getValueIsAdjusting()) return;
        ChannelCover channel = (ChannelCover) channelView.getSelectedValue();
        if (channel == null) return;
        log.info("list selection: channel = " + channel);
        removeSelectedChannel(channel);
    }

    private boolean connect() {
        if (connected()) return true;
        try {
            source = new Source();
            source.OpenRBNBConnection(DataViewer.getRBNBHostName() + ":" + DataViewer.getRBNBPort(), outSourceName);
        } catch (SAPIException e) {
            setDetailFromException(e);
            e.printStackTrace();
            disconnect();
            return false;
        }
        return true;
    }

    private boolean connected() {
        if (source == null) return false;
        return source.VerifyConnection();
    }

    private void disconnect() {
        if (source != null) source.CloseRBNBConnection();
        source = null;
    }

    private class ChannelCover {

        private Channel channel;

        private Vector keep = new Vector();

        private double[] currentTime;

        private double[] currentData;

        private int index = -1;

        ;

        ChannelCover(Channel c) {
            channel = c;
        }

        public Channel getChannel() {
            return channel;
        }

        public String getName() {
            return channel.getName();
        }

        public String toString() {
            return getName();
        }

        public boolean hasData() {
            return currentTime != null;
        }

        public double getData() {
            return currentData[index];
        }

        public double getTime() {
            return currentTime[index];
        }

        public void addData(double[] time, double[] data) {
            if (currentTime == null) {
                currentTime = time;
                currentData = data;
                index = 0;
                return;
            }
            keep.addElement(new DataHolder(time, data));
            return;
        }

        public boolean hasNext() {
            if (currentTime == null) return false;
            if (index + 1 < currentTime.length) return true;
            if (keep.size() > 0) return true;
            return false;
        }

        public void increment() {
            if (!hasData()) {
                index = -1;
                return;
            }
            index++;
            if (index < currentTime.length) return;
            if (keep.size() == 0) {
                currentTime = null;
                currentData = null;
                index = -1;
                return;
            }
            DataHolder h = (DataHolder) keep.firstElement();
            keep.removeElementAt(0);
            currentTime = h.time;
            currentData = h.data;
            index = 0;
        }

        public boolean advance(double front) {
            while (true) {
                if (!hasData()) return false;
                if (!hasNext()) return false;
                if (nextTime() > front) return true;
                increment();
            }
        }

        public double nextTime() {
            int probe = index + 1;
            if (probe < currentTime.length) return currentTime[probe];
            if (keep.size() == 0) log.error("in nextTime with empty keep and no data!");
            return ((DataHolder) keep.firstElement()).time[0];
        }
    }

    private class DataHolder {

        double[] time;

        double[] data;

        DataHolder(double[] t, double[] d) {
            time = t;
            data = d;
        }
    }

    public class DetailDialog extends JDialog implements KeyEventDispatcher {

        public DetailDialog(String detail) {
            super();
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter() {

                public void windowActivated(WindowEvent e) {
                    bindKeys();
                }

                public void windowDeactivated(WindowEvent e) {
                    unbindKeys();
                }
            });
            setTitle("Error Detail");
            JLabel label = new JLabel("Details:");
            JTextArea ta = new JTextArea();
            ta.setText(detail);
            ta.setEditable(false);
            JScrollPane sp = new JScrollPane(ta);
            JPanel container = new JPanel();
            container.add(sp);
            getContentPane().add(container, BorderLayout.CENTER);
            pack();
            setVisible(true);
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
    }
}
