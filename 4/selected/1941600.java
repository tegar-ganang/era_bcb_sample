package magictool;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Vector;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

/**
 * TableEditFrame is a frame which displays an editable table of data which a user can save
 * either as the same file or as a new file. This frame is currently used to display gene data
 * and or gene information and saves the changed information in the appropriate expression
 * file.
 */
public class TableEditFrame extends JInternalFrame {

    private JMenuBar jMenuBar = new JMenuBar();

    private JMenu filemenu = new JMenu();

    private JMenuItem save = new JMenuItem();

    private JMenuItem saveas = new JMenuItem();

    private JMenuItem print = new JMenuItem();

    private JMenuItem close = new JMenuItem();

    private JScrollPane jScrollPane1;

    private JMenu editMenu = new JMenu();

    private JMenuItem decimalMenu = new JMenuItem();

    private DefaultTableModel defaulttablemodel = new DefaultTableModel();

    /**editable and printable table displaying the data and or information about the genes*/
    protected PrintableTable jTable;

    /**expression file displayed*/
    protected ExpFile expMain;

    /**project associated with the expression file*/
    protected Project project = null;

    /**parent frame*/
    protected MainFrame parent = null;

    /**
   * Constructor creates the frame with the given expression file and project showing
   * the gene data but not the gene information
   * @param exp expression file displayed
   * @param project project associated with the expression file
   */
    public TableEditFrame(ExpFile exp, Project project) {
        this(exp, project, true, false);
    }

