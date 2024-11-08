package com.byterefinery.rmbench.dnd;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;

/**
 * listener for imported schema elements that are dropped on a diagram
 *  
 * @author cse
 */
public class DiagramDropTargetListener extends AbstractTransferDropTargetListener {

    public DiagramDropTargetListener(EditPartViewer viewer, RMBenchTransfer transfer) {
        super(viewer);
        setTransfer(transfer);
    }

    protected void updateTargetRequest() {
        ((CreateRequest) getTargetRequest()).setLocation(getDropLocation());
    }

    protected Request createTargetRequest() {
        CreateRequest request = new CreateRequest();
        RMBenchTransfer transfer = (RMBenchTransfer) getTransfer();
        request.setFactory(transfer);
        return request;
    }

    public void dragEnter(DropTargetEvent event) {
        event.detail = DND.DROP_LINK;
        super.dragEnter(event);
    }

    /**
     * @param viewer
     * @return a DiagramDropTargetListener ready configured for schema import DnD to
     * the given viewer
     */
    public static org.eclipse.jface.util.TransferDropTargetListener forImport(GraphicalViewer viewer) {
        return new DiagramDropTargetListener(viewer, ImportTransfer.getInstance());
    }

    /**
     * @param viewer
     * @return a DiagramDropTargetListener ready configured for model DnD to the given viewer
     */
    public static org.eclipse.jface.util.TransferDropTargetListener forModel(GraphicalViewer viewer) {
        return new DiagramDropTargetListener(viewer, ModelTransfer.getInstance());
    }
}
