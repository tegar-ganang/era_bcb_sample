package magictool.explore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import magictool.ExpFile;
import magictool.Gene;
import magictool.Project;
import magictool.TwoListPanel;
import magictool.VerticalLayout;

/**
 *LimitFrame is a class that enables the user to limit existing data by adding
 *or removing columns from an existing expression file
 */
public class LimitFrame extends JDialog implements ContainerListener, KeyListener {

    private JPanel jPanel2 = new JPanel();

    private JPanel jPanel3 = new JPanel();

    private JLabel jLabel1 = new JLabel();

    private JTextField fileField = new JTextField();

    private Border border1;

    private Border border2;

    private BorderLayout borderLayout1 = new BorderLayout();

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private JButton okButton = new JButton();

    private JButton cancelButton = new JButton();

    private TitledBorder titledBorder1;

    /**project associated with the expression file that is being limited*/
    protected Project project;

    /**panels of the columns to be added or removed*/
    protected TwoListPanel twoPanel;

    /**expression file that is being limited*/
    protected ExpFile exp;

    /**name of expression file*/
    protected String filename = "";

    /**whether or not the data limiting is complete*/
    protected boolean finished = false;

    /**
   * Constructs a frame with all the columns of the expression file in the added column list and with
   * a blank removed column list
   * @param exp expression file being limited
   * @param project project associated with the expression file
   * @param parent parent frame
   */
    public LimitFrame(ExpFile exp, Project project, Frame parent) {
        super(parent);
        this.exp = exp;
        this.project = project;
        Object o[] = exp.getLabelArray();
        String s[] = new String[o.length];
        for (int i = 0; i < s.length; i++) {
            s[i] = o[i].toString();
        }
        twoPanel = new TwoListPanel(s, null, "Maintained Columns", "Removed Columns", true);
        this.setTitle("Limiting " + exp.getName());
        try {
            jbInit();
            addKeyAndContainerListenerRecursively(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        border1 = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        border2 = BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED, Color.white, Color.white, new Color(148, 145, 140), new Color(103, 101, 98)), BorderFactory.createEmptyBorder(3, 3, 3, 3));
        titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2), "Select Columns");
        jLabel1.setBorder(border2);
        jLabel1.setText("New Expression File");
        String name = exp.getName();
        if (name.endsWith(".exp")) name = name.substring(0, name.lastIndexOf("."));
        name += "_limited.exp";
        fileField.setText(name);
        jPanel2.setLayout(borderLayout1);
        this.getContentPane().setLayout(verticalLayout1);
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
        twoPanel.setBorder(titledBorder1);
        this.getContentPane().add(twoPanel, null);
        this.getContentPane().add(jPanel2, null);
        jPanel2.add(jLabel1, BorderLayout.WEST);
        jPanel2.add(fileField, BorderLayout.CENTER);
        this.getContentPane().add(jPanel3, null);
        jPanel3.add(okButton, null);
        jPanel3.add(cancelButton, null);
    }

    private void okButton_actionPerformed(ActionEvent e) {
        String file = fileField.getText().trim();
        if (file.endsWith(".exp")) file = file.substring(0, file.lastIndexOf("."));
        File f = new File(project.getPath() + file + File.separator + file + ".exp");
        int deleteFiles = JOptionPane.CANCEL_OPTION;
        if (!f.exists() || (deleteFiles = JOptionPane.showConfirmDialog(null, "File Already Exists! Do You Wish To Overwrite?\nOverwriting The File Will Delete All Files Which Used The Previous File")) == JOptionPane.OK_OPTION) {
            try {
                if (deleteFiles == JOptionPane.OK_OPTION) f.getParentFile().delete();
                f.getParentFile().mkdirs();
                String[] cols = twoPanel.getFirstElements();
                boolean useColumn[] = new boolean[exp.getColumns()];
                BufferedWriter bw = new BufferedWriter(new FileWriter(f.getPath()));
                int colnum = 0;
                for (int i = 0; i < exp.getColumns(); i++) {
                    useColumn[i] = false;
                    for (int j = 0; j < cols.length; j++) {
                        if (cols[j].equals(exp.getLabel(i))) {
                            useColumn[i] = true;
                            colnum++;
                            break;
                        }
                    }
                    if (useColumn[i]) bw.write(exp.getLabel(i) + "\t");
                }
                bw.write("\n");
                for (int i = 0; i < exp.numGenes(); i++) {
                    bw.write(exp.getGeneName(i) + "\t");
                    double data[] = exp.getData(i);
                    int written = 0;
                    for (int j = 0; j < data.length; j++) {
                        if (useColumn[j]) {
                            bw.write("" + data[j] + (written == colnum - 1 ? "" : "\t"));
                            written++;
                        }
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
                finished = true;
                filename = file + File.separator + file + ".exp";
                this.dispose();
            } catch (Exception e2) {
                JOptionPane.showMessageDialog(null, "Error Writing Exp File");
            }
        }
    }

    private void cancelButton_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    /**
     * returns the new expression filename
     * @return new expression filename
     */
    public String getValue() {
        if (finished) return filename;
        return null;
    }

    private void addKeyAndContainerListenerRecursively(Component c) {
        c.removeKeyListener(this);
        c.addKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.removeContainerListener(this);
            cont.addContainerListener(this);
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                addKeyAndContainerListenerRecursively(children[i]);
            }
        }
    }

    private void removeKeyAndContainerListenerRecursively(Component c) {
        c.removeKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            cont.removeContainerListener(this);
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                removeKeyAndContainerListenerRecursively(children[i]);
            }
        }
    }

    /**
        * adds key and container listeners when a component or container is added
        * @param e container event
        */
    public void componentAdded(ContainerEvent e) {
        addKeyAndContainerListenerRecursively(e.getChild());
    }

    /**
        * removes key and container listeners when a component or container is removed
        * @param e container event
        */
    public void componentRemoved(ContainerEvent e) {
        removeKeyAndContainerListenerRecursively(e.getChild());
    }

    /**
       * Closes the frame when user press control + 'w'
       * @param e key event
       */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_MASK).getKeyCode() && e.isControlDown()) {
            this.dispose();
        }
    }

    /**
       * Not implemented in this frame
       * @param e key event
       */
    public void keyReleased(KeyEvent e) {
    }

    /**
       * Not implemented in this frame
       * @param e key event
       */
    public void keyTyped(KeyEvent e) {
    }
}
