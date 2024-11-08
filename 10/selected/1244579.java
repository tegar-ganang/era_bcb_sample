package DA;

import BE.ronda;
import java.sql.SQLException;
import conexionBD.conexionBD;
import java.util.Calendar;
import java.sql.*;
import java.util.ArrayList;

/**
 *
 * @author Billy
 */
public class rondaDA {

    ronda unaRonda;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of rondaDA */
    public rondaDA() {
    }

    public rondaDA(ronda unaRonda) {
        this.unaRonda = unaRonda;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into ronda" + "(divisionxTorneo_idDivisionxTorneo, fechaRonda, nRonda, estado ) " + "values (?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(unaRonda);
            result = ps.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException exe) {
                exe.printStackTrace();
            }
        } finally {
            conexionBD.close(ps);
            conexionBD.close(connection);
        }
        return (result > 0);
    }

    private void populatePreparedStatement(ronda unaRonda) {
        try {
            ps.setInt(1, unaRonda.getIdDivision());
            Calendar p = unaRonda.getCalendar();
            if (p == null) ps.setDate(2, null); else {
                java.sql.Date dia = new java.sql.Date(unaRonda.getCalendar().getTimeInMillis());
                ps.setDate(2, dia, unaRonda.getCalendar());
            }
            ps.setInt(3, unaRonda.getNumeroRonda());
            ps.setInt(4, unaRonda.getEstado());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ronda getIdRonda(int idDivision, int nRonda) {
        ronda unaRonda = null;
        try {
            String sql = "SELECT * FROM ronda" + " WHERE divisionxTorneo_idDivisionxTorneo = " + idDivision + " AND " + " nRonda = " + nRonda;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                unaRonda = populateDivisiones(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return unaRonda;
    }

    private ronda populateDivisiones(ResultSet resultSet) {
        ronda unaRonda = new ronda();
        try {
            unaRonda.setIdRonda(resultSet.getInt("numeroRonda"));
            unaRonda.setIdDivision(resultSet.getInt("divisionxTorneo_idDivisionxTorneo"));
            unaRonda.setNumeroRonda(resultSet.getInt("nRonda"));
            unaRonda.setEstado(resultSet.getInt("estado"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return unaRonda;
    }

    public boolean actEstadoEnBD(int idRonda) {
        int intResult = 0;
        String sql = "UPDATE ronda " + " SET estado = 1" + " WHERE numeroRonda = " + idRonda;
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            intResult = ps.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException exe) {
                exe.printStackTrace();
            }
        } finally {
            conexionBD.close(ps);
            conexionBD.close(connection);
        }
        return (intResult > 0);
    }

    public int getEstadoRonda(int idRonda) {
        int dato = 0;
        try {
            String sql = "SELECT estado FROM ronda" + " WHERE numeroRonda = " + idRonda;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateEstado(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return dato;
    }

    private int populateEstado(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("estado");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }
}
