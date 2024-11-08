package org.eaasyst.eaa.data.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import org.eaasyst.eaa.data.DataConnector;
import org.eaasyst.eaa.data.HibernateSessionManager;
import org.eaasyst.eaa.data.UserGroupMemberDab;
import org.eaasyst.eaa.syst.data.persistent.BasicUser;
import org.eaasyst.eaa.syst.data.persistent.UserGroupMember;
import org.eaasyst.eaa.syst.data.transients.SearchSpecification;
import org.eaasyst.eaa.utils.SqlUtils;
import org.eaasyst.eaa.utils.StringUtils;

/**
 * <p>Data accesss bean for User Group information.</p>
 *
 * @version 2.6
 * @author Jeff Chilton
 */
public class HibernateUserGroupMemberDab extends UserGroupMemberDab {

    private SessionFactory sessionFactory = HibernateSessionManager.getHibernateSessionFactory("system");

    /**
	 * <p>Constructs a new "HibernateUserGroupMemberDab" object.</p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public HibernateUserGroupMemberDab() {
        className = StringUtils.computeClassName(getClass());
    }

    /**
	 * <p>Used to execute the key-based "read" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeRead() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.get(BasicUser.class, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
            if (executionResults != null) {
                responseCode = 0;
                responseString = "Execution complete";
            } else {
                responseCode = 1;
                responseString = "Record not found";
            }
            tx.commit();
        } catch (Throwable t) {
            responseCode = 10;
            responseString = t.toString();
            t.printStackTrace();
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        }
    }

    /**
	 * <p>Used to execute the filter-based "read" command after all command
	 * parameters have been set and the command is "ready to excute".</p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeSearch() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find(getQueryStatement("from UserGroupMember as member {filter} {sort}"));
            if (executionResults != null) {
                responseCode = 0;
                responseString = "Execution complete";
                try {
                    Collection emptyTest = (Collection) executionResults;
                    if (emptyTest.isEmpty()) {
                        responseCode = 1;
                        responseString = "Record not found";
                    }
                } catch (ClassCastException e) {
                    ;
                }
            } else {
                responseCode = 1;
                responseString = "Record not found";
            }
            tx.commit();
        } catch (Throwable t) {
            responseCode = 10;
            responseString = t.toString();
            t.printStackTrace();
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        }
    }

    /**
	 * <p>Used to execute the "insert" command after all command parameters
	 * have been set and the command is "ready to excute".</p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeInsert() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            UserGroupMember group = (UserGroupMember) parameters.get(DataConnector.RECORD_PARAMETER);
            session.save(group);
            responseCode = 0;
            responseString = "Execution complete";
            tx.commit();
        } catch (Throwable t) {
            responseCode = 10;
            responseString = t.toString();
            t.printStackTrace();
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        }
    }

    /**
	 * <p>Used to execute the "update" command after all command parameters
	 * have been set and the command is "ready to excute".</p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeUpdate() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            String key = (String) parameters.get(DataConnector.RECORD_KEY_PARAMETER);
            List updated = (List) parameters.get(DataConnector.RECORD_PARAMETER);
            List existing = session.find("from UserGroupMember as member where member.user.idString = '" + key + "'");
            if (updated != null) {
                Iterator i = updated.iterator();
                while (i.hasNext()) {
                    UserGroupMember thisMember = (UserGroupMember) i.next();
                    UserGroupMember matchingMember = null;
                    if (existing != null) {
                        Iterator j = existing.iterator();
                        while (j.hasNext()) {
                            UserGroupMember existingMember = (UserGroupMember) j.next();
                            if (existingMember.getUserGroupId().equalsIgnoreCase(thisMember.getUserGroupId())) {
                                matchingMember = existingMember;
                            }
                        }
                    }
                    if (matchingMember == null) {
                        session.save(thisMember);
                    } else {
                        existing.remove(matchingMember);
                    }
                }
            }
            if (existing != null) {
                Iterator i = existing.iterator();
                while (i.hasNext()) {
                    UserGroupMember existingMember = (UserGroupMember) i.next();
                    session.delete(existingMember);
                }
            }
            tx.commit();
            responseCode = 0;
            responseString = "Execution complete";
        } catch (Throwable t) {
            responseCode = 10;
            responseString = t.toString();
            t.printStackTrace();
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        }
    }

    /**
	 * <p>Used to execute the "delete" command after all command parameters
	 * have been set and the command is "ready to excute".</p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeDelete() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Object toBeDeleted = session.get(UserGroupMember.class, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
            session.delete(toBeDeleted);
            responseCode = 0;
            responseString = "Execution complete";
            tx.commit();
        } catch (Throwable t) {
            responseCode = 10;
            responseString = t.toString();
            t.printStackTrace();
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (Throwable t2) {
                    t2.printStackTrace();
                }
            }
        }
    }

    /**
	 * <p>Returns the query statement in use for this data access bean.</p>
	 * 
	 * @return the query statement in use for this data access bean
	 * @since Eaasy Street 2.2.1
	 */
    private String getQueryStatement(String hqlStatement) {
        if (hqlStatement.indexOf("{key}") != -1) {
            return resolveKeyBasedQuery(hqlStatement);
        } else if (hqlStatement.indexOf("{filter") != -1) {
            return SqlUtils.resolveFilterBasedQuery(hqlStatement, (SearchSpecification) parameters.get("filter"));
        } else {
            return hqlStatement;
        }
    }

    /**
	 * <p>Returns the query statement in use for this data access bean
	 * after parametic replacement of key value.</p>
	 * 
	 * @return the resolved query statement
	 * @since Eaasy Street 2.2.1
	 */
    private String resolveKeyBasedQuery(String hqlStatement) {
        String returnValue = hqlStatement;
        Iterator i = parameters.keySet().iterator();
        while (i.hasNext()) {
            String key = i.next().toString();
            if (key.startsWith(DataConnector.RECORD_KEY_PARAMETER)) {
                String symbolic = "{" + key + "}";
                returnValue = StringUtils.replace(returnValue, symbolic, parameters.get(key).toString());
            }
        }
        return returnValue;
    }
}
