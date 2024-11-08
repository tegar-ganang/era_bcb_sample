package DA;

import BE.jugador;
import BE.torneo;
import conexionBD.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

/**
 *
 * @author Billy
 */
public class jugadorDA {

    jugador elJugador;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of jugadorDA */
    public jugadorDA() {
    }

    public jugadorDA(jugador elJugador) {
        this.elJugador = elJugador;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into jugador" + "(apellidoPaterno, apellidoMaterno, nombres, fechaNacimiento, pais, rating, sexo)" + "values (?, ?, ?, ?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(elJugador);
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

    private void populatePreparedStatement(jugador elJugador) {
        try {
            ps.setString(1, elJugador.getApellidoPaterno());
            ps.setString(2, elJugador.getApellidoMaterno());
            ps.setString(3, elJugador.getNombres());
            Calendar q = elJugador.getFechadeNacimiento();
            if (q == null) ps.setDate(4, null); else {
                java.sql.Date dia2 = new java.sql.Date(elJugador.getFechadeNacimiento().getTimeInMillis());
                ps.setDate(4, dia2, elJugador.getFechadeNacimiento());
            }
            ps.setString(5, elJugador.getPais());
            ps.setInt(6, elJugador.getRating());
            ps.setInt(7, elJugador.getSexo());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public ArrayList<jugador> listarJugadores() {
        ArrayList<jugador> todosJugadores = new ArrayList<jugador>();
        try {
            String sql = "SELECT * FROM jugador";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.elJugador = new jugador();
                populateJugadores(resultSet);
                todosJugadores.add(this.elJugador);
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

    public ArrayList<jugador> listarJugadoresxDivision(int idDivision) {
        ArrayList<jugador> todosJugadores = new ArrayList<jugador>();
        try {
            String sql = " SELECT * FROM jugador A, jugadorxdivision B" + " WHERE A.idJugador = B.jugador_idJugador " + " AND divisionxTorneo_idDivisionxTorneo = " + idDivision;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.elJugador = new jugador();
                populateJugadores(resultSet);
                todosJugadores.add(this.elJugador);
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

    private void populateJugadores(ResultSet resultSet) {
        try {
            this.elJugador.setIdJugador(resultSet.getInt("idJugador"));
            this.elJugador.setApellidoPaterno(resultSet.getString("apellidoPaterno"));
            this.elJugador.setApellidoMaterno(resultSet.getString("apellidoMaterno"));
            this.elJugador.setNombres(resultSet.getString("nombres"));
            java.util.Calendar fecha1 = Calendar.getInstance();
            fecha1.setTime(resultSet.getDate("fechaNacimiento"));
            this.elJugador.setFechadeNacimiento(fecha1);
            this.elJugador.setPais(resultSet.getString("pais"));
            this.elJugador.setRating(resultSet.getInt("rating"));
            this.elJugador.setSexo(resultSet.getInt("sexo"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean update(int idJugador, jugador jugadorModificado) {
        int intResult = 0;
        String sql = "UPDATE jugador " + "SET apellidoPaterno = ?, apellidoMaterno = ?, nombres = ?, fechaNacimiento = ?, " + " pais = ?, rating = ?, sexo = ? " + " WHERE idJugador = " + idJugador;
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(jugadorModificado);
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

    public jugador getUnJugador(int id) {
        jugador unjugador = null;
        try {
            String sql = " SELECT * " + " FROM jugador " + " WHERE idJugador = " + id;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.elJugador = new jugador();
                populateJugadores(resultSet);
                unjugador = this.elJugador;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return unjugador;
    }
}
