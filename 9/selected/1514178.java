package org.fh.auge.chart.model;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.internal.Workbench;

public class CopyOftest1 extends Composite {

    /**
     * This method initializes canvas
     * 
     */
    private void createCanvas() {
        GraphicalViewer viewer = new ScrollingGraphicalViewer();
        viewer.setRootEditPart(new ScalableRootEditPart());
        viewer.setEditPartFactory(new BlockEditPartFactory());
        viewer.createControl(this);
        viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
        ActionRegistry actionRegistry = new ActionRegistry();
        createActions(actionRegistry);
        ContextMenuProvider cmProvider = new BlockContextMenuProvider(viewer, actionRegistry);
        viewer.setContextMenu(cmProvider);
        Block b = new Block();
        b.addChild(new ChartItem());
        viewer.setContents(b);
        PaletteViewer paletteViewer = new PaletteViewer();
        paletteViewer.createControl(this);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        Display display = Display.getDefault();
        Shell shell = new Shell(display);
        shell.setLayout(new FillLayout());
        shell.setSize(new Point(600, 500));
        new CopyOftest1(shell, SWT.NONE);
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
        display.dispose();
    }

    public CopyOftest1(Composite parent, int style) {
        super(parent, style);
        initialize();
    }

    private void initialize() {
        createCanvas();
        setLayout(new FillLayout());
    }

    protected void createActions(ActionRegistry actionRegistry) {
        ActionRegistry registry = actionRegistry;
        IAction action;
    }
}
