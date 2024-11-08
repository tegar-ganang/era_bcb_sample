package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import edu.univalle.lingweb.persistence.CoSequence;
import edu.univalle.lingweb.persistence.CoSequenceDAO;
import edu.univalle.lingweb.persistence.CoUnit;
import edu.univalle.lingweb.persistence.CoUnitDAO;
import edu.univalle.lingweb.persistence.CoUnitUserHistory;
import edu.univalle.lingweb.persistence.CoUnitUserHistoryDAO;
import edu.univalle.lingweb.persistence.CoUnitUserHistoryId;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_unit'( Unidades )
 * 
 * @author Jose Aricapa
 */
public class DataManagerUnit extends DataManager {

    /**
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerUnit.class);

    /**
	 * Constructor de la clase
	 */
    public DataManagerUnit() {
    }

    /**
	 * Realiza el proceso de guardar una unidad
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coUnit
	 *            Unidad a crear
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoUnit coUnit) {
        CoUnitDAO coUnitDAO = new CoUnitDAO();
        try {
            coUnit.setUnitId(getSequence("sq_co_unit"));
            EntityManagerHelper.beginTransaction();
            coUnitDAO.save(coUnit);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coUnit);
            log.info("Unidad" + coUnit.getUnitName() + " creada con �xito...");
            Object[] arrayParam = { coUnit.getUnitName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la unit: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza la actualizaci�n de la unidad
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param objElement
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoUnit coUnit) {
        CoUnitDAO coUnitDAO = new CoUnitDAO();
        try {
            log.info("Actualizando la unidad: " + coUnit.getUnitName());
            EntityManagerHelper.beginTransaction();
            coUnitDAO.update(coUnit);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coUnit);
            Object[] args = { coUnit.getUnitName() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.update.success"), args));
            }
            log.info("Se actualizo la unidad con �xito: " + coUnit.getUnitName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la unidad: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de la Unidad
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUnitId
	 *            C�digo de la unidad
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoUnit coUnit) {
        try {
            log.info("Eliminando la unidad: " + coUnit.getUnitName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_UNIT);
            query.setParameter(1, coUnit.getUnitId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { coUnit.getUnitName() };
            log.info("Unidad eliminada con �xito: " + coUnit.getUnitName());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la unidad: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coUnit.getUnitName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una unidad
	 * 
	 * @param result
	 *            result El {@link RestServiceResult} que contendr�n los
	 *            mensajes localizados y estado SQL .
	 * @param sUnitName
	 *            Nombre de la unidads
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nUnitId) {
        log.info("Buscando Unidad: " + nUnitId);
        CoUnit coUnit = new CoUnitDAO().findById(nUnitId);
        if (coUnit == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("unit.search.notFound"));
        } else {
            List<CoUnit> list = new ArrayList<CoUnit>();
            EntityManagerHelper.refresh(coUnit);
            list.add(coUnit);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de unidades
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
	 * Obtiene la lista de unidades
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoUnitDAO coUnitDAO = new CoUnitDAO();
        List<CoUnit> list = coUnitDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("unit.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coUnitDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de unidades clonables
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult result) {
        return listClone(result, 0, 0);
    }

    /**
	 * Obtiene la lista de unidades clonables
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoUnitDAO coUnitDAO = new CoUnitDAO();
        List<CoUnit> list = coUnitDAO.findByFlagClone("1", nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("unit.listClone.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.listClone.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) {
                RestServiceResult serviceResult2 = listClone(new RestServiceResult());
                int nNumStudent = serviceResult2.getNumResult();
                serviceResult.setNumResult(nNumStudent);
            } else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * M�todo que permite la clonaci�n de una unidad
	 * 
	 * @param nUnitOldId
	 *            C�digo del curso a clonar
	 * @param nUnitNewId
	 *            C�digo del curso nuevo
	 */
    public void cloneUnit(Long nUnitOldId, Long nUnitNewId, MaUser maUser) {
        CoUnit coUnit = new CoUnitDAO().findById(nUnitOldId);
        EntityManagerHelper.refresh(coUnit);
        EntityManagerHelper.beginTransaction();
        cloneUserHistory(nUnitOldId, nUnitNewId);
        EntityManagerHelper.commit();
        Set<CoSequence> setSequence = coUnit.getCoSequences();
        for (CoSequence coSequence : setSequence) {
            EntityManagerHelper.beginTransaction();
            Long nSequenceNewId = getSequence("sq_co_sequence");
            Query query = EntityManagerHelper.createNativeQuery(Statements.CLONE_SEQUENCE.replaceAll(":CLONE", this.getBundle().getString("course.create.clone")));
            query.setParameter(1, nSequenceNewId);
            query.setParameter(2, nUnitNewId);
            query.setParameter(3, coSequence.getSequenceId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            CoSequence coSequenceNew = new CoSequenceDAO().findById(nSequenceNewId);
            EntityManagerHelper.refresh(coSequenceNew);
            if (coSequenceNew != null) {
                EntityManagerHelper.beginTransaction();
                DataManagerSequence.addUserHistory(new RestServiceResult(), maUser, coSequenceNew);
                EntityManagerHelper.commit();
                DataManagerSequence dataManagerSequence = new DataManagerSequence();
                dataManagerSequence.setBundle(bundle);
                dataManagerSequence.cloneSequence(coSequence.getSequenceId(), nSequenceNewId, maUser);
            }
        }
    }

    /**
	 * Permite clonar el historial de usuario de una unidad clonada
	 * @param nUnitOldId C�digo de la unidad a clonar
	 * @param coUnitNew Unidad Creada a partir de clonaci�n
	 */
    private void cloneUserHistory(Long nUnitOldId, Long nUnitNew) {
        try {
            log.info("CLONACION HISTORIAL DE UNIDADES X USUARIO");
            CoUnit coUnit = new CoUnitDAO().findById(nUnitOldId);
            EntityManagerHelper.refresh(coUnit);
            Set<CoUnitUserHistory> setUserHistoryOld = coUnit.getCoUnitUserHistories();
            CoUnit coUnitNew = new CoUnitDAO().findById(nUnitNew);
            for (CoUnitUserHistory coUnitUserHistory : setUserHistoryOld) {
                MaUser maUser = coUnitUserHistory.getMaUser();
                CoUnitUserHistory unitUserHistory = new CoUnitUserHistoryDAO().findById(new CoUnitUserHistoryId(coUnitNew.getUnitId(), maUser.getUserId()));
                log.info("Resultado de la consulta => " + unitUserHistory);
                if (unitUserHistory == null) {
                    log.info("Agregando HistorialxUnidades: Usuario '" + maUser.getUserId() + "' - unidad '" + coUnitNew.getUnitId() + "'");
                    addUserHistory(maUser, coUnitNew);
                } else {
                    log.info("El usuario '" + maUser.getUserId() + "' tiene la unidad '" + coUnitNew.getUnitId() + "'");
                }
            }
            log.info("Ok...Termina clonaci�n de Historial de usuarios en UNIDADES");
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Realiza el proceso de relacionar un material con una unidad
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createUnitMaterial(RestServiceResult serviceResult, String sArrayMaterialId, CoUnit coUnit) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_UNIT_MATERIAL);
            query.setParameter(1, coUnit.getUnitId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_UNIT_MATERIAL);
                query.setParameter(1, coUnit.getUnitId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coUnit.getUnitName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.create.success"), arrayParam));
        } catch (PersistenceException e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Actividad - Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("unit.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de agregar un usuario al historial de modificaciones
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public static RestServiceResult addUserHistory(RestServiceResult serviceResult, MaUser maUser, CoUnit coUnitNew) {
        log.info("HISTORIAL DE UNIDADES X USUARIO");
        CoUnitUserHistory unitUserHistory = new CoUnitUserHistoryDAO().findById(new CoUnitUserHistoryId(coUnitNew.getUnitId(), maUser.getUserId()));
        log.info("Resultado de la consulta => " + unitUserHistory);
        if (unitUserHistory == null) {
            log.info("Agregando HistorialxUnidades: Usuario '" + maUser.getUserId() + "' - Unidad '" + coUnitNew.getUnitId() + "'");
            addUserHistory(maUser, coUnitNew);
        } else {
            log.info("El usuario '" + maUser.getUserId() + "' tiene la unidad '" + coUnitNew.getUnitId() + "'");
        }
        log.info("Termina HISTORIAL DE UNIDADES...");
        return serviceResult;
    }

    /**
	 * Registra en el historial de unidades un usuario
	 * 
	 * @param maUser usuario a guarda
	 * @param coUnit unidad a guardar
	 */
    private static void addUserHistory(MaUser maUser, CoUnit coUnit) {
        try {
            CoUnitUserHistoryId userHistoryId = new CoUnitUserHistoryId();
            userHistoryId.setUnitId(coUnit.getUnitId());
            userHistoryId.setUserId(maUser.getUserId());
            new CoUnitUserHistoryDAO().save(new CoUnitUserHistory(userHistoryId, maUser, coUnit, new Date()));
            log.info("Se guardo el usuario " + maUser.getUserName() + " con �xito al historial de la unidad '" + coUnit.getUnitId() + "' ");
        } catch (PersistenceException e) {
            log.info("El usuario " + maUser.getUserId() + " ya esta en el  historial de la unidad '" + coUnit.getUnitId() + "' " + "OMITIR EXCEPCION PRIMARY KEY");
        }
        return;
    }

    public static void main(String[] args) {
        DataManagerUnit dataManagerUnit = new DataManagerUnit();
    }
}
