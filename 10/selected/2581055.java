package org.gridbus.broker.common;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.gridbus.broker.common.security.UserCredential;
import org.gridbus.broker.constants.Constants;
import org.gridbus.broker.constants.JobStatus;
import org.gridbus.broker.constants.JobType;
import org.gridbus.broker.constants.ServiceType;
import org.gridbus.broker.event.BrokerEvent;
import org.gridbus.broker.exceptions.GridBrokerException;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.gridbus.scs.common.SCSJob;

public final class BrokerStorage {

    /**
	 * Logger for this class
	 */
    private static final Logger logger = Logger.getLogger(BrokerStorage.class);

    private static BrokerStorage store;

    private SessionFactory sf;

    private CredentialStorage cstore;

    private BrokerStorage() throws GridBrokerException {
        super();
        try {
            logger.info("Initialising Broker Storage...");
            Configuration cfg = new Configuration();
            cfg.configure(Constants.BROKER_HIBERNATE_CONFIG);
            long start = System.currentTimeMillis();
            sf = cfg.buildSessionFactory();
            long end = System.currentTimeMillis();
            logger.info("Done: " + (end - start) + " ms.");
            cstore = new CredentialStorage();
        } catch (Exception e) {
            e.printStackTrace();
            throw new GridBrokerException("Error creating storage instance.", e);
        }
    }

    /**
	 * @param config
	 * @return store object
	 */
    static final synchronized BrokerStorage initialise() throws GridBrokerException {
        if (store == null) {
            store = new BrokerStorage();
        }
        return store;
    }

    /**
	 * Shutdown the broker store
	 */
    static final synchronized void shutdown() {
        if (store != null) {
            try {
                if (store.sf != null && !store.sf.isClosed()) {
                    store.sf.getStatistics().logSummary();
                    store.sf.close();
                }
            } catch (Exception ex1) {
            }
            store = null;
        }
    }

    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
	 * @return instance of the broker store
	 * @throws GridBrokerException
	 */
    static final synchronized BrokerStorage getInstance() throws GridBrokerException {
        if (store == null) {
            throw new GridBrokerException("Storage not initialised");
        } else {
            return store;
        }
    }

    /**
	 * Returns the total number of jobs with the given status, on the given server.
	 * If no status is given, count of jobs with all statuses are returned.
	 * If no serverID is given, count of jobs on all servers (including unassigned jobs) is given.
	 * @param applicationID 
	 * @param jobStatus
	 * @param jobType 
	 * @param serverName
	 * @return job count
	 * @throws GridBrokerException 
	 */
    public long getJobCount(String applicationID, int jobStatus, int jobType, String serverName) throws GridBrokerException {
        long count = 0;
        String query = "SELECT count(job) FROM Job as job " + "WHERE job.application.id = '" + applicationID + "'";
        if (jobStatus != JobStatus.STATUS_ANY) {
            query += " AND job.status=" + jobStatus;
        }
        if (jobType != JobType.TYPE_ANY) {
            query += " AND job.type=" + jobType;
        }
        if (serverName != Constants.ANY_SERVER && serverName.length() > 0) {
            query += " AND job.service.name = '" + serverName + "'";
        }
        Object result = getUniqueResult(query);
        count = ((Integer) result).longValue();
        return count;
    }

    public long getSCSJobCount(String applicationID, int jobStatus, int jobType, String serverName) throws GridBrokerException {
        long count = 0;
        String query = "SELECT count(job) FROM SCSJob as job " + "WHERE job.application.id = '" + applicationID + "'";
        if (jobStatus != JobStatus.STATUS_ANY) {
            query += " AND job.status=" + jobStatus;
        }
        if (jobType != JobType.TYPE_ANY) {
            query += " AND job.type=" + jobType;
        }
        if (serverName != Constants.ANY_SERVER && serverName.length() > 0) {
            query += " AND job.service.name = '" + serverName + "'";
        }
        Object result = getUniqueResult(query);
        count = ((Integer) result).longValue();
        return count;
    }

