package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import edu.estacio.siscope.bean.CelulaFinanceira;
import edu.estacio.siscope.bean.ItemNotaFiscal;

public class ItemNotaFiscalDAO {

    public static ItemNotaFiscal selectByID(int codigo) {
        Connection c = DBConnection.getConnection();
        ItemNotaFiscal objINF = null;
        if (c == null) {
            return null;
        }
        if (codigo < 1) {
            return null;
        }
        String sql = "SELECT i.id_item_nota_fiscal, i.id_item_pedido, i.id_produto, " + "       i.quantidade, truncate(i.subtotal,2) as subtotal " + "FROM item_nota_fiscal i " + "where i.id_item_nota_fiscal = ? ";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            while (rs.next()) {
                objINF = new ItemNotaFiscal();
                objINF.setCodigo(rs.getInt("id_item_nota_fiscal"));
                objINF.setItemPedido(ItemPedidoDAO.selectByIDFromNotaFiscal(rs.getInt("id_item_pedido")));
                objINF.setProduto(ProdutoDAO.selectByID(rs.getString("id_produto")));
                objINF.setQuantidade(rs.getInt("quantidade"));
                objINF.setSubtotal(rs.getDouble("subtotal"));
            }
        } catch (Exception e) {
            System.out.println("[ItemNotaFiscalDAO.selectByID]" + e.getMessage());
            objINF = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objINF;
    }

    public static ArrayList<ItemNotaFiscal> selectByNotaFiscal(int codigo) {
        Connection c = DBConnection.getConnection();
        ArrayList<ItemNotaFiscal> al = new ArrayList<ItemNotaFiscal>();
        ItemNotaFiscal objINF;
        if (c == null) {
            return null;
        }
        String sql = "SELECT i.id_item_nota_fiscal, i.id_item_pedido, i.id_produto, " + "       i.quantidade, truncate(i.subtotal,2) as subtotal " + "FROM item_nota_fiscal i, produto p " + "where i.id_nota_fiscal = ? " + "and i.id_produto = p.id_produto " + "order by p.nome";
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            while (rs.next()) {
                objINF = new ItemNotaFiscal();
                objINF.setCodigo(rs.getInt("id_item_nota_fiscal"));
                objINF.setItemPedido(ItemPedidoDAO.selectByIDFromNotaFiscal(rs.getInt("id_item_pedido")));
                objINF.setProduto(ProdutoDAO.selectByID(rs.getString("id_produto")));
                objINF.setQuantidade(rs.getInt("quantidade"));
                objINF.setSubtotal(rs.getDouble("subtotal"));
                al.add(objINF);
                objINF = null;
            }
        } catch (Exception e) {
            System.out.println("[ItemNotaFiscalDAO.selectByNotaFiscal]" + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return al;
    }

    public static boolean update(ItemNotaFiscal objINF) {
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        int result;
        CelulaFinanceira objCF = null;
        if (c == null) {
            return false;
        }
        if (objINF == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            String sql = "";
            sql = "update item_nota_fiscal " + "set id_item_pedido = ? " + "where id_item_nota_fiscal = ?";
            pst = c.prepareStatement(sql);
            pst.setInt(1, objINF.getItemPedido().getCodigo());
            pst.setInt(2, objINF.getCodigo());
            result = pst.executeUpdate();
            if (result > 0) {
                if (objINF.getItemPedido().getCelulaFinanceira() != null) {
                    objCF = objINF.getItemPedido().getCelulaFinanceira();
                    objCF.atualizaGastoReal(objINF.getSubtotal());
                    if (CelulaFinanceiraDAO.update(objCF)) {
                    }
                }
            }
            c.commit();
        } catch (final SQLException e) {
            try {
                c.rollback();
            } catch (final Exception e1) {
                System.out.println("[ItemNotaFiscalDAO.update.rollback] Erro ao inserir -> " + e1.getMessage());
            }
            System.out.println("[ItemNotaFiscalDAO.update.insert] Erro ao inserir -> " + e.getMessage());
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
}
