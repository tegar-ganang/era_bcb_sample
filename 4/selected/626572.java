package magictool.clusterdisplay;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Enumeration;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import magictool.DidNotFinishException;
import magictool.ExpFile;
import magictool.GrpFile;
import magictool.PlotFrame;
import magictool.Project;
import magictool.TreeableCluster;
import magictool.cluster.HiClust;
import magictool.cluster.KMeansClust;
import magictool.cluster.QTClust;

/**
 * TreeFrame displays an expandable and collapsable tree of the elements from
 * a cluster file. Elements can be selected and plotted in another frame.
 */
public class TreeFrame extends JInternalFrame implements KeyListener {

    private JPanel contentPane = new JPanel();

    private JScrollPane jScrollPane1;

    private BorderLayout borderLayout1 = new BorderLayout();

    private BorderLayout borderLayout2 = new BorderLayout();

    private JPanel jPanel1 = new JPanel();

    private JButton plotButton = new JButton();

    private JButton saveGrpButton = new JButton();

    private JPanel jPanel2 = new JPanel();

    private JTree firsttree;

    private JButton explodeButton = new JButton();

    private JButton collapseButton = new JButton();

    /**hierarchical cluster*/
    public static final int HICLUST = 0;

    /**qt cluster*/
    public static final int QTCLUST = 1;

    /**kmeans cluster*/
    public static final int KMEANS = 2;

    /**supervised qt cluster*/
    public static final int SUPERVISED = 3;

    /**parent frame*/
    protected Frame parentFrame;

    /**top node of the cluster*/
    protected DefaultMutableTreeNode firstNode;

    /**cluster file to be displayed*/
    protected File clustFile;

    /**cluster method*/
    protected int clustMethod;

    /**expression file associated with cluster file*/
    protected ExpFile exp;

    private Project project;

