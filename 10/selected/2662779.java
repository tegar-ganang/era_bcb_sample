package org.cyberaide.queue;

import java.util.List;
import java.util.Date;
import org.cyberaide.core.CoGObject;
import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.Query;
import org.hibernate.Transaction;

/**
 * TaskPoolOperator class provides interface with MySQL database using Hibernate
 */
public class TaskPoolOperator {

    private static final SessionFactory sessionFactory;

    static {
        sessionFactory = new Configuration().configure().buildSessionFactory();
    }

    /**
     * constructor
     */
    public TaskPoolOperator() {
    }

    /**
     * insert a task into the TASKS table
     *
     * @param cogTask: task object
     */
    public void add(CoGObject cogTask) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            session.beginTransaction();
            Task task = new Task();
            task.setTypename(cogTask.get("typename"));
            task.setJobid(cogTask.get("jobid"));
            task.setInputfile(cogTask.get("inputfile"));
            task.setOutputfile(cogTask.get("outputfile"));
            task.setErrorfile(cogTask.get("errorfile"));
            task.setSuspended(cogTask.get("suspended"));
            task.setJobtype(cogTask.get("job.type"));
            task.setResource(cogTask.get("resource"));
            task.setExecutable(cogTask.get("executable"));
            task.setDirectory(cogTask.get("directory"));
            task.setMpi(cogTask.get("mpi"));
            task.setWalltime(cogTask.get("walltime"));
            task.setTimestamp(new Date());
            session.save(task);
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * set the suspended field to 'yes'
     *
     * @param jobid: task id
     */
    public void setSuspended(String jobid) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query update = session.createQuery("update Task task " + "set task.suspended = :suspended" + " where task.jobid = :jobid");
            update.setParameter("suspended", "yes");
            update.setParameter("jobid", jobid);
            update.executeUpdate();
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * set the suspended field to NULL
     *
     * @param jobid: task id
     */
    public void clearSuspended(String jobid) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query update = session.createQuery("update Task task " + "set task.suspended = :suspended" + " where task.jobid = :jobid");
            update.setParameter("suspended", "NULL");
            update.setParameter("jobid", jobid);
            update.executeUpdate();
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * set the replicated field to 'yes'
     *
     * @param jobid: task id
     */
    public void setReplicated(String jobid) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query update = session.createQuery("update Task task " + "set task.replicated = :replicated" + " where task.jobid = :jobid");
            update.setParameter("replicated", "yes");
            update.setParameter("jobid", jobid);
            update.executeUpdate();
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * set the replicated field to NULL
     *
     * @param jobid: task id
     */
    public void clearReplicated(String jobid) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query update = session.createQuery("update Task task " + "set task.replicated = :replicated" + " where task.jobid = :jobid");
            update.setParameter("replicated", "NULL");
            update.setParameter("jobid", jobid);
            update.executeUpdate();
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * get a task based on the FIFO policy
     *
     * @return task object (CoGObject)
     */
    public CoGObject getFirst() {
        CoGObject task = null;
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query select = session.createQuery("select task.typename," + "task.jobid," + "task.inputfile," + "task.outputfile," + "task.errorfile," + "task.suspended," + "task.jobtype," + "task.resource," + "task.executable," + "task.directory," + "task.mpi," + "task.walltime " + "from Task task " + "where task.timestamp = " + "(select min(timestamp) " + "from Task task " + "where task.suspended is null)");
            List result = select.list();
            task = getTask((Object[]) result.get(0));
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return task;
    }

    /**
     * create a replica
     *
     * @return: task object (CoGObject)
     */
    public CoGObject getReplica() {
        CoGObject task = null;
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query select = session.createQuery("select task.typename," + "task.jobid," + "task.inputfile," + "task.outputfile," + "task.errorfile," + "task.suspended," + "task.jobtype," + "task.resource," + "task.executable," + "task.directory," + "task.mpi," + "task.walltime " + "from Task task " + "where task.timestamp = " + "(select min(timestamp) " + "from Task task " + "where task.suspended is null " + "and task.replicated is null)");
            List result = select.list();
            task = getTask((Object[]) result.get(0));
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return task;
    }

    /**
     * get a task with the given id
     *
     * @param jobid: task id
     * @return task object (CoGObject)
     */
    public CoGObject get(String jobid) {
        CoGObject task = null;
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query select = session.createQuery("select task.typename," + "task.jobid," + "task.inputfile," + "task.outputfile," + "task.errorfile," + "task.suspended," + "task.jobtype," + "task.resource," + "task.executable," + "task.directory," + "task.mpi," + "task.walltime " + "from Task task " + "where task.jobid = :jobid");
            select.setParameter("jobid", jobid);
            List result = select.list();
            task = getTask((Object[]) result.get(0));
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return task;
    }

    /**
     * get a random task
     *
     * @return task object (CoGObject)
     */
    public CoGObject getRandom() {
        CoGObject task = null;
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query select = session.createQuery("select task.typename," + "task.jobid," + "task.inputfile," + "task.outputfile," + "task.errorfile," + "task.suspended," + "task.jobtype," + "task.resource," + "task.executable," + "task.directory," + "task.mpi," + "task.walltime " + "from Task task " + "order by rand()");
            select.setMaxResults(1);
            List result = select.list();
            task = getTask((Object[]) result.get(0));
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return task;
    }

    /**
     * remove a task with the given id
     *
     * @param jobid: task id
     */
    public void delete(String jobid) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query delete = session.createQuery("delete from Task task" + " where task.jobid = :jobid");
            delete.setParameter("jobid", jobid);
            int row = delete.executeUpdate();
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
    }

    /**
     * check if task pool (table) is empty
     *
     * @return true if there is no task
     *         false if there is at least one task
     */
    public boolean isEmpty() {
        boolean empty = true;
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        try {
            Query select = session.createQuery("select count(*) from Task task");
            List result = select.list();
            if (Integer.parseInt(result.get(0).toString()) > 0) {
                empty = false;
            }
            session.flush();
            session.clear();
            tx.commit();
        } catch (RuntimeException e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            session.close();
        }
        return empty;
    }

    /**
     * retrieve task attributes 
     *
     * @param columns: an array of fields obtained from a query
     * @return task object (CoGObject)
     */
    private CoGObject getTask(Object[] columns) {
        CoGObject task = null;
        task = new CoGObject();
        String value;
        value = (columns[0] != null) ? columns[0].toString() : null;
        task.set("typename", value);
        value = (columns[1] != null) ? columns[1].toString() : null;
        task.set("jobid", value);
        value = (columns[2] != null) ? columns[2].toString() : null;
        task.set("inputfile", value);
        value = (columns[3] != null) ? columns[3].toString() : null;
        task.set("outputfile", value);
        value = (columns[4] != null) ? columns[4].toString() : null;
        task.set("errorfile", value);
        value = (columns[5] != null) ? columns[5].toString() : null;
        task.set("suspended", value);
        value = (columns[6] != null) ? columns[6].toString() : null;
        task.set("job.type", value);
        value = (columns[7] != null) ? columns[7].toString() : null;
        task.set("resource", value);
        value = (columns[8] != null) ? columns[8].toString() : null;
        task.set("executable", value);
        value = (columns[9] != null) ? columns[9].toString() : null;
        task.set("directory", value);
        value = (columns[10] != null) ? columns[10].toString() : null;
        task.set("mpi", value);
        value = (columns[11] != null) ? columns[11].toString() : null;
        task.set("walltime", value);
        return task;
    }
}
