package magictool.explore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import magictool.ExpFile;
import magictool.FileComboBox;
import magictool.Gene;
import magictool.InfoFile;
import magictool.MainFrame;
import magictool.ProgressFrame;
import magictool.Project;
import magictool.VerticalLayout;

/**
 * ImportInfoDialog is a JDialog which allows users to add gene information from
 * an info file to an expression file selected by a user.
 */
public class ImportInfoDialog extends JDialog {

    private JPanel jPanel1 = new JPanel();

    private JPanel jPanel3 = new JPanel();

    private JPanel jPanel4 = new JPanel();

    private Border border1;

    private TitledBorder titledBorder1;

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private JLabel jLabel1 = new JLabel();

    private FileComboBox filebox1;

    private JButton okButton = new JButton();

    private JButton cancelButton = new JButton();

    private BorderLayout borderLayout1 = new BorderLayout();

    private JPanel jPanel9 = new JPanel();

    private JPanel jPanel10 = new JPanel();

    private JLabel jLabel3 = new JLabel();

    private Border border2;

    private JTextField outFileField = new JTextField();

    private BorderLayout borderLayout3 = new BorderLayout();

    private VerticalLayout verticalLayout5 = new VerticalLayout();

    private BorderLayout borderLayout4 = new BorderLayout();

    private JTextField infoFileField = new JTextField();

    private BorderLayout borderLayout5 = new BorderLayout();

    private JPanel jPanel11 = new JPanel();

    private JButton selectInfoButton = new JButton();

    private String expFilePath = null;

    /**project whose expression files can be merged*/
    protected Project project;

    /**desktop where dialog is placed*/
    protected JDesktopPane desktop;

    /**whether or not the merging has been finished*/
    protected boolean finished = false;

    /**new exoression filename*/
    protected String filename = null;

    /**file path of the info file*/
    protected String infoPath = null;

    /**parent frame*/
    protected Frame parent;

