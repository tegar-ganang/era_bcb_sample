package net.sf.persistant.metadata;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.lang.reflect.Method;

/**
 * <p>
 * <code>PropertyMetadataImpl</code> is the default implementation of the {@link PropertyMetadata} interface.
 * </p>
 */
public class PropertyMetadataImpl implements PropertyMetadata {

    private final transient Logger LOGGER = LoggerFactory.getLogger(PropertyMetadataImpl.class);

    /**
     * <p>
     * The property name.
     * </p>
     */
    private final String m_name;

    /**
     * <p>
     * Flag indicating whether the property is nullable.
     * </p>
     */
    private final boolean m_nullable;

    /**
     * <p>
     * The class of the property.
     * </p>
     */
    private final Class<?> m_propertyClass;

    /**
     * <p>
     * The property accessor method. 
     * </p>
     */
    private final Method m_readMethod;

    /**
     * <p>
     * The property mutator method. 
     * </p>
     */
    private final Method m_writeMethod;

    /**
     * <p>
     * Construct a {@link PropertyMetadata} instance.
     * </p>
     *
     * @param name the property name.
     * @param propertyClass the property class.
     * @param nullable flag indicating whether the property is nullable.
     * @param readMethod the property accessor method.
     * @param writeMethod the property mutator method.
     */
    public PropertyMetadataImpl(final String name, final Class<?> propertyClass, final boolean nullable, final Method readMethod, final Method writeMethod) {
        super();
        assert null != name : "The [name] argument cannot be null.";
        assert null != propertyClass : "The [propertyClass] argument cannot be null.";
        assert null != readMethod : "The [readMethod] argument cannot be null.";
        m_name = name;
        m_nullable = nullable;
        m_propertyClass = propertyClass;
        m_readMethod = readMethod;
        m_writeMethod = writeMethod;
    }

    public String getName() {
        return m_name;
    }

    public boolean isNullable() {
        return m_nullable;
    }

    public Class<?> getPropertyClass() {
        return m_propertyClass;
    }

    public Method getReadMethod() {
        return m_readMethod;
    }

    public Method getWriteMethod() {
        return m_writeMethod;
    }
}
