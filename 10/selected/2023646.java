package org.eaasyst.eaa.data.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import org.eaasyst.eaa.data.DataConnector;
import org.eaasyst.eaa.data.HibernateSessionManager;
import org.eaasyst.eaa.data.RecordFactory;
import org.eaasyst.eaa.data.RecordSet;
import org.eaasyst.eaa.data.UserProfileDab;
import org.eaasyst.eaa.security.UserProfileManager;
import org.eaasyst.eaa.syst.data.persistent.BasicUser;
import org.eaasyst.eaa.syst.data.persistent.SecurityQuestion;
import org.eaasyst.eaa.syst.data.transients.SearchSpecification;
import org.eaasyst.eaa.utils.SqlUtils;
import org.eaasyst.eaa.utils.StringUtils;

/**
 * <p>Data accesss bean for User Profile information. This bean supports
 * the following commands:
 * <ul>
 * <li>get - Returns a single <code>UserProfile</code> object
 * <li>getByEmail - Returns a single <code>UserProfile</code> object
 * <li>getByGroup - Returns a set of <code>UserProfile</code> objects
 * associated with the group id passed in the bean parameters
 * <li>All - Returns all <code>UserProfile</code> objects in a List
 * <li>getUserIdsAndNames - Returns the userId and useName for all users
 * <li>READ (key) - Returns a <code>RecordSet</code> object containing
 * a single row for the profile associated with the specified key
 * <li>READ (filter) - Returns a <code>RecordSet</code> object containing
 * zero to many rows based on the specified search filter
 * <li>INSERT - inserts a new profile record into the database from the
 * <code>UserProfile</code> object passed in the bean parameters
 * <li>UPDATE - updates the database from the <code>UserProfile</code>
 * object passed in the bean parameters
 * <li>updateLastLogon - updates the last logon date
 * <li>updateSecurityQuestions - updates the database from the
 * <code>List</code> of security questions passed in the bean parameters
 * <li>updateForcedApplications - updates the database from the
 * <code>List</code> of forced applications passed in the bean parameters
 * <li>Reset - updates the database with a new password from the
 * <code>UserProfile</code> object passed in the bean parameters
 * <li>DELETE - deletes from the database the information for the profile
 * associated with the specified key
 * </ul>
 * </p>
 *
 * @version 2.9.1
 * @author Jeff Chilton
 */
public class HibernateUserProfileDab extends UserProfileDab {

    private SessionFactory sessionFactory = HibernateSessionManager.getHibernateSessionFactory("system");

    private static final RecordFactory readFactory = new UserProfileRecordFactory("single");

