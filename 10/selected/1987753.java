package com.odontosis.dao;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.odontosis.as.OdontosisDataAccessObject;
import com.odontosis.entidade.Paciente;
import com.odontosis.entidade.ParametrosFIOD;
import com.odontosis.util.HibernateUtil;

public class ParametrosDAO extends OdontosisDataAccessObject<ParametrosFIOD> {

    public String buscarCpf() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select cpf from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarCnpj() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select cnpj from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarCidade() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select cidade from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarBairro() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select bairro from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarEnd() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select endereco from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarDentista() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select dentista from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarCRO() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select cro from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarClinica() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select nomeclinica from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public String buscarEnderecoImpressora() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select endereco_impressora parametros from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public Integer buscarTipoImpressao() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select tipo_impressao parametros from parametros where id = 1");
        return (Integer) query.uniqueResult();
    }

    public String buscarEndereecoBatImpressao() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select endereco_impressao parametros from parametros where id = 1");
        return (String) query.uniqueResult();
    }

    public Integer buscarParcelasGerar() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createSQLQuery("select numeroParcelas parametros from parametros where id = 1");
        return (Integer) query.uniqueResult();
    }

    public void inserirParametros(Integer parcelas, Integer tipoImpressao, String enderecoImpressao) {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        try {
            Query query = session.createSQLQuery("update parametros set numeroParcelas = :parcelas, tipo_impressao = :tipoImpressao, endereco_impressora = :enderecoImpressao where id = 1");
            query.setParameter("parcelas", parcelas);
            query.setParameter("tipoImpressao", tipoImpressao);
            query.setParameter("enderecoImpressao", enderecoImpressao);
            query.executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            e.printStackTrace();
        }
    }

    public ParametrosFIOD buscarParametrosFiod() {
        HibernateUtil.closeSession();
        Session session = HibernateUtil.currentSession();
        Query query = session.createQuery("from ParametrosFIOD where id = 1");
        return (ParametrosFIOD) query.uniqueResult();
    }
}
