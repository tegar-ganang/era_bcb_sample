package org.nexopenframework.management.jee.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.nexopenframework.management.jee.JeeManagementException;
import org.nexopenframework.management.monitor.channels.ChannelNotification;
import org.nexopenframework.management.monitor.support.PropertiesLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Generic class for read and create objects of type {@link ChannelNotification}. This class reads
 * files with a given XML format, as given in following example.</p>
 * 
 * <pre>
 *   &lt;channels-list&gt;
 *     &lt;channel class="org.nexopenframework.management.monitor.channels.LogChannelNotification"/&gt;
 *     &lt;channel class="org.nexopenframework.management.spring.monitor.channels.ExternalEmailChannelNotification"&gt;
 *         &lt;property name="from" value="notifier@nexopen.org"/&gt;
 *         &lt;property name="to" value="magdaleno@strands.com"/&gt;
 *         &lt;property name="jndiName" value="java:/strandsMail"/&gt;
 *         &lt;property name="period" value="2000000"/&gt;
 *     &lt;/channel&gt;
 *   &lt;/channels-list&gt;
 * </pre>
 * 
 * @see org.nexopenframework.management.monitor.channels.ChannelNotification
 * @see org.nexopenframework.management.monitor.support.PropertiesLoader
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0.0.m2
 */
public abstract class ChannelsDigester {

    /**
	 * <p>Avoid explicit creation</p>
	 */
    private ChannelsDigester() {
        super();
    }

    /**
	 * <p>Retrieve a list of {@link ChannelNotification} from a resource file located in classpath.</p>
	 * 
	 * @param resourceName a file name located somewhere in classpath
	 * @return a list of {@link ChannelNotification} or an empty list if resource not found in classpath
	 * @throws ClassNotFoundException If some class is not found in current classloader
	 */
    public static List<ChannelNotification> getChannels(final String resourceName) throws ClassNotFoundException {
        return getChannels(resourceName, Thread.currentThread().getContextClassLoader());
    }

    /**
	 * <p>Retrieve a list of {@link ChannelNotification} from a resource file located in classpath. We add
	 * a {@link ClassLoader} which contains classes to be created.</p>
	 * 
	 * <p><b>IMPORTANT NOTE</b> : Please, you must be sure that all classes defined in this file, they are present in given Classloader</p>
	 * 
	 * @see #getChannels(Element, ClassLoader)
	 * @param resourceName a file name located somewhere in classpath
	 * @param cls {@link ClassLoader} for loading classes contained in resource file
	 * @return a list of {@link ChannelNotification} or an empty list if resource not found in classpath
	 * @throws ClassNotFoundException 
	 */
    public static List<ChannelNotification> getChannels(final String resourceName, final ClassLoader cls) throws ClassNotFoundException {
        try {
            final InputStream is = cls.getResourceAsStream(resourceName);
            if (is == null) {
                return Collections.emptyList();
            }
            final DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final Document doc = db.parse(is);
            final Element channels = doc.getDocumentElement();
            return getChannels(channels, cls);
        } catch (final SAXException e) {
            throw new JeeManagementException(e);
        } catch (final IOException e) {
            throw new JeeManagementException(e);
        } catch (ParserConfigurationException e) {
            throw new JeeManagementException("DocumentBuilder could not be created", e);
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
    public static List<ChannelNotification> getChannels(final Element channels, final ClassLoader cls) throws ClassNotFoundException {
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
            throw new JeeManagementException(e);
        } catch (final IllegalAccessException e) {
            throw new JeeManagementException(e);
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
    public static List<ChannelNotification> getChannels(final Element channels) throws ClassNotFoundException {
        return getChannels(channels, Thread.currentThread().getContextClassLoader());
    }

    /**
    * Returns an iterator over the children of the given element with
    * the given tag name.
    *
    * @param element    The parent element
    * @param tagName    The name of the desired child
    * @return           An iterator of children or null if element is null
    */
    static final Iterator<Element> getChildrenByTagName(final Element element, final String tagName) {
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
     * <p></p>
     * 
     * @param node
     * @param desiredName
     * @return
     */
    static final boolean nodeNameEquals(final Node node, final String desiredName) {
        return desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName());
    }
}
