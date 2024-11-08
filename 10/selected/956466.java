package br.com.pleno.core;

import br.com.pleno.core.domain.Usuario;
import br.com.pleno.core.ui.LoginPanel;
import br.com.pleno.core.util.EMUtil;
import br.com.pleno.core.util.Utilitario;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import org.openide.util.NbPreferences;

/**
 * Principal gerenciador de componentes da aplicação e fachada para as principais
 * funcionalidades do sistema.
 * @author Lourival Almeida
 */
public class Pleno {

    private static Pleno instance;

    private Map sessao;

    private Pleno() {
        sessao = new HashMap();
    }

    public Map getSessao() {
        return sessao;
    }

    public Object getAtributoSessao(Atributos atr) {
        return sessao.get(atr.getValor());
    }

    public Usuario getUsuarioLogado() {
        Usuario usuario = (Usuario) getAtributoSessao(Atributos.USUARIO);
        if (usuario == null) {
            String usuarioStr = NbPreferences.forModule(Pleno.class).get(Atributos.LOGIN_USUARIO.getValor(), "");
            String senha = NbPreferences.forModule(Pleno.class).get(Atributos.LOGIN_SENHA.getValor(), "");
            if (Pleno.getInstance().buscarUsuario(usuarioStr, senha) == null) {
                Utilitario.mostrarPainel(new LoginPanel(null));
            }
            return getUsuarioLogado();
        }
        return usuario;
    }

    public void setUsuarioLogado(Usuario usuario) {
        sessao.put(Atributos.USUARIO.getValor(), usuario);
    }

    /**
     * Retorna uma instância ativa da Fachada Gestão de Projeto
     * @return instância da fachada
     */
    public static Pleno getInstance() {
        if (instance == null) instance = new Pleno();
        return instance;
    }

    public List query(String queryString) {
        EntityManager em = EMUtil.getEntityManager();
        Query query = em.createQuery(queryString);
        List results = query.getResultList();
        em.close();
        return results;
    }

    public List queryNativa(String queryString) {
        EntityManager em = EMUtil.getEntityManager();
        Query query = em.createNativeQuery(queryString);
        List results = query.getResultList();
        em.close();
        return results;
    }

    /**
     * Retorna uma entidade qualquer do banco de dados passada a classe e o ID
     */
    public Object buscarEntidade(Class classe, Integer id) {
        EntityManager em = EMUtil.getEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("from " + classe.getSimpleName() + " where id = :id");
        query.setParameter("id", id);
        Object result = query.getSingleResult();
        em.getTransaction().commit();
        em.close();
        return result;
    }

    /**
     * Responsável por inserir um objeto na base de dados correspondente.
     */
    public void inserir(Object entidade) {
        EntityManager em = EMUtil.getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            em.persist(entidade);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
        } finally {
            em.close();
        }
    }

    /**
     * Remove uma entidade qualquer da base de dados.
     */
    public void remover(Integer id, Class classe) {
        EntityManager em = EMUtil.getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            Query q = em.createQuery("delete " + classe.getSimpleName() + " where id = :id");
            q.setParameter("id", id);
            q.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    /**
     * Busca uma entidade pelo id na base de dados.
     */
    public Object consulta(Class classe, Integer id) {
        EntityManager em = EMUtil.getEntityManager();
        EntityTransaction tx = null;
        Object entidade = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            entidade = em.find(classe, id);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
        return entidade;
    }

    /**
     * Atualiza informações de uma entidade.
     */
    public void atualizar(Object entidade) {
        EntityManager em = EMUtil.getEntityManager();
        EntityTransaction tx = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            em.merge(entidade);
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
        }
    }

    /**
     * Retorna um usuário a partir do login e senha informados.
     * @param login
     * @param senha
     * @return
     */
    public Usuario buscarUsuario(String login, String senha) {
        String senhaAux = Utilitario.stringHexa(Utilitario.gerarHash(senha));
        EntityManager em = EMUtil.getEntityManager();
        em.getTransaction().begin();
        Query query = em.createQuery("from Usuario where login = :login and senha = :senha");
        query.setParameter("login", login);
        query.setParameter("senha", senhaAux);
        Usuario usuario = null;
        try {
            usuario = (Usuario) query.getSingleResult();
            usuario.setUltimoAcesso(new Date());
            em.merge(usuario);
            setUsuarioLogado(usuario);
        } catch (javax.persistence.NoResultException e) {
        }
        em.getTransaction().commit();
        em.close();
        return usuario;
    }
}
