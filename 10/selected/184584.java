package DAOImplementacao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import Entidade.Disciplina;
import java.sql.CallableStatement;
import java.sql.Types;

/**
 * Classe respons�vel por fazer as querys das Disciplinas
 */
public class DisciplinaDAO {

    private String url = "jdbc:postgresql://localhost/QuestoesProvas";

    private String usuario = "postgres";

    private String senha = "vick01";

    private String driverPostgree = "org.postgresql.Driver";

    private Connection connection;

    /**
	 * M�todo respons�vel por inserir disciplina
	 * @param recebe os dados que o usu�rio digitou
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro no carregamento da class pelo nome
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void inserirDisciplina(Disciplina disciplina) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "INSERT INTO \"Disciplina\" " + "     VALUES ( ?, ? )";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, disciplina.getId());
            stmt.setString(2, disciplina.getNome());
            stmt.execute();
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

    /**
	 * M�todo respons�vel por alterar disciplina 
	 * @param recebe os dados que o usu�rio digitou
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro no carregamento da class pelo nome
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void alterarDisciplina(Disciplina disciplina) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "UPDATE \"Disciplina\"         " + "   SET \"Nome_Disciplina\" = ?" + " WHERE \"ID_Disciplina\"   = ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, disciplina.getNome());
            stmt.setString(2, disciplina.getId());
            stmt.execute();
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

    /**
	 * M�todo respons�vel por remover a disciplina
	 * @param recebe os dados que o usu�rio digitou
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro no carregamento da class pelo nome
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void removerDisciplina(Disciplina disciplina) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "DELETE FROM \"Disciplina\"    " + "      WHERE ID_Disciplina =  ? )";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, disciplina.getId());
            stmt.executeUpdate();
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

    /**
	 * M�todo respons�vel por criar a conex�o com o Banco de Dados
	 * @param autocommit - testa se a conex�o est� sendo usada (TRUE)usando e (FALSE)n�o usando
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro na localiza��o do Drive do BD
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void criaConexao(Boolean autocommit) throws ClassNotFoundException, SQLException {
        try {
            Class.forName(driverPostgree);
            System.out.println("passouCriaConexao");
        } catch (ClassNotFoundException e) {
            throw e;
        }
        try {
            connection = DriverManager.getConnection(url, usuario, senha);
            connection.setAutoCommit(autocommit);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
	 * M�todo respons�vel por fechar a conex�o com o Banco de Dados
	 * @throws SQLException - Arremessa a excess�o se n�o conseguir fechar a conex�o
	 */
    public void fechaConexao() throws SQLException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw e;
        }
    }
}
