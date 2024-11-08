package magictool.clusterdisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.Hashtable;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import magictool.ColorLabel;
import magictool.DidNotFinishException;
import magictool.ExpFile;
import magictool.GrpFile;
import magictool.MainFrame;
import magictool.PrintableTable;
import magictool.Project;
import magictool.VerticalLayout;
import magictool.cluster.AbstractCluster;
import magictool.dissim.DissimListPanel;
import magictool.filefilters.GifFilter;
import magictool.filefilters.NoEditFileChooser;
import slider.MThumbSlider;
import slider.MetalMThumbSliderUI;

/**
 * MetricTreeTableFrame displays a table containing colored data of the genes in the order they
 * appear in the cluster. The frame also displays either a metric tree or a list of dissimilarities
 * on the side of the table depending upon which type of cluster is displayed.
 */
public class MetricTreeTableFrame extends JInternalFrame {

    private JPanel side;

    private JPanel heightPanel = new JPanel();

    private JButton heightButton = new JButton();

    private JTextField heightField = new JTextField();

    private JScrollPane jScrollPane = new JScrollPane();

    private JViewport viewport;

    private JPanel buttonPanel = new JPanel();

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private VerticalLayout verticalLayout2 = new VerticalLayout();

    private JMenuBar menuBar = new JMenuBar();

    private JMenu fileMenu = new JMenu();

    private JMenuItem saveMenu = new JMenuItem();

    private JMenuItem saveGrpMenu = new JMenuItem();

    private JMenuItem printMenu = new JMenuItem();

    private JMenuItem closeMenu = new JMenuItem();

    private JMenu editMenu = new JMenu();

    private JMenuItem decimalMenu = new JMenuItem();

    private JMenu colorMenu = new JMenu();

    private JLabel centerLabel = new JLabel("", JLabel.CENTER);

    private JLabel white = new JLabel();

    private JLabel black = new JLabel("", JLabel.RIGHT);

    private JPanel sliderPanel = new JPanel();

    private JPanel labelsPanel = new JPanel();

    private JCheckBoxMenuItem rgmenu = new JCheckBoxMenuItem();

    private JCheckBoxMenuItem graymenu = new JCheckBoxMenuItem();

    private DecimalFormat labelFormat = new DecimalFormat("0.##");

    private MThumbSlider mSlider;

    private float actualmin, actualmax;

    /**table to display colored gene data*/
    protected PrintableTable table;

    /**minimum value of gene data*/
    protected float minvalue;

    /**maximum value of gene data*/
    protected float maxvalue;

    /**center value of gene data*/
    protected float centervalue;

    /**name of cluster file to display*/
    protected File clustFile;

    /**expression file associated with the cluster file*/
    protected ExpFile exp;

    /**whether or not to use a metric tree in the display*/
    private boolean useTree = false;

    /**color label to display the gradient of colors*/
    protected ColorLabel colorLabel;

    /**width of the side list of dissimilarities or metric tree*/
    protected int colwidth = 40;

    /**parent frame*/
    protected Frame parentFrame;

    /**project associated with metric tree table*/
    protected Project project;

