package com.pentagaia.tb.start.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.pentagaia.tb.start.api.IPdsAppKernelAppContext;

/**
 * Implementation of the app kernel app context
 * 
 * @author mepeisen
 * @version 0.1.0
 * @since 0.1.0
 */
class AppContextImpl implements IPdsAppKernelAppContext {

    /** The kernels app context */
    private final Object fAppContext;

    /** The get channel manager method */
    private final Method fGetChannelManager;

    /** The get data manager method */
    private final Method fGetDataManager;

    /** The get task manager method */
    private final Method fGetTaskManager;

    /** The get manager method */
    private final Method fGetManager;

    /** The get service method */
    private final Method fGetService;

    /** The services field */
    private final Field fServices;

    /** The managers field */
    private final Field fManagers;

    /**
     * Constructor
     * 
     * @param context
     *            the app context
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws NoSuchFieldException
     */
    public AppContextImpl(final Object context) throws SecurityException, NoSuchMethodException, NoSuchFieldException {
        this.fAppContext = context;
        final Class<?> clazz = this.fAppContext.getClass();
        this.fGetChannelManager = clazz.getDeclaredMethod("getChannelManager");
        this.fGetDataManager = clazz.getDeclaredMethod("getDataManager");
        this.fGetTaskManager = clazz.getDeclaredMethod("getTaskManager");
        this.fGetManager = clazz.getDeclaredMethod("getManager", Class.class);
        this.fGetService = clazz.getDeclaredMethod("getService", Class.class);
        this.fServices = clazz.getDeclaredField("serviceComponents");
        this.fManagers = clazz.getDeclaredField("managerComponents");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getChannelManager()
     */
    public synchronized Object getChannelManager() {
        try {
            this.fGetChannelManager.setAccessible(true);
            return this.fGetChannelManager.invoke(this.fAppContext);
        } catch (final Exception e) {
            return null;
        } finally {
            this.fGetChannelManager.setAccessible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getDataManager()
     */
    public synchronized Object getDataManager() {
        try {
            this.fGetDataManager.setAccessible(true);
            return this.fGetDataManager.invoke(this.fAppContext);
        } catch (final Exception e) {
            return null;
        } finally {
            this.fGetDataManager.setAccessible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getManager(java.lang.Class)
     */
    public synchronized <T> T getManager(final Class<T> type) {
        try {
            this.fGetManager.setAccessible(true);
            return type.cast(this.fGetManager.invoke(this.fAppContext, type));
        } catch (final Exception e) {
            return null;
        } finally {
            this.fGetManager.setAccessible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getService(java.lang.Class)
     */
    public synchronized <T> T getService(final Class<T> type) {
        try {
            this.fGetService.setAccessible(true);
            return type.cast(this.fGetService.invoke(this.fAppContext, type));
        } catch (final Exception e) {
            return null;
        } finally {
            this.fGetService.setAccessible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#getTaskManager()
     */
    public synchronized Object getTaskManager() {
        try {
            this.fGetTaskManager.setAccessible(true);
            return this.fGetTaskManager.invoke(this.fAppContext);
        } catch (final Exception e) {
            return null;
        } finally {
            this.fGetTaskManager.setAccessible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#initialize(java.lang.String, java.util.Set, java.util.Set)
     */
    public void initialize(final String applicationName, final Set<Object> services, final Set<Object> managers) {
        throw new IllegalStateException("Cannot initialize this context");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#shutdownServices()
     */
    public void shutdownServices() {
        throw new IllegalStateException("Cannot shutdown this context");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#listManagers()
     */
    @SuppressWarnings("unchecked")
    public synchronized Iterable<Object> listManagers() {
        try {
            this.fManagers.setAccessible(true);
            final List<Object> result = new ArrayList<Object>();
            final Iterable<Object> iter = (Iterable<Object>) this.fManagers.get(this.fAppContext);
            for (final Object object : iter) {
                result.add(object);
            }
            return result;
        } catch (final Exception e) {
            return null;
        } finally {
            this.fManagers.setAccessible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsAppKernelAppContext#listServices()
     */
    @SuppressWarnings("unchecked")
    public synchronized Iterable<Object> listServices() {
        try {
            this.fServices.setAccessible(true);
            final List<Object> result = new ArrayList<Object>();
            final Iterable<Object> iter = (Iterable<Object>) this.fServices.get(this.fAppContext);
            for (final Object object : iter) {
                result.add(object);
            }
            return result;
        } catch (final Exception e) {
            return null;
        } finally {
            this.fServices.setAccessible(false);
        }
    }
}
