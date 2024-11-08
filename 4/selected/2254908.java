package mx4j.remote.rmi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.MarshalledObject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureClassLoader;

/**
 * Marshaller/Unmarshaller for RMI's MarshalledObjects. <br />
 * <p/>
 * This class implements the JMX Remote Specification, chapter 2. <br />
 * <strong>
 * Don't touch unless discussed on mx4j-devel@users.sourceforge.net, and unless
 * you know what you're doing (included who wrote this javadoc).
 * </strong>
 * <br />
 * IMPLEMENTATION NOTES: <br />
 * MarshalledObject.get() loads the object it contains by using the first user-defined classloader it can find
 * in the stack frames of the call. <br />
 * If the class cannot be found with that loader, then the RMI semantic is tried: first the thread context
 * classloader, then dynamic code download (if there is a security manager). <br />
 * We need that MarshalledObject.get() unmarshals using the context classloader and not the first user-defined
 * classloader, since this way we can implement correctly the JMX specification (see
 * {@link #unmarshal(MarshalledObject, ClassLoader, ClassLoader)}). <br />
 * Here we load the {@link Marshaller} class using {@link MarshallerClassLoader} that can only load the
 * {@link Marshaller} class.
 * It is important that {@link MarshallerClassLoader} cannot load from the classpath (the system classloader)
 * since that would break the JMX Remote Specification compliance. <br />
 * This URLClassLoader then becomes the first user-defined classloader in the stack frames, but it will fail
 * to load anything else, thus allowing MarshalledObject.get() to use the thread context classloader.
 * The stack trace will be something like:
 * <pre>
 * {@link Marshaller#unmarshal} [MarshallerClassLoader]
 *   ... reflection classes ... [Boot ClassLoader]
 *     {@link RMIMarshaller#unmarshal(MarshalledObject)} [System ClassLoader (normally)]
 * </pre>
 * <br />
 * Note that the classloader that loaded this class may be something totally different from URLClassLoader:
 * this is the case for the <a href="http://www.osgi.org">OSGi</a>. <br />
 * We just rely on the {@link ClassLoader#getResourceAsStream(java.lang.String)} semantic to load the
 * {@link Marshaller} class' bytes.
 *
 * @version $Revision: 1.13 $
 */
class RMIMarshaller {

    private static final Method unmarshal = getUnmarshalMethod();

    private static Method getUnmarshalMethod() {
        return (Method) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                String marshallerName = Marshaller.class.getName();
                InputStream stream = Marshaller.class.getResourceAsStream(marshallerName.substring(marshallerName.lastIndexOf('.') + 1) + ".class");
                if (stream == null) throw new Error("Could not load implementation class " + marshallerName);
                BufferedInputStream bis = new BufferedInputStream(stream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedOutputStream bos = new BufferedOutputStream(baos);
                try {
                    byte[] buffer = new byte[256];
                    int read = -1;
                    while ((read = bis.read(buffer)) >= 0) bos.write(buffer, 0, read);
                    bis.close();
                    bos.close();
                } catch (IOException x) {
                    throw new Error(x.toString());
                }
                byte[] classBytes = baos.toByteArray();
                MarshallerClassLoader loader = new MarshallerClassLoader(classBytes);
                try {
                    Class cls = loader.loadClass(marshallerName);
                    return cls.getMethod("unmarshal", new Class[] { MarshalledObject.class });
                } catch (ClassNotFoundException x) {
                    throw new Error(x.toString());
                } catch (NoSuchMethodException x) {
                    throw new Error(x.toString());
                }
            }
        });
    }

    /**
    * Returns a MarshalledObject obtained by marshalling the given object.
    */
    public static MarshalledObject marshal(Object object) throws IOException {
        if (object == null) return null;
        return new MarshalledObject(object);
    }

    /**
    * Returns the unmarshalled object obtained unmarshalling the given MarshalledObject,
    * using as context classloader first the given mbeanLoader, if not null, then with the given defaultLoader.
    */
    public static Object unmarshal(MarshalledObject object, final ClassLoader mbeanLoader, final ClassLoader defaultLoader) throws IOException {
        if (object == null) return null;
        if (mbeanLoader == null) return unmarshal(object, defaultLoader);
        ClassLoader loader = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                return new ExtendedClassLoader(mbeanLoader, defaultLoader);
            }
        });
        return unmarshal(object, loader);
    }

    private static Object unmarshal(MarshalledObject object, ClassLoader loader) throws IOException {
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        try {
            setContextClassLoader(loader);
            return unmarshal(object);
        } catch (IOException x) {
            throw x;
        } catch (ClassNotFoundException ignored) {
            throw new IOException(ignored.toString());
        } finally {
            setContextClassLoader(old);
        }
    }

    private static Object unmarshal(MarshalledObject marshalled) throws IOException, ClassNotFoundException {
        try {
            return unmarshal.invoke(null, new Object[] { marshalled });
        } catch (InvocationTargetException x) {
            Throwable t = x.getTargetException();
            if (t instanceof IOException) throw (IOException) t;
            if (t instanceof ClassNotFoundException) throw (ClassNotFoundException) t;
            throw new IOException(t.toString());
        } catch (Exception x) {
            throw new IOException(x.toString());
        }
    }

    private static void setContextClassLoader(final ClassLoader loader) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                Thread.currentThread().setContextClassLoader(loader);
                return null;
            }
        });
    }

    private static class MarshallerClassLoader extends SecureClassLoader {

        private byte[] bytes;

        /**
       * Parent classloader is null, thus we can load JDK classes, but for example not the JMX classes.
       * This will force usage of the context classloader for unmarshalling, and the context classloader
       * must be able to find the JMX classes. For example, when invoking setAttribute(), the marshalled
       * parameter is a javax.management.Attribute that may contain an object of a custom class.
       * To unmarshal the custom class (seen only by the context classloader), we must be able to unmarshal
       * first the Attribute class that so it must either be loadable by the context classloader or by
       * one of its ancestor classloaders.
       */
        private MarshallerClassLoader(byte[] classBytes) {
            super(null);
            this.bytes = classBytes;
        }

        /**
       * This method is overridden to define the {@link Marshaller} class and to delegate to the parent
       * further loading.
       * Classes from java.* packages (like java.lang.Object and java.rmi.MarshalledObject) are
       * referenced by {@link Marshaller} itself.
       * We don't load classes from javax.management.* packages, since we assume the context classloader
       * can load those classes by itself or by one of its ancestor classloaders.
       */
        public Class loadClass(final String name) throws ClassNotFoundException {
            if (name.startsWith(Marshaller.class.getName())) {
                try {
                    return defineClass(name, bytes, 0, bytes.length, MarshallerClassLoader.this.getClass().getProtectionDomain());
                } catch (ClassFormatError x) {
                    throw new ClassNotFoundException("Class Format Error", x);
                }
            } else {
                return super.loadClass(name);
            }
        }
    }

    /**
    * This is an implementation of the extended classloader as defined by the
    * JMX Remote Specification, chapter 2.
    */
    private static class ExtendedClassLoader extends SecureClassLoader {

        private final ClassLoader defaultLoader;

        private ExtendedClassLoader(ClassLoader mbeanLoader, ClassLoader defaultLoader) {
            super(mbeanLoader);
            this.defaultLoader = defaultLoader;
        }

        protected Class findClass(String name) throws ClassNotFoundException {
            return defaultLoader.loadClass(name);
        }

        protected URL findResource(String name) {
            return defaultLoader.getResource(name);
        }
    }
}
