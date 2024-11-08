package magictool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Vector;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.BevelBorder;
import magictool.filefilters.GifFilter;
import magictool.filefilters.NoEditFileChooser;

/**
 * PlotFrame is a frame which displays a graph of data from the GraphDisplay class.
 * The class also shows information about the genes selected in the data and allows
 * the user to change several graph settings including normalizing the data. PlotFrame
 * permits the user to print the graph and to save it as a gif image.
 */
public class PlotFrame extends JInternalFrame {

    private JMenuBar jMenuBar = new JMenuBar();

    private JMenu jMenu1 = new JMenu();

    private JMenuItem printMenu = new JMenuItem();

    private JMenuItem closeMenu = new JMenuItem();

    private JMenuItem undoZoom = new JMenuItem();

    private JMenuItem origData = new JMenuItem();

    private JMenuItem regress = new JMenuItem();

    private JMenuItem saveImage = new JMenuItem();

    private JMenuItem saveGrp = new JMenuItem();

    private JMenuItem changeShape = new JMenuItem();

    private JMenuItem changePointSize = new JMenuItem();

    private JMenu jMenu2 = new JMenu();

    private JMenuItem titleMenu = new JMenuItem();

    private JMenuItem xMenu = new JMenuItem();

    private JMenuItem yMenu = new JMenuItem();

    private JMenu searchMenu = new JMenu();

    private JMenuItem selectChromo = new JMenuItem();

    private JMenuItem selectComment = new JMenuItem();

    private JMenuItem selectFunction = new JMenuItem();

    private JMenuItem selectProcess = new JMenuItem();

    private JMenuItem selectComponent = new JMenuItem();

    private JMenu jMenu3 = new JMenu();

    private JMenuItem reset = new JMenuItem();

    private JMenu jMenu4 = new JMenu();

    private JMenuItem normChoice = new JMenuItem();

    private JComboBox geneSelector;

    private JLabel position;

    private JLabel selectedGene;

    private JPanel lowerPanel;

    private JSplitPane split;

    private JPanel mainPanel;

    /**graph plot of the data*/
    protected GraphDisplay plotPanel;

    /**panel containing table of information about selected genes*/
    protected GenePanel genePanel;

    /**group file containing group of genes to be graphed from expression file*/
    protected GrpFile group;

    /**expression file to be graphed*/
    protected ExpFile expMain;

    /**minimum x-value of the data*/
    protected double xmin = 0;

    /**maximum x-value of the data*/
    protected double xmax = 0;

    /**minimum y-value of the data*/
    protected double ymin = 0;

    /**maximum y-value of the data*/
    protected double ymax = 0;

    /**x-value data for the graph*/
    public double[] x;

    /**y-value data for the graph*/
    public double[] y;

    /**whether or not to draw a best fit OLS regression line of the data*/
    protected boolean regression = false;

    /**whether or not to the graph is a plot of two columns of data - one on the x-axis and one on the y-axis*/
    protected boolean columnPlot = false;

    /**first column number for two column plot*/
    protected int col1 = 0;

    /**second column number for two column plot*/
    protected int col2 = 0;

    /**parent frame*/
    protected Frame parentFrame;

    private ActionListener geneListener;

    private Project project;