    /**
	 * @param appId
	 * @return Qos object
	 * @throws GridBrokerException 
	 */
    public Qos getQos(String appId) throws GridBrokerException {
        String hql = "SELECT app.qos FROM ApplicationContext app WHERE app.id='" + appId + "'";
        Qos qos = (Qos) getUniqueResult(hql);
        return qos;
    }

    /**
	 * @param applicationID
	 * @return application object
	 * @throws GridBrokerException 
	 */
    public ApplicationContext getApplication(String applicationID) throws GridBrokerException {
        ApplicationContext app = null;
        app = (ApplicationContext) getObject(ApplicationContext.class, applicationID);
        return app;
    }

    /**
     * @param username 
     * @param appname 
     * @return application object
     * @throws GridBrokerException 
     */
    public ApplicationContext getApplication(String username, String appname) throws GridBrokerException {
        ApplicationContext app = null;
        List apps = null;
        String hql = "FROM ApplicationContext app WHERE app.username='" + username + "' AND app.name='" + appname + "' ORDER BY app.startTime DESC";
        apps = getList(hql);
        if (apps != null && apps.size() > 0) app = (ApplicationContext) apps.get(0);
        return app;
    }

    /**
	 * 
	 * @param applicationID
	 * @param jobName
	 * @return job object
	 * @throws GridBrokerException
	 */
    public Job getJob(String applicationID, String jobName) throws GridBrokerException {
        Job job = null;
        String hql = "FROM Job j WHERE j.application.id='" + applicationID + "' AND j.name='" + jobName + "'";
        job = (Job) getUniqueResult(hql);
        return job;
    }

    /**
	 * 
	 * @param applicationID
	 * @param jobName
	 * @return job object
	 * @throws GridBrokerException
	 */
    public SCSJob getSCSJob(String applicationID, String jobName) throws GridBrokerException {
        SCSJob job = null;
        String hql = "FROM SCSJob j WHERE j.application.id='" + applicationID + "' AND j.name='" + jobName + "'";
        job = (SCSJob) getUniqueResult(hql);
        return job;
    }

    /**
	 * 
	 * @param applicationID
	 * @param serviceType 
	 * @param name
	 * @return service object
	 * @throws GridBrokerException 
	 */
    public Service getService(String applicationID, int serviceType, String name) throws GridBrokerException {
        List services = getServices(applicationID, serviceType, " name = '" + name + "'");
        Service service = (Service) services.get(0);
        return service;
    }

    /**
	 * 
	 * @param applicationID
	 * @param serviceType 
	 * @param name
	 * @return service object
	 * @throws GridBrokerException 
	 */
    public Service getServiceByMappingId(String applicationID, int serviceType, String mappingId) throws GridBrokerException {
        List services = getServices(applicationID, serviceType, " mappingID = '" + mappingId + "'");
        Service service = (Service) services.get(0);
        return service;
    }

    /**
	 * @param applicationId
	 * @return
	 * @throws GridBrokerException
	 */
    List getReadyServers(String applicationId) throws GridBrokerException {
        String hqlQuery = "FROM ComputeServer cs WHERE cs.available=true AND cs.applicationID='" + applicationId + "' AND cs.jobLimit > (cs.jobStats.TotalJobs - (cs.jobStats.DoneJobs + cs.jobStats.FailedJobs))";
        return getList(hqlQuery);
    }

