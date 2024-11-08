import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.Rectangle;
import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.JTree;
import javax.swing.JScrollPane;

public class ImportExportDialog {

    public class ExtensionFilter extends FileFilter {

        public ExtensionFilter(String ext, String descr) {
            extension = ext.toLowerCase();
            description = descr;
        }

        public boolean accept(File file) {
            return (file.isDirectory() || file.getName().toLowerCase().endsWith(extension));
        }

        public String getDescription() {
            return description;
        }

        public String getExtension() {
            return extension;
        }

        private String description;

        private String extension;
    }

    private JFrame projectFrame = null;

    private JPanel jContentPane = null;

    private JTextField importTextField = null;

    private JTextField exportTextField = null;

    private JButton browseImportButton = null;

    private JButton browseExportButton = null;

    private JButton browseSrcButton = null;

    private JLabel importLabel = null;

    private JLabel exportLabel = null;

    private JLabel srcLabel = null;

    private JButton importButton = null;

    private JButton exportButton = null;

    private JFileChooser fileChooser = null;

    private JTree projectTree = null;

    private Project project = null;

    private String srcFolder = System.getProperty("user.dir") + "\\src".replace("\\", File.separator);

    private JScrollPane jScrollPane = null;

    private JCheckBox createJar = null;

    private ExtensionFilter projectFilter = new ExtensionFilter(".prj", "FBench project file (*.prj)");

    private ExtensionFilter projectJarFilter = new ExtensionFilter(".jar", "Compressed FBench project file (*.jar)");

    private ExtensionFilter systemFilter = new ExtensionFilter(".sys", "System file (*.sys)");

    public void show() {
        getProjectFrame().setVisible(true);
    }

    /**
	 * This method initializes projectFrame
	 * 
	 * @return javax.swing.JFrame
	 */
    private JFrame getProjectFrame() {
        if (projectFrame == null) {
            projectFrame = new JFrame();
            projectFrame.setSize(new Dimension(380, 145));
            projectFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            projectFrame.setTitle("Project");
            projectFrame.setContentPane(getJContentPane());
        }
        return projectFrame;
    }

    /**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            exportLabel = new JLabel();
            exportLabel.setBounds(new Rectangle(8, 45, 180, 16));
            exportLabel.setText("Export project from source:");
            importLabel = new JLabel();
            importLabel.setBounds(new Rectangle(8, 5, 180, 16));
            importLabel.setText("Import project into source:");
            srcLabel = new JLabel();
            srcLabel.setBounds(new Rectangle(8, 88, 300, 16));
            srcLabel.setText("Source: " + srcFolder);
            srcLabel.setToolTipText(srcFolder);
            jContentPane = new JPanel();
            jContentPane.setLayout(null);
            jContentPane.add(getImportTextField(), null);
            jContentPane.add(getExportTextField(), null);
            jContentPane.add(getBrowseImportButton(), null);
            jContentPane.add(getBrowseExportButton(), null);
            jContentPane.add(getBrowseSrcButton(), null);
            jContentPane.add(importLabel, null);
            jContentPane.add(exportLabel, null);
            jContentPane.add(srcLabel, null);
            jContentPane.add(getImportButton(), null);
            jContentPane.add(getExportButton(), null);
            jContentPane.add(getJScrollPane(), null);
            jContentPane.add(getCreateJar(), null);
            getProjectTree();
        }
        return jContentPane;
    }

    /**
	 * This method initializes importTextField
	 * 
	 * @return javax.swing.JTextField
	 */
    private JTextField getImportTextField() {
        if (importTextField == null) {
            importTextField = new JTextField();
            importTextField.setPreferredSize(new Dimension(200, 20));
            importTextField.setBounds(new Rectangle(8, 24, 196, 20));
        }
        return importTextField;
    }

    /**
	 * This method initializes exportTextField
	 * 
	 * @return javax.swing.JTextField
	 */
    private JTextField getExportTextField() {
        if (exportTextField == null) {
            exportTextField = new JTextField();
            exportTextField.setBounds(new Rectangle(8, 62, 196, 20));
        }
        return exportTextField;
    }

