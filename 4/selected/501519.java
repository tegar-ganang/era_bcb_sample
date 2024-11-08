package com.fastaop.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import com.fastaop.advice.IAdwiceWithPointcutProvider;
import com.fastaop.instrument.FastAOPTransformer;
import com.fastaop.instrument.TransformationResult;

/**
 * class loader for testing.
 * 
 * @author Daniel Wiese
 * 
 */
public class AOPClassLoader extends ClassLoader {

    private final ClassLoader cl;

    private final FastAOPTransformer transformer;

    /**
	 * Constructor.
	 * 
	 * @param className
	 *            the class name (to change)
	 * @param ca
	 *            the class visitor
	 * @param the
	 *            aspects to test
	 */
    public AOPClassLoader(IAdwiceWithPointcutProvider... aspects) {
        super();
        this.cl = Thread.currentThread().getContextClassLoader();
        this.transformer = new FastAOPTransformer(aspects);
    }

    /**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        TransformationResult result = new TransformationResult();
        byte[] bytecode = transformClass(className, result);
        if (!this.transformer.isNonAdvisableClassName(className) && result.isWasTransformed()) {
            return super.defineClass(className, bytecode, 0, bytecode.length);
        }
        return cl.loadClass(className);
    }

    private byte[] transformClass(String className, TransformationResult result) {
        try {
            InputStream resourceAsStream = getClass().getResourceAsStream("/" + className.replace('.', '/') + ".class");
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] byteArray = new byte[1024];
            int readBytes = -1;
            while ((readBytes = resourceAsStream.read(byteArray, 0, byteArray.length)) != -1) out.write(byteArray, 0, readBytes);
            resourceAsStream.close();
            return this.transformer.transformClass(out.toByteArray(), className, this.cl, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
