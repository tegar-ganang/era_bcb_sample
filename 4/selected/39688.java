package org.atricore.idbus.kernel.main.mediation.camel.component.binding;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Registry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atricore.idbus.kernel.main.mediation.*;
import org.springframework.context.ApplicationContext;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sgonzalez@atricore.org">Sebastian Gonzalez Oyuela</a>
 * @version $Id$
 */
public class CamelMediationEndpoint extends DefaultEndpoint<CamelMediationExchange> {

    private static final transient Log logger = LogFactory.getLog(CamelMediationEndpoint.class);

    public static final String CAMEL_ADDRESS_PREFIX = "camel://";

    private String binding;

    private String directEndpointUri;

    private boolean logMessages;

    private String channelRef;

    private Channel channel;

    protected Registry registry;

    protected ApplicationContext applicationContext;

    private Map<String, CamelMediationBinding> bindingRegistry = new HashMap<String, CamelMediationBinding>();

    private CamelMediationConsumer<CamelMediationExchange> idBusBindingConsumer;

    public CamelMediationEndpoint(String uri, String consumingAddress, MediationBindingComponent component) {
        super(uri, component);
        if (consumingAddress.startsWith(CAMEL_ADDRESS_PREFIX)) this.directEndpointUri = consumingAddress.substring(CAMEL_ADDRESS_PREFIX.length()); else this.directEndpointUri = consumingAddress;
    }

    @Override
    public CamelMediationExchange createExchange() {
        logger.debug("Creating Camel Mediation Exchange for Exchange");
        return super.createExchange();
    }

    @Override
    public CamelMediationExchange createExchange(ExchangePattern exchangePattern) {
        logger.debug("Creating Camel Mediation Exchange for Exchange Pattern : " + exchangePattern);
        return super.createExchange(exchangePattern);
    }

    /**
     * This method will create a Camel Mediation Exchange
     * @param exchange
     * @return
     */
    @Override
    public CamelMediationExchange createExchange(Exchange exchange) {
        logger.debug("Creating new Camel Mediation Exchage from Binding Endpoint, nested exchange is : " + (exchange != null ? exchange.getClass().getName() : "null"));
        CamelMediationExchange camelMediationExchange = new CamelMediationExchange(getCamelContext(), this, exchange.getPattern(), exchange);
        CamelMediationMessage in = new CamelMediationMessage();
        camelMediationExchange.setIn(in);
        return camelMediationExchange;
    }

    public boolean isSingleton() {
        return true;
    }

    /**
     * Producing to this endpoint is unsupported
     * @return
     * @throws Exception
     */
    public Producer<CamelMediationExchange> createProducer() throws Exception {
        throw new UnsupportedOperationException("Producing to this endpoint is unsupported");
    }

    /**
     * Create consumer to receive exchanges from Camel direct component (direct:) processor.
     * Create consumer for Camel Mediation binding component.
     */
    public Consumer<CamelMediationExchange> createConsumer(Processor processor) throws Exception {
        registry = super.getCamelContext().getRegistry();
        applicationContext = registry.lookup("applicationContext", ApplicationContext.class);
        assert channelRef != null : "Endpoint requires 'channelRef' parameter";
        channel = (Channel) applicationContext.getBean(channelRef);
        logger.debug("Creating Mediation Binding consumer for URI " + getEndpointUri());
        logger.debug("Receiving exchanges from " + directEndpointUri);
        Endpoint destinationEndpoint = getCamelContext().getEndpoint(directEndpointUri);
        logger.debug("Endpoint type : " + destinationEndpoint.getClass().getName());
        logger.debug("Processor type : " + processor.getClass().getName());
        Consumer directEndpointConsumer = destinationEndpoint.createConsumer(new ConsumerProcessor());
        directEndpointConsumer.start();
        this.idBusBindingConsumer = new CamelMediationConsumer<CamelMediationExchange>(this, processor);
        return this.idBusBindingConsumer;
    }

    public MediationMessage createBody(CamelMediationMessage message) {
        CamelMediationBinding b = getMediationBinding();
        if (b == null) throw new IllegalStateException("No registered binding found for endpoint binding " + binding);
        return b.createMessage(message);
    }

