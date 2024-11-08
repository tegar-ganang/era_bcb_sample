package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.persistence.CoSingleText3;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaSingleTextForm;
import edu.univalle.lingweb.persistence.MaSingleTextFormDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'singleText'( Respuesta abierta )
 * 
 * @author Juan Pablo Rivera Velasco
 */
public class DataManagerSingleTextForm extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerSingleTextForm() {
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
    public RestServiceResult create(RestServiceResult result, MaSingleTextForm singleTextForm) {
        MaSingleTextFormDAO maSingleTextFormDAO = new MaSingleTextFormDAO();
        log.info("estoy en el create del data manager");
        try {
            singleTextForm.setSingleTextFormId(getSequence("sq_ma_single_text_form_1"));
            EntityManagerHelper.beginTransaction();
            maSingleTextFormDAO.save(singleTextForm);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(singleTextForm);
            log.info("Pregunta de respuesta abierta con �xito: " + singleTextForm.getSingleTextFormId());
            Object[] args = { singleTextForm.getSingleTextFormId() };
            result.setMessage(MessageFormat.format(bundle.getString("singleTextForm.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar la respuesta abierta: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    public RestServiceResult search(RestServiceResult serviceResult, Long nSingleTextFormId) {
        MaSingleTextForm maSingleTextForm = new MaSingleTextFormDAO().findById(nSingleTextFormId);
        if (maSingleTextForm == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("singleTextForm.search.notFound"));
        } else {
            List<MaSingleTextForm> list = new ArrayList<MaSingleTextForm>();
            EntityManagerHelper.refresh(maSingleTextForm);
            list.add(maSingleTextForm);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("singleTextForm.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    public RestServiceResult delete(RestServiceResult serviceResult, MaSingleTextForm maSingleTextForm) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.DELETE_MA_SINGLE_TEXT_FORM);
            query.setParameter(1, maSingleTextForm.getSingleTextFormId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(maSingleTextForm);
            Object[] arrayParam = { maSingleTextForm.getSingleTextFormId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("singleTextForm.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el texto individual: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { maSingleTextForm.getSingleTextFormId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("singleTextForm.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Se crea el metodo que permite listar las plantillas por idioma
	 * @author Ing. Juan Pablo Rivera Velasco
	 * @Enterprize ApuntadeCambio.com Hardware and Software Solutions
	 * @web www.apuntadecambio.com/hs
	 */
    public RestServiceResult listByLanguagueId(RestServiceResult serviceResult, Long nLanguagueId) {
        List<MaSingleTextForm> listMaSingleText = null;
        Query query = EntityManagerHelper.getEntityManager().createNativeQuery(Statements.SELECT_MA_SINGLE_TEXT_FORM_LANG, MaSingleTextForm.class);
        query.setParameter(1, nLanguagueId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listMaSingleText = query.getResultList();
        if (listMaSingleText == null || listMaSingleText.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("singleText.search.notFound"));
        } else {
            Object[] arrayParam = { listMaSingleText.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("singleText.search.success"), arrayParam));
            serviceResult.setObjResult(listMaSingleText);
            serviceResult.setNumResult(listMaSingleText.size());
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
    public RestServiceResult update(RestServiceResult serviceResult, MaSingleTextForm singleTextForm) {
        MaSingleTextFormDAO singleTextFormDAO = new MaSingleTextFormDAO();
        try {
            log.info("Actualizando la respuesta abierta: " + singleTextForm.getSingleTextFormId());
            EntityManagerHelper.beginTransaction();
            singleTextFormDAO.update(singleTextForm);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(singleTextForm);
            Object[] arrayParam = { singleTextForm.getSingleTextFormId() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("singleText.update.success"), arrayParam));
            log.info("Se actualizo la respuesta Abierta con �xito: " + singleTextForm.getSingleTextFormId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la respuesta abierta: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de tecnicas
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
	 * Obtiene la lista de tecnicas
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
        MaSingleTextFormDAO singleTextFormDAO = new MaSingleTextFormDAO();
        List<MaSingleTextForm> list = singleTextFormDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("singleTextForm.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("singleTextForm.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(singleTextFormDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de tecnicas por habilidad
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nHabilityId
	 *            Es el id o c�digo de la habilidad.	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listbyId(RestServiceResult serviceResult, Long nSingleTextFormId) {
        MaSingleTextForm maSingleTextForm = new MaSingleTextFormDAO().findById(nSingleTextFormId);
        EntityManagerHelper.refresh(maSingleTextForm);
        if (maSingleTextForm == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("singleTextForm.search.notFound"));
        } else {
            List<MaSingleTextForm> list = new ArrayList<MaSingleTextForm>();
            if (list.size() == 0) {
                Object[] arrayParam = { maSingleTextForm.getSingleTextFormId() };
                serviceResult.setError(true);
                serviceResult.setMessage(MessageFormat.format(bundle.getString("singleTextForm.listbyId.notFound"), arrayParam));
            } else {
                Object[] arrayParam = { list.size(), maSingleTextForm.getSingleTextFormId() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("singleTextForm.listbyId.success"), arrayParam));
                serviceResult.setObjResult(list);
                serviceResult.setNumResult(list.size());
            }
        }
        return serviceResult;
    }
}
