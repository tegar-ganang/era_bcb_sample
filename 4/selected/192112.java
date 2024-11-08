package com.safi.asterisk.handler.importing;

import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.xmi.UnresolvedReferenceException;
import org.eclipse.emf.ecore.xmi.XMIException;
import org.eclipse.emf.ecore.xmi.XMIResource;
import org.eclipse.emf.ecore.xmi.XMLLoad;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.SAXXMIHandler;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLLoadImpl;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.hibernate.Session;
import org.sadun.util.MovedFile;
import org.sadun.util.polling.BasePollManager;
import org.sadun.util.polling.FileMovedEvent;
import org.xml.sax.helpers.DefaultHandler;
import com.safi.asterisk.AsteriskPackage;
import com.safi.asterisk.actionstep.ActionstepPackage;
import com.safi.asterisk.handler.GlobalVariableManager;
import com.safi.asterisk.handler.SafletEngine;
import com.safi.asterisk.handler.util.FileUtils;
import com.safi.asterisk.initiator.InitiatorPackage;
import com.safi.asterisk.saflet.SafletPackage;
import com.safi.core.actionstep.ActionStepPackage;
import com.safi.core.scripting.ScriptingPackage;
import com.safi.db.DBConnection;
import com.safi.db.DBDriver;
import com.safi.db.DbPackage;
import com.safi.db.Query;
import com.safi.db.SafiDriverManager;
import com.safi.db.Variable;
import com.safi.db.manager.DBManager;
import com.safi.db.server.config.ConfigPackage;
import com.safi.db.server.config.Prompt;
import com.safi.db.server.config.Saflet;
import com.safi.db.server.config.SafletProject;
import de.schlichtherle.io.ArchiveDetector;
import de.schlichtherle.io.ArchiveException;
import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;

public class SafiArchiveImporter extends BasePollManager {

    private static final Logger log = Logger.getLogger(SafiArchiveImporter.class);

    protected static SafletPackage handlerPackage = SafletPackage.eINSTANCE;

    protected static com.safi.core.saflet.SafletPackage handlerPackage2 = com.safi.core.saflet.SafletPackage.eINSTANCE;

    protected static ActionStepPackage toolstepPackage = ActionStepPackage.eINSTANCE;

    protected static ActionstepPackage astToolstepPackage = ActionstepPackage.eINSTANCE;

    protected static InitiatorPackage initiatorPackage = InitiatorPackage.eINSTANCE;

    protected static ScriptingPackage scriptingPackage = ScriptingPackage.eINSTANCE;

    protected static DbPackage dbPackage = DbPackage.eINSTANCE;

    protected static ConfigPackage cfPackage = ConfigPackage.eINSTANCE;

    protected static AsteriskPackage asteriskPackage = AsteriskPackage.eINSTANCE;

    static {
        File.setDefaultArchiveDetector(new DefaultArchiveDetector(ArchiveDetector.NULL, new String[] { "sar", "de.schlichtherle.io.archive.zip.JarDriver" }));
    }

    private Map<Object, Object> optionMap;

    public SafiArchiveImporter() {
        super();
    }

    @Override
    public void fileMoved(FileMovedEvent evt) {
        MovedFile moved = evt.getMovedFile();
        java.io.File f = moved.getDestinationPath();
        try {
            doImport(f, OverwriteMode.FAIL);
        } catch (Exception e) {
        }
    }

