package imp.gui;

import imp.Constants;
import imp.ImproVisor;
import imp.com.CommandManager;
import imp.com.PlayScoreCommand;
import imp.data.*;
import imp.util.ErrorLog;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import polya.Polylist;

/**
 * @author Robert Keller, from original code by Jim Herold
 * 
 * When there is time, this should be refactored into three separate
 * derived classes. There is no point in having all three lists: bass, chord,
 * and drums in each of three instances of this editor.
 * 
 * Before that, however, use of public access to elements in MIDIBeast should
 * be changed to use proper methods.
 */
public class ExtractionEditor extends javax.swing.JDialog implements Constants {

    public static final int DRUM_CHANNEL = 9;

    Notate notate;

    StyleEditor styleEditor;

    CommandManager cm;

    int type;

    RepresentativeBassRules repBassRules;

    ArrayList<RepresentativeBassRules.BassPattern> selectedBassRules;

    RepresentativeDrumRules repDrumRules;

    ArrayList<RepresentativeDrumRules.DrumPattern> selectedDrumRules;

    RepresentativeChordRules repChordRules;

    ArrayList<RepresentativeChordRules.ChordPattern> selectedChordRules;

    /**
 * Models for the raw and selected JLists
 */
    DefaultListModel rawRulesModel;

    DefaultListModel selectedRulesModel;

    /**
 * minimum duration (in slots) for a note not to be counted as a rest.
 */
    private int minDuration = 0;

    public static final int BASS = 0;

    public static final int DRUM = 1;

    public static final int CHORD = 2;

    public static final String PREVIEWCHORD = "CM";

    /**
 * Creates new form ExtractionEditor
 */
    public ExtractionEditor(java.awt.Frame parent, boolean modal, StyleEditor p, CommandManager cm, int type) {
        this(parent, modal, p, cm, type, 0);
    }

    public ExtractionEditor(java.awt.Frame parent, boolean modal, StyleEditor p, CommandManager cm, int type, int minDuration) {
        super(parent, modal);
        this.styleEditor = p;
        this.notate = p.getNotate();
        this.cm = cm;
        this.type = type;
        this.minDuration = minDuration;
        rawRulesModel = new DefaultListModel();
        selectedRulesModel = new DefaultListModel();
        initComponents();
        initComponents2();
        setSize(900, 425);
        SpinnerModel model = new SpinnerNumberModel(1, 1, 100, 1);
        numberOfClustersSpinner.setModel(model);
        setPotentialParts();
        switch(type) {
            case BASS:
                setBackground(Color.orange);
                tempoVolumeLabel.setBackground(Color.orange);
                setTitle("Bass Extraction from " + styleEditor.getTitle());
                setBassDefaults();
                doubleDrumLength.setVisible(false);
                repBassRules = MIDIBeast.repBassRules;
                setBassSelectedRules();
                setBassRawRules();
                break;
            case CHORD:
                setBackground(Color.green);
                tempoVolumeLabel.setBackground(Color.green);
                setTitle("Chord Extraction from " + styleEditor.getTitle());
                setChordDefaults();
                doubleDrumLength.setVisible(false);
                repChordRules = MIDIBeast.repChordRules;
                setChordRawRules();
                setChordSelectedRules();
                break;
            case DRUM:
                setBackground(Color.yellow);
                tempoVolumeLabel.setBackground(Color.yellow);
                setTitle("Drum Extraction from " + styleEditor.getTitle());
                setDrumDefaults();
                repDrumRules = MIDIBeast.repDrumRules;
                setDrumRawRules();
                setDrumSelectedRules();
                break;
        }
    }

    public void setPotentialParts() {
        ArrayList<String> potentialInstruments = new ArrayList<String>();
        for (int i = 0; i < MIDIBeast.allParts.size(); i++) {
            if (MIDIBeast.allParts.get(i).getChannel() == DRUM_CHANNEL) {
                potentialInstruments.add("DRUMS");
            } else {
                potentialInstruments.add(MIDIBeast.getInstrumentForPart(i));
            }
        }
        potentialInstrumentsJList.setListData(potentialInstruments.toArray());
        potentialInstrumentsJList.setSelectedIndex(0);
    }

    public void setBassDefaults() {
        startBeatTextField.setText(Double.toString(Math.round(MIDIBeast.bassPart.getPhrase(0).getStartTime())));
        endBeatTextField.setText(Double.toString(Math.round(MIDIBeast.bassPart.getPhrase(0).getEndTime())));
    }

