package org.musicnotation.gef.ui.views.movements;

import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.util.ui.AdvancedTreeViewer;
import org.eclipse.gef.util.ui.BasePage;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.musicnotation.gef.editparts.movements.MovementsTreeEditPartFactory;
import org.musicnotation.model.Movement;

public class MovementsPage extends BasePage {

    private static final AdvancedTreeViewer tree = new AdvancedTreeViewer();

    public MovementsPage(EditDomain editDomain, Object contents, final GraphicalViewer viewer) {
        super(tree, editDomain, contents);
        tree.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    if (selection.getFirstElement() instanceof Movement) {
                        Movement movement = (Movement) selection.getFirstElement();
                        viewer.setContents(movement);
                    }
                }
            }
        });
    }

    @Override
    protected EditPartFactory getEditPartFactory() {
        return new MovementsTreeEditPartFactory();
    }
}
