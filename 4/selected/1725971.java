package com.pentagaia.tb.start.testbase.tools;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import com.pentagaia.tb.start.api.IPdsAppKernelAppContext;
import com.pentagaia.tb.start.api.IPdsKernel;
import com.pentagaia.tb.start.api.IPdsKernelProvider;

/**
 * A kernel provider that tests kernel contextes
 * 
 * @author mepeisen
 * @version 0.1.0
 * @since 0.1.0
 */
public class AppContextKernelProvider implements IPdsKernelProvider, IPdsAppKernelAppContext {

    /** The application name */
    @SuppressWarnings("unused")
    private String appName;

    /** Managers */
    private Set<Object> managers = new HashSet<Object>();

    /** Services */
    private Set<Object> services = new HashSet<Object>();

    /** Channel manager class */
    private Class<?> channelManagerClazz;

    /** Data managers class */
    private Class<?> dataManagerClazz;

    /** Task manager class */
    private Class<?> taskManagerClazz;

    /** {@code true} if the method getChannelManager was called */
    public boolean getChannelManagerCalled = false;

    /** {@code true} if the method getDataManager was called */
    public boolean getDataManagerCalled = false;

    /** {@code true} if the method getTaskManager was called */
    public boolean getTaskManagerCalled = false;

    /** {@code true} if the method getManager was called */
    public boolean getManagerCalled = false;

    /** {@code true} if the method getService was called */
    public boolean getServiceCalled = false;

    /** {@code true} if the method shutdown was called */
    public boolean shutdownCalled = false;

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsKernelProvider#onPreStartup(com.pentagaia.tb.start.api.IPdsKernel)
     */
    public void onPreStartup(IPdsKernel kernel) {
        kernel.installAppKernelAppContext(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getChannelManager()
     */
    public Object getChannelManager() {
        this.getChannelManagerCalled = true;
        return getManager(this.channelManagerClazz);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getDataManager()
     */
    public Object getDataManager() {
        this.getDataManagerCalled = true;
        return getManager(this.dataManagerClazz);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getManager(java.lang.Class)
     */
    public <T> T getManager(Class<T> type) {
        this.getManagerCalled = true;
        for (final Object manager : this.managers) {
            if (type.isInstance(manager)) {
                return type.cast(manager);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getService(java.lang.Class)
     */
    public <T> T getService(Class<T> type) {
        this.getServiceCalled = true;
        for (final Object service : this.services) {
            if (type.isInstance(service)) {
                return type.cast(service);
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getTaskManager()
     */
    public Object getTaskManager() {
        this.getTaskManagerCalled = true;
        return getManager(this.taskManagerClazz);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#initialize(java.lang.String, java.util.Set, java.util.Set)
     */
    public void initialize(String applicationName, Set<Object> servicesSet, Set<Object> managersSet) {
        this.appName = applicationName;
        final Finder finder = AccessController.doPrivileged(new PrivilegedAction<Finder>() {

            public Finder run() {
                return new Finder();
            }
        });
        final Class<?>[] stack = finder.getClassContext();
        final ClassLoader loader = stack[2].getClassLoader();
        try {
            this.channelManagerClazz = loader.loadClass("com.sun.sgs.app.ChannelManager");
            this.dataManagerClazz = loader.loadClass("com.sun.sgs.app.DataManager");
            this.taskManagerClazz = loader.loadClass("com.sun.sgs.app.TaskManager");
        } catch (ClassNotFoundException e) {
            Assert.fail("Error fetching classes: " + e.getClass().getName() + " / " + e.getMessage());
        }
        this.managers.addAll(managersSet);
        this.services.addAll(servicesSet);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#listManagers()
     */
    public Iterable<Object> listManagers() {
        return Collections.unmodifiableCollection(this.managers);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#listServices()
     */
    public Iterable<Object> listServices() {
        return Collections.unmodifiableCollection(this.services);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#shutdownServices()
     */
    public void shutdownServices() {
        this.shutdownCalled = true;
        final ArrayList<Object> list = new ArrayList<Object>();
        for (final Object service : this.listServices()) {
            list.add(service);
        }
        Collections.reverse(list);
        for (final Object service : list) {
            try {
                service.getClass().getMethod("shutdown").invoke(service);
            } catch (Exception e) {
            }
        }
    }

    /**
     * A small class to receive the execution class context
     */
    static final class Finder extends SecurityManager {

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.SecurityManager#getClassContext()
         */
        @Override
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
}
