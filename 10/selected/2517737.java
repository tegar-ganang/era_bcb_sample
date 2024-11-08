package modelo.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import modelo.Disciplina;
import modelo.Topico;

/**
 *
 * @author Cinthia
 */
public class PostgresDisciplinaDAO implements IDisciplinaDAO {

    public static final String MSG_NOME_DISCIPLINA_EXISTE = "Já existe disciplina cadastrada com esse nome.";

    public static final String MSG_COD_DISCIPLINA_EXISTE = "Já existe disciplina cadastrada com esse código.";

    private Connection conexao;

    public PostgresDisciplinaDAO() {
        try {
            conexao = new Conexao().conectar();
        } catch (Exception ex) {
            Logger.getLogger(PostgresDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void criar(Disciplina t) throws Exception {
        checkNomeDisciplina(t);
        checkCodDisciplina(t);
        PreparedStatement stmt = null;
        String sql = "INSERT INTO disciplina (nm_disciplina,cod_disciplina) values (?,?) returning id_disciplina;";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setString(1, t.getNomeDisciplina());
            stmt.setString(2, String.valueOf(t.getCodDisciplina()));
            ResultSet rs = stmt.executeQuery();
            conexao.commit();
            int id_disciplina = 0;
            if (rs.next()) {
                id_disciplina = rs.getInt("id_disciplina");
            }
            for (Topico item : t.getTopicos()) {
                criarTopico(item, id_disciplina);
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public void alterar(Disciplina t) throws Exception {
        PreparedStatement stmt = null;
        String sql = "UPDATE disciplina SET nm_disciplina = ?, cod_disciplina = ? WHERE id_disciplina = ?";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setString(1, t.getNomeDisciplina());
            stmt.setString(2, t.getCodDisciplina());
            stmt.setInt(3, t.getIdDisciplina());
            stmt.executeUpdate();
            conexao.commit();
            int id_disciplina = t.getIdDisciplina();
            excluirTopico(t.getIdDisciplina());
            for (Topico item : t.getTopicos()) {
                criarTopico(item, id_disciplina);
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public void excluir(Disciplina t) throws Exception {
        PreparedStatement stmt = null;
        String sql = "DELETE from disciplina where id_disciplina = ?";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, t.getIdDisciplina());
            stmt.executeUpdate();
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        } finally {
            try {
                stmt.close();
                conexao.close();
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    @Override
    public Disciplina consultar(String t) throws Exception {
        PreparedStatement stmt = null;
        String sql = "SELECT d.nm_disciplina, d.id_disciplina, d.cod_disciplina, t.nm_topico " + "FROM disciplina d, topico t  " + "WHERE d.nm_disciplina = ? " + "AND d.id_disciplina = t.id_disciplina";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setString(1, t);
            ResultSet rs = stmt.executeQuery();
            conexao.commit();
            Disciplina disciplina = new Disciplina();
            int count = 0;
            while (rs.next()) {
                count++;
                disciplina.setNomeDisciplina(rs.getString(1));
                disciplina.setIdDisciplina(rs.getInt(2));
                disciplina.setCodDisciplina(rs.getString(3));
                disciplina.setTopicos(listarTopicos(disciplina));
            }
            if (count > 0) {
                return disciplina;
            } else {
                return null;
            }
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    @Override
    public List<Disciplina> listar() throws Exception {
        List<Disciplina> lista_disciplinas = new ArrayList();
        PreparedStatement stmt = null;
        String sql = "SELECT * FROM disciplina";
        stmt = conexao.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        conexao.commit();
        while (rs.next()) {
            Disciplina disciplina = new Disciplina();
            disciplina.setIdDisciplina(rs.getInt(1));
            disciplina.setNomeDisciplina(rs.getString(2));
            disciplina.setCodDisciplina(rs.getString(3));
            lista_disciplinas.add(disciplina);
        }
        return lista_disciplinas;
    }

    public void excluirTopico(Integer idDisciplina) throws Exception {
        String sql = "DELETE from topico WHERE id_disciplina = ?";
        PreparedStatement stmt = null;
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setInt(1, idDisciplina);
            stmt.executeUpdate();
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }

    public void criarTopico(Topico topico, Integer idDisciplina) throws Exception {
        PreparedStatement stmt = null;
        String sql = "INSERT INTO topico (nm_topico, id_disciplina) values (?,?)";
        stmt = conexao.prepareStatement(sql);
        stmt.setString(1, topico.getNmTopico());
        stmt.setInt(2, idDisciplina);
        stmt.executeUpdate();
        conexao.commit();
    }

    @Override
    public List<Topico> listarTopicos(Disciplina disc) throws Exception {
        List<Topico> topicos = new ArrayList();
        PreparedStatement stmt = null;
        String sql = "SELECT * FROM topico where id_disciplina = ?";
        stmt = conexao.prepareStatement(sql);
        stmt.setInt(1, disc.getIdDisciplina());
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            Topico topico = new Topico();
            topico.setIdTopico(rs.getInt(1));
            topico.setNmTopico(rs.getString(2));
            Disciplina disciplina = new Disciplina();
            disciplina.setIdDisciplina(rs.getInt(3));
            topico.setDisciplina(disciplina);
            topicos.add(topico);
        }
        return topicos;
    }

    public void checkNomeDisciplina(Disciplina d) throws Exception {
        PreparedStatement stmt = null;
        String sql = "SELECT nm_disciplina FROM disciplina WHERE nm_disciplina = ?";
        stmt = conexao.prepareStatement(sql);
        stmt.setString(1, d.getNomeDisciplina());
        if (!stmt.execute()) throw new Exception(MSG_NOME_DISCIPLINA_EXISTE);
    }

    public void checkCodDisciplina(Disciplina d) throws Exception {
        PreparedStatement stmt = null;
        String sql = "SELECT cod_disciplina FROM disciplina WHERE cod_disciplina = ?";
        stmt = conexao.prepareStatement(sql);
        stmt.setString(1, d.getCodDisciplina());
        if (!stmt.execute()) throw new Exception(MSG_COD_DISCIPLINA_EXISTE);
    }

    @Override
    public Topico recuperarTopico(Disciplina disciplina, String nomeTopico) throws Exception {
        PreparedStatement stmt = null;
        String sql = "SELECT * FROM topico WHERE nm_topico = ? AND id_disciplina = ?";
        try {
            stmt = conexao.prepareStatement(sql);
            stmt.setString(1, nomeTopico);
            stmt.setInt(2, disciplina.getIdDisciplina());
            ResultSet rs = stmt.executeQuery();
            conexao.commit();
            Topico topico = new Topico();
            if (rs.next()) {
                topico.setDisciplina(disciplina);
                topico.setIdTopico(rs.getInt("id_topico"));
                topico.setNmTopico(rs.getString("nm_topico"));
            }
            return topico;
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        }
    }
}
