package org.apache.commons.logging;

import junit.framework.TestCase;

/**
 * testcase to emulate container and application isolated from container
 * @author  baliuka
 * @version $Id$
 */
public class LoadTestCase extends TestCase {

    private static String LOG_PCKG[] = { "org.apache.commons.logging", "org.apache.commons.logging.impl" };

    /**
     * A custom classloader which "duplicates" logging classes available
     * in the parent classloader into itself.
     * <p>
     * When asked to load a class that is in one of the LOG_PCKG packages,
     * it loads the class itself (child-first). This class doesn't need
     * to be set up with a classpath, as it simply uses the same classpath
     * as the classloader that loaded it.
     */
    static class AppClassLoader extends ClassLoader {

        java.util.Map classes = new java.util.HashMap();

        AppClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class def(String name) throws ClassNotFoundException {
            Class result = (Class) classes.get(name);
            if (result != null) {
                return result;
            }
            try {
                ClassLoader cl = this.getClass().getClassLoader();
                String classFileName = name.replace('.', '/') + ".class";
                java.io.InputStream is = cl.getResourceAsStream(classFileName);
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                while (is.available() > 0) {
                    out.write(is.read());
                }
                byte data[] = out.toByteArray();
                result = super.defineClass(name, data, 0, data.length);
                classes.put(name, result);
                return result;
            } catch (java.io.IOException ioe) {
                throw new ClassNotFoundException(name + " caused by " + ioe.getMessage());
            }
        }

        public Class loadClass(String name) throws ClassNotFoundException {
            for (int i = 0; i < LOG_PCKG.length; i++) {
                if (name.startsWith(LOG_PCKG[i]) && name.indexOf("Exception") == -1) {
                    return def(name);
                }
            }
            return super.loadClass(name);
        }
    }

    /**
     * Call the static setAllowFlawedContext method on the specified class
     * (expected to be a UserClass loaded via a custom classloader), passing
     * it the specified state parameter.
     */
    private void setAllowFlawedContext(Class c, String state) throws Exception {
        Class[] params = { String.class };
        java.lang.reflect.Method m = c.getDeclaredMethod("setAllowFlawedContext", params);
        m.invoke(null, new Object[] { state });
    }

    /**
     * Test what happens when we play various classloader tricks like those
     * that happen in web and j2ee containers.
     * <p>
     * Note that this test assumes that commons-logging.jar and log4j.jar
     * are available via the system classpath.
     */
    public void testInContainer() throws Exception {
        Class cls = reload();
        Thread.currentThread().setContextClassLoader(cls.getClassLoader());
        execute(cls);
        cls = reload();
        Thread.currentThread().setContextClassLoader(null);
        execute(cls);
        cls = reload();
        Thread.currentThread().setContextClassLoader(null);
        try {
            setAllowFlawedContext(cls, "false");
            execute(cls);
            fail("Logging config succeeded when context classloader was null!");
        } catch (LogConfigurationException ex) {
        }
        cls = reload();
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        execute(cls);
        cls = reload();
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        try {
            setAllowFlawedContext(cls, "false");
            execute(cls);
            fail("Error: somehow downcast a Logger loaded via system classloader" + " to the Log interface loaded via a custom classloader");
        } catch (LogConfigurationException ex) {
        }
    }

    /**
     * Load class UserClass via a temporary classloader which is a child of
     * the classloader used to load this test class.
     */
    private Class reload() throws Exception {
        Class testObjCls = null;
        AppClassLoader appLoader = new AppClassLoader(this.getClass().getClassLoader());
        try {
            testObjCls = appLoader.loadClass(UserClass.class.getName());
        } catch (ClassNotFoundException cnfe) {
            throw cnfe;
        } catch (Throwable t) {
            t.printStackTrace();
            fail("AppClassLoader failed ");
        }
        assertTrue("app isolated", testObjCls.getClassLoader() == appLoader);
        return testObjCls;
    }

    private void execute(Class cls) throws Exception {
        cls.newInstance();
    }

    public static void main(String[] args) {
        String[] testCaseName = { LoadTestCase.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public void setUp() {
        origContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public void tearDown() {
        Thread.currentThread().setContextClassLoader(origContextClassLoader);
    }

    private ClassLoader origContextClassLoader;
}
