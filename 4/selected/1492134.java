package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.IOException;
import uk.ac.gla.terrier.compression.BitOut;

/** Class holding the information for a posting list read
 * from a previously written run at disk. Used in the merging phase of the Single pass inversion method.
 * This class knows how to append itself to a {@link uk.ac.gla.terrier.compression.BitOut} and it
 * represents a posting with blocks information <code>(tf, df, [docid, idf, blockFr [blockid]])</code>
 * @author Roi Blanco
 *
 */
public class BlockPostingInRun extends SimplePostingInRun {

    /**
	 * Constructor for the class.
	 */
    public BlockPostingInRun() {
        super();
    }

    /**
	 * Writes the document data of this posting to a {@link uk.ac.gla.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as <code>d1, idf(d1), blockNo(d1), bid1, bid2, ...,  d2 - d1, idf(d2), blockNo(d2), ...</code> etc
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in this posting.
	 * @param runShift amount of delta to apply to the first posting read.
	 * @return The last posting written.
	 */
    public int append(BitOut bos, int last, int runShift) throws IOException {
        int current = runShift - 1;
        for (int i = 0; i < termDf; i++) {
            int docid = postingSource.readGamma() + current;
            bos.writeGamma(docid - last);
            bos.writeUnary(postingSource.readGamma());
            current = last = docid;
            final int numOfBlocks = postingSource.readUnary() - 1;
            bos.writeUnary(numOfBlocks + 1);
            if (numOfBlocks > 0) for (int j = 0; j < numOfBlocks; j++) {
                bos.writeGamma(postingSource.readGamma());
            }
        }
        try {
            postingSource.align();
        } catch (Exception e) {
        }
        return last;
    }
}
