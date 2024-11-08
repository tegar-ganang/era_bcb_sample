package edu.lehigh.mab305.swproj.ConferenceModel;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.ArrayList;
import edu.lehigh.mab305.swproj.Application.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;

/***
 * Various  
 * @author marc
 *
 */
public class OWLUtil {

    protected static HashMap<String, Model> loadedModels = null;

    protected static void initModel(String path, Model m) {
        if (m.size() == 0) {
            System.err.println("Seriously? This should never happen, i don't think.");
            System.exit(-1);
        }
    }

    protected static void initModelHash() {
        if (loadedModels == null) {
            loadedModels = new HashMap<String, Model>();
        }
    }

    public static String geftOntologyLabelByFilename(String filepath, Model m) {
        String ret = "";
        NodeIterator s;
        ResIterator res;
        Resource ont;
        initModelHash();
        initModel(filepath, m);
        if (m != null) {
            res = m.listSubjectsWithProperty(RDF.type, OWL.Ontology);
            if (res.hasNext()) {
                ont = res.nextResource();
                s = m.listObjectsOfProperty(ont, RDFS.label);
                if (s.hasNext()) {
                    ret = s.nextNode().toString();
                }
            }
        }
        return ret;
    }

    protected static String getURLandWriteToDisk(String url, Model retModel) throws MalformedURLException, IOException {
        String path = null;
        URL ontURL = new URL(url);
        InputStream ins = ontURL.openStream();
        InputStreamReader bufRead;
        OutputStreamWriter bufWrite;
        int offset = 0, read = 0;
        initModelHash();
        if (System.getProperty("user.dir") != null) {
            String delimiter;
            path = System.getProperty("user.dir");
            if (path.contains("/")) {
                delimiter = "/";
            } else {
                delimiter = "\\";
            }
            char c = path.charAt(path.length() - 1);
            if (c == '/' || c == '\\') {
                path = path.substring(0, path.length() - 2);
            }
            path = path.substring(0, path.lastIndexOf(delimiter) + 1);
            path = path.concat("ontologies" + delimiter + "downloaded");
            (new File(path)).mkdir();
            path = path.concat(delimiter);
            path = createFullPath(url, path);
            bufWrite = new OutputStreamWriter(new FileOutputStream(path));
            bufRead = new InputStreamReader(ins);
            read = bufRead.read();
            while (read != -1) {
                bufWrite.write(read);
                offset += read;
                read = bufRead.read();
            }
            bufRead.close();
            bufWrite.close();
            ins.close();
            FileInputStream fs = new FileInputStream(path);
            retModel.read(fs, "");
        }
        return path;
    }

    protected static String createFullPath(String url, String startpath) {
        StringBuilder s = new StringBuilder();
        StringBuilder relPath = new StringBuilder();
        String delimiter;
        initModelHash();
        if (startpath.contains("/")) {
            delimiter = "/";
        } else {
            delimiter = "\\";
        }
        s.append(url);
        s.delete(0, s.indexOf("//") + 2);
        s.delete(0, s.indexOf("/") + 1);
        relPath.append(startpath);
        while (s.indexOf("/") != -1) {
            relPath.append(s.substring(0, s.indexOf("/")));
            relPath.append(delimiter);
            (new File(relPath.toString())).mkdir();
            s.delete(0, s.indexOf("/") + 1);
        }
        relPath.append(s);
        return relPath.toString();
    }

    public static String getOntologyLabel(String uri, Model model) {
        String ret = "";
        NodeIterator s;
        ResIterator res;
        Resource ont;
        initModelHash();
        if (model != null) {
            res = model.listSubjectsWithProperty(RDF.type, OWL.Ontology);
            while (res.hasNext()) {
                ont = res.nextResource();
                if (ont.toString().equals(uri)) {
                    s = model.listObjectsOfProperty(ont, RDFS.label);
                    if (s.hasNext()) {
                        ret = s.nextNode().toString();
                    }
                }
            }
        }
        return ret;
    }

    public static String getBaseURIForLocalOntology(FileInputStream in, Model m) {
        String ret = null;
        ResIterator riter;
        initModelHash();
        m.read(in, "");
        riter = m.listSubjectsWithProperty(RDF.type, OWL.Ontology);
        if (riter.hasNext()) {
            ret = riter.nextResource().toString();
        }
        return ret;
    }

