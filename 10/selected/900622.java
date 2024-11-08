package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import edu.estacio.siscope.bean.ItemPedido;
import edu.estacio.siscope.bean.Pedido;

public class PedidoDAO {

    public static Pedido insert(Pedido objPedido) {
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        int result;
        if (c == null) {
            return null;
        }
        try {
            c.setAutoCommit(false);
            String sql = "";
            int idPedido;
            idPedido = PedidoDAO.getLastCodigo();
            if (idPedido < 1) {
                return null;
            }
            sql = "insert into pedido " + "(id_pedido, id_funcionario,data_pedido,valor) " + "values(?,?,now(),truncate(?,2))";
            pst = c.prepareStatement(sql);
            pst.setInt(1, idPedido);
            pst.setInt(2, objPedido.getFuncionario().getCodigo());
            pst.setString(3, new DecimalFormat("#0.00").format(objPedido.getValor()));
            result = pst.executeUpdate();
            pst = null;
            if (result > 0) {
                Iterator<ItemPedido> itItemPedido = (objPedido.getItemPedido()).iterator();
                while ((itItemPedido != null) && (itItemPedido.hasNext())) {
                    ItemPedido objItemPedido = (ItemPedido) itItemPedido.next();
                    sql = "";
                    sql = "insert into item_pedido " + "(id_pedido,id_produto,quantidade,subtotal) " + "values (?,?,?,truncate(?,2))";
                    pst = c.prepareStatement(sql);
                    pst.setInt(1, idPedido);
                    pst.setInt(2, (objItemPedido.getProduto()).getCodigo());
                    pst.setInt(3, objItemPedido.getQuantidade());
                    pst.setString(4, new DecimalFormat("#0.00").format(objItemPedido.getSubtotal()));
                    result = pst.executeUpdate();
                }
            }
            pst = null;
            sql = "";
            sql = "insert into pedido_situacao " + "(id_pedido,id_situacao, em, observacao, id_funcionario) " + "values (?,?,now(), ?, ?)";
            pst = c.prepareStatement(sql);
            pst.setInt(1, idPedido);
            pst.setInt(2, 1);
            pst.setString(3, "Inclus�o de pedido");
            pst.setInt(4, objPedido.getFuncionario().getCodigo());
            result = pst.executeUpdate();
            pst = null;
            sql = "";
            sql = "insert into tramitacao " + "(data_tramitacao, id_pedido, id_dep_origem, id_dep_destino) " + "values (now(),?,?, ?)";
            pst = c.prepareStatement(sql);
            pst.setInt(1, idPedido);
            pst.setInt(2, 6);
            pst.setInt(3, 2);
            result = pst.executeUpdate();
            c.commit();
            objPedido.setCodigo(idPedido);
        } catch (final Exception e) {
            try {
                c.rollback();
            } catch (final Exception e1) {
                System.out.println("[PedidoDAO.insert] Erro ao inserir -> " + e1.getMessage());
            }
            System.out.println("[PedidoDAO.insert] Erro ao inserir -> " + e.getMessage());
        } finally {
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objPedido;
    }

    private static int getLastCodigo() {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        int ret = -1;
        if (c == null) {
            return ret;
        }
        try {
            String sql = "select max(id_pedido) from pedido";
            pst = c.prepareStatement(sql);
            rs = pst.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    ret = rs.getInt(1) + 1;
                } else {
                    ret = 1;
                }
            }
        } catch (Exception e) {
            System.out.println("[PedidoDAO.getLastCodigo] Erro ao atualizar -> " + e.getMessage());
            ret = -1;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return ret;
    }

