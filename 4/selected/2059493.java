package vademecum.learner.esomMapper;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.java.plugin.registry.ExtensionPoint;
import vademecum.Core;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.BestMatch;
import vademecum.data.DataGrid;
import vademecum.data.GridUtils;
import vademecum.data.IClusterNumber;
import vademecum.data.IColumn;
import vademecum.data.IDataGrid;
import vademecum.data.IDataRow;
import vademecum.data.IRetina;
import vademecum.data.Retina;
import vademecum.data.TypeMismatchException;
import vademecum.extensionPoint.DefaultDataNode;
import vademecum.io.reader.Lrn;
import vademecum.learner.esom.EsomIO;
import vademecum.learner.esom.MyRetina;
import vademecum.math.distance.IDistance;
import vademecum.ui.project.Expertice;

public class EsomMapper extends DefaultDataNode implements ActionListener {

    /**
	 * A static variable to get an unique filename for saving and loading
	 */
    private static int instance = 0;

    /**
	 * A variable for the actual instance, non static
	 */
    private int actInstance;

    /**
	 * internal Logger
	 */
    private static Log log = LogFactory.getLog(EsomMapper.class);

    private IDataGrid inputVectors;

    private IRetina inputRetina, outputRetina;

    private JFileChooser lrnFileChooser, wtsFileChooser;

    private JTextField lrnTextField, wtsTextField;

    private JDialog prefDialog;

    private String lrnWorkingDir;

    private String wtsWorkingDir;

    private JRadioButton lrnPreviousRadio;

    private JRadioButton lrnLoadRadio;

    private JRadioButton lrnSelectRadio;

    private JRadioButton wtsPreviousRadio;

    private JRadioButton wtsLoadRadio;

    private JRadioButton wtsSelectRadio;

    private long progressIndex;

    private long numOfVecs;

    private String message;

    private IDistance distFunc;

    private Double[][] inputArray;

    private JTree lrnTree;

    private JTree wtsTree;

    public static final String PROPERTY_INSTANCE = "EsomMapper_instance";

    public static final String APPLY_ACTION = "Apply";

    public static final String CANCEL_ACTION = "Cancel";

    public static final String RESET_ACTION = "Reset";

    public static final String BROWSE_LRN_ACTION = "Browse LRN";

    public static final String BROWSE_WTS_ACTION = "Browse WTS";

    public static final String PROPERTY_LRN_FILE = "lrnFile";

    public static final String PROPERTY_WTS_FILE = "wtsFile";

    public static final String PROPERTY_LRN_RADIO_SELECTED = "selectedLrnRadio";

    public static final String PROPERTY_WTS_RADIO_SELECTED = "selectedWtsRadio";

    public static final String RADIO_PREVIOUS_SELECTED = "1";

    public static final String RADIO_LOAD_SELECTED = "2";

    public static final String RADIO_SELECT_FROM_DATANAV_SELECTED = "3";

    public static final String PROPERTY_LRN_NODE = "lrnNode";

    public static final String PROPERTY_WTS_NODE = "wtsNode";

    @Override
    public String getName() {
        return "ESOM Mapper";
    }

    @Override
    public Object getOutput(Class outputType) {
        if (outputType.equals(Retina.class) || outputType.equals(IRetina.class)) return outputRetina;
        if (outputType.equals(IDataGrid.class) || outputType.equals(DataGrid.class)) return outputRetina.getInputVectors();
        log.error("there's something wrong in the getOutput method! (or XML parsing error?)");
        return null;
    }

