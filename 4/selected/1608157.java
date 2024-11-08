package magictool.explore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import magictool.ExpFile;
import magictool.FileComboBox;
import magictool.Gene;
import magictool.Project;
import magictool.VerticalLayout;

/**
 * FileMerger is a JDialog which allows the user to select two expression files
 * and merge them by displaying all the columns of any common genes together.
 * The files are merged into a new expression file specified by the user.
 */
public class FileMerger extends JDialog {

    private JPanel jPanel1 = new JPanel();

    private JPanel jPanel3 = new JPanel();

    private JPanel jPanel2 = new JPanel();

    private JPanel jPanel4 = new JPanel();

    private Border border1;

    private TitledBorder titledBorder1;

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private JLabel jLabel1 = new JLabel();

    private FileComboBox filebox1;

    private JLabel jLabel2 = new JLabel();

    private FileComboBox filebox2;

    private JButton okButton = new JButton();

    private JButton cancelButton = new JButton();

    private JPanel jPanel5 = new JPanel();

    private JPanel jPanel6 = new JPanel();

    private JPanel jPanel7 = new JPanel();

    private VerticalLayout verticalLayout2 = new VerticalLayout();

    private JLabel nicklabel1 = new JLabel();

    private JLabel nicklabel2 = new JLabel();

    private JTextField nick1 = new JTextField();

    private JTextField nick2 = new JTextField();

    private FlowLayout flowLayout3 = new FlowLayout();

    private BorderLayout borderLayout1 = new BorderLayout();

    private BorderLayout borderLayout2 = new BorderLayout();

    private JPanel jPanel8 = new JPanel();

    private JLabel jLabel5 = new JLabel();

    private JLabel jLabel6 = new JLabel();

    private VerticalLayout verticalLayout3 = new VerticalLayout();

    private JPanel jPanel9 = new JPanel();

    private JPanel jPanel10 = new JPanel();

    private JLabel jLabel3 = new JLabel();

    private Border border2;

    private JTextField outFileField = new JTextField();

    private BorderLayout borderLayout3 = new BorderLayout();

    private VerticalLayout verticalLayout5 = new VerticalLayout();

    private BorderLayout borderLayout4 = new BorderLayout();

    private JPanel functionPanel = new JPanel();

    private JLabel functionLabel = new JLabel();

    private JComboBox functionCombo = new JComboBox();

    private Object removed1 = null, removed2 = null;

    private String mainExpFile = null;

    /**project whose expression files can be merged*/
    protected Project project;

    /**whether or not the merging has been finished*/
    protected boolean finished = false;

    /**new exoression filename*/
    protected String filename = null;

