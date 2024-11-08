package org.aacc.administrationpanel.campaigns;

import org.aacc.administrationpanel.AdministrationPanelView;
import org.aacc.administrationpanel.LoginUser;
import org.aacc.administrationpanel.socketservices.DaemonErrorException;
import org.aacc.administrationpanel.socketservices.RequestErrorException;
import java.awt.HeadlessException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import org.aacc.utils.comm.AACCResponse;
import org.aacc.utils.comm.CampaignNotifyMessage;
import org.aacc.utils.comm.UnsupportedMessageException;
import org.aacc.utils.comm.UpdateCampaignMessage;

/**
 * A centralized panel for managing campaigns. 
 * <br>
 * User will be able to do basic management, such as changing campaign parameters, as well
 * as more complex operations, such as creating call dispositions, products, assigning
 * users as agents, supervisors, or administrators, and work with a searchable CDR, which
 * also allows to select, listen to, and save, recorded conversations.
 * <br>
 * Additionally, a user will be able to start and stop predictive campaigns, and load
 * one or more contact lists for the campaign.
 * @author  Fernando
 */
public class CampaignAdmin extends javax.swing.JFrame {

    private String campaign = "";

    private LoginUser me = AdministrationPanelView.getMe();

    private Connection dbConnection = null;

    /** Creates new form CampaignAdmin */
    public CampaignAdmin() {
        try {
            DriverManager.registerDriver(new com.mysql.jdbc.Driver());
            dbConnection = DriverManager.getConnection(me.getSqlReportsURL(), me.getSqlReportsUser(), me.getSqlReportsPassword());
            initComponents();
            loadCampaigns();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this.getRootPane(), ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
            dispose();
        }
    }

