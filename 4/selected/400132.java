package jdbm.extser.profiler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jdbm.extser.AbstractExtensibleSerializer;
import jdbm.extser.ISerializer;
import jdbm.extser.NativeType;
import jdbm.extser.Stateless;

/**
 * Per class statistics.
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: ClassStatistics.java,v 1.6 2006/05/03 18:51:56 thompsonbry Exp $
 */
public class ClassStatistics extends Statistics {

    public final int classId;

    public final String classname;

    public final boolean stateless;

    public ClassStatistics(AbstractExtensibleSerializer serializer, int classId) {
        super(serializer);
        this.classId = classId;
        if (classId >= NativeType.NULL && classId <= NativeType.OBJECT_ARRAY) {
            classname = NativeType.asString(classId);
            stateless = false;
        } else {
            classname = getSerializer().getClassName(classId);
            try {
                Class cl = Class.forName(classname);
                stateless = Stateless.class.isAssignableFrom(cl);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
        reset();
    }

    public VersionStatistics get(short versionId, ISerializer serializer) {
        Short versionId2 = new Short(versionId);
        VersionStatistics stats = (VersionStatistics) versionStatistics.get(versionId2);
        if (stats == null) {
            stats = new VersionStatistics(getSerializer(), versionId, serializer);
            versionStatistics.put(versionId2, stats);
        }
        return stats;
    }

    /**
     * Members are {@link VersionStatistics}. 
     */
    public Map versionStatistics;

    public void reset() {
        super.reset();
        versionStatistics = new HashMap();
    }

    public void read(ISerializer ser, short version, int nbytes) {
        super.read(nbytes);
        get(version, ser).read(nbytes);
    }

    public void write(ISerializer ser, short version, int nbytes) {
        super.write(nbytes);
        get(version, ser).write(nbytes);
    }

    public void writeOn(PrintStream ps) {
        long avgPerRead = (nread == 0 ? 0 : bytesRead / nread);
        long avgPerWrite = (nwritten == 0 ? 0 : bytesWritten / nwritten);
        ps.println("class=" + classname + (stateless ? "(Stateless)" : "") + ", classId=" + classId + ", read(" + nread + "," + bytesRead + "," + avgPerRead + ")" + ", write(" + nwritten + "," + bytesWritten + "," + avgPerWrite + ")");
        Iterator itr = versionStatistics.values().iterator();
        while (itr.hasNext()) {
            VersionStatistics stats = (VersionStatistics) itr.next();
            stats.writeOn(ps);
        }
    }
}
