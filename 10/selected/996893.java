package conexionBD;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.io.Serializable;

public class Conexion implements Serializable {

    private static String driver;

    private static String nombreBD;

    private static String url;

    private static String usuario;

    private static String password;

    private static Connection conexion;

    public Conexion() {
    }

    public static void establecerPropiedadesConexion(String driver, String url, String nombreBd, String usuario, String password) {
        Conexion.driver = driver;
        Conexion.url = url;
        Conexion.nombreBD = nombreBd;
        Conexion.usuario = usuario;
        Conexion.password = password;
    }

    private static Connection getConexion() throws Exception {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName(driver);
                conexion = DriverManager.getConnection(url + nombreBD, usuario, password);
            }
        } catch (SQLException e) {
            throw new Exception("Error Conectando con la Base de Datos");
        } catch (ClassNotFoundException e) {
            throw new Exception("Error Cargando Driver de Gestor Base de Datos");
        }
        return conexion;
    }

    public static ResultSet ejecutarDQL(String tiraSQL) throws Exception {
        ResultSet rs = null;
        try {
            getConexion();
            Statement st = conexion.createStatement();
            rs = st.executeQuery(tiraSQL);
            conexion.close();
        } catch (SQLException e) {
            throw new Exception("Error Ejecutando Consulta SQL");
        } catch (Exception e) {
            throw e;
        }
        return rs;
    }

    public static boolean ejecutarDML(String tiraSQL) throws Exception {
        boolean ok = false;
        try {
            getConexion();
            Statement st = conexion.createStatement();
            if (st.executeUpdate(tiraSQL) > 0) ok = true;
            conexion.close();
        } catch (SQLException e) {
            throw new Exception("Error Ejecutando Actualizacion en la Base de Datos");
        } catch (Exception e) {
            throw e;
        }
        return ok;
    }

    public static boolean ejecutarDMLTransaccion(List<String> tirasSQL) throws Exception {
        boolean ok = true;
        try {
            getConexion();
            conexion.setAutoCommit(false);
            Statement st = conexion.createStatement();
            for (String cadenaSQL : tirasSQL) {
                if (st.executeUpdate(cadenaSQL) < 1) {
                    ok = false;
                    break;
                }
            }
            if (ok) conexion.commit(); else conexion.rollback();
            conexion.setAutoCommit(true);
            conexion.close();
        } catch (SQLException e) {
            if (conexion != null && !conexion.isClosed()) {
                conexion.rollback();
            }
            throw new Exception("Error en Transaccion");
        } catch (Exception e) {
            throw new Exception("Error en Transaccion");
        }
        return ok;
    }

    public static String getDriver() {
        return driver;
    }

    public static void setDriver(String driver) {
        Conexion.driver = driver;
    }

    public static String getNombreBD() {
        return nombreBD;
    }

    public static void setNombreBD(String nombreBD) {
        Conexion.nombreBD = nombreBD;
    }

    public static String getUrl() {
        return url;
    }

    public static void setUrl(String url) {
        Conexion.url = url;
    }

    public static String getUsuario() {
        return usuario;
    }

    public static void setUsuario(String usuario) {
        Conexion.usuario = usuario;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        Conexion.password = password;
    }

    public static void setConexion(Connection conexion) {
        Conexion.conexion = conexion;
    }
}
