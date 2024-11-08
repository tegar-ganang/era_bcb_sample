package org.centricframework.core;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.beanutils.BeanUtils;
import org.mvel2.MVEL;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;

public class ComponentLoader extends DefaultHandler {

    private static SAXParserFactory spf = SAXParserFactory.newInstance();

    private static Configuration configuration;

    SAXParser sp;

    private AbstractComponent root;

    private AbstractComponent current;

    private boolean processCurrentNode = true;

    int skippedChildren = 0;

    static {
        configuration = new Configuration();
        File templateRoot = new File(Configuration.class.getResource("/").getFile());
        try {
            configuration.setDirectoryForTemplateLoading(templateRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
        configuration.setObjectWrapper(new DefaultObjectWrapper());
    }

    public ComponentLoader(AbstractComponent component) {
        try {
            sp = spf.newSAXParser();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        root = component;
    }

    public static void load(AbstractComponent component) throws Exception {
        if (component.isBasicComponent()) return;
        String fileName = component.getClass().getName();
        fileName = fileName.replace('.', '/') + ".ftl";
        Template tpl = configuration.getTemplate(fileName);
        StringWriter swriter = new StringWriter();
        tpl.process(Execution.getCurrent().getModel(), swriter);
        Reader sreader = new StringReader(swriter.toString());
        InputSource is = new InputSource(sreader);
        if (is == null) return;
        ComponentLoader handler = new ComponentLoader(component);
        handler.sp.parse(is, handler);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (!processCurrentNode) {
            skippedChildren++;
            return;
        }
        String ifa = attributes.getValue("if");
        if (ifa != null) {
            try {
                processCurrentNode = (Boolean) MVEL.eval(ifa, Execution.getCurrent().getModel());
                if (!processCurrentNode) return;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (current == null) {
            current = root;
            setAttributes(root, attributes);
            return;
        } else {
            AbstractComponent cmp = null;
            String classname = null;
            if (qName.equals("component")) {
                classname = attributes.getValue("class");
            } else {
                classname = "org.centricframework.ui." + qName.substring(0, 1).toUpperCase() + qName.substring(1);
            }
            try {
                cmp = ComponentLoader.newComponent(classname);
                setAttributes(cmp, attributes);
            } catch (Exception e) {
                e.printStackTrace();
            }
            cmp.setParent(current);
            current.addChild(cmp);
            if (cmp.getId() != null) {
                root.getDeclaredDescendants().put(cmp.getId(), cmp);
            }
            current = cmp;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (skippedChildren >= 0) skippedChildren--;
        if (processCurrentNode && current != null) current = current.getParent();
        if (skippedChildren == -1 && !processCurrentNode) {
            processCurrentNode = true;
        }
    }

    private static AbstractComponent newComponent(String classname) {
        try {
            Class<? extends AbstractComponent> cls = Class.forName(classname).asSubclass(AbstractComponent.class);
            return cls.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setAttributes(AbstractComponent cmp, Attributes attributes) {
        try {
            if (attributes == null || attributes.getLength() == 0) return;
            int count = attributes.getLength();
            for (int i = 0; i < count; i++) {
                String key = attributes.getQName(i);
                if (key.equals("class")) continue;
                String value = attributes.getValue(i).trim();
                if ((value.startsWith("@{") || value.startsWith("@out{") || value.startsWith("@in{")) && value.endsWith("}")) {
                    String both = extract(value, "@{");
                    String in = extract(value, "@in{");
                    String out = extract(value, "@in{");
                    if (both != null) {
                        in = both;
                        out = both;
                    }
                    if (in != null) cmp.getBoundInAttributes().put(key, in);
                    if (out != null) cmp.getBoundOutAttributes().put(key, out);
                } else {
                    BeanUtils.setProperty(cmp, key, value);
                }
            }
            cmp.afterPropertiesSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String extract(String value, String prefix) {
        int pos = value.indexOf(prefix);
        if (pos == -1) return null;
        return value.substring(pos + prefix.length(), value.indexOf("}", pos));
    }
}
