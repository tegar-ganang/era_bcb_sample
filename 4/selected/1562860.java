package at.fhjoanneum.aim.sdi.project.service.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.ISVNEditor;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import at.fhjoanneum.aim.sdi.project.exceptions.CreateGroupException;
import at.fhjoanneum.aim.sdi.project.exceptions.CreateHomeException;
import at.fhjoanneum.aim.sdi.project.exceptions.CreateProjectException;
import at.fhjoanneum.aim.sdi.project.exceptions.CreateRepositoryException;
import at.fhjoanneum.aim.sdi.project.exceptions.DeleteGroupException;
import at.fhjoanneum.aim.sdi.project.exceptions.DeleteProjectException;
import at.fhjoanneum.aim.sdi.project.exceptions.ModifyPermissionException;
import at.fhjoanneum.aim.sdi.project.exceptions.SVNSetupException;
import at.fhjoanneum.aim.sdi.project.ldap.LDAPObject;
import at.fhjoanneum.aim.sdi.project.object.Project;
import at.fhjoanneum.aim.sdi.project.service.IAccessFileService;
import at.fhjoanneum.aim.sdi.project.svnconfig.AccessGroup;
import at.fhjoanneum.aim.sdi.project.svnconfig.DirAccess;
import at.fhjoanneum.aim.sdi.project.svnconfig.Group;
import at.fhjoanneum.aim.sdi.project.svnconfig.Repository;
import at.fhjoanneum.aim.sdi.project.svnconfig.Target;
import at.fhjoanneum.aim.sdi.project.svnconfig.User;
import at.fhjoanneum.aim.sdi.project.utilities.GlobalProperties;
import at.fhjoanneum.aim.sdi.project.utilities.TemplateGenerator;
import at.fhjoanneum.aim.sdi.project.utilities.TemplateWriter;
import at.fhjoanneum.aim.sdi.project.utilities.WorkspaceMediator;
import at.fhjoanneum.aim.sdi.project.window.MainWindow;

public class AccessFileServiceImpl implements IAccessFileService {

    private static final Logger log = Logger.getLogger(AccessFileServiceImpl.class);

    private TemplateGenerator tg = new TemplateGenerator();

    private RepositoryAccessService ras = null;

    private LinkedList<Repository> repoList = new LinkedList<Repository>();

    private LinkedList<Repository> displayRepos = new LinkedList<Repository>();

    private MainWindow main = new MainWindow();

    @SuppressWarnings("static-access")
    public LinkedList<Repository> getRepoList() {
        for (Repository repo : ras.getRepoAccess()) {
            if (!repo.getName().equals("[/]")) {
                repoList.add(repo);
            }
        }
        return repoList;
    }

