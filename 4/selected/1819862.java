package jdbm.extser.profiler;

import java.io.PrintStream;
import jdbm.extser.AbstractExtensibleSerializer;

/**
 * Abstract base class for collecting statistics on (de-)serialization.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: Statistics.java,v 1.4 2006/05/02 16:20:07 thompsonbry Exp $
 */
public abstract class Statistics {

    private final AbstractExtensibleSerializer serializer;

    public AbstractExtensibleSerializer getSerializer() {
        return serializer;
    }

    /**
     * @param serializer
     */
    public Statistics(AbstractExtensibleSerializer serializer) {
        this.serializer = serializer;
    }

    public long nread;

    public long bytesRead;

    public long nwritten;

    public long bytesWritten;

    public void read(int nbytes) {
        nread++;
        bytesRead += nbytes;
    }

    public void write(int nbytes) {
        nwritten++;
        bytesWritten += nbytes;
    }

    /**
     * Write the statistics.
     * @param ps Where to write the statistics.
     */
    public void writeOn(PrintStream ps) {
        long avgPerRead = (nread == 0 ? 0 : bytesRead / nread);
        long avgPerWrite = (nwritten == 0 ? 0 : bytesWritten / nwritten);
        ps.println("read(" + nread + "," + bytesRead + "," + avgPerRead + ")" + ", write(" + nwritten + "," + bytesWritten + "," + avgPerWrite + ")");
    }

    /**
     * Reset the statistics.
     */
    public void reset() {
        nread = bytesRead = 0L;
        nwritten = bytesWritten = 0L;
    }
}
