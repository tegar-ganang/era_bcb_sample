package bioinfo.comaWebServer.dataServices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import nu.localhost.tapestry.acegi.services.SaltSourceService;
import org.acegisecurity.providers.encoding.Md5PasswordEncoder;
import org.acegisecurity.providers.encoding.PasswordEncoder;
import org.apache.tapestry5.grid.ColumnSort;
import org.apache.tapestry5.grid.SortConstraint;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import bioinfo.comaWebServer.dataManagement.JobStatus;
import bioinfo.comaWebServer.entities.Alignment;
import bioinfo.comaWebServer.entities.AlignmentFilter;
import bioinfo.comaWebServer.entities.Autocorrection;
import bioinfo.comaWebServer.entities.Cluster;
import bioinfo.comaWebServer.entities.ComaParams;
import bioinfo.comaWebServer.entities.ComaResults;
import bioinfo.comaWebServer.entities.EmailNotification;
import bioinfo.comaWebServer.entities.GapProbability;
import bioinfo.comaWebServer.entities.InformationCorrection;
import bioinfo.comaWebServer.entities.Job;
import bioinfo.comaWebServer.entities.Masking;
import bioinfo.comaWebServer.entities.Output;
import bioinfo.comaWebServer.entities.DatabaseItem;
import bioinfo.comaWebServer.entities.PeriodicalWorkerParams;
import bioinfo.comaWebServer.entities.ProfileConstruction;
import bioinfo.comaWebServer.entities.ResultsAlignment;
import bioinfo.comaWebServer.entities.SEG;
import bioinfo.comaWebServer.entities.Search;
import bioinfo.comaWebServer.entities.User;
import bioinfo.comaWebServer.enums.ParamType;
import bioinfo.comaWebServer.services.InitSessionFactory;
import bioinfo.comaWebServer.services.SaltSourceImpl;

public class HibernateDataSource<IList> implements IDataSource {

    private static final Random generator = new Random();

    private final Logger logger = LoggerFactory.getLogger(HibernateDataSource.class);

    private static final String JOB_TABLE = " Job ";

    private static final String USER_TABLE = " User ";

    private static final String DATABASE_ITEM_TABLE = " DatabaseItem ";

    private static final String OUTPUT_TABLE = " Output ";

    private static final String PROFILE_CONSTRUCTION_TABLE = " ProfileConstruction ";

    private static final String MASKING_TABLE = " Masking ";

    private static final String SEG_TABLE = " SEG ";

    private static final String ALIGNMENT_TABLE = " Alignment ";

    private static final String GAP_PROBALBILITY_TABLE = " GapProbability ";

    private static final String AUTOCORRECTION_TABLE = " Autocorrection ";

    private static final String INFORMATION_CORRECTION_TABLE = " InformationCorrection ";

    private static final String SEARCH_TABLE = " Search ";

    private static final String CLUSTER_TABLE = " Cluster ";

    private static final String PERIODICAL_WORKER_TABLE = " PeriodicalWorkerParams ";

    private static final String EMAIL_NOTIFICATION_TABLE = " EmailNotification ";

    private static final String ALIGNMENT_FILTER_TABLE = " AlignmentFilter ";

    private static final String RESULTS_ALIGNMENT_TABLE = " ResultsAlignment ";

    private static final String COMA_RESULTS_TABLE = " ComaResults ";

    private static final String SELECT_OUTPUT = "select o from Output as o";

    private static final String SELECT_ROFILE_CONSTRUCTION = "select o from ProfileConstruction as o";

    private static final String SELECT_MASKING = "select o from Masking as o";

    private static final String SELECT_SEG = "select o from SEG as o";

    private static final String SELECT_ALIGNMENT = "select o from Alignment as o";

    private static final String SELECT_GAP_PROBALBILITY = "select o from GapProbability as o";

    private static final String SELECT_AUTOCORRECTION = "select o from Autocorrection as o";

    private static final String SELECT_INFORMATION_CORRECTION = "select o from InformationCorrection as o";

    private static final String SELECT_SEARCH = "select o from Search as o";

    private static final String SELECT_CLUSTER = "select o from Cluster as o";

    private static final String SELECT_EMAIL_NOTIFICATION = "select o from EmailNotification as o";

    private static final String SELECT_ALIGNMENT_FILTER = "select o from AlignmentFilter as o";

