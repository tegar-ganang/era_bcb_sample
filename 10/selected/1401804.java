package edu.univalle.lingweb.model;

import jade.wrapper.gateway.JadeGateway;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import edu.univalle.lingweb.persistence.CoQuestion;
import edu.univalle.lingweb.persistence.CoQuestionWeighted;
import edu.univalle.lingweb.persistence.CoScoreQuestion;
import edu.univalle.lingweb.persistence.CoScoreQuestionDAO;
import edu.univalle.lingweb.persistence.CoTest;
import edu.univalle.lingweb.persistence.CoTestDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_score_question'( Calificaci�n preguntas )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerScoreQuestion extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerScoreQuestion.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerScoreQuestion() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva calificaci�n para la pregunta en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * @param coScoreQuestion
	 *            Calificaci�n a crear 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, CoScoreQuestion coScoreQuestion) {
        CoScoreQuestionDAO coScoreQuestionDAO = new CoScoreQuestionDAO();
        try {
            EntityManagerHelper.beginTransaction();
            coScoreQuestionDAO.save(coScoreQuestion);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreQuestion);
            Object[] args = { coScoreQuestion.getMaUser().getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            result.setError(true);
            Object[] args = { coScoreQuestion.getMaUser().getUserName() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.create.error"), args));
            log.error("Error al guardar la calificaci�n para la pregunta: " + e.getMessage());
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
	 * @param nQuestionId
	 *            c�digo de la pregunta
	 * @param nUserId
	 *            c�digo del usuario
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nQuestionId, Long nUserId) {
        List<CoScoreQuestion> listCoScoreQuestion = null;
        Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_QUESTION, CoScoreQuestion.class);
        query.setParameter(1, nQuestionId);
        query.setParameter(2, nUserId);
        query.setHint(QueryHints.REFRESH, HintValues.TRUE);
        List listRest = new ArrayList();
        listCoScoreQuestion = query.getResultList();
        if (listCoScoreQuestion == null || listCoScoreQuestion.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreQuestion.search.notFound"));
        } else {
            Object[] arrayParam = { listCoScoreQuestion.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.search.success"), arrayParam));
            listRest.add(listCoScoreQuestion);
            serviceResult.setObjResult(listRest);
            serviceResult.setNumResult(listCoScoreQuestion.size());
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
	 * @param coScoreQuestion
	 *            calificaci�n a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoScoreQuestion coScoreQuestion) {
        CoScoreQuestionDAO coScoreQuestionDAO = new CoScoreQuestionDAO();
        try {
            log.info("Actualizando la calificaci�n para la pregunta: " + coScoreQuestion.getScore());
            EntityManagerHelper.beginTransaction();
            coScoreQuestionDAO.update(coScoreQuestion);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreQuestion);
            Object[] arrayParam = { coScoreQuestion.getMaUser().getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.update.success"), arrayParam));
            log.info("Se actualizo la calificaci�n con �xito para el usuario: " + coScoreQuestion.getMaUser().getUserName());
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
	 * @param coCourse
	 *            Curso a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoScoreQuestion coScoreQuestion) {
        String sUserName = null;
        try {
            sUserName = coScoreQuestion.getMaUser().getUserName();
            log.error("Eliminando la calificaci�n del estudiante: " + sUserName);
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_SCORE_QUESTION);
            query.setParameter(1, coScoreQuestion.getCoQuestion().getQuestionId());
            query.setParameter(2, coScoreQuestion.getMaUser().getUserId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coScoreQuestion);
            Object[] arrayParam = { sUserName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.delete.success"), arrayParam));
            log.info("Eliminando la calificaci�n para el estudiante: " + sUserName);
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la calificaci�n: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coScoreQuestion.getMaUser().getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.delete.error") + e.getMessage(), args));
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
	 *            localizados y estado SQL.
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la
	 *            consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result, int nRowStart, int nMaxResults) {
        CoScoreQuestionDAO coScoreQuestionDAO = new CoScoreQuestionDAO();
        List listRest = new ArrayList();
        List<CoScoreQuestion> list = coScoreQuestionDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("coScoreQuestion.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.list.success"), array));
            listRest.add(list);
            result.setObjResult(listRest);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coScoreQuestionDAO.findAll().size()); else result.setNumResult(list.size());
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
    public RestServiceResult listScoreQuestionForUser(RestServiceResult serviceResult, Long nUserId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreQuestion.search.notFound"));
        } else {
            List listRest = new ArrayList();
            List<CoScoreQuestion> list = new ArrayList<CoScoreQuestion>(maUser.getCoScoreQuestions());
            Object[] arrayParam = { list.size(), maUser.getUserName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.listScoreQuestionForUser.success"), arrayParam));
            listRest.add(list);
            serviceResult.setObjResult(listRest);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de calificaciones por estudiante de acuerdo a una prueba
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUserId
	 *            Es el id o c�digo del estudiante.
	 * @param nTestId
	 *            C�digo de la prueba	 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listScoreQuestionForUserAndTest(RestServiceResult serviceResult, Long nUserId, Long nTestId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        CoTest coTest = new CoTestDAO().findById(nTestId);
        EntityManagerHelper.refresh(coTest);
        if (maUser == null || coTest == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("coScoreQuestion.search.notFound"));
        } else {
            List<CoScoreQuestion> listCoScoreQuestion = null;
            Query query = EntityManagerHelper.createNativeQuery(Statements.SELECT_CO_SCORE_QUESTION_BY_TEST, CoScoreQuestion.class);
            query.setParameter(1, nTestId);
            query.setParameter(2, nUserId);
            query.setHint(QueryHints.REFRESH, HintValues.TRUE);
            List listRest = new ArrayList();
            listCoScoreQuestion = query.getResultList();
            if (listCoScoreQuestion == null || listCoScoreQuestion.size() == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("coScoreQuestion.search.notFound"));
            } else {
                Object[] arrayParam = { listCoScoreQuestion.size(), maUser.getUserName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("coScoreQuestion.listScoreQuestionForUser.success"), arrayParam));
                Hashtable<Long, Double> hashTable = getCoQuestionWeighted(coTest, listCoScoreQuestion);
                listRest.add(listCoScoreQuestion);
                listRest.add(hashTable);
                serviceResult.setObjResult(listRest);
                serviceResult.setNumResult(listCoScoreQuestion.size());
            }
        }
        return serviceResult;
    }

    /**
	 * Obtiene una tabla de claves con los diferentes porcentajes para cada pregunta
	 * @param coTest prueba
	 * @return hashTable con los items y su respectivo porcentaje
	 */
    private Hashtable<Long, Double> getCoQuestionWeighted(CoTest coTest, List listScoreQuestion) {
        Set<CoQuestionWeighted> hashWeighted = coTest.getCoQuestionWeighteds();
        Hashtable<Long, Double> hashTable = new Hashtable<Long, Double>();
        Double nWeighted = new Double(0);
        if (listScoreQuestion.size() != 0) for (int i = 0; i < listScoreQuestion.size(); i++) {
            CoScoreQuestion coScoreQuestion = (CoScoreQuestion) listScoreQuestion.get(i);
            EntityManagerHelper.refresh(coScoreQuestion);
            nWeighted = getWeighted(hashWeighted, coScoreQuestion.getCoQuestion());
            hashTable.put(coScoreQuestion.getCoQuestion().getItem(), nWeighted);
        }
        return hashTable;
    }

    /**
	 * Obtiene el porcentaje de una pregunta especifica
	 * @param hashWeighted Clase que representa la tabla de porcentaje de las preguntas
	 * @param coQuestion preguntas
	 * @return porcentaje de la pregunta pasada como argumento
	 */
    public Double getWeighted(Set<CoQuestionWeighted> hashWeighted, CoQuestion coQuestion) {
        Double nWeighted = new Double(0);
        if (hashWeighted != null) for (Iterator iteratorWeighted = hashWeighted.iterator(); iteratorWeighted.hasNext(); ) {
            CoQuestionWeighted coQuestionWeighted = (CoQuestionWeighted) iteratorWeighted.next();
            if (coQuestionWeighted.getQuestionNum().equals(coQuestion.getItem())) {
                nWeighted = coQuestionWeighted.getWeighted();
                break;
            }
        }
        return nWeighted;
    }

    /**
	 * Valida el tipo de anuncio para saber que tipo de ejercicio de refuerzo se recomienda
	 * @param coScoreQuestion calificaci�n del ejercicio
	 * @return String tipo de anuncio a generar con relaci�n a los ejercicios complementarios
	 */
    public Long validateAnnouncementType(CoScoreQuestion coScoreQuestion) {
        Float nScore = new Float(coScoreQuestion.getScore());
        if (nScore < Common.CRITIC_VALUE_QUANTITATIVE) {
            return Constant.STRENGTHENING_EXERCISE;
        } else if (nScore >= Common.EXELLENT_VALUE_QUANTITATIVE) {
            return Constant.EXERCISE_OF_DEEPENING;
        } else {
            return new Long(0);
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
	 * @param coScoreQuestion calificaci�n del ejercicio
	 */
    public void createScoredAnnouncement(CoScoreQuestion coScoreQuestion) {
        String sExerciseName = coScoreQuestion.getCoQuestion().getQuestionName();
        Long nUserId = coScoreQuestion.getMaUser().getUserId();
        String sUserName = coScoreQuestion.getMaUser().getUserName();
        Float nScore = new Float(coScoreQuestion.getScore());
        String sScore = nScore.toString();
        String sDate = Util.parseToLocalDate(new Date());
        Long nAnnouncemntType = Constant.SCORED_EXERCISE;
        Announcement announcement = UtilAnnouncement.buildAnnouncementForType(sExerciseName, sUserName, sScore, sDate, nAnnouncemntType, bundle);
        createAndSendAnnouncementForUser(announcement, nUserId, nAnnouncemntType);
    }

    /**
	 * Permite crea un anuncio complementario ya se de profundizaci�n o refuerzo.
	 * @param coScoreQuestion calificaci�n del ejercicio
	 */
    public void createCompExercAnnouncement(CoScoreQuestion coScoreQuestion) {
        Long annType = validateAnnouncementType(coScoreQuestion);
        CoQuestion coQuestion = coScoreQuestion.getCoQuestion();
        String sExerciseName = coQuestion.getQuestionName();
        Long nUserId = coScoreQuestion.getMaUser().getUserId();
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
