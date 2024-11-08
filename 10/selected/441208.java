package es.wtestgen.services;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Restrictions;
import es.wtestgen.bean.profesor.ExamenForm;
import es.wtestgen.domain.Alumno;
import es.wtestgen.domain.Asignatura;
import es.wtestgen.domain.Examen;
import es.wtestgen.domain.Pregunta;
import es.wtestgen.domain.Respuesta;
import es.wtestgen.util.HibernateUtil;

public class ExamenDAO {

    private static final Log log = LogFactory.getLog(ExamenDAO.class);

    public ExamenDAO() {
    }

    public boolean eliminar(int codExam) {
        Transaction tx = null;
        Session session = HibernateUtil.currentSession();
        boolean eliminado = false;
        try {
            tx = session.beginTransaction();
            Examen examen = (Examen) session.get(Examen.class, codExam);
            Asignatura asignatura = (Asignatura) session.get(Asignatura.class, examen.getAsignatura().getCodAsig());
            asignatura.getExamenes().remove(examen);
            session.delete(examen);
            tx.commit();
            eliminado = true;
        } catch (Exception e) {
            try {
                tx.rollback();
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } finally {
            HibernateUtil.closeSession();
        }
        return eliminado;
    }

    public List findAll() {
        List lista = new ArrayList();
        Criteria criteria = null;
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Examen.class);
            lista = criteria.list();
        } catch (HibernateException e) {
            log.error("_____Error al obtener la todos los registros de la clase Examen", e);
            throw new HibernateException(e);
        }
        return lista;
    }

    public List findById(String cod) {
        List lista = new ArrayList();
        Criteria criteria = null;
        int id = Integer.parseInt(cod);
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Examen.class);
            criteria.add(Restrictions.eq("codExam", id));
            lista = criteria.list();
        } catch (HibernateException e) {
            log.error("_____Error al obtener el registro de la clase Examen con id: " + cod, e);
            throw new HibernateException(e);
        }
        return lista;
    }

    public List findByAsignatura(Asignatura asignatura) {
        List lista = new ArrayList();
        Criteria criteria = null;
        try {
            Session session = HibernateUtil.currentSession();
            Asignatura asig = (Asignatura) session.get(Asignatura.class, asignatura.getCodAsig());
            criteria = session.createCriteria(Examen.class);
            criteria.add(Restrictions.eq("asignatura", asig));
            lista = criteria.list();
        } catch (HibernateException e) {
            log.error("_____Error al obtener los registros de la clase Examen para la asignatura: " + asignatura.getNombreAsig(), e);
            throw new HibernateException(e);
        }
        return lista;
    }

    public boolean guardarExamenAutomaticoEnAsignatura(Examen exam, int codAsig) {
        Transaction tx = null;
        Session session = HibernateUtil.currentSession();
        boolean guardado = false;
        PreguntaDAO preguntaDao = new PreguntaDAO();
        Set<Pregunta> preguntas = new HashSet<Pregunta>(exam.getNumPreg());
        RespuestaDAO respuestaDao = new RespuestaDAO();
        Set<Respuesta> respuestas = new HashSet<Respuesta>(exam.getNumResp());
        List respuestasPreguntas = new ArrayList(exam.getNumPreg());
        try {
            tx = session.beginTransaction();
            session.clear();
            Asignatura asignatura = (Asignatura) session.get(Asignatura.class, codAsig);
            for (int i = 0; i < exam.getNumPreg(); i++) {
                int codPreg = 0;
                Pregunta pregunta = (Pregunta) session.get(Pregunta.class, codPreg);
                preguntas.add(pregunta);
                pregunta.getExamenes().add(exam);
                respuestas = findRespuestasDePregunta(pregunta, exam.getNumResp());
                pregunta.setRespuestasPreguntaExamen(respuestas);
            }
            exam.setPreguntas(preguntas);
            session.saveOrUpdate(asignatura);
            tx.commit();
            guardado = true;
        } catch (Exception e) {
            try {
                tx.rollback();
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } finally {
            HibernateUtil.closeSession();
        }
        return guardado;
    }

    private Set<Respuesta> findRespuestasDePregunta(Pregunta pregunta, int numResp) {
        List lista = new ArrayList();
        Set listaRespuestas = new HashSet(numResp);
        Criteria criteria = null;
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Respuesta.class);
            criteria.add(Restrictions.eq("pregunta", pregunta.getCodPreg()));
            lista = criteria.list();
            if (numResp > lista.size()) {
                numResp = lista.size();
            }
            int i = 0;
            for (Iterator iterator = lista.iterator(); iterator.hasNext(); ) {
                Respuesta respuesta = (Respuesta) iterator.next();
                if (i <= numResp) {
                    listaRespuestas.add(respuesta);
                    i++;
                } else {
                    break;
                }
            }
        } catch (HibernateException e) {
            log.error("_____Error al obtener las respuestas para la pregunta con id: " + pregunta.getCodPreg(), e);
            throw new HibernateException(e);
        }
        return listaRespuestas;
    }

    public boolean guardarExamenManualEnAsignatura(Examen exam, int codAsig) {
        Transaction tx = null;
        Session session = HibernateUtil.currentSession();
        boolean guardado = false;
        try {
            tx = session.beginTransaction();
            session.clear();
            Asignatura asignatura = (Asignatura) session.get(Asignatura.class, codAsig);
            if (exam.getCodExam() != -1) {
                Examen examen = (Examen) session.get(Examen.class, exam.getCodExam());
                examen.setAsignatura(asignatura);
                examen.setFechaExam(exam.getFechaExam());
                examen.setPublicado(exam.isPublicado());
                session.saveOrUpdate(examen);
                asignatura.getExamenes().add(examen);
            } else {
                exam.setAsignatura(asignatura);
                session.saveOrUpdate(exam);
                asignatura.getExamenes().add(exam);
            }
            session.saveOrUpdate(asignatura);
            tx.commit();
            guardado = true;
        } catch (Exception e) {
            try {
                tx.rollback();
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } finally {
            HibernateUtil.closeSession();
        }
        return guardado;
    }

    public boolean guardarRespuestasDePreguntaDeExamen(int codExam, int codPreg, int[] respuestas) {
        Transaction tx = null;
        Session session = HibernateUtil.currentSession();
        SQLQuery query = null;
        boolean guardado = false;
        try {
            tx = session.beginTransaction();
            session.clear();
            Examen examen = (Examen) session.get(Examen.class, codExam);
            Pregunta pregunta = (Pregunta) session.get(Pregunta.class, codPreg);
            for (int i = 0; i < respuestas.length; i++) {
                Respuesta resp = (Respuesta) session.get(Respuesta.class, respuestas[i]);
                query = session.createSQLQuery("INSERT INTO respuestas_pregunta_examen (codExam,codPreg,codResp) VALUES (?,?,?)");
                query.setInteger(0, codExam);
                query.setInteger(1, codPreg);
                query.setInteger(2, resp.getCodResp());
                if (query.executeUpdate() <= 0) {
                    break;
                }
            }
            tx.commit();
            guardado = true;
        } catch (Exception e) {
            try {
                tx.rollback();
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } finally {
            HibernateUtil.closeSession();
        }
        return guardado;
    }

    public boolean guardarExamen(Examen examen) {
        Transaction tx = null;
        Session session = HibernateUtil.currentSession();
        boolean guardado = false;
        try {
            tx = session.beginTransaction();
            session.clear();
            Asignatura asignatura = (Asignatura) session.get(Asignatura.class, examen.getAsignatura().getCodAsig());
            if (examen.getCodExam() != -1) {
                Examen exam = (Examen) session.get(Examen.class, examen.getCodExam());
                exam.setAsignatura(asignatura);
                exam.setNumPreg(examen.getNumPreg());
                exam.setNumResp(examen.getNumResp());
                exam.setFechaExam(examen.getFechaExam());
                exam.setPublicado(examen.isPublicado());
                session.saveOrUpdate(exam);
                asignatura.getExamenes().add(exam);
            } else {
                session.saveOrUpdate(examen);
                asignatura.getExamenes().add(examen);
            }
            session.saveOrUpdate(asignatura);
            tx.commit();
            guardado = true;
        } catch (Exception e) {
            try {
                tx.rollback();
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } finally {
            HibernateUtil.closeSession();
        }
        return guardado;
    }

    public boolean guardarExamenAutomaticoEnAsignatura(Examen examen) {
        Transaction tx = null;
        Session session = HibernateUtil.currentSession();
        boolean guardado = false;
        try {
            tx = session.beginTransaction();
            session.clear();
            Asignatura asignatura = (Asignatura) session.get(Asignatura.class, examen.getAsignatura().getCodAsig());
            Set<Pregunta> preguntas = (Set) examen.getPreguntas();
            Set<Respuesta> respuestas = (Set) examen.getRespuestas();
            if (examen.getCodExam() != -1 && examen.getCodExam() != 0) {
            } else {
                session.save(examen);
                asignatura.getExamenes().add(examen);
                session.save(asignatura);
                tx.commit();
                for (Iterator iterator = preguntas.iterator(); iterator.hasNext(); ) {
                    Pregunta pregunta = (Pregunta) iterator.next();
                    Pregunta preg = (Pregunta) session.get(Pregunta.class, pregunta.getCodPreg());
                    preg.getExamenes().add(examen);
                    Session session2 = HibernateUtil.currentSession();
                    Transaction tx2 = null;
                    try {
                        tx2 = session2.beginTransaction();
                        session2.update(preg);
                        tx2.commit();
                    } catch (Exception e) {
                        try {
                            tx2.rollback();
                            e.printStackTrace();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
                for (Iterator iterator = respuestas.iterator(); iterator.hasNext(); ) {
                    Respuesta respuesta = (Respuesta) iterator.next();
                    Respuesta resp = (Respuesta) session.get(Respuesta.class, respuesta.getCodResp());
                    resp.getExamenes().add(examen);
                    Session session3 = HibernateUtil.currentSession();
                    Transaction tx3 = null;
                    try {
                        tx3 = session3.beginTransaction();
                        session3.update(resp);
                        tx3.commit();
                    } catch (Exception e) {
                        try {
                            tx3.rollback();
                            e.printStackTrace();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
            session.clear();
            guardado = true;
        } catch (Exception e) {
            try {
                tx.rollback();
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        } finally {
            HibernateUtil.closeSession();
        }
        return guardado;
    }

    public List getPreguntas(Examen examen) {
        List lista = new ArrayList();
        List listaPreguntas = new ArrayList();
        Criteria criteria = null;
        SQLQuery query = null;
        PreguntaDAO preguntaDao = new PreguntaDAO();
        Pregunta pregunta = new Pregunta();
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Pregunta.class);
            query = session.createSQLQuery("SELECT codPreg FROM examen_tiene_pregunta WHERE codExam = ?");
            query.setInteger(0, examen.getCodExam());
            lista = query.list();
            for (Iterator iterator = lista.iterator(); iterator.hasNext(); ) {
                int codPreg = (Integer) iterator.next();
                pregunta = (Pregunta) preguntaDao.findById(String.valueOf(codPreg)).get(0);
                listaPreguntas.add(pregunta);
            }
        } catch (HibernateException e) {
            log.error("_____Error al obtener las preguntas para el examen con id: " + examen.getCodExam(), e);
            throw new HibernateException(e);
        }
        return listaPreguntas;
    }

    public List getRespuestas(Examen examen) {
        List lista = new ArrayList();
        List listaRespuestas = new ArrayList();
        Criteria criteria = null;
        SQLQuery query = null;
        RespuestaDAO respuestaDao = new RespuestaDAO();
        Respuesta respuesta = new Respuesta();
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Pregunta.class);
            query = session.createSQLQuery("SELECT codResp FROM examen_tiene_respuesta WHERE codExam = ?");
            query.setInteger(0, examen.getCodExam());
            lista = query.list();
            for (Iterator iterator = lista.iterator(); iterator.hasNext(); ) {
                int codResp = (Integer) iterator.next();
                respuesta = (Respuesta) respuestaDao.findById(String.valueOf(codResp)).get(0);
                listaRespuestas.add(respuesta);
            }
        } catch (HibernateException e) {
            log.error("_____Error al obtener las respuestas para el examen con id: " + examen.getCodExam(), e);
            throw new HibernateException(e);
        }
        return listaRespuestas;
    }

    public Asignatura getAsignatura(String codExam) {
        Asignatura asignatura = new Asignatura();
        Criteria criteria = null;
        SQLQuery query = null;
        int id = Integer.parseInt(codExam);
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Asignatura.class);
            query = session.createSQLQuery("SELECT asignatura FROM examen WHERE codExam = ?");
            query.setInteger(0, id);
            int codAsig = (Integer) query.list().get(0);
            criteria.add(Restrictions.eq("codAsig", codAsig));
            asignatura = (Asignatura) criteria.list().get(0);
        } catch (HibernateException e) {
            log.error("_____Error al obtener la asignatura para el examen con id: " + codExam, e);
            throw new HibernateException(e);
        }
        return asignatura;
    }

    public List findByParameters(ExamenForm examenForm) {
        List lista = new ArrayList();
        Criteria criteria = null;
        try {
            Session session = HibernateUtil.currentSession();
            criteria = session.createCriteria(Examen.class);
            if (examenForm.getFechaExam() != null && !"".equals(examenForm.getFechaExam())) {
                criteria.add(Restrictions.like("fechaExam", "%" + examenForm.getFechaExam() + "%"));
            }
            if (examenForm.getCodAsig() != 0) {
                AsignaturaDAO asignaturaDao = new AsignaturaDAO();
                Asignatura asignatura = (Asignatura) asignaturaDao.findById(String.valueOf(examenForm.getCodAsig())).get(0);
                criteria.add(Restrictions.eq("asignatura", asignatura));
            }
            if (examenForm.getNumPreg() != 0) {
                criteria.add(Restrictions.eq("numPreg", examenForm.getNumPreg()));
            }
            if (examenForm.getNumResp() != 0) {
                criteria.add(Restrictions.eq("numResp", examenForm.getNumResp()));
            }
            if (examenForm.getDificultadExamen() != null && !"".equals(examenForm.getDificultadExamen())) {
                criteria.add(Restrictions.like("dificultadExamen", "%" + examenForm.getDificultadExamen() + "%"));
            }
            criteria.add(Restrictions.eq("publicado", examenForm.isPublicado()));
            lista = criteria.list();
        } catch (HibernateException e) {
            log.error("_____Error al obtener los registros de la clase Examen para los parametros de busquedas.", e);
            throw new HibernateException(e);
        }
        return lista;
    }
}
