package com.phloc.types.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.annotations.PresentForCodeCoverage;
import com.phloc.commons.annotations.VisibleForTesting;
import com.phloc.commons.equals.EqualsUtils;
import com.phloc.commons.hash.HashCodeGenerator;
import com.phloc.commons.lang.ClassHelper;
import com.phloc.commons.priviledged.AccessControllerHelper;
import com.phloc.commons.priviledged.PrivilegedActionAccessibleObjectSetAccessible;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.ToStringGenerator;

/**
 * This cache saves much time, since the introspection takes a lot of time when
 * creating new {@link PropertyDescriptor} objects.
 * 
 * @author philip
 */
@NotThreadSafe
public final class PropertyDescriptorCache {

    /**
   * Key for a single {@link PropertyDescriptor}.
   * 
   * @author philip
   */
    @VisibleForTesting
    static final class PDState {

        private final Class<?> m_aClass;

        private final String m_sField;

        private final String m_sReadMethodName;

        private final String m_sWriteMethodName;

        PDState(@Nonnull final Class<?> aClass, @Nonnull @Nonempty final String sField, @Nullable final String sReadMethodName, @Nullable final String sWriteMethodName) {
            if (aClass == null) throw new NullPointerException("class");
            if (StringHelper.hasNoText(sField)) throw new IllegalArgumentException("field");
            m_aClass = aClass;
            m_sField = sField;
            m_sReadMethodName = sReadMethodName;
            m_sWriteMethodName = sWriteMethodName;
        }

        @Nonnull
        public PropertyDescriptor createPropertyDescriptor() {
            PropertyDescriptor aPD;
            try {
                if (m_sReadMethodName == null && m_sWriteMethodName == null) aPD = new PropertyDescriptor(m_sField, m_aClass); else aPD = new PropertyDescriptor(m_sField, m_aClass, m_sReadMethodName, m_sWriteMethodName);
            } catch (final IntrospectionException ex) {
                throw new IllegalArgumentException("Failed to create PropertyDescriptor for " + this, ex);
            }
            final Method aReadMethod = aPD.getReadMethod();
            if (aReadMethod != null && !ClassHelper.isPublicClass(aReadMethod.getDeclaringClass())) AccessControllerHelper.run(new PrivilegedActionAccessibleObjectSetAccessible(aReadMethod));
            final Method aWriteMethod = aPD.getWriteMethod();
            if (aWriteMethod != null && !ClassHelper.isPublicClass(aWriteMethod.getDeclaringClass())) AccessControllerHelper.run(new PrivilegedActionAccessibleObjectSetAccessible(aWriteMethod));
            return aPD;
        }

        @Override
        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof PDState)) return false;
            final PDState rhs = (PDState) o;
            return m_aClass.equals(rhs.m_aClass) && m_sField.equals(rhs.m_sField) && EqualsUtils.equals(m_sReadMethodName, rhs.m_sReadMethodName) && EqualsUtils.equals(m_sWriteMethodName, rhs.m_sWriteMethodName);
        }

        @Override
        public int hashCode() {
            return new HashCodeGenerator(this).append(m_aClass).append(m_sField).append(m_sReadMethodName).append(m_sWriteMethodName).getHashCode();
        }

        @Override
        public String toString() {
            return new ToStringGenerator(this).append("class", m_aClass).append("field", m_sField).appendIfNotNull("readMethodName", m_sReadMethodName).appendIfNotNull("writeMethodName", m_sWriteMethodName).toString();
        }
    }

    private static final Map<PDState, PropertyDescriptor> s_aPDCache = new WeakHashMap<PDState, PropertyDescriptor>();

    @PresentForCodeCoverage
    @SuppressWarnings("unused")
    private static final PropertyDescriptorCache s_aInstance = new PropertyDescriptorCache();

    private PropertyDescriptorCache() {
    }

    @Nonnull
    public static PropertyDescriptor getPropertyDescriptor(@Nonnull final Class<?> aClass, @Nonnull @Nonempty final String sField) {
        return getPropertyDescriptor(aClass, sField, null, null);
    }

    @Nonnull
    public static PropertyDescriptor getPropertyDescriptor(@Nonnull final Class<?> aClass, @Nonnull @Nonempty final String sField, @Nullable final String sReadMethodName) {
        return getPropertyDescriptor(aClass, sField, sReadMethodName, null);
    }

    @Nonnull
    public static PropertyDescriptor getPropertyDescriptor(@Nonnull final Class<?> aClass, @Nonnull @Nonempty final String sField, @Nullable final String sReadMethodName, @Nullable final String sWriteMethodName) {
        final PDState aPDState = new PDState(aClass, sField, sReadMethodName, sWriteMethodName);
        PropertyDescriptor aPD = s_aPDCache.get(aPDState);
        if (aPD == null) {
            aPD = aPDState.createPropertyDescriptor();
            s_aPDCache.put(aPDState, aPD);
        }
        return aPD;
    }
}
