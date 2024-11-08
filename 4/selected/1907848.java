package org.sf.jspread.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import org.sf.jspread.utils.*;
import org.sf.jspread.objects.*;
import org.sf.jspread.gui.tables.*;
import org.sf.jspread.marketdata.*;
import org.sf.jspread.gui.plotter.*;
import org.sf.jspread.gui.adapter.*;
import org.sf.jspread.ordermanager.rv.*;
import org.sf.jspread.gui.tables.models.*;

public class SpreadGUI extends JFrame {

    private JPanel contentPane;

    private ImageIcon spreadIcon;

    private SpreadLogger log = SpreadManager.getLog();

    private JTabbedPane ordersExecsAndInfo = new JTabbedPane();

    private BorderLayout stBorderLayout = new BorderLayout();

    private SpreadTableModel sTableModel = new SpreadTableModel();

    private SpreadTable stTable = new SpreadTable(sTableModel);

    private OrderTableModel oTableModel;

    private ExecTableModel eTableModel = new ExecTableModel();

    private ExecTable stExecsTable = new ExecTable(eTableModel);

    private BaikaiNProfilerTablePanel stBaikaiPanel = new BaikaiNProfilerTablePanel();

    private BaikaiTableModel bkTableModel = stBaikaiPanel.getModel();

    private JSplitPane splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private JSplitPane graphBaikaiPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

    private ColumnChooser cellChooser = new ColumnChooser(stTable);

    private SpreadPlotter graphPanel = new SpreadPlotter();

    private OrderTablePanel orderPanel = new OrderTablePanel();

    private StringTokenizer toknizer;

