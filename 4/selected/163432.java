package com.javable.cese.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import com.javable.cese.CESE;
import com.javable.cese.ModelManager;
import com.javable.cese.ResourceManager;
import com.javable.utils.ActionSupport;
import com.javable.utils.ExceptionDialog;
import com.javable.utils.ExtensionFileFilter;

public class SaveParamsAction extends ActionSupport implements java.beans.PropertyChangeListener {

    private File dir, file;

    private ModelManager manager;

    /** Creates new SaveParamsAction */
    public SaveParamsAction(ModelManager m) {
        this();
        manager = m;
        manager.addPropertyChangeListener(this);
    }

    /** Creates new SaveParamsAction */
    public SaveParamsAction() {
        super(ResourceManager.getResource("Save_Parameters..."), ResourceManager.getResource("Save_parameters"), ResourceManager.getResource("save_params_image"));
        dir = new File(this.getClass().getResource(ResourceManager.getResource("params_dir")).getFile());
    }

    public void actionPerformed(ActionEvent evt) {
        ObjectOutputStream out = null;
        try {
            JFileChooser filechooser = new JFileChooser(dir);
            filechooser.setDialogTitle(ResourceManager.getResource("Save_Model_Parameters"));
            ExtensionFileFilter mdlFilter = new ExtensionFileFilter(ResourceManager.getResource("mdl_extension"), ResourceManager.getResource("Model_Parameters_Files"));
            filechooser.addChoosableFileFilter(mdlFilter);
            ExtensionFileFilter txtFilter = new ExtensionFileFilter(ResourceManager.getResource("txt_extension"), ResourceManager.getResource("Parameters_Text_Files"));
            filechooser.addChoosableFileFilter(txtFilter);
            filechooser.setFileFilter(mdlFilter);
            int retVal = filechooser.showSaveDialog(CESE.getEnvironment());
            if (retVal == JFileChooser.APPROVE_OPTION) {
                file = filechooser.getSelectedFile();
                dir = filechooser.getCurrentDirectory();
                if (file.getPath() == null) return;
                ExtensionFileFilter filter = (ExtensionFileFilter) filechooser.getFileFilter();
                if (filter.getExtension(file) == null) file = new java.io.File(file.getAbsolutePath() + "." + filter.getExtension());
                if (file.exists()) {
                    int n = JOptionPane.showConfirmDialog(CESE.getEnvironment(), ResourceManager.getResource("Overwrite_file_") + file.getPath() + " ?", ResourceManager.getResource("File_already_exists"), JOptionPane.YES_NO_OPTION);
                    if (n == JOptionPane.NO_OPTION) return;
                }
                if (filter.getExtension(file).equals(ResourceManager.getResource("mdl_extension"))) {
                    out = new ObjectOutputStream(new FileOutputStream(file.getPath()));
                    out.writeObject(manager.getModel());
                    manager.setTabName(file.getName());
                } else {
                    manager.exportParametersASCII(file.getPath());
                }
            }
        } catch (Exception e) {
            ExceptionDialog.showExceptionDialog(ResourceManager.getResource("Error"), ResourceManager.getResource("Error_saving_parameters_file."), e);
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ex) {
            }
        }
    }

    public void propertyChange(java.beans.PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("model_control")) this.setEnabled(((Boolean) evt.getNewValue()).booleanValue());
    }
}
