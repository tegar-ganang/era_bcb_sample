package edu.univalle.lingweb.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import com.csvreader.CsvReader;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.CoActivity;
import edu.univalle.lingweb.persistence.CoActivityDAO;
import edu.univalle.lingweb.persistence.CoCourse;
import edu.univalle.lingweb.persistence.CoCourseDAO;
import edu.univalle.lingweb.persistence.CoCourseUser;
import edu.univalle.lingweb.persistence.CoCourseUserDAO;
import edu.univalle.lingweb.persistence.CoCourseUserHistory;
import edu.univalle.lingweb.persistence.CoCourseUserHistoryDAO;
import edu.univalle.lingweb.persistence.CoCourseUserHistoryId;
import edu.univalle.lingweb.persistence.CoCourseUserId;
import edu.univalle.lingweb.persistence.CoMaterial;
import edu.univalle.lingweb.persistence.CoMaterialDAO;
import edu.univalle.lingweb.persistence.CoUnit;
import edu.univalle.lingweb.persistence.CoUnitDAO;
import edu.univalle.lingweb.persistence.CoUserExer2Group;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaRoleDAO;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros para la tabla 'co_course'( Cursos )
 * 
 * @author Jose Aricapa
 */
public class DataManagerCourse extends DataManager {

    /**
	 * @uml.property name="log"
	 * @uml.associationEnd multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerCourse.class);

    public DataManagerCourse() {
        super();
        DOMConfigurator.configure(DataManagerCourse.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea un nuevo curso en la base de datos
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, CoCourse coCourse) {
        CoCourseDAO coCourseDAO = new CoCourseDAO();
        try {
            coCourse.setCourseId(getSequence("sq_co_course"));
            EntityManagerHelper.beginTransaction();
            coCourseDAO.save(coCourse);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coCourse);
            Object[] args = { coCourse.getCourseName(), coCourse.getCourseId() };
            result.setMessage(MessageFormat.format(bundle.getString("course.create.success"), args));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            e.printStackTrace();
            log.error("Error al guardar el curso: " + e.getMessage());
            result.setError(true);
            result.setMessage(e.getMessage());
        }
        return result;
    }

    /**
	 * Actualiza los datos de un curso
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coCourse
	 *            curso a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoCourse coCourse) {
        CoCourseDAO coCourseDAO = new CoCourseDAO();
        try {
            log.info("Actualizando el curso: " + coCourse.getCourseName());
            EntityManagerHelper.beginTransaction();
            coCourseDAO.update(coCourse);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coCourse);
            Object[] arrayParam = { coCourse.getCourseName() };
            if (bundle != null) {
                serviceResult.setMessage(MessageFormat.format(bundle.getString("course.update.success"), arrayParam));
            }
            log.info("Se actualizo el curso con �xito: " + coCourse.getCourseName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(e.getMessage());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un curso
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nCourseId
	 *            c�digo del programa del curso
	 * @param nUserId
	 *            c�digo o id del usuario que realiza la petici�n
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nCourseId, Long nUserId) {
        CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
        EntityManagerHelper.refresh(coCourse);
        if (nUserId != 0) {
            MaUser maUser = new MaUserDAO().findById(nUserId);
            EntityManagerHelper.refresh(maUser);
            serviceResult.setRoleUser(maUser.getMaRole().getRoleId());
            serviceResult.setId(nUserId);
        }
        if (coCourse == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.search.notFound"));
        } else {
            List<CoCourse> list = new ArrayList<CoCourse>();
            EntityManagerHelper.refresh(coCourse);
            list.add(coCourse);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.search.success"), arrayParam));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un curso a trav�s de su c�digo
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param sCourseCod
	 *            c�digo del programa del curso
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult searchCourseCod(RestServiceResult serviceResult, String sCourseCod) {
        List<CoCourse> list = new CoCourseDAO().findByCourseCod(sCourseCod);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.search.notFound"));
        } else {
            Object[] args = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.search.success"), args));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la busqueda de un curso
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param sCourseName
	 *            c�digo del programa del curso
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult searchCourseName(RestServiceResult serviceResult, String sCourseName) {
        List<CoCourse> list = new CoCourseDAO().findByCourseName(sCourseName);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.search.notFound"));
        } else {
            Object[] args = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.search.success"), args));
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de un curso
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la operaci�n.
	 * @param coCourse
	 *            Curso a eliminar
	 * @return El {@link RestServiceResult} contiene el resultado de la operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoCourse coCourse) {
        String sCourseName = null;
        try {
            sCourseName = coCourse.getCourseName();
            log.error("Eliminando el curso: " + coCourse.getCourseName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_COURSE);
            query.setParameter(1, coCourse.getCourseId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { sCourseName };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.delete.success"), arrayParam));
            log.info("Eliminando el curso: " + coCourse.getCourseName());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar el curso: " + e.getMessage());
            serviceResult.setError(true);
            Object[] args = { coCourse.getCourseName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.delete.error") + e.getMessage(), args));
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de cursos
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult list(RestServiceResult result) {
        return list(result, 0, 0);
    }

    /**
	 * Obtiene la lista de cursos
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
        CoCourseDAO coCourseDAO = new CoCourseDAO();
        List<CoCourse> list = coCourseDAO.findAll(nRowStart, nMaxResults);
        if (list.size() == 0) {
            result.setError(true);
            result.setMessage(bundle.getString("course.list.notFound"));
        } else {
            Object[] array = { list.size() };
            result.setMessage(MessageFormat.format(bundle.getString("course.list.success"), array));
            result.setObjResult(list);
            if ((nRowStart > 0) || (nMaxResults > 0)) result.setNumResult(coCourseDAO.findAll().size()); else result.setNumResult(list.size());
        }
        return result;
    }

    /**
	 * Obtiene la lista de cursos clonables
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult result) {
        return listClone(result, 0, 0);
    }

    /**
	 * Obtiene la lista de cursos clonables
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nRowStart
	 *            Especifica el �ndice de la fila en los resultados de la consulta.
	 * @param nMaxResults
	 *            Especifica el m�ximo n�mero de resultados a retornar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult listClone(RestServiceResult serviceResult, int nRowStart, int nMaxResults) {
        CoCourseDAO coCourseDAO = new CoCourseDAO();
        List<CoCourse> list = coCourseDAO.findByFlagClone("1", nRowStart, nMaxResults);
        if (list.size() == 0) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.listClone.notFound"));
        } else {
            Object[] array = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.listClone.success"), array));
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
	 * Obtiene el arbol XML de cursos por profesor
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult getTreeCourseMaster(RestServiceResult serviceResult, Long nTearcherId) {
        List<CoCourse> listCourse = null;
        MaUserDAO maUserDAO = new MaUserDAO();
        MaUser maUser = maUserDAO.findById(nTearcherId);
        Long nRoleId = maUser.getMaRole().getRoleId();
        Long nUserId = maUser.getUserId();
        serviceResult.setRoleUser(nRoleId);
        serviceResult.setId(nUserId);
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.list.notFound"));
        } else {
            EntityManagerHelper.refresh(maUser);
            Set<CoCourse> hashCourseUser = maUser.getCoCoursesForUserId();
            listCourse = new ArrayList<CoCourse>();
            for (Iterator iterator = hashCourseUser.iterator(); iterator.hasNext(); ) {
                CoCourse coCourse = (CoCourse) iterator.next();
                EntityManagerHelper.refresh(coCourse);
                listCourse.add(coCourse);
            }
            if (listCourse.size() == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("course.list.notFound"));
            } else {
                Object[] array = { listCourse.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("course.list.success"), array));
                serviceResult.setObjResult(listCourse);
            }
        }
        return serviceResult;
    }

    /**
	 * Agrega una lista de estudiantes a un curso
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nCourseId  C�digo del curso
	 * @param sArrayStuden Identificadores de estudiantes separados por comas.
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult addStudent(RestServiceResult serviceResult, Long nCourseId, String sArrayStudent, boolean isFile) {
        List<MaUser> listUserInsert = null;
        String sSql = "";
        Query query = null;
        try {
            CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
            if (coCourse == null) {
                serviceResult.setMessage(bundle.getString("course.search.notFound"));
                serviceResult.setError(true);
            } else {
                EntityManagerHelper.beginTransaction();
                if (!isFile) {
                    sSql = Statements.UPDATE_FLAG_Y_STUDENTS;
                    sSql = sSql.replaceFirst("v1", sArrayStudent);
                    query = EntityManagerHelper.createNativeQuery(sSql);
                    query.setParameter(1, "Y");
                    query.setParameter(2, nCourseId);
                    query.executeUpdate();
                }
                sSql = Statements.UPDATE_FLAG_N_STUDENTS;
                sSql = sSql.replaceFirst("v1", sArrayStudent);
                query = EntityManagerHelper.createNativeQuery(sSql);
                query.setParameter(1, "N");
                query.setParameter(2, nCourseId);
                query.executeUpdate();
                sSql = Statements.SELECT_MA_USER_IN;
                sSql = sSql.replaceFirst("v1", sArrayStudent);
                query = EntityManagerHelper.createNativeQuery(sSql, MaUser.class);
                query.setHint(QueryHints.REFRESH, HintValues.TRUE);
                listUserInsert = query.getResultList();
                for (Iterator<MaUser> iterator = listUserInsert.iterator(); iterator.hasNext(); ) {
                    MaUser maUser = iterator.next();
                    query = EntityManagerHelper.createNativeQuery(Statements.SELECT_EXIST_STUDENT_COURSE, CoCourseUser.class);
                    query.setParameter(1, maUser.getUserId());
                    query.setParameter(2, nCourseId);
                    query.setHint(QueryHints.REFRESH, HintValues.TRUE);
                    Vector vecResult = (Vector) query.getResultList();
                    if (vecResult.size() == 0) {
                        CoCourseUserId coCourseUserHistoryId = new CoCourseUserId(maUser.getUserId(), coCourse.getCourseId());
                        CoCourseUser coCourseUser = new CoCourseUser();
                        coCourseUser.setCoCourse(coCourse);
                        coCourseUser.setMaUser(maUser);
                        coCourseUser.setFlagDeleted("N");
                        coCourseUser.setId(coCourseUserHistoryId);
                        new CoCourseUserDAO().save(coCourseUser);
                    } else {
                    }
                }
                EntityManagerHelper.commit();
                Object[] arrayParam = { coCourse.getCourseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("course.addStudent.success"), arrayParam));
            }
        } catch (PersistenceException e) {
            e.printStackTrace();
            Object[] arrayParam = { e.getMessage() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.addStudent.error)"), arrayParam));
            serviceResult.setError(true);
            EntityManagerHelper.rollback();
        }
        return serviceResult;
    }

    /**
	 * Eliminar la asociaci�n de una lista de estudiantes a un curso.
	 * @param serviceResult El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param nCourseId  C�digo del curso
	 * @param sArrayStuden Identificadores de estudiantes separados por comas.
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult removeStudent(RestServiceResult serviceResult, Long nCourseId, String sArrayStudent) {
        String sSql = "";
        Query query = null;
        try {
            CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
            if (coCourse == null) {
                serviceResult.setMessage(bundle.getString("course.search.notFound"));
                serviceResult.setError(true);
            } else {
                EntityManagerHelper.beginTransaction();
                sSql = Statements.REMOVE_STUDENTS_IN_COURSE;
                sSql = sSql.replaceFirst("v1", sArrayStudent);
                query = EntityManagerHelper.createNativeQuery(sSql);
                query.setParameter(1, nCourseId);
                query.executeUpdate();
                EntityManagerHelper.commit();
                Object[] arrayParam = { coCourse.getCourseName() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("course.removeStudent.success"), arrayParam));
            }
        } catch (PersistenceException e) {
            e.printStackTrace();
            Object[] arrayParam = { e.getMessage() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.removeStudent.error)"), arrayParam));
            serviceResult.setError(true);
            EntityManagerHelper.rollback();
        }
        return serviceResult;
    }

    /**
	 * M�todo que permite guardar usuarios a partir de un archivos CSV
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param archivo
	 *            a importar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult addStudentMasive(RestServiceResult serviceResult, File fileImport) {
        FileReader fr = null;
        String sArrayStudent = null;
        HashMap<Long, String> tableStudentForCourse = null;
        CsvReader cvsReader = null;
        FileReader freader = null;
        try {
            freader = new FileReader(fileImport);
            cvsReader = new CsvReader(freader, ";".charAt(0));
            String[] headers = null;
            int nCount = 0;
            Vector<String> vecError = new Vector<String>();
            Vector<String> vecSuccess = new Vector<String>();
            Vector<String> vecWarning = new Vector<String>();
            if (cvsReader.readHeaders()) {
                headers = cvsReader.getHeaders();
            }
            while (cvsReader.readRecord()) {
                int nColumnCount = cvsReader.getColumnCount();
                if (nColumnCount != 4) {
                    Object[] args = { 1, cvsReader.getValues()[0] };
                    vecError.add(MessageFormat.format(bundle.getString("user.uploadFile.errorParameter"), args));
                    serviceResult.setObjResult(vecError);
                    serviceResult.setError(true);
                    return serviceResult;
                }
                String sResult = validateLine(cvsReader, headers, nCount);
                if (sResult != null) {
                    System.out.println("ERROR => " + sResult);
                    vecError.add((nCount), sResult);
                    serviceResult.setError(true);
                    serviceResult.setMessage(sResult);
                } else {
                    vecSuccess.add(cvsReader.getRawRecord());
                    vecError.add((nCount), bundle.getString("user.uploadFile.validateSuccess"));
                }
                nCount++;
            }
            nCount = 0;
            if (serviceResult.isError()) {
                serviceResult.setObjResult(vecError);
                log.info("Registros error: " + vecError.size());
                serviceResult.setMessage(bundle.getString("user.update.failureUpload"));
            } else {
                tableStudentForCourse = new HashMap<Long, String>();
                for (String sLineSuccess : vecSuccess) {
                    log.info("sLineSuccess => " + sLineSuccess);
                    StringTokenizer tokenizer = new StringTokenizer(sLineSuccess, ";");
                    Long nCourseId = new Long(tokenizer.nextToken());
                    Long nCodStudent = new Long(tokenizer.nextToken());
                    String sUserName = tokenizer.nextToken();
                    String sMail = tokenizer.nextToken();
                    CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
                    MaUser maUser = null;
                    List<MaUser> list = new MaUserDAO().findByCodStudent(nCodStudent);
                    if (list.size() == 0) {
                        maUser = new MaUser();
                        maUser.setUserName(sUserName);
                        maUser.setCodStudent(nCodStudent);
                        maUser.setPassword(nCodStudent.toString());
                        maUser.setEmail(sMail);
                        maUser.setMaRole(new MaRoleDAO().findById(new Long(Common.ROLE_ID_STUDENT)));
                        DataManagerUser dataManagerUser = new DataManagerUser();
                        dataManagerUser.setBundle(bundle);
                        serviceResult = dataManagerUser.create(serviceResult, maUser);
                        if (serviceResult.isError()) return serviceResult;
                        Object[] args = { (nCount + 1), nCodStudent };
                        vecWarning.add(MessageFormat.format(bundle.getString("user.uploadFile.warningCodStudentCreateReg"), args));
                    } else {
                        maUser = list.get(0);
                        vecWarning.add(bundle.getString("user.uploadFile.success"));
                    }
                    if (!tableStudentForCourse.containsKey(coCourse.getCourseId())) {
                        tableStudentForCourse.put(coCourse.getCourseId(), new String());
                    }
                    sArrayStudent = tableStudentForCourse.get(coCourse.getCourseId());
                    sArrayStudent += maUser.getUserId() + ",";
                    tableStudentForCourse.put(coCourse.getCourseId(), sArrayStudent);
                    nCount++;
                }
                for (Iterator iterator = tableStudentForCourse.keySet().iterator(); iterator.hasNext(); ) {
                    Long nCourseId = (Long) iterator.next();
                    sArrayStudent = tableStudentForCourse.get(nCourseId);
                    sArrayStudent = sArrayStudent.substring(0, sArrayStudent.length() - 1);
                    serviceResult = addStudent(serviceResult, nCourseId, sArrayStudent, true);
                    if (serviceResult.isError()) return serviceResult;
                }
                serviceResult.setObjResult(vecWarning);
                Object[] args = { nCount };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.update.successUpload"), args));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fr) fr.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return serviceResult;
    }

    /** M�todo que permite guardar usuarios a partir de un archivos CSV
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param archivo
	 *            a importar
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult removeStudentMasive(RestServiceResult serviceResult, File fileImport) {
        FileReader fr = null;
        String sArrayStudent = null;
        HashMap<Long, String> tableStudentForCourse = null;
        CsvReader cvsReader = null;
        FileReader freader = null;
        try {
            freader = new FileReader(fileImport);
            cvsReader = new CsvReader(freader, ";".charAt(0));
            String[] headers = null;
            int nCount = 0;
            Vector<String> vecError = new Vector<String>();
            Vector<String> vecSuccess = new Vector<String>();
            Vector<String> vecWarning = new Vector<String>();
            if (cvsReader.readHeaders()) {
                headers = cvsReader.getHeaders();
            }
            while (cvsReader.readRecord()) {
                int nColumnCount = cvsReader.getColumnCount();
                if (nColumnCount != 2) {
                    Object[] args = { 1, cvsReader.getValues()[0] };
                    vecError.add(MessageFormat.format(bundle.getString("user.uploadFile.errorParameter"), args));
                    serviceResult.setObjResult(vecError);
                    serviceResult.setError(true);
                    return serviceResult;
                }
                String sResult = validateLineRemoveStudent(cvsReader, headers, nCount);
                if (sResult != null) {
                    System.out.println("ERROR => " + sResult);
                    vecError.add((nCount), sResult);
                    serviceResult.setError(true);
                    serviceResult.setMessage(sResult);
                } else {
                    vecSuccess.add(cvsReader.getRawRecord());
                    vecError.add(bundle.getString("user.uploadFile.validateSuccess"));
                }
                nCount++;
            }
            nCount = 0;
            if (serviceResult.isError()) {
                serviceResult.setObjResult(vecError);
                log.info("Registros error: " + vecError.size());
                serviceResult.setMessage(bundle.getString("user.update.failureUpload"));
            } else {
                tableStudentForCourse = new HashMap<Long, String>();
                for (String sLineSuccess : vecSuccess) {
                    log.info("sLineSuccess => " + sLineSuccess);
                    StringTokenizer tokenizer = new StringTokenizer(sLineSuccess, ";");
                    Long nCourseId = new Long(tokenizer.nextToken());
                    Long nCodStudent = new Long(tokenizer.nextToken());
                    CoCourse coCourse = new CoCourseDAO().findById(nCourseId);
                    MaUser maUser = null;
                    List<MaUser> list = new MaUserDAO().findByCodStudent(nCodStudent);
                    if (list.size() == 0) {
                        Object[] args = { (nCount + 1), nCodStudent };
                        vecWarning.add(MessageFormat.format(bundle.getString("user.uploadFile.warningRemoveStudentNotExist"), args));
                    } else {
                        maUser = list.get(0);
                    }
                    if (!tableStudentForCourse.containsKey(coCourse.getCourseId())) {
                        tableStudentForCourse.put(coCourse.getCourseId(), new String());
                    }
                    sArrayStudent = tableStudentForCourse.get(coCourse.getCourseId());
                    sArrayStudent += maUser.getUserId() + ",";
                    tableStudentForCourse.put(coCourse.getCourseId(), sArrayStudent);
                    nCount++;
                }
                for (Iterator iterator = tableStudentForCourse.keySet().iterator(); iterator.hasNext(); ) {
                    Long nCourseId = (Long) iterator.next();
                    sArrayStudent = tableStudentForCourse.get(nCourseId);
                    sArrayStudent = sArrayStudent.substring(0, sArrayStudent.length() - 1);
                    serviceResult = removeStudent(serviceResult, nCourseId, sArrayStudent);
                    if (serviceResult.isError()) {
                        return serviceResult;
                    } else {
                        vecWarning.add(bundle.getString("user.uploadFile.successRemove"));
                    }
                }
                serviceResult.setObjResult(vecWarning);
                Object[] args = { nCount };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("user.update.successUpload"), args));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fr) fr.close();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return serviceResult;
    }

    /**
	 * Realiza la validaci�n de una l�nea proveniente de un archivo CSV con datos de usuarios. Par�metros: Identificaci�n,C�digo,Nombre,Role (Si no tiene c�digo � Identificaci�n se agrega cero ( 0 ))
	 * 
	 * @param sLine
	 *            l�nea con 4 p�ramtros separados por comas
	 * @return {@link String } con el mensaje de error � null si no encontr� errores
	 */
    private String validateLine(CsvReader cvsReader, String[] headers, int nNumLine) throws IOException {
        String sCodStudent = null;
        String sCourseId = null;
        String sStudentName = null;
        String sMail = null;
        String sLine = cvsReader.getRawRecord();
        Object[] args = { nNumLine, sLine };
        sCourseId = cvsReader.get(headers[0]);
        if (!Util.validateNumeric(log, sCourseId)) {
            return MessageFormat.format(bundle.getString("user.uploadFile.errorCourseId"), args);
        }
        CoCourse coCourse = new CoCourseDAO().findById(new Long(sCourseId));
        if (coCourse == null) {
            Object[] arrayParam = { nNumLine, "", sCourseId };
            return MessageFormat.format(bundle.getString("user.uploadFile.errorNotExistCourse"), arrayParam);
        }
        sCodStudent = cvsReader.get(headers[1]);
        if (!Util.validateStringNumeric(log, sCodStudent)) return MessageFormat.format(bundle.getString("user.uploadFile.errorCodStudent"), args);
        sStudentName = cvsReader.get(headers[2]);
        if (sStudentName == null || sStudentName.trim().equals("") || sStudentName.trim().length() <= 4) return MessageFormat.format(bundle.getString("user.uploadFile.errorName"), args);
        sMail = cvsReader.get(headers[3]);
        if (sMail == null || sMail.trim().equals("")) return MessageFormat.format(bundle.getString("user.uploadFile.errorMail"), args);
        return null;
    }

