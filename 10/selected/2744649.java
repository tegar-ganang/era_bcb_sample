package util;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public class PortafoglioProcedures {

    private static final String query = "SELECT AZIONE.nome_azione," + "PORTAFOGLIO.quantita, PORTAFOGLIO.prezzo_acquisto " + "FROM AZIONE, PORTAFOGLIO" + " WHERE AZIONE.id=PORTAFOGLIO.azione_id";

    private static final String queryAzione = "SELECT id,nome_azione FROM AZIONE";

    private static final String totQuantita = "SELECT SUM(PORTAFOGLIO.quantita) FROM" + "  PORTAFOGLIO WHERE PORTAFOGLIO.azione_id=? " + "GROUP BY PORTAFOGLIO.azione_id";

    private static final String queryPrezzo = "SELECT " + "PORTAFOGLIO.prezzo_acquisto " + "FROM PORTAFOGLIO" + " WHERE PORTAFOGLIO.azione_id=?";

    private static final String insertAcquisto = " INSERT INTO MOV_ACQUISTO(quantita , data_mov , prezzo_carico , azione_id ) VALUES (?,?,?,?)";

    private static final String delPortafoglio = " DELETE FROM PORTAFOGLIO WHERE azione_id= ?";

    private static final String insertPortafoglio = " INSERT INTO PORTAFOGLIO(azione_id, prezzo_acquisto, quantita) VALUES (?,?,?)";

    private static final String updPortafoglioQuantitaPrezzo = " UPDATE PORTAFOGLIO SET prezzo_acquisto=?,quantita = ? WHERE azione_id = ?";

    private static final String updPortafogliUltimaQuotazione = " UPDATE PORTAFOGLIO SET ultima_quotazione=?, data_ultima_quotazione = ? WHERE azione_id = ?";

    private static final String getQuantita = "SELECT quantita FROM PORTAFOGLIO where azione_id=?";

    private static final String allStoksInBag = "SELECT A.NOME_AZIONE FROM AZIONE A,PORTAFOGLIO P WHERE A.ID=P.AZIONE_ID ORDER BY A.NOME_AZIONE";

    private static final String allStoksIndexInBag = "SELECT A.ID FROM AZIONE A,PORTAFOGLIO P WHERE A.ID=P.AZIONE_ID ORDER BY A.NOME_AZIONE";

    private static final String getNamePriceQuantita = "SELECT A.nome_azione,P.prezzo_acquisto, P.quantita FROM AZIONE A,PORTAFOGLIO P WHERE A.ID=P.AZIONE_ID ORDER BY A.NOME_AZIONE";

    public static PieDataset posDeposito() {
        DefaultPieDataset ds = new DefaultPieDataset();
        MyDBConnection con = new MyDBConnection();
        try {
            con.init();
            ResultSet res = MyDBConnection.executeQuery(getNamePriceQuantita, con.getMyConnection());
            while (res.next()) {
                double carico = res.getDouble(2);
                int tot = res.getInt(3);
                ds.setValue(res.getString(1), carico * tot);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            try {
                con.getMyConnection().rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        } finally {
            con.close();
        }
        return ds;
    }

    public static void nuovoAcquisto(int quantita, Date d, double price, int id) throws SQLException {
        MyDBConnection c = new MyDBConnection();
        c.init();
        Connection conn = c.getMyConnection();
        PreparedStatement ps = conn.prepareStatement(insertAcquisto);
        ps.setInt(1, quantita);
        ps.setDate(2, d);
        ps.setDouble(3, price);
        ps.setInt(4, id);
        ps.executeUpdate();
        double newPrice = price;
        int newQ = quantita;
        ResultSet rs = MyDBConnection.executeQuery(queryPrezzo.replace("?", "" + id), conn);
        if (rs.next()) {
            int oldQ = rs.getInt(1);
            double oldPrice = rs.getDouble(2);
            newQ = quantita + oldQ;
            newPrice = (oldPrice * oldQ + price * quantita) / newQ;
            updatePortafoglio(conn, newPrice, newQ, id);
        } else insertPortafoglio(conn, id, newPrice, newQ);
        try {
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("Effettuato rollback dopo " + e.getMessage());
        } finally {
            c.close();
        }
    }

    public static double prezzoCarico(Connection c, int quote_id) {
        try {
            PreparedStatement ps = c.prepareStatement(queryPrezzo);
            ps.setInt(1, quote_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Double.NaN;
    }

    public static double valTot() throws SQLException {
        MyDBConnection c = new MyDBConnection();
        double tot = 0;
        try {
            c.init();
            ResultSet rs = MyDBConnection.executeQuery("SELECT quantita, prezzo_acquisto FROM PORTAFOGLIO", c.getMyConnection());
            while (rs.next()) {
                tot += rs.getInt(1) * rs.getDouble(2);
            }
        } catch (SQLException e) {
            c.getMyConnection().rollback();
            throw e;
        } finally {
            c.close();
        }
        return tot;
    }

    /**
	 * This method take an array containing the name of the stoks contained in
	 * pocketbook
	 * 
	 * @return java.lang.String[]
	 */
    public String[] getStoksInBag() {
        ArrayList<String> list = new ArrayList<String>();
        MyDBConnection con = new MyDBConnection();
        try {
            con.init();
            ResultSet rs = MyDBConnection.executeQuery(allStoksInBag, con.getMyConnection());
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[] { "" };
        } finally {
            con.close();
        }
        return list.toArray(new String[list.size()]);
    }

    /**
	 * This method take an array containing the index of the stoks contained in
	 * pocketbook
	 * 
	 * @return java.lang.String[]
	 */
    public String[] getStoksIndexesBag() {
        ArrayList<String> list = new ArrayList<String>();
        MyDBConnection con = new MyDBConnection();
        try {
            con.init();
            ResultSet rs = MyDBConnection.executeQuery(allStoksIndexInBag, con.getMyConnection());
            while (rs.next()) list.add(rs.getString(1));
        } catch (SQLException e) {
            e.printStackTrace();
            return new String[] { "" };
        } finally {
            con.close();
        }
        return list.toArray(new String[list.size()]);
    }

    private static void deletePortafoglio(Connection conn, int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(delPortafoglio);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    private static void insertPortafoglio(Connection conn, int id, double p, int q) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(insertPortafoglio);
        ps.setInt(1, id);
        ps.setDouble(2, p);
        ps.setInt(3, q);
        ps.executeUpdate();
    }

    public static void updatePortafoglio(Connection conn, double p, int q, int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(updPortafoglioQuantitaPrezzo);
        ps.setDouble(1, p);
        ps.setInt(2, q);
        ps.setInt(3, id);
        ps.executeUpdate();
    }

    public static void updQuotazione(Connection conn, double p, Date date, int id) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(updPortafogliUltimaQuotazione);
        ps.setDouble(1, p);
        ps.setDate(2, date);
        ps.setInt(3, id);
        ps.executeUpdate();
    }

    public static int quantita(Connection c, int azione) throws SQLException {
        PreparedStatement ps = c.prepareStatement(getQuantita);
        ps.setInt(1, azione);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1); else return -1;
    }
}
