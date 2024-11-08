package org.jmik.asterisk.model.agi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.apache.log4j.Logger;
import org.jdesktop.layout.GroupLayout;
import org.jmik.asterisk.gui.PresentationModel;
import org.jmik.asterisk.model.impl.Call;
import org.jmik.asterisk.model.impl.Channel;
import org.jmik.asterisk.model.impl.ConferenceCall;
import org.jmik.asterisk.model.impl.SinglePartyCall;
import org.jmik.asterisk.model.impl.TwoPartiesCall;

/**
 * Graphic User Interface for call monitor.
 * @author michele
 *
 */
public class CallMonitorGUI extends javax.swing.JFrame implements PresentationModel.Listener {

    private static final long serialVersionUID = 5290308198571753349L;

    private static Logger logger = Logger.getLogger(CallMonitorGUI.class);

    private PresentationModel presentationModel;

    private JMenuBar jMenuBar1;

    private JMenu jMenu1;

    private JMenuItem jMenuItem1;

    private javax.swing.JTabbedPane callsTabs;

    private javax.swing.JButton dropSinglePartyCall_btn;

    private javax.swing.JButton dropTwoPartiesCall_btn;

    private javax.swing.JButton dropConferenceCall_btn;

    private javax.swing.JButton monitorSinglePartyCall_btn;

    private javax.swing.JButton monitorTwoPartiesCall_btn;

    private javax.swing.JButton monitorConferenceCall_btn;

    private javax.swing.JTable singlePartyCalls_tbl;

    private javax.swing.JTable twoPartiesCalls_tbl;

    private javax.swing.JTable conferenceCalls_tbl;

    private SinglePartyCallsTableModel singlePartyCalls_tblModel;

    private TwoPartiesCallsTableModel twoPartiesCalls_tblModel;

    private ConferenceCallsTableModel conferenceCalls_tblModel;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JPanel jPanel2;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JScrollPane jScrollPane3;

    /** Creates new form AgiExp */
    public CallMonitorGUI(PresentationModel presentationModel) {
        if (presentationModel == null) throw new IllegalArgumentException("presentationModel can not be null");
        presentationModel.addListener(this);
        this.presentationModel = presentationModel;
        initComponents();
        logger.info(this);
    }

