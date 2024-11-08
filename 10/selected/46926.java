package br.uniriotec.pssgbd.gerencia.model.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import br.uniriotec.pssgbd.gerencia.model.dao.BasePostgresDAO;
import br.uniriotec.pssgbd.gerencia.model.entidade.Casamento;
import br.uniriotec.pssgbd.gerencia.model.entidade.Cidade;
import br.uniriotec.pssgbd.gerencia.model.entidade.Cliente;
import br.uniriotec.pssgbd.gerencia.model.entidade.Local;

public class LocalPostgresDAO extends BasePostgresDAO<Local> {

    public int getIdUltimoLocal() throws Exception {
        Connection connection = criaConexao(false);
        String sql = "SELECT MAX(idlocal) FROM Local";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        int id = 0;
        try {
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            if (rs.next()) {
                id = rs.getInt("max");
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
        return id;
    }

    @Override
    public void incluir(Local local) throws Exception {
        Connection connection = criaConexao(false);
        String sql = "insert into local select nextval('local_idlocal_seq') as idlocal, ? as numlocal,  ? as nome, ? as idbairro";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setInt(1, local.getNumeroLocal());
            stmt.setString(2, local.getNome());
            stmt.setInt(3, local.getBairro().getIdBairro());
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

    @Override
    public void alterar(Local entidade) throws Exception {
    }

    @Override
    public void excluir(Local entidade) throws Exception {
    }

    @Override
    public Local consultarPorId(Long id) throws Exception {
        return null;
    }

    @Override
    public List<Local> listar() throws Exception {
        Connection connection = criaConexao(false);
        String sql = "SELECT * FROM Local LEFT OUTER JOIN LocalEnder ON Local.idlocal=LocalEnder.idlocalender LEFT OUTER JOIN Igreja ON Igreja.idlocal=Local.idlocal LEFT OUTER JOIN Casa_festas ON Casa_festas.idlocal=Local.idlocal;";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Local local = null;
        List<Local> listaLocal = new ArrayList<Local>();
        try {
            stmt = connection.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                local = new Local();
                local.setNome(rs.getString("nome"));
                local.setAmbiente(rs.getString("ambiente"));
                local.getBairro().setIdBairro(rs.getString("idbairro"));
                local.getBairro().setNome("");
                listaLocal.add(local);
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
        return listaLocal;
    }
}
