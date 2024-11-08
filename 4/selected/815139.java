package mocaptracker;

import mocaptracking.TrackingData;
import mocapvis.ProgressStatus;
import mocapcommon.utils.LinAlg;
import mocapcommon.trackdata.TrackedBody;
import mocapcommon.trackdata.DataFrame;
import mocapoptions.guiinterfaces.*;
import mocapvis.MainGL;
import mocapmodel.eval.OccludedTrackingEvalRect;
import mocapmodel.eval.OccludedTrackingEvalGUI;
import mocapmodel.eval.OccludedTrackingEvalCB;
import mocapmodel.InitialModelNode;
import mocapmodel.InitialModelBase;
import mocaptracking.WriteBodyNameDBThread;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

/**
 *
 * @author a.heinrich
 */
public class NeuralNetOptionsGUI extends javax.swing.JPanel implements NeuralNetOptionsInterface {

    protected MainGUI mainGUI;

    /** include TrackingData class with curTrackData, bodyModel, bodyTracking, refBodyModel, refBodyTracking*/
    protected TrackingData trackingData;

    /** gui panel for showing the status */
    protected StatusControlGUI statusControlGUI;

    /** hashtable of body database id to body */
    protected HashMap<Integer, TrackedBody> bodyIdMap;

    /** initialize gui interfaces */
    protected DatabaseInterface database;

    protected FramesInterface framesInt;

    protected OptionsInterface optionsInt;

    /** visualization */
    protected MainGL gl = null;

    /** evaluation gui */
    protected OccludedTrackingEvalGUI evalGUI;

    protected OccludedTrackingEvalCB cb;

    /** random occlusion areas */
    protected Vector<OccludedTrackingEvalRect> occlAreas;

    /** flag to stop animation */
    protected boolean stopAnimation;

    /** handling of progress bar and status label */
    protected ProgressStatus progrStat;