    public void setChordDefaults() {
        startBeatTextField.setText(Double.toString(Math.round(MIDIBeast.chordPart.getPhrase(0).getStartTime())));
        endBeatTextField.setText(Double.toString(Math.round(MIDIBeast.chordPart.getPhrase(0).getEndTime())));
    }

    public void setDrumDefaults() {
        startBeatTextField.setText(Double.toString(Math.round(MIDIBeast.drumPart.getPhrase(0).getStartTime())));
        endBeatTextField.setText(Double.toString(Math.round(MIDIBeast.drumPart.getPhrase(0).getEndTime())));
    }

    public void setBassRawRules() {
        rawRulesModel.clear();
        ArrayList<RepresentativeBassRules.Section> sections = repBassRules.getSections();
        ArrayList<Object> rawRules = new ArrayList<Object>();
        for (int i = 0; i < sections.size(); i++) {
            RepresentativeBassRules.Section currentSection = sections.get(i);
            int length = (currentSection.getSlotCount() / BEAT) / BEAT;
            ArrayList<RepresentativeBassRules.Cluster> clusters = currentSection.getClusters();
            for (int j = 0; j < clusters.size(); j++) {
                RepresentativeBassRules.Cluster currentCluster = clusters.get(j);
                rawRules.add("---- Cluster " + j + " of length " + length + " patterns ----");
                for (int k = 0; k < currentCluster.size(); k++) {
                    rawRules.add(repBassRules.makeBassPatternObj(currentCluster.getStringRule(k), 1));
                }
            }
        }
        for (Object rawRule : rawRules) {
            rawRulesModel.addElement(rawRule);
        }
        rawRulesJList.setModel(rawRulesModel);
        rawRulesJList.setSelectedIndex(0);
    }

    public void setChordRawRules() {
        rawRulesModel.clear();
        ArrayList<RepresentativeChordRules.Section> sections = repChordRules.getSections();
        ArrayList<Object> rawRules = new ArrayList<Object>();
        for (int i = 0; i < sections.size(); i++) {
            RepresentativeChordRules.Section currentSection = sections.get(i);
            int length = (currentSection.getSlotCount() / BEAT) / BEAT;
            ArrayList<RepresentativeChordRules.Cluster> clusters = currentSection.getClusters();
            for (int j = 0; j < clusters.size(); j++) {
                RepresentativeChordRules.Cluster currentCluster = clusters.get(j);
                rawRules.add("---- Cluster " + j + " of length " + length + " patterns ----");
                for (int k = 0; k < currentCluster.size(); k++) {
                    rawRules.add(repChordRules.makeChordPattern(currentCluster.getStringRule(k), 1));
                }
            }
        }
        ArrayList<String> duplicates = repChordRules.getDuplicates();
        if (duplicates.size() > 0) {
            rawRules.add("Duplicates:");
            for (int i = 0; i < duplicates.size(); i++) {
                rawRules.add("    " + duplicates.get(i));
            }
        } else {
            rawRules.add("No Duplicates Found");
        }
        for (Object rawRule : rawRules) {
            rawRulesModel.addElement(rawRule);
        }
        rawRulesJList.setModel(rawRulesModel);
        rawRulesJList.setSelectedIndex(0);
    }

    public void setDrumRawRules() {
        rawRulesModel.clear();
        ArrayList<RepresentativeDrumRules.Cluster> clusters = repDrumRules.getClusters();
        ArrayList<Object> rawRules = new ArrayList<Object>();
        for (int i = 1; i < clusters.size(); i++) {
            RepresentativeDrumRules.Cluster cluster = clusters.get(i);
            String[] clusterRules = cluster.getRules();
            rawRules.add(clusterRules[0]);
            for (int j = 1; j < clusterRules.length; j++) {
                rawRules.add(makeDrumPattern(clusterRules[j] + "(weight 1))"));
            }
        }
        rawRules.add("Duplicates");
        ArrayList<String> duplicates = MIDIBeast.repDrumRules.getDuplicates();
        for (int i = 0; i < duplicates.size(); i++) {
            rawRules.add(makeDrumPattern(duplicates.get(i) + "(weight 1))"));
        }
        for (Object rawRule : rawRules) {
            rawRulesModel.addElement(rawRule);
        }
        rawRulesJList.setModel(rawRulesModel);
        rawRulesJList.setSelectedIndex(0);
    }

