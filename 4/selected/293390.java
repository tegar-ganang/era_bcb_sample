package de.schwarzrot.epgmgr.app;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSource;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import com.jgoodies.binding.PresentationModel;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import de.schwarzrot.app.domain.AccessMode;
import de.schwarzrot.app.support.AbstractApplication;
import de.schwarzrot.app.support.ApplicationServiceProvider;
import de.schwarzrot.concurrent.ProgressPublisher;
import de.schwarzrot.data.access.support.ConditionElement;
import de.schwarzrot.data.access.support.LowerBoundConditionElement;
import de.schwarzrot.data.transaction.TORead;
import de.schwarzrot.data.transaction.TORemove;
import de.schwarzrot.data.transaction.TOSave;
import de.schwarzrot.data.transaction.Transaction;
import de.schwarzrot.data.transaction.TransactionStatus;
import de.schwarzrot.epgmgr.app.view.ChannelTableView;
import de.schwarzrot.epgmgr.app.view.EpgTableView;
import de.schwarzrot.epgmgr.app.view.TimerDetails;
import de.schwarzrot.epgmgr.app.view.TimerTableView;
import de.schwarzrot.epgmgr.domain.EMConfig;
import de.schwarzrot.epgmgr.support.FavoriteChannel;
import de.schwarzrot.ui.action.ActionContextEvent;
import de.schwarzrot.ui.action.ApplicationActionHandler;
import de.schwarzrot.ui.action.support.AbstractActionCallback;
import de.schwarzrot.ui.action.support.AbstractActionContextCallback;
import de.schwarzrot.ui.action.support.AbstractActionHandler;
import de.schwarzrot.ui.action.support.SubmitHandler;
import de.schwarzrot.ui.control.ApplicationPage;
import de.schwarzrot.ui.control.support.AbstractEditor;
import de.schwarzrot.ui.support.AbstractStatusBar;
import de.schwarzrot.vdr.data.domain.ChannelInfo;
import de.schwarzrot.vdr.data.domain.EpgEvent;
import de.schwarzrot.vdr.data.domain.Timer;

/**
 * controller implementation to manage {@code EpgEvent}s. Supports extended
 * filtering and uses {@code EpgTableView} as view. Allows to switch view
 * between epg events, channel lists and timers.
 * 
 * @author <a href="mailto:rmantey@users.sourceforge.net">Reinhard Mantey</a>
 * 
 */
public class EpgManager extends AbstractApplication<EMConfig> {

    public class EpgStatusBar extends AbstractStatusBar {

        private static final long serialVersionUID = 713L;

        public EpgStatusBar() {
            super(60000);
            if (msgSource == null) msgSource = ApplicationServiceProvider.getService(MessageSource.class);
        }

        @Override
        public void checkExtends() {
            checkHost();
        }

        public void setExtend(String text) {
            extend.setText(text);
        }

        @Override
        public void updateExtends() {
            StringBuilder sb = new StringBuilder(" ");
            sb.append(msgSource.getMessage(getName() + ".host.name", null, getName() + ".host.name", null));
            sb.append(": ");
            if (hostOnline) sb.append(msgSource.getMessage(getName() + ".host.online", null, getName() + ".host.online", null)); else sb.append(msgSource.getMessage(getName() + ".host.offline", null, getName() + ".host.offline", null));
            setExtend(sb.toString());
        }

        @Override
        protected GridBagConstraints init() {
            GridBagConstraints c = super.init();
            extend = new JTextField();
            extend.setHorizontalAlignment(JLabel.LEFT);
            extend.setEditable(false);
            extend.setBorder(BorderFactory.createLoweredBevelBorder());
            Dimension size = extend.getSize();
            size.width = 130;
            extend.setPreferredSize(size);
            c.fill = GridBagConstraints.VERTICAL;
            c.weightx = 0;
            c.gridx = 1;
            c.gridy = 0;
            c.gridwidth = 1;
            add(extend, c);
            return c;
        }

        private JTextField extend;
    }

