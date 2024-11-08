package org.convey;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import com.echomine.jabber.*;
import com.echomine.common.*;
import com.echomine.net.*;
import org.jdom.*;
import org.jdom.input.*;
import org.jdom.output.*;
import java.io.*;
import java.util.*;
import java.net.*;

/**
 * @author Andy Ames
 * @version 0.3
 */
public class ConveyFrame extends JFrame implements ChangeListener {

    private static ConveyFrame conveyFrame = null;

    protected ColorComboBox fillColorComboBox;

    protected ColorComboBox strokeColorComboBox;

    protected ColorComboBox textColorComboBox;

    /**
	 * Jabber connect action. Fired when the user selects the Jabber "Connect"
	 * tool.
	 */
    protected Action connectAction;

    protected JButton connectButton;

    /**
	 * Jabber disconnect action. Fired when the user selects the Jabber
	 * "Disconnect" tool.
	 */
    protected Action disconnectAction;

    protected JButton disconnectButton;

    /**
	 * Jabber Chat action. Fired when the user selects the Jabber "Chat" tool.
	 */
    protected Action chatAction;

    protected JButton chatButton;

    protected Action newCanvasAction;

    protected JButton newCanvasButton;

    protected Action exitAction;

    protected Action reportAction;

    protected Button reportButton;

    protected ConnectDialog connectDialog;

    protected ChatDialog chatDialog;

    protected JabberSession session = null;

    protected JabberMessageListener listener = null;

    protected ActionListener chatActionListener = null;

    protected java.util.Timer timer = new java.util.Timer();

    protected Hashtable jabberCanvasPanels = new Hashtable();

    protected transient Vector clipboardGraphics = new Vector();

    protected JTabbedPaneWithCloseIcons tabbedPane;

    protected Box toolBarBox;

    protected JFileChooser fileChooser = null;

    protected JTextField status;

    /**
	 * Graphic select action. Fired when the user selects the Graphic "Select"
	 * tool.
	 */
    protected Action selectAction;

    protected JToggleButton selectButton = null;

    protected JRadioButtonMenuItem selectMenuItem = null;

    /**
	 * Graphic scale action. Fired when the user selects the Graphic
	 * "Scale" tool.
	 */
    protected Action scaleAction;

    protected JToggleButton scaleButton = null;

    protected JRadioButtonMenuItem scaleMenuItem = null;

    /**
	 * Graphic create text action. Fired when the user selects the create
	 * "Text" tool.
	 */
    protected Action createTextAction;

    protected JToggleButton createTextButton = null;

    protected JRadioButtonMenuItem textMenuItem = null;

    /**
	 * Graphic create rectangle action. Fired when the user selects the create
	 * "Rectangle" tool.
	 */
    protected Action createRectangleAction;

    protected JToggleButton createRectangleButton = null;

    protected JRadioButtonMenuItem rectangleMenuItem = null;

    /**
	 * Graphic create rectangle action. Fired when the user selects the create
	 * "Ellipse" tool.
	 */
    protected Action createEllipseAction;

    protected JToggleButton createEllipseButton = null;

    protected JRadioButtonMenuItem ellipseMenuItem = null;

    /**
	 * Graphic create rectangle action. Fired when the user selects the create
	 * "Polyline" tool.
	 */
    protected Action createPolylineAction;

    protected JToggleButton createPolylineButton = null;

    protected JRadioButtonMenuItem polylineMenuItem = null;

    /**
	 * Graphic create rectangle action. Fired when the user selects the create
	 * "Polygon" tool.
	 */
    protected Action createPolygonAction;

    protected JToggleButton createPolygonButton = null;

    protected JRadioButtonMenuItem polygonMenuItem = null;

    /**
	 * Graphic create rectangle action. Fired when the user selects the create
	 * "QCurve" tool.
	 */
    protected Action createQuadCurveAction;

    protected JToggleButton createQuadCurveButton = null;

    protected JRadioButtonMenuItem quadcurveMenuItem = null;

    /**
	 * Graphic create rectangle action. Fired when the user selects the create
	 * "CCurve" tool.
	 */
    protected Action createCubicCurveAction;

    protected JToggleButton createCubicCurveButton = null;

    protected JRadioButtonMenuItem cubicMenuItem = null;

    /**
	 * Graphic create fitted curve action. Fired when the user selects the create
	 * "Fitted Curve" tool.
	 */
    protected Action createFittedCurveAction;

    protected JToggleButton createFittedCurveButton = null;

    protected JRadioButtonMenuItem fittedcurveMenuItem = null;

    /**
	 * Graphic create freehand curve action. Fired when the user selects the create
	 * "Freehand Curve" tool.
	 */
    protected Action createFreehandCurveAction;

    protected JToggleButton createFreehandCurveButton = null;

    protected JRadioButtonMenuItem freehandcurveMenuItem = null;

    /**
         * Graphic create grid action. Fired when the user selects the create
         * "Grid" tool.
         */
    protected Action createGridAction;

    protected JToggleButton createGridButton = null;

    protected JRadioButtonMenuItem gridMenuItem = null;

    /**
	 * Graphic group action. Fired when the user selects the graphic "Group" tool.
	 */
    protected Action groupAction;

    protected JButton groupButton;

    /**
	 * Graphic ungroup action. Fired when the user selects the graphic "Ungroup"
	 * tool.
	 */
    protected Action ungroupAction;

    protected JButton ungroupButton;

    /**
	 * Graphic cut action. Fired when the user selects the graphic "Cut"
	 * tool.
	 */
    protected Action cutAction;

    protected JButton cutButton;

    /**
	 * Graphic copy action. Fired when the user selects the graphic "Copy"
	 * tool.
	 */
    protected Action copyAction;

    protected JButton copyButton;

    /**
	 * Graphic paste action. Fired when the user selects the graphic "Paste"
	 * tool.
	 */
    protected Action pasteAction;

    protected JButton pasteButton;

    protected Action selectAllAction;

    /**
	 * Graphic delete action. Fired when the user selects the graphic "Delete"
	 * tool.
	 */
    protected Action deleteAction;

    protected JButton deleteButton;

    /**
	 * Graphic clear action. Fired when the user selects the graphic "Clear"
	 * tool.
	 */
    protected Action clearAction;

    protected JButton clearButton;

    protected Action bringForwardAction;

    protected JButton bringForwardButton;

    protected Action sendBackwardAction;

    protected JButton sendBackwardButton;

    protected Action bringToFrontAction;

    protected JButton bringToFrontButton;

    protected Action sendToBackAction;

    protected JButton sendToBackButton;

    protected Action yieldPenAction;

    protected JButton yieldPenButton;

    protected Action requestPenAction;

    protected JButton requestPenButton;

    protected Action revokePenAction;

    protected JButton revokePenButton;

    protected Action autoSendAction;

    protected JToggleButton autoSendButton;

    protected JRadioButtonMenuItem autoSendMenuItem;

    /**
	 * Open existing file action. Fired when the user selects open in the
	 * file menu or the open button.
	 *
	 */
    protected Action openAction;

    protected JButton openButton;

    /**
	 * Save file action. Fired when the user selects save in the
	 * file menu or the save button.
	 */
    protected Action saveAction;

    protected JButton saveButton;

    /**
	 * SaveAs file action. Fired when the user selects saveas in the
	 * file menu.
	 */
    protected Action saveAsAction;

    protected JButton saveAsButton;

    protected Action closeAction;

    protected JButton closeButton;

    /**
	 * Jabber user account that is connected. This is non-null when the user
	 * is connected, regardless of whether they are sending or receiving messages.
	 * <p>
	 * This field consists of the username, an ampersand, and the server
	 * (johndoe@jabber.org). The Jabbber resource is not included.
	 * (johndoe@jabber.org/Convey is incorrect.)
	 */
    protected String from = null;

