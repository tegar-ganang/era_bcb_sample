package uk.ac.bolton.archimate.editor.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.ui.IWorkbenchPart;
import uk.ac.bolton.archimate.editor.diagram.IArchimateDiagramEditor;
import uk.ac.bolton.archimate.editor.ui.IArchimateImages;

/**
 * Show Derived Relations Chains Action
 * 
 * @author Phillip Beauvoir
 */
public class ShowStructuralChainsAction extends WorkbenchPartAction {

    public static final String ID = "ShowStructuralChainsAction";

    public static final String DEFAULT_TEXT = Messages.ShowStructuralChainsAction_0;

    public ShowStructuralChainsAction(IWorkbenchPart part) {
        super(part);
        setId(ID);
        setText(DEFAULT_TEXT);
        setImageDescriptor(IArchimateImages.ImageFactory.getImageDescriptor(IArchimateImages.ICON_DERIVED_16));
    }

    @Override
    protected boolean calculateEnabled() {
        return true;
    }

    private boolean isShowingChains() {
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        Boolean val = (Boolean) viewer.getProperty(IArchimateDiagramEditor.PROPERTY_SHOW_STRUCTURAL_CHAIN);
        if (val != null) return val.booleanValue();
        return false;
    }

    @Override
    public void run() {
        GraphicalViewer viewer = (GraphicalViewer) getWorkbenchPart().getAdapter(GraphicalViewer.class);
        boolean val = !isShowingChains();
        viewer.setProperty(IArchimateDiagramEditor.PROPERTY_SHOW_STRUCTURAL_CHAIN, new Boolean(val));
        setText(isShowingChains() ? Messages.ShowStructuralChainsAction_1 : DEFAULT_TEXT);
    }
}
