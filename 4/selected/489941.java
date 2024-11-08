package de.ibis.permoto.gui.result.panes.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import org.apache.log4j.Logger;
import org.freehep.swing.ExtensionFileFilter;
import de.ibis.permoto.gui.result.panes.TabbedPane;
import de.ibis.permoto.gui.result.panes.panels.tables.DataTableModel;
import de.ibis.permoto.gui.result.util.GradientLabel;
import de.ibis.permoto.model.definitions.IBusinessCase;
import de.ibis.permoto.result.Result;
import de.ibis.permoto.result.ResultCollection;
import de.ibis.permoto.result.ResultLoader;
import de.ibis.permoto.util.db.DBManager;

/**
 * The general information panel holds the overall information corresponding to
 * given scenario set ID.
 * <p>
 * It holds information about:
 * <li>The id of the solved model
 * <li>Type of solution
 * <li>User name
 * <li>Date
 * <li>The total throughput of the model.
 * @author Thomas Jansson
 * @author Guercan Karakoc
 * @author Oliver H�hn
 */
public class TextPanel extends AbstractResultPanel {

    /** Serial number. */
    private static final long serialVersionUID = -8273349106672735199L;

    /** Panel holding the input data. */
    private JPanel inputPanel;

    /** JTable holding the input data. */
    private JTable inputTable;

    /** Panel holding the result data. */
    private JPanel resultPanel;

    /** JTable holding the input data. */
    private JTable resultTable;

    static boolean drawDiagram = false;

    private JPanel northPanel;

    private JButton exportInputDataButton;

    private JButton exportResultDataButton;

    private JButton showDataButton;

    private JComboBox comboBoxOfLoadFactor;

    private Hashtable<String, Integer> executionIDTOLoadFactor = new Hashtable<String, Integer>();

    private StringBuffer inputData = null;

    private File lastDirectory;

    /** Logger for this class */
    private static final Logger logger = Logger.getLogger(TextPanel.class);

    public Result[] result = null;

    public StringBuffer resultData = new StringBuffer();

    public StringBuffer resultDataForExport = new StringBuffer();

    private JTable loadFactorTable;

    private Hashtable<String, JTable> jTablesOfLoadFactors = new Hashtable<String, JTable>();

    @SuppressWarnings("unused")
    private boolean isTableAlreadyCreated = false;

    private String scenarioName = "";

    private static String selectedLoadFactor;

    @SuppressWarnings("unused")
    private static Integer selectedExecutionID;

    /**
	 * Creates a new TextPanel in the given parent ResultWizard.
	 * @param parent - AbstractTabbedPane.
	 * @param result - AbstractResult.
	 * @param tabTitle - String
	 */
    public TextPanel(final TabbedPane parent, final Result[] result, final String tabTitle) {
        super(parent, result, tabTitle);
        this.scenarioName = this.getScenarioName();
        this.blueTitle = this.parentPane.getName() + "-" + result[0].getBlueTitle();
        this.initComponent();
    }

    /**
	 * Initializes the GUI of this panel.
	 */
    public final void initComponent() {
        this.inputPanel = this.makePanel();
        this.makeNorthPanel();
        this.makeInputPanel();
        this.makeResultPanel(null, null);
    }

    /**
	 * Creates the panel for the buttons.
	 */
    private void makeNorthPanel() {
        this.northPanel = new JPanel();
        this.northPanel.setLayout(new GridLayout(2, 1));
        JPanel northPanelRow1 = new JPanel();
        northPanelRow1.setLayout(new GridLayout(0, 1));
        JPanel northPanelRow2 = new JPanel();
        northPanelRow2.setLayout(new GridLayout(0, 6));
        northPanelRow1.add(this.makeBlueTitle(this.blueTitle));
        northPanelRow2.add(new JLabel("INPUT:", JLabel.LEFT));
        northPanelRow2.add(new JLabel("Select LoadFactor:", JLabel.RIGHT));
        this.makeLoadFactorComboBox();
        northPanelRow2.add(this.comboBoxOfLoadFactor);
        this.makeExportButtons();
        northPanelRow2.add(this.exportInputDataButton);
        northPanelRow2.add(this.exportResultDataButton);
        this.makeShowLoadFactorButton();
        northPanelRow2.add(this.showDataButton);
        this.northPanel.add(northPanelRow1);
        this.northPanel.add(northPanelRow2);
        this.inputPanel.add(this.northPanel, BorderLayout.NORTH);
    }

    /**
	 * Creates the panel that shows the input data.
	 */
    private void makeInputPanel() {
        this.inputTable = this.makeInputTable();
        final JScrollPane scrollInput = new JScrollPane(this.inputTable);
        this.inputTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        this.inputPanel.add(scrollInput, BorderLayout.CENTER);
        this.add(this.inputPanel, BorderLayout.CENTER);
    }

