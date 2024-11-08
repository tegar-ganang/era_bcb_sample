package org.isistan.flabot.edit.editor;

import java.util.List;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jface.dialogs.MessageDialog;
import org.isistan.flabot.edit.editormodel.Diagram;
import org.isistan.flabot.edit.multipage.FlabotMultiPageEditor;
import org.isistan.flabot.edit.multipage.UnsettableDirtyStateEditor;
import org.isistan.flabot.messages.Messages;

/**
 * Superclass for the component editor and the use case map editor that
 * contains their common functionality
 * @author $Author: franco $
 *
 */
public abstract class FlabotGraphicalEditor extends GraphicalEditorWithFlyoutPalette implements CommandExecutor, UnsettableDirtyStateEditor, Adapter {

    public FlabotGraphicalEditor(FlabotMultiPageEditor parentEditor) {
        DefaultEditDomain editDomain = new DefaultEditDomain(this);
        editDomain.setCommandStack(parentEditor.getCommandStack());
        setEditDomain(editDomain);
        this.parentEditor = parentEditor;
    }

    private boolean dirty;

    /** This is the root of the editor's model. */
    private Diagram diagram;

    protected FlabotMultiPageEditor parentEditor;

    public void executeCommand(Command command, boolean askUser) {
        if (command == null || !command.canExecute()) return;
        if (!askUser || askUser(command)) this.getCommandStack().execute(command);
    }

    public FlabotMultiPageEditor getParent() {
        return parentEditor;
    }

    /**
	 * Create a dialog to ask the user if the given command must be executed
	 * or not.
	 * @param command the command that will be executed
	 * @return
	 */
    private boolean askUser(Command command) {
        String message = Messages.getString("org.isistan.flabot.edit.editor.FlabotGraphicalEditor.commandExecutionMessageQuestion", command.getLabel());
        return MessageDialog.openConfirm(getEditorSite().getShell(), Messages.getString("org.isistan.flabot.edit.editor.FlabotGraphicalEditor.commandExecutionMessageTitle"), message);
    }

    @Override
    public boolean isDirty() {
        return dirty || super.isDirty();
    }

    public void unsetDirty() {
        this.dirty = false;
        this.firePropertyChange(PROP_DIRTY);
    }

    @Override
    public Object getAdapter(Class type) {
        if (CommandExecutor.class.equals(type)) return this;
        return super.getAdapter(type);
    }

    public Diagram getModel() {
        return diagram;
    }

    public void setModel(Diagram diagram) {
        if (this.diagram != null) this.diagram.eAdapters().remove(this);
        this.diagram = diagram;
        if (diagram != null) diagram.eAdapters().add(this);
    }

    public Notifier getTarget() {
        return diagram;
    }

    public boolean isAdapterForType(Object type) {
        return (type instanceof Diagram);
    }

    public void notifyChanged(Notification notification) {
        switch(notification.getEventType()) {
            case Notification.SET:
                markDirty();
        }
    }

    private void markDirty() {
        if (!dirty) {
            dirty = true;
            firePropertyChange(PROP_DIRTY);
        }
    }

    /**
	 * Activate the editor for the given diagram. If necessary, a page will 
	 * be created and centers the view to the x, y position.
	 * @param diagram
	 * @return
	 */
    public void openDiagramAtPosition(Diagram diagram, int x, int y) {
        parentEditor.openDiagramAtPosition(diagram, x, y);
    }

    public void arrageViewerToPosition(int x, int y) {
        FigureCanvas control = (FigureCanvas) getGraphicalViewer().getControl();
        control.getViewport().setHorizontalLocation(x - control.getHorizontalBar().getThumb() / 2);
        control.getViewport().setVerticalLocation(y - control.getVerticalBar().getThumb() / 2);
    }

    public void openDiagramAndSelect(Diagram diagram, List selection) {
        parentEditor.openDiagramAndSelect(diagram, selection);
    }

    public void closeDiagram(Diagram diagram) {
        parentEditor.closeDiagram(diagram);
    }

    public void selectEditPartForModel(Object model) {
        EditPart editPart = (EditPart) getGraphicalViewer().getEditPartRegistry().get(model);
        if (editPart != null) getGraphicalViewer().appendSelection(editPart);
    }

    public void setTarget(Notifier newTarget) {
    }
}
