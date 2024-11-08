package net.sf.mustang.xbean;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import org.dom4j.Attribute;
import org.dom4j.Element;
import net.sf.mustang.K;
import net.sf.mustang.Mustang;
import net.sf.mustang.bean.BeanDescriptor;
import net.sf.mustang.conf.ConfTool;
import net.sf.mustang.log.KLog;
import net.sf.mustang.xml.XMLTool;

public class XDescriptor extends BeanDescriptor {

    private static KLog log = Mustang.getLog(XDescriptor.class);

    public static final String VALUE = "value";

    public static final String VALUE_LIST = "value_list";

    public static final String BEAN = "bean";

    public static final String BEAN_LIST = "bean_list";

    public static final String XBEAN = "xbean";

    public static final String XBEAN_LIST = "xbean_list";

    private static final String[] EXCLUDE_ATTRIBUTES = { "key", "type", "xdescriptor", "xbean", "xbean-class", "parent-key", "spread-method" };

    private String refName;

    private DriverInfo driver;

    private XPropertyDescriptor[] kPropertyDescriptors;

    private Method[] methods;

    public String getRefName() {
        return refName;
    }

    public void setRefName(String refName) {
        this.refName = refName;
    }

    public DriverInfo getDriver() {
        return driver;
    }

    public void setDriver(DriverInfo driver) {
        this.driver = driver;
    }

    public Method[] getMethods() {
        return methods;
    }

    public void setMethods(Method[] methods) {
        this.methods = methods;
    }

    public Method[] getMethods(String methodName) {
        Method[] retVal = null;
        Vector v = new Vector();
        for (int i = 0; methods != null && i < methods.length; i++) if (Arrays.asList(methods[i].getNames()).contains(methodName)) v.addElement(methods[i]);
        if (v.size() > 0) {
            retVal = new Method[v.size()];
            v.copyInto(retVal);
        }
        return retVal;
    }

    public XDescriptor(String file) throws Exception {
        super(null);
        if (log.isInfo()) log.info("parsing " + file);
        Element element = null;
        element = XMLTool.readXML(file).getRootElement();
        driver = elementToDriver(element.element("driver"));
        methods = elementsToMethods(element.elements("method"));
        kPropertyDescriptors = elementsToAttributeDescriptors(element.elements("property"));
        Element inElement;
        XDescriptor inXDescriptor;
        Method[] tempMethod, tempMethodCopy;
        XPropertyDescriptor[] tempXPropertyDesc, tempXPropertyDescCopy;
        String inXDescriptorName;
        List<Element> inc = element.elements("include");
        int incNum = inc.size();
        if (log.isInfo()) log.info("including " + incNum + " xdescriptors");
        for (int i = 0; i < incNum; i++) {
            inElement = inc.get(i);
            inXDescriptorName = inElement.attributeValue("xdescriptor");
            if (inXDescriptorName.length() == 0) {
                if (log.isWarning()) log.warning("empty <include> tag in: " + file);
                continue;
            }
            if (log.isInfo()) log.info("including " + inXDescriptorName + " in: " + file);
            try {
                inXDescriptor = XDescriptorManager.getInstance().getXDescriptor(inXDescriptorName);
                if (inXDescriptor.getDriver() != null) {
                    if (log.isInfo()) log.info("ignoring global <driver> " + inXDescriptor.getDriver().getClassName() + " in " + inXDescriptorName);
                }
                tempMethod = inXDescriptor.getMethods();
                if (tempMethod != null) {
                    if (methods == null) {
                        methods = tempMethod;
                    } else {
                        tempMethodCopy = new Method[methods.length + tempMethod.length];
                        System.arraycopy(methods, 0, tempMethodCopy, 0, methods.length);
                        System.arraycopy(tempMethod, 0, tempMethodCopy, methods.length, tempMethod.length);
                        methods = tempMethodCopy;
                    }
                }
                tempXPropertyDesc = (XPropertyDescriptor[]) inXDescriptor.getPropertyDescriptors();
                if (tempXPropertyDesc != null) {
                    if (kPropertyDescriptors == null) {
                        kPropertyDescriptors = tempXPropertyDesc;
                    } else {
                        tempXPropertyDescCopy = new XPropertyDescriptor[kPropertyDescriptors.length + tempXPropertyDesc.length];
                        System.arraycopy(kPropertyDescriptors, 0, tempXPropertyDescCopy, 0, kPropertyDescriptors.length);
                        System.arraycopy(tempXPropertyDesc, 0, tempXPropertyDescCopy, kPropertyDescriptors.length, tempXPropertyDesc.length);
                        kPropertyDescriptors = tempXPropertyDescCopy;
                    }
                }
            } catch (Exception e) {
                log.error("Error including " + inXDescriptorName + " in: " + file + "\n");
                throw e;
            }
            if (log.isInfo()) log.info("included " + inXDescriptorName + " in: " + file);
        }
        super.setPropertyDescriptors(kPropertyDescriptors);
    }