    /**
   * Constructs the frame from a specified cluster file and expression file. The constructor
   * determines the type of cluster to decide whether to display a list of dissimilarities
   * or a metric tree representation of the dissimilarities.
   * @param clustFile name of the cluster file to display
   * @param exp associated expression file
   * @param parentFrame parent frame
   * @param project open project
   */
    public MetricTreeTableFrame(File clustFile, ExpFile exp, Frame parentFrame, Project project) {
        this.exp = exp;
        this.clustFile = clustFile;
        this.parentFrame = parentFrame;
        this.project = project;
        try {
            String[] info = AbstractCluster.readHeaders(clustFile.getPath());
            if (info[5].equals("Hierarchical")) useTree = true; else useTree = false;
        } catch (Exception e) {
        }
        if (useTree) side = new MetricTree(clustFile, false, false, 4, colwidth); else side = new DissimListPanel(clustFile, 4);
        table = new PrintableTable(exp, (useTree ? ((MetricTree) side).getGroupOfLeaves() : ((DissimListPanel) side).getGeneVector()), PrintableTable.REDGREEN);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        jScrollPane.getViewport().setBackground(new Color(204, 204, 204));
        this.setClosable(true);
        this.setJMenuBar(menuBar);
        this.setMaximizable(true);
        this.setResizable(true);
        heightButton.setText("Update Line Height");
        heightButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                heightButton_actionPerformed(e);
            }
        });
        buttonPanel.setLayout(verticalLayout1);
        minvalue = actualmin = exp.getMinExpValue();
        maxvalue = actualmax = exp.getMaxExpValue();
        centervalue = minvalue + (maxvalue - minvalue) / 2;
        colorLabel = new ColorLabel((double) minvalue, (double) maxvalue, (double) minvalue, (double) maxvalue, (double) centervalue, Color.green, Color.black, Color.red);
        colorLabel.setText("colorLabel");
        colorLabel.showLabels();
        table.setRowHeight(10);
        int n = 3;
        mSlider = new MThumbSlider(n, 0, 1000);
        mSlider.setUI(new MetalMThumbSliderUI());
        heightField.setColumns(5);
        heightField.setText("" + table.getRowHeight());
        if (useTree) ((MetricTree) side).setLineHeight(10); else ((DissimListPanel) side).setLineHeight(10);
        table.paintTable(minvalue, centervalue, maxvalue, table.getRowHeight());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());
        fileMenu.setText("File");
        saveMenu.setText("Save Image As...");
        saveMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveMenu_actionPerformed(e);
            }
        });
        saveGrpMenu.setText("Save Selected As Group...");
        saveGrpMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveGrpMenu_actionPerformed(e);
            }
        });
        printMenu.setText("Print");
        printMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK));
        printMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                printMenu_actionPerformed(e);
            }
        });
        closeMenu.setText("Close");
        closeMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK));
        closeMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeMenu_actionPerformed(e);
            }
        });
        editMenu.setText("Edit");
        decimalMenu.setText("Decimal Places");
        decimalMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                decimalMenu_actionPerformed(e);
            }
        });
        colorMenu.setText("Color");
        rgmenu.setText("Red/Green");
        rgmenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                rgmenu_actionPerformed(e);
            }
        });
        graymenu.setText("Grayscale");
        graymenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                graymenu_actionPerformed(e);
            }
        });
        setParams();
        buttonPanel.add(sliderPanel, null);
        buttonPanel.add(colorLabel, null);
        buttonPanel.add(heightPanel, null);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        JPanel p = new JPanel(new FlowLayout());
        side.setPreferredSize(new Dimension(colwidth, table.getRowCount() * 10));
        viewport = new JViewport();
        viewport.setView(side);
        viewport.setPreferredSize(new Dimension(colwidth, table.getRowCount() * 10));
        viewport.setScrollMode(viewport.SIMPLE_SCROLL_MODE);
        jScrollPane.getViewport().add(table, null);
        jScrollPane.setRowHeader(viewport);
        this.getContentPane().add(jScrollPane, BorderLayout.CENTER);
        heightPanel.add(heightField, null);
        heightPanel.add(heightButton, null);
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(colorMenu);
        fileMenu.add(saveGrpMenu);
        fileMenu.add(saveMenu);
        fileMenu.add(printMenu);
        fileMenu.addSeparator();
        fileMenu.add(closeMenu);
        editMenu.add(decimalMenu);
        colorMenu.add(rgmenu);
        colorMenu.add(graymenu);
        repaint();
    }

    private void setParams() {
        mSlider = new MThumbSlider(3, 0, 1000);
        mSlider.setUI(new MetalMThumbSliderUI());
        mSlider.setValueAt(0, 0);
        mSlider.setValueAt(1000, 1);
        mSlider.setValueAt(500, 2);
        white.setForeground(new Color(51, 153, 51));
        black.setForeground(new Color(255, 0, 0));
        centerLabel.setForeground(new Color(0, 0, 0));
        mSlider.setFillColorAt(Color.green, 0);
        mSlider.setFillColorAt(Color.black, 1);
        mSlider.setTrackFillColor(Color.red);
        Hashtable imageDictionary = new Hashtable();
        imageDictionary.put(new Integer(0), new JLabel(labelFormat.format(convertSlider(0))));
        imageDictionary.put(new Integer(250), new JLabel(labelFormat.format(convertSlider(250))));
        imageDictionary.put(new Integer(500), new JLabel(labelFormat.format(convertSlider(500))));
        imageDictionary.put(new Integer(750), new JLabel(labelFormat.format(convertSlider(750))));
        imageDictionary.put(new Integer(1000), new JLabel(labelFormat.format(convertSlider(1000))));
        mSlider.setMinorTickSpacing(1000 / 8);
        mSlider.setMajorTickSpacing(1000 / 4);
        mSlider.setPaintTicks(true);
        mSlider.setPaintLabels(true);
        mSlider.setLabelTable(imageDictionary);
        labelsPanel = new JPanel(new GridLayout(1, 3));
        sliderPanel.setLayout(verticalLayout2);
        sliderPanel.add(labelsPanel);
        sliderPanel.add(mSlider);
        labelsPanel.add(white);
        labelsPanel.add(centerLabel);
        labelsPanel.add(black);
        mSlider.setMiddleRange();
        mSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                mSlider_stateChanged(e);
            }
        });
        centerLabel.setText("Center: " + labelFormat.format(convertSlider(mSlider.getValueAt(2))));
        white.setText("Green: " + labelFormat.format(convertSlider(mSlider.getValueAt(0))));
        black.setText("Red: " + labelFormat.format(convertSlider(mSlider.getValueAt(1))));
    }

    private void heightButton_actionPerformed(ActionEvent e) {
        try {
            int i = Integer.parseInt(heightField.getText().trim());
            table.setRowHeight(i);
            if (useTree) ((MetricTree) side).setLineHeight(i); else ((DissimListPanel) side).setLineHeight(i);
            table.paintTable(minvalue, centervalue, maxvalue, table.getRowHeight());
            side.setPreferredSize(new Dimension(colwidth, table.getRowCount() * i));
            viewport = new JViewport();
            viewport.setView(side);
            viewport.setPreferredSize(new Dimension(colwidth, table.getRowCount() * i));
            viewport.setScrollMode(viewport.SIMPLE_SCROLL_MODE);
            jScrollPane.setRowHeader(viewport);
            repaint();
        } catch (Exception e1) {
            heightField.setText("" + table.getRowHeight());
        }
    }

    private void saveMenu_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
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
                        table.setHeader(colorLabel);
                        table.setSidebar(side);
                        saveImage(name);
                    }
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(null, "Failed To Create Image");
                }
            }
        };
        thread.start();
    }

    private void saveImage(String name) {
        try {
            int number = 1;
            table.saveImage(name, number = (int) Math.ceil(table.getMegaPixels() / project.getImageSize()));
            if (number > 1) {
                String tn = name.substring(name.lastIndexOf(File.separator), name.lastIndexOf("."));
                String tempname = name.substring(0, name.lastIndexOf(File.separator)) + tn + ".html";
                BufferedWriter bw = new BufferedWriter(new FileWriter(tempname));
                bw.write("<html><header><title>" + name + "</title></header>");
                bw.write("<body>");
                bw.write("<table cellpadding=0 cellspacing=0 border=0");
                for (int i = 0; i < number; i++) {
                    bw.write("<tr><td><img src=" + tn.substring(1) + "_images" + tn + i + ".gif border=0></td></tr>");
                }
                bw.write("</table></body></html>");
                bw.close();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed To Create Image");
        }
    }

    private void printMenu_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
                PrinterJob pj = PrinterJob.getPrinterJob();
                PageFormat pf = pj.pageDialog(pj.defaultPage());
                table.setDoubleBuffered(false);
                pj.setPrintable(table, pf);
                table.setHeader(colorLabel);
                table.setSidebar(side);
                if (pj.printDialog()) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    try {
                        pj.print();
                    } catch (Exception PrintException) {
                    }
                    setCursor(Cursor.getDefaultCursor());
                }
                table.setDoubleBuffered(true);
            }
        };
        thread.start();
    }

    private void closeMenu_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    private void saveGrpMenu_actionPerformed(ActionEvent e) {
        DefaultListModel groupModel = new DefaultListModel();
        JList groupGenes = new JList();
        groupGenes.setModel(groupModel);
        int[] rowNumbers = table.getSelectedRows();
        Object[] o = new Object[rowNumbers.length];
        for (int i = 0; i < rowNumbers.length; i++) {
            o[i] = table.getGeneName(rowNumbers[i]);
        }
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
                newGrp.setExpFile(exp.getName());
                try {
                    File file = new File(project.getPath() + exp.getName() + File.separator + s);
                    int result = JOptionPane.YES_OPTION;
                    if (file.exists()) {
                        result = JOptionPane.showConfirmDialog(parentFrame, "The file " + file.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) file.delete();
                    }
                    if (result == JOptionPane.YES_OPTION) newGrp.writeGrpFile(project.getPath() + exp.getName() + File.separator + s);
                } catch (DidNotFinishException e2) {
                    JOptionPane.showMessageDialog(parentFrame, "Error Writing Group File");
                }
                project.addFile(exp.getName() + File.separator + s);
            }
        } else {
            JOptionPane.showMessageDialog(parentFrame, "No Genes Selected");
        }
    }

    private void decimalMenu_actionPerformed(ActionEvent e) {
        try {
            String number = "";
            number = JOptionPane.showInputDialog(this, "Please Enter Decimal Places To Show");
            if (number != null) {
                int n = Integer.parseInt(number);
                if (n >= 1) {
                    String form = "####.#";
                    for (int i = 1; i < n; i++) {
                        form += "#";
                    }
                    DecimalFormat df = new DecimalFormat(form);
                    table.setDecimalFormat(df);
                } else JOptionPane.showMessageDialog(this, "Error! You Must Enter An Integer Value Greater Than Or Equal To 1.", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e2) {
            JOptionPane.showMessageDialog(this, "Error! You Must Enter An Integer Value Greater Than Or Equal To 1.", "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double convertSlider(int val) {
        return actualmin + (val / 1000.0) * (actualmax - actualmin);
    }

    private void mSlider_stateChanged(ChangeEvent e) {
        mSlider.setMiddleRange();
        centerLabel.setText("Center: " + labelFormat.format(convertSlider(mSlider.getValueAt(2))));
        white.setText((table.getType() == table.GRAYSCALE ? "White: " : "Green: ") + labelFormat.format(convertSlider(mSlider.getValueAt(0))));
        black.setText((table.getType() == table.GRAYSCALE ? "Black: " : "Red: ") + labelFormat.format(convertSlider(mSlider.getValueAt(1))));
        minvalue = (float) convertSlider(mSlider.getValueAt(0));
        centervalue = (float) convertSlider(mSlider.getValueAt(2));
        maxvalue = (float) convertSlider(mSlider.getValueAt(1));
        table.paintTable(minvalue, centervalue, maxvalue, table.getRowHeight());
        colorLabel.setBeginEndValues((double) minvalue, (double) maxvalue);
        colorLabel.setCenter((double) centervalue);
    }

    private void rgmenu_actionPerformed(ActionEvent e) {
        graymenu.setState(false);
        rgmenu.setState(true);
        table.setType(PrintableTable.REDGREEN);
        table.paintTable(minvalue, centervalue, maxvalue, table.getRowHeight());
        centerLabel.setText("Center: " + labelFormat.format(convertSlider(mSlider.getValueAt(2))));
        white.setText("Green: " + labelFormat.format(convertSlider(mSlider.getValueAt(0))));
        white.setForeground(new Color(51, 153, 51));
        black.setText("Red: " + labelFormat.format(convertSlider(mSlider.getValueAt(1))));
        black.setForeground(new Color(255, 0, 0));
        centerLabel.setForeground(new Color(0, 0, 0));
        colorLabel.setColors(Color.green, Color.black, Color.red);
        mSlider.setFillColorAt(Color.green, 0);
        mSlider.setFillColorAt(Color.black, 1);
        mSlider.setTrackFillColor(Color.red);
    }

    private void graymenu_actionPerformed(ActionEvent e) {
        graymenu.setState(true);
        rgmenu.setState(false);
        table.setType(PrintableTable.GRAYSCALE);
        table.paintTable(minvalue, centervalue, maxvalue, table.getRowHeight());
        centerLabel.setText("Center: " + labelFormat.format(convertSlider(mSlider.getValueAt(2))));
        white.setText("White: " + labelFormat.format(convertSlider(mSlider.getValueAt(0))));
        black.setText("Black: " + labelFormat.format(convertSlider(mSlider.getValueAt(1))));
        black.setForeground(Color.black);
        white.setForeground(Color.white);
        centerLabel.setForeground(new Color(255 / 2, 255 / 2, 255 / 2));
        colorLabel.setColors(Color.white, new Color(255 / 2, 255 / 2, 255 / 2), Color.black);
        mSlider.setFillColorAt(Color.white, 0);
        mSlider.setFillColorAt(new Color(255 / 2, 255 / 2, 255 / 2), 1);
        mSlider.setTrackFillColor(Color.black);
    }
}
