package de.schlund.pfixcore.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.springframework.util.AntPathMatcher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author mleidig@schlund.de
 *
 */
public class ModuleDescriptor {

    private static final Logger LOG = Logger.getLogger(ModuleDescriptor.class);

    private static final String DEPRECATED_NS_MODULE_DESCRIPTOR = "http://pustefix.sourceforge.net/moduledescriptor200702";

    private static final String NS_MODULE_DESCRIPTOR = "http://www.pustefix-framework.org/2008/namespace/module-descriptor";

    private URL url;

    private String name;

    private boolean contentEditable;

    private Map<String, Set<String>> moduleToResourcePaths = new HashMap<String, Set<String>>();

    private Map<String, Set<String>> moduleToResourcePathPatterns = new HashMap<String, Set<String>>();

    private Dictionary<String, String> moduleOverrideFilterAttributes = new Hashtable<String, String>();

    private Dictionary<String, String> defaultSearchFilterAttributes = new Hashtable<String, String>();

    private boolean defaultSearchable;

    private int defaultSearchPriority = 10;

    private List<String> staticPaths = new ArrayList<String>();

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    public ModuleDescriptor(URL url, String name) {
        this.url = url;
        this.name = name;
    }

    public URL getURL() {
        return url;
    }

    public String getName() {
        return name;
    }

    public boolean isContentEditable() {
        return contentEditable;
    }

    public void setContentEditable(boolean contentEditable) {
        this.contentEditable = contentEditable;
    }

    /**
     * Adds resource/module overridden by this module.
     * 
     * @param module - Module name
     * @param resourcePath - Resource path (either the full path or an ant-style pattern)
     */
    public void addOverridedResource(String module, String resourcePath) {
        if (resourcePath.contains("?") || resourcePath.contains("*")) {
            Set<String> resList = moduleToResourcePathPatterns.get(module);
            if (resList == null) {
                resList = new HashSet<String>();
                moduleToResourcePathPatterns.put(module, resList);
            }
            resList.add(resourcePath);
        } else {
            Set<String> resList = moduleToResourcePaths.get(module);
            if (resList == null) {
                resList = new HashSet<String>();
                moduleToResourcePaths.put(module, resList);
            }
            resList.add(resourcePath);
        }
    }

    public boolean overridesResource(String module, String path) {
        Set<String> overrides = moduleToResourcePaths.get(module);
        if (overrides != null) {
            if (overrides.contains(path)) return true;
        }
        overrides = moduleToResourcePathPatterns.get(module);
        if (overrides != null) {
            for (String pattern : overrides) {
                if (antPathMatcher.match(pattern, path)) return true;
            }
        }
        return false;
    }

    public void addModuleOverrideFilterAttribute(String name, String value) {
        moduleOverrideFilterAttributes.put(name, value);
    }

    public void addDefaultSearchFilterAttribute(String name, String value) {
        defaultSearchFilterAttributes.put(name, value);
    }

    public void setDefaultSearchable(boolean defaultSearchable) {
        this.defaultSearchable = defaultSearchable;
    }

    public boolean isDefaultSearchable() {
        return defaultSearchable;
    }

    public void setDefaultSearchPriority(int defaultSearchPriority) {
        this.defaultSearchPriority = defaultSearchPriority;
    }

    public int getDefaultSearchPriority() {
        return defaultSearchPriority;
    }

    public Dictionary<String, String> getModuleOverrideFilterAttributes() {
        return moduleOverrideFilterAttributes;
    }

    public Dictionary<String, String> getDefaultSearchFilterAttributes() {
        return defaultSearchFilterAttributes;
    }

    public void addStaticPath(String staticPath) {
        staticPaths.add(staticPath);
    }

    public List<String> getStaticPaths() {
        return staticPaths;
    }

    @Override
    public String toString() {
        return "MODULE " + name;
    }