    /**
	 * <p>Constructs a new "HibernateUserProfileDab" object.</p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public HibernateUserProfileDab() {
        className = StringUtils.computeClassName(getClass());
    }

    /**
	 * <p>Used to execute the key-based "read" command after all command
	 * parameters have been set and the command is "ready to excute".<p>
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>RecordSet</code>
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeRead() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            BasicUser user = (BasicUser) session.get(BasicUser.class, userId);
            if (user != null) {
                executionResults = new RecordSet(readFactory.getMetaData());
                HashMap thisRecord = readFactory.createRecord(new Object[] { user.getUserId(), user.getPerson().getUseName(), user.getPerson().getNamePrefix(), user.getPerson().getFirstName(), user.getPerson().getMiddleName(), user.getPerson().getLastName(), user.getPerson().getNameSuffix(), user.getAttributes() });
                ((RecordSet) executionResults).addRecord(thisRecord);
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
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>"filter"</code> - <code>Map</code> (search criteria)</li>
	 * <li><code>"sort"</code> - <code>List</code> (sort criteria)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>Set</code>
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeSearch() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find(getQueryStatement("from BasicUser as user {filter} {sort}"));
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
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * <li><code>DataConnector.RECORD_PARAMETER</code> -
	 * <code>UserProfile</code> (profile fields)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeInsert() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            BasicUser user = (BasicUser) parameters.get(DataConnector.RECORD_PARAMETER);
            user.setIdString(user.getIdString().toLowerCase());
            user.setLastUpdate(new Date());
            user.setLastUpdateBy(UserProfileManager.getUserId());
            session.save(user);
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
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * <li><code>DataConnector.RECORD_PARAMETER</code> -
	 * <code>UserProfile</code> (profile fields)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeUpdate() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            BasicUser user = (BasicUser) session.load(BasicUser.class, userId);
            user.update((BasicUser) parameters.get(DataConnector.RECORD_PARAMETER));
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
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeDelete() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Object toBeDeleted = session.get(BasicUser.class, (Serializable) parameters.get(DataConnector.RECORD_KEY_PARAMETER));
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
	 * <p>Used to execute the "all" command after all command parameters
	 * have been set and the command is "ready to excute".</p>
	 * <p>
	 * <strong>Parameters:</strong>
	 * <br>(none)
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>Set</code> (user profile objects)</li>
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeAll() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find("from BasicUser order by idString");
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
	 * <p>Used to execute the "get" command after all command parameters
	 * have been set and the command is "ready to excute".</p>
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>UserProfile</code>
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeGet() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.get(BasicUser.class, userId);
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
	 * <p>Used to execute the "getByEmail" command after all command
	 * parameters have been set and the command is "ready to excute".</p>
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (e-mail address)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>UserProfile</code>
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeGetByEmail() {
        Session session = null;
        Transaction tx = null;
        String email = (String) parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            Object allResults = session.find("from BasicUser as user where user.person.contacts['email'].value='" + email + "' order by user.person.lastName");
            if (allResults != null) {
                ArrayList arrayList = (ArrayList) allResults;
                if (arrayList.size() == 0) {
                    responseCode = 1;
                    responseString = "Record not found";
                } else {
                    Iterator i = arrayList.iterator();
                    executionResults = i.next();
                    responseCode = 0;
                    responseString = "Execution complete";
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
	 * <p>Used to execute the "getByGroup" command after all command
	 * parameters have been set and the command is "ready to excute".</p>
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>Integer</code>
	 * (group id)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>Set</code> (LabelValueBean objects)</li>
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeGetByGroup() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find(getQueryStatement("select new org.eaasyst.eaa.syst.data.transients.PersonValueBean(ugm.user.person.lastName, ugm.user.person.firstName, ugm.user.person.middleName, ugm.user.idString) from UserGroupMember as ugm where ugm.userGroupId='{key}' order by ugm.user.person.lastName, ugm.user.person.firstName, ugm.user.idString"));
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
	 * <p>Used to execute the "getUserIdsAndNames" command after all command
	 * parameters have been set and the command is "ready to excute".</p>
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>"filter"</code> - <code>Map</code> (search criteria - optional)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br><code>List</code>
	 * </p>
	 *
	 * @since Eaasy Street 2.2.1
	 */
    public void executeGetUserIdsAndNames() {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            executionResults = session.find("select new org.eaasyst.eaa.syst.data.transients.PersonValueBean(user.person.lastName, user.person.firstName, user.person.middleName, user.idString) from BasicUser as user order by user.person.lastName, user.person.firstName, user.idString");
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
	 * Used to execute the "reset" command after all command parameters
	 * have been set and the command is "ready to excute".
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * <li><code>DataConnector.RECORD_PARAMETER</code> -
	 * <code>UserProfile</code> (profile fields)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeReset() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            BasicUser newValues = (BasicUser) parameters.get(DataConnector.RECORD_PARAMETER);
            BasicUser user = (BasicUser) session.load(BasicUser.class, userId);
            user.setEncryptedPassword(newValues.getEncryptedPassword());
            user.setPasswordExpirationDate(newValues.getPasswordExpirationDate());
            user.setPreviousPasswords(newValues.getPreviousPasswords());
            user.setLastUpdate(new Date());
            user.setLastUpdateBy(UserProfileManager.getUserId());
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
	 * Used to execute the "updateLastLogon" command after all command
	 * parameters have been set and the command is "ready to excute".
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * <li><code>DataConnector.RECORD_PARAMETER</code> -
	 * <code>UserProfile</code> (profile fields)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeUpdateLastLogon() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            BasicUser user = (BasicUser) session.load(BasicUser.class, userId);
            Date rightNow = new Date();
            user.setLastLogonDate(rightNow);
            user.setLastUpdate(rightNow);
            user.setLastUpdateBy(UserProfileManager.getUserId());
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
	 * Used to execute the "updateSecurityQuestions" command after
	 * all command parameters have been set and the command is "ready
	 * to excute".
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * <li><code>DataConnector.RECORD_PARAMETER</code> -
	 * <code>List</code> (security questions)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeUpdateSecurityQuestions() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List newValues = (List) parameters.get(DataConnector.RECORD_PARAMETER);
            BasicUser user = (BasicUser) session.load(BasicUser.class, userId);
            Set originalQuestions = new HashSet(user.getSecurityQuestions());
            user.getSecurityQuestions().removeAll(originalQuestions);
            Iterator i = newValues.iterator();
            Date rightNow = new Date();
            userId = UserProfileManager.getUserId();
            while (i.hasNext()) {
                SecurityQuestion question = (SecurityQuestion) i.next();
                SecurityQuestion original = null;
                Iterator j = originalQuestions.iterator();
                while (j.hasNext()) {
                    SecurityQuestion thisQuestion = (SecurityQuestion) j.next();
                    if (thisQuestion.getQuestion().equals(question.getQuestion())) {
                        original = thisQuestion;
                    }
                }
                if (original != null) {
                    original.update(question);
                    user.getSecurityQuestions().add(original);
                } else {
                    question.setCreationDate(rightNow);
                    question.setCreatedBy(userId);
                    question.setLastUpdate(rightNow);
                    question.setLastUpdateBy(userId);
                    user.getSecurityQuestions().add(question);
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
	 * Used to execute the "updateForcedApplications" command after
	 * all command parameters have been set and the command is "ready
	 * to excute".
	 * <p>
	 * <strong>Parameters:</strong>
	 * <ul>
	 * <li><code>DataConnector.RECORD_KEY_PARAMETER</code> - <code>String</code>
	 * (userId)</li>
	 * <li><code>DataConnector.RECORD_PARAMETER</code> -
	 * <code>List</code> (forced applications)</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>ExecutionResults:</strong>
	 * <br>(none)
	 * </p>
	 *
	 * @since Eaasy Street 2.0.4
	 */
    public void executeUpdateForcedApplications() {
        String userId = "";
        Object idObject = parameters.get(DataConnector.RECORD_KEY_PARAMETER);
        if (idObject != null) {
            if (StringUtils.isString(idObject)) {
                String originalId = (String) idObject;
                userId = originalId.toLowerCase();
            }
        }
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            List newValues = (List) parameters.get(DataConnector.RECORD_PARAMETER);
            BasicUser user = (BasicUser) session.load(BasicUser.class, userId);
            user.setForcedApplications(newValues);
            user.setLastUpdate(new Date());
            user.setLastUpdateBy(UserProfileManager.getUserId());
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
	 * <p>Returns the query statement in use for this data access bean.</p>
	 * 
	 * @return	the query statement in use for this data access bean
	 * @since Eaasy Street 2.0.4
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
	 * @since Eaasy Street 2.0.4
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
