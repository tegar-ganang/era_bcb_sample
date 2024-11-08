package jp.ekasi.pms.ui.editors;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import jp.ekasi.common.ui.nebula.grid.DefaultColumnHeaderRenderer;
import jp.ekasi.pms.model.ModelPackage;
import jp.ekasi.pms.model.Project;
import jp.ekasi.pms.model.provider.ModelItemProviderAdapterFactory;
import jp.ekasi.pms.ui.ganttchart.gef.GEFGanttchartViewer;
import jp.ekasi.pms.ui.ganttchart.gef.editparts.GanttchartEditPartFactory;
import jp.ekasi.pms.ui.gef.emf.GefEmfEditDomain;
import jp.ekasi.pms.ui.grid.ProjectGridViewer;
import jp.ekasi.pms.ui.grid.providor.PmsAdapterFactoryContentProvider;
import jp.ekasi.pms.ui.grid.util.EAttributeTypeColumnLabelProvider;
import jp.ekasi.pms.ui.properties.tabbed.PmsTabbedPropertySheetPage;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.domain.IEditingDomainProvider;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryContentProvider;
import org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider;
import org.eclipse.emf.edit.ui.util.EditUIUtil;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.nebula.jface.gridviewer.GridTreeViewer;
import org.eclipse.nebula.jface.gridviewer.GridViewerColumn;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

/**
 * GantchartEditor.
 * @author Yuusuke Hikime
 */
public class GanttchartEditor extends GraphicalEditor implements IEditingDomainProvider, IMenuListener, ISelectionProvider, IViewerProvider, ITabbedPropertySheetPageContributor {

    /** Container */
    private Composite container;

    /** SelectionViewer */
    protected TreeViewer treeViewer;

    /** EMF AdapterFactoryEditingDomain */
    protected AdapterFactoryEditingDomain editingDomain;

    /** ComposedAdapterFactory */
    protected ComposedAdapterFactory adapterFactory;

    /** SelectionChangedListener. */
    protected ISelectionChangedListener selectionChangedListener;

    /**  */
    protected ISelection editorSelection = StructuredSelection.EMPTY;

    /**  */
    protected Collection<ISelectionChangedListener> selectionChangedListeners = new ArrayList<ISelectionChangedListener>();

    /** GRIDTREEVIEWER */
    protected ProjectGridViewer projectGridViewer;

    /** �A�E�g���C�� */
    protected IContentOutlinePage contentOutlinePage;

    protected TreeViewer contentOutlineViewer;

    protected TabbedPropertySheetPage propertySheetPage;