    /**
   * Constructor creates the frame with the given expression file and project showing
   * the gene data and or the gene information depending upon the specifications
   * @param exp expression file displayed
   * @param project project associated with the expression file
   * @param showData whether or not to display the gene data
   * @param showInfo whether or not to display the gene info
   */
    public TableEditFrame(ExpFile exp, Project project, boolean showData, boolean showInfo) {
        this.expMain = exp;
        this.project = project;
        jTable = new PrintableTable(expMain, new Vector(), jTable.NORMAL, showData, showInfo);
        jScrollPane1 = new JScrollPane(jTable);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setClosable(true);
        this.setJMenuBar(jMenuBar);
        this.setMaximizable(true);
        this.setResizable(true);
        this.setSize(500, 300);
        this.setVisible(true);
        if (expMain != null) this.setTitle("Editing " + expMain.getExpFile().getName()); else this.setTitle("Editing Temporary File");
        jTable.setDoubleBuffered(false);
        filemenu.setText("File");
        save.setText("Save");
        save.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK, false));
        save.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                save_actionPerformed(e);
            }
        });
        saveas.setText("Save as...");
        saveas.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveas_actionPerformed(e);
            }
        });
        print.setText("Print...");
        print.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_MASK, false));
        print.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                print_actionPerformed(e);
            }
        });
        close.setText("Close");
        close.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK, false));
        close.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                close_actionPerformed(e);
            }
        });
        jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jTable.setCellSelectionEnabled(true);
        editMenu.setText("Edit");
        decimalMenu.setText("Decimal Places");
        decimalMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                decimalMenu_actionPerformed(e);
            }
        });
        jMenuBar.add(filemenu);
        jMenuBar.add(editMenu);
        filemenu.add(save);
        filemenu.add(saveas);
        filemenu.addSeparator();
        filemenu.add(print);
        filemenu.addSeparator();
        filemenu.add(close);
        this.getContentPane().add(jScrollPane1);
        editMenu.add(decimalMenu);
    }

    /**
   * sets the parent frame
   * @param parent parent frame
   */
    public void setParent(MainFrame parent) {
        this.parent = parent;
    }

    private String getTableText() {
        String theText = new String();
        int row = jTable.getRowCount();
        int col = jTable.getColumnCount();
        int datalength = expMain.getColumns();
        if (jTable.showData()) {
            for (int i = 1; i < (datalength + 1) && i < col; i++) {
                theText += "\t" + jTable.getColumnName(i);
            }
            theText += "\n";
            DecimalFormat df = jTable.getDecimalFormat();
            for (int i = 0; i < row; i++) {
                int index = expMain.findGeneName(jTable.getValueAt(i, 0).toString());
                if (index == -1) {
                    for (int j = 0; j < (datalength + 1) && j < col; j++) {
                        theText += jTable.getValueAt(i, j).toString() + "\t";
                    }
                } else {
                    Gene g = expMain.getGene(index);
                    theText += jTable.getValueAt(i, 0).toString() + "\t";
                    for (int j = 1; j < (datalength + 1) && j < col; j++) {
                        try {
                            if (df.format(g.getDataPoint(j - 1)).equals(jTable.getValueAt(i, j).toString())) {
                                theText += g.getDataPoint(j - 1) + "\t";
                            } else theText += jTable.getValueAt(i, j).toString() + "\t";
                        } catch (Exception e) {
                            theText += "\t";
                        }
                    }
                }
                if (jTable.showInfo()) {
                    theText += jTable.getValueAt(i, 1 + datalength);
                } else {
                    String comment = expMain.getGene(expMain.findGeneName(jTable.getValueAt(i, 0).toString())).getComments();
                    if (comment != null) theText += comment;
                }
                theText += "\n";
            }
        } else {
            datalength = 0;
            for (int i = 0; i < expMain.getColumns(); i++) {
                theText += "\t" + expMain.getLabel(i);
            }
            theText += "\n";
            for (int i = 0; i < row; i++) {
                String ge = jTable.getValueAt(i, 0).toString();
                int pos = expMain.findGeneName(ge);
                if (pos != -1) {
                    Gene gene = expMain.getGene(pos);
                    theText += gene.getName() + "\t";
                    for (int j = 0; j < gene.getData().length; j++) {
                        theText += gene.getDataPoint(j) + "\t";
                    }
                    if (jTable.showInfo()) {
                        theText += jTable.getValueAt(i, 1 + datalength);
                    } else {
                        String comment = gene.getComments();
                        if (comment != null) theText += comment;
                    }
                    theText += "\n";
                }
            }
        }
        theText += "/**Gene Info**/" + "\n";
        if (jTable.showInfo()) {
            for (int i = 0; i < row; i++) {
                String n = jTable.getValueAt(i, 0).toString();
                String a = jTable.getValueAt(i, 1 + datalength + 1).toString();
                String c = jTable.getValueAt(i, 1 + datalength + 2).toString();
                String l = jTable.getValueAt(i, 1 + datalength + 3).toString();
                String p = jTable.getValueAt(i, 1 + datalength + 4).toString();
                String f = jTable.getValueAt(i, 1 + datalength + 5).toString();
                String co = jTable.getValueAt(i, 1 + datalength + 6).toString();
                if (n != null) theText += n + "\t" + (a != null ? a : " ") + "\t" + (c != null ? c : " ") + "\t" + (l != null ? l : " ") + "\t" + (p != null ? p : " ") + "\t" + (f != null ? f : " ") + "\t" + (co != null ? co : " ") + "\n";
            }
        } else {
            for (int i = 0; i < expMain.numGenes(); i++) {
                Gene g = expMain.getGene(i);
                String n = g.getName();
                String a = g.getAlias();
                String c = g.getChromo();
                String l = g.getLocation();
                String p = g.getProcess();
                String f = g.getFunction();
                String co = g.getComponent();
                if (n != null) theText += n + "\t" + (a != null ? a : " ") + "\t" + (c != null ? c : " ") + "\t" + (l != null ? l : " ") + "\t" + (p != null ? p : " ") + "\t" + (f != null ? f : " ") + "\t" + (co != null ? co : " ") + "\n";
            }
        }
        return theText;
    }

    private void saveTextFile(String text, String currFileName) throws IOException {
        try {
            File file = new File(currFileName);
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(text);
            out.close();
            if (parent != null) parent.expMain = new ExpFile(file);
            this.setTitle("Editing " + expMain.getExpFile().getName());
        } catch (IOException e) {
            throw new IOException();
        }
    }

    /**
     * sets the columns to fit all the information in them
     */
    public void setColumnsToFit() {
        jTable.setColumnsToFit();
    }

    private void saveTextFile(String text) {
        saveAsFile(text);
    }

    private void saveAsFile(String text) {
        String s = (String) JOptionPane.showInputDialog(this, "Enter New File Name:");
        if (s != null && s.indexOf(File.separator) == -1) {
            if (s.endsWith(".exp")) s = s.substring(0, s.lastIndexOf("."));
            String currFileName = project.getPath() + s + File.separator + s + ".exp";
            if (saveFileIsValid(currFileName)) {
                try {
                    saveTextFile(text, currFileName);
                    this.setTitle(s + ".exp");
                    if (parent != null) {
                        parent.addExpFile(currFileName);
                    }
                    if (project != null) project.addFile(s + File.separator + s + ".exp");
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Error Saving File");
                }
            }
        } else if (s.indexOf(File.separator) != -1) {
            JOptionPane.showMessageDialog(this, "Error! You Must Save File In Current Directory.\n File name must contain no file seperator characters");
            saveAsFile(text);
        }
    }

    private boolean saveFileIsValid(String outfile) {
        outfile.trim();
        File outFile = new File(outfile);
        if (outFile.isDirectory()) {
            JOptionPane.showMessageDialog(this, "The output file path is a directory.  Please add a file name.", "Directory Found", JOptionPane.OK_OPTION);
            return false;
        } else if (outFile.exists()) {
            String[] options = new String[2];
            options[0] = UIManager.getString("OptionPane.yesButtonText");
            options[1] = UIManager.getString("OptionPane.noButtonText");
            int result = JOptionPane.showOptionDialog(parent, "The file " + outFile.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (result == JOptionPane.YES_OPTION) {
                outFile.delete();
                return true;
            } else return false;
        } else {
            outFile.getParentFile().mkdirs();
            return true;
        }
    }

    private void save_actionPerformed(ActionEvent e) {
        File f;
        if ((f = expMain.getExpFile()) != null) {
            int result = JOptionPane.CANCEL_OPTION;
            String[] options = new String[2];
            options[0] = UIManager.getString("OptionPane.yesButtonText");
            options[1] = UIManager.getString("OptionPane.noButtonText");
            result = JOptionPane.showOptionDialog(parent, "Saving this file will alter the data created and invalidate all files made previously with it. Do you wish to continue?", "Save File?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
            if (result == JOptionPane.OK_OPTION) {
                try {
                    saveTextFile(getTableText(), f.getPath());
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(parent, "Error Saving File");
                }
            }
        } else saveTextFile(getTableText());
    }

    private void saveas_actionPerformed(ActionEvent e) {
        saveTextFile(getTableText());
    }

    private void print_actionPerformed(ActionEvent e) {
        PrinterJob pj = PrinterJob.getPrinterJob();
        pj.setPrintable(jTable);
        if (pj.printDialog()) {
            try {
                pj.print();
            } catch (Exception PrintException) {
            }
        }
    }

    private void close_actionPerformed(ActionEvent e) {
        dispose();
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
                    jTable.setDecimalFormat(df);
                } else JOptionPane.showMessageDialog(this, "Error! You Must Enter An Integer Value Greater Than Or Equal To 1.", "Error!", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e2) {
            JOptionPane.showMessageDialog(this, "Error! You Must Enter An Integer Value Greater Than Or Equal To 1.", "Error!", JOptionPane.ERROR_MESSAGE);
        }
    }
}
