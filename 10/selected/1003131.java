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
import py.una.pol.gwttest.client.abm.ClienteService;
import py.una.pol.gwttest.domain.*;

public class ClienteManager extends PersistentRemoteService implements ClienteService {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static List<Cliente> clientes = null;

    private Log log = LogFactory.getLog(ClienteManager.class);

    private static HibernateUtil gileadHibernateUtil = new HibernateUtil();

    public ClienteManager() {
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

    public List<Cliente> getClientes() throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        List<Cliente> clie;
        List<Cliente> pdto;
        try {
            clie = new ArrayList<Cliente>(session.createQuery("from Cliente ").list());
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
        trans.commit();
        return clie;
    }

    private Cliente crearCliente(Cliente p) {
        Cliente pdto = new Cliente();
        pdto.setRuc(p.getRuc());
        pdto.setDireccion(p.getDireccion());
        pdto.setNombre(p.getNombre());
        pdto.setTelefono(p.getTelefono());
        pdto.setCodigo(p.getCodigo());
        return pdto;
    }

    public void addCliente(Cliente cliente) {
        clientes.add(crearCliente(cliente));
    }

    public Cliente getCliente(Integer id) {
        try {
            System.out.println(" En get Cliente :  " + "Identificador = a " + id);
            clientes = getClientes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        for (Cliente cliente : clientes) {
            System.out.println("Clientes retornasos    :  " + cliente.getNombre() + "");
            if (cliente.getCodigo().equals(id)) {
                return cliente;
            }
        }
        return null;
    }

    public void guardar(Cliente cliente) throws Exception {
        System.out.print("***********Guardando en el Manager***************  " + cliente.getNombre() + "   ");
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        try {
            session.saveOrUpdate(cliente);
            trans.commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            trans.rollback();
            throw ex;
        }
    }

    public String borrar(List<Cliente> cliente) throws Exception {
        Session session = getSession();
        Transaction trans = session.beginTransaction();
        String noBorrados = new String();
        List<Venta> pd;
        System.out.print("**************************Guardando en el Manager*********************************************  ");
        try {
            for (Cliente p : cliente) {
                pd = new ArrayList<Venta>(session.createQuery("select vent from Venta vent where vent.codCliente =:c").setParameter("c", p).list());
                System.out.print("Resultado del query -----------> ");
                if (pd.size() == 0) {
                    System.out.println("Borrando a " + p.getNombre() + "\n");
                    session.createQuery("delete from Cliente where codigo =:cod").setParameter("cod", p.getCodigo()).executeUpdate();
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
