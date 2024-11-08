package py.una.pol.gwttest.server.abm;

import java.util.List;
import java.util.ArrayList;
import net.sf.gilead.core.PersistentBeanManager;
import net.sf.gilead.core.serialization.GwtProxySerialization;
import net.sf.gilead.core.store.stateless.StatelessProxyStore;
import net.sf.gilead.gwt.PersistentRemoteService;
import net.sf.gilead.core.hibernate.HibernateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window;
import py.una.pol.gwttest.client.abm.ProveedorService;
import py.una.pol.gwttest.domain.*;

public class ProveedorManager extends PersistentRemoteService implements ProveedorService {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static List<Proveedor> proveedores = null;

    private static List<Producto> productos = null;

    private Log log = LogFactory.getLog(ProveedorManager.class);

    private static HibernateUtil gileadHibernateUtil = new HibernateUtil();

    public ProveedorManager() {
        log.debug("initializing hibernate persistance for gilead");
        gileadHibernateUtil.setSessionFactory(py.una.pol.gwttest.server.util.HibernateUtil.getSessionFactory());
        PersistentBeanManager persistentBeanManager = new PersistentBeanManager();
        persistentBeanManager.setPersistenceUtil(gileadHibernateUtil);
        StatelessProxyStore sps = new StatelessProxyStore();
        sps.setProxySerializer(new GwtProxySerialization());
        persistentBeanManager.setProxyStore(sps);
        setBeanManager(persistentBeanManager);
        log.debug("persistentBeanManager initialized");
    }

    public List<Producto> getProductos(Proveedor proveedor) throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        try {
            productos = (ArrayList<Producto>) session.createQuery("select q,TRIM(nombre) LTRIM(nombre)  RTRIM(nombre)  LTRIM(RTRIM(nombre)) from Producto q where q.codproveedor = :prov").setParameter("prov", proveedor).list();
            trans.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
        return productos;
    }

    public List<Proveedor> getProveedores() throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        List<Proveedor> prov;
        List<Proveedor> pdto;
        try {
            prov = new ArrayList<Proveedor>(session.createQuery("from Proveedor ").list());
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
        trans.commit();
        return prov;
    }

    private Proveedor crearProveedor(Proveedor p) {
        Proveedor pdto = new Proveedor();
        pdto.setRuc(p.getRuc());
        pdto.setDireccion(p.getDireccion());
        pdto.setNombre(p.getNombre());
        pdto.setTelefono(p.getTelefono());
        pdto.setCodigo(p.getCodigo());
        return pdto;
    }

    public void addProveedor(Proveedor proveedor) {
        proveedores.add(crearProveedor(proveedor));
    }

    public Proveedor getProveedor(Integer id) {
        try {
            proveedores = getProveedores();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        for (Proveedor proveedor : proveedores) {
            if (proveedor.getCodigo().equals(id)) {
                return proveedor;
            }
        }
        return null;
    }

    public void guardar(Proveedor proveedor) throws Exception {
        System.out.print("***********Guardando en el Manager***************  ");
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%" + proveedor.getRuc() + "  ********  " + proveedor.getNombre() + " **** " + proveedor.getDireccion() + " /////////// " + proveedor.getTelefono() + "#####" + proveedor.getCodigo());
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        try {
            session.merge(proveedor);
            trans.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
    }

    public Session getSession() {
        return gileadHibernateUtil.getSessionFactory().getCurrentSession();
    }

    public String borrar(List<Proveedor> proveedor) throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        String noBorrados = new String();
        List<Producto> pd;
        System.out.print("**************************Guardando en el Manager*********************************************  ");
        try {
            for (Proveedor p : proveedor) {
                pd = new ArrayList<Producto>(session.createQuery("select prod from Producto prod where prod.codproveedor =:p").setParameter("p", p).list());
                System.out.print("Resultado del query -----------> ");
                for (Producto q : pd) {
                    System.out.println(q.getNombre() + "  \n " + q.getDescripcion() + "  \n " + q.getCantidad() + "  \n " + q.getCodigo() + "  \n " + q.getCodproveedor().getNombre());
                }
                System.out.print("\nLista con cantidad de elementos igual a ***************  " + pd.size() + " ---------- \n");
                if (pd.size() == 0) {
                    System.out.println("Borrando a " + p.getNombre() + "\n");
                    session.createQuery("delete from Proveedor where codigo =:cod").setParameter("cod", p.getCodigo()).executeUpdate();
                } else {
                    noBorrados = noBorrados + p.getNombre() + "\n";
                    System.out.println("No se puede borrar\n");
                }
            }
            System.out.print("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            trans.commit();
            return noBorrados;
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
    }

    @Override
    public List<Proveedor> getProveedorespro() throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        List<Proveedor> prov;
        List<Proveedor> pdto;
        prov = new ArrayList<Proveedor>();
        List<Proveedor> pro;
        try {
            prov = new ArrayList<Proveedor>(((Query) session.createQuery("select distinct prov.codproveedor from Producto as prov")).list());
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
        trans.commit();
        return prov;
    }
}
