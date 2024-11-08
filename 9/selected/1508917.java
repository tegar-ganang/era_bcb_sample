package com.safi.workshop.edit.parts;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gmf.runtime.diagram.ui.editparts.AbstractBorderedShapeEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles;
import org.eclipse.gmf.runtime.diagram.ui.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gmf.runtime.diagram.ui.figures.BorderItemLocator;
import org.eclipse.gmf.runtime.diagram.ui.figures.BorderedNodeFigure;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditDomain;
import org.eclipse.gmf.runtime.diagram.ui.requests.CreateConnectionViewAndElementRequest;
import org.eclipse.gmf.runtime.draw2d.ui.figures.ConstrainedToolbarLayout;
import org.eclipse.gmf.runtime.gef.ui.figures.DefaultSizeNodeFigure;
import org.eclipse.gmf.runtime.gef.ui.figures.NodeFigure;
import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.swt.graphics.Color;
import com.safi.asterisk.figures.DefaultToolstepFigure;
import com.safi.asterisk.figures.ToolstepAnchor;
import com.safi.core.actionstep.ActionStep;
import com.safi.core.actionstep.ActionStepException;
import com.safi.core.actionstep.DynamicValue;
import com.safi.core.actionstep.DynamicValueType;
import com.safi.core.actionstep.impl.ActionStepImpl;
import com.safi.core.saflet.SafletContext;
import com.safi.db.Variable;
import com.safi.server.plugin.SafiServerPlugin;
import com.safi.workshop.edit.policies.OpenEditorEditPolicy;
import com.safi.workshop.part.AsteriskDiagramEditor;
import com.safi.workshop.part.AsteriskDiagramEditorPlugin;
import com.safi.workshop.part.SafiWorkshopEditorUtil;
import com.safi.workshop.part.ValidateAction;
import com.safi.workshop.preferences.PreferenceConstants;

/**
 * @generated NOT
 */
public abstract class ToolstepEditPart extends AbstractBorderedShapeEditPart {

    protected static final Status OK_STATUS = new Status(IStatus.OK, AsteriskDiagramEditorPlugin.ID, null);

    public final Dimension MIN_DIMENSION = new Dimension(getMinWidth(), getMinHeight());

    protected IFigure activeFeedback;

    protected IFigure feedbackLayer;

    protected ActiveFeedbackZoomListener zoomListener;

    protected boolean minimized;

    /**
   * @generated
   */
    public ToolstepEditPart(View view) {
        super(view);
    }

    @Override
    public Command getCommand(Request request) {
        return super.getCommand(request);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        setMinimized(AsteriskDiagramEditorPlugin.getInstance().getPreferenceStore().getBoolean(PreferenceConstants.PREF_ACTIONSTEPS_MINIMIZED));
    }

    @Override
    protected NodeFigure createNodeFigure() {
        return new BorderedNodeFigure(createMainFigure()) {

            @Override
            public void setBounds(Rectangle rect) {
                super.setBounds(rect);
            }
        };
    }

