package uk.ac.bolton.archimate.editor.diagram.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import uk.ac.bolton.archimate.editor.diagram.util.DiagramUtils;

/**
 * Export As Image to clipboard Action
 * 
 * @author Phillip Beauvoir
 */
public class ExportAsImageToClipboardAction extends Action {

    public static final String ID = "uk.ac.bolton.archimate.editor.action.exportAsImageToClipboard";

    public static final String TEXT = Messages.ExportAsImageToClipboardAction_0;

    private GraphicalViewer fDiagramViewer;

    public ExportAsImageToClipboardAction(GraphicalViewer diagramViewer) {
        super(TEXT);
        fDiagramViewer = diagramViewer;
        setId(ID);
        setActionDefinitionId(getId());
    }

    @Override
    public void run() {
        Image image = DiagramUtils.createImage(fDiagramViewer);
        Clipboard cb = new Clipboard(Display.getDefault());
        cb.setContents(new Object[] { image.getImageData() }, new Transfer[] { ImageTransfer.getInstance() });
        image.dispose();
        MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ExportAsImageToClipboardAction_1, Messages.ExportAsImageToClipboardAction_2);
    }
}
