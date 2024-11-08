package br.cefetrn.smartcefet.persistencia.sgbd;

import br.cefetrn.smartcefet.dominio.Ambiente;
import br.cefetrn.smartcefet.dominio.FuncionarioSistema;
import br.cefetrn.smartcefet.dominio.Permissao;
import br.cefetrn.smartcefet.dominio.Ponto;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

/**
 * @author Cr�stian Deives <cristiandeives@gmail.com>
 */
public class Persistencia {

    public static final String PERSISTENCE_UNIT = "smartcefet-pu";

    private static Persistencia instance;

    private static final String JPQL_FUNCIONARIO_POR_MATRICULA = "SELECT f FROM FuncionarioSistema f WHERE f.matricula = ?1";

    private static final String JPQL_FUNCIONARIO_POR_NOME = "SELECT f FROM FuncionarioSistema f " + "WHERE UPPER(f.nome) LIKE ?1";

    private static final String JPQL_TODOS_AMBIENTES = "SELECT a FROM Ambiente a ORDER BY a.descricao";

    private EntityManagerFactory emf;

    private Persistencia() throws PersistenceException {
        emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }

    public static Persistencia getInstance() throws PersistenceException {
        if (instance == null) {
            instance = new Persistencia();
        }
        return instance;
    }

    public static void fechar() throws PersistenceException {
        if (instance != null && !instance.emf.isOpen()) {
            instance.emf.close();
        }
    }

    public void inserir(Object entidade) throws PersistenceException {
        EntityManager em = null;
        try {
            System.out.println("aqui 53");
            em = emf.createEntityManager();
            System.out.println("aqui 55");
            em.getTransaction().begin();
            System.out.println("aqui 57");
            em.persist(entidade);
            System.out.println("aqui 59");
            em.getTransaction().commit();
            System.out.println("aqui 61");
        } catch (PersistenceException e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void inserirPonto(Ponto ponto) {
        EntityManager em = null;
        try {
            System.out.println("aqui 53");
            em = emf.createEntityManager();
            System.out.println("aqui 55");
            em.getTransaction().begin();
            System.out.println("aqui 57");
            em.persist(ponto);
            System.out.println("aqui 59");
            em.getTransaction().commit();
            System.out.println("aqui 61");
        } catch (PersistenceException e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Ambiente> findTodosAmbientes() throws PersistenceException {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            return em.createQuery(JPQL_TODOS_AMBIENTES).getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public FuncionarioSistema findFuncionarioPorMatricula(String matricula) throws PersistenceException {
        if (matricula == null) {
            throw new NullPointerException("matricula n�o pode ser null");
        }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            return (FuncionarioSistema) em.createQuery(JPQL_FUNCIONARIO_POR_MATRICULA).setParameter(1, matricula).getSingleResult();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<FuncionarioSistema> findFuncionariosPorNome(String nome) throws PersistenceException {
        if (nome == null) {
            throw new NullPointerException("nome n�o pode ser null");
        }
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            return em.createQuery(JPQL_FUNCIONARIO_POR_NOME).setParameter(1, '%' + nome.toUpperCase() + '%').getResultList();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public <T> T find(Class<T> classe, Object id) throws PersistenceException {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            return em.find(classe, id);
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public FuncionarioSistema verificarSenhaFuncionario(String senha) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Query query = em.createQuery("select f from FuncionarioSistema f where f.senha = :senha");
            query.setParameter("senha", senha);
            FuncionarioSistema funcionario = (FuncionarioSistema) query.getSingleResult();
            System.out.println("Persistencia - nome funcinario: " + funcionario.getNome());
            return funcionario;
        } catch (Exception e) {
            return null;
        } finally {
            if (em != null) em.close();
        }
    }

    public Permissao recuperarPermisao(FuncionarioSistema funcionario, Ambiente ambiente) {
        EntityManager em = null;
        em = emf.createEntityManager();
        Query query = em.createQuery("select p from Permissao p where (p.funcionario = :funcionario AND p.ambiente = :ambiente )");
        query.setParameter("funcionario", funcionario);
        query.setParameter("ambiente", ambiente);
        Permissao permissao;
        try {
            permissao = (Permissao) query.getSingleResult();
        } catch (NoResultException e) {
            permissao = null;
            System.out.println("nehum resultado");
        } finally {
            if (em != null) em.close();
        }
        return permissao;
    }

    public void atualizarSenhaFuncionario(FuncionarioSistema funcionario) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Query query = em.createNativeQuery("UPDATE Funcionario SET senha ='" + funcionario.getSenha() + "' WHERE(matricula ='" + funcionario.getMatricula() + "')");
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw e;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public boolean verificarPermissaoAmbiente(Ambiente ambienteSelecionado, FuncionarioSistema funcionario) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            Query query = em.createNativeQuery("Select *From permissao " + "where (id_ambiente='" + ambienteSelecionado.getId() + "' " + "AND matricula_funcionario='" + funcionario.getMatricula() + "' )");
            System.out.println("Matricula func: " + funcionario.getMatricula());
            System.out.println("Id do ambiente: " + ambienteSelecionado.getId());
            System.out.println("Consulta: " + query);
            if (query.getResultList() == null) {
                em.close();
                return false;
            } else {
                em.close();
                return true;
            }
        } catch (Exception e) {
            System.out.println("Cath -191");
            e.printStackTrace();
            em.close();
            return false;
        }
    }

    public FuncionarioSistema findFuncionarioPorNome(String nome) throws PersistenceException {
        EntityManager em = null;
        em = emf.createEntityManager();
        Query query = em.createQuery("select f from FuncionarioSistema f where (f.nome = :nome )");
        query.setParameter("nome", nome);
        FuncionarioSistema funcionario;
        try {
            funcionario = (FuncionarioSistema) query.getSingleResult();
            em.close();
        } catch (NoResultException e) {
            funcionario = null;
            System.out.println("nehum resultado");
            em.close();
        }
        return funcionario;
    }

    public List<Permissao> recuperarPermicoesPorFuncionario(FuncionarioSistema funcionario) {
        EntityManager em = null;
        em = emf.createEntityManager();
        Query query = em.createQuery("select p from Permissao p where (p.funcionario = :funcionario )");
        query.setParameter("funcionario", funcionario);
        List<Permissao> permissoes = null;
        try {
            permissoes = query.getResultList();
            em.close();
        } catch (NoResultException e) {
            funcionario = null;
            System.out.println("nehum resultado");
            em.close();
        }
        return permissoes;
    }

    public void excluirPermissao(Permissao permissao) {
        EntityManager em = null;
        em = emf.createEntityManager();
        em.getTransaction().begin();
        Query query = em.createNativeQuery("delete from Permissao where(idpermissao='" + permissao.getIdPermissao() + "')");
        query.executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    public void atualizarFuncionarioAtivo(FuncionarioSistema funcionario) {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            em.getTransaction().begin();
            Query query = em.createNativeQuery("UPDATE Funcionario SET ativo ='" + funcionario.isAtivo() + "' WHERE(matricula ='" + funcionario.getMatricula() + "')");
            query.executeUpdate();
            em.getTransaction().commit();
        } catch (PersistenceException e) {
            em.getTransaction().rollback();
            e.printStackTrace();
            throw e;
        } finally {
            System.out.println("bloco finally");
            if (em != null) {
                em.close();
            }
        }
    }
}