    public void doImport(java.io.File f, OverwriteMode mode) throws ImportArchiveException {
        File archive = new File(f);
        String archivePath = f.getAbsolutePath();
        File dbDir = new File(archivePath + "/db");
        Session session = null;
        try {
            session = DBManager.getInstance().createSession();
            session.beginTransaction();
            String[] files = dbDir.list(new DBFilter());
            if (files != null && files.length > 0) {
                FileInputStream fo = null;
                try {
                    fo = new FileInputStream(archivePath + "/db/" + files[0]);
                    SafiDriverManager loadedManager = loadManager(fo);
                    if (loadedManager != null) {
                        SafiDriverManager manager = DBManager.getInstance().getDriverManagerFromDB(session);
                        List<DBDriver> drivers = new ArrayList<DBDriver>(loadedManager.getDrivers());
                        for (DBDriver d : drivers) {
                            DBDriver existingDriver = manager.getDriver(d.getName());
                            if (existingDriver == null) {
                                manager.getDrivers().add(d);
                                session.saveOrUpdate(manager);
                            } else {
                                log.warn("Driver " + existingDriver.getName() + " already exists");
                                List<DBConnection> connections = new ArrayList<DBConnection>(d.getConnections());
                                for (DBConnection conn : connections) {
                                    DBConnection existingConnection = existingDriver.getConnection(conn.getName());
                                    if (existingConnection == null) {
                                        existingDriver.getConnections().add(conn);
                                        session.saveOrUpdate(existingDriver);
                                    } else {
                                        if (mode == OverwriteMode.FAIL) throw new ImportArchiveException(ImportArchiveException.Type.RESOURCE_EXISTS, "Connection " + existingConnection.getName() + " already exists for driver " + existingDriver.getName());
                                        log.error("Connection " + existingConnection.getName() + " already exists for driver " + existingDriver.getName());
                                        if (mode == OverwriteMode.SKIP) {
                                            List<Query> queries = new ArrayList<Query>(conn.getQueries());
                                            for (Query q : queries) {
                                                Query existingQuery = existingConnection.getQuery(q.getName());
                                                if (existingQuery == null) {
                                                    existingConnection.getQueries().add(q);
                                                    session.saveOrUpdate(existingConnection);
                                                } else {
                                                    log.warn("Query " + q.getName() + " already exists for connection " + existingConnection.getName());
                                                }
                                            }
                                        } else {
                                            existingDriver.getConnections().remove(existingConnection);
                                            session.delete(existingConnection);
                                            existingDriver.getConnections().add(conn);
                                            session.save(existingDriver);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    if (fo != null) try {
                        fo.close();
                    } catch (IOException e) {
                    }
                }
            }
            {
                File globalDir = new File(archivePath + "/globals");
                if (globalDir.exists()) {
                    files = globalDir.list(new GlobalVarFilter());
                    if (files != null && files.length > 0) {
                        for (String file : files) {
                            FileInputStream fo = new FileInputStream(archivePath + "/globals/" + file);
                            Variable v = loadVariable(fo);
                            if (v == null) continue;
                            Variable oldVar = GlobalVariableManager.getInstance().getGlobalVariable(v.getName(), true);
                            if (oldVar != null) {
                                if (mode == OverwriteMode.OVERWRITE) GlobalVariableManager.getInstance().deleteGlobalVariable(oldVar); else if (mode == OverwriteMode.FAIL) throw new ImportArchiveException(ImportArchiveException.Type.RESOURCE_EXISTS, "Global Variable named " + oldVar.getName() + " already exists on this server"); else if (mode == OverwriteMode.SKIP) continue;
                            }
                            GlobalVariableManager.getInstance().addGlobalVariable(v);
                        }
                    }
                }
            }
            File prjDir = new File(archivePath + "/projects");
            files = prjDir.list(new PrjFilter());
            if (files != null && files.length > 0) {
                FileInputStream fo = null;
                for (String file : files) {
                    try {
                        fo = new FileInputStream(archivePath + "/projects/" + file);
                        List<SafletProject> projects = loadProjects(fo);
                        for (SafletProject sp : projects) {
                            SafletProject existingProject = DBManager.getInstance().getSafletProject(session, sp.getName());
                            if (existingProject == null) {
                                DBManager.getInstance().saveOrUpdateServerResource(session, sp);
                            } else {
                                if (mode == OverwriteMode.OVERWRITE) {
                                    session.delete(existingProject);
                                    DBManager.getInstance().saveOrUpdateServerResource(session, sp);
                                } else if (mode == OverwriteMode.FAIL) throw new ImportArchiveException(ImportArchiveException.Type.RESOURCE_EXISTS, "SafletProject named " + sp.getName() + " already exists on this server"); else {
                                    boolean saveProject = false;
                                    List<Saflet> saflets = new ArrayList<Saflet>(sp.getSaflets());
                                    for (Saflet s : saflets) {
                                        if (!hasSaflet(existingProject, s.getName())) {
                                            existingProject.getSaflets().add(s);
                                            saveProject = true;
                                        } else {
                                            log.warn("Project " + existingProject.getName() + " alread contains Saflet named " + s.getName() + ". Import failed");
                                        }
                                    }
                                    if (saveProject) DBManager.getInstance().saveOrUpdateServerResource(session, existingProject);
                                }
                            }
                        }
                    } finally {
                        if (fo != null) try {
                            fo.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            File promptDir = new File(archivePath + "/prompts");
            files = promptDir.list(new PromptFilter());
            if (files != null && files.length > 0) {
                for (String file : files) {
                    FileInputStream fo = null;
                    try {
                        fo = new FileInputStream(archivePath + "/prompts/" + file);
                        Prompt prompt = loadPrompt(fo);
                        if (prompt != null) {
                            Prompt existingPrompt = DBManager.getInstance().getPromptByName(session, prompt.getName());
                            if (existingPrompt != null && mode == OverwriteMode.FAIL) {
                                throw new ImportArchiveException(ImportArchiveException.Type.RESOURCE_EXISTS, "Prompt " + existingPrompt.getName() + " already exists. Import from " + archive + " failed");
                            } else {
                                if (existingPrompt != null && mode == OverwriteMode.OVERWRITE) {
                                    log.warn("Prompt " + existingPrompt.getName() + " already exists. overwriting");
                                    DBManager.getInstance().delete(session, existingPrompt);
                                }
                                prompt.setId(-1);
                                if (prompt.getProject() != null) {
                                    String pname = prompt.getProject().getName();
                                    SafletProject sp = DBManager.getInstance().getSafletProject(session, pname);
                                    sp.getPrompts().add(prompt);
                                    DBManager.getInstance().saveOrUpdateServerResource(session, prompt);
                                    DBManager.getInstance().saveOrUpdateServerResource(session, sp);
                                } else {
                                    DBManager.getInstance().saveOrUpdateServerResource(session, prompt);
                                }
                            }
                        }
                    } finally {
                        if (fo != null) try {
                            fo.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            {
                String prefix = archivePath + "/prompts";
                File audioDir = new File(prefix);
                String audioDirRoot = SafletEngine.getInstance().getAudioDirectoryRoot();
                if (!audioDirRoot.endsWith("/")) audioDirRoot += "/";
                String initialPath = archivePath;
                if (!initialPath.endsWith("/")) initialPath += "/";
                if (mode == OverwriteMode.FAIL) {
                    String filename = filesExist(initialPath, "prompts/audio/", "", audioDirRoot, false);
                    if (filename != null) throw new ImportArchiveException(ImportArchiveException.Type.RESOURCE_EXISTS, "AudioFile " + filename + " already exists. Import from " + archive + " failed");
                    audioDir.copyTo(new File(audioDirRoot));
                } else if (mode == OverwriteMode.OVERWRITE) {
                    audioDir.copyTo(new File(audioDirRoot));
                } else if (mode == OverwriteMode.SKIP) {
                    filesExist(initialPath, "prompts/audio/", "", audioDirRoot, true);
                }
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            log.error("Exception caught while importing archive ", e);
            if (session != null && session.getTransaction() != null) session.getTransaction().rollback();
            if (e instanceof ImportArchiveException) throw (ImportArchiveException) e;
            throw new ImportArchiveException(ImportArchiveException.Type.SYSTEM, "Exception caught while importing archive " + archive, e);
        } finally {
            try {
                File.umount(archive, true);
            } catch (ArchiveException e) {
                e.printStackTrace();
            }
            try {
                if (session != null) session.close();
            } catch (Exception e2) {
            }
        }
    }

    private String filesExist(final String pathPrefix, final String startingFrom, String currentSubDir, final String destRoot, final boolean doCopy) throws ImportArchiveException {
        File source = new File(pathPrefix + startingFrom + currentSubDir);
        File[] files = (File[]) source.listFiles();
        if (files != null && files.length > 0) for (File file : files) {
            if (file.isFile()) {
                File destFile = new File(destRoot + currentSubDir + file.getName());
                if (destFile.exists()) {
                    if (!doCopy) return destFile.getAbsolutePath();
                } else {
                    try {
                        destFile.getParentFile().mkdirs();
                        FileUtils.copyStreams(new FileInputStream(file), new FileOutputStream(destFile));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                String enclEntryName = file.getEnclEntryName().substring(startingFrom.length());
                if (!enclEntryName.endsWith("/")) enclEntryName += "/";
                String result = filesExist(pathPrefix, startingFrom, enclEntryName, destRoot, doCopy);
                if (!doCopy) return result;
            }
        }
        return null;
    }

    private boolean filesExistHelper(File source, File dest) throws ImportArchiveException {
        return false;
    }

    private Prompt loadPrompt(FileInputStream fo) {
        Resource res = getResource();
        try {
            res.load(fo, getLoadOptionMap());
            List<EObject> list = res.getContents();
            if (list.isEmpty()) return null;
            SafletProject proj = null;
            Prompt p = null;
            for (EObject obj : list) {
                if (obj instanceof Prompt) p = (Prompt) obj; else if (obj instanceof SafletProject) proj = (SafletProject) obj;
            }
            if (proj != null && p.getProject() != proj) {
                p.setProject(proj);
            }
            return p;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private Variable loadVariable(FileInputStream fo) {
        Resource res = getResource();
        try {
            res.load(fo, getLoadOptionMap());
            List<EObject> list = res.getContents();
            if (list.isEmpty()) return null;
            for (EObject obj : list) {
                if (obj instanceof Variable) return (Variable) obj;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private boolean hasSaflet(SafletProject existingProject, String name) {
        for (Saflet s : existingProject.getSaflets()) {
            if (StringUtils.equals(name, s.getName())) return true;
        }
        return false;
    }

    private List<SafletProject> loadProjects(FileInputStream fo) {
        Resource res = getResource();
        try {
            res.load(fo, getLoadOptionMap());
            List<EObject> list = res.getContents();
            if (list.isEmpty()) return null;
            List<SafletProject> projects = new ArrayList<SafletProject>();
            for (EObject o : list) {
                if (o instanceof SafletProject) projects.add((SafletProject) o);
            }
            return projects;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private SafiDriverManager loadManager(FileInputStream f) {
        Resource res = getResource();
        try {
            res.load(f, getLoadOptionMap());
            List<EObject> list = res.getContents();
            if (list.isEmpty()) return null;
            EObject obj = list.get(0);
            if (obj instanceof SafiDriverManager) return (SafiDriverManager) obj;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    public Map<Object, Object> getLoadOptionMap() {
        if (optionMap == null) {
            optionMap = new HashMap<Object, Object>();
            optionMap.put(XMLResource.OPTION_RECORD_UNKNOWN_FEATURE, Boolean.TRUE);
            optionMap.put(XMIResource.OPTION_LAX_FEATURE_PROCESSING, Boolean.TRUE);
            optionMap.putAll(new XMLResourceImpl().getDefaultLoadOptions());
        }
        return optionMap;
    }

    public Resource getResource() {
        XMIResourceImpl xmiResourceLoader = new XMIResourceImpl() {

            @Override
            protected XMLLoad createXMLLoad() {
                return new XMLLoadImpl(createXMLHelper()) {

                    @Override
                    protected DefaultHandler makeDefaultHandler() {
                        return new SAXXMIHandler(resource, helper, options) {

                            @Override
                            public void error(XMIException e) {
                                System.err.println("gotttatata");
                                e.printStackTrace();
                                if (e instanceof UnresolvedReferenceException) return;
                                super.error(e);
                            }

                            @Override
                            protected EPackage handleMissingPackage(String uriString) {
                                EPackage pack = super.handleMissingPackage(uriString);
                                System.err.println("Missing package wuz " + uriString + " but got " + pack);
                                return pack;
                            }
                        };
                    }
                };
            }
        };
        return xmiResourceLoader;
    }

    class DBFilter implements FilenameFilter {

        @Override
        public boolean accept(java.io.File dir, String name) {
            return name != null && name.endsWith(".db");
        }
    }

    class PrjFilter implements FilenameFilter {

        @Override
        public boolean accept(java.io.File dir, String name) {
            return name != null && name.endsWith(".spj");
        }
    }

    class PromptFilter implements FilenameFilter {

        @Override
        public boolean accept(java.io.File dir, String name) {
            return name != null && name.endsWith(".prompt");
        }
    }

    class GlobalVarFilter implements FilenameFilter {

        @Override
        public boolean accept(java.io.File dir, String name) {
            return name != null && name.endsWith(".gbl");
        }
    }
}
