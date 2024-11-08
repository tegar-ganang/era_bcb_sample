package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.IOException;
import uk.ac.gla.terrier.compression.BitOut;
import uk.ac.gla.terrier.utility.FieldScore;

/** Class holding the information for a posting list read
 * from a previously written run at disk. Used in the merging phase of the Single pass inversion method.
 * This class knows how to append itself to a {@link uk.ac.gla.terrier.compression.BitOut} and it
 * represents a posting with field information <code>(tf, df, [docid, idf, fieldScore])</code>
 * @author Roi Blanco
 *
 */
public class FieldPostingInRun extends SimplePostingInRun {

    /** The number of different fields that are used for indexing field information.*/
    protected static final int fieldTags = FieldScore.FIELDS_COUNT;

    /**
	 * Constructor for the class.
	 */
    public FieldPostingInRun() {
        super();
    }

    /**
	 * Writes the document data of this posting to a {@link uk.ac.gla.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as d1, idf(d1), fieldScore(d1) , d2 - d1, idf(d2), fieldScore(d2) etc.
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in this posting.
	 * @param runShift amount of delta to apply to the first posting read.
	 * @return The docid of the last posting written.
	 */
    public int append(BitOut bos, int last, int runShift) throws IOException {
        int current = runShift - 1;
        for (int i = 0; i < termDf; i++) {
            int docid = postingSource.readGamma() + current;
            bos.writeGamma(docid - last);
            bos.writeUnary(postingSource.readGamma());
            bos.writeBinary(fieldTags, postingSource.readBinary(fieldTags));
            current = last = docid;
        }
        try {
            postingSource.align();
        } catch (Exception e) {
        }
        return last;
    }
}