    /**
	 * Retourniert eine LinkedList mit allen Repositories, wo ein
	 * Benutzer zumindest einen Lesezugriff auf ein Unterverzeichnis hat.
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public LinkedList<Repository> getDisplayRepositories() {
        LinkedList<String> list = new LinkedList<String>();
        for (int i = 1; i < RepositoryAccessService.getRepoAccess().size(); i++) {
            String repopath = RepositoryAccessService.getRepoAccess().get(i).getName().substring(1, RepositoryAccessService.getRepoAccess().get(i).getName().indexOf(":"));
            boolean allowed = true;
            try {
                new ServiceImpl(GlobalProperties.getURL() + repopath, Sessions.getCurrent().getAttribute("g_usr").toString(), Sessions.getCurrent().getAttribute("g_pwd").toString());
                ISVNEditor editor = ServiceImpl.getRepository().getCommitEditor("", new WorkspaceMediator());
                editor.openRoot(-1);
                editor.closeEdit();
            } catch (SVNSetupException e) {
                allowed = false;
            } catch (SVNException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
            }
            if (allowed) {
                list.add(RepositoryAccessService.getRepoAccess().get(i).getName());
            }
        }
        Collections.sort((List<String>) list);
        if (list.size() < 1) {
        } else if (list.size() == 1) {
            for (int i = 1; i < RepositoryAccessService.getRepoAccess().size(); i++) {
                if (RepositoryAccessService.getRepoAccess().get(i).getName().contains(list.get(0))) {
                    displayRepos.add(RepositoryAccessService.getRepoAccess().get(i));
                    break;
                }
            }
        } else {
            for (int j = 0; j < list.size(); j++) {
                for (int i = 1; i < RepositoryAccessService.getRepoAccess().size(); i++) {
                    if (RepositoryAccessService.getRepoAccess().get(i).getName().contains(list.get(j))) {
                        displayRepos.add(RepositoryAccessService.getRepoAccess().get(i));
                    }
                }
            }
        }
        return displayRepos;
    }

    public void parseFile() {
        tg.setGroupList(new Vector<Group>(1, 1));
        tg.setRepoList(new Vector<Repository>(1, 1));
        tg.generateTemplate(GlobalProperties.getFILEPATH());
        tg.parseTemplate("");
        RepositoryAccessService.setGroupList(new Vector<Group>(1, 1));
        RepositoryAccessService.setRepoAccess(new Vector<Repository>(1, 1));
        ras = new RepositoryAccessService(tg.getRepoList(), tg.getGroupList());
        RepositoryAccessService.setRevision(tg.getRev());
    }

    public Collection<String> getPermissionList(String path) {
        CalcEffectiveRights myCalc = new CalcEffectiveRights();
        return myCalc.getEffectiveRights(path);
    }

    public Collection<String> getRootPermissions() {
        CalcEffectiveRights myCalc = new CalcEffectiveRights();
        return myCalc.getRootPermission();
    }

    @SuppressWarnings("static-access")
    public Collection<Group> getLocalGroups() {
        return ras.getGroupList();
    }

    @SuppressWarnings("static-access")
    public void addGroupToPermissionList(String path, Listitem item, boolean read, boolean write) {
        if (this.checkRevision()) {
            path = preparePath(path);
            AccessGroup tempTarget = new AccessGroup();
            tempTarget.setName("@" + item.getLabel());
            DirAccess dirA = new DirAccess();
            dirA.setReadPermission(read);
            dirA.setWritePermission(write);
            dirA.setTargets(tempTarget);
            try {
                ras.addUserOrGroupRights(path, dirA);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (ModifyPermissionException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    @SuppressWarnings("static-access")
    public void addUserToPermissionList(String path, Listitem item, boolean read, boolean write) {
        if (this.checkRevision()) {
            path = preparePath(path);
            User tempTarget = new User();
            tempTarget.setName(item.getLabel());
            DirAccess dirA = new DirAccess();
            dirA.setReadPermission(read);
            dirA.setWritePermission(write);
            dirA.setTargets(tempTarget);
            try {
                ras.addUserOrGroupRights(path, dirA);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (ModifyPermissionException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    @SuppressWarnings("static-access")
    public void modifyUserInPermissionList(String path, Listitem item, boolean read, boolean write) {
        if (this.checkRevision()) {
            User tempTarget = new User();
            tempTarget.setName(item.getLabel());
            path = preparePath(path);
            DirAccess dirA = new DirAccess();
            dirA.setReadPermission(read);
            dirA.setWritePermission(write);
            dirA.setTargets(tempTarget);
            try {
                ras.setUserOrGroupRights(path, dirA);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (ModifyPermissionException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    @SuppressWarnings("static-access")
    public void modifyGroupInPermissionList(String path, Listitem item, boolean read, boolean write) {
        if (this.checkRevision()) {
            path = preparePath(path);
            AccessGroup tempTarget = new AccessGroup();
            tempTarget.setName(item.getLabel());
            DirAccess dirA = new DirAccess();
            dirA.setReadPermission(read);
            dirA.setWritePermission(write);
            dirA.setTargets(tempTarget);
            try {
                ras.setUserOrGroupRights(path, dirA);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (ModifyPermissionException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    @SuppressWarnings("static-access")
    public void deleteGroupInPermissionList(String path, Listitem item, String curRepository) {
        if (this.checkRevision()) {
            path = preparePath(path);
            AccessGroup tempTarget = new AccessGroup();
            tempTarget.setName(item.getLabel());
            DirAccess dirA = new DirAccess();
            dirA.setTargets(tempTarget);
            ras.deleteUserOrGroup(path, dirA);
            this.updateAccessFile();
            RepositoryAccessService.updateRevision();
            Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
        }
    }

    @SuppressWarnings("static-access")
    public void deleteUserInPermissionList(String path, Listitem item, String curRepository) {
        if (this.checkRevision()) {
            path = preparePath(path);
            User tempTarget = new User();
            tempTarget.setName(item.getLabel());
            DirAccess dirA = new DirAccess();
            dirA.setTargets(tempTarget);
            ras.deleteUserOrGroup(path, dirA);
            this.updateAccessFile();
            RepositoryAccessService.updateRevision();
            Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
        }
    }

    public void createFolder(String pathFolderName, boolean withSubFolder) {
        if (this.checkRevision()) {
            CreateProject project = new CreateProject();
            Project tempProject = new Project();
            tempProject.setStructured(withSubFolder);
            tempProject.setTargetURL("/" + pathFolderName);
            try {
                project.createProject(tempProject);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (CreateProjectException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    public void deleteFolder(String pathFolderName) {
        if (this.checkRevision()) {
            DeleteProject project = new DeleteProject();
            Project tempProject = new Project();
            tempProject.setTargetURL(pathFolderName);
            try {
                project.deleteProject(tempProject);
                updateAccessFile();
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (DeleteProjectException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    @SuppressWarnings("static-access")
    public boolean updateAccessFile() {
        TemplateWriter tw = new TemplateWriter();
        try {
            tw.storeToFS(GlobalProperties.getFILEPATH(), ras.getRepoAccess(), ras.getGroupList());
        } catch (IOException e) {
            String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
            for (StackTraceElement element : e.getStackTrace()) {
                message += element.toString();
            }
            GlobalProperties.getMyLogger().severe(message);
            return false;
        }
        return true;
    }

    public Repository addRepo(String repoPath) throws CreateRepositoryException {
        if (this.checkRevision()) {
            repoPath = "/" + repoPath;
            CreateRepository newRepo = new CreateRepository();
            Repository repo = newRepo.createRepository(repoPath);
            RepositoryAccessService.updateRevision();
            Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            return repo;
        } else {
            return null;
        }
    }

    public void addGroup(String groupName) {
        if (this.checkRevision()) {
            CreateGroup cg = new CreateGroup();
            Group newGroup = new Group();
            newGroup.setName(groupName);
            try {
                cg.createGroup(newGroup);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (CreateGroupException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    public void deleteGroup(String groupName) {
        if (this.checkRevision()) {
            DeleteGroup dg = new DeleteGroup();
            try {
                dg.deleteGroup(groupName, true);
                RepositoryAccessService.updateRevision();
                Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
            } catch (DeleteGroupException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.info(e);
            }
        }
    }

    @SuppressWarnings("static-access")
    public void updateGroup(String groupName, List<Listitem> groupMembers) {
        if (this.checkRevision()) {
            for (Group tempGroup : this.getLocalGroups()) {
                Group group = new Group();
                group.setName(groupName);
                if (tempGroup.getName().equals(groupName)) {
                    for (Listitem member : groupMembers) {
                        User tar = new User();
                        tar.setName(member.getLabel());
                        group.addMember(tar);
                    }
                    ras.updateGroup(group);
                }
            }
            RepositoryAccessService.updateRevision();
            Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
        }
    }

    public void updateGroup(String groupName, Collection<LDAPObject> ldapList) {
        if (this.checkRevision()) {
            for (Group tempGroup : this.getLocalGroups()) {
                if (tempGroup.getName().contains(groupName)) {
                    Vector<Target> memberList = new Vector<Target>();
                    for (LDAPObject member : ldapList) {
                        Target tar = new User();
                        tar.setName(member.getName());
                        memberList.add(tar);
                    }
                    tempGroup.setMembers(memberList);
                }
            }
            RepositoryAccessService.updateRevision();
            Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
        }
    }

    public void addHomeFolder(String imaRepo, Collection<LDAPObject> ldapList) throws CreateHomeException {
        if (this.checkRevision()) {
            CreateHome homeFolder = new CreateHome();
            homeFolder.createHomeForRepo(ldapList, imaRepo);
            RepositoryAccessService.updateRevision();
            Sessions.getCurrent().setAttribute("user_rev", RepositoryAccessService.getRevision());
        }
    }

    private boolean checkRevision() {
        int curRevision = (Integer) Sessions.getCurrent().getAttribute("user_rev");
        int fileRevision = RepositoryAccessService.getRevision();
        log.info("SessionRevision: " + curRevision + ", File Revision: " + fileRevision);
        if (curRevision == fileRevision) {
            return true;
        } else {
            try {
                Messagebox.show("Changes of remote user affect current session. Data is reloaded ", "Information", Messagebox.OK, Messagebox.INFORMATION);
            } catch (InterruptedException e) {
                String message = e.getClass().getCanonicalName() + " " + e.getLocalizedMessage();
                for (StackTraceElement element : e.getStackTrace()) {
                    message += element.toString();
                }
                GlobalProperties.getMyLogger().severe(message);
                log.error(e);
            }
            main.refreshTree();
            return false;
        }
    }

    private String preparePath(String path) {
        if (path.contains("/")) {
            String[] splitter = path.split("/");
            path = "[" + splitter[0] + (splitter[0].contains(":") ? "/" : ":/");
            for (int i = 1; i < splitter.length; i++) {
                path += splitter[i] + "/";
            }
            path = path.substring(0, path.length() - 1) + "]";
        } else {
            path = "[" + path + ":/]";
        }
        return path;
    }
}
