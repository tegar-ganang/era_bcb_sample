package org.sourceforge.zlang.ui;

import java.awt.event.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.*;
import javax.swing.JOptionPane;
import org.sourceforge.zlang.model.IndentedWriter;
import org.sourceforge.zlang.model.ZClass;
import org.sourceforge.zlang.model.ZElement;
import org.sourceforge.zlang.model.ZFile;

/**
 * Changes package for a file.
 *
 * @author <a href="Tim.Lebedkov@web.de">Tim Lebedkov</a>
 * @version $Id: ExportToJavaAction.java,v 1.3 2002/12/04 22:49:02 hilt2 Exp $
 */
public class ExportToJavaAction extends NodeAction {

    /**
     * Constructor for ChangeClassNameAction.
     *
     * @param tree Zlang tree component
     */
    public ExportToJavaAction(ZlangTree tree) {
        super("Export to Java", tree);
    }

    public void update(ZElement el) {
        setEnabled(el instanceof ZFile);
    }

    public void actionPerformed(ZElement el) {
        ZFile c = (ZFile) el;
        File f = c.getFile();
        String n = f.getName();
        n = n.substring(0, n.lastIndexOf('.')) + ".java";
        f = new File(f.getParent(), n);
        if (f.exists()) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.getInstance(), "File " + f + " already exists.\n" + "Would you like to overwrite it?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ret == JOptionPane.NO_OPTION) return;
        }
        try {
            IndentedWriter w = new IndentedWriter(new BufferedWriter(new FileWriter(f)));
            c.printJava(w);
            w.close();
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(MainFrame.getInstance(), "Syntax not yet supported " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(MainFrame.getInstance(), "Error writing file " + f + " " + e.getMessage());
        }
    }
}
