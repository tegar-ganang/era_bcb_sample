package org.gvt.action;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.gvt.ChisioMain;
import java.io.File;

/**
 * @author Ozgun Babur
 *
 * Copyright: Bilkent Center for Bioinformatics, 2007 - present
 */
public class SaveAsBioPAXFileAction extends Action {

    private ChisioMain main;

    private boolean saved;

    /**
	 * Constructor
	 *
	 * @param chisio
	 */
    public SaveAsBioPAXFileAction(ChisioMain chisio) {
        super("Save As ...");
        setToolTipText(getText());
        setImageDescriptor(ImageDescriptor.createFromFile(ChisioMain.class, "icon/save-as.png"));
        this.main = chisio;
    }

    public void run() {
        this.saved = false;
        if (main.getOwlModel() == null) {
            return;
        }
        String fileName = null;
        boolean done = false;
        while (!done) {
            FileDialog fileChooser = new FileDialog(main.getShell(), SWT.SAVE);
            String currentFilename = main.getOwlFileName();
            if (currentFilename != null) {
                if (!currentFilename.endsWith(".owl")) {
                    if (currentFilename.indexOf(".") > 0) {
                        currentFilename = currentFilename.substring(0, currentFilename.lastIndexOf("."));
                    }
                    currentFilename += ".owl";
                }
                fileChooser.setFileName(currentFilename);
            }
            String[] filterExtensions = new String[] { "*.owl" };
            String[] filterNames = new String[] { "BioPAX (*.owl)" };
            fileChooser.setFilterExtensions(filterExtensions);
            fileChooser.setFilterNames(filterNames);
            fileName = fileChooser.open();
            if (fileName == null) {
                done = true;
            } else {
                File file = new File(fileName);
                if (file.exists()) {
                    MessageBox mb = new MessageBox(fileChooser.getParent(), SWT.ICON_WARNING | SWT.YES | SWT.NO);
                    mb.setMessage(fileName + " already exists. Do you want to overwrite?");
                    mb.setText("Confirm Replace File");
                    done = mb.open() == SWT.YES;
                } else {
                    done = true;
                }
            }
        }
        if (fileName == null) {
            return;
        }
        SaveBioPAXFileAction action = new SaveBioPAXFileAction(main, fileName);
        action.run();
        this.saved = action.isSaved();
    }

    public boolean isSaved() {
        return saved;
    }
}