    /**
   * Constructs the ImportInfo dialog and sets up the available expression files to be merged.
   * @param p project where info is to be added to an expression file
   * @param parent parent frame
   * @param desktop desktop pane
   */
    public ImportInfoDialog(Project p, Frame parent, JDesktopPane desktop) {
        super(parent);
        this.project = p;
        this.parent = parent;
        this.desktop = desktop;
        filebox1 = new FileComboBox(p, Project.EXP);
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        border1 = BorderFactory.createLineBorder(new Color(153, 153, 153), 2);
        titledBorder1 = new TitledBorder(border1, "Select Files To Merge");
        border2 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.white, Color.white, new Color(148, 145, 140), new Color(103, 101, 98)), BorderFactory.createEmptyBorder(3, 3, 3, 3));
        jPanel1.setBorder(titledBorder1);
        jPanel1.setLayout(verticalLayout1);
        jLabel1.setText("Select Expression File: ");
        jPanel4.setLayout(borderLayout1);
        okButton.setEnabled(false);
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                okButton_actionPerformed(e);
            }
        });
        rootPane.setDefaultButton(okButton);
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                cancelButton_actionPerformed(e);
            }
        });
        filebox1.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                filebox1_itemStateChanged(e);
            }
        });
        jLabel3.setBorder(border2);
        jLabel3.setText("New Expression File");
        jPanel10.setBorder(BorderFactory.createEtchedBorder());
        jPanel10.setLayout(borderLayout3);
        this.getContentPane().setLayout(borderLayout4);
        jPanel9.setLayout(verticalLayout5);
        titledBorder1.setTitle("Select Files");
        jPanel11.setLayout(borderLayout5);
        jPanel11.setBorder(BorderFactory.createEtchedBorder());
        selectInfoButton.setText("Select Info File...");
        selectInfoButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectInfoButton_actionPerformed(e);
            }
        });
        this.getContentPane().add(jPanel1, BorderLayout.NORTH);
        jPanel11.add(infoFileField, BorderLayout.CENTER);
        jPanel11.add(selectInfoButton, BorderLayout.WEST);
        jPanel1.add(jPanel4, null);
        jPanel4.add(jLabel1, BorderLayout.WEST);
        jPanel4.add(filebox1, BorderLayout.CENTER);
        jPanel1.add(jPanel11, null);
        this.getContentPane().add(jPanel9, BorderLayout.CENTER);
        jPanel9.add(jPanel10, null);
        jPanel10.add(jLabel3, BorderLayout.WEST);
        jPanel10.add(outFileField, BorderLayout.CENTER);
        this.getContentPane().add(jPanel3, BorderLayout.SOUTH);
        jPanel3.add(okButton, null);
        jPanel3.add(cancelButton, null);
        this.setSize(425, 200);
    }

    private void checkForOK() {
        if (filebox1.getSelectedIndex() != 0 && !infoFileField.getText().equals("")) {
            okButton.setEnabled(true);
        } else okButton.setEnabled(false);
    }

    private void filebox1_itemStateChanged(ItemEvent e) {
        outFileField.setText(filebox1.getSimpleName() + "_i.exp");
        checkForOK();
    }

    private void okButton_actionPerformed(ActionEvent e) {
        checkForOK();
        if (okButton.isEnabled()) {
            final ProgressFrame progress = new ProgressFrame("Loading Info File");
            desktop.add(progress);
            int genesFound = 0;
            ExpFile exp1 = new ExpFile(new File(filebox1.getFilePath()));
            expFilePath = filebox1.getFilePath();
            progress.setMaximum(exp1.numGenes());
            String file = outFileField.getText().trim();
            if (file.endsWith(".exp")) file = file.substring(0, file.lastIndexOf("."));
            File f = new File(project.getPath() + file + File.separator + file + ".exp");
            int deleteFiles = JOptionPane.CANCEL_OPTION;
            if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(parent, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
                try {
                    if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    f.getParentFile().mkdirs();
                    progress.show();
                    progress.setTitle("Loading " + infoFileField.getText().trim());
                    InfoFile infoFile = new InfoFile(new File(infoFileField.getText().trim()));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath()));
                    for (int i = 0; i < exp1.getColumns(); i++) {
                        bw.write(exp1.getLabel(i) + "\t");
                    }
                    bw.write("\n");
                    for (int i = 0; i < exp1.numGenes(); i++) {
                        bw.write(exp1.getGeneName(i) + "\t");
                        double data[] = exp1.getData(i);
                        for (int j = 0; j < data.length; j++) {
                            bw.write("" + data[j] + "\t");
                        }
                        int pos = exp1.findGeneName(exp1.getGeneName(i));
                        if (pos != -1) {
                            String comments = exp1.getGene(pos).getComments();
                            if (comments == null) comments = "";
                            bw.write("" + comments);
                        }
                        bw.write("\n");
                    }
                    bw.write("/**Gene Info**/" + "\n");
                    for (int i = 0; i < exp1.numGenes(); i++) {
                        Gene g = exp1.getGene(i);
                        String n = g.getName();
                        String nTemp = null;
                        if (n.indexOf("/") == -1) nTemp = n; else nTemp = n.substring(0, n.indexOf("/"));
                        int infoNumber = infoFile.findGeneName(nTemp);
                        int place;
                        if ((place = nTemp.toLowerCase().indexOf("_rep")) != -1) infoNumber = infoFile.findGeneName(nTemp.substring(0, place));
                        String a = null;
                        String c = null;
                        String l = null;
                        String p = null;
                        String fl = null;
                        String co = null;
                        if (infoNumber != -1) {
                            genesFound++;
                            a = infoFile.getGene(infoNumber).getAlias();
                            c = infoFile.getGene(infoNumber).getChromo();
                            l = infoFile.getGene(infoNumber).getLocation();
                            p = infoFile.getGene(infoNumber).getProcess();
                            fl = infoFile.getGene(infoNumber).getFunction();
                            co = infoFile.getGene(infoNumber).getComponent();
                        }
                        if (n != null) bw.write(n + "\t" + (a != null ? a : " ") + "\t" + (c != null ? c : " ") + "\t" + (l != null ? l : " ") + "\t" + (p != null ? p : " ") + "\t" + (fl != null ? fl : " ") + "\t" + (co != null ? co : " ") + "\n");
                        progress.addValue(1);
                    }
                    bw.close();
                    finished = true;
                    progress.dispose();
                    filename = file + File.separator + file + ".exp";
                    JOptionPane.showMessageDialog(parent, "Found info for " + genesFound + " of " + exp1.numGenes() + " genes.");
                    dispose();
                } catch (Exception e2) {
                    JOptionPane.showMessageDialog(parent, "Error Writing Exp File - " + e2);
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    private void selectInfoButton_actionPerformed(ActionEvent e) {
        MainFrame.fileLoader.setFileFilter(MainFrame.fileLoader.infoFilter);
        MainFrame.fileLoader.setDialogTitle("Load Gene Info File...");
        MainFrame.fileLoader.setApproveButtonText("Load");
        File f = new File(project.getPath() + "info" + File.separator);
        if (!f.exists()) f.mkdirs();
        MainFrame.fileLoader.setCurrentDirectory(f);
        MainFrame.fileLoader.setSelectedFile(null);
        int result = MainFrame.fileLoader.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = MainFrame.fileLoader.getSelectedFile();
            infoPath = file.getAbsolutePath();
            infoFileField.setText(infoPath);
        }
        checkForOK();
    }

    private void cancelButton_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    /**
   * returns the new expression file name
   * @return new expression file name
   */
    public String getValue() {
        if (finished) return filename;
        return null;
    }

    /**
   * returns the file path of the expression file which info is being added to
   * @return file path of the expression file which info is being added to
   */
    public String getExpFilePath() {
        if (finished) return expFilePath;
        return null;
    }
}
