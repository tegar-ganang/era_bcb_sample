package spindles.api.services;

import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spindles.api.db.DAO;
import spindles.api.db.DB;
import spindles.api.db.DBGateway;
import spindles.api.db.PersonDAO;
import spindles.api.db.SessionPartDAO;
import spindles.api.db.SleepSessionDAO;
import spindles.api.domain.Audit;
import spindles.api.domain.Epoch;
import spindles.api.domain.Interval;
import spindles.api.domain.Person;
import spindles.api.domain.SessionPart;
import spindles.api.domain.Settings;
import spindles.api.domain.SleepSession;
import spindles.api.domain.ThresholdGroup;
import spindles.api.domain.User;
import spindles.api.domain.VDSpindle;
import spindles.api.util.ApplicationException;
import spindles.api.util.ErrorMessages;
import spindles.api.util.FileUtil;
import spindles.api.util.Processor;
import spindles.api.util.UserException;
import spindles.api.util.Util;
import com.sleepycat.je.Transaction;

public class ImportService {

    final Logger log = LoggerFactory.getLogger(ImportService.class);

    private File file;

    private EEGParser parser;

    private PersonDAO personDAO = new PersonDAO();

    private SessionPartDAO partDAO = new SessionPartDAO();

    private SleepSessionDAO sessionDAO = new SleepSessionDAO();

    private DBGateway db = new DBGateway();

    private DAO<Epoch> epochDAO = new DAO<Epoch>(Epoch.class, "sessionPartID");

    private DAO<Audit> auditDAO = new DAO<Audit>(Audit.class);

    private Transaction impTxn;

    private ThresholdGroup group;

    private Audit audit;

    public ImportService(ThresholdGroup group) {
        setThresholdGroup(group);
    }

    public ImportService() {
    }

    ;

    private void validateFilename(String filename) throws UserException {
        if (!FileUtil.validFilename(filename)) {
            throw new UserException(ErrorMessages.FILENAME_INVALID);
        }
    }

    public void setThresholdGroup(ThresholdGroup group) {
        this.group = group;
    }

    public void importSettings(String exportDir) {
        importThresholdGroups();
        Transaction txn = DB.beginTransaction();
        try {
            DAO<Settings> dao = new DAO<Settings>(Settings.class);
            dao.setTransaction(txn);
            Settings s = new Settings();
            s.setDefaultGroupName(db.getDefaultThresholdGroup().getGroupName());
            s.setExportDirectory(exportDir);
            s.setShowOnlyRawData(false);
            s.setPlotHeight(800);
            s.setPlotWidth(1100);
            dao.put(s);
            dao.commit();
        } catch (ApplicationException e) {
            DB.abort(txn);
            throw e;
        }
    }

    public void importThresholdGroups() {
        Transaction txn = DB.beginTransaction();
        try {
            DAO<ThresholdGroup> dao = new DAO<ThresholdGroup>(ThresholdGroup.class);
            dao.setTransaction(txn);
            List<ThresholdGroup> all = ThresholdGroup.findAll();
            for (ThresholdGroup group : all) {
                try {
                    db.getThresholdGroup(group.getGroupName());
                } catch (NoSuchElementException e) {
                    dao.put(group);
                    continue;
                }
            }
            dao.commit();
        } catch (ApplicationException e) {
            DB.abort(txn);
            throw e;
        }
    }

    public void importUsers() {
        Transaction txn = DB.beginTransaction();
        try {
            DAO<User> dao = new DAO<User>(User.class);
            dao.setTransaction(txn);
            try {
                db.getUser(User.DEFAULT_USERNAME);
            } catch (NoSuchElementException e) {
                dao.put(User.getDefaultUser());
            }
            dao.commit();
        } catch (ApplicationException e) {
            DB.abort(txn);
            throw e;
        }
    }

    public void importSpindles(String filename, final long sessionPartID) throws UserException {
        Transaction txn = DB.beginTransaction();
        try {
            epochDAO.setTransaction(txn);
            validateFilename(filename);
            file = new File(filename);
            FileUtil.readFile(file, new Processor() {

                public void process(BufferedReader r) throws Exception {
                    Collection<Epoch> epochs = epochDAO.findAllSorted(sessionPartID);
                    User user = db.getUser(User.DEFAULT_USERNAME);
                    String line = null;
                    Interval lastISI = null;
                    for (Epoch e : epochs) {
                        line = r.readLine();
                        while (!Util.isEmpty(line)) {
                            Scanner scan = new Scanner(line).useDelimiter("\t");
                            VDSpindle s = new VDSpindle(user, Util.addSeconds(e.getStart(), scan.nextDouble()), Util.addSeconds(e.getStart(), scan.nextDouble()));
                            e.addSpindle(s);
                            line = r.readLine();
                        }
                        lastISI = e.setISIs(user, lastISI);
                        epochDAO.put(e);
                    }
                }
            });
            epochDAO.commit();
        } catch (ApplicationException e) {
            DB.abort(txn);
            throw e;
        }
    }

    public long importSessionPartData(String filename, String channel) throws UserException {
        return importSessionPartData(filename, channel, false, false);
    }

