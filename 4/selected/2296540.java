package magictool.clusterdisplay;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import magictool.DidNotFinishException;
import magictool.ExpFile;
import magictool.GrpFile;
import magictool.MainFrame;
import magictool.PlotFrame;
import magictool.Project;
import magictool.filefilters.GifFilter;
import magictool.filefilters.NoEditFileChooser;

/**
 *MetricTreeFrame is a JInternalFrame which displays a printable MetricTree and ruler to go along with
 *the tree.
 */
public class MetricTreeFrame extends JInternalFrame {

    private JPanel mainPanel = new JPanel();

    private JScrollPane treeScroll = new JScrollPane();

    private JPanel buttonPanel = new JPanel();

    private JMenuBar menus = new JMenuBar();

    private JMenu fileMenu = new JMenu();

    private JMenuItem saveMenu = new JMenuItem();

    private JMenuItem saveGrpMenu = new JMenuItem();

    private JMenuItem printMenu = new JMenuItem();

    private JMenuItem closeMenu = new JMenuItem();

    private BorderLayout borderLayout1 = new BorderLayout();

    private JButton plotButton = new JButton();

    private Project project;

    /**MetricTree to be displayed*/
    protected MetricTree theTree;

    /**ruler for the MetricTree to be displayed as the column header*/
    protected MetricRuler ruler;

    /**parent frame*/
    protected Frame parentFrame;

    /**cluster file to display in MetricTree form*/
    protected File clustFile;

    /**expression file associated with the cluster file*/
    protected ExpFile exp;

    public MetricTreeFrame(File clustFile, ExpFile exp, Frame parentFrame, Project project) {
        this.clustFile = clustFile;
        this.exp = exp;
        this.parentFrame = parentFrame;
        this.project = project;
        theTree = new MetricTree(clustFile);
        ruler = new MetricRuler();
        this.setTitle("Displaying " + clustFile.getName());
        try {
            jbInit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void jbInit() throws Exception {
        this.setClosable(true);
        this.setJMenuBar(menus);
        this.setMaximizable(true);
        this.setResizable(true);
        theTree.setBackground(Color.white);
        theTree.setPreferredSize(theTree.getPS());
        this.setMinimumSize(new Dimension(300, 300));
        mainPanel.setLayout(borderLayout1);
        plotButton.setText("Plot Selected Nodes");
        plotButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(ActionEvent e) {
                plotButton_actionPerformed(e);
            }
        });
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
        printMenu.setText("Print Tree");
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
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        buttonPanel.add(plotButton, null);
        mainPanel.add(treeScroll, BorderLayout.CENTER);
        treeScroll.getViewport().add(theTree);
        treeScroll.getHorizontalScrollBar().setUnitIncrement(10);
        treeScroll.getVerticalScrollBar().setUnitIncrement(10);
        menus.add(fileMenu);
        fileMenu.add(saveMenu);
        fileMenu.add(saveGrpMenu);
        fileMenu.add(printMenu);
        fileMenu.addSeparator();
        fileMenu.add(closeMenu);
        treeScroll.setColumnHeaderView(theTree.getRuler());
    }

    private void plotButton_actionPerformed(ActionEvent e) {
        GrpFile group = theTree.getGroup();
        if (group.getNumGenes() == 0) return;
        PlotFrame plotFrame = new PlotFrame(group, exp, parentFrame, project);
        this.getDesktopPane().add(plotFrame);
        plotFrame.show();
    }

    private void closeMenu_actionPerformed(ActionEvent e) {
        this.dispose();
    }

    private void saveMenu_actionPerformed(ActionEvent e) {
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
                final String picture = name;
                Thread thread = new Thread() {

                    public void run() {
                        saveImage(picture);
                    }
                };
                thread.start();
            }
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(this, "Failed To Create Image");
        }
    }

    private void saveGrpMenu_actionPerformed(ActionEvent e) {
        DefaultListModel groupModel = new DefaultListModel();
        JList groupGenes = new JList();
        groupGenes.setModel(groupModel);
        Object[] o = theTree.getGroup().getGroup();
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

    private void saveImage(String name) {
        try {
            int number = 1;
            theTree.saveImage(name, number = (int) Math.ceil(theTree.getMegaPixels() / project.getImageSize()));
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
                PrinterJob printJob = PrinterJob.getPrinterJob();
                PageFormat pf = printJob.pageDialog(printJob.defaultPage());
                printJob.setPrintable(theTree, pf);
                if (printJob.printDialog()) {
                    try {
                        printJob.print();
                    } catch (Exception PrintException) {
                    }
                }
            }
        };
        thread.start();
    }
}