    /**
	 * Creates the panel that shows the result table.
	 * @param table JTable the table to show
	 * @param loadfactor String the current loadfactor to show
	 */
    private void makeResultPanel(JTable table, String loadfactor) {
        this.resultPanel = this.makePanel();
        this.resultPanel.add(new JLabel("RESULT:", JLabel.LEFT), BorderLayout.NORTH);
        if (table == null) {
            this.resultTable = this.makeResultTable(loadfactor);
        } else {
            this.resultTable = table;
        }
        final JScrollPane scrollResult = new JScrollPane(this.resultTable);
        this.resultTable.setPreferredScrollableViewportSize(new Dimension(500, 70));
        this.resultPanel.add(scrollResult, BorderLayout.CENTER);
        this.add(this.resultPanel, BorderLayout.SOUTH);
    }

    /**
	 * Get the name of this scenario
	 * @return String the name of the scenario
	 */
    private String getScenarioName() {
        DBManager db = DBManager.getInstance();
        String scenarioID = db.getScenarioIDOfSolutionID(this.parentPane.parentWizard.getSolutionID()[0]);
        String scenarioName = db.getScenarioNameOfScenarioID(scenarioID);
        return scenarioName;
    }

    private JLabel makeBlueTitle(String title) {
        JLabel label = new JLabel();
        label = new GradientLabel("<html><font size=4><b>" + this.scenarioName + "-" + title + "</b></color></html>");
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        label.setIconTextGap(10);
        return label;
    }

    private void makeExportButtons() {
        this.exportInputDataButton = new JButton("Export input data as csv");
        this.exportInputDataButton.addActionListener(new ExportInputDataListener());
        this.exportResultDataButton = new JButton("Export result data as csv");
        this.exportResultDataButton.addActionListener(new ExportResultDataListener());
    }

