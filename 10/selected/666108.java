package biblioteca.dao;

import biblioteca.dao.connection.ThreadLocalDataSource;
import biblioteca.entidades.ClienteBean;
import biblioteca.dao.interfaces.ClienteInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

/**
 *
 * @author aluno
 */
public class ClienteDAO implements ClienteInterface {

    private Connection connection;

    public ClienteDAO() throws SQLException {
        connection = ThreadLocalDataSource.getInstance().getConnection();
    }

    public void adicionaCliente(ClienteBean cliente) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "insert into cliente(nome,cpf,telefone,cursoCargo,bloqueado,ativo,tipo) values(?,?,?,?,?,?,?)";
        try {
            pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, cliente.getNome());
            pstmt.setString(2, cliente.getCPF());
            pstmt.setString(3, cliente.getTelefone());
            pstmt.setString(4, cliente.getCursoCargo());
            pstmt.setString(5, cliente.getBloqueado());
            pstmt.setString(6, cliente.getAtivo());
            pstmt.setString(7, cliente.getTipo());
            pstmt.executeUpdate();
            rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                cliente.setIdCliente(rs.getLong(1));
            }
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ex1) {
                throw new RuntimeException("Erro ao inserir cliente.", ex1);
            }
            throw new RuntimeException("Erro ao inserir cliente.", ex);
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException ex) {
                throw new RuntimeException("Ocorreu um erro no banco de dados.", ex);
            }
        }
    }

    public void excluirCliente(String cpf) {
        PreparedStatement pstmt = null;
        String sql = "delete from cliente where cpf = ?";
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, cpf);
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ex1) {
                throw new RuntimeException("Erro ao exclir ciente.", ex1);
            }
            throw new RuntimeException("Erro ao excluir cliente.", ex);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Ocorreu um erro no banco de dados.", ex);
            }
        }
    }

    public void alterarCliente(ClienteBean cliente, String cpf) {
        PreparedStatement pstmt = null;
        String sql = "UPDATE cliente SET nome = ?," + "cpf = ?," + "telefone = ?," + "cursoCargo = ?," + "bloqueado = ?," + "ativo = ?," + "tipo = ? WHERE cpf = ?";
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, cliente.getNome());
            pstmt.setString(2, cliente.getCPF());
            pstmt.setString(3, cliente.getTelefone());
            pstmt.setString(4, cliente.getCursoCargo());
            pstmt.setString(5, cliente.getBloqueado());
            pstmt.setString(6, cliente.getAtivo());
            pstmt.setString(7, cliente.getTipo());
            pstmt.setString(8, cpf);
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ex1) {
                throw new RuntimeException("Erro ao atualizar cliente.", ex1);
            }
            throw new RuntimeException("Erro ao atualizar cliente.", ex);
        } finally {
            try {
                if (pstmt != null) pstmt.close();
            } catch (SQLException ex) {
                throw new RuntimeException("Ocorreu um erro no banco de dados.", ex);
            }
        }
    }

    public ClienteBean pesquisarCliente(String cpf) {
        ClienteBean cliente = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "select * from cliente where cpf=?";
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, cpf);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                cliente = new ClienteBean();
                cliente.setIdCliente(rs.getLong("id_cliente"));
                cliente.setNome(rs.getString("nome"));
                cliente.setCPF(rs.getString("cpf"));
                cliente.setBloqueado(rs.getString("bloqueado"));
                cliente.setAtivo(rs.getString("ativo"));
                cliente.setTelefone(rs.getString("telefone"));
                cliente.setCursoCargo(rs.getString("cursoCargo"));
                cliente.setTipo(rs.getString("tipo"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao pesquisar cliente.", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Ocorreu um erro no banco de dados.", ex);
            }
        }
        return cliente;
    }

    public Vector<ClienteBean> listCliente() {
        Vector<ClienteBean> clientes = new Vector<ClienteBean>();
        ClienteBean cliente = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "select * from cliente";
        try {
            pstmt = connection.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                cliente = new ClienteBean();
                cliente.setIdCliente(rs.getLong("id_cliente"));
                cliente.setNome(rs.getString("nome"));
                cliente.setCPF(rs.getString("cpf"));
                cliente.setBloqueado(rs.getString("bloqueado"));
                cliente.setAtivo(rs.getString("ativo"));
                cliente.setTelefone(rs.getString("telefone"));
                cliente.setCursoCargo(rs.getString("cursoCargo"));
                cliente.setTipo(rs.getString("tipo"));
                clientes.add(cliente);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao pesquisar cliente.", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Ocorreu um erro no banco de dados.", ex);
            }
        }
        return clientes;
    }

    public Vector<String> selecinaCursoCadastrado() {
        Vector<String> cursos = new Vector<String>();
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        String sql = "select distinct cursoCargo  from cliente where tipo='A'";
        try {
            pstmt = connection.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                cursos.add(rs.getString(1));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao pesquisar cliente.", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Ocorreu um erro no banco de dados.", ex);
            }
        }
        return cursos;
    }

    public void alterarCliente(ClienteBean cliente) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
