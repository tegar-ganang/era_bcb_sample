package org.ist.contract.editor;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.ist.contract.ContractUIPlugin;
import org.ist.contract.actions.ContractContextMenuProvider;
import org.ist.contract.edit.GraphicalEditPartsFactory;
import org.ist.contract.store.core.i18n.Messages;
import org.ist.contract.store.core.i18n.MessagesConstants;

/**
 * Contract editor page.
 */
public class ContractPage extends AbstractEditorPage {

    /**
	 * Creates a new Contract editor page instance.
	 * 
	 * <p>By design this page uses its own <code>EditDomain</code>.
	 * The main goal of this approach is that this page has its own
	 * undo/redo command stack.
	 * 
	 * @param parent the parent multi page editor 
	 */
    public ContractPage(ContractEditor parent) {
        super(parent, new EditDomain());
    }

    protected String getPageName() {
        return Messages.getMessage(ContractUIPlugin.I18N_LOCATION, MessagesConstants.CONTRACT_EDITOR_PAGE_NAME);
    }

    protected void createPageControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setBackground(parent.getBackground());
        composite.setLayout(new GridLayout(2, false));
        createPaletteViewer(composite);
        GridData gd = new GridData(GridData.FILL_VERTICAL);
        gd.widthHint = 125;
        getPaletteViewer().getControl().setLayoutData(gd);
        createGraphicalViewer(composite);
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 275;
        getGraphicalViewer().getControl().setLayoutData(gd);
    }

    /** the graphical viewer */
    private GraphicalViewer viewer;

    /**
	 * Creates the GraphicalViewer on the specified <code>Composite</code>.
	 * @param parent the parent composite
	 */
    private void createGraphicalViewer(Composite parent) {
        viewer = new ScrollingGraphicalViewer();
        viewer.createControl(parent);
        viewer.getControl().setBackground(parent.getBackground());
        viewer.setRootEditPart(new ScalableFreeformRootEditPart());
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        registerEditPartViewer(viewer);
        configureEditPartViewer(viewer);
        viewer.setEditPartFactory(new GraphicalEditPartsFactory(getSite().getShell()));
        viewer.setContents(getContractEditor().getContract());
        ContextMenuProvider provider = new ContractContextMenuProvider(getGraphicalViewer(), getContractEditor().getActionRegistry());
        getGraphicalViewer().setContextMenu(provider);
        getSite().registerContextMenu(provider, getGraphicalViewer());
    }

    protected GraphicalViewer getGraphicalViewer() {
        return viewer;
    }
}
