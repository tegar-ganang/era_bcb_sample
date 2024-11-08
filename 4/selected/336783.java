package dms.core.document.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.pdfbox.util.operator.SetMatrix;
import org.springframework.transaction.annotation.Transactional;
import dms.core.document.docmeta.logic.DocMeta;
import dms.core.document.docmeta.logic.DocMetaModel;
import dms.core.document.docversion.logic.DocVersion;
import dms.core.document.docversion.logic.DocVersionModel;
import dms.core.document.folder.logic.Folder;
import dms.core.document.resourceauthority.logic.ResourceAuthority;
import dms.core.document.resourceauthority.logic.ResourceAuthorityModel;
import dms.core.document.resourceauthority.logic.ResourceAuthorityService;
import dms.core.document.route.logic.RouteDoc;
import dms.core.document.route.logic.RouteDocModel;
import dms.core.document.workspace.logic.Workspace;
import dms.core.logic.ServiceBase;
import dms.core.user.logic.User;
import dms.portal.preference.logic.Preference;
import dms.portal.preference.logic.PreferenceModel;
import dms.portal.preference.logic.PreferenceService;
import dms.util.Constants;
import dms.util.Page;

@Transactional
public class DocumentServiceImpl extends ServiceBase implements DocumentService {

    private EntityManager em;

    public EntityManager getEntityManager() {
        return em;
    }

    @PersistenceContext
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    private ResourceAuthorityService resourceAuthorityService;

    private PreferenceService prefService;

    private static Logger log = Logger.getLogger(DocumentServiceImpl.class);

