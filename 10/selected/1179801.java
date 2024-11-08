package DA;

import BE.partida;
import java.sql.SQLException;
import conexionBD.conexionBD;
import java.util.Calendar;
import java.sql.*;
import java.util.ArrayList;

/**
 *
 * @author Billy
 */
public class partidaDA {

    partida unaPartida;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of partidaDA */
    public partidaDA() {
    }

    public partidaDA(partida unaPartida) {
        this.unaPartida = unaPartida;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into partida" + "(torneo_idTorneo, jugador_idJugadorNegras, jugador_idJugadorBlancas, registrado, fecha," + " movs, resultado, nombreBlancas, nombreNegras, eloBlancas, eloNegras, idApertura)" + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(unaPartida);
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

    private void populatePreparedStatement(partida unaPartida) {
        try {
            ps.setInt(1, unaPartida.dameIdTorneo());
            ps.setInt(2, unaPartida.dameIdJugadorNegras());
            ps.setInt(3, unaPartida.dameIdJugadorBlancas());
            ps.setString(4, unaPartida.dameEstadoRegistrado());
            Calendar p = unaPartida.dameFechaJuego();
            if (p == null) ps.setDate(5, null); else {
                java.sql.Date dia = new java.sql.Date(unaPartida.dameFechaJuego().getTimeInMillis());
                ps.setDate(5, dia, unaPartida.dameFechaJuego());
            }
            ps.setString(6, unaPartida.dameMovimientos());
            ps.setString(7, unaPartida.dameResultado());
            ps.setString(8, unaPartida.dameNombreJugadorBlanco());
            ps.setString(9, unaPartida.dameNombreJugadorNegro());
            ps.setString(10, unaPartida.dameEloBlancas());
            ps.setString(11, unaPartida.dameEloNegras());
            ps.setInt(12, unaPartida.dameidApertura());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<partida> listarPartidas() {
        ArrayList<partida> partidasJugadas = new ArrayList<partida>();
        try {
            String sql = "SELECT * FROM partida p, torneo t WHERE p.torneo_idTorneo = t.idTorneo";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.unaPartida = new partida();
                populatePartidas(resultSet);
                partidasJugadas.add(this.unaPartida);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return partidasJugadas;
    }

    private void populatePartidas(ResultSet resultSet) {
        try {
            this.unaPartida.setIdPartida(resultSet.getInt("idPartida"));
            this.unaPartida.setIdTorneo(resultSet.getInt("torneo_idTorneo"));
            this.unaPartida.setIdJugadorNegras(resultSet.getInt("jugador_idJugadorNegras"));
            this.unaPartida.setIdJugadorBlancas(resultSet.getInt("jugador_idJugadorBlancas"));
            this.unaPartida.setEstadoRegistrado(resultSet.getString("registrado"));
            java.util.Calendar fecha1 = Calendar.getInstance();
            fecha1.setTime(resultSet.getDate("fecha"));
            this.unaPartida.setFechaJuego(fecha1);
            this.unaPartida.setMovimimentos(resultSet.getString("movs"));
            this.unaPartida.setResultado(resultSet.getString("resultado"));
            this.unaPartida.setNombreJugadorBlanco(resultSet.getString("nombreBlancas"));
            this.unaPartida.setNombreJugadorNegro(resultSet.getString("nombreNegras"));
            this.unaPartida.setEloBlancas(resultSet.getString("eloBlancas"));
            this.unaPartida.setEloNegras(resultSet.getString("eloNegras"));
            this.unaPartida.setApertura(resultSet.getInt("idApertura"));
            this.unaPartida.setNombreTorneo(resultSet.getString("nombreTorneo"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean update(int idPartida, partida partidaModificada) {
        int intResult = 0;
        String sql = "UPDATE partida " + "SET torneo_idTorneo = ?, " + " jugador_idJugadorNegras = ?, jugador_idJugadorBlancas = ?, " + " fecha = ?, " + " resultado = ?, " + " nombreBlancas = ?, nombreNegras = ?, eloBlancas = ?, eloNegras = ?, idApertura = ? " + " WHERE idPartida = " + idPartida;
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement2(partidaModificada);
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

    private void populatePreparedStatement2(partida unaPartida) {
        try {
            ps.setInt(1, unaPartida.dameIdTorneo());
            ps.setInt(2, unaPartida.dameIdJugadorNegras());
            ps.setInt(3, unaPartida.dameIdJugadorBlancas());
            Calendar p = unaPartida.dameFechaJuego();
            if (p == null) ps.setDate(4, null); else {
                java.sql.Date dia = new java.sql.Date(unaPartida.dameFechaJuego().getTimeInMillis());
                ps.setDate(4, dia, unaPartida.dameFechaJuego());
            }
            ps.setString(5, unaPartida.dameResultado());
            ps.setString(6, unaPartida.dameNombreJugadorBlanco());
            ps.setString(7, unaPartida.dameNombreJugadorNegro());
            ps.setString(8, unaPartida.dameEloBlancas());
            ps.setString(9, unaPartida.dameEloNegras());
            ps.setInt(10, unaPartida.dameidApertura());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public int getUltimoIdPartida() {
        int dato = 0;
        try {
            String sql = "SELECT MAX(idPartida) FROM partida ";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateIdPartidas(resultSet);
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

    private int populateIdPartidas(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("MAX(idPartida)");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public partida getPartidaxId(int idPartida) {
        try {
            String sql = " SELECT * FROM partida p, torneo t " + " WHERE p.torneo_idTorneo = t.idTorneo AND p.idPartida = " + idPartida;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.unaPartida = new partida();
                populatePartidas(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return this.unaPartida;
    }
}
