package spindles.api.db;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sleepycat.je.Transaction;
import spindles.api.domain.Epoch;
import spindles.api.domain.Interval;
import spindles.api.domain.Person;
import spindles.api.domain.SessionPart;
import spindles.api.domain.SleepSession;
import spindles.api.domain.SpindleIndication;
import spindles.api.domain.ThresholdGroup;
import spindles.api.domain.User;
import spindles.api.util.ApplicationException;

public class DBGateway {

    private PersonDAO personDAO = new PersonDAO();

    private SessionPartDAO partDAO = new SessionPartDAO();

    private SleepSessionDAO sleepDAO = new SleepSessionDAO();

    private DAO<ThresholdGroup> thresholdDAO = new DAO<ThresholdGroup>(ThresholdGroup.class, String.class, "groupName");

    private DAO<User> userDAO = new DAO<User>(User.class, String.class, "userName");

    private DAO<Epoch> epochDAO = new DAO<Epoch>(Epoch.class, "sessionPartID");

    public DBGateway() {
    }

    public DBGateway(Transaction txn) {
        setTransaction(txn);
    }

    public void setTransaction(Transaction txn) {
        personDAO.setTransaction(txn);
        partDAO.setTransaction(txn);
        sleepDAO.setTransaction(txn);
    }

    public ThresholdGroup getThresholdGroup(String groupName) {
        SortedSet<ThresholdGroup> result = thresholdDAO.findAllSorted(groupName);
        return result.first();
    }

    public ThresholdGroup getDefaultThresholdGroup() {
        SortedSet<ThresholdGroup> groups = thresholdDAO.findAllSorted();
        for (ThresholdGroup group : groups) {
            if (group.isDefaultGroup()) {
                return group;
            }
        }
        throw new ApplicationException("No default threshold group found");
    }

    public User getUser(String userName) {
        SortedSet<User> result = userDAO.findAllSorted(userName);
        return result.first();
    }

    public String getChannel(long sessionPartID) {
        SessionPart sp = partDAO.get(sessionPartID);
        return sp.getChannel();
    }

    public String getName(long sessionPartID) {
        SessionPart sp = partDAO.get(sessionPartID);
        SleepSession ss = sleepDAO.get(sp.getSleepSessionID());
        Person p = personDAO.get(ss.getPersonID());
        return p.getName();
    }

    public List<SpindleIndication> getSpindleIndications(Long sessionPartID, boolean soft) {
        List<SpindleIndication> result = new ArrayList<SpindleIndication>();
        SortedSet<Epoch> epochs = epochDAO.findAllSorted(sessionPartID);
        for (Epoch e : epochs) {
            result.addAll(e.getSpindleIndications(getDefaultThresholdGroup(), soft));
        }
        return result;
    }

    public List<SpindleIndication> getSpindleIndications(Long sessionPartID, Interval partInterval, boolean soft) {
        List<SpindleIndication> result = new ArrayList<SpindleIndication>();
        SortedSet<Epoch> epochs = epochDAO.findAllSorted(sessionPartID);
        for (Epoch e : epochs) {
            List<SpindleIndication> all = e.getSpindleIndications(getDefaultThresholdGroup(), soft);
            for (SpindleIndication si : all) {
                if ((si.getStart().compareTo(partInterval.getStart()) >= 0) && (si.getEnd().compareTo(partInterval.getEnd()) <= 0)) {
                    result.add(si);
                }
            }
        }
        return result;
    }

    public SortedSet<SessionPart> findAllSorted(long personID) {
        final SortedSet<SessionPart> result = new TreeSet<SessionPart>();
        SortedSet<SleepSession> sleeps = sleepDAO.findAllSorted(personID);
        for (SleepSession sleep : sleeps) {
            result.addAll(partDAO.findAllSorted(sleep.getId()));
        }
        return result;
    }

    public int getSamplingRate(long sessionPartID) {
        SessionPart sp = partDAO.get(sessionPartID);
        SleepSession ss = sleepDAO.get(sp.getSleepSessionID());
        return ss.getSamplingRate();
    }

    public String getSleepDate(long sessionPartID) {
        SessionPart sp = partDAO.get(sessionPartID);
        return sleepDAO.get(sp.getSleepSessionID()).getDateFormatted();
    }
}