    public static String getModelWithProxy(String url, Model retModel, ArrayList<String> errorURLs) throws IOException {
        boolean localFileExists = false;
        HashMap<String, String> map = SettingsManager.getInstance().getMapping();
        InputStream ins = null;
        String path = null;
        initModelHash();
        if (loadedModels.containsKey(url)) {
            retModel.add(loadedModels.get(url));
        } else {
            if (map.containsKey(url)) {
                try {
                    ins = new FileInputStream(map.get(url));
                    retModel.read(ins, "");
                    localFileExists = true;
                    ins.close();
                    path = map.get(url);
                    loadedModels.put(url, retModel);
                } catch (IOException ie) {
                }
            }
            if (!localFileExists) {
                if (SettingsManager.getInstance().getDownloadImports()) {
                    path = OWLUtil.getURLandWriteToDisk(url, retModel);
                    map = SettingsManager.getInstance().getMapping();
                    map.put(url, path);
                    SettingsManager.getInstance().setMapping(map);
                    loadedModels.put(url, retModel);
                } else {
                    URL link = new URL(url);
                    ins = link.openStream();
                    retModel.read(ins, "");
                    loadedModels.put(url, retModel);
                }
            }
        }
        retModel.add(OWLUtil.getImportedModels(retModel, errorURLs));
        return path;
    }

    public static Model getImportedModels(Model model, ArrayList<String> errorURLs) throws IOException {
        Model importModel = ModelFactory.createDefaultModel();
        NodeIterator niter = model.listObjectsOfProperty((Resource) null, OWL.imports);
        HashMap<String, Integer> imports = new HashMap<String, Integer>();
        initModelHash();
        while (niter.hasNext()) {
            String url = niter.nextNode().toString();
            if (imports.containsKey(url)) {
            } else {
                imports.put(url, 1);
                boolean localFileExists = false;
                HashMap<String, String> map = SettingsManager.getInstance().getMapping();
                InputStream ins = null;
                Model sub = null;
                if (map.containsKey(url)) {
                    try {
                        ins = new FileInputStream(map.get(url));
                        sub = ModelFactory.createDefaultModel();
                        sub.read(ins, "");
                        localFileExists = true;
                        ins.close();
                    } catch (IOException ie) {
                        System.err.println(ie + " wtf?");
                    }
                }
                if (!localFileExists) {
                    sub = ModelFactory.createDefaultModel();
                    try {
                        OWLUtil.getModelWithProxy(url, sub, errorURLs);
                    } catch (IOException ie) {
                        if (ie.getMessage().equals(url) && errorURLs != null) {
                            errorURLs.add(url);
                        }
                    }
                }
                if (sub != null) {
                    Model subImport;
                    if (localFileExists) {
                        subImport = OWLUtil.getImportedModels(sub, errorURLs);
                        if (subImport != null) {
                            sub.add(subImport);
                        }
                    }
                    importModel.add(sub);
                    try {
                        if (ins != null) {
                            ins.close();
                        }
                    } catch (IOException ie) {
                    }
                }
            }
        }
        return importModel;
    }

    public static ArrayList<String> getSubAndEquivalentProperties(Model model, String propertyURI) {
        ArrayList<String> retList = new ArrayList<String>(), subProps = new ArrayList<String>();
        initModelHash();
        if (model != null) {
            retList.add(propertyURI);
            ResIterator riter = model.listSubjectsWithProperty(OWL.equivalentProperty, model.getResource(propertyURI));
            while (riter.hasNext()) {
                retList.add(riter.nextResource().toString());
            }
            for (String uri : retList) {
                riter = model.listSubjectsWithProperty(RDFS.subPropertyOf, model.getResource(uri));
                while (riter.hasNext()) {
                    subProps.add(riter.nextResource().toString());
                }
            }
            retList.addAll(subProps);
            retList.addAll(getSubLocationURIsHelper(model, subProps));
        }
        return retList;
    }

    protected static ArrayList<String> getSubLocationURIsHelper(Model model, ArrayList<String> uriList) {
        ArrayList<String> temp = new ArrayList<String>(), subProps = new ArrayList<String>(), curLevel = new ArrayList<String>();
        for (String uri : uriList) {
            ResIterator riter = model.listSubjectsWithProperty(OWL.equivalentProperty, model.getResource(uri));
            while (riter.hasNext()) {
                String s = riter.nextResource().toString();
                temp.add(s);
            }
        }
        curLevel.addAll(uriList);
        curLevel.addAll(temp);
        for (String uri : curLevel) {
            ResIterator riter = model.listSubjectsWithProperty(RDFS.subPropertyOf, model.getResource(uri));
            while (riter.hasNext()) {
                subProps.add(riter.nextResource().toString());
            }
        }
        temp.addAll(subProps);
        if (subProps.size() > 0) {
            temp.addAll(getSubLocationURIsHelper(model, subProps));
        }
        return temp;
    }

    public static ArrayList<String> getInferredTypes(Model model, String uriType) {
        ArrayList<String> retList = new ArrayList<String>(), subClasses = new ArrayList<String>();
        initModelHash();
        if (model != null) {
            retList.add(uriType);
            ResIterator riter = model.listSubjectsWithProperty(OWL.equivalentClass, model.getResource(uriType));
            while (riter.hasNext()) {
                retList.add(riter.nextResource().toString());
            }
            for (String uri : retList) {
                riter = model.listSubjectsWithProperty(RDFS.subClassOf, model.getResource(uri));
                while (riter.hasNext()) {
                    subClasses.add(riter.nextResource().toString());
                }
            }
            retList.addAll(subClasses);
            retList.addAll(getLocationTypesHelper(model, subClasses));
        }
        return retList;
    }

