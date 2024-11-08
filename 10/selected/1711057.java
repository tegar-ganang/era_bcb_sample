package kwikserver.data;

import java.util.List;
import kwikserver.HibernateUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class RegistrationsManager {

    public static Registration getRegistrationByEmail(String email) throws Exception {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Query q = session.createQuery("from kwikserver.data.Registration as registration where registration.email = :email");
            q.setString("email", email);
            List registrations = q.list();
            if (registrations.isEmpty()) {
                return null;
            }
            tx.commit();
            return (Registration) registrations.get(0);
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public static int deleteRegistrationByEmail(String email) throws Exception {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            Query query = session.createQuery("delete from Registration where email = :email");
            query.setString("email", email);
            int rowCount = query.executeUpdate();
            tx.commit();
            return rowCount;
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }

    public static void saveRegistration(Registration registration) throws Exception {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            session.save(registration);
            tx.commit();
        } catch (Exception ex) {
            if (tx != null) {
                tx.rollback();
            }
            throw ex;
        }
    }

    static void deleteOldRegistrations() {
    }
}
