package edu.estacio.siscope.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import edu.estacio.siscope.bean.PedidoSituacao;

public class PedidoSituacaoDAO {

    public static boolean insert(final PedidoSituacao pedidoSituacao) {
        int result = 0;
        final Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        if (c == null) {
            return false;
        }
        try {
            c.setAutoCommit(false);
            final String sql = "insert into pedido_situacao (id_pedido, id_situacao, em, observacao, id_funcionario) " + "values (?, ? , now(), ?, ?) ";
            pst = c.prepareStatement(sql);
            pst.setInt(1, pedidoSituacao.getPedido().getCodigo());
            pst.setInt(2, pedidoSituacao.getSituacao().getCodigo());
            pst.setString(3, pedidoSituacao.getObservacao());
            pst.setInt(4, pedidoSituacao.getFuncionario().getCodigo());
            result = pst.executeUpdate();
            c.commit();
        } catch (final SQLException e) {
            try {
                c.rollback();
            } catch (final SQLException e1) {
                e1.printStackTrace();
            }
            System.out.println("[PedidoSituacaoDAO.insert] Erro ao inserir -> " + e.getMessage());
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

    public static PedidoSituacao selectPedidoSituacaoCorrenteByPedido(int codigo) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        PedidoSituacao objPS = null;
        if (c == null) {
            return null;
        }
        try {
            String sql = "SELECT distinct id_pedido, id_situacao, id_funcionario, em, observacao, " + "DATE_FORMAT(em, '%d/%m/%Y %H:%i:%S') as em_formatada " + "FROM pedido_situacao " + "where id_pedido = ? " + "order by em desc";
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            if (rs.next()) {
                objPS = new PedidoSituacao();
                objPS.setFuncionario(FuncionarioDAO.selectByID(rs.getString("id_funcionario")));
                objPS.setObservacao(rs.getString("observacao"));
                objPS.setSituacao(SituacaoDAO.selectByID(rs.getInt("id_situacao")));
                objPS.setDataSituacao(rs.getDate("em"));
                objPS.setDataSituacaoFormatada(rs.getString("em_formatada"));
            }
        } catch (SQLException e) {
            System.out.println("[PedidoSituacaoDAO.selectPedidoSituacaoCorrenteByPedido] Erro ao atualizar -> " + e.getMessage());
            objPS = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return objPS;
    }

    public static ArrayList<PedidoSituacao> selectPedidoSituacaoByPedido(int codigo) {
        Connection c = DBConnection.getConnection();
        PreparedStatement pst = null;
        ResultSet rs = null;
        PedidoSituacao objPS = null;
        ArrayList<PedidoSituacao> al = null;
        al = new ArrayList<PedidoSituacao>();
        if (c == null) {
            return null;
        }
        try {
            String sql = "SELECT distinct id_pedido, id_situacao, id_funcionario, em, observacao, " + "DATE_FORMAT(em, '%d/%m/%Y %H:%i:%S') as em_formatada " + "FROM pedido_situacao " + "where id_pedido = ? " + "order by em desc";
            pst = c.prepareStatement(sql);
            pst.setInt(1, codigo);
            rs = pst.executeQuery();
            while (rs.next()) {
                objPS = new PedidoSituacao();
                objPS.setFuncionario(FuncionarioDAO.selectByID(rs.getString("id_funcionario")));
                objPS.setObservacao(rs.getString("observacao"));
                objPS.setSituacao(SituacaoDAO.selectByID(rs.getInt("id_situacao")));
                objPS.setDataSituacao(rs.getDate("em"));
                objPS.setDataSituacaoFormatada(rs.getString("em_formatada"));
                al.add(objPS);
                objPS = null;
            }
        } catch (Exception e) {
            System.out.println("[PedidoSituacaoDAO.selectPedidoSituacaoByPedido] Erro ao atualizar -> " + e.getMessage());
            al = null;
        } finally {
            DBConnection.closeResultSet(rs);
            DBConnection.closePreparedStatement(pst);
            DBConnection.closeConnection(c);
        }
        return al;
    }
}