    public static Collection selectAll() {
        Connection c = DBConnection.getConnection();
        if (c == null) {
            return null;
        }
        String sql = "SELECT id_pedido, id_funcionario, data_pedido, valor, " + "DATE_FORMAT(data_pedido, '%d/%m/%Y %H:%i:%S') as data_pedido_formatada " + "FROM pedido order by id_pedido";
        Statement st = null;
        ResultSet rs = null;
        ArrayList<Pedido> al = new ArrayList<Pedido>();
        Pedido objPedido = null;
        try {
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                objPedido = new Pedido();
                objPedido.setCodigo(rs.getInt("id_pedido"));
                objPedido.setData(rs.getDate("data_pedido"));
                objPedido.setDataFormatada(rs.getString("data_pedido_formatada"));
                objPedido.setValor(rs.getDouble("valor"));
                objPedido.setFuncionario(FuncionarioDAO.selectByID(rs.getString("id_funcionario")));
                objPedido.setItemPedido(ItemPedidoDAO.selectAllByPedido(rs.getString("id_pedido")));
                objPedido.setSituacaoCorrente(PedidoSituacaoDAO.selectPedidoSituacaoCorrenteByPedido(rs.getInt("id_pedido")));
                objPedido.setSituacao(PedidoSituacaoDAO.selectPedidoSituacaoByPedido(rs.getInt("id_pedido")));
                objPedido.setTramitacaoCorrente(TramitacaoDAO.selectTramitacaoCorrenteByPedido(rs.getInt("id_pedido")));
                al.add(objPedido);
            }
        } catch (Exception e) {
            System.out.println("[PedidoDAO.selectAll]" + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        return al;
    }

    public static Pedido selectByID(int codigo) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        Pedido objPedido = null;
        if (c == null) {
            return null;
        }
        if (codigo < 1) {
            return null;
        }
        try {
            String sql = "select id_pedido, id_funcionario, data_pedido, valor, " + "DATE_FORMAT(data_pedido, '%d/%m/%Y %H:%i:%S') as data_pedido_formatada " + "from pedido " + "where id_pedido = ?";
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            if (rs.next()) {
                objPedido = new Pedido();
                objPedido.setCodigo(rs.getInt("id_pedido"));
                objPedido.setData(rs.getDate("data_pedido"));
                objPedido.setValor(rs.getDouble("valor"));
                objPedido.setDataFormatada(rs.getString("data_pedido_formatada"));
                objPedido.setFuncionario(FuncionarioDAO.selectByID(rs.getString("id_funcionario")));
                objPedido.setItemPedido(ItemPedidoDAO.selectAllByPedido(rs.getString("id_pedido")));
                objPedido.setSituacaoCorrente(PedidoSituacaoDAO.selectPedidoSituacaoCorrenteByPedido(rs.getInt("id_pedido")));
                objPedido.setSituacao(PedidoSituacaoDAO.selectPedidoSituacaoByPedido(rs.getInt("id_pedido")));
                objPedido.setTramitacaoCorrente(TramitacaoDAO.selectTramitacaoCorrenteByPedido(rs.getInt("id_pedido")));
                objPedido.setTramitacao(TramitacaoDAO.selectTramitacaoByPedido(rs.getInt("id_pedido")));
            }
        } catch (SQLException e) {
            System.out.println("[PedidoDAO.getPedidoById] Erro ao atualizar -> " + e.getMessage());
            objPedido = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objPedido;
    }

    public static Collection selectFiltrado(String dataInicial, String dataFinal) {
        Connection c = DBConnection.getConnection();
        String complemento = "WHERE";
        if (c == null) {
            return null;
        }
        String dia = dataInicial.substring(0, 2);
        String mes = dataInicial.substring(3, 5);
        String ano = dataInicial.substring(6);
        dataInicial = ano + "/" + mes + "/" + dia;
        dia = dataFinal.substring(0, 2);
        mes = dataFinal.substring(3, 5);
        ano = dataFinal.substring(6);
        dataFinal = ano + "/" + mes + "/" + dia;
        String sql = "SELECT id_pedido, id_funcionario, data_pedido, valor, " + "DATE_FORMAT(data_pedido, '%d/%m/%Y %H:%i:%S') as data_pedido_formatada " + "FROM pedido ";
        if (dataInicial != "" && dataFinal != "") {
            complemento = complemento + " data_pedido >= \"" + dataInicial + "\" AND data_pedido <= \"" + dataFinal + "\"";
        }
        if (complemento != "WHERE") {
            sql = sql + " " + complemento;
        }
        sql = sql + " order by id_pedido ";
        Statement st = null;
        ResultSet rs = null;
        ArrayList<Pedido> al = new ArrayList<Pedido>();
        Pedido objPedido = null;
        try {
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                objPedido = new Pedido();
                objPedido.setCodigo(rs.getInt("id_pedido"));
                objPedido.setData(rs.getDate("data_pedido"));
                objPedido.setDataFormatada(rs.getString("data_pedido_formatada"));
                objPedido.setValor(rs.getDouble("valor"));
                objPedido.setFuncionario(FuncionarioDAO.selectByID(rs.getString("id_funcionario")));
                objPedido.setItemPedido(ItemPedidoDAO.selectAllByPedido(rs.getString("id_pedido")));
                objPedido.setSituacaoCorrente(PedidoSituacaoDAO.selectPedidoSituacaoCorrenteByPedido(rs.getInt("id_pedido")));
                objPedido.setSituacao(PedidoSituacaoDAO.selectPedidoSituacaoByPedido(rs.getInt("id_pedido")));
                objPedido.setTramitacaoCorrente(TramitacaoDAO.selectTramitacaoCorrenteByPedido(rs.getInt("id_pedido")));
                al.add(objPedido);
            }
        } catch (Exception e) {
            System.out.println("[PedidoDAO.selectAll]" + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closeStatement(st);
            DBConnection.closeConnection(c);
        }
        return al;
    }
}
