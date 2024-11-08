package de.schwarzrot.epgmgr.app.view;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.table.TableColumn;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.swing.control.VirtualFrame;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.epgmgr.support.ChannelSelector;
import de.schwarzrot.epgmgr.support.DurationMatcherEditor;
import de.schwarzrot.epgmgr.support.EventThresholdEvaluator;
import de.schwarzrot.ui.action.support.SubmitHandler;
import de.schwarzrot.ui.control.support.AbstractTableView;
import de.schwarzrot.ui.control.support.ListFilterComponent;
import de.schwarzrot.ui.service.FormComponentFactory;
import de.schwarzrot.ui.service.RendererFactory;
import de.schwarzrot.vdr.data.domain.ChannelInfo;
import de.schwarzrot.vdr.data.domain.EpgEvent;
import de.schwarzrot.vdr.data.domain.Timer;

public class EpgTableView extends AbstractTableView<EpgEvent> {

    protected static FormComponentFactory formFactory;

    protected static final int DELTA_MAX = 691200;

    protected static RendererFactory rf;

    private static final String PREFIX = EpgTableView.class.getSimpleName() + ".";

    public class EpgTableFormat extends AbstractTableFormat {

        public EpgTableFormat() {
            super(EpgTableView.class.getSimpleName());
        }

        @Override
        public Object getColumnValue(EpgEvent evt, int idx) {
            Object rv = null;
            if (evt != null) {
                switch(idx) {
                    case 0:
                        rv = evt.getBegin();
                        break;
                    case 1:
                        rv = evt.getDuration();
                        break;
                    case 2:
                        rv = evt.getTitle();
                        break;
                    case 3:
                        rv = evt.getSubTitle();
                        break;
                    case 4:
                        rv = evt.getChannel().getName();
                        break;
                }
            }
            return rv;
        }

        @Override
        protected String[] getColumnNames() {
            return colNames;
        }

        @Override
        protected int getColumnWidth(int idx) {
            return width[idx];
        }

        @Override
        protected int getMaxColWidth(int idx) {
            return maxWidth[idx];
        }

        @Override
        protected void setColumnRenderer(TableColumn tc, int colIndex) {
            if (tc != null) {
                switch(colIndex) {
                    case 0:
                        tc.setCellRenderer(rf.getDateRenderer("EE dd.MM.yyyy HH:mm"));
                        break;
                    case 1:
                        tc.setCellRenderer(rf.getNumberRenderer());
                        break;
                }
            }
        }

        @Override
        protected void setTypeRenderers(JTable table) {
            rf = ApplicationServiceProvider.getService(RendererFactory.class);
        }

        private String[] colNames = new String[] { "time", "length", "title", "subtitle", "channel" };

        private final int[] width = { 150, 36, 250, 230, 100 };

        private final int[] maxWidth = { 200, 40, 600, 600, 600 };
    }

    protected class DurationFilter implements ListFilterComponent<EpgEvent> {

        public DurationFilter() {
            model = new ValueHolder(new Integer(0));
            duration = formFactory.createIntegerSpinner(model, 60, 0, 300, 5);
            dme = new DurationMatcherEditor(model);
        }

        @Override
        public final JComponent getFilterPane() {
            return duration;
        }

        @Override
        public final MatcherEditor<EpgEvent> getMatcherEditor() {
            return dme;
        }

        public final ValueModel getModel() {
            return model;
        }

        private ValueModel model;

        private JSpinner duration;

        private MatcherEditor<EpgEvent> dme;
    }

    protected class EpgFilterator implements TextFilterator<EpgEvent> {

        @Override
        public void getFilterStrings(List<String> baseList, EpgEvent evt) {
            baseList.add(evt.getTitle());
            baseList.add(evt.getSubTitle());
            baseList.add(evt.getDescription());
        }
    }

    protected class EpgTextFilter implements ListFilterComponent<EpgEvent> {

        public EpgTextFilter() {
            filter = new JTextField(20);
            me = new TextComponentMatcherEditor<EpgEvent>(filter, new EpgFilterator());
        }

        @Override
        public final JComponent getFilterPane() {
            return filter;
        }

        @Override
        public final MatcherEditor<EpgEvent> getMatcherEditor() {
            return me;
        }

        private JTextField filter;

        private TextComponentMatcherEditor<EpgEvent> me;
    }

    protected class TimeFilter extends AbstractThresholdFilter {

        public TimeFilter() {
            super(new EventThresholdEvaluator());
        }

        public TimeFilter(ValueModel durationModel) {
            super(new EventThresholdEvaluator(durationModel));
        }

        @Override
        protected void setupRange(BoundedRangeModel model) {
            model.setRangeProperties(86400, 1, 0, DELTA_MAX, false);
        }

        @Override
        protected void setupSlider(JSlider slider) {
            Dictionary<Integer, JLabel> priorityFilterSliderLabels = new Hashtable<Integer, JLabel>();
            priorityFilterSliderLabels.put(new Integer(0), new JLabel("Low"));
            priorityFilterSliderLabels.put(DELTA_MAX, new JLabel("High"));
            slider.setLabelTable(priorityFilterSliderLabels);
            slider.setMajorTickSpacing(86400);
            slider.setSnapToTicks(false);
            slider.setPaintLabels(true);
            slider.setPaintTicks(true);
        }
    }

