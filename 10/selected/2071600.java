package br.uniriotec.pssgbd.gerencia.model.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import br.uniriotec.pssgbd.gerencia.model.dao.BasePostgresDAO;
import br.uniriotec.pssgbd.gerencia.model.entidade.Cidade;
import br.uniriotec.pssgbd.gerencia.model.entidade.Cliente;

/**
 * Implementa��o concreta do BasePostgresDAO de cliente.
 * 
 * 
 * @author Flavio
 *
 */
public class ClientePostgresDAO extends BasePostgresDAO<Cliente> {

    public void alterar(Cliente cliente) throws Exception {
        Connection connection = criaConexao(false);
        String sql = "update cliente set nome = ?, sexo = ?, cod_cidade = ? where cod_cliente = ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getSexo());
            stmt.setInt(3, cliente.getCidade().getCodCidade());
            stmt.setLong(4, cliente.getId());
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                connection.rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de alterar dados de cliente no banco!");
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            try {
                stmt.close();
                this.fechaConexao();
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public void excluir(Cliente cliente) throws Exception {
        Connection connection = criaConexao(false);
        String sql = "delete from cliente where cod_cliente = ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setLong(1, cliente.getId());
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                connection.rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de remover dados de cliente no banco!");
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            try {
                stmt.close();
                this.fechaConexao();
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    public Cliente consultarPorId(Long id) throws Exception {
        Connection connection = criaConexao(false);
        String sql = "select * from cliente cl , cidade ci where cl.cod_cidade = ci.cod_cidade and cod_cliente = ? ";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Cliente cliente = null;
        Cidade cidade = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setInt(1, id.intValue());
            rs = stmt.executeQuery();
            if (rs.next()) {
                cliente = new Cliente();
                cliente.setId(rs.getLong("cod_cliente"));
                cliente.setNome(rs.getString("nome"));
                cliente.setSexo(rs.getString("sexo"));
                cidade = new Cidade();
                cidade.setCodCidade(rs.getInt("cod_cidade"));
                cidade.setNome(rs.getString("nome_cidade"));
                cliente.setCidade(cidade);
            }
        } catch (SQLException e) {
            throw e;
        } finally {
            try {
                if (stmt != null) stmt.close();
                if (rs != null) rs.close();
                this.fechaConexao();
            } catch (SQLException e) {
                throw e;
            }
        }
        return cliente;
    }

    @Override
    public List<Cliente> listar() throws Exception {
        Connection connection = criaConexao(false);
        String sql = "select * from cliente cl , cidade ci where cl.cod_cidade = ci.cod_cidade order by nome";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Cliente cliente = null;
        Cidade cidade = null;
        List<Cliente> listaCliente = new ArrayList<Cliente>();
        try {
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                cliente = new Cliente();
                cliente.setId(rs.getLong("cod_cliente"));
                cliente.setNome(rs.getString("nome"));
                cliente.setSexo(rs.getString("sexo"));
                cidade = new Cidade();
                cidade.setCodCidade(rs.getInt("cod_cidade"));
                cidade.setNome(rs.getString("nome_cidade"));
                cliente.setCidade(cidade);
                listaCliente.add(cliente);
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
        return listaCliente;
    }

    @Override
    public void incluir(Cliente cliente) throws Exception {
        Connection connection = criaConexao(false);
        String sql = "insert into cliente select nextval('sq_cliente') as cod_cliente, ? as nome,  ? as sexo, ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getSexo());
            stmt.setInt(3, cliente.getCidade().getCodCidade());
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
                this.fechaConexao();
            } catch (SQLException e) {
                throw e;
            }
        }
    }
}
