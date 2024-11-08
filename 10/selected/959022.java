package org.compiere.apps.form;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.math.*;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.*;
import org.adempiere.plaf.AdempierePLAF;
import org.compiere.apps.*;
import org.compiere.grid.ed.*;
import org.compiere.minigrid.*;
import org.compiere.model.*;
import org.compiere.plaf.*;
import org.compiere.print.*;
import org.compiere.process.*;
import org.compiere.swing.*;
import org.compiere.util.*;

/**
 *	Manual Shipment Selection
 *
 *  @author Jorg Janke
 *  @version $Id: VInOutGen.java,v 1.2 2006/07/30 00:51:28 jjanke Exp $
 */
public class VInOutGen extends CPanel implements FormPanel, ActionListener, VetoableChangeListener, ChangeListener, TableModelListener, ASyncProcess {

    /**
	 *	Initialize Panel
	 *  @param WindowNo window
	 *  @param frame frame
	 */
    public void init(int WindowNo, FormFrame frame) {
        log.info("");
        m_WindowNo = WindowNo;
        m_frame = frame;
        Env.setContext(Env.getCtx(), m_WindowNo, "IsSOTrx", "Y");
        try {
            fillPicks();
            jbInit();
            dynInit();
            frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
            frame.getContentPane().add(statusBar, BorderLayout.SOUTH);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "init", ex);
        }
    }

    /**	Window No			*/
    private int m_WindowNo = 0;

    /**	FormFrame			*/
    private FormFrame m_frame;

    private boolean m_selectionActive = true;

    private Object m_M_Warehouse_ID = null;

    private Object m_C_BPartner_ID = null;

    /**	Logger			*/
    private static CLogger log = CLogger.getCLogger(VInOutGen.class);

    private CTabbedPane tabbedPane = new CTabbedPane();

    private CPanel selPanel = new CPanel();

    private CPanel selNorthPanel = new CPanel();

    private BorderLayout selPanelLayout = new BorderLayout();

    private CLabel lWarehouse = new CLabel();

    private VLookup fWarehouse;

    private CLabel lBPartner = new CLabel();

    private VLookup fBPartner;

    private FlowLayout northPanelLayout = new FlowLayout();

    private ConfirmPanel confirmPanelSel = new ConfirmPanel(true);

    private ConfirmPanel confirmPanelGen = new ConfirmPanel(false, true, false, false, false, false, true);

    private StatusBar statusBar = new StatusBar();

    private CPanel genPanel = new CPanel();

    private BorderLayout genLayout = new BorderLayout();

    private CTextPane info = new CTextPane();

    private JScrollPane scrollPane = new JScrollPane();

    private MiniTable miniTable = new MiniTable();

    private CLabel lDocType = new CLabel();

    private VComboBox cmbDocType = new VComboBox();

    /** User selection */
    private ArrayList<Integer> selection = null;

    /**
	 *	Static Init.
	 *  <pre>
	 *  selPanel (tabbed)
	 *      fOrg, fBPartner
	 *      scrollPane & miniTable
	 *  genPanel
	 *      info
	 *  </pre>
	 *  @throws Exception
	 */
    void jbInit() throws Exception {
        CompiereColor.setBackground(this);
        selPanel.setLayout(selPanelLayout);
        lWarehouse.setLabelFor(fWarehouse);
        lBPartner.setLabelFor(fBPartner);
        lBPartner.setText("BPartner");
        selNorthPanel.setLayout(northPanelLayout);
        northPanelLayout.setAlignment(FlowLayout.LEFT);
        tabbedPane.add(selPanel, Msg.getMsg(Env.getCtx(), "Select"));
        selPanel.add(selNorthPanel, BorderLayout.NORTH);
        selNorthPanel.add(lWarehouse, null);
        selNorthPanel.add(fWarehouse, null);
        selNorthPanel.add(lBPartner, null);
        selNorthPanel.add(fBPartner, null);
        selPanel.setName("selPanel");
        selPanel.add(confirmPanelSel, BorderLayout.SOUTH);
        selPanel.add(scrollPane, BorderLayout.CENTER);
        scrollPane.getViewport().add(miniTable, null);
        confirmPanelSel.addActionListener(this);
        tabbedPane.add(genPanel, Msg.getMsg(Env.getCtx(), "Generate"));
        genPanel.setLayout(genLayout);
        genPanel.add(info, BorderLayout.CENTER);
        genPanel.setEnabled(false);
        info.setBackground(AdempierePLAF.getFieldBackground_Inactive());
        info.setEditable(false);
        genPanel.add(confirmPanelGen, BorderLayout.SOUTH);
        confirmPanelGen.addActionListener(this);
        lDocType.setLabelFor(cmbDocType);
        selNorthPanel.add(lDocType, null);
        selNorthPanel.add(cmbDocType, null);
    }

    /**
	 *	Fill Picks.
	 *		Column_ID from C_Order
	 *  @throws Exception if Lookups cannot be initialized
	 */
    private void fillPicks() throws Exception {
        MLookup orgL = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, 2223, DisplayType.TableDir);
        fWarehouse = new VLookup("M_Warehouse_ID", true, false, true, orgL);
        lWarehouse.setText(Msg.translate(Env.getCtx(), "M_Warehouse_ID"));
        fWarehouse.addVetoableChangeListener(this);
        m_M_Warehouse_ID = fWarehouse.getValue();
        MLookup bpL = MLookupFactory.get(Env.getCtx(), m_WindowNo, 0, 2762, DisplayType.Search);
        fBPartner = new VLookup("C_BPartner_ID", false, false, true, bpL);
        lBPartner.setText(Msg.translate(Env.getCtx(), "C_BPartner_ID"));
        fBPartner.addVetoableChangeListener(this);
        lDocType.setText(Msg.translate(Env.getCtx(), "C_DocType_ID"));
        cmbDocType.addItem(new KeyNamePair(MOrder.Table_ID, Msg.translate(Env.getCtx(), "Order")));
        cmbDocType.addItem(new KeyNamePair(MRMA.Table_ID, Msg.translate(Env.getCtx(), "VendorRMA")));
        cmbDocType.addActionListener(this);
    }

    /**
	 *	Dynamic Init.
	 *	- Create GridController & Panel
	 *	- AD_Column_ID from C_Order
	 */
    private void dynInit() {
        miniTable.addColumn("C_Order_ID");
        miniTable.addColumn("AD_Org_ID");
        miniTable.addColumn("C_DocType_ID");
        miniTable.addColumn("DocumentNo");
        miniTable.addColumn("C_BPartner_ID");
        miniTable.addColumn("DateOrdered");
        miniTable.addColumn("TotalLines");
        miniTable.setMultiSelection(true);
        miniTable.setRowSelectionAllowed(true);
        miniTable.setColumnClass(0, IDColumn.class, false, " ");
        miniTable.setColumnClass(1, String.class, true, Msg.translate(Env.getCtx(), "AD_Org_ID"));
        miniTable.setColumnClass(2, String.class, true, Msg.translate(Env.getCtx(), "C_DocType_ID"));
        miniTable.setColumnClass(3, String.class, true, Msg.translate(Env.getCtx(), "DocumentNo"));
        miniTable.setColumnClass(4, String.class, true, Msg.translate(Env.getCtx(), "C_BPartner_ID"));
        miniTable.setColumnClass(5, Timestamp.class, true, Msg.translate(Env.getCtx(), "DateOrdered"));
        miniTable.setColumnClass(6, BigDecimal.class, true, Msg.translate(Env.getCtx(), "TotalLines"));
        miniTable.autoSize();
        miniTable.getModel().addTableModelListener(this);
        statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "InOutGenerateSel"));
        statusBar.setStatusDB(" ");
        tabbedPane.addChangeListener(this);
    }

    /**
	 * Get SQL for Orders that needs to be shipped
	 * @return sql
	 */
    private String getOrderSQL() {
        StringBuffer sql = new StringBuffer("SELECT C_Order_ID, o.Name, dt.Name, DocumentNo, bp.Name, DateOrdered, TotalLines " + "FROM M_InOut_Candidate_v ic, AD_Org o, C_BPartner bp, C_DocType dt " + "WHERE ic.AD_Org_ID=o.AD_Org_ID" + " AND ic.C_BPartner_ID=bp.C_BPartner_ID" + " AND ic.C_DocType_ID=dt.C_DocType_ID" + " AND ic.AD_Client_ID=?");
        if (m_M_Warehouse_ID != null) sql.append(" AND ic.M_Warehouse_ID=").append(m_M_Warehouse_ID);
        if (m_C_BPartner_ID != null) sql.append(" AND ic.C_BPartner_ID=").append(m_C_BPartner_ID);
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        String lockedIDs = MPrivateAccess.getLockedRecordWhere(MOrder.Table_ID, AD_User_ID);
        if (lockedIDs != null) {
            if (sql.length() > 0) sql.append(" AND ");
            sql.append("C_Order_ID").append(lockedIDs);
        }
        sql.append(" ORDER BY o.Name,bp.Name,DateOrdered");
        return sql.toString();
    }

    /**
	 * Get SQL for Vendor RMA that need to be shipped
	 * @return sql
	 */
    private String getRMASql() {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT rma.M_RMA_ID, org.Name, dt.Name, rma.DocumentNo, bp.Name, rma.Created, rma.Amt ");
        sql.append("FROM M_RMA rma INNER JOIN AD_Org org ON rma.AD_Org_ID=org.AD_Org_ID ");
        sql.append("INNER JOIN C_DocType dt ON rma.C_DocType_ID=dt.C_DocType_ID ");
        sql.append("INNER JOIN C_BPartner bp ON rma.C_BPartner_ID=bp.C_BPartner_ID ");
        sql.append("INNER JOIN M_InOut io ON rma.InOut_ID=io.M_InOut_ID ");
        sql.append("WHERE rma.DocStatus='CO' ");
        sql.append("AND dt.DocBaseType = 'POO' ");
        sql.append("AND EXISTS (SELECT * FROM M_RMA r INNER JOIN M_RMALine rl ");
        sql.append("ON r.M_RMA_ID=rl.M_RMA_ID WHERE r.M_RMA_ID=rma.M_RMA_ID ");
        sql.append("AND rl.IsActive='Y' AND rl.M_InOutLine_ID > 0 AND rl.QtyDelivered < rl.Qty) ");
        sql.append("AND NOT EXISTS (SELECT * FROM M_InOut oio WHERE oio.M_RMA_ID=rma.M_RMA_ID ");
        sql.append("AND oio.DocStatus IN ('IP', 'CO', 'CL')) ");
        sql.append("AND rma.AD_Client_ID=?");
        if (m_M_Warehouse_ID != null) sql.append(" AND io.M_Warehouse_ID=").append(m_M_Warehouse_ID);
        if (m_C_BPartner_ID != null) sql.append(" AND bp.C_BPartner_ID=").append(m_C_BPartner_ID);
        int AD_User_ID = Env.getContextAsInt(Env.getCtx(), "#AD_User_ID");
        String lockedIDs = MPrivateAccess.getLockedRecordWhere(MRMA.Table_ID, AD_User_ID);
        if (lockedIDs != null) {
            sql.append(" AND rma.M_RMA_ID").append(lockedIDs);
        }
        sql.append(" ORDER BY org.Name, bp.Name, rma.Created ");
        return sql.toString();
    }

    /**
	 *  Query Info
	 */
    private void executeQuery() {
        log.info("");
        int AD_Client_ID = Env.getAD_Client_ID(Env.getCtx());
        String sql = "";
        KeyNamePair docTypeKNPair = (KeyNamePair) cmbDocType.getSelectedItem();
        if (docTypeKNPair.getKey() == MRMA.Table_ID) {
            sql = getRMASql();
        } else {
            sql = getOrderSQL();
        }
        log.fine(sql);
        int row = 0;
        miniTable.setRowCount(row);
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql.toString(), null);
            pstmt.setInt(1, AD_Client_ID);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                miniTable.setRowCount(row + 1);
                miniTable.setValueAt(new IDColumn(rs.getInt(1)), row, 0);
                miniTable.setValueAt(rs.getString(2), row, 1);
                miniTable.setValueAt(rs.getString(3), row, 2);
                miniTable.setValueAt(rs.getString(4), row, 3);
                miniTable.setValueAt(rs.getString(5), row, 4);
                miniTable.setValueAt(rs.getTimestamp(6), row, 5);
                miniTable.setValueAt(rs.getBigDecimal(7), row, 6);
                row++;
            }
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            log.log(Level.SEVERE, sql.toString(), e);
        }
        miniTable.autoSize();
    }

    /**
	 * 	Dispose
	 */
    public void dispose() {
        if (m_frame != null) m_frame.dispose();
        m_frame = null;
    }

    /**
	 *	Action Listener
	 *  @param e event
	 */
    public void actionPerformed(ActionEvent e) {
        log.info("Cmd=" + e.getActionCommand());
        if (e.getActionCommand().equals(ConfirmPanel.A_CANCEL)) {
            dispose();
            return;
        }
        if (cmbDocType.equals(e.getSource())) {
            executeQuery();
            return;
        }
        saveSelection();
        if (selection != null && selection.size() > 0 && m_selectionActive && m_M_Warehouse_ID != null) generateShipments(); else dispose();
    }

    /**
	 *	Vetoable Change Listener - requery
	 *  @param e event
	 */
    public void vetoableChange(PropertyChangeEvent e) {
        log.info(e.getPropertyName() + "=" + e.getNewValue());
        if (e.getPropertyName().equals("M_Warehouse_ID")) m_M_Warehouse_ID = e.getNewValue();
        if (e.getPropertyName().equals("C_BPartner_ID")) {
            m_C_BPartner_ID = e.getNewValue();
            fBPartner.setValue(m_C_BPartner_ID);
        }
        executeQuery();
    }

    /**
	 *	Change Listener (Tab changed)
	 *  @param e event
	 */
    public void stateChanged(ChangeEvent e) {
        int index = tabbedPane.getSelectedIndex();
        m_selectionActive = (index == 0);
    }

    /**
	 *  Table Model Listener
	 *  @param e event
	 */
    public void tableChanged(TableModelEvent e) {
        int rowsSelected = 0;
        int rows = miniTable.getRowCount();
        for (int i = 0; i < rows; i++) {
            IDColumn id = (IDColumn) miniTable.getValueAt(i, 0);
            if (id != null && id.isSelected()) rowsSelected++;
        }
        statusBar.setStatusDB(" " + rowsSelected + " ");
    }

    /**
	 *	Save Selection & return selecion Query or ""
	 *  @return where clause like C_Order_ID IN (...)
	 */
    private void saveSelection() {
        log.info("");
        miniTable.editingStopped(new ChangeEvent(this));
        ArrayList<Integer> results = new ArrayList<Integer>();
        selection = null;
        int rows = miniTable.getRowCount();
        for (int i = 0; i < rows; i++) {
            IDColumn id = (IDColumn) miniTable.getValueAt(i, 0);
            if (id != null && id.isSelected()) results.add(id.getRecord_ID());
        }
        if (results.size() == 0) return;
        log.config("Selected #" + results.size());
        selection = results;
    }

    /**************************************************************************
	 *	Generate Shipments
	 */
    private void generateShipments() {
        log.info("M_Warehouse_ID=" + m_M_Warehouse_ID);
        String trxName = Trx.createTrxName("IOG");
        Trx trx = Trx.get(trxName, true);
        m_selectionActive = false;
        statusBar.setStatusLine(Msg.getMsg(Env.getCtx(), "InOutGenerateGen"));
        statusBar.setStatusDB(String.valueOf(selection.size()));
        int AD_Process_ID = 0;
        KeyNamePair docTypeKNPair = (KeyNamePair) cmbDocType.getSelectedItem();
        if (docTypeKNPair.getKey() == MRMA.Table_ID) {
            AD_Process_ID = 52001;
        } else {
            AD_Process_ID = 199;
        }
        MPInstance instance = new MPInstance(Env.getCtx(), AD_Process_ID, 0);
        if (!instance.save()) {
            info.setText(Msg.getMsg(Env.getCtx(), "ProcessNoInstance"));
            return;
        }
        StringBuffer insert = new StringBuffer();
        insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
        int counter = 0;
        for (Integer selectedId : selection) {
            counter++;
            if (counter > 1) insert.append(" UNION ");
            insert.append("SELECT ");
            insert.append(instance.getAD_PInstance_ID());
            insert.append(", ");
            insert.append(selectedId);
            insert.append(" FROM DUAL ");
            if (counter == 1000) {
                if (DB.executeUpdate(insert.toString(), trxName) < 0) {
                    String msg = "No Shipments";
                    log.config(msg);
                    info.setText(msg);
                    trx.rollback();
                    return;
                }
                insert = new StringBuffer();
                insert.append("INSERT INTO T_SELECTION(AD_PINSTANCE_ID, T_SELECTION_ID) ");
                counter = 0;
            }
        }
        if (counter > 0) {
            if (DB.executeUpdate(insert.toString(), trxName) < 0) {
                String msg = "No Shipments";
                log.config(msg);
                info.setText(msg);
                trx.rollback();
                return;
            }
        }
        ProcessInfo pi = new ProcessInfo("VInOutGen", AD_Process_ID);
        pi.setAD_PInstance_ID(instance.getAD_PInstance_ID());
        MPInstancePara ip = new MPInstancePara(instance, 10);
        ip.setParameter("Selection", "Y");
        if (!ip.save()) {
            String msg = "No Parameter added";
            info.setText(msg);
            log.log(Level.SEVERE, msg);
            return;
        }
        ip = new MPInstancePara(instance, 20);
        ip.setParameter("M_Warehouse_ID", Integer.parseInt(m_M_Warehouse_ID.toString()));
        if (!ip.save()) {
            String msg = "No Parameter added";
            info.setText(msg);
            log.log(Level.SEVERE, msg);
            return;
        }
        ProcessCtl worker = new ProcessCtl(this, Env.getWindowNo(this), pi, trx);
        worker.start();
    }

    /**
	 *  Complete generating shipments.
	 *  Called from Unlock UI
	 *  @param pi process info
	 */
    private void generateShipments_complete(ProcessInfo pi) {
        tabbedPane.setSelectedIndex(1);
        ProcessInfoUtil.setLogFromDB(pi);
        StringBuffer iText = new StringBuffer();
        iText.append("<b>").append(pi.getSummary()).append("</b><br>(").append(Msg.getMsg(Env.getCtx(), "InOutGenerateInfo")).append(")<br>").append(pi.getLogInfo(true));
        info.setText(iText.toString());
        int[] ids = pi.getIDs();
        if (ids == null || ids.length == 0) return;
        log.config("PrintItems=" + ids.length);
        confirmPanelGen.getOKButton().setEnabled(false);
        if (ADialog.ask(m_WindowNo, this, "PrintShipments")) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int retValue = ADialogDialog.A_CANCEL;
            do {
                for (int i = 0; i < ids.length; i++) {
                    int M_InOut_ID = ids[i];
                    ReportCtl.startDocumentPrint(ReportEngine.SHIPMENT, M_InOut_ID, this, Env.getWindowNo(this), true);
                }
                ADialogDialog d = new ADialogDialog(m_frame, Env.getHeader(Env.getCtx(), m_WindowNo), Msg.getMsg(Env.getCtx(), "PrintoutOK?"), JOptionPane.QUESTION_MESSAGE);
                retValue = d.getReturnCode();
            } while (retValue == ADialogDialog.A_CANCEL);
            setCursor(Cursor.getDefaultCursor());
        }
        confirmPanelGen.getOKButton().setEnabled(true);
    }

    /**************************************************************************
	 *  Lock User Interface.
	 *  Called from the Worker before processing
	 *  @param pi process info
	 */
    public void lockUI(ProcessInfo pi) {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.setEnabled(false);
    }

    /**
	 *  Unlock User Interface.
	 *  Called from the Worker when processing is done
	 *  @param pi result of execute ASync call
	 */
    public void unlockUI(ProcessInfo pi) {
        this.setEnabled(true);
        this.setCursor(Cursor.getDefaultCursor());
        generateShipments_complete(pi);
    }

    /**
	 *  Is the UI locked (Internal method)
	 *  @return true, if UI is locked
	 */
    public boolean isUILocked() {
        return this.isEnabled();
    }

    /**
	 *  Method to be executed async.
	 *  Called from the Worker
	 *  @param pi ProcessInfo
	 */
    public void executeASync(ProcessInfo pi) {
    }
}
