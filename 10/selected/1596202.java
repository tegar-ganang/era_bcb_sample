package DA;

import BE.jugadorxDivxRonda;
import BE.jugadorxELO;
import java.sql.SQLException;
import conexionBD.conexionBD;
import java.sql.*;
import java.util.ArrayList;

/**
 *
 * @author Billy
 */
public class jugadorxDivxRondaDA {

    jugadorxDivxRonda unjxdxr;

    private Connection connection;

    private Statement statement;

    private PreparedStatement ps;

    private ResultSet resultSet;

    /** Creates a new instance of jugadorxDivxRondaDA */
    public jugadorxDivxRondaDA() {
    }

    public jugadorxDivxRondaDA(jugadorxDivxRonda unjxdxr) {
        this.unjxdxr = unjxdxr;
    }

    public boolean crear() {
        int result = 0;
        String sql = "insert into jugadorxdivxronda" + "(jugadorxDivision_idJugadorxDivision, ronda_numeroRonda, resultado, idPareoRival, color, flotante, puntajeRonda, idPareo) " + "values (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatement(unjxdxr);
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

    private void populatePreparedStatement(jugadorxDivxRonda unjxdxr) {
        try {
            ps.setInt(1, unjxdxr.getIdJugador());
            ps.setInt(2, unjxdxr.getRonda());
            ps.setString(3, unjxdxr.getResultado());
            ps.setInt(4, unjxdxr.getPareoRival());
            ps.setInt(5, unjxdxr.getColor());
            ps.setInt(6, unjxdxr.getFlotante());
            ps.setDouble(7, unjxdxr.getPuntaje());
            ps.setInt(8, unjxdxr.getIdPareo());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public double getPuntaje(int id) {
        double puntaje = 0;
        try {
            String sql = "SELECT puntajeRonda FROM jugadorxdivxronda " + " WHERE idJugxDivxRnd = " + id;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                puntaje = populateDivisiones(resultSet);
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

    private double populateDivisiones(ResultSet resultSet) {
        double puntaje = 0;
        try {
            puntaje = resultSet.getDouble("puntajeRonda");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return puntaje;
    }

    public boolean actualizarDatosFinal(int idJugadorDiv, int idRonda, jugadorxDivxRonda unjxdxr) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET resultado = ?, puntajeRonda = ? " + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
        try {
            connection = conexionBD.getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);
            populatePreparedStatementActFinal(unjxdxr);
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

    private void populatePreparedStatementActFinal(jugadorxDivxRonda unjxdxr) {
        try {
            ps.setString(1, unjxdxr.getResultado());
            ps.setDouble(2, unjxdxr.getPuntaje());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public boolean actualizarDatosPrevia(int idJugadorDiv, int idRonda, int idPareoRival, int color, int flotante) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET idPareoRival = " + idPareoRival + " , color = " + color + " , flotante = " + flotante + " " + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
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

    public int getIdJugadorxRonda(int idJugxDiv, int idRonda) {
        int id = 0;
        try {
            String sql = "SELECT idJugxDivxRnd FROM jugadorxdivxronda " + " WHERE  jugadorxDivision_idJugadorxDivision = " + idJugxDiv + " AND ronda_numeroRonda = " + idRonda;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                id = populateId(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return id;
    }

    private int populateId(ResultSet resultSet) {
        int id = 0;
        try {
            id = resultSet.getInt("idJugxDivxRnd");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return id;
    }

    public ArrayList<jugadorxDivxRonda> getJugadoresxRonda(int idRonda) {
        ArrayList<jugadorxDivxRonda> arr = new ArrayList<jugadorxDivxRonda>();
        try {
            String sql = " SELECT ronda_numeroRonda, resultado, idPareoRival, color, flotante, puntajeRonda, idPareo, idPartida" + " FROM jugadorxdivxronda " + " WHERE ronda_numeroRonda = " + idRonda;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                this.unjxdxr = new jugadorxDivxRonda();
                populateArr(resultSet);
                arr.add(this.unjxdxr);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    public jugadorxDivxRonda getJugadorxRonda(int idJugDiv, int idRonda) {
        jugadorxDivxRonda unjxdxr = null;
        try {
            String sql = " SELECT ronda_numeroRonda, resultado, idPareoRival, color, flotante, puntajeRonda, idPareo" + " FROM jugadorxdivxronda " + " WHERE ronda_numeroRonda = " + idRonda + " AND jugadorxDivision_idJugadorxDivision = " + idJugDiv;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                unjxdxr = populateArr(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return unjxdxr;
    }

    private jugadorxDivxRonda populateArr(ResultSet resultSet) {
        try {
            this.unjxdxr.setRonda(resultSet.getInt("ronda_numeroRonda"));
            this.unjxdxr.setResultado(resultSet.getString("resultado"));
            this.unjxdxr.setPareoRival(resultSet.getInt("idPareoRival"));
            this.unjxdxr.setColor(resultSet.getInt("color"));
            this.unjxdxr.setFlotante(resultSet.getInt("flotante"));
            this.unjxdxr.setPuntaje(resultSet.getInt("puntajeRonda"));
            this.unjxdxr.setIdPareo(resultSet.getInt("idPareo"));
            this.unjxdxr.setIdPartida(resultSet.getInt("idPartida"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return unjxdxr;
    }

    public ArrayList<String> getResultadosxRonda(int idJugadorDivision) {
        ArrayList<String> arr = new ArrayList<String>();
        String dato;
        try {
            String sql = " SELECT resultado FROM jugadorxdivxronda A, ronda B" + " WHERE A.ronda_numeroRonda = B.numeroRonda AND " + " A.jugadorxDivision_idJugadorxDivision = " + idJugadorDivision + " ORDER BY B.nRonda";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateResultados(resultSet);
                arr.add(dato);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    private String populateResultados(ResultSet resultSet) {
        String dato = "";
        try {
            dato = resultSet.getString("resultado");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public ArrayList<Integer> getContrincantesxRonda(int idJugadorDivision) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        int dato;
        try {
            String sql = " SELECT idPareoRival FROM jugadorxdivxronda A, ronda B" + " WHERE A.ronda_numeroRonda = B.numeroRonda AND " + " A.jugadorxDivision_idJugadorxDivision = " + idJugadorDivision + " ORDER BY B.nRonda";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateContrincantes(resultSet);
                arr.add(dato);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    private int populateContrincantes(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("idPareoRival");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public ArrayList<Integer> getColoresxRonda(int idJugadorDivision) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        int dato;
        try {
            String sql = " SELECT color FROM jugadorxdivxronda A, ronda B" + " WHERE A.ronda_numeroRonda = B.numeroRonda AND " + " A.jugadorxDivision_idJugadorxDivision = " + idJugadorDivision + " ORDER BY B.nRonda";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateColores(resultSet);
                arr.add(dato);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    private int populateColores(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("color");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public boolean limpiarContrincantexRonda(jugadorxDivxRonda unjxdxr) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET idPareoRival = 0 " + " WHERE idJugxDivxRnd = " + unjxdxr.getIdJugxDivxRnd();
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

    public ArrayList<Integer> getFlotantesxRonda(int idJugadorDivision) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        int dato;
        try {
            String sql = " SELECT flotante FROM jugadorxdivxronda " + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDivision;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateFlotantes(resultSet);
                arr.add(dato);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    private int populateFlotantes(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("flotante");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public boolean ponerFlotantexRonda(int idJugadorDiv, int idRonda, int dato) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET flotante = " + dato + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
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

    public boolean ponerRivalxRonda(int idJugadorDiv, int idRonda, int dato) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET idPareoRival = " + dato + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
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

    public ArrayList<Integer> getLstColoresxRonda(int idJugadorDivision) {
        ArrayList<Integer> arr = new ArrayList<Integer>();
        int dato;
        try {
            String sql = " SELECT color FROM jugadorxdivxronda " + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDivision;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                dato = populateColor(resultSet);
                arr.add(dato);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    private int populateColor(ResultSet resultSet) {
        int dato = 0;
        try {
            dato = resultSet.getInt("color");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return dato;
    }

    public boolean ponerColorxRonda(int idJugadorDiv, int idRonda, int dato) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET color = " + dato + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
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

    public boolean actualizarIdPartida(int idJugadorDiv, int idRonda, int idPartida) {
        int intResult = 0;
        String sql = "UPDATE jugadorxdivxronda " + " SET idPartida = " + idPartida + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
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

    public int getIdPartidaRegistrada(int idJugadorDiv, int idRonda) {
        int idPartida = 0;
        try {
            String sql = "SELECT idPartida FROM jugadorxdivxronda " + " WHERE jugadorxDivision_idJugadorxDivision = " + idJugadorDiv + " AND ronda_numeroRonda = " + idRonda;
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                idPartida = populateIdPartida(resultSet);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return idPartida;
    }

    private int populateIdPartida(ResultSet resultSet) {
        int idPartida = 0;
        try {
            idPartida = resultSet.getInt("idPartida");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return idPartida;
    }

    public ArrayList<jugadorxELO> getContrincantesxELO(int idPareo, int idDivision) {
        String sql;
        ArrayList<jugadorxELO> arr = new ArrayList<jugadorxELO>();
        jugadorxELO jxELO;
        try {
            sql = " SELECT C.apellidoPaterno, C.nombres, C.rating, A.puntajeRonda, A.idPareo, A.ronda_numeroRonda " + " FROM jugadorxdivxronda A, jugadorxDivision B, jugador C, ronda D" + " WHERE A.idPareoRival = " + idPareo + " AND B.divisionxTorneo_idDivisionxTorneo = " + idDivision + " AND A.jugadorxDivision_idJugadorxDivision = B.idJugadorxDivision " + " AND B.jugador_idJugador = C.idJugador " + " AND A.ronda_numeroRonda = D.numeroRonda " + " ORDER BY D.nRonda";
            connection = conexionBD.getConnection();
            statement = connection.createStatement();
            resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                jxELO = populateContrincantesxELO(resultSet);
                arr.add(jxELO);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            conexionBD.close(resultSet);
            conexionBD.close(statement);
            conexionBD.close(connection);
        }
        return arr;
    }

    private jugadorxELO populateContrincantesxELO(ResultSet resultSet) {
        jugadorxELO unjxELO = new jugadorxELO();
        try {
            unjxELO.setEloOponente(resultSet.getInt("rating"));
            unjxELO.setApellidosOponente(resultSet.getString("apellidoPaterno"));
            unjxELO.setNombreOponente(resultSet.getString("nombres"));
            unjxELO.setPuntajePropio(resultSet.getDouble("puntajeRonda"));
            unjxELO.setIdPareo(resultSet.getInt("idPareo"));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return unjxELO;
    }
}
