package gate.gui;

import gate.creole.annic.Constants;
import gate.creole.annic.Hit;
import gate.creole.annic.PatternAnnotation;
import gate.creole.annic.Pattern;
import gate.creole.annic.SearchException;
import gate.creole.annic.Searcher;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.regex.*;
import javax.swing.table.*;
import javax.swing.*;
import gate.*;
import gate.corpora.SerialCorpusImpl;
import gate.creole.AbstractVisualResource;
import gate.event.DatastoreEvent;
import gate.event.DatastoreListener;
import gate.event.ProgressListener;
import gate.gui.MainFrame;
import gate.persist.LuceneDataStoreImpl;
import gate.persist.PersistenceException;
import gate.swing.XJTable;
import gate.util.GateRuntimeException;
import gate.util.OptionsMap;

/**
 * Shows the results of a IR query. This VR is associated to
 * {@link gate.creole.ir.SearchPR}.
 */
public class LuceneDataStoreSearchAlternativeGUI extends AbstractVisualResource implements ProgressListener, DatastoreListener {

    private static final long serialVersionUID = 3256720688877285686L;

    /** The GUI is associated with the AnnicSearchPR */
    private Object target;

    /**
   * arraylist consist of instances of patterns associated found in the
   * document
   */
    private List<Hit> patterns;

    /**
   * A Map that contains annotation types as the key values and the
   * corresponding arraylist consists of features
   */
    private Map<String, List<String>> allAnnotTypesAndFeaturesFromDatastore;

    /**
   * Populated annotationTypesAndFeatures
   */
    private Map<String, Set<String>> populatedAnnotationTypesAndFeatures;

    /** Table that lists the patterns found by the query */
    private XJTable patternTable;

    /**
   * Model of the patternTable.
   */
    private PatternsTableModel patternsTableModel;

    /** Combobox to list the types */
    private JComboBox annotTypesBox;

    private JComboBox featuresBox;

    /**
   * Display the feature manager.
   */
    private JButton featureManagerButton;

    /**
   * Button that allows retrieving next n number of results
   */
    private JButton nextResults;

    /** Button to execute a new Query */
    private JButton executeQuery;

    /** Button to clear the newQueryQuery Text Field */
    private JButton clearQueryTF;

    /** Button to export results into an HTML file */
    private JButton exportToHTML;

    /**
   * group for allPatterns and selectedPatterns, only one of them should
   * be selected at a time
   */
    private ButtonGroup patternExportButtonsGroup;

    /** Export all pattern to html */
    private JRadioButton allPatterns;

    /** Export only selectedPatterns to html */
    private JRadioButton selectedPatterns;

    /** Text Field that holds the query */
    private JTextField newQuery;

    /**
   * Which corpus to use when searching in
   */
    private JComboBox corpusToSearchIn;

    /**
   * Which annotation set to search in.
   */
    private JComboBox annotationSetToSearchIn;

    /**
   * We maintain a list of IDs available in datastore
   */
    private List<Object> corpusIds;

    /**
   * AnnotationSet IDS the structure is: CorpusID;annotationSetName
   */
    private String[] annotationSetIDsFromDataStore;

    /**
   * User will specify the noOfPatternsToSearch here
   */
    private JSpinner numberOfResultsSpinner;

    /**
   * No Of tokens to be shown in context window
   */
    private JSpinner contextSizeSpinner;

    /** Label */
    private JLabel queryToExecute;

    /**
   * Gives the page number displayed in the results.
   */
    private JLabel titleResults;

    /**
   * Number of the page of results.
   */
    private int pageOfResults;

    /** JPanel that contains the center panel of pattern rows. */
    private JPanel centerPanel;

    /**
   * JPanel that contains the top panel of drop down lists and buttons for
   * writing a query, executing it, select a corpus/annotation set. 
   */
    private JPanel topPanel;

    /** Color Generator */
    private gate.swing.ColorGenerator colorGenerator = new gate.swing.ColorGenerator();

    /** Instance of ExecuteQueryAction */
    private ExecuteQueryAction execQueryAction;

    /**
   * Instance of NextResultAction
   */
    private NextResultAction nextResultAction;

    /** Instance of ClearQueryAction */
    private ClearQueryAction clearQueryAction;

    /** Instance of ExportResultsAction */
    private ExportResultsAction exportResultsAction;

    /**
   * A working wheel to indicate search is going on.
   */
    private JLabel progressLabel;

    /**
   * Instance of this class.
   */
    LuceneDataStoreSearchAlternativeGUI thisInstance;

    /**
   * Current instance of the feature manager.
   */
    private FeaturesManager featuresManager;

    /** Names of the columns for shortcuts data. */
    String[] columnNames = { "Display", "Shortcut", "Annotation type", "Feature", "Crop" };

    /** Column (second dimension) of shortcuts double array. */
    private static final int DISPLAY = 0;

    /** Column (second dimension) of shortcuts double array. */
    private static final int SHORTCUT = 1;

    /** Column (second dimension) of shortcuts double array. */
    private static final int ANNOTATION_TYPE = 2;

    /** Column (second dimension) of shortcuts double array. */
    private static final int FEATURE = 3;

    /** Column (second dimension) of shortcuts double array. */
    private static final int CROP = 4;

    /** Maximum number of shortcut */
    private static final int maxShortcuts = 100;

    /** Number of shortcuts. */
    private int numShortcuts = 0;

    /**
   * Double array that contains [row, column] of the shortcuts
   * data.
   */
    private String[][] shortcuts = new String[maxShortcuts][columnNames.length];

    /**
   * Searcher object obtained from the datastore
   */
    private Searcher searcher;

    /**
   * Called when a View is loaded in GATE.
   */
    public Resource init() {
        patterns = new ArrayList<Hit>();
        allAnnotTypesAndFeaturesFromDatastore = new HashMap<String, List<String>>();
        thisInstance = this;
        corpusIds = new ArrayList<Object>();
        populatedAnnotationTypesAndFeatures = new HashMap<String, Set<String>>();
        OptionsMap userConfig = Gate.getUserConfig();
        if (userConfig.get("Features_shortcuts") != null) {
            String shortcutsString = (String) userConfig.get("Features_shortcuts");
            shortcutsString = shortcutsString.replaceAll("^\\[\\[", "");
            shortcutsString = shortcutsString.replaceAll("\\]\\]$", "");
            String[] rows = shortcutsString.split("\\], \\[");
            numShortcuts = rows.length;
            if (shortcutsString.length() > 0) {
                for (int row = 0; row < numShortcuts; row++) {
                    String[] cols = rows[row].split(", ");
                    for (int col = 0; col < cols.length; col++) {
                        shortcuts[row][col] = cols[col];
                    }
                }
            }
        }
        initGui();
        if (target != null) {
            if (target instanceof Searcher) {
                searcher = (Searcher) target;
            } else if (target instanceof LuceneDataStoreImpl) {
                searcher = ((LuceneDataStoreImpl) target).getSearcher();
            } else {
                throw new GateRuntimeException("Invalid target specified for the GUI");
            }
            updateDisplay();
        }
        validate();
        return this;
    }

    public void cleanup() {
        OptionsMap userConfig = Gate.getUserConfig();
        String shortcutsString = "[";
        for (int row = 0; row < numShortcuts; row++) {
            shortcutsString += (row == 0) ? "[" : ", [";
            for (int col = 0; col < columnNames.length; col++) {
                shortcutsString += (col == 0) ? shortcuts[row][col] : ", " + shortcuts[row][col];
            }
            shortcutsString += "]";
        }
        shortcutsString += "]";
        userConfig.put("Features_shortcuts", shortcutsString);
    }

