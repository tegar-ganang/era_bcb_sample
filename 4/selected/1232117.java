package com.monad.homerun.admin.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.List;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import com.monad.homerun.admin.impl.Activator;
import com.monad.homerun.control.Control;
import com.monad.homerun.core.GlobalProps;
import com.monad.homerun.model.ModelCore;
import com.monad.homerun.modelmgt.ModelService;
import com.monad.homerun.object.CompBinding;
import com.monad.homerun.object.Domain;
import com.monad.homerun.object.Type;
import com.monad.homerun.object.Instance;
import com.monad.homerun.objmgt.ActionService;
import com.monad.homerun.objmgt.ObjectService;
import com.monad.homerun.rule.Rule;
import com.monad.homerun.store.ObjectStore;
import com.monad.homerun.view.Scene;

/**
 * Exporter is a utility for gathering user-mutable data from a server
 * and putting it into an installable package - in essence 
 * creating a backup or allowing a system to be transferred from one
 * server to another. User data can include: data objects,
 * configuration files, images, localizations, a record of which
 * packages (bundles) have been installed.
 * Creates a standard HomeRun/OSGi bundle, in the server 'temp' directory.
 */
public class Exporter {

    private Exporter() {
    }

    public static String export(String pkgName, String version, String descrip, String opts) {
        JarOutputStream jarOut = null;
        descrip = descrip.replace("_", " ");
        String pkgPath = GlobalProps.getHomeDir() + File.separator + "temp" + File.separator + pkgName + "-" + version + ".jar";
        boolean wantImages = opts.indexOf("i") != -1;
        boolean wantData = opts.indexOf("d") != -1;
        boolean wantConf = opts.indexOf("c") != -1;
        boolean wantL10n = opts.indexOf("l") != -1;
        boolean wantPkgs = opts.indexOf("p") != -1;
        boolean wantCores = opts.indexOf("m") != -1;
        Manifest mft = new Manifest();
        Attributes attrs = mft.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0");
        attrs.putValue("Bundle-Name", pkgName);
        attrs.putValue("Bundle-Version", version);
        attrs.putValue("Bundle-Description", descrip);
        attrs.putValue("Bundle-Vendor", "homerun");
        attrs.putValue("Bundle-Category", "export");
        attrs.putValue("Homerun-Type", "export");
        try {
            if (wantConf) {
                String confList = processConf(null);
                if (confList != null) {
                    attrs.putValue("Homerun-Conf", confList);
                } else {
                    wantConf = false;
                }
            }
            if (wantImages) {
                String imageList = processImages(null);
                if (imageList != null) {
                    attrs.putValue("Homerun-Images", imageList);
                } else {
                    wantImages = false;
                }
            }
            if (wantData) {
                String dataList = processData(null);
                if (dataList != null) {
                    attrs.putValue("Homerun-Load", dataList);
                } else {
                    wantData = false;
                }
            }
            if (wantCores) {
                String coreList = processCores(null);
                if (coreList != null) {
                    attrs.putValue("Homerun-Cores", coreList);
                } else {
                    wantCores = false;
                }
            }
            if (wantL10n) {
                String l10nList = processL10n(null);
                if (l10nList != null) {
                    attrs.putValue("Homerun-L10n", l10nList);
                } else {
                    wantL10n = false;
                }
            }
            if (wantPkgs) {
                String pkgList = processPkgs(null);
                if (pkgList != null) {
                    attrs.putValue("Homerun-Install", pkgList);
                } else {
                    wantPkgs = false;
                }
            }
            jarOut = new JarOutputStream(new FileOutputStream(pkgPath), mft);
            if (wantConf) {
                processConf(jarOut);
            }
            if (wantImages) {
                processImages(jarOut);
            }
            if (wantData) {
                processData(jarOut);
            }
            if (wantCores) {
                processCores(jarOut);
            }
            if (wantL10n) {
                processL10n(jarOut);
            }
            jarOut.flush();
            jarOut.finish();
            jarOut.close();
        } catch (IOException ioE) {
            if (GlobalProps.DEBUG) {
                ioE.printStackTrace();
            }
            return "Error";
        }
        return null;
    }

    private static String processConf(JarOutputStream jarOut) throws IOException {
        String cfgPath = GlobalProps.getHomeDir() + File.separator + "conf";
        StringBuffer cfgList = new StringBuffer();
        for (File cfgDir : new File(cfgPath).listFiles()) {
            if (cfgDir.isDirectory()) {
                for (File cfgFile : cfgDir.listFiles()) {
                    String relPath = cfgDir.getName() + "/" + cfgFile.getName();
                    if (jarOut == null) {
                        cfgList.append(relPath);
                        cfgList.append(",");
                    } else {
                        addFile(jarOut, "conf/" + relPath, cfgFile);
                    }
                }
            }
        }
        String cList = cfgList.toString();
        if (jarOut == null && cList.length() > 0) {
            return cList.substring(0, cList.length() - 1);
        }
        return null;
    }

