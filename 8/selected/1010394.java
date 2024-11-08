package tec.stan.ling.topo.graph;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IEditorInput;
import tec.stan.ling.core.top.model.Subnet;
import tec.stan.ling.topo.graph.editpart.NEEditPartFactory;

/**
 * 
 * 
 * @author Stan 张新潮
 * @since 2009-12-31
 */
public class TopoViewEditor extends GraphicalEditor {

    public static final String ID = "tec.stan.ling.topo.graph.TopoViewEditor";

    private Subnet currentSubnet = null;

    public TopoViewEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout(1, false);
        layout.marginBottom = 0;
        layout.marginHeight = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.marginTop = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;
        parent.setLayout(layout);
        Composite listComposite = new Composite(parent, SWT.NONE);
        GridData listGD = new GridData(GridData.FILL_HORIZONTAL);
        listComposite.setLayoutData(listGD);
        createPathBar(listComposite);
        Composite editorComposite = new Composite(parent, SWT.NONE);
        GridData editorGD = new GridData(GridData.FILL_BOTH);
        editorComposite.setLayoutData(editorGD);
        editorComposite.setLayout(new FillLayout());
        super.createPartControl(editorComposite);
    }

    private void createPathBar(Composite parent) {
        GridLayout layout = new GridLayout(2, false);
        parent.setLayout(layout);
        Label label = new Label(parent, SWT.NONE);
        label.setText("Path: ");
        label.setLayoutData(new GridData(GridData.FILL_VERTICAL));
        Link link = new Link(parent, SWT.NONE);
        link.setText("Topo > Subnet1 > NE1 ");
        link.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    @Override
    protected void initializeGraphicalViewer() {
        getGraphicalViewer().setContents(currentSubnet);
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof TopoEditInput) {
            this.currentSubnet = ((TopoEditInput) input).getObject();
        }
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        ScalableFreeformRootEditPart root = new ScalableFreeformRootEditPart();
        viewer.setRootEditPart(root);
        viewer.setEditPartFactory(new NEEditPartFactory());
    }
}
