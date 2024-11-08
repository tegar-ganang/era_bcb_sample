package net.sf.fixx;

import junit.framework.TestCase;
import java.util.*;
import java.util.jar.*;
import java.io.*;
import java.net.*;
import nu.xom.*;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.*;
import org.apache.commons.logging.*;

/**
 * @author Michael R. Abato
 */
public class FixtureCase extends TestCase {

    protected static final Log TRACE = LogFactory.getLog(FixtureCase.class);

    private BeanFactoryPostProcessor configurer = new PropertyPlaceholderConfigurer() {

        protected String resolvePlaceholder(String placeholder, Properties props) {
            Object f = files.get(placeholder);
            if (f == null) {
                f = fixtures.get(placeholder);
            }
            if (f != null) try {
                TRACE.info("FixtureCase configurer resolving file: " + placeholder + " ==> " + f);
                File file = f instanceof File ? (File) f : ((DirectoryFixture) f).getDir();
                return file.toURL().toExternalForm();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            String prop = System.getProperty(placeholder);
            if (prop != null) {
                TRACE.info("FixtureCase configurer resolving system property: " + placeholder + " ==> " + prop);
                return prop;
            }
            return props.getProperty(placeholder);
        }
    };

    private DirectoryFixture fixture = new DirectoryFixture(getClass());

    private Map fixtures = new HashMap();

    private Map files = new HashMap();

    private Map data = new HashMap();

    private Map beans = new HashMap();

    private boolean cleanup = true;

    public DirectoryFixture getFixture() {
        return fixture;
    }

    public DirectoryFixture getFixture(Object id) {
        return (DirectoryFixture) fixtures.get(id);
    }

    public File getFile(Object id) {
        return (File) files.get(id);
    }

    public Element getData(Object id) {
        return (Element) data.get(id);
    }

    public BeanFactory getBeans(Object id) {
        return (BeanFactory) beans.get(id);
    }

    public String getFileContent(Object id) throws IOException {
        String content = null;
        File f = (File) files.get(id);
        if (f != null) {
            content = DirectoryFixture.read(f);
        }
        return content;
    }

    public byte[] getFileData(Object id) throws IOException {
        byte[] data = null;
        File f = (File) files.get(id);
        if (f != null) {
            data = DirectoryFixture.readBytes(f);
        }
        return data;
    }

    protected void tearDown() throws Exception {
        if (cleanup) {
            fixture.doTeardown();
        }
    }

    protected void setUp() throws Exception {
        Document configuration = getConfiguration();
        Element fixtureElement = configuration.getRootElement();
        String isolation = fixtureElement.getAttributeValue("isolation");
        if ("unique".equals(isolation)) {
            String basename = fixtureElement.getAttributeValue("basename");
            if (basename == null || basename.length() < 3) {
                basename = System.getProperty("user.name");
                if (basename == null || basename.length() < 3) {
                    basename = "tmp";
                }
            }
            fixture = fixture.createUniqueDirectory(basename);
        } else if ("clean".equals(isolation)) {
            fixture.doTeardown();
        } else if ("reuse".equals(isolation)) {
        }
        createFiles(fixture, fixtureElement);
        fixtures.put("~base", fixture);
        Elements datasets = fixtureElement.getChildElements("data");
        for (int i = 0; i < datasets.size(); ++i) {
            Element dataElement = datasets.get(i);
            data.put(dataElement.getAttributeValue("id"), dataElement);
        }
        Elements beansElements = fixtureElement.getChildElements("beans");
        for (int i = 0; i < beansElements.size(); ++i) {
            Element beansElement = beansElements.get(i);
            Element root = (Element) beansElement.copy();
            root.removeAttribute(root.getAttribute("id"));
            Document beansDoc = new Document(root);
            String publicid = "-//SPRING//DTD BEAN//EN";
            String systemid = "http://www.springframework.org/dtd/spring-beans.dtd";
            DocType doctype = new DocType(beansDoc.getRootElement().getLocalName(), publicid, systemid);
            beansDoc.setDocType(doctype);
            byte[] beansBytes = beansDoc.toXML().getBytes();
            Resource r = new InputStreamResource(new ByteArrayInputStream(beansBytes), "");
            XmlBeanFactory bf = new XmlBeanFactory(r);
            configurer.postProcessBeanFactory(bf);
            beans.put(beansElement.getAttributeValue("id"), bf);
        }
        Elements jarElements = fixtureElement.getChildElements("jar");
        for (int i = 0; i < jarElements.size(); ++i) {
            Element jarElement = jarElements.get(i);
            Element manifestElement = jarElement.getChildElements("manifest").get(0);
            String manifestText = manifestElement.getValue().trim() + "\n";
            InputStream mis = new ByteArrayInputStream(manifestText.getBytes());
            Manifest manifest = new Manifest(mis);
            File jarFile = createFile(fixture, jarElement);
            JarOutputStream jout = new JarOutputStream(new FileOutputStream(jarFile), manifest);
            Elements entryElements = jarElement.getChildElements("file");
            for (int j = 0; j < entryElements.size(); ++j) {
                Element entryElement = entryElements.get(j);
                File entryFile = createFile(fixture, entryElement);
                JarEntry entry = new JarEntry(entryFile.getName());
                jout.putNextEntry(entry);
                jout.write(DirectoryFixture.readBytes(entryFile));
                jout.closeEntry();
            }
            jout.close();
        }
        if ("false".equals(fixtureElement.getAttributeValue("cleanup"))) {
            cleanup = false;
        }
    }

    protected void createFiles(DirectoryFixture dirFixture, Element dirElement) throws IOException {
        Elements files = dirElement.getChildElements("file");
        for (int i = 0; i < files.size(); ++i) {
            createFile(dirFixture, files.get(i));
        }
        Elements dirs = dirElement.getChildElements("dir");
        for (int i = 0; i < dirs.size(); ++i) {
            Element subElement = dirs.get(i);
            String subName = subElement.getAttributeValue("name");
            String unique = subElement.getAttributeValue("unique");
            DirectoryFixture sub = unique != null && "true".equals(unique) ? dirFixture.createUniqueDirectory(subName) : dirFixture.createDirectory(subName);
            String id = subElement.getAttributeValue("id");
            if (id != null) {
                fixtures.put(id, sub);
            }
            createFiles(sub, subElement);
        }
    }

    protected File createFile(DirectoryFixture fixture, Element fileElement) throws IOException {
        String fileName = fileElement.getAttributeValue("name");
        String fileType = fileElement.getAttributeValue("type");
        String unique = fileElement.getAttributeValue("unique");
        String suffix = fileElement.getAttributeValue("suffix");
        String contents = fileElement.getValue().trim();
        File file;
        if ("random".equals(fileType)) {
            String sizeStr = fileElement.getAttributeValue("size");
            long size = sizeStr != null ? new Long(sizeStr).longValue() : 1024L * 1024;
            file = fixture.createFile(fileName, size);
        } else if ("binary".equals(fileType)) {
            byte[] data = Base64.decode(contents);
            file = unique != null && "true".equals(unique) ? fixture.createUniqueFile(fileName, suffix, data) : fixture.createFile(fileName, data);
        } else if ("xml".equals(fileType)) {
            Document doc = new Document((Element) fileElement.getChildElements().get(0).copy());
            String publicid = fileElement.getAttributeValue("public");
            String systemid = fileElement.getAttributeValue("system");
            DocType doctype = new DocType(doc.getRootElement().getLocalName(), publicid, systemid);
            doc.setDocType(doctype);
            file = fixture.createFile(fileName, doc.toXML());
        } else if ("beans".equals(fileType)) {
            Document doc = new Document((Element) fileElement.getChildElements().get(0).copy());
            String publicid = fileElement.getAttributeValue("public");
            if (publicid == null) {
                publicid = "-//SPRING//DTD BEAN//EN";
            }
            String systemid = fileElement.getAttributeValue("system");
            if (systemid == null) {
                systemid = "http://www.springframework.org/dtd/spring-beans.dtd";
            }
            DocType doctype = new DocType(doc.getRootElement().getLocalName(), publicid, systemid);
            doc.setDocType(doctype);
            file = fixture.createFile(fileName, doc.toXML());
            XmlBeanFactory bf = new XmlBeanFactory(new FileSystemResource(file));
            configurer.postProcessBeanFactory(bf);
            beans.put(fileElement.getAttributeValue("id"), bf);
        } else if ("rsrc".equals(fileType)) {
            String rsrc = fileElement.getAttributeValue("rsrc");
            InputStream in = getClass().getResourceAsStream(rsrc);
            file = fixture.createFile(fileName, in);
        } else {
            file = unique != null && "true".equals(unique) ? fixture.createUniqueFile(fileName, suffix, contents) : fixture.createFile(fileName, contents);
        }
        String id = fileElement.getAttributeValue("id");
        files.put(id != null ? id : fileName, file);
        return file;
    }

    /**
   * Return the configuration of this test as a XOM Document.
   *
   * @return
   */
    protected Document getConfiguration() {
        Builder builder = new Builder();
        String configurationSystemId = getConfigurationSystemId();
        try {
            return builder.build(configurationSystemId);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load test configuration " + configurationSystemId, e);
        } catch (ParsingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to parse test configuration " + configurationSystemId, e);
        }
    }

    /**
   * Return the system-id (absolute url) of the configuration file. This default
   * implementation lookups up the name of this class ".xml" as a local resource
   * (i.e.: classpath). Subclasses can change this strategy for locating the
   * configuration file.
   *
   * @return
   */
    protected String getConfigurationSystemId() {
        String name = getTestName() + ".xml";
        URL url = getClass().getResource(name);
        if (url == null) {
            throw new RuntimeException("Failed to locate configuration resource '" + name + "'");
        }
        return url.toExternalForm();
    }

    /**
   * Return the name of the test for this class. This is used to build up the
   * systemid for the configuration file. The default implemenation uses just
   * the same name as the class itself - this can be subclassed as desired to
   * change the name of the configuration file to be used.
   *
   * @return
   */
    protected String getTestName() {
        String className = getClass().getName();
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
