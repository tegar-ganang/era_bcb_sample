package edu.univalle.lingweb.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import edu.univalle.lingweb.Common;
import edu.univalle.lingweb.persistence.CoMaterial;
import edu.univalle.lingweb.persistence.CoMenu;
import edu.univalle.lingweb.persistence.CoMenuDAO;
import edu.univalle.lingweb.persistence.CoMenuType;
import edu.univalle.lingweb.persistence.CoMenuTypeDAO;
import edu.univalle.lingweb.persistence.EntityManagerHelper;
import edu.univalle.lingweb.persistence.MaUser;
import edu.univalle.lingweb.persistence.MaUserDAO;
import edu.univalle.lingweb.rest.RestServiceResult;

/**
 * Clase que contiene los m�todos CRUD (Create Read Update Delete) entre otros
 * para la tabla 'co_menu'( Opciones men� repositorio)
 * 
 * @author Jose Aricapa
 */
public class DataManagerMenu extends DataManager {

    /**
	 * @uml.property  name="htOptions"
	 */
    private HashMap<Long, MenuOption> htOptions;

    /**
	 * Manejador de mensajes de Log'ss
	 * @uml.property  name="log"
	 * @uml.associationEnd  multiplicity="(1 1)"
	 */
    private Logger log = Logger.getLogger(DataManagerMenu.class);

    /**
	 * Contructor de la clase
	 */
    public DataManagerMenu() {
        super();
        DOMConfigurator.configure(DataManagerMenu.class.getResource("/log4j.xml"));
    }

    /**
	 * Realiza el proceso de guardar una opci�n de men�
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMenu
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult create(RestServiceResult serviceResult, CoMenu coMenu) {
        CoMenuDAO coMenuDAO = new CoMenuDAO();
        try {
            coMenu.setMenuId(getSequence("sq_co_menu"));
            if (coMenu.getCoMenu() == null) {
                coMenu.setCoMenu(coMenu);
            }
            if (coMenu.getMenuId() == coMenu.getCoMenu().getMenuId()) {
                coMenu.setPath("/" + coMenu.getMenuName() + "/");
            } else {
                coMenu.setPath(coMenu.getCoMenu().getPath() + coMenu.getMenuName() + "/");
            }
            EntityManagerHelper.beginTransaction();
            coMenuDAO.save(coMenu);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMenu);
            log.info("Men� " + coMenu.getMenuName() + " creada con �xito...");
            Object[] arrayParam = { coMenu.getMenuName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.create.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la unit: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.create.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de guardar una opci�n de men�
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMenu
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult update(RestServiceResult serviceResult, CoMenu coMenu) {
        CoMenuDAO coMenuDAO = new CoMenuDAO();
        try {
            coMenu.setMenuId(getSequence("sq_co_menu"));
            EntityManagerHelper.beginTransaction();
            coMenuDAO.update(coMenu);
            EntityManagerHelper.commit();
            EntityManagerHelper.refresh(coMenu);
            log.info("Men� " + coMenu.getMenuName() + " creada con �xito...");
            Object[] arrayParam = { coMenu.getMenuName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.update.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la unit: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.update.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de un determinado men�
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUnitId
	 *            C�digo de la unidad
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult delete(RestServiceResult serviceResult, CoMenu coMenu) {
        try {
            log.info("Eliminando la menu: " + coMenu.getMenuName());
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.DELETE_CO_MENU);
            query.setParameter(1, coMenu.getMenuId());
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { coMenu.getMenuName() };
            log.info("Menu eliminada con �xito: " + coMenu.getMenuName());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la opci�n de men�: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coMenu.getMenuName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.delete.error") + e.getMessage(), arrayParam));
        }
        return serviceResult;
    }

    /**
	 * Realiza la eliminaci�n de los materiales de un men�
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param nUnitId
	 *            C�digo de la unidad
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult deleteMenuMaterial(RestServiceResult serviceResult, CoMenu coMenu) {
        DataManagerMaterial dataManagerMaterial = new DataManagerMaterial();
        try {
            dataManagerMaterial.setBundle(this.bundle);
            log.info("Eliminando la material del menu..." + coMenu.getMenuName());
            Set<CoMaterial> setMaterial = coMenu.getCoMaterials();
            for (CoMaterial coMaterial : setMaterial) {
                dataManagerMaterial.delete(serviceResult, coMaterial);
            }
            log.info("Eliminaci�n OK");
            Object[] arrayParam = { coMenu.getMenuName() };
            log.info("Menu eliminada con �xito: " + coMenu.getMenuName());
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.delete.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al eliminar la opci�n de men�: " + e.getMessage());
            serviceResult.setError(true);
            Object[] arrayParam = { coMenu.getMenuName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.delete.error") + e.getMessage(), arrayParam));
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
    public RestServiceResult search(RestServiceResult serviceResult, Long nMenuId) {
        log.info("Buscando Menu: " + nMenuId);
        CoMenu coMenu = new CoMenuDAO().findById(nMenuId);
        if (coMenu == null) {
            serviceResult.setError(true);
            serviceResult.setMessage(bundle.getString("menu.search.notFound"));
        } else {
            List<CoMenu> list = new ArrayList<CoMenu>();
            EntityManagerHelper.refresh(coMenu);
            list.add(coMenu);
            Object[] arrayParam = { list.size() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.search.success"), arrayParam));
            serviceResult.setObjResult(list);
        }
        return serviceResult;
    }

    /**
	 * Realiza el proceso de relacionar una opci�n de men� a un usuario
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n los mensajes
	 *            localizados y estado SQL .
	 * @param coMenu
	 *            men� a guardar
	 * 
	 * @return El {@link RestServiceResult} que contiene el resultado de la
	 *         operaci�n.s
	 */
    public RestServiceResult createMenuUser(RestServiceResult serviceResult, CoMenu coMenu, Long nUserId) {
        try {
            EntityManagerHelper.beginTransaction();
            Query query = EntityManagerHelper.createNativeQuery(Statements.INSERT_CO_MENU_USER);
            query.setParameter(1, coMenu.getMenuId());
            query.setParameter(2, nUserId);
            query.executeUpdate();
            EntityManagerHelper.commit();
            Object[] arrayParam = { coMenu.getMenuName() };
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.createMenuUser.success"), arrayParam));
        } catch (PersistenceException e) {
            EntityManagerHelper.rollback();
            log.error("Error al guardar la unit: " + e.getMessage());
            serviceResult.setError(true);
            serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.createMenuUser.error"), e.getMessage()));
            Util.printStackTrace(log, e.getStackTrace());
        }
        return serviceResult;
    }