    public static ModuleDescriptor read(URL url) throws Exception {
        ModuleDescriptor moduleInfo;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(url.openStream());
        Element root = document.getDocumentElement();
        if (root.getLocalName().equals("module-descriptor")) {
            if (DEPRECATED_NS_MODULE_DESCRIPTOR.equals(root.getNamespaceURI()) || DEPRECATED_NS_MODULE_DESCRIPTOR.equals(root.getAttribute("xmlns"))) {
                String msg = "[DEPRECATED] Module descriptor file '" + url.toString() + "' uses deprecated namespace '" + DEPRECATED_NS_MODULE_DESCRIPTOR + "'. It should be replaced by '" + NS_MODULE_DESCRIPTOR + "'.";
                LOG.warn(msg);
            }
            Element nameElem = getSingleChildElement(root, "module-name", true);
            String name = nameElem.getTextContent().trim();
            if (name.equals("")) throw new Exception("Text content of element 'module-name' must not be empty!");
            moduleInfo = new ModuleDescriptor(url, name);
            Element editElem = getSingleChildElement(root, "content-editable", false);
            if (editElem != null) {
                boolean editable = Boolean.valueOf(editElem.getTextContent());
                moduleInfo.setContentEditable(editable);
            }
            Element searchElem = getSingleChildElement(root, "default-search", false);
            if (searchElem != null) {
                moduleInfo.setDefaultSearchable(true);
                String priority = searchElem.getAttribute("priority").trim();
                if (priority.length() > 0) {
                    moduleInfo.setDefaultSearchPriority(Integer.parseInt(priority));
                }
                List<Element> filterAttrElems = getChildElements(searchElem, "filter-attribute");
                for (Element filterAttrElem : filterAttrElems) {
                    String filterAttrName = filterAttrElem.getAttribute("name");
                    if (filterAttrName.equals("")) throw new Exception("Element 'filter-attribute' requires 'name' attribute!");
                    String filterAttrValue = filterAttrElem.getAttribute("value");
                    if (filterAttrValue.equals("")) throw new Exception("Element 'filter-attribute' requires 'value' attribute!");
                    moduleInfo.addDefaultSearchFilterAttribute(filterAttrName, filterAttrValue);
                }
            }
            Element overElem = getSingleChildElement(root, "override-modules", false);
            if (overElem != null) {
                List<Element> filterAttrElems = getChildElements(overElem, "filter-attribute");
                for (Element filterAttrElem : filterAttrElems) {
                    String filterAttrName = filterAttrElem.getAttribute("name");
                    if (filterAttrName.equals("")) throw new Exception("Element 'filter-attribute' requires 'name' attribute!");
                    String filterAttrValue = filterAttrElem.getAttribute("value");
                    if (filterAttrValue.equals("")) throw new Exception("Element 'filter-attribute' requires 'value' attribute!");
                    moduleInfo.addModuleOverrideFilterAttribute(filterAttrName, filterAttrValue);
                }
                List<Element> modElems = getChildElements(overElem, "module");
                for (Element modElem : modElems) {
                    String modName = modElem.getAttribute("name").trim();
                    if (modName.equals("")) throw new Exception("Element 'module' requires 'name' attribute!");
                    List<Element> resElems = getChildElements(modElem, "resource");
                    for (Element resElem : resElems) {
                        String resPath = resElem.getAttribute("path").trim();
                        if (resPath.equals("")) throw new Exception("Element 'resource' requires 'path' attribute!");
                        moduleInfo.addOverridedResource(modName, resPath);
                    }
                }
            }
            Element staticElem = getSingleChildElement(root, "static", false);
            if (staticElem != null) {
                List<Element> pathElems = getChildElements(staticElem, "path");
                for (Element pathElem : pathElems) {
                    String path = pathElem.getTextContent().trim();
                    if (!path.equals("")) {
                        if (!path.startsWith("/")) path = "/" + path;
                        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                        if (path.equals("") || path.equals("/PUSTEFIX-INF")) path = "/"; else if (path.startsWith("/PUSTEFIX-INF")) path = path.substring(13);
                        moduleInfo.addStaticPath(path);
                    }
                }
            }
        } else throw new Exception("Illegal module descriptor");
        return moduleInfo;
    }

    private static Element getSingleChildElement(Element parent, String localName, boolean mandatory) throws Exception {
        Element elem = null;
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(localName)) {
                if (elem != null) throw new Exception("Multiple '" + localName + "' child elements aren't allowed.");
                elem = (Element) node;
            }
        }
        if (mandatory && elem == null) throw new Exception("Missing '" + localName + "' child element.");
        return elem;
    }

    private static List<Element> getChildElements(Element parent, String localName) {
        List<Element> elems = new ArrayList<Element>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getLocalName().equals(localName)) {
                elems.add((Element) node);
            }
        }
        return elems;
    }
}
