package dk.highflier.airlog.dataaccess.dboracle;

import dk.highflier.airlog.utility.*;
import java.sql.*;
import java.awt.*;

public class ComponentPropertiesDAImpl implements dk.highflier.airlog.dataaccess.ComponentPropertiesDA {

    private org.log4j.Category log = org.log4j.Category.getInstance("Log.ComponentPropertiesDB");

    private JdbcConnection jdbc;

    public ComponentPropertiesDAImpl(JdbcConnection jdbc) {
        this.jdbc = jdbc;
    }

    public void store(Component component, String componentName, int currentPilot) {
        try {
            PreparedStatement psta = jdbc.prepareStatement("UPDATE component_prop " + "SET size_height = ?, size_width = ?, pos_x = ?, pos_y = ? " + "WHERE pilot_id = ? " + "AND component_name = ?");
            psta.setInt(1, component.getHeight());
            psta.setInt(2, component.getWidth());
            Point point = component.getLocation();
            psta.setInt(3, point.x);
            psta.setInt(4, point.y);
            psta.setInt(5, currentPilot);
            psta.setString(6, componentName);
            int update = psta.executeUpdate();
            if (update == 0) {
                psta = jdbc.prepareStatement("INSERT INTO component_prop " + "(size_height, size_width, pos_x, pos_y, pilot_id, component_name) " + "VALUES (?,?,?,?,?,?)");
                psta.setInt(1, component.getHeight());
                psta.setInt(2, component.getWidth());
                psta.setInt(3, point.x);
                psta.setInt(4, point.y);
                psta.setInt(5, currentPilot);
                psta.setString(6, componentName);
                psta.executeUpdate();
            }
            jdbc.commit();
        } catch (SQLException e) {
            jdbc.rollback();
            log.debug(e);
        }
    }

    public void restore(Component component, String componentName, int currentPilot) {
        try {
            PreparedStatement psta = jdbc.prepareStatement("SELECT size_height, size_width, pos_x, pos_y " + "FROM component_prop " + "WHERE pilot_id = ? " + "AND component_name = ?");
            psta.setInt(1, currentPilot);
            psta.setString(2, componentName);
            ResultSet resl = psta.executeQuery();
            if (resl.next()) {
                component.setSize(resl.getInt(2), resl.getInt(1));
                Point point = new Point(resl.getInt(3), resl.getInt(4));
                component.setLocation(point);
            }
        } catch (SQLException e) {
            log.debug(e);
        }
    }

    public void reset(String componentName, int currentPilot) {
        try {
            PreparedStatement psta = jdbc.prepareStatement("DELETE FROM component_prop " + "WHERE pilot_id = ? " + "AND component_name = ?");
            psta.setInt(1, currentPilot);
            psta.setString(2, componentName);
            psta.executeUpdate();
            jdbc.commit();
        } catch (SQLException e) {
            jdbc.rollback();
            log.debug(e);
        }
    }

    public void reset(int currentPilot) {
        try {
            PreparedStatement psta = jdbc.prepareStatement("DELETE FROM component_prop " + "WHERE pilot_id = ? ");
            psta.setInt(1, currentPilot);
            psta.executeUpdate();
            jdbc.commit();
        } catch (SQLException e) {
            jdbc.rollback();
            log.debug(e);
        }
    }
}
