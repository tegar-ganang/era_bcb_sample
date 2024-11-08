import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

public class GestorCliente {

    GestorDisco gd;

    public GestorCliente() throws errorConexionBD {
        gd = new GestorDisco();
        gd.abrirConexion();
    }

    public void elimina(Cliente cli) throws errorSQL, errorConexionBD {
        System.out.println("GestorCliente.elimina()");
        int id = cli.getId();
        String sql;
        Statement stmt = null;
        try {
            gd.begin();
            sql = "DELETE FROM cliente WHERE cod_cliente = '" + id + "'";
            System.out.println("Ejecutando: " + sql);
            stmt = gd.getConexion().createStatement();
            stmt.executeUpdate(sql);
            System.out.println("executeUpdate");
            gd.commit();
            System.out.println("commit");
            stmt.close();
        } catch (SQLException e) {
            gd.rollback();
            throw new errorSQL(e.toString());
        } catch (errorConexionBD e) {
            System.err.println("Error en GestorCliente.elimina(): " + e);
        } catch (errorSQL e) {
            System.err.println("Error en GestorCliente.elimina(): " + e);
        }
    }

    public Cliente getCliente(int id) throws errorSQL, errorConexionBD {
        System.out.println("GestorCliente.getCliente()");
        Cliente cli = new Cliente();
        cli.setId(id);
        String sql;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            sql = "SELECT id, nif, nombre, apellido1, apellido2, " + "direccion, poblacion, telefono, movil, email, fecha_baja, " + "cod_cliente, tarjeta_credito, puntos " + "FROM persona, cliente " + "WHERE id=cod_cliente AND id=" + id;
            System.out.println("Ejecutando: " + sql);
            stmt = gd.getConexion().createStatement();
            rs = stmt.executeQuery(sql);
            System.out.println("executeQuery");
            if (rs.next()) {
                cli.setId(id);
                cli.setNif(rs.getString("nif"));
                cli.setNombre(rs.getString("nombre"));
                cli.setApellido1(rs.getString("apellido1"));
                cli.setApellido2(rs.getString("apellido2"));
                cli.setDireccion(rs.getString("direccion"));
                cli.setPoblacion(rs.getString("poblacion"));
                cli.setTelefono(rs.getString("telefono"));
                cli.setMovil(rs.getString("movil"));
                cli.setEmail(rs.getString("email"));
                cli.setFechaBaja(rs.getDate("fecha_baja"));
                cli.setCodUsuario(rs.getString("cod_cliente"));
                cli.setTarjetaCredito(rs.getString("tarjeta_credito"));
                cli.setPuntos(rs.getInt("puntos"));
                System.out.println("Cargados datos de persona y cliente");
            } else {
                System.out.println("cliente " + id + " no encontrado.");
            }
            stmt.close();
            rs.close();
        } catch (SQLException e) {
            throw new errorSQL(e.toString());
        }
        return cli;
    }

    public int addCliente(Cliente cli) throws errorConexionBD, errorSQL {
        System.out.println("GestorCliente.addCliente()");
        String sql;
        PreparedStatement pstmt = null;
        int nuevoId = 0;
        try {
            gd.begin();
            sql = "INSERT INTO persona(nif, nombre, apellido1, apellido2, " + "direccion, poblacion,telefono, movil, email, fecha_baja)" + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
            System.out.println("Ejecuando: " + sql);
            pstmt = gd.getConexion().prepareStatement(sql);
            pstmt.setString(1, cli.getNif());
            pstmt.setString(2, cli.getNombre());
            pstmt.setString(3, cli.getApellido1());
            pstmt.setString(4, cli.getApellido2());
            pstmt.setString(5, cli.getDireccion());
            pstmt.setString(6, cli.getPoblacion());
            pstmt.setString(7, cli.getTelefono());
            pstmt.setString(8, cli.getMovil());
            pstmt.setString(9, cli.getEmail());
            pstmt.setDate(10, (java.sql.Date) cli.getFechaBaja());
            ResultSet rs = pstmt.executeQuery();
            System.out.println("Devolviendo el nuevo ID");
            if (rs.next()) {
                nuevoId = rs.getInt(1);
                System.out.println("Nuevo id: " + nuevoId);
            }
            pstmt.close();
            rs.close();
            sql = "INSERT INTO cliente (cod_cliente, tarjeta_credito, puntos)" + "VALUES (?,?,?) RETURNING cod_cliente";
            System.out.println("Ejecuando: " + sql);
            pstmt = gd.getConexion().prepareStatement(sql);
            pstmt.setInt(1, nuevoId);
            pstmt.setString(2, cli.getTarjetaCredito());
            pstmt.setInt(3, 0);
            gd.commit();
            System.out.println("commit");
            rs = pstmt.executeQuery();
            rs.close();
            pstmt.close();
            return nuevoId;
        } catch (SQLException e) {
            System.err.println("Error en GestorCliente.addCliente()");
            gd.rollback();
            throw new errorSQL(e.toString());
        }
    }

    public Vector<Cliente> lista() throws errorSQL {
        Vector<Cliente> v = new Vector<Cliente>();
        Cliente cli = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = gd.getConexion().createStatement();
            String sql = "SELECT id, nif, nombre, apellido1, apellido2," + " direccion, poblacion, telefono, movil, email, fecha_baja, " + "cod_cliente, tarjeta_credito, puntos " + "FROM cliente,persona " + "WHERE id = cod_cliente";
            System.out.println("Ejecutando: \n" + sql);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cli = new Cliente();
                cli.setNif(rs.getString("nif"));
                cli.setNombre(rs.getString("nombre"));
                cli.setApellido1(rs.getString("apellido1"));
                cli.setApellido2(rs.getString("apellido2"));
                cli.setDireccion(rs.getString("direccion"));
                cli.setPoblacion(rs.getString("poblacion"));
                cli.setTelefono(rs.getString("telefono"));
                cli.setMovil(rs.getString("movil"));
                cli.setEmail(rs.getString("email"));
                cli.setFechaBaja(rs.getDate("fecha_baja"));
                cli.setTarjetaCredito(rs.getString("tarjeta_credito"));
                cli.setPuntos(rs.getInt("puntos"));
                v.add(cli);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw new errorSQL(e.toString());
        }
        return v;
    }

    public Collection<Cliente> listaPorApellidos(String apellido1, String apellido2) throws errorSQL {
        Vector<Cliente> v = new Vector<Cliente>();
        Cliente cli = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = gd.getConexion().createStatement();
            String sql = "SELECT id, nif, nombre, apellido1, apellido2," + " direccion, poblacion, telefono, movil, email, fecha_baja, " + "cod_cliente, tarjeta_credito, puntos " + "FROM cliente,persona " + "WHERE id = cod_cliente AND apellido1 = '" + apellido1 + "' AND apellido2='" + apellido2 + "'";
            System.out.println("Ejecutando: \n" + sql);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cli = new Cliente();
                cli.setNif(rs.getString("nif"));
                cli.setNombre(rs.getString("nombre"));
                cli.setApellido1(rs.getString("apellido1"));
                cli.setApellido2(rs.getString("apellido2"));
                cli.setDireccion(rs.getString("direccion"));
                cli.setPoblacion(rs.getString("poblacion"));
                cli.setTelefono(rs.getString("telefono"));
                cli.setMovil(rs.getString("movil"));
                cli.setEmail(rs.getString("email"));
                cli.setFechaBaja(rs.getDate("fecha_baja"));
                cli.setTarjetaCredito(rs.getString("tarjeta_credito"));
                cli.setPuntos(rs.getInt("puntos"));
                v.add(cli);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw new errorSQL(e.toString());
        }
        return v;
    }

    public Collection<Cliente> listaPorNomre(String nombre) throws errorSQL {
        Vector<Cliente> v = new Vector<Cliente>();
        Cliente cli = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = gd.getConexion().createStatement();
            String sql = "SELECT id, nif, nombre, apellido1, apellido2," + " direccion, poblacion, telefono, movil, email, fecha_baja, " + "cod_cliente, tarjeta_credito, puntos " + "FROM cliente,persona " + "WHERE id = cod_cliente AND nombre = '" + nombre + "'";
            System.out.println("Ejecutando: \n" + sql);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                cli = new Cliente();
                cli.setNif(rs.getString("nif"));
                cli.setNombre(rs.getString("nombre"));
                cli.setApellido1(rs.getString("apellido1"));
                cli.setApellido2(rs.getString("apellido2"));
                cli.setDireccion(rs.getString("direccion"));
                cli.setPoblacion(rs.getString("poblacion"));
                cli.setTelefono(rs.getString("telefono"));
                cli.setMovil(rs.getString("movil"));
                cli.setEmail(rs.getString("email"));
                cli.setFechaBaja(rs.getDate("fecha_baja"));
                cli.setTarjetaCredito(rs.getString("tarjeta_credito"));
                cli.setPuntos(rs.getInt("puntos"));
                v.add(cli);
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            throw new errorSQL(e.toString());
        }
        return v;
    }

    public void liberarRecursos() {
        gd.cerrarConexion();
    }

    public static void main(String[] args) {
        Cliente c = new Cliente();
        try {
            GestorCliente gCli = new GestorCliente();
            try {
                c.setId(0);
                c.setNif("12345678");
                c.setNombre("Pepe");
                c.setApellido1("P�rez");
                c.setApellido2("G�mez");
                c.setDireccion("C/Saboya 12-3");
                c.setPoblacion("Barcelona");
                c.setTelefono("93666555");
                c.setMovil("666 001122");
                c.setEmail("un.correo@no.es");
                c.setCodUsuario("2");
                c.setTarjetaCredito("123456789");
                System.out.println(gCli.listaPorNomre("Pepe"));
            } catch (errorSQL e) {
                System.out.println(e.getMessage());
            }
        } catch (errorConexionBD e) {
            System.out.println(e.getMessage());
        }
    }
}