    private final PasswordEncoder passwordEncoder;

    private final SaltSourceService saltSource;

    public HibernateDataSource() {
        passwordEncoder = new Md5PasswordEncoder();
        saltSource = new SaltSourceImpl();
    }

    public ComaParams getComaParams() {
        ComaParams comaParams = new ComaParams();
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.beginTransaction();
        List<AlignmentFilter> alignmentFilterList = session.createQuery(SELECT_ALIGNMENT_FILTER).list();
        if (alignmentFilterList.size() > 0) {
            comaParams.setAlignmentFilter(alignmentFilterList.get(0));
        }
        List<Alignment> alignmentList = session.createQuery(SELECT_ALIGNMENT).list();
        if (alignmentList.size() > 0) {
            comaParams.setAlignment(alignmentList.get(0));
        }
        List<GapProbability> gapProbabilityList = session.createQuery(SELECT_GAP_PROBALBILITY).list();
        if (gapProbabilityList.size() > 0) {
            comaParams.setGapProbability(gapProbabilityList.get(0));
        }
        List<Masking> maskingList = session.createQuery(SELECT_MASKING).list();
        if (maskingList.size() > 0) {
            comaParams.setMasking(maskingList.get(0));
        }
        List<SEG> segList = session.createQuery(SELECT_SEG).list();
        if (segList.size() > 0) {
            comaParams.setSeg(segList.get(0));
        }
        List<Output> outputList = session.createQuery(SELECT_OUTPUT).list();
        if (outputList.size() > 0) {
            comaParams.setOutput(outputList.get(0));
        }
        List<Autocorrection> autocorrectionList = session.createQuery(SELECT_AUTOCORRECTION).list();
        if (autocorrectionList.size() > 0) {
            comaParams.setAutocorrection(autocorrectionList.get(0));
        }
        List<InformationCorrection> informationCorrectionList = session.createQuery(SELECT_INFORMATION_CORRECTION).list();
        if (informationCorrectionList.size() > 0) {
            comaParams.setInformationCorrection(informationCorrectionList.get(0));
        }
        List<ProfileConstruction> profileConstructionList = session.createQuery(SELECT_ROFILE_CONSTRUCTION).list();
        if (profileConstructionList.size() > 0) {
            comaParams.setProfileConstruction(profileConstructionList.get(0));
        }
        transaction.commit();
        return comaParams;
    }

    public void update(ComaParams comaParams) {
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.beginTransaction();
        session.update(comaParams.getAlignment());
        session.update(comaParams.getAlignmentFilter());
        session.update(comaParams.getGapProbability());
        session.update(comaParams.getSeg());
        session.update(comaParams.getMasking());
        session.update(comaParams.getOutput());
        session.update(comaParams.getProfileConstruction());
        session.update(comaParams.getInformationCorrection());
        session.update(comaParams.getAutocorrection());
        transaction.commit();
    }

