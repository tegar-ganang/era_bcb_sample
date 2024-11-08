package de.schwarzrot.epgmgr.app.view;

import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.vdr.data.domain.Timer;
import de.schwarzrot.ui.control.support.AbstractTableView;
import de.schwarzrot.ui.service.RendererFactory;

public class TimerTableView extends AbstractTableView<Timer> {

    public class TimerTableFormat extends AbstractTableFormat {

        private final String[] columnNames = { "sequence", "active", "vps", "channelID", "begin", "end", "name", "priority", "lifeTime" };

        private final int[] width = { 1, 1, 1, 100, 120, 120, 300, 40, 40 };

        private final int[] maxWidth = { 25, 30, 25, 170, 130, 130, 900, 40, 40 };

        protected TimerTableFormat() {
            super(TimerTableView.class.getSimpleName());
        }

        @Override
        protected String[] getColumnNames() {
            return columnNames;
        }

        @Override
        protected int getColumnWidth(int idx) {
            if (idx < width.length) return width[idx];
            return 0;
        }

        @Override
        protected int getMaxColWidth(int idx) {
            if (idx < maxWidth.length) return maxWidth[idx];
            return 0;
        }

        @Override
        protected void setColumnRenderer(TableColumn tc, int colIndex) {
            if (tc != null) {
                switch(colIndex) {
                    case 0:
                    case 7:
                    case 8:
                        tc.setCellRenderer(rf.getNumberRenderer());
                        break;
                    case 1:
                    case 2:
                        tc.setCellRenderer(rf.getBooleanRenderer());
                        break;
                    case 4:
                    case 5:
                        tc.setCellRenderer(rf.getDatetimeRenderer());
                        break;
                }
            }
        }

        @Override
        protected void setTypeRenderers(JTable table) {
            rf = (RendererFactory) ApplicationServiceProvider.getService(RendererFactory.class);
        }

        @Override
        public Object getColumnValue(Timer tm, int column) {
            Object rv = null;
            switch(column) {
                case 0:
                    rv = tm.getSequence();
                    break;
                case 1:
                    rv = tm.isActive();
                    break;
                case 2:
                    rv = tm.isVps();
                    break;
                case 3:
                    if (tm.getEvent() != null && tm.getEvent().getChannel() != null) rv = tm.getEvent().getChannel().getName(); else rv = tm.getChannelId();
                    break;
                case 4:
                    rv = tm.getBegin();
                    if (rv == null && tm.getEvent() != null) rv = tm.getEvent().getBegin();
                    break;
                case 5:
                    rv = tm.getEnd();
                    break;
                case 6:
                    if (tm.getEvent() != null && tm.getEvent().getTitle() != null) {
                        StringBuilder sb = new StringBuilder(tm.getEvent().getTitle());
                        if (tm.getEvent().getSubTitle() != null) {
                            sb.append(" - ");
                            sb.append(tm.getEvent().getSubTitle());
                        }
                        rv = sb.toString();
                    }
                    break;
                case 7:
                    rv = tm.getPriority();
                    break;
                case 8:
                    rv = tm.getLifeTime();
                    break;
            }
            return rv;
        }
    }

    protected static RendererFactory rf;

    public TimerTableView() {
        this(null);
    }

    public TimerTableView(Collection<Timer> list) {
        super(list);
        setTableFormat(new TimerTableFormat());
    }

    @Override
    protected JComponent buildPanel() {
        return createTable();
    }
}
