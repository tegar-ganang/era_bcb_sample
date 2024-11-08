package edu.usc.epigenome.uecgatk.YapingWalker;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.broadinstitute.sting.utils.GenomeLoc;

public class readsWriterImp extends FormatWriter {

    public readsWriterImp(File location) {
        super(location);
    }

    public readsWriterImp(OutputStream output) {
        super(output);
    }

    public readsWriterImp(File location, OutputStream output) {
        super(location, output);
    }

    @Override
    public void add(GenomeLoc loc, double value) {
    }

    @Override
    public void add(String contig, long start, long end, double value) {
    }

    @Override
    public void addHeader() {
    }

    /**
	 * @param args
	 */
    public void add(String contig, long pos, byte base, int baseQ, char strand, String readID) {
        String readsLine = String.format("%s\t%d\t%c\t%d\t%c\t%s\n", contig, pos, base, baseQ, strand, readID);
        try {
            mWriter.write(readsLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addCpg(String contig, long pos, byte base, int baseQ, char strand, double methyValue, String readID) {
        String readsLine = String.format("%s\t%d\t%c\t%d\t%c\t%.2f\t%s\n", contig, pos, base, baseQ, strand, methyValue, readID);
        try {
            mWriter.write(readsLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
