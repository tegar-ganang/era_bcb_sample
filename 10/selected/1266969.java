package br.com.prossys.dao;

import java.util.Collection;
import br.com.prossys.modelo.Empresa;
import br.com.prossys.modelo.Estabelecimento;
import br.com.prossys.modelo.Produto;
import br.com.prossys.util.Macros;
import javax.persistence.Query;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.NoResultException;
import org.apache.log4j.Logger;

/**
 * Classe de persistência de dados relacionados à empresa já cadastrada no sistema
 * @author Victor Gutmann
 * @version 1.0
 */
public class EmpresaDAO implements InterfaceEmpresaDAO {

    Logger logger = Logger.getLogger(EmpresaDAO.class.getName());

    private EntityManagerFactory emf;

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Cria uma factory a partir das configurações feitas no xml de persistência
     */
    public EmpresaDAO() {
        emf = Persistence.createEntityManagerFactory(Macros.getPersistenceUnitName());
    }

    /**
     * Verifica se há uma empresa no sistema com determinado username e senha e retorna o objeto relacionado a ela.
     * @param username Username da empresa
     * @param senha Senha da empresa
     * @return Objeto relacionado à empresa ou null se não existir.
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Empresa consultarPorLoginESenha(String username, String senha) throws PersistenciaException {
        if (username == null) {
            String erro = "Erro ao tentar buscar a empresa. O login informado é nulo.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        if (senha == null) {
            String erro = "Erro ao tentar buscar a empresa. A senha informada é nula.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        Empresa empresa = new Empresa();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select e from Empresa e where e.username = :username and e.senha = :senha");
            query.setParameter("username", username);
            query.setParameter("senha", senha);
            empresa = (Empresa) query.getSingleResult();
            em.getTransaction().commit();
        } catch (NoResultException e) {
            String warning = "Nenhuma empresa com usuario e senha passados foi encontrado.";
            logger.error(warning);
            return null;
        } catch (Exception e) {
            String erro = "Erro ao tentar buscar empresa.";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
        return empresa;
    }

    /**
     * Salva empresa no banco de dados.
     * @param empresa Empresa a ser salva no sistema
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void salvar(Empresa empresa) throws PersistenciaException {
        if (empresa == null) {
            String erro = "Erro ao tentar salvar empresa. A empresa informada é nulo.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        PessoaDAO dao = new PessoaDAO();
        if (dao.usernameEmUso(empresa.getUsername())) {
            String erro = "Erro ao tentar salvar empresa. Username ja existe.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(empresa);
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar salvar empresa";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }

    /**
     * Consulta uma empresa por seu id.
     * @param id Id da empresa
     * @return Objeto da empresa com o id passado pelo argumento.
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Empresa consultaPorId(Integer id) throws PersistenciaException {
        if (id == null) {
            String erro = "Erro ao tentar consultar empresa por id. Id informada é nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        Empresa empresa = null;
        try {
            em.getTransaction().begin();
            empresa = em.find(Empresa.class, id);
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar consultar empresa por id.";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
        return empresa;
    }

    /**
     * Lista todos os estabelecimentos de uma determinada empresa.
     * @param empresa Empresa a ser consultada
     * @return Coleção de objetos de estabelecimentos da empresa consultada.
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Collection<Estabelecimento> listarEstabelecimentosPorEmpresa(Empresa empresa) throws PersistenciaException {
        EntityManager em = getEntityManager();
        Collection<Estabelecimento> estabelecimentos = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select e from Estabelecimento e where e.empresa = :empresa").setParameter("empresa", empresa);
            estabelecimentos = query.getResultList();
        } catch (Exception e) {
            String erro = "Erro ao tentar listar todas as empresas";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
        return estabelecimentos;
    }

    /**
     * Exclui um produto de determinada empresa.
     * @param produto Produto a ser removido
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void excluirProduto(Produto produto) throws PersistenciaException {
        if (produto == null) {
            String erro = "Erro ao tentar excluir produto. Produto esta nulo.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("delete from Produto p where p.cdProduto = :id").setParameter("id", produto.getCdProduto());
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar excluir produto.";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }

    /**
     * Exclui um estabelecimento de determinada empresa
     * @param estabelecimento Estabelecimento a ser excluído
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void excluirEstabelecimento(Estabelecimento estabelecimento) throws PersistenciaException {
        if (estabelecimento == null) {
            String erro = "Erro ao tentar excluir estabelecimento: argumento nulo.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("delete from Pessoa e where e.cdPessoa = :id").setParameter("id", estabelecimento.getCdPessoa());
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar excluir estabelecimento.";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }

    /**
     * Lista todos os produtos que estão cadastrados por uma determinada empresa.
     * @param empresa Empresa a ser consultada.
     * @return Coleção de objetos de produtos que a empresa consultada possui no sistema.
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Collection<Produto> listarProdutosPorEmpresa(Empresa empresa) throws PersistenciaException {
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from Produto p where p.empresa = :empresa");
            query.setParameter("empresa", empresa);
            return query.getResultList();
        } catch (Exception e) {
            String erro = "Erro ao tentar listar produtos da empresa";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }

    /**
     * Altera os dados de uma empresa
     * @param empresa empresa em questão
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void alterar(Empresa empresa) throws PersistenciaException {
        if (empresa == null) {
            String erro = "Erro ao tentar alterar empresa: argumento nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(empresa);
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar alterar dados da empresa";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }
}
