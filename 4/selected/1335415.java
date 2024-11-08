package magictool.explore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import magictool.DidNotFinishException;
import magictool.ExpFile;
import magictool.FileComboBox;
import magictool.GrpFile;
import magictool.PlotFrame;
import magictool.Project;
import magictool.TableFrame;
import magictool.VerticalLayout;
import magictool.groupdisplay.CircleDisplayFrame;
import magictool.groupdisplay.ColumnChooser;

/**
 * ExploreFrame displays options to load existing gene groups, or form new gene groups
 * based on specified criteria.  With the groups the ExploreFrame can create a grayscale
 * table to display the gene data.
 */
public class ExploreFrame extends JInternalFrame implements KeyListener {

    private JPanel selectPane = new JPanel();

    private JPanel currentPane = new JPanel();

    private JPanel newGroupPanel = new JPanel();

    private JPanel selectedPanel = new JPanel();

    private JPanel plotPanel = new JPanel();

    private TitledBorder titledBorder1;

    private JButton useCritButton = new JButton();

    private JButton editGrpButton = new JButton();

    private JButton plotGroupButton = new JButton();

    private JLabel selLabel = new JLabel();

    private JLabel grpLabel = new JLabel();

    private JButton tableButton = new JButton();

    private ExpFile expMain;

    private GrpFile grpMain = null;

    ;

    private TitledBorder titledBorder2;

    private TitledBorder titledBorder3;

    private GridLayout gridLayout1 = new GridLayout(1, 2);

    private VerticalLayout verticalLayout1 = new VerticalLayout();

    private BorderLayout borderLayout1 = new BorderLayout();

    private VerticalLayout verticalLayout2 = new VerticalLayout();

    private JLabel grpLabel2 = new JLabel();

    private VerticalLayout verticalLayout3 = new VerticalLayout();

    private JButton circleDisplayButton = new JButton();

    private JPanel jPanel1 = new JPanel();

    private JButton saveGrpButton = new JButton();

    /**JComboBox holding list of group files*/
    protected FileComboBox groupBox;

    private BorderLayout borderLayout2 = new BorderLayout();

    private Border border1;

    private TitledBorder titledBorder4;

    private Project project;

    private JButton scatterButton = new JButton();

    private VerticalLayout verticalLayout4 = new VerticalLayout();

    private CritDialog.Criteria criteria = null;

    /**parent frame*/
    protected Frame parentFrame;

