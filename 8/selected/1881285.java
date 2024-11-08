package com.prolix.editor.graph.commands.connection;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.commands.Command;
import com.prolix.editor.graph.editparts.connections.ConnectionEditPart;
import com.prolix.editor.graph.model.connections.ModelConnection;
import com.prolix.editor.main.workspace.prolix.mainEditor.ProlixScrollingGraphicalViewer;

public class BendPointCommand extends Command {

    protected int index;

    protected Point location;

    protected ModelConnection connection;

    protected ConnectionEditPart editPart;

    protected int getIndex() {
        return index;
    }

    protected Point getLocation() {
        return location;
    }

    protected ModelConnection getConnection() {
        return connection;
    }

    public void redo() {
        execute();
    }

    public void setIndex(int i) {
        index = i;
    }

    public void setLocation(Point p) {
        ProlixScrollingGraphicalViewer test = ((ProlixScrollingGraphicalViewer) editPart.getViewer());
        location = test.getEditor().translateLocationScrollbar(p);
    }

    protected void setConnection(ModelConnection w) {
        connection = w;
    }

    /**
	 * @return the editPart
	 */
    public ConnectionEditPart getEditPart() {
        return editPart;
    }

    /**
	 * @param editPart
	 *           the editPart to set
	 */
    public void setEditPart(ConnectionEditPart editPart) {
        this.editPart = editPart;
        setConnection(editPart.getModelConnection());
    }
}
