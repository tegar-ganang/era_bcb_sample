package imp.gui;

import imp.ImproVisor;
import imp.com.CommandManager;
import imp.com.PlayScoreCommand;
import imp.data.*;
import imp.util.ErrorLog;
import java.util.ArrayList;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import polya.Polylist;

/**
 * I think there was originally a form for this, but it seems to have gotten
 * lost some how.
 * 
 * @author  Jim Herold
 */
public class ExtractionEditorOld extends javax.swing.JDialog {

    Notate parent;

    StyleEditor styleGenerator;

    CommandManager cm;

    int type;

    RepresentativeBassRules repBassRules;

    ArrayList<RepresentativeBassRules.BassPattern> rawBassRules;

    ArrayList<RepresentativeBassRules.BassPattern> selectedBassRules;

    RepresentativeDrumRules repDrumRules;

    ArrayList<RepresentativeDrumRules.DrumPattern> rawDrumRules;

    ArrayList<RepresentativeDrumRules.DrumPattern> selectedDrumRules;

    RepresentativeChordRules repChordRules;

    ArrayList<RepresentativeChordRules.ChordPattern> rawChordRules;

    ArrayList<RepresentativeChordRules.ChordPattern> selectedChordRules;

    /**
     * minimum duration (in slots) for a note not to be counted as a rest.
     */
    private int minDuration = 0;

    public static final int BASS = 0;

    public static final int DRUM = 1;

    public static final int CHORD = 2;

    public static final String PREVIEWCHORD = "CM";

    /** Creates new form BeanForm */
    public ExtractionEditorOld(java.awt.Frame parent, boolean modal, StyleEditor p, CommandManager cm, int type) {
        this(parent, modal, p, cm, type, 0);
    }

    public ExtractionEditorOld(java.awt.Frame parent, boolean modal, StyleEditor p, CommandManager cm, int type, int minDuration) {
        super(parent, modal);
        this.styleGenerator = p;
        this.parent = p.getNotate();
        this.cm = cm;
        this.type = type;
        this.minDuration = minDuration;
        initComponents();
        setSize(900, 425);
        SpinnerModel model = new SpinnerNumberModel(1, 1, 100, 1);
        numberOfClustersSpinner.setModel(model);
        setPotentialParts();
        switch(type) {
            case BASS:
                setTitle("Bass Extraction");
                setBassDefaults();
                doubleDrumLength.setVisible(false);
                repBassRules = MIDIBeast.repBassRules;
                addBassSelectedRules();
                addBassRawRules();
                break;
            case DRUM:
                setTitle("Drum Extraction");
                setDrumDefaults();
                repDrumRules = MIDIBeast.repDrumRules;
                addDrumRawRules();
                addDrumSelectedRules();
                break;
            case CHORD:
                setTitle("Chord Extraction");
                setChordDefaults();
                doubleDrumLength.setVisible(false);
                repChordRules = MIDIBeast.repChordRules;
                addChordRawRules();
                addChordSelectedRules();
        }
    }

    public void setPotentialParts() {
        ArrayList<String> potentialInstruments = new ArrayList<String>();
        for (int i = 0; i < MIDIBeast.allParts.size(); i++) {
            if (MIDIBeast.allParts.get(i).getChannel() == 9) potentialInstruments.add("DRUMS"); else potentialInstruments.add(MIDIBeast.getInstrumentName(MIDIBeast.allParts.get(i).getInstrument()));
        }
        potentialInstrumentsJList.setListData(potentialInstruments.toArray());
        potentialInstrumentsJList.setSelectedIndex(0);
    }

    public void setBassDefaults() {
        startBeatTextField.setText(Double.toString(Math.round(MIDIBeast.bassPart.getPhrase(0).getStartTime())));
        endBeatTextField.setText(Double.toString(Math.round(MIDIBeast.bassPart.getPhrase(0).getEndTime())));
    }

    public void setDrumDefaults() {
        startBeatTextField.setText(Double.toString(Math.round(MIDIBeast.drumPart.getPhrase(0).getStartTime())));
        endBeatTextField.setText(Double.toString(Math.round(MIDIBeast.drumPart.getPhrase(0).getEndTime())));
    }

