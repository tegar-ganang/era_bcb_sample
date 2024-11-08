package jmri.configurexml;

import jmri.InstanceManager;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Store the JMRI configuration information as XML.
 * <P>
 * Note that this does not store preferences, tools or user information
 * in the file.  This is not a complete store!
 * See {@link jmri.ConfigureManager} for information on the various
 * types of information stored in configuration files.
 *
 * @author	Bob Jacobsen   Copyright (C) 2002
 * @version	$Revision: 1.16 $
 * @see         jmri.jmrit.XmlFile
 */
public class StoreXmlConfigAction extends LoadStoreBaseAction {

    public StoreXmlConfigAction() {
        this("Store configuration ...");
    }

    public StoreXmlConfigAction(String s) {
        super(s);
    }

    public static File getFileName(JFileChooser fileChooser) {
        fileChooser.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        return getFileCustom(fileChooser);
    }

    /**
     * Do the filename handling:
     *<OL>
     *<LI>rescan directory to see any new files
     *<LI>Prompt user to select a file
     *<LI>adds .xml extension if needed
     *<LI>if that file exists, check with user
     *</OL>
     * Returns null if selection failed for any reason
     */
    public static File getFileCustom(JFileChooser fileChooser) {
        fileChooser.rescanCurrentDirectory();
        int retVal = fileChooser.showDialog(null, null);
        if (retVal != JFileChooser.APPROVE_OPTION) return null;
        File file = fileChooser.getSelectedFile();
        if (fileChooser.getFileFilter() != fileChooser.getAcceptAllFileFilter()) {
            String fileName = file.getAbsolutePath();
            String fileNameLC = fileName.toLowerCase();
            if (!fileNameLC.endsWith(".xml")) {
                fileName = fileName + ".xml";
                file = new File(fileName);
            }
        }
        if (log.isDebugEnabled()) log.debug("Save file: " + file.getPath());
        if (file.exists()) {
            int selectedValue = JOptionPane.showConfirmDialog(null, "File " + file.getName() + " already exists, overwrite it?", "Overwrite file?", JOptionPane.OK_CANCEL_OPTION);
            if (selectedValue != JOptionPane.OK_OPTION) return null;
        }
        return file;
    }

    public void actionPerformed(ActionEvent e) {
        File file = getFileName(configFileChooser);
        if (file == null) return;
        InstanceManager.configureManagerInstance().storeConfig(file);
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(StoreXmlConfigAction.class.getName());
}
