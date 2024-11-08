package net.sourceforge.entrainer.eeg.persistence;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import net.sourceforge.entrainer.eeg.core.EEGChannelState;
import net.sourceforge.entrainer.eeg.core.EEGChannelValue;
import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;

public class EEGSessionManager {

    public static final String DB = "EntrainerEEG_DB.neodatis";

    private EEGSession session;

    public EEGSessionManager() {
        super();
    }

    public EEGSessionManager(EEGSession session) {
        super();
        setSession(session);
    }

    public static ODB getDatabase() {
        return ODBFactory.open(DB);
    }

    public void persistSession() {
        ODB odb = getDatabase();
        try {
            getSession().setSessionCount(countSessionsForUser(odb));
            getSession().setDate(Calendar.getInstance().getTime());
            odb.store(getSession());
        } finally {
            closeDB(odb);
        }
    }

    public Objects<EEGSession> getSessionsForDate(Date date) {
        IQuery query = new CriteriaQuery(EEGSession.class, Where.equal("date", date));
        ODB odb = getDatabase();
        Objects<EEGSession> sessions = null;
        try {
            sessions = odb.getObjects(query);
        } finally {
            closeDB(odb);
        }
        return sessions;
    }

    public EEGSession getLastSessionForDate(Date date) {
        Objects<EEGSession> sessions = getSessionsForDate(date);
        EEGSession session = null;
        for (EEGSession sess : sessions) {
            if (session == null || sess.getSessionCount() > session.getSessionCount()) {
                session = sess;
            }
        }
        return session;
    }

    private void closeDB(ODB odb) {
        if (odb != null) {
            odb.close();
        }
    }

    private int countSessionsForUser(ODB odb) {
        IQuery query = new CriteriaQuery(EEGSession.class, Where.equal("firstName", getSession().getUser().getFirstName()));
        Objects<EEGSession> results = odb.getObjects(query);
        return results.size();
    }

    public void addChannelStates(List<EEGChannelState> states) {
        for (EEGChannelState state : states) {
            addChannelState(state);
        }
    }

    public void addChannelState(EEGChannelState state) {
        EEGSessionData data = getSession().getSessionDataForState(state);
        if (data == null) {
            data = new EEGSessionData(state);
            getSession().getSessionData().add(data);
        }
    }

    public void addChannelValue(EEGChannelValue value) {
        EEGSessionData data = getSession().getSessionDataForState(value.getChannelState());
        data.addState(value);
    }

    public EEGSession getSession() {
        return session;
    }

    public void setSession(EEGSession session) {
        this.session = session;
    }
}