    /**
   * Constructs the FileMerger dialog and sets up the available expression files to be merged.
   * @param p project whose expression files can be merged
   * @param parent parent frame
   */
    public FileMerger(Project p, Frame parent) {
        super(parent);
        this.project = p;
        filebox1 = new FileComboBox(p, Project.EXP);
        filebox2 = new FileComboBox(p, Project.EXP);
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
        jLabel1.setText("Select File #1");
        jLabel2.setText("Select File #2");
        jPanel4.setLayout(borderLayout1);
        jPanel2.setLayout(borderLayout2);
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
        jPanel5.setBorder(BorderFactory.createEtchedBorder());
        jPanel5.setLayout(verticalLayout2);
        nicklabel1.setEnabled(false);
        nicklabel1.setText("File #1 Nickname");
        nicklabel2.setEnabled(false);
        nicklabel2.setText("File #2 Nickname");
        nick1.setEnabled(false);
        nick1.setColumns(5);
        nick1.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                nick1_keyTyped(e);
            }
        });
        nick2.setEnabled(false);
        nick2.setColumns(5);
        nick2.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyTyped(KeyEvent e) {
                nick2_keyTyped(e);
            }
        });
        jPanel7.setLayout(flowLayout3);
        jLabel5.setFont(new java.awt.Font("Dialog", 0, 9));
        jLabel5.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel5.setText("**The Nickname is composed of 5 or less characters used to identify " + "which");
        jLabel6.setFont(new java.awt.Font("Dialog", 0, 10));
        jLabel6.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel6.setText("columns came from which file");
        jPanel8.setLayout(verticalLayout3);
        filebox1.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                filebox1_itemStateChanged(e);
            }
        });
        filebox2.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                filebox2_itemStateChanged(e);
            }
        });
        jLabel3.setBorder(border2);
        jLabel3.setText("New Expression File");
        jPanel10.setBorder(BorderFactory.createEtchedBorder());
        jPanel10.setLayout(borderLayout3);
        this.getContentPane().setLayout(borderLayout4);
        jPanel9.setLayout(verticalLayout5);
        functionLabel.setText("Take Gene Information and Group Files From:");
        this.getContentPane().add(jPanel1, BorderLayout.NORTH);
        jPanel1.add(jPanel4, null);
        jPanel4.add(jLabel1, BorderLayout.WEST);
        jPanel4.add(filebox1, BorderLayout.CENTER);
        jPanel1.add(jPanel2, null);
        jPanel2.add(jLabel2, BorderLayout.WEST);
        jPanel2.add(filebox2, BorderLayout.CENTER);
        jPanel1.add(jPanel5, null);
        jPanel5.add(jPanel6, null);
        jPanel6.add(nicklabel1, null);
        jPanel6.add(nick1, null);
        jPanel5.add(jPanel7, null);
        jPanel7.add(nicklabel2, null);
        jPanel7.add(nick2, null);
        jPanel5.add(jPanel8, null);
        jPanel8.add(functionPanel, null);
        jPanel8.add(jLabel5, null);
        jPanel8.add(jLabel6, null);
        this.getContentPane().add(jPanel9, BorderLayout.CENTER);
        jPanel9.add(jPanel10, null);
        jPanel10.add(jLabel3, BorderLayout.WEST);
        jPanel10.add(outFileField, BorderLayout.CENTER);
        this.getContentPane().add(jPanel3, BorderLayout.SOUTH);
        jPanel3.add(okButton, null);
        jPanel3.add(cancelButton, null);
        functionPanel.add(functionLabel, null);
        functionPanel.add(functionCombo, null);
        functionCombo.addItem("File #1");
        functionCombo.addItem("File #2");
    }

    private void checkForOK() {
        if (filebox1.getSelectedIndex() != 0 && filebox2.getSelectedIndex() != 0 && !nick1.getText().trim().equals("") && !nick2.getText().trim().equals("")) {
            okButton.setEnabled(true);
            outFileField.setText(nick1.getText() + "_" + nick2.getText() + "_merged.exp");
        } else okButton.setEnabled(false);
    }

    private void filebox1_itemStateChanged(ItemEvent e) {
        if (filebox1.getSelectedIndex() == 0) {
            nick1.setEnabled(false);
            nicklabel1.setEnabled(false);
        } else {
            nick1.setEnabled(true);
            nicklabel1.setEnabled(true);
            if (removed2 != null) filebox2.addItem(removed2);
            filebox2.removeItem(removed2 = filebox1.getSelectedItem());
            String s = removed2.toString();
            s = s.substring(s.lastIndexOf(File.separator) + 1, s.lastIndexOf("."));
            nick1.setText(s.substring(0, (s.length() > 5 ? 5 : s.length())));
            checkForOK();
        }
    }

    private void filebox2_itemStateChanged(ItemEvent e) {
        if (filebox2.getSelectedIndex() == 0) {
            nick2.setEnabled(false);
            nicklabel2.setEnabled(false);
        } else {
            nick2.setEnabled(true);
            nicklabel2.setEnabled(true);
            Object o = filebox1.getSelectedItem();
            if (removed1 != null) filebox1.addItem(removed1);
            filebox1.removeItem(removed1 = filebox2.getSelectedItem());
            String s = removed1.toString();
            s = s.substring(s.lastIndexOf(File.separator) + 1, s.lastIndexOf("."));
            nick2.setText(s.substring(0, (s.length() > 5 ? 5 : s.length())));
            checkForOK();
        }
    }

    private void nick1_keyTyped(KeyEvent e) {
        if (nick1.getText().length() > 5) {
            nick1.setText(nick1.getText().substring(0, 5));
        }
        checkForOK();
    }

    private void nick2_keyTyped(KeyEvent e) {
        if (nick2.getText().length() > 5) {
            nick2.setText(nick2.getText().substring(0, 5));
        }
        checkForOK();
    }

    private void okButton_actionPerformed(ActionEvent e) {
        final Component parent = this;
        Thread thread = new Thread() {

            public void run() {
                ExpFile exp1 = new ExpFile(new File(filebox1.getFilePath()));
                ExpFile exp2 = new ExpFile(new File(filebox2.getFilePath()));
                mainExpFile = (functionCombo.getSelectedIndex() == 0 ? filebox1.getFilePath() : filebox2.getFilePath());
                String file = outFileField.getText().trim();
                if (file.endsWith(".exp")) file = file.substring(0, file.lastIndexOf("."));
                File f = new File(project.getPath() + file + File.separator + file + ".exp");
                int deleteFiles = JOptionPane.CANCEL_OPTION;
                if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(parent, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
                    try {
                        if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        f.getParentFile().mkdirs();
                        BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath()));
                        for (int i = 0; i < exp1.getColumns(); i++) {
                            bw.write(exp1.getLabel(i) + "_" + nick1.getText() + "\t");
                        }
                        for (int i = 0; i < exp2.getColumns(); i++) {
                            bw.write(exp2.getLabel(i) + "_" + nick2.getText() + (i == exp2.getColumns() - 1 ? "" : "\t"));
                        }
                        bw.write("\n");
                        for (int i = 0; i < exp1.numGenes(); i++) {
                            int pos;
                            if ((pos = exp2.findGeneName(exp1.getGeneName(i))) != -1) {
                                bw.write(exp1.getGeneName(i) + "\t");
                                double data[] = exp1.getData(i);
                                for (int j = 0; j < data.length; j++) {
                                    bw.write("" + data[j] + "\t");
                                }
                                double data2[] = exp2.getData(pos);
                                for (int j = 0; j < data2.length; j++) {
                                    bw.write("" + data2[j] + (j == data2.length - 1 ? "" : "\t"));
                                }
                                String comments = "", comments1, comments2;
                                comments1 = exp1.getGene(i).getComments();
                                comments2 = exp2.getGene(pos).getComments();
                                if (comments1 != null) comments += comments1 + " ";
                                if (comments2 != null) comments += comments2;
                                bw.write("\t" + comments);
                                bw.write("\n");
                            }
                        }
                        bw.write("/**Gene Info**/" + "\n");
                        ExpFile tempexp = exp1;
                        if (functionCombo.getSelectedIndex() == 1) tempexp = exp2;
                        for (int i = 0; i < tempexp.numGenes(); i++) {
                            Gene g = tempexp.getGene(i);
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
                        JOptionPane.showMessageDialog(parent, "Error Writing Exp File");
                        e2.printStackTrace();
                    }
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        thread.start();
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
   * returns the filepath of the main expression file
   * @return filepath of the main expression file
   */
    public String getMainExpFilePath() {
        if (finished) return mainExpFile;
        return null;
    }
}
