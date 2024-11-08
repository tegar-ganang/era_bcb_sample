package org.genie.gef.editor;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.genie.browser.storage.ExtensionInput;
import org.genie.gef.GefEditorActivator;
import org.genie.gef.directedit.StatusLineValidationMessageHandler;
import org.genie.gef.dnd.DataEditDropTargetListener;
import org.genie.gef.factory.DiagramEditPartFactory;
import org.xmi.XMIRuntimeException;
import org.xmi.gui.eclipse.XMIPluginActivator;
import org.xmi.infoset.XMIExtension;
import org.xmi.infoset.ext.Diagram;
import org.xmi.repository.RepositoryDAO;

public class DiagramEditor extends GraphicalEditorWithFlyoutPalette implements CommandStackListener, ISelectionListener, ITabbedPropertySheetPageContributor {

    private final RepositoryDAO dao = XMIPluginActivator.getDefault().getRepositoryDriver();

    private XMIExtension ext;

    private Diagram diagram;

    private DefaultEditDomain editDomain;

    private GraphicalViewer graphicalViewer;

    private TabbedPropertySheetPage tabbedPropertySheetPage;

    private String modelId;

    private String xmiId;

    public DiagramEditor() {
        editDomain = new DefaultEditDomain(this);
        setEditDomain(editDomain);
    }

    public String getContributorId() {
        return getSite().getId();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getAdapter(Class adapter) {
        if (adapter == IPropertySheetPage.class) {
            if (tabbedPropertySheetPage == null) {
                tabbedPropertySheetPage = new TabbedPropertySheetPage(this);
            }
            return tabbedPropertySheetPage;
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        setSite(site);
        setInput(input);
        getCommandStack().addCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        createActions();
    }

    @Override
    protected void createGraphicalViewer(Composite parent) {
        IEditorSite editorSite = getEditorSite();
        StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(editorSite);
        GraphicalViewer viewer = new ValidationEnabledGraphicalViewer(validationMessageHandler);
        viewer.createControl(parent);
        viewer.getControl().setBackground(ColorConstants.white);
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        viewer.addDropTargetListener(new DataEditDropTargetListener(viewer, diagram));
        viewer.setEditPartFactory(new DiagramEditPartFactory());
        getSite().setSelectionProvider(viewer);
        getEditDomain().addViewer(viewer);
        viewer.setContents(diagram);
        this.graphicalViewer = viewer;
    }

    @Override
    public GraphicalViewer getGraphicalViewer() {
        return graphicalViewer;
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        return new PaletteViewerCreator().createPaletteRoot();
    }

    @Override
    public void doSave(IProgressMonitor arg0) {
        try {
            GefEditorActivator.getDefault().getExtension(ext.getXmiNamespace()).saveDiagram(modelId, diagram);
        } catch (Exception ex) {
            throw new XMIRuntimeException(ex);
        }
    }

    public Diagram getDiagram() {
        return diagram;
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        super.selectionChanged(part, selection);
    }

    protected void setInput(IEditorInput input) {
        super.setInput(input);
        try {
            if (input instanceof ExtensionInput) {
                IStorage storage = ((ExtensionInput) input).getStorage();
                byte[] buffer = new byte[1024];
                int length = storage.getContents().read(buffer);
                String uri = new String(buffer, 0, length);
                modelId = uri.substring(0, uri.indexOf(" "));
                xmiId = uri.substring(uri.indexOf(" ") + 1, uri.length());
                ext = (XMIExtension) dao.getModelElement(modelId, xmiId, -1);
                diagram = GefEditorActivator.getDefault().getExtension(ext.getXmiNamespace()).diagramFactory(modelId, ext);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
        }
        assert (diagram != null) : "diagram is null.";
    }
}
