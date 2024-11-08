package GabrielPIMP_9Sep;

import java.sql.*;

public class Clientes {

    public static Connection conexion = null;

    public static PreparedStatement stm = null;

    public static ResultSet resultado = null;

    public boolean setCliente(int IDcliente, String nombre, String paterno, String materno, String ocupacion, String rfc) {
        boolean inserto = false;
        try {
            stm = conexion.prepareStatement("insert into clientes values( '" + IDcliente + "' , '" + nombre.toUpperCase() + "' , '" + paterno.toUpperCase() + "' , '" + materno.toUpperCase() + "' , '" + ocupacion.toUpperCase() + "' , '" + rfc + "' )");
            stm.executeUpdate();
            conexion.commit();
            inserto = true;
        } catch (SQLException e) {
            System.out.println("error al insertar registro en la tabla clientes general  " + e.getMessage());
            try {
                conexion.rollback();
            } catch (SQLException ee) {
                System.out.println(ee.getMessage());
            }
            return inserto = false;
        }
        return inserto;
    }

    public boolean setUpdateCliente(int IDcliente, String nombre, String paterno, String materno, String ocupacion, String rfc) {
        boolean update = false;
        try {
            stm = conexion.prepareStatement("update clientes set nombre='" + nombre.toUpperCase().trim() + "' , paterno='" + paterno.toUpperCase().trim() + "' ," + "materno='" + materno.toUpperCase().trim() + "',ocupacion='" + ocupacion.toUpperCase().trim() + "',rfc='" + rfc.trim() + "' where IDcliente ='" + IDcliente + "' ");
            stm.executeUpdate();
            conexion.commit();
            update = true;
        } catch (SQLException e) {
            System.out.println("error al actualizar registro en la tabla clientes  " + e.getMessage());
            try {
                conexion.rollback();
            } catch (SQLException ee) {
                System.out.println(ee.getMessage());
            }
            return update = false;
        }
        return update;
    }

    public boolean setDeleteCliente(int IDcliente) {
        boolean delete = false;
        try {
            stm = conexion.prepareStatement("delete clientes where IDcliente='" + IDcliente + "'");
            stm.executeUpdate();
            conexion.commit();
            delete = true;
        } catch (SQLException e) {
            System.out.println("Error en la eliminacion del registro en tabla clientes " + e.getMessage());
            try {
                conexion.rollback();
            } catch (SQLException ee) {
                System.out.println(ee.getMessage());
            }
            return delete = false;
        }
        return delete;
    }

    public int getNextIDClient() {
        int total = 0;
        try {
            stm = conexion.prepareStatement("SELECT count(*) FROM clientes");
            resultado = stm.executeQuery();
            while (resultado.next()) {
                total = Integer.parseInt(resultado.getString(1));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return total + 1;
    }

    public void getOpenConectionDB() {
        try {
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            } catch (Exception sqle1) {
                System.out.println("Fallo al cargar driver de DB..." + sqle1.getMessage());
            }
            conexion = DriverManager.getConnection("jdbc:sqlserver://localhost\\SQLEXPRESS;DatabaseName = visual", "sa", "univa");
            conexion.setAutoCommit(false);
        } catch (SQLException sqle) {
            System.out.println("Error En La Conexion... " + sqle.getMessage());
        }
    }

    public void getCloseConectionDB() {
        try {
            if (conexion != null) {
                conexion.close();
            }
            if (stm != null) {
                stm.close();
            }
            if (resultado != null) {
                resultado.close();
            }
        } catch (Exception e) {
            System.out.println("Error al cerrar conexiones de la DB...  " + e.getMessage());
        }
    }

    public String[] getClienteCompleto(int IDcliente) {
        String[] cliente = new String[6];
        try {
            stm = conexion.prepareStatement("SELECT IDcliente, nombre , paterno , materno ,ocupacion , rfc  from clientes where IDcliente='" + IDcliente + "' ");
            resultado = stm.executeQuery();
            while (resultado.next()) {
                cliente[0] = resultado.getString("IDcliente");
                cliente[1] = resultado.getString("nombre");
                cliente[2] = resultado.getString("paterno");
                cliente[3] = resultado.getString("materno");
                cliente[4] = resultado.getString("ocupacion");
                cliente[5] = resultado.getString("rfc");
            }
        } catch (SQLException e) {
            System.out.println("Error procedimiento a tabla clientes" + e.getMessage());
        }
        return cliente;
    }

    public boolean validarCliente(int IDcliente) {
        boolean encontro = false;
        try {
            stm = conexion.prepareStatement("SELECT IDcliente FROM clientes where IDcliente='" + IDcliente + "' ");
            resultado = stm.executeQuery();
            if (resultado.next() == true) {
                return encontro = true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return encontro;
    }
}
