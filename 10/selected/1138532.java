package uniriotec.pm.empresa.dao.postgresql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import uniriotec.pm.empresa.dao.EmpregadoDao;
import uniriotec.pm.empresa.model.Empregado;

/**
 *
 * @author albertoapr
 */
public class PostgresqlEmpregadoDao extends PostgresqlDB implements EmpregadoDao {

    public PostgresqlEmpregadoDao(String url, String user, String password, String driver) {
        super(url, user, password, driver);
    }

    /** The Constant LOG. */
    private static final Logger LOG = Logger.getLogger(PostgresqlEmpregadoDao.class);

    @Override
    public void create(Empregado empregado) throws SQLException, ClassNotFoundException, Exception {
        String sql = "";
        sql = "insert into empregado " + "? as cpf, " + "? as nome," + "? as sexo," + "? as data_nascimento," + "? as data_admissao," + "? as salario";
        this.criaConexao(false);
        PreparedStatement stmt = null;
        stmt = this.getConnection().prepareStatement(sql);
        stmt.setString(1, "");
        stmt.setString(2, empregado.getNome());
        stmt.setString(3, empregado.getSexo());
        stmt.setDate(4, empregado.getDataNascimento());
        stmt.setDate(5, empregado.getDataAdmissao());
        stmt.setDate(6, empregado.getDataDesligamento());
        stmt.setDouble(7, empregado.getSalario());
        stmt.execute();
        this.getConnection().commit();
        this.getConnection().close();
    }

    @Override
    public void remove(String cpf) throws SQLException, ClassNotFoundException {
        this.criaConexao(true);
        PreparedStatement stmt = null;
        stmt = this.getConnection().prepareStatement("delete from empregado where cpf =" + cpf);
        stmt.execute();
        this.getConnection().commit();
        this.getConnection().close();
    }

    @Override
    public void update(Empregado empregado) throws SQLException, ClassNotFoundException {
        this.criaConexao(false);
        LOG.debug("Criou a conex�o!");
        String sql = "update empregado set " + "cpf =  ?," + "sexo = ?," + "data_nascimento = ?," + "data_admissao =?," + "data_desligamento =?," + "salario =? ," + "where id = ?";
        PreparedStatement stmt = null;
        try {
            stmt = this.getConnection().prepareStatement(sql);
            LOG.debug("PreparedStatement criado com sucesso!");
            stmt.setString(1, empregado.getCpf());
            stmt.setString(2, empregado.getNome());
            stmt.setString(3, empregado.getSexo());
            stmt.setDate(4, empregado.getDataNascimento());
            stmt.setDate(5, empregado.getDataAdmissao());
            stmt.setDate(6, empregado.getDataDesligamento());
            stmt.setDouble(7, empregado.getSalario());
            stmt.setInt(8, empregado.getId());
            int retorno = stmt.executeUpdate();
            if (retorno == 0) {
                this.getConnection().rollback();
                throw new SQLException("Ocorreu um erro inesperado no momento de alterar dados de Revendedor no banco!");
            }
            LOG.debug("Confirmando as altera��es no banco.");
            this.getConnection().commit();
        } catch (SQLException e) {
            LOG.debug("Desfazendo as altera��es no banco.");
        } finally {
            try {
                stmt.close();
                this.fechaConexao();
            } catch (SQLException e) {
            }
        }
    }

    @Override
    public ArrayList<Empregado> listAll() throws SQLException, ClassNotFoundException, Exception {
        ArrayList<Empregado> empregados = null;
        String sql = "";
        sql = "select cpf,nome,sexo,data_nascimento,data_admissao," + "data_desligamento,salario from empregado";
        this.criaConexao(false);
        PreparedStatement stmt = null;
        stmt = this.getConnection().prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();
        empregados = new ArrayList<Empregado>();
        while (rs.next()) {
            Empregado e = new Empregado(rs.getString("cpf"), rs.getString("nome"), rs.getString("sexo"), rs.getDate("data_nascimento"), rs.getDate("data_admissao"), rs.getDouble("salario"), rs.getDate("data_desligamento"));
            empregados.add(e);
        }
        this.getConnection().close();
        return empregados;
    }

    @Override
    public Empregado searchById(int empregadoId) throws ClassNotFoundException, SQLException {
        Empregado empregado = null;
        this.criaConexao(false);
        String sql = "select id,cpf, nome from Empregado where id = ?";
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = this.getConnection().prepareStatement(sql);
            stmt.setInt(1, empregadoId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                empregado = new Empregado();
                empregado.setId(rs.getInt("id"));
                empregado.setCpf(rs.getString("cpf"));
                empregado.setNome(rs.getString("nome"));
            }
        } finally {
            try {
                stmt.close();
                rs.close();
                this.fechaConexao();
                return empregado;
            } catch (SQLException e) {
                throw e;
            }
        }
    }

    @Override
    public Empregado searchByCpf(String cpf) throws SQLException, ClassNotFoundException, Exception {
        Empregado empregado = null;
        String sql = "select cpf, nome,sexo,data_nascimento,data_admissao,salario,data_desligamento from empregado where cpf = ?";
        this.criaConexao(true);
        PreparedStatement stmt = null;
        stmt = this.getConnection().prepareStatement(sql);
        stmt.setString(1, cpf);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            empregado = new Empregado(rs.getString("cpf"), rs.getString("nome"), rs.getString("sexo"), rs.getDate("data_nascimento"), rs.getDate("data_admissao"), rs.getDouble("salario"), rs.getDate("data_desligamento"));
        }
        this.getConnection().close();
        return empregado;
    }
}
