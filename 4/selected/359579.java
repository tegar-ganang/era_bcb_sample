package br.unb.unbiquitous.ubiquitos.uos.connectivity.proxying;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import org.apache.log4j.Logger;
import br.unb.unbiquitous.ubiquitos.network.exceptions.NetworkException;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.SmartSpaceGateway;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.connectivity.ConnectivityException;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpNetworkInterface;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

/**
 * Class responsible for representing a driver on a remote device, the real provider of the
 * driver we are representing. This driver only forwards to the real provider every service 
 * call made to us.
 * 
 * @author Lucas Paranhos Quintella
 *
 */
public class ProxyDriverImpl implements ProxyDriver {

    /** The real provider of this driver */
    private UpDevice provider;

    /** The interface of the real driver */
    private UpDriver driver;

    /** The context of the middleware */
    private Gateway gateway;

    /** Logging object */
    private static final Logger logger = Logger.getLogger(ProxyDriverImpl.class);

    /** Threading attribute */
    private boolean doneServiceCall;

    /** Time to establish the context correctly */
    private static final int TIME_TO_MAKE_THE_CONTEXT = 4000;

    /**
	 * Constructor
	 * @param driver The driver's interface
	 * @param provider The real provider of this driver
	 */
    public ProxyDriverImpl(UpDriver driver, UpDevice provider) {
        this.driver = driver;
        this.provider = provider;
        this.doneServiceCall = false;
    }

    /**
	 * Method responsible for forwarding the service call to the real provider. Any service call made to
	 * us is redirected to the real provider by using this method. 
	 * @param serviceCall The service call
	 * @param serviceResponse The service response
	 * @param messageContext Our message context of streams respective to the caller device
	 */
    public synchronized void forwardServiceCall(ServiceCall serviceCall, ServiceResponse serviceResponse, UOSMessageContext messageContext) {
        if (serviceCall.getServiceType().equals(ServiceCall.ServiceType.STREAM)) {
            try {
                UpNetworkInterface netInt = ((SmartSpaceGateway) this.gateway).getConnectivityManager().getAppropriateInterface(this.provider, serviceCall);
                serviceCall.setChannelType(netInt.getNetType());
            } catch (NetworkException e) {
                logger.error(e.getMessage());
            }
        }
        Thread serviceCallThread = new ProxyServiceCall(serviceCall, serviceResponse, messageContext);
        serviceCallThread.start();
        synchronized (serviceCallThread) {
            while (!doneServiceCall) {
                try {
                    serviceCallThread.wait();
                } catch (InterruptedException e) {
                    logger.debug("ProxyDriverImpl - Problem sleeping");
                }
            }
        }
    }

    /**
	 * Gets the real provider of this driver
	 * @return The provider device
	 */
    public UpDevice getProvider() {
        return this.provider;
    }

    /**
	 * Gets the interface of this driver
	 * @return The driver's interface
	 */
    public UpDriver getDriver() {
        return this.driver;
    }

    public void init(Gateway gateway, String instanceId) {
        this.gateway = gateway;
    }

    /**
	 * Tears down this driver and its dependencies
	 */
    public void destroy() {
    }

    /**
	 * Class responsible for making a single service call on a new thread for getting a new message
	 * context of streams.
	 * @author Lucas Paranhos Quintella
	 *
	 */
    private class ProxyServiceCall extends Thread {

        private ServiceCall serviceCall;

        private ServiceResponse serviceResponse;

        private UOSMessageContext messageContextBefore;

        private UOSMessageContext messageContextAfter;

        private int numberChannels;

        /**
		 * Constructor
		 * @param serviceCall
		 * @param serviceResponse
		 * @param messageContextBefore
		 */
        public ProxyServiceCall(ServiceCall serviceCall, ServiceResponse serviceResponse, UOSMessageContext messageContextBefore) {
            this.serviceCall = serviceCall;
            this.serviceResponse = serviceResponse;
            this.messageContextBefore = messageContextBefore;
            this.numberChannels = serviceCall.getChannels();
        }

        /**
		 * Starts the thread which will basically do the service call and get a new context of streams.
		 */
        public synchronized void run() {
            ServiceResponse newServiceResponse = null;
            try {
                newServiceResponse = ProxyDriverImpl.this.gateway.callService(ProxyDriverImpl.this.provider, this.serviceCall);
            } catch (ServiceCallException e) {
                logger.error(e.getMessage());
            }
            if (this.serviceCall.getServiceType().equals(ServiceCall.ServiceType.STREAM)) {
                try {
                    Thread.sleep(TIME_TO_MAKE_THE_CONTEXT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.messageContextAfter = this.messageContextBefore;
                try {
                    redirectStreams();
                } catch (ConnectivityException e) {
                    logger.error(e);
                    this.serviceResponse.setError("Error during proxying. Cause:" + e.getMessage());
                    ProxyDriverImpl.this.doneServiceCall = true;
                    logger.debug("The proxyied service call has been done.");
                    notify();
                    return;
                }
            }
            this.serviceResponse.setResponseData(newServiceResponse.getResponseData());
            ProxyDriverImpl.this.doneServiceCall = true;
            logger.debug("The proxyied service call has been done.");
            notify();
        }

        /**
		 * Redirects the content of the given input to the given output
		 */
        private void redirectStreams() throws ConnectivityException {
            for (int i = 0; i < this.numberChannels; i++) {
                DataInputStream input = this.messageContextBefore.getDataInputStream(i);
                DataOutputStream output = this.messageContextAfter.getDataOutputStream(i);
                Thread stream = new RedirectStream(input, output);
                stream.start();
                output = this.messageContextBefore.getDataOutputStream(i);
                input = this.messageContextAfter.getDataInputStream(i);
                stream = new RedirectStream(input, output);
                stream.start();
            }
        }

        /**
		 * Inner thread class responsible for redirecting the streams
		 * @author Lucas Paranhos Quintella
		 *
		 */
        private class RedirectStream extends Thread {

            /** The input stream */
            DataInputStream input;

            /** The output stream */
            DataOutputStream output;

            /**
			 * Constructor.
			 * @param input The input for reading data.
			 * @param output The output for writing data.
			 */
            public RedirectStream(DataInputStream input, DataOutputStream output) throws ConnectivityException {
                if (input == null || output == null) {
                    logger.error("Constructor: Input or output is null");
                    throw new ConnectivityException("Problem getting the message context");
                }
                this.input = input;
                this.output = output;
            }

            /**
			 * Starts the thread. While it does have content on the input, writes it on the output.
			 */
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.input));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(this.output));
                while (true) {
                    try {
                        if (reader.ready()) {
                            int available = this.input.available();
                            StringBuilder builder = new StringBuilder();
                            for (int i = 0; i < available; i++) {
                                builder.append((char) reader.read());
                            }
                            writer.write(builder.toString());
                            writer.flush();
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    public List<UpDriver> getParent() {
        return null;
    }
}