    private static String processImages(JarOutputStream jarOut) throws IOException {
        String imgPath = GlobalProps.getHomeDir() + File.separator + "var" + File.separator + "images";
        StringBuffer imgList = new StringBuffer();
        for (File imgDir : new File(imgPath).listFiles()) {
            if (imgDir.isDirectory()) {
                for (File imgFile : imgDir.listFiles()) {
                    String jarPath = imgDir.getName() + "/" + imgFile.getName();
                    if (jarOut != null) {
                        addFile(jarOut, "images/" + jarPath, imgFile);
                    } else {
                        imgList.append(jarPath);
                        imgList.append(",");
                    }
                }
            }
        }
        String iList = imgList.toString();
        if (jarOut == null && iList.length() > 0) {
            return iList.substring(0, iList.length() - 1);
        }
        return null;
    }

    private static String processL10n(JarOutputStream jarOut) throws IOException {
        String lznPath = GlobalProps.getHomeDir() + File.separator + "var" + File.separator + "package" + File.separator + "L10n";
        StringBuffer lznList = new StringBuffer();
        for (File lznFile : new File(lznPath).listFiles()) {
            String jarPath = lznFile.getName();
            if (jarOut != null) {
                addFile(jarOut, "L10n/" + jarPath, lznFile);
            } else {
                lznList.append(jarPath);
                lznList.append(",");
            }
        }
        String iList = lznList.toString();
        if (jarOut == null && iList.length() > 0) {
            return iList.substring(0, iList.length() - 1);
        }
        return null;
    }

    private static String processPkgs(JarOutputStream jarOut) throws IOException {
        StringBuffer pkgList = new StringBuffer();
        BundleContext bc = Activator.getBundleContext();
        long bId = 1L;
        Bundle bundle = bc.getBundle(bId);
        while (bundle != null) {
            Dictionary headers = bundle.getHeaders();
            String hrType = (String) headers.get("Homerun-Type");
            if (hrType != null && !"export".equals(hrType)) {
                pkgList.append((String) headers.get("Bundle-SymbolicName"));
                String vsn = (String) headers.get("Bundle-Version");
                if (vsn != null) {
                    pkgList.append(";version=\"");
                    pkgList.append(vsn);
                    pkgList.append("\"");
                }
                pkgList.append(",");
            }
            bundle = bc.getBundle(++bId);
        }
        String pList = pkgList.toString();
        if (jarOut == null && pList.length() > 0) {
            return pList.substring(0, pList.length() - 1);
        }
        return null;
    }