    public void initComponents() {
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu1.setText("Menu");
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem1.setText("Remove Invalid Calls");
        jMenuItem1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.removeInvalidCalls();
            }
        });
        jMenu1.add(jMenuItem1);
        jMenuBar1.add(jMenu1);
        setJMenuBar(jMenuBar1);
        callsTabs = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        singlePartyCalls_tbl = new javax.swing.JTable();
        singlePartyCalls_tbl.addMouseListener(new MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                presentationModel.setSelectedIndex(PresentationModel.SINGLEPARTY_CALLTYPE, singlePartyCalls_tbl.getSelectedRow());
                dropSinglePartyCall_btn.setEnabled(presentationModel.isDropButtonEnabled(PresentationModel.SINGLEPARTY_CALLTYPE));
                monitorSinglePartyCall_btn.setEnabled(presentationModel.isMonitorButtonEnabled(PresentationModel.SINGLEPARTY_CALLTYPE));
            }
        });
        monitorSinglePartyCall_btn = new javax.swing.JButton();
        monitorSinglePartyCall_btn.setEnabled(false);
        monitorSinglePartyCall_btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.monitorButtonClicked(PresentationModel.SINGLEPARTY_CALLTYPE);
            }
        });
        dropSinglePartyCall_btn = new javax.swing.JButton();
        dropSinglePartyCall_btn.setEnabled(false);
        dropSinglePartyCall_btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.dropButtonClicked(PresentationModel.SINGLEPARTY_CALLTYPE);
            }
        });
        jPanel2 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        twoPartiesCalls_tbl = new javax.swing.JTable();
        twoPartiesCalls_tbl.addMouseListener(new MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                presentationModel.setSelectedIndex(PresentationModel.TWOPARTIES_CALLTYPE, twoPartiesCalls_tbl.getSelectedRow());
                dropTwoPartiesCall_btn.setEnabled(presentationModel.isDropButtonEnabled(PresentationModel.TWOPARTIES_CALLTYPE));
                monitorTwoPartiesCall_btn.setEnabled(presentationModel.isMonitorButtonEnabled(PresentationModel.TWOPARTIES_CALLTYPE));
            }
        });
        monitorTwoPartiesCall_btn = new javax.swing.JButton();
        monitorTwoPartiesCall_btn.setEnabled(false);
        monitorTwoPartiesCall_btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.monitorButtonClicked(PresentationModel.TWOPARTIES_CALLTYPE);
            }
        });
        dropTwoPartiesCall_btn = new javax.swing.JButton();
        dropTwoPartiesCall_btn.setEnabled(false);
        dropTwoPartiesCall_btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.dropButtonClicked(PresentationModel.TWOPARTIES_CALLTYPE);
            }
        });
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        conferenceCalls_tbl = new javax.swing.JTable();
        conferenceCalls_tbl.addMouseListener(new MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                presentationModel.setSelectedIndex(PresentationModel.CONFERENCE_CALLTYPE, conferenceCalls_tbl.getSelectedRow());
                dropConferenceCall_btn.setEnabled(presentationModel.isDropButtonEnabled(PresentationModel.CONFERENCE_CALLTYPE));
                monitorConferenceCall_btn.setEnabled(presentationModel.isMonitorButtonEnabled(PresentationModel.CONFERENCE_CALLTYPE));
            }
        });
        monitorConferenceCall_btn = new javax.swing.JButton();
        monitorConferenceCall_btn.setEnabled(false);
        monitorConferenceCall_btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.monitorButtonClicked(PresentationModel.CONFERENCE_CALLTYPE);
            }
        });
        dropConferenceCall_btn = new javax.swing.JButton();
        dropConferenceCall_btn.setEnabled(false);
        dropConferenceCall_btn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                presentationModel.dropButtonClicked(PresentationModel.CONFERENCE_CALLTYPE);
            }
        });
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        singlePartyCalls_tblModel = new SinglePartyCallsTableModel();
        singlePartyCalls_tbl.setModel(singlePartyCalls_tblModel);
        jScrollPane1.setViewportView(singlePartyCalls_tbl);
        monitorSinglePartyCall_btn.setText("Monitor");
        monitorSinglePartyCall_btn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                monitorSinglePartyCall_btnActionPerformed(evt);
            }
        });
        dropSinglePartyCall_btn.setText("Drop");
        GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap().add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE).add(jPanel1Layout.createSequentialGroup().add(dropSinglePartyCall_btn).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(monitorSinglePartyCall_btn))).addContainerGap()));
        jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap().add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(monitorSinglePartyCall_btn).add(dropSinglePartyCall_btn)).addContainerGap()));
        callsTabs.addTab("Single Party Calls", jPanel1);
        twoPartiesCalls_tblModel = new TwoPartiesCallsTableModel();
        twoPartiesCalls_tbl.setModel(twoPartiesCalls_tblModel);
        jScrollPane2.setViewportView(twoPartiesCalls_tbl);
        monitorTwoPartiesCall_btn.setText("Monitor");
        dropTwoPartiesCall_btn.setText("Drop");
        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup().addContainerGap().add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE).add(jPanel2Layout.createSequentialGroup().add(dropTwoPartiesCall_btn).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(monitorTwoPartiesCall_btn))).addContainerGap()));
        jPanel2Layout.setVerticalGroup(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup().addContainerGap().add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(monitorTwoPartiesCall_btn).add(dropTwoPartiesCall_btn)).addContainerGap()));
        callsTabs.addTab("Two Parties Calls", jPanel2);
        conferenceCalls_tblModel = new ConferenceCallsTableModel();
        conferenceCalls_tbl.setModel(conferenceCalls_tblModel);
        jScrollPane3.setViewportView(conferenceCalls_tbl);
        monitorConferenceCall_btn.setText("Monitor");
        dropConferenceCall_btn.setText("Drop");
        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup().addContainerGap().add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING).add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE).add(jPanel3Layout.createSequentialGroup().add(dropConferenceCall_btn).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(monitorConferenceCall_btn))).addContainerGap()));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup().addContainerGap().add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(monitorConferenceCall_btn).add(dropConferenceCall_btn)).addContainerGap()));
        callsTabs.addTab("Conference Calls", jPanel3);
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(callsTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 509, Short.MAX_VALUE).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(callsTabs, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 414, Short.MAX_VALUE).addContainerGap()));
        pack();
    }

    private void monitorSinglePartyCall_btnActionPerformed(java.awt.event.ActionEvent evt) {
        logger.info("TODO monitorSinglePartyCall_btnActionPerformed");
    }

    public class SinglePartyCallsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        public int getColumnCount() {
            return 4;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "ID";
                case 1:
                    return "Caller ID";
                case 2:
                    return "State";
                case 3:
                    return "Reason";
            }
            return null;
        }

        public int getRowCount() {
            return presentationModel.getCalls(PresentationModel.SINGLEPARTY_CALLTYPE).size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            SinglePartyCall call = (SinglePartyCall) presentationModel.getCalls(PresentationModel.SINGLEPARTY_CALLTYPE).get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return call.getId();
                case 1:
                    return call.getChannel().getDescriptor().getEndpoint().getId();
                case 2:
                    switch(call.getState()) {
                        case Call.IDLE_STATE:
                            return "IDLE";
                        case Call.CONNECTING_STATE:
                            return "CONNECTING";
                        case Call.ACTIVE_STATE:
                            return "ACTIVE";
                        case Call.INVALID_STATE:
                            return "INVALID";
                    }
                case 3:
                    return call.getReasonForStateChange();
            }
            return null;
        }
    }

    public class TwoPartiesCallsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        public int getRowCount() {
            return presentationModel.getCalls(PresentationModel.TWOPARTIES_CALLTYPE).size();
        }

        public int getColumnCount() {
            return 5;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "ID";
                case 1:
                    return "Caller ID";
                case 2:
                    return "Called ID";
                case 3:
                    return "State";
                case 4:
                    return "Reason";
            }
            return null;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            TwoPartiesCall call = (TwoPartiesCall) presentationModel.getCalls(PresentationModel.TWOPARTIES_CALLTYPE).get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return call.getId();
                case 1:
                    return call.getCallerChannel().getDescriptor().getEndpoint().getId();
                case 2:
                    return call.getCalledChannel().getDescriptor().getEndpoint().getId();
                case 3:
                    switch(call.getState()) {
                        case Call.IDLE_STATE:
                            return "IDLE";
                        case Call.CONNECTING_STATE:
                            return "CONNECTING";
                        case Call.ACTIVE_STATE:
                            return "ACTIVE";
                        case Call.INVALID_STATE:
                            return "INVALID";
                    }
                case 4:
                    return call.getReasonForStateChange();
            }
            return null;
        }
    }

    public class ConferenceCallsTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        public int getRowCount() {
            return presentationModel.getCalls(PresentationModel.CONFERENCE_CALLTYPE).size();
        }

        public int getColumnCount() {
            return 4;
        }

        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return "ID";
                case 1:
                    return "Participants";
                case 2:
                    return "State";
                case 3:
                    return "Reason";
            }
            return null;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            ConferenceCall call = (ConferenceCall) presentationModel.getCalls(PresentationModel.CONFERENCE_CALLTYPE).get(rowIndex);
            switch(columnIndex) {
                case 0:
                    return call.getId();
                case 1:
                    return new Integer(call.getChannels().size());
                case 2:
                    switch(call.getState()) {
                        case Call.IDLE_STATE:
                            return "IDLE";
                        case Call.CONNECTING_STATE:
                            return "CONNECTING";
                        case Call.ACTIVE_STATE:
                            return "ACTIVE";
                        case Call.INVALID_STATE:
                            return "INVALID";
                    }
                case 3:
                    return call.getReasonForStateChange();
            }
            return null;
        }
    }

    public void callAttached(PresentationModel model, Call call) {
        if (call instanceof SinglePartyCall) {
            fireTableDataChanged(singlePartyCalls_tblModel, dropSinglePartyCall_btn, monitorSinglePartyCall_btn, PresentationModel.SINGLEPARTY_CALLTYPE);
        } else if (call instanceof TwoPartiesCall) {
            fireTableDataChanged(twoPartiesCalls_tblModel, dropTwoPartiesCall_btn, monitorTwoPartiesCall_btn, PresentationModel.TWOPARTIES_CALLTYPE);
        } else if (call instanceof ConferenceCall) {
            fireTableDataChanged(conferenceCalls_tblModel, dropConferenceCall_btn, monitorConferenceCall_btn, PresentationModel.CONFERENCE_CALLTYPE);
        }
        logger.info("callAttached " + call);
    }

    public void refreshTable(int callType) {
        switch(callType) {
            case PresentationModel.SINGLEPARTY_CALLTYPE:
                fireTableDataChanged(singlePartyCalls_tblModel, dropSinglePartyCall_btn, monitorSinglePartyCall_btn, PresentationModel.SINGLEPARTY_CALLTYPE);
                break;
            case PresentationModel.TWOPARTIES_CALLTYPE:
                fireTableDataChanged(twoPartiesCalls_tblModel, dropTwoPartiesCall_btn, monitorTwoPartiesCall_btn, PresentationModel.TWOPARTIES_CALLTYPE);
                break;
            case PresentationModel.CONFERENCE_CALLTYPE:
                fireTableDataChanged(conferenceCalls_tblModel, dropConferenceCall_btn, monitorConferenceCall_btn, PresentationModel.CONFERENCE_CALLTYPE);
                break;
        }
        logger.info("refreshTable " + callType);
    }

    public void callStateChanged(PresentationModel model, int oldState, Call call) {
        if (call instanceof SinglePartyCall) {
            fireTableDataChanged(singlePartyCalls_tblModel, dropSinglePartyCall_btn, monitorSinglePartyCall_btn, PresentationModel.SINGLEPARTY_CALLTYPE);
        } else if (call instanceof TwoPartiesCall) {
            fireTableDataChanged(twoPartiesCalls_tblModel, dropTwoPartiesCall_btn, monitorTwoPartiesCall_btn, PresentationModel.TWOPARTIES_CALLTYPE);
        } else if (call instanceof ConferenceCall) {
            fireTableDataChanged(conferenceCalls_tblModel, dropConferenceCall_btn, monitorConferenceCall_btn, PresentationModel.CONFERENCE_CALLTYPE);
        }
        logger.info("callStateChanged " + call);
    }

    public void channelAdded(PresentationModel model, ConferenceCall conferenceCall, Channel channel) {
        fireTableDataChanged(conferenceCalls_tblModel, dropConferenceCall_btn, monitorConferenceCall_btn, PresentationModel.CONFERENCE_CALLTYPE);
        logger.info("channelAdded " + channel);
    }

    public void channelRemoved(PresentationModel model, ConferenceCall conferenceCall, Channel channel) {
        fireTableDataChanged(conferenceCalls_tblModel, dropConferenceCall_btn, monitorConferenceCall_btn, PresentationModel.CONFERENCE_CALLTYPE);
        logger.info("channelRemoved " + channel);
    }

    private void fireTableDataChanged(final AbstractTableModel tableModel, final JButton dropButton, final JButton monitorButton, final int callType) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                tableModel.fireTableDataChanged();
                dropButton.setEnabled(presentationModel.isDropButtonEnabled(callType));
                monitorButton.setEnabled(presentationModel.isMonitorButtonEnabled(callType));
            }
        });
    }
}
