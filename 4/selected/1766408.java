package rogue.netbeans.module.importsources;

import java.awt.Dialog;
import java.awt.EventQueue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import rogue.netbeans.module.importsources.ui.JCheckBoxTree;
import rogue.netbeans.module.importsources.ui.JCheckBoxTreeNode;

/**
 * The main entry point for this module.
 *
 * @author Rogue
 */
public final class ImportSourcesAction extends CallableSystemAction {

    private JFileChooser fileChooser = null;

    private PrintWriter pw = null;

    private WizardDescriptor wizardDescriptor = null;

    private JCheckBoxTree tree = null;

    private Project project = null;

    private SourceGroup group = null;

    private String rootDir = null;

    public String getName() {
        return NbBundle.getMessage(ImportSourcesAction.class, "CTL_ImportSourcesAction");
    }

    protected void initialize() {
        super.initialize();
        putValue("noIconInMenu", Boolean.TRUE);
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    protected boolean asynchronous() {
        return false;
    }

    public void performAction() {
        ImportSourcesWizardIterator iterator = new ImportSourcesWizardIterator();
        wizardDescriptor = new WizardDescriptor(iterator);
        wizardDescriptor.setTitleFormat(new MessageFormat("{0} {1}"));
        wizardDescriptor.setTitle("Import Sources Wizard.");
        Dialog dialog = DialogDisplayer.getDefault().createDialog(wizardDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        this.tree = null;
        this.project = null;
        this.group = null;
        this.rootDir = null;
        boolean cancelled = wizardDescriptor.getValue() != WizardDescriptor.FINISH_OPTION;
        if (!cancelled) {
            this.tree = (JCheckBoxTree) wizardDescriptor.getProperty(ImportSourcesConstants.SELECTED_FILES.toString());
            this.project = (Project) wizardDescriptor.getProperty(ImportSourcesConstants.SELECTED_PROJECT.toString());
            this.group = (SourceGroup) wizardDescriptor.getProperty(ImportSourcesConstants.SELECTED_SOURCE_GROUP.toString());
            this.rootDir = (String) wizardDescriptor.getProperty(ImportSourcesConstants.SELECTED_DIRECTORY.toString());
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    importSources();
                }
            });
        } else {
            iterator.terminateAllRunningTasks();
        }
    }

    @Override
    public boolean isEnabled() {
        if (OpenProjects.getDefault().getOpenProjects().length == 0) {
            return false;
        }
        return true;
    }

    private void importSources() {
        InputOutput io = IOProvider.getDefault().getIO("Import Sources", false);
        io.select();
        PrintWriter pw = new PrintWriter(io.getOut());
        pw.println("Beginning transaction....");
        pw.println("Processing selected files:");
        String[][] selectedFiles = getSelectedFiles(pw);
        if (selectedFiles.length == 0) {
            pw.println("There are no files to process.");
        } else {
            pw.println(new StringBuilder("Importing ").append(selectedFiles.length).append(" files to ").append(group.getDisplayName()).append(" within project ").append(ProjectUtils.getInformation(project).getDisplayName()).toString());
            FileObject destFO = group.getRootFolder();
            try {
                String destRootDir = new File(destFO.getURL().toURI()).getAbsolutePath();
                if (destFO.canWrite()) {
                    for (String[] s : selectedFiles) {
                        try {
                            File parentDir = new File(new StringBuilder(destRootDir).append(File.separator).append(s[0]).toString());
                            if (!parentDir.exists()) {
                                parentDir.mkdirs();
                            }
                            File f = new File(new StringBuilder(destRootDir).append(s[0]).append(File.separator).append(s[1]).toString());
                            if (!f.exists()) {
                                f.createNewFile();
                            }
                            FileInputStream fin = null;
                            FileOutputStream fout = null;
                            byte[] b = new byte[1024];
                            int read = -1;
                            try {
                                File inputFile = new File(new StringBuilder(rootDir).append(s[0]).append(File.separator).append(s[1]).toString());
                                pw.print(new StringBuilder("\tImporting file:").append(inputFile.getAbsolutePath()).toString());
                                fin = new FileInputStream(inputFile);
                                fout = new FileOutputStream(f);
                                while ((read = fin.read(b)) != -1) {
                                    fout.write(b, 0, read);
                                }
                                pw.println(" ... done");
                                fin.close();
                                fout.close();
                            } catch (FileNotFoundException ex) {
                                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Exception(ex, "Error while importing sources!"));
                            } catch (IOException ex) {
                                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Exception(ex, "Error while importing sources!"));
                            } finally {
                                if (fin != null) {
                                    try {
                                        fin.close();
                                    } catch (IOException ex) {
                                        DialogDisplayer.getDefault().notify(new NotifyDescriptor.Exception(ex, "Error while importing sources!"));
                                    }
                                }
                                if (fout != null) {
                                    try {
                                        fout.close();
                                    } catch (IOException ex) {
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            DialogDisplayer.getDefault().notify(new NotifyDescriptor.Exception(ex, "Error while importing sources!"));
                        }
                    }
                    pw.println("Import sources completed successfully.");
                } else {
                    pw.println("Cannot write to the destination directory." + " Please check the priviledges and try again.");
                    return;
                }
            } catch (FileStateInvalidException ex) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Exception(ex, "Error while importing sources!"));
                pw.println("Import failed!!");
            } catch (URISyntaxException ex) {
                DialogDisplayer.getDefault().notify(new NotifyDescriptor.Exception(ex, "Error while importing sources!"));
                pw.println("Import failed!!");
            }
        }
    }

    private String[][] getSelectedFiles(PrintWriter pw) {
        String[][] selectedFiles = null;
        Object obj = tree.getModel().getRoot();
        if (obj != null && obj instanceof JCheckBoxTreeNode) {
            List<String[]> selectedFilesList = new ArrayList<String[]>();
            JCheckBoxTreeNode rootNode = (JCheckBoxTreeNode) obj;
            if (rootNode.getDepth() == 0) {
                return new String[0][0];
            }
            pw.println("Initializing the transaction set.");
            JCheckBoxTreeNode leaf = (JCheckBoxTreeNode) rootNode.getFirstLeaf();
            while (leaf != null) {
                if (leaf.isSelected()) {
                    String[] data = new String[2];
                    Object[] path = leaf.getUserObjectPath();
                    StringBuilder bldr = new StringBuilder();
                    for (int i = 1; i < path.length - 1; i++) {
                        bldr.append(File.separator).append(path[i]);
                    }
                    data[0] = bldr.toString();
                    data[1] = (String) path[path.length - 1];
                    pw.println(new StringBuilder("\tAdding file: ").append(rootDir).append(data[0]).append(File.separator).append(data[1]).append(" to the transaction set."));
                    selectedFilesList.add(data);
                }
                leaf = (JCheckBoxTreeNode) leaf.getNextLeaf();
            }
            selectedFiles = selectedFilesList.toArray(new String[selectedFilesList.size()][2]);
            pw.println("Transaction set initialization complete.");
        } else {
            selectedFiles = new String[0][0];
        }
        return selectedFiles;
    }
}
