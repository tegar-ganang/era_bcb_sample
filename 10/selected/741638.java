package lichen.internal.job;

import java.util.Date;
import lichen.entities.user.OnlineUser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 检查在线用户的job.
 * <p>供Quartz调用.
 * @author jcai
 * @version $Revision: 138 $
 * @since 0.0.1
 */
public class CheckOnline implements Job {

    /**
	 * Logger for this class
	 */
    private static final Log logger = LogFactory.getLog(CheckOnline.class);

    private static final long MAX_ONLINE_TIME = 20 * 60 * 1000;

    /**
	 * 
	 * @see org.quartz.Job#execute(org.quartz.JobExecutionContext)
	 */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        SessionFactory sessionFactory = (SessionFactory) context.getJobDetail().getJobDataMap().get(SchedulerJobModule.HIBERNATE_SESSION_SOURCE);
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Date date = new Date();
            long time = date.getTime();
            Query q = session.getNamedQuery(OnlineUser.DELETE_TIMEOUT_USERS_QUERY);
            q.setLong(0, time - MAX_ONLINE_TIME);
            int rows = q.executeUpdate();
            if (logger.isDebugEnabled()) {
                logger.debug("delete online users [" + rows + "]");
            }
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            transaction.rollback();
        } finally {
            if (session.isConnected()) {
                session.close();
            }
        }
    }
}
