package org.nexopenframework.management.jee.jboss4x;

import static org.jboss.system.server.Server.START_NOTIFICATION_TYPE;
import static org.jboss.system.server.ServerImplMBean.OBJECT_NAME;
import static org.nexopenframework.management.support.ClassUtils.isPresent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.jboss.system.ServiceMBeanSupport;
import org.nexopenframework.management.Version;
import org.nexopenframework.management.jee.JeeManagementException;
import org.nexopenframework.management.jee.monitor.jboss4x.JBoss4xConnectionPoolTaskMonitor;
import org.nexopenframework.management.jee.monitor.jboss4x.JBoss4xJmsDestinationTaskMonitor;
import org.nexopenframework.management.jee.monitor.jboss4x.JBoss4xJmsMessageCacheTaskMonitor;
import org.nexopenframework.management.jee.monitor.jboss4x.JBoss4xTransactionManagerTaskMonitor;
import org.nexopenframework.management.jee.monitor.services.ServiceTaskMonitorBuilder;
import org.nexopenframework.management.jee.monitor.support.HttpRequestMonitor;
import org.nexopenframework.management.jee.monitor.support.MBeanQueryManager;
import org.nexopenframework.management.jee.monitor.tomcat55x.TomcatWebThreadPoolTaskMonitor;
import org.nexopenframework.management.jee.support.ComponentsDigester;
import org.nexopenframework.management.module.core.ModuleRepository;
import org.nexopenframework.management.monitor.EventMonitor;
import org.nexopenframework.management.monitor.TaskMonitor;
import org.nexopenframework.management.monitor.channels.ChannelNotification;
import org.nexopenframework.management.monitor.core.MonitorManager;
import org.nexopenframework.management.monitor.plugins.PluginException;
import org.nexopenframework.management.monitor.plugins.Process;
import org.nexopenframework.management.monitor.plugins.ProcessManager;
import org.nexopenframework.management.monitor.plugins.hyperic.HypericProcessManager;
import org.nexopenframework.management.monitor.support.CpuMonitor;
import org.nexopenframework.management.monitor.support.JvmHeapMonitor;
import org.nexopenframework.management.monitor.support.ServiceComponentMonitor;
import org.nexopenframework.management.support.MBeanServerHandle;
import org.w3c.dom.Element;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Comment here</p>
 * 
 * <pre>  
 *     &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *     &lt;!DOCTYPE server&gt;
 *     &lt;server&gt;
 *     &lt;mbean code="org.nexopenframework.management.jee.monitor.jboss4x.ManagementService"
 * 		     name="openfrwk.management:service=ManagementService,appserver=jboss4x,type=mbean"
 * 			 display-name="NexOpen Monitoring Framework"&gt;  
 *         &lt;attribute name="channels"&gt;
 *             &lt;channels-list&gt;
 *                &lt;!-- put the list of channels class names --&gt;
 *                &lt;channel class="org.nexopenframework.management.monitor.channels.LogChannelNotification"/&gt;
 *                &lt;channel class="com.mycompany.channels.MyChannelNotification"&gt;
 *                   &lt;!-- only string properties accepted --&gt;
 *                   &lt;property name="prop1" value="value1"/&gt;
 *                   &lt;property name="prop2" value="value2"/&gt;
 *                &lt;/channel&gt; 
 *             &lt;/channels-list&gt;
 *         &lt;/attribute&gt;
 *      &lt;/mbean&gt;
 *      &lt;/server&gt;  
 * </pre>
 * 
 * @see org.nexopenframework.management.jee.jboss4x.ManagementServiceMBean
 * @see org.jboss.system.ServiceMBeanSupport
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class ManagementService extends ServiceMBeanSupport implements ManagementServiceMBean, NotificationListener {

    /**Core member {@link ModuleRepository} which holds Modules of applications to be monitored*/
    private ModuleRepository repository;

    /**Core member {@link MonitorManager} which handles channels, sending events and execution of tasks*/
    private MonitorManager manager;

    /**List of custom {@link ChannelNotification}. They MUST be available in classpath */
    private final List<ChannelNotification> l_channels = new LinkedList<ChannelNotification>();

    /**List of custom {@link EventMonitor}. They MUST be available in classpath */
    private final List<EventMonitor> l_events = new LinkedList<EventMonitor>();

    /**List of custom {@link ChannelNotification}. They MUST be available in classpath */
    private final List<TaskMonitor<? extends Number>> l_tasks = new LinkedList<TaskMonitor<? extends Number>>();

    /** Package information for org.jboss */
    private final Package jbossPackage = Package.getPackage("org.jboss");

    /**Core component to handle processes*/
    private ProcessManager processManager;

    /**
	 * <p>It returns as String of Channels registered by {@link MonitorManager}</p>
	 * 
	 * @see org.nexopenframework.management.jee.jboss4x.ManagementServiceMBean#listChannels()
	 */
    public String listChannels() {
        final StringBuilder sb = new StringBuilder("");
        for (final ChannelNotification channel : l_channels) {
            sb.append(channel.getClass().getName()).append(" ");
        }
        sb.append("");
        return sb.toString();
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.jee.jboss4x.ManagementServiceMBean#getMonitorManager()
	 */
    public ObjectName getMonitorManager() {
        return (manager != null) ? manager.getObjectName() : null;
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.jee.jboss4x.ManagementServiceMBean#getModuleRepository()
	 */
    public ObjectName getModuleRepository() {
        return (repository != null) ? repository.getObjectName() : null;
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.jee.jboss4x.ManagementServiceMBean#setChannels(org.w3c.dom.Element)
	 */
    public void setChannels(final Element channels) throws ClassNotFoundException, JeeManagementException {
        this.l_channels.clear();
        this.l_channels.addAll(ComponentsDigester.getChannels(channels));
    }

    public void setEvents(final Element events) throws ClassNotFoundException, JeeManagementException {
        this.l_events.clear();
        this.l_events.addAll(ComponentsDigester.getEvents(events, Thread.currentThread().getContextClassLoader()));
    }

    public void setTasks(final Element tasks) throws ClassNotFoundException, JeeManagementException {
        this.l_tasks.clear();
        this.l_tasks.addAll(ComponentsDigester.getTasks(tasks, Thread.currentThread().getContextClassLoader()));
    }

    /**
	 * <p></p>
	 * 
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
    public void handleNotification(final Notification notification, final Object handback) {
        final String type = notification.getType();
        if (START_NOTIFICATION_TYPE.equals(type)) {
            final List<ServiceTaskMonitorBuilder> builders = new ArrayList<ServiceTaskMonitorBuilder>();
            try {
                builders.add(new JBoss4xConnectionPoolTaskMonitor.Builder());
                builders.add(new JBoss4xTransactionManagerTaskMonitor.Builder());
                builders.add(new JBoss4xJmsMessageCacheTaskMonitor.Builder());
                builders.add(new JBoss4xJmsDestinationTaskMonitor.Builder(true));
                builders.add(new JBoss4xJmsDestinationTaskMonitor.Builder(false));
                builders.add(new TomcatWebThreadPoolTaskMonitor.Builder("jboss.web:type=ThreadPool,*", "jboss4x"));
                final List<ServiceTaskMonitorBuilder> notCompleted = MBeanQueryManager.createAndRegisterServices(server, builders, manager);
                if (!notCompleted.isEmpty()) {
                    final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
                    service.schedule(new Runnable() {

                        public void run() {
                            for (final ServiceTaskMonitorBuilder builder : notCompleted) {
                                if (log.isInfoEnabled()) {
                                    log.info("Trying again ServiceTaskMonitorBuilder :: " + builder);
                                    log.info("JMX Query name " + builder.getQueryName());
                                }
                                MBeanQueryManager.registerInMonitorManager(server, manager, builder);
                            }
                        }
                    }, 4, TimeUnit.SECONDS);
                }
                manager.init();
            } finally {
                builders.clear();
            }
        }
    }

    /**
	 * <p></p>
	 * 
	 * @see org.jboss.system.ServiceMBeanSupport#createService()
	 */
    @Override
    protected void createService() throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Registering JMX MBeanServer " + this.server);
        }
        new MBeanServerHandle(this.server);
    }

    /**
	 * <p></p>
	 * 
	 * @see org.jboss.system.ServiceMBeanSupport#startService()
	 */
    @Override
    protected void startService() {
        try {
            this.server.addNotificationListener(OBJECT_NAME, this, null, null);
        } catch (final InstanceNotFoundException e) {
            this.log.error("Not registerd as NotificationListener. Monitor Frameowrk not correctly created", e);
            throw new JeeManagementException("Not correctly created. Problem registering as a NotificationListener", e);
        }
        manager = new MonitorManager();
        manager.setServer(server);
        if (!this.l_channels.isEmpty()) {
            this.manager.setChannels(l_channels);
        }
        final ServiceComponentMonitor scm_monitor = new ServiceComponentMonitor();
        final List<EventMonitor> monitors = new ArrayList<EventMonitor>();
        monitors.add(scm_monitor);
        monitors.add(new HttpRequestMonitor());
        manager.setEventMonitors(monitors);
        final List<TaskMonitor<? extends Number>> task_monitors = new ArrayList<TaskMonitor<? extends Number>>();
        task_monitors.add(scm_monitor);
        task_monitors.add(new JvmHeapMonitor());
        if (isPresent("org.hyperic.sigar.Sigar")) {
            handleCpuMonitor(task_monitors);
        }
        manager.setTaskMonitors(task_monitors);
        manager.activateWaitAtStartup();
        repository = new ModuleRepository();
        repository.setServer(server);
        repository.init();
        this.log.info("Started NexOpen Monitoring Framework version [" + Version.getVersion() + "] " + "in JBoss AS [" + jbossPackage.getImplementationVersion() + "]");
    }

    /**
	 * <p></p>
	 * 
	 * @param task_monitors
	 * @throws PluginException
	 */
    protected void handleCpuMonitor(final List<TaskMonitor<? extends Number>> task_monitors) throws PluginException {
        final HypericProcessManager hypericProcessManager = new HypericProcessManager();
        final long currentPid = hypericProcessManager.getCurrentPid();
        final List<Process> l_process = hypericProcessManager.getProcesses("State.Name.eq=java,Args.*.eq=org.jboss.Main");
        if (log.isInfoEnabled()) {
            log.debug("Detected " + l_process.size() + " JBoss processes in your server machine.");
        }
        for (final Process p : l_process) {
            if (p.getPid() == currentPid) {
                if (log.isInfoEnabled()) {
                    log.debug("Detected " + p.getPid() + " current JBoss.");
                }
                task_monitors.add(new CpuMonitor(p));
            }
        }
        this.processManager = hypericProcessManager;
    }

    /**
	 * <pfree resources (such as {@link ModuleRepository} and {@link MonitorManager})></p>
	 * 
	 * @see org.jboss.system.ServiceMBeanSupport#stopService()
	 */
    @Override
    protected void stopService() {
        if (manager != null) {
            manager.stop();
        }
        if (repository != null) {
            repository.destroy();
        }
        if (this.processManager != null) {
            this.processManager.destroy();
        }
        if (this.log.isInfoEnabled()) {
            this.log.info("Shutting down NexOpen Monitoring Framework");
        }
    }

    /**
	 * <p>Free resources like clear channels</p>
	 * 
	 * @see org.jboss.system.ServiceMBeanSupport#destroyService()
	 */
    @Override
    protected void destroyService() {
        this.l_channels.clear();
        this.l_events.clear();
        this.l_tasks.clear();
    }
}
