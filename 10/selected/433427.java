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
import edu.univalle.lingweb.persistence.CoExercises1;
import edu.univalle.lingweb.persistence.CoScoreExercises1;
import edu.univalle.lingweb.persistence.CoScoreExercises1DAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_score_exercises1'( Calificaci�n ejercicio tipo 1 )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerScoreExerciseS1 extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerScoreExerciseS1.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerScoreExerciseS1() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva calificaci�n para el ejercicio tipo 1  en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * @param coScoreExercises1
	 *            Calificaci�n a crear 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, CoScoreExercises1 coScoreExercises1) {
        CoScoreExercises1DAO coScoreExercises1DAO = new CoScoreExercises1DAO();
        try {
            EntityManagerHelper.beginTransaction();
            coScoreExercises1DAO.save(coScoreExercises1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreExercises1);
            Object[] args = { coScoreExercises1.getMaUser().getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            result.setError(true);
            Object[] args = { coScoreExercises1.getMaUser().getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.create.error"), args));
            log.error("Error al guardar la calificaci�n para el ejercicio de tipo 1: " + e.getMessage());
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
	 * @param nExercise1Id
	 *            c�digo del ejercicio
	 * @param nUserId
	 *            c�digo del usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nExercise1Id, Long nUserId) {
        List<CoScoreExercises1> listCoScoreExercises1 = null;
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_EXERCISES1, CoScoreExercises1.class);
        query.setParameter(1, nExercise1Id);
        query.setParameter(2, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        listCoScoreExercises1 = query.getResultList();
        if (listCoScoreExercises1 == null || listCoScoreExercises1.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreExercises1.search.notFound"));
        } else {
            Object[] arrayParam = { listCoScoreExercises1.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.search.success"), arrayParam));
            serviceResult.setObjResult(listCoScoreExercises1);
            serviceResult.setNumResult(listCoScoreExercises1.size());
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
	 * @param coScoreExercises1
	 *            Calificaci�n a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoScoreExercises1 coScoreExercises1) {
        CoScoreExercises1DAO coScoreExercises1DAO = new CoScoreExercises1DAO();
        try {
            log.info("Actualizando la calificaci�n para el ejecicio1: " + coScoreExercises1.getScore());
            EntityManagerHelper.beginTransaction();
            coScoreExercises1DAO.update(coScoreExercises1);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreExercises1);
            Object[] arrayParam = { coScoreExercises1.getMaUser().getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.update.success"), arrayParam));
            log.info("Se actualizo la calificaci�n con �xito para el usuario: " + coScoreExercises1.getMaUser().getUserName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar la t�cnica: " + e.getMessage());
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
	 * @param coScoreExercises1
	 *            Calificaci�n a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoScoreExercises1 coScoreExercises1) {
        String sUserName = null;
        try {
            sUserName = coScoreExercises1.getMaUser().getUserName();
            log.error("Eliminando la calificaci�n del estudiante: " + sUserName);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_SCORE_EXERCISE1);
            query.setParameter(1, coScoreExercises1.getCoExercises1().getExerciseId());
            query.setParameter(2, coScoreExercises1.getMaUser().getUserId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreExercises1);
            Object[] arrayParam = { sUserName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.delete.success"), arrayParam));
            log.info("Eliminando la calificaci�n para el estudiante: " + sUserName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la calificaci�n: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coScoreExercises1.getMaUser().getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.delete.error") + e.getMessage(), args));
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
        CoScoreExercises1DAO coScoreExercises1DAO = new CoScoreExercises1DAO();
        List<CoScoreExercises1> list = coScoreExercises1DAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("coScoreExercises1.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coScoreExercises1DAO.findAll().size()); else result.setNumResult(list.size());
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
    public RestServiceResult listScoreExerciseS1ForUser(RestServiceResult serviceResult, Long nUserId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreExercises1.search.notFound"));
        } else {
            List<CoScoreExercises1> list = new ArrayList<CoScoreExercises1>(maUser.getCoScoreExercises1s());
            Object[] arrayParam = { list.size(), maUser.getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreExercises1.listScoreExerciseS1ForUser.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Valida el tipo de anuncio para saber que tipo de ejercicio de refuerzo se recomienda
	 * @param coScoreExercises1 calificaci�n del ejercicio
	 * @return String tipo de anuncio a generar con relaci�n a los ejercicios complementarios
	 */
    public Long validateAnnouncementType(CoScoreExercises1 coScoreExercises1) {
        String sScoreType = coScoreExercises1.getCoExercises1().getFlagScoreType();
        if (sScoreType.equalsIgnoreCase("0")) {
            Float nScore = new Float(coScoreExercises1.getScore());
            if (nScore < Common.CRITIC_VALUE_QUANTITATIVE) {
                return Constant.STRENGTHENING_EXERCISE;
            } else if (nScore >= Common.EXELLENT_VALUE_QUANTITATIVE) {
                return Constant.EXERCISE_OF_DEEPENING;
            } else {
                return new Long(0);
            }
        } else {
            Long nScore = new Long(coScoreExercises1.getScore());
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
	 * @param coScoreExercises1 calificaci�n del ejercicio
	 */
    public void createScoredAnnouncement(CoScoreExercises1 coScoreExercises1) {
        String sExerciseName = coScoreExercises1.getCoExercises1().getExerciseName();
        Long nUserId = coScoreExercises1.getMaUser().getUserId();
        String sUserName = coScoreExercises1.getMaUser().getUserName();
        Float nScore = new Float(coScoreExercises1.getScore());
        String sScore = "";
        String sDate = Util.parseToLocalDate(new Date());
        Long nAnnouncemntType = Constant.SCORED_EXERCISE;
        String sFlagScoreType = coScoreExercises1.getCoExercises1().getFlagScoreType();
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
	 * @param coScoreExercises1 calificaci�n del ejercicio
	 */
    public void createCompExercAnnouncement(CoScoreExercises1 coScoreExercises1) {
        Long annType = validateAnnouncementType(coScoreExercises1);
        CoExercises1 coExercises1 = coScoreExercises1.getCoExercises1();
        String sExerciseName = coExercises1.getExerciseName();
        Long nUserId = coScoreExercises1.getMaUser().getUserId();
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
