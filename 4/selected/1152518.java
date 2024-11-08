package edu.utah.seq.data.sam;

import net.sf.picard.sam.*;
import net.sf.picard.cmdline.CommandLineProgram;
import net.sf.picard.io.IoUtil;
import net.sf.samtools.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class PicardSortSam extends CommandLineProgram {

    public File INPUT;

    public File OUTPUT;

    public SAMFileHeader.SortOrder SORT_ORDER;

    protected int doWork() {
        SAMFileReader reader = new SAMFileReader(IoUtil.openFileForReading(INPUT));
        reader.getFileHeader().setSortOrder(SORT_ORDER);
        SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader.getFileHeader(), false, OUTPUT);
        Iterator<SAMRecord> iterator = reader.iterator();
        while (iterator.hasNext()) writer.addAlignment(iterator.next());
        reader.close();
        writer.close();
        return 0;
    }

    /** Launches SortSam suppressing all output except warnings and errors
	 * Temp files are written to the parent of the outputBamFile
	 */
    public PicardSortSam(File inputSamFile, File outputBamFile) {
        File realOutputFile;
        try {
            realOutputFile = outputBamFile.getCanonicalFile();
            String[] argv = { "I=" + inputSamFile, "O=" + outputBamFile, "SO=coordinate", "CREATE_INDEX=true", "TMP_DIR=" + realOutputFile.getParent(), "QUIET=true", "VERBOSITY=WARNING" };
            new SortSam().instanceMain(argv);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