    private RepresentativeDrumRules.DrumPattern makeDrumPattern(String string) {
        String[] split = string.split("\n");
        RepresentativeDrumRules.DrumPattern drumPattern = repDrumRules.makeDrumPattern();
        for (int i = 1; i < split.length - 1; i++) {
            RepresentativeDrumRules.DrumRule drumRule = repDrumRules.makeDrumRule();
            int instrumentNumber = Integer.parseInt(split[i].substring(split[i].indexOf('m') + 2, split[i].indexOf('m') + 4));
            drumRule.setInstrumentNumber(instrumentNumber);
            int startIndex = split[i].indexOf('m') + 2;
            int endIndex = split[i].indexOf(')');
            String elements = split[i].substring(startIndex, endIndex);
            String[] split2 = elements.split(" ");
            for (int j = 1; j < split2.length; j++) {
                drumRule.addElement(split2[j]);
            }
            String weightString = split[split.length - 1];
            drumPattern.setWeight(1);
            drumPattern.addRule(drumRule);
        }
        return drumPattern;
    }

    public void setBassSelectedRules() {
        selectedRulesModel.clear();
        selectedBassRules = repBassRules.getBassRules();
        for (RepresentativeBassRules.BassPattern selectedRule : selectedBassRules) {
            selectedRulesModel.addElement(selectedRule);
        }
        selectedRulesJList.setModel(selectedRulesModel);
        selectedRulesJList.setSelectedIndex(selectedBassRules.size() - 1);
    }

    public void setChordSelectedRules() {
        selectedRulesModel.clear();
        selectedChordRules = repChordRules.getChordRules();
        for (RepresentativeChordRules.ChordPattern selectedRule : selectedChordRules) {
            selectedRulesModel.addElement(selectedRule);
        }
        selectedRulesJList.setModel(selectedRulesModel);
        selectedRulesJList.setSelectedIndex(selectedChordRules.size() - 1);
    }

    public void setDrumSelectedRules() {
        selectedRulesModel.clear();
        selectedDrumRules = repDrumRules.getRepresentativePatterns();
        for (RepresentativeDrumRules.DrumPattern selectedRule : selectedDrumRules) {
            selectedRulesModel.addElement(selectedRule);
        }
        selectedRulesJList.setModel(selectedRulesModel);
        selectedRulesJList.setSelectedIndex(selectedDrumRules.size() - 1);
    }

    private void initComponents2() {
        java.awt.GridBagConstraints gridBagConstraints;
        errorDialog = new javax.swing.JDialog();
        errorMessage = new javax.swing.JLabel();
        errorButton = new javax.swing.JButton();
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
    }

