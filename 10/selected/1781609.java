package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import edu.estacio.siscope.bean.Departamento;

public class DepartamentoDAO {

    public static boolean insert(final Departamento ObjDepartamento) {
        int result = 0;
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        if (c == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            final String sql = "insert into departamento " + "(nome, sala, telefone, id_orgao)" + " values (?, ?, ?, ?)";
            pst = c.prepareStatement(sql);
            pst.setString(1, ObjDepartamento.getNome());
            pst.setString(2, ObjDepartamento.getSala());
            pst.setString(3, ObjDepartamento.getTelefone());
            pst.setInt(4, (ObjDepartamento.getOrgao()).getCodigo());
            result = pst.executeUpdate();
            c.commit();
        } catch (final SQLException e) {
            try {
                c.rollback();
            } catch (final SQLException e1) {
                e1.printStackTrace();
            }
            System.out.println("[DepartamentoDAO.insert] Erro ao inserir -> " + e.getMessage());
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

    public static boolean update(Departamento objDepartamento) {
        int result = 0;
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        if (c == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            String sql = "update departamento set nome = ?, sala = ?, telefone = ?, id_orgao = ? where id_departamento= ?";
            pst = c.prepareStatement(sql);
            pst.setString(1, objDepartamento.getNome());
            pst.setString(2, objDepartamento.getSala());
            pst.setString(3, objDepartamento.getTelefone());
            pst.setLong(4, (objDepartamento.getOrgao()).getCodigo());
            pst.setInt(5, objDepartamento.getCodigo());
            result = pst.executeUpdate();
            c.commit();
        } catch (SQLException e) {
            try {
                c.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
            System.out.println("[DepartamentoDAO.update] Erro ao atualizar -> " + e.getMessage());
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

    public static Departamento selectByID(String id_departamento) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        Departamento objDepartamento = null;
        if (c == null) {
            return null;
        }
        try {
            String sql = "Select id_departamento, nome, sala, telefone, id_orgao from departamento where id_departamento = ?";
            pst = c.prepareStatement(sql);
            pst.setString(1, id_departamento);
            rs = pst.executeQuery();
            if (rs.next()) {
                objDepartamento = new Departamento();
                objDepartamento.setCodigo(rs.getInt("id_departamento"));
                objDepartamento.setNome(rs.getString("nome"));
                objDepartamento.setSala(rs.getString("sala"));
                objDepartamento.setTelefone(rs.getString("telefone"));
                objDepartamento.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
            }
        } catch (SQLException e) {
            System.out.println("[DepartamentoDAO.selectByID] Erro ao atualizar -> " + e.getMessage());
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objDepartamento;
    }

    public static Collection selectAll() {
        Connection c = DBConnection.getConnection();
        if (c == null) {
            return null;
        }
        String sql = "select id_departamento, nome, sala, telefone, id_orgao from departamento order by nome";
        Statement st = null;
        ResultSet rs = null;
        ArrayList<Departamento> al = new ArrayList<Departamento>();
        Departamento objDepartamento = null;
        try {
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                objDepartamento = new Departamento();
                objDepartamento.setCodigo(rs.getInt("id_Departamento"));
                objDepartamento.setNome(rs.getString("nome"));
                objDepartamento.setSala(rs.getString("sala"));
                objDepartamento.setTelefone(rs.getString("telefone"));
                objDepartamento.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
                al.add(objDepartamento);
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

    public static boolean delete(String codigo) {
        int result = 0;
        Connection c = DBConnection.getConnection();
        Statement st = null;
        if (c == null) {
            return false;
        }
        try {
            st = c.createStatement();
            String sql = "delete from departamento where id_departamento = " + codigo;
            result = st.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("[DepartamentoDAO.delete] Erro ao deletar -> " + e.getMessage());
        } finally {
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        if (result > 0) return true; else return false;
    }

    public static Departamento selectByFuncionario(int codigo) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        Departamento objDepartamento = null;
        if (c == null) {
            return null;
        }
        try {
            String sql = "SELECT distinct d.id_departamento, d.id_orgao, d.telefone " + ", d.sala, d.nome, d.cod_depto " + "FROM alocacao a, departamento d " + "where a.id_departamento = d.id_departamento " + "and a.id_funcionario = ? " + "and data_fim is null";
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            if (rs.next()) {
                objDepartamento = new Departamento();
                objDepartamento.setCodigo(rs.getInt("id_departamento"));
                objDepartamento.setNome(rs.getString("nome"));
                objDepartamento.setSala(rs.getString("sala"));
                objDepartamento.setTelefone(rs.getString("telefone"));
                objDepartamento.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
            }
        } catch (SQLException e) {
            System.out.println("[DepartamentoDAO.selectByID] Erro ao atualizar -> " + e.getMessage());
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objDepartamento;
    }

    public static ArrayList<Departamento> selectByOrgao(int codigo) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        Departamento objDepartamento = null;
        ArrayList<Departamento> al = new ArrayList<Departamento>();
        if (c == null) {
            return null;
        }
        try {
            String sql = "SELECT distinct d.id_departamento, d.id_orgao, d.telefone " + ", d.sala, d.nome, d.cod_depto " + "FROM departamento d " + "where d.id_orgao = ? " + "order by d.nome";
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            while (rs.next()) {
                objDepartamento = new Departamento();
                objDepartamento.setCodigo(rs.getInt("id_departamento"));
                objDepartamento.setNome(rs.getString("nome"));
                objDepartamento.setSala(rs.getString("sala"));
                objDepartamento.setTelefone(rs.getString("telefone"));
                objDepartamento.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
                al.add(objDepartamento);
                objDepartamento = null;
            }
        } catch (SQLException e) {
            System.out.println("[DepartamentoDAO.selectByID] Erro ao atualizar -> " + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return al;
    }
}
