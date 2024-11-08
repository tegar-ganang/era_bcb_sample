package modelo.dao;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import modelo.Dificuldade;
import modelo.Disciplina;
import modelo.Questao;
import modelo.QuestaoDiscursiva;
import modelo.Topico;

/**
 *
 * @author np2tec-07
 */
public class PostgresQuestaoDiscursivaDAO implements IQuestaoDiscursivaDAO {

    private Connection conexao;

    public PostgresQuestaoDiscursivaDAO() {
        try {
            conexao = new Conexao().conectar();
        } catch (Exception ex) {
            Logger.getLogger(PostgresDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void criar(QuestaoDiscursiva q) throws Exception {
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
                System.out.println("questao" + q.getIdQuestao());
            }
            criarQuestaoDiscursiva(q);
            for (Topico item : q.getTopicos()) {
                criarTopicoQuestao(q, item.getIdTopico());
                System.out.println("topico" + item.getNmTopico());
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    public void criarQuestaoDiscursiva(QuestaoDiscursiva q) throws SQLException {
        PreparedStatement stmt = null;
        String sql = "INSERT INTO discursiva (id_questao,gabarito) VALUES (?,?)";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, q.getIdQuestao());
            stmt.setString(2, q.getGabarito());
            stmt.executeUpdate();
            conexao.commit();
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

    @Override
    public void alterar(QuestaoDiscursiva q) throws Exception {
        System.out.println("ALTERAR " + q.getIdQuestao());
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
            alterarQuestaoDiscursiva(q);
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    public void alterarQuestaoDiscursiva(QuestaoDiscursiva q) throws SQLException {
        PreparedStatement stmt = null;
        String sql = "UPDATE discursiva SET  gabarito=? WHERE id_questao=?";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setString(1, q.getGabarito());
            stmt.setInt(2, q.getIdQuestao());
            stmt.executeUpdate();
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public void excluir(QuestaoDiscursiva q) throws Exception {
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
    public QuestaoDiscursiva recuperarQuestao(Disciplina disciplina, Dificuldade dificuldade, List<Topico> pTopicos) throws Exception {
        PreparedStatement stmt = null;
        Boolean existe = false;
        String sql = "SELECT q.id_questao, q.enunciado, d.gabarito, t.nm_topico " + "FROM questao q, discursiva d, topico t, questao_topico qt " + "WHERE q.id_questao = d.id_questao " + "AND q.id_questao = qt.id_questao " + "AND qt.id_topico = t.id_topico " + "AND q.id_disciplina= ? " + "AND q.grau_dificuldade= ? " + "AND t.nm_topico like (?)";
        String topicos = pTopicos.toString();
        topicos = topicos.replace("[", "");
        topicos = topicos.replace("]", "");
        System.out.println("topicos " + topicos);
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, disciplina.getIdDisciplina());
            stmt.setString(2, dificuldade.name());
            stmt.setString(3, topicos);
            ResultSet rs = stmt.executeQuery();
            conexao.commit();
            QuestaoDiscursiva questao = new QuestaoDiscursiva();
            List<Topico> listaTopicos = new ArrayList();
            IDisciplinaDAO disciplinaDAO = DAOFactory.getInstance().getDisciplinaDAO();
            while (rs.next()) {
                existe = true;
                questao.setDisciplina(disciplina);
                questao.setDificuldade(dificuldade);
                questao.setIdQuestao(rs.getInt(1));
                questao.setEnunciado(rs.getString(2));
                questao.setGabarito(rs.getString(3));
                listaTopicos.add(disciplinaDAO.recuperarTopico(disciplina, rs.getString(4)));
            }
            questao.setTopicos(listaTopicos);
            if (existe) {
                return questao;
            }
            throw new Exception("Não existe questão cadastrada com os dados fornecidos.");
        } catch (SQLException e) {
            conexao.rollback();
            throw new SQLException();
        }
    }
}
