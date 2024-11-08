package magictool;

import ij.ImagePlus;
import ij.io.Opener;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import magictool.cluster.AbstractCluster;
import magictool.cluster.ClusterFrame;
import magictool.cluster.ClusterExportFrame;
import magictool.clusterdisplay.ClusterDisplayFrame;
import magictool.dissim.DissimilarityFrame;
import magictool.explore.AverageDataDialog;
import magictool.explore.CritDialog;
import magictool.explore.ExploreFrame;
import magictool.explore.FileMerger;
import magictool.explore.ImportInfoDialog;
import magictool.explore.LimitFrame;
import magictool.explore.TransformDialog;
import magictool.filefilters.DirectoryChooser;
import magictool.image.GeneList;
import magictool.image.GridManager;
import magictool.image.GriddingFrame;
import magictool.image.SegmentFrame;
import magictool.task.TaskBuilderFrame;
import magictool.task.TaskManager;
import magictool.task.TaskManagerFrame;

/**
 * MainFrame creates the main frame for the whole application and is the base
 * desktop for all internal frames in the application.
 */
public class MainFrame extends JFrame {

    private JPanel contentPane;

    private BorderLayout borderLayout1 = new BorderLayout();

    private String magicToolVersionNumber = "1.4";

    private JMenuBar topMenuBar = new JMenuBar();

    private JMenuItem closeChoice = new JMenuItem();

    private JMenuItem newChoice = new JMenuItem();

    private JMenuItem propChoice = new JMenuItem();

    private JMenuItem exitChoice = new JMenuItem();

    private JMenu projMenu = new JMenu();

    private JMenuItem loadChoice = new JMenuItem();

    private JDesktopPane jdesktoppane;

    private JMenu createExpFileMenu = new JMenu();

    private JMenu loadImagePair = new JMenu();

    private JCheckBoxMenuItem loadRed = new JCheckBoxMenuItem();

    private JCheckBoxMenuItem loadGreen = new JCheckBoxMenuItem();

    private JMenu loadGeneListChoice = new JMenu();

    private JCheckBoxMenuItem loadList = new JCheckBoxMenuItem();

    private JMenuItem gridChoice = new JMenuItem();

    private JMenuItem segmentChoice = new JMenuItem();

    private JMenu clustMenu = new JMenu();

    private JMenuItem filtChoice = new JMenuItem();

    private JMenu manipChoice = new JMenu();

    private JMenu scramChoice = new JMenu();

    private JMenuItem scramOrder = new JMenuItem();

    private JMenuItem scramGene = new JMenuItem();

    private JMenuItem scramColumn = new JMenuItem();

    private JMenuItem scramBoth = new JMenuItem();

    private JMenuItem normChoice = new JMenuItem();

    private JMenu expMenu = new JMenu();

    private JMenuItem mergeChoice = new JMenuItem();

    private JMenuItem loadInfoChoice = new JMenuItem();

    private JMenuItem avgExpFileChoice = new JMenuItem();

    private JMenuItem transChoice = new JMenuItem();

    private JMenuItem compDisChoice = new JMenuItem();

    private JMenuItem exploreChoice = new JMenuItem();

    private JMenu disChoice = new JMenu();

    private JMenuItem editExpChoice = new JMenuItem();

    private JMenuItem editExpInfoChoice = new JMenuItem();

    private JMenu expSelector = new JMenu();

    private ButtonGroup expFileGroup = new ButtonGroup();

    private JMenuItem clusterChoice = new JMenuItem();

    private JMenuItem clusterExportChoice = new JMenuItem();

    private JMenuItem clustDisplayChoice = new JMenuItem();

    private JMenuItem scanChoice = new JMenuItem();

    private JMenuItem addOneChoice = new JMenuItem();

    private JMenuItem addManyChoice = new JMenuItem();

    private JMenuItem removeChoice = new JMenuItem();

    private JMenuItem limitDataChoice = new JMenuItem();

    private JMenu taskMenu = new JMenu();

    private JMenuItem taskmanChoice = new JMenuItem();

    private JMenuItem addtaskChoice = new JMenuItem();

    private JMenu helpMenu = new JMenu("Help");

    private JMenuItem helpChoice = new JMenuItem();

    private JMenuItem aboutChoice = new JMenuItem();

    private JMenuItem licenseChoice = new JMenuItem();

    /**fileLoader for file types used by application */
    public static GeneFileLoader fileLoader = new GeneFileLoader();

    /**project which is currently open*/
    protected Project openProject = null;

    /**vector containing all expression files available for user*/
    protected Vector expFiles = new Vector();

    /**manages tasks that user has scheduled*/
    protected TaskManager taskManager;

    /**frame which displays list of current tasks scheduled*/
    protected TaskManagerFrame tmFrame;

    /**filepath for red image*/
    protected String redPath = null;

    /**filepath for green image*/
    protected String greenPath = null;

    /**filepath for gene list*/
    protected String geneListPath = null;

    /**dimensions for red image*/
    protected Dimension redDim = new Dimension(0, 0);

    /**dimensions for green image*/
    protected Dimension greenDim = new Dimension(0, 0);

    /**manages the grids for the images*/
    protected GridManager manager;

    /**frame for gridding the images*/
    protected GriddingFrame griddingFrame;

    /**frame for the segmentation of the images*/
    protected SegmentFrame sf;

    /**list of genes for expression file*/
    protected GeneList geneList = null;

    /**current group file open*/
    public static File currentGrpFile;

    /**current expression file open*/
    public static ExpFile expMain;

    private Image desktopImage;