    /**
	 * @param applicationID
	 * @param serviceType 
	 * @param filter : an expression (in hql) to filter the data
	 * @return list of servers.
	 * @throws GridBrokerException 
	 */
    public List getServices(String applicationID, int serviceType, String filter) throws GridBrokerException {
        List services = null;
        String query = null;
        switch(serviceType) {
            case ServiceType.APPLICATION:
                query = "FROM ApplicationService s WHERE s.applicationID='" + applicationID + "'";
                break;
            case ServiceType.COMPUTE:
                query = "FROM ComputeServer s WHERE s.applicationID='" + applicationID + "'";
                break;
            case ServiceType.DATAHOST:
                query = "FROM DataHost s WHERE s.applicationID='" + applicationID + "'";
                break;
            case ServiceType.INFORMATION:
                query = "FROM InformationService s WHERE s.applicationID='" + applicationID + "'";
                break;
            case ServiceType.ANY:
                query = "FROM Service s WHERE s.applicationID='" + applicationID + "'";
                break;
        }
        if (filter != null && filter.length() > 0) {
            query += " AND (" + filter + ")";
        }
        services = getList(query);
        return services;
    }

    /**
	 * @param applicationID
	 * @param jobStatus (STATUS_ANY means any status)
	 * @param jobType (TYPE_ANY means any type)
	 * @param serverName (Constants.ANY_SERVER signifies any server)
	 * @return collection of jobs
	 * @throws GridBrokerException 
	 */
    public List getJobs(String applicationID, int jobStatus, int jobType, String serverName) throws GridBrokerException {
        List jobs = null;
        String query = "FROM Job as job " + "WHERE job.application.id = '" + applicationID + "'";
        if (jobStatus != JobStatus.STATUS_ANY) {
            query += " AND job.status=" + jobStatus;
        }
        if (jobType != JobType.TYPE_ANY) {
            query += " AND job.type=" + jobType;
        }
        if (serverName != Constants.ANY_SERVER && serverName.length() > 0) {
            query += " AND job.service.name = '" + serverName + "'";
        }
        jobs = getList(query);
        return jobs;
    }

    public List getSCSJobs(String applicationID, int jobStatus, int jobType, String serverName) throws GridBrokerException {
        List jobs = null;
        String query = "FROM SCSJob as job " + "WHERE job.application.id = '" + applicationID + "'";
        if (jobStatus != JobStatus.STATUS_ANY) {
            query += " AND job.status=" + jobStatus;
        }
        if (jobType != JobType.TYPE_ANY) {
            query += " AND job.type=" + jobType;
        }
        if (serverName != Constants.ANY_SERVER && serverName.length() > 0) {
            query += " AND job.service.name = '" + serverName + "'";
        }
        jobs = getList(query);
        return jobs;
    }

    public List getSCSJobs(String applicationID, int[] jobStates, int jobType, String serverName) throws GridBrokerException {
        System.err.println("in getSCSJobs");
        List jobs = null;
        String query = "FROM SCSJob as job " + "WHERE job.application.id = '" + applicationID + "'";
        StringBuffer filter = new StringBuffer("");
        for (int i = 0; i < jobStates.length; i++) {
            int jobStatus = jobStates[i];
            if (filter.length() != 0) filter.append(" OR ");
            filter.append("job.status=" + jobStatus);
        }
        if (filter.length() != 0) query += " AND (" + filter.toString() + ")";
        if (jobType != JobType.TYPE_ANY) {
            query += " AND job.type=" + jobType;
        }
        if (serverName != Constants.ANY_SERVER && serverName.length() > 0) {
            query += " AND job.service.name = '" + serverName + "'";
        }
        System.err.println("Sent Query: " + query);
        jobs = getList(query);
        return jobs;
    }

    /**
	 * @param applicationID
	 * @param jobStates
	 * @param jobType 
	 * @param serverName
	 * @return collection of jobs
	 * @throws GridBrokerException
	 */
    public List getJobs(String applicationID, int[] jobStates, int jobType, String serverName) throws GridBrokerException {
        List jobs = null;
        String query = "FROM Job as job " + "WHERE job.application.id = '" + applicationID + "'";
        StringBuffer filter = new StringBuffer("");
        for (int i = 0; i < jobStates.length; i++) {
            int jobStatus = jobStates[i];
            if (filter.length() != 0) filter.append(" OR ");
            filter.append("job.status=" + jobStatus);
        }
        if (filter.length() != 0) query += " AND (" + filter.toString() + ")";
        if (jobType != JobType.TYPE_ANY) {
            query += " AND job.type=" + jobType;
        }
        if (serverName != Constants.ANY_SERVER && serverName.length() > 0) {
            query += " AND job.service.name = '" + serverName + "'";
        }
        jobs = getList(query);
        return jobs;
    }

