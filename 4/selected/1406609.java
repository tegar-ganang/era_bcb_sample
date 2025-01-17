package org.gzigzag.impl;

import org.gzigzag.*;
import org.gzigzag.mediaserver.*;
import junit.framework.*;
import java.io.*;

/** Test the reader and writer for GZZ1 space format
 */
public class TestGZZ1Ugliness extends TestCase {

    public static final String rcsid = "$Id: TestGZZ1Ugliness.java,v 1.10 2002/03/10 01:16:23 bfallenstein Exp $";

    public TestGZZ1Ugliness(String s) {
        super(s);
    }

    PermanentSpace readFrom, writeTo;

    GZZ1Ugliness ugly;

    Cell c1, c2, cspan;

    public void setUp() throws IOException {
        readFrom = new PermanentSpace(TestImpl.zms);
        Dim vdim = (Dim) readFrom.getVStreamDim();
        Cell h = readFrom.getHomeCell();
        c1 = h.N(h);
        c2 = c1.N(h);
        cspan = readFrom.makeSpanRank("blaah", c1);
        c2.connect(h, cspan);
        Mediaserver.Id id = readFrom.save(TestImpl.zms);
        readFrom = new PermanentSpace(TestImpl.zms, id);
        h = readFrom.getHomeCell();
        c1 = h.s(h);
        c2 = c1.s(h);
        cspan = c2.s(h);
        writeTo = new PermanentSpace(TestImpl.zms);
        ugly = new GZZ1Ugliness(readFrom, writeTo);
    }

    public void testUgliness() throws IOException {
        assertTrue(!writeTo.exists(c1.id));
        assertTrue(!writeTo.exists(c2.id));
        ugly.makeExist(c1.id);
        assertTrue(writeTo.exists(c1.id));
        assertTrue(!writeTo.exists(c2.id));
        ugly.makeExist(c1.id);
        assertTrue(writeTo.exists(c1.id));
        assertTrue(!writeTo.exists(c2.id));
        ugly.makeExist(c2.id);
        assertTrue(writeTo.exists(c1.id));
        assertTrue(writeTo.exists(c2.id));
    }

    public void testSave() throws IOException {
        assertTrue(readFrom.exists(cspan.id));
        ugly.makeExist(c1.id);
        ugly.makeExist(cspan.id);
        Cell c = writeTo.getCell(cspan.id), d = c.s(Dims.d_vstream_id);
        c.disconnect(Dims.d_vstream_id, 1);
        c.connect(Dims.d_vstream_id, d);
        writeTo.getCell(c1.id).connect(Dims.d_vstream_id, c);
        Mediaserver.Id saved = writeTo.save(TestImpl.zms);
        PermanentSpace loaded = new PermanentSpace(TestImpl.zms, saved);
        assertTrue(loaded.exists(c1.id));
        assertTrue(!loaded.exists(c2.id));
        assertTrue("Exist " + cspan.id, loaded.exists(cspan.id));
        Cell x = loaded.getCell(c1.id);
        assertEquals("blaah", x.t());
    }

    public void testSpans() throws IOException {
        assertTrue(!writeTo.exists(cspan.id));
        ugly.makeExist(cspan.id);
        assertTrue(writeTo.exists(cspan.id));
        cspan = writeTo.getCell(cspan.id);
        Cell x = cspan.N((Dim) writeTo.getVStreamDim(), -1);
        assertEquals("blaah", x.t());
    }
}
