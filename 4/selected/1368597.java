package org.dbe.dss.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Array;
import org.apache.log4j.Logger;
import org.dbe.dss.DSSException;
import org.dbe.dss.service.index.IndexConnector;
import org.dbe.dss.service.index.IndexFactory;
import org.dbe.dss.service.store.StoreConnector;
import org.dbe.dss.service.store.StoreFactory;
import org.dbe.dss.utils.FileHelper;
import org.dbe.servent.ServentContext;
import org.dbe.servent.ServerServentException;
import org.dbe.servent.ServiceContext;
import org.dbe.servent.service.ServiceSearcher;
import org.dbe.servent.tools.CoreAdapter;

/**
 * The implementation of the DSS Service.
 *
 * @author Intel Ireland Ltd.
 * @version 0.5.0
 */
public class DSSServiceImpl implements DSSService, CoreAdapter {

    /** Log4j logger for this object */
    private static Logger logger = Logger.getLogger(DSSServiceImpl.class.getName());

    /** DSS Service Index class */
    private static IndexConnector indexConnection = null;

    /** DSS Service Store class  */
    private static StoreConnector localStore = null;

    /** The unique ID of the store associated with this DSS Service */
    private static String storeID;

    /** ServiceContext for this service */
    private ServiceContext thisServiceContext;

    /** ServentContext for this core service */
    private ServentContext thisServentContext;

    /**
     * Default constructor.
     */
    public DSSServiceImpl() {
        super();
        logger.info("DSS Service instantiated");
    }

    /**
     * @see org.dbe.dss.service.DSSService#write(byte[], int)
     */
    public final String write(final byte[] content, final int timetolive) throws DSSException {
        logger.debug("write(..., " + timetolive + ")");
        return write(content, timetolive, true);
    }

    /**
     * @see org.dbe.dss.service.DSSService#write(byte[], int, boolean)
     */
    public final String write(final byte[] content, final int timetolive, final boolean replicate) throws DSSException {
        logger.debug("write(..., " + timetolive + "," + replicate + ")");
        String errMsg;
        String hashcode = localStore.write(content);
        boolean success = false;
        try {
            success = indexConnection.setLocation(hashcode, storeID, timetolive);
        } catch (DSSException e) {
            logger.info("Pretending that setLocation worked even though there was this exception: " + e.getMessage());
            success = true;
        }
        if (success && replicate) {
            logger.debug("Going to try to save remotely");
            ServiceSearcher ss = thisServiceContext.getSearcher();
            logger.debug("Got searcher");
            try {
                Object[] dssServices = ss.getService(DSSService.class, new String[] { "DSS" });
                logger.debug("Received response.");
                logger.debug("Received array of size " + dssServices.length);
                int saves = 0;
                for (int i = 0; (i < dssServices.length) && (saves < 2); i++) {
                    DSSService targetService = (DSSService) dssServices[i];
                    String remoteStoreID = targetService.getStoreID();
                    logger.debug("Considering saving a copy to dss with id" + remoteStoreID);
                    if (!remoteStoreID.equals(storeID)) {
                        logger.debug("Saving a copy to dss with id " + remoteStoreID);
                        targetService.write(content, timetolive, false);
                        saves++;
                    }
                }
            } catch (ServerServentException e) {
                errMsg = "Unexpected ServerServentException when attempting to write block remotely: " + e.getMessage();
                logger.error(errMsg);
                throw new DSSException(errMsg);
            } catch (ClassCastException e) {
                errMsg = "Unexpected ClassCastException when attempting to write block remotely: " + e.getMessage();
                logger.error(errMsg);
                throw new DSSException(errMsg);
            }
            logger.debug("Finished attempt to save remotely");
        }
        logger.debug("Checking save on DHT. Locations now there are: ");
        logger.debug(arrayToString(indexConnection.getLocation(hashcode)));
        logger.debug("End of check.");
        if (!success) {
            throw new DSSException("Index could not be updated");
        }
        return hashcode;
    }

    /**
     * @see org.dbe.dss.service.DSSService#read(java.lang.String)
     */
    public final byte[] read(final String hashcode) throws DSSException {
        logger.debug("read(" + hashcode + ")");
        if (!localStore.exists(hashcode)) {
            retrieveBlock(hashcode);
        }
        return localStore.read(hashcode);
    }

    /**
     * Retrieves the requested content from an appropriate remote DSS Service.
     *
     * @param hashcode the identifer corresponding to the content to be retrieved
     * @throws DSSException if an error occurs during invocation
     */
    private void retrieveBlock(final String hashcode) throws DSSException {
        logger.debug("retrieveBlock(" + hashcode + ")");
        String errMsg;
        String[] location = indexConnection.getLocation(hashcode);
        if (indexConnection == null) {
            logger.debug("indexConnetion is null");
        }
        if (location == null) {
            logger.debug("location is null");
        }
        logger.debug("Apparently hashcode " + hashcode + " is at the following " + location.length + " locations:");
        for (int i = 0; i < location.length; i++) {
            logger.debug(location[i]);
        }
        boolean retrieved = false;
        if (location.length > 0) {
            for (int j = 0; j < location.length && !retrieved; j++) {
                String remoteStoreID = location[j];
                logger.debug("Checking " + remoteStoreID);
                if (remoteStoreID.equals(storeID)) {
                    logger.debug("This service is location " + storeID + " so ignoring");
                } else {
                    logger.debug("Appears to be remote service, so will attempt to get proxy...");
                    Object dssService = null;
                    try {
                        logger.debug("about to getSearcher()...");
                        ServiceSearcher dssServiceSearcher = thisServiceContext.getSearcher();
                        logger.debug("about to getService()...");
                        Object[] services = dssServiceSearcher.getService(DSSService.class, new String[] { "DSS", remoteStoreID });
                        logger.debug("Found " + services.length + " services");
                        dssService = services[0];
                        if (dssService != null) {
                            logger.debug("services[0] was not null");
                            DSSService targetService = (DSSService) dssService;
                            logger.debug("going to retrieve, and write...");
                            localStore.write(targetService.read(hashcode));
                            indexConnection.setLocation(hashcode, storeID, 100000);
                            retrieved = true;
                        }
                    } catch (IOException e) {
                        errMsg = "Unexpected IOException when attempting to retrieve block locally: " + e.getMessage();
                        logger.error(errMsg);
                        throw new DSSException(errMsg);
                    } catch (ServerServentException e) {
                        errMsg = "Unexpected ServerServentException when attempting to retrieve block locally: " + e.getMessage();
                        logger.error(errMsg);
                        throw new DSSException(errMsg);
                    }
                }
            }
        } else {
            throw new DSSException("The hashcode " + hashcode + " cannot be located on the DSS");
        }
    }

