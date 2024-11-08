package org.isistan.flabot.edit.editor.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.isistan.flabot.edit.componenteditor.ComponentEditor;
import org.isistan.flabot.edit.multipage.FlabotMultiPageEditor;
import org.isistan.flabot.edit.ucmeditor.UCMEditor;
import org.isistan.flabot.messages.Messages;
import org.isistan.flabot.util.ImageSaveUtil;

/**
 * This action is used to print a diagram.
 * 
 * @author $Author: franco $
 *
 */
public class PrintDiagramAction extends SelectionAction {

    private GraphicalViewer viewer;

    private IEditorPart editorPart;

    /**
	 * Creates a new PrintDiagramAction in the given viewer
	 * @param viewer
	 * @param editorPart the editor that contains the diagram to print
	 */
    public PrintDiagramAction(GraphicalViewer viewer, IEditorPart editorPart) {
        super(editorPart);
        this.viewer = viewer;
        this.editorPart = editorPart;
        setText(Messages.getString("org.isistan.flabot.edit.editor.actions.PrintDiagramAction.text"));
        setToolTipText(Messages.getString("org.isistan.flabot.edit.editor.actions.PrintDiagramAction.toolTipText"));
        setId(ActionFactory.PRINT.getId());
        setEnabled(false);
    }

    /**
	 * Determines whether the action should be enabled or not.
	 * @return true
	 */
    @Override
    protected boolean calculateEnabled() {
        return canPerformAction();
    }

    /**
	 * Determines whether the action should be enabled or not.
	 * @return true
	 */
    private boolean canPerformAction() {
        return true;
    }

    /**
	 * Prints the diagram that contains the activeEditor.
	 * ImageSaveUtil is used to print.
	 * 
	 * @see ImageSaveUtil.print()
	 */
    public void run() {
        FlabotMultiPageEditor multiPageEditor = (FlabotMultiPageEditor) getWorkbenchPart().getSite().getPage().getActiveEditor();
        IEditorPart activeEditor = multiPageEditor.getActiveEditor();
        String title = "flabot print job";
        if (activeEditor instanceof ComponentEditor) {
            ComponentEditor componentEditor = (ComponentEditor) activeEditor;
            title = Messages.getString("org.isistan.flabot.edit.editor.actions.PrintDiagramAction.componentDiagram") + componentEditor.getModel().getName();
        } else if (activeEditor instanceof UCMEditor) {
            UCMEditor ucmEditor = (UCMEditor) activeEditor;
            title = Messages.getString("org.isistan.flabot.edit.editor.actions.PrintDiagramAction.ucmDiagram") + ucmEditor.getModel().getName();
        }
        ImageSaveUtil.print(editorPart, viewer, title);
    }
}
