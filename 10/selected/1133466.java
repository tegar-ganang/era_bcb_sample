package appbaratoextreme.repositorio;

import appbaratoextreme.classesBasicas.Paragrafo;
import appbaratoextreme.util.Hibernate;
import appbaratoextreme.util.MethodsUtil;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 *
 * @author MarcosPaulo
 */
public class RepositorioParagrafo {

    private static RepositorioParagrafo repositorioParagrafo;

    private RepositorioParagrafo() {
        MethodsUtil.systemOutInstanciandoInfo(RepositorioParagrafo.class);
    }

    /**
     * 
     * @return 
     */
    public static RepositorioParagrafo getRepositorioParagrafo() {
        if (repositorioParagrafo == null) {
            return new RepositorioParagrafo();
        }
        return repositorioParagrafo;
    }

    /**
     * 
     * @param Paragrafo
     * @throws Exception 
     */
    public void cadastrarParagrafo(final Paragrafo Paragrafo) throws HibernateException, Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            session.save(Paragrafo);
            session.getTransaction().commit();
            return;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE: Erro no Cadastrar Paragrafo: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL: Erro no Cadastrar Paragrafo: ", e);
        }
    }

    /**
     * 
     * @param Paragrafo
     *          
     * @throws Exception 
     */
    public void deletarParagrafo(final Paragrafo... Paragrafos) throws HibernateException, Exception {
        for (Paragrafo Paragrafo : Paragrafos) {
            Session session = Hibernate.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();
                String sqlDeletaLocador = "DELETE FROM bdappcontrato.clasulaparagrafopk WHERE codParagrafo = :id ;";
                SQLQuery createSQLQuery = session.createSQLQuery(sqlDeletaLocador);
                createSQLQuery.setInteger("id", Paragrafo.getCodParagrafo());
                createSQLQuery.executeUpdate();
                session.delete(Paragrafo);
                session.getTransaction().commit();
            } catch (HibernateException e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                throw new HibernateException("HIBERNATE Erro no Deletar Paragrafo: ", e);
            } catch (Exception e) {
                throw new Exception("GERAL Erro no Deletar Paragrafo: ", e);
            }
        }
    }

    public void atualizarParagrafo(final Paragrafo... Paragrafos) throws HibernateException, Exception {
        for (Paragrafo Paragrafo : Paragrafos) {
            Session session = Hibernate.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();
                session.update(Paragrafo);
                session.getTransaction().commit();
            } catch (HibernateException e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                throw new HibernateException("HIBERNATE Erro no Atualizar Paragrafo: ", e);
            } catch (Exception e) {
                throw new Exception("GERAL Erro no Atualizar Paragrafo: ", e);
            }
        }
    }

    public List<Paragrafo> listarParagrafo(final String hql) throws HibernateException, Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            List<Paragrafo> listParagrafo = session.createQuery(hql).list();
            session.getTransaction().commit();
            return listParagrafo;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Listar Paragrafo: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Listar Paragrafo: ", e);
        }
    }

    public List<Paragrafo> listarParagrafo() throws HibernateException, Exception {
        return listarParagrafo("from Paragrafo");
    }

    public Paragrafo procurarParagrafo(final Paragrafo Paragrafo) throws HibernateException, Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            Paragrafo returnParagrafo = (Paragrafo) session.get(Paragrafo.class, Paragrafo);
            session.getTransaction().commit();
            return returnParagrafo;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Procurar Locador: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Procurar Locador: ", e);
        }
    }

    public Paragrafo procurarParagrafoId(final Integer id) throws HibernateException, Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            Paragrafo returnParagrafo = (Paragrafo) session.createQuery("from Paragrafo where codParagrafo = " + id);
            session.getTransaction().commit();
            return returnParagrafo;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Procurar Por ID Paragrafo: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Procurar Por ID Paragrafo: ", e);
        }
    }

    public static void main(String[] args) {
    }

    public List<Paragrafo> listarParagrafoPorDescricao(String trim) throws HibernateException, Exception {
        return listarParagrafo("from Paragrafo where descricao = '" + trim + "'");
    }
}