    /**
	 * Realiza la validaci�n de una l�nea proveniente de un archivo CSV con datos de usuarios. Par�metros: Identificaci�n,C�digo,Nombre,Role (Si no tiene c�digo � Identificaci�n se agrega cero ( 0 ))
	 * 
	 * @param sLine
	 *            l�nea con 4 p�ramtros separados por comas
	 * @return {@link String } con el mensaje de error � null si no encontr� errores
	 */
    private String validateLineRemoveStudent(CsvReader cvsReader, String[] headers, int nNumLine) throws IOException {
        String sCodStudent = null;
        String sCourseId = null;
        String sLine = cvsReader.getRawRecord();
        Object[] args = { nNumLine, sLine };
        sCourseId = cvsReader.get(headers[0]);
        if (!Util.validateNumeric(log, sCourseId)) {
            return MessageFormat.format(bundle.getString("user.uploadFile.errorCourseId"), args);
        }
        CoCourse coCourse = new CoCourseDAO().findById(new Long(sCourseId));
        if (coCourse == null) {
            Object[] arrayParam = { nNumLine, "", sCourseId };
            return MessageFormat.format(bundle.getString("user.uploadFile.errorNotExistCourse"), arrayParam);
        }
        sCodStudent = cvsReader.get(headers[1]);
        if (!Util.validateStringNumeric(log, sCodStudent)) return MessageFormat.format(bundle.getString("user.uploadFile.errorCodStudent"), args);
        Long nCodStudent = new Long(sCodStudent);
        List<MaUser> list = new MaUserDAO().findByCodStudent(nCodStudent);
        if (list.size() == 0) {
            Object[] arrayParam = { nNumLine, nCodStudent };
            return MessageFormat.format(bundle.getString("user.uploadFile.warningRemoveStudentNotExist"), arrayParam);
        }
        return null;
    }

