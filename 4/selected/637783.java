package iwork.icrafter.system;

import java.io.*;
import iwork.eheap2.*;
import iwork.state.*;
import java.util.*;
import java.lang.reflect.*;
import iwork.icrafter.util.*;
import java.net.*;

/**
 *
 * An ICrafter service is a special type of EventHeapService (@see
 * iwork.icrafter.system.EventHeapService) which has the following
 * properties:
 * 
 * <ul> 
 *
 * <li> An ICrafter service can typically be represented as a set of
 * callback methods that implement the service functionality. (For
 * example, a simple light service functionality can be represented by
 * the callback methods poweroff() and poweron()). Any of these methods
 * may be called over the EventHeap by other applications talking to
 * the EventHeap. </li>

 * <li> An ICrafter service "advertizes" itself over the EventHeap
 * (i.e., makes its presence known over the EventHeap. This
 * advertizement includes the SDL (which describes the methods
 * supported by the service and their parameters), the interfaces
 * implemented by the service (for example the light service
 * implements the interface
 * iwork.icrafter.services.devices.LightController), a human
 * understandable description of the service etc. </li>
 * 
 * <li> Users can request control interfaces for an ICrafter
 * service. While such control interfaces are automatically generated
 * by default, it is possible to write custom control interfaces. </li>
 *
 * </ul>
 *
 * This class is intended to be a base class for all ICrafter
 * Services. Typically, you will subclass this class to create your
 * own ICrafter service. There are two types of methods in this class:
 * 1. methods you can override while writing your service and
 * 2. methods you can use as helper library methods while writing your
 * services.
 *
 * Methods to override:
 * ~~~~~~~~~~~~~~~~~~~~
 * 
 * Typically, the following methods are overridden by the subclasses:
 *
 * <ul> 
 * <li><b>init:</b> Initialization method </li>
 * <li><b>getDescription:</b> Provides a description of the service </li>
 * <li><b>getShortDescription:</b> Provides a short description of the
 * service </li>
 * <li> <b>addSDL</b>: Adds SDL information for methods and parameters.
 * <li><b>getAdvertizeInterfaces:</b> Returns the interfaces that are
 * advertized for this service </li>
 * </ul>
 *
 * In some special cases, the following methods can also be overridden,
 * though you are NOT encouraged to do so unless you know what you are
 * doing!
 *
 * <ul>
 * <li><b>getSDL:</b> A method that returns the SDL for this service
 * which describes the methods supported by this service etc. </li>
 * <li><b>getSubscribeEvents:</b> A method that returns the events
 * this service wishes to subscribe to. For example, all services
 * typically subscribe to method call events directed to them.</li>
 * <li><b>receivedEvent:</b> A method that handles a method call (also
 * called operation call) event directed to this service</li>
 * <li><b>receivedNonOperationEvent:</b> A method that handles a
 * non-methodcall event directed to this service</li>
 * </ul>
 *
 * This class provides reasonable default implementationss for all the
 * methods that can be overridden. So, overriding is necessary only
 * when the defaults are not good enough. 
 *
 * Helper "library" methods
 * ~~~~~~~~~~~~~~~~~~~~~~~~~
 * The following helper methods can be invoked while writing your
 * services. The programmer guide and examples explain how/when to
 * use them.
 *
 * <ul>
 * <li>getName</li>
 * <li>getInitParameter</li>
 * <li>addMethodSDL</li>
 * <li>addParameterSDL</li>
 * <li>getStateSpace</li>
 * <li>initEventHeap</li>
 * <li>getEventHeap</li>
 * </ul>
 *
 * Other methods should neither be overridden nor be invoked.
 *
 * Note: All ICrafterService services are "started" by the class
 * iwork.icrafter.system.StartServices. This takes a configuration
 * file listing all services that need be run on a machine and
 * "starts" all of them. For more details see StartServices and the
 * programmer guide.
 * 
 * @see iwork.icrafter.system.StartServices
 * @see iwork.icrafter.system.EventHeapService
 *
 **/
public class ICrafterService implements EventHeapService {

    /** Name of this service */
    private String svcName;

    /** Maps the names of initial parameters for the services to values */
    private HashMap map;

    private String heapMachine;

    /** these two maps store sdl info for the methods and parameters
        of this service */
    private Map methodSDL = null;

    private Map paramSDL = null;

    /** caches generated SDL so it need not be generated again */
    private String sdl = null;

    private Object serviceObj;

    private boolean wrapper;

    private Hashtable icsHashtable;

    private int sync_model;

    private Object numThreadsLock = new String("Lock");

    private int numThreads = 0;

    private boolean initted = false;

    private StateSpace ss;

    private EventHeap sharedEventHeap;

    private EventHeap privateEventHeap = null;

