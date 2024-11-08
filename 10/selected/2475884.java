package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import edu.estacio.siscope.bean.CelulaFinanceira;

public class CelulaFinanceiraDAO {

    public static CelulaFinanceira selectByID(String id_celula_financeira) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        CelulaFinanceira objCF = null;
        if (c == null) {
            return null;
        }
        try {
            String sql = "select * from celula_financeira where id_celula_financeira = ?";
            pst = c.prepareStatement(sql);
            pst.setString(1, id_celula_financeira);
            rs = pst.executeQuery();
            if (rs.next()) {
                objCF = new CelulaFinanceira();
                objCF.setCodigo(rs.getInt("id_celula_financeira"));
                objCF.setDescricao(rs.getString("descricao"));
                objCF.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
                objCF.setNaturezaDespesa(NaturezaDespesaDAO.selectByID(rs.getString("id_natureza_despesa")));
                objCF.setProgramaTrabalho(ProgramaTrabalhoDAO.selectByID(rs.getString("id_programa_trabalho")));
                objCF.setUnidadeOrcamentaria(UnidadeOrcamentariaDAO.selectByID(rs.getString("id_unidade_orcamentaria")));
                objCF.setFonteRecursos(FonteRecursosDAO.selectByID(rs.getString("id_fonte_recursos")));
                objCF.setGestao(GestaoDAO.selectByID(rs.getString("id_gestao")));
                objCF.setValorProvisionado(rs.getDouble("valor_provisionado"));
                objCF.setGastoPrevisto(rs.getDouble("gasto_previsto"));
                objCF.setGastoReal(rs.getDouble("gasto_real"));
                objCF.setSaldoPrevisto(rs.getDouble("saldo_previsto"));
                objCF.setSaldoReal(rs.getDouble("saldo_real"));
            }
        } catch (SQLException e) {
            System.out.println("[CelulaFinanceiraDAO.selectByID] Erro ao atualizar -> " + e.getMessage());
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objCF;
    }

