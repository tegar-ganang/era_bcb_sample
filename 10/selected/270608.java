package DAOImplementacao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import Entidade.Disciplina;
import Entidade.QuestaoMultiplaEscolha;

/**
 * Classe respons�vel por fazer as querys das Quest�es Multipla Escolha
 */
public class QuestaoMultiplaDAO {

    private String url = "jdbc:postgresql://localhost/QuestoesProvas";

    private String usuario = "postgres";

    private String senha = "vick01";

    private String driverPostgree = "org.postgresql.Driver";

    private Connection connection;

    /**
	 * M�todo respons�vel por inserir quest�o
	 * @param recebe os dados que o usu�rio digitou
	 * @throws ClassNotFoundException - Arremessa a excess�o se houver erro no carregamento da class pelo nome
	 * @throws SQLException - Arremessa a excess�o se houver erro na query do SQL
	 */
    public void inserirQuestaoMultiplaEScolha(QuestaoMultiplaEscolha multiplaEscolha) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "INSERT INTO \"QuestaoMultiplaEscolha\"(\"ID_Questao\", \"Enunciado\", " + "            \"OpcaoEscolha1\", \"OpcaoEscolha2\", \"OpcaoEscolha3\",  " + "            \"RespostaCorreta\", \"ID_Disciplina\", \"ID_Topicos\",   " + "            \"GrauDificuldade\")                                      " + "     VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, multiplaEscolha.getId());
            stmt.setString(2, multiplaEscolha.getEnunciado());
            stmt.setString(3, multiplaEscolha.getOpcaoEscolha1());
            stmt.setString(4, multiplaEscolha.getOpcaoEscolha2());
            stmt.setString(5, multiplaEscolha.getOpcaoEscolha3());
            stmt.setString(6, multiplaEscolha.getResposta());
            stmt.setString(7, multiplaEscolha.getIdDisciplina());
            stmt.setString(8, multiplaEscolha.getIdTopicos());
            stmt.setString(9, multiplaEscolha.getIdTopicos());
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
    public void alterarQuestaoMultiplaEscolha(QuestaoMultiplaEscolha multiplaEscolha) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "UPDATE \"Disciplina\"         " + "   SET \"Nome_Disciplina\" = ?" + " WHERE \"ID_Disciplina\"   = ?";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
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
    public void removerQuestaoMultiplaEscolha(QuestaoMultiplaEscolha multiplaEscolha) throws ClassNotFoundException, SQLException {
        this.criaConexao(false);
        String sql = "DELETE FROM \"Disciplina\"    " + "      WHERE ID_Disciplina =  ? )";
        PreparedStatement stmt = null;
        try {
            stmt = connection.prepareStatement(sql);
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
