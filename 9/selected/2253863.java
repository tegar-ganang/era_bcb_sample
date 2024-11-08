package com.prolix.editor.graph.templates.commands.detailelements;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import com.prolix.editor.graph.editparts.MainEditPart;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.graph.model.activities.ModelDiagramActivityLearning;
import com.prolix.editor.graph.templates.commands.GraphicalTemplateCommand;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;

/**
 * <<class description>>
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public class GraphicalTemplateCreateLearningActivity extends Command implements GraphicalTemplateGetGeneratetElement {

    private ModelDiagramActivityLearning activityLearning;

    private GraphicalTemplateCommand parent;

    private Point location;

    private int width;

    private String name;

    public GraphicalTemplateCreateLearningActivity(GraphicalTemplateCommand parent, String name, Point move, int width) {
        this.parent = parent;
        location = parent.getConstraints().getLocation().getCopy();
        if (move != null) location.translate(move);
        this.name = name;
        this.width = width;
    }

    public Object getGeneratedElement() {
        return activityLearning;
    }

    public boolean canExecute() {
        return true;
    }

    public void execute() {
        activityLearning = new ModelDiagramActivityLearning();
        activityLearning.setParent(parent.getParent());
        activityLearning.setLearningDesign(parent.getParent().getLearningDesign());
        activityLearning.setLocation(location);
        if (width > activityLearning.getMinimumWidth()) activityLearning.getSize().width = width;
        activityLearning.executeGen(false);
        parent.getParent().addChild(activityLearning);
        parent.getParent().updatePosElements();
        activityLearning.setName(name);
        ModelDiagramMain maindiag = parent.getParent();
        LearningDesignDataModel lddm = (LearningDesignDataModel) maindiag.getLearningDesign().getDataModel();
        GraphicalViewer viewer = lddm.getEditor().getGraphicalViewer();
        MainEditPart mainEditPart = (MainEditPart) viewer.getEditPartRegistry().get(maindiag);
        EditPart part = mainEditPart.findEditPartForModel(activityLearning);
        viewer.appendSelection(part);
    }

    public void redo() {
        execute();
    }

    public void undo() {
        activityLearning.performDelete();
        activityLearning = null;
    }
}