    public void setChordDefaults() {
        startBeatTextField.setText(Double.toString(Math.round(MIDIBeast.chordPart.getPhrase(0).getStartTime())));
        endBeatTextField.setText(Double.toString(Math.round(MIDIBeast.chordPart.getPhrase(0).getEndTime())));
    }

    public void addBassRawRules() {
        ArrayList<RepresentativeBassRules.Section> sections = repBassRules.getSections();
        ArrayList<String> rawRules = new ArrayList<String>();
        for (int i = 0; i < sections.size(); i++) {
            RepresentativeBassRules.Section currentSection = sections.get(i);
            rawRules.add("Patterns of length: " + currentSection.getSlotCount());
            ArrayList<RepresentativeBassRules.Cluster> clusters = currentSection.getClusters();
            for (int j = 0; j < clusters.size(); j++) {
                RepresentativeBassRules.Cluster currentCluster = clusters.get(j);
                rawRules.add("     Cluster (" + j + ")");
                for (int k = 0; k < currentCluster.size(); k++) rawRules.add("          " + currentCluster.getStringRule(k));
            }
        }
        rawRulesJList.setListData(rawRules.toArray());
        rawRulesJList.setSelectedIndex(0);
    }

    public void addDrumRawRules() {
        ArrayList<RepresentativeDrumRules.Cluster> clusters = repDrumRules.getClusters();
        ArrayList<String> rawRules = new ArrayList<String>();
        for (int i = 1; i < clusters.size(); i++) {
            RepresentativeDrumRules.Cluster cluster = clusters.get(i);
            String[] clusterRules = cluster.getRules();
            rawRules.add(clusterRules[0]);
            for (int j = 1; j < clusterRules.length; j++) rawRules.add(clusterRules[j] + "(weight 1))");
        }
        rawRules.add("Duplicates");
        ArrayList<String> duplicates = MIDIBeast.repDrumRules.getDuplicates();
        for (int i = 0; i < duplicates.size(); i++) rawRules.add(duplicates.get(i) + "(weight 1))");
        rawRulesJList.setListData(rawRules.toArray());
        rawRulesJList.setSelectedIndex(0);
    }

    public void addChordRawRules() {
        ArrayList<RepresentativeChordRules.Section> sections = repChordRules.getSections();
        ArrayList<String> rawRules = new ArrayList<String>();
        for (int i = 0; i < sections.size(); i++) {
            RepresentativeChordRules.Section currentSection = sections.get(i);
            rawRules.add("Patterns of length: " + currentSection.getSlotCount());
            ArrayList<RepresentativeChordRules.Cluster> clusters = currentSection.getClusters();
            for (int j = 0; j < clusters.size(); j++) {
                RepresentativeChordRules.Cluster currentCluster = clusters.get(j);
                rawRules.add("    Cluster(" + j + ")");
                for (int k = 0; k < currentCluster.size(); k++) rawRules.add("        " + currentCluster.getStringRule(k));
            }
        }
        ArrayList<String> duplicates = repChordRules.getDuplicates();
        if (duplicates.size() > 0) {
            rawRules.add("Duplicates");
            for (int i = 0; i < duplicates.size(); i++) rawRules.add("    " + duplicates.get(i));
        } else rawRules.add("No Duplicates Found");
        rawRulesJList.setListData(rawRules.toArray());
        rawRulesJList.setSelectedIndex(0);
    }

    public void addBassSelectedRules() {
        selectedBassRules = repBassRules.getBassRules();
        selectedRulesJList.setListData(selectedBassRules.toArray());
        selectedRulesJList.setSelectedIndex(0);
    }

    public void addDrumSelectedRules() {
        selectedDrumRules = repDrumRules.getRepresentativePatterns();
        selectedRulesJList.setListData(selectedDrumRules.toArray());
        selectedRulesJList.setSelectedIndex(0);
    }

