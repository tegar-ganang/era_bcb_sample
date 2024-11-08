package org.pagger.util.config;

import java.beans.ExceptionListener;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.pagger.util.Observable;

/**
 * This is the main class for the configuration library. First you define some 
 * configuration interface:
 * [code]
 *     #@Configuration("TestConfiguration")
 *     public interface Config {
 *         
 *         #@Section("SectionOne")
 *         public interface SectionOne {
 *         
 *             #@Entry(name = "EntryNameOne", value = "DefaultValue")
 *             public String getEntryNameOne();
 *             public void setEntryName(String entry);
 *             
 *             #@Entry(name = "ReadOnly", value = "Value")
 *             public String getReadOnly();
 *         
 *         }
 *         
 *         #@Section("SectionTwo")
 *         public interface SectionTwo {
 *             
 *             #@Entry(name = "Entry")
 *             public Integer getEntry();
 *             public void setEntry(Integer entry);
 *             
 *         }
 *         
 *         public SectionOne getSectionOne();
 *         
 *         public SectionTwo getSectionTwo();
 *     }
 * [/code]
 * 
 * After you have defined the configuration interface you can create an instance with
 * the {@code ConfigBuilder}:
 * [code]
 *     public class Foo {
 *         //Usually you create a static final instance of your configuration,
 *         //because two consecutive calls of ConfigBuilder.config(Config.class)
 *         //will return the same reference.
 *         private static final Config CONFIG = ConfigBuilder.config(Config.class);
 *     
 *         public static void main(String[] args) {
 *             //Return the same reference.
 *             assert (CONFIG == ConfigBuilder.config(Config.class));
 *             
 *             CONFIG.getSectionOne().setEntryName("New entry name");
 *             System.out.println(CONFIG.getSectionOne().getEntryName());
 *         }
 *     }
 * [/code]
 * 
 * This code will print
 * <pre>
 *     New entry name
 * </pre>
 * on the console. By default the configuration is stored with the java preference
 * package. 
 * 
 * Each object created with {@link ConfigBuilder#build(Class)} will implement
 * the {@link Observable} interface. So you can add a
 * {@link PropertyChangeListener} and listen to property changes.
 * [code]
 *     Config config = ConfigBuilder.config(Config.class);
 *     PropertyChangeObservable observable = (PropertyChangeObservable)config;
 *     observable.addPropertyChangeListener("SectionOne.EntryName", new PropertyChangeListener() {
 *         #@Override public void propertyChange(final PropertyChangeEvent evt) {
 *             System.out.println("New Value: " + evt.getNewValue();
 *         }
 *     });
 *     
 *     //This method call will print "New Value: New entry value" to the console.
 *     config.getSectionOne().setEntryName("New entry value");
 * [/code]
 * 
 * @author Franz Wilhelmstötter
 */
public final class ConfigBuilder {

    private static ConfigurationReaderFactory readerFactory = PrefsConfigurationReader.FACTORY;

    private static ConfigurationWriterFactory writerFactory = PrefsConfigurationWriter.FACTORY;

    private static ExceptionListener exceptionListener = new ConsoleLogExceptionHandler();

    private static ConfigurationListener configListener = null;

    private static final Map<Class<?>, Object> configurations = new HashMap<Class<?>, Object>();

    private ConfigBuilder() {
    }

    public static synchronized <T> T build(final Class<T> type) {
        if (!configurations.containsKey(type)) {
            Configuration annotation = type.getAnnotation(Configuration.class);
            if (annotation == null) {
                throw new IllegalConfigurationException("Expected " + Configuration.class.getCanonicalName() + "annotation for interface " + type.getCanonicalName());
            }
            Data.Configuration configuration = new Data.Configuration(annotation.value());
            ConfigUtils.build(type, configuration);
            try {
                final ConfigurationReader reader = readerFactory.newReader(type);
                ConfigurationData data = reader.read(annotation.value());
                configuration.update(data);
                final ConfigurationWriter writer = writerFactory.newWriter(type);
                writer.write(configuration);
            } catch (IOException e) {
                exceptionListener.exceptionThrown(e);
            }
            configuration.addConfigurationListener(new ReadWriteConfigurationListener(type, readerFactory, writerFactory, exceptionListener));
            if (configListener != null) {
                configuration.addConfigurationListener(configListener);
            }
            SectionsHandler handler = new SectionsHandler(configuration);
            Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type, Observable.class }, handler);
            configurations.put(type, proxy);
        }
        return type.cast(configurations.get(type));
    }

    public static synchronized void setConfigurationReaderFactory(final ConfigurationReaderFactory readerFactory) {
        if (readerFactory == null) {
            throw new NullPointerException("Configuration reader must not be null.");
        }
        ConfigBuilder.readerFactory = readerFactory;
    }

    public static synchronized void setConfigurationWriterFactory(final ConfigurationWriterFactory writerFactory) {
        if (writerFactory == null) {
            throw new NullPointerException("Configuration writer must not be null.");
        }
        ConfigBuilder.writerFactory = writerFactory;
    }

    static synchronized void setConfigListener(final ConfigurationListener configListener) {
        ConfigBuilder.configListener = configListener;
    }

    public static synchronized void setExceptionListener(final ExceptionListener exceptionListener) {
        if (exceptionListener == null) {
            throw new NullPointerException("Error handler must not be null.");
        }
        ConfigBuilder.exceptionListener = exceptionListener;
    }
}