    /**
	 * M�todo que permite la clonaci�n de un curso
	 * 
	 * @param nCourseOldId
	 *            C�digo del curso a clonar
	 * @param nCourseNewId
	 *            C�digo del curso nuevo
	 */
    public void cloneCourse(Long nCourseOldId, Long nCourseNewId, MaUser maUser) {
        try {
            Query query;
            CoCourse coCourse = new CoCourseDAO().findById(nCourseOldId);
            EntityManagerHelper.refresh(coCourse);
            EntityManagerHelper.beginTransaction();
            cloneUserHistory(nCourseOldId, nCourseNewId);
            EntityManagerHelper.commit();
            if (coCourse.getCoActivities().size() > 0) {
                Set<CoActivity> setActivity = coCourse.getCoActivities();
                for (CoActivity coActivity : setActivity) {
                    Long nActivityNewId = getSequence("sq_co_activity");
                    log.info("Clonando Curso Solo actividades. Numero: " + coCourse.getCourseId());
                    EntityManagerHelper.beginTransaction();
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_ACTIVITY_FOR_COURSE.replaceAll(":CLONE", bundle.getString("course.create.clone")));
                    query.setParameter(1, nActivityNewId);
                    query.setParameter(2, nCourseNewId);
                    query.setParameter(3, coActivity.getActivityId());
                    query.executeUpdate();
                    EntityManagerHelper.commit();
                    log.info("OK...");
                    CoActivity coActivityNew = new CoActivityDAO().findById(nActivityNewId);
                    EntityManagerHelper.refresh(coActivityNew);
                    if (coActivityNew != null) {
                        EntityManagerHelper.beginTransaction();
                        DataManagerActivity.addUserHistory(new RestServiceResult(), maUser, coActivityNew);
                        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_ACTIVITY_MATERIAL);
                        query.setParameter(1, coActivityNew.getActivityId());
                        query.setParameter(2, coActivityNew.getActivityParentId());
                        query.executeUpdate();
                        EntityManagerHelper.commit();
                        new DataManagerActivity().cloneActivity(coActivityNew.getActivityParentId(), coActivityNew.getActivityId());
                    }
                }
            } else {
                Set<CoUnit> setUnit = coCourse.getCoUnits();
                for (CoUnit coUnit : setUnit) {
                    EntityManagerHelper.beginTransaction();
                    Long nUnitNewId = getSequence("sq_co_unit");
                    query = EntityManagerHelper.createNativeQuery(Statements.CLONE_UNIT.replaceAll(":CLONE", bundle.getString("course.create.clone")));
                    query.setParameter(1, nUnitNewId);
                    query.setParameter(2, nCourseNewId);
                    query.setParameter(3, coUnit.getUnitId());
                    query.executeUpdate();
                    EntityManagerHelper.commit();
                    CoUnit coUnitNew = new CoUnitDAO().findById(nUnitNewId);
                    EntityManagerHelper.refresh(coUnitNew);
                    if (coUnitNew != null) {
                        EntityManagerHelper.beginTransaction();
                        DataManagerUnit.addUserHistory(new RestServiceResult(), maUser, coUnitNew);
                        query = EntityManagerHelper.createNativeQuery(Statements.CLONE_UNIT_MATERIAL);
                        query.setParameter(1, nUnitNewId);
                        query.setParameter(2, coUnit.getUnitId());
                        query.executeUpdate();
                        EntityManagerHelper.commit();
                        DataManagerUnit dataManagerUnit = new DataManagerUnit();
                        dataManagerUnit.setBundle(bundle);
                        dataManagerUnit.cloneUnit(coUnit.getUnitId(), nUnitNewId, maUser);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            EntityManagerHelper.rollback();
        }
    }

    /**
	 * Permite clonar el historial de usuario de una curso clonado
	 * 
	 * @param nCourseOldId
	 *            C�digo de la actividad a clonar
	 * @param coCourseNew
	 *            Actividad Creada a partir de clonaci�n
	 */
    private void cloneUserHistory(Long nCourseOldId, Long nCourseNewId) {
        try {
            log.info("CLONACION HISTORIAL DE CURSOS X USUARIO");
            CoCourse coCourse = new CoCourseDAO().findById(nCourseOldId);
            EntityManagerHelper.refresh(coCourse);
            Set<CoCourseUserHistory> setUserHistoryOld = coCourse.getCoCourseUserHistories();
            CoCourse coCourseNew = new CoCourseDAO().findById(nCourseNewId);
            for (CoCourseUserHistory coCourseUserHistory : setUserHistoryOld) {
                MaUser maUser = coCourseUserHistory.getMaUser();
                CoCourseUserHistory courseUserHistory = new CoCourseUserHistoryDAO().findById(new CoCourseUserHistoryId(coCourseNew.getCourseId(), maUser.getUserId()));
                log.info("Resultado de la consulta=> " + courseUserHistory);
                if (courseUserHistory == null) {
                    log.info("Agregando HistorialxCurso: Usuario '" + maUser.getUserId() + "' - Curso '" + coCourseNew.getCourseId() + "'");
                    addUserHistory(maUser, coCourseNew);
                } else {
                    log.info("El usuario '" + maUser.getUserId() + "' tiene el curso '" + coCourseNew.getCourseId() + "'");
                }
            }
            log.info("Ok...Termina clonaci�n de Historial de usuarios en CURSOS");
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Obtiene el arbol XML de cursos por Estudiante
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult getTreeCourseMasterStudent(RestServiceResult serviceResult, Long nStudentId) {
        MaUser maUser = new MaUserDAO().findById(nStudentId);
        Long nRoleId = maUser.getMaRole().getRoleId();
        Long nUserId = maUser.getUserId();
        serviceResult.setRoleUser(nRoleId);
        serviceResult.setId(nUserId);
        List<CoCourse> listCourse = null;
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.list.notFound"));
        } else {
            EntityManagerHelper.refresh(maUser);
            listCourse = new ArrayList<CoCourse>();
            List<CoCourseUser> list = new ArrayList<CoCourseUser>(maUser.getCoCourseUsers());
            for (int i = 0; i < list.size(); i++) {
                CoCourseUser coCourseUser = (CoCourseUser) list.get(i);
                EntityManagerHelper.refresh(coCourseUser);
                listCourse.add(coCourseUser.getCoCourse());
            }
            if (listCourse.size() == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("course.list.notFound"));
            } else {
                Object[] arrayParam = { listCourse.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("course.list.success"), arrayParam));
                serviceResult.setObjResult(listCourse);
                log.info("cargo la lista");
                serviceResult.setNumResult(listCourse.size());
            }
        }
        return serviceResult;
    }