    public void addChordSelectedRules() {
        selectedChordRules = repChordRules.getChordRules();
        selectedRulesJList.setListData(selectedChordRules.toArray());
        selectedRulesJList.setSelectedIndex(0);
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        errorDialog = new javax.swing.JDialog();
        errorMessage = new javax.swing.JLabel();
        errorButton = new javax.swing.JButton();
        jFormattedTextField1 = new javax.swing.JFormattedTextField();
        rawRulesLabel = new javax.swing.JLabel();
        rawRulesScrollPane = new javax.swing.JScrollPane();
        rawRulesJList = new javax.swing.JList();
        addRuleButton = new javax.swing.JButton();
        playRawRuleButton = new javax.swing.JButton();
        selectedRulesLabel = new javax.swing.JLabel();
        selectedRulesScrollPanel = new javax.swing.JScrollPane();
        selectedRulesJList = new javax.swing.JList();
        removeRuleButton = new javax.swing.JButton();
        playSelectedRuleButton = new javax.swing.JButton();
        generationOptionsPanel = new javax.swing.JPanel();
        numberOfClustersLabel = new javax.swing.JLabel();
        startLabel = new javax.swing.JLabel();
        startBeatTextField = new javax.swing.JTextField();
        endLabel = new javax.swing.JLabel();
        endBeatTextField = new javax.swing.JTextField();
        numberOfClustersSpinner = new javax.swing.JSpinner();
        regenerateButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        potentialRulesScrollPane = new javax.swing.JScrollPane();
        potentialInstrumentsJList = new javax.swing.JList();
        doubleDrumLength = new javax.swing.JCheckBox();
        errorDialog.getContentPane().setLayout(new java.awt.GridBagLayout());
        errorDialog.setTitle("Error");
        errorDialog.setBackground(java.awt.Color.white);
        errorMessage.setForeground(new java.awt.Color(255, 0, 51));
        errorMessage.setText("jLabel1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        errorDialog.getContentPane().add(errorMessage, gridBagConstraints);
        errorButton.setText("OK");
        errorButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                errorButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(36, 7, 5, 7);
        errorDialog.getContentPane().add(errorButton, gridBagConstraints);
        jFormattedTextField1.setText("jFormattedTextField1");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        rawRulesLabel.setText("Raw Rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(11, 5, 0, 0);
        getContentPane().add(rawRulesLabel, gridBagConstraints);
        rawRulesScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        rawRulesScrollPane.setMaximumSize(new java.awt.Dimension(220, 150));
        rawRulesScrollPane.setMinimumSize(new java.awt.Dimension(220, 150));
        rawRulesScrollPane.setPreferredSize(new java.awt.Dimension(220, 150));
        rawRulesJList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        rawRulesJList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        rawRulesJList.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rawRulesJListMouseClicked(evt);
            }
        });
        rawRulesScrollPane.setViewportView(rawRulesJList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 120;
        gridBagConstraints.ipady = 110;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        getContentPane().add(rawRulesScrollPane, gridBagConstraints);
        addRuleButton.setText("Add Rule");
        addRuleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addRuleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(11, 0, 69, 0);
        getContentPane().add(addRuleButton, gridBagConstraints);
        playRawRuleButton.setText("Play Rule");
        playRawRuleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playRawRuleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        getContentPane().add(playRawRuleButton, gridBagConstraints);
        selectedRulesLabel.setText("Selected Rules");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 0, 0);
        getContentPane().add(selectedRulesLabel, gridBagConstraints);
        selectedRulesScrollPanel.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        selectedRulesScrollPanel.setMaximumSize(new java.awt.Dimension(220, 150));
        selectedRulesScrollPanel.setMinimumSize(new java.awt.Dimension(220, 150));
        selectedRulesScrollPanel.setPreferredSize(new java.awt.Dimension(220, 150));
        selectedRulesJList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        selectedRulesJList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        selectedRulesJList.addMouseListener(new java.awt.event.MouseAdapter() {

            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectedRulesJListMouseClicked(evt);
            }
        });
        selectedRulesScrollPanel.setViewportView(selectedRulesJList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 120;
        gridBagConstraints.ipady = 110;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        getContentPane().add(selectedRulesScrollPanel, gridBagConstraints);
        removeRuleButton.setText("Remove Rule");
        removeRuleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeRuleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        getContentPane().add(removeRuleButton, gridBagConstraints);
        playSelectedRuleButton.setText("Play Rule");
        playSelectedRuleButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playSelectedRuleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        getContentPane().add(playSelectedRuleButton, gridBagConstraints);
        generationOptionsPanel.setLayout(new java.awt.GridBagLayout());
        generationOptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Re-Generation Options"));
        generationOptionsPanel.setMaximumSize(new java.awt.Dimension(237, 101));
        numberOfClustersLabel.setText("Maximum Number Of Clusters:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        generationOptionsPanel.add(numberOfClustersLabel, gridBagConstraints);
        startLabel.setText("Start Beat:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        generationOptionsPanel.add(startLabel, gridBagConstraints);
        startBeatTextField.setText("jTextField1");
        startBeatTextField.setMaximumSize(new java.awt.Dimension(50, 20));
        startBeatTextField.setMinimumSize(new java.awt.Dimension(50, 20));
        startBeatTextField.setPreferredSize(new java.awt.Dimension(50, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        generationOptionsPanel.add(startBeatTextField, gridBagConstraints);
        endLabel.setText("End Beat:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
        generationOptionsPanel.add(endLabel, gridBagConstraints);
        endBeatTextField.setText("jTextField2");
        endBeatTextField.setMaximumSize(new java.awt.Dimension(50, 20));
        endBeatTextField.setMinimumSize(new java.awt.Dimension(50, 20));
        endBeatTextField.setPreferredSize(new java.awt.Dimension(50, 20));
        endBeatTextField.setScrollOffset(-5);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        generationOptionsPanel.add(endBeatTextField, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.insets = new java.awt.Insets(0, 23, 5, 10);
        generationOptionsPanel.add(numberOfClustersSpinner, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 5);
        getContentPane().add(generationOptionsPanel, gridBagConstraints);
        regenerateButton.setText("Re-Generate");
        regenerateButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                regenerateButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        getContentPane().add(regenerateButton, gridBagConstraints);
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 79);
        getContentPane().add(okButton, gridBagConstraints);
        jButton2.setText("Cancel");
        jButton2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 50, 12, 0);
        getContentPane().add(jButton2, gridBagConstraints);
        jPanel1.setLayout(new java.awt.GridBagLayout());
        potentialRulesScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Part"));
        potentialRulesScrollPane.setMaximumSize(new java.awt.Dimension(237, 100));
        potentialRulesScrollPane.setMinimumSize(new java.awt.Dimension(237, 100));
        potentialRulesScrollPane.setPreferredSize(new java.awt.Dimension(237, 100));
        potentialInstrumentsJList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        potentialInstrumentsJList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        potentialRulesScrollPane.setViewportView(potentialInstrumentsJList);
        jPanel1.add(potentialRulesScrollPane, new java.awt.GridBagConstraints());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(77, 0, 5, 5);
        getContentPane().add(jPanel1, gridBagConstraints);
        doubleDrumLength.setText(" Double Drum Length");
        doubleDrumLength.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        doubleDrumLength.setMargin(new java.awt.Insets(0, 0, 0, 0));
        doubleDrumLength.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doubleDrumLengthActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        getContentPane().add(doubleDrumLength, gridBagConstraints);
    }

    private void doubleDrumLengthActionPerformed(java.awt.event.ActionEvent evt) {
        if (doubleDrumLength.isSelected()) MIDIBeast.drumMeasureSize *= 2; else MIDIBeast.drumMeasureSize = MIDIBeast.slotsPerMeasure;
    }

    private void selectedRulesJListMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() != 2) return;
        playSelectedRule();
    }

    private void rawRulesJListMouseClicked(java.awt.event.MouseEvent evt) {
        if (evt.getClickCount() != 2) return;
        playRawRule();
    }

    private void errorButtonActionPerformed(java.awt.event.ActionEvent evt) {
        errorDialog.setVisible(false);
    }

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
        this.setVisible(false);
    }

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {
        switch(type) {
            case BASS:
                MIDIBeast.selectedBassRules = selectedBassRules;
                styleGenerator.loadBassPatterns(MIDIBeast.repBassRules.getBassRules());
                break;
            case DRUM:
                MIDIBeast.selectedDrumRules = selectedDrumRules;
                styleGenerator.loadDrumPatterns(MIDIBeast.repDrumRules.getRepresentativePatterns());
                break;
            case CHORD:
                MIDIBeast.selectedChordRules = selectedChordRules;
                styleGenerator.loadChordPatterns(MIDIBeast.repChordRules.getChordRules());
                break;
        }
        this.setVisible(false);
    }

    private void regenerateButtonActionPerformed(java.awt.event.ActionEvent evt) {
        checkForAndThrowErrors();
        double endBeat = Double.parseDouble(endBeatTextField.getText());
        double startBeat = Double.parseDouble(startBeatTextField.getText());
        Integer maxNumberOfClusters = (Integer) numberOfClustersSpinner.getValue();
        switch(type) {
            case BASS:
                int selectedBassIndex = potentialInstrumentsJList.getSelectedIndex();
                jm.music.data.Part selectedBassPart = MIDIBeast.allParts.get(selectedBassIndex);
                MIDIBeast.repBassRules = new RepresentativeBassRules(startBeat, endBeat, maxNumberOfClusters, selectedBassPart);
                repBassRules = MIDIBeast.repBassRules;
                addBassRawRules();
                addBassSelectedRules();
                break;
            case DRUM:
                int selectedDrumIndex = potentialInstrumentsJList.getSelectedIndex();
                jm.music.data.Part selectedDrumPart = MIDIBeast.allParts.get(selectedDrumIndex);
                MIDIBeast.repDrumRules = new RepresentativeDrumRules(startBeat, endBeat, maxNumberOfClusters, selectedDrumPart);
                repDrumRules = MIDIBeast.repDrumRules;
                addDrumRawRules();
                addDrumSelectedRules();
                break;
            case CHORD:
                int selectedChordIndex = potentialInstrumentsJList.getSelectedIndex();
                jm.music.data.Part selectedChordPart = MIDIBeast.allParts.get(selectedChordIndex);
                MIDIBeast.repChordRules = new RepresentativeChordRules(startBeat, endBeat, maxNumberOfClusters, selectedChordPart, minDuration);
                repChordRules = MIDIBeast.repChordRules;
                addChordRawRules();
                addChordSelectedRules();
                break;
        }
    }

    private void checkForAndThrowErrors() {
        double endBeat;
        double startBeat;
        try {
            endBeat = Double.parseDouble(endBeatTextField.getText());
            startBeat = Double.parseDouble(startBeatTextField.getText());
        } catch (Exception e) {
            errorMessage.setText("ERROR: Malformed Start/End Beat.");
            errorDialog.setSize(250, 200);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
            return;
        }
        if (endBeat < 0 || startBeat < 0) {
            errorMessage.setText("ERROR: Start/End Beats must be positive.");
            errorDialog.setSize(250, 200);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
            return;
        } else if (startBeat > endBeat) {
            errorMessage.setText("ERROR: Start beat must be less than end beat.");
            errorDialog.setSize(250, 200);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
            return;
        } else if (endBeat < startBeat) {
            errorMessage.setText("ERROR: End beat must be greater than start beat.");
            errorDialog.setSize(250, 200);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
            return;
        }
    }

    private void playSelectedRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        playSelectedRule();
    }

    public void playSelectedRule() {
        Polylist rule = null;
        int duration = 0;
        switch(type) {
            case BASS:
                try {
                    RepresentativeBassRules.BassPattern selectedBassRule = (RepresentativeBassRules.BassPattern) selectedRulesJList.getSelectedValue();
                    duration = selectedBassRule.getDuration();
                    rule = Notate.parseListFromString(selectedBassRule.toString());
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case DRUM:
                RepresentativeDrumRules.DrumPattern selectedDrumRule = (RepresentativeDrumRules.DrumPattern) selectedRulesJList.getSelectedValue();
                duration = selectedDrumRule.getDuration();
                rule = Notate.parseListFromString(selectedDrumRule.toString());
                break;
            case CHORD:
                RepresentativeChordRules.ChordPattern selectedChordRule = (RepresentativeChordRules.ChordPattern) selectedRulesJList.getSelectedValue();
                duration = selectedChordRule.getDuration();
                rule = Notate.parseListFromString(selectedChordRule.toString());
                break;
        }
        if (rule.isEmpty()) {
            ErrorLog.log(ErrorLog.WARNING, "Internal Error:" + "Extraction Editor: Empty Rule");
            return;
        }
        Style tempStyle = Style.makeStyle(rule);
        ChordPart c = new ChordPart();
        String chord = styleGenerator.getChord();
        boolean muteChord = styleGenerator.isChordMuted();
        c.addChord(chord, new Double(duration).intValue());
        c.setStyle(tempStyle);
        Score s = new Score(4);
        s.setBassVolume(styleGenerator.getVolume());
        if (muteChord) parent.setChordVolume(0); else parent.setChordVolume(styleGenerator.getVolume());
        parent.setDrumVolume(styleGenerator.getVolume());
        s.setTempo(styleGenerator.getTempo());
        s.setChordProg(c);
        parent.cm.execute(new PlayScoreCommand(s, 0, true, parent.getMidiSynth(), ImproVisor.getCurrentWindow(), 0, parent.getTransposition()));
    }

    private void playRawRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        playRawRule();
    }

    public void playRawRule() {
        String incompleteRule = (String) rawRulesJList.getSelectedValue();
        Polylist rule = null;
        int duration = 0;
        int firstParensIndex, lastParensIndex;
        switch(type) {
            case BASS:
                if (!(incompleteRule.trim().charAt(0) == '(')) return;
                firstParensIndex = incompleteRule.indexOf("(");
                lastParensIndex = incompleteRule.lastIndexOf(")");
                incompleteRule = incompleteRule.substring(firstParensIndex + 1, lastParensIndex);
                RepresentativeBassRules.BassPattern selectedBassRule = repBassRules.makeBassPatternObj(incompleteRule, 1);
                duration = selectedBassRule.getDuration();
                rule = Notate.parseListFromString(selectedBassRule.toString());
                break;
            case DRUM:
                if (incompleteRule.charAt(0) == 'C') return;
                rule = Notate.parseListFromString(incompleteRule);
                duration = MIDIBeast.slotsPerMeasure;
                break;
            case CHORD:
                if (!(incompleteRule.trim().charAt(0) == '(')) return;
                firstParensIndex = incompleteRule.indexOf("(");
                lastParensIndex = incompleteRule.lastIndexOf(")");
                incompleteRule = incompleteRule.substring(firstParensIndex + 1, lastParensIndex);
                RepresentativeChordRules.ChordPattern selectedChordRule = repChordRules.makeChordPattern(incompleteRule, 1);
                duration = selectedChordRule.getDuration();
                rule = Notate.parseListFromString(selectedChordRule.toString());
                break;
        }
        if (rule.isEmpty()) {
            ErrorLog.log(ErrorLog.WARNING, "Internal Error:" + "Extraction Editor: Empty Rule");
            return;
        }
        Style tempStyle = Style.makeStyle(rule);
        ChordPart c = new ChordPart();
        String chord = styleGenerator.getChord();
        boolean muteChord = styleGenerator.isChordMuted();
        c.addChord(chord, duration);
        c.setStyle(tempStyle);
        Score s = new Score(4);
        s.setBassVolume(styleGenerator.getVolume());
        if (muteChord) s.setChordVolume(0); else s.setChordVolume(styleGenerator.getVolume());
        s.setDrumVolume(styleGenerator.getVolume());
        s.setTempo(styleGenerator.getTempo());
        s.setChordProg(c);
        parent.cm.execute(new PlayScoreCommand(s, 0, true, parent.getMidiSynth(), ImproVisor.getCurrentWindow(), 0, parent.getTransposition()));
    }

    private void removeRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int indexOfRuleToBeRemoved = selectedRulesJList.getSelectedIndex();
        switch(type) {
            case BASS:
                selectedBassRules.remove(indexOfRuleToBeRemoved);
                selectedRulesJList.setListData(selectedBassRules.toArray());
                break;
            case DRUM:
                selectedDrumRules.remove(indexOfRuleToBeRemoved);
                selectedRulesJList.setListData(selectedDrumRules.toArray());
                break;
            case CHORD:
                selectedChordRules.remove(indexOfRuleToBeRemoved);
                selectedRulesJList.setListData(selectedChordRules.toArray());
                break;
        }
        selectedRulesJList.setSelectedIndex(0);
    }

    private void addRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        String incompleteRule = (String) rawRulesJList.getSelectedValue();
        int firstParensIndex, lastParensIndex;
        switch(type) {
            case BASS:
                if (!(incompleteRule.trim().charAt(0) == '(')) return;
                firstParensIndex = incompleteRule.indexOf("(");
                lastParensIndex = incompleteRule.lastIndexOf(")");
                incompleteRule = incompleteRule.substring(firstParensIndex + 1, lastParensIndex);
                RepresentativeBassRules.BassPattern selectedBassRule = repBassRules.makeBassPatternObj(incompleteRule, 1);
                selectedBassRules.add(selectedBassRule);
                selectedRulesJList.setListData(selectedBassRules.toArray());
                break;
            case DRUM:
                if (incompleteRule.charAt(0) == 'C') return;
                String[] split = incompleteRule.split("\n");
                RepresentativeDrumRules.DrumPattern drumPattern = repDrumRules.makeDrumPattern();
                for (int i = 1; i < split.length - 1; i++) {
                    RepresentativeDrumRules.DrumRule drumRule = repDrumRules.makeDrumRule();
                    int instrumentNumber = Integer.parseInt(split[i].substring(split[i].indexOf('m') + 2, split[i].indexOf('m') + 4));
                    drumRule.setInstrumentNumber(instrumentNumber);
                    int startIndex = split[i].indexOf('m') + 2;
                    int endIndex = split[i].indexOf(')');
                    String elements = split[i].substring(startIndex, endIndex);
                    String[] split2 = elements.split(" ");
                    for (int j = 0; j < split2.length; j++) drumRule.addElement(split2[j]);
                    String weightString = split[split.length - 1];
                    drumPattern.setWeight(1);
                    drumPattern.addRule(drumRule);
                }
                selectedDrumRules.add(drumPattern);
                selectedRulesJList.setListData(selectedDrumRules.toArray());
                break;
            case CHORD:
                if (!(incompleteRule.trim().charAt(0) == '(')) return;
                firstParensIndex = incompleteRule.indexOf("(");
                lastParensIndex = incompleteRule.lastIndexOf(")");
                incompleteRule = incompleteRule.substring(firstParensIndex + 1, lastParensIndex);
                RepresentativeChordRules.ChordPattern selectedChordRule = repChordRules.makeChordPattern(incompleteRule, 1);
                selectedChordRules.add(selectedChordRule);
                selectedRulesJList.setListData(selectedChordRules.toArray());
                break;
        }
    }

    private javax.swing.JButton addRuleButton;

    private javax.swing.JCheckBox doubleDrumLength;

    private javax.swing.JTextField endBeatTextField;

    private javax.swing.JLabel endLabel;

    private javax.swing.JButton errorButton;

    private javax.swing.JDialog errorDialog;

    private javax.swing.JLabel errorMessage;

    private javax.swing.JPanel generationOptionsPanel;

    private javax.swing.JButton jButton2;

    private javax.swing.JFormattedTextField jFormattedTextField1;

    private javax.swing.JPanel jPanel1;

    private javax.swing.JLabel numberOfClustersLabel;

    private javax.swing.JSpinner numberOfClustersSpinner;

    private javax.swing.JButton okButton;

    private javax.swing.JButton playRawRuleButton;

    private javax.swing.JButton playSelectedRuleButton;

    private javax.swing.JList potentialInstrumentsJList;

    private javax.swing.JScrollPane potentialRulesScrollPane;

    private javax.swing.JList rawRulesJList;

    private javax.swing.JLabel rawRulesLabel;

    private javax.swing.JScrollPane rawRulesScrollPane;

    private javax.swing.JButton regenerateButton;

    private javax.swing.JButton removeRuleButton;

    private javax.swing.JList selectedRulesJList;

    private javax.swing.JLabel selectedRulesLabel;

    private javax.swing.JScrollPane selectedRulesScrollPanel;

    private javax.swing.JTextField startBeatTextField;

    private javax.swing.JLabel startLabel;
}
