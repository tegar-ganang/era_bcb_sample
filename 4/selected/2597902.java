package org.aquastarz.score.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JasperViewer;
import org.aquastarz.score.ScoreApp;
import org.aquastarz.score.controller.RoutinesController;
import org.aquastarz.score.controller.ScoreController;
import org.aquastarz.score.manager.SwimmerManager;
import org.aquastarz.score.domain.FiguresParticipant;
import org.aquastarz.score.domain.Meet;
import org.aquastarz.score.domain.Swimmer;
import org.aquastarz.score.domain.Team;
import org.aquastarz.score.gui.event.MeetSetupPanelListener;
import org.aquastarz.score.gui.event.FiguresParticipantSearchPanelListener;
import org.aquastarz.score.gui.event.RoutinesPanelEventListener;
import org.aquastarz.score.manager.MeetManager;
import org.aquastarz.score.report.FiguresLabel;
import org.aquastarz.score.report.NumMeets;
import org.aquastarz.score.report.TeamPoints;

public class SynchroFrame extends javax.swing.JFrame {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SynchroFrame.class.getName());

    public enum Tab {

        MEET_SETUP, SWIMMERS, FIGURES_ORDER, FIGURES, ROUTINES, REPORTS, LEAGUE
    }

    ;

    private ScoreController controller = null;

    private static Meet meet = null;

    Image appIcon = null;

    /** Creates new form Synchro */
    public SynchroFrame(ScoreController controller, Meet meet) {
        this.controller = controller;
        SynchroFrame.meet = meet;
        try {
            URL rsrcUrl = Thread.currentThread().getContextClassLoader().getResource("org/aquastarz/score/gui/synchro-icon.png");
            appIcon = ImageIO.read(rsrcUrl);
        } catch (Exception e) {
            logger.error("Error loading app icon.", e);
        }
        initComponents();
        listenForHotKeys();
        registerListeners();
        meetSetup.fillForm(meet, controller.getFigures(), controller.getTeams());
        disableAllTabs();
        setTabEnabled(SynchroFrame.Tab.MEET_SETUP, true);
        updateStatus();
        updateFiguresStatus();
        updateRoutinesStatus();
    }

    public static Meet getMeet() {
        return meet;
    }

    private void listenForHotKeys() {
        Action swimmerSearchAction = new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                if (tabPane.getSelectedComponent() == figureScore) {
                    swimmerSearchPanel.requestFocus();
                }
            }
        };
        String keyStrokeAndKey = "PERIOD";
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeAndKey);
        tabPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, keyStrokeAndKey);
        tabPane.getActionMap().put(keyStrokeAndKey, swimmerSearchAction);
        keyStrokeAndKey = "DECIMAL";
        keyStroke = KeyStroke.getKeyStroke(keyStrokeAndKey);
        tabPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, keyStrokeAndKey);
        tabPane.getActionMap().put(keyStrokeAndKey, swimmerSearchAction);
    }

    private void setSetupStatus(Color color, int percent) {
        setupProgress.setForeground(color);
        setupProgress.setValue(percent);
    }

    public void setNovFiguresStatus(Color color, int percent) {
        novFiguresProgress.setForeground(color);
        novFiguresProgress.setValue(percent);
    }

    public void setIntFiguresStatus(Color color, int percent) {
        intFiguresProgress.setForeground(color);
        intFiguresProgress.setValue(percent);
    }

    public void setRoutinesStatus(Color color, int percent) {
        routinesProgress.setForeground(color);
        routinesProgress.setValue(percent);
    }

    private void updateStatus() {
        updateLeagueList();
        setTabEnabled(Tab.LEAGUE, true);
        if (meet.isValid()) {
            setTabEnabled(SynchroFrame.Tab.SWIMMERS, true);
            setTabEnabled(Tab.ROUTINES, true);
            updateSwimmerTab();
            if (meet.hasFiguresParticipants(meet)) {
                if (meet.getFiguresOrderGenerated()) {
                    setSetupStatus(Color.GREEN.darker(), 100);
                    setTabEnabled(Tab.FIGURES_ORDER, true);
                    setTabEnabled(Tab.FIGURES, true);
                    setTabEnabled(Tab.REPORTS, true);
                    updateFiguresOrderList();
                } else {
                    setSetupStatus(Color.YELLOW, 90);
                }
            } else {
                setSetupStatus(Color.YELLOW, 45);
                setTabEnabled(Tab.FIGURES_ORDER, false);
                setTabEnabled(Tab.FIGURES, false);
                setTabEnabled(Tab.REPORTS, false);
            }
        } else {
            setSetupStatus(Color.RED, 5);
            setTabEnabled(SynchroFrame.Tab.SWIMMERS, false);
            setTabEnabled(Tab.ROUTINES, false);
        }
    }

    public final void updateFiguresStatus() {
        int p = ScoreController.percentCompleteFigures(meet, true);
        if (p < 100) {
            setNovFiguresStatus(Color.GREEN, p);
        } else {
            setNovFiguresStatus(Color.GREEN.darker(), p);
        }
        p = ScoreController.percentCompleteFigures(meet, false);
        if (p < 100) {
            setIntFiguresStatus(Color.GREEN, p);
        } else {
            setIntFiguresStatus(Color.GREEN.darker(), p);
        }
    }

    public final void updateRoutinesStatus() {
        int p = RoutinesController.percentCompleteRoutines(meet);
        if (p < 100) {
            setRoutinesStatus(Color.GREEN, p);
        } else {
            setRoutinesStatus(Color.GREEN.darker(), p);
        }
    }

    private void disableAllTabs() {
        for (Tab tab : Tab.values()) {
            setTabEnabled(tab, false);
        }
    }

    private void setTabEnabled(Tab tab, boolean enabled) {
        tabPane.setEnabledAt(tab.ordinal(), enabled);
    }

    private void selectTab(Tab tab) {
        tabPane.setSelectedIndex(tab.ordinal());
    }

    private void registerListeners() {
        meetSetup.addMeetSetupPanelListener(new MeetSetupPanelListener() {

            public void meetSetupSaved() {
                meet.clearPoints();
                controller.saveMeet(meet);
                updateStatus();
                if (!meet.isValid()) {
                    JOptionPane.showMessageDialog(rootPane, "The Meet setup that you have saved is not complete/correct.", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        swimmerSearchPanel.addFiguresParticipantSearchPanelListener(new FiguresParticipantSearchPanelListener() {

            public void figuresParticipantSearchRequested(String figureOrder) {
                doFiguresParticipantSearch(figureOrder);
            }

            public void figuresParticipantSet() {
                figureScorePanel.requestFocus();
            }
        });
    }

    private void doFiguresParticipantSearch(String figureOrder) {
        FiguresParticipant figuresParticipant = controller.findFiguresParticipantByFigureOrder(meet, figureOrder);
        if (figuresParticipant != null) {
            swimmerSearchPanel.setFiguresParticipant(figuresParticipant);
            figureScorePanel.setData(meet.getFigureList(figuresParticipant.getSwimmer()), figuresParticipant);
        } else {
            clearFiguresScorePanel();
            JOptionPane.showMessageDialog(swimmerSearchPanel, "Swimmer not found.", "Invalid Entry", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void clearFiguresScorePanel() {
        swimmerSearchPanel.clear();
        figureScorePanel.clearScores();
    }

    private void updateSwimmerTab() {
        int selectedIndex = teamTabs.getSelectedIndex();
        String selectedTab = selectedIndex == -1 ? "" : teamTabs.getTitleAt(teamTabs.getSelectedIndex());
        HashMap<String, Integer> sortMap = new HashMap<String, Integer>();
        for (int i = 0; i < teamTabs.getTabCount(); i++) {
            SwimmerSelectionPanel ssp = (SwimmerSelectionPanel) teamTabs.getComponentAt(i);
            sortMap.put(teamTabs.getTitleAt(i), ssp.getSortIndex());
        }
        teamTabs.removeAll();
        SwimmerSelectionPanel panel = new SwimmerSelectionPanel(meet.getHomeTeam());
        teamTabs.add(meet.getHomeTeam().getTeamId(), panel);
        panel.setSwimmers(SwimmerManager.getSwimmers(meet.getHomeTeam()), meet.getSwimmers());
        for (Team opponent : meet.getOpponents()) {
            panel = new SwimmerSelectionPanel(opponent);
            teamTabs.add(opponent.getTeamId(), panel);
            panel.setSwimmers(SwimmerManager.getSwimmers(opponent), meet.getSwimmers());
        }
        selectedIndex = teamTabs.indexOfTab(selectedTab);
        if (selectedIndex > -1) {
            teamTabs.setSelectedIndex(selectedIndex);
        }
        for (String title : sortMap.keySet()) {
            int i = teamTabs.indexOfTab(title);
            if (i > -1) {
                SwimmerSelectionPanel ssp = (SwimmerSelectionPanel) teamTabs.getComponentAt(i);
                ssp.setSortIndex(sortMap.get(title));
            }
        }
    }

    private void updateFiguresOrderList() {
        List<FiguresParticipant> figuresParticipants = meet.getFiguresParticipants();
        Map<String, FiguresParticipant> sortedFiguresParticipants = new TreeMap<String, FiguresParticipant>();
        for (FiguresParticipant figuresParticipant : figuresParticipants) {
            String key = figuresParticipant.getFigureOrder();
            if (figureOrderSortByName.isSelected()) {
                key = figuresParticipant.getSwimmer().getLastName() + figuresParticipant.getSwimmer().getFirstName();
            }
            sortedFiguresParticipants.put(key, figuresParticipant);
        }
        figureOrderTable.setModel(new FiguresParticipantsTableModel(sortedFiguresParticipants.values()));
    }

    private List<Swimmer> getLeagueTabSwimmerList() {
        EntityManager entityManager = ScoreApp.getEntityManager();
        Query swimmerQuery = null;
        String teamId = "%";
        Object o = leagueTeamCombo.getSelectedItem();
        if (o != null && o instanceof String) {
            teamId = (String) o;
            if ("[All]".equals(o)) {
                teamId = "%";
            }
        }
        if (leagueSortByName.isSelected()) {
            swimmerQuery = entityManager.createNamedQuery("Swimmer.findByTeamIdAndSeasonOrderByName");
        } else if (leagueSortByTeam.isSelected()) {
            swimmerQuery = entityManager.createNamedQuery("Swimmer.findByTeamIdAndSeasonOrderByTeamAndName");
        } else if (leagueSortByLevel.isSelected()) {
            swimmerQuery = entityManager.createNamedQuery("Swimmer.findByTeamIdAndSeasonOrderByLevelAndName");
        } else {
            swimmerQuery = entityManager.createNamedQuery("Swimmer.findByTeamIdAndSeasonOrderByLeagueNum");
        }
        swimmerQuery.setParameter("teamId", teamId);
        swimmerQuery.setParameter("season", ScoreApp.getCurrentSeason());
        return swimmerQuery.getResultList();
    }

    private void updateLeagueList() {
        List<Swimmer> swimmerList = getLeagueTabSwimmerList();
        swimmerTable.setModel(new SwimmersTableModel(swimmerList));
    }

    private void viewFiguresResultsReport(List<FiguresParticipant> figuresParticipants, String title) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresResults.jasper"));
            JRDataSource data = new JRBeanCollectionDataSource(figuresParticipants);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("Title", title);
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
    }

    private Integer getSkipLabels() {
        Integer skipLabels = -1;
        while (skipLabels < 0) {
            String s = (String) JOptionPane.showInputDialog(this, "How many empty labels?", "Print labels", JOptionPane.QUESTION_MESSAGE + JOptionPane.OK_OPTION, null, null, "0");
            if (s == null) {
                skipLabels = null;
                break;
            }
            try {
                skipLabels = Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                skipLabels = -1;
            }
            if (skipLabels < 0 || skipLabels > 30) {
                skipLabels = -1;
            }
        }
        return skipLabels;
    }

    private void showMeetCalcErrors() {
        if (meet.hasCalcErrors()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><p>");
            TreeSet<String> sortedErrors = new TreeSet<String>();
            sortedErrors.addAll(meet.getCalcErrors());
            for (String s : sortedErrors) {
                sb.append(s).append("<br>");
            }
            sb.append("</p></html>");
            JScrollPane scrollPane = new JScrollPane(new JLabel(sb.toString()));
            scrollPane.setPreferredSize(new Dimension(600, 100));
            Object message = scrollPane;
            JOptionPane.showMessageDialog(this, message, "Errors calculating meet results", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        figuresOrderSortButtonGroup = new javax.swing.ButtonGroup();
        leagueSortButtonGroup = new javax.swing.ButtonGroup();
        tabPane = new javax.swing.JTabbedPane();
        meetSetup = new org.aquastarz.score.gui.MeetSetupPanel();
        swimmers = new javax.swing.JPanel();
        saveSwimmersButton = new javax.swing.JButton();
        teamTabs = new javax.swing.JTabbedPane();
        generateRandomFiguresOrderButton = new javax.swing.JButton();
        figuresOrder = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        figureOrderSortByNumber = new javax.swing.JRadioButton();
        figureOrderSortByName = new javax.swing.JRadioButton();
        figureOrderScrollPane = new javax.swing.JScrollPane();
        figureOrderTable = new javax.swing.JTable();
        figuresOrderPrintButton = new javax.swing.JButton();
        figuresOrderLinesCheckbox = new javax.swing.JCheckBox();
        figureScore = new javax.swing.JPanel();
        swimmerSearchPanel = new org.aquastarz.score.gui.FiguresParticipantSearchPanel();
        figureScorePanel = new org.aquastarz.score.gui.FigureScorePanel();
        jPanel3 = new javax.swing.JPanel();
        saveFigureScoreButton = new javax.swing.JButton();
        clearFigureScoreButton = new javax.swing.JButton();
        routinesPanel = new org.aquastarz.score.gui.RoutinesPanel();
        reportPanel = new javax.swing.JPanel();
        reportNoviceFigures = new javax.swing.JButton();
        reportNovMeetSheet = new javax.swing.JButton();
        reportNoviceStation = new javax.swing.JButton();
        reportIntermediateFigures = new javax.swing.JButton();
        reportIntMeetSheet = new javax.swing.JButton();
        reportIntStation = new javax.swing.JButton();
        reportTeamResults = new javax.swing.JButton();
        reportNovFigureLabels = new javax.swing.JButton();
        reportIntFigureLabels = new javax.swing.JButton();
        reportNovRoutines = new javax.swing.JButton();
        reportNovRoutineLabels = new javax.swing.JButton();
        reportIntRoutines = new javax.swing.JButton();
        reportIntRoutineLabels = new javax.swing.JButton();
        reportAllRoutines = new javax.swing.JButton();
        reportAllRoutineLabels = new javax.swing.JButton();
        exportMeetDataButton = new javax.swing.JButton();
        compareMeetButton = new javax.swing.JButton();
        leaguePanel = new javax.swing.JPanel();
        leaguePrintButton = new javax.swing.JButton();
        swimmerScrollPane = new javax.swing.JScrollPane();
        swimmerTable = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        leagueSortByNumber = new javax.swing.JRadioButton();
        leagueSortByName = new javax.swing.JRadioButton();
        leagueSortByTeam = new javax.swing.JRadioButton();
        jLabel7 = new javax.swing.JLabel();
        leagueTeamCombo = new javax.swing.JComboBox();
        leagueSortByLevel = new javax.swing.JRadioButton();
        numMeetsPrintButton = new javax.swing.JButton();
        jToolBar1 = new javax.swing.JToolBar();
        jLabel1 = new javax.swing.JLabel();
        setupProgress = new javax.swing.JProgressBar();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        jLabel2 = new javax.swing.JLabel();
        novFiguresProgress = new javax.swing.JProgressBar();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jLabel3 = new javax.swing.JLabel();
        intFiguresProgress = new javax.swing.JProgressBar();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jLabel4 = new javax.swing.JLabel();
        routinesProgress = new javax.swing.JProgressBar();
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("SynchroScore");
        setIconImage(appIcon);
        setMinimumSize(new java.awt.Dimension(603, 200));
        setName("");
        tabPane.setFont(new java.awt.Font("Tahoma", 0, 14));
        tabPane.setMinimumSize(new java.awt.Dimension(603, 200));
        tabPane.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabPaneStateChanged(evt);
            }
        });
        tabPane.addTab("Meet Setup", meetSetup);
        saveSwimmersButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        saveSwimmersButton.setText("Save");
        saveSwimmersButton.setToolTipText("Save figures participants.");
        saveSwimmersButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSwimmersButtonActionPerformed(evt);
            }
        });
        teamTabs.setTabPlacement(javax.swing.JTabbedPane.LEFT);
        teamTabs.setFont(new java.awt.Font("Tahoma", 0, 14));
        generateRandomFiguresOrderButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        generateRandomFiguresOrderButton.setText("Randomize");
        generateRandomFiguresOrderButton.setToolTipText("Generate random figures order.");
        generateRandomFiguresOrderButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                generateRandomFiguresOrderButtonActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout swimmersLayout = new javax.swing.GroupLayout(swimmers);
        swimmers.setLayout(swimmersLayout);
        swimmersLayout.setHorizontalGroup(swimmersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, swimmersLayout.createSequentialGroup().addComponent(teamTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 612, Short.MAX_VALUE).addGap(18, 18, 18).addGroup(swimmersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(saveSwimmersButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(generateRandomFiguresOrderButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()));
        swimmersLayout.setVerticalGroup(swimmersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(swimmersLayout.createSequentialGroup().addContainerGap().addComponent(saveSwimmersButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(generateRandomFiguresOrderButton).addContainerGap(459, Short.MAX_VALUE)).addComponent(teamTabs, javax.swing.GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE));
        tabPane.addTab("Swimmers", swimmers);
        jLabel5.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel5.setText("Sort by:");
        figuresOrderSortButtonGroup.add(figureOrderSortByNumber);
        figureOrderSortByNumber.setFont(new java.awt.Font("Tahoma", 0, 14));
        figureOrderSortByNumber.setSelected(true);
        figureOrderSortByNumber.setText("Number");
        figureOrderSortByNumber.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                figuresOrderSortByNumberActionPerformed(evt);
            }
        });
        figuresOrderSortButtonGroup.add(figureOrderSortByName);
        figureOrderSortByName.setFont(new java.awt.Font("Tahoma", 0, 14));
        figureOrderSortByName.setText("Name");
        figureOrderSortByName.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                figuresOrderSortByNameActionPerformed(evt);
            }
        });
        figureOrderTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        figureOrderScrollPane.setViewportView(figureOrderTable);
        figuresOrderPrintButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        figuresOrderPrintButton.setText("Print");
        figuresOrderPrintButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                figuresOrderPrintButtonActionPerformed(evt);
            }
        });
        figuresOrderLinesCheckbox.setFont(new java.awt.Font("Tahoma", 0, 14));
        figuresOrderLinesCheckbox.setSelected(true);
        figuresOrderLinesCheckbox.setText("Lines");
        javax.swing.GroupLayout figuresOrderLayout = new javax.swing.GroupLayout(figuresOrder);
        figuresOrder.setLayout(figuresOrderLayout);
        figuresOrderLayout.setHorizontalGroup(figuresOrderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, figuresOrderLayout.createSequentialGroup().addContainerGap().addComponent(figureOrderScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 638, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(figuresOrderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel5).addComponent(figureOrderSortByNumber).addComponent(figureOrderSortByName).addComponent(figuresOrderLinesCheckbox).addComponent(figuresOrderPrintButton)).addContainerGap()));
        figuresOrderLayout.setVerticalGroup(figuresOrderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(figuresOrderLayout.createSequentialGroup().addContainerGap().addGroup(figuresOrderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(figureOrderScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE).addGroup(figuresOrderLayout.createSequentialGroup().addComponent(jLabel5).addGap(1, 1, 1).addComponent(figureOrderSortByNumber).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(figureOrderSortByName).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(figuresOrderLinesCheckbox).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(figuresOrderPrintButton))).addContainerGap()));
        tabPane.addTab("Figures Order", figuresOrder);
        figureScore.setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        figureScore.add(swimmerSearchPanel, gridBagConstraints);
        figureScorePanel.setMinimumSize(new java.awt.Dimension(725, 150));
        figureScorePanel.setPreferredSize(new java.awt.Dimension(725, 150));
        figureScorePanel.addPropertyChangeListener(new java.beans.PropertyChangeListener() {

            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                figureScorePanelPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        figureScore.add(figureScorePanel, gridBagConstraints);
        saveFigureScoreButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        saveFigureScoreButton.setText("Save");
        saveFigureScoreButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveFigureScoreButtonActionPerformed(evt);
            }
        });
        saveFigureScoreButton.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                saveFigureScoreButtonKeyPressed(evt);
            }
        });
        clearFigureScoreButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        clearFigureScoreButton.setText("Clear");
        clearFigureScoreButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearFigureScoreButtonActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addComponent(saveFigureScoreButton).addGap(18, 18, 18).addComponent(clearFigureScoreButton).addContainerGap(585, Short.MAX_VALUE)));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(saveFigureScoreButton).addComponent(clearFigureScoreButton)).addContainerGap(262, Short.MAX_VALUE)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        figureScore.add(jPanel3, gridBagConstraints);
        tabPane.addTab("Figures", figureScore);
        tabPane.addTab("Routines", routinesPanel);
        routinesPanel.addRoutinesPanelEventListener(new RoutinesPanelEventListener() {

            public void routineSaved() {
                updateRoutinesStatus();
            }
        });
        reportNoviceFigures.setText("Nov. Figures");
        reportNoviceFigures.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportNoviceFiguresActionPerformed(evt);
            }
        });
        reportNovMeetSheet.setText("Nov. Meet Sheet");
        reportNovMeetSheet.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportNovMeetSheetActionPerformed(evt);
            }
        });
        reportNoviceStation.setText("Nov. Station");
        reportNoviceStation.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportNoviceStationActionPerformed(evt);
            }
        });
        reportIntermediateFigures.setText("Int. Figures");
        reportIntermediateFigures.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportIntermediateFiguresActionPerformed(evt);
            }
        });
        reportIntMeetSheet.setText("Int. Meet Sheet");
        reportIntMeetSheet.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportIntMeetSheetActionPerformed(evt);
            }
        });
        reportIntStation.setText("Int. Station");
        reportIntStation.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportIntStationActionPerformed(evt);
            }
        });
        reportTeamResults.setText("Team Results");
        reportTeamResults.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportTeamResultsActionPerformed(evt);
            }
        });
        reportNovFigureLabels.setText("Nov. Figure Labels");
        reportNovFigureLabels.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportNovFigureLabelsActionPerformed(evt);
            }
        });
        reportIntFigureLabels.setText("Int. Figure Labels");
        reportIntFigureLabels.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportIntFigureLabelsActionPerformed(evt);
            }
        });
        reportNovRoutines.setText("Nov. Routines");
        reportNovRoutines.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportNovRoutinesActionPerformed(evt);
            }
        });
        reportNovRoutineLabels.setText("Nov. Routine Labels");
        reportNovRoutineLabels.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportNovRoutineLabelsActionPerformed(evt);
            }
        });
        reportIntRoutines.setText("Int. Routines");
        reportIntRoutines.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportIntRoutinesActionPerformed(evt);
            }
        });
        reportIntRoutineLabels.setText("Int. Routine Labels");
        reportIntRoutineLabels.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportIntRoutineLabelsActionPerformed(evt);
            }
        });
        reportAllRoutines.setText("All Routines");
        reportAllRoutines.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportAllRoutinesActionPerformed(evt);
            }
        });
        reportAllRoutineLabels.setText("All Routine Labels");
        reportAllRoutineLabels.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportAllRoutineLabelsActionPerformed(evt);
            }
        });
        exportMeetDataButton.setText("Export Meet Data");
        exportMeetDataButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMeetDataButtonActionPerformed(evt);
            }
        });
        compareMeetButton.setText("Compare Meet");
        compareMeetButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                compareMeetButtonActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout reportPanelLayout = new javax.swing.GroupLayout(reportPanel);
        reportPanel.setLayout(reportPanelLayout);
        reportPanelLayout.setHorizontalGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(reportPanelLayout.createSequentialGroup().addGap(44, 44, 44).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(reportNovRoutineLabels).addComponent(reportNovRoutines).addComponent(reportNoviceStation).addComponent(reportNovMeetSheet).addComponent(reportNovFigureLabels).addComponent(reportNoviceFigures)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(reportPanelLayout.createSequentialGroup().addComponent(reportIntRoutines).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(reportAllRoutines)).addComponent(reportIntStation).addComponent(reportIntMeetSheet).addComponent(reportIntFigureLabels).addGroup(reportPanelLayout.createSequentialGroup().addComponent(reportIntermediateFigures).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(reportTeamResults)).addGroup(reportPanelLayout.createSequentialGroup().addComponent(reportIntRoutineLabels).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(reportAllRoutineLabels, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(exportMeetDataButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(compareMeetButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))).addContainerGap(308, Short.MAX_VALUE)));
        reportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { reportAllRoutineLabels, reportAllRoutines, reportIntFigureLabels, reportIntMeetSheet, reportIntRoutineLabels, reportIntRoutines, reportIntStation, reportIntermediateFigures, reportTeamResults });
        reportPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] { reportNovFigureLabels, reportNovMeetSheet, reportNovRoutineLabels, reportNovRoutines, reportNoviceFigures, reportNoviceStation });
        reportPanelLayout.setVerticalGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(reportPanelLayout.createSequentialGroup().addGap(5, 5, 5).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(reportNoviceFigures).addComponent(reportIntermediateFigures).addComponent(reportTeamResults)).addGap(5, 5, 5).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(reportNovFigureLabels).addComponent(reportIntFigureLabels)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(reportPanelLayout.createSequentialGroup().addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(reportNovMeetSheet).addComponent(reportIntMeetSheet)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(reportNoviceStation).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(reportNovRoutines).addComponent(reportIntRoutines).addComponent(reportAllRoutines))).addGroup(reportPanelLayout.createSequentialGroup().addGap(29, 29, 29).addComponent(reportIntStation))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(reportPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(reportNovRoutineLabels).addComponent(reportIntRoutineLabels).addComponent(reportAllRoutineLabels)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(exportMeetDataButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(compareMeetButton).addContainerGap(296, Short.MAX_VALUE)));
        tabPane.addTab("Reports", reportPanel);
        leaguePrintButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        leaguePrintButton.setText("Print");
        leaguePrintButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leaguePrintButtonActionPerformed(evt);
            }
        });
        swimmerTable.setModel(new javax.swing.table.DefaultTableModel(new Object[][] { { null, null, null, null }, { null, null, null, null }, { null, null, null, null }, { null, null, null, null } }, new String[] { "Title 1", "Title 2", "Title 3", "Title 4" }));
        swimmerScrollPane.setViewportView(swimmerTable);
        jLabel6.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel6.setText("Sort by:");
        leagueSortButtonGroup.add(leagueSortByNumber);
        leagueSortByNumber.setFont(new java.awt.Font("Tahoma", 0, 14));
        leagueSortByNumber.setSelected(true);
        leagueSortByNumber.setText("Number");
        leagueSortByNumber.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leagueSortByNumberfiguresOrderSortByNumberActionPerformed(evt);
            }
        });
        leagueSortButtonGroup.add(leagueSortByName);
        leagueSortByName.setFont(new java.awt.Font("Tahoma", 0, 14));
        leagueSortByName.setText("Name");
        leagueSortByName.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leagueSortByNamefiguresOrderSortByNameActionPerformed(evt);
            }
        });
        leagueSortButtonGroup.add(leagueSortByTeam);
        leagueSortByTeam.setFont(new java.awt.Font("Tahoma", 0, 14));
        leagueSortByTeam.setText("Team");
        leagueSortByTeam.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leagueSortByTeamfiguresOrderSortByNameActionPerformed(evt);
            }
        });
        jLabel7.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel7.setText("Team:");
        Query teamQuery = ScoreApp.getEntityManager().createNamedQuery("Team.findAllOrderByTeamId");
        List<Team> teamList = teamQuery.getResultList();
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel();
        dcbm.addElement("[All]");
        for (Team team : teamList) {
            dcbm.addElement(team.getTeamId());
        }
        leagueTeamCombo.setModel(dcbm);
        leagueTeamCombo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leagueTeamComboActionPerformed(evt);
            }
        });
        leagueSortButtonGroup.add(leagueSortByLevel);
        leagueSortByLevel.setFont(new java.awt.Font("Tahoma", 0, 14));
        leagueSortByLevel.setText("Level");
        leagueSortByLevel.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leagueSortByLevelfiguresOrderSortByNameActionPerformed(evt);
            }
        });
        numMeetsPrintButton.setFont(new java.awt.Font("Tahoma", 0, 14));
        numMeetsPrintButton.setText("# Meets");
        numMeetsPrintButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numMeetsPrintButtonActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout leaguePanelLayout = new javax.swing.GroupLayout(leaguePanel);
        leaguePanel.setLayout(leaguePanelLayout);
        leaguePanelLayout.setHorizontalGroup(leaguePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, leaguePanelLayout.createSequentialGroup().addComponent(swimmerScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 642, Short.MAX_VALUE).addGroup(leaguePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addGroup(leaguePanelLayout.createSequentialGroup().addGap(6, 6, 6).addGroup(leaguePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel6).addComponent(leagueSortByNumber, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(leagueSortByName).addComponent(leagueSortByTeam).addComponent(leagueSortByLevel))).addGroup(leaguePanelLayout.createSequentialGroup().addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(leaguePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(leagueTeamCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addGroup(leaguePanelLayout.createSequentialGroup().addComponent(jLabel7).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)).addGroup(leaguePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addComponent(leaguePrintButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(numMeetsPrintButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))).addContainerGap()));
        leaguePanelLayout.setVerticalGroup(leaguePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(swimmerScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE).addGroup(leaguePanelLayout.createSequentialGroup().addComponent(jLabel6).addGap(1, 1, 1).addComponent(leagueSortByNumber).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(leagueSortByName).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(leagueSortByTeam).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(leagueSortByLevel).addGap(18, 18, 18).addComponent(jLabel7).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(leagueTeamCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addGap(18, 18, 18).addComponent(leaguePrintButton).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(numMeetsPrintButton).addContainerGap(268, Short.MAX_VALUE)));
        tabPane.addTab("League", leaguePanel);
        getContentPane().add(tabPane, java.awt.BorderLayout.PAGE_START);
        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);
        jLabel1.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel1.setText("Setup");
        jLabel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.add(jLabel1);
        jToolBar1.add(setupProgress);
        jSeparator1.setPreferredSize(new java.awt.Dimension(12, 0));
        jToolBar1.add(jSeparator1);
        jLabel2.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel2.setText("Nov. Figures");
        jLabel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.add(jLabel2);
        jToolBar1.add(novFiguresProgress);
        jSeparator2.setPreferredSize(new java.awt.Dimension(12, 0));
        jToolBar1.add(jSeparator2);
        jLabel3.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel3.setText("Int. Figures");
        jLabel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.add(jLabel3);
        jToolBar1.add(intFiguresProgress);
        jSeparator3.setPreferredSize(new java.awt.Dimension(12, 0));
        jToolBar1.add(jSeparator3);
        jLabel4.setFont(new java.awt.Font("Tahoma", 0, 14));
        jLabel4.setText("Routines");
        jLabel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jToolBar1.add(jLabel4);
        jToolBar1.add(routinesProgress);
        getContentPane().add(jToolBar1, java.awt.BorderLayout.SOUTH);
        pack();
    }

    private void reportIntRoutineLabelsActionPerformed(java.awt.event.ActionEvent evt) {
        Integer skipLabels = getSkipLabels();
        if (skipLabels == null) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RoutinesController.showRoutinesLabelsReport(meet, skipLabels, false, true);
        setCursor(Cursor.getDefaultCursor());
    }

    private void tabPaneStateChanged(javax.swing.event.ChangeEvent evt) {
        logger.info("Tab selected = " + Tab.values()[tabPane.getSelectedIndex()]);
        if (tabPane.getSelectedIndex() == Tab.FIGURES.ordinal()) {
            swimmerSearchPanel.focus();
        }
    }

    private void leagueSortByLevelfiguresOrderSortByNameActionPerformed(java.awt.event.ActionEvent evt) {
        updateLeagueList();
    }

    private void leagueTeamComboActionPerformed(java.awt.event.ActionEvent evt) {
        updateLeagueList();
    }

    private void leagueSortByTeamfiguresOrderSortByNameActionPerformed(java.awt.event.ActionEvent evt) {
        updateLeagueList();
    }

    private void leagueSortByNamefiguresOrderSortByNameActionPerformed(java.awt.event.ActionEvent evt) {
        updateLeagueList();
    }

    private void leagueSortByNumberfiguresOrderSortByNumberActionPerformed(java.awt.event.ActionEvent evt) {
        updateLeagueList();
    }

    private void leaguePrintButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/Roster.jasper"));
            JRDataSource data = new JRTableModelDataSource(swimmerTable.getModel());
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("Title", "Roster");
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
    }

    private void reportAllRoutineLabelsActionPerformed(java.awt.event.ActionEvent evt) {
        Integer skipLabels = getSkipLabels();
        if (skipLabels == null) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RoutinesController.showRoutinesLabelsReport(meet, skipLabels, true, true);
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportAllRoutinesActionPerformed(java.awt.event.ActionEvent evt) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RoutinesController.showRoutinesReport(meet, true, true);
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportIntRoutinesActionPerformed(java.awt.event.ActionEvent evt) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RoutinesController.showRoutinesReport(meet, false, true);
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportNovRoutineLabelsActionPerformed(java.awt.event.ActionEvent evt) {
        Integer skipLabels = getSkipLabels();
        if (skipLabels == null) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RoutinesController.showRoutinesLabelsReport(meet, skipLabels, true, false);
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportNovRoutinesActionPerformed(java.awt.event.ActionEvent evt) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        RoutinesController.showRoutinesReport(meet, true, false);
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportIntFigureLabelsActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, false) < 100) {
            JOptionPane.showMessageDialog(this, "Intermediate figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        Integer skipLabels = getSkipLabels();
        if (skipLabels == null) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresLabels.jasper"));
            List<FiguresLabel> figuresLabels = new LinkedList<FiguresLabel>();
            for (int i = 0; i < skipLabels; i++) {
                figuresLabels.add(null);
            }
            figuresLabels.addAll(ScoreController.generateFiguresLabels(meet, false));
            JRDataSource data = new JRBeanCollectionDataSource(figuresLabels);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportNovFigureLabelsActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, true) < 100) {
            JOptionPane.showMessageDialog(this, "Novice figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        Integer skipLabels = getSkipLabels();
        if (skipLabels == null) {
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresLabels.jasper"));
            List<FiguresLabel> figuresLabels = new LinkedList<FiguresLabel>();
            for (int i = 0; i < skipLabels; i++) {
                figuresLabels.add(null);
            }
            figuresLabels.addAll(ScoreController.generateFiguresLabels(meet, true));
            JRDataSource data = new JRBeanCollectionDataSource(figuresLabels);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportTeamResultsActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        Map<Team, BigDecimal> points = meet.getPointsMap();
        if (points == null || meet.hasCalcErrors()) {
            showMeetCalcErrors();
            if (points == null) {
                return;
            }
        }
        Map<BigDecimal, TeamPoints> pointsMap = new TreeMap<BigDecimal, TeamPoints>().descendingMap();
        for (Entry<Team, BigDecimal> pointsEntry : points.entrySet()) {
            TeamPoints tp = new TeamPoints(pointsEntry.getKey(), pointsEntry.getValue());
            pointsMap.put(tp.getPoints(), tp);
        }
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/TeamResults.jasper"));
            JRDataSource data = new JRBeanCollectionDataSource(pointsMap.values());
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
    }

    private void reportIntStationActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, false) < 100) {
            JOptionPane.showMessageDialog(this, "Intermediate figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresStation.jasper"));
            JRDataSource data = new JRBeanCollectionDataSource(ScoreController.generateStationResults(meet, false));
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportIntMeetSheetActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, false) < 100) {
            JOptionPane.showMessageDialog(this, "Intermediate figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresMeetSheet.jasper"));
            JRDataSource data = new JRBeanCollectionDataSource(ScoreController.generateFiguresMeetSheets(meet, false));
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportIntermediateFiguresActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, false) < 100) {
            JOptionPane.showMessageDialog(this, "Intermediate figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        viewFiguresResultsReport(ScoreController.findAllFiguresParticipantByMeetAndDivision(meet, false), "Meet Results - Intermediate Figures");
    }

    private void reportNoviceStationActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, true) < 100) {
            JOptionPane.showMessageDialog(this, "Novice figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresStation.jasper"));
            JRDataSource data = new JRBeanCollectionDataSource(ScoreController.generateStationResults(meet, true));
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportNovMeetSheetActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, true) < 100) {
            JOptionPane.showMessageDialog(this, "Novice figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresMeetSheet.jasper"));
            JRDataSource data = new JRBeanCollectionDataSource(ScoreController.generateFiguresMeetSheets(meet, true));
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
        setCursor(Cursor.getDefaultCursor());
    }

    private void reportNoviceFiguresActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.calculateMeetResultsIfNeeded(meet);
        if (ScoreController.percentCompleteFigures(meet, true) < 100) {
            JOptionPane.showMessageDialog(this, "Novice figures scores are not complete.", "Warning", JOptionPane.WARNING_MESSAGE);
        }
        viewFiguresResultsReport(ScoreController.findAllFiguresParticipantByMeetAndDivision(meet, true), "Meet Results - Novice Figures");
    }

    private void clearFigureScoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int yesno = JOptionPane.showConfirmDialog(figureScorePanel, "Are you sure that you want to clear all figures scores for swimmer #" + figureScorePanel.getFiguresParticipant().getFigureOrder() + "?", "Confirm Clear", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (yesno == JOptionPane.YES_OPTION) {
            if (controller.saveFigureScores(figureScorePanel.getFiguresParticipant(), null)) {
                doFiguresParticipantSearch(figureScorePanel.getFiguresParticipant().getFigureOrder());
                swimmerSearchPanel.focus();
            } else {
                JOptionPane.showMessageDialog(figureScorePanel, "Clear failed.  Restart program if this error persists.", "Error Clearing", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFigureScoreButtonKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            saveFigureScoreButton.doClick();
        }
    }

    private void saveFigureScoreButtonActionPerformed(java.awt.event.ActionEvent evt) {
        logger.info("Save figures score start.");
        if (figureScorePanel.scoresValid()) {
            if (controller.saveFigureScores(figureScorePanel.getFiguresParticipant(), figureScorePanel.getFigureScores())) {
                doFiguresParticipantSearch(figureScorePanel.getFiguresParticipant().getFigureOrder());
                swimmerSearchPanel.focus();
            } else {
                JOptionPane.showMessageDialog(figureScorePanel, "Check scores and try again.  Restart program if this error persists.", "Error Saving", JOptionPane.ERROR_MESSAGE);
                logger.warn("saveFigureScores failed data=" + MeetManager.getFiguresParticipantExport(figureScorePanel.getFiguresParticipant()));
            }
        } else {
            JOptionPane.showMessageDialog(figureScorePanel, "Check scores and try again.", "Invalid Score", JOptionPane.WARNING_MESSAGE);
            logger.warn("scoresValid is false data=" + MeetManager.getFiguresParticipantExport(figureScorePanel.getFiguresParticipant()));
        }
        logger.info("Save figures score complete.");
    }

    private void figureScorePanelPropertyChange(java.beans.PropertyChangeEvent evt) {
        if ("FocusSave".equals(evt.getPropertyName())) {
            saveFigureScoreButton.requestFocusInWindow();
        }
        if ("SwimmerSearch".equals(evt.getPropertyName())) {
            swimmerSearchPanel.requestFocus();
        }
    }

    private void figuresOrderPrintButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/FiguresOrder.jasper"));
            FiguresParticipantsTableModel fptm = (FiguresParticipantsTableModel) figureOrderTable.getModel();
            JRDataSource data = new JRTableModelDataSource(fptm);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("MeetDate", meet.getMeetDate());
            params.put("MeetName", meet.getName());
            if (figureOrderSortByNumber.isSelected()) {
                ArrayList<String> breaks = new ArrayList<String>();
                if (figuresOrderLinesCheckbox.isSelected()) {
                    int startInt = -1;
                    for (int i = 0; i < fptm.getRowCount(); i++) {
                        String s = (String) fptm.getValueAt(i, FiguresParticipantsTableModel.LEVEL_COL);
                        if (s.startsWith("I")) {
                            startInt = i;
                            break;
                        }
                    }
                    int numGroups = ((meet.getType() == 'R') ? 2 : 4);
                    if (startInt > 0) {
                        double groupSize = (double) startInt / (double) numGroups;
                        for (int i = 1; i < numGroups; i++) {
                            int breakAt = (int) Math.ceil(groupSize * (double) i) - 1;
                            if (breakAt < startInt) {
                                breaks.add((String) fptm.getValueAt(breakAt, FiguresParticipantsTableModel.FIGURES_ORDER_COL));
                            }
                        }
                        breaks.add((String) fptm.getValueAt(startInt - 1, FiguresParticipantsTableModel.FIGURES_ORDER_COL));
                    }
                    if (startInt > -1) {
                        double groupSize = (double) (fptm.getRowCount() - startInt) / (double) numGroups;
                        for (int i = 1; i < numGroups; i++) {
                            int breakAt = (int) Math.ceil(groupSize * (double) i) + startInt - 1;
                            if (breakAt < fptm.getRowCount()) {
                                breaks.add((String) fptm.getValueAt(breakAt, FiguresParticipantsTableModel.FIGURES_ORDER_COL));
                            }
                        }
                    }
                }
                params.put("Breaks", breaks);
            }
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage(), ex);
        }
    }

    private void figuresOrderSortByNameActionPerformed(java.awt.event.ActionEvent evt) {
        updateFiguresOrderList();
    }

    private void figuresOrderSortByNumberActionPerformed(java.awt.event.ActionEvent evt) {
        updateFiguresOrderList();
    }

    private void generateRandomFiguresOrderButtonActionPerformed(java.awt.event.ActionEvent evt) {
        logger.info("Randomize figures order started.");
        if (meet.getFiguresOrderGenerated()) {
            int confirm = JOptionPane.showConfirmDialog(this, "You have already generated the random meet order.  Shall I do it again and overwrite the current ordering?", "Warning", JOptionPane.OK_CANCEL_OPTION);
            if (confirm != JOptionPane.OK_OPTION) {
                logger.info("Randomize figures order cancelled.");
                return;
            }
            logger.info("Randomize figures order override.");
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        controller.generateRandomFiguresOrder(meet);
        updateStatus();
        selectTab(Tab.FIGURES_ORDER);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        logger.info("Randomize figures order complete.");
    }

    private void saveSwimmersButtonActionPerformed(java.awt.event.ActionEvent evt) {
        logger.info("Save swimmers start.");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        ArrayList<Swimmer> participatingSwimmers = new ArrayList<Swimmer>();
        ArrayList<Swimmer> removedSwimmers = new ArrayList<Swimmer>();
        for (int i = 0; i < teamTabs.getTabCount(); i++) {
            SwimmerSelectionPanel ssp = (SwimmerSelectionPanel) teamTabs.getComponentAt(i);
            participatingSwimmers.addAll(ssp.getSelectedSwimmers());
            removedSwimmers.addAll(ssp.getRemovedSwimmers());
        }
        boolean okToSave = true;
        for (Swimmer swimmer : removedSwimmers) {
            FiguresParticipant fp = ScoreController.findFiguresParticipant(meet, swimmer);
            if (fp != null && fp.getFiguresScores().size() > 0) {
                okToSave = false;
                JOptionPane.showMessageDialog(this, "Swimmer #" + swimmer.getLeagueNum() + " with meet #" + fp.getFigureOrder() + " has scores.  Please clear those scores and try again.", "Cannot remove swimmer", JOptionPane.ERROR_MESSAGE);
            }
        }
        if (okToSave) {
            controller.updateFiguresSwimmers(meet, participatingSwimmers);
            meet.clearPoints();
            controller.saveMeet(meet);
            updateStatus();
        }
        logger.info("Save swimmers complete.");
        updateSwimmerTab();
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void numMeetsPrintButtonActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(getClass().getResourceAsStream("/org/aquastarz/score/report/NumMeets.jasper"));
            List<NumMeets> numMeetsList = NumMeets.generateList(getLeagueTabSwimmerList(), ScoreApp.getCurrentSeason());
            JRDataSource data = new JRBeanCollectionDataSource(numMeetsList);
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("Title", "Count of Meets Attended");
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, data);
            JasperViewer.viewReport(jasperPrint, false);
        } catch (Exception ex) {
            logger.error("Could not create the report.\n" + ex.getLocalizedMessage());
        }
    }

    private void exportMeetDataButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ScoreController.exportMeetData(meet, this);
    }

    private void compareMeetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Open Meet data file");
        jfc.setFileFilter(new FileNameExtensionFilter("csv file", "csv"));
        int ret = jfc.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Meet meet2 = MeetManager.readMeet(jfc.getSelectedFile());
            List<String> errors = MeetManager.compareMeets(meet, meet2);
            setCursor(Cursor.getDefaultCursor());
            if (errors.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Data matches.");
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("<html><p>Errors found:</p><p>");
                for (String s : errors) {
                    sb.append(s).append("<br>");
                }
                sb.append("</p></html>");
                JScrollPane scrollPane = new JScrollPane(new JLabel(sb.toString()));
                scrollPane.setPreferredSize(new Dimension(600, 100));
                Object message = scrollPane;
                JOptionPane.showMessageDialog(this, message, "Comparison Errors", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private javax.swing.JButton clearFigureScoreButton;

    private javax.swing.JButton compareMeetButton;

    private javax.swing.JButton exportMeetDataButton;

    private javax.swing.JScrollPane figureOrderScrollPane;

    private javax.swing.JRadioButton figureOrderSortByName;

    private javax.swing.JRadioButton figureOrderSortByNumber;

    private javax.swing.JTable figureOrderTable;

    private javax.swing.JPanel figureScore;

    private org.aquastarz.score.gui.FigureScorePanel figureScorePanel;

    private javax.swing.JPanel figuresOrder;

    private javax.swing.JCheckBox figuresOrderLinesCheckbox;

    private javax.swing.JButton figuresOrderPrintButton;

    private javax.swing.ButtonGroup figuresOrderSortButtonGroup;

    private javax.swing.JButton generateRandomFiguresOrderButton;

    private javax.swing.JProgressBar intFiguresProgress;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel5;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JLabel jLabel7;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JToolBar.Separator jSeparator1;

    private javax.swing.JToolBar.Separator jSeparator2;

    private javax.swing.JToolBar.Separator jSeparator3;

    private javax.swing.JToolBar jToolBar1;

    private javax.swing.JPanel leaguePanel;

    private javax.swing.JButton leaguePrintButton;

    private javax.swing.ButtonGroup leagueSortButtonGroup;

    private javax.swing.JRadioButton leagueSortByLevel;

    private javax.swing.JRadioButton leagueSortByName;

    private javax.swing.JRadioButton leagueSortByNumber;

    private javax.swing.JRadioButton leagueSortByTeam;

    private javax.swing.JComboBox leagueTeamCombo;

    private org.aquastarz.score.gui.MeetSetupPanel meetSetup;

    private javax.swing.JProgressBar novFiguresProgress;

    private javax.swing.JButton numMeetsPrintButton;

    private javax.swing.JButton reportAllRoutineLabels;

    private javax.swing.JButton reportAllRoutines;

    private javax.swing.JButton reportIntFigureLabels;

    private javax.swing.JButton reportIntMeetSheet;

    private javax.swing.JButton reportIntRoutineLabels;

    private javax.swing.JButton reportIntRoutines;

    private javax.swing.JButton reportIntStation;

    private javax.swing.JButton reportIntermediateFigures;

    private javax.swing.JButton reportNovFigureLabels;

    private javax.swing.JButton reportNovMeetSheet;

    private javax.swing.JButton reportNovRoutineLabels;

    private javax.swing.JButton reportNovRoutines;

    private javax.swing.JButton reportNoviceFigures;

    private javax.swing.JButton reportNoviceStation;

    private javax.swing.JPanel reportPanel;

    private javax.swing.JButton reportTeamResults;

    private org.aquastarz.score.gui.RoutinesPanel routinesPanel;

    private javax.swing.JProgressBar routinesProgress;

    private javax.swing.JButton saveFigureScoreButton;

    private javax.swing.JButton saveSwimmersButton;

    private javax.swing.JProgressBar setupProgress;

    private javax.swing.JScrollPane swimmerScrollPane;

    private org.aquastarz.score.gui.FiguresParticipantSearchPanel swimmerSearchPanel;

    private javax.swing.JTable swimmerTable;

    private javax.swing.JPanel swimmers;

    private javax.swing.JTabbedPane tabPane;

    private javax.swing.JTabbedPane teamTabs;
}
