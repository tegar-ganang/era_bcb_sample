package DA;

import BE.jugadorxDivision;
import conexionBD.conexionBD;
import java.sql.*;
import java.util.ArrayList;

/**
 *
 * @author Billy
 */
public class jugadorxDivisionDA {

    jugadorxDivision jxd;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of jugadorxDivisionDA */
    public jugadorxDivisionDA() {
    }

    public jugadorxDivisionDA(jugadorxDivision jxd) {
        this.jxd = jxd;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into jugadorxdivision" + "(divisionxTorneo_idDivisionxTorneo, jugador_idJugador, posicionFinal, numeroId, ganados, derrotas, " + "empate, estado, puntajeTotal, bye)" + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(jxd);
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

    private void populatePreparedStatement(jugadorxDivision jxd) {
        try {
            ps.setInt(1, this.jxd.getIdDivisionTorneo());
            ps.setInt(2, this.jxd.getIdJugador());
            ps.setInt(3, this.jxd.getPosicionFinal());
            ps.setInt(4, this.jxd.getNumeroId());
            ps.setInt(5, this.jxd.getGanados());
            ps.setInt(6, this.jxd.getDerrotas());
            ps.setInt(7, this.jxd.getEmpates());
            ps.setString(8, this.jxd.getEstado());
            ps.setDouble(9, this.jxd.getPuntajeTotal());
            ps.setInt(10, this.jxd.getBye());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<jugadorxDivision> listarJugadoresxDiv(int idDivision) {
        ArrayList<jugadorxDivision> todosJugadores = new ArrayList<jugadorxDivision>();
        try {
            String sql = "SELECT * FROM jugadorxdivision JD, jugador J, divisionxtorneo D " + " WHERE JD.jugador_idJugador = J.idJugador AND " + " divisionxTorneo_idDivisionxTorneo = D.idDivisionxTorneo AND " + " D.idDivisionxTorneo = " + idDivision + " AND JD.estado ='A' ";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.jxd = new jugadorxDivision();
                populateJugadoresxDiv(resultSet);
                todosJugadores.add(this.jxd);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return todosJugadores;
    }

    private void populateJugadoresxDiv(ResultSet resultSet) {
        try {
            this.jxd.setIdJugadorxDiv(resultSet.getInt("idJugadorxDivision"));
            this.jxd.setIdDivisionTorneo(resultSet.getInt("divisionxTorneo_idDivisionxTorneo"));
            this.jxd.setIdJugador(resultSet.getInt("jugador_idJugador"));
            this.jxd.setPosicionFinal(resultSet.getInt("posicionFinal"));
            this.jxd.setNumeroId(resultSet.getInt("numeroId"));
            this.jxd.setGanados(resultSet.getInt("ganados"));
            this.jxd.setDerrotas(resultSet.getInt("derrotas"));
            this.jxd.setEmpates(resultSet.getInt("empate"));
            this.jxd.setEstado(resultSet.getString("estado").charAt(0));
            this.jxd.setPuntajeTotal(resultSet.getDouble("puntajeTotal"));
            this.jxd.setBye(resultSet.getInt("bye"));
            this.jxd.setApellidoNombres(resultSet.getString("apellidoPaterno") + ", " + resultSet.getString("nombres"));
            this.jxd.setSexo(resultSet.getInt("sexo"));
            this.jxd.setPais(resultSet.getString("pais"));
            this.jxd.setRating(resultSet.getInt("rating"));
            this.jxd.setNumeroRondas(resultSet.getInt("numFechas"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean asignarIdDivisionTorneo(jugadorxDivision jxd, int idDiv, int valor) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivision " + " SET numeroId = " + valor + " " + " WHERE jugador_idJugador = " + jxd.getIdJugador() + " AND divisionxTorneo_idDivisionxTorneo = " + idDiv;
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

    public boolean actualizarPuntajeTotal(jugadorxDivision jxd, int idDiv, double valor) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivision " + " SET puntajeTotal = " + valor + " " + " WHERE jugador_idJugador = " + jxd.getIdJugador() + " AND divisionxTorneo_idDivisionxTorneo = " + idDiv;
        ;
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

    public jugadorxDivision buscarJugadorxDiv(int idDivision, String apellidoPaterno, String nombre) {
        try {
            String sql = "SELECT * FROM jugadorxdivision JD, jugador J, divisionxtorneo D " + " WHERE JD.jugador_idJugador = J.idJugador AND " + " divisionxTorneo_idDivisionxTorneo = D.idDivisionxTorneo AND " + " D.idDivisionxTorneo = " + idDivision + " AND J.apellidoPaterno = '" + apellidoPaterno + "' AND " + " J.nombres = '" + nombre + "'";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.jxd = new jugadorxDivision();
                populateJugadoresxDiv(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return this.jxd;
    }

    public boolean eliminarJugador(int idJxd) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivision " + " SET estado = 'X' " + " WHERE idJugadorxDivision = " + idJxd;
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

    public double getSumaPuntosRivales(int idPareo, int idDivision) {
        double puntaje = 0;
        String sql;
        try {
            sql = " SELECT SUM(C.puntajeTotal) " + " FROM jugadorxDivision C " + " WHERE C.divisionxTorneo_idDivisionxTorneo = " + idDivision + " AND C.numeroId IN (SELECT DISTINCT A.idPareoRival " + " FROM jugadorxdivxronda A, jugadorxDivision B " + " WHERE A.idPareo = " + idPareo + " AND B.divisionxTorneo_idDivisionxTorneo = " + idDivision + " AND A.jugadorxDivision_idJugadorxDivision = B.idJugadorxDivision)";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                puntaje = populatePuntaje(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return puntaje;
    }

    private double populatePuntaje(ResultSet resultSet) {
        double puntaje = 0;
        try {
            puntaje = resultSet.getDouble("SUM(C.puntajeTotal)");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return puntaje;
    }

    public ArrayList<Double> getListaPuntosRivales(int idPareo, int idDivision) {
        ArrayList<Double> lstpuntaje = new ArrayList<Double>();
        double puntaje;
        String sql;
        try {
            sql = " SELECT puntajeTotal " + " FROM jugadorxDivision " + " WHERE divisionxTorneo_idDivisionxTorneo = " + idDivision + " AND numeroId " + "IN (SELECT DISTINCT idPareoRival " + " FROM jugadorxdivxronda, jugadorxDivision  " + " WHERE idPareo = " + idPareo + " AND divisionxTorneo_idDivisionxTorneo = " + idDivision + " AND jugadorxDivision_idJugadorxDivision = idJugadorxDivision)";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                puntaje = populatePuntaje2(resultSet);
                lstpuntaje.add(puntaje);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return lstpuntaje;
    }

    private double populatePuntaje2(ResultSet resultSet) {
        double puntaje = 0;
        try {
            puntaje = resultSet.getDouble("puntajeTotal");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return puntaje;
    }
}
