package br.com.fabrica_ti.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import br.com.fabrica_ti.model.IUsuario;
import br.com.fabrica_ti.model.RecursoHumano;
import br.com.fabrica_ti.model.Requerente;
import br.com.fabrica_ti.model.Usuario;

public class UsuarioDAO {

    private ConnectionFactory connectionFactory = ConnectionFactory.getInstance();

    public Usuario getUsuario(Usuario usuario) throws SQLException {
        Connection conn = null;
        Usuario usuarioUpdate;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Usuario where idUsuario = " + usuario.getIdUsuario();
            usuario = null;
            ResultSet rs = stmt.executeQuery(sqlSelect);
            while (rs.next()) {
                usuarioUpdate = new Usuario();
                usuarioUpdate.setIdUsuario(rs.getInt("idUsuario"));
                usuarioUpdate.setNome(rs.getString("nome"));
                usuarioUpdate.setEmail(rs.getString("email"));
                usuarioUpdate.setTelefone(rs.getString("telefone"));
                usuarioUpdate.setCpf(rs.getString("cpf"));
                usuarioUpdate.setLogin(rs.getString("login"));
                usuarioUpdate.setSenha(rs.getString("senha"));
                return usuarioUpdate;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return null;
    }

    public IUsuario logar(IUsuario usuario) throws SQLException {
        Connection conn = null;
        IUsuario usuarioUpdate = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Usuario where login = '" + usuario.getLogin() + "' and senha='" + usuario.getSenha() + "'";
            usuario = null;
            ResultSet rs = stmt.executeQuery(sqlSelect);
            boolean isRequerente = false;
            while (rs.next()) {
                int idUsuario = rs.getInt("idUsuario");
                Statement stmtRequerente = conn.createStatement();
                String sqlRequerente = "SELECT * FROM Requerente where usuario_idusuario=" + idUsuario;
                ResultSet rsRequerente = stmtRequerente.executeQuery(sqlRequerente);
                while (rsRequerente.next()) {
                    isRequerente = true;
                    if (isRequerente) {
                        usuarioUpdate = new Requerente();
                        ((Requerente) usuarioUpdate).setIdRequerente(rsRequerente.getInt("idrequerente"));
                    }
                }
                if (!isRequerente) {
                    Statement stmtRecursoHumano = conn.createStatement();
                    String sqlRecursoHumano = "SELECT * FROM recurso_humano where usuario_idusuario=" + idUsuario;
                    ResultSet rsRecursoHumano = stmtRecursoHumano.executeQuery(sqlRecursoHumano);
                    while (rsRecursoHumano.next()) {
                        usuarioUpdate = new RecursoHumano();
                        ((RecursoHumano) usuarioUpdate).setIdRecursoHumano(rsRecursoHumano.getInt("idrecursohumano"));
                    }
                }
                usuarioUpdate.setIdUsuario(rs.getInt("idUsuario"));
                usuarioUpdate.setNome(rs.getString("nome"));
                usuarioUpdate.setEmail(rs.getString("email"));
                usuarioUpdate.setTelefone(rs.getString("telefone"));
                usuarioUpdate.setCpf(rs.getString("cpf"));
                usuarioUpdate.setLogin(rs.getString("login"));
                usuarioUpdate.setSenha(rs.getString("senha"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return usuarioUpdate;
    }

    public Usuario insertUsuario(IUsuario usuario) throws SQLException {
        Connection conn = null;
        String insert = "insert into Usuario (idusuario, nome, email, telefone, cpf, login, senha) " + "values " + "(nextval('seq_usuario'), '" + usuario.getNome() + "', '" + usuario.getEmail() + "', " + "'" + usuario.getTelefone() + "', '" + usuario.getCpf() + "', '" + usuario.getLogin() + "', '" + usuario.getSenha() + "')";
        try {
            conn = connectionFactory.getConnection(true);
            conn.setAutoCommit(false);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(insert);
            if (result == 1) {
                String sqlSelect = "select last_value from seq_usuario";
                ResultSet rs = stmt.executeQuery(sqlSelect);
                while (rs.next()) {
                    usuario.setIdUsuario(rs.getInt("last_value"));
                }
                if (usuario instanceof Requerente) {
                    RequerenteDAO requerenteDAO = new RequerenteDAO();
                    requerenteDAO.insertRequerente((Requerente) usuario, conn);
                } else if (usuario instanceof RecursoHumano) {
                    RecursoHumanoDAO recursoHumanoDAO = new RecursoHumanoDAO();
                    recursoHumanoDAO.insertRecursoHumano((RecursoHumano) usuario, conn);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.close();
        }
        return null;
    }

    public void updateUsuario(Usuario usuarioNovo) throws SQLException {
        Connection conn = null;
        String update = "update Usuario set nome='" + usuarioNovo.getNome() + "', " + "email='" + usuarioNovo.getEmail() + "', telefone='" + usuarioNovo.getTelefone() + "', " + "cpf='" + usuarioNovo.getCpf() + "', login='" + usuarioNovo.getLogin() + "', senha='" + usuarioNovo.getSenha() + "' " + "where " + "idUsuario=" + usuarioNovo.getIdUsuario();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }

    public List<Usuario> getUsuarios() throws SQLException {
        List<Usuario> usuarios = new ArrayList<Usuario>();
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            String sqlSelect = "SELECT * FROM Usuario";
            ResultSet rs = stmt.executeQuery(sqlSelect);
            Usuario userList = null;
            while (rs.next()) {
                userList = new Usuario();
                userList.setIdUsuario(rs.getInt("idUsuario"));
                userList.setNome(rs.getString("nome"));
                userList.setEmail(rs.getString("email"));
                userList.setTelefone(rs.getString("telefone"));
                userList.setCpf(rs.getString("cpf"));
                userList.setLogin(rs.getString("login"));
                usuarios.add(userList);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
        return usuarios;
    }

    public void deleteUsuario(Usuario usuario) throws SQLException {
        Connection conn = null;
        String update = "delete from Usuario " + "where " + "idUsuario=" + usuario.getIdUsuario();
        try {
            conn = connectionFactory.getConnection(true);
            Statement stmt = conn.createStatement();
            Integer result = stmt.executeUpdate(update);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            conn.close();
        }
    }
}
