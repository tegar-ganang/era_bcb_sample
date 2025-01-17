package edu.usc.epigenome.uecgatk.benWalkers.cytosineWalkers;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.broadinstitute.sting.commandline.Argument;
import org.broadinstitute.sting.gatk.contexts.AlignmentContext;
import edu.usc.epigenome.genomeLibs.ListUtils;
import edu.usc.epigenome.uecgatk.benWalkers.CpgBackedByGatk;
import edu.usc.epigenome.uecgatk.benWalkers.LocusWalkerToBisulfiteCytosineWalker;

public class CytosineToMethylDbWalker extends LocusWalkerToBisulfiteCytosineWalker<Integer, Long> {

    @Argument(fullName = "outPrefix", shortName = "pre", doc = "Output prefix for all output files", required = true)
    public String outPrefix = null;

    protected static Map<String, FileChannel> outfilesByChr = new HashMap<String, FileChannel>();

    private static final ReentrantLock outfilesMapLock = new ReentrantLock();

    protected Charset charset = Charset.forName("UTF-8");

    /**
	 * Provides an initial value for the reduce function.  Hello walker counts loci,
	 * so the base case for the inductive step is 0, indicating that the walker has seen 0 loci.
	 * @return 0.
	 */
    @Override
    public Long reduceInit() {
        Long out = 0L;
        return out;
    }

    @Override
    public Long treeReduce(Long lhs, Long rhs) {
        return lhs + rhs;
    }

    /**
	 * Retrieves the final result of the traversal.
	 * @param result The ultimate value of the traversal, produced when map[n] is combined with reduce[n-1]
	 *               by the reduce function. 
	 */
    @Override
    public void onTraversalDone(Long result) {
        out.printf("Saw %d cytosines\n", result);
        for (String chr : outfilesByChr.keySet()) {
            FileChannel fc = outfilesByChr.get(chr);
            try {
                fc.close();
            } catch (Exception e) {
                System.err.printf("Fatal error , could close file for %s\n%s\n", chr, e.toString());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /***************************************************
	 * cytosine walker overrides
	 ***************************************************/
    @Override
    protected void alertNewContig(String newContig) {
    }

    @Override
    protected Integer processCytosine(CpgBackedByGatk thisC) {
        FileChannel fc = null;
        String chr = thisC.getChrom();
        outfilesMapLock.lock();
        try {
            if (outfilesByChr.containsKey(chr)) {
                fc = outfilesByChr.get(chr);
            } else {
                String name = String.format("%s-%s", this.outPrefix, chr);
                String outfn = String.format("%s.txt", name);
                logger.info("NEW file " + outfn);
                FileOutputStream fout = null;
                fout = new FileOutputStream(outfn);
                fc = fout.getChannel();
                outfilesByChr.put(chr, fc);
            }
        } catch (Exception e) {
            System.err.printf("Fatal error , could not write to file for  %s\n%s\n", chr, e.toString());
            e.printStackTrace();
            System.exit(1);
        } finally {
            outfilesMapLock.unlock();
        }
        try {
            fc.write(charset.newEncoder().encode(CharBuffer.wrap(thisC.toString() + "\n")));
        } catch (Exception e) {
            System.err.printf("Fatal error , could not write to file for %s\n%s\n", chr, e.toString());
            e.printStackTrace();
            System.exit(1);
        }
        return 1;
    }

    @Override
    protected Long reduceCytosines(Integer value, Long sum) {
        return value.longValue() + sum;
    }
}
