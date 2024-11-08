package com.ivis.xprocess.ui.processdesigner.diagram;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Handle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Tool;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.handles.ConnectionHandle;
import org.eclipse.gef.palette.PaletteContainer;
import org.eclipse.gef.palette.PaletteEntry;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.services.IDisposable;
import com.ivis.xprocess.framework.Xelement;
import com.ivis.xprocess.ui.UIPlugin;
import com.ivis.xprocess.ui.datawrappers.DataCacheManager;
import com.ivis.xprocess.ui.datawrappers.IElementWrapper;
import com.ivis.xprocess.ui.processdesigner.diagram.layout.LayoutHelperImpl;
import com.ivis.xprocess.ui.processdesigner.diagram.layout.LayoutPerformer;
import com.ivis.xprocess.ui.processdesigner.diagram.model.ChildPattern;
import com.ivis.xprocess.ui.processdesigner.diagram.model.Element;
import com.ivis.xprocess.ui.processdesigner.diagram.model.Model;
import com.ivis.xprocess.ui.processdesigner.diagram.model.Node;
import com.ivis.xprocess.ui.processdesigner.diagram.palette.ArtifactTypeToolDescriptor;
import com.ivis.xprocess.ui.processdesigner.diagram.palette.GatewayTypeToolDescriptor;
import com.ivis.xprocess.ui.processdesigner.diagram.palette.PreferenceStoreBasedFlyoutPreferences;
import com.ivis.xprocess.ui.processdesigner.diagram.palette.RoleTypeToolDescriptor;
import com.ivis.xprocess.ui.processdesigner.dnd.XElementTransferDropTargetListener;
import com.ivis.xprocess.ui.processdesigner.preferences.PreferenceConstants;
import com.ivis.xprocess.ui.processdesigner.preferences.Preferences;
import com.ivis.xprocess.ui.processdesigner.util.StructuredSelectionConverter;
import com.ivis.xprocess.ui.processdesigner.util.Util;
import com.ivis.xprocess.ui.processdesigner.util.StructuredSelectionConverter.OneObjectConverter;

public class Visualizer implements ModelProvider.RootChangeListener, IDisposable {

    private static final Object FORBID_SELECTION = new Object();

    private IPropertyChangeListener myFilterPreferenceListener;

    private final ModelProvider myModelProvider;

    private final ScrollingGraphicalViewer myViewer;

    private final ISelectionProvider mySelectionProvider;

    private final CoolBarManager myCoolBarManager;

