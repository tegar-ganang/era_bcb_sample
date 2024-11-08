package uk.ac.gla.terrier.structures.indexing.singlepass;

import java.io.IOException;
import uk.ac.gla.terrier.compression.BitOut;

/** Class holding the information for a posting list read
 * from a previously written run at disk. Used in the merging phase of the Single pass inversion method.
 * This class knows how to append itself to a {@link uk.ac.gla.terrier.compression.BitOut} and it
 * represents the simpler class of posting <code>(TF, df, [docid, tf])</code>
 * @author Roi Blanco
 *
 */
public class SimplePostingInRun extends PostingInRun {

    /**
	 * Constructor for the class.
	 */
    public SimplePostingInRun() {
        termTF = 0;
    }

    /**
	 * Writes the document data of this posting to a {@link uk.ac.gla.terrier.compression.BitOut} 
	 * It encodes the data with the right compression methods.
	 * The stream is written as <code>d1, idf(d1) , d2 - d1, idf(d2)</code> etc.
	 * @param bos BitOut to be written.
	 * @param last int representing the last document written in timport uk.ac.gla.terrier.structures.indexing.singlepass.RunReader;his posting.
	 * @return The last posting written.
	 */
    public int append(BitOut bos, int last, int runShift) throws IOException {
        int current = runShift - 1;
        for (int i = 0; i < termDf; i++) {
            final int docid = postingSource.readGamma() + current;
            bos.writeGamma(docid - last);
            bos.writeUnary(postingSource.readGamma());
            current = last = docid;
        }
        try {
            postingSource.align();
        } catch (Exception e) {
        }
        return last;
    }
}