    /** Creates new customizer NeurelNetOptionsGUI */
    public NeuralNetOptionsGUI(MainGUI mainGUI, MainGL gl, StatusControlGUI statusControlGUI, DatabaseInterface database, FramesInterface framesInt, OptionsInterface optionsInt, OccludedTrackingEvalCB cb) {
        initComponents();
        this.mainGUI = mainGUI;
        this.gl = gl;
        trackingData = TrackingData.getInstance();
        this.statusControlGUI = statusControlGUI;
        this.database = database;
        this.framesInt = framesInt;
        this.optionsInt = optionsInt;
        this.cb = cb;
        bodyIdMap = new HashMap<Integer, TrackedBody>();
        occlAreas = new Vector<OccludedTrackingEvalRect>();
        evalGUI = new OccludedTrackingEvalGUI(mainGUI, false, cb);
        evalGUI.setVisible(false);
        gl.getEventListener().setEvalGui(evalGUI);
        gl.getEventListener().setOcclAreas(occlAreas);
        progrStat = new ProgressStatus(mainGUI.progressLabel, mainGUI.progressBar, statusControlGUI);
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        sigmaTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        epsTextField = new javax.swing.JTextField();
        wtaNetBox = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        winnerMethodComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        lprTextField = new javax.swing.JTextField();
        inflDistPresCheckBox = new javax.swing.JCheckBox();
        idpTextField = new javax.swing.JTextField();
        inflAnglesResetCheckBox = new javax.swing.JCheckBox();
        resJointsRatio = new javax.swing.JTextField();
        resetPosMarkerPosAfterCheckBox = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        minDistTextField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        maxStepsTextField = new javax.swing.JTextField();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel10 = new javax.swing.JLabel();
        trackIDsButton = new javax.swing.JButton();
        asInitialButton = new javax.swing.JButton();
        resetToInitialButton = new javax.swing.JButton();
        reverseAnimBox = new javax.swing.JCheckBox();
        writeDBCheckBox = new javax.swing.JCheckBox();
        animateButton = new javax.swing.JButton();
        loopButton = new javax.swing.JButton();
        fbLoopButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel11 = new javax.swing.JLabel();
        evalButton = new javax.swing.JButton();
        gridSearchButton = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        jLabel12 = new javax.swing.JLabel();
        setLabelledButton = new javax.swing.JButton();
        setUnlabelledButton = new javax.swing.JButton();
        resetLabelledButton = new javax.swing.JButton();
        jLabel1.setText("Neural Net Control");
        jLabel2.setText("Parameters:");
        jLabel3.setText("Sigma:");
        sigmaTextField.setText("0.008");
        jLabel4.setText("Epsilon:");
        epsTextField.setText("0.015");
        wtaNetBox.setSelected(true);
        wtaNetBox.setText("WTA-Net");
        jLabel5.setText("Method:");
        winnerMethodComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Find winner neuron", "Find marker neuron", "Combined", "Hung. Algorithm" }));
        jLabel6.setText("LPR:");
        lprTextField.setText("0.02");
        inflDistPresCheckBox.setSelected(true);
        inflDistPresCheckBox.setText("Influence of Dist. Pres.");
        idpTextField.setText("0.05");
        inflAnglesResetCheckBox.setSelected(true);
        inflAnglesResetCheckBox.setText("Influence of Angles Reset:");
        resJointsRatio.setText("0.002");
        resetPosMarkerPosAfterCheckBox.setText("Set Positions to Identified the Markers afterwrads");
        jLabel7.setText("Abort citeria:");
        jLabel8.setText("Min. Distance");
        minDistTextField.setText("25");
        jLabel9.setText("Max. Steps:");
        maxStepsTextField.setText("500");
        jLabel10.setText("Tracking");
        trackIDsButton.setText("Track IDs for surrent Frame");
        trackIDsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                trackIDsButtonActionPerformed(evt);
            }
        });
        asInitialButton.setText("Use current SOM as Initial Model");
        asInitialButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                asInitialButtonActionPerformed(evt);
            }
        });
        resetToInitialButton.setText("Reset to Initial");
        resetToInitialButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetToInitialButtonActionPerformed(evt);
            }
        });
        reverseAnimBox.setText("Reverse");
        writeDBCheckBox.setText("Write-DB");
        animateButton.setText("Animate");
        animateButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                animateButtonActionPerformed(evt);
            }
        });
        loopButton.setText("Loop");
        loopButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                animateButtonActionPerformed(evt);
            }
        });
        fbLoopButton.setText("Forth-Back-loop");
        fbLoopButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                animateButtonActionPerformed(evt);
            }
        });
        stopButton.setText("Stop Animation/Loop");
        stopButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        jLabel11.setText("Evaluation:");
        evalButton.setText("Evaluate ...");
        evalButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                evalButtonActionPerformed(evt);
            }
        });
        gridSearchButton.setText("Grid Search");
        gridSearchButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridSearchButtonActionPerformed(evt);
            }
        });
        jLabel12.setText("Set series: All frames are ...");
        setLabelledButton.setText("Set Labelled");
        setLabelledButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setLabelledButtonActionPerformed(evt);
            }
        });
        setUnlabelledButton.setText("Set Unlabelled");
        setUnlabelledButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setUnlabelledButtonActionPerformed(evt);
            }
        });
        resetLabelledButton.setText("Reset Labelled");
        resetLabelledButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetLabelledButtonActionPerformed(evt);
            }
        });
        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().addContainerGap().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(jSeparator3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE).add(layout.createSequentialGroup().add(reverseAnimBox).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(writeDBCheckBox)).add(resetPosMarkerPosAfterCheckBox).add(inflAnglesResetCheckBox).add(inflDistPresCheckBox).add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE).add(jLabel1).add(jLabel2).add(layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(jLabel6).add(18, 18, 18).add(lprTextField)).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(jLabel3).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(sigmaTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 87, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(org.jdesktop.layout.GroupLayout.LEADING, wtaNetBox)).add(28, 28, 28).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(jLabel4).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(epsTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 115, Short.MAX_VALUE)).add(layout.createSequentialGroup().add(jLabel5).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(winnerMethodComboBox, 0, 118, Short.MAX_VALUE)).add(layout.createSequentialGroup().add(18, 18, 18).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(resJointsRatio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 55, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(idpTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE))))).add(jLabel7).add(layout.createSequentialGroup().add(jLabel8).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(minDistTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 37, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(18, 18, 18).add(jLabel9).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(maxStepsTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).add(jSeparator2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE).add(jLabel10).add(jLabel11).add(layout.createSequentialGroup().add(evalButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(gridSearchButton)).add(jSeparator4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 319, Short.MAX_VALUE).add(jLabel12).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, setLabelledButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, setUnlabelledButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).add(resetLabelledButton).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, stopButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(animateButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(loopButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 70, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(fbLoopButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup().add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false).add(org.jdesktop.layout.GroupLayout.LEADING, trackIDsButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).add(org.jdesktop.layout.GroupLayout.LEADING, asInitialButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(resetToInitialButton)))).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING).add(layout.createSequentialGroup().add(jLabel1).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(18, 18, 18).add(jLabel2).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel3).add(sigmaTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jLabel4).add(epsTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(wtaNetBox).add(jLabel5).add(winnerMethodComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel6).add(lprTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(inflDistPresCheckBox).add(idpTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(inflAnglesResetCheckBox).add(resJointsRatio, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(resetPosMarkerPosAfterCheckBox).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(jLabel7).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(jLabel8).add(minDistTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(jLabel9).add(maxStepsTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(jLabel10).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(trackIDsButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(asInitialButton).add(resetToInitialButton)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(reverseAnimBox).add(writeDBCheckBox)).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(animateButton).add(loopButton).add(fbLoopButton)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(stopButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).add(18, 18, 18).add(jLabel11).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE).add(evalButton).add(gridSearchButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 23, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE).addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED).add(jLabel12).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(setLabelledButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(setUnlabelledButton).addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED).add(resetLabelledButton).addContainerGap(109, Short.MAX_VALUE)));
    }

    private void trackIDsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (trackingData.getCurTrackData() != null) {
            String refBodyString = optionsInt.getReferenceBody();
            TrackedBody refBody = null;
            if (refBodyString != null && !refBodyString.isEmpty() && !refBodyString.equals("none") && optionsInt.isUseForModelGen()) {
                int bodyId = Integer.parseInt(refBodyString);
                refBody = bodyIdMap.get(bodyId);
            }
            if (trackingData.getBodyTracking() != null) {
                trackingData.getBodyTracking().fitModel(trackingData.getCurTrackData(), Double.parseDouble(sigmaTextField.getText()), Double.parseDouble(epsTextField.getText()), Double.valueOf(lprTextField.getText()), Double.valueOf(idpTextField.getText()), wtaNetBox.isSelected(), Double.parseDouble(minDistTextField.getText()), Integer.parseInt(maxStepsTextField.getText()), true, Double.parseDouble(resJointsRatio.getText()), inflDistPresCheckBox.isSelected(), inflAnglesResetCheckBox.isSelected(), refBody, optionsInt.isNotUseOther6D(), resetPosMarkerPosAfterCheckBox.isSelected(), winnerMethodComboBox.getSelectedIndex());
            }
            if (trackingData.getRefBodyTracking() != null && evalGUI.isEvalActive()) {
                trackingData.getRefBodyTracking().fitModel(trackingData.getCurTrackData(), Double.parseDouble(sigmaTextField.getText()), Double.parseDouble(epsTextField.getText()), Double.valueOf(lprTextField.getText()), Double.valueOf(idpTextField.getText()), wtaNetBox.isSelected(), Double.parseDouble(minDistTextField.getText()), Integer.parseInt(maxStepsTextField.getText()), false, Double.parseDouble(resJointsRatio.getText()), inflDistPresCheckBox.isSelected(), inflAnglesResetCheckBox.isSelected(), refBody, optionsInt.isNotUseOther6D(), resetPosMarkerPosAfterCheckBox.isSelected(), winnerMethodComboBox.getSelectedIndex());
            }
        }
    }

    @Override
    public final boolean getReverseAnim() {
        return reverseAnimBox.isSelected();
    }

    private void asInitialButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (trackingData.getBodyModel() != null) {
            trackingData.getBodyModel().fitToScaled();
        }
        if (trackingData.getRefBodyModel() != null) {
            trackingData.getRefBodyModel().fitToScaled();
        }
    }

    private void resetToInitialButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (trackingData.getBodyModel() != null) {
            trackingData.getBodyModel().createFittedFromScaled();
        }
        if (trackingData.getRefBodyModel() != null) {
            trackingData.getRefBodyModel().createFittedFromScaled();
        }
    }

    /** set current track data */
    @Override
    public final void setCurTrackData(DataFrame data) {
        trackingData.setCurTrackData(data);
        mctplugin.Factory.getInstance().setProperty("curtrackdata", trackingData.getCurTrackData());
    }

    private void animateButtonActionPerformed(java.awt.event.ActionEvent evt) {
        stopAnimation = false;
        final java.awt.event.ActionEvent ev = evt;
        Thread runner = new Thread() {

            @Override
            public void run() {
                boolean doLoop = false;
                boolean forthBack = false;
                boolean forth = true;
                if (ev.getSource() == fbLoopButton || ev.getSource() == loopButton) {
                    doLoop = true;
                    if (ev.getSource() == fbLoopButton) {
                        forthBack = true;
                    }
                }
                String selectedStr = "";
                int[] selectedInd = framesInt.getSelectedIndices();
                if (selectedInd.length == 0) {
                    javax.swing.JOptionPane.showMessageDialog(mainGUI, "You should select some frames.", "No frames selected", javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }
                for (int i = 0; i < selectedInd.length; ++i) {
                    selectedStr += framesInt.getDataIDForFrame(selectedInd[i]);
                    if (i < selectedInd.length - 1) {
                        selectedStr += ",";
                    }
                }
                try {
                    String selectStat = "select data.*,bodies.* from data,bodies where " + "data.id in (" + selectedStr + ") and data.id=bodies.dataid";
                    if (reverseAnimBox.isSelected()) {
                        selectStat += " order by data.id desc";
                    }
                    Statement mysqlStat = database.getMysqlSource().getConnection().createStatement();
                    ResultSet mysqlRes = mysqlStat.executeQuery(selectStat);
                    if (mysqlRes == null) {
                        javax.swing.JOptionPane.showMessageDialog(mainGUI, "animateButtonActionPerformed: No body data in database for frame " + framesInt.getCountForFrame(framesInt.getLeadSelectionIndex()) + ".", "No data", javax.swing.JOptionPane.ERROR_MESSAGE);
                    } else {
                        database.getMysqlSource().getConnection().setAutoCommit(false);
                        mysqlRes.last();
                        int rowCount = mysqlRes.getRow();
                        mysqlRes.beforeFirst();
                        progrStat.setMinMaxWorkingVis(0, rowCount);
                        WriteBodyNameDBThread writerBodyName = new WriteBodyNameDBThread(database.getMysqlSource().getConnection(), mainGUI);
                        Thread writerBodyNameThread = new Thread(writerBodyName);
                        writerBodyNameThread.start();
                        FileWriter evalWriter = null;
                        String evalFileName = evalGUI.getFileName();
                        if (!evalFileName.isEmpty()) {
                            try {
                                evalWriter = new FileWriter(evalFileName);
                            } catch (IOException ex) {
                                javax.swing.JOptionPane.showMessageDialog(mainGUI, "animateButtonActionPerformed: Wasn't able to open file " + evalFileName + ".", "Open file error", javax.swing.JOptionPane.ERROR_MESSAGE);
                                evalWriter = null;
                            }
                        }
                        DataFrame data = null;
                        int count = -1, oldFrameCounter = -1, frameCounter, dataid = -1;
                        while (!stopAnimation) {
                            if ((forth && mysqlRes.first()) || (!forth && mysqlRes.last())) {
                                do {
                                    ++count;
                                    progrStat.setProgress(count);
                                    dataid = mysqlRes.getInt("data.id");
                                    frameCounter = mysqlRes.getInt("data.framecounter");
                                    if (oldFrameCounter != frameCounter) {
                                        if (data != null) {
                                            String refBodyString = optionsInt.getReferenceBody();
                                            int refBodyId = -1;
                                            TrackedBody refBody = null;
                                            if (refBodyString != null && !refBodyString.isEmpty() && !refBodyString.equals("none") && optionsInt.isUseForModelGen()) {
                                                refBodyId = Integer.parseInt(refBodyString);
                                                refBody = bodyIdMap.get(refBodyId);
                                            }
                                            if (evalGUI.isEvalActive()) {
                                                trackingData.getRefBodyTracking().fitModel(data, Double.parseDouble(sigmaTextField.getText()), Double.parseDouble(epsTextField.getText()), Double.valueOf(lprTextField.getText()), Double.valueOf(idpTextField.getText()), wtaNetBox.isSelected(), Double.parseDouble(minDistTextField.getText()), Integer.parseInt(maxStepsTextField.getText()), false, Double.parseDouble(resJointsRatio.getText()), inflDistPresCheckBox.isSelected(), inflAnglesResetCheckBox.isSelected(), refBody, optionsInt.isNotUseOther6D(), resetPosMarkerPosAfterCheckBox.isSelected(), winnerMethodComboBox.getSelectedIndex());
                                            }
                                            trackingData.getBodyTracking().fitModel(data, Double.parseDouble(sigmaTextField.getText()), Double.parseDouble(epsTextField.getText()), Double.valueOf(lprTextField.getText()), Double.valueOf(idpTextField.getText()), wtaNetBox.isSelected(), Double.parseDouble(minDistTextField.getText()), Integer.parseInt(maxStepsTextField.getText()), true, Double.parseDouble(resJointsRatio.getText()), inflDistPresCheckBox.isSelected(), inflAnglesResetCheckBox.isSelected(), refBody, optionsInt.isNotUseOther6D(), resetPosMarkerPosAfterCheckBox.isSelected(), winnerMethodComboBox.getSelectedIndex());
                                            if (evalGUI.isEvalActive()) {
                                                InitialModelBase.Eval eval = trackingData.getRefBodyModel().doEvaluation(trackingData.getBodyModel());
                                                int numNoNames = 0, numDoubleNames = 0;
                                                for (Iterator<TrackedBody> evalBodyIt = data.bodies.iterator(); evalBodyIt.hasNext(); ) {
                                                    TrackedBody bd = evalBodyIt.next();
                                                    if (bd.name.isEmpty()) {
                                                        ++numNoNames;
                                                    } else {
                                                        for (Iterator<TrackedBody> otherBodyIt = data.bodies.iterator(); otherBodyIt.hasNext(); ) {
                                                            TrackedBody obd = otherBodyIt.next();
                                                            if (bd != obd && obd.name.equals(bd.name)) {
                                                                ++numDoubleNames;
                                                            }
                                                        }
                                                    }
                                                }
                                                double noNames = (double) numNoNames / (double) data.bodies.size();
                                                double doubleNames = (double) numDoubleNames / (double) data.bodies.size();
                                                if (evalWriter != null) {
                                                    try {
                                                        evalWriter.write(data.internalTimeStamp + " " + eval.minError + " " + eval.avrgError + " " + eval.maxError + " " + eval.errorDeviation + " " + noNames + " " + doubleNames + "\n");
                                                    } catch (IOException ex) {
                                                        javax.swing.JOptionPane.showMessageDialog(mainGUI, "animateButtonActionPerformed: Wasn't able to write to evaluation file.", "Write error", javax.swing.JOptionPane.ERROR_MESSAGE);
                                                        evalWriter = null;
                                                    }
                                                } else {
                                                    System.out.println(data.internalTimeStamp + " " + eval.minError + " " + eval.avrgError + " " + eval.maxError + " " + eval.errorDeviation + " " + noNames + " " + doubleNames);
                                                }
                                            }
                                            gl.getEventListener().setTrackData(data);
                                            if (writeDBCheckBox.isSelected()) {
                                                writerBodyName.addToQueue(data, dataid);
                                            }
                                            setCurTrackData(data);
                                        }
                                        oldFrameCounter = frameCounter;
                                        data = new DataFrame();
                                        data.frameCounter = frameCounter;
                                        data.internalTimeStamp = mysqlRes.getLong("data.internaltimestamp");
                                        data.numCalibBodies = mysqlRes.getInt("data.calibBodies");
                                        data.seriesid = mysqlRes.getInt("data.seriesid");
                                        data.specialFlag = mysqlRes.getString("data.flag");
                                        data.timeStamp = mysqlRes.getDouble("data.timestamp");
                                    }
                                    TrackedBody body = new TrackedBody();
                                    body.dbid = mysqlRes.getInt("bodies.id");
                                    body.artid = mysqlRes.getInt("bodies.artid");
                                    body.name = mysqlRes.getString("bodies.name");
                                    body.quality = mysqlRes.getDouble("bodies.quality");
                                    body.button = mysqlRes.getInt("bodies.button");
                                    body.type = mysqlRes.getInt("bodies.type");
                                    body.trackedId = mysqlRes.getInt("bodies.trackedid");
                                    body.position[0] = mysqlRes.getDouble("bodies.pos1");
                                    body.position[1] = mysqlRes.getDouble("bodies.pos2");
                                    body.position[2] = mysqlRes.getDouble("bodies.pos3");
                                    body.angles[0] = mysqlRes.getDouble("bodies.angle1");
                                    body.angles[1] = mysqlRes.getDouble("bodies.angle2");
                                    body.angles[2] = mysqlRes.getDouble("bodies.angle3");
                                    body.matrix[0] = mysqlRes.getDouble("bodies.mat1");
                                    body.matrix[1] = mysqlRes.getDouble("bodies.mat2");
                                    body.matrix[2] = mysqlRes.getDouble("bodies.mat3");
                                    body.matrix[3] = mysqlRes.getDouble("bodies.mat4");
                                    body.matrix[4] = mysqlRes.getDouble("bodies.mat5");
                                    body.matrix[5] = mysqlRes.getDouble("bodies.mat6");
                                    body.matrix[6] = mysqlRes.getDouble("bodies.mat7");
                                    body.matrix[7] = mysqlRes.getDouble("bodies.mat8");
                                    body.matrix[8] = mysqlRes.getDouble("bodies.mat9");
                                    boolean isOccluded = false;
                                    if (evalGUI.isStaticOccl()) {
                                        double xmin, xmax, ymin, ymax, zmin, zmax;
                                        OccludedTrackingEvalRect[] areaItems = evalGUI.getAreaItems();
                                        for (int i = 0; i < areaItems.length && !isOccluded; ++i) {
                                            xmin = areaItems[i].xmin;
                                            xmax = areaItems[i].xmax;
                                            ymin = areaItems[i].ymin;
                                            ymax = areaItems[i].ymax;
                                            zmin = areaItems[i].zmin;
                                            zmax = areaItems[i].zmax;
                                            double temp;
                                            if (xmax < xmin) {
                                                temp = xmax;
                                                xmax = xmin;
                                                xmin = temp;
                                            }
                                            if (ymax < ymin) {
                                                temp = ymax;
                                                ymax = ymin;
                                                ymin = temp;
                                            }
                                            if (zmax < zmin) {
                                                temp = zmax;
                                                zmax = zmin;
                                                zmin = temp;
                                            }
                                            if (xmin <= body.position[0] && xmax >= body.position[0] && ymin <= body.position[1] && ymax >= body.position[1] && zmin <= body.position[2] && zmax >= body.position[2]) {
                                                isOccluded = true;
                                            }
                                        }
                                    }
                                    if (evalGUI.isRandomOccl()) {
                                        Vector<OccludedTrackingEvalRect> removeBoxes = new Vector<OccludedTrackingEvalRect>();
                                        for (Iterator<OccludedTrackingEvalRect> boxIt = occlAreas.iterator(); boxIt.hasNext(); ) {
                                            OccludedTrackingEvalRect occlBox = boxIt.next();
                                            if (data.internalTimeStamp - occlBox.timestamp > occlBox.lasting) {
                                                removeBoxes.add(occlBox);
                                            }
                                        }
                                        for (Iterator<OccludedTrackingEvalRect> boxIt = removeBoxes.iterator(); boxIt.hasNext(); ) {
                                            OccludedTrackingEvalRect occlBox = boxIt.next();
                                            occlAreas.remove(occlBox);
                                        }
                                        if (occlAreas.size() < evalGUI.getMinNrAreas() || occlAreas.size() > evalGUI.getMaxNrAreas()) {
                                            Random random = new Random();
                                            int addNr = (int) (random.nextDouble() * (double) (evalGUI.getMaxNrAreas() - evalGUI.getMinNrAreas()) + (double) evalGUI.getMinNrAreas());
                                            for (int i = 0; i < addNr; ++i) {
                                                double xmin = random.nextDouble() * 3000 - 1500, xmax = random.nextDouble() * 3000 - 1500, ymin = random.nextDouble() * 3000 - 1500, ymax = random.nextDouble() * 3000 - 1500, zmin = random.nextDouble() * 3000 - 1500, zmax = random.nextDouble() * 3000 - 1500;
                                                double temp;
                                                if (xmax < xmin) {
                                                    temp = xmax;
                                                    xmax = xmin;
                                                    xmin = temp;
                                                }
                                                if (ymax < ymin) {
                                                    temp = ymax;
                                                    ymax = ymin;
                                                    ymin = temp;
                                                }
                                                if (zmax < zmin) {
                                                    temp = zmax;
                                                    zmax = zmin;
                                                    zmin = temp;
                                                }
                                                OccludedTrackingEvalRect newBox = new OccludedTrackingEvalRect(xmin, xmax, ymin, ymax, zmin, zmax);
                                                newBox.timestamp = data.internalTimeStamp;
                                                newBox.lasting = (long) (random.nextDouble() * (double) (evalGUI.getMaxChangeTime() - evalGUI.getMinChangeTime()) + (double) evalGUI.getMinChangeTime());
                                                occlAreas.add(newBox);
                                            }
                                        }
                                        for (Iterator<OccludedTrackingEvalRect> boxIt = occlAreas.iterator(); boxIt.hasNext(); ) {
                                            OccludedTrackingEvalRect occlBox = boxIt.next();
                                            if (occlBox.xmin <= body.position[0] && occlBox.xmax >= body.position[0] && occlBox.ymin <= body.position[1] && occlBox.ymax >= body.position[1] && occlBox.zmin <= body.position[2] && occlBox.zmax >= body.position[2]) {
                                                isOccluded = true;
                                            }
                                        }
                                    }
                                    body.occluded = isOccluded;
                                    data.bodies.add(body);
                                    Thread.yield();
                                } while (!stopAnimation && ((forth && mysqlRes.next()) || (!forth && mysqlRes.previous())));
                                if (!doLoop) {
                                    stopAnimation = true;
                                }
                                if (forthBack) {
                                    forth = !forth;
                                    count = -1;
                                }
                            }
                        }
                        if (writeDBCheckBox.isSelected()) {
                            database.getMysqlSource().getConnection().commit();
                        }
                    }
                    database.getMysqlSource().getConnection().setAutoCommit(true);
                } catch (SQLException ex) {
                    javax.swing.JOptionPane.showMessageDialog(mainGUI, "animateButtonActionPerformed: Couldn't retrieve data: " + ex.getMessage(), "SQL error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
                progrStat.resetIdleInvis();
            }
        };
        runner.start();
    }

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {
        stopAnimation = true;
    }

    private void evalButtonActionPerformed(java.awt.event.ActionEvent evt) {
        evalGUI.setVisible(true);
    }

    private double calcError() {
        if (trackingData.getCurTrackData().bodies.size() == 0) {
            return 0.0;
        }
        double dist, minError;
        double sumError = 0.0;
        InitialModelNode minNode;
        Vector<InitialModelNode> allNodes = new Vector<InitialModelNode>();
        for (Iterator<InitialModelNode> nodeIt = trackingData.getBodyModel().getFittedNodes().values().iterator(); nodeIt.hasNext(); ) {
            allNodes.add(nodeIt.next());
        }
        for (Iterator<TrackedBody> bodyIt = trackingData.getCurTrackData().bodies.iterator(); bodyIt.hasNext(); ) {
            TrackedBody body = bodyIt.next();
            minError = Double.MAX_VALUE;
            minNode = null;
            for (Iterator<InitialModelNode> nodeIt = allNodes.iterator(); nodeIt.hasNext(); ) {
                InitialModelNode node = nodeIt.next();
                dist = LinAlg.distSqu(body.position, node.getAbsPos());
                if (dist < minError) {
                    minError = dist;
                    minNode = node;
                }
            }
            if (minNode != null) {
                allNodes.remove(minNode);
                sumError += Math.sqrt(minError);
            }
        }
        return sumError / (double) trackingData.getCurTrackData().bodies.size();
    }

    private void gridSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {
        stopAnimation = false;
        Thread runner = new Thread() {

            @Override
            public void run() {
                final double minSigma = 0.001;
                final double maxSigma = 0.05;
                final double incSigma = 0.001;
                final double minEps = 0.001;
                final double maxEps = 0.05;
                final double incEps = 0.001;
                final double minLPReduce = 0.0;
                final double maxLPReduce = 1.0;
                final double incLPReduce = 0.2;
                final double minPDReduce = 0.0;
                final double maxPDReduce = 1.0;
                final double incPDReduce = 0.33;
                final int minIterations = 100;
                final int maxIterations = 500;
                final int incIterations = 200;
                final int maxSteps = ((int) (((maxSigma - minSigma) / incSigma + 1.0) * ((maxEps - minEps) / incEps + 1.0) * ((maxLPReduce - minLPReduce) / incLPReduce + 1.0) * ((maxPDReduce - minPDReduce) / incPDReduce + 1.0))) * ((maxIterations - minIterations) / incIterations + 1);
                progrStat.setMinMaxWorkingVis(0, maxSteps);
                if (trackingData.getCurTrackData() != null) {
                    if (trackingData.getBodyModel() != null && trackingData.getBodyTracking() != null) {
                        String refBodyString = optionsInt.getReferenceBody();
                        TrackedBody refBody = null;
                        if (refBodyString != null && !refBodyString.isEmpty() && !refBodyString.equals("none") && optionsInt.isUseForModelGen()) {
                            int bodyId = Integer.parseInt(refBodyString);
                            refBody = bodyIdMap.get(bodyId);
                        }
                        FileWriter evalWriter = null;
                        String evalFileName = evalGUI.getFileName();
                        if (!evalFileName.isEmpty()) {
                            try {
                                evalWriter = new FileWriter(evalFileName);
                            } catch (IOException ex) {
                                javax.swing.JOptionPane.showMessageDialog(mainGUI, "Wasn't able to open file " + evalFileName + ".", "Open file error", javax.swing.JOptionPane.ERROR_MESSAGE);
                                evalWriter = null;
                            }
                        }
                        double lprAvrgError, minLprAvrgError = Double.MAX_VALUE, minLpr = -1.0;
                        double minError = Double.MAX_VALUE, minErrorLprVal = 0.0, minErrorSigmaVal = 0.0, minErrorEpsVal = 0.0;
                        double resetJointsRatio = Double.parseDouble(resJointsRatio.getText());
                        int lprSteps;
                        int step = 0;
                        for (int it = minIterations; it <= maxIterations && !stopAnimation; it += incIterations) {
                            for (double pdreduce = minPDReduce; pdreduce <= maxPDReduce && !stopAnimation; pdreduce += incPDReduce) {
                                for (double lpreduce = minLPReduce; lpreduce <= maxLPReduce && !stopAnimation; lpreduce += incLPReduce) {
                                    lprAvrgError = 0.0;
                                    lprSteps = 0;
                                    for (double sigma = minSigma; sigma <= maxSigma && !stopAnimation; sigma += incSigma) {
                                        for (double eps = minEps; eps <= maxEps && !stopAnimation; eps += incEps) {
                                            progrStat.setProgress(++step);
                                            progrStat.setStatus("Working, Step Nr.: " + step + "   Out of max.: " + maxSteps);
                                            trackingData.getBodyModel().createFittedFromScaled();
                                            trackingData.getBodyTracking().setBaseModel(trackingData.getBodyModel());
                                            int iterations = trackingData.getBodyTracking().fitModel(trackingData.getCurTrackData(), sigma, eps, lpreduce, pdreduce, false, it, maxIterations, true, resetJointsRatio, inflDistPresCheckBox.isSelected(), inflAnglesResetCheckBox.isSelected(), refBody, optionsInt.isNotUseOther6D(), resetPosMarkerPosAfterCheckBox.isSelected(), winnerMethodComboBox.getSelectedIndex());
                                            double dblIter = (double) iterations / (double) maxIterations;
                                            double evalErr = calcError() / 10000.0;
                                            double error = Math.sqrt(dblIter * dblIter / 10.0 + evalErr * evalErr);
                                            if (evalWriter != null) {
                                                try {
                                                    evalWriter.write(it + " " + sigma + " " + eps + " " + lpreduce + " " + pdreduce + " " + dblIter + " " + evalErr + " " + error + "\n");
                                                } catch (IOException ex) {
                                                    javax.swing.JOptionPane.showMessageDialog(mainGUI, "Wasn't able to write to file.", "Write error", javax.swing.JOptionPane.ERROR_MESSAGE);
                                                    evalWriter = null;
                                                }
                                            } else {
                                                System.out.println(it + " " + sigma + " " + eps + " " + lpreduce + " " + pdreduce + " " + dblIter + " " + evalErr + " " + error);
                                            }
                                            lprAvrgError += error;
                                            ++lprSteps;
                                            if (error < minError) {
                                                minError = error;
                                                minErrorLprVal = lpreduce;
                                                minErrorSigmaVal = sigma;
                                                minErrorEpsVal = eps;
                                            }
                                        }
                                    }
                                    lprAvrgError /= (double) lprSteps;
                                    if (lprAvrgError < minLprAvrgError) {
                                        minLprAvrgError = lprAvrgError;
                                        minLpr = lpreduce;
                                    }
                                }
                            }
                        }
                        if (evalWriter != null) {
                            try {
                                evalWriter.flush();
                                evalWriter.close();
                            } catch (IOException ex) {
                            }
                            evalWriter = null;
                        }
                    }
                }
                progrStat.resetIdleInvis();
            }
        };
        runner.start();
    }

    /** set labelled flag for current series */
    private final void labelSeries(Integer label, String color) {
        if (database.getMysqlSource().isConnected() && database.selectedSeriesExists()) {
            final int selInd = database.getSelectedSeriesIndex();
            final Integer seriesid = database.getSelectedSeriesID();
            if (seriesid != null) {
                try {
                    Statement mysqlStat = database.getMysqlSource().getConnection().createStatement();
                    if (label == null) mysqlStat.executeUpdate("update series set alllabelled=null where id=" + seriesid); else mysqlStat.executeUpdate("update series set alllabelled=" + label + " where id=" + seriesid);
                    String selItem = database.getSelectedSeries();
                    int brOpen = selItem.indexOf('<'), brClose = -1;
                    while (brOpen >= 0) {
                        brClose = selItem.indexOf('>', brOpen);
                        selItem = selItem.substring(0, brOpen) + selItem.substring(brClose + 1);
                        brOpen = selItem.indexOf('<');
                    }
                    selItem.trim();
                    if (color == null) database.replaceSeries(selInd, selItem, true); else database.replaceSeries(selInd, "<html><body bgcolor=\"" + color + "\">" + selItem + "</body></html>", true);
                    database.labelSeriesForPos(selInd, label);
                } catch (SQLException ex) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Wasn't able to update table: " + ex.getMessage(), "SQL error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void setLabelledButtonActionPerformed(java.awt.event.ActionEvent evt) {
        labelSeries(1, "#10FF10");
    }

    private void setUnlabelledButtonActionPerformed(java.awt.event.ActionEvent evt) {
        labelSeries(0, "#FF1010");
    }

    private void resetLabelledButtonActionPerformed(java.awt.event.ActionEvent evt) {
        labelSeries(null, null);
    }

    private javax.swing.JButton animateButton;

    private javax.swing.JButton asInitialButton;

    private javax.swing.JTextField epsTextField;

    private javax.swing.JButton evalButton;

    private javax.swing.JButton fbLoopButton;

    private javax.swing.JButton gridSearchButton;

    private javax.swing.JTextField idpTextField;

    private javax.swing.JCheckBox inflAnglesResetCheckBox;

    private javax.swing.JCheckBox inflDistPresCheckBox;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel10;

    private javax.swing.JLabel jLabel11;

    private javax.swing.JLabel jLabel12;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JLabel jLabel8;

    private javax.swing.JLabel jLabel9;

    private javax.swing.JSeparator jSeparator1;

    private javax.swing.JSeparator jSeparator2;

    private javax.swing.JSeparator jSeparator3;

    private javax.swing.JSeparator jSeparator4;

    private javax.swing.JButton loopButton;

    private javax.swing.JTextField lprTextField;

    private javax.swing.JTextField maxStepsTextField;

    private javax.swing.JTextField minDistTextField;

    private javax.swing.JTextField resJointsRatio;

    private javax.swing.JButton resetLabelledButton;

    private javax.swing.JCheckBox resetPosMarkerPosAfterCheckBox;

    private javax.swing.JButton resetToInitialButton;

    private javax.swing.JCheckBox reverseAnimBox;

    private javax.swing.JButton setLabelledButton;

    private javax.swing.JButton setUnlabelledButton;

    private javax.swing.JTextField sigmaTextField;

    private javax.swing.JButton stopButton;

    private javax.swing.JButton trackIDsButton;

    private javax.swing.JComboBox winnerMethodComboBox;

    private javax.swing.JCheckBox writeDBCheckBox;

    private javax.swing.JCheckBox wtaNetBox;

    @Override
    public final void resetOcclAreas() {
        gl.getEventListener().setStatOcclAreas(evalGUI.getAreaItems());
    }

    /** get sigma */
    @Override
    public final String getSigma() {
        return sigmaTextField.getText();
    }

    /** get epsilon */
    @Override
    public final String getEpsilon() {
        return epsTextField.getText();
    }

    /** get wta-net*/
    @Override
    public final Boolean getWta() {
        return wtaNetBox.isSelected();
    }

    /** get lpr */
    @Override
    public final String getLPR() {
        return lprTextField.getText();
    }

    /** get influence distance */
    @Override
    public final Boolean getInfluenceDistance() {
        return inflDistPresCheckBox.isSelected();
    }

    /** get idp */
    @Override
    public final String getIDP() {
        return idpTextField.getText();
    }

    /** return influence angles */
    @Override
    public final Boolean getInfluenceAngles() {
        return inflAnglesResetCheckBox.isSelected();
    }

    /** return reset joints */
    @Override
    public final String getResetJoints() {
        return resJointsRatio.getText();
    }

    /** return reset postion marker position after */
    @Override
    public final Boolean getPositionMarker() {
        return resetPosMarkerPosAfterCheckBox.isSelected();
    }

    /** return minimal distance */
    @Override
    public final String getMinDistance() {
        return minDistTextField.getText();
    }

    /** return maximal steps */
    @Override
    public final String getMaxSteps() {
        return maxStepsTextField.getText();
    }

    /** return tracked ids*/
    @Override
    public final String getTrackID() {
        return maxStepsTextField.getText();
    }

    @Override
    public final Integer getWinnerMethod() {
        return winnerMethodComboBox.getSelectedIndex();
    }
}
