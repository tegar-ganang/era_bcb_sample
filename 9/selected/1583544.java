package com.prolix.editor.graph.templates.commands.detailelements;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import com.prolix.editor.graph.editparts.MainEditPart;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.graph.model.points.ModelSyncPoint;
import com.prolix.editor.graph.templates.commands.GraphicalTemplateCommand;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;

/**
 * <<class description>>
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public class GraphicalTemplateCreateSynPoint extends Command implements GraphicalTemplateGetGeneratetElement {

    private ModelSyncPoint syncPoint;

    private GraphicalTemplateCommand parent;

    private Point location;

    public GraphicalTemplateCreateSynPoint(GraphicalTemplateCommand parent, Point move) {
        this.parent = parent;
        location = parent.getConstraints().getLocation().getCopy().translate(move);
    }

    public Object getGeneratedElement() {
        return syncPoint;
    }

    public boolean canExecute() {
        return true;
    }

    public void execute() {
        syncPoint = new ModelSyncPoint();
        syncPoint.setParent(parent.getParent());
        syncPoint.setLearningDesign(parent.getParent().getLearningDesign());
        syncPoint.setLocation(location);
        syncPoint.executeGen(false);
        parent.getParent().addChild(syncPoint);
        parent.getParent().updatePosElements();
        ModelDiagramMain maindiag = parent.getParent();
        LearningDesignDataModel lddm = (LearningDesignDataModel) maindiag.getLearningDesign().getDataModel();
        GraphicalViewer viewer = lddm.getEditor().getGraphicalViewer();
        MainEditPart mainEditPart = (MainEditPart) viewer.getEditPartRegistry().get(maindiag);
        EditPart part = mainEditPart.findEditPartForModel(this.syncPoint);
        viewer.appendSelection(part);
    }

    public void redo() {
        execute();
    }

    public void undo() {
        syncPoint.performDelete();
        syncPoint.getParent().removeChild(syncPoint);
        syncPoint = null;
    }
}
