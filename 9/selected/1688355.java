package de.objectcode.time4u.client.views;

import java.util.Calendar;
import java.util.Collection;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.editparts.ScalableRootEditPart;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import de.objectcode.time4u.client.Activator;
import de.objectcode.time4u.client.ICommandIds;
import de.objectcode.time4u.client.controls.ComboViewerCellEditor;
import de.objectcode.time4u.client.controls.TimeComboCellEditor;
import de.objectcode.time4u.client.dnd.TaskTransfer;
import de.objectcode.time4u.client.provider.TaskContentProvider;
import de.objectcode.time4u.client.provider.TaskLabelProvider;
import de.objectcode.time4u.client.provider.WorkItemContentProvider;
import de.objectcode.time4u.client.provider.WorkItemTableLabelProvider;
import de.objectcode.time4u.client.provider.WorkItemTreeContentProvider;
import de.objectcode.time4u.client.provider.WorkItemTreeLabelProvider;
import de.objectcode.time4u.client.views.parts.DayGraphEditPartFactory;
import de.objectcode.time4u.client.views.parts.ModelSelectionManager;
import de.objectcode.time4u.store.Day;
import de.objectcode.time4u.store.IProjectStore;
import de.objectcode.time4u.store.IRepositoryEvent;
import de.objectcode.time4u.store.IRepositoryListener;
import de.objectcode.time4u.store.IWorkItemStore;
import de.objectcode.time4u.store.Project;
import de.objectcode.time4u.store.RepositoryEventType;
import de.objectcode.time4u.store.Task;
import de.objectcode.time4u.store.WorkItem;

public class WorkItemView extends ViewPart implements IRepositoryListener, ISelectionListener {

    public static final String ID = "time4u_client.workItemListView";

    public enum ViewType {

        HIERARCHICAL, FLAT, DAYGRAPH
    }

    ;

    private Project m_selectedProject;

    private Task m_selectedTask;

    private Calendar m_selectedDay;

    private TableViewer m_tableViewer;

    private TreeViewer m_treeViewer;

    private GraphicalViewer m_dayGraphViewer;

    private PageBook m_pageBook;

    private IProjectStore m_projectStore;

    private IWorkItemStore m_workItemStore;

    private CompoundSelectionProvider m_selectionProvider;

    private ViewType m_activeViewType;

