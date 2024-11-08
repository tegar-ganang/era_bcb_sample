package DA;

import BE.division;
import java.sql.*;
import conexionBD.conexionBD;
import java.util.ArrayList;

/**
 *
 * @author Billy
 */
public class divisionDA {

    division laDivision;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of divisionDA */
    public divisionDA() {
    }

    public divisionDA(division laDivision) {
        this.laDivision = laDivision;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into divisionxTorneo" + "(torneo_idTorneo, tipoTorneo_idTipoTorneo, nombreDivision, descripcion, numJugadores, numFechas, terminado, tipoDesempate, rondaActual, ptosxbye)" + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement();
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

    private void populatePreparedStatement() {
        try {
            ps.setInt(1, laDivision.getidTorneo());
            ps.setInt(2, laDivision.getidTipoTorneo());
            ps.setString(3, laDivision.getnombreDivision());
            ps.setString(4, laDivision.getDescripcion());
            ps.setInt(5, laDivision.getnumJugadores());
            ps.setInt(6, laDivision.getnumFechas());
            ps.setString(7, laDivision.getTerminado());
            ps.setInt(8, laDivision.getDesempate());
            ps.setInt(9, laDivision.getRondaActual());
            ps.setDouble(10, laDivision.getPtosxBye());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public division getDivision(int idDivision) {
        try {
            String sql = "SELECT * FROM divisionxTorneo " + " WHERE idDivisionxTorneo = " + idDivision;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.laDivision = new division();
                populateDivisiones(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return this.laDivision;
    }

    public boolean actualizarDivisionActual(division div) {
        int intResult = 0;
        String sql = "UPDATE divisionxTorneo " + " SET tipoTorneo_idTipoTorneo = " + div.getidTipoTorneo() + " , nombreDivision = '" + div.getnombreDivision() + "', " + " descripcion = '" + div.getDescripcion() + "', tipoDesempate = " + div.getDesempate() + ", " + " numFechas = " + div.getnumFechas() + " , ptosxbye = " + div.getPtosxBye() + " WHERE idDivisionxTorneo = " + div.getidDivision();
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

    public ArrayList<division> listarDivisiones(int idTorneo) {
        ArrayList<division> lstDivisiones = new ArrayList<division>();
        try {
            String sql = "SELECT * FROM divisionxTorneo" + " WHERE torneo_idTorneo=" + idTorneo;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.laDivision = new division();
                populateDivisiones(resultSet);
                lstDivisiones.add(this.laDivision);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return lstDivisiones;
    }

    public int getRondaActual(int idDivision) {
        int dato = 0;
        try {
            String sql = "SELECT rondaActual FROM divisionxTorneo " + " WHERE idDivisionxTorneo = " + idDivision;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateRondaActual(resultSet);
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

    private int populateRondaActual(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("rondaActual");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    private void populateDivisiones(ResultSet resultSet) {
        try {
            this.laDivision.setidDivision(resultSet.getInt("idDivisionxTorneo"));
            this.laDivision.setnombreDivision(resultSet.getString("nombreDivision"));
            this.laDivision.setDescripcion(resultSet.getString("descripcion"));
            this.laDivision.setnumJugadores(resultSet.getInt("numJugadores"));
            this.laDivision.setnumFechas(resultSet.getInt("numFechas"));
            this.laDivision.setTerminado(resultSet.getString("terminado").charAt(0));
            this.laDivision.setDesempate(resultSet.getInt("tipoDesempate"));
            this.laDivision.setidTorneo(resultSet.getInt("torneo_idTorneo"));
            this.laDivision.setidTipoTorneo(resultSet.getInt("tipoTorneo_idTipoTorneo"));
            this.laDivision.setRondaActual(resultSet.getInt("rondaActual"));
            this.laDivision.setPtosxBye(resultSet.getDouble("ptosxbye"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean actualizarEstadoDivision(division div) {
        int intResult = 0;
        String sql = "UPDATE divisionxTorneo " + " SET terminado = '1' " + " WHERE idDivisionxTorneo = " + div.getidDivision();
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

    public boolean actualizarRondaActual(division div) {
        int intResult = 0;
        String sql = "UPDATE divisionxTorneo " + " SET rondaActual = " + div.getRondaActual() + " WHERE idDivisionxTorneo = " + div.getidDivision();
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

    public int getTotalFechas(int idDivision) {
        int dato = 0;
        try {
            String sql = "SELECT numFechas FROM divisionxTorneo " + " WHERE idDivisionxTorneo = " + idDivision;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateTotalFechas(resultSet);
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

    private int populateTotalFechas(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("numFechas");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public boolean actualizarNumeroRondas(int idDivision, int numFechas) {
        int intResult = 0;
        String sql = "UPDATE divisionxTorneo " + " SET numFechas = " + numFechas + " WHERE idDivisionxTorneo = " + idDivision;
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