    /**
   * Initialize the local data (i.e. Pattern data etc.) and then update
   * the GUI
   */
    protected void updateDisplay() {
        patterns.clear();
        if (target != null) {
            initLocalData();
            updateGui();
            patternsTableModel.fireTableDataChanged();
            if (patternTable.getRowCount() > 0) {
                String query = newQuery.getText();
                ArrayList<String> toAdd = new ArrayList<String>();
                if (query.length() > 0 && !patterns.isEmpty()) {
                    String[] posStart = new String[] { "{", ",", " " };
                    String[] posEnd = new String[] { "}", ".", ",", " ", "=" };
                    outer: for (String annotType : populatedAnnotationTypesAndFeatures.keySet()) {
                        for (String start : posStart) {
                            for (String end : posEnd) {
                                String toSearch = start + annotType + end;
                                if (query.indexOf(toSearch) > -1) {
                                    toAdd.add(annotType);
                                    continue outer;
                                }
                            }
                        }
                    }
                    if (!toAdd.isEmpty()) {
                        for (String at : toAdd) {
                            for (int row = 0; row < numShortcuts; row++) {
                                if (shortcuts[row][ANNOTATION_TYPE].equals(at) && shortcuts[row][DISPLAY].equals("false")) {
                                    shortcuts[row][DISPLAY] = "true";
                                }
                            }
                        }
                    }
                }
                patternTable.setRowSelectionInterval(0, 0);
                tableValueChanged();
                exportResultsAction.setEnabled(true);
            } else if (newQuery.getText().trim().length() < 1) {
                centerPanel.removeAll();
                centerPanel.add(new JLabel("Please, enter a query in the query text field."), new GridBagConstraints());
                centerPanel.validate();
                newQuery.requestFocusInWindow();
            } else {
                centerPanel.removeAll();
                centerPanel.add(new JTextArea("No pattern found for your query.\n\n" + "Here are the different type of queries you can use:\n" + "1. String, ex. the\n" + "2. {AnnotationType}, ex. {Person}\n" + "3. {AnnotationType == String}, ex. {Person == \"Smith\"}\n" + "4. {AnnotationType.feature == featureValue}, ex. {Person.gender == \"male\"}\n" + "5. {AnnotationType1, AnnotationType2.feature == featureValue}\n" + "6. {AnnotationType1.feature == featureValue, AnnotationType2.feature == featureValue}\n\n" + "ANNIC also supports the ∣ (OR) operator.\n" + "For instance, {A} ({B}∣{C}) is a pattern of two annotations where the ﬁrst is an annotation of type A followed by the annotation of type either B or C.\n" + "ANNIC supports two operators, + and *, to specify the number of times a particular annotation or a sub pattern should appear in the main query pattern.\n" + "Here, ({A})+n means one and up to n occurrences of annotation {A} and ({A})*n means zero or up to n occurrences of annotation {A}.\n"), new GridBagConstraints());
                centerPanel.validate();
                exportResultsAction.setEnabled(false);
            }
        }
    }

