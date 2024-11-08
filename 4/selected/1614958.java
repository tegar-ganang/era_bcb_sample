package my.donews.wanyancan;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.NumberFormatter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.farng.mp3.MP3File;
import org.farng.mp3.TagConstant;
import org.farng.mp3.TagException;
import org.farng.mp3.TagOptionSingleton;
import org.farng.mp3.id3.AbstractID3v2;
import org.farng.mp3.id3.AbstractID3v2Frame;
import org.farng.mp3.id3.FrameBodyTALB;
import org.farng.mp3.id3.FrameBodyTIT2;
import org.farng.mp3.id3.FrameBodyTPE1;
import org.farng.mp3.id3.FrameBodyTRCK;
import org.farng.mp3.id3.FrameBodyTYER;
import org.farng.mp3.id3.ID3v1;
import org.farng.mp3.id3.ID3v1_1;
import org.farng.mp3.id3.ID3v2_4;

/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class Mp3Tagger extends javax.swing.JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7000605140789409549L;

    public static final boolean DEBUG = false;

    {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JMenuBar jMenuBar1;

    private JMenu jMenu3;

    private JMenuItem openFileMenuItem;

    private JMenuItem cutMenuItem;

    private JMenuItem pasteMenuItem;

    private JSplitPane jSplitPane2;

    private JTabbedPane jTabbedPane1;

    private JMenuItem helpMenuItem;

    private JMenu jMenu5;

    private JMenuItem deleteMenuItem;

    private JSeparator jSeparator1;

    private JMenuItem copyMenuItem;

    private JMenu jMenu4;

    private JMenuItem exitMenuItem;

    private JSeparator jSeparator2;

    private JTable jTable2;

    private JLabel jLabelTitle;

    private JTextField jTextFieldYear;

    private JCheckBox jCheckBoxDeleteEmptyFolder;

    private JCheckBox jCheckBoxViewAll;

    private JMenuItem jMenuItemViewAllFiles;

    private JMenuItem jMenuItemOpenFolder;

    private JPopupMenu jPopupMenuTree;

    private JMenuItem jMnItmAutoMove;

    private JMenuItem jMnItmRename;

    private JButton jButtonStop;

    private JButton jButtonPause;

    private JButton jButtonPlay;

    private JMenuItem jMenuItem1;

    private JPopupMenu jPMenuTable;

    private JCheckBox jCheckBox1;

    private JButton jButtonSave;

    private JTextField jTextFieldTrack;

    private JLabel jLabelTrack;

    private JLabel jLabelYear;

    private JTextField jTextFieldAlbum;

    private JTextField jTextFieldAuthor;

    private JLabel jLabelAlbum;

    private JLabel jLabelAuthor;

    private JTextField jTextFieldTitle;

    private JTextField jTextFieldFileName;

    private JLabel jLabelFileName;

    private JCheckBox recurfillchkbox;

    private JLabel timecost;

    private JPanel jPanel2;

    private JScrollPane jScrollPane2;

    private JScrollPane jScrollPane1;

    private JSplitPane jSplitPane1;

    private JPanel jPanel1;

    private JMenuItem closeFileMenuItem;

    private JMenuItem saveAsMenuItem;

    private JMenuItem saveMenuItem;

    private JMenuItem newFileMenuItem;

    public my.donews.wanyancan.Mp3Tagger.MyTree treemod;

    private JTree tree;

    private MyTableModel tablemodel;

    private Object[][] data;

    private String[] titles = new String[] { "FileName", "Title", "Artist", "Album", "Year", "Track" };

    private ListSelectionModel tableselmodel;

    private ArrayList<Integer> NeedUpdateSet = new ArrayList<Integer>();

    /**
	 * doingfile[not_real_index] store the item in "data", the real 
	 * index is data[doingfile.get(i)] where "i" is the displayed
	 * index in the table
	 */
    private ArrayList<Integer> doingfile = new ArrayList<Integer>();

    private Mp3Playing mp3runnable;

    private Thread mp3playthread;

    private JMenuItem jMnOpenItem;

    /**
	* Auto-generated main method to display this JFrame
	*/
    public static void main(String[] args) {
        Mp3Tagger inst = new Mp3Tagger();
        inst.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        inst.setVisible(true);
    }

    public Mp3Tagger() {
        super();
        initGUI();
    }

    private void initGUI() {
        try {
            {
                this.setFocusTraversalKeysEnabled(false);
                {
                    jSplitPane1 = new JSplitPane();
                    getContentPane().add(jSplitPane1, BorderLayout.CENTER);
                    {
                        jTabbedPane1 = new JTabbedPane();
                        jSplitPane1.add(jTabbedPane1, JSplitPane.RIGHT);
                        {
                            jPanel1 = new JPanel();
                            GridBagLayout jPanel1Layout = new GridBagLayout();
                            jPanel1Layout.columnWidths = new int[] { 7, 7, 7, 7 };
                            jPanel1Layout.rowHeights = new int[] { 7, 7, 7, 7 };
                            jPanel1Layout.columnWeights = new double[] { 0.1, 0.1, 0.1, 0.1 };
                            jPanel1Layout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.1 };
                            jTabbedPane1.addTab("jPanel1", null, jPanel1, null);
                            jPanel1Layout = new GridBagLayout();
                            jPanel1Layout.columnWidths = new int[] { 7, 7, 7, 7 };
                            jPanel1Layout.rowHeights = new int[] { 7, 7, 7, 7 };
                            jPanel1Layout.columnWeights = new double[] { 0.1, 0.1, 0.1, 0.1 };
                            jPanel1Layout.rowWeights = new double[] { 0.1, 0.1, 0.1, 0.1 };
                            jPanel1.setLayout(jPanel1Layout);
                            {
                                jScrollPane2 = new JScrollPane();
                                jPanel1.add(jScrollPane2, new GridBagConstraints(0, 0, 5, 2, 100.0, 100.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
                            }
                            {
                                tablemodel = new MyTableModel(new DefaultTableModel(data, titles));
                                jTable2 = new JTable();
                                jTable2.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                                jTable2.setCellEditor(new MyTableEditor());
                                tableselmodel = jTable2.getSelectionModel();
                                tableselmodel.addListSelectionListener(new ListSelectionListener() {

                                    public void valueChanged(ListSelectionEvent e) {
                                        if (e.getValueIsAdjusting()) return;
                                        ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                                        ArrayList<Integer> sels = new ArrayList<Integer>();
                                        if (!lsm.isSelectionEmpty()) {
                                            int minIndex = lsm.getMinSelectionIndex();
                                            int maxIndex = lsm.getMaxSelectionIndex();
                                            for (int i = minIndex; i <= maxIndex; i++) {
                                                if (lsm.isSelectedIndex(i)) {
                                                    sels.add(tablemodel.rows[i].index);
                                                }
                                            }
                                        }
                                        PrePare(sels);
                                        if (sels.size() == 0) jButtonPlay.setEnabled(false); else jButtonPlay.setEnabled(true);
                                    }
                                });
                                jTable2.getTableHeader().addMouseListener(new MouseAdapter() {

                                    public void mouseClicked(MouseEvent event) {
                                        if (event.getClickCount() < 2) return;
                                        int tableColumn = jTable2.columnAtPoint(event.getPoint());
                                        int modelColumn = jTable2.convertColumnIndexToModel(tableColumn);
                                        tablemodel.sort(modelColumn);
                                    }
                                });
                                jScrollPane2.setViewportView(jTable2);
                                jTable2.setModel(tablemodel);
                                jPMenuTable = new JPopupMenu();
                                {
                                    jMenuItem1 = new JMenuItem();
                                    jPMenuTable.add(jMenuItem1);
                                    jMenuItem1.setText("Delete");
                                    jMenuItem1.addActionListener(new ActionListener() {

                                        public void actionPerformed(ActionEvent evt) {
                                            File[] files = new File[doingfile.size()];
                                            for (int i = 0; i < doingfile.size(); i++) {
                                                files[i] = (File) data[doingfile.get(i)][6];
                                            }
                                            DeleteFiles(files);
                                            UpdateTable();
                                        }
                                    });
                                }
                                {
                                    jMnItmRename = new JMenuItem();
                                    jPMenuTable.add(jMnItmRename);
                                    jMnItmRename.setText("Rename");
                                    jMnItmRename.addActionListener(new ActionListener() {

                                        public void actionPerformed(ActionEvent evt) {
                                            File[] files = new File[doingfile.size()];
                                            for (int i = 0; i < doingfile.size(); i++) {
                                                files[i] = (File) data[doingfile.get(i)][6];
                                            }
                                            RenameFiles(files);
                                            UpdateTable();
                                        }
                                    });
                                }
                                {
                                    jMnOpenItem = new JMenuItem();
                                    jPMenuTable.add(jMnOpenItem);
                                    jMnOpenItem.setText("Open folder");
                                    jMnOpenItem.addActionListener(new ActionListener() {

                                        public void actionPerformed(ActionEvent e) {
                                            File[] files = new File[doingfile.size()];
                                            for (int i = 0; i < doingfile.size(); i++) {
                                                files[i] = (File) data[doingfile.get(i)][6];
                                            }
                                            OpenFiles(files);
                                        }
                                    });
                                }
                                {
                                    jMnItmAutoMove = new JMenuItem();
                                    jPMenuTable.add(jMnItmAutoMove);
                                    jMnItmAutoMove.setText("AutoMove");
                                    jMnItmAutoMove.addActionListener(new ActionListener() {

                                        public void actionPerformed(ActionEvent evt) {
                                            File[] files = new File[doingfile.size()];
                                            for (int i = 0; i < doingfile.size(); i++) {
                                                files[i] = (File) data[doingfile.get(i)][6];
                                            }
                                            AutoMoveFiles(files);
                                            UpdateTable();
                                        }
                                    });
                                }
                                setComponentPopupMenu(jTable2, jPMenuTable);
                            }
                        }
                        {
                            jLabelFileName = new JLabel();
                            jPanel1.add(jLabelFileName, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 10, 0));
                            jLabelFileName.setText("File Location:");
                        }
                        {
                            jTextFieldFileName = new JTextField();
                            jPanel1.add(jTextFieldFileName, new GridBagConstraints(1, 2, 1, 1, 100.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jTextFieldFileName.setText("jTextField1");
                            jTextFieldFileName.setEditable(false);
                        }
                        {
                            jTextFieldTitle = new JTextField();
                            jPanel1.add(jTextFieldTitle, new GridBagConstraints(3, 2, 1, 1, 120.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jTextFieldTitle.setText("Title");
                            jTextFieldTitle.setEditable(false);
                        }
                        {
                            jLabelTitle = new JLabel();
                            jPanel1.add(jLabelTitle, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 10, 0));
                            jLabelTitle.setText("Title:");
                        }
                        {
                            jLabelAuthor = new JLabel();
                            jPanel1.add(jLabelAuthor, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jLabelAuthor.setText("Artist:");
                        }
                        {
                            jLabelAlbum = new JLabel();
                            jPanel1.add(jLabelAlbum, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 10, 0));
                            jLabelAlbum.setText("Album:");
                        }
                        {
                            jTextFieldAuthor = new JTextField();
                            jPanel1.add(jTextFieldAuthor, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jTextFieldAuthor.setText("jTextFieldAuthor");
                            jTextFieldAuthor.setEditable(false);
                        }
                        {
                            jTextFieldAlbum = new JTextField();
                            jPanel1.add(jTextFieldAlbum, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jTextFieldAlbum.setText("jTextFieldAlbum");
                            jTextFieldAlbum.setEditable(false);
                        }
                        {
                            jLabelYear = new JLabel();
                            jPanel1.add(jLabelYear, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jLabelYear.setText("Year:");
                        }
                        {
                            jLabelTrack = new JLabel();
                            jPanel1.add(jLabelTrack, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jLabelTrack.setText("Track:");
                        }
                        {
                            jTextFieldTrack = new JTextField();
                            jPanel1.add(jTextFieldTrack, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jTextFieldTrack.setText("jTextFieldTrack");
                            jTextFieldTrack.setEditable(false);
                        }
                        {
                            jTextFieldYear = new JTextField();
                            jPanel1.add(jTextFieldYear, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jTextFieldYear.setText("jTextFieldYear");
                        }
                        {
                            jButtonSave = new JButton();
                            jPanel1.add(jButtonSave, new GridBagConstraints(3, 6, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                            jButtonSave.setText("Save");
                            jButtonSave.setEnabled(false);
                            jButtonSave.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    File mp3file;
                                    for (int i : NeedUpdateSet) {
                                        mp3file = (File) data[i][6];
                                        String[] cr = new String[5];
                                        for (int j = 1; j <= 5; j++) {
                                            cr[j - 1] = (String) data[i][j];
                                        }
                                        savercrd(mp3file, cr);
                                    }
                                    UpdateTable();
                                }
                            });
                        }
                        {
                            jButtonPlay = new JButton();
                            jPanel1.add(jButtonPlay, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jButtonPlay.setText("Play");
                            jButtonPlay.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent evt) {
                                    File file;
                                    for (int i : doingfile) {
                                        file = (File) data[i][6];
                                        PlayMp3(file);
                                    }
                                }
                            });
                            jButtonPlay.setEnabled(false);
                        }
                        {
                            jButtonPause = new JButton();
                            jPanel1.add(jButtonPause, new GridBagConstraints(4, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
                            jButtonPause.setText("Pause/Continue");
                            jButtonPause.setEnabled(false);
                            jButtonPause.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    mp3runnable.setPaused(!mp3runnable.isPaused());
                                }
                            });
                        }
                        {
                            jButtonStop = new JButton();
                            jPanel1.add(jButtonStop, new GridBagConstraints(4, 5, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
                            jButtonStop.setText("Stop");
                            jButtonStop.setEnabled(false);
                            jButtonStop.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    mp3playthread.stop();
                                    jButtonPlay.setEnabled(true);
                                    jButtonStop.setEnabled(false);
                                    jButtonPause.setEnabled(false);
                                }
                            });
                        }
                    }
                }
                {
                    jSplitPane2 = new JSplitPane();
                    jSplitPane1.add(jSplitPane2, JSplitPane.LEFT);
                    jSplitPane2.setOrientation(JSplitPane.VERTICAL_SPLIT);
                    {
                        jScrollPane1 = new JScrollPane();
                        jSplitPane2.add(jScrollPane1, JSplitPane.TOP);
                        {
                            treemod = new MyTree();
                            tree = new JTree(treemod.getTreemode());
                            tree.setRootVisible(false);
                            jScrollPane1.setViewportView(tree);
                            {
                                jPopupMenuTree = new JPopupMenu();
                                setComponentPopupMenu(tree, jPopupMenuTree);
                                {
                                    jMenuItemOpenFolder = new JMenuItem();
                                    jPopupMenuTree.add(jMenuItemOpenFolder);
                                    jMenuItemOpenFolder.setText("Open Folder");
                                    jMenuItemOpenFolder.addActionListener(new ActionListener() {

                                        public void actionPerformed(ActionEvent evt) {
                                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                                            if (node == null) return;
                                            MyFile curdir = (MyFile) node.getUserObject();
                                            File parent;
                                            ProcessBuilder pb;
                                            parent = curdir;
                                            pb = new ProcessBuilder("explorer", parent.getAbsolutePath());
                                            pb = pb.redirectErrorStream(true);
                                            try {
                                                System.out.println("starting: " + pb);
                                                System.out.flush();
                                                pb.start();
                                            } catch (IOException e) {
                                                pb = new ProcessBuilder("kfmclient", "exec", parent.getAbsolutePath());
                                                pb = pb.redirectErrorStream(true);
                                                try {
                                                    System.out.println("starting: " + pb);
                                                    System.out.flush();
                                                    pb.start();
                                                } catch (IOException ee) {
                                                    JOptionPane.showMessageDialog(jPanel1, "Sorry, not supported");
                                                }
                                            }
                                        }
                                    });
                                }
                                {
                                    jMenuItemViewAllFiles = new JMenuItem();
                                    jPopupMenuTree.add(jMenuItemViewAllFiles);
                                    jMenuItemViewAllFiles.setText("View All Files");
                                    jMenuItemViewAllFiles.addActionListener(new ActionListener() {

                                        public void actionPerformed(ActionEvent evt) {
                                            System.out.println("jMenuItemViewAllFiles.actionPerformed, event=" + evt);
                                            long starttime = System.currentTimeMillis();
                                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                                            if (node == null) return;
                                            MyFile curdir = (MyFile) node.getUserObject();
                                            TreePath path = tree.getSelectionPath();
                                            if (treemod.explore(node)) {
                                                filltable(curdir);
                                                tree.scrollPathToVisible(path);
                                                jButtonSave.setEnabled(false);
                                                jButtonPlay.setEnabled(false);
                                                jButtonPause.setEnabled(false);
                                                jButtonStop.setEnabled(false);
                                                NeedUpdateSet.clear();
                                                doingfile.clear();
                                            }
                                            starttime = System.currentTimeMillis() - starttime;
                                            timecost.setText("|op spd:" + ((Long) starttime).toString());
                                        }
                                    });
                                }
                            }
                        }
                        tree.addTreeSelectionListener(new TreeSelectionListener() {

                            public void valueChanged(TreeSelectionEvent arg0) {
                                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                                if (node == null) return;
                                MyFile curdir = (MyFile) node.getUserObject();
                                if (curdir != null && curdir.exists() && curdir.listFiles().length == 0 && jCheckBoxDeleteEmptyFolder.isSelected()) {
                                    int result = JOptionPane.showConfirmDialog(jPanel1, "Folder is empty, wannt to kick it out?");
                                    if (result == JOptionPane.YES_OPTION) {
                                        curdir.delete();
                                        tree.setSelectionPath(tree.getSelectionPath().getParentPath());
                                    }
                                }
                                UpdateTable();
                            }
                        });
                    }
                    {
                        jPanel2 = new JPanel();
                        GridLayout jPanel2Layout = new GridLayout(5, 1);
                        jPanel2Layout.setHgap(5);
                        jPanel2Layout.setVgap(5);
                        jPanel2Layout.setColumns(1);
                        jPanel2Layout.setRows(5);
                        jPanel2.setLayout(jPanel2Layout);
                        jSplitPane2.add(jPanel2, JSplitPane.BOTTOM);
                        {
                            timecost = new JLabel();
                            jPanel2.add(timecost);
                            timecost.setText("jLabel1");
                        }
                        {
                            recurfillchkbox = new JCheckBox();
                            jPanel2.add(recurfillchkbox);
                            recurfillchkbox.setText("recursively search");
                        }
                        {
                            jCheckBox1 = new JCheckBox();
                            jPanel2.add(jCheckBox1);
                            jCheckBox1.setText("Save backup");
                            jCheckBox1.addActionListener(new ActionListener() {

                                public void actionPerformed(ActionEvent e) {
                                    TagOptionSingleton.getInstance().setOriginalSavedAfterAdjustingID3v2Padding(jCheckBox1.isSelected());
                                }
                            });
                            TagOptionSingleton.getInstance().setOriginalSavedAfterAdjustingID3v2Padding(jCheckBox1.isSelected());
                        }
                        {
                            jCheckBoxViewAll = new JCheckBox();
                            jPanel2.add(jCheckBoxViewAll);
                            jCheckBoxViewAll.setText("View all Files");
                        }
                        {
                            jCheckBoxDeleteEmptyFolder = new JCheckBox();
                            jPanel2.add(jCheckBoxDeleteEmptyFolder);
                            jCheckBoxDeleteEmptyFolder.setText("Delete Empty Folder");
                            jCheckBoxDeleteEmptyFolder.setSelected(true);
                        }
                    }
                }
            }
            this.setSize(736, 480);
            Toolkit kit = Toolkit.getDefaultToolkit();
            Dimension dimen = kit.getScreenSize();
            this.setBounds(dimen.width / 10, dimen.height / 10, dimen.width * 8 / 10, dimen.height * 8 / 10);
            {
                jMenuBar1 = new JMenuBar();
                setJMenuBar(jMenuBar1);
                {
                    jMenu3 = new JMenu();
                    jMenuBar1.add(jMenu3);
                    jMenu3.setText("File");
                    {
                        newFileMenuItem = new JMenuItem();
                        jMenu3.add(newFileMenuItem);
                        newFileMenuItem.setText("New");
                    }
                    {
                        openFileMenuItem = new JMenuItem();
                        jMenu3.add(openFileMenuItem);
                        openFileMenuItem.setText("Open");
                    }
                    {
                        saveMenuItem = new JMenuItem();
                        jMenu3.add(saveMenuItem);
                        saveMenuItem.setText("Save");
                    }
                    {
                        saveAsMenuItem = new JMenuItem();
                        jMenu3.add(saveAsMenuItem);
                        saveAsMenuItem.setText("Save As ...");
                    }
                    {
                        closeFileMenuItem = new JMenuItem();
                        jMenu3.add(closeFileMenuItem);
                        closeFileMenuItem.setText("Close");
                    }
                    {
                        jSeparator2 = new JSeparator();
                        jMenu3.add(jSeparator2);
                    }
                    {
                        exitMenuItem = new JMenuItem();
                        jMenu3.add(exitMenuItem);
                        exitMenuItem.setText("Exit");
                    }
                }
                {
                    jMenu4 = new JMenu();
                    jMenuBar1.add(jMenu4);
                    jMenu4.setText("Edit");
                    {
                        cutMenuItem = new JMenuItem();
                        jMenu4.add(cutMenuItem);
                        cutMenuItem.setText("Cut");
                    }
                    {
                        copyMenuItem = new JMenuItem();
                        jMenu4.add(copyMenuItem);
                        copyMenuItem.setText("Copy");
                    }
                    {
                        pasteMenuItem = new JMenuItem();
                        jMenu4.add(pasteMenuItem);
                        pasteMenuItem.setText("Paste");
                    }
                    {
                        jSeparator1 = new JSeparator();
                        jMenu4.add(jSeparator1);
                    }
                    {
                        deleteMenuItem = new JMenuItem();
                        jMenu4.add(deleteMenuItem);
                        deleteMenuItem.setText("Delete");
                    }
                }
                {
                    jMenu5 = new JMenu();
                    jMenuBar1.add(jMenu5);
                    jMenu5.setText("Help");
                    {
                        helpMenuItem = new JMenuItem();
                        jMenu5.add(helpMenuItem);
                        helpMenuItem.setText("Help");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void DeleteFiles(File[] files) {
        for (File file : files) {
            int rt = JOptionPane.showConfirmDialog(jPanel1, "Sure to delete file ? :" + file.getAbsolutePath());
            if (rt == JOptionPane.YES_OPTION) {
                if (!file.delete()) JOptionPane.showMessageDialog(jPanel1, "Can't delete this file, Maybe used by some other guys");
            }
        }
    }

    protected void UpdateTable() {
        long starttime = System.currentTimeMillis();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return;
        MyFile curdir = (MyFile) node.getUserObject();
        TreePath path = tree.getSelectionPath();
        if (treemod.explore(node)) {
            filltable(curdir);
            tree.scrollPathToVisible(path);
            jButtonSave.setEnabled(false);
            jButtonPlay.setEnabled(false);
            jButtonPause.setEnabled(false);
            jButtonStop.setEnabled(false);
            NeedUpdateSet.clear();
            doingfile.clear();
        }
        starttime = System.currentTimeMillis() - starttime;
        timecost.setText("|op spd:" + ((Long) starttime).toString());
    }

    private void clearTextEdits() {
        jTextFieldFileName.setText("");
        jTextFieldAlbum.setText("");
        jTextFieldAuthor.setText("");
        jTextFieldTitle.setText("");
        jTextFieldTrack.setText("");
        jTextFieldYear.setText("");
    }

    private void setTextEdits(int i) {
        try {
            File mp3file = (File) data[i][6];
            String[] record = getrcrd(mp3file);
            jTextFieldAlbum.setText(record[3]);
            jTextFieldAuthor.setText(record[2]);
            jTextFieldFileName.setText(mp3file.getCanonicalPath());
            jTextFieldTitle.setText(record[1]);
            jTextFieldTrack.setText(record[5]);
            jTextFieldYear.setText(record[4]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void PrePare(ArrayList<Integer> sels) {
        if (sels.size() == 0) {
            clearTextEdits();
            return;
        }
        doingfile = sels;
        setTextEdits(sels.get(0));
    }

    @SuppressWarnings("finally")
    private File[] listdir(File curdir, int depth) {
        File[] ret = new File[0];
        if (depth == 0) return ret;
        ArrayList<File> dir = new ArrayList<File>();
        try {
            File[] subs = curdir.listFiles();
            for (File subf : subs) {
                if (subf.isDirectory()) {
                    List<File> tmp = Arrays.asList(listdir(subf, depth - 1));
                    dir.addAll(tmp);
                    dir.add(subf);
                }
            }
        } catch (Exception e) {
        } finally {
            return dir.toArray(new File[0]);
        }
    }

    private File[] getRoots() {
        if (System.getProperty("os.name").indexOf("Windows") != -1) {
            Vector<File> list = new Vector<File>();
            for (char i = 'c'; i <= 'z'; ++i) {
                File drive = new File(i + ":" + File.separator);
                if (drive.isDirectory() && drive.exists()) {
                    list.add(drive);
                }
            }
            File[] roots = (File[]) list.toArray(new File[list.size()]);
            return roots;
        } else {
            File root = new File(File.separator);
            return new File[] { root };
        }
    }

    private class MyFile extends File {

        /**
		 * 
		 */
        private static final long serialVersionUID = -6760222533637654638L;

        public MyFile(String arg0) {
            super(arg0);
        }

        public String toString() {
            try {
                return getName().equals("") ? getCanonicalPath() : getName();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return getName();
        }

        @SuppressWarnings("finally")
        public Set<MyFile> getSet() {
            Set<MyFile> result = new TreeSet<MyFile>();
            try {
                File[] files = super.listFiles();
                for (File f : files) try {
                    if (!f.isFile()) result.add(new MyFile(f.getCanonicalPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
            } finally {
                return result;
            }
        }
    }

    /**
	 * find mp3 files non-recursively, save to String[][] data
	 * @param curdir   directory to search
	 * @param clearold   if true, will append newly found file to data, 
	 * will clear old otherwise
	 */
    private void fillnonrecu(File curdir, boolean clearold) {
        ArrayList<Object[]> mp3array;
        mp3array = new ArrayList<Object[]>();
        try {
            File[] files = curdir.listFiles();
            if (!clearold) {
                List<Object[]> lll = Arrays.asList((Object[][]) data);
                mp3array.addAll(lll);
            }
            for (File f : files) {
                if (f.getName().endsWith(".mp3") || jCheckBoxViewAll.isSelected()) {
                    String[] record = null;
                    record = getrcrd(f);
                    ArrayList<Object> bigrec = new ArrayList<Object>();
                    bigrec.addAll(Arrays.asList((Object[]) record));
                    bigrec.add(f);
                    mp3array.add(bigrec.toArray());
                }
            }
        } catch (Exception e) {
        } finally {
            data = new Object[mp3array.size()][];
            data = mp3array.toArray(data);
        }
    }

    private void savercrd(File file, String[] values) {
        MP3File theMp3 = null;
        try {
            theMp3 = new MP3File(file);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        String Tt = values[0];
        String Art = values[1];
        String Alb = values[2];
        String Yr = values[3];
        String Trk = values[4];
        ID3v1 id3tagv1 = null;
        AbstractID3v2 id3tagv2 = null;
        id3tagv1 = theMp3.getID3v1Tag();
        if (id3tagv1 == null || !(id3tagv1 instanceof ID3v1_1)) {
            id3tagv1 = new ID3v1_1();
        }
        id3tagv1.setTitle(Tt);
        id3tagv1.setAlbum(Alb);
        id3tagv1.setArtist(Art);
        id3tagv1.setYear(Yr);
        try {
            ((ID3v1_1) id3tagv1).setTrack((byte) Integer.parseInt(Trk));
        } catch (NumberFormatException e) {
            ((ID3v1_1) id3tagv1).setTrack((byte) 0);
        } finally {
            AbstractID3v2Frame frame;
            id3tagv2 = theMp3.getID3v2Tag();
            if (id3tagv2 == null) {
                id3tagv2 = new ID3v2_4(id3tagv1);
            }
            if (id3tagv2.hasFrame("TIT2")) {
                frame = id3tagv2.getFrame("TIT2");
                ((FrameBodyTIT2) frame.getBody()).setText(Tt);
            }
            if (id3tagv2.hasFrame("TALB")) {
                frame = id3tagv2.getFrame("TALB");
                ((FrameBodyTALB) frame.getBody()).setText(Alb);
            }
            if (id3tagv2.hasFrame("TPE1")) {
                frame = id3tagv2.getFrame("TPE1");
                ((FrameBodyTPE1) frame.getBody()).setText(Art);
            }
            if (id3tagv2.hasFrame("TYER")) {
                frame = id3tagv2.getFrame("TYER");
                ((FrameBodyTYER) frame.getBody()).setText(Yr);
            }
            if (id3tagv2.hasFrame("TRCK")) {
                frame = id3tagv2.getFrame("TRCK");
                ((FrameBodyTRCK) frame.getBody()).setText(Trk);
            }
            theMp3.setID3v1Tag(id3tagv1);
            theMp3.setID3v2Tag(id3tagv2);
            try {
                theMp3.save(TagConstant.MP3_FILE_SAVE_WRITE);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TagException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * index order "file.getName() + tagflag, Tt, Art, Alb, Yr,Trk"
	 * 
	 * @param file
	 * @return
	 */
    private String[] getrcrd(File file) {
        MP3File theMp3 = null;
        String[] objects = null;
        try {
            theMp3 = new MP3File(file);
        } catch (Exception e) {
            if (theMp3 == null) {
                objects = new String[] { file.getName(), (file.isFile()) ? "not MP3 file" : "Folder", "", "", "", "" };
                return objects;
            }
        }
        String tagflag = "";
        String Tt = "";
        String Art = "";
        String Alb = "";
        String Yr = "";
        String Trk = "";
        if (theMp3.hasID3v1Tag()) {
            tagflag = "1";
            ID3v1 id3tag = theMp3.getID3v1Tag();
            if (id3tag != null) {
                Tt = id3tag.getTitle();
                Art = id3tag.getArtist();
                Alb = id3tag.getAlbum();
                Yr = id3tag.getYear();
                try {
                    Trk = String.valueOf(((ID3v1_1) id3tag).getTrack());
                } catch (Exception e) {
                }
            }
        }
        if (theMp3.hasID3v2Tag()) {
            AbstractID3v2Frame frame;
            tagflag = tagflag + "2";
            AbstractID3v2 id3tag = theMp3.getID3v2Tag();
            if (id3tag != null) {
                if (id3tag.hasFrame("TIT2")) {
                    frame = id3tag.getFrame("TIT2");
                    Tt = ((FrameBodyTIT2) frame.getBody()).getText();
                }
                if (id3tag.hasFrame("TALB")) {
                    frame = id3tag.getFrame("TALB");
                    Alb = ((FrameBodyTALB) frame.getBody()).getText();
                }
                if (id3tag.hasFrame("TPE1")) {
                    frame = id3tag.getFrame("TPE1");
                    Art = ((FrameBodyTPE1) frame.getBody()).getText();
                }
                if (id3tag.hasFrame("TYER")) {
                    frame = id3tag.getFrame("TYER");
                    Yr = ((FrameBodyTYER) frame.getBody()).getText();
                }
                if (id3tag.hasFrame("TRCK")) {
                    frame = id3tag.getFrame("TRCK");
                    Trk = ((FrameBodyTRCK) frame.getBody()).getText();
                }
            }
        }
        tagflag = (tagflag.equals("")) ? "" : "  v" + tagflag;
        objects = new String[] { file.getName() + tagflag, Tt, Art, Alb, Yr, Trk };
        return objects;
    }

    class Mp3Playing implements Runnable {

        File file;

        boolean paused = false;

        public Mp3Playing(File mp3file) {
            file = mp3file;
        }

        public boolean isPaused() {
            return paused;
        }

        public void setPaused(boolean paused) {
            this.paused = paused;
        }

        private void rawplay(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException, InterruptedException {
            byte[] data = new byte[4096];
            SourceDataLine line = getLine(targetFormat);
            if (line != null) {
                line.start();
                @SuppressWarnings("unused") int nBytesRead = 0, nBytesWritten = 0;
                while ((!mp3playthread.isInterrupted()) && nBytesRead != -1) {
                    nBytesRead = din.read(data, 0, data.length);
                    while (paused) Thread.sleep(3);
                    if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead);
                    Thread.sleep(3);
                }
                line.drain();
                line.stop();
                line.close();
                din.close();
            }
        }

        private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
            SourceDataLine res = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            res = (SourceDataLine) AudioSystem.getLine(info);
            res.open(audioFormat);
            return res;
        }

        public void run() {
            try {
                PrintStream out = System.out;
                if (out != null) out.println("---  Start : " + file.getName() + "  ---");
                AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
                if (out != null) out.println("Audio Type : " + aff.getType());
                AudioInputStream in = AudioSystem.getAudioInputStream(file);
                AudioInputStream din = null;
                if (in != null) {
                    AudioFormat baseFormat = in.getFormat();
                    if (out != null) out.println("Source Format : " + baseFormat.toString());
                    AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                    if (out != null) out.println("Target Format : " + decodedFormat.toString());
                    Map proper = aff.properties();
                    out.println("Meta Data: " + proper.toString());
                    din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    rawplay(decodedFormat, din);
                    in.close();
                    if (out != null) out.println("---  Stop : " + file.getName() + "  ---");
                    jButtonPlay.setEnabled(true);
                    jButtonPause.setEnabled(false);
                    jButtonStop.setEnabled(false);
                }
            } catch (InterruptedException e) {
                System.out.println("Interrupted...");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void PlayMp3(File file) {
        if (mp3playthread != null) {
            mp3playthread.interrupt();
            mp3playthread.stop();
        }
        jButtonPlay.setEnabled(false);
        jButtonPause.setEnabled(true);
        jButtonStop.setEnabled(true);
        mp3runnable = new Mp3Playing(file);
        mp3playthread = new Thread(mp3runnable);
        mp3playthread.start();
    }

    private static String ValidateFileName(File pardir, String name) {
        name = name.replace("\\", "/");
        name = name.replace("*", "_");
        name = name.replace("?", "_");
        name = name.replace("<", "[");
        name = name.replace(">", "]");
        name = name.replace("\"", "_");
        name = name.replace("|", "_");
        String curDir = null;
        try {
            curDir = pardir.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int parLev = 0;
        int pos = 0;
        String FileSep = System.getProperty("file.separator");
        while ((pos = curDir.indexOf(FileSep, pos)) != -1) {
            parLev++;
            pos += FileSep.length();
        }
        String wannaName = null;
        try {
            wannaName = new File(curDir + FileSep + name).getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int wanLev = 0;
        pos = 0;
        while ((pos = wannaName.indexOf(FileSep, pos)) != -1) {
            wanLev++;
            pos += FileSep.length();
        }
        while (wanLev <= parLev) {
            pos = name.indexOf("../");
            if (pos == -1) break;
            name = name.substring(0, pos) + name.substring(pos + 3);
            wanLev++;
        }
        return name;
    }

    private void OpenFiles(File[] files) {
        File parent;
        ProcessBuilder pb;
        for (File file : files) {
            parent = file.getParentFile();
            pb = new ProcessBuilder("explorer", parent.getAbsolutePath());
            pb = pb.redirectErrorStream(true);
            try {
                System.out.println("starting: " + pb);
                System.out.flush();
                pb.start();
            } catch (IOException e) {
                pb = new ProcessBuilder("kfmclient", "exec", parent.getAbsolutePath());
                pb = pb.redirectErrorStream(true);
                try {
                    System.out.println("starting: " + pb);
                    System.out.flush();
                    pb.start();
                } catch (IOException ee) {
                    JOptionPane.showMessageDialog(this, "Sorry, not supported");
                }
            }
        }
    }

    private void RenameFiles(File[] files) {
        String defPathname = JOptionPane.showInputDialog(jSplitPane1, "Pattern of new name to Rename to \n(%1$s:Tt,%2$s:Art,%3$s:Alb,%4$s:Yr, %5$s:Trk" + System.getProperty("line.separator"), "%5$s_%1$s_%2$s");
        for (File file : files) {
            String[] rc = getrcrd(file);
            String newname = String.format(defPathname, rc[1], rc[2], rc[3], rc[4], rc[5]) + ".mp3";
            newname = ValidateFileName(file.getParentFile(), newname);
            newname = file.getParent() + "/" + newname;
            File parDir = new File(newname).getParentFile();
            if (!parDir.mkdirs()) System.out.println("can't create directory");
            if (!file.renameTo(new File(newname))) System.out.println("Can't move file");
            try {
                System.out.println("Moved to: " + new File(newname).getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void AutoMoveFiles(File[] files) {
        String namePtn = JOptionPane.showInputDialog(jSplitPane1, "Pattern of new name to Rename to \n(%1$s:Tt,%2$s:Art,%3$s:Alb,%4$s:Yr, %5$s:Trk" + System.getProperty("line.separator"), "../../%2$s/%3$s/%5$s_%1$s_%2$s");
        for (File file : files) {
            String[] rc = getrcrd(file);
            String newname = String.format(namePtn, rc[1], rc[2], rc[3], rc[4], rc[5]) + ".mp3";
            newname = ValidateFileName(file.getParentFile(), newname);
            newname = file.getParent() + "/" + newname;
            File parDir = new File(newname).getParentFile();
            if (!parDir.mkdirs()) System.out.println("can't create directory");
            if (!file.renameTo(new File(newname))) System.out.println("Can't move file");
            try {
                System.out.println("Moved to " + new File(newname).getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filltable(File curdir) {
        fillnonrecu(curdir, true);
        if (recurfillchkbox.isSelected()) {
            File[] subs = listdir(curdir, 2);
            for (File subf : subs) {
                if (subf.isDirectory()) {
                    fillnonrecu(subf, false);
                }
            }
        }
        tablemodel = new MyTableModel(new DefaultTableModel(data, titles));
        jTable2.setModel(tablemodel);
    }

    /**
	* Auto-generated method for setting the popup menu for a component
	*/
    private void setComponentPopupMenu(final java.awt.Component parent, final javax.swing.JPopupMenu menu) {
        parent.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(parent, e.getX(), e.getY());
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(parent, e.getX(), e.getY());
            }
        });
    }

    class MyTableEditor extends DefaultCellEditor {

        /**
		 * 
		 */
        private static final long serialVersionUID = 5959801312614147773L;

        JTextField ftf;

        NumberFormat integerFormat;

        private Integer minimum, maximum;

        public MyTableEditor() {
            super(new JTextField());
            ftf = (JTextField) getComponent();
            integerFormat = NumberFormat.getIntegerInstance();
            NumberFormatter intFormatter = new NumberFormatter(integerFormat);
            intFormatter.setFormat(integerFormat);
            intFormatter.setMinimum(minimum);
            intFormatter.setMaximum(maximum);
            ftf.setText("δ֪");
            ftf.setHorizontalAlignment(JTextField.TRAILING);
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            return new JTextField();
        }

        public boolean stopCellEditing() {
            System.out.println("Stopped editing");
            return super.stopCellEditing();
        }
    }

    class MyTableModel extends AbstractTableModel {

        /**
		 * 
		 */
        private static final long serialVersionUID = -2443393572866809774L;

        /**
	      Constructs a sort filter model.
	      @param m the table model whose rows should be sorted
	   */
        public MyTableModel(TableModel m) {
            model = m;
            rows = new Row[model.getRowCount()];
            for (int i = 0; i < rows.length; i++) {
                rows[i] = new Row();
                rows[i].index = i;
            }
        }

        /**
	      Sorts the rows.
	      @param c the column that should become sorted
	   */
        public void sort(int c) {
            sortColumn = c;
            Arrays.sort(rows);
            fireTableDataChanged();
        }

        public Object getValueAt(int r, int c) {
            return data[rows[r].index][c];
        }

        public boolean isCellEditable(int r, int c) {
            if (c == 0) return false;
            return model.isCellEditable(rows[r].index, c);
        }

        public void setValueAt(Object aValue, int r, int c) {
            jButtonSave.setEnabled(true);
            data[rows[r].index][c] = aValue;
            if (DEBUG) {
                int numRows = getRowCount();
                int numCols = getColumnCount();
                for (int i = 0; i < numRows; i++) {
                    System.out.print("    row " + i + ":");
                    for (int j = 0; j < numCols; j++) {
                        System.out.print("  " + data[i][j]);
                    }
                    System.out.println();
                }
                System.out.println("--------------------------");
            }
            for (int i = 0; i < NeedUpdateSet.size(); i++) if (NeedUpdateSet.get(i) == rows[r].index) return;
            NeedUpdateSet.add(rows[r].index);
        }

        public int getRowCount() {
            return model.getRowCount();
        }

        public int getColumnCount() {
            return model.getColumnCount();
        }

        public String getColumnName(int c) {
            return model.getColumnName(c);
        }

        @SuppressWarnings("unchecked")
        public Class getColumnClass(int c) {
            return model.getColumnClass(c);
        }

        /** 
	      This inner class holds the index of the model row
	      Rows are compared by looking at the model row entries
	      in the sort column.
	   */
        private class Row implements Comparable<Row> {

            public int index;

            @SuppressWarnings("unchecked")
            public int compareTo(Row other) {
                Object a = model.getValueAt(index, sortColumn);
                Object b = model.getValueAt(other.index, sortColumn);
                if (sortColumn == 4 || sortColumn == 5) {
                    try {
                        return ((Comparable<Integer>) Integer.parseInt((String) a)).compareTo((Integer.parseInt((String) b)));
                    } catch (Exception e) {
                    }
                }
                if (a instanceof Comparable) return ((Comparable) a).compareTo(b); else return a.toString().compareTo(b.toString());
            }
        }

        private TableModel model;

        private int sortColumn;

        public Row[] rows;
    }

    class MyTree {

        private DefaultTreeModel treemode;

        private DefaultMutableTreeNode root;

        public void syncData() {
            listall(root, 1);
        }

        public boolean explore(DefaultMutableTreeNode curnode) {
            MyFile f = (MyFile) curnode.getUserObject();
            listall(curnode, 1);
            if (!f.exists()) return false;
            return true;
        }

        public MyTree() {
            root = new DefaultMutableTreeNode(new MyFile("fakeroot"));
            treemode = new DefaultTreeModel(root);
            File[] roots = getRoots();
            for (File f : roots) {
                DefaultMutableTreeNode realroot = null;
                try {
                    realroot = new DefaultMutableTreeNode(new MyFile(f.getCanonicalPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                treemode.insertNodeInto(realroot, root, root.getChildCount());
                listall(realroot, 1);
            }
            treemode.setAsksAllowsChildren(true);
        }

        public DefaultTreeModel getTreemode() {
            return treemode;
        }

        public void setTreemode(DefaultTreeModel treemode) {
            this.treemode = treemode;
        }

        private void listall(DefaultMutableTreeNode parent, int maxdifflevel) {
            try {
                Enumeration e = parent.breadthFirstEnumeration();
                kk: while (e.hasMoreElements()) {
                    DefaultMutableTreeNode testnode = (DefaultMutableTreeNode) e.nextElement();
                    if (testnode.getLevel() - parent.getLevel() >= maxdifflevel) break;
                    MyFile f = (MyFile) testnode.getUserObject();
                    if (!f.exists()) {
                        DefaultMutableTreeNode testparent = testnode;
                        do {
                            testnode = testparent;
                            testparent = (DefaultMutableTreeNode) testparent.getParent();
                            treemode.removeNodeFromParent(testnode);
                            f = (MyFile) testparent.getUserObject();
                        } while (!f.exists() && !testparent.isRoot());
                        testnode = testparent;
                    }
                    Set<MyFile> fileset = f.getSet();
                    Set<MyFile> treeset = new TreeSet<MyFile>();
                    Enumeration ec = testnode.children();
                    while (ec.hasMoreElements()) {
                        DefaultMutableTreeNode childnode = (DefaultMutableTreeNode) ec.nextElement();
                        treeset.add((MyFile) childnode.getUserObject());
                    }
                    fileset.removeAll(treeset);
                    for (MyFile subf : fileset) {
                        boolean bFound = false;
                        if (!bFound && subf.isDirectory()) {
                            treemode.insertNodeInto(new DefaultMutableTreeNode(subf), testnode, testnode.getChildCount());
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