    /**Constructs the frame*/
    public MainFrame() {
        enableEvents(AWTEvent.WINDOW_EVENT_MASK);
        ImageIcon icon = new ImageIcon(this.getClass().getResource("gifs/icon.gif"));
        this.setIconImage(icon.getImage());
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        ImageIcon icon = new ImageIcon(getClass().getResource("gifs/logo.gif"));
        desktopImage = icon.getImage();
        int w = desktopImage.getWidth(this);
        int h = desktopImage.getHeight(this);
        int[] mypix = new int[w * h];
        PixelGrabber pixgrab = new PixelGrabber(desktopImage, 0, 0, w, h, mypix, 0, w);
        try {
            pixgrab.grabPixels();
            for (int i = 0; i < mypix.length; i++) {
                int a = (mypix[i] >> 24) & 0xFF;
                int r = (mypix[i] >> 16) & 0xFF;
                int gr = (mypix[i] >> 8) & 0xFF;
                int b = mypix[i] & 0xFF;
                a = 50;
                mypix[i] = (a << 24 | r << 16 | gr << 8 | b);
            }
        } catch (Exception e3) {
        }
        desktopImage = createImage(new MemoryImageSource(w, h, mypix, 0, w));
        jdesktoppane = new JDesktopPane() {

            public void paintComponent(Graphics g) {
                Rectangle b = getBounds();
                g.drawImage(desktopImage, b.x + b.width / 2 - desktopImage.getWidth(this) / 2, b.y + b.height / 2 - desktopImage.getHeight(this) / 2, this);
            }
        };
        this.jdesktoppane.setBackground(Color.white);
        this.setSize(Toolkit.getDefaultToolkit().getScreenSize().width / 2, Toolkit.getDefaultToolkit().getScreenSize().height / 2 + Toolkit.getDefaultToolkit().getScreenSize().height / 4);
        this.setTitle("MAGIC Tool, version " + magicToolVersionNumber);
        closeChoice.setText("Close Project");
        closeChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_P, java.awt.event.KeyEvent.CTRL_MASK, false));
        closeChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeChoice_actionPerformed();
            }
        });
        newChoice.setText("New Project...");
        newChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_N, java.awt.event.KeyEvent.CTRL_MASK, false));
        newChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newChoice_actionPerformed(e);
            }
        });
        propChoice.setEnabled(false);
        propChoice.setText("Project Properties...");
        propChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                propChoice_actionPerformed(e);
            }
        });
        exitChoice.setText("Exit");
        exitChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_Q, java.awt.event.KeyEvent.CTRL_MASK, false));
        exitChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exitChoice_actionPerformed(e);
            }
        });
        projMenu.setText("Project");
        loadChoice.setText("Load Project...");
        loadChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_L, java.awt.event.KeyEvent.CTRL_MASK, false));
        loadChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadChoice_actionPerformed(e);
            }
        });
        createExpFileMenu.setText("Build Expression File");
        createExpFileMenu.setEnabled(false);
        loadImagePair.setText("Load Image Pair...");
        loadRed.setText("Red: <none>");
        loadRed.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_R, java.awt.event.KeyEvent.CTRL_MASK, false));
        loadRed.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadRed_actionPerformed(e);
            }
        });
        loadGreen.setText("Green: <none>");
        loadGreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_G, java.awt.event.KeyEvent.CTRL_MASK, false));
        loadGreen.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadGreen_actionPerformed(e);
            }
        });
        loadGeneListChoice.setText("Load Gene List... ");
        loadList.setText("<none>");
        loadList.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_X, java.awt.event.KeyEvent.CTRL_MASK, false));
        loadGeneListChoice.setEnabled(false);
        loadList.setEnabled(false);
        loadList.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadList_actionPerformed(e);
            }
        });
        gridChoice.setText("Addressing/Gridding");
        gridChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, java.awt.event.KeyEvent.CTRL_MASK, false));
        gridChoice.setEnabled(false);
        gridChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gridChoice_actionPerformed(e);
            }
        });
        segmentChoice.setText("Segmentation");
        segmentChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.event.KeyEvent.CTRL_MASK, false));
        segmentChoice.setEnabled(false);
        segmentChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                segmentChoice_actionPerformed(e);
            }
        });
        clustMenu.setText("Cluster");
        filtChoice.setText("Filter...");
        filtChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(70, java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
        filtChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                filtChoice_actionPerformed(e);
            }
        });
        manipChoice.setEnabled(false);
        manipChoice.setText("Manipulate Data");
        scramChoice.setText("Scramble");
        scramOrder.setText("Scramble Gene Order");
        scramOrder.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scramOrder_actionPerformed(e);
            }
        });
        scramGene.setText("Scramble Data Within Genes");
        scramGene.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scramGene_actionPerformed(e);
            }
        });
        scramColumn.setText("Scramble Data Within Columns");
        scramColumn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scramColumn_actionPerformed(e);
            }
        });
        scramBoth.setText("Scramble Data Within Genes and Columns");
        scramBoth.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scramBoth_actionPerformed(e);
            }
        });
        normChoice.setText("Normalize");
        normChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(78, java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
        normChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                normChoice_actionPerformed(e);
            }
        });
        expMenu.setText("Expression");
        mergeChoice.setEnabled(false);
        mergeChoice.setText("Merge Expression Files...");
        mergeChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_M, java.awt.event.KeyEvent.CTRL_MASK, false));
        mergeChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                mergeChoice_actionPerformed(e);
            }
        });
        loadInfoChoice.setEnabled(false);
        loadInfoChoice.setText("Import Gene Info...");
        loadInfoChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_I, java.awt.event.KeyEvent.CTRL_MASK, false));
        loadInfoChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                loadInfoChoice_actionPerformed(e);
            }
        });
        avgExpFileChoice.setEnabled(false);
        avgExpFileChoice.setText("Average Replicates...");
        avgExpFileChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                avgExpFileChoice_actionPerformed(e);
            }
        });
        transChoice.setText("Transform...");
        transChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(84, java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
        transChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                transChoice_actionPerformed(e);
            }
        });
        compDisChoice.setText("Compute...");
        compDisChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_D, java.awt.event.KeyEvent.CTRL_MASK, false));
        compDisChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                compDisChoice_actionPerformed(e);
            }
        });
        expSelector.setText("Working Expression File");
        exploreChoice.setText("Explore...");
        exploreChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_E, java.awt.event.KeyEvent.CTRL_MASK, false));
        exploreChoice.setEnabled(false);
        exploreChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exploreChoice_actionPerformed(e);
            }
        });
        disChoice.setEnabled(false);
        disChoice.setText("Dissimilarities");
        editExpChoice.setEnabled(false);
        editExpChoice.setText("View / Edit Data");
        editExpChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_V, java.awt.event.KeyEvent.CTRL_MASK, false));
        editExpChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editExpChoice_actionPerformed(e);
            }
        });
        editExpInfoChoice.setEnabled(false);
        editExpInfoChoice.setText("View / Edit Gene Info");
        editExpInfoChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_I, java.awt.event.KeyEvent.CTRL_MASK, false));
        editExpInfoChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editExpInfoChoice_actionPerformed(e);
            }
        });
        clusterChoice.setText("Compute...");
        clusterChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_C, java.awt.event.KeyEvent.CTRL_MASK, false));
        clusterChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clusterChoice_actionPerformed(e);
            }
        });
        clusterExportChoice.setText("Export...");
        clusterExportChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clusterExportChoice_actionPerformed(e);
            }
        });
        clustDisplayChoice.setText("Display...");
        clustDisplayChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                clustDisplayChoice_actionPerformed(e);
            }
        });
        addOneChoice.setEnabled(false);
        addOneChoice.setText("Add File...");
        addOneChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addOneChoice_actionPerformed(e);
            }
        });
        scanChoice.setEnabled(false);
        scanChoice.setText("Update Project...");
        scanChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scanChoice_actionPerformed();
            }
        });
        addManyChoice.setEnabled(false);
        addManyChoice.setText("Add Directory...");
        addManyChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addManyChoice_actionPerformed(e);
            }
        });
        removeChoice.setEnabled(false);
        removeChoice.setText("Remove File...");
        removeChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeChoice_actionPerformed(e);
            }
        });
        limitDataChoice.setText("Limit Data...");
        limitDataChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(76, java.awt.event.KeyEvent.CTRL_MASK | java.awt.event.KeyEvent.SHIFT_MASK, false));
        limitDataChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                limitDataChoice_actionPerformed(e);
            }
        });
        taskMenu.setEnabled(false);
        taskMenu.setText("Task");
        taskmanChoice.setText("Task Manager");
        taskmanChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, false));
        taskmanChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                taskmanChoice_actionPerformed(e);
            }
        });
        addtaskChoice.setText("Add Task");
        addtaskChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK, false));
        addtaskChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addtaskChoice_actionPerformed(e);
            }
        });
        helpChoice.setText("Help");
        helpChoice.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_H, java.awt.event.KeyEvent.CTRL_MASK, false));
        helpChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                helpChoice_actionPerformed(e);
            }
        });
        aboutChoice.setText("About MAGIC Tool...");
        aboutChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                aboutChoice_actionPerformed(e);
            }
        });
        licenseChoice.setText("License Info");
        licenseChoice.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                licenseChoice_actionPerformed(e);
            }
        });
        topMenuBar.add(projMenu);
        topMenuBar.add(createExpFileMenu);
        topMenuBar.add(expMenu);
        topMenuBar.add(clustMenu);
        topMenuBar.add(taskMenu);
        topMenuBar.add(helpMenu);
        projMenu.add(newChoice);
        projMenu.add(loadChoice);
        projMenu.add(closeChoice);
        projMenu.addSeparator();
        projMenu.add(addOneChoice);
        projMenu.add(addManyChoice);
        projMenu.add(removeChoice);
        projMenu.add(scanChoice);
        projMenu.add(propChoice);
        projMenu.addSeparator();
        projMenu.add(exitChoice);
        createExpFileMenu.add(loadImagePair);
        loadImagePair.add(loadRed);
        loadImagePair.add(loadGreen);
        createExpFileMenu.add(loadGeneListChoice);
        loadGeneListChoice.add(loadList);
        createExpFileMenu.add(gridChoice);
        createExpFileMenu.add(segmentChoice);
        manipChoice.add(transChoice);
        manipChoice.add(normChoice);
        manipChoice.add(limitDataChoice);
        manipChoice.add(filtChoice);
        manipChoice.add(scramChoice);
        scramChoice.add(scramGene);
        scramChoice.add(scramColumn);
        scramChoice.add(scramBoth);
        expMenu.add(mergeChoice);
        expMenu.add(loadInfoChoice);
        expMenu.add(avgExpFileChoice);
        expMenu.add(editExpChoice);
        expMenu.add(editExpInfoChoice);
        expMenu.add(manipChoice);
        expMenu.add(disChoice);
        expMenu.add(exploreChoice);
        disChoice.add(compDisChoice);
        clustMenu.add(clusterChoice);
        clustMenu.add(clusterExportChoice);
        clustMenu.add(clustDisplayChoice);
        taskMenu.add(taskmanChoice);
        taskMenu.add(addtaskChoice);
        helpMenu.add(helpChoice);
        helpMenu.addSeparator();
        helpMenu.add(licenseChoice);
        helpMenu.add(aboutChoice);
        clustMenu.setEnabled(false);
        expMenu.setEnabled(false);
        this.setJMenuBar(topMenuBar);
        this.getContentPane().add(jdesktoppane, null);
        manager = new GridManager();
    }

    /**
     * Overridden so we can exit when window is closed
     * @param e window event
     */
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            System.exit(0);
        }
    }

    private void exploreChoice_actionPerformed(ActionEvent e) {
        ExploreFrame exploreframe = new ExploreFrame(expMain, openProject, this);
        exploreframe.setLocation(80, 80);
        exploreframe.setClosable(true);
        exploreframe.setVisible(true);
        exploreframe.pack();
        jdesktoppane.add(exploreframe);
        exploreframe.show();
        exploreframe.toFront();
    }

    /**
     * adds an expression file to the available list of expression files if the
     * file is valid and sets the open expression file to this new file
     * @param filename full file name of the expression file to add
     */
    public void addExpFile(String filename) {
        if (expFiles.contains(filename)) {
            int i = 0;
            while (!((String) expFiles.elementAt(i)).equals(filename)) {
                i++;
            }
            expSelector.getItem(i).setSelected(true);
            for (int j = 0; j < expSelector.getItemCount(); j++) {
                if (j != i) expSelector.getItem(j).setSelected(false);
            }
            expMain = new ExpFile(new File(filename));
        } else {
            expMain = new ExpFile(new File(filename));
            if (expMain.isValid()) {
                JCheckBoxMenuItem newExpFile = new JCheckBoxMenuItem(filename.substring(filename.lastIndexOf(File.separator) + 1));
                expFiles.insertElementAt(filename, 0);
                newExpFile.setName(filename);
                newExpFile.setSelected(true);
                newExpFile.addItemListener(new java.awt.event.ItemListener() {

                    public void itemStateChanged(ItemEvent e) {
                        switchExpFile(e);
                    }
                });
                expSelector.add(newExpFile, 0);
                for (int j = 1; j < expSelector.getItemCount(); j++) {
                    expSelector.getItem(j).setSelected(false);
                }
                expMenu.add(expSelector, 0);
                if (expSelector.getItemCount() >= 1) loadInfoChoice.setEnabled(true); else loadInfoChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) avgExpFileChoice.setEnabled(true); else avgExpFileChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 2) mergeChoice.setEnabled(true); else mergeChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) editExpChoice.setEnabled(true); else editExpChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) editExpInfoChoice.setEnabled(true); else editExpInfoChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) exploreChoice.setEnabled(true); else exploreChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) manipChoice.setEnabled(true); else manipChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) disChoice.setEnabled(true); else disChoice.setEnabled(false);
                if (expSelector.getItemCount() >= 1) editExpChoice.setToolTipText(filename);
            } else {
                JOptionPane.showMessageDialog(this, "Error! Could Not Open " + expMain.getExpFile().getName() + "\nPlease Make Sure That The File Follows The Correct Tab-Delimited Format Specified In The Manual");
                openProject.removeFile(expMain.name + File.separator + expMain.name + ".exp");
                if (expSelector.getItemCount() > 0) expSelector.getItem(0).setSelected(true);
            }
        }
    }

    /**
     * removes an expression file from the list of available expression files
     * @param filename full name of the file to be removed from the list of expression files
     */
    public void removeExpFile(String filename) {
        int i = 0;
        while (!((String) expFiles.elementAt(i)).equals(filename)) {
            i++;
        }
        boolean select = expSelector.getItem(i).isSelected();
        if (select) {
            if (expSelector.getItemCount() > 1) {
                expSelector.getItem((i == 0 ? 1 : 0)).setSelected(true);
            } else {
                editExpChoice.setEnabled(false);
                editExpInfoChoice.setEnabled(false);
                exploreChoice.setEnabled(false);
                manipChoice.setEnabled(false);
                disChoice.setEnabled(false);
                mergeChoice.setEnabled(false);
                loadInfoChoice.setEnabled(false);
                avgExpFileChoice.setEnabled(false);
            }
        }
        expSelector.remove(i);
        File fi = new File(expFiles.elementAt(i).toString());
        openProject.removeFile(fi.getParentFile().getName() + File.separator + fi.getName());
        expFiles.removeElementAt(i);
        if (expSelector.getItemCount() < 1) loadInfoChoice.setEnabled(false);
        if (expSelector.getItemCount() < 1) avgExpFileChoice.setEnabled(false);
        if (expSelector.getItemCount() < 2) mergeChoice.setEnabled(false);
        if (expSelector.getItemCount() < 1) editExpChoice.setEnabled(false);
        if (expSelector.getItemCount() < 1) editExpInfoChoice.setEnabled(false);
        if (expSelector.getItemCount() < 1) exploreChoice.setEnabled(false);
        if (expSelector.getItemCount() < 1) manipChoice.setEnabled(false);
        if (expSelector.getItemCount() < 1) disChoice.setEnabled(false);
    }

    private void setUpTaskManager() {
        taskManager = new TaskManager(openProject);
        tmFrame = new TaskManagerFrame(taskManager, jdesktoppane, this);
        tmFrame.pack();
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        tmFrame.setLocation(screen.width / 2 - tmFrame.getWidth() / 2, screen.height / 2 - tmFrame.getHeight() / 2);
        jdesktoppane.add(tmFrame);
    }

    /**
     * switchs the open expression file
     * @param e item event from user selecting new file
     */
    public void switchExpFile(ItemEvent e) {
        if (e.getStateChange() == e.SELECTED) {
            String filename = ((JCheckBoxMenuItem) e.getItem()).getName();
            try {
                expMain = new ExpFile(new File(filename));
                int i = 0;
                while (!((String) expFiles.elementAt(i)).equals(filename)) {
                    i++;
                }
                for (int j = 0; j < expSelector.getItemCount(); j++) {
                    if (j != i) expSelector.getItem(j).setSelected(false);
                }
            } catch (Exception e2) {
                expMain = null;
            }
            if (expMain == null || !expMain.isValid()) {
                JOptionPane.showMessageDialog(this, "Error Opening Expression File! File Is Either Corrupted Or No Longer Exists!");
                removeExpFile(filename);
            } else {
                editExpChoice.setToolTipText(filename);
            }
        }
    }

    private void exitChoice_actionPerformed(ActionEvent e) {
        System.exit(0);
    }

    private void editExpChoice_actionPerformed(ActionEvent e) {
        TableEditFrame tableEditExp = new TableEditFrame(expMain, openProject);
        tableEditExp.setParent(this);
        tableEditExp.setColumnsToFit();
        jdesktoppane.add(tableEditExp);
        tableEditExp.show();
        tableEditExp.toFront();
    }

    private void editExpInfoChoice_actionPerformed(ActionEvent e) {
        TableEditFrame tableEditExp = new TableEditFrame(expMain, openProject, false, true);
        tableEditExp.setParent(this);
        tableEditExp.setColumnsToFit();
        jdesktoppane.add(tableEditExp);
        tableEditExp.show();
        tableEditExp.toFront();
    }

    private void normChoice_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                ExpFile norm = new ExpFile(expMain.getExpFile());
                norm.normalize();
                writeExpFile(norm, expMain.name + "_norm.exp", expMain.getName(), true);
                setCursor(Cursor.getDefaultCursor());
            }
        };
        thread.start();
    }

    private void transChoice_actionPerformed(ActionEvent e) {
        ExpFile t = new ExpFile(expMain.getExpFile());
        TransformDialog transform = new TransformDialog(t, this);
        transform.setModal(true);
        if (transform.getOK()) writeExpFile(transform.getExpFile(), expMain.name + "_t" + transform.getTransform() + ".exp", expMain.getName(), true);
    }

    private void compDisChoice_actionPerformed(ActionEvent e) {
        DissimilarityFrame dissimframe1 = new DissimilarityFrame(expMain, openProject, this);
        dissimframe1.setVisible(true);
        dissimframe1.setLocation(180, 100);
        dissimframe1.pack();
        jdesktoppane.add(dissimframe1);
        dissimframe1.show();
        dissimframe1.toFront();
    }

    private void clusterChoice_actionPerformed(ActionEvent e) {
        ClusterFrame clusterframe = new ClusterFrame(openProject, this);
        clusterframe.setVisible(true);
        clusterframe.setLocation(200, 60);
        clusterframe.pack();
        jdesktoppane.add(clusterframe);
        clusterframe.show();
        clusterframe.toFront();
    }

    private void clusterExportChoice_actionPerformed(ActionEvent e) {
        ClusterExportFrame exporter = new ClusterExportFrame(openProject, jdesktoppane);
        exporter.setVisible(true);
        exporter.setLocation(200, 60);
        exporter.pack();
        jdesktoppane.add(exporter);
        exporter.show();
        exporter.toFront();
    }

    private void clustDisplayChoice_actionPerformed(ActionEvent e) {
        ClusterDisplayFrame clustdisplay = new ClusterDisplayFrame(openProject, this);
        clustdisplay.setVisible(true);
        clustdisplay.pack();
        clustdisplay.setSize(500, clustdisplay.getPreferredSize().height);
        jdesktoppane.add(clustdisplay);
        clustdisplay.setLocation(220, 140);
        clustdisplay.show();
        clustdisplay.toFront();
    }

    private void loadChoice_actionPerformed(ActionEvent e) {
        int c = JOptionPane.CANCEL_OPTION;
        if (openProject != null) c = JOptionPane.showConfirmDialog(this, "Warning! There Is A Project Currently Open. Do You Wish To Close That Project And Open A New One?");
        if (openProject == null || c == JOptionPane.YES_OPTION) {
            MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.gprjFilter);
            MainFrame.fileLoader.setDialogTitle("Load Project File...");
            MainFrame.fileLoader.setApproveButtonText("Load");
            MainFrame.fileLoader.setSelectedFile(null);
            int result = MainFrame.fileLoader.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                final File fileobj = MainFrame.fileLoader.getSelectedFile();
                if (c == JOptionPane.YES_OPTION) {
                    closeChoice_actionPerformed();
                }
                Thread thread = new Thread() {

                    public void run() {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        String path = fileobj.getAbsolutePath();
                        try {
                            openProject = Project.openProject(path);
                            setTitle("MAGIC Tool v1.2 - " + fileobj.getPath());
                            scanChoice_actionPerformed();
                            String[] files = openProject.getExpressionFiles();
                            for (int i = 0; i < files.length; i++) {
                                addExpFile(openProject.getPath() + files[i]);
                            }
                        } catch (Exception e3) {
                            openProject = null;
                            JOptionPane.showMessageDialog(null, "Error! Could Not Open Project File");
                        }
                        if (openProject != null) {
                            setUpTaskManager();
                            clustMenu.setEnabled(true);
                            expMenu.setEnabled(true);
                            addManyChoice.setEnabled(true);
                            addOneChoice.setEnabled(true);
                            propChoice.setEnabled(true);
                            removeChoice.setEnabled(true);
                            scanChoice.setEnabled(true);
                            taskMenu.setEnabled(true);
                            createExpFileMenu.setEnabled(true);
                        } else {
                            clustMenu.setEnabled(false);
                            expMenu.setEnabled(false);
                            addManyChoice.setEnabled(false);
                            addOneChoice.setEnabled(false);
                            removeChoice.setEnabled(false);
                            scanChoice.setEnabled(false);
                            taskMenu.setEnabled(false);
                            propChoice.setEnabled(false);
                            createExpFileMenu.setEnabled(false);
                        }
                        setCursor(Cursor.getDefaultCursor());
                    }
                };
                thread.start();
            }
        }
    }

    private void newChoice_actionPerformed(ActionEvent e) {
        int c = JOptionPane.CANCEL_OPTION;
        if (openProject != null) c = JOptionPane.showConfirmDialog(this, "Warning! There Is A Project Currently Open. Do You Wish To Close That Project And Open A New One?");
        if (openProject == null || c == JOptionPane.YES_OPTION) {
            closeChoice_actionPerformed();
            MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.gprjFilter);
            MainFrame.fileLoader.setDialogTitle("Create New Project File...");
            MainFrame.fileLoader.setApproveButtonText("Select");
            int result = MainFrame.fileLoader.showSaveDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File fileobj = MainFrame.fileLoader.getSelectedFile();
                String name = fileobj.getName();
                if (!name.toLowerCase().endsWith(".gprj")) name += ".gprj";
                String path = fileobj.getAbsolutePath();
                openProject = new Project(path);
                this.setTitle("MAGIC Tool - " + fileobj.getPath());
                openProject.writeProject();
            }
        }
        if (openProject != null) {
            setUpTaskManager();
            clustMenu.setEnabled(true);
            expMenu.setEnabled(true);
            addManyChoice.setEnabled(true);
            addOneChoice.setEnabled(true);
            removeChoice.setEnabled(true);
            scanChoice.setEnabled(true);
            taskMenu.setEnabled(true);
            propChoice.setEnabled(true);
            createExpFileMenu.setEnabled(true);
        } else {
            clustMenu.setEnabled(false);
            expMenu.setEnabled(false);
            addManyChoice.setEnabled(false);
            addOneChoice.setEnabled(false);
            removeChoice.setEnabled(false);
            scanChoice.setEnabled(false);
            taskMenu.setEnabled(false);
            propChoice.setEnabled(false);
            createExpFileMenu.setEnabled(false);
        }
    }

    private void closeChoice_actionPerformed() {
        if (openProject != null) openProject.writeProject();
        openProject = null;
        this.jdesktoppane.removeAll();
        this.jdesktoppane.repaint();
        this.setTitle("MAGIC Tool");
        expMain = null;
        expSelector.removeAll();
        expFiles.removeAllElements();
        if (openProject != null) {
            clustMenu.setEnabled(true);
            expMenu.setEnabled(true);
            addManyChoice.setEnabled(true);
            addOneChoice.setEnabled(true);
            removeChoice.setEnabled(true);
            scanChoice.setEnabled(true);
            taskMenu.setEnabled(true);
            propChoice.setEnabled(true);
            createExpFileMenu.setEnabled(true);
        } else {
            clustMenu.setEnabled(false);
            expMenu.setEnabled(false);
            addManyChoice.setEnabled(false);
            addOneChoice.setEnabled(false);
            removeChoice.setEnabled(false);
            scanChoice.setEnabled(false);
            taskMenu.setEnabled(false);
            propChoice.setEnabled(false);
            createExpFileMenu.setEnabled(false);
        }
        taskManager = null;
        tmFrame = null;
        this.jdesktoppane.removeAll();
    }

    /**
     * adds a file to the open project moving it to the proper location while leaving a
     * copy of the file in the old location as well
     * @param fileobj file to add to the open project
     */
    public void addFile(File fileobj) {
        addFile(fileobj, false);
    }

    /**
     * adds a file to the open project moving it to the proper location while deleting
     * the old copy of the file if the user desires
     * @param fileobj file to add to the open project
     * @param delete whether or not to delete the old copy of the file
     */
    public void addFile(File fileobj, boolean delete) {
        String oldFileName = fileobj.getPath();
        String currFileName = setUpFile(fileobj);
        if (currFileName != null) {
            File f = new File(currFileName);
            int deleteFiles = JOptionPane.CANCEL_OPTION;
            if (oldFileName.equals(currFileName)) {
                currFileName = currFileName.substring(openProject.getPath().length());
                openProject.addFile(currFileName);
                if (f.getName().toLowerCase().endsWith(".exp")) addExpFile(f.getPath());
            } else if (!f.exists() || JOptionPane.OK_OPTION == (deleteFiles = JOptionPane.showConfirmDialog(this, "File" + f.getName() + " Already Exists! Do You Wish To Overwrite That File?" + (f.getName().toLowerCase().endsWith(".exp") ? "\nOverwriting An Expression File Will Delete All Files Which Previously Required The Orginal File" : "")))) {
                try {
                    if (deleteFiles == JOptionPane.OK_OPTION && f.getName().toLowerCase().endsWith(".exp")) {
                        File expF[] = f.getParentFile().listFiles();
                        for (int i = 0; i < expF.length; i++) {
                            while (expF[i].exists()) {
                                expF[i].delete();
                            }
                        }
                        f.getParentFile().delete();
                    }
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    FileInputStream in = new FileInputStream(fileobj);
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] buffer = new byte[8 * 1024];
                    int count = 0;
                    do {
                        out.write(buffer, 0, count);
                        count = in.read(buffer, 0, buffer.length);
                    } while (count != -1);
                    in.close();
                    out.close();
                    if (delete) fileobj.delete();
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(this, "Error! Could Not Add " + fileobj.getName() + " To Project");
                }
                currFileName = currFileName.substring(currFileName.lastIndexOf(openProject.getName()) + openProject.getName().length() + 1);
                openProject.addFile(currFileName);
                if (f.getName().toLowerCase().endsWith(".exp")) addExpFile(f.getPath());
            }
        } else {
            String message = "Error! Could Not Add " + fileobj.getName() + " To Project\n";
            if (fileobj.getName().endsWith(".gprj")) {
                message += "You May Not Add A Project File To An Existing Project";
            } else if (fileobj.getName().toLowerCase().endsWith(".ds_store")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".txt")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".gif")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".jpeg")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".jpg")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".info")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".html")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".db")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".raw")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".cdt")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".gtr")) {
                message = "";
            } else if (fileobj.getName().toLowerCase().endsWith(".jtv")) {
                message = "";
            } else message += "File Extension Unknown. Please Check The File To Ensure It Has The Correct Extension";
            if (!message.equals("")) JOptionPane.showMessageDialog(this, message);
        }
    }

    private void addOneChoice_actionPerformed(ActionEvent e) {
        MainFrame.fileLoader.setApproveButtonText("Select");
        MainFrame.fileLoader.setApproveButtonToolTipText(null);
        MainFrame.fileLoader.setAcceptAllFileFilterUsed(true);
        MainFrame.fileLoader.setSelectedFile(null);
        MainFrame.fileLoader.setMultiSelectionEnabled(true);
        MainFrame.fileLoader.setDialogTitle("Select A File...");
        int result = MainFrame.fileLoader.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = MainFrame.fileLoader.getSelectedFiles();
            for (int i = 0; i < files.length; i++) addFile(files[i]);
        }
        MainFrame.fileLoader.setMultiSelectionEnabled(false);
    }

    /**
     * reads a file to determine its proper file path within the project
     * @param f file to add to the project
     * @return string representation full path of the new location of the file within the project
     */
    public String setUpFile(File f) {
        try {
            String name = f.getName();
            String exp = "";
            if (name.toLowerCase().endsWith(".exp")) {
                exp = name.substring(0, name.lastIndexOf("."));
            } else if (name.toLowerCase().endsWith(".dis")) {
                RandomAccessFile raf = new RandomAccessFile(f, "r");
                raf.readInt();
                if (raf.readBoolean() == true) exp = raf.readUTF();
                raf.close();
            } else if (name.toLowerCase().endsWith(".clust")) {
                String info[] = AbstractCluster.readHeaders(f.getPath());
                exp = info[1];
            } else if (name.toLowerCase().endsWith(".grp")) {
                RandomAccessFile raf = new RandomAccessFile(f, "r");
                exp = raf.readLine();
                raf.close();
            } else if (name.toLowerCase().endsWith(".tif") || name.toLowerCase().endsWith(".tiff") || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg") || name.toLowerCase().endsWith(".html")) {
                if (f.getParentFile().getName().endsWith("_images")) return openProject.getPath() + "images" + File.separator + f.getParentFile().getName() + File.separator + name;
                return openProject.getPath() + "images" + File.separator + name;
            } else if (name.toLowerCase().endsWith(".grid")) {
                return openProject.getPath() + "grids" + File.separator + name;
            } else if (name.toLowerCase().endsWith(".info")) {
                return openProject.getPath() + "info" + File.separator + name;
            } else if (name.toLowerCase().endsWith(".txt")) {
                return openProject.getPath() + "lists" + File.separator + name;
            } else return null;
            int temp = 0;
            exp = exp.substring(exp.lastIndexOf(File.separator) + 1, ((temp = exp.lastIndexOf(".")) != -1 ? temp : exp.length()));
            return openProject.getPath() + exp + File.separator + name;
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(this, "Error Adding File " + f.getName());
            return null;
        }
    }

    private void removeChoice_actionPerformed(ActionEvent e) {
        RemoveDialog removeDialog = new RemoveDialog(this, openProject);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = removeDialog.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        removeDialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        removeDialog.show();
    }

    private void propChoice_actionPerformed(ActionEvent e) {
        ProjectPropertiesFrame ppf = new ProjectPropertiesFrame(this, openProject);
        ppf.pack();
        ppf.validate();
        jdesktoppane.add(ppf);
        Dimension screen = jdesktoppane.getSize();
        ppf.setSize((ppf.getWidth() > 450 ? ppf.getWidth() : 450), (ppf.getHeight() > 250 ? ppf.getHeight() : 250));
        ppf.setLocation((screen.width - ppf.getWidth()) / 2, (screen.height - ppf.getHeight()) / 2);
        ppf.show();
        ppf.setSize(ppf.getWidth() + 1, ppf.getHeight());
    }

    private void addManyChoice_actionPerformed(ActionEvent e) {
        DirectoryChooser dc = new DirectoryChooser(MainFrame.fileLoader.getCurrentDirectory());
        dc.setDialogTitle("Please Select A Folder");
        int result = dc.showOpenDialog(null);
        if (result == dc.APPROVE_OPTION) {
            File directory = dc.getSelectedFile();
            addFiles(directory);
        }
    }

    /**
   * adds a all the files in a folder to the open project moving them to the proper location while leaving
   * copies of the file in the old locations
   * @param directory folder of files to add to the open project
   */
    public void addFiles(File directory) {
        addFiles(directory, false);
    }

    /**
   * adds a all the files in a folder to the open project moving them to the proper location while leaving
   * copies of the file in the old locations
   * @param directory folder of files to add to the open project
   * @param addDirectories whether or not to add files from directories within specified directory
   */
    public void addFiles(File directory, boolean addDirectories) {
        if (directory.isDirectory()) {
            boolean addThem = true, addFolders = addDirectories;
            final File files[] = directory.listFiles();
            if (!addFolders) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        addFolders = (JOptionPane.showConfirmDialog(this, "This Folder Contains Other Folders. Do You Wish To Add All Files From These Folders?", "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
                        break;
                    }
                }
            }
            for (int i = 0; i < files.length; i++) {
                String fname = files[i].getName();
                if (fname.toLowerCase().endsWith(".exp")) {
                    File expTry = new File(openProject.getPath() + fname.substring(0, fname.lastIndexOf(File.separator) + 1) + fname);
                    if (expTry.exists()) {
                        JOptionPane.showMessageDialog(this, "One Or More Expression Files Already Exist In The Project.\n" + "You May Not Overwrite These Files Since These Files Are Required By Other Files\n" + "Review The Users Manual For More Information\n" + "You May Still Add Individual Files From This Folder Through Project->Add File");
                        addThem = false;
                        break;
                    }
                }
            }
            if (addThem) {
                Thread r = new Thread() {

                    public void run() {
                        ProgressFrame progressbar = new ProgressFrame("Adding Files...");
                        jdesktoppane.add(progressbar);
                        progressbar.setMaximum(files.length);
                        progressbar.setVisible(true);
                        for (int i = 0; i < files.length; i++) {
                            if (files[i].isFile()) {
                                progressbar.setTitle("Adding " + files[i].getName());
                                addFile(files[i]);
                                progressbar.addValue(1);
                            } else {
                                addFiles(files[i], true);
                            }
                        }
                        progressbar.dispose();
                    }
                };
                r.start();
            }
        }
    }

    private void scanChoice_actionPerformed() {
        scanChoice_actionPerformed(new File(openProject.getPath()));
    }

    private void scanChoice_actionPerformed(File directory) {
        File files[] = directory.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile() && !files[i].getName().toLowerCase().endsWith(".gprj")) {
                addFile(files[i], true);
            } else if (files[i].isDirectory()) {
                scanChoice_actionPerformed(files[i]);
                if (files[i].getName().endsWith("_images")) {
                    files[i].delete();
                }
            }
        }
        openProject.scanDirectory();
    }

    private void filtChoice_actionPerformed(ActionEvent e) {
        CritDialog critdialog = new CritDialog(expMain, this);
        critdialog.setModal(true);
        critdialog.show();
        GrpFile temp = critdialog.getValue();
        if (temp != null) {
            writeFilteredExpFile(temp);
        }
    }

    private void writeFilteredExpFile(GrpFile temp) {
        String old = expMain.getName();
        if (old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
        if (old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
        String name = (String) JOptionPane.showInputDialog(this, "You have selected " + temp.getNumGenes() + " genes. \nPlease enter new file name", "Save As...", JOptionPane.QUESTION_MESSAGE, null, null, expMain.name + "_" + "filtered.exp");
        if (name != null) {
            if (name.toLowerCase().endsWith(".exp")) name = name.substring(0, name.lastIndexOf("."));
            String file = openProject.getPath() + name + File.separator + name + ".exp";
            File f = new File(file);
            if (!f.exists() || JOptionPane.showConfirmDialog(this, "File Already Exists! Do You Wish To Overwrite?") == JOptionPane.OK_OPTION) {
                try {
                    f.getParentFile().mkdirs();
                    if (!file.toLowerCase().endsWith(".exp")) file += ".exp";
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    for (int i = 0; i < expMain.getColumns(); i++) {
                        bw.write(expMain.getLabel(i) + (i == expMain.getColumns() - 1 ? "" : "\t"));
                    }
                    bw.write("\n");
                    Object o[] = temp.getGroup();
                    for (int i = 0; i < o.length; i++) {
                        int pos = expMain.findGeneName((String) o[i]);
                        if (pos != -1) {
                            bw.write((String) o[i] + "\t");
                            double data[] = expMain.getData(pos);
                            for (int j = 0; j < data.length; j++) {
                                bw.write("" + data[j] + (j == data.length - 1 ? "" : "\t"));
                            }
                            String comments;
                            if ((comments = expMain.getGene(pos).getComments()) != null) bw.write("\t" + comments);
                            bw.write("\n");
                        }
                    }
                    bw.write("/**Gene Info**/" + "\n");
                    for (int i = 0; i < o.length; i++) {
                        int pos = expMain.findGeneName((String) o[i]);
                        if (pos != -1) {
                            Gene g = expMain.getGene(pos);
                            String n = g.getName();
                            String a = g.getAlias();
                            String c = g.getChromo();
                            String l = g.getLocation();
                            String p = g.getProcess();
                            String fl = g.getFunction();
                            String co = g.getComponent();
                            if (n != null) bw.write(n + "\t" + (a != null ? a : " ") + "\t" + (c != null ? c : " ") + "\t" + (l != null ? l : " ") + "\t" + (p != null ? p : " ") + "\t" + (fl != null ? fl : " ") + "\t" + (co != null ? co : " ") + "\n");
                        }
                    }
                    bw.close();
                    this.addExpFile(f.getPath());
                    openProject.addFile(name + File.separator + name + ".exp");
                    if (openProject.getGroupMethod() <= 1) {
                        String groupFiles[] = openProject.getGroupFiles(old);
                        for (int i = 0; i < groupFiles.length; i++) {
                            GrpFile gf = new GrpFile(new File(openProject.getPath() + groupFiles[i]));
                            gf.setExpFile(name);
                            Object[] genes = gf.getGroup();
                            for (int j = 0; j < genes.length; j++) {
                                if (expMain.findGeneName((String) genes[j]) == -1) gf.remove(genes[j]);
                            }
                            try {
                                gf.writeGrpFile(openProject.getPath() + name + File.separator + gf.getTitle());
                                openProject.addFile(name + File.separator + gf.getTitle());
                            } catch (DidNotFinishException e3) {
                            }
                        }
                    }
                    expMain = new ExpFile(f);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error Writing Exp File");
                }
            } else writeFilteredExpFile(temp);
        }
    }

    private void mergeChoice_actionPerformed(ActionEvent e) {
        try {
            FileMerger mergedialog = new FileMerger(openProject, this);
            mergedialog.setModal(true);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            mergedialog.pack();
            mergedialog.setResizable(false);
            Dimension frameSize = mergedialog.getSize();
            if (frameSize.height > screenSize.height) {
                frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                frameSize.width = screenSize.width;
            }
            mergedialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
            mergedialog.show();
            String file = mergedialog.getValue();
            if (file != null) {
                String shortfile = file;
                if (shortfile.toLowerCase().endsWith(".exp")) shortfile = shortfile.substring(0, shortfile.toLowerCase().lastIndexOf(".exp"));
                if (shortfile.lastIndexOf(File.separator) != -1) shortfile = shortfile.substring(shortfile.lastIndexOf(File.separator) + 1);
                String old = mergedialog.getMainExpFilePath();
                if (old != null && old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
                if (old != null && old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
                openProject.addFile(file);
                this.addExpFile(openProject.getPath() + file);
                if (old != null && openProject.getGroupMethod() == 0) {
                    String groupFiles[] = openProject.getGroupFiles(old);
                    for (int i = 0; i < groupFiles.length; i++) {
                        GrpFile gf = new GrpFile(new File(openProject.getPath() + groupFiles[i]));
                        gf.setExpFile(shortfile);
                        Object[] genes = gf.getGroup();
                        for (int j = 0; j < genes.length; j++) {
                            if (expMain.findGeneName((String) genes[j]) == -1) gf.remove(genes[j]);
                        }
                        try {
                            gf.writeGrpFile(openProject.getPath() + shortfile + File.separator + gf.getTitle());
                            openProject.addFile(shortfile + File.separator + gf.getTitle());
                        } catch (DidNotFinishException e3) {
                            e3.printStackTrace();
                        }
                    }
                }
                expMain = new ExpFile(new File(openProject.getPath() + file));
            }
        } catch (Exception e2) {
            JOptionPane.showMessageDialog(this, "Error Writing Exp File");
        }
    }

    private void loadInfoChoice_actionPerformed(ActionEvent e) {
        try {
            ImportInfoDialog infodialog = new ImportInfoDialog(openProject, this, jdesktoppane);
            infodialog.setModal(true);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            infodialog.pack();
            infodialog.setResizable(false);
            Dimension frameSize = infodialog.getSize();
            if (frameSize.height > screenSize.height) {
                frameSize.height = screenSize.height;
            }
            if (frameSize.width > screenSize.width) {
                frameSize.width = screenSize.width;
            }
            infodialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
            infodialog.show();
            String file = infodialog.getValue();
            if (file != null) {
                String shortfile = file;
                if (shortfile.toLowerCase().endsWith(".exp")) shortfile = shortfile.substring(0, shortfile.toLowerCase().lastIndexOf(".exp"));
                if (shortfile.lastIndexOf(File.separator) != -1) shortfile = shortfile.substring(shortfile.lastIndexOf(File.separator) + 1);
                String old = infodialog.getExpFilePath();
                if (old != null && old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
                if (old != null && old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
                openProject.addFile(file);
                this.addExpFile(openProject.getPath() + file);
                if (old != null && openProject.getGroupMethod() <= 1) {
                    String groupFiles[] = openProject.getGroupFiles(old);
                    for (int i = 0; i < groupFiles.length; i++) {
                        GrpFile gf = new GrpFile(new File(openProject.getPath() + groupFiles[i]));
                        gf.setExpFile(shortfile);
                        try {
                            gf.writeGrpFile(openProject.getPath() + shortfile + File.separator + gf.getTitle());
                            openProject.addFile(shortfile + File.separator + gf.getTitle());
                        } catch (DidNotFinishException e3) {
                            e3.printStackTrace();
                        }
                    }
                }
                expMain = new ExpFile(new File(openProject.getPath() + file));
            }
        } catch (Exception e2) {
            JOptionPane.showMessageDialog(this, "Error Writing Exp File");
        }
    }

    private void avgExpFileChoice_actionPerformed(ActionEvent e) {
        AverageDataDialog avgdialog = new AverageDataDialog(openProject, this, jdesktoppane);
        avgdialog.setModal(true);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        avgdialog.pack();
        avgdialog.setResizable(false);
        Dimension frameSize = avgdialog.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        avgdialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
        avgdialog.show();
        String file = avgdialog.getValue();
        if (file != null) {
            String shortfile = file;
            if (shortfile.toLowerCase().endsWith(".exp")) shortfile = shortfile.substring(0, shortfile.toLowerCase().lastIndexOf(".exp"));
            if (shortfile.lastIndexOf(File.separator) != -1) shortfile = shortfile.substring(shortfile.lastIndexOf(File.separator) + 1);
            String old = avgdialog.getExpFilePath();
            if (old != null && old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
            if (old != null && old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
            openProject.addFile(file);
            this.addExpFile(openProject.getPath() + file);
            if (old != null && openProject.getGroupMethod() == Project.ALWAYS_CREATE) {
                String groupFiles[] = openProject.getGroupFiles(old);
                for (int i = 0; i < groupFiles.length; i++) {
                    GrpFile gf = new GrpFile(new File(openProject.getPath() + groupFiles[i]));
                    gf.setExpFile(shortfile);
                    Object o[] = gf.getGroup();
                    for (int p = 0; p < o.length; p++) {
                        if (o[p] != null && !((String) o[p]).trim().equals("")) {
                            String geneName = (String) o[p];
                            int loc = -1;
                            if ((loc = geneName.lastIndexOf("_rep")) != -1) {
                                if (openProject.getAverageReplicateMethod() == Project.NEVER_ADD_REPLICATES) gf.remove(o[p]); else {
                                    int count = 1;
                                    String shortname = geneName.substring(0, loc);
                                    gf.remove(o[p]);
                                    for (int j = i + 1; j < o.length; j++) {
                                        int oloc = -1;
                                        if ((oloc = ((String) o[j]).lastIndexOf("_rep")) != -1) {
                                            if (((String) o[j]).substring(0, oloc).equalsIgnoreCase(shortname)) {
                                                count++;
                                                gf.remove(o[j]);
                                                o[j] = null;
                                            }
                                        }
                                    }
                                    if (openProject.getAverageReplicateMethod() == Project.NEVER_ADD_REPLICATES) ; else if (openProject.getAverageReplicateMethod() == Project.ANY_ADD_REPLICATES) {
                                        gf.addOne(shortname);
                                    } else {
                                        int expcount = 0;
                                        ExpFile tempExp = new ExpFile(new File(openProject.getPath() + old + File.separator + old + ".exp"));
                                        for (int q = 0; q < tempExp.numGenes(); q++) {
                                            String ttempname = tempExp.getGeneName(q);
                                            int eloc = -1;
                                            if ((eloc = ttempname.lastIndexOf("_rep")) != -1) {
                                                if (ttempname.substring(0, eloc).equalsIgnoreCase(shortname)) expcount++;
                                            }
                                        }
                                        if (openProject.getAverageReplicateMethod() == Project.ALL_ADD_REPLICATES) {
                                            if (count >= expcount) gf.addOne(shortname);
                                        } else if (openProject.getAverageReplicateMethod() == Project.HALF_ADD_REPLICATES) {
                                            if (count >= (expcount / 2)) gf.addOne(shortname);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    try {
                        gf.writeGrpFile(openProject.getPath() + shortfile + File.separator + gf.getTitle());
                        openProject.addFile(shortfile + File.separator + gf.getTitle());
                    } catch (DidNotFinishException e3) {
                    }
                }
            }
            expMain = new ExpFile(new File(openProject.getPath() + file));
        }
    }

    private void scramble(int type) {
        final int method = type;
        Thread thread = new Thread() {

            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                ExpFile scram = new ExpFile(expMain.getExpFile());
                scram.scramble(method);
                writeExpFile(scram, expMain.name + "_scr.exp", expMain.getName(), (method != ExpFile.SCRAMBLE_ORDER));
                setCursor(Cursor.getDefaultCursor());
            }
        };
        thread.start();
    }

    private void scramOrder_actionPerformed(ActionEvent e) {
        scramble(ExpFile.SCRAMBLE_ORDER);
    }

    private void scramGene_actionPerformed(ActionEvent e) {
        scramble(ExpFile.SCRAMBLE_WITHIN_GENE);
    }

    private void scramColumn_actionPerformed(ActionEvent e) {
        scramble(ExpFile.SCRAMBLE_WITHIN_COLUMN);
    }

    private void scramBoth_actionPerformed(ActionEvent e) {
        scramble(ExpFile.SCRAMBLE_WITHIN_BOTH);
    }

    /**
     * saves an expression file to a disk drive using the name specified by the user
     * @param exp expression file to save to disk
     * @param defaultName generated name of the expression file
     * @param oldExpFile name of old expression file
     * @param modify whether or not the data is modified
     */
    public void writeExpFile(ExpFile exp, String defaultName, String oldExpFile, boolean modify) {
        String old = null;
        if (oldExpFile != null && !oldExpFile.trim().equals("")) {
            old = oldExpFile;
            if (old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
            if (old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
        }
        String file = (String) JOptionPane.showInputDialog(this, "Please Enter New Expression File Name", "Select File Name", JOptionPane.QUESTION_MESSAGE, null, null, defaultName);
        if (file != null) {
            if (file.toLowerCase().endsWith(".exp")) file = file.substring(0, file.lastIndexOf("."));
            File f = new File(openProject.getPath() + file + File.separator + file + ".exp");
            int deleteFiles = JOptionPane.CANCEL_OPTION;
            if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(this, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
                try {
                    if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                    f.getParentFile().mkdirs();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath()));
                    for (int i = 0; i < exp.getColumns(); i++) {
                        bw.write(exp.getLabel(i) + "\t");
                    }
                    bw.write("\n");
                    for (int i = 0; i < exp.numGenes(); i++) {
                        bw.write(exp.getGeneName(i) + "\t");
                        double data[] = exp.getData(i);
                        for (int j = 0; j < data.length; j++) {
                            bw.write("" + data[j] + (j == data.length - 1 ? "" : "\t"));
                        }
                        String comments;
                        if ((comments = exp.getGene(i).getComments()) != null) bw.write("\t" + comments);
                        bw.write("\n");
                    }
                    bw.write("/**Gene Info**/" + "\n");
                    for (int i = 0; i < exp.numGenes(); i++) {
                        Gene g = exp.getGene(i);
                        String n = g.getName();
                        String a = g.getAlias();
                        String c = g.getChromo();
                        String l = g.getLocation();
                        String p = g.getProcess();
                        String fl = g.getFunction();
                        String co = g.getComponent();
                        if (n != null) bw.write(n + "\t" + (a != null ? a : " ") + "\t" + (c != null ? c : " ") + "\t" + (l != null ? l : " ") + "\t" + (p != null ? p : " ") + "\t" + (fl != null ? fl : " ") + "\t" + (co != null ? co : " ") + "\n");
                    }
                    bw.close();
                    String filename = file + File.separator + file + ".exp";
                    openProject.addFile(filename);
                    addExpFile(f.getPath());
                    if (old != null && (openProject.getGroupMethod() == 0 || (!modify && openProject.getGroupMethod() == 1))) {
                        String groupFiles[] = openProject.getGroupFiles(old);
                        for (int i = 0; i < groupFiles.length; i++) {
                            GrpFile gf = new GrpFile(new File(openProject.getPath() + groupFiles[i]));
                            gf.setExpFile(file);
                            try {
                                gf.writeGrpFile(openProject.getPath() + file + File.separator + gf.getTitle());
                                openProject.addFile(file + File.separator + gf.getTitle());
                            } catch (DidNotFinishException e3) {
                            }
                        }
                    }
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(this, "Error Writing Exp File");
                }
            } else writeExpFile(exp, defaultName, oldExpFile, modify);
        }
    }

    private void limitDataChoice_actionPerformed(ActionEvent e) {
        try {
            LimitFrame lf = new LimitFrame(expMain, openProject, this);
            lf.setModal(true);
            lf.pack();
            lf.setResizable(true);
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            lf.setLocation((screen.width - lf.getWidth()) / 2, (screen.height - lf.getHeight()) / 2);
            lf.show();
            lf.toFront();
            String file = lf.getValue();
            if (file != null) {
                String shortfile = file;
                if (shortfile.toLowerCase().endsWith(".exp")) shortfile = shortfile.substring(0, shortfile.toLowerCase().lastIndexOf(".exp"));
                if (shortfile.lastIndexOf(File.separator) != -1) shortfile = shortfile.substring(shortfile.lastIndexOf(File.separator) + 1);
                String old = expMain.getName();
                if (old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
                if (old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
                openProject.addFile(file);
                this.addExpFile(openProject.getPath() + file);
                if (openProject.getGroupMethod() == 0) {
                    String groupFiles[] = openProject.getGroupFiles(old);
                    for (int i = 0; i < groupFiles.length; i++) {
                        GrpFile gf = new GrpFile(new File(openProject.getPath() + groupFiles[i]));
                        gf.setExpFile(shortfile);
                        try {
                            gf.writeGrpFile(openProject.getPath() + shortfile + File.separator + gf.getTitle());
                            openProject.addFile(shortfile + File.separator + gf.getTitle());
                        } catch (DidNotFinishException e3) {
                        }
                    }
                }
                expMain = new ExpFile(new File(openProject.getPath() + file));
            }
        } catch (Exception e2) {
            JOptionPane.showMessageDialog(this, "Error! Could Not Create New Expression File.", "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void taskmanChoice_actionPerformed(ActionEvent e) {
        tmFrame.show();
    }

    private void addtaskChoice_actionPerformed(ActionEvent e) {
        TaskBuilderFrame taskdisplay = new TaskBuilderFrame(openProject, taskManager, this);
        taskdisplay.setVisible(true);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        taskdisplay.pack();
        jdesktoppane.add(taskdisplay);
        taskdisplay.setLocation((screen.width - taskdisplay.getWidth()) / 2, (screen.height - taskdisplay.getHeight()) / 2);
        taskdisplay.show();
    }

    private void helpChoice_actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(this, "Help for MAGIC Tool Will Be Available Shortly");
    }

    private void aboutChoice_actionPerformed(ActionEvent e) {
        AboutFrame about = new AboutFrame(this);
    }

    private void licenseChoice_actionPerformed(ActionEvent e) {
        TextViewer viewer = new TextViewer(this, this.getClass().getResourceAsStream("license/gpl.txt"), "MAGIC Tool License");
    }

    private void loadRed_actionPerformed(ActionEvent e) {
        if (redPath != null) {
            loadRed.setState(true);
        }
        MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.tifFilter);
        MainFrame.fileLoader.setDialogTitle("Load Red Image File...");
        MainFrame.fileLoader.setApproveButtonText("Load");
        File f = new File(openProject.getPath() + "images" + File.separator);
        if (!f.exists()) f.mkdirs();
        MainFrame.fileLoader.setCurrentDirectory(f);
        MainFrame.fileLoader.setSelectedFile(null);
        int redResult = MainFrame.fileLoader.showOpenDialog(null);
        if (redResult == JFileChooser.APPROVE_OPTION) {
            File file = MainFrame.fileLoader.getSelectedFile();
            redPath = file.getAbsolutePath();
            Opener redImage = new Opener();
            ImagePlus ipred = redImage.openImage(redPath);
            redDim = new Dimension(ipred.getWidth(), ipred.getHeight());
            loadRed.setText("Red: " + redPath);
            if (redPath != null && greenDim.width == redDim.width && greenDim.height == redDim.height) {
                loadGeneListChoice.setEnabled(true);
                loadList.setEnabled(true);
                manager.setGridNum(0);
                segmentChoice.setEnabled(false);
            } else {
                loadGeneListChoice.setEnabled(false);
                gridChoice.setEnabled(false);
                segmentChoice.setEnabled(false);
            }
        } else {
            if ((redResult == JFileChooser.CANCEL_OPTION) && (redPath == null)) {
                loadRed.setState(false);
            }
        }
    }

    private void loadGreen_actionPerformed(ActionEvent e) {
        if (greenPath != null) {
            loadGreen.setState(true);
        }
        MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.tifFilter);
        MainFrame.fileLoader.setDialogTitle("Load Green Image File...");
        MainFrame.fileLoader.setApproveButtonText("Load");
        File f = new File(openProject.getPath() + "images" + File.separator);
        if (!f.exists()) f.mkdirs();
        MainFrame.fileLoader.setCurrentDirectory(f);
        MainFrame.fileLoader.setSelectedFile(null);
        int greenResult = MainFrame.fileLoader.showOpenDialog(null);
        if (greenResult == JFileChooser.APPROVE_OPTION) {
            File file = MainFrame.fileLoader.getSelectedFile();
            greenPath = file.getAbsolutePath();
            Opener greenImage = new Opener();
            ImagePlus ipgreen = greenImage.openImage(greenPath);
            greenDim = new Dimension(ipgreen.getWidth(), ipgreen.getHeight());
            loadGreen.setText("Green: " + greenPath);
            if (redPath != null && greenDim.width == redDim.width && greenDim.height == redDim.height) {
                loadGeneListChoice.setEnabled(true);
                loadList.setEnabled(true);
                manager.setGridNum(0);
                segmentChoice.setEnabled(false);
            } else {
                loadGeneListChoice.setEnabled(false);
                gridChoice.setEnabled(false);
                segmentChoice.setEnabled(false);
            }
        } else {
            if ((greenResult == JFileChooser.CANCEL_OPTION) && (greenPath == null)) {
                loadGreen.setState(false);
            }
        }
    }

    private void loadList_actionPerformed(ActionEvent e) {
        if (geneListPath != null) {
            loadList.setState(true);
        }
        MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.txtFilter);
        MainFrame.fileLoader.setDialogTitle("Load Gene List File...");
        MainFrame.fileLoader.setApproveButtonText("Load");
        File f = new File(openProject.getPath() + "lists" + File.separator);
        if (!f.exists()) f.mkdirs();
        MainFrame.fileLoader.setCurrentDirectory(f);
        MainFrame.fileLoader.setSelectedFile(null);
        int result = MainFrame.fileLoader.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = MainFrame.fileLoader.getSelectedFile();
            geneListPath = file.getAbsolutePath();
            loadList.setText(geneListPath);
            try {
                geneList = new GeneList(geneListPath);
            } catch (Exception e1) {
                JOptionPane.showMessageDialog(null, "Error! Could Not Open Gene List");
            }
            if (geneList != null) {
                if (manager.getGeneList() != null && manager.getGeneListSize() != geneList.getNumGenes()) segmentChoice.setEnabled(false);
                manager.setGeneList(geneList);
                gridChoice.setEnabled(true);
            }
        } else {
            if ((result == JFileChooser.CANCEL_OPTION) && (geneListPath == null)) {
                loadList.setState(false);
            }
        }
    }

    private void gridChoice_actionPerformed(ActionEvent e) {
        if (griddingFrame == null) {
            griddingFrame = new GriddingFrame(openProject, manager, redPath, greenPath, this);
            griddingFrame.addInternalFrameListener(new InternalFrameAdapter() {

                public void internalFrameClosed(InternalFrameEvent e) {
                    segmentChoice.setEnabled(griddingFrame.canSegment());
                    griddingFrame = null;
                    loadImagePair.setEnabled(true);
                }
            });
            jdesktoppane.add(griddingFrame);
            griddingFrame.pack();
            griddingFrame.setBounds(20, 20, jdesktoppane.getWidth() - 40, jdesktoppane.getHeight() - 40);
            griddingFrame.fillScreen();
            griddingFrame.show();
            griddingFrame.toFront();
            griddingFrame.finalInit();
            if (griddingFrame != null) {
                loadImagePair.setEnabled(false);
                segmentChoice.setEnabled(griddingFrame.canSegment());
                if (sf != null && sf.isVisible()) griddingFrame.setAllowChanges(false);
            }
        } else {
            griddingFrame.toFront();
            try {
                griddingFrame.setIcon(false);
            } catch (Exception e2) {
            }
            loadImagePair.setEnabled(false);
            segmentChoice.setEnabled(griddingFrame.canSegment());
            if (sf != null && sf.isVisible()) griddingFrame.setAllowChanges(false);
        }
    }

    private void segmentChoice_actionPerformed(ActionEvent e) {
        final MainFrame mframe = this;
        if (sf == null || !sf.isVisible()) {
            Thread thread = new Thread() {

                public void run() {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    Opener greenImage = new Opener();
                    Opener redImage = new Opener();
                    Image imageGreen = greenImage.openImage(greenPath).getImage();
                    Image imageRed = redImage.openImage(redPath).getImage();
                    sf = new SegmentFrame(imageRed, imageGreen, manager, openProject, mframe);
                    jdesktoppane.add(sf);
                    sf.addInternalFrameListener(new InternalFrameAdapter() {

                        public void internalFrameClosed(InternalFrameEvent e) {
                            loadImagePair.setEnabled(true);
                            loadGeneListChoice.setEnabled(true);
                            if (griddingFrame != null) griddingFrame.setAllowChanges(true);
                        }
                    });
                    sf.pack();
                    sf.setBounds(50, 50, jdesktoppane.getWidth() - 100, jdesktoppane.getHeight() - 100);
                    sf.jSplitPaneVert.setDividerLocation(0.5d);
                    sf.zoomToCell();
                    sf.setSpot(0, 0);
                    sf.show();
                    loadImagePair.setEnabled(false);
                    loadGeneListChoice.setEnabled(false);
                    if (griddingFrame != null) griddingFrame.setAllowChanges(false);
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
            };
            thread.start();
        } else {
            sf.toFront();
            try {
                sf.setIcon(false);
            } catch (Exception e2) {
            }
            loadImagePair.setEnabled(false);
            loadGeneListChoice.setEnabled(false);
            if (griddingFrame != null) griddingFrame.setAllowChanges(false);
        }
    }
}