    /**
	 * Constructor
	 */
    public ConveyFrame() {
        super("Convey [disconnected]");
        Dimension dim = super.getToolkit().getScreenSize();
        int w = (int) (dim.getWidth() * 3.0 / 4.0);
        int h = (int) (dim.getHeight() * 4.0 / 5.0);
        super.setSize(w, h);
        super.setLocation((int) (dim.getWidth() / 2.0 - w / 2.0), (int) (dim.getHeight() / 2.0 - h / 2.0));
        super.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        WindowListener l = new WindowAdapter() {

            public void windowClosing(WindowEvent evt) {
                if (disconnect(true)) {
                    saveConfig();
                    System.exit(0);
                }
            }
        };
        super.addWindowListener(l);
        super.getContentPane().setLayout(new BorderLayout(5, 5));
        JMenuBar menuBar = new JMenuBar();
        JMenu conveyMenu = new JMenu("Convey");
        conveyMenu.setMnemonic('C');
        JToolBar conveyToolBar = new JToolBar(JToolBar.HORIZONTAL);
        conveyToolBar.setFloatable(false);
        this.connectAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/connect.png")) {

            public void actionPerformed(ActionEvent evt) {
                connect();
            }
        };
        this.connectButton = new JButton(this.connectAction);
        this.connectButton.setToolTipText("Connect...");
        this.connectButton.setMargin(new Insets(1, 1, 1, 1));
        this.connectAction.setEnabled(true);
        conveyToolBar.add(this.connectButton);
        JMenuItem connectMenuItem = new JMenuItem(this.connectAction);
        connectMenuItem.setText("Connect...");
        conveyMenu.add(connectMenuItem);
        this.disconnectAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/disconnect.png")) {

