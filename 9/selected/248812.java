package org.fh.auge.chart.views;

import model.Block;
import model.Chart;
import model.ChartObject;
import model.DateAxis;
import model.DateRange;
import model.EMA;
import model.MACD;
import model.StockLine;
import model.cmd.BlockCreateCommand;
import model.cmd.DataSetCreateCommand;
import model.parts.ChartEditPartFactory;
import model.ui.BlockContextMenuProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.fh.auge.chart.model.Data;

public class SampleView extends ViewPart implements ISelectionListener {

    private GraphicalViewer viewer;

    private UndoAction undoAction;

    private RedoAction redoAction;

    private DeleteAction deleteAction;

    private Action emaAction;

    private Chart chart;

    private Slider slider;

    private Form form;

    /**
	 * The constructor.
	 */
    public SampleView() {
    }

    /**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
    public void createPartControl(Composite parent) {
        FormToolkit toolkit;
        toolkit = new FormToolkit(parent.getDisplay());
        form = toolkit.createForm(parent);
        form.setText("Apple Inc.");
        toolkit.decorateFormHeading(form);
        form.getBody().setLayout(new GridLayout());
        chart = createChart();
        final DateAxis dateAxis = new DateAxis();
        viewer = new GraphicalViewerImpl();
        viewer.setRootEditPart(new ScalableRootEditPart());
        viewer.setEditPartFactory(new ChartEditPartFactory(dateAxis));
        viewer.createControl(form.getBody());
        viewer.setContents(chart);
        viewer.setEditDomain(new EditDomain());
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                System.err.println("selectionChanged " + event.getSelection());
            }
        });
        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                deleteAction.update();
            }
        });
        ActionRegistry actionRegistry = new ActionRegistry();
        createActions(actionRegistry);
        ContextMenuProvider cmProvider = new BlockContextMenuProvider(viewer, actionRegistry);
        viewer.setContextMenu(cmProvider);
        getSite().setSelectionProvider(viewer);
        deleteAction.setSelectionProvider(viewer);
        viewer.getEditDomain().getCommandStack().addCommandStackEventListener(new CommandStackEventListener() {

            public void stackChanged(CommandStackEvent event) {
                undoAction.setEnabled(viewer.getEditDomain().getCommandStack().canUndo());
                redoAction.setEnabled(viewer.getEditDomain().getCommandStack().canRedo());
            }
        });
        Data data = Data.getData();
        chart.setInput(data);
        DateRange dateRange = new DateRange(0, 50);
        dateAxis.setDates(data.date);
        dateAxis.setSelectedRange(dateRange);
        slider = new Slider(form.getBody(), SWT.NONE);
        slider.setMinimum(0);
        slider.setMaximum(data.close.length - 1);
        slider.setSelection(dateRange.start);
        slider.setThumb(dateRange.length);
        slider.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                DateRange r = new DateRange(slider.getSelection(), slider.getThumb());
                dateAxis.setSelectedRange(r);
            }
        });
        final Scale spinner = new Scale(form.getBody(), SWT.NONE);
        spinner.setMinimum(5);
        spinner.setMaximum(data.close.length - 1);
        spinner.setSelection(dateRange.length);
        spinner.addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                slider.setThumb(spinner.getSelection());
                DateRange r = new DateRange(slider.getSelection(), slider.getThumb());
                dateAxis.setSelectedRange(r);
            }
        });
        GridDataFactory.defaultsFor(viewer.getControl()).grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(viewer.getControl());
        GridDataFactory.defaultsFor(slider).grab(true, false).align(GridData.FILL, GridData.FILL).grab(true, false).applyTo(slider);
        GridDataFactory.defaultsFor(spinner).grab(true, false).align(GridData.FILL, GridData.FILL).grab(true, false).applyTo(spinner);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
    }

    private Chart createChart() {
        Chart c = new Chart();
        Block block = new Block(c);
        block.setName("Test");
        EMA ema30 = new EMA(block, 30, ColorConstants.green);
        EMA ema10 = new EMA(block, 10, ColorConstants.red);
        block.getDataSets().add(new StockLine(block));
        block.getDataSets().add(ema30);
        block.getDataSets().add(ema10);
        Block block1 = new Block(c);
        block1.setName("11");
        block1.getDataSets().add(new MACD(block1));
        Block block2 = new Block(c);
        block2.setName("22");
        block2.getDataSets().add(new StockLine(block2));
        c.addChild(block);
        for (Block bb : c.getBlocks()) {
            System.err.println("Block " + bb);
            for (ChartObject indic : bb.getDataSets()) {
                System.err.println("Indic" + indic);
            }
        }
        return c;
    }

    /**
	 * Passing the focus request to the viewer's control.
	 */
    public void setFocus() {
    }

    protected void createActions(ActionRegistry actionRegistry) {
        ActionRegistry registry = actionRegistry;
        undoAction = new UndoAction(this);
        registry.registerAction(undoAction);
        redoAction = new RedoAction(this);
        registry.registerAction(redoAction);
        deleteAction = new DeleteAction(this);
        registry.registerAction(deleteAction);
        emaAction = new Action("EMA") {

            @Override
            public void run() {
                EMA ema = new EMA(chart.getBlocks().get(0), 15, ColorConstants.cyan);
                DataSetCreateCommand cmd = new DataSetCreateCommand(ema, chart.getBlocks().get(0), null);
                System.err.println("run " + getId() + cmd.canExecute());
                viewer.getEditDomain().getCommandStack().execute(cmd);
            }
        };
        emaAction.setId("add.ema");
        Action macdAction = new Action("MACD") {

            @Override
            public void run() {
                Block b = new Block(chart);
                b.setPreferredHeight(150);
                b.setMargin(10);
                b.addChild(new MACD(b));
                BlockCreateCommand cmd = new BlockCreateCommand(b, chart);
                viewer.getEditDomain().getCommandStack().execute(cmd);
            }
        };
        macdAction.setId("add.macd");
        registry.registerAction(emaAction);
        registry.registerAction(macdAction);
    }

    @Override
    public Object getAdapter(Class adapter) {
        if (adapter == CommandStack.class) {
            return viewer.getEditDomain().getCommandStack();
        }
        if (adapter == IPropertySheetPage.class) {
        }
        return super.getAdapter(adapter);
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            IStructuredSelection ss = (IStructuredSelection) selection;
            if (ss.getFirstElement() instanceof IFile) {
                IFile resource = (IFile) ss.getFirstElement();
                System.err.println(selection);
                try {
                    Data data = Data.getData(resource.getContents());
                    chart.setInput(data);
                    form.setText(resource.getName());
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
