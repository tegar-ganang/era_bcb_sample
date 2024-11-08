package org.genie.cmof.gef.editparts;

import java.beans.PropertyChangeEvent;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.TextCellEditor;
import org.genie.gef.directedit.ColumnNameTypeCellEditorValidator;
import org.genie.gef.directedit.ExtendedDirectEditManager;
import org.genie.gef.directedit.LabelCellEditorLocator;
import org.genie.gef.directedit.ValidationMessageHandler;
import org.genie.gef.editor.ValidationEnabledGraphicalViewer;
import org.genie.gef.figure.EditableLabel;
import org.genie.gef.model.IGraphicalPart;
import org.genie.gef.part.PropertyAwarePart;
import org.genie.gef.policy.ColumnDirectEditPolicy;
import org.genie.gef.policy.ColumnEditPolicy;
import org.xmi.infoset.ext.Shape;
import org.xmi.uml.model.Property;

public class ClassElementPropertyPart extends PropertyAwarePart implements IGraphicalPart {

    protected DirectEditManager manager;

    protected IFigure createFigure() {
        Property column = (Property) getModel();
        String label = column.getName();
        EditableLabel columnLabel = new EditableLabel(label);
        return columnLabel;
    }

    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new ColumnEditPolicy());
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new ColumnDirectEditPolicy());
        installEditPolicy(EditPolicy.LAYOUT_ROLE, null);
    }

    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
            if (request instanceof DirectEditRequest && !directEditHitTest(((DirectEditRequest) request).getLocation().getCopy())) return;
            performDirectEdit();
        }
    }

    private boolean directEditHitTest(Point requestLoc) {
        IFigure figure = getFigure();
        figure.translateToRelative(requestLoc);
        if (figure.containsPoint(requestLoc)) return true;
        return false;
    }

    protected void performDirectEdit() {
        if (manager == null) {
            ValidationEnabledGraphicalViewer viewer = (ValidationEnabledGraphicalViewer) getViewer();
            ValidationMessageHandler handler = viewer.getValidationHandler();
            Label l = (Label) getFigure();
            ColumnNameTypeCellEditorValidator columnNameTypeCellEditorValidator = new ColumnNameTypeCellEditorValidator(handler);
            manager = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(l), l, columnNameTypeCellEditorValidator);
        }
        manager.show();
    }

    /**
	 * Sets the width of the line when selected
	 */
    public void setSelected(int value) {
        super.setSelected(value);
        EditableLabel columnLabel = (EditableLabel) getFigure();
        if (value != EditPart.SELECTED_NONE) {
            columnLabel.setSelected(true);
        } else {
            columnLabel.setSelected(false);
        }
        columnLabel.repaint();
    }

    /**
	 * Handles name change during direct edit
	 * @param textValue
	 */
    public void handleNameChange(String textValue) {
        EditableLabel label = (EditableLabel) getFigure();
        label.setVisible(false);
        setSelected(EditPart.SELECTED_NONE);
        label.revalidate();
    }

    /**
	 * Handles when successfully applying direct edit
	 */
    protected void commitNameChange(PropertyChangeEvent evt) {
    }

    public void revertNameChange(String oldValue) {
        EditableLabel label = (EditableLabel) getFigure();
        label.setVisible(true);
        setSelected(EditPart.SELECTED_PRIMARY);
        label.revalidate();
    }

    protected void refreshVisuals() {
        Property column = (Property) getModel();
        EditableLabel columnLabel = (EditableLabel) getFigure();
        columnLabel.setText(column.getName());
    }

    public Shape getElement() {
        return (Shape) getModel();
    }
}
