package com.monad.homerun.modelmgt.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Observer;
import java.util.Properties;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.monad.homerun.base.DataObject;
import com.monad.homerun.base.Value;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.event.Event;
import com.monad.homerun.event.Emitter;
import com.monad.homerun.model.Model;
import com.monad.homerun.model.ModelCore;
import com.monad.homerun.model.ModelStatus;
import com.monad.homerun.model.time.TimeModel;
import com.monad.homerun.view.Scene;
import com.monad.homerun.view.Album;
import com.monad.homerun.view.Snapshot;
import com.monad.homerun.view.Region;
import com.monad.homerun.view.View;
import com.monad.homerun.function.Function;
import com.monad.homerun.function.math.Sum;
import com.monad.homerun.function.math.Mean;
import com.monad.homerun.function.math.Min;
import com.monad.homerun.function.math.Max;
import com.monad.homerun.modelmgt.ModelInformer;
import com.monad.homerun.modelmgt.ModelRuntime;
import com.monad.homerun.modelmgt.ModelService;
import com.monad.homerun.modelmgt.ModelFactory;
import com.monad.homerun.objmgt.ModelUpdater;
import com.monad.homerun.objmgt.ObjectService;
import com.monad.homerun.objmgt.impl.Herald;
import com.monad.homerun.object.CompBinding;
import com.monad.homerun.model.scalar.ValueType;
import com.monad.homerun.store.ObjectStore;
import com.monad.homerun.store.RecordingService;

/**
 * ModelManager manages storage and instantiation of object models
 * and related data structures.
 */
public class ModelManager implements ModelService, ModelUpdater, ModelFactory {

    private Logger logger = null;

    private ObjectStore store = null;

    private Map<String, RTContext> modelMap = null;

    private Map<String, ModelInformer> informerMap = null;

    private Map<String, ModelFactory> factMap = null;

    private String snapPath = null;

    private Map<String, String> funcMap = null;

    private Map<String, List<Observer>> priPendMap = null;

    private Map<String, List<Observer>> regPendMap = null;

    public ModelManager() {
        logger = Activator.logSvc.getLogger();
        store = Activator.getStore("model", Scene.class);
        informerMap = new HashMap<String, ModelInformer>();
    }

    public void init(boolean start) {
        if (GlobalProps.DEBUG) {
            logger.log(Level.FINE, "ModelManager initializing - start: " + start);
        }
        snapPath = GlobalProps.getHomeDir() + File.separator + "temp" + File.separator + "snapshot";
        File dirPath = new File(snapPath);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        snapPath += File.separator;
        if (start) {
            modelMap = new HashMap<String, RTContext>();
            factMap = new HashMap<String, ModelFactory>();
            factMap.put("platform", this);
            funcMap = new HashMap<String, String>();
            Function bifunc = new Sum();
            funcMap.put(bifunc.getName(), bifunc.getClass().getCanonicalName());
            bifunc = new Mean();
            funcMap.put(bifunc.getName(), bifunc.getClass().getCanonicalName());
            bifunc = new Min();
            funcMap.put(bifunc.getName(), bifunc.getClass().getCanonicalName());
            bifunc = new Max();
            funcMap.put(bifunc.getName(), bifunc.getClass().getCanonicalName());
            priPendMap = new HashMap<String, List<Observer>>();
            regPendMap = new HashMap<String, List<Observer>>();
        }
        if (GlobalProps.DEBUG) {
            logger.log(Level.FINE, "ModelManager initializing - end: " + start);
        }
    }

    public void shutdown() {
        persistAllModels();
        priPendMap = null;
        regPendMap = null;
        logger.log(Level.INFO, "shutdown");
    }

