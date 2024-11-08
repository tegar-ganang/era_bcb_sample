package com.explosion.expfmodules.fileutils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import com.explosion.expf.Application;
import com.explosion.expf.ExpActionListener;
import com.explosion.expfmodules.rdbmsconn.dbom.utils.SQLEngine;
import com.explosion.expfmodules.wizard.Wizard;
import com.explosion.expfmodules.wizard.standard.load.WizardDefinitionLoader;
import com.explosion.expfmodules.wizard.standard.view.WizardBasePanel;
import com.explosion.utilities.FileSystemUtils;
import com.explosion.utilities.GeneralUtils;

/**
 * @author Stephen Cowx
 */
public class FileUtilsListener implements ExpActionListener {

    private static Logger log = LogManager.getLogger(SQLEngine.class);

    private HashMap map;

    private int newDocumentNumbers = 1;

    /**
   * Constructor for TextEditorListener.
   */
    public FileUtilsListener() {
        map = new HashMap();
        map.put(FileUtilsConstants.MENU_FILE_SPLIT, FileUtilsConstants.MENU_FILE_SPLIT);
        map.put(FileUtilsConstants.MENU_FILE_UNSPLIT, FileUtilsConstants.MENU_FILE_UNSPLIT);
    }

    /**
   * @see package com.explosion.expf.Interfaces.ExpActionListener#getListensFor()
   */
    public Map getListensFor() {
        return map;
    }

    /**
   * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
   */
    public void actionPerformed(ActionEvent e) {
        try {
            if (e.getActionCommand().equals(FileUtilsConstants.MENU_FILE_SPLIT)) {
                log.debug("Split files");
                Frame frame = Application.getApplicationFrame();
                WizardDefinitionLoader loader = new WizardDefinitionLoader();
                InputStream inputStream = loader.getClass().getClassLoader().getResourceAsStream("com/explosion/expfmodules/fileutils/filesplitter/FileSplitterWizard.xml");
                Wizard wizard = loader.getWizard(new InputStreamReader(inputStream));
                wizard.start();
                JDialog dialog = new JDialog(frame, true);
                WizardBasePanel view = new WizardBasePanel(wizard, dialog);
                dialog.getContentPane().setLayout(new BorderLayout());
                dialog.getContentPane().add(view, BorderLayout.CENTER);
                dialog.setSize(new Dimension(600, 400));
                dialog.setTitle(wizard.getName());
                GeneralUtils.centreWindowInParent(dialog);
                dialog.setVisible(true);
            } else if (e.getActionCommand().equals(FileUtilsConstants.MENU_FILE_UNSPLIT)) {
                File[] files1 = FileSystemUtils.chooseFiles(Application.getApplicationFrame(), FileSystemUtils.OPENTYPE, false, new File(System.getProperty("user.dir")), FileSystemUtils.FILES_ONLY, "Select file1 of two to unsplit.");
                File[] files2 = FileSystemUtils.chooseFiles(Application.getApplicationFrame(), FileSystemUtils.OPENTYPE, false, new File(System.getProperty("user.dir")), FileSystemUtils.FILES_ONLY, "Select file2 to two to unsplit.");
                if (files1 != null && files1.length > 0 && files1[0] != null && files2 != null && files2.length > 0 && files2[0] != null) {
                    File[] files = new File[2];
                    files[0] = files1[0];
                    files[1] = files2[0];
                    unsplit(files[0].getAbsolutePath() + ".joined", files);
                }
            }
        } catch (Exception ex) {
            com.explosion.utilities.exception.ExceptionManagerFactory.getExceptionManager().manageException(ex, "Exception caught while responding to event.");
        }
    }

    /**
     * Unsplits files previously split.
     * @param fileName
     * @throws Exception
     */
    public void unsplit(String newFilename, File[] files) throws Exception {
        FileOutputStream stream = new FileOutputStream(new File(newFilename));
        for (int i = 0; i < files.length; i++) {
            FileInputStream fin = new FileInputStream(files[i].getAbsolutePath());
            DataInputStream din = new DataInputStream(fin);
            while (din.available() > 0) {
                stream.write(din.read());
            }
            din.close();
            fin.close();
        }
        stream.close();
    }
}