    public Search getSearchParams() {
        Search search = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + SEARCH_TABLE + " o where o.type =:type");
            query.setString("type", ParamType.ADMIN.getType());
            search = (Search) query.uniqueResult();
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
        }
        return search;
    }

    public String updateParams(Search search) {
        String info = "Search: OK";
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.update(search);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
            info = "Search: Failed";
        }
        return info;
    }

    public PeriodicalWorkerParams getPeriodicalWorkerParams() {
        PeriodicalWorkerParams periodicalWorkerParams = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            List<PeriodicalWorkerParams> clusterList = session.createQuery("select o from " + PERIODICAL_WORKER_TABLE + " as o").list();
            if (clusterList.size() > 0) {
                periodicalWorkerParams = clusterList.get(0);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
        }
        return periodicalWorkerParams;
    }

    public String updateParams(PeriodicalWorkerParams periodicalWorkerParams) {
        String info = "Updated successfully!";
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(periodicalWorkerParams);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
            info = "Updating failed!";
        }
        return info;
    }

    public Cluster getClusterParams() {
        Cluster cluster = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            List<Cluster> clusterList = session.createQuery(SELECT_CLUSTER).list();
            if (clusterList.size() > 0) {
                cluster = clusterList.get(0);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
        }
        return cluster;
    }

    public String updateParams(Cluster cluster) {
        String info = "Updated successfully!";
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(cluster);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
            info = "Updating failed!";
        }
        return info;
    }

    public EmailNotification getEmailNotificationParams() {
        EmailNotification emailNotification = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            List<EmailNotification> emailNotificationList = session.createQuery(SELECT_EMAIL_NOTIFICATION).list();
            if (emailNotificationList.size() > 0) {
                emailNotification = emailNotificationList.get(0);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                    logger.debug("Error rolling back transaction");
                }
                throw e;
            }
        }
        return emailNotification;
    }

    public void updateParams(EmailNotification emailNotification) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(emailNotification);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void saveOrUpdate(DatabaseItem db) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(db);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void delete(DatabaseItem db) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.delete(db);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void deletePSIBlastDatabase(long id) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query q = session.createQuery("delete from " + DATABASE_ITEM_TABLE + " as s where s.id =:id");
            q.setLong("id", id);
            q.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public DatabaseItem getDatabase(long id) {
        DatabaseItem database = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + DATABASE_ITEM_TABLE + " o  where o.id =:id");
            query.setLong("id", id);
            database = (DatabaseItem) query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return database;
    }

    public List<DatabaseItem> getDatabases(String type) {
        List<DatabaseItem> db = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + DATABASE_ITEM_TABLE + " o  where o.type =:type");
            query.setString("type", type);
            db = query.list();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return db;
    }

    public void saveOrUpdate(User user) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            user.setPassword(passwordEncoder.encodePassword(user.getPassword(), saltSource.getSalt(user)));
            session.saveOrUpdate(user);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void delete(User user) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.delete(user);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public ComaResults getComaResultsById(long id) throws Exception {
        ComaResults comaResults = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + COMA_RESULTS_TABLE + " o  where o.id =:id");
            query.setLong("id", id);
            comaResults = (ComaResults) query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return comaResults;
    }

    public void update(Job job) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.update(job);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void save(Job job) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.save(job);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void delete(Job job) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            session.delete(job);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public void deleteJob(long id) {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query q = session.createQuery("delete from " + JOB_TABLE + " as o where o.id = :id");
            q.setLong("id", id);
            q.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
    }

    public Job getJob(long id) {
        Job job = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + JOB_TABLE + " o  where o.id =:id");
            query.setLong("id", id);
            job = (Job) query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return job;
    }

    public List<Job> getJobs() {
        List<Job> jobs = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + JOB_TABLE + " o ");
            jobs = query.list();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return jobs;
    }

    public Job getJobByGeneratedIdORDescription(String info) throws Exception {
        Job job = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + JOB_TABLE + " o " + "where o.generatedId =:info OR o.description =:info");
            query.setString("info", info);
            job = (Job) query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return job;
    }

    public Job getJobByGeneratedId(String id) {
        Job job = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + JOB_TABLE + " o where o.generatedId =:id");
            query.setString("id", id);
            job = (Job) query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return job;
    }

    public Job registerJob(String prefix) {
        Job job = new Job();
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            String generatedId = null;
            Job jobInDB = null;
            do {
                generatedId = prefix + generator.nextInt(Integer.MAX_VALUE);
                Query query = session.createQuery("from " + JOB_TABLE + " o where o.generatedId =:id");
                query.setString("id", generatedId);
                jobInDB = (Job) query.uniqueResult();
            } while (jobInDB != null);
            job.setGeneratedId(String.valueOf(generatedId));
            session.save(job);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return job;
    }

    public Set<Job> getNotFinishedJobs() {
        Set<Job> jobs = new HashSet<Job>();
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + JOB_TABLE + " o where " + "o.status != :finished AND " + "o.status != :errors AND " + "o.status != :canceled");
            query.setString("finished", JobStatus.FINISHED.getStatus());
            query.setString("errors", JobStatus.ERRORS.getStatus());
            query.setString("canceled", JobStatus.CANCELED.getStatus());
            List jobList = query.list();
            for (Object o : jobList) {
                jobs.add((Job) o);
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return jobs;
    }

    public List<Job> getJobs(int start, int end, List<SortConstraint> sortConstraints) throws Exception {
        List<Job> jobs = new ArrayList<Job>();
        if (end < start) {
            return jobs;
        }
        StringBuffer buffer = new StringBuffer("from " + JOB_TABLE + " as o ");
        if (sortConstraints.size() > 0) {
            buffer.append(" order by ");
        } else {
            buffer.append(" order by o.expirationDate desc ");
        }
        for (SortConstraint c : sortConstraints) {
            buffer.append(" o." + c.getPropertyModel().getPropertyName());
            if (c.getColumnSort() == ColumnSort.ASCENDING) {
                buffer.append(" asc ");
            } else {
                buffer.append(" desc ");
            }
        }
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery(buffer.toString());
            query.setFirstResult(start);
            query.setMaxResults(end - start + 1);
            jobs = (List<Job>) query.list();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return jobs;
    }

    public Set<Job> getExpiredJobs() {
        Set<Job> jobs = new HashSet<Job>();
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + JOB_TABLE + " o where " + "o.expirationDate <= NOW() OR " + "o.status = :canceled");
            query.setString("canceled", JobStatus.CANCELED.getStatus());
            List jobList = query.list();
            for (Object o : jobList) {
                jobs.add((Job) o);
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return jobs;
    }

    public String jobStatus(String generatedId) throws Exception {
        String status = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("select o.status from " + JOB_TABLE + " o where o.generatedId = :generatedId");
            query.setString("generatedId", generatedId);
            List<String> stats = query.list();
            if (stats != null && stats.size() > 0) {
                status = stats.get(0);
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return status;
    }

    public ResultsAlignment getResultsAlignment(long id) {
        ResultsAlignment resultsAlignment = null;
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            Query query = session.createQuery("from " + RESULTS_ALIGNMENT_TABLE + " o where o.id =:id");
            query.setLong("id", id);
            resultsAlignment = (ResultsAlignment) query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) transaction.rollback();
            throw e;
        }
        return resultsAlignment;
    }

    public void initializeSystem() {
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            if (getObjectNumber(session, OUTPUT_TABLE) == 0) {
                session.save(new Output());
            }
            if (getObjectNumber(session, PROFILE_CONSTRUCTION_TABLE) == 0) {
                session.save(new ProfileConstruction());
            }
            if (getObjectNumber(session, MASKING_TABLE) == 0) {
                session.save(new Masking());
            }
            if (getObjectNumber(session, ALIGNMENT_TABLE) == 0) {
                session.save(new Alignment());
            }
            if (getObjectNumber(session, SEG_TABLE) == 0) {
                session.save(new SEG());
            }
            if (getObjectNumber(session, GAP_PROBALBILITY_TABLE) == 0) {
                session.save(new GapProbability());
            }
            if (getObjectNumber(session, AUTOCORRECTION_TABLE) == 0) {
                session.save(new Autocorrection());
            }
            if (getObjectNumber(session, INFORMATION_CORRECTION_TABLE) == 0) {
                session.save(new InformationCorrection());
            }
            if (getObjectNumber(session, SEARCH_TABLE) == 0) {
                session.save(new Search(ParamType.ADMIN.getType()));
            }
            if (getObjectNumber(session, ALIGNMENT_FILTER_TABLE) == 0) {
                session.save(new AlignmentFilter());
            }
            if (getObjectNumber(session, PERIODICAL_WORKER_TABLE) == 0) {
                session.save(new PeriodicalWorkerParams());
            }
            if (getObjectNumber(session, USER_TABLE) == 0) {
                User ud = new User();
                ud.setUsername("admin");
                ud.setPassword(passwordEncoder.encodePassword("admin", saltSource.getSalt(ud)));
                ud.addRole("ROLE_USER");
                ud.addRole("ROLE_ADMIN");
                session.save(ud);
            }
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                }
            }
        }
    }

    public long runningJobsNumber() {
        Set<Job> set = getNotFinishedJobs();
        return set.size();
    }

    public Long jobNumber() throws Exception {
        Long jobNumber = new Long(0);
        Transaction transaction = null;
        Session session = InitSessionFactory.getInstance().getCurrentSession();
        try {
            transaction = session.beginTransaction();
            jobNumber = getObjectNumber(session, JOB_TABLE);
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction != null && transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (HibernateException e1) {
                }
            }
        }
        return jobNumber;
    }

    private Long getObjectNumber(Session session, String tableName) {
        String queryStr = "select count(*) from " + tableName;
        Query query = session.createQuery(queryStr);
        List list = query.list();
        return (Long) list.get(0);
    }
}
