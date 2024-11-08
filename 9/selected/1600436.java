package com.open_squad.openplan.editors;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.jface.action.IAction;
import org.hibernate.Session;
import com.open_squad.openplan.PlanningContextMenuProvider;
import com.open_squad.openplan.actions.FinishedAction;
import com.open_squad.openplan.actions.StartedAction;
import com.open_squad.openplan.model.Machine;
import com.open_squad.openplan.model.Planner;
import com.open_squad.openplan.part.PlanEditPartFactory;
import com.open_squad.openplan.utils.HibernateUtil;

/**
 * @author kerkenia
 *
 */
public class PlanEditor extends GraphicalEditor {

    @Override
    protected void configureGraphicalViewer() {
        super.configureGraphicalViewer();
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setEditPartFactory(new PlanEditPartFactory());
        viewer.setContextMenu(new PlanningContextMenuProvider(getGraphicalViewer(), getActionRegistry()));
    }

    public PlanEditor() {
        setEditDomain(new DefaultEditDomain(this));
    }

    public static final String ID = "com.open_squad.openplan.editors.PlanEditor";

    Planner createModel() {
        Planner planning = new Planner();
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List<Machine> machines = session.createQuery("from Machine").list();
        planning.setMachines(machines);
        return planning;
    }

    @Override
    protected void initializeGraphicalViewer() {
        GraphicalViewer viewer = getGraphicalViewer();
        viewer.setContents(createModel());
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
    }

    @Override
    protected void createActions() {
        super.createActions();
        IAction action;
        action = new StartedAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
        action = new FinishedAction(this);
        getActionRegistry().registerAction(action);
        getSelectionActions().add(action.getId());
    }
}
