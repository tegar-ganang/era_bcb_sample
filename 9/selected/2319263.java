package org.eclipse.smd.gef.part;

import java.beans.PropertyChangeEvent;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.smd.gef.directedit.ExtendedDirectEditManager;
import org.eclipse.smd.gef.directedit.LabelCellEditorLocator;
import org.eclipse.smd.gef.directedit.ValidationMessageHandler;
import org.eclipse.smd.gef.directedit.validator.SimpleCellEditorValidator;
import org.eclipse.smd.gef.editor.ValidationEnabledGraphicalViewer;
import org.eclipse.smd.gef.figure.EditableLabel;
import org.eclipse.smd.gef.figure.INamedFigure;
import org.eclipse.smd.gef.policy.namedObject.NamedObjectDirectEditPolicy;
import org.eclipse.smd.model.NamedObject;

/**
 * Le controlleur associ� aux objets nomm�.
 * G�re l'�dition en direct du nom et de la description.
 * @author Pierrick HYMBERT (phymbert [at] users.sourceforge.net)
 *         
 *
 */
public abstract class NamedObjectPart extends GraphicalObjectPart implements INamedObjectPart {

    /**
	 * Permet de g�rer l'�dition du nom en direct dans l'�diteur.
	 */
    protected DirectEditManager directEditManagerName;

    /**
	 * Le type d'�dition en cours.
	 */
    private String directEditType;

    /**
	 * Instancie la politique d'�dition pour pour un objet nomm�.
	 */
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new NamedObjectDirectEditPolicy());
    }

    /**
	 * Cache la figure du nom pour permettre l'�dition.
	 * @param value La nouvelle valeur du champ nom.
	 */
    public void handleNameChange(String value) {
        logger.debug(this + " handles name changed.");
        INamedFigure namedFigure = (INamedFigure) getFigure();
        EditableLabel label = namedFigure.getNameLabel();
        if (label != null) {
            label.setVisible(false);
        }
        refreshVisuals();
    }

    /**
	 * Remet le nom existant du mod�le quand on quitte l'�dition en direct.
	 * Rend visible la figure du nom.
	 * Cela peut �tre avant le commitNameChange.
	 */
    public void revertNameChange() {
        logger.debug(this + " reverts name changed.");
        INamedFigure namedFigure = (INamedFigure) getFigure();
        NamedObject namedObject = (NamedObject) getModel();
        namedFigure.setName(namedObject.getName());
        namedFigure.repaint();
    }

    /**
	 * Indique � la figure d'afficher la selection.
	 */
    public void setSelected(int value) {
        logger.debug(this + " is selected.");
        super.setSelected(value);
        INamedFigure namedFigure = (INamedFigure) getFigure();
        if (value != EditPart.SELECTED_NONE) {
            namedFigure.setSelected(true);
        } else namedFigure.setSelected(false);
        namedFigure.repaint();
    }

    /**
	 * Met � jour le champ nom de la figure.
	 */
    protected void commitNameChange(PropertyChangeEvent evt) {
        logger.debug(this + " commit name changed.");
        INamedFigure namedFigure = (INamedFigure) getFigure();
        EditableLabel label = namedFigure.getNameLabel();
        if (label != null) label.setVisible(true);
        namedFigure.setName(((NamedObject) getModel()).getName());
        namedFigure.repaint();
    }

    /**
	 * Court-circuite le performRequest de EditPart pour g�rer l'�dition en direct du nom ou de la description.
	 * @see org.eclipse.gef.EditPart#performRequest(org.eclipse.gef.Request)
	 */
    public void performRequest(Request request) {
        if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) {
            if (request instanceof DirectEditRequest) {
                if (directEditHitNameTest(((DirectEditRequest) request).getLocation().getCopy())) {
                    directEditType = DIRECT_EDIT_TYPE_NAME;
                    performNameDirectEdit();
                }
            }
        } else super.performRequest(request);
    }

    /**
	 * Regarde si le point d'o� provient le clic de la souris est sur le champ nom.
	 * @param requestLoc Le point de location de la souris.
	 * @return
	 */
    private boolean directEditHitNameTest(Point requestLoc) {
        INamedFigure figure = (INamedFigure) getFigure();
        EditableLabel nameLabel = figure.getNameLabel();
        if (nameLabel != null) {
            nameLabel.translateToRelative(requestLoc);
            if (nameLabel.containsPoint(requestLoc)) return true;
        }
        return false;
    }

    public String getDirectEditType() {
        return directEditType;
    }

    /**
	 * Instancie le direct edit manager pour le champ nom.
	 */
    protected void performNameDirectEdit() {
        logger.debug(this + " perform name direct edit.");
        if (directEditManagerName == null) {
            ValidationEnabledGraphicalViewer viewer = (ValidationEnabledGraphicalViewer) getViewer();
            ValidationMessageHandler handler = viewer.getValidationHandler();
            INamedFigure figure = (INamedFigure) getFigure();
            EditableLabel nameLabel = figure.getNameLabel();
            directEditManagerName = new ExtendedDirectEditManager(this, TextCellEditor.class, new LabelCellEditorLocator(nameLabel, false), nameLabel, getNameCellEditorValidator(handler), true);
        }
        directEditManagerName.show();
    }

    /**
	 * G�re les �v�nements lev�s par le mod�le. En particulier si la propri�t�
	 * modifi�e est NamedObject.NAME ou NamedObject.DESCRIPTION.
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
    public void propertyChange(PropertyChangeEvent evt) {
        String property = evt.getPropertyName();
        if (NamedObject.NAME.equals(property)) {
            commitNameChange(evt);
        } else super.propertyChange(evt);
    }

    /**
	 * Retourne le validateur pour le nom de l'objet.
	 * @param handler Le validateur pour le nom de l'objet.
	 * @return Le validateur pour le nom de l'objet.
	 */
    protected ICellEditorValidator getNameCellEditorValidator(ValidationMessageHandler handler) {
        return new SimpleCellEditorValidator(handler);
    }
}
