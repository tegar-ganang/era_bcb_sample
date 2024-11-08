package com.monad.homerun.app.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import com.monad.homerun.action.Binding;
import com.monad.homerun.action.Month;
import com.monad.homerun.action.Schedule;
import com.monad.homerun.action.Scheme;
import com.monad.homerun.action.Plan;
import com.monad.homerun.action.ReflexSet;
import com.monad.homerun.app.AppService;
import com.monad.homerun.base.DataObject;
import com.monad.homerun.config.ConfigContext;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.core.NoResourceException;
import com.monad.homerun.control.Control;
import com.monad.homerun.control.Panel;
import com.monad.homerun.event.Event;
import com.monad.homerun.event.Emitter;
import com.monad.homerun.filter.Filter;
import com.monad.homerun.filter.FilterTrace;
import com.monad.homerun.function.Function;
import com.monad.homerun.model.Model;
import com.monad.homerun.model.ModelStatus;
import com.monad.homerun.objmgt.ActionService;
import com.monad.homerun.objmgt.ObjectService;
import com.monad.homerun.object.Domain;
import com.monad.homerun.object.Instance;
import com.monad.homerun.object.Type;
import com.monad.homerun.util.LocalAddress;
import com.monad.homerun.model.scalar.ValueType;
import com.monad.homerun.view.Scene;
import com.monad.homerun.view.Album;
import com.monad.homerun.view.Snapshot;
import com.monad.homerun.rule.Rule;
import com.monad.homerun.rule.RuleTrace;
import com.monad.homerun.rule.Situation;
import com.monad.homerun.rule.TraceDescription;
import com.monad.homerun.rule.TraceContext;
import com.monad.homerun.modelmgt.ModelService;
import com.monad.homerun.msgmgt.MessageService;
import com.monad.homerun.wiring.Circuit;
import com.monad.homerun.message.Message;
import com.monad.homerun.message.Track;
import com.monad.homerun.rmictrl.AdminCtrl;
import com.monad.homerun.rmictrl.AppCtrl;
import com.monad.homerun.rmictrl.ActionCtrl;
import com.monad.homerun.rmictrl.LogCtrl;
import com.monad.homerun.rmictrl.MessageCtrl;
import com.monad.homerun.rmictrl.ModelCtrl;
import com.monad.homerun.rmictrl.ObjectCtrl;
import com.monad.homerun.rmictrl.PlanCtrl;
import com.monad.homerun.rmictrl.Registrar;
import com.monad.homerun.rmictrl.SceneCtrl;
import com.monad.homerun.rmictrl.ScheduleCtrl;
import com.monad.homerun.rmictrl.SchemeCtrl;
import com.monad.homerun.rmictrl.ServerCtrl;
import com.monad.homerun.rmictrl.TransportCtrl;
import com.monad.homerun.svrd.SvrdService;
import com.monad.homerun.app.TransportMonitor;

/**
 * Singleton class that manages RMI client applications, mainly
 * by dispensing RMI stubs, factory-style.
 */
public class RMIMonitor {

    private static RMIMonitor monInst = null;

    private ConfigContext appsCtx = null;

    Logger sysLogger = Activator.logSvc.getLogger();

    Logger actLogger = Activator.logSvc.getLogger("activity");

    private Registry registry = null;

    private RMIClassServer classServer = null;

    private SessionMonitor sessMon = null;

    public static synchronized RMIMonitor getInstance() {
        if (monInst == null) {
            monInst = new RMIMonitor();
        }
        return monInst;
    }

    private RMIMonitor() {
        sessMon = SessionMonitor.getInstance();
        try {
            appsCtx = Activator.cfgSvc.getContext("platform/app", "apps");
        } catch (IOException ioe) {
            sysLogger.log(Level.SEVERE, "Error: " + ioe);
            if (GlobalProps.DEBUG) {
                ioe.printStackTrace();
            }
        }
    }

