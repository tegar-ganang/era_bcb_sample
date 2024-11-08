package net.sf.persistant.metadata;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>
 * <code>EntityMetadataBuilder</code> is a builder object which is used to construct {@link EntityMetadata} instances.
 * </p>
 */
public class EntityMetadataBuilder {

    /**
     * <p>
     * The entity class. 
     * </p>
     */
    private final Class<?> m_entityClass;

    /**
     * <p>
     * The entity name. 
     * </p>
     */
    private final String m_entityName;

    /**
     * <p>
     * The identifier class. 
     * </p>
     */
    private final Class<?> m_identifierClass;

    /**
     * <p>
     * Property metadata. 
     * </p>
     */
    private final Set<PropertyMetadata> m_propertyMetadata = new HashSet<PropertyMetadata>();

    /**
     * <p>
     * Construct a {@link EntityMetadataBuilder} instance.
     * </p>
     *
     * @param entityName the entity name.
     * @param entityClass the entity class.
     * @param identifierClass the identifier class.
     */
    public EntityMetadataBuilder(final String entityName, final Class<?> entityClass, final Class<?> identifierClass) {
        super();
        assert null != entityClass : "The [entityClass] argument cannot be null.";
        assert null != entityName : "The [entityName] argument cannot be null.";
        assert null != identifierClass : "The [identifierClass] argument cannot be null.";
        m_entityClass = entityClass;
        m_entityName = entityName;
        m_identifierClass = identifierClass;
    }

    /**
     * <p>
     * Add a property to the entity metadata.
     * </p>
     *
     * @param name the property name.
     * @param clazz the property class.
     * @param nullable flag indicating whether the property is nullable.
     * @param readMethod the property accessor method.
     * @param writeMethod the property mutator method.
     * @return {@link EntityMetadataBuilder} this instance.
     */
    public EntityMetadataBuilder addProperty(final String name, final Class<?> clazz, final boolean nullable, final Method readMethod, final Method writeMethod) {
        m_propertyMetadata.add(new PropertyMetadataImpl(name, clazz, nullable, readMethod, writeMethod));
        return this;
    }

    /**
     * <p>
     * Build an {@link EntityMetadata} instance from the information which has been stored in this builder instance.
     * </p>
     *
     * @return {@link EntityMetadata} instance.
     */
    public final EntityMetadata toEntityMetadata() {
        return createEntityMetadata(m_entityName, m_entityClass, m_identifierClass, m_propertyMetadata);
    }

    /**
     * <p>
     * Create an {@link EntityMetadata} instance from given entity information.
     * </p>
     * 
     * @param entityName the entity name.
     * @param entityClass the entity class.
     * @param identifierClass the identifier class.
     * @param propertyMetadata the property metadata.
     * @return {@link EntityMetadata} instance.
     */
    protected EntityMetadata createEntityMetadata(final String entityName, final Class<?> entityClass, final Class<?> identifierClass, final Set<PropertyMetadata> propertyMetadata) {
        return new EntityMetadataImpl(entityName, entityClass, identifierClass, propertyMetadata);
    }
}
