package org.dengues.ui.editors;

import org.dengues.core.process.ICompNode;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.tools.PanningSelectionTool;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf Qiang.Zhang.Adolf@gmail.com 2008-4-16 qiang.zhang $
 * 
 */
public class EmptyGraphicalEditor extends GraphicalEditor {

    private KeyHandler sharedKeyHandler;

    protected RulerComposite rulerComp;

    protected ScrollingGraphicalViewer viewer;

    protected EmptyRootEditPart root;

    protected KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));
        }
        return sharedKeyHandler;
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com EmptyGraphicalEditor constructor comment.
     */
    public EmptyGraphicalEditor() {
        DefaultEditDomain editDomain = new DefaultEditDomain(this);
        editDomain.setDefaultTool(new PanningSelectionTool());
        setEditDomain(editDomain);
    }

    @Override
    protected void initializeGraphicalViewer() {
        viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        root = new EmptyRootEditPart();
        viewer.setRootEditPart(root);
        getGraphicalViewer().setKeyHandler(new GraphicalViewerKeyHandler(getGraphicalViewer()).setParent(getCommonKeyHandler()));
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    protected void createGraphicalViewer(final Composite parent) {
        rulerComp = new RulerComposite(parent, SWT.BORDER);
        super.createGraphicalViewer(rulerComp);
        rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
    }

    public Control getGraphicalControl() {
        rulerComp.setLayoutData(new GridData(GridData.FILL_BOTH));
        return rulerComp;
    }

    protected void hookGraphicalViewer() {
        getSelectionSynchronizer().addViewer(getGraphicalViewer());
    }

    /**
     * Getter for viewer.
     * 
     * @return the viewer
     */
    public ScrollingGraphicalViewer getViewer() {
        return this.viewer;
    }

    /**
     * Sets the compNode.
     * 
     * @param compNode the compNode to set
     */
    public void setCompNode(ICompNode compNode) {
        root.setCompNode(compNode);
    }
}
