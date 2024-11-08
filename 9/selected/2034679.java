package org.aspencloud.calypso.ui.workbench.views.calendar.actions;

import org.aspencloud.calypso.ui.calendar.tasksCalendar.TasksCalendarFigure;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.remus.infomngmnt.calendar.messages.Messages;
import org.remus.infomngmnt.ccalendar.CCalendar;

public class ShowGridAction extends Action {

    public static final String ID = "org.aspencloud.calypso.ui.views.Calendar.actions.ShowGridAction";

    private CCalendar calendar;

    public ShowGridAction() {
        super(Messages.ShowGridAction_Grid, SWT.CHECK);
        setToolTipText(Messages.ShowGridAction_ToggleGrid);
        setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(CCalendar.PLUGIN_ID, "icons/iconexperience/16/table.png"));
        setId(ID);
        setChecked(true);
    }

    public void setViewers(final CCalendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public void run() {
        GraphicalViewer[] viewers = new GraphicalViewer[] { this.calendar.getTasksViewer(), this.calendar.getActivitiesViewer() };
        if (isChecked()) {
            for (int i = 0; i < viewers.length; i++) {
                ((TasksCalendarFigure) ((GraphicalEditPart) viewers[i].getContents()).getFigure()).setGridVisible(true);
            }
        } else {
            for (int i = 0; i < viewers.length; i++) {
                ((TasksCalendarFigure) ((GraphicalEditPart) viewers[i].getContents()).getFigure()).setGridVisible(false);
            }
        }
    }
}
