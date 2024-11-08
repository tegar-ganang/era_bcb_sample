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
 * This is the implementation of <code>KernelAppContext</code> used by the kernel to manage the context of a single application. It knows the name
 * of an application, its available managers, and its backing services.
 * 
 * FIXME: the context should check that it isn't shutdown before handing out services and managers.
 *
 * This class is compatible to kernel 0.9.5.1
 * 
 * @author this is a copy of the original AppKernelAppContext provided by sun. This class was changed to use AppKernelAppContext proxies.
 * @see IPdsAppKernelAppContext
 */
class AppKernelAppContext extends AbstractKernelAppContext {

    /** the managers available in this context */
    private final ComponentRegistry managerComponents;

    /** the services used in this context */
    private final ComponentRegistry serviceComponents;

    /** standard manager */
    private final ChannelManager channelManager;

    /** standard manager */
    private final DataManager dataManager;

    /** standard manager */
    private final TaskManager taskManager;

    /** the context that should be used */
    private IPdsAppKernelAppContext fContextProxy;

    /**
     * Creates an instance of <code>AppKernelAppContext</code>.
     * 
     * @param applicationName
     *            the name of the application represented by this context
     * @param services
     *            the services available in this context
     * @param managers
     *            the managers available in this context
     */
    AppKernelAppContext(final String applicationName, final ComponentRegistry services, final ComponentRegistry managers) {
        super(applicationName);
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    <T extends Service> T getService(final Class<T> type) {
        if (this.fContextProxy != null) {
            return this.fContextProxy.getService(type);
        }
        return this.serviceComponents.getComponent(type);
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
}