    /**
	 * �R���X�g���N�^.
	 */
    public GanttchartEditor() {
        super();
        adapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
        adapterFactory.addAdapterFactory(new ResourceItemProviderAdapterFactory());
        adapterFactory.addAdapterFactory(new ModelItemProviderAdapterFactory());
        adapterFactory.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());
        BasicCommandStack commandStack = new BasicCommandStack();
        editingDomain = new AdapterFactoryEditingDomain(adapterFactory, commandStack, new HashMap<Resource, Boolean>());
        setEditDomain(new GefEmfEditDomain(this, editingDomain));
    }

    @Override
    public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
        setSite(site);
        setInputWithNotify(editorInput);
        getCommandStack().addCommandStackListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        setPartName(editorInput.getName());
        site.setSelectionProvider(this);
        initializeActionRegistry();
    }

    @Override
    public void dispose() {
        adapterFactory.dispose();
        if (propertySheetPage != null) {
            propertySheetPage.dispose();
        }
        if (contentOutlinePage != null) {
            contentOutlinePage.dispose();
        }
        super.dispose();
    }

    /**
	 * ���f���̐���
	 */
    public void loadResources() {
        URI resourceURI = EditUIUtil.getURI(getEditorInput());
        Exception exception = null;
        Resource resource = null;
        try {
            resource = editingDomain.getResourceSet().getResource(resourceURI, true);
        } catch (Exception e) {
            exception = e;
            resource = editingDomain.getResourceSet().getResource(resourceURI, false);
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        loadResources();
        container = new Composite(parent, SWT.NONE);
        container.setLayout(new FillLayout());
        final SashForm sash = new SashForm(container, SWT.HORIZONTAL);
        createGridTreeViewer(sash);
        createGraphicalViewer(sash);
        getSite().setSelectionProvider(this);
    }

    /**
	 * �R���e�L�X�g���j���[�𐶐����܂��B.<br/>
	 * 
	 * @param ganttchartViewer
	 *            Viewer
	 */
    protected void createContextMenuFor(GridTreeViewer viewer) {
        MenuManager contextMenu = new MenuManager("#PopUp");
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.add(new Separator(org.eclipse.ui.IWorkbenchActionConstants.MB_ADDITIONS));
        contextMenu.addMenuListener(this);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        getSite().registerContextMenu(contextMenu, viewer);
    }

    /**
	 * �O���b�h�c���[�r���[���[�𐶐�����B
	 * 
	 * @param composite
	 *            �R���|�W�b�g
	 * @return �������ꂽ�O���b�h�c���[�r���[���[
	 */
    protected void createGridTreeViewer(Composite composite) {
        final ProjectGridViewer projectGridViewer = new ProjectGridViewer(composite, SWT.H_SCROLL);
        {
            Grid grid = projectGridViewer.getGrid();
            grid.setRowsResizeable(true);
            grid.setRowHeaderVisible(true);
            grid.setLinesVisible(true);
            grid.setHeaderVisible(true);
            grid.setCellSelectionEnabled(true);
        }
        this.setProjectGridViewer(projectGridViewer);
        final DefaultColumnHeaderRenderer headerRenderer = new DefaultColumnHeaderRenderer();
        EAttribute[] columnEAttribute = new EAttribute[] { ModelPackage.Literals.TASK__WBS, ModelPackage.Literals.TASK__NAME, ModelPackage.Literals.TASK__START, ModelPackage.Literals.TASK__FINISH };
        for (EAttribute attribute : columnEAttribute) {
            GridViewerColumn gridViewerColumn = new GridViewerColumn(projectGridViewer, SWT.NONE);
            GridColumn column = gridViewerColumn.getColumn();
            column.setText(attribute.getName());
            column.setMoveable(true);
            column.setResizeable(true);
            column.setHeaderRenderer(headerRenderer);
            gridViewerColumn.setLabelProvider(new EAttributeTypeColumnLabelProvider(attribute));
            switch(attribute.getFeatureID()) {
                case ModelPackage.TASK__WBS:
                    column.setWidth(40);
                    column.setAlignment(SWT.LEFT);
                    break;
                case ModelPackage.TASK__NAME:
                    column.setTree(true);
                    column.setWidth(120);
                    column.setAlignment(SWT.LEFT);
                    break;
                case ModelPackage.TASK__START:
                case ModelPackage.TASK__FINISH:
                    column.setWidth(80);
                    column.setAlignment(SWT.LEFT);
                    break;
                default:
                    break;
            }
        }
        configureProjectGridViewer(projectGridViewer);
        initializeProjectGridViewer(projectGridViewer);
    }

    /**
	 * ProjectGridViewer�̐ݒ�
	 * @param projectGridViewer
	 */
    protected void configureProjectGridViewer(ProjectGridViewer projectGridViewer) {
        projectGridViewer.setContentProvider(new AdapterFactoryContentProvider(new PmsAdapterFactoryContentProvider()));
    }

    /**
	 * ProjectGridViewer�̏���
	 * @param projectGridViewer
	 */
    protected void initializeProjectGridViewer(ProjectGridViewer projectGridViewer) {
        Project project = getProject();
        projectGridViewer.setProject(project);
        projectGridViewer.setSelection(new StructuredSelection(project), true);
        if (selectionChangedListener == null) {
            selectionChangedListener = new ISelectionChangedListener() {

                public void selectionChanged(SelectionChangedEvent selectionChangedEvent) {
                    setSelection(selectionChangedEvent.getSelection());
                }
            };
        }
        projectGridViewer.addSelectionChangedListener(selectionChangedListener);
        setSelection(new StructuredSelection(project));
    }

    @Override
    protected void createGraphicalViewer(Composite parent) {
        GEFGanttchartViewer viewer = new GEFGanttchartViewer();
        viewer.createControl(parent);
        setGraphicalViewer(viewer);
        configureGraphicalViewer();
        hookGraphicalViewer();
        initializeGraphicalViewer();
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(getProjectGridViewer());
    }

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new GanttchartEditPartFactory());
    }

    protected void createContextMenuFor(StructuredViewer viewer) {
        MenuManager contextMenu = new MenuManager("#PopUp");
        contextMenu.add(new Separator("additions"));
        contextMenu.setRemoveAllWhenShown(true);
        contextMenu.addMenuListener(this);
        Menu menu = contextMenu.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(contextMenu, viewer);
    }

    @Override
    public boolean isDirty() {
        return ((BasicCommandStack) editingDomain.getCommandStack()).isSaveNeeded();
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {
        getContainer().setFocus();
    }

    /**
	 * This is how the framework determines which interfaces we implement. <!--
	 * begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
    @SuppressWarnings("unchecked")
    @Override
    public Object getAdapter(Class key) {
        if (key.equals(IContentOutlinePage.class)) {
            return showOutlineView() ? getContentOutlinePage() : null;
        } else if (key.equals(IPropertySheetPage.class)) {
            return getPropertySheetPage();
        } else {
            return super.getAdapter(key);
        }
    }

    /**
	 * �R���e���c�A�E�g���C���y�[�W��Ԃ��܂��B �Ƃ肠�����̎���
	 */
    public IContentOutlinePage getContentOutlinePage() {
        if (contentOutlinePage == null) {
            class MyContentOutlinePage extends ContentOutlinePage {

                @Override
                public void createControl(Composite parent) {
                    super.createControl(parent);
                    contentOutlineViewer = getTreeViewer();
                    contentOutlineViewer.addSelectionChangedListener(this);
                    contentOutlineViewer.setContentProvider(new AdapterFactoryContentProvider(adapterFactory));
                    contentOutlineViewer.setLabelProvider(new AdapterFactoryLabelProvider(adapterFactory));
                    contentOutlineViewer.setInput(editingDomain.getResourceSet());
                    if (!editingDomain.getResourceSet().getResources().isEmpty()) {
                        contentOutlineViewer.setSelection(new StructuredSelection(editingDomain.getResourceSet().getResources().get(0)), true);
                    }
                }

                @Override
                public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
                    super.makeContributions(menuManager, toolBarManager, statusLineManager);
                }
            }
            contentOutlinePage = new MyContentOutlinePage();
        }
        return contentOutlinePage;
    }

    /**
	 * �v���p�e�B�V�[�g�𐶐����܂��B
	 * 
	 * @return �v���p�e�B�V�[�g��Ԃ��܂��B
	 */
    public TabbedPropertySheetPage getPropertySheetPage() {
        if (propertySheetPage == null || propertySheetPage.getControl() == null) {
            propertySheetPage = new PmsTabbedPropertySheetPage(this);
        }
        return propertySheetPage;
    }

    /**
	 * 
	 * @param resource
	 * @return
	 */
    protected boolean isPersisted(Resource resource) {
        boolean result = false;
        try {
            InputStream stream = editingDomain.getResourceSet().getURIConverter().createInputStream(resource.getURI());
            if (stream != null) {
                result = true;
                stream.close();
            }
        } catch (IOException e) {
        }
        return result;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    public void doSaveAs() {
    }

    /**
	 * �R���e�L�X�g���j���[��o�^���܂��B
	 */
    public void menuAboutToShow(IMenuManager manager) {
        ((IMenuListener) getEditorSite().getActionBarContributor()).menuAboutToShow(manager);
    }

    /**
	 * @return
	 */
    public IEditorActionBarContributor getActionBarContributor() {
        return (IEditorActionBarContributor) getEditorSite().getActionBarContributor();
    }

    /**
	 * @return EditingDomain��Ԃ��܂��B
	 */
    public EditingDomain getEditingDomain() {
        return editingDomain;
    }

    /**
	 * @return ���[�g�R���e�i��Ԃ��܂��B
	 */
    public Composite getContainer() {
        return container;
    }

    /**
	 * @return
	 */
    protected boolean showOutlineView() {
        return true;
    }

    /**
	 * 
	 */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.add(listener);
    }

    /**
	 * 
	 */
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListeners.remove(listener);
    }

    public ISelection getSelection() {
        return editorSelection;
    }

    public void setSelection(ISelection selection) {
        editorSelection = selection;
        for (ISelectionChangedListener listener : selectionChangedListeners) {
            listener.selectionChanged(new SelectionChangedEvent(this, selection));
        }
    }

    public GridTreeViewer getProjectGridViewer() {
        return projectGridViewer;
    }

    public void setProjectGridViewer(ProjectGridViewer projectGridViewer) {
        this.projectGridViewer = projectGridViewer;
    }

    /**
	 * @return projectGridViewer��Ԃ��B
	 */
    public Viewer getViewer() {
        return projectGridViewer;
    }

    /**
	 * @return Project��Ԃ��܂��B
	 */
    protected Project getProject() {
        Project project;
        Resource resource = (Resource) editingDomain.getResourceSet().getResources().get(0);
        project = (Project) resource.getContents().get(0);
        return project;
    }

    /**
	 * @return return contribute id
	 */
    public String getContributorId() {
        return "jp.ekasi.pms.ui.properties.tabbed";
    }

    /**
	 * @return adapterFactory
	 */
    public AdapterFactory getAdapterFactory() {
        return adapterFactory;
    }
}
