package com.pentagaia.tb.start.impl.kernel0951;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import com.pentagaia.tb.start.api.IPdsClassLoaderExtension;
import com.pentagaia.tb.start.impl.util.StreamUtil;

/**
 * A class loader extension (root level).
 * 
 * This class is compatible to kernel 0.9.5.1
 */
public final class ClassLoaderExtension0951 implements IPdsClassLoaderExtension {

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getGlobalClassSet()
     */
    public String[] getGlobalClassSet() {
        return new String[] { "com.sleepycat.**" };
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#loadClass(java.lang.String)
     */
    public byte[] loadClass(final String className) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#loadClassFirst(java.lang.String)
     */
    public byte[] loadClassFirst(final String className) {
        if (className.equals("com.sun.sgs.impl.kernel.AppKernelAppContext")) {
            final URL url = Thread.currentThread().getContextClassLoader().getResource("com/sun/sgs/impl/kernel/AppKernelAppContext.0.9.5.1.class.bin");
            if (url != null) {
                try {
                    return StreamUtil.read(url.openStream());
                } catch (IOException e) {
                }
            }
            throw new IllegalStateException("Unable to load AppKernelAppContext.0.9.5.1.class.bin");
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#loadClassExternal(java.lang.String)
     */
    public Class<?> loadClassExternal(final String className) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#refactorClass(java.lang.String, byte[])
     */
    public byte[] refactorClass(final String className, final byte[] source) {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getResource(java.lang.String)
     */
    public URL getResource(String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getResourceFirst(java.lang.String)
     */
    public URL getResourceFirst(String name) {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see com.pentagaia.tb.start.api.IPdsClassLoaderExtension#getResources(java.lang.String)
     */
    public Enumeration<URL> getResources(String name) {
        return null;
    }
}
