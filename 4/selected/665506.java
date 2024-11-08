package com.sitechasia.webx.core.utils.populator;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import javax.servlet.ServletRequest;

/**
 * Bean到Bean的注值器的实现
 *
 * @author Zal
 * @author todd
 * @version 1.2 , 2008/5/7
 * @since JDK1.5
 */
public class BeanPopulator extends BasePopulator {

    /**
	 * 进行Bean到Bean的注值
	 */
    protected boolean doPopulate(Object source, Object target, Map<String, String> propertiesMapping, String[] ignoreProperties, Object... params) {
        if (source instanceof Collection || target instanceof Collection || source instanceof Map || target instanceof Map || source instanceof ResultSet || target instanceof ResultSet || source instanceof ServletRequest || target instanceof ServletRequest) {
            return false;
        }
        PropertyDescriptor[] sourceDescriptors = null;
        PropertyDescriptor[] targetDescriptors = null;
        try {
            sourceDescriptors = Introspector.getBeanInfo(source.getClass()).getPropertyDescriptors();
            targetDescriptors = Introspector.getBeanInfo(target.getClass()).getPropertyDescriptors();
        } catch (IntrospectionException ie) {
            logger.debug("Failed on getting bean's properties", ie);
            return false;
        }
        for (PropertyDescriptor targetDescriptor : targetDescriptors) {
            if (targetDescriptor.getName().equals("class") || !doProcess(targetDescriptor.getName(), ignoreProperties)) {
                continue;
            }
            PropertyDescriptor sourceDescriptor = getSourceDescriptor(propertiesMapping, targetDescriptor, sourceDescriptors);
            if (sourceDescriptor == null) {
                continue;
            }
            try {
                Method readMethod = sourceDescriptor.getReadMethod();
                Method writeMethod = targetDescriptor.getWriteMethod();
                if (readMethod == null || writeMethod == null) {
                    continue;
                }
                Object sourceValue = readMethod.invoke(source, new Object[0]);
                Object convertedValue = getConverter().convertValue(sourceValue, targetDescriptor.getPropertyType(), params);
                writeMethod.invoke(target, new Object[] { convertedValue });
            } catch (Exception e) {
                logger.debug("Exception", e);
            }
        }
        return true;
    }

    /**
	 * 获得源对象中的属性
	 *
	 * @param propertiesMapping
	 * @param targetDescriptor
	 * @param sourceDescriptors
	 * @return
	 */
    private PropertyDescriptor getSourceDescriptor(Map<String, String> propertiesMapping, PropertyDescriptor targetDescriptor, PropertyDescriptor[] sourceDescriptors) {
        String targetPropertyName = targetDescriptor.getName();
        String tempSourcePropertyName = null;
        if (propertiesMapping != null && propertiesMapping.containsKey(targetPropertyName)) {
            tempSourcePropertyName = propertiesMapping.get(targetPropertyName);
        }
        PropertyDescriptor finalPropertyDescriptor = null;
        for (PropertyDescriptor sourceDescriptor : sourceDescriptors) {
            String iterSourcePropertyName = sourceDescriptor.getName();
            if (tempSourcePropertyName != null && tempSourcePropertyName.equals(iterSourcePropertyName)) {
                finalPropertyDescriptor = sourceDescriptor;
                break;
            } else if (targetPropertyName.equals(iterSourcePropertyName)) {
                finalPropertyDescriptor = sourceDescriptor;
                if (tempSourcePropertyName == null) {
                    break;
                }
            }
        }
        return finalPropertyDescriptor;
    }
}