    public static boolean insert(final CelulaFinanceira objCelulaFinanceira) {
        int result = 0;
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        if (c == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            final String sql = "insert into celula_financeira " + "(descricao, id_orgao, id_gestao, " + "id_natureza_despesa, id_programa_trabalho, " + "id_unidade_orcamentaria, id_fonte_recursos, " + "valor_provisionado, gasto_previsto, gasto_real, " + "saldo_previsto, saldo_real)" + " values (?, ?, ?, ?, ?, ?, ?, TRUNCATE(?,2), TRUNCATE(?,2), TRUNCATE(?,2), TRUNCATE(?,2), TRUNCATE(?,2))";
            pst = c.prepareStatement(sql);
            pst.setString(1, objCelulaFinanceira.getDescricao());
            pst.setLong(2, (objCelulaFinanceira.getOrgao()).getCodigo());
            pst.setString(3, (objCelulaFinanceira.getGestao()).getCodigo());
            pst.setString(4, (objCelulaFinanceira.getNaturezaDespesa()).getCodigo());
            pst.setString(5, (objCelulaFinanceira.getProgramaTrabalho()).getCodigo());
            pst.setString(6, (objCelulaFinanceira.getUnidadeOrcamentaria()).getCodigo());
            pst.setString(7, (objCelulaFinanceira.getFonteRecursos()).getCodigo());
            pst.setDouble(8, objCelulaFinanceira.getValorProvisionado());
            pst.setDouble(9, objCelulaFinanceira.getGastoPrevisto());
            pst.setDouble(10, objCelulaFinanceira.getGastoReal());
            pst.setDouble(11, objCelulaFinanceira.getSaldoPrevisto());
            pst.setDouble(12, objCelulaFinanceira.getSaldoReal());
            result = pst.executeUpdate();
            c.commit();
        } catch (final SQLException e) {
            try {
                c.rollback();
            } catch (final SQLException e1) {
                System.out.println("[CelulaFinanceiraDAO.insert] Erro ao inserir -> " + e1.getMessage());
            }
            System.out.println("[CelulaFinanceiraDAO.insert] Erro ao inserir -> " + e.getMessage());
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

    public static boolean update(CelulaFinanceira objCelulaFinanceira) {
        int result = 0;
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        if (c == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            final String sql = "update celula_financeira " + " set descricao=?, id_orgao=?, id_gestao=?, " + "id_natureza_despesa=?, id_programa_trabalho=?, " + "id_unidade_orcamentaria=?, id_fonte_recursos=?, " + "valor_provisionado=TRUNCATE(?,2), gasto_previsto=TRUNCATE(?,2), gasto_real=TRUNCATE(?,2), " + "saldo_previsto=TRUNCATE(?,2), saldo_real=TRUNCATE(?,2) " + " where id_celula_financeira = ?";
            pst = c.prepareStatement(sql);
            pst.setString(1, objCelulaFinanceira.getDescricao());
            pst.setLong(2, (objCelulaFinanceira.getOrgao()).getCodigo());
            pst.setString(3, (objCelulaFinanceira.getGestao()).getCodigo());
            pst.setString(4, (objCelulaFinanceira.getNaturezaDespesa()).getCodigo());
            pst.setString(5, (objCelulaFinanceira.getProgramaTrabalho()).getCodigo());
            pst.setString(6, (objCelulaFinanceira.getUnidadeOrcamentaria()).getCodigo());
            pst.setString(7, (objCelulaFinanceira.getFonteRecursos()).getCodigo());
            pst.setString(8, String.valueOf(objCelulaFinanceira.getValorProvisionado()));
            pst.setString(9, String.valueOf(objCelulaFinanceira.getGastoPrevisto()));
            pst.setString(10, String.valueOf(objCelulaFinanceira.getGastoReal()));
            pst.setString(11, String.valueOf(objCelulaFinanceira.getSaldoPrevisto()));
            pst.setString(12, String.valueOf(objCelulaFinanceira.getSaldoReal()));
            pst.setLong(13, objCelulaFinanceira.getCodigo());
            System.out.println(pst.toString());
            result = pst.executeUpdate();
            c.commit();
        } catch (Exception e) {
            System.out.println("[CelulaFinanceiraDAO.update] Erro ao atualizar -> " + e.getMessage());
            result = 0;
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

    public static Collection selectAll() {
        Connection c = DBConnection.getConnection();
        if (c == null) {
            return null;
        }
        String sql = "SELECT * FROM celula_financeira order by id_natureza_despesa";
        Statement st = null;
        ResultSet rs = null;
        ArrayList<CelulaFinanceira> al = new ArrayList<CelulaFinanceira>();
        CelulaFinanceira objCF = null;
        try {
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                objCF = new CelulaFinanceira();
                objCF.setCodigo(rs.getInt("id_celula_financeira"));
                objCF.setDescricao(rs.getString("descricao"));
                objCF.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
                objCF.setNaturezaDespesa(NaturezaDespesaDAO.selectByID(rs.getString("id_natureza_despesa")));
                objCF.setProgramaTrabalho(ProgramaTrabalhoDAO.selectByID(rs.getString("id_programa_trabalho")));
                objCF.setUnidadeOrcamentaria(UnidadeOrcamentariaDAO.selectByID(rs.getString("id_unidade_orcamentaria")));
                objCF.setFonteRecursos(FonteRecursosDAO.selectByID(rs.getString("id_fonte_recursos")));
                objCF.setGestao(GestaoDAO.selectByID(rs.getString("id_gestao")));
                objCF.setValorProvisionado(rs.getDouble("valor_provisionado"));
                objCF.setGastoPrevisto(rs.getDouble("gasto_previsto"));
                objCF.setGastoReal(rs.getDouble("gasto_real"));
                objCF.setSaldoPrevisto(rs.getDouble("saldo_previsto"));
                objCF.setSaldoReal(rs.getDouble("saldo_real"));
                al.add(objCF);
            }
        } catch (SQLException e) {
            System.out.println("[CelulaFinanceiraDAO.selectAll] Erro ao selecionar -> " + e.getMessage());
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        return al;
    }

    public static ArrayList<CelulaFinanceira> selectByOrgao(int codigo) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        CelulaFinanceira objCF = null;
        ArrayList<CelulaFinanceira> al = new ArrayList<CelulaFinanceira>();
        if (c == null) {
            return null;
        }
        try {
            String sql = "select * from celula_financeira where id_orgao = ?";
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            while (rs.next()) {
                objCF = new CelulaFinanceira();
                objCF.setCodigo(rs.getInt("id_celula_financeira"));
                objCF.setDescricao(rs.getString("descricao"));
                objCF.setOrgao(OrgaoDAO.selectByID(rs.getString("id_orgao")));
                objCF.setNaturezaDespesa(NaturezaDespesaDAO.selectByID(rs.getString("id_natureza_despesa")));
                objCF.setProgramaTrabalho(ProgramaTrabalhoDAO.selectByID(rs.getString("id_programa_trabalho")));
                objCF.setUnidadeOrcamentaria(UnidadeOrcamentariaDAO.selectByID(rs.getString("id_unidade_orcamentaria")));
                objCF.setFonteRecursos(FonteRecursosDAO.selectByID(rs.getString("id_fonte_recursos")));
                objCF.setGestao(GestaoDAO.selectByID(rs.getString("id_gestao")));
                objCF.setValorProvisionado(rs.getDouble("valor_provisionado"));
                objCF.setGastoPrevisto(rs.getDouble("gasto_previsto"));
                objCF.setGastoReal(rs.getDouble("gasto_real"));
                objCF.setSaldoPrevisto(rs.getDouble("saldo_previsto"));
                objCF.setSaldoReal(rs.getDouble("saldo_real"));
                al.add(objCF);
                objCF = null;
            }
        } catch (SQLException e) {
            System.out.println("[CelulaFinanceiraDAO.selectByID] Erro ao atualizar -> " + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
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
            String sql = "delete from celula_financeira where id_celula_financeira = " + codigo;
            result = st.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("[CelulaFinanceiraDAO.delete] Erro ao deletar -> " + e.getMessage());
        } finally {
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        if (result > 0) return true; else return false;
    }
}
