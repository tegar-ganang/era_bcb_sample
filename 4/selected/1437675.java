package net.sf.hibernate.audit;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Method;

/**
 * @author: Amar
 * Date: Dec 22, 2008
 */
class ClassAuditDefinition {

    private Class beanClass;

    private Map<String, String> propertyRuleMapping;

    private boolean ignoreClass = true;

    public void setClass(Class beanClass) {
        this.beanClass = beanClass;
        propertyRuleMapping = new HashMap<String, String>();
    }

    public void add(String property, String ruleId) {
        propertyRuleMapping.put(property, ruleId);
    }

    public void validateDefinition() throws IntrospectionException {
        Map<String, String> tempMapper = new HashMap<String, String>(propertyRuleMapping);
        BeanInfo beanInfo = Introspector.getBeanInfo(beanClass);
        PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
        Map<String, String> misSpelledProperties = new HashMap<String, String>();
        for (int i = 0; i < descriptors.length && tempMapper.size() > 0; i++) {
            PropertyDescriptor descriptor = descriptors[i];
            for (Map.Entry<String, String> entry : tempMapper.entrySet()) {
                String property = entry.getKey();
                Method readMethod = descriptor.getReadMethod();
                Method writeMethod = descriptor.getWriteMethod();
                if (readMethod != null && writeMethod != null) {
                    if (descriptor.getName().equals(property)) {
                        tempMapper.remove(property);
                        break;
                    } else if (descriptor.getName().equalsIgnoreCase(property)) {
                        misSpelledProperties.put(property, descriptor.getName());
                    }
                }
            }
        }
        if (tempMapper.size() > 0) {
            if (misSpelledProperties.size() > 0) {
                StringBuffer buffer = new StringBuffer("Misspelled Propertie(s) {");
                List<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(misSpelledProperties.entrySet());
                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<String, String> entry = entries.get(i);
                    String userDefined = entry.getKey();
                    String beanProperty = entry.getValue();
                    buffer.append("Given=").append(userDefined).append(";").append("Suggested=").append(beanProperty);
                    if (i < tempMapper.size() - 1) {
                        buffer.append(",");
                    }
                }
                buffer.append("} in Bean [").append(beanClass.getName()).append("] ");
                throw new IntrospectionException(buffer.toString());
            } else {
                List<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(tempMapper.entrySet());
                StringBuffer buffer = new StringBuffer("Could not find Propertie(s) {");
                for (int i = 0; i < entries.size(); i++) {
                    Map.Entry<String, String> ruleMapping = entries.get(i);
                    buffer.append(ruleMapping.getKey());
                    if (i < tempMapper.size() - 1) {
                        buffer.append(",");
                    }
                }
                buffer.append("} in Bean [").append(beanClass.getName()).append("] ");
                throw new IntrospectionException(buffer.toString());
            }
        }
    }

    public Class getBeanClass() {
        return beanClass;
    }

    public Map<String, String> getPropertyRuleMapping() {
        return propertyRuleMapping;
    }

    public void setIgnoreClass(boolean ignoreClass) {
        this.ignoreClass = ignoreClass;
    }

    public boolean isClassVisible() {
        return ignoreClass;
    }

    public String toString() {
        return "ClassAuditDefinition{" + "beanClass=" + beanClass + ", propertyRuleMapping=" + propertyRuleMapping + ", ignoreClass=" + ignoreClass + '}';
    }
}