            public void actionPerformed(ActionEvent evt) {
                disconnect(true);
            }
        };
        this.disconnectButton = new JButton(this.disconnectAction);
        this.disconnectButton.setToolTipText("Disconnect");
        this.disconnectButton.setMargin(new Insets(1, 1, 1, 1));
        this.disconnectAction.setEnabled(false);
        conveyToolBar.add(this.disconnectButton);
        JMenuItem disconnectMenuItem = new JMenuItem(this.disconnectAction);
        disconnectMenuItem.setText("Disconnect");
        conveyMenu.add(disconnectMenuItem);
        this.chatAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/chat.png")) {

            public void actionPerformed(ActionEvent evt) {
                newChat();
            }
        };
        this.chatButton = new JButton(this.chatAction);
        this.chatButton.setToolTipText("Chat...");
        this.chatButton.setMargin(new Insets(1, 1, 1, 1));
        this.chatAction.setEnabled(false);
        conveyToolBar.add(this.chatButton);
        JMenuItem chatMenuItem = new JMenuItem(this.chatAction);
        chatMenuItem.setText("Chat...");
        conveyMenu.add(chatMenuItem);
        conveyMenu.addSeparator();
        conveyToolBar.addSeparator();
        this.autoSendAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/autosend.png")) {

            public void actionPerformed(ActionEvent evt) {
                if (evt.getSource() == autoSendButton) {
                    if (autoSendButton.isSelected()) {
                        Iterator iter = jabberCanvasPanels.entrySet().iterator();
                        while (iter.hasNext()) {
                            JabberCanvasPanel jabberCanvasPanel = (JabberCanvasPanel) ((Map.Entry) iter.next()).getValue();
                            jabberCanvasPanel.getGraphicPanel().flushGraphicEventQueue();
                        }
                    }
                    autoSendMenuItem.setSelected(autoSendButton.isSelected());
                } else {
                    if (autoSendMenuItem.isSelected()) {
                        Iterator iter = jabberCanvasPanels.entrySet().iterator();
                        while (iter.hasNext()) {
                            JabberCanvasPanel jabberCanvasPanel = (JabberCanvasPanel) ((Map.Entry) iter.next()).getValue();
                            jabberCanvasPanel.getGraphicPanel().flushGraphicEventQueue();
                        }
                    }
                    autoSendButton.setSelected(autoSendMenuItem.isSelected());
                }
            }
        };
        this.autoSendButton = new JToggleButton(this.autoSendAction);
        this.autoSendButton.setMargin(new Insets(1, 1, 1, 1));
        this.autoSendButton.setToolTipText("Auto Send");
        conveyToolBar.add(this.autoSendButton);
        this.autoSendMenuItem = new JRadioButtonMenuItem(this.autoSendAction);
        this.autoSendMenuItem.setText("Auto Send");
        this.autoSendButton.setSelected(true);
        this.autoSendMenuItem.setSelected(true);
        conveyMenu.add(autoSendMenuItem);
        conveyMenu.addSeparator();
        conveyToolBar.addSeparator();
        this.yieldPenAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/yieldpen.png")) {

            public void actionPerformed(ActionEvent evt) {
                CanvasPanel canvasPanel = getSelectedCanvasPanel();
                if (canvasPanel instanceof JabberCanvasPanel) ((JabberCanvasPanel) canvasPanel).penYield();
            }
        };
        this.yieldPenButton = new JButton(this.yieldPenAction);
        this.yieldPenButton.setToolTipText("Yield Pen");
        this.yieldPenButton.setMargin(new Insets(1, 1, 1, 1));
        this.yieldPenAction.setEnabled(false);
        conveyToolBar.add(this.yieldPenButton);
        JMenuItem yieldPenMenuItem = new JMenuItem(this.yieldPenAction);
        yieldPenMenuItem.setText("Yield Pen");
        conveyMenu.add(yieldPenMenuItem);
        this.requestPenAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/requestpen.png")) {

            public void actionPerformed(ActionEvent evt) {
                CanvasPanel canvasPanel = getSelectedCanvasPanel();
                if (canvasPanel instanceof JabberCanvasPanel) ((JabberCanvasPanel) canvasPanel).penRequest();
            }
        };
        this.requestPenButton = new JButton(this.requestPenAction);
        this.requestPenButton.setToolTipText("Request Pen");
        this.requestPenButton.setMargin(new Insets(1, 1, 1, 1));
        this.requestPenAction.setEnabled(false);
        conveyToolBar.add(this.requestPenButton);
        JMenuItem requestPenMenuItem = new JMenuItem(this.requestPenAction);
        requestPenMenuItem.setText("Request Pen");
        conveyMenu.add(requestPenMenuItem);
        this.revokePenAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/revokepen.png")) {

            public void actionPerformed(ActionEvent evt) {
                CanvasPanel canvasPanel = getSelectedCanvasPanel();
                if (canvasPanel instanceof JabberCanvasPanel) ((JabberCanvasPanel) canvasPanel).penRevoke();
            }
        };
        this.revokePenButton = new JButton(this.revokePenAction);
        this.revokePenButton.setToolTipText("Revoke Pen");
        this.revokePenButton.setMargin(new Insets(1, 1, 1, 1));
        this.revokePenAction.setEnabled(false);
        conveyToolBar.add(this.revokePenButton);
        JMenuItem revokePenMenuItem = new JMenuItem(this.revokePenAction);
        revokePenMenuItem.setText("Revoke Pen");
        conveyMenu.add(revokePenMenuItem);
        conveyMenu.addSeparator();
        this.exitAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/null.png")) {

            public void actionPerformed(ActionEvent evt) {
                if (disconnect(true)) {
                    saveConfig();
                    System.exit(0);
                }
            }
        };
        JMenuItem exitMenuItem = new JMenuItem(this.exitAction);
        exitMenuItem.setText("Exit");
        conveyMenu.add(exitMenuItem);
        menuBar.add(conveyMenu);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        JToolBar fileToolBar = new JToolBar(JToolBar.HORIZONTAL);
        fileToolBar.setFloatable(false);
        this.newCanvasAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/newcanvas.png")) {

            public void actionPerformed(ActionEvent evt) {
                newLocal();
            }
        };
        this.newCanvasButton = new JButton(this.newCanvasAction);
        this.newCanvasButton.setMargin(new Insets(1, 1, 1, 1));
        this.newCanvasButton.setToolTipText("New Canvas...");
        fileToolBar.add(this.newCanvasButton);
        JMenuItem newCanvasMenuItem = new JMenuItem(this.newCanvasAction);
        newCanvasMenuItem.setText("New Canvas...");
        fileMenu.add(newCanvasMenuItem);
        this.openAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/open.png")) {

            public void actionPerformed(ActionEvent evt) {
                fileOpen();
            }
        };
        this.openButton = new JButton(this.openAction);
        this.openButton.setMargin(new Insets(1, 1, 1, 1));
        this.openButton.setToolTipText("Open   Ctrl-O");
        fileToolBar.add(this.openButton);
        JMenuItem openMenuItem = new JMenuItem(this.openAction);
        openMenuItem.setText("Open...");
        openMenuItem.setMnemonic('O');
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK));
        fileMenu.add(openMenuItem);
        this.saveAsAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/save.png")) {

            public void actionPerformed(ActionEvent evt) {
                fileSaveAs();
            }
        };
        this.saveAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/save.png")) {

            public void actionPerformed(ActionEvent evt) {
                fileSave();
            }
        };
        this.saveButton = new JButton(this.saveAction);
        this.saveButton.setMargin(new Insets(1, 1, 1, 1));
        this.saveButton.setToolTipText("Save   Ctrl-S");
        fileToolBar.add(this.saveButton);
        JMenuItem saveMenuItem = new JMenuItem(this.saveAction);
        saveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK));
        saveMenuItem.setText("Save");
        saveMenuItem.setMnemonic('S');
        fileMenu.add(saveMenuItem);
        this.saveAsAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/null.png")) {

            public void actionPerformed(ActionEvent evt) {
                fileSaveAs();
            }
        };
        this.saveAsButton = new JButton(this.saveAsAction);
        this.saveAsButton.setMargin(new Insets(1, 1, 1, 1));
        JMenuItem saveAsMenuItem = new JMenuItem(this.saveAsAction);
        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.setMnemonic('v');
        fileMenu.add(saveAsMenuItem);
        fileMenu.addSeparator();
        this.closeAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/null.png")) {

            public void actionPerformed(ActionEvent evt) {
                int tab = tabbedPane.getSelectedIndex();
                if (tab > 0) tabbedPane.remove(tab);
            }
        };
        JMenuItem closeMenuItem = new JMenuItem(this.closeAction);
        closeMenuItem.setText("Close");
        closeMenuItem.setMnemonic('C');
        fileMenu.add(closeMenuItem);
        menuBar.add(fileMenu);
        JToolBar graphicToolBar = new JToolBar(JToolBar.HORIZONTAL);
        JMenu graphicMenu = new JMenu("Graphic");
        graphicMenu.setMnemonic('G');
        this.selectAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/select16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.SELECT);
                selectButton.setSelected(true);
                selectMenuItem.setSelected(true);
                status.setText("Move graphic or one of its vertices");
            }
        };
        this.selectButton = new JToggleButton(this.selectAction);
        this.selectButton.setMargin(new Insets(1, 1, 1, 1));
        this.selectButton.setToolTipText("Select   Ctrl-`");
        graphicToolBar.add(this.selectButton);
        graphicToolBar.setFloatable(false);
        this.selectMenuItem = new JRadioButtonMenuItem(this.selectAction);
        this.selectMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, Event.CTRL_MASK));
        this.selectMenuItem.setText("Select");
        this.selectMenuItem.setMnemonic('S');
        graphicMenu.add(selectMenuItem);
        this.scaleAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/scale16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.SCALE);
                scaleButton.setSelected(true);
                scaleMenuItem.setSelected(true);
                status.setText("Move vertex to scale graphic");
            }
        };
        this.scaleButton = new JToggleButton(this.scaleAction);
        this.scaleButton.setMargin(new Insets(1, 1, 1, 1));
        this.scaleButton.setToolTipText("Scale   Ctrl-1");
        graphicToolBar.add(this.scaleButton);
        this.scaleMenuItem = new JRadioButtonMenuItem(this.scaleAction);
        this.scaleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, Event.CTRL_MASK));
        this.scaleMenuItem.setText("Scale");
        this.scaleMenuItem.setMnemonic('a');
        graphicMenu.add(scaleMenuItem);
        graphicToolBar.addSeparator();
        graphicMenu.addSeparator();
        this.createTextAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/text16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.TEXT);
                createTextButton.setSelected(true);
                textMenuItem.setSelected(true);
                status.setText("Click mouse to create text box");
            }
        };
        this.createTextButton = new JToggleButton(this.createTextAction);
        this.createTextButton.setMargin(new Insets(1, 1, 1, 1));
        this.createTextButton.setToolTipText("Text   Ctrl-2");
        graphicToolBar.add(this.createTextButton);
        this.textMenuItem = new JRadioButtonMenuItem(this.createTextAction);
        this.textMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, Event.CTRL_MASK));
        this.textMenuItem.setText("Text");
        this.textMenuItem.setMnemonic('T');
        graphicMenu.add(textMenuItem);
        this.createRectangleAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/rectangle16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.RECTANGLE);
                createRectangleButton.setSelected(true);
                rectangleMenuItem.setSelected(true);
                status.setText("Drag mouse to create rectangle");
            }
        };
        this.createRectangleButton = new JToggleButton(this.createRectangleAction);
        this.createRectangleButton.setMargin(new Insets(1, 1, 1, 1));
        this.createRectangleButton.setToolTipText("Rectangle   Ctrl-3");
        graphicToolBar.add(this.createRectangleButton);
        this.rectangleMenuItem = new JRadioButtonMenuItem(this.createRectangleAction);
        this.rectangleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, Event.CTRL_MASK));
        this.rectangleMenuItem.setText("Rectangle");
        this.rectangleMenuItem.setMnemonic('R');
        graphicMenu.add(rectangleMenuItem);
        this.createEllipseAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/ellipse16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.ELLIPSE);
                createEllipseButton.setSelected(true);
                ellipseMenuItem.setSelected(true);
                status.setText("Drag mouse to create ellipse");
            }
        };
        this.createEllipseButton = new JToggleButton(this.createEllipseAction);
        this.createEllipseButton.setMargin(new Insets(1, 1, 1, 1));
        this.createEllipseButton.setToolTipText("Ellipse   Ctrl-4");
        graphicToolBar.add(this.createEllipseButton);
        this.ellipseMenuItem = new JRadioButtonMenuItem(this.createEllipseAction);
        this.ellipseMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_4, Event.CTRL_MASK));
        this.ellipseMenuItem.setText("Ellipse");
        this.ellipseMenuItem.setMnemonic('E');
        graphicMenu.add(ellipseMenuItem);
        this.createPolylineAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/polyline16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.POLYLINE);
                createPolylineButton.setSelected(true);
                polylineMenuItem.setSelected(true);
                status.setText("Click mouse to add polyline points (Shift+Click to finish)");
            }
        };
        this.createPolylineButton = new JToggleButton(this.createPolylineAction);
        this.createPolylineButton.setMargin(new Insets(1, 1, 1, 1));
        this.createPolylineButton.setToolTipText("Polyline   Ctrl-5");
        graphicToolBar.add(this.createPolylineButton);
        this.polylineMenuItem = new JRadioButtonMenuItem(this.createPolylineAction);
        this.polylineMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_5, Event.CTRL_MASK));
        this.polylineMenuItem.setText("Polyline");
        this.polylineMenuItem.setMnemonic('n');
        graphicMenu.add(polylineMenuItem);
        this.createPolygonAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/polygon16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.POLYGON);
                createPolygonButton.setSelected(true);
                polygonMenuItem.setSelected(true);
                status.setText("Click mouse to add polygon points (Shift+Click to finish)");
            }
        };
        this.createPolygonButton = new JToggleButton(this.createPolygonAction);
        this.createPolygonButton.setMargin(new Insets(1, 1, 1, 1));
        this.createPolygonButton.setToolTipText("Polygon   Ctrl-6");
        graphicToolBar.add(this.createPolygonButton);
        this.polygonMenuItem = new JRadioButtonMenuItem(this.createPolygonAction);
        this.polygonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_6, Event.CTRL_MASK));
        this.polygonMenuItem.setText("Polygon");
        this.polygonMenuItem.setMnemonic('g');
        graphicMenu.add(polygonMenuItem);
        this.createQuadCurveAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/quadcurve16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.QUADCURVE);
                createQuadCurveButton.setSelected(true);
                quadcurveMenuItem.setSelected(true);
                status.setText("Click mouse in three places on canvas to create Quadratic curve");
            }
        };
        this.createQuadCurveButton = new JToggleButton(this.createQuadCurveAction);
        this.createQuadCurveButton.setMargin(new Insets(1, 1, 1, 1));
        this.createQuadCurveButton.setToolTipText("Quadratic Curve   Ctrl-7");
        graphicToolBar.add(this.createQuadCurveButton);
        this.quadcurveMenuItem = new JRadioButtonMenuItem(this.createQuadCurveAction);
        this.quadcurveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, Event.CTRL_MASK));
        this.quadcurveMenuItem.setText("Quadratic Curve");
        this.quadcurveMenuItem.setMnemonic('Q');
        graphicMenu.add(quadcurveMenuItem);
        this.createCubicCurveAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/cubiccurve16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.CUBICCURVE);
                createCubicCurveButton.setSelected(true);
                cubicMenuItem.setSelected(true);
                status.setText("Click mouse in four places on canvas to create Cubic curve");
            }
        };
        this.createCubicCurveButton = new JToggleButton(this.createCubicCurveAction);
        this.createCubicCurveButton.setMargin(new Insets(1, 1, 1, 1));
        this.createCubicCurveButton.setToolTipText("Cubic Curve   Ctrl-8");
        graphicToolBar.add(this.createCubicCurveButton);
        this.cubicMenuItem = new JRadioButtonMenuItem(this.createCubicCurveAction);
        this.cubicMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_8, Event.CTRL_MASK));
        this.cubicMenuItem.setText("Cubic Curve");
        this.cubicMenuItem.setMnemonic('C');
        graphicMenu.add(cubicMenuItem);
        this.createFittedCurveAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/fittedcurve16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.FITTEDCURVE);
                createFittedCurveButton.setSelected(true);
                fittedcurveMenuItem.setSelected(true);
                status.setText("Drag mouse to make curve. Smooth curve is created on mouse release");
            }
        };
        this.createFittedCurveButton = new JToggleButton(this.createFittedCurveAction);
        this.createFittedCurveButton.setMargin(new Insets(1, 1, 1, 1));
        this.createFittedCurveButton.setToolTipText("Smooth Curve   Ctrl-9");
        graphicToolBar.add(this.createFittedCurveButton);
        this.fittedcurveMenuItem = new JRadioButtonMenuItem(this.createFittedCurveAction);
        this.fittedcurveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9, Event.CTRL_MASK));
        this.fittedcurveMenuItem.setText("Smooth Curve");
        this.fittedcurveMenuItem.setMnemonic('m');
        graphicMenu.add(fittedcurveMenuItem);
        this.createFreehandCurveAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/freehand16.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.FREEHANDCURVE);
                createFreehandCurveButton.setSelected(true);
                freehandcurveMenuItem.setSelected(true);
                status.setText("What you drag is what you get...");
            }
        };
        this.createFreehandCurveButton = new JToggleButton(this.createFreehandCurveAction);
        this.createFreehandCurveButton.setMargin(new Insets(1, 1, 1, 1));
        this.createFreehandCurveButton.setToolTipText("Freehand Drawing   Ctrl-0");
        graphicToolBar.add(this.createFreehandCurveButton);
        this.freehandcurveMenuItem = new JRadioButtonMenuItem(this.createFreehandCurveAction);
        this.freehandcurveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0, Event.CTRL_MASK));
        this.freehandcurveMenuItem.setText("Freehand Drawing");
        this.freehandcurveMenuItem.setMnemonic('h');
        graphicMenu.add(freehandcurveMenuItem);
        this.createGridAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/grid.png")) {

            public void actionPerformed(ActionEvent evt) {
                GraphicPanel graphicPanel = getSelectedGraphicPanel();
                if (graphicPanel != null) graphicPanel.setState(GraphicPanel.GRID);
                createGridButton.setSelected(true);
                gridMenuItem.setSelected(true);
                status.setText("Drag mouse to create grid");
            }
        };
        this.createGridButton = new JToggleButton(this.createGridAction);
        this.createGridButton.setMargin(new Insets(1, 1, 1, 1));
        this.createGridButton.setToolTipText("Grid");
        graphicToolBar.add(this.createGridButton);
        this.gridMenuItem = new JRadioButtonMenuItem(this.createGridAction);
        this.gridMenuItem.setText("Grid");
        this.gridMenuItem.setMnemonic('G');
        graphicMenu.add(gridMenuItem);
        this.strokeColorComboBox = new ColorComboBox();
        this.strokeColorComboBox.setMaximumSize(new Dimension(250, 100));
        this.strokeColorComboBox.setSelectedColor(Color.black);
        ActionListener al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getSelectedGraphicPanel().setSelectedStrokeColors(strokeColorComboBox.getSelectedColor());
            }
        };
        this.strokeColorComboBox.addActionListener(al);
        graphicToolBar.addSeparator();
        graphicToolBar.add(new JLabel("Stroke: "));
        graphicToolBar.add(this.strokeColorComboBox);
        this.fillColorComboBox = new ColorComboBox();
        this.fillColorComboBox.setMaximumSize(new Dimension(250, 100));
        al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getSelectedGraphicPanel().setSelectedFillColors(fillColorComboBox.getSelectedColor());
            }
        };
        this.fillColorComboBox.addActionListener(al);
        graphicToolBar.add(new JLabel(" Fill: "));
        graphicToolBar.add(this.fillColorComboBox);
        this.textColorComboBox = new ColorComboBox(false);
        this.textColorComboBox.setMaximumSize(new Dimension(250, 100));
        this.textColorComboBox.setSelectedColor(Color.black);
        al = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                getSelectedGraphicPanel().setSelectedTextColors(textColorComboBox.getSelectedColor());
            }
        };
        this.textColorComboBox.addActionListener(al);
        graphicToolBar.add(new JLabel(" Text: "));
        graphicToolBar.add(this.textColorComboBox);
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic('E');
        fileToolBar.addSeparator();
        this.cutAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/cut.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editCut();
                pasteAction.setEnabled(true);
            }
        };
        this.cutButton = new JButton(this.cutAction);
        this.cutButton.setMargin(new Insets(1, 1, 1, 1));
        this.cutButton.setToolTipText("Cut   Ctrl-X");
        this.cutAction.setEnabled(false);
        fileToolBar.add(this.cutButton);
        JMenuItem cutMenuItem = new JMenuItem(this.cutAction);
        cutMenuItem.setText("Cut");
        cutMenuItem.setMnemonic('t');
        cutMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Event.CTRL_MASK));
        editMenu.add(cutMenuItem);
        this.copyAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/copy.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editCopy();
                pasteAction.setEnabled(true);
            }
        };
        this.copyButton = new JButton(this.copyAction);
        this.copyButton.setMargin(new Insets(1, 1, 1, 1));
        this.copyButton.setToolTipText("Copy   Ctrl-C");
        this.copyAction.setEnabled(false);
        fileToolBar.add(this.copyButton);
        JMenuItem copyMenuItem = new JMenuItem(this.copyAction);
        copyMenuItem.setText("Copy");
        copyMenuItem.setMnemonic('C');
        copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
        editMenu.add(copyMenuItem);
        this.pasteAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/paste.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editPaste();
            }
        };
        this.pasteButton = new JButton(this.pasteAction);
        this.pasteButton.setMargin(new Insets(1, 1, 1, 1));
        this.pasteButton.setToolTipText("Paste   Ctrl-V");
        fileToolBar.add(this.pasteButton);
        if (this.clipboardGraphics.size() == 0) this.pasteAction.setEnabled(false);
        JMenuItem pasteMenuItem = new JMenuItem(this.pasteAction);
        pasteMenuItem.setText("Paste");
        pasteMenuItem.setMnemonic('s');
        pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Event.CTRL_MASK));
        editMenu.add(pasteMenuItem);
        this.deleteAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/delete.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editDelete();
            }
        };
        this.deleteButton = new JButton(this.deleteAction);
        this.deleteButton.setMargin(new Insets(1, 1, 1, 1));
        this.deleteButton.setToolTipText("Delete");
        fileToolBar.add(this.deleteButton);
        this.deleteAction.setEnabled(false);
        JMenuItem deleteMenuItem = new JMenuItem(this.deleteAction);
        deleteMenuItem.setText("Delete");
        deleteMenuItem.setMnemonic('D');
        deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        editMenu.add(deleteMenuItem);
        this.clearAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/null.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editClear();
            }
        };
        this.clearAction.setEnabled(false);
        JMenuItem clearMenuItem = new JMenuItem(this.clearAction);
        clearMenuItem.setText("Clear");
        clearMenuItem.setMnemonic('r');
        editMenu.add(clearMenuItem);
        fileToolBar.addSeparator();
        editMenu.addSeparator();
        this.groupAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/group.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editGroup();
            }
        };
        this.groupButton = new JButton(this.groupAction);
        this.groupButton.setMargin(new Insets(1, 1, 1, 1));
        this.groupButton.setToolTipText("Group   Ctrl-G");
        fileToolBar.add(this.groupButton);
        this.groupAction.setEnabled(false);
        JMenuItem groupMenuItem = new JMenuItem(this.groupAction);
        groupMenuItem.setText("Group");
        groupMenuItem.setMnemonic('G');
        groupMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK));
        editMenu.add(groupMenuItem);
        this.ungroupAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/ungroup.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editUngroup();
            }
        };
        this.ungroupButton = new JButton(this.ungroupAction);
        this.ungroupButton.setMargin(new Insets(1, 1, 1, 1));
        this.ungroupButton.setToolTipText("Ungroup   Ctrl-U");
        fileToolBar.add(this.ungroupButton);
        this.ungroupAction.setEnabled(false);
        JMenuItem ungroupMenuItem = new JMenuItem(this.ungroupAction);
        ungroupMenuItem.setText("Ungroup");
        ungroupMenuItem.setMnemonic('U');
        ungroupMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, KeyEvent.CTRL_MASK));
        editMenu.add(ungroupMenuItem);
        editMenu.addSeparator();
        this.selectAllAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/selectall.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().selectAll();
            }
        };
        selectAllAction.setEnabled(false);
        JMenuItem selectAllMenuItem = new JMenuItem(this.selectAllAction);
        selectAllMenuItem.setText("Select All");
        selectAllMenuItem.setMnemonic('A');
        selectAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_MASK));
        editMenu.add(selectAllMenuItem);
        editMenu.addSeparator();
        fileToolBar.addSeparator();
        this.sendToBackAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/toback.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editSendToBack();
            }
        };
        this.sendToBackButton = new JButton(this.sendToBackAction);
        this.sendToBackButton.setMargin(new Insets(1, 1, 1, 1));
        this.sendToBackButton.setToolTipText("Send To Back   Ctrl-B");
        fileToolBar.add(this.sendToBackButton);
        JMenuItem sendToBackMenuItem = new JMenuItem(this.sendToBackAction);
        sendToBackMenuItem.setText("Send To Back");
        sendToBackMenuItem.setMnemonic('B');
        sendToBackMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK));
        editMenu.add(sendToBackMenuItem);
        this.bringToFrontAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/tofront.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editBringToFront();
            }
        };
        this.bringToFrontButton = new JButton(this.bringToFrontAction);
        this.bringToFrontButton.setMargin(new Insets(1, 1, 1, 1));
        this.bringToFrontButton.setToolTipText("Bring To Front   Ctrl-F");
        fileToolBar.add(this.bringToFrontButton);
        JMenuItem bringToFrontMenuItem = new JMenuItem(this.bringToFrontAction);
        bringToFrontMenuItem.setText("Bring To Front");
        bringToFrontMenuItem.setMnemonic('F');
        bringToFrontMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK));
        editMenu.add(bringToFrontMenuItem);
        this.sendBackwardAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/backward.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editSendBackward();
            }
        };
        this.sendBackwardButton = new JButton(this.sendBackwardAction);
        this.sendBackwardButton.setMargin(new Insets(1, 1, 1, 1));
        this.sendBackwardButton.setToolTipText("Send Backward   Ctrl-Shift-B");
        fileToolBar.add(this.sendBackwardButton);
        JMenuItem sendBackwardMenuItem = new JMenuItem(this.sendBackwardAction);
        sendBackwardMenuItem.setText("Send Backward");
        sendBackwardMenuItem.setMnemonic('A');
        sendBackwardMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift B"));
        editMenu.add(sendBackwardMenuItem);
        this.bringForwardAction = new AbstractAction(null, ResourceLoader.loadImageIcon("org/convey/images/forward.png")) {

            public void actionPerformed(ActionEvent evt) {
                getSelectedGraphicPanel().editBringForward();
            }
        };
        this.bringForwardButton = new JButton(this.bringForwardAction);
        this.bringForwardButton.setMargin(new Insets(1, 1, 1, 1));
        this.bringForwardButton.setToolTipText("Bring Forward   Ctrl-Shift-F");
        fileToolBar.add(this.bringForwardButton);
        JMenuItem bringForwardMenuItem = new JMenuItem(this.bringForwardAction);
        bringForwardMenuItem.setText("Bring Forward");
        bringForwardMenuItem.setMnemonic('O');
        bringForwardMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl shift F"));
        editMenu.add(bringForwardMenuItem);
        menuBar.add(editMenu);
        menuBar.add(graphicMenu);
        super.setJMenuBar(menuBar);
        ButtonGroup group = new ButtonGroup();
        group.add(this.selectButton);
        group.add(this.scaleButton);
        group.add(this.createTextButton);
        group.add(this.createRectangleButton);
        group.add(this.createEllipseButton);
        group.add(this.createPolylineButton);
        group.add(this.createPolygonButton);
        group.add(this.createQuadCurveButton);
        group.add(this.createCubicCurveButton);
        group.add(this.createFittedCurveButton);
        group.add(this.createFreehandCurveButton);
        group.add(this.createGridButton);
        ButtonGroup graphicsMenuGroup = new ButtonGroup();
        graphicsMenuGroup.add((AbstractButton) selectMenuItem);
        graphicsMenuGroup.add((AbstractButton) scaleMenuItem);
        graphicsMenuGroup.add((AbstractButton) rectangleMenuItem);
        graphicsMenuGroup.add((AbstractButton) ellipseMenuItem);
        graphicsMenuGroup.add((AbstractButton) polylineMenuItem);
        graphicsMenuGroup.add((AbstractButton) polygonMenuItem);
        graphicsMenuGroup.add((AbstractButton) quadcurveMenuItem);
        graphicsMenuGroup.add((AbstractButton) cubicMenuItem);
        graphicsMenuGroup.add((AbstractButton) fittedcurveMenuItem);
        graphicsMenuGroup.add((AbstractButton) freehandcurveMenuItem);
        graphicsMenuGroup.add((AbstractButton) gridMenuItem);
        this.toolBarBox = new Box(BoxLayout.Y_AXIS);
        Box conveyToolBarBox = new Box(BoxLayout.X_AXIS);
        conveyToolBarBox.add(conveyToolBar);
        conveyToolBarBox.add(Box.createHorizontalGlue());
        this.toolBarBox.add(conveyToolBarBox);
        Box fileToolBarBox = new Box(BoxLayout.X_AXIS);
        fileToolBarBox.add(fileToolBar);
        fileToolBarBox.add(Box.createHorizontalGlue());
        this.toolBarBox.add(fileToolBarBox);
        Box graphicToolBarBox = new Box(BoxLayout.X_AXIS);
        graphicToolBarBox.add(graphicToolBar);
        graphicToolBarBox.add(Box.createHorizontalGlue());
        this.toolBarBox.add(graphicToolBarBox);
        super.getContentPane().add(this.toolBarBox, BorderLayout.NORTH);
        this.tabbedPane = new JTabbedPaneWithCloseIcons();
        this.tabbedPane.addChangeListener(this);
        this.status = new JTextField();
        this.status.setBackground(Color.lightGray);
        this.status.setForeground(Color.blue);
        super.getContentPane().add(this.tabbedPane, BorderLayout.CENTER);
        super.getContentPane().add(this.status, BorderLayout.SOUTH);
        this.selectAction.actionPerformed(new ActionEvent(this, 0, ""));
        this.newLocal();
        this.updateButtons();
        super.show();
    }

    public GraphicPanel getSelectedGraphicPanel() {
        CanvasPanel canvasPanel = this.getSelectedCanvasPanel();
        if (canvasPanel != null) return canvasPanel.getGraphicPanel();
        return null;
    }

    public CanvasPanel getSelectedCanvasPanel() {
        Component component = tabbedPane.getSelectedComponent();
        if (component == null) return null;
        return ((CanvasPanel) component);
    }

    class FlashTimerTask extends TimerTask {

        private boolean isColored = false;

        private Color color = null;

        private JabberCanvasPanel jabberCanvasPanel;

        private int count = 0;

        FlashTimerTask(JabberCanvasPanel jabberCanvasPanel, Color color) {
            this.jabberCanvasPanel = jabberCanvasPanel;
            this.color = color;
        }

        public void run() {
            int index = tabbedPane.indexOfComponent(jabberCanvasPanel);
            if (isColored) {
                tabbedPane.setBackgroundAt(index, null);
                isColored = false;
            } else {
                tabbedPane.setBackgroundAt(index, this.color);
                isColored = true;
                count++;
            }
            if (count > 5 || tabbedPane.getSelectedComponent() == jabberCanvasPanel) {
                if (tabbedPane.getSelectedComponent() == jabberCanvasPanel) tabbedPane.setBackgroundAt(index, null);
                cancel();
            }
        }
    }

    ;

    public void handleMessage(JabberConveyMessage msg) {
        String threadId = msg.getThreadID();
        String from = msg.getFrom();
        StringTokenizer fromString = new StringTokenizer(from, "@");
        String user = fromString.nextToken();
        String subj = msg.getSubject();
        if (subj == null) subj = "{no subject}";
        JabberCanvasPanel jabberCanvasPanel;
        if (threadId != null) {
            String key = from + ":" + threadId;
            Object obj = jabberCanvasPanels.get(key);
            if (obj != null) jabberCanvasPanel = (JabberCanvasPanel) obj; else {
                jabberCanvasPanel = new JabberCanvasPanel(this, msg.getFrom(), threadId, subj);
                jabberCanvasPanel.getGraphicPanel().setHasPen(false);
                jabberCanvasPanel.setInitiator(false);
                jabberCanvasPanels.put(key, jabberCanvasPanel);
                StringTokenizer strtok = new StringTokenizer(msg.getFrom(), "@");
                this.tabbedPane.addTab(strtok.nextToken(), jabberCanvasPanel, ResourceLoader.loadImageIcon("org/convey/images/no_pen.png"));
                this.tabbedPane.setToolTipTextAt(this.tabbedPane.getComponentCount() - 1, msg.getFrom() + "  \"" + subj + "\"");
            }
        } else {
            jabberCanvasPanel = new JabberCanvasPanel(this, msg.getFrom(), "", subj);
            StringTokenizer strtok = new StringTokenizer(msg.getFrom(), "@");
            this.tabbedPane.addTab(strtok.nextToken(), jabberCanvasPanel, ResourceLoader.loadImageIcon("org/convey/images/no_pen.png"));
            this.tabbedPane.setToolTipTextAt(this.tabbedPane.getComponentCount() - 1, msg.getFrom() + "  \"" + subj + "\"");
        }
        jabberCanvasPanel.handleMessage(msg);
        boolean foundPane = false;
        for (int i = 0; i < this.tabbedPane.getComponentCount(); i++) {
            if (this.tabbedPane.getComponentAt(i) == jabberCanvasPanel) {
                foundPane = true;
                break;
            }
        }
        if (!foundPane) {
            this.tabbedPane.addTab(user, jabberCanvasPanel, ResourceLoader.loadImageIcon("org/convey/images/no_pen.png"));
            this.tabbedPane.setToolTipTextAt(this.tabbedPane.getComponentCount() - 1, jabberCanvasPanel.getTo() + "  \"" + jabberCanvasPanel.getSubject() + "\"");
        }
        if (this.tabbedPane.getSelectedComponent() != jabberCanvasPanel) {
            FlashTimerTask task = null;
            if (msg.isPen()) task = new FlashTimerTask(jabberCanvasPanel, Color.yellow); else task = new FlashTimerTask(jabberCanvasPanel, Color.green);
            this.timer.scheduleAtFixedRate(task, 0, 500);
        }
        this.updateButtons();
    }

    public void newChat(String server, String user, String subject) {
        String to = user + "@" + server;
        JabberCanvasPanel jabberCanvasPanel = new JabberCanvasPanel(this, to, subject);
        jabberCanvasPanel.getGraphicPanel().setHasPen(true);
        jabberCanvasPanel.setInitiator(true);
        String threadId = jabberCanvasPanel.getThreadId();
        this.jabberCanvasPanels.put(to + ":" + threadId, jabberCanvasPanel);
        this.tabbedPane.addTab(user, jabberCanvasPanel, ResourceLoader.loadImageIcon("org/convey/images/pen.png"));
        this.tabbedPane.setSelectedIndex(this.tabbedPane.getComponentCount() - 1);
        this.tabbedPane.setToolTipTextAt(this.tabbedPane.getComponentCount() - 1, to + "  \"" + subject + "\"");
        this.updateButtons();
    }

    public void newLocal() {
        CanvasPanel canvasPanel = new CanvasPanel(this);
        this.tabbedPane.addTab("Untitled", canvasPanel);
        this.tabbedPane.setSelectedIndex(this.tabbedPane.getComponentCount() - 1);
        this.tabbedPane.setToolTipTextAt(this.tabbedPane.getComponentCount() - 1, "A canvas for your ideas - it's not networked");
        this.updateButtons();
    }

    public Vector getClipboardGraphics() {
        return this.clipboardGraphics;
    }

    public void updateButtons() {
        boolean hasPen = true;
        if (this.session != null && this.session.getConnection().isConnected()) {
            CanvasPanel canvasPanel = this.getSelectedCanvasPanel();
            if (canvasPanel instanceof JabberCanvasPanel) {
                JabberCanvasPanel jabberCanvasPanel = (JabberCanvasPanel) canvasPanel;
                if (jabberCanvasPanel.getGraphicPanel().hasPen()) {
                    this.yieldPenAction.setEnabled(true);
                    this.requestPenAction.setEnabled(false);
                    this.revokePenAction.setEnabled(false);
                } else {
                    hasPen = false;
                    this.yieldPenAction.setEnabled(false);
                    if (jabberCanvasPanel.isRequest()) this.requestPenAction.setEnabled(false); else this.requestPenAction.setEnabled(true);
                    if (jabberCanvasPanel.isInitiator() && !jabberCanvasPanel.isRevoke()) this.revokePenAction.setEnabled(true); else this.revokePenAction.setEnabled(false);
                }
            } else {
                this.yieldPenAction.setEnabled(false);
                this.requestPenAction.setEnabled(false);
                this.revokePenAction.setEnabled(false);
            }
        }
        if (this.getSelectedGraphicPanel().graphicCount() > 0) {
            if (hasPen) this.clearAction.setEnabled(true); else this.clearAction.setEnabled(false);
            this.selectAllAction.setEnabled(true);
        } else {
            this.clearAction.setEnabled(false);
            this.selectAllAction.setEnabled(false);
        }
        switch(this.getSelectedGraphicPanel().selectedGraphicCount()) {
            case 0:
                if (hasPen) {
                    this.groupAction.setEnabled(false);
                    this.ungroupAction.setEnabled(false);
                    this.scaleAction.setEnabled(true);
                    this.copyAction.setEnabled(false);
                    this.cutAction.setEnabled(false);
                    this.copyAction.setEnabled(false);
                    this.deleteAction.setEnabled(false);
                    this.sendBackwardAction.setEnabled(false);
                    this.bringForwardAction.setEnabled(false);
                    this.sendToBackAction.setEnabled(false);
                    this.bringToFrontAction.setEnabled(false);
                } else {
                    this.groupAction.setEnabled(false);
                    this.ungroupAction.setEnabled(false);
                    this.scaleAction.setEnabled(false);
                    this.copyAction.setEnabled(true);
                    this.cutAction.setEnabled(false);
                    this.copyAction.setEnabled(false);
                    this.deleteAction.setEnabled(false);
                    this.sendBackwardAction.setEnabled(false);
                    this.bringForwardAction.setEnabled(false);
                    this.sendToBackAction.setEnabled(false);
                    this.bringToFrontAction.setEnabled(false);
                }
                break;
            case 1:
                if (hasPen) {
                    this.groupAction.setEnabled(false);
                    Graphic selected = this.getSelectedGraphicPanel().selectedGraphic(0);
                    this.copyAction.setEnabled(true);
                    this.cutAction.setEnabled(true);
                    this.deleteAction.setEnabled(true);
                    if (selected instanceof GroupGraphic) this.ungroupAction.setEnabled(true); else this.ungroupAction.setEnabled(false);
                    this.scaleAction.setEnabled(true);
                    this.sendBackwardAction.setEnabled(true);
                    this.bringForwardAction.setEnabled(true);
                    this.sendToBackAction.setEnabled(true);
                    this.bringToFrontAction.setEnabled(true);
                } else {
                    this.groupAction.setEnabled(false);
                    this.copyAction.setEnabled(true);
                    this.cutAction.setEnabled(false);
                    this.deleteAction.setEnabled(false);
                    this.ungroupAction.setEnabled(false);
                    this.scaleAction.setEnabled(false);
                    this.sendBackwardAction.setEnabled(false);
                    this.bringForwardAction.setEnabled(false);
                    this.sendToBackAction.setEnabled(false);
                    this.bringToFrontAction.setEnabled(false);
                }
                break;
            default:
                if (hasPen) {
                    this.groupAction.setEnabled(true);
                    this.ungroupAction.setEnabled(false);
                    this.scaleAction.setEnabled(false);
                    this.copyAction.setEnabled(true);
                    this.cutAction.setEnabled(true);
                    this.deleteAction.setEnabled(true);
                    this.sendBackwardAction.setEnabled(true);
                    this.bringForwardAction.setEnabled(true);
                    this.sendToBackAction.setEnabled(true);
                    this.bringToFrontAction.setEnabled(true);
                } else {
                    this.groupAction.setEnabled(false);
                    this.ungroupAction.setEnabled(false);
                    this.scaleAction.setEnabled(false);
                    this.copyAction.setEnabled(true);
                    this.cutAction.setEnabled(false);
                    this.deleteAction.setEnabled(false);
                    this.sendBackwardAction.setEnabled(false);
                    this.bringForwardAction.setEnabled(false);
                    this.sendToBackAction.setEnabled(false);
                    this.bringToFrontAction.setEnabled(false);
                }
                break;
        }
        if (!hasPen) {
            this.selectButton.setSelected(true);
            this.selectMenuItem.setSelected(true);
            this.pasteAction.setEnabled(false);
            this.scaleAction.setEnabled(false);
            this.createRectangleAction.setEnabled(false);
            this.createEllipseAction.setEnabled(false);
            this.createTextAction.setEnabled(false);
            this.createPolygonAction.setEnabled(false);
            this.createPolylineAction.setEnabled(false);
            this.createQuadCurveAction.setEnabled(false);
            this.createCubicCurveAction.setEnabled(false);
            this.createFittedCurveAction.setEnabled(false);
            this.createFreehandCurveAction.setEnabled(false);
            this.createGridAction.setEnabled(false);
        } else {
            if (this.clipboardGraphics.size() > 0) this.pasteAction.setEnabled(true);
            this.scaleAction.setEnabled(true);
            this.createRectangleAction.setEnabled(true);
            this.createEllipseAction.setEnabled(true);
            this.createTextAction.setEnabled(true);
            this.createPolygonAction.setEnabled(true);
            this.createPolylineAction.setEnabled(true);
            this.createQuadCurveAction.setEnabled(true);
            this.createCubicCurveAction.setEnabled(true);
            this.createFittedCurveAction.setEnabled(true);
            this.createFreehandCurveAction.setEnabled(true);
            this.createGridAction.setEnabled(true);
        }
    }

    /**
	 * @todo provide filter for accepting only supported svg tags in convey
	 */
    public void fileOpen() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(new ExtensionFilter());
        }
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file == null || file.getName().equals("")) {
                JOptionPane.showMessageDialog(this, "Invalid File Name", "Invalid File Name", JOptionPane.ERROR_MESSAGE);
            } else {
                CanvasPanel canvasPanel = new CanvasPanel(this);
                this.tabbedPane.addTab(file.getName(), canvasPanel);
                canvasPanel.openSVG(file);
            }
        }
        this.updateButtons();
    }

    public void fileSaveAs() {
    }

    public void fileSave() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(new ExtensionFilter());
        }
        int response = fileChooser.showSaveDialog(this);
        if (response == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            this.getSelectedCanvasPanel().saveSVG(file);
            if (!this.getSelectedGraphicPanel().canvasPanel.isNetworked) {
                this.tabbedPane.setTitleAt(this.tabbedPane.getSelectedIndex(), file.getName());
            }
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (this.selectButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.SELECT); else if (this.scaleButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.SCALE); else if (this.createRectangleButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.RECTANGLE); else if (this.createEllipseButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.ELLIPSE); else if (this.createPolylineButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.POLYLINE); else if (this.createPolygonButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.POLYGON); else if (this.createQuadCurveButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.QUADCURVE); else if (this.createCubicCurveButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.CUBICCURVE); else if (this.createFittedCurveButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.FITTEDCURVE); else if (this.createFreehandCurveButton.isSelected()) this.getSelectedGraphicPanel().setState(GraphicPanel.FREEHANDCURVE);
        this.setTitle();
        CanvasPanel canvasPanel = this.getSelectedCanvasPanel();
        if (canvasPanel != null && canvasPanel instanceof JabberCanvasPanel) {
            JabberCanvasPanel jabberCanvasPanel = (JabberCanvasPanel) canvasPanel;
            if (jabberCanvasPanel.isRequest()) jabberCanvasPanel.doRequest();
        }
        this.updateButtons();
    }

    public void setTitle() {
        if (this.tabbedPane.getComponentCount() == 0) {
            if (this.session != null) super.setTitle("Convey " + "[" + this.from + "]"); else super.setTitle("Convey " + "[disconnected]");
        } else {
            Component component = this.tabbedPane.getSelectedComponent();
            if (component instanceof JabberCanvasPanel) {
                JabberCanvasPanel panel = (JabberCanvasPanel) component;
                String title;
                if (this.session != null) title = "Convey [" + this.from + " - " + panel.getTo() + "] " + panel.getSubject(); else title = "Convey [disconnected - " + panel.getTo() + "] " + panel.getSubject();
                super.setTitle(title);
                tabbedPane.setBackgroundAt(this.tabbedPane.indexOfComponent(panel), null);
            } else {
                String title;
                if (this.session != null) title = "Convey [" + this.from + "] " + ((CanvasPanel) component).getSavedFilename(); else title = "Convey [disconnected] " + ((CanvasPanel) component).getSavedFilename();
                super.setTitle(title);
            }
        }
    }

    /**
	 * Display connect dialog and connects to a Jabber server if the user does
	 * not cancel the operation.
	 */
    public void connect() {
        if (this.connectDialog == null) {
            File homeFile;
            File configDirFile;
            File connectFile;
            String home = System.getProperty("user.home");
            if (home == null) home = ".";
            homeFile = new File(home);
            configDirFile = new File(homeFile, ".convey");
            if (configDirFile.exists()) {
                connectFile = new File(configDirFile, ".connect");
                if (connectFile.exists()) {
                    try {
                        FileInputStream fis = new FileInputStream(connectFile.getPath());
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        this.connectDialog = new ConnectDialog();
                        this.connectDialog.readExternal(ois);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (this.connectDialog == null) this.connectDialog = new ConnectDialog(this);
        this.connectDialog.show();
        while (this.connectDialog.getResult() == JOptionPane.OK_OPTION) {
            try {
                this.connect(this.connectDialog.getServer(), this.connectDialog.getUser(), this.connectDialog.getPassword(), this.connectDialog.isNewUser());
                return;
            } catch (JabberMessageException e) {
                JOptionPane.showMessageDialog(this, "User name or password is invalid.", "Unauthorized", JOptionPane.ERROR_MESSAGE);
                this.session = null;
            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(this, "Unknown Host.", "Unknown Host", JOptionPane.ERROR_MESSAGE);
                this.session = null;
            } catch (Exception e) {
                e.printStackTrace();
                this.session = null;
            }
            this.connectDialog.show();
        }
    }

    /**
	 * Connect to a jabber server as the specified user.
	 *
	 * @todo Modify title bar somehow to indicate connect status and Jabber ID.
	 * @todo Chat history should auto scroll to bottom when a message is added,
	 *       except if the user manually scrolled away from bottom. Then, when the
	 *       user scrolls back to bottom, reset flag.
	 */
    public void connect(String server, String user, String password, boolean newUser) throws JabberMessageException, UnknownHostException, ConnectionFailedException, SendMessageFailedException, ParseException {
        if (this.session == null) {
            if (server == null || user == null || password == null) throw new NullPointerException();
            if (newUser) {
                Jabber jabber = new Jabber();
                JabberContext context = new JabberContext(null, null, server);
                this.session = jabber.createSession(context);
                this.session.connect(server, 5222);
                JabberUserService userService = this.session.getUserService();
                HashMap fieldMap = userService.getRegisterFields("jabber.org");
                Iterator iter = fieldMap.keySet().iterator();
                fieldMap.put("username", user);
                fieldMap.put("password", password);
                try {
                    userService.register("jabber.org", fieldMap);
                } catch (JabberMessageException e) {
                    JOptionPane.showMessageDialog(this, e.getErrorMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
                    this.session.disconnect();
                    this.session = null;
                    return;
                }
                this.session.disconnect();
            }
            Jabber jabber = new Jabber();
            DefaultMessageParser parser = new DefaultMessageParser();
            parser.setParser("message", JabberCode.XMLNS_CHAT, "org.convey.JabberConveyMessage");
            JabberContext context = new JabberContext(user, password, server);
            this.session = jabber.createSession(context, parser);
            this.session.connect(server, 5222);
            this.session.getUserService().login();
            JabberPresenceMessage msg = new JabberPresenceMessage(PresenceCode.TYPE_AVAILABLE);
            msg.setPriority("1");
            msg.setShowState("Online");
            msg.setStatus("Online");
            String str = msg.encode();
            this.session.getPresenceService().setToAvailable("Online", null);
            final ConveyFrame thisConveyFrame = this;
            JabberMessageListener listener = new JabberMessageListener() {

                public void messageReceived(JabberMessageEvent evt) {
                    if (evt.getMessageType() == JabberCode.MSG_CHAT) {
                        JabberConveyMessage msg = (JabberConveyMessage) evt.getMessage();
                        thisConveyFrame.handleMessage(msg);
                    }
                }
            };
            this.session.addMessageListener(listener);
            this.from = user + "@" + server;
            this.setTitle();
            this.disconnectAction.setEnabled(true);
            this.chatAction.setEnabled(true);
            this.connectAction.setEnabled(false);
        }
    }

    /**
	 * Disconnects from the Jabber server, optionally displaying a prompt for the
	 * user to cancel the disconnect action,
	 *
	 * @return true if the user chose to disconnect; false, otherwise.
	 *
	 * @todo Modify title bar somehow to indicate disconnect status.
	 */
    public boolean disconnect(boolean prompt) {
        if (this.session != null) {
            if (prompt == true) {
                int result = JOptionPane.showConfirmDialog(this, "Are you sure you want to disconnect?", "Disconnect", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.NO_OPTION) return false;
            }
            try {
                this.session.getPresenceService().setToUnavailable();
            } catch (SendMessageFailedException e) {
                e.printStackTrace();
            }
            this.session.removeMessageListener(this.listener);
            this.session.disconnect();
            this.session = null;
            this.setTitle();
            this.chatAction.setEnabled(false);
            this.disconnectAction.setEnabled(false);
            this.connectAction.setEnabled(true);
        }
        return true;
    }

    /**
	 * Displays chat receiver dialog box and sets up chat receiver status if the
	 * user does not cancel operation.
	 */
    public void newChat() {
        if (this.chatDialog == null) {
            File homeFile;
            File configDirFile;
            File chatFile;
            String home = System.getProperty("user.home");
            if (home == null) home = ".";
            homeFile = new File(home);
            configDirFile = new File(homeFile, ".convey");
            if (configDirFile.exists()) {
                chatFile = new File(configDirFile, ".chat");
                if (chatFile.exists()) {
                    try {
                        FileInputStream fis = new FileInputStream(chatFile.getPath());
                        ObjectInputStream ois = new ObjectInputStream(fis);
                        this.chatDialog = new ChatDialog();
                        this.chatDialog.readExternal(ois);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (this.chatDialog == null) this.chatDialog = new ChatDialog(this);
        this.chatDialog.show();
        if (this.chatDialog.getResult() == JOptionPane.OK_OPTION) this.newChat(this.chatDialog.getServer(), this.chatDialog.getUser(), this.chatDialog.getSubject());
    }

    public JabberSession getJabberSession() {
        return this.session;
    }

    public String getFrom() {
        return this.from;
    }

    public static ConveyFrame getConveyFrame() {
        return ConveyFrame.conveyFrame;
    }

    public void saveConfig() {
        if (connectDialog != null) {
            try {
                File homeFile;
                File configDirFile;
                File connectFile;
                String home = System.getProperty("user.home");
                if (home == null) home = ".";
                homeFile = new File(home);
                configDirFile = new File(homeFile, ".convey");
                if (!configDirFile.exists()) configDirFile.mkdir();
                connectFile = new File(configDirFile, ".connect");
                if (!connectFile.exists()) connectFile.createNewFile();
                if (connectFile.canWrite()) {
                    FileOutputStream fos = new FileOutputStream(connectFile);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    this.connectDialog.writeExternal(oos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (chatDialog != null) {
            try {
                File homeFile;
                File configDirFile;
                File chatFile;
                String home = System.getProperty("user.home");
                if (home == null) home = ".";
                homeFile = new File(home);
                configDirFile = new File(homeFile, ".convey");
                if (!configDirFile.exists()) configDirFile.mkdir();
                chatFile = new File(configDirFile, ".chat");
                if (!chatFile.exists()) chatFile.createNewFile();
                if (chatFile.canWrite()) {
                    FileOutputStream fos = new FileOutputStream(chatFile);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    this.chatDialog.writeExternal(oos);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Color getFillColor() {
        return this.fillColorComboBox.getSelectedColor();
    }

    public Color getStrokeColor() {
        return this.strokeColorComboBox.getSelectedColor();
    }

    public Color getTextColor() {
        return this.textColorComboBox.getSelectedColor();
    }

    public boolean isAutoSend() {
        return this.autoSendButton.isSelected();
    }

    public void changePenIcon(JabberCanvasPanel jabberCanvasPanel, boolean value) {
        int index = this.tabbedPane.indexOfComponent(jabberCanvasPanel);
        if (index != -1) {
            if (value) this.tabbedPane.setIconAt(index, new CloseTabIcon(ResourceLoader.loadImageIcon("org/convey/images/pen.png"))); else this.tabbedPane.setIconAt(index, new CloseTabIcon(ResourceLoader.loadImageIcon("org/convey/images/no_pen.png")));
        }
    }

    /**
	 * Main method invoked when Convey client application starts. Sets up system
	 * look and feel.
	 */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        ConveyFrame.conveyFrame = new ConveyFrame();
    }
}
