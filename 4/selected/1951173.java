package net.sf.mmm.util.pojo.descriptor.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import net.sf.mmm.util.pojo.descriptor.api.PojoDescriptor;
import net.sf.mmm.util.pojo.descriptor.api.PojoPropertyDescriptor;
import net.sf.mmm.util.pojo.descriptor.api.accessor.PojoPropertyAccessorNonArg;
import net.sf.mmm.util.pojo.descriptor.api.accessor.PojoPropertyAccessorNonArgMode;
import net.sf.mmm.util.pojo.descriptor.api.accessor.PojoPropertyAccessorOneArg;
import net.sf.mmm.util.pojo.descriptor.api.accessor.PojoPropertyAccessorOneArgMode;

/**
 * This is the abstract test-case for implementations of
 * {@link net.sf.mmm.util.pojo.descriptor.api.PojoDescriptorBuilder}.
 * 
 * @author Joerg Hohwiller (hohwille at users.sourceforge.net)
 */
public abstract class AbstractPojoDescriptorBuilderTest {

    /**
   * This method checks read/write accessors to the property
   * <code>propertyName</code> of the <code>pojoDescriptor</code> according
   * to the given <code>readType</code> and <code>writeType</code>.
   * 
   * @param pojoDescriptor is the descriptor.
   * @param propertyName is the name of the property to check.
   * @param readType is the expected read-type or <code>null</code> if NOT to
   *        check.
   * @param writeType is the expected write-type or <code>null</code> if NOT
   *        to check.
   */
    protected void checkProperty(PojoDescriptor<?> pojoDescriptor, String propertyName, Class<?> readType, Class<?> writeType) {
        PojoPropertyDescriptor propertyDescriptor = pojoDescriptor.getPropertyDescriptor(propertyName);
        assertNotNull(propertyDescriptor);
        assertEquals(propertyName, propertyDescriptor.getName());
        PojoPropertyAccessorNonArg getAccessor = propertyDescriptor.getAccessor(PojoPropertyAccessorNonArgMode.GET);
        if (readType == null) {
            assertNull(getAccessor);
        } else {
            assertNotNull(getAccessor);
            assertEquals(propertyName, getAccessor.getName());
            assertEquals(readType, getAccessor.getPropertyClass());
            assertEquals(readType, getAccessor.getPropertyType().getRetrievalClass());
            assertEquals(getAccessor.getPropertyType(), getAccessor.getReturnType());
            assertSame(getAccessor.getPropertyClass(), getAccessor.getReturnClass());
        }
        PojoPropertyAccessorOneArg setAccessor = propertyDescriptor.getAccessor(PojoPropertyAccessorOneArgMode.SET);
        if (writeType == null) {
            assertNull(setAccessor);
        } else {
            assertNotNull(setAccessor);
            assertEquals(propertyName, setAccessor.getName());
            assertEquals(writeType, setAccessor.getPropertyClass());
            assertEquals(writeType, setAccessor.getPropertyType().getAssignmentClass());
        }
    }
}
