package org.eaasyst.eaa.data.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import org.eaasyst.eaa.data.DataAccessBeanBase;
import org.eaasyst.eaa.data.DataConnector;
import org.eaasyst.eaa.syst.EaasyStreet;
import org.eaasyst.eaa.syst.data.PersistentDataBean;
import org.eaasyst.eaa.syst.data.transients.SearchSpecification;
import org.eaasyst.eaa.utils.SqlUtils;
import org.eaasyst.eaa.utils.StringUtils;

/**
 * <p>Generic Hibernate data accesss bean.</p>
 *
 * @version 2.9.1
 * @author Jeff Chilton
 * @author Peter Ruan
 */
public class HibernateDataAccessBean extends DataAccessBeanBase {

    private SessionFactory sessionFactory = null;

    private String hqlStatement = null;

    private Class targetClass = null;

    /**
	 * <p>Constructs a new "HibernateDataAccessBean" object.</p>
	 * 
	 * @param sessionFactory the Hibernate session factory
	 * @param version the class name of the primary class
	 * @since Eaasy Street 2.0.1
	 */
    public HibernateDataAccessBean(SessionFactory sessionFactory, String version) {
        this(sessionFactory, version, "from " + version + (((PersistentDataBean) EaasyStreet.getInstance(version)).isIdAString() ? " as db order by db.idString" : " as db order by db.id"));
    }

    /**
	 * <p>Constructs a new "HibernateDataAccessBean" object.</p>
	 * 
	 * @param sessionFactory the Hibernate session factory
	 * @param version the class name of the primary class
	 * @param hqlStatement the HQL to use for the find command
	 * @since Eaasy Street 2.0.1
	 */
    public HibernateDataAccessBean(SessionFactory sessionFactory, String version, String hqlStatement) {
        super();
        className = StringUtils.computeClassName(getClass());
        this.sessionFactory = sessionFactory;
        this.hqlStatement = hqlStatement;
        try {
            targetClass = Class.forName(version);
        } catch (Throwable t) {
            try {
                targetClass = EaasyStreet.getInstance(version).getClass();
            } catch (Throwable t2) {
                t2.printStackTrace();
            }
        }
    }

    /**
	 * <p>Used to execute the specified command after all command
	 * parameters have been set and the command is "ready to excute".</p>
	 * 
	 * @since Eaasy Street 2.0.1
	 */
    public void execute() {
        EaasyStreet.logTrace("[In] HibernateDataAccessBean.execute()");
        EaasyStreet.logTrace("execute(): command = " + command);
        if (isReadyToExecute()) {
            if (DataConnector.READ_COMMAND.equalsIgnoreCase(command)) {
                if (parameters.get("useFindCommand") != null) {
                    executeFind();
                } else if (parameters.get(DataConnector.RECORD_KEY_PARAMETER) != null) {
                    executeRead();
                } else {
                    executeFind();
                }
            } else if (command.equals(DataConnector.INSERT_COMMAND)) {
                executeInsert();
            } else if (command.equals(DataConnector.UPDATE_COMMAND)) {
                executeUpdate();
            } else if (command.equals(DataConnector.DELETE_COMMAND)) {
                executeDelete();
            } else {
                responseCode = 5;
                responseString = "Unsupported command: " + command;
            }
        } else {
            responseCode = -1;
            responseString = "Not ready to execute";
        }
        EaasyStreet.logTrace("[Out] HibernateDataAccessBean.execute(); Response code = " + responseCode);
    }

    /**
	 * Returns true if the command parameters have been successfully set
	 * and the command is ready to execute.
	 * 
	 * @return true if the command is ready to execute
	 * @since Eaasy Street 2.0.1
	 */
    public boolean isReadyToExecute() {
        boolean isReady = false;
        if (command != null) {
            if (command.equals(DataConnector.READ_COMMAND)) {
                if (parameters != null) {
                    isReady = true;
                }
            } else if (command.equals(DataConnector.INSERT_COMMAND) || command.equals(DataConnector.UPDATE_COMMAND) || command.equals(DataConnector.DELETE_COMMAND)) {
                if (parameters != null) {
                    if (parameters.containsKey(DataConnector.RECORD_PARAMETER)) {
                        if (command.equals(DataConnector.UPDATE_COMMAND)) {
                            if (parameters.containsKey(DataConnector.RECORD_KEY_PARAMETER)) {
                                isReady = true;
                            }
                        } else {
                            isReady = true;
                        }
                    } else {
                        if (command.equals(DataConnector.DELETE_COMMAND)) {
                            if (parameters.containsKey(DataConnector.RECORD_KEY_PARAMETER)) {
                                isReady = true;
                            }
                        }
                    }
                }
            }
        }
        return isReady;
    }

    /**
	 * <p>Used to execute the key-based "read" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 * 
	 * @since Eaasy Street 2.0.1
	 */
    public void executeRead() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.get(targetClass, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
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
	 * parameters have been set and the command is "ready to excute".<p>
	 * 
	 * @since Eaasy Street 2.0.1
	 */
    public void executeFind() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find(getQueryStatement());
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
	 * <p>Used to execute the "insert" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 * 
	 * @since Eaasy Street 2.0.1
	 */
    public void executeInsert() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.save(parameters.get(DataConnector.RECORD_PARAMETER));
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
	 * <p>Used to execute the "update" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 * 
	 * @since Eaasy Street 2.0.1
	 */
    public void executeUpdate() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            PersistentDataBean toBeUpdated = (PersistentDataBean) session.load(targetClass, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
            toBeUpdated.update((PersistentDataBean) parameters.get(DataConnector.RECORD_PARAMETER));
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
	 * <p>Used to execute the "delete" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 * 
	 * @since Eaasy Street 2.0.1
	 */
    public void executeDelete() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Object toBeDeleted = session.get(targetClass, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
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
	 * @since Eaasy Street 2.0.1
	 */
    private String getQueryStatement() {
        if (hqlStatement.indexOf("{key}") != -1) {
            return resolveKeyBasedQuery();
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
	 * @since	Eaasy Street 2.0.1
	 */
    private String resolveKeyBasedQuery() {
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

    /**
	 * <p>Returns the sessionFactory.</p>
	 * 
	 * @return the sessionFactory
	 * @since Eaasy Street 2.2.2
	 */
    protected SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
	 * Returns the hqlStatement.
	 * @return String
	 */
    public String getHqlStatement() {
        return hqlStatement;
    }

    /**
	 * Sets the hqlStatement.
	 * @param hqlStatement The hqlStatement to set
	 */
    public void setHqlStatement(String hqlStatement) {
        this.hqlStatement = hqlStatement;
    }
}