    /**
     * Initializes the exploreFrame or throws an exception
     * @param expMain Loads expression file
     * @param p project associated with expression file that is being explored
     * @param parentFrame parent frame
     */
    public ExploreFrame(ExpFile expMain, Project p, Frame parentFrame) {
        this.project = p;
        this.expMain = expMain;
        this.parentFrame = parentFrame;
        groupBox = new FileComboBox(project, Project.GRP, expMain.getName());
        try {
            jbInit();
            addKeyListenerRecursively(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the frame
     * @exception Exception
     */
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2), "Form New Group");
        titledBorder2 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2), "Group Info");
        titledBorder3 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2), "Group Options");
        border1 = BorderFactory.createEmptyBorder();
        titledBorder4 = new TitledBorder(BorderFactory.createLineBorder(new Color(153, 153, 153), 2), "Select Existing Group");
        this.getContentPane().setLayout(borderLayout1);
        this.setClosable(true);
        this.getContentPane().setBackground(new Color(204, 204, 204));
        this.setTitle("Exploring " + expMain.getName());
        selectPane.setLayout(verticalLayout1);
        useCritButton.setText("Find Genes Matching Criteria...");
        useCritButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                useCritButton_actionPerformed(e);
            }
        });
        newGroupPanel.setBorder(titledBorder1);
        newGroupPanel.setLayout(verticalLayout2);
        currentPane.setLayout(gridLayout1);
        selLabel.setHorizontalAlignment(SwingConstants.CENTER);
        selLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        selLabel.setText("Selected Group");
        grpLabel.setFont(new java.awt.Font("Dialog", 1, 12));
        grpLabel.setHorizontalAlignment(SwingConstants.CENTER);
        grpLabel.setText("Entire Expression File");
        editGrpButton.setText("View / Edit File");
        editGrpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editGrpButton_actionPerformed(e);
            }
        });
        plotGroupButton.setText("Plot Selected Group");
        plotGroupButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotGroupButton_actionPerformed(e);
            }
        });
        tableButton.setText("Create Table");
        tableButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                tableButton_actionPerformed(e);
            }
        });
        selectedPanel.setBorder(titledBorder2);
        selectedPanel.setLayout(verticalLayout3);
        plotPanel.setBorder(titledBorder3);
        plotPanel.setLayout(verticalLayout4);
        gridLayout1.setColumns(2);
        grpLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        circleDisplayButton.setText("Circular Display");
        circleDisplayButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                circleDisplayButton_actionPerformed(e);
            }
        });
        jPanel1.setLayout(borderLayout2);
        jPanel1.setBorder(titledBorder4);
        groupBox.addItemListener(new java.awt.event.ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                groupBox_itemStateChanged(e);
            }
        });
        scatterButton.setText("Two Column Plot");
        scatterButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scatterButton_actionPerformed(e);
            }
        });
        saveGrpButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveGrpButton_actionPerformed(e);
            }
        });
        saveGrpButton.setText("Save Group As...");
        saveGrpButton.setEnabled(false);
        this.getContentPane().add(selectPane, BorderLayout.NORTH);
        selectPane.add(jPanel1, null);
        jPanel1.add(groupBox, BorderLayout.CENTER);
        selectPane.add(newGroupPanel, null);
        newGroupPanel.add(useCritButton, null);
        this.getContentPane().add(currentPane, BorderLayout.CENTER);
        selectedPanel.add(selLabel, null);
        selectedPanel.add(grpLabel, null);
        selectedPanel.add(grpLabel2, null);
        selectedPanel.add(editGrpButton, null);
        selectedPanel.add(saveGrpButton, null);
        currentPane.add(plotPanel, null);
        currentPane.add(selectedPanel, null);
        plotPanel.add(plotGroupButton, null);
        plotPanel.add(tableButton, null);
        plotPanel.add(scatterButton, null);
        plotPanel.add(circleDisplayButton, null);
        this.addFocusListener(new FocusAdapter() {

            public void focusGained(FocusEvent e) {
                reload();
            }
        });
    }

    private void editGrpButton_actionPerformed(ActionEvent e) {
        GroupEditFrame gef = new GroupEditFrame((grpMain == null ? new GrpFile() : grpMain), expMain, project, parentFrame);
        gef.setParentFrame(this);
        this.getDesktopPane().add(gef);
        gef.setSize(400, 400);
        gef.setLocation(100, 100);
        gef.show();
        gef.toFront();
    }

    private void plotGroupButton_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                PlotFrame plotframe = new PlotFrame((grpMain == null ? new GrpFile() : grpMain), expMain, parentFrame, project);
                getDesktopPane().add(plotframe);
                plotframe.pack();
                plotframe.show();
                plotframe.toFront();
                setCursor(Cursor.getDefaultCursor());
            }
        };
        thread.start();
    }

    private void useCritButton_actionPerformed(ActionEvent e) {
        CritDialog critdialog;
        if (criteria == null) critdialog = new CritDialog(expMain, parentFrame); else critdialog = new CritDialog(expMain, parentFrame, criteria);
        critdialog.setModal(true);
        critdialog.show();
        GrpFile temp = critdialog.getValue();
        CritDialog.Criteria c = critdialog.getCriteria();
        if (temp != null) {
            this.setGrpFile(temp);
            criteria = c;
        }
    }

    private void tableButton_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                TableFrame tableframe = new TableFrame(expMain, (grpMain == null ? new GrpFile() : grpMain), project);
                getDesktopPane().add(tableframe);
                tableframe.show();
                tableframe.toFront();
                tableframe.setSize(tableframe.getWidth() + 1, tableframe.getHeight());
                setCursor(Cursor.getDefaultCursor());
            }
        };
        thread.start();
    }

    /**
     * sets the select group file
     * @param grpMain select group file
     */
    public void setGrpFile(GrpFile grpMain) {
        this.grpMain = grpMain;
        grpLabel.setForeground(Color.red);
        grpLabel.setText(grpMain.getTitle());
        grpLabel2.setForeground(Color.red);
        grpLabel2.setText(grpMain.getNumGenes() + " genes");
        editGrpButton.setEnabled(true);
        plotGroupButton.setEnabled(true);
        circleDisplayButton.setEnabled(true);
        if (grpMain.getNumGenes() > 0) saveGrpButton.setEnabled(true); else saveGrpButton.setEnabled(false);
    }

    /**
     * sets the selected group files
     * @param fileobj select group file
     */
    public void setGrpFile(File fileobj) {
        grpMain = new GrpFile(fileobj);
        setGrpFile(grpMain);
    }

    private void circleDisplayButton_actionPerformed(ActionEvent e) {
        Thread thread = new Thread() {

            public void run() {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                CircleDisplayFrame cdframe = new CircleDisplayFrame(expMain, (grpMain == null ? new GrpFile() : grpMain), parentFrame, project);
                Dimension d = getToolkit().getScreenSize();
                getDesktopPane().add(cdframe);
                cdframe.setSize(getDesktopPane().getSize());
                cdframe.setLocation(0, 0);
                cdframe.show();
                cdframe.toFront();
                setCursor(Cursor.getDefaultCursor());
            }
        };
        thread.start();
    }

    private void groupBox_itemStateChanged(ItemEvent e) {
        if (groupBox.getSelectedIndex() != 0 && groupBox.getSelectedIndex() != -1) {
            setGrpFile(new File(groupBox.getFilePath()));
            criteria = null;
        }
    }

    private void scatterButton_actionPerformed(ActionEvent e) {
        final ColumnChooser chooser = new ColumnChooser(expMain, parentFrame);
        chooser.setModal(true);
        chooser.pack();
        chooser.setSize((chooser.getWidth() < 300 ? 300 : chooser.getWidth()), (chooser.getHeight() < 150 ? 150 : chooser.getHeight()));
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        chooser.setLocation((screen.width - chooser.getWidth()) / 2, (screen.height - chooser.getHeight()) / 2);
        chooser.setVisible(true);
        if (chooser.getOK()) {
            Thread thread = new Thread() {

                public void run() {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    PlotFrame plotframe = new PlotFrame((grpMain == null ? new GrpFile() : grpMain), expMain, parentFrame, project);
                    getDesktopPane().add(plotframe);
                    plotframe.setColumns(chooser.getColumn1(), chooser.getColumn2());
                    plotframe.showRegression(true);
                    plotframe.pack();
                    plotframe.show();
                    plotframe.toFront();
                    setCursor(Cursor.getDefaultCursor());
                }
            };
            thread.start();
        }
    }

    private void saveGrpButton_actionPerformed(ActionEvent e) {
        DefaultListModel groupModel = new DefaultListModel();
        JList groupGenes = new JList();
        groupGenes.setModel(groupModel);
        Object[] o = grpMain.getGroup();
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
                newGrp.setExpFile(expMain.getName());
                try {
                    File file = new File(project.getPath() + expMain.getName() + File.separator + s);
                    int result = JOptionPane.YES_OPTION;
                    if (file.exists()) {
                        result = JOptionPane.showConfirmDialog(parentFrame, "The file " + file.getPath() + " already exists.  Overwrite this file?", "Overwrite File?", JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) file.delete();
                    }
                    if (result == JOptionPane.YES_OPTION) newGrp.writeGrpFile(project.getPath() + expMain.getName() + File.separator + s);
                } catch (DidNotFinishException e2) {
                    JOptionPane.showMessageDialog(parentFrame, "Error Writing Group File");
                }
                project.addFile(expMain.getName() + File.separator + s);
                groupBox.reload();
                groupBox.setSelectedIndex(groupBox.getItemCount() - 1);
            }
        } else {
            JOptionPane.showMessageDialog(parentFrame, "No Genes Selected");
        }
    }

    /**
   * reloads the list of group files
   */
    public void reload() {
        groupBox.reload();
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