    public void createModel(String domain, String objectName, CompBinding binding, boolean isNew) {
        if (binding == null) {
            logger.log(Level.SEVERE, "No binding for '" + objectName + "' - cannot instantiate");
            return;
        }
        String modelName = binding.getCompName();
        String bindingName = binding.getBindingName();
        Model model = getModel(domain, modelName);
        if (model == null) {
            logger.log(Level.SEVERE, "Cannot create model '" + modelName + "' - no definition exists");
            return;
        }
        ModelRuntime modelRt = instantiateModel(binding);
        if (modelRt == null) {
            logger.log(Level.SEVERE, "Cannot create model '" + modelName + "' - cannot instantiate");
            return;
        }
        RTContext rtc = new RTContext(modelRt);
        String informer = null;
        String infType = null;
        if (binding.getBooleanProperty("emit")) {
            rtc.herald = new Herald(this);
        }
        informer = binding.getProperty("informer");
        infType = binding.getProperty("infType");
        Properties props = binding.getProperties();
        boolean noCore = binding.getBooleanProperty("noCore");
        if (noCore) {
            rtc.saveCore = false;
        }
        if (!isNew && rtc.saveCore) {
            String key = model.getDomain() + "." + objectName + "." + bindingName;
            ModelCore core = (ModelCore) store.getObject(key, ModelCore.class.getName());
            if (core != null) {
                props.put("core", core);
            } else {
                logger.log(Level.SEVERE, "No core found for '" + model.getModelName() + "' of: '" + objectName + "'");
            }
        }
        rtc.autoSave = rtc.saveCore && binding.getBooleanProperty("autoSave");
        modelRt.init(model, props, rtc.herald);
        String key = domain + "." + objectName + "." + bindingName;
        modelMap.put(key, rtc);
        if (informer != null) {
            assignInformer(informer, infType, domain, objectName, modelName);
        }
        if (rtc.herald != null) {
            List<Observer> obsList = priPendMap.get(key);
            if (obsList != null) {
                for (Observer obs : obsList) {
                    rtc.herald.addPriorityObserver(obs);
                }
                priPendMap.remove(key);
            }
            obsList = regPendMap.get(key);
            if (obsList != null) {
                for (Observer obs : obsList) {
                    rtc.herald.addObserver(obs);
                }
                regPendMap.remove(key);
            }
        }
    }

    private ModelRuntime instantiateModel(CompBinding modBind) {
        String factName = modBind.getProperty("factory");
        if (factName == null || factName.length() == 0) {
            factName = "platform";
        }
        String className = modBind.getProperty("implClass");
        if (className == null || className.length() == 0) {
            logger.log(Level.SEVERE, "Missing/bad className '" + className + "' cannot instantiate object: '" + modBind.getCompName() + "'.");
            return null;
        }
        ModelFactory factory = (ModelFactory) factMap.get(factName);
        if (factory == null) {
            factory = Activator.getModelFactory(factName);
            if (factory != null) {
                factMap.put(factName, factory);
            } else {
                factory = (ModelFactory) factMap.get("platform");
            }
        }
        return factory.createRuntime(className);
    }

