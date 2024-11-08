package net.ontopia.topicmaps.entry;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.ontopia.utils.OntopiaRuntimeException;
import net.ontopia.utils.PropertyUtils;
import net.ontopia.utils.StringUtils;
import net.ontopia.utils.URIUtils;
import net.ontopia.xml.DefaultXMLReaderFactory;
import net.ontopia.xml.Slf4jSaxErrorHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PUBLIC: Reads store configuration parameters from an XML
 * file. The config source is able to handle multiple sources at the
 * same time by using an instance of the {@link TopicMapRepositoryIF}
 * class.<p>
 *
 * The class understands XML documents using the following DTD:</p>
 *
 * <pre>
 * &lt;!ELEMENT repository (source+)              >
 * &lt;!ELEMENT source     (param*)               >
 * &lt;!ATTLIST source      id    ID    #IMPLIED  >
 * &lt;!ATTLIST source      class CDATA #REQUIRED >
 * &lt;!ELEMENT param       EMPTY                 >
 * &lt;!ATTLIST param       name  CDATA #REQUIRED
 *                          value CDATA #REQUIRED >
 * </pre>
 *
 * <b>Example:</b></p>
 *
 * <pre>
 * &lt;repository>
 *   &lt;!-- source that references all .xtm files in a directory -->
 *   &lt;source class="net.ontopia.topicmaps.xml.XTMPathTopicMapSource">
 *     &lt;param name="path" value="/ontopia/topicmaps"/>
 *     &lt;param name="suffix" value=".xtm"/>
 *   &lt;/source>
 *   &lt;!-- source that references a topic map in a relational database -->
 *   &lt;source class="net.ontopia.topicmaps.impl.rdbms.RDBMSSingleTopicMapSource">
 *     &lt;param name="topicMapId" value="5001"/>
 *     &lt;param name="title" value="My RDBMS Topic Map"/>
 *     &lt;param name="referenceId" value="mytm"/>
 *     &lt;param name="propertyFile" value="${CWD}/db.postgresql.props"/>
 *   &lt;/source>
 * &lt;/repository>
 * </pre>
 *
 * This example makes XMLConfigSource use a TopicMapRepositoryIF that
 * contains two other topic maps sources, namely instances of the two
 * specifed in the class attributes of the source elements. Note that
 * the classes must have empty constructors for them to be used with
 * this class.</p>
 *
 * The two sources would locate all XTM files with the .xtm extension
 * in the /ontopia/topicmaps directory in which the config file is
 * located.</p>
 *
 * The <code>param</code> element is used to set bean properties on
 * the source instance. This way the config source can be configured.</p>
 *
 * <p><b>Environment variables:</b></p>
 *
 * <p>XMLConfigSource is able to replace environment variables in the
 * param value attribute. The only environment variable available at
 * this time is ${CWD}, which contains the full path of the directory
 * in which the config file is located.</p>
 *
 * <p>NOTE: Topic map sources with supportsCreate set to true will get
 * ids assigned automatically. This is done so that the sources can be
 * referred to from the outside.</p>
 *
 */
public class XMLConfigSource {

    static Logger log = LoggerFactory.getLogger(XMLConfigSource.class.getName());

    /**
   * INTERNAL: Don't call constructor directly. Instead used static
   * factory methods.
   */
    private XMLConfigSource() {
    }

    /**
   * PUBLIC: Get the topic map repository that is created by loading
   * the 'tm-sources.xml' configuration file from the classpath.<p>
   *
   * @since 3.0
   */
    public static TopicMapRepositoryIF getRepositoryFromClassPath() {
        return getRepositoryFromClassPath("tm-sources.xml");
    }

    /**
   * PUBLIC: Get the topic map repository that is created by loading
   * the named resource from the classpath.<p>
   *
   * @since 3.0
   */
    public static TopicMapRepositoryIF getRepositoryFromClassPath(String resourceName) {
        return getRepositoryFromClassPath(resourceName, null);
    }

    /**
   * INTERNAL:
   */
    public static TopicMapRepositoryIF getRepositoryFromClassPath(Map<String, String> environ) {
        return getRepositoryFromClassPath("tm-sources.xml", environ);
    }