    /**
     * @see org.dbe.dss.service.DSSService#read(java.lang.String, int, int)
     */
    public final byte[] read(final String hashcode, final int offset, final int len) throws DSSException {
        logger.debug("read(" + hashcode + ", " + offset + ", " + len + ")");
        if (!localStore.exists(hashcode)) {
            retrieveBlock(hashcode);
        }
        return localStore.read(hashcode, offset, len);
    }

    /**
     * @see org.dbe.dss.service.DSSService#update(java.lang.String, int)
     */
    public final boolean update(final String hashcode, final int timetolive) {
        logger.debug("update(" + hashcode + "," + timetolive + ")");
        return false;
    }

    /**
     * @see org.dbe.dss.service.DSSService#getTimeToLive(java.lang.String)
     */
    public final int getTimeToLive(final String hashcode) {
        logger.debug("getTimeToLive(" + hashcode + ")");
        return 0;
    }

    /**
     * @see org.dbe.dss.service.DSSService#getStoreID()
     */
    public final String getStoreID() {
        logger.debug("getStoreID()");
        return storeID;
    }

    /**
     * @see org.dbe.dss.service.DSSService#isAlive()
     */
    public final boolean isAlive() {
        logger.debug("isAlive()");
        return true;
    }

    /**
     * @see org.dbe.servent.Adapter#destroy()
     */
    public final void destroy() {
        logger.debug("destroy()");
    }

    /**
     * @see org.dbe.servent.Adapter#init(org.dbe.servent.ServiceContext)
     */
    public final void init(final ServiceContext serviceContext) {
        logger.debug("init(ServiceContext)");
        logger.info("DSS Service initialising - service context");
        thisServiceContext = serviceContext;
        String localStoreType = serviceContext.getParameter("dss.store.type");
        logger.debug("localStoreType: " + localStoreType);
        try {
            localStore = StoreFactory.getStoreConnector(localStoreType);
            storeID = localStore.configure(serviceContext);
            if (!storeID.equals(serviceContext.getParameter("dss.store.id"))) {
                String propertyPath;
                propertyPath = thisServiceContext.getHome().getAbsolutePath() + "/deployment.props";
                try {
                    FileHelper.replace(propertyPath, "__UNCONFIGURED_DSS_STORE_ID__", storeID);
                    logger.info("Attempted to update dss.store.id in " + propertyPath);
                } catch (FileNotFoundException e) {
                    logger.debug("The properties file " + propertyPath + " does not exist, so configuration not being updated there");
                } catch (IOException e) {
                    logger.warn("Could not update configuration in file " + propertyPath + ": " + e.getMessage());
                }
                propertyPath = thisServiceContext.getHome().getAbsolutePath() + "/deployment.xml";
                try {
                    FileHelper.replace(propertyPath, "__UNCONFIGURED_DSS_STORE_ID__", storeID);
                    logger.info("Attempted to update dss.store.id in " + propertyPath);
                } catch (FileNotFoundException e) {
                    logger.error("The properties file " + propertyPath + " does not exist, so configuration can not be updated");
                } catch (IOException e) {
                    logger.error("Could not update configuration in file " + propertyPath + ": " + e.getMessage());
                }
            }
        } catch (DSSException e) {
            logger.fatal("Could not initialise the local DSS store: " + e.getMessage());
        }
        logger.info("DSS Service Service initialisation complete");
    }

    /**
     * @see org.dbe.servent.tools.CoreAdapter#init(org.dbe.servent.ServentContext)
     */
    public final void init(final ServentContext serventContext) {
        logger.debug("init(ServentContext)");
        logger.info("DSS Service initialising - servent context");
        thisServentContext = serventContext;
        String localIndexType = thisServiceContext.getParameter("dss.index.type");
        logger.debug("localIndexType: " + localIndexType);
        try {
            indexConnection = IndexFactory.getIndexConnector(localIndexType);
            indexConnection.configure(thisServentContext, thisServiceContext);
        } catch (DSSException e) {
            logger.fatal("Could not initialise the DSS index: " + e.getMessage());
        }
        logger.info("DSS Service Servent initialisation complete");
    }

    private static String arrayToString(String[] array) {
        if (array == null) {
            return "[NULL]";
        } else {
            String str = null;
            int length = Array.getLength(array);
            int lastItem = length - 1;
            StringBuffer sb = new StringBuffer("[");
            for (int i = 0; i < length; i++) {
                str = (String) Array.get(array, i);
                if (str != null) {
                    sb.append(str);
                } else {
                    sb.append("[NULL]");
                }
                if (i < lastItem) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
