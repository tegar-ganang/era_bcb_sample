package br.com.fabrica_ti.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import br.com.fabrica_ti.model.Atividade;
import br.com.fabrica_ti.model.Leilao;
import br.com.fabrica_ti.util.Utils;

public class LeilaoDAO {

    private ConnectionFactory connectionFactory = ConnectionFactory.getInstance();

    public Leilao getLeilao(Leilao leilao) throws SQLException {
        Connection conn = null;
        Leilao leilaoUpdate;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Leilao where idleilao = " + leilao.getIdLeilao();
            leilao = null;
            ResultSet rs = stmt.executeQuery(sqlSelect);
            while (rs.next()) {
                leilao = new Leilao();
                leilao.setIdLeilao(rs.getInt("idleilao"));
                leilao.setDataInicio(Utils.getDateFormat(rs.getString("datainicio"), "yyyy-MM-dd"));
                leilao.setDataFim(Utils.getDateFormat(rs.getString("datafim"), "yyyy-MM-dd"));
                return leilao;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return null;
    }

    public Leilao insertLeilao(Leilao leilao) throws SQLException {
        Connection conn = null;
        String insert = "insert into Leilao (idleilao, atividade_idatividade, datainicio, datafim) " + "values " + "(nextval('seq_leilao'), " + leilao.getAtividade().getIdAtividade() + ", '" + leilao.getDataInicio() + "', '" + leilao.getDataFim() + "')";
        try {
            conn = connectionFactory.getConnection(true);
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(insert);
            if (result == 1) {
                String sqlSelect = "select last_value from seq_leilao";
                ResultSet rs = stmt.executeQuery(sqlSelect);
                while (rs.next()) {
                    leilao.setIdLeilao(rs.getInt("last_value"));
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
        return null;
    }

    public List<Leilao> getLeiloes() throws SQLException {
        List<Leilao> leiloes = new ArrayList<Leilao>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Leilao";
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Leilao leilaoList = null;
            while (rs.next()) {
                leilaoList = new Leilao();
                leilaoList.setIdLeilao(rs.getInt("idleilao"));
                leilaoList.setDataInicio(Utils.getDateFormat(rs.getString("datainicio"), "yyyy-MM-dd"));
                leilaoList.setDataFim(Utils.getDateFormat(rs.getString("datafim"), "yyyy-MM-dd"));
                leiloes.add(leilaoList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return leiloes;
    }

    public void deleteLeilao(Leilao leilao) throws SQLException {
        Connection conn = null;
        String update = "delete from Leilao " + "where " + "atividade_idatividade=" + leilao.getAtividade().getIdAtividade();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    public Leilao getLeilaoPorAtividade(Leilao leilao) throws SQLException {
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Leilao where atividade_idatividade = " + leilao.getAtividade().getIdAtividade();
            leilao = null;
            ResultSet rs = stmt.executeQuery(sqlSelect);
            while (rs.next()) {
                leilao = new Leilao();
                leilao.setIdLeilao(rs.getInt("idleilao"));
                leilao.setDataInicio(Utils.getDateFormat(rs.getString("datainicio"), "yyyy-MM-dd"));
                leilao.setDataFim(Utils.getDateFormat(rs.getString("datafim"), "yyyy-MM-dd"));
                return leilao;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return null;
    }

    public void updateLeilao(Leilao leilao) throws SQLException {
        Connection conn = null;
        String update = "update Leilao set " + "datainicio = '" + leilao.getDataInicio() + "', " + "datafim = '" + leilao.getDataFim() + "' " + "where " + "atividade_idatividade=" + leilao.getAtividade().getIdAtividade();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
            if (result == 0) {
                this.insertLeilao(leilao);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }
}
