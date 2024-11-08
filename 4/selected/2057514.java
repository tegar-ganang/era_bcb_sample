package uk.co.westhawk.examplev2c;

import uk.co.westhawk.snmp.stack.*;
import uk.co.westhawk.snmp.pdu.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import prefuse.*;
import prefuse.data.*;
import prefuse.data.util.*;
import prefuse.visual.*;
import prefuse.render.*;
import prefuse.activity.*;
import prefuse.action.*;
import prefuse.action.layout.*;
import prefuse.action.layout.graph.*;
import prefuse.action.assignment.*;
import prefuse.util.*;
import prefuse.util.collections.*;
import prefuse.data.expression.*;
import prefuse.visual.expression.*;
import prefuse.controls.*;

/**
 * <p>
 * This class is written to test the Asterisk host functionality.
 * </p>
 *
 * <p>
 * It walks the tree by creating a new AsteriskChanTablePdu out off the
 * previous one, and it collects the values of all the channels.
 * </p>
 *
 * <p>
 * The information will be printed to System.out .
 * </p>
 *
 * <p>
 * The host, port, community name and sockettype can be configured in the
 * properties file.
 * The name of the properties file can be passed as first argument to
 * this application. If there is no such argument, it will look for
 * <code>MonitorAsteriskGraph.properties</code>. If this file does not exist, the
 * application will use default parameters.
 * </p>
 *
 * @author <a href="mailto:snmp@westhawk.co.uk">Birgit Arkesteijn</a>
 * @version $Revision: 1.2 $ $Date: 2006/06/19 10:52:16 $
 */
public class MonitorAsteriskGraph extends JPanel implements Observer, Runnable, WindowListener {

    private static final String version_id = "@(#)$Id: MonitorAsteriskGraph.java,v 1.2 2006/06/19 10:52:16 birgit Exp $ Copyright Westhawk Ltd";

    /**
     * Use 2 (sec) as interval
     */
    public static final long SLEEPTIME = 2000;

    private boolean _mayLoopStart;

    private AsteriskChanTypeTablePdu _atPdu;

    private AsteriskChanTablePdu _aPdu;

    private String _host;

    private int _port;

    private SnmpContextv2c _context;

    private Util _util;

    private HashMap _channelTypeMap = new HashMap();

    private HashMap _oldActiveChannelMap;

    private HashMap _activeChannelMap;

    private HashMap _activeChannelNameMap;

    public static final String CTYPE = "channelType";

    public static final String ACHAN = "activeChannel";

    public static final String ISACT = "isActiveChannel";

    public static final String GNAME = "graph";

    public static final String EDGES = "graph.edges";

    public static final String NODES = "graph.nodes";

    public static final String LAYOUT = "layout";

    public static final String COLOUR = "colour";

    public static final String FONT = "font";

    public static final String REPAINT = "repaint";

    private Visualization _viz;

    private Graph _graph;

    private Display _display;

    private Predicate _activeChannelPred;

    /**
 * Constructor.
 *
 * @param propertiesFilename The name of the properties file. Can be
 * null.
 */
    public MonitorAsteriskGraph(String propertiesFilename) {
        _util = new Util(propertiesFilename, this.getClass().getName());
        createGraph();
        this.setLayout(new BorderLayout());
        this.add(_display, BorderLayout.CENTER);
    }

