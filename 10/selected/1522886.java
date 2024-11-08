package edu.univalle.lingweb.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.agent.model.Announcement;
import edu.univalle.lingweb.persistence.AgAnnouncement;
import edu.univalle.lingweb.persistence.AgAnnouncementDAO;
import edu.univalle.lingweb.persistence.AgAnnouncementType;
import edu.univalle.lingweb.persistence.AgAnnouncementTypeDAO;
import edu.univalle.lingweb.persistence.CoActivity;
import edu.univalle.lingweb.persistence.CoActivityDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'ag_announcement'( Anuncios del sistema )
 * 
 * @author Julio Cesar Puentes Delgado
 */
public class DataManagerAgAnnouncement extends DataManager {

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerAgAnnouncement.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerAgAnnouncement() {
        super();
        DOMConfigurator.configure(DataManagerAgAnnouncement.class.getResource("/log4j.xml"));
    }

    /**
	 * Crea una nuevo anuncio para el sistema
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * @param coActivity
	 *            Actividad para el que se desea crear el estado
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult result, AgAnnouncement agAnnouncement) {
        AgAnnouncementDAO agAnnouncementDAO = new AgAnnouncementDAO();
        try {
            agAnnouncement.setAnnouncementId(getSequence("sq_ag_announcement"));
            EntityManagerHelper.beginTransaction();
            agAnnouncementDAO.save(agAnnouncement);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(agAnnouncement);
            log.info("Se creo el anuncio con �xito: " + agAnnouncement.getAnnouncementId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            result.setError(true);
            e.printStackTrace();
            log.info("Error al crear el anuncio: " + agAnnouncement.getAnnouncementId());
        }
        return result;
    }

    /**
	 * Realiza la busqueda de un anuncio
	 * <p> * En caso de error, se retorna {@link RestServiceResult} con el
	 * mensaje de error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nAnnouncementId
	 *            c�digo del anuncio
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult search(RestServiceResult serviceResult, Long nAnnouncementId) {
        AgAnnouncement agAnnouncement = new AgAnnouncementDAO().findById(nAnnouncementId);
        EntityManagerHelper.refresh(agAnnouncement);
        if (agAnnouncement == null) {
            serviceResult.setError(true);
        } else {
            List<AgAnnouncement> listAgAnnouncement = new ArrayList<AgAnnouncement>();
            listAgAnnouncement.add(agAnnouncement);
            serviceResult.setObjResult(listAgAnnouncement);
            log.info("Tamano de la lista de anuncios: " + listAgAnnouncement.size());
        }
        return serviceResult;
    }

    /**
	 * Actualiza los datos del anuncio 
	 * <p>
	 * En caso de error, se retorna {@link RestServiceResult} con el mensaje de
	 * error
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param agAnnouncement
	 *            Anuncio a actualizar
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, AgAnnouncement agAnnouncement) {
        AgAnnouncementDAO agAnnouncementDAO = new AgAnnouncementDAO();
        try {
            EntityManagerHelper.beginTransaction();
            agAnnouncementDAO.update(agAnnouncement);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(agAnnouncement);
            log.info("Se actualiz� el anuncio: " + agAnnouncement.getAnnouncementId());
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al actualizar anuncio: " + e.getMessage());
            serviceResult.setError(true);
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de anuncios por usuario
	 * 
	 * @param serviceResult
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
    public RestServiceResult listAnnouncementForUser(RestServiceResult serviceResult, Long nUserId) {
        log.info("El usuario es: " + nUserId);
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        if (maUser == null) {
            serviceResult.setError(true);
        } else {
            List<AgAnnouncement> list = new ArrayList<AgAnnouncement>(maUser.getAgAnnouncements());
            if (list.size() == 0) {
                log.info("lista de anuncios vacia..");
            } else {
                log.info("lista de anuncios cargada..");
            }
            serviceResult.setObjResult(list);
            serviceResult.setNumResult(list.size());
        }
        return serviceResult;
    }

    /**
	 * Permite construir un anuncio para ser guardado en la base de datos.
	 * @param announcement anuncio a generar para ser guardado en la base de datos
	 * @return
	 */
    public AgAnnouncement buildAgAnnouncement(Announcement announcement) {
        AgAnnouncement agAnnouncement = new AgAnnouncement();
        AgAnnouncementType agAnnouncementType = new AgAnnouncementTypeDAO().findById(announcement.getType());
        EntityManagerHelper.refresh(agAnnouncement);
        agAnnouncement.setTitle(announcement.getTitle());
        agAnnouncement.setDate(new Date());
        agAnnouncement.setContent(announcement.getContent());
        agAnnouncement.setAgAnnouncementType(agAnnouncementType);
        return agAnnouncement;
    }

    /**
	 * Permite guardar la relaci�n entre la tabla de anuncios y la tabla de usuarios
	 * @param agAnnouncement anuncio
	 * @param nUserId c�digo del usuario
	 */
    public void addAgAnnouncementUser(AgAnnouncement agAnnouncement, Long nUserId) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.INSERT_AG_ANNOUNCEMENT_USER);
            query.setParameter(1, agAnnouncement.getAnnouncementId());
            query.setParameter(2, nUserId);
            query.executeUpdate();
            EntityManagerHelper.commit();
        } catch (Exception e) {
            EntityManagerHelper.rollback();
            log.info("Error al asociarle al anuncio un usuario.." + e.getStackTrace());
        }
    }

    public static void main(String[] args) {
        CoActivity coActivity = new CoActivityDAO().findById(new Long("236"));
        EntityManagerHelper.refresh(coActivity);
        System.out.println("anuncio: " + coActivity);
    }
}