    /** 
     * Default constructor. This constructor is called when the
     * service object is a subclass of the ICrafter Service itself. In
     * this case, this object is the service object. Contrast this
     * with {@link #ICrafterService(Object)} 
     */
    public ICrafterService() {
        this.serviceObj = null;
        wrapper = false;
        Class ics = ICrafterService.class;
        Method[] icsMethods = ics.getMethods();
        icsHashtable = new Hashtable();
        for (int i = 0; i < icsMethods.length; i++) {
            icsHashtable.put(icsMethods[i].getName(), icsMethods[i].getName());
        }
        sync_model = EventHeapService.SINGLE_INSTANCE;
        if (this instanceof MultiThreadModel) {
            sync_model = EventHeapService.MULTI_THREAD;
        } else if (this instanceof SingleThreadModel) {
            sync_model = EventHeapService.SINGLE_THREAD;
        }
    }

    /** 
     * This constructor is called when the service object is an
     * arbitrary object that is not a subclass of ICrafterService. In
     * this case, the service object is wrappered by this object to
     * provide the ICrafterService functionality for the service
     * object 
     **/
    public ICrafterService(Object serviceObj) {
        this.serviceObj = serviceObj;
        wrapper = true;
        Class ics = ICrafterService.class;
        Method[] icsMethods = ics.getMethods();
        icsHashtable = new Hashtable();
        for (int i = 0; i < icsMethods.length; i++) {
            icsHashtable.put(icsMethods[i].getName(), icsMethods[i].getName());
        }
        sync_model = EventHeapService.SINGLE_INSTANCE;
        if (serviceObj instanceof MultiThreadModel) {
            sync_model = EventHeapService.MULTI_THREAD;
        } else if (serviceObj instanceof SingleThreadModel) {
            sync_model = EventHeapService.SINGLE_THREAD;
        }
    }

    /**
     * Returns an integer indicating the synchronization model for this service
     * @return The synchronization model for this service -- one of
     * EventHeapService.SINGLE_INSTANCE,
     * EventHeapService.SINGLE_THREAD, or
     * EventHeapService.MULTI_THREAD
     **/
    public int getSyncModel() {
        return sync_model;
    }

    public String getName() {
        return svcName;
    }

    /**
     * Returns the value of the given init/config parameter. These
     * parameters are read off the config file which is read by
     * StartServices
     * @param paramName Name of the parameter whose value is sought 
     * @return Value of the passed parameter 
     **/
    protected String getInitParameter(String paramName) {
        return (String) map.get(paramName);
    }

    /**
     * Initialization method. This method is passed the initial
     * parameters for this service thru the map object. This
     * method is also responsible for initializing the data structures
     * used to store SDL information for this service. The service
     * name and type, and the machinename are "cached" in local
     * variables for use in advertisements and subscriptions. 

     * Why a separate method initConfig? why not do this in the
     * constructor - it turns out that you can only instantiate
     * objects of a given class when the class has an argument-free
     * constructor. InitConfig is a way of passing the initial
     * parameters for the service
     * 
     * @param map Map object that maps the names of
     * initial parameters of the service to their values. These
     * parameters are normally read from a config file

     * @throws ICrafterException if one or more of service type, name,
     * description, or machine name information is missing in the
     * initial parameters provided to the service in the config file
     **/
    public void initConfig(HashMap map) throws ICrafterException {
        try {
            this.map = map;
            svcName = getInitParameter("name");
            heapMachine = getInitParameter("heapMachine");
            initSDL();
            String hostName = getInitParameter("hostName");
            String description = getInitParameter("description");
            if (description == null) {
                description = getDescription();
            }
            String shortDescription = getInitParameter("shortDescription");
            if (shortDescription == null) {
                shortDescription = getShortDescription();
            }
            String syncModel = getInitParameter("syncModel");
            if (syncModel != null) {
                sync_model = Integer.parseInt(syncModel);
            }
            if (sync_model != EventHeapService.SINGLE_INSTANCE && sync_model != EventHeapService.SINGLE_THREAD && sync_model != EventHeapService.MULTI_THREAD) {
                throw new ICrafterException("Illegal value for sync_model " + "in the config file");
            }
            ss = (StateSpace) map.get("ss");
            sharedEventHeap = (EventHeap) map.get("eventHeap");
            init();
            SimpleLocalVariable hostVar = new SimpleLocalVariable(svcName, StateConstants.HOST, StateConstants.STRING, hostName);
            ss.register(hostVar);
            SimpleLocalVariable typeVar = new SimpleLocalVariable(svcName, StateConstants.TYPE, StateConstants.STRING, ICrafterConstants.ICRAFTERSERVICE);
            ss.register(typeVar);
            Class[] ifs = getAdvertizeInterfaces();
            String[] ifNames = new String[ifs.length];
            for (int i = 0; i < ifs.length; i++) {
                ifNames[i] = ifs[i].getName();
                Utils.debug("ICrafterService", "Advertised interface: " + ifNames[i]);
            }
            SimpleLocalVariable ifNamesVar = new SimpleLocalVariable(svcName, ICrafterConstants.INTERFACES, StateConstants.STRINGARRAY, ifNames);
            ss.register(ifNamesVar);
            SimpleLocalVariable descVar = new SimpleLocalVariable(svcName, StateConstants.DESCRIPTION, StateConstants.STRING, description);
            ss.register(descVar);
            SimpleLocalVariable shortDescVar = new SimpleLocalVariable(svcName, StateConstants.SHORTDESCRIPTION, StateConstants.STRING, shortDescription);
            ss.register(shortDescVar);
            SimpleLocalVariable sdlVar = new SimpleLocalVariable(svcName, ICrafterConstants.SDL, StateConstants.STRING, getSDL());
            ss.register(sdlVar);
            Utils.debug("ICrafterService", "Service " + svcName + " initted");
        } catch (ICrafterException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ICrafterException(e);
        }
    }

