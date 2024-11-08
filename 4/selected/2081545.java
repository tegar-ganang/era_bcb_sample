package wfinder;

import static org.jlambda.util.Lists.foldl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import junit.framework.TestCase;
import org.jlambda.Context;
import org.jlambda.functions.Fun1;
import org.jlambda.functions.Fun2;
import org.jlambda.functions.Fun3;
import org.jlambda.listFunctions.ListFun2;
import org.jlambda.tuples.Tuple2;
import org.jlambda.util.Lists;
import org.jlambda.util.Text;
import org.jlambda.util.Unmemoize;
import org.jlambda.util.ConcurrentUtils.MapBg;
import org.jlambda.util.Text.ReadLineBlocks;

public class BlockFileReads extends TestCase {

    final String FNAME = "o1000k.ap";

    long start = 0;

    static {
        Context.MaxPool = Runtime.getRuntime().availableProcessors() * 2;
        Context.StartPool = Context.MaxPool;
    }

    public BlockFileReads() {
        super();
    }

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

    Pattern pattern = Pattern.compile("GET /ongoing/When/\\d\\d\\dx/(\\d\\d\\d\\d/\\d\\d/\\d\\d/[^ .]+) ");

    private void doSplitChannelReader(Fun2<Integer, ByteBuffer, Iterable<InputStream>> blockReader, Fun2<Fun1<InputStream, Map<String, Integer>>, Iterable<InputStream>, Iterable<Map<String, Integer>>> fun) throws Exception {
        File f = new File(FNAME);
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        int sz = (int) fc.size();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
        try {
            Iterable<InputStream> streams = blockReader.apply((int) (fc.size() / Context.MaxPool), bb);
            HashMap<String, Integer> results = foldl(new Fun2<Map<String, Integer>, HashMap<String, Integer>, HashMap<String, Integer>>() {

                @Override
                public HashMap<String, Integer> apply(Map<String, Integer> p1, HashMap<String, Integer> p2) {
                    for (Map.Entry<String, Integer> entry : p1.entrySet()) {
                        int i = entry.getValue();
                        int j = (p2.containsKey(entry.getKey())) ? p2.get(entry.getKey()) : 0;
                        p2.put(entry.getKey(), i + j);
                    }
                    return p2;
                }
            }, fun.apply(Fun3.compose(new Text.InputStreamToBufferedReader().apply(Charset.forName("us-ascii")), new Unmemoize.Unmemoizer1<BufferedReader, String>().apply(Text.readLines), new Ranking().apply(pattern)), streams), new HashMap<String, Integer>());
            new Printem().apply(results);
        } finally {
            fc.close();
        }
    }

    public void testSplitChannelReader() throws Exception {
        ListFun2<Fun1<InputStream, Map<String, Integer>>, Iterable<InputStream>, Map<String, Integer>> fun = new Lists.Map<InputStream, Map<String, Integer>>();
        fun.setMemoized(false);
        doSplitChannelReader(new ReadLineBlocks(), fun);
    }

    public void testSplitChannelReaderBGSimple() throws Exception {
        Fun2<Fun1<InputStream, Map<String, Integer>>, Iterable<InputStream>, Iterable<Map<String, Integer>>> fun = new MapBg<InputStream, Map<String, Integer>>().apply(null, null, null);
        doSplitChannelReader(new ReadLineBlocks(), fun);
    }

    public void testSplitChannelReaderBGChoice() throws Exception {
        Fun2<Fun1<InputStream, Map<String, Integer>>, Iterable<InputStream>, Iterable<Map<String, Integer>>> fun = new MapBg<InputStream, Map<String, Integer>>().apply(null, null, Tuple2.tuple(1, 20));
        doSplitChannelReader(new ReadLineBlocks(), fun);
    }
}
