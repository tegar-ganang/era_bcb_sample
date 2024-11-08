package com.gc.iotools.stream.is.inspection;

import static org.junit.Assert.*;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;

public class StatsInputStreamTest {

    @org.junit.Test
    public void fullReadOnClose() throws IOException {
        final InputStream istream = StatsInputStreamTest.class.getResourceAsStream("/testFile.txt");
        final StatsInputStream ris = new StatsInputStream(istream, true);
        ris.read(new byte[5]);
        assertEquals("read size", 5, ris.getSize());
        ris.close();
        assertEquals("in the end", 30, ris.getSize());
    }

    @org.junit.Test
    public void markAndReset() throws Exception {
        final InputStream istream = StatsInputStreamTest.class.getResourceAsStream("/testFile.txt");
        final StatsInputStream ris = new StatsInputStream(istream, true);
        ris.read(new byte[5]);
        assertEquals("read size", 5, ris.getSize());
        ris.mark(50);
        ris.read(new byte[10]);
        assertEquals("read after mark", 15, ris.getSize());
        ris.reset();
        assertEquals("read after reset", 5, ris.getSize());
        ris.close();
        assertEquals("in the end", 30, ris.getSize());
    }

    @org.junit.Test
    public void noFullReadOnClose() throws IOException {
        final InputStream istream = StatsInputStreamTest.class.getResourceAsStream("/testFile.txt");
        final StatsInputStream ris = new StatsInputStream(istream, false);
        ris.read(new byte[5]);
        assertEquals("read size", 5, ris.getSize());
        ris.close();
        assertEquals("in the end", 5, ris.getSize());
    }

    @org.junit.Test
    public void simpleRead() throws Exception {
        final InputStream istream = StatsInputStreamTest.class.getResourceAsStream("/testFile.txt");
        final StatsInputStream ris = new StatsInputStream(istream);
        assertEquals("read size", 0, ris.getSize());
        IOUtils.copy(ris, new NullOutputStream());
        assertEquals("in the end", 30, ris.getSize());
    }

    @org.junit.Test
    public void skip() throws Exception {
        final InputStream istream = StatsInputStreamTest.class.getResourceAsStream("/testFile.txt");
        final StatsInputStream ris = new StatsInputStream(istream, true);
        ris.read(new byte[5]);
        assertEquals("read size", 5, ris.getSize());
        ris.skip(10);
        assertEquals("read size", 15, ris.getSize());
        ris.close();
        assertEquals("in the end", 30, ris.getSize());
    }
}