    @Override
    protected void createDefaultEditPolicies() {
        super.createDefaultEditPolicies();
        removeEditPolicy(org.eclipse.gmf.runtime.diagram.ui.editpolicies.EditPolicyRoles.CONNECTION_HANDLES_ROLE);
        installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new GraphicalNodeEditPolicy() {

            @Override
            protected Command getConnectionAndRelationshipCompleteCommand(CreateConnectionViewAndElementRequest request) {
                GraphicalViewer graphicalViewer = ((GraphicalViewer) (getHandlerEditPart()).getViewer());
                if (graphicalViewer.getEditDomain().getPaletteViewer() == null) {
                    SafiWorkshopEditorUtil.initializePalette();
                }
                return super.getConnectionAndRelationshipCompleteCommand(request);
            }

            @Override
            protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
                return super.getConnectionCreateCommand(request);
            }
        });
        installEditPolicy(EditPolicyRoles.OPEN_ROLE, new OpenEditorEditPolicy());
    }

    @Override
    protected void addNotationalListeners() {
        super.addNotationalListeners();
    }

    @Override
    public boolean canAttachNote() {
        return true;
    }

    protected abstract IFigure getPrimaryShape();

    @Override
    protected void handleNotificationEvent(Notification notification) {
        Object obj = notification.getFeature();
        if (obj instanceof EStructuralFeature) {
            EStructuralFeature feature = (EStructuralFeature) obj;
            if ("active".equals(feature.getName())) {
                if (notification.getNewBooleanValue()) {
                    showActiveFeedback();
                } else {
                    hideActiveFeedback();
                }
            } else if (!((HandlerEditPart) getParent()).isDebug() && (notification.getEventType() == Notification.ADD || notification.getEventType() == Notification.ADD_MANY || notification.getEventType() == Notification.REMOVE || notification.getEventType() == Notification.REMOVE_MANY || notification.getEventType() == Notification.SET || notification.getEventType() == Notification.UNSET)) {
                ValidateAction.runValidation((DiagramEditPart) getParent(), getDiagramView());
            }
        }
        super.handleNotificationEvent(notification);
    }

    protected void hideActiveFeedback() {
        if (feedbackLayer != null) {
            feedbackLayer.remove(activeFeedback);
            ((DiagramRootEditPart) getRoot()).getZoomManager().removeZoomListener(zoomListener);
        }
    }

    protected void showActiveFeedback() {
        AsteriskDiagramEditor editor = (AsteriskDiagramEditor) ((DiagramEditDomain) getEditDomain()).getEditorPart();
        editor.getSite().getPage().activate(editor);
        getViewer().reveal(this);
        if (activeFeedback == null) {
            activeFeedback = createActiveFeedbackFigure();
            feedbackLayer = LayerManager.Helper.find(this).getLayer(LayerConstants.FEEDBACK_LAYER);
            zoomListener = new ActiveFeedbackZoomListener();
        }
        feedbackLayer.add(activeFeedback);
        ((DiagramRootEditPart) getRoot()).getZoomManager().addZoomListener(zoomListener);
        updateActiveFeedbackBounds();
        SafiWorkshopEditorUtil.activateWorkbenchShell();
    }

    private void updateActiveFeedbackBounds() {
        Rectangle bounds = getFigure().getBounds().getCopy();
        getFigure().translateToAbsolute(bounds);
        activeFeedback.translateToRelative(bounds);
        activeFeedback.setBounds(bounds);
    }

    protected IFigure createActiveFeedbackFigure() {
        RoundedRectangle r = new RoundedRectangle();
        FigureUtilities.makeGhostShape(r);
        r.setLineStyle(Graphics.LINE_DOT);
        r.setBackgroundColor(new Color(null, 0, 0, 150));
        return r;
    }

    public IStatus validate() {
        ActionStep ts = getActionStep();
        SafletContext context = ts.getSaflet().getSafletContext();
        for (EObject eo : ts.eClass().eContents()) {
            DynamicValue dyn = null;
            if (eo instanceof DynamicValue) dyn = (DynamicValue) eo; else if (eo instanceof EReference) {
                Object o = ts.eGet(((EReference) eo));
                if (o instanceof DynamicValue) dyn = (DynamicValue) o; else continue;
            } else continue;
            if (dyn != null && dyn.getType() == DynamicValueType.VARIABLE_NAME) {
                try {
                    String varNameStr = ((ActionStepImpl) ts).resolveVariableName(dyn, context);
                    if (StringUtils.isBlank(varNameStr)) {
                        return new Status(IStatus.ERROR, AsteriskDiagramEditorPlugin.ID, "Couldn't resolve variable name");
                    }
                    Variable v = context.getVariable(varNameStr);
                    if (v == null) {
                        v = SafiServerPlugin.getDefault().getGlobalVariable(varNameStr);
                    }
                    if (v == null) return new Status(IStatus.ERROR, AsteriskDiagramEditorPlugin.ID, "Couldn't resolve variable named " + varNameStr);
                } catch (ActionStepException e) {
                    e.printStackTrace();
                    return new Status(IStatus.ERROR, AsteriskDiagramEditorPlugin.ID, "Couldn't find variable: " + e.getLocalizedMessage());
                }
            }
        }
        return Status.OK_STATUS;
    }

    /**
   * @generated NOT
   */
    protected boolean addFixedChild(EditPart childEditPart) {
        if (childEditPart instanceof OutputEditPart) {
            OutputEditPart output = (OutputEditPart) childEditPart;
            IFigure fig = output.getFigure();
            BorderItemLocator locator = configureLocator(fig);
            output.setLocator(locator);
            return true;
        }
        return false;
    }

    public BorderItemLocator configureLocator(IFigure fig) {
        BorderItemLocator locator = new com.safi.asterisk.figures.OutputFigure.TerminalLocator(getMainFigure(), PositionConstants.EAST);
        locator.setBorderItemOffset(new Dimension(fig.getSize().width - 8, 0));
        getBorderedFigure().getBorderItemContainer().add(fig, locator);
        return locator;
    }

    public ActionStep getActionStep() {
        return (ActionStep) resolveSemanticElement();
    }

    /**
   * Default implementation treats passed figure as content pane. Respects layout one may
   * have set for generated figure.
   * 
   * @param nodeShape
   *          instance of generated figure class
   * @generated
   */
    protected IFigure setupContentPane(IFigure nodeShape) {
        if (nodeShape.getLayoutManager() == null) {
            ConstrainedToolbarLayout layout = new ConstrainedToolbarLayout();
            layout.setSpacing(getMapMode().DPtoLP(5));
            nodeShape.setLayoutManager(layout);
        }
        return nodeShape;
    }

    /**
   * @generated
   */
    protected boolean removeFixedChild(EditPart childEditPart) {
        if (childEditPart instanceof OutputEditPart) {
            getBorderedFigure().getBorderItemContainer().remove(((OutputEditPart) childEditPart).getFigure());
            return true;
        }
        return false;
    }

    /**
   * @generated
   */
    @Override
    protected void addChildVisual(EditPart childEditPart, int index) {
        if (addFixedChild(childEditPart)) {
            return;
        }
        super.addChildVisual(childEditPart, -1);
    }

    /**
   * @generated
   */
    @Override
    protected void removeChildVisual(EditPart childEditPart) {
        if (removeFixedChild(childEditPart)) {
            return;
        }
        super.removeChildVisual(childEditPart);
    }

    class ActiveFeedbackZoomListener implements ZoomListener {

        @Override
        public void zoomChanged(double zoom) {
            updateActiveFeedbackBounds();
        }
    }

    protected int getDefaultHeight() {
        return 75;
    }

    protected int getDefaultWidth() {
        return 100;
    }

    protected int getMinWidth() {
        return 80;
    }

    protected int getMinHeight() {
        return Math.max(34, getNumOutputs() * (OutputEditPart.OUTPUT_DEFAULT_HEIGHT + 8));
    }

    protected int getNumOutputs() {
        ActionStep ts = getActionStep();
        if (ts == null) return 0;
        return ts.getOutputs().size();
    }

    public int getSemanticHint() {
        return -1;
    }

    protected final NodeFigure createNodePlate() {
        DefaultSizeNodeFigure result = new DefaultSizeNodeFigure(getMapMode().DPtoLP(getDefaultWidth()), getMapMode().DPtoLP(getDefaultHeight())) {

            @Override
            protected ConnectionAnchor createDefaultAnchor() {
                return new ToolstepAnchor(this);
            }

            @Override
            protected ConnectionAnchor createAnchor(PrecisionPoint p) {
                return createDefaultAnchor();
            }

            @Override
            protected ConnectionAnchor createConnectionAnchor(Point p) {
                return createDefaultAnchor();
            }

            @Override
            public void revalidate() {
                super.revalidate();
            }

            @Override
            public Dimension getDefaultSize() {
                if (minimized) return ToolstepEditPart.this.getMinimumSize();
                return super.getDefaultSize();
            }

            @Override
            public void setBounds(Rectangle rect) {
                super.setBounds(rect);
            }
        };
        return result;
    }

    protected Dimension getMinimumSize() {
        return MIN_DIMENSION;
    }

    public class ActionStepNodeFigure extends DefaultSizeNodeFigure {

        public ActionStepNodeFigure(Dimension defSize) {
            super(defSize);
        }

        public ActionStepNodeFigure(int width, int height) {
            super(width, height);
        }

        @Override
        protected ConnectionAnchor createAnchor(PrecisionPoint p) {
            return super.createAnchor(p);
        }

        @Override
        public Dimension getPreferredSize(int hint, int hint2) {
            return super.getPreferredSize(hint, hint2);
        }

        @Override
        public void setBounds(Rectangle rect) {
            super.setBounds(rect);
        }
    }

    public boolean isMinimized() {
        return minimized;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
        IFigure fig = getPrimaryShape();
        if (fig == null) return;
        if (fig instanceof DefaultToolstepFigure) {
            DefaultToolstepFigure d = (DefaultToolstepFigure) fig;
            d.setMinimized(minimized);
        }
        refreshVisuals();
    }

    public AsteriskDiagramEditor getAsteriskDiagramEditor() {
        AsteriskDiagramEditor editor = (AsteriskDiagramEditor) ((DiagramEditDomain) getEditDomain()).getEditorPart();
        return editor;
    }

    public HandlerEditPart getHandlerEditPart() {
        return (HandlerEditPart) getParent();
    }

    public int getVisualId() {
        try {
            Field f = getClass().getDeclaredField("VISUAL_ID");
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                return f.getInt(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AsteriskDiagramEditorPlugin.getDefault().logError("Couldn't retrieve VISUAL_ID from " + this, e);
        }
        return -1;
    }
}