    private void errorButtonActionPerformed(java.awt.event.ActionEvent evt) {
        errorDialog.setVisible(false);
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
        } else if (startBeat > endBeat) {
            errorMessage.setText("ERROR: Start beat must be less than end beat.");
            errorDialog.setSize(250, 200);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
        } else if (endBeat < startBeat) {
            errorMessage.setText("ERROR: End beat must be greater than start beat.");
            errorDialog.setSize(250, 200);
            errorDialog.setLocationRelativeTo(this);
            errorDialog.setVisible(true);
        }
    }

    /**
 * Plays a selected rule. In this case, the rules themselves are stored
 * in the JList (in contrast to playRawRule()).
 */
    public void playSelectedRule() {
        Polylist rule = null;
        int duration = 0;
        Object selected = selectedRulesJList.getSelectedValue();
        if (selected instanceof RepPattern) {
            switch(type) {
                case BASS:
                    try {
                        RepresentativeBassRules.BassPattern selectedBassRule = (RepresentativeBassRules.BassPattern) selected;
                        duration = selectedBassRule.getDuration();
                        rule = Notate.parseListFromString(selectedBassRule.toString());
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case DRUM:
                    RepresentativeDrumRules.DrumPattern selectedDrumPattern = (RepresentativeDrumRules.DrumPattern) selected;
                    duration = selectedDrumPattern.getDuration();
                    rule = Notate.parseListFromString(selectedDrumPattern.toString());
                    break;
                case CHORD:
                    RepresentativeChordRules.ChordPattern selectedChordRule = (RepresentativeChordRules.ChordPattern) selected;
                    duration = selectedChordRule.getDuration();
                    rule = Notate.parseListFromString(selectedChordRule.toString());
                    break;
            }
            if (rule.isEmpty()) {
                ErrorLog.log(ErrorLog.WARNING, "Internal Error:" + "Extraction Editor: Empty Rule");
                return;
            }
            Style tempStyle = Style.makeStyle(rule);
            tempStyle.setSwing(styleEditor.getSwingValue());
            tempStyle.setAccompanimentSwing(styleEditor.getAccompanimentSwingValue());
            tempStyle.setName("extractionPattern");
            Style.setStyle("extractionPattern", tempStyle);
            notate.reloadStyles();
            ChordPart c = new ChordPart();
            String chord = styleEditor.getChord();
            boolean muteChord = styleEditor.isChordMuted();
            c.addChord(chord, new Double(duration).intValue());
            c.setStyle(tempStyle);
            Score s = new Score(c);
            s.setBassVolume(styleEditor.getVolume());
            if (type == CHORD) {
                notate.setChordVolume(styleEditor.getVolume());
            } else {
                notate.setChordVolume(0);
            }
            notate.setDrumVolume(styleEditor.getVolume());
            s.setTempo(styleEditor.getTempo());
            new PlayScoreCommand(s, 0, true, notate.getMidiSynth(), ImproVisor.getCurrentWindow(), 0, notate.getTransposition()).execute();
        }
    }

    /**
 * Plays a raw rule. In this case, Strings are stored in the JList and
 * rules must be created from them (in contrast to playSelectedRule()).
 */
    public void playRawRule() {
        Object rawOb = rawRulesJList.getSelectedValue();
        if (rawOb instanceof RepPattern) {
            RepPattern repPattern = (RepPattern) rawOb;
            Polylist rule = null;
            int duration = 0;
            switch(type) {
                case BASS:
                    RepresentativeBassRules.BassPattern selectedBassRule = (RepresentativeBassRules.BassPattern) repPattern;
                    duration = selectedBassRule.getDuration();
                    rule = Notate.parseListFromString(selectedBassRule.toString());
                    break;
                case CHORD:
                    RepresentativeChordRules.ChordPattern selectedChordRule = (RepresentativeChordRules.ChordPattern) repPattern;
                    duration = selectedChordRule.getDuration();
                    rule = Notate.parseListFromString(selectedChordRule.toString());
                    break;
                case DRUM:
                    RepresentativeDrumRules.DrumPattern selectedDrumPattern = (RepresentativeDrumRules.DrumPattern) repPattern;
                    duration = selectedDrumPattern.getDuration();
                    rule = Notate.parseListFromString(selectedDrumPattern.toString());
                    break;
            }
            Style tempStyle = Style.makeStyle(rule);
            tempStyle.setSwing(styleEditor.getSwingValue());
            tempStyle.setAccompanimentSwing(styleEditor.getAccompanimentSwingValue());
            tempStyle.setName("extractionPattern");
            Style.setStyle("extractionPattern", tempStyle);
            notate.reloadStyles();
            ChordPart c = new ChordPart();
            String chord = styleEditor.getChord();
            boolean muteChord = styleEditor.isChordMuted();
            c.addChord(chord, new Double(duration).intValue());
            c.setStyle(tempStyle);
            Score s = new Score(c);
            s.setBassVolume(styleEditor.getVolume());
            if (type == CHORD) {
                notate.setChordVolume(styleEditor.getVolume());
            } else {
                notate.setChordVolume(0);
            }
            notate.setDrumVolume(styleEditor.getVolume());
            s.setTempo(styleEditor.getTempo());
            new PlayScoreCommand(s, 0, true, notate.getMidiSynth(), ImproVisor.getCurrentWindow(), 0, notate.getTransposition()).execute();
        }
    }

    /**
 * This method is called from within the constructor to initialize the form.
 * WARNING: Do NOT modify this code. The content of this method is always
 * regenerated by the Form Editor.
 */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        rawPatternsPanel = new javax.swing.JScrollPane();
        rawRulesJList = new javax.swing.JList();
        selectedPatternsPanel = new javax.swing.JScrollPane();
        selectedRulesJList = new javax.swing.JList();
        optionPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        startBeatTextField = new javax.swing.JTextField();
        endBeatTextField = new javax.swing.JTextField();
        numberOfClustersSpinner = new javax.swing.JSpinner();
        reExtractBtn = new javax.swing.JButton();
        doubleDrumLength = new javax.swing.JCheckBox();
        potentialInstrumentsJList = new javax.swing.JList();
        selectPatternBtn = new javax.swing.JButton();
        leftPlayPatternBtn = new javax.swing.JButton();
        removePatternBtn = new javax.swing.JButton();
        rightPlayPatternBtn = new javax.swing.JButton();
        copySelectionsBtn = new javax.swing.JButton();
        closeWindowBtn = new javax.swing.JButton();
        tempoVolumeLabel = new javax.swing.JLabel();
        widePatternTextField = new javax.swing.JTextField();
        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridBagLayout());
        rawPatternsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Raw Patterns"));
        rawPatternsPanel.setMinimumSize(new java.awt.Dimension(300, 200));
        rawPatternsPanel.setPreferredSize(new java.awt.Dimension(300, 200));
        rawRulesJList.setModel(selectedRulesModel);
        rawRulesJList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                rawPatternsMouseClicked(evt);
            }
        });
        rawPatternsPanel.setViewportView(rawRulesJList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 77;
        gridBagConstraints.ipady = 77;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.35;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(rawPatternsPanel, gridBagConstraints);
        selectedPatternsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Selected Patterns"));
        selectedPatternsPanel.setMinimumSize(new java.awt.Dimension(300, 200));
        selectedPatternsPanel.setPreferredSize(new java.awt.Dimension(300, 200));
        selectedRulesJList.setModel(selectedRulesModel);
        selectedRulesJList.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                selectedPatternsMouseClicked(evt);
            }
        });
        selectedPatternsPanel.setViewportView(selectedRulesJList);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 77;
        gridBagConstraints.ipady = 77;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 0.35;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(selectedPatternsPanel, gridBagConstraints);
        optionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Re-Extraction Options"));
        optionPanel.setToolTipText("Extract patterns again, possibly using different parameters.");
        optionPanel.setLayout(new java.awt.GridBagLayout());
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Maximum Number of Clusters: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        optionPanel.add(jLabel1, gridBagConstraints);
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Start Beat: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        optionPanel.add(jLabel2, gridBagConstraints);
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("End Beat: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        optionPanel.add(jLabel3, gridBagConstraints);
        startBeatTextField.setText("8.0");
        startBeatTextField.setToolTipText("The starting beat from which patterns will be extracted");
        startBeatTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startBeatTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        optionPanel.add(startBeatTextField, gridBagConstraints);
        endBeatTextField.setText(" ");
        endBeatTextField.setToolTipText("The ending beat from which patterns will be extracted");
        endBeatTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                endBeatTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        optionPanel.add(endBeatTextField, gridBagConstraints);
        numberOfClustersSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, 99, 1));
        numberOfClustersSpinner.setToolTipText("The number of clusters sought in pattern extraction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.5;
        optionPanel.add(numberOfClustersSpinner, gridBagConstraints);
        reExtractBtn.setText("Re-Extract Patterns");
        reExtractBtn.setToolTipText("Extract the patterns for this window using new parameters.");
        reExtractBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reExtractBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        optionPanel.add(reExtractBtn, gridBagConstraints);
        doubleDrumLength.setText("Double Drum Length");
        doubleDrumLength.setToolTipText("Change the length of extracted drum pattern to be double what it was.");
        doubleDrumLength.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doubleDrumLengthActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        optionPanel.add(doubleDrumLength, gridBagConstraints);
        potentialInstrumentsJList.setBorder(javax.swing.BorderFactory.createTitledBorder("Instrument"));
        potentialInstrumentsJList.setModel(new javax.swing.AbstractListModel() {

            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };

            public int getSize() {
                return strings.length;
            }

            public Object getElementAt(int i) {
                return strings[i];
            }
        });
        potentialInstrumentsJList.setToolTipText("Select MIDI instrument for re-estraction");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        optionPanel.add(potentialInstrumentsJList, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.3;
        gridBagConstraints.weighty = 0.2;
        getContentPane().add(optionPanel, gridBagConstraints);
        selectPatternBtn.setText("Include Pattern");
        selectPatternBtn.setToolTipText("Moves the selected pattern into the right list for inclusion in the style.");
        selectPatternBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectPatternBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.4;
        getContentPane().add(selectPatternBtn, gridBagConstraints);
        leftPlayPatternBtn.setText("Play Pattern");
        leftPlayPatternBtn.setToolTipText("Play the selected pattern (also can achieve with a double-click).");
        leftPlayPatternBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                leftPlayPatternBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(leftPlayPatternBtn, gridBagConstraints);
        removePatternBtn.setText("Remove Pattern");
        removePatternBtn.setToolTipText("Removes the selected pattern from further consideration for the style.");
        removePatternBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePatternBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.4;
        getContentPane().add(removePatternBtn, gridBagConstraints);
        rightPlayPatternBtn.setText("Play Pattern");
        rightPlayPatternBtn.setToolTipText("Play the selected pattern (also can achieve with a double-click).");
        rightPlayPatternBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rightPlayPatternBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(rightPlayPatternBtn, gridBagConstraints);
        copySelectionsBtn.setText("Copy Selections to Style Editor");
        copySelectionsBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copySelectionsBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.2;
        getContentPane().add(copySelectionsBtn, gridBagConstraints);
        closeWindowBtn.setText("Dismiss this Window");
        closeWindowBtn.setToolTipText("Close the window. Any patterns to be included should be copied to the Style Editor before closing.");
        closeWindowBtn.setActionCommand("Dismiss this Window");
        closeWindowBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeWindowBtnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(closeWindowBtn, gridBagConstraints);
        tempoVolumeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tempoVolumeLabel.setText("Tempo and Volume are set in Style Editor.");
        tempoVolumeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        tempoVolumeLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        getContentPane().add(tempoVolumeLabel, gridBagConstraints);
        widePatternTextField.setEditable(false);
        widePatternTextField.setBorder(javax.swing.BorderFactory.createTitledBorder("Most Recent Pattern"));
        widePatternTextField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                widePatternTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        getContentPane().add(widePatternTextField, gridBagConstraints);
        pack();
    }

    private void leftPlayPatternBtnActionPerformed(java.awt.event.ActionEvent evt) {
        playRawRule();
    }

    private void rightPlayPatternBtnActionPerformed(java.awt.event.ActionEvent evt) {
        playSelectedRule();
    }

    private void closeWindowBtnActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void copySelectionsBtnActionPerformed(java.awt.event.ActionEvent evt) {
        switch(type) {
            case BASS:
                MIDIBeast.selectedBassRules = selectedBassRules;
                styleEditor.loadBassPatterns(MIDIBeast.repBassRules.getBassRules());
                break;
            case CHORD:
                MIDIBeast.selectedChordRules = selectedChordRules;
                styleEditor.loadChordPatterns(MIDIBeast.repChordRules.getChordRules());
                break;
            case DRUM:
                MIDIBeast.selectedDrumRules = selectedDrumRules;
                styleEditor.loadDrumPatterns(MIDIBeast.repDrumRules.getRepresentativePatterns());
                break;
        }
    }

    private void startBeatTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void selectPatternBtnActionPerformed(java.awt.event.ActionEvent evt) {
        Object ob = rawRulesJList.getSelectedValue();
        if (ob instanceof RepPattern) {
            RepPattern repPattern = (RepPattern) ob;
            int index = rawRulesJList.getSelectedIndex();
            switch(type) {
                case BASS:
                    RepresentativeBassRules.BassPattern selectedBassRule = (RepresentativeBassRules.BassPattern) repPattern;
                    selectedBassRules.add(selectedBassRule);
                    setBassSelectedRules();
                    break;
                case CHORD:
                    RepresentativeChordRules.ChordPattern selectedChordRule = (RepresentativeChordRules.ChordPattern) repPattern;
                    selectedChordRules.add(selectedChordRule);
                    setChordSelectedRules();
                    break;
                case DRUM:
                    RepresentativeDrumRules.DrumPattern drumPattern = (RepresentativeDrumRules.DrumPattern) repPattern;
                    selectedDrumRules.add(drumPattern);
                    setDrumSelectedRules();
                    break;
            }
            rawRulesModel.removeElement(ob);
            rawRulesJList.setSelectedIndex(Math.max(0, index - 1));
        }
    }

    private void doubleDrumLengthActionPerformed(java.awt.event.ActionEvent evt) {
        if (doubleDrumLength.isSelected()) {
            MIDIBeast.drumMeasureSize *= 2;
        } else {
            MIDIBeast.drumMeasureSize = MIDIBeast.slotsPerMeasure;
        }
    }

    private void selectedPatternsMouseClicked(java.awt.event.MouseEvent evt) {
        Object selectedOb = selectedRulesJList.getSelectedValue();
        if (selectedOb instanceof RepPattern) {
            widePatternTextField.setText(selectedOb.toString());
            playSelectedRule();
        }
    }

    private void rawPatternsMouseClicked(java.awt.event.MouseEvent evt) {
        Object selectedOb = rawRulesJList.getSelectedValue();
        if (selectedOb instanceof RepPattern) {
            widePatternTextField.setText(selectedOb.toString());
            playRawRule();
        }
    }

    private void removePatternBtnActionPerformed(java.awt.event.ActionEvent evt) {
        int indexOfRuleToBeRemoved = selectedRulesJList.getSelectedIndex();
        switch(type) {
            case BASS:
                selectedBassRules.remove(indexOfRuleToBeRemoved);
                selectedRulesJList.setListData(selectedBassRules.toArray());
                break;
            case CHORD:
                selectedChordRules.remove(indexOfRuleToBeRemoved);
                selectedRulesJList.setListData(selectedChordRules.toArray());
                break;
            case DRUM:
                selectedDrumRules.remove(indexOfRuleToBeRemoved);
                selectedRulesJList.setListData(selectedDrumRules.toArray());
                break;
        }
        selectedRulesJList.setSelectedIndex(Math.max(0, indexOfRuleToBeRemoved - 1));
    }

    private void reExtractBtnActionPerformed(java.awt.event.ActionEvent evt) {
        checkForAndThrowErrors();
        double endBeat = Double.parseDouble(endBeatTextField.getText());
        double startBeat = Double.parseDouble(startBeatTextField.getText());
        Integer maxNumberOfClusters = (Integer) numberOfClustersSpinner.getValue();
        int selectedIndex = potentialInstrumentsJList.getSelectedIndex();
        jm.music.data.Part selectedPart = MIDIBeast.allParts.get(selectedIndex);
        switch(type) {
            case BASS:
                MIDIBeast.repBassRules = new RepresentativeBassRules(startBeat, endBeat, maxNumberOfClusters, selectedPart);
                repBassRules = MIDIBeast.repBassRules;
                setBassRawRules();
                setBassSelectedRules();
                break;
            case CHORD:
                MIDIBeast.repChordRules = new RepresentativeChordRules(startBeat, endBeat, maxNumberOfClusters, selectedPart, minDuration);
                repChordRules = MIDIBeast.repChordRules;
                setChordRawRules();
                setChordSelectedRules();
                break;
            case DRUM:
                MIDIBeast.repDrumRules = new RepresentativeDrumRules(startBeat, endBeat, maxNumberOfClusters, selectedPart);
                repDrumRules = MIDIBeast.repDrumRules;
                setDrumRawRules();
                setDrumSelectedRules();
                break;
        }
    }

    private void endBeatTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void widePatternTextFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private javax.swing.JButton closeWindowBtn;

    private javax.swing.JButton copySelectionsBtn;

    private javax.swing.JCheckBox doubleDrumLength;

    private javax.swing.JTextField endBeatTextField;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel2;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JButton leftPlayPatternBtn;

    private javax.swing.JSpinner numberOfClustersSpinner;

    private javax.swing.JPanel optionPanel;

    private javax.swing.JList potentialInstrumentsJList;

    private javax.swing.JScrollPane rawPatternsPanel;

    private javax.swing.JList rawRulesJList;

    private javax.swing.JButton reExtractBtn;

    private javax.swing.JButton removePatternBtn;

    private javax.swing.JButton rightPlayPatternBtn;

    private javax.swing.JButton selectPatternBtn;

    private javax.swing.JScrollPane selectedPatternsPanel;

    private javax.swing.JList selectedRulesJList;

    private javax.swing.JTextField startBeatTextField;

    private javax.swing.JLabel tempoVolumeLabel;

    private javax.swing.JTextField widePatternTextField;

    private javax.swing.JButton errorButton;

    private javax.swing.JDialog errorDialog;

    private javax.swing.JLabel errorMessage;

    /**
   * Override dispose so as to unregister this window first.
   */
    @Override
    public void dispose() {
        WindowRegistry.unregisterWindow(this);
        super.dispose();
    }
}