    protected StateSpace getStateSpace() {
        return ss;
    }

    /**
     * Prepares the private EventHeap for this service.  {@link #init} is
     * the recommended place to call this method.
     */
    protected void initEventHeap() throws ICrafterException {
        try {
            privateEventHeap = new EventHeap(sharedEventHeap, null);
        } catch (EventHeapException ex) {
            throw new ICrafterException(ex);
        }
    }

    /**
     * Returns the EventHeap for this service.  You have to call {@link #initEventHeap}
     * before calling this method.
     */
    protected EventHeap getEventHeap() {
        return privateEventHeap;
    }

    /**
     * Service-specific Initialization method. This method can be
     * overriden by subclasses of ICrafterService to perform any
     * service-specific initializations. If the initialization fails,
     * then ICrafterException should be thrown and the service will not
     * be advertized.
     *
     * @throws ICrafterException If initialization fails
     **/
    protected void init() throws ICrafterException {
    }

    /**
     * This method can be overriden by subclasses of ICrafterService
     * to return the interfaces to be advertized for this
     * service. The default implemention of this method returns all
     * the interfaces implemented by the class except
     * "EventHeapService".
     *
     **/
    protected Class[] getAdvertizeInterfaces() {
        Vector ifs = null;
        if (wrapper) ifs = getInterfaces(serviceObj.getClass()); else ifs = getInterfaces(this.getClass());
        int len = ifs.size();
        Class ehs = EventHeapService.class;
        Class multiThread = MultiThreadModel.class;
        Class singleThread = SingleThreadModel.class;
        Class singleInstance = SingleInstanceModel.class;
        for (int i = 0; i < ifs.size(); i++) {
            if (ehs.equals((Class) ifs.elementAt(i)) || multiThread.equals((Class) ifs.elementAt(i)) || singleThread.equals((Class) ifs.elementAt(i)) || singleInstance.equals((Class) ifs.elementAt(i))) {
                ifs.removeElementAt(i);
                break;
            }
        }
        return (Class[]) ifs.toArray(new Class[0]);
    }

    private Vector getInterfaces(Class cls) {
        Vector ifs = new Vector(10);
        Class[] classIFs = cls.getInterfaces();
        for (int i = 0; i < classIFs.length; i++) {
            ifs.add(classIFs[i]);
            Vector superIFs = getInterfaces(classIFs[i]);
            for (int j = 0; j < superIFs.size(); j++) {
                ifs.add(superIFs.elementAt(j));
            }
        }
        return ifs;
    }

    /**
     * This method can be overriden by subclasses of ICrafterService
     * to return a very short (<5 word) human understandable
     * description of this service (such as "Projector
     * controller"). The default implementation of this method just returns 
     * the classname (such as "icrafter.services.devices.ProjectorController")
     * 
     **/
    protected String getShortDescription() {
        if (wrapper) return serviceObj.getClass().getName(); else return this.getClass().getName();
    }

    /**
     * This method can be overriden by subclasses of ICrafterService
     * to return a human understandable description of this service --
     * for example, "This service allows control of lights. It allows
     * turning the lights on/off and dimming/brightening the lights". The 
     * default implementation just returns the classname (such as 
     * "icrafter.services.devices.ProjectorController").
     *    
     **/
    protected String getDescription() {
        if (wrapper) return serviceObj.getClass().getName(); else return this.getClass().getName();
    }

