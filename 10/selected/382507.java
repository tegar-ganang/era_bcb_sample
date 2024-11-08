package org.openemergency.openCommon;

import org.openemergency.openCommon.CodesImpl;
import org.openemergency.openCommon.database.ContactInfo;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.Criteria.*;
import org.hibernate.cfg.Configuration;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author harlan
 */
public class ContactInfoImpl {

    public ContactInfoImpl() {
    }

    static final Logger logger = Logger.getLogger(ContactInfoImpl.class.getName());

    public final void add(ContactInfo info) throws Exception {
        String moduleName = this.getClass().getName() + "add";
        logger.debug("begin " + moduleName);
        Configuration config = new Configuration();
        config.configure();
        SessionFactory sessionFactory;
        Session session;
        sessionFactory = config.buildSessionFactory();
        session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(info);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.debug("ERROR " + moduleName + e);
            throw new Exception("Transaction failed (" + moduleName + ")  ", e);
        } finally {
            session.close();
        }
        sessionFactory.close();
        logger.debug("end " + moduleName);
    }

    public final void update(ContactInfo info, String oldNumber) throws Exception {
        String moduleName = this.getClass().getName() + "update";
        logger.debug("begin " + moduleName);
        Configuration config = new Configuration();
        config.configure();
        SessionFactory sessionFactory;
        Session session;
        Query query;
        sessionFactory = config.buildSessionFactory();
        session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            query = session.getNamedQuery("org.openemergency.openPatient.database.updateContactInfo");
            query.setString("numberEmail", info.getNumberEmail());
            query.setLong("contactCodeID", info.getContactCodeID());
            query.setDate("updateDate", new Date());
            query.setString("numberEmail", oldNumber);
            query.setLong("personSerialID", info.getPersonSerialID());
            int rowCount = query.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.debug("ERROR " + moduleName + "  " + e);
            throw new Exception("Transaction failed (" + moduleName + ")  ", e);
        } finally {
            session.close();
        }
        sessionFactory.close();
        logger.debug("end " + moduleName);
    }

    public final List<ContactInfo> getByPersonID(long personID) throws Exception {
        String moduleName = this.getClass().getName() + "getByPersonID";
        logger.debug("begin " + moduleName);
        Configuration config = new Configuration();
        config.configure();
        SessionFactory sessionFactory;
        Session session;
        Query query;
        List returnValue = null;
        sessionFactory = config.buildSessionFactory();
        session = sessionFactory.openSession();
        Transaction tx = null;
        try {
            query = session.getNamedQuery("org.openemergency.openPatient.database.getPatientByName");
            query.setLong("personid", personID);
            List found = query.list();
            if (found.isEmpty()) {
                logger.error("  " + moduleName + "  No person found with ID:  " + personID);
                returnValue = null;
            } else {
                returnValue = found;
            }
        } catch (Exception e) {
            if (tx != null) {
                tx.rollback();
            }
            logger.debug("ERROR " + moduleName + "  " + e);
            throw new Exception("Transaction failed (" + moduleName + ")  ", e);
        } finally {
            session.close();
        }
        sessionFactory.close();
        logger.debug("end " + moduleName);
        return returnValue;
    }
}