    /**
	 * @param j
	 * @throws GridBrokerException 
	 */
    public void saveJob(Job j) throws GridBrokerException {
        saveObject(j);
    }

    public void saveJob(SCSJob j) throws GridBrokerException {
        System.err.println("SaveJOB - SCSJob");
        saveObject(j);
    }

    /**
	 * @param savedJob
	 * @throws GridBrokerException
	 */
    public void removeJob(Job savedJob) throws GridBrokerException {
        deleteObject(savedJob);
    }

    /**
	 * @param s
	 * @throws GridBrokerException
	 */
    public void saveService(Service s) throws GridBrokerException {
        saveObject(s);
    }

    /**
	 * @param app
	 * @throws GridBrokerException
	 */
    public void saveApplication(ApplicationContext app) throws GridBrokerException {
        saveObject(app);
    }

    /**
	 * Updates the qos for a given application
	 * @param applicationId 
	 * @param qos
	 * @throws GridBrokerException
	 */
    public void saveQos(String applicationId, Qos qos) throws GridBrokerException {
        ApplicationContext app = getApplication(applicationId);
        app.setQos(qos);
        saveObject(app);
    }

    /**
	 * Saves the network link entity for a given application
	 * @param applicationID
	 * @param nl
	 * @throws GridBrokerException
	 */
    public void saveNetworkLink(String applicationID, NetworkLink nl) throws GridBrokerException {
        saveObject(nl);
    }

    /**
	 * Gets an integer count returned by the given HQL query.
	 * An exception is thrown if the query doesnot return an int value
	 * @param hql
	 * @return count
	 * @throws GridBrokerException 
	 */
    public int getCount(String hql) throws GridBrokerException {
        Integer i = (Integer) getUniqueResult(hql);
        return i.intValue();
    }

    /**
	 * 
	 * @param hqlQuery
	 * @return list
	 * @throws GridBrokerException
	 */
    public List getList(String hqlQuery) throws GridBrokerException {
        return getResultList(hqlQuery, 0, 0);
    }

    /**
	 * @param hqlQuery
	 * @param limit
	 * @return list
	 * @throws GridBrokerException
	 */
    public List getList(String hqlQuery, int limit) throws GridBrokerException {
        return getResultList(hqlQuery, 0, limit);
    }

    /**
	 * Returns a list of entities specified by the given HQL query,
	 * Limits the results to "limit". If limit = 0, all results are 
	 * returned.
	 * @param hqlQuery
	 * @param limit
	 * @return list
	 * @throws GridBrokerException
	 */
    private synchronized List getResultList(String hqlQuery, int pageNumber, int limit) throws GridBrokerException {
        List result = null;
        Transaction t = null;
        Session s = null;
        try {
            s = getSession();
            t = s.beginTransaction();
            Query q = s.createQuery(hqlQuery);
            if (limit > 0) {
                q.setMaxResults(limit);
            }
            if (pageNumber > 0) {
                q.setFirstResult(pageNumber);
            }
            result = q.list();
            t.commit();
        } catch (Exception e) {
            if (t != null && t.isActive()) {
                t.rollback();
            }
            throw new GridBrokerException("Error getting list: query=" + hqlQuery, e);
        } finally {
            closeSession(s);
        }
        return result;
    }

    /**
	 * @return
	 * @throws HibernateException
	 */
    private Session getSession() throws HibernateException {
        Session session = sf.getCurrentSession();
        return session;
    }

