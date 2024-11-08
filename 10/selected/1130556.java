package org.larOzanam.arquitetura.dao;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.larOzanam.arquitetura.core.Sessao;
import org.larOzanam.arquitetura.excecoes.ExcecaoNegocio;
import org.larOzanam.arquitetura.excecoes.ExcecaoSistema;
import org.larOzanam.arquitetura.excecoes.GerenciadorExcecao;
import org.larOzanam.arquitetura.logging.GerenciadorLog;

/**
 *
 * @author Administrador
 */
public class DaoLocal<Model> implements Dao<Model> {

    private static Sessao sessao;

    private Class<Model> model;

    private static GerenciadorLog logger = GerenciadorLog.getInstance(DaoLocal.class);

    public ExcecaoNegocio ENTIDADE_NAO_ENCONTRADA;

    public static final ExcecaoNegocio CHAVE_CONSULTA_NULA = new ExcecaoNegocio("Valor informado para consulta � nulo ou inv�lido");

    /** Creates a new instance of Dao */
    public DaoLocal(Sessao sessao, Class<Model> model) {
        this.sessao = sessao;
        this.model = model;
        ENTIDADE_NAO_ENCONTRADA = new ExcecaoNegocio("Nenhum registro de " + sessao.getDicionario().traduzirEntidade(model) + " foi encontrado.");
    }

    /**
     * Retorna a representa��o da classe utilizada neste DAO.
     */
    public Class<Model> getModel() {
        return model;
    }