    /**
	 * Obtiene un curso especifico del arbol del estudiante
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    @SuppressWarnings("unchecked")
    public RestServiceResult getCourseMasterStudent(RestServiceResult serviceResult, Long nStudentId, Long nCourseId) {
        MaUser maUser = new MaUserDAO().findById(nStudentId);
        EntityManagerHelper.refresh(maUser);
        Long nRoleId = maUser.getMaRole().getRoleId();
        Long nUserId = maUser.getUserId();
        serviceResult.setRoleUser(nRoleId);
        serviceResult.setId(nUserId);
        List<CoCourseUser> listCourse = null;
        if (maUser == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("course.list.notFound"));
        } else {
            listCourse = new ArrayList<CoCourseUser>();
            List<CoCourseUser> list = new ArrayList<CoCourseUser>(maUser.getCoCourseUsers());
            for (int i = 0; i < list.size(); i++) {
                CoCourseUser coCourse = (CoCourseUser) list.get(i);
                EntityManagerHelper.refresh(coCourse);
                Long cId = coCourse.getCoCourse().getCourseId();
                if (cId.equals(nCourseId)) {
                    listCourse.add(coCourse);
                }
            }
            if (listCourse.size() == 0) {
                serviceResult.setError(true);
                serviceResult.setMessage(bundle.getString("course.list.notFound"));
            } else {
                Object[] arrayParam = { listCourse.size() };
                serviceResult.setMessage(MessageFormat.format(bundle.getString("course.list.success"), arrayParam));
                serviceResult.setObjResult(listCourse);
                serviceResult.setNumResult(listCourse.size());
            }
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de agregar un usuario al historial de modificaciones
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.s
	 */
    public RestServiceResult addUserHistory(RestServiceResult serviceResult, MaUser maUser, CoCourse coCourse) {
        log.info("HISTORIAL DE CURSOS X USUARIO");
        CoCourseUserHistory activityUserHistory = new CoCourseUserHistoryDAO().findById(new CoCourseUserHistoryId(coCourse.getCourseId(), maUser.getUserId()));
        log.info("Resultado de la consulta => " + activityUserHistory);
        if (activityUserHistory == null) {
            log.info("Agregando HistorialxCurso: Usuario '" + maUser.getUserId() + "' - Curso '" + coCourse.getCourseId() + "'");
            addUserHistory(maUser, coCourse);
        } else {
            log.info("El usuario '" + maUser.getUserId() + "' tiene el curso '" + coCourse.getCourseId() + "'");
        }
        log.info("Termina HISTORIAL DE CURSOS...");
        return serviceResult;
    }