    @Override
    public JDialog getPreferencesDialog(Frame owner) {
        prefDialog = new JDialog(owner, "ESOM Mapper preferences");
        prefDialog.setLayout(new BorderLayout());
        JPanel prefPanelTop = new JPanel();
        ;
        GridBagLayout prefPanelTopLayout = new GridBagLayout();
        prefPanelTop.setLayout(prefPanelTopLayout);
        prefPanelTop.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        JLabel selectData = new JLabel("Select data");
        c.gridx = 0;
        c.gridy = 0;
        prefPanelTopLayout.setConstraints(selectData, c);
        prefPanelTop.add(selectData);
        lrnPreviousRadio = new JRadioButton("Previous data");
        c.gridy = 1;
        prefPanelTopLayout.setConstraints(lrnPreviousRadio, c);
        prefPanelTop.add(lrnPreviousRadio);
        lrnLoadRadio = new JRadioButton("Load data");
        c.gridy = 2;
        prefPanelTopLayout.setConstraints(lrnLoadRadio, c);
        prefPanelTop.add(lrnLoadRadio);
        lrnTextField = new JTextField(this.getProperty(EsomMapper.PROPERTY_LRN_FILE), 20);
        c.gridx = 1;
        prefPanelTopLayout.setConstraints(lrnTextField, c);
        prefPanelTop.add(lrnTextField);
        JButton lrnLoadButton = new JButton("Browse");
        lrnLoadButton.setActionCommand(EsomMapper.BROWSE_LRN_ACTION);
        lrnLoadButton.addActionListener(this);
        c.gridx = 2;
        prefPanelTopLayout.setConstraints(lrnLoadButton, c);
        prefPanelTop.add(lrnLoadButton);
        lrnSelectRadio = new JRadioButton("Select from DataNav");
        c.gridx = 0;
        c.gridy = 3;
        prefPanelTopLayout.setConstraints(lrnSelectRadio, c);
        prefPanelTop.add(lrnSelectRadio);
        lrnTree = exploreDataNav(IDataGrid.class);
        JScrollPane lrnTreeScrollPane = new JScrollPane(lrnTree);
        c.gridy = 4;
        c.gridwidth = 3;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        prefPanelTopLayout.setConstraints(lrnTreeScrollPane, c);
        if (this.getExperimentNode().getParent().getParent() != null) {
            prefPanelTop.add(lrnTreeScrollPane);
        }
        ButtonGroup lrnRadioGroup = new ButtonGroup();
        lrnRadioGroup.add(lrnPreviousRadio);
        lrnRadioGroup.add(lrnLoadRadio);
        lrnRadioGroup.add(lrnSelectRadio);
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        c.gridx = 3;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 5, 0, 5);
        prefPanelTopLayout.setConstraints(sep, c);
        prefPanelTop.add(sep);
        JLabel selectRetina = new JLabel("Select retina");
        c.gridx = 4;
        c.gridy = 0;
        c.gridheight = 1;
        c.insets = new Insets(0, 0, 0, 0);
        prefPanelTopLayout.setConstraints(selectRetina, c);
        prefPanelTop.add(selectRetina);
        wtsPreviousRadio = new JRadioButton("Previous Retina");
        c.gridy = 1;
        prefPanelTopLayout.setConstraints(wtsPreviousRadio, c);
        prefPanelTop.add(wtsPreviousRadio);
        wtsLoadRadio = new JRadioButton("Load Retina");
        c.gridy = 2;
        prefPanelTopLayout.setConstraints(wtsLoadRadio, c);
        prefPanelTop.add(wtsLoadRadio);
        wtsTextField = new JTextField(this.getProperty(EsomMapper.PROPERTY_WTS_FILE), 20);
        c.gridx = 5;
        prefPanelTopLayout.setConstraints(wtsTextField, c);
        prefPanelTop.add(wtsTextField);
        JButton wtsLoadButton = new JButton("Browse");
        wtsLoadButton.setActionCommand(EsomMapper.BROWSE_WTS_ACTION);
        wtsLoadButton.addActionListener(this);
        c.gridx = 6;
        prefPanelTopLayout.setConstraints(wtsLoadButton, c);
        prefPanelTop.add(wtsLoadButton);
        wtsSelectRadio = new JRadioButton("Select from DataNav");
        c.gridx = 4;
        c.gridy = 3;
        prefPanelTopLayout.setConstraints(wtsSelectRadio, c);
        prefPanelTop.add(wtsSelectRadio);
        wtsTree = exploreDataNav(IRetina.class);
        JScrollPane wtsTreeScrollPane = new JScrollPane(wtsTree);
        wtsTreeScrollPane.setMaximumSize(new Dimension(100, 200));
        c.gridy = 4;
        c.gridwidth = 3;
        c.gridheight = 2;
        c.fill = GridBagConstraints.BOTH;
        prefPanelTopLayout.setConstraints(wtsTreeScrollPane, c);
        if (this.getExperimentNode().getParent().getParent() != null) {
            prefPanelTop.add(wtsTreeScrollPane);
        }
        ButtonGroup wtsRadioGroup = new ButtonGroup();
        wtsRadioGroup.add(wtsPreviousRadio);
        wtsRadioGroup.add(wtsLoadRadio);
        wtsRadioGroup.add(wtsSelectRadio);
        this.resetDialog();
        JPanel prefPanelBottom = new JPanel(new FlowLayout());
        JButton acceptButton = new JButton("Apply");
        acceptButton.setActionCommand(EsomMapper.APPLY_ACTION);
        acceptButton.addActionListener(this);
        prefPanelBottom.add(acceptButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(EsomMapper.CANCEL_ACTION);
        cancelButton.addActionListener(this);
        prefPanelBottom.add(cancelButton);
        JButton resetButton = new JButton("Reset");
        resetButton.setActionCommand(EsomMapper.RESET_ACTION);
        resetButton.addActionListener(this);
        prefPanelBottom.add(resetButton);
        prefDialog.add(prefPanelTop, BorderLayout.PAGE_START);
        prefDialog.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.CENTER);
        prefDialog.add(prefPanelBottom, BorderLayout.PAGE_END);
        prefDialog.pack();
        prefDialog.setResizable(false);
        return prefDialog;
    }

    @Override
    public String getResultText() {
        String result = "EsomMapper successful finished\n" + this.numOfVecs + " input vectors mapped.";
        return result;
    }

    @Override
    public boolean hasFinished() {
        if (progressIndex == numOfVecs) return true;
        return false;
    }

    @Override
    public void init() throws Exception {
        log.debug("Selected lrn radiobutton: " + this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED));
        log.debug("Selected wts radiobutton: " + this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED));
        if (this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED).equalsIgnoreCase(String.valueOf(EsomMapper.RADIO_LOAD_SELECTED))) {
            log.debug("loading lrn file " + this.getProperty(EsomMapper.PROPERTY_LRN_FILE) + " ...");
            try {
                new FileReader(this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
                Lrn lrnReader = new Lrn();
                File tempFile = new File(this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
                lrnReader.setProperty("file", tempFile.getName());
                lrnReader.load(new File(tempFile.getParent()));
                this.inputVectors = (IDataGrid) lrnReader.getOutput(IDataGrid.class);
                log.debug("loading of lrn file" + this.getProperty(EsomMapper.PROPERTY_LRN_FILE) + " done");
            } catch (FileNotFoundException e) {
                throw new Exception("lrn file not found!");
            }
        }
        if (this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED).equalsIgnoreCase(String.valueOf(EsomMapper.RADIO_LOAD_SELECTED))) {
            log.debug("loading wts file " + this.getProperty(EsomMapper.PROPERTY_WTS_FILE) + " ...");
            try {
                new FileReader(this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
                MyRetina myRetina = EsomIO.loadWTSFile(this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
                this.inputRetina = myRetina.toRetina();
                log.debug("loading of wts file" + this.getProperty(EsomMapper.PROPERTY_WTS_FILE) + " done");
            } catch (FileNotFoundException e) {
                throw new Exception("wts file not found!");
            }
        }
        if (this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED).equalsIgnoreCase(EsomMapper.RADIO_SELECT_FROM_DATANAV_SELECTED)) {
            long lrnNodeID = ((EsomMapperNodeIdentifier) ((DefaultMutableTreeNode) lrnTree.getLastSelectedPathComponent()).getUserObject()).getId();
            ExperimentNode lrnExperimentNode = rekursiveSearchExperimentNodeInTree((ExperimentNode) this.getExperimentNode().getTreePath().getPath()[0], lrnNodeID);
            this.inputVectors = (IDataGrid) lrnExperimentNode.getMethod().getOutput(IDataGrid.class);
        }
        if (this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED).equalsIgnoreCase(EsomMapper.RADIO_SELECT_FROM_DATANAV_SELECTED)) {
            long wtsNodeID = ((EsomMapperNodeIdentifier) ((DefaultMutableTreeNode) wtsTree.getLastSelectedPathComponent()).getUserObject()).getId();
            ExperimentNode wtsExperimentNode = rekursiveSearchExperimentNodeInTree((ExperimentNode) this.getExperimentNode().getTreePath().getPath()[0], wtsNodeID);
            this.inputRetina = (IRetina) wtsExperimentNode.getMethod().getOutput(IRetina.class);
        }
        int dimInputVectors = 0;
        for (int i = 0; i < this.inputVectors.getNumCols(); i++) {
            log.debug("Type of Column: " + this.inputVectors.getColumn(i).getType());
            if (this.inputVectors.getColumn(i).getType() == Double.class) dimInputVectors++;
        }
        log.debug("Dimensions of input vectors: " + dimInputVectors);
        log.debug("Dimensions of input retina : " + this.inputRetina.getDim());
        if (dimInputVectors != this.inputRetina.getDim()) {
            log.error("Dimensions do not match!!!");
            throw new Exception("Dimension sizes do not match! " + "Dimensions of the input vectors: " + dimInputVectors + "," + "dimensions of the input retina:" + this.inputRetina.getDim() + ".");
        }
        this.outputRetina = new Retina(this.inputRetina.getNumRows(), this.inputRetina.getNumCols(), this.inputRetina.getDim(), this.inputRetina.isToroid(), this.inputRetina.getDistanceFunction());
        this.outputRetina.setInputVectors(this.inputVectors.copy());
        progressIndex = 0;
        numOfVecs = this.inputVectors.getNumRows();
        distFunc = this.outputRetina.getDistanceFunction();
        inputArray = inputVectors.doubleColsToDoubleArray();
        message = "copy retina";
        fireProgressChangedEvent(0, message);
        try {
            for (int i = 0; i < this.outputRetina.getNumRows(); i++) {
                for (int j = 0; j < this.outputRetina.getNumCols(); j++) {
                    outputRetina.setPoint(i, j, ((Vector<Double>) this.inputRetina.getPoint(i, j)).clone());
                }
            }
        } catch (IndexOutOfBoundsException e) {
            throw new Exception("Index out of bound while copying the retina!");
        } catch (TypeMismatchException e) {
            throw new Exception("Type mismatch while copying the retina!");
        }
        message = "mapping";
    }

    private ExperimentNode rekursiveSearchExperimentNodeInTree(ExperimentNode eNode, long nodeID) {
        if (eNode.getId() == nodeID) return eNode; else {
            for (ExperimentNode child : eNode.getChildren()) {
                ExperimentNode searchedNode = rekursiveSearchExperimentNodeInTree(child, nodeID);
                if (searchedNode != null) return searchedNode;
            }
        }
        return null;
    }

    @Override
    public void iterate() {
        fireProgressChangedEvent((int) Math.round((float) progressIndex / (float) numOfVecs * 100.0), message);
        double minDist = Double.MAX_VALUE;
        double actDist;
        int minX = 0;
        int minY = 0;
        for (int i = 0; i < this.outputRetina.getNumRows(); i++) {
            for (int j = 0; j < this.outputRetina.getNumCols(); j++) {
                actDist = distFunc.getDistance(this.outputRetina.getPointasDoubleArray(i, j), this.inputArray[(int) progressIndex]);
                if (actDist < minDist) {
                    minDist = actDist;
                    minX = i;
                    minY = j;
                }
            }
        }
        this.outputRetina.addBestMatch(new BestMatch(minX, minY, this.inputVectors.getKey((int) progressIndex), new Double(minDist)));
        progressIndex++;
    }

    @Override
    public void load(File folder) {
        actInstance = Integer.parseInt(this.getProperty(EsomMapper.PROPERTY_INSTANCE));
        if (EsomMapper.instance <= this.actInstance) EsomMapper.instance = this.actInstance + 1;
        if (this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED).equalsIgnoreCase(String.valueOf(EsomMapper.RADIO_LOAD_SELECTED))) {
            log.debug("correcting lrn filename to " + folder.getAbsolutePath() + File.separator + this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
            this.setProperty(EsomMapper.PROPERTY_LRN_FILE, folder.getAbsolutePath() + File.separator + this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
        }
        if (this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED).equalsIgnoreCase(String.valueOf(EsomMapper.RADIO_LOAD_SELECTED))) {
            log.debug("correcting wts filename to " + folder.getAbsolutePath() + File.separator + this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
            this.setProperty(EsomMapper.PROPERTY_WTS_FILE, folder.getAbsolutePath() + File.separator + this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
        }
        try {
            init();
            while (!hasFinished()) iterate();
        } catch (Exception e) {
            log.error(e.getMessage());
            JOptionPane.showMessageDialog(Core.getProjectPanel(), "Load Error in EsomMapper: " + e.getMessage(), "I/O Error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        }
        if (this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED).equalsIgnoreCase(String.valueOf(EsomMapper.RADIO_LOAD_SELECTED))) {
            File tempFile = new File(this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
            log.debug("setting lrn filename back to " + tempFile.getName());
            this.setProperty(EsomMapper.PROPERTY_LRN_FILE, tempFile.getName());
        }
        if (this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED).equalsIgnoreCase(String.valueOf(EsomMapper.RADIO_LOAD_SELECTED))) {
            File tempFile = new File(this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
            log.debug("setting wts filename back to " + tempFile.getName());
            this.setProperty(EsomMapper.PROPERTY_WTS_FILE, tempFile.getName());
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void save(File folder) {
        actInstance = instance;
        this.setProperty(EsomMapper.PROPERTY_INSTANCE, String.valueOf(actInstance));
        log.debug("instance: " + this.getProperty(EsomMapper.PROPERTY_INSTANCE));
        if (this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED) == EsomMapper.RADIO_LOAD_SELECTED) {
            File src = new File(this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
            if (src.getParent() != folder.getPath()) {
                log.debug("saving lrn file in save folder " + folder.getPath());
                File dst = new File(folder.getAbsolutePath() + File.separator + src.getName() + String.valueOf(actInstance));
                try {
                    FileReader fr = new FileReader(src);
                    BufferedReader br = new BufferedReader(fr);
                    dst.createNewFile();
                    FileWriter fw = new FileWriter(dst);
                    BufferedWriter bw = new BufferedWriter(fw);
                    int i = 0;
                    while ((i = br.read()) != -1) bw.write(i);
                    bw.flush();
                    bw.close();
                    br.close();
                    fr.close();
                } catch (FileNotFoundException e) {
                    log.error("Error while opening lrn sourcefile! Saving wasn't possible!!!");
                    e.printStackTrace();
                } catch (IOException e) {
                    log.error("Error while creating lrn destfile! Creating wasn't possible!!!");
                    e.printStackTrace();
                }
                this.setProperty(EsomMapper.PROPERTY_LRN_FILE, dst.getName());
                log.debug("done saving lrn file");
            }
        }
        if (this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED) == EsomMapper.RADIO_LOAD_SELECTED) {
            File src = new File(this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
            if (src.getParent() != folder.getPath()) {
                log.debug("saving wts file in save folder " + folder.getPath());
                File dst = new File(folder.getAbsolutePath() + File.separator + src.getName() + String.valueOf(actInstance));
                try {
                    FileReader fr = new FileReader(src);
                    BufferedReader br = new BufferedReader(fr);
                    dst.createNewFile();
                    FileWriter fw = new FileWriter(dst);
                    BufferedWriter bw = new BufferedWriter(fw);
                    int i = 0;
                    while ((i = br.read()) != -1) bw.write(i);
                    bw.flush();
                    bw.close();
                    br.close();
                    fr.close();
                } catch (FileNotFoundException e) {
                    log.error("Error while opening wts sourcefile! Saving wasn't possible!!!");
                    e.printStackTrace();
                } catch (IOException e) {
                    log.error("Error while creating wts destfile! Creating wasn't possible!!!");
                    e.printStackTrace();
                }
                this.setProperty(EsomMapper.PROPERTY_WTS_FILE, dst.getName());
                log.debug("done saving wts file");
            }
        }
        if (this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED) == EsomMapper.RADIO_SELECT_FROM_DATANAV_SELECTED) {
            this.setProperty(EsomMapper.PROPERTY_LRN_FILE, "EsomMapper" + this.actInstance + ".lrn");
            File dst = new File(folder + File.separator + this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
            try {
                FileWriter fw = new FileWriter(dst);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("# EsomMapper LRN save file\n");
                bw.write("% " + this.inputVectors.getNumRows() + "\n");
                bw.write("% " + this.inputVectors.getNumCols() + "\n");
                bw.write("% 9");
                for (IColumn col : this.inputVectors.getColumns()) {
                    if (col.getType() == IClusterNumber.class) bw.write("\t2"); else if (col.getType() == String.class) bw.write("\t8"); else bw.write("\t1");
                }
                bw.write("\n% Key");
                for (IColumn col : this.inputVectors.getColumns()) {
                    bw.write("\t" + col.getLabel());
                }
                bw.write("\n");
                int keyIterator = 0;
                for (Vector<Object> row : this.inputVectors.getGrid()) {
                    bw.write(this.inputVectors.getKey(keyIterator++).toString());
                    for (Object point : row) bw.write("\t" + point.toString());
                    bw.write("\n");
                }
                bw.flush();
                fw.flush();
                bw.close();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.setProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED, EsomMapper.RADIO_LOAD_SELECTED);
        }
        if (this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED) == EsomMapper.RADIO_SELECT_FROM_DATANAV_SELECTED) {
            this.setProperty(EsomMapper.PROPERTY_WTS_FILE, "EsomMapper" + this.actInstance + ".wts");
            MyRetina tempRetina = new MyRetina(this.outputRetina.getNumRows(), this.outputRetina.getNumCols(), this.outputRetina.getDim(), this.outputRetina.getDistanceFunction(), this.outputRetina.isToroid());
            for (int row = 0; row < this.outputRetina.getNumRows(); row++) {
                for (int col = 0; col < this.outputRetina.getNumCols(); col++) {
                    for (int dim = 0; dim < this.outputRetina.getDim(); dim++) {
                        tempRetina.setNeuron(row, col, dim, this.outputRetina.getPointasDoubleArray(row, col)[dim]);
                    }
                }
            }
            EsomIO.writeWTSFile(folder + File.separator + this.getProperty(EsomMapper.PROPERTY_WTS_FILE), tempRetina);
            this.setProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED, EsomMapper.RADIO_LOAD_SELECTED);
        }
        EsomMapper.instance++;
    }

    @Override
    public void setInput(Class inputType, Object data) {
        log.debug("setInput: " + inputType);
        if (inputType.equals(IDataGrid.class)) {
            this.inputVectors = (IDataGrid) data;
        }
        if (inputType.equals(IRetina.class)) {
            this.inputRetina = (IRetina) data;
        }
        if (inputType.equals(Retina.class)) {
            this.inputRetina = (Retina) data;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(EsomMapper.APPLY_ACTION)) {
            log.debug("apply button pressed");
            if (lrnLoadRadio.isSelected()) {
                if (lrnTextField.getText() == null || lrnTextField.getText() == "") {
                    JOptionPane.showMessageDialog(this.prefDialog, "No data file selected!");
                    return;
                } else {
                    File file = new File(lrnTextField.getText());
                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(this.prefDialog, "data file does not exist!");
                        return;
                    }
                }
            }
            if (wtsLoadRadio.isSelected()) {
                if (wtsTextField.getText() == null || wtsTextField.getText() == "") {
                    JOptionPane.showMessageDialog(this.prefDialog, "No retina file selected!");
                    return;
                } else {
                    File file = new File(wtsTextField.getText());
                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(this.prefDialog, "retina file does not exist!");
                        return;
                    }
                }
            }
            if (lrnSelectRadio.isSelected()) {
                if (lrnTree.getLastSelectedPathComponent() == null) {
                    JOptionPane.showMessageDialog(this.prefDialog, "No node selected! Please select a node for data vector input.");
                    return;
                } else {
                    if (!((EsomMapperNodeIdentifier) ((DefaultMutableTreeNode) lrnTree.getLastSelectedPathComponent()).getUserObject()).isOutputOK()) {
                        JOptionPane.showMessageDialog(this.prefDialog, "Node contains no output that is usable as data vectors! Please select only nodes with a green flag.");
                        return;
                    }
                }
            }
            if (wtsSelectRadio.isSelected()) {
                if (wtsTree.getLastSelectedPathComponent() == null) {
                    JOptionPane.showMessageDialog(this.prefDialog, "No node selected! Please select a node for retina input.");
                    return;
                } else {
                    if (!((EsomMapperNodeIdentifier) ((DefaultMutableTreeNode) wtsTree.getLastSelectedPathComponent()).getUserObject()).isOutputOK()) {
                        JOptionPane.showMessageDialog(this.prefDialog, "Node contains no output that is usable as retina! Please select only nodes with a green flag.");
                        return;
                    }
                }
            }
            log.debug("input seems sane");
            saveProperties();
            this.firePropertiesChangedEvent();
            this.closeDialog();
        }
        if (e.getActionCommand().equals(EsomMapper.CANCEL_ACTION)) {
            log.debug("cancel button pressed");
            resetDialog();
            closeDialog();
        }
        if (e.getActionCommand().equals(EsomMapper.RESET_ACTION)) {
            log.debug("reset button pressed");
            resetDialog();
        }
        if (e.getActionCommand().equals(EsomMapper.BROWSE_LRN_ACTION)) {
            log.debug("lrn browse button pressed");
            if (lrnFileChooser == null) {
                lrnFileChooser = new JFileChooser(this.lrnWorkingDir);
                lrnFileChooser.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "*.lrn Data file";
                    }

                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".lrn");
                    }
                });
                lrnFileChooser.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource().equals(lrnFileChooser)) {
                            log.debug("lrnFileChooser Action");
                        }
                        if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                            lrnTextField.setText(lrnFileChooser.getSelectedFile().getPath());
                            lrnWorkingDir = lrnFileChooser.getSelectedFile().getParent();
                            log.debug("lrn working dir set to '" + lrnWorkingDir + "'");
                        }
                    }
                });
            }
            lrnFileChooser.showDialog(EsomMapper.this.prefDialog, "Load");
        }
        if (e.getActionCommand().equals(EsomMapper.BROWSE_WTS_ACTION)) {
            log.debug("wts browse button pressed");
            if (wtsFileChooser == null) {
                wtsFileChooser = new JFileChooser(wtsWorkingDir);
                wtsFileChooser.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "*.wts Data file";
                    }

                    @Override
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().endsWith(".wts");
                    }
                });
                wtsFileChooser.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (e.getSource() == (wtsFileChooser)) {
                            log.debug("wtsFileChooser Action");
                        }
                        if (e.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                            wtsTextField.setText(wtsFileChooser.getSelectedFile().getPath());
                            wtsWorkingDir = wtsFileChooser.getSelectedFile().getParent();
                            log.debug("wts working dir set to " + wtsWorkingDir + "'");
                        }
                    }
                });
            }
            wtsFileChooser.showDialog(EsomMapper.this.prefDialog, "Load");
        }
    }

    private void saveProperties() {
        if (this.lrnPreviousRadio.isSelected()) this.setProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED, EsomMapper.RADIO_PREVIOUS_SELECTED);
        if (this.lrnLoadRadio.isSelected()) {
            this.setProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED, EsomMapper.RADIO_LOAD_SELECTED);
            this.setProperty(EsomMapper.PROPERTY_LRN_FILE, this.lrnTextField.getText());
        }
        if (this.lrnSelectRadio.isSelected()) {
            this.setProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED, EsomMapper.RADIO_SELECT_FROM_DATANAV_SELECTED);
            this.setProperty(EsomMapper.PROPERTY_LRN_NODE, Long.toString(((EsomMapperNodeIdentifier) ((DefaultMutableTreeNode) lrnTree.getLastSelectedPathComponent()).getUserObject()).getId()));
        }
        if (this.wtsPreviousRadio.isSelected()) this.setProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED, EsomMapper.RADIO_PREVIOUS_SELECTED);
        if (this.wtsLoadRadio.isSelected()) {
            this.setProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED, EsomMapper.RADIO_LOAD_SELECTED);
            this.setProperty(EsomMapper.PROPERTY_WTS_FILE, this.wtsTextField.getText());
        }
        if (this.wtsSelectRadio.isSelected()) {
            this.setProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED, EsomMapper.RADIO_SELECT_FROM_DATANAV_SELECTED);
            this.setProperty(EsomMapper.PROPERTY_LRN_NODE, Long.toString(((EsomMapperNodeIdentifier) ((DefaultMutableTreeNode) wtsTree.getLastSelectedPathComponent()).getUserObject()).getId()));
        }
    }

    private void closeDialog() {
        prefDialog.dispose();
    }

    private void resetDialog() {
        if (this.getExperimentNode().getParent().getParent() == null) {
            lrnSelectRadio.setEnabled(false);
            wtsSelectRadio.setEnabled(false);
        }
        if (this.inputVectors != null) lrnPreviousRadio.setSelected(true); else {
            lrnPreviousRadio.setEnabled(false);
            lrnLoadRadio.setSelected(true);
        }
        if (this.inputRetina != null) wtsPreviousRadio.setSelected(true); else {
            wtsPreviousRadio.setEnabled(false);
            wtsLoadRadio.setSelected(true);
        }
        String s = this.getProperty(EsomMapper.PROPERTY_LRN_RADIO_SELECTED);
        if (s != null) {
            int selectedLrnRadio = Integer.valueOf(s);
            switch(selectedLrnRadio) {
                case 1:
                    this.lrnPreviousRadio.setSelected(true);
                    break;
                case 2:
                    this.lrnLoadRadio.setSelected(true);
                    break;
                case 3:
                    this.lrnSelectRadio.setSelected(true);
                    break;
            }
        }
        s = this.getProperty(EsomMapper.PROPERTY_WTS_RADIO_SELECTED);
        if (s != null) {
            int selectedLrnRadio = Integer.valueOf(s);
            switch(selectedLrnRadio) {
                case 1:
                    this.wtsPreviousRadio.setSelected(true);
                    break;
                case 2:
                    this.wtsLoadRadio.setSelected(true);
                    break;
                case 3:
                    this.wtsSelectRadio.setSelected(true);
                    break;
            }
        }
        this.lrnTextField.setText(this.getProperty(EsomMapper.PROPERTY_LRN_FILE));
        this.wtsTextField.setText(this.getProperty(EsomMapper.PROPERTY_WTS_FILE));
        this.prefDialog.repaint();
    }

    private JTree exploreDataNav(Class dataType) {
        log.debug("Building a tree, dataType is " + dataType.getName());
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Experiment");
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        root.setUserObject(new EsomMapperNodeIdentifier("Experiment", -1, false));
        TreePath path = this.eNode.getTreePath();
        ExperimentNode rootENode = (ExperimentNode) path.getPathComponent(0);
        log.debug("Starting to explore the DataNav tree at " + rootENode.getName());
        for (ExperimentNode node : rootENode.getChildren()) {
            log.debug("adding '" + node.getName() + "' to 'Experiment'");
            if (node.getId() != this.eNode.getId()) root.add(rekursiveNodeAdd(node, dataType));
        }
        JTree tree = new JTree(root);
        this.expandAll(tree, true);
        tree.setCellRenderer(new EsomMapperTreeCellRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        return tree;
    }

    private MutableTreeNode rekursiveNodeAdd(ExperimentNode node, Class dataType) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(node.getName());
        boolean outputOK = false;
        StringBuffer outputTypes = new StringBuffer();
        for (String s : node.getOutputTypes()) {
            outputTypes.append(s + ";");
            if (s.equalsIgnoreCase(dataType.getName())) outputOK = true;
        }
        log.debug("Output types of " + node.getName() + ": " + outputTypes.toString());
        if (outputOK) log.debug("node contains the right outputType");
        newNode.setUserObject(new EsomMapperNodeIdentifier(node.getName(), node.getId(), outputOK));
        log.debug("node identifier of " + " set to id: " + newNode);
        for (ExperimentNode childNode : node.getChildren()) {
            log.debug("adding '" + childNode.getName() + "' to '" + node.getName() + "'");
            if (childNode.getId() != this.eNode.getId()) newNode.add(rekursiveNodeAdd(childNode, dataType));
        }
        return newNode;
    }

    public void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }
}

class EsomMapperNodeIdentifier {

    private long id;

    private boolean outputOK;

    private String name;

    public EsomMapperNodeIdentifier(String name, long id, boolean outputOK) {
        this.name = name;
        this.id = id;
        this.outputOK = outputOK;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isOutputOK() {
        return outputOK;
    }

    public void setOutputOK(boolean outputOK) {
        this.outputOK = outputOK;
    }

    @Override
    public String toString() {
        return "**" + name;
    }
}

class EsomMapperTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
	 * 
	 */
    private static final long serialVersionUID = -5442686078747266416L;

    private JTree tree;

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);
        this.tree = tree;
        this.hasFocus = hasFocus;
        setText(stringValue);
        if (sel) setForeground(getTextSelectionColor()); else setForeground(getTextNonSelectionColor());
        setEnabled(true);
        setIcon(getIcon((DefaultMutableTreeNode) value));
        setComponentOrientation(tree.getComponentOrientation());
        selected = sel;
        return this;
    }

    private Icon getIcon(DefaultMutableTreeNode value) {
        if (((EsomMapperNodeIdentifier) value.getUserObject()).isOutputOK()) return new ImageIcon("data/images/flag_green.gif"); else return new ImageIcon("data/images/flag_red.gif");
    }
}
