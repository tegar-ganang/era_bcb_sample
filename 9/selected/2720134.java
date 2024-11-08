package com.ivis.xprocess.ui.diagram;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Tool;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.PageBook;
import com.ivis.xprocess.ui.datawrappers.DataCacheManager;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.diagram.model.DiagElement;
import com.ivis.xprocess.ui.diagram.model.DiagNode;
import com.ivis.xprocess.ui.diagram.util.StructuredSelectionConverter;
import com.ivis.xprocess.ui.diagram.util.StructuredSelectionConverter.OneObjectConverter;
import com.ivis.xprocess.ui.util.ViewUtil;

public class DiagramVisualizer {

    private static final Object FORBID_SELECTION = new Object();

    private final DiagramVisualizerSettings mySettings;

    private final GraphicalViewer myViewer;

    private final PageBook myPageBook;

    private final Composite myDiagramPane;

    private final Composite myMessagePane;

    private final Label myMessageLabel;

    private final ISelectionProvider mySelectionProvider;

    public DiagramVisualizer(final DiagramVisualizerSettings settings, Composite parent) {
        settings.checkRootEditPart();
        mySettings = settings;
        myPageBook = new PageBook(parent, SWT.NONE);
        myMessagePane = new Composite(myPageBook, SWT.NONE);
        myMessagePane.setLayout(new FillLayout());
        myMessageLabel = new Label(myMessagePane, SWT.NONE);
        if (mySettings.isInfiniteDiagramSize()) {
            myViewer = new InfiniteGraphicalViewer();
        } else {
            myViewer = new FiniteGraphicalViewer();
        }
        myViewer.setRootEditPart(mySettings.getRootEditPart());
        myViewer.setEditPartFactory(mySettings.getEditPartFactory());
        myViewer.setEditDomain(new EditDomain());
        myDiagramPane = new Composite(myPageBook, SWT.NONE);
        myDiagramPane.setLayout(new FillLayout());
        if (mySettings.getToolbarBuilder() == null) {
            myViewer.createControl(myDiagramPane);
        } else {
            Composite c = new Composite(myDiagramPane, SWT.NONE);
            GridLayout gridLayout = new GridLayout();
            gridLayout.marginHeight = 0;
            gridLayout.marginWidth = 0;
            gridLayout.verticalSpacing = 0;
            c.setLayout(gridLayout);
            CoolBar coolbar = new CoolBar(c, SWT.FLAT);
            coolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            Label separator = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
            myViewer.createControl(c);
            myViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            CoolBarManager coolBarManager = new CoolBarManager(coolbar);
            for (Iterable<IAction> toolbar : mySettings.getToolbarBuilder().buildToolbar(getViewer())) {
                ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.WRAP | SWT.HORIZONTAL);
                for (IAction action : toolbar) {
                    toolBarManager.add(action);
                }
                coolBarManager.add(toolBarManager);
            }
            coolBarManager.update(false);
        }
        if (mySettings.getContentMenu() != null) {
            myViewer.setContextMenu(mySettings.getContentMenu());
        }
        mySelectionProvider = new StructuredSelectionConverter(myViewer, new ObjectConverterImpl()) {

            @Override
            protected void setConvertedSelection(ISelection selection, boolean forceListen) {
                super.setConvertedSelection(selection, forceListen);
                if (!selection.isEmpty()) {
                    EditPart editPart = (EditPart) ((IStructuredSelection) selection).getFirstElement();
                    myViewer.reveal(editPart);
                }
            }

            @Override
            protected IStructuredSelection convertFromOriginal(IStructuredSelection originalSelection) {
                IStructuredSelection selection = super.convertFromOriginal(originalSelection);
                if (selection.isEmpty()) {
                    if (mySettings.getModelProvider().getRootUuid() != null) {
                        IElementWrapper elementWrapper = DataCacheManager.getWrapperByUuid(mySettings.getModelProvider().getRootUuid());
                        if ((elementWrapper != null) && !elementWrapper.isGhost()) {
                            return new StructuredSelection(elementWrapper);
                        }
                    }
                    return StructuredSelection.EMPTY;
                } else if ((selection.size() == 1) && FORBID_SELECTION.equals(selection.getFirstElement())) {
                    return StructuredSelection.EMPTY;
                } else {
                    return selection;
                }
            }
        };
        showDiagram();
    }

    /**
     * @return the diagrams zoom manager
     */
    public ZoomManager getZoomManager() {
        if (mySettings.isInfiniteDiagramSize()) {
            return ((ScalableFreeformRootEditPart) getViewer().getRootEditPart()).getZoomManager();
        }
        throw new UnsupportedOperationException("Zooming not supported for finite-size diagram");
    }

    /**
     * @return the base control of the diagram
     */
    public Control getControl() {
        return myViewer.getControl();
    }

    public Control getRootControl() {
        return myPageBook;
    }

    /**
     * Refresh the diagram.
     */
    public void refresh() {
        refresh(true);
    }

    /**
     * Refresh the diagram
     * @param rebuildVisualModel - additionally refresh the diagrams content
     */
    public void refresh(boolean rebuildVisualModel) {
        List<DiagNode> selectedElements = new ArrayList<DiagNode>(myViewer.getSelectedEditParts().size());
        for (Object editPart : myViewer.getSelectedEditParts()) {
            DiagElement element = findElement((EditPart) editPart);
            if (element instanceof DiagNode) {
                selectedElements.add((DiagNode) element);
            }
        }
        Tool currentTool = myViewer.getEditDomain().getActiveTool();
        if (currentTool != null) {
            currentTool.deactivate();
        }
        DiagNode modelRoot = mySettings.getModelProvider().getModel().getRoot();
        if ((myViewer.getContents() == null) || rebuildVisualModel) {
            myViewer.setContents(null);
            myViewer.setContents(modelRoot);
        }
        LayerManager layerManager = (LayerManager) myViewer.getEditPartRegistry().get(LayerManager.ID);
        IFigure primaryLayer = layerManager.getLayer(LayerConstants.PRIMARY_LAYER);
        primaryLayer.setFont(myDiagramPane.isDisposed() ? ViewUtil.getDisplay().getSystemFont() : myDiagramPane.getFont());
        if (modelRoot != null) {
            LayoutHelper layoutHelper = new LayoutHelper(this);
            mySettings.getLayoutPerformer().execute(modelRoot, layoutHelper);
            getRootFigure().validate();
            layoutHelper.applyLinksPoints();
            getRootFigure().validate();
        }
        List<EditPart> editPartsToSelect = new ArrayList<EditPart>();
        for (DiagNode node : selectedElements) {
            EditPart editPart = getEditPart(node);
            if (editPart == null) {
                IElementWrapper elementWrapper = node.getElementWrapper();
                if (elementWrapper != null) {
                    List<? extends DiagElement> modelElements = mySettings.getModelProvider().getModel().getElementList(elementWrapper);
                    if ((modelElements != null) && (modelElements.size() == 1)) {
                        editPart = getEditPart(modelElements.get(0));
                    }
                }
            }
            if (editPart != null) {
                editPartsToSelect.add(editPart);
            }
        }
        myViewer.setSelection(new StructuredSelection(editPartsToSelect));
        if (currentTool != null) {
            currentTool.activate();
        }
    }

    public AbstractGraphicalEditPart getEditPart(DiagElement element) {
        return (AbstractGraphicalEditPart) myViewer.getEditPartRegistry().get(element);
    }

    public GraphicalViewer getViewer() {
        return myViewer;
    }

    /**
     * Empty implementation
     */
    public void dispose() {
    }

    /**
     * @return the root figure of the digram
     */
    public IFigure getRootFigure() {
        if ((myViewer != null) && (myViewer.getContents() != null)) {
            AbstractGraphicalEditPart rootEditPart = (AbstractGraphicalEditPart) myViewer.getContents().getRoot();
            if (rootEditPart != null) {
                return rootEditPart.getFigure();
            }
        }
        return null;
    }

    /**
     * Display a text string in the diagrams page.
     *
     * @param message
     */
    public void showMessage(String message) {
        if (!myMessageLabel.isDisposed()) {
            myMessageLabel.setText(message);
            myPageBook.showPage(myMessagePane);
        }
    }

    /**
     * Display the diagram page.
     */
    public void showDiagram() {
        myPageBook.showPage(myDiagramPane);
    }

    /**
     * @return StructuredSelection&lt;IElementWrappers&gt;
     */
    public ISelectionProvider getSelectionProvider() {
        return mySelectionProvider;
    }

    private DiagElement findElement(EditPart editPart) {
        Object model = editPart.getModel();
        if (!(model instanceof DiagElement)) {
            return findElement(editPart.getParent());
        }
        return (DiagElement) model;
    }

    /**
     * @param elementWrapper
     * @return the DiagElement based on the elementWrapper from the model behind the diagram
     */
    public DiagElement getElement(IElementWrapper elementWrapper) {
        List<? extends DiagElement> elements = mySettings.getModelProvider().getModel().getElementList(elementWrapper);
        if (elements.size() > 1) {
            DiagElement selectedElement = findElement((EditPart) myViewer.getSelectedEditParts().get(0));
            assert elements.contains(selectedElement);
            return selectedElement;
        }
        return elements.get(0);
    }

    private IFigure getLabelFigure(DiagNode node, String labelId) {
        ModelNodeEditPart editPart = (ModelNodeEditPart) getEditPart(node);
        return editPart.getLabelFigure(labelId);
    }

    /**
     * @param node
     * @param labelId
     * @return the bounds of the label in the node
     */
    public Rectangle getLabelBounds(DiagNode node, String labelId) {
        IFigure labelFigure = getLabelFigure(node, labelId);
        if (labelFigure != null) {
            final Rectangle bounds;
            if (labelFigure instanceof org.eclipse.draw2d.Label) {
                bounds = ((org.eclipse.draw2d.Label) labelFigure).getTextBounds().getCopy();
            } else {
                bounds = labelFigure.getBounds().getCopy();
            }
            labelFigure.translateToAbsolute(bounds);
            return bounds;
        }
        return null;
    }

    /**
     * @param node
     * @param labelId
     * @return the labels font
     */
    public Font getLabelFont(DiagNode node, String labelId) {
        IFigure labelFigure = getLabelFigure(node, labelId);
        if (labelFigure != null) {
            return labelFigure.getFont();
        }
        return null;
    }

    public void reveal(DiagNode node) {
        EditPart editPart = getEditPart(node);
        myViewer.reveal(editPart);
    }

    @SuppressWarnings("unused")
    private String dumpFigures() {
        return dumpFigureBounds(((AbstractGraphicalEditPart) myViewer.getRootEditPart()).getFigure(), 0);
    }

    @SuppressWarnings("nls")
    private static String dumpFigureBounds(IFigure figure, int indent) {
        String s = "";
        for (int i = 0; i < indent; i++) {
            s += "  ";
        }
        s += (figure.hashCode() + " ");
        s += (figure.getClass().getName().substring(figure.getClass().getName().lastIndexOf('.') + 1) + " (");
        if (figure instanceof org.eclipse.draw2d.Label) {
            s += ("\"" + ((org.eclipse.draw2d.Label) figure).getText() + "\", ");
        }
        if (figure.getLayoutManager() != null) {
            s += (figure.getLayoutManager().getClass().getName().substring(figure.getLayoutManager().getClass().getName().lastIndexOf('.') + 1) + "): ");
        } else {
            s += "null): ";
        }
        s += ("bounds=[" + figure.getBounds().x + "," + figure.getBounds().y + " " + figure.getBounds().width + "x" + figure.getBounds().height + "]");
        s += (", border: " + figure.getBounds().x + "," + figure.getBounds().y + " " + figure.getBounds().right() + "," + figure.getBounds().bottom());
        if (figure instanceof Polyline) {
            Polyline polyline = (Polyline) figure;
            s += ", points {";
            for (int i = 0; i < polyline.getPoints().size(); i++) {
                Point p = polyline.getPoints().getPoint(i);
                s += ("[" + p.x + "," + p.y + "] ");
            }
            s += "}";
        }
        s += "\n";
        for (int i = 0; i < figure.getChildren().size(); i++) {
            s += dumpFigureBounds((IFigure) figure.getChildren().get(i), indent + 1);
        }
        return s;
    }

    private class ObjectConverterImpl implements OneObjectConverter {

        public Object convertFromOriginal(Object original) {
            DiagElement modelElement = findElement((EditPart) original);
            if (modelElement.allowGlobalSelection()) {
                if (modelElement instanceof DiagNode) {
                    DiagNode nodeElement = (DiagNode) modelElement;
                    return nodeElement.getElementWrapper();
                }
                return modelElement;
            }
            return FORBID_SELECTION;
        }

        public Object convertToOriginal(Object converted) {
            if (converted instanceof IElementWrapper) {
                return getEditPart(getElement((IElementWrapper) converted));
            }
            return null;
        }
    }
}
