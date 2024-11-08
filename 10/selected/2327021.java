package org.eaasyst.eaa.data.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import org.eaasyst.eaa.data.DataConnector;
import org.eaasyst.eaa.data.HibernateSessionManager;
import org.eaasyst.eaa.data.UserGroupDab;
import org.eaasyst.eaa.syst.data.persistent.UserGroup;
import org.eaasyst.eaa.syst.data.transients.SearchSpecification;
import org.eaasyst.eaa.utils.SqlUtils;
import org.eaasyst.eaa.utils.StringUtils;

/**
 * <p>Data accesss bean for User Group information.</p>
 *
 * @version 2.6.3
 * @author Jeff Chilton
 */
public class HibernateUserGroupDab extends UserGroupDab {

    private SessionFactory sessionFactory = HibernateSessionManager.getHibernateSessionFactory("system");

    /**
	 * <p>Constructs a new "HibernateUserGroupDab" object.</p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public HibernateUserGroupDab() {
        className = StringUtils.computeClassName(getClass());
    }

    /**
	 * <p>Used to execute the key-based "read" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 *
	 * @since	Eaasy Street 2.0.4
	 */
    public void executeRead() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.get(UserGroup.class, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
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
	 * @since Eaasy Street 2.0.4
	 */
    public void executeSearch() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find(getQueryStatement("select new org.eaasyst.eaa.syst.data.persistent.UserGroup(userGroup.idString, userGroup.title) from UserGroup userGroup {filter} {sort}"));
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
	 * @since	Eaasy Street 2.0.4
	 */
    public void executeInsert() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            UserGroup group = (UserGroup) parameters.get(DataConnector.RECORD_PARAMETER);
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
	 * @since	Eaasy Street 2.0.4
	 */
    public void executeUpdate() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            UserGroup group = (UserGroup) session.load(UserGroup.class, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
            group.update((UserGroup) parameters.get(DataConnector.RECORD_PARAMETER));
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
	 * <p>Used to execute the "delete" command after all command parameters
	 * have been set and the command is "ready to excute".</p>
	 *
	 * @since	Eaasy Street 2.0.4
	 */
    public void executeDelete() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Object toBeDeleted = session.get(UserGroup.class, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
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
	 * <p>Used to execute the "getGroupIdsAndNames" command after all
	 * command parameters have been set and the command is "ready
	 * to excute".</p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeGetGroupIdsAndNames() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find(getQueryStatement("select new org.apache.struts.util.LabelValueBean(ug.title, ug.idString) from UserGroup as ug order by ug.title, ug.idString"));
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
	 * <p>Returns the query statement in use for this data access bean.</p>
	 * 
	 * @return	the query statement in use for this data access bean
	 * @since	Eaasy Street 2.0.4
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
	 * @return	the resolved query statement
	 * @since	Eaasy Street 2.0.4
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
