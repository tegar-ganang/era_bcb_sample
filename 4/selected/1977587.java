package org.cagrid.transfer.client;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import org.apache.axis.client.Stub;
import org.apache.axis.message.addressing.EndpointReferenceType;
import org.apache.axis.types.URI.MalformedURIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cagrid.transfer.common.TransferServiceI;
import org.cagrid.transfer.context.client.TransferServiceContextClient;
import org.cagrid.transfer.context.client.helper.TransferClientHelper;
import org.cagrid.transfer.context.stubs.types.TransferServiceContextReference;
import org.cagrid.transfer.descriptor.DataDescriptor;
import org.cagrid.transfer.descriptor.Status;
import org.globus.gsi.GlobusCredential;

/**
 * This class is auto-generated, DO NOT EDIT GENERATED GRID SERVICE ACCESS METHODS.
 *
 * This client is generated automatically by Introduce to provide a clean unwrapped API to the
 * service.
 *
 * On construction the class instance will contact the remote service and retrieve it's security
 * metadata description which it will use to configure the Stub specifically for each method call.
 * 
 * @created by Introduce Toolkit version 1.2
 */
public class TransferServiceClient extends TransferServiceClientBase implements TransferServiceI {

    static final Log logger = LogFactory.getLog(TransferServiceClientBase.class);

    public TransferServiceClient(String url) throws MalformedURIException, RemoteException {
        this(url, null);
    }

    public TransferServiceClient(String url, GlobusCredential proxy) throws MalformedURIException, RemoteException {
        super(url, proxy);
    }

    public TransferServiceClient(EndpointReferenceType epr) throws MalformedURIException, RemoteException {
        this(epr, null);
    }

    public TransferServiceClient(EndpointReferenceType epr, GlobusCredential proxy) throws MalformedURIException, RemoteException {
        super(epr, proxy);
    }

    public static void usage() {
        System.out.println(TransferServiceClient.class.getName() + " -url <service url>");
    }

    public static void main(String[] args) {
        System.out.println("Running the Grid Service Client");
        try {
            if (!(args.length < 2)) {
                if (args[0].equals("-url")) {
                    TransferServiceClient client = new TransferServiceClient(args[1], GlobusCredential.getDefaultCredential());
                    String downloadFile = "25508470-94b8-11dd-a868-ff5e5e9523c5.cache";
                    logger.info("creating context reference");
                    TransferServiceContextReference ref3 = client.retrieveFile(downloadFile);
                    logger.info("creating service context client");
                    TransferServiceContextClient tclient3 = new TransferServiceContextClient(ref3.getEndpointReference());
                    logger.info("calling getData");
                    InputStream stream3 = TransferClientHelper.getData(tclient3.getDataTransferDescriptor());
                    logger.info("creating file " + downloadFile);
                    File data3 = new File("downloads/" + downloadFile);
                    logger.info("calling writeToFile to write " + downloadFile);
                    writeToFile(stream3, data3);
                } else {
                    usage();
                    System.exit(1);
                }
            } else {
                usage();
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void writeToFile(InputStream is, File file) {
        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            byte[] b = new byte[1024];
            int r = 0;
            boolean again = true;
            while (again) {
                if ((r = is.read(b)) > -1) out.write(b, 0, r); else again = false;
            }
            is.close();
            out.close();
        } catch (IOException e) {
            System.err.println("Error Writing/Reading Streams.");
        }
    }

    public org.cagrid.transfer.context.stubs.types.TransferServiceContextReference storeData() throws RemoteException {
        synchronized (portTypeMutex) {
            configureStubSecurity((Stub) portType, "storeData");
            org.cagrid.transfer.stubs.StoreDataRequest params = new org.cagrid.transfer.stubs.StoreDataRequest();
            org.cagrid.transfer.stubs.StoreDataResponse boxedResult = portType.storeData(params);
            return boxedResult.getTransferServiceContextReference();
        }
    }

    public org.cagrid.transfer.context.stubs.types.TransferServiceContextReference retrieveData() throws RemoteException {
        synchronized (portTypeMutex) {
            configureStubSecurity((Stub) portType, "retrieveData");
            org.cagrid.transfer.stubs.RetrieveDataRequest params = new org.cagrid.transfer.stubs.RetrieveDataRequest();
            org.cagrid.transfer.stubs.RetrieveDataResponse boxedResult = portType.retrieveData(params);
            return boxedResult.getTransferServiceContextReference();
        }
    }

    public org.cagrid.transfer.context.stubs.types.TransferServiceContextReference retrieveFile(java.lang.String filename) throws RemoteException {
        synchronized (portTypeMutex) {
            configureStubSecurity((Stub) portType, "retrieveFile");
            org.cagrid.transfer.stubs.RetrieveFileRequest params = new org.cagrid.transfer.stubs.RetrieveFileRequest();
            params.setFilename(filename);
            org.cagrid.transfer.stubs.RetrieveFileResponse boxedResult = portType.retrieveFile(params);
            return boxedResult.getTransferServiceContextReference();
        }
    }
}