    private void createGraph() {
        _viz = new Visualization();
        _viz.setInteractive(EDGES, null, false);
        _display = new Display(_viz);
        _display.setForeground(Color.GRAY);
        _display.setBackground(Color.WHITE);
        _graph = new Graph(true);
        VisualGraph vg = _viz.addGraph(GNAME, _graph);
        Table nodesTable = _graph.getNodeTable();
        nodesTable.addColumn(CTYPE, String.class);
        nodesTable.addColumn(ACHAN, String.class);
        nodesTable.addColumn(ISACT, boolean.class);
        Table edgeTable = _graph.getEdgeTable();
        edgeTable.addColumn(ISACT, boolean.class);
        LabelRenderer rt = new LabelRenderer(CTYPE);
        rt.setHorizontalPadding(10);
        rt.setVerticalPadding(10);
        LabelRenderer ar = new LabelRenderer(ACHAN);
        ar.setHorizontalPadding(5);
        ar.setVerticalPadding(5);
        ar.setRoundedCorner(8, 8);
        _activeChannelPred = new ColumnExpression(ISACT);
        DefaultRendererFactory drf = new DefaultRendererFactory();
        drf.setDefaultRenderer(rt);
        drf.add(_activeChannelPred, ar);
        _viz.setRendererFactory(drf);
        HoverPredicate hoverPred = new HoverPredicate();
        ColorAction nStroke = new ColorAction(NODES, VisualItem.STROKECOLOR);
        nStroke.setDefaultColor(ColorLib.color(Color.BLACK));
        ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR);
        nFill.setDefaultColor(ColorLib.color(Color.PINK));
        nFill.add(hoverPred, ColorLib.color(Color.LIGHT_GRAY));
        nFill.add(_activeChannelPred, ColorLib.color(Color.CYAN));
        ColorAction nText = new ColorAction(NODES, VisualItem.TEXTCOLOR);
        nText.setDefaultColor(ColorLib.color(Color.BLACK));
        nText.add(hoverPred, ColorLib.color(Color.RED));
        ColorAction eStroke = new ColorAction(EDGES, VisualItem.STROKECOLOR);
        eStroke.setDefaultColor(ColorLib.color(Color.BLACK));
        ColorAction eFill = new ColorAction(EDGES, VisualItem.FILLCOLOR);
        eFill.setDefaultColor(ColorLib.color(Color.BLACK));
        ActionList colours = new ActionList();
        colours.add(nStroke);
        colours.add(nFill);
        colours.add(nText);
        colours.add(eStroke);
        colours.add(eFill);
        _viz.putAction(COLOUR, colours);
        ActionList repaint = new ActionList();
        repaint.add(colours);
        repaint.add(new RepaintAction());
        _viz.putAction(REPAINT, repaint);
        FontAction fonts = new FontAction(NODES, FontLib.getFont("Tahoma", Font.BOLD, 12));
        ActionList font = new ActionList();
        font.add(fonts);
        _viz.putAction(FONT, font);
        ActionList layout = new ActionList(Activity.DEFAULT_STEP_TIME);
        layout.add(new ForceDirectedLayout(GNAME));
        layout.add(new CircleLayout(GNAME));
        _viz.putAction(LAYOUT, layout);
        _display.addControlListener(new HoverActionControl(REPAINT));
        _display.addControlListener(new ControlAdapter() {

            public void itemEntered(VisualItem item, MouseEvent e) {
                if (item.canGetString(ACHAN)) {
                    String txt = item.getString(ACHAN);
                    if (txt != null) {
                        showActiveChannelInfo(txt);
                    }
                }
            }

            public void itemExited(VisualItem item, MouseEvent e) {
            }
        });
    }

    public void init() {
        AsnObject.setDebug(6);
        _host = _util.getHost();
        String bindAddr = _util.getBindAddress();
        _port = _util.getPort(SnmpContextBasisFace.DEFAULT_PORT);
        String socketType = _util.getSocketType();
        String community = _util.getCommunity();
        try {
            _context = new SnmpContextv2c(_host, _port, bindAddr, socketType);
            _context.setCommunity(community);
            System.out.println("context: " + _context.toString());
        } catch (IOException exc) {
            System.out.println("IOException: " + exc.getMessage());
            System.exit(0);
        }
    }

    /** 
 * Sends a request, asking for the channels types. The types shouldn't
 * change during the lifetime of the agent.
 */
    public void getChannelTypes(SnmpContextBasisFace con, AsteriskChanTypeTablePdu prev) {
        _atPdu = new AsteriskChanTypeTablePdu(con);
        _atPdu.addObserver(this);
        _atPdu.addOids(prev);
        try {
            _atPdu.send();
        } catch (java.io.IOException exc) {
            System.out.println("getChannelTypes(): IOException " + exc.getMessage());
        } catch (uk.co.westhawk.snmp.stack.PduException exc) {
            System.out.println("getChannelTypes(): PduException " + exc.getMessage());
        }
    }

    /** 
 * Sends a request, asking for the active channels. The active channels
 * change by nature.
 */
    public void getActiveChannels(SnmpContextBasisFace con, AsteriskChanTablePdu prev) {
        _aPdu = new AsteriskChanTablePdu(con);
        _aPdu.addObserver(this);
        _aPdu.addOids(prev);
        try {
            _aPdu.send();
        } catch (java.io.IOException exc) {
            System.out.println("getActiveChannels(): IOException " + exc.getMessage());
        } catch (uk.co.westhawk.snmp.stack.PduException exc) {
            System.out.println("getActiveChannels(): PduException " + exc.getMessage());
        }
    }

    public void start() {
        if (_context != null) {
            getChannelTypes(_context, null);
        }
    }

    protected void startThread() {
        _mayLoopStart = true;
        Thread me = new Thread(this);
        me.setPriority(Thread.MIN_PRIORITY);
        me.start();
    }

    public void run() {
        while (_context != null) {
            if (_mayLoopStart == true) {
                _mayLoopStart = false;
                _oldActiveChannelMap = _activeChannelMap;
                _activeChannelMap = new HashMap();
                _activeChannelNameMap = new HashMap();
                getActiveChannels(_context, null);
            }
            try {
                Thread.sleep(SLEEPTIME);
            } catch (InterruptedException ix) {
                ;
            }
        }
    }

    /**
 * Implementing the Observer interface. 
 * Receiving the response from getChannelTypes() or getActiveChannels().
 *
 * @param obs the pdu variable
 * @param ov the array of varbind (not used)
 *
 * @see AsteriskChanTypeTablePdu
 * @see AsteriskChanTablePdu
 * @see #getChannelTypes
 * @see #getActiveChannels
 */
    public void update(Observable obs, Object ov) {
        if (obs instanceof AsteriskChanTypeTablePdu) {
            handleChannelType(obs, ov);
        } else {
            handleActiveChannel(obs, ov);
        }
    }

    protected void handleChannelType(Observable obs, Object ov) {
        AsteriskChanTypeTablePdu prev;
        int errStatus = _atPdu.getErrorStatus();
        int errIndex = _atPdu.getErrorIndex();
        if (errStatus == AsnObject.SNMP_ERR_NOERROR) {
            prev = _atPdu;
            String name = _atPdu.getAstChanTypeName();
            Node cTypeNode = _graph.addNode();
            cTypeNode.setString(CTYPE, name);
            cTypeNode.setBoolean(ISACT, false);
            _channelTypeMap.put(name, new Integer(cTypeNode.getRow()));
            getChannelTypes(_context, prev);
        } else {
            _viz.run(FONT);
            _viz.run(LAYOUT);
            _viz.run(COLOUR);
            _viz.run(REPAINT);
            startThread();
        }
    }

    protected void handleActiveChannel(Observable obs, Object ov) {
        int errStatus = _aPdu.getErrorStatus();
        int errIndex = _aPdu.getErrorIndex();
        if (errStatus == AsnObject.SNMP_ERR_NOERROR) {
            handleActiveChannelUp(obs, ov);
        } else if (errStatus == AsnObject.SNMP_ERR_NOSUCHNAME) {
            if (errIndex == 0) {
                _mayLoopStart = true;
            } else if (errIndex == 1) {
                Integer value = _aPdu.getAstNumChannels();
                if (value.intValue() == 0) {
                } else {
                }
                _mayLoopStart = true;
            } else {
                handleActiveChannelUp(obs, ov);
            }
        } else {
            _mayLoopStart = true;
        }
        if (_mayLoopStart == true) {
            redrawActiveChannelsInGraph();
        }
    }

    protected void handleActiveChannelUp(Observable obs, Object ov) {
        AsteriskChanTablePdu prev;
        prev = _aPdu;
        Integer chanIndexI = _aPdu.getAstChanIndex();
        String channelName = _aPdu.getAstChanName();
        _activeChannelMap.put(chanIndexI, _aPdu);
        getActiveChannels(_context, prev);
    }

    protected void redrawActiveChannelsInGraph() {
        IntIterator it;
        Table nodesTable = _graph.getNodeTable();
        it = nodesTable.rows(_activeChannelPred);
        while (it.hasNext()) {
            int row = it.nextInt();
            _graph.removeNode(row);
        }
        it = _graph.edgeRows();
        while (it.hasNext()) {
            int edge = it.nextInt();
            _graph.removeEdge(edge);
        }
        Set keyset;
        Iterator i;
        keyset = _activeChannelMap.keySet();
        i = keyset.iterator();
        while (i.hasNext()) {
            Integer index = (Integer) i.next();
            AsteriskChanTablePdu pdu = (AsteriskChanTablePdu) _activeChannelMap.get(index);
            String channelType = pdu.getAstChanType();
            Integer channelTypeNodeRowId = (Integer) _channelTypeMap.get(channelType);
            Node cTypeNode = _graph.getNode(channelTypeNodeRowId.intValue());
            String channelName = pdu.getAstChanName();
            Node aChanNode = _graph.addNode();
            aChanNode.setString(ACHAN, channelName);
            aChanNode.setBoolean(ISACT, true);
            int thisNodeIndex = aChanNode.getRow();
            _activeChannelNameMap.put(channelName, new Integer(thisNodeIndex));
            Edge edge1 = _graph.addEdge(aChanNode, cTypeNode);
            edge1.setBoolean(ISACT, false);
        }
        keyset = _activeChannelMap.keySet();
        i = keyset.iterator();
        while (i.hasNext()) {
            Integer index = (Integer) i.next();
            AsteriskChanTablePdu pdu = (AsteriskChanTablePdu) _activeChannelMap.get(index);
            String channelName = pdu.getAstChanName();
            String bridgedChan = pdu.getAstChanBridge();
            Object nameNodeO = _activeChannelNameMap.get(channelName);
            Object bridgedChanO = _activeChannelNameMap.get(bridgedChan);
            if (nameNodeO != null && bridgedChanO != null) {
                Integer nameNodeInd = (Integer) nameNodeO;
                Integer bridgedChanInd = (Integer) bridgedChanO;
                Node nodeA = _graph.getNode(nameNodeInd.intValue());
                Node nodeB = _graph.getNode(bridgedChanInd.intValue());
                Edge edge2 = _graph.addEdge(nodeA, nodeB);
                edge2.setBoolean(ISACT, false);
                Edge edge3 = _graph.addEdge(nodeB, nodeA);
                edge3.setBoolean(ISACT, false);
            }
        }
        _viz.run(FONT);
        _viz.run(LAYOUT);
        _viz.run(COLOUR);
        _viz.run(REPAINT);
    }

    protected void showActiveChannelInfo(String channelName) {
        Set keyset;
        Iterator i;
        keyset = _activeChannelMap.keySet();
        i = keyset.iterator();
        while (i.hasNext()) {
            Integer index = (Integer) i.next();
            AsteriskChanTablePdu pdu = (AsteriskChanTablePdu) _activeChannelMap.get(index);
            String thisChannelName = pdu.getAstChanName();
            if (channelName.equals(thisChannelName)) {
                System.out.println(pdu.toString());
            }
        }
    }

    private void printNodeTableInfo() {
        Table nodesTable = _graph.getNodeTable();
        TableIterator it = nodesTable.iterator();
        System.out.println(getClass().getName() + ".printNodeTableInfo():");
        while (it.hasNext()) {
            int row = it.nextInt();
            Tuple t = nodesTable.getTuple(row);
            int cno = t.getColumnCount();
            System.out.println("row=" + row + ", #col=" + cno);
            for (int col = 0; col < cno; col++) {
                String name = t.getColumnName(col);
                Class cl = t.getColumnType(col);
                System.out.println("row=" + row + ", col=" + col + ": name=" + name + ", class=" + cl.getName());
            }
        }
        int nno = _graph.getNodeCount();
        for (int i = 0; i < nno; i++) {
            int ind = _graph.getNodeIndex(i);
            Node n = _graph.getNodeFromKey(i);
            System.out.println("i=" + i + ", ind=" + ind + ", node=" + n.toString());
        }
    }

    private void printMap(Map map) {
        Set keyset = map.keySet();
        Iterator i = keyset.iterator();
        while (i.hasNext()) {
            Object key = i.next();
            Object value = map.get(key);
            System.out.println(key.toString() + " = " + value.toString());
        }
    }

    public void freeResources() {
        if (_context != null) {
            _mayLoopStart = false;
            _context.destroy();
            _context = null;
        }
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        this.freeResources();
        System.exit(0);
    }

    /**
 * Main. To use a properties file different from
 * <code>MonitorAsteriskGraph.properties</code>, pass the name as first argument.
 */
    public static void main(String[] args) {
        String propFileName = null;
        if (args.length > 0) {
            propFileName = args[0];
        }
        MonitorAsteriskGraph application = new MonitorAsteriskGraph(propFileName);
        JFrame frame = new JFrame();
        frame.setTitle(application.getClass().getName());
        frame.getContentPane().add(application, BorderLayout.CENTER);
        application.init();
        frame.addWindowListener(application);
        Dimension dim = new Dimension(600, 600);
        frame.setSize(dim);
        application.setSize(dim);
        frame.setVisible(true);
        application.start();
    }
}
