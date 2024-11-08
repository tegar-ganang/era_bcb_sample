package jadaedor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author Cris
 */
public class EditorUtil {

    public static void updateTree(DefaultMutableTreeNode node, File source) {
        if (source.isHidden()) ; else if (source.isDirectory()) {
            for (File sel : source.listFiles()) {
                if (sel.isDirectory()) {
                    if (!sel.getName().startsWith(".")) {
                        DefaultMutableTreeNode inner = new DefaultMutableTreeNode(sel.getName());
                        node.add(inner);
                        updateTree(inner, new File(source.getPath() + "/" + sel.getName()));
                    }
                } else {
                    node.add(new DefaultMutableTreeNode(JadaProjectObj.createProjectObj(source)));
                }
            }
        } else {
            DefaultMutableTreeNode temp = new DefaultMutableTreeNode(JadaProjectObj.createProjectObj(source));
            node.add(temp);
        }
    }

    public static void initTree(DefaultMutableTreeNode node) {
        File workspace = new File("./workspace");
        if (node.isRoot()) {
            for (File sel : workspace.listFiles()) {
                if (sel.isHidden()) ; else if (sel.isDirectory()) {
                    if (!sel.getName().startsWith(".")) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(sel.getName());
                        node.add(temp);
                        initTree(temp);
                    }
                }
            }
        } else {
            String path = "./";
            for (Object sele : node.getUserObjectPath()) {
                path += sele + "/";
            }
            File current = new File(path);
            for (File sel : current.listFiles()) {
                if (sel.isHidden()) ; else if (sel.isDirectory()) {
                    if (!sel.getName().startsWith(".")) {
                        DefaultMutableTreeNode temp = new DefaultMutableTreeNode(sel.getName());
                        node.add(temp);
                        initTree(temp);
                    }
                } else {
                    DefaultMutableTreeNode tempLeaf = new DefaultMutableTreeNode(JadaProjectObj.createProjectObj(sel));
                    node.add(tempLeaf);
                }
            }
        }
    }

    public static void copyTo(File source, File dest) {
        if (source.isHidden()) ; else if (source.isDirectory()) {
            File temp = new File(dest.getPath() + "/" + source.getName());
            temp.mkdir();
            for (File sel : source.listFiles()) copyTo(sel, temp);
        } else {
            try {
                File tempDest = new File(dest.getPath() + "/" + source.getName());
                tempDest.createNewFile();
                FileChannel sourceCh = new FileInputStream(source).getChannel();
                FileChannel destCh = new FileOutputStream(tempDest).getChannel();
                sourceCh.transferTo(0, sourceCh.size(), destCh);
                sourceCh.close();
                destCh.close();
            } catch (IOException ex) {
                Logger.getLogger(EditorUtil.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void deleteFile(File file) {
        if (file.isDirectory()) {
            for (File sel : file.listFiles()) deleteFile(sel);
            file.delete();
        } else file.delete();
    }

    public static File getFileFromNode(DefaultMutableTreeNode node) {
        String path = "./";
        for (Object sele : node.getUserObjectPath()) {
            path += sele + "/";
        }
        return new File(path);
    }
}
