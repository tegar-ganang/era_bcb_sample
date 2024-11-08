package magictool.explore;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import magictool.DidNotFinishException;
import magictool.ExpFile;
import magictool.GrpFile;
import magictool.Project;
import magictool.VerticalLayout;

/**
 *The GroupEditFrame is a class that enables the user to add and remove genes from
 *an existing group file.
 */
public class GroupEditFrame extends JInternalFrame {

    private JPanel jPanel1 = new JPanel();

    private JScrollPane expScroll;

    private JPanel jPanel2 = new JPanel();

    private JButton addButton = new JButton();

    private JButton removeButton = new JButton();

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private JScrollPane groupScroll;

    private GridLayout gridLayout1 = new GridLayout();

    private JPanel jPanel3 = new JPanel();

    private JLabel jLabel1 = new JLabel();

    private JLabel jLabel2 = new JLabel();

    private JLabel jLabel3 = new JLabel();

    private GridLayout gridLayout2 = new GridLayout();

    private JMenuBar menubar = new JMenuBar();

    private JMenu jMenu1 = new JMenu();

    private JMenuItem saveMenu = new JMenuItem();

    private JMenuItem saveAsMenu = new JMenuItem();

    private JMenuItem closeMenu = new JMenuItem();

    /**list of genes that are in the group file*/
    protected JList groupGenes = new JList();

    /**list of the genes that aren't in the group file*/
    protected JList expGenes = new JList();

    /**group file that is being edited*/
    protected GrpFile group;

    /**expression file that the group file was created from*/
    protected ExpFile exp;

    /**model holds the list of genes that are in the group file*/
    protected DefaultListModel groupModel;

    /**model holds the list of the genes that aren't in the group file but are in the expression file*/
    protected DefaultListModel expModel;

    /**project associated with the group file*/
    protected Project p;

    /**parent frame*/
    protected ExploreFrame parent = null;

    /**parent frame*/
    protected Frame parentFrame;

