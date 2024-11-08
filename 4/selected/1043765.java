package edu.usc.epigenome.uecgatk.YapingWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author yaping
 * @contact lyping1986@gmail.com
 * @time 2012 Mar 14, 2012 6:11:39 PM
 * 
 */
public class NOMeSeqReadsWriterImp extends FormatWriterBase {

    /**
	 * @param location
	 */
    public NOMeSeqReadsWriterImp(File location) {
        super(location);
    }

    /**
	 * @param output
	 */
    public NOMeSeqReadsWriterImp(OutputStream output) {
        super(output);
    }

    /**
	 * @param location
	 * @param output
	 */
    public NOMeSeqReadsWriterImp(File location, OutputStream output) {
        super(location, output);
    }

    @Override
    public void add(genomeObject obj) {
        String readsLine = String.format("%s\t%d\t%s\t%s\t%c\t%d\t%c\t%d\n", obj.getChr(), obj.getStart(), ((NOMeSeqReads) obj).getRefContext(), ((NOMeSeqReads) obj).getSampleContext(), ((NOMeSeqReads) obj).getMethyStatus(), ((NOMeSeqReads) obj).getbaseQ(), ((NOMeSeqReads) obj).getstrand(), ((NOMeSeqReads) obj).getEncryptID());
        try {
            mWriter.write(readsLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addHeader(Object o) {
        String header = "#chr\tpos\trefContext\tsampleContext\tmethyStatus\tbaseQ\tstrand\treadID\n";
        try {
            mWriter.write(header);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
