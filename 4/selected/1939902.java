package magictool.image;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import magictool.DidNotFinishException;
import magictool.ExpFile;
import magictool.Gene;
import magictool.GrpFile;
import magictool.MainFrame;
import magictool.ProgressFrame;
import magictool.Project;

/**
 * SegmentFrame is an internal frame which provides the graphical user interface that
 * allows users to complete the segmentation of microarray spots. It allows users to select the
 * desired segementation method and parameters and to save the generated data into a new
 * or appended expression file.
 */
public class SegmentFrame extends JInternalFrame {

    private JSplitPane jSplitPaneHoriz = new JSplitPane();

    public JSplitPane jSplitPaneVert = new JSplitPane();

    private JScrollPane scrollLeft;

    public JScrollPane scrollGreen;

    private JScrollPane scrollRed;

    private SegmentPanel infoPanel;

    private JLabel statusBar = new JLabel();

    public SegmentDisplay idGreen;

    public SegmentDisplay idRed;

    private BorderLayout borderLayout1 = new BorderLayout();

    private JPanel greenPanel = new JPanel();

    private JPanel redPanel = new JPanel();

    private JLabel greenTitle = new JLabel("Green Image: ");

    private JLabel redTitle = new JLabel("Red Image: ");

    private BorderLayout borderLayout2 = new BorderLayout();

    private BorderLayout borderLayout3 = new BorderLayout();

    private int w, h, newTopLeftX, newTopLeftY;

    private ProgressFrame progress = null;

    /**individual spot cell height*/
    protected double cellHeight;

    /**green image*/
    protected Image imageGreen;

    /**red image*/
    protected Image imageRed;

    /**grid manager for the microarray images*/
    protected GridManager manager;

    /**parent main frame*/
    protected MainFrame main;

    /**open project from the main frame*/
    protected Project project = null;

    /**polygon containing the coordinates of current spot cell*/
    protected Polygon cell;

    /**
   * Constructs the segment frame with the specified microarray images, grid manager, project, and main frame.
   * @param imageGreen green microarray image
   * @param imageRed red microarray image
   * @param m grid manager for the microarray images
   * @param project open project to place new expression files
   * @param main parent main frame
   */
    public SegmentFrame(Image imageRed, Image imageGreen, GridManager m, Project project, MainFrame main) {
        this.imageGreen = imageGreen;
        this.imageRed = imageRed;
        this.manager = m;
        this.project = project;
        this.main = main;
        idGreen = new SegmentDisplay(imageGreen, m);
        idRed = new SegmentDisplay(imageRed, m);
        infoPanel = new SegmentPanel(this.manager, this.idRed, this.idGreen, this);
        this.setClosable(true);
        this.setTitle("SEGMENTATION");
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setCurrentCell();
        showCurrentCell();
    }

    private void jbInit() throws Exception {
        greenTitle.setForeground(Color.green);
        greenTitle.setLabelFor(scrollGreen);
        redTitle.setForeground(Color.red);
        redTitle.setLabelFor(scrollRed);
        this.setIconifiable(true);
        this.setMaximizable(true);
        this.setResizable(true);
        scrollLeft = new JScrollPane(infoPanel);
        scrollGreen = new JScrollPane(idGreen);
        scrollRed = new JScrollPane(idRed);
        jSplitPaneHoriz.setOneTouchExpandable(true);
        jSplitPaneVert.setOneTouchExpandable(true);
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        greenPanel.setLayout(borderLayout2);
        greenPanel.add(greenTitle, BorderLayout.NORTH);
        greenPanel.add(scrollGreen, BorderLayout.CENTER);
        redPanel.setLayout(borderLayout3);
        redPanel.add(redTitle, BorderLayout.NORTH);
        redPanel.add(scrollRed, BorderLayout.CENTER);
        scrollGreen.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollGreen.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollRed.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollRed.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        jSplitPaneVert.add(greenPanel, JSplitPane.BOTTOM);
        jSplitPaneVert.add(redPanel, JSplitPane.TOP);
        jSplitPaneVert.setPreferredSize(new Dimension(800, 400));
        jSplitPaneHoriz.add(scrollLeft, JSplitPane.LEFT);
        jSplitPaneVert.setOrientation(JSplitPane.VERTICAL_SPLIT);
        jSplitPaneHoriz.add(jSplitPaneVert, JSplitPane.RIGHT);
        jSplitPaneHoriz.setDividerLocation(scrollLeft.getPreferredSize().width + 20);
        this.getContentPane().setBackground(Color.lightGray);
        this.getContentPane().add(statusBar, BorderLayout.SOUTH);
        this.getContentPane().add(jSplitPaneHoriz, BorderLayout.CENTER);
    }