    class ConfigEditor extends AbstractEditor<EMConfig> {

        private static final long serialVersionUID = 713L;

        public ConfigEditor(EMConfig cfg) {
            super(cfg, false);
        }

        @Override
        protected JComponent buildPanel() {
            if (msgSource == null) msgSource = ApplicationServiceProvider.getService(MessageSource.class);
            FormLayout layout = new FormLayout("left:max(100dlu;pref), 3dlu, 100dlu:grow");
            DefaultFormBuilder builder = new DefaultFormBuilder(layout);
            String prefix = EMConfig.class.getSimpleName() + ".";
            PresentationModel<EMConfig> pm = getModel();
            builder.setDefaultDialogBorder();
            builder.append(msgSource.getMessage(prefix + "minEventLength.label", null, prefix + "minEventLength.label", null), componentFactory.createIntegerSpinner(pm.getBufferedModel(EMConfig.FLD_MIN_EVENT_LENGTH), getInstance().getMinEventLength(), 0, 240, 1));
            builder.append(msgSource.getMessage(prefix + "vdrhost.label", null, prefix + "vdrhost.label", null), componentFactory.createTextField(pm.getBufferedModel(EMConfig.FLD_HOSTNAME)));
            builder.append(msgSource.getMessage(prefix + "vdrproxy.port.label", null, prefix + "vdrproxy.port.label", null), componentFactory.createIntegerField(pm.getBufferedModel(EMConfig.FLD_PORT)));
            return builder.getPanel();
        }
    }

    class EpgActionHandler extends AbstractActionHandler implements ApplicationActionHandler {

        public EpgActionHandler(String name) {
            super(name);
        }

        @Override
        public Enum<?>[] getCommands() {
            return viewCommands;
        }

        @Override
        public Enum<?>[] getPopupCommands(Object context) {
            return viewCommands;
        }

        @Override
        public Enum<?>[] getToolBarCommands() {
            return viewCommands;
        }

