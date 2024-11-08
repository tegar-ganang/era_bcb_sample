package com.sun.sgs.impl.kernel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Set;
import com.pentagaia.tb.start.api.IPdsAppKernelAppContext;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.ManagerNotFoundException;
import com.sun.sgs.app.TaskManager;
import com.sun.sgs.kernel.ComponentRegistry;
import com.sun.sgs.service.Service;

/**
 * This is the implementation of <code>KernelContext</code> used by the kernel to manage the context of a single application. It knows the name of
 * an application, its available managers, and its backing services.
 * 
 * FIXME: the context should check that it isn't shutdown before handing out services and managers.
 * 
 * This class is compatible to kernel 0.9.7
 * 
 * @author this is a copy of the original KernelContext provided by sun. This class was changed to use AppKernelAppContext proxies.
 * @see IPdsAppKernelAppContext
 */
class KernelContext {

    /** the managers available in this context */
    protected final ComponentRegistry managerComponents;

    /** the services used in this context */
    protected final ComponentRegistry serviceComponents;

    /** standard manager */
    private final ChannelManager channelManager;

    /** standard manager */
    private final DataManager dataManager;

    /** standard manager */
    private final TaskManager taskManager;

    /** the application's name */
    private final String applicationName;

    /** the context that should be used */
    private IPdsAppKernelAppContext fContextProxy;

    /**
     * Creates an instance of <code>KernelContext</code> based on the components that have already been collected in another instance.
     * 
     * @param context
     *            the existing <code>KernelContext</code> to use as a source of components
     */
    KernelContext(final KernelContext context) {
        this(context.applicationName, context.serviceComponents, context.managerComponents);
    }

    /**
     * Creates an instance of <code>AppKernelAppContext</code>.
     * 
     * @param appName
     *            the name of the application represented by this context
     * @param services
     *            the services available in this context
     * @param managers
     *            the managers available in this context
     */
    protected KernelContext(final String appName, final ComponentRegistry services, final ComponentRegistry managers) {
        this.applicationName = appName;
        this.serviceComponents = services;
        this.managerComponents = managers;
        ChannelManager cm;
        try {
            cm = this.managerComponents.getComponent(ChannelManager.class);
        } catch (final MissingResourceException mre) {
            cm = null;
        }
        this.channelManager = cm;
        DataManager dm;
        try {
            dm = this.managerComponents.getComponent(DataManager.class);
        } catch (final MissingResourceException mre) {
            dm = null;
        }
        this.dataManager = dm;
        TaskManager tm;
        try {
            tm = this.managerComponents.getComponent(TaskManager.class);
        } catch (final MissingResourceException mre) {
            tm = null;
        }
        this.taskManager = tm;
    }

    /**
     * Installs a custom app context
     * 
     * @param context
     *            the app context that should be used
     */
    synchronized void installCustomContext(final IPdsAppKernelAppContext context) {
        this.fContextProxy = context;
        final Set<Object> services = new HashSet<Object>();
        final Set<Object> managers = new HashSet<Object>();
        for (final Object service : this.serviceComponents) {
            services.add(service);
        }
        for (final Object manager : this.managerComponents) {
            managers.add(manager);
        }
        this.fContextProxy.initialize(this.toString(), services, managers);
    }

    /**
     * Returns the <code>ChannelManager</code> used in this context.
     * 
     * @return the context's <code>ChannelManager</code>
     */
    ChannelManager getChannelManager() {
        if (this.fContextProxy != null) {
            return (ChannelManager) this.fContextProxy.getChannelManager();
        }
        if (this.channelManager == null) {
            throw new ManagerNotFoundException("this application is running without a ChannelManager");
        }
        return this.channelManager;
    }

    /**
     * Returns the <code>DataManager</code> used in this context.
     * 
     * @return the context's <code>DataManager</code>
     */
    DataManager getDataManager() {
        if (this.fContextProxy != null) {
            return (DataManager) this.fContextProxy.getDataManager();
        }
        if (this.dataManager == null) {
            throw new ManagerNotFoundException("this application is running without a DataManager");
        }
        return this.dataManager;
    }

    /**
     * Returns the <code>TaskManager</code> used in this context.
     * 
     * @return the context's <code>TaskManager</code>
     */
    TaskManager getTaskManager() {
        if (this.fContextProxy != null) {
            return (TaskManager) this.fContextProxy.getTaskManager();
        }
        if (this.taskManager == null) {
            throw new ManagerNotFoundException("this application is running without a TaskManager");
        }
        return this.taskManager;
    }

    /**
     * Returns a manager based on the given type. If the manager type is unknown, or if there is more than one manager of the given type,
     * <code>ManagerNotFoundException</code> is thrown. This may be used to find any available manager, including the three standard managers.
     * 
     * @param <T>
     *            the <code>Class</code> of the requested manager
     * 
     * @param type
     *            the <code>Class</code> of the requested manager
     * 
     * @return the requested manager
     * 
     * @throws ManagerNotFoundException
     *             if there wasn't exactly one match to the requested type
     */
    <T> T getManager(final Class<T> type) {
        if (this.fContextProxy != null) {
            return this.fContextProxy.getManager(type);
        }
        try {
            return this.managerComponents.getComponent(type);
        } catch (final MissingResourceException mre) {
            throw new ManagerNotFoundException("couldn't find manager: " + type.getName());
        }
    }

    /**
     * Returns a <code>Service</code> based on the given type. If the type is unknown, or if there is more than one <code>Service</code> of the
     * given type, <code>MissingResourceException</code> is thrown. This is the only way to resolve service components directly, and should be used
     * with care, as <code>Service</code>s should not be resolved and invoked directly outside of a transactional context.
     * 
     * @param <T>
     *            the <code>Class</code> of the requested <code>Service</code>
     * @param type
     *            the <code>Class</code> of the requested <code>Service</code>
     * 
     * @return the requested <code>Service</code>
     * 
     * @throws MissingResourceException
     *             if there wasn't exactly one match to the requested type
     */
    <T extends Service> T getService(final Class<T> type) {
        if (this.fContextProxy != null) {
            return this.fContextProxy.getService(type);
        }
        return this.serviceComponents.getComponent(type);
    }

    /**
     * Notifies all included Services that the system is now ready.
     * 
     * @throws Exception
     *             if there is any failure during notification
     */
    void notifyReady() throws Exception {
        if (this.fContextProxy != null) {
            for (final Object service : this.fContextProxy.listServices()) {
                ((Service) service).ready();
            }
            UtilizedKernel097.contextReady(this);
            return;
        }
        for (final Object service : this.serviceComponents) {
            ((Service) service).ready();
        }
        UtilizedKernel097.contextReady(this);
    }

    /**
     * Shut down all the service components in the reverse order that they were added.
     */
    void shutdownServices() {
        if (this.fContextProxy != null) {
            this.fContextProxy.shutdownServices();
            return;
        }
        final ArrayList<Object> list = new ArrayList<Object>();
        for (final Object service : this.serviceComponents) {
            list.add(service);
        }
        Collections.reverse(list);
        for (final Object service : list) {
            ((Service) service).shutdown();
        }
    }

    /**
     * Returns a unique representation of this context, in this case the name of the application.
     * 
     * @return a <code>String</code> representation of the context
     */
    @Override
    public String toString() {
        return this.applicationName;
    }
}