    /**
     * Returns the events this service wishes to subscribe to. The
     * default event to subscribe to is all events directed to this
     * service *name*. This method can be overridden by subclasses of
     * ICrafterService that need to subscribe to special types of events
     * @return Events to subscribe to on behalf of this service 
     * @throws ICrafterException
     */
    public Event[] getSubscribeEvents() throws ICrafterException {
        try {
            Class[] ifs = getAdvertizeInterfaces();
            Event[] sEvents = new Event[1 + ifs.length];
            for (int i = 0; i < ifs.length; i++) {
                sEvents[i] = new Event();
                sEvents[i].setFieldValue(Event.EVENTTYPE, ifs[i].getName());
                sEvents[i].addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.INTERFACE_OPERATION_EVENT_CLASS);
            }
            sEvents[ifs.length] = new Event();
            sEvents[ifs.length].setFieldValue(Event.EVENTTYPE, svcName);
            sEvents[ifs.length].addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.OPERATION_EVENT_CLASS);
            return sEvents;
        } catch (Exception e) {
            throw new ICrafterException(e);
        }
    }

    /** 
     * Initializes the maps that store SDL information SDL information
     * for this service is stored method-wise and parameter-wise in
     * two Maps: methodSDL and paramSDL respectively
     */
    private final void initSDL() {
        methodSDL = new HashMap();
        paramSDL = new HashMap();
    }

    /** 
     * This method should be overridden by subclasses to add SDL
     * information. By default, the SDL information is generated using
     * reflection. This information can be supplemented with
     * additional information about parameters and methods such as
     * method and parameter descriptions etc. Such additional
     * information can be added in the overridden addSDL() method. To
     * add SDL for methods and parameters, the methods {@link
     * #addMethodSDL} and {@link #addParameterSDL} can respectively be
     * used 
     * @throws IllegalSDLException 
     */
    protected void addSDL() throws IllegalSDLException {
    }

    /**
     * Callback method which handles an event directed to this
     * service. If the event is of type methodcall, the method name
     * and parameters are read from the event. Then the call is
     * dispatched to the appropriate method in the service object
     * using dynamic invocation.If the event expects a return, any
     * returns of the method are packaged in a return event and
     * returned to the caller. If the event is not of type methodcall,
     * then the method receivedNonOperationEvent is called {@link
     * #receivedNonOperationEvent}
     * @param evt The method call event
     * @param eh The Event Heap object that should be used for
     * submitting the return event. Note that we don't want to create
     * a fresh EventHeap reference every time an event is received.
     * @throws ICrafterException If there are errors while processing
     * the event 
     **/
    public void receivedEvent(Event evt, EventHeap eh) throws ICrafterException {
        try {
            Utils.debug("ICrafterService", evt.toString());
            if (!evt.fieldExists(ICrafterConstants.EVENTCLASS)) {
                receivedNonOperationEvent(evt, eh);
                return;
            }
            String evtClass = (String) evt.getPostValue(ICrafterConstants.EVENTCLASS);
            Utils.debug("ICrafterService", evtClass + " == " + ICrafterConstants.OPERATION_EVENT_CLASS);
            if (!evtClass.equals(ICrafterConstants.OPERATION_EVENT_CLASS) && !evtClass.equals(ICrafterConstants.INTERFACE_OPERATION_EVENT_CLASS)) {
                receivedNonOperationEvent(evt, eh);
                return;
            }
            Serializable ret = null;
            Utils.debug("ICrafterService", svcName + ": received an operation invocation: " + evt.toString());
            int callID = ((Integer) evt.getPostValue(ICrafterConstants.CALLID)).intValue();
            try {
                Class cls;
                if (wrapper) cls = serviceObj.getClass(); else cls = this.getClass();
                if (evtClass.equals(ICrafterConstants.INTERFACE_OPERATION_EVENT_CLASS)) {
                    int i = 0;
                    Vector mVars = new Vector();
                    while (true) {
                        if (!evt.fieldExists(ICrafterConstants.MATCHING_VARIABLE + i)) break;
                        MatchingVariable mVar = new MatchingVariable((String) evt.getPostValue(ICrafterConstants.MATCHING_VARIABLE + i));
                        mVars.add(mVar);
                        i++;
                    }
                    Utils.debug("ICrafterService", "mVars.size(): " + mVars.size());
                    MatchingVariable[] mVarArray = (MatchingVariable[]) mVars.toArray(new MatchingVariable[0]);
                    if (!ss.matchLocal(getName(), mVarArray)) {
                        Utils.debug("ICrafterService", "Mismatched interface call dropped!");
                        return;
                    }
                }
                String opName = (String) evt.getPostValue(ICrafterConstants.OPERATION_NAME);
                int numParams = 0;
                while (evt.fieldExists(ICrafterConstants.PARAM + numParams)) {
                    numParams++;
                }
                Class paramTypes[] = new Class[numParams];
                Serializable params[] = new Serializable[numParams];
                Hashtable rdataThreads = new Hashtable(5);
                boolean dataparam = false;
                Event rdataEvent = new Event();
                rdataEvent.setFieldValue(Event.EVENTTYPE, svcName);
                rdataEvent.addField(ICrafterConstants.EVENTCLASS, "DataReturnClass");
                rdataEvent.addField(ICrafterConstants.CALLID, new Integer(callID));
                for (int i = 0; i < numParams; i++) {
                    params[i] = (Serializable) evt.getPostValue(ICrafterConstants.PARAM + i);
                    paramTypes[i] = params[i].getClass();
                    Utils.debug("ICrafterService", svcName + ": opName: " + opName + " parameter " + i + " " + paramTypes[i].getName() + " " + params[i]);
                    boolean isrdata = false;
                    if (evt.fieldExists("RData" + i)) {
                        isrdata = ((Boolean) evt.getPostValue("RData" + i)).booleanValue();
                    }
                    if (!isrdata) continue;
                    dataparam = true;
                    String mc = java.net.InetAddress.getLocalHost().getHostName();
                    ServerSocket s = new ServerSocket(0);
                    int port = s.getLocalPort();
                    RDataThread rdt = new RDataThread(s, 300000);
                    rdt.start();
                    rdataThreads.put(new Integer(i), rdt);
                    rdataEvent.addField("RDataRet" + i, mc + ";" + port);
                }
                if (dataparam) {
                    Utils.debug("ICrafterService", "Dealing with rdata params");
                    eh.putEvent(rdataEvent);
                    for (int i = 0; i < numParams; i++) {
                        params[i] = (Serializable) evt.getPostValue(ICrafterConstants.PARAM + i);
                        if (!evt.fieldExists("RData" + i)) {
                            continue;
                        }
                        boolean isrdata = ((Boolean) evt.getPostValue("RData" + i)).booleanValue();
                        if (!isrdata) continue;
                        RDataThread rdt = (RDataThread) rdataThreads.get(new Integer(i));
                        rdt.join();
                        RData rdata = rdt.getRData();
                        params[i] = (Serializable) rdata;
                        paramTypes[i] = params[i].getClass();
                        Utils.debug("ICrafterService", svcName + ": opName: " + opName + " rdata parameter " + i + " " + paramTypes[i].getName() + " " + params[i]);
                    }
                }
                Utils.debug("ICrafterService", svcName + ": Making the dynamic invocation");
                Method[] meths = cls.getMethods();
                int i;
                for (i = 0; i < meths.length; i++) {
                    String name = meths[i].getName();
                    if (icsHashtable.get(name) != null) continue;
                    if (!name.equals(opName)) {
                        continue;
                    }
                    Utils.debug("ICrafterService", "Method with the same name found!");
                    Class[] mParamTypes = meths[i].getParameterTypes();
                    if (mParamTypes.length != paramTypes.length) continue;
                    boolean diff[] = new boolean[paramTypes.length];
                    for (int j = 0; j < paramTypes.length; j++) diff[j] = false;
                    int j;
                    for (j = 0; j < paramTypes.length; j++) {
                        if (mParamTypes[j].isAssignableFrom(paramTypes[j])) {
                            continue;
                        } else if (mParamTypes[j].getName().equals("int")) {
                            if (paramTypes[j].getName().equals("java.lang.Integer")) {
                                diff[j] = true;
                                continue;
                            } else {
                                Utils.debug("ICrafterService", "Expected type: " + mParamTypes[j].getName());
                                Utils.debug("ICrafterService", "Passed Type: " + paramTypes[j].getName());
                                break;
                            }
                        } else if (mParamTypes[j].getName().equals("float")) {
                            if (paramTypes[j].getName().equals("java.lang.Float")) {
                                diff[j] = true;
                                continue;
                            } else {
                                Utils.debug("ICrafterService", "Expected type: " + mParamTypes[j].getName());
                                Utils.debug("ICrafterService", "Passed Type: " + paramTypes[j].getName());
                                break;
                            }
                        } else if (mParamTypes[j].getName().equals("boolean")) {
                            if (paramTypes[j].getName().equals("java.lang.Boolean")) {
                                diff[j] = true;
                                continue;
                            } else {
                                Utils.debug("ICrafterService", "Expected type: " + mParamTypes[j].getName());
                                Utils.debug("ICrafterService", "Passed Type: " + paramTypes[j].getName());
                                break;
                            }
                        } else {
                            Utils.debug("ICrafterService", "Expected type: " + mParamTypes[j].getName());
                            Utils.debug("ICrafterService", "Passed Type: " + paramTypes[j].getName());
                            break;
                        }
                    }
                    if (j == paramTypes.length) {
                        break;
                    }
                }
                if (i == meths.length) {
                    throw new ICrafterException("No matching method found for the name " + opName + " and the given parameters");
                }
                long before = System.currentTimeMillis();
                if (wrapper) ret = (Serializable) meths[i].invoke(serviceObj, params); else ret = (Serializable) meths[i].invoke(this, params);
                long after = System.currentTimeMillis();
                long etime = after - before;
                Utils.debug("ICrafterService", "Before: " + before + " After: " + after + " Elapsed time: " + etime);
                Utils.debug("ICrafterService", "Sending return event: " + ret);
                Event retEvt = new Event();
                retEvt.setFieldValue(Event.TIMETOLIVE, new Integer(ICrafterConstants.EXPIRE_TIME));
                retEvt.setFieldValue(Event.EVENTTYPE, svcName);
                retEvt.addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.RETURN_EVENT_CLASS);
                retEvt.addField(ICrafterConstants.CALLID, new Integer(callID));
                boolean isrdata = false;
                if (ret != null) {
                    Class[] ifs = ret.getClass().getInterfaces();
                    for (int j = 0; j < ifs.length; j++) {
                        if (ifs[j].getName().equals("iwork.icrafter.system.RData")) isrdata = true;
                    }
                }
                if (isrdata) {
                    retEvt.addField(ICrafterConstants.RETURN_CODE, ICrafterConstants.RDATA_RETURN);
                    String mc = java.net.InetAddress.getLocalHost().getHostName();
                    ServerSocket s = new ServerSocket(0);
                    int port = s.getLocalPort();
                    RDataSenderThread rdt = new RDataSenderThread(s, 300000, (RData) ret);
                    rdt.start();
                    retEvt.addField(ICrafterConstants.RETURN_VALUE, mc + ";" + port);
                } else if (ret != null) {
                    retEvt.addField(ICrafterConstants.RETURN_CODE, ICrafterConstants.NO_ERROR_RETURN);
                    if (ret.getClass().equals(java.lang.Integer.TYPE)) {
                        retEvt.addField(ICrafterConstants.RETURN_VALUE, (Integer) ret);
                    } else if (ret.getClass().equals(java.lang.Float.TYPE)) {
                        retEvt.addField(ICrafterConstants.RETURN_VALUE, (Float) ret);
                    } else if (ret.getClass().equals(java.lang.Boolean.TYPE)) {
                        retEvt.addField(ICrafterConstants.RETURN_VALUE, (Boolean) ret);
                    } else {
                        retEvt.addField(ICrafterConstants.RETURN_VALUE, ret);
                    }
                } else {
                    retEvt.addField(ICrafterConstants.RETURN_CODE, ICrafterConstants.NULL_RETURN);
                }
                Utils.debug("ICrafterService", svcName + ": submitting return event " + retEvt.toString());
                eh.putEvent(retEvt);
            } catch (InvocationTargetException e) {
                Event retEvt = new Event();
                retEvt.setFieldValue(Event.TIMETOLIVE, new Integer(ICrafterConstants.EXPIRE_TIME));
                retEvt.setFieldValue(Event.EVENTTYPE, svcName);
                retEvt.addField(ICrafterConstants.EVENTCLASS, ICrafterConstants.RETURN_EVENT_CLASS);
                retEvt.addField(ICrafterConstants.CALLID, new Integer(callID));
                retEvt.addField(ICrafterConstants.RETURN_CODE, ICrafterConstants.ERROR_RETURN);
                StringWriter sw = new StringWriter();
                e.getTargetException().printStackTrace(new PrintWriter(sw));
                retEvt.addField(ICrafterConstants.RETURN_ERROR_MESSAGE, sw.toString());
                eh.putEvent(retEvt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ICrafterException(e);
        }
    }

    /**
     * Handles non-operation events [also called non-methodcall
     * events] directed at this service. Non-operation events are
     * events that don't follow the standard event encoding for
     * operation calls.  These events are primarily sent by legacy
     * applications. This method is just a placeholder and does
     * nothing. Services that expect to receive such (typically
     * legacy) non-operation events must override this method to
     * handle these events appropriately
     * @param evt The non-methodcall event
     * @param eh The Event Heap object over which this event was received
     **/
    protected synchronized void receivedNonOperationEvent(Event evt, EventHeap eh) throws ICrafterException {
        Utils.warning("ICrafterService", svcName + ": Non operation event not handled by the " + "service!! Dropped.");
    }

    /**
     * This method can be used to add SDL information for a method of
     * this service 
     * @param methodName Name of the method for which SDl info is being added
     * @param paramTypes Parameter types of the method for which SDL
     * info is being added. Note that both the method name and the
     * parameter types are needed to uniquely identify the method.
     * @param desc A human-readable description of the method
     * @throws IllegalSDLException 
     */
    protected void addMethodSDL(String methodName, Class[] paramTypes, String descr) throws IllegalSDLException {
        Method meth;
        try {
            meth = this.getClass().getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalSDLException("No such method");
        }
        methodSDL.put(meth, descr);
    }

    /**
     * This method can be used to add SDL information for a parameter
     * of this service. SDL information consists of a name,
     * description, type and associated type information
     * @param methodName Name of the method for whose parameter SDL
     * info is being added
     * @param paramTypes Parameters for the method containing the
     * parameter for which SDL info is being added. Note that a method
     * name and parameter types are both necessary to uniquely
     * identify a method
     * @param paramNum Identifies the parameter of the method (ranges
     * from 0..numParams - 1) for which the SDL info  is being added
     * @param name Meaningful name for the parameter
     * @param type SDL type of the parameter, such as int, string, etc
     * @param typeInfo Additional information, if any, associated with
     * the type, such as ranges for a parameter of type intrange etc
     * @param description A human-readable description of the parameter
     * @throws IllegalSDLException If the method or parameter cannot
     * be identified, if the type and typeInfo are not compatible, etc
     */
    protected void addParameterSDL(String methodName, Class[] paramTypes, int paramNum, String name, String type, Object typeInfo, String description) throws IllegalSDLException {
        Method meth;
        try {
            meth = this.getClass().getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalSDLException("Mo such method");
        }
        if (paramNum >= paramTypes.length) throw new IllegalSDLException("Parameter number " + paramNum + " illegal for method " + methodName);
        ParamSDL param = new ParamSDL();
        if (type.equals(StateConstants.INT)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.Integer") && !paramTypes[paramNum].getName().equals("int")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
        } else if (type.equals(ICrafterConstants.INTENUM)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.Integer") && !paramTypes[paramNum].getName().equals("int")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
            Integer[] itInfo;
            ;
            if (typeInfo instanceof int[]) {
                itInfo = new Integer[((int[]) typeInfo).length];
                for (int j = 0; j < itInfo.length; j++) {
                    itInfo[j] = new Integer(((int[]) typeInfo)[j]);
                }
            } else if (typeInfo instanceof Integer[]) {
                itInfo = (Integer[]) typeInfo;
            } else {
                throw new IllegalSDLException("Must supply Integer[] or int[] as type information for " + type);
            }
            typeInfo = itInfo;
        } else if (type.equals(ICrafterConstants.INTRANGE)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.Integer") && !paramTypes[paramNum].getName().equals("int")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
            Integer[] itInfo;
            ;
            if (typeInfo instanceof int[]) {
                itInfo = new Integer[((int[]) typeInfo).length];
                for (int j = 0; j < itInfo.length; j++) {
                    itInfo[j] = new Integer(((int[]) typeInfo)[j]);
                }
            } else if (typeInfo instanceof Integer[]) {
                itInfo = (Integer[]) typeInfo;
            } else {
                throw new IllegalSDLException("Must supply Integer[] or int[] as type information for " + type);
            }
            if (itInfo.length != 2) {
                throw new IllegalSDLException("Must supply Integer[] or int[] of size 2 as type " + "information for " + type);
            }
            typeInfo = itInfo;
        } else if (type.equals(StateConstants.STRING)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.String")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
        } else if (type.equals(ICrafterConstants.STRINGENUM)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.String")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
            if (!(typeInfo instanceof String[])) {
                throw new IllegalSDLException("Must supply String[] as type information for " + type);
            }
        } else if (type.equals(StateConstants.FLOAT)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.Float") && !paramTypes[paramNum].getName().equals("float")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
        } else if (type.equals(ICrafterConstants.FLOATENUM)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.Float") && !paramTypes[paramNum].getName().equals("float")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
            Float[] ftInfo;
            ;
            if (typeInfo instanceof float[]) {
                ftInfo = new Float[((float[]) typeInfo).length];
                for (int j = 0; j < ftInfo.length; j++) {
                    ftInfo[j] = new Float(((float[]) typeInfo)[j]);
                }
            } else if (typeInfo instanceof Float[]) {
                ftInfo = (Float[]) typeInfo;
            } else {
                throw new IllegalSDLException("Must supply Float[] or float[] as type information for " + type);
            }
            typeInfo = ftInfo;
        } else if (type.equals(ICrafterConstants.FLOATRANGE)) {
            if (!paramTypes[paramNum].getName().equals("java.lang.Float") && !paramTypes[paramNum].getName().equals("float")) {
                throw new IllegalSDLException("Can't declare type " + type + " for actual parameter type " + paramTypes[paramNum].getName());
            }
            Float[] ftInfo;
            ;
            if (typeInfo instanceof float[]) {
                ftInfo = new Float[((float[]) typeInfo).length];
                for (int j = 0; j < ftInfo.length; j++) {
                    ftInfo[j] = new Float(((float[]) typeInfo)[j]);
                }
            } else if (typeInfo instanceof Float[]) {
                ftInfo = (Float[]) typeInfo;
            } else {
                throw new IllegalSDLException("Must supply Float[] or float[] of size 2 as type " + "information for " + type);
            }
            if (ftInfo.length != 2) {
                throw new IllegalSDLException("Must supply Float[] or float[] of size 2 as type " + "information for " + type);
            }
            typeInfo = ftInfo;
        } else {
            throw new IllegalSDLException("Illegal type " + type);
        }
        param.setType(type);
        param.setName(name);
        param.setTypeInfo(typeInfo);
        param.setDescription(description);
        paramSDL.put(meth.toString() + paramNum, param);
    }

    /**
     * Returns the service description for this service
     * SDL is generated based on the information gathered from:
     * <ul>
     *  <li> reflection </li>
     *  <li> SDL information added thru the addSDL method </li>
     * </ul>
     * @return SDL for this service
     */
    protected String getSDL() throws IllegalSDLException {
        if (sdl != null) {
            return sdl;
        }
        addSDL();
        sdl = "<?xml version=\"1.0\"?>\n";
        sdl += "<sdl>\n";
        Class cls;
        if (wrapper) cls = serviceObj.getClass(); else cls = this.getClass();
        sdl += "<operations>\n";
        Method[] methods = cls.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            String mName = method.getName();
            if (icsHashtable.get(mName) != null) {
                continue;
            }
            if (mName.equals("init")) continue;
            String methodsdl = "";
            methodsdl += "<operation>";
            methodsdl += "<name>" + mName + "</name>\n";
            boolean unknownParam = false;
            String description = (String) methodSDL.get(method);
            if (description != null) {
                methodsdl += "<description>" + description + "</description>\n";
            } else {
                methodsdl += "<description></description>\n";
            }
            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes != null && paramTypes.length > 0) {
                methodsdl += "<parameters>\n";
                for (int j = 0; j < paramTypes.length; j++) {
                    methodsdl += "<parameter>";
                    ParamSDL param = (ParamSDL) paramSDL.get(method.toString() + j);
                    if (param == null) {
                        methodsdl += "<name>param" + j + "</name>";
                        String paramType = paramTypes[j].getName();
                        if (paramType.equals("java.lang.Integer")) {
                            methodsdl += "<type>" + StateConstants.INT + "</type>";
                        } else if (paramType.equals("java.lang.String")) {
                            methodsdl += "<type>" + StateConstants.STRING + "</type>";
                        } else if (paramType.equals("java.lang.Float")) {
                            methodsdl += "<type>" + StateConstants.FLOAT + "</type>";
                        } else if (paramType.equals("int")) {
                            methodsdl += "<type>" + StateConstants.INT + "</type>";
                        } else if (paramType.equals("float")) {
                            methodsdl += "<type>" + StateConstants.FLOAT + "</type>";
                        } else {
                            methodsdl += "<type>" + ICrafterConstants.JAVAPREFIX + paramType + "</type>";
                        }
                        methodsdl += "<description></description>";
                    } else {
                        methodsdl += "<name>" + param.getName() + "</name>";
                        methodsdl += "<type>" + param.getType();
                        if (param.isRange()) {
                            methodsdl += "<low>" + ((Object[]) param.getTypeInfo())[0] + "</low>";
                            methodsdl += "<high>" + ((Object[]) param.getTypeInfo())[1] + "</high>";
                        }
                        if (param.isEnum()) {
                            Object[] vals = (Object[]) param.getTypeInfo();
                            for (int k = 0; k < vals.length; k++) {
                                methodsdl += "<value>" + vals[k] + "</value>";
                            }
                        }
                        methodsdl += "</type>";
                        methodsdl += "<description>" + param.getDescription() + "</description>";
                    }
                    methodsdl += "</parameter>\n";
                }
                if (unknownParam) continue;
                methodsdl += "</parameters>\n";
            }
            Class retType = method.getReturnType();
            String retClsName = retType.getName();
            if (retClsName != null && !retClsName.equals("void")) {
                if (retClsName.equals("java.lang.Integer")) {
                    methodsdl += "<return>" + StateConstants.INT + "</return>\n";
                } else if (retClsName.equals("java.lang.String")) {
                    methodsdl += "<return>" + StateConstants.STRING + "</return>\n";
                } else if (retClsName.equals("java.lang.Float")) {
                    methodsdl += "<return>" + StateConstants.FLOAT + "</return>\n";
                } else if (retClsName.equals("float")) {
                    methodsdl += "<return>" + StateConstants.FLOAT + "</return>\n";
                } else if (retClsName.equals("int")) {
                    methodsdl += "<return>" + StateConstants.INT + "</return>\n";
                } else {
                }
            }
            methodsdl += "</operation>\n";
            sdl += methodsdl;
        }
        sdl += "</operations>\n";
        sdl += "</sdl>\n";
        return sdl;
    }
}

