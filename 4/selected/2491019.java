package com.ewansilver.raindrop;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.ewansilver.concurrency.Channel;
import com.ewansilver.concurrency.ChannelFactory;
import com.ewansilver.raindrop.nio.SelectorRunnable;

/**
 * <p>
 * Creates an instance of the Raindrop runtime.
 * </p>
 * 
 * <p>
 * The logger is <i>com.ewansilver.raindrop.Raindrop</i>. All exceptions are
 * logged at Level.FINER.
 * </p>
 * 
 * @author Ewan Silver
 */
public class Raindrop {

    /**
	 * The Logger to use for Raindrop.
	 */
    private static Logger logger = Logger.getLogger("com.ewansilver.raindrop.Raindrop");

    private StageManager manager;

    /**
	 * A Map of admission controller names to QueueAdmissionController
	 * instances.
	 */
    private Map<String, QueueAdmissionsController> admissionControllers;

    /**
	 * Constructor. Initialises the system, in the correct order, with the correct file.
	 * 
	 * @param anXMLConfigFile
	 *            the xml configuration that will initialise the runtime.
	 * @throws StartupException
	 *             thrown if there is a problem initialising the system.
	 */
    public Raindrop(File anXMLConfigFile) throws StartupException {
        this(anXMLConfigFile.getAbsolutePath());
    }

