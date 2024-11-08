package spindles.api.db;

import static spindles.api.db.DB.runCmd;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import spindles.api.domain.SessionPart;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityCursor;

public class SessionPartDAO extends DAO<SessionPart> {

    public SessionPartDAO() {
        super(SessionPart.class, "sleepSessionID");
    }

    public Set<SessionPart> findSessionParts(final long sleepID) {
        final Set<SessionPart> result = new HashSet<SessionPart>();
        runCmd(new DBCommand() {

            public void execute() throws DatabaseException {
                final EntityCursor<SessionPart> parts = longSecondaryIndex.subIndex(sleepID).entities();
                runCmd(new Closeable<SessionPart>(parts) {

                    public void execute(EntityCursor<SessionPart> cursor) {
                        for (SessionPart s : cursor) {
                            result.add(s);
                        }
                    }
                });
            }
        });
        return result;
    }

    public boolean existsSessionPart(Date startDate, String channel, long sleepID) {
        Set<SessionPart> parts = findSessionParts(sleepID);
        for (SessionPart sp : parts) {
            if (sp.getStart().equals(startDate) && sp.getChannel().equals(channel)) {
                return true;
            }
        }
        return false;
    }
}