    protected void copyBackExchange(CamelMediationExchange camelMediationExchange, Exchange exchange) {
        CamelMediationMessage out = (CamelMediationMessage) camelMediationExchange.getOut();
        Message fault = camelMediationExchange.getFault(false);
        if (fault != null) {
            logger.debug("Camel Fault Message received " + fault.getMessageId() + ".  Using binding " + this.binding);
            CamelMediationBinding binding = getCamelMediationBinding(this.binding);
            CamelMediationMessage mediationFault = (CamelMediationMessage) fault;
            binding.copyFaultMessageToExchange(mediationFault, exchange);
        } else if (out != null) {
            String bindingName = null;
            MediationMessage outMsg = out.getMessage();
            if (outMsg == null) {
                if (exchange.getPattern().isOutCapable()) {
                    logger.error("Exchage OUT does not contain a Message, you MUST provide an output. " + exchange.getExchangeId() + "[" + exchange + "]");
                    throw new IllegalStateException("Exchage OUT does not have a Mediation Message. " + exchange.getExchangeId() + "[" + exchange + "]");
                }
                logger.debug("Using non-out capable exchange pattern");
                return;
            }
            if (out.getMessage().getDestination() != null) bindingName = out.getMessage().getDestination().getBinding();
            String b = bindingName != null ? bindingName : this.binding;
            CamelMediationBinding binding = getCamelMediationBinding(b);
            if (binding == null) throw new IllegalStateException("No registered binding found for " + b);
            binding.copyMessageToExchange(out, exchange);
        } else {
            if (exchange.getPattern().isOutCapable()) {
                logger.error("Exchage OUT is NULL, you MUST provide an output. " + exchange.getExchangeId() + "[" + exchange + "]");
                throw new IllegalStateException("Exchage OUT does not have a Mediation Message. " + exchange.getExchangeId() + "[" + exchange + "]");
            } else {
                logger.debug("Using non-out capable exchange pattern");
            }
        }
    }

    public String getBinding() {
        return binding;
    }

    public void setBinding(String binding) {
        this.binding = binding;
    }

    public boolean isLogMessages() {
        return logMessages;
    }

    public void setLogMessages(boolean logMessages) {
        this.logMessages = logMessages;
    }

    public String getChannelRef() {
        return channelRef;
    }

    public void setChannelRef(String channelRef) {
        this.channelRef = channelRef;
    }

    public Channel getChannel() {
        return channel;
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    protected CamelMediationBinding getMediationBinding() {
        return getCamelMediationBinding(binding);
    }

    protected CamelMediationBinding getCamelMediationBinding(String b) {
        CamelMediationBinding binding = bindingRegistry.get(b);
        if (binding == null) {
            MediationBindingFactory factory = channel.getIdentityMediator().getBindingFactory();
            if (factory == null) throw new IllegalArgumentException("No configured Mediation Binding Factory in mediator");
            if (logger.isTraceEnabled()) logger.trace("Attempting to create binding for " + b + " with factory " + factory);
            binding = (CamelMediationBinding) factory.createBinding(b, getChannel());
            if (logger.isTraceEnabled()) logger.trace("Created binding " + binding + " for " + b + " with factory " + factory);
            if (binding != null) {
                bindingRegistry.put(b, binding);
            } else throw new IllegalArgumentException("Factory " + factory + " does not support binding " + b);
        }
        return binding;
    }

    public void registerCamelMediationBinding(CamelMediationBinding bindingImpl) {
        this.bindingRegistry.put(binding, bindingImpl);
    }

    /**
     * Inner class that consumes an incomming message and sends it to the next processor.
     */
    protected class ConsumerProcessor implements Processor {

        public void process(Exchange exchange) throws Exception {
            logger.debug("Processing exchange " + exchange.getClass().getName() + " for IDBus Binding " + binding);
            CamelMediationExchange camelMediationExchange = createExchange(exchange);
            try {
                MediationMessage body = (MediationMessage) camelMediationExchange.getIn().getBody();
                camelMediationExchange.getIn().setBody(body);
                idBusBindingConsumer.getProcessor().process(camelMediationExchange);
            } catch (IdentityMediationFault e) {
                logger.debug("Error processing exchange " + exchange.getClass().getName() + " for IDBus Binding " + binding + ".  " + e.getMessage(), e);
                String errorMsg = "[" + channel.getName() + "@" + channel.getLocation() + "]" + e.getMessage() + "'";
                CamelMediationMessage fault = (CamelMediationMessage) camelMediationExchange.getFault();
                fault.setBody(new MediationMessageImpl(fault.getMessageId(), errorMsg, e));
            } catch (Exception e) {
                IdentityMediationFault f = new IdentityMediationFault("urn:org:atricore:idbus:error:fatal", null, "Fatal Error while processing requets", e.getMessage(), e);
                if (logger.isDebugEnabled()) logger.debug(e.getMessage(), e);
                logger.error("Generating Fault message for " + f.getMessage());
                Message fault = exchange.getFault();
                if (fault != null) {
                    if (fault instanceof CamelMediationMessage) {
                        CamelMediationMessage mediationFault = (CamelMediationMessage) fault;
                        fault.setBody(new MediationMessageImpl(fault.getMessageId(), f.getMessage(), f));
                    } else {
                        fault.setBody(new MediationMessageImpl(fault.getMessageId(), f.getMessage(), f));
                    }
                } else {
                    logger.error(e.getMessage(), e);
                }
            }
            copyBackExchange(camelMediationExchange, exchange);
        }
    }
}
