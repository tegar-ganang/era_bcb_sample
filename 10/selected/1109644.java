package appbaratoextreme.repositorio;

import appbaratoextreme.classesBasicas.Imovel;
import appbaratoextreme.util.Hibernate;
import appbaratoextreme.util.MethodsUtil;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

/**
 *
 * @author MarcosPaulo
 */
public class RepositorioImovel {

    private static RepositorioImovel repositorioImovel;

    private RepositorioImovel() {
        MethodsUtil.systemOutInstanciandoInfo(RepositorioImovel.class);
    }

    public static RepositorioImovel getRepositorioImovel() {
        if (repositorioImovel != null) {
            return repositorioImovel;
        } else {
            return new RepositorioImovel();
        }
    }

    public void cadastrarImovel(final Imovel imovel) throws Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            session.save(imovel);
            session.getTransaction().commit();
            return;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Cadastrar Imovel: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Cadastrar Imovel: ", e);
        }
    }

    public void deletarImovel(final Imovel... imoveis) throws Exception {
        for (Imovel imovel : imoveis) {
            Session session = Hibernate.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();
                String sqlDeletaLocador = "DELETE FROM locadorimovelpk WHERE codImovel = :id ;";
                SQLQuery createSQLQuery = session.createSQLQuery(sqlDeletaLocador);
                createSQLQuery.setInteger("id", imovel.getCodImovel());
                System.out.println(createSQLQuery.executeUpdate());
                session.delete(imovel);
                session.getTransaction().commit();
            } catch (HibernateException e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                throw new HibernateException("HIBERNATE Erro no Deletar Imovel: ", e);
            } catch (Exception e) {
                throw new Exception("GERAL Erro no Deletar Imovel: ", e);
            }
        }
    }

    public void atualizarImovel(final Imovel... imoveis) throws Exception {
        for (Imovel imovel : imoveis) {
            Session session = Hibernate.getSessionFactory().getCurrentSession();
            try {
                session.beginTransaction();
                session.update(imovel);
                session.getTransaction().commit();
            } catch (HibernateException e) {
                if (session != null) {
                    session.getTransaction().rollback();
                }
                throw new HibernateException("HIBERNATE Erro no Atualizar Imovel: ", e);
            } catch (Exception e) {
                throw new Exception("GERAL Erro no Atualizar Imovel: ", e);
            }
        }
    }

    public List<Imovel> listarImovel(final String hql) throws HibernateException, Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            List<Imovel> listImovel = session.createQuery(hql).list();
            session.getTransaction().commit();
            return listImovel;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Listar Imovel: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Listar Imovel: ", e);
        }
    }

    public List<Imovel> listarImovel() throws HibernateException, Exception {
        return listarImovel("from Imovel");
    }

    public Imovel procurarImovel(final Imovel imovel) throws Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            Imovel returnImovel = (Imovel) session.get(Imovel.class, imovel);
            session.getTransaction().commit();
            return returnImovel;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Procurar Imovel: ", e);
        } catch (Exception e) {
            throw new Exception("GERAL Erro no Procurar Imovel:", e);
        }
    }

    public Imovel procurarImovelId(final Integer id) throws Exception {
        Session session = Hibernate.getSessionFactory().getCurrentSession();
        try {
            session.beginTransaction();
            Imovel returnImovel = (Imovel) session.createQuery("from Imovel where codImovel =" + id);
            session.getTransaction().commit();
            return returnImovel;
        } catch (HibernateException e) {
            if (session != null) {
                session.getTransaction().rollback();
            }
            throw new HibernateException("HIBERNATE Erro no Procurar Por ID Imovel: ", e);
        } catch (Exception e) {
            throw new Exception("", e);
        }
    }

    public static void main(String[] args) {
        try {
            List<Imovel> listarImovel = RepositorioImovel.getRepositorioImovel().listarImovel();
            for (Imovel imovel : listarImovel) {
                System.out.println(imovel);
            }
        } catch (HibernateException ex) {
            Logger.getLogger(RepositorioImovel.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(RepositorioImovel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<Imovel> listarImovelPorEndereco(String trim) throws HibernateException, Exception {
        return listarImovel("from Imovel where endereco = '" + trim + "'");
    }
}
