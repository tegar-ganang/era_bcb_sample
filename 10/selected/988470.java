package appbaratoextreme.repositorio;

import appbaratoextreme.classesBasicas.Locador;
import appbaratoextreme.util.Hibernate;
import appbaratoextreme.util.MethodsUtil;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.classic.Session;

/**
 *
 * @author MarcosPaulo
 */
public class RepositorioLocador {

    private static RepositorioLocador repositorioLocador;

    /**
     * 
     * @return 
     */
    public static RepositorioLocador getRepositorioLocador() {
        if (repositorioLocador != null) {
            return repositorioLocador;
        }
        return new RepositorioLocador();
    }

    private RepositorioLocador() {
    }

    /**
     * 
     * @param locador
     * @throws Exception 
     */
    public void cadasrarLocador(final Locador locador) throws Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            session.save(locador);
            System.out.println("Imovel size: " + locador.getImovels().size());
            session.getTransaction().commit();
            return;
        } catch (HibernateException e) {
            e.printStackTrace();
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Cadastrar Locador: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Cadastrar Locador: ", e);
        }
    }

    /**
     * 
     * @param locadors
     *          
     * @throws Exception 
     */
    public void deletarLocador(final Locador... locadors) throws Exception {
        for (Locador locador : locadors) {
            Session session = Hibernate.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();
                String sqlDeletaLocador = "DELETE FROM locadorimovelpk WHERE codLocador = :id ;";
                SQLQuery createSQLQuery = session.createSQLQuery(sqlDeletaLocador);
                createSQLQuery.setInteger("id", locador.getCodLocador());
                System.out.println(createSQLQuery.executeUpdate());
                session.delete(locador);
                session.getTransaction().commit();
            } catch (HibernateException e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                throw new HibernateException("HIBERNATE Erro no Deletar Locador: ", e);
            } catch (Exception e) {
                throw new Exception("GERAL Erro no Deletar Locador: ", e);
            }
        }
    }

    public void atualizarLocador(final Locador... locador) throws Exception {
        for (Locador l : locador) {
            Session session = Hibernate.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();
                session.update(l);
                session.getTransaction().commit();
            } catch (HibernateException e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                e.printStackTrace();
                throw new HibernateException("HIBERNATE Erro no Atualizar Locador: ", e);
            } catch (Exception e) {
                throw new Exception("GERAL Erro no Atualizar Locador: ", e);
            }
        }
    }

    public List<Locador> listarLocador(final String hql) throws Exception, HibernateException {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            List<Locador> listLocador = session.createQuery(hql).list();
            session.getTransaction().commit();
            return listLocador;
        } catch (HibernateException e) {
            e.printStackTrace();
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Listar Locador: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Listar Locador: ", e);
        }
    }

    public List<Locador> listarLocador() throws Exception, HibernateException {
        return listarLocador("from Locador");
    }

    public Locador procurarLocador(final Locador locador) throws Exception, HibernateException {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            Locador returnlocador = (Locador) session.get(Locador.class, locador);
            session.getTransaction().commit();
            return returnlocador;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
            throw new HibernateException("HIBERNATE Erro no Procurar Locador: ", e);
        } catch (Exception e) {
            throw e;
        }
    }

    public Locador procurarLocadorId(final Integer id) throws Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            Locador returnLocador = (Locador) session.createQuery("from Locador where codLocador =" + id);
            session.getTransaction().commit();
            return returnLocador;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            e.printStackTrace();
            throw new HibernateException("HIBERNATE Erro no Procurar Por ID Locador: ", e);
        } catch (Exception e) {
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            RepositorioLocador repositorioLocadorMain = RepositorioLocador.getRepositorioLocador();
            List<Locador> listarLocador = repositorioLocadorMain.listarLocador();
            for (Locador locador : listarLocador) {
                System.out.println(locador.getNome());
                System.out.println(locador.getCpf());
                System.out.println(locador.getRg());
            }
        } catch (HibernateException e) {
            for (int i = 0; i < e.getMessages().length; i++) {
                System.err.println(e.getMessage(i));
            }
            e.printStackTrace();
            System.err.println(e.getMessage() + e.getCause().getMessage());
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    public List<Locador> listarLocadorPorNome(String string) throws Exception {
        return listarLocador("from Locador where nome = '" + string + "'");
    }
}
