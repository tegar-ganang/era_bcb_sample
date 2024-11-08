package org.apache.commons.logging;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * testcase to emulate container and application isolated from container
 * @author  baliuka
 * @version $Id: LoadTest.java,v 1.5 2004/02/28 21:46:45 craigmcc Exp $
 */
public class LoadTest extends TestCase {

    private static String LOG_PCKG[] = { "org.apache.commons.logging", "org.apache.commons.logging.impl" };

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
                java.io.InputStream is = this.getClass().getClassLoader().getResourceAsStream(name.replace('.', '\\') + ".class");
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

    public void testInContainer() throws Exception {
        Class cls = reload();
        Thread.currentThread().setContextClassLoader(cls.getClassLoader());
        execute(cls);
        cls = reload();
        Thread.currentThread().setContextClassLoader(null);
        execute(cls);
        cls = reload();
        Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        try {
            execute(cls);
            fail("SystemClassLoader");
        } catch (LogConfigurationException ok) {
        }
        cls = reload();
        Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
        try {
            execute(cls);
            fail("ContainerClassLoader");
        } catch (LogConfigurationException ok) {
        }
    }

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

    /** Creates a new instance of LoadTest */
    public LoadTest(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        String[] testCaseName = { LoadTest.class.getName() };
        junit.textui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(LoadTest.class);
        return suite;
    }
}
