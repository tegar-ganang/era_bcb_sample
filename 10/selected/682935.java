package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import edu.estacio.siscope.bean.ItemNotaFiscal;
import edu.estacio.siscope.bean.NotaFiscal;

public class NotaFiscalDAO {

    public static ArrayList<NotaFiscal> selectAll() {
        Connection c = DBConnection.getConnection();
        ArrayList<NotaFiscal> al = new ArrayList<NotaFiscal>();
        if (c == null) {
            return null;
        }
        String sql = "SELECT id_nota_fiscal, id_fornecedor, " + "   data_emissao, data_cadastro, numero, truncate(total,2) as total, " + "   DATE_FORMAT(data_emissao, '%d/%m/%Y %H:%i:%S') as data_emissao_formatada, " + "   DATE_FORMAT(data_cadastro, '%d/%m/%Y %H:%i:%S') as data_cadastro_formatada " + "FROM nota_fiscal " + "ORDER BY id_nota_fiscal";
        Statement st = null;
        ResultSet rs = null;
        NotaFiscal objNF = null;
        try {
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                objNF = new NotaFiscal();
                objNF.setCodigo(rs.getInt("id_nota_fiscal"));
                objNF.setDataCadastro(rs.getDate("data_cadastro"));
                objNF.setDataCadastroFormatada(rs.getString("data_cadastro_formatada"));
                objNF.setDataEmissao(rs.getDate("data_emissao"));
                objNF.setDataEmissaoFormatada(rs.getString("data_emissao_formatada"));
                objNF.setValor(rs.getDouble("total"));
                objNF.setFornecedor(FornecedorDAO.selectByID(rs.getString("id_fornecedor")));
                objNF.setNumero(rs.getString("numero"));
                al.add(objNF);
                objNF = null;
            }
        } catch (Exception e) {
            System.out.println("[NotaFiscalDAO.selectAll]" + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        return al;
    }

    public static NotaFiscal selectByID(int codigo) {
        Connection c = DBConnection.getConnection();
        if (c == null) {
            return null;
        }
        String sql = "SELECT id_nota_fiscal, id_fornecedor, " + "data_emissao, data_cadastro, numero, truncate(total,2) as valor, " + "DATE_FORMAT(data_emissao, '%d/%m/%Y %H:%i:%S') as data_emissao_formatada, " + "DATE_FORMAT(data_cadastro, '%d/%m/%Y %H:%i:%S') as data_cadastro_formatada " + "FROM nota_fiscal " + "WHERE id_nota_fiscal = ?";
        PreparedStatement pst = null;
        ResultSet rs = null;
        NotaFiscal objNF = null;
        try {
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            if (rs.next()) {
                objNF = new NotaFiscal();
                objNF.setCodigo(rs.getInt("id_nota_fiscal"));
                objNF.setDataCadastro(rs.getDate("data_cadastro"));
                objNF.setDataCadastroFormatada(rs.getString("data_cadastro_formatada"));
                objNF.setDataEmissao(rs.getDate("data_emissao"));
                objNF.setDataEmissaoFormatada(rs.getString("data_emissao_formatada"));
                objNF.setValor(rs.getDouble("valor"));
                objNF.setFornecedor(FornecedorDAO.selectByID(rs.getString("id_fornecedor")));
                objNF.setItemNotaFiscal(ItemNotaFiscalDAO.selectByNotaFiscal(rs.getInt("id_nota_fiscal")));
                objNF.setNumero(rs.getString("numero"));
            }
        } catch (Exception e) {
            System.out.println("[NotaFiscalDAO.selectByID]" + e.getMessage());
            objNF = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objNF;
    }

    public static NotaFiscal insert(NotaFiscal objNF) {
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        int result;
        if (c == null) {
            return null;
        }
        if (objNF == null) {
            return null;
        }
        try {
            c.setAutoCommit(false);
            String sql = "";
            int idNotaFiscal;
            idNotaFiscal = NotaFiscalDAO.getLastCodigo();
            if (idNotaFiscal < 1) {
                return null;
            }
            sql = "INSERT INTO nota_fiscal " + "(id_nota_fiscal, id_fornecedor, total, data_emissao, data_cadastro, numero) " + "VALUES(?, ?, TRUNCATE(?,2), STR_TO_DATE(?,'%d/%m/%Y'), now(), ?) ";
            pst = c.prepareStatement(sql);
            pst.setInt(1, idNotaFiscal);
            pst.setLong(2, objNF.getFornecedor().getCodigo());
            pst.setString(3, new DecimalFormat("#0.00").format(objNF.getValor()));
            pst.setString(4, objNF.getDataEmissaoFormatada());
            pst.setString(5, objNF.getNumero());
            result = pst.executeUpdate();
            pst = null;
            if (result > 0) {
                Iterator<ItemNotaFiscal> itINF = (objNF.getItemNotaFiscal()).iterator();
                while ((itINF != null) && (itINF.hasNext())) {
                    ItemNotaFiscal objINF = (ItemNotaFiscal) itINF.next();
                    sql = "";
                    sql = "INSERT INTO item_nota_fiscal " + "(id_nota_fiscal, id_produto, quantidade, subtotal) " + "VALUES(?, ?, ?, TRUNCATE(?,2))";
                    pst = c.prepareStatement(sql);
                    pst.setInt(1, idNotaFiscal);
                    pst.setInt(2, objINF.getProduto().getCodigo());
                    pst.setInt(3, objINF.getQuantidade());
                    pst.setString(4, new DecimalFormat("#0.00").format(objINF.getSubtotal()));
                    result = pst.executeUpdate();
                }
            }
            c.commit();
            objNF.setCodigo(idNotaFiscal);
        } catch (final Exception e) {
            try {
                c.rollback();
            } catch (final Exception e1) {
                System.out.println("[NotaFiscalDAO.insert.rollback] Erro ao inserir -> " + e1.getMessage());
            }
            System.out.println("[NotaFiscalDAO.insert] Erro ao inserir -> " + e.getMessage());
            objNF = null;
        } finally {
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objNF;
    }

    private static int getLastCodigo() {
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        int ret = -1;
        if (c == null) {
            return ret;
        }
        try {
            final String sql = "select max(id_nota_fiscal) from nota_fiscal";
            pst = c.prepareStatement(sql);
            rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    ret = rs.getInt(1) + 1;
                } else {
                    ret = 1;
                }
            }
        } catch (final Exception e) {
            System.out.println("[NotaFiscalDAO.getLastCodigo] Erro ao atualizar -> " + e.getMessage());
            ret = -1;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return ret;
    }
}