        @Override
        public void init() {
            setupAction(getName(), Command.FILE_EPG, new AbstractActionCallback() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    showEPG(null);
                }
            }, KeyEvent.VK_E, AccessMode.APP_READ);
            setupAction(getName(), Command.FILE_CHANNELS, new AbstractActionCallback() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    showChannels();
                }
            }, KeyEvent.VK_C, AccessMode.APP_READ);
            setupAction(getName(), Command.FILE_TIMERS, new AbstractActionCallback() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    showTimers();
                }
            }, KeyEvent.VK_T, AccessMode.APP_READ);
            setupAction(getName(), Command.EDIT_TIMER_ACTIVATE, new AbstractActionCallback() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    activateTimer(ae);
                }
            }, KeyEvent.VK_A, AccessMode.APP_MODIFY);
            setupAction(getName(), Command.EDIT_TIMER_DELETE, new AbstractActionCallback() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    deleteTimer(ae);
                }
            }, KeyEvent.VK_D, AccessMode.APP_MODIFY);
            setupAction(getName(), Command.VIEW_TIMER_REFRESH, new AbstractActionCallback() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    refreshTimers();
                }
            }, KeyEvent.VK_R, AccessMode.APP_MODIFY);
        }

        @Override
        public void init(Object contextObj) {
            setupAction(getName(), Command.EDIT_TIMER_ACTIVATE, new AbstractActionContextCallback(contextObj) {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    activateTimer(new ActionContextEvent(ae, getContext()));
                }
            }, KeyEvent.VK_A, AccessMode.APP_MODIFY);
            setupAction(getName(), Command.EDIT_TIMER_DELETE, new AbstractActionContextCallback(contextObj) {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    deleteTimer(new ActionContextEvent(ae, getContext()));
                }
            }, KeyEvent.VK_D, AccessMode.APP_MODIFY);
        }
    }

    class EpgLoader extends Thread {

        public EpgLoader(EventList<EpgEvent> list) {
            this.list = list;
            setDaemon(true);
        }

        @SuppressWarnings("synthetic-access")
        @Override
        public void run() {
            List<ConditionElement> args = new ArrayList<ConditionElement>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.HOUR, -24);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            args.add(new LowerBoundConditionElement("begin", cal.getTime()));
            Transaction ta = taFactory.createTransaction();
            ta.add(new TORead<EpgEvent>(list, EpgEvent.class, args, 0, true));
            ta.setRollbackOnly();
            ta.execute();
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (epgView != null) {
                        epgView.selectPreferred();
                        getStatusBar().setTimeoutMessage("fertig mit EPG-Daten lesen und aufbereiten.", 30);
                    }
                }
            });
        }

        private EventList<EpgEvent> list;
    }

    class TimerHandler implements SubmitHandler<Timer> {

        @Override
        @SuppressWarnings("synthetic-access")
        public void onSubmit(Timer any) {
            getLogger().info("onSubmit() ...(start): " + any);
            getLogger().info("begin: " + any.getBegin());
            getLogger().info("end: " + any.getEnd());
            long lBegin = 0;
            if (msgSource == null) msgSource = ApplicationServiceProvider.getService(MessageSource.class);
            if (any.isVps()) lBegin = any.getEvent().getVpsBegin().getTime(); else lBegin = any.getEvent().getBegin().getTime();
            long lEnd = lBegin + any.getEvent().getDuration() * 60000l;
            if (!any.isVps()) {
                lBegin -= any.getPreStart() * 60000;
                lEnd += any.getPostEnd() * 60000;
            }
            any.setBegin(new Date(lBegin));
            any.setEnd(new Date(lEnd));
            getLogger().info("begin: " + any.getBegin());
            getLogger().info("end: " + any.getEnd());
            getLogger().info("onSubmit() ...(end): " + any);
            try {
                Transaction ta = taFactory.createTransaction();
                ta.add(new TOSave<Timer>(any));
                ta.execute();
                if (ta.getStatus() != TransactionStatus.STATUS_COMMITTED) {
                    String msgId = TimerHandler.class.getSimpleName() + ".timer.creation.error";
                    String message = msgSource.getMessage(msgId, null, msgId, null);
                    String title = msgSource.getMessage(TITLE_ERROR, null, TITLE_ERROR, null);
                    JOptionPane.showMessageDialog(getMainWindow(), message, title, JOptionPane.WARNING_MESSAGE);
                    getLogger().info("oups");
                }
            } catch (Exception e) {
                String msgId = TimerHandler.class.getSimpleName() + ".timer.creation.error";
                String message = msgSource.getMessage(msgId, null, msgId, null);
                String title = msgSource.getMessage(TITLE_ERROR, null, TITLE_ERROR, null);
                JOptionPane.showMessageDialog(getMainWindow(), message, title, JOptionPane.WARNING_MESSAGE);
                getLogger().info("oups", e);
            }
        }
    }

    private enum Command {

        FILE_EPG, FILE_CHANNELS, FILE_TIMERS, EDIT_TIMER_ACTIVATE, EDIT_TIMER_DELETE, VIEW_TIMER_REFRESH
    }

    public EpgManager() {
        super(EpgManager.class.getSimpleName());
        setActionHandler(new EpgActionHandler(getName()));
        timerHandler = new TimerHandler();
    }

    @Override
    public JComponent createPane() {
        return getView(null);
    }

    @Override
    public AbstractStatusBar createStatusBar() {
        AbstractStatusBar rv = new EpgStatusBar();
        rv.setName(getName());
        return rv;
    }

    @Override
    public JComponent getConfigPage() {
        if (cfgEditor == null) cfgEditor = new ConfigEditor(getAppConfig());
        return cfgEditor;
    }

    public JComponent getView(String command) {
        if (content == null) content = buildPanel();
        return content;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent ae) {
    }

    @Override
    public void setup() {
    }

    @Override
    public void stop() {
        super.stop();
        epg = null;
        content = null;
        viewHandler = null;
        channelPane = null;
        epgPane = null;
        timerPane = null;
        channelView = null;
        timerView = null;
        epgView = null;
        System.gc();
    }

    protected void activateTimer(ActionEvent ae) {
        Timer timer = null;
        if (ae instanceof ActionContextEvent) {
            timer = (Timer) ((ActionContextEvent) ae).getContext();
        } else {
            EventList<Timer> selected = timerView.getSelectionModel().getSelected();
            if (selected.size() > 0) timer = selected.get(0);
        }
        if (timer != null) {
            timer.setActive(!timer.isActive());
            Transaction t = taFactory.createTransaction();
            t.add(new TOSave<Timer>(timer));
            t.execute();
        }
    }

    protected JComponent buildPanel() {
        if (content == null) {
            if (msgSource == null) msgSource = ApplicationServiceProvider.getService(MessageSource.class);
            content = new JPanel();
            viewHandler = new CardLayout();
            content.setLayout(viewHandler);
            showEPG(null);
        }
        return content;
    }

    protected void channelChanged(ActionEvent ae) {
    }

    protected void checkHost() {
        try {
            if (host == null) host = InetAddress.getByName(getAppConfig().getVDRHost());
            hostOnline = host.isReachable(500);
        } catch (Exception e) {
            getLogger().debug("error determining host: ", e);
        }
    }

    protected JComponent createChannelPane() {
        if (channelPane == null) {
            channelView = new ChannelTableView(getChannelData());
            channelView.setSelectionChangedExecutor(new AbstractAction() {

                private static final long serialVersionUID = 713L;

                @Override
                public void actionPerformed(ActionEvent ae) {
                    channelChanged(ae);
                }
            });
            channelView.setPopDoubleLeftExecutor(new AbstractAction() {

                private static final long serialVersionUID = 713L;

                @Override
                public void actionPerformed(ActionEvent ae) {
                    popChannelDoubleLeft(ae);
                }
            });
            channelView.setPopSingleRightExecutor(new AbstractAction() {

                private static final long serialVersionUID = 713L;

                @Override
                public void actionPerformed(ActionEvent ae) {
                    popChannelSingleRight(ae);
                }
            });
            channelPane = new ApplicationPage(getName() + ".channel", channelView.getView());
        }
        return channelPane;
    }

    protected JComponent createEpgPane() {
        if (epgPane == null) {
            if (channels == null) getChannelData();
            getStatusBar().setMessage("EPG-Daten werden gelesen und aufbereitet ...");
            epg = getEpgData1();
            FilterList<ChannelInfo> fl = new FilterList<ChannelInfo>(channels, new FavoriteChannel());
            epgView = new EpgTableView(epg, getAppConfig().getMinEventLength());
            epgView.setTimerCreationHandler(timerHandler);
            epgView.setFavoriteChannels(fl);
            epgPane = new ApplicationPage(getName() + ".epg", epgView.getView());
        }
        return epgPane;
    }

    protected JComponent createTimerPane() {
        if (timerPane == null) {
            timerView = new TimerTableView(getTimerData());
            timerPane = new ApplicationPage(getName() + ".timer", timerView.getView());
        }
        return timerPane;
    }

    protected void deleteTimer(ActionEvent ae) {
        Timer timer = null;
        if (ae instanceof ActionContextEvent) {
            timer = (Timer) ((ActionContextEvent) ae).getContext();
        } else {
            EventList<Timer> selected = timerView.getSelectionModel().getSelected();
            if (selected.size() > 0) timer = selected.get(0);
        }
        if (timer != null) {
            Transaction t = taFactory.createTransaction();
            t.add(new TORemove<Timer>(timer));
            t.execute();
        }
    }

    protected List<ChannelInfo> getChannelData() {
        if (channels == null) {
            channels = new BasicEventList<ChannelInfo>();
            Transaction ta = taFactory.createTransaction();
            ta.add(new TORead<ChannelInfo>(channels, ChannelInfo.class));
            ta.execute();
        }
        return channels;
    }

    protected EventList<EpgEvent> getEpgData1() {
        EventList<EpgEvent> rv = new BasicEventList<EpgEvent>();
        Thread epgLoader = new EpgLoader(rv);
        epgLoader.start();
        return rv;
    }

    protected List<Timer> getTimerData() {
        List<Timer> rv = new BasicEventList<Timer>();
        List<ConditionElement> args = new ArrayList<ConditionElement>();
        Transaction ta = taFactory.createTransaction();
        args.add(new LowerBoundConditionElement("end", new Date()));
        ta.add(new TORead<Timer>(rv, Timer.class, args, 0, true));
        ta.setRollbackOnly();
        ta.execute();
        return rv;
    }

    protected void popChannelDoubleLeft(ActionEvent ae) {
    }

    protected void popChannelSingleRight(ActionEvent ae) {
    }

    protected void popEpgDoubleLeft(ActionEvent ae) {
    }

    protected void popEpgSingleRight(ActionEvent ae) {
    }

    protected void popTimerDoubleLeft(ActionEvent ae) {
        Timer timer = null;
        if (ae instanceof ActionContextEvent) {
            timer = (Timer) ((ActionContextEvent) ae).getContext();
        } else {
            EventList<Timer> selected = timerView.getSelectionModel().getSelected();
            if (selected.size() > 0) timer = selected.get(0);
        }
        if (timer != null) {
            TimerDetails tdd = new TimerDetails("edit.timer", timer);
            tdd.showDialog(content);
        }
    }

    protected void refreshTimers() {
        Transaction t = taFactory.createTransaction();
        List<ConditionElement> args = new ArrayList<ConditionElement>();
        List<Timer> timers = new BasicEventList<Timer>();
        args.add(new LowerBoundConditionElement("end", new Date()));
        t.add(new TORead<Timer>(timers, Timer.class, args, 0, true));
        t.setRollbackOnly();
        t.execute();
        if (timers.size() > 0) {
            timerView.getList().clear();
            timerView.getList().addAll(timers);
            timerView.refresh();
        }
    }

    protected void showChannels() {
        content.add(createChannelPane(), Command.FILE_CHANNELS.name());
        viewHandler.show(content, Command.FILE_CHANNELS.name());
        updateCommands(Command.FILE_CHANNELS);
    }

    protected void showEPG(ProgressPublisher pp) {
        JComponent tmp = null;
        try {
            if (pp != null && !pp.isActive()) pp.start();
            tmp = createEpgPane();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pp != null && pp.isActive()) pp.end();
        }
        content.add(tmp, Command.FILE_EPG.name());
        viewHandler.show(content, Command.FILE_EPG.name());
        updateCommands(Command.FILE_EPG);
    }

    protected void showTimers() {
        content.add(createTimerPane(), Command.FILE_TIMERS.name());
        viewHandler.show(content, Command.FILE_TIMERS.name());
        updateCommands(Command.FILE_TIMERS);
    }

    protected void updateCommands(Command cmd) {
        for (Enum<?> cur : viewCommands) getActionHandler().enableAction(getName() + "." + cur.name(), cmd != cur);
    }

    @SuppressWarnings("unused")
    private final Enum<?>[] timerTbCommands = new Enum<?>[] { Command.EDIT_TIMER_ACTIVATE, Command.EDIT_TIMER_DELETE, Command.VIEW_TIMER_REFRESH };

    protected static MessageSource msgSource;

    private static final long serialVersionUID = 713L;

    protected static final Enum<?>[] viewCommands;

    protected EventList<EpgEvent> epg;

    protected EpgTableView epgView;

    protected JComponent content;

    protected boolean hostOnline;

    private TimerHandler timerHandler;

    private JComponent channelPane;

    private JComponent epgPane;

    private JComponent timerPane;

    private CardLayout viewHandler;

    private ChannelTableView channelView;

    private TimerTableView timerView;

    private EventList<ChannelInfo> channels;

    private ConfigEditor cfgEditor;

    private volatile InetAddress host;

    static {
        viewCommands = new Enum<?>[] { Command.FILE_EPG, Command.FILE_CHANNELS, Command.FILE_TIMERS };
    }
}
