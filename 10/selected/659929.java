package dao;

import java.io.Serializable;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import util.HelpDeskUtil;
import util.MsgErros;
import excecoes.HelpDeskException;

/**
 * 
 * <b>HelpDeskTRE</b><br>
 * <br>
 * 
 * 
 * Classe que implementa a interface DAO e prover aos tipos existentes no
 * projeto uma integracao com banco de dados atraves do framework hibernate..
 * 
 * @author Arthur Lucio Meneses Farias <br>
 * @author Andre Luiz Alves <br>
 * @author Danilo Coura Moreira <br>
 */
public abstract class AbstractDAO {

    protected SessionFactory sessionFactoryLocal;

    /**
	 * O construtor da Classe.
	 */
    protected AbstractDAO() {
        sessionFactoryLocal = getSessionFactory();
    }

    /**
	 * O construtor da Classe.
	 * 
	 * @param sessionFactory
	 *            um factory para o sessionFactory
	 */
    private AbstractDAO(SessionFactory sessionFactory) {
        this.sessionFactoryLocal = sessionFactory;
    }

    /**
	 * Metodo que salva no Banco de Dados um objeto em uma determinda tabela.
	 * 
	 * @param obj
	 *            O objeto a ser salvo no Banco de Dados.
	 * @return O id do objeto criado no Banco de Dados.
	 */
    protected synchronized Serializable insert(Object obj) throws HelpDeskException {
        Session session = openSession();
        Transaction tx = null;
        Serializable identifier = null;
        try {
            tx = session.beginTransaction();
            identifier = session.save(obj);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new HelpDeskException(MsgErros.OPER_NAO_REALIZADA.msg("Inser��o", e.getMessage()));
        } finally {
            session.flush();
            session.close();
        }
        return identifier;
    }

    /**
	 * Metodo que modifica no Banco de Dados um objeto de uma determinda tabela.
	 * 
	 * @param obj
	 *            O objeto a ser modificado no Banco de Dados
	 * @throws HelpDeskException
	 *             caso ocorra algum erro
	 */
    protected synchronized void update(Object obj) throws HelpDeskException {
        Session session = openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(obj);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new HelpDeskException(MsgErros.OPER_NAO_REALIZADA.msg("Atualiza��o", e.getMessage()));
        } finally {
            session.flush();
            session.close();
        }
    }

    /**
	 * Metodo que remove no Banco de Dados um objeto de uma determinda tabela.
	 * 
	 * @param obj
	 *            O objeto a ser removido no Banco de Dados.
	 * @throws HelpDeskException
	 *             caso ocorra algum erro
	 */
    protected synchronized void delete(Object obj) throws HelpDeskException {
        Session session = openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(obj);
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new HelpDeskException(MsgErros.OPER_NAO_REALIZADA.msg("Remo��o", e.getMessage()));
        } finally {
            session.flush();
            session.close();
        }
    }

    /**
	 * Metodo que le um objeto do Banco de Dados a partir de um id.
	 * 
	 * @param id
	 *            O id do objeto.
	 * @param classe
	 *            A classe que o objeto pertence.
	 * @return O objeto lido do Banco de Dados.
	 */
    protected synchronized Object read(Class classe, Serializable id) {
        if (id == null) return null;
        Session session = openSession();
        Object obj = session.get(classe, id);
        session.flush();
        session.close();
        return obj;
    }

    /**
	 * Retorna uma lista de objetos do Banco de Dados com as caracteristicas
	 * definidas por queryString.
	 * 
	 * @param queryString
	 *            A string da busca.
	 * @return Uma lista de objetos com as caracteristicas definidas por
	 *         queryString.
	 */
    protected synchronized List getList(String queryString) {
        Session session = openSession();
        Query query = session.createQuery(queryString);
        List list = query.list();
        session.flush();
        session.close();
        return list;
    }

    protected synchronized List getAll(String nameClass) {
        Session session = openSession();
        String queryCompleta = " from " + nameClass;
        Query query = session.createQuery(queryCompleta);
        List list = query.list();
        session.flush();
        session.close();
        return list;
    }

    /**
	 * Retorna uma lista de objetos do Banco de Dados com as caracteristicas
	 * definidas por queryString.
	 * 
	 * @param conditionQueryString
	 *            A string da busca.
	 * @return Uma lista de objetos com as caracteristicas definidas por
	 *         queryString.
	 */
    protected synchronized List getList(String conditionQueryString, String nameClass) {
        Session session = openSession();
        String queryCompleta = "from " + nameClass + " objeto where " + conditionQueryString;
        Query query = session.createQuery(queryCompleta);
        List list = query.list();
        session.flush();
        session.close();
        return list;
    }

    /**
	 * Remove todos os objetos de um determinado tipo contidos no Banco de
	 * Dados.
	 * 
	 * @throws HelpDeskException
	 *             caso ocorra algum erro
	 */
    protected synchronized void removeAll(String deleteFrom) throws HelpDeskException {
        Session session = openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Query queryDelete = session.createQuery("delete from " + deleteFrom);
            queryDelete.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new HelpDeskException(MsgErros.OPER_NAO_REALIZADA.msg("Limpeza de cadastro", e.getMessage()));
        } finally {
            session.flush();
            session.close();
        }
    }

    /**
	 * Estabelece uma conexao e abre uma sessao com o Banco de Dados
	 * retornando-a.
	 * 
	 * @return A sessao.
	 */
    protected Session openSession() {
        return getSessionFactory().openSession();
    }

    protected SessionFactory getSessionFactory() {
        return HelpDeskUtil.getSessionFactoryLocal();
    }

    protected synchronized void uptadeQuery(String uptade, String conditionQueryString, String nameClass) throws HelpDeskException {
        String queryCompleta = "update " + nameClass + " set " + uptade + " where " + conditionQueryString;
        Session session = openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Query query = session.createQuery(queryCompleta);
            int valor = query.executeUpdate();
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw new HelpDeskException(MsgErros.OPER_NAO_REALIZADA.msg("Atualiza��o", e.getMessage()));
        } finally {
            session.flush();
            session.close();
        }
    }
}
