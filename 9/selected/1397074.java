package org.plazmaforge.studio.dbdesigner.actions;

import java.util.Iterator;
import java.util.List;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPart;
import org.plazmaforge.studio.dbdesigner.commands.SetConstraintCommand;
import org.plazmaforge.studio.dbdesigner.editor.ERDesignerEditor;
import org.plazmaforge.studio.dbdesigner.model.ERDiagram;
import org.plazmaforge.studio.dbdesigner.model.ERTableNode;

public class GridLayoutAction extends WorkbenchPartAction {

    public GridLayoutAction(IWorkbenchPart iworkbenchpart) {
        super(iworkbenchpart);
        editor = (ERDesignerEditor) iworkbenchpart;
        setId("Grid Layout");
        layoutMode = 0;
    }

    protected boolean calculateEnabled() {
        return true;
    }

    public void run() {
        if (MessageDialog.openQuestion(null, "Messages.GridLayoutAction_dialog_warning_relayout_title", "Messages.GridLayoutAction_dialog_warning_relayout_message")) {
            CompoundCommand compoundcommand = createClearLayoutCommand();
            compoundcommand.add(createLayoutCommand());
            execute(compoundcommand);
        }
    }

    protected CompoundCommand createClearLayoutCommand() {
        ERDiagram erdiagram = editor.getDiagram();
        List list = erdiagram.getChildren();
        CompoundCommand compoundcommand = new CompoundCommand();
        SetConstraintCommand setconstraintcommand;
        for (Iterator iterator = list.iterator(); iterator.hasNext(); compoundcommand.add(setconstraintcommand)) {
            ERTableNode ertablenode = (ERTableNode) iterator.next();
            setconstraintcommand = new SetConstraintCommand();
            setconstraintcommand.setPart(ertablenode);
            setconstraintcommand.setLocation((Point) null);
            setconstraintcommand.setSize(null);
        }
        return compoundcommand;
    }

    protected Command createLayoutCommand() {
        return new Command() {

            public void execute() {
                editor.setRelayout(true);
                editor.setLayoutMode(layoutMode);
                ((GraphicalEditPart) editor.getGraphicalViewer().getContents()).getFigure().revalidate();
            }
        };
    }

    public static final String GRID_LAYOUT = "Grid Layout";

    protected ERDesignerEditor editor;

    protected int layoutMode;
}
