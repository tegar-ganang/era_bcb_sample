package org.nexopenframework.management.adapters.loaders;

import static org.nexopenframework.management.support.ClassUtils.isPresent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nexopenframework.management.adapters.AdapterManagementException;
import org.nexopenframework.management.monitor.EventMonitor;
import org.nexopenframework.management.monitor.TaskMonitor;
import org.nexopenframework.management.monitor.channels.ChannelNotification;
import org.nexopenframework.management.monitor.scripting.ScriptMonitorManager;
import org.nexopenframework.management.monitor.scripting.groovy.GroovyScriptMonitorManager;
import org.nexopenframework.management.monitor.support.PropertiesLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>NexOpen Framework</p>
 *
 * <p>Default class for read and create objects of type {@link ChannelNotification}, {@link EventMonitor} 
 * and {@link TaskMonitor}. This class reads files with a given XML format, as given in following example.</p>
 * 
 * <pre>
 *  &lt;components-configuration&gt;
 *   &lt;!-- List of channels --&gt;
 *   &lt;script-monitor-manager class="com.mypackage.MyScriptMonitorManager"/&gt;
 *   &lt;!-- List of channels --&gt;
 *   &lt;channels-list&gt;
 *     &lt;channel class="org.nexopenframework.management.monitor.channels.LogChannelNotification"/&gt;
 *     &lt;channel class="org.nexopenframework.management.spring.monitor.channels.ExternalEmailChannelNotification"&gt;
 *         &lt;property name="from" value="notifier@nexopen.org"/&gt;
 *         &lt;property name="to" value="magdaleno@strands.com"/&gt;
 *         &lt;property name="jndiName" value="java:/strandsMail"/&gt;
 *         &lt;property name="period" value="2000000"/&gt;
 *     &lt;/channel&gt;
 *   &lt;/channels-list&gt;
 *   &lt;!-- List of events --&gt;
 *   &lt;events-list&gt;
 *       &lt;event class="com.mycompany.MyEventMonitor"&gt;
 *          &lt;property name="property1" value="some.value.property1"/&gt;
 *       &lt;/event&gt;
 *   &lt;/events-list&gt;
 *   &lt;!-- List of tasks --&gt;
 *   &lt;tasks-list&gt;
 *       &lt;task class="com.mycompany.MyTaskMonitor"&gt;
 *          &lt;property name="property1" value="some.value.property1"/&gt;
 *       &lt;/task&gt;
 *   &lt;/tasks-list&gt;
 *  &lt;/components-configuration&gt; 
 * </pre>
 *
 * @see org.nexopenframework.management.jee.loaders.ConfigLoaderProvider
 * @author Francesc Xavier Magdaleno Escar
 * @version $Revision ,$Date 12/04/2009 21:02:13
 * @since 1.0.0.m3
 */
public class DefaultConfigLoaderProvider implements ConfigLoaderProvider {

    /**logging facility based in Jakarta Commons Logging*/
    static final Log logger = LogFactory.getLog(DefaultConfigLoaderProvider.class);

