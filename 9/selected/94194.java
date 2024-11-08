package com.prolix.editor.commands.roles;

import java.util.Iterator;
import java.util.List;
import org.eclipse.gef.GraphicalViewer;
import com.prolix.editor.graph.commands.ChainCommand;
import com.prolix.editor.graph.commands.ChangeRoleCommand;
import com.prolix.editor.graph.editparts.activities.ActivityEditPart;
import com.prolix.editor.graph.model.activities.ModelDiagramActivity;
import com.prolix.editor.roleview.roles.RoleRole;

/**
 * <<class description>>
 * 
 * @author Susanne Neumann, Stefan Zander, Philipp Prenner
 */
public class AssignRoleToSelectedActivitiesCommand extends ChainCommand {

    private RoleRole role;

    /**
	 * 
	 */
    public AssignRoleToSelectedActivitiesCommand(RoleRole role) {
        super();
        this.role = role;
        if (role == null) return;
        setLabel("Assign Role: " + role.getName() + " to selected Activities");
        buildCommandChain();
    }

    private void buildCommandChain() {
        GraphicalViewer gefeditor = role.getLearningDesignDataModel().getEditor().getGraphicalViewer();
        List editparts = gefeditor.getSelectedEditParts();
        Iterator it = editparts.iterator();
        while (it.hasNext()) {
            Object editpart = it.next();
            if (editpart instanceof ActivityEditPart) {
                ModelDiagramActivity activity = ((ActivityEditPart) editpart).getModelPlayActivity();
                ChangeRoleCommand command = new ChangeRoleCommand();
                command.set_role(role);
                command.setElement(activity);
                addCommand(command);
            }
        }
    }

    public boolean canExecute() {
        if (role == null) return false;
        return super.canExecute();
    }
}