    /**
     * Constructs a tree frame from the specified cluster file
     * @param clustFile cluster file to be displayed
     * @param clustMethod cluster method
     * @param exp expression file associated with cluster file
     * @param parentFrame parent frame
     * @param project open project
     */
    public TreeFrame(File clustFile, int clustMethod, ExpFile exp, Frame parentFrame, Project project) {
        this.clustFile = clustFile;
        this.clustMethod = clustMethod;
        this.parentFrame = parentFrame;
        this.project = project;
        this.exp = exp;
        try {
            jbInit();
            this.addKeyListenerRecursively(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs a tree frame from the specified cluster file
     * @param clustFile cluster file to be displayed
     * @param clustMethod cluster method
     */
    public TreeFrame(File clustFile, int clustMethod) {
        this.clustFile = clustFile;
        this.clustMethod = clustMethod;
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        TreeableCluster tc = null;
        if (clustMethod == QTCLUST) tc = new QTClust(); else if (clustMethod == KMEANS) tc = new KMeansClust(); else tc = new HiClust();
        if (tc != null) firstNode = tc.getDataInTree(clustFile);
        firsttree = new JTree(firstNode);
        jScrollPane1 = new JScrollPane(firsttree);
        contentPane.setLayout(borderLayout1);
        this.getContentPane().setLayout(borderLayout2);
        plotButton.setText("Plot Selected");
        plotButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotButton_actionPerformed(e);
            }
        });
        saveGrpButton.setText("Save As Group");
        saveGrpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveGrpButton_actionPerformed(e);
            }
        });
        this.setClosable(true);
        this.setMaximizable(true);
        this.setResizable(true);
        this.setTitle("Displaying " + clustFile.getName());
        explodeButton.setText("Explode");
        explodeButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                explodeButton_actionPerformed(e);
            }
        });
        collapseButton.setText("Collapse");
        collapseButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                collapseButton_actionPerformed(e);
            }
        });
        this.getContentPane().add(contentPane, BorderLayout.CENTER);
        contentPane.add(jScrollPane1, BorderLayout.CENTER);
        jPanel1.add(plotButton, null);
        jPanel1.add(saveGrpButton, null);
        jPanel1.add(explodeButton, null);
        jPanel1.add(collapseButton, null);
        contentPane.add(jPanel1, BorderLayout.SOUTH);
        contentPane.add(jPanel2, BorderLayout.NORTH);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        firsttree.setCellRenderer(renderer);
        firsttree.expandPath(new TreePath(firsttree.getModel().getRoot()));
        setVisible(true);
    }

    private void plotButton_actionPerformed(ActionEvent e) {
        try {
            TreePath allselected[] = firsttree.getSelectionPaths();
            for (int count = 0; count < allselected.length; count++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) allselected[count].getLastPathComponent();
                GrpFile group = formGroup(node);
                PlotFrame plotFrame = new PlotFrame(group, exp, parentFrame, project);
                this.getDesktopPane().add(plotFrame);
                plotFrame.show();
            }
        } catch (NullPointerException e1) {
            JOptionPane.showMessageDialog(this, "You Must Select A Group To Plot!", "Alert", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveGrpButton_actionPerformed(ActionEvent e) {
        DefaultListModel groupModel = new DefaultListModel();
        JList groupGenes = new JList();
        groupGenes.setModel(groupModel);
        try {
            TreePath allselected[] = firsttree.getSelectionPaths();
            for (int count = 0; count < allselected.length; count++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) allselected[count].getLastPathComponent();
                GrpFile group = formGroup(node);
                Object[] o = group.getGroup();
                if (o.length > 0) {
                    for (int i = 0; i < o.length; i++) {
                        groupModel.addElement(o[i].toString());
                    }
                }
            }
        } catch (NullPointerException e1) {
            JOptionPane.showMessageDialog(this, "You Must Select A Group To Save!", "Alert", JOptionPane.ERROR_MESSAGE);
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
    }

    private GrpFile formGroup(DefaultMutableTreeNode node) {
        GrpFile group = new GrpFile(clustFile.getName() + "_" + node.toString());
        groupLoop(node, group);
        return group;
    }

    private void groupLoop(DefaultMutableTreeNode node, GrpFile group) {
        if (node.isLeaf()) {
            group.addOne(node.toString());
        } else {
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) children.nextElement();
                groupLoop(child, group);
            }
        }
    }

    private void explodeButton_actionPerformed(ActionEvent e) {
        int r = 0;
        try {
            TreePath allselected[] = firsttree.getSelectionPaths();
            for (int count = 0; count < allselected.length; count++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) allselected[count].getLastPathComponent();
                expandAll(firsttree.getRowForPath(allselected[count]));
            }
        } catch (NullPointerException e1) {
            while (r < firsttree.getRowCount()) {
                firsttree.expandRow(r++);
            }
        }
        firsttree.clearSelection();
    }

    private void expandAll(int row) {
        if (!((DefaultMutableTreeNode) (firsttree.getPathForRow(row)).getLastPathComponent()).isLeaf()) try {
            firsttree.expandRow(row);
            firsttree.expandRow(row + 2);
            expandAll(row + 2);
            firsttree.expandRow(row + 1);
            expandAll(row + 1);
        } catch (Exception e) {
        }
    }

    /**
   * collapses all tree nodes
   * @param nodeToCollapse top level tree node
   */
    public void collapseAll(DefaultMutableTreeNode nodeToCollapse) {
        JTree subtree = new JTree(nodeToCollapse);
        TreePath tp = new TreePath((nodeToCollapse).getPath());
        for (int i = 0; i < subtree.getModel().getChildCount(nodeToCollapse); i++) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) subtree.getModel().getChild(nodeToCollapse, i);
            if (!currentNode.isLeaf()) {
                collapseAll((DefaultMutableTreeNode) subtree.getModel().getChild(nodeToCollapse, i));
            }
        }
        firsttree.collapsePath(tp);
    }

    private void collapseButton_actionPerformed(ActionEvent e) {
        int r = 1;
        try {
            TreePath allselected[] = firsttree.getSelectionPaths();
            for (int count = 0; count < allselected.length; count++) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) allselected[count].getLastPathComponent();
                collapseAll(node);
            }
        } catch (NullPointerException e1) {
            int count = firsttree.getRowCount();
            for (int i = count; i >= 1; i--) {
                firsttree.collapseRow(i);
            }
        }
        firsttree.clearSelection();
    }

    private void addKeyListenerRecursively(Component c) {
        c.removeKeyListener(this);
        c.addKeyListener(this);
        if (c instanceof Container) {
            Container cont = (Container) c;
            Component[] children = cont.getComponents();
            for (int i = 0; i < children.length; i++) {
                addKeyListenerRecursively(children[i]);
            }
        }
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