    /**
	 * @param s
	 */
    private void closeSession(Session s) {
        if (s != null && s.isOpen()) s.close();
    }

    /**
	 * Gets appropriate user-credentials from the in-memory collection of credentials.
	 * @param applicationId
	 * @param uc
	 */
    public void addCredentials(String applicationId, UserCredential uc) {
        synchronized (cstore) {
            cstore.addCredentials(applicationId, uc);
        }
    }

    /**
	 * Returns the appropriate credentials from the in-memory credential store
	 * for the given service entity
	 * @param applicationId
	 * @param s
	 * @return UserCredential
	 */
    public UserCredential getCredentialsForService(String applicationId, Service s) {
        UserCredential uc = null;
        synchronized (cstore) {
            uc = cstore.getCredentialsForService(applicationId, s);
        }
        return uc;
    }

    private synchronized void deleteObject(Object o) throws GridBrokerException {
        synchronized (o) {
            Transaction t = null;
            Session s = null;
            try {
                s = getSession();
                t = s.beginTransaction();
                s.delete(o);
                t.commit();
            } catch (Exception e) {
                if (t != null && t.isActive()) {
                    t.rollback();
                }
                throw new GridBrokerException("Error deleting object: " + e.getMessage(), e);
            } finally {
                closeSession(s);
            }
        }
    }

    synchronized void saveObject(Object o) throws GridBrokerException {
        synchronized (o) {
            Transaction t = null;
            Session s = null;
            try {
                s = getSession();
                t = s.beginTransaction();
                s.saveOrUpdate(o);
                t.commit();
            } catch (Exception e) {
                if (t != null && t.isActive()) {
                    t.rollback();
                }
                e.printStackTrace();
                throw new GridBrokerException("Error saving object: " + e.getMessage(), e);
            } finally {
                closeSession(s);
            }
        }
    }

    private synchronized Object getObject(Class objClass, Serializable id) throws GridBrokerException {
        Object result = null;
        Session s = null;
        Transaction t = null;
        try {
            s = getSession();
            t = s.beginTransaction();
            result = s.get(objClass, id);
            t.commit();
        } catch (Exception e) {
            if (t != null && t.isActive()) t.rollback();
            throw new GridBrokerException("Error getting object:" + id, e);
        } finally {
            closeSession(s);
        }
        return result;
    }

    private synchronized Object getUniqueResult(String hqlQuery) throws GridBrokerException {
        Object result = null;
        Transaction t = null;
        Session s = null;
        try {
            s = getSession();
            t = s.beginTransaction();
            result = s.createQuery(hqlQuery).uniqueResult();
            t.commit();
        } catch (Exception e) {
            if (t != null && t.isActive()) {
                t.rollback();
            }
            throw new GridBrokerException("Error getting unique result: query=" + hqlQuery, e);
        } finally {
            closeSession(s);
        }
        return result;
    }

    private synchronized void executeUpdate(String hql) throws GridBrokerException {
        Transaction t = null;
        Session s = null;
        try {
            s = getSession();
            t = s.beginTransaction();
            s.createQuery(hql).executeUpdate();
            t.commit();
        } catch (Exception e) {
            if (t != null && t.isActive()) {
                t.rollback();
            }
            throw new GridBrokerException("Error executing query: " + hql, e);
        } finally {
            closeSession(s);
        }
    }

    /**
	 * @param applicationId
	 * @param credentialID
	 * @param serviceMappingID
	 */
    public void addCredentialMappings(String applicationId, String credentialID, String serviceMappingID) {
        synchronized (cstore) {
            cstore.addCredentialMappings(applicationId, credentialID, serviceMappingID);
        }
    }

    /**
	 * @param evt
	 */
    public void saveEvent(BrokerEvent evt) throws GridBrokerException {
        saveObject(evt);
    }

