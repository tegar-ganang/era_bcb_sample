package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.persistence.CoQuestion;
import edu.univalle.lingweb.persistence.CoQuestionDAO;
import edu.univalle.lingweb.persistence.CoQuestionWeighted;
import edu.univalle.lingweb.persistence.CoQuestionWeightedDAO;
import edu.univalle.lingweb.persistence.CoTest;
import edu.univalle.lingweb.persistence.CoTestDAO;
import edu.univalle.lingweb.persistence.CoTestUserHistory;
import edu.univalle.lingweb.persistence.CoTestUserHistoryDAO;
import edu.univalle.lingweb.persistence.CoTestUserHistoryId;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_test'( Pruebas)
 * 
 * @author Jose Aricapa
 */
public class DataManagerTest extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private static Logger log = Logger.getLogger(DataManagerTest.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerTest() {
        super();
        DOMConfigurator.configure(DataManagerTest.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nueva prueba en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coTest
	 *            Prueba a guardar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoTest coTest) {
        CoTestDAO coTestDAO = new CoTestDAO();
        try {
            coTest.setTestId(getSequence("sq_co_test"));
            EntityManagerHelper.beginTransaction();
            coTestDAO.save(coTest);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coTest);
            log.info("Prueba " + coTest.getTestName() + " creada con �xito...");
            Object[] arrayParam = { coTest.getTestName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la prueba: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.create.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Actualiza una una nueva prueba en la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coTest
	 *            Prueba a actualizar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoTest coTest) {
        CoTestDAO coTestDAO = new CoTestDAO();
        try {
            log.info("Actualizando la secuencia: " + coTest.getTestName());
            EntityManagerHelper.beginTransaction();
            coTestDAO.update(coTest);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coTest);
            Object[] args = { coTest.getTestName() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("test.update.success"), args));
            }
            log.info("Se actualizo la secuencia con �xito: " + coTest.getTestName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la secuencia: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.update.error"), e.getMessage()));
        }
        return serviceResult;
    }

    /**
	 * Elimina una prueba de la base de datos
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param coTest
	 *            Secuencia a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoTest coTest) {
        try {
            log.info("Eliminando la prueba: " + coTest.getTestName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_TEST);
            query.setParameter(1, coTest.getTestId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { coTest.getTestName() };
            log.info("Prueba eliminada con �xito: " + coTest.getTestName());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la prueba: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coTest.getTestName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una prueba
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param sTestName
	 *            Nombre de la prueba
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, String sTestName) {
        List<CoTest> list = new CoTestDAO().findByTestName(sTestName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("test.search.notFound"));
        } else {
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de una secuencia
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @param nTestId
	 *            C�digo de la prueba
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nTestId) {
        CoTest coTest = new CoTestDAO().findById(nTestId);
        if (coTest == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("test.search.notFound"));
        } else {
            List<CoTest> list = new ArrayList<CoTest>();
            EntityManagerHelper.refresh(coTest);
            list.add(coTest);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de pruebas
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
	 * Obtiene la lista de pruebas
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult list(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoTestDAO coTestDAO = new CoTestDAO();
        List<CoTest> list = coTestDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("test.list.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.list.success"), array));
            serviceResult.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) serviceResult.setNumResult(coTestDAO.findAll().size()); else serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de pruebas clonables
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
	 * Obtiene la lista de pruebas clonables
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoTestDAO coTestDAO = new CoTestDAO();
        List<CoTest> list = coTestDAO.findByFlagClone("1", nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("test.listClone.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.listClone.success"), array));
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
	 * Realiza el proceso de relacionar un material con una prueba
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
    public RestServiceResult createTestMaterial(RestServiceResult serviceResult, String sArrayMaterialId, CoTest coTest) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_TEST_MATERIAL);
            query.setParameter(1, coTest.getTestId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayMaterialId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                long nMaterialId = Long.parseLong(stringTokenizer.nextToken());
                query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_TEST_MATERIAL);
                query.setParameter(1, coTest.getTestId());
                query.setParameter(2, nMaterialId);
                query.executeUpdate();
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coTest.getTestName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Pregunta- Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar un material con una prueba
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
    public RestServiceResult createQuestionWeighted(RestServiceResult serviceResult, String sArrayQuestionWeightedId, CoTest coTest) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_TEST_QUESTION_WEIGHTED);
            query.setParameter(1, coTest.getTestId());
            query.executeUpdate();
            StringTokenizer stringTokenizer = new StringTokenizer(sArrayQuestionWeightedId, ",");
            while (stringTokenizer.hasMoreTokens()) {
                CoQuestionWeighted coQuestionWeighted = new CoQuestionWeighted();
                StringTokenizer stringTokenizer2 = new StringTokenizer(stringTokenizer.nextToken(), "|");
                String temp = stringTokenizer2.nextToken();
                if (temp.equals("null")) return serviceResult;
                long nQuestionNum = Long.parseLong(temp);
                double nWeighted = Double.parseDouble(stringTokenizer2.nextToken());
                coQuestionWeighted.setQuestionWeightedId(getSequence("sq_co_menu"));
                coQuestionWeighted.setQuestionNum(nQuestionNum);
                coQuestionWeighted.setWeighted(nWeighted);
                coQuestionWeighted.setCoTest(coTest);
                new CoQuestionWeightedDAO().save(coQuestionWeighted);
            }
            EntityManagerHelper.commit();
            Object[] arrayParam = { coTest.getTestName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la asociaci�n - Pregunta- Material: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("test.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Agrega un usuario al historial de modificaciones 
	 * @param serviceResult  El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param maUser Usuario a registrar 
	 * @param coTest Prueba 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n
	 */
    public static RestServiceResult addUserHistory(RestServiceResult serviceResult, MaUser maUser, CoTest coTest) {
        log.info("HISTORIAL DE PRUEBAS X USUARIO");
        CoTestUserHistory testUserHistory = new CoTestUserHistoryDAO().findById(new CoTestUserHistoryId(coTest.getTestId(), maUser.getUserId()));
        log.info("Resultado de la consulta => " + testUserHistory);
        if (testUserHistory == null) {
            log.info("Agregando HistorialxPrueba: Usuario '" + maUser.getUserId() + "' - Prueba '" + coTest.getTestId() + "'");
            addUserHistory(maUser, coTest);
        } else {
            log.info("El usuario '" + maUser.getUserId() + "' tiene la prueba '" + coTest.getTestId() + "'");
        }
        log.info("Termina HISTORIAL DE PRUEBA...");
        return serviceResult;
    }

    /**
	 * Registra en el historial de prueba un usuario
	 * 
	 * @param maUser usuario a guarda
	 * @param coTest unidad a guardar
	 */
    private static void addUserHistory(MaUser maUser, CoTest coTest) {
        try {
            CoTestUserHistoryId userHistoryId = new CoTestUserHistoryId();
            userHistoryId.setTestId(coTest.getTestId());
            userHistoryId.setUserId(maUser.getUserId());
            new CoTestUserHistoryDAO().save(new CoTestUserHistory(userHistoryId, maUser, coTest, new Date()));
            log.info("Se guardo el usuario " + maUser.getUserName() + " con �xito al historial de la prueba '" + coTest.getTestId() + "' ");
        } catch (PersistenceException e) {
            log.info("El usuario " + maUser.getUserId() + " ya esta en el  historial de la prueba '" + coTest.getTestId() + "' " + "OMITIR EXCEPCION PRIMARY KEY");
        }
        return;
    }

    /**
	 * Clona lel historial de usuarios
	 * 
	 * @param nActivityOldId
	 *            C�digo de la actividad a clonar
	 * @param nActivityNewId
	 *            C�digo de la nueva actividad
	 */
    public void cloneTest(Long nTestOldId, Long nTestNew) {
        CoTest coTestOld = new CoTestDAO().findById(nTestOldId);
        Set<CoQuestion> setQuestion = coTestOld.getCoQuestions();
        if (setQuestion.size() != 0) {
            for (Iterator<CoQuestion> iterator = setQuestion.iterator(); iterator.hasNext(); ) {
                EntityManagerHelper.beginTransaction();
                log.info("\n\n*****************************************");
                CoQuestion coQuestionOld = iterator.next();
                Long nQuestionNewId = getSequence("sq_co_question");
                Long nQuestionOldId = coQuestionOld.getQuestionId();
                Query query = EntityManagerHelper.createNativeQuery(Statements.CLONE_QUESTION);
                query.setParameter(1, nQuestionNewId);
                query.setParameter(2, coTestOld.getTestId());
                query.setParameter(3, nQuestionOldId);
                int nUpdate = query.executeUpdate();
                log.info("Clonaci�n NEWQUESTION[" + nQuestionNewId + "] = Modificados: " + nUpdate);
                EntityManagerHelper.commit();
                CoQuestion coQuestion = new CoQuestionDAO().findById(nQuestionNewId);
                EntityManagerHelper.refresh(coQuestion);
                if (coQuestion != null) {
                    EntityManagerHelper.beginTransaction();
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_QUESTION_MATERIAL);
                    query.setParameter(1, nQuestionNewId);
                    query.setParameter(2, nQuestionOldId);
                    nUpdate = query.executeUpdate();
                    log.info("Clonaci�n[" + nQuestionOldId + "] - NEWQUESTION[" + nQuestionNewId + "]" + " - Materiales " + nUpdate);
                    EntityManagerHelper.commit();
                    new DataManagerQuestion().cloneQuestion(nQuestionOldId, nQuestionNewId);
                }
            }
        }
        EntityManagerHelper.beginTransaction();
        cloneUserHistory(nTestOldId, nTestNew);
        EntityManagerHelper.rollback();
    }

    /**
	 * Permite clonar el historial de usuario de una prueba clonada
	 * @param nTestOldId C�digo de la unidad a clonar
	 * @param coUnitNew Unidad Creada a partir de clonaci�n
	 */
    private void cloneUserHistory(Long nTestOldId, Long nTestNew) {
        try {
            log.info("CLONACION HISTORIAL DE PRUEBAS X USUARIO");
            CoTest coTest = new CoTestDAO().findById(nTestOldId);
            EntityManagerHelper.refresh(coTest);
            Set<CoTestUserHistory> setUserHistoryOld = coTest.getCoTestUserHistories();
            CoTest coTestNew = new CoTestDAO().findById(nTestNew);
            for (CoTestUserHistory coTestUserHistory : setUserHistoryOld) {
                MaUser maUser = coTestUserHistory.getMaUser();
                CoTestUserHistory testUserHistory = new CoTestUserHistoryDAO().findById(new CoTestUserHistoryId(coTestNew.getTestId(), maUser.getUserId()));
                log.info("Resultado de la consulta => " + testUserHistory);
                if (testUserHistory == null) {
                    log.info("Agregando HistorialxUnidades: Usuario '" + maUser.getUserId() + "' - prueba '" + coTestNew.getTestId() + "'");
                    addUserHistory(maUser, coTestNew);
                } else {
                    log.info("El usuario '" + maUser.getUserId() + "' tiene la prueba '" + coTestNew.getTestId() + "'");
                }
            }
            log.info("Ok...Termina clonaci�n de Historial de usuarios en PRUEBAS");
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new DataManagerTest().cloneTest(new Long(55), new Long(36));
    }
}