/**
 *  Stores SDL information associated with a parameter. SDL
 *  information for a parameter consists of:
 *  <ul>
 *   <li> name </li>
 *   <li> type </li>
 *   <li> associated type info (for eg. for type intrange, low and
 *   high are stored etc) </li>
 *   <li> description : human understandable description </li>
 * </ul> 
 **/
class ParamSDL {

    String type;

    String name;

    String description;

    Object typeInfo;

    boolean isEnum;

    boolean isRange;

    public ParamSDL() {
        type = null;
        name = null;
        description = null;
        typeInfo = null;
        isEnum = false;
        isRange = false;
    }

    public void setType(String type) {
        if (type.equals(ICrafterConstants.INTENUM) || type.equals(ICrafterConstants.STRINGENUM) || type.equals(ICrafterConstants.FLOATENUM)) {
            isEnum = true;
        }
        if (type.equals(ICrafterConstants.INTRANGE) || type.equals(ICrafterConstants.FLOATRANGE)) {
            isRange = true;
        }
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTypeInfo(Object typeInfo) {
        this.typeInfo = typeInfo;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Object getTypeInfo() {
        return typeInfo;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public boolean isRange() {
        return isRange;
    }
}

class RDataThread extends Thread {

    ServerSocket s;

    int to;

    RData data;

    public RDataThread(ServerSocket sock, int timeout) {
        s = sock;
        to = timeout;
    }

    public void run() {
        try {
            s.setSoTimeout(to);
            Socket newsock = s.accept();
            InputStream is = newsock.getInputStream();
            StreamData tmp = new StreamData(is);
            data = new ByteData(tmp.getDataBytes());
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public RData getRData() {
        return data;
    }
}

class RDataSenderThread extends Thread {

    ServerSocket s;

    int to;

    RData data;

    public RDataSenderThread(ServerSocket sock, int timeout, RData d) {
        s = sock;
        to = timeout;
        data = d;
    }

    public void run() {
        try {
            s.setSoTimeout(to);
            Socket newsock = s.accept();
            InputStream is = data.getDataStream();
            OutputStream os = newsock.getOutputStream();
            byte[] b = new byte[1000];
            int len = -1;
            while ((len = is.read(b)) != -1) os.write(b, 0, len);
            is.close();
            os.close();
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
