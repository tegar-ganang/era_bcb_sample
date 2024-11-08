package DA;

import BE.torneo;
import conexionBD.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author Billy
 */
public class torneoDA {

    torneo eltorneo;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of torneoDA */
    public torneoDA() {
    }

    public torneoDA(torneo elTorneo) {
        this.eltorneo = elTorneo;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into torneo" + "(nombreTorneo, ciudad, fechaInicio, fechaFinal, organizador, numeroDivisiones, terminado)" + "values (?, ?, ?, ?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(eltorneo);
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

    private int otropopulate(ResultSet resultSet) {
        int dato;
        try {
            dato = resultSet.getInt("numeroDivisiones");
            return dato;
        } catch (SQLException ex) {
            return 0;
        }
    }

    public int getNdivTorneo(int idTorneo) {
        int intResult = 0, dato = 0;
        try {
            String sql = "SELECT * FROM torneo WHERE idTorneo=" + idTorneo;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                intResult = 1;
                dato = otropopulate(resultSet);
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

    public boolean actualizarNdivisiones(int idTorneo, int nDivisiones) {
        int intResult = 0;
        String sql = "UPDATE torneo" + " SET  numeroDivisiones=" + nDivisiones + " WHERE idTorneo=" + idTorneo;
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

    private void populatePreparedStatement(torneo eltorneo) {
        try {
            ps.setString(1, eltorneo.getnombreTorneo());
            ps.setString(2, eltorneo.getCiudad());
            Calendar p = eltorneo.getFechaInicio();
            if (p == null) ps.setDate(3, null); else {
                java.sql.Date dia = new java.sql.Date(eltorneo.getFechaInicio().getTimeInMillis());
                ps.setDate(3, dia, eltorneo.getFechaInicio());
            }
            Calendar q = eltorneo.getFechaFin();
            if (q == null) ps.setDate(4, null); else {
                java.sql.Date dia2 = new java.sql.Date(eltorneo.getFechaFin().getTimeInMillis());
                ps.setDate(4, dia2, eltorneo.getFechaFin());
            }
            ps.setString(5, eltorneo.getOrganizador());
            ps.setInt(6, eltorneo.getNumeroDivisiones());
            ps.setBoolean(7, eltorneo.getTerminoTorneo());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<torneo> listarTorneos() {
        ArrayList<torneo> torneosAbiertos = new ArrayList<torneo>();
        try {
            String sql = "SELECT * FROM torneo WHERE terminado <> 3";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.eltorneo = new torneo();
                populateTorneos(resultSet);
                torneosAbiertos.add(this.eltorneo);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return torneosAbiertos;
    }

    private void populateTorneos(ResultSet resultSet) {
        try {
            this.eltorneo.setTorneo(resultSet.getInt("idTorneo"));
            this.eltorneo.setnombreTorneo(resultSet.getString("nombreTorneo"));
            this.eltorneo.setCiudad(resultSet.getString("ciudad"));
            java.util.Calendar fecha2 = Calendar.getInstance();
            fecha2.setTime(resultSet.getDate("fechaFinal"));
            this.eltorneo.setFechaFin(fecha2);
            java.util.Calendar fecha1 = Calendar.getInstance();
            fecha1.setTime(resultSet.getDate("fechaInicio"));
            this.eltorneo.setFechaInicio(fecha1);
            this.eltorneo.setOrganizador(resultSet.getString("organizador"));
            this.eltorneo.setNumeroDivisiones(resultSet.getInt("numeroDivisiones"));
            this.eltorneo.setTerminoTorneo(resultSet.getBoolean("terminado"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean update(int idTorneo, torneo torneoModificado) {
        int intResult = 0;
        String sql = "UPDATE torneo " + "SET nombreTorneo = ?, ciudad = ?, fechaInicio = ?, fechaFinal = ?, " + " organizador = ? " + " WHERE idTorneo = " + idTorneo;
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement2(torneoModificado);
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

    private void populatePreparedStatement2(torneo eltorneo) {
        try {
            ps.setString(1, eltorneo.getnombreTorneo());
            ps.setString(2, eltorneo.getCiudad());
            Calendar p = eltorneo.getFechaInicio();
            if (p == null) ps.setDate(3, null); else {
                java.sql.Date dia = new java.sql.Date(eltorneo.getFechaInicio().getTimeInMillis());
                ps.setDate(3, dia, eltorneo.getFechaInicio());
            }
            Calendar q = eltorneo.getFechaFin();
            if (q == null) ps.setDate(4, null); else {
                java.sql.Date dia2 = new java.sql.Date(eltorneo.getFechaFin().getTimeInMillis());
                ps.setDate(4, dia2, eltorneo.getFechaFin());
            }
            ps.setString(5, eltorneo.getOrganizador());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public torneo buscarTorneoxId(int id) {
        torneo unTorneo = null;
        try {
            String sql = "SELECT * FROM torneo WHERE idTorneo = " + id;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                unTorneo = populateTorneosdeBuscar(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return unTorneo;
    }

    private torneo populateTorneosdeBuscar(ResultSet resultSet) {
        torneo eltorneo = new torneo();
        try {
            eltorneo.setTorneo(resultSet.getInt("idTorneo"));
            eltorneo.setnombreTorneo(resultSet.getString("nombreTorneo"));
            eltorneo.setCiudad(resultSet.getString("ciudad"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return eltorneo;
    }

    public boolean actualizarEstadoEliminacion(int idTorneo) {
        int intResult = 0;
        String sql = "UPDATE torneo " + " SET  terminado = 3 WHERE idTorneo= " + idTorneo;
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
}