    public SpreadGUI() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        try {
            jbInit();
        } catch (Exception e) {
            log.error("Exeception Creating SpreadGUI");
            log.error(e.getMessage());
            e.printStackTrace(log.getPrinter());
        }
    }

    private void jbInit() throws Exception {
        log.debug("SpreadGUI Component initialization begin ..");
        spreadIcon = new ImageIcon(SpreadProperties.getProperty("ICONSDIR") + "stIcon.gif");
        contentPane = (JPanel) this.getContentPane();
        contentPane.setLayout(stBorderLayout);
        this.setTitle("JSpread" + " [User: " + SpreadManager.getUserID() + "]");
        toknizer = SpreadProperties.getColumnsToHide();
        while (toknizer.hasMoreTokens()) {
            stTable.hideColumn(Integer.parseInt(toknizer.nextToken()));
        }
        oTableModel = orderPanel.getOrderTableModel();
        graphPanel.setBackground(Color.white);
        graphBaikaiPanel.setVerifyInputWhenFocusTarget(true);
        graphBaikaiPanel.setTopComponent(stBaikaiPanel);
        graphBaikaiPanel.setDividerLocation(0.50);
        graphBaikaiPanel.setBottomComponent(graphPanel);
        ordersExecsAndInfo.addTab("Spread Details", graphBaikaiPanel);
        ordersExecsAndInfo.addTab("Orders        ", orderPanel);
        ordersExecsAndInfo.addTab("Executions    ", stExecsTable.getStExecTableScrollPane());
        splitPaneV.setVerifyInputWhenFocusTarget(true);
        splitPaneV.setBottomComponent(ordersExecsAndInfo);
        splitPaneV.setDividerLocation(0.50);
        splitPaneV.setTopComponent(stTable.getStTableScrollPane());
        setJMenuBar(new SpreadMenu(this));
        addComponentListener(new SpreadGUIWindowAdapter(this));
        contentPane.add(new SpreadToolBar(this), BorderLayout.NORTH);
        contentPane.add(splitPaneV, BorderLayout.CENTER);
        log.debug("SpreadGUI Component initialization complete");
    }

    public void setGraphPanel(SpreadPlotter graphPanel) {
        if (this.graphPanel != graphPanel) {
            this.graphPanel = graphPanel;
            graphBaikaiPanel.setBottomComponent(graphPanel);
            this.graphPanel.fitGraphToFrame();
        }
    }

    public void refreshGraphPanel(SpreadPlotter graphPanel) {
        graphBaikaiPanel.setBottomComponent(graphPanel);
    }

    public void openStDatFileActionPerformed(ActionEvent e) {
        JFileChooser stDatFileChooser = new JFileChooser(SpreadProperties.getProperty("DATDIR"));
        stDatFileChooser.setDialogTitle("Select Spread File");
        int result = stDatFileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            log.info("Open ST Dat File : " + stDatFileChooser.getSelectedFile());
            SpreadReader reader = new SpreadReader(stDatFileChooser.getSelectedFile());
            Spread _spread = reader.ReadObject();
            log.debug("Got:" + _spread);
            if (_spread != null) {
                _spread.initSpread();
                SpreadManager.addSpread(_spread);
                _spread.attachToGUI(sTableModel, oTableModel, eTableModel, bkTableModel);
                log.info("New Spread : " + _spread.getSpreadID() + " [Size: " + SpreadManager.getSize() + "]");
            }
        }
    }

    public void newSpreadActionPerformed(ActionEvent e) {
        NewSpread newSpread = new NewSpread();
        newSpread.setModal(true);
        Spread Spread = newSpread.getSpread();
        if (Spread != null) {
            Spread.initSpread();
            SpreadManager.addSpread(Spread);
            Spread.attachToGUI(sTableModel, oTableModel, eTableModel, bkTableModel);
            log.info("New Spread : " + Spread.getSpreadID() + " [Size: " + SpreadManager.getSize() + "]");
        }
    }

    public void saveSpreadActionPerformed(ActionEvent e) {
        if (SpreadManager.getSelectedSpread() == null) {
        } else {
            JFileChooser stDatFileChooser = new JFileChooser(SpreadProperties.getProperty("DATDIR"));
            stDatFileChooser.setDialogTitle("Save Spread As");
            int result = stDatFileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                log.info("Save Spread [" + SpreadManager.getSelectedSpread().getSpreadID() + "] To File : " + stDatFileChooser.getSelectedFile());
                SpreadWriter _write = new SpreadWriter(stDatFileChooser.getSelectedFile());
                _write.WriteSpread(SpreadManager.getSelectedSpread());
            }
        }
    }

    public void startSpreadActionPerformed(ActionEvent e) {
        if (SpreadManager.getSelectedSpread() == null) {
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_READY)) {
            log.info("Starting New Spread, SpreadID: " + SpreadManager.getSelectedSpread().getSpreadID());
            SpreadManager.getSelectedSpread().startSpread();
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_PAUSED)) {
            log.info("Resuming Paused Spread, SpreadID: " + SpreadManager.getSelectedSpread().getSpreadID());
            SpreadManager.getSelectedSpread().setStatus(SpreadManager.SPREAD_STATUS_RESUMING);
            SpreadManager.getSelectedSpread().checkSpread();
            SpreadManager.getSelectedSpread().getSTableModel().updateSpread(SpreadManager.getSelectedSpread());
        }
    }

    public void pauseSpreadActionPerformed(ActionEvent e) {
        if (SpreadManager.getSelectedSpread() == null) {
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_PAUSED)) {
            log.info("Cannot Pause Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[current Status: " + SpreadManager.getSelectedSpread().getStatus() + "]");
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_STOPPED)) {
            log.info("Cannot Pause Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[current Status: " + SpreadManager.getSelectedSpread().getStatus() + "]");
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_COMPLETED)) {
            log.info("Cannot Pause Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[current Status: " + SpreadManager.getSelectedSpread().getStatus() + "]");
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_READY)) {
            log.info("Cannot Pause Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[current Status: " + SpreadManager.getSelectedSpread().getStatus() + "]");
        } else {
            log.info("Pausing Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[Sending Open Orders Cancel .. ]");
            SpreadManager.getSelectedSpread().setStatus(SpreadManager.SPREAD_STATUS_PAUSING);
            SpreadManager.getSelectedSpread().getDtmHandler().sendSpreadCancels();
        }
    }

    public void pauseAllSpreadActionPerformed(ActionEvent e) {
        log.info("Pause All Spreads invoked.");
        Vector allSpreads = SpreadManager.getAllSpreads();
        for (int i = 0; i < allSpreads.size(); i++) {
            Spread spread = (Spread) allSpreads.elementAt(i);
            if (spread.getStatus().equals(SpreadManager.SPREAD_STATUS_PAUSED)) {
                log.info("Pausing Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[Sending Open Orders Cancel .. ]");
            } else if (spread.getStatus().equals(SpreadManager.SPREAD_STATUS_STOPPED)) {
                log.info("Pausing Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[Sending Open Orders Cancel .. ]");
            } else if (spread.getStatus().equals(SpreadManager.SPREAD_STATUS_COMPLETED)) {
                log.info("Pausing Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[Sending Open Orders Cancel .. ]");
            } else if (spread.getStatus().equals(SpreadManager.SPREAD_STATUS_READY)) {
                log.info("Pausing Spread: " + SpreadManager.getSelectedSpread().getSpreadID() + "[Sending Open Orders Cancel .. ]");
            } else {
                log.info("Pausing Spread: " + spread.getSpreadID() + " [Sending Open Order Cancels .. ]");
                spread.setStatus(SpreadManager.SPREAD_STATUS_PAUSING);
                spread.getDtmHandler().sendSpreadCancels();
            }
        }
    }

    public void stopSpreadActionPerformed(ActionEvent e) {
        if (SpreadManager.getSelectedSpread() == null) {
        } else if (SpreadManager.getSelectedSpread().getStatus().equals(SpreadManager.SPREAD_STATUS_PAUSED)) {
            log.info("Stop Spread : " + SpreadManager.getSelectedSpread().getSpreadID());
            SpreadManager.getSelectedSpread().setStatus(SpreadManager.SPREAD_STATUS_STOPPED);
            SpreadManager.getSelectedSpread().getSTableModel().updateSpread(SpreadManager.getSelectedSpread());
        }
    }

    public void columnSettingsActionPerformed(ActionEvent e) {
        cellChooser.setVisible(true);
        cellChooser.repaint();
    }

    public void jMenuFileExitActionPerformed(ActionEvent e) {
        int opt = JOptionPane.showConfirmDialog(null, "Are you sure you want to close JSpread ?");
        if (opt == 0) {
            Vector _spreads = SpreadManager.getAllSpreads();
            for (int i = 0; i < _spreads.size(); i++) {
                Spread _spread = (Spread) _spreads.elementAt(i);
                _spread.destroySpread();
            }
            MarketGateway.stop();
            MarketDataGateway.stop();
            log.info("Spread Window Closed");
            System.out.println(">>>Settings to Save<<<");
            System.out.println("GUI Width  : " + this.getWidth());
            System.out.println("GUI Height : " + this.getHeight());
            System.out.print("Hidden Cols: ");
            Vector hiddenCols = stTable.getHiddenColumns();
            for (int i = 0; i < hiddenCols.size(); i++) {
                System.out.print(hiddenCols.elementAt(i) + ", ");
            }
            System.exit(0);
        }
    }

    public void jMenuHelpAboutActionPerformed(ActionEvent e) {
        AboutSpread dlg = new AboutSpread(this);
        Dimension dlgSize = dlg.getPreferredSize();
        Dimension frmSize = getSize();
        Point loc = getLocation();
        dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
        dlg.setModal(true);
        dlg.pack();
        dlg.show();
    }

    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            jMenuFileExitActionPerformed(null);
        }
    }
}