    /**
	 * Imports a file with EEG data in the BrainQuick format, in the
	 * Berkeley DB.
	 * There are 5 cases. Import data for:
	 * <ol>
	 * 	<li>A new person and create a new record for her.</li>
	 * 	<li>An existing person and create a new record for her 
	 * (allowDuplicatePerson=true).</li>
	 * 	<li>An existing person, but a new sleep session.</li>
	 * 	<li>An existing person and sleep session, but a new session part.</li>
	 *  <li>An existing person, sleep session and session part, but assume that 
	 *  it is a new session part (allowDuplicateSessionPart=true).
	 * </ol>
	 * 
	 * @param filename
	 * @param allowDuplicatePerson
	 * @param allowDuplicateSessionPart
	 * @throws UserException
	 */
    public long importSessionPartData(String filename, String channel, boolean allowDuplicatePerson, boolean allowDuplicateSessionPart) throws UserException {
        impTxn = DB.beginTransaction();
        log.info("Started import transaction...");
        audit = new Audit();
        try {
            personDAO.setTransaction(impTxn);
            partDAO.setTransaction(impTxn);
            sessionDAO.setTransaction(impTxn);
            epochDAO.setTransaction(impTxn);
            auditDAO.setTransaction(impTxn);
            validateFilename(filename);
            file = new File(filename);
            parser = new EEGParser(file, channel);
            audit.setFilename(FilenameUtils.getName(filename));
            audit.setFileSize((int) file.length() / 1024 / 1024);
            audit.setImportStartTime(new Date());
            audit.setRecordsCount(FileUtil.getRecordsCount(file));
            audit.setSamplingRate(parser.getSamplingRate().value());
            Person p = parsePerson();
            if (!personDAO.exists(p)) {
                return importNewPersonData(p);
            }
            Person personInDB = personDAO.find(p.getFirstName(), p.getLastName());
            if (allowDuplicatePerson) {
                return importNewPersonData(p);
            }
            Date sleepStart = parser.getSessionStartDate();
            SleepSession sleepInDB = sessionDAO.findSleepSession(sleepStart, personInDB.getId());
            if (sleepInDB == null) {
                return importNewSleepSessionData(new SleepSession(sleepStart, parser.getSamplingRate(), personInDB.getId()));
            }
            Date partStart = parser.getPartStartDate();
            SessionPart sessionPart = new SessionPart(partStart, parser.getChannel(), sleepInDB.getId());
            if (!partDAO.existsSessionPart(partStart, sessionPart.getChannel(), sleepInDB.getId())) {
                return importNewSessionPartData(sessionPart);
            }
            if (allowDuplicateSessionPart) {
                return importNewSessionPartData(sessionPart);
            }
        } catch (ApplicationException e) {
            DB.abort(impTxn);
            throw e;
        }
        DB.abort(impTxn);
        throw new UserException(ErrorMessages.FILE_IMPORTED);
    }

    protected long importNewPersonData(Person p) throws UserException {
        personDAO.put(p);
        SleepSession session = new SleepSession(parser.getSessionStartDate(), parser.getSamplingRate(), p.getId());
        return importNewSleepSessionData(session);
    }

    protected long importNewSleepSessionData(SleepSession sleep) throws UserException {
        sessionDAO.put(sleep);
        SessionPart part = new SessionPart(parser.getPartStartDate(), parser.getChannel(), sleep.getId());
        return importNewSessionPartData(part);
    }

    private class Producer implements Runnable {

        private final BlockingQueue<List<Double>> queue;

        private boolean close = false;

        public Producer(BlockingQueue<List<Double>> q) {
            queue = q;
        }

        public void run() {
            try {
                while (!close) {
                    queue.put(produce());
                }
            } catch (UserException e) {
                throw new ApplicationException(e);
            } catch (InterruptedException ex) {
                throw new ApplicationException(ex);
            }
        }

        public List<Double> produce() throws UserException {
            List<Double> data = parser.getNextEpoch();
            if (data != null) {
                return data;
            }
            close = true;
            return new ArrayList<Double>();
        }
    }

    private class Consumer implements Runnable {

        private final BlockingQueue<List<Double>> queue;

        private final Long id;

        private int index = -1;

        private Epoch epoch = null;

        private Interval softLastISI = null;

        private Interval hardLastISI = null;

        private Date end;

        public Consumer(BlockingQueue<List<Double>> q, Long id) {
            queue = q;
            this.id = id;
        }

        public Date getEnd() {
            return end;
        }

        public void run() {
            try {
                while (true) {
                    if (!consume(queue.take())) {
                        break;
                    }
                }
            } catch (InterruptedException ex) {
                throw new ApplicationException(ex);
            }
        }

        public boolean consume(List<Double> samples) {
            index++;
            if (samples.isEmpty()) {
                end = epoch.getEnd();
                return false;
            }
            if (index == 0) {
                epoch = new Epoch(parser.getPartStartDate(), id);
            } else {
                epoch = new Epoch(new Date(epoch.getEnd().getTime()), id);
            }
            epoch.setEEGSamples(samples);
            epoch.detectAndLoadSpindleIndications(parser.getSamplingRate(), group);
            softLastISI = epoch.setISIs(group, true, softLastISI);
            hardLastISI = epoch.setISIs(group, false, hardLastISI);
            epochDAO.put(epoch);
            return true;
        }
    }

    protected long importNewSessionPartData(SessionPart part) throws UserException {
        try {
            partDAO.put(part);
            BlockingQueue<List<Double>> q = new ArrayBlockingQueue<List<Double>>(1);
            Producer p = new Producer(q);
            Consumer c = new Consumer(q, part.getId());
            Thread producer = new Thread(p);
            Thread consumer = new Thread(c);
            producer.start();
            consumer.start();
            producer.join();
            consumer.join();
            part.setEnd(c.getEnd());
            partDAO.put(part);
            audit.setImportEndTime(new Date());
            audit.setEegDuration(part.duration().intValue());
            auditDAO.put(audit);
            DB.commit(impTxn);
            log.info("Commited import transaction...");
            return part.getId();
        } catch (InterruptedException e) {
            throw new ApplicationException(e);
        }
    }

    public SortedSet<Audit> getAudits() {
        return auditDAO.findAllSorted();
    }

    private Person parsePerson() {
        return new Person(parser.getFirstName(), parser.getLastName(), null);
    }
}
