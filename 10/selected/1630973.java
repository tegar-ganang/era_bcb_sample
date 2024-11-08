package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.CoLanguage;
import edu.univalle.lingweb.persistence.CoLanguageDAO;
import edu.univalle.lingweb.persistence.CoParagraph;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.CoParagraphBaseKnowledge;
import edu.univalle.lingweb.persistence.CoParagraphBaseKnowledgeDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_paragraph_base_knowledge'( paragraph base knowledge)
 * 
 * @author Diana Carolina Rivera
 */
public class DataManagerParagraphBaseKnowledge extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * 
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerMaPostag.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerParagraphBaseKnowledge() {
        super();
        DOMConfigurator.configure(DataManagerParagraphBaseKnowledge.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo postag en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maPostag
	 *            Postag a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoParagraphBaseKnowledge coParagraphBaseKnowledge) {
        CoParagraphBaseKnowledgeDAO coParagraphBaseKnowledgeDAO = new CoParagraphBaseKnowledgeDAO();
        try {
            coParagraphBaseKnowledge.setKnowledgeId(getSequence("sq_co_paragraph_base_knowledge"));
            EntityManagerHelper.beginTransaction();
            coParagraphBaseKnowledgeDAO.save(coParagraphBaseKnowledge);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coParagraphBaseKnowledge);
            log.info("Knowledge creado con �xito: " + coParagraphBaseKnowledge.getKnowledgeId());
            Object[] arrayParam = { coParagraphBaseKnowledge.getKnowledgeId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar el knowledge: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza un Knowledge en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maPostag
	 *            Postag a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoParagraphBaseKnowledge coParagraphBaseKnowledge) {
        CoParagraphBaseKnowledgeDAO coParagraphBaseKnowledgeDAO = new CoParagraphBaseKnowledgeDAO();
        try {
            EntityManagerHelper.beginTransaction();
            coParagraphBaseKnowledgeDAO.update(coParagraphBaseKnowledge);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coParagraphBaseKnowledge);
            Object[] args = { coParagraphBaseKnowledge.getKnowledgeId() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.update.success"), args));
            }
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el knowledge: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param maPostag
	 *            Postag a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoParagraphBaseKnowledge coParagraphBaseKnowledge) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_PARAGRAPH_BASE_KNOWLEDGE);
            query.setParameter(1, coParagraphBaseKnowledge.getKnowledgeId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { coParagraphBaseKnowledge.getKnowledgeId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar el Knowledge: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coParagraphBaseKnowledge.getKnowledgeId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un Postag por Id
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nPostagId
	 *            C�digo del Postag
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nKnowledgeId) {
        CoParagraphBaseKnowledge coParagraphBaseKnowledge = new CoParagraphBaseKnowledgeDAO().findById(nKnowledgeId);
        if (coParagraphBaseKnowledge == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("paragraphBaseKnowledge.search.notFound"));
        } else {
            List<CoParagraphBaseKnowledge> list = new ArrayList<CoParagraphBaseKnowledge>();
            EntityManagerHelper.refresh(coParagraphBaseKnowledge);
            list.add(coParagraphBaseKnowledge);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la lista de un Postag por Idioma
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param nPostagId
	 *            C�digo del Postag
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listByParagraphId(RestServiceResult serviceResult, Long nLangugeId, Long nParagraphId, int nRow) {
        List<CoParagraphBaseKnowledge> listCoParagraphBaseKnowledge = null;
        log.info("nLangugeId: " + nLangugeId);
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_CO_PARAGRAPH_BASE_KNOWLEDGE, CoParagraphBaseKnowledge.class);
        query.setParameter(1, nLangugeId);
        query.setParameter(2, nParagraphId);
        query.setParameter(3, nRow);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoParagraphBaseKnowledge = query.getResultList();
        if (listCoParagraphBaseKnowledge == null || listCoParagraphBaseKnowledge.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("paragraphBaseKnowledge.search.notFound"));
        } else {
            Object[] arrayParam = { listCoParagraphBaseKnowledge.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.search.success"), arrayParam));
            serviceResult.setObjResult(listCoParagraphBaseKnowledge);
            serviceResult.setNumResult(listCoParagraphBaseKnowledge.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de Postags
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de Postags
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoParagraphBaseKnowledgeDAO coParagraphBaseKnowledgeDAO = new CoParagraphBaseKnowledgeDAO();
        List<CoParagraphBaseKnowledge> list = coParagraphBaseKnowledgeDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("paragraphBaseKnowledge.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("paragraphBaseKnowledge.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coParagraphBaseKnowledgeDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public static void main(String[] args) {
    }
}
