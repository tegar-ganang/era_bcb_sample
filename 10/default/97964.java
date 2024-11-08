import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.Vector;

public class GestorPedido {

    GestorDisco gd;

    public GestorPedido() throws errorConexionBD {
        gd = new GestorDisco();
        gd.abrirConexion();
    }

    public int addPedido(Pedido pe) throws errorConexionBD, errorSQL {
        System.out.println("GestorPedido.addPedido()");
        String sql;
        PreparedStatement pstmt = null;
        Statement stmt = null;
        ResultSet rs = null;
        int nuevoId = 0;
        try {
            gd.begin();
            sql = "INSERT INTO pedido(id_cliente, id_promocion, id_producto, " + "importe, fecha) VALUES (?, ?, ?, ?, ?) RETURNING id";
            System.out.println("Ejecuando: " + sql);
            pstmt = gd.getConexion().prepareStatement(sql);
            pstmt.setInt(1, pe.getIdCliente());
            pstmt.setInt(2, pe.getIdPromocion());
            pstmt.setInt(3, pe.getIdProducto());
            pstmt.setFloat(4, pe.getImporte());
            if (pe.getFecha() != null) {
                pstmt.setDate(5, new java.sql.Date(pe.getFecha().getTime()));
            } else {
                pstmt.setNull(5, Types.DATE);
            }
            rs = pstmt.executeQuery();
            System.out.println("Devolviendo el nuevo ID");
            if (rs.next()) {
                nuevoId = rs.getInt(1);
                System.out.println("Nuevo id: " + nuevoId);
            }
            gd.commit();
            System.out.println("commit");
            pstmt.close();
            rs.close();
            return nuevoId;
        } catch (SQLException e) {
            System.err.println("Error en GestorPedido.addPedido()");
            gd.rollback();
            throw new errorSQL(e.toString());
        }
    }

    public void elimina(Pedido pe) throws errorSQL, errorConexionBD {
        System.out.println("GestorPedido.elimina()");
        int id = pe.getId();
        String sql;
        Statement stmt = null;
        try {
            gd.begin();
            sql = "DELETE FROM pedido WHERE id=" + id;
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
            System.err.println("Error en GestorPedido.elimina(): " + e);
        } catch (errorSQL e) {
            System.err.println("Error en GestorPedido.elimina(): " + e);
        }
    }

    public Vector<Pedido> listaPedidosEnCurso() {
        String strSQL = "";
        Pedido ped = null;
        Statement stmt = null;
        return null;
    }

    public static void main(String[] args) {
        try {
            GestorPedido gp = new GestorPedido();
            Pedido pe = new Pedido(0, 1, 0, 0, 10.5f, 2, new Date(), "EN_CURSO");
            gp.elimina(pe);
        } catch (errorConexionBD e) {
            System.err.println("Error: " + e);
            e.printStackTrace();
        } catch (errorSQL e) {
            System.err.println("Error: " + e);
            e.printStackTrace();
        }
    }
}
