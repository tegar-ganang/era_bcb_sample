package net.mjrz.fm.entity;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import net.mjrz.fm.entity.beans.*;
import net.mjrz.fm.entity.utils.*;
import net.mjrz.fm.exceptions.TransactionNotFoundException;
import net.mjrz.fm.constants.*;
import net.mjrz.fm.actions.*;
import net.mjrz.fm.search.*;
import net.mjrz.fm.ui.panels.AccountsTreePanel;
import net.mjrz.fm.utils.*;
import net.mjrz.fm.utils.crypto.CHelper;

/**
 * @author Mjrz contact@mjrz.net
 *
 */
public class FManEntityManager {

    private static Logger logger = Logger.getLogger(FManEntityManager.class.getName());

    public void cleanTT() throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Query deleteQuery = s.createQuery("delete from TT tt");
            int del = deleteQuery.executeUpdate();
            logger.info("Clean up temp tables. Deleted " + del + " rows");
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        }
    }

    public int deleteTT(Session s, long txId) throws Exception {
        try {
            try {
                String query = "delete from TT T where T.txId=?";
                Query q = s.createQuery(query);
                q.setLong(0, txId);
                int r = q.executeUpdate();
                return r;
            } catch (Exception e) {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public void initializeTT(User u) throws Exception {
        Session s = null;
        try {
            long start = System.currentTimeMillis();
            cleanTT();
            List l = getTransactions(u);
            if (l.size() == 0) return;
            List<TT> children = new ArrayList<TT>();
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            int sz = l.size();
            for (int i = 0; i < sz; i++) {
                Transaction t = (Transaction) l.get(i);
                Account from = this.getAccount(s, u.getUid(), t.getFromAccountId());
                Account to = this.getAccount(s, u.getUid(), t.getToAccountId());
                TT tt = new TT();
                Converter bdConverter = new BigDecimalConverter(new BigDecimal(0.0d));
                ConvertUtils.register(bdConverter, BigDecimal.class);
                BeanUtils.copyProperties(tt, t);
                tt.setFromName(from.getAccountName());
                tt.setToName(to.getAccountName());
                if (tt.getParentTxId() != null && tt.getParentTxId().longValue() != 0) {
                    children.add(tt);
                }
                s.save(tt);
                if (i % 20 == 0) {
                    s.flush();
                    s.clear();
                }
            }
            updateParentInfo(s, children);
            String query2 = "select count(*) from TT";
            Query q2 = s.createQuery(query2);
            Long val = Long.valueOf(q2.uniqueResult().toString());
            s.getTransaction().commit();
            long stop = System.currentTimeMillis();
            logger.info("Temp tables initialized in " + (stop - start) + " msecs. Loaded " + val + " rows out of " + sz);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    private void updateParentInfo(Session s, List<TT> children) throws Exception {
        if (children == null || children.size() == 0) return;
        String query = "update TT R set R.isParent=" + TT.IsParent.YES.getVal() + " where R.txId in(<inlist>)";
        int sz = children.size();
        StringBuilder inlist = new StringBuilder();
        for (int i = 0; i < sz; i++) {
            TT t = children.get(i);
            long parentId = t.getParentTxId();
            inlist.append(String.valueOf(parentId));
            if (i < sz - 1) {
                inlist.append(",");
            }
        }
        query = query.replaceAll("<inlist>", inlist.toString());
        logger.info(query);
        Query q = s.createQuery(query);
        int upd = q.executeUpdate();
        logger.info("Updated parent info for " + upd + " transactions");
    }

    public static boolean userExists(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select uid from User R where R.uid=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            Long l = (Long) q.uniqueResult();
            s.getTransaction().commit();
            return l != null;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addUser(User u) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.save(u);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addTransaction(ActionResponse resp, Transaction t) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = IDGenerator.getInstance().generateId(s);
            t.setTxId(id);
            s.save(t);
            s.getTransaction().commit();
            resp.setErrorCode(ActionResponse.NOERROR);
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addAccount(User u, Account a) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = IDGenerator.getInstance().generateId(s);
            a.setAccountId(id);
            s.save(a);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addNetWorthHistory(NetWorthHistory u) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "delete from NetWorthHistory R where R.uid=? and R.date=?";
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            q.setDate(1, u.getDate());
            q.executeUpdate();
            long id = IDGenerator.getInstance().generateId(s);
            u.setId(id);
            s.save(u);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public User getUser(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from User R where R.uid=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            User u = (User) q.uniqueResult();
            s.getTransaction().commit();
            return u;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public User getUser(String uname) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from User R where R.userName=?";
            Query q = s.createQuery(query);
            q.setString(0, uname);
            User u = (User) q.uniqueResult();
            s.getTransaction().commit();
            return u;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Account getAccount(Session s, long uid, long aid) throws Exception {
        try {
            String query = "select R from Account R where R.accountId=? and R.ownerId=?";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            q.setLong(1, uid);
            Account a = (Account) q.uniqueResult();
            return a;
        } catch (Exception e) {
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public List<Account> getAccount(long uid, int acctType, String acctName, String acctNumber) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Account R where R.ownerId=? and " + "R.accountType=? and (R.accountName=? or R.accountNumber=?)";
            if (acctNumber == null || acctNumber.trim().length() == 0) {
                query = "select R from Account R where R.ownerId=? and " + "R.accountType=? and R.accountName=?";
            }
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setInteger(1, acctType);
            q.setParameter(2, acctName);
            if (acctNumber != null && acctNumber.trim().length() > 0) {
                q.setParameter(3, acctNumber);
            }
            List<Account> a = q.list();
            s.getTransaction().commit();
            return a;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    /**
	 * Note:
	 * This and the getAccountFromNumber set the accountName attribute as encrypted string
	 * because these will be called from the XMLProcessor for importing OFX data. 
	 * This is a special case, there is no need to encrypt data since it will be done automatically 
	 */
    public Account getAccount(long uid, String acctName) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Account R where R.ownerId=? and R.accountName=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setString(1, CHelper.encrypt(acctName));
            Account a = (Account) q.uniqueResult();
            s.getTransaction().commit();
            return a;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    /**
	 * Note:
	 * This and the getAccountFromNumber set the accountName attribute as encrypted string
	 * because these will be called from the XMLProcessor for importing OFX data. 
	 * This is a special case, there is no need to encrypt data since it will be done automatically 
	 */
    public Account getAccountFromNumber(long uid, String acctNum) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Account R where R.ownerId=? and R.accountNumber=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setString(1, CHelper.encrypt(acctNum));
            Account a = (Account) q.uniqueResult();
            s.getTransaction().commit();
            return a;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Account getAccountFromName(long uid, int acctType, String acctName) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Account R where R.ownerId=? and R.accountType=? and R.accountName=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setInteger(1, acctType);
            q.setString(2, acctName);
            Account a = (Account) q.uniqueResult();
            s.getTransaction().commit();
            return a;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Account getAccount(long uid, long aid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Account R where R.accountId=? and R.ownerId=?";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            q.setLong(1, uid);
            Account a = (Account) q.uniqueResult();
            s.getTransaction().commit();
            return a;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getAccountNames(User u, int accountType) throws Exception {
        Session s = null;
        try {
            String query = "select A.accountName from Account A where A.ownerId=? and A.accountType=?";
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            q.setInteger(1, accountType);
            List ret = q.list();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getAccountNames(User u) throws Exception {
        return getAccountNames(u.getUid());
    }

    public List getAccountNames(long uid) throws Exception {
        Session s = null;
        try {
            String query = "select A.accountName from Account A where A.ownerId=?";
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            List ret = q.list();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getAccountsForUser(long uid) throws Exception {
        Session s = null;
        try {
            String query = "select R from Account R where R.ownerId=? order by R.accountType, R.accountName";
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getAccountsForUser(User u) throws Exception {
        return getAccountsForUser(u.getUid());
    }

    public List getAccountsForUser(User u, int acctType) throws Exception {
        return getAccountsForUser(u.getUid(), acctType);
    }

    public List getAccountsForUser(long uid, int acctType) throws Exception {
        Session s = null;
        try {
            String query = "select R from Account R where R.ownerId=? and R.accountType=?";
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setInteger(1, acctType);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteAccount(long uid, long accountid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            String query = "delete from Account R where R.accountId=?";
            s.beginTransaction();
            int r = deleteTransactions(s, uid, accountid);
            Query q = s.createQuery(query);
            q.setLong(0, accountid);
            r = q.executeUpdate();
            s.getTransaction().commit();
            return r;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        }
    }

    public int deleteTransactions(Session s, long uid, long acctid) throws Exception {
        try {
            String query = "delete from Transaction T where T.initiatorId=? and T.fromAccountId=? or T.toAccountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setLong(1, acctid);
            q.setLong(2, acctid);
            int r = q.executeUpdate();
            return r;
        } catch (Exception e) {
            throw e;
        }
    }

    public int updateChildrenTx(Session s, long oldTxId, long newTxId) throws Exception {
        try {
            String query = "update Transaction R set R.parentTxId=? where R.parentTxId=?";
            Query q = s.createQuery(query);
            q.setLong(0, newTxId);
            q.setLong(1, oldTxId);
            int r = q.executeUpdate();
            return r;
        } catch (Exception e) {
            throw e;
        }
    }

    public int updateChildrenTT(Session s, long oldTxId, long newTxId) throws Exception {
        try {
            String query = "update TT R set R.parentTxId=? where R.parentTxId=?";
            Query q = s.createQuery(query);
            q.setLong(0, newTxId);
            q.setLong(1, oldTxId);
            int r = q.executeUpdate();
            return r;
        } catch (Exception e) {
            throw e;
        }
    }

    public Integer getNumChildren(Session s, Long txId) throws Exception {
        try {
            String query = "select count(R.txId) from Transaction R where R.parentTxId=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            Integer count = (Integer) q.uniqueResult();
            return count;
        } catch (Exception e) {
            throw e;
        }
    }

    public int deleteTransaction(Session s, long txId) throws Exception {
        try {
            String query = "delete from Transaction T where T.txId=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            int r = q.executeUpdate();
            return r;
        } catch (Exception e) {
            throw e;
        }
    }

    public List getTransactions(User u) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? order by R.txDate desc";
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int getTransactionsCount(long uid, long accountId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select count(R.txId) from Transaction R " + "where R.initiatorId=? and (R.fromAccountId=? or R.toAccountId=?)";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setLong(1, accountId);
            q.setLong(2, accountId);
            int count = (Integer) q.uniqueResult();
            return count;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getNetWorthHistory(User u, String from, String to) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from NetWorthHistory R where R.uid=? and R.date >= ? and R.date <= ? order by R.date";
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            q.setString(1, from);
            q.setString(2, to);
            List l = q.list();
            for (int i = 0; i < l.size(); i++) {
                NetWorthHistory hist = (NetWorthHistory) l.get(i);
            }
            return l;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getTransactions(User u, Date dt, int status) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? and R.txDate <= ? and R.txStatus=? order by R.txDate desc";
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            q.setTimestamp(1, dt);
            q.setInteger(2, status);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getTransactionsTo(long uid, long accountId, Date fromDate, Date toDate) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? and R.txDate >= ? and R.txDate <= ?" + "and R.toAccountId=? order by R.txDate desc";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setTimestamp(1, fromDate);
            q.setTimestamp(2, toDate);
            q.setLong(3, accountId);
            return q.list();
        } catch (Exception e) {
            throw e;
        }
    }

    public List getTransactions(User u, Date fromDate, Date toDate, int status) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? and R.txDate >= ? and R.txDate <= ?" + "and R.txStatus=? order by R.txDate desc";
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            q.setTimestamp(1, fromDate);
            q.setTimestamp(2, toDate);
            q.setInteger(3, status);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Transaction> getTransactionsForAccount(long userId, long accountId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? and " + "(R.fromAccountId=? or R.toAccountId=?) order by R.txDate desc";
            Query q = s.createQuery(query);
            q.setLong(0, userId);
            q.setLong(1, accountId);
            q.setLong(2, accountId);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void updateTransactionAmount(long txId, BigDecimal val) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Transaction t = (Transaction) s.load(Transaction.class, Long.valueOf(txId));
            TT tt = (TT) s.load(TT.class, Long.valueOf(txId));
            t.setTxAmount(val);
            tt.setTxAmount(val);
            s.update(t);
            s.update(tt);
            s.getTransaction().commit();
        } catch (Exception e) {
            logger.error(MiscUtils.stackTrace2String(e));
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Transaction getTransaction(User u, long txId) throws Exception {
        return getTransaction(u.getUid(), txId);
    }

    public Transaction getTransaction(long uid, long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? and R.txId=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setLong(1, txId);
            return (Transaction) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List<Transaction> getChildTransactions(User u, long txId) throws Exception {
        return getChildTransactions(u.getUid(), txId);
    }

    @SuppressWarnings("unchecked")
    public List<Transaction> getChildTransactions(long uid, long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Transaction R where R.initiatorId=? and R.parentTxId=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setLong(1, txId);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public List<TT> getChildTT(long uid, long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from TT R where R.initiatorId=? and R.parentTxId=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setLong(1, txId);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public boolean fitIdExists(Session s, long userid, long fromAcctId, long toAcctId, String fitid) throws Exception {
        try {
            String query = "select R.txId from Transaction R where R.initiatorId=? " + "and (R.fromAccountId=? or R.toAccountId=?) and R.fitid=?";
            Query q = s.createQuery(query);
            q.setLong(0, userid);
            q.setLong(1, fromAcctId);
            q.setLong(2, toAcctId);
            q.setString(3, fitid);
            Long txid = (Long) q.uniqueResult();
            return txid != null;
        } catch (Exception e) {
            throw e;
        }
    }

    public Transaction getTransaction(Session s, User u, long txId) throws Exception {
        try {
            String query = "select R from Transaction R where R.initiatorId=? and R.txId=?";
            Query q = s.createQuery(query);
            q.setLong(0, u.getUid());
            q.setLong(1, txId);
            return (Transaction) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        }
    }

    public List getAccountCategories(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from AccountCategory R where R.uid=? order by R.categoryId";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getChildrenAccountCategories(long uid, long catid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from AccountCategory R where R.uid=? and R.parentCategoryId=? order by R.categoryId";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setLong(1, catid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Long getNextAccountCategoryId() throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Long id = IDGenerator.getInstance().generateId(s);
            s.getTransaction().commit();
            return id;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public boolean addAccountCategory(AccountCategory c) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            if (c.getCategoryId() == null) {
                long id = IDGenerator.getInstance().generateId(s);
                c.setCategoryId(id);
            }
            java.io.Serializable ret = s.save(c);
            System.out.println(c.getCategoryId() + ":" + c.getCategoryName() + " %% ");
            s.getTransaction().commit();
            return ret != null;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void updateAccountCategory(AccountCategory c) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            if (c.getCategoryId() == null) throw new NullPointerException();
            s.update(c);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public boolean deleteAccountCategory(AccountCategory c) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            boolean result = true;
            String query = "delete from AccountCategory R where R.categoryId=? and R.uid=?";
            Query q = s.createQuery(query);
            q.setLong(0, c.getCategoryId());
            q.setLong(1, c.getUid());
            int r = q.executeUpdate();
            if (r != 1) {
                s.getTransaction().rollback();
            } else {
                s.getTransaction().commit();
            }
            return result;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public boolean deleteAccountCategory(ArrayList<AccountCategory> list) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            boolean result = true;
            String query = "delete from AccountCategory R where R.categoryId=? and R.uid=?";
            Query q = s.createQuery(query);
            for (AccountCategory c : list) {
                int u = updateAccountForCategory(s, c.getCategoryId(), c.getParentCategoryId());
                if (u == 0) continue;
                q.setLong(0, c.getCategoryId());
                q.setLong(1, c.getUid());
                int r = q.executeUpdate();
                if (r != 1) {
                    result = false;
                    break;
                }
            }
            if (result) s.getTransaction().commit(); else {
                s.getTransaction().rollback();
            }
            return result;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    private int updateAccountForCategory(Session s, Long oldcategory, Long newcategory) throws Exception {
        try {
            if (oldcategory == null || newcategory == null) return -1;
            String query = "update Account R set R.categoryId=? where R.categoryId=?";
            Query q = s.createQuery(query);
            q.setLong(0, newcategory);
            q.setLong(1, oldcategory);
            int r = q.executeUpdate();
            return r;
        } catch (Exception e) {
            throw e;
        }
    }

    public int updateAccountForCategory(Long accountId, Long newcategory) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            if (accountId == null || newcategory == null) return -1;
            String query = "update Account R set R.categoryId=? where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, newcategory);
            q.setLong(1, accountId);
            int r = q.executeUpdate();
            s.getTransaction().commit();
            return r;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void updateAccountNumber(Long accountid, String anumber) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            Account a = (Account) s.load(Account.class, Long.valueOf(accountid));
            a.setAccountNumber(anumber);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static boolean isCategoryPopulated(long catid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.accountId from Account R where R.categoryId=?";
            Query q = s.createQuery(query);
            q.setLong(0, catid);
            List l = q.list();
            s.getTransaction().commit();
            if (l == null) return false;
            return l.size() != 0;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static String getAccountNameFromId(long aid, boolean closeSession) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.accountName from Account R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            String aname = (String) q.uniqueResult();
            return aname;
        } catch (Exception e) {
            throw e;
        } finally {
            if (closeSession) if (s != null) HibernateUtils.closeSession();
        }
    }

    public static String getAccountName(long aid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.accountName from Account R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            String aname = (String) q.uniqueResult();
            s.getTransaction().commit();
            return aname;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static String getCategoryName(long aid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.categoryName from AccountCategory R where R.categoryId=(select A.categoryId from Account A where A.accountId=?)";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            String aname = (String) q.uniqueResult();
            s.getTransaction().commit();
            return aname;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<AccountCategory> getRootCategoies() throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from AccountCategory R where R.parentCategoryId=?";
            Query q = s.createQuery(query);
            q.setLong(0, -1);
            List<AccountCategory> ret = (List<AccountCategory>) q.list();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static String getAccountNumber(long aid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.accountNumber from Account R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            String anum = (String) q.uniqueResult();
            s.getTransaction().commit();
            return anum;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static boolean accountExists(long uid, int acctType, String acctName) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.accountId from Account R where R.ownerId=? and R.accountType=? and R.accountName=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setInteger(1, acctType);
            q.setString(2, acctName);
            Long a = (Long) q.uniqueResult();
            s.getTransaction().commit();
            return a != null;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static double getCurrentBalance(long aid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.currentBalance from Account R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, aid);
            double aname = (Double) q.uniqueResult();
            s.getTransaction().commit();
            return aname;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addContact(Contact u, boolean existing) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            if (existing) {
                deleteContact(s, u.getId());
            }
            long id = IDGenerator.getInstance().generateId(s);
            u.setId(id);
            s.save(u);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteContact(long id) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            int r = deleteContact(s, id);
            s.getTransaction().commit();
            return r;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        }
    }

    private int deleteContact(Session s, long id) throws Exception {
        try {
            String query = "delete from Contact R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            return q.executeUpdate();
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        }
    }

    public List getContacts(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Contact R where R.userId=? order by R.fullName";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Contact getContact(long cid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Contact R where R.id=? order by R.fullName";
            Query q = s.createQuery(query);
            q.setLong(0, cid);
            return (Contact) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addONLB(ONLBDetails u, boolean existing) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            if (existing) {
                deleteOnlb(s, u.getId());
            }
            long id = IDGenerator.getInstance().generateId(s);
            u.setId(id);
            s.save(u);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    private int deleteOnlb(Session s, long id) throws Exception {
        try {
            String query = "delete from ONLBDetails R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            int r = q.executeUpdate();
            return r;
        } catch (Exception e) {
            s.getTransaction().rollback();
            throw e;
        }
    }

    public ONLBDetails getONLBFromAccountId(long acctid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from ONLBDetails R where R.accountId=?";
            Query q = s.createQuery(query);
            q.setLong(0, acctid);
            ONLBDetails a = (ONLBDetails) q.uniqueResult();
            return a;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public ONLBDetails getONLB(long id) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from ONLBDetails R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            return (ONLBDetails) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getPortfolio(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Portfolio R where R.uid=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public long addPortfolio(Portfolio u) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = IDGenerator.getInstance().generateId(s);
            u.setId(id);
            s.save(u);
            s.getTransaction().commit();
            return id;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public long addPortfolioEntry(PortfolioEntry u) throws Exception {
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

    public int deletePortfolioEntry(PortfolioEntry u) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            String query = "delete from PortfolioEntry R where R.id=?";
            s.beginTransaction();
            Query q = s.createQuery(query);
            q.setLong(0, u.getId());
            int r = q.executeUpdate();
            s.getTransaction().commit();
            return r;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getPortfolioEntries(long pid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from PortfolioEntry R where R.portfolioId=? order by R.id";
            Query q = s.createQuery(query);
            q.setLong(0, pid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public PortfolioEntry getPortfolioEntry(long id) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from PortfolioEntry R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            return (PortfolioEntry) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Long getPortfolioEntryExists(long pid, String name) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R.id from PortfolioEntry R where R.portfolioId=? and R.name=?";
            Query q = s.createQuery(query);
            q.setLong(0, pid);
            q.setString(1, name);
            s.getTransaction().commit();
            return (Long) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public PortfolioEntry getPortfolioEntry(long pid, String name) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from PortfolioEntry R where R.portfolioId=? and R.name=?";
            Query q = s.createQuery(query);
            q.setLong(0, pid);
            q.setString(1, name);
            return (PortfolioEntry) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteFutureTransaction(long id) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "delete from FutureTransaction R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            int ret = q.executeUpdate();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List getFutureTransaction(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from FutureTransaction R where R.uid=? and R.nextRunDate = ? order by R.id";
            GregorianCalendar today = new GregorianCalendar();
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            q.setDate(1, today.getTime());
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List listFutureTransactions(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select F.id, A.accountName as from, " + "B.accountName as to, T.txAmount, F.nextRunDate, " + "F.endDate  from FutureTransaction F,  " + "Account A, Account B, Transaction T  " + "where F.uid=? and F.transactionId=T.txId and " + "T.fromAccountId = A.accountId and T.toAccountId=B.accountId";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addFavourite(Favourites fav) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = IDGenerator.getInstance().generateId(s);
            fav.setId(id);
            s.save(fav);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteFavourites(long id) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "delete from Favourites R where R.id=?";
            Query q = s.createQuery(query);
            q.setLong(0, id);
            int ret = q.executeUpdate();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List<Favourites> getFavourites(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Favourites R where R.uid=? order by R.name";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            return q.list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public TxDecorator getTxDecorator(long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from TxDecorator R where R.txId=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            return (TxDecorator) q.uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public boolean isParent(long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select count(R) from TT R where R.parentTxId=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            List l = q.list();
            return l.size() != 0;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addTxDecorator(TxDecorator dec) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.saveOrUpdate(dec);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteTxDecorator(long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "delete from TxDecorator R where R.txId=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            int r = q.executeUpdate();
            s.getTransaction().commit();
            return r;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addBudget(Budget b) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = IDGenerator.getInstance().generateId(s);
            b.setId(id);
            s.save(b);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void updateBudget(Budget b) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.update(b);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void deleteBudget(Budget b) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.delete(b);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public List<Budget> getBudgets(long uid) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from Budget R where R.uid=?";
            Query q = s.createQuery(query);
            q.setLong(0, uid);
            List<Budget> l = Collections.checkedList(q.list(), Budget.class);
            s.getTransaction().commit();
            return l;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<AttachmentRef> getAttachmentId(long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from AttachmentRef R where R.key=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            List<AttachmentRef> ret = q.list();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public static TT getTT(long txId) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String query = "select R from TT R where R.txId=?";
            Query q = s.createQuery(query);
            q.setLong(0, txId);
            TT tt = (TT) q.uniqueResult();
            s.getTransaction().commit();
            return tt;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void addObject(String tableName, FManEntity entity) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            long id = IDGenerator.getInstance().generateId(s);
            entity.setPK(id);
            s.save(entity);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public void updateObject(String tableName, FManEntity entity) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            s.update(entity);
            s.getTransaction().commit();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public int deleteObject(String name, String filter) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String q = "delete from " + name + " P ";
            if (filter != null) {
                q = q + " where P." + filter;
            }
            Query query = s.createQuery(q);
            int ret = query.executeUpdate();
            s.getTransaction().commit();
            return ret;
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    @SuppressWarnings("unchecked")
    public List<Object> getObjects(String name, String filter) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String q = "select P from " + name + " P ";
            if (filter != null) {
                q = q + " where P." + filter;
            }
            return s.createQuery(q).list();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }

    public Object getObject(String name, String filter) throws Exception {
        Session s = null;
        try {
            s = HibernateUtils.getSessionFactory().getCurrentSession();
            s.beginTransaction();
            String q = "select P from " + name + " P ";
            if (filter != null) {
                q = q + " where P." + filter;
            }
            return s.createQuery(q).uniqueResult();
        } catch (Exception e) {
            throw e;
        } finally {
            if (s != null) HibernateUtils.closeSession();
        }
    }
}
