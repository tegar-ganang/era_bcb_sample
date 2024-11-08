package test.net.hawk.digiextractor.digic;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.hawk.digiextractor.digic.AbstractNameEntry;
import net.hawk.digiextractor.digic.S2TableOfContentsLine;
import net.hawk.digiextractor.digic.TOCEntry;
import net.hawk.digiextractor.digic.NameEntry;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The Class ReconstructionTest.
 * Test the reconstruction routines, repairing damaged recording names.
 * TODO: this test should be extended to do some in-depth checking of the
 *       reconstruction routines.
 */
public class ReconstructionTest {

    /** The minimum size of video entries.
	 * If an entry is bigger than this, we treat it as a video entry. */
    private static final int MIN_VIDEO_SIZE = 1000;

    /** The maximum difference in timestamp values for toc-entries that
	 *  belong together (in milliseconds). */
    private static final int MAX_TIME_DIFF = 10;

    /** The size of the ByteBuffer. */
    private static final int BUFFER_SIZE = 0xFFFF;

    /** A ByteBuffer containing the table of contents data. */
    private static ByteBuffer toc = ByteBuffer.allocate(BUFFER_SIZE);

    /** The list containing the single TOC-lines. */
    private static List<S2TableOfContentsLine> tocLines = new ArrayList<S2TableOfContentsLine>();

    /**
	 * Sets the up before class.
	 * Read the binary table of contents data from file.
	 * Fill the list of TOC-lines.
	 *
	 * @throws Exception the exception
	 */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        File f = new File("./testdata/TOC1.dat");
        FileInputStream fs = new FileInputStream(f);
        fs.getChannel().read(toc);
        toc.rewind();
        fs.close();
        int i = 0;
        while (toc.remaining() > S2TableOfContentsLine.LINE_LENGTH) {
            tocLines.add(new S2TableOfContentsLine(toc, i++));
        }
    }

    /**
	 * Filter by timestamp.
	 * Helper function that searches the list of TOC-lines for an entry which
	 * has a timestamp that differs by no more than 10 milliseconds from the
	 * given timestamp value.
	 *
	 * @param lines the list of TOC-lines that should be searched.
	 * @param timestamp the timestamp to search for.
	 * @return the list of matching TOC-lines.
	 */
    private List<S2TableOfContentsLine> filterByTimestamp(final List<S2TableOfContentsLine> lines, final long timestamp) {
        List<S2TableOfContentsLine> result = new ArrayList<S2TableOfContentsLine>();
        for (S2TableOfContentsLine e : lines) {
            if (Math.abs(e.getTimestamp() - timestamp) < MAX_TIME_DIFF) {
                result.add(e);
            }
        }
        return result;
    }

    /**
	 * Perform advanced reconstruction of recording entries.
	 * (Find pairs of toc-lines that have (almost) the same timestamp.)
	 *
	 * @param lines the table of contents lines to search for entries.
	 * @return the list of reconstructed table of content entries.
	 */
    public final List<TOCEntry> advancedRecon(final List<S2TableOfContentsLine> lines) {
        ArrayList<TOCEntry> result = new ArrayList<TOCEntry>();
        Collections.sort(lines);
        while (lines.get(0).getTimestamp() == 0) {
            lines.remove(0);
        }
        ArrayList<S2TableOfContentsLine> candRecord = new ArrayList<S2TableOfContentsLine>();
        ArrayList<S2TableOfContentsLine> candInfo = new ArrayList<S2TableOfContentsLine>();
        for (S2TableOfContentsLine t : lines) {
            if (t.getSize() > MIN_VIDEO_SIZE) {
                candRecord.add(t);
            } else {
                candInfo.add(t);
            }
        }
        int i = 0;
        for (S2TableOfContentsLine t : candRecord) {
            List<S2TableOfContentsLine> maybeInfo = filterByTimestamp(candInfo, t.getTimestamp());
            if (maybeInfo.size() == 1) {
                System.out.println("found exactly one match");
                System.out.println(t.getSize() / (maybeInfo.get(0).getSize() + 1));
                AbstractNameEntry name = new NameEntry(++i);
                result.add(new TOCEntry(t, maybeInfo.get(0), name));
            } else {
                System.out.println("multiple or no match for recording" + maybeInfo.size());
            }
        }
        return result;
    }

    /**
	 * Test reconstruction.
	 */
    @Test
    public final void testReconstruction() {
        List<TOCEntry> res = advancedRecon(tocLines);
        for (TOCEntry t : res) {
            System.out.println(t);
        }
        assertTrue(res.size() > 0);
    }

    /**
	 * Tear down after class.
	 *
	 * @throws Exception the exception
	 */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }
}