    /**
	 * Constructor. Initialises the system, in the correct order, with the correct URI.
	 * 
	 * @param aURI the URI that contains the configuration file that will initialise the 
	 * runtime.
	 * @throws StartupException thrown if there is a problem initialising the system.
	 */
    public Raindrop(String aURI) throws StartupException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        Element root;
        try {
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            Document document = builder.parse(aURI);
            root = document.getDocumentElement();
        } catch (ParserConfigurationException e) {
            logger.log(Level.FINER, "Unable to parse the configuration URI: " + aURI, e);
            throw new StartupException("Unable to parse the configuration URI: " + aURI, e);
        } catch (SAXException e) {
            logger.log(Level.FINER, "A problem has occurred in processing the configuration URI: " + aURI, e);
            throw new StartupException("A problem has occurred in processing the configuration URI: " + aURI, e);
        } catch (IOException e) {
            logger.log(Level.FINER, "A problem has occurred in accessing the configuration URI: " + aURI, e);
            throw new StartupException("A problem has occurred in accessing the configuration URI: " + aURI, e);
        }
        ResponseTimeMonitorThread rtMonitorThread = new ResponseTimeMonitorThread();
        Thread rtMonitorThreadT = new Thread(rtMonitorThread);
        rtMonitorThreadT.setDaemon(true);
        rtMonitorThreadT.start();
        long threadPoolUpdate = 1000;
        manager = new StageManager(threadPoolUpdate);
        try {
            createNIO();
        } catch (IOException e) {
            logger.log(Level.FINER, "A problem has occurred in setting up the NIO subsystem.", e);
            throw new StartupException("A problem has occurred in setting up the NIO subsystem.", e);
        }
        NodeList admissionControllerNodes = root.getElementsByTagName("admissioncontrollers");
        logger.info("Number of admission controllers found: " + admissionControllerNodes.getLength());
        admissionControllers = createAdmissionControllers(admissionControllerNodes);
        NodeList stages = root.getElementsByTagName("stage");
        logger.info("Number of stages found: " + stages.getLength());
        createStages(stages, rtMonitorThread.getChannel());
        logger.info("stagegetlength=" + stages.getLength());
        initialiseStages(stages);
    }

    /**
	 * Parse the QueueAdmissionControllers in the config file.
	 * 
	 * @param admissionControllerNodes
	 * @return
	 */
    private Map<String, QueueAdmissionsController> createAdmissionControllers(NodeList admissionControllerNodes) {
        return new HashMap();
    }

    /**
	 * The StageManager that controls the Stage instances.
	 * 
	 * @return the StageManager.
	 */
    public StageManager getStageManager() {
        return manager;
    }

    /**
	 * Creates stages.
	 * 
	 * @param stages
	 * @param responseTimeMonitorChannel
	 * @throws StartupException
	 *             if there is a problem creating or initialising any of the
	 *             Stages or associated AdmissionControllers.
	 */
    public void createStages(NodeList stages, Channel responseTimeMonitorChannel) throws StartupException {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (int i = 0; i < stages.getLength(); i++) {
            Element stageElement = (Element) stages.item(i);
            String handle = stageElement.getAttribute("name");
            String className = stageElement.getAttribute("eventhandler");
            Class eventHandlerClass;
            try {
                eventHandlerClass = Class.forName(className);
            } catch (ClassNotFoundException e1) {
                logger.log(Level.FINER, "Unable to find the eventhandler class '" + className + "' for the stage '" + handle + "'.", e1);
                throw new StartupException("Unable to find the eventhandler class '" + className + "' for the stage '" + handle + "'.", e1);
            }
            Handler eventHandler;
            try {
                eventHandler = (Handler) eventHandlerClass.newInstance();
            } catch (InstantiationException e1) {
                logger.log(Level.FINER, "Unable to instantiate the eventhandler class '" + className + "' for the stage '" + handle + "'.", e1);
                throw new StartupException("Unable to instantiate the eventhandler class '" + className + "' for the stage '" + handle + "'.", e1);
            } catch (IllegalAccessException e1) {
                logger.log(Level.FINER, "Unable to access the eventhandler class '" + className + "' for the stage '" + handle + "'.", e1);
                throw new StartupException("Unable to access the eventhandler class '" + className + "' for the stage '" + handle + "'.", e1);
            }
            ResponseTimeMonitor responseTimeMonitor = new ResponseTimeMonitor(responseTimeMonitorChannel);
            Stage stage = new Stage(eventHandler, responseTimeMonitor);
            manager.register(handle, stage);
            NodeList controllers = stageElement.getElementsByTagName("admincontroller");
            for (int j = 0; j < controllers.getLength(); j++) {
                Element adminController = (Element) controllers.item(i);
                String adminControllerClassName = adminController.getAttribute("class");
                Class clazz;
                try {
                    clazz = Class.forName(adminControllerClassName);
                } catch (ClassNotFoundException e) {
                    logger.log(Level.FINER, "Unable to find the AdmissionsController class '" + adminControllerClassName + "'.", e);
                    throw new StartupException("Unable to find the AdmissionsControler class '" + adminControllerClassName + "'.", e);
                }
                QueueAdmissionsController controller;
                try {
                    controller = (QueueAdmissionsController) clazz.newInstance();
                } catch (InstantiationException e) {
                    logger.log(Level.FINER, "Unable to instantiate the AdmissionsController class '" + adminControllerClassName + "'.", e);
                    throw new StartupException("Unable to instantiate the AdmissionsControler class '" + adminControllerClassName + "'.", e);
                } catch (IllegalAccessException e) {
                    logger.log(Level.FINER, "Unable to access the AdmissionsController class '" + adminControllerClassName + "'.", e);
                    throw new StartupException("Unable to access the AdmissionsControler class '" + adminControllerClassName + "'.", e);
                }
                stage.addQueueAdmissionsController(controller);
                NodeList paramNodes = adminController.getElementsByTagName("param");
                Map<String, String> parameterMap = new HashMap<String, String>();
                for (int k = 0; k < paramNodes.getLength(); k++) {
                    Element parameter = (Element) paramNodes.item(k);
                    String parameterName = parameter.getAttribute("name");
                    String parameterValue = parameter.getAttribute("value");
                    parameterMap.put(parameterName, parameterValue);
                }
                controller.initialise(parameterMap);
            }
            try {
                ObjectName stageName = new ObjectName("Stage:handler=" + stage.getEventHandler().getClass().getName());
                mbs.registerMBean(stage, stageName);
            } catch (MalformedObjectNameException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (InstanceAlreadyExistsException e) {
                e.printStackTrace();
            } catch (MBeanRegistrationException e) {
                e.printStackTrace();
            } catch (NotCompliantMBeanException e) {
                e.printStackTrace();
            }
            logger.info("Stage loaded: " + eventHandler.getClass().getName());
        }
    }

    /**
	 * Initialises stages.
	 * 
	 * @param stages
	 * @throws StartupException
	 *             if there is a problem initialising any of the Stages.
	 */
    public void initialiseStages(NodeList stages) throws StartupException {
        logger.entering(Raindrop.class.getName(), "initialiseStages");
        for (int i = 0; i < stages.getLength(); i++) {
            Element stageElement = (Element) stages.item(i);
            String handle = stageElement.getAttribute("name");
            logger.info("handle@ " + handle);
            NodeList params = stageElement.getElementsByTagName("param");
            int numberOfParams = params.getLength();
            NameValuePair[] nvParams = new NameValuePair[numberOfParams];
            for (int j = 0; j < numberOfParams; j++) {
                Element param = (Element) params.item(j);
                String name = param.getAttribute("name");
                String value = param.getAttribute("value");
                logger.info("VALUE: " + value);
                nvParams[j] = new NameValuePair(name, value);
            }
            Stage stage;
            try {
                stage = manager.getStage(handle);
            } catch (UnknownTaskQueueException e) {
                logger.log(Level.FINER, "Unable to find a TaskQueue.", e);
                throw new StartupException("Unable to find a TaskQueue.", e);
            }
            Handler handler = stage.getEventHandler();
            try {
                handler.init(handle, manager, nvParams);
            } catch (InvalidEventHandlerException e) {
                logger.log(Level.FINER, "A problem has occurred in initalising the event handler '" + handle + "'.", e);
                throw new StartupException("A problem has occurred in initalising the event handler '" + handle + "'.", e);
            }
        }
    }

    /**
	 * Inits the RaindropServerRunnable threads, channels, etc. The taskqueue is
	 * known as "nio".
	 * 
	 */
    public void createNIO() throws IOException {
        Selector selector = Selector.open();
        Channel channel = ChannelFactory.instance().getChannel();
        Queue nioQueue = new Queue(new CompositeQueueAdmissionsController(), channel);
        SelectorRunnable nioRunnable = new SelectorRunnable(selector, channel, manager);
        Thread thread = new Thread(nioRunnable);
        thread.start();
        manager.setNioTaskQueue(nioQueue);
    }

    /**
	 * Shuts down the Raindrop instance, stopping any active Stages and
	 * WorkerThreads.
	 */
    public void shutDown() {
    }
}
