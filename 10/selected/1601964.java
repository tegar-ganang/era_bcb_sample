package com.cinformatique.business.quartz.delicious;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import net.databinder.DataStaticService;
import org.hibernate.HibernateException;
import org.hibernate.Session;

public class OldResetBundles {

    public static void reset() throws Exception {
        Session session = DataStaticService.getHibernateSessionFactory().openSession();
        try {
            Connection connection = session.connection();
            try {
                Statement statement = connection.createStatement();
                try {
                    statement.executeUpdate("delete from Bundle");
                    connection.commit();
                } finally {
                    statement.close();
                }
            } catch (HibernateException e) {
                connection.rollback();
                throw new Exception(e);
            } catch (SQLException e) {
                connection.rollback();
                throw new Exception(e);
            }
        } catch (SQLException e) {
            throw new Exception(e);
        } finally {
            session.close();
        }
    }
}