    private static String processData(JarOutputStream jarOut) throws IOException {
        ObjectService objSvc = Activator.objectSvc;
        ObjectStore objStr = Activator.storeFact.getInstance("object", Instance.class);
        ActionService actSvc = Activator.actionSvc;
        ObjectStore actStr = Activator.storeFact.getInstance("action", Rule.class);
        ModelService mdlSvc = Activator.modelSvc;
        ObjectStore mdlStr = Activator.storeFact.getInstance("model", Scene.class);
        List<Object> objects = new ArrayList<Object>();
        List<Object> models = new ArrayList<Object>();
        StringBuffer dataList = new StringBuffer();
        for (Control ctrl : objSvc.getControlsForType(null)) {
            objects.add(objSvc.getControl(ctrl.getControlName()));
        }
        updateData("controls", objects, jarOut, dataList, objStr);
        for (String emtrName : objSvc.getEmittersForType(null)) {
            objects.add(objSvc.getEmitter(emtrName));
        }
        updateData("emitters", objects, jarOut, dataList, objStr);
        for (String panelCat : objSvc.getPanelCategories()) {
            for (String panelName : objSvc.getPanelNames(panelCat)) {
                objects.add(objSvc.getPanel(panelCat, panelName));
            }
        }
        updateData("panels", objects, jarOut, dataList, objStr);
        Domain[] domains = objSvc.getDomains(Domain.FLT_NONE);
        for (int i = 0; i < domains.length; i++) {
            String domain = domains[i].getName();
            objects.add(objSvc.getDomain(domain, Domain.FLT_NONE));
            for (Type type : objSvc.getTypes(domain)) {
                objects.add(type);
            }
            for (String filterName : objSvc.getFilterNames(domain)) {
                objects.add(objSvc.getFilter(domain, filterName));
            }
            for (String instName : objSvc.getObjectNames(domain)) {
                objects.add(objSvc.getObject(domain, instName));
            }
            for (String modelName : mdlSvc.getModelNames(domain)) {
                models.add(mdlSvc.getModel(domain, modelName));
            }
            updateData(domain, objects, jarOut, dataList, objStr);
            updateData(domain + "-models", models, jarOut, dataList, mdlStr);
        }
        for (String ruleCat : actSvc.getRuleCategories()) {
            for (String ruleName : actSvc.getRuleNames(ruleCat)) {
                objects.add(actSvc.getRule(ruleCat, ruleName));
            }
        }
        updateData("actions", objects, jarOut, dataList, actStr);
        for (String situCat : actSvc.getSituationCategories()) {
            for (String situName : actSvc.getSituationNames(situCat)) {
                objects.add(actSvc.getSituation(situCat, situName));
            }
        }
        updateData("situations", objects, jarOut, dataList, actStr);
        for (String schedName : actSvc.getScheduleNames()) {
            objects.add(actSvc.getSchedule(schedName));
        }
        updateData("schedules", objects, jarOut, dataList, actStr);
        for (String planName : actSvc.getPlanNames()) {
            objects.add(actSvc.getPlan(planName));
        }
        updateData("plans", objects, jarOut, dataList, actStr);
        for (String monthName : actSvc.getMonthNames()) {
            objects.add(actSvc.getMonth(monthName));
        }
        updateData("months", objects, jarOut, dataList, actStr);
        for (String schemeName : actSvc.getSchemeNames()) {
            objects.add(actSvc.getScheme(schemeName));
        }
        updateData("schemes", objects, jarOut, dataList, actStr);
        for (String sceneCat : mdlSvc.getSceneCategories()) {
            for (String sceneName : mdlSvc.getSceneNames(sceneCat)) {
                objects.add(mdlSvc.getScene(sceneCat, sceneName));
            }
        }
        updateData("scenes", objects, jarOut, dataList, mdlStr);
        for (String albumName : mdlSvc.getAlbumNames()) {
            objects.add(mdlSvc.getAlbum(albumName));
        }
        updateData("albums", objects, jarOut, dataList, mdlStr);
        for (String circuitName : objSvc.getCircuitNames()) {
            objects.add(objSvc.getCircuit(circuitName));
        }
        updateData("circuits", objects, jarOut, dataList, objStr);
        for (String setName : objSvc.getReflexSetNames()) {
            objects.add(objSvc.getReflexSet(setName));
        }
        updateData("reflexSets", objects, jarOut, dataList, objStr);
        String dList = dataList.toString();
        if (jarOut == null && dList.length() > 0) {
            return dList.substring(0, dList.length() - 1);
        }
        return null;
    }

    private static void updateData(String name, List<Object> objects, JarOutputStream jarOut, StringBuffer dataList, ObjectStore store) throws IOException {
        if (objects.size() > 0) {
            if (jarOut == null) {
                dataList.append(name).append(",");
            } else {
                addDataFile(jarOut, name, objects, store);
            }
        }
    }

    private static String processCores(JarOutputStream jarOut) throws IOException {
        ObjectService objSvc = Activator.objectSvc;
        ModelService mdlSvc = Activator.modelSvc;
        ObjectStore mdlStr = Activator.storeFact.getInstance("model", Scene.class);
        List<Object> objects = new ArrayList<Object>();
        StringBuffer dataList = new StringBuffer();
        for (Domain domain : objSvc.getDomains(Domain.FLT_NONE)) {
            for (String objName : objSvc.getObjectNames(domain.getName())) {
                Instance object = objSvc.getObject(domain.getName(), objName);
                for (CompBinding binding : object.getModelBindings()) {
                    String key = domain.getName() + "." + object.getName() + "." + binding.getBindingName();
                    ModelCore core = mdlSvc.getModelCore(key);
                    if (core != null) {
                        objects.add(core);
                    }
                }
            }
            updateData(domain.getName() + "-cores", objects, jarOut, dataList, mdlStr);
        }
        String dList = dataList.toString();
        if (jarOut == null && dList.length() > 0) {
            return dList.substring(0, dList.length() - 1);
        }
        return null;
    }

    private static void addFile(JarOutputStream jout, String path, File file) throws IOException {
        jout.putNextEntry(new JarEntry(path));
        FileInputStream in = new FileInputStream(file);
        int read = in.read();
        while (read != -1) {
            jout.write(read);
            read = in.read();
        }
        jout.flush();
        jout.closeEntry();
    }

    private static void addDataFile(JarOutputStream jout, String name, List<Object> objects, ObjectStore store) throws IOException {
        jout.putNextEntry(new JarEntry("data/" + name + ".xml"));
        store.writeObjects(objects, jout);
        jout.flush();
        jout.closeEntry();
        objects.clear();
    }
}