    /**
   * sets the segement displays to show the current spot cells for both the red and green image
   */
    public void showCurrentCell() {
        setCurrentCell();
        newTopLeftX = ((int) (idGreen.getZoom() * cell.xpoints[0])) - 4;
        newTopLeftY = ((int) (idGreen.getZoom() * cell.ypoints[0])) - 4;
        scrollGreen.getViewport().setViewPosition(new Point(newTopLeftX, newTopLeftY));
        scrollRed.getViewport().setViewPosition(new Point(newTopLeftX, newTopLeftY));
    }

    /**
   * sets the magnification in both segment displays to the spot level
   */
    public void zoomToCell() {
        idGreen.zoom(((jSplitPaneVert.getHeight() - jSplitPaneVert.getDividerSize() - (2 * redPanel.getHeight())) / 2) / cellHeight);
        idRed.zoom(((jSplitPaneVert.getHeight() - jSplitPaneVert.getDividerSize() - (2 * redPanel.getHeight())) / 2) / cellHeight);
        showCurrentCell();
    }

    /**
   * sets the current cell coordinates
   */
    public void setCurrentCell() {
        Polygon p = manager.getCurrentGrid().getTranslatedPolygon();
        Polygon q = new Polygon();
        if (p != null) {
            for (int j = 0; j < p.xpoints.length; j++) {
                q.xpoints[j] = (int) ((idGreen.screenX(p.xpoints[j])) / idGreen.getZoom());
                q.ypoints[j] = (int) ((idGreen.screenX(p.ypoints[j])) / idGreen.getZoom());
            }
            manager.getCurrentGrid().setSpots(q);
            cell = manager.getCurrentGrid().getCurrentSpot();
        }
        cellHeight = cell.ypoints[3] - cell.ypoints[0];
    }

    /**
   * sets the current spot
   * @param grid grid number
   * @param spot spot number
   */
    public void setSpot(int grid, int spot) {
        infoPanel.setSpot(grid, spot);
    }