    private void initComponents() {
        cboCampaigns = new javax.swing.JComboBox();
        pnlTabbedPanel = new javax.swing.JTabbedPane();
        pnlGeneral = new javax.swing.JPanel();
        pnlStatus = new javax.swing.JPanel();
        chkRunning = new javax.swing.JCheckBox();
        chkActive = new javax.swing.JCheckBox();
        pnlStatus1 = new javax.swing.JPanel();
        btnAgent = new javax.swing.JRadioButton();
        btnIVR = new javax.swing.JRadioButton();
        pnlDates = new javax.swing.JPanel();
        lblStartDate = new javax.swing.JLabel();
        lblEndDate = new javax.swing.JLabel();
        txtStartDate = new javax.swing.JTextField();
        txtEndDate = new javax.swing.JTextField();
        pnlRecording = new javax.swing.JPanel();
        lblRecordingPolicy = new javax.swing.JLabel();
        cboRecordingPolicy = new javax.swing.JComboBox();
        lblRecordingMaxAge = new javax.swing.JLabel();
        txtRecordingMaxAge = new javax.swing.JTextField();
        lblRecordingMaxAgeDays = new javax.swing.JLabel();
        lblRecordingPct = new javax.swing.JLabel();
        txtRecordingPct = new javax.swing.JTextField();
        pnlAgentCampaignDetails = new javax.swing.JPanel();
        chkAgentCanUpdateContacts = new javax.swing.JCheckBox();
        chkAgentCanReschedule = new javax.swing.JCheckBox();
        chkAgentCanRescheduleSelf = new javax.swing.JCheckBox();
        lblURL = new javax.swing.JLabel();
        txtURL = new javax.swing.JTextField();
        lblACW = new javax.swing.JLabel();
        txtACW = new javax.swing.JTextField();
        lblACWSeconds = new javax.swing.JLabel();
        pnlScript = new javax.swing.JPanel();
        scrollPanelScript = new javax.swing.JScrollPane();
        txtScript = new javax.swing.JTextArea();
        pnlDialing = new javax.swing.JPanel();
        pnlPBX = new javax.swing.JPanel();
        lblDialPrefix = new javax.swing.JLabel();
        txtDialPrefix = new javax.swing.JTextField();
        lblDialContext = new javax.swing.JLabel();
        txtDialContext = new javax.swing.JTextField();
        lblDialExtension = new javax.swing.JLabel();
        txtDialExtension = new javax.swing.JTextField();
        lblQueue = new javax.swing.JLabel();
        txtQueue = new javax.swing.JTextField();
        pnlRetryPolicy = new javax.swing.JPanel();
        lblMaxRetries = new javax.swing.JLabel();
        txtMaxRetries = new javax.swing.JTextField();
        lblFirstRetry = new javax.swing.JLabel();
        txtFirstRetry = new javax.swing.JTextField();
        lblFirstRetryMinutes = new javax.swing.JLabel();
        lblSecondRetry = new javax.swing.JLabel();
        txtSecondRetry = new javax.swing.JTextField();
        lblSecondRetryMinutes = new javax.swing.JLabel();
        lblFurtherRetries = new javax.swing.JLabel();
        txtFurtherRetries = new javax.swing.JTextField();
        lblFurtherRetriesMinutes = new javax.swing.JLabel();
        pnlCallHandling = new javax.swing.JPanel();
        lblContext = new javax.swing.JLabel();
        txtContext = new javax.swing.JTextField();
        lblExtension = new javax.swing.JLabel();
        txtExtension = new javax.swing.JTextField();
        pnlDialer = new javax.swing.JPanel();
        lblDialingMethod = new javax.swing.JLabel();
        cboDialingMethod = new javax.swing.JComboBox();
        lblContactBatchSize = new javax.swing.JLabel();
        txtContactBatchSize = new javax.swing.JTextField();
        lblRescheduleBatchPct = new javax.swing.JLabel();
        txtRescheduleBatchPct = new javax.swing.JTextField();
        lblAdjustRatio = new javax.swing.JLabel();
        txtAdjustRatio = new javax.swing.JTextField();
        lblReserveAgents = new javax.swing.JLabel();
        txtReserveAgents = new javax.swing.JTextField();
        lblDialLimit = new javax.swing.JLabel();
        txtDialLimit = new javax.swing.JTextField();
        lblInitialDialingRatio = new javax.swing.JLabel();
        txtInitialDialingRatio = new javax.swing.JTextField();
        lblMinDialingRatio = new javax.swing.JLabel();
        txtMinDialingRatio = new javax.swing.JTextField();
        lblMaxDialingRatio = new javax.swing.JLabel();
        txtMaxDialingRatio = new javax.swing.JTextField();
        lblMaxIVRChannels = new javax.swing.JLabel();
        txtMaxIVRChannels = new javax.swing.JTextField();
        lblDNCListPreference = new javax.swing.JLabel();
        cboDNCListPreference = new javax.swing.JComboBox();
        lblRetryBatchPct = new javax.swing.JLabel();
        txtRetryBatchPct = new javax.swing.JTextField();
        pnlCustomFields = new javax.swing.JPanel();
        pnlCallDispositions = new javax.swing.JPanel();
        pnlProducts = new javax.swing.JPanel();
        pnlUsers = new javax.swing.JPanel();
        pnlContacts = new javax.swing.JPanel();
        pnlCalls = new javax.swing.JPanel();
        btnSave = new javax.swing.JButton();
        lblCampaigns = new javax.swing.JLabel();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.aacc.administrationpanel.AdministrationPanelApp.class).getContext().getResourceMap(CampaignAdmin.class);
        setTitle(resourceMap.getString("Form.title"));
        setName("Form");
        cboCampaigns.setName("cboCampaigns");
        cboCampaigns.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cboCampaignsActionPerformed(evt);
            }
        });
        pnlTabbedPanel.setName("pnlTabbedPanel");
        pnlGeneral.setName("pnlGeneral");
        pnlStatus.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlStatus.border.title")));
        pnlStatus.setName("pnlStatus");
        chkRunning.setText(resourceMap.getString("chkRunning.text"));
        chkRunning.setName("chkRunning");
        chkActive.setText(resourceMap.getString("chkActive.text"));
        chkActive.setName("chkActive");
        javax.swing.GroupLayout pnlStatusLayout = new javax.swing.GroupLayout(pnlStatus);
        pnlStatus.setLayout(pnlStatusLayout);
        pnlStatusLayout.setHorizontalGroup(pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlStatusLayout.createSequentialGroup().addGroup(pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(chkActive).addComponent(chkRunning)).addContainerGap(18, Short.MAX_VALUE)));
        pnlStatusLayout.setVerticalGroup(pnlStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlStatusLayout.createSequentialGroup().addComponent(chkRunning).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkActive).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pnlStatus1.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlStatus1.border.title")));
        pnlStatus1.setName("pnlStatus1");
        btnAgent.setText(resourceMap.getString("btnAgent.text"));
        btnAgent.setEnabled(false);
        btnAgent.setName("btnAgent");
        btnIVR.setText(resourceMap.getString("btnIVR.text"));
        btnIVR.setEnabled(false);
        btnIVR.setName("btnIVR");
        javax.swing.GroupLayout pnlStatus1Layout = new javax.swing.GroupLayout(pnlStatus1);
        pnlStatus1.setLayout(pnlStatus1Layout);
        pnlStatus1Layout.setHorizontalGroup(pnlStatus1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlStatus1Layout.createSequentialGroup().addGroup(pnlStatus1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(btnAgent).addComponent(btnIVR)).addContainerGap(28, Short.MAX_VALUE)));
        pnlStatus1Layout.setVerticalGroup(pnlStatus1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlStatus1Layout.createSequentialGroup().addComponent(btnAgent).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnIVR).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pnlDates.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlDates.border.title")));
        pnlDates.setName("pnlDates");
        lblStartDate.setText(resourceMap.getString("lblStartDate.text"));
        lblStartDate.setName("lblStartDate");
        lblEndDate.setText(resourceMap.getString("lblEndDate.text"));
        lblEndDate.setName("lblEndDate");
        txtStartDate.setText(resourceMap.getString("txtStartDate.text"));
        txtStartDate.setName("txtStartDate");
        txtEndDate.setText(resourceMap.getString("txtEndDate.text"));
        txtEndDate.setName("txtEndDate");
        javax.swing.GroupLayout pnlDatesLayout = new javax.swing.GroupLayout(pnlDates);
        pnlDates.setLayout(pnlDatesLayout);
        pnlDatesLayout.setHorizontalGroup(pnlDatesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDatesLayout.createSequentialGroup().addComponent(lblStartDate).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(txtStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(56, 56, 56).addComponent(lblEndDate).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(txtEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(43, Short.MAX_VALUE)));
        pnlDatesLayout.setVerticalGroup(pnlDatesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDatesLayout.createSequentialGroup().addGroup(pnlDatesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblStartDate).addComponent(txtStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblEndDate)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pnlRecording.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlRecording.border.title")));
        pnlRecording.setName("pnlRecording");
        lblRecordingPolicy.setText(resourceMap.getString("lblRecordingPolicy.text"));
        lblRecordingPolicy.setName("lblRecordingPolicy");
        cboRecordingPolicy.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Incoming", "Outgoing", "Incoming + Outgoing" }));
        cboRecordingPolicy.setName("cboRecordingPolicy");
        lblRecordingMaxAge.setText(resourceMap.getString("lblRecordingMaxAge.text"));
        lblRecordingMaxAge.setName("lblRecordingMaxAge");
        txtRecordingMaxAge.setText(resourceMap.getString("txtRecordingMaxAge.text"));
        txtRecordingMaxAge.setName("txtRecordingMaxAge");
        lblRecordingMaxAgeDays.setText(resourceMap.getString("lblRecordingMaxAgeDays.text"));
        lblRecordingMaxAgeDays.setName("lblRecordingMaxAgeDays");
        lblRecordingPct.setText(resourceMap.getString("lblRecordingPct.text"));
        lblRecordingPct.setName("lblRecordingPct");
        txtRecordingPct.setText(resourceMap.getString("txtRecordingPct.text"));
        txtRecordingPct.setName("txtRecordingPct");
        javax.swing.GroupLayout pnlRecordingLayout = new javax.swing.GroupLayout(pnlRecording);
        pnlRecording.setLayout(pnlRecordingLayout);
        pnlRecordingLayout.setHorizontalGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlRecordingLayout.createSequentialGroup().addContainerGap().addGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblRecordingPct).addComponent(lblRecordingPolicy).addComponent(lblRecordingMaxAge)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(cboRecordingPolicy, javax.swing.GroupLayout.Alignment.TRAILING, 0, 135, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlRecordingLayout.createSequentialGroup().addGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(txtRecordingMaxAge, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE).addComponent(txtRecordingPct, javax.swing.GroupLayout.DEFAULT_SIZE, 76, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(lblRecordingMaxAgeDays).addGap(32, 32, 32)))));
        pnlRecordingLayout.setVerticalGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlRecordingLayout.createSequentialGroup().addGap(6, 6, 6).addGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cboRecordingPolicy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblRecordingPolicy)).addGap(8, 8, 8).addGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblRecordingMaxAge).addComponent(txtRecordingMaxAge, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblRecordingMaxAgeDays)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlRecordingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblRecordingPct).addComponent(txtRecordingPct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pnlAgentCampaignDetails.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlAgentCampaignDetails.border.title")));
        pnlAgentCampaignDetails.setName("pnlAgentCampaignDetails");
        chkAgentCanUpdateContacts.setText(resourceMap.getString("chkAgentCanUpdateContacts.text"));
        chkAgentCanUpdateContacts.setName("chkAgentCanUpdateContacts");
        chkAgentCanReschedule.setText(resourceMap.getString("chkAgentCanReschedule.text"));
        chkAgentCanReschedule.setName("chkAgentCanReschedule");
        chkAgentCanRescheduleSelf.setText(resourceMap.getString("chkAgentCanRescheduleSelf.text"));
        chkAgentCanRescheduleSelf.setName("chkAgentCanRescheduleSelf");
        lblURL.setText(resourceMap.getString("lblURL.text"));
        lblURL.setName("lblURL");
        txtURL.setText(resourceMap.getString("txtURL.text"));
        txtURL.setName("txtURL");
        lblACW.setText(resourceMap.getString("lblACW.text"));
        lblACW.setName("lblACW");
        txtACW.setText(resourceMap.getString("txtACW.text"));
        txtACW.setName("txtACW");
        lblACWSeconds.setText(resourceMap.getString("lblACWSeconds.text"));
        lblACWSeconds.setName("lblACWSeconds");
        javax.swing.GroupLayout pnlAgentCampaignDetailsLayout = new javax.swing.GroupLayout(pnlAgentCampaignDetails);
        pnlAgentCampaignDetails.setLayout(pnlAgentCampaignDetailsLayout);
        pnlAgentCampaignDetailsLayout.setHorizontalGroup(pnlAgentCampaignDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlAgentCampaignDetailsLayout.createSequentialGroup().addContainerGap().addGroup(pnlAgentCampaignDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlAgentCampaignDetailsLayout.createSequentialGroup().addGap(21, 21, 21).addComponent(chkAgentCanRescheduleSelf)).addGroup(pnlAgentCampaignDetailsLayout.createSequentialGroup().addComponent(chkAgentCanUpdateContacts).addGap(17, 17, 17).addComponent(lblACW).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtACW, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(lblACWSeconds).addGap(139, 139, 139)).addComponent(chkAgentCanReschedule).addGroup(pnlAgentCampaignDetailsLayout.createSequentialGroup().addComponent(lblURL).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(txtURL, javax.swing.GroupLayout.DEFAULT_SIZE, 421, Short.MAX_VALUE))).addContainerGap()));
        pnlAgentCampaignDetailsLayout.setVerticalGroup(pnlAgentCampaignDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlAgentCampaignDetailsLayout.createSequentialGroup().addGroup(pnlAgentCampaignDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(chkAgentCanUpdateContacts).addComponent(lblACW).addComponent(lblACWSeconds).addComponent(txtACW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkAgentCanReschedule).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(chkAgentCanRescheduleSelf).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlAgentCampaignDetailsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblURL).addComponent(txtURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pnlScript.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlScript.border.title")));
        pnlScript.setName("pnlScript");
        scrollPanelScript.setName("scrollPanelScript");
        txtScript.setColumns(20);
        txtScript.setLineWrap(true);
        txtScript.setRows(5);
        txtScript.setWrapStyleWord(true);
        txtScript.setName("txtScript");
        scrollPanelScript.setViewportView(txtScript);
        javax.swing.GroupLayout pnlScriptLayout = new javax.swing.GroupLayout(pnlScript);
        pnlScript.setLayout(pnlScriptLayout);
        pnlScriptLayout.setHorizontalGroup(pnlScriptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollPanelScript, javax.swing.GroupLayout.DEFAULT_SIZE, 785, Short.MAX_VALUE));
        pnlScriptLayout.setVerticalGroup(pnlScriptLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(scrollPanelScript, javax.swing.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE));
        javax.swing.GroupLayout pnlGeneralLayout = new javax.swing.GroupLayout(pnlGeneral);
        pnlGeneral.setLayout(pnlGeneralLayout);
        pnlGeneralLayout.setHorizontalGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlGeneralLayout.createSequentialGroup().addContainerGap().addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(pnlScript, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(pnlGeneralLayout.createSequentialGroup().addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlGeneralLayout.createSequentialGroup().addComponent(pnlRecording, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pnlAgentCampaignDetails, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(pnlDates, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(pnlStatus1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(pnlStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))).addContainerGap()));
        pnlGeneralLayout.setVerticalGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlGeneralLayout.createSequentialGroup().addContainerGap().addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlGeneralLayout.createSequentialGroup().addComponent(pnlDates, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlGeneralLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pnlAgentCampaignDetails, 0, 121, Short.MAX_VALUE).addComponent(pnlRecording, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))).addGroup(pnlGeneralLayout.createSequentialGroup().addComponent(pnlStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pnlStatus1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pnlScript, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap()));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlGeneral.TabConstraints.tabTitle"), pnlGeneral);
        pnlDialing.setName("pnlDialing");
        pnlPBX.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlPBX.border.title")));
        pnlPBX.setName("pnlPBX");
        lblDialPrefix.setText(resourceMap.getString("lblDialPrefix.text"));
        lblDialPrefix.setName("lblDialPrefix");
        txtDialPrefix.setText(resourceMap.getString("txtDialPrefix.text"));
        txtDialPrefix.setName("txtDialPrefix");
        lblDialContext.setText(resourceMap.getString("lblDialContext.text"));
        lblDialContext.setName("lblDialContext");
        txtDialContext.setText(resourceMap.getString("txtDialContext.text"));
        txtDialContext.setName("txtDialContext");
        lblDialExtension.setText(resourceMap.getString("lblDialExtension.text"));
        lblDialExtension.setName("lblDialExtension");
        txtDialExtension.setText(resourceMap.getString("txtDialExtension.text"));
        txtDialExtension.setName("txtDialExtension");
        lblQueue.setText(resourceMap.getString("lblQueue.text"));
        lblQueue.setName("lblQueue");
        txtQueue.setText(resourceMap.getString("txtQueue.text"));
        txtQueue.setName("txtQueue");
        javax.swing.GroupLayout pnlPBXLayout = new javax.swing.GroupLayout(pnlPBX);
        pnlPBX.setLayout(pnlPBXLayout);
        pnlPBXLayout.setHorizontalGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlPBXLayout.createSequentialGroup().addContainerGap().addGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblDialPrefix).addComponent(lblDialContext).addComponent(lblDialExtension).addComponent(lblQueue)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(txtDialContext, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtDialExtension, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtQueue, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtDialPrefix, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        pnlPBXLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { txtDialContext, txtDialExtension, txtQueue });
        pnlPBXLayout.setVerticalGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlPBXLayout.createSequentialGroup().addGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDialPrefix).addComponent(txtDialPrefix, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDialContext).addComponent(txtDialContext, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDialExtension).addComponent(txtDialExtension, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(pnlPBXLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblQueue).addComponent(txtQueue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        pnlRetryPolicy.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlRetryPolicy.border.title")));
        pnlRetryPolicy.setName("pnlRetryPolicy");
        lblMaxRetries.setText(resourceMap.getString("lblMaxRetries.text"));
        lblMaxRetries.setName("lblMaxRetries");
        txtMaxRetries.setText(resourceMap.getString("txtMaxRetries.text"));
        txtMaxRetries.setName("txtMaxRetries");
        lblFirstRetry.setText(resourceMap.getString("lblFirstRetry.text"));
        lblFirstRetry.setName("lblFirstRetry");
        txtFirstRetry.setText(resourceMap.getString("txtFirstRetry.text"));
        txtFirstRetry.setName("txtFirstRetry");
        lblFirstRetryMinutes.setText(resourceMap.getString("lblFirstRetryMinutes.text"));
        lblFirstRetryMinutes.setName("lblFirstRetryMinutes");
        lblSecondRetry.setText(resourceMap.getString("lblSecondRetry.text"));
        lblSecondRetry.setName("lblSecondRetry");
        txtSecondRetry.setText(resourceMap.getString("txtSecondRetry.text"));
        txtSecondRetry.setName("txtSecondRetry");
        lblSecondRetryMinutes.setText(resourceMap.getString("lblSecondRetryMinutes.text"));
        lblSecondRetryMinutes.setName("lblSecondRetryMinutes");
        lblFurtherRetries.setText(resourceMap.getString("lblFurtherRetries.text"));
        lblFurtherRetries.setName("lblFurtherRetries");
        txtFurtherRetries.setText(resourceMap.getString("txtFurtherRetries.text"));
        txtFurtherRetries.setName("txtFurtherRetries");
        lblFurtherRetriesMinutes.setText(resourceMap.getString("lblFurtherRetriesMinutes.text"));
        lblFurtherRetriesMinutes.setName("lblFurtherRetriesMinutes");
        javax.swing.GroupLayout pnlRetryPolicyLayout = new javax.swing.GroupLayout(pnlRetryPolicy);
        pnlRetryPolicy.setLayout(pnlRetryPolicyLayout);
        pnlRetryPolicyLayout.setHorizontalGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlRetryPolicyLayout.createSequentialGroup().addContainerGap().addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblMaxRetries).addComponent(lblFirstRetry).addComponent(lblSecondRetry).addComponent(lblFurtherRetries)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(txtMaxRetries, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtFirstRetry, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtSecondRetry, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtFurtherRetries, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(lblFirstRetryMinutes).addComponent(lblSecondRetryMinutes)).addComponent(lblFurtherRetriesMinutes)).addContainerGap()));
        pnlRetryPolicyLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { txtFirstRetry, txtFurtherRetries, txtMaxRetries, txtSecondRetry });
        pnlRetryPolicyLayout.setVerticalGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlRetryPolicyLayout.createSequentialGroup().addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblMaxRetries).addComponent(txtMaxRetries, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblFirstRetry).addComponent(txtFirstRetry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFirstRetryMinutes)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblSecondRetry).addComponent(txtSecondRetry, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblSecondRetryMinutes)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(pnlRetryPolicyLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblFurtherRetries).addComponent(txtFurtherRetries, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblFurtherRetriesMinutes)).addContainerGap()));
        pnlCallHandling.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlCallHandling.border.title")));
        pnlCallHandling.setToolTipText(resourceMap.getString("pnlCallHandling.toolTipText"));
        pnlCallHandling.setName("pnlCallHandling");
        lblContext.setText(resourceMap.getString("lblContext.text"));
        lblContext.setName("lblContext");
        txtContext.setText(resourceMap.getString("txtContext.text"));
        txtContext.setName("txtContext");
        lblExtension.setText(resourceMap.getString("lblExtension.text"));
        lblExtension.setName("lblExtension");
        txtExtension.setText(resourceMap.getString("txtExtension.text"));
        txtExtension.setName("txtExtension");
        javax.swing.GroupLayout pnlCallHandlingLayout = new javax.swing.GroupLayout(pnlCallHandling);
        pnlCallHandling.setLayout(pnlCallHandlingLayout);
        pnlCallHandlingLayout.setHorizontalGroup(pnlCallHandlingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlCallHandlingLayout.createSequentialGroup().addContainerGap().addGroup(pnlCallHandlingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblContext).addComponent(lblExtension)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlCallHandlingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(txtContext, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtExtension, javax.swing.GroupLayout.PREFERRED_SIZE, 137, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
        pnlCallHandlingLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { txtContext, txtExtension });
        pnlCallHandlingLayout.setVerticalGroup(pnlCallHandlingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlCallHandlingLayout.createSequentialGroup().addGroup(pnlCallHandlingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblContext).addComponent(txtContext, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlCallHandlingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblExtension).addComponent(txtExtension, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        pnlDialer.setBorder(javax.swing.BorderFactory.createTitledBorder(resourceMap.getString("pnlDialer.border.title")));
        pnlDialer.setName("pnlDialer");
        lblDialingMethod.setText(resourceMap.getString("lblDialingMethod.text"));
        lblDialingMethod.setName("lblDialingMethod");
        cboDialingMethod.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Manual", "Preview", "Progressive", "Predictive" }));
        cboDialingMethod.setName("cboDialingMethod");
        lblContactBatchSize.setText(resourceMap.getString("lblContactBatchSize.text"));
        lblContactBatchSize.setName("lblContactBatchSize");
        txtContactBatchSize.setText(resourceMap.getString("txtContactBatchSize.text"));
        txtContactBatchSize.setName("txtContactBatchSize");
        lblRescheduleBatchPct.setText(resourceMap.getString("lblRescheduleBatchPct.text"));
        lblRescheduleBatchPct.setName("lblRescheduleBatchPct");
        txtRescheduleBatchPct.setText(resourceMap.getString("txtRescheduleBatchPct.text"));
        txtRescheduleBatchPct.setName("txtRescheduleBatchPct");
        lblAdjustRatio.setText(resourceMap.getString("lblAdjustRatio.text"));
        lblAdjustRatio.setToolTipText(resourceMap.getString("lblAdjustRatio.toolTipText"));
        lblAdjustRatio.setName("lblAdjustRatio");
        txtAdjustRatio.setText(resourceMap.getString("txtAdjustRatio.text"));
        txtAdjustRatio.setToolTipText(resourceMap.getString("txtAdjustRatio.toolTipText"));
        txtAdjustRatio.setName("txtAdjustRatio");
        lblReserveAgents.setText(resourceMap.getString("lblReserveAgents.text"));
        lblReserveAgents.setName("lblReserveAgents");
        txtReserveAgents.setText(resourceMap.getString("txtReserveAgents.text"));
        txtReserveAgents.setName("txtReserveAgents");
        lblDialLimit.setText(resourceMap.getString("lblDialLimit.text"));
        lblDialLimit.setToolTipText(resourceMap.getString("lblDialLimit.toolTipText"));
        lblDialLimit.setName("lblDialLimit");
        txtDialLimit.setText(resourceMap.getString("txtDialLimit.text"));
        txtDialLimit.setToolTipText(resourceMap.getString("txtDialLimit.toolTipText"));
        txtDialLimit.setName("txtDialLimit");
        lblInitialDialingRatio.setText(resourceMap.getString("lblInitialDialingRatio.text"));
        lblInitialDialingRatio.setName("lblInitialDialingRatio");
        txtInitialDialingRatio.setText(resourceMap.getString("txtInitialDialingRatio.text"));
        txtInitialDialingRatio.setToolTipText(resourceMap.getString("txtInitialDialingRatio.toolTipText"));
        txtInitialDialingRatio.setName("txtInitialDialingRatio");
        lblMinDialingRatio.setText(resourceMap.getString("lblMinDialingRatio.text"));
        lblMinDialingRatio.setName("lblMinDialingRatio");
        txtMinDialingRatio.setText(resourceMap.getString("txtMinDialingRatio.text"));
        txtMinDialingRatio.setName("txtMinDialingRatio");
        lblMaxDialingRatio.setText(resourceMap.getString("lblMaxDialingRatio.text"));
        lblMaxDialingRatio.setName("lblMaxDialingRatio");
        txtMaxDialingRatio.setText(resourceMap.getString("txtMaxDialingRatio.text"));
        txtMaxDialingRatio.setName("txtMaxDialingRatio");
        lblMaxIVRChannels.setText(resourceMap.getString("lblMaxIVRChannels.text"));
        lblMaxIVRChannels.setName("lblMaxIVRChannels");
        txtMaxIVRChannels.setText(resourceMap.getString("txtMaxIVRChannels.text"));
        txtMaxIVRChannels.setName("txtMaxIVRChannels");
        lblDNCListPreference.setText(resourceMap.getString("lblDNCListPreference.text"));
        lblDNCListPreference.setName("lblDNCListPreference");
        cboDNCListPreference.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Standard", "Custom", "Standard + Custom" }));
        cboDNCListPreference.setName("cboDNCListPreference");
        lblRetryBatchPct.setText(resourceMap.getString("lblRetryBatchPct.text"));
        lblRetryBatchPct.setName("lblRetryBatchPct");
        txtRetryBatchPct.setName("txtRetryBatchPct");
        javax.swing.GroupLayout pnlDialerLayout = new javax.swing.GroupLayout(pnlDialer);
        pnlDialer.setLayout(pnlDialerLayout);
        pnlDialerLayout.setHorizontalGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialerLayout.createSequentialGroup().addContainerGap().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialerLayout.createSequentialGroup().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblDialingMethod).addComponent(lblContactBatchSize).addComponent(lblRescheduleBatchPct)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(cboDialingMethod, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(txtContactBatchSize).addComponent(txtRescheduleBatchPct))).addGroup(pnlDialerLayout.createSequentialGroup().addComponent(lblDialLimit).addGap(165, 165, 165)).addGroup(pnlDialerLayout.createSequentialGroup().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblAdjustRatio).addComponent(lblReserveAgents).addComponent(lblRetryBatchPct)).addGap(20, 20, 20).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtRetryBatchPct, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE).addComponent(txtDialLimit, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE).addComponent(txtReserveAgents, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE).addComponent(txtAdjustRatio, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)))).addGap(18, 18, 18).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialerLayout.createSequentialGroup().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(lblMaxIVRChannels).addComponent(lblDNCListPreference).addComponent(lblInitialDialingRatio)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(txtMaxDialingRatio, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE).addComponent(txtMinDialingRatio, javax.swing.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE).addComponent(txtInitialDialingRatio, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(cboDNCListPreference, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(txtMaxIVRChannels, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))).addComponent(lblMaxDialingRatio).addComponent(lblMinDialingRatio)).addContainerGap()));
        pnlDialerLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { txtAdjustRatio, txtContactBatchSize, txtDialLimit, txtInitialDialingRatio, txtMaxDialingRatio, txtMaxIVRChannels, txtMinDialingRatio, txtRescheduleBatchPct, txtReserveAgents });
        pnlDialerLayout.setVerticalGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialerLayout.createSequentialGroup().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialerLayout.createSequentialGroup().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDialingMethod).addComponent(cboDialingMethod, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblContactBatchSize).addComponent(txtContactBatchSize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblRescheduleBatchPct).addComponent(txtRescheduleBatchPct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addGap(32, 32, 32).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblAdjustRatio).addComponent(txtAdjustRatio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblReserveAgents).addComponent(txtReserveAgents, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDialLimit).addComponent(txtDialLimit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(pnlDialerLayout.createSequentialGroup().addGap(78, 78, 78).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblRetryBatchPct).addComponent(txtRetryBatchPct, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(pnlDialerLayout.createSequentialGroup().addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblMaxIVRChannels).addComponent(txtMaxIVRChannels, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblDNCListPreference).addComponent(cboDNCListPreference, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addGroup(pnlDialerLayout.createSequentialGroup().addGap(52, 52, 52).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblInitialDialingRatio).addComponent(txtInitialDialingRatio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblMinDialingRatio).addComponent(txtMinDialingRatio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(pnlDialerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(lblMaxDialingRatio).addComponent(txtMaxDialingRatio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))).addContainerGap(68, Short.MAX_VALUE)));
        javax.swing.GroupLayout pnlDialingLayout = new javax.swing.GroupLayout(pnlDialing);
        pnlDialing.setLayout(pnlDialingLayout);
        pnlDialingLayout.setHorizontalGroup(pnlDialingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialingLayout.createSequentialGroup().addContainerGap().addGroup(pnlDialingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pnlDialer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGroup(pnlDialingLayout.createSequentialGroup().addComponent(pnlPBX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pnlRetryPolicy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(385, 385, 385).addComponent(pnlCallHandling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))));
        pnlDialingLayout.setVerticalGroup(pnlDialingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(pnlDialingLayout.createSequentialGroup().addContainerGap().addGroup(pnlDialingLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(pnlCallHandling, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(pnlRetryPolicy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(pnlPBX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(pnlDialer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addContainerGap()));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlDialing.TabConstraints.tabTitle"), pnlDialing);
        pnlCustomFields.setName("pnlCustomFields");
        javax.swing.GroupLayout pnlCustomFieldsLayout = new javax.swing.GroupLayout(pnlCustomFields);
        pnlCustomFields.setLayout(pnlCustomFieldsLayout);
        pnlCustomFieldsLayout.setHorizontalGroup(pnlCustomFieldsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 813, Short.MAX_VALUE));
        pnlCustomFieldsLayout.setVerticalGroup(pnlCustomFieldsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 441, Short.MAX_VALUE));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlCustomFields.TabConstraints.tabTitle"), pnlCustomFields);
        pnlCallDispositions.setName("pnlCallDispositions");
        javax.swing.GroupLayout pnlCallDispositionsLayout = new javax.swing.GroupLayout(pnlCallDispositions);
        pnlCallDispositions.setLayout(pnlCallDispositionsLayout);
        pnlCallDispositionsLayout.setHorizontalGroup(pnlCallDispositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 813, Short.MAX_VALUE));
        pnlCallDispositionsLayout.setVerticalGroup(pnlCallDispositionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 441, Short.MAX_VALUE));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlCallDispositions.TabConstraints.tabTitle"), pnlCallDispositions);
        pnlProducts.setName("pnlProducts");
        javax.swing.GroupLayout pnlProductsLayout = new javax.swing.GroupLayout(pnlProducts);
        pnlProducts.setLayout(pnlProductsLayout);
        pnlProductsLayout.setHorizontalGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 813, Short.MAX_VALUE));
        pnlProductsLayout.setVerticalGroup(pnlProductsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 441, Short.MAX_VALUE));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlProducts.TabConstraints.tabTitle"), pnlProducts);
        pnlUsers.setName("pnlUsers");
        javax.swing.GroupLayout pnlUsersLayout = new javax.swing.GroupLayout(pnlUsers);
        pnlUsers.setLayout(pnlUsersLayout);
        pnlUsersLayout.setHorizontalGroup(pnlUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 813, Short.MAX_VALUE));
        pnlUsersLayout.setVerticalGroup(pnlUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 441, Short.MAX_VALUE));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlUsers.TabConstraints.tabTitle"), pnlUsers);
        pnlContacts.setName("pnlContacts");
        javax.swing.GroupLayout pnlContactsLayout = new javax.swing.GroupLayout(pnlContacts);
        pnlContacts.setLayout(pnlContactsLayout);
        pnlContactsLayout.setHorizontalGroup(pnlContactsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 813, Short.MAX_VALUE));
        pnlContactsLayout.setVerticalGroup(pnlContactsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 441, Short.MAX_VALUE));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlContacts.TabConstraints.tabTitle"), pnlContacts);
        pnlCalls.setName("pnlCalls");
        javax.swing.GroupLayout pnlCallsLayout = new javax.swing.GroupLayout(pnlCalls);
        pnlCalls.setLayout(pnlCallsLayout);
        pnlCallsLayout.setHorizontalGroup(pnlCallsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 813, Short.MAX_VALUE));
        pnlCallsLayout.setVerticalGroup(pnlCallsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 441, Short.MAX_VALUE));
        pnlTabbedPanel.addTab(resourceMap.getString("pnlCalls.TabConstraints.tabTitle"), pnlCalls);
        btnSave.setText(resourceMap.getString("btnSave.text"));
        btnSave.setName("btnSave");
        btnSave.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        lblCampaigns.setFont(resourceMap.getFont("lblCampaigns.font"));
        lblCampaigns.setText(resourceMap.getString("lblCampaigns.text"));
        lblCampaigns.setName("lblCampaigns");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addGap(8, 8, 8).addComponent(lblCampaigns).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(cboCampaigns, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)).addComponent(btnSave).addComponent(pnlTabbedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 818, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(cboCampaigns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(lblCampaigns)).addGap(11, 11, 11).addComponent(pnlTabbedPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 469, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(btnSave).addGap(11, 11, 11)));
        pack();
    }

    /**
     * This method is called when the user changes the selection in cboCampaigns.
     * <br>
     * It causes the chosen campaign's information to be loaded.
     * 
     * @param evt
     */
    private void cboCampaignsActionPerformed(java.awt.event.ActionEvent evt) {
        String chosenCampaign = cboCampaigns.getSelectedItem().toString();
        if (!chosenCampaign.isEmpty() && !chosenCampaign.equals(campaign)) {
            if (campaign.isEmpty()) {
                if (validateData()) {
                    saveCampaign();
                }
            }
            setCampaign(chosenCampaign);
        }
    }

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {
        if (validateData()) {
            saveCampaign();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new CampaignAdmin().setVisible(true);
            }
        });
    }

    /**
     * Notify the daemon that the campaign has been updated.
     * 
     * @return
     * @throws java.io.IOException
     * @throws administrationpanel.socketservices.RequestErrorException
     * @throws administrationpanel.socketservices.RequestErrorException
     * @throws administrationpanel.socketservices.DaemonErrorException
     * @throws org.aacc.utils.comm.UnsupportedMessageException
     */
    private AACCResponse doNotifyCampaignUpdated() throws IOException, RequestErrorException, RequestErrorException, DaemonErrorException, UnsupportedMessageException {
        UpdateCampaignMessage message = new UpdateCampaignMessage();
        message.setCampaign(cboCampaigns.getSelectedItem().toString());
        AACCResponse response = AdministrationPanelView.getDaemonSocket().send(message);
        return response;
    }

    /**
     * Get the status of the campaign from the daemon. For example, find out if
     * the campaign is running or not.
     * @return
     * @throws java.io.IOException
     * @throws administrationpanel.socketservices.RequestErrorException
     * @throws administrationpanel.socketservices.RequestErrorException
     * @throws administrationpanel.socketservices.DaemonErrorException
     * @throws org.aacc.utils.comm.UnsupportedMessageException
     */
    private AACCResponse doGetCampaignStatus() throws IOException, RequestErrorException, RequestErrorException, DaemonErrorException, UnsupportedMessageException {
        CampaignNotifyMessage message = new CampaignNotifyMessage();
        message.setCampaign(cboCampaigns.getSelectedItem().toString());
        message.setStatus(CampaignNotifyMessage.NOTIFY_UNKNOWN);
        AACCResponse response = AdministrationPanelView.getDaemonSocket().send(message);
        return response;
    }

    /**
     * Tells the daemon to change the status of the campaign. For instance, stop or run,
     * activate or inactivate.
     * @param status A valid state from CampaignNotifyMessage.NOTIFY_xxxx 
     * @return
     * @throws java.io.IOException
     * @throws administrationpanel.socketservices.RequestErrorException
     * @throws administrationpanel.socketservices.RequestErrorException
     * @throws administrationpanel.socketservices.DaemonErrorException
     * @throws org.aacc.utils.comm.UnsupportedMessageException
     */
    private AACCResponse doSetCampaignStatus(byte status) throws IOException, RequestErrorException, RequestErrorException, DaemonErrorException, UnsupportedMessageException {
        CampaignNotifyMessage message = new CampaignNotifyMessage();
        message.setCampaign(cboCampaigns.getSelectedItem().toString());
        message.setStatus(status);
        AACCResponse response = AdministrationPanelView.getDaemonSocket().send(message);
        return response;
    }

    /**
     * Gets campaign information from the database and fills the fields with the
     * data it has obtained.
     */
    private void loadCampaignData() {
        String query = "SELECT * FROM campaigns WHERE name = ?";
        try {
            PreparedStatement statement = dbConnection.prepareStatement(query);
            statement.setString(1, campaign);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
            } else {
                JOptionPane.showMessageDialog(this.getRootPane(), "Sorry, I have no record of a campaign named " + campaign, "Error", JOptionPane.ERROR_MESSAGE);
                dispose();
            }
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this.getRootPane(), ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    /**
     * Load campaigns combo
     */
    private void loadCampaigns() {
        try {
            String sql = "";
            if (me.getIsSuperUser()) {
                sql = "SELECT id, name FROM campaigns ORDER BY name";
            } else {
                sql = "SELECT campaigns.id, campaigns.name, usercampaigns.role FROM campaigns, usercampaigns " + "WHERE campaigns.id = usercampaigns.campaignid " + "AND usercampaigns.userid = " + me.getId() + "AND ((campaigns.active = 1 " + "AND usercampaigns.role IN ('leader', 'supervisor','owner')) " + "OR usercampaigns.role = 'admin') " + "ORDER BY name";
            }
            Statement statement = dbConnection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            DefaultComboBoxModel model = new DefaultComboBoxModel();
            model.addElement("");
            while (rs.next()) {
                model.addElement(rs.getString("name"));
            }
            cboCampaigns.setModel(model);
        } catch (SQLException ex) {
            Logger.getLogger(CampaignAdmin.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this.getRootPane(), ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public String getCampaign() {
        return campaign;
    }

    /**
     * Sets current. Triggers loading campaign data into the screen.
     * 
     * @param campaign
     */
    public void setCampaign(String campaign) {
        this.campaign = campaign;
        loadCampaignData();
    }

    /**
     * Save campaign data in database.
     * 
     * @throws java.awt.HeadlessException
     */
    private void saveCampaign() throws HeadlessException {
        try {
            dbConnection.setAutoCommit(false);
            dbConnection.setSavepoint();
            String sql = "UPDATE campaigns SET " + "queue = ? ," + "adjustRatioPeriod = ?, " + "asterisk = ?, " + "context = ?," + "extension = ?, " + "dialContext = ?, " + "dialPrefix = ?," + "dialTimeout = ?, " + "dialingMethod = ?," + "dialsPerFreeResourceRatio = ?, " + "maxIVRChannels = ?, " + "maxDialingThreads = ?," + "maxDialsPerFreeResourceRatio = ?," + "minDialsPerFreeResourceRatio = ?, " + "maxTries = ?, " + "firstRetryAfterMinutes = ?," + "secondRetryAfterMinutes = ?, " + "furtherRetryAfterMinutes = ?, " + "startDate = ?, " + "endDate = ?," + "popUpURL = ?, " + "contactBatchSize = ?, " + "retriesBatchPct = ?, " + "reschedulesBatchPct = ?, " + "allowReschedule = ?, " + "rescheduleToOnself = ?, " + "script = ?," + "agentsCanUpdateContacts = ?, " + "hideContactFields = ?, " + "afterCallWork = ?, " + "reserveAvailableAgents = ?, " + "useDNCList = ?, " + "enableAgentDNC = ?, " + "contactsFilter = ?, " + "DNCTo = ?," + "callRecordingPolicy = ?, " + "callRecordingPercent = ?, " + "callRecordingMaxAge = ?, " + "WHERE name = ?";
            PreparedStatement statement = dbConnection.prepareStatement(sql);
            int i = 1;
            statement.setString(i++, txtQueue.getText());
            statement.setInt(i++, Integer.valueOf(txtAdjustRatio.getText()));
            statement.setString(i++, "");
            statement.setString(i++, txtContext.getText());
            statement.setString(i++, txtExtension.getText());
            statement.setString(i++, txtDialContext.getText());
            statement.setString(i++, txtDialPrefix.getText());
            statement.setInt(i++, 30000);
            statement.setInt(i++, cboDialingMethod.getSelectedIndex());
            statement.setFloat(i++, Float.valueOf(txtInitialDialingRatio.getText()));
            statement.setInt(i++, Integer.valueOf(txtMaxIVRChannels.getText()));
            statement.setInt(i++, Integer.valueOf(txtDialLimit.getText()));
            statement.setFloat(i++, Float.valueOf(txtMaxDialingRatio.getText()));
            statement.setFloat(i++, Float.valueOf(txtMinDialingRatio.getText()));
            statement.setInt(i++, Integer.valueOf(txtMaxRetries.getText()));
            statement.setInt(i++, Integer.valueOf(txtFirstRetry.getText()));
            statement.setInt(i++, Integer.valueOf(txtSecondRetry.getText()));
            statement.setInt(i++, Integer.valueOf(txtFurtherRetries.getText()));
            statement.setDate(i++, Date.valueOf(txtStartDate.getText()));
            statement.setDate(i++, Date.valueOf(txtEndDate.getText()));
            statement.setString(i++, txtURL.getText());
            statement.setInt(i++, Integer.valueOf(txtContactBatchSize.getText()));
            statement.setInt(i++, Integer.valueOf(txtRetryBatchPct.getText()));
            statement.setInt(i++, Integer.valueOf(txtRescheduleBatchPct.getText()));
            statement.setInt(i++, chkAgentCanReschedule.isSelected() ? 1 : 0);
            statement.setInt(i++, chkAgentCanRescheduleSelf.isSelected() ? 1 : 0);
            statement.setString(i++, txtScript.getText());
            statement.setInt(i++, chkAgentCanUpdateContacts.isSelected() ? 1 : 0);
            statement.setString(i++, "");
            statement.setInt(i++, Integer.valueOf(txtACW.getText()));
            statement.setInt(i++, Integer.valueOf(txtReserveAgents.getText()));
            statement.setInt(i++, cboDNCListPreference.getSelectedIndex());
            statement.setInt(i++, 1);
            statement.setString(i++, "");
            statement.setInt(i++, 0);
            statement.setInt(i++, cboRecordingPolicy.getSelectedIndex());
            statement.setInt(i++, Integer.valueOf(txtRecordingPct.getText()));
            statement.setInt(i++, Integer.valueOf(txtRecordingMaxAge.getText()));
            statement.setString(i++, campaign);
            statement.executeUpdate();
            dbConnection.commit();
        } catch (SQLException ex) {
            try {
                dbConnection.rollback();
            } catch (SQLException ex1) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex1);
            }
            JOptionPane.showMessageDialog(this.getRootPane(), ex.getLocalizedMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Validates data entered by the user
     * @return True if all data is valid
     */
    private boolean validateData() {
        return true;
    }

    private javax.swing.JRadioButton btnAgent;

    private javax.swing.JRadioButton btnIVR;

    private javax.swing.JButton btnSave;

    private javax.swing.JComboBox cboCampaigns;

    private javax.swing.JComboBox cboDNCListPreference;

    private javax.swing.JComboBox cboDialingMethod;

    private javax.swing.JComboBox cboRecordingPolicy;

    private javax.swing.JCheckBox chkActive;

    private javax.swing.JCheckBox chkAgentCanReschedule;

    private javax.swing.JCheckBox chkAgentCanRescheduleSelf;

    private javax.swing.JCheckBox chkAgentCanUpdateContacts;

    private javax.swing.JCheckBox chkRunning;

    private javax.swing.JLabel lblACW;

    private javax.swing.JLabel lblACWSeconds;

    private javax.swing.JLabel lblAdjustRatio;

    private javax.swing.JLabel lblCampaigns;

    private javax.swing.JLabel lblContactBatchSize;

    private javax.swing.JLabel lblContext;

    private javax.swing.JLabel lblDNCListPreference;

    private javax.swing.JLabel lblDialContext;

    private javax.swing.JLabel lblDialExtension;

    private javax.swing.JLabel lblDialLimit;

    private javax.swing.JLabel lblDialPrefix;

    private javax.swing.JLabel lblDialingMethod;

    private javax.swing.JLabel lblEndDate;

    private javax.swing.JLabel lblExtension;

    private javax.swing.JLabel lblFirstRetry;

    private javax.swing.JLabel lblFirstRetryMinutes;

    private javax.swing.JLabel lblFurtherRetries;

    private javax.swing.JLabel lblFurtherRetriesMinutes;

    private javax.swing.JLabel lblInitialDialingRatio;

    private javax.swing.JLabel lblMaxDialingRatio;

    private javax.swing.JLabel lblMaxIVRChannels;

    private javax.swing.JLabel lblMaxRetries;

    private javax.swing.JLabel lblMinDialingRatio;

    private javax.swing.JLabel lblQueue;

    private javax.swing.JLabel lblRecordingMaxAge;

    private javax.swing.JLabel lblRecordingMaxAgeDays;

    private javax.swing.JLabel lblRecordingPct;

    private javax.swing.JLabel lblRecordingPolicy;

    private javax.swing.JLabel lblRescheduleBatchPct;

    private javax.swing.JLabel lblReserveAgents;

    private javax.swing.JLabel lblRetryBatchPct;

    private javax.swing.JLabel lblSecondRetry;

    private javax.swing.JLabel lblSecondRetryMinutes;

    private javax.swing.JLabel lblStartDate;

    private javax.swing.JLabel lblURL;

    private javax.swing.JPanel pnlAgentCampaignDetails;

    private javax.swing.JPanel pnlCallDispositions;

    private javax.swing.JPanel pnlCallHandling;

    private javax.swing.JPanel pnlCalls;

    private javax.swing.JPanel pnlContacts;

    private javax.swing.JPanel pnlCustomFields;

    private javax.swing.JPanel pnlDates;

    private javax.swing.JPanel pnlDialer;

    private javax.swing.JPanel pnlDialing;

    private javax.swing.JPanel pnlGeneral;

    private javax.swing.JPanel pnlPBX;

    private javax.swing.JPanel pnlProducts;

    private javax.swing.JPanel pnlRecording;

    private javax.swing.JPanel pnlRetryPolicy;

    private javax.swing.JPanel pnlScript;

    private javax.swing.JPanel pnlStatus;

    private javax.swing.JPanel pnlStatus1;

    private javax.swing.JTabbedPane pnlTabbedPanel;

    private javax.swing.JPanel pnlUsers;

    private javax.swing.JScrollPane scrollPanelScript;

    private javax.swing.JTextField txtACW;

    private javax.swing.JTextField txtAdjustRatio;

    private javax.swing.JTextField txtContactBatchSize;

    private javax.swing.JTextField txtContext;

    private javax.swing.JTextField txtDialContext;

    private javax.swing.JTextField txtDialExtension;

    private javax.swing.JTextField txtDialLimit;

    private javax.swing.JTextField txtDialPrefix;

    private javax.swing.JTextField txtEndDate;

    private javax.swing.JTextField txtExtension;

    private javax.swing.JTextField txtFirstRetry;

    private javax.swing.JTextField txtFurtherRetries;

    private javax.swing.JTextField txtInitialDialingRatio;

    private javax.swing.JTextField txtMaxDialingRatio;

    private javax.swing.JTextField txtMaxIVRChannels;

    private javax.swing.JTextField txtMaxRetries;

    private javax.swing.JTextField txtMinDialingRatio;

    private javax.swing.JTextField txtQueue;

    private javax.swing.JTextField txtRecordingMaxAge;

    private javax.swing.JTextField txtRecordingPct;

    private javax.swing.JTextField txtRescheduleBatchPct;

    private javax.swing.JTextField txtReserveAgents;

    private javax.swing.JTextField txtRetryBatchPct;

    private javax.swing.JTextArea txtScript;

    private javax.swing.JTextField txtSecondRetry;

    private javax.swing.JTextField txtStartDate;

    private javax.swing.JTextField txtURL;
}
