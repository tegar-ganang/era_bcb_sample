package jdbm.extser.profiler;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jdbm.extser.AbstractExtensibleSerializer;
import jdbm.extser.ISerializer;
import jdbm.extser.NativeType;

/**
 * Collects statistics about serialization. You can use this to identify
 * objects for which serializers are not registered (they will show up as
 * <code>versionId == 0</code> and
 * <code>serializer == DefaultSerializer</code>, the versions of the
 * objects being read or written, etc. Objects wrapping Java primitives,
 * e.g., {@link Boolean},{@link Integer}, etc., as well as array types
 * will all have classId values less than {@link NativeType#FIRST_OBJECT_INDEX}.
 * <p>
 * 
 * Note: Objects which correspond to compound records (comprised of other
 * objects) will be double-counted by the profiler. That is, the profiler
 * will report the serialized size of both the compound record and each
 * object within that compound record.
 * <p>
 * 
 * The profiler is disabled by default. However it imposes a negligable
 * overhead. It may be enabled using a configuration option, in which case
 * the profiler will automatically report when it is finalized. Usually this
 * is not long after the recman is closed.
 * <p>
 * 
 * @author <a href="mailto:thompsonbry@users.sourceforge.net">Bryan Thompson</a>
 * @version $Id: Profiler.java,v 1.4 2006/05/02 16:20:06 thompsonbry Exp $
 */
public class Profiler extends Statistics {

    private final AbstractExtensibleSerializer serializer;

    private boolean enabled = false;

    /**
     * Enable or disable the profiler.  It is disabled by default.
     * 
     * @param val The new value.
     * 
     * @return The old value.
     */
    public boolean enable(boolean val) {
        boolean tmp = enabled;
        enabled = val;
        return tmp;
    }

    /**
     * If the profiler is enabled and there has been at least one read or
     * write reported, then writes the statistics onto {@link System#err}.
     * While it is deprecated, you can use
     * {@link System#runFinalizersOnExit(boolean)} to trigger this
     * automatically.
     */
    protected void finalize() throws Throwable {
        super.finalize();
        if (enabled && (nread > 0 || nwritten > 0)) {
            writeOn(System.err);
        }
    }

    public Profiler(AbstractExtensibleSerializer serializer) {
        super(serializer);
        reset();
        this.serializer = serializer;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Members are {@link ClassStatistics}.
     */
    private Map classStatistics;

    public ClassStatistics get(int classId) {
        Integer classId2 = new Integer(classId);
        ClassStatistics stats = (ClassStatistics) classStatistics.get(classId2);
        if (stats == null) {
            stats = new ClassStatistics(getSerializer(), classId);
            classStatistics.put(classId2, stats);
        }
        return stats;
    }

    public synchronized void serialized(ISerializer ser, int classId, short versionId, int nbytes) {
        if (enabled) {
            nread++;
            bytesRead += nbytes;
            get(classId).read(ser, versionId, nbytes);
        }
    }

    public synchronized void deserialized(ISerializer ser, int classId, short versionId, int nbytes) {
        if (enabled) {
            nwritten++;
            bytesWritten += nbytes;
            get(classId).write(ser, versionId, nbytes);
        }
    }

    /**
     * Write out the collected statistics.
     * 
     * @param ps
     * 
     * @todo Change to a tab delimited format so that you can analyze it in a
     *       spreadsheet.
     */
    public void writeOn(PrintStream ps) {
        ps.println("----- serialization statistics -----");
        ps.println("read(#read,#bytesRead,avgPerRead), write(#written,#bytesWritten,avgPerWrite)");
        super.writeOn(ps);
        Iterator itr = classStatistics.values().iterator();
        while (itr.hasNext()) {
            ClassStatistics stats = (ClassStatistics) itr.next();
            stats.writeOn(ps);
        }
        ps.println("------------------------------------");
    }

    public void reset() {
        super.reset();
        classStatistics = new HashMap();
    }
}
