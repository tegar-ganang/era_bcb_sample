package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoActivity;
import edu.univalle.lingweb.persistence.CoActivityDAO;
import edu.univalle.lingweb.persistence.CoMaterial;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToGlossary;
import edu.univalle.lingweb.persistence.ToGlossaryDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'to_glossary'( Cursos )
 * 
 * @author Jose Aricapa
 */
public class DataManagerGlossary extends DataManager {

    public static final int MATERIAL_IMAGE = 1;

    public static final int MATERIAL_SOUND = 2;

    public static final int MATERIAL_PHONETIC = 3;

    /**
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerGlossary.class);

    public DataManagerGlossary() {
        super();
        DOMConfigurator.configure(DataManagerGlossary.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo termino de glosario en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, ToGlossary toGlossary) {
        ToGlossaryDAO toGlossaryDAO = new ToGlossaryDAO();
        try {
            toGlossary.setGlossaryId(getSequence("sq_to_glossary"));
            EntityManagerHelper.beginTransaction();
            toGlossaryDAO.save(toGlossary);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toGlossary);
            Object[] args = { toGlossary.getTerm(), toGlossary.getGlossaryId() };
            result.setMessage(MessageFormat.format(bundle.getString("glossary.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar el termino del glosario : " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Actualiza los datos de un termino de glosario
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param toGlossary
	 *            termino del glosario a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToGlossary toGlossary) {
        ToGlossaryDAO toGlossaryDAO = new ToGlossaryDAO();
        try {
            log.info("Actualizando el termino del glosario : " + toGlossary.getTerm());
            EntityManagerHelper.beginTransaction();
            toGlossaryDAO.update(toGlossary);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(toGlossary);
            Object[] arrayParam = { toGlossary.getTerm() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.update.success"), arrayParam));
            }
            log.info("Se actualizo el termino del glosario  con �xito: " + toGlossary.getTerm());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el termino del glosario : " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un termino del glosario
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nGlossaryId
	 *            c�digo del programa del termino del glosario
	 * @param nUserId
	 *            c�digo o id del usuario que realiza la petici�n
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nGlossaryId) {
        ToGlossary toGlossary = new ToGlossaryDAO().findById(nGlossaryId);
        EntityManagerHelper.refresh(toGlossary);
        if (toGlossary == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("glossary.search.notFound"));
        } else {
            List<ToGlossary> list = new ArrayList<ToGlossary>();
            EntityManagerHelper.refresh(toGlossary);
            list.add(toGlossary);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un termino del glosario a trav�s de su c�digo
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param sTerm
	 *            c�digo del programa del termino del glosario
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult searchGlossaryTerm(RestServiceResult serviceResult, String sTerm) {
        List<ToGlossary> list = new ToGlossaryDAO().findByTerm(sTerm);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("glossary.search.notFound"));
        } else {
            Object[] args = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.search.success"), args));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de un termino del glosario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param toGlossary
	 *            Curso a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToGlossary toGlossary) {
        String sTerm = null;
        try {
            sTerm = toGlossary.getTerm();
            log.error("Eliminando el termino del glosario : " + toGlossary.getTerm());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_TO_GLOSSARY);
            query.setParameter(1, toGlossary.getGlossaryId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sTerm };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.delete.success"), arrayParam));
            log.info("Eliminando el termino del glosario : " + toGlossary.getTerm());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el termino del glosario : " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { toGlossary.getTerm() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de termino del glosario s
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de termino del glosario s
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result, int nRowStart, int nMaxResults) {
        ToGlossaryDAO toGlossaryDAO = new ToGlossaryDAO();
        List<ToGlossary> list = toGlossaryDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("glossary.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("glossary.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(toGlossaryDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de termino del glosario de una actividad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nActivityId
	 *            C�digo de la actividad
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listGlossaryForActivity(RestServiceResult serviceResult, long nActivityId) {
        List<ToGlossary> listGlosary = new ArrayList<ToGlossary>();
        CoActivity coActivity = new CoActivityDAO().findById(nActivityId);
        EntityManagerHelper.refresh(coActivity);
        if (coActivity == null) {
            serviceResult.setObjResult(listGlosary);
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("glossary.list.notFound"));
        } else {
            for (Iterator<ToGlossary> iterator = coActivity.getToGlossaries().iterator(); iterator.hasNext(); ) {
                ToGlossary toGlossary = iterator.next();
                EntityManagerHelper.refresh(toGlossary);
                listGlosary.add(toGlossary);
            }
            if (listGlosary.size() == 0) {
                serviceResult.setObjResult(listGlosary);
                serviceResult.setNumResult(0);
                serviceResult.setMessage(bundle.getString("glossary.list.notFound"));
            } else {
                Object[] array = { listGlosary.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.list.success"), array));
                serviceResult.setObjResult(listGlosary);
                serviceResult.setNumResult(listGlosary.size());
            }
        }
        return serviceResult;
    }

    /**
	 * Obtiene el audio de un glosario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nGlossaryId
	 *            C�digo de la actividad
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public synchronized RestServiceResult listMaterial(RestServiceResult serviceResult, long nGlossaryId, int nMaterialType) {
        List<CoMaterial> listMaterial = new ArrayList<CoMaterial>();
        ToGlossary toGlossary = new ToGlossaryDAO().findById(nGlossaryId);
        EntityManagerHelper.refresh(toGlossary);
        if (toGlossary == null) {
            serviceResult.setObjResult(listMaterial);
            serviceResult.setNumResult(0);
            serviceResult.setMessage(bundle.getString("glossary.list.notFound"));
        } else {
            CoMaterial coMaterial = null;
            switch(nMaterialType) {
                case MATERIAL_IMAGE:
                    coMaterial = toGlossary.getCoMaterialByImageMaterialId();
                    log.info("IMAGE...");
                    break;
                case MATERIAL_SOUND:
                    coMaterial = toGlossary.getCoMaterialBySoundMaterialId();
                    log.info("SOUND...");
                    break;
                case MATERIAL_PHONETIC:
                    coMaterial = toGlossary.getCoMaterialByPhoneticTransMaterialId();
                    log.info("PHONETIC...");
                    break;
                default:
                    break;
            }
            if (coMaterial == null) {
                serviceResult.setObjResult(listMaterial);
                serviceResult.setNumResult(0);
                serviceResult.setMessage(bundle.getString("glossary.list.notFound"));
            } else {
                listMaterial.add(coMaterial);
                Object[] array = { 1 };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("glossary.list.success"), array));
                serviceResult.setObjResult(listMaterial);
                serviceResult.setNumResult(1);
            }
        }
        return serviceResult;
    }
}
