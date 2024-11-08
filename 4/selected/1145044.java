package com.googlecode.acpj.internal.config;

import com.googlecode.acpj.Arguments;
import com.googlecode.acpj.actors.ActorFactory;
import com.googlecode.acpj.channels.ChannelFactory;
import com.googlecode.acpj.channels.ChannelRegistry;
import com.googlecode.acpj.internal.actors.ExecutorBasedActorFactory;
import com.googlecode.acpj.internal.channels.DefaultChannelFactory;
import com.googlecode.acpj.internal.channels.DefaultChannelRegistry;

/**
 * <p>
 * Internal - used to retrieve configurable properties, will look in system 
 * properties first then use local defaults. This might even become a 3-tier
 * system by adding a properties file.
 * </p>
 * 
 * @author Simon Johnston (simon@johnstonshome.org)
 * @since 0.1.0
 * 
 */
public class Configuration {

    @SuppressWarnings("unchecked")
    public static Class<? extends ChannelFactory> getDefaultChannelFactoryImpl() {
        String className = System.getProperty(Arguments.CFG_DEFAULT_CHANNEL_FACTORY_CLASS);
        if (className == null) {
            return DefaultChannelFactory.class;
        }
        Class<? extends ChannelFactory> theClass = null;
        try {
            theClass = (Class<? extends ChannelFactory>) Configuration.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return theClass;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends ChannelRegistry> getDefaultChannelRegistryImpl() {
        String className = System.getProperty(Arguments.CFG_DEFAULT_CHANNEL_REGISTRY_CLASS);
        if (className == null) {
            return DefaultChannelRegistry.class;
        }
        Class<? extends ChannelRegistry> theClass = null;
        try {
            theClass = (Class<? extends ChannelRegistry>) Configuration.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return theClass;
    }

    @SuppressWarnings("unchecked")
    public static Class<? extends ActorFactory> getDefaultActorFactoryImpl() {
        String className = System.getProperty(Arguments.CFG_DEFAULT_CHANNEL_FACTORY_CLASS);
        if (className == null) {
            return ExecutorBasedActorFactory.class;
        }
        Class<? extends ActorFactory> theClass = null;
        try {
            theClass = (Class<? extends ActorFactory>) Configuration.class.getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return theClass;
    }

    public static int getThreadPoolSize() {
        int processors = Runtime.getRuntime().availableProcessors();
        String poolSize = System.getProperty(Arguments.CFG_THREAD_POOL_SIZE, String.valueOf(processors * 4));
        return Integer.parseInt(poolSize);
    }

    public static boolean getChannelMonitorStatus() {
        String monitorChannels = System.getProperty(Arguments.CFG_MONITOR_CHANNELS, "true");
        return Boolean.parseBoolean(monitorChannels);
    }
}