    /**
   * Initialize the GUI.
   */
    protected void initGui() {
        setLayout(new BorderLayout());
        topPanel = new JPanel(new GridBagLayout());
        topPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(new JLabel("Corpus: "), gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        corpusToSearchIn = new JComboBox();
        corpusToSearchIn.addItem(Constants.ENTIRE_DATASTORE);
        corpusToSearchIn.setPrototypeDisplayValue(Constants.ENTIRE_DATASTORE);
        corpusToSearchIn.setToolTipText("Select the corpus to search in.");
        if (target == null || target instanceof Searcher) {
            corpusToSearchIn.setEnabled(false);
        }
        corpusToSearchIn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ie) {
                updateAnnotationSetsToSearchInBox();
            }
        });
        gbc.weightx = 1;
        topPanel.add(corpusToSearchIn, gbc);
        gbc.weightx = 0;
        topPanel.add(Box.createHorizontalStrut(4), gbc);
        topPanel.add(new JLabel("Annotation set: "), gbc);
        annotationSetToSearchIn = new JComboBox();
        annotationSetToSearchIn.setPrototypeDisplayValue(Constants.COMBINED_SET);
        annotationSetToSearchIn.setToolTipText("Select the annotation set to search in.");
        annotationSetToSearchIn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ie) {
                updateAnnotationTypeBox();
            }
        });
        gbc.weightx = 1;
        topPanel.add(annotationSetToSearchIn, gbc);
        gbc.weightx = 0;
        JLabel contextWindowLabel = new JLabel("Context size: ");
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        topPanel.add(contextWindowLabel, gbc);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        queryToExecute = new JLabel("Query:");
        topPanel.add(queryToExecute, gbc);
        clearQueryAction = new ClearQueryAction();
        clearQueryTF = new JButton();
        clearQueryTF.setBorderPainted(false);
        clearQueryTF.setContentAreaFilled(false);
        clearQueryTF.setAction(clearQueryAction);
        clearQueryTF.setEnabled(true);
        gbc.fill = GridBagConstraints.NONE;
        topPanel.add(clearQueryTF, gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        newQuery = new JTextField();
        newQuery.setToolTipText("Please enter a query to search in the datastore.");
        newQuery.setEnabled(true);
        newQuery.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                executeQuery.doClick();
            }
        });
        gbc.gridwidth = 4;
        gbc.weightx = 1;
        topPanel.add(newQuery, gbc);
        execQueryAction = new ExecuteQueryAction();
        executeQuery = new JButton();
        executeQuery.setBorderPainted(false);
        executeQuery.setContentAreaFilled(false);
        executeQuery.setAction(execQueryAction);
        executeQuery.setEnabled(true);
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        topPanel.add(executeQuery, gbc);
        contextSizeSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 30, 1));
        contextSizeSpinner.setToolTipText("Number of Tokens to be displayed in the context.");
        contextSizeSpinner.setEnabled(true);
        topPanel.add(contextSizeSpinner, gbc);
        add(topPanel, BorderLayout.NORTH);
        centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.setOpaque(true);
        centerPanel.setBackground(Color.WHITE);
        featureManagerButton = new JButton();
        featureManagerButton.setAction(new ManageFeaturesToDisplayAction());
        featureManagerButton.setBorderPainted(true);
        featureManagerButton.setMargin(new Insets(0, 0, 0, 0));
        progressLabel = new JLabel(MainFrame.getIcon("working"));
        progressLabel.setOpaque(false);
        progressLabel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        progressLabel.setEnabled(true);
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        bottomPanel.setOpaque(false);
        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 3);
        gbc.gridwidth = 1;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        titleResults = new JLabel("Results");
        bottomPanel.add(titleResults, gbc);
        nextResultAction = new NextResultAction();
        nextResults = new JButton();
        nextResults.setBorderPainted(false);
        nextResults.setContentAreaFilled(false);
        nextResults.setAction(nextResultAction);
        nextResults.setEnabled(true);
        bottomPanel.add(nextResults, gbc);
        JLabel noOfPatternsLabel = new JLabel("Results per page: ");
        bottomPanel.add(noOfPatternsLabel, gbc);
        numberOfResultsSpinner = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 5));
        numberOfResultsSpinner.setToolTipText("Number of results per page.");
        numberOfResultsSpinner.setEnabled(true);
        bottomPanel.add(numberOfResultsSpinner, gbc);
        gbc.weightx = 1;
        bottomPanel.add(Box.createHorizontalStrut(1), gbc);
        gbc.weightx = 0;
        bottomPanel.add(new JLabel("Export results:"), gbc);
        allPatterns = new JRadioButton("All");
        allPatterns.setToolTipText("Exports all the patterns in the table.");
        allPatterns.setSelected(true);
        allPatterns.setEnabled(true);
        bottomPanel.add(allPatterns, gbc);
        selectedPatterns = new JRadioButton("Selected");
        selectedPatterns.setToolTipText("Exports selected patterns in the table.");
        selectedPatterns.setSelected(false);
        selectedPatterns.setEnabled(true);
        bottomPanel.add(selectedPatterns, gbc);
        patternExportButtonsGroup = new ButtonGroup();
        patternExportButtonsGroup.add(allPatterns);
        patternExportButtonsGroup.add(selectedPatterns);
        exportResultsAction = new ExportResultsAction();
        exportToHTML = new JButton();
        exportToHTML.setBorderPainted(false);
        exportToHTML.setContentAreaFilled(false);
        exportToHTML.setAction(exportResultsAction);
        exportResultsAction.setEnabled(false);
        bottomPanel.add(exportToHTML, gbc);
        patternsTableModel = new PatternsTableModel();
        patternTable = new XJTable(patternsTableModel);
        patternTable.addMouseMotionListener(new MouseMotionListener() {

            public void mouseMoved(MouseEvent me) {
                int row = patternTable.rowAtPoint(me.getPoint());
                row = patternTable.rowViewToModel(row);
                Pattern pattern = null;
                if (row > -1) {
                    pattern = (Pattern) patterns.get(row);
                    patternTable.setToolTipText(pattern.getQueryString());
                }
            }

            public void mouseDragged(MouseEvent me) {
            }
        });
        patternTable.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    final JPopupMenu popup = new JPopupMenu();
                    JButton delete = new JButton("Delete");
                    popup.add(delete);
                    delete.addActionListener(new ActionListener() {

                        public void actionPerformed(ActionEvent ae) {
                            int[] rows = patternTable.getSelectedRows();
                            for (int i = 0; i < rows.length; i++) {
                                rows[i] = patternTable.rowViewToModel(rows[i]);
                            }
                            rows = sortRows(rows);
                            for (int i = rows.length - 1; i >= 0; i--) {
                                patterns.remove(rows[i]);
                            }
                            patternsTableModel.fireTableDataChanged();
                            tableValueChanged();
                            popup.setVisible(false);
                        }
                    });
                    popup.show(patternTable, me.getX(), me.getY() - 10);
                }
            }

            public void mousePressed(MouseEvent me) {
            }

            public void mouseReleased(MouseEvent me) {
            }

            public void mouseEntered(MouseEvent me) {
            }

            public void mouseExited(MouseEvent me) {
            }
        });
        patternTable.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {

            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                tableValueChanged();
            }
        });
        patternTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        patternTable.setSortable(true);
        JScrollPane tableScrollPane = new JScrollPane(patternTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        gbc.weightx = 2;
        gbc.weighty = 2;
        bottomPanel.add(tableScrollPane, gbc);
        JSplitPane centerBottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        centerBottomSplitPane.setDividerLocation(350);
        centerBottomSplitPane.add(new JScrollPane(centerPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
        centerBottomSplitPane.add(bottomPanel);
        add(centerBottomSplitPane, BorderLayout.CENTER);
        annotTypesBox = new JComboBox();
        annotTypesBox.setBackground(Color.WHITE);
        annotTypesBox.setToolTipText("Select an annotation type then a feature.");
        annotTypesBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ie) {
                updateFeaturesBox();
            }
        });
        featuresBox = new JComboBox();
        featuresBox.setBackground(Color.WHITE);
        featuresBox.setToolTipText("Select a feature to display it.");
    }

    protected void initLocalData() {
        Hit[] pats = searcher.getHits();
        if (patterns == null) patterns = new ArrayList<Hit>();
        patterns.clear();
        for (int m = 0; m < pats.length; m++) {
            patterns.add(pats[m]);
        }
        pats = null;
    }

    /**
   * Initializes the comboboxes for annotation Types and their features.
   */
    public void updateGui() {
        centerPanel.removeAll();
        centerPanel.validate();
        centerPanel.updateUI();
    }

    /**
   * Updates the pattern row view when the user changes
   * his/her selection of pattern in the patternTable.
   */
    public void tableValueChanged() {
        int maxColumns = 200;
        int maxValueLength = 20;
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        if (patternTable.getSelectedRow() == -1) {
            if (patternTable.getRowCount() > 0) {
                centerPanel.removeAll();
                centerPanel.add(new JLabel("Please select a row in the pattern table."), gbc);
                centerPanel.validate();
                centerPanel.repaint();
                selectedPatterns.setEnabled(false);
            }
            return;
        }
        centerPanel.removeAll();
        selectedPatterns.setEnabled(true);
        annotTypesBox.setEnabled(true);
        featuresBox.setEnabled(true);
        Pattern pattern = (Pattern) patterns.get(patternTable.rowViewToModel(patternTable.getSelectedRow()));
        gbc.insets = new java.awt.Insets(0, 0, 0, 30);
        centerPanel.add(new JLabel("Text"), gbc);
        gbc.insets = new java.awt.Insets(0, 0, 0, 0);
        PatternAnnotation[] baseUnitPatternAnnotations = pattern.getPatternAnnotations((String) ((LuceneDataStoreImpl) target).getIndexer().getParameters().get(Constants.BASE_TOKEN_ANNOTATION_TYPE), "string");
        int BaseUnitNum = 0;
        String baseUnit = ((PatternAnnotation) baseUnitPatternAnnotations[BaseUnitNum]).getText();
        int startOffsetPattern = pattern.getStartOffset() - pattern.getLeftContextStartOffset();
        int endOffsetPattern = pattern.getEndOffset() - pattern.getLeftContextStartOffset();
        for (int charNum = 0; charNum < pattern.getPatternText().length() && charNum < maxColumns; charNum++) {
            gbc.gridx = charNum + 1;
            JLabel label = new JLabel(String.valueOf((pattern.getPatternText().charAt(charNum))));
            if (charNum >= startOffsetPattern && charNum < endOffsetPattern) {
                label.setBackground(new Color(240, 201, 184));
            } else {
                label.setBackground(Color.WHITE);
            }
            label.setOpaque(true);
            if (charNum == 0) {
                label.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 0, Color.BLACK));
            } else if (charNum == pattern.getPatternText().length() - 1 || charNum == maxColumns - 1) {
                label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, Color.BLACK));
            } else {
                label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.BLACK));
            }
            if (((PatternAnnotation) baseUnitPatternAnnotations[BaseUnitNum]).getEndOffset() - pattern.getLeftContextStartOffset() < charNum) {
                BaseUnitNum++;
                baseUnit = ((PatternAnnotation) baseUnitPatternAnnotations[BaseUnitNum]).getText();
            }
            label.addMouseListener(new AddPatternRowInQueryMouseInputListener(baseUnit));
            centerPanel.add(label, gbc);
        }
        if (pattern.getPatternText().length() > maxColumns) {
            gbc.gridx++;
            centerPanel.add(new JLabel(String.valueOf('…')), gbc);
        }
        gbc.gridy++;
        centerPanel.add(Box.createVerticalStrut(5), gbc);
        for (int row = 0; row < numShortcuts; row++) {
            if (shortcuts[row][DISPLAY].equals("false")) {
                continue;
            }
            String type = shortcuts[row][ANNOTATION_TYPE];
            String feature = shortcuts[row][FEATURE];
            gbc.gridy++;
            gbc.gridx = 0;
            JLabel annotationTypeAndFeature = new JLabel();
            String shortcut = null;
            for (int row2 = 0; row2 < numShortcuts; row2++) {
                if (shortcuts[row2][ANNOTATION_TYPE].equals(type) && shortcuts[row2][FEATURE].equals(feature)) {
                    shortcut = shortcuts[row2][SHORTCUT];
                    break;
                }
            }
            annotationTypeAndFeature.setText((shortcut != null) ? shortcut : type + "." + feature);
            annotationTypeAndFeature.setName(type + "." + feature);
            gbc.insets = new java.awt.Insets(0, 0, 0, 30);
            centerPanel.add(annotationTypeAndFeature, gbc);
            gbc.insets = new java.awt.Insets(0, 0, 0, 0);
            PatternAnnotation[] annots = pattern.getPatternAnnotations(type, feature);
            if (annots == null || annots.length == 0) {
                annots = new PatternAnnotation[0];
            }
            HashMap<Integer, Integer> gridSet = new HashMap<Integer, Integer>();
            for (int k = 0, j = 0; k < annots.length; k++, j += 2) {
                PatternAnnotation ann = (PatternAnnotation) annots[k];
                gbc.gridx = ann.getStartOffset() - pattern.getLeftContextStartOffset() + 1;
                gbc.gridwidth = ann.getEndOffset() - pattern.getLeftContextStartOffset() - gbc.gridx + 1;
                if ((gbc.gridx + gbc.gridwidth) > maxColumns) {
                    continue;
                }
                String value = (String) ann.getFeatures().get(feature);
                if (value.length() > maxValueLength) {
                    if (shortcuts[row][CROP].equals("Crop end")) {
                        value = value.substring(0, maxValueLength - 2) + String.valueOf('…');
                    } else {
                        value = String.valueOf('…') + value.substring(value.length() - maxValueLength - 1);
                    }
                }
                JLabel label = new JLabel(value);
                label.setBackground(getAnnotationTypeColor(ann.getType()));
                label.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                label.setOpaque(true);
                label.addMouseListener(new AddPatternRowInQueryMouseInputListener(type, feature, (String) ann.getFeatures().get(feature), (gbc.gridx < startOffsetPattern) ? AddPatternRowInQueryMouseInputListener.LEFT : (gbc.gridx > endOffsetPattern) ? AddPatternRowInQueryMouseInputListener.RIGHT : AddPatternRowInQueryMouseInputListener.CENTER));
                if (gridSet.containsKey(gbc.gridx)) {
                    int oldGridy = gbc.gridy;
                    gbc.gridy = gridSet.get(gbc.gridx) + 1;
                    centerPanel.add(label, gbc);
                    for (int w = 0; w < gbc.gridwidth; w++) {
                        gridSet.put(gbc.gridx + w, gbc.gridy);
                    }
                    gbc.gridy = oldGridy;
                } else {
                    centerPanel.add(label, gbc);
                    for (int w = 0; w < gbc.gridwidth; w++) {
                        gridSet.put(gbc.gridx + w, gbc.gridy);
                    }
                }
            }
            if (gridSet.size() > 0) {
                gbc.gridy = Collections.max(gridSet.values()).intValue();
            }
        }
        gbc.gridy++;
        centerPanel.add(Box.createVerticalStrut(5), gbc);
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        centerPanel.add(featureManagerButton, gbc);
        validate();
        updateUI();
    }

    private void updateAnnotationSetsToSearchInBox() {
        String corpusName = (corpusToSearchIn.getSelectedItem().equals(Constants.ENTIRE_DATASTORE)) ? null : (String) corpusIds.get(corpusToSearchIn.getSelectedIndex() - 1);
        TreeSet<String> ts = new TreeSet<String>(getAnnotationSetNames(corpusName));
        DefaultComboBoxModel dcbm = new DefaultComboBoxModel(ts.toArray());
        dcbm.insertElementAt(Constants.ALL_SETS, 0);
        annotationSetToSearchIn.setModel(dcbm);
        annotationSetToSearchIn.setSelectedItem(Constants.ALL_SETS);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                annotationSetToSearchIn.updateUI();
                if (annotationSetToSearchIn.getItemCount() > 1) {
                    updateAnnotationTypeBox();
                }
            }
        });
    }

    private void updateAnnotationTypeBox() {
        String corpusName = (corpusToSearchIn.getSelectedItem().equals(Constants.ENTIRE_DATASTORE)) ? null : (String) corpusIds.get(corpusToSearchIn.getSelectedIndex() - 1);
        String annotationSetName = (annotationSetToSearchIn.getSelectedItem().equals(Constants.ALL_SETS)) ? null : (String) annotationSetToSearchIn.getSelectedItem();
        populatedAnnotationTypesAndFeatures = getAnnotTypesFeatures(corpusName, annotationSetName);
        List<String> annotTypes = new ArrayList<String>();
        if (populatedAnnotationTypesAndFeatures != null && populatedAnnotationTypesAndFeatures.keySet() != null) {
            annotTypes.addAll(populatedAnnotationTypesAndFeatures.keySet());
        }
        Collections.sort(annotTypes);
        DefaultComboBoxModel aNewModel = new DefaultComboBoxModel(annotTypes.toArray());
        annotTypesBox.setModel(aNewModel);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                featuresBox.setActionCommand("not a user input");
                annotTypesBox.updateUI();
                if (annotTypesBox.getItemCount() > 0) {
                    updateFeaturesBox();
                } else {
                    ((DefaultComboBoxModel) featuresBox.getModel()).removeAllElements();
                    featuresBox.updateUI();
                }
                featuresBox.setActionCommand("");
            }
        });
    }

    private void updateFeaturesBox() {
        List<String> features = new ArrayList<String>();
        Set<String> featuresToAdd = populatedAnnotationTypesAndFeatures.get((String) annotTypesBox.getSelectedItem());
        if (featuresToAdd != null) {
            features.addAll(featuresToAdd);
        }
        Collections.sort(features);
        featuresBox.setActionCommand("not a user input");
        featuresBox.removeAllItems();
        for (String aFeat : features) {
            featuresBox.addItem(aFeat);
        }
        featuresBox.updateUI();
        featuresBox.setActionCommand("");
    }

    /**
   * Sort use for the pattern table.
   * 
   * @param rows table of rows to sort
   * @return rows sorted 
   */
    private int[] sortRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows.length - 1; j++) {
                if (rows[j] > rows[j + 1]) {
                    int temp = rows[j];
                    rows[j] = rows[j + 1];
                    rows[j + 1] = temp;
                }
            }
        }
        return rows;
    }

    /**
   * This method uses the java.util.prefs.Preferences and get the color
   * for particular annotationType.. This color could have been saved by
   * the AnnotationSetsView
   * 
   * @param annotationType
   * @return
   */
    private Color getAnnotationTypeColor(String annotationType) {
        java.util.prefs.Preferences prefRoot = null;
        try {
            prefRoot = java.util.prefs.Preferences.userNodeForPackage(Class.forName("gate.gui.docview.AnnotationSetsView"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        int rgba = prefRoot.getInt(annotationType, -1);
        Color colour;
        if (rgba == -1) {
            float components[] = colorGenerator.getNextColor().getComponents(null);
            colour = new Color(components[0], components[1], components[2], 0.5f);
            int rgb = colour.getRGB();
            int alpha = colour.getAlpha();
            rgba = rgb | (alpha << 24);
            prefRoot.putInt(annotationType, rgba);
        } else {
            colour = new Color(rgba, true);
        }
        return colour;
    }

    private Set<String> getAnnotationSetNames(String corpusName) {
        Set<String> toReturn = new HashSet<String>();
        if (corpusName == null) {
            for (String aSet : annotationSetIDsFromDataStore) {
                aSet = aSet.substring(aSet.indexOf(';') + 1);
                toReturn.add(aSet);
            }
        } else {
            for (String aSet : annotationSetIDsFromDataStore) {
                if (aSet.startsWith(corpusName + ";")) {
                    aSet = aSet.substring(aSet.indexOf(';') + 1);
                    toReturn.add(aSet);
                }
            }
        }
        return toReturn;
    }

    private Map<String, Set<String>> getAnnotTypesFeatures(String corpusName, String annotationSetName) {
        HashMap<String, Set<String>> toReturn = new HashMap<String, Set<String>>();
        if (corpusName == null && annotationSetName == null) {
            for (String type : allAnnotTypesAndFeaturesFromDatastore.keySet()) {
                String annotation = type.substring(type.lastIndexOf(';') + 1);
                Set<String> features = toReturn.get(annotation);
                if (features == null) {
                    features = new HashSet<String>();
                    toReturn.put(annotation, features);
                }
                features.addAll(allAnnotTypesAndFeaturesFromDatastore.get(type));
            }
        } else if (corpusName == null && annotationSetName != null) {
            for (String type : allAnnotTypesAndFeaturesFromDatastore.keySet()) {
                String annotation = type.substring(type.indexOf(';') + 1);
                if (annotation.startsWith(annotationSetName + ";")) {
                    annotation = annotation.substring(annotation.indexOf(';') + 1);
                    Set<String> features = toReturn.get(annotation);
                    if (features == null) {
                        features = new HashSet<String>();
                        toReturn.put(annotation, features);
                    }
                    features.addAll(allAnnotTypesAndFeaturesFromDatastore.get(type));
                }
            }
        } else if (corpusName != null && annotationSetName == null) {
            for (String type : allAnnotTypesAndFeaturesFromDatastore.keySet()) {
                if (type.startsWith(corpusName + ";")) {
                    String annotation = type.substring(type.lastIndexOf(';') + 1);
                    Set<String> features = toReturn.get(annotation);
                    if (features == null) {
                        features = new HashSet<String>();
                        toReturn.put(annotation, features);
                    }
                    features.addAll(allAnnotTypesAndFeaturesFromDatastore.get(type));
                }
            }
        } else {
            for (String type : allAnnotTypesAndFeaturesFromDatastore.keySet()) {
                if (type.startsWith(corpusName + ";" + annotationSetName + ";")) {
                    String annotation = type.substring(type.lastIndexOf(';') + 1);
                    Set<String> features = toReturn.get(annotation);
                    if (features == null) {
                        features = new HashSet<String>();
                        toReturn.put(annotation, features);
                    }
                    features.addAll(allAnnotTypesAndFeaturesFromDatastore.get(type));
                }
            }
        }
        return toReturn;
    }

    /**
   * Exports all patterns to the XML File.
   */
    protected class ExportResultsAction extends AbstractAction {

        private static final long serialVersionUID = 3257286928859412277L;

        public ExportResultsAction() {
            super("", MainFrame.getIcon("annic-export"));
            super.putValue(SHORT_DESCRIPTION, "Export Results to HTML.");
            super.putValue(MNEMONIC_KEY, KeyEvent.VK_E);
        }

        public void actionPerformed(ActionEvent ae) {
            Map<Object, Object> parameters = searcher.getParameters();
            if (patterns == null || patterns.isEmpty()) {
                try {
                    JOptionPane.showMessageDialog(gate.Main.getMainFrame(), "No patterns found to export");
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            try {
                JFileChooser fileDialog = new JFileChooser();
                String fileDialogTitle = "HTML";
                fileDialog.setDialogTitle(fileDialogTitle + " File to export pattern results to...");
                JFrame frame = target instanceof Searcher ? null : gate.Main.getMainFrame();
                fileDialog.showSaveDialog(frame);
                java.io.File file = fileDialog.getSelectedFile();
                if (file == null) return;
                java.io.FileWriter fileWriter = new java.io.FileWriter(file);
                ArrayList<Hit> patternsToExport = new ArrayList<Hit>();
                if (selectedPatterns.isSelected()) {
                    int[] rows = patternTable.getSelectedRows();
                    for (int i = 0; i < rows.length; i++) {
                        int num = patternTable.rowViewToModel(rows[i]);
                        patternsToExport.add(patterns.get(num));
                    }
                } else {
                    for (int i = 0; i < patternTable.getRowCount(); i++) {
                        int num = patternTable.rowViewToModel(i);
                        patternsToExport.add(patterns.get(num));
                    }
                }
                java.io.BufferedWriter bw = new java.io.BufferedWriter(fileWriter);
                bw.write("<HTML><TITLE>ANNIC Output</TITLE><BODY>");
                bw.write("<BR><B>Query Issued: " + searcher.getQuery() + "<BR>Context Window :" + ((Integer) parameters.get(Constants.CONTEXT_WINDOW)).intValue() + "</B><BR><BR>");
                bw.write("<BR><B>Queries:</B>");
                String queryString = "";
                for (int i = 0; i < patternsToExport.size(); i++) {
                    Pattern ap = (Pattern) patternsToExport.get(i);
                    if (!ap.getQueryString().equals(queryString)) {
                        bw.write("<BR><a href=\"#" + ap.getQueryString() + "\">" + ap.getQueryString() + "</a>");
                        queryString = ap.getQueryString();
                    }
                }
                bw.write("<BR><BR>");
                queryString = "";
                for (int i = 0; i < patternsToExport.size(); i++) {
                    Pattern ap = (Pattern) patternsToExport.get(i);
                    if (!ap.getQueryString().equals(queryString)) {
                        if (!queryString.equals("")) {
                            bw.write("</TABLE><BR><BR>");
                        }
                        queryString = ap.getQueryString();
                        bw.write("<BR><B> <a name=\"" + ap.getQueryString() + "\">Query Pattern : " + ap.getQueryString() + "</a></B><BR>");
                        bw.write("<BR><TABLE border=\"1\">");
                        bw.write("<TR><TD><B> No. </B></TD>");
                        bw.write("<TD><B> Document ID </B></TD>");
                        bw.write("<TD><B> Left Context </B></TD>");
                        bw.write("<TD><B> Pattern Text </B></TD>");
                        bw.write("<TD><B> Right Context </B></TD>");
                        bw.write("</TR>");
                    }
                    bw.write("<TR><TD>" + (i + 1) + "</TD>");
                    bw.write("<TD>" + ap.getDocumentID() + "</TD>");
                    bw.write("<TD>" + ap.getPatternText(ap.getLeftContextStartOffset(), ap.getStartOffset()) + "</TD>");
                    bw.write("<TD>" + ap.getPatternText(ap.getStartOffset(), ap.getEndOffset()) + "</TD>");
                    bw.write("<TD>" + ap.getPatternText(ap.getEndOffset(), ap.getRightContextEndOffset()) + "</TD>");
                    bw.write("</TR>");
                }
                bw.write("</TABLE></BODY></HTML>");
                bw.flush();
                bw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
   * Clear the newQuery text box.
   */
    protected class ClearQueryAction extends AbstractAction {

        private static final long serialVersionUID = 3257569516199228209L;

        public ClearQueryAction() {
            super("", MainFrame.getIcon("annic-clean"));
            super.putValue(SHORT_DESCRIPTION, "Clear Query Text Box.");
            super.putValue(MNEMONIC_KEY, KeyEvent.VK_BACK_SPACE);
        }

        public void actionPerformed(ActionEvent ae) {
            newQuery.setText("");
        }
    }

    /** 
   * Finds out the newly created query and execute it.
   */
    protected class ExecuteQueryAction extends AbstractAction {

        private static final long serialVersionUID = 3258128055204917812L;

        public ExecuteQueryAction() {
            super("", MainFrame.getIcon("annic-search"));
            super.putValue(SHORT_DESCRIPTION, "Execute Query.");
            super.putValue(MNEMONIC_KEY, KeyEvent.VK_ENTER);
        }

        public void actionPerformed(ActionEvent ae) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    centerPanel.removeAll();
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.weighty = 1.0;
                    gbc.weightx = 1.0;
                    centerPanel.add(progressLabel, gbc);
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            progressLabel.updateUI();
                            thisInstance.setEnabled(false);
                            Map<Object, Object> parameters = searcher.getParameters();
                            if (parameters == null) parameters = new HashMap<Object, Object>();
                            if (target instanceof LuceneDataStoreImpl) {
                                URL indexLocationURL = (URL) ((LuceneDataStoreImpl) target).getIndexer().getParameters().get(Constants.INDEX_LOCATION_URL);
                                String indexLocation = null;
                                try {
                                    indexLocation = new File(indexLocationURL.toURI()).getAbsolutePath();
                                } catch (URISyntaxException use) {
                                    indexLocation = new File(indexLocationURL.getFile()).getAbsolutePath();
                                }
                                ArrayList<String> indexLocations = new ArrayList<String>();
                                indexLocations.add(indexLocation);
                                parameters.put(Constants.INDEX_LOCATIONS, indexLocations);
                                String corpus2SearchIn = (corpusToSearchIn.getSelectedItem().equals(Constants.ENTIRE_DATASTORE)) ? null : (String) corpusIds.get(corpusToSearchIn.getSelectedIndex() - 1);
                                parameters.put(Constants.CORPUS_ID, corpus2SearchIn);
                            }
                            int noOfPatterns = ((Number) numberOfResultsSpinner.getValue()).intValue();
                            int contextWindow = ((Number) contextSizeSpinner.getValue()).intValue();
                            String query = newQuery.getText().trim();
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{([^\\{=]+)==");
                            Matcher matcher = pattern.matcher(query);
                            int start = 0;
                            while (matcher.find(start)) {
                                start = matcher.end(1);
                                for (int row = 0; row < numShortcuts; row++) {
                                    if (shortcuts[row][SHORTCUT].equals(matcher.group(1))) {
                                        query = query.substring(0, matcher.start(1)) + shortcuts[row][ANNOTATION_TYPE] + "." + shortcuts[row][FEATURE] + query.substring(matcher.end(1));
                                        matcher = pattern.matcher(query);
                                        break;
                                    }
                                }
                            }
                            parameters.put(Constants.CONTEXT_WINDOW, new Integer(contextWindow));
                            if (annotationSetToSearchIn.getSelectedItem().equals(Constants.ALL_SETS)) {
                                parameters.remove(Constants.ANNOTATION_SET_ID);
                            } else {
                                String annotationSet = (String) annotationSetToSearchIn.getSelectedItem();
                                parameters.put(Constants.ANNOTATION_SET_ID, annotationSet);
                            }
                            try {
                                if (searcher.search(query, parameters)) {
                                    searcher.next(noOfPatterns);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                thisInstance.setEnabled(true);
                            }
                            processFinished();
                            pageOfResults = 1;
                            titleResults.setText("Results - Page " + pageOfResults);
                            thisInstance.setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    /**
   * Finds out the next few results.
   */
    protected class NextResultAction extends AbstractAction {

        private static final long serialVersionUID = 3257005436719871288L;

        public NextResultAction() {
            super("", MainFrame.getIcon("annic-forward"));
            super.putValue(SHORT_DESCRIPTION, "Show Next Patterns.");
            super.putValue(MNEMONIC_KEY, KeyEvent.VK_RIGHT);
        }

        public void actionPerformed(ActionEvent ae) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    thisInstance.setEnabled(false);
                    try {
                        searcher.next(((Number) numberOfResultsSpinner.getValue()).intValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                        thisInstance.setEnabled(true);
                    }
                    processFinished();
                    pageOfResults++;
                    titleResults.setText("Results - Page " + pageOfResults);
                    thisInstance.setEnabled(true);
                }
            });
        }
    }

    /**
   * Manage the features to display in the pattern row.
   */
    protected class ManageFeaturesToDisplayAction extends AbstractAction {

        private static final long serialVersionUID = 1L;

        public ManageFeaturesToDisplayAction() {
            super("", MainFrame.getIcon("add.gif"));
            super.putValue(SHORT_DESCRIPTION, "Display the features manager.");
        }

        public void actionPerformed(ActionEvent e) {
            if (featuresManager == null) {
                featuresManager = new FeaturesManager("Features Manager");
                featuresManager.setIconImage(((ImageIcon) MainFrame.getIcon("add.gif")).getImage());
                featuresManager.setLocationRelativeTo(thisInstance);
                featuresManager.setSize(10, 10);
                featuresManager.validate();
                featuresManager.pack();
            }
            featuresManager.setVisible(false);
            featuresManager.setVisible(true);
        }
    }

    /**
   * Modify the query with the current clicked pattern row.
   */
    protected class AddPatternRowInQueryMouseInputListener extends javax.swing.event.MouseInputAdapter {

        private String type;

        private String feature;

        private String text;

        private int position;

        public static final int LEFT = 0;

        public static final int CENTER = 1;

        public static final int RIGHT = 2;

        public AddPatternRowInQueryMouseInputListener(String type, String feature, String text, int position) {
            this.type = type;
            this.feature = feature;
            this.text = text;
            this.position = position;
        }

        public AddPatternRowInQueryMouseInputListener(String text) {
            this.type = null;
            this.feature = null;
            this.text = text;
            this.position = -1;
        }

        public void mouseClicked(MouseEvent me) {
            int caretPosition = newQuery.getCaretPosition();
            String query = newQuery.getText();
            String queryMiddle;
            if (type != null && feature != null) {
                queryMiddle = "{" + type + "." + feature + "==\"" + text + "\"}";
                for (int row = 0; row < numShortcuts; row++) {
                    if (shortcuts[row][ANNOTATION_TYPE].equals(type) && shortcuts[row][FEATURE].equals(feature)) {
                        queryMiddle = "{" + shortcuts[row][SHORTCUT] + "==\"" + text + "\"}";
                        break;
                    }
                }
            } else {
                queryMiddle = text;
            }
            if (position == LEFT) {
                newQuery.setText(queryMiddle + query);
            } else if (position == RIGHT) {
                newQuery.setText(query + queryMiddle);
            } else {
                String queryLeft = (newQuery.getSelectionStart() == newQuery.getSelectionEnd()) ? query.substring(0, caretPosition) : query.substring(0, newQuery.getSelectionStart());
                String queryRight = (newQuery.getSelectionStart() == newQuery.getSelectionEnd()) ? query.substring(caretPosition, query.length()) : query.substring(newQuery.getSelectionEnd(), query.length());
                newQuery.setText(queryLeft + queryMiddle + queryRight);
            }
        }
    }

    /**
   * Table model for the Pattern Tables.
   */
    protected class PatternsTableModel extends AbstractTableModel {

        /**
     * serial version id
     */
        private static final long serialVersionUID = 3977012959534854193L;

        /** Returns the number of rows (patterns) in the table */
        public int getRowCount() {
            return patterns.size();
        }

        /** Number of columns in table */
        public int getColumnCount() {
            return 5;
        }

        /** Column headings */
        public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case DOC_NAME_COLUMN:
                    return "Document";
                case ANNOTATION_SET_NAME_COLUMN:
                    return "AnnotationSet";
                case LEFT_CONTEXT_COLUMN:
                    return "Left Context";
                case PATTERN_COLUMN:
                    return "Pattern";
                case RIGHT_CONTEXT_COLUMN:
                    return "Right Context";
                default:
                    return "?";
            }
        }

        /** Returns the class of the column object */
        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex) {
                case DOC_NAME_COLUMN:
                    return String.class;
                case ANNOTATION_SET_NAME_COLUMN:
                    return String.class;
                case LEFT_CONTEXT_COLUMN:
                    return String.class;
                case PATTERN_COLUMN:
                    return String.class;
                case RIGHT_CONTEXT_COLUMN:
                    return String.class;
                default:
                    return Object.class;
            }
        }

        /** None of the cells in the table are editable */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        /** Finally return an object for the corresponding cell in the table */
        public Object getValueAt(int rowIndex, int columnIndex) {
            Pattern aResult = (Pattern) patterns.get(rowIndex);
            switch(columnIndex) {
                case DOC_NAME_COLUMN:
                    return aResult.getDocumentID();
                case ANNOTATION_SET_NAME_COLUMN:
                    return aResult.getAnnotationSetName();
                case LEFT_CONTEXT_COLUMN:
                    return aResult.getPatternText(aResult.getLeftContextStartOffset(), aResult.getStartOffset());
                case PATTERN_COLUMN:
                    return aResult.getPatternText(aResult.getStartOffset(), aResult.getEndOffset());
                case RIGHT_CONTEXT_COLUMN:
                    return aResult.getPatternText(aResult.getEndOffset(), aResult.getRightContextEndOffset());
                default:
                    return Object.class;
            }
        }

        private static final int DOC_NAME_COLUMN = 3;

        private static final int ANNOTATION_SET_NAME_COLUMN = 4;

        private static final int LEFT_CONTEXT_COLUMN = 0;

        private static final int PATTERN_COLUMN = 1;

        private static final int RIGHT_CONTEXT_COLUMN = 2;
    }

    /**
   * Panel that show a table of shortcut, annotation type and feature
   * to display in the pattern row.
   */
    protected class FeaturesManager extends JFrame {

        private static final long serialVersionUID = 1L;

        private JTable featuresJTable;

        public FeaturesManager(String title) {
            super(title);
            setLayout(new BorderLayout());
            JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            featuresJTable = new JTable(new FeatureManagerTableModel());
            DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
            renderer.setToolTipText("Tick to display this row in the pattern row.");
            featuresJTable.getColumnModel().getColumn(DISPLAY).setCellRenderer(renderer);
            featuresJTable.getColumnModel().getColumn(SHORTCUT).setCellEditor(new DefaultCellEditor(new JTextField(10)));
            featuresJTable.getColumnModel().getColumn(ANNOTATION_TYPE).setCellEditor(new DefaultCellEditor(annotTypesBox));
            featuresJTable.getColumnModel().getColumn(FEATURE).setCellEditor(new DefaultCellEditor(featuresBox));
            String[] s = { "Crop start", "Crop end" };
            featuresJTable.getColumnModel().getColumn(CROP).setCellEditor(new DefaultCellEditor(new JComboBox(s)));
            featuresJTable.getColumnModel().getColumn(columnNames.length).setCellRenderer(new removeButtonRenderer());
            scrollPane.setViewportView(featuresJTable);
            add(scrollPane, BorderLayout.CENTER);
        }
    }

    /**
   * Table model for the feature manager.
   */
    protected class FeatureManagerTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 3977012959534854193L;

        private final int REMOVE = columnNames.length;

        public int getRowCount() {
            return numShortcuts + 1;
        }

        public int getColumnCount() {
            return columnNames.length + 1;
        }

        public String getColumnName(int col) {
            if (col == REMOVE) {
                return "Remove";
            } else {
                return columnNames[col];
            }
        }

        public boolean isCellEditable(int row, int col) {
            if (row == getRowCount() - 1 && col == REMOVE) {
                return false;
            } else {
                return true;
            }
        }

        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public Object getValueAt(int row, int col) {
            if (col == DISPLAY) {
                return (shortcuts[row][col] == null || shortcuts[row][col].equals("true")) ? new Boolean(true) : new Boolean(false);
            } else if (col == REMOVE) {
                return (row == getRowCount() - 1) ? "" : "remove";
            } else {
                if (shortcuts[row][col] == null) {
                    return "";
                } else {
                    return shortcuts[row][col];
                }
            }
        }

        public void setValueAt(Object value, int row, int col) {
            if (col == REMOVE) {
                return;
            }
            if (value == null) {
                return;
            }
            shortcuts[row][col] = value.toString();
            fireTableRowsUpdated(row, row);
            if (row == getRowCount() - 1 && shortcuts[row][SHORTCUT] != null && shortcuts[row][ANNOTATION_TYPE] != null && shortcuts[row][FEATURE] != null) {
                if (shortcuts[row][DISPLAY] == null) {
                    shortcuts[row][DISPLAY] = "true";
                }
                if (shortcuts[row][CROP] == null) {
                    shortcuts[row][CROP] = "Crop end";
                }
                numShortcuts++;
                fireTableRowsInserted(row + 1, row + 1);
                featuresManager.pack();
            }
        }
    }

    /**
   * Use to add a button in the last row of the FeatureManagerTable.
   */
    public class removeButtonRenderer extends JButton implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (!value.equals("remove")) {
                setIcon(null);
                setEnabled(false);
                return this;
            }
            setIcon(MainFrame.getIcon("delete.gif"));
            setEnabled(true);
            if (isSelected && hasFocus) {
                setSelected(true);
                for (int row2 = row + 1; row2 < numShortcuts; row2++) {
                    for (int col2 = 0; col2 < columnNames.length; col2++) {
                        shortcuts[row2 - 1][col2] = shortcuts[row2][col2];
                        if (row2 == numShortcuts - 1) {
                            shortcuts[row2][col2] = null;
                        }
                    }
                }
                numShortcuts--;
                ((FeatureManagerTableModel) table.getModel()).fireTableStructureChanged();
            }
            return this;
        }
    }

    /**
   * Called by the GUI when this viewer/editor has to initialise itself
   * for a specific object.
   * 
   * @param target the object (be it a {@link gate.Resource},
   *          {@link gate.DataStore}or whatever) this viewer has to
   *          display
   */
    public void setTarget(Object target) {
        if (!(target instanceof LuceneDataStoreImpl) && !(target instanceof Searcher)) {
            throw new IllegalArgumentException("The GATE LuceneDataStoreSearchGUI can only be used with a GATE LuceneDataStores!\n" + target.getClass().toString() + " is not a GATE LuceneDataStore or an object of Searcher!");
        }
        this.target = target;
        if (this.target instanceof LuceneDataStoreImpl) {
            ((LuceneDataStoreImpl) this.target).addDatastoreListener(this);
            corpusToSearchIn.setEnabled(true);
            annotationSetToSearchIn.setEnabled(true);
            this.searcher = ((LuceneDataStoreImpl) this.target).getSearcher();
            allPatterns.setEnabled(true);
            URL indexLocationURL = (URL) ((LuceneDataStoreImpl) target).getIndexer().getParameters().get(Constants.INDEX_LOCATION_URL);
            String location = null;
            try {
                location = new File(indexLocationURL.toURI()).getAbsolutePath();
            } catch (URISyntaxException use) {
                location = new File(indexLocationURL.getFile()).getAbsolutePath();
            }
            try {
                annotationSetIDsFromDataStore = ((LuceneDataStoreImpl) this.target).getSearcher().getIndexedAnnotationSetNames(location);
                allAnnotTypesAndFeaturesFromDatastore = ((LuceneDataStoreImpl) this.target).getSearcher().getAnnotationTypesMap();
            } catch (SearchException se) {
                throw new GateRuntimeException(se);
            }
            try {
                java.util.List corpusPIds = ((LuceneDataStoreImpl) this.target).getLrIds(SerialCorpusImpl.class.getName());
                if (corpusIds != null) {
                    for (int i = 0; i < corpusPIds.size(); i++) {
                        String name = ((LuceneDataStoreImpl) this.target).getLrName(corpusPIds.get(i));
                        this.corpusIds.add(corpusPIds.get(i));
                        ((DefaultComboBoxModel) corpusToSearchIn.getModel()).addElement(name);
                    }
                }
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        corpusToSearchIn.updateUI();
                        if (corpusIds.size() > 0) corpusToSearchIn.setSelectedIndex(0);
                    }
                });
            } catch (PersistenceException pe) {
            }
        } else {
            this.searcher = (Searcher) this.target;
            corpusToSearchIn.setEnabled(false);
            try {
                annotationSetIDsFromDataStore = this.searcher.getIndexedAnnotationSetNames(null);
                allAnnotTypesAndFeaturesFromDatastore = this.searcher.getAnnotationTypesMap();
                TreeSet<String> ts = new TreeSet<String>();
                for (String aSetName : annotationSetIDsFromDataStore) {
                    ts.add(aSetName.substring(aSetName.indexOf(";") + 1));
                }
                annotationSetToSearchIn.setModel(new DefaultComboBoxModel(ts.toArray()));
                annotationSetToSearchIn.addItem(Constants.ALL_SETS);
                annotationSetToSearchIn.setSelectedItem(Constants.ALL_SETS);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        annotationSetToSearchIn.updateUI();
                        if (annotationSetIDsFromDataStore.length > 0) {
                            annotationSetToSearchIn.setSelectedIndex(0);
                        }
                    }
                });
            } catch (SearchException pe) {
                throw new GateRuntimeException(pe);
            }
        }
        annotationSetToSearchIn.setEnabled(true);
        executeQuery.setEnabled(true);
        nextResults.setEnabled(true);
        newQuery.setEnabled(true);
        numberOfResultsSpinner.setEnabled(true);
        contextSizeSpinner.setEnabled(true);
        clearQueryTF.setEnabled(true);
        updateDisplay();
    }

    public void progressChanged(int i) {
    }

    /**
   * Called when the process is finished, fires a refresh for this VR.
   */
    public void processFinished() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                executeQuery.setEnabled(true);
                nextResults.setEnabled(true);
                newQuery.setEnabled(true);
                if (target instanceof LuceneDataStoreImpl) {
                    corpusToSearchIn.setEnabled(true);
                    allPatterns.setEnabled(true);
                    annotationSetToSearchIn.setEnabled(true);
                } else {
                    corpusToSearchIn.setEnabled(false);
                }
                numberOfResultsSpinner.setEnabled(true);
                contextSizeSpinner.setEnabled(true);
                clearQueryTF.setEnabled(true);
                updateDisplay();
            }
        });
    }

    /**
   * This method is called by datastore when a new resource is adopted
   */
    public void resourceAdopted(DatastoreEvent de) {
    }

    /**
   * This method is called by datastore when an existing resource is
   * deleted
   */
    public void resourceDeleted(DatastoreEvent de) {
        Resource resource = de.getResource();
        if (resource instanceof Corpus) {
            Object id = de.getResourceID();
            int index = corpusIds.indexOf(id);
            if (index < 0) {
                return;
            }
            index++;
            ((DefaultComboBoxModel) corpusToSearchIn.getModel()).removeElementAt(index);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    corpusToSearchIn.updateUI();
                }
            });
        }
        URL indexLocationURL = (URL) ((LuceneDataStoreImpl) target).getIndexer().getParameters().get(Constants.INDEX_LOCATION_URL);
        String location = null;
        try {
            location = new File(indexLocationURL.toURI()).getAbsolutePath();
        } catch (URISyntaxException use) {
            location = new File(indexLocationURL.getFile()).getAbsolutePath();
        }
        try {
            annotationSetIDsFromDataStore = ((LuceneDataStoreImpl) this.target).getSearcher().getIndexedAnnotationSetNames(location);
            allAnnotTypesAndFeaturesFromDatastore = ((LuceneDataStoreImpl) this.target).getSearcher().getAnnotationTypesMap();
            updateAnnotationSetsToSearchInBox();
        } catch (SearchException se) {
            throw new GateRuntimeException(se);
        }
    }

    /**
   * This method is called when a resource is written into the datastore
   */
    public void resourceWritten(DatastoreEvent de) {
        Resource resource = de.getResource();
        if (resource instanceof Corpus) {
            Object id = de.getResourceID();
            if (!corpusIds.contains(id)) {
                corpusIds.add(id);
                ((DefaultComboBoxModel) corpusToSearchIn.getModel()).addElement(resource.getName());
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        corpusToSearchIn.updateUI();
                    }
                });
            }
            URL indexLocationURL = (URL) ((LuceneDataStoreImpl) target).getIndexer().getParameters().get(Constants.INDEX_LOCATION_URL);
            String location = null;
            try {
                location = new File(indexLocationURL.toURI()).getAbsolutePath();
            } catch (URISyntaxException use) {
                location = new File(indexLocationURL.getFile()).getAbsolutePath();
            }
            try {
                annotationSetIDsFromDataStore = ((LuceneDataStoreImpl) this.target).getSearcher().getIndexedAnnotationSetNames(location);
                allAnnotTypesAndFeaturesFromDatastore = ((LuceneDataStoreImpl) this.target).getSearcher().getAnnotationTypesMap();
                updateAnnotationSetsToSearchInBox();
            } catch (SearchException se) {
                throw new GateRuntimeException(se);
            }
        }
    }
}