    @Override
    public void createPartControl(Composite parent) {
        m_selectionProvider = new CompoundSelectionProvider();
        getSite().setSelectionProvider(m_selectionProvider);
        m_projectStore = Activator.getDefault().getRepository().getProjectStore();
        m_workItemStore = Activator.getDefault().getRepository().getWorkItemStore();
        m_pageBook = new PageBook(parent, SWT.NONE);
        m_tableViewer = new TableViewer(m_pageBook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        TableLayout layout = new TableLayout();
        layout.addColumnData(new ColumnWeightData(10, 50, true));
        layout.addColumnData(new ColumnWeightData(10, 50, true));
        layout.addColumnData(new ColumnWeightData(10, 50, true));
        layout.addColumnData(new ColumnWeightData(15, 50, true));
        layout.addColumnData(new ColumnWeightData(15, 50, true));
        layout.addColumnData(new ColumnWeightData(40, 100, true));
        m_tableViewer.getTable().setHeaderVisible(true);
        m_tableViewer.getTable().setLinesVisible(true);
        m_tableViewer.getTable().setLayout(layout);
        TableColumn beginColumn = new TableColumn(m_tableViewer.getTable(), SWT.LEFT);
        beginColumn.setText("Begin");
        beginColumn.setMoveable(true);
        TableColumn endColumn = new TableColumn(m_tableViewer.getTable(), SWT.LEFT);
        endColumn.setText("End");
        endColumn.setMoveable(true);
        TableColumn durationColumn = new TableColumn(m_tableViewer.getTable(), SWT.LEFT);
        durationColumn.setText("Duration");
        durationColumn.setMoveable(true);
        TableColumn projectColumn = new TableColumn(m_tableViewer.getTable(), SWT.LEFT);
        projectColumn.setText("Project");
        projectColumn.setMoveable(true);
        TableColumn todoColumn = new TableColumn(m_tableViewer.getTable(), SWT.LEFT);
        todoColumn.setText("Task");
        todoColumn.setMoveable(true);
        TableColumn commentColumn = new TableColumn(m_tableViewer.getTable(), SWT.LEFT);
        commentColumn.setText("Comment");
        commentColumn.setMoveable(true);
        m_tableViewer.setColumnProperties(new String[] { "begin", "end", "duration", "project", "task", "comment" });
        m_tableViewer.setContentProvider(new WorkItemContentProvider(m_workItemStore));
        m_tableViewer.setLabelProvider(new WorkItemTableLabelProvider(m_projectStore, false));
        m_selectedDay = Calendar.getInstance();
        m_tableViewer.setInput(m_selectedDay);
        m_tableViewer.setCellEditors(new CellEditor[] { new TimeComboCellEditor(m_tableViewer.getTable()), new TimeComboCellEditor(m_tableViewer.getTable()), null, null, new ComboViewerCellEditor(m_tableViewer.getTable(), new TaskContentProvider(m_projectStore, false), new TaskLabelProvider()), new TextCellEditor(m_tableViewer.getTable()) });
        m_tableViewer.setCellModifier(new WorkItemCellModifier());
        m_tableViewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_DEFAULT, new Transfer[] { TaskTransfer.getInstance() }, new DropTargetAdapter() {

            @Override
            public void drop(DropTargetEvent event) {
                if ((event.data == null) || !(event.data instanceof TaskTransfer.ProjectTask)) {
                    return;
                }
                doDropTask((TaskTransfer.ProjectTask) event.data);
            }
        });
        m_treeViewer = new TreeViewer(m_pageBook, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
        layout = new TableLayout();
        layout.addColumnData(new ColumnWeightData(30, 50, true));
        layout.addColumnData(new ColumnWeightData(10, 50, true));
        layout.addColumnData(new ColumnWeightData(10, 50, true));
        layout.addColumnData(new ColumnWeightData(10, 50, true));
        layout.addColumnData(new ColumnWeightData(40, 100, true));
        m_treeViewer.getTree().setLayout(layout);
        m_treeViewer.getTree().setHeaderVisible(true);
        m_treeViewer.getTree().setLinesVisible(true);
        TreeColumn projectTaskColumn = new TreeColumn(m_treeViewer.getTree(), SWT.LEFT);
        projectTaskColumn.setText("Project / Task");
        TreeColumn tBeginColumn = new TreeColumn(m_treeViewer.getTree(), SWT.LEFT);
        tBeginColumn.setText("Begin");
        TreeColumn tEndColumn = new TreeColumn(m_treeViewer.getTree(), SWT.LEFT);
        tEndColumn.setText("End");
        TreeColumn tDurationColumn = new TreeColumn(m_treeViewer.getTree(), SWT.LEFT);
        tDurationColumn.setText("Duration");
        TreeColumn tCommentColumn = new TreeColumn(m_treeViewer.getTree(), SWT.LEFT);
        tCommentColumn.setText("Comment");
        m_treeViewer.setColumnProperties(new String[] { "projectTask", "begin", "end", "duration", "comment" });
        m_treeViewer.setContentProvider(new WorkItemTreeContentProvider(m_projectStore, m_workItemStore));
        m_treeViewer.setLabelProvider(new WorkItemTreeLabelProvider(m_projectStore));
        MenuManager menuMgr = new MenuManager();
        menuMgr.add(new GroupMarker("newGroup"));
        menuMgr.add(new Separator());
        menuMgr.add(new GroupMarker("objectGroup"));
        menuMgr.add(new Separator());
        menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        Menu menu = menuMgr.createContextMenu(m_tableViewer.getControl());
        m_tableViewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, getSite().getPage().findView(TaskListView.ID).getSite().getSelectionProvider());
        getSite().registerContextMenu(menuMgr, m_tableViewer);
        m_tableViewer.addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                try {
                    ICommandService commandService = (ICommandService) getSite().getWorkbenchWindow().getWorkbench().getService(ICommandService.class);
                    Command command = commandService.getCommand(ICommandIds.CMD_WORKITEM_EDIT);
                    command.executeWithChecks(new ExecutionEvent());
                } catch (Exception e) {
                    Activator.getDefault().log(e);
                }
            }
        });
        m_treeViewer.addDoubleClickListener(new IDoubleClickListener() {

            public void doubleClick(DoubleClickEvent event) {
                try {
                    ICommandService commandService = (ICommandService) getSite().getWorkbenchWindow().getWorkbench().getService(ICommandService.class);
                    Command command = commandService.getCommand(ICommandIds.CMD_WORKITEM_EDIT);
                    command.executeWithChecks(new ExecutionEvent());
                } catch (Exception e) {
                    Activator.getDefault().log(e);
                }
            }
        });
        m_dayGraphViewer = new ScrollingGraphicalViewer();
        m_dayGraphViewer.createControl(m_pageBook);
        m_dayGraphViewer.setEditPartFactory(new DayGraphEditPartFactory(Activator.getDefault().getRepository()));
        m_dayGraphViewer.setRootEditPart(new ScalableRootEditPart());
        m_dayGraphViewer.setSelectionManager(new ModelSelectionManager());
        m_dayGraphViewer.setEditDomain(new EditDomain());
        m_dayGraphViewer.addDropTargetListener(new AbstractTransferDropTargetListener(m_dayGraphViewer, TaskTransfer.getInstance()) {

            @Override
            protected Request createTargetRequest() {
                return new CreateRequest();
            }

            @Override
            protected void updateTargetRequest() {
                CreateRequest request = (CreateRequest) getTargetRequest();
                request.setLocation(getDropLocation());
                if ((getCurrentEvent().data != null) && (getCurrentEvent().data instanceof TaskTransfer.ProjectTask)) {
                    final TaskTransfer.ProjectTask projectTask = (TaskTransfer.ProjectTask) getCurrentEvent().data;
                    request.setFactory(new CreationFactory() {

                        public Object getNewObject() {
                            WorkItem workItem = new WorkItem();
                            workItem.setProjectId(projectTask.getProject().getId());
                            workItem.setTaskId(projectTask.getTask().getId());
                            workItem.setComment("");
                            return workItem;
                        }

                        public Object getObjectType() {
                            return null;
                        }
                    });
                }
            }
        });
        Activator.getDefault().getRepository().addRepositoryListener(RepositoryEventType.PROJECT, this);
        Activator.getDefault().getRepository().addRepositoryListener(RepositoryEventType.TASK, this);
        Activator.getDefault().getRepository().addRepositoryListener(RepositoryEventType.WORKITEM, this);
        m_pageBook.showPage(m_tableViewer.getTable());
        m_activeViewType = ViewType.FLAT;
        m_selectionProvider.setSelectionProvider(m_tableViewer);
        getSite().getPage().addSelectionListener(ProjectTreeView.ID, this);
        getSite().getPage().addSelectionListener(TaskListView.ID, this);
        getSite().getPage().addSelectionListener(CalendarView.ID, this);
    }

    public void setViewType(ViewType viewType) {
        switch(viewType) {
            case HIERARCHICAL:
                {
                    m_pageBook.showPage(m_treeViewer.getTree());
                    m_activeViewType = viewType;
                    m_selectionProvider.setSelectionProvider(m_treeViewer);
                    m_treeViewer.setInput(m_selectedDay);
                    m_tableViewer.setInput(null);
                    m_treeViewer.expandAll();
                    break;
                }
            case FLAT:
                {
                    m_pageBook.showPage(m_tableViewer.getTable());
                    m_activeViewType = viewType;
                    m_selectionProvider.setSelectionProvider(m_tableViewer);
                    m_tableViewer.setInput(m_selectedDay);
                    m_treeViewer.setInput(null);
                    break;
                }
            case DAYGRAPH:
                {
                    m_pageBook.showPage(m_dayGraphViewer.getControl());
                    m_activeViewType = viewType;
                    m_selectionProvider.setSelectionProvider(m_dayGraphViewer);
                    m_dayGraphViewer.setContents(new Day(m_selectedDay));
                    break;
                }
        }
    }

    @Override
    public void setFocus() {
        m_pageBook.setFocus();
    }

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(ProjectTreeView.ID, this);
        getSite().getPage().removeSelectionListener(TaskListView.ID, this);
        getSite().getPage().removeSelectionListener(CalendarView.ID, this);
        Activator.getDefault().getRepository().removeRepositoryListener(this);
        super.dispose();
    }

    public WorkItem getSelectedWorkItem() {
        ISelection selection = m_selectionProvider.getSelection();
        if ((selection != null) && (selection instanceof IStructuredSelection)) {
            Object obj = ((IStructuredSelection) selection).getFirstElement();
            if ((obj != null) && (obj instanceof WorkItem)) {
                return (WorkItem) obj;
            }
        }
        return null;
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if ((part instanceof ProjectTreeView) && (selection instanceof IStructuredSelection)) {
            Object sel = ((IStructuredSelection) selection).getFirstElement();
            if ((sel != null) && (sel instanceof Project)) {
                m_selectedProject = (Project) sel;
            } else {
                m_selectedProject = null;
            }
        } else if ((part instanceof TaskListView) && (selection instanceof IStructuredSelection)) {
            Object sel = ((IStructuredSelection) selection).getFirstElement();
            if ((sel != null) && (sel instanceof Task)) {
                m_selectedTask = (Task) sel;
            } else {
                m_selectedTask = null;
            }
        } else if ((part instanceof CalendarView) && (selection instanceof CalendarSelection)) {
            Calendar sel = ((CalendarSelection) selection).getCalendar();
            m_selectedDay = sel;
            switch(m_activeViewType) {
                case HIERARCHICAL:
                    {
                        m_treeViewer.setInput(m_selectedDay);
                        m_treeViewer.expandAll();
                        break;
                    }
                case FLAT:
                    {
                        m_tableViewer.setInput(m_selectedDay);
                        break;
                    }
                case DAYGRAPH:
                    {
                        m_dayGraphViewer.setContents(new Day(m_selectedDay));
                        break;
                    }
            }
        }
    }

    public Project getSelectedProject() {
        return m_selectedProject;
    }

    public Task getSelectedTask() {
        return m_selectedTask;
    }

    public Calendar getSelectedDay() {
        return m_selectedDay;
    }

    protected void doDropTask(TaskTransfer.ProjectTask projectTask) {
        try {
            int maxTime = 0;
            Collection<WorkItem> workItems = m_workItemStore.getWorkItems(new Day(m_selectedDay));
            if (workItems != null) {
                for (WorkItem workItem : workItems) {
                    if (workItem.getBegin() > maxTime) {
                        maxTime = workItem.getBegin();
                    }
                    if (workItem.getEnd() > maxTime) {
                        maxTime = workItem.getEnd();
                    }
                }
            }
            WorkItem workItem = new WorkItem();
            workItem.setProjectId(projectTask.getProject().getId());
            workItem.setTaskId(projectTask.getTask().getId());
            workItem.setBegin(maxTime);
            workItem.setEnd(maxTime);
            workItem.setDay(new Day(m_selectedDay));
            workItem.setComment("");
            m_workItemStore.storeWorkItem(workItem, false);
        } catch (Exception e) {
            Activator.getDefault().log(e);
        }
    }

    public void handleRepositoryEvent(final IRepositoryEvent event) {
        switch(event.getType()) {
            case PROJECT:
            case TASK:
                {
                    m_tableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                        public void run() {
                            switch(m_activeViewType) {
                                case HIERARCHICAL:
                                    {
                                        ISelection selection = m_treeViewer.getSelection();
                                        m_treeViewer.refresh();
                                        m_treeViewer.expandAll();
                                        m_treeViewer.setSelection(selection);
                                    }
                                    break;
                                case FLAT:
                                    {
                                        ISelection selection = m_tableViewer.getSelection();
                                        m_tableViewer.refresh();
                                        m_tableViewer.setSelection(selection);
                                    }
                                    break;
                            }
                        }
                    });
                    break;
                }
            case WORKITEM:
                {
                    m_tableViewer.getTable().getDisplay().asyncExec(new Runnable() {

                        public void run() {
                            switch(m_activeViewType) {
                                case HIERARCHICAL:
                                    {
                                        ISelection selection = m_treeViewer.getSelection();
                                        m_treeViewer.refresh();
                                        m_treeViewer.expandAll();
                                        m_treeViewer.setSelection(selection);
                                    }
                                    break;
                                case FLAT:
                                    {
                                        if (event.getWorkItems() != null) {
                                            for (WorkItem workItem : event.getWorkItems()) {
                                                m_tableViewer.update(workItem, new String[] { "begin", "end", "duration", "project", "task", "comment" });
                                            }
                                        }
                                        ISelection selection = m_tableViewer.getSelection();
                                        m_tableViewer.refresh();
                                        m_tableViewer.setSelection(selection);
                                    }
                                    break;
                            }
                        }
                    });
                    break;
                }
        }
    }

    private class WorkItemCellModifier implements ICellModifier {

        public boolean canModify(Object element, String property) {
            if (element instanceof WorkItem) {
                if ("end".equals(property) && !((WorkItem) element).isActive()) {
                    return true;
                }
                if ("begin".equals(property) || "task".equals(property) || "comment".equals(property)) {
                    return true;
                }
            }
            return false;
        }

        public Object getValue(Object element, String property) {
            if (element instanceof WorkItem) {
                WorkItem workItem = (WorkItem) element;
                if ("begin".equals(property)) {
                    return workItem.getBegin();
                } else if ("end".equals(property)) {
                    return workItem.getEnd();
                } else if ("task".equals(property)) {
                    try {
                        Project project = m_projectStore.getProject(workItem.getProjectId());
                        Task task = m_projectStore.getTask(workItem.getTaskId());
                        return new ProjectTaskHolder(project, task);
                    } catch (Exception e) {
                        Activator.getDefault().log(e);
                    }
                } else if ("comment".equals(property)) {
                    return workItem.getComment();
                }
            }
            return null;
        }

        public void modify(Object swtElement, String property, Object value) {
            if (swtElement instanceof Item) {
                Object element = ((Item) swtElement).getData();
                if (element instanceof WorkItem) {
                    WorkItem workItem = (WorkItem) element;
                    if ("begin".equals(property)) {
                        Assert.isTrue((value != null) && (value instanceof Integer));
                        int begin = ((Integer) value).intValue();
                        if ((begin >= 0) && (begin <= (24 * 60)) && (workItem.getBegin() != begin)) {
                            workItem.setBegin(begin);
                            try {
                                m_workItemStore.storeWorkItem(workItem, false);
                            } catch (Exception e) {
                                Activator.getDefault().log(e);
                            }
                        }
                    } else if ("end".equals(property)) {
                        Assert.isTrue((value != null) && (value instanceof Integer));
                        int end = ((Integer) value).intValue();
                        if ((end >= 0) && (end <= (24 * 60)) && (workItem.getEnd() != end)) {
                            workItem.setEnd(end);
                            try {
                                m_workItemStore.storeWorkItem(workItem, false);
                            } catch (Exception e) {
                                Activator.getDefault().log(e);
                            }
                        }
                    } else if ("task".equals(property)) {
                        Task task = null;
                        if (value instanceof Task) {
                            task = (Task) value;
                        } else if (value instanceof ProjectTaskHolder) {
                            task = ((ProjectTaskHolder) value).getTask();
                        }
                        if (task != null) {
                            if (task.getId() != workItem.getTaskId()) {
                                workItem.setTaskId(((Task) value).getId());
                                try {
                                    m_workItemStore.storeWorkItem(workItem, false);
                                } catch (Exception e) {
                                    Activator.getDefault().log(e);
                                }
                            }
                        }
                    } else if ("comment".equals(property)) {
                        if (!value.toString().equals(workItem.getComment())) {
                            workItem.setComment(value.toString());
                            try {
                                m_workItemStore.storeWorkItem(workItem, false);
                            } catch (Exception e) {
                                Activator.getDefault().log(e);
                            }
                        }
                    }
                }
            }
        }
    }
}