    /**
     * @param serverName
     * @return job statistics
     * @throws GridBrokerException 
     */
    public JobStatistics getJobStatistics(String applicationId, String serverName) throws GridBrokerException {
        return getJobStatistics(applicationId, serverName, JobType.USER);
    }

    /**
	 * @param serverName
	 * @return job statistics
	 * @throws GridBrokerException 
	 */
    public JobStatistics getJobStatistics(String applicationId, String serverName, int jobType) throws GridBrokerException {
        JobStatistics stats = new JobStatistics();
        String hql = "SELECT job.status, count(job) FROM Job job WHERE job.application.id='" + applicationId + "' AND job.type=" + jobType;
        if (serverName != Constants.ANY_SERVER && !serverName.trim().equals("")) {
            hql += " AND job.service.name='" + serverName + "'";
        }
        hql += " GROUP BY job.status";
        List results = getList(hql);
        for (Iterator it = results.iterator(); it.hasNext(); ) {
            Object[] obj = (Object[]) it.next();
            if (obj.length == 2) {
                int status = ((Integer) obj[0]).intValue();
                int count = ((Integer) obj[1]).intValue();
                switch(status) {
                    case JobStatus.ACTIVE:
                        stats.setActiveJobs(count);
                        break;
                    case JobStatus.DONE:
                        stats.setDoneJobs(count);
                        break;
                    case JobStatus.FAILED:
                        stats.setFailedJobs(count);
                        break;
                    case JobStatus.PENDING:
                        stats.setPendingJobs(count);
                        break;
                    case JobStatus.READY:
                        stats.setReadyJobs(count);
                        break;
                    case JobStatus.SCHEDULED:
                        stats.setScheduledJobs(count);
                        break;
                    case JobStatus.STAGE_IN:
                        stats.setStageInJobs(count);
                        break;
                    case JobStatus.STAGE_OUT:
                        stats.setStageOutJobs(count);
                        break;
                    case JobStatus.SUBMITTED:
                        stats.setSubmittedJobs(count);
                        break;
                    case JobStatus.UNKNOWN:
                        stats.setUnknownJobs(count);
                        break;
                }
            }
        }
        stats.setTotalJobs(stats.getActiveJobs() + stats.getDoneJobs() + stats.getFailedJobs() + stats.getPendingJobs() + stats.getReadyJobs() + stats.getScheduledJobs() + stats.getStageInJobs() + stats.getStageOutJobs() + stats.getSubmittedJobs() + stats.getUnknownJobs());
        return stats;
    }

    /**
	 * @param applicationID
	 * @param pageNumber
	 * @param pageSize
	 * @return
	 * @throws GridBrokerException 
	 */
    public Collection getUserJobsPaged(String applicationID, int pageNumber, int pageSize) throws GridBrokerException {
        String hql = "FROM Job job WHERE job.application.id='" + applicationID + "' AND job.type=" + JobType.USER;
        return getResultList(hql, pageNumber, pageSize);
    }

    /**
	 * @param applicationID
	 * @return
	 * @throws GridBrokerException
	 */
    public List getWorkflowTasks(String applicationID) throws GridBrokerException {
        String hql = "FROM WfTask wft WHERE wft.applicationID='" + applicationID + "'";
        List tasks = getList(hql);
        return tasks;
    }

    /**
	 * @param applicationID
	 * @return
	 * @throws GridBrokerException
	 */
    public List getDataConstraints(String applicationID) throws GridBrokerException {
        String hql = "FROM DataConstraint dc WHERE dc.applicationID='" + applicationID + "'";
        List dcList = getList(hql);
        return dcList;
    }

    public Service getServiceByHostname(String applicationID, String hostname) throws GridBrokerException {
        List services = getServices(applicationID, ServiceType.COMPUTE, " hostname = '" + hostname + "'");
        Service service = null;
        if (services.size() != 0) {
            service = (Service) services.get(0);
        }
        return service;
    }
}
