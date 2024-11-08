package uniriotec.pm.trabalho2.dao.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import uniriotec.pm.trabalho2.dao.DisciplinaDAO;
import uniriotec.pm.trabalho2.dto.DisciplinaDTO;
import uniriotec.pm.trabalho2.dto.TopicoDTO;

/**
 *
 * @author albertoapr
 */
public class PostgresqlDisciplinaDAO extends PostgresqlDB implements DisciplinaDAO {

    /** The Constant LOG. */
    private static final Logger LOG = Logger.getLogger(PostgresqlDisciplinaDAO.class);

    @Override
    public void create(DisciplinaDTO disciplina) {
        try {
            this.criaConexao(false);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        String sql = "insert into Disciplina select nextval('sq_Disciplina') as id, ? as nome";
        PreparedStatement stmt = null;
        try {
            stmt = this.getConnection().prepareStatement(sql);
            stmt.setString(1, disciplina.getNome());
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                this.getConnection().rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de inserir dados de Disciplina no banco!");
            }
            this.getConnection().commit();
        } catch (SQLException e) {
            try {
                this.getConnection().rollback();
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                throw e;
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            try {
                stmt.close();
                this.fechaConexao();
            } catch (SQLException e) {
                try {
                    throw e;
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public void remove(int disciplinaId) {
        try {
            this.criaConexao(false);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        String sql = "delete from Disciplina where id = ?";
        PreparedStatement stmt = null;
        try {
            stmt = this.getConnection().prepareStatement(sql);
            stmt.setInt(1, disciplinaId);
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                this.getConnection().rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de remover dados de Revendedor no banco!");
            }
            this.getConnection().commit();
        } catch (SQLException e) {
            try {
                this.getConnection().rollback();
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                throw e;
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            try {
                stmt.close();
                this.fechaConexao();
            } catch (SQLException e) {
                try {
                    throw e;
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public void update(DisciplinaDTO disciplina) {
        try {
            this.criaConexao(false);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        LOG.debug("Criou a conex�o!");
        String sql = "update Disciplina set nome = ? where id = ?";
        PreparedStatement stmt = null;
        try {
            stmt = this.getConnection().prepareStatement(sql);
            LOG.debug("PreparedStatement criado com sucesso!");
            stmt.setString(1, disciplina.getNome());
            stmt.setInt(2, disciplina.getId());
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                this.getConnection().rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de alterar dados de Revendedor no banco!");
            }
            LOG.debug("Confirmando as altera��es no banco.");
            this.getConnection().commit();
        } catch (SQLException e) {
            LOG.debug("Desfazendo as altera��es no banco.");
            try {
                this.getConnection().rollback();
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
            LOG.debug("Lan�ando a exce��o da camada de persist�ncia.");
            try {
                throw e;
            } catch (SQLException ex) {
                java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
            }
        } finally {
            try {
                stmt.close();
                this.fechaConexao();
            } catch (SQLException e) {
                try {
                    throw e;
                } catch (SQLException ex) {
                    java.util.logging.Logger.getLogger(PostgresqlDisciplinaDAO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Override
    public List<DisciplinaDTO> listAllDisciplinas() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<TopicoDTO> listAllTopicos() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