    /**
   * Add/Remove genes from an existing group file
   * @param group the group file that is being edited
   * @param exp the expression file that has been narrowed into a group file
   * @param p project associated with the group file
   * @param parentFrame parent frame
   */
    public GroupEditFrame(GrpFile group, ExpFile exp, Project p, Frame parentFrame) {
        this.exp = exp;
        this.group = group;
        if (group.getNumGenes() == 0) {
            for (int i = 0; i < exp.numGenes(); i++) {
                group.addOne(exp.getGeneName(i));
            }
        }
        this.p = p;
        this.parentFrame = parentFrame;
        groupModel = new DefaultListModel();
        expModel = new DefaultListModel();
        expGenes.setModel(expModel);
        expGenes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        groupGenes.setModel(groupModel);
        groupGenes.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Object[] o = group.getGroup();
        for (int i = 0; i < o.length; i++) {
            groupModel.addElement(o[i].toString());
        }
        for (int i = 0; i < exp.numGenes(); i++) {
            String s = exp.getGeneName(i);
            if (!groupModel.contains(s)) expModel.addElement(s);
        }
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        jPanel1.setLayout(gridLayout1);
        jPanel2.setLayout(verticalLayout1);
        addButton.setText("<<Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                addButton_actionPerformed(e);
            }
        });
        removeButton.setText("Remove>>");
        removeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                removeButton_actionPerformed(e);
            }
        });
        verticalLayout1.setAlignment(VerticalLayout.MIDDLE);
        this.setClosable(true);
        this.setJMenuBar(menubar);
        this.setMaximizable(true);
        this.setResizable(true);
        this.setTitle(group.getTitle());
        groupScroll = new JScrollPane(groupGenes);
        expScroll = new JScrollPane(expGenes);
        jLabel1.setText("Group Genes");
        jLabel3.setText("Other Genes");
        jPanel3.setLayout(gridLayout2);
        jMenu1.setText("File");
        saveMenu.setText("Save");
        saveMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_S, java.awt.event.KeyEvent.CTRL_MASK));
        saveMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveMenu_actionPerformed(e);
            }
        });
        saveAsMenu.setText("Save As...");
        saveAsMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveAsMenu_actionPerformed(e);
            }
        });
        closeMenu.setText("Close");
        closeMenu.setAccelerator(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_W, java.awt.event.KeyEvent.CTRL_MASK));
        closeMenu.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                closeMenu_actionPerformed(e);
            }
        });
        this.getContentPane().add(jPanel1, BorderLayout.CENTER);
        jPanel2.add(addButton, null);
        jPanel2.add(removeButton, null);
        jPanel1.add(groupScroll, null);
        jPanel1.add(jPanel2, null);
        jPanel1.add(expScroll, null);
        this.getContentPane().add(jPanel3, BorderLayout.NORTH);
        jPanel3.add(jLabel1, null);
        jPanel3.add(jLabel2, null);
        jPanel3.add(jLabel3, null);
        menubar.add(jMenu1);
        jMenu1.add(saveMenu);
        jMenu1.add(saveAsMenu);
        jMenu1.addSeparator();
        jMenu1.add(closeMenu);
        if (group.getTitle() == null || group.getTitle().equals("Temporary Group") || group.getTitle().trim().equals("")) saveMenu.setEnabled(false);
    }

    /**
   * sets the parent explore frame so that the list of available group files may be updated
   * @param f parent explore frame
   */
    public void setParentFrame(ExploreFrame f) {
        parent = f;
    }

    private void addButton_actionPerformed(ActionEvent e) {
        int pos[] = expGenes.getSelectedIndices();
        for (int i = 0; i < pos.length; i++) {
            groupModel.addElement(expModel.elementAt(pos[i] - i));
            expModel.removeElementAt(pos[i] - i);
        }
    }

    private void removeButton_actionPerformed(ActionEvent e) {
        int pos[] = groupGenes.getSelectedIndices();
        for (int i = 0; i < pos.length; i++) {
            expModel.addElement(groupModel.elementAt(pos[i] - i));
            groupModel.removeElementAt(pos[i] - i);
        }
    }

    private void saveMenu_actionPerformed(ActionEvent e) {
        String s = group.getTitle();
        if (s == null || s.trim().equals("")) {
            saveAsMenu_actionPerformed(e);
        } else {
            group = new GrpFile(s);
            for (int i = 0; i < groupModel.size(); i++) {
                group.addOne(groupModel.elementAt(i));
            }
            if (!s.endsWith(".grp")) s += ".grp";
            group.setExpFile(exp.getName());
            try {
                File file = new File(p.getPath() + exp.getName() + File.separator + s);
                int result = JOptionPane.YES_OPTION;
                if (file.exists()) {
                    result = JOptionPane.showConfirmDialog(parentFrame, "The file " + file.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) file.delete();
                }
                if (result == JOptionPane.YES_OPTION) group.writeGrpFile(p.getPath() + exp.getName() + File.separator + s);
            } catch (DidNotFinishException e2) {
                JOptionPane.showMessageDialog(this, "Error Writing Group File");
            }
            updateParentFrame();
        }
    }

    private void closeMenu_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    private void saveAsMenu_actionPerformed(ActionEvent e) {
        String s = JOptionPane.showInputDialog(this, "Enter The Group Name:");
        if (s != null) {
            group = new GrpFile(s);
            for (int i = 0; i < groupModel.size(); i++) {
                group.addOne(groupModel.elementAt(i));
            }
            if (!s.endsWith(".grp")) s += ".grp";
            group.setExpFile(exp.getName());
            try {
                File file = new File(p.getPath() + exp.getName() + File.separator + s);
                int result = JOptionPane.YES_OPTION;
                if (file.exists()) {
                    result = JOptionPane.showConfirmDialog(parentFrame, "The file " + file.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) file.delete();
                }
                if (result == JOptionPane.YES_OPTION) {
                    group.writeGrpFile(p.getPath() + exp.getName() + File.separator + s);
                    p.addFile(exp.getName() + File.separator + s);
                    saveMenu.setEnabled(true);
                    this.setTitle(group.getTitle());
                    updateParentFrame();
                }
            } catch (DidNotFinishException e2) {
                JOptionPane.showMessageDialog(this, "Error Writing Group File");
            }
        }
    }

    private void updateParentFrame() {
        parent.groupBox.reload();
        parent.groupBox.setSelectedIndex(parent.groupBox.getItemCount() - 1);
    }
}
