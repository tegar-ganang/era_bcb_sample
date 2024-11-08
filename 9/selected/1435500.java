package cnaf.sidoc.ide.docflows.graphics;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMAttr;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMElement;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import cnaf.sidoc.ide.docflows.graphics.model.Docflow;
import cnaf.sidoc.ide.docflows.graphics.parts.DocflowEditPartsFactory;

public class GraphicalDocflowEditor extends GraphicalEditorWithFlyoutPalette {

    private final Docflow docflowModel;

    private final DocflowEditor parentEditor;

    private INodeAdapter observeDocflowURIAttrListener = new INodeAdapter() {

        public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {
            if (notifier == null) return;
            switch(eventType) {
                case INodeNotifier.CHANGE:
                case INodeNotifier.ADD:
                case INodeNotifier.REMOVE:
                    if (changedFeature instanceof Attr) {
                        IDOMAttr attr = ((IDOMAttr) changedFeature);
                        if ("uri".equals(attr.getName())) {
                            GraphicalDocflowEditor.this.parentEditor.updatePageText((Element) notifier, (String) newValue);
                        }
                    }
                    break;
            }
        }

        public boolean isAdapterForType(Object arg0) {
            return false;
        }
    };

    public GraphicalDocflowEditor(DocflowEditor parentEditor, IDOMElement docflowElement) {
        super.setEditDomain(new DefaultEditDomain(this));
        this.docflowModel = new Docflow(docflowElement);
        docflowModel.addAdapter(observeDocflowURIAttrListener);
        this.parentEditor = parentEditor;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        return null;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(DocflowEditPartsFactory.INSTANCE);
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getDocflowModel());
    }

    public Docflow getDocflowModel() {
        return docflowModel;
    }

    @Override
    public void dispose() {
        getDocflowModel().removeAdapter(observeDocflowURIAttrListener);
        super.dispose();
    }
}
