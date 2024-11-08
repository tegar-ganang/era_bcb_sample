package demo.restful.server;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLOutputFactory;

public final class Server {

    private Server() {
    }

    public static void main(String[] args) throws Exception {
        CustomerServiceImpl bs = new CustomerServiceImpl();
        createSoapService(bs);
        createRestService(bs);
        createJsonRestService(bs);
        serveHTML();
        System.out.println("Started CustomerService!");
        System.out.println("Server ready...");
        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }

    private static void createRestService(Object serviceObj) {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(CustomerService.class);
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:8080/xml/");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));
        sf.getServiceFactory().setWrapped(false);
        sf.create();
    }

    private static void createJsonRestService(Object serviceObj) {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(CustomerService.class);
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:8080/json");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));
        sf.getServiceFactory().setWrapped(false);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type", "text/plain");
        Map<String, String> nstojns = new HashMap<String, String>();
        nstojns.put("http://demo.restful.server", "acme");
        MappedXMLInputFactory xif = new MappedXMLInputFactory(nstojns);
        properties.put(XMLInputFactory.class.getName(), xif);
        MappedXMLOutputFactory xof = new MappedXMLOutputFactory(nstojns);
        properties.put(XMLOutputFactory.class.getName(), xof);
        sf.setProperties(properties);
        sf.create();
    }

    private static void createSoapService(Object serviceObj) {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(CustomerService.class);
        sf.setAddress("http://localhost:8080/soap");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));
        sf.getServiceFactory().setWrapped(false);
        sf.create();
    }

    /**
     * Serve out a static HTTP file because the Javascript XMLHttpRequest can
     * only work within one domain.
     */
    private static void serveHTML() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm.getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://localhost:8080/test.html");
        Destination d = df.getDestination(ei);
        d.setMessageObserver(new MessageObserver() {

            public void onMessage(Message message) {
                try {
                    ExchangeImpl ex = new ExchangeImpl();
                    ex.setInMessage(message);
                    Conduit backChannel = message.getDestination().getBackChannel(message, null, null);
                    MessageImpl res = new MessageImpl();
                    res.put(Message.CONTENT_TYPE, "text/html");
                    backChannel.prepare(res);
                    OutputStream out = res.getContent(OutputStream.class);
                    FileInputStream is = new FileInputStream("test.html");
                    IOUtils.copy(is, out, 2048);
                    out.flush();
                    out.close();
                    is.close();
                    backChannel.close(res);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
