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
import org.hibernate.Session;
import org.hibernate.Transaction;
import py.una.pol.gwttest.client.abm.ProductoService;
import py.una.pol.gwttest.domain.*;

public class ProductoManager extends PersistentRemoteService implements ProductoService {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static List<Producto> productos = null;

    private Log log = LogFactory.getLog(ProductoManager.class);

    private static HibernateUtil gileadHibernateUtil = new HibernateUtil();

    public ProductoManager() {
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

    public List<Producto> getProductos() throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        List<Producto> prov;
        List<Producto> pdto;
        try {
            prov = new ArrayList<Producto>(session.createQuery("from Producto ").list());
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
        for (Producto m : prov) System.out.println(" Nombre del producto: " + m.getNombre());
        return prov;
    }

    private Producto crearProducto(Producto p) {
        Producto pdto = new Producto();
        pdto.setCodigo(p.getCodigo());
        pdto.setNombre(p.getNombre());
        pdto.setPrecio(p.getPrecio());
        pdto.setCantidad(p.getCantidad());
        pdto.setDescripcion(p.getDescripcion());
        pdto.setCodproveedor(p.getCodproveedor());
        return pdto;
    }

    public void addProducto(Producto producto) {
        productos.add(crearProducto(producto));
    }

    public Producto getProducto(Integer id) {
        try {
            System.out.println(" En get Producto :  " + "Identificador = a " + id);
            productos = getProductos();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        for (Producto producto : productos) {
            System.out.println("Productos retornasos    :  " + producto.getNombre() + "");
            if (producto.getCodigo().equals(id)) {
                return producto;
            }
        }
        return null;
    }

    public void guardar(Producto producto) throws Exception {
        System.out.print("***********Guardando en el Manager***************  " + producto.getNombre() + "   ");
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        try {
            session.saveOrUpdate(producto);
            trans.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
        System.out.println(" ########################LA CON DEL LORO#########################");
    }

    public String borrar(List<Producto> producto) throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        String noBorrados = new String();
        List<Compra> pd;
        System.out.print("**************************Guardando en el Manager*********************************************  ");
        try {
            for (Producto p : producto) {
                pd = new ArrayList<Compra>(session.createQuery("select comp from Compra comp where comp.codProducto =:c").setParameter("c", p).list());
                System.out.print("Resultado del query -----------> ");
                if (pd.size() == 0) {
                    System.out.println("Borrando a " + p.getNombre() + "\n");
                    session.createQuery("delete from Producto where codigo =:cod").setParameter("cod", p.getCodigo()).executeUpdate();
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

    public Session getSession() {
        return gileadHibernateUtil.getSessionFactory().getCurrentSession();
    }
}
