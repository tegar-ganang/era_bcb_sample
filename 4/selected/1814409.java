package org.photovault.image;

import java.io.IOException;
import java.io.StringReader;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

/**
 Unit tests for {@link ChannelMapOperation} and associated classes
 */
public class Test_ChannelMapOperation extends TestCase {

    /** Creates a new instance of Test_ChannelMapOperation */
    public Test_ChannelMapOperation() {
    }

    /**
     Test creation of ChannelMapOperations from scratch & from existing operations
     */
    public void testMapCreation() {
        ChannelMapOperationFactory f = new ChannelMapOperationFactory();
        ColorCurve r = new ColorCurve();
        r.addPoint(0.0, 0.1);
        r.addPoint(0.2, 0.4);
        r.addPoint(1.0, 1.0);
        f.setChannelCurve("red", r);
        ColorCurve b = new ColorCurve();
        b.addPoint(0.0, 0.2);
        b.addPoint(0.4, 0.4);
        b.addPoint(1.0, 0.9);
        f.setChannelCurve("blue", b);
        ChannelMapOperation o = f.create();
        ColorCurve r1 = o.getChannelCurve("red");
        assertEquals(r, r1);
        ColorCurve b1 = o.getChannelCurve("blue");
        assertEquals(b, b1);
        ColorCurve e1 = o.getChannelCurve("nonexisting");
        assertNull(e1);
        ChannelMapOperationFactory f2 = new ChannelMapOperationFactory(o);
        ChannelMapOperation o2 = f2.create();
        assertEquals(o, o2);
        ColorCurve r2 = new ColorCurve();
        f2.setChannelCurve("red", r2);
        ChannelMapOperation o3 = f2.create();
        assertFalse(o2.equals(o3));
    }

    /**
     Test that converting ChannelMapOperation to its XML representation and back to 
     java object creates an identical object.
     */
    public void testXmlConvert() {
        ChannelMapOperationFactory f = new ChannelMapOperationFactory();
        ColorCurve r = new ColorCurve();
        r.addPoint(0.0, 0.1);
        r.addPoint(0.2, 0.4);
        r.addPoint(1.0, 1.0);
        f.setChannelCurve("red", r);
        ColorCurve b = new ColorCurve();
        b.addPoint(0.0, 0.2);
        b.addPoint(0.4, 0.4);
        b.addPoint(1.0, 0.9);
        f.setChannelCurve("blue", b);
        ChannelMapOperation o = f.create();
        String xml = o.getAsXml();
        ChannelMapOperation o2 = null;
        Digester d = new Digester();
        d.addRuleSet(new ChannelMapRuleSet());
        try {
            ChannelMapOperationFactory f2 = (ChannelMapOperationFactory) d.parse(new StringReader(xml));
            o2 = f2.create();
        } catch (SAXException ex) {
            fail(ex.getMessage());
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
        assertEquals(o, o2);
    }

    public static Test suite() {
        return new TestSuite(Test_ChannelMapOperation.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