    private XPropertyDescriptor[] elementsToAttributeDescriptors(List<Element> elements) throws Exception {
        XPropertyDescriptor[] retVal = null;
        if (elements != null) {
            if (log.isInfo()) log.info("reading " + elements.size() + " <property>");
            retVal = new XPropertyDescriptor[elements.size()];
            String type, xDescriptor, xBean;
            Attribute attribute;
            for (int i = 0; i < elements.size(); i++) {
                retVal[i] = new XPropertyDescriptor();
                type = elements.get(i).attributeValue("type");
                if (type == null || type.length() == 0) type = VALUE;
                retVal[i].setType(type);
                if (type.equals(XBEAN) || type.equals(XBEAN_LIST)) {
                    xDescriptor = elements.get(i).attributeValue("xdescriptor");
                    xBean = elements.get(i).attributeValue("xbean");
                    if ((xDescriptor == null || xDescriptor.length() == 0) && (xBean == null || xBean.length() == 0)) throw new Exception("if 'type' is 'xbean' or 'xbean_list' 'xdescriptor' or 'xbean' is required on xdescriptor: " + refName);
                    retVal[i].setChildXBean(xBean);
                    retVal[i].setChildXDescriptor(xDescriptor);
                    retVal[i].setChildXBeanClass(elements.get(i).attributeValue("xbean-class"));
                    retVal[i].setParentKey(elements.get(i).attributeValue("parent-key"));
                    retVal[i].setSpreadMethod(K.YES.equals(elements.get(i).attributeValue("spread-method")));
                }
                retVal[i].setKey(elements.get(i).attributeValue("key"));
                retVal[i].setDriver(elementToDriver(elements.get(i).element("driver")));
                retVal[i].setMethods(elementsToMethods(elements.get(i).elements("method")));
                if (retVal[i].getKey() == null || retVal[i].getKey().length() == 0) throw new Exception("<property key> is required");
                for (int j = 0; j < elements.get(i).attributes().size(); j++) {
                    attribute = (Attribute) elements.get(i).attributes().get(j);
                    if (!Arrays.asList(EXCLUDE_ATTRIBUTES).contains(attribute.getName())) retVal[i].setAttribute(attribute.getName(), ConfTool.processParameter(attribute.getValue()));
                }
            }
        }
        return retVal;
    }

    private Method[] elementsToMethods(List elements) throws Exception {
        Method[] retVal = null;
        if (elements != null) {
            if (log.isInfo()) log.info("reading " + elements.size() + " <method>");
            retVal = new Method[elements.size()];
            Element element = null;
            for (int i = 0; i < elements.size(); i++) {
                element = (Element) elements.get(i);
                retVal[i] = new Method();
                if (element.attributeValue("name") != null) retVal[i].setNames(element.attributeValue("name").split(K.COMMA));
                retVal[i].setDriver(elementToDriver(element.element("driver")));
                retVal[i].setRules(elementsToRules(element.elements("rule")));
                retVal[i].setActions(elementsToActions(element.elements("action")));
                if (retVal[i].getNames() == null || retVal[i].getNames().length == 0) throw new Exception("<method name> is required");
            }
        }
        return retVal;
    }

