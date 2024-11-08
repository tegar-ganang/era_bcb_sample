package explorer;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import org.wings.*;
import org.wings.table.*;

/**
 * TODO: documentation
 *
 * @author Holger Engels
 * @author Andreas Gruener
 * @author Armin Haaf
 * @version $Revision: 1759 $
 */
public class ExplorerComponents {

    private final DirTableModel dirTableModel;

    private final STable dirTable;

    private final STree explorerTree;

    private final SLabel currentDirectory;

    public ExplorerComponents() {
        dirTableModel = new DirTableModel();
        explorerTree = new STree();
        explorerTree.setCellRenderer(new DirectoryTreeCellRenderer());
        explorerTree.setNodeIndentDepth(20);
        dirTable = new STable(dirTableModel);
        currentDirectory = new SLabel();
        createTree();
        createTable();
        currentDirectory.setText("- no dir -");
    }

    public ExplorerComponents(String dir) {
        this();
        setExplorerBaseDir(dir);
    }

    public void setExplorerBaseDir(String dir) {
        explorerTree.setModel(createDirectoryModel(dir));
        if (explorerTree.getRowCount() > 0) explorerTree.setSelectionRow(0);
    }

    /**
     *
     */
    private void deleteFiles() {
        int selected[] = dirTable.getSelectedRows();
        for (int i = 0; i < selected.length; i++) dirTableModel.getFileAt(selected[i]).delete();
        dirTableModel.reset();
    }

    public SComponent getTable() {
        return dirTable;
    }

    public SComponent getTree() {
        return explorerTree;
    }

    public SLabel getCurrentDirLabel() {
        return currentDirectory;
    }

    protected SComponent createTable() {
        dirTable.setSelectionMode(STable.MULTIPLE_SELECTION);
        dirTable.setDefaultRenderer(Date.class, new DateTableCellRenderer());
        dirTable.setDefaultRenderer(Long.class, new SizeTableCellRenderer());
        return dirTable;
    }

    public SComponent createDeleteButton() {
        SButton delete = new SButton("delete selected");
        delete.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                deleteFiles();
            }
        });
        return delete;
    }

    public SForm createUpload() {
        SForm p = new SForm();
        p.setEncodingType("multipart/form-data");
        final SFileChooser chooser = new SFileChooser();
        p.add(chooser);
        SButton submit = new SButton("upload");
        p.add(submit);
        p.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    writeFile(chooser.getSelectedFile(), chooser.getFileName());
                } catch (IOException uploadProblem) {
                }
            }
        });
        return p;
    }

    /**
     *
     */
    private void writeFile(File file, String fileName) {
        try {
            FileInputStream fin = new FileInputStream(file);
            FileOutputStream fout = new FileOutputStream(dirTableModel.getDirectory().getAbsolutePath() + File.separator + fileName);
            int val;
            while ((val = fin.read()) != -1) fout.write(val);
            fin.close();
            fout.close();
            dirTableModel.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected SComponent createTree() {
        explorerTree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                TreePath tpath = e.getNewLeadSelectionPath();
                if (tpath != null) {
                    File newDir = (File) tpath.getLastPathComponent();
                    dirTableModel.setDirectory(newDir);
                    currentDirectory.setText(newDir.toString());
                } else {
                    dirTableModel.setDirectory(null);
                    currentDirectory.setText("- no dir -");
                }
            }
        });
        explorerTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        return explorerTree;
    }

    protected TreeModel createDirectoryModel(String dir) {
        try {
            return new DirectoryTreeModel(new File(dir));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
