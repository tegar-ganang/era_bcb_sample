package org.xaware.ide.shared;

import java.io.File;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Button;
import org.xaware.ide.xadev.XA_Designer_Plugin;
import org.xaware.ide.xadev.common.ControlFactory;
import org.xaware.ide.xadev.common.ResourceUtils;
import org.xaware.ide.xadev.gui.FileChooserWithLabeling;
import org.xaware.shared.i18n.Translator;

/**
 * Utility class that provides methods commonly used by the wizards.
 * 
 */
public class WizardUtil {

    /** Translator used for Localization */
    public static final Translator translator = XA_Designer_Plugin.getTranslator();

    /**
	 * Check if the BizDriver file name entered by the user is valid.
	 * 
	 * @param fileChooser
	 *            Component used to browse files.
	 * @param createNewBtn
	 *            Radio button to create new BizDriver file.
	 * @param useExistingBtn
	 *            Radio button to use existing BizDriver file.
	 * @return true if valid, else false.
	 */
    public static boolean isValidBizDriverFileName(FileChooserWithLabeling fileChooser, Button createNewBtn, Button useExistingBtn) {
        String fname = fileChooser.getFileString().trim();
        fileChooser.setFileString(fname);
        if (fname.equals("")) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"));
            return false;
        }
        File bizDriverFile = ResourceUtils.getFile(fname);
        if (bizDriverFile.isDirectory()) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"));
            return false;
        }
        if (bizDriverFile.getName().trim().equals(translator.getString(".xdr"))) {
            ControlFactory.showMessageDialog(translator.getString("Enter BizDriver file name."), translator.getString("Information"));
            return false;
        }
        if ((bizDriverFile.getAbsolutePath().indexOf('#') > -1) || (bizDriverFile.getAbsolutePath().indexOf('%') > -1)) {
            ControlFactory.showMessageDialog(translator.getString("Please do not enter # or % in the file path or name."), translator.getString("Information"));
            return false;
        }
        String dirName = bizDriverFile.getParent();
        if ((dirName == null) || dirName.trim().equals("")) {
            dirName = XA_Designer_Plugin.getActiveEditedFileDirectory();
            if (dirName.charAt(dirName.length() - 1) != File.separatorChar) {
                dirName = dirName + File.separator;
            }
            fileChooser.setFileString(dirName + fileChooser.getFileString());
        } else if (dirName.trim().equals(File.separator)) {
            dirName = XA_Designer_Plugin.getActiveEditedFileDirectory();
            fileChooser.setFileString(dirName.substring(0, dirName.length() - 1) + fileChooser.getFileString());
        } else {
            dirName = dirName.trim();
            if (dirName.charAt(dirName.length() - 1) == File.separatorChar) {
                fileChooser.setFileString(dirName + bizDriverFile.getName().trim());
            } else {
                fileChooser.setFileString(fname);
            }
        }
        final File tmpFile = new File(dirName);
        if (!tmpFile.isDirectory()) {
            ControlFactory.showMessageDialog(translator.getString("Invalid directory for BizDriver file."), translator.getString("Information"));
            return false;
        }
        fname = fileChooser.getFileString().trim();
        bizDriverFile = ResourceUtils.getFile(fname);
        if (createNewBtn.getSelection()) {
            final String fileNameStr = bizDriverFile.getName();
            if (fileNameStr.indexOf('.') == -1) {
                fname = fname + ".xdr";
                fileChooser.setFileString(fname);
                bizDriverFile = new File(fname);
            } else if (fileNameStr.indexOf('.') == (fileNameStr.length() - 1)) {
                fname = fname + "xdr";
                fileChooser.setFileString(fname);
                bizDriverFile = new File(fname);
            }
            if (bizDriverFile.exists()) {
                final int choice = ControlFactory.showConfirmDialog(translator.getString("BizDriver file which you have entered already exists. Overwrite?"));
                if (choice != Window.OK) {
                    return false;
                }
            }
        } else if (useExistingBtn.getSelection()) {
            if (bizDriverFile.isDirectory() || !bizDriverFile.exists()) {
                ControlFactory.showMessageDialog(translator.getString("BizDriver file which you have entered does not exist. Please enter correct file name."), translator.getString("Information"));
                return false;
            }
        }
        return true;
    }
}
