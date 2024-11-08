package test.de.unibi.techfak.bibiserv.biodom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import junit.framework.Assert;
import junit.framework.TestCase;
import de.unibi.techfak.bibiserv.biodom.PhyloML;
import de.unibi.techfak.bibiserv.biodom.PhyloMLInterface;
import de.unibi.techfak.bibiserv.biodom.exception.BioDOMException;

/**
 * testing-class for PhyloML
 * 
 * @author Alexander Kaiser <akaiser@techfak.uni-bielefeld.de>
 * @version $Id: PhyloMLTest.java,v 1.7 2006/08/07 08:21:48 spindle_dev Exp $
 */
public class PhyloMLTest extends TestCase {

    private String TREE_WITH_ANCESTORS = "((SEQ6:125)SEQ5:250,(SEQ10:250,SEQ14:250):125);";

    private String BORING_TREE = "((),(((),()),()));";

    private String BROKEN_TREE = "((HTL2:0.1111024(MMLV:0.078471ECOL:0.078471):0.032554):0.0121218HEPB:0.232242);";

    private String XML_LOC = "/test/de/unibi/techfak/bibiserv/biodom/data/phyloML_sample.xml";

    private PhyloMLInterface pm;

    protected void setUp() throws Exception {
        super.setUp();
        pm = new PhyloML();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(PhyloMLTest.class);
    }

    public void testAppendTree() {
        try {
            pm.appendTree(TREE_WITH_ANCESTORS);
            System.out.println(pm.toString());
            Assert.assertTrue(pm.validate());
            System.out.println(pm.toPhylip());
            Assert.assertEquals(TREE_WITH_ANCESTORS.trim(), pm.toPhylip());
        } catch (BioDOMException e) {
            System.err.println(e);
            Assert.assertTrue(false);
        }
    }

    public void testAppendTree2() {
        try {
            pm.appendTree(BORING_TREE);
            System.out.println(pm.toString());
            Assert.assertTrue(pm.validate());
            System.out.println(pm.toPhylip());
            Assert.assertEquals(BORING_TREE, pm.toPhylip());
        } catch (BioDOMException e) {
            System.err.println(e);
            Assert.assertTrue(false);
        }
    }

    public void testAppendTree3() {
        try {
            pm.appendTree(BROKEN_TREE);
            System.out.println(pm.toPhylip());
            Assert.assertFalse(true);
        } catch (BioDOMException e) {
            System.out.println(e);
            Assert.assertFalse(false);
        }
    }

    public void testSetDOM() throws BioDOMException {
        String xml = is2String(this.getClass().getResourceAsStream(XML_LOC));
        pm.setDom(xml);
        System.out.println(pm.toPhylip());
        Assert.assertEquals(pm.toPhylip(), TREE_WITH_ANCESTORS.trim());
    }

    private static String is2String(InputStream is) {
        int c;
        int buffSize = 1024;
        byte buff[] = new byte[buffSize];
        OutputStream os = new ByteArrayOutputStream(buffSize);
        try {
            while ((c = is.read(buff)) != -1) os.write(buff, 0, c);
            return os.toString();
        } catch (IOException ex) {
            return null;
        }
    }
}
