package org.dcm4chee.web.dao.folder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import net.sf.json.JSONObject;
import org.dcm4chee.archive.entity.StudyPermission;
import org.dcm4chee.usr.model.Role;
import org.jboss.annotation.ejb.LocalBinding;
import org.jboss.system.server.ServerConfigLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Robert David <robert.david@agfa.com>
 * @version $Revision$ $Date$
 * @since 05.10.2010
 */
@Stateless
@LocalBinding(jndiBinding = StudyPermissionsLocal.JNDI_NAME)
public class StudyPermissionsBean implements StudyPermissionsLocal {

    private static Logger log = LoggerFactory.getLogger(StudyPermissionsBean.class);

    @PersistenceContext(unitName = "dcm4chee-arc")
    private EntityManager em;

    private File dicomRolesFile;

    @SuppressWarnings("unused")
    @PostConstruct
    private void config() {
        if (this.dicomRolesFile == null) {
            dicomRolesFile = new File(System.getProperty("dcm4chee-web3.cfg.path", "conf/dcm4chee-web3") + "roles.json");
            if (!dicomRolesFile.isAbsolute()) dicomRolesFile = new File(ServerConfigLocator.locate().getServerHomeDir(), dicomRolesFile.getPath());
            if (log.isDebugEnabled()) {
                log.debug("mappingFile:" + dicomRolesFile);
            }
            if (!dicomRolesFile.exists()) {
                try {
                    if (dicomRolesFile.getParentFile().mkdirs()) log.info("M-WRITE dir:" + dicomRolesFile.getParent());
                    dicomRolesFile.createNewFile();
                } catch (IOException e) {
                    log.error("Roles file doesn't exist and can't be created!", e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<StudyPermission> getStudyPermissions(String studyInstanceUID) {
        return (List<StudyPermission>) em.createQuery("SELECT sp FROM StudyPermission sp WHERE sp.studyInstanceUID = :studyInstanceUID").setParameter("studyInstanceUID", studyInstanceUID).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<StudyPermission> getStudyPermissionsForPatient(long pk) {
        return (List<StudyPermission>) em.createQuery("SELECT sp FROM StudyPermission sp, Study s WHERE sp.studyInstanceUID = s.studyInstanceUID AND s.patientFk = :pk ORDER BY sp.role, sp.action").setParameter("pk", pk).getResultList();
    }

    public void grant(StudyPermission studyPermission) {
        em.persist(studyPermission);
    }

    public void revoke(long pk) {
        this.em.createQuery("DELETE FROM StudyPermission sp WHERE sp.pk = :pk").setParameter("pk", pk).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<String> grantForPatient(long pk, String action, String role) {
        List<String> suids = (List<String>) em.createQuery("SELECT s.studyInstanceUID FROM Study s WHERE s.patientFk = :pk AND s.studyInstanceUID NOT IN(SELECT sp.studyInstanceUID FROM StudyPermission sp WHERE sp.action = :action AND sp.role = :role)").setParameter("pk", pk).setParameter("action", action).setParameter("role", role).getResultList();
        for (String studyInstanceUID : suids) {
            StudyPermission sp = new StudyPermission();
            sp.setAction(action);
            sp.setRole(role);
            sp.setStudyInstanceUID(studyInstanceUID);
            em.persist(sp);
        }
        return suids;
    }

    @SuppressWarnings("unchecked")
    public List<String> revokeForPatient(long pk, String action, String role) {
        List<String> suids = (List<String>) em.createQuery("SELECT s.studyInstanceUID FROM Study s WHERE s.patientFk = :pk AND s.studyInstanceUID NOT IN(SELECT sp.studyInstanceUID FROM StudyPermission sp WHERE sp.action = :action AND sp.role = :role)").setParameter("pk", pk).setParameter("action", action).setParameter("role", role).getResultList();
        this.em.createQuery("DELETE FROM StudyPermission sp WHERE sp.studyInstanceUID IN(SELECT s.studyInstanceUID FROM Study s WHERE s.patientFk = :pk) AND sp.action = :action AND sp.role = :role").setParameter("pk", pk).setParameter("action", action).setParameter("role", role).executeUpdate();
        return suids;
    }

    public long countStudiesOfPatient(long pk) {
        return (Long) em.createQuery("SELECT COUNT(s) FROM Patient p, IN(p.studies) s WHERE p.pk = :pk").setParameter("pk", pk).getSingleResult();
    }

    public List<String> getAllDicomRolenames() {
        List<String> dicomRolenames = new ArrayList<String>();
        for (Role dicomRole : getAllDicomRoles()) dicomRolenames.add(dicomRole.getRolename());
        return dicomRolenames;
    }

    public List<Role> getAllDicomRoles() {
        BufferedReader reader = null;
        try {
            List<Role> roleList = new ArrayList<Role>();
            String line;
            reader = new BufferedReader(new FileReader(dicomRolesFile));
            while ((line = reader.readLine()) != null) {
                Role role = (Role) JSONObject.toBean(JSONObject.fromObject(line), Role.class);
                if (role.isDicomRole()) roleList.add(role);
            }
            Collections.sort(roleList);
            return roleList;
        } catch (Exception e) {
            log.error("Can't get dicom roles from roles file!", e);
            return null;
        } finally {
            close(reader, "dicom roles file reader");
        }
    }

    @SuppressWarnings("unchecked")
    public void updateDicomRoles() {
        List<String> dicomRolenames = getAllDicomRolenames();
        List<String> newRoles = (dicomRolenames.size() == 0) ? em.createQuery("SELECT DISTINCT sp.role FROM StudyPermission sp").getResultList() : em.createQuery("SELECT DISTINCT sp.role FROM StudyPermission sp WHERE sp.role NOT IN(:dicomRoles)").setParameter("dicomRoles", dicomRolenames).getResultList();
        log.info("dicomRolenames:" + dicomRolenames);
        log.info("newRoles:" + newRoles);
        List<Role> roles = new ArrayList<Role>();
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(dicomRolesFile));
            while ((line = reader.readLine()) != null) {
                Role role = (Role) JSONObject.toBean(JSONObject.fromObject(line), Role.class);
                if (newRoles.contains(role.getRolename())) {
                    role.setDicomRole(true);
                    newRoles.remove(role.getRolename());
                }
                roles.add(role);
            }
            if (close(reader, "roles file reader")) reader = null;
            log.info("newRoles to add:" + newRoles);
            for (String rolename : newRoles) {
                Role role = new Role(rolename);
                role.setDicomRole(true);
                roles.add(role);
            }
            Collections.sort(roles);
            log.info("save Roles:" + roles);
            save(roles);
        } catch (Exception e) {
            log.error("Can't get roles from roles file!", e);
            return;
        } finally {
            close(reader, "roles file reader in finally");
        }
    }

    public void addDicomRole(String rolename) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(dicomRolesFile, true));
            Role role = new Role(rolename);
            role.setDicomRole(true);
            JSONObject jsonObject = JSONObject.fromObject(role);
            writer.write(jsonObject.toString());
            writer.newLine();
        } catch (IOException e) {
            log.error("Can't add dicom role to roles file!", e);
        } finally {
            close(writer, "roles file reader");
        }
    }

    public void removeDicomRole(Role role) {
        List<Role> roles = new ArrayList<Role>();
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(dicomRolesFile));
            while ((line = reader.readLine()) != null) {
                Role currentRole = (Role) JSONObject.toBean(JSONObject.fromObject(line), Role.class);
                if (currentRole.equals(role)) currentRole.setDicomRole(false);
                roles.add(currentRole);
            }
        } catch (Exception e) {
            log.error("Can't get roles from roles file!", e);
            return;
        } finally {
            close(reader, "roles file reader");
        }
        save(roles);
    }

    private void save(List<Role> roles) {
        BufferedWriter writer = null;
        try {
            File tmpFile = File.createTempFile(dicomRolesFile.getName(), null, dicomRolesFile.getParentFile());
            log.info("tmpFile:" + tmpFile);
            writer = new BufferedWriter(new FileWriter(tmpFile, true));
            JSONObject jsonObject;
            for (int i = 0, len = roles.size(); i < len; i++) {
                jsonObject = JSONObject.fromObject(roles.get(i));
                writer.write(jsonObject.toString());
                writer.newLine();
            }
            if (close(writer, "Temporary roles file")) writer = null;
            dicomRolesFile.delete();
            tmpFile.renameTo(dicomRolesFile);
            log.info("dicomRolesFile:" + dicomRolesFile);
        } catch (IOException e) {
            log.error("Can't save roles in roles file!", e);
        } finally {
            close(writer, "Temporary roles file (in finally block)");
        }
    }

    private boolean close(Closeable toClose, String desc) {
        log.debug("Closing ", desc);
        if (toClose != null) {
            try {
                toClose.close();
                return true;
            } catch (IOException ignore) {
                log.warn("Error closing : " + desc, ignore);
            }
        }
        return false;
    }
}