    public ModelRuntime createRuntime(String type) {
        ModelRuntime newModel = null;
        try {
            newModel = (ModelRuntime) Class.forName(type).newInstance();
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.SEVERE, "Can't create model - class '" + type + "' not found.");
            newModel = null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Caught exception loading class '" + type + "': " + e.getMessage());
            newModel = null;
        }
        return newModel;
    }

    public void destroyModel(String domain, String objectName, String modelName) {
        String key = domain + "." + objectName + "." + modelName;
        modelMap.remove(key);
    }

    public void persistModel(String domain, String objectName, String modelName) {
        String key = domain + "." + objectName + "." + modelName;
        persistModel(key);
    }

    public void persistAllModels() {
        Iterator<String> iter = modelMap.keySet().iterator();
        while (iter.hasNext()) {
            persistModel(iter.next());
        }
    }

    private boolean persistModel(String key) {
        RTContext rtc = modelMap.get(key);
        if (rtc.model != null && rtc.saveCore && !rtc.autoSave) {
            ModelCore core = rtc.model.getCore();
            if (core != null) {
                core.setStorageKey(key);
                setModelCore(core);
            }
        }
        return true;
    }

    public String[] getModelTypes(String domain) {
        return new String[] { "state", "bag", "value", "number", "time", "date" };
    }

    public boolean canObserveModel(String domain, String objectName, String modelName) {
        String key = domain + "." + objectName + "." + modelName;
        if (GlobalProps.DEBUG) {
            System.out.println("canObs - enter " + domain + " " + objectName + " " + modelName);
        }
        RTContext rtc = modelMap.get(key);
        return (rtc != null && rtc.herald != null);
    }

    public void addModelObserver(String domain, String objectName, String modelName, Observer observer, boolean priority) {
        if (!(objectName.equals("*") || objectName.equals("$"))) {
            String key = domain + "." + objectName + "." + modelName;
            if (GlobalProps.DEBUG) {
                System.out.println("addMO - enter " + domain + " " + objectName + " " + modelName);
            }
            RTContext rtc = modelMap.get(key);
            if (rtc != null && rtc.herald != null) {
                if (priority) {
                    rtc.herald.addPriorityObserver(observer);
                } else {
                    rtc.herald.addObserver(observer);
                }
            } else {
                if (priority) {
                    List<Observer> priList = priPendMap.get(key);
                    if (priList == null) {
                        priList = new ArrayList<Observer>();
                        priPendMap.put(key, priList);
                    }
                    priList.add(observer);
                } else {
                    List<Observer> regList = regPendMap.get(key);
                    if (regList == null) {
                        regList = new ArrayList<Observer>();
                        regPendMap.put(key, regList);
                    }
                    regList.add(observer);
                }
            }
        } else {
            Iterator<String> iter = modelMap.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                if (key.indexOf(modelName) != -1) {
                    RTContext rtc = modelMap.get(key);
                    if (rtc != null && rtc.herald != null) {
                        if (priority) {
                            rtc.herald.addPriorityObserver(observer);
                        } else {
                            rtc.herald.addObserver(observer);
                        }
                    }
                }
            }
        }
    }

    public void removeModelObserver(String domain, String objectName, String modelName, Observer observer) {
        String key = domain + "." + objectName + "." + modelName;
        if (GlobalProps.DEBUG) {
            System.out.println("remMO - enter " + domain + " " + objectName + " " + modelName);
        }
        RTContext rtc = modelMap.get(key);
        if (rtc != null && rtc.herald != null) {
            rtc.herald.deleteObserver(observer);
        }
    }

    public String[] getValueTypeNames() {
        return store.getKeys(null, ValueType.class.getName());
    }

    public ValueType getValueType(String typeName) {
        String key = typeName;
        return (ValueType) store.getObject(key, ValueType.class.getName());
    }

    public boolean addValueType(ValueType valType) {
        String key = valType.getTypeName();
        return store.addObject(key, ValueType.class.getName(), valType);
    }

    public boolean removeValueType(ValueType valType) {
        String key = valType.getTypeName();
        return store.removeObject(key, ValueType.class.getName());
    }

    public String[] getObjectModelNames(String domain, String objectName) {
        List<String> nameList = new ArrayList<String>();
        String match = domain + "." + objectName;
        Iterator<String> iter = modelMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            if (key.startsWith(match)) {
                RTContext rtc = modelMap.get(key);
                nameList.add(rtc.model.getModelName());
            }
        }
        return nameList.toArray(new String[0]);
    }

    public ModelStatus getModelStatus(String domain, String objectName, String modelName) {
        String key = domain + "." + objectName + "." + modelName;
        if (GlobalProps.DEBUG) {
            System.out.println("getMStat: " + domain + " " + objectName + " " + modelName);
        }
        if (modelMap == null) {
            if (GlobalProps.DEBUG) {
                System.out.println("getMStat: no model map");
            }
            return null;
        }
        RTContext rtc = modelMap.get(key);
        if (rtc != null) {
            if (GlobalProps.DEBUG) {
                System.out.println("getMStat: model not null " + domain + " " + objectName + " " + modelName);
                System.out.println("getMStat: model " + rtc.model);
                System.out.println("getMStat: stat: " + rtc.model.getStatus());
            }
            return rtc.model.getStatus();
        }
        return null;
    }

    public ModelCore getModelCore(String key) {
        return (ModelCore) store.getObject(key, ModelCore.class.getName());
    }

    public boolean setModelCore(ModelCore core) {
        if (core.getStorageKey() == null) {
            logger.log(Level.SEVERE, "Model Core lacks key - not stored");
            return false;
        }
        return store.writeObject(core.getStorageKey(), ModelCore.class.getName(), core);
    }

    public void registerInformer(ModelInformer informer) {
        informerMap.put(informer.getInformerName(), informer);
    }

    public void unregisterInformer(ModelInformer informer) {
        informerMap.remove(informer.getInformerName());
    }

    public Emitter[] getInformers(String domain, String modelName) {
        Model md = getModel(domain, modelName);
        List<Emitter> infList = new ArrayList<Emitter>();
        Iterator<String> iter = informerMap.keySet().iterator();
        while (iter.hasNext()) {
            String infName = iter.next();
            ModelInformer informer = informerMap.get(infName);
            Emitter emitter = informer.canInform(md);
            if (emitter != null) {
                if (GlobalProps.DEBUG) {
                    System.out.println("GetInformers: adding info");
                }
                infList.add(emitter);
            }
        }
        return infList.toArray(new Emitter[0]);
    }

    public Emitter canInform(String informerName, String domain, String modelName) {
        ModelInformer informer = informerMap.get(informerName);
        if (informer != null) {
            Model model = getModel(domain, modelName);
            if (model != null) {
                return informer.canInform(model);
            }
        }
        return null;
    }

    public void assignInformer(String informerName, String type, String domain, String objectName, String modelName) {
        ModelInformer informer = informerMap.get(informerName);
        if (informer != null) {
            Model model = getModel(domain, modelName);
            informer.addModel(type, domain, objectName, model);
        }
    }

    public void unassignInformer(String informerName, String domain, String objectName, String modelName) {
        ModelInformer informer = informerMap.get(informerName);
        if (informer != null) {
            informer.removeModel(domain, objectName, modelName);
        }
    }

    public void informModel(String domain, String objectName, String modelName, Event event) {
        if (GlobalProps.DEBUG) {
            System.out.println("MM got name: " + objectName + " " + modelName);
        }
        String key = domain + "." + objectName + "." + modelName;
        RTContext rtc = modelMap.get(key);
        if (rtc != null) {
            if (GlobalProps.DEBUG) {
                System.out.println("MM informing name: " + objectName + " " + modelName);
            }
            rtc.inform(key, event);
        }
    }

    public void informModel(String domain, String objectName, String modelName, String action, String source) {
        if (GlobalProps.DEBUG) {
            System.out.println("MM inform name: " + objectName + " " + modelName);
        }
        String key = domain + "." + objectName + "." + modelName;
        RTContext rtc = modelMap.get(key);
        if (rtc != null) {
            String modelType = rtc.model.getStatus().getModelType();
            Event infEvent = null;
            if ("state".equals(modelType)) {
                Value state = new Value(action, System.currentTimeMillis());
                infEvent = new Event(state, source);
            } else if ("number".equals(modelType)) {
                infEvent = new Event(action, source);
            } else if ("time".equals(modelType)) {
                if (TimeModel.STAMP.equals(action)) {
                    Value now = new Value(System.currentTimeMillis());
                    infEvent = new Event(now, source);
                }
            } else if ("value".equals(modelType)) {
            }
            if (infEvent != null) {
                if (GlobalProps.DEBUG) {
                    System.out.println("MM informing name: " + objectName + " " + modelName);
                }
                rtc.inform(key, infEvent);
            } else {
                if (GlobalProps.DEBUG) {
                    System.out.println("MM no event for name: " + objectName + " " + modelName);
                }
            }
        }
    }

    private class RTContext {

        public ModelRuntime model = null;

        public Herald herald = null;

        public boolean wired = false;

        public boolean saveCore = true;

        public boolean autoSave = false;

        public RTContext(ModelRuntime model) {
            this.model = model;
        }

        public boolean inform(String key, Event event) {
            model.inform(event);
            if (saveCore && autoSave) {
                ModelCore core = model.getCore();
                if (core != null) {
                    core.setStorageKey(key);
                    setModelCore(core);
                }
                return false;
            }
            return true;
        }
    }

    public boolean modelExists(String domain, String modelName) {
        String key = domain + "." + modelName;
        return store.containsObject(key, Model.class.getName());
    }

    public String[] getModelNames(String domain) {
        if (GlobalProps.DEBUG) {
            System.out.println("getModelNames dom: " + domain + " store: " + store);
        }
        String[] names = store.getKeys(domain, Model.class.getName());
        if (GlobalProps.DEBUG) {
            System.out.println("getModelNames len: " + names.length);
        }
        for (int i = 0; i < names.length; i++) {
            names[i] = names[i].substring(names[i].lastIndexOf(".") + 1);
        }
        return names;
    }

    public Model getModel(String domain, String modelName) {
        String key = domain + "." + modelName;
        if (GlobalProps.DEBUG) {
            System.out.println("RLR getModel key: " + key + "store: " + store);
        }
        return (Model) store.getObject(key, Model.class.getName());
    }

    public boolean addModel(Model model) {
        ((DataObject) model).setModificationTime(System.currentTimeMillis());
        String key = model.getDomain() + "." + model.getModelName();
        if (GlobalProps.DEBUG) {
            System.out.println("RLR addModel: " + key);
        }
        if (store.addObject(key, Model.class.getName(), model)) {
            store.index(Model.class.getName(), "type", model.getModelType(), model.getModelName());
            return true;
        }
        return false;
    }

    public boolean updateModel(Model model) {
        String key = model.getDomain() + "." + model.getModelName();
        if (GlobalProps.DEBUG) {
            System.out.println("RLR updateModel: " + key);
        }
        return store.updateObject(key, Model.class.getName(), model);
    }

    public boolean removeModel(Model model) {
        String key = model.getDomain() + "." + model.getModelName();
        if (GlobalProps.DEBUG) {
            System.out.println("RLR removeModel: " + key);
        }
        store.deIndex(Model.class.getName(), "type", model.getModelType(), model.getModelName());
        return store.removeObject(key, Model.class.getName());
    }

    public String[] getModelTypes(String domain, boolean activeOnly) {
        if (!activeOnly) {
            return getModelTypes(domain);
        }
        String[] modelKeys = store.getKeys(domain, Model.class.getName());
        List<String> retTypes = new ArrayList<String>();
        for (int i = 0; i < modelKeys.length; i++) {
            String modelName = modelKeys[i].substring(modelKeys[i].indexOf(".") + 1);
            Model model = getModel(domain, modelName);
            if (model != null && !retTypes.contains(model.getModelType())) {
                retTypes.add(model.getModelType());
            }
        }
        return retTypes.toArray(new String[0]);
    }

    public String[] getModelsOfType(String domain, String type) {
        String[] modelNames = getModelNames(domain);
        List<String> models = new ArrayList<String>();
        if (GlobalProps.DEBUG) {
            System.out.println("getMOdOT dom: " + domain + " num:" + modelNames.length);
        }
        for (int i = 0; i < modelNames.length; i++) {
            if (GlobalProps.DEBUG) {
                System.out.println("getMOdOT modName: " + modelNames[i]);
            }
            Model md = getModel(domain, modelNames[i]);
            if (md != null && md.getModelType().equals(type)) {
                models.add(modelNames[i]);
            }
        }
        return models.toArray(new String[0]);
    }

    public String[] getSceneCategories() {
        String[] storeKeys = store.getKeys(null, Scene.class.getName());
        List<String> catList = new ArrayList<String>();
        for (int i = 0; i < storeKeys.length; i++) {
            String category = storeKeys[i].substring(0, storeKeys[i].indexOf("."));
            if (!catList.contains(category)) {
                catList.add(category);
            }
        }
        return catList.toArray(new String[0]);
    }

    public String[] getSceneNames(String category) {
        String[] names = store.getKeys(category, Scene.class.getName());
        for (int i = 0; i < names.length; i++) {
            names[i] = names[i].substring(names[i].indexOf(".") + 1);
        }
        return names;
    }

    public Scene getScene(String category, String sceneName) {
        String key = category + "." + sceneName;
        return (Scene) store.getObject(key, Scene.class.getName());
    }

    public boolean addScene(Scene scene) {
        scene.setModificationTime(System.currentTimeMillis());
        String key = scene.getCategory() + "." + scene.getName();
        return store.addObject(key, Scene.class.getName(), scene);
    }

    public boolean updateScene(Scene scene) {
        scene.setModificationTime(System.currentTimeMillis());
        String key = scene.getCategory() + "." + scene.getName();
        return store.updateObject(key, Scene.class.getName(), scene);
    }

    public boolean removeScene(Scene scene) {
        String key = scene.getCategory() + "." + scene.getName();
        return store.removeObject(key, Scene.class.getName());
    }

    public void captureScene(String category, String sceneName) {
        Scene scene = getScene(category, sceneName);
        if (scene != null) {
            Snapshot snap = new Snapshot(scene);
            Region[] regions = scene.getRegions();
            for (int i = 0; i < regions.length; i++) {
                View[] views = regions[i].getViews();
                for (int j = 0; j < views.length; j++) {
                    String type = views[j].getType();
                    if ("model".equals(type)) {
                        String domain = views[j].getDomain();
                        String modelName = views[j].getCompName();
                        boolean single = !views[j].getSelector().equals("*");
                        Model modelDef = getModel(domain, modelName);
                        if (modelDef != null) {
                            if (GlobalProps.DEBUG) {
                                System.out.println("Mtype: " + modelDef.getModelType());
                            }
                            String[] objects = null;
                            ObjectService objSvc = Activator.getObjectService();
                            if (single) {
                                objects = new String[1];
                                objects[0] = views[j].getSelector();
                            } else {
                                objects = objSvc.getBearersOfModel(domain, modelName);
                            }
                            for (int k = 0; k < objects.length; k++) {
                                snap.addStatus(domain, objects[k], modelName, objSvc.getModelStatus(domain, objects[k], modelName));
                            }
                        }
                    }
                }
            }
            String fileName = snapFileName(category, sceneName, snap.getCaptureTime());
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(fileName)));
                out.writeObject(snap);
                out.flush();
                out.close();
            } catch (IOException ioe) {
                ;
            }
        }
    }

    public long[] getSnapshotTimes(String category, String sceneName) {
        File snapDir = new File(snapPath);
        String prefix = category + "." + sceneName.replace(' ', '_') + "-";
        File[] files = snapDir.listFiles(new SnapshotFilter(prefix));
        long[] times = new long[files.length];
        for (int i = 0; i < times.length; i++) {
            times[i] = Long.parseLong(snapTime(files[i].getName()));
        }
        return times;
    }

    public Snapshot getSnapshot(String category, String sceneName, long time) {
        Snapshot snapshot = null;
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(snapFileName(category, sceneName, time))));
            snapshot = (Snapshot) in.readObject();
            in.close();
        } catch (Exception exp) {
        }
        return snapshot;
    }

    private String snapTime(String fileName) {
        return fileName.substring(fileName.indexOf("-") + 1, fileName.indexOf(".snap"));
    }

    private String snapFileName(String category, String sceneName, long time) {
        StringBuffer snapB = new StringBuffer();
        snapB.append(snapPath);
        snapB.append(category);
        snapB.append(".");
        snapB.append(sceneName.replace(' ', '_'));
        snapB.append("-");
        snapB.append(time);
        snapB.append(".snap");
        return snapB.toString();
    }

    private class SnapshotFilter implements FilenameFilter {

        private String sceneName = null;

        public SnapshotFilter(String sceneName) {
            this.sceneName = sceneName;
        }

        public boolean accept(File file, String name) {
            return name.startsWith(sceneName);
        }
    }

    public String[] getViewTypes() {
        List<String> typeList = new ArrayList<String>();
        if (getImageNames("user").length > 0) {
            typeList.add("image");
        }
        if (Activator.isBundleInstalled("record")) {
            typeList.add("trail:value");
            typeList.add("trail:number");
        }
        if (Activator.isBundleInstalled("hms")) {
            typeList.add("message");
        }
        return (String[]) typeList.toArray(new String[0]);
    }

    public String[] getImageNames(String category) {
        String[] imageList = new String[0];
        String path = GlobalProps.getHomeDir() + File.separator + "var" + File.separator + "images" + File.separator + category;
        File imageDir = new File(path);
        if (imageDir.isDirectory()) {
            imageList = imageDir.list();
        }
        return imageList;
    }

    public byte[] getViewBytes(View view) {
        String opts = view.hasOption(0x08) ? "delta" : null;
        return getModelTrail(view.getDomain(), view.getSelector(), view.getCompName(), opts);
    }

    public byte[] getImageBytes(String category, String imageName) {
        String path = GlobalProps.getHomeDir() + File.separator + "var" + File.separator + "images" + File.separator + category + File.separator + imageName;
        if (GlobalProps.DEBUG) {
            System.out.println("GetImBy path: " + path);
        }
        return getFileBytes(new File(path));
    }

    public byte[] getModelTrail(String domain, String object, String model, String opts) {
        RecordingService recSvc = Activator.getRecordingService();
        if (recSvc != null) {
            List<String> recs = new ArrayList<String>();
            if ("*".equals(object)) {
                ObjectService objSvc = Activator.getObjectService();
                for (String obj : objSvc.getBearersOfModel(domain, model)) {
                    recs.add(domain + ":" + obj + ":" + model);
                }
            } else {
                recs.add(domain + ":" + object + ":" + model);
            }
            return recSvc.getPlot(recs, model, opts);
        }
        return new byte[0];
    }

    private byte[] getFileBytes(File file) {
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

    public String[] getAlbumNames() {
        return store.getKeys(null, Album.class.getName());
    }

    public Album getAlbum(String albumName) {
        return (Album) store.getObject(albumName, Album.class.getName());
    }

    public boolean addAlbum(Album album) {
        album.setModificationTime(System.currentTimeMillis());
        return store.addObject(album.getName(), Album.class.getName(), album);
    }

    public boolean updateAlbum(Album album) {
        album.setModificationTime(System.currentTimeMillis());
        return store.updateObject(album.getName(), Album.class.getName(), album);
    }

    public boolean removeAlbum(Album album) {
        String[] myScenes = album.getSceneNames(album.getName());
        for (int i = 0; i < myScenes.length; i++) {
            Scene scene = getScene(album.getName(), myScenes[i]);
            if (scene != null) {
                removeScene(scene);
            }
        }
        return store.removeObject(album.getName(), Album.class.getName());
    }

    public String[] getFunctionNames() {
        String[] names = new String[funcMap.size()];
        int i = 0;
        for (String name : funcMap.keySet()) {
            names[i++] = name;
        }
        return names;
    }

    public Function getFunction(String functionName) {
        Function func = null;
        String className = funcMap.get(functionName);
        if (className != null) {
            try {
                func = (Function) Class.forName(className).newInstance();
            } catch (Exception e) {
                func = null;
            }
        }
        return func;
    }
}
