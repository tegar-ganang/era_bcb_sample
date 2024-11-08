package br.com.prossys.dao;

import br.com.prossys.modelo.Estabelecimento;
import br.com.prossys.modelo.PrecoProduto;
import br.com.prossys.modelo.PrecoProdutoPK;
import br.com.prossys.modelo.TipoProduto;
import br.com.prossys.util.Macros;
import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import org.apache.log4j.Logger;

/**
 * Classe de persistência de dados relacionados ao preco.
 * @author Victor Gutmann
 * @version 1.0
 */
public class PrecoProdutoDAO implements InterfacePrecoProdutoDAO {

    private EntityManagerFactory emf;

    Logger logger = Logger.getLogger(PrecoProdutoDAO.class.getName());

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    /**
     * Cria uma factory a partir das configurações feitas no xml de persistência
     */
    public PrecoProdutoDAO() {
        emf = Persistence.createEntityManagerFactory(Macros.getPersistenceUnitName());
    }

    /**
     * Salva preço na tabela.
     * @param produto preço do produto a ser salvo
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void salvar(PrecoProduto produto) throws PersistenciaException {
        if (produto == null) {
            String erro = "Erro ao tentar salvar preco. O preco informado é nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(produto);
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar salvar preco";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }

    /**
     * Consulta preços com determinado nome de produto, ou parte dele.
     * @param nome Nome ou parte do nome de produto a ser consultado
     * @return Coleção de precos com determinado nome do produto.
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Collection<PrecoProduto> consultaPorNome(String nome) throws PersistenciaException {
        if (nome == null) {
            String erro = "Erro ao tentar consultar por nome. Nome informado é nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        Collection<PrecoProduto> produtos = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from PrecoProduto p where p.produto.nome like ?1").setParameter(1, "%" + nome + "%");
            produtos = query.getResultList();
        } catch (Exception e) {
            String erro = "Erro ao tentar consultar produto por nome";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
        return produtos;
    }

    /**
     * Consulta preços de produtos por um determinado tipo.
     * @param tipo Tipo de produto a ser consultado.
     * @return Coleção de preços com produtos de determinado tipo.
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Collection<PrecoProduto> consultaPorTipo(TipoProduto tipo) throws PersistenciaException {
        if (tipo == null) {
            String erro = "Erro ao tentar consultar por tipo. Tipo informado é nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        Collection<PrecoProduto> produtos = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from PrecoProduto p where p.produto.tipo like ?1").setParameter(1, tipo);
            produtos = query.getResultList();
        } catch (Exception e) {
            String erro = "Erro ao tentar consultar produto por tipo";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
        return produtos;
    }

    /**
     * Altera preco de um produto
     * @param preco preco a ser alterado
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void alterar(PrecoProduto preco) throws PersistenciaException {
        if (preco == null) {
            String erro = "Erro ao tentar alterar preco: nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(preco);
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar alterar preco";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }

    /**
     * Lista preços de produtos de um determinado estabelecimento
     * @param estabelecimento
     * @return Coleção de objetos PrecoProduto
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public Collection<PrecoProduto> consultaPorEstabelecimento(Estabelecimento estabelecimento) throws PersistenciaException {
        if (estabelecimento == null) {
            String erro = "Erro ao tentar consultar por estabelecimento: nulo";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        Collection<PrecoProduto> produtos = null;
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("select p from PrecoProduto p where p.estabelecimento = ?1").setParameter(1, estabelecimento);
            produtos = query.getResultList();
        } catch (Exception e) {
            String erro = "Erro ao tentar consultar preços de produtos por estabelecimento";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
        return produtos;
    }

    /**
     * Exclui um preço relacionado a um estabelecimento e a um produto
     * @param precoProduto preço a ser excluído
     * @throws br.com.prossys.dao.PersistenciaException
     */
    public void excluir(PrecoProduto precoProduto) throws PersistenciaException {
        if (precoProduto == null) {
            String erro = "Erro ao tentar excluir preco: argumento nulo.";
            logger.error(erro);
            throw new IllegalArgumentException(erro);
        }
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            Query query = em.createQuery("delete from PrecoProduto p where p.id = :id");
            query.setParameter("id", new PrecoProdutoPK(precoProduto.getEstabelecimento().getCdPessoa(), precoProduto.getProduto().getCdProduto()));
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            String erro = "Erro ao tentar excluir preco.";
            logger.error(erro);
            em.getTransaction().rollback();
            throw new PersistenciaException(e);
        } finally {
            em.close();
        }
    }
}