    /**
   * creates or appends an expression file with the generated data based on the user specified
   * segementation method and parameters
   * @param name name of the new expression file
   * @param colName data column name (AKA microarray designator)
   * @param newFile whether creating entirely new file or appending existing expression file
   * @param appendname name of the expression file to append
   * @param byname whether to append by gene name or by list order
   * @param method segmentation method
   * @param ratiomethod ratio method
   * @param params other segmentation parameters
   */
    public void createNewExpressionFile(String name, String colName, boolean newFile, String appendname, boolean byname, int method, int ratiomethod, Object params[]) {
        final String theName = name;
        final String colname = colName;
        final String appendName = appendname;
        final boolean byName = byname;
        final int meth = method;
        final int ratioMethod = ratiomethod;
        final Object[] par = params;
        final boolean createNew = newFile;
        if (project != null) {
            String file = theName;
            if (file.toLowerCase().endsWith(".exp")) file = file.substring(0, file.lastIndexOf("."));
            File f = new File(project.getPath() + file + File.separator + file + ".exp");
            int deleteFiles = JOptionPane.CANCEL_OPTION;
            if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(null, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
                try {
                    if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    f.getParentFile().mkdirs();
                    File temp = null;
                    if (!createNew) temp = File.createTempFile(file, null);
                    BufferedWriter bw;
                    if (createNew) bw = new BufferedWriter(new FileWriter(f.getPath())); else bw = new BufferedWriter(new FileWriter(temp.getPath()));
                    bw.write(colname);
                    bw.write("\n");
                    progress = new ProgressFrame("Creating New Expression File: " + file + ".exp");
                    if (!createNew) progress.setTitle("Generating Expression Data");
                    getDesktopPane().add(progress);
                    progress.show();
                    int totalNumSpots = 0;
                    for (int i = 0; i < manager.getGridNum(); i++) {
                        totalNumSpots += manager.getGrid(i).getNumOfSpots();
                    }
                    progress.setMaximum(totalNumSpots);
                    int num = 0;
                    for (int i = 0; i < manager.getGridNum(); i++) {
                        Grid g = manager.getGrid(i);
                        idRed.setSpots(i);
                        for (int j = 0; j < g.getNumOfSpots(); j++) {
                            int aspot = manager.getActualSpotNum(i, j);
                            String gname = manager.getGeneName(i, aspot);
                            if (!gname.equalsIgnoreCase("empty") && !gname.equalsIgnoreCase("blank") && !gname.equalsIgnoreCase("missing") && !gname.equalsIgnoreCase("none") && !gname.equalsIgnoreCase("No Gene Specified")) {
                                SingleGeneImage currentGene = new SingleGeneImage(idRed.getCellPixels(i, aspot), idGreen.getCellPixels(i, aspot), idRed.getCellHeight(i, aspot), idRed.getCellWidth(i, aspot));
                                GeneData gd = null;
                                gd = currentGene.getData(meth, par);
                                bw.write(gname + "\t");
                                if (gd != null) {
                                    bw.write(String.valueOf(gd.getRatio(ratioMethod)));
                                } else {
                                    bw.write("\t");
                                }
                                bw.write("\n");
                                progress.addValue(1);
                                num++;
                            }
                        }
                    }
                    bw.close();
                    boolean add = true;
                    String app = appendName;
                    if (!createNew && appendName.toLowerCase().endsWith(".exp")) app = appendName.substring(0, appendName.lastIndexOf("."));
                    if (!createNew) add = mergeFiles(f, new File(project.getPath() + app + File.separator + app + ".exp"), temp, byName);
                    if (add) project.addFile(file + File.separator + file + ".exp");
                    progress.dispose();
                    if (add) main.addExpFile(f.getPath());
                    if (!createNew && add && project.getGroupMethod() == Project.ALWAYS_CREATE) {
                        String shortfile = theName;
                        if (shortfile.toLowerCase().endsWith(".exp")) shortfile = shortfile.substring(0, shortfile.toLowerCase().lastIndexOf(".exp"));
                        if (shortfile.lastIndexOf(File.separator) != -1) shortfile = shortfile.substring(shortfile.lastIndexOf(File.separator) + 1);
                        String old = appendName;
                        if (old != null && old.endsWith(".exp")) old = old.substring(0, old.lastIndexOf(".exp"));
                        if (old != null && old.lastIndexOf(File.separator) != -1) old = old.substring(old.lastIndexOf(File.separator) + 1);
                        String groupFiles[] = project.getGroupFiles(old);
                        for (int i = 0; i < groupFiles.length; i++) {
                            GrpFile gf = new GrpFile(new File(project.getPath() + groupFiles[i]));
                            gf.setExpFile(shortfile);
                            try {
                                gf.writeGrpFile(project.getPath() + shortfile + File.separator + gf.getTitle());
                                project.addFile(shortfile + File.separator + gf.getTitle());
                            } catch (DidNotFinishException e3) {
                            }
                        }
                    }
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(null, "Error Writing Exp File");
                    f.delete();
                    if (progress != null) progress.dispose();
                    e2.printStackTrace();
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**writes the raw expression data to disk in a tab-delimited format.  The .exp file and column name are stored as well as headers.
   * 
   * @param rawFileName name to save the .raw file as
   * @param expFileName name of the corresponding .exp file (defaults to unknown)
   * @param colName name of column of corresponding .exp file (defaults to unknown)
   * @param method segmentation method
   * @param ratioMethod ratio method
   * @param params segmentation parameters
   */
    public void createNewRawDataFile(String rawFileName, String expFileName, String colName, int method, int ratioMethod, Object params[]) {
        System.out.println("top of createNewRawDataFile");
        if (rawFileName.toLowerCase().endsWith(".raw")) rawFileName = rawFileName.substring(0, rawFileName.lastIndexOf("."));
        final String file = rawFileName;
        File f = new File(project.getPath() + file + File.separator + file + ".raw");
        int deleteFiles = JOptionPane.CANCEL_OPTION;
        if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(null, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
            try {
                if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                f.getParentFile().mkdirs();
                System.out.println("file: " + f);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        progress = new ProgressFrame("creating new raw data file: " + file + ".raw");
                    }
                });
                getDesktopPane().add(progress);
                progress.show();
                int totalNumSpots = 0;
                for (int i = 0; i < manager.getGridNum(); i++) totalNumSpots += manager.getGrid(i).getNumOfSpots();
                progress.setMaximum(totalNumSpots);
                BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath()), 4096);
                bw.write(expFileName + ":" + colName + "\tRedFGtot\tRedBGtot\tGrnFGtot\tGrnBGtot\tRedFGavg\tRedBGavg\tGrnFGavg\tGrnBGavg");
                bw.write("\n");
                for (int i = 0; i < manager.getGridNum(); i++) {
                    Grid g = manager.getGrid(i);
                    idRed.setSpots(i);
                    for (int j = 0; j < g.getNumOfSpots(); j++) {
                        int aspot = manager.getActualSpotNum(i, j);
                        String gname = manager.getGeneName(i, aspot);
                        if (!gname.equalsIgnoreCase("empty") && !gname.equalsIgnoreCase("blank") && !gname.equalsIgnoreCase("missing") && !gname.equalsIgnoreCase("none") && !gname.equalsIgnoreCase("No Gene Specified")) {
                            SingleGeneImage currentGene = new SingleGeneImage(idRed.getCellPixels(i, aspot), idGreen.getCellPixels(i, aspot), idRed.getCellHeight(i, aspot), idRed.getCellWidth(i, aspot));
                            GeneData gd = null;
                            gd = currentGene.getData(method, params);
                            bw.write(gname + "\t");
                            if (gd != null) {
                                bw.write(String.valueOf(gd.getRedForegroundTotal()) + "\t");
                                bw.write(String.valueOf(gd.getRedBackgroundTotal()) + "\t");
                                bw.write(String.valueOf(gd.getGreenForegroundTotal()) + "\t");
                                bw.write(String.valueOf(gd.getGreenBackgroundTotal()) + "\t");
                                bw.write(String.valueOf(gd.getRedForegroundAvg()) + "\t");
                                bw.write(String.valueOf(gd.getRedBackgroundAvg()) + "\t");
                                bw.write(String.valueOf(gd.getGreenForegroundAvg()) + "\t");
                                bw.write(String.valueOf(gd.getGreenBackgroundAvg()));
                            } else {
                                bw.write("\t");
                            }
                            bw.write("\n");
                            progress.addValue(1);
                            progress.dispose();
                        }
                    }
                }
                bw.close();
                System.out.println("bottom of createNewRawDataFile");
            } catch (Exception e2) {
                JOptionPane.showMessageDialog(null, "Error Writing .raw File");
                f.delete();
                if (progress != null) progress.dispose();
                e2.printStackTrace();
            }
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private boolean mergeFiles(File output, File append, File temp, boolean byName) {
        if (temp != null && append.exists() && temp.exists()) {
            try {
                output.getParentFile().mkdirs();
                ExpFile exp1 = new ExpFile(append);
                ExpFile exp2 = new ExpFile(temp);
                progress.setTitle("Appending Expression File");
                progress.setValue(0);
                BufferedWriter bw = new BufferedWriter(new FileWriter(output));
                for (int i = 0; i < exp1.getColumns(); i++) {
                    bw.write(exp1.getLabel(i) + "\t");
                }
                bw.write(exp2.getLabel(0));
                bw.write("\n");
                if (byName) {
                    progress.setMaximum(Math.max(exp1.numGenes(), exp2.numGenes()) + exp1.numGenes());
                    boolean[] usedNew = new boolean[exp2.numGenes()];
                    for (int j = 0; j < usedNew.length; j++) {
                        usedNew[j] = false;
                    }
                    for (int i = 0; i < exp1.numGenes(); i++) {
                        int pos;
                        String comments = "", comments1 = null, comments2 = null;
                        bw.write(exp1.getGeneName(i) + "\t");
                        double data[] = exp1.getData(i);
                        for (int j = 0; j < data.length; j++) {
                            bw.write("" + data[j] + "\t");
                        }
                        comments1 = exp1.getGene(i).getComments();
                        if ((pos = exp2.findGeneName(exp1.getGeneName(i))) != -1) {
                            double data2[] = exp2.getData(pos);
                            bw.write("" + data2[0] + "\t");
                            comments2 = exp2.getGene(pos).getComments();
                            usedNew[pos] = true;
                        } else bw.write("" + Double.POSITIVE_INFINITY + "\t");
                        if (comments1 != null) comments += comments1 + " ";
                        if (comments2 != null) comments += comments2;
                        bw.write(comments);
                        bw.write("\n");
                        progress.addValue(1);
                    }
                    for (int j = 0; j < usedNew.length; j++) {
                        if (usedNew[j] == false) {
                            bw.write(exp2.getGeneName(j) + "\t");
                            double data[] = exp1.getData(0);
                            for (int p = 0; p < data.length; p++) {
                                bw.write("" + Double.POSITIVE_INFINITY + "\t");
                            }
                            double data2[] = exp2.getData(j);
                            bw.write("" + data2[0] + "\t");
                            String comments = "", comments2;
                            comments2 = exp2.getGene(j).getComments();
                            if (comments2 != null) comments = comments2;
                            bw.write(comments);
                            bw.write("\n");
                            progress.addValue(1);
                        }
                    }
                } else {
                    for (int i = 0; i < exp1.numGenes(); i++) {
                        String comments = "", comments1 = null, comments2 = null;
                        bw.write(exp1.getGeneName(i) + "\t");
                        double data[] = exp1.getData(i);
                        for (int j = 0; j < data.length; j++) {
                            bw.write("" + data[j] + "\t");
                        }
                        comments1 = exp1.getGene(i).getComments();
                        if (exp2.numGenes() > i) {
                            double data2[] = exp2.getData(i);
                            bw.write("" + data2[0] + "\t");
                            comments2 = exp2.getGene(i).getComments();
                        } else bw.write("" + Double.POSITIVE_INFINITY + "\t");
                        if (comments1 != null) comments += comments1 + " ";
                        if (comments2 != null) comments += comments2;
                        bw.write(comments);
                        bw.write("\n");
                        progress.addValue(1);
                    }
                }
                bw.write("/**Gene Info**/" + "\n");
                for (int i = 0; i < exp1.numGenes(); i++) {
                    Gene g = exp1.getGene(i);
                    String n = g.getName();
                    String a = g.getAlias();
                    String c = g.getChromo();
                    String l = g.getLocation();
                    String p = g.getProcess();
                    String fl = g.getFunction();
                    String co = g.getComponent();
                    if (n != null) bw.write(n + "\t" + (a != null ? a : " ") + "\t" + (c != null ? c : " ") + "\t" + (l != null ? l : " ") + "\t" + (p != null ? p : " ") + "\t" + (fl != null ? fl : " ") + "\t" + (co != null ? co : " ") + "\n");
                    progress.addValue(1);
                }
                bw.close();
                return true;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error! Could Not Append File");
                e.printStackTrace();
                temp.delete();
                return false;
            }
        } else {
            JOptionPane.showMessageDialog(this, "Error! Could Not Append File");
            if (temp != null && temp.exists()) temp.delete();
            return false;
        }
    }
}
