package biblioteca.dao;

import biblioteca.entidades.LivroBean;
import biblioteca.util.ConnectionFactory;
import biblioteca.interfaces.LivroInterface;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aluno
 */
public class LivroDAO implements LivroInterface {

    private Connection connection;

    public LivroDAO() {
        connection = new ConnectionFactory().getConnection();
    }

    public void adicionaLivro(LivroBean livro) {
        String sql = "insert into livro(isbn,autor,editora,edicao,titulo) values(?,?,?,?,?)";
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, livro.getISBN());
            stmt.setString(2, livro.getAutor());
            stmt.setString(3, livro.getEditora());
            stmt.setString(4, livro.getEdicao());
            stmt.setString(5, livro.getTitulo());
            stmt.executeUpdate();
            stmt.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException("erro ao inserir livro", e);
        }
    }

    public void excluirLivro(String ISBN) {
    }

    public void atualizarLivro(LivroBean livro) {
        PreparedStatement pstmt = null;
        String sql = "update livro " + "set " + "isbn = ?, " + "autor = ?, " + "editora = ?, " + "edicao = ?, " + "titulo = ? " + "where " + "isbn = ?";
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, livro.getISBN());
            pstmt.setString(2, livro.getAutor());
            pstmt.setString(3, livro.getEditora());
            pstmt.setString(4, livro.getEdicao());
            pstmt.setString(5, livro.getTitulo());
            pstmt.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException ex1) {
                throw new RuntimeException("Erro ao tentar atualizar livro.", ex1);
            }
            throw new RuntimeException("Erro ao tentar atualizar livro.", ex);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Erro ao tentar atualizar livro.", ex);
            }
        }
    }

    public LivroBean pesquisarLivro(String ISBN) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        LivroBean livro = null;
        String sql = "select " + "id_livro, " + "isbn, " + "autor, " + "editora, " + "edicao, " + "titulo " + "from " + "livro " + "where " + "isbn = ?";
        try {
            pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, ISBN);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                livro = new LivroBean();
                livro.setIdLivro(rs.getLong("id_livro"));
                livro.setISBN(rs.getString("isbn"));
                livro.setAutor(rs.getString("autor"));
                livro.setEditora(rs.getString("editora"));
                livro.setEdicao(rs.getString("edicao"));
                livro.setTitulo(rs.getString("titulo"));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao pesquisar livro.", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Erro ao pesquisar livro.", ex);
            }
        }
        return livro;
    }

    public Vector<LivroBean> listLivros() {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        LivroBean livro = null;
        Vector<LivroBean> livros = new Vector<LivroBean>();
        String sql = "select " + "id_livro, " + "isbn, " + "autor, " + "editora, " + "edicao, " + "titulo " + "from " + "livro ";
        try {
            pstmt = connection.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                livro = new LivroBean();
                livro.setIdLivro(rs.getLong("id_livro"));
                livro.setISBN(rs.getString("isbn"));
                livro.setAutor(rs.getString("autor"));
                livro.setEditora(rs.getString("editora"));
                livro.setEdicao(rs.getString("edicao"));
                livro.setTitulo(rs.getString("titulo"));
                livros.add(livro);
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao pesquisar livro.", ex);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                throw new RuntimeException("Erro ao pesquisar livro.", ex);
            }
        }
        return livros;
    }
}