    /**
	 * Obtiene la lista de menus (Padres-Hijos) del repositorio cursos de un
	 * determinado usuario
	 * 
	 * @param serviceResult
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listForUser(RestServiceResult serviceResult, Long nUserId) {
        MaUser maUser = new MaUserDAO().findById(nUserId);
        EntityManagerHelper.refresh(maUser);
        Set<CoMenu> setMenu = maUser.getCoMenus();
        return listMenu(serviceResult, setMenu);
    }

    /**
	 * Obtiene la lista de menus (Padres-Hijos) del repositorio general
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    public RestServiceResult listMenuGeneralRepository(RestServiceResult serviceResult) {
        CoMenuTypeDAO coMenuTypeDAO = new CoMenuTypeDAO();
        CoMenuType coMenuType = coMenuTypeDAO.findById(Common.REPOSITORY_GENERAL);
        EntityManagerHelper.refresh(coMenuType);
        Set<CoMenu> setMenu = coMenuType.getCoMenus();
        log.info("General => " + setMenu.size());
        return listMenu(serviceResult, setMenu);
    }

    /**
	 * Obtiene la lista de menus (Padres-Hijos)
	 * 
	 * @param result
	 *            El {@link RestServiceResult} que contendr�n el resultado de la
	 *            operaci�n.
	 * @return El {@link RestServiceResult} contiene el resultado de la
	 *         operaci�n.
	 */
    private RestServiceResult listMenu(RestServiceResult serviceResult, Set<CoMenu> setMenu) {
        htOptions = new HashMap<Long, MenuOption>();
        if (setMenu != null) {
            HashMap<Long, MenuOption> htAll = new HashMap<Long, MenuOption>();
            for (CoMenu menu : setMenu) {
                MenuOption menuOption = new MenuOption(menu.getMenuId(), menu.getCoMenu().getMenuId(), menu.getMenuName());
                htAll.put(menuOption.nMenuId, menuOption);
            }
            Iterator<Long> it = htAll.keySet().iterator();
            while (it.hasNext()) {
                MenuOption option = htAll.get(it.next());
                if (option.nMenuId != option.nParentMenuId) {
                    MenuOption parent = htAll.get(option.nParentMenuId);
                    if (parent != null) {
                        parent.addOption(option);
                    }
                } else {
                    htOptions.put(option.nMenuId, option);
                }
            }
        }
        Object[] array = { htOptions.size() };
        serviceResult.setMessage(MessageFormat.format(bundle.getString("menu.list.success"), array));
        serviceResult.setObjResult(htOptions);
        return serviceResult;
    }
}
