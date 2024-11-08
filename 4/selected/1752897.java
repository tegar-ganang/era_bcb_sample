package net.sf.cglib.transform.impl;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author Juozas
 *
 */
public class TestInterceptFieldsSubclass extends TestInterceptFields {

    private boolean readTest = false;

    private boolean writeTest = false;

    public TestInterceptFieldsSubclass() {
        super();
    }

    public TestInterceptFieldsSubclass(String name) {
        super(name);
    }

    public void testSubClass() {
        super.test();
        assertTrue("super class read field", readTest);
        assertTrue("super class write field", readTest);
    }

    public Object readObject(Object _this, String name, Object oldValue) {
        if (name.equals("field")) {
            readTest = true;
        }
        return super.readObject(_this, name, oldValue);
    }

    public Object writeObject(Object _this, String name, Object oldValue, Object newValue) {
        if (name.equals("field")) {
            writeTest = true;
        }
        return super.writeObject(_this, name, oldValue, newValue);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() throws Exception {
        return new TestSuite(new TestInterceptFieldsSubclass().transform());
    }
}
