package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import edu.estacio.siscope.bean.Orgao;

public class OrgaoDAO {

    public static Orgao selectByID(String id_orgao) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        Orgao objOrgao = null;
        if (c == null) {
            return null;
        }
        try {
            String sql = "select id_orgao, nome, cod_dep_fin from orgao where id_orgao = ?";
            pst = c.prepareStatement(sql);
            pst.setString(1, id_orgao);
            rs = pst.executeQuery();
            if (rs.next()) {
                objOrgao = new Orgao();
                objOrgao.setCodigo(rs.getInt("id_orgao"));
                objOrgao.setNome(rs.getString("nome"));
            }
        } catch (SQLException e) {
            System.out.println("[OrgaoDAO.selectByID] Erro ao atualizar -> " + e.getMessage());
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objOrgao;
    }

    public static Collection selectAll() {
        Connection c = DBConnection.getConnection();
        if (c == null) {
            return null;
        }
        String sql = "select id_orgao, nome from orgao order by nome";
        Statement st = null;
        ResultSet rs = null;
        ArrayList<Orgao> al = new ArrayList<Orgao>();
        Orgao objOrgao = null;
        try {
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                objOrgao = new Orgao();
                objOrgao.setCodigo(rs.getInt("id_orgao"));
                objOrgao.setNome(rs.getString("nome"));
                al.add(objOrgao);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        return al;
    }

    public static boolean update(Orgao orgao) {
        int result = 0;
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        if (c == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            String sql = "update orgao set nome = (?) where id_orgao= ?";
            pst = c.prepareStatement(sql);
            pst.setString(1, orgao.getNome());
            pst.setInt(2, orgao.getCodigo());
            result = pst.executeUpdate();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            System.out.println("[OrgaoDAO.update] Erro ao atualizar -> " + e.getMessage());
        } finally {
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        if (result > 0) {
            return true;
        } else {
            return false;
        }
    }
}
