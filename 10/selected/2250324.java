package DAOImplementacao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import Entidade.Topicos;

/**
 * Classe respons�vel por fazer as querys dos T�picos
 */
public class TopicosDAO {

    private String url = "jdbc:postgresql://localhost/QuestoesProvas";

    private String usuario = "postgres";

    private String senha = "vick01";

    private String driverPostgree = "org.postgresql.Driver";

    private Connection connection;

    /**
	 * M�todo respons�vel por inserir t�picos
	 * @param recebe os dados que o usu�rio digitou
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro no carregamento da class pelo nome
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void inserirTopicos(Topicos topico) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "INSERT INTO \"Topicos\" " + "     VALUES ( ?, ? , ?)";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, topico.getIdTopicos());
            stmt.setString(2, topico.getNomeTopicos());
            stmt.setString(3, topico.getIdDisciplina());
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
	 * M�todo respons�vel por remover o t�pico
	 * @param recebe os dados que o usu�rio digitou
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro no carregamento da class pelo nome
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void removerTopicos(Topicos topicos) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "DELETE FROM \"Topicos\"    " + "      WHERE \"id_Topicos\" =  ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, topicos.getIdTopicos());
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
