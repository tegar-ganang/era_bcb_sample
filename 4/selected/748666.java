package wfinder;

import static org.jlambda.util.Text.readLines;
import static org.jlambda.util.Unmemoize.unmemoize;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import org.jlambda.functions.Fun1;
import org.jlambda.functions.Fun2;
import org.jlambda.util.ByteBufferToReadableByteChannel;
import org.jlambda.util.Text;

public class SimpleFileReads extends TestCase {

    final String FNAME = "o1000k.ap";

    long start = 0;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        start = System.currentTimeMillis();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        System.out.println("Test " + getName() + " took " + (System.currentTimeMillis() - start) + " ms");
    }

    Pattern pattern = Pattern.compile("GET /ongoing/When/.*? ");

    Fun1<Iterable<String>, Void> doit = Fun2.compose(new Ranking().apply(pattern), new Printem());

    public void testSimpleBufferedStreamReader() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(FNAME), "us-ascii"));
        try {
            Iterable<String> lines = unmemoize(readLines, reader);
            doit.apply(lines);
        } finally {
            reader.close();
        }
    }

    public void testSimpleBufferedFileReader() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(FNAME));
        try {
            Iterable<String> lines = unmemoize(readLines, reader);
            doit.apply(lines);
        } finally {
            reader.close();
        }
    }

    public void testNIOChannelsReader() throws Exception {
        File f = new File(FNAME);
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
        try {
            Iterable<String> lines = unmemoize(Text.readLines, new BufferedReader(Channels.newReader(new ByteBufferToReadableByteChannel(bb), "us-ascii"), 1000));
            doit.apply(lines);
        } finally {
            fc.close();
        }
    }
}
