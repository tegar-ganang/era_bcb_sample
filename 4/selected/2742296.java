package org.jnf.dwr.convert;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;
import org.directwebremoting.convert.BeanConverter;
import org.directwebremoting.extend.MarshallException;
import org.directwebremoting.extend.Property;
import org.directwebremoting.impl.PropertyDescriptorProperty;
import org.eclipse.persistence.exceptions.ValidationException;
import org.eclipse.persistence.indirection.IndirectContainer;

/**
 * BeanConverter that works with EclipseLink.
 * 
 * @author Daniel Martins [daniel at destaquenet dot com]
 * @author Pablo Krause, modified to work with EclipseLink
 * 
 */
public class EclipseLinkConverter extends BeanConverter {

    @Override
    public Map<String, Property> getPropertyMapFromObject(Object example, boolean readRequired, boolean writeRequired) throws MarshallException {
        Class<?> clazz = example.getClass();
        try {
            BeanInfo info = Introspector.getBeanInfo(clazz);
            Map<String, Property> properties = new HashMap<String, Property>();
            for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
                String name = descriptor.getName();
                if ("class".equals(name)) {
                    continue;
                }
                if (!isAllowedByIncludeExcludeRules(name)) {
                    continue;
                }
                if (readRequired && descriptor.getReadMethod() == null) {
                    continue;
                }
                if (writeRequired && descriptor.getWriteMethod() == null) {
                    continue;
                }
                properties.put(name, new EclipseLinkPropertyDescriptorProperty(descriptor));
            }
            return properties;
        } catch (Exception ex) {
            throw new MarshallException(clazz, ex);
        }
    }
}

/**
 * A {@link Property} that catches EclipseLink exceptions. This is useful for EclipseLink where lazy
 * loading results in an exception and you are unable to detect and prevent this.
 * 
 * @author Daniel Martins [daniel at destaquenet dot com]
 * @author Pablo Krause, modified to work with EclipseLink
 */
class EclipseLinkPropertyDescriptorProperty extends PropertyDescriptorProperty {

    /**
	 * Simple constructor
	 * 
	 * @param descriptor
	 *            The PropertyDescriptor that we are proxying to
	 */
    public EclipseLinkPropertyDescriptorProperty(PropertyDescriptor descriptor) {
        super(descriptor);
    }

    @Override
    public Object getValue(Object bean) throws MarshallException {
        try {
            Object value = super.getValue(bean);
            if (value instanceof IndirectContainer) {
                if (!((IndirectContainer) value).isInstantiated()) {
                    throw new MarshallException(bean.getClass(), ValidationException.instantiatingValueholderWithNullSession());
                }
            }
            return value;
        } catch (MarshallException ex) {
            if (ex.getCause() instanceof ValidationException) {
                if (((ValidationException) ex.getCause()).getErrorCode() == ValidationException.INSTANTIATING_VALUEHOLDER_WITH_NULL_SESSION) {
                    return null;
                }
            }
            throw ex;
        } catch (Exception ex) {
            throw new MarshallException(bean.getClass(), ex);
        }
    }
}
