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
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JDialog;
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
import magictool.Project;
import magictool.VerticalLayout;

/**
 * AverageDataDialog is a JDialog which allows to select an expression file to average
 * replicate gene data.
 */
public class AverageDataDialog extends JDialog {

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

    private String expFilePath = null;

    /**project whose expression files can be merged*/
    protected Project project;

    /**desktop where dialog is displayed*/
    protected JDesktopPane desktop;

    /**whether or not the merging has been finished*/
    protected boolean finished = false;

    /**new expression filename*/
    protected String filename = null;

    /**file path for the info file*/
    protected String infoPath = null;

    /**parent frame*/
    protected Frame parent;

    /**
   * Constructs the FileMerger dialog and sets up the available expression files to be merged.
   * @param p project whose expression files can be merged
   * @param parent parent frame
   * @param desktop desktop
   */
    public AverageDataDialog(Project p, Frame parent, JDesktopPane desktop) {
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
        this.getContentPane().add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(jPanel4, null);
        jPanel4.add(jLabel1, BorderLayout.WEST);
        jPanel4.add(filebox1, BorderLayout.CENTER);
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
        if (filebox1.getSelectedIndex() != 0) okButton.setEnabled(true); else okButton.setEnabled(false);
    }

    private void filebox1_itemStateChanged(ItemEvent e) {
        outFileField.setText(filebox1.getSimpleName() + "_avg.exp");
        checkForOK();
    }

    private void okButton_actionPerformed(ActionEvent e) {
        checkForOK();
        if (okButton.isEnabled()) {
            expFilePath = filebox1.getFilePath();
            ExpFile exp1 = new ExpFile(new File(filebox1.getFilePath()));
            String file = outFileField.getText().trim();
            if (file.toLowerCase().endsWith(".exp")) file = file.substring(0, file.lastIndexOf("."));
            File f = new File(project.getPath() + file + File.separator + file + ".exp");
            int deleteFiles = JOptionPane.CANCEL_OPTION;
            if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(parent, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
                try {
                    if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    f.getParentFile().mkdirs();
                    BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath()));
                    for (int i = 0; i < exp1.getColumns(); i++) {
                        bw.write(exp1.getLabel(i) + "\t");
                    }
                    bw.write("\n");
                    Vector allGenes = new Vector(exp1.numGenes());
                    for (int i = 0; i < exp1.numGenes(); i++) allGenes.add(exp1.getGene(i));
                    for (int i = 0; i < exp1.numGenes(); i++) {
                        String name = exp1.getGeneName(i);
                        if (allGenes.contains(exp1.getGene(exp1.findGeneName(name)))) {
                            if (name.toLowerCase().indexOf("_rep") != -1) {
                                Vector allReps = new Vector();
                                String comments = "";
                                for (int j = 0; j < allGenes.size(); j++) {
                                    if (((Gene) allGenes.get(j)).getName().startsWith(name.substring(0, name.toLowerCase().indexOf("_rep")))) {
                                        Gene g = (Gene) allGenes.get(j);
                                        allReps.add(g);
                                        if (g.getComments() != null && !g.getComments().trim().equals("")) comments += g.getComments() + " ";
                                    }
                                }
                                double avgData[] = ((Gene) allReps.get(0)).getData();
                                for (int j = 1; j < allReps.size(); j++) {
                                    double data[] = ((Gene) allReps.get(j)).getData();
                                    for (int k = 0; k < avgData.length; k++) avgData[k] += data[k];
                                }
                                for (int k = 0; k < avgData.length; k++) avgData[k] = avgData[k] / (double) allReps.size();
                                for (int j = 0; j < allReps.size(); j++) allGenes.remove(allReps.get(j));
                                String newname = name.substring(0, name.toLowerCase().indexOf("_rep"));
                                int geneNum = exp1.findGeneName(name);
                                allGenes.add(new Gene(newname, avgData, exp1.getGene(geneNum).getChromo(), exp1.getGene(geneNum).getLocation(), exp1.getGene(geneNum).getAlias(), exp1.getGene(geneNum).getProcess(), exp1.getGene(geneNum).getFunction(), exp1.getGene(geneNum).getComponent()));
                                bw.write(newname + "\t");
                                for (int j = 0; j < avgData.length; j++) bw.write("" + avgData[j] + "\t");
                                if (comments == null) comments = "";
                                bw.write(comments + "\n");
                            } else {
                                bw.write(name + "\t");
                                double data[] = exp1.getData(i);
                                for (int j = 0; j < data.length; j++) {
                                    bw.write("" + data[j] + "\t");
                                }
                                String comments = exp1.getGene(exp1.findGeneName(name)).getComments();
                                if (comments == null) comments = "";
                                bw.write(comments + "\n");
                            }
                        }
                    }
                    bw.write("/**Gene Info**/" + "\n");
                    for (int i = 0; i < allGenes.size(); i++) {
                        Gene g = (Gene) allGenes.get(i);
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
                    finished = true;
                    filename = file + File.separator + file + ".exp";
                    dispose();
                } catch (Exception e2) {
                    e2.printStackTrace();
                    JOptionPane.showMessageDialog(parent, "Error Writing Exp File");
                }
                setCursor(Cursor.getDefaultCursor());
            }
        }
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
