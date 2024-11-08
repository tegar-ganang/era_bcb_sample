package com.prolix.editor.graph.templates.commands.detailelements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import uk.ac.reload.straker.datamodel.learningdesign.components.activities.SupportActivity;
import com.prolix.editor.graph.editparts.MainEditPart;
import com.prolix.editor.graph.model.ModelDiagramMain;
import com.prolix.editor.graph.model.activities.ModelDiagramActivitySupport;
import com.prolix.editor.graph.templates.commands.GraphicalTemplateCommand;
import com.prolix.editor.resourcemanager.zip.LearningDesignDataModel;
import com.prolix.editor.roleview.roles.RoleRole;

/**
 * <<class description>>
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public class GraphicalTemplateCreateSupportActivity extends Command implements GraphicalTemplateGetGeneratetElement {

    private ModelDiagramActivitySupport activitySupport;

    private GraphicalTemplateCommand parent;

    private Point location;

    private int width;

    private String name;

    private List supportetRoles;

    public GraphicalTemplateCreateSupportActivity(GraphicalTemplateCommand parent, String name, Point move, int width) {
        this.parent = parent;
        location = parent.getConstraints().getLocation().getCopy();
        if (move != null) location.translate(move);
        this.name = name;
        this.supportetRoles = new ArrayList();
        this.width = width;
    }

    public Object getGeneratedElement() {
        return activitySupport;
    }

    public void addSupportetRole(RoleRole role) {
        supportetRoles.add(role);
    }

    public void addSupportetRole(GraphicalTemplateGetGeneratetElement role) {
        supportetRoles.add(role);
    }

    public boolean canExecute() {
        return true;
    }

    public void execute() {
        activitySupport = new ModelDiagramActivitySupport();
        activitySupport.setParent(parent.getParent());
        activitySupport.setLearningDesign(parent.getParent().getLearningDesign());
        activitySupport.setLocation(location);
        if (width > activitySupport.getMinimumWidth()) activitySupport.getSize().width = width;
        activitySupport.executeGen(false);
        parent.getParent().addChild(activitySupport);
        parent.getParent().updatePosElements();
        activitySupport.setName(name);
        Iterator it = supportetRoles.iterator();
        while (it.hasNext()) ((SupportActivity) activitySupport.getDataComponent()).addRoleRef(getRoleIdentifier(it.next()));
        ModelDiagramMain maindiag = parent.getParent();
        LearningDesignDataModel lddm = (LearningDesignDataModel) maindiag.getLearningDesign().getDataModel();
        GraphicalViewer viewer = lddm.getEditor().getGraphicalViewer();
        MainEditPart mainEditPart = (MainEditPart) viewer.getEditPartRegistry().get(maindiag);
        EditPart part = mainEditPart.findEditPartForModel(activitySupport);
        viewer.appendSelection(part);
    }

    private String getRoleIdentifier(Object role) {
        if (role instanceof RoleRole) return ((RoleRole) role).getData().getIdentifier();
        if (role instanceof GraphicalTemplateGetGeneratetElement) return ((RoleRole) ((GraphicalTemplateGetGeneratetElement) role).getGeneratedElement()).getData().getIdentifier();
        throw new IllegalArgumentException();
    }

    public void redo() {
        execute();
    }

    public void undo() {
        activitySupport.performDelete();
        activitySupport = null;
    }
}
