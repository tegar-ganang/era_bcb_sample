package modelo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import modelo.Alternativa;
import modelo.Dificuldade;
import modelo.Disciplina;
import modelo.Questao;
import modelo.QuestaoMultiplaEscolha;
import modelo.Topico;

/**
 *
 * @author np2tec-07
 */
public class PostgresQuestaoMultiplaEscolhaDAO implements IQuestaoMultiplaEscolhaDAO {

    private Connection conexao;

    public PostgresQuestaoMultiplaEscolhaDAO() {
        try {
            conexao = new Conexao().conectar();
        } catch (Exception ex) {
            Logger.getLogger(PostgresDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void criar(QuestaoMultiplaEscolha q) throws Exception {
        PreparedStatement stmt = null;
        String sql = "INSERT INTO questao (id_disciplina, enunciado, grau_dificuldade) VALUES (?,?,?) returning id_questao";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, q.getDisciplina().getIdDisciplina());
            stmt.setString(2, q.getEnunciado());
            stmt.setString(3, q.getDificuldade().name());
            ResultSet rs = stmt.executeQuery();
            conexao.commit();
            if (rs.next()) {
                q.setIdQuestao(rs.getInt("id_questao"));
            }
            criarQuestaoMultiplaEscolha(q);
            for (Topico item : q.getTopicos()) {
                criarTopicoQuestao(q, item.getIdTopico());
                System.out.println("topico" + item.getNmTopico());
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    public void criarTopicoQuestao(Questao q, Integer idTopico) throws SQLException {
        PreparedStatement stmt = null;
        String sql = "INSERT INTO questao_topico (id_questao, id_disciplina, id_topico) VALUES (?,?,?)";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, q.getIdQuestao());
            stmt.setInt(2, q.getDisciplina().getIdDisciplina());
            stmt.setInt(3, idTopico);
            stmt.executeUpdate();
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    private void criarQuestaoMultiplaEscolha(QuestaoMultiplaEscolha q) throws SQLException {
        PreparedStatement stmt = null;
        String sql = "INSERT INTO multipla_escolha (id_questao, texto, gabarito) VALUES (?,?,?)";
        try {
            for (Alternativa alternativa : q.getAlternativa()) {
                stmt = conexao.prepareStatement(sql);
                stmt.setInt(1, q.getIdQuestao());
                stmt.setString(2, alternativa.getTexto());
                stmt.setBoolean(3, alternativa.getGabarito());
                stmt.executeUpdate();
                conexao.commit();
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public void alterar(QuestaoMultiplaEscolha q) throws Exception {
        PreparedStatement stmt = null;
        String sql = "UPDATE questao SET id_disciplina=?, enunciado=?, grau_dificuldade=? WHERE id_questao=?";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, q.getDisciplina().getIdDisciplina());
            stmt.setString(2, q.getEnunciado());
            stmt.setString(3, q.getDificuldade().name());
            stmt.setInt(4, q.getIdQuestao());
            stmt.executeUpdate();
            conexao.commit();
            alterarQuestaoMultiplaEscolha(q);
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    public void alterarQuestaoMultiplaEscolha(QuestaoMultiplaEscolha q) throws SQLException {
        PreparedStatement stmt = null;
        String sql = "UPDATE multipla_escolha SET texto=?, gabarito=? WHERE id_questao=?";
        try {
            for (Alternativa alternativa : q.getAlternativa()) {
                stmt = conexao.prepareStatement(sql);
                stmt.setString(1, alternativa.getTexto());
                stmt.setBoolean(2, alternativa.getGabarito());
                stmt.setInt(3, q.getIdQuestao());
                stmt.executeUpdate();
                conexao.commit();
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public void excluir(QuestaoMultiplaEscolha q) throws Exception {
        PreparedStatement stmt = null;
        String sql = "DELETE FROM questao WHERE id_questao=?";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, q.getIdQuestao());
            stmt.executeUpdate();
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public QuestaoMultiplaEscolha recuperarQuestao(Disciplina disciplina, Dificuldade dificuldade, List<Topico> pTopicos) throws Exception {
        PreparedStatement stmt = null;
        Boolean existe = false;
        String sql = "SELECT q.id_questao, q.enunciado, me.gabarito, me.texto, t.nm_topico " + "FROM questao q, multipla_escolha me, topico t, questao_topico qt " + "WHERE q.id_questao = me.id_questao " + "AND qt.id_topico = t.id_topico " + "AND q.id_disciplina= ? " + "AND q.grau_dificuldade= ? " + "AND t.nm_topico like (?)";
        String topicos = pTopicos.toString();
        topicos = topicos.replace("[", "");
        topicos = topicos.replace("]", "");
        System.out.println("topicos" + topicos);
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, disciplina.getIdDisciplina());
            stmt.setString(2, dificuldade.name());
            stmt.setString(3, topicos);
            ResultSet rs = stmt.executeQuery();
            conexao.commit();
            QuestaoMultiplaEscolha questao = new QuestaoMultiplaEscolha();
            List<Topico> listaTopicos = new ArrayList();
            IDisciplinaDAO disciplinaDAO = DAOFactory.getInstance().getDisciplinaDAO();
            ArrayList<Alternativa> alternativas = new ArrayList(5);
            while (rs.next()) {
                existe = true;
                questao.setDisciplina(disciplina);
                questao.setDificuldade(dificuldade);
                questao.setIdQuestao(rs.getInt(1));
                questao.setEnunciado(rs.getString(2));
                Alternativa alternativa = new Alternativa();
                alternativa.setGabarito(rs.getBoolean(3));
                alternativa.setTexto(rs.getString(4));
                alternativas.add(alternativa);
                listaTopicos.add(disciplinaDAO.recuperarTopico(disciplina, rs.getString(4)));
            }
            questao.setAlternativa(alternativas);
            questao.setTopicos(listaTopicos);
            if (existe) {
                return questao;
            }
            throw new Exception("Não existe questão cadastrada com os dados fornecidos.");
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }
}