    /**
	 * <p></p>
	 * 
	 * @see org.nexopenframework.management.jee.loaders.ConfigLoaderProvider#buildConfiguration(java.io.InputStream)
	 */
    public Configuration buildConfiguration(final String resourceName, final ClassLoader cls) throws ClassNotFoundException {
        final InputStream is = cls.getResourceAsStream(resourceName);
        if (is == null) {
            if (logger.isInfoEnabled()) {
                logger.info("No found resource file for name " + resourceName);
            }
            return new Configuration();
        }
        try {
            final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = db.parse(is);
            final Element components = doc.getDocumentElement();
            return new Configuration(getChannels(components, cls), getEvents(components, cls), getTasks(components, cls), getScriptMonitorManager(components, cls));
        } catch (final SAXException e) {
            throw new AdapterManagementException(e);
        } catch (final IOException e) {
            throw new AdapterManagementException(e);
        } catch (final ParserConfigurationException e) {
            throw new AdapterManagementException("DocumentBuilder could not be created", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("I/O problem closing resource " + resourceName, e);
                    }
                }
            }
        }
    }

    /**
	 * <p>Create a suitable implementation of {@link ScriptMonitorManager} under element <code>script-monitor-manager</code>.</p>
	 * <p>If it is not found this element, we check existence of <code>groovy</code> library in <i>classpath</i> and if it is found, 
	 * we return an instance of {@link GroovyScriptMonitorManager}. Otherwise, we will return <code>null</code>.</p>
	 * 
	 * @param components
	 * @param cls
	 * @return a suitable instance of ScriptMonitorManager or <code>null</code>
	 * @throws ClassNotFoundException
	 */
    protected final ScriptMonitorManager getScriptMonitorManager(final Element components, final ClassLoader cls) throws ClassNotFoundException {
        final Iterator<Element> it_smm = getChildrenByTagName(components, "script-monitor-manager");
        if (!it_smm.hasNext()) {
            if (logger.isInfoEnabled()) {
                logger.info("[INFO] No <script-monitor-manager/> element found. We will use GroovyScriptMonitorManager " + "if groovy library is present in classpath. Otherwise, a null value will be returned.");
                logger.info("[INFO] In case of groovy-all-1.x.jar is found in classpath, we assume " + "existence of a groovy folder in classpath, where groovy files are located.");
            }
            return (isPresent("groovy.lang.GroovyClassLoader", cls)) ? new GroovyScriptMonitorManager() : null;
        }
        try {
            final Element elemScript = it_smm.next();
            final String className = elemScript.getAttribute("class");
            final ScriptMonitorManager smm = (ScriptMonitorManager) cls.loadClass(className).newInstance();
            if (smm instanceof PropertiesLoader) {
                final Iterator<Element> it_properties = getChildrenByTagName(elemScript, "property");
                final Map<String, Object> properties = new HashMap<String, Object>();
                while (it_properties.hasNext()) {
                    final Element prop = it_properties.next();
                    properties.put(prop.getAttribute("name"), prop.getAttribute("value"));
                }
                ((PropertiesLoader) smm).loadProperties(properties);
            }
            return smm;
        } catch (final InstantiationException e) {
            throw new AdapterManagementException(e);
        } catch (final IllegalAccessException e) {
            throw new AdapterManagementException(e);
        }
    }

    /**
	 * <p>Retrieves a list of {@link ChannelNotification} from a W3C {@link Element}</p>
	 * 
	 * <p><b>IMPORTANT NOTE</b> : Please, you must be sure that all classes defined in this file, they are present in given Classloader</p>
	 * 
	 * @see org.nexopenframework.management.monitor.support.PropertiesLoader
	 * @param channels
	 * @param cls
	 * @return
	 * @throws ClassNotFoundException if someone of specific class is not found
	 */
    public final List<ChannelNotification> getChannels(final Element channels, final ClassLoader cls) throws ClassNotFoundException {
        final List<ChannelNotification> l_channels = new LinkedList<ChannelNotification>();
        final Iterator<Element> it_channels = getChildrenByTagName(channels, "channel");
        try {
            while (it_channels.hasNext()) {
                final Element elemChannel = it_channels.next();
                final String className = elemChannel.getAttribute("class");
                final ChannelNotification clazz = (ChannelNotification) cls.loadClass(className).newInstance();
                if (clazz instanceof PropertiesLoader) {
                    final Iterator<Element> it_properties = getChildrenByTagName(elemChannel, "property");
                    final Map<String, Object> properties = new HashMap<String, Object>();
                    while (it_properties.hasNext()) {
                        final Element prop = it_properties.next();
                        properties.put(prop.getAttribute("name"), prop.getAttribute("value"));
                    }
                    ((PropertiesLoader) clazz).loadProperties(properties);
                }
                l_channels.add(clazz);
            }
        } catch (final InstantiationException e) {
            throw new AdapterManagementException(e);
        } catch (final IllegalAccessException e) {
            throw new AdapterManagementException(e);
        }
        return l_channels;
    }

    /**
	 * <p></p>
	 * 
	 * @param channels
	 * @return
	 * @throws ClassNotFoundException
	 */
    public final List<ChannelNotification> getChannels(final Element channels) throws ClassNotFoundException {
        return getChannels(channels, Thread.currentThread().getContextClassLoader());
    }

    public final List<EventMonitor> getEvents(final Element events, final ClassLoader cls) throws ClassNotFoundException {
        final List<EventMonitor> l_events = new LinkedList<EventMonitor>();
        final Iterator<Element> it_events = getChildrenByTagName(events, "event");
        try {
            while (it_events.hasNext()) {
                final Element elemChannel = it_events.next();
                final String className = elemChannel.getAttribute("class");
                final EventMonitor clazz = (EventMonitor) cls.loadClass(className).newInstance();
                if (clazz instanceof PropertiesLoader) {
                    final Iterator<Element> it_properties = getChildrenByTagName(elemChannel, "property");
                    final Map<String, Object> properties = new HashMap<String, Object>();
                    while (it_properties.hasNext()) {
                        final Element prop = it_properties.next();
                        properties.put(prop.getAttribute("name"), prop.getAttribute("value"));
                    }
                    ((PropertiesLoader) clazz).loadProperties(properties);
                }
                l_events.add(clazz);
            }
        } catch (final InstantiationException e) {
            throw new AdapterManagementException(e);
        } catch (final IllegalAccessException e) {
            throw new AdapterManagementException(e);
        }
        return l_events;
    }

    /**
	 * <p></p>
	 * 
	 * @param events
	 * @return
	 * @throws ClassNotFoundException
	 */
    public final List<EventMonitor> getEvents(final Element events) throws ClassNotFoundException {
        return getEvents(events, Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings("unchecked")
    protected final List<TaskMonitor<? extends Number>> getTasks(final Element events, final ClassLoader cls) throws ClassNotFoundException {
        final List<TaskMonitor<? extends Number>> l_tasks = new LinkedList<TaskMonitor<? extends Number>>();
        final Iterator<Element> it_tasks = getChildrenByTagName(events, "task");
        try {
            while (it_tasks.hasNext()) {
                final Element elemChannel = it_tasks.next();
                final String className = elemChannel.getAttribute("class");
                final TaskMonitor<? extends Number> clazz = (TaskMonitor<? extends Number>) cls.loadClass(className).newInstance();
                if (clazz instanceof PropertiesLoader) {
                    final Iterator<Element> it_properties = getChildrenByTagName(elemChannel, "property");
                    final Map<String, Object> properties = new HashMap<String, Object>();
                    while (it_properties.hasNext()) {
                        final Element prop = it_properties.next();
                        properties.put(prop.getAttribute("name"), prop.getAttribute("value"));
                    }
                    ((PropertiesLoader) clazz).loadProperties(properties);
                }
                l_tasks.add(clazz);
            }
        } catch (final InstantiationException e) {
            throw new AdapterManagementException(e);
        } catch (final IllegalAccessException e) {
            throw new AdapterManagementException(e);
        }
        return l_tasks;
    }

    /**
	 * <p></p>
	 * 
	 * @param tasks
	 * @return
	 * @throws ClassNotFoundException
	 */
    public final List<TaskMonitor<? extends Number>> getTasks(final Element tasks) throws ClassNotFoundException {
        return getTasks(tasks, Thread.currentThread().getContextClassLoader());
    }

    /**
	 * Returns an iterator over the children of the given element with the given
	 * tag name.
	 * 
	 * @param element
	 *            The parent element
	 * @param tagName
	 *            The name of the desired child
	 * @return An iterator of children or null if element is null
	 */
    final Iterator<Element> getChildrenByTagName(final Element element, final String tagName) {
        if (element == null) {
            return null;
        }
        final NodeList children = element.getChildNodes();
        final ArrayList<Element> goodChildren = new ArrayList<Element>(children.getLength());
        for (int i = 0; i < children.getLength(); i++) {
            final Node node = children.item(i);
            if (node instanceof Element && nodeNameEquals(node, tagName)) {
                goodChildren.add((Element) node);
            }
            if (node instanceof Element) {
                final Element elem = (Element) node;
                final Iterator<Element> childs = getChildrenByTagName(elem, tagName);
                if (childs != null) {
                    while (childs.hasNext()) {
                        goodChildren.add(childs.next());
                    }
                }
            }
        }
        return goodChildren.iterator();
    }

    /**
	 * <p>
	 * </p>
	 * 
	 * @param node
	 * @param desiredName
	 * @return
	 */
    final boolean nodeNameEquals(final Node node, final String desiredName) {
        return desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName());
    }
}