    public EpgTableView(List<EpgEvent> list, int defDuration) {
        super(list);
        setTableFormat(new EpgTableFormat());
        if (formFactory == null) formFactory = ApplicationServiceProvider.getService(FormComponentFactory.class);
        channelFilter = new ChannelSelector(getList());
        textFilter = new EpgTextFilter();
        durationFilter = new DurationFilter();
        timeFilter = new TimeFilter(durationFilter.getModel());
        addFilter(channelFilter);
        addFilter(textFilter);
        addFilter(durationFilter);
        setThresholdFilter(timeFilter);
        durationFilter.getModel().setValue(defDuration);
    }

    @Override
    public JComponent buildPanel() {
        JComponent table = createTable();
        FormLayout layout = new FormLayout("fill:80dlu:grow(0.05), 5dlu, fill:200dlu:grow(0.95)", "[30dlu,p], 3dlu, [93dlu,p], 3dlu, fill:[100dlu,p]:grow");
        CellConstraints cc = new CellConstraints();
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setDefaultDialogBorder();
        JToolBar detailsToolbar = new JToolBar();
        JToolBar channelToolbar = new JToolBar();
        JButton createTimer = createToolBarButton("create.timer");
        JButton clearSelection = createToolBarButton("clear.selection");
        JButton selectPreferred = createToolBarButton("preferred.selection");
        EpgDetails epgDetails = new EpgDetails(getSelectionModel());
        createTimer.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                createTimer();
            }
        });
        clearSelection.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                clearChannelSelection();
            }
        });
        selectPreferred.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                selectPreferred();
            }
        });
        detailsToolbar.add(createTimer);
        channelToolbar.add(selectPreferred);
        channelToolbar.add(clearSelection);
        builder.add(createSubFilter(), cc.xy(1, 1));
        builder.add(new VirtualFrame(getMessageSource().getMessage(PREFIX + "details", null, "details", null), detailsToolbar, epgDetails.buildPanel()), cc.xywh(3, 1, 1, 3));
        builder.add(new VirtualFrame(getMessageSource().getMessage(PREFIX + "channels", null, "channels", null), channelToolbar, new JScrollPane(channelFilter.getFilterPane())), cc.xywh(1, 3, 1, 3));
        builder.add(table, cc.xy(3, 5));
        selectPreferred();
        return builder.getPanel();
    }

    public void clearChannelSelection() {
        channelFilter.clearSelection();
    }

    public final EventList<ChannelInfo> getFavoriteChannels() {
        return favoriteChannels;
    }

    public final SubmitHandler<Timer> getTimerCreationHandler() {
        return timerCreationHandler;
    }

    public void selectPreferred() {
        try {
            channelFilter.clearSelection();
            channelFilter.selectChannels(favoriteChannels);
        } catch (Exception e) {
        }
    }

    public final void setFavoriteChannels(EventList<ChannelInfo> favoriteChannels) {
        this.favoriteChannels = favoriteChannels;
        favoriteChannels.addListEventListener(new ListEventListener<ChannelInfo>() {

            @Override
            public void listChanged(ListEvent<ChannelInfo> listChanges) {
                selectPreferred();
            }
        });
    }

    public final void setTimerCreationHandler(SubmitHandler<Timer> timerCreationHandler) {
        this.timerCreationHandler = timerCreationHandler;
    }

    protected JComponent createSubFilter() {
        getLogger().info("start creating filter for event list ...");
        FormLayout layout = new FormLayout("max(25dlu;pref), 3dlu, default:grow");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.append(getMessageSource().getMessage(PREFIX + "Filter", null, "filter", null), textFilter.getFilterPane(), true);
        builder.append(getMessageSource().getMessage(PREFIX + "Begin", null, "begin", null), timeFilter.getFilterPane(), true);
        builder.append(getMessageSource().getMessage(PREFIX + "duration", null, "len", null), durationFilter.getFilterPane(), true);
        return builder.getPanel();
    }

    protected void createTimer() {
        EventList<EpgEvent> tmp = getSelectionModel().getSelected();
        EpgEvent event = null;
        if (tmp != null && tmp.size() > 0) event = tmp.get(0);
        if (event != null) {
            TimerDetails td = new TimerDetails("new.timer", new Timer(event));
            td.setSubmitHandler(getTimerCreationHandler());
            if (td.showDialog(getView()) == TimerDetails.APPROVE_OPTION) {
                getLogger().info("created timer for " + event.getTitle());
            }
            td = null;
            event = null;
        }
    }

    private EventList<ChannelInfo> favoriteChannels;

    private SubmitHandler<Timer> timerCreationHandler;

    private ChannelSelector channelFilter;

    private EpgTextFilter textFilter;

    private DurationFilter durationFilter;

    private TimeFilter timeFilter;
}
