package uk.ac.bolton.archimate.editor.diagram.dnd;

import org.eclipse.gef.dnd.TemplateTransfer;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import uk.ac.bolton.archimate.editor.diagram.ICreationFactory;
import uk.ac.bolton.archimate.editor.diagram.IDiagramModelEditor;

/**
 * This Drop Target Listener is used when dragging a Pallete Entry onto a Graphical Viewer.
 * We have to make sure that we are dragging the correct (template) entry from the correct Palette.
 * It is possible to re-arrange the Eclipse Viewers side-by side and drag from one Viewer's
 * Palette to another (different) Viewer causing a NPE because the EditPartFactory is trying to
 * create the wrong type of part.
 * 
 * @author Phillip Beauvoir
 */
public class PaletteTemplateTransferDropTargetListener extends TemplateTransferDropTargetListener {

    private IDiagramModelEditor fEditor;

    public PaletteTemplateTransferDropTargetListener(IDiagramModelEditor editor) {
        super(editor.getGraphicalViewer());
        fEditor = editor;
    }

    @Override
    public boolean isEnabled(DropTargetEvent event) {
        ICreationFactory factory = (ICreationFactory) getFactory(TemplateTransfer.getInstance().getTemplate());
        if (factory != null && !factory.isUsedFor(fEditor)) {
            return false;
        }
        return super.isEnabled(event);
    }
}
