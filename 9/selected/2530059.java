package com.ivis.xprocess.ui.processdesigner.actions;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import com.ivis.xprocess.ui.UIType;
import com.ivis.xprocess.ui.diagram.editor.DiagramEditor;
import com.ivis.xprocess.ui.processdesigner.diagram.dialogs.OverviewDialog;
import com.ivis.xprocess.ui.properties.ProcessDesignerMessages;

public class OverviewAction extends WorkbenchPartAction {

    private OverviewDialog myPopupDialog;

    private GraphicalViewer myGraphicalViewer;

    public OverviewAction(final IWorkbenchPart editPart, GraphicalViewer viewer) {
        super(editPart, AS_PUSH_BUTTON);
        setImageDescriptor(UIType.overview.getImageDescriptor());
        setText(ProcessDesignerMessages.OverviewAction_showOverview);
        setToolTipText(getText());
        myGraphicalViewer = viewer;
        myPopupDialog = new OverviewDialog(myGraphicalViewer);
        editPart.getSite().getPage().addPartListener(new IPartListener() {

            public void partActivated(IWorkbenchPart part) {
                if (part.equals(editPart)) {
                    setActiveEditor((DiagramEditor) part);
                    if (myPopupDialog.isVisible()) {
                        run();
                    }
                }
            }

            public void partBroughtToTop(IWorkbenchPart part) {
            }

            public void partClosed(IWorkbenchPart part) {
            }

            public void partDeactivated(IWorkbenchPart part) {
                if (part.equals(editPart)) {
                    if ((myPopupDialog != null) && myPopupDialog.isOpened()) {
                        myPopupDialog.close();
                    }
                }
            }

            public void partOpened(IWorkbenchPart part) {
            }
        });
    }

    @Override
    public void run() {
        if (myPopupDialog.isOpened()) {
            myPopupDialog.getContents().setFocus();
            return;
        }
        myPopupDialog.create();
        myPopupDialog.open();
    }

    @Override
    protected boolean calculateEnabled() {
        return true;
    }

    public void setActiveEditor(DiagramEditor editor) {
        if (myPopupDialog != null) {
            myPopupDialog.setInput(myGraphicalViewer);
        }
    }
}