    /**
	 * Registra en el historial de actividades un usuario
	 * 
	 * @param maUser
	 *            usuario a registrar
	 * @param coCourse
	 *            actividad a guardar
	 */
    private void addUserHistory(MaUser maUser, CoCourse coCourse) {
        try {
            CoCourseUserHistoryId userHistoryId = new CoCourseUserHistoryId();
            userHistoryId.setCourseId(coCourse.getCourseId());
            userHistoryId.setUserId(maUser.getUserId());
            new CoCourseUserHistoryDAO().save(new CoCourseUserHistory(userHistoryId, coCourse, maUser, new Date()));
            log.info("Se guardo el usuario " + maUser.getUserId() + " con �xito al historial del curso '" + coCourse.getCourseId() + "'");
        } catch (PersistenceException e) {
            log.info("El usuario " + maUser.getUserName() + " ya esta en el  historial del curso '" + coCourse.getCourseId() + "' " + "OMITIR EXCEPCION PRIMARY KEY");
        }
        return;
    }

    /**
	 * Realiza el proceso de agregar un usuario al historial de modificaciones
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.s
	 */
    public RestServiceResult listCourseForUser(RestServiceResult serviceResult, Long nUserId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        Set<CoCourse> set = new HashSet<CoCourse>();
        List<CoCourse> listCourse = new ArrayList<CoCourse>();
        if (maUser.getMaRole().getRoleId().equals(Common.ROLE_ID_STUDENT)) {
            List<CoCourseUser> list = new ArrayList<CoCourseUser>(maUser.getCoCourseUsers());
            for (CoCourseUser coCourseUser : list) {
                set.add(coCourseUser.getCoCourse());
            }
        } else if (maUser.getMaRole().getRoleId().equals(Common.ROLE_ID_TEACHER)) {
            set = maUser.getCoCoursesForUserId();
        }
        if (set.size() == 0) {
            serviceResult.setMessage(bundle.getString("course.list.notFound"));
        } else {
            listCourse.addAll(set);
            Object[] array = { listCourse.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("course.list.success"), array));
        }
        serviceResult.setObjResult(listCourse);
        return serviceResult;
    }

    /**
	 * Realiza el proceso de actualizar el cronograma y programa.
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes localizados y estado SQL .
	 * @param coMaterial
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la operaci�n.
	 */
    public RestServiceResult updateCourseMaterial(RestServiceResult serviceResult, String sProgramMaterialId, String sTimeLineMaterialId, CoCourse coCourse) {
        if (Util.validateStringNumeric(log, sProgramMaterialId)) {
            CoMaterial coMaterial = new CoMaterialDAO().findById(new Long(sProgramMaterialId));
            coCourse.setCoMaterialByProgramMaterialId(coMaterial);
            update(serviceResult, coCourse);
        }
        if (Util.validateStringNumeric(log, sTimeLineMaterialId)) {
            CoMaterial coMaterial = new CoMaterialDAO().findById(new Long(sTimeLineMaterialId));
            coCourse.setCoMaterialByTimelineMaterialId(coMaterial);
            update(serviceResult, coCourse);
        }
        return serviceResult;
    }

    public static void main(String[] args) {
        Long nCourseId = new Long(218);
        String sArrayStudent = "873";
        File fileImport = new File("C:/Users/Jose/Documents/a/Registrar_estudiantes.csv");
        fileImport = new File("C:/Users/Jose/Documents/a/Eliminar_estudiantes.csv");
        RestServiceResult serviceResult = new RestServiceResult();
        DataManagerCourse dataManagerCourse = new DataManagerCourse();
        dataManagerCourse.setBundle(ResourceBundle.getBundle("edu.univalle.lingweb.LzTrackMessages", new Locale("es")));
        dataManagerCourse.removeStudentMasive(serviceResult, fileImport);
        System.out.println("result => " + serviceResult.getMessage());
        if (true) return;
        dataManagerCourse.addStudent(serviceResult, nCourseId, sArrayStudent, false);
        System.out.println("result => " + serviceResult.getMessage());
    }
}