    protected void bindServer() {
        try {
            if (registry == null) {
                Properties svcProps = Activator.cfgSvc.getProperties("service");
                InetAddress addr = LocalAddress.getLocalAddress();
                int csPort = Integer.parseInt(svcProps.getProperty("svr.port").trim());
                classServer = new RMIClassServer(csPort);
                String cbase = "http://" + addr.getHostAddress() + ":" + csPort + "/";
                System.getProperties().put("java.rmi.server.codebase", cbase);
                System.getProperties().put("java.rmi.server.hostname", addr.getHostAddress());
                if (GlobalProps.DEBUG) {
                    System.out.println("rmi codebase: " + System.getProperty("java.rmi.server.codebase"));
                    System.out.println("security: " + System.getProperty("java.security.policy"));
                    System.out.println("Check Permission");
                }
                int regPort = Integer.parseInt(svcProps.getProperty("rmi.port").trim());
                try {
                    registry = LocateRegistry.getRegistry(regPort);
                    registry.list();
                } catch (AccessException ae) {
                    registry = null;
                    if (GlobalProps.DEBUG) {
                        System.out.println("Local RMI registry access exception");
                    }
                } catch (RemoteException re) {
                    registry = null;
                    if (GlobalProps.DEBUG) {
                        System.out.println("Local RMI registry remote exception");
                    }
                }
                if (registry == null) {
                    registry = LocateRegistry.createRegistry(regPort);
                }
            }
            registry.rebind("HomeRun", new ServerRegistrar());
            if (GlobalProps.DEBUG) {
                System.out.println("Bound: " + registry.list()[0]);
            }
            SvrdService regSvc = Activator.getRegService();
            if (regSvc != null) {
                regSvc.registerService(GlobalProps.RMI_SVC_TAG);
            }
        } catch (Exception e) {
            sysLogger.log(Level.SEVERE, "Error: " + e);
            if (GlobalProps.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    protected void unbindServer() {
        if (registry != null) {
            try {
                registry.unbind("HomeRun");
                SvrdService regSvc = Activator.getRegService();
                if (regSvc != null) {
                    regSvc.unregisterService(GlobalProps.RMI_SVC_TAG);
                }
            } catch (Exception e) {
                sysLogger.log(Level.SEVERE, "Error: " + e);
                if (GlobalProps.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        if (classServer != null) {
            classServer.shutdown();
        }
    }

    public String registerApp(String appName, String userName) {
        String sessionID = Registrar.REG_FAIL;
        ConfigContext appCtx = appsCtx.getFeature(appName);
        if (appCtx.isEmpty()) {
            sysLogger.log(Level.WARNING, "Unknown RMI application:" + appName);
        } else if (!appCtx.isEnabled()) {
            sysLogger.log(Level.WARNING, "Disabled RMI application:" + appName);
        } else {
            sessionID = sessMon.createSession(appName, userName);
        }
        return sessionID;
    }

    public ServerCtrl getControl(String sessionID, String controlName) throws RemoteException, NoResourceException {
        if (controlName.equals("admin")) {
            return new RemAdminCtrl(sessionID);
        } else if (controlName.equals("app")) {
            return new RemAppCtrl(sessionID);
        } else if (controlName.equals("log")) {
            return new RemLogCtrl(sessionID);
        } else if (controlName.equals("message")) {
            return new RemMessageCtrl(sessionID);
        } else if (controlName.equals("model")) {
            return new RemModelCtrl(sessionID);
        } else if (controlName.equals("object")) {
            return new RemObjectCtrl(sessionID);
        } else if (controlName.equals("rule")) {
            return new RemRuleCtrl(sessionID);
        } else if (controlName.equals("scene")) {
            return new RemSceneCtrl(sessionID);
        } else if (controlName.equals("schedule")) {
            return new RemScheduleCtrl(sessionID);
        } else if (controlName.equals("plan")) {
            return new RemPlanCtrl(sessionID);
        } else if (controlName.equals("scheme")) {
            return new RemSchemeCtrl(sessionID);
        } else if (controlName.equals("x10")) {
            return new RemTransportCtrl(sessionID, "x10");
        }
        return null;
    }

    public void unregisterApp(String sessionID) throws RemoteException {
        sessMon.removeSession(sessionID);
    }

    public ConfigContext getAppConfig(String appName) {
        return appsCtx.getFeature(appName);
    }

    private class RemCtrl extends UnicastRemoteObject {

        private static final long serialVersionUID = -7689756498978370870L;

        private String sessionID = null;

        public RemCtrl(String sessionID) throws RemoteException {
            this.sessionID = sessionID;
        }

        public String getSessionID() {
            return sessionID;
        }
    }

    private class RemAdminCtrl extends RemCtrl implements AdminCtrl {

        private static final long serialVersionUID = -565496642430504955L;

        public RemAdminCtrl(String sessionID) throws RemoteException {
            super(sessionID);
        }

        public String[] getConfigFeatures(String configPath, boolean enabledOnly) throws RemoteException {
            return null;
        }
    }

    private class RemAppCtrl extends RemCtrl implements AppCtrl {

        private static final long serialVersionUID = -533316170789705819L;

        private AppService appMan = null;

        public RemAppCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            appMan = Activator.appSvc;
            if (appMan == null) {
                throw new NoResourceException("AppService");
            }
        }

        public String[] getConfigFeatures(String configPath, boolean enabledOnly) throws RemoteException {
            return appMan.getConfigFeatures(configPath, enabledOnly);
        }

        public String[] getReachableApps(String userName) throws RemoteException {
            return appMan.getReachableApps(userName);
        }

        public String[] getUserNames() throws RemoteException {
            return appMan.getUserNames();
        }

        public String describeUser(String userName) throws RemoteException {
            return appMan.describeUser(userName);
        }

        public boolean updateUser(String userName, String[] roleList) throws RemoteException {
            return appMan.updateUser(userName, roleList);
        }

        public boolean removeUser(String userName) throws RemoteException {
            return appMan.removeUser(userName);
        }
    }

    private class RemScheduleCtrl extends RemCtrl implements ScheduleCtrl {

        private static final long serialVersionUID = -6978897855748125952L;

        private ActionService actMan = null;

        public RemScheduleCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            actMan = Activator.actionSvc;
            if (actMan == null) {
                throw new NoResourceException("ActionService");
            }
        }

        public String[] getScheduleNames() throws RemoteException {
            return actMan.getScheduleNames();
        }

        public Schedule getSchedule(String scheduleName) throws RemoteException {
            return actMan.getSchedule(scheduleName);
        }

        public String getActiveScheduleName() throws RemoteException {
            return actMan.getActiveScheduleName();
        }

        public boolean updateSchedule(Schedule schedule) throws RemoteException {
            logActivity(getSessionID(), "updating schedule", schedule);
            return actMan.updateSchedule(schedule);
        }

        public boolean addSchedule(Schedule schedule) throws RemoteException {
            logActivity(getSessionID(), "adding schedule", schedule);
            return actMan.addSchedule(schedule);
        }

        public boolean removeSchedule(String scheduleName) throws RemoteException {
            Schedule schedule = new Schedule(scheduleName);
            logActivity(getSessionID(), "removing schedule", schedule);
            return actMan.removeSchedule(scheduleName);
        }

        public boolean startSchedule(String scheduleName) throws RemoteException {
            Schedule schedule = new Schedule(scheduleName);
            logActivity(getSessionID(), "starting schedule", schedule);
            return actMan.startSchedule(scheduleName);
        }

        public void stopSchedule() throws RemoteException {
            actMan.stopSchedule();
        }

        public Month getMonth(String monthName) throws RemoteException {
            return actMan.getMonth(monthName);
        }

        public boolean addMonth(Month month) throws RemoteException {
            return actMan.addMonth(month);
        }

        public boolean updateMonth(Month month) throws RemoteException {
            return actMan.updateMonth(month);
        }

        public boolean removeMonth(Month month) throws RemoteException {
            return actMan.removeMonth(month);
        }
    }

    private class RemSceneCtrl extends RemCtrl implements SceneCtrl {

        private static final long serialVersionUID = 7781683340528120279L;

        private ModelService modelMan = null;

        public RemSceneCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            modelMan = Activator.modelSvc;
            if (modelMan == null) {
                throw new NoResourceException("ModelManager");
            }
        }

        public String[] getSceneCategories() throws RemoteException {
            return modelMan.getSceneCategories();
        }

        public String[] getSceneNames(String category) throws RemoteException {
            return modelMan.getSceneNames(category);
        }

        public Scene getScene(String category, String sceneName) throws RemoteException {
            return modelMan.getScene(category, sceneName);
        }

        public boolean updateScene(Scene scene) throws RemoteException {
            logActivity(getSessionID(), "updating scene", scene);
            return modelMan.updateScene(scene);
        }

        public boolean addScene(Scene scene) throws RemoteException {
            logActivity(getSessionID(), "adding scene", scene);
            return modelMan.addScene(scene);
        }

        public boolean removeScene(Scene scene) throws RemoteException {
            logActivity(getSessionID(), "removing scene", scene);
            return modelMan.removeScene(scene);
        }

        public void captureScene(String category, String sceneName) throws RemoteException {
            modelMan.captureScene(category, sceneName);
        }

        public long[] getSnapshotTimes(String category, String sceneName) throws RemoteException {
            return modelMan.getSnapshotTimes(category, sceneName);
        }

        public Snapshot getSnapshot(String category, String sceneName, long time) throws RemoteException {
            return modelMan.getSnapshot(category, sceneName, time);
        }

        public String[] getViewTypes() throws RemoteException {
            return modelMan.getViewTypes();
        }

        public byte[] getModelTrail(String domain, String object, String model, String options) throws RemoteException {
            return modelMan.getModelTrail(domain, object, model, options);
        }

        public String[] getImageNames(String category) throws RemoteException {
            return modelMan.getImageNames(category);
        }

        public byte[] getImageBytes(String category, String imageName) throws RemoteException {
            return modelMan.getImageBytes(category, imageName);
        }

        public String[] getAlbumNames() throws RemoteException {
            return modelMan.getAlbumNames();
        }

        public Album getAlbum(String albumName) throws RemoteException {
            return modelMan.getAlbum(albumName);
        }

        public boolean updateAlbum(Album album) throws RemoteException {
            logActivity(getSessionID(), "updating album", album);
            return modelMan.updateAlbum(album);
        }

        public boolean addAlbum(Album album) throws RemoteException {
            logActivity(getSessionID(), "adding album", album);
            return modelMan.addAlbum(album);
        }

        public boolean removeAlbum(Album album) throws RemoteException {
            logActivity(getSessionID(), "removing album", album);
            return modelMan.removeAlbum(album);
        }
    }

    private class RemPlanCtrl extends RemCtrl implements PlanCtrl {

        private static final long serialVersionUID = 310647545614070629L;

        private ActionService eventMan = null;

        public RemPlanCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            eventMan = Activator.actionSvc;
            if (eventMan == null) {
                throw new NoResourceException("ActionService");
            }
        }

        public String[] getPlanNames() throws RemoteException {
            return eventMan.getPlanNames();
        }

        public Plan getPlan(String planName) throws RemoteException {
            return eventMan.getPlan(planName);
        }

        public String[] getActivePlanNames() throws RemoteException {
            return eventMan.getActivePlanNames();
        }

        public boolean updatePlan(Plan plan) throws RemoteException {
            logActivity(getSessionID(), "updating plan", plan);
            return eventMan.updatePlan(plan);
        }

        public boolean addPlan(Plan plan) throws RemoteException {
            logActivity(getSessionID(), "adding plan", plan);
            return eventMan.addPlan(plan);
        }

        public boolean removePlan(String planName) throws RemoteException {
            return (eventMan != null) ? eventMan.removePlan(planName) : false;
        }

        public boolean runPlan(String planName) throws RemoteException {
            return (eventMan != null) ? eventMan.runPlan(planName) : false;
        }

        public void stopPlan(String planName) throws RemoteException {
            if (eventMan != null) eventMan.stopPlan(planName);
        }
    }

    private class RemSchemeCtrl extends RemCtrl implements SchemeCtrl {

        private static final long serialVersionUID = 310647545614070629L;

        private ActionService actionMan = null;

        public RemSchemeCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            actionMan = Activator.actionSvc;
            if (actionMan == null) {
                throw new NoResourceException("ActionService");
            }
        }

        public String[] getSchemeNames() throws RemoteException {
            return actionMan.getSchemeNames();
        }

        public Scheme getScheme(String schemeName) throws RemoteException {
            return actionMan.getScheme(schemeName);
        }

        public String[] getActiveSchemeNames() throws RemoteException {
            return actionMan.getActiveSchemeNames();
        }

        public boolean updateScheme(Scheme scheme) throws RemoteException {
            logActivity(getSessionID(), "updating scheme", scheme);
            return actionMan.updateScheme(scheme);
        }

        public boolean addScheme(Scheme scheme) throws RemoteException {
            logActivity(getSessionID(), "adding scheme", scheme);
            return actionMan.addScheme(scheme);
        }

        public boolean removeScheme(String schemeName) throws RemoteException {
            return actionMan.removeScheme(schemeName);
        }

        public boolean runScheme(String schemeName) throws RemoteException {
            return actionMan.runScheme(schemeName);
        }

        public void stopScheme(String schemeName) throws RemoteException {
            actionMan.stopScheme(schemeName);
        }
    }

    private class RemModelCtrl extends RemCtrl implements ModelCtrl {

        private static final long serialVersionUID = -1292761783488333528L;

        private ModelService modelMan = null;

        private ObjectService objMan = null;

        public RemModelCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            modelMan = Activator.modelSvc;
            if (modelMan == null) {
                throw new NoResourceException("ModelService");
            }
            objMan = Activator.objectSvc;
            if (objMan == null) {
                throw new NoResourceException("ObjectService");
            }
        }

        public String[] getModelNames(String domain) throws RemoteException {
            return modelMan.getModelNames(domain);
        }

        public Model getModel(String domain, String modelName) throws RemoteException {
            return modelMan.getModel(domain, modelName);
        }

        public String[] getValueTypeNames() throws RemoteException {
            return modelMan.getValueTypeNames();
        }

        public ValueType getValueType(String typeName) throws RemoteException {
            return modelMan.getValueType(typeName);
        }

        public ModelStatus getModelStatus(String domain, String bearerName, String modelName) throws RemoteException {
            return objMan.getModelStatus(domain, bearerName, modelName);
        }

        public Emitter[] getInformers(String domain, String modelName) throws RemoteException {
            return modelMan.getInformers(domain, modelName);
        }

        public Emitter canInform(String informerName, String domain, String modelName) throws RemoteException {
            return modelMan.canInform(informerName, domain, modelName);
        }

        public void informModel(String domain, String bearerName, String modelName, Event event) throws RemoteException {
            modelMan.informModel(domain, bearerName, modelName, event);
        }

        public boolean addModel(Model model) throws RemoteException {
            return modelMan.addModel(model);
        }

        public boolean updateModel(Model model) throws RemoteException {
            return modelMan.updateModel(model);
        }

        public boolean removeModel(Model model) throws RemoteException {
            return modelMan.removeModel(model);
        }

        public String[] getModelTypes(String domain, boolean activeOnly) throws RemoteException {
            return modelMan.getModelTypes(domain, activeOnly);
        }

        public String[] getModelsOfType(String domain, String type) throws RemoteException {
            return modelMan.getModelsOfType(domain, type);
        }

        public String[] getFunctionNames() {
            return modelMan.getFunctionNames();
        }

        public Function getFunction(String functionName) {
            return modelMan.getFunction(functionName);
        }
    }

