package br.uniriotec.pssgbd.gerencia.model.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import br.uniriotec.pssgbd.gerencia.model.dao.BasePostgresDAO;
import br.uniriotec.pssgbd.gerencia.model.dao.DAOFactory;
import br.uniriotec.pssgbd.gerencia.model.entidade.Igreja;
import br.uniriotec.pssgbd.gerencia.model.entidade.Local;

public class IgrejaPostgresDAO extends BasePostgresDAO<Igreja> {

    @Override
    public void incluir(Igreja igreja) throws Exception {
        Connection connection = criaConexao(false);
        String sql = "insert into igreja ? as idlocal, ? as possui_salao;";
        String sql2 = "SELECT MAX(idlocal) FROM Local";
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        ResultSet rs = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt2 = connection.prepareStatement(sql2);
            rs = stmt2.executeQuery();
            stmt.setInt(1, rs.getInt("max"));
            stmt.setBoolean(2, igreja.getPossuiSalao());
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                connection.rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de inserir dados de cliente no banco!");
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            try {
                stmt.close();
                stmt2.close();
                rs.close();
                this.fechaConexao();
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    @Override
    public void alterar(Igreja entidade) throws Exception {
    }

    @Override
    public void excluir(Igreja entidade) throws Exception {
    }

    @Override
    public Igreja consultarPorId(Long id) throws Exception {
        return null;
    }

    @Override
    public List<Igreja> listar() throws Exception {
        Connection connection = criaConexao(false);
        String sql = "SELECT * FROM Igreja";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Igreja igreja = null;
        List<Igreja> listaIgreja = new ArrayList<Igreja>();
        try {
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                igreja = new Igreja();
                igreja.setIdLocal(rs.getInt("idlocal"));
                listaIgreja.add(igreja);
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            try {
                stmt.close();
                rs.close();
                this.fechaConexao();
            } catch (SQLException e) {
                throw e;
            }
        }
        return listaIgreja;
    }
}