    /**
   * INTERNAL:
   */
    public static TopicMapRepositoryIF getRepositoryFromClassPath(String resourceName, Map<String, String> environ) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(resourceName);
        if (url == null) throw new OntopiaRuntimeException("Could not find resource '" + resourceName + "' on CLASSPATH.");
        if (environ == null) environ = new HashMap<String, String>(1);
        if (url.getProtocol().equals("file")) {
            String file = url.getFile();
            environ.put("CWD", file.substring(0, file.lastIndexOf('/')));
        } else environ.put("CWD", ".");
        try {
            return createRepository(readSources(new InputSource(url.openStream()), environ));
        } catch (IOException e) {
            throw new OntopiaRuntimeException(e);
        }
    }

    /**
   * PUBLIC: Gets the topic map repository that is created by reading
   * the configuration file.<p>
   */
    public static TopicMapRepositoryIF getRepository(String config_file) {
        return createRepository(readSources(config_file));
    }

    /**
   * INTERNAL: Gets the topic map repository that is created by
   * reading the configuration file with the given environment.<p>
   */
    public static TopicMapRepositoryIF getRepository(String config_file, Map<String, String> environ) {
        return createRepository(readSources(config_file, environ));
    }

    /**
   * PUBLIC: Gets the topic map repository that is created by reading
   * the configuration file from the reader.<p>
   *
   * @since 3.0
   */
    public static TopicMapRepositoryIF getRepository(Reader config_file) {
        return createRepository(readSources(new InputSource(config_file), null));
    }

    /**
   * INTERNAL: Gets the topic map repository that is created by
   * reading the configuration file from the reader with the given
   * environment.<p>
   *
   * @since 3.0
   */
    public static TopicMapRepositoryIF getRepository(Reader config_file, Map<String, String> environ) {
        return createRepository(readSources(new InputSource(config_file), environ));
    }

    private static TopicMapSourceManager createRepository(Collection<TopicMapSourceIF> sources) {
        int counter = 1;
        Iterator<TopicMapSourceIF> iter = sources.iterator();
        while (iter.hasNext()) {
            TopicMapSourceIF source = iter.next();
            if (source.getId() == null && source.supportsCreate()) {
                String newId = source.getClass().getName() + "-" + (counter++);
                source.setId(newId);
                if (source.getTitle() == null) source.setTitle(newId);
            }
        }
        return new TopicMapSourceManager(sources);
    }

    /**
   * INTERNAL: Returns a collection containing the topic map sources
   * created by reading the configuration file.
   */
    public static List<TopicMapSourceIF> readSources(String config_file) {
        return readSources(config_file, new HashMap<String, String>(1));
    }

    /**
   * INTERNAL: Returns a collection containing the topic map sources
   * created by reading the configuration file.
   */
    public static List<TopicMapSourceIF> readSources(String config_file, Map<String, String> environ) {
        if (environ == null) environ = new HashMap<String, String>(1);
        if (!environ.containsKey("CWD")) {
            File file = new File(config_file);
            if (!file.exists()) throw new OntopiaRuntimeException("Config file '" + config_file + "' does not exist.");
            environ.put("CWD", file.getParent());
        }
        String url = URIUtils.getURI(config_file).getAddress();
        return readSources(new InputSource(url), environ);
    }

    private static List<TopicMapSourceIF> readSources(InputSource inp_source, Map<String, String> environ) {
        ConfigHandler handler = new ConfigHandler(environ);
        try {
            XMLReader parser = new DefaultXMLReaderFactory().createXMLReader();
            parser.setContentHandler(handler);
            parser.setErrorHandler(new Slf4jSaxErrorHandler(log));
            parser.parse(inp_source);
        } catch (SAXParseException e) {
            String msg = "" + e.getSystemId() + ":" + e.getLineNumber() + ":" + e.getColumnNumber() + ": " + e.getMessage();
            throw new OntopiaRuntimeException(msg, e);
        } catch (Exception e) {
            throw new OntopiaRuntimeException(e);
        }
        return handler.sources;
    }

    static class ConfigHandler extends DefaultHandler {

        Map<String, String> environ;

        List<TopicMapSourceIF> sources = new ArrayList<TopicMapSourceIF>();

        TopicMapSourceIF source;

        ConfigHandler(Map<String, String> environ) {
            this.environ = environ;
        }

        public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
            if (qName.equals("source")) {
                source = null;
                try {
                    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                    source = (TopicMapSourceIF) Class.forName(atts.getValue("class"), true, classLoader).newInstance();
                    String id = atts.getValue("id");
                    if (id != null) source.setId(id);
                    sources.add(source);
                } catch (ClassNotFoundException e) {
                    log.error("Cannot find class " + e.getMessage());
                } catch (Exception e) {
                    log.error("Exception: " + e.getClass().getName() + ": " + e.getMessage());
                }
            } else if (qName.equals("param") && source != null) {
                String param_name = atts.getValue("name");
                String param_value = atts.getValue("value");
                Iterator<String> iter = environ.keySet().iterator();
                while (iter.hasNext()) {
                    String environ_key = iter.next();
                    param_value = StringUtils.replace(param_value, "${" + environ_key + "}", environ.get(environ_key));
                }
                try {
                    BeanInfo bean_info = Introspector.getBeanInfo(source.getClass());
                    PropertyDescriptor[] props = bean_info.getPropertyDescriptors();
                    boolean found_property = false;
                    for (int i = 0; i < props.length; i++) {
                        if (props[i].getName().equals(param_name)) {
                            Method setter = props[i].getWriteMethod();
                            if (props[i].getPropertyType().equals(String.class)) {
                                setter.invoke(source, new Object[] { param_value });
                                found_property = true;
                                break;
                            } else if (props[i].getPropertyType().equals(boolean.class)) {
                                setter.invoke(source, new Object[] { new Boolean(PropertyUtils.isTrue(param_value)) });
                                found_property = true;
                                break;
                            }
                        }
                    }
                    if (!found_property) throw new SAXException("Cannot find property '" + param_name + "' " + "on source " + source);
                } catch (IntrospectionException e) {
                    throw new SAXException(e);
                } catch (InvocationTargetException e) {
                    throw new SAXException(e);
                } catch (IllegalAccessException e) {
                    throw new SAXException(e);
                }
            }
        }

        public void endElement(String uri, String name, String qName) throws SAXException {
            if (qName.equals("source")) {
                source = null;
            }
        }
    }
}