    /**
	 * This method initializes browseImportButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getBrowseImportButton() {
        if (browseImportButton == null) {
            browseImportButton = new JButton();
            browseImportButton.setBounds(new Rectangle(203, 24, 25, 19));
            browseImportButton.setText("Browse");
            browseImportButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    browseImport();
                }
            });
        }
        return browseImportButton;
    }

    /**
	 * This method initializes browseExportButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getBrowseExportButton() {
        if (browseExportButton == null) {
            browseExportButton = new JButton();
            browseExportButton.setBounds(new Rectangle(202, 62, 25, 19));
            browseExportButton.setText("Browse");
            browseExportButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    browseExport();
                }
            });
        }
        return browseExportButton;
    }

    /**
	 * This method initializes importButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getImportButton() {
        if (importButton == null) {
            importButton = new JButton();
            importButton.setBounds(new Rectangle(233, 24, 75, 19));
            importButton.setText("Import");
            importButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (getImportTextField().getText().equals("")) {
                        if (browseImport() == JFileChooser.APPROVE_OPTION) {
                            importProject();
                        }
                    } else {
                        importProject();
                    }
                }
            });
        }
        return importButton;
    }

    /**
	 * This method initializes exportButton
	 * 
	 * @return javax.swing.JButton
	 */
    private JButton getExportButton() {
        if (exportButton == null) {
            exportButton = new JButton();
            exportButton.setBounds(new Rectangle(232, 62, 75, 19));
            exportButton.setText("Export");
            exportButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    if (getExportTextField().getText().equals("")) {
                        if (browseExport() == JFileChooser.APPROVE_OPTION) {
                            createProject();
                        }
                    } else {
                        createProject();
                    }
                }
            });
        }
        return exportButton;
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser(System.getProperty("user.dir"));
        }
        return fileChooser;
    }

    private int browseImport() {
        JFileChooser chooser = getFileChooser();
        chooser.setDialogTitle("Import Project");
        chooser.setApproveButtonText("Import");
        chooser.setApproveButtonToolTipText("Import the selected project file");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(projectJarFilter);
        chooser.setFileFilter(projectFilter);
        int result = chooser.showOpenDialog(getProjectFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            String file = chooser.getSelectedFile().getPath();
            getImportTextField().setText(file);
            project = Project.fromFile(file);
            if (project == null) {
                JOptionPane.showMessageDialog(getProjectFrame(), "Error loading project file.");
                hideTree();
            } else {
                showTree(getProjectName(file));
            }
        } else {
            project = null;
            hideTree();
        }
        return result;
    }

    private int browseExport() {
        JFileChooser chooser = getFileChooser();
        chooser.setDialogTitle("Choose System file");
        chooser.setApproveButtonText("Open");
        chooser.setApproveButtonToolTipText("Open the selected file.");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(systemFilter);
        int result = chooser.showOpenDialog(getProjectFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            String file = chooser.getSelectedFile().getPath();
            if (!file.startsWith(srcFolder)) {
                JOptionPane.showMessageDialog(projectFrame, "The system file must reside within the source directory.\n\n" + "Please set the make sure that the given source directory\n" + "is correct, and that the system file resides beneath it.");
                return JFileChooser.CANCEL_OPTION;
            }
            getExportTextField().setText(file);
            project = new Project(file, srcFolder);
            if (project == null) {
                JOptionPane.showMessageDialog(getProjectFrame(), "Error loading system file.");
                hideTree();
            } else {
                showTree(getProjectName(file));
            }
        } else {
            project = null;
            hideTree();
        }
        return result;
    }

    private void importProject() {
        String file = getImportTextField().getText();
        if (!Project.importProject(file, false, srcFolder)) JOptionPane.showMessageDialog(getProjectFrame(), "Project successfully imported"); else JOptionPane.showMessageDialog(getProjectFrame(), "Error importing project.");
    }

    private void createProject() {
        JFileChooser chooser = getFileChooser();
        chooser.setDialogTitle("Choose export directory");
        chooser.setApproveButtonText("Open");
        chooser.setApproveButtonToolTipText("Open the selected directory.");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setFileFilter(null);
        if (chooser.showOpenDialog(getProjectFrame()) == JFileChooser.APPROVE_OPTION) {
            String file = getExportTextField().getText();
            project = new Project(file, srcFolder);
            if (project == null) {
                JOptionPane.showMessageDialog(getProjectFrame(), "Error creating project");
                hideTree();
            } else {
                String path = chooser.getSelectedFile().getPath();
                boolean jar = getCreateJar().isSelected();
                if (!jar) {
                    File f = new File(path + File.separator + "config.prj");
                    if (f.exists()) {
                        int result = JOptionPane.showConfirmDialog(projectFrame, "A project already exists in this directory, overwrite?");
                        if (result == JOptionPane.NO_OPTION) {
                            createProject();
                            return;
                        } else if (result == JOptionPane.CANCEL_OPTION) return;
                    }
                }
                project.exportFiles(path, jar);
                JOptionPane.showMessageDialog(getProjectFrame(), "Project successfully created:\n" + project.getLastSavedLocation());
                showTree(getProjectName(file));
            }
        }
    }

    private String getProjectName(String file) {
        return file.substring(file.lastIndexOf(File.separator) + 1);
    }

    private void showTree(String name) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(name);
        for (ProjectInclude include : project.getIncludes().values()) {
            String path = include.path;
            path = path.replace("$SRC$", "");
            path = path.substring(1, path.length());
            String[] parts = path.split("\\\\", 100);
            DefaultMutableTreeNode current = root;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("")) continue;
                DefaultMutableTreeNode child = null;
                boolean found = false;
                Enumeration children = current.children();
                while (children.hasMoreElements()) {
                    child = (DefaultMutableTreeNode) children.nextElement();
                    if (child.toString().equals(parts[i])) {
                        current = child;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    child = new DefaultMutableTreeNode(parts[i]);
                    current.add(child);
                    current = child;
                }
            }
        }
        createProjectTree(root);
        getJScrollPane().setVisible(true);
        getProjectFrame().setSize(380, 352);
    }

    private void hideTree() {
        getProjectFrame().setSize(380, 145);
        getJScrollPane().setVisible(false);
    }

    /**
	 * This method initializes projectTree
	 * 
	 * @return javax.swing.JTree
	 */
    private JTree getProjectTree() {
        if (projectTree == null) {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Project");
            createProjectTree(root);
        }
        return projectTree;
    }

    private JTree createProjectTree(DefaultMutableTreeNode root) {
        projectTree = new JTree(root);
        projectTree.setBounds(new Rectangle(7, 125, 354, 177));
        projectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        getJScrollPane().setViewportView(projectTree);
        return projectTree;
    }

    /**
	 * This method initializes jScrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
    private JScrollPane getJScrollPane() {
        if (jScrollPane == null) {
            jScrollPane = new JScrollPane();
            jScrollPane.setBounds(new Rectangle(7, 112, 353, 197));
            jScrollPane.setVisible(false);
        }
        return jScrollPane;
    }

    public static void main(String[] args) {
        new ImportExportDialog().show();
    }

    private JCheckBox getCreateJar() {
        if (createJar == null) {
            createJar = new JCheckBox("JAR");
            createJar.setPreferredSize(new Dimension(50, 20));
            createJar.setBounds(new Rectangle(310, 62, 50, 20));
        }
        return createJar;
    }

    public static void showDialog(Vector v) {
        new ImportExportDialog().show();
    }

    private JButton getBrowseSrcButton() {
        if (browseSrcButton == null) {
            browseSrcButton = new JButton();
            browseSrcButton.setBounds(new Rectangle(314, 86, 25, 19));
            browseSrcButton.setText("Browse");
            browseSrcButton.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent e) {
                    browseSrc();
                }
            });
        }
        return browseSrcButton;
    }

    private void browseSrc() {
        JFileChooser chooser = getFileChooser();
        chooser.setDialogTitle("Choose source directory");
        chooser.setApproveButtonText("Open");
        chooser.setApproveButtonToolTipText("Open the selected directory.");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setSelectedFile(new File(srcFolder));
        chooser.setFileFilter(null);
        if (chooser.showOpenDialog(getProjectFrame()) == JFileChooser.APPROVE_OPTION) {
            srcFolder = chooser.getSelectedFile().getPath();
            srcLabel.setText("Source: " + srcFolder);
            srcLabel.setToolTipText(srcFolder);
        }
    }
}
