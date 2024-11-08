package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.ToGloss;
import edu.univalle.lingweb.persistence.ToGlossDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'to_gloss'( Glosas Explicativas )
 * 
 * @author Jose Luis Aricapa M.
 */
public class DataManagerGloss extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerGloss() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nueva t�cnica en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, ToGloss gloss) {
        ToGlossDAO maGlossDAO = new ToGlossDAO();
        try {
            gloss.setGlossId(getSequence("sq_to_gloss"));
            EntityManagerHelper.beginTransaction();
            maGlossDAO.save(gloss);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(gloss);
            log.info("Se cre� Glosa Explicativa con �xito: " + gloss.getGlossId());
            Object[] args = { gloss.getGlossId() };
            result.setMessage(MessageFormat.format(bundle.getString("gloss.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la Glosa Explicativa: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Reliza la busqueda de una glosa a partir de su c�digo 
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nGlossId C�digo de la glosa
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nGlossId) {
        ToGloss maGloss = new ToGlossDAO().findById(nGlossId);
        if (maGloss == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("gloss.search.notFound"));
        } else {
            List<ToGloss> list = new ArrayList<ToGloss>();
            EntityManagerHelper.refresh(maGloss);
            list.add(maGloss);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("gloss.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una Glosa Explicativa
	 * @param serviceResult serviceResult El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param maGloss Glosa Explicativa a Eliminar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, ToGloss maGloss) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.DELETE_TO_GLOSS);
            query.setParameter(1, maGloss.getGlossId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maGloss);
            Object[] arrayParam = { maGloss.getGlossId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("gloss.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el texto individual: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maGloss.getGlossId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("gloss.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una respuesta Abierta
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coCourse
	 *            curso a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, ToGloss gloss) {
        ToGlossDAO glossDAO = new ToGlossDAO();
        try {
            log.info("Actualizando la Glosa Explicativa: " + gloss.getGlossId());
            EntityManagerHelper.beginTransaction();
            glossDAO.update(gloss);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(gloss);
            Object[] arrayParam = { gloss.getGlossId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("singleText.update.success"), arrayParam));
            log.info("Se actualizo la Glosa Explicativa con �xito: " + gloss.getGlossId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la Glosa Explicativa: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de Glosas Explicativas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de Glosas Explicativas
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result, int nRowStart, int nMaxResults) {
        ToGlossDAO glossDAO = new ToGlossDAO();
        List<ToGloss> list = glossDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("gloss.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("gloss.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(glossDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }
}
