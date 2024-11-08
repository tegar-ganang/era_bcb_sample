package edu.univalle.lingweb.model;

import jade.wrapper.gateway.JadeGateway;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.agent.model.Announcement;
import edu.univalle.lingweb.agent.model.AnnouncementEvent;
import edu.univalle.lingweb.agent.model.Constant;
import edu.univalle.lingweb.persistence.AgAnnouncement;
import edu.univalle.lingweb.persistence.CoExercises2;
import edu.univalle.lingweb.persistence.CoScoreExercises2;
import edu.univalle.lingweb.persistence.CoScoreExercises2DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_score_exercises2'( Calificaci�n ejercicio tipo 2 )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerScoreExerciseS2 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerScoreExerciseS2.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerScoreExerciseS2() {
        super();
        DOMConfigurator.configure(DataManagerScoreExerciseS2.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva calificaci�n para el ejercicio tipo 2  en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * @param coScoreExercises2
	 *            Calificaci�n a crear 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, CoScoreExercises2 coScoreExercises2) {
        CoScoreExercises2DAO coScoreExercises2DAO = new CoScoreExercises2DAO();
        try {
            EntityManagerHelper.beginTransaction();
            coScoreExercises2DAO.save(coScoreExercises2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreExercises2);
            Object[] args = { coScoreExercises2.getMaUser().getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            result.setError(true);
            Object[] args = { coScoreExercises2.getMaUser().getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.create.error"), args));
            log.error("Error al guardar la calificaci�n para el ejercicio de tipo 2: " + e.getMessage());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de una calificaci�n
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nExercise2Id
	 *            c�digo del ejercicio
	 * @param nUserId
	 *            c�digo del usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nExercise2Id, Long nUserId) {
        List<CoScoreExercises2> listCoScoreExercises2 = null;
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_EXERCISES2, CoScoreExercises2.class);
        query.setParameter(1, nExercise2Id);
        query.setParameter(2, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoScoreExercises2 = query.getResultList();
        if (listCoScoreExercises2 == null || listCoScoreExercises2.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreExercises2.search.notFound"));
        } else {
            Object[] arrayParam = { listCoScoreExercises2.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.search.success"), arrayParam));
            serviceResult.setObjResult(listCoScoreExercises2);
            serviceResult.setNumResult(listCoScoreExercises2.size());
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos de una calificaci�n
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coScoreExercises2
	 *            Calificaci�n a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoScoreExercises2 coScoreExercises2) {
        CoScoreExercises2DAO coScoreExercises2DAO = new CoScoreExercises2DAO();
        try {
            log.info("Actualizando la calificaci�n para el ejercicio2: " + coScoreExercises2.getScore());
            EntityManagerHelper.beginTransaction();
            coScoreExercises2DAO.update(coScoreExercises2);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreExercises2);
            Object[] arrayParam = { coScoreExercises2.getMaUser().getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.update.success"), arrayParam));
            log.info("Se actualizo la calificaci�n con �xito para el usuario: " + coScoreExercises2.getMaUser().getUserName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la calificaci�n del ejercicio2: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de una calificaci�n
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coScoreExercises2
	 *            Calificaci�n a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoScoreExercises2 coScoreExercises2) {
        String sUserName = null;
        try {
            sUserName = coScoreExercises2.getMaUser().getUserName();
            log.error("Eliminando la calificaci�n del estudiante: " + sUserName);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_SCORE_EXERCISE2);
            query.setParameter(1, coScoreExercises2.getCoExercises2().getExerciseId());
            query.setParameter(2, coScoreExercises2.getMaUser().getUserId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreExercises2);
            Object[] arrayParam = { sUserName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.delete.success"), arrayParam));
            log.info("Eliminando la calificaci�n para el estudiante: " + sUserName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la calificaci�n: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coScoreExercises2.getMaUser().getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de calificaciones
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
	 * Obtiene la lista de calificaciones
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
        CoScoreExercises2DAO coScoreExercises2DAO = new CoScoreExercises2DAO();
        List<CoScoreExercises2> list = coScoreExercises2DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("coScoreExercises2.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coScoreExercises2DAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de calificaciones por estudiante
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUserId
	 *            Es el id o c�digo del estudiante.	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listScoreExerciseS2ForUser(RestServiceResult serviceResult, Long nUserId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreExercises2.search.notFound"));
        } else {
            List<CoScoreExercises2> list = new ArrayList<CoScoreExercises2>(maUser.getCoScoreExercises2s());
            Object[] arrayParam = { list.size(), maUser.getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises2.listScoreExerciseS2ForUser.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Valida el tipo de anuncio para saber que tipo de ejercicio de refuerzo se recomienda
	 * @param coScoreExercises2 calificaci�n del ejercicio
	 * @return String tipo de anuncio a generar con relaci�n a los ejercicios complementarios
	 */
    public Long validateAnnouncementType(CoScoreExercises2 coScoreExercises2) {
        String sFlagScoreType = coScoreExercises2.getCoExercises2().getFlagScoreType();
        if (sFlagScoreType.equalsIgnoreCase("0")) {
            Float nScore = new Float(coScoreExercises2.getScore());
            if (nScore < Common.CRITIC_VALUE_QUANTITATIVE) {
                return Constant.STRENGTHENING_EXERCISE;
            } else if (nScore >= Common.EXELLENT_VALUE_QUANTITATIVE) {
                return Constant.EXERCISE_OF_DEEPENING;
            } else {
                return new Long(0);
            }
        } else {
            Long nScore = new Long(coScoreExercises2.getScore());
            if (nScore < Common.CRITIC_VALUE_QUALITATIVE) {
                return Constant.STRENGTHENING_EXERCISE;
            } else if (nScore >= Common.EXELLENT_VALUE_QUALITATIVE) {
                return Constant.EXERCISE_OF_DEEPENING;
            } else {
                return new Long(0);
            }
        }
    }

    /**
	 * crear anuncio y lo almacena en la base de datos, envia anuncio al agente usuario
	 * @param announcement anuncio
	 * @param nUserId c�digo del usuario
	 * @param nAnnouncementType tipo de anuncio
	 */
    public void createAndSendAnnouncementForUser(Announcement announcement, Long nUserId, Long nAnnouncementType) {
        DataManagerAgAnnouncement dataManagerAgAnnouncement = new DataManagerAgAnnouncement();
        AgAnnouncement agAnnouncement = dataManagerAgAnnouncement.buildAgAnnouncement(announcement);
        RestServiceResult serviceResult = dataManagerAgAnnouncement.create(new RestServiceResult(), agAnnouncement);
        if (!serviceResult.isError()) dataManagerAgAnnouncement.addAgAnnouncementUser(agAnnouncement, nUserId);
        AnnouncementEvent announcementEvent = new AnnouncementEvent();
        jade.util.leap.List arrayUsersId = new jade.util.leap.ArrayList();
        jade.util.leap.List listAnnouncement = new jade.util.leap.ArrayList();
        arrayUsersId.add(nUserId.toString());
        listAnnouncement.add(announcement);
        announcementEvent.setArrayUsersId(arrayUsersId);
        announcementEvent.setTypeEvent(nAnnouncementType);
        announcementEvent.setListAnnouncement(listAnnouncement);
        try {
            JadeGateway.execute(announcementEvent);
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Error al enviarle el mensaje al usuario: " + nUserId);
        }
        log.info("retorna el objeto announcementEvent...");
    }

    /**
	 * Permite crear un anuncio de ejercicio calificado.
	 * @param coScoreExercises2 calificaci�n del ejercicio
	 */
    public void createScoredAnnouncement(CoScoreExercises2 coScoreExercises2) {
        String sExerciseName = coScoreExercises2.getCoExercises2().getExerciseName();
        Long nUserId = coScoreExercises2.getMaUser().getUserId();
        String sUserName = coScoreExercises2.getMaUser().getUserName();
        Float nScore = new Float(coScoreExercises2.getScore());
        String sScore = "";
        String sDate = Util.parseToLocalDate(new Date());
        Long nAnnouncemntType = Constant.SCORED_EXERCISE;
        String sFlagScoreType = coScoreExercises2.getCoExercises2().getFlagScoreType();
        if (sFlagScoreType.equals("0")) {
            sScore = nScore.toString();
        } else if (sFlagScoreType.equals("1")) {
            if (nScore.equals(Common.EXCELLENT)) {
                sScore = "excelente";
            } else if (nScore.equals(Common.VERY_GOOD)) {
                sScore = "muy bien";
            } else if (nScore.equals(Common.GOOD)) {
                sScore = "bien";
            } else if (nScore.equals(Common.NEEDS_IMPROVEMENT)) {
                sScore = "necesita mejorar";
            } else {
                sScore = "inaceptable";
            }
        }
        Announcement announcement = UtilAnnouncement.buildAnnouncementForType(sExerciseName, sUserName, sScore, sDate, nAnnouncemntType, bundle);
        createAndSendAnnouncementForUser(announcement, nUserId, nAnnouncemntType);
    }

    /**
	 * Permite crea un anuncio complementario ya se de profundizaci�n o refuerzo.
	 * @param coScoreExercises2 calificaci�n del ejercicio
	 */
    public void createCompExercAnnouncement(CoScoreExercises2 coScoreExercises2) {
        Long annType = validateAnnouncementType(coScoreExercises2);
        CoExercises2 coExercises2 = coScoreExercises2.getCoExercises2();
        String sExerciseName = coExercises2.getExerciseName();
        Long nUserId = coScoreExercises2.getMaUser().getUserId();
        String sDate = Util.parseToLocalDate(new Date());
        Announcement announcement = null;
        if (annType != 0) {
            if (annType.equals(Constant.STRENGTHENING_EXERCISE)) {
                announcement = UtilAnnouncement.buildAnnouncementForType(sExerciseName, "", "", sDate, annType, bundle);
            } else if (annType.equals(Constant.EXERCISE_OF_DEEPENING)) {
                announcement = UtilAnnouncement.buildAnnouncementForType(sExerciseName, "", "", sDate, annType, bundle);
            }
            createAndSendAnnouncementForUser(announcement, nUserId, annType);
        }
    }
}
