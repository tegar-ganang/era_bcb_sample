package net.mjrz.fm.entity;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import net.mjrz.fm.entity.beans.*;
import net.mjrz.fm.entity.utils.HibernateUtils;
import net.mjrz.fm.entity.utils.IDGenerator;
import net.mjrz.fm.services.SessionManager;
import net.mjrz.fm.ui.panels.PageControlPanel;
import net.mjrz.fm.utils.AlertsCache;
import net.mjrz.fm.utils.MiscUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class AlertsEntityManager {

    private static Logger logger = Logger.getLogger(AlertsEntityManager.class.getName());

    public long addAlert(Alert u) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = u.getId();
            if (id == 0) {
                id = IDGenerator.getInstance().generateId(s);
                u.setId(id);
                s.save(u);
            } else {
                s.update(u);
            }
            s.getTransaction().commit();
            return id;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void saveAlert(Alert alert) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.update(alert);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void updateAlert(Session s, Alert alert) throws Exception {
        try {
            s.save(alert);
        } catch (Exception e) {
            throw e;
        }
    }

    public List getAlerts(Session s, long accountId) throws Exception {
        try {
            String query = "select R from Alert R where R.accountId=? order by R.id";
            Query q = s.createQuery(query);
            q.setLong(0, accountId);
            return q.list();
        } catch (Exception e) {
            throw e;
        }
    }

    public List getAlerts(long accountId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Alert R where R.accountId=? order by R.id";
            Query q = s.createQuery(query);
            q.setLong(0, accountId);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Alert> getAllAlerts() throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Alert R";
            Query q = s.createQuery(query);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Alert getAlert(long id) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Alert R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            return (Alert) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteAlert(long accountid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            String query = "delete from Alert R where R.accountId=?";
            s.beginTransaction();
            Query q = s.createQuery(query);
            q.setLong(0, accountid);
            int r = q.executeUpdate();
            s.getTransaction().commit();
            return r;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        }
    }

    public static final boolean hasAlert(long accountId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select count(*) from Alert R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, accountId);
            Integer count = (Integer) q.uniqueResult();
            return count > 0;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public static final boolean accountAlertRaised(long accountId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Alert R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, accountId);
            List alerts = (List) q.list();
            if (alerts == null || alerts.size() == 0) return false;
            Account a = (Account) s.load(Account.class, Long.valueOf(accountId));
            return checkAlert(s, a, alerts);
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean checkAlert(Session s, Account a, List alerts) {
        try {
            if (alerts == null || alerts.size() == 0) return false;
            Alert alert = (Alert) alerts.get(0);
            if (alert.getConditional().equals(Alert.EXCEEDS)) {
                BigDecimal bal = a.getCurrentBalance();
                BigDecimal alertBal = alert.getAmount();
                if (bal == null || alertBal == null) return false;
                return bal.compareTo(alertBal) >= 0;
            }
            if (alert.getConditional().equals(Alert.FALLS_BELOW)) {
                BigDecimal bal = a.getCurrentBalance();
                BigDecimal alertBal = alert.getAmount();
                if (bal == null || alertBal == null) return false;
                return bal.compareTo(alertBal) <= 0;
            }
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
        }
        return false;
    }

    public static void updateAllAlerts() {
        try {
            FManEntityManager em = new FManEntityManager();
            AlertsEntityManager aem = new AlertsEntityManager();
            List alerts = aem.getAllAlerts();
            if (alerts == null) return;
            for (Object o : alerts) {
                Alert a = (Alert) o;
                if (a.getAmount() == null) {
                    aem.deleteAlert(a.getAccountId());
                    continue;
                }
                Account acct = em.getAccount(SessionManager.getSessionUserId(), a.getAccountId());
                AlertsEntityManager.checkAlert(acct, a);
            }
        } catch (Exception e) {
            Logger log = Logger.getLogger(AlertsEntityManager.class.getName());
            log.error(e);
        }
    }

    private static void updateAlertCache(long accountId, int status) {
        try {
            AlertsCache.getInstance().setAlert(accountId, status);
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
        }
    }

    @SuppressWarnings("unchecked")
    public static void checkAlert(Account account, Alert alert) {
        if (account.getAccountType() == net.mjrz.fm.constants.AccountTypes.ACCT_TYPE_EXPENSE) {
            checkExpenseAlert(account, alert);
            return;
        }
        try {
            if (alert.getConditional().equals(Alert.EXCEEDS)) {
                BigDecimal bal = account.getCurrentBalance();
                BigDecimal alertBal = alert.getAmount();
                if (bal == null || alertBal == null) return;
                if (bal.compareTo(alertBal) >= 0) {
                    alert.setStatus(Alert.ALERT_RAISED);
                    updateAlertCache(account.getAccountId(), Alert.ALERT_RAISED);
                } else {
                    alert.setStatus(Alert.ALERT_CLEARED);
                    updateAlertCache(account.getAccountId(), Alert.ALERT_CLEARED);
                }
            }
            if (alert.getConditional().equals(Alert.FALLS_BELOW)) {
                BigDecimal bal = account.getCurrentBalance();
                BigDecimal alertBal = alert.getAmount();
                if (bal == null || alertBal == null) return;
                if (bal.compareTo(alertBal) <= 0) {
                    alert.setStatus(Alert.ALERT_RAISED);
                    updateAlertCache(account.getAccountId(), Alert.ALERT_RAISED);
                } else {
                    alert.setStatus(Alert.ALERT_CLEARED);
                    updateAlertCache(account.getAccountId(), Alert.ALERT_CLEARED);
                }
            }
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkExpenseAlert(Account a, Alert alert) {
        if (a.getAccountType() != net.mjrz.fm.constants.AccountTypes.ACCT_TYPE_EXPENSE) {
            return;
        }
        try {
            Date[] range = getDateRange(alert.getRange());
            List txList = new FManEntityManager().getTransactionsTo(SessionManager.getSessionUserId(), a.getAccountId(), range[0], range[1]);
            BigDecimal total = new BigDecimal(0d);
            if (txList != null) {
                if (txList.size() == 0) {
                    alert.setStatus(Alert.ALERT_CLEARED);
                    updateAlertCache(a.getAccountId(), Alert.ALERT_CLEARED);
                    return;
                }
                for (Object o : txList) {
                    Transaction t = (Transaction) o;
                    total = total.add(t.getTxAmount());
                }
                if (alert.getConditional().equals(Alert.FALLS_BELOW)) {
                    if (total.compareTo(alert.getAmount()) <= 0) {
                        alert.setStatus(Alert.ALERT_RAISED);
                        updateAlertCache(a.getAccountId(), Alert.ALERT_RAISED);
                    } else {
                        alert.setStatus(Alert.ALERT_CLEARED);
                        updateAlertCache(a.getAccountId(), Alert.ALERT_CLEARED);
                    }
                }
                if (alert.getConditional().equals(Alert.EXCEEDS)) {
                    if (total.compareTo(alert.getAmount()) >= 0) {
                        alert.setStatus(Alert.ALERT_RAISED);
                        updateAlertCache(a.getAccountId(), Alert.ALERT_RAISED);
                    } else {
                        alert.setStatus(Alert.ALERT_CLEARED);
                        updateAlertCache(a.getAccountId(), Alert.ALERT_CLEARED);
                    }
                }
            }
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
        }
    }

    public static Date[] getDateRange(String rangeStr) {
        Date[] ret = new Date[2];
        if (rangeStr.equals(Alert.RANGE[0])) {
            Calendar start = new GregorianCalendar();
            int dayOfWeek = start.get(Calendar.DAY_OF_WEEK) - 1;
            start.add(Calendar.DATE, -1 * dayOfWeek);
            Calendar end = new GregorianCalendar();
            dayOfWeek = end.get(Calendar.DAY_OF_WEEK);
            end.add(Calendar.DATE, (7 - dayOfWeek));
            ret[0] = start.getTime();
            ret[1] = end.getTime();
        }
        if (rangeStr.equals(Alert.RANGE[1])) {
            Calendar start = new GregorianCalendar();
            int dayOfMonth = start.get(Calendar.DATE) - 1;
            start.add(Calendar.DATE, -1 * dayOfMonth);
            Calendar end = new GregorianCalendar();
            dayOfMonth = end.get(Calendar.DATE);
            end.add(Calendar.MONTH, 1);
            end.set(Calendar.DATE, 1);
            end.add(Calendar.DATE, -1);
            ret[0] = start.getTime();
            ret[1] = end.getTime();
        }
        if (rangeStr.equals(Alert.RANGE[2])) {
            Calendar start = new GregorianCalendar();
            ret[0] = start.getTime();
            ret[1] = start.getTime();
        }
        return ret;
    }
}