    @Override
    @SuppressWarnings("unchecked")
    public List<Document> findAll() {
        List<Document> documents = new ArrayList<Document>();
        documents = em.createQuery("FROM DocumentModel doc WHERE doc.status=:status ORDER BY doc.name").setParameter(Document.FLD_STATUS, Constants.STATUS_ACTIVE).getResultList();
        return documents;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Document> findByName(String name) {
        List<Document> documents = new ArrayList<Document>();
        documents = em.createQuery("FROM DocumentModel doc WHERE doc.status=:status AND doc.name like :name ORDER BY doc.name").setParameter(Document.FLD_STATUS, Constants.STATUS_ACTIVE).setParameter(Document.FLD_NAME, "%" + name + "%").getResultList();
        return documents;
    }

    @SuppressWarnings("unchecked")
    private List<Document> findByFolder(Folder folder) {
        List<Document> documents = new ArrayList<Document>();
        if (folder != null && folder.getId() != null) {
            documents = em.createQuery("FROM DocumentModel doc WHERE doc.status=:status AND doc.folder=:folder ORDER BY doc.name").setParameter(Document.FLD_STATUS, Constants.STATUS_ACTIVE).setParameter(Document.FLD_FOLDER, folder).getResultList();
        }
        return documents;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page findByFolder(Folder folder, User user, int pgNo, int pgSize, String status) throws Exception {
        List<Document> documents = new ArrayList<Document>();
        int totalRows = 0;
        Map<Long, List<ResourceAuthority>> resourceAuthorities = new TreeMap<Long, List<ResourceAuthority>>();
        if (folder != null && folder.getId() != null && user != null && user.getId() != null) {
            documents = em.createQuery("FROM DocumentModel doc WHERE doc.status=:status AND doc.folder=:folder AND " + "doc.id IN " + "( " + "SELECT ra.document.id FROM ResourceAuthorityModel ra WHERE " + "( " + "ra.user = :user OR ra.group.id IN " + "( " + "SELECT ugroup.group.id FROM UserGroupModel ugroup WHERE ugroup.user=:user " + ") " + "OR ra.group.id IN (1,3) " + ") AND ra.document IS NOT NULL AND ra.status=:status " + ") " + "ORDER BY doc.name").setParameter(Document.FLD_STATUS, status).setParameter("user", user).setParameter(Document.FLD_FOLDER, folder).setFirstResult((pgNo - 1) * pgSize).setMaxResults(pgSize).getResultList();
            totalRows = countByFolder(folder, user);
            List<ResourceAuthority> authorities = new ArrayList<ResourceAuthority>();
            for (Document doc : documents) {
                authorities = resourceAuthorityService.findByResourceUser(doc, user);
                resourceAuthorities.put(doc.getId(), authorities);
            }
        }
        return new Page(pgNo, pgSize, documents, totalRows, resourceAuthorities);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page findRecycleBinByWorkspace(Workspace workspace, User user, int pgNo, int pgSize) throws Exception {
        int totalRows = 0;
        Map<Long, List<ResourceAuthority>> resourceAuthorities = new TreeMap<Long, List<ResourceAuthority>>();
        List<Document> documents = new ArrayList<Document>();
        if (workspace != null && workspace.getId() != null && user != null && user.getId() != null) {
            documents = em.createQuery("FROM DocumentModel doc WHERE doc.status=:status AND doc.workspace=:workspace AND doc.id IN (" + "SELECT ra.document.id FROM ResourceAuthorityModel ra WHERE (ra.user=:user or " + "ra.group.id IN (SELECT ugroup.group.id FROM UserGroupModel ugroup WHERE ugroup.user=:user) " + "OR ra.group.id IN (1,3)))").setParameter(Document.FLD_STATUS, Constants.STATUS_INACTIVE).setParameter(Document.FLD_WORKSPACE, workspace).setParameter("user", user).setFirstResult((pgNo - 1) * pgSize).setMaxResults(pgSize).getResultList();
            totalRows = countRecycleBinByWorkspace(workspace, user);
            List<ResourceAuthority> authorities = new ArrayList<ResourceAuthority>();
            for (Document doc : documents) {
                authorities = resourceAuthorityService.findByResourceUser(doc, user);
                resourceAuthorities.put(doc.getId(), authorities);
            }
        }
        return new Page(pgNo, pgSize, documents, totalRows, resourceAuthorities);
    }

    private int countByFolder(Folder folder, User user) throws Exception {
        int totalRows = 0;
        if (folder != null && folder.getId() != null && user != null && user.getId() != null) {
            totalRows = ((Long) em.createQuery("SELECT COUNT(doc.id) FROM DocumentModel doc WHERE doc.status=:status AND doc.folder=:folder AND " + "doc.id IN " + "( " + "SELECT ra.document.id FROM ResourceAuthorityModel ra WHERE " + "( " + "ra.user = :user OR ra.group.id IN " + "( " + "SELECT ugroup.group.id FROM UserGroupModel ugroup WHERE ugroup.user=:user " + ") " + "OR ra.group.id IN (1,3) " + ") AND ra.document IS NOT NULL AND ra.status=:status " + ") " + "ORDER BY doc.name").setParameter(Document.FLD_STATUS, Constants.STATUS_ACTIVE).setParameter("user", user).setParameter(Document.FLD_FOLDER, folder).getSingleResult()).intValue();
        }
        return totalRows;
    }

    private int countRecycleBinByWorkspace(Workspace workspace, User user) throws Exception {
        int totalRows = ((Long) em.createQuery("SELECT COUNT(doc.id) FROM DocumentModel doc WHERE doc.status=:status AND doc.workspace=:workspace AND doc.id IN (" + "SELECT ra.document.id FROM ResourceAuthorityModel ra WHERE (ra.user=:user or " + "ra.group.id IN (SELECT ugroup.group.id FROM UserGroupModel ugroup WHERE ugroup.user=:user) " + "OR ra.group.id IN (1,3)))").setParameter(Document.FLD_STATUS, Constants.STATUS_INACTIVE).setParameter(Document.FLD_WORKSPACE, workspace).setParameter("user", user).getSingleResult()).intValue();
        return totalRows;
    }

    @Override
    public Document resetDuplicateDocName(Document document) {
        boolean isDuplicate = false;
        if (document.getFolder() != null) {
            String name = document.getName();
            List<Document> documents = findByFolder(document.getFolder());
            for (Iterator<Document> iter = documents.iterator(); iter.hasNext(); ) {
                Document doc = iter.next();
                if (doc != null && doc.getName().equals(name)) {
                    isDuplicate = true;
                    break;
                }
            }
            if (isDuplicate) {
                if (name.indexOf('(') != -1 && name.indexOf(')') != -1) {
                    String szIndex = name.substring(name.indexOf('(') + 1, name.indexOf(')'));
                    Integer index = null;
                    try {
                        index = new Integer(szIndex);
                        name = name.substring(0, name.indexOf('(') + 1);
                        index += 1;
                        String szNewName = name + index.toString() + ")." + document.getExt();
                        while (isDuplicate) {
                            isDuplicate = false;
                            for (Iterator<Document> iter = documents.iterator(); iter.hasNext(); ) {
                                Document doc = iter.next();
                                if (doc != null && doc.getName().equals(szNewName)) {
                                    isDuplicate = true;
                                    index += 1;
                                    szNewName = name + index.toString() + ")." + document.getExt();
                                    break;
                                }
                            }
                        }
                        name = szNewName;
                        document.setName(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return document;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean removeAll(Document document) throws Exception {
        log.info("removeAll doc id:" + document.getId());
        List<DocVersion> versions = new ArrayList<DocVersion>();
        versions = em.createQuery("FROM DocVersionModel v WHERE v.document=:document").setParameter(DocVersion.FLD_DOCUMENT, document).getResultList();
        for (Iterator<DocVersion> iter = versions.iterator(); iter.hasNext(); ) {
            DocVersion version = iter.next();
            if (version != null && version.getId() != null) {
                em.remove(version);
            }
        }
        List<ResourceAuthority> authorities = new ArrayList<ResourceAuthority>();
        authorities = em.createQuery("FROM ResourceAuthorityModel r WHERE r.document=:document").setParameter(ResourceAuthority.FLD_DOCUMENT, document).getResultList();
        for (Iterator<ResourceAuthority> iter = authorities.iterator(); iter.hasNext(); ) {
            ResourceAuthority auth = iter.next();
            if (auth != null && auth.getId() != null) {
                em.remove(auth);
            }
        }
        List<DocMeta> metas = new ArrayList<DocMeta>();
        metas = em.createQuery("FROM DocMetaModel meta WHERE meta.document=:document").setParameter(DocMeta.FLD_DOCUMENT, document).getResultList();
        for (Iterator<DocMeta> iter = metas.iterator(); iter.hasNext(); ) {
            DocMeta meta = iter.next();
            if (meta != null && meta.getId() != null) {
                em.remove(meta);
            }
        }
        List<RouteDoc> routeDocs = new ArrayList<RouteDoc>();
        routeDocs = em.createQuery("FROM RouteDocModel route WHERE route.document=:document").setParameter(RouteDoc.FLD_DOCUMENT, document).getResultList();
        for (Iterator<RouteDoc> iter = routeDocs.iterator(); iter.hasNext(); ) {
            RouteDoc routeDoc = iter.next();
            if (routeDoc != null && routeDoc.getId() != null) {
                em.remove(routeDoc);
            }
        }
        document = find(document.getId());
        em.remove(document);
        return false;
    }

    @Override
    public Document find(Long id) throws Exception {
        return em.find(DocumentModel.class, id);
    }

    @Override
    public void inactive(Document document) throws Exception {
        if (document.getId() != null) {
            document = find(document.getId());
            document.setStatus(Constants.STATUS_INACTIVE);
            em.merge(document);
        }
    }

    @Override
    public boolean restore(Document document) throws Exception {
        boolean isRestored = false;
        if (document.getId() != null) {
            document = find(document.getId());
            document.setStatus(Constants.STATUS_ACTIVE);
            em.merge(document);
        }
        return isRestored;
    }

    @Override
    public boolean copy(Document document, Folder folder) throws Exception {
        boolean isCopied = false;
        if (document.getId() != null && folder.getId() != null) {
            Document copiedDoc = new DocumentModel();
            copiedDoc.setValues(document.getValues());
            copiedDoc.setFolder(folder);
            copiedDoc.setId(null);
            em.persist(copiedDoc);
            resourceAuthorityService.applyAuthority(copiedDoc);
            List<Preference> preferences = prefService.findAll();
            Preference preference = new PreferenceModel();
            if (preferences != null && !preferences.isEmpty()) {
                preference = preferences.get(0);
            }
            String repo = preference.getRepository();
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATEFORMAT_YYYYMMDD);
            Calendar calendar = Calendar.getInstance();
            StringBuffer sbRepo = new StringBuffer(repo);
            sbRepo.append(File.separator);
            StringBuffer sbFolder = new StringBuffer(sdf.format(calendar.getTime()));
            sbFolder.append(File.separator).append(calendar.get(Calendar.HOUR_OF_DAY));
            File fFolder = new File(sbRepo.append(sbFolder).toString());
            if (!fFolder.exists()) {
                fFolder.mkdirs();
            }
            copiedDoc.setLocation(sbFolder.toString());
            em.merge(copiedDoc);
            File in = new File(repo + File.separator + document.getLocation() + File.separator + document.getId() + "." + document.getExt());
            File out = new File(fFolder.getAbsolutePath() + File.separator + copiedDoc.getId() + "." + copiedDoc.getExt());
            FileChannel inChannel = new FileInputStream(in).getChannel();
            FileChannel outChannel = new FileOutputStream(out).getChannel();
            try {
                inChannel.transferTo(0, inChannel.size(), outChannel);
            } catch (IOException e) {
                throw e;
            } finally {
                if (inChannel != null) inChannel.close();
                if (outChannel != null) outChannel.close();
            }
        }
        return isCopied;
    }

    @Override
    public boolean cut(Document document, Folder folder) throws Exception {
        boolean isCut = false;
        if (document.getId() != null && folder.getId() != null) {
            document = find(document.getId());
            document.setFolder(folder);
            em.merge(document);
        }
        return isCut;
    }

    @Override
    public boolean save(Document document) throws Exception {
        if (document.getId() == null) {
            em.persist(document);
        } else {
            em.merge(document);
        }
        return true;
    }

    public ResourceAuthorityService getResourceAuthorityService() {
        return resourceAuthorityService;
    }

    public void setResourceAuthorityService(ResourceAuthorityService resourceAuthorityService) {
        this.resourceAuthorityService = resourceAuthorityService;
    }

    public PreferenceService getPrefService() {
        return prefService;
    }

    public void setPrefService(PreferenceService prefService) {
        this.prefService = prefService;
    }
}