    /**
     * Constructs a plot frame from a given group file of genes from the user specified
     * expression file.
     * @param group group file containing group of genes to be plotted
     * @param expMain expression file to be plotted
     * @param parentFrame parent frame
     * @param project open project
     */
    public PlotFrame(GrpFile group, ExpFile expMain, Frame parentFrame, Project project) {
        this.group = group;
        this.expMain = expMain;
        this.parentFrame = parentFrame;
        this.project = project;
        genePanel = new GenePanel(expMain, new Vector());
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        if (group.hasTitle()) {
            plotPanel = new GraphDisplay(group.getTitle(), "Labels", "Expression Level");
        } else {
            plotPanel = new GraphDisplay("Group Plot", "Labels", "Expression Level");
        }
        plotPanel.setPlotFrame(this);
        this.setClosable(true);
        this.setMaximizable(true);
        this.setResizable(true);
        this.setSize(400, 300);
        this.setMinimumSize(new Dimension(300, 300));
        lowerPanel = new JPanel(new GridLayout(1, 2));
        JPanel selectPanel = new JPanel(new BorderLayout());
        position = new JLabel(" ");
        selectedGene = new JLabel("Selected: ");
        geneSelector = new JComboBox();
        geneSelector.addItem("Select Gene");
        Object gnames[];
        if (group == null || group.getNumGenes() == 0) {
            gnames = expMain.getGeneVector();
        } else gnames = group.getGroup();
        for (int i = 0; i < gnames.length; i++) {
            geneSelector.addItem(gnames[i].toString());
        }
        selectPanel.add(selectedGene, BorderLayout.WEST);
        selectPanel.add(geneSelector, BorderLayout.CENTER);
        lowerPanel.add(position);
        lowerPanel.add(selectPanel);
        lowerPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
        plotPanel.setExpFileName(expMain.getName());
        System.out.println("Setting exp file");
        searchMenu.setText("Search");
        selectChromo.setText("Select Chromosome");
        selectChromo.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectChromo_actionPerformed(e);
            }
        });
        selectComment.setText("Gene Comment Contains");
        selectComment.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectComment_actionPerformed(e);
            }
        });
        selectFunction.setText("Gene Molecular Function Contains");
        selectFunction.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectFunction_actionPerformed(e);
            }
        });
        selectProcess.setText("Gene Biological Process Contains");
        selectProcess.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectProcess_actionPerformed(e);
            }
        });
        selectComponent.setText("Gene Cellular Component Contains");
        selectComponent.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectComponent_actionPerformed(e);
            }
        });
        jMenu1.setText("File");
        printMenu.setText("Print");
        printMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK));
        printMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                printMenu_actionPerformed(e);
            }
        });
        saveImage.setText("Save As Image...");
        saveImage.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveImage_actionPerformed(e);
            }
        });
        saveGrp.setText("Save Selected As Group...");
        saveGrp.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveGrp_actionPerformed(e);
            }
        });
        changeShape.setText("Change Point Shape");
        changeShape.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                changePointShape_actionPerformed(e);
            }
        });
        changePointSize.setText("Change Point Size");
        changePointSize.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                changePointSize_actionPerformed(e);
            }
        });
        undoZoom.setText("Undo Zoom");
        undoZoom.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                undoZoom_actionPerformed(e);
            }
        });
        origData.setText("Original Data");
        origData.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                origData_actionPerformed(e);
            }
        });
        closeMenu.setText("Close");
        closeMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
        closeMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeMenu_actionPerformed(e);
            }
        });
        jMenu2.setText("Plot Options");
        titleMenu.setText("Title Label");
        titleMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                titleMenu_actionPerformed(e);
            }
        });
        xMenu.setText("X-Axis Label");
        xMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                xMenu_actionPerformed(e);
            }
        });
        yMenu.setText("Y-Axis Label");
        yMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                yMenu_actionPerformed(e);
            }
        });
        jMenu3.setText("Plot View");
        reset.setText("Reset");
        reset.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                reset_actionPerformed(e);
            }
        });
        jMenu4.setText("Data");
        normChoice.setText("Normalize Data");
        normChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                normChoice_actionPerformed(e);
            }
        });
        regress.setEnabled(regression);
        regress.setText("Regression Data");
        regress.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                regress_actionPerformed(e);
            }
        });
        mainPanel = new JPanel(new BorderLayout());
        this.getContentPane().setLayout(new BorderLayout());
        mainPanel.add(plotPanel, BorderLayout.CENTER);
        mainPanel.add(lowerPanel, BorderLayout.SOUTH);
        split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.add(genePanel);
        split.add(mainPanel);
        this.getContentPane().add(split, BorderLayout.CENTER);
        split.setContinuousLayout(true);
        split.setDividerLocation(0);
        split.setOneTouchExpandable(true);
        jMenuBar.add(jMenu1);
        jMenuBar.add(jMenu2);
        jMenuBar.add(jMenu3);
        jMenuBar.add(jMenu4);
        jMenu1.add(saveImage);
        jMenu1.add(saveGrp);
        jMenu1.add(printMenu);
        jMenu1.addSeparator();
        jMenu1.add(closeMenu);
        jMenu2.add(titleMenu);
        jMenu2.add(xMenu);
        jMenu2.add(yMenu);
        jMenu2.addSeparator();
        jMenu2.add(changeShape);
        jMenu2.add(changePointSize);
        jMenu3.add(undoZoom);
        jMenu3.add(reset);
        jMenu4.add(normChoice);
        jMenu4.add(origData);
        jMenu4.add(regress);
        jMenuBar.add(searchMenu);
        searchMenu.add(selectChromo);
        searchMenu.add(selectComment);
        searchMenu.add(selectProcess);
        searchMenu.add(selectFunction);
        searchMenu.add(selectComponent);
        this.setJMenuBar(jMenuBar);
        undoZoom.setEnabled(false);
        setUpPlot();
        plotPanel.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                pp_mouseReleased();
            }

            public void mouseClicked(MouseEvent e) {
            }
        });
        plotPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {

            public void mouseMoved(MouseEvent e) {
                pp_mouseMoved(e);
            }
        });
        geneListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                geneSelector_actionPerformed(e);
            }
        };
        geneSelector.addActionListener(geneListener);
    }

    /**
     * sets the two columns for a two column plot and changes the plot style to the
     * two column format (one column on x-axis plotted against other column on y-axis)
     * @param col1 first column number
     * @param col2 second column number
     */
    public void setColumns(int col1, int col2) {
        columnPlot = true;
        this.col1 = col1;
        this.col2 = col2;
        plotPanel.setXLabel(expMain.getLabel(col1));
        plotPanel.setYLabel(expMain.getLabel(col2));
        setUpPlot();
    }

    /**
     * clears the columns for a two column plot and changes the plotting style back to normal
     */
    public void clearColumns() {
        columnPlot = false;
        plotPanel.setXLabel("Labels");
        plotPanel.setYLabel("Expression Level");
        setUpPlot();
    }

    /**
     * changes whether or not to show a best fit OLS regression line for the data
     * in the graph
     * @param show whether or not to show a best fit OLS regression line
     */
    public void showRegression(boolean show) {
        this.regression = show;
        setUpPlot();
        regress.setEnabled(regression);
    }

    /**
     * returns whether or not the graph is showing a best fit OLS regression line for the data
     * in the graph
     * @return whether or not the graph is showing a best fit OLS regression line
     */
    public boolean regressionShowing() {
        return regression;
    }

    /**
     * sets up the plot by placing the correct data in the graph display and setting
     * the view window to the minimums and maximums of the data. This method also sets up
     * the correct labels for the plot and adds a regression line if desired.
     */
    public void setUpPlot() {
        Object names[];
        if (group == null || group.getNumGenes() == 0) {
            names = expMain.getGeneVector();
        } else names = group.getGroup();
        plotPanel.clearData();
        double txmin = Double.POSITIVE_INFINITY, txmax = Double.NEGATIVE_INFINITY;
        ymin = Double.POSITIVE_INFINITY;
        ymax = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < names.length; i++) {
            double y[];
            double x[] = new double[1];
            if (columnPlot) {
                y = new double[1];
                int pos = expMain.findGeneName(names[i].toString());
                y[0] = expMain.getDataPoint(pos, col2);
                x[0] = expMain.getDataPoint(pos, col1);
                if (x[0] != Double.POSITIVE_INFINITY && x[0] != Double.NEGATIVE_INFINITY && x[0] != Double.NaN) {
                    if (x[0] > txmax) txmax = x[0];
                    if (x[0] < txmin) txmin = x[0];
                }
            } else {
                y = expMain.getData(names[i].toString());
                if (y.length == 1) {
                    double tdata = y[0];
                    y = new double[3];
                    y[0] = Double.POSITIVE_INFINITY;
                    y[1] = tdata;
                    y[2] = Double.POSITIVE_INFINITY;
                }
            }
            double tempmax = Double.NEGATIVE_INFINITY;
            double tempmin = Double.POSITIVE_INFINITY;
            for (int j = 0; j < y.length; j++) {
                if (y[j] != Double.POSITIVE_INFINITY && y[j] != Double.NEGATIVE_INFINITY && y[j] != Double.NaN) {
                    if (y[j] > tempmax) tempmax = y[j];
                    if (y[j] < tempmin) tempmin = y[j];
                }
            }
            if (tempmax != Double.POSITIVE_INFINITY && tempmax != Double.NEGATIVE_INFINITY && tempmax != Double.NaN) {
                if (tempmax > ymax) ymax = tempmax;
            }
            if (tempmin != Double.POSITIVE_INFINITY && tempmin != Double.NEGATIVE_INFINITY && tempmin != Double.NaN) {
                if (tempmin < ymin) ymin = tempmin;
            }
            if (columnPlot) {
                plotPanel.addData(names[i].toString(), x, y);
            } else plotPanel.addData(names[i].toString(), y);
        }
        Object la[] = expMain.getLabelArray();
        String[] xlabels = new String[la.length];
        for (int j = 0; j < la.length; j++) {
            xlabels[j] = la[j].toString();
        }
        if (la.length == 1) {
            xlabels = new String[3];
            xlabels[0] = "";
            xlabels[1] = la[0].toString();
            ;
            xlabels[2] = "";
        }
        if (regression) plotPanel.addRegression();
        if (xlabels != null && !columnPlot) plotPanel.setXLabels(xlabels); else plotPanel.clearXLabels();
        if (columnPlot) plotPanel.setGraphSize(txmin, txmax, ymin, ymax); else plotPanel.setGraphSize(0, (expMain.getColumns() == 1 ? 2 : expMain.getColumns() - 1), ymin, ymax);
        plotPanel.repaint();
    }

    private void printMenu_actionPerformed(ActionEvent e) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        PageFormat pf = pj.pageDialog(pj.defaultPage());
        pj.setPrintable(plotPanel, pf);
        if (pj.printDialog()) {
            try {
                pj.print();
            } catch (Exception PrintException) {
            }
        }
    }

    private void closeMenu_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    private void yMenu_actionPerformed(ActionEvent e) {
        String name;
        if ((name = (JOptionPane.showInputDialog(this, "Please Enter New Y-Axis Label:"))) != null) {
            plotPanel.setYLabel(name);
            plotPanel.repaint();
        }
    }

    private void xMenu_actionPerformed(ActionEvent e) {
        String name;
        if ((name = (JOptionPane.showInputDialog(this, "Please Enter New X-Axis Label:"))) != null) {
            plotPanel.setXLabel(name);
            plotPanel.repaint();
        }
    }

    private void titleMenu_actionPerformed(ActionEvent e) {
        String name;
        if ((name = (JOptionPane.showInputDialog(this, "Please Enter New Plot Title:"))) != null) {
            plotPanel.setTitle(name);
            plotPanel.repaint();
        }
    }

    private void saveImage_actionPerformed(ActionEvent e) {
        try {
            NoEditFileChooser jfc = new NoEditFileChooser(MainFrame.fileLoader.getFileSystemView());
            jfc.setFileFilter(new GifFilter());
            jfc.setDialogTitle("Create New Gif File...");
            jfc.setApproveButtonText("Select");
            File direct = new File(project.getPath() + "images" + File.separator);
            if (!direct.exists()) direct.mkdirs();
            jfc.setCurrentDirectory(direct);
            int result = jfc.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File fileobj = jfc.getSelectedFile();
                String name = fileobj.getPath();
                if (!name.endsWith(".gif")) name += ".gif";
                plotPanel.saveImage(name);
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(this, "Failed To Create Image");
        }
    }

    private void saveGrp_actionPerformed(ActionEvent e) {
        DefaultListModel groupModel = new DefaultListModel();
        JList groupGenes = new JList();
        groupGenes.setModel(groupModel);
        Object[] o = this.getSelected();
        if (o.length > 0) {
            for (int i = 0; i < o.length; i++) {
                groupModel.addElement(o[i].toString());
            }
            String s = JOptionPane.showInputDialog(parentFrame, "Enter The Group Name:");
            if (s != null) {
                GrpFile newGrp = new GrpFile(s);
                for (int i = 0; i < groupModel.size(); i++) {
                    newGrp.addOne(groupModel.elementAt(i));
                }
                if (!s.endsWith(".grp")) s += ".grp";
                newGrp.setExpFile(expMain.getName());
                try {
                    File file = new File(project.getPath() + expMain.getName() + File.separator + s);
                    int result = JOptionPane.YES_OPTION;
                    if (file.exists()) {
                        result = JOptionPane.showConfirmDialog(parentFrame, "The file " + file.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) file.delete();
                    }
                    if (result == JOptionPane.YES_OPTION) newGrp.writeGrpFile(project.getPath() + expMain.getName() + File.separator + s);
                } catch (DidNotFinishException e2) {
                    JOptionPane.showMessageDialog(parentFrame, "Error Writing Group File");
                }
                project.addFile(expMain.getName() + File.separator + s);
            }
        } else {
            JOptionPane.showMessageDialog(parentFrame, "No Genes Selected");
        }
    }

    private void reset_actionPerformed(ActionEvent e) {
        plotPanel.resetGraph();
        plotPanel.repaint();
        undoZoom.setEnabled(false);
    }

    private void undoZoom_actionPerformed(ActionEvent e) {
        plotPanel.undoGraphSizeChange();
        plotPanel.repaint();
        undoZoom.setEnabled(false);
    }

    private void normChoice_actionPerformed(ActionEvent e) {
        Vector s = plotPanel.selectedData;
        String selected[] = new String[s.size()];
        for (int i = 0; i < s.size(); i++) {
            selected[i] = s.elementAt(i).toString();
        }
        expMain = new ExpFile(expMain.getExpFile());
        expMain.normalize();
        setUpPlot();
        undoZoom.setEnabled(false);
        geneSelector.setSelectedIndex(0);
        plotPanel.selectData(selected);
    }

    private void regress_actionPerformed(ActionEvent e) {
        if (plotPanel.regressions != null && plotPanel.regressions.size() > 0) {
            double rd[] = plotPanel.getRegressionData(0);
            if (rd != null) {
                DecimalFormat df = new DecimalFormat("####.####");
                JOptionPane.showMessageDialog(this, "Equation: Y = " + df.format(rd[0]) + " + " + df.format(rd[1]) + "(X)\nR-Squared = " + df.format(rd[2]), "Regression Information", JOptionPane.INFORMATION_MESSAGE);
            } else JOptionPane.showMessageDialog(this, "Error! No Regressions Data.");
        } else JOptionPane.showMessageDialog(this, "Error! No Regressions Exist.");
    }

    private void origData_actionPerformed(ActionEvent e) {
        Vector s = plotPanel.selectedData;
        String selected[] = new String[s.size()];
        for (int i = 0; i < s.size(); i++) {
            selected[i] = s.elementAt(i).toString();
        }
        expMain = new ExpFile(expMain.getExpFile());
        setUpPlot();
        undoZoom.setEnabled(false);
        geneSelector.setSelectedIndex(0);
        plotPanel.selectData(selected);
    }

    private void changePointShape_actionPerformed(ActionEvent e) {
        String shape;
        String shapes[] = { "Square", "Circle" };
        if ((shape = ((String) JOptionPane.showInputDialog(this, "Select Shape:", "", JOptionPane.QUESTION_MESSAGE, null, shapes, shapes[0]))) != null) {
            try {
                int s = 0;
                if (shape.equals(shapes[1])) s = 1;
                plotPanel.setPointShape(s);
                plotPanel.repaint();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "Error! Invalid Number Entered");
            }
        }
    }

    private void changePointSize_actionPerformed(ActionEvent e) {
        String size;
        if ((size = ((String) JOptionPane.showInputDialog(this, "Enter Point Size:", "", JOptionPane.QUESTION_MESSAGE, null, null, "" + plotPanel.pointSize))) != null) {
            try {
                int num = Integer.parseInt(size);
                plotPanel.setPointSize(num);
                plotPanel.repaint();
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(this, "Error! Invalid Number Entered");
            }
        }
    }

    private void selectChromo_actionPerformed(ActionEvent e) {
        String chromos[] = expMain.getChromosomes();
        if (chromos.length == 0) {
            chromos = new String[1];
            chromos[0] = "No Chromosomes Listed";
        }
        String c = null;
        if ((c = ((String) JOptionPane.showInputDialog(this, "Select Chromosome:", "", JOptionPane.QUESTION_MESSAGE, null, chromos, chromos[0]))) != null) {
            if (!c.equals("No Chromosomes Listed")) {
                geneSelector.setSelectedIndex(0);
                Vector s = new Vector();
                Object gnames[] = group.getGroup();
                if (gnames.length == 0) gnames = expMain.getGeneNames();
                for (int i = 0; i < gnames.length; i++) {
                    Gene g = expMain.getGene(expMain.findGeneName(gnames[i].toString()));
                    if (g.getChromo() != null && g.getChromo().equalsIgnoreCase(c)) s.add(gnames[i]);
                }
                genePanel.setGene(s);
                String sel[] = new String[s.size()];
                for (int i = 0; i < s.size(); i++) {
                    sel[i] = s.elementAt(i).toString();
                }
                plotPanel.selectData(sel);
                plotPanel.selectedGene = "Selected Chromosome: " + c;
                plotPanel.repaint();
            }
        }
    }

    private void selectComment_actionPerformed(ActionEvent e) {
        String c = null;
        if ((c = ((String) JOptionPane.showInputDialog(this, "Comment Contains:"))) != null) {
            geneSelector.setSelectedIndex(0);
            Vector s = new Vector();
            Object gnames[] = group.getGroup();
            if (gnames.length == 0) gnames = expMain.getGeneNames();
            for (int i = 0; i < gnames.length; i++) {
                Gene g = expMain.getGene(expMain.findGeneName(gnames[i].toString()));
                if (g.getComments() != null && g.getComments().toLowerCase().indexOf(c.toLowerCase()) != -1) s.add(gnames[i]);
            }
            genePanel.setGene(s);
            String sel[] = new String[s.size()];
            for (int i = 0; i < s.size(); i++) {
                sel[i] = s.elementAt(i).toString();
            }
            plotPanel.selectData(sel);
            plotPanel.selectedGene = "Comment Contains: " + c;
            plotPanel.repaint();
        }
    }

    private void selectFunction_actionPerformed(ActionEvent e) {
        String c = null;
        if ((c = ((String) JOptionPane.showInputDialog(this, "Molecular Function Contains:"))) != null) {
            geneSelector.setSelectedIndex(0);
            Vector s = new Vector();
            Object gnames[] = group.getGroup();
            if (gnames.length == 0) gnames = expMain.getGeneNames();
            for (int i = 0; i < gnames.length; i++) {
                Gene g = expMain.getGene(expMain.findGeneName(gnames[i].toString()));
                if (g.getFunction() != null && g.getFunction().toLowerCase().indexOf(c.toLowerCase()) != -1) s.add(gnames[i]);
            }
            genePanel.setGene(s);
            String sel[] = new String[s.size()];
            for (int i = 0; i < s.size(); i++) {
                sel[i] = s.elementAt(i).toString();
            }
            plotPanel.selectData(sel);
            plotPanel.selectedGene = "Molecular Function Contains: " + c;
            plotPanel.repaint();
        }
    }

    private void selectComponent_actionPerformed(ActionEvent e) {
        String c = null;
        if ((c = ((String) JOptionPane.showInputDialog(this, "Cellular Component Contains:"))) != null) {
            geneSelector.setSelectedIndex(0);
            Vector s = new Vector();
            Object gnames[] = group.getGroup();
            if (gnames.length == 0) gnames = expMain.getGeneNames();
            for (int i = 0; i < gnames.length; i++) {
                Gene g = expMain.getGene(expMain.findGeneName(gnames[i].toString()));
                if (g.getComponent() != null && g.getComponent().toLowerCase().indexOf(c.toLowerCase()) != -1) s.add(gnames[i]);
            }
            genePanel.setGene(s);
            String sel[] = new String[s.size()];
            for (int i = 0; i < s.size(); i++) {
                sel[i] = s.elementAt(i).toString();
            }
            plotPanel.selectData(sel);
            plotPanel.selectedGene = "Cellular Component Contains: " + c;
            plotPanel.repaint();
        }
    }

    private void selectProcess_actionPerformed(ActionEvent e) {
        String c = null;
        if ((c = ((String) JOptionPane.showInputDialog(this, "Biological Process Contains:"))) != null) {
            geneSelector.setSelectedIndex(0);
            Vector s = new Vector();
            Object gnames[] = group.getGroup();
            if (gnames.length == 0) gnames = expMain.getGeneNames();
            for (int i = 0; i < gnames.length; i++) {
                Gene g = expMain.getGene(expMain.findGeneName(gnames[i].toString()));
                if (g.getProcess() != null && g.getProcess().toLowerCase().indexOf(c.toLowerCase()) != -1) s.add(gnames[i]);
            }
            genePanel.setGene(s);
            String sel[] = new String[s.size()];
            for (int i = 0; i < s.size(); i++) {
                sel[i] = s.elementAt(i).toString();
            }
            plotPanel.selectData(sel);
            plotPanel.selectedGene = "Biological Process Contains: " + c;
            plotPanel.repaint();
        }
    }

    private void pp_mouseReleased() {
        if (plotPanel.canUndo() && !undoZoom.isEnabled()) undoZoom.setEnabled(true);
    }

    /**
     * updates the selected gene(s) when the mouse is clicked on plot panel
     */
    protected void pp_mouseClicked() {
        geneSelector.removeActionListener(geneListener);
        if (plotPanel.selectedData.size() > 0) {
            if (plotPanel.selectedData.size() == 1) {
                for (int i = 1; i < geneSelector.getItemCount(); i++) {
                    if (geneSelector.getItemAt(i).toString().equalsIgnoreCase(plotPanel.selectedData.elementAt(0).toString())) {
                        geneSelector.setSelectedIndex(i);
                        break;
                    }
                }
            } else geneSelector.setSelectedIndex(0);
            genePanel.setGene(plotPanel.selectedData);
        } else {
            geneSelector.setSelectedIndex(0);
            genePanel.setGene(new Vector());
        }
        geneSelector.addActionListener(geneListener);
    }

    private void pp_mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (plotPanel.inGraph(x, y)) {
            DecimalFormat xformat = plotPanel.getDecimalFormat(plotPanel.xmin, plotPanel.xmax);
            DecimalFormat yformat = plotPanel.getDecimalFormat(plotPanel.ymin, plotPanel.ymax);
            if (!plotPanel.useXLabels) position.setText("x:" + xformat.format(plotPanel.xPosToValue(x)) + " y:" + yformat.format(plotPanel.yPosToValue(y))); else {
                int closest;
                if (x < plotPanel.xValueToPos(0)) closest = 0; else if ((x > plotPanel.xValueToPos(plotPanel.xMarks.length - 1))) closest = plotPanel.xMarks.length - 1; else closest = Math.round((float) plotPanel.xPosToValue(x));
                position.setText("x:" + plotPanel.xMarks[closest] + " y:" + yformat.format(plotPanel.yPosToValue(y)));
            }
        } else {
            position.setText(" ");
        }
    }

    private void geneSelector_actionPerformed(ActionEvent e) {
        if (e.getSource().equals(this)) {
        } else {
            if (geneSelector.getSelectedIndex() != 0) {
                plotPanel.selectData(geneSelector.getSelectedItem().toString());
                plotPanel.selectedGene = "Selected Gene: " + geneSelector.getSelectedItem().toString();
                Vector v = new Vector();
                v.addElement(geneSelector.getSelectedItem().toString());
                genePanel.setGene(v);
            } else {
                plotPanel.clearSelectedGene();
                plotPanel.selectedGene = null;
                genePanel.setGene(new Vector());
            }
        }
    }

    /**
     * gets the array of selected genes
     * @return array of selected genes
     */
    public String[] getSelected() {
        return plotPanel.getSelectedData();
    }
}
