package es.devel.opentrats.booking.service.business;

import es.devel.opentrats.booking.util.InitSessionFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author Fran Serrano
 */
public class JDBCService {

    /**
     * Creates a new instance of JDBCService
     */
    private JDBCService() {
    }

    public static synchronized ResultSet Retrieve(String SQL, Connection conexion) throws SQLException {
        java.sql.Statement sentencia = null;
        java.sql.ResultSet rs = null;
        sentencia = (Statement) conexion.createStatement();
        Logger.getRootLogger().debug(SQL);
        return (ResultSet) sentencia.executeQuery(SQL);
    }

    public static synchronized Integer ExecuteSQL(String SQL, Connection conexion) {
        Transaction tx = null;
        try {
            Session session = InitSessionFactory.getSessionFactory().getCurrentSession();
            tx = session.beginTransaction();
            Integer affected = new Integer(session.createSQLQuery(SQL).executeUpdate());
            tx.commit();
            return affected;
        } catch (HibernateException he) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new HibernateException("No se pudo ejecutar la instrucción contra la base de datos: " + tx.toString());
        }
    }
}
