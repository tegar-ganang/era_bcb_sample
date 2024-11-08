package org.jsorb.rmi;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

/**
 * @author Brad Koehn
 */
public class MarshallerTest extends TestCase {

    Marshaller marshaller;

    Writer writer;

    /**
	 * Constructor for MarshallerTest.
	 * @param arg0 arg
	 */
    public MarshallerTest(String arg0) {
        super(arg0);
    }

    public void testMarshalInteger() {
        try {
            marshaller.marshal(new Integer(2), writer);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testMarshalBoolean() {
        try {
            marshaller.marshal(Boolean.TRUE, writer);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testMarshalBadObject() {
        try {
            marshaller.marshal(Thread.currentThread(), writer);
            fail("should not have marshalled");
        } catch (Exception e) {
        }
    }

    public void testMarshalObjectArray() throws Exception {
        Object[] array = new Object[4];
        array[0] = "Hello, world";
        array[1] = new Double(2.445);
        array[2] = new String[1];
        array[3] = new int[2];
        marshaller.marshal(array, writer);
    }

    public void testMarshalList() {
        List list = new ArrayList();
        list.add("hello");
        list.add("world");
        marshaller.marshal(list, writer);
    }

    public void testMarshalStrings() {
        String string = "hello \r\n world";
        marshaller.marshal(string, writer);
    }

    public void testMarshal2DArray() {
        Object[][] twoD = new Object[3][3];
        int c = 0;
        for (int i = 0; i < twoD.length; i++) {
            for (int j = 0; j < twoD[0].length; j++) {
                twoD[i][j] = new Integer(c++);
            }
        }
        marshaller.marshal(twoD, writer);
    }

    public void testMarshalCDataClosure() {
        marshaller.marshal("Hello ]]> World!", writer);
        marshaller.marshal("Hello World!]]>", writer);
        marshaller.marshal("]]>Hello World!", writer);
        marshaller.marshal("]]>", writer);
    }

    /**
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception {
        super.setUp();
        marshaller = new XmlMarshaller();
        writer = new PrintWriter(System.err);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        writer.write("\r\n\r\n");
        writer.flush();
    }
}