    /**
     * Este m�todo insere um objeto diretamente no banco de dados.
     */
    public Boolean insert(Model m) throws ExcecaoSistema {
        logger.debug("insert >> " + m);
        EntityManager entity = this.getSessao().getEntityManager();
        try {
            entity.getTransaction().begin();
            logger.debug("transa��o iniciada...");
            entity.persist(m);
            logger.debug("inser��o no banco efetuada...");
            entity.getTransaction().commit();
            logger.debug("commit executado com sucesso...");
            logger.debug("Objeto inserido no banco: " + m);
        } catch (EntityExistsException ex) {
            logger.error("Erro ao tentar inserir entidade j� existente no sistema", ex);
            GerenciadorExcecao.tratarExcecao(ex, this.getModel());
        } catch (IllegalArgumentException ex) {
            logger.error("Erro ao tentar inserir no banco de dados", ex);
            GerenciadorExcecao.tratarExcecao(ex, this.getModel());
        } catch (Exception ex) {
            logger.error("Erro ao tentar inserir no banco de dados", ex);
            GerenciadorExcecao.tratarExcecao(ex, m);
        } finally {
            if (entity.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                entity.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        logger.debug("insert << true");
        return true;
    }

    /**
     * Este m�todo altera um objeto ja cadastrado no banco de dados.
     */
    public Model update(Model m) throws ExcecaoSistema {
        logger.debug("update >> " + m);
        EntityManager entity = this.getSessao().getEntityManager();
        try {
            entity.getTransaction().begin();
            logger.debug("transa��o inciada...");
            entity.merge(m);
            logger.debug("alteracao realizada...");
            entity.getTransaction().commit();
            logger.debug("comit realizado com sucesso...");
            logger.debug("update << Entidade alterada: " + m);
        } catch (Exception e) {
            logger.error("Erro ao tentar alterar informa��es no banco de dados", e);
            GerenciadorExcecao.tratarExcecao(e, m);
        } finally {
            if (entity.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                entity.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        return m;
    }

    /**
     * Este m�todo remove um objeto do banco de dados.
     */
    public Boolean delete(Model m) throws ExcecaoSistema {
        logger.debug("delete >> " + m);
        EntityManager entity = this.getSessao().getEntityManager();
        try {
            entity.getTransaction().begin();
            logger.debug("transa��o iniciada...");
            entity.remove(m);
            logger.debug("objeto removido do banco...");
            entity.getTransaction().commit();
            logger.debug("commit realizado com sucesso...");
        } catch (Exception e) {
            logger.error("Erro ao tentar remover informa��es no banco", e);
            GerenciadorExcecao.tratarExcecao(e, m);
        } finally {
            if (entity.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                entity.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        logger.debug("delete << Entidade removida do banco de dados: " + m);
        return true;
    }

    /**
     * Este m�todo seleciona um objeto do banco de dados. Para executar a sele��o � necess�rio
     * passar como argumento a chave prim�ria do objeto sendo pesquisado.
     * <br><br><b> Exce��es</b>
     * ENTIDADE_NAO_ENCONTRADA - Lan�ado sempre que uma pesquisa n�o retornar o valor da entidade sendo pesquisada.
     */
    public Model select(Object chavePrimaria) throws ExcecaoSistema, ExcecaoNegocio {
        logger.debug("select >> [Nome da Consulta : consultaPorChavePrimaria]");
        logger.debug("Parametros para consulta - [Chave Primaria : " + chavePrimaria + "]");
        if (chavePrimaria == null) {
            throw CHAVE_CONSULTA_NULA;
        }
        EntityManager entity = this.getSessao().getEntityManager();
        Model m = null;
        try {
            entity.getTransaction().begin();
            logger.debug("transa��o iniciada...");
            m = (Model) entity.find(this.getModel(), chavePrimaria);
            if (m != null) {
                entity.refresh(m);
                logger.debug("objeto atualizado com o banco de dados...");
            }
            entity.getTransaction().commit();
            logger.debug("pesquisa finalizada com sucesso...");
            logger.debug("Objeto localizado: " + m);
        } catch (Exception e) {
            logger.error("Erro ao tentar selecionar informa��es pela chave prim�ria", e);
            GerenciadorExcecao.tratarExcecao(e, this.getModel());
        } finally {
            if (entity.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                entity.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        if (m == null) {
            this.lancarExcecaoEntidadeNaoEncontrada();
        }
        logger.debug("select <<");
        return m;
    }

    /**
     * Este m�todo seleciona uma lista de objetos do banco de dados. Para executar a sele��o � necess�rio
     * passar como argumento o nome da consulta e um objeto Parametro com todos os par�metros necess�rios.
     * <br><br><b> Exce��es</b>
     * ENTIDADE_NAO_ENCONTRADA - Lan�ado sempre que uma pesquisa n�o retornar o valor da entidade sendo pesquisada.
     */
    public List<Model> select(String nomeConsulta, Parametro parametros) throws ExcecaoSistema, ExcecaoNegocio {
        NullPointerException nullEx = null;
        if (nomeConsulta == null) {
            nullEx = new NullPointerException("O argumento nomeConsulta possui valor nulo.");
            logger.fatal("Erro ao inicializar os parametros do selec", nullEx);
            throw nullEx;
        }
        logger.debug("select >> [Nome da Consulta : " + nomeConsulta + "]");
        EntityManager manager = this.getSessao().getEntityManager();
        Query query = manager.createNamedQuery(nomeConsulta);
        query.setHint("toplink.refresh", "true");
        List<Model> lista = null;
        if (parametros != null) {
            for (Map.Entry<String, Object> e : parametros.entrySet()) {
                logger.debug("Parametros para consulta - [nomeCampo = " + e.getKey() + "][valorProcurado = " + e.getValue() + "]");
                query.setParameter(e.getKey(), e.getValue());
            }
        } else {
            logger.debug("Os parametros para esta consulta s�o nulos");
        }
        try {
            lista = query.getResultList();
            logger.debug("pesquisa realizada com sucesso...");
        } catch (Exception ex) {
            logger.error("Erro ao tentar executar uma " + nomeConsulta, ex);
            GerenciadorExcecao.tratarExcecao(ex, nomeConsulta);
        } finally {
            if (manager.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                manager.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        if (lista != null && lista.size() > 0) {
            for (Model m : lista) {
                logger.debug("Objeto localizado: " + m);
            }
        } else {
            this.lancarExcecaoEntidadeNaoEncontrada();
        }
        logger.debug("select <<");
        return lista;
    }

    /**
     * Este m�todo atualiza um objeto com seu respectivo valor diretamente no banco.
     */
    public Model refresh(Model model) throws ExcecaoSistema {
        logger.debug("refresh >> Nome Entidade: " + model);
        EntityManager manager = this.getSessao().getEntityManager();
        try {
            manager.getTransaction().begin();
            logger.debug("transa��o ativada...");
            manager.refresh(model);
            logger.debug("objeto atualizado com o banco de dados...");
            manager.getTransaction().commit();
            logger.debug("atualiza��o realizada com sucesso...");
        } catch (Exception ex) {
            GerenciadorExcecao.tratarExcecao(ex, this.getModel());
        } finally {
            if (manager.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                manager.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        logger.debug("refresh << Entidade recarregada: " + model);
        return model;
    }

    /**
     * Este m�todo realiza uma consulta no banco de acordo com NamedNativeQueries definidas na classe.
     * Estas consultas s�o feitas com sql comun e por isso possui um m�todo de execu��o diferenciado.
     */
    public List<List> selectBySQL(String nomeConsulta, Parametro parametros) throws ExcecaoSistema, ExcecaoNegocio {
        NullPointerException nullEx = null;
        if (nomeConsulta == null) {
            nullEx = new NullPointerException("O argumento nomeConsulta possui valor nulo.");
            logger.fatal("Erro ao inicializar os parametros do selec", nullEx);
            throw nullEx;
        }
        logger.debug("selectBySQL >> [Consulta por querySQL : " + nomeConsulta + "]");
        EntityManager manager = this.getSessao().getEntityManager();
        Query query = manager.createNamedQuery(nomeConsulta);
        List<List> lista = null;
        if (parametros != null) {
            for (Map.Entry<String, Object> e : parametros.entrySet()) {
                logger.debug("Parametros para consulta - [nomeCampo = " + e.getKey() + "][valorProcurado = " + e.getValue() + "]");
                query.setParameter(e.getKey(), e.getValue());
            }
        } else {
            logger.debug("Os parametros para esta consulta s�o nulos");
        }
        try {
            logger.debug("transa��o iniciada...");
            lista = query.getResultList();
            logger.debug("pesquisa finalizada...");
        } catch (Exception ex) {
            logger.error("Erro ao tentar executar uma consulta por SQL " + nomeConsulta, ex);
            GerenciadorExcecao.tratarExcecao(ex, nomeConsulta);
        } finally {
            if (manager.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                manager.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        if (lista != null && lista.size() > 0) {
            for (Object m : lista) {
                logger.debug("Objeto localizado: " + m);
            }
        } else {
            this.lancarExcecaoEntidadeNaoEncontrada();
        }
        logger.debug("select <<");
        return lista;
    }

    /**
     * Este m�todo realiza uma consulta no banco de acordo com NamedNativeQueries definidas na classe.
     * Estas consultas s�o feitas com sql comun e por isso possui um m�todo de execu��o diferenciado.
     */
    public List<List> selectBySQL(String nomeConsulta) throws ExcecaoSistema, ExcecaoNegocio {
        return this.selectBySQL(nomeConsulta, null);
    }

    public boolean executeNamedCommand(String nomeComando, Parametro parametros) throws ExcecaoSistema {
        NullPointerException nullEx = null;
        if (nomeComando == null) {
            nullEx = new NullPointerException("O argumento nomeComando possui valor nulo.");
            logger.fatal("Erro ao inicializar os parametros do executeNamedCommand", nullEx);
            throw nullEx;
        }
        logger.debug("executeNamedCommand >> [Executar named command : " + nomeComando + "]");
        EntityManager manager = this.getSessao().getEntityManager();
        Query query = manager.createNamedQuery(nomeComando);
        if (parametros != null) {
            for (Map.Entry<String, Object> e : parametros.entrySet()) {
                logger.debug("Parametros para execu��o - [nomeCampo = " + e.getKey() + "][valorProcurado = " + e.getValue() + "]");
                query.setParameter(e.getKey(), e.getValue());
            }
        } else {
            logger.debug("Os parametros para esta execu��o s�o nulos");
        }
        boolean executed = false;
        try {
            logger.debug("iniciando transa��o...");
            manager.getTransaction().begin();
            logger.debug("executando named command...");
            executed = query.executeUpdate() == 0 ? false : true;
            logger.debug("finalizando execu��o...");
            manager.getTransaction().commit();
            if (executed) logger.debug("comando executado com sucesso!"); else logger.debug("ERRO! comando n�o executado...");
        } catch (Exception ex) {
            logger.error("Erro ao tentar executar um named command " + nomeComando, ex);
            GerenciadorExcecao.tratarExcecao(ex, nomeComando);
        } finally {
            if (manager.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                manager.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        logger.debug("executeNamedCommand <<");
        return executed;
    }

    /**
     * Retorna a exce��o executada no momento.
     */
    private Sessao getSessao() {
        return sessao;
    }

    public boolean callStoredProcedure(String nomeProcedure, Parametro parametros) throws ExcecaoSistema {
        NullPointerException nullEx = null;
        if (nomeProcedure == null) {
            nullEx = new NullPointerException("O argumento nomeProcedure possui valor nulo.");
            logger.fatal("Erro ao inicializar os parametros do callStoredProcedure", nullEx);
            throw nullEx;
        }
        logger.debug("callStoredProcedure >> [Executar stored procedure: " + nomeProcedure + "]");
        String procedureCall = "{Call " + nomeProcedure.toUpperCase() + "(";
        int i = 0;
        if (parametros != null && (i = parametros.keySet().size()) > 0) {
            procedureCall += "?";
            for (int n = 1; n < i; n++) {
                procedureCall += ", ?";
            }
        }
        procedureCall += ")}";
        logger.debug("callStoredProcedure >> [Executar stored procedure: " + procedureCall + "]");
        EntityManager manager = this.getSessao().getEntityManager();
        Query query = manager.createNativeQuery(procedureCall);
        int nrParametro = 1;
        if (parametros != null) {
            for (Map.Entry<String, Object> e : parametros.entrySet()) {
                logger.debug("Parametros para execu��o - [ordemCampo = " + nrParametro + "]" + "[nomeCampo = " + e.getKey() + "]" + "[valorEnviado = " + e.getValue() + "]");
                query.setParameter(nrParametro++, e.getValue());
            }
        } else {
            logger.debug("Os parametros para esta execu��o s�o nulos");
        }
        boolean executed = false;
        try {
            logger.debug("iniciando transa��o...");
            manager.getTransaction().begin();
            logger.debug("executando stored procedure...");
            executed = query.executeUpdate() == 0 ? false : true;
            logger.debug("finalizando execu��o...");
            manager.getTransaction().commit();
            if (executed) logger.debug("stored procedure executada com sucesso!"); else logger.debug("ERRO! stored procedure n�o executada...");
        } catch (Exception ex) {
            logger.error("Erro ao tentar executar uma stored procedure " + nomeProcedure, ex);
            GerenciadorExcecao.tratarExcecao(ex, nomeProcedure);
        } finally {
            if (manager.getTransaction().isActive()) {
                logger.debug("transa��o ainda est� ativa...");
                logger.debug("fechando transa��o...");
                manager.getTransaction().rollback();
                logger.debug("transa��o fechada com sucesso...");
            }
        }
        logger.debug("callStoredProcedure <<");
        return executed;
    }

    private void lancarExcecaoEntidadeNaoEncontrada() throws ExcecaoNegocio {
        logger.fatal(ENTIDADE_NAO_ENCONTRADA.getMessage(), ENTIDADE_NAO_ENCONTRADA);
        throw ENTIDADE_NAO_ENCONTRADA;
    }
}