    public Visualizer(Composite parent, ModelProvider modelProvider, MenuManager contextMenu, IWorkbenchPage workbenchPage, PaletteRoot palette, ProcessDesignerInplaceManager inplaceManager) {
        myModelProvider = modelProvider;
        myModelProvider.addRootChangeListener(this);
        parent.setLayout(new GridLayout(1, false));
        myViewer = new ScrollingGraphicalViewer() {

            @Override
            public Handle findHandleAt(Point p) {
                Handle handle = super.findHandleAt(p);
                if (handle instanceof ConnectionHandle) {
                    return null;
                } else {
                    return handle;
                }
            }
        };
        myViewer.setEditPartFactory(new EditPartFactoryImpl(inplaceManager));
        ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart();
        ((ConnectionLayer) rootEditPart.getLayer(LayerConstants.CONNECTION_LAYER)).setConnectionRouter(new BendpointConnectionRouter());
        myViewer.setRootEditPart(rootEditPart);
        myViewer.setEditDomain(new EditDomain());
        PaletteViewerProvider paletteViewerProvider = new PaletteViewerProvider(myViewer.getEditDomain()) {

            @Override
            protected void configurePaletteViewer(PaletteViewer viewer) {
                super.configurePaletteViewer(viewer);
            }
        };
        PreferenceStoreBasedFlyoutPreferences palettePrefs = new PreferenceStoreBasedFlyoutPreferences(UIPlugin.getDefault().getPreferenceStore());
        FlyoutPaletteComposite paletteComposite = new FlyoutPaletteComposite(parent, SWT.BORDER, workbenchPage, paletteViewerProvider, palettePrefs);
        paletteComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite composite = new Composite(paletteComposite, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginHeight = 0;
        gridLayout.marginWidth = 0;
        gridLayout.verticalSpacing = 0;
        composite.setLayout(gridLayout);
        CoolBar coolbar = new CoolBar(composite, SWT.FLAT);
        coolbar.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        myCoolBarManager = new CoolBarManager(coolbar);
        Label separator = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        myViewer.createControl(composite);
        paletteComposite.setGraphicalControl(composite);
        myViewer.getEditDomain().setPaletteRoot(palette);
        setFilterListeners(palette);
        myViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        myViewer.getControl().setBackground(ColorConstants.white);
        myViewer.setContextMenu(contextMenu);
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
                    if (myModelProvider.getRootElementUuid() != null) {
                        IElementWrapper elementWrapper = DataCacheManager.getWrapperByUuid(myModelProvider.getRootElementUuid());
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
        myViewer.addDropTargetListener(new XElementTransferDropTargetListener(myViewer));
        refresh(true);
    }

    public static PaletteEntry getToolEntry(String id, PaletteRoot root) {
        return findInPalette(id, root.getChildren());
    }

    private static PaletteEntry findInPalette(String id, List<?> children) {
        for (int i = 0; i < children.size(); i++) {
            Object child = children.get(i);
            if (child instanceof PaletteContainer) {
                PaletteEntry result = findInPalette(id, ((PaletteContainer) child).getChildren());
                if (result == null) {
                    continue;
                }
                return result;
            } else if (child instanceof PaletteEntry) {
                if (((PaletteEntry) child).getId().equals(id)) {
                    return (PaletteEntry) child;
                }
            }
        }
        return null;
    }

    public ZoomManager getZoomManager() {
        return ((ScalableFreeformRootEditPart) myViewer.getRootEditPart()).getZoomManager();
    }

    public void setupActions(Iterable<Iterable<IAction>> toolbars) {
        for (Iterable<IAction> toolbar : toolbars) {
            ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT | SWT.WRAP | SWT.HORIZONTAL);
            for (IAction action : toolbar) {
                toolBarManager.add(action);
            }
            myCoolBarManager.add(toolBarManager);
        }
        myCoolBarManager.update(false);
    }

    public void refresh() {
        refresh(false);
    }

    public void refresh(boolean forceModelRebuilding) {
        List<String> selectedUuids = new ArrayList<String>(myViewer.getSelectedEditParts().size());
        for (Object editPart : myViewer.getSelectedEditParts()) {
            Element element = getElement((EditPart) editPart);
            if (element instanceof Node && (((Node) element).getXelement() != null)) {
                selectedUuids.add(((Node) element).getXelement().getUuid());
            }
        }
        Tool currentTool = myViewer.getEditDomain().getActiveTool();
        if (currentTool != null) {
            currentTool.deactivate();
        }
        myViewer.setContents(null);
        if ((myModelProvider.getRootElementUuid() != null) && !Util.isDeleted(myModelProvider.getRootElementUuid())) {
            if (forceModelRebuilding) {
                myModelProvider.rebuildModel();
            }
            Model model = myModelProvider.getModel();
            if (model.getRoot() == null) {
                throw new RuntimeException("null model root");
            }
            myViewer.setContents(model.getRoot());
            LayerManager layerManager = (LayerManager) myViewer.getEditPartRegistry().get(LayerManager.ID);
            IFigure primaryLayer = layerManager.getLayer(LayerConstants.PRIMARY_LAYER);
            primaryLayer.setFont(Preferences.getDiagramFont());
            myViewer.getControl().setBackground(Preferences.getDiagramColor());
            LayoutHelperImpl helper = new LayoutHelperImpl(this);
            new LayoutPerformer().execute(model.getRoot(), helper, Preferences.layoutHorizontalNotVertical(), true);
            getRootFigure().validate();
            helper.applyLinksPoints();
            getRootFigure().validate();
        }
        List<EditPart> editPartsToSelect = new ArrayList<EditPart>();
        for (String uuid : selectedUuids) {
            List<Element> modelElements = myModelProvider.getModel().getElementList(Util.getElement(uuid));
            if (modelElements.size() == 1) {
                EditPart editPart = getEditPart(modelElements.get(0));
                if (editPart != null) {
                    editPartsToSelect.add(editPart);
                }
            }
        }
        myViewer.setSelection(new StructuredSelection(editPartsToSelect));
        if (currentTool != null) {
            currentTool.activate();
        }
    }

    public IFigure getRootFigure() {
        if ((myViewer != null) && (myViewer.getContents() != null)) {
            AbstractGraphicalEditPart rootEditPart = (AbstractGraphicalEditPart) myViewer.getContents().getRoot();
            if (rootEditPart != null) {
                return rootEditPart.getFigure();
            }
        }
        return null;
    }

    public Control getControl() {
        return myViewer.getControl();
    }

    public Object getViewer() {
        return myViewer;
    }

    /**
     * @return StructuredSelection&lt;IElementWrappers&gt;
     */
    public ISelectionProvider getSelectionProvider() {
        return mySelectionProvider;
    }

    public void rootChanged(String newRootUuid) {
        refresh();
    }

    public void dispose() {
        if (myModelProvider != null) {
            myModelProvider.removeRootChangeListener(this);
        }
        if (myFilterPreferenceListener != null) {
            Preferences.removePreferenceListener(myFilterPreferenceListener);
            myFilterPreferenceListener = null;
        }
    }

    public void reveal(Node node) {
        EditPart editPart = getEditPart(node);
        myViewer.reveal(editPart);
    }

    public Element getElement(EditPart part) {
        Object model = part.getModel();
        if (model instanceof Element) {
            return (Element) model;
        }
        return null;
    }

    public EditPart getEditPart(Element element) {
        return (EditPart) myViewer.getEditPartRegistry().get(element);
    }

    public Element getElement(IElementWrapper elementWrapper) {
        Xelement xElement = Util.getElement(elementWrapper);
        List<Element> elements = myModelProvider.getModel().getElementList(xElement);
        if (elements.size() > 1) {
            Element selectedElement = findElement((EditPart) myViewer.getSelectedEditParts().get(0));
            assert elements.contains(selectedElement);
            return selectedElement;
        } else if (!elements.isEmpty()) {
            return elements.get(0);
        } else {
            return null;
        }
    }

    private IFigure getLabelFigure(Node node, String inplaceId) {
        ModelNodeEditPart editPart = (ModelNodeEditPart) getEditPart(node);
        return editPart.getLabelFigure(inplaceId);
    }

    public Rectangle getLabelBounds(Node node, String inplaceId) {
        IFigure labelFigure = getLabelFigure(node, inplaceId);
        if (labelFigure != null) {
            final Rectangle bounds;
            if (labelFigure instanceof org.eclipse.draw2d.Label) {
                bounds = ((org.eclipse.draw2d.Label) labelFigure).getTextBounds().getCopy();
            } else {
                bounds = labelFigure.getBounds().getCopy();
            }
            labelFigure.translateToAbsolute(bounds);
            return bounds;
        } else {
            return null;
        }
    }

    public Font getLabelFont(Node node, String inplaceId) {
        IFigure labelFigure = getLabelFigure(node, inplaceId);
        if (labelFigure != null) {
            return labelFigure.getFont();
        } else {
            return null;
        }
    }

    private Element findElement(EditPart editPart) {
        Object model = editPart.getModel();
        if (!(model instanceof Element)) {
            return findElement(editPart.getParent());
        }
        return (Element) model;
    }

    private void setFilterListeners(final PaletteRoot root) {
        myFilterPreferenceListener = new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                if (PreferenceConstants.DIAGRAM_FILTER_HIDE_GATEWAY.equals(event.getProperty())) {
                    showPaletteTool(root, GatewayTypeToolDescriptor.TOOL_ID, !Preferences.getPreferenceStore().getBoolean(event.getProperty()));
                } else if (PreferenceConstants.DIAGRAM_FILTER_HIDE_ROLETYPE.equals(event.getProperty())) {
                    showPaletteTool(root, RoleTypeToolDescriptor.TOOL_ID, !Preferences.getPreferenceStore().getBoolean(event.getProperty()));
                } else if (PreferenceConstants.DIAGRAM_FILTER_HIDE_ARTIFACT.equals(event.getProperty())) {
                    showPaletteTool(root, ArtifactTypeToolDescriptor.TOOL_ID, !Preferences.getPreferenceStore().getBoolean(event.getProperty()));
                }
            }
        };
        Preferences.addPreferenceListener(myFilterPreferenceListener);
    }

    private static void showPaletteTool(PaletteRoot root, String toolId, boolean checked) {
        PaletteEntry paletteEntry = getToolEntry(toolId, root);
        if (paletteEntry != null) {
            paletteEntry.setVisible(checked);
        }
    }

    @SuppressWarnings("unused")
    private String dumpFigures() {
        return dumpFigureBounds(((AbstractGraphicalEditPart) myViewer.getRootEditPart()).getFigure(), 0);
    }

    @SuppressWarnings("nls")
    public static String dumpFigureBounds(IFigure figure, int indent) {
        String s = "";
        for (int i = 0; i < indent; i++) {
            s += "  ";
        }
        s += (figure.hashCode() + " ");
        s += (figure.getClass().getName().substring(figure.getClass().getName().lastIndexOf('.') + 1) + " (");
        if (figure.getLayoutManager() != null) {
            s += (figure.getLayoutManager().getClass().getName().substring(figure.getLayoutManager().getClass().getName().lastIndexOf('.') + 1) + "): ");
        } else {
            s += "null): ";
        }
        s += (figure.getBounds().x + "," + figure.getBounds().y + " " + figure.getBounds().width + "x" + figure.getBounds().height);
        s += (", border: " + figure.getBounds().x + "," + figure.getBounds().y + " " + figure.getBounds().right() + "," + figure.getBounds().bottom());
        s += "\n";
        for (int i = 0; i < figure.getChildren().size(); i++) {
            s += dumpFigureBounds((IFigure) figure.getChildren().get(i), indent + 1);
        }
        return s;
    }

    private class ObjectConverterImpl implements OneObjectConverter {

        public Object convertFromOriginal(Object original) {
            Element modelElement = findElement((EditPart) original);
            if (modelElement.allowGlobalSelection()) {
                if (modelElement instanceof Node) {
                    Node nodeElement = (Node) modelElement;
                    if (nodeElement.getLevel() == 0) {
                        return null;
                    } else {
                        if (modelElement instanceof ChildPattern) {
                            return modelElement;
                        } else {
                            Xelement xElement = nodeElement.getXelement();
                            return (xElement != null) ? Util.getElementWrapper(xElement) : null;
                        }
                    }
                } else {
                    return modelElement;
                }
            } else {
                return FORBID_SELECTION;
            }
        }

        public Object convertToOriginal(Object converted) {
            if (converted instanceof IElementWrapper) {
                Element element = getElement((IElementWrapper) converted);
                if (element != null) {
                    return getEditPart(element);
                }
            }
            return null;
        }
    }
}