    protected static ArrayList<String> getLocationTypesHelper(Model model, ArrayList<String> uriList) {
        ArrayList<String> temp = new ArrayList<String>(), subClasses = new ArrayList<String>(), curLevel = new ArrayList<String>();
        for (String uri : uriList) {
            ResIterator riter = model.listSubjectsWithProperty(OWL.equivalentClass, model.getResource(uri));
            while (riter.hasNext()) {
                String s = riter.nextResource().toString();
                temp.add(s);
            }
        }
        curLevel.addAll(uriList);
        curLevel.addAll(temp);
        for (String uri : curLevel) {
            ResIterator riter = model.listSubjectsWithProperty(RDFS.subClassOf, model.getResource(uri));
            while (riter.hasNext()) {
                subClasses.add(riter.nextResource().toString());
            }
        }
        temp.addAll(subClasses);
        if (subClasses.size() > 0) {
            temp.addAll(getLocationTypesHelper(model, subClasses));
        }
        return temp;
    }

    public static String getLocationType(Model model, String uri) {
        String ret = null;
        Resource resURI = null;
        initModelHash();
        if (model != null) {
            resURI = model.getResource(uri);
            if (resURI != null) {
                NodeIterator niter = model.listObjectsOfProperty(resURI, RDF.type);
                if (niter.hasNext()) {
                    String type = niter.nextNode().toString();
                    ret = checkIsLocation(type);
                    if (ret == null) {
                        ret = getLocationHelper(model, type);
                    }
                }
            }
        }
        return ret;
    }

    protected static String getLocationHelper(Model model, String type) {
        String ret = null;
        Resource resType = null;
        boolean checkSuperclasses = true;
        initModelHash();
        resType = model.getResource(type);
        NodeIterator niter = model.listObjectsOfProperty(resType, OWL.equivalentClass);
        while (niter.hasNext()) {
            String s = checkIsLocation(niter.nextNode().toString());
            if (s != null) {
                ret = s;
                checkSuperclasses = false;
                break;
            }
        }
        if (checkSuperclasses) {
            niter = model.listObjectsOfProperty(resType, RDFS.subClassOf);
            while (niter.hasNext()) {
                String s = getLocationHelper(model, niter.nextNode().toString());
                if (s != null) {
                    ret = s;
                    break;
                }
            }
        }
        return ret;
    }

    protected static String checkIsLocation(String type) {
        String ret = null;
        initModelHash();
        if (type.equals(Conference.CITY)) {
            ret = Conference.CITY;
        } else if (type.equals(Conference.COUNTRY)) {
            ret = Conference.COUNTRY;
        } else if (type.equals(Conference.STATE)) {
            ret = Conference.STATE;
        } else if (type.equals(Conference.PROVINCE)) {
            ret = Conference.PROVINCE;
        }
        return ret;
    }

    public static String getRDFSLabel(Model m, String uri) {
        String retLabel = null;
        NodeIterator niter = m.listObjectsOfProperty(m.getResource(uri), RDFS.label);
        if (niter.hasNext()) {
            retLabel = niter.nextNode().toString();
        }
        if (retLabel != null) {
            if (retLabel.contains("@")) {
                retLabel = retLabel.substring(0, retLabel.indexOf("@"));
            }
        } else {
            retLabel = uri;
        }
        return retLabel;
    }

    public static ArrayList<String> getLocationHierarchyAsList(Model m, String uri, ArrayList<String> subURIs) {
        ArrayList<String> retList = new ArrayList<String>();
        String parentURI = null;
        if (m != null && uri != null && uri.length() > 0) {
            retList.add(uri);
            for (String subURI : subURIs) {
                NodeIterator niter = m.listObjectsOfProperty(m.getResource(uri), m.getProperty(subURI));
                if (niter.hasNext()) {
                    parentURI = niter.nextNode().toString();
                    break;
                }
            }
            if (parentURI != null) {
                retList.add(0, parentURI);
                OWLUtil.getLocationHierarchyAsListHelper(m, parentURI, subURIs, retList);
            }
        }
        return retList;
    }

    protected static void getLocationHierarchyAsListHelper(Model m, String uri, ArrayList<String> subURIs, ArrayList<String> hierarchy) {
        String parentURI = null;
        if (m != null && uri != null && uri.length() > 0) {
            for (String subURI : subURIs) {
                NodeIterator niter = m.listObjectsOfProperty(m.getResource(uri), m.getProperty(subURI));
                if (niter.hasNext()) {
                    parentURI = niter.nextNode().toString();
                    break;
                }
            }
        }
        if (parentURI != null) {
            hierarchy.add(0, parentURI);
            OWLUtil.getLocationHierarchyAsListHelper(m, parentURI, subURIs, hierarchy);
        }
    }
}