    private class RemMessageCtrl extends RemCtrl implements MessageCtrl {

        private static final long serialVersionUID = -3613924337330664312L;

        private MessageService msgSvc = null;

        public RemMessageCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            msgSvc = Activator.msgSvc;
            if (msgSvc == null) {
                throw new NoResourceException("MessageManager");
            }
        }

        public String[] getMessageBoards(String reader) throws RemoteException {
            return msgSvc.getMessageBoards(reader);
        }

        public String[] getReaderNames() throws RemoteException {
            return msgSvc.getReaderNames();
        }

        public String[] getRecipientNames() throws RemoteException {
            return msgSvc.getRecipientNames();
        }

        public int getNumMessages(String from) throws RemoteException {
            return msgSvc.getNumMessages(from);
        }

        public Message getMessage(String msgKey, boolean next, String recip) throws RemoteException {
            return msgSvc.getMessage(msgKey, next, recip);
        }

        public boolean postMessage(Message msg) throws RemoteException {
            return msgSvc.postMessage(msg);
        }

        public boolean ackMessage(String msgKey, String reader, String type) throws RemoteException {
            return msgSvc.ackMessage(msgKey, reader, type);
        }

        public Track getTrack(String msgKey) throws RemoteException {
            return msgSvc.getTrack(msgKey);
        }
    }

    private class RemObjectCtrl extends RemCtrl implements ObjectCtrl {

        private static final long serialVersionUID = -436725464068087690L;

        private ObjectService objMan = null;

        public RemObjectCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            objMan = Activator.objectSvc;
            if (objMan == null) {
                throw new NoResourceException("ObjectService");
            }
        }

        public boolean domainExists(String domainName) throws RemoteException {
            return objMan.domainExists(domainName);
        }

        public Domain[] getDomains(int filter) throws RemoteException {
            return objMan.getDomains(filter);
        }

        public Domain getDomain(String domain, int catFilter) throws RemoteException {
            return objMan.getDomain(domain, catFilter);
        }

        public String[] getTypeNames(String domainName) throws RemoteException {
            return objMan.getTypeNames(domainName);
        }

        public Type[] getTypes(String domainName) throws RemoteException {
            return objMan.getTypes(domainName);
        }

        public Type[] getCategoryTypes(String domainName, String category) throws RemoteException {
            return objMan.getCategoryTypes(domainName, category);
        }

        public Type getType(String domainName, String className) throws RemoteException {
            return objMan.getType(domainName, className);
        }

        public boolean removeType(Type type) throws RemoteException {
            return objMan.removeType(type);
        }

        public boolean objectExists(String domainName, String objectName) throws RemoteException {
            return objMan.objectExists(domainName, objectName);
        }

        public String[] getIndexKeys(String domainName, String indexName) throws RemoteException {
            return objMan.getIndexKeys(domainName, indexName);
        }

        public String[] getObjectNames(String domainName) throws RemoteException {
            return objMan.getObjectNames(domainName);
        }

        public String[] getObjectNames(String domainName, String indexName, String indexKey) throws RemoteException {
            return objMan.getObjectNames(domainName, indexName, indexKey);
        }

        public Properties getObjectProperties(String domainName, String objectName) throws RemoteException {
            return objMan.getObjectProperties(domainName, objectName);
        }

        public String[] getBearersOfModel(String domain, String modelName) throws RemoteException {
            return objMan.getBearersOfModel(domain, modelName);
        }

        public String[] getIconNames(String category) throws RemoteException {
            return getIconNamesInternal(category);
        }

        public byte[] getIconBytes(String category, String iconName) throws RemoteException {
            return getIconBytesInternal(category, iconName);
        }

        public Instance getObject(String domainName, String objectName) throws RemoteException {
            return objMan.getObject(domainName, objectName);
        }

        public Control[] getControlsForType(String type) throws RemoteException {
            return objMan.getControlsForType(type);
        }

        public Control getControl(String controlName) throws RemoteException {
            return objMan.getControl(controlName);
        }

        public String[] getEmittersForType(String type) throws RemoteException {
            return objMan.getEmittersForType(type);
        }

        public Emitter getEmitter(String emitterName) throws RemoteException {
            return objMan.getEmitter(emitterName);
        }

        public boolean addObject(Instance instance) throws RemoteException {
            logActivity(getSessionID(), "adding object", instance);
            return objMan.addObject(instance);
        }

        public boolean updateObject(Instance instance) throws RemoteException {
            logActivity(getSessionID(), "updating object", instance);
            return objMan.updateObject(instance);
        }

        public boolean removeObject(Instance instance) throws RemoteException {
            logActivity(getSessionID(), "removing object", instance);
            return objMan.removeObject(instance);
        }

        public boolean controlObject(String domainName, String objectName, String controlName, String command, Map modifiers) throws RemoteException {
            objMan.recordActivity(domainName, objectName);
            Map<String, String> context = new HashMap<String, String>();
            context.put("agent", "rmiuser");
            return objMan.controlObject(domainName, objectName, controlName, command, modifiers, context);
        }

        public String[] getFilterNames(String domainName) throws RemoteException {
            return objMan.getFilterNames(domainName);
        }

        public Filter getFilter(String domainName, String filterName) throws RemoteException {
            return objMan.getFilter(domainName, filterName);
        }

        public String[] applyFilter(Filter filter) throws RemoteException {
            return objMan.applyFilter(filter);
        }

        public String[] applyFilter(String domainName, String filterName, String type) throws RemoteException {
            return objMan.applyFilter(domainName, filterName, type);
        }

        public boolean applyFilter(Filter filter, String objectName) throws RemoteException {
            return objMan.applyFilter(filter, objectName);
        }

        public boolean validateFilter(Filter filter) throws RemoteException {
            return objMan.validateFilter(filter);
        }

        public String[] getFilterIComps(Filter filter) throws RemoteException {
            return objMan.getFilterIComps(filter);
        }

        public FilterTrace traceFilter(Filter filter, String objectName, String mode) throws RemoteException {
            return objMan.traceFilter(filter, objectName, mode);
        }

        public boolean addFilter(Filter filter) throws RemoteException {
            logActivity(getSessionID(), "adding filter", filter);
            return objMan.addFilter(filter);
        }

        public boolean updateFilter(Filter filter) throws RemoteException {
            logActivity(getSessionID(), "updating filter", filter);
            return objMan.updateFilter(filter);
        }

        public boolean removeFilter(Filter filter) throws RemoteException {
            logActivity(getSessionID(), "removing filter", filter);
            return objMan.removeFilter(filter);
        }

        public String[] getPanelCategories() throws RemoteException {
            return objMan.getPanelCategories();
        }

        public String[] getPanelNames(String category) throws RemoteException {
            return objMan.getPanelNames(category);
        }

        public Panel getPanel(String category, String panelName) throws RemoteException {
            return objMan.getPanel(category, panelName);
        }

        public boolean addPanel(Panel panel) throws RemoteException {
            logActivity(getSessionID(), "adding panel", panel);
            return objMan.addPanel(panel);
        }

        public boolean updatePanel(Panel panel) throws RemoteException {
            logActivity(getSessionID(), "updating panel", panel);
            return objMan.updatePanel(panel);
        }

        public boolean removePanel(Panel panel) throws RemoteException {
            logActivity(getSessionID(), "removing panel", panel);
            return objMan.removePanel(panel);
        }

        public ModelStatus getModelStatus(String domain, String objectName, String modelName) throws RemoteException {
            return objMan.getModelStatus(domain, objectName, modelName);
        }

        public String[] getCircuitNames() throws RemoteException {
            return objMan.getCircuitNames();
        }

        public Circuit getCircuit(String circuitName) throws RemoteException {
            return objMan.getCircuit(circuitName);
        }

        public boolean updateCircuit(Circuit circuit) throws RemoteException {
            logActivity(getSessionID(), "updating circuit", circuit);
            return objMan.updateCircuit(circuit);
        }

        public boolean addCircuit(Circuit circuit) throws RemoteException {
            logActivity(getSessionID(), "adding circuit", circuit);
            return objMan.addCircuit(circuit);
        }

        public boolean removeCircuit(String circuitName) throws RemoteException {
            return objMan.removeCircuit(circuitName);
        }

        public ReflexSet getReflexSet(String reflexSetName) throws RemoteException {
            return objMan.getReflexSet(reflexSetName);
        }

        public boolean updateReflexSet(ReflexSet reflexSet) throws RemoteException {
            logActivity(getSessionID(), "updating reflexSet", reflexSet);
            return objMan.updateReflexSet(reflexSet);
        }

        public boolean addReflexSet(ReflexSet reflexSet) throws RemoteException {
            logActivity(getSessionID(), "adding reflexSet", reflexSet);
            return objMan.addReflexSet(reflexSet);
        }

        public boolean removeReflexSet(String reflexSetName) throws RemoteException {
            return objMan.removeReflexSet(reflexSetName);
        }
    }

    private class RemRuleCtrl extends RemCtrl implements ActionCtrl {

        private static final long serialVersionUID = 6855867385073124086L;

        private ActionService actionSvc = null;

        public RemRuleCtrl(String sessionID) throws RemoteException, NoResourceException {
            super(sessionID);
            actionSvc = Activator.actionSvc;
            if (actionSvc == null) {
                throw new NoResourceException("ActionService");
            }
        }

        public String[] getRuleCategories() throws RemoteException {
            return actionSvc.getRuleCategories();
        }

        public String[] getRuleNames(String category) throws RemoteException {
            return actionSvc.getRuleNames(category);
        }

        public boolean ruleExists(String ruleName) throws RemoteException {
            return actionSvc.ruleExists(ruleName);
        }

        public boolean isRuleStartable(String category, String ruleName) throws RemoteException {
            return actionSvc.isRuleStartable(category, ruleName);
        }

        public String[] getRuleBindingVariables(String category, String ruleName) throws RemoteException {
            return actionSvc.getRuleBindingVariables(category, ruleName);
        }

        public Rule getRule(String category, String ruleName) throws RemoteException {
            return actionSvc.getRule(category, ruleName);
        }

        public boolean applyBoundRule(Binding binding) throws RemoteException {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", "apply");
            context.put("bindingProps", binding.getProperties());
            return actionSvc.applyRule(binding.getActionCategory(), binding.getActionName(), context);
        }

        public boolean applyRule(Rule rule, String mode) throws RemoteException {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("mode", mode);
            return actionSvc.applyRule(rule, context);
        }

        public RuleTrace traceRule(Rule rule, RuleTrace trace) throws RemoteException {
            return actionSvc.traceRule(rule, trace);
        }

        public TraceDescription[] getTraceDescriptions() throws RemoteException {
            return actionSvc.getTraceDescriptions();
        }

        public TraceContext getTraceContext(String contextID) throws RemoteException {
            return actionSvc.getTraceContext(contextID);
        }

        public boolean addRule(Rule rule) throws RemoteException {
            logActivity(getSessionID(), "adding rule", rule);
            return actionSvc.addRule(rule);
        }

        public boolean updateRule(Rule rule) throws RemoteException {
            logActivity(getSessionID(), "updating rule", rule);
            return actionSvc.updateRule(rule);
        }

        public boolean removeRule(String category, String ruleName) throws RemoteException {
            return actionSvc.removeRule(category, ruleName);
        }

        public String[] getSituationCategories() throws RemoteException {
            return actionSvc.getSituationCategories();
        }

        public String[] getSituationNames(String category) throws RemoteException {
            return actionSvc.getSituationNames(category);
        }

        public boolean situationExists(String situationName) throws RemoteException {
            return actionSvc.situationExists(situationName);
        }

        public Situation getSituation(String category, String situationName) throws RemoteException {
            return actionSvc.getSituation(category, situationName);
        }

        public boolean evalSituation(Situation situation, String mode) {
            return actionSvc.evalSituation(situation, mode);
        }

        public RuleTrace traceSituation(Situation situation, RuleTrace trace) {
            return actionSvc.traceSituation(situation, trace);
        }

        public boolean addSituation(Situation situation) throws RemoteException {
            logActivity(getSessionID(), "adding situation", situation);
            return actionSvc.addSituation(situation);
        }

        public boolean updateSituation(Situation situation) throws RemoteException {
            logActivity(getSessionID(), "updating situation", situation);
            return actionSvc.updateSituation(situation);
        }

        public boolean removeSituation(String category, String situationName) throws RemoteException {
            return actionSvc.removeSituation(category, situationName);
        }
    }

    private class RemLogCtrl extends RemCtrl implements LogCtrl {

        private static final long serialVersionUID = 324943259258881372L;

        private Logger logger = null;

        public RemLogCtrl(String sessionID) throws RemoteException {
            super(sessionID);
            logger = Activator.logSvc.getLogger();
        }

        public void log(Level level, String msg) throws RemoteException {
            logger.log(level, msg);
        }
    }

    private class RemTransportCtrl extends RemCtrl implements TransportCtrl {

        private static final long serialVersionUID = -97048964418453635L;

        private TransportMonitor monitor = null;

        public RemTransportCtrl(String sessionID, String transport) throws RemoteException, NoResourceException {
            super(sessionID);
            monitor = Activator.getTransportMonitor(transport);
            if (monitor == null) {
                throw new NoResourceException("No " + transport + " Monitor");
            }
        }

        public void listen() throws RemoteException {
            monitor.listen();
        }

        public void stop() throws RemoteException {
            monitor.stop();
        }

        public int getNumberEvents() throws RemoteException {
            return monitor.getNumberEvents();
        }

        public String getNextEvent() throws RemoteException {
            return monitor.getNextEvent();
        }

        public void transmit(String[] codes) throws RemoteException {
            try {
                monitor.transmit(codes);
            } catch (IOException ioE) {
                throw new RemoteException("IOException");
            }
        }
    }

    private void logActivity(String sessionID, String verb, DataObject object) {
        String logMsg = "'" + sessMon.getSessionUser(sessionID) + "' " + verb + " '" + object.getName() + "'";
        actLogger.log(Level.INFO, logMsg);
    }

    private InputStream getClassBytes(String className) {
        return this.getClass().getClassLoader().getResourceAsStream(className);
    }

    public String[] getIconNamesInternal(String category) {
        String[] iconList = new String[0];
        String path = GlobalProps.getHomeDir() + File.separator + "icons" + File.separator + category;
        File iconDir = new File(path);
        if (iconDir.isDirectory()) {
            iconList = iconDir.list();
        }
        return iconList;
    }

    public byte[] getIconBytesInternal(String category, String iconName) {
        String path = GlobalProps.getHomeDir() + File.separator + "icons" + File.separator + category + File.separator + iconName;
        if (GlobalProps.DEBUG) {
            System.out.println("getIconBytes - path: " + path);
        }
        return getFileBytes(new File(path));
    }

    private static byte[] getFileBytes(File file) {
        if (file.exists()) {
            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
            try {
                FileInputStream fileIn = new FileInputStream(file);
                byte[] buff = new byte[2048];
                int read = 0;
                while ((read = fileIn.read(buff)) != -1) {
                    bytesOut.write(buff, 0, read);
                }
            } catch (FileNotFoundException fnfe) {
                if (GlobalProps.DEBUG) {
                    System.out.println("getFileBy - FNFexp");
                }
            } catch (IOException ioe) {
                if (GlobalProps.DEBUG) {
                    System.out.println("getFileBy - IOexp");
                }
            }
            byte[] bytes = bytesOut.toByteArray();
            if (GlobalProps.DEBUG) {
                System.out.println("getFileBy - size: " + bytes.length);
            }
            return bytesOut.toByteArray();
        }
        return null;
    }
}
