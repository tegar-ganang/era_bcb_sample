package bpmetrics.editors;

import org.eclipse.bpel.ui.BPELEditor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.swt.widgets.Composite;

public class NewBPELEditor extends BPELEditor {

    public NewBPELEditor(DefaultEditDomain ed) {
        super(ed);
    }

    public void registerViewer(EditPartViewer viewer) {
        super.registerViewer(viewer);
    }

    public KeyHandler getKeyHandler() {
        return super.getKeyHandler();
    }

    @Override
    protected void initializeTrayViewer() {
    }

    public void createPartControl(Composite parent) {
        createGraphicalViewer(parent);
    }
}
