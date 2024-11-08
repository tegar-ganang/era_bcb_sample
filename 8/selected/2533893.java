package org.dengues.reports.editor.forms;

import org.dengues.core.DenguesCorePlugin;
import org.dengues.core.warehouse.ENodeStatus;
import org.dengues.core.warehouse.IWarehouseNode;
import org.dengues.core.warehouse.IWarehouseView;
import org.dengues.reports.DenguesReportsPlugin;
import org.dengues.reports.editor.design.GraphicalPartFactory;
import org.dengues.reports.editor.design.RepDesignBarContributor;
import org.dengues.reports.editor.design.ReportDesignPaletteFactory;
import org.dengues.reports.editor.design.ReportRootEditPart;
import org.dengues.reports.editor.design.ReportsEditorInput;
import org.dengues.reports.editor.design.models.ReportsDiagram;
import org.dengues.reports.editor.design.ruler.RepEditorRulerProvider;
import org.dengues.ui.editors.AbstractEditorInput;
import org.dengues.ui.editors.GEFEditorUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.AlignmentAction;
import org.eclipse.gef.ui.actions.MatchHeightAction;
import org.eclipse.gef.ui.actions.MatchWidthAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 2007-12-13 qiang.zhang $
 * 
 */
public class ReportDesignEditor extends GraphicalEditorWithFlyoutPalette {

    private GraphicalPartFactory editPartFactoy;

    private ReportsDiagram reportsDiagram;

    private RulerComposite rulerComp;

    private RepEditorRulerProvider topRuler;

    private RepEditorRulerProvider leftRuler;

    /**
     * Qiang.Zhang.Adolf@gmail.com ReportDesignEditor constructor comment.
     */
    public ReportDesignEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    @Override
    protected PaletteRoot getPaletteRoot() {
        return ReportDesignPaletteFactory.getPaletteRoot();
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        ScrollingGraphicalViewer viewer = (ScrollingGraphicalViewer) getGraphicalViewer();
        ReportRootEditPart root = new ReportRootEditPart();
        viewer.setRootEditPart(root);
        getEditDomain().addViewer(viewer);
        getSite().setSelectionProvider(viewer);
        viewer.setEditPartFactory(getEditPartFactory());
        createRulers();
    }

    /**
     * Gets default edit part factory.
     */
    protected EditPartFactory getEditPartFactory() {
        if (editPartFactoy == null) {
            editPartFactoy = new GraphicalPartFactory();
        }
        return editPartFactoy;
    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInput(input);
        if (input instanceof ReportsEditorInput) {
            ReportsEditorInput editorInput = (ReportsEditorInput) input;
            reportsDiagram = editorInput.getReportsDiagram();
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getAction".
     * 
     * @param id
     * @return
     */
    public IAction getAction(String id) {
        return getActionRegistry().getAction(id);
    }

    @Override
    protected void initializeGraphicalViewer() {
        super.initializeGraphicalViewer();
        getGraphicalViewer().setContents(reportsDiagram);
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    protected void createGraphicalViewer(Composite parent) {
        rulerComp = new RulerComposite(parent, SWT.NONE);
        super.createGraphicalViewer(rulerComp);
        rulerComp.setGraphicalViewer((ScrollingGraphicalViewer) getGraphicalViewer());
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "createRulers".
     */
    private void createRulers() {
        if (topRuler == null) {
            topRuler = new RepEditorRulerProvider(reportsDiagram, true);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, topRuler);
        if (leftRuler == null) {
            leftRuler = new RepEditorRulerProvider(reportsDiagram, false);
        }
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, leftRuler);
        getGraphicalViewer().setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, new Boolean(reportsDiagram.isRulerVisibility()));
    }

    @Override
    protected void createActions() {
        super.createActions();
        for (int i : RepDesignBarContributor.ALIG_ACTION_IDS) {
            AlignmentAction alignmentAction = new AlignmentAction((IWorkbenchPart) this, i);
            getActionRegistry().registerAction(alignmentAction);
        }
        MatchHeightAction heightAction = new MatchHeightAction(this);
        getActionRegistry().registerAction(heightAction);
        MatchWidthAction widthAction = new MatchWidthAction(this);
        getActionRegistry().registerAction(widthAction);
    }

    protected Control getGraphicalControl() {
        return rulerComp;
    }

    @Override
    protected FlyoutPreferences getPalettePreferences() {
        return GEFEditorUtils.createPalettePreferences(DenguesReportsPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void dispose() {
        if (getEditorInput() instanceof ReportsEditorInput) {
            ReportsEditorInput processEditorInput = ((ReportsEditorInput) getEditorInput());
            IWarehouseNode warehouseNode = processEditorInput.getWarehouseNode();
            if (warehouseNode != null) {
                warehouseNode.setNodeStatus(ENodeStatus.NORMAL);
                refreshWarehouseView();
            }
        }
        super.dispose();
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "refreshWarehouseView".
     */
    protected void refreshWarehouseView() {
        IViewPart findView = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IWarehouseView.VIEW_ID);
        if (findView instanceof IWarehouseView) {
            ((IWarehouseView) findView).refresh(((AbstractEditorInput) getEditorInput()).getWarehouseNode().getParent());
        }
    }

    /**
     * Qiang.Zhang.Adolf@gmail.com Comment method "getReportTitle".
     * 
     * @return
     */
    public String getReportTitle() {
        return DenguesCorePlugin.getDefault().getDenguesTitle(((AbstractEditorInput) getEditorInput()).getFile().getFullPath().toPortableString().substring(1));
    }
}
