package uk.ac.bolton.archimate.editor.diagram.sketch;

import org.eclipse.draw2d.ScalableFreeformLayeredPane;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import uk.ac.bolton.archimate.editor.diagram.AbstractDiagramEditor;
import uk.ac.bolton.archimate.editor.diagram.sketch.dnd.SketchDiagramTransferDropTargetListener;
import uk.ac.bolton.archimate.editor.diagram.sketch.editparts.SketchEditPartFactory;
import uk.ac.bolton.archimate.editor.diagram.util.ExtendedViewportAutoexposeHelper;
import uk.ac.bolton.archimate.editor.preferences.IPreferenceConstants;
import uk.ac.bolton.archimate.editor.preferences.Preferences;
import uk.ac.bolton.archimate.model.ISketchModel;

/**
 * Sketch Editor
 * 
 * @author Phillip Beauvoir
 */
public class SketchEditor extends AbstractDiagramEditor implements ISketchEditor {

    /**
     * Palette
     */
    private SketchEditorPalette fPalette;

    private ScalableFreeformLayeredPane fScalableFreeformLayeredPane;

    private BackgroundImageLayer fBackgroundImageLayer;

    private IPropertyChangeListener fPrefsListener = new IPropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (IPreferenceConstants.SKETCH_SHOW_BACKGROUND.equals(event.getProperty())) {
                showImagePane();
            }
        }
    };

    @Override
    public void doCreatePartControl(Composite parent) {
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);
        Preferences.STORE.addPropertyChangeListener(fPrefsListener);
    }

    @Override
    public PaletteRoot getPaletteRoot() {
        if (fPalette == null) {
            fPalette = new SketchEditorPalette();
        }
        return fPalette;
    }

    @Override
    public ISketchModel getModel() {
        return (ISketchModel) super.getModel();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new SketchEditPartFactory());
        viewer.setContents(getModel());
        viewer.addDropTargetListener(new SketchDiagramTransferDropTargetListener(viewer));
    }

    @Override
    protected void createRootEditPart(GraphicalViewer viewer) {
        RootEditPart rootPart = new ScalableFreeformRootEditPart() {

            @Override
            protected ScalableFreeformLayeredPane createScaledLayers() {
                fScalableFreeformLayeredPane = super.createScaledLayers();
                showImagePane();
                return fScalableFreeformLayeredPane;
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Object getAdapter(Class adapter) {
                if (adapter == AutoexposeHelper.class) {
                    return new ExtendedViewportAutoexposeHelper(this, new Insets(50), false);
                }
                return super.getAdapter(adapter);
            }
        };
        viewer.setRootEditPart(rootPart);
    }

    private void showImagePane() {
        if (Preferences.doSketchShowBackground()) {
            if (fBackgroundImageLayer == null) {
                fBackgroundImageLayer = new BackgroundImageLayer();
            }
            fScalableFreeformLayeredPane.add(fBackgroundImageLayer, BackgroundImageLayer.NAME, 0);
        } else {
            if (fBackgroundImageLayer != null) {
                fScalableFreeformLayeredPane.remove(fBackgroundImageLayer);
            }
        }
    }

    /**
     * Set up and register the context menu
     */
    @Override
    protected void registerContextMenu(GraphicalViewer viewer) {
        MenuManager provider = new SketchEditorContextMenuProvider(viewer, getActionRegistry());
        viewer.setContextMenu(provider);
        getSite().registerContextMenu(SketchEditorContextMenuProvider.ID, provider, viewer);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (fPalette != null) {
            fPalette.dispose();
        }
        Preferences.STORE.removePropertyChangeListener(fPrefsListener);
    }

    public int getContextChangeMask() {
        return NONE;
    }

    public IContext getContext(Object target) {
        return HelpSystem.getContext(HELP_ID);
    }

    public String getSearchExpression(Object target) {
        return Messages.SketchEditor_0;
    }
}
