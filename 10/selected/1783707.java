package dbrouter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Persistencia {

    private static int servidorConsulta = -1;

    ArrayList<Connection> conexoes = new ArrayList<Connection>();

    private Statement stm;

    public Persistencia() {
        try {
            Class.forName("org.sqlite.JDBC");
            Properties prop = new Properties();
            prop.put("charSet", "UTF-8");
            TradutorXML trad = new TradutorXML();
            ArrayList<BancoDados> bancos = trad.traduzir();
            for (int i = 0; i < bancos.size(); i++) {
                Connection conn;
                conn = DriverManager.getConnection("jdbc:sqlite:" + bancos.get(i).getLocal(), prop);
                conn.setAutoCommit(false);
                conexoes.add(conn);
            }
        } catch (Exception ex) {
            Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void executar(String sql) {
        this.executar(sql, false);
    }

    public int executar(String sql, Boolean retornaAutoIncremento) {
        int autoIncremento = 0;
        try {
            for (Connection conn : conexoes) {
                stm = conn.createStatement();
                stm.executeUpdate(sql);
            }
            for (Connection conn : conexoes) {
                conn.commit();
            }
        } catch (Exception ex) {
            try {
                for (Connection conn : conexoes) {
                    conn.rollback();
                }
                return 0;
            } catch (SQLException Sqlex) {
                Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, Sqlex);
            }
        }
        if (retornaAutoIncremento) autoIncremento = getUltimoIdentificador();
        return autoIncremento;
    }

    public ResultSet executarConsulta(String consultaSql) {
        ResultSet rs = null;
        try {
            servidorConsulta = (servidorConsulta + 1) % conexoes.size();
            System.out.println("Executando consulta no servidor: " + servidorConsulta);
            Connection conexaoAtual = conexoes.get(servidorConsulta);
            stm = conexaoAtual.createStatement();
            rs = stm.executeQuery(consultaSql);
        } catch (SQLException ex) {
            Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rs;
    }

    private int getUltimoIdentificador() {
        int ultimoNumero = 0;
        try {
            String sqlConsulta = "select last_insert_rowid()";
            ResultSet rs = this.executarConsulta(sqlConsulta);
            while (rs.next()) {
                ultimoNumero = rs.getInt(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(BaseModelo.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ultimoNumero;
    }

    @Override
    public void finalize() {
        try {
            super.finalize();
            stm = null;
            for (Connection conn : conexoes) {
                conn.close();
            }
        } catch (Throwable ex) {
            Logger.getLogger(Persistencia.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