    /**
	 * Listener for action of button "export input data".
	 */
    private class ExportInputDataListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            TextPanel.this.createFile(TextPanel.this.inputData);
        }
    }

    /**
	 * Listener for action of button "export result data".
	 */
    private class ExportResultDataListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            TextPanel.this.makeResultTableToExport();
            TextPanel.this.createFile(TextPanel.this.resultDataForExport);
        }
    }

    /**
	 * to initialize the "show load factor" button
	 */
    private void makeShowLoadFactorButton() {
        this.showDataButton = new JButton("Arrivalrates/Populations");
        this.showDataButton.addActionListener(new ShowLoadFactorTable());
    }

    /**
	 * Save the data from the given table in a file.
	 * @param stringBuffer the input to save to file
	 */
    public void createFile(StringBuffer stringBuffer) {
        if (lastDirectory == null) {
            lastDirectory = new File(System.getProperty("user.dir"));
        }
        JFileChooser fileChooser = new JFileChooser(lastDirectory);
        ExtensionFileFilter filter = new ExtensionFileFilter("Excel files");
        filter.addExtension(".csv");
        fileChooser.setFileFilter(filter);
        int status = fileChooser.showSaveDialog(this);
        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "This file already exists. Do you want to overwrite it?", "Confirm overwrite", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    lastDirectory = selectedFile.getParentFile();
                    try {
                        writeFile(stringBuffer, selectedFile.getAbsolutePath());
                    } catch (Exception e) {
                        logger.info("writeFile() failed to save to file: " + selectedFile.getAbsolutePath(), e);
                    }
                }
            } else {
                lastDirectory = selectedFile.getParentFile();
                try {
                    writeFile(stringBuffer, selectedFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.info("writeFile() failed to save to file: " + selectedFile.getAbsolutePath(), e);
                }
            }
        }
    }

    /**
	 * Writes the data to a file.
	 * @param data the data to write to file
	 * @param absolutPath the file URL
	 */
    private void writeFile(StringBuffer data, String absolutePath) {
        try {
            File f = new File(absolutePath);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
            out.write(new String(data));
            out.close();
            System.gc();
        } catch (UnsupportedEncodingException e1) {
            logger.error("UnsopportedEncoding: " + e1);
        } catch (FileNotFoundException e1) {
            logger.error("FileNotFound: " + e1);
        } catch (IOException e1) {
            logger.error("IOException: " + e1);
        }
    }

    /**
	 * Creates a table in a pop-up window which shows the real load factor.
	 */
    private class ShowLoadFactorTable implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            JFrame frame = new JFrame("LoadFactor");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            String[] columnNames = { "Class Name", "Class Type", "Arrivalrate/Population", "Load Factor" };
            DBManager db = DBManager.getInstance();
            String[][] values = db.getRequestValue(TextPanel.this.parentPane.parentWizard.getExecutionIDs(), TextPanel.this.parentPane.parentWizard);
            TextPanel.this.loadFactorTable = new JTable(values, columnNames);
            TextPanel.this.loadFactorTable.setEnabled(false);
            TextPanel.this.loadFactorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(TextPanel.this.loadFactorTable);
            frame.getContentPane().add(scrollPane, BorderLayout.CENTER);
            frame.setSize(new Dimension(600, 400));
            frame.setVisible(true);
        }
    }

    /**
	 * You can invoke this method either you want to make a table of load
	 * factors or you want to export the result data in a excel file.
	 */
    private void makeResultTableToExport() {
        int columnLength = 0;
        int rowLength = 0;
        this.resultDataForExport = new StringBuffer();
        Enumeration<String> loadFactors = executionIDTOLoadFactor.keys();
        double[] loadFactorsArray = new double[executionIDTOLoadFactor.size()];
        logger.debug("Length of loadFactorsArray = " + loadFactorsArray.length);
        int n = 0;
        while (loadFactors.hasMoreElements()) {
            loadFactorsArray[n] = Double.parseDouble(loadFactors.nextElement());
            n++;
        }
        Arrays.sort(loadFactorsArray);
        for (int i = 0; i < loadFactorsArray.length; i++) {
            String nextLoadFactor = loadFactorsArray[i] + "";
            TextPanel.this.updateResults(nextLoadFactor);
            TextPanel.this.setResult(TextPanel.this.result);
            JTable table = null;
            if (TextPanel.this.jTablesOfLoadFactors.containsKey(nextLoadFactor)) {
                table = TextPanel.this.jTablesOfLoadFactors.get(nextLoadFactor);
            } else {
                table = this.makeResultTable(nextLoadFactor);
            }
            columnLength = table.getColumnCount();
            rowLength = table.getRowCount();
            if (i == 0) {
                for (int k = 0; k < columnLength; k++) {
                    this.resultDataForExport.append(table.getColumnName(k) + "; ");
                }
            }
            this.resultDataForExport.append(System.getProperty("line.separator"));
            for (int j = 0; j < rowLength; j++) {
                for (int m = 0; m < columnLength; m++) {
                    this.resultDataForExport.append(table.getValueAt(j, m).toString().replaceAll("\\.", ",") + "; ");
                }
                this.resultDataForExport.append(System.getProperty("line.separator"));
            }
            this.resultDataForExport.append(System.getProperty("line.separator"));
        }
        isTableAlreadyCreated = true;
    }

    /**
	 * A combo box to show the load factors which belong to the executionIDs
	 */
    private void makeLoadFactorComboBox() {
        DBManager db = DBManager.getInstance();
        Integer[] executionID = this.parentPane.parentWizard.getExecutionIDs();
        String[] loadFactors = db.getLoadFactorsOfExecutionIDs(executionID);
        this.comboBoxOfLoadFactor = new JComboBox(loadFactors);
        this.comboBoxOfLoadFactor.addItemListener(new SelectLoadFactorListener());
        for (int i = 0; i < executionID.length; i++) {
            this.executionIDTOLoadFactor.put(loadFactors[i], executionID[i]);
        }
    }

    /**
	 * Listener that is invoked when selecting a new loadfactor in the GUI.
	 */
    private class SelectLoadFactorListener implements ItemListener {

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                TextPanel.selectedLoadFactor = (String) TextPanel.this.comboBoxOfLoadFactor.getSelectedItem();
                if (TextPanel.this.jTablesOfLoadFactors.containsKey(selectedLoadFactor)) {
                    TextPanel.this.remove(TextPanel.this.resultPanel);
                    TextPanel.selectedExecutionID = TextPanel.this.executionIDTOLoadFactor.get(selectedLoadFactor);
                    TextPanel.this.makeResultPanel(TextPanel.this.jTablesOfLoadFactors.get(selectedLoadFactor), selectedLoadFactor);
                    TextPanel.this.getParentPane().getParentWizard().update();
                } else {
                    TextPanel.this.updateResults(selectedLoadFactor);
                    TextPanel.this.setResult(TextPanel.this.result);
                    TextPanel.this.remove(TextPanel.this.resultPanel);
                    TextPanel.this.makeResultPanel(null, selectedLoadFactor);
                    TextPanel.this.getParentPane().getParentWizard().update();
                }
            }
        }
    }

    /**
	 * Creates a JTable presenting the given data.
	 * @return JTable - filled with data;
	 */
    private JTable makeInputTable() {
        final Result[] results = this.getResult();
        final String[] nameTypes = results[0].getIdTypes();
        final String[] inputParameters = results[0].getInputParameterNames();
        final String[] columnNames = new String[nameTypes.length + inputParameters.length];
        System.arraycopy(nameTypes, 0, columnNames, 0, nameTypes.length);
        System.arraycopy(inputParameters, 0, columnNames, nameTypes.length, inputParameters.length);
        final String[][] data = new String[results.length][columnNames.length];
        this.inputData = new StringBuffer(data.length);
        for (int i = 0; i < results.length; i++) {
            final String[] name = results[i].getIDs();
            final String[] input = results[i].getInputData();
            System.arraycopy(name, 0, data[i], 0, name.length);
            System.arraycopy(input, 0, data[i], name.length, input.length);
        }
        final DataTableModel model = new DataTableModel(data, columnNames);
        final JTable table = new JTable(model);
        int columnLength = table.getColumnCount();
        int rowLength = table.getRowCount();
        for (int i = 0; i < columnLength; i++) {
            this.inputData.append(table.getColumnName(i) + ";");
        }
        this.inputData.append(System.getProperty("line.separator"));
        for (int k = 0; k < rowLength; k++) {
            for (int j = 0; j < columnLength; j++) {
                this.inputData.append(table.getValueAt(k, j) + ";");
            }
            this.inputData.append(System.getProperty("line.separator"));
        }
        table.setEnabled(false);
        return table;
    }

    /**
	 * Creates a JPanel and sets the layout of the panel.
	 * @return JPanel.
	 */
    private JPanel makePanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        return panel;
    }

    /**
	 * Creates a JTable presenting the results for this result instance.
	 * @return JTable - filled with data;
	 */
    private JTable makeResultTable(String loadfactor) {
        final Result[] results = this.getResult();
        final String[] nameTypes = results[0].getIdTypes();
        final String[] parameterNames = results[0].getResultParameterNames();
        final String[] columnNames = new String[nameTypes.length + parameterNames.length];
        System.arraycopy(nameTypes, 0, columnNames, 0, nameTypes.length);
        System.arraycopy(parameterNames, 0, columnNames, nameTypes.length, parameterNames.length);
        final String[][] data = new String[results.length][columnNames.length];
        for (int i = 0; i < results.length; i++) {
            final String[] name = results[i].getIDs();
            for (int m = 0; m < name.length; m++) {
            }
            System.arraycopy(name, 0, data[i], 0, name.length);
            for (int j = 0; j < parameterNames.length; j++) {
                final double[] value = results[i].getResultOf(parameterNames[j]);
                if (value.length != 0) {
                    data[i][name.length + j] = value[0] + "";
                }
            }
        }
        final DataTableModel model = new DataTableModel(data, columnNames);
        final JTable table = new JTable(model);
        if (loadfactor != null) this.jTablesOfLoadFactors.put(loadfactor, table);
        table.setEnabled(false);
        int columnLength = table.getColumnCount();
        int rowLength = table.getRowCount();
        for (int k = 0; k < columnLength; k++) {
            this.resultData.append(table.getColumnName(k) + "; ");
        }
        this.resultData.append(System.getProperty("line.separator"));
        for (int j = 0; j < rowLength; j++) {
            for (int m = 0; m < columnLength; m++) {
                this.resultData.append(table.getValueAt(j, m) + "; ");
            }
            this.resultData.append(System.getProperty("line.separator"));
        }
        this.resultData.append(System.getProperty("line.separator"));
        return table;
    }

    /**
	 * The result table will be updated if a new loadfactor was selected and if
	 * a excel file will be created.
	 * @param selectedLoadFactor
	 */
    public void updateResults(String selectedLoadFactor) {
        Integer[] executionID = new Integer[1];
        executionID[0] = this.executionIDTOLoadFactor.get(selectedLoadFactor);
        logger.info("selected ExecutionID = " + executionID[0]);
        IBusinessCase bc = DBManager.getInstance().getBusinessCaseByExecutionID(executionID);
        this.parentPane.parentWizard.setBc(bc);
        ResultLoader rl = ResultLoader.getInstance(this.parentPane.parentWizard.getBc());
        rl.setBc(bc);
        ResultCollection[] resultCollection = rl.getResultCollection(DBManager.getInstance().getSolutionIDOfExecutionID(executionID), executionID);
        logger.info("Lenght of ResultCollection: " + resultCollection.length);
        if (TextPanel.this.getName().equalsIgnoreCase("Scenario")) {
            this.result = resultCollection[0].getModelResult();
        } else if (TextPanel.this.getName().equalsIgnoreCase("Class")) {
            this.result = resultCollection[0].getClassResults();
        } else if (TextPanel.this.getName().equalsIgnoreCase("Station")) {
            this.result = resultCollection[0].getStationResults();
        } else if (TextPanel.this.getName().equalsIgnoreCase("ClassStation")) {
            this.result = resultCollection[0].getClassStationResults();
        }
    }
}