    private Action[] elementsToActions(List elements) throws Exception {
        Action[] retVal = null;
        if (elements != null) {
            if (log.isInfo()) log.info("reading " + elements.size() + " <action>");
            retVal = new Action[elements.size()];
            Element element = null;
            for (int i = 0; i < elements.size(); i++) {
                element = (Element) elements.get(i);
                retVal[i] = new Action();
                retVal[i].setValue(element.attributeValue("value"));
                retVal[i].setReader(K.YES.equals(element.attributeValue("reader")));
                retVal[i].setForeach(K.YES.equals(element.attributeValue("foreach")));
                retVal[i].setNoMessages(K.YES.equals(element.attributeValue("no-messages")));
                if (!K.EMPTY.equals(element.getTextTrim())) retVal[i].setScript(element.getText());
                if (retVal[i].getValue() == null && retVal[i].getScript() == null) throw new Exception("<action value> or 'script' is required");
            }
        }
        return retVal;
    }

    private Rule[] elementsToRules(List elements) throws Exception {
        Rule[] retVal = null;
        if (elements != null) {
            if (log.isInfo()) log.info("reading " + elements.size() + " <rule>");
            retVal = new Rule[elements.size()];
            Element element = null;
            for (int i = 0; i < elements.size(); i++) {
                element = (Element) elements.get(i);
                retVal[i] = new Rule();
                retVal[i].setForeach(K.YES.equals(element.attributeValue("foreach")));
                retVal[i].setMessage(element.attributeValue("message"));
                if (!K.EMPTY.equals(element.getTextTrim())) retVal[i].setScript(element.getText());
                if (retVal[i].getScript() == null) throw new Exception("<rule> 'script' is required");
                if (retVal[i].getMessage() == null) throw new Exception("<rule message> is required");
            }
        }
        return retVal;
    }

    private DriverInfo elementToDriver(Element element) throws Exception {
        DriverInfo retVal = null;
        if (element != null) {
            retVal = new DriverInfo();
            if (log.isInfo()) log.info("reading <driver>");
            retVal.setClassName(element.attributeValue("class-name"));
            retVal.setChannel(element.attributeValue("channel"));
            if (retVal.getClassName() == null || retVal.getClassName().length() == 0) throw new Exception("<driver class-name> is required");
            if (retVal.getChannel() == null || retVal.getChannel().length() == 0) if (log.isWarning()) log.warning("<driver channel> not defined");
        }
        return retVal;
    }

    public XDescriptor(XPropertyDescriptor[] kPropertyDescriptors) {
        super(kPropertyDescriptors);
        this.kPropertyDescriptors = kPropertyDescriptors;
    }

    public String getType(String key) {
        return kPropertyDescriptors[getIndex(key)].getType();
    }

    public String getChildXDescriptor(String key) {
        return kPropertyDescriptors[getIndex(key)].getChildXDescriptor();
    }

    public String getChildXBeanClass(String key) {
        return kPropertyDescriptors[getIndex(key)].getChildXBeanClass();
    }

    public String getChildXBean(String key) {
        return kPropertyDescriptors[getIndex(key)].getChildXBean();
    }

    public String getParentKey(String key) {
        return kPropertyDescriptors[getIndex(key)].getParentKey();
    }

    public boolean getSpreadMethod(String key) {
        return kPropertyDescriptors[getIndex(key)].getSpreadMethod();
    }

    public DriverInfo getDriver(String key) {
        return kPropertyDescriptors[getIndex(key)].getDriver();
    }

    public String getType(int index) {
        return kPropertyDescriptors[index].getType();
    }

    public String getChildXDescriptor(int index) {
        return kPropertyDescriptors[index].getChildXDescriptor();
    }

    public String getChildXBeanClass(int index) {
        return kPropertyDescriptors[index].getChildXBeanClass();
    }

    public String getChildXBean(int index) {
        return kPropertyDescriptors[index].getChildXBean();
    }

    public String getParentKey(int index) {
        return kPropertyDescriptors[index].getParentKey();
    }

    public boolean getSpreadMethod(int index) {
        return kPropertyDescriptors[index].getSpreadMethod();
    }

    public DriverInfo getDriver(int index) {
        return kPropertyDescriptors[index].getDriver();
    }

    public Method[] getMethods(String methodName, String key) {
        return kPropertyDescriptors[getIndex(key)].getMethods(methodName);
    }

    public Method[] getMethods(String methodName, int index) {
        return kPropertyDescriptors[index].getMethods(methodName);
    }

    public String getAttribute(int index, String name) {
        return kPropertyDescriptors[index].getAttribute(name);
    }

    public String getAttribute(String key, String name) {
        return kPropertyDescriptors[getIndex(key)].getAttribute(name);
    }
}
