package org.gvt.action;

import org.biopax.paxtools.io.BioPAXIOHandler;
import org.biopax.paxtools.io.SimpleIOHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.gvt.ChisioMain;
import org.gvt.model.BioPAXGraph;
import java.io.FileOutputStream;

/**
 * @author Ozgun Babur
 *
 * Copyright: Bilkent Center for Bioinformatics, 2007 - present
 */
public class SaveBioPAXFileAction extends Action {

    private ChisioMain main;

    private String filename;

    private boolean saved;

    /**
	 * Constructor
	 *
	 * @param chisio
	 */
    public SaveBioPAXFileAction(ChisioMain chisio) {
        super("Save");
        setToolTipText(getText());
        setImageDescriptor(ImageDescriptor.createFromFile(ChisioMain.class, "icon/save.png"));
        this.main = chisio;
    }

    public SaveBioPAXFileAction(ChisioMain main, String filename) {
        this(main);
        this.filename = filename;
    }

    public void run() {
        this.saved = false;
        if (main.getOwlModel() == null) {
            return;
        }
        if (filename == null) {
            filename = main.getOwlFileName();
        }
        if (filename == null) {
            SaveAsBioPAXFileAction action = new SaveAsBioPAXFileAction(main);
            action.run();
            this.saved = action.isSaved();
            return;
        }
        try {
            main.lockWithMessage("Saving ...");
            for (ScrollingGraphicalViewer viewer : main.getTabToViewerMap().values()) {
                Object o = viewer.getContents().getModel();
                if (o instanceof BioPAXGraph) {
                    BioPAXGraph graph = (BioPAXGraph) o;
                    if (graph.isMechanistic()) {
                        graph.recordLayout();
                    }
                }
            }
            BioPAXIOHandler exporter = new SimpleIOHandler(main.getOwlModel().getLevel());
            FileOutputStream stream = new FileOutputStream(filename);
            exporter.convertToOWL(main.getOwlModel(), stream);
            stream.close();
            main.setOwlFileName(filename);
            main.markSaved();
            this.saved = true;
        } catch (Exception e) {
            e.printStackTrace();
            MessageDialog.openError(main.getShell(), ChisioMain.TOOL_NAME, "File cannot be saved!\n" + e.getMessage());
        } finally {
            main.unlock();
            filename = null;
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
