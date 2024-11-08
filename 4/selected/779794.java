package jdbm.extser.profiler;

import java.io.PrintStream;
import jdbm.extser.AbstractExtensibleSerializer;
import jdbm.extser.ISerializer;

/**
 * Per version statistics (collected on a per class basis).
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: VersionStatistics.java,v 1.4 2006/05/02 16:20:07 thompsonbry Exp $
 */
public class VersionStatistics extends Statistics {

    public final short versionId;

    public final ISerializer serializer;

    public VersionStatistics(AbstractExtensibleSerializer serializer, short versionId, ISerializer versionSerializer) {
        super(serializer);
        this.versionId = versionId;
        this.serializer = versionSerializer;
    }

    public void writeOn(PrintStream ps) {
        long avgPerRead = (nread == 0 ? 0 : bytesRead / nread);
        long avgPerWrite = (nwritten == 0 ? 0 : bytesWritten / nwritten);
        ps.println("   versionId=" + versionId + ", class=" + serializer.getClass().getName() + ", read(" + nread + "," + bytesRead + "," + avgPerRead + ")" + ", write(" + nwritten + "," + bytesWritten + "," + avgPerWrite + ")");
    }
}
