package org.meta.net.impl.consumer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.meta.net.FederationRequest;
import org.meta.net.FederationRequestType;
import org.meta.net.FederationService;
import org.meta.net.FederationServiceConsumer;
import org.meta.net.FederationServiceMessageCodes;
import org.meta.net.exception.FederationServiceConsumptionFailed;
import org.meta.net.exception.FederationServiceDiscoveryFailed;

/**
 * A simple "object push" consumer. The one who is actually pushing the Object
 * across
 *
 * @author  V.Ganesh
 * @version 2.0 (Part of MeTA v2.0)
 */
public class FederationServiceObjectPushConsumer extends FederationServiceConsumer {

    /** Creates a new instance of FederationServiceObjectPushConsumer */
    public FederationServiceObjectPushConsumer(Serializable pushedObject) {
        this.pushedObject = pushedObject;
    }

    /**
     * Discover if the desired service is available at a specified host.
     *
     * @param serviceProvider address of the service provider.
     * @throws FederationServiceDiscoveryFailed if the required service is
     *         unavailable on the specified host
     * @return an valid instance of FederationRequest that can be consumed by
     *         a valid instance of this calss
     */
    @Override
    public FederationRequest discover(InetAddress serviceProvider) throws FederationServiceDiscoveryFailed {
        try {
            int port = FederationService.getInstance().getFederatingPort();
            Socket sock = new Socket(serviceProvider, port);
            SSLSocketFactory sockFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket federatingSocket = (SSLSocket) sockFactory.createSocket(sock, serviceProvider.getHostAddress(), port, true);
            SSLServerSocketFactory serverFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            federatingSocket.setEnabledCipherSuites(serverFactory.getSupportedCipherSuites());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(federatingSocket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(federatingSocket.getInputStream()));
            writer.write(FederationRequestType.OBJECT_PUSH.toString() + '\n');
            writer.flush();
            String responce = reader.readLine();
            if (FederationServiceMessageCodes.valueOf(responce) != FederationServiceMessageCodes.SERVICE_AVAILABLE) {
                throw new IOException("Unexpected responce: " + responce);
            }
            return new FederationRequest(federatingSocket, reader, writer, FederationRequestType.OBJECT_PUSH);
        } catch (IOException ioe) {
            throw new FederationServiceDiscoveryFailed("Error while discovery: " + ioe.toString());
        }
    }

    /**
     * Consume the serive provided by the service provider.
     *
     * @param service the service that is to be consumed
     * @throws FederationServiceConsumptionFailed if an error occurs
     */
    @Override
    public void consume(FederationRequest service) throws FederationServiceConsumptionFailed {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(service.getFederationConnection().getOutputStream());
            oos.writeObject(pushedObject);
            oos.close();
            service.closeIt();
        } catch (Exception e) {
            System.err.println("Service consumption failed: " + e);
            e.printStackTrace();
            throw new FederationServiceConsumptionFailed(e.toString());
        }
    }

    private Serializable pushedObject;

    /**
     * Get the value of pushedObject
     *
     * @return the value of pushedObject
     */
    public Serializable getPushedObject() {
        return pushedObject;
    }

    /**
     * Set the value of pushedObject
     *
     * @param pushedObject new value of pushedObject
     */
    public void setPushedObject(Serializable pushedObject) {
        this.pushedObject = pushedObject;
    }
}
