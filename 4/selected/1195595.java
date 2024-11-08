package org.nexopenframework.management.adapters.jboss4x;

import static org.jboss.system.server.Server.START_NOTIFICATION_TYPE;
import static org.jboss.system.server.ServerImplMBean.OBJECT_NAME;
import static org.nexopenframework.management.support.ClassUtils.*;
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
import org.nexopenframework.management.adapters.AdapterManagementException;
import org.nexopenframework.management.adapters.loaders.ConfigLoaderProvider;
import org.nexopenframework.management.adapters.loaders.Configuration;
import org.nexopenframework.management.adapters.loaders.ConfigurationDigester;
import org.nexopenframework.management.adapters.loaders.DefaultConfigLoaderProvider;
import org.nexopenframework.management.adapters.loaders.ProviderType;
import org.nexopenframework.management.adapters.monitor.jboss4x.JBoss4xConnectionPoolTaskMonitor;
import org.nexopenframework.management.adapters.monitor.jboss4x.JBoss4xJmsDestinationTaskMonitor;
import org.nexopenframework.management.adapters.monitor.jboss4x.JBoss4xJmsMessageCacheTaskMonitor;
import org.nexopenframework.management.adapters.monitor.jboss4x.JBoss4xTransactionManagerTaskMonitor;
import org.nexopenframework.management.adapters.monitor.resources.ResourceTaskMonitorBuilder;
import org.nexopenframework.management.adapters.monitor.support.HttpRequestMonitor;
import org.nexopenframework.management.adapters.monitor.support.MBeanQueryManager;
import org.nexopenframework.management.adapters.monitor.tomcat55x.TomcatWebThreadPoolTaskMonitor;
import org.nexopenframework.management.module.core.ModuleRepository;
import org.nexopenframework.management.monitor.EventMonitor;
import org.nexopenframework.management.monitor.TaskMonitor;
import org.nexopenframework.management.monitor.channels.ChannelNotification;
import org.nexopenframework.management.monitor.core.MonitorManager;
import org.nexopenframework.management.monitor.plugins.PluginException;
import org.nexopenframework.management.monitor.plugins.Process;
import org.nexopenframework.management.monitor.plugins.ProcessManager;
import org.nexopenframework.management.monitor.plugins.hyperic.HypericProcessManager;
import org.nexopenframework.management.monitor.scripting.ScriptMonitorManager;
import org.nexopenframework.management.monitor.support.CpuMonitor;
import org.nexopenframework.management.monitor.support.JvmHeapMonitor;
import org.nexopenframework.management.monitor.support.ServiceComponentMonitor;
import org.nexopenframework.management.support.MBeanServerHandle;
import org.w3c.dom.Element;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>JBoss extension of {@link ServiceMBeanSupport} for dealing with loading and adapter for a JBoss Application Server. 
 * Default solution is adding values inside JBoss configuration file.</p>
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
 * <p>Alternative solutions is adding a custom provider by name (supported names are available in {@link ProviderType} <code>enum</code>)</p>
 * 
 * <pre>  
 *     &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *     &lt;!DOCTYPE server&gt;
 *     &lt;server&gt;
 *     &lt;mbean code="org.nexopenframework.management.jee.monitor.jboss4x.ManagementService"
 * 		     name="openfrwk.management:service=ManagementService,appserver=jboss4x,type=mbean"
 * 			 display-name="NexOpen Monitoring Framework"&gt;  
 *         &lt;attribute name="provider"&gt;spring&lt;/attribute&gt;
 *         &lt;attribute name="resourceName"&gt;openfrwk-module-monitoring-components.xml&lt;/attribute&gt;
 *      &lt;/mbean&gt;
 *      &lt;/server&gt;  
 * </pre>
 * 
 * @see org.nexopenframework.management.jee.jboss4x.ManagementServiceMBean
 * @see org.jboss.system.ServiceMBeanSupport
 * @author Francesc Xavier Magdaleno
 * @version $Revision ,$Date 13/04/2009 21:56:19
 * @since 1.0.0.m1
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

    /**Custom implementation of {@link ConfigLoaderProvider} for loading needed configuration*/
    private ConfigLoaderProvider provider;

    /**
	 * <p>Name of resource to be load by {@link ConfigLoaderProvider}</p>
	 * 
	 * @since 1.0.0.m3
	 */
    private String resourceName;

    /**
	 * <p>Added a custom implementation of {@link ConfigLoaderProvider} for loading needed configuration
	 * to start monitoring in this application server.</p>
	 * 
	 * @since 1.0.0.m3
	 * @param provider a suitabl implementation for loading needed configuration
	 */
    public void setConfigLoaderProvider(final ConfigLoaderProvider provider) {
        this.provider = provider;
    }

    /**
	 * <p>Added a custom name of an implementation of {@link ConfigLoaderProvider} for loading needed configuration
	 * to start monitoring in this application server.</p>
	 * 
	 * @see org.nexopenframework.management.jee.loaders.ProviderType
	 * @since 1.0.0.m3
	 * @param name
	 */
    public void setProvider(final String name) {
        try {
            final ProviderType type = Enum.valueOf(ProviderType.class, name);
            this.provider = (ConfigLoaderProvider) forName(type.qualifiedClassName()).newInstance();
        } catch (final IllegalArgumentException e) {
            if (log.isInfoEnabled()) {
                log.info("[INFO] Detected problems with name [" + name + "]. Please, see ProviderType " + "in order to know which names are supported (def, spring,...).");
            }
        } catch (final NullPointerException e) {
            if (log.isInfoEnabled()) {
                log.info("Null value is passed as argument in setProvider of MonitorLifecycleListener");
            }
        } catch (final ClassNotFoundException e) {
            if (log.isInfoEnabled()) {
                log.info("ClassNotFoundException in setProvider of MonitorLifecycleListener. " + "See stack trace for more information.", e);
            }
        } catch (final Throwable e) {
            if (log.isInfoEnabled()) {
                log.info("Unexpected problem in setProvider of MonitorLifecycleListener. " + "See stack trace for more information.", e);
            }
        }
    }

    /**
	 * <p>Name of resource to be load by {@link ConfigLoaderProvider}</p>
	 * 
	 * @param resourceName
	 */
    public void setResourceName(final String resourceName) {
        this.resourceName = resourceName;
    }

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
    public void setChannels(final Element channels) throws ClassNotFoundException, AdapterManagementException {
        this.l_channels.clear();
        final DefaultConfigLoaderProvider defaultCfgLoader = ConfigurationDigester.getDefaultConfigLoaderProvider();
        this.l_channels.addAll(defaultCfgLoader.getChannels(channels));
    }

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.adapters.jboss4x.ManagementServiceMBean#setEvents(org.w3c.dom.Element)
	 */
    public void setEvents(final Element events) throws ClassNotFoundException, AdapterManagementException {
        this.l_events.clear();
        final DefaultConfigLoaderProvider defaultCfgLoader = ConfigurationDigester.getDefaultConfigLoaderProvider();
        this.l_events.addAll(defaultCfgLoader.getEvents(events));
    }

    public void setTasks(final Element tasks) throws ClassNotFoundException, AdapterManagementException {
        this.l_tasks.clear();
        final DefaultConfigLoaderProvider defaultCfgLoader = ConfigurationDigester.getDefaultConfigLoaderProvider();
        this.l_tasks.addAll(defaultCfgLoader.getTasks(tasks));
    }

    /**
	 * <p></p>
	 * 
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
    public void handleNotification(final Notification notification, final Object handback) {
        final String type = notification.getType();
        if (START_NOTIFICATION_TYPE.equals(type)) {
            final List<ResourceTaskMonitorBuilder> builders = new ArrayList<ResourceTaskMonitorBuilder>();
            try {
                builders.add(new JBoss4xConnectionPoolTaskMonitor.Builder());
                builders.add(new JBoss4xTransactionManagerTaskMonitor.Builder());
                builders.add(new JBoss4xJmsMessageCacheTaskMonitor.Builder());
                builders.add(new JBoss4xJmsDestinationTaskMonitor.Builder(true));
                builders.add(new JBoss4xJmsDestinationTaskMonitor.Builder(false));
                builders.add(new TomcatWebThreadPoolTaskMonitor.Builder("jboss.web:type=ThreadPool,*", "jboss4x"));
                final List<ResourceTaskMonitorBuilder> notCompleted = MBeanQueryManager.createAndRegisterServices(server, builders, manager);
                if (!notCompleted.isEmpty()) {
                    final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
                    service.schedule(new Runnable() {

                        public void run() {
                            for (final ResourceTaskMonitorBuilder builder : notCompleted) {
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
            throw new AdapterManagementException("Not correctly created. Problem registering as a NotificationListener", e);
        }
        ScriptMonitorManager smm = null;
        if (this.provider != null && (resourceName != null && resourceName.length() > 0)) {
            try {
                final Configuration cfg = ConfigurationDigester.buildConfiguration(this.resourceName, provider);
                this.l_channels.addAll(cfg.channels);
                this.l_events.addAll(cfg.events);
                this.l_tasks.addAll(cfg.tasks);
                smm = cfg.scriptMonitorManager;
            } catch (final ClassNotFoundException e) {
                throw new AdapterManagementException("problems loading resources from config file ", e);
            }
        }
        manager = new MonitorManager();
        manager.setServer(server);
        manager.setScriptMonitorManager(smm);
        if (!this.l_channels.isEmpty()) {
            this.manager.setChannels(l_channels);
        }
        if (!this.l_events.isEmpty()) {
            for (final EventMonitor eventMonitor : l_events) {
                this.manager.addEventMonitor(eventMonitor);
            }
        }
        if (!this.l_tasks.isEmpty()) {
            for (final TaskMonitor<? extends Number> taskMonitor : l_tasks) {
                manager.addTaskMonitor(taskMonitor);
            }
        }
        processDefaultConfiguration();
        manager.activateWaitAtStartup();
        repository = new ModuleRepository();
        repository.setServer(server);
        repository.init();
        this.log.info("Started NexOpen Monitoring Framework version [" + Version.getVersion() + "] " + "in JBoss AS [" + jbossPackage.getImplementationVersion() + "]");
    }

    /**
	 * <p>Handle default values of configuration</p>
	 * 
	 * @see org.nexopenframework.management.monitor.support.ServiceComponentMonitor
	 * @see org.nexopenframework.management.jee.monitor.support.HttpRequestMonitor
	 * @see org.nexopenframework.management.monitor.support.JvmHeapMonitor
	 * @see #handleCpuMonitor(List)
	 * @throws PluginException
	 */
    protected final void processDefaultConfiguration() throws PluginException {
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
    }

    /**
	 * <p>If <code>Sigar</code> is present in classpath handle monitoring of CPU</p>
	 * 
	 * @see org.nexopenframework.management.monitor.support.CpuMonitor
	 * @see org.nexopenframework.management.monitor.plugins.hyperic.HypericProcessManager
	 * @param task_monitors a list of {@link TaskMonitor} to add available {@link CpuMonitor}
	 * @throws PluginException if some problems happens during initialization
	 */
    protected void handleCpuMonitor(final List<TaskMonitor<? extends Number>> task_monitors) throws PluginException {
        final HypericProcessManager hypericProcessManager = new HypericProcessManager();
        hypericProcessManager.init();
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
