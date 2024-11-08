package com.ecmdeveloper.plugin.diagrams.actions;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.UnexecutableCommand;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IWorkbenchPart;
import com.ecmdeveloper.plugin.core.model.IClassDescription;
import com.ecmdeveloper.plugin.diagrams.editors.ClassDiagramClassFactory;
import com.ecmdeveloper.plugin.diagrams.editors.ClassDiagramEditor;
import com.ecmdeveloper.plugin.diagrams.parts.ClassDiagramEditPart;

/**
 * @author Ricardo.Belfor
 *
 */
public class AddClassDiagramClassAction extends SelectionAction {

    public static final String ID = "com.ecmdeveloper.plugin.diagrams.actions.addClassDiagramClassAction";

    public static final String REQUEST_TYPE = "addClassDiagramClass";

    private static final String ACTION_NAME = "Add Class";

    private IClassDescription classDescription;

    public AddClassDiagramClassAction(IWorkbenchPart part) {
        super(part);
        setId(ID);
        setText(ACTION_NAME);
    }

    public void setClassDescription(IClassDescription classDescription) {
        this.classDescription = classDescription;
    }

    @Override
    protected boolean calculateEnabled() {
        return true;
    }

    private Command createClassDiagramClassCreateCommand() {
        CreateRequest createRequest = new CreateRequest(REQUEST_TYPE);
        createRequest.setFactory(new ClassDiagramClassFactory(classDescription));
        createRequest.setLocation(new Point(20, 20));
        GraphicalViewer graphicalViewer = ((ClassDiagramEditor) getWorkbenchPart()).getViewer();
        ScalableFreeformRootEditPart rootEditPart = (ScalableFreeformRootEditPart) graphicalViewer.getRootEditPart();
        if (!rootEditPart.getChildren().isEmpty() && rootEditPart.getChildren().get(0) instanceof ClassDiagramEditPart) {
            EditPart editPart = (EditPart) rootEditPart.getChildren().get(0);
            return editPart.getCommand(createRequest);
        } else {
            return UnexecutableCommand.INSTANCE;
        }
    }

    @Override
    public void run() {
        execute(createClassDiagramClassCreateCommand());
    }
}
